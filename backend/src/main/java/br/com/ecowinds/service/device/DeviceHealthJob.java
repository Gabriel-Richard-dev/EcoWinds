package br.com.ecowinds.service.device;

import br.com.ecowinds.repository.EspDeviceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Component
@ConditionalOnProperty(value = "app.devices.health-check-enabled", havingValue = "true", matchIfMissing = true)
public class DeviceHealthJob {

    private static final Logger log = LoggerFactory.getLogger(DeviceHealthJob.class);

    private final EspDeviceRepository deviceRepository;
    private final long timeoutSeconds;

    public DeviceHealthJob(EspDeviceRepository deviceRepository,
                           @Value("${app.devices.heartbeat-timeout-seconds:900}") long timeoutSeconds) {
        this.deviceRepository = deviceRepository;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${app.devices.health-check-interval-ms:60000}",
               initialDelayString = "${app.devices.health-check-initial-delay-ms:30000}")
    @Transactional
    public void markStaleOffline() {
        LocalDateTime threshold = LocalDateTime.now().minusSeconds(timeoutSeconds);
        int updated = deviceRepository.markStaleOffline(threshold);
        if (updated > 0) {
            log.info("Marked {} device(s) offline (no heartbeat since {})", updated, threshold);
        }
    }
}
