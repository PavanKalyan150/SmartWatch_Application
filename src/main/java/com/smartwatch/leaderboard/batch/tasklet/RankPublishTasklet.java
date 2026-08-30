package com.smartwatch.leaderboard.batch.tasklet;

import com.smartwatch.leaderboard.model.Challenge;
import com.smartwatch.leaderboard.model.Leaderboard;
import com.smartwatch.leaderboard.model.UserChallenge;
import com.smartwatch.leaderboard.model.enums.ChallengeStatus;
import com.smartwatch.leaderboard.model.enums.RewardScheme;
import com.smartwatch.leaderboard.model.enums.UserChallengeStatus;
import com.smartwatch.leaderboard.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankPublishTasklet implements Tasklet {

    private static final List<Integer> RANK_BASED_POINTS = List.of(100, 50, 25);

    private final ChallengeRepository challengeRepository;
    private final UserChallengeRepository userChallengeRepository;
    private final LeaderboardRepository leaderboardRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        challengeRepository.findByStatus(ChallengeStatus.EXPIRED)
                .forEach(this::processChallenge);
        return RepeatStatus.FINISHED;
    }

    private void processChallenge(Challenge challenge) {
        List<UserChallenge> participants = userChallengeRepository
                .findByChallengeIdAndStatus(challenge.getId(), UserChallengeStatus.RANKED)
                .stream()
                .sorted(rankingOrder())
                .toList();

        if (participants.isEmpty()) {
            log.warn("Challenge {} has no scored participants — marking RANKED anyway", challenge.getId());
            finalizeChallenge(challenge);
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        List<Leaderboard> leaderboardEntries = IntStream.range(0, participants.size())
                .mapToObj(i -> assignRankAndBuildEntry(participants.get(i), i + 1, challenge, now))
                .toList();

        leaderboardRepository.saveAll(leaderboardEntries);
        userChallengeRepository.saveAll(participants);
        finalizeChallenge(challenge);

        log.info("Challenge {} finalized with {} participants", challenge.getId(), participants.size());
    }

    private Leaderboard assignRankAndBuildEntry(UserChallenge uc, int rank, Challenge challenge, LocalDateTime now) {
        int points = resolvePoints(challenge.getRewardScheme(), rank);

        uc.setRank(rank);
        uc.setPointsAwarded(points);
        uc.setRankedAt(now);
        uc.setStatus(UserChallengeStatus.RANKED);

        if (points > 0) {
            userRepository.addPoints(uc.getUser().getId(), points);
        }

        log.info("Challenge {} | Rank {} | User {} | Score={} | Points={}",
                challenge.getId(), rank, uc.getUser().getId(), uc.getFinalScore(), points);

        return buildLeaderboardEntry(challenge, uc, rank, points, now);
    }

    private Leaderboard buildLeaderboardEntry(Challenge challenge, UserChallenge uc,
                                              int rank, int points, LocalDateTime now) {
        Leaderboard entry = new Leaderboard();
        entry.setChallenge(challenge);
        entry.setUser(uc.getUser());
        entry.setFinalScore(uc.getFinalScore());
        entry.setRank(rank);
        entry.setAwardedPoints(points);
        entry.setGeneratedAt(now);
        return entry;
    }

    private void finalizeChallenge(Challenge challenge) {
        challenge.setStatus(ChallengeStatus.RANKED);
        challengeRepository.save(challenge);
    }

    private Comparator<UserChallenge> rankingOrder() {
        return Comparator
                .comparingDouble(UserChallenge::getFinalScore).reversed()
                .thenComparing(uc -> uc.getRankedAt() != null ? uc.getRankedAt() : LocalDateTime.MAX);
    }

    private int resolvePoints(RewardScheme scheme, int rank) {
        if (scheme != RewardScheme.RANK_BASED) return 0;
        int idx = rank - 1;
        return idx < RANK_BASED_POINTS.size() ? RANK_BASED_POINTS.get(idx) : 0;
    }
}
