package br.com.ecowinds.repository;

import br.com.ecowinds.model.AcSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

@Repository
public interface AcScheduleRepository extends JpaRepository<AcSchedule, Long> {

    List<AcSchedule> findByDayOfWeekAndEnabledTrue(DayOfWeek dayOfWeek);

    List<AcSchedule> findAllByOrderByDayOfWeekAscTimeAsc();
}
