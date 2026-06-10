package br.com.ecowinds.service.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MqttCommandPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttCommandPublisher.class);

    @Value("${mqtt.topic.command}")
    private String commandTopic;

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    public MqttCommandPublisher(@Qualifier("mqttOutboundChannel") MessageChannel mqttOutboundChannel,
                                ObjectMapper objectMapper) {
        this.mqttOutboundChannel = mqttOutboundChannel;
        this.objectMapper = objectMapper;
    }

    public void publishCommand(String action) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("action", action));
            mqttOutboundChannel.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, commandTopic)
                            .setHeader(MqttHeaders.QOS, 1)
                            .build()
            );
            log.info("[mqtt] published command={} topic={}", action, commandTopic);
        } catch (Exception e) {
            log.error("[mqtt] failed to publish command: {}", e.getMessage());
        }
    }
}
