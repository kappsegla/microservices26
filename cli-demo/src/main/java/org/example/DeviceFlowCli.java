package org.example;


import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class DeviceFlowCli {

    // --- Konfiguration ---
    static final String AUTH_SERVER   = "http://localhost:9000";
    static final String CLIENT_ID     = "cli-client";
    static final String CLIENT_SECRET = "secret";
    static final String SCOPE         = "openid read";
    static final int    POLL_INTERVAL = 5; // sekunder

    static final String DEVICE_AUTH_URL = AUTH_SERVER + "/oauth2/device_authorization";
    static final String TOKEN_URL       = AUTH_SERVER + "/oauth2/token";
    static final String GRANT_TYPE      = "urn:ietf:params:oauth:grant-type:device_code";

    static final HttpClient HTTP    = HttpClient.newHttpClient();
    static final ObjectMapper JSON  = new ObjectMapper();

    public static void main(String[] args) throws Exception {

        // ── Steg 1: Begär device code ─────────────────────────────────────
        System.out.println("Begär device-auktorisering...");
        JsonNode deviceResponse = postForm(DEVICE_AUTH_URL,"");
               // "scope=" + URLEncoder.encode(SCOPE, StandardCharsets.UTF_8));

        String deviceCode       = deviceResponse.get("device_code").asText();
        String userCode         = deviceResponse.get("user_code").asText();
        String verificationUri  = deviceResponse.get("verification_uri_complete").asText();
        int expiresIn           = deviceResponse.get("expires_in").asInt();

        // ── Steg 2: Be användaren logga in ───────────────────────────────
        System.out.println();
        System.out.println("┌───────────────────────────────────────────────────────────────────────┐");
        System.out.println("│  Öppna följande URL i din webbläsare:                                 │");
        System.out.printf( "│  %-69s│%n", verificationUri);
        System.out.printf( "│  Ange koden: %-57s│%n", userCode);
        System.out.println("└───────────────────────────────────────────────────────────────────────┘");
        System.out.println();

        // ── Steg 3: Polla token-endpointen ───────────────────────────────
        System.out.println("Väntar på att du godkänner åtkomst...");
        long deadline = System.currentTimeMillis() + (expiresIn * 1000L);

        while (System.currentTimeMillis() < deadline) {
            Thread.sleep(POLL_INTERVAL * 1000L);
            System.out.print(".");

            JsonNode tokenResponse = postForm(TOKEN_URL,
                    "grant_type=" + URLEncoder.encode(GRANT_TYPE, StandardCharsets.UTF_8)
                    + "&device_code=" + URLEncoder.encode(deviceCode, StandardCharsets.UTF_8));

            // Kontrollera om vi fick ett fel eller ett token
            if (tokenResponse.has("error")) {
                String error = tokenResponse.get("error").asText();
                switch (error) {
                    case "authorization_pending" -> { /* fortsätt polla */ }
                    case "slow_down"             -> Thread.sleep(5000); // polla mer sällan
                    case "access_denied"         -> { System.out.println("\nÅtkomst nekad av användaren."); return; }
                    case "expired_token"          -> { System.out.println("\nDevice code har gått ut. Starta om."); return; }
                    default                      -> { System.out.println("\nOkänt fel: " + error); return; }
                }
            } else {
                // ── Steg 4: Vi fick ett token! ────────────────────────────
                System.out.println("\n\nLyckades! Token mottaget:\n");
                System.out.println("Access token : " + tokenResponse.get("access_token").asText());
                if (tokenResponse.has("refresh_token")) {
                    System.out.println("Refresh token: " + tokenResponse.get("refresh_token").asText());
                }
                System.out.println("Token type   : " + tokenResponse.get("token_type").asText());
                System.out.println("Expires in   : " + tokenResponse.get("expires_in").asText() + "s");
                if (tokenResponse.has("scope")) {
                    System.out.println("Scope        : " + tokenResponse.get("scope").asText());
                }
                return;
            }
        }

        System.out.println("\nTimeout – ingen auktorisering mottagen inom " + expiresIn + " sekunder.");
    }

    /**
     * Skickar ett HTTP POST med application/x-www-form-urlencoded body
     * och Basic auth-header, returnerar JSON-svaret.
     */
    static JsonNode postForm(String url, String body) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
                (CLIENT_ID + ":" + CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " + credentials)
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        return JSON.readTree(response.body());
    }
}
