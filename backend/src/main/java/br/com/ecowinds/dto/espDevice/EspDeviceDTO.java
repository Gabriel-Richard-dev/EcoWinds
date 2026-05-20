package br.com.ecowinds.dto.espDevice;

import br.com.ecowinds.model.EspDevice;

public record EspDeviceDTO(
        Long id,
        String macAddress,
        String ipAddress,
        Boolean connectionStatus,
        String infraredFrequency,
        Long roomId,
        Boolean airOn
) {
    public EspDeviceDTO(EspDevice espDevice) {
        this(
                espDevice.getId(),
                espDevice.getMacAddress(),
                espDevice.getIpAddress(),
                espDevice.getConnectionStatus(),
                espDevice.getInfraredFrequency(),
                espDevice.getRoom() != null ? espDevice.getRoom().getId() : null,
                espDevice.getAirOn()
        );
    }
}
