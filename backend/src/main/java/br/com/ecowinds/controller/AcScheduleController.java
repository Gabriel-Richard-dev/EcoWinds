package br.com.ecowinds.controller;

import br.com.ecowinds.dto.acSchedule.AcScheduleDTO;
import br.com.ecowinds.service.AcScheduleService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "AC Schedules", description = "Agendamentos dedicados para ligar/desligar o ar-condicionado")
@RestController
@RequestMapping("/ac-schedule")
public class AcScheduleController {

    private final AcScheduleService service;

    public AcScheduleController(AcScheduleService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<AcScheduleDTO>> findAll() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<AcScheduleDTO> findById(@PathVariable Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ResponseEntity<AcScheduleDTO> save(@RequestBody AcScheduleDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.save(dto));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AcScheduleDTO> update(@PathVariable Long id, @RequestBody AcScheduleDTO dto) {
        return ResponseEntity.ok(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
