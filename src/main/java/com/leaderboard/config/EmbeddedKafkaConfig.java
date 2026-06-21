package com.leaderboard.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.test.EmbeddedKafkaBroker;

import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class EmbeddedKafkaConfig {

    @Bean
    public EmbeddedKafkaBroker embeddedKafkaBroker() {
        // Run embedded Kafka broker with 1 broker instance, controlled shutdown, and our telemetry topic
        org.springframework.kafka.test.EmbeddedKafkaZKBroker broker = 
            new org.springframework.kafka.test.EmbeddedKafkaZKBroker(1, true, "smartwatch-telemetry");
        broker.kafkaPorts(9092);
        return broker;
    }
}
