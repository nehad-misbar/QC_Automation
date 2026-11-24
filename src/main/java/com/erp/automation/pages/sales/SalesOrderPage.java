package com.erp.automation.pages.sales;

import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;
import static com.erp.automation.ui.DocumentApi.isTrue;
import static com.erp.automation.ui.DocumentApi.textOf;

/**
 * Sales Order screen (المبيعات → العمليات → أمر البيع): {@code /sales/sales-order}.
 *
 * <p>Explored in QC on 2026-11-12:</p>
 * <ul>
 *     <li>data-testid prefix {@code sales-order}; number {@code #code} (SOyyMM-nnnnnn);</li>
 *     <li>API {@code POST /api/SalesOrder/filter}: 35 records, {@code recordStatus}
 *     0 = مسودة (14), 1 = معتمد (20), 2 = ملغي (1, {@code isCanceled}); flags
 *     {@code isInvoice} (a sales invoice was made from it) and {@code hasRemainingQty};</li>
 *     <li>list columns: الرقم المرجعي، التاريخ، العميل، مندوب المبيعات، تاريخ
 *     الاعتماد، الاجمالي، الإجمالي الاجنبي، المستند المرجعي، الحالة، البيان;</li>
 *     <li>header (screen order): الرقم المرجعي، العميل*، الرقم الضريبي، مندوب
 *     المبيعات، العملة، التعادل، فريق المبيعات، التاريخ، المخزن*، تاريخ الانتهاء،
 *     الفرع*، مركز التكلفة، طريقة احتساب الضريبة*، الأسعار*، تعميم خصم على
 *     الأصناف، قائمة الأسعار، شروط السداد، تاريخ الاعتماد، المستند المرجعي
 *     (two buttons: choose an approved quotation / download it)، طريقة الشحن،
 *     عنوان التوصيل، البيان، انزال من (أوامر البيع)، الرقم;</li>
 *     <li>choosing the branch empties the warehouse and limits the warehouses
 *     (فرع الرياض → مخزن 100 / مخزن 200);</li>
 *     <li>QC 2026-11-15: "فريق المبيعات" limits "مندوب المبيعات" (فريق مبيعات الرياض →
 *     4 of 32 persons), so the team comes first; "العملة" offers an empty option, and a
 *     foreign currency fills an editable "التعادل" (الدولار الأمريكي → 3.75); the branch
 *     does not fill "مركز التكلفة" on an order; "تعميم خصم على الأصناف" = نسبة % / قيمة,
 *     its amount is enabled once a type is chosen;</li>
 *     <li>lines: quantity col-id {@code qty} (not "quantity"), price {@code price};
 *     no "مميز" tab;</li>
 *     <li>save: "هل انت متاكد من عملية الحفظ؟" → نعم → "تم الحفظ بنجاح." → مسودة;
 *     approve: "هل تريد الاعتماد ؟" → موافق → معتمد, approval date today;</li>
 *     <li>toolbar: Draft → edit/delete/approve/cancel enabled. A sales invoice is
 *     made from an Approved order only (business rule, QC team 2026-11-15); a Draft
 *     with {@code isInvoice} (SO2609-000009) is a defect, not a state to test. Approved → <b>edit disabled</b>, delete
 *     disabled, disapprove/cancel enabled; Approved with {@code isInvoice} →
 *     disapprove disabled but cancel ENABLED (SO2608-000001). Cancelled →
 *     edit/delete/approve/cancel/attachments disabled;</li>
 *     <li>Draft in edit mode ({@code /edit/}): everything but the number, rate,
 *     tax number, cost centre, approval date and "المستند المرجعي"; "انزال من"
 *     is offered;</li>
 *     <li>"⋮": new → الاستيراد من Excel, متابعة من عرض سعر; saved → التصدير الى
 *     Excel, متابعة من عرض سعر (disabled), and on an Approved order
 *     "انشاء فاتورة مبيعات" → {@code /sales/invoices/add/<id>?origin=SalesOrderStore}.</li>
 * </ul>
 */
public class SalesOrderPage extends SalesDocumentPage {

    public static final String PREFIX = "sales-order";
    public static final String SCREEN_PATH = "/sales/sales-order";
    public static final String API_RESOURCE = "SalesOrder";

    public static final String QUANTITY_COLUMN = "qty";
    public static final String PRICE_COLUMN = "price";

    /*
     * Line columns (QC 2026-11-15, column definitions of the order's grid). Editable once
     * the item is chosen: الوحدة، الكمية، السعر، الخصم%، الخصم، الاضافة%، الاضافة،
     * الكمية المجانية، البيان. Not editable: العبوة، الإجمالي، الإجمالي بعد الخصم
     * والإضافة، نسبة الضريبة، ضريبة القيمة المضافة، الإجمالي بعد الخصم والإضافة والضريبة.
     */
    public static final String COL_UNIT = "uomId";
    public static final String COL_DISCOUNT_RATE = "discountRate";
    public static final String COL_DISCOUNT_VALUE = "discountValue";
    public static final String COL_EXTRA_RATE = "extraRate";
    public static final String COL_EXTRA_VALUE = "extraValue";
    public static final String COL_FREE_QUANTITY = "freeQuantities";
    public static final String COL_NOTE = "note";

    /** "تعميم خصم على الأصناف" types (QC 2026-11-15); the amount is enabled once a type is chosen. */
    public static final String DISCOUNT_PERCENT = "نسبة %";
    public static final String DISCOUNT_VALUE = "قيمة";

    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";
    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";
    public static final String MENU_FOLLOW_QUOTATION = "متابعة من عرض سعر";
    public static final String MENU_CREATE_INVOICE = "انشاء فاتورة مبيعات";

    public static final String INVOICE_ADD_PATH = "/sales/invoices/add/";

    /** The only "انزال من" source on an order (QC 2026-11-12). */
    public static final String DOWNLOAD_SOURCE = "أوامر البيع";

    /** {@code recordStatus}: 0 = مسودة, 1 = معتمد, 2 = ملغي (QC 2026-11-12). */
    public enum RecordStatus {
        DRAFT(0), APPROVED(1), CANCELLED(2);

        public final int code;

        RecordStatus(int code) {
            this.code = code;
        }

        public boolean of(Map<String, Object> record) {
            return intOf(record, "recordStatus") == code;
        }
    }

    /** Header React Select fields, in screen order. */
    public static final List<String> SELECT_FIELDS = List.of(
            "partnerId", "salesPersonId", "currencyId", "salesTeamId", "warehouseId", "branchId",
            "costCenterId", "taxCalculationMethode", "bulkDiscountType", "salePriceListId",
            "paymentTerms", "saleShippingMethodId");

    public static final List<String> READ_ONLY_FIELDS = List.of("code", "taxNumber", "currencyRate", "approvalDate");

    private static final String[] PRICE_RADIOS = {"prices-0", "prices-1"};

    public SalesOrderPage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE, "code");
    }

    public void setOrderDate(LocalDate date) {
        setDate("saleOrderDate", date);
    }

    /** "تاريخ الانتهاء" of the order. */
    public void setDeadline(LocalDate date) {
        setDate("orderDeadLine", date);
    }

    public boolean setDeliveryAddress(String address) {
        return setTextInput("deliveryAddress", address);
    }

    public String selectRandomPriceOption() {
        return clickRandomRadio(PRICE_RADIOS);
    }

    public String selectOtherPriceOption() {
        return clickOtherRadio(PRICE_RADIOS);
    }

    public String priceOption() {
        return checkedRadio(PRICE_RADIOS);
    }

    /** "انزال من" = أوامر البيع → random order → download its lines. */
    public String downloadLinesFromAnotherOrder() {
        return downloadLinesFrom(DOWNLOAD_SOURCE);
    }

    /** "تعميم خصم على الأصناف": type then amount; false when locked. */
    public boolean setBulkDiscount(String type, String amount) {
        if (isSelectDisabled("bulkDiscountType")) {
            return false;
        }
        selectOption("bulkDiscountType", type);
        confirmDialogIfShown(java.time.Duration.ofMillis(800));
        return setTextInput("bulkDiscountAmount", amount);
    }

    // ----- API record filters -----

    public static String codeOf(Map<String, Object> record) {
        return textOf(record, "code");
    }

    public static boolean isCancelled(Map<String, Object> record) {
        return isTrue(record, "isCanceled") || RecordStatus.CANCELLED.of(record);
    }

    /** A sales invoice was already made from it. */
    public static boolean isInvoiced(Map<String, Object> record) {
        return isTrue(record, "isInvoice");
    }

    /** Draft that can still be deleted / approved (no invoice made from it). */
    public static boolean isFreeDraft(Map<String, Object> record) {
        return RecordStatus.DRAFT.of(record) && !isCancelled(record) && !isInvoiced(record);
    }

    /** Approved, not cancelled, no invoice made from it. */
    public static boolean isFreeApproved(Map<String, Object> record) {
        return RecordStatus.APPROVED.of(record) && !isCancelled(record) && !isInvoiced(record);
    }
}
