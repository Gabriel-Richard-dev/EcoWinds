package br.com.ecowinds.service.sigeho.parser;

import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import br.com.ecowinds.service.sigeho.dto.ParsedRoom;
import br.com.ecowinds.service.sigeho.dto.ParsedSchedule;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Interpreta a página HTML de horários de curso do SIGEHO em um {@link ParsedImport}.
 *
 * A página contém uma tabela Bootstrap por período letivo.
 * A primeira linha de cada tabela é o cabeçalho dos dias da semana; as linhas seguintes trazem
 * uma célula de horário seguida de uma célula de disciplina por dia.
 * Células ocupadas usam {@code bg-white}; vazias usam {@code bg-light}.
 */
@Component
public class SigehoHtmlParser {

    private static final Logger log = LoggerFactory.getLogger(SigehoHtmlParser.class);
    private static final Pattern TIME_RANGE = Pattern.compile("(\\d{2}:\\d{2})\\s*-\\s*(\\d{2}:\\d{2})");
    private static final Pattern BLOCK_PATTERN = Pattern.compile("(?i)bl[.\\s]*(\\S+)");

    public ParsedImport parse(byte[] html) {
        Document doc = Jsoup.parse(new String(html, StandardCharsets.UTF_8));
        Elements tables = doc.select("table.tablePrint");

        Map<String, RoomAccumulator> byRoom = new LinkedHashMap<>();
        int tableCount = 0;

        for (Element table : tables) {
            Elements rows = table.select("tr");
            if (rows.size() < 2) continue;

            List<DayOfWeek> days = extractDays(rows.first());
            if (days.isEmpty()) continue;
            tableCount++;

            for (int r = 1; r < rows.size(); r++) {
                Elements cells = rows.get(r).select("td");
                if (cells.isEmpty()) continue;

                // Primeira célula = faixa de horário
                String timeText = cells.first().select("b").text().trim();
                Matcher m = TIME_RANGE.matcher(timeText);
                if (!m.find()) continue;

                LocalTime startTime = LocalTime.parse(m.group(1));
                LocalTime endTime   = LocalTime.parse(m.group(2));

                for (int c = 1; c < cells.size() && (c - 1) < days.size(); c++) {
                    Element cell = cells.get(c);
                    if (!cell.hasClass("bg-white")) continue;

                    Element disciplineEl = cell.selectFirst("b");
                    if (disciplineEl == null) continue;
                    String discipline = disciplineEl.ownText().trim();
                    if (discipline.isBlank()) continue;

                    Element roomBtn = cell.selectFirst("form[action*=sala] button");
                    String roomRaw = roomBtn != null ? roomBtn.text().trim() : "";
                    if (roomRaw.isBlank()) roomRaw = "UNKNOWN";

                    String block = extractBlock(roomRaw);
                    DayOfWeek day = days.get(c - 1);

                    byRoom.computeIfAbsent(roomRaw, k -> new RoomAccumulator(k, block))
                          .schedules.add(new ParsedSchedule(day, startTime, endTime, discipline));
                }
            }
        }

        log.info("SigehoHtmlParser: parsed {} tables, {} rooms", tableCount, byRoom.size());

        List<ParsedRoom> rooms = byRoom.values().stream()
                .map(a -> new ParsedRoom(a.id, a.block, List.copyOf(a.schedules)))
                .toList();
        return new ParsedImport(rooms);
    }

    private List<DayOfWeek> extractDays(Element headerRow) {
        List<DayOfWeek> days = new ArrayList<>();
        Elements cells = headerRow.select("td");
        for (int i = 1; i < cells.size(); i++) {
            DayOfWeek day = parseDayName(cells.get(i).select("b").text().trim());
            if (day != null) days.add(day);
        }
        return days;
    }

    private DayOfWeek parseDayName(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String n = Normalizer.normalize(raw.toLowerCase().trim(), Normalizer.Form.NFD)
                .replaceAll("[^\\p{ASCII}]", "");
        if (n.startsWith("segunda")) return DayOfWeek.MONDAY;
        if (n.startsWith("terca"))   return DayOfWeek.TUESDAY;
        if (n.startsWith("quarta"))  return DayOfWeek.WEDNESDAY;
        if (n.startsWith("quinta"))  return DayOfWeek.THURSDAY;
        if (n.startsWith("sexta"))   return DayOfWeek.FRIDAY;
        if (n.startsWith("sabado"))  return DayOfWeek.SATURDAY;
        return null;
    }

    private String extractBlock(String roomRaw) {
        Matcher m = BLOCK_PATTERN.matcher(roomRaw);
        return m.find() ? m.group(1) : null;
    }

    private static final class RoomAccumulator {
        final String id;
        final String block;
        final List<ParsedSchedule> schedules = new ArrayList<>();

        RoomAccumulator(String id, String block) {
            this.id    = id;
            this.block = block;
        }
    }
}
