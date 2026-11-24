package com.erp.automation.pages.sales;

import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;
import static com.erp.automation.ui.DocumentApi.isTrue;
import static com.erp.automation.ui.DocumentApi.textOf;

/**
 * Sales Price Quotation screen (المبيعات → العمليات → عرض السعر):
 * {@code /sales/price-quotations}.
 *
 * <p>Everything common to document screens is in
 * {@link SalesDocumentPage} / {@code PurchasingDocumentPage}. This class adds
 * only what is specific to sales quotations.</p>
 *
 * <p>Explored in QC on 2026-11-12:</p>
 * <ul>
 *     <li>data-testid prefix {@code price-quotations}; the number is
 *     {@code #code} on the form and col-id {@code code} in the list
 *     (SQyyMM-nnnnnn);</li>
 *     <li>API resource {@code SalesQuotation} ({@code POST /api/SalesQuotation/filter}),
 *     24 records: {@code recordStatus} 0 (11, مسودة) and 1 (13, معتمد);
 *     flags {@code isApproved}, {@code isCanceled}, {@code isInvoice};</li>
 *     <li><b>"منتهي" is shown by the FORM when "تاريخ الانتهاء" is before
 *     today</b>, for Drafts and Approved ones alike, while the list and the
 *     API still say مسودة / معتمد (SQ2608-000016: list مسودة, form منتهي);</li>
 *     <li>required (label *): العميل، العملة، الفرع، المخزن، طريقة احتساب
 *     الضريبة، الأسعار. An empty save shows "تفاصيل عروض السعر: يجب اضافة صنف
 *     واحد على الأقل" and marks العميل / الفرع / المخزن "هذا الحقل مطلوب";</li>
 *     <li>the warehouse list depends on the branch; choosing the branch also
 *     fills (and locks) "مركز التكلفة";</li>
 *     <li>"اضافة سطر" appears once العميل / الفرع / المخزن are chosen;</li>
 *     <li>save → "هل انت متاكد من عملية الحفظ؟" (نعم); a "تاريخ الانتهاء"
 *     later than the sales-period setting (today + 20 days) asks again
 *     "هل تريد المتابعة؟ الموعد النهائي لعرض السعر يتجاوز الموعد المحدد لمهلة
 *     البيع في الاعدادات" (متابعة) and is then saved; toast "تم الحفظ بنجاح.";</li>
 *     <li>approve → "هل تريد الاعتماد ؟" (موافق); approval date = today;</li>
 *     <li>Draft in edit mode ({@code /edit/}): التاريخ and most fields editable;
 *     طريقة الشحن، تاريخ الانتهاء، قالب عرض السعر، المستند المرجعي locked;
 *     no "انزال من";</li>
 *     <li>Approved in edit mode ({@code /view-approved/}): البيان only;</li>
 *     <li>"⋮": الاستيراد من Excel (new) / التصدير الى Excel (saved),
 *     ارسال للعميل لغرض التوقيع (disabled on every quotation seen),
 *     انشاء امر بيع (enabled only on an Approved, not expired, not invoiced
 *     quotation; opens {@code /sales/sales-order/add/<id>} with prefix
 *     {@code sales-order} and "المستند المرجعي" = the quotation number);</li>
 *     <li>"انزال من" offers only "عروض أسعار المبيعات"; "المستند المرجعي" is
 *     a free text field;</li>
 *     <li>the list has no "الملخصات"; group by works (filter → تجميع حسب).</li>
 * </ul>
 */
public class SalesQuotationPage extends SalesDocumentPage {

    public static final String PREFIX = "price-quotations";
    public static final String SCREEN_PATH = "/sales/price-quotations";
    public static final String API_RESOURCE = "SalesQuotation";
    public static final String CODE_FIELD = "code";

    /** {@code recordStatus} of the API: 0 = مسودة, 1 = معتمد (QC 2026-11-12). */
    public enum RecordStatus {
        DRAFT(0), APPROVED(1);

        public final int code;

        RecordStatus(int code) {
            this.code = code;
        }

        /** Same recordStatus and not cancelled. */
        public boolean of(Map<String, Object> record) {
            return intOf(record, "recordStatus") == code && !isCancelled(record);
        }
    }

    /** Header fields that are React Select dropdowns, in screen order (right to left, row by row). */
    public static final List<String> SELECT_FIELDS = List.of(
            "shippingMethodId", "currencyId", "paymentTerms",
            "salesRepresentativeId", "partnerId",
            "bulkDiscountType", "saleTeamId", "branchId",
            "taxCalculationMethode", "salesQuotationTemplateId", "costCenterId", "priceListId",
            "warehouseId");

    /** Read-only fields filled by the system. */
    public static final List<String> READ_ONLY_FIELDS = List.of("code", "conversionRate", "approvalDate", "taxNumber");

    /** "الأسعار": باستثناء الضريبة (prices-0) / شاملة الضريبة (prices-1). */
    private static final String[] PRICE_RADIOS = {"prices-0", "prices-1"};

    /** "تعميم خصم على الأصناف": نسبة % / قيمة. */
    public static final String DISCOUNT_PERCENT = "نسبة %";
    public static final String DISCOUNT_VALUE = "قيمة";

    /**
     * "مندوب المبيعات" used by the tests. QC 2026-11-12: with many other reps
     * (e.g. "vbfgg", "jgh") "فريق المبيعات" has no option and "حفظ" opens no
     * confirmation (known bug, worked around as agreed with QC). "محمود السيد"
     * fills "فريق مبيعات الرياض" by himself and the quotation saves.
     */
    public static final String SALES_REPRESENTATIVE = "محمود السيد";

    /**
     * Price list used when the chosen one offers no item in "اسم الصنف"
     * (QC 2026-11-12: "قائمة د" + "مخزن 100" gave an empty item list).
     */
    public static final String DEFAULT_PRICE_LIST = "القائمة الافتراضية";

    /**
     * Item cells that can be edited on a line with an item, in both
     * "قائمة الأصناف" and "مميز" (AG Grid column definitions, QC 2026-11-12).
     * Not editable: العبوة, الإجمالي, معدل الضريبة, ضريبة القيمة المضافة, الإجمالي بعد ….
     * Discount and addition are each a pair: the rate fills the value and the value fills the rate.
     */
    public static final String COL_UNIT = "uomId";
    public static final String COL_DISCOUNT_RATE = "discountRate";
    public static final String COL_DISCOUNT_VALUE = "discountValue";
    public static final String COL_EXTRA_RATE = "extraRate";
    public static final String COL_EXTRA_VALUE = "extraValue";
    public static final String COL_FREE_QUANTITY = "freeQuantities";
    public static final String COL_NOTE = "note";

    /** "⋮" items. */
    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";
    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";
    public static final String MENU_SEND_FOR_SIGNATURE = "ارسال للعميل لغرض التوقيع";
    public static final String MENU_CREATE_SALES_ORDER = "انشاء امر بيع";

    /** "انشاء امر بيع" opens this path + the quotation id; the order form uses this prefix. */
    public static final String SALES_ORDER_ADD_PATH = "/sales/sales-order/add/";
    public static final String SALES_ORDER_PREFIX = "sales-order";

    /** The only "انزال من" source on a sales quotation (QC 2026-11-12). */
    public static final String DOWNLOAD_SOURCE = "عروض أسعار المبيعات";

    public SalesQuotationPage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE, CODE_FIELD);
    }

    // ----- header -----

    /** "التاريخ" (today on a new quotation; editable on a Draft). */
    public void setQuotationDate(LocalDate date) {
        setDate("quotationDate", date);
    }

    /** "تاريخ الانتهاء". After it the form shows "منتهي". Locked in edit mode. */
    public void setExpiryDate(LocalDate date) {
        setDate("quotationExpiryDate", date);
    }

    /** "عنوان التوصيل" (free text). */
    public boolean setDeliveryAddress(String address) {
        return setTextInput("deliveryAddress", address);
    }

    /** "المستند المرجعي" (free text on sales quotations). */
    public boolean setReferenceDocument(String text) {
        return setTextInput("referenceDocument", text);
    }

    /**
     * "تعميم خصم على الأصناف": type ({@link #DISCOUNT_PERCENT} / {@link #DISCOUNT_VALUE})
     * then the amount (enabled once a type is chosen).
     *
     * @return false when the type is locked.
     */
    public boolean setBulkDiscount(String type, String amount) {
        if (isSelectDisabled("bulkDiscountType")) {
            log.info("\"تعميم خصم على الأصناف\" is locked → not changed.");
            return false;
        }
        selectOption("bulkDiscountType", type);
        confirmDialogIfShown(java.time.Duration.ofMillis(800));
        return setTextInput("bulkDiscountAmount", amount) || setBulkDiscountAmountByTestId(amount);
    }

    /** The amount input has a data-testid but no id (QC 2026-11-12). */
    private boolean setBulkDiscountAmountByTestId(String amount) {
        org.openqa.selenium.WebElement input = new org.openqa.selenium.support.ui.WebDriverWait(driver, WAIT)
                .until(org.openqa.selenium.support.ui.ExpectedConditions.presenceOfElementLocated(byTestId("bulkDiscountAmount")));
        if (!input.isEnabled()) {
            return false;
        }
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const i=arguments[0];"
                        + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(i, arguments[1]);"
                        + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('change',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('blur',{bubbles:true}));",
                input, amount);
        log.info("bulkDiscountAmount = {}", amount);
        return true;
    }

    /** Value of "تعميم خصم على الأصناف" amount ("" when empty). */
    public String bulkDiscountAmount() {
        return driver.findElements(byTestId("bulkDiscountAmount")).stream()
                .map(e -> e.getAttribute("value")).findFirst().orElse("");
    }

    /** "الأسعار" at random; returns the radio id. */
    public String selectRandomPriceOption() {
        return clickRandomRadio(PRICE_RADIOS);
    }

    /** "الأسعار" = باستثناء الضريبة (prices-0). */
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

    /** "انزال من" = "عروض أسعار المبيعات" → random quotation → download its lines. */
    public String downloadLinesFromAnotherQuotation() {
        return downloadLinesFrom(DOWNLOAD_SOURCE);
    }

    // ----- record filters for DocumentApi -----

    /** Number of the quotation in an API record. */
    public static String codeOf(Map<String, Object> record) {
        return textOf(record, CODE_FIELD);
    }

    /** "تاريخ الانتهاء" of an API record, or {@code null}. */
    public static LocalDate expiryOf(Map<String, Object> record) {
        String value = textOf(record, "quotationExpiryDate");
        return value.length() >= 10 ? LocalDate.parse(value.substring(0, 10)) : null;
    }

    /** The form shows "منتهي": "تاريخ الانتهاء" is before today (QC 2026-11-12). */
    public static boolean isExpired(Map<String, Object> record) {
        LocalDate expiry = expiryOf(record);
        return expiry != null && expiry.isBefore(LocalDate.now());
    }

    public static boolean isCancelled(Map<String, Object> record) {
        return isTrue(record, "isCanceled");
    }

    /** Draft whose form shows "مسودة" (not expired, not cancelled). */
    public static boolean isActiveDraft(Map<String, Object> record) {
        return RecordStatus.DRAFT.of(record) && !isExpired(record);
    }

    /** Approved whose form shows "معتمد" (not expired, not cancelled). */
    public static boolean isActiveApproved(Map<String, Object> record) {
        return RecordStatus.APPROVED.of(record) && !isExpired(record);
    }

    /** Not turned into a sales order / invoice ({@code isInvoice} false). */
    public static boolean isUnlinked(Map<String, Object> record) {
        return !isTrue(record, "isInvoice");
    }
}
