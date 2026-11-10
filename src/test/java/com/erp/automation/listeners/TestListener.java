package com.erp.automation.listeners;

import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.MediaEntityBuilder;
import com.erp.automation.core.DriverManager;
import com.erp.automation.reporting.ExtentReportManager;
import com.erp.automation.utils.ScreenshotManager;

import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.testng.IConfigurationListener;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Writes every TestNG result to the Extent report.
 *
 * <p>The report entry is stored on the {@link ITestResult} itself, not only
 * in a ThreadLocal. A test skipped because its {@code @BeforeMethod} failed
 * (login, SkipException …) never goes through {@code onTestStart}; the old
 * listener then threw "ExtentTest has not been initialized" or marked the
 * PREVIOUS test as skipped. Now the entry is created on demand.</p>
 *
 * <p>Failure screenshots are embedded in the HTML (Base64), so the report
 * still shows them when it is opened from another folder or sent by mail.
 * The PNG file is still saved under {@code test-output/screenshots}.</p>
 */
public class TestListener implements ITestListener, IConfigurationListener {

    private static final Logger logger =
            LoggerFactory.getLogger(TestListener.class);

    private static final String EXTENT_TEST_ATTRIBUTE = "extentTest";

    // =========================
    // Suite / <test> lifecycle
    // =========================

    @Override
    public void onStart(ITestContext context) {
        ExtentReportManager.initializeReport();
    }

    @Override
    public void onFinish(ITestContext context) {

        logger.info("TEST SUITE FINISHED: {}", context.getSuite().getName());

        ExtentReportManager.flushReport();

        logger.info("Extent report: {}", ExtentReportManager.reportPath());
    }

    // =========================
    // Test results
    // =========================

    @Override
    public void onTestStart(ITestResult result) {

        logger.info("TEST STARTED: {}", testName(result));

        reportEntry(result).info("Test execution started.");
    }

    @Override
    public void onTestSuccess(ITestResult result) {

        logger.info("TEST PASSED: {}", testName(result));

        reportEntry(result).pass("Test passed successfully.");
        ExtentReportManager.removeTest();
        ExtentReportManager.flushReport();   // keep the HTML up to date after every test
    }

    @Override
    public void onTestFailure(ITestResult result) {

        String testName = testName(result);
        Throwable throwable = result.getThrowable();

        logger.error("TEST FAILED: {}", testName, throwable);

        ExtentTest test = reportEntry(result);

        if (throwable != null) {
            test.fail(throwable);
        } else {
            test.fail("Test failed.");
        }

        attachScreenshot(test, result);
        ExtentReportManager.removeTest();
        ExtentReportManager.flushReport();   // keep the HTML up to date after every test
    }

    @Override
    public void onTestFailedWithTimeout(ITestResult result) {
        onTestFailure(result);
    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {
        onTestFailure(result);
    }

    @Override
    public void onTestSkipped(ITestResult result) {

        logger.warn("TEST SKIPPED: {}", testName(result));

        ExtentTest test = reportEntry(result);
        Throwable throwable = result.getThrowable();

        if (throwable != null) {
            test.skip(throwable);
        } else {
            test.skip("Test was skipped.");
        }

        ExtentReportManager.removeTest();
        ExtentReportManager.flushReport();   // keep the HTML up to date after every test
    }

    // =========================
    // Configuration methods (@BeforeMethod / @AfterMethod …)
    // =========================

    @Override
    public void onConfigurationFailure(ITestResult result) {

        // The affected test is reported as SKIPPED by onTestSkipped; this
        // keeps the real cause (e.g. login failed) visible in the console.
        logger.error("CONFIGURATION FAILED: {}.{}",
                result.getTestClass().getRealClass().getSimpleName(),
                result.getMethod().getMethodName(),
                result.getThrowable());
    }

    // =========================
    // Helpers
    // =========================

    private static String testName(ITestResult result) {
        return result.getMethod().getMethodName();
    }

    /**
     * The report entry of this result; created if the test never went
     * through {@code onTestStart} (skipped by a configuration failure).
     */
    private static ExtentTest reportEntry(ITestResult result) {

        Object stored = result.getAttribute(EXTENT_TEST_ATTRIBUTE);
        if (stored instanceof ExtentTest test) {
            return test;
        }

        List<String> categories = new ArrayList<>();
        categories.add(result.getTestClass().getRealClass().getSimpleName());
        categories.addAll(Arrays.asList(result.getMethod().getGroups()));

        ExtentTest test = ExtentReportManager.createTestUnderPage(
                pageName(result),
                testName(result),
                result.getMethod().getDescription(),
                categories.toArray(String[]::new));

        result.setAttribute(EXTENT_TEST_ATTRIBUTE, test);
        return test;
    }

    /** Test class → the page title shown in the app's menu (parent entry in the report). */
    private static final java.util.Map<String, String> PAGE_NAMES = java.util.Map.of(
            "LoginTest", "تسجيل الدخول",
            "PurchaseRequisitionTest", "طلبات الشراء",
            "QuotationTest", "عروض الاسعار",
            "PurchaseOrderTest", "اوامر الشراء",
            "LocalPurchaseInvoiceTest", "فواتير المشتريات المحلية",
            "ForeignPurchaseInvoiceTest", "فواتير المشتريات الخارجية",
            "PurchaseInvoiceReturnTest", "فواتير مردود المشتريات",
            "FreeQuantityProcessingTest", "معالجة الكمية المجانية");

    /** Page title of the test's class; the class name when it is not in {@link #PAGE_NAMES}. */
    private static String pageName(ITestResult result) {
        String className = result.getTestClass().getRealClass().getSimpleName();
        return PAGE_NAMES.getOrDefault(className, className);
    }

    private static final String SCREENSHOT_ATTRIBUTE = "failureScreenshotBase64";

    /**
     * Takes the failure screenshot once per result (PNG file for the log +
     * Base64 kept on the result for the report).
     *
     * <p>Called from {@code BaseTest.tearDown} BEFORE the browser is closed,
     * and again from {@code onTestFailure}; whichever runs first captures it,
     * so the screenshot exists whatever order TestNG uses.</p>
     */
    public static void captureFailureScreenshot(ITestResult result) {

        if (result.getAttribute(SCREENSHOT_ATTRIBUTE) != null) {
            return;
        }

        WebDriver driver;
        try {
            driver = DriverManager.getDriver();
        } catch (IllegalStateException e) {
            return; // no browser: nothing to capture
        }

        String testName = testName(result);

        try {
            String screenshotPath = ScreenshotManager.takeScreenshot(driver, testName);
            logger.error("Screenshot saved: {}", screenshotPath);
        } catch (Exception e) {
            logger.error("Failed to save screenshot file.", e);
        }

        try {
            result.setAttribute(SCREENSHOT_ATTRIBUTE,
                    ((TakesScreenshot) driver).getScreenshotAs(OutputType.BASE64));
        } catch (Exception e) {
            logger.error("Failed to capture screenshot for the report.", e);
        }
    }

    /** Embeds the failure screenshot (captured now or in tearDown) in the report. */
    private static void attachScreenshot(ExtentTest test, ITestResult result) {

        captureFailureScreenshot(result);

        Object base64 = result.getAttribute(SCREENSHOT_ATTRIBUTE);
        if (base64 == null) {
            test.warning("Screenshot could not be captured (no browser available).");
            return;
        }

        try {
            test.fail("Screenshot at failure",
                    MediaEntityBuilder.createScreenCaptureFromBase64String(
                            base64.toString(), testName(result)).build());
        } catch (Exception e) {
            logger.error("Failed to attach screenshot to the report.", e);
            test.warning("Screenshot could not be attached.");
        }
    }
}
