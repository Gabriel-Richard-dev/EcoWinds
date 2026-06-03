package br.com.ecowinds.service;

import br.com.ecowinds.dto.classSchedule.ClassScheduleDTO;
import br.com.ecowinds.dto.classSchedule.NextScheduleResponse;
import br.com.ecowinds.model.ClassSchedule;
import br.com.ecowinds.model.Room;
import br.com.ecowinds.repository.ClassScheduleRepository;
import br.com.ecowinds.repository.RoomRepository;
import br.com.ecowinds.service.holiday.HolidayService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class ClassScheduleService {

    private static final int LOOKAHEAD_DAYS = 14;

    private final ClassScheduleRepository classScheduleRepository;
    private final RoomRepository roomRepository;
    private final HolidayService holidayService;

    public ClassScheduleService(ClassScheduleRepository classScheduleRepository,
                                RoomRepository roomRepository,
                                HolidayService holidayService) {
        this.classScheduleRepository = classScheduleRepository;
        this.roomRepository = roomRepository;
        this.holidayService = holidayService;
    }

    @Transactional(readOnly = true)
    public Page<ClassScheduleDTO> searchPageable(String searchTerm, int page, int size) {
        String term = (searchTerm == null) ? "" : searchTerm;
        Pageable pageable = PageRequest.of(page, size, Sort.by("course").ascending());

        return classScheduleRepository.search(term, pageable).map(ClassScheduleDTO::new);
    }

    @Transactional(readOnly = true)
    public ClassScheduleDTO findById(Long id) {
        return classScheduleRepository.findById(id)
                .map(ClassScheduleDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("ClassSchedule not found"));
    }

    @Transactional
    public ClassScheduleDTO save(ClassScheduleDTO dto) {
        ClassSchedule classSchedule = new ClassSchedule();
        DtoToEntity(dto, classSchedule);

        return new ClassScheduleDTO(classScheduleRepository.save(classSchedule));
    }

    @Transactional
    public ClassScheduleDTO update(Long id, ClassScheduleDTO dto) {
        ClassSchedule classSchedule = classScheduleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("ClassSchedule not found"));

        DtoToEntity(dto, classSchedule);
        return new ClassScheduleDTO(classScheduleRepository.save(classSchedule));
    }

    @Transactional
    public void delete(Long id) {
        if (!classScheduleRepository.existsById(id)) {
            throw new EntityNotFoundException("ClassSchedule not found");
        }

        classScheduleRepository.deleteById(id);
    }

    private void DtoToEntity(ClassScheduleDTO dto, ClassSchedule entity) {
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setStartTime(dto.startTime());
        entity.setEndTime(dto.endTime());
        entity.setCourse(dto.course());

        if (dto.roomId() != null) {
            Room room = roomRepository.findById(dto.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            entity.setRoom(room);
        }
    }



    @Transactional(readOnly = true)
    public NextScheduleResponse nextSchedule(Long espDeviceId) {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        for (int offset = 0; offset < LOOKAHEAD_DAYS; offset++) {
            LocalDate probe = today.plusDays(offset);
            if (!holidayService.findByDate(probe).isEmpty()) {
                continue;
            }
            LocalTime threshold = (offset == 0) ? now : LocalTime.MIN;
            ClassSchedule next = classScheduleRepository.GetNext(
                    espDeviceId, probe.getDayOfWeek(), threshold);
            if (next != null) {
                return new NextScheduleResponse(probe, new ClassScheduleDTO(next));
            }
        }
        return null;
    }
}
