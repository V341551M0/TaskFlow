package util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class TokenService {
    private static final String SECRET = getEnvOrDefault("JWT_SECRET", "taskflow-dev-secret-mude-em-producao-2026");
    private static final long VALIDITY_MS = 24L * 60 * 60 * 1000;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();

    private TokenService() {
    }

    public static String generate(String userId) {
        long now = System.currentTimeMillis();
        String header = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");
        String payload = base64Url("{\"sub\":\"" + userId + "\",\"iat\":" + now / 1000
                + ",\"exp\":" + (now + VALIDITY_MS) / 1000 + "}");
        String signature = sign(header + "." + payload);
        return header + "." + payload + "." + signature;
    }

    public static String validate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return null;
        }
        String header = parts[0];
        String payload = parts[1];
        String signature = parts[2];

        if (!constantTimeEquals(signature, sign(header + "." + payload))) {
            return null;
        }

        String json = new String(URL_DECODER.decode(payload), StandardCharsets.UTF_8);
        long exp = extractExp(json);
        if (exp == 0 || exp * 1000 <= System.currentTimeMillis()) {
            return null;
        }
        return extractSubject(json);
    }

    private static String sign(String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return URL_ENCODER.encodeToString(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível assinar o token", ex);
        }
    }

    private static String base64Url(String json) {
        return URL_ENCODER.encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    private static long extractExp(String json) {
        return extractLong(json, "\"exp\":");
    }

    private static String extractSubject(String json) {
        String value = extractString(json, "\"sub\":");
        return value == null ? null : value;
    }

    private static long extractLong(String json, String prefix) {
        int index = json.indexOf(prefix);
        if (index < 0) {
            return 0;
        }
        int start = index + prefix.length();
        if (start >= json.length()) {
            return 0;
        }
        if (json.charAt(start) == '"') {
            int end = json.indexOf('"', start + 1);
            if (end < 0) {
                return 0;
            }
            try {
                return Long.parseLong(json.substring(start + 1, end));
            } catch (NumberFormatException ex) {
                return 0;
            }
        }
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') {
            end++;
        }
        try {
            return Long.parseLong(json.substring(start, end));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String extractString(String json, String prefix) {
        int index = json.indexOf(prefix);
        if (index < 0) {
            return null;
        }
        int start = index + prefix.length();
        if (start >= json.length() || json.charAt(start) != '"') {
            return null;
        }
        int end = json.indexOf('"', start + 1);
        if (end < 0) {
            return null;
        }
        return json.substring(start + 1, end);
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqualSafe(a, b);
    }

    private static String getEnvOrDefault(String name, String defaultValue) {
        String value = System.getenv(name);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    private static final class MessageDigest {
        static boolean isEqualSafe(String a, String b) {
            if (a == null || b == null) {
                return false;
            }
            byte[] ba = a.getBytes(StandardCharsets.UTF_8);
            byte[] bb = b.getBytes(StandardCharsets.UTF_8);
            return java.security.MessageDigest.isEqual(ba, bb);
        }
    }
}