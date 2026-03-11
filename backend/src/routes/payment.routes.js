const express = require("express");
const router = express.Router();
const pool = require("../db");
const stripe = require("../config/stripe");
const authenticateToken = require("../middleware/auth.middleware");

// Create payment intent for ticket booking
router.post("/create-intent", authenticateToken, async (req, res) => {
    const { event_id, seat_number } = req.body;

    try {
        // Get event details
        const event = await pool.query("SELECT * FROM events WHERE id = $1", [event_id]);
        if (event.rows.length === 0) {
            return res.status(404).json({ error: "Event not found" });
        }

        if (event.rows[0].available_seats <= 0) {
            return res.status(400).json({ error: "No seats available" });
        }

        const eventData = event.rows[0];
        const amount = Math.round(eventData.price * 100); // Convert to paise/cents

        // Create Stripe payment intent
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

        // Create pending ticket (will be confirmed after payment)
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

// Confirm payment and activate ticket
router.post("/confirm", authenticateToken, async (req, res) => {
    const { payment_intent_id } = req.body;

    try {
        // Verify payment with Stripe
        const paymentIntent = await stripe.paymentIntents.retrieve(payment_intent_id);

        if (paymentIntent.status === 'succeeded') {
            // Update ticket status
            const ticket = await pool.query(
                `UPDATE tickets
                 SET status = 'active', payment_status = 'succeeded'
                 WHERE payment_intent_id = $1
                 RETURNING *`,
                [payment_intent_id]
            );

            if (ticket.rows.length > 0) {
                // Decrease available seats
                await pool.query(
                    "UPDATE events SET available_seats = available_seats - 1 WHERE id = $1",
                    [ticket.rows[0].event_id]
                );

                res.json({
                    success: true,
                    ticket: ticket.rows[0],
                    message: "Payment successful! Ticket booked."
                });
            } else {
                res.status(404).json({ error: "Ticket not found" });
            }
        } else {
            res.status(400).json({ error: "Payment not completed" });
        }

    } catch (err) {
        console.error(err.message);
        res.status(500).json({ error: err.message });
    }
});

// Process refund for cancelled ticket
router.post("/refund", authenticateToken, async (req, res) => {
    const { ticket_id } = req.body;

    try {
        // Get ticket details
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

        if (ticketData.refund_status === 'refunded') {
            return res.status(400).json({ error: "Already refunded" });
        }

        // Create Stripe refund
        const refund = await stripe.refunds.create({
            payment_intent: ticketData.payment_intent_id,
            amount: Math.round(ticketData.payment_amount * 100)
        });

        // Update ticket
        await pool.query(
            `UPDATE tickets
             SET status = 'cancelled', refund_id = $1, refund_status = 'refunded'
             WHERE id = $2`,
            [refund.id, ticket_id]
        );

        // Restore seat
        await pool.query(
            "UPDATE events SET available_seats = available_seats + 1 WHERE id = $1",
            [ticketData.event_id]
        );

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