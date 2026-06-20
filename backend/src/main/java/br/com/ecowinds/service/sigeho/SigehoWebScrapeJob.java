package br.com.ecowinds.service.sigeho;

import br.com.ecowinds.model.enums.ImportSource;
import br.com.ecowinds.service.sigeho.SigehoWebScraper.ScrapeSession;
import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import br.com.ecowinds.service.sigeho.parser.SigehoHtmlParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(value = "app.sigeho.scraper.enabled", havingValue = "true", matchIfMissing = false)
public class SigehoWebScrapeJob {

    private static final Logger log = LoggerFactory.getLogger(SigehoWebScrapeJob.class);

    private final SigehoWebScraper    scraper;
    private final SigehoHtmlParser    htmlParser;
    private final SigehoImportService importService;

    @Value("${app.sigeho.scraper.campus-id:3}")
    private String campusId;

    @Value("${app.sigeho.scraper.campus-slug:maracanau}")
    private String campusSlug;

    @Value("${app.sigeho.scraper.course-ids:9,18}")
    private List<String> courseIds;

    public SigehoWebScrapeJob(SigehoWebScraper scraper,
                              SigehoHtmlParser htmlParser,
                              SigehoImportService importService) {
        this.scraper       = scraper;
        this.htmlParser    = htmlParser;
        this.importService = importService;
    }

    @Scheduled(cron = "${app.sigeho.scraper.cron:0 0 3 1 * *}")
    public void run() {
        runForCourses(courseIds);
    }

    /**
     * Opens a single SIGEHO session (one GET) and scrapes all given courses.
     * The session auto-resolves the latest semester from the form dropdown.
     */
    public List<ImportOutcome> runForCourses(List<String> targetCourseIds) {
        log.info("SIGEHO web scrape starting — campus={} courses={}", campusSlug, targetCourseIds);
        ScrapeSession session;
        try {
            session = scraper.openSession(campusSlug);
            log.info("Resolved latest semester: {}", session.latestSemestreId());
        } catch (Exception e) {
            log.error("SIGEHO session failed — aborting scrape", e);
            return List.of();
        }

        return targetCourseIds.stream()
                .map(courseId -> {
                    try {
                        return scrapeOne(session, courseId);
                    } catch (Exception e) {
                        log.error("SIGEHO scrape failed for courseId={}", courseId, e);
                        return null;
                    }
                })
                .filter(o -> o != null)
                .toList();
    }

    /** Scrapes a single course using an existing session. */
    public ImportOutcome scrapeOne(ScrapeSession session, String courseId) throws Exception {
        byte[] html = scraper.fetchCourseHtml(session, campusId, courseId);
        ParsedImport parsed = htmlParser.parse(html);

        String label = "sigeho-web-c%s-s%s-course%s"
                .formatted(campusId, session.latestSemestreId(), courseId);
        ImportOutcome outcome = importService.importFromParsed(label, html, parsed, ImportSource.WEB_SCRAPER);

        log.info("SIGEHO scrape courseId={} → status={} rooms={} schedules={}",
                courseId,
                outcome.record().getStatus(),
                outcome.record().getRoomsAffected(),
                outcome.record().getSchedulesCreated());
        return outcome;
    }
}
