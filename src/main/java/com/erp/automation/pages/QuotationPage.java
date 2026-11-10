package com.erp.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;
import static com.erp.automation.ui.DocumentApi.isTrue;

/**
 * Price Quotation screen (عروض الاسعار): {@code /purchase/price-offers}.
 *
 * <p>Everything common to Purchasing documents is in
 * {@link PurchasingDocumentPage}. This class adds only what is specific to
 * quotations: its header fields, the "المستند المرجعي" (requisition) step,
 * its statuses and its API flags.</p>
 *
 * <p>Checked in QC on 2026-11-02 (nothing saved):</p>
 * <ul>
 *     <li>data-testid prefix {@code price-offers}; the number is {@code #qCode}
 *     on the form and col-id {@code qCode} in the list;</li>
 *     <li>API resource {@code PurchasQutations} (52 records: 16 Draft,
 *     5 Approved, 2 Cancelled, 29 Expired);</li>
 *     <li>required: المورد، العملة، الفرع، المخزن، طريقة احتساب الضريبة، الأسعار;</li>
 *     <li>Draft in edit mode: every field but التاريخ and المستند المرجعي; no "انزال من";</li>
 *     <li>Approved in edit mode ({@code /view-approved/}): البيان only;</li>
 *     <li>"⋮": التصدير الى Excel, انشاء امر شراء (enabled on Approved only),
 *     طلب شراء (always disabled);</li>
 *     <li>"انشاء امر شراء" opens {@code /purchase/orders/add/<id>?typeName=PurchaseQutation}.</li>
 * </ul>
 *
 * <p>The old page object (90 KB, fixed sleeps, one method per field) was
 * replaced on 2026-11-02. It is kept in git history.</p>
 */
public class QuotationPage extends PurchasingDocumentPage {

    public static final String PREFIX = "price-offers";

    /** Form input id and list col-id of the quotation number. */
    public static final String CODE_FIELD = "qCode";

    /** {@code recordStatus} of the API: 0 = مسودة, 1 = معتمد, 2 = ملغي, 3 = منتهي (QC 2026-11-02). */
    public enum RecordStatus {
        DRAFT(0), APPROVED(1), CANCELLED(2), EXPIRED(3);

        public final int code;

        RecordStatus(int code) {
            this.code = code;
        }

        public boolean of(Map<String, Object> record) {
            return intOf(record, "recordStatus") == code;
        }
    }

    /** Header fields that are React Select dropdowns, in screen order. */
    public static final List<String> SELECT_FIELDS = List.of(
            "vendorId", "currencyId", "branchId", "warehouseId", "costCenterId",
            "taxCalculationMethod", "purchaseRepresentativeId");

    /** Date fields the user can set, in screen order ("التاريخ" is set on a new quotation only). */
    public static final List<String> DATE_FIELDS = List.of("quotationDeadLine", "receivedDate");

    /** "الأسعار" radios: باستثناء الضريبة (prices-0) / شاملة الضريبة (prices-1). */
    private static final String[] PRICE_RADIOS = {"prices-0", "prices-1"};

    public QuotationPage(WebDriver driver) {
        super(driver, PREFIX, "/purchase/price-offers", "PurchasQutations", CODE_FIELD);
    }

    // ----- header -----

    /** "التاريخ" (quotation date; today on a new quotation, locked in edit mode). */
    public void setQuotationDate(LocalDate date) {
        setDate("quotationDate", date);
    }

    /** "تاريخ الانتهاء". After it the quotation becomes "منتهي". */
    public void setQuotationDeadline(LocalDate date) {
        setDate("quotationDeadLine", date);
    }

    /** "اخر موعد للاستلام". */
    public void setReceivedDate(LocalDate date) {
        setDate("receivedDate", date);
    }

    /**
     * "رقم عرض المورد". QC 2026-11-02: typing the whole text with sendKeys
     * left only "V" in the field (the form re-renders after the first key),
     * so the value is set the way React expects (native setter + input
     * event) and checked; if it is not kept it is typed one key at a time.
     *
     * @return false when the field is locked.
     */
    public boolean setVendorQuotationNumber(String number) {
        return setTextInput("vendorQuotationNumber", number);
    }

    /** Sets text input {@code #<field>} and checks it keeps {@code text}; false when locked. */
    public boolean setTextInput(String field, String text) {
        WebElement input = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(By.id(field)));
        if (!input.isEnabled() || input.getAttribute("readonly") != null) {
            log.info("Field {} is not editable → not changed.", field);
            return false;
        }
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
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
        log.warn("{} shows [{}] after setting [{}] → typing it key by key.", field, inputValue(field), text);
        WebElement retry = driver.findElement(By.id(field));
        retry.click();
        retry.sendKeys(org.openqa.selenium.Keys.chord(org.openqa.selenium.Keys.CONTROL, "a"),
                org.openqa.selenium.Keys.DELETE);
        for (char c : text.toCharArray()) {
            driver.findElement(By.id(field)).sendKeys(String.valueOf(c));
        }
        if (!keeps(field, text)) {
            throw new IllegalStateException(field + " shows [" + inputValue(field) + "] instead of [" + text + "].");
        }
        log.info("{} = {} (typed)", field, text);
        return true;
    }

    /** True when {@code #<field>} shows {@code text} and still shows it a moment later. */
    private boolean keeps(String field, String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).pollingEvery(Duration.ofMillis(200))
                    .until(d -> text.equals(inputValue(field)));
            Thread.sleep(400); // one render later it must still be there
        } catch (TimeoutException e) {
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return text.equals(inputValue(field));
    }

    /** "مده العرض" (days). */
    public boolean setDuration(String days) {
        return typeInto("duration", days);
    }

    /** "الأسعار" at random; returns the radio id. */
    public String selectRandomPriceOption() {
        return clickRandomRadio(PRICE_RADIOS);
    }

    /** "الأسعار" = باستثناء الضريبة (prices-0); "" when the radio is locked. */
    public String selectPricesExcludingTax() {
        return "prices-0".equals(priceOption()) ? "prices-0" : clickRandomRadio("prices-0");
    }

    /** "الأسعار": the option that is not selected now (edit mode). */
    public String selectOtherPriceOption() {
        return clickOtherRadio(PRICE_RADIOS);
    }

    /** Checked "الأسعار" radio id, or "". */
    public String priceOption() {
        return checkedRadio(PRICE_RADIOS);
    }

    /** Current value of a date field, or {@code null} when empty / not a date. */
    public LocalDate dateValue(String field) {
        try {
            return LocalDate.parse(inputValue(field));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ----- "المستند المرجعي" (a requisition) -----

    private By referenceDocumentButton(int index) {
        return By.xpath("//input[@data-testid='" + PREFIX + "-referenceDocument']"
                + "/following-sibling::div//button[" + index + "]");
    }

    /**
     * Opens the "المستند المرجعي" dialog (requisitions), selects a random row
     * and confirms it.
     *
     * @return the chosen requisition number, as shown in the field.
     */
    public String selectRandomReferenceDocument() {
        ui.clickWhenReady(referenceDocumentButton(1));
        String picked = selectRandomRowInOpenDialog();
        confirmDialog();
        String number = new WebDriverWait(driver, WAIT)
                .until(d -> {
                    String value = inputValue("referenceDocument").trim();
                    return value.isBlank() ? null : value;
                });
        log.info("المستند المرجعي = {} (row {})", number, picked);
        return number;
    }

    /**
     * Download button next to "المستند المرجعي": loads the requisition's
     * data and lines into the quotation. A confirmation may follow.
     */
    public void downloadReferenceDocument() {
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(referenceDocumentButton(2)));
        button.click();
        confirmDialogIfShown(Duration.ofSeconds(2));
        ui.waitForGridToSettle();
    }

    /** True when the "المستند المرجعي" field can be changed (new quotation only). */
    public boolean isReferenceDocumentEditable() {
        List<WebElement> inputs = driver.findElements(By.id("referenceDocument"));
        return !inputs.isEmpty() && inputs.get(0).isEnabled();
    }

    /** Waits until a header select shows a value (data loaded after a download), max {@code timeout}. */
    public String waitForSelectValue(String field, Duration timeout) {
        try {
            return new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> {
                        String text = selectedText(field);
                        return text == null || text.isBlank() ? null : text;
                    });
        } catch (TimeoutException e) {
            return "";
        }
    }

    // ----- lines grid: "البيان" column -----

    /**
     * Scrolls the lines grid sideways until column {@code headerText} is
     * rendered and returns its col-id ("" when the grid has no such column).
     * AG Grid renders only the columns in view, and the grid is right to
     * left, so both scroll directions are tried.
     */
    public String revealLineColumn(String headerText) {
        Object colId = ((org.openqa.selenium.JavascriptExecutor) driver).executeAsyncScript(
                "const [text, done] = arguments;"
                        + "const views=['.ag-center-cols-viewport','.ag-body-horizontal-scroll-viewport']"
                        + "  .map(s=>document.querySelector(s)).filter(Boolean);"
                        + "const find=()=>{const h=[...document.querySelectorAll('.ag-header-cell[col-id]')]"
                        + "  .find(c=>c.textContent.trim()===text); return h ? h.getAttribute('col-id') : null;};"
                        + "const steps=[0,-300,-600,-900,-1200,-1500,-1800,-2100,-2400,300,600,900,1200,1500,1800,2100,2400];"
                        + "let i=0; const next=()=>{const id=find(); if(id||i>=steps.length){done(id||'');return;}"
                        + "  views.forEach(v=>{v.scrollLeft=steps[i]; v.dispatchEvent(new Event('scroll',{bubbles:true}));});"
                        + "  i++; setTimeout(next,250);}; next();",
                headerText);
        String id = colId == null ? "" : colId.toString();
        log.info("Lines column [{}] → col-id [{}]", headerText, id);
        return id;
    }

    /**
     * Line "البيان": its column is found by its header (the col-id is not
     * assumed) and the text is entered like any other line cell.
     *
     * @return the col-id used, or "" when the grid has no "البيان" column.
     */
    public String setLineNote(int row, String text) {
        String colId = revealLineColumn("البيان");
        if (colId.isEmpty()) {
            log.warn("The lines grid has no \"البيان\" column → line note not entered.");
            return "";
        }
        setLineText(row, colId, text);
        return colId;
    }

    // ----- record filters for DocumentApi -----

    /** Number of the quotation in an API record ("qCode"). */
    public static String codeOf(Map<String, Object> record) {
        Object code = record.get(CODE_FIELD);
        return code == null ? "" : code.toString();
    }

    /**
     * Not converted into a purchase order. QC 2026-11-02: the API flag
     * {@code isInvoice} is true on Approved PQ2608-000005 / PQ2608-000010 /
     * PQ2609S16-000004 / PQ2609S7-000009, and false on PQ2609S26-000001.
     * Whether other links exist is not known yet; a refused action is
     * handled by the test (it tries another quotation).
     */
    public static boolean isUnlinked(Map<String, Object> record) {
        return !isTrue(record, "isInvoice");
    }

    /** Has a requisition in "المستند المرجعي". */
    public static boolean hasReferenceDocument(Map<String, Object> record) {
        Object ref = record.get("referenceDocument");
        return ref != null && !ref.toString().isBlank();
    }
}
