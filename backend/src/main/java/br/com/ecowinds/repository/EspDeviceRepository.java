package br.com.ecowinds.repository;

import br.com.ecowinds.model.EspDevice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface EspDeviceRepository extends JpaRepository<EspDevice, Long> {

    Optional<EspDevice> findByApiKeyHash(String apiKeyHash);

    @Modifying
    @Query("UPDATE EspDevice d SET d.connectionStatus = false "
            + "WHERE d.connectionStatus = true "
            + "AND (d.lastHeartbeatAt IS NULL OR d.lastHeartbeatAt < :threshold)")
    int markStaleOffline(@Param("threshold") LocalDateTime threshold);


    @Query("SELECT ed FROM EspDevice ed WHERE " +
            "LOWER(ed.ipAddress) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    Page<EspDevice> search(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );
}
