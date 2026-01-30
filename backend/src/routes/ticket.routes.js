const express = require("express");
const router = express.Router();
const pool = require("../db");
const authenticateToken = require("../middleware/auth.middleware");

// Book a ticket
router.post("/book", authenticateToken, async (req, res) => {
    const { event_id, seat_number } = req.body;
    const user_id = req.user.id;

    try {
        // Check if event exists
        const eventRes = await pool.query("SELECT * FROM events WHERE id = $1", [event_id]);
        if (eventRes.rows.length === 0) return res.status(404).json({ error: "Event not found" });

        const event = eventRes.rows[0];

        // Check available seats
        if (event.available_seats <= 0) return res.status(400).json({ error: "No seats available" });

        // Optional: Check if seat_number is already booked
        if (seat_number) {
            const seatRes = await pool.query(
                "SELECT * FROM tickets WHERE event_id = $1 AND seat_number = $2",
                [event_id, seat_number]
            );
            if (seatRes.rows.length > 0) return res.status(400).json({ error: "Seat already booked" });
        }

        // Insert ticket
        const newTicket = await pool.query(
            "INSERT INTO tickets (user_id, event_id, seat_number) VALUES ($1, $2, $3) RETURNING *",
            [user_id, event_id, seat_number || null]
        );

        // Update available seats
        await pool.query(
            "UPDATE events SET available_seats = available_seats - 1 WHERE id = $1",
            [event_id]
        );

        res.json({ ticket: newTicket.rows[0] });

    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

//List User Tickets
router.get("/my", authenticateToken, async (req, res) => {
    try {
        const tickets = await pool.query(
            `SELECT t.id, t.seat_number, t.booked_at, e.name as event_name, e.venue, e.start_time
             FROM tickets t
             JOIN events e ON t.event_id = e.id
             WHERE t.user_id = $1
             ORDER BY t.booked_at DESC`,
            [req.user.id]
        );
        res.json({ tickets: tickets.rows });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

module.exports = router;
