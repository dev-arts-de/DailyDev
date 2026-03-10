package de.shopitech.job;

import de.shopitech.service.DailyTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyTaskJob {

    private final DailyTaskService dailyTaskService;

    @Scheduled(initialDelayString = "${dailyDev.task.initialDelay}", fixedDelayString = "${dailyDev.task.fixedDelay}")
    private void runDailyTaks() {
        dailyTaskService.executeDailyTask();
    }
}
