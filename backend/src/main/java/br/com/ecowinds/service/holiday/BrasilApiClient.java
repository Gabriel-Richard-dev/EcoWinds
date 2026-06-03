package br.com.ecowinds.service.holiday;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
public class BrasilApiClient {

    private static final Logger log = LoggerFactory.getLogger(BrasilApiClient.class);

    private final RestClient client;

    public BrasilApiClient(@Value("${app.holidays.brasilapi-base-url:https://brasilapi.com.br/api}") String baseUrl,
                           @Value("${app.holidays.timeout-seconds:8}") int timeoutSeconds) {
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(new org.springframework.http.client.SimpleClientHttpRequestFactory() {{
                    setConnectTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                    setReadTimeout((int) Duration.ofSeconds(timeoutSeconds).toMillis());
                }})
                .build();
    }

    public List<NationalHoliday> fetchNationalHolidays(int year) {
        JsonNode body = client.get()
                .uri("/feriados/v1/{year}", year)
                .retrieve()
                .body(JsonNode.class);
        if (body == null || !body.isArray()) {
            log.warn("BrasilAPI returned non-array body for year {}", year);
            return List.of();
        }
        List<NationalHoliday> out = new ArrayList<>(body.size());
        for (JsonNode n : body) {
            String dateStr = n.path("date").asText(null);
            String name = n.path("name").asText(null);
            String type = n.path("type").asText(null);
            if (dateStr == null || name == null) continue;
            try {
                out.add(new NationalHoliday(LocalDate.parse(dateStr), name, type));
            } catch (Exception e) {
                log.warn("Skipping malformed holiday entry: {}", n);
            }
        }
        return out;
    }

    public record NationalHoliday(LocalDate date, String name, String type) {}
}
