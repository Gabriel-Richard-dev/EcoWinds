package br.com.ecowinds.service.mqtt;

import br.com.ecowinds.model.AuditLog;
import br.com.ecowinds.model.EspDevice;
import br.com.ecowinds.repository.AuditLogRepository;
import br.com.ecowinds.repository.EspDeviceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class MqttInboundHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttInboundHandler.class);

    @Value("${mqtt.topic.telemetry}")
    private String telemetryTopic;

    @Value("${mqtt.topic.heartbeat}")
    private String heartbeatTopic;

    @Value("${mqtt.topic.log}")
    private String logTopic;

    private final EspDeviceRepository deviceRepository;
    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public MqttInboundHandler(EspDeviceRepository deviceRepository,
                               AuditLogRepository auditLogRepository,
                               ObjectMapper objectMapper) {
        this.deviceRepository = deviceRepository;
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    @Transactional
    public void handleMessage(Message<?> message) {
        String topic = (String) message.getHeaders().get("mqtt_receivedTopic");
        String payload = message.getPayload().toString();
        log.debug("[mqtt] received topic={} payload={}", topic, payload);

        try {
            if (telemetryTopic.equals(topic)) {
                handleTelemetry(payload);
            } else if (heartbeatTopic.equals(topic)) {
                handleHeartbeat(payload);
            } else if (logTopic.equals(topic)) {
                handleLog(payload);
            }
        } catch (Exception e) {
            log.error("[mqtt] error processing message on topic={}: {}", topic, e.getMessage());
        }
    }

    private void handleTelemetry(String payload) throws Exception {
        JsonNode node = objectMapper.readTree(payload);
        EspDevice device = findDevice();
        if (device == null) return;

        if (node.has("air_on")) {
            device.setAirOn(node.get("air_on").asBoolean());
        }
        if (node.has("temperature") && !node.get("temperature").isNull()) {
            device.setTemperature(new BigDecimal(node.get("temperature").asText()));
        }
        deviceRepository.save(device);
        log.debug("[mqtt] telemetry updated air_on={} temperature={}", device.getAirOn(), device.getTemperature());
    }

    private void handleHeartbeat(String payload) {
        EspDevice device = findDevice();
        if (device == null) return;

        device.setConnectionStatus(true);
        device.setLastHeartbeatAt(LocalDateTime.now());
        deviceRepository.save(device);
        log.debug("[mqtt] heartbeat received, device marked online");
    }

    private void handleLog(String payload) throws Exception {
        EspDevice device = findDevice();
        JsonNode node = objectMapper.readTree(payload);
        String action = node.has("action") ? node.get("action").asText() : "UNKNOWN";
        String detail = node.has("detail") ? node.get("detail").asText() : null;

        String origin = detail != null ? "ESP32:" + detail : "ESP32";
        AuditLog entry = new AuditLog(
                LocalDateTime.now(), action, origin,
                null,
                device != null ? device.getRoom() : null,
                device
        );
        auditLogRepository.save(entry);
    }

    private EspDevice findDevice() {
        List<EspDevice> devices = deviceRepository.findAll();
        if (devices.isEmpty()) {
            log.warn("[mqtt] no device registered, ignoring message");
            return null;
        }
        return devices.get(0);
    }
}
