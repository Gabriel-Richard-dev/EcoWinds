package br.com.ecowinds.service.sigeho;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Service
public class SigehoWebScraper {

    private static final Logger log = LoggerFactory.getLogger(SigehoWebScraper.class);

    private static final String BASE         = "https://sigeho.ifce.edu.br";
    private static final String POST_URL     = BASE + "/visualizar-horarios-curso/";
    private static final Pattern CSRF_COOKIE = Pattern.compile("csrftoken=([^;]+)");

    /**
     * A reusable session that holds the CSRF token, the resolved latest semester ID,
     * and the HTTP client with its associated cookie store.
     * Open once per job run and pass to each {@link #fetchCourseHtml} call.
     */
    public record ScrapeSession(HttpClient client, String csrfToken,
                                String latestSemestreId, String campusFormUrl) {}

    /**
     * Opens a scrape session for the given campus slug.
     * Performs a single GET to the campus form page, extracts the CSRF token
     * and the most recent semester ID from the select dropdown.
     */
    public ScrapeSession openSession(String campusSlug)
            throws IOException, InterruptedException {

        String formPageUrl = BASE + "/visualizar-horarios-curso/" + campusSlug + "/";

        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(15))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        HttpRequest getReq = HttpRequest.newBuilder()
                .uri(URI.create(formPageUrl))
                .GET()
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> getResp = client.send(getReq, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String csrf = extractCsrf(getResp);
        if (csrf == null || csrf.isBlank()) {
            throw new IOException("CSRF token not found in SIGEHO response for campus: " + campusSlug);
        }

        String semestreId = resolveLatestSemestreId(getResp.body());
        if (semestreId == null || semestreId.isBlank()) {
            throw new IOException("Could not resolve latest semester from SIGEHO form page for campus: " + campusSlug);
        }

        log.info("SIGEHO session opened — campus={} latestSemestre={}", campusSlug, semestreId);
        return new ScrapeSession(client, csrf, semestreId, formPageUrl);
    }

    /**
     * Fetches the schedule HTML for a single course using an existing session.
     * Reuses the session's CSRF token and resolved semester ID.
     */
    public byte[] fetchCourseHtml(ScrapeSession session, String campusId, String courseId)
            throws IOException, InterruptedException {

        String body = "campus_id=" + encode(campusId)
                + "&csrfmiddlewaretoken=" + encode(session.csrfToken())
                + "&todosSemestre=" + encode(session.latestSemestreId())
                + "&todosCurso=" + encode(courseId)
                + "&buscar=";

        HttpRequest postReq = HttpRequest.newBuilder()
                .uri(URI.create(POST_URL))
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("Referer", session.campusFormUrl())
                .header("User-Agent", "Mozilla/5.0")
                .timeout(Duration.ofSeconds(40))
                .build();

        HttpResponse<byte[]> postResp = session.client().send(postReq, HttpResponse.BodyHandlers.ofByteArray());

        if (postResp.statusCode() != 200) {
            throw new IOException("SIGEHO returned HTTP " + postResp.statusCode()
                    + " for campusId=" + campusId + " semestre=" + session.latestSemestreId()
                    + " course=" + courseId);
        }

        log.info("SIGEHO HTML fetched: {} bytes (campusId={} semestre={} course={})",
                postResp.body().length, campusId, session.latestSemestreId(), courseId);
        return postResp.body();
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    /**
     * Parses the semester select dropdown and returns the value of the first
     * non-placeholder option (i.e. the most recent semester).
     */
    private String resolveLatestSemestreId(String html) {
        Element select = Jsoup.parse(html).selectFirst("select[name=todosSemestre]");
        if (select == null) return null;
        return select.children().stream()
                .map(opt -> opt.attr("value"))
                .filter(v -> !v.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String extractCsrf(HttpResponse<String> response) {
        return response.headers().allValues("set-cookie").stream()
                .flatMap(v -> {
                    Matcher m = CSRF_COOKIE.matcher(v);
                    return m.find() ? Stream.of(m.group(1)) : Stream.empty();
                })
                .findFirst()
                .orElseGet(() -> {
                    Matcher m = Pattern.compile("name=[\"']csrfmiddlewaretoken[\"']\\s+value=[\"']([^\"']+)[\"']")
                            .matcher(response.body());
                    if (!m.find()) {
                        m = Pattern.compile("value=[\"']([^\"']+)[\"']\\s+name=[\"']csrfmiddlewaretoken[\"']")
                                .matcher(response.body());
                    }
                    return m.find() ? m.group(1) : null;
                });
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
