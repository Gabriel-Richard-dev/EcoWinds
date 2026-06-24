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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Parser heurístico para exportação bruta do SIGEHO.
 *
 * Formato esperado (melhor palpite até haver amostra real):
 * array de aulas no topo, cada entrada com sala + horário.
 * Variantes aceitas via fallbacks de chave; ajustar conforme
 * a estrutura real do arquivo SIGEHO for conhecida.
 *
 * Exemplo de entrada aceita:
 * [
 *   { "sala": "LAB01", "bloco": "B1", "disciplina": "Algoritmos",
 *     "diaSemana": "Segunda", "horarioInicio": "07:00", "horarioFim": "09:00" },
 *   ...
 * ]
 * ou encapsulado em "aulas" / "turmas" / "data".
 */
@Component
@Order(100)
public class SigehoNativeParser implements SigehoParser {

    @Override
    public boolean supports(JsonNode root) {
        JsonNode arr = entriesArray(root);
        if (arr == null || !arr.isArray() || arr.isEmpty()) return false;
        JsonNode first = arr.get(0);
        return first.has("sala") || first.has("disciplina") || first.has("diaSemana")
                || first.has("horarioInicio") || first.has("turma");
    }

    @Override
    public ParsedImport parse(JsonNode root) {
        JsonNode arr = entriesArray(root);
        Map<String, RoomAccumulator> byRoom = new LinkedHashMap<>();

        int idx = 0;
        for (JsonNode entry : arr) {
            idx++;
            String roomId = text(entry, "sala", "salaIdentificacao", "ambiente", "local", "room");
            if (roomId == null || roomId.isBlank()) {
                throw new IllegalArgumentException("Entry #" + idx + " missing room identifier");
            }
            String block = text(entry, "bloco", "block", "predio");
            String weekdayRaw = text(entry, "diaSemana", "dia", "weekday");
            DayOfWeek weekday = WeekdayMapper.parse(weekdayRaw);
            if (weekday == null) {
                throw new IllegalArgumentException("Entry #" + idx + " invalid weekday '" + weekdayRaw + "'");
            }
            LocalTime start = parseTime(text(entry, "horarioInicio", "inicio", "startTime", "horaInicio"));
            LocalTime end = parseTime(text(entry, "horarioFim", "fim", "endTime", "horaFim"));
            if (start == null || end == null) {
                throw new IllegalArgumentException("Entry #" + idx + " missing start/end time");
            }
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("Entry #" + idx + " endTime must be after startTime");
            }
            String course = text(entry, "disciplina", "course", "materia", "turma");

            RoomAccumulator acc = byRoom.computeIfAbsent(roomId.trim(),
                    k -> new RoomAccumulator(k, block == null ? null : block.trim()));
            if (acc.block == null && block != null) acc.block = block.trim();
            acc.schedules.add(new ParsedSchedule(weekday, start, end, course == null ? "" : course.trim()));
        }

        List<ParsedRoom> rooms = new ArrayList<>(byRoom.size());
        for (RoomAccumulator a : byRoom.values()) {
            rooms.add(new ParsedRoom(a.id, a.block, a.schedules));
        }
        return new ParsedImport(rooms);
    }

    private JsonNode entriesArray(JsonNode root) {
        if (root == null) return null;
        if (root.isArray()) return root;
        for (String wrapper : new String[]{"aulas", "turmas", "data", "horarios", "schedules"}) {
            JsonNode n = root.get(wrapper);
            if (n != null && n.isArray()) return n;
        }
        return null;
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
            if (v != null && !v.isNull() && v.isValueNode()) return v.asText();
        }
        return null;
    }

    private static final class RoomAccumulator {
        final String id;
        String block;
        final List<ParsedSchedule> schedules = new ArrayList<>();
        RoomAccumulator(String id, String block) { this.id = id; this.block = block; }
    }
}
