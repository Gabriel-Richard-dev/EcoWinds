package br.com.ecowinds.service.device;

import br.com.ecowinds.dto.device.DeviceStateResponse;
import br.com.ecowinds.model.AcSchedule;
import br.com.ecowinds.model.AuditLog;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.repository.AcScheduleRepository;
import br.com.ecowinds.repository.AuditLogRepository;
import br.com.ecowinds.repository.EspDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Component
public class AcCommandScheduler {

    private static final Logger log = LoggerFactory.getLogger(AcCommandScheduler.class);

    private final EspDeviceRepository deviceRepository;
    private final DeviceStateService deviceStateService;
    private final AcScheduleRepository acScheduleRepository;
    private final AuditLogRepository auditLogRepository;

    public AcCommandScheduler(EspDeviceRepository deviceRepository,
                               DeviceStateService deviceStateService,
                               AcScheduleRepository acScheduleRepository,
                               AuditLogRepository auditLogRepository) {
        this.deviceRepository = deviceRepository;
        this.deviceStateService = deviceStateService;
        this.acScheduleRepository = acScheduleRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Scheduled(cron = "0 * * * * *")
    @Transactional
    public void evaluate() {
        List<EspDevice> devices = deviceRepository.findAll();
        if (devices.isEmpty()) return;
        EspDevice device = devices.get(0);

        LocalDateTime now = LocalDateTime.now();
        DayOfWeek dow = now.getDayOfWeek();
        LocalTime nowTime = now.toLocalTime();

        // AcSchedule: check if any dedicated schedule fires this minute
        String acScheduleAction = findAcScheduleAction(dow, nowTime);

        // Class schedule state (continuous)
        DeviceStateResponse classState = deviceStateService.computeState(device);

        // AcSchedule overrides class schedule at its trigger minute
        String desiredAction = acScheduleAction != null ? acScheduleAction : classState.desired();
        boolean desiredOn = "ON".equals(desiredAction);
        boolean currentOn = Boolean.TRUE.equals(device.getAirOn());

        if (desiredOn != currentOn) {
            // airOn é o estado desejado; o ESP aplica no próximo poll de /esp-device/sync
            device.setAirOn(desiredOn);
            deviceRepository.save(device);

            String reason = acScheduleAction != null ? "AC_SCHEDULE" : classState.reason();
            auditLogRepository.save(new AuditLog(
                    now, "AC_" + desiredAction, "SCHEDULER:" + reason,
                    null, device.getRoom(), device));

            log.info("[scheduler] AC {} reason={}", desiredAction, reason);
        }
    }

    private String findAcScheduleAction(DayOfWeek dow, LocalTime now) {
        List<AcSchedule> schedules = acScheduleRepository.findByDayOfWeekAndEnabledTrue(dow);
        return schedules.stream()
                .filter(s -> s.getTime().getHour() == now.getHour()
                          && s.getTime().getMinute() == now.getMinute())
                .map(s -> s.getAction().name())
                .findFirst()
                .orElse(null);
    }
}
