const express = require("express");
const router = express.Router();
const pool = require("../db");
const stripe = require("../config/stripe");
const authenticateToken = require("../middleware/auth.middleware");

// PREVENT DUPLICATE BOOKINGS
// POST /create-intent to check for existing active tickets
router.post("/create-intent", authenticateToken, async (req, res) => {
    const { event_id, seat_number } = req.body;

    try {
        const event = await pool.query("SELECT * FROM events WHERE id = $1", [event_id]);
        if (event.rows.length === 0) {
            return res.status(404).json({ error: "Event not found" });
        }

        if (event.rows[0].available_seats <= 0) {
            return res.status(400).json({ error: "No seats available" });
        }

        const eventStart = new Date(event.rows[0].start_time);
        if (eventStart < new Date()) {
            return res.status(400).json({ error: "This event has already passed" });
        }

        // NEW: Check for existing active/pending ticket for same event
        const existingTicket = await pool.query(
            `SELECT * FROM tickets
             WHERE user_id = $1 AND event_id = $2
             AND status IN ('active', 'pending')
             AND payment_status IN ('succeeded', 'pending')`,
            [req.user.id, event_id]
        );

        if (existingTicket.rows.length > 0) {
            return res.status(409).json({
                error: "You already have a ticket for this event",
                ticket_id: existingTicket.rows[0].id
            });
        }

        // NEW: If specific seat requested, check if already booked
        if (seat_number) {
            const seatTaken = await pool.query(
                `SELECT * FROM tickets
                 WHERE event_id = $1 AND seat_number = $2
                 AND status = 'active' AND payment_status = 'succeeded'`,
                [event_id, seat_number]
            );

            if (seatTaken.rows.length > 0) {
                return res.status(409).json({
                    error: `Seat ${seat_number} is already booked. Please choose another seat.`
                });
            }
        }

        const eventData = event.rows[0];
        const amount = Math.round(eventData.price * 100);

        // Delete old pending tickets (expired payment intents)
        await pool.query(
            "DELETE FROM tickets WHERE user_id = $1 AND event_id = $2 AND payment_status = 'pending'",
            [req.user.id, event_id]
        );

        const paymentIntent = await stripe.paymentIntents.create({
            amount: amount,
            currency: 'inr',
            metadata: {
                user_id: req.user.id,
                event_id: event_id,
                seat_number: seat_number || 'any',
                event_name: eventData.name
            }
        });

        const ticket = await pool.query(
            `INSERT INTO tickets (user_id, event_id, seat_number, status, price, payment_intent_id, payment_status, payment_amount, payment_currency)
             VALUES ($1, $2, $3, 'pending', $4, $5, 'pending', $6, 'inr')
             RETURNING *`,
            [req.user.id, event_id, seat_number || null, eventData.price, paymentIntent.id, eventData.price]
        );

        res.json({
            clientSecret: paymentIntent.client_secret,
            ticketId: ticket.rows[0].id,
            amount: eventData.price
        });

    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: err.message });
    }
});

// IDEMPOTENT PAYMENT CONFIRMATION
// Update POST /confirm to handle duplicate confirmations
router.post("/confirm", authenticateToken, async (req, res) => {
    const { payment_intent_id } = req.body;

    try {
        // Check if already confirmed (idempotency)
        const existing = await pool.query(
            `SELECT * FROM tickets
             WHERE payment_intent_id = $1 AND payment_status = 'succeeded'`,
            [payment_intent_id]
        );

        if (existing.rows.length > 0) {
            // Already processed - return success
            return res.json({
                success: true,
                ticket: existing.rows[0],
                message: "Payment already confirmed"
            });
        }

        const paymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);

        if (paymentIntent.status === 'succeeded') {
            const ticket = await pool.query(
                `UPDATE tickets
                 SET status = 'active', payment_status = 'succeeded'
                 WHERE payment_intent_id = $1 AND payment_status = 'pending'
                 RETURNING *`,
                [payment_intent_id]
            );

            if (ticket.rows.length > 0) {
                // Decrease available seats (with row locking to prevent race conditions)
                await pool.query(
                    `UPDATE events
                     SET available_seats = available_seats - 1
                     WHERE id = $1 AND available_seats > 0`,
                    [ticket.rows[0].event_id]
                );

                res.json({
                    success: true,
                    ticket: ticket.rows[0],
                    message: "Payment successful! Ticket booked."
                });
            } else {
                res.status(404).json({ error: "Ticket not found or already confirmed" });
            }
        } else {
            res.status(400).json({
                error: `Payment status: ${paymentIntent.status}`,
                status: paymentIntent.status
            });
        }

    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: err.message });
    }
});


// 3. SAFE REFUND WITH DOUBLE-REFUND PREVENTION
// Update POST /refund

router.post("/refund", authenticateToken, async (req, res) => {
    const { ticket_id } = req.body;

    try {
        const ticket = await pool.query(
            "SELECT * FROM tickets WHERE id = $1 AND user_id = $2",
            [ticket_id, req.user.id]
        );

        if (ticket.rows.length === 0) {
            return res.status(404).json({ error: "Ticket not found" });
        }

        const ticketData = ticket.rows[0];

        if (ticketData.payment_status !== 'succeeded') {
            return res.status(400).json({ error: "Cannot refund unpaid ticket" });
        }

        // Check if already refunded (prevent double refund)
        if (ticketData.refund_status === 'refunded' || ticketData.status === 'cancelled') {
            return res.status(400).json({
                error: "Ticket already refunded",
                refund_id: ticketData.refund_id
            });
        }

        // Validate cancellation window
        const bookedAt = new Date(ticketData.booked_at);
        const eventStart = new Date(await pool.query(
            "SELECT start_time FROM events WHERE id = $1",
            [ticketData.event_id]
        ).then(r => r.rows[0].start_time));
        const now = new Date();

        const daysUntilEvent = (eventStart - now) / (1000 * 60 * 60 * 24);
        if (daysUntilEvent < 2) {
            return res.status(400).json({
                error: "Cannot cancel. Event is less than 2 days away."
            });
        }

        // Create Stripe refund
        const refund = await stripe.refunds.create({
            payment_intent: ticketData.payment_intent_id,
            amount: Math.round(ticketData.payment_amount * 100)
        });

        // Update ticket (use transaction to ensure atomicity)
        await pool.query('BEGIN');

        try {
            await pool.query(
                `UPDATE tickets
                 SET status = 'cancelled', refund_id = $1, refund_status = 'refunded'
                 WHERE id = $2`,
                [refund.id, ticket_id]
            );

            await pool.query(
                "UPDATE events SET available_seats = available_seats + 1 WHERE id = $1",
                [ticketData.event_id]
            );

            await pool.query('COMMIT');
        } catch (e) {
            await pool.query('ROLLBACK');
            throw e;
        }

        res.json({
            success: true,
            refund_id: refund.id,
            amount: ticketData.payment_amount,
            message: `Refund of ₹${ticketData.payment_amount} processed successfully`
        });

    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: err.message });
    }
});

// Stripe webhook endpoint (for payment confirmation)
router.post("/webhook", express.raw({ type: 'application/json' }), async (req, res) => {
    const sig = req.headers['stripe-signature'];
    const endpointSecret = process.env.STRIPE_WEBHOOK_SECRET;

    let event;

    try {
        event = stripe.webhooks.constructEvent(req.body, sig, endpointSecret);
    } catch (err) {
        console.error('Webhook signature verification failed:', err.message);
        return res.status(400).send(`Webhook Error: ${err.message}`);
    }

    // Handle payment events
    switch (event.type) {
        case 'payment_intent.succeeded':
            const paymentIntent = event.data.object;
            await pool.query(
                `UPDATE tickets SET status = 'active', payment_status = 'succeeded'
                 WHERE payment_intent_id = $1`,
                [paymentIntent.id]
            );
            console.log('Payment succeeded:', paymentIntent.id);
            break;

        case 'payment_intent.payment_failed':
            const failedIntent = event.data.object;
            await pool.query(
                `UPDATE tickets SET payment_status = 'failed'
                 WHERE payment_intent_id = $1`,
                [failedIntent.id]
            );
            console.log('Payment failed:', failedIntent.id);
            break;
    }

    res.json({ received: true });
});

module.exports = router;