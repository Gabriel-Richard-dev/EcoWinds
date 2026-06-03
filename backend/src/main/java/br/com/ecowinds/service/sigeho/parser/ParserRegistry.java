package br.com.ecowinds.service.sigeho.parser;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ParserRegistry {

    private final List<SigehoParser> parsers;

    public ParserRegistry(List<SigehoParser> parsers) {
        this.parsers = parsers;
    }

    public SigehoParser select(JsonNode root) {
        for (SigehoParser p : parsers) {
            if (p.supports(root)) return p;
        }
        throw new IllegalArgumentException(
                "No registered parser recognizes the JSON structure. "
              + "Available parsers: " + parsers.stream().map(SigehoParser::name).toList());
    }
}
