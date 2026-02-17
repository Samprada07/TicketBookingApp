const jwt = require("jsonwebtoken");

// Check if user is admin
function isAdmin(req, res, next) {
    const authHeader = req.headers['authorization'];
    const token = authHeader && authHeader.split(' ')[1];

    if (!token) return res.status(401).json({ error: "No token provided" });

    jwt.verify(token, process.env.JWT_SECRET, (err, user) => {
        if (err) return res.status(403).json({ error: "Invalid token" });

        // Check if user has admin role
        if (user.role !== 'admin') {
            return res.status(403).json({ error: "Access denied. Admin only." });
        }

        req.user = user;
        next();
    });
}

module.exports = isAdmin;