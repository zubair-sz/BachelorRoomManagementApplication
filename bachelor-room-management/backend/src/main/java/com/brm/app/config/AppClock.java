package com.brm.app.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;

/** Single place that decides what "today" means for the whole app (see app.timezone). */
@Component
public class AppClock {

    private final ZoneId zone;

    public AppClock(@Value("${app.timezone}") String timezone) {
        this.zone = ZoneId.of(timezone);
    }

    public ZoneId zone() {
        return zone;
    }

    public LocalDate today() {
        return LocalDate.now(zone);
    }
}
