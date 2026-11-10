package com.erp.automation.pages;

import com.erp.automation.ui.ApiRecorder;
import com.erp.automation.ui.DocumentApi;
import com.erp.automation.ui.DocumentHeader;
import com.erp.automation.ui.DocumentList;
import com.erp.automation.ui.Downloads;
import com.erp.automation.ui.UiWaits;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Shared page object for every Purchasing document screen.
 * The screens are Requisitions, Quotations, Orders, Invoices and Returns.
 *
 * <p>They are all built from the same parts:</p>
 * <ul>
 *     <li>the same toolbar ({@code <prefix>-save}, {@code -edit}, …);</li>
 *     <li>React Select header fields ({@code <prefix>-<field>});</li>
 *     <li>the same "انزال من" + "الرقم" download;</li>
 *     <li>an AG Grid of lines;</li>
 *     <li>the same "⋮" menu and dialogs.</li>
 * </ul>
 * <p>Only the {@code data-testid} prefix, the URL and the API resource
 * differ. A screen's page object extends this class and adds only what is
 * really specific to that screen.</p>
 *
 * <p>Rules followed here (see docs/purchasing-tests-guide.md):</p>
 * <ul>
 *     <li>No fixed waits. Every wait polls a condition.</li>
 *     <li>One generic method per kind of control, not one per field.
 *         For example {@link #selectRandomOption(String)} serves every
 *         React Select field.</li>
 *     <li>Actions that the app is known to lose (a click while the toolbar
 *         re-renders) are retried once and then fail with the reason.</li>
 *     <li>Logging goes through SLF4J, never {@code System.out}.</li>
 * </ul>
 *
 * <p>{@code PurchaseOrderPage} predates this class and is not built on it
 * yet. It will be moved onto this class once the Purchase Requisition screen
 * is stable on it.</p>
 */
public abstract class PurchasingDocumentPage extends BasePage {

    protected final Logger log = LoggerFactory.getLogger(getClass());

    /** {@code data-testid} prefix of the screen, e.g. {@code requisitions}. */
    protected final String prefix;

    /** Path of the list, e.g. {@code /purchase/requisitions}. */
    protected final String screenPath;

    /** Form input id and list col-id of the document number ("code", or "qCode" on quotations). */
    protected final String codeField;

    protected final UiWaits ui;
    protected final ApiRecorder apiRecorder;
    protected final Downloads downloads;
    protected final DocumentList list;
    protected final DocumentHeader header;
    protected final DocumentApi api;

    protected final Random random = new Random();

    protected static final Duration WAIT = Duration.ofSeconds(10);
    private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ----- locators shared by every screen -----

    private static final By NEW_BUTTON =
            By.xpath("//button[normalize-space()='جديد' or .//span[normalize-space()='جديد']]");
    private static final By LIST_ROWS =
            By.cssSelector(".ag-center-cols-container div[role='row'][row-id]");
    private static final By OPEN_DIALOG =
            By.cssSelector("[role='dialog'][data-state='open'], [role='alertdialog'][data-state='open']");
    private static final By DIALOG_CONFIRM =
            By.cssSelector("button[data-modal-agree]");
    private static final By SELECT_OPTION =
            By.cssSelector("div[class*='select__option']");
    private static final By OPEN_MENU =
            By.cssSelector("[role='menu'][data-state='open']");
    private static final By ADD_LINE_BUTTON =
            By.xpath("//button[normalize-space()='اضافة سطر']");
    private static final By LINE_ROWS =
            By.cssSelector(".ag-center-cols-container div[role='row'][row-index]");
    private static final By EXCEL_FILE_INPUT =
            By.cssSelector("input[type='file'][accept*='.xlsx']");

    protected PurchasingDocumentPage(WebDriver driver, String prefix, String screenPath, String apiResource) {
        this(driver, prefix, screenPath, apiResource, "code");
    }

    /**
     * @param codeField id of the document-number input on the form and col-id
     *                  of the number column in the list. "code" on orders and
     *                  requisitions; "qCode" on price quotations (QC 2026-11-02).
     */
    protected PurchasingDocumentPage(WebDriver driver, String prefix, String screenPath, String apiResource,
                                     String codeField) {
        super(driver);
        this.prefix = prefix;
        this.screenPath = screenPath;
        this.codeField = codeField;
        this.listCodeCells = By.cssSelector(".ag-center-cols-container div[role='row'] [col-id='" + codeField + "']");
        this.ui = new UiWaits(driver);
        this.apiRecorder = new ApiRecorder(driver);
        this.downloads = new Downloads(driver);
        this.list = new DocumentList(driver);
        this.header = new DocumentHeader(driver, prefix);
        this.api = new DocumentApi(driver, apiResource);
    }

    /** {@code [data-testid='<prefix>-<id>']}. */
    protected By byTestId(String id) {
        return By.cssSelector("[data-testid='" + prefix + "-" + id + "']");
    }

    public String prefix() {
        return prefix;
    }

    public DocumentApi api() {
        return api;
    }

    public DocumentList list() {
        return list;
    }

    public Downloads downloads() {
        return downloads;
    }

    public ApiRecorder apiRecorder() {
        return apiRecorder;
    }

    /** Header values read in one pass (see {@link DocumentHeader}). */
    public Map<String, String> readHeaderValues(List<String> fields) {
        return header.readHeaderValues(fields);
    }

    // =====================================================================
    // List and opening documents
    // =====================================================================

    /** Closes the "تثبيت Primex ERP" pop-up when it is shown (it covers the pager). */
    public void dismissInstallPrompt() {
        ui.dismissInstallPrompt();
    }

    /** Waits until the list grid shows its rows. */
    public void waitForListLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.presenceOfElementLocated(LIST_ROWS));
    }

    /** "جديد" on the list → new, empty form. */
    public void clickNew() {
        ui.clickWhenReady(NEW_BUTTON);
        waitForUrl("/add");
    }

    /**
     * Opens a document by its id (view mode), the way a bookmarked link does.
     * Used after the record was chosen through {@link DocumentApi}, because
     * the list cannot tell the status of a record (see {@link DocumentApi}).
     */
    public void openDocument(Object id) {

        String origin = (String) ((JavascriptExecutor) driver).executeScript("return location.origin;");
        driver.navigate().to(origin + screenPath + "/view/" + id);
        waitForDocumentLoaded();
    }

    /**
     * Opens a random document matching {@code filter} (read through the API).
     *
     * @return the opened record, or empty when no document matches.
     */
    public Optional<Map<String, Object>> openRandomDocument(Predicate<Map<String, Object>> filter) {

        Map<String, Object> record = api.randomRecord(filter);
        if (record == null) {
            return Optional.empty();
        }
        log.info("Opening {} (id {}).", record.get(codeField), record.get("id"));
        openDocument(record.get("id"));
        return Optional.of(record);
    }

    private static final By NEXT_PAGE = By.xpath(
            "//button[@aria-label='Next'][.//span[contains(@class,'sr-only') and normalize-space()='Next']]");

    /**
     * Opens a random document whose "الحالة" column shows {@code statusLabel}
     * (e.g. "مسودة"). It moves through the list pages the same way as orders
     * (PurchaseOrderPage.selectRandomPurchaseOrderWithStatus): it checks the
     * current page, then "Next" page by page until one is found.
     *
     * @return the opened document's code, or empty when no page has one.
     */
    public Optional<String> openRandomFromListWithStatus(String statusLabel) {

        waitForListLoaded();
        for (int page = 1; ; page++) {
            List<String> codes = codesWithStatusOnCurrentPage(statusLabel);
            if (!codes.isEmpty()) {
                String code = codes.get(random.nextInt(codes.size()));
                log.info("Opening {} [{}] from list page {}.", code, statusLabel, page);
                openFromListByCode(code);
                return Optional.of(code);
            }
            if (!isNextPageEnabled()) {
                log.info("No document with status [{}] on any of the {} list page(s).", statusLabel, page);
                return Optional.empty();
            }
            goToNextPage();
        }
    }

    private static final String SCROLL_GRID_SIDEWAYS =
            "for (const sel of ['.ag-center-cols-viewport', '.ag-body-horizontal-scroll-viewport']) {"
                    + "  const el = document.querySelector(sel);"
                    + "  if (el) { el.scrollLeft = arguments[0]; el.dispatchEvent(new Event('scroll', {bubbles: true})); }"
                    + "}";

    /**
     * Codes of the rows on the current list page whose "الحالة" shows
     * {@code statusLabel}. Read with {@link #readListRows}: AG Grid renders
     * only the columns in view, and on quotations the number (qCode) and the
     * status are far apart (QC 2026-11-02: the status column is the last one,
     * and the old way never reached it, so no Draft was found on any page).
     */
    private List<String> codesWithStatusOnCurrentPage(String statusLabel) {
        List<String> codes = new ArrayList<>();
        readListRows(List.of(codeField, "recordStatus")).values().forEach(cells -> {
            if (statusLabel.equals(cells.get("recordStatus")) && !cells.getOrDefault(codeField, "").isEmpty()) {
                codes.add(cells.get(codeField));
            }
        });
        return codes;
    }

    /**
     * Cells {@code colIds} of every row on the current list page, by row-id.
     * The grid is scrolled sideways across its WHOLE width (both directions,
     * as the grid is right to left) until every row has every column, then
     * scrolled back to the start.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Map<String, String>> readListRows(List<String> colIds) {
        Object result = ((JavascriptExecutor) driver).executeAsyncScript(
                "const [cols, done] = arguments;"
                        + "const vps=['.ag-center-cols-viewport','.ag-body-horizontal-scroll-viewport']"
                        + "  .map(q=>document.querySelector(q)).filter(Boolean);"
                        + "const max=Math.max(0,...vps.map(v=>v.scrollWidth-v.clientWidth));"
                        + "const width=Math.max(300,...vps.map(v=>v.clientWidth));"
                        + "const step=Math.max(200,Math.floor(width/2));"
                        + "const pos=[0];"
                        + "for(let x=step;x<max+step;x+=step) pos.push(-Math.min(x,max));"
                        + "for(let x=step;x<max+step;x+=step) pos.push(Math.min(x,max));"
                        + "const out={}; let i=0;"
                        + "const scroll=x=>vps.forEach(v=>{v.scrollLeft=x; v.dispatchEvent(new Event('scroll',{bubbles:true}));});"
                        + "const read=()=>document.querySelectorAll('.ag-center-cols-container div[role=row][row-id]').forEach(r=>{"
                        + "  const id=r.getAttribute('row-id'); out[id]=out[id]||{};"
                        + "  cols.forEach(c=>{const cell=r.querySelector('[col-id=\"'+c+'\"]'); if(cell) out[id][c]=cell.textContent.trim();});});"
                        + "const complete=()=>Object.keys(out).length>0 && Object.values(out).every(o=>cols.every(c=>c in o));"
                        + "const next=()=>{read();"
                        + "  if(complete()||i>=pos.length){scroll(0); setTimeout(()=>done(out),250); return;}"
                        + "  scroll(pos[i++]); setTimeout(next,250);};"
                        + "next();",
                colIds);
        Map<String, Map<String, String>> rows = new java.util.LinkedHashMap<>();
        if (result instanceof Map<?, ?> map) {
            map.forEach((id, cells) -> rows.put(String.valueOf(id), (Map<String, String>) cells));
        }
        ui.waitForGridColumnsToRender();
        return rows;
    }

    private boolean isNextPageEnabled() {
        List<WebElement> next = driver.findElements(NEXT_PAGE);
        return !next.isEmpty() && next.get(0).isEnabled() && next.get(0).getAttribute("disabled") == null;
    }

    /** "Next" page; waits until the pagination summary and the rows show the new page. */
    public void goToNextPage() {
        int fromBefore = list.getPaginationRange()[0];
        ui.clickWhenReady(NEXT_PAGE);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(200))
                .ignoring(IllegalStateException.class)
                .until(d -> list.getPaginationRange()[0] != fromBefore);
        ui.waitForGridToSettle();
    }

    /** "Previous" page; waits until the pagination summary and the rows show the new page. */
    public void goToPreviousPage() {
        int fromBefore = list.getPaginationRange()[0];
        ui.clickWhenReady(PREVIOUS_PAGE);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(200))
                .ignoring(IllegalStateException.class)
                .until(d -> list.getPaginationRange()[0] != fromBefore);
        ui.waitForGridToSettle();
    }

    /** Number → status ("الحالة") of every row on the current list page (see {@link #readListRows}). */
    public Map<String, String> statusesOnCurrentPage() {
        Map<String, String> statusByCode = new java.util.LinkedHashMap<>();
        readListRows(List.of(codeField, "recordStatus")).values().forEach(cells -> {
            String code = cells.getOrDefault(codeField, "");
            if (!code.isEmpty()) {
                statusByCode.put(code, cells.getOrDefault("recordStatus", ""));
            }
        });
        return statusByCode;
    }

    /** Double-clicks the row with this code on the current list page and waits for the form. */
    public void openFromListByCode(String code) {
        // The number cell may be out of view (quotations, QC 2026-11-02), so the
        // row is found by its number and ANY of its cells is double-clicked.
        String rowId = readListRows(List.of(codeField)).entrySet().stream()
                .filter(e -> code.equals(e.getValue().get(codeField)))
                .map(Map.Entry::getKey).findFirst()
                .orElseThrow(() -> new NoSuchElementException(code + " is not on the current list page."));
        By cell = By.cssSelector(".ag-center-cols-container div[role='row'][row-id='" + rowId + "'] div[role='gridcell']");
        WebElement first = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(cell));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", first);
        new Actions(driver).doubleClick(driver.findElement(cell)).perform();
        waitForUrl("/view/");
        waitForDocumentLoaded();
    }


    /** The form is shown with its document number (data arrives after the URL changes). */
    public void waitForDocumentLoaded() {
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    List<WebElement> code = d.findElements(By.id(codeField));
                    return !code.isEmpty() && !code.get(0).getAttribute("value").isBlank();
                });
    }

    /** Document number shown on the form ("" while it is not loaded). */
    public String code() {
        return inputValue(codeField).trim();
    }

    /** Waits (max 30 s) until the form shows a document number, then returns it. */
    public String waitForCode() {
        waitForDocumentLoaded();
        return code();
    }

    /** Status badge shown under "الحالة" on the form ("" when not found). */
    public String statusLabel() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const l=[...document.querySelectorAll('label')].find(x=>x.textContent.trim()==='الحالة');"
                        + "if(!l) return '';"
                        // QC 2026-11-02 (quotations): label → div → div holding the badge.
                        + "const box=l.parentElement && l.parentElement.parentElement;"
                        + "return box ? box.innerText.replace('الحالة','').trim() : '';");
        return text == null ? "" : text.toString().trim();
    }

    /** Waits (max 20 s) until the status badge shows {@code label}. */
    public void waitForStatusLabel(String label) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20))
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> label.equals(statusLabel()));
        } catch (TimeoutException e) {
            throw new TimeoutException("Status is [" + statusLabel() + "], expected [" + label + "].", e);
        }
    }

    /** Reloads the browser page; the form shows what is saved on the server. */
    public void reload() {
        driver.navigate().refresh();
        waitForDocumentLoaded();
    }

    /** True while the form of a document (not the list) is shown. */
    public boolean isFormOpen() {
        return !driver.findElements(byTestId("xIcon")).isEmpty();
    }

    private void waitForUrl(String part) {
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains(part));
    }

    // =====================================================================
    // List search
    // =====================================================================

    /** "بحث ..." box of the list. It is rendered twice; only the displayed one is used. */
    private static final By LIST_SEARCH_INPUT =
            By.cssSelector("input[type='search'][placeholder^='بحث']");
    private final By listCodeCells;

    /** Types {@code text} in the list search box and waits until every shown code contains it (or none is shown). */
    public void searchList(String text) {
        WebElement input = new WebDriverWait(driver, WAIT)
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(LIST_SEARCH_INPUT).stream()
                        .filter(e -> e.isDisplayed() && e.isEnabled()).findFirst().orElse(null));
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(text);
        new WebDriverWait(driver, WAIT)
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> visibleListCodes().stream().allMatch(c -> c.contains(text)));
        log.info("List searched for [{}]: {}", text, visibleListCodes());
    }

    /** Document numbers shown on the current list page. */
    public List<String> visibleListCodes() {
        List<String> shown = driver.findElements(listCodeCells).stream()
                .map(e -> e.getText().trim())
                .filter(c -> !c.isEmpty())
                .toList();
        int rows = driver.findElements(By.cssSelector(".ag-center-cols-container div[role='row'][row-id]")).size();
        if (shown.size() >= rows) {
            return shown;
        }
        // The number column is not in view (quotations): read it across the grid's width.
        return readListRows(List.of(codeField)).values().stream()
                .map(cells -> cells.getOrDefault(codeField, ""))
                .filter(c -> !c.isEmpty())
                .toList();
    }

    /** Waits (max 10 s) until the list shows {@code code}; false when it does not. */
    public boolean waitUntilListShows(String code) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> visibleListCodes().contains(code));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // =====================================================================
    // Transaction log ("سجل العمليات")
    // =====================================================================

    private static final By TRANSACTION_LOG_BUTTON = By.id("transactionLogs");
    private static final By TRANSACTION_LOG_DIALOG = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='سجل العمليات']]");
    /** Action badge of each entry, e.g. "انشاء العملية", "اعتماد العملية". */
    private static final By TRANSACTION_LOG_ACTIONS = By.cssSelector("span.rounded-full");

    /**
     * Opens "سجل العمليات", reads the action of every entry (oldest first, as
     * shown) and closes it. The entries load after the dialog opens, so it
     * waits up to 10 s for them; an empty list means none arrived.
     */
    public List<String> readTransactionLog() {
        WebDriverWait wait = new WebDriverWait(driver, WAIT);
        WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(TRANSACTION_LOG_BUTTON));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({inline:'center', block:'nearest'});", button);
        wait.until(ExpectedConditions.elementToBeClickable(TRANSACTION_LOG_BUTTON)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(TRANSACTION_LOG_DIALOG));

        List<String> actions;
        try {
            actions = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        List<String> found = d.findElement(TRANSACTION_LOG_DIALOG)
                                .findElements(TRANSACTION_LOG_ACTIONS).stream()
                                .map(e -> e.getText().trim())
                                .filter(t -> !t.isEmpty())
                                .toList();
                        return found.isEmpty() ? null : found;
                    });
        } catch (TimeoutException none) {
            actions = List.of();
        }
        WebElement dialog = driver.findElement(TRANSACTION_LOG_DIALOG);
        dialog.findElement(By.xpath(".//button[normalize-space()='خروج']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(TRANSACTION_LOG_DIALOG));
        log.info("Transaction log: {}", actions);
        return actions;
    }

    // =====================================================================
    // Toolbar
    // =====================================================================

    /** Clicks toolbar button {@code <prefix>-<id>} once it is clickable. */
    public void clickAction(String id) {
        ui.clickWhenReady(byTestId(id));
    }

    /** True when toolbar button {@code <prefix>-<id>} is rendered (enabled or not). */
    public boolean hasAction(String id) {
        return !driver.findElements(byTestId(id)).isEmpty();
    }

    public boolean isActionEnabled(String id) {
        List<WebElement> buttons = driver.findElements(byTestId(id));
        return !buttons.isEmpty() && buttons.get(0).isEnabled();
    }

    /** Waits up to 10 s until toolbar action {@code id} is enabled; returns the final state. */
    public boolean waitForActionEnabled(String id) {
        try {
            new WebDriverWait(driver, WAIT)
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> isActionEnabled(id));
            return true;
        } catch (TimeoutException e) {
            log.warn("Toolbar action '{}' still disabled after {} s.", id, WAIT.toSeconds());
            return false;
        }
    }

    /** "نسخ": opens an unsaved copy of the opened document. No confirmation. */
    public void clickCopy() {
        waitForDocumentLoaded();
        clickAction("copy");
        waitForUrl("/add");
    }

    /**
     * "إضافة" on an opened document → new, empty form.
     * The first click can be lost while the opened document settles
     * (QC, 2026-10-27 on orders), so it is clicked again once.
     */
    public void clickAdd() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            clickAction("add");
            try {
                waitForUrl("/add");
                return;
            } catch (TimeoutException e) {
                log.warn("\"إضافة\" did not open a new form (attempt {}/2).", attempt);
            }
        }
        throw new TimeoutException("\"إضافة\" did not open a new form. URL: " + driver.getCurrentUrl());
    }

    /**
     * "تعديل" → edit mode. The URL changes to {@code /edit/} (Draft) or
     * {@code /view-approved/} (Approved). The click is retried once when the
     * URL does not change (lost click while the toolbar re-renders).
     */
    public void clickEdit() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            clickAction("edit");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(d -> d.getCurrentUrl().contains("/edit/")
                                || d.getCurrentUrl().contains("/view-approved/"));
                return;
            } catch (TimeoutException e) {
                log.warn("\"تعديل\" did not switch to edit mode (attempt {}/2).", attempt);
            }
        }
        throw new TimeoutException("The document did not switch to edit mode. URL: " + driver.getCurrentUrl());
    }

    public void clickSave() {
        clickAction("save");
    }

    public void clickUndo() {
        clickAction("undo");
    }

    public void clickDelete() {
        clickAction("delete");
    }

    public void clickApproveOrDisapprove() {
        clickAction("approveOrDisapprove");
    }

    public void clickCancelDocument() {
        clickAction("cancel");
    }

    /**
     * Closes the form with the "X" button and returns to the list.
     * Confirms "نعم" when the app asks to discard changes.
     * "إضافة" and "نسخ" open the new form on top of the current one, so up to
     * three forms are closed.
     */
    public void closeForm() {
        for (int i = 0; i < 3 && isFormOpen(); i++) {
            clickAction("xIcon");
            new WebDriverWait(driver, WAIT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> isDialogOpen() || !isFormOpen());
            if (isDialogOpen()) {
                confirmDialog();
            }
        }
    }

    // =====================================================================
    // Dialogs
    // =====================================================================

    public boolean isDialogOpen() {
        return driver.findElements(OPEN_DIALOG).stream().anyMatch(WebElement::isDisplayed);
    }

    /** Title (h2) of the top-most open dialog, or "" when none is open. */
    public String openDialogTitle() {
        List<WebElement> dialogs = driver.findElements(OPEN_DIALOG);
        if (dialogs.isEmpty()) {
            return "";
        }
        List<WebElement> titles = dialogs.get(dialogs.size() - 1).findElements(By.cssSelector("h2"));
        return titles.isEmpty() ? "" : titles.get(0).getText().trim();
    }

    /**
     * Clicks the confirm button ({@code data-modal-agree}: نعم / موافق /
     * متابعة) of the open dialog once it has finished animating, and waits
     * until the button is gone.
     */
    public void confirmDialog() {
        ui.waitForOpenDialogReady();
        WebElement button = visible(DIALOG_CONFIRM)
                .orElseThrow(() -> new NoSuchElementException("No confirm button in the open dialog."));
        button.click();
        new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.or(
                        ExpectedConditions.stalenessOf(button),
                        ExpectedConditions.invisibilityOf(button)));
    }

    /**
     * Some selections open an optional "متابعة" dialog, for example changing
     * the supplier of a document that already has lines. Confirms it when it
     * appears within {@code timeout}.
     *
     * @return true if a dialog was confirmed.
     */
    public boolean confirmDialogIfShown(Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(150))
                    .until(d -> visible(DIALOG_CONFIRM).isPresent());
        } catch (TimeoutException none) {
            return false;
        }
        log.info("Optional dialog [{}] shown → confirming.", openDialogTitle());
        confirmDialog();
        return true;
    }

    private Optional<WebElement> visible(By locator) {
        return driver.findElements(locator).stream()
                .filter(e -> {
                    try {
                        return e.isDisplayed();
                    } catch (StaleElementReferenceException gone) {
                        return false;
                    }
                })
                .findFirst();
    }

    // =====================================================================
    // Header fields
    // =====================================================================

    /**
     * Selects a random option of React Select field {@code <prefix>-<field>}.
     *
     * @return the selected option's text; "" when the field is disabled or
     *         offers no option (callers decide whether that is acceptable).
     */
    public String selectRandomOption(String field) {
        return selectOption(field, options -> options.get(random.nextInt(options.size())));
    }

    /**
     * Random option that is DIFFERENT from the current value (edit mode: the
     * field must really change). Falls back to any option when it is the only one.
     */
    public String selectRandomOtherOption(String field) {
        String current = selectedText(field).replaceAll("\\s+", " ").trim();
        return selectOption(field, options -> {
            List<String> others = options.stream()
                    .filter(o -> !o.replaceAll("\\s+", " ").trim().equals(current))
                    .toList();
            if (others.isEmpty()) {
                log.info("Field {} has no other option than [{}] → kept.", field, current);
                return options.get(0);
            }
            return others.get(random.nextInt(others.size()));
        });
    }

    /** Selects the option whose text is exactly {@code text}. */
    public String selectOption(String field, String text) {
        return selectOption(field, options -> {
            if (!options.contains(text)) {
                throw new NoSuchElementException("Option [" + text + "] not in " + field + ": " + options);
            }
            return text;
        });
    }

    /**
     * Option whose text contains every word of {@code text} ("فرع رئيسي" also matches
     * "الفرع الرئيسي"); an exact match wins when there is one.
     */
    public String selectOptionContaining(String field, String text) {
        return selectOption(field, options -> {
            if (options.contains(text)) {
                return text;
            }
            String[] words = normalizeArabic(text).replace("ال", "").trim().split("\\s+");
            return options.stream()
                    .filter(option -> java.util.Arrays.stream(words).allMatch(normalizeArabic(option)::contains))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No option like [" + text + "] in " + field + ": " + options));
        });
    }

    /** ى→ي, أ/إ/آ→ا, ة→ه, spaces collapsed: "فرع رئيسى" = "فرع رئيسي". */
    private static String normalizeArabic(String text) {
        return text.replace('ى', 'ي').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
                .replace('ة', 'ه').replaceAll("\\s+", " ").trim();
    }

    private String selectOption(String field, Function<List<String>, String> chooser) {

        By control = By.cssSelector("[data-testid='" + prefix + "-" + field + "'] .select__control");
        boolean hadNoOptions = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement controlElement = new WebDriverWait(driver, WAIT)
                        .until(ExpectedConditions.presenceOfElementLocated(control));
                if (isSelectDisabled(field)) {
                    log.info("Field {} is disabled → not changed.", field);
                    return "";
                }
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center'});", controlElement);
                controlElement.click();

                List<String> options = waitForOptions();
                if (options.isEmpty()) {
                    new Actions(driver).sendKeys(Keys.ESCAPE).perform();
                    // Right after a form opens, the first click can land before
                    // the lookup is loaded (menu opens empty). Try once more
                    // before concluding that the field really has no options.
                    if (hadNoOptions) {
                        log.warn("Field {} has no options.", field);
                        return "";
                    }
                    hadNoOptions = true;
                    log.warn("Field {} opened without options (attempt {}/3) → opening it again.", field, attempt);
                    continue;
                }
                String chosen = chooser.apply(options);
                clickOption(chosen);

                if (waitForSelectedText(field, chosen)) {
                    log.info("{} = {}", field, chosen);
                    return chosen;
                }
                log.warn("Field {}: option [{}] was clicked but the field shows [{}] (attempt {}/3).",
                        field, chosen, selectedText(field), attempt);

            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                log.warn("Field {} re-rendered while selecting (attempt {}/3).", field, attempt);
            }
        }
        throw new IllegalStateException("Could not select an option in " + field + " after 3 attempts.");
    }

    /** True when {@code field} shows {@code text} within 3 s (the selection was really committed). */
    private boolean waitForSelectedText(String field, String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .pollingEvery(Duration.ofMillis(150))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> selectedText(field).replaceAll("\\s+", " ").equals(text.replaceAll("\\s+", " ")));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Waits until React Select {@code field} is on the form and enabled.
     * Right after a form opens, fields are briefly missing or disabled while
     * the lookups load (QC, 2026-11-01). Without this wait "النوع" was taken
     * as locked and skipped, so filling started at "المورد".
     *
     * @return true when the field became enabled within {@code timeout}.
     */
    public boolean waitForSelectEnabled(String field, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(150))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> !isSelectDisabled(field));
            return true;
        } catch (TimeoutException stillLocked) {
            return false;
        }
    }

    /** True when field {@code <prefix>-<field>} is on the form (waits up to {@code timeout}). */
    public boolean hasField(String field, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(200))
                    .until(d -> !d.findElements(byTestId(field)).isEmpty());
            return true;
        } catch (TimeoutException absent) {
            return false;
        }
    }

    /** True when the React Select of {@code field} is disabled. */
    public boolean isSelectDisabled(String field) {
        return Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(
                "const f=document.querySelector(\"[data-testid='\"+arguments[0]+\"']\");"
                        + "if(!f) return true;"
                        + "const c=f.querySelector('.select__control');"
                        + "const i=f.querySelector('input');"
                        + "return !c || c.className.includes('is-disabled')"
                        + " || c.getAttribute('aria-disabled')==='true' || (i && i.disabled);",
                prefix + "-" + field));
    }

    /** Visible, non-empty option texts of the open React Select menu ([] after 3 s). */
    private List<String> waitForOptions() {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(3))
                    .pollingEvery(Duration.ofMillis(150))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        List<String> texts = new ArrayList<>();
                        for (WebElement option : d.findElements(SELECT_OPTION)) {
                            String text = option.getText().trim();
                            if (option.isDisplayed() && !text.isEmpty()) {
                                texts.add(text);
                            }
                        }
                        return texts.isEmpty() ? null : texts;
                    });
        } catch (TimeoutException none) {
            return List.of();
        }
    }

    private void clickOption(String text) {
        By option = By.xpath("//div[contains(@class,'select__option')][normalize-space(.)="
                + xpathLiteral(text) + "]");
        new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.elementToBeClickable(option))
                .click();
    }

    /** Text shown in React Select field {@code field} ("" when empty). */
    public String selectedText(String field) {
        List<WebElement> values = driver.findElements(By.cssSelector(
                "[data-testid='" + prefix + "-" + field + "'] .select__single-value"));
        return values.isEmpty() ? "" : values.get(0).getText().trim();
    }

    /**
     * Sets date input {@code #<field>} the way React expects. It uses the
     * native value setter followed by input/change events. Typing is not
     * reliable in these date pickers.
     */
    public void setDate(String field, LocalDate date) {
        String value = date.format(ISO_DATE);
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement input = new WebDriverWait(driver, WAIT)
                    .until(ExpectedConditions.presenceOfElementLocated(By.id(field)));
            if (!input.isEnabled()) {
                log.info("Date {} is locked → kept as [{}].", field, input.getAttribute("value"));
                return;
            }
            ((JavascriptExecutor) driver).executeScript(
                    "const input=arguments[0];"
                            + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input, arguments[1]);"
                            + "input.dispatchEvent(new Event('input',{bubbles:true}));"
                            + "input.dispatchEvent(new Event('change',{bubbles:true}));"
                            + "input.dispatchEvent(new Event('blur',{bubbles:true}));",
                    input, value);
            if (isInputValue(field, value)) {
                log.info("{} = {}", field, value);
                return;
            }
            log.warn("Date {} shows [{}] instead of {} (attempt {}/3).", field, inputValue(field), value, attempt);
        }
        throw new IllegalStateException("Could not set date " + field + " to " + value + ".");
    }

    /** True when the input shows {@code value} and still shows it one render later. */
    private boolean isInputValue(String field, String value) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2))
                    .pollingEvery(Duration.ofMillis(200))
                    .until(d -> value.equals(inputValue(field)));
            try {
                Thread.sleep(400); // one render later it must still be there
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return value.equals(inputValue(field));
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Types {@code text} into text input {@code #<field>}, replacing its value.
     * Does nothing when the input is disabled or read-only.
     *
     * @return true if the text was entered.
     */
    public boolean typeInto(String field, String text) {
        WebElement input = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(By.id(field)));
        if (!input.isEnabled() || input.getAttribute("readonly") != null) {
            log.info("Field {} is not editable → not changed.", field);
            return false;
        }
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
        input.sendKeys(text);
        log.info("{} = {}", field, text);
        return true;
    }

    /** Value of input {@code #<field>}. */
    public String inputValue(String field) {
        List<WebElement> inputs = driver.findElements(By.id(field));
        return inputs.isEmpty() ? "" : inputs.get(0).getAttribute("value");
    }

    /** Clicks one of the given radio buttons ({@code <prefix>-<id>}) at random; returns its id. */
    public String clickRandomRadio(String... ids) {
        return clickRadio(ids[random.nextInt(ids.length)]);
    }

    /** The id among {@code ids} whose radio is checked now, or "" when none is. */
    public String checkedRadio(String... ids) {
        return java.util.Arrays.stream(ids)
                .filter(id -> driver.findElements(byTestId(id)).stream()
                        .anyMatch(e -> "true".equals(e.getAttribute("aria-checked"))))
                .findFirst().orElse("");
    }

    /** Clicks a radio among {@code ids} that is NOT checked now (edit mode: the value must change). */
    public String clickOtherRadio(String... ids) {
        List<String> unchecked = java.util.Arrays.stream(ids)
                .filter(id -> driver.findElements(byTestId(id)).stream()
                        .noneMatch(e -> "true".equals(e.getAttribute("aria-checked"))))
                .toList();
        return clickRadio(unchecked.isEmpty() ? ids[random.nextInt(ids.length)]
                : unchecked.get(random.nextInt(unchecked.size())));
    }

    private String clickRadio(String id) {
        By radio = byTestId(id);
        WebElement element = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(radio));
        if (!element.isEnabled()) {
            log.info("Radio {} is disabled → not changed.", id);
            return "";
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement target = driver.findElement(radio);
            if (attempt == 1) {
                target.click();
            } else {
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .pollingEvery(Duration.ofMillis(150))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> "true".equals(d.findElement(radio).getAttribute("aria-checked"))
                                || "checked".equals(d.findElement(radio).getAttribute("data-state")));
                Object label = ((JavascriptExecutor) driver).executeScript(
                        "const r=arguments[0]; const w=r.closest('div'); return w ? w.innerText.trim() : '';",
                        driver.findElement(radio));
                log.info("{} = {}", id, label);
                return id;
            } catch (TimeoutException e) {
                log.warn("Radio {} is not checked after the click (attempt {}/3).", id, attempt);
            }
        }
        throw new IllegalStateException("Radio " + id + " could not be checked.");
    }

    // =====================================================================
    // "انزال من" + "الرقم" → download lines from another document
    // =====================================================================

    /**
     * Chooses the source screen in "انزال من", picks a random document in the
     * "الرقم" dialog, then downloads its lines into the grid.
     *
     * @param source "عروض الاسعار", "طلبات الشراء", "اوامر الشراء", …;
     *               {@code null} = random.
     * @return the downloaded document's number.
     */
    public String downloadLinesFrom(String source) {

        String chosen = source == null ? selectRandomOption("downloadFrom") : selectOption("downloadFrom", source);
        String number = pickRandomReferenceNumber();
        log.info("Downloading lines from {} {}.", chosen, number);
        clickDownloadLines();
        return number;
    }

    /** Opens the "الرقم" dialog, selects a random row and confirms; returns the chosen number. */
    public String pickRandomReferenceNumber() {

        By openGrid = By.xpath("//*[@data-testid='" + prefix + "-referenceField']/following-sibling::div//button[1]");
        ui.clickWhenReady(openGrid);
        selectRandomRowInOpenDialog();
        confirmDialog();

        return new WebDriverWait(driver, WAIT)
                .until(d -> {
                    String value = d.findElement(byTestId("referenceField")).getAttribute("value");
                    return value.isBlank() ? null : value.trim();
                });
    }

    /** Download button next to "الرقم" (enabled once a number is chosen). */
    public void clickDownloadLines() {
        By download = By.xpath("//*[@data-testid='" + prefix + "-referenceField']"
                + "/following-sibling::div//button[not(@disabled)][.//*[contains(@class,'lucide-download')]]");
        ui.clickWhenReady(download);
        ui.waitForGridToSettle();
    }

    /**
     * Selects (radio) a random row of the grid inside the top-most open dialog.
     *
     * @return the {@code code} column of that row ("" if the grid has none).
     */
    public String selectRandomRowInOpenDialog() {

        // ExtentReport 2026-11-08 17:19 (userCanDuplicatePurchaseReturnInvoiceWithReferenceDocument,
        // PRI2611-000031): StaleElementReferenceException on radio.click() — the dialog's grid was
        // still rendering its rows. The grid is now waited for until its rows stop changing, and
        // every element is looked up again (dialog, row, radio) when it was re-rendered.
        String rowIndex = null;
        for (int attempt = 1; ; attempt++) {
            try {
                WebElement dialog = waitForDialogRowsToSettle();
                if (rowIndex == null || dialog.findElements(dialogRadioOfRow(rowIndex)).isEmpty()) {
                    List<WebElement> rows = dialog.findElements(DIALOG_ROWS);
                    rowIndex = rows.get(random.nextInt(rows.size())).getAttribute("row-index");
                }
                WebElement radio = dialog.findElement(dialogRadioOfRow(rowIndex));
                // QC 2026-11-05 (purchase returns): the first click was sometimes lost (the radio
                // stayed unchecked); later attempts click it into view by script.
                if (attempt == 1) {
                    radio.click();
                } else {
                    ((JavascriptExecutor) driver).executeScript(
                            "arguments[0].scrollIntoView({block:'center'}); arguments[0].click();", radio);
                }
                String checkedRow = rowIndex;
                new WebDriverWait(driver, Duration.ofSeconds(4))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            WebElement r = openDialogWithRadios(d).findElement(dialogRadioOfRow(checkedRow));
                            return "true".equals(r.getAttribute("aria-checked"))
                                    || "checked".equals(r.getAttribute("data-state"));
                        });
                List<WebElement> codes = openDialogWithRadios(driver).findElements(By.cssSelector(
                        ".ag-center-cols-container div[role='row'][row-index='" + rowIndex + "'] [col-id='code']"));
                return codes.isEmpty() ? "" : codes.get(0).getText().trim();
            } catch (StaleElementReferenceException | TimeoutException | NoSuchElementException lost) {
                if (attempt == 3) {
                    throw lost;
                }
                log.warn("Dialog row {} was not selected ({}, attempt {}/3) → again.",
                        rowIndex, lost.getClass().getSimpleName(), attempt);
            }
        }
    }

    private static final By DIALOG_RADIOS = By.cssSelector(".ag-pinned-right-cols-container [role='radio']");
    private static final By DIALOG_ROWS = By.cssSelector(".ag-pinned-right-cols-container div[role='row'][row-index]");

    private static By dialogRadioOfRow(String rowIndex) {
        return By.cssSelector(".ag-pinned-right-cols-container div[role='row'][row-index='" + rowIndex + "'] [role='radio']");
    }

    /** Top-most open dialog that shows radios; null when none yet (for waits). */
    private static WebElement openDialogWithRadios(org.openqa.selenium.SearchContext d) {
        List<WebElement> dialogs = d.findElements(OPEN_DIALOG);
        for (int i = dialogs.size() - 1; i >= 0; i--) {
            if (dialogs.get(i).findElements(DIALOG_RADIOS).stream().anyMatch(WebElement::isDisplayed)) {
                return dialogs.get(i);
            }
        }
        throw new NoSuchElementException("No open dialog with radios.");
    }

    /** Waits for the dialog, then until its row count is the same on two reads 500 ms apart. */
    private WebElement waitForDialogRowsToSettle() {
        int[] last = {-1};
        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(500))
                .ignoring(StaleElementReferenceException.class)
                .ignoring(NoSuchElementException.class)
                .until(d -> {
                    WebElement dialog = openDialogWithRadios(d);
                    int rows = dialog.findElements(DIALOG_ROWS).size();
                    boolean settled = rows > 0 && rows == last[0];
                    last[0] = rows;
                    return settled ? dialog : null;
                });
    }

    // =====================================================================
    // Lines grid
    // =====================================================================

    /** Waits until the number of lines stops changing (lines downloaded / copied arrive late). */
    public void waitForGridToSettle() {
        ui.waitForGridToSettle();
    }

    /** Number of lines in the visible lines grid (scrolls it to render the last line). */
    public int lineCount() {
        // aria-rowcount of the grid = all lines (rendered or not) + the header
        // row. QC, 2026-11-01: the old way scrolled to the bottom and read the
        // highest rendered row-index straight away. With 38 lines, that read
        // came before the grid re-rendered and gave 32, so "اضافة سطر" was
        // judged against the wrong count and an existing line was overwritten.
        Number count = (Number) ((JavascriptExecutor) driver).executeScript(
                "const cc=[...document.querySelectorAll('.ag-center-cols-container')].find(c=>c.offsetParent!==null);"
                        + "if(!cc) return 0;"
                        + "const root=cc.closest('[aria-rowcount]');"
                        + "if(root) return Math.max(0, Number(root.getAttribute('aria-rowcount')) - 1);"
                        + "return cc.querySelectorAll('div[role=row][row-index]').length;");
        return count == null ? 0 : count.intValue();
    }

    /**
     * "اضافة سطر" → a new, empty line at the end.
     *
     * <p>The button does nothing while an existing line has price 0
     * (same as on orders, QC 2026-10-27). Callers fill those prices first.</p>
     *
     * @return row index of the new line.
     */
    public int addLine() {

        int before = lineCount();
        // QC 2026-11-09 (free quantity processing): after line 1 is filled the grid adds an empty
        // line 2 by itself, and "اضافة سطر" then adds nothing while that line is empty. The line is
        // already there, it only has to be completed, so it is used instead of clicking.
        if (before > 0 && isEmptyLine(before - 1)) {
            log.info("Line {} is already there, empty (added by the grid) → filling it instead of \"اضافة سطر\".", before);
            return before - 1;
        }
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                ui.clickWhenReady(ADD_LINE_BUTTON);
            } catch (TimeoutException notClickable) {
                // QC 2026-11-02 (quotation with 9 downloaded lines): the button was
                // enabled but outside the visible area ("on top=nothing"), so the
                // normal click never became possible. It is clicked by script then.
                String state = addLineButtonState();
                Object clicked = ((JavascriptExecutor) driver).executeScript(
                        "const b=[...document.querySelectorAll('button')].find(x=>x.innerText.trim()==='اضافة سطر');"
                                + "if(!b || b.disabled) return false;"
                                + "b.scrollIntoView({block:'center', inline:'center'}); b.click(); return true;");
                if (!Boolean.TRUE.equals(clicked)) {
                    throw new IllegalStateException("\"اضافة سطر\" could not be clicked for 10 s: "
                            + state + " | lines: " + describeLines(before), notClickable);
                }
                log.warn("\"اضافة سطر\" was not clickable ({}) → clicked by script.", state);
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .pollingEvery(Duration.ofMillis(250))
                        .until(d -> lineCount() > before);
                return newEmptyLine(before);
            } catch (TimeoutException e) {
                if (before > 0 && isEmptyLine(before - 1)) { // the empty line appeared late
                    log.info("Line {} is empty (added by the grid) → filling it.", before);
                    return before - 1;
                }
                log.warn("\"اضافة سطر\" did not add a line (attempt {}/3, lines {}).", attempt, before);
            }
        }
        throw new IllegalStateException("\"اضافة سطر\" did not add a line after 3 clicks (lines stayed "
                + before + "). A line with price 0 blocks it.");
    }

    /** Why "اضافة سطر" cannot be clicked: missing, hidden, disabled, or covered (and by what). */
    private String addLineButtonState() {
        Object state = ((JavascriptExecutor) driver).executeScript(
                "const b=[...document.querySelectorAll('button')].find(x=>x.innerText.trim()==='اضافة سطر');"
                        + "if(!b) return 'button not on the page';"
                        + "b.scrollIntoView({block:'center'});"
                        + "const r=b.getBoundingClientRect();"
                        + "const top=document.elementFromPoint(r.x+r.width/2, r.y+r.height/2);"
                        + "return 'disabled=' + b.disabled + ', aria-disabled=' + b.getAttribute('aria-disabled')"
                        + " + ', size=' + Math.round(r.width) + 'x' + Math.round(r.height)"
                        + " + ', title=[' + (b.title||'') + ']'"
                        + " + ', on top=' + (top===b||b.contains(top) ? 'the button' : (top ? top.outerHTML.slice(0,150) : 'nothing'));");
        return String.valueOf(state);
    }

    /** "code @ price × quantity" of the first {@code count} lines, for failure messages. */
    private String describeLines(int count) {
        StringBuilder text = new StringBuilder();
        for (int row = 0; row < count; row++) {
            try {
                text.append(row + 1).append(") ").append(lineText(row, "code"))
                        .append(" q=").append(lineText(row, "quantity"))
                        .append(" p=").append(lineText(row, "price"))
                        .append(" wh=").append(lineText(row, "warehouseId")).append("; ");
            } catch (RuntimeException e) {
                text.append(row + 1).append(") ?; ");
            }
        }
        return text.toString();
    }

    /**
     * Index of the new (empty) line after "اضافة سطر": normally {@code from},
     * the first index after the old lines. It checks that this line really
     * has no item, so an existing line is never overwritten.
     */
    private int newEmptyLine(int from) {
        int count = lineCount();
        for (int row = from; row < count; row++) {
            if (lineText(row, "itemId").isEmpty() && lineText(row, "code").isEmpty()) {
                return row;
            }
        }
        throw new IllegalStateException("\"اضافة سطر\" added a line, but no empty line was found from line "
                + (from + 1) + " to " + count + ".");
    }

    /**
     * Picks a random item in the item cell of line {@code row}.
     *
     * @return the chosen item name.
     */
    public String selectRandomItem(int row) {
        return selectItem(row, names -> names.get(random.nextInt(names.size())));
    }

    /**
     * Picks item {@code name} in the item cell of line {@code row} (typed in the
     * search box, so it is found even when the list shows only the first 100).
     */
    public String selectItem(int row, String name) {
        return selectItem(row, name, names -> {
            if (!names.contains(name)) {
                throw new NoSuchElementException("Item [" + name + "] is not offered on line " + (row + 1) + ": " + names);
            }
            return name;
        });
    }

    private String selectItem(int row, Function<List<String>, String> chooser) {
        return selectItem(row, null, chooser);
    }

    private String selectItem(int row, String typed, Function<List<String>, String> chooser) {

        By cell = lineCell(row, "itemId");
        By input = By.cssSelector(".ag-popup-editor input.select__input, .ag-cell-inline-editing input.select__input");
        WebElement editor = openCellEditor("item (line " + (row + 1) + ")", cell,
                ExpectedConditions.visibilityOfElementLocated(input));
        // The editor can re-render right after it opens (stale input in the
        // invoice edit mode, QC 2026-11-05), so it is found again and retried.
        String listbox = null;
        for (int attempt = 1; ; attempt++) {
            try {
                editor.click();
                listbox = editor.getAttribute("aria-controls");
                break;
            } catch (StaleElementReferenceException rerendered) {
                if (attempt == 3) {
                    throw rerendered;
                }
                log.warn("Item editor of line {} re-rendered (attempt {}/3) → finding it again.", row + 1, attempt);
                List<WebElement> open = driver.findElements(input).stream()
                        .filter(WebElement::isDisplayed).toList();
                editor = open.isEmpty()
                        ? openCellEditor("item (line " + (row + 1) + ")", cell,
                                ExpectedConditions.visibilityOfElementLocated(input))
                        : open.get(0);
            }
        }
        By options = listbox == null || listbox.isBlank()
                ? SELECT_OPTION
                : By.cssSelector("#" + listbox + " div[class*='select__option']");
        if (typed != null) {
            editor.sendKeys(typed);
        }

        // The option list re-renders while it loads (stale options seen in
        // QC, 2026-11-01), so options are read by text and clicked fresh.
        List<String> names = new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    List<String> shown = d.findElements(options).stream()
                            .filter(WebElement::isDisplayed)
                            .map(o -> o.getText().trim())
                            .filter(t -> !t.isEmpty()).toList();
                    return shown.isEmpty() ? null : shown;
                });
        String item = chooser.apply(names);
        By chosen = By.xpath((listbox == null || listbox.isBlank() ? "" : "//*[@id='" + listbox + "']")
                + "//div[contains(@class,'select__option')][normalize-space(.)=" + xpathLiteral(item) + "]");
        new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    ((JavascriptExecutor) d).executeScript("arguments[0].click();", d.findElement(chosen));
                    return true;
                });
        log.info("Line {} item = {}", row + 1, item);
        waitForItemLoaded(row);
        return item;
    }

    /**
     * After an item is chosen, the app loads its data into the line: code,
     * unit, warehouse and price. It re-renders the row when the data arrives,
     * and that discards a value typed in the meantime. QC, 2026-11-01: a
     * quantity typed right after choosing the item was lost every time. So
     * wait until the unit is shown.
     *
     * <p>Does not fail: an item the document does not accept never loads.
     * The caller sees that on the next step and handles it.</p>
     */
    private void waitForItemLoaded(int row) {
        try {
            new WebDriverWait(driver, WAIT)
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> !lineText(row, "uomId").isEmpty() && !lineText(row, "code").isEmpty());
            ui.waitForGridColumnsToRender();
        } catch (TimeoutException e) {
            log.warn("Line {}: item data (unit/code) did not load within {} s.", row + 1, WAIT.toSeconds());
        }
    }

    /**
     * Types {@code value} into numeric cell {@code colId} of line {@code row}
     * and checks that the grid shows it afterwards.
     *
     * <p>Why not {@code DocumentForm.setGridNumber}: committing a cell with
     * Enter makes the grid open the editor of the NEXT cell by itself. For
     * example, quantity → Enter opens the price editor (QC, 2026-11-01, on
     * requisitions). {@code setGridNumber} then clicked a cell that was
     * already editing, and "100" ended up in the line's note while the price
     * stayed 0. Here, a cell that is already editing is typed into directly.</p>
     */
    public void setLineNumber(int row, String colId, String value) {
        BigDecimal expected = new BigDecimal(value);
        setLineValue(row, colId, value, () -> expected.compareTo(lineNumber(row, colId)) == 0);
    }

    /** Types {@code text} into text cell {@code colId} (e.g. "note") of line {@code row}; checks it is shown. */
    public void setLineText(int row, String colId, String text) {
        setLineValue(row, colId, text, () -> lineText(row, colId).equals(text.trim()));
    }

    private void setLineValue(int row, String colId, String value, java.util.function.BooleanSupplier committed) {

        ensureLineRendered(row);
        By cell = lineCell(row, colId);
        By editor = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "'].ag-cell-inline-editing :is(input, textarea)");
        for (int attempt = 1; attempt <= 3; attempt++) {
            closeOtherEditorsAndDialogs(editor);
            WebElement input;
            try {
                Optional<WebElement> open = visible(editor);
                input = open.isPresent()
                        ? open.get()
                        : openCellEditor(colId + " (line " + (row + 1) + ")", cell,
                                ExpectedConditions.visibilityOfElementLocated(editor));
            } catch (TimeoutException notOpened) {
                log.warn("Editor of {} on line {} did not open (attempt {}/3).", colId, row + 1, attempt);
                stopCellEditing();
                continue;
            }
            try {
                // The value is written by script (native setter + input event),
                // the same as on orders. Typed keys went to whatever had the
                // focus: QC, 2026-11-01, "a17" ended up in the item-code cell
                // and opened the "البحث عن صنف" dialog. Only Enter is sent as a
                // key, to the editor itself, to commit.
                ((JavascriptExecutor) driver).executeScript(
                        "const i=arguments[0];"
                                + "const set=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(i),'value').set;"
                                + "i.focus(); set.call(i, arguments[1]);"
                                + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                                + "i.dispatchEvent(new Event('change',{bubbles:true}));",
                        input, value);
                input.sendKeys(Keys.ENTER);
            } catch (StaleElementReferenceException rerendered) {
                log.warn("Editor of {} re-rendered while typing (attempt {}/3).", colId, attempt);
                continue;
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .pollingEvery(Duration.ofMillis(150))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> !d.findElement(cell).getAttribute("class").contains("ag-cell-inline-editing")
                                && committed.getAsBoolean());
                log.info("Line {} {} = {}", row + 1, colId, value);
                return;
            } catch (TimeoutException e) {
                log.warn("Line {} {} shows [{}] instead of {} (attempt {}/3).",
                        row + 1, colId, lineText(row, colId), value, attempt);
            }
        }
        throw new IllegalStateException("Could not set " + colId + " of line " + (row + 1) + " to " + value
                + "; it shows [" + lineText(row, colId) + "].");
    }

    /**
     * Before editing a cell: closes a dialog left open by a previous step
     * (for example "البحث عن صنف") and an editor that is open on ANOTHER cell.
     * Typing would otherwise go there.
     */
    private void closeOtherEditorsAndDialogs(By targetEditor) {

        if (isDialogOpen()) {
            log.warn("Dialog [{}] is open while editing a line → closing it.", openDialogTitle());
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> !isDialogOpen());
            } catch (TimeoutException stillOpen) {
                log.warn("Dialog [{}] did not close with Escape.", openDialogTitle());
            }
        }
        boolean otherEditorOpen = visible(By.cssSelector(".ag-cell-inline-editing, .ag-popup-editor")).isPresent()
                && visible(targetEditor).isEmpty();
        if (otherEditorOpen) {
            stopCellEditing();
        }
    }

    /**
     * Chooses a random option in an AG Grid "rich select" cell of line
     * {@code row}, for example the unit ("uomId") or the line's warehouse
     * ("warehouseId").
     *
     * <p>Done with the keyboard: F2 opens the list, ↓ moves a random number of
     * times, and Enter selects. Checked by hand on QC, 2026-11-01: a mouse
     * click on an option from script left the cell EMPTY.</p>
     *
     * @return the text the cell shows afterwards.
     */
    public String setLineOption(int row, String colId) {

        ensureLineRendered(row);
        By editor = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "'].ag-cell-inline-editing [aria-controls^='ag-rich-select-list']");
        for (int attempt = 1; attempt <= 3; attempt++) {
            closeOtherEditorsAndDialogs(editor);
            WebElement wrapper;
            try {
                wrapper = openCellEditor(colId + " (line " + (row + 1) + ")", lineCell(row, colId),
                        ExpectedConditions.visibilityOfElementLocated(editor));
            } catch (TimeoutException notOpened) {
                log.warn("List of {} on line {} did not open (attempt {}/3).", colId, row + 1, attempt);
                continue;
            }
            Number options = (Number) ((JavascriptExecutor) driver).executeScript(
                    "const l=document.getElementById(arguments[0].getAttribute('aria-controls'));"
                            + "return l ? l.querySelectorAll('.ag-rich-select-row').length : 0;", wrapper);
            int count = options == null ? 0 : options.intValue();
            if (count == 0) {
                log.info("Line {} {} has no options → not changed.", row + 1, colId);
                stopCellEditing();
                return lineText(row, colId);
            }
            int steps = 1 + random.nextInt(count);
            Actions keys = new Actions(driver);
            for (int i = 0; i < steps; i++) {
                keys.sendKeys(Keys.ARROW_DOWN);
            }
            keys.sendKeys(Keys.ENTER).perform();
            stopCellEditing(); // Enter moves the editor to the next cell
            String shown = lineText(row, colId);
            if (!shown.isEmpty()) {
                log.info("Line {} {} = {}", row + 1, colId, shown);
                return shown;
            }
            log.warn("Line {} {} is empty after choosing an option (attempt {}/3).", row + 1, colId, attempt);
        }
        throw new IllegalStateException("Could not choose an option in " + colId + " of line " + (row + 1) + ".");
    }

    /** Numeric value of cell {@code colId} in line {@code row} (0 when empty). */
    public BigDecimal lineNumber(int row, String colId) {
        String text = lineText(row, colId).replace(",", "").trim();
        return text.isEmpty() || !text.matches("-?\\d+(\\.\\d+)?") ? BigDecimal.ZERO : new BigDecimal(text);
    }

    /**
     * Closes a cell editor the grid opened by itself after the last Enter
     * (see {@link #setLineNumber}). Escape cancels only that untouched cell;
     * the values already committed stay.
     */
    public void stopCellEditing() {
        if (visible(By.cssSelector(".ag-cell-inline-editing, .ag-popup-editor")).isPresent()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            ui.waitForNoCellEditor(Duration.ofSeconds(2));
        }
    }

    /**
     * Deletes line {@code row} with its trash button (pinned "إجراءات" column).
     * The line goes at once, without a confirmation (QC, 2026-11-01).
     */
    public void deleteLine(int row) {

        int before = lineCount();
        // The button is found and clicked inside one script: scrolling makes
        // AG Grid re-render the row, and a WebElement found before it went
        // stale (QC, 2026-11-01, Excel import of 39 lines).
        String clickTrash =
                "const sel=['.ag-pinned-left-cols-container','.ag-pinned-right-cols-container']"
                        + ".map(c=>c+' div[role=row][row-index=\"'+arguments[0]+'\"] [col-id=actions] button').join(',');"
                        + "const b=[...document.querySelectorAll(sel)].find(x=>x.offsetParent!==null);"
                        + "if(!b) return false; b.scrollIntoView({block:'center'}); b.click(); return true;";

        // Identity of the line, to tell whether the click removed it. QC,
        // 2026-11-01: the line count alone was not reliable here. The grid
        // adds an empty line by itself after an Enter on the last line, so
        // the count did not drop. The test then clicked again and removed
        // OTHER lines.
        String identity = lineText(row, "code") + "|" + lineText(row, "itemId");

        for (int attempt = 1; attempt <= 3; attempt++) {
            ensureLineRendered(row);
            boolean clicked = Boolean.TRUE.equals(((JavascriptExecutor) driver).executeScript(clickTrash, row));
            if (!clicked) {
                log.warn("Trash button of line {} not found (attempt {}/3).", row + 1, attempt);
                continue;
            }
            confirmDialogIfShown(Duration.ofMillis(800));
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .pollingEvery(Duration.ofMillis(250))
                        .until(d -> !(lineText(row, "code") + "|" + lineText(row, "itemId")).equals(identity)
                                || lineCount() < before);
                log.info("Line {} deleted.", row + 1);
                return;
            } catch (TimeoutException stillThere) {
                log.warn("Line {} still shows [{}] after the trash click (attempt {}/3).", row + 1, identity, attempt);
            }
        }
        throw new IllegalStateException("Could not delete line " + (row + 1) + ".");
    }

    /**
     * Removes lines that have no item. Enter on the last cell of the last line
     * makes the grid add a new, empty line by itself (QC, 2026-11-01), and an
     * empty line must not be sent with the save.
     *
     * @return number of lines removed.
     */
    /** True when line {@code row} has no item yet (an empty line, e.g. added by the grid). */
    public boolean isEmptyLine(int row) {
        return lineText(row, "itemId").isEmpty() && lineText(row, "code").isEmpty();
    }

    public int deleteEmptyLines() {
        int removed = 0;
        for (int row = lineCount() - 1; row >= 0; row--) {
            if (lineText(row, "itemId").isEmpty() && lineText(row, "code").isEmpty()) {
                log.info("Line {} has no item (added by the grid) → removing it.", row + 1);
                deleteLine(row);
                removed++;
            }
        }
        return removed;
    }

    /**
     * Starts recording the body of the next save request
     * ({@code POST/PUT …/api/<resource>}) so a refused save can be reported
     * with the lines the page really sent (item, quantity, price).
     */
    public void startRecordingSaveRequest() {
        ((JavascriptExecutor) driver).executeScript(
                "window.__qaSaveBodies = [];"
                        + "if (!window.__qaSaveHooked) {"
                        + "  window.__qaSaveHooked = true;"
                        + "  const oo = XMLHttpRequest.prototype.open, os = XMLHttpRequest.prototype.send;"
                        + "  XMLHttpRequest.prototype.open = function(m, u) { this.__qaSm = m; this.__qaSu = u; return oo.apply(this, arguments); };"
                        + "  XMLHttpRequest.prototype.send = function(b) {"
                        + "    if ((this.__qaSm === 'POST' || this.__qaSm === 'PUT') && typeof b === 'string'"
                        + "        && !/filter/i.test(this.__qaSu)) { window.__qaSaveBodies.push(b); }"
                        + "    return os.apply(this, arguments);"
                        + "  };"
                        + "}");
    }

    /**
     * The lines of the last recorded save request, one "item × quantity @ price"
     * per line ("" when nothing was recorded).
     */
    public String lastSavedLines() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const b = (window.__qaSaveBodies || []).slice(-1)[0];"
                        + "if (!b) return '';"
                        + "try { const j = JSON.parse(b);"
                        + "  const lines = Object.values(j).find(v => Array.isArray(v) && v.length && v[0] && 'itemId' in v[0]) || [];"
                        + "  return lines.map((l, i) => (i + 1) + ') ' + (l.itemName || ('itemId ' + l.itemId))"
                        + "    + ' × ' + l.quantity + ' @ ' + l.price).join(' | ');"
                        + "} catch (e) { return b.slice(0, 500); }");
        return text == null ? "" : text.toString();
    }

    /** Text of column {@code colId} in line {@code row}. */
    public String lineText(int row, String colId) {
        ensureLineRendered(row);
        List<WebElement> cells = driver.findElements(lineCell(row, colId));
        return cells.isEmpty() ? "" : cells.get(0).getText().trim();
    }

    /**
     * AG Grid renders only the lines in view. QC, 2026-11-01: after an Excel
     * import of 39 lines, the lines out of view read as empty and were taken
     * as repeated items. This scrolls the grid's own viewport to line
     * {@code row} when it is not rendered, and waits (max 3 s) until it is.
     */
    public void ensureLineRendered(int row) {
        String script =
                "const cc=[...document.querySelectorAll('.ag-center-cols-container')].find(c=>c.offsetParent!==null);"
                        + "if(!cc) return true;"
                        + "if(cc.querySelector('div[role=row][row-index=\"'+arguments[0]+'\"]')) return true;"
                        + "const vp=cc.closest('.ag-root').querySelector('.ag-body-viewport');"
                        + "const any=cc.querySelector('div[role=row][row-index]');"
                        + "const h=any ? any.offsetHeight : 40;"
                        + "if(vp) vp.scrollTop=Math.max(0, arguments[0]*h - vp.clientHeight/2);"
                        + "return false;";
        JavascriptExecutor js = (JavascriptExecutor) driver;
        if (Boolean.TRUE.equals(js.executeScript(script, row))) {
            return;
        }
        try {
            new WebDriverWait(driver, Duration.ofSeconds(3))
                    .pollingEvery(Duration.ofMillis(100))
                    .until(d -> Boolean.TRUE.equals(js.executeScript(script, row)));
            ui.waitForGridColumnsToRender();
        } catch (TimeoutException e) {
            log.warn("Line {} did not render after scrolling the grid.", row + 1);
        }
    }

    /** Texts of column {@code colId} for every rendered line, in row order. */
    public List<String> lineTexts(String colId) {
        return ui.getGridColumnTexts(colId);
    }

    private By lineCell(int row, String colId) {
        return By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] div[role='gridcell'][col-id='" + colId + "']");
    }

    /**
     * Opens the editor of an AG Grid cell: focus + F2 (keyboard, not covered
     * by the sticky bar), then a double-click, then focus + F2 again. The
     * first gesture is sometimes swallowed while the grid re-renders. Checked
     * by hand on QC, 2026-11-01.
     */
    protected <T> T openCellEditor(String name, By cell, Function<WebDriver, T> opened) {

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement element = new WebDriverWait(driver, WAIT)
                        .until(ExpectedConditions.presenceOfElementLocated(cell));
                bringCellIntoView(element);
                element = driver.findElement(cell);
                switch (attempt) {
                    case 2 -> new Actions(driver).doubleClick(element).perform();
                    // Keyboard, without a pointer click: the last lines can sit
                    // under the sticky bar at the bottom of the form, which takes
                    // the click (QC, 2026-11-01, line 4: "click intercepted").
                    // F2 only: in these grids Enter on a focused cell moves to
                    // the NEXT cell and edits that one (checked by hand).
                    default -> focusCellAndPress(element, Keys.F2);
                }
            } catch (StaleElementReferenceException | ElementClickInterceptedException e) {
                log.warn("Cell of {} re-rendered or covered (attempt {}/3): {}.",
                        name, attempt, e.getClass().getSimpleName());
                continue;
            }
            try {
                return new WebDriverWait(driver, Duration.ofSeconds(4))
                        .pollingEvery(Duration.ofMillis(250))
                        .ignoring(StaleElementReferenceException.class)
                        .until(opened);
            } catch (TimeoutException e) {
                log.warn("Editor of {} did not open (attempt {}/3).", name, attempt);
            }
        }
        throw new TimeoutException("Editor of " + name + " did not open after 3 attempts.");
    }

    /**
     * Scrolls the page so the cell is in the upper part of the window, above
     * the sticky bar at the bottom of the form. Then scrolls the grid
     * horizontally to the cell.
     */
    private void bringCellIntoView(WebElement cell) {
        ((JavascriptExecutor) driver).executeScript(
                "const c=arguments[0];"
                        + "c.scrollIntoView({block:'center', inline:'center'});"
                        + "const r=c.getBoundingClientRect();"
                        + "if (r.bottom > window.innerHeight * 0.6) window.scrollBy(0, r.bottom - window.innerHeight * 0.4);",
                cell);
        ui.waitForGridColumnsToRender();
    }

    /** Focuses an AG Grid cell from script and presses {@code key} (F2 starts editing that cell). */
    private void focusCellAndPress(WebElement cell, Keys key) {
        ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", cell);
        new Actions(driver).sendKeys(key).perform();
    }

    // =====================================================================
    // "⋮" menu
    // =====================================================================

    /**
     * Opens the "⋮" menu and waits until it is really open.
     * The first click can be lost while the toolbar re-renders
     * (QC, 2026-10-29), so it is retried up to 3 times.
     */
    public void openFormMenu() {
        for (int attempt = 1; attempt <= 3; attempt++) {
            clickAction("clipboardBar");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.visibilityOfElementLocated(OPEN_MENU));
                return;
            } catch (TimeoutException e) {
                log.warn("\"⋮\" menu did not open (attempt {}/3).", attempt);
            }
        }
        throw new TimeoutException("The \"⋮\" menu did not open after 3 clicks.");
    }

    private By menuItem(String label) {
        return By.xpath("//*[@role='menu' and @data-state='open']//button[normalize-space()=" + xpathLiteral(label) + "]");
    }

    /**
     * "⋮" → {@code label}. The item is found by its text with spaces and
     * direction marks ignored. QC 2026-11-01 (PR2608-000024, Approved): the
     * menu was open and showed "التصدير الى Excel", yet the exact-text XPath
     * never became clickable. If no item matches, the labels shown are in
     * the error.
     */
    public void clickMenuItem(String label) {
        openFormMenu();
        String wanted = comparable(label);
        By items = By.cssSelector("[role='menu'][data-state='open'] [role='menuitem']");
        WebElement item;
        try {
            item = new WebDriverWait(driver, WAIT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(items).stream()
                            .filter(e -> comparable(e.getText()).equals(wanted))
                            .findFirst().orElse(null));
        } catch (TimeoutException notFound) {
            throw new NoSuchElementException("No item [" + label + "] in the ⋮ menu. Items shown: "
                    + driver.findElements(items).stream().map(WebElement::getText).toList());
        }
        WebElement target = item.findElements(By.cssSelector("button")).stream().findFirst().orElse(item);
        try {
            target.click();
        } catch (RuntimeException notClickable) {
            log.warn("Menu item [{}] could not be clicked ({}) → clicked by script.", label,
                    notClickable.getClass().getSimpleName());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", target);
        }
        log.info("⋮ → {}", label);
    }

    /** Text without spaces and bidi/zero-width marks, for comparing labels. */
    private static String comparable(String text) {
        return text == null ? "" : text.replaceAll("[\\s\\u200B-\\u200F\\u061C\\u202A-\\u202E\\u2066-\\u2069]", "");
    }

    /** Whether "⋮" → {@code label} is enabled (the menu is closed again afterwards). */
    public boolean isMenuItemEnabled(String label) {
        openFormMenu();
        boolean enabled = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(menuItem(label)))
                .isEnabled();
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        return enabled;
    }

    /** Labels of the "⋮" menu items (the menu is closed again afterwards). */
    public List<String> menuItems() {
        openFormMenu();
        // Items are read from [role=menuitem]: on an Approved requisition they
        // have no inner <button> (QC 2026-11-01).
        List<String> labels = driver.findElements(By.cssSelector("[role='menu'][data-state='open'] [role='menuitem']")).stream()
                .map(b -> b.getText().trim()).filter(t -> !t.isEmpty()).distinct().toList();
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        return labels;
    }

    /**
     * "⋮" → "الاستيراد من Excel" → upload {@code file} → confirm, then waits
     * (up to 2 min) until the import dialog closes.
     */
    public void importLinesFromExcel(Path file) {

        if (!Files.isRegularFile(file)) {
            throw new IllegalArgumentException("Excel test file not found: " + file.toAbsolutePath());
        }
        clickMenuItem("الاستيراد من Excel");
        new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(EXCEL_FILE_INPUT))
                .sendKeys(file.toAbsolutePath().normalize().toString());
        ui.clickWhenReady(DIALOG_CONFIRM);

        By importDialog = By.xpath("//*[@role='dialog' and @data-state='open']"
                + "[.//*[normalize-space()='الاستيراد من Excel']]");
        new WebDriverWait(driver, Duration.ofSeconds(120))
                .pollingEvery(Duration.ofMillis(500))
                .until(d -> d.findElements(importDialog).isEmpty());
        ui.waitForGridToSettle();
    }

    // =====================================================================
    // Helpers
    // =====================================================================

    protected static String xpathLiteral(String value) {
        if (!value.contains("'")) {
            return "'" + value + "'";
        }
        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }
        return "concat('" + String.join("', \"'\", '", value.split("'", -1)) + "')";
    }

    // =====================================================================
    // Print preview ("طباعة" → template → "طباعة")
    // =====================================================================

    private static final By PRINT_TEMPLATES =
            By.cssSelector("[role='menu'][data-state='open'] button[role='radio']");
    private static final By PRINT_PREVIEW = By.cssSelector(".pd-overlay");
    private static final By PREVIEW_PDF_BUTTON = By.cssSelector("button.pd-btn.pd-btn-pdf");
    private static final By PREVIEW_PRINT_BUTTON = By.cssSelector("button.pd-btn.pd-btn-print");
    private static final By PREVIEW_CLOSE_BUTTON = By.cssSelector("button.pd-btn.pd-btn-close");

    /**
     * "طباعة" → a random template of the menu → "طباعة" (printConfirm), then
     * waits for the preview and returns its text. On requisitions the
     * templates are radio buttons inside the print menu (QC 2026-11-01: one
     * template, "الافتراضي", testid requisitions-default).
     */
    public String openPrintPreviewWithRandomTemplate() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            clickAction("print");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.visibilityOfElementLocated(PRINT_TEMPLATES));
                break;
            } catch (TimeoutException e) {
                log.warn("Print templates did not appear (attempt {}/2).", attempt);
                if (attempt == 2) {
                    throw e;
                }
            }
        }
        ui.waitForAnimationsToFinish(OPEN_MENU);
        List<WebElement> templates = driver.findElements(PRINT_TEMPLATES);
        WebElement template = templates.get(random.nextInt(templates.size()));
        String name = template.getAttribute("data-testid");
        if (!"true".equals(template.getAttribute("aria-checked"))) {
            template.click();
        }
        log.info("Print template: {} (of {})", name, templates.size());
        clickAction("printConfirm");

        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(PRINT_PREVIEW));
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .ignoring(StaleElementReferenceException.class)
                // "الرقم المرجعي" on orders / invoices; free quantity processing shows "الرمز" (QC 2026-11-05).
                .until(d -> {
                    String text = d.findElement(PRINT_PREVIEW).getText();
                    return text.contains("الرقم المرجعي") || (!code().isBlank() && text.contains(code()));
                });
        return driver.findElement(PRINT_PREVIEW).getText();
    }

    /** "حفظ كـ PDF" in the print preview. */
    public void savePreviewAsPdf() {
        // QC 2026-11-01 (PR2609B7-000093): the preview toolbar showed only
        // "طباعة"; "حفظ كـ PDF" was in the page but not displayed, so a normal
        // click timed out. It is clicked by script when it is hidden.
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(PREVIEW_PDF_BUTTON));
        if (button.isDisplayed() && button.isEnabled()) {
            button.click();
        } else {
            log.warn("\"حفظ كـ PDF\" is not displayed in the preview (displayed={}, enabled={}) → clicked by script.",
                    button.isDisplayed(), button.isEnabled());
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", button);
        }
    }

    /** "طباعة" in the print preview (opens Chrome's own print dialog). */
    public void printFromPreview() {
        ui.clickWhenReady(PREVIEW_PRINT_BUTTON);
    }

    /**
     * Chrome's own print dialog (opened by {@link #printFromPreview()}) is
     * outside the page, so Selenium cannot see it. As on orders, it waits for
     * the dialog to load and presses Enter, which clicks its "Print" button
     * and sends the page to the printer chosen there (QC machine's default
     * printer, or "Save as PDF" if that is the default).
     * Needs a visible (not headless) browser and the focus on its window.
     */
    public void confirmChromePrintDialog() throws java.awt.AWTException {
        java.awt.Robot robot = new java.awt.Robot();
        robot.delay(3000); // Chrome print preview loads
        robot.keyPress(java.awt.event.KeyEvent.VK_ENTER);
        robot.keyRelease(java.awt.event.KeyEvent.VK_ENTER);
        robot.delay(2000); // dialog closes and the job is sent
        log.info("Chrome print dialog confirmed (Enter).");
    }

    /** Closes the print preview with its ✕. */
    public void closePrintPreview() {
        if (driver.findElements(PRINT_PREVIEW).stream().noneMatch(WebElement::isDisplayed)) {
            return; // already closed (for example after printing)
        }
        ui.clickWhenReady(PREVIEW_CLOSE_BUTTON);
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.invisibilityOfElementLocated(PRINT_PREVIEW));
    }

    // =====================================================================
    // Downloads
    // =====================================================================

    public Path prepareDownloadFolder(String name) {
        return downloads.prepareDownloadFolder(name);
    }

    public Path waitForDownloadedFile(Path dir, String extension, Duration timeout) {
        return downloads.waitForDownloadedFile(dir, extension, timeout);
    }

    // =====================================================================
    // "⋮" → "ارسال للمورد عن طريق الايميل"
    // =====================================================================

    public static final String SEND_TO_SUPPLIER_MENU_ITEM = "ارسال للمورد عن طريق الايميل";
    private static final By SEND_DIALOG = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[starts-with(normalize-space(),'إرسال')]]");

    /** What the "إرسال ..." dialog shows before anything is sent. */
    public record SendToSupplierDialog(String title, List<String> recipients, String notes, String fileTypes) {
    }

    /**
     * "⋮" → "ارسال للمورد عن طريق الايميل". Opens the "إرسال طلب الشراء"
     * dialog (QC 2026-11-01): الى = the document's supplier, البيان = a text
     * with the document number, and a PDF attachment field. Nothing is sent.
     */
    public SendToSupplierDialog openSendToSupplierDialog() {
        clickMenuItem(SEND_TO_SUPPLIER_MENU_ITEM);
        WebElement dialog = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(SEND_DIALOG));
        // No "data-modal-agree" button in this dialog (ارسال / إلغاء are plain
        // buttons), so the generic dialog wait cannot be used: wait for its
        // pre-filled note instead (QC 2026-11-01, PR2611B25-000003).
        new WebDriverWait(driver, WAIT).until(d -> {
            List<WebElement> areas = d.findElement(SEND_DIALOG).findElements(By.cssSelector("textarea"));
            return !areas.isEmpty() && !areas.get(0).getAttribute("value").isBlank();
        });
        dialog = driver.findElement(SEND_DIALOG);
        List<String> recipients = dialog.findElements(By.cssSelector("[class*='multi-value__label'], [class*='single-value']"))
                .stream().map(e -> e.getText().trim()).filter(t -> !t.isEmpty()).distinct().toList();
        List<WebElement> notes = dialog.findElements(By.cssSelector("textarea"));
        List<WebElement> file = dialog.findElements(By.cssSelector("input[type='file']"));
        SendToSupplierDialog shown = new SendToSupplierDialog(
                dialog.findElement(By.cssSelector("h2")).getText().trim(),
                recipients,
                notes.isEmpty() ? "" : notes.get(0).getAttribute("value"),
                file.isEmpty() ? "" : file.get(0).getAttribute("accept"));
        log.info("Send dialog: {}", shown);
        return shown;
    }

    /** "إلغاء" of the send dialog; nothing is sent. */
    public void cancelSendToSupplierDialog() {
        WebElement dialog = driver.findElement(SEND_DIALOG);
        dialog.findElement(By.xpath(".//button[normalize-space()='إلغاء']")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.invisibilityOfElementLocated(SEND_DIALOG));
    }

    /** Adds {@code line} as a new line at the end of "البيان" of the send dialog; returns the whole note. */
    public String appendSendNote(String line) {
        WebElement area = driver.findElement(SEND_DIALOG).findElement(By.cssSelector("textarea"));
        area.click();
        area.sendKeys(Keys.chord(Keys.CONTROL, Keys.END));
        area.sendKeys(Keys.chord(Keys.SHIFT, Keys.ENTER));
        area.sendKeys(line);
        String note = area.getAttribute("value");
        log.info("Send note now ends with: ...{}", note.substring(Math.max(0, note.length() - 80)));
        return note;
    }

    /**
     * Attaches {@code files} in "المرفقات" of the send dialog, ONE BY ONE: the
     * field takes a single file per selection (QC 2026-11-01: "the element can
     * not hold multiple files"). After each file it waits until its name is
     * shown. Stops when the dialog no longer offers a file field.
     *
     * @return the files that were attached.
     */
    public List<Path> attachToSendDialog(List<Path> files) {
        List<Path> attached = new ArrayList<>();
        for (Path file : files) {
            List<WebElement> inputs = driver.findElement(SEND_DIALOG).findElements(By.cssSelector("input[type='file']"));
            if (inputs.isEmpty()) {
                log.info("No more file field in the send dialog after {} file(s).", attached.size());
                break;
            }
            inputs.get(0).sendKeys(file.toAbsolutePath().toString());
            String name = file.getFileName().toString();
            new WebDriverWait(driver, WAIT)
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElement(SEND_DIALOG).getText().contains(name));
            attached.add(file);
        }
        log.info("Files given to the e-mail: {}", attached.stream().map(f -> f.getFileName().toString()).toList());
        return attached;
    }

    /**
     * Names of the files listed under "المرفقات" of the send dialog now.
     * QC 2026-11-01: after two PDFs were chosen one by one, only the second
     * was listed (the field keeps one file; a new one replaces it).
     */
    public List<String> sendDialogAttachments(List<Path> candidates) {
        String text = driver.findElement(SEND_DIALOG).getText();
        return candidates.stream().map(f -> f.getFileName().toString()).filter(text::contains).toList();
    }

    /**
     * "ارسال" of the send dialog: REALLY sends the e-mail to the supplier.
     * Old toasts (the save toast) are cleared first, so the toast read after
     * this is the answer to the send. The API calls made by the send are
     * recorded and returned (QC 2026-11-01: after one click the dialog stayed
     * open and the only toast read was the earlier "تم الحفظ بنجاح").
     *
     * @return the API calls recorded while sending.
     */
    public List<String> sendToSupplier() {
        ui.waitForToastsToClear();
        apiRecorder.startRecordingApiCalls();
        By send = By.xpath("//*[@role='dialog' and @data-state='open'][.//h2[starts-with(normalize-space(),'إرسال')]]"
                + "//button[normalize-space()='ارسال']");
        ui.clickWhenReady(send);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !isSendDialogOpen());
        } catch (TimeoutException stillOpen) {
            List<String> calls = apiRecorder.getRecordedApiCalls();
            log.warn("Send dialog still open 20 s after \"ارسال\". API calls: {}", calls);
            if (calls.isEmpty() && isSendDialogOpen()) {
                log.warn("No request was sent → clicking \"ارسال\" again by script.");
                ((JavascriptExecutor) driver).executeScript("arguments[0].click();", driver.findElement(send));
                new WebDriverWait(driver, Duration.ofSeconds(20)).until(d -> !isSendDialogOpen());
            }
        }
        List<String> calls = apiRecorder.getRecordedApiCalls();
        log.info("API calls of the send: {}", calls);
        return calls;
    }

    /** True while the send dialog is open. */
    public boolean isSendDialogOpen() {
        return driver.findElements(SEND_DIALOG).stream().anyMatch(WebElement::isDisplayed);
    }

    /**
     * PDF files for the e-mail: every .pdf in test-data/attachments; when there
     * is none, a small PDF is written to target/ so the test does not depend on it.
     */
    public List<Path> pdfAttachments() {
        try (java.util.stream.Stream<Path> files = Files.list(ATTACHMENTS_FOLDER)) {
            List<Path> pdfs = files.filter(f -> f.getFileName().toString().toLowerCase().endsWith(".pdf")).toList();
            if (!pdfs.isEmpty()) {
                return pdfs;
            }
        } catch (java.io.IOException ignored) {
            // folder missing: a PDF is generated below
        }
        Path pdf = Path.of(System.getProperty("user.dir"), "target", "requisition-email-test.pdf");
        String body = "%PDF-1.4\n1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj\n"
                + "2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj\n"
                + "3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 200 200]>>endobj\n"
                + "trailer<</Root 1 0 R>>\n%%EOF\n";
        try {
            Files.createDirectories(pdf.getParent());
            Files.writeString(pdf, body);
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot write " + pdf, e);
        }
        return List.of(pdf);
    }

    // =====================================================================
    // Attachments ("تفاصيل المرفق")
    // =====================================================================

    private static final By ATTACHMENTS_DIALOG = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='تفاصيل المرفق']]");
    private static final By ADD_ATTACHMENT_DIALOG = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='إضافة مرفق']]");
    private static final String NO_ATTACHMENT_TEXT = "لا يوجد ملف محدد";
    private static final Path ATTACHMENTS_FOLDER =
            Path.of(System.getProperty("user.dir"), "test-data", "attachments");

    /** Toolbar 📎 → "تفاصيل المرفق". */
    public void openAttachments() {
        clickAction("uploadAttachment");
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.visibilityOfElementLocated(ATTACHMENTS_DIALOG));
        // Not ui.waitForOpenDialogReady(): it waits for a "data-modal-agree"
        // button, and in VIEW mode this dialog has "إلغاء" only (QC 2026-11-01,
        // after save + reload of PR2610B30-000001). Wait for its animation instead.
        // waitForAnimationsToFinish runs document.querySelector, so it needs a CSS locator.
        ui.waitForAnimationsToFinish(By.cssSelector("[role='dialog'][data-state='open']"));
    }

    /** Attachments listed in the open "تفاصيل المرفق". */
    public int attachmentCount() {
        WebElement dialog = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(ATTACHMENTS_DIALOG));
        if (dialog.getText().contains(NO_ATTACHMENT_TEXT)) {
            return 0;
        }
        return dialog.findElements(By.cssSelector(".ag-center-cols-container div[role='row']")).size();
    }

    /** Waits (max 15 s) until {@code expected} attachments are listed; returns the last count. */
    public int waitForAttachmentCount(int expected) {
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> attachmentCount() == expected ? expected : null);
        } catch (TimeoutException e) {
            return attachmentCount();
        }
    }

    /** Files in test-data/attachments (what {@link #addAttachmentFiles} uploads). */
    public int countAttachmentFiles() {
        try (java.util.stream.Stream<Path> files = Files.list(ATTACHMENTS_FOLDER)) {
            return (int) files.filter(Files::isRegularFile).count();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + ATTACHMENTS_FOLDER, e);
        }
    }

    /**
     * In the open "تفاصيل المرفق": "إضافة مرفق" → every file of
     * test-data/attachments → title + description → "اضافة". Waits for the
     * upload toast to clear, so a later save does not read it.
     */
    public void addAttachmentFiles(String title, String description) {
        // The dialog opens first and its buttons follow; "إضافة مرفق" is shown
        // only in edit mode (view mode has "إلغاء" only, QC 2026-11-01).
        // QC 2026-11-01 (PR2610B30-000001): read right after opening → not there yet.
        By addButton = By.xpath("//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='تفاصيل المرفق']]"
                + "//button[normalize-space()='إضافة مرفق']");
        try {
            ui.clickWhenReady(addButton);
        } catch (TimeoutException missing) {
            throw new IllegalStateException("\"إضافة مرفق\" is not in 'تفاصيل المرفق' after 10 s (URL "
                    + driver.getCurrentUrl() + "; the button is shown in edit mode only). Buttons: "
                    + driver.findElement(ATTACHMENTS_DIALOG).findElements(By.cssSelector("button")).stream()
                    .map(WebElement::getText).filter(t -> !t.isBlank()).toList(), missing);
        }
        WebElement form = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(ADD_ATTACHMENT_DIALOG));
        ui.waitForOpenDialogReady();

        List<String> paths;
        try (java.util.stream.Stream<Path> files = Files.list(ATTACHMENTS_FOLDER)) {
            paths = files.filter(Files::isRegularFile).map(f -> f.toAbsolutePath().toString()).toList();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("Cannot read " + ATTACHMENTS_FOLDER, e);
        }
        if (paths.isEmpty()) {
            throw new IllegalStateException("No files in " + ATTACHMENTS_FOLDER);
        }
        form.findElement(By.cssSelector("input[type='file']")).sendKeys(String.join("\n", paths));
        WebElement titleInput = form.findElement(byTestId("title"));
        titleInput.clear();
        titleInput.sendKeys(title);
        WebElement descriptionInput = form.findElement(byTestId("description"));
        descriptionInput.clear();
        descriptionInput.sendKeys(description);
        form.findElement(By.xpath(".//button[@data-modal-agree and normalize-space()='اضافة']")).click();
        // Wait for the upload toast itself ("تمت إضافة الملفات بنجاح"), not any
        // toast: on 2026-11-01 an empty toast came first and the upload toast
        // was then read as the save answer.
        ui.waitForToastToAppearAndClear("تمت إضافة الملفات");
        log.info("Uploaded {} attachment file(s).", paths.size());
    }

    /** Deletes the last attachment of "تفاصيل المرفق": its ⚙ menu → "حذف". */
    public void deleteLastAttachment() {
        WebElement dialog = driver.findElement(ATTACHMENTS_DIALOG);
        List<WebElement> menus = dialog.findElements(
                By.cssSelector(".ag-center-cols-container [col-id='actions'] button[aria-haspopup='menu']"));
        if (menus.isEmpty()) {
            throw new IllegalStateException("No attachment to delete in 'تفاصيل المرفق'.");
        }
        new Actions(driver).moveToElement(menus.get(menus.size() - 1)).click().perform();
        ui.clickWhenReady(By.xpath(
                "//*[@role='menu' and @data-state='open']//*[@role='menuitem'][normalize-space()='حذف']"));
    }

    /** Closes "تفاصيل المرفق" with "إلغاء". */
    public void closeAttachments() {
        WebElement dialog = driver.findElement(ATTACHMENTS_DIALOG);
        dialog.findElement(By.xpath(".//button[normalize-space()='إلغاء']")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.invisibilityOfElementLocated(ATTACHMENTS_DIALOG));
    }

    /** Waits for a toast to appear and disappear (a delete toast must not be read as the save toast). */
    public void waitForToastToAppearAndClear() {
        ui.waitForToastToAppearAndClear("");
    }

    // =====================================================================
    // Record search (🔍) and record navigation
    // =====================================================================

    private static final By RECORD_SEARCH_INPUT = By.cssSelector("input[placeholder^='ابحث بالاسم']");

    /** 🔍 → type {@code code} → its result; waits until another document is shown. */
    public void openFromRecordSearch(String code) {
        String urlBefore = driver.getCurrentUrl();
        clickAction("recordSearch");
        recordSearchInput();
        By result = By.xpath("//button[span[1][normalize-space()=" + xpathLiteral(code) + "]]");
        // QC 2026-11-05 (invoices): the result did not show within 10 s. The popup
        // loads its first page of records while the code is typed, so the text can be
        // lost or answered late. Wait for that first page, then type, check the box
        // and type again if the result does not come.
        waitForRecordSearchResults();
        for (int attempt = 1; ; attempt++) {
            WebElement box = recordSearchInput();
            box.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
            box.sendKeys(code);
            try {
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .ignoring(StaleElementReferenceException.class)
                        .until(ExpectedConditions.elementToBeClickable(result));
                break;
            } catch (TimeoutException late) {
                String typed = recordSearchInput().getAttribute("value");
                if (attempt == 3) {
                    throw new TimeoutException("Record search did not list " + code + " after " + attempt
                            + " tries (search box shows [" + typed + "]).", late);
                }
                log.warn("Record search did not list {} (box shows [{}], attempt {}/3) → typing again.",
                        code, typed, attempt);
            }
        }
        ui.clickWhenReady(result);
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> !d.getCurrentUrl().equals(urlBefore));
        waitForDocumentLoaded();
    }

    /** The visible record-search box (waits for it). */
    private WebElement recordSearchInput() {
        return new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(RECORD_SEARCH_INPUT).stream().filter(WebElement::isDisplayed)
                        .findFirst().orElse(null));
    }

    /** Waits (max 10 s, no failure) until the record-search popup lists its first records ("code — id"). */
    private void waitForRecordSearchResults() {
        By anyResult = By.xpath("//button[span[2][contains(normalize-space(),'—')]]");
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> d.findElements(anyResult).stream().anyMatch(WebElement::isDisplayed));
        } catch (TimeoutException empty) {
            log.warn("Record search listed no records before typing; typing anyway.");
        }
    }

    /** {@code id}: pageFirst, pagePrevious10, pageBack, pageNext, pageNext10, pageLast. */
    public void clickRecordNavigation(String id) {
        String urlBefore = driver.getCurrentUrl();
        ui.clickWhenReady(By.id(id));
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> !d.getCurrentUrl().equals(urlBefore));
        waitForDocumentLoaded();
    }

    public boolean isRecordNavigationEnabled(String id) {
        return driver.findElement(By.id(id)).isEnabled();
    }

    // =====================================================================
    // List pager and list views
    // =====================================================================

    private static final By FIRST_PAGE = By.xpath(
            "//button[@aria-label='Previous'][.//span[contains(@class,'sr-only') and normalize-space()='first']]");
    private static final By LAST_PAGE = By.xpath(
            "//button[@aria-label='Next'][.//span[contains(@class,'sr-only') and normalize-space()='last']]");
    private static final By PREVIOUS_PAGE = By.xpath(
            "//button[@aria-label='Previous'][.//span[contains(@class,'sr-only') and normalize-space()='Previous']]");
    private static final By NEXT_ONE_PAGE = By.xpath(
            "//button[@aria-label='Next'][.//span[contains(@class,'sr-only') and normalize-space()='Next']]");

    /** {from, to, total} of "عرض x - y / z". */
    public int[] paginationRange() {
        return list.getPaginationRange();
    }

    public int visibleRowCount() {
        return driver.findElements(LIST_ROWS).size();
    }

    public boolean isListNextPageEnabled() {
        return driver.findElement(NEXT_ONE_PAGE).isEnabled();
    }

    public boolean isListPreviousPageEnabled() {
        return driver.findElement(PREVIOUS_PAGE).isEnabled();
    }

    public void goToFirstListPage() {
        goToEndPage(FIRST_PAGE, r -> r[0] == 1);
    }

    public void goToLastListPage() {
        goToEndPage(LAST_PAGE, r -> r[1] == r[2]);
    }

    private void goToEndPage(By button, Predicate<int[]> arrived) {
        WebElement pager = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(button));
        if (pager.isEnabled()) {
            ui.clickWhenReady(button);
        }
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(IllegalStateException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    // QC 2026-11-05 (free quantity processing): a short grid renders only the rows in
                    // view (8 of "1 - 10"), so the rows are not required to equal the range.
                    int[] r = paginationRange();
                    return arrived.test(r) && visibleRowCount() > 0;
                });
    }

    public void switchToKanbanView() {
        list.switchToKanbanView();
    }

    public void switchToListView() {
        list.switchToListView();
    }

    public List<String> kanbanCardCodes() {
        return list.getKanbanCardCodes();
    }

    public String kanbanCardStatus(String code) {
        return list.getKanbanCardStatus(code);
    }

    public void openKanbanCard(String code) {
        list.openKanbanCard(code);
        waitForDocumentLoaded();
    }

    public void groupListBy(String field) {
        list.groupListBy(field);
    }

    public Map<String, Integer> groupCounts() {
        return list.getGroupCounts();
    }

    public Map<String, Integer> summaryCards() {
        return list.getSummaryCards();
    }

    /** True while the grid (not the Kanban board) is shown. */
    public boolean isListViewShown() {
        return !driver.findElements(By.cssSelector(".ag-root")).isEmpty();
    }

    // =====================================================================
    // Follow-up screens: Items Pricing ($), Print Barcode
    // =====================================================================

    /** Toolbar "$" → Sales → Pricing (/sales/pricing/add?itemIds=...). */
    public void clickItemsPricing() {
        clickAndFollowToScreen(() -> ui.clickWhenReady(By.id("itemsPricing")), "/sales/pricing/add");
    }

    /** Toolbar barcode → Inventory → Item Barcodes (?referenceCode=<code>). */
    public void clickPrintBarcode() {
        clickAndFollowToScreen(() -> ui.clickWhenReady(By.id("printBarcode")), "/inventory/item-barcodes");
    }

    /** Texts of grid column {@code colId} on the current screen. */
    public List<String> gridColumnTexts(String colId) {
        return ui.getGridColumnTexts(colId);
    }

    /**
     * Clicks something that opens another screen and follows it. In the test
     * browser such screens may open in a new tab (seen on orders); in a
     * normal browser in the same tab. Waits for either case.
     */
    private void clickAndFollowToScreen(Runnable click, String expectedPath) {
        java.util.Set<String> before = driver.getWindowHandles();
        click.run();
        String target = new WebDriverWait(driver, Duration.ofSeconds(15)).until(d -> {
            if (d.getCurrentUrl().contains(expectedPath)) {
                return "same-tab";
            }
            return d.getWindowHandles().stream().filter(h -> !before.contains(h)).findFirst().orElse(null);
        });
        if (!"same-tab".equals(target)) {
            driver.switchTo().window(target);
        }
        new WebDriverWait(driver, Duration.ofSeconds(15)).until(ExpectedConditions.urlContains(expectedPath));
        log.info("Opened {}", driver.getCurrentUrl());
    }

    // =====================================================================
    // "⋮" follow-up documents of an Approved document
    // =====================================================================

    /** "⋮" → {@code label}, then follows the screen it opens (URL containing {@code expectedPath}). */
    public void clickMenuItemAndFollow(String label, String expectedPath) {
        clickAndFollowToScreen(() -> clickMenuItem(label), expectedPath);
    }

    /**
     * "⋮" → {@code parent} (a sub-menu) → {@code child}, then follows the
     * screen it opens. QC 2026-11-01: "مقارنة عروض الاسعار" opens a sub-menu
     * with "مقارنة يدوية" and "مقارنة آلية".
     */
    public void clickSubMenuItemAndFollow(String parent, String child, String expectedPath) {
        clickAndFollowToScreen(() -> {
            openFormMenu();
            By subTriggers = By.cssSelector("[role='menu'][data-state='open'] [role='menuitem'][aria-haspopup='menu']");
            WebElement trigger = new WebDriverWait(driver, WAIT)
                    .until(d -> d.findElements(subTriggers).stream()
                            .filter(e -> comparable(e.getText()).equals(comparable(parent)))
                            .findFirst().orElse(null));
            new Actions(driver).moveToElement(trigger).perform();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(d -> d.findElements(OPEN_MENU).size() > 1);
            } catch (TimeoutException hoverNotEnough) {
                trigger.sendKeys(Keys.ENTER); // keyboard opens the sub-menu too
                new WebDriverWait(driver, WAIT).until(d -> d.findElements(OPEN_MENU).size() > 1);
            }
            List<WebElement> menus = driver.findElements(OPEN_MENU);
            WebElement item = menus.get(menus.size() - 1).findElements(By.cssSelector("[role='menuitem']")).stream()
                    .filter(e -> comparable(e.getText()).equals(comparable(child)))
                    .findFirst()
                    .orElseThrow(() -> new NoSuchElementException("No [" + child + "] under [" + parent + "]."));
            // QC 2026-11-01 (PR2608-000021): a native click on the sub-menu item
            // closed the menus without opening the comparison (the pointer
            // crosses the parent menu on its way). A script click selects it,
            // as checked by hand in the browser on PR2608-000024.
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", item);
        }, expectedPath);
    }

    /** Value of input #id on the current screen ("" when missing). */
    public String inputValueById(String id) {
        List<WebElement> inputs = driver.findElements(By.id(id));
        return inputs.isEmpty() ? "" : inputs.get(0).getAttribute("value").trim();
    }

    /**
     * Texts of the toasts shown during {@code within} that do NOT contain
     * {@code ignored} (several toasts can be stacked, e.g. the attachment
     * upload toast and the save toast). Returns as soon as one is seen.
     */
    public List<String> otherToastTexts(String ignored, Duration within) {
        By toasts = By.cssSelector("[data-rht-toaster] > div");
        long end = System.currentTimeMillis() + within.toMillis();
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        while (System.currentTimeMillis() < end) {
            try {
                driver.findElements(toasts).stream()
                        .map(e -> e.getText().trim())
                        .filter(t -> !t.isEmpty() && !t.contains(ignored))
                        .forEach(t -> seen.add(t.replace('\n', ' ')));
            } catch (StaleElementReferenceException ignoredStale) {
                // toast re-rendered; read again
            }
            if (!seen.isEmpty()) {
                break;
            }
            try {
                Thread.sleep(150);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        return new ArrayList<>(seen);
    }

    /**
     * Texts of grid column {@code colId}, or an empty list when the grid shows
     * its empty state ("المحتوى فارغ"). Waits up to {@code timeout} for either.
     */
    public List<String> gridColumnTextsOrEmpty(String colId, Duration timeout) {
        By cells = By.cssSelector(".ag-center-cols-container div[role='row'] [col-id='" + colId + "']");
        Boolean hasRows = new WebDriverWait(driver, timeout)
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    if (!d.findElements(cells).isEmpty()) {
                        return Boolean.TRUE;
                    }
                    String body = d.findElement(By.tagName("body")).getText();
                    return body.contains("المحتوى فارغ") ? Boolean.FALSE : null;
                });
        if (!hasRows) {
            log.info("Grid is empty (\"المحتوى فارغ\") on {}", driver.getCurrentUrl());
            return List.of();
        }
        return ui.getGridColumnTexts(colId);
    }
}
