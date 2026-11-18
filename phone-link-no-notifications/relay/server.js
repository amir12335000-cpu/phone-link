const express = require("express");
const http = require("http");
const WebSocket = require("ws");
const app = express();
const server = http.createServer(app);
const wss = new WebSocket.Server({ server, maxPayload: 16 * 1024 });
const rooms = new Map();
const joinAttempts = new Map();
const ROOM_TTL = 30 * 60 * 1000;

app.disable("x-powered-by");
app.use(express.static("public", { maxAge: "1h" }));
app.get("/health", (_req, res) => res.json({ ok: true }));

function send(ws, obj) {
  if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify(obj));
}
function cleanCode(v) { return String(v || "").toUpperCase().replace(/[^A-Z0-9]/g, ""); }

wss.on("connection", (ws, req) => {
  const ip = req.headers["x-forwarded-for"]?.split(",")[0]?.trim() || req.socket.remoteAddress || "unknown";
  ws.on("message", raw => {
    let m;
    try { m = JSON.parse(raw.toString()); } catch { return; }
    if (m.type === "join") {
      const now = Date.now();
      const a = joinAttempts.get(ip) || { start: now, count: 0 };
      if (now - a.start > 60_000) { a.start = now; a.count = 0; }
      a.count++; joinAttempts.set(ip, a);
      if (a.count > 30) return send(ws, { type: "error", text: "تلاش اتصال بیش از حد است." });
      if (ws.room) return;
      const code = cleanCode(m.code);
      if (!/^[A-Z0-9]{12}$/.test(code)) return send(ws, { type: "error", text: "کد اتصال نامعتبر است." });
      ws.room = code; ws.role = m.role === "phone" ? "phone" : "computer";
      if (!rooms.has(code)) rooms.set(code, new Set());
      const room = rooms.get(code); room.add(ws);
      room.expires = now + ROOM_TTL;
      send(ws, { type: "joined" });
      for (const peer of room) if (peer !== ws) send(peer, { type: "peer", role: ws.role });
      return;
    }

  });
  ws.on("close", () => {
    if (!ws.room || !rooms.has(ws.room)) return;
    const room = rooms.get(ws.room); room.delete(ws);
    for (const peer of room) send(peer, { type: "peer-left" });
    if (!room.size) rooms.delete(ws.room);
  });
});
setInterval(() => {
  const now = Date.now();
  for (const [code, room] of rooms) if (room.expires < now) {
    for (const ws of room) ws.close(4000, "expired");
    rooms.delete(code);
  }
}, 60_000).unref();

server.listen(process.env.PORT || 3000, "0.0.0.0", () =>
  console.log("Phone Link relay listening on port " + (process.env.PORT || 3000)));