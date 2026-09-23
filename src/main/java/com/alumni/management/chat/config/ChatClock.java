package com.alumni.management.chat.config;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ChatClock {

    private static final Logger log = LoggerFactory.getLogger(ChatClock.class);

    private static volatile long clockSkewMillis = 0;
    private static volatile boolean calibrated = false;

    @PostConstruct
    public void init() {
        calibrateClock();
    }

    public synchronized void calibrateClock() {
        try {
            long before = System.currentTimeMillis();
            HttpURLConnection conn = (HttpURLConnection) new URL("https://www.google.com").openConnection();
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
                log.info("⏰ CHAT CLOCK CALIBRATED: Host clock skew detected = {} ms ({} seconds). System time has been synchronized with network time.",
                        skew, String.format("%.2f", skew / 1000.0));
            } else {
                log.warn("⚠️ Could not read network date header. Using system clock directly.");
            }
        } catch (Exception e) {
            log.warn("⚠️ Chat clock calibration skipped (offline or network error): {}. Using system clock.", e.getMessage());
        }
    }

    public LocalDateTime now() {
        if (!calibrated || clockSkewMillis == 0) {
            return LocalDateTime.now();
        }
        Clock authoritativeClock = Clock.offset(Clock.systemDefaultZone(), Duration.ofMillis(clockSkewMillis));
        return LocalDateTime.now(authoritativeClock);
    }

    public Instant nowInstant() {
        if (!calibrated || clockSkewMillis == 0) {
            return Instant.now();
        }
        Clock authoritativeClock = Clock.offset(Clock.systemUTC(), Duration.ofMillis(clockSkewMillis));
        return Instant.now(authoritativeClock);
    }

    public long getClockSkewMillis() {
        return clockSkewMillis;
    }

    public boolean isCalibrated() {
        return calibrated;
    }
}
