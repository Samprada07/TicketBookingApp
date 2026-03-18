require("dotenv").config();
const cron = require('node-cron');
const app = require("./app");

const PORT = process.env.PORT || 3000;

app.listen(PORT, '0.0.0.0', () => {
  console.log(`Server running on port ${PORT}`);
});

cron.schedule('0 * * * *', async () => {
    const result = await pool.query(
        `UPDATE tickets SET status = 'expired'
         WHERE status IN ('active', 'pending')
         AND event_id IN (SELECT id FROM events WHERE start_time < NOW())`
    );
    console.log('Auto-expired ${result.rowCount} tickets');
});

console.log('Auto-expire cron started (runs hourly)');