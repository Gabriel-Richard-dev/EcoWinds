package br.com.ecowinds.controller;

import br.com.ecowinds.dto.espDevice.EspDeviceDTO;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.repository.EspDeviceRepository;
import br.com.ecowinds.security.ApiKeyHasher;
import br.com.ecowinds.service.EspDeviceService;
import jakarta.persistence.EntityNotFoundException;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Esp Devices", description = "Endpoints for managing ESP32 devices and their connections")
@RestController
@RequestMapping("/esp-device")
public class EspDeviceController {

    private final EspDeviceService espDeviceService;
    private final EspDeviceRepository espDeviceRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public EspDeviceController(EspDeviceService espDeviceService,
                               EspDeviceRepository espDeviceRepository) {
        this.espDeviceService = espDeviceService;
        this.espDeviceRepository = espDeviceRepository;
    }

    @Operation(summary = "Rotate API key for a device",
            description = "Generates a new API key. The raw key is returned ONCE in this response; only its hash is stored.")
    @PostMapping("/{id}/api-key")
    public ResponseEntity<Map<String, String>> rotateApiKey(@PathVariable Long id) {
        EspDevice device = espDeviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
        byte[] rnd = new byte[32];
        secureRandom.nextBytes(rnd);
        String rawKey = Base64.getUrlEncoder().withoutPadding().encodeToString(rnd);
        device.setApiKeyHash(ApiKeyHasher.sha256(rawKey));
        espDeviceRepository.save(device);
        return ResponseEntity.ok(Map.of(
                "deviceId", id.toString(),
                "apiKey", rawKey,
                "warning", "Store this key now; it will not be shown again."
        ));
    }

    @Operation(summary = "Search for paginated Esp Devices", description = "Returns a list of Esp Devices filtered by ipAddress, sorted by ID.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthenticated user"),
            @ApiResponse(responseCode = "403", description = "No permission to access this resource.")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/search")
    public ResponseEntity<Page<EspDeviceDTO>> search(
            @RequestParam(value = "search") String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(espDeviceService.searchPageable(search, page, size));
    }

    @Operation(summary = "Get device by ID", description = "Returns details of a specific device. Requires authentication.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Device found"),
            @ApiResponse(responseCode = "404", description = "Device not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ResponseEntity<EspDeviceDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(espDeviceService.findById(id));
    }

    @Operation(summary = "Register new device", description = "Creates a new ESP device entry. Requires authentication.")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<EspDeviceDTO> save(@RequestBody EspDeviceDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(espDeviceService.save(dto));
    }

    @Operation(summary = "Update device", description = "Updates an existing device's data (IP, Status, Frequency). Requires authentication.")
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    public ResponseEntity<EspDeviceDTO> update(@PathVariable Long id, @RequestBody EspDeviceDTO dto) {
        return ResponseEntity.ok(espDeviceService.update(id, dto));
    }

    @Operation(summary = "Remove device", description = "Deletes a device from the system. Requires authentication.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        espDeviceService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Swipe on/off", description = "Swipe on/off device.")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("swipe/{id}")
    public ResponseEntity<Void> swipeOnOff(@PathVariable Long id) {
        espDeviceService.swipeOnOff(id);
        return ResponseEntity.noContent().build();
    }

}
