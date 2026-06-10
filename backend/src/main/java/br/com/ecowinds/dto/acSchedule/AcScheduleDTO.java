package br.com.ecowinds.dto.acSchedule;

import br.com.ecowinds.model.AcSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record AcScheduleDTO(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime time,
        AcSchedule.Action action,
        Boolean enabled
) {
    public AcScheduleDTO(AcSchedule entity) {
        this(
                entity.getId(),
                entity.getDayOfWeek(),
                entity.getTime(),
                entity.getAction(),
                entity.getEnabled()
        );
    }
}
