package br.com.ecowinds.dto.holiday;

import br.com.ecowinds.model.enums.HolidayScope;

import java.time.LocalDate;

public record CreateHolidayRequest(
        LocalDate date,
        String name,
        HolidayScope scope,
        String type
) {}
