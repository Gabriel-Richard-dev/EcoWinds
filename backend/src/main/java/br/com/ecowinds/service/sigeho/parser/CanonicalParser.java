package br.com.ecowinds.service.sigeho.parser;

import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import br.com.ecowinds.service.sigeho.dto.ParsedRoom;
import br.com.ecowinds.service.sigeho.dto.ParsedSchedule;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Formato canônico interno:
 * { "rooms": [ { "id": "LAB01", "name": "...", "block": "B1",
 *   "schedules": [ { "weekday": "MONDAY", "startTime": "07:00", "endTime": "09:00", "course": "..." } ] } ] }
 */
@Component
@Order(0)
public class CanonicalParser implements SigehoParser {

    @Override
    public boolean supports(JsonNode root) {
        return root != null && root.has("rooms") && root.get("rooms").isArray();
    }

    @Override
    public ParsedImport parse(JsonNode root) {
        List<ParsedRoom> rooms = new ArrayList<>();
        for (JsonNode r : root.get("rooms")) {
            String id = text(r, "id", "identification");
            if (id == null || id.isBlank()) {
                throw new IllegalArgumentException("Room without id/identification");
            }
            String block = text(r, "block", "bloco");
            List<ParsedSchedule> schedules = new ArrayList<>();
            JsonNode sched = r.get("schedules");
            if (sched != null && sched.isArray()) {
                for (JsonNode s : sched) {
                    schedules.add(parseSchedule(s, id));
                }
            }
            rooms.add(new ParsedRoom(id.trim(), block == null ? null : block.trim(), schedules));
        }
        return new ParsedImport(rooms);
    }

    private ParsedSchedule parseSchedule(JsonNode s, String roomId) {
        String weekdayRaw = text(s, "weekday", "dayOfWeek", "diaSemana");
        DayOfWeek weekday = WeekdayMapper.parse(weekdayRaw);
        if (weekday == null) {
            throw new IllegalArgumentException("Invalid weekday '" + weekdayRaw + "' in room " + roomId);
        }
        LocalTime start = parseTime(text(s, "startTime", "horarioInicio", "inicio"));
        LocalTime end = parseTime(text(s, "endTime", "horarioFim", "fim"));
        if (start == null || end == null) {
            throw new IllegalArgumentException("Missing start/end time in room " + roomId);
        }
        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("endTime must be after startTime in room " + roomId);
        }
        String course = text(s, "course", "disciplina", "nome");
        return new ParsedSchedule(weekday, start, end, course == null ? "" : course.trim());
    }

    private LocalTime parseTime(String raw) {
        if (raw == null) return null;
        String t = raw.trim();
        if (t.length() == 5) t = t + ":00";
        return LocalTime.parse(t);
    }

    private String text(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode v = node.get(k);
            if (v != null && !v.isNull() && v.isValueNode()) {
                return v.asText();
            }
        }
        return null;
    }
}
