package com.smartwatch.leaderboard.service;

import com.smartwatch.leaderboard.dto.response.HealthResponse;
import com.smartwatch.leaderboard.dto.response.HealthResponse.ComponentHealth;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.clients.admin.DescribeClusterResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

@Service
public class HealthService {

    private static final Logger log = LoggerFactory.getLogger(HealthService.class);
    private static final int DB_VALIDATION_TIMEOUT_SECONDS = 2;
    private static final int KAFKA_TIMEOUT_MS = 3000;

    private final DataSource dataSource;
    private final String kafkaBootstrapServers;

    public HealthService(DataSource dataSource,
                         @Value("${spring.kafka.bootstrap-servers:localhost:9092}") String kafkaBootstrapServers) {
        this.dataSource = dataSource;
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public HealthResponse checkHealth() {
        Map<String, ComponentHealth> components = new LinkedHashMap<>();
        components.put("database", checkDatabase());
        components.put("kafka", checkKafka());

        boolean allUp = components.values().stream()
                .allMatch(c -> "UP".equals(c.getStatus()));

        return new HealthResponse(
                allUp ? "UP" : "DOWN",
                Instant.now(),
                components
        );
    }

    ComponentHealth checkDatabase() {
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(DB_VALIDATION_TIMEOUT_SECONDS);
            return valid
                    ? new ComponentHealth("UP", null)
                    : new ComponentHealth("DOWN", "Connection validation failed");
        } catch (Exception e) {
            log.warn("Database health check failed: {}", e.getMessage());
            return new ComponentHealth("DOWN", e.getMessage());
        }
    }

    ComponentHealth checkKafka() {
        Properties props = new Properties();
        props.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, kafkaBootstrapServers);
        props.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, KAFKA_TIMEOUT_MS);
        props.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, KAFKA_TIMEOUT_MS);

        try (AdminClient admin = AdminClient.create(props)) {
            DescribeClusterResult cluster = admin.describeCluster();
            String clusterId = cluster.clusterId().get(KAFKA_TIMEOUT_MS, TimeUnit.MILLISECONDS);
            int nodeCount = cluster.nodes().get(KAFKA_TIMEOUT_MS, TimeUnit.MILLISECONDS).size();
            return new ComponentHealth("UP", "clusterId=" + clusterId + ", nodes=" + nodeCount);
        } catch (Exception e) {
            log.warn("Kafka health check failed: {}", e.getMessage());
            return new ComponentHealth("DOWN", e.getMessage());
        }
    }
}