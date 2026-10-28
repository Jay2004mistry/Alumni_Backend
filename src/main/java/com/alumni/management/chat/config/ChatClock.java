package com.alumni.management.chat.config;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/**
 * Authoritative Clock provider for chat message timestamps.
 * Continuously synchronizes with network time to eliminate host system clock skew/drift
 * (e.g. host clock lagging behind real-world mobile client clocks).
 */
@Component
public class ChatClock {

    private static final Logger log = LoggerFactory.getLogger(ChatClock.class);

    private static final String[] TIME_SERVERS = {
        "https://www.google.com",
        "https://www.cloudflare.com",
        "https://aws.amazon.com"
    };

    private static volatile long clockSkewMillis = 0;
    private static volatile boolean calibrated = false;
    private static volatile Clock fixedClockForTesting = null;

    private ScheduledExecutorService scheduler;

    @PostConstruct
    public void init() {
        calibrateClock();

        // Periodically recalibrate every 5 minutes to track continuous drift or OS sleep/wake cycles
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ChatClock-Synchronizer");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleWithFixedDelay(this::calibrateClock, 5, 5, TimeUnit.MINUTES);
    }

    @PreDestroy
    public void destroy() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
    }

    public synchronized void calibrateClock() {
        if (fixedClockForTesting != null) {
            return;
        }

        for (String serverUrl : TIME_SERVERS) {
            try {
                long before = System.currentTimeMillis();
                HttpURLConnection conn = (HttpURLConnection) URI.create(serverUrl).toURL().openConnection();
                conn.setRequestMethod("HEAD");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(3000);
                conn.setInstanceFollowRedirects(false);

                long serverDate = conn.getHeaderFieldDate("Date", 0);
                long after = System.currentTimeMillis();
                long rtt = after - before;

                if (serverDate > 0) {
                    long estimatedNetworkTime = serverDate + (rtt / 2);
                    long skew = estimatedNetworkTime - after;
                    clockSkewMillis = skew;
                    calibrated = true;
                    log.info("⏰ CHAT CLOCK CALIBRATED via {}: Host clock skew detected = {} ms ({} seconds). System time synchronized with authoritative network time.",
                            serverUrl, skew, String.format("%.2f", skew / 1000.0));
                    return;
                }
            } catch (Exception e) {
                log.debug("Chat clock ping to {} failed: {}", serverUrl, e.getMessage());
            }
        }

        if (!calibrated) {
            log.warn("⚠️ Chat clock calibration skipped (all time endpoints unreachable). Using local host clock.");
        }
    }

    public Clock getAuthoritativeClock() {
        if (fixedClockForTesting != null) {
            return fixedClockForTesting;
        }
        if (!calibrated || clockSkewMillis == 0) {
            return Clock.systemUTC();
        }
        return Clock.offset(Clock.systemUTC(), Duration.ofMillis(clockSkewMillis));
    }

    public Instant nowInstant() {
        if (fixedClockForTesting != null) {
            return fixedClockForTesting.instant();
        }
        return Instant.now(getAuthoritativeClock());
    }

    public LocalDateTime now() {
        if (fixedClockForTesting != null) {
            return LocalDateTime.now(fixedClockForTesting);
        }
        Clock authoritativeClock = !calibrated || clockSkewMillis == 0
                ? Clock.systemDefaultZone()
                : Clock.offset(Clock.systemDefaultZone(), Duration.ofMillis(clockSkewMillis));
        return LocalDateTime.now(authoritativeClock);
    }

    public long getClockSkewMillis() {
        return clockSkewMillis;
    }

    public boolean isCalibrated() {
        return calibrated;
    }

    // Testing utilities
    public static void setFixedClock(Clock clock) {
        fixedClockForTesting = clock;
    }

    public static void setClockSkewMillis(long millis) {
        clockSkewMillis = millis;
        calibrated = true;
    }

    public static void resetClock() {
        fixedClockForTesting = null;
        clockSkewMillis = 0;
        calibrated = false;
    }
}
