package com.erp.automation.reporting;

import com.erp.automation.config.ConfigReader;
import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Single Extent Spark report for the whole run
 * ({@code test-output/reports/ExtentReport.html}).
 *
 * <p>Fixes compared to the first version:</p>
 * <ul>
 *     <li><b>Offline mode</b>: the Spark report loaded its CSS/JS from CDNs,
 *         so it opened blank without internet (or inside IntelliJ's preview).
 *         The assets are now written next to the report.</li>
 *     <li><b>One ExtentTest per TestNG result</b>, created on demand: a test
 *         skipped by a failing {@code @BeforeMethod} never reaches
 *         {@code onTestStart}, so {@code getTest()} used to throw (and the
 *         listener crashed) or return the PREVIOUS test of the thread, which
 *         was then marked skipped by mistake.</li>
 *     <li>The ThreadLocal is cleared after every test.</li>
 * </ul>
 */
public final class ExtentReportManager {

    private static final String REPORT_DIRECTORY =
            "test-output/reports";

    private static final String REPORT_FILE =
            REPORT_DIRECTORY + "/ExtentReport.html";

    private static ExtentReports extentReports;

    private static final ThreadLocal<ExtentTest> extentTest =
            new ThreadLocal<>();

    private ExtentReportManager() {
        // Prevent object creation
    }

    public static synchronized void initializeReport() {

        if (extentReports != null) {
            return;
        }

        try {
            Files.createDirectories(Paths.get(REPORT_DIRECTORY));
        } catch (Exception e) {
            throw new RuntimeException("Failed to create report directory.", e);
        }

        ExtentSparkReporter sparkReporter =
                new ExtentSparkReporter(REPORT_FILE);

        sparkReporter.config().setDocumentTitle("ERP Automation Report");
        sparkReporter.config().setReportName("ERP Automation Test Execution");
        sparkReporter.config().setTheme(Theme.STANDARD);
        sparkReporter.config().setTimeStampFormat("yyyy-MM-dd HH:mm:ss");

        // Write the report's CSS/JS locally so it opens without internet.
        sparkReporter.config().enableOfflineMode(true);

        // The tests under each page start COLLAPSED (name + status only); a click on a
        // test opens its steps. Spark copies a page's content from the hidden list when the
        // page is clicked, so the nodes are collapsed there once, when the report loads
        // (same classes as Spark's own "collapse all": .node.collapsed + .collapse body).
        sparkReporter.config().setJs(
                "(function(){"
                        + "function collapseNodes(root){"
                        + "  $(root).find('.node').each(function(){"
                        + "    $(this).addClass('collapsed').closest('.card-header').next().addClass('collapse');"
                        + "  });"
                        + "}"
                        + "collapseNodes('.test-list-item .test-contents');"
                        + "collapseNodes('.test-content-detail');"
                        + "})();");

        extentReports = new ExtentReports();
        extentReports.attachReporter(sparkReporter);

        // Last safety net: write the report even if the run is interrupted
        // (Ctrl+C, surefire fork timeout) before TestNG calls onFinish.
        Runtime.getRuntime().addShutdownHook(
                new Thread(ExtentReportManager::flushReport, "extent-flush"));

        // Every console line written during a test becomes a step of that
        // test in the report (same steps as the IntelliJ / Maven console).
        ConsoleToReportStream.install();

        extentReports.setSystemInfo("Browser", safeConfig("browser"));
        extentReports.setSystemInfo("Environment", safeConfig("environment"));
        extentReports.setSystemInfo("Base URL", safeConfig("baseUrl"));
        extentReports.setSystemInfo("Framework", "Selenium + Java + TestNG");
        extentReports.setSystemInfo("Operating System", System.getProperty("os.name"));
        extentReports.setSystemInfo("OS Version", System.getProperty("os.version"));
        extentReports.setSystemInfo("Java Version", System.getProperty("java.version"));
        extentReports.setSystemInfo("User", System.getProperty("user.name"));
    }

    /** A missing config key must not break the report. */
    private static String safeConfig(String key) {
        try {
            String value = ConfigReader.get(key);
            return value == null ? "-" : value;
        } catch (Exception e) {
            return "-";
        }
    }

    /**
     * Creates the report entry of the current test and binds it to this thread.
     *
     * @param name        shown as the test title
     * @param description TestNG {@code description}, may be null/blank
     * @param categories  class name and groups (filters in the report)
     */
    public static synchronized ExtentTest createTest(String name,
                                                     String description,
                                                     String... categories) {

        initializeReport();

        ExtentTest test = (description == null || description.isBlank())
                ? extentReports.createTest(name)
                : extentReports.createTest(name, description);

        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    test.assignCategory(category);
                }
            }
        }

        extentTest.set(test);
        return test;
    }

    /** One parent entry per page (screen), in the order the pages first run. */
    private static final java.util.Map<String, ExtentTest> pages = new java.util.LinkedHashMap<>();

    /**
     * Creates the report entry of the current test UNDER the entry of its page
     * ({@code page}, e.g. "أوامر الشراء"), so the report lists the tests grouped by
     * page. The page entry is created by the first test of that page; its status
     * follows its tests (failed when any test failed).
     *
     * @param page        page title (parent entry)
     * @param name        test title (child entry)
     * @param description TestNG {@code description}, may be null/blank
     * @param categories  class name and groups (filters in the report)
     */
    public static synchronized ExtentTest createTestUnderPage(String page,
                                                              String name,
                                                              String description,
                                                              String... categories) {

        initializeReport();

        ExtentTest parent = pages.computeIfAbsent(page, title -> extentReports.createTest(title));
        ExtentTest test = (description == null || description.isBlank())
                ? parent.createNode(name)
                : parent.createNode(name, description);

        if (categories != null) {
            for (String category : categories) {
                if (category != null && !category.isBlank()) {
                    test.assignCategory(category);
                }
            }
        }

        extentTest.set(test);
        return test;
    }

    /** Kept for existing callers. */
    public static ExtentTest createTest(String testName) {
        return createTest(testName, null);
    }

    /** The report entry of the current thread, or null if none was created. */
    public static ExtentTest currentTestOrNull() {
        return extentTest.get();
    }

    public static ExtentTest getTest() {

        ExtentTest test = extentTest.get();

        if (test == null) {
            throw new IllegalStateException("ExtentTest has not been initialized.");
        }

        return test;
    }

    /** Adds a step line to the current test (ignored when no test is active). */
    public static void info(String message) {

        ExtentTest test = extentTest.get();
        if (test != null) {
            test.info(message);
        }
    }

    public static void removeTest() {
        extentTest.remove();
    }

    public static synchronized void flushReport() {

        if (extentReports != null) {
            extentReports.flush();
        }
    }

    /** Absolute path of the HTML report (for the log). */
    public static Path reportPath() {
        return Paths.get(REPORT_FILE).toAbsolutePath();
    }
}
