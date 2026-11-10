package com.erp.automation.pages;

import org.openqa.selenium.WebDriver;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.intOf;
import static com.erp.automation.ui.DocumentApi.isTrue;

/**
 * Purchase Requisition screen (طلبات الشراء): {@code /purchase/requisitions}.
 *
 * <p>Everything common to Purchasing documents is in
 * {@link PurchasingDocumentPage}. This class adds only what is specific to
 * requisitions: its header fields, its statuses and its API flags.</p>
 *
 * <p>The old page object (69 KB, fixed sleeps, one method per field) was
 * replaced on 2026-11-01. It is kept in git history.</p>
 */
public class PurchaseRequisitionPage extends PurchasingDocumentPage {

    public static final String PREFIX = "requisitions";

    /**
     * {@code recordStatus} values of the API, checked against the status label
     * shown on the form (QC, 2026-11-01):
     * 0 = مسودة, 1 = معتمد, 2 = ملغي, 3 = منتهي.
     * "منتهي" is set when the request end date has passed.
     */
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

    /** Header fields that are React Select dropdowns. */
    public static final List<String> SELECT_FIELDS = List.of(
            "operationTypeCategoryId", "vendorId", "currencyId",
            "purchaseRepresentativeId", "branchId", "warehouseId", "costCenterId");

    public PurchaseRequisitionPage(WebDriver driver) {
        super(driver, PREFIX, "/purchase/requisitions", "PurchasRequisitions");
    }

    // ----- header -----

    /** "اخر موعد للاستلام". */
    public void setExpectedReceiptDate(LocalDate date) {
        setDate("expectedReceiptDate", date);
    }

    /** "التاريخ" (request date; today by default). */
    public void setRequestDate(LocalDate date) {
        setDate("requestDate", date);
    }

    /** "تاريخ الانتهاء". After it the requisition becomes "منتهي". */
    public void setRequestEndDate(LocalDate date) {
        setDate("requestEndDate", date);
    }

    /** "الاسعار": باستثناء الضريبة (r_1) or شاملة الضريبة (r_2), at random. */
    public String selectRandomPriceOption() {
        return clickRandomRadio("r_1", "r_2");
    }

    /** "الاسعار": the option that is not selected now (edit mode). */
    public String selectOtherPriceOption() {
        return clickOtherRadio("r_1", "r_2");
    }

    /** Checked "الاسعار" option: r_1 (باستثناء الضريبة), r_2 (شاملة الضريبة) or "". */
    public String priceOption() {
        return checkedRadio("r_1", "r_2");
    }

    /** Date fields of the header, in screen order. */
    public static final List<String> DATE_FIELDS = List.of(
            "expectedReceiptDate", "requestDate", "requestEndDate");

    /** Current value of a date field, or {@code null} when empty / not a date. */
    public LocalDate dateValue(String field) {
        try {
            return LocalDate.parse(inputValue(field));
        } catch (RuntimeException e) {
            return null;
        }
    }

    // ----- record filters for DocumentApi -----

    /** Draft or Approved with nothing linked to it. These can be edited, approved or cancelled freely. */
    public static boolean isUnlinked(Map<String, Object> record) {
        return !isTrue(record, "isInvoice") && intOf(record, "totalQutotaionCount") <= 0;
    }

    public static boolean hasStatus(Map<String, Object> record, RecordStatus status) {
        return status.of(record);
    }
}
