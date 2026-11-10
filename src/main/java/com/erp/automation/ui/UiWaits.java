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
 * Explicit waits and safe clicks shared by every StudioERP screen
 * (Purchasing documents, Sales, Inventory …). Item 3, QC 2026-10-31.
 *
 * <p>Use these instead of fixed sleeps. Each method waits for a visible
 * condition (an animation ended, a toast disappeared, a grid stopped changing)
 * and documents the QC behaviour that made it necessary.</p>
 */
public class UiWaits {

    private final WebDriver driver;

    public UiWaits(WebDriver driver) {

        this.driver = driver;
    }

    /**
     * Clicks {@code locator} once it can really receive the click.
     *
     * <p>QC, 2026-10-31: right after jumping to the last page the pager
     * re-lays out ("311 - 312 / 312" is wider) and the "first" button click was
     * intercepted by the summary span. Retries (max 10 s) on a click that is
     * intercepted or hits a re-rendered element, scrolling the element to the
     * centre first.</p>
     */
    public void clickWhenReady(By locator) {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(250))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    WebElement element = ExpectedConditions.elementToBeClickable(locator).apply(d);
                    if (element == null) {
                        return false;
                    }
                    ((JavascriptExecutor) d).executeScript(
                            "arguments[0].scrollIntoView({block:'center', inline:'center'});", element);
                    try {
                        element.click();
                        return true;
                    } catch (ElementClickInterceptedException covered) {
                        // QC, 2026-10-31: the "تثبيت Primex ERP" install pop-up
                        // (bottom corner) covered the pager buttons.
                        dismissInstallPrompt();
                        return false;
                    }
                });
    }

    /**
     * Closes the "تثبيت Primex ERP" (install the app) pop-up with "لا شكراً"
     * when it is shown. It sits in a bottom corner and covers the pager's
     * first/previous buttons. Returns true when it was closed.
     */
    public boolean dismissInstallPrompt() {

        List<WebElement> noThanks = driver.findElements(By.xpath(
                "//button[contains(normalize-space(),'لا شكر')]"));
        for (WebElement button : noThanks) {
            if (button.isDisplayed()) {
                button.click();
                System.out.println("Closed the 'تثبيت Primex ERP' pop-up (لا شكراً).");
                return true;
            }
        }
        return false;
    }

    /** Waits until the text of {@code locator} has not changed for {@code stableFor}. */
    public void waitForStableText(By locator, Duration stableFor, Duration timeout) {

        long end = System.currentTimeMillis() + timeout.toMillis();
        String last = null;
        long since = System.currentTimeMillis();
        while (System.currentTimeMillis() < end) {
            String now;
            try {
                now = driver.findElement(locator).getText();
            } catch (StaleElementReferenceException | NoSuchElementException e) {
                now = null;
            }
            if (now == null || !now.equals(last)) {
                last = now;
                since = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - since >= stableFor.toMillis()) {
                return;
            }
            pollPause();
        }
        System.out.println("WARNING: " + locator + " still changing after " + timeout.toSeconds() + " s.");
    }

    /** Waits until the element's CSS animations (and its children's) have finished, max 3 s. */
    public void waitForAnimationsToFinish(By locator) {

        new WebDriverWait(driver, Duration.ofSeconds(3))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                        "const e=document.querySelector(arguments[0]);"
                                + "return !!e && e.getAnimations({subtree:true})"
                                + ".every(a => a.playState !== 'running');",
                        cssOf(locator)));
    }

    /**
     * A confirmation modal is ready: its confirm button ([data-modal-agree],
     * shared by every ERP modal: save, delete, approve, cancel, undo) is visible
     * and the modal's open animation has finished.
     *
     * <p>Not for the print-template chooser: it has no modal
     * (use {@link #openPrintTemplateChooser()}).</p>
     */
    public void waitForOpenDialogReady() {

        By confirm = By.cssSelector("button[data-modal-agree]");
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(confirm));
        new WebDriverWait(driver, Duration.ofSeconds(3))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> (Boolean) ((JavascriptExecutor) d).executeScript(
                        "const b=[...document.querySelectorAll('button[data-modal-agree]')]"
                                + ".find(x => x.offsetParent !== null);"
                                + "const m=b && (b.closest('[role=dialog],[role=alertdialog],[data-state]') || b.parentElement);"
                                + "return !!m && m.getAnimations({subtree:true})"
                                + ".every(a => a.playState !== 'running');"));
    }

    /**
     * Waits (max {@code timeout}) until no AG Grid cell editor is open.
     * Returns false instead of failing: some editors (e.g. the Currency
     * select of "الخصومات والإضافات") stay open after their value is committed
     * and only close when the next cell is edited (QC, 2026-10-31).
     */
    public boolean waitForNoCellEditor(Duration timeout) {

        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.invisibilityOfElementLocated(
                            By.cssSelector(".ag-cell-inline-editing, .ag-popup-editor")));
            return true;
        } catch (TimeoutException stillOpen) {
            System.out.println("A cell editor is still open after " + timeout.toMillis()
                    + " ms; continuing (the next cell edit closes it).");
            return false;
        }
    }

    /** The number of rows of the visible grid has not changed for 1 s (max 10 s). */
    public void waitForGridToSettle() {

        By rows = By.cssSelector(".ag-center-cols-container div[role='row'][row-index]");
        long end = System.currentTimeMillis() + 10_000;
        int last = -1;
        long since = System.currentTimeMillis();
        while (System.currentTimeMillis() < end) {
            int now = driver.findElements(rows).size();
            if (now != last) {
                last = now;
                since = System.currentTimeMillis();
            } else if (System.currentTimeMillis() - since >= 1_000) {
                return;
            }
            pollPause();
        }
    }

    /** One AG Grid render after a horizontal scroll (two animation frames, max 1 s). */
    public void waitForGridColumnsToRender() {

        ((JavascriptExecutor) driver).executeAsyncScript(
                "const done=arguments[arguments.length-1];"
                        + "requestAnimationFrame(()=>requestAnimationFrame(()=>done(true)));"
                        + "setTimeout(()=>done(false),1000);");
    }

    /**
     * Waits (max 15 s) until no toast is visible any more.
     *
     * <p>QC, 2026-10-29: uploading attachments shows "تمت إضافة الملفات بنجاح /
     * تم الرفع بنجاح". When the order is saved right after, that toast is still
     * on screen and DocumentForm.waitForToast() reads it instead of the save
     * toast. Call this before an action whose own toast must be checked.</p>
     */
    public void waitForToastsToClear() {

        By toast = By.cssSelector("[data-rht-toaster] > div");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(toast).stream()
                            .noneMatch(t -> t.isDisplayed() && !t.getText().isBlank()));
        } catch (TimeoutException e) {
            System.out.println("WARNING: a toast is still visible after 15 s: "
                    + driver.findElements(toast).stream().map(WebElement::getText)
                    .map(x -> x.replace('\n', ' ')).toList());
        }
    }

    /**
     * Waits for a toast that contains {@code text} to APPEAR (max 8 s), then
     * waits until no toast is visible any more (max 15 s).
     *
     * <p>QC, 2026-10-31: the upload toast "تمت إضافة الملفات بنجاح / تم الرفع
     * بنجاح" does not show at once. Waiting only for "no toast" passed right
     * away, the toast showed up during the save, and DocumentForm.waitForToast()
     * read it instead of the save toast. Pass "" to wait for any toast.</p>
     */
    public void waitForToastToAppearAndClear(String text) {

        By toast = By.cssSelector("[data-rht-toaster] > div");
        long start = System.currentTimeMillis();
        try {
            new WebDriverWait(driver, Duration.ofSeconds(8))
                    .pollingEvery(Duration.ofMillis(100))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(toast).stream()
                            .anyMatch(t -> t.isDisplayed() && t.getText().contains(text)
                                    && !t.getText().isBlank()));
            System.out.println("Toast [" + text + "] appeared after "
                    + (System.currentTimeMillis() - start) + " ms.");
        } catch (TimeoutException e) {
            System.out.println("Toast [" + text + "] did not appear within 8 s; continuing.");
        }
        waitForToastsToClear();
        System.out.println("No toast visible after " + (System.currentTimeMillis() - start) + " ms.");
    }

    /**
     * Texts of column {@code colId} in the active AG Grid, in row order
     * (read by row-index with textContent, so off-screen cells count too).
     * Works on any screen with one grid, e.g. the order lines ("itemId") and
     * Inventory → Item Barcodes.
     */
    @SuppressWarnings("unchecked")
    public List<String> getGridColumnTexts(String colId) {

        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.presenceOfElementLocated(
                        By.cssSelector(".ag-center-cols-container div[role='row'][row-index] [col-id='" + colId + "']")));
        return (List<String>) ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('.ag-center-cols-container div[role=row][row-index]')]"
                        + ".sort((a,b)=>Number(a.getAttribute('row-index'))-Number(b.getAttribute('row-index')))"
                        + ".map(r=>{const c=r.querySelector(\"[col-id='\"+arguments[0]+\"']\");"
                        + "return c ? c.textContent.trim() : '';});",
                colId);
    }

    /** CSS selector string of a By.cssSelector locator. */
    private static String cssOf(By locator) {

        String text = locator.toString();
        return text.substring(text.indexOf(':') + 1).trim();
    }

    /** Polling interval of the explicit waits above (not a fixed wait for the UI). Shared by the ui components.. */
    public static void pollPause() {

        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting.", e);
        }
    }
}
