package br.com.ecowinds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "esp_devices")
@AllArgsConstructor
@Getter
@Setter
public class EspDevice extends BaseEntity {

    @Column(name = "mac_address", nullable = false, unique = true)
    private String macAddress;

    @Column(name = "ip_address", nullable = false)
    private String ipAddress;

    @Column(name = "connection_status", nullable = false)
    private Boolean connectionStatus;

    @Column(name = "infrared_frequency", nullable = false)
    private String infraredFrequency;

    @OneToOne
    @JoinColumn(name = "room_id", referencedColumnName = "id")
    private Room room;

    @Column(name = "api_key_hash", length = 128)
    private String apiKeyHash;

    @Column(name = "last_heartbeat_at")
    private LocalDateTime lastHeartbeatAt;

    public String getApiKeyHash() { return apiKeyHash; }
    public void setApiKeyHash(String apiKeyHash) { this.apiKeyHash = apiKeyHash; }
    public LocalDateTime getLastHeartbeatAt() { return lastHeartbeatAt; }
    public void setLastHeartbeatAt(LocalDateTime lastHeartbeatAt) { this.lastHeartbeatAt = lastHeartbeatAt; }

    public EspDevice() {
    }

    @Column(name = "air_on", nullable = false)
    private Boolean airOn;

    @Column(name = "temperature", precision = 5, scale = 2)
    private BigDecimal temperature;

    @Column(name = "target_temperature")
    private Integer targetTemperature;

    public Boolean getAirOn() {
        return airOn;
    }

    public void setAirOn(Boolean airOn) {
        this.airOn = airOn;
    }

    public EspDevice(String macAddress, String ipAddress, Boolean connectionStatus, String infraredFrequency, Room room) {
        this.macAddress = macAddress;
        this.ipAddress = ipAddress;
        this.connectionStatus = connectionStatus;
        this.infraredFrequency = infraredFrequency;
        this.room = room;
    }



    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public Boolean getConnectionStatus() {
        return connectionStatus;
    }

    public void setConnectionStatus(Boolean connectionStatus) {
        this.connectionStatus = connectionStatus;
    }

    public String getInfraredFrequency() {
        return infraredFrequency;
    }

    public void setInfraredFrequency(String infraredFrequency) {
        this.infraredFrequency = infraredFrequency;
    }

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public BigDecimal getTemperature() { return temperature; }
    public void setTemperature(BigDecimal temperature) { this.temperature = temperature; }

    public Integer getTargetTemperature() { return targetTemperature; }
    public void setTargetTemperature(Integer targetTemperature) { this.targetTemperature = targetTemperature; }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        EspDevice espDevice = (EspDevice) o;
        return Objects.equals(getMacAddress(), espDevice.getMacAddress()) && Objects.equals(getIpAddress(), espDevice.getIpAddress()) && Objects.equals(getConnectionStatus(), espDevice.getConnectionStatus()) && Objects.equals(getInfraredFrequency(), espDevice.getInfraredFrequency()) && Objects.equals(getRoom(), espDevice.getRoom());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getMacAddress(), getIpAddress(), getConnectionStatus(), getInfraredFrequency(), getRoom());
    }
}
