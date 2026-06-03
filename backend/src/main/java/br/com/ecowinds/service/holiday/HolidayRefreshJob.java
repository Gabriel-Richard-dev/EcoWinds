package br.com.ecowinds.service.holiday;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "app.holidays.refresh-enabled", havingValue = "true", matchIfMissing = true)
public class HolidayRefreshJob {

    private static final Logger log = LoggerFactory.getLogger(HolidayRefreshJob.class);

    private final HolidayService service;

    public HolidayRefreshJob(HolidayService service) {
        this.service = service;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        try {
            service.syncCurrentAndNextYear();
        } catch (Exception e) {
            log.warn("Initial holiday sync failed: {}", e.getMessage());
        }
    }

    /** Daily refresh at 03:15 — keeps cache warm and catches government calendar updates. */
    @Scheduled(cron = "${app.holidays.refresh-cron:0 15 3 * * *}")
    public void scheduledRefresh() {
        service.syncCurrentAndNextYear();
    }
}
