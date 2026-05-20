package br.com.ecowinds.controller;

import br.com.ecowinds.dto.espDevice.EspDeviceDTO;
import br.com.ecowinds.service.EspDeviceService;
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

    public EspDeviceController(EspDeviceService espDeviceService) {
        this.espDeviceService = espDeviceService;
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
