package br.com.ecowinds.dto.classSchedule;

import java.time.LocalDate;

public record NextScheduleResponse(
        LocalDate resolvedDate,
        ClassScheduleDTO schedule
) {}
