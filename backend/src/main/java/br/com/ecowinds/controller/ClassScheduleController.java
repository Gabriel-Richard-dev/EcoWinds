package br.com.ecowinds.controller;

import br.com.ecowinds.dto.classSchedule.ClassScheduleDTO;
import br.com.ecowinds.dto.classSchedule.NextScheduleResponse;
import br.com.ecowinds.service.ClassScheduleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Class Schedules", description = "Endpoint for classroom schedule management.")
@RestController
@RequestMapping("/class-schedule")
@SecurityRequirement(name = "bearerAuth")
public class ClassScheduleController {

    private final ClassScheduleService classScheduleService;

    public ClassScheduleController(ClassScheduleService classScheduleService) {
        this.classScheduleService = classScheduleService;
    }

    @Operation(summary = "Search for paginated class schedules", description = "Returns a list of schedules filtered by course or room, sorted by course.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Search completed successfully."),
            @ApiResponse(responseCode = "401", description = "Unauthenticated user"),
            @ApiResponse(responseCode = "403", description = "No permission to access this resource.")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/search")
    public ResponseEntity<Page<ClassScheduleDTO>> search(
            @RequestParam(value = "search") String search,
            @RequestParam(value = "page", required = false, defaultValue = "0") int page,
            @RequestParam(value = "size", required = false, defaultValue = "10") int size
    ) {
        return ResponseEntity.ok().body(classScheduleService.searchPageable(search, page, size));
    }

    @Operation(summary = "Find class schedule by ID", description = "Returns the details of a specific schedule using its unique identifier.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ClassSchedule found successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "ClassSchedule not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @GetMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> findById(@PathVariable Long id){
        return ResponseEntity.ok().body(classScheduleService.findById(id));
    }

    @Operation(summary = "Create a new class schedule", description = "Registers a new class schedule in the system based on the provided data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "ClassSchedule created successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "400", description = "Invalid request data")
    })
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<ClassScheduleDTO> save(@RequestBody ClassScheduleDTO dto) {
        return ResponseEntity.ok().body(classScheduleService.save(dto));
    }

    @Operation(summary = "Update an existing class schedule", description = "Updates schedule details based on the provided ID and body data.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "ClassSchedule updated successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "ClassSchedule not found")
    })
    @ResponseStatus(HttpStatus.OK)
    @PutMapping("/{id}")
    public ResponseEntity<ClassScheduleDTO> update(@PathVariable Long id, @RequestBody ClassScheduleDTO dto) {
        return ResponseEntity.ok(classScheduleService.update(id, dto));
    }

    @Operation(summary = "Get next class schedule for a device",
            description = "Returns the next class schedule today for the room associated with the given ESP device, after the current time. 204 if there is none today.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Next schedule found"),
            @ApiResponse(responseCode = "204", description = "No further schedule today"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @GetMapping("/next/{deviceId}")
    public ResponseEntity<NextScheduleResponse> next(@PathVariable Long deviceId) {
        NextScheduleResponse response = classScheduleService.nextSchedule(deviceId);
        return response != null ? ResponseEntity.ok(response) : ResponseEntity.noContent().build();
    }

    @Operation(summary = "Delete a class schedule", description = "Permanently removes a class schedule from the system.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "ClassSchedule deleted successfully"),
            @ApiResponse(responseCode = "401", description = "Unauthorized"),
            @ApiResponse(responseCode = "404", description = "ClassSchedule not found")
    })
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        classScheduleService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
