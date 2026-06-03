package br.com.ecowinds.service.sigeho.parser;

import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import com.fasterxml.jackson.databind.JsonNode;

public interface SigehoParser {

    /**
     * @return true if this parser recognizes the JSON shape and should attempt parsing.
     */
    boolean supports(JsonNode root);

    /**
     * Parses the JSON tree into the canonical internal representation.
     * Throws on malformed/invalid input recognized as this format.
     */
    ParsedImport parse(JsonNode root);

    /**
     * Identifier persisted in schedule_imports.parser_used.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
