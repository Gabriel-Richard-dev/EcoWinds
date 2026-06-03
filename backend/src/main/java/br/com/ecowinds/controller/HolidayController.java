package br.com.ecowinds.controller;

import br.com.ecowinds.dto.holiday.CreateHolidayRequest;
import br.com.ecowinds.dto.holiday.HolidayDTO;
import br.com.ecowinds.service.holiday.HolidayService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/holidays")
public class HolidayController {

    private final HolidayService service;

    public HolidayController(HolidayService service) {
        this.service = service;
    }

    @GetMapping
    public List<HolidayDTO> list(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        return service.findInRange(LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31))
                .stream().map(HolidayDTO::new).toList();
    }

    @GetMapping("/today")
    public Map<String, Object> today() {
        LocalDate today = LocalDate.now();
        var items = service.findByDate(today).stream().map(HolidayDTO::new).toList();
        return Map.of("date", today, "isHoliday", !items.isEmpty(), "holidays", items);
    }

    @GetMapping("/check")
    public Map<String, Object> check(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var items = service.findByDate(date).stream().map(HolidayDTO::new).toList();
        return Map.of("date", date, "isHoliday", !items.isEmpty(), "holidays", items);
    }

    @PostMapping
    public ResponseEntity<HolidayDTO> create(@RequestBody CreateHolidayRequest req) {
        var h = service.createCustom(req.date(), req.name(), req.scope(), req.type());
        return ResponseEntity.ok(new HolidayDTO(h));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteCustom(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/sync")
    public Map<String, Object> sync(@RequestParam(required = false) Integer year) {
        int y = year != null ? year : Year.now().getValue();
        int upserts = service.syncNationalHolidays(y);
        return Map.of("year", y, "upserts", upserts);
    }
}
