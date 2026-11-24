package com.erp.automation.pages.sales;

import com.erp.automation.pages.purchasing.PurchasingDocumentPage;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Shared page object for the Sales document screens (quotations, orders,
 * invoices, returns).
 *
 * <p>The Sales screens are built from the same parts as the Purchasing ones
 * (toolbar {@code <prefix>-save}/{@code -edit}/…, React Select header fields,
 * "انزال من" + "الرقم", AG Grid lines, the "⋮" menu, print preview,
 * attachments, record search and navigation, list pager / Kanban / group by).
 * Checked on "عرض السعر" in QC, 2026-11-12. So everything common is inherited
 * from {@link PurchasingDocumentPage} and only what Sales adds is here:</p>
 * <ul>
 *     <li>the two line tabs "قائمة الأصناف" (itemsList) and "مميز" (features,
 *     optional items with a check box and "إضافة الأصناف المحددة إلى قائمة
 *     الأصناف");</li>
 *     <li>"الشروط والاحكام" with its "عرض" toggle under the totals;</li>
 *     <li>text inputs set the way React expects (native setter + events).</li>
 * </ul>
 */
public abstract class SalesDocumentPage extends PurchasingDocumentPage {

    /** Line tab "قائمة الأصناف" (URL ?tab=itemsList). */
    public static final String ITEMS_TAB = "قائمة الأصناف";

    /** Line tab "مميز" (URL ?tab=features). */
    public static final String FEATURES_TAB = "مميز";

    /** Button of the "مميز" tab that moves the ticked lines to "قائمة الأصناف". */
    private static final By MOVE_FEATURES_BUTTON = By.xpath(
            "//button[contains(normalize-space(),'إضافة الأصناف المحددة إلى قائمة الأصناف')]");

    /**
     * data-testid / id of "البيان": "note" on quotations and orders, "notes" on
     * invoices and returns (QC 2026-11-12).
     */
    protected String noteField = "note";

    protected SalesDocumentPage(WebDriver driver, String prefix, String screenPath, String apiResource) {
        super(driver, prefix, screenPath, apiResource);
    }

    protected SalesDocumentPage(WebDriver driver, String prefix, String screenPath, String apiResource,
                                String codeField) {
        super(driver, prefix, screenPath, apiResource, codeField);
    }

    /** id / data-testid of the document number ("code", "invoiceCode"). */
    public String codeField() {
        return codeField;
    }

    /** The API record of document {@code code} (the API field has the same name as the form input), or null. */
    public java.util.Map<String, Object> recordByCode(String code) {
        return api.find(r -> code.equals(String.valueOf(r.get(codeField)))).stream().findFirst().orElse(null);
    }

    /**
     * The API record whose "البيان" is {@code note}, or null. The API names it
     * like the form ("note" / "notes"), except pricing, whose form field is
     * "statement" and API field "note".
     */
    public java.util.Map<String, Object> recordByNote(String note) {
        return api.find(r -> note.equals(String.valueOf(r.get(noteField)))
                        || note.equals(String.valueOf(r.get("note"))) || note.equals(String.valueOf(r.get("notes"))))
                .stream().findFirst().orElse(null);
    }

    // =====================================================================
    // "البيان"
    // =====================================================================

    /** "البيان" (input {@code #note} or {@code #notes}); checks it is kept. */
    public void setNote(String note) {
        if (!setTextInput(noteField, note)) {
            throw new IllegalStateException("\"البيان\" (" + noteField + ") is locked.");
        }
    }

    /** Value of "البيان". */
    public String note() {
        return textValue(noteField);
    }

    /** Waits (max 10 s) until "البيان" shows {@code expected}; returns what it shows. */
    public String waitForNote(String expected) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(10)).pollingEvery(Duration.ofMillis(250))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> expected.equals(note()));
        } catch (TimeoutException e) {
            // returned below for the caller's assertion
        }
        return note();
    }

    /** True when "البيان" can be changed. */
    public boolean isNoteEditable() {
        return isInputEditable(noteField);
    }

    // =====================================================================
    // List (before a document is opened)
    // =====================================================================

    /** col-id → header text of the list grid, in screen order (e.g. code=الرقم المرجعي). */
    @SuppressWarnings("unchecked")
    public java.util.Map<String, String> listColumns() {
        waitForListLoaded();
        Object raw = ((JavascriptExecutor) driver).executeScript(
                "const out={};"
                        + "document.querySelectorAll('.ag-header-cell[col-id]').forEach(c=>{"
                        + "  const id=c.getAttribute('col-id'); if(id && !(id in out)) out[id]=c.innerText.trim();});"
                        + "return out;");
        return new java.util.LinkedHashMap<>((java.util.Map<String, String>) raw);
    }

    /** True when the list's "جديد" button is shown and enabled. */
    public boolean isNewButtonShown() {
        return driver.findElements(By.xpath("//button[normalize-space()='جديد' or .//span[normalize-space()='جديد']]"))
                .stream().anyMatch(b -> b.isDisplayed() && b.isEnabled());
    }

    /**
     * List row of {@code code} → "إجراءات" (⋯) → "عرض" (QC 2026-11-12: the row
     * menu has "عرض" only). Waits for the document to open.
     */
    public void openFromRowActions(String code) {
        waitForListLoaded();
        Object rowIndex = ((JavascriptExecutor) driver).executeScript(
                "const c=[...document.querySelectorAll(\"div[role=row][row-index] [col-id='\" + arguments[1] + \"']\")]"
                        + "  .find(x=>x.innerText.trim()===arguments[0]);"
                        + "return c ? c.closest('div[role=row]').getAttribute('row-index') : null;",
                code, codeField);
        if (rowIndex == null) {
            throw new org.openqa.selenium.NoSuchElementException(code + " is not on the current list page: " + visibleListCodes());
        }
        By button = By.cssSelector("div[role='row'][row-index='" + rowIndex + "'] [col-id='actions' i] button");
        WebElement menu = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(button));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", menu);
        new org.openqa.selenium.interactions.Actions(driver).moveToElement(menu).click().perform();
        // The menu item is <div role=menuitem><span><a href=".../view/<id>">عرض</a></span></div>.
        // Only the link navigates: a click on the centre of the (wider) menu item
        // closes the menu and stays on the list (QC 2026-11-12, mvn run 11:48).
        By viewLink = By.xpath("//*[@role='menu' and @data-state='open']//*[@role='menuitem']//a[normalize-space()='عرض']");
        By viewItem = By.xpath("//*[@role='menu' and @data-state='open']//*[@role='menuitem'][normalize-space()='عرض']");
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(viewItem));
        ui.clickWhenReady(driver.findElements(viewLink).isEmpty() ? viewItem : viewLink);
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("/view/"));
        waitForDocumentLoaded();
    }

    /**
     * Items of the list row menu (⋯) of the first row; the menu is closed again.
     *
     * <p>The actions column is {@code col-id="actions"} on most lists but
     * {@code col-id="Actions"} on "فواتير المبيعات" (QC 2026-11-16), so the row
     * menu selectors match it case-insensitively ({@code [col-id='actions' i]}).</p>
     */
    public List<String> rowActionItems() {
        waitForListLoaded();
        By button = By.cssSelector("div[role='row'][row-index='0'] [col-id='actions' i] button");
        WebElement menu = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(button));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", menu);
        new org.openqa.selenium.interactions.Actions(driver).moveToElement(menu).click().perform();
        By items = By.cssSelector("[role='menu'][data-state='open'] [role='menuitem']");
        List<String> labels = new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    List<String> shown = d.findElements(items).stream().map(e -> e.getText().trim())
                            .filter(t -> !t.isEmpty()).toList();
                    return shown.isEmpty() ? null : shown;
                });
        new org.openqa.selenium.interactions.Actions(driver).sendKeys(Keys.ESCAPE).perform();
        return labels;
    }

    // =====================================================================
    // Line tabs: "قائمة الأصناف" / "مميز"
    // =====================================================================

    private static By tab(String label) {
        return By.xpath("//*[@role='tab'][normalize-space()=" + xpathLiteral(label) + "]");
    }

    /** Opens line tab {@code label} and waits until it is the active one. */
    public void openLinesTab(String label) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement tab = new WebDriverWait(driver, WAIT)
                    .until(ExpectedConditions.presenceOfElementLocated(tab(label)));
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", tab);
            tab.click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> "active".equals(d.findElement(tab(label)).getAttribute("data-state")));
                ui.waitForGridToSettle();
                log.info("Lines tab: {}", label);
                return;
            } catch (TimeoutException notYet) {
                log.warn("Tab [{}] did not open (attempt {}/3).", label, attempt);
            }
        }
        throw new TimeoutException("Lines tab [" + label + "] did not open after 3 clicks.");
    }

    /** "قائمة الأصناف". */
    public void openItemsTab() {
        openLinesTab(ITEMS_TAB);
    }

    /** "مميز". */
    public void openFeaturesTab() {
        openLinesTab(FEATURES_TAB);
    }

    /** True when line tab {@code label} is the active one. */
    public boolean isLinesTabActive(String label) {
        List<WebElement> tabs = driver.findElements(tab(label));
        return !tabs.isEmpty() && "active".equals(tabs.get(0).getAttribute("data-state"));
    }

    /** Ticks the check box of line {@code row} in the active tab ("مميز"); checks it is ticked. */
    public void tickLine(int row) {
        By box = By.cssSelector("div[role='row'][row-index='" + row + "'] [col-id='custom-checkbox'] input[type='checkbox']");
        WebElement input = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(box));
        if (input.isSelected()) {
            return;
        }
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center', inline:'center'});", input);
        try {
            input.click();
        } catch (RuntimeException covered) {
            // AG Grid toggles the selection with Space on the focused check box.
            ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", input);
            input.sendKeys(Keys.SPACE);
        }
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElement(box).isSelected());
        log.info("Line {} ticked.", row + 1);
    }

    /** True when "إضافة الأصناف المحددة إلى قائمة الأصناف" can be clicked. */
    public boolean isMoveFeaturesEnabled() {
        List<WebElement> buttons = driver.findElements(MOVE_FEATURES_BUTTON);
        return !buttons.isEmpty() && buttons.get(0).isEnabled();
    }

    /** "إضافة الأصناف المحددة إلى قائمة الأصناف" (moves the ticked "مميز" lines). */
    public void moveTickedFeaturesToItems() {
        ui.clickWhenReady(MOVE_FEATURES_BUTTON);
        ui.waitForGridToSettle();
    }

    // =====================================================================
    // "الشروط والاحكام"
    // =====================================================================

    /**
     * "الشروط والاحكام" → "عرض": opens the terms and returns their text
     * (QC 2026-11-12: "البضاعة التي تباع لا ترد ولا تستبدل").
     */
    public String showTermsAndConditions() {
        By heading = By.xpath("//*[normalize-space()='الشروط والاحكام']");
        WebElement title = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(heading));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", title);
        By toggle = By.xpath("//*[normalize-space()='الشروط والاحكام']/following::button[normalize-space()='عرض'][1]");
        Object before = termsText();
        ui.clickWhenReady(toggle);
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(d -> {
                        String now = termsText();
                        return !now.isBlank() && !now.equals(before) ? now : null;
                    });
        } catch (TimeoutException unchanged) {
            return termsText();
        }
    }

    /** Text under "الشروط والاحكام" without the heading and the "عرض" button. */
    public String termsText() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const h=[...document.querySelectorAll('*')].find(e=>e.children.length===0"
                        + " && e.textContent.trim()==='الشروط والاحكام');"
                        + "if(!h) return '';"
                        + "let box=h.parentElement;"
                        + "for(let i=0;i<3 && box && box.innerText.replace('الشروط والاحكام','').replace('عرض','').trim()==='';i++)"
                        + "  box=box.parentElement;"
                        + "return box ? box.innerText.replace('الشروط والاحكام','').replace('عرض','').trim() : '';");
        return text == null ? "" : text.toString().trim();
    }

    // =====================================================================
    // Text inputs
    // =====================================================================

    /**
     * Sets text input {@code #<field>} the way React expects (native setter +
     * input/change/blur) and checks it keeps {@code text}; when it does not,
     * it is typed key by key. Same approach as "رقم عرض المورد" on purchase
     * quotations (QC 2026-11-02: sendKeys kept only the first character).
     *
     * @return false when the field is locked.
     */
    public boolean setTextInput(String field, String text) {
        WebElement input = new WebDriverWait(driver, WAIT)
                .until(d -> textInput(field));
        if (!input.isEnabled() || input.getAttribute("readonly") != null) {
            log.info("Field {} is not editable → not changed.", field);
            return false;
        }
        ((JavascriptExecutor) driver).executeScript(
                "const i=arguments[0];"
                        + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(i, arguments[1]);"
                        + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('change',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('blur',{bubbles:true}));",
                input, text);
        if (keeps(field, text)) {
            log.info("{} = {}", field, text);
            return true;
        }
        log.warn("{} shows [{}] after setting [{}] → typing it key by key.", field, textValue(field), text);
        WebElement retry = textInput(field);
        retry.click();
        retry.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        for (char c : text.toCharArray()) {
            textInput(field).sendKeys(String.valueOf(c));
        }
        if (!keeps(field, text)) {
            throw new IllegalStateException(field + " shows [" + textValue(field) + "] instead of [" + text + "].");
        }
        log.info("{} = {} (typed)", field, text);
        return true;
    }

    /** True when {@code #<field>} shows {@code text} and still shows it a moment later. */
    private boolean keeps(String field, String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).pollingEvery(Duration.ofMillis(200))
                    .until(d -> text.equals(textValue(field)));
            Thread.sleep(400); // one render later it must still be there
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return text.equals(textValue(field));
    }

    /**
     * Input {@code #<field>}, or the input with data-testid {@code <prefix>-<field>}:
     * some text inputs have no id (QC 2026-11-12: "عنوان التوصيل" on orders,
     * "الرقم الضريبي" on orders). Null when there is none (for waits).
     */
    protected WebElement textInput(String field) {
        List<WebElement> byId = driver.findElements(By.id(field));
        if (!byId.isEmpty()) {
            return byId.get(0);
        }
        List<WebElement> byTestId = driver.findElements(By.cssSelector("input[data-testid='" + prefix + "-" + field + "'], "
                + "[data-testid='" + prefix + "-" + field + "'] input"));
        return byTestId.isEmpty() ? null : byTestId.get(0);
    }

    /** Value of input {@code field} (by id, else by data-testid); "" when there is none. */
    public String textValue(String field) {
        WebElement input = textInput(field);
        return input == null ? "" : input.getAttribute("value");
    }

    /** True when input / React Select {@code field} exists and can be changed. */
    public boolean isInputEditable(String field) {
        WebElement input = textInput(field);
        return input != null && input.isEnabled() && input.getAttribute("readonly") == null;
    }

    // =====================================================================
    // "المستند المرجعي" with its two buttons (orders, invoices, returns)
    // =====================================================================

    private By referenceDocumentButton(int index) {
        return By.xpath("//input[@data-testid='" + prefix + "-referenceDocument']/following-sibling::div//button[" + index + "]");
    }

    /** True when "المستند المرجعي" has its picker buttons (orders, invoices, returns; not quotations). */
    public boolean hasReferenceDocumentPicker() {
        return !driver.findElements(referenceDocumentButton(1)).isEmpty();
    }

    /**
     * "المستند المرجعي" → first button: the list of source documents (approved
     * quotations for an order, orders for an invoice, invoices for a return) →
     * a random row → "موافق".
     *
     * @return the chosen number, as shown in the field.
     */
    public String selectRandomReferenceDocument() {
        ui.clickWhenReady(referenceDocumentButton(1));
        String picked = selectRandomRowInOpenDialog();
        confirmDialog();
        String number = waitForReferenceDocument();
        log.info("المستند المرجعي = {} (row {})", number, picked);
        return number;
    }

    /**
     * Like {@link #selectRandomReferenceDocument()}, but only a document whose number
     * is accepted by {@code allowedCode} is chosen (QC 2026-11-18: an invoice without
     * a warehouse left "المخزن" of the return empty).
     *
     * @return the chosen number, or "" when the list shows no accepted document
     *         (the dialog is closed).
     */
    public String selectReferenceDocument(java.util.function.Predicate<String> allowedCode) {
        ui.clickWhenReady(referenceDocumentButton(1));
        String picked = selectRandomRowInOpenDialog(allowedCode);
        if (picked.isEmpty()) {
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
            return "";
        }
        confirmDialog();
        String number = waitForReferenceDocument();
        log.info("المستند المرجعي = {} (chosen {})", number, picked);
        return number;
    }

    /**
     * "المستند المرجعي" → first button → the row of {@code number} (searched in the
     * dialog, or found page by page) → "موافق". Used with an invoice chosen through
     * the API (QC 2026-11-18).
     *
     * @return true when {@code number} is now in "المستند المرجعي"; false when the
     *         dialog does not list it (the dialog is closed).
     */
    public boolean selectReferenceDocumentByNumber(String number) {
        ui.clickWhenReady(referenceDocumentButton(1));
        if (!selectRowInOpenDialogByNumber(number, 40)) {
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
            return false;
        }
        confirmDialog();
        String shown = waitForReferenceDocument();
        log.info("المستند المرجعي = {} (API choice {})", shown, number);
        return number.equals(shown);
    }

    /**
     * "⋮" → {@code menuItem} ("متابعة من عرض سعر" / "متابعة من أمر بيع") → a
     * random row → "موافق". Fills "المستند المرجعي" like its first button.
     */
    public String followFromMenu(String menuItem) {
        clickMenuItem(menuItem);
        String picked = selectRandomRowInOpenDialog();
        confirmDialog();
        String number = waitForReferenceDocument();
        log.info("⋮ {} → {} (row {})", menuItem, number, picked);
        return number;
    }

    private String waitForReferenceDocument() {
        return new WebDriverWait(driver, WAIT)
                .until(d -> {
                    String value = textValue("referenceDocument").trim();
                    return value.isBlank() ? null : value;
                });
    }

    /**
     * Second button of "المستند المرجعي": loads the chosen document's data and
     * lines (QC 2026-11-12: an order from SQ2609-000030 got its line and the
     * URL became /sales/sales-order/add/31?typeName=SalesQutation).
     */
    public void downloadReferenceDocument() {
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(referenceDocumentButton(2)));
        button.click();
        confirmDialogIfShown(Duration.ofSeconds(2));
        ui.waitForGridToSettle();
    }

    // =====================================================================
    // "قيد اليومية" (invoices, returns)
    // =====================================================================

    private static final By JOURNAL_DIALOG = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='قيد اليومية']]");

    /** One line of the journal entry: account, debit, credit (base currency). */
    public record JournalLine(String account, java.math.BigDecimal debit, java.math.BigDecimal credit) {
    }

    /** Toolbar "قيد اليومية" → reads every line → "إغلاق". */
    @SuppressWarnings("unchecked")
    public List<JournalLine> readJournalEntry() {
        clickAction("journalEntry");
        WebElement dialog = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.visibilityOfElementLocated(JOURNAL_DIALOG));
        List<java.util.Map<String, String>> rows = new WebDriverWait(driver, WAIT)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    Object raw = ((JavascriptExecutor) d).executeScript(
                            "const dl=arguments[0]; const out={};"
                                    + "dl.querySelectorAll('div[role=row][row-index]').forEach(r=>{"
                                    + "  const i=r.getAttribute('row-index'); const o=out[i]||(out[i]={});"
                                    + "  r.querySelectorAll('[col-id]').forEach(c=>{o[c.getAttribute('col-id')]=c.innerText.trim();});});"
                                    + "return Object.values(out);",
                            d.findElement(JOURNAL_DIALOG));
                    List<java.util.Map<String, String>> list = (List<java.util.Map<String, String>>) raw;
                    return list == null || list.isEmpty() ? null : list;
                });
        List<JournalLine> lines = rows.stream()
                .map(r -> new JournalLine(r.getOrDefault("accountName", ""),
                        number(r.get("baseDebitAmount")), number(r.get("baseCreditAmount"))))
                .toList();
        dialog.findElement(By.xpath(".//button[normalize-space()='إغلاق']")).click();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.invisibilityOfElementLocated(JOURNAL_DIALOG));
        log.info("Journal entry: {}", lines);
        return lines;
    }

    private static java.math.BigDecimal number(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9.\\-]", "");
        return digits.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(digits);
    }

    /**
     * A number shown after {@code label} in the totals under the lines, e.g.
     * "الإجمالي شامل الضريبة:" → 23.000. {@code null} when the label is not shown.
     */
    public java.math.BigDecimal totalShown(String label) {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const t=document.querySelector('main').innerText.split('\\n');"
                        + "for(const line of t){ if(line.replace(/[:\\s]/g,'').startsWith(arguments[0].replace(/[:\\s]/g,'')))"
                        + "  return line; } return null;", label);
        if (text == null) {
            return null;
        }
        String value = text.toString().substring(text.toString().indexOf(label.replace(":", "").trim()) + label.replace(":", "").trim().length());
        String digits = value.replaceAll("[^0-9.\\-]", "");
        return digits.isEmpty() ? null : new java.math.BigDecimal(digits);
    }
}
