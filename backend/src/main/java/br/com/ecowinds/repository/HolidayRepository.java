package br.com.ecowinds.repository;

import br.com.ecowinds.model.Holiday;
import br.com.ecowinds.model.enums.HolidayScope;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    Optional<Holiday> findByDateAndScope(LocalDate date, HolidayScope scope);

    List<Holiday> findByDate(LocalDate date);

    @Query("SELECT h FROM Holiday h WHERE h.date >= :start AND h.date <= :end ORDER BY h.date")
    List<Holiday> findInRange(@Param("start") LocalDate start, @Param("end") LocalDate end);

    @Query("SELECT COUNT(h) FROM Holiday h WHERE FUNCTION('year', h.date) = :year AND h.scope = :scope")
    long countByYearAndScope(@Param("year") int year, @Param("scope") HolidayScope scope);
}
