package br.com.ecowinds.service.sigeho.dto;

import java.util.List;

public record ParsedRoom(
        String identification,
        String block,
        List<ParsedSchedule> schedules
) {}
