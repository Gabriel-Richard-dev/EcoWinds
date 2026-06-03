package br.com.ecowinds.dto.imports;

import br.com.ecowinds.model.ScheduleImport;
import br.com.ecowinds.model.enums.ImportSource;
import br.com.ecowinds.model.enums.ImportStatus;

import java.time.LocalDateTime;

public record ScheduleImportDTO(
        Long id,
        String filename,
        String fileHash,
        ImportSource source,
        ImportStatus status,
        String parserUsed,
        int roomsAffected,
        int roomsCreated,
        int schedulesCreated,
        int schedulesDeleted,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
    public ScheduleImportDTO(ScheduleImport s) {
        this(s.getId(), s.getFilename(), s.getFileHash(), s.getSource(), s.getStatus(),
                s.getParserUsed(), s.getRoomsAffected(), s.getRoomsCreated(),
                s.getSchedulesCreated(), s.getSchedulesDeleted(),
                s.getErrorMessage(), s.getStartedAt(), s.getFinishedAt());
    }
}
