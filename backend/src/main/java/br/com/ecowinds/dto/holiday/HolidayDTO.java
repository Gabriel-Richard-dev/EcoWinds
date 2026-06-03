package br.com.ecowinds.dto.holiday;

import br.com.ecowinds.model.Holiday;
import br.com.ecowinds.model.enums.HolidayScope;

import java.time.LocalDate;

public record HolidayDTO(
        Long id,
        LocalDate date,
        String name,
        HolidayScope scope,
        String source,
        String type
) {
    public HolidayDTO(Holiday h) {
        this(h.getId(), h.getDate(), h.getName(), h.getScope(), h.getSource(), h.getType());
    }
}
