/*
 * Copyright © Wynntils 2022-2025.
 * This file is released under LGPLv3. See LICENSE for full license details.
 */
package com.wynntils.utils;

import com.wynntils.core.WynntilsMod;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystemException;
import java.security.MessageDigest;
import java.util.Objects;
import java.util.function.Consumer;

public final class FileUtils {
    /**
     * Wraps File#mkdirs with a log output, in case of failure
     */
    public static void mkdir(File dir) {
        if (dir.isDirectory()) return;

        if (!dir.mkdirs()) {
            WynntilsMod.error("Directory " + dir + " could not be created");
        }
    }

    /**
     * Wraps File#mkdirs with a log output, in case of failure
     */
    public static void createNewFile(File file) {
        if (file.isFile()) {
            assert false;
            return;
        }

        try {
            if (!file.createNewFile()) {
                WynntilsMod.error("File " + file + " could not be created");
            }
        } catch (IOException e) {
            WynntilsMod.error("IOException while created File " + file);
        }
    }

    public static void deleteFile(File file) {
        if (!file.exists()) return;

        if (!file.delete()) {
            WynntilsMod.error("File " + file + " could not be deleted");
        }
    }

    public static void deleteFolder(File folder) {
        if (!folder.exists()) return;

        for (File file : Objects.requireNonNullElse(folder.listFiles(), new File[0])) {
            if (file.isDirectory()) {
                deleteFolder(file);
            } else {
                deleteFile(file);
            }
        }

        if (!folder.delete()) {
            WynntilsMod.error("Folder " + folder + " could not be deleted");
        }
    }

    public static void moveFile(File sourceFile, File destFile) {
        try {
            org.apache.commons.io.FileUtils.moveFile(sourceFile, destFile);
        } catch (IOException exception) {
            WynntilsMod.error("Move file " + sourceFile + " to " + destFile + " failed");
        }
    }

    public static void tryCopyFile(File sourceFile, File destFile) {
        try {
            copyFile(sourceFile, destFile);
        } catch (IOException e) {
            WynntilsMod.error("Copy file " + sourceFile + " to " + destFile + " failed");
        }
    }

    public static void copyFile(File sourceFile, File destFile) throws IOException {
        if (sourceFile == null || destFile == null) {
            throw new IllegalArgumentException("Argument files should not be null.");
        }

        try {
            org.apache.commons.io.FileUtils.copyFile(sourceFile, destFile);
        } catch (FileSystemException exception) {
            // Jar is locked on Windows, use streams
            copyFileWindows(sourceFile, destFile);
        }
    }

    private static void copyFileWindows(File sourceFile, File destFile) {
        try (FileInputStream inputStream = new FileInputStream(sourceFile);
                FileChannel source = inputStream.getChannel();
                FileOutputStream outputStream = new FileOutputStream(destFile);
                FileChannel destination = outputStream.getChannel()) {
            destination.transferFrom(source, 0, source.size());
        } catch (Exception e) {
            WynntilsMod.warn("Failed to copy file " + sourceFile + " to " + destFile, e);
        }
    }

    public static String getMd5(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Argument file should not be null or a directory.");
        }

        MD5Verification verification = new MD5Verification(file);
        return verification.getMd5();
    }
    
    public static String getSha256(File file) {
        if (!file.exists() || file.isDirectory()) {
            throw new IllegalArgumentException("Argument file should not be null or a directory.");
        }

        try (FileInputStream fis = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            
            while ((bytesRead = fis.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            
            byte[] hashBytes = digest.digest();
            StringBuilder result = new StringBuilder();
            for (byte b : hashBytes) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (Exception e) {
            WynntilsMod.error("Failed to calculate SHA-256 hash", e);
            return null;
        }
    }

    public static void downloadFileWithProgress(
            URLConnection connection, File fileDestination, Consumer<Float> progressCallback) {
        try {
            int fileSize = connection.getContentLength();
            
            // Validate file size to prevent DoS
            if (fileSize > 100 * 1024 * 1024) { // 100MB limit
                throw new IOException("File size exceeds maximum allowed size (100MB)");
            }

            createNewFile(fileDestination);

            try (InputStream inputStream = new BufferedInputStream(connection.getInputStream());
                 FileOutputStream outputStream = new FileOutputStream(fileDestination)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                long totalBytesRead = 0;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                    totalBytesRead += bytesRead;

                    // Additional check to prevent zip bombs
                    if (totalBytesRead > 100 * 1024 * 1024) {
                        throw new IOException("Downloaded file exceeds maximum allowed size");
                    }

                    if (fileSize > 0) {
                        float progress = (float) totalBytesRead / fileSize;
                        progressCallback.accept(progress);
                    }
                }
            }
        } catch (IOException exception) {
            fileDestination.delete();
            WynntilsMod.error("Exception whilst downloading file", exception);
            throw new RuntimeException("Failed to download file", exception);
        }
    }
}
