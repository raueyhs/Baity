const crypto = require("crypto");
const storage = require("./storage");

const SESSION_SERVER = "https://sessionserver.mojang.com/session/minecraft/hasJoined";

const TOKEN_TTL_MS = 7 * 24 * 60 * 60 * 1000;
const USER_TTL_MS = 14 * 24 * 60 * 60 * 1000;
const LIVENESS_REFRESH_MS = 12 * 60 * 60 * 1000;
const MAX_BODY_BYTES = 8 * 1024;
const REPORT_MIN_INTERVAL_MS = 3 * 1000;

const USERS_KEY = "users.json";

const TOKEN_SECRET = process.env.TOKEN_SECRET || "";

function json(data, status = 200) {
  return {
    statusCode: status,
    headers: { "content-type": "application/json; charset=utf-8" },
    body: JSON.stringify(data)
  };
}

function signToken(uuid, expiresAt) {
  const payload = `${uuid}|${expiresAt}`;
  const signature = crypto.createHmac("sha256", TOKEN_SECRET).update(payload).digest("base64url");
  return `${Buffer.from(payload).toString("base64url")}.${signature}`;
}

function verifyToken(token) {
  if (!token || !TOKEN_SECRET) return null;
  const dot = token.lastIndexOf(".");
  if (dot <= 0) return null;
  const payloadB64 = token.slice(0, dot);
  const provided = token.slice(dot + 1);
  let payload;
  try {
    payload = Buffer.from(payloadB64, "base64url").toString("utf8");
  } catch {
    return null;
  }
  const expected = crypto.createHmac("sha256", TOKEN_SECRET).update(payload).digest("base64url");
  if (provided.length !== expected.length) return null;
  if (!crypto.timingSafeEqual(Buffer.from(provided), Buffer.from(expected))) return null;
  const parts = payload.split("|");
  const uuid = parts[0];
  const expiresAt = Number(parts[1]);
  if (!uuid || !Number.isFinite(expiresAt) || Date.now() > expiresAt) return null;
  return uuid;
}

function normalizeUuid(raw) {
  const value = String(raw || "").trim().toLowerCase();
  if (!/^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/.test(value)) return "";
  return value;
}

function sanitizeName(raw) {
  let out = "";
  let count = 0;
  for (const char of String(raw || "")) {
    const code = char.codePointAt(0);
    const isFormat =
      code === 0x00a7 ||
      (code >= 0x200b && code <= 0x200f) ||
      (code >= 0x202a && code <= 0x202e) ||
      (code >= 0x2066 && code <= 0x2069) ||
      code === 0xfeff;
    if (code < 32 || code === 127 || isFormat) continue;
    out += char;
    count++;
    if (count >= 32) break;
  }
  return out.trim();
}

function appearanceCore(appearance) {
  if (!appearance || typeof appearance !== "object") return "";
  const core = {};
  for (const [key, value] of Object.entries(appearance)) {
    if (key === "meta") continue;
    core[key] = value;
  }
  return JSON.stringify(core);
}

function readBody(event) {
  let raw = event.body;
  if (event.isBase64Encoded && typeof raw === "string") {
    raw = Buffer.from(raw, "base64").toString("utf8");
  }
  if (typeof raw !== "string" || !raw.trim()) return {};
  if (raw.length > MAX_BODY_BYTES) return {};
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

function headersOf(event) {
  const source = event.headers || {};
  const out = {};
  for (const [key, value] of Object.entries(source)) {
    out[String(key).toLowerCase()] = value;
  }
  return out;
}

function bearerToken(event) {
  const raw = headersOf(event).authorization || "";
  const match = /^Bearer\s+(.+)$/i.exec(String(raw).trim());
  return match ? match[1].trim() : "";
}

function usersUrl() {
  return storage.objectUrl(USERS_KEY);
}

async function handleAuth(event) {
  const body = readBody(event);
  const uuid = normalizeUuid(body.uuid);
  if (!uuid) return json({ ok: false, error: "invalid_uuid" }, 400);

  const name = String(body.name || "").trim();
  if (!name || name.length > 32) return json({ ok: false, error: "invalid_name" }, 400);

  const serverId = String(body.serverId || "").trim().toLowerCase();
  if (!/^[0-9a-f]{32}$/.test(serverId)) {
    return json({ ok: false, error: "session_challenge_required" }, 401);
  }

  const verifyUrl = `${SESSION_SERVER}?username=${encodeURIComponent(name)}&serverId=${serverId}`;
  let verifiedId = "";
  try {
    const response = await fetch(verifyUrl, { headers: { accept: "application/json" } });
    if (response.status === 200) {
      const profile = await response.json();
      if (profile && typeof profile.id === "string") {
        verifiedId = profile.id.trim().toLowerCase();
      }
    }
  } catch {
    verifiedId = "";
  }

  if (!verifiedId || verifiedId !== uuid.replace(/-/g, "")) {
    return json({ ok: false, error: "session_verification_failed" }, 401);
  }

  const expiresAt = Date.now() + TOKEN_TTL_MS;
  return json({ ok: true, token: signToken(uuid, expiresAt), expiresAt });
}

// TODO: the read-modify-write below is not atomic, so two concurrent /sync calls can
// overwrite each other's entry. Revisit if the user base grows well beyond the current
// few dozen (e.g. optimistic version check, or one object per user).
async function handleSync(event) {
  const uuid = verifyToken(bearerToken(event));
  if (!uuid) return json({ ok: false, error: "unauthorized" }, 401);

  const body = readBody(event);
  const appearance = body.appearance && typeof body.appearance === "object" ? body.appearance : null;
  const name = sanitizeName(body.name);
  const now = Date.now();

  const root = (await storage.readJson(USERS_KEY)) || { version: 0, updatedAt: 0, users: {} };
  if (!root.users || typeof root.users !== "object") root.users = {};

  let dirty = false;
  let contentChanged = false;

  if (appearance) {
    const previous = root.users[uuid] || null;
    const previousAppearance = previous && previous.appearance ? previous.appearance : null;
    const changed = appearanceCore(previousAppearance) !== appearanceCore(appearance);
    const lastWrite = Number(previous && previous.updatedAt) || 0;
    if (changed && now - lastWrite < REPORT_MIN_INTERVAL_MS) {
      return json({ ok: false, error: "rate_limited" }, 429);
    }
    if (changed || now - lastWrite > LIVENESS_REFRESH_MS) {
      root.users[uuid] = { name, appearance, updatedAt: now };
      dirty = true;
    }
    if (changed) contentChanged = true;
  }

  for (const [entryUuid, entry] of Object.entries(root.users)) {
    const seenAt = Number(entry && entry.updatedAt) || 0;
    if (now - seenAt > USER_TTL_MS) {
      delete root.users[entryUuid];
      dirty = true;
      contentChanged = true;
    }
  }

  if (dirty) {
    if (contentChanged) {
      root.version = now;
    }
    root.updatedAt = now;
    await storage.writeJson(USERS_KEY, root);
  }

  return json({
    ok: true,
    version: Number(root.version) || 0,
    changed: contentChanged,
    usersUrl: usersUrl()
  });
}

exports.main = async (event) => {
  const path = String(event.path || "/").replace(/\/+$/, "") || "/";
  const method = String(event.httpMethod || "GET").toUpperCase();

  if (method === "GET" && path === "/health") {
    return json({ ok: true, ts: Date.now(), usersUrl: usersUrl() });
  }
  if (method === "POST" && path === "/auth") {
    return handleAuth(event);
  }
  if (method === "POST" && path === "/sync") {
    return handleSync(event);
  }
  return json({ ok: false, error: "not_found" }, 404);
};
