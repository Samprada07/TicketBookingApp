require("dotenv").config();

const pool = require("./db");

const express = require("express");
const cors = require("cors");

const app = express();

// Middlewares
app.use(cors());
app.use(express.json());
app.use('/api/payment/webhook', express.raw({ type: 'application/json' }));

// Test route
app.get("/health", (req, res) => {
  res.json({
    status: "OK",
    message: "Ticket Booking Backend is running 🚀"
  });
});

app.use('/uploads', express.static('uploads'));

const authRoutes = require("./routes/auth.routes");
app.use("/api/auth", authRoutes);

const eventRoutes = require("./routes/event.routes");
app.use("/api/events", eventRoutes);

const ticketRoutes = require("./routes/ticket.routes");
app.use("/api/tickets", ticketRoutes);

const uploadRoutes = require("./routes/upload.routes");
app.use("/api/upload", uploadRoutes);

const paymentRoutes = require("./routes/payment.routes");
app.use("/api/payment", paymentRoutes);

module.exports = app;

