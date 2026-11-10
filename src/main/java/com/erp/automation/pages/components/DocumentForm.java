package com.erp.automation.pages.components;

import com.erp.automation.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Read-side helper for any Purchasing document form (Requisition, Quotation,
 * Order, Invoice…). It reads what is on screen so tests can assert on it.
 *
 * <p>All locators are built from the screen's data-testid prefix, e.g.
 * {@code new DocumentForm(driver, "orders")} → {@code [data-testid='orders-note']}.</p>
 *
 * <p>Verified against qc.studioerp.com, Purchase Orders, 2026-10-26:</p>
 * <ul>
 *     <li>Success toast: react-hot-toast inside {@code [data-rht-toaster]},
 *         text "نجاح" + "تم الحفظ بنجاح." — visible for ~2–3 s only.</li>
 *     <li>Validation: {@code p.text-destructive} "هذا الحقل مطلوب" under each field,
 *         plus an error toast "خطأ" for document-level rules.</li>
 *     <li>Status: the {@code <p>} after the "الحالة" label.</li>
 *     <li>URL modes: /add, /view/{id}, /edit/{id} (draft), /view-approved/{id} (approved).</li>
 * </ul>
 */
public class DocumentForm extends BasePage {

    /** Form modes, recognised from the URL. */
    public enum Mode {
        ADD("/add"),
        VIEW("/view/"),
        EDIT_DRAFT("/edit/"),
        EDIT_APPROVED("/view-approved/");

        private final String urlPart;

        Mode(String urlPart) {
            this.urlPart = urlPart;
        }
    }

    /** A toast message split into its title line and its message. */
    public record Toast(String title, String message) {

        public static final String SUCCESS_TITLE = "نجاح";
        public static final String ERROR_TITLE = "خطأ";

        /**
         * Title and message together. Use this for "contains" checks: the toast
         * can be read while its title line is still rendering, in which case
         * the message ends up in {@code title} and {@code message} is empty.
         */
        public String fullText() {
            return message.isEmpty() ? title : title + "\n" + message;
        }

        public boolean isError() {
            return ERROR_TITLE.equals(title) || fullText().startsWith(ERROR_TITLE);
        }

        /**
         * Success = "نجاح" title, or — when only the message was captured —
         * a non-error toast whose text says "بنجاح" (e.g. "تم الحفظ بنجاح.").
         */
        public boolean isSuccess() {
            return SUCCESS_TITLE.equals(title)
                    || (!isError() && fullText().contains("بنجاح"));
        }

        @Override
        public String toString() {
            return "[" + title + "] " + message;
        }
    }

    public static final String REQUIRED_FIELD_MESSAGE = "هذا الحقل مطلوب";

    private static final By TOAST =
            By.cssSelector("[data-rht-toaster] > div");

    private static final By STATUS_LABEL_VALUE =
            By.xpath("//label[normalize-space()='الحالة']/following::p[1]");

    private static final By FIELD_ERROR =
            By.cssSelector("p.text-destructive");

    private static final By OPEN_DIALOG =
            By.cssSelector("[role='dialog'][data-state='open']");

    private final String prefix;

    public DocumentForm(WebDriver driver, String testIdPrefix) {
        super(driver);
        this.prefix = testIdPrefix;
    }

    // =========================
    // Locators
    // =========================

    private By testId(String name) {
        return By.cssSelector("[data-testid='" + prefix + "-" + name + "']");
    }

    // =========================
    // Header values
    // =========================

    /** Document number, e.g. PO2608-000001. Empty for an unsaved document. */
    public String getCode() {
        return valueOf(wait.waitForPresence(testId("code")));
    }

    /**
     * Number of the loaded reference document (e.g. PQ2608-000001, PR2608-000001).
     * Waits until it is filled, because it appears only after the download completes.
     */
    public String getReferenceDocument() {

        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> {
                    String value = valueOf(d.findElement(testId("referenceDocument")));
                    return value.isEmpty() ? null : value;
                });
    }

    public String getNote() {
        return valueOf(wait.waitForPresence(testId("note")));
    }

    /**
     * Waits until the document number is shown and returns it.
     * After save the URL switches to /view/ before the record's data is loaded.
     */
    public String waitForCode() {

        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> {
                    String code = valueOf(d.findElement(testId("code")));
                    return code.isEmpty() ? null : code;
                });
    }

    /**
     * Waits up to 10 s for the description to equal {@code expected}
     * (data loads asynchronously) and returns the last value seen.
     */
    public String waitForNote(String expected) {

        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .ignoring(StaleElementReferenceException.class)
                    .ignoring(NoSuchElementException.class)
                    .until(d -> expected.equals(valueOf(d.findElement(testId("note")))));
        } catch (TimeoutException ignored) {
            // fall through: the caller asserts on the returned value
        }
        return getNote();
    }

    /**
     * Sets the Description (البيان) through the native value setter so that
     * React registers the change (same technique as the page objects).
     */
    public void setNote(String note) {

        WebElement input = wait.waitForVisible(testId("note"));

        ((JavascriptExecutor) driver).executeScript(
                "const s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                        + "s.call(arguments[0],arguments[1]);"
                        + "arguments[0].dispatchEvent(new Event('input',{bubbles:true}));"
                        + "arguments[0].dispatchEvent(new Event('change',{bubbles:true}));",
                input, note
        );

        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(d -> note.equals(getNote()));
    }

    /** Text shown in a React-Select field, e.g. "PG005002 - اشرف الرشيدي". */
    public String getSelectedText(String field) {

        List<WebElement> values = wait.waitForPresence(testId(field))
                .findElements(By.cssSelector(".select__single-value"));

        return values.isEmpty() ? "" : values.get(0).getText().trim();
    }

    /**
     * Waits up to {@code timeout} for a React-Select field to show a value
     * (values loaded from a reference document arrive asynchronously).
     * Returns the value, or "" if it stays empty.
     */
    public String waitForSelectedText(String field, Duration timeout) {

        try {
            return new WebDriverWait(driver, timeout)
                    .ignoring(StaleElementReferenceException.class)
                    .ignoring(NoSuchElementException.class)
                    .until(d -> {
                        String text = getSelectedText(field);
                        return text.isEmpty() ? null : text;
                    });
        } catch (TimeoutException e) {
            return "";
        }
    }

    /** Status label as displayed ("مسودة", "معتمد", …). */
    public String getStatusLabel() {
        return wait.waitForVisible(STATUS_LABEL_VALUE).getText().trim();
    }

    /** Waits until the status shows {@code expectedLabel} (status refreshes after actions). */
    public void waitForStatusLabel(String expectedLabel) {

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> expectedLabel.equals(
                        d.findElement(STATUS_LABEL_VALUE).getText().trim()
                ));
    }

    // =========================
    // Editability
    // =========================

    /**
     * True when the user can change the field. Handles inputs, date pickers
     * and React-Select (which marks itself with {@code select--is-disabled}).
     */
    public boolean isFieldEditable(String field) {

        // React re-renders the form right after switching modes, so the
        // element is re-located on every attempt (avoids StaleElementReference).
        // The result is wrapped in Optional because WebDriverWait keeps
        // polling while the function returns Boolean.FALSE.
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    WebElement element = d.findElement(testId(field));
                    Object editable = ((JavascriptExecutor) d).executeScript(
                            "const e=arguments[0];"
                                    + "if(e.classList.contains('select--is-disabled')) return false;"
                                    + "if(e.matches('input,textarea,select')) return !e.disabled && !e.readOnly;"
                                    + "const i=e.querySelector('input,textarea,select');"
                                    + "if(i) return !i.disabled && !i.readOnly;"
                                    + "return !e.querySelector('.select__control--is-disabled');",
                            element
                    );
                    return Optional.of(Boolean.TRUE.equals(editable));
                })
                .get();
    }

    /**
     * Waits until {@code field} becomes editable. Use it after switching to an
     * edit mode: the URL changes before the form finishes re-rendering.
     */
    public void waitUntilFieldEditable(String field) {

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> isFieldEditable(field));
    }

    /** Returns the given fields that are currently editable (empty = all locked). */
    public List<String> editableFieldsAmong(List<String> fields) {

        List<String> editable = new ArrayList<>();
        for (String field : fields) {
            if (isFieldEditable(field)) {
                editable.add(field);
            }
        }
        return editable;
    }

    /** Toolbar action state, e.g. "edit", "delete", "approveOrDisapprove", "cancel". */
    public boolean isActionEnabled(String action) {
        return wait.waitForPresence(testId(action)).isEnabled();
    }

    // =========================
    // Detail grid (AG Grid lines)
    // =========================

    /*
     * Lines are addressed by their AG Grid row-index (0 = first line, the "#"
     * column shows row-index + 1), never by their position in the DOM:
     *
     *  - the data-testid suffix (orders-cost-0, -1 …) does not follow the
     *    visual order for lines loaded via "انزال من" (QC, 2026-10-26);
     *  - AG Grid renders only the rows near the visible area (row
     *    virtualization). With many lines (QC, 2026-10-27: Excel import with
     *    36+ lines) the n-th RENDERED row is not line n, so reading/editing by
     *    list position hit the wrong line once the grid had scrolled.
     *
     * Every lookup therefore scrolls the grid body until the wanted row-index
     * is rendered, then reads that row.
     */

    /** Body viewport of the detail grid (vertical scroll container). */
    private static final String GRID_VIEWPORT_JS =
            "const cc=document.querySelector('.ag-center-cols-container');"
                    + "const root=cc && cc.closest('.ag-root');"
                    + "const vp=root && root.querySelector('.ag-body-viewport');";

    /**
     * Number of detail lines in the grid, including lines not rendered yet.
     * Scrolls to the bottom to let AG Grid render the last row, reads its
     * row-index, then scrolls back to the top.
     */
    public int gridRowCount() {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        js.executeScript(GRID_VIEWPORT_JS + "if(vp) vp.scrollTop = vp.scrollHeight;");
        sleepQuietly(400);

        Number last = (Number) js.executeScript(
                "const idx=[...document.querySelectorAll("
                        + "'.ag-center-cols-container div[role=row][row-index]')]"
                        + ".map(r => r.getAttribute('row-index'))"
                        + ".filter(i => /^\\d+$/.test(i)).map(Number);"
                        + "return idx.length ? Math.max(...idx) : -1;");

        js.executeScript(GRID_VIEWPORT_JS + "if(vp) vp.scrollTop = 0;");
        sleepQuietly(300);

        return last == null ? 0 : last.intValue() + 1;
    }

    /**
     * The {@code column} cell of line {@code row} (row-index). Scrolls the grid
     * vertically until that row is rendered, then horizontally to the column.
     *
     * @throws IllegalStateException if the row/column never renders.
     */
    private WebElement gridCell(String column, int row) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String findCell =
                "const r=document.querySelector(\".ag-center-cols-container "
                        + "div[role=row][row-index='\"+arguments[0]+\"']\");"
                        + "return r ? r.querySelector(\"[col-id='\"+arguments[1]+\"']\") : null;";

        for (int attempt = 1; attempt <= 3; attempt++) {

            WebElement cell = (WebElement) js.executeScript(findCell, row, column);

            if (cell == null) {
                // Bring the row into the rendered window (row height from any rendered row).
                js.executeScript(GRID_VIEWPORT_JS
                                + "const any=document.querySelector("
                                + "'.ag-center-cols-container div[role=row][row-index]');"
                                + "const h=any ? any.offsetHeight : 42;"
                                + "if(vp) vp.scrollTop = Math.max(0, arguments[0]*h - vp.clientHeight/2);",
                        row);
                sleepQuietly(400);
                cell = (WebElement) js.executeScript(findCell, row, column);
            }

            if (cell != null) {
                try {
                    js.executeScript(
                            "arguments[0].scrollIntoView({block:'nearest',inline:'center'});", cell);
                    sleepQuietly(200);
                    WebElement fresh = (WebElement) js.executeScript(findCell, row, column);
                    if (fresh != null) {
                        return fresh;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // re-rendered while scrolling; try again
                }
            }
        }

        throw new IllegalStateException("Line " + (row + 1) + " (row-index " + row
                + ") column '" + column + "' is not rendered in the grid.");
    }

    /** Numeric value shown in line {@code row} (row-index); blank → 0. */
    public BigDecimal getGridNumber(String column, int row) {

        String raw = null;
        for (int attempt = 1; attempt <= 3 && raw == null; attempt++) {
            try {
                raw = gridCell(column, row).getText();
            } catch (StaleElementReferenceException e) {
                sleepQuietly(200);
            }
        }
        if (raw == null) {
            throw new IllegalStateException("Could not read line " + (row + 1)
                    + " column '" + column + "' (grid kept re-rendering).");
        }

        // Keep digits, dot and minus only (drops thousands separators and any unit text).
        String text = raw.replaceAll("[^0-9.\\-]", "");
        return text.isEmpty() || text.equals(".") || text.equals("-")
                ? BigDecimal.ZERO
                : new BigDecimal(text);
    }

    /**
     * Edits a numeric cell of line {@code row} (row-index):
     * double-click → native value setter on the editor that opened → TAB to commit
     * (ENTER is avoided: it can trigger "Add Line").
     *
     * <p>Double-click is required: a single click only focuses the cell. The
     * editor is taken from the focused element (falls back to any inline/popup
     * editor), not searched inside the cell, so it is found wherever AG Grid
     * renders it.</p>
     *
     * @throws IllegalStateException if no editor opens (cell is read-only).
     */
    public void setGridNumber(String column, int row, String value) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        boolean valueSet = false;
        String lastFocus = "";
        String rowIndex = String.valueOf(row);

        /*
         * Finds the editor of THIS line and sets the value in ONE script call.
         * No WebElement of the editor is kept on the Java side: AG Grid
         * re-renders the row right after the editor opens (seen in QC,
         * 2026-10-27, Excel import: StaleElementReferenceException on the
         * editor), so a held reference can go stale between two calls.
         * Returns "ok", "none" (no editor yet) or "other:<row-index>".
         */
        String setInEditor =
                "const row=arguments[0], col=arguments[1], val=arguments[2];"
                        + "const r=document.querySelector(\".ag-center-cols-container "
                        + "div[role=row][row-index='\"+row+\"']\");"
                        + "let ed = r && r.querySelector(\"[col-id='\"+col+\"'] input\");"
                        + "if(!ed){ const a=document.activeElement;"
                        + "  if(a && a.tagName==='INPUT'){ const ar=a.closest('[row-index]');"
                        + "    if(ar && ar.getAttribute('row-index')!==String(row))"
                        + "      return 'other:'+ar.getAttribute('row-index');"
                        + "    ed=a; } }"
                        + "if(!ed) ed=document.querySelector('.ag-popup-editor input');"
                        + "if(!ed) return 'none';"
                        + "const s=Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set;"
                        + "s.call(ed,val);"
                        + "ed.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "ed.focus();"
                        + "return ed.value===String(val) ? 'ok' : 'none';";

        for (int attempt = 1; attempt <= 3 && !valueSet; attempt++) {

            try {
                WebElement cell = gridCell(column, row);   // scrolls the row/column into view
                new Actions(driver).doubleClick(cell).perform();
            } catch (StaleElementReferenceException e) {
                lastFocus = "cell re-rendered before double-click";
                continue;
            }

            String result;
            try {
                result = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .pollingEvery(Duration.ofMillis(200))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            String r = String.valueOf(((JavascriptExecutor) d)
                                    .executeScript(setInEditor, row, column, value));
                            return "none".equals(r) ? null : r;
                        });
            } catch (TimeoutException e) {
                lastFocus = String.valueOf(js.executeScript(
                        "return (document.activeElement||{}).outerHTML?.slice(0,150);"));
                continue;
            }

            if (result.startsWith("other:")) {
                // The editor opened on a neighbour line: close it and retry.
                closeOpenEditorOnRow(result.substring("other:".length()));
                lastFocus = "editor opened on row-index " + result.substring(6)
                        + " instead of " + rowIndex;
                continue;
            }

            valueSet = true;
        }

        if (!valueSet) {
            throw new IllegalStateException("Line " + (row + 1) + " column '" + column
                    + "' did not open an editor after 3 attempts. Last state: " + lastFocus);
        }

        new Actions(driver).sendKeys(Keys.TAB).perform();

        // TAB commits THIS cell and opens the editor of the NEXT cell, so the grid
        // is never editor-free: wait only for the edited cell to leave edit mode.
        String editedRow = rowIndex;
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> Boolean.TRUE.equals(((JavascriptExecutor) d).executeScript(
                        "const r=document.querySelector("
                                + "\".ag-center-cols-container div[role=row][row-index='\"+arguments[0]+\"']\");"
                                + "const c=r && r.querySelector(\"[col-id='\"+arguments[1]+\"']\");"
                                + "return !!c && !c.classList.contains('ag-cell-inline-editing');",
                        editedRow, column)));

        // TAB opened an editor on the next cell (e.g. "total"). Escape does not
        // close it in this app; a single click on a read-only cell of the same
        // line does, without changing any value (verified in QC, 2026-10-26).
        closeOpenEditorOnRow(editedRow);
    }

    /** Read-only columns used to end editing by a single click. */
    private static final List<String> READ_ONLY_COLUMNS =
            List.of("mainCategoryName", "barcode");

    private void closeOpenEditorOnRow(String rowIndex) {

        By openEditor = By.cssSelector(".ag-cell-inline-editing, .ag-popup-editor");
        if (driver.findElements(openEditor).isEmpty()) {
            return;
        }

        JavascriptExecutor js = (JavascriptExecutor) driver;
        String rowSelector = ".ag-center-cols-container div[role=row][row-index='" + rowIndex + "']";

        for (String column : READ_ONLY_COLUMNS) {

            By cell = By.cssSelector(rowSelector + " [col-id='" + column + "']");
            List<WebElement> found = driver.findElements(cell);
            if (found.isEmpty()) {
                continue; // column not rendered (AG Grid column virtualization)
            }

            js.executeScript("arguments[0].scrollIntoView({block:'center',inline:'nearest'});", found.get(0));
            sleepQuietly(300);
            List<WebElement> fresh = driver.findElements(cell); // re-locate after scroll
            if (fresh.isEmpty()) {
                continue;
            }
            new Actions(driver).moveToElement(fresh.get(0)).click().perform();

            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(d -> d.findElements(openEditor).isEmpty());
                return;
            } catch (TimeoutException ignored) {
                // try the next read-only column
            }
        }
        // Not fatal: the next double-click on another cell also ends this editing.
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // =========================
    // Validation
    // =========================

    /**
     * Names (data-testid without the prefix) of the fields that currently
     * show "هذا الحقل مطلوب".
     */
    public List<String> fieldsWithRequiredError() {

        List<String> fields = new ArrayList<>();

        for (WebElement error : driver.findElements(FIELD_ERROR)) {

            if (!REQUIRED_FIELD_MESSAGE.equals(error.getText().trim())) {
                continue;
            }

            // The error <p> sits inside the same div.relative as the field.
            List<WebElement> owners = error.findElements(By.xpath(
                    "./ancestor::div[contains(concat(' ',normalize-space(@class),' '),' relative ')][1]"
                            + "//*[starts-with(@data-testid,'" + prefix + "-')]"
            ));

            if (!owners.isEmpty()) {
                fields.add(owners.get(0)
                        .getAttribute("data-testid")
                        .substring(prefix.length() + 1));
            }
        }

        return fields;
    }

    // =========================
    // Toasts & dialogs
    // =========================

    /**
     * Waits for the next toast and returns it. Call it right after the action:
     * toasts disappear after ~2–3 seconds.
     */
    public Toast waitForToast() {

        WebElement toast = new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(100))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(TOAST).stream()
                        .filter(e -> !e.getText().isBlank())
                        .findFirst()
                        .orElse(null));

        // Give the title line a moment to render (it can lag behind the message).
        String text = toast.getText();
        long deadline = System.currentTimeMillis() + 1500;
        while (!text.contains("\n") && System.currentTimeMillis() < deadline) {
            try {
                Thread.sleep(100);
                text = toast.getText();
            } catch (StaleElementReferenceException e) {
                break; // toast already gone: keep what was read
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        return parseToast(text);
    }

    /**
     * After clicking Save: waits for whichever comes first.
     * <ul>
     *     <li>the confirmation dialog → returns {@code Optional.empty()} (save may proceed);</li>
     *     <li>a toast → returns it (the app rejected the save before confirmation).</li>
     * </ul>
     *
     * @throws TimeoutException if neither appears within 15 s
     *         (typically: inline "هذا الحقل مطلوب" errors without a toast).
     */
    public Optional<Toast> waitForConfirmationDialogOrToast() {

        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(100))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    if (!d.findElements(OPEN_DIALOG).isEmpty()) {
                        return Optional.<Toast>empty();
                    }
                    for (WebElement toast : d.findElements(TOAST)) {
                        String text = toast.getText();
                        if (!text.isBlank()) {
                            return Optional.of(parseToast(text));
                        }
                    }
                    return null;
                });
    }

    private static Toast parseToast(String text) {

        String[] lines = text.trim().split("\\R", 2);

        return new Toast(
                lines[0].trim(),
                lines.length > 1 ? lines[1].trim() : ""
        );
    }

    public boolean isDialogOpen() {
        return !driver.findElements(OPEN_DIALOG).isEmpty();
    }

    /** Title of the item-search dialog that opens when an item is not accepted. */
    private static final String ITEM_SEARCH_TITLE = "البحث عن صنف";

    /** Text shown by the item-search dialog when it has no results. */
    private static final String EMPTY_RESULTS_TEXT = "المحتوى فارغ";

    /**
     * Closes the "البحث عن صنف" dialog if it is open AND shows no results.
     *
     * @return true if such a dialog was found and closed; false otherwise
     *         (no dialog, another dialog, or the dialog has results).
     */
    public boolean closeEmptyItemSearchDialog() {

        List<WebElement> dialogs = driver.findElements(OPEN_DIALOG);
        if (dialogs.isEmpty()) {
            return false;
        }

        WebElement dialog = dialogs.get(dialogs.size() - 1); // top-most
        String text = dialog.getText();
        if (!text.contains(ITEM_SEARCH_TITLE) || !text.contains(EMPTY_RESULTS_TEXT)) {
            return false;
        }

        List<WebElement> close = dialog.findElements(
                By.xpath(".//button[.//span[normalize-space()='Close']]"));
        if (close.isEmpty()) {
            return false;
        }
        close.get(0).click();

        return waitForDialogToClose(Duration.ofSeconds(5));
    }

    /** Waits up to {@code timeout} for every dialog to close; true if they did. */
    public boolean waitForDialogToClose(Duration timeout) {

        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(200))
                    .until(d -> d.findElements(OPEN_DIALOG).isEmpty());
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /** Title of the open dialog, or "none" (for failure diagnostics). */
    public String openDialogTitle() {

        Object title = ((JavascriptExecutor) driver).executeScript(
                "const d=document.querySelector(\"[role='dialog'][data-state='open']\");"
                        + "if(!d) return 'none';"
                        + "const h=d.querySelector('h2');"
                        + "return (h?h.innerText:d.innerText).trim().slice(0,120);");
        return String.valueOf(title);
    }

    /**
     * Diagnostic snapshot of every button whose icon has {@code iconClass}
     * (e.g. "lucide-download"): owning field, displayed, disabled, covered.
     */
    public String describeButtonsWithIcon(String iconClass) {

        Object description = ((JavascriptExecutor) driver).executeScript(
                "const cls=arguments[0];"
                        + "const out=[...document.querySelectorAll('button')]"
                        + ".filter(b=>b.querySelector('svg.'+cls)).map((b,i)=>{"
                        + "  let p=b.parentElement,id='?';"
                        + "  for(let k=0;k<4&&p;k++,p=p.parentElement){"
                        + "    const inp=p.querySelector('input[id]'); if(inp){id=inp.id;break;} }"
                        + "  const r=b.getBoundingClientRect();"
                        + "  const top=document.elementFromPoint(r.x+r.width/2,r.y+r.height/2);"
                        + "  const covered=!!top && !b.contains(top);"
                        + "  return '#'+i+' field='+id+' displayed='+(b.offsetParent!==null)"
                        + "    +' disabled='+b.disabled+' covered='+covered; });"
                        + "return out.length? out.join('; ') : 'no button with icon '+cls;",
                iconClass);
        return String.valueOf(description);
    }

    // =========================
    // Mode
    // =========================

    /**
     * True once a new, unsaved document is shown: the URL is in Add mode or the
     * document number is empty. Returns false if that does not happen in time.
     */
    public boolean waitForNewDocument(Duration timeout) {

        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(250))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        if (d.getCurrentUrl().contains(Mode.ADD.urlPart)) {
                            return true;
                        }
                        List<WebElement> code = d.findElements(testId("code"));
                        return !code.isEmpty() && valueOf(code.get(0)).isEmpty();
                    });
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    public void waitForMode(Mode mode) {
        waitForUrlContains(mode.urlPart);
    }

    public boolean isInMode(Mode mode) {
        return getCurrentUrl().contains(mode.urlPart);
    }

    // =========================
    // Helpers
    // =========================

    private static String valueOf(WebElement input) {
        String value = input.getAttribute("value");
        return value == null ? "" : value.trim();
    }
}
