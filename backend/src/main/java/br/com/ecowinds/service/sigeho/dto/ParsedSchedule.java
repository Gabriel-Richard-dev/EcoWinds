package br.com.ecowinds.service.sigeho.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ParsedSchedule(
        DayOfWeek weekday,
        LocalTime startTime,
        LocalTime endTime,
        String course
) {}
