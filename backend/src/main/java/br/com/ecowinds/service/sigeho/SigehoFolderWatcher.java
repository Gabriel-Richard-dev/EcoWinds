package br.com.ecowinds.service.sigeho;

import br.com.ecowinds.model.enums.ImportSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.Comparator;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(value = "app.import.poll-enabled", havingValue = "true", matchIfMissing = true)
public class SigehoFolderWatcher {

    private static final Logger log = LoggerFactory.getLogger(SigehoFolderWatcher.class);

    private final SigehoImportService importService;
    private final Path directory;
    private final Path archiveDirectory;
    private final boolean archiveOnSuccess;

    public SigehoFolderWatcher(SigehoImportService importService,
                               @Value("${app.import.directory:./downloads}") String directory,
                               @Value("${app.import.archive-directory:./downloads/processed}") String archive,
                               @Value("${app.import.archive-on-success:true}") boolean archiveOnSuccess) {
        this.importService = importService;
        this.directory = Paths.get(directory);
        this.archiveDirectory = Paths.get(archive);
        this.archiveOnSuccess = archiveOnSuccess;
    }

    @Scheduled(fixedDelayString = "${app.import.poll-interval-ms:300000}",
               initialDelayString = "${app.import.poll-initial-delay-ms:15000}")
    public void poll() {
        if (!Files.isDirectory(directory)) {
            log.debug("Import directory not present: {}", directory.toAbsolutePath());
            return;
        }
        scanOnce();
    }

    public int scanOnce() {
        int count = 0;
        try (Stream<Path> files = Files.list(directory)) {
            var sorted = files
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".json"))
                    .sorted(Comparator.comparingLong(p -> p.toFile().lastModified()))
                    .toList();
            for (Path file : sorted) {
                var outcome = importService.importFromPath(file, ImportSource.FOLDER_POLL);
                log.info("Imported {} -> {}", file.getFileName(), outcome.record().getStatus());
                if (archiveOnSuccess && outcome.record().getStatus().name().equals("SUCCESS")) {
                    archive(file);
                }
                count++;
            }
        } catch (IOException e) {
            log.error("Failed listing {}", directory, e);
        }
        return count;
    }

    private void archive(Path file) {
        try {
            Files.createDirectories(archiveDirectory);
            Path target = archiveDirectory.resolve(System.currentTimeMillis() + "_" + file.getFileName());
            Files.move(file, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Could not archive {}: {}", file, e.getMessage());
        }
    }
}
