package util;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dto.TaskDto;

public final class Json {

    private Json() {
    }

    public static Map<String, String> parseObject(String body) {
        Map<String, String> values = new HashMap<>();
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            String inner = trimmed.substring(1, trimmed.length() - 1).trim();
            if (!inner.isEmpty()) {
                for (String part : inner.split(",")) {
                    String[] keyValue = part.split(":", 2);
                    if (keyValue.length != 2) {
                        continue;
                    }
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim();
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1);
                    }
                    values.put(key, value);
                }
            }
        }
        return values;
    }

    public static String toJson(Object body) {
        if (body instanceof String s) {
            return "\"" + escape(s) + "\"";
        }
        if (body instanceof Number || body instanceof Boolean) {
            return String.valueOf(body);
        }
        if (body instanceof Map<?, ?> map) {
            StringBuilder builder = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    builder.append(",");
                }
                builder.append("\"").append(escape(String.valueOf(entry.getKey()))).append("\":")
                        .append(toJson(entry.getValue()));
                first = false;
            }
            return builder.append("}").toString();
        }
        if (body instanceof List<?> list) {
            StringBuilder builder = new StringBuilder("[");
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    builder.append(",");
                }
                builder.append(toJson(item));
                first = false;
            }
            return builder.append("]").toString();
        }
        if (body instanceof TaskDto dto) {
            return "{\"id\":\"" + escape(dto.getId()) + "\",\"name\":\"" + escape(dto.getName()) + "\",\"date\":\"" + escape(dto.getDate()) + "\",\"allDays\":" + dto.isAllDays() + ",\"frequencyPerDay\":\"" + escape(dto.getFrequencyPerDay()) + "\",\"type\":\"" + escape(dto.getType()) + "\",\"completedToday\":" + dto.isCompletedToday() + ",\"completionCount\":" + dto.getCompletionCount() + ",\"status\":\"" + escape(dto.getStatus()) + "\"}";
        }
        return "\"\"";
    }

    public static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
