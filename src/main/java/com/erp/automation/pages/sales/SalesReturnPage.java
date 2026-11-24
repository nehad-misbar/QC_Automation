package com.erp.automation.pages.sales;

import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Sales Return screen (المبيعات → العمليات → مردود المبيعات): {@code /sales/sales-return}.
 *
 * <p>Explored in QC on 2026-11-12 (see {@link SalesInvoiceDocumentPage} for
 * what invoices and returns share):</p>
 * <ul>
 *     <li>data-testid prefix {@code sales-return}; number SRIyyMM-nnnnnn;</li>
 *     <li>API {@code POST /api/SaleInvoice/filter/1?includePOS=false}: 60
 *     records, all {@code invoiceStatus} 1 (معتمد);</li>
 *     <li>same list columns as the invoices;</li>
 *     <li>line tabs: تفاصيل الفاتورة، الخصومات والإضافات;</li>
 *     <li>extra required field "سعر المرتجع" ({@code returnPrice}): اخر سعر
 *     بيع للعميل / متوسط اسعار البيع للعميل / السعر في قائمة التسعير
 *     الافتراضية / تحديد يدوي;</li>
 *     <li>"المستند المرجعي" picks a sales invoice; "انزال من": فواتير
 *     المبيعات / مردود المبيعات;</li>
 *     <li>no "نسخ" (copy) button;</li>
 *     <li>"⋮" new: استيراد من Execl (disabled); saved: تصدير الى Execl، دفع.</li>
 * </ul>
 */
public class SalesReturnPage extends SalesInvoiceDocumentPage {

    public static final String PREFIX = "sales-return";
    public static final String SCREEN_PATH = "/sales/sales-return";
    public static final String API_RESOURCE = "SaleInvoice/filter/1?includePOS=false";

    public static final String MENU_IMPORT_EXCEL = "استيراد من Execl";
    public static final String MENU_EXPORT_EXCEL = "تصدير الى Execl";
    public static final String MENU_PAY = "دفع";

    public static final String RETURN_PRICE_FIELD = "returnPrice";
    public static final List<String> RETURN_PRICE_OPTIONS = List.of(
            "اخر سعر بيع للعميل", "متوسط اسعار البيع للعميل", "السعر في قائمة التسعير الافتراضية", "تحديد يدوي");

    /** "تحديد يدوي": the price of each line is typed by the user. */
    public static final String MANUAL_RETURN_PRICE = "تحديد يدوي";

    public static final String DOWNLOAD_FROM_INVOICES = "فواتير المبيعات";
    public static final String DOWNLOAD_FROM_RETURNS = "مردود المبيعات";

    public static final List<String> LINE_TABS = List.of("تفاصيل الفاتورة", "الخصومات والإضافات");

    public SalesReturnPage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE);
    }

    /**
     * True when "سعر المرتجع" is on the form. QC run 2026-11-18 15:25 (SI2611-000225): a
     * return downloaded from an invoice ("المستند المرجعي") has no "سعر المرتجع" field.
     */
    public boolean hasReturnPrice() {
        return hasField(RETURN_PRICE_FIELD, java.time.Duration.ofSeconds(1));
    }

    /** "سعر المرتجع" at random (when shown and editable); returns the option ("" when not shown). */
    public String selectRandomReturnPrice() {
        if (!hasReturnPrice()) {
            log.info("\"سعر المرتجع\" is not shown → not chosen.");
            return "";
        }
        if (!waitForSelectEnabled(RETURN_PRICE_FIELD, java.time.Duration.ofSeconds(3))) {
            return selectedText(RETURN_PRICE_FIELD);
        }
        String option = selectRandomOption(RETURN_PRICE_FIELD);
        confirmDialogIfShown(java.time.Duration.ofMillis(800));
        return option;
    }

    /** "سعر المرتجع" = {@code option} (one of {@link #RETURN_PRICE_OPTIONS}); returns the option shown. */
    public String selectReturnPrice(String option) {
        if (!hasReturnPrice()) {
            throw new IllegalStateException("\"سعر المرتجع\" is not shown; cannot choose " + option + ".");
        }
        if (!waitForSelectEnabled(RETURN_PRICE_FIELD, java.time.Duration.ofSeconds(5))) {
            throw new IllegalStateException("\"سعر المرتجع\" is locked; cannot choose " + option + ".");
        }
        if (!option.equals(selectedText(RETURN_PRICE_FIELD))) {
            selectOption(RETURN_PRICE_FIELD, option);
            confirmDialogIfShown(java.time.Duration.ofMillis(800));
        }
        waitForGridToSettle();
        return selectedText(RETURN_PRICE_FIELD);
    }
}
