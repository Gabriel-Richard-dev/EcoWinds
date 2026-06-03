package br.com.ecowinds.repository;

import br.com.ecowinds.model.ClassSchedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ClassScheduleRepository extends JpaRepository<ClassSchedule, Long> {

    @Query("SELECT cs FROM ClassSchedule cs WHERE " +
            "LOWER(cs.course) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(cs.room.identification) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    Page<ClassSchedule> search(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );


    @Query("""
    FROM ClassSchedule c INNER JOIN c.room r WHERE r.espDevice.id = :EspId
    AND c.dayOfWeek = :DayofWeek
    AND c.startTime >= :LocalHour
    ORDER BY c.startTime ASC LIMIT 1
        """)
    ClassSchedule GetNext(
            @Param("EspId") Long id,
            @Param("DayofWeek")DayOfWeek diaSemana,
            @Param("LocalHour")LocalTime horaAtual
            );

    @Modifying
    @Query("DELETE FROM ClassSchedule cs WHERE cs.room.id = :roomId")
    int deleteByRoomId(@Param("roomId") Long roomId);

    List<ClassSchedule> findByRoomIdOrderByDayOfWeekAscStartTimeAsc(Long roomId);
}
