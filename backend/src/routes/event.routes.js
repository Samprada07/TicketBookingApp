const express = require("express");
const router = express.Router();
const pool = require("../db");
const authenticateToken = require("../middleware/auth.middleware");

// Get all events
router.get("/", async (req, res) => {
    try {
        const events = await pool.query("SELECT * FROM events ORDER BY start_time ASC");
        res.json({ events: events.rows });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Get single event
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

// Create event (protected)
router.post("/", authenticateToken, async (req, res) => {
    const { name, description, venue, start_time, end_time, total_seats } = req.body;
    try {
        const newEvent = await pool.query(
            `INSERT INTO events (name, description, venue, start_time, end_time, total_seats, available_seats)
             VALUES ($1, $2, $3, $4, $5, $6, $6) RETURNING *`,
            [name, description, venue, start_time, end_time, total_seats]
        );
        res.json({ event: newEvent.rows[0] });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

module.exports = router;
