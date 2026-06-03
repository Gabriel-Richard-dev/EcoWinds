package br.com.ecowinds.dto.device;

import java.time.LocalDateTime;

public record DeviceStateResponse(
        String desired,
        String reason,
        LocalDateTime serverTime,
        LocalDateTime nextChangeAt,
        String currentCourse,
        Long currentScheduleId,
        boolean holidayToday,
        String holidayName,
        String infraredFrequency,
        String roomIdentification
) {}
