package com.erp.automation.pages;

import com.erp.automation.utils.LocatorUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;

public class PurchasingPage extends BasePage {

    private final By purchasingMenu =
            LocatorUtils.xpath(
                    "//span[normalize-space()='المشتريات']/parent::div"
            );

    private final By operationsMenu =
            LocatorUtils.xpath(
                    "//div[normalize-space()='العمليات']"
            );

    private final By purchaseRequisitionsMenu =
            LocatorUtils.xpath(
                    "//button[normalize-space()='طلبات الشراء']"
            );

    private final By priceOffers =
            By.cssSelector("[data-testid='purchase-price-offers']");

    private final By purchaseOrders =
            By.cssSelector("a[data-testid='purchase-orders']");

    /**
     * Locates the Local Purchase Invoices navigation link
     * using its stable data-testid attribute.
     */
    private final By localPurchaseInvoices =
            By.cssSelector("a[data-testid='purchase-invoice-local']");

    /**
     * Locator for the Foreign Purchase Invoices navigation link
     * in the sidebar.
     */
    private final By foreignPurchaseInvoices =
            By.cssSelector("a[data-testid='purchase-invoice-foreign']");

    /**
     * Locates the Purchase Return Invoices navigation link using its stable data-testid.
     */
    private final By purchaseInvoiceReturn =
            By.cssSelector("a[data-testid='purchase-invoice-return']");

    /**
     * Locator for the Free Quantity Processing navigation tab.
     * Uses the stable data-testid attribute.
     */
    private final By freeQuantityProcessing =
            By.cssSelector("a[data-testid='purchase-process-free-qty']");

    public PurchasingPage(WebDriver driver) {
        super(driver);
    }

    public void clickPurchasing() {
        click(purchasingMenu);
    }

    public void clickOperations() {
        click(operationsMenu);
    }

    public void clickPurchaseRequisitions() {
        click(purchaseRequisitionsMenu);
    }

    public void clickPriceOffers() {click(priceOffers);}

    public void clickPurchaseOrders() {click(purchaseOrders);}

    /**
     * Opens the Local Purchase Invoices screen
     * from the Purchasing navigation menu.
     * Waits until the navigation link is clickable before clicking it.
     */
    public void clickLocalPurchaseInvoices() { click(localPurchaseInvoices); }

    /**
     * Clicks the Foreign Purchase Invoices link in the sidebar
     * to navigate to the Foreign Purchase Invoice screen.
     */
    public void clickForeignPurchaseInvoices() { click(foreignPurchaseInvoices); }

    /**
     * Navigates to the Purchase Return Invoices page by clicking its sidebar link.
     *
     * Uses the stable data-testid attribute and waits until the link is clickable
     * before clicking it.
     */
    public void clickPurchaseInvoiceReturn() { click(purchaseInvoiceReturn);}

    /**
     * Clicks the Free Quantity Processing navigation tab
     * after waiting until it becomes clickable.
     */
    public void clickFreeQuantityProcessing() { click(freeQuantityProcessing);}
}