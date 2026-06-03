package br.com.ecowinds.service.sigeho.parser;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.util.Map;

public final class WeekdayMapper {

    private static final Map<String, DayOfWeek> MAP = Map.ofEntries(
            Map.entry("monday", DayOfWeek.MONDAY),
            Map.entry("mon", DayOfWeek.MONDAY),
            Map.entry("segunda", DayOfWeek.MONDAY),
            Map.entry("segunda-feira", DayOfWeek.MONDAY),
            Map.entry("seg", DayOfWeek.MONDAY),
            Map.entry("2", DayOfWeek.MONDAY),

            Map.entry("tuesday", DayOfWeek.TUESDAY),
            Map.entry("tue", DayOfWeek.TUESDAY),
            Map.entry("terca", DayOfWeek.TUESDAY),
            Map.entry("terca-feira", DayOfWeek.TUESDAY),
            Map.entry("ter", DayOfWeek.TUESDAY),
            Map.entry("3", DayOfWeek.TUESDAY),

            Map.entry("wednesday", DayOfWeek.WEDNESDAY),
            Map.entry("wed", DayOfWeek.WEDNESDAY),
            Map.entry("quarta", DayOfWeek.WEDNESDAY),
            Map.entry("quarta-feira", DayOfWeek.WEDNESDAY),
            Map.entry("qua", DayOfWeek.WEDNESDAY),
            Map.entry("4", DayOfWeek.WEDNESDAY),

            Map.entry("thursday", DayOfWeek.THURSDAY),
            Map.entry("thu", DayOfWeek.THURSDAY),
            Map.entry("quinta", DayOfWeek.THURSDAY),
            Map.entry("quinta-feira", DayOfWeek.THURSDAY),
            Map.entry("qui", DayOfWeek.THURSDAY),
            Map.entry("5", DayOfWeek.THURSDAY),

            Map.entry("friday", DayOfWeek.FRIDAY),
            Map.entry("fri", DayOfWeek.FRIDAY),
            Map.entry("sexta", DayOfWeek.FRIDAY),
            Map.entry("sexta-feira", DayOfWeek.FRIDAY),
            Map.entry("sex", DayOfWeek.FRIDAY),
            Map.entry("6", DayOfWeek.FRIDAY),

            Map.entry("saturday", DayOfWeek.SATURDAY),
            Map.entry("sat", DayOfWeek.SATURDAY),
            Map.entry("sabado", DayOfWeek.SATURDAY),
            Map.entry("sab", DayOfWeek.SATURDAY),
            Map.entry("7", DayOfWeek.SATURDAY),

            Map.entry("sunday", DayOfWeek.SUNDAY),
            Map.entry("sun", DayOfWeek.SUNDAY),
            Map.entry("domingo", DayOfWeek.SUNDAY),
            Map.entry("dom", DayOfWeek.SUNDAY),
            Map.entry("1", DayOfWeek.SUNDAY)
    );

    private WeekdayMapper() {}

    public static DayOfWeek parse(String raw) {
        if (raw == null) return null;
        String key = Normalizer.normalize(raw.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase();
        return MAP.get(key);
    }
}
