const express = require("express");
const router = express.Router();
const bcrypt = require("bcrypt");
const pool = require("../db");

// Register user
const jwt = require("jsonwebtoken");
router.post("/register", async (req, res) => {
    const { name, email, password } = req.body;

    try {
        // Check if user exists
        const userExist = await pool.query(
            "SELECT * FROM users WHERE email = $1",
            [email]
        );
        if (userExist.rows.length > 0) {
            return res.status(400).json({ error: "User already exists" });
        }

        // Hash password
        const salt = await bcrypt.genSalt(10);
        const password_hash = await bcrypt.hash(password, salt);

        // Insert into DB (role defaults to 'user')
        const newUser = await pool.query(
            "INSERT INTO users (name, email, password_hash) VALUES ($1, $2, $3) RETURNING *",
            [name, email, password_hash]
        );

        const user = newUser.rows[0];

        // Generate JWT
        const token = jwt.sign(
            { id: user.id, email: user.email, role: user.role },  // Include role in token
            process.env.JWT_SECRET,
            { expiresIn: "7d" }
        );

        // Send back token + user (including role)
        res.status(201).json({
            token,
            user: {
                id: user.id,
                name: user.name,
                email: user.email,
                role: user.role || 'user'
            }
        });

    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Login user
router.post("/login", async (req, res) => {
    const { email, password } = req.body;

    try {
        // Check if user exists
        const user = await pool.query(
            "SELECT * FROM users WHERE email = $1",
            [email]
        );

        if (user.rows.length === 0) {
            return res.status(400).json({ error: "Invalid credentials" });
        }

        const validPassword = await bcrypt.compare(password, user.rows[0].password_hash);
        if (!validPassword) {
            return res.status(400).json({ error: "Invalid credentials" });
        }

        // Create JWT (include role)
        const jwt = require("jsonwebtoken");
        const token = jwt.sign(
            { id: user.rows[0].id, email: user.rows[0].email, role: user.rows[0].role },
            process.env.JWT_SECRET,
            { expiresIn: process.env.JWT_EXPIRES_IN }
        );

        res.json({
            token,
            user: {
                id: user.rows[0].id,
                name: user.rows[0].name,
                email: user.rows[0].email,
                role: user.rows[0].role || 'user'
            }
        });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

const authenticateToken = require("../middleware/auth.middleware");

// Protected route example
router.get("/me", authenticateToken, async (req, res) => {
    try {
        const user = await pool.query(
            "SELECT id, name, email, role FROM users WHERE id = $1",
            [req.user.id]
        );
        res.json({ user: user.rows[0] });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

// Update user profile (authenticated users)
router.put("/profile", authenticateToken, async (req, res) => {
    const { name, email } = req.body;

    try {
        // Check if email is already taken by another user
        if (email) {
            const existingUser = await pool.query(
                "SELECT * FROM users WHERE email = $1 AND id != $2",
                [email, req.user.id]
            );
            if (existingUser.rows.length > 0) {
                return res.status(400).json({ error: "Email already in use" });
            }
        }

        // Update user
        const updatedUser = await pool.query(
            "UPDATE users SET name = $1, email = $2 WHERE id = $3 RETURNING id, name, email, role",
            [name || req.user.name, email || req.user.email, req.user.id]
        );

        if (updatedUser.rows.length === 0) {
            return res.status(404).json({ error: "User not found" });
        }

        res.json({
            user: updatedUser.rows[0],
            message: "Profile updated successfully"
        });
    } catch (err) {
        console.error(err.message);
        res.status(500).send("Server error");
    }
});

module.exports = router;