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

    // ----- building an import file from a downloaded template -----

    /**
     * Copies the template {@code template} to {@code target} with the rows {@code rows}
     * written under its header row {@code headerRow} (rows below it in the template are
     * dropped). Each row maps a header text (as in the template, e.g. "كود الصنف") to a
     * value: a Number is written as a number, anything else as text. Headers a row does
     * not name stay empty. No library is needed: the sheet XML is edited in the zip.
     *
     * @return {@code target}.
     */
    public static java.nio.file.Path xlsxFromTemplate(java.nio.file.Path template, java.nio.file.Path target, int headerRow,
                                                      java.util.List<java.util.Map<String, Object>> rows) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(template.toFile())) {
            java.util.List<String> shared = sharedStrings(zip);
            String sheetName = "xl/worksheets/sheet1.xml";
            String sheet = new String(zip.getInputStream(zip.getEntry(sheetName)).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            java.util.Map<String, String> headers = headerColumns(sheet, headerRow, shared);
            if (headers.isEmpty()) {
                throw new IllegalStateException("Row " + headerRow + " of " + template.getFileName() + " has no headers.");
            }
            StringBuilder written = new StringBuilder();
            int r = headerRow;
            for (java.util.Map<String, Object> row : rows) {
                r++;
                written.append("<row r=\"").append(r).append("\">");
                for (java.util.Map.Entry<String, String> header : headers.entrySet()) {
                    Object value = row.get(header.getValue());
                    if (value == null) {
                        continue;
                    }
                    String ref = header.getKey() + r;
                    if (value instanceof Number) {
                        written.append("<c r=\"").append(ref).append("\"><v>").append(value).append("</v></c>");
                    } else {
                        written.append("<c r=\"").append(ref).append("\" t=\"inlineStr\"><is><t>")
                                .append(xmlEscape(String.valueOf(value))).append("</t></is></c>");
                    }
                }
                written.append("</row>");
                for (String name : row.keySet()) {
                    if (!headers.containsValue(name)) {
                        throw new IllegalStateException("No column \"" + name + "\" in " + template.getFileName()
                                + ": " + headers.values());
                    }
                }
            }
            // Keep the rows up to the header row, then ours.
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(?s)<row\\b[^>]*\\br=\"(\\d+)\"[^>]*?(?:/>|>.*?</row>)").matcher(sheet);
            StringBuilder kept = new StringBuilder();
            while (m.find()) {
                if (Integer.parseInt(m.group(1)) <= headerRow) {
                    kept.append(m.group());
                }
            }
            String lastColumn = headers.keySet().stream().reduce((a, b) -> columnIndex(a) >= columnIndex(b) ? a : b).orElse("A");
            String newSheet = sheet
                    .replaceFirst("(?s)<sheetData\\s*/>", "<sheetData></sheetData>")
                    .replaceFirst("(?s)<sheetData>.*</sheetData>",
                            java.util.regex.Matcher.quoteReplacement("<sheetData>" + kept + written + "</sheetData>"))
                    .replaceFirst("<dimension ref=\"[^\"]*\"", "<dimension ref=\"A1:" + lastColumn + r + "\"");

            java.nio.file.Files.createDirectories(target.toAbsolutePath().getParent());
            try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(java.nio.file.Files.newOutputStream(target))) {
                for (java.util.zip.ZipEntry entry : java.util.Collections.list(zip.entries())) {
                    out.putNextEntry(new java.util.zip.ZipEntry(entry.getName()));
                    if (entry.getName().equals(sheetName)) {
                        out.write(newSheet.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    } else if (!entry.getName().equals("xl/calcChain.xml")) {
                        zip.getInputStream(entry).transferTo(out);
                    }
                    out.closeEntry();
                }
            }
            System.out.println("Import file " + target + ": headers " + headers.values() + ", " + rows.size() + " row(s).");
            return target;
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot build " + target + " from " + template, e);
        }
    }

    /** Header texts of row {@code headerRow} of the first sheet of {@code xlsx}, in column order. */
    public static java.util.List<String> xlsxHeaders(java.nio.file.Path xlsx, int headerRow) {
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(xlsx.toFile())) {
            String sheet = new String(zip.getInputStream(zip.getEntry("xl/worksheets/sheet1.xml")).readAllBytes(),
                    java.nio.charset.StandardCharsets.UTF_8);
            return new java.util.ArrayList<>(headerColumns(sheet, headerRow, sharedStrings(zip)).values());
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + xlsx, e);
        }
    }

    /** Column letter → header text of row {@code headerRow} (shared, inline or "str" cells), in column order. */
    private static java.util.Map<String, String> headerColumns(String sheet, int headerRow, java.util.List<String> shared) {
        java.util.Map<String, String> headers = new java.util.TreeMap<>(java.util.Comparator.comparingInt(Downloads::columnIndex));
        java.util.regex.Matcher row = java.util.regex.Pattern
                .compile("(?s)<row\\b[^>]*\\br=\"" + headerRow + "\"[^>]*>(.*?)</row>").matcher(sheet);
        if (!row.find()) {
            return headers;
        }
        java.util.regex.Matcher cell = java.util.regex.Pattern
                .compile("(?s)<c\\b([^>]*?)(?:/>|>(.*?)</c>)").matcher(row.group(1));
        while (cell.find()) {
            String attrs = cell.group(1);
            String body = cell.group(2) == null ? "" : cell.group(2);
            java.util.regex.Matcher ref = java.util.regex.Pattern.compile("\\br=\"([A-Z]+)\\d+\"").matcher(attrs);
            if (!ref.find()) {
                continue;
            }
            String text;
            java.util.regex.Matcher v = java.util.regex.Pattern.compile("(?s)<v>(.*?)</v>").matcher(body);
            if (attrs.contains("t=\"s\"") && v.find()) {
                text = shared.get(Integer.parseInt(v.group(1).trim()));
            } else if (attrs.contains("t=\"inlineStr\"")) {
                text = body.replaceAll("<[^>]+>", "");
            } else {
                text = v.find() ? v.group(1) : "";
            }
            text = xmlUnescape(text).replaceAll("\\s+", " ").trim();
            if (!text.isEmpty()) {
                headers.put(ref.group(1), text);
            }
        }
        return headers;
    }

    private static java.util.List<String> sharedStrings(java.util.zip.ZipFile zip) throws java.io.IOException {
        java.util.List<String> strings = new java.util.ArrayList<>();
        java.util.zip.ZipEntry entry = zip.getEntry("xl/sharedStrings.xml");
        if (entry == null) {
            return strings;
        }
        String xml = new String(zip.getInputStream(entry).readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        java.util.regex.Matcher si = java.util.regex.Pattern.compile("(?s)<si>(.*?)</si>").matcher(xml);
        while (si.find()) {
            strings.add(si.group(1).replaceAll("<rPh\\b.*?</rPh>", "").replaceAll("<[^>]+>", ""));
        }
        return strings;
    }

    private static int columnIndex(String letters) {
        int index = 0;
        for (char c : letters.toCharArray()) {
            index = index * 26 + (c - 'A' + 1);
        }
        return index;
    }

    private static String xmlEscape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String xmlUnescape(String text) {
        // Numeric references ("&#1603;"): some writers store Arabic text this way.
        java.util.regex.Matcher ref = java.util.regex.Pattern.compile("&#(x?)([0-9a-fA-F]+);").matcher(text);
        StringBuilder decoded = new StringBuilder();
        while (ref.find()) {
            int code = Integer.parseInt(ref.group(2), ref.group(1).isEmpty() ? 10 : 16);
            ref.appendReplacement(decoded, java.util.regex.Matcher.quoteReplacement(new String(Character.toChars(code))));
        }
        ref.appendTail(decoded);
        text = decoded.toString();
        return text.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&apos;", "'").replace("&amp;", "&");
    }
}
