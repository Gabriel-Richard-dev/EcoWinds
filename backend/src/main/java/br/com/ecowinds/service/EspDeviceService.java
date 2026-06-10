package br.com.ecowinds.service;

import br.com.ecowinds.dto.espDevice.EspDeviceDTO;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.model.Room;
import br.com.ecowinds.repository.EspDeviceRepository;
import br.com.ecowinds.repository.RoomRepository;
import br.com.ecowinds.service.mqtt.MqttCommandPublisher;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class EspDeviceService {

    private final EspDeviceRepository espDeviceRepository;
    private final RoomRepository roomRepository;
    private final MqttCommandPublisher mqttCommandPublisher;

    public EspDeviceService(EspDeviceRepository espDeviceRepository,
                            RoomRepository roomRepository,
                            @Lazy MqttCommandPublisher mqttCommandPublisher) {
        this.espDeviceRepository = espDeviceRepository;
        this.roomRepository = roomRepository;
        this.mqttCommandPublisher = mqttCommandPublisher;
    }

    @Transactional(readOnly = true)
    public Page<EspDeviceDTO> searchPageable(String searchTerm, int page, int size) {
        String term = (searchTerm == null) ? "" : searchTerm;
        Pageable pageable = PageRequest.of(page, size, Sort.by("ipAddress").ascending());

        return espDeviceRepository.search(searchTerm, pageable).map(EspDeviceDTO::new);
    }

    @Transactional(readOnly = true)
    public EspDeviceDTO findById(Long id) {
        return espDeviceRepository.findById(id)
                .map(EspDeviceDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("EspDevice not found"));
    }

    @Transactional
    public EspDeviceDTO save(EspDeviceDTO dto) {
        EspDevice device = new EspDevice();
        DtoToEntity(dto, device);
        return new EspDeviceDTO(espDeviceRepository.save(device));
    }

    @Transactional
    public EspDeviceDTO update(Long id, EspDeviceDTO dto) {
        EspDevice device = espDeviceRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Device not found"));
        DtoToEntity(dto, device);
        return new EspDeviceDTO(espDeviceRepository.save(device));
    }

    @Transactional
    public void delete(Long id) {
        if (!espDeviceRepository.existsById(id)) {
            throw new EntityNotFoundException("Device not found");
        }
        espDeviceRepository.deleteById(id);
    }

    private void DtoToEntity(EspDeviceDTO dto, EspDevice entity) {
        entity.setMacAddress(dto.macAddress());
        entity.setIpAddress(dto.ipAddress());
        entity.setConnectionStatus(dto.connectionStatus());
        entity.setInfraredFrequency(dto.infraredFrequency());

        if (dto.roomId() != null) {
            Room room = roomRepository.findById(dto.roomId())
                    .orElseThrow(() -> new EntityNotFoundException("Room not found"));
            entity.setRoom(room);
        }
    }

    @Transactional(readOnly = true)
    public EspDeviceDTO findFirst() {
        return espDeviceRepository.findTopByOrderByIdAsc()
                .map(EspDeviceDTO::new)
                .orElseThrow(() -> new EntityNotFoundException("No device registered"));
    }

    @Transactional
    public void swipeOnOff(Long espId) {
        EspDevice esp = espDeviceRepository.findById(espId)
                .orElseThrow(() -> new EntityNotFoundException("Not found"));
        boolean newState = !esp.getAirOn();
        esp.setAirOn(newState);
        espDeviceRepository.save(esp);
        mqttCommandPublisher.publishCommand(newState ? "ON" : "OFF");
    }

}
