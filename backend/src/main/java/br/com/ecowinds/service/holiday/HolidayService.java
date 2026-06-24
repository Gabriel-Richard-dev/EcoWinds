package br.com.ecowinds.service.holiday;

import br.com.ecowinds.model.Holiday;
import br.com.ecowinds.model.enums.HolidayScope;
import br.com.ecowinds.repository.HolidayRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;
import java.util.List;

@Service
public class HolidayService {

    private static final Logger log = LoggerFactory.getLogger(HolidayService.class);
    private static final String SOURCE_BRASILAPI = "BRASILAPI";

    private final HolidayRepository repository;
    private final BrasilApiClient brasilApi;

    public HolidayService(HolidayRepository repository, BrasilApiClient brasilApi) {
        this.repository = repository;
        this.brasilApi = brasilApi;
    }

    @Transactional(readOnly = true)
    public boolean isHoliday(LocalDate date) {
        return !repository.findByDate(date).isEmpty();
    }

    @Transactional(readOnly = true)
    public List<Holiday> findByDate(LocalDate date) {
        return repository.findByDate(date);
    }

    @Transactional(readOnly = true)
    public List<Holiday> findInRange(LocalDate start, LocalDate end) {
        return repository.findInRange(start, end);
    }

    @Transactional
    public Holiday createCustom(LocalDate date, String name, HolidayScope scope, String type) {
        if (scope == HolidayScope.NATIONAL) {
            throw new IllegalArgumentException("NATIONAL holidays are synced from BrasilAPI, not created manually");
        }
        return repository.findByDateAndScope(date, scope)
                .orElseGet(() -> repository.save(new Holiday(date, name, scope, "MANUAL", type)));
    }

    @Transactional
    public void deleteCustom(Long id) {
        Holiday h = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Holiday not found"));
        if (h.getScope() == HolidayScope.NATIONAL && SOURCE_BRASILAPI.equals(h.getSource())) {
            throw new IllegalArgumentException("Cannot delete a BrasilAPI-sourced national holiday; resync instead");
        }
        repository.delete(h);
    }

    /**
     * Sincroniza feriados nacionais de um ano via BrasilAPI de forma idempotente.
     * Em falha, mantém entradas em cache (fallback) e retorna 0.
     */
    @Transactional
    public int syncNationalHolidays(int year) {
        List<BrasilApiClient.NationalHoliday> remote;
        try {
            remote = brasilApi.fetchNationalHolidays(year);
        } catch (Exception e) {
            long cached = repository.countByYearAndScope(year, HolidayScope.NATIONAL);
            log.warn("BrasilAPI failed for year {} ({}). Using {} cached holiday(s).",
                    year, e.getMessage(), cached);
            return 0;
        }

        int upserts = 0;
        for (var nh : remote) {
            var existing = repository.findByDateAndScope(nh.date(), HolidayScope.NATIONAL).orElse(null);
            if (existing == null) {
                repository.save(new Holiday(nh.date(), nh.name(), HolidayScope.NATIONAL,
                        SOURCE_BRASILAPI, nh.type()));
                upserts++;
            } else if (!existing.getName().equals(nh.name())) {
                existing.setName(nh.name());
                existing.setType(nh.type());
                existing.setSource(SOURCE_BRASILAPI);
                repository.save(existing);
                upserts++;
            }
        }
        log.info("Holiday sync year={} fetched={} upserted={}", year, remote.size(), upserts);
        return upserts;
    }

    public int syncCurrentAndNextYear() {
        int y = Year.now().getValue();
        return syncNationalHolidays(y) + syncNationalHolidays(y + 1);
    }
}
