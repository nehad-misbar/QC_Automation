package com.erp.automation.pages.sales;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;

/**
 * Shared page object of "التسعير" and "طلبات تعديل الأسعار": both use the same
 * pricing form (QC 2026-11-12).
 *
 * <ul>
 *     <li>number {@code #priceOperationCode} (a plain number: 64, 65 …);
 *     "البيان" is the input with data-testid {@code <prefix>-statement} (no id);</li>
 *     <li>header: الرمز، التاريخ (today, locked)، الفرع، قوائم الأسعار*، البيان،
 *     آلية إدخال الأصناف (يدوي / تلقائي)، أساس المقارنة* (قائمة الأسعار
 *     المحددة / التكلفة / سعر البيع الافتراضي / آخر سعر شراء)، التطبيق على
 *     (الأصناف / فئات الأصناف)، الأصناف (multi select)، سياسة التعديل (السعر
 *     شامل التعديل / استعراض التعديل والسعر قبل وبعد التعديل)، هامش الربح
 *     (نسبة / قيمة)، تسعير الأصناف (الكل / المسعرة / الغير مسعرة)، التعامل
 *     بكافة الوحدات (switch)، طريقة التطبيق* (يدوي / تخفيض / زيادة / معادلة)،
 *     انزال من + الرمز;</li>
 *     <li>"يدوي": "اضافة سطر" adds item lines; "تلقائي": التطبيق على، الأصناف،
 *     تسعير الأصناف and انزال من become editable and "انزال" loads the chosen
 *     items (it is enabled once an item is chosen);</li>
 *     <li>طريقة التطبيق: زيادة → نوع الزيادة ({@code typeOfExtra}) + النسبة
 *     ({@code extraRate}); تخفيض → نوع التخفيض ({@code typeOfDiscount}) +
 *     النسبة ({@code discountRate}); معادلة → خصم ({@code formulaDiscountRate})
 *     + مبلغ إضافي ({@code formulaExtraValue}). زيادة 10% turned 20 → 22 and
 *     100 → 110 in "السعر بعد التعديل";</li>
 *     <li>lines: رمز الصنف ({@code itemCode})، اسم الصنف، الباركود، رقم صنف
 *     المورد، الوحدة، العبوة، التكلفة ({@code avgCost})، السعر بعد التعديل
 *     ({@code priceAfter})، الفارق ({@code difference} = price − cost)، الضريبة
 *     ({@code taxRate} "15%")، السعر شامل الضريبة ({@code priceAfterTax})، نسبة
 *     هامش الربح ({@code profitMarginRatio} "−77.777778 %")، هامش الربح قيمة;</li>
 *     <li>save: "هل انت متاكد من عملية الحفظ؟" → نعم; when a price is below its
 *     cost a second dialog "أصناف الفارق فيها بالسالب" lists those items with
 *     "نعم، متأكد من الحفظ" (data-modal-agree) / "لا، مراجعة الأصناف"; saved as مسودة;</li>
 *     <li>approve: "هل تريد الاعتماد ؟" → موافق → معتمد. The first approval seen
 *     asked "توجد أصناف مسعرة مسبقا ... ما هو الإجراء الذي تريد تطبيقه؟" with
 *     المتابعة في الإعتماد / تجاهل الأصناف المسعرة / إنشاء طلب تعديل الأسعار;
 *     the second one (same items, same list) did not;</li>
 *     <li>Approved: edit, delete and approve disabled (no disapprove, no cancel).</li>
 *     <li>QC 2026-11-19: save without lines → toast "يجب اضافة صنف واحد على الأقل"
 *     (no confirmation); only "طريقة التطبيق" has a clear (×) among the required
 *     selects, and clearing it makes save show "هذا الحقل مطلوب"; تخفيض النسبة
 *     accepts 0–100 (100 → price 0), 101 shows "يجب أن يكون أقل من أو يساوي 100"
 *     and blocks save, a minus sign is not accepted; delete asks "هل انت متاكد من
 *     الحذف؟" (حذف / إلغاء); closing with changes asks "هل انت متأكد من تجاهل
 *     التغييرات التي اجريتها؟" (نعم / إلغاء).</li>
 * </ul>
 */
public abstract class PricingOperationPage extends SalesDocumentPage {

    public static final String CODE_FIELD = "priceOperationCode";

    public static final String ITEM_CODE_COLUMN = "itemCode";
    public static final String PRICE_COLUMN = "priceAfter";
    public static final String COST_COLUMN = "avgCost";
    public static final String DIFFERENCE_COLUMN = "difference";
    public static final String TAX_COLUMN = "taxRate";
    public static final String PRICE_WITH_TAX_COLUMN = "priceAfterTax";
    public static final String MARGIN_RATIO_COLUMN = "profitMarginRatio";
    public static final String MARGIN_VALUE_COLUMN = "profitMarginValue";

    public static final String ENTRY_MANUAL = "يدوي";
    public static final String ENTRY_AUTOMATIC = "تلقائي";

    public static final String METHOD_MANUAL = "يدوي";
    public static final String METHOD_DISCOUNT = "تخفيض";
    public static final String METHOD_INCREASE = "زيادة";
    public static final String METHOD_FORMULA = "معادلة";

    public static final List<String> BASE_OF_COMPARISON_OPTIONS = List.of(
            "قائمة الأسعار المحددة", "التكلفة", "سعر البيع الافتراضي", "آخر سعر شراء");
    public static final List<String> APPLICATION_METHOD_OPTIONS = List.of(
            METHOD_MANUAL, METHOD_DISCOUNT, METHOD_INCREASE, METHOD_FORMULA);
    public static final List<String> CHANGE_POLICY_OPTIONS = List.of(
            "السعر شامل التعديل", "استعراض التعديل والسعر قبل وبعد التعديل");

    /** Fields that "تلقائي" makes editable. */
    public static final List<String> AUTOMATIC_ENTRY_FIELDS = List.of("applyOn", "items", "downPricingType", "downOpType");

    /** Second save dialog when a price is below its cost. */
    public static final String NEGATIVE_DIFFERENCE_TITLE = "أصناف الفارق فيها بالسالب";
    /**
     * Save dialog shown BEFORE the negative-difference one when some lines kept
     * their price ("يوجد أصناف لم يتم تعديل سعرها بعد. هل تريد الحفظ دون
     * تسعيرها؟": نعم، حفظ / لا، مراجعة الأصناف; QC 2026-11-22).
     */
    public static final String UNPRICED_ITEMS_TITLE = "أصناف بدون تسعير";
    public static final String REVIEW_ITEMS_BUTTON = "لا، مراجعة الأصناف";

    /** Approval dialog when items are already priced, and its choices. */
    public static final String ALREADY_PRICED_TEXT = "توجد أصناف مسعرة مسبقا";
    public static final String APPROVE_CONTINUE = "المتابعة في الإعتماد";
    public static final String APPROVE_SKIP_PRICED = "تجاهل الأصناف المسعرة";
    public static final String APPROVE_CREATE_REQUEST = "إنشاء طلب تعديل الأسعار";

    /** Messages and dialog texts (QC 2026-11-19). */
    public static final String NO_LINES_MESSAGE = "يجب اضافة صنف واحد على الأقل";
    public static final String REQUIRED_MESSAGE = "هذا الحقل مطلوب";
    public static final String RATE_MAX_MESSAGE = "يجب أن يكون أقل من أو يساوي 100";
    public static final String DELETE_QUESTION = "هل انت متاكد من الحذف";
    public static final String DISCARD_QUESTION = "هل انت متأكد من تجاهل التغييرات";
    public static final String DIALOG_CANCEL = "إلغاء";

    /**
     * "هامش الربح" (QC 2026-11-19): empty by default; قيمة shows "القيمة*"
     * ({@code profitMarginValue}, 0 counts as empty: save shows "هذا الحقل
     * مطلوب"), نسبة shows {@code profitMarginRate} (max 100). API:
     * typeOfProfitMargin 1 = قيمة. The margin does not change the line prices.
     */
    public static final String PROFIT_MARGIN_TYPE = "typeOfProfitMargin";
    public static final String MARGIN_BY_VALUE = "قيمة";
    public static final String MARGIN_BY_RATE = "نسبة";
    public static final String MARGIN_VALUE_FIELD = "profitMarginValue";
    public static final String MARGIN_RATE_FIELD = "profitMarginRate";

    /**
     * Editable line cells (QC 2026-11-22): رمز الصنف (itemCode), السعر بعد
     * التعديل (priceAfter) and السعر شامل الضريبة (priceAfterTax); the price
     * stays editable whatever "طريقة التطبيق" is. Rate fields keep their default
     * type نسبة ({@code typeOfExtra} / {@code typeOfDiscount}).
     */
    public static final String RATE_TYPE_PERCENT = "نسبة";

    private static final By LOAD_ITEMS_BUTTON = By.xpath("//main//button[normalize-space()='انزال']");

    protected PricingOperationPage(WebDriver driver, String prefix, String screenPath, String apiResource) {
        super(driver, prefix, screenPath, apiResource, CODE_FIELD);
        this.noteField = "statement";
    }

    /** The item code of the lines is "itemCode" (not "code"). */
    @Override
    protected String lineCodeColumn() {
        return ITEM_CODE_COLUMN;
    }

    // ----- header -----

    /** "آلية إدخال الأصناف" = {@code entry} (يدوي / تلقائي). */
    public void setItemEntry(String entry) {
        selectOption("downTypeId", entry);
        confirmDialogIfShown(Duration.ofMillis(800));
    }

    /** "طريقة التطبيق" = {@code method}. */
    public void setApplicationMethod(String method) {
        selectOption("applicationMethod", method);
        confirmDialogIfShown(Duration.ofMillis(800));
    }

    /** Number input with data-testid {@code <prefix>-<field>} (extraRate, discountRate, formula…). */
    public boolean setNumberField(String field, String value) {
        return setTextInput(field, value);
    }

    /** "التعامل بكافة الوحدات" (switch {@code downAllUOM}): true when on. */
    public boolean isAllUnits() {
        return driver.findElements(By.cssSelector("[data-testid='" + prefix + "-downAllUOM']")).stream()
                .anyMatch(e -> "checked".equals(e.getAttribute("data-state")));
    }

    /** Turns "التعامل بكافة الوحدات" on or off (off by default, QC 2026-11-22). */
    public void setAllUnits(boolean on) {
        if (isAllUnits() != on) {
            driver.findElement(By.cssSelector("[data-testid='" + prefix + "-downAllUOM']")).click();
            new WebDriverWait(driver, WAIT).until(d -> isAllUnits() == on);
        }
        log.info("التعامل بكافة الوحدات = {}", on);
    }

    /**
     * "الأصناف" (multi select): {@code count} different random items; returns
     * the labels the field shows afterwards.
     *
     * <p>Opened with the keyboard, not a click: QC 2026-11-22, the click on the
     * middle of the field hit the × of the item chosen before and removed it
     * (2 items chosen, 1 left, 1 line loaded).</p>
     */
    public List<String> selectRandomItems(int count) {
        By input = By.cssSelector("[data-testid='" + prefix + "-items'] input");
        By options = By.cssSelector("div[class*='select__option']:not([class*='is-disabled'])");
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < count; i++) {
            List<String> already = chosenItems();
            WebElement field = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(input));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'}); arguments[0].focus();", field);
            field.sendKeys(org.openqa.selenium.Keys.ARROW_DOWN);
            List<WebElement> offered = new WebDriverWait(driver, WAIT).until(d -> {
                List<WebElement> free = d.findElements(options).stream()
                        .filter(o -> !already.contains(o.getText().trim())).toList();
                return free.isEmpty() ? null : free;
            });
            WebElement option = offered.get(random.nextInt(offered.size()));
            String label = option.getText().trim();
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'nearest'});", option);
            option.click();
            new WebDriverWait(driver, WAIT).until(d -> chosenItems().contains(label));
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
        }
        List<String> chosen = chosenItems();
        log.info("الأصناف = {}", chosen);
        return chosen;
    }

    /** Labels of the items chosen in "الأصناف". */
    public List<String> chosenItems() {
        return driver.findElements(By.cssSelector("[data-testid='" + prefix + "-items'] [class*='multi-value__label']"))
                .stream().map(e -> e.getText().trim()).toList();
    }

    public boolean isLoadItemsEnabled() {
        List<WebElement> buttons = driver.findElements(LOAD_ITEMS_BUTTON);
        return !buttons.isEmpty() && buttons.get(0).isEnabled();
    }

    /** "انزال": loads the chosen items into the lines. */
    public void loadItems() {
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.elementToBeClickable(LOAD_ITEMS_BUTTON)).click();
        confirmDialogIfShown(Duration.ofSeconds(1));
        ui.waitForGridToSettle();
    }

    /** True when the "اضافة سطر" button is shown (manual entry). */
    public boolean isAddLineShown() {
        return !driver.findElements(By.xpath("//button[normalize-space()='اضافة سطر']")).isEmpty();
    }

    /** Clears "طريقة التطبيق" with its × (the only required select that has one). */
    public void clearApplicationMethod() {
        WebElement clear = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(
                By.cssSelector("[data-testid='" + prefix + "-applicationMethod'] .select__clear-indicator")));
        // React Select clears on mousedown, not on click.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].dispatchEvent(new MouseEvent('mousedown',{bubbles:true,button:0}));", clear);
    }

    /** True when {@code text} is shown in the form (field errors, hints). */
    public boolean isTextShown(String text) {
        return driver.findElements(By.tagName("main")).stream().anyMatch(m -> m.getText().contains(text));
    }

    // ----- list: Kanban -----

    /**
     * A Kanban card (QC 2026-11-19): a button with a status badge
     * (span.rounded-full). The pricing cards show only the currency and the
     * badge, no operation number.
     */
    private static final By KANBAN_CARD = By.xpath("//button[contains(@class,'flex-col') and contains(@class,'rounded-lg')]"
            + "[.//span[contains(@class,'rounded-full')]]");

    /** Full text of every Kanban card, in order (waits for the cards). */
    public List<String> kanbanCardTexts() {
        return new WebDriverWait(driver, WAIT).until(d -> {
            List<String> texts = d.findElements(KANBAN_CARD).stream().map(e -> e.getText().trim()).toList();
            return texts.isEmpty() ? null : texts;
        });
    }

    /** Status badge of every Kanban card, in order. */
    public List<String> kanbanCardStatuses() {
        kanbanCardTexts();
        return driver.findElements(KANBAN_CARD).stream()
                .map(card -> card.findElement(By.xpath(".//span[contains(@class,'rounded-full')]")).getText().trim())
                .toList();
    }

    /**
     * Opens the Kanban card at {@code index} (0-based) by double-click: a single
     * click does nothing (QC 2026-11-19: the first card opened /view/1 on double-click).
     */
    public void openKanbanCardAt(int index) {
        kanbanCardTexts();
        WebElement card = driver.findElements(KANBAN_CARD).get(index);
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", card);
        new org.openqa.selenium.interactions.Actions(driver).doubleClick(card).perform();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("/view/"));
    }

    // ----- lines -----

    /** A number of a line cell, ignoring "%" and spaces ("15%" → 15, "-77.7 %" → -77.7). */
    public BigDecimal lineValue(int row, String colId) {
        String text = lineText(row, colId).replaceAll("[^0-9.\\-]", "");
        return text.isEmpty() || text.equals("-") || text.equals(".") ? BigDecimal.ZERO : new BigDecimal(text);
    }

    /** "السعر بعد التعديل" of every line, in order. */
    public List<BigDecimal> pricesAfter() {
        List<BigDecimal> prices = new ArrayList<>();
        for (int row = 0; row < lineCount(); row++) {
            prices.add(lineValue(row, PRICE_COLUMN));
        }
        return prices;
    }

    /**
     * ONE try, no retry: opens the editor of {@code colId} on line {@code row},
     * writes {@code value}, confirms with {@code confirmKey} (Enter / Tab) and returns what the cell shows
     * 2 s later. The shared setLineNumber() retries, and after Enter on the last
     * line in edit mode (QC 2026-11-22) its retries hit the new empty line and
     * opened "البحث عن صنف".
     */
    public String typeLineNumberOnce(int row, String colId, String value, org.openqa.selenium.Keys confirmKey) {
        ensureLineRendered(row);
        String line = ".ag-center-cols-container div[role='row'][row-index='" + row + "'] ";
        By cell = By.cssSelector(line + "div[role='gridcell'][col-id='" + colId + "']");
        By editor = By.cssSelector(line + "[col-id='" + colId + "'].ag-cell-inline-editing :is(input, textarea)");
        WebElement input = openCellEditor(colId + " (line " + (row + 1) + ")", cell,
                ExpectedConditions.visibilityOfElementLocated(editor));
        // Written by script, then confirmed with a key. Keys typed into this grid's editor
        // in edit mode do not arrive whole (QC 2026-11-22: "450.49" → the editor held "4",
        // the rest went to other cells). A value written by script is shown and saved,
        // but the form does not count it as a change for "تراجع".
        ((JavascriptExecutor) driver).executeScript(
                "const i=arguments[0];"
                        + "const set=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(i),'value').set;"
                        + "i.focus(); set.call(i, arguments[1]);"
                        + "i.dispatchEvent(new Event('input',{bubbles:true}));",
                input, value);
        input.sendKeys(confirmKey);
        try {
            Thread.sleep(2000); // the grid recalculates the lines after Enter
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        String shown = lineText(row, colId);
        log.info("Line {} {}: typed {} + {} → shows {}", row + 1, colId, value, confirmKey.name(), shown);
        return shown;
    }

    /**
     * Types "البيان" with the keyboard, like a user (setNote() writes it by script).
     * QC 2026-11-22: after setNote(), Undo left the new text in the field, while a
     * text typed by hand was undone.
     */
    public void typeNote(String text) {
        // QC 2026-11-22 (seen by hand): right after a line cell was edited, the grid takes
        // the focus back while "البيان" is being typed, so the rest of the text goes into
        // the grid and opens "البحث عن صنف". Wait until the grid lets go of the focus.
        ((JavascriptExecutor) driver).executeScript("if(document.activeElement) document.activeElement.blur();");
        new WebDriverWait(driver, Duration.ofSeconds(5)).pollingEvery(Duration.ofMillis(250)).until(d -> Boolean.TRUE.equals(
                ((JavascriptExecutor) d).executeScript("return !document.querySelector('.ag-cell-inline-editing')"
                        + " && (document.activeElement===document.body || !document.activeElement.closest('.ag-root'));")));
        try {
            Thread.sleep(1000); // the grid's own focus restore comes a moment later
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        WebElement input = textInput(noteField);
        // A REAL mouse click, like a user: focus() by script left the grid's own "focused
        // cell" in place, and the grid then took the digits and spaces typed afterwards
        // ("تسعير آلي 20261122-…" arrived as "تسعيرآلي") and opened "البحث عن صنف"
        // (QC 2026-11-22). Scrolled to the middle first: at the top the sticky toolbar
        // took the click.
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", input);
        new org.openqa.selenium.interactions.Actions(driver).moveToElement(input).click().perform();
        input.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"), org.openqa.selenium.Keys.DELETE);
        input.sendKeys(text);
        input.sendKeys(org.openqa.selenium.Keys.TAB);
        String shown = textValue(noteField);
        if (!text.equals(shown)) {
            throw new IllegalStateException("\"البيان\" shows [" + shown + "] after typing [" + text
                    + "]: the focus left the field while typing.");
        }
        log.info("البيان typed = {}", text);
    }

    // ----- dialogs -----

    /** Text of the top-most open dialog ("" when none). */
    public String topDialogText() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const d=[...document.querySelectorAll(\"[role=dialog][data-state=open],[role=alertdialog][data-state=open]\")];"
                        + "return d.length ? d[d.length-1].innerText : '';");
        return text == null ? "" : text.toString();
    }

    /** Clicks the button {@code label} of the top-most open dialog. */
    public void clickDialogButton(String label) {
        Object clicked = ((JavascriptExecutor) driver).executeScript(
                "const d=[...document.querySelectorAll(\"[role=dialog][data-state=open],[role=alertdialog][data-state=open]\")].pop();"
                        + "if(!d) return false;"
                        + "const b=[...d.querySelectorAll('button,[role=radio],label')].find(x=>x.innerText.trim()===arguments[0]);"
                        + "if(!b) return false; b.click(); return true;", label);
        if (!Boolean.TRUE.equals(clicked)) {
            throw new org.openqa.selenium.NoSuchElementException("No [" + label + "] in the open dialog: " + topDialogText());
        }
    }

    /**
     * "اعتماد" → "موافق"; when the "already priced" question follows (or is in
     * the same dialog), {@code choice} is picked and confirmed.
     *
     * @return true when the "already priced" question was asked.
     */
    public boolean approve(String choice) {
        clickApproveOrDisapprove();
        boolean asked = false;
        for (int step = 0; step < 3; step++) {
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> isDialogOpen());
            } catch (org.openqa.selenium.TimeoutException none) {
                break;
            }
            if (topDialogText().contains(ALREADY_PRICED_TEXT)) {
                asked = true;
                log.info("Approval asks about already-priced items → [{}].", choice);
                clickDialogButton(choice);
            }
            confirmDialog();
        }
        return asked;
    }

    // ----- API record filters -----

    /** {@code recordStatus}: 0 = مسودة, 1 = معتمد (QC 2026-11-12). */
    public static boolean isDraft(Map<String, Object> record) {
        return intOf(record, "recordStatus") == 0 && !Boolean.TRUE.equals(record.get("isDeleted"));
    }

    public static boolean isApproved(Map<String, Object> record) {
        return intOf(record, "recordStatus") == 1;
    }
}
