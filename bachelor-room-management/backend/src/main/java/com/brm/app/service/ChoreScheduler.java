package com.brm.app.service;

import com.brm.app.config.AppClock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Generates the day's recurring-chore tasks just after midnight (app.timezone), and once on
 * startup so a restart that spanned midnight doesn't skip a day. Generation is idempotent.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChoreScheduler {

    private final ChoreService choreService;
    private final AppClock clock;

    @Scheduled(cron = "0 0 0 * * *", zone = "${app.timezone}")
    public void generateAtMidnight() {
        run("midnight run");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void catchUpOnStartup() {
        run("startup catch-up");
    }

    private void run(String trigger) {
        try {
            int created = choreService.generateDueTasks(clock.today());
            log.info("Chore scheduler ({}): created {} task(s)", trigger, created);
        } catch (RuntimeException e) {
            log.error("Chore scheduler ({}) failed", trigger, e);
        }
    }
}
