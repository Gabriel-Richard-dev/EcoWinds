package br.com.ecowinds.service.sigeho.parser;

import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import com.fasterxml.jackson.databind.JsonNode;

public interface SigehoParser {

    /**
     * @return true se este parser reconhece o formato JSON e deve tentar o parse.
     */
    boolean supports(JsonNode root);

    /**
     * Converte a árvore JSON na representação canônica interna.
     * Lança exceção em entrada malformada/inválida reconhecida como deste formato.
     */
    ParsedImport parse(JsonNode root);

    /**
     * Identificador persistido em schedule_imports.parser_used.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
