package br.com.ecowinds.service.device;

import br.com.ecowinds.dto.device.DeviceStateResponse;
import br.com.ecowinds.model.ClassSchedule;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.model.Holiday;
import br.com.ecowinds.model.Room;
import br.com.ecowinds.repository.ClassScheduleRepository;
import br.com.ecowinds.service.holiday.HolidayService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

@Service
public class DeviceStateService {

    private final ClassScheduleRepository scheduleRepository;
    private final HolidayService holidayService;

    public DeviceStateService(ClassScheduleRepository scheduleRepository,
                              HolidayService holidayService) {
        this.scheduleRepository = scheduleRepository;
        this.holidayService = holidayService;
    }

    @Transactional(readOnly = true)
    public DeviceStateResponse computeState(EspDevice device) {
        Room room = device.getRoom();
        if (room == null) {
            return new DeviceStateResponse("OFF", "NO_ROOM",
                    LocalDateTime.now(), null, null, null, false, null,
                    device.getInfraredFrequency(), null);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalTime nowTime = now.toLocalTime();

        // Holiday gate: today is holiday => OFF until tomorrow (or next non-holiday day)
        List<Holiday> todayHolidays = holidayService.findByDate(today);
        if (!todayHolidays.isEmpty()) {
            LocalDate nextWorkingDay = nextNonHolidayDay(today.plusDays(1), 14);
            LocalDateTime nextChange = nextWorkingDay != null
                    ? LocalDateTime.of(nextWorkingDay, LocalTime.MIDNIGHT) : null;
            return new DeviceStateResponse("OFF", "HOLIDAY",
                    now, nextChange, null, null, true, todayHolidays.get(0).getName(),
                    device.getInfraredFrequency(), room.getIdentification());
        }

        List<ClassSchedule> all = scheduleRepository
                .findByRoomIdOrderByDayOfWeekAscStartTimeAsc(room.getId());

        // Currently inside a class?
        DayOfWeek dow = today.getDayOfWeek();
        ClassSchedule current = all.stream()
                .filter(s -> s.getDayOfWeek() == dow)
                .filter(s -> !nowTime.isBefore(s.getStartTime()) && nowTime.isBefore(s.getEndTime()))
                .findFirst().orElse(null);
        if (current != null) {
            return new DeviceStateResponse("ON", "IN_CLASS",
                    now, LocalDateTime.of(today, current.getEndTime()),
                    current.getCourse(), current.getId(), false, null,
                    device.getInfraredFrequency(), room.getIdentification());
        }

        // Next schedule (search up to 14 days ahead, skipping holidays)
        ClassSchedule next = null;
        LocalDate nextDate = null;
        for (int offset = 0; offset < 14; offset++) {
            LocalDate probe = today.plusDays(offset);
            if (!holidayService.findByDate(probe).isEmpty()) continue;
            DayOfWeek pdow = probe.getDayOfWeek();
            LocalTime threshold = (offset == 0) ? nowTime : LocalTime.MIN;
            var candidates = all.stream()
                    .filter(s -> s.getDayOfWeek() == pdow)
                    .filter(s -> s.getStartTime().isAfter(threshold))
                    .sorted(Comparator.comparing(ClassSchedule::getStartTime))
                    .toList();
            if (!candidates.isEmpty()) {
                next = candidates.get(0);
                nextDate = probe;
                break;
            }
        }

        LocalDateTime nextChange = (next != null)
                ? LocalDateTime.of(nextDate, next.getStartTime()) : null;
        return new DeviceStateResponse("OFF", "OUT_OF_CLASS",
                now, nextChange, null, null, false, null,
                device.getInfraredFrequency(), room.getIdentification());
    }

    private LocalDate nextNonHolidayDay(LocalDate from, int maxLookahead) {
        for (int i = 0; i < maxLookahead; i++) {
            LocalDate d = from.plusDays(i);
            if (holidayService.findByDate(d).isEmpty()) return d;
        }
        return null;
    }
}
