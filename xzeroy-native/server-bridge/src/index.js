import http from "node:http";

const port = Number(process.env.PORT || 8787);
const token = process.env.XZEROY_ADMIN_TOKEN || "";
if (token.length < 32) {
  console.error("XZEROY_ADMIN_TOKEN must contain at least 32 characters.");
  process.exit(1);
}

function send(res, status, data) {
  res.writeHead(status, {
    "content-type": "application/json; charset=utf-8",
    "cache-control": "no-store",
    "x-content-type-options": "nosniff"
  });
  res.end(JSON.stringify(data));
}

async function readBody(req) {
  let raw = "";
  for await (const chunk of req) {
    raw += chunk;
    if (raw.length > 16384) throw new Error("BODY_TOO_LARGE");
  }
  return raw ? JSON.parse(raw) : {};
}

const server = http.createServer(async (req, res) => {
  const url = new URL(req.url || "/", "http://127.0.0.1");
  if (req.method === "GET" && url.pathname === "/health") {
    return send(res, 200, { ok: true, service: "xzeroy-bridge", adapter: "not-implemented" });
  }
  if (req.method === "GET" && url.pathname === "/v1/modules") {
    return send(res, 200, { modules: [
      { id: "auto-jump", status: "ui-prototype-only" },
      { id: "auto-clicker", status: "ui-prototype-only" },
      { id: "coordinates", status: "not-implemented" },
      { id: "esp", status: "not-implemented" }
    ] });
  }
  const adminPath = url.pathname === "/v1/admin/creative" || url.pathname === "/v1/admin/give";
  if (req.method === "POST" && adminPath) {
    if (req.headers.authorization !== "Bearer " + token) {
      return send(res, 401, { ok: false, error: "UNAUTHORIZED" });
    }
    try {
      await readBody(req);
    } catch (error) {
      const code = error.message === "BODY_TOO_LARGE" ? "BODY_TOO_LARGE" : "INVALID_JSON";
      return send(res, 400, { ok: false, error: code });
    }
    return send(res, 501, {
      ok: false,
      error: "ADAPTER_NOT_IMPLEMENTED",
      message: "Nenhuma ação executada: falta um adaptador testado para o servidor autorizado."
    });
  }
  return send(res, 404, { ok: false, error: "NOT_FOUND" });
});

server.listen(port, "127.0.0.1", () => {
  console.log("XZeroy bridge listening on http://127.0.0.1:" + port);
});
