package br.com.ecowinds.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

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

    public EspDevice() {
    }

    @Column(name = "air_on", nullable = false)
    private Boolean airOn;

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
