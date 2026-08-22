package dto;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;

/**
 * Payload de entrada para criação de itens (task/habit/recurring).
 * Centraliza a extração e validação dos campos recebidos da API,
 * normalizando os nomes de chave usados pelo frontend e pelo backend.
 */
public final class ItemRequest {
    private final String name;
    private final LocalDate date;
    private final boolean allDays;
    private final int frequencyPerDay;

    private ItemRequest(String name, LocalDate date, boolean allDays, int frequencyPerDay) {
        this.name = name;
        this.date = date;
        this.allDays = allDays;
        this.frequencyPerDay = frequencyPerDay;
    }

    public static ItemRequest from(Map<String, Object> data) {
        String name = stringValue(data.getOrDefault("nome", data.getOrDefault("name", ""))).trim();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Informe o nome da atividade.");
        }

        String dateValue = stringValue(data.getOrDefault("data", data.getOrDefault("date", "")));
        LocalDate date = dateValue.isBlank() ? LocalDate.now() : parseDate(dateValue);

        boolean allDays = booleanValue(data.getOrDefault("todosOsDias", data.getOrDefault("allDays", false)));

        int frequency = parseFrequency(stringValue(data.getOrDefault("vezesAoDia", data.getOrDefault("frequencyPerDay", "1"))));
        if (frequency < 1) {
            throw new IllegalArgumentException("Informe uma frequência válida (número de vezes por dia).");
        }

        return new ItemRequest(name, date, allDays, frequency);
    }

    public static LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Data inválida: " + value);
        }
    }

    public String name() {
        return name;
    }

    public LocalDate date() {
        return date;
    }

    public boolean allDays() {
        return allDays;
    }

    public int frequencyPerDay() {
        return frequencyPerDay;
    }

    private static int parseFrequency(String frequency) {
        try {
            int value = Integer.parseInt(frequency);
            return Math.max(1, value);
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private static boolean booleanValue(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Boolean bool) {
            return bool;
        }
        return "true".equalsIgnoreCase(String.valueOf(value));
    }

    private static String stringValue(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        return String.valueOf(value);
    }
}