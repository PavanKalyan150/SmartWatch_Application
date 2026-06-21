package com.leaderboard.service;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class GameService {

    private final JobLauncher jobLauncher;
    private final Job gamificationJob;

    public GameService(JobLauncher jobLauncher, Job gamificationJob) {
        this.jobLauncher = jobLauncher;
        this.gamificationJob = gamificationJob;
    }

    public void runGamificationJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("timestamp", System.currentTimeMillis())
                .toJobParameters();
        jobLauncher.run(gamificationJob, params);
    }
}
