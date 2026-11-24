package com.erp.automation.pages.sales;

import com.erp.automation.pages.BasePage;
import com.erp.automation.utils.LocatorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

/**
 * Sales module navigation: the "المبيعات" icon of the modules bar, then the
 * sidebar group "العمليات" and its screens.
 *
 * <p>Checked in QC on 2026-11-12:</p>
 * <ul>
 *     <li>the module icon is a {@code span} "المبيعات" inside a div (same
 *     markup as "المشتريات"), and opens {@code /sales/};</li>
 *     <li>the sidebar groups have stable data-testids: {@code sales-preparation}
 *     (التهيئة), {@code sales-inputs} (المدخلات), {@code sales-operations}
 *     (العمليات), {@code sales-reports} (تقارير);</li>
 *     <li>every screen link under "العمليات" has a data-testid
 *     ({@code sales-price-quotations} → {@code /sales/price-quotations}, …).</li>
 * </ul>
 */
public class SalesPage extends BasePage {

    private final By salesModule =
            LocatorUtils.xpath("//span[normalize-space()='المبيعات']/parent::div");

    private final By operationsMenu =
            By.cssSelector("[data-testid='sales-operations']");

    /** "عرض السعر" → /sales/price-quotations. */
    private final By priceQuotations =
            By.cssSelector("a[data-testid='sales-price-quotations']");

    /** "أمر البيع" → /sales/sales-order. */
    private final By salesOrders =
            By.cssSelector("a[data-testid='sales-sales-orders']");

    /** "فواتير المبيعات" → /sales/invoices. */
    private final By salesInvoices =
            By.cssSelector("a[data-testid='sales-invoices']");

    /** "مردود المبيعات" → /sales/sales-return. */
    private final By salesReturns =
            By.cssSelector("a[data-testid='sales-sales-return']");

    /** "التسعير" → /sales/pricing. */
    private final By pricing =
            By.cssSelector("a[data-testid='sales-sale-price-operation']");

    /** "طلبات تعديل الأسعار" → /sales/pricing-request. */
    private final By priceChangeRequests =
            By.cssSelector("a[data-testid='sales-sale-price-operation-request']");

    public SalesPage(WebDriver driver) {
        super(driver);
    }

    /** Modules bar → "المبيعات". */
    public void clickSales() {
        click(salesModule);
    }

    /** Sidebar → "العمليات" (opens the group with the screen links). */
    public void clickOperations() {
        click(operationsMenu);
    }

    /** "العمليات" → "عرض السعر". */
    public void clickPriceQuotations() {
        click(priceQuotations);
    }

    /** "العمليات" → "أمر البيع". */
    public void clickSalesOrders() {
        click(salesOrders);
    }

    /** "العمليات" → "فواتير المبيعات". */
    public void clickSalesInvoices() {
        click(salesInvoices);
    }

    /** "العمليات" → "مردود المبيعات". */
    public void clickSalesReturns() {
        click(salesReturns);
    }

    /** "العمليات" → "التسعير". */
    public void clickPricing() {
        click(pricing);
    }

    /** "العمليات" → "طلبات تعديل الأسعار" (/sales/pricing-request). */
    public void clickPriceChangeRequests() {
        click(priceChangeRequests);
    }
}
