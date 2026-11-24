package com.erp.automation.pages.sales;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.List;
import java.util.Map;

/**
 * "طلبات تعديل الأسعار" (المبيعات → العمليات → طلبات تعديل الأسعار):
 * {@code /sales/pricing-request}.
 *
 * <p>data-testid prefix {@code pricing-request}; API
 * {@code POST /api/SalePriceOperationRequestV/filter}, one request
 * {@code GET /api/SalePriceOperationRequestV/<id>}. Same form as "التسعير"
 * (see {@link PricingOperationPage}) plus a line column "موافقة"
 * ({@code isApproved}, a check box) in view mode.</p>
 *
 * <p>QC 2026-11-23 (checked by hand):</p>
 * <ul>
 *     <li>list columns: التاريخ، الفرع، سياسة التعديل، التطبيق على، فئات
 *     الأصناف، الأصناف، أساس المقارنة، الحالة، هامش الربح، هامش الربح قيمة،
 *     نسبة هامش الربح — <b>no number column</b> ("#" is the row index), so
 *     rows are opened by their position. Row ⋯ has only "عرض";</li>
 *     <li>the first row (code 1, معتمد) still opens {@code /view/0} with an
 *     EMPTY form and status "ملغي";</li>
 *     <li>new request: number shown, today's date, "القائمة الافتراضية"
 *     preselected; toolbar add/undo/delete/save/print/approve/log disabled;
 *     "⋮": الاستيراد من Excel / التصدير الى Excel;</li>
 *     <li>saving without lines: error "يجب اضافة صنف واحد على الأقل", nothing
 *     saved, stays on {@code /add};</li>
 *     <li>save: "هل انت متاكد من عملية الحفظ؟" → نعم → مسودة. "البيان" is
 *     NOT kept (empty after save and in the API, request 17);</li>
 *     <li>approve: "هل تريد الاعتماد ؟" → موافق → refused with "لا يمكن
 *     الاعتماد، توجد أصناف لم يتم تحديد موافقة أو رفض لها" while a line has
 *     no decision;</li>
 *     <li>line decisions (QC 2026-11-23, request 19 تلقائي and request 17
 *     يدوي): edit mode shows the line columns "موافقة" ({@code isApproved}),
 *     "رفض" ({@code isRejected}) and "سبب الرفض" ({@code rejectReason}) for
 *     both entry modes. They are the last columns of the grid and are rendered
 *     only when the grid is scrolled to them (scrollLeft −100000 in RTL).
 *     Ticking one box disables the other on that line. Saved, then "اعتماد"
 *     → معتمد. "رفض" without a reason was accepted (request 17);</li>
 *     <li>approved: edit/delete/approve disabled, add enabled;</li>
 *     <li>Kanban: one card per request of the list page, in the list order (card 2 opened
 *     request 2). Every card shows only "الريال السعودي" and the badge "غير معتمد", while the
 *     list page shows 9 معتمد and 1 مسودة.</li>
 * </ul>
 */
public class PriceChangeRequestPage extends PricingOperationPage {

    public static final String PREFIX = "pricing-request";
    public static final String SCREEN_PATH = "/sales/pricing-request";
    public static final String API_RESOURCE = "SalePriceOperationRequestV";

    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";
    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";

    /** Line column "موافقة". */
    public static final String LINE_APPROVAL_COLUMN = "isApproved";

    /** Line column "رفض" (a check box). */
    public static final String LINE_REJECT_COLUMN = "isRejected";

    /** Line column "سبب الرفض" (text). */
    public static final String LINE_REJECT_REASON_COLUMN = "rejectReason";

    /** Part of the error shown when a request is approved while a line has no decision. */
    public static final String LINE_DECISION_REQUIRED = "لم يتم تحديد موافقة أو رفض";

    /** "أساس المقارنة" whose lines accept only items priced in the chosen price list. */
    public static final String BASE_SELECTED_PRICE_LIST = "قائمة الأسعار المحددة";

    /** Error when an item that is not priced in the chosen list is put on a line (the line is removed). */
    public static final String ITEM_NOT_PRICED = "هذا الصنف غير مسعر";

    /** Error of "انزال" when the chosen price list has none of the chosen items priced. */
    public static final String NOTHING_TO_LOAD = "لا يوجد أصناف لإنزالها";

    public PriceChangeRequestPage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE);
    }

    /** Number of rows on the current list page. */
    public int listRowCount() {
        waitForListLoaded();
        return visibleRowCount();
    }

    /** Text of column {@code colId} of list row {@code rowIndex}. */
    public String listCell(int rowIndex, String colId) {
        List<WebElement> cells = driver.findElements(By.cssSelector(
                "div[role='row'][row-index='" + rowIndex + "'] [col-id='" + colId + "']"));
        return cells.isEmpty() ? "" : cells.get(0).getText().trim();
    }

    /** Double-click on list row {@code rowIndex} (the list has no number to search by). */
    public void openListRow(int rowIndex) {
        waitForListLoaded();
        WebElement cell = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-index='" + rowIndex + "'] [col-id='priceOperationDate']")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", cell);
        new Actions(driver).doubleClick(cell).perform();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("/view/"));
    }

    /**
     * List row {@code rowIndex} → "إجراءات" (⋯) → "عرض"; waits for the request to open. By position:
     * the list has no number column. Only the link navigates (see SalesDocumentPage#openFromRowActions).
     */
    public void openListRowFromActions(int rowIndex) {
        waitForListLoaded();
        By button = By.cssSelector("div[role='row'][row-index='" + rowIndex + "'] [col-id='actions' i] button");
        WebElement menu = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(button));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", menu);
        new Actions(driver).moveToElement(menu).click().perform();
        By viewLink = By.xpath("//*[@role='menu' and @data-state='open']//*[@role='menuitem']//a[normalize-space()='عرض']");
        By viewItem = By.xpath("//*[@role='menu' and @data-state='open']//*[@role='menuitem'][normalize-space()='عرض']");
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(viewItem));
        ui.clickWhenReady(driver.findElements(viewLink).isEmpty() ? viewItem : viewLink);
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("/view/"));
    }

    /** "أساس المقارنة" options whose value an item must have to be accepted on a line. */
    public static final String BASE_COST = "التكلفة";
    public static final String BASE_LAST_PURCHASE_PRICE = "آخر سعر شراء";

    /** The first 200 items with what the comparison bases need; read once per run (≈ 8 s). */
    private static volatile List<Map<String, Object>> itemFacts;

    /**
     * Items a line accepts with "أساس المقارنة" = {@code base} and price list {@code priceListName}.
     * A line refuses an item that has no value for the base ("هذا الصنف غير مسعر", the line is
     * removed) — QC 2026-11-23: قائمة الأسعار المحددة + item فانتا (not priced in the list), آخر سعر
     * شراء + item 37 (no purchase price). Read from {@code Item/ByItemCode}, the call the form uses:
     * <ul>
     *     <li>قائمة الأسعار المحددة → {@code uoms[].priceLists} has the list (that day only
     *     القائمة الافتراضية and مجموعه مصر (8, 10, 11) priced any item);</li>
     *     <li>آخر سعر شراء → {@code lastPurchasePrice} &gt; 0;</li>
     *     <li>التكلفة → {@code uoms[].avgCost} &gt; 0;</li>
     *     <li>سعر البيع الافتراضي → priced in القائمة الافتراضية.</li>
     * </ul>
     *
     * @return maps with "code" and "name".
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> itemsAcceptedFor(String base, String priceListName) {
        java.util.function.Predicate<Map<String, Object>> accepted = switch (base) {
            case BASE_SELECTED_PRICE_LIST -> i -> ((List<Object>) i.get("lists")).contains(priceListName);
            case BASE_LAST_PURCHASE_PRICE -> i -> ((Number) i.get("lastPurchase")).doubleValue() > 0;
            case BASE_COST -> i -> ((Number) i.get("cost")).doubleValue() > 0;
            default -> i -> ((List<Object>) i.get("lists")).contains("القائمة الافتراضية");
        };
        return readItemFacts().stream().filter(accepted).toList();
    }

    /** Items priced in price list {@code priceListName} (see {@link #itemsAcceptedFor}). */
    public List<Map<String, Object>> itemsPricedIn(String priceListName) {
        return itemsAcceptedFor(BASE_SELECTED_PRICE_LIST, priceListName);
    }

    @SuppressWarnings("unchecked")
    private synchronized List<Map<String, Object>> readItemFacts() {
        if (itemFacts != null) {
            return itemFacts;
        }
        Object result = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[0];"
                        + "const cookie = document.cookie.split(';').map(c => c.trim()).find(c => c.startsWith('authToken='));"
                        + "if (!cookie) { done('ERROR: no authToken cookie'); return; }"
                        + "const token = decodeURIComponent(cookie.substring('authToken='.length));"
                        + "const m = performance.getEntriesByType('resource').map(e => e.name)"
                        + "  .map(n => n.match(/^(https?:\\/\\/[^/]+\\/api)\\//)).find(x => x);"
                        + "if (!m) { done('ERROR: API host not found'); return; }"
                        + "const get = u => fetch(m[1] + '/' + u, {headers: {Authorization: 'Bearer ' + token}})"
                        + "  .then(r => r.ok ? r.json() : Promise.reject('HTTP ' + r.status + ' ' + u));"
                        + "(async () => {"
                        + "  const names = {}; ((await get('SalePriceList/Lookup')).data || []).forEach(l => names[l.id] = l.lookupName);"
                        + "  const found = await get('Item/lookupAutoComplete?moduleType=2&searchValue=&showVariants=true&skip=0&take=200&typeOfSearch=2');"
                        + "  const items = (found.data && (found.data.data || found.data)) || [];"
                        + "  const out = [];"
                        + "  for (let i = 0; i < items.length; i += 10) {"
                        + "    const batch = await Promise.all(items.slice(i, i + 10).map(it =>"
                        + "      get('Item/ByItemCode?branchId=0&checkStopItem=false&needMaster=true&onlyBarcode=false&priceListId=1'"
                        + "        + '&searchValue=' + encodeURIComponent(it.itemBarCode || it.code)"
                        + "        + '&showVariants=true&warehouseId=0').then(r => r.data).catch(() => null)));"
                        + "    batch.forEach(d => { if (!d) return; const uoms = d.uoms || [];"
                        + "      out.push({code: String(d.code), name: d.itemName,"
                        + "        lists: [...new Set(uoms.flatMap(u => (u.priceLists || []).map(p => names[p.priceListId]).filter(n => n)))],"
                        + "        lastPurchase: Number(d.lastPurchasePrice || 0),"
                        + "        cost: Math.max(0, ...uoms.map(u => Number(u.avgCost || 0)))}); });"
                        + "  }"
                        + "  done(out);"
                        + "})().catch(e => done('ERROR: ' + e));");
        if (result instanceof String error) {
            throw new IllegalStateException("Reading the items through the API failed: " + error);
        }
        itemFacts = new java.util.ArrayList<>((List<Map<String, Object>>) result);
        log.info("Items read through the API: {}", itemFacts.size());
        return itemFacts;
    }

    /** Names of the price lists in "قوائم الأسعار" (from the API lookup the form uses). */
    @SuppressWarnings("unchecked")
    public List<String> priceListNames() {
        Object result = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done = arguments[0];"
                        + "const cookie = document.cookie.split(';').map(c => c.trim()).find(c => c.startsWith('authToken='));"
                        + "const token = decodeURIComponent(cookie.substring('authToken='.length));"
                        + "const m = performance.getEntriesByType('resource').map(e => e.name)"
                        + "  .map(n => n.match(/^(https?:\\/\\/[^/]+\\/api)\\//)).find(x => x);"
                        + "fetch(m[1] + '/SalePriceList/Lookup', {headers: {Authorization: 'Bearer ' + token}})"
                        + "  .then(r => r.json()).then(j => done((j.data || []).map(l => l.lookupName)))"
                        + "  .catch(e => done('ERROR: ' + e));");
        if (result instanceof String error) {
            throw new IllegalStateException("Reading the price lists failed: " + error);
        }
        return new java.util.ArrayList<>((List<String>) result);
    }

    /**
     * "الأصناف" (multi select): chooses the items {@code labels} ("code - name"); returns the labels
     * the field shows. Opened with the keyboard, like selectRandomItems(): a click on the field hits
     * the × of an item chosen before and removes it (QC 2026-11-22, and run 2026-11-23 17:00 with
     * selectOption: 2 items chosen, 1 loaded).
     */
    public List<String> chooseItems(List<String> labels) {
        By input = By.cssSelector("[data-testid='" + PREFIX + "-items'] input");
        By options = By.cssSelector("div[class*='select__option']:not([class*='is-disabled'])");
        for (String label : labels) {
            WebElement field = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(input));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", field);
            field.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
            WebElement option = new WebDriverWait(driver, WAIT).until(d -> d.findElements(options).stream()
                    .filter(o -> o.getText().trim().equals(label)).findFirst().orElse(null));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'nearest'});", option);
            option.click();
            new WebDriverWait(driver, WAIT).until(d -> chosenItems().contains(label));
            new Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
        }
        List<String> chosen = chosenItems();
        log.info("الأصناف = {}", chosen);
        return chosen;
    }

    /** States of the "موافقة" check boxes shown in the grid ("checked" / "unchecked"). */
    public List<String> lineApprovalStates() {
        return driver.findElements(By.cssSelector(
                        ".ag-center-cols-container div[role='row'][row-index] [col-id='" + LINE_APPROVAL_COLUMN + "'] [role='checkbox']"))
                .stream().map(c -> c.getAttribute("data-state")).toList();
    }

    // ----- line decisions: "موافقة" / "رفض" / "سبب الرفض" -----

    /**
     * Scrolls the lines grid sideways until cell {@code colId} of line
     * {@code row} is rendered (AG Grid renders only the columns in view; the
     * decision columns are the last ones, at the far left in RTL).
     *
     * @return false when the grid has no such column (e.g. "رفض" in view mode).
     */
    public boolean revealLineColumn(int row, String colId) {
        ensureLineRendered(row);
        By cell = lineGridCell(row, colId);
        JavascriptExecutor js = (JavascriptExecutor) driver;
        for (long scrollLeft : new long[]{Long.MIN_VALUE, -100_000, 100_000, 0}) {
            if (scrollLeft != Long.MIN_VALUE) {
                js.executeScript("for (const sel of ['.ag-center-cols-viewport', '.ag-body-horizontal-scroll-viewport']) {"
                        + "  for (const el of document.querySelectorAll(sel)) {"
                        + "    if (el.offsetParent === null) continue;"
                        + "    el.scrollLeft = arguments[0]; el.dispatchEvent(new Event('scroll', {bubbles: true}));"
                        + "  }"
                        + "}", scrollLeft);
            }
            try {
                new WebDriverWait(driver, java.time.Duration.ofMillis(1500))
                        .until(ExpectedConditions.presenceOfElementLocated(cell));
                return true;
            } catch (org.openqa.selenium.TimeoutException notRendered) {
                // try the next side
            }
        }
        return false;
    }

    /** The check box of {@code colId} ("isApproved" / "isRejected") in line {@code row}. */
    private WebElement lineCheckBox(int row, String colId) {
        if (!revealLineColumn(row, colId)) {
            throw new org.openqa.selenium.NoSuchElementException("Line " + (row + 1) + " has no column [" + colId + "].");
        }
        return driver.findElement(By.cssSelector(lineGridCellCss(row, colId) + " [role='checkbox']"));
    }

    /** "checked" / "unchecked" of check box {@code colId} in line {@code row}. */
    public String lineCheckState(int row, String colId) {
        return lineCheckBox(row, colId).getAttribute("data-state");
    }

    /** Whether check box {@code colId} of line {@code row} can be clicked. */
    public boolean isLineCheckEnabled(int row, String colId) {
        WebElement box = lineCheckBox(row, colId);
        return box.isEnabled() && box.getAttribute("data-disabled") == null;
    }

    /** Ticks (or clears) check box {@code colId} of line {@code row}; waits until it shows the new state. */
    public void setLineCheck(int row, String colId, boolean on) {
        String wanted = on ? "checked" : "unchecked";
        if (wanted.equals(lineCheckState(row, colId))) {
            return;
        }
        WebElement box = lineCheckBox(row, colId);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", box);
        try {
            box.click();
        } catch (RuntimeException covered) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", box);
        }
        new WebDriverWait(driver, WAIT)
                .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                .until(d -> wanted.equals(lineCheckState(row, colId)));
    }

    /** "موافقة" on line {@code row}. */
    public void approveLine(int row) {
        setLineCheck(row, LINE_APPROVAL_COLUMN, true);
    }

    /** "رفض" on line {@code row} with "سبب الرفض" = {@code reason}. */
    public void rejectLine(int row, String reason) {
        setLineCheck(row, LINE_REJECT_COLUMN, true);
        revealLineColumn(row, LINE_REJECT_REASON_COLUMN);
        setLineText(row, LINE_REJECT_REASON_COLUMN, reason);
    }

    private static String lineGridCellCss(int row, String colId) {
        return ".ag-center-cols-container div[role='row'][row-index='" + row + "'] div[role='gridcell'][col-id='" + colId + "']";
    }

    private static By lineGridCell(int row, String colId) {
        return By.cssSelector(lineGridCellCss(row, colId));
    }

    /**
     * Lines of the open request as the API returns them ({@code itemCode},
     * {@code isApproved}, {@code isRejected} …), read with the same GET the
     * page used. The "موافقة" cells are not read from the grid: the column is
     * virtualised off-screen in a narrow window.
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> apiLinesOfOpenRequest() {
        String id = driver.getCurrentUrl().replaceAll(".*/(?:view|edit)/(\\d+).*", "$1");
        String suffix = "/" + API_RESOURCE + "/" + id;
        String url = api().apiUrlsUsedByPage(suffix).stream()
                .filter(u -> u.endsWith(suffix))
                .reduce((first, last) -> last)
                .orElseThrow(() -> new IllegalStateException("The page did not read request " + id + " through the API."));
        Map<String, Object> json = (Map<String, Object>) api().getJson(url);
        Map<String, Object> data = (Map<String, Object>) json.get("data");
        Object lines = data == null ? null : data.get("saleItemPriceOperations");
        return lines == null ? List.of() : (List<Map<String, Object>>) lines;
    }
}
