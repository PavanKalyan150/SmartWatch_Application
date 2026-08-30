package com.smartwatch.leaderboard.batch.processor;

import com.smartwatch.leaderboard.model.Level;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.repository.LevelRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.annotation.BeforeStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class LevelUpProcessor implements ItemProcessor<User, User> {

    private final LevelRepository levelRepository;

    // Loaded once per step — highest threshold first so the first match is always the correct level
    private List<Level> levelsDesc;

    @BeforeStep
    public void loadLevels(StepExecution stepExecution) {
        levelsDesc = levelRepository.findAllByOrderByPointThresholdDesc();
        log.info("Loaded {} levels for evaluation", levelsDesc.size());
    }

    @Override
    public User process(User user) {
        Level qualified = levelsDesc.stream()
                .filter(l -> user.getPointsBalance() >= l.getPointThreshold())
                .findFirst()
                .orElse(null);

        if (qualified == null || qualified.getId().equals(user.getLevel().getId())) {
            return null; // no change — Spring Batch skips null returns
        }

        log.debug("User {} promoted: {} → {} (balance={})",
                user.getId(), user.getLevel().getLevelName(),
                qualified.getLevelName(), user.getPointsBalance());

        user.setLevel(qualified);
        return user;
    }
}