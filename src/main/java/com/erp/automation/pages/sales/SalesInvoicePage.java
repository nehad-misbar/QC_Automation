package com.erp.automation.pages.sales;

import org.openqa.selenium.WebDriver;

import java.util.List;

/**
 * Sales Invoice screen (المبيعات → العمليات → فواتير المبيعات): {@code /sales/invoices}.
 *
 * <p>Explored in QC on 2026-11-12 (see {@link SalesInvoiceDocumentPage} for
 * what invoices and returns share):</p>
 * <ul>
 *     <li>data-testid prefix {@code invoices}; number SIyyMM-nnnnnn;</li>
 *     <li>API {@code POST /api/SaleInvoice/filter/0?includePOS=false}: 215
 *     records, all {@code invoiceStatus} 1 (معتمد), none cancelled;</li>
 *     <li>list columns: الرقم المرجعي، التاريخ، المستند المرجعي، العميل،
 *     المخزن، الفرع، حالة الفاتورة، حالة التسليم، عنوان التسليم، مندوب المبيعات;</li>
 *     <li>line tabs: تفاصيل الفاتورة، الخصومات والإضافات، شروط السداد و جدولة
 *     الدفعات، الدفعات;</li>
 *     <li>new invoice: "حفظ" disabled until something is changed; an empty save
 *     says "تفاصيل الفاتورة: يجب اضافة صنف واحد على الأقل" and marks الفرع /
 *     المخزن / العميل; العميل fills الرقم الضريبي and حساب العميل; الفرع
 *     limits the warehouses and fills مركز التكلفة;</li>
 *     <li>"⋮" new: استيراد من Execl (disabled), متابعة من أمر بيع; saved:
 *     تصدير الى Execl، انشاء مردود مبيعات
 *     ({@code /sales/sales-return/add/<id>?origin=invoice})، إنشاء إشعار مدين،
 *     إنشاء إشعار دائن، دفع (disabled when paid)، إنشاء أمر تسليم;</li>
 *     <li>"انزال من": فواتير المبيعات / مردود المبيعات;</li>
 *     <li>"قيد اليومية" of SI2611-000217: عملاء 23 مدين = المبيعات 20 + الضريبة 3 دائن.</li>
 * </ul>
 */
public class SalesInvoicePage extends SalesInvoiceDocumentPage {

    public static final String PREFIX = "invoices";
    public static final String SCREEN_PATH = "/sales/invoices";
    public static final String API_RESOURCE = "SaleInvoice/filter/0?includePOS=false";

    /** The menu labels are spelled "Execl" in the app (QC 2026-11-12). */
    public static final String MENU_IMPORT_EXCEL = "استيراد من Execl";
    public static final String MENU_EXPORT_EXCEL = "تصدير الى Execl";
    public static final String MENU_FOLLOW_ORDER = "متابعة من أمر بيع";
    public static final String MENU_CREATE_RETURN = "انشاء مردود مبيعات";
    public static final String MENU_DEBIT_NOTE = "إنشاء إشعار مدين";
    public static final String MENU_CREDIT_NOTE = "إنشاء إشعار دائن";
    public static final String MENU_PAY = "دفع";
    public static final String MENU_DELIVERY_ORDER = "إنشاء أمر تسليم";

    public static final String RETURN_ADD_PATH = "/sales/sales-return/add/";

    public static final List<String> DOWNLOAD_SOURCES = List.of("فواتير المبيعات", "مردود المبيعات");

    public static final List<String> LINE_TABS = List.of(
            "تفاصيل الفاتورة", "الخصومات والإضافات", "شروط السداد و جدولة الدفعات", "الدفعات");

    public SalesInvoicePage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE);
    }

    // =====================================================================
    // "شامل نقاط البيع" (QC 2026-11-17): a switch beside "جديد" on the list;
    // on → the list also shows the POS invoices (API includePOS=true): 271 → 664.
    // =====================================================================

    public static final String POS_SWITCH_LABEL = "شامل نقاط البيع";

    /** The switch whose container also holds the label "شامل نقاط البيع" (null when absent). */
    private static final String POS_SWITCH_JS =
            "const lab=[...document.querySelectorAll('label, span, p, div')]"
                    + "  .find(e=>e.childElementCount===0 && e.textContent.trim()===arguments[0]);"
                    + "let sw=null, p=lab; for(let i=0;i<5&&p&&!sw;i++){ p=p.parentElement; sw=p&&p.querySelector('[role=switch]'); }";

    /** True when the switch is shown and on. */
    public boolean isPosSwitchOn() {
        Object on = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(POS_SWITCH_JS
                + "return !!sw && (sw.getAttribute('data-state')==='checked' || sw.getAttribute('aria-checked')==='true');",
                POS_SWITCH_LABEL);
        return Boolean.TRUE.equals(on);
    }

    /**
     * Where the switch, its label and "جديد" are, for the layout check:
     * {newX, newY, newW, newH, switchX, switchY, switchW, switchH, labelX, labelW}; empty when one is missing.
     */
    @SuppressWarnings("unchecked")
    public List<Number> posSwitchLayout() {
        Object rects = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(POS_SWITCH_JS
                + "const nb=[...document.querySelectorAll('button')].find(b=>b.innerText.trim()==='جديد' && b.offsetParent!==null);"
                + "if(!sw||!nb||!lab) return [];"
                + "const a=nb.getBoundingClientRect(), b=sw.getBoundingClientRect(), c=lab.getBoundingClientRect();"
                + "return [a.x,a.y,a.width,a.height,b.x,b.y,b.width,b.height,c.x,c.width];",
                POS_SWITCH_LABEL);
        return rects == null ? List.of() : (List<Number>) rects;
    }

    /** Turns the switch on / off (a click), then waits until the list total changes or settles. */
    public void setPosSwitch(boolean on) {
        if (isPosSwitchOn() == on) {
            return;
        }
        int before = listTotalOrMinus1();
        org.openqa.selenium.WebElement sw = (org.openqa.selenium.WebElement) ((org.openqa.selenium.JavascriptExecutor) driver)
                .executeScript(POS_SWITCH_JS + "return sw;", POS_SWITCH_LABEL);
        if (sw == null) {
            throw new IllegalStateException("The switch \"" + POS_SWITCH_LABEL + "\" is not on the list.");
        }
        sw.click();
        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, java.time.Duration.ofSeconds(10))
                    .pollingEvery(java.time.Duration.ofMillis(300))
                    .until(d -> {
                        int now = listTotalOrMinus1();
                        return isPosSwitchOn() == on && now >= 0 && now != before;
                    });
        } catch (org.openqa.selenium.TimeoutException sameTotal) {
            log.warn("\"{}\" → {}: the list total stayed {}.", POS_SWITCH_LABEL, on, before);
        }
        waitForListLoaded();
        log.info("\"{}\" = {} → list total {}", POS_SWITCH_LABEL, on ? "on" : "off", listTotalOrMinus1());
    }

    /**
     * List total ("عرض x - y / z"), or -1 while the list is (re)loading and shows no
     * summary. QC run 2026-11-17 18:23: right after the switch was clicked the list showed
     * "جاري التحميل ..." and the wait ended with "Pagination summary … is not displayed".
     */
    private int listTotalOrMinus1() {
        try {
            return paginationRange()[2];
        } catch (IllegalStateException loading) {
            return -1;
        }
    }

    // =====================================================================
    // ⋮ → "استيراد من Execl" on a new invoice (QC 2026-11-17): enabled once الفرع,
    // المخزن and العميل are chosen. Dialog "الاستيراد من Excel": "تحميل النموذج"
    // (Example-Template.xlsx: row 2 = headers, كود الصنف … البيان), a file input
    // (.xls/.xlsx), "الاستيراد من Excel" and "إلغاء".
    // =====================================================================

    // The Excel import methods (openImportFromExcel, importDialogButtons, downloadImportTemplate,
    // importExcelFile) are in SalesInvoiceDocumentPage since 2026-11-19: the sales return imports too.

    // =====================================================================
    // "دفع" dialog (QC 2026-11-17, SI2611-000270): ⋮ → "دفع" on an approved, unpaid
    // invoice opens a dialog titled "دفع" over the invoice: طريقة الدفع (نقدا / بنك /
    // شيك / نقدا / بنك), حساب الدفع (options load on a real click), المبلغ*
    // (#cashAmount, 0), العملة, التعادل (1), البيان ("سداد فاتورة بيع رقم …") and the
    // buttons "دفع" and "Close". "نقدا / بنك" adds a second حساب الدفع
    // (#banckAccountId) and المبلغ (#otherAmount). Its fields have the SAME
    // data-testid as the invoice header ("invoices-paymentMethod" twice), so every
    // lookup below is scoped to the dialog. Escape closes the whole dialog.
    // QC run 2026-11-17 17:39: with "بنك" the dialog has no #cashAmount (its المبلغ is
    // another input, plus "اشعار التحويل"), so the dialog is found by its طريقة الدفع.
    // =====================================================================

    // The "دفع" dialog methods (waitForPayDialog, payDialog…, submitPayDialog, closePayDialog) are in
    // SalesInvoiceDocumentPage since 2026-11-19: the sales return pays through the same dialog.

    private static void pauseQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
