package br.com.ecowinds.service.sigeho;

import br.com.ecowinds.model.ClassSchedule;
import br.com.ecowinds.model.Room;
import br.com.ecowinds.model.ScheduleImport;
import br.com.ecowinds.model.enums.RoomStatus;
import br.com.ecowinds.repository.ClassScheduleRepository;
import br.com.ecowinds.repository.RoomRepository;
import br.com.ecowinds.service.sigeho.dto.ParsedImport;
import br.com.ecowinds.service.sigeho.dto.ParsedRoom;
import br.com.ecowinds.service.sigeho.dto.ParsedSchedule;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SigehoPersistenceService {

    static final String DEFAULT_BLOCK = "UNKNOWN";

    private final RoomRepository roomRepository;
    private final ClassScheduleRepository classScheduleRepository;

    public SigehoPersistenceService(RoomRepository roomRepository,
                                    ClassScheduleRepository classScheduleRepository) {
        this.roomRepository = roomRepository;
        this.classScheduleRepository = classScheduleRepository;
    }

    @Transactional
    public void apply(ParsedImport parsed, ScheduleImport rec) {
        int roomsAffected = 0, roomsCreated = 0, schedulesCreated = 0, schedulesDeleted = 0;

        for (ParsedRoom pr : parsed.rooms()) {
            Room room = roomRepository.findByIdentificationIgnoreCase(pr.identification())
                    .orElse(null);
            if (room == null) {
                room = new Room();
                room.setIdentification(pr.identification());
                room.setBlock(pr.block() == null || pr.block().isBlank() ? DEFAULT_BLOCK : pr.block());
                room.setStatus(RoomStatus.ACTIVE);
                room = roomRepository.save(room);
                roomsCreated++;
            } else if (pr.block() != null && !pr.block().isBlank()
                    && (room.getBlock() == null || DEFAULT_BLOCK.equals(room.getBlock()))) {
                room.setBlock(pr.block());
                room = roomRepository.save(room);
            }

            schedulesDeleted += classScheduleRepository.deleteByRoomId(room.getId());

            for (ParsedSchedule ps : pr.schedules()) {
                classScheduleRepository.save(new ClassSchedule(
                        ps.weekday(), ps.startTime(), ps.endTime(), ps.course(), room));
                schedulesCreated++;
            }
            roomsAffected++;
        }

        rec.setRoomsAffected(roomsAffected);
        rec.setRoomsCreated(roomsCreated);
        rec.setSchedulesCreated(schedulesCreated);
        rec.setSchedulesDeleted(schedulesDeleted);
    }
}
