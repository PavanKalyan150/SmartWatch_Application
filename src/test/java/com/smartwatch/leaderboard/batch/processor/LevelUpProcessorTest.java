package com.smartwatch.leaderboard.batch.processor;

import com.smartwatch.leaderboard.model.Level;
import com.smartwatch.leaderboard.model.User;
import com.smartwatch.leaderboard.repository.LevelRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LevelUpProcessorTest {

    @Mock private LevelRepository levelRepository;

    @InjectMocks private LevelUpProcessor processor;

    // Levels in descending threshold order (matches what the processor expects)
    private Level platinum;   // 5000
    private Level gold;       // 2000
    private Level silver;     // 500
    private Level bronze;     // 0

    @BeforeEach
    void setUp() {
        platinum = Level.builder().id(4L).levelName("PLATINUM").pointThreshold(5000).build();
        gold     = Level.builder().id(3L).levelName("GOLD").pointThreshold(2000).build();
        silver   = Level.builder().id(2L).levelName("SILVER").pointThreshold(500).build();
        bronze   = Level.builder().id(1L).levelName("BRONZE").pointThreshold(0).build();
    }

    private void primeLevels(List<Level> levels) {
        when(levelRepository.findAllByOrderByPointThresholdDesc()).thenReturn(levels);
        processor.loadLevels(null); // simulate Spring Batch's @BeforeStep callback
    }

    private User userWith(Long id, Level currentLevel, int pointsBalance) {
        return User.builder()
                .id(id)
                .level(currentLevel)
                .pointsBalance(pointsBalance)
                .build();
    }

    // ---------- loadLevels (@BeforeStep) ----------

    @Nested
    class LoadLevels {

        @Test
        void shouldLoadLevelsFromRepositoryOnBeforeStep() {
            // Without loading levels, process() would NPE on the stream.
            // Loading is what wires the processor for actual work.
            primeLevels(List.of(platinum, gold, silver, bronze));

            // Sanity check: with levels loaded, processor functions normally.
            User user = userWith(1L, bronze, 600);
            assertThat(processor.process(user)).isNotNull();
        }

        @Test
        void shouldHandleEmptyLevelsListGracefully() {
            primeLevels(Collections.emptyList());

            User user = userWith(1L, bronze, 1000);
            // No levels => no qualifying level => no promotion (returns null)
            assertThat(processor.process(user)).isNull();
        }
    }

    // ---------- process: promotions ----------

    @Nested
    class Promotions {

        @BeforeEach
        void primeStandardLevels() {
            primeLevels(List.of(platinum, gold, silver, bronze));
        }

        @Test
        void shouldPromoteFromBronzeToSilverWhenJustOverThreshold() {
            User user = userWith(1L, bronze, 500);

            User result = processor.process(user);

            assertThat(result).isNotNull();
            assertThat(result.getLevel()).isSameAs(silver);
        }

        @Test
        void shouldPromoteFromBronzeToGoldSkippingSilver() {
            // User accumulated enough points to skip a level — should jump straight to gold
            User user = userWith(1L, bronze, 2500);

            User result = processor.process(user);

            assertThat(result).isNotNull();
            assertThat(result.getLevel()).isSameAs(gold);
        }

        @Test
        void shouldPromoteToHighestQualifyingLevelWhenMultipleMatch() {
            // 6000 qualifies for platinum, gold, silver, bronze — must pick platinum
            User user = userWith(1L, bronze, 6000);

            User result = processor.process(user);

            assertThat(result.getLevel()).isSameAs(platinum);
        }

        @Test
        void shouldPromoteFromSilverToGoldExactlyAtThreshold() {
            // Boundary: pointsBalance == threshold should qualify (>=)
            User user = userWith(1L, silver, 2000);

            User result = processor.process(user);

            assertThat(result.getLevel()).isSameAs(gold);
        }
    }

    // ---------- process: no promotion ----------

    @Nested
    class NoPromotion {

        @BeforeEach
        void primeStandardLevels() {
            primeLevels(List.of(platinum, gold, silver, bronze));
        }

        @Test
        void shouldReturnNullWhenUserAlreadyAtCorrectLevel() {
            // Silver user with 600 points — silver is correct, no promotion needed
            User user = userWith(1L, silver, 600);

            assertThat(processor.process(user)).isNull();
        }

        @Test
        void shouldReturnNullWhenUserAlreadyAtMaxLevel() {
            User user = userWith(1L, platinum, 9999);

            assertThat(processor.process(user)).isNull();
        }

        @Test
        void shouldReturnNullWhenJustBelowNextThreshold() {
            // 1999 doesn't qualify for gold (2000), still silver
            User user = userWith(1L, silver, 1999);

            assertThat(processor.process(user)).isNull();
        }

        @Test
        void shouldReturnNullForBronzeUserAtZeroPoints() {
            // Brand new user — bronze threshold is 0, balance is 0, still bronze
            User user = userWith(1L, bronze, 0);

            assertThat(processor.process(user)).isNull();
        }
    }

    // ---------- process: edge cases ----------

    @Nested
    class EdgeCases {

        @Test
        void shouldNotMutateUserWhenReturningNull() {
            // Critical: Spring Batch skips writing null returns. But if we accidentally
            // mutated the user before deciding to return null, the change would leak
            // back to the persistence context. Lock this down.
            primeLevels(List.of(platinum, gold, silver, bronze));
            User user = userWith(1L, silver, 600);

            processor.process(user);

            assertThat(user.getLevel()).isSameAs(silver); // unchanged
        }

        @Test
        void shouldMutateUserInPlaceWhenPromoting() {
            // The processor mutates the input and returns the same instance —
            // it doesn't create a copy. This matters for JPA dirty-checking.
            primeLevels(List.of(platinum, gold, silver, bronze));
            User user = userWith(1L, bronze, 2500);

            User result = processor.process(user);

            assertThat(result).isSameAs(user); // same reference, not a copy
            assertThat(user.getLevel()).isSameAs(gold);
        }

        @Test
        void shouldHandleSingleLevelConfiguration() {
            // What if there's only one level configured? Everyone's at it.
            primeLevels(List.of(bronze));
            User user = userWith(1L, bronze, 100_000);

            assertThat(processor.process(user)).isNull();
        }
    }
}
