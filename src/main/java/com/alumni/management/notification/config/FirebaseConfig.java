package com.alumni.management.notification.config;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import jakarta.annotation.PostConstruct;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${firebase.enabled:true}")
    private boolean firebaseEnabled;

    @Value("${firebase.credentials.path:}")
    private String credentialsPath;

    @Value("${firebase.credentials.json:}")
    private String credentialsJson;

    private boolean initialized = false;

    @PostConstruct
    public synchronized void initializeFirebase() {
        if (!firebaseEnabled) {
            log.info("ℹ️ Firebase is disabled via firebase.enabled=false configuration.");
            return;
        }

        if (!FirebaseApp.getApps().isEmpty()) {
            log.info("ℹ️ FirebaseApp already initialized.");
            initialized = true;
            return;
        }

        try {
            InputStream serviceAccountStream = null;

            // 1. Raw JSON string from property / environment variable
            if (credentialsJson != null && !credentialsJson.trim().isEmpty()) {
                log.info("🔑 Loading Firebase credentials from inline JSON string.");
                serviceAccountStream = new ByteArrayInputStream(credentialsJson.trim().getBytes(StandardCharsets.UTF_8));
            }
            // 2. File path from configuration property
            else if (credentialsPath != null && !credentialsPath.trim().isEmpty()) {
                File file = new File(credentialsPath.trim());
                if (file.exists() && file.isFile()) {
                    log.info("🔑 Loading Firebase credentials from file path: {}", file.getAbsolutePath());
                    serviceAccountStream = new FileInputStream(file);
                } else {
                    log.warn("⚠️ Configured firebase.credentials.path does not exist: {}", credentialsPath);
                }
            }

            // 3. Environment variables fallback
            if (serviceAccountStream == null) {
                String envPath = System.getenv("FIREBASE_CREDENTIALS_PATH");
                if (envPath == null || envPath.trim().isEmpty()) {
                    envPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                }
                if (envPath != null && !envPath.trim().isEmpty()) {
                    File file = new File(envPath.trim());
                    if (file.exists() && file.isFile()) {
                        log.info("🔑 Loading Firebase credentials from environment variable path: {}", file.getAbsolutePath());
                        serviceAccountStream = new FileInputStream(file);
                    }
                }
            }

            // 4. Default credentials or graceful fallback
            GoogleCredentials credentials;
            if (serviceAccountStream != null) {
                try (InputStream stream = serviceAccountStream) {
                    credentials = GoogleCredentials.fromStream(stream);
                }
            } else {
                try {
                    credentials = GoogleCredentials.getApplicationDefault();
                    log.info("🔑 Loading Firebase Application Default Credentials.");
                } catch (Exception e) {
                    log.warn("⚠️ Firebase service account credentials not configured. Push notification sending will be skipped gracefully. (Configure FIREBASE_CREDENTIALS_PATH or FIREBASE_CREDENTIALS_JSON to enable real push notifications).");
                    return;
                }
            }

            FirebaseOptions.Builder optionsBuilder = FirebaseOptions.builder()
                    .setCredentials(credentials);

            if (credentials instanceof com.google.auth.oauth2.ServiceAccountCredentials) {
                String projectId = ((com.google.auth.oauth2.ServiceAccountCredentials) credentials).getProjectId();
                if (projectId != null && !projectId.trim().isEmpty()) {
                    optionsBuilder.setProjectId(projectId);
                }
            }

            FirebaseApp.initializeApp(optionsBuilder.build());
            initialized = true;
            log.info("🔥 FirebaseApp initialized successfully!");

        } catch (Exception e) {
            log.error("⚠️ Failed to initialize Firebase App: {}. Application will continue without push notifications.", e.getMessage());
            initialized = false;
        }
    }

    public boolean isFirebaseAvailable() {
        return initialized && !FirebaseApp.getApps().isEmpty();
    }
}

