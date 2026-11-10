package com.erp.automation.utils;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotManager {

    private static final String SCREENSHOT_DIRECTORY =
            "test-output/screenshots";

    private static final DateTimeFormatter TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private ScreenshotManager() {
        // Prevent object creation
    }

    public static String takeScreenshot(
            WebDriver driver,
            String testName) {

        if (driver == null) {
            throw new IllegalArgumentException(
                    "WebDriver cannot be null."
            );
        }

        try {

            Path directory =
                    Paths.get(SCREENSHOT_DIRECTORY);

            Files.createDirectories(directory);

            String timestamp =
                    LocalDateTime.now()
                            .format(TIMESTAMP_FORMAT);

            String fileName =
                    sanitizeFileName(testName)
                            + "_"
                            + timestamp
                            + ".png";

            Path destination =
                    directory.resolve(fileName);

            File source =
                    ((TakesScreenshot) driver)
                            .getScreenshotAs(OutputType.FILE);

            Files.copy(
                    source.toPath(),
                    destination
            );

            return destination.toAbsolutePath().toString();

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to capture screenshot.",
                    e
            );
        }
    }

    private static String sanitizeFileName(
            String fileName) {

        return fileName
                .replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}