function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: {
      "content-type": "application/json; charset=utf-8",
      "cache-control": "no-store",
      ...extraHeaders
    }
  });
}

const USER_RECORD_TTL_SECONDS = 60 * 60 * 24 * 3;
const USER_SOFT_STALE_MS = 1000 * 60 * 60 * 24 * 3;
const USER_HARD_EXPIRE_MS = 1000 * 60 * 60 * 24 * 3;
const READ_LOG_MAX_ENTRIES = 5000;
const WRITE_LOG_MAX_ENTRIES = 5000;
const TOKEN_MIN_LEN = 16;
const TOKEN_MAX_LEN = 128;
const DEFAULT_READ_TOKEN = "baity_sync_read_v1_f4c9e7a2d1b84e73";

const READ_LOG_DEDUP_SECONDS = 10 * 60;
const WRITE_LOG_DEDUP_SECONDS = 10 * 60;

const REGISTER_IP_WINDOW_SECONDS = 10 * 60;
const REGISTER_IP_MAX = 2;

function normalizeHexColor(value) {
  if (typeof value !== "string") return null;
  const m = value.match(/^#([A-Fa-f0-9]{6})$/);
  if (!m) return null;
  return `#${m[1].toUpperCase()}`;
}

function clampNumber(value, min, max, fallback) {
  const n = Number(value);
  if (!Number.isFinite(n)) return fallback;
  return Math.max(min, Math.min(max, n));
}

function sanitizeUserPayload(payload) {
  if (!payload || typeof payload !== "object") return null;
  const user = payload.user;
  if (!user || typeof user !== "object") return null;

  const uuid = String(user.uuid || "").trim();
  const name = String(user.name || "").trim();
  if (!/^[0-9a-fA-F-]{36}$/.test(uuid)) return null;
  if (!name || name.length > 32) return null;

  const isBaityUser = Boolean(user.isBaityUser);
  const features = user.features || {};
  const nickTweaksRaw = features.nickTweaks || {};
  const smolRaw = features.smolPeople || {};
  const nickEnabled = Boolean(nickTweaksRaw.enabled);
  const chromaEnabled = nickEnabled && Boolean(nickTweaksRaw.chromaEnabled);
  const customNickColorEnabled = nickEnabled && !chromaEnabled && Boolean(nickTweaksRaw.customNickColorEnabled);
  const nickChanger = nickEnabled ? String(nickTweaksRaw.nickChanger || "").slice(0, 128) : "";
  const chromaRaw = chromaEnabled ? (nickTweaksRaw.chroma || {}) : {};
  const solidRaw = customNickColorEnabled ? (nickTweaksRaw.solid || {}) : {};

  const palette = Array.isArray(chromaRaw.palette)
    ? chromaRaw.palette.map(normalizeHexColor).filter(Boolean).slice(0, 16)
    : [];
  const customColorStart = normalizeHexColor(solidRaw.customColorStart) || "#FF4D4D";
  const customColorEnd = normalizeHexColor(solidRaw.customColorEnd) || "#C299FF";

  return {
    uuid: uuid.toLowerCase(),
    name,
    isBaityUser,
    features: {
      nickTweaks: {
        enabled: nickEnabled,
        boldEnabled: nickEnabled && Boolean(nickTweaksRaw.boldEnabled),
        chromaEnabled,
        customNickColorEnabled,
        nickChanger,
        ...(nickEnabled
          ? (chromaEnabled
            ? {
                chroma: {
                  enabled: true,
                  speed: clampNumber(chromaRaw.speed, 0, 8, 1),
                  size: clampNumber(chromaRaw.size, 0.1, 12, 3.1),
                  chroma: clampNumber(chromaRaw.chroma, 0, 0.4, 0.2),
                  lightness: clampNumber(chromaRaw.lightness, 0.2, 1, 0.8),
                  palette: palette.length > 0 ? palette : ["#FF4D4D", "#FFAA00", "#FFFF66", "#66FF99", "#66CCFF", "#C299FF"]
                }
              }
            : (customNickColorEnabled ? {
                solid: {
                  customColorStart,
                  customColorEnd
                }
              } : {}))
          : {})
      },
      smolPeople: {
        enabled: Boolean(smolRaw.enabled)
      }
    },
    meta: {
      protocol: 1,
      lastSeenAt: new Date().toISOString()
    }
  };
}

function normalizeUuid(uuid) {
  const v = String(uuid || "").trim().toLowerCase();
  return /^[0-9a-f-]{36}$/.test(v) ? v : "";
}

function normalizeToken(token) {
  const value = String(token || "").trim();
  if (value.length < TOKEN_MIN_LEN || value.length > TOKEN_MAX_LEN) return "";
  if (!/^[A-Za-z0-9._~-]+$/.test(value)) return "";
  return value;
}

function tokenBindingKey(token) {
  return `token:${token}`;
}

function userTokenKey(uuid) {
  return `user-token:${uuid}`;
}

function readLogKey() {
  return "reads:log";
}

function writeLogKey() {
  return "writes:log";
}

function readThrottleKey(uuid) {
  return `reads:throttle:${uuid}`;
}

function writeThrottleKey(uuid) {
  return `writes:throttle:${uuid}`;
}

function ipRegisterKey(ip) {
  return `register:ip:${ip}`;
}

function normalizeIpKey(ip) {
  const s = String(ip || "").trim();
  if (!s) return "unknown";
  return s.replace(/[^0-9a-zA-Z\\-\\.:]/g, "_");
}

function generateUserWriteToken() {
  const bytes = new Uint8Array(24);
  crypto.getRandomValues(bytes);
  return Array.from(bytes, b => b.toString(16).padStart(2, "0")).join("");
}

async function readIndex(env) {
  const raw = await env.PRESENCE_KV.get("users:index");
  if (!raw) return {};
  try {
    const parsed = JSON.parse(raw);
    return parsed && typeof parsed === "object" ? parsed : {};
  } catch {
    return {};
  }
}

async function writeIndex(env, indexObj) {
  await env.PRESENCE_KV.put("users:index", JSON.stringify(indexObj));
}

async function readUserTokenRecord(env, uuid) {
  const raw = await env.PRESENCE_KV.get(userTokenKey(uuid));
  if (!raw) return null;
  try {
    const parsed = JSON.parse(raw);
    if (parsed && typeof parsed === "object" && normalizeToken(parsed.token)) {
      return parsed;
    }
  } catch {
  }
  const legacy = normalizeToken(raw);
  if (!legacy) return null;
  return { token: legacy, createdAt: null, updatedAt: null };
}

async function writeUserTokenRecord(env, uuid, token, nowIso, createdAt = null) {
  const record = {
    token,
    createdAt: createdAt || nowIso,
    updatedAt: nowIso
  };
  await env.PRESENCE_KV.put(userTokenKey(uuid), JSON.stringify(record), { expirationTtl: USER_RECORD_TTL_SECONDS });
}

async function appendReadLog(env, uuid, name, readAtIso) {
  const raw = await env.PRESENCE_KV.get(readLogKey());
  let logs = [];
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        logs = parsed;
      }
    } catch {
    }
  }
  logs.push({ uuid, name, readAt: readAtIso });
  if (logs.length > READ_LOG_MAX_ENTRIES) {
    logs = logs.slice(logs.length - READ_LOG_MAX_ENTRIES);
  }
  await env.PRESENCE_KV.put(readLogKey(), JSON.stringify(logs));
}

async function appendWriteLog(env, uuid, name, writeAtIso) {
  const raw = await env.PRESENCE_KV.get(writeLogKey());
  let logs = [];
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      if (Array.isArray(parsed)) {
        logs = parsed;
      }
    } catch {
    }
  }
  logs.push({ uuid, name, writeAt: writeAtIso });
  if (logs.length > WRITE_LOG_MAX_ENTRIES) {
    logs = logs.slice(logs.length - WRITE_LOG_MAX_ENTRIES);
  }
  await env.PRESENCE_KV.put(writeLogKey(), JSON.stringify(logs));
}

async function shouldLog(env, throttleKey, dedupSeconds) {
  const exists = await env.PRESENCE_KV.get(throttleKey);
  if (exists) return false;
  await env.PRESENCE_KV.put(throttleKey, "1", { expirationTtl: dedupSeconds });
  return true;
}

async function popLog(env, keyFn) {
  const key = keyFn();
  const raw = await env.PRESENCE_KV.get(key);
  let logs = [];
  if (raw) {
    try {
      const parsed = JSON.parse(raw);
      logs = Array.isArray(parsed) ? parsed : [];
    } catch {
      logs = [];
    }
  }
  await env.PRESENCE_KV.delete(key);
  return logs;
}

async function deleteByPrefix(env, prefix) {
  let deleted = 0;
  let cursor = undefined;
  while (true) {
    const res = await env.PRESENCE_KV.list({ prefix, limit: 1000, cursor });
    const keys = res && Array.isArray(res.keys) ? res.keys : [];
    for (const k of keys) {
      if (!k || !k.name) continue;
      await env.PRESENCE_KV.delete(k.name);
      deleted++;
    }
    cursor = res && res.cursor ? res.cursor : undefined;
    if (!cursor) break;
  }
  return deleted;
}

export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const adminToken = env.BAITY_ADMIN_TOKEN || "";
    const readToken = env.BAITY_READ_TOKEN || DEFAULT_READ_TOKEN;
    const providedAdminTokenHeader = request.headers.get("x-baity-admin-token") || "";
    const isAdminRequest = !!adminToken && providedAdminTokenHeader === adminToken;

    if (request.method === "GET" && path === "/health") {
      return json({ ok: true, ts: Date.now() });
    }

    if (request.method === "GET" && path === "/admin/verify") {
      if (!isAdminRequest) return json({ ok: false, error: "unauthorized" }, 401);
      return json({ ok: true });
    }

    if (request.method === "POST" && path === "/admin/reads-log") {
      if (!isAdminRequest) return json({ ok: false, error: "unauthorized" }, 401);
      const logs = await popLog(env, readLogKey);
      return json({ ok: true, logs });
    }

    if (request.method === "POST" && path === "/admin/writes-log") {
      if (!isAdminRequest) return json({ ok: false, error: "unauthorized" }, 401);
      const logs = await popLog(env, writeLogKey);
      return json({ ok: true, logs });
    }

    if (request.method === "POST" && path === "/admin/clear-all") {
      if (!isAdminRequest) return json({ ok: false, error: "unauthorized" }, 401);

      const users = await deleteByPrefix(env, "user:");
      const userTokens = await deleteByPrefix(env, "user-token:");
      const tokens = await deleteByPrefix(env, "token:");
      const registers = await deleteByPrefix(env, "register:");
      await env.PRESENCE_KV.delete("users:index");

      return json({
        ok: true,
        users,
        userTokens,
        tokens,
        registers
      });
    }

    if (request.method === "POST" && path === "/admin/token-bind") {
      const providedAdminToken = request.headers.get("x-baity-admin-token") || "";
      if (!adminToken || providedAdminToken !== adminToken) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }
      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }
      const uuid = normalizeUuid(body?.uuid);
      const token = normalizeToken(body?.token);
      if (!uuid || !token) {
        return json({ ok: false, error: "invalid_bind_payload" }, 400);
      }
      await env.PRESENCE_KV.put(tokenBindingKey(token), uuid, { expirationTtl: USER_RECORD_TTL_SECONDS });
      return json({ ok: true, uuid });
    }

    if (request.method === "POST" && path === "/admin/token-unbind") {
      const providedAdminToken = request.headers.get("x-baity-admin-token") || "";
      if (!adminToken || providedAdminToken !== adminToken) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }
      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }
      const token = normalizeToken(body?.token);
      if (!token) {
        return json({ ok: false, error: "invalid_unbind_payload" }, 400);
      }
      await env.PRESENCE_KV.delete(tokenBindingKey(token));
      return json({ ok: true });
    }

    if (request.method === "POST" && path === "/register") {
      const accessToken = request.headers.get("x-baity-token") || "";
      if (!readToken || accessToken !== readToken) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }
      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }
      const uuid = normalizeUuid(body?.uuid);
      if (!uuid) {
        return json({ ok: false, error: "invalid_uuid" }, 400);
      }

      const ipRaw =
        request.headers.get("cf-connecting-ip") ||
        request.headers.get("x-forwarded-for") ||
        "";
      const ipKey = normalizeIpKey(ipRaw.split(",")[0]);

      const ipRecordRaw = await env.PRESENCE_KV.get(ipRegisterKey(ipKey));
      let ipRecord = { uuids: [] };
      if (ipRecordRaw) {
        try {
          const parsed = JSON.parse(ipRecordRaw);
          if (parsed && Array.isArray(parsed.uuids)) {
            ipRecord.uuids = parsed.uuids;
          }
        } catch {
        }
      }

      const nowIso = new Date().toISOString();
      const existingRecord = await readUserTokenRecord(env, uuid);
      if (existingRecord && normalizeToken(existingRecord.token)) {
        const boundUuid = await env.PRESENCE_KV.get(tokenBindingKey(existingRecord.token));
        if (boundUuid === uuid) {
          await writeUserTokenRecord(env, uuid, existingRecord.token, nowIso, existingRecord.createdAt);
          await env.PRESENCE_KV.put(tokenBindingKey(existingRecord.token), uuid, { expirationTtl: USER_RECORD_TTL_SECONDS });
          if (!ipRecord.uuids.includes(uuid)) {
            ipRecord.uuids.push(uuid);
          }
          while (ipRecord.uuids.length > REGISTER_IP_MAX) {
            ipRecord.uuids.shift();
          }
          await env.PRESENCE_KV.put(ipRegisterKey(ipKey), JSON.stringify({ uuids: ipRecord.uuids }), { expirationTtl: REGISTER_IP_WINDOW_SECONDS });
          return json({ ok: true, token: existingRecord.token });
        }
      }

      if (!ipRecord.uuids.includes(uuid)) {
        while (ipRecord.uuids.length >= REGISTER_IP_MAX) {
          const evictUuid = ipRecord.uuids.shift();
          if (!evictUuid) break;
          const evictRecord = await readUserTokenRecord(env, evictUuid);
          if (evictRecord && normalizeToken(evictRecord.token)) {
            await env.PRESENCE_KV.delete(tokenBindingKey(evictRecord.token));
          }
          await env.PRESENCE_KV.delete(userTokenKey(evictUuid));
        }
        ipRecord.uuids.push(uuid);
      }
      await env.PRESENCE_KV.put(
        ipRegisterKey(ipKey),
        JSON.stringify({ uuids: ipRecord.uuids }),
        { expirationTtl: REGISTER_IP_WINDOW_SECONDS }
      );

      const token = generateUserWriteToken();
      await writeUserTokenRecord(env, uuid, token, nowIso);
      await env.PRESENCE_KV.put(tokenBindingKey(token), uuid, { expirationTtl: USER_RECORD_TTL_SECONDS });
      return json({ ok: true, token });
    }

    if (request.method === "POST" && path === "/report") {
      const writeToken = request.headers.get("x-baity-token") || "";

      let body;
      try {
        body = await request.json();
      } catch {
        return json({ ok: false, error: "invalid_json" }, 400);
      }

      const sanitized = sanitizeUserPayload(body);
      if (!sanitized) return json({ ok: false, error: "invalid_payload" }, 400);
      const normalizedToken = normalizeToken(writeToken);
      const tokenBoundUuid = normalizedToken ? await env.PRESENCE_KV.get(tokenBindingKey(normalizedToken)) : null;
      if (tokenBoundUuid !== sanitized.uuid) {
        return json({ ok: false, error: "unauthorized" }, 401);
      }

      const key = `user:${sanitized.uuid}`;
      await env.PRESENCE_KV.put(key, JSON.stringify(sanitized), { expirationTtl: USER_RECORD_TTL_SECONDS });

      const indexObj = await readIndex(env);
      indexObj[sanitized.uuid] = { name: sanitized.name, lastSeenAt: sanitized.meta.lastSeenAt };
      await writeIndex(env, indexObj);

      try {
        const doLog = await shouldLog(env, writeThrottleKey(sanitized.uuid), WRITE_LOG_DEDUP_SECONDS);
        if (doLog) {
          await appendWriteLog(env, sanitized.uuid, sanitized.name, new Date().toISOString());
        }
      } catch {
      }

      return json({ ok: true });
    }

    if (request.method === "GET" && path === "/users.json") {
      const indexObj = await readIndex(env);
      let requestUuid = "";
      if (!isAdminRequest) {
        const accessToken = request.headers.get("x-baity-token") || "";
        if (!readToken || accessToken !== readToken) {
          return json({ ok: false, error: "unauthorized" }, 401);
        }
        requestUuid = normalizeUuid(request.headers.get("x-baity-uuid") || "");
        if (!requestUuid) {
          return json({ ok: false, error: "invalid_uuid" }, 400);
        }
        const doLog = await shouldLog(env, readThrottleKey(requestUuid), READ_LOG_DEDUP_SECONDS);
        if (doLog) {
          const entry = indexObj[requestUuid];
          const requestName = entry && typeof entry.name === "string" ? entry.name : "";
          await appendReadLog(env, requestUuid, requestName, new Date().toISOString());
        }
      }
      const uuids = Object.keys(indexObj);
      const users = {};
      const now = Date.now();

      for (const uuid of uuids) {
        const raw = await env.PRESENCE_KV.get(`user:${uuid}`);
        if (!raw) {
          delete indexObj[uuid];
          continue;
        }
        try {
          const entry = JSON.parse(raw);
          const lastSeen = Date.parse(entry?.meta?.lastSeenAt || "");
          if (!Number.isFinite(lastSeen)) {
            delete indexObj[uuid];
            continue;
          }
          const elapsedMs = now - lastSeen;
          if (elapsedMs > USER_HARD_EXPIRE_MS) {
            delete indexObj[uuid];
            continue;
          }
          const stale = elapsedMs > USER_SOFT_STALE_MS;
          if (!entry.meta || typeof entry.meta !== "object") {
            entry.meta = {};
          }
          entry.meta.lastSeenAt = new Date(lastSeen).toISOString();
          entry.meta.stale = stale;
          users[uuid] = entry;
        } catch {
          delete indexObj[uuid];
        }
      }

      await writeIndex(env, indexObj);

      let reads = undefined;
      let writes = undefined;
      if (isAdminRequest) {
        const raw = await env.PRESENCE_KV.get(readLogKey());
        if (raw) {
          try {
            const parsed = JSON.parse(raw);
            reads = Array.isArray(parsed) ? parsed : [];
          } catch {
            reads = [];
          }
        } else {
          reads = [];
        }

        const wraw = await env.PRESENCE_KV.get(writeLogKey());
        if (wraw) {
          try {
            const parsed = JSON.parse(wraw);
            writes = Array.isArray(parsed) ? parsed : [];
          } catch {
            writes = [];
          }
        } else {
          writes = [];
        }
      }

      return json({
        version: 1,
        updatedAt: new Date().toISOString(),
        users,
        ...(isAdminRequest ? { reads, writes } : {})
      }, 200, { "cache-control": "public, max-age=15" });
    }

    return json({ ok: false, error: "not_found" }, 404);
  }
};