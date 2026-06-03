package br.com.ecowinds.model;

import br.com.ecowinds.model.enums.ImportSource;
import br.com.ecowinds.model.enums.ImportStatus;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_imports")
public class ScheduleImport extends BaseEntity {

    @Column(nullable = false, length = 512)
    private String filename;

    @Column(name = "file_hash", nullable = false, length = 128)
    private String fileHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ImportStatus status;

    @Column(name = "parser_used", length = 128)
    private String parserUsed;

    @Column(name = "rooms_affected", nullable = false)
    private int roomsAffected;

    @Column(name = "schedules_created", nullable = false)
    private int schedulesCreated;

    @Column(name = "schedules_deleted", nullable = false)
    private int schedulesDeleted;

    @Column(name = "rooms_created", nullable = false)
    private int roomsCreated;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public String getFilename() { return filename; }
    public void setFilename(String filename) { this.filename = filename; }
    public String getFileHash() { return fileHash; }
    public void setFileHash(String fileHash) { this.fileHash = fileHash; }
    public ImportSource getSource() { return source; }
    public void setSource(ImportSource source) { this.source = source; }
    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }
    public String getParserUsed() { return parserUsed; }
    public void setParserUsed(String parserUsed) { this.parserUsed = parserUsed; }
    public int getRoomsAffected() { return roomsAffected; }
    public void setRoomsAffected(int roomsAffected) { this.roomsAffected = roomsAffected; }
    public int getSchedulesCreated() { return schedulesCreated; }
    public void setSchedulesCreated(int schedulesCreated) { this.schedulesCreated = schedulesCreated; }
    public int getSchedulesDeleted() { return schedulesDeleted; }
    public void setSchedulesDeleted(int schedulesDeleted) { this.schedulesDeleted = schedulesDeleted; }
    public int getRoomsCreated() { return roomsCreated; }
    public void setRoomsCreated(int roomsCreated) { this.roomsCreated = roomsCreated; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}
