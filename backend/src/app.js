require("dotenv").config();

const pool = require("./db");

const express = require("express");
const cors = require("cors");

const app = express();

// Middlewares
app.use(cors());
app.use(express.json());

// Test route
app.get("/health", (req, res) => {
  res.json({
    status: "OK",
    message: "Ticket Booking Backend is running 🚀"
  });
});

const authRoutes = require("./routes/auth.routes");
app.use("/api/auth", authRoutes);

const eventRoutes = require("./routes/event.routes");
app.use("/api/events", eventRoutes);

const ticketRoutes = require("./routes/ticket.routes");
app.use("/api/tickets", ticketRoutes);

module.exports = app;

