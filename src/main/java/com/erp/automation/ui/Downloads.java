package com.erp.automation.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Downloads in Chrome: a clean folder per test (CDP Browser.setDownloadBehavior)
 * and checks on the downloaded file (PDF / xlsx). Without it files go to the
 * user's Downloads folder and cannot be checked.
 */
public class Downloads {

    private final WebDriver driver;

    public Downloads(WebDriver driver) {

        this.driver = driver;
    }

    /**
     * Creates an empty folder target/downloads/{@code name} and tells Chrome to
     * save downloads there (CDP Browser.setDownloadBehavior), so a test can
     * check the file it downloads. Without it files go to the user's Downloads.
     */
    public java.nio.file.Path prepareDownloadFolder(String name) {

        java.nio.file.Path dir = java.nio.file.Paths.get("target", "downloads", name).toAbsolutePath();
        try {
            if (java.nio.file.Files.exists(dir)) {
                try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                    for (java.nio.file.Path f : files.toList()) {
                        java.nio.file.Files.deleteIfExists(f);
                    }
                }
            }
            java.nio.file.Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot prepare download folder " + dir, e);
        }

        if (!(driver instanceof org.openqa.selenium.chromium.ChromiumDriver chromium)) {
            throw new IllegalStateException("Download checks need Chrome/Edge; driver is "
                    + driver.getClass().getSimpleName());
        }
        chromium.executeCdpCommand("Browser.setDownloadBehavior",
                Map.of("behavior", "allow", "downloadPath", dir.toString(), "eventsEnabled", true));
        System.out.println("Downloads go to: " + dir);
        return dir;
    }

    /**
     * Waits until a finished file ending with {@code extension} (e.g. ".pdf")
     * is in {@code dir} (no .crdownload/.tmp, size stable for 1 s).
     */
    public java.nio.file.Path waitForDownloadedFile(java.nio.file.Path dir, String extension, Duration timeout) {

        long end = System.currentTimeMillis() + timeout.toMillis();
        long lastSize = -1;
        while (System.currentTimeMillis() < end) {
            try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
                java.util.Optional<java.nio.file.Path> file = files
                        .filter(f -> f.getFileName().toString().toLowerCase().endsWith(extension.toLowerCase()))
                        .findFirst();
                if (file.isPresent()) {
                    long size = java.nio.file.Files.size(file.get());
                    if (size > 0 && size == lastSize) {
                        System.out.println("Downloaded: " + file.get().getFileName() + " (" + size + " bytes)");
                        return file.get();
                    }
                    lastSize = size;
                }
            } catch (java.io.IOException ignored) {
                // file still being written
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        java.util.List<String> found = new java.util.ArrayList<>();
        try (java.util.stream.Stream<java.nio.file.Path> files = java.nio.file.Files.list(dir)) {
            files.forEach(f -> found.add(f.getFileName().toString()));
        } catch (java.io.IOException ignored) {
        }
        throw new TimeoutException("No " + extension + " file was downloaded to " + dir
                + " within " + timeout.toSeconds() + " s. Folder has: " + found);
    }

    // ----- checks on downloaded files -----

    /** True when the file starts with {@code magic} ("%PDF", "PK" for .xlsx/.zip). */
    public static boolean fileStartsWith(java.nio.file.Path file, String magic) {

        try (java.io.InputStream in = java.nio.file.Files.newInputStream(file)) {
            byte[] head = in.readNBytes(magic.length());
            return new String(head, java.nio.charset.StandardCharsets.ISO_8859_1).equals(magic);
        } catch (java.io.IOException e) {
            return false;
        }
    }

    public static long sizeOf(java.nio.file.Path file) {

        try {
            return java.nio.file.Files.size(file);
        } catch (java.io.IOException e) {
            return -1;
        }
    }

    /** All text of the workbook's XML parts (shared strings and sheets). */
    public static String xlsxText(java.nio.file.Path xlsx) {

        StringBuilder all = new StringBuilder();
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(xlsx.toFile())) {
            for (java.util.zip.ZipEntry entry : java.util.Collections.list(zip.entries())) {
                if (entry.getName().startsWith("xl/") && entry.getName().endsWith(".xml")) {
                    all.append(new String(zip.getInputStream(entry).readAllBytes(),
                            java.nio.charset.StandardCharsets.UTF_8));
                }
            }
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + xlsx, e);
        }
        return all.toString();
    }

    /** Number of rows in the first sheet. */
    public static int xlsxRowCount(java.nio.file.Path xlsx) {

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(xlsx.toFile())) {
            String sheet = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return (int) java.util.regex.Pattern.compile("<row[ >]").matcher(sheet).results().count();
        } catch (java.io.IOException | NullPointerException e) {
            throw new IllegalStateException("Cannot read the first sheet of " + xlsx, e);
        }
    }
}
