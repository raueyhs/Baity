package com.shyeuar.baity.sync;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;

@Environment(EnvType.CLIENT)
final class SyncClient {

    private static final Logger LOGGER = LoggerFactory.getLogger("Baity/SyncClient");
    private static final int CONNECT_TIMEOUT_MS = 8_000;
    private static final int READ_TIMEOUT_MS = 15_000;

    private static volatile String lastFailure = "";

    record AuthResult(String token, long expiresAt) {
    }

    record SyncResult(boolean changed, long version, String usersUrl) {
    }

    private SyncClient() {
    }

    static String lastFailure() {
        return lastFailure;
    }

    static String health(String baseUrl) {
        JsonObject json = parse(request("GET", joinUrl(baseUrl, "/health"), null, null));
        if (json == null || !json.has("usersUrl")) {
            return "";
        }
        return json.get("usersUrl").getAsString();
    }

    static AuthResult authenticate(String baseUrl, String uuid, String name, String serverId) {
        JsonObject body = new JsonObject();
        body.addProperty("uuid", uuid);
        body.addProperty("name", name);
        body.addProperty("serverId", serverId);
        String raw = request("POST", joinUrl(baseUrl, "/auth"), null, body.toString());
        JsonObject json = parse(raw);
        if (json == null || !json.has("token")) {
            LOGGER.warn("[sync] auth failed, body={}", abbreviate(raw));
            lastFailure = "POST /auth -> rejected or malformed response: " + abbreviate(raw);
            return null;
        }
        long expiresAt = json.has("expiresAt") ? json.get("expiresAt").getAsLong() : 0L;
        return new AuthResult(json.get("token").getAsString(), expiresAt);
    }

    static SyncResult sync(String baseUrl, String token, String name, String appearanceJson) {
        JsonObject body = new JsonObject();
        body.addProperty("name", name);
        if (appearanceJson != null && !appearanceJson.isBlank()) {
            body.add("appearance", JsonParser.parseString(appearanceJson));
        }
        String raw = request("POST", joinUrl(baseUrl, "/sync"), token, body.toString());
        JsonObject json = parse(raw);
        if (json == null || !json.has("version")) {
            LOGGER.warn("[sync] sync failed, body={}", abbreviate(raw));
            lastFailure = "POST /sync -> rejected or malformed response: " + abbreviate(raw);
            return null;
        }
        return new SyncResult(
                json.has("changed") && json.get("changed").getAsBoolean(),
                json.get("version").getAsLong(),
                json.has("usersUrl") ? json.get("usersUrl").getAsString() : "");
    }

    static String fetch(String url) {
        return request("GET", url, null, null);
    }

    private static String joinUrl(String baseUrl, String path) {
        String base = baseUrl == null ? "" : baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + path;
    }

    private static JsonObject parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return JsonParser.parseString(raw).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }

    private static String abbreviate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }

    private static String request(String method, String url, String token, String body) {
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
            connection.setRequestMethod(method);
            connection.setConnectTimeout(CONNECT_TIMEOUT_MS);
            connection.setReadTimeout(READ_TIMEOUT_MS);
            connection.setUseCaches(false);
            connection.setRequestProperty("Accept", "application/json");
            if (token != null && !token.isBlank()) {
                connection.setRequestProperty("Authorization", "Bearer " + token);
            }
            if (body != null) {
                byte[] payload = body.getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(payload.length);
                try (OutputStream out = connection.getOutputStream()) {
                    out.write(payload);
                }
            }
            int code = connection.getResponseCode();
            InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
            String raw = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            if (code < 200 || code >= 300) {
                LOGGER.warn("[sync] {} {} -> {} {}", method, url, code, abbreviate(raw));
                lastFailure = method + " " + url + " -> HTTP " + code + " " + abbreviate(raw);
                return null;
            }
            return raw;
        } catch (Exception e) {
            LOGGER.warn("[sync] {} {} failed: {}", method, url, e.toString());
            lastFailure = method + " " + url + " failed: " + e;
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
