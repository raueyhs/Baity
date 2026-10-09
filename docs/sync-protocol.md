# Baity Sync Protocol / Baity 远端同步协议

This document specifies the endpoints, authentication flow and data format used by Baity remote synchronization, and what data is exchanged.
本文档说明 Baity 远端同步所使用的接口、鉴权流程与数据格式，以及会交换哪些数据。

## 1. Endpoints / 接口

| Endpoint | Method | Auth | Request | Response |
|---|---|---|---|---|
| `/health` | GET | — | — | `ok`, `ts`, `usersUrl` |
| `/auth` | POST | — | `uuid`, `name`, `serverId` | `token`, `expiresAt` |
| `/sync` | POST | `Bearer <token>` | `name`, `appearance` | `version`, `changed`, `usersUrl` |

Error responses are JSON: `{"ok": false, "error": "<code>"}`. Codes include `invalid_uuid`, `invalid_name`, `session_challenge_required`, `session_verification_failed`, `unauthorized` and `not_found`.
错误响应为 JSON：`{"ok": false, "error": "<code>"}`。错误码包括 `invalid_uuid`、`invalid_name`、`session_challenge_required`、`session_verification_failed`、`unauthorized` 与 `not_found`。

## 2. Authentication / 鉴权

The client requests a session challenge from Mojang (`joinServer`), then calls `POST /auth` with the resulting `serverId`. The backend verifies it against `sessionserver.mojang.com/session/minecraft/hasJoined`; the returned UUID must match the claimed one. On success the backend issues a stateless, HMAC-signed token valid for 7 days. Only writes require this token — reading the shared data file is unauthenticated.

客户端先向 Mojang 申请会话挑战（`joinServer`），再带着得到的 `serverId` 调用 `POST /auth`。后端通过 `sessionserver.mojang.com/session/minecraft/hasJoined` 校验；返回的 UUID 必须与声明的 UUID 一致。校验通过后，后端签发一个无状态、HMAC 签名的令牌，有效期 7 天。只有写入需要该令牌 —— 读取共享数据文件无需鉴权。

## 3. Data format / 数据格式

All synchronized appearances live in a single shared file, `users.json`:

所有被同步的外观数据存放在同一个共享文件 `users.json` 中：

```json
{
  "version": 1730000000123,
  "updatedAt": 1730000000123,
  "users": {
    "<uuid>": {
      "name": "PlayerName",
      "appearance": {
        "isBaityUser": true,
        "features": { "smolPeople": { "enabled": false }, "nickTweaks": { "enabled": true } },
        "meta": { "lastSeenAt": "2026-10-09T08:00:00Z" }
      },
      "updatedAt": 1730000000000
    }
  }
}
```

An entry is dropped after 14 days without an update. `version` only advances when some entry’s appearance content actually changes, so a client can tell whether re-reading the file is worthwhile.
条目在 14 天无更新后会被移除。仅当某个条目的外观内容确实发生变化时 `version` 才推进，客户端据此判断是否有必要重新读取该文件。

## 4. Synchronization logic / 同步逻辑

    Baity's sync backend runs on Tencent CloudBase (Shanghai, mainland China) and stores the shared data file in Tencent COS object storage. The client reads the whole file once at game startup and again whenever the player runs the `/baity sync` command; there is no background polling. Writing goes through `POST /sync`, which merges the caller's own entry into the file.

    Baity 的同步后端部署在腾讯云开发 CloudBase（中国大陆上海），共享数据文件存放于腾讯云 COS 对象存储。客户端在游戏启动时读取整个文件一次，之后每次玩家执行 `/baity sync` 命令时再读取一次；没有后台轮询。写入通过 `POST /sync`，把调用者自己的条目合并进该文件。

## 5. User information fetched / 获取到的用户信息

    the backend returns only what is needed for Baity presence synchronization, including users’ UUID and Baity’s synchronized configuration (e.g. SmolPeople / NickTweaks).

    后端只返回用于 Baity 远端同步所需的数据，包括用户 UUID 以及 Baity 的同步配置（例如 SmolPeople / NickTweaks）。

## 6. Data visibility / 数据可见性

    the shared data file lives in a publicly readable bucket, so anyone who knows its URL can read every stored appearance. Writing always requires a valid account token, so uploads cannot impersonate another player.

    共享数据文件存放在公有读的存储桶中，任何知道其链接的人都可以读取其中全部外观数据。写入始终需要有效的账号令牌，因此无法冒充其他玩家。

## 7. Network / 网络

    the sync backend is hosted in mainland China, so a direct connection is sufficient and no proxy is required. Client-side proxy routes are not used for synchronization.

    同步后端部署在中国大陆，直连即可，不需要任何代理。客户端不会为同步使用代理线路。

## 8. Risk acceptance / 风险承担

    by using this mod, you agree that remote synchronization may involve privacy, network, and policy risks, and you take responsibility for any consequences at your own risk.

    使用本模组即表示你同意：远端同步可能涉及隐私、网络与合规等风险，并且你会对其产生的所有后果自担风险。
