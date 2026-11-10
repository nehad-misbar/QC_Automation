package com.erp.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.isTrue;
import static com.erp.automation.ui.DocumentApi.textOf;

/**
 * Free Quantity Processing screen (معالجة الكمية المجانية): {@code /purchase/process-free-qty},
 * data-testid prefix {@code process-free-qty}, list API {@code PurchasFreeQuantities/filter},
 * numbers FQP… in {@code operationCode}.
 *
 * <p>Checked in QC on 2026-11-05 (nothing saved):</p>
 * <ul>
 *     <li>"اضافة سطر" appears only after the branch AND the warehouse are chosen; the item list
 *         offers every item (not only those with free quantity);</li>
 *     <li>header: "التاريخ*" ({@code operationDate}), "الفرع*", "المخزن*", "البيان" ({@code note}),
 *         "نوع المعالجة*" = radios {@code r_1} هدية مجانية للعميل, {@code r_2} تخفيض التكلفة,
 *         {@code r_3} إيراد للشركة; no tabs;</li>
 *     <li>lines: itemCode, itemId, itemSupplierCode, barCode, availableFreeQuantities
 *         ("الكمية المجانية المتوفرة", read only), quantitiesToBeProcessed ("الكمية المراد معالجتها"),
 *         uomId, displayPackingUnit, note;</li>
 *     <li>statuses (API): Draft, Approved ({@code isApproved}), Cancelled ({@code isCanceled});
 *         the form has no "الحالة" label, so the status is read through the API;</li>
 *     <li>toolbar: new = save disabled until the form is filled; Draft = copy, add, edit, delete,
 *         print barcode, items pricing, approve, cancel, log; Approved = delete disabled,
 *         disapprove, cancel and journal entry enabled;</li>
 *     <li>"⋮": new = "الاستيراد من Excel"; saved = "التصدير الى Excel";</li>
 *     <li>list: Kanban (cards: warehouse, user, status "معتمده", number) and group by;
 *         no summaries.</li>
 * </ul>
 */
public class FreeQuantityPage extends PurchasingDocumentPage {

    public static final String PREFIX = "process-free-qty";
    public static final String CODE_FIELD = "operationCode";

    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";
    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";

    /**
     * "نوع المعالجة" radios (هدية مجانية للعميل، تخفيض التكلفة، إيراد للشركة). QC 2026-11-05:
     * data-testid {@code process-free-qty-processingTypeId-r_1..3} (the button id is r_1..3).
     */
    private static final String[] PROCESSING_TYPES = {"processingTypeId-r_1", "processingTypeId-r_2", "processingTypeId-r_3"};

    public static final String COL_ITEM_CODE = "itemCode";
    public static final String COL_AVAILABLE = "availableFreeQuantities";
    public static final String COL_TO_PROCESS = "quantitiesToBeProcessed";

    private static final By OPEN_DIALOG = By.cssSelector("[role='dialog'][data-state='open']");

    public FreeQuantityPage(WebDriver driver) {
        super(driver, PREFIX, "/purchase/process-free-qty", "PurchasFreeQuantities/filter", CODE_FIELD);
    }

    // ----- header -----

    public void setOperationDate(LocalDate date) {
        setDate("operationDate", date);
    }

    public String operationDate() {
        return inputValue("operationDate").trim();
    }

    public boolean setNote(String text) {
        return typeInto("note", text);
    }

    public String note() {
        return inputValue("note").trim();
    }

    public String selectRandomProcessingType() {
        return clickRandomRadio(PROCESSING_TYPES);
    }

    /** A processing type other than the selected one (edit mode). */
    public String selectOtherProcessingType() {
        return clickOtherRadio(PROCESSING_TYPES);
    }

    public String processingType() {
        return checkedRadio(PROCESSING_TYPES);
    }

    // ----- lines -----

    /** "الكمية المجانية المتوفرة" of line {@code row}. */
    public BigDecimal availableFreeQuantity(int row) {
        return lineNumber(row, COL_AVAILABLE);
    }

    public BigDecimal quantityToProcess(int row) {
        return lineNumber(row, COL_TO_PROCESS);
    }

    public void setQuantityToProcess(int row, String quantity) {
        setLineNumber(row, COL_TO_PROCESS, quantity);
    }

    public String itemCode(int row) {
        return lineText(row, COL_ITEM_CODE);
    }

    public List<String> requiredErrorFields() {
        Object names = ((JavascriptExecutor) driver).executeScript(
                "const p=arguments[0]+'-';"
                        + "return [...document.querySelectorAll('*')]"
                        + " .filter(e=>e.children.length===0 && e.textContent.trim()==='هذا الحقل مطلوب')"
                        + " .map(e=>{let x=e; for(let i=0;i<5 && x;i++){x=x.parentElement;"
                        + "   const f=x && x.querySelector('[data-testid^=\"'+p+'\"],input[id]');"
                        + "   if(f) return (f.dataset.testid||'').startsWith(p) ? f.dataset.testid.substring(p.length) : f.id;}"
                        + "   return '?';});",
                PREFIX);
        return names == null ? List.of() : (List<String>) names;
    }

    // ----- "قيد اليومية" (Approved) -----

    /** The journal entry of the opened document: number of lines and the debit / credit totals (base currency). */
    public record JournalEntry(int lines, java.math.BigDecimal debit, java.math.BigDecimal credit, String title) {
    }

    public JournalEntry readJournalEntry() {
        return readJournalEntry(Duration.ofSeconds(15));
    }

    /**
     * The journal entry of the opened document, or empty when "قيد اليومية" shows no line within
     * {@code timeout} (the document has no journal entry). Any dialog left open is closed.
     */
    public java.util.Optional<JournalEntry> tryReadJournalEntry(Duration timeout) {
        try {
            return java.util.Optional.of(readJournalEntry(timeout));
        } catch (org.openqa.selenium.TimeoutException noLines) {
            if (driver.findElements(OPEN_DIALOG).stream().anyMatch(WebElement::isDisplayed)) {
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            }
            return java.util.Optional.empty();
        }
    }

    /**
     * True when the API record names a journal entry (any field with "journal" in its name
     * that holds a value other than null / "" / 0 / false). Used to try those documents first.
     */
    public static boolean hasJournalEntryHint(Map<String, Object> record) {
        return record.entrySet().stream()
                .filter(field -> field.getKey().toLowerCase().contains("journal"))
                .map(Map.Entry::getValue)
                .anyMatch(value -> value != null && !(value instanceof Boolean b && !b)
                        && !(value instanceof Number n && n.doubleValue() == 0)
                        && !value.toString().isBlank() && !"0".equals(value.toString().trim()));
    }

    @SuppressWarnings("unchecked")
    private JournalEntry readJournalEntry(Duration timeout) {
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(By.id("journalEntry")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({inline:'center', block:'nearest'});", button);
        driver.findElement(By.id("journalEntry")).click();
        Map<String, Object> read = (Map<String, Object>) new WebDriverWait(driver, timeout)
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    Object r = ((JavascriptExecutor) d).executeScript(
                            "const dl=[...document.querySelectorAll(\"[role='dialog'][data-state='open']\")]"
                                    + " .find(x=>(x.querySelector('h2')||{}).textContent==='قيد اليومية');"
                                    + "if(!dl) return null;"
                                    + "const rows=[...dl.querySelectorAll('.ag-center-cols-container [role=row]')];"
                                    + "if(!rows.length) return null;"
                                    + "const sum=c=>rows.reduce((s,r)=>s+Number(((r.querySelector('[col-id=\"'+c+'\"]')||{}).textContent||'0').replace(/,/g,'')),0);"
                                    + "return {lines: rows.length, debit: String(sum('baseDebitAmount')), credit: String(sum('baseCreditAmount')),"
                                    + " title: dl.querySelector('h2').textContent};");
                    return r instanceof Map<?, ?> ? r : null;
                });
        JournalEntry entry = new JournalEntry(((Number) read.get("lines")).intValue(),
                new java.math.BigDecimal(String.valueOf(read.get("debit"))),
                new java.math.BigDecimal(String.valueOf(read.get("credit"))), String.valueOf(read.get("title")));
        closeOpenDialogWith("إغلاق");
        log.info("Journal entry: {}", entry);
        return entry;
    }

    /** Closes the open dialog with its button {@code label} ("إغلاق" / "Close"), and waits until it is gone. */
    public void closeOpenDialogWith(String label) {
        WebElement dialog = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(OPEN_DIALOG));
        List<WebElement> buttons = dialog.findElements(By.xpath(".//button[normalize-space()=" + xpathLiteral(label)
                + " or normalize-space()='Close']"));
        if (buttons.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        } else {
            buttons.get(0).click();
        }
        new WebDriverWait(driver, WAIT).until(d -> d.findElements(OPEN_DIALOG).stream().noneMatch(WebElement::isDisplayed));
    }

    // ----- record filters for DocumentApi -----

    public static String codeOf(Map<String, Object> record) {
        return textOf(record, CODE_FIELD);
    }

    /**
     * QC 2026-11-09: after a successful "إلغاء" (PUT /cancel → 200, data true) {@code isCanceled}
     * alone did not turn true, so every flag the API uses for a cancelled record is accepted:
     * {@code isCanceled} / {@code isCancelled} / {@code isCancel}, {@code recordStatus} = 2
     * (as on requisitions and quotations), or a status text "ملغي".
     */
    public static boolean isCancelled(Map<String, Object> record) {
        for (Map.Entry<String, Object> field : record.entrySet()) {
            String key = field.getKey().toLowerCase();
            Object value = field.getValue();
            if (key.startsWith("iscancel") && truthy(value)) {
                return true; // isCanceled / isCancelled / isCancel: true, 1 or "true"
            }
            if ((key.equals("recordstatus") || key.equals("status") || key.equals("documentstatus"))
                    && value instanceof Number n && n.intValue() == 2) {
                return true; // 0 مسودة, 1 معتمد, 2 ملغي (as on requisitions / quotations)
            }
            if (key.contains("status") && value instanceof String text && text.contains("ملغ")) {
                return true;
            }
        }
        return false;
    }

    /** Boolean true, a non-zero number, or the text "true" / "1" (the API is not consistent). */
    private static boolean truthy(Object value) {
        if (value instanceof Boolean b) {
            return b;
        }
        if (value instanceof Number n) {
            return n.intValue() != 0;
        }
        return value != null && ("true".equalsIgnoreCase(value.toString().trim()) || "1".equals(value.toString().trim()));
    }

    /** The status-like fields of a record (approve / cancel / status), for failure messages. */
    public static Map<String, Object> statusFields(Map<String, Object> record) {
        Map<String, Object> fields = new java.util.TreeMap<>();
        record.forEach((key, value) -> {
            String k = key.toLowerCase();
            if (k.contains("cancel") || k.contains("approv") || k.contains("status")) {
                fields.put(key, value);
            }
        });
        return fields;
    }

    public static boolean isApproved(Map<String, Object> record) {
        return truthy(record.get("isApproved")) && !isCancelled(record);
    }

    public static boolean isDraft(Map<String, Object> record) {
        return !truthy(record.get("isApproved")) && !isCancelled(record);
    }
}
