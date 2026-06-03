package br.com.ecowinds.repository;

import br.com.ecowinds.model.ScheduleImport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ScheduleImportRepository extends JpaRepository<ScheduleImport, Long> {

    Optional<ScheduleImport> findFirstByFileHashOrderByStartedAtDesc(String fileHash);

    Page<ScheduleImport> findAllByOrderByStartedAtDesc(Pageable pageable);
}
