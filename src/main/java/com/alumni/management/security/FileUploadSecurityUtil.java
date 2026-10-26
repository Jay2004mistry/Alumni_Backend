package com.alumni.management.security;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadSecurityUtil {

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList(
            ".jpg", ".jpeg", ".png", ".webp", ".gif"
    );

    /**
     * Validates and saves an uploaded image safely to the target directory.
     * Prevents path traversal, enforces allowed image extensions, and verifies canonical path boundaries.
     *
     * @param image the uploaded MultipartFile
     * @param baseDir relative path of the target directory (e.g. "uploads/profiles/")
     * @return the relative URL path of the saved file
     * @throws IOException on I/O error
     * @throws IllegalArgumentException on missing or invalid file
     * @throws SecurityException on path traversal or disallowed extension
     */
    public static String saveImageSafely(MultipartFile image, String baseDir) throws IOException {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }

        String rawFilename = image.getOriginalFilename();
        if (rawFilename == null || rawFilename.trim().isEmpty()) {
            throw new IllegalArgumentException("Invalid filename");
        }

        // Strip any directory traversal components from filename
        String cleanBaseName = Paths.get(rawFilename).getFileName().toString();

        // Extract and validate extension
        int dotIdx = cleanBaseName.lastIndexOf('.');
        if (dotIdx == -1) {
            throw new IllegalArgumentException("File must have an image extension");
        }
        String extension = cleanBaseName.substring(dotIdx).toLowerCase(Locale.ROOT);
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new SecurityException("Invalid file extension: " + extension + ". Allowed: " + ALLOWED_IMAGE_EXTENSIONS);
        }

        // Verify size limit (5MB)
        if (image.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException("File size exceeds maximum allowed limit (5MB)");
        }

        // Verify MIME type
        String contentType = image.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new SecurityException("Invalid content type: only image MIME types are permitted");
        }

        // Verify actual image magic bytes to prevent masqueraded executables
        byte[] header = new byte[12];
        try (java.io.InputStream is = image.getInputStream()) {
            int read = is.read(header, 0, header.length);
            if (read < 4 || !isValidImageMagicBytes(header, extension)) {
                throw new SecurityException("Invalid image content: magic byte signature does not match declared image format");
            }
        }
        File targetDirectory = new File(baseDir);
        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs();
        }

        // Generate safe unique filename
        String safeFileName = System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8) + extension;
        File destinationFile = new File(targetDirectory, safeFileName);

        // Verify canonical path containment (anti-path traversal)
        String canonicalDest = destinationFile.getCanonicalPath();
        String canonicalDir = targetDirectory.getCanonicalPath();
        if (!canonicalDest.startsWith(canonicalDir + File.separator)) {
            throw new SecurityException("Potential path traversal attempt detected");
        }

        image.transferTo(destinationFile);

        // Normalize return URL
        String normalizedDir = baseDir.startsWith("/") ? baseDir : "/" + baseDir;
        if (!normalizedDir.endsWith("/")) {
            normalizedDir += "/";
        }
        return normalizedDir + safeFileName;
    }

    private static boolean isValidImageMagicBytes(byte[] header, String extension) {
        if (header == null || header.length < 4) return false;

        // PNG: 89 50 4E 47 (.png)
        boolean isPng = (header[0] == (byte) 0x89 && header[1] == (byte) 0x50 &&
                         header[2] == (byte) 0x4E && header[3] == (byte) 0x47);

        // JPEG: FF D8 FF (.jpg, .jpeg)
        boolean isJpeg = (header[0] == (byte) 0xFF && header[1] == (byte) 0xD8 && header[2] == (byte) 0xFF);

        // GIF: 47 49 46 38 ('G' 'I' 'F' '8') (.gif)
        boolean isGif = (header[0] == (byte) 0x47 && header[1] == (byte) 0x49 &&
                        header[2] == (byte) 0x46 && header[3] == (byte) 0x38);

        // WebP: 'R' 'I' 'F' 'F' ... 'W' 'E' 'B' 'P' (.webp)
        boolean isWebP = (header[0] == (byte) 0x52 && header[1] == (byte) 0x49 &&
                          header[2] == (byte) 0x46 && header[3] == (byte) 0x46 &&
                          header.length >= 12 &&
                          header[8] == (byte) 0x57 && header[9] == (byte) 0x45 &&
                          header[10] == (byte) 0x42 && header[11] == (byte) 0x50);

        if (".png".equals(extension)) return isPng;
        if (".jpg".equals(extension) || ".jpeg".equals(extension)) return isJpeg;
        if (".gif".equals(extension)) return isGif;
        if (".webp".equals(extension)) return isWebP;

        return isPng || isJpeg || isGif || isWebP;
    }
}
