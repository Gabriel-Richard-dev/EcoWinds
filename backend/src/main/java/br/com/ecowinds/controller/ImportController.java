package br.com.ecowinds.controller;

import br.com.ecowinds.dto.imports.ScheduleImportDTO;
import br.com.ecowinds.model.enums.ImportSource;
import br.com.ecowinds.repository.ScheduleImportRepository;
import br.com.ecowinds.service.sigeho.SigehoFolderWatcher;
import br.com.ecowinds.service.sigeho.SigehoImportService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/imports")
public class ImportController {

    private final SigehoImportService importService;
    private final SigehoFolderWatcher folderWatcher;
    private final ScheduleImportRepository repository;

    public ImportController(SigehoImportService importService,
                            SigehoFolderWatcher folderWatcher,
                            ScheduleImportRepository repository) {
        this.importService = importService;
        this.folderWatcher = folderWatcher;
        this.repository = repository;
    }

    @GetMapping
    public Page<ScheduleImportDTO> list(@RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "20") int size) {
        return repository.findAllByOrderByStartedAtDesc(PageRequest.of(page, size))
                .map(ScheduleImportDTO::new);
    }

    @GetMapping("/{id}")
    public ScheduleImportDTO get(@PathVariable Long id) {
        return repository.findById(id).map(ScheduleImportDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("Import not found"));
    }

    @PostMapping("/upload")
    public ResponseEntity<ScheduleImportDTO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        var outcome = importService.importFromBytes(
                file.getOriginalFilename() == null ? "upload.json" : file.getOriginalFilename(),
                file.getBytes(),
                ImportSource.UPLOAD);
        return ResponseEntity.ok(new ScheduleImportDTO(outcome.record()));
    }

    @PostMapping("/trigger")
    public Map<String, Object> trigger() {
        int processed = folderWatcher.scanOnce();
        return Map.of("processed", processed);
    }
}
