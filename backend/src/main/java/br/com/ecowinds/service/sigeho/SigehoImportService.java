package br.com.ecowinds.service.sigeho;

import br.com.ecowinds.model.ScheduleImport;
import br.com.ecowinds.model.enums.ImportSource;
import br.com.ecowinds.model.enums.ImportStatus;
import br.com.ecowinds.repository.ScheduleImportRepository;
import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import br.com.ecowinds.service.sigeho.parser.ParserRegistry;
import br.com.ecowinds.service.sigeho.parser.SigehoParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;

@Service
public class SigehoImportService {

    private static final Logger log = LoggerFactory.getLogger(SigehoImportService.class);

    private final ParserRegistry parserRegistry;
    private final ObjectMapper objectMapper;
    private final ScheduleImportRepository scheduleImportRepository;
    private final SigehoPersistenceService persistenceService;

    public SigehoImportService(ParserRegistry parserRegistry,
                               ObjectMapper objectMapper,
                               ScheduleImportRepository scheduleImportRepository,
                               SigehoPersistenceService persistenceService) {
        this.parserRegistry = parserRegistry;
        this.objectMapper = objectMapper;
        this.scheduleImportRepository = scheduleImportRepository;
        this.persistenceService = persistenceService;
    }

    public ImportOutcome importFromPath(Path file, ImportSource source) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            return importFromBytes(file.getFileName().toString(), bytes, source);
        } catch (IOException e) {
            log.error("Failed reading {}", file, e);
            ScheduleImport rec = baseRecord(file.getFileName().toString(), "", source);
            rec.setStatus(ImportStatus.PARSE_ERROR);
            rec.setErrorMessage("IO: " + e.getMessage());
            rec.setFinishedAt(LocalDateTime.now());
            return new ImportOutcome(scheduleImportRepository.save(rec), "io_error");
        }
    }

    public ImportOutcome importFromBytes(String filename, byte[] content, ImportSource source) {
        String hash = sha256(content);

        // Dedup: same hash already processed successfully? skip.
        var prior = scheduleImportRepository.findFirstByFileHashOrderByStartedAtDesc(hash);
        if (prior.isPresent() && prior.get().getStatus() == ImportStatus.SUCCESS) {
            ScheduleImport rec = baseRecord(filename, hash, source);
            rec.setStatus(ImportStatus.SKIPPED_DUPLICATE);
            rec.setFinishedAt(LocalDateTime.now());
            log.info("Skipping {}: identical hash already imported (import #{})", filename, prior.get().getId());
            return new ImportOutcome(scheduleImportRepository.save(rec), "duplicate");
        }

        ScheduleImport rec = baseRecord(filename, hash, source);

        JsonNode root;
        try {
            root = objectMapper.readTree(content);
        } catch (IOException e) {
            rec.setStatus(ImportStatus.PARSE_ERROR);
            rec.setErrorMessage("Invalid JSON: " + e.getMessage());
            rec.setFinishedAt(LocalDateTime.now());
            return new ImportOutcome(scheduleImportRepository.save(rec), "invalid_json");
        }

        SigehoParser parser;
        ParsedImport parsed;
        try {
            parser = parserRegistry.select(root);
            rec.setParserUsed(parser.name());
            parsed = parser.parse(root);
        } catch (RuntimeException e) {
            rec.setStatus(ImportStatus.VALIDATION_ERROR);
            rec.setErrorMessage(e.getMessage());
            rec.setFinishedAt(LocalDateTime.now());
            log.warn("Parse failure for {}: {}", filename, e.getMessage());
            return new ImportOutcome(scheduleImportRepository.save(rec), "parse_failure");
        }

        try {
            persistenceService.apply(parsed, rec);
            rec.setStatus(ImportStatus.SUCCESS);
        } catch (RuntimeException e) {
            log.error("Persistence failure importing {}", filename, e);
            rec.setStatus(ImportStatus.PERSISTENCE_ERROR);
            rec.setErrorMessage(e.getMessage());
        }
        rec.setFinishedAt(LocalDateTime.now());
        return new ImportOutcome(scheduleImportRepository.save(rec), rec.getStatus().name());
    }

    private ScheduleImport baseRecord(String filename, String hash, ImportSource source) {
        ScheduleImport rec = new ScheduleImport();
        rec.setFilename(filename);
        rec.setFileHash(hash);
        rec.setSource(source);
        rec.setStatus(ImportStatus.PENDING);
        rec.setStartedAt(LocalDateTime.now());
        return rec;
    }

    private String sha256(byte[] content) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(md.digest(content));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
