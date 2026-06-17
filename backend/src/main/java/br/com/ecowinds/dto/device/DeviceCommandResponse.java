package br.com.ecowinds.dto.device;

import java.time.LocalDateTime;

public record DeviceCommandResponse(String action, LocalDateTime serverTime) {}
