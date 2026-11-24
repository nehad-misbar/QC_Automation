package com.erp.automation.pages.sales;

import org.openqa.selenium.WebDriver;

/**
 * "التسعير" (المبيعات → العمليات → التسعير): {@code /sales/pricing}.
 *
 * <p>QC 2026-11-12: data-testid prefix {@code pricing}; API
 * {@code POST /api/SalePriceOperation/filter} (62 records: 10 مسودة, 52
 * معتمد). List columns: رمز، التاريخ، نوع الصلاحية، سياسة التعديل، التطبيق
 * على، أساس المقارنة، هامش الربح، هامش الربح قيمة، نسبة هامش الربح، طريقة
 * التسعير، الحالة ({@code recordStatus}, far to the side: scrolled to) … ملاحظات
 * (QC 2026-11-19). Kanban cards show the currency and a status, no number. "⋮": الاستيراد من Excel (new) /
 * التصدير الى Excel (saved). See {@link PricingOperationPage} for the form.</p>
 */
public class PricingPage extends PricingOperationPage {

    public static final String PREFIX = "pricing";
    public static final String SCREEN_PATH = "/sales/pricing";
    public static final String API_RESOURCE = "SalePriceOperation";

    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";
    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";

    public PricingPage(WebDriver driver) {
        super(driver, PREFIX, SCREEN_PATH, API_RESOURCE);
    }
}
