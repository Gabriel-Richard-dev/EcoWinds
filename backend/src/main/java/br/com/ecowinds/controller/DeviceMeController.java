package br.com.ecowinds.controller;

import br.com.ecowinds.dto.classSchedule.NextScheduleResponse;
import br.com.ecowinds.dto.device.DeviceLogRequest;
import br.com.ecowinds.dto.device.DeviceStateResponse;
import br.com.ecowinds.dto.device.HeartbeatRequest;
import br.com.ecowinds.model.AuditLog;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.repository.AuditLogRepository;
import br.com.ecowinds.repository.EspDeviceRepository;
import br.com.ecowinds.service.ClassScheduleService;
import br.com.ecowinds.service.device.DeviceStateService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/devices/me")
public class DeviceMeController {

    private final EspDeviceRepository deviceRepository;
    private final DeviceStateService stateService;
    private final AuditLogRepository auditLogRepository;
    private final ClassScheduleService classScheduleService;

    public DeviceMeController(EspDeviceRepository deviceRepository,
                              DeviceStateService stateService,
                              AuditLogRepository auditLogRepository,
                              ClassScheduleService classScheduleService) {
        this.deviceRepository = deviceRepository;
        this.stateService = stateService;
        this.auditLogRepository = auditLogRepository;
        this.classScheduleService = classScheduleService;
    }

    @GetMapping("/state")
    public DeviceStateResponse state() {
        return stateService.computeState(currentDevice());
    }

    @GetMapping("/next-schedule")
    public ResponseEntity<NextScheduleResponse> nextSchedule() {
        EspDevice device = currentDevice();
        NextScheduleResponse response = classScheduleService.nextSchedule(device.getId());
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @PostMapping("/heartbeat")
    public Map<String, Object> heartbeat(@RequestBody(required = false) HeartbeatRequest req) {
        EspDevice d = currentDevice();
        if (req != null && req.ip() != null && !req.ip().isBlank()) {
            d.setIpAddress(req.ip());
        }
        d.setConnectionStatus(true);
        d.setLastHeartbeatAt(LocalDateTime.now());
        deviceRepository.save(d);
        return Map.of("ok", true, "serverTime", LocalDateTime.now());
    }

    @PostMapping("/log")
    public ResponseEntity<Void> log(@RequestBody DeviceLogRequest req) {
        if (req == null || req.action() == null || req.action().isBlank()) {
            return ResponseEntity.badRequest().build();
        }
        EspDevice d = currentDevice();
        AuditLog entry = new AuditLog(LocalDateTime.now(), req.action(), "ESP32",
                null, d.getRoom(), d);
        if (req.detail() != null) {
            entry.setOrigin("ESP32:" + req.detail());
        }
        auditLogRepository.save(entry);
        return ResponseEntity.accepted().build();
    }

    private EspDevice currentDevice() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof Long id)) {
            throw new EntityNotFoundException("Device not authenticated");
        }
        return deviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
    }
}
