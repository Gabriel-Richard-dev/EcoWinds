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
     * Sessão reutilizável que mantém o token CSRF, o ID do semestre mais recente
     * e o cliente HTTP com seu armazenamento de cookies.
     * Abra uma vez por execução do job e repasse a cada chamada de {@link #fetchCourseHtml}.
     */
    public record ScrapeSession(HttpClient client, String csrfToken,
                                String latestSemestreId, String campusFormUrl) {}

    /**
     * Abre uma sessão de scrape para o slug do campus informado.
     * Faz um GET na página do formulário do campus, extrai o token CSRF
     * e o ID do semestre mais recente no select.
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
     * Busca o HTML de horários de um curso usando uma sessão existente.
     * Reutiliza o token CSRF e o semestre já resolvidos na sessão.
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

    // ── Auxiliares ───────────────────────────────────────────────────────────

    /**
     * Interpreta o select de semestre e retorna o valor da primeira opção
     * que não seja placeholder (ou seja, o semestre mais recente).
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
