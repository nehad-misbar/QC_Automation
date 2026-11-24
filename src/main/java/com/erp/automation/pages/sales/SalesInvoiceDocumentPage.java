package com.erp.automation.pages.sales;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.Month;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;
import static com.erp.automation.ui.DocumentApi.textOf;

/**
 * Shared page object of the two invoice-shaped Sales screens: "فواتير
 * المبيعات" and "مردود المبيعات". Both use the same form (checked in QC on
 * 2026-11-12, return SRI2611-000060 made from invoice SI2611-000217):
 * <ul>
 *     <li>number {@code #invoiceCode}; customer {@code vendorId}; "البيان"
 *     {@code #notes}; prices radios {@code priceNature-0/1};</li>
 *     <li>three statuses instead of one: "حالة الفاتورة" (معتمد), "حالة
 *     التسليم" (معلقة …) and "حالة الدفع" (مدفوع / غير مدفوع …). There is no
 *     Draft: a saved document is Approved at once;</li>
 *     <li>list status column {@code invoiceStatus};</li>
 *     <li>lines: quantity {@code quantity}, price {@code cost};</li>
 *     <li>"طريقة الدفع" (نقدا / بنك / آجل / شيك …) and "حساب الدفع": saving
 *     "نقدا" without "حساب الدفع" is refused by the server ("حساب الدفع مطلوب");</li>
 *     <li>toolbar "قيد اليومية" ({@code journalEntry}) once saved;</li>
 *     <li>edit of a saved document ({@code /view-approved/}): "البيان" only.</li>
 * </ul>
 */
public abstract class SalesInvoiceDocumentPage extends SalesDocumentPage {

    public static final String QUANTITY_COLUMN = "quantity";
    public static final String PRICE_COLUMN = "cost";

    /** Label of the main status on the form and its list column. */
    public static final String STATUS_LABEL = "حالة الفاتورة";
    public static final String DELIVERY_STATUS_LABEL = "حالة التسليم";
    public static final String PAYMENT_STATUS_LABEL = "حالة الدفع";

    public static final String FUND_ACCOUNT_FIELD = "invoicePaymentInformation.fundAccountId";

    /** Prices radios: باستثناء الضريبة / شاملة الضريبة. */
    private static final String[] PRICE_RADIOS = {"priceNature-0", "priceNature-1"};

    /** Fields of a new document that the system fills (locked). */
    public static final List<String> READ_ONLY_FIELDS = List.of(
            "invoiceCode", "convertionRate", "invoiceDate", "vendorTaxNumber", "vendorAccountId", "approvalDate");

    protected SalesInvoiceDocumentPage(WebDriver driver, String prefix, String screenPath, String apiResource) {
        super(driver, prefix, screenPath, apiResource, "invoiceCode");
        this.noteField = "notes";
        this.listStatusColumn = "invoiceStatus";
    }

    /** "حالة الفاتورة" on the form ("" before the first save). */
    @Override
    public String statusLabel() {
        return labelledValue(STATUS_LABEL);
    }

    public String deliveryStatus() {
        return labelledValue(DELIVERY_STATUS_LABEL);
    }

    public String paymentStatus() {
        return labelledValue(PAYMENT_STATUS_LABEL);
    }

    /** Text next to label {@code label} (label → div → div holding the badge). */
    private String labelledValue(String label) {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const l=[...document.querySelectorAll('label')].find(x=>x.textContent.trim()===arguments[0]);"
                        + "if(!l) return '';"
                        + "const box=l.parentElement && l.parentElement.parentElement;"
                        + "return box ? box.innerText.replace(arguments[0],'').trim() : '';", label);
        return text == null ? "" : text.toString().trim();
    }

    public String selectRandomPriceOption() {
        return clickRandomRadio(PRICE_RADIOS);
    }

    public String priceOption() {
        return checkedRadio(PRICE_RADIOS);
    }

    /**
     * "طريقة الدفع" at random, then "حساب الدفع" when it can be chosen and is
     * empty (the server needs it for نقدا, QC 2026-11-12).
     *
     * @return the payment method chosen ("" when locked).
     */
    public String selectRandomPaymentMethod() {
        String method = "";
        if (waitForSelectEnabled("paymentMethod", java.time.Duration.ofSeconds(3))) {
            method = selectRandomOption("paymentMethod");
            confirmDialogIfShown(java.time.Duration.ofMillis(800));
        }
        fillFundAccountIfNeeded();
        return method;
    }

    /** "طريقة الدفع" option for cash. */
    public static final String PAYMENT_CASH = "نقدا";

    /**
     * Sets "طريقة الدفع" to {@code method} (e.g. {@link #PAYMENT_CASH}), then
     * "حساب الدفع" when it is empty (the server needs it for نقدا, QC 2026-11-12).
     *
     * @return the payment method shown afterwards.
     */
    public String selectPaymentMethod(String method) {
        if (!waitForSelectEnabled("paymentMethod", java.time.Duration.ofSeconds(5))) {
            throw new IllegalStateException("\"طريقة الدفع\" is locked; cannot choose " + method + ".");
        }
        if (!method.equals(selectedText("paymentMethod"))) {
            selectOptionContaining("paymentMethod", method);
            confirmDialogIfShown(java.time.Duration.ofMillis(800));
        }
        fillFundAccountIfNeeded();
        String shown = selectedText("paymentMethod");
        log.info("طريقة الدفع = {}", shown);
        return shown;
    }

    /**
     * "الى حساب" (required with طريقة الدفع "الحساب", QC run 2026-11-18 13:56,
     * SRI2611-000062: "هذا الحقل مطلوب").
     */
    public static final String TO_ACCOUNT_FIELD = "invoicePaymentInformation.toAccountId";

    /** "حساب الدفع" and "الى حساب" when they are shown, selectable and empty. */
    public void fillFundAccountIfNeeded() {
        for (String field : List.of(FUND_ACCOUNT_FIELD, TO_ACCOUNT_FIELD)) {
            if (hasField(field, java.time.Duration.ofSeconds(1))
                    && waitForSelectEnabled(field, java.time.Duration.ofSeconds(2))
                    && (selectedText(field).isBlank() || "اختر".equals(selectedText(field).trim()))) {
                String account = selectRandomOption(field);
                log.info("{} = {}", FUND_ACCOUNT_FIELD.equals(field) ? "حساب الدفع" : "الى حساب", account);
            }
        }
    }

    // =====================================================================
    // "Fill everything that is enabled" (QC 2026-11-16): the fields, tabs and
    // line cells are found on the screen, so a field added later is filled too.
    // =====================================================================

    /**
     * Returns the AG Grid API of the item lines grid (the grid whose header has
     * {@code itemId}, else the grid of the active tab), read from React's state.
     * It is used to ask which cells the app lets the user edit and to bring a
     * cell into view; values are still typed through the screen.
     */
    private static final String ITEMS_GRID_JS =
            "const w=[...document.querySelectorAll('.ag-root-wrapper')]"
                    + "  .find(x=>!x.closest('[role=dialog]') && x.querySelector(\".ag-header-cell[col-id='itemId']\"))"
                    + " || document.querySelector('[role=tabpanel][data-state=active] .ag-root-wrapper');";
    private static final String ACTIVE_TAB_GRID_JS =
            "const w=document.querySelector('[role=tabpanel][data-state=active] .ag-root-wrapper');";

    /**
     * False: the item lines grid ("تفاصيل الفاتورة"). True: the grid of the active line tab
     * (e.g. "الخصومات والإضافات"); the line methods (editableLineColumns, lineData,
     * setLineNumberInEditor …) then work on that grid. See {@link #useActiveTabGrid}.
     */
    private boolean activeTabGrid = false;

    /** Makes the line methods work on the grid of the active tab (true) or on the item lines (false). */
    public void useActiveTabGrid(boolean onActiveTab) {
        this.activeTabGrid = onActiveTab;
    }

    /** CSS prefix of the grid the line methods work on. */
    private String gridScope() {
        return activeTabGrid ? "[role=tabpanel][data-state=active] " : "";
    }

    private String gridApiJs() {
        return LINES_GRID_API_JS.replace("/*GRID*/", activeTabGrid ? ACTIVE_TAB_GRID_JS : ITEMS_GRID_JS);
    }

    private static final String LINES_GRID_API_JS =
            "const linesGridApi=()=>{"
                    + "/*GRID*/"
                    + "if(!w) return null;"
                    + "const fk=Object.keys(w).find(k=>k.startsWith('__reactFiber'));"
                    + "const isApi=v=>v && typeof v==='object' && typeof v.getColumn==='function'"
                    + "  && typeof v.getDisplayedRowAtIndex==='function';"
                    + "let f=fk ? w[fk] : null, d=0;"
                    + "while(f && d<15){ let h=f.memoizedState, i=0;"
                    + "  while(h && i<40){ const ms=h.memoizedState;"
                    + "    if(isApi(ms)) return ms; if(ms && ms.current && isApi(ms.current)) return ms.current;"
                    + "    h=h.next; i++; }"
                    + "  f=f.return; d++; }"
                    + "return null;};";

    /**
     * Ids of the enabled React Selects of the header ({@code inActiveTab}
     * false) or of the active line tab: data-testid without the prefix, in
     * screen order. Selects in grids and dialogs are not included.
     */
    @SuppressWarnings("unchecked")
    public List<String> enabledSelects(boolean inActiveTab) {
        Object ids = ((JavascriptExecutor) driver).executeScript(
                "const pre=arguments[0]+'-', inTab=arguments[1];"
                        + "const scope=inTab ? document.querySelector('[role=tabpanel][data-state=active]') : document;"
                        + "if(!scope) return [];"
                        + "return [...scope.querySelectorAll('.react-select[data-testid^=\"'+pre+'\"]')]"
                        + "  .filter(s=>!s.closest('.ag-root-wrapper') && !s.closest('[role=dialog]')"
                        + "    && (inTab || !s.closest('[role=tabpanel]')) && s.offsetParent!==null)"
                        + "  .filter(s=>{const c=s.querySelector('[class*=select__control]'), i=s.querySelector('input');"
                        + "    return c && !/is-disabled/.test(c.className) && !(i && i.disabled);})"
                        + "  .map(s=>s.getAttribute('data-testid').substring(pre.length));",
                prefix, inActiveTab);
        return new ArrayList<>((List<String>) ids);
    }

    /**
     * Ids of the enabled text inputs of the header (or of the active line tab):
     * the input id, else its data-testid without the prefix. Inputs of React
     * Selects, grids, dialogs and date pickers are not included, nor inputs
     * with a generated id only.
     */
    @SuppressWarnings("unchecked")
    public List<String> enabledTextInputs(boolean inActiveTab) {
        Object ids = ((JavascriptExecutor) driver).executeScript(
                "const pre=arguments[0]+'-', inTab=arguments[1];"
                        + "const scope=inTab ? document.querySelector('[role=tabpanel][data-state=active]') : document;"
                        + "if(!scope) return [];"
                        + "return [...scope.querySelectorAll('input[type=text], input:not([type]), textarea')]"
                        + "  .filter(i=>!i.closest('.react-select') && !i.closest('.ag-root-wrapper') && !i.closest('[role=dialog]')"
                        + "    && !i.closest('.rmdp-container') && (inTab || !i.closest('[role=tabpanel]'))"
                        + "    && i.offsetParent!==null && !i.disabled && !i.readOnly)"
                        + "  .map(i=>i.id || (i.getAttribute('data-testid')||'').replace(pre,''))"
                        + "  .filter(x=>x && !/^_r_/.test(x));",
                prefix, inActiveTab);
        return new ArrayList<>((List<String>) ids);
    }

    /** Line tabs as label → enabled, in screen order. */
    @SuppressWarnings("unchecked")
    public Map<String, Boolean> lineTabs() {
        // A list of pairs, not a JS object: the order of a returned object is not kept.
        List<List<Object>> tabs = (List<List<Object>>) ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('[role=tab]')]"
                        + "  .filter(t=>t.innerText.trim())"
                        + "  .map(t=>[t.innerText.trim(),"
                        + "    !(t.disabled || t.getAttribute('aria-disabled')==='true' || t.hasAttribute('data-disabled'))]);");
        Map<String, Boolean> out = new LinkedHashMap<>();
        tabs.forEach(pair -> out.put(String.valueOf(pair.get(0)), Boolean.TRUE.equals(pair.get(1))));
        return out;
    }

    /**
     * Columns of line {@code row} that the app lets the user edit now (visible
     * columns, screen order), as the grid itself decides
     * ({@code column.isCellEditable}).
     */
    @SuppressWarnings("unchecked")
    public List<String> editableLineColumns(int row) {
        Object cols = ((JavascriptExecutor) driver).executeScript(gridApiJs()
                        + "const api=linesGridApi(); if(!api) return null;"
                        + "const node=api.getDisplayedRowAtIndex(arguments[0]); if(!node) return [];"
                        + "return api.getAllDisplayedColumns().filter(c=>c.isCellEditable(node)).map(c=>c.getColId());",
                row);
        if (cols == null) {
            throw new IllegalStateException("The item lines grid (AG Grid API) was not found.");
        }
        return new ArrayList<>((List<String>) cols);
    }

    /**
     * Whether a USER can change cell {@code colId} of line {@code row}: the cell is double-clicked,
     * {@code text} is typed and Enter pressed; the cell can be changed when the value shown
     * changed or a dialog opened (e.g. "البحث عن صنف" for the item). The attempt is then
     * cancelled (editing stopped without keeping, dialogs closed with Escape).
     *
     * <p>QC run 2026-11-19 11:15 (SRI2608-000003): {@link #editableLineColumns} said "itemId"
     * was editable on a saved return, but by hand the item cannot be changed: the grid's
     * column definition is not what the user can do.</p>
     */
    public boolean userCanChangeLineCell(int row, String colId, String text) {
        stopCellEditing();
        scrollLineCellIntoView(row, colId);
        String before = lineData(row).getOrDefault(colId, "");
        String grid = activeTabGrid ? ACTIVE_TAB_GRID_JS : ITEMS_GRID_JS;
        Object found = ((JavascriptExecutor) driver).executeScript(grid
                        + "if(!w) return null;"
                        + "return [...w.querySelectorAll(\"div[role=row][row-index='\"+arguments[0]+\"'] [col-id='\"+arguments[1]+\"']\")]"
                        + "  .find(c=>c.offsetParent!==null) || null;",
                row, colId);
        if (!(found instanceof org.openqa.selenium.WebElement cell)) {
            log.info("Line {} {}: cell not shown → cannot be changed.", row + 1, colId);
            return false;
        }
        boolean changed = false;
        try {
            new org.openqa.selenium.interactions.Actions(driver).doubleClick(cell).perform();
            pause(700);
            Object editor = ((JavascriptExecutor) driver).executeScript(
                    "const e=document.querySelector('.ag-cell-inline-editing, .ag-popup-editor');"
                            + "if(!e) return null;"
                            + "const i=e.querySelector('input:not([type=checkbox]), textarea');"
                            + "return (i && !i.disabled && !i.readOnly) ? i : null;");
            if (editor instanceof org.openqa.selenium.WebElement input) {
                input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), text,
                        org.openqa.selenium.Keys.ENTER);
                pause(900);
                changed = isDialogOpen() || !before.equals(lineData(row).getOrDefault(colId, ""));
            }
        } catch (RuntimeException notEditable) {
            log.info("Line {} {}: {} → cannot be changed.", row + 1, colId, notEditable.getClass().getSimpleName());
        } finally {
            ((JavascriptExecutor) driver).executeScript(gridApiJs()
                    + "const api=linesGridApi(); if(api) api.stopEditing(true);");
            for (int attempt = 1; attempt <= 3 && isDialogOpen(); attempt++) {
                new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
                pause(400);
            }
        }
        log.info("Line {} {}: [{}] typed → {} (shows [{}]).", row + 1, colId, text,
                changed ? "CHANGED (the user can edit it)" : "not changed (locked for the user)",
                lineData(row).getOrDefault(colId, ""));
        return changed;
    }

    /** Data of line {@code row} as the grid holds it: field → text ("" for none). */
    @SuppressWarnings("unchecked")
    public Map<String, String> lineData(int row) {
        Object data = ((JavascriptExecutor) driver).executeScript(gridApiJs()
                        + "const api=linesGridApi(); const node=api && api.getDisplayedRowAtIndex(arguments[0]);"
                        + "if(!node || !node.data) return {};"
                        + "const out={}; Object.entries(node.data).forEach(([k,v])=>{"
                        + "  out[k]= v==null ? '' : (typeof v==='object' ? JSON.stringify(v) : String(v));});"
                        + "return out;",
                row);
        return new LinkedHashMap<>((Map<String, String>) data);
    }

    /** Scrolls the lines grid so that cell ({@code row}, {@code colId}) is rendered. */
    public void scrollLineCellIntoView(int row, String colId) {
        ((JavascriptExecutor) driver).executeScript(gridApiJs()
                        + "const api=linesGridApi(); if(!api) return;"
                        + "api.ensureIndexVisible(arguments[0]); api.ensureColumnVisible(arguments[1]);",
                row, colId);
        pause(300);
    }

    /**
     * Sets date cell {@code colId} of line {@code row} (e.g. تاريخ الإنتاج /
     * تاريخ الانتهاء) with its calendar: the editor is opened, the calendar is
     * moved to the month of {@code date} with its arrows and the day is clicked
     * (a click on a day closes the editor and keeps the date, QC 2026-11-16).
     */
    public void setLineDate(int row, String colId, LocalDate date) {
        // QC run 2026-11-16 14:35: after the previous cell the grid had added an empty
        // line 2 and moved the focus there; the editor opened by the API alone did not
        // show its calendar. So any open editor is closed first, the cell is
        // double-clicked like a user does, and the API is only the fallback.
        stopCellEditing();
        scrollLineCellIntoView(row, colId);
        By cell = By.cssSelector(gridScope() + ".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "']");
        By calendar = By.cssSelector(".rmdp-calendar");
        new org.openqa.selenium.interactions.Actions(driver)
                .doubleClick(new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(cell)))
                .perform();
        if (!appears(calendar, Duration.ofSeconds(3))) {
            log.warn("Line {} {}: no calendar after a double-click → opened through the grid.", row + 1, colId);
            ((JavascriptExecutor) driver).executeScript(gridApiJs()
                            + "const api=linesGridApi(); api.startEditingCell({rowIndex:arguments[0], colKey:arguments[1]});",
                    row, colId);
            if (!appears(calendar, Duration.ofSeconds(3))) {
                // The calendar opens when the editor's input gets the focus.
                ((JavascriptExecutor) driver).executeScript(
                        "const i=document.querySelector('.ag-popup-editor input, .ag-cell-inline-editing input');"
                                + "if(i){ i.focus(); i.click(); }");
            }
            if (!appears(calendar, Duration.ofSeconds(5))) {
                throw new IllegalStateException("Line " + (row + 1) + " " + colId + ": the date calendar did not open.");
            }
        }
        chooseInOpenCalendar(date, colId);
        try {
            new WebDriverWait(driver, WAIT).pollingEvery(Duration.ofMillis(200))
                    .until(d -> lineData(row).getOrDefault(colId, "").startsWith(date.toString()));
        } catch (TimeoutException notKept) {
            throw new IllegalStateException("Line " + (row + 1) + " " + colId + " shows ["
                    + lineData(row).get(colId) + "] instead of " + date + ".", notKept);
        }
        stopCellEditing();
        log.info("Line {} {} = {}", row + 1, colId, date);
    }

    /**
     * Writes {@code text} into text cell {@code colId} (e.g. "note") of line
     * {@code row}, whatever its editor is: inline input or the pop-up text box.
     * QC run 2026-11-16 14:41: the usual way (Enter to commit) left "الملاحظة"
     * empty, then its editor did not open again. Here the editor is opened by the
     * grid, the value is written into it and the grid itself ends the editing,
     * which keeps the value; the result is read back from the grid's data.
     *
     * @return what the line holds afterwards. It can differ from {@code text}: QC run
     *         2026-11-16 16:27, "الملاحظة" kept "ملاحظةسطرآلية" (no spaces, digits or "-");
     *         the caller decides. Fails only when the cell stays empty.
     */
    public String setLineTextInEditor(int row, String colId, String text) {
        return writeLineCell(row, colId, text, kept -> text.equals(kept.trim()));
    }

    /**
     * Number cell {@code colId} (e.g. "discountRate") of line {@code row}, the same safe
     * way as {@link #setLineTextInEditor}. QC run 2026-11-17 09:44 (invoice from a sales
     * order, line 2): the usual way typed "5" into the item cell (it opened "البحث عن صنف")
     * and "الخصم%" stayed 0.
     *
     * @return what the line holds afterwards.
     */
    public String setLineNumberInEditor(int row, String colId, String value) {
        java.math.BigDecimal expected = new java.math.BigDecimal(value);
        String kept = writeLineCell(row, colId, value, held -> {
            try {
                return !held.isBlank() && new java.math.BigDecimal(held.trim()).compareTo(expected) == 0;
            } catch (NumberFormatException notANumber) {
                return false;
            }
        });
        try {
            if (new java.math.BigDecimal(kept.trim()).compareTo(expected) != 0) {
                throw new IllegalStateException("Line " + (row + 1) + " " + colId + " holds [" + kept + "] instead of " + value + ".");
            }
        } catch (NumberFormatException notANumber) {
            throw new IllegalStateException("Line " + (row + 1) + " " + colId + " holds [" + kept + "] instead of " + value + ".");
        }
        return kept;
    }

    /** Opens the cell through the grid, writes {@code value}, ends the editing; returns what the line holds. */
    private String writeLineCell(int row, String colId, String value, java.util.function.Predicate<String> isKept) {
        String text = value;
        By editor = By.cssSelector(".ag-popup-editor :is(input, textarea), .ag-cell-inline-editing :is(input, textarea)");
        for (int attempt = 1; attempt <= 3; attempt++) {
            stopCellEditing();
            scrollLineCellIntoView(row, colId);
            if (attempt < 3) {
                ((JavascriptExecutor) driver).executeScript(gridApiJs()
                                + "const api=linesGridApi(); api.startEditingCell({rowIndex:arguments[0], colKey:arguments[1]});",
                        row, colId);
            } else {
                // QC run 2026-11-16 16:54: the editor opened by the grid closed at once three times;
                // the last attempt opens it the way a user does.
                By cell = By.cssSelector(gridScope() + ".ag-center-cols-container div[role='row'][row-index='" + row
                        + "'] [col-id='" + colId + "']");
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(cell)))
                        .perform();
            }
            pause(400); // let the grid finish re-rendering the row before writing
            if (!appears(editor, Duration.ofSeconds(4))) {
                log.warn("Line {} {}: no editor (attempt {}/3).", row + 1, colId, attempt);
                continue;
            }
            // QC run 2026-11-16 16:45: the editor was there, then gone a moment later
            // (NoSuchElementException); the grid re-rendered it. Try again instead of failing.
            final int tryNumber = attempt;
            try {
                // Found again and written in one step, for up to 3 s, while the grid re-renders it.
                new WebDriverWait(driver, Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(200))
                        .ignoring(org.openqa.selenium.NoSuchElementException.class)
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            writeIntoEditor(d.findElement(editor), text, tryNumber);
                            return true;
                        });
            } catch (TimeoutException gone) {
                log.warn("Line {} {}: the editor closed while writing (attempt {}/3).", row + 1, colId, attempt);
                continue;
            }
            ((JavascriptExecutor) driver).executeScript(gridApiJs()
                    + "const api=linesGridApi(); api.stopEditing(false);");
            try {
                new WebDriverWait(driver, Duration.ofSeconds(4)).pollingEvery(Duration.ofMillis(200))
                        .until(d -> isKept.test(lineData(row).getOrDefault(colId, "")));
                log.info("Line {} {} = {}", row + 1, colId, text);
                return lineData(row).getOrDefault(colId, "");
            } catch (TimeoutException notKept) {
                log.warn("Line {} {} holds [{}] (attempt {}/3).", row + 1, colId, lineData(row).get(colId), attempt);
                String held = lineData(row).getOrDefault(colId, "").trim();
                if (!held.isBlank() && !held.matches("0+(\\.0+)?")) {
                    break; // something was kept (the cell changes what is typed): the caller compares
                }
            }
        }
        String kept = lineData(row).getOrDefault(colId, "").trim();
        if (kept.isEmpty()) {
            throw new IllegalStateException("Could not set " + colId + " of line " + (row + 1) + " to " + text + "; it stays empty.");
        }
        return kept;
    }

    /** Attempt 1: value set by script; later attempts: typed into the editor. */
    private void writeIntoEditor(org.openqa.selenium.WebElement input, String text, int attempt) {
        if (attempt == 1) {
            ((JavascriptExecutor) driver).executeScript(
                    "const i=arguments[0];"
                            + "const set=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(i),'value').set;"
                            + "i.focus(); set.call(i, arguments[1]);"
                            + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                            + "i.dispatchEvent(new Event('change',{bubbles:true}));",
                    input, text);
        } else {
            input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"));
            input.sendKeys(text);
        }
    }

    /**
     * In the open calendar (react-multi-date-picker): moves to the month of {@code date}
     * with the arrows and clicks its day. Works by script, so a calendar Selenium does
     * not see as "visible" is handled too.
     */
    private void chooseInOpenCalendar(LocalDate date, String what) {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (int step = 0; step < 36; step++) {
            // textContent: works even when Selenium does not see the calendar as "visible".
            String[] shown = String.valueOf(js.executeScript(
                    "return document.querySelector('.rmdp-calendar .rmdp-header-values').textContent;"))
                    .replaceAll("\\s", "").split(",");
            int shownMonth;
            try {
                shownMonth = Month.valueOf(shown[0].toUpperCase(Locale.ROOT)).getValue();
            } catch (RuntimeException notEnglish) {
                // Month name not in English (e.g. Arabic): the calendar opens on the current
                // month, so move as many months as the date is ahead of today.
                int ahead = (date.getYear() * 12 + date.getMonthValue())
                        - (LocalDate.now().getYear() * 12 + LocalDate.now().getMonthValue());
                for (int i = 0; i < Math.abs(ahead); i++) {
                    js.executeScript("document.querySelector('.rmdp-calendar .rmdp-arrow-container."
                            + (ahead > 0 ? "rmdp-right" : "rmdp-left") + "').click();");
                    pause(200);
                }
                break;
            }
            int diff = (date.getYear() * 12 + date.getMonthValue()) - (Integer.parseInt(shown[1]) * 12 + shownMonth);
            if (diff == 0) {
                break;
            }
            // "rmdp-right" shows the next month, "rmdp-left" the previous one (QC 2026-11-16).
            js.executeScript("document.querySelector('.rmdp-calendar .rmdp-arrow-container." + (diff > 0 ? "rmdp-right" : "rmdp-left")
                    + "').click();");
            pause(200);
        }
        Object clicked = js.executeScript(
                "const d=[...document.querySelectorAll('.rmdp-calendar .rmdp-day:not(.rmdp-deactive):not(.rmdp-disabled)')]"
                        + "  .find(x=>x.textContent.trim()===String(arguments[0]));"
                        + "if(!d) return false; (d.querySelector('span')||d).click(); return true;",
                date.getDayOfMonth());
        if (!Boolean.TRUE.equals(clicked)) {
            throw new IllegalStateException("Day " + date.getDayOfMonth() + " cannot be chosen in the calendar of " + what + ".");
        }
    }

    /**
     * Keys of the enabled, EMPTY date fields of the header (or of the active line tab):
     * inputs of a date picker, or whose id / data-testid / name holds "date". The key is
     * the id, else the data-testid without the prefix, else "#n" (n-th date field).
     * A date picker input counts as enabled even when read-only (it opens a calendar).
     */
    @SuppressWarnings("unchecked")
    public List<String> emptyDateFields(boolean inActiveTab) {
        Object keys = ((JavascriptExecutor) driver).executeScript(DATE_FIELDS_JS
                        + "return dateFields(arguments[0], arguments[1])"
                        + "  .map((i,idx)=>({i, idx, key: i.id || ((i.getAttribute('data-testid')||'').startsWith(arguments[0]+'-')"
                        + "      ? i.getAttribute('data-testid').substring(arguments[0].length+1) : '')}))"
                        + "  .filter(x=>!x.i.disabled && (x.i.closest('.rmdp-container') || !x.i.readOnly) && !x.i.value.trim())"
                        + "  .map(x=> x.key && !/^_r_/.test(x.key) ? x.key : '#'+x.idx);",
                prefix, inActiveTab);
        return new ArrayList<>((List<String>) keys);
    }

    /** All visible date inputs of the header (or of the active tab), in screen order. */
    private static final String DATE_FIELDS_JS =
            "const dateFields=(pre, inTab)=>{"
                    + "const scope=inTab ? document.querySelector('[role=tabpanel][data-state=active]') : document;"
                    + "if(!scope) return [];"
                    + "return [...scope.querySelectorAll('input')].filter(i=>i.type!=='hidden'"
                    + "  && (i.closest('.rmdp-container') || /date/i.test(i.id+' '+(i.getAttribute('data-testid')||'')+' '+(i.name||'')))"
                    + "  && !i.closest('.ag-root-wrapper') && !i.closest('[role=dialog]')"
                    + "  && (inTab || !i.closest('[role=tabpanel]')) && i.offsetParent!==null);};";

    /**
     * Opens date field {@code key} (from {@link #emptyDateFields}) and picks {@code date}
     * in its calendar; checks the field is no longer empty.
     */
    public void setDateField(String key, boolean inActiveTab, LocalDate date) {
        org.openqa.selenium.WebElement input = (org.openqa.selenium.WebElement) ((JavascriptExecutor) driver).executeScript(
                DATE_FIELDS_JS
                        + "const all=dateFields(arguments[0], arguments[1]); const key=arguments[2];"
                        + "if(key.startsWith('#')) return all[Number(key.substring(1))] || null;"
                        + "return all.find(i=>i.id===key || i.getAttribute('data-testid')===arguments[0]+'-'+key) || null;",
                prefix, inActiveTab, key);
        if (input == null) {
            throw new IllegalStateException("Date field [" + key + "] is not on the page.");
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        By calendar = By.cssSelector(".rmdp-calendar");
        try {
            input.click();
        } catch (RuntimeException notClickable) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus(); arguments[0].click();", input);
        }
        if (!appears(calendar, Duration.ofSeconds(3))) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus(); arguments[0].click();", input);
            if (!appears(calendar, Duration.ofSeconds(3))) {
                throw new IllegalStateException("Date field [" + key + "]: the calendar did not open.");
            }
        }
        chooseInOpenCalendar(date, key);
        try {
            new WebDriverWait(driver, Duration.ofSeconds(4)).pollingEvery(Duration.ofMillis(200))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> !input.getAttribute("value").isBlank());
        } catch (TimeoutException stillEmpty) {
            throw new IllegalStateException("Date field [" + key + "] stayed empty after choosing " + date + ".");
        }
        // Close the calendar if it is still open.
        ((JavascriptExecutor) driver).executeScript(
                "if(document.querySelector('.rmdp-calendar')){document.activeElement && document.activeElement.blur();"
                        + " document.body.click();}");
        log.info("Date field {} = {} ({})", key, input.getAttribute("value"), date);
    }

    /**
     * "اضافة سطر" of the ACTIVE tab (e.g. "الخصومات والإضافات"); returns the index of the
     * new line in that tab's grid.
     */
    public int addLineInActiveTab() {
        useActiveTabGrid(true);
        int before = activeGridRowCount();
        By button = By.xpath(ACTIVE_TAB_ADD_LINE);
        org.openqa.selenium.WebElement add = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(button));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", add);
        try {
            add.click();
        } catch (RuntimeException covered) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", add);
        }
        try {
            new WebDriverWait(driver, WAIT).pollingEvery(Duration.ofMillis(200))
                    .until(d -> activeGridRowCount() > before);
        } catch (TimeoutException notAdded) {
            throw new IllegalStateException("\"اضافة سطر\" of the active tab added no line (lines: " + before + ").");
        }
        int row = activeGridRowCount() - 1;
        log.info("Active tab: line {} added.", row + 1);
        return row;
    }

    private static final String ACTIVE_TAB_ADD_LINE = "//*[@role='tabpanel' and @data-state='active']"
            + "//button[normalize-space()='اضافة سطر' or normalize-space()='إضافة سطر']";

    /** True when the active tab has an enabled "اضافة سطر" (a locked document has none). */
    public boolean canAddLineInActiveTab() {
        return driver.findElements(By.xpath(ACTIVE_TAB_ADD_LINE)).stream()
                .anyMatch(b -> b.isEnabled() && b.getAttribute("disabled") == null);
    }

    /** Data of every line of the ACTIVE tab's grid (e.g. "الخصومات والإضافات"), in screen order. */
    public List<Map<String, String>> activeTabLines() {
        boolean previous = activeTabGrid;
        useActiveTabGrid(true);
        try {
            List<Map<String, String>> lines = new ArrayList<>();
            int count = activeGridRowCount();
            for (int row = 0; row < count; row++) {
                lines.add(lineData(row));
            }
            return lines;
        } finally {
            useActiveTabGrid(previous);
        }
    }

    private int activeGridRowCount() {
        Object count = ((JavascriptExecutor) driver).executeScript(gridApiJs()
                + "const api=linesGridApi(); return api ? api.getDisplayedRowCount() : 0;");
        return count == null ? 0 : ((Number) count).intValue();
    }

    /**
     * Kind of the editor of cell ({@code row}, {@code colId}): "date" (calendar), "select"
     * (a list to choose from), "number" or "text". The editor is opened to look at it and
     * closed again without a change.
     */
    public String lineEditorKind(int row, String colId) {
        stopCellEditing();
        scrollLineCellIntoView(row, colId);
        ((JavascriptExecutor) driver).executeScript(gridApiJs()
                        + "const api=linesGridApi(); api.startEditingCell({rowIndex:arguments[0], colKey:arguments[1]});",
                row, colId);
        pause(500);
        Object kind = ((JavascriptExecutor) driver).executeScript(
                "const e=document.querySelector('.ag-popup-editor, .ag-cell-inline-editing');"
                        + "const name=arguments[0];"
                        + "if(document.querySelector('.rmdp-calendar') || (e && e.querySelector('.rmdp-container'))) return 'date';"
                        + "if(!e) return /date/i.test(name) ? 'date' : 'text';"
                        + "if(e.querySelector('input.select__input, .ag-rich-select, [class*=select__control]')"
                        + "   || document.querySelector('.ag-rich-select-list, .ag-rich-select-virtual-list-container')) return 'select';"
                        + "const i=e.querySelector('input, textarea');"
                        + "if(i && (i.type==='number' || i.inputMode==='numeric' || i.inputMode==='decimal')) return 'number';"
                        + "if(/(rate|percent|value|amount|qty|quantity|price|cost|total|discount|extra)/i.test(name)) return 'number';"
                        + "return 'text';",
                colId);
        ((JavascriptExecutor) driver).executeScript(gridApiJs()
                + "const api=linesGridApi(); api.stopEditing(true);");
        return String.valueOf(kind);
    }

    /**
     * Opens the list editor of cell ({@code row}, {@code colId}) and clicks a random option;
     * returns what the line holds afterwards ("" when nothing was kept: logged, the save
     * then tells whether the cell was needed).
     *
     * <p>Done like the item cell, which always works: the cell is double-clicked, its React
     * Select input is clicked, the options are read from that select's own list
     * (aria-controls) and one is clicked by script. QC run 2026-11-17 11:55: an option
     * clicked in an editor opened by the grid API was not kept ("accountId" of
     * "الخصومات والإضافات"). The second attempt opens the editor through the grid.</p>
     */
    public String selectRandomInLineCell(int row, String colId) {
        By input = By.cssSelector(".ag-popup-editor input.select__input, .ag-cell-inline-editing input.select__input");
        String chosen = "";
        // QC run 2026-11-17 12:00: the chosen account never showed under "accountId": the grid
        // keeps it in another field. So "kept" = any field of the line changed.
        Map<String, String> before = lineData(row);
        for (int attempt = 1; attempt <= 2; attempt++) {
            stopCellEditing();
            scrollLineCellIntoView(row, colId);
            if (attempt == 1) {
                By cell = By.cssSelector(gridScope() + ".ag-center-cols-container div[role='row'][row-index='" + row
                        + "'] [col-id='" + colId + "']");
                new org.openqa.selenium.interactions.Actions(driver)
                        .doubleClick(new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(cell)))
                        .perform();
            } else {
                ((JavascriptExecutor) driver).executeScript(gridApiJs()
                                + "const api=linesGridApi(); api.startEditingCell({rowIndex:arguments[0], colKey:arguments[1]});",
                        row, colId);
            }
            pause(400);
            // AG Grid rich select ("accountId" of "الخصومات والإضافات", QC 2026-11-17): a click on
            // an option is not kept; the keyboard is: ArrowDown (n+1 times) to the option, then Enter.
            if (appears(By.cssSelector(".ag-rich-select-row"), Duration.ofSeconds(2))) {
                // QC run 2026-11-17 12:12: keys sent by script to document.activeElement were lost
                // in the Selenium browser (the focus was not on the editor). Real keys are sent to
                // the editor's own input.
                List<String> names = driver.findElements(By.cssSelector(".ag-rich-select-row")).stream()
                        .map(r -> r.getAttribute("textContent").trim()).filter(t -> !t.isEmpty()).toList();
                List<org.openqa.selenium.WebElement> inputs = driver.findElements(By.cssSelector(
                        ".ag-cell-inline-editing input, .ag-popup-editor input"));
                if (!names.isEmpty() && !inputs.isEmpty()) {
                    int idx = random.nextInt(names.size());
                    chosen = names.get(idx);
                    try {
                        org.openqa.selenium.WebElement field = inputs.get(0);
                        ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", field);
                        for (int i = 0; i <= idx; i++) {
                            field.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
                            pause(150);
                        }
                        field.sendKeys(org.openqa.selenium.Keys.ENTER);
                    } catch (RuntimeException keysLost) {
                        log.warn("Line {} {}: keys could not be sent ({}).", row + 1, colId, keysLost.getMessage());
                    }
                    try {
                        new WebDriverWait(driver, Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(200))
                                .until(d -> !lineData(row).getOrDefault(colId, "").isBlank());
                        log.info("Line {} {} = {} (holds [{}])", row + 1, colId, chosen, lineData(row).get(colId));
                        return lineData(row).get(colId);
                    } catch (TimeoutException notKept) {
                        log.warn("Line {} {}: [{}] was not kept with the keyboard (attempt {}/2).", row + 1, colId, chosen, attempt);
                        continue;
                    }
                }
            }
            String listbox = null;
            if (appears(input, Duration.ofSeconds(3))) {
                try {
                    org.openqa.selenium.WebElement editor = driver.findElement(input);
                    editor.click();
                    listbox = editor.getAttribute("aria-controls");
                } catch (RuntimeException rerendered) {
                    log.warn("Line {} {}: the list editor re-rendered (attempt {}/2).", row + 1, colId, attempt);
                }
            }
            By options = listbox == null || listbox.isBlank()
                    ? By.cssSelector(".ag-rich-select-row, [class*='select__option']")
                    : By.cssSelector("#" + listbox + " [class*='select__option']");
            List<String> names;
            try {
                names = new WebDriverWait(driver, Duration.ofSeconds(6)).pollingEvery(Duration.ofMillis(200))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            List<String> shown = d.findElements(options).stream().map(o -> o.getText().trim())
                                    .filter(t -> !t.isEmpty()).toList();
                            return shown.isEmpty() ? null : shown;
                        });
            } catch (TimeoutException none) {
                log.warn("Line {} {}: the list shows no option (attempt {}/2).", row + 1, colId, attempt);
                continue;
            }
            chosen = names.get(random.nextInt(names.size()));
            Object clicked = ((JavascriptExecutor) driver).executeScript(
                    "const o=[...document.querySelectorAll(arguments[0])].find(x=>x.textContent.trim()===arguments[1]);"
                            + "if(!o) return false; o.scrollIntoView({block:'nearest'}); o.click(); return true;",
                    listbox == null || listbox.isBlank() ? ".ag-rich-select-row, [class*=select__option]"
                            : "#" + listbox + " [class*=select__option]",
                    chosen);
            if (!Boolean.TRUE.equals(clicked)) {
                continue;
            }
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(200))
                        .until(d -> !lineData(row).equals(before) || !cellText(row, colId).isBlank());
            } catch (TimeoutException notYet) {
                // the editor may keep the value until it is closed
            }
            ((JavascriptExecutor) driver).executeScript(gridApiJs() + "const api=linesGridApi(); if(api) api.stopEditing(false);");
            Map<String, String> after = lineData(row);
            String held = after.getOrDefault(colId, "");
            if (!after.equals(before) || !held.isBlank() || !cellText(row, colId).isBlank()) {
                Map<String, String> changed = new LinkedHashMap<>();
                after.forEach((k, v) -> {
                    if (!v.equals(before.get(k))) {
                        changed.put(k, v);
                    }
                });
                log.info("Line {} {} = {} (fields changed: {})", row + 1, colId, chosen, changed);
                return held.isBlank() ? chosen : held;
            }
            log.warn("Line {} {}: [{}] was not kept (attempt {}/2).", row + 1, colId, chosen, attempt);
        }
        String held = lineData(row).getOrDefault(colId, "");
        log.warn("Line {} {}: no option was kept (last tried [{}]).", row + 1, colId, chosen);
        return held;
    }

    /** Text the grid shows in cell ({@code row}, {@code colId}) of the current grid; "" when not rendered. */
    private String cellText(int row, String colId) {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const c=document.querySelector(arguments[0]); return c ? c.textContent.trim() : '';",
                gridScope() + ".ag-center-cols-container div[role='row'][row-index='" + row + "'] [col-id='" + colId + "']");
        return text == null ? "" : text.toString();
    }

    /** True when {@code locator} is in the page within {@code timeout} (present, not necessarily "visible"). */
    private boolean appears(By locator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout).pollingEvery(Duration.ofMillis(200))
                    .until(ExpectedConditions.presenceOfElementLocated(locator));
            return true;
        } catch (TimeoutException absent) {
            return false;
        }
    }

    // =====================================================================
    // "الاستيراد من Excel" dialog (⋮ → استيراد من Execl), shared by invoices and returns
    // (moved from SalesInvoicePage, QC 2026-11-19).
    // =====================================================================

    public static final String IMPORT_DIALOG_TITLE = "الاستيراد من Excel";
    public static final String IMPORT_TEMPLATE_BUTTON = "تحميل النموذج";

    /** ⋮ item of a new invoice / return (spelt "Execl" in the system). */
    public static final String IMPORT_EXCEL = "استيراد من Execl";

    private static final org.openqa.selenium.By IMPORT_DIALOG = org.openqa.selenium.By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='" + IMPORT_DIALOG_TITLE + "']]");

    /** ⋮ → "استيراد من Execl"; waits for its dialog. */
    public void openImportFromExcel() {
        clickMenuItem(IMPORT_EXCEL);
        new org.openqa.selenium.support.ui.WebDriverWait(driver, WAIT)
                .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(IMPORT_DIALOG));
        log.info("Dialog \"{}\" is open.", IMPORT_DIALOG_TITLE);
    }

    private org.openqa.selenium.WebElement importDialogButton(String text) {
        return driver.findElement(IMPORT_DIALOG).findElements(org.openqa.selenium.By.cssSelector("button")).stream()
                .filter(b -> b.getText().trim().equals(text)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No \"" + text + "\" in \"" + IMPORT_DIALOG_TITLE + "\"."));
    }

    /** Button texts of the import dialog, for checks. */
    public List<String> importDialogButtons() {
        return driver.findElement(IMPORT_DIALOG).findElements(org.openqa.selenium.By.cssSelector("button")).stream()
                .map(b -> b.getText().trim()).filter(t -> !t.isEmpty()).toList();
    }

    /** "تحميل النموذج" → the .xlsx template, downloaded into {@code dir}. */
    public java.nio.file.Path downloadImportTemplate(java.nio.file.Path dir) {
        importDialogButton(IMPORT_TEMPLATE_BUTTON).click();
        return waitForDownloadedFile(dir, ".xlsx", java.time.Duration.ofSeconds(30));
    }

    /** Puts {@code file} in the dialog's file input, then "الاستيراد من Excel"; waits until the dialog closes. */
    public void importExcelFile(java.nio.file.Path file) {
        driver.findElement(IMPORT_DIALOG).findElement(org.openqa.selenium.By.cssSelector("input[type='file']"))
                .sendKeys(file.toAbsolutePath().toString());
        pause(800);
        importDialogButton(IMPORT_DIALOG_TITLE).click();
        new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(30))
                .until(org.openqa.selenium.support.ui.ExpectedConditions.invisibilityOfElementLocated(IMPORT_DIALOG));
        waitForGridToSettle();
        log.info("Imported {}.", file.getFileName());
    }

    // =====================================================================
    // "دفع" dialog (⋮ → دفع), shared by invoices and returns (moved from SalesInvoicePage,
    // QC 2026-11-19). Its fields have the same data-testid as the header (e.g. "<prefix>-paymentMethod"
    // twice), so every lookup is scoped to the dialog. Escape closes the whole dialog.
    // =====================================================================

    /** "دفع" of the actions menu (invoices and returns). */
    public static final String PAY = "دفع";

    /** The open "دفع" dialog: the open dialog that has this screen's طريقة الدفع. */
    private org.openqa.selenium.By payDialogBy() {
        return org.openqa.selenium.By.xpath(
                "//*[@role='dialog' and @data-state='open'][.//*[@data-testid='" + prefix + "-paymentMethod']]");
    }
    private static final org.openqa.selenium.By PAY_OPTION = org.openqa.selenium.By.cssSelector(".select__option");

    /** Waits for the "دفع" dialog; false when it did not open within 15 s. */
    public boolean waitForPayDialog() {
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(15))
                    .until(org.openqa.selenium.support.ui.ExpectedConditions.visibilityOfElementLocated(payDialogBy()));
            pause(500);
            return true;
        } catch (org.openqa.selenium.TimeoutException notOpen) {
            return false;
        }
    }

    public boolean isPayDialogOpen() {
        return driver.findElements(payDialogBy()).stream().anyMatch(org.openqa.selenium.WebElement::isDisplayed);
    }

    private org.openqa.selenium.WebElement payDialog() {
        return driver.findElement(payDialogBy());
    }

    /** Field names (data-testid without "invoices-") of the enabled selects of the dialog, in screen order. */
    public List<String> payDialogSelects() {
        List<String> fields = new java.util.ArrayList<>();
        for (org.openqa.selenium.WebElement box : payDialog().findElements(
                org.openqa.selenium.By.cssSelector("[data-testid^='" + prefix + "-']"))) {
            if (box.findElements(org.openqa.selenium.By.cssSelector(".select__control")).isEmpty()
                    || !box.findElements(org.openqa.selenium.By.cssSelector(".select__control--is-disabled")).isEmpty()) {
                continue;
            }
            String field = box.getAttribute("data-testid").substring(prefix.length() + 1);
            if (!fields.contains(field)) {
                fields.add(field);
            }
        }
        return fields;
    }

    /** Text the dialog's select {@code field} shows ("" when empty: the placeholder "اختر" is not a value). */
    public String payDialogSelectedText(String field) {
        List<org.openqa.selenium.WebElement> value = payDialog().findElements(org.openqa.selenium.By.cssSelector(
                "[data-testid='" + prefix + "-" + field + "'] .select__single-value"));
        return value.isEmpty() ? "" : value.get(0).getText().trim();
    }

    /**
     * Opens the dialog's select {@code field} with a real click and chooses an option
     * picked by {@code chooser} from the offered ones (never Escape: it closes the dialog).
     *
     * @return the chosen option; "" when the field offers none.
     */
    public String payDialogSelect(String field, java.util.function.Function<List<String>, String> chooser) {
        org.openqa.selenium.By control = org.openqa.selenium.By.cssSelector(
                "[data-testid='" + prefix + "-" + field + "'] .select__control");
        for (int attempt = 1; attempt <= 3; attempt++) {
            org.openqa.selenium.WebElement box = payDialog().findElement(control);
            ((org.openqa.selenium.JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", box);
            box.click();
            List<String> options = payOptions();
            if (options.isEmpty()) {
                log.warn("Pay dialog {}: no options (attempt {}/3).", field, attempt);
                payDialog().findElement(control).click(); // closes the empty menu
                pause(800);
                continue;
            }
            String chosen = chooser.apply(options);
            for (org.openqa.selenium.WebElement option : driver.findElements(PAY_OPTION)) {
                if (option.isDisplayed() && option.getText().trim().equals(chosen)) {
                    option.click();
                    break;
                }
            }
            pause(600);
            String shown = payDialogSelectedText(field);
            if (shown.equals(chosen.split("\\R", 2)[0].trim())) {
                log.info("Pay dialog {} = {}", field, shown);
                return shown;
            }
            log.warn("Pay dialog {}: [{}] clicked but it shows [{}] (attempt {}/3).", field, chosen, shown, attempt);
        }
        log.warn("Pay dialog {}: nothing could be chosen.", field);
        return "";
    }

    public String payDialogSelectRandom(String field) {
        return payDialogSelect(field, options -> options.get(new java.util.Random().nextInt(options.size())));
    }

    private List<String> payOptions() {
        try {
            return new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(4))
                    .pollingEvery(java.time.Duration.ofMillis(200))
                    .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                    .until(d -> {
                        List<String> texts = d.findElements(PAY_OPTION).stream()
                                .filter(org.openqa.selenium.WebElement::isDisplayed)
                                .map(o -> o.getText().trim()).filter(t -> !t.isEmpty()).toList();
                        return texts.isEmpty() ? null : texts;
                    });
        } catch (org.openqa.selenium.TimeoutException none) {
            return List.of();
        }
    }

    /** Ids of the enabled text inputs of the dialog (selects excluded), in screen order. */
    public List<String> payDialogTextInputs() {
        return payDialog().findElements(org.openqa.selenium.By.cssSelector("input[id]:not([type='hidden']), textarea[id]")).stream()
                .filter(i -> i.isDisplayed() && i.isEnabled() && i.getAttribute("readonly") == null)
                .filter(i -> i.findElements(org.openqa.selenium.By.xpath("./ancestor::*[contains(@class,'react-select')]")).isEmpty()
                        && !String.valueOf(i.getAttribute("class")).contains("select__input"))
                .map(i -> i.getAttribute("id")).distinct().toList();
    }

    public String payDialogTextValue(String id) {
        List<org.openqa.selenium.WebElement> input = payDialog().findElements(org.openqa.selenium.By.id(id));
        return input.isEmpty() ? "" : String.valueOf(input.get(0).getAttribute("value"));
    }

    /** Sets the dialog's input {@code #id} (React setter + events, typed when not kept). */
    public void payDialogSetText(String id, String text) {
        org.openqa.selenium.WebElement input = payDialog().findElement(org.openqa.selenium.By.id(id));
        String setter = "textarea".equalsIgnoreCase(input.getTagName()) ? "HTMLTextAreaElement" : "HTMLInputElement";
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const i=arguments[0];"
                        + "Object.getOwnPropertyDescriptor(window[arguments[2]].prototype,'value').set.call(i, arguments[1]);"
                        + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('change',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('blur',{bubbles:true}));",
                input, text, setter);
        pause(400);
        if (!text.equals(payDialogTextValue(id))) {
            input = payDialog().findElement(org.openqa.selenium.By.id(id));
            input.click();
            input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.DELETE);
            input.sendKeys(text);
            pause(400);
        }
        log.info("Pay dialog {} = {} (shows [{}])", id, text, payDialogTextValue(id));
    }

    /**
     * Clicks the dialog's "دفع" and waits up to 20 s for the dialog to close.
     *
     * @return the toasts shown meanwhile (the server's answer).
     */
    public List<String> submitPayDialog() {
        payDialog().findElements(org.openqa.selenium.By.cssSelector("button")).stream()
                .filter(b -> b.getText().trim().equals(PAY)).findFirst()
                .orElseThrow(() -> new IllegalStateException("No \"دفع\" button in the pay dialog."))
                .click();
        java.util.Set<String> toasts = new java.util.LinkedHashSet<>();
        long end = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < end) {
            toasts.addAll(otherToastTexts("#no-filter#", java.time.Duration.ofMillis(300)));
            if (!isPayDialogOpen()) {
                toasts.addAll(otherToastTexts("#no-filter#", java.time.Duration.ofSeconds(2)));
                break;
            }
        }
        log.info("Pay dialog submitted → dialog {}, toasts {}", isPayDialogOpen() ? "still open" : "closed", toasts);
        return new java.util.ArrayList<>(toasts);
    }

    /** Closes the dialog with its "Close" button (nothing is paid). */
    public void closePayDialog() {
        if (isPayDialogOpen()) {
            payDialog().findElements(org.openqa.selenium.By.cssSelector("button")).stream()
                    .filter(b -> "Close".equals(b.getText().trim()) || "Close".equals(b.getAttribute("aria-label")))
                    .findFirst().ifPresent(org.openqa.selenium.WebElement::click);
            pause(500);
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ----- API record filters -----

    public String codeOf(Map<String, Object> record) {
        return textOf(record, "invoiceCode");
    }

    /** {@code invoiceStatus} 1 = معتمد (every record in QC on 2026-11-12). */
    public static boolean isApproved(Map<String, Object> record) {
        return intOf(record, "invoiceStatus") == 1;
    }
}
