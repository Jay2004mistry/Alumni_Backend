package com.alumni.management.security;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

public class FileUploadSecurityUtilTest {

    private final String testUploadDir = "target/test_uploads/";

    @AfterEach
    void tearDown() {
        File dir = new File(testUploadDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
        }
    }

    private static final byte[] VALID_PNG_BYTES = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    private static final byte[] VALID_JPEG_BYTES = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};

    @Test
    void testSaveImageSafelyWithValidPng() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", VALID_PNG_BYTES);
        String savedUrl = FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);

        assertNotNull(savedUrl);
        assertTrue(savedUrl.endsWith(".png"));
        assertTrue(savedUrl.contains("target/test_uploads/"));
    }

    @Test
    void testSaveImageSafelyWithValidJpeg() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "photo.jpg", "image/jpeg", VALID_JPEG_BYTES);
        String savedUrl = FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);

        assertNotNull(savedUrl);
        assertTrue(savedUrl.endsWith(".jpg"));
        assertTrue(savedUrl.contains("target/test_uploads/"));
    }

    @Test
    void testSaveImageSafelyRejectsDisguisedExecutableContent() {
        // A file with .png extension but containing plain text / script content instead of PNG magic bytes
        MockMultipartFile file = new MockMultipartFile("image", "malicious.png", "image/png", "<?php echo 'attack'; ?>".getBytes());

        assertThrows(SecurityException.class, () -> {
            FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);
        });
    }

    @Test
    void testSaveImageSafelyRejectsPathTraversalFilename() throws IOException {
        MockMultipartFile file = new MockMultipartFile("image", "../../../evil.png", "image/png", VALID_PNG_BYTES);
        String savedUrl = FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);

        assertNotNull(savedUrl);
        // Stripped path traversal - target file must stay inside testUploadDir
        File savedFile = new File(savedUrl.startsWith("/") ? savedUrl.substring(1) : savedUrl);
        assertTrue(savedFile.getAbsolutePath().contains("target" + File.separator + "test_uploads"));
    }

    @Test
    void testSaveImageSafelyRejectsDisallowedExtension() {
        MockMultipartFile file = new MockMultipartFile("image", "script.jsp", "text/plain", "malicious code".getBytes());

        assertThrows(SecurityException.class, () -> {
            FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);
        });
    }

    @Test
    void testSaveImageSafelyRejectsEmptyFile() {
        MockMultipartFile file = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> {
            FileUploadSecurityUtil.saveImageSafely(file, testUploadDir);
        });
    }
}
