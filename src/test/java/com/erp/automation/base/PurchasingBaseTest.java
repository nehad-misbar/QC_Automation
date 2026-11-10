package com.erp.automation.base;

import com.erp.automation.config.ConfigReader;
import com.erp.automation.core.DriverFactory;
import com.erp.automation.core.DriverManager;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.LoginPage;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.utils.WaitManager;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.ApiRecorder;
import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.BeforeMethod;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;

/**
 * Base class for every screen under Purchasing → Operations.
 *
 * <p>Responsibilities (so each test class doesn't repeat them):</p>
 * <ul>
 *     <li>Log in and open Purchasing → Operations before every test.</li>
 *     <li>Let the subclass open its own screen through {@link #openScreen(PurchasingPage)}.</li>
 *     <li>Provide shared helpers: status guards, bounded retries,
 *         confirmation-dialog verification, and the (temporary) fixed pause.</li>
 * </ul>
 *
 * <p>How to use it for a new Purchasing screen:</p>
 * <pre>
 * public class QuotationTest extends PurchasingBaseTest {
 *
 *     private QuotationPage quotationPage;
 *
 *     &#64;Override
 *     protected void openScreen(PurchasingPage purchasingPage) {
 *         purchasingPage.clickPriceOffers();
 *         quotationPage = new QuotationPage(driver);
 *     }
 * }
 * </pre>
 */
public abstract class PurchasingBaseTest extends BaseTest {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * The confirmation button shared by every ERP modal
     * (save / delete / approve / cancel confirmations).
     */
    private static final By MODAL_CONFIRM_BUTTON =
            By.cssSelector("button[data-modal-agree][type='submit']");

    /** Upper bound for "retry until it works" loops so a test can never hang forever. */
    protected static final int DEFAULT_MAX_ATTEMPTS = 5;

    // =========================
    // Setup
    // =========================

    /**
     * Runs after {@link BaseTest#setUp()} (TestNG runs superclass
     * {@code @BeforeMethod}s first), so {@code driver} is ready here.
     */
    @BeforeMethod(alwaysRun = true)
    public void loginAndOpenScreen() {

        // Chrome sometimes stops answering during login ("Timed out receiving
        // message from renderer", 2026-10-28 and 2026-10-31, each time right
        // after several tests in a row). One retry in a fresh browser; if that
        // fails too, the original error is reported.
        try {
            loginAndOpenScreenOnce();
        } catch (WebDriverException firstTry) {
            log.warn("Login / opening the screen failed ({}). Retrying once in a new browser.",
                    firstTry.getClass().getSimpleName() + ": " + firstLine(firstTry.getMessage()));
            restartBrowser();
            try {
                loginAndOpenScreenOnce();
            } catch (WebDriverException secondTry) {
                secondTry.addSuppressed(firstTry);
                throw secondTry;
            }
        }
    }

    private void loginAndOpenScreenOnce() {

        new LoginPage(driver)
                .open(ConfigReader.get("baseUrl"))
                .login(
                        ConfigReader.get("username"),
                        ConfigReader.get("password")
                );

        PurchasingPage purchasingPage = new PurchasingPage(driver);
        purchasingPage.clickPurchasing();
        purchasingPage.clickOperations();

        openScreen(purchasingPage);
    }

    /** Closes the (hung) browser and opens a new one, as BaseTest.setUp() does. */
    private void restartBrowser() {

        try {
            DriverManager.quitDriver();
        } catch (RuntimeException alreadyGone) {
            log.warn("Closing the hung browser failed: {}", firstLine(alreadyGone.getMessage()));
        }
        driver = DriverFactory.createDriver(ConfigReader.get("browser"));
        DriverManager.setDriver(driver);
        driver.manage().window().maximize();
    }

    private static String firstLine(String text) {

        return text == null ? "" : text.lines().findFirst().orElse("");
    }

    /**
     * Opens the screen under test from the Purchasing → Operations menu
     * and initialises the screen's page object.
     */
    protected abstract void openScreen(PurchasingPage purchasingPage);

    // =========================
    // Status guards
    // =========================

    /**
     * Skips (does not pass) the current test when the randomly selected
     * document is not in one of the statuses the scenario needs.
     *
     * <p>A skipped test shows up in the report as "skipped", which is honest.
     * Printing a message and returning would mark the test green even though
     * nothing was tested.</p>
     */
    protected void requireStatus(DocumentStatus actual, DocumentStatus... allowed) {

        if (!actual.isOneOf(allowed)) {
            throw new SkipException(
                    "Selected document status is [" + actual.label()
                            + "]; scenario requires one of "
                            + Arrays.toString(allowed) + "."
            );
        }

        log.info("Selected document status: {}", actual.label());
    }

    // =========================
    // Verification
    // =========================

    /**
     * Minimal post-condition after confirming a modal action:
     * the confirmation dialog must close. If the server rejects the action
     * the dialog normally stays open, so this catches silent failures.
     *
     * <p>Stronger checks (success toast, new status in the grid) belong in
     * the page object once those locators are known.</p>
     */
    protected void verifyConfirmationDialogClosed(String action) {

        try {
            new WaitManager(driver).waitForInvisible(MODAL_CONFIRM_BUTTON);
        } catch (TimeoutException e) {
            Assert.fail(
                    "Confirmation dialog is still open after: " + action, e
            );
        }

        log.info("{} confirmed; dialog closed.", action);
    }

    // =========================
    // Retry
    // =========================

    /**
     * Repeats {@code attempt} until it returns true, at most
     * {@code maxAttempts} times, then fails with a clear message.
     * Replaces open-ended {@code while (!done)} loops.
     */
    protected void retryUntilSuccessful(
            String description,
            int maxAttempts,
            BooleanSupplier attempt) {

        for (int i = 1; i <= maxAttempts; i++) {

            if (attempt.getAsBoolean()) {
                return;
            }

            log.warn(
                    "{} failed (attempt {}/{}). Current URL: {}",
                    description, i, maxAttempts, driver.getCurrentUrl()
            );
        }

        Assert.fail(description + " did not succeed after "
                + maxAttempts + " attempts.");
    }

    // =========================
    // Explicit waits (shared by every Purchasing screen, item 3)
    // =========================

    /** Polling interval of the explicit waits below (not a fixed wait for the UI). */
    protected static final long POLL_MILLIS = 200;

    /**
     * Repeats {@code condition} until it is true or {@code timeout} passes,
     * then fails with {@code what}. Use it instead of a fixed pause.
     */
    protected void waitUntil(String what, Duration timeout, BooleanSupplier condition) {

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            try {
                if (condition.getAsBoolean()) {
                    return;
                }
            } catch (StaleElementReferenceException | NoSuchElementException rerendered) {
                // the page re-rendered; check again
            }
            pause(POLL_MILLIS);
        }
        Assert.fail("Timed out after " + timeout.toSeconds() + " s waiting for " + what
                + ". URL: " + driver.getCurrentUrl());
    }

    /**
     * Re-reads {@code reader} until it returns {@code expected} (max 10 s) and
     * returns the last value read, so the caller's assertion shows it.
     */
    protected String waitUntilEquals(Supplier<String> reader, String expected) {

        long end = System.currentTimeMillis() + 10_000;
        String value = "";
        while (System.currentTimeMillis() < end) {
            try {
                value = reader.get();
                if (expected.equals(value)) {
                    return value;
                }
            } catch (StaleElementReferenceException | NoSuchElementException ignored) {
                // form re-rendering; read again
            }
            pause(POLL_MILLIS);
        }
        return value;
    }

    /** Waits (max 5 s) for a confirmation dialog of {@code form}; true when one is open. */
    protected boolean waitForConfirmDialog(DocumentForm form) {

        long end = System.currentTimeMillis() + 5_000;
        while (!form.isDialogOpen() && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
        }
        return form.isDialogOpen();
    }

    // =========================
    // Actions judged by the server's answer (shared, item 3)
    // =========================

    /** What the screen (toast) and the server (recorded API calls) answered to an action. */
    protected record ActionOutcome(DocumentForm.Toast toast, List<String> apiCalls) {

        /** True when the toast or an API answer contains {@code text} (spaces normalised). */
        public boolean mentions(String text) {
            // The server message has extra spaces ("مرتبط   ب امر استلام").
            String wanted = text.replaceAll("\\s+", " ");
            return (toast != null && toast.fullText().replaceAll("\\s+", " ").contains(wanted))
                    || apiCalls.stream().anyMatch(c -> c.replaceAll("\\s+", " ").contains(wanted));
        }

        @Override
        public String toString() {
            return "toast=" + (toast == null ? "none" : toast.fullText().replace('\n', ' ')) + ", api=" + apiCalls;
        }
    }

    /**
     * Runs {@code action} while recording the API calls, then returns the toast
     * of {@code form} (null when none appears) and the calls once one contains
     * {@code apiPart}. The API answer is the evidence; the toast alone was wrong
     * once (QC, 2026-10-31).
     */
    protected ActionOutcome performAndRecord(DocumentForm form, String apiPart, Runnable action) {

        ApiRecorder recorder = new ApiRecorder(driver);
        recorder.startRecordingApiCalls();
        action.run();
        DocumentForm.Toast toast = null;
        try {
            toast = form.waitForToast();
        } catch (TimeoutException noToast) {
            // judged by the API call
        }
        long end = System.currentTimeMillis() + 10_000;
        List<String> calls = recorder.getRecordedApiCalls();
        while (calls.stream().noneMatch(c -> c.contains(apiPart)) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            calls = recorder.getRecordedApiCalls();
        }
        ActionOutcome outcome = new ActionOutcome(toast, calls);
        log.info("Outcome of {}: {}", apiPart, outcome);
        return outcome;
    }

    /** The {@code apiPart} request was sent and answered 2xx, with no error toast. */
    protected void assertAccepted(String what, String apiPart, ActionOutcome outcome) {

        boolean sent = outcome.apiCalls().stream().anyMatch(c -> c.contains(apiPart));
        boolean ok = outcome.apiCalls().stream()
                .anyMatch(c -> c.contains(apiPart) && c.matches("(?s).* -> 2\\d\\d .*"));
        Assert.assertTrue(sent, what + ": no " + apiPart + " request was sent. " + outcome);
        Assert.assertTrue(ok && (outcome.toast() == null || !outcome.toast().isError()),
                what + " was rejected: " + outcome);
    }

    // =========================
    // Fixed pause (temporary)
    // =========================

    /**
     * Fixed wait kept ONLY where the current page objects still need it.
     *
     * <p>Every call is technical debt: the goal is to replace it with an
     * explicit wait inside the related page-object method, then delete the
     * call here. Centralising it makes those spots easy to find
     * (search for {@code pause(}).</p>
     */
    protected void pause(long millis) {

        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Test thread was interrupted.", e);
        }
    }
}
