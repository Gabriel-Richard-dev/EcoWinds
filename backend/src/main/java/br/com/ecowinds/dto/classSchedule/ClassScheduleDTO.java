package br.com.ecowinds.dto.classSchedule;

import br.com.ecowinds.model.ClassSchedule;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ClassScheduleDTO(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        String course,
        Long roomId,
        String roomIdentification
) {
    public ClassScheduleDTO(ClassSchedule classSchedule) {
        this(
                classSchedule.getId(),
                classSchedule.getDayOfWeek(),
                classSchedule.getStartTime(),
                classSchedule.getEndTime(),
                classSchedule.getCourse(),
                classSchedule.getRoom() != null ? classSchedule.getRoom().getId() : null,
                classSchedule.getRoom() != null ? classSchedule.getRoom().getIdentification() : null
        );
    }
}
