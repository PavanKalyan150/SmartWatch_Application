package com.leaderboard.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class RankService {

    private final JobLauncher jobLauncher;
    private final Job rankingJob;

    public RankService(JobLauncher jobLauncher, Job rankingJob) {
        this.jobLauncher = jobLauncher;
        this.rankingJob = rankingJob;
    }

    public void runRankingJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(rankingJob, params);
    }
}
