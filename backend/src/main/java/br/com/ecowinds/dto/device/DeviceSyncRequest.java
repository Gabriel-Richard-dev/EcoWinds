package br.com.ecowinds.dto.device;

import java.math.BigDecimal;

public record DeviceSyncRequest(Boolean airOn, BigDecimal temperature, Integer rssi, Long uptime, String ip) {}
