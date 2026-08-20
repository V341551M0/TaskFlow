package util;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class Json {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private Json() {
    }

    public static Map<String, Object> parseObject(String body) {
        if (body == null || body.isBlank()) {
            return Map.of();
        }
        try {
            return OBJECT_MAPPER.readValue(body, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception ex) {
            throw new IllegalArgumentException("JSON inválido", ex);
        }
    }

    public static String toJson(Object body) {
        try {
            return OBJECT_MAPPER.writeValueAsString(body);
        } catch (Exception ex) {
            throw new IllegalStateException("Não foi possível serializar a resposta", ex);
        }
    }
}