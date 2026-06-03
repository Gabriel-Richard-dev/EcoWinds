package br.com.ecowinds.repository;

import br.com.ecowinds.model.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    @Query("SELECT r FROM Room r WHERE " +
            "LOWER(r.identification) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(r.block) LIKE LOWER(CONCAT('%', :searchTerm, '%'))"
    )
    Page<Room> search(
            @Param("searchTerm") String searchTerm,
            Pageable pageable
    );

    Optional<Room> findByIdentificationIgnoreCase(String identification);
}
