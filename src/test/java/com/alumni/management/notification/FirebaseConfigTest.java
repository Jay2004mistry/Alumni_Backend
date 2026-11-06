package com.alumni.management.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.alumni.management.notification.config.FirebaseConfig;
import com.google.firebase.FirebaseApp;

public class FirebaseConfigTest {

    @AfterEach
    void tearDown() {
        for (FirebaseApp app : FirebaseApp.getApps()) {
            try {
                app.delete();
            } catch (Exception ignored) {
            }
        }
    }

    @Test
    @DisplayName("Verify Firebase initializes from configured file path")
    void testFirebaseInitializationFromPath() {
        String path = "E:/Documents/GLS-Secrets/firebase-service-account.json";
        File file = new File(path);
        assertTrue(file.exists(), "Service account JSON key file must exist at " + path);
        assertTrue(file.canRead(), "Service account JSON key file must be readable");

        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseEnabled", true);
        ReflectionTestUtils.setField(config, "credentialsPath", path);

        config.initializeFirebase();

        assertTrue(config.isFirebaseAvailable(), "Firebase should be marked as available");
        assertFalse(FirebaseApp.getApps().isEmpty(), "FirebaseApp should have an initialized app");
        assertEquals("alumni-connect-80612", FirebaseApp.getInstance().getOptions().getProjectId());

        // Verify idempotency (calling initialize again does not fail and initializes exactly once)
        config.initializeFirebase();
        assertEquals(1, FirebaseApp.getApps().size(), "FirebaseApp must only initialize once");
    }

    @Test
    @DisplayName("Verify missing or invalid credentials path handled safely without throwing")
    void testInvalidPathHandledSafely() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseEnabled", true);
        ReflectionTestUtils.setField(config, "credentialsPath", "E:/NonExistentPath/invalid.json");

        // Must complete without throwing any exception
        config.initializeFirebase();
        assertFalse(config.isFirebaseAvailable(), "Firebase should be gracefully disabled on invalid path");
    }

    @Test
    @DisplayName("Verify firebase disabled setting skips initialization")
    void testFirebaseDisabledConfig() {
        FirebaseConfig config = new FirebaseConfig();
        ReflectionTestUtils.setField(config, "firebaseEnabled", false);
        ReflectionTestUtils.setField(config, "credentialsPath", "E:/Documents/GLS-Secrets/firebase-service-account.json");

        config.initializeFirebase();
        assertFalse(config.isFirebaseAvailable(), "Firebase should be disabled when firebase.enabled=false");
        assertTrue(FirebaseApp.getApps().isEmpty(), "No FirebaseApp should be created when disabled");
    }
}

