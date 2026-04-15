package com.shyeuar.baity.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.text.Normalizer;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class VersionCheckUtils {
    private static final String GITHUB_API_URL = "https://api.github.com/repos/raueyhs/Baity/releases";
    private static final String MC_VERSION_PREFIX = "1.21.10";
    private static final Pattern MOD_VERSION_PATTERN = Pattern.compile("(?i)\\bv([0-9]+\\.[0-9]+\\.[0-9]+)\\b");
    
    public static class VersionCheckResult {
        public final boolean isLatest;
        public final String latestVersion;
        public final boolean hasError;
        
        public VersionCheckResult(boolean isLatest, String latestVersion, boolean hasError) {
            this.isLatest = isLatest;
            this.latestVersion = latestVersion;
            this.hasError = hasError;
        }
    }
    
    public static CompletableFuture<VersionCheckResult> checkVersionAsync(String currentVersion) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                URL url = URI.create(GITHUB_API_URL).toURL();
                
                if (url.getProtocol().equals("https")) {
                    javax.net.ssl.HttpsURLConnection httpsConnection = (javax.net.ssl.HttpsURLConnection) url.openConnection();
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                    httpsConnection.setSSLSocketFactory(createTrustAllSocketFactory());
                    httpsConnection.setRequestMethod("GET");
                    httpsConnection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    httpsConnection.setConnectTimeout(5000);
                    httpsConnection.setReadTimeout(5000);
                    
                    int responseCode = httpsConnection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return new VersionCheckResult(true, null, true);
                    }
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(httpsConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();

                    String latestVersion = findLatestMatchingReleaseVersion(jsonResponse);
                    if (latestVersion == null) {
                        return new VersionCheckResult(true, null, false);
                    }

                    return compareWithLatest(currentVersion, latestVersion);
                } else {
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/vnd.github.v3+json");
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000);
                
                    int responseCode = connection.getResponseCode();
                    if (responseCode != HttpURLConnection.HTTP_OK) {
                        return new VersionCheckResult(true, null, true);
                    }
                    
                    BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }
                    reader.close();
                    
                    String jsonResponse = response.toString();

                    String latestVersion = findLatestMatchingReleaseVersion(jsonResponse);
                    if (latestVersion == null) {
                        return new VersionCheckResult(true, null, false);
                    }

                    return compareWithLatest(currentVersion, latestVersion);
                }
                
            } catch (Exception e) {
                return new VersionCheckResult(true, null, true);
            }
        });
    }
    
    private static String extractVersionFromTag(String tagName) {
        if (tagName == null || tagName.isEmpty()) return null;
        
        Matcher matcher = MOD_VERSION_PATTERN.matcher(tagName);
        while (matcher.find()) {
            String version = matcher.group(1);
            if (version == null || version.isEmpty()) {
                continue;
            }
            return "v" + version;
        }
        
        return null;
    }

    private static VersionCheckResult compareWithLatest(String currentVersion, String latestVersion) {
        String normalizedCurrent = normalizeVersion(currentVersion);
        String normalizedLatest = normalizeVersion(latestVersion);

        int[] currentParts = parseVXYZ(normalizedCurrent);
        int[] latestParts = parseVXYZ(normalizedLatest);
        boolean isLatest;
        if (currentParts == null || latestParts == null) {
            isLatest = normalizedCurrent.equals(normalizedLatest);
        } else {
            isLatest = compareParts(currentParts, latestParts) >= 0;
        }
        return new VersionCheckResult(isLatest, latestVersion, false);
    }

    private static String findLatestMatchingReleaseVersion(String jsonResponse) {
        JsonElement root;
        try {
            root = JsonParser.parseString(jsonResponse);
        } catch (Exception e) {
            return null;
        }

        if (root == null || !root.isJsonArray()) return null;

        JsonArray releases = root.getAsJsonArray();
        int[] bestParts = null;
        String bestVersionRaw = null;

        for (JsonElement releaseEl : releases) {
            if (releaseEl == null || !releaseEl.isJsonObject()) continue;
            JsonObject releaseObj = releaseEl.getAsJsonObject();

            String releaseName = getStringOrNull(releaseObj, "name");
            String tagName = getStringOrNull(releaseObj, "tag_name");

            String normalizedReleaseName = normalizeForMatch(releaseName);
            String normalizedTagName = normalizeForMatch(tagName);

            if (normalizedReleaseName == null && normalizedTagName == null) continue;
            boolean releaseNameMatches = normalizedReleaseName != null && containsMcPrefix(normalizedReleaseName);
            boolean tagNameMatches = normalizedTagName != null && containsMcPrefix(normalizedTagName);
            if (!releaseNameMatches && !tagNameMatches) continue;

            String extracted = null;
            if (releaseNameMatches) extracted = extractVersionFromTag(normalizedReleaseName);
            if (extracted == null && tagNameMatches) extracted = extractVersionFromTag(normalizedTagName);
            if (extracted == null && normalizedReleaseName != null) extracted = extractVersionFromTag(normalizedReleaseName);
            if (extracted == null && normalizedTagName != null) extracted = extractVersionFromTag(normalizedTagName);
            if (extracted == null) continue;

            String normalizedLatest = normalizeVersion(extracted);
            int[] parts = parseVXYZ(normalizedLatest);
            if (parts == null) continue;

            if (bestParts == null || compareParts(parts, bestParts) > 0) {
                bestParts = parts;
                bestVersionRaw = extracted;
            }
        }

        return bestVersionRaw;
    }

    private static boolean containsMcPrefix(String value) {
        if (value == null) return false;
        String normalized = normalizeForMatch(value);
        if (normalized == null) return false;
        return normalized.startsWith(MC_VERSION_PREFIX + "-")
            || normalized.startsWith(MC_VERSION_PREFIX + "_")
            || normalized.startsWith(MC_VERSION_PREFIX + " ")
            || normalized.equals(MC_VERSION_PREFIX);
    }

    private static String normalizeForMatch(String value) {
        if (value == null) return null;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        normalized = normalized.replaceAll("[\\u200B-\\u200D\\uFEFF\\u2060]", "");
        normalized = normalized.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private static int[] parseVXYZ(String version) {
        if (version == null) return null;
        String v = version.trim();
        if (v.startsWith("v") || v.startsWith("V")) {
            v = v.substring(1);
        }
        String[] parts = v.split("\\.");
        if (parts.length != 3) return null;
        try {
            return new int[] {
                Integer.parseInt(parts[0]),
                Integer.parseInt(parts[1]),
                Integer.parseInt(parts[2])
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static int compareParts(int[] a, int[] b) {
        if (a == null || b == null) return 0;
        for (int i = 0; i < 3; i++) {
            int ai = a[i];
            int bi = b[i];
            if (ai != bi) return Integer.compare(ai, bi);
        }
        return 0;
    }

    private static String getStringOrNull(JsonObject obj, String key) {
        if (obj == null || key == null) return null;
        JsonElement el = obj.get(key);
        if (el == null || el.isJsonNull()) return null;
        try {
            return el.getAsString();
        } catch (Exception e) {
            return null;
        }
    }
    
    private static String normalizeVersion(String version) {
        if (version == null) return "";
        version = version.trim();
        if (version.startsWith("v") || version.startsWith("V")) {
            version = version.substring(1);
        }
        return version;
    }
    
    private static SSLSocketFactory createTrustAllSocketFactory() {
        try {
            TrustManager[] trustAllCerts = new TrustManager[] {
                new X509TrustManager() {
                    @Override
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                    @Override
                    public void checkClientTrusted(X509Certificate[] certs, String authType) { }
                    @Override
                    public void checkServerTrusted(X509Certificate[] certs, String authType) { }
                }
            };
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new SecureRandom());
            return sc.getSocketFactory();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}