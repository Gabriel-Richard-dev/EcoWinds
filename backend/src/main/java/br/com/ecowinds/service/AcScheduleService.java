package br.com.ecowinds.service;

import br.com.ecowinds.dto.acSchedule.AcScheduleDTO;
import br.com.ecowinds.model.AcSchedule;
import br.com.ecowinds.repository.AcScheduleRepository;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AcScheduleService {

    private final AcScheduleRepository repository;

    public AcScheduleService(AcScheduleRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<AcScheduleDTO> findAll() {
        return repository.findAllByOrderByDayOfWeekAscTimeAsc()
                .stream().map(AcScheduleDTO::new).toList();
    }

    @Transactional(readOnly = true)
    public AcScheduleDTO findById(Long id) {
        return repository.findById(id)
                .map(AcScheduleDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("AcSchedule not found"));
    }

    @Transactional
    public AcScheduleDTO save(AcScheduleDTO dto) {
        AcSchedule entity = new AcSchedule();
        mapToEntity(dto, entity);
        return new AcScheduleDTO(repository.save(entity));
    }

    @Transactional
    public AcScheduleDTO update(Long id, AcScheduleDTO dto) {
        AcSchedule entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("AcSchedule not found"));
        mapToEntity(dto, entity);
        return new AcScheduleDTO(repository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        if (!repository.existsById(id)) {
            throw new EntityNotFoundException("AcSchedule not found");
        }
        repository.deleteById(id);
    }

    private void mapToEntity(AcScheduleDTO dto, AcSchedule entity) {
        entity.setDayOfWeek(dto.dayOfWeek());
        entity.setTime(dto.time());
        entity.setAction(dto.action());
        entity.setEnabled(dto.enabled() != null ? dto.enabled() : true);
    }
}
