package com.leaderboard.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.leaderboard.dto.UserTelemetryMessage;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class KafkaProducerService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private static final String TOPIC = "smartwatch-telemetry";

    public KafkaProducerService(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    public void sendTelemetry(UserTelemetryMessage message) {
        try {
            String jsonPayload = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(TOPIC, message.getUserId().toString(), jsonPayload);
        } catch (Exception e) {
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}
