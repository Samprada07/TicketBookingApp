const express = require("express");
const router = express.Router();
const pool = require("../db");
const authenticateToken = require("../middleware/auth.middleware");
const isAdmin = require("../middleware/admin.middleware");

// Get all events (public)
router.get("/", async (req, res) => {
    try {
        const events = await pool.query("SELECT * FROM events ORDER BY start_time ASC");
        res.json({ events: events.rows });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Get single event (public)
router.get("/:id", async (req, res) => {
    try {
        const event = await pool.query("SELECT * FROM events WHERE id = $1", [req.params.id]);
        if (event.rows.length === 0) return res.status(404).json({ error: "Event not found" });
        res.json({ event: event.rows[0] });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Create event (ADMIN ONLY)
router.post("/", isAdmin, async (req, res) => {
    const { name, description, venue, start_time, end_time, total_seats, image_url } = req.body;
    try {
        const newEvent = await pool.query(
            `INSERT INTO events (name, description, venue, start_time, end_time, total_seats, available_seats, image_url)
             VALUES ($1, $2, $3, $4, $5, $6, $6, $7) RETURNING *`,
            [name, description, venue, start_time, end_time, total_seats, image_url || null]
        );
        res.json({ event: newEvent.rows[0] });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Delete event (ADMIN ONLY)
router.delete("/:id", isAdmin, async (req, res) => {
    try {
        const deletedEvent = await pool.query(
            "DELETE FROM events WHERE id = $1 RETURNING *",
            [req.params.id]
        );

        if (deletedEvent.rows.length === 0) {
            return res.status(404).json({ error: "Event not found" });
        }

        res.json({ message: "Event deleted", event: deletedEvent.rows[0] });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Get all bookings for an event (ADMIN ONLY)
router.get("/:id/bookings", isAdmin, async (req, res) => {
    try {
        const bookings = await pool.query(
            `SELECT t.id, t.seat_number, t.booked_at, u.name as user_name, u.email as user_email
             FROM tickets t
             JOIN users u ON t.user_id = u.id
             WHERE t.event_id = $1
             ORDER BY t.booked_at DESC`,
            [req.params.id]
        );
        res.json({ bookings: bookings.rows });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

module.exports = router;