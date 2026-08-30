package com.smartwatch.leaderboard.kafka;

import com.smartwatch.leaderboard.dto.kafka.ActivityEventMessage;
import com.smartwatch.leaderboard.repository.UserActivityEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ActivityEventConsumer {

    private final UserActivityEventRepository activityEventRepository;
    private final ActivityEventProcessingService processingService;

    @KafkaListener(
            topics = "events",
            groupId = "consumer-group"
    )
    public void consume(ActivityEventMessage message) {
        if (activityEventRepository.existsByEventId(message.getEventId())) {
            log.info("Duplicate event skipped [eventId={}]", message.getEventId());
            return;
        }

        try {
            processingService.process(message);
        } catch (Exception ex) {
            log.error("Unhandled failure processing event [eventId={}] — marking DEAD_LETTER: {}",
                    message.getEventId(), ex.getMessage(), ex);
            processingService.markDeadLetter(message);
        }
    }
}