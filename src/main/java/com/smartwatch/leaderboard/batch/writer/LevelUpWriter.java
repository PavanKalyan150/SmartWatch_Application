package com.smartwatch.leaderboard.batch.writer;

import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LevelUpWriter implements ItemWriter<User> {

    private final UserRepository userRepository;

    @Override
    public void write(Chunk<? extends User> chunk) {
        userRepository.saveAll(chunk.getItems());
        log.info("Level-up applied to {} user(s)", chunk.size());
    }
}

