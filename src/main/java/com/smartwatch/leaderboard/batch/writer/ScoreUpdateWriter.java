package com.smartwatch.leaderboard.batch.writer;

import com.smartwatch.leaderboard.dto.batch.ScoredParticipant;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import com.smartwatch.leaderboard.repository.UserChallengeRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ScoreUpdateWriter implements ItemWriter<ScoredParticipant> {

    private final UserChallengeRepository userChallengeRepository;

    @Override
    public void write(Chunk<? extends ScoredParticipant> chunk) {
        for (ScoredParticipant sp : chunk) {
            sp.getUserChallenge().setFinalScore(sp.getFinalScore());
            sp.getUserChallenge().setStatus(UserChallengeStatus.RANKED);
        }
        userChallengeRepository.saveAll(
                chunk.getItems().stream().map(ScoredParticipant::getUserChallenge).toList()
        );
        log.debug("Persisted scores for {} participants", chunk.size());
    }
}