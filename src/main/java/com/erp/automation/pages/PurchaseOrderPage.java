package com.erp.automation.pages;

import com.aventstack.extentreports.util.Assert;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PurchaseOrderPage extends BasePage {

    // Shared screen components (item 3, 2026-10-31). Other screens create their own.
    private final com.erp.automation.ui.UiWaits ui = new com.erp.automation.ui.UiWaits(driver);
    private final com.erp.automation.ui.ApiRecorder apiRecorder = new com.erp.automation.ui.ApiRecorder(driver);
    private final com.erp.automation.ui.Downloads downloads = new com.erp.automation.ui.Downloads(driver);
    private final com.erp.automation.ui.DocumentList list = new com.erp.automation.ui.DocumentList(driver);
    private final com.erp.automation.ui.DocumentHeader header = new com.erp.automation.ui.DocumentHeader(driver, "orders");

    /**
     * Stores the unique data-testid of the Item cell in the newly added
     * Purchase Order row. This value is used as the stable anchor for
     * locating other cells within the same row after AG Grid re-renders.
     */
    private String newItemTestId;

    /**
     * Stores the newly added Purchase Order grid row.
     * The row is captured after clicking the "Add Line" button and is reused
     * by item-selection methods to avoid dynamic row locators.
     */
    private WebElement newRow;
    /**
     * Stores the AG Grid row index of the newly added Purchase Order item row.
     * This index allows the framework to re-locate the row after AG Grid
     * re-renders the DOM and invalidates the previously captured WebElement.
     */
    private int newRowIndex;

    /**
     * Stores the row index of the newly created row in the
     * Discounts & Additions AG Grid.
     *
     * This value is intentionally separate from newRowIndex because
     * the Purchase Order items grid and the Discounts & Additions grid
     * maintain independent AG Grid row indexes.
     */
    private int newDiscountAdditionRowIndex = -1;

    public PurchaseOrderPage(WebDriver driver) {
        super(driver);
    }

    private final By newButton =
            By.xpath("//button[normalize-space()='جديد' or .//span[normalize-space()='جديد']]");

    /**
     * Locator for the Purchase Order "Operation Type" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Operation Type field.
     */
    private final By operationTypeDropdown =
            By.cssSelector("[data-testid='orders-operationTypeCategoryId']");

    /**
     * Locator for the visible Operation Type options displayed by React Select.
     * The locator is intentionally resolved again during each selection attempt
     * because React may recreate the option elements during re-rendering.
     */
    private final By operationTypeOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for a specific Operation Type option by its visible text.
     * The option is located again at click time to avoid stale WebElement references
     * caused by React Select re-rendering the dropdown options.
     */
    private By operationTypeOptionByText(String optionText) {
        return By.xpath(
                "//div[contains(@class,'select__option') and normalize-space()="
                        + "\"" + optionText + "\"" + "]"
        );
    }

    /**
     * Locator for the Purchase Order "Supplier" React Select dropdown.
     * Uses the stable data-testid attribute to identify the supplier field.
     */
    private final By supplierDropdown =
            By.cssSelector("[data-testid='orders-vendorId']");

    /**
     * Locator for the visible Supplier options displayed by the React Select dropdown.
     * Used after opening the Supplier dropdown to retrieve all available suppliers.
     */
    private final By supplierOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Branch" React Select dropdown.
     * Uses the stable data-testid attribute to identify the branch field.
     */
    private final By branchDropdown =
            By.cssSelector("[data-testid='orders-branchId']");

    /**
     * Locator for the visible Branch options displayed by the React Select dropdown.
     * Used after opening the Branch dropdown to retrieve all available branches.
     */
    private final By branchOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Warehouse" React Select dropdown.
     * Uses the stable data-testid attribute to identify the warehouse field.
     */
    private final By warehouseDropdown =
            By.cssSelector("[data-testid='orders-warehouseId']");

    /**
     * Locates visible React Select options in the currently open dropdown.
     * The selector targets actual options and excludes the "no options" notice.
     */
    private final By warehouseOptions =
            By.cssSelector(
                    "div.select__menu-portal " +
                            "div[class*='select__option']"
            );

    /**
     * Locator for the Purchase Order "Currency" React Select dropdown.
     * Uses the stable data-testid attribute to identify the currency field.
     */
    private final By currencyDropdown =
            By.cssSelector("[data-testid='orders-currencyId']");

    /**
     * Locator for the visible Currency options displayed by the React Select dropdown.
     * Used after opening the Currency dropdown to retrieve all available currencies.
     */
    private final By currencyOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Cost Center" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Cost Center field.
     */
    private final By costCenterDropdown =
            By.cssSelector("[data-testid='orders-costCenterId']");

    /**
     * Locator for the visible Cost Center options displayed by the React Select dropdown.
     * Used after opening the Cost Center dropdown to retrieve all available cost centers.
     */
    private final By costCenterOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Tax Calculation Method" React Select dropdown.
     * Uses the stable data-testid attribute to identify the tax calculation method field.
     */
    private final By taxCalculationMethodDropdown =
            By.cssSelector("[data-testid='orders-taxCalculationMethode']");

    /**
     * Locator for the visible Tax Calculation Method options displayed by the React Select dropdown.
     * Used after opening the dropdown to retrieve all available tax calculation methods.
     */
    private final By taxCalculationMethodOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Order Date" field.
     * Uses the stable data-testid attribute to identify the date field container.
     */
    private final By purchaseOrderDateField =
            By.cssSelector("[data-testid='orders-orderDate'] input#orderDate");

    /**
     * Locator for the Purchase Order "Deadline" date field.
     * Uses the stable data-testid attribute of the field container
     * and targets its date input directly.
     */
    private final By purchaseOrderDeadlineField =
            By.cssSelector("[data-testid='orders-quotationtDeadline'] input");

    /**
     * Locator for the Purchase Order "Purchase Representative" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Purchase Representative field.
     */
    private final By purchaseRepresentativeDropdown =
            By.cssSelector("[data-testid='orders-purchaseRepresentativeId']");

    /**
     * Locator for the visible Purchase Representative options displayed by the React Select dropdown.
     * Used after opening the dropdown to retrieve all available purchase representatives.
     */
    private final By purchaseRepresentativeOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the "Prices" radio button group in the Purchase Order form.
     * Uses the stable data-testid attribute of the radio group container.
     */
    private final By pricesRadioGroup =
            By.cssSelector("[data-testid='orders-prices']");

    /**
     * Locator for the "Prices Excluding Tax" radio button.
     * Represents the first Purchase Order price option.
     */
    private final By pricesExcludingTaxRadio =
            By.cssSelector("[data-testid='orders-prices-0']");

    /**
     * Locator for the "Prices Including Tax" radio button.
     * Represents the second Purchase Order price option.
     */
    private final By pricesIncludingTaxRadio =
            By.cssSelector("[data-testid='orders-prices-1']");

    /**
     * Locator for the Purchase Order "Supplier Invoice Number" field.
     * Uses the stable data-testid attribute to identify the supplier invoice number input.
     */
    private final By supplierInvoiceNumberField =
            By.cssSelector("[data-testid='orders-vendorInvoiceCode']");


    /**
     * Locates the enabled "Download Item" button by its unique Download icon.
     *
     * The button is initially disabled and becomes enabled after a valid
     * Reference Number is selected. Therefore, the locator explicitly
     * excludes buttons that still have the disabled attribute.
     *
     * This prevents Selenium from matching another disabled Download button
     * when multiple Download icons exist on the Purchase Order page.
     */
    private By downloadItemButtonLocator =
            By.cssSelector(
                    "button:not([disabled]):has(svg.lucide-download)"
            );



    /**
     * Locator for the Purchase Order "Item Costs Method" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Item Costs Method field.
     */
    private final By itemsCostsMethodDropdown =
            By.cssSelector("[data-testid='orders-itemsCostsMethode']");

    /**
     * Locator for the visible Item Costs Method options displayed by React Select.
     * The options are resolved again during each selection attempt because React
     * may recreate the option elements during dropdown rendering.
     */
    private final By itemsCostsMethodOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Bulk Discount Type" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Bulk Discount Type field.
     */
    private final By bulkDiscountTypeDropdown =
            By.cssSelector("[data-testid='orders-bulkDiscountType']");

    /**
     * Locator for the visible Bulk Discount Type options displayed by React Select.
     * The options are resolved again during each selection attempt because React
     * may recreate the option elements during dropdown rendering.
     */
    private final By bulkDiscountTypeOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Bulk Discount Amount" input field.
     * Uses the stable data-testid attribute to identify the discount amount field.
     */
    private final By bulkDiscountAmountField =
            By.cssSelector("[data-testid='orders-bulkDiscountAmount']");

    /**
     * Locator for the Purchase Order "Tag" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Tag field.
     */
    private final By tagDropdown =
            By.cssSelector("[data-testid='orders-tagId']");

    /**
     * Locator for the visible Tag options displayed by the React Select dropdown.
     * The options are resolved again during each selection attempt because React
     * may recreate the option elements during dropdown rendering.
     */
    private final By tagOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the Purchase Order "Download From" React Select dropdown.
     * Uses the stable data-testid attribute to identify the Download From field.
     */
    private final By downloadFromDropdown =
            By.cssSelector("[data-testid='orders-downloadFrom']");

    /**
     * Locator for the visible Download From options displayed by React Select.
     * The options are resolved again during each selection attempt because React
     * may recreate the option elements during dropdown rendering.
     */
    private final By downloadFromOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locator for the button used to open the Reference Number Grid.
     * Uses the stable data-testid of the Reference Number field and targets
     * the first button inside its container, which represents the grid button.
     */
    private final By openReferenceNumberGridButton =
            By.cssSelector(
                    "[data-testid='orders-referenceField']" +
                            "+ div > button:first-child"
            );

    /**
     * Locator for all Reference Number radio buttons displayed in the
     * pinned-right section of the AG Grid.
     * Uses the stable radio role to identify the selectable reference rows.
     */
    private final By referenceNumberRadioButtons =
            By.cssSelector(
                    "div.ag-pinned-right-cols-container " +
                            "button[role='radio']"
            );

    /**
     * Locator for the Purchase Order "Description" (البيان) field.
     * Uses the stable data-testid attribute to identify the description input.
     */
    private final By descriptionField =
            By.cssSelector("[data-testid='orders-note']");

    /**
     * Locator for the "Add Line" (اضافة سطر) button in the Purchase Order form.
     * Uses the stable visible button text because the data-testid is dynamically generated.
     */
    private final By addLineButton =
            By.xpath("//button[normalize-space()='اضافة سطر']");

    /**
     * Locator for the Item Name cell inside the newly added Purchase Order row.
     * Uses the stable AG Grid column identifier "itemId" and relies on the
     * previously captured newRow instead of any dynamically generated identifier.
     */
    private final By itemNameCell =
            By.cssSelector(
                    "div[role='gridcell'][col-id='itemId']"
            );

    /**
     * Locator for the Quantity cell in the ERP items AG Grid.
     * Uses the stable AG Grid column identifier instead of dynamic IDs or test IDs.
     */
    private final By quantityCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='quantity']"
            );

    /**
     * Locator for the Quantity input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and the input data-ref.
     */
    private final By quantityEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='quantity'].ag-cell-inline-editing " +
                            "input.ag-input-field-input.ag-text-field-input[data-ref='eInput']"
            );

    /**
     * Locates the Quantity cell specifically within the newly added
     * Purchase Order row.
     *
     * The Item cell identified by newItemTestId is used as the stable
     * row anchor, then the Quantity cell is located inside that same row.
     *
     * This prevents Duplicate Purchase Order operations from editing
     * Quantity in one of the existing copied rows.
     */
    private By getQuantityCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='quantity']"
        );
    }

    /**
     * Locator for the Cost cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic test IDs.
     */
    private final By costCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='cost']"
            );

    /**
     * Locates the Cost cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The Item cell's unique data-testid stored in newItemTestId
     * is used as the stable row anchor. The Cost cell is then
     * located within that same logical row.
     *
     * This prevents the Price value from being entered into one
     * of the existing duplicated Purchase Order rows.
     */
    private By getCostCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='cost']"
        );
    }

    /**
     * Locator for the Cost input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and avoids dynamic IDs.
     */
    private final By costEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='cost'].ag-cell-inline-editing " +
                            "input[data-ref='eInput'][aria-label='Input Editor']"
            );

    /**
     * Locator for the Discount Rate (%) cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs and test IDs.
     */
    private final By discountRateCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='discountRate']"
            );

    /**

     * Locates the Discount Rate cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The unique Item-cell data-testid stored in newItemTestId is used
     * as the stable row anchor. This prevents the automation from
     * editing the Discount Rate cell of one of the existing duplicated
     * Purchase Order rows.
     */
    private By getDiscountRateCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='discountRate']"
        );
    }


    /**
     * Locator for the Discount value cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs.
     */
    private final By discountValueCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='discountValue']"
            );

    /**
     * Locates the Discount Value cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The stable Item data-testid is used as the row anchor, ensuring that
     * the Discount Value is entered only in the newly created row even when
     * other rows exist in the grid.
     */
    private By getDiscountValueCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='discountValue']"
        );
    }

    /**
     * Locator for the Discount Rate (%) input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and avoids dynamic IDs.
     */
    private final By discountRateEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='discountRate'].ag-cell-inline-editing " +
                            "input[data-ref='eInput'][aria-label='Input Editor']"
            );

    /**
     * Locator for the Addition Rate (%) cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs and test IDs.
     */
    private final By extraRateCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='extraRate']"
            );

    /**
     * Locator for the Addition Rate (%) input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and avoids dynamic editor IDs.
     */
    private final By extraRateEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='extraRate'].ag-cell-inline-editing " +
                            "input[data-ref='eInput'][aria-label='Input Editor']"
            );

    /**
     * Locates the Addition Rate (%) cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The stable Item data-testid is used as the row anchor so that the
     * Addition Rate cell is always resolved from the NEW row, regardless
     * of AG Grid row indexes or re-rendering.
     */
    private By getExtraRateCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='extraRate']"
        );
    }

    /**
     * Locates the active Addition Rate (%) editor inside the newly added
     * Purchase Order AG Grid row.
     *
     * The editor is restricted to the row identified by newItemTestId
     * and to the extraRate column, preventing an editor from another
     * row or another grid field from being selected.
     */
    private By getExtraRateEditorLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='extraRate' " +
                        "and contains(@class,'ag-cell-inline-editing')]" +
                        "//input[" +
                        "@data-ref='eInput' " +
                        "and @aria-label='Input Editor'" +
                        "]"
        );
    }

    /**
     * Locator for the Addition value cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs and test IDs.
     */
    private final By extraValueCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='extraValue']"
            );

    /**
     * Locator for the Addition value input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and avoids dynamic editor IDs.
     */
    private final By extraValueEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='extraValue'].ag-cell-inline-editing " +
                            "input[data-ref='eInput'][aria-label='Input Editor']"
            );

    /**
     * Locates the Addition Value cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The stable Item data-testid is used as the row anchor so that the
     * Addition Value cell is always resolved from the NEW row, regardless
     * of AG Grid row indexes or DOM re-rendering.
     */
    private By getExtraValueCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='extraValue']"
        );
    }

    /**
     * Locates the active Addition Value editor specifically inside the
     * newly added Purchase Order AG Grid row.
     *
     * The editor is scoped to the row identified by newItemTestId and
     * the extraValue column, preventing an editor from another row or
     * another grid field from being selected.
     */
    private By getExtraValueEditorLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='extraValue' " +
                        "and contains(@class,'ag-cell-inline-editing')]" +
                        "//input[" +
                        "@data-ref='eInput' " +
                        "and @aria-label='Input Editor'" +
                        "]"
        );
    }

    /**
     * Locator for the Production Date cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs and test IDs.
     */
    private final By productionDateCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='productionDate']"
            );

    /**
     * Locator for the Production Date input while the AG Grid cell is in inline-edit mode.
     * Uses stable AG Grid editing attributes and avoids dynamic editor IDs.
     */
    private final By productionDateEditorLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='productionDate'].ag-cell-inline-editing " +
                            "input[data-ref='eInput'][aria-label='Input Editor']"
            );

    /**
     * Locator for the Expire Date cell in the Purchase Order AG Grid.
     * Uses the stable AG Grid column identifier and avoids dynamic IDs and test IDs.
     */
    private final By expireDateCellLocator =
            By.cssSelector(
                    "div[role='gridcell'][col-id='expireDate']"
            );

    /**
     * Locator for the Expire Date cell of the newly created Purchase Order row.
     * Uses the AG Grid row-index on the parent row and the stable expireDate
     * column identifier on the grid cell, allowing the cell to be re-located
     * directly from the driver after React/AG Grid re-renders.
     */
    private By getExpireDateCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='expireDate']"
        );
    }

    /**
     * Locator for the Free Quantities column cell in the AG Grid.
     * Uses the stable AG Grid column identifier instead of a dynamic row index.
     */
    private final By freeQuantityCellLocator =
            By.cssSelector("[col-id='freeQuantities']");

    /**
     * Locator for the inline editor created when the Free Quantities
     * AG Grid cell enters edit mode.
     */
    private final By freeQuantityEditorLocator =
            By.cssSelector("[col-id='freeQuantities'] input");

    /**
     * Locates the confirmation "Yes" button in the Purchase Order confirmation modal.
     *
     * Uses the stable data-modal-agree attribute and submit type instead of
     * the dynamically generated data-testid value.
     */
    private By getConfirmYesButtonLocator() {
        return By.cssSelector(
                "button[data-modal-agree][type='submit']"
        );
    }

    /**
     * Locator for the Note (البيان) cell in the newly created Purchase Order row.
     * The row index is used because AG Grid may re-render the row during editing.
     */
    private By getNoteCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newRowIndex + "'] " +
                        "[role='gridcell'][col-id='note']"
        );
    }

    /**
     * Locator for the Account (الحساب) cell in the newly created Purchase Order row.
     * The row index is used to keep the locator dynamic and stable across AG Grid
     * re-renders while the column is identified by its fixed col-id.
     */
    private By getAccountCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newRowIndex + "'] " +
                        "[role='gridcell'][col-id='accountId']"
        );
    }

    /**
     * Locates the Account cell specifically inside the newly added
     * Purchase Order AG Grid row.
     *
     * The stable Item data-testid is used as the row anchor instead of
     * newRowIndex, ensuring that the Account cell always belongs to the
     * newly created row even after AG Grid re-renders or changes row indexes.
     */
    private By getAccountCellLocatorForNewRow() {

        return By.xpath(
                "//div[@role='gridcell' " +
                        "and @col-id='itemId' " +
                        "and @data-testid='" + newItemTestId + "']" +
                        "/ancestor::div[@role='row'][1]" +
                        "//div[@role='gridcell' " +
                        "and @col-id='accountId']"
        );
    }

    /**
     * Locates the Account cell inside the newly created row of the
     * Discounts & Additions AG Grid.
     *
     * IMPORTANT:
     * This grid is independent from the Purchase Order items grid,
     * therefore it uses its own newDiscountAdditionRowIndex and does
     * NOT depend on the Purchase Order newRowIndex.
     */
    private By getDiscountAdditionAccountCellLocator() {

        return By.cssSelector(
                "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='accountId']"
        );
    }



    /**
     * Locator for the Influencing on Cost (التأثير على التكلفة) cell
     * in the newly created Purchase Order row.
     *
     * newRowIndex identifies the newly created AG Grid row,
     * while data-testid provides a stable identifier for the target column.
     */
    private By getInfluencingOnCostCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='influencingOnCost']" +
                        "[data-testid='orders-influencingOnCost-" + newRowIndex + "']"
        );
    }

    /**
     * Locates the "Influencing on Cost" cell (التأثير على التكلفة)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "influencingOnCost" together with the row index captured
     * specifically for the Discounts & Additions grid.
     */
    private By getInfluencingOnCostCellLocator2() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='influencingOnCost']"
        );
    }


    /**
     * Locator for the Discount Rate cell in the newly created Purchase Order row.
     * Uses the stable AG Grid column identifier and the captured new-row index.
     */
    private By getDiscountRateCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='discountRate']" +
                        "[data-testid='orders-discountRate-" + newRowIndex + "']"
        );
    }

    /**
     * Locates the "Discount Rate" cell (نسبة الخصم)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "discountRate" together with the independent row index
     * captured for the Discounts & Additions grid.
     */
    private By getDiscountAdditionDiscountRateCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='discountRate']"
        );
    }



    /**
     * Locator for the Discount Value (قيمة الخصم) cell
     * in the newly created Purchase Order row.
     *
     * newRowIndex identifies the newly created AG Grid row,
     * while data-testid identifies the Discount Value column.
     */
    private By getDiscountValueCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='discountValue']" +
                        "[data-testid='orders-discountValue-" + newRowIndex + "']"
        );
    }

    /**
     * Locates the "Discount Value" cell (قيمة الخصم)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "discountValue" together with the independent row index
     * captured for the Discounts & Additions grid.
     */
    private By getDiscountAdditionDiscountValueCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='discountValue']"
        );
    }



    /**
     * Locator for the Addition Rate cell in the newly created Purchase Order row.
     * Uses the stable AG Grid column identifier together with the captured new-row index.
     */
    private By getAdditionRateCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='additionRate']" +
                        "[data-testid='orders-additionRate-" + newRowIndex + "']"
        );
    }

    /**
     * Locates the "Addition Rate" cell (نسبة الإضافي)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "additionRate" together with the independent row index
     * captured for the Discounts & Additions grid.
     */
    private By getDiscountAdditionAdditionRateCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='additionRate']"
        );
    }


    /**
     * Locator for the Addition Value cell in the newly created Purchase Order row.
     * Uses the stable AG Grid column identifier and the captured new-row index.
     */
    private By getAdditionValueCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='additionValue']" +
                        "[data-testid='orders-additionValue-" + newRowIndex + "']"
        );
    }

    /**
     * Locates the "Addition Value" cell (قيمة الإضافي)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "additionValue" together with the independent row index
     * captured for the Discounts & Additions grid.
     */
    private By getDiscountAdditionAdditionValueCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='additionValue']"
        );
    }



    /**
     * Locates the Currency cell in the newly created Purchase Order row.
     * Uses AG Grid's stable currencyId column identifier.
     */
    private By getCurrencyCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='currencyId']"
        );
    }


    /**
     * Locates the "Currency" cell (العملة)
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier
     * "currencyId" together with the independent row index
     * captured for the Discounts & Additions grid.
     */
    private By getDiscountAdditionCurrencyCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='currencyId']"
        );
    }



    /**
     * Locates the Note (البيان) cell specifically within the Discounts & Additions section.
     * Uses a dedicated method name to distinguish it from the main Purchase Order Note cell.
     */
    private By getDiscountsAdditionsNoteCellLocator() {
        return By.cssSelector(
                "[role='gridcell'][col-id='note']"
        );
    }

    /**
     * Locates the "Note" cell (البيان) in the newly created row
     * of the Discounts & Additions AG Grid.
     *
     * The locator uses the stable AG Grid column identifier "note"
     * together with the independent row index captured specifically
     * for the Discounts & Additions grid.
     */
    private By getDiscountAdditionNoteCellLocator() {
        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='note']"
        );
    }

    /**
     * Locates the main Save button of the Purchase Order.
     *
     * Uses the stable data-testid attribute to identify the Save button
     * without depending on dynamic IDs or the internal SVG structure.
     */
    private By getSaveButtonLocator() {
        return By.cssSelector(
                "[data-testid='orders-save']"
        );
    }

    /**
     * Locates all data rows in the external Purchase Orders AG Grid.
     *
     * Uses the stable AG Grid role attribute to identify Purchase Order
     * rows without relying on dynamic row IDs or dynamic data-testid values.
     */
    private By getPurchaseOrderRowsLocator() {
        return By.cssSelector(
                "[role='row'][row-index]"
        );
    }

    /**
     * Locates the Copy/Duplicate button used to duplicate
     * the selected Purchase Order.
     *
     * Uses the stable data-testid attribute and avoids
     * the dynamic button ID or internal SVG structure.
     */
    private final By copyButtonLocator =
            By.cssSelector("[data-testid='orders-copy']");

    /**
     * Locates the "Continue" button in the Purchase Order modal.
     *
     * Uses stable attributes and the button text instead of the
     * dynamically generated data-testid value.
     */
    private final By continueButtonLocator =
            By.cssSelector(
                    "button[data-modal-agree][type='submit']"
            );

    /**
     * Locates the "Update Cost" button in the Purchase Order modal.
     *
     * Uses stable modal attributes and the button text instead of
     * the dynamically generated data-testid value.
     */
    private final By updateCostButtonLocator =
            By.xpath(
                    "//button[@data-modal-agree and @type='submit'" +
                            " and normalize-space()='تحديث التكلفة']"
            );

    /**
     * Builds a locator for the Quantity inline editor inside the newly
     * captured AG Grid row.
     *
     * The dynamic row-index ensures that after AG Grid re-renders the DOM,
     * the editor belonging to an existing Purchase Order row cannot be
     * selected accidentally.
     */
    private By getQuantityEditorLocator() {

        return By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newRowIndex + "'] " +
                        "div[role='gridcell'][col-id='quantity'].ag-cell-inline-editing " +
                        "input.ag-input-field-input.ag-text-field-input[data-ref='eInput']"
        );
    }

    /**
     * Identifies the Add Purchase Order button in the external Purchase Orders grid.
     *
     * The stable data-testid attribute is preferred over the dynamic/short
     * HTML id because it explicitly identifies the Add action in the ERP UI.
     */
    private final By addPurchaseOrderButton =
            By.cssSelector(
                    "button[data-testid='orders-add']"
            );

    /**
     * Locates the button that opens the Reference Document selection grid.
     * Anchors the locator to the stable referenceDocument input ID
     * and selects the first button inside its parent container.
     */
    private final By openReferenceDocumentGridButtonLocator =
            By.xpath("//input[@id='referenceDocument']/parent::div//button[1]");

    /**
     * Locates the Purchases Store radio button using its dedicated test ID.
     */
    private final By purchasesStoreRadioLocator =
            By.cssSelector("[data-testid='orders-referenceScreen-PurchasesStore']");

    /**
     * Locates the Purchases Quotations Store radio button using its dedicated test ID.
     */
    private final By purchasesQuotationsStoreRadioLocator =
            By.cssSelector("[data-testid='orders-referenceScreen-PurchasesQutationsStore']");

    /**
     * Identifies the reference-document download button by its unique
     * Lucide download icon, avoiding reliance on styling classes alone.
     */
    private final By downloadReferenceDocumentButton = By.cssSelector(
            "button[type='button']:has(svg.lucide-download)"
    );

    /**
     * Locates the business-status cell for a specific Purchase Order.
     *
     * Uses the selected AG Grid row-id and the recordStatus column identifier
     * to find the status cell within that row, including rows rendered in
     * different AG Grid containers.
     *
     * @param rowId the unique row-id of the selected Purchase Order
     * @return a locator for the selected Purchase Order's status cell
     */
    private By statusCellLocator(String rowId) {
        return By.cssSelector(
                "div[role='row'][row-id='" + rowId + "'] " +
                        "div[role='gridcell'][col-id='recordStatus']"
        );
    }

    /**
     * Locator for the Edit/View button of a Purchase Order.
     * Uses the stable data-testid attribute to avoid brittle selectors.
     */
    private final By editPurchaseOrderButton =
            By.cssSelector("button[data-testid='orders-edit']");

    /**
     * Identifies the Delete button used to delete the currently opened
     * Purchase Order.
     */
    private final By deletePurchaseOrderButton =
            By.cssSelector("[data-testid='orders-delete']");

    /**
     * Identifies the confirmation button in the Purchase Order deletion dialog.
     */
    private final By confirmDeletePurchaseOrderButton =
            By.cssSelector(
                    "button[data-modal-agree][type='submit']"
            );

    /**
     * Identifies the Approve/Disapprove button for the currently opened
     * Purchase Order.
     */
    /*private final By approvePurchaseOrderButton =
            By.cssSelector(
                    "button[data-testid='requisitions-approveOrDisapprove']"
            );*/


    /**
     * Locates the approval action button for a Purchase Order.
     * Uses the stable action type and button ID instead of the
     * potentially context-specific requisitions data-testid.
     */
    private final By approvePurchaseOrderButton =
            By.cssSelector("button#approveOrDisapprove[data-action-type='approveOrDisapprove']");

    /**
     * Identifies the confirmation button in the Draft Purchase Order
     * approval dialog.
     */
    private final By confirmApprovePurchaseOrderButton =
            By.cssSelector(
                    "button[data-modal-agree][type='submit']"
            );

    /**
     * Locates the Print button for the currently opened Purchase Order.
     * Uses stable data-testid and data-action-type attributes instead
     * of the dynamically generated button ID.
     */
    private final By printPurchaseOrderButton =
            By.cssSelector(
                    "button[data-testid='orders-print']" +
                            "[data-action-type='print']"
            );

    /**
     * Locates the Purchase Order print-template radio group.
     * Uses the role attribute because the group's data-testid is dynamically generated.
     */
    private final By printTemplatesGroup =
            By.cssSelector("div[role='radiogroup'] button[role='radio'][data-testid^='orders-template']");

    /**
     * Locates all individual Purchase Order print-template radio buttons.
     * Uses the stable orders-template test ID prefix to avoid dynamic element IDs.
     */
    private final By printTemplateOptions =
            By.cssSelector("button[role='radio'][data-testid^='orders-template']");

    /**
     * Locates the final Print confirmation button in the Purchase Order
     * print dialog.
     * Uses stable data-testid and data-action-type attributes instead
     * of relying on the element ID.
     */
    private final By confirmPrintPurchaseOrderButton =
            By.cssSelector(
                    "button[data-testid='orders-printConfirm']" +
                            "[data-action-type='printConfirm']"
            );
    /**
     * Locates the Save as PDF button in the Purchase Order print preview.
     * Uses the stable CSS class and title attribute rather than dynamic IDs.
     */
    private final By savePurchaseOrderAsPdfButton =
            By.cssSelector("button.pd-btn.pd-btn-pdf[title='حفظ كـ PDF']");

    /**
     * Locates the final Print button in the Purchase Order print preview.
     * Uses the stable CSS classes and title attribute instead of a dynamic ID.
     */
    private final By finalPrintPurchaseOrderButton =
            By.cssSelector("button.pd-btn.pd-btn-print[title='طباعة']");

    /**
     * Locates the Cancel Purchase Order button on the currently opened
     * Purchase Order page.
     * Uses stable data-testid and data-action-type attributes,
     * avoiding the element ID.
     */
    private final By cancelPurchaseOrderButton =
            By.cssSelector(
                    "button[data-testid='orders-cancel'][data-action-type='cancel']"
            );

    /**
     * Locates the confirmation button in the Cancel Purchase Order dialog.
     * Uses the stable data-modal-agree attribute and submit type
     * instead of the dynamically generated data-testid.
     */
    private final By confirmCancelPurchaseOrderButton =
            By.cssSelector("button[data-modal-agree][type='submit']");

    /**
     * Locates the Attachments button on the currently opened Purchase Order.
     * Uses stable data-testid and data-action-type attributes
     * instead of the element ID.
     */
    private final By attachmentsButton =
            By.cssSelector(
                    "button[data-testid='orders-uploadAttachment']" +
                            "[data-action-type='uploadAttachment']"
            );

    /**
     * Locates the Add Attachment button in the attachment dialog.
     * Uses the stable data-modal-agree attribute and submit type
     * instead of the dynamically generated data-testid.
     */
    /**
     * "إضافة مرفق" in the "تفاصيل المرفق" dialog, found by its text.
     * QC, 2026-10-31: the old generic locator button[data-modal-agree][type='submit']
     * worked while the order had no attachments, but timed out on an order
     * that already had 2 (PO2610T3-000151), although the button was shown.
     */
    private final By addAttachmentButton =
            By.xpath("//*[@role='dialog'][.//h2[normalize-space()='تفاصيل المرفق']]"
                    + "//button[normalize-space()='إضافة مرفق']");

    /**
     * Locates the Attachment Title input field.
     * Uses the stable data-testid attribute to avoid relying on the element ID.
     */
    private final By attachmentTitleInput =
            By.cssSelector("input[data-testid='orders-title']");

    /**
     * Locates the Attachment Description input field.
     * Uses the stable data-testid attribute instead of the element ID.
     */
    private final By attachmentDescriptionInput =
            By.cssSelector("input[data-testid='orders-description']");

    /**
     * Locates the final Add Attachment confirmation button.
     * Uses the modal confirmation attribute and the button's Arabic text
     * instead of the dynamically generated data-testid.
     */
    private final By addAttachmentConfirmButton =
            By.xpath(
                    "//button[@data-modal-agree and @type='submit' " +
                            "and normalize-space()='اضافة']"
            );

    /**
     * Locates the Undo button on the currently opened Purchase Order page.
     * Uses stable data-testid and data-action-type attributes
     * instead of the element ID.
     */
    private final By undoButton =
            By.cssSelector(
                    "button[data-testid='orders-undo'][data-action-type='undo']"
            );

    /**
     * Locates the confirmation button in the Undo dialog.
     * Uses the stable data-modal-agree attribute and submit type,
     * together with the button's Arabic text.
     */
    private final By confirmUndoButton =
            By.xpath(
                    "//button[@data-modal-agree and @type='submit' " +
                            "and normalize-space()='موافق']"
            );

    /**
     * Identifies the clipboard actions menu button by its stable data-testid.
     */
    private final By clipboardBarButtonLocator =
            By.cssSelector("[data-testid='orders-clipboardBar']");

    /**
     * Locates the "طلب الشراء" menu item in the clipboard actions menu.
     */
    private final By purchaseRequestMenuItemLocator =
            By.xpath("//div[@role='menuitem'][.//button[normalize-space()='طلب الشراء']]");

    /**
     * Locates the confirmation button labeled "موافق" in the current modal dialog.
     */
    private final By confirmModalButtonLocator =
            By.cssSelector("button[data-modal-agree][type='submit']");

    /**
     * Locates the "متابعة من عروض الاسعار" option in the menu.
     */
    private final By continueFromQuotationsMenuItemLocator =
            By.xpath("//div[@role='menuitem'][.//button[normalize-space()='متابعة من عروض الاسعار']]");

    /**
     * Locates the "الاستيراد من Excel" option inside the menu.
     */
    private final By importFromExcelMenuItemLocator =
            By.xpath("//div[@role='menuitem'][.//button[normalize-space()='الاستيراد من Excel']]");

    /**
     * Locates the hidden file input used to upload Excel files (.xls and .xlsx)
     * through the Excel import dialog.
     */
    private final By excelFileInputLocator =
            By.cssSelector("input[type='file'][accept*='.xlsx'][accept*='.xls']");

    /**
     * Locates the "الاستيراد من Excel" confirmation button
     * inside the Excel import dialog.
     */
    private final By importExcelConfirmButtonLocator =
            By.cssSelector("button[data-modal-agree][type='submit']");

    /**
     * Locates the "التصدير الى Excel" option inside the actions menu.
     */
    private final By exportToExcelMenuItemLocator =
            By.xpath("//div[@role='menuitem'][.//button[normalize-space()='التصدير الى Excel']]");

    /**
     * Locates the close (X) button of the current Purchase Order dialog or panel.
     */
    private final By closePurchaseOrderDialogLocator =
            By.cssSelector("button[data-testid='orders-xIcon']");

    /**
     * Locates the "نعم" confirmation button in the current modal dialog.
     */
    private final By confirmYesButtonLocator =
            By.xpath("//button[@data-modal-agree and @type='submit' and normalize-space()='نعم']");


    /**
     * Attempts to activate an AG Grid cell for inline editing.
     *
     * Returns true when the native AG Grid editor appears successfully.
     * Returns false when the cell cannot be edited, allowing the calling
     * field method to skip the disabled/non-editable cell without failure.
     */
    private boolean tryActivateGridCellEditor(
            By cellLocator,
            By editorLocator
    ) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(2));

        /**
         * Locate the target AG Grid cell.
         */
        WebElement cell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        cellLocator
                )
        );

        /**
         * Scroll the target cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                cell
        );

        /**
         * Double-click the cell to activate AG Grid inline editing.
         */
        new Actions(driver)
                .doubleClick(cell)
                .perform();

        /**
         * Wait for the native AG Grid editor to appear.
         *
         * If the editor appears, the cell is editable.
         * If it does not appear, the cell is treated as disabled/non-editable.
         */
        try {

            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            editorLocator
                    )
            );

            return true;

        } catch (org.openqa.selenium.TimeoutException e) {

            System.out.println(
                    "Cell is disabled/non-editable. Skipping: " +
                            cellLocator
            );

            return false;
        }
    }

    public void clickNew() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement newButtonElement = wait.until(
                ExpectedConditions.elementToBeClickable(newButton)
        );
        newButtonElement.click();
    }

    /**
     * Selects a random Operation Type from the available Purchase Order options.
     *
     * The method handles React Select re-rendering by re-finding the dropdown
     * and option elements during every attempt. Both stale-element and timeout
     * conditions are treated as temporary rendering failures, allowing the
     * method to retry instead of terminating the test immediately.
     */
    public void selectRandomOperationType() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Operation Type - attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

                /*
                 * Re-find and open the Operation Type dropdown
                 * on every attempt.
                 */
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                operationTypeDropdown
                        )
                );

                dropdown.click();

                System.out.println(
                        "Operation Type dropdown opened."
                );

                /*
                 * Read the currently visible Operation Type options.
                 *
                 * The option WebElements are used only during this read
                 * operation and are never stored for later interaction.
                 *
                 * If React re-renders one of the options while reading it,
                 * the condition returns null so WebDriverWait can retry
                 * reading the current DOM.
                 */
                List<String> operationTypes = wait.until(driver -> {

                    try {

                        List<WebElement> elements =
                                driver.findElements(
                                        operationTypeOptions
                                );

                        List<String> texts =
                                new ArrayList<>();

                        for (WebElement element : elements) {

                            if (element.isDisplayed()) {

                                String text =
                                        element.getText().trim();

                                if (!text.isEmpty()) {
                                    texts.add(text);
                                }
                            }
                        }

                        return texts.isEmpty()
                                ? null
                                : texts;

                    } catch (StaleElementReferenceException e) {

                        return null;
                    }
                });

                if (operationTypes == null ||
                        operationTypes.isEmpty()) {

                    throw new IllegalStateException(
                            "No Operation Types found."
                    );
                }

                /*
                 * Select one Operation Type randomly by its visible text.
                 */
                String selectedOperationType =
                        operationTypes.get(
                                random.nextInt(
                                        operationTypes.size()
                                )
                        );

                System.out.println(
                        "Number of operation types: "
                                + operationTypes.size()
                );

                System.out.println(
                        "Selected operation type: "
                                + selectedOperationType
                );

                /*
                 * Build a fresh locator using the selected visible text.
                 *
                 * The option is intentionally re-located immediately
                 * before clicking because React Select may replace the
                 * original option element during rendering.
                 */
                By selectedOperationTypeOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]" +
                                        "[normalize-space(.)=" +
                                        "'" + selectedOperationType + "'" +
                                        "]"
                        );

                /*
                 * Re-find the option and click it.
                 */
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOperationTypeOption
                        )
                ).click();

                System.out.println(
                        "Operation Type selected successfully: "
                                + selectedOperationType
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Operation Type dropdown was re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Operation Type dropdown/options were not "
                                + "stable during attempt "
                                + attempt
                                + ". Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Operation Type after "
                        + maxAttempts
                        + " attempts because the React Select "
                        + "dropdown kept re-rendering or timing out."
        );
    }


    /**
     * Waits for the Supplier field to initialize, checks whether it is disabled,
     * and skips supplier selection when the field is disabled.
     *
     * If the Supplier field is enabled, the method opens the dropdown and selects
     * a random supplier while handling stale option elements caused by React re-rendering.
     */
    public void selectRandomSupplier() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        By supplierField = By.cssSelector("[data-testid='orders-vendorId']");

        By supplierControl = By.cssSelector(
                "[data-testid='orders-vendorId'] .select__control"
        );

        By supplierInput = By.cssSelector(
                "[data-testid='orders-vendorId'] input"
        );

        By supplierOptions = By.cssSelector("div[class*='select__option']");

        // Wait briefly for the Supplier field to appear after the form initializes.
        WebElement field = wait.until(
                ExpectedConditions.visibilityOfElementLocated(supplierField)
        );

        // Wait until the reference-document logic has finished updating the
        // Supplier field (its text unchanged for 700 ms, max 5 s).
        waitForStableText(supplierField, Duration.ofMillis(700), Duration.ofSeconds(5));

        // Re-locate the elements after the UI has had time to update.
        field = driver.findElement(supplierField);

        List<WebElement> controls = field.findElements(
                By.cssSelector(".select__control")
        );

        List<WebElement> inputs = field.findElements(
                By.cssSelector("input")
        );

        if (controls.isEmpty()) {
            System.out.println(
                    "Supplier control was not found. Skipping supplier selection."
            );
            return;
        }

        WebElement control = controls.get(0);

        String fieldClass = field.getAttribute("class");
        String controlClass = control.getAttribute("class");
        String controlAriaDisabled = control.getAttribute("aria-disabled");

        String inputDisabled = inputs.isEmpty()
                ? null
                : inputs.get(0).getAttribute("disabled");

        String inputAriaDisabled = inputs.isEmpty()
                ? null
                : inputs.get(0).getAttribute("aria-disabled");

        /*
         * Check common React Select disabled indicators.
         * The control's actual DOM attributes are checked before any click.
         */
        boolean isDisabled =
                (fieldClass != null
                        && fieldClass.contains("select--is-disabled"))
                        || (controlClass != null
                        && controlClass.contains("select__control--is-disabled"))
                        || "true".equalsIgnoreCase(controlAriaDisabled)
                        || inputDisabled != null
                        || "true".equalsIgnoreCase(inputAriaDisabled);

        if (isDisabled) {
            System.out.println(
                    "Supplier field is disabled. Skipping supplier selection."
            );
            return;
        }

        // Continue only when the Supplier field appears to be enabled.
        System.out.println(
                "Supplier field is enabled. Starting random Supplier selection."
        );

        // Open the dropdown only after the disabled-state check.
        wait.until(ExpectedConditions.elementToBeClickable(control)).click();

        // Wait until at least one supplier option is available.
        wait.until(ExpectedConditions.presenceOfAllElementsLocatedBy(supplierOptions));

        // Retry selection if React re-renders and invalidates an option element.
        Random random = new Random();

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                List<WebElement> options = driver.findElements(supplierOptions);

                if (options.isEmpty()) {
                    throw new TimeoutException(
                            "No Supplier options were displayed."
                    );
                }

                WebElement selectedSupplier =
                        options.get(random.nextInt(options.size()));

                String supplierName = selectedSupplier.getText().trim();

                selectedSupplier.click();

                System.out.println(
                        "Randomly selected Supplier: " + supplierName
                );

                return;

            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Supplier options changed. Retrying selection, attempt "
                                + attempt + " of 3."
                );
            }
        }

        throw new TimeoutException(
                "Unable to select a Supplier after 3 attempts."
        );

    }

    /**
     * Creates a safe XPath string literal for supplier names that may contain
     * single quotes, double quotes, or both.
     */
    private String xpathLiteral(String value) {

        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'", -1);
        return "concat('" + String.join("', \"'\", '", parts) + "')";
    }


    /**
     * Selects a random Branch from the available Purchase Order branches.
     * Opens the dropdown, waits for the available options, selects one randomly,
     * and logs the selected branch for test execution traceability.
     */
    public void selectRandomBranch() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dropdown = wait.until(
                ExpectedConditions.elementToBeClickable(branchDropdown)
        );
        dropdown.click();

        List<WebElement> options = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(branchOptions)
        );

        if (options.isEmpty()) {
            throw new IllegalStateException("No branches found.");
        }

        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        System.out.println("Number of branches: " + options.size());
        System.out.println("Selected branch: "
                + options.get(randomIndex).getText());

        options.get(randomIndex).click();
    }

    /**
     * Attempts to select a random Warehouse for the currently selected Branch.
     *
     * Returns true when a Warehouse is successfully selected.
     * Returns false when no Warehouse options are available, allowing the
     * PurchaseOrderTest to restart the selection flow from Supplier.
     */

    /**
     * Opens the Warehouse dropdown, waits briefly for its options to load,
     * and selects a random available Warehouse.
     *
     * @return true if a Warehouse was selected; false if no options appeared
     */
    public boolean selectRandomWarehouse() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait until the Warehouse input is enabled before opening the dropdown.
        wait.until(driver -> {
            WebElement warehouse =
                    driver.findElement(warehouseDropdown);

            WebElement input = warehouse.findElement(
                    By.cssSelector(
                            "input[data-testid='warehouseId'], input#warehouseId"
                    )
            );

            return input.isEnabled()
                    && input.getAttribute("disabled") == null;
        });

        // Open the Warehouse dropdown when it becomes clickable.
        WebElement dropdown = wait.until(
                ExpectedConditions.elementToBeClickable(warehouseDropdown)
        );

        dropdown.click();

        // Wait briefly for at least one visible Warehouse option to appear.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        optionsWait.pollingEvery(Duration.ofMillis(200));

        boolean optionAppeared;

        try {
            optionAppeared = optionsWait.until(currentDriver ->
                    currentDriver.findElements(warehouseOptions)
                            .stream()
                            .anyMatch(WebElement::isDisplayed)
            );
        } catch (TimeoutException e) {
            optionAppeared = false;
        }

        // Return false if no visible Warehouse options appeared.
        if (!optionAppeared) {
            System.out.println(
                    "No visible Warehouse options appeared for the selected branch."
            );
            return false;
        }

        // Retrieve the currently visible and enabled Warehouse options.
        List<WebElement> options = driver.findElements(warehouseOptions)
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .toList();

        // Handle the case where the options disappeared before selection.
        if (options.isEmpty()) {
            System.out.println(
                    "Warehouse options disappeared before selection."
            );
            return false;
        }

        // Choose a random option from the available Warehouses.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        WebElement selectedOption = options.get(randomIndex);
        String selectedWarehouse = selectedOption.getText();

        System.out.println("Number of visible warehouses: " + options.size());
        System.out.println("Selected warehouse: " + selectedWarehouse);

        // Select the chosen Warehouse.
        selectedOption.click();

        return true;
    }


    /**
     * Selects a random Currency from the available Purchase Order currencies.
     * Opens the Currency dropdown, waits for the available options, selects one
     * randomly, and logs the selected currency for test execution traceability.
     */
    public void selectRandomCurrency() {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dropdown = wait.until(
                ExpectedConditions.elementToBeClickable(currencyDropdown)
        );

        dropdown.click();

        List<WebElement> options = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(currencyOptions)
        );

        if (options.isEmpty()) {
            throw new IllegalStateException("No currencies found.");
        }

        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        System.out.println("Number of currencies: " + options.size());
        System.out.println("Selected currency: "
                + options.get(randomIndex).getText());

        options.get(randomIndex).click();
    }

    /**
     * Selects a random Cost Center when the Cost Center field is enabled.
     *
     * If the Cost Center field is disabled, the method skips the selection
     * completely and returns to the calling test without opening the dropdown.
     */
    public void selectRandomCostCenter() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Cost Center input inside the dropdown container
         * to determine whether the field is enabled or disabled.
         */
        WebElement costCenterInput =
                wait.until(
                        ExpectedConditions.presenceOfNestedElementLocatedBy(
                                costCenterDropdown,
                                By.cssSelector(
                                        "input[data-testid='costCenterId'], input#costCenterId"
                                )
                        )
                );

        /**
         * Skip Cost Center selection when the field is disabled.
         *
         * No dropdown click, option lookup, or random selection
         * will be performed in this case.
         */
        if (!costCenterInput.isEnabled()
                || costCenterInput.getAttribute("disabled") != null) {

            System.out.println(
                    "Cost Center field is disabled. " +
                            "Skipping random Cost Center selection."
            );

            return;
        }

        /**
         * Locate and open the enabled Cost Center dropdown.
         */
        WebElement dropdown =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                costCenterDropdown
                        )
                );

        dropdown.click();

        /**
         * Retrieve all visible Cost Center options.
         */
        List<WebElement> options =
                wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                costCenterOptions
                        )
                );

        /**
         * Fail explicitly if the Cost Center is enabled but
         * no options are available.
         */
        if (options.isEmpty()) {

            throw new IllegalStateException(
                    "No cost centers found."
            );
        }

        /**
         * Select one Cost Center randomly from the available options.
         */
        Random random = new Random();

        int randomIndex =
                random.nextInt(options.size());

        System.out.println(
                "Number of cost centers: " +
                        options.size()
        );

        System.out.println(
                "Selected cost center: " +
                        options.get(randomIndex).getText()
        );

        /**
         * Select the randomly chosen Cost Center.
         */
        options.get(randomIndex).click();
    }


    /**
     * Selects a random Tax Calculation Method only when the React Select
     * dropdown is enabled and its state has remained stable for two seconds.
     *
     * The method skips the field without clicking when it is disabled,
     * preventing ElementClickInterceptedException on disabled dropdowns.
     * If enabled, it opens the dropdown, selects a random available option,
     * and logs the selection for test execution traceability.
     */
    public void selectRandomTaxCalculationMethod() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locator for the Tax Calculation Method React Select container.
        // Its data-testid remains the primary reference for checking its state.
        By taxCalculationContainer =
                By.cssSelector("[data-testid='orders-taxCalculationMethode']");

        // Locator for the internal React Select input, used to verify
        // whether the dropdown is disabled at the input level.
        By taxCalculationInput =
                By.cssSelector(
                        "[data-testid='orders-taxCalculationMethode'] input[role='combobox']"
                );

        long stableDurationMillis = 2000;
        long pollingIntervalMillis = 200;
        long timeoutMillis = 10000;

        long startTime = System.currentTimeMillis();
        long enabledSince = -1;
        Boolean previousState = null;

        // Wait for the dropdown's enabled/disabled state to stabilize.
        while (System.currentTimeMillis() - startTime < timeoutMillis) {

            try {
                WebElement container =
                        driver.findElement(taxCalculationContainer);

                String classes = container.getAttribute("class");
                String ariaDisabled = container.getAttribute("aria-disabled");

                boolean disabledByClass =
                        classes != null
                                && classes.contains("select--is-disabled");

                boolean disabledByAria =
                        "true".equalsIgnoreCase(ariaDisabled);

                boolean disabledByInput = false;

                List<WebElement> inputs =
                        driver.findElements(taxCalculationInput);

                if (!inputs.isEmpty()) {
                    WebElement input = inputs.get(0);

                    disabledByInput =
                            !input.isEnabled()
                                    || "true".equalsIgnoreCase(
                                    input.getAttribute("aria-disabled")
                            );
                }

                boolean isEnabled =
                        !disabledByClass
                                && !disabledByAria
                                && !disabledByInput;

                if (previousState == null || previousState != isEnabled) {
                    previousState = isEnabled;
                    enabledSince = System.currentTimeMillis();
                }

                if (System.currentTimeMillis() - enabledSince
                        >= stableDurationMillis) {

                    if (!isEnabled) {
                        System.out.println(
                                "Tax Calculation Method field is disabled. "
                                        + "Random selection skipped."
                        );
                        return;
                    }

                    break;
                }

            } catch (StaleElementReferenceException
                     | NoSuchElementException e) {

                // React may re-render the dropdown; restart the stability check.
                previousState = null;
                enabledSince = -1;
            }

            try {
                Thread.sleep(pollingIntervalMillis);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while checking Tax Calculation Method state.",
                        e
                );
            }
        }

        // Recheck the current DOM immediately before interacting with the field.
        WebElement dropdown = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        taxCalculationContainer
                )
        );

        String currentClasses = dropdown.getAttribute("class");
        String currentAriaDisabled = dropdown.getAttribute("aria-disabled");

        List<WebElement> currentInputs =
                driver.findElements(taxCalculationInput);

        boolean currentlyDisabled =
                (currentClasses != null
                        && currentClasses.contains("select--is-disabled"))
                        || "true".equalsIgnoreCase(currentAriaDisabled)
                        || (!currentInputs.isEmpty()
                        && (!currentInputs.get(0).isEnabled()
                        || "true".equalsIgnoreCase(
                        currentInputs.get(0).getAttribute("aria-disabled")
                )));

        if (currentlyDisabled) {
            System.out.println(
                    "Tax Calculation Method field became disabled. "
                            + "Random selection skipped."
            );
            return;
        }

        // Click only after confirming that the dropdown is enabled.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        taxCalculationContainer
                )
        ).click();

        // Retrieve the visible options after opening the enabled dropdown.
        List<WebElement> options = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        taxCalculationMethodOptions
                )
        );

        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No tax calculation methods found."
            );
        }

        // Choose one available tax calculation method at random.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        String selectedMethod = options.get(randomIndex).getText();

        System.out.println(
                "Number of tax calculation methods: " + options.size()
        );

        System.out.println(
                "Selected tax calculation method: " + selectedMethod
        );

        // Click the selected option while the dropdown is open.
        options.get(randomIndex).click();
    }


    /**
     * Enters today's date into the Purchase Order "Order Date" field.
     * Generates the current system date in YYYY-MM-DD format, clears the existing
     * value, enters today's date, and triggers the field change event.
     */
    /**
     * Sets the Purchase Order date to today's date.
     * Uses JavaScript to update the date field value and dispatches input and change
     * events so the React application detects the updated date.
     */
    public void setPurchaseOrderDate() {

        String purchaseOrderDate =
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                );

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        WebElement dateField =
                driver.findElement(purchaseOrderDateField);

        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                purchaseOrderDate
        );

        System.out.println(
                "Purchase Order date: " + purchaseOrderDate
        );
    }

    /**
     * Sets the Purchase Order Deadline to one month after today's date.
     * Uses the native HTML input value setter so that the React-controlled
     * date field detects the value change correctly.
     */
    public void setPurchaseOrderDeadline() {

        String purchaseOrderDeadline =
                LocalDate.now()
                        .plusMonths(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        WebElement dateField =
                driver.findElement(purchaseOrderDeadlineField);

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const value = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(" +
                        "HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, value);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                purchaseOrderDeadline
        );

        System.out.println(
                "Purchase Order deadline: " + purchaseOrderDeadline
        );
    }

    /**
     * Selects a random Purchase Representative only when the dropdown is enabled.
     *
     * Waits up to 10 seconds for the dropdown state to stabilize.
     * If the dropdown is disabled, exits without clicking it or interacting
     * with its options. If enabled, selects a random available representative.
     */
    public void selectRandomPurchaseRepresentative() {

        // Allows up to 10 seconds for the dropdown state to stabilize.
        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(10)
        );

        wait.pollingEvery(Duration.ofMillis(200));
        wait.ignoring(StaleElementReferenceException.class);

        // Waits until the Purchase Representative dropdown is present and visible.
        wait.until(driver ->
                ExpectedConditions.visibilityOfElementLocated(
                        purchaseRepresentativeDropdown
                ).apply(driver)
        );

        // Tracks the last observed state to ensure it remains stable for 2 seconds.
        final String[] lastState = {null};
        final long[] stateStartTime = {0L};

        // Waits until the enabled/disabled state remains unchanged for 2 seconds.
        String stableState = wait.until(currentDriver -> {
            try {
                WebElement dropdown = currentDriver.findElement(
                        purchaseRepresentativeDropdown
                );

                String dropdownClass = dropdown.getAttribute("class");
                String ariaDisabled = dropdown.getAttribute("aria-disabled");

                // Checks the dropdown's native and React Select disabled indicators.
                boolean disabled =
                        (dropdownClass != null
                                && dropdownClass.contains("select--is-disabled"))
                                || "true".equalsIgnoreCase(ariaDisabled)
                                || !dropdown.isEnabled();

                String currentState = disabled ? "DISABLED" : "ENABLED";
                long now = System.nanoTime();

                // Restarts the stability timer if the observed state changes.
                if (!currentState.equals(lastState[0])) {
                    lastState[0] = currentState;
                    stateStartTime[0] = now;
                    return null;
                }

                long stableDurationMillis =
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                now - stateStartTime[0]
                        );

                // Accepts the state only after 2 seconds without a change.
                return stableDurationMillis >= 2000
                        ? currentState
                        : null;

            } catch (StaleElementReferenceException e) {
                // Resets state detection when React replaces the dropdown element.
                lastState[0] = null;
                stateStartTime[0] = 0L;
                return null;
            }
        });

        // Exits immediately when the dropdown is disabled; no click is performed.
        if ("DISABLED".equals(stableState)) {
            System.out.println(
                    "Purchase Representative field is disabled. "
                            + "Skipping representative selection."
            );
            return;
        }

        // Rechecks the current state immediately before attempting to click.
        try {
            WebElement dropdown = driver.findElement(
                    purchaseRepresentativeDropdown
            );

            String dropdownClass = dropdown.getAttribute("class");
            String ariaDisabled = dropdown.getAttribute("aria-disabled");

            boolean currentlyDisabled =
                    (dropdownClass != null
                            && dropdownClass.contains("select--is-disabled"))
                            || "true".equalsIgnoreCase(ariaDisabled)
                            || !dropdown.isEnabled();

            // Prevents interaction if the dropdown became disabled after the wait.
            if (currentlyDisabled) {
                System.out.println(
                        "Purchase Representative field became disabled. "
                                + "Skipping representative selection."
                );
                return;
            }

        } catch (StaleElementReferenceException e) {
            // Avoids clicking an element whose state could not be verified.
            System.out.println(
                    "Purchase Representative field changed during the state check. "
                            + "Skipping representative selection."
            );
            return;
        }

        // Clicks the dropdown only after confirming that it is enabled.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        purchaseRepresentativeDropdown
                )
        ).click();

        // Waits until at least one representative option is visible.
        wait.until(currentDriver ->
                currentDriver.findElements(purchaseRepresentativeOptions)
                        .stream()
                        .anyMatch(option -> {
                            try {
                                return option.isDisplayed();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        })
        );

        // Retrieves the current visible options after the dropdown has opened.
        List<WebElement> options = wait.until(currentDriver -> {
            try {
                List<WebElement> visibleOptions =
                        currentDriver.findElements(
                                        purchaseRepresentativeOptions
                                )
                                .stream()
                                .filter(option -> {
                                    try {
                                        return option.isDisplayed();
                                    } catch (StaleElementReferenceException e) {
                                        return false;
                                    }
                                })
                                .toList();

                return visibleOptions.isEmpty() ? null : visibleOptions;

            } catch (StaleElementReferenceException e) {
                return null;
            }
        });

        // Fails clearly if the enabled dropdown contains no available options.
        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No purchase representatives found."
            );
        }

        // Chooses a random representative from the currently visible options.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        // Logs the number of available representatives for test traceability.
        System.out.println(
                "Number of purchase representatives: " + options.size()
        );

        // Reads the selected option's text before clicking it.
        String selectedRepresentative = options.get(randomIndex).getText().trim();

        // Selects the randomly chosen representative.
        options.get(randomIndex).click();

        // Records the selected representative in the test execution log.
        System.out.println(
                "Selected purchase representative: " + selectedRepresentative
        );
    }

    /**
     * Selects one of the two Purchase Order price options randomly.
     * Waits for the Prices radio button group, randomly chooses between
     * "Excluding Tax" and "Including Tax", and clicks the selected option.
     */
    public void selectRandomPriceOption() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(pricesRadioGroup)
        );

        List<By> priceOptions = Arrays.asList(
                pricesExcludingTaxRadio,
                pricesIncludingTaxRadio
        );

        Random random = new Random();
        int randomIndex = random.nextInt(priceOptions.size());

        By selectedOption = priceOptions.get(randomIndex);

        WebElement radioButton = wait.until(
                ExpectedConditions.elementToBeClickable(selectedOption)
        );

        radioButton.click();

        System.out.println(
                "Selected price option: "
                        + (randomIndex == 0
                        ? "باستثناء الضريبة"
                        : "شاملة الضريبة")
        );
    }

    /**
     * Enters a random unique Supplier Invoice Number.
     * Generates a numeric invoice number using the current timestamp combined
     * with a random component to minimize the possibility of duplicate values
     * between test executions.
     */
    /**
     * Enters a random unique Supplier Invoice Number.
     * Uses the native HTML input value setter and dispatches input and change
     * events so that the React application detects the complete value correctly.
     */
    public void enterRandomSupplierInvoiceNumber() {

        String supplierInvoiceNumber =
                String.valueOf(
                        System.currentTimeMillis()
                );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement invoiceField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        supplierInvoiceNumberField
                )
        );

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const value = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(" +
                        "HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, value);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                invoiceField,
                supplierInvoiceNumber
        );

        System.out.println(
                "Supplier Invoice Number: "
                        + supplierInvoiceNumber
        );
    }

    /**
     * Selects a random Item Costs Method from the available Purchase Order options.
     *
     * The method handles React Select re-rendering by re-finding the dropdown
     * and option elements whenever a StaleElementReferenceException occurs.
     * It reads the available option texts, selects one randomly, then re-finds
     * the selected option by its visible text before clicking it.
     */
    public void selectRandomItemsCostsMethod() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                /*
                 * Re-find and open the Item Costs Method dropdown
                 * on every attempt.
                 */
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                itemsCostsMethodDropdown
                        )
                );

                dropdown.click();

                /*
                 * Read the available option texts from the current DOM.
                 */
                List<String> itemsCostsMethods = wait.until(driver -> {

                    List<WebElement> elements =
                            driver.findElements(itemsCostsMethodOptions);

                    List<String> texts = new ArrayList<>();

                    for (WebElement element : elements) {

                        try {

                            if (element.isDisplayed()) {

                                String text =
                                        element.getText().trim();

                                if (!text.isEmpty()) {
                                    texts.add(text);
                                }
                            }

                        } catch (StaleElementReferenceException ignored) {

                            return null;
                        }
                    }

                    return texts.isEmpty() ? null : texts;
                });

                if (itemsCostsMethods == null
                        || itemsCostsMethods.isEmpty()) {

                    throw new IllegalStateException(
                            "No Item Costs Methods found."
                    );
                }

                /*
                 * Select a random Item Costs Method.
                 */
                String selectedItemsCostsMethod =
                        itemsCostsMethods.get(
                                random.nextInt(
                                        itemsCostsMethods.size()
                                )
                        );

                System.out.println(
                        "Number of item costs methods: "
                                + itemsCostsMethods.size()
                );

                System.out.println(
                        "Selected item costs method: "
                                + selectedItemsCostsMethod
                );

                /*
                 * Re-find the selected option immediately before clicking it.
                 */
                By selectedOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]" +
                                        "[normalize-space(.)=" +
                                        "'" + selectedItemsCostsMethod + "'" +
                                        "]"
                        );

                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                ).click();

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Item Costs Method dropdown was re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Item Costs Method after "
                        + maxAttempts
                        + " attempts because the React Select "
                        + "dropdown kept re-rendering."
        );
    }


    /**
     * Selects a random Bulk Discount Type ("تعميم خصم على الأصناف").
     *
     * <p>Why it was slow (QC, 2026-10-29): it runs right after "تحديث التكلفة",
     * which re-renders the form. A click during that refresh does not open the
     * list, and the old code then waited the full 10 s for options before
     * retrying (up to 5 times). Now:</p>
     * <ul>
     *     <li>after each click it waits at most {@link #DROPDOWN_OPEN_TIMEOUT}
     *         for THIS field's menu to open, and clicks again at once if not;</li>
     *     <li>options are read only from this field's own listbox. React Select
     *         renders the menu in a portal outside the field, so the listbox is
     *         found through the input's {@code aria-controls} id;</li>
     *     <li>each step prints its duration, so a slow step is visible.</li>
     * </ul>
     */
    public void selectRandomBulkDiscountType() {

        long started = System.currentTimeMillis();
        By input = By.cssSelector("[data-testid='orders-bulkDiscountType'] input.select__input");
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            long t0 = System.currentTimeMillis();
            try {
                WebElement dropdown = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(bulkDiscountTypeDropdown));
                dropdown.click();

                // 1) Wait briefly for THIS field's listbox to open.
                String listboxId = new WebDriverWait(driver, DROPDOWN_OPEN_TIMEOUT)
                        .pollingEvery(Duration.ofMillis(150))
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            WebElement in = d.findElement(input);
                            String id = in.getAttribute("aria-controls");
                            return "true".equals(in.getAttribute("aria-expanded"))
                                    && id != null && !d.findElements(By.id(id)).isEmpty() ? id : null;
                        });
                System.out.println("Bulk Discount Type list opened in "
                        + (System.currentTimeMillis() - t0) + " ms (attempt " + attempt + ").");

                // 2) Read the options of this listbox only.
                By options = By.cssSelector("#" + listboxId + " [class*='select__option']");
                List<String> texts = new WebDriverWait(driver, DROPDOWN_OPEN_TIMEOUT)
                        .ignoring(StaleElementReferenceException.class)
                        .until(d -> {
                            List<String> found = d.findElements(options).stream()
                                    .map(o -> o.getText().trim())
                                    .filter(t -> !t.isEmpty())
                                    .collect(Collectors.toList());
                            return found.isEmpty() ? null : found;
                        });

                String chosen = texts.get(new Random().nextInt(texts.size()));
                System.out.println("Number of bulk discount types: " + texts.size());
                System.out.println("Selected bulk discount type: " + chosen);

                // 3) Click the chosen option inside the same listbox.
                By chosenOption = By.xpath("//*[@id='" + listboxId + "']"
                        + "//div[contains(@class,'select__option') and normalize-space(.)="
                        + xpathLiteral(chosen) + "]");
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.elementToBeClickable(chosenOption))
                        .click();

                System.out.println("Bulk Discount Type selected successfully: " + chosen
                        + " (total " + (System.currentTimeMillis() - started) + " ms)");
                return;

            } catch (TimeoutException | StaleElementReferenceException
                     | ElementClickInterceptedException e) {
                System.out.println("Bulk Discount Type attempt " + attempt + "/" + maxAttempts
                        + " failed after " + (System.currentTimeMillis() - t0) + " ms ("
                        + e.getClass().getSimpleName() + ") → clicking again.");
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            }
        }

        throw new IllegalStateException("Unable to select Bulk Discount Type after " + maxAttempts
                + " attempts (" + (System.currentTimeMillis() - started) + " ms).");
    }

    /** How long to wait for a React Select list to open before clicking again. */
    private static final Duration DROPDOWN_OPEN_TIMEOUT = Duration.ofSeconds(3);


    /**
     * Enters the Bulk Discount Amount as 10.
     * Uses the native HTML input value setter and dispatches input and change
     * events so the React application detects the entered value correctly.
     */
    public void enterBulkDiscountAmount() {

        String bulkDiscountAmount = "10";

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement amountField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        bulkDiscountAmountField
                )
        );

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const value = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(" +
                        "HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, value);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                amountField,
                bulkDiscountAmount
        );

        System.out.println(
                "Bulk discount amount: " + bulkDiscountAmount
        );
    }

    /**
     * Selects a random Tag from the available Purchase Order options.
     *
     * React Select may re-render the dropdown and recreate its option elements.
     * Therefore, this method never keeps Tag WebElements between operations.
     *
     * If React re-renders the dropdown during selection, the complete selection
     * attempt is restarted using fresh elements.
     */
    public void selectRandomTag() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Tag - attempt " +
                                attempt +
                                " of " +
                                maxAttempts
                );

                /**
                 * Re-locate the Tag dropdown on every attempt because React
                 * may recreate the element after another field is selected.
                 */
                WebElement dropdown =
                        wait.until(
                                ExpectedConditions.elementToBeClickable(
                                        tagDropdown
                                )
                        );

                dropdown.click();

                System.out.println(
                        "Tag dropdown opened."
                );

                /**
                 * Read the currently visible Tag options.
                 *
                 * The WebElements are used only during this single read
                 * operation. If React re-renders them, the wait retries.
                 */
                List<String> tags =
                        wait.until(driver -> {

                            try {

                                List<WebElement> elements =
                                        driver.findElements(tagOptions);

                                List<String> texts =
                                        new ArrayList<>();

                                for (WebElement element : elements) {

                                    if (!element.isDisplayed()) {
                                        continue;
                                    }

                                    String text =
                                            element.getText().trim();

                                    if (!text.isEmpty()
                                            && !texts.contains(text)) {

                                        texts.add(text);
                                    }
                                }

                                return texts.isEmpty()
                                        ? null
                                        : texts;

                            } catch (StaleElementReferenceException e) {

                                // React recreated the option elements.
                                return null;
                            }
                        });

                if (tags == null || tags.isEmpty()) {

                    throw new IllegalStateException(
                            "No Tags found in the Tag dropdown."
                    );
                }

                System.out.println(
                        "Number of tags: " +
                                tags.size()
                );

                /**
                 * Select a random Tag by its visible text.
                 */
                String selectedTag =
                        tags.get(
                                random.nextInt(tags.size())
                        );

                System.out.println(
                        "Selected tag: " +
                                selectedTag
                );

                /**
                 * Stable text-based locator for the selected Tag option.
                 *
                 * The option is located only after the random Tag has been
                 * selected, so no WebElement from the previous collection
                 * is reused.
                 */
                By selectedTagOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]" +
                                        "[normalize-space(.)=" +
                                        "'" +
                                        selectedTag.replace(
                                                "'",
                                                "&apos;"
                                        ) +
                                        "']"
                        );

                /**
                 * Re-find the selected option immediately before clicking it.
                 */
                WebElement option =
                        wait.until(
                                ExpectedConditions.elementToBeClickable(
                                        selectedTagOption
                                )
                        );

                option.click();

                System.out.println(
                        "Tag selected successfully: " +
                                selectedTag
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Tag dropdown was re-rendered. " +
                                "Restarting selection attempt " +
                                attempt +
                                " of " +
                                maxAttempts
                );
            } catch (TimeoutException e) {

                /**
                 * A React Select re-render can also manifest as a timeout
                 * instead of exposing StaleElementReferenceException.
                 *
                 * Therefore, restart the complete Tag selection attempt.
                 */
                System.out.println(
                        "Tag selection state changed or dropdown was " +
                                "re-rendered. " +
                                "Restarting attempt " +
                                attempt +
                                " of " +
                                maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Tag after " +
                        maxAttempts +
                        " attempts."
        );
    }

    /**
     * Selects a random "Download From" option from the available Purchase Order options.
     *
     * The method handles React Select re-rendering by re-finding the dropdown
     * and option elements whenever a StaleElementReferenceException occurs.
     * It reads the available option texts, selects one randomly, then re-finds
     * the selected option by its visible text before clicking it.
     */
    public void selectRandomDownloadFrom() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                downloadFromDropdown
                        )
                );

                dropdown.click();

                List<String> downloadFromValues = wait.until(driver -> {

                    List<WebElement> elements =
                            driver.findElements(downloadFromOptions);

                    List<String> texts = new ArrayList<>();

                    for (WebElement element : elements) {

                        try {

                            if (element.isDisplayed()) {

                                String text =
                                        element.getText().trim();

                                if (!text.isEmpty()) {
                                    texts.add(text);
                                }
                            }

                        } catch (StaleElementReferenceException ignored) {
                            return null;
                        }
                    }

                    return texts.isEmpty() ? null : texts;
                });

                if (downloadFromValues == null
                        || downloadFromValues.isEmpty()) {

                    throw new IllegalStateException(
                            "No Download From options found."
                    );
                }

                String selectedDownloadFrom =
                        downloadFromValues.get(
                                random.nextInt(
                                        downloadFromValues.size()
                                )
                        );

                System.out.println(
                        "Number of Download From options: "
                                + downloadFromValues.size()
                );

                System.out.println(
                        "Selected Download From: "
                                + selectedDownloadFrom
                );

                By selectedDownloadFromOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]" +
                                        "[normalize-space(.)=" +
                                        "'" + selectedDownloadFrom + "'" +
                                        "]"
                        );

                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedDownloadFromOption
                        )
                ).click();

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Download From dropdown was re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Download From after "
                        + maxAttempts
                        + " attempts because the React Select "
                        + "dropdown kept re-rendering."
        );
    }

    /**
     * Opens the Reference Number Grid.
     * Waits until the button becomes enabled and clickable, then clicks it.
     *
     * The button is initially disabled in the provided HTML, so the method
     * waits for the application to enable it before attempting the click.
     */
    public void openReferenceNumberGrid() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement button = wait.until(
                driver -> {
                    WebElement element =
                            driver.findElement(
                                    openReferenceNumberGridButton
                            );

                    return element.isEnabled()
                            && element.isDisplayed()
                            ? element
                            : null;
                }
        );

        button.click();
    }


    /**
     * Selects a random Reference Number from the Reference Number Grid
     * and verifies that the Reference Number dialog has been closed
     * before allowing the test flow to continue.
     *
     * The method intentionally re-locates the radio button before the
     * final double-click because AG Grid may rebuild its DOM during
     * reference-number loading or selection.
     *
     * After confirming the Reference Number, the method waits until
     * the Reference Number radio buttons disappear. This is used as
     * the synchronization point that confirms the dialog is no longer
     * active and prevents the next Purchase Order step from interacting
     * with the still-open Reference Number Grid.
     */
    public void selectRandomReferenceNumber() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Stable locator for all Reference Number radio buttons.
         *
         * The locator is also used later as the synchronization signal
         * to confirm that the Reference Number dialog has disappeared.
         */
        By referenceRadioButtonsLocator =
                referenceNumberRadioButtons;

        List<WebElement> radios = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        referenceRadioButtonsLocator
                )
        );

        if (radios.isEmpty()) {
            throw new IllegalStateException(
                    "No reference numbers found in the Grid."
            );
        }

        int randomIndex =
                new Random().nextInt(radios.size());

        /*
         * Read the selected row index immediately.
         * The WebElement itself is intentionally not reused later.
         */
        String value =
                radios.get(randomIndex).getAttribute("value");

        if (value == null || value.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Selected reference number radio button has no value."
            );
        }

        String rowIndex =
                value.trim();

        /**
         * Stable locator for the Reference Number code cell.
         *
         * The row-index comes directly from the selected radio button,
         * ensuring that the code belongs to the same AG Grid row.
         */
        By referenceNumberCodeCellLocator =
                By.cssSelector(
                        "div.ag-center-cols-container " +
                                "div[role='row'][row-index='" +
                                rowIndex +
                                "'] " +
                                "div[role='gridcell'][col-id='code']"
                );

        // Searched inside the open reference dialog only: the order's own
        // lines grid behind it uses the same classes and row-index values,
        // and with many lines (virtualization) its row N may not be rendered
        // (QC, 2026-10-27: Excel import with 37 lines → timeout on row-index 4).
        String referenceNumber =
                wait.until(d -> {
                    try {
                        for (WebElement cell : referenceGridScope(d)
                                .findElements(referenceNumberCodeCellLocator)) {
                            if (cell.isDisplayed()) {
                                return cell.getText().trim();
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // re-rendered; read again
                    }
                    return null;
                });

        if (referenceNumber.isEmpty()) {
            throw new IllegalStateException(
                    "Reference Number code is empty for row-index: " +
                            rowIndex
            );
        }

        /**
         * Stable locator for the selected Reference Number radio button.
         *
         * The element is located again immediately before confirmation
         * to avoid stale-element problems caused by AG Grid re-rendering.
         */
        By selectedRadioLocator =
                By.cssSelector(
                        "div.ag-pinned-right-cols-container " +
                                "button[role='radio'][value='" +
                                rowIndex +
                                "']"
                );

        WebElement selectedRadio =
                wait.until(d -> clickableIn(referenceGridScope(d), selectedRadioLocator));

        /*
         * Confirm the selected Reference Number.
         */
        new Actions(driver)
                .doubleClick(selectedRadio)
                .perform();

        System.out.println(
                "Number of reference numbers: " +
                        radios.size()
        );

        System.out.println(
                "Selected reference number: " +
                        referenceNumber
        );

        System.out.println(
                "Selected radio value: " +
                        value
        );

        /*
         * IMPORTANT:
         *
         * The double-click only confirms the selection.
         * We must also verify that the Reference Number dialog
         * has actually disappeared before continuing.
         */
        try {

            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            referenceRadioButtonsLocator
                    )
            );

            System.out.println(
                    "Reference Number dialog closed successfully."
            );

        } catch (TimeoutException e) {

            /*
             * The Reference Number was selected, but the dialog
             * remained open. Try the confirmation once more because
             * this is an intermittent UI synchronization issue.
             */
            System.out.println(
                    "Reference Number dialog is still open. " +
                            "Retrying confirmation..."
            );

            WebElement retryRadio =
                    wait.until(d -> clickableIn(referenceGridScope(d), selectedRadioLocator));

            new Actions(driver)
                    .doubleClick(retryRadio)
                    .perform();

            /*
             * The dialog MUST disappear after the retry.
             * If it does not, fail here with a meaningful message
             * instead of allowing the next grid operation to fail.
             */
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            referenceRadioButtonsLocator
                    )
            );

            System.out.println(
                    "Reference Number dialog closed successfully after retry."
            );
        }

        System.out.println(
                "Reference number confirmed."
        );
    }





    /**
     * Enters the Description (البيان) value in the Purchase Order form.
     *
     * Re-finds the description input before every attempt to handle React
     * re-rendering and stale element references. Uses the native HTML input
     * value setter and dispatches input and change events to notify React.
     */
    public void enterDescription() {

        String description = "أمر شراء تجريبي";

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        int maxAttempts = 3;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                /*
                 * Locate the current description input from the latest DOM.
                 */
                WebElement descriptionInput = wait.until(
                        ExpectedConditions.refreshed(
                                ExpectedConditions.visibilityOfElementLocated(
                                        descriptionField
                                )
                        )
                );

                /*
                 * Set the input value and notify the React application.
                 */
                js.executeScript(
                        "const input = arguments[0];" +
                                "const value = arguments[1];" +
                                "const setter = Object.getOwnPropertyDescriptor(" +
                                "HTMLInputElement.prototype, 'value').set;" +
                                "setter.call(input, value);" +
                                "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                                "input.dispatchEvent(new Event('change', { bubbles: true }));",
                        descriptionInput,
                        description
                );

                /*
                 * Confirm that the current input contains the expected value.
                 */
                wait.until(currentDriver -> {

                    try {

                        WebElement currentInput =
                                currentDriver.findElement(descriptionField);

                        return description.equals(
                                currentInput.getAttribute("value")
                        );

                    } catch (StaleElementReferenceException e) {

                        return false;
                    }
                });

                System.out.println(
                        "Description entered successfully: " + description
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Description input was re-rendered. Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Failed to enter the Purchase Order description after "
                        + maxAttempts
                        + " attempts because the input kept becoming stale."
        );
    }


    /**
     * Clicks "اضافة سطر" and captures the row it adds ({@code newRowIndex},
     * {@code newRow}, {@code newItemTestId}) for the line-filling methods.
     *
     * <p>The new line is identified by its row-index: it is always appended
     * at the end, so its row-index equals the number of lines before the
     * click. The earlier version compared the rendered Item cells before and
     * after the click; with many lines (QC, 2026-10-27: 39 lines) AG Grid
     * renders only ~20 rows, so the rendered set shifts while scrolling and
     * the cells captured "before" were already stale (StaleElementReference
     * in clickAddLine).</p>
     *
     * <p>Every lookup is done by row-index in a single script call, never
     * through a WebElement kept from an earlier step.</p>
     */
    public void clickAddLine() {

        By addLineButtonLocator =
                By.xpath("//button[normalize-space()='اضافة سطر']");

        int linesBefore = countGridLines();
        System.out.println("Lines before Add Line: " + linesBefore);

        for (int attempt = 1; attempt <= 3; attempt++) {

            WebElement addLineButton = new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(addLineButtonLocator));

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    addLineButton);

            new Actions(driver)
                    .moveToElement(driver.findElement(addLineButtonLocator))
                    .click()
                    .perform();

            System.out.println("Add Line button clicked (attempt " + attempt + "/3).");

            Integer added = waitForLineCount(linesBefore + 1, Duration.ofSeconds(5));

            if (added != null) {
                newRowIndex = linesBefore;          // appended at the end
                newRow = locateNewRow();
                // Only the items grid has an Item column; the "الخصومات
                // والإضافات" grid uses the same button but has no itemId cell.
                List<WebElement> itemCells = newRow.findElements(
                        By.cssSelector("div[role='gridcell'][col-id='itemId']"));
                newItemTestId = itemCells.isEmpty()
                        ? null
                        : itemCells.get(0).getAttribute("data-testid");

                System.out.println("New line added. Row index: " + newRowIndex
                        + " | Item test id: " + newItemTestId);
                return;
            }

            System.out.println("Add Line click did not add a line; retrying.");
        }

        throw new IllegalStateException(
                "\"اضافة سطر\" did not add a line after 3 clicks (lines stayed "
                        + linesBefore + ").");
    }

    /** Scrolls the detail grid to its end and returns the number of lines (last row-index + 1). */
    private int countGridLines() {

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "const cc=[...document.querySelectorAll('.ag-center-cols-container')].find(c => c.offsetParent !== null);"
                        + "const vp=cc && cc.closest('.ag-root').querySelector('.ag-body-viewport');"
                        + "if(vp) vp.scrollTop = vp.scrollHeight;");
        waitForGridColumnsToRender(); // one render after the scroll, instead of 400 ms

        // Only the grid of the visible tab (items or discounts/additions).
        Number last = (Number) js.executeScript(
                "const cc=[...document.querySelectorAll('.ag-center-cols-container')].find(c => c.offsetParent !== null);"
                        + "if(!cc) return -1;"
                        + "const idx=[...cc.querySelectorAll('div[role=row][row-index]')]"
                        + ".map(r => r.getAttribute('row-index'))"
                        + ".filter(i => /^\\d+$/.test(i)).map(Number);"
                        + "return idx.length ? Math.max(...idx) : -1;");

        return last == null ? 0 : last.intValue() + 1;
    }

    /** Waits until the grid has {@code expected} lines; returns it, or null on timeout. */
    private Integer waitForLineCount(int expected, Duration timeout) {

        long end = System.currentTimeMillis() + timeout.toMillis();
        while (System.currentTimeMillis() < end) {
            if (countGridLines() >= expected) {
                return expected;
            }
            sleepQuietly(300);
        }
        return null;
    }

    /**
     * The row of the line added by the last {@link #clickAddLine()}, freshly
     * located (scrolls it into the rendered window first).
     */
    private WebElement locateNewRow() {

        By rowLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-index='" + newRowIndex + "']");

        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    for (WebElement row : d.findElements(rowLocator)) {
                        if (row.isDisplayed()) {      // grid of the visible tab
                            return row;
                        }
                    }
                    ((JavascriptExecutor) d).executeScript(
                            "const cc=[...document.querySelectorAll('.ag-center-cols-container')].find(c => c.offsetParent !== null);"
                                    + "const vp=cc && cc.closest('.ag-root').querySelector('.ag-body-viewport');"
                                    + "if(vp) vp.scrollTop = vp.scrollHeight;");
                    return null;
                });
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Selects a random Item Name from the React Select dropdown inside
     * the newly added Purchase Order row.
     *
     * The method uses the previously captured newRow to avoid dynamic
     * row identifiers and locators. It opens the Item Name cell, activates
     * the React Select editor, retrieves the visible options, and randomly
     * selects one item.
     */
    public void selectRandomItem() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        if (newRow == null) {
            throw new IllegalStateException(
                    "New row was not captured. Call clickAddLine() first."
            );
        }

        // Re-locate: the row captured by clickAddLine() may have been
        // re-rendered since (AG Grid row virtualization).
        newRow = locateNewRow();

        System.out.println("Using captured new row.");

        System.out.println(
                "New row index: " +
                        newRow.getAttribute("row-index")
        );

        // QC, 2026-10-31: right after "Add Line" the grid is still re-rendering,
        // so one click + ENTER on the new Item cell was sometimes lost (it had
        // been hidden by a fixed 300 ms sleep). Open the editor with retries on
        // a freshly located cell: double-click → click + ENTER → click + F2.
        String rowIndex = newRow.getAttribute("row-index");
        By itemCellOfNewRow = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-index='" + rowIndex + "'] "
                        + "div[role='gridcell'][col-id='itemId']");
        By reactSelectInput =
                By.cssSelector(
                        ".ag-popup-editor input.select__input"
                );

        WebElement editor = openCellEditorWithRetry(
                "Item (row " + rowIndex + ")",
                itemCellOfNewRow,
                ExpectedConditions.visibilityOfElementLocated(reactSelectInput));

        System.out.println(
                "React Select editor found."
        );

        editor.click();

        System.out.println(
                "Item dropdown opened."
        );

        String listboxId =
                editor.getAttribute("aria-controls");

        if (listboxId == null || listboxId.isBlank()) {
            throw new IllegalStateException(
                    "React Select listbox ID was not found."
            );
        }

        System.out.println(
                "React Select listbox ID: " +
                        listboxId
        );

        By optionLocator =
                By.cssSelector(
                        "#" + listboxId +
                                " div[class*='select__option']"
                );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        optionLocator
                )
        );

        List<WebElement> options =
                driver.findElements(optionLocator)
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .toList();

        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No visible items found in Item dropdown."
            );
        }

        Random random = new Random();

        int randomIndex =
                random.nextInt(options.size());

        WebElement selectedOption =
                options.get(randomIndex);

        String selectedItem =
                selectedOption.getText().trim();

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();",
                selectedOption
        );

        System.out.println(
                "Number of visible items: " +
                        options.size()
        );

        System.out.println(
                "Selected item: " +
                        selectedItem
        );

        System.out.println(
                "Item selected successfully in NEW ROW."
        );
    }

    /**
     * Enters the requested quantity into the Quantity cell of the newly added
     * Purchase Order AG Grid row.
     *
     * The Quantity cell is located through the unique Item cell data-testid
     * stored in newItemTestId, ensuring that the method edits only the new
     * row even when the Purchase Order contains duplicated existing rows.
     *
     * The method re-locates the inline editor before each interaction to handle
     * React/AG Grid DOM re-rendering and prevent StaleElementReferenceException.
     */
    public void enterQuantity(String quantity) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        /*
         * Build the Quantity locator specifically for the newly added row.
         *
         * newItemTestId is the stable Item-cell anchor captured by clickAddLine().
         */
        By quantityCellLocator =
                getQuantityCellLocatorForNewRow();

        System.out.println("========================================");
        System.out.println("Entering quantity");
        System.out.println("Requested quantity: " + quantity);
        System.out.println(
                "New Item test id: " +
                        newItemTestId
        );

        /*
         * Make sure a valid Item test id exists before attempting
         * to locate the Quantity cell.
         */
        if (newItemTestId == null ||
                newItemTestId.isBlank()) {

            throw new IllegalStateException(
                    "newItemTestId is empty. " +
                            "Call clickAddLine() before enterQuantity()."
            );
        }

        WebElement quantityCell =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                quantityCellLocator
                        )
                );

        System.out.println(
                "Quantity cell found in NEW ROW."
        );

        System.out.println(
                "Quantity cell text BEFORE editing: [" +
                        quantityCell.getText() +
                        "]"
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                quantityCell
        );

        /*
         * Re-locate the Quantity cell after scrolling because AG Grid
         * may re-render the cell during scrolling.
         */
        quantityCell =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                quantityCellLocator
                        )
                );

        quantityCell.click();

        System.out.println(
                "NEW ROW Quantity cell clicked."
        );

        /*
         * Press ENTER to activate AG Grid inline editing.
         *
         * The cell is re-located immediately before the interaction
         * to avoid using a stale WebElement.
         */
        quantityCell =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                quantityCellLocator
                        )
                );

        quantityCell.sendKeys(Keys.ENTER);

        System.out.println(
                "ENTER sent to NEW ROW Quantity cell."
        );

        /*
         * Wait until the inline Quantity editor is created.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityEditorLocator
                )
        );

        System.out.println(
                "NEW ROW Quantity editor found."
        );

        /*
         * Re-locate the editor immediately before clearing its value.
         * React/AG Grid may replace the editor element during rendering.
         */
        wait.until(driver -> {

            try {

                WebElement input =
                        driver.findElement(quantityEditorLocator);

                return input.isDisplayed();

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        /*
         * Clear the current value using the native HTMLInputElement setter.
         *
         * The editor is located again inside JavaScript execution to avoid
         * interacting with a stale editor element.
         */
        wait.until(driver -> {

            try {

                WebElement input =
                        driver.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
    
                        const setter =
                            Object.getOwnPropertyDescriptor(
                                HTMLInputElement.prototype,
                                'value'
                            ).set;
    
                        setter.call(input, '');
    
                        input.dispatchEvent(
                            new Event('input', { bubbles: true })
                        );
    
                        input.dispatchEvent(
                            new Event('change', { bubbles: true })
                        );
                        """,
                        input
                );

                return true;

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        System.out.println(
                "Existing NEW ROW Quantity value cleared."
        );

        /*
         * Re-locate the editor again before entering the requested quantity.
         */
        wait.until(driver -> {

            try {

                WebElement input =
                        driver.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
                        const value = arguments[1];
    
                        const setter =
                            Object.getOwnPropertyDescriptor(
                                HTMLInputElement.prototype,
                                'value'
                            ).set;
    
                        setter.call(input, value);
    
                        input.dispatchEvent(
                            new Event('input', { bubbles: true })
                        );
    
                        input.dispatchEvent(
                            new Event('change', { bubbles: true })
                        );
                        """,
                        input,
                        quantity
                );

                return true;

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        System.out.println(
                "Requested Quantity assigned to NEW ROW: " +
                        quantity
        );

        /*
         * Verify that the newly rendered editor contains the requested value.
         */
        wait.until(driver -> {

            try {

                WebElement input =
                        driver.findElement(quantityEditorLocator);

                String value =
                        input.getAttribute("value");

                System.out.println(
                        "NEW ROW Quantity input current value: [" +
                                value +
                                "]"
                );

                return quantity.equals(value);

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        System.out.println(
                "NEW ROW Quantity input contains requested value."
        );

        /*
         * Re-locate the editor one final time before pressing ENTER.
         */
        WebElement quantityInput =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                quantityEditorLocator
                        )
                );

        quantityInput.sendKeys(Keys.ENTER);

        System.out.println(
                "ENTER sent to commit NEW ROW Quantity."
        );

        /*
         * Verify the final value displayed by AG Grid after committing.
         *
         * The Quantity cell is located again through newItemTestId,
         * so verification is performed against the NEW ROW only.
         */
        wait.until(driver -> {

            try {

                WebElement cell =
                        driver.findElement(
                                quantityCellLocator
                        );

                String value =
                        cell.getText().trim();

                System.out.println(
                        "NEW ROW Quantity cell value after commit: [" +
                                value +
                                "]"
                );

                return value.equals(quantity)
                        || value.equals(quantity + ".0")
                        || value.equals(quantity + ".00")
                        || value.equals(quantity + ".000");

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        System.out.println(
                "NEW ROW Quantity entered successfully: " +
                        quantity
        );

        System.out.println("========================================");
    }


    /**
     * Enters the requested Cost value into the Cost cell of the
     * newly added Purchase Order AG Grid row.
     *
     * The new row is identified through the unique Item-cell
     * data-testid stored in newItemTestId.
     *
     * Unlike the Quantity cell, the Cost cell enters edit mode
     * immediately after clicking it. Therefore, no ENTER key is
     * sent to activate the Cost editor.
     *
     * The actual Cost editor is the AG Grid text input created
     * inside the Cost cell:
     *
     *     input.ag-input-field-input.ag-text-field-input
     *
     * The value is assigned using the native HTML input setter
     * and an input event so that the React/AG Grid application
     * receives the changed value correctly.
     *
     * TAB is used only to commit the Cost value and move to the
     * next grid cell. ENTER is intentionally avoided because
     * ENTER can trigger the Add Line behavior in this grid.
     *
     * After TAB, AG Grid may re-render the row and invalidate
     * the previous WebElement reference. Therefore, the Cost
     * cell is re-located using the stable locator before reading
     * the final committed value.
     */
    public void enterPrice(String price) {

        System.out.println("========================================");
        System.out.println("Entering price");
        System.out.println("Requested price: " + price);
        System.out.println(
                "New Item test id: " + newItemTestId
        );

        if (newItemTestId == null || newItemTestId.isBlank()) {

            throw new IllegalStateException(
                    "New Item test id was not captured. " +
                            "Call clickAddLine() before enterPrice()."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(10)
                );

        /**
         * Locates the Cost cell specifically inside the newly added
         * Purchase Order AG Grid row.
         *
         * The unique Item data-testid is used as the stable row anchor
         * so that Cost is never entered into one of the copied rows.
         */
        By costCellLocator =
                getCostCellLocatorForNewRow();

        WebElement costCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                costCellLocator
                        )
                );

        System.out.println(
                "Cost cell found in NEW ROW."
        );

        System.out.println(
                "Cost cell text BEFORE editing: ["
                        + costCell.getText()
                        + "]"
        );

        /**
         * Scroll the Cost cell into the visible viewport before
         * activating its AG Grid inline editor.
         */
        ((JavascriptExecutor) driver).executeScript(
                """
                arguments[0].scrollIntoView({
                    block: 'center',
                    inline: 'nearest'
                });
                """,
                costCell
        );

        /**
         * Click the Cost cell.
         *
         * Cost enters inline-editing mode directly after the click,
         * so ENTER must NOT be sent here.
         */
        new Actions(driver)
                .moveToElement(costCell)
                .click()
                .perform();

        System.out.println(
                "NEW ROW Cost cell clicked."
        );

        /**
         * Locates the actual AG Grid Cost editor created inside the
         * newly activated Cost cell.
         *
         * The editor is scoped to the new row's Cost cell so that an
         * input belonging to another field cannot be selected.
         */
        By costInputLocator =
                By.xpath(
                        "//div[@role='gridcell' " +
                                "and @col-id='itemId' " +
                                "and @data-testid='" + newItemTestId + "']" +
                                "/ancestor::div[@role='row'][1]" +
                                "//div[@role='gridcell' " +
                                "and @col-id='cost']" +
                                "//input[" +
                                "contains(@class,'ag-input-field-input')" +
                                " and " +
                                "contains(@class,'ag-text-field-input')" +
                                "]"
                );

        WebElement costInput =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                costInputLocator
                        )
                );

        System.out.println(
                "NEW ROW Cost editor found."
        );

        System.out.println(
                "Existing price value: ["
                        + costInput.getAttribute("value")
                        + "]"
        );

        /**
         * Clears the existing Cost value using the native HTML
         * input value setter and dispatches an input event so that
         * the React/AG Grid application receives the change.
         */
        ((JavascriptExecutor) driver).executeScript(
                """
                const input = arguments[0];
    
                const nativeSetter =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                nativeSetter.call(input, '');
    
                input.dispatchEvent(
                    new Event('input', {
                        bubbles: true
                    })
                );
                """,
                costInput
        );

        System.out.println(
                "Existing NEW ROW Cost value cleared."
        );

        /**
         * Assigns the requested Cost value using the native HTML
         * input setter and dispatches an input event.
         */
        ((JavascriptExecutor) driver).executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const nativeSetter =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                nativeSetter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', {
                        bubbles: true
                    })
                );
                """,
                costInput,
                price
        );

        System.out.println(
                "Requested Cost assigned to NEW ROW: "
                        + price
        );

        /**
         * Verifies the value directly from the active Cost editor
         * before committing it.
         */
        String currentPrice =
                costInput.getAttribute("value");

        System.out.println(
                "NEW ROW Cost input current value: ["
                        + currentPrice
                        + "]"
        );

        if (!price.equals(currentPrice)) {

            throw new IllegalStateException(
                    "NEW ROW Cost input does not contain " +
                            "the requested value. Requested=["
                            + price
                            + "], Actual=["
                            + currentPrice
                            + "]"
            );
        }

        System.out.println(
                "NEW ROW Cost input contains requested value."
        );

        /**
         * TAB commits the Cost value.
         *
         * ENTER is intentionally not used because it can trigger
         * Add Line behavior in this AG Grid.
         */
        new Actions(driver)
                .sendKeys(Keys.TAB)
                .perform();

        System.out.println(
                "TAB sent to commit NEW ROW Cost."
        );

        /**
         * AG Grid may re-render the row immediately after TAB.
         * Therefore, do NOT reuse the old costCell WebElement.
         *
         * Instead, repeatedly locate the Cost cell using the stable
         * costCellLocator until the cell is no longer in inline
         * editing mode.
         */
        wait.until(driver -> {

            try {

                WebElement currentCostCell =
                        driver.findElement(costCellLocator);

                String classAttribute =
                        currentCostCell.getAttribute("class");

                return classAttribute != null
                        && !classAttribute.contains(
                        "ag-cell-inline-editing"
                );

            } catch (StaleElementReferenceException e) {

                /**
                 * AG Grid replaced the cell while it was being checked.
                 * Returning false causes Selenium to locate the newly
                 * rendered cell on the next polling attempt.
                 */
                return false;
            }
        });

        /**
         * Re-locate the Cost cell one final time after AG Grid
         * finishes rendering.
         *
         * This guarantees that the WebElement used for getText()
         * belongs to the current DOM and is not stale.
         */
        WebElement updatedCostCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                costCellLocator
                        )
                );

        /**
         * Read the final formatted Cost value displayed by AG Grid.
         */
        String finalPrice =
                updatedCostCell.getText().trim();

        System.out.println(
                "NEW ROW Cost cell value after commit: ["
                        + finalPrice
                        + "]"
        );

        System.out.println(
                "Price entered successfully."
        );

        System.out.println(
                "Requested price: [" + price + "]"
        );

        System.out.println(
                "Final ERP cost value: [" + finalPrice + "]"
        );

        System.out.println("========================================");
    }


    /**
     * Enters the Discount Rate value into the Discount Rate cell
     * of the newly added Purchase Order row.
     *
     * The method first locates the Discount Rate cell using the new row's
     * unique Item data-testid, then activates the cell editor with F2.
     *
     * The value is assigned directly to the AG Grid input using JavaScript
     * instead of sendKeys(), preventing partial input such as entering only
     * the first digit of a multi-digit value.
     *
     * TAB is used to commit the value because ENTER may trigger AG Grid
     * row creation instead of committing the current cell.
     */
    public void enterDiscountRate(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        By discountRateCellLocator =
                getDiscountRateCellLocatorForNewRow();

        // Locate the Discount Rate cell inside the newly added row.
        WebElement discountRateCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                discountRateCellLocator
                        )
                );

        System.out.println(
                "Discount Rate cell found in NEW ROW."
        );

        System.out.println(
                "Discount Rate cell text BEFORE editing: [" +
                        discountRateCell.getText() + "]"
        );

        // Locate the Total cell in the same newly added row.
        By totalCellLocator =
                By.xpath(
                        "//div[@role='gridcell' " +
                                "and @col-id='itemId' " +
                                "and @data-testid='" +
                                newItemTestId + "']" +
                                "/ancestor::div[@role='row'][1]" +
                                "//div[@role='gridcell' " +
                                "and @col-id='total']"
                );

        WebElement totalCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                totalCellLocator
                        )
                );

        System.out.println(
                "Total cell found in NEW ROW."
        );

        System.out.println(
                "Total cell text BEFORE navigation: [" +
                        totalCell.getText() + "]"
        );

        // Establish AG Grid keyboard focus from the Total cell.
        totalCell.click();

        System.out.println(
                "Total cell clicked to establish AG Grid focus."
        );

        // Move one cell left: Total -> Discount Rate.
        totalCell.sendKeys(Keys.ARROW_LEFT);

        System.out.println(
                "ARROW_LEFT sent from Total toward Discount Rate."
        );

        // Verify that Discount Rate is actually the focused AG Grid cell.
        wait.until(driver -> {

            try {

                WebElement currentDiscountCell =
                        driver.findElement(discountRateCellLocator);

                String classAttribute =
                        currentDiscountCell.getAttribute("class");

                return classAttribute != null
                        && classAttribute.contains("ag-cell-focus");

            } catch (StaleElementReferenceException e) {

                return false;
            }
        });

        System.out.println(
                "Discount Rate cell received AG Grid focus."
        );

        // F2 activates editing without triggering AG Grid row creation.
        discountRateCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                discountRateCellLocator
                        )
                );

        discountRateCell.sendKeys(Keys.F2);

        System.out.println(
                "F2 sent to focused NEW ROW Discount Rate cell."
        );

        // Locate the actual AG Grid input editor.
        By discountRateEditorLocator =
                By.xpath(
                        "//div[@role='gridcell' " +
                                "and @col-id='itemId' " +
                                "and @data-testid='" +
                                newItemTestId + "']" +
                                "/ancestor::div[@role='row'][1]" +
                                "//div[@role='gridcell' " +
                                "and @col-id='discountRate']" +
                                "//input[" +
                                "contains(@class,'ag-input-field-input')" +
                                " and " +
                                "contains(@class,'ag-text-field-input')" +
                                "]"
                );

        WebElement discountRateInput =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                discountRateEditorLocator
                        )
                );

        System.out.println(
                "NEW ROW Discount Rate editor found."
        );

        // Select and clear the existing editor value.
        discountRateInput.sendKeys(
                Keys.CONTROL,
                "a"
        );

        discountRateInput.sendKeys(Keys.BACK_SPACE);

        System.out.println(
                "Existing NEW ROW Discount Rate value cleared."
        );

        // Assign the complete value directly to the AG Grid input.
        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                discountRateInput,
                discountRate
        );

        System.out.println(
                "Requested Discount Rate assigned to NEW ROW: " +
                        discountRate
        );

        // Verify the entire requested value was assigned.
        String currentDiscountRate =
                discountRateInput.getAttribute("value");

        System.out.println(
                "NEW ROW Discount Rate input current value: [" +
                        currentDiscountRate + "]"
        );

        if (!discountRate.equals(currentDiscountRate)) {

            throw new AssertionError(
                    "Discount Rate input contains [" +
                            currentDiscountRate +
                            "] instead of requested [" +
                            discountRate +
                            "]"
            );
        }

        System.out.println(
                "NEW ROW Discount Rate input contains requested value."
        );

        // TAB commits the current cell without creating a new row.
        discountRateInput.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit NEW ROW Discount Rate."
        );

        // Wait until AG Grid exits inline-editing mode.
        wait.until(driver -> {

            try {

                WebElement currentDiscountCell =
                        driver.findElement(discountRateCellLocator);

                String classAttribute =
                        currentDiscountCell.getAttribute("class");

                return classAttribute != null
                        && !classAttribute.contains(
                        "ag-cell-inline-editing"
                );

            } catch (StaleElementReferenceException e) {

                return false;
            }
        });

        // Re-locate the cell because AG Grid may re-render it after commit.
        WebElement updatedDiscountRateCell =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                discountRateCellLocator
                        )
                );

        String finalDiscountRate =
                updatedDiscountRateCell.getText().trim();

        System.out.println(
                "NEW ROW Discount Rate cell value after commit: [" +
                        finalDiscountRate + "]"
        );

        System.out.println(
                "Discount Rate entered successfully."
        );
    }



    /**
     * Enters the Discount Value in the newly added Purchase Order AG Grid row.
     *
     * The Discount Value cell receives keyboard focus automatically after the
     * Discount Rate (%) value is committed, so no additional mouse click is
     * required.
     *
     * The new row is identified through the stable Item data-testid rather than
     * a hard-coded row index. The first character activates AG Grid inline
     * editing, after which the complete requested value is assigned through
     * the native HTML input setter and committed with TAB.
     */
    public void enterDiscountValue(String discountValue) {

        System.out.println("========================================");
        System.out.println("Entering Discount Value");
        System.out.println("Requested discount value: " + discountValue);
        System.out.println("New Item test id: " + newItemTestId);

        if (newItemTestId == null || newItemTestId.isBlank()) {

            throw new IllegalStateException(
                    "New Item test id was not captured. " +
                            "Call clickAddLine() before enterDiscountValue()."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(10)
                );

        /**
         * Locates the Discount Value cell specifically inside the newly added
         * Purchase Order AG Grid row.
         *
         * The stable Item data-testid is used as the row anchor so that the
         * Discount Value is never entered into an existing/copied row.
         */
        By discountValueCellLocator =
                getDiscountValueCellLocatorForNewRow();

        WebElement discountCell =
                wait.until(
                        ExpectedConditions.presenceOfElementLocated(
                                discountValueCellLocator
                        )
                );

        System.out.println(
                "Discount Value cell found in NEW ROW."
        );

        System.out.println(
                "Discount Value cell BEFORE editing: [" +
                        discountCell.getText() +
                        "]"
        );

        /**
         * Wait until AG Grid moves keyboard focus to the Discount Value cell.
         *
         * The Discount Rate method commits with TAB, therefore AG Grid should
         * naturally move focus to this cell without requiring another click.
         */
        wait.until(driver -> {

            try {

                WebElement currentCell =
                        driver.findElement(
                                discountValueCellLocator
                        );

                String classAttribute =
                        currentCell.getAttribute("class");

                return classAttribute != null
                        && classAttribute.contains(
                        "ag-cell-focus"
                );

            } catch (StaleElementReferenceException e) {

                return false;
            }
        });

        System.out.println(
                "Discount Value cell is focused and ready for keyboard input."
        );

        /**
         * Send only the first character to the focused Discount Value cell.
         *
         * AG Grid uses the first keyboard character to activate its inline
         * editor. The complete value will be assigned to the actual editor
         * immediately after it is created.
         */
        String firstCharacter =
                discountValue.substring(0, 1);

        new Actions(driver)
                .sendKeys(firstCharacter)
                .perform();

        System.out.println(
                "First Discount Value character sent: " +
                        firstCharacter
        );

        /**
         * Locates the actual AG Grid Discount Value editor inside the
         * newly added row.
         *
         * The locator is scoped to the row identified by newItemTestId,
         * preventing an editor belonging to another row from being selected.
         */
        By discountValueEditorLocator =
                By.xpath(
                        "//div[@role='gridcell' " +
                                "and @col-id='itemId' " +
                                "and @data-testid='" + newItemTestId + "']" +
                                "/ancestor::div[@role='row'][1]" +
                                "//div[@role='gridcell' " +
                                "and @col-id='discountValue' " +
                                "and contains(@class,'ag-cell-inline-editing')]" +
                                "//input[" +
                                "@data-ref='eInput' " +
                                "and @aria-label='Input Editor'" +
                                "]"
                );

        WebElement discountEditor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                discountValueEditorLocator
                        )
                );

        System.out.println(
                "NEW ROW Discount Value editor found."
        );

        System.out.println(
                "Discount Value editor BEFORE setting complete value: [" +
                        discountEditor.getAttribute("value") +
                        "]"
        );

        /**
         * Sets the complete Discount Value using the native HTML input setter.
         *
         * The input event informs React/AG Grid that the editor value has
         * changed, while the change event preserves normal application
         * change handling.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', {
                        bubbles: true
                    })
                );
    
                input.dispatchEvent(
                    new Event('change', {
                        bubbles: true
                    })
                );
                """,
                discountEditor,
                discountValue
        );

        System.out.println(
                "Complete Discount Value assigned to NEW ROW: " +
                        discountValue
        );

        /**
         * Verify that the active AG Grid editor contains the complete
         * requested value before committing it.
         */
        wait.until(driver -> {

            try {

                WebElement currentEditor =
                        driver.findElement(
                                discountValueEditorLocator
                        );

                return discountValue.equals(
                        currentEditor.getAttribute("value")
                );

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        String editorValue =
                discountEditor.getAttribute("value");

        System.out.println(
                "NEW ROW Discount Value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!discountValue.equals(editorValue)) {

            throw new IllegalStateException(
                    "NEW ROW Discount Value editor does not contain " +
                            "the requested value. Requested=[" +
                            discountValue +
                            "], Actual=[" +
                            editorValue +
                            "]"
            );
        }

        System.out.println(
                "NEW ROW Discount Value contains requested value."
        );

        /**
         * TAB commits the Discount Value and allows AG Grid to move the
         * keyboard focus naturally to Addition Rate (%).
         *
         * ENTER is intentionally NOT used because it can trigger Add Line
         * behavior in this Purchase Order grid.
         */
        discountEditor.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit NEW ROW Discount Value."
        );

        /*
         * Do not wait for the editor to become invisible.
         * Do not reuse the old discountEditor WebElement after TAB.
         *
         * AG Grid is responsible for committing the value and moving
         * keyboard focus to Addition Rate (%).
         *
         * The next operation can continue from the newly focused cell.
         */
        System.out.println(
                "Discount Value committed successfully."
        );

        System.out.println(
                "Keyboard focus should now be on Addition Rate (%)."
        );

        System.out.println("========================================");
    }
    /**
     * Enters the Addition Rate (%) value in the newly added Purchase Order
     * AG Grid row.
     *
     * The Discount Value method commits with TAB, which naturally moves
     * AG Grid keyboard focus to the Addition Rate (%) cell.
     *
     * No mouse click is performed on the Addition Rate cell. The method
     * uses the existing keyboard focus, activates AG Grid inline editing
     * with the first character, assigns the complete value through the
     * native HTML input setter, and commits it with TAB.
     *
     * The new row is identified using newItemTestId instead of a hard-coded
     * AG Grid row index, making the method stable after grid re-rendering.
     */
    public void enterExtraRate(String extraRate) {

        System.out.println("========================================");
        System.out.println("Entering Addition Rate");
        System.out.println("Requested addition rate: " + extraRate);
        System.out.println("New Item test id: " + newItemTestId);

        if (newItemTestId == null || newItemTestId.isBlank()) {

            throw new IllegalStateException(
                    "New Item test id was not captured. " +
                            "Call clickAddLine() before enterExtraRate()."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(10)
                );

        /**
         * Locates the Addition Rate (%) cell specifically inside the newly
         * added row using the stable Item data-testid as the row anchor.
         */
        By extraRateCellLocator =
                getExtraRateCellLocatorForNewRow();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        extraRateCellLocator
                )
        );

        System.out.println(
                "Addition Rate cell found in NEW ROW."
        );

        /**
         * Wait until AG Grid moves keyboard focus to the Addition Rate cell.
         *
         * The previous Discount Value operation commits with TAB, so no
         * additional click is required here.
         *
         * The cell is re-located on every polling attempt because AG Grid
         * may re-render the row while keyboard focus is moving.
         */
        wait.until(driver -> {

            try {

                WebElement currentExtraRateCell =
                        driver.findElement(
                                extraRateCellLocator
                        );

                String classAttribute =
                        currentExtraRateCell.getAttribute("class");

                return classAttribute != null
                        && classAttribute.contains(
                        "ag-cell-focus"
                );

            } catch (StaleElementReferenceException e) {

                return false;
            }
        });

        System.out.println(
                "Addition Rate cell is focused through AG Grid keyboard navigation."
        );

        /**
         * Send only the first character to the focused cell.
         *
         * AG Grid uses the first keyboard character to activate its
         * inline editor. The complete value is assigned after the
         * editor has been created.
         */
        String firstCharacter =
                extraRate.substring(0, 1);

        new Actions(driver)
                .sendKeys(firstCharacter)
                .perform();

        System.out.println(
                "First addition-rate character sent: " +
                        firstCharacter
        );

        /**
         * Locate the actual AG Grid Addition Rate editor created inside
         * the NEW row after the first character activates editing.
         */
        By extraRateEditorLocator =
                getExtraRateEditorLocatorForNewRow();

        WebElement extraRateEditor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraRateEditorLocator
                        )
                );

        System.out.println(
                "NEW ROW Addition Rate editor found."
        );

        /**
         * Set the complete Addition Rate value using the native HTML
         * input setter and notify React/AG Grid through input and change
         * events.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', {
                        bubbles: true
                    })
                );
    
                input.dispatchEvent(
                    new Event('change', {
                        bubbles: true
                    })
                );
                """,
                extraRateEditor,
                extraRate
        );

        System.out.println(
                "Complete Addition Rate assigned to NEW ROW: " +
                        extraRate
        );

        /**
         * Verify that the active editor contains the complete requested
         * value before committing it.
         *
         * The editor is re-located on every polling attempt because React/
         * AG Grid may recreate it after the JavaScript value assignment.
         */
        wait.until(driver -> {

            try {

                WebElement currentEditor =
                        driver.findElement(
                                extraRateEditorLocator
                        );

                return extraRate.equals(
                        currentEditor.getAttribute("value")
                );

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        /**
         * Re-locate the editor immediately before reading its value so that
         * the WebElement belongs to the current DOM after any AG Grid
         * re-rendering.
         */
        WebElement currentExtraRateEditor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraRateEditorLocator
                        )
                );

        String editorValue =
                currentExtraRateEditor.getAttribute("value");

        System.out.println(
                "NEW ROW Addition Rate editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!extraRate.equals(editorValue)) {

            throw new IllegalStateException(
                    "NEW ROW Addition Rate editor does not contain " +
                            "the requested value. Requested=[" +
                            extraRate +
                            "], Actual=[" +
                            editorValue +
                            "]"
            );
        }

        System.out.println(
                "NEW ROW Addition Rate editor contains requested value."
        );

        /**
         * TAB commits Addition Rate (%) and allows AG Grid to move
         * keyboard focus naturally to the next cell.
         *
         * ENTER is intentionally not used because it can trigger
         * Add Line behavior in this Purchase Order grid.
         */
        currentExtraRateEditor.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit NEW ROW Addition Rate."
        );

        /**
         * Do not wait for the editor to disappear or attempt to reuse
         * the old WebElement after TAB.
         *
         * AG Grid handles the commit and moves focus to the next cell.
         */
        System.out.println(
                "Addition Rate entered successfully: " +
                        extraRate
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Addition Value in the newly added Purchase Order AG Grid row.
     *
     * The Addition Rate (%) method commits with TAB, which naturally moves
     * AG Grid keyboard focus to the Addition Value cell.
     *
     * No mouse click is performed on the Addition Value cell. The method
     * uses the existing keyboard focus, activates AG Grid inline editing
     * with the first character, assigns the complete requested value through
     * the native HTML input setter, and commits it with TAB.
     *
     * The new row is identified using newItemTestId instead of a hard-coded
     * AG Grid row index, making the method stable after grid re-rendering.
     */
    public void enterExtraValue(String extraValue) {

        System.out.println("========================================");
        System.out.println("Entering Addition Value");
        System.out.println("Requested addition value: " + extraValue);
        System.out.println("New Item test id: " + newItemTestId);

        if (newItemTestId == null || newItemTestId.isBlank()) {

            throw new IllegalStateException(
                    "New Item test id was not captured. " +
                            "Call clickAddLine() before enterExtraValue()."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(
                        driver,
                        Duration.ofSeconds(10)
                );

        /**
         * Locates the Addition Value cell specifically inside the newly
         * added row using the stable Item data-testid as the row anchor.
         */
        By extraValueCellLocator =
                getExtraValueCellLocatorForNewRow();

        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        extraValueCellLocator
                )
        );

        System.out.println(
                "Addition Value cell found in NEW ROW."
        );

        /**
         * Wait until AG Grid moves keyboard focus to the Addition Value cell.
         *
         * The previous Addition Rate operation commits with TAB, so no
         * additional mouse click is required.
         *
         * The cell is re-located on every polling attempt because AG Grid
         * may re-render the row while keyboard focus is moving.
         */
        wait.until(driver -> {

            try {

                WebElement currentExtraValueCell =
                        driver.findElement(
                                extraValueCellLocator
                        );

                String classAttribute =
                        currentExtraValueCell.getAttribute("class");

                return classAttribute != null
                        && classAttribute.contains(
                        "ag-cell-focus"
                );

            } catch (StaleElementReferenceException e) {

                return false;
            }
        });

        System.out.println(
                "Addition Value cell is focused through AG Grid keyboard navigation."
        );

        /**
         * Send only the first character to the focused cell.
         *
         * AG Grid uses the first keyboard character to activate its
         * inline editor. The complete value is assigned after the
         * editor is created.
         */
        String firstCharacter =
                extraValue.substring(0, 1);

        new Actions(driver)
                .sendKeys(firstCharacter)
                .perform();

        System.out.println(
                "First addition-value character sent: " +
                        firstCharacter
        );

        /**
         * Locate the actual AG Grid Addition Value editor created inside
         * the NEW row after the first character activates editing.
         */
        By extraValueEditorLocator =
                getExtraValueEditorLocatorForNewRow();

        WebElement extraValueEditor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraValueEditorLocator
                        )
                );

        System.out.println(
                "NEW ROW Addition Value editor found."
        );

        /**
         * Set the complete Addition Value using the native HTML input
         * setter and notify React/AG Grid through input and change events.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', {
                        bubbles: true
                    })
                );
    
                input.dispatchEvent(
                    new Event('change', {
                        bubbles: true
                    })
                );
                """,
                extraValueEditor,
                extraValue
        );

        System.out.println(
                "Complete Addition Value assigned to NEW ROW: " +
                        extraValue
        );

        /**
         * Verify that the active editor contains the complete requested
         * value before committing it.
         *
         * The editor is re-located on every polling attempt because
         * React/AG Grid may recreate it after the JavaScript update.
         */
        wait.until(driver -> {

            try {

                WebElement currentEditor =
                        driver.findElement(
                                extraValueEditorLocator
                        );

                return extraValue.equals(
                        currentEditor.getAttribute("value")
                );

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {

                return false;
            }
        });

        /**
         * Re-locate the editor immediately before reading its value so that
         * the WebElement belongs to the current DOM after any AG Grid
         * re-rendering.
         */
        WebElement currentExtraValueEditor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraValueEditorLocator
                        )
                );

        String editorValue =
                currentExtraValueEditor.getAttribute("value");

        System.out.println(
                "NEW ROW Addition Value editor BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!extraValue.equals(editorValue)) {

            throw new IllegalStateException(
                    "NEW ROW Addition Value editor does not contain " +
                            "the requested value. Requested=[" +
                            extraValue +
                            "], Actual=[" +
                            editorValue +
                            "]"
            );
        }

        System.out.println(
                "NEW ROW Addition Value editor contains requested value."
        );

        /**
         * TAB commits the Addition Value and allows AG Grid to move
         * keyboard focus naturally to the next cell.
         *
         * ENTER is intentionally not used because it can trigger
         * Add Line behavior in this Purchase Order grid.
         */
        currentExtraValueEditor.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit NEW ROW Addition Value."
        );

        /**
         * Do not wait for the editor to disappear or reuse the old
         * WebElement after TAB.
         *
         * AG Grid handles the commit and moves keyboard focus to the
         * next cell in the grid.
         */
        System.out.println(
                "Addition Value entered successfully: " +
                        extraValue
        );

        System.out.println("========================================");
    }

    /**
     * Selects today's date as the Production Date from the React DatePicker calendar.
     *
     * The Production Date cell is an AG Grid cell that opens a calendar
     * through a double-click instead of using the standard AG Grid inline editor.
     *
     * The method locates the cell by its stable col-id, scrolls it into view,
     * double-clicks it to open the calendar, then selects today's date using
     * the stable aria-label generated by the React DatePicker.
     */
    public void selectTodayAsProductionDate() {

        System.out.println("Selecting today's production date...");

        LocalDate today = LocalDate.now();
        String expectedText = today.toString(); // grid shows yyyy-MM-dd

        System.out.println("Today's production date: " + today);

        /*
         * The cell of the NEW row only. A page-wide [col-id='productionDate']
         * returns the FIRST row's cell, so on orders that already have lines
         * (reference document / duplicate) the date was picked for another
         * row, or not committed at all, while the new row stayed empty.
         */
        By cellLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-index='" +
                        newRowIndex + "'] div[role='gridcell'][col-id='productionDate']"
        );

        String dateAriaLabel = String.format(
                "Choose %s %s %02d of %d",
                today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)),
                today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)),
                today.getDayOfMonth(),
                today.getYear()
        );

        System.out.println("Target date aria-label: " + dateAriaLabel);

        By calendarLocator = By.cssSelector(".rmdp-wrapper");
        By dateOptionLocator = By.cssSelector(
                ".rmdp-wrapper [aria-label='" + dateAriaLabel + "']");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        String committed = "";

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                WebElement cell = wait.until(
                        ExpectedConditions.presenceOfElementLocated(cellLocator));

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                        cell);

                // Re-locate after the scroll (column virtualization).
                cell = wait.until(ExpectedConditions.elementToBeClickable(cellLocator));

                System.out.println("Production Date cell found for row index: "
                        + newRowIndex + " (attempt " + attempt + "/3).");

                // Open the calendar: click + F2 (same as Expire Date), then
                // fall back to a double-click if it did not open.
                cell.click();
                driver.findElement(cellLocator).sendKeys(Keys.F2);

                if (!isCalendarOpen(calendarLocator, Duration.ofSeconds(3))) {
                    new Actions(driver)
                            .doubleClick(driver.findElement(cellLocator))
                            .perform();
                }

                if (!isCalendarOpen(calendarLocator, Duration.ofSeconds(4))) {
                    System.out.println("Production Date calendar did not open.");
                    continue;
                }

                System.out.println("Production Date calendar opened.");

                wait.until(ExpectedConditions.elementToBeClickable(dateOptionLocator)).click();

                System.out.println("Target production date clicked in calendar.");
            } catch (StaleElementReferenceException e) {
                System.out.println("Grid re-rendered during Production Date selection; retrying.");
                continue;
            }

            // Selecting a date normally closes the calendar; make sure it does
            // so the next cell is not blocked by a leftover editor.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(ExpectedConditions.invisibilityOfElementLocated(calendarLocator));
            } catch (TimeoutException e) {
                new Actions(driver).sendKeys(Keys.TAB).perform();
            }

            // Verify the value really reached THIS row's cell.
            try {
                committed = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .until(d -> {
                            try {
                                String text = d.findElement(cellLocator).getText().trim();
                                return text.isEmpty() ? null : text;
                            } catch (StaleElementReferenceException
                                     | NoSuchElementException e) {
                                return null; // grid re-rendered the row; read again
                            }
                        });
            } catch (TimeoutException e) {
                committed = "";
            }

            System.out.println("Production Date cell value after selection: ["
                    + committed + "]");

            if (committed.contains(expectedText)) {
                break;
            }
        }

        if (!committed.contains(expectedText)) {
            throw new IllegalStateException(
                    "Production Date " + expectedText + " was not committed to row-index "
                            + newRowIndex + " after 3 attempts. Cell value: [" + committed + "]");
        }

        System.out.println("Production Date selected successfully: " + committed);
    }

    /** True when the RMDP calendar becomes visible within {@code timeout}. */
    private boolean isCalendarOpen(By calendarLocator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(calendarLocator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }



    /**
     * Selects the Expire Date for the newly created Purchase Order row.
     *
     * Uses the dynamic AG Grid row index together with the stable expireDate
     * column identifier. The method avoids storing the AG Grid row as a
     * WebElement for longer than necessary because React/AG Grid may replace
     * DOM elements during rendering and virtualization.
     *
     * The RMDP calendar is activated through F2 mode, checks whether the
     * target date is already visible, navigates month by month only when
     * necessary, selects the exact visible date using its aria-label,
     * and waits for the calendar to close before validating the committed value.
     */
    public void selectExpireDate() {

        System.out.println("========================================");
        System.out.println("Selecting Expire Date...");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Calculate the expiration date.
         * The Purchase Order expiration date is 30 days after today.
         */
        LocalDate expireDate = LocalDate.now().plusDays(30);

        System.out.println(
                "Target expire date: " +
                        expireDate
        );

        /*
         * Locator for the Expire Date cell of the newly created row.
         * Uses the dynamic AG Grid row index and stable column identifier.
         */
        By expireDateCellLocator =
                By.cssSelector(
                        ".ag-center-cols-container " +
                                "div[role='row'][row-index='" +
                                newRowIndex +
                                "'] " +
                                "div[role='gridcell'][col-id='expireDate']"
                );

        System.out.println(
                "Expire Date locator prepared for row index: " +
                        newRowIndex
        );

        /*
         * Wait until the Expire Date cell exists in the DOM.
         */
        wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        expireDateCellLocator
                )
        );

        System.out.println(
                "Expire Date cell found for row index: " +
                        newRowIndex
        );

        /*
         * Stable CSS selector used by JavaScript to locate
         * the current Expire Date cell directly from the DOM.
         */
        String expireDateSelector =
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='expireDate']";

        /*
         * Scroll the Expire Date cell into view.
         */
        ((JavascriptExecutor) driver).executeScript(
                """
                const selector = arguments[0];
                const element = document.querySelector(selector);
    
                if (element) {
                    element.scrollIntoView({
                        block: 'center',
                        inline: 'center'
                    });
                }
                """,
                expireDateSelector
        );

        System.out.println(
                "Expire Date cell scroll requested successfully."
        );

        /*
         * Re-locate the cell after scrolling to avoid stale elements.
         */
        WebElement expireDateCell =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                expireDateCellLocator
                        )
                );

        System.out.println(
                "Fresh Expire Date cell located after scrolling."
        );

        /*
         * Click the cell to give keyboard focus to AG Grid.
         */
        expireDateCell.click();

        System.out.println(
                "Expire Date cell clicked."
        );

        /*
         * Re-locate the cell after clicking.
         */
        expireDateCell =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                expireDateCellLocator
                        )
                );

        /*
         * F2 activates the AG Grid date editor.
         */
        expireDateCell.sendKeys(Keys.F2);

        System.out.println(
                "F2 sent. Waiting for Expire Date calendar..."
        );

        /*
         * Locator for the RMDP calendar wrapper.
         */
        By calendarWrapperLocator =
                By.cssSelector(".rmdp-wrapper");

        /*
         * Wait until the calendar is visible.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        System.out.println(
                "Expire Date calendar opened."
        );

        /*
         * Build the exact aria-label generated by RMDP.
         *
         * Example:
         * Choose Thursday December 03 of 2026
         */
        String targetDateAriaLabel =
                "Choose " +
                        expireDate.getDayOfWeek()
                                .getDisplayName(
                                        TextStyle.FULL,
                                        Locale.ENGLISH
                                ) +
                        " " +
                        expireDate.getMonth()
                                .getDisplayName(
                                        TextStyle.FULL,
                                        Locale.ENGLISH
                                ) +
                        " " +
                        String.format(
                                "%02d",
                                expireDate.getDayOfMonth()
                        ) +
                        " of " +
                        expireDate.getYear();

        System.out.println(
                "Target date aria-label: " +
                        targetDateAriaLabel
        );

        /*
         * Locator for the exact requested date.
         *
         * Excludes hidden days to prevent selecting a date
         * belonging to an adjacent calendar month.
         */
        By targetDateLocator =
                By.cssSelector(
                        ".rmdp-wrapper " +
                                ".rmdp-day:not(.rmdp-day-hidden)" +
                                "[aria-label='" +
                                targetDateAriaLabel +
                                "']"
                );

        /*
         * Locator for the RMDP Next Month control.
         */
        By nextMonthButtonLocator =
                By.cssSelector(
                        ".rmdp-wrapper " +
                                ".rmdp-arrow-container.rmdp-right"
                );

        /*
         * Maximum number of month-navigation attempts.
         */
        int maxMonthNavigationAttempts = 12;

        /*
         * Check whether the target date is already visible.
         */
        boolean targetDateVisible =
                !driver.findElements(targetDateLocator).isEmpty();

        /*
         * Navigate month by month only when the target date
         * is not currently available.
         */
        for (
                int attempt = 1;
                attempt <= maxMonthNavigationAttempts
                        && !targetDateVisible;
                attempt++
        ) {

            System.out.println(
                    "Target expire date is not visible. " +
                            "Navigating to next month - attempt " +
                            attempt +
                            " of " +
                            maxMonthNavigationAttempts
            );

            /*
             * Re-locate the Next Month control on every attempt.
             */
            WebElement nextMonthButton =
                    wait.until(driver -> {

                        try {

                            List<WebElement> buttons =
                                    driver.findElements(
                                            nextMonthButtonLocator
                                    );

                            for (WebElement button : buttons) {

                                try {

                                    if (button.isDisplayed()
                                            && button.isEnabled()) {

                                        return button;
                                    }

                                } catch (
                                        StaleElementReferenceException ignored
                                ) {
                                    // Retry during the next polling cycle.
                                }
                            }

                            return null;

                        } catch (
                                StaleElementReferenceException |
                                NoSuchElementException e
                        ) {

                            return null;
                        }
                    });

            System.out.println(
                    "Next Month control found."
            );

            /*
             * Move to the next calendar month.
             */
            nextMonthButton.click();

            System.out.println(
                    "Moved to next month."
            );

            /*
             * Wait until the exact visible target date is available.
             */
            targetDateVisible =
                    wait.until(driver -> {

                        try {

                            List<WebElement> dates =
                                    driver.findElements(
                                            targetDateLocator
                                    );

                            for (WebElement date : dates) {

                                try {

                                    if (date.isDisplayed()) {
                                        return true;
                                    }

                                } catch (
                                        StaleElementReferenceException ignored
                                ) {
                                    // Retry after calendar re-rendering.
                                }
                            }

                            return false;

                        } catch (
                                StaleElementReferenceException |
                                NoSuchElementException e
                        ) {

                            return false;
                        }
                    });
        }

        /*
         * Fail explicitly if the target date could not be reached.
         */
        if (!targetDateVisible) {

            throw new IllegalStateException(
                    "Unable to locate Expire Date " +
                            expireDate +
                            " in the RMDP calendar after " +
                            maxMonthNavigationAttempts +
                            " month navigation attempts."
            );
        }

        /*
         * Re-locate the exact visible target date after the
         * final calendar render.
         */
        WebElement targetDate =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                targetDateLocator
                        )
                );

        System.out.println(
                "Target expire date found in calendar."
        );

        /*
         * Select the requested expiration date.
         */
        targetDate.click();

        System.out.println(
                "Target expire date clicked in calendar."
        );

        /*
         * Wait for the calendar to close.
         * If it remains open, use TAB as a fallback to commit
         * the selected date, matching the Production Date workflow.
         */
        try {

            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            calendarWrapperLocator
                    )
            );

        } catch (TimeoutException e) {

            new Actions(driver)
                    .sendKeys(Keys.TAB)
                    .perform();
        }

        System.out.println(
                "Expire Date calendar closed or TAB fallback executed."
        );

        /*
         * Wait until the selected Expire Date is committed
         * to the correct AG Grid row.
         */
        String committedExpireDate =
                new WebDriverWait(driver, Duration.ofSeconds(10))
                        .pollingEvery(Duration.ofMillis(250))
                        .ignoring(StaleElementReferenceException.class)
                        .ignoring(NoSuchElementException.class)
                        .until(d -> {

                            String currentValue =
                                    d.findElement(expireDateCellLocator)
                                            .getText()
                                            .trim();

                            return currentValue.equals(
                                    expireDate.toString()
                            )
                                    ? currentValue
                                    : null;
                        });

        System.out.println(
                "Expire Date cell value after selection: [" +
                        committedExpireDate +
                        "]"
        );

        /*
         * Verify that the selected expiration date was actually
         * committed to the AG Grid cell.
         */
        if (!committedExpireDate.equals(
                expireDate.toString()
        )) {

            throw new IllegalStateException(
                    "Expire Date mismatch. Expected: " +
                            expireDate +
                            " | Actual: " +
                            committedExpireDate
            );
        }

        System.out.println(
                "Expire Date entered successfully: " +
                        committedExpireDate
        );

        System.out.println(
                "========================================"
        );
    }

    /**
     * Enters the Free Quantity value into the Free Quantity cell of the
     * newly created Purchase Order row.
     * The cell is activated using a double-click, then the AG Grid text
     * editor is populated through JavaScript events and committed using TAB.
     */
    public void enterFreeQuantity(int freeQuantity) {

        System.out.println("========================================");
        System.out.println("Entering free quantity");
        System.out.println("Requested free quantity: " + freeQuantity);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locator for the newly created AG Grid row.
         * The stored row index allows the row to be re-located after
         * React/AG Grid re-renders the DOM.
         */
        By currentRowLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newRowIndex +
                        "']"
        );

        /**
         * Locator for the Free Quantity cell.
         */
        By freeQuantityCellLocator = By.cssSelector(
                "[role='gridcell'][col-id='freeQuantities']"
        );

        /**
         * Editor input inside the Free Quantity cell of THIS row only
         * (AG Grid may keep other rows' cells with the same col-id).
         */
        By freeQuantityInputLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-index='" +
                        newRowIndex + "'] " +
                        "[role='gridcell'][col-id='freeQuantities'] input"
        );

        /**
         * Open the editor. The previous step (date pickers) can leave another
         * cell in edit mode; the first click then only closes that editor and
         * the double-click does not open Free Quantity. So: scroll, re-locate,
         * double-click, and retry (then ENTER / F2) until the editor appears.
         */
        WebElement currentRow = null;
        WebElement freeQuantityCell = null;
        WebElement freeQuantityInput = null;

        for (int attempt = 1; attempt <= 3 && freeQuantityInput == null; attempt++) {

            currentRow = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            currentRowLocator
                    )
            );

            freeQuantityCell = currentRow.findElement(freeQuantityCellLocator);

            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    freeQuantityCell
            );

            // Re-locate after the scroll (column virtualization re-renders cells).
            currentRow = driver.findElement(currentRowLocator);
            freeQuantityCell = currentRow.findElement(freeQuantityCellLocator);

            if (attempt == 1) {
                System.out.println("Free Quantity cell found.");
                System.out.println(
                        "Free Quantity cell text BEFORE editing: [" +
                                freeQuantityCell.getText() + "]"
                );
            }

            Actions actions = new Actions(driver);

            if (attempt == 1) {
                actions.doubleClick(freeQuantityCell).perform();
                System.out.println("Free Quantity cell double-clicked.");
            } else if (attempt == 2) {
                actions.click(freeQuantityCell).pause(Duration.ofMillis(300))
                        .doubleClick(freeQuantityCell).perform();
                System.out.println("Free Quantity cell clicked + double-clicked (attempt 2).");
            } else {
                actions.click(freeQuantityCell).pause(Duration.ofMillis(300))
                        .sendKeys(Keys.ENTER).perform();
                System.out.println("ENTER sent to Free Quantity cell (attempt 3).");
            }

            try {
                freeQuantityInput = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .pollingEvery(Duration.ofMillis(250))
                        .until(ExpectedConditions.visibilityOfElementLocated(
                                freeQuantityInputLocator));
            } catch (TimeoutException e) {
                System.out.println(
                        "Free Quantity editor did not open (attempt " + attempt + "/3)."
                );
            }
        }

        if (freeQuantityInput == null) {

            String cellClass = freeQuantityCell == null
                    ? "?" : String.valueOf(freeQuantityCell.getAttribute("class"));

            String focused = String.valueOf(((JavascriptExecutor) driver).executeScript(
                    "const a = document.activeElement;" +
                            "if (!a) return 'none';" +
                            "const c = a.closest('[col-id]');" +
                            "return a.tagName + ' col-id=' + (c ? c.getAttribute('col-id') : '-');"
            ));

            throw new TimeoutException(
                    "Free Quantity editor did not open on row-index " + newRowIndex +
                            " after 3 attempts. Cell class: [" + cellClass +
                            "], focused element: [" + focused + "]."
            );
        }

        System.out.println(
                "Free Quantity editor found."
        );

        System.out.println(
                "Free Quantity input type: [" +
                        freeQuantityInput.getAttribute("type") +
                        "]"
        );

        System.out.println(
                "Free Quantity input value BEFORE typing: [" +
                        freeQuantityInput.getAttribute("value") +
                        "]"
        );

        /**
         * Focus the AG Grid editor input.
         */
        freeQuantityInput.click();

        /**
         * Clear any existing value from the editor.
         */
        freeQuantityInput.sendKeys(
                Keys.CONTROL,
                "a"
        );

        freeQuantityInput.sendKeys(
                Keys.BACK_SPACE
        );

        /**
         * Set the requested value using JavaScript and dispatch the
         * input/change events so the React/AG Grid editor can detect
         * the changed value.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                freeQuantityInput,
                String.valueOf(freeQuantity)
        );

        /**
         * Read the editor value after JavaScript assignment.
         */
        String editorValue =
                freeQuantityInput.getAttribute("value");

        System.out.println(
                "Free Quantity input value AFTER JavaScript assignment: [" +
                        editorValue +
                        "]"
        );

        /**
         * Verify that the editor contains the requested value
         * before attempting to commit it.
         */
        if (!String.valueOf(freeQuantity).equals(editorValue)) {

            throw new IllegalStateException(
                    "Free Quantity editor did not accept the requested value. " +
                            "Expected: [" + freeQuantity +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Commit the Free Quantity value.
         */
        freeQuantityInput.sendKeys(
                Keys.TAB
        );

        System.out.println(
                "TAB sent to commit Free Quantity."
        );

        /**
         * Wait until AG Grid exits inline editing mode.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        freeQuantityInputLocator
                )
        );

        /**
         * Re-locate the row after AG Grid re-renders the DOM.
         */
        currentRow = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currentRowLocator
                )
        );

        /**
         * Re-locate the Free Quantity cell after the re-render.
         */
        freeQuantityCell =
                currentRow.findElement(
                        freeQuantityCellLocator
                );

        String committedValue =
                freeQuantityCell.getText().trim();

        System.out.println(
                "Free Quantity cell value after commit: [" +
                        committedValue +
                        "]"
        );

        /**
         * Verify that the Free Quantity was successfully committed.
         */
        if (committedValue.isEmpty()) {

            throw new IllegalStateException(
                    "Free Quantity was not committed to the AG Grid cell."
            );
        }

        System.out.println(
                "Free Quantity entered successfully: " +
                        committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Note (البيان) value into the Note cell of the newly created
     * Purchase Order row and commits the edit by clicking the "Discounts and
     * Additions" section outside the AG Grid.
     *
     * JavaScript is used to assign the Note value directly to the native input
     * so Arabic text and spaces are preserved correctly. The external section
     * is clicked to move focus away from the AG Grid editor without using TAB
     * or ENTER, preventing AG Grid from creating a new row.
     */
    public void enterNote(String note) {

        System.out.println("========================================");
        System.out.println("Entering Note (البيان)");
        System.out.println("Requested note: [" + note + "]");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Note cell in the newly created Purchase Order row.
         */
        WebElement noteCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getNoteCellLocator()
                )
        );

        System.out.println(
                "Note cell found. Current value: [" +
                        noteCell.getText() +
                        "]"
        );

        /**
         * Double-click the Note cell to activate AG Grid inline editing.
         */
        Actions actions = new Actions(driver);

        actions.doubleClick(noteCell).perform();

        System.out.println(
                "Note cell double-clicked."
        );

        /**
         * Locator for the AG Grid text editor inside the Note cell.
         */
        By noteInputLocator = By.cssSelector(
                "[role='gridcell'][col-id='note'] " +
                        "input.ag-text-field-input"
        );

        /**
         * Wait for the Note editor to appear.
         */
        WebElement noteInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println(
                "Note editor found."
        );

        /**
         * Assign the Note value through the native HTML input setter.
         *
         * JavaScript is used because sendKeys() previously removed the spaces
         * between Arabic words in this AG Grid editor.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                noteInput,
                note
        );

        /**
         * Verify that the requested Note value was assigned to the editor
         * before moving focus away from the AG Grid cell.
         */
        String editorValue =
                noteInput.getAttribute("value");

        System.out.println(
                "Note editor value AFTER JavaScript assignment: [" +
                        editorValue +
                        "]"
        );

        if (!note.equals(editorValue)) {

            throw new IllegalStateException(
                    "Note editor did not accept the requested value. " +
                            "Expected: [" + note +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Locator for the "Discounts and Additions" section outside the AG Grid.
         *
         * This element is intentionally used to move focus away from the Note
         * editor because TAB and ENTER cause AG Grid to create a new row.
         */
        By discountsAndAdditionsLocator = By.xpath(
                "//div[contains(@class,'flex') " +
                        "and contains(@class,'item-center') " +
                        "and contains(@class,'gap-2') " +
                        "and normalize-space()='الخصومات والإضافات']"
        );

        /**
         * Wait until the "Discounts and Additions" section is clickable.
         */
        WebElement discountsAndAdditions = wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAndAdditionsLocator
                )
        );

        /**
         * Click the external "Discounts and Additions" section to move focus
         * away from the AG Grid Note editor and commit the current value.
         *
         * TAB and ENTER are deliberately not used because they previously
         * caused AG Grid to create an additional row.
         */
        discountsAndAdditions.click();

        System.out.println(
                "Clicked 'الخصومات والإضافات' to commit Note."
        );

        /**
         * Wait until the Note editor disappears.
         *
         * The disappearance of the editor confirms that AG Grid has exited
         * inline-editing mode. The Note cell is intentionally NOT re-located
         * after this point because AG Grid may horizontally virtualize the
         * Note column and remove it from the DOM after clicking the section.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println(
                "Note editor closed successfully."
        );

        /**
         * The Note value was already verified inside the editor before the
         * commit. Since the editor has now disappeared, the value has been
         * committed and the method can safely continue with the remaining
         * Purchase Order fields.
         */
        System.out.println(
                "Note committed successfully: [" +
                        note +
                        "]"
        );

        System.out.println("========================================");
    }

    /**
     * Captures the row index of the newly created row in the
     * Discounts & Additions AG Grid after clicking Add Line.
     * The new row is identified as the last visible row in the
     * center AG Grid container. This method is completely independent
     * from the Purchase Order items grid row tracking.
     */
    public void captureNewDiscountAdditionRowIndex() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        By centerRowsLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row']"
        );

        wait.until(driver -> {
            List<WebElement> rows =
                    driver.findElements(centerRowsLocator);

            return !rows.isEmpty();
        });

        List<WebElement> rows =
                driver.findElements(centerRowsLocator);

        WebElement lastRow = rows.get(rows.size() - 1);

        String rowIndex =
                lastRow.getAttribute("row-index");

        if (rowIndex == null || rowIndex.isBlank()) {
            throw new IllegalStateException(
                    "Could not determine row-index of the new Discounts & Additions row."
            );
        }

        newDiscountAdditionRowIndex =
                Integer.parseInt(rowIndex);

        System.out.println(
                "Discounts & Additions new row index: "
                        + newDiscountAdditionRowIndex
        );
    }

    /**
     * Selects a random Account (الحساب) from the Account cell in the newly
     * created Discounts & Additions row.
     *
     * IMPORTANT:
     * The Discounts & Additions grid has its own row structure and its own
     * row index. Therefore, this method uses newDiscountAdditionRowIndex
     * instead of the Purchase Order items grid newRowIndex.
     *
     * ENTER opens the Account React Select editor, then a random visible
     * selectable option is selected and committed.
     */
    public void selectRandomAccount() {

        System.out.println("========================================");
        System.out.println("Selecting random Account");
        System.out.println(
                "Discounts & Additions new row index: " +
                        newDiscountAdditionRowIndex
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Account cell inside the newly created
         * Discounts & Additions row.
         */
        WebElement accountCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAccountCellLocator()
                )
        );

        System.out.println(
                "Account cell found in NEW Discounts & Additions row."
        );

        System.out.println(
                "Account cell current value: [" +
                        accountCell.getText() +
                        "]"
        );

        /**
         * Scroll the Account cell into the visible viewport.
         *
         * This is important because the Discounts & Additions grid
         * can horizontally virtualize its columns.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                accountCell
        );

        /**
         * Re-locate the Account cell after scrolling because AG Grid
         * may re-render the DOM during horizontal virtualization.
         */
        accountCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAccountCellLocator()
                )
        );

        /*
         * Open the Account editor. A click + ENTER does not always open it
         * (QC, 2026-10-27, edit of a draft order: ENTER went to a cell that
         * had just been re-rendered, so no editor appeared). Retry with a
         * fresh cell each time: click + ENTER, then double-click, then F2.
         * The editor input is looked up inside THIS row only.
         */
        By accountInputLocator = By.cssSelector(
                "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] "
                        + "[role='gridcell'][col-id='accountId'] input"
        );

        WebElement accountInput = null;

        for (int attempt = 1; attempt <= 3 && accountInput == null; attempt++) {

            try {
                accountCell = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                getDiscountAdditionAccountCellLocator()));

                if (attempt == 1) {
                    accountCell.click();
                    driver.findElement(getDiscountAdditionAccountCellLocator())
                            .sendKeys(Keys.ENTER);
                    System.out.println("Account cell clicked + ENTER.");
                } else if (attempt == 2) {
                    new Actions(driver).doubleClick(accountCell).perform();
                    System.out.println("Account cell double-clicked (attempt 2).");
                } else {
                    accountCell.click();
                    driver.findElement(getDiscountAdditionAccountCellLocator())
                            .sendKeys(Keys.F2);
                    System.out.println("Account cell clicked + F2 (attempt 3).");
                }
            } catch (StaleElementReferenceException e) {
                System.out.println("Account cell re-rendered; retrying.");
                continue;
            }

            try {
                accountInput = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .pollingEvery(Duration.ofMillis(250))
                        .until(ExpectedConditions.visibilityOfElementLocated(
                                accountInputLocator));
            } catch (TimeoutException e) {
                System.out.println("Account editor did not open (attempt " + attempt + "/3).");
            }
        }

        if (accountInput == null) {
            throw new TimeoutException(
                    "Account editor did not open on Discounts & Additions row-index "
                            + newDiscountAdditionRowIndex + " after 3 attempts.");
        }

        System.out.println(
                "Account React Select editor found."
        );

        /**
         * Open the Account dropdown.
         */
        accountInput.click();

        /**
         * React Select renders the opened options inside a listbox.
         */
        By accountListboxLocator = By.cssSelector(
                "div[role='listbox']"
        );

        WebElement accountListbox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountListboxLocator
                )
        );

        System.out.println(
                "Account dropdown opened."
        );

        /**
         * Locate all options currently rendered by React Select.
         */
        List<WebElement> accountOptions =
                accountListbox.findElements(
                        By.cssSelector("[role='option']")
                );

        /**
         * Keep only displayed and enabled options.
         */
        List<WebElement> selectableOptions =
                accountOptions.stream()
                        .filter(WebElement::isDisplayed)
                        .filter(option ->
                                !"true".equals(
                                        option.getAttribute("aria-disabled")
                                )
                        )
                        .toList();

        System.out.println(
                "Number of visible Account options: " +
                        selectableOptions.size()
        );

        if (selectableOptions.isEmpty()) {

            throw new IllegalStateException(
                    "No selectable Account options were found."
            );
        }

        /**
         * Select a random Account from the available options.
         */
        Random random = new Random();

        WebElement randomAccount =
                selectableOptions.get(
                        random.nextInt(selectableOptions.size())
                );

        String selectedAccount =
                randomAccount.getText().trim();

        System.out.println(
                "Random Account selected: [" +
                        selectedAccount +
                        "]"
        );

        /**
         * Click the selected Account option.
         */
        randomAccount.click();

        /**
         * Wait until the React Select editor disappears.
         * Its disappearance confirms that the selection was committed.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        accountInputLocator
                )
        );

        System.out.println(
                "Account selected successfully: [" +
                        selectedAccount +
                        "]"
        );

        System.out.println("========================================");
    }


    /**
     * Opens the editor of an AG Grid cell and returns what {@code opened}
     * yields once the editor is there (an input, a list of options …).
     *
     * <p>One gesture is not always enough: the grid may re-render the cell
     * right after the previous edit, or a leftover editor swallows the first
     * click. Up to 3 attempts, each on a freshly located cell:
     * double-click → click + ENTER → click + F2.</p>
     *
     * @throws TimeoutException if the editor never opens.
     */
    private <T> T openCellEditorWithRetry(String cellName, By cellLocator,
                                          java.util.function.Function<WebDriver, T> opened) {

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                WebElement cell = new WebDriverWait(driver, Duration.ofSeconds(10))
                        .until(ExpectedConditions.elementToBeClickable(cellLocator));

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center', inline:'center'});", cell);
                cell = driver.findElement(cellLocator);   // fresh after scroll

                switch (attempt) {
                    case 1 -> new Actions(driver).doubleClick(cell).perform();
                    case 2 -> {
                        cell.click();
                        driver.findElement(cellLocator).sendKeys(Keys.ENTER);
                    }
                    default -> {
                        cell.click();
                        driver.findElement(cellLocator).sendKeys(Keys.F2);
                    }
                }
                System.out.println(cellName + " editor: gesture " + attempt + "/3 sent.");

            } catch (StaleElementReferenceException e) {
                System.out.println(cellName + " cell re-rendered; retrying.");
                continue;
            }

            try {
                return new WebDriverWait(driver, Duration.ofSeconds(4))
                        .pollingEvery(Duration.ofMillis(250))
                        .ignoring(StaleElementReferenceException.class)
                        .until(opened);
            } catch (TimeoutException e) {
                System.out.println(cellName + " editor did not open (attempt " + attempt + "/3).");
            }
        }

        throw new TimeoutException(cellName + " editor did not open after 3 attempts ("
                + cellLocator + ").");
    }

    /**
     * Selects a random value from the "Influencing on Cost"
     * (التأثير على التكلفة) cell in the newly created
     * Discounts & Additions row.
     *
     * The cell uses a custom AG Grid dropdown editor.
     * The method activates the cell, waits for visible selectable
     * options, selects one random option, and verifies that the
     * selected value was committed successfully.
     */
    public void selectRandomInfluencingOnCost() {

        System.out.println("========================================");
        System.out.println("Selecting random Influencing on Cost");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Influencing on Cost cell in the newly created row.
         */
        WebElement influencingOnCostCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getInfluencingOnCostCellLocator2()
                )
        );

        System.out.println(
                "Influencing on Cost cell found. Current value: [" +
                        influencingOnCostCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the target cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                influencingOnCostCell
        );

        /**
         * Re-locate the cell after scrolling to avoid using
         * a potentially stale AG Grid DOM reference.
         */
        influencingOnCostCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getInfluencingOnCostCellLocator2()
                )
        );

        /**
         * Locate selectable dropdown options.
         */
        By optionLocator = By.cssSelector(
                "[role='option']:not([aria-disabled='true'])"
        );

        /*
         * Activate the custom dropdown editor, retrying with other gestures
         * if it does not open (QC, 2026-10-27: a single double-click right
         * after the Account selection did not open the options).
         */
        List<WebElement> visibleOptions = openCellEditorWithRetry(
                "Influencing on Cost",
                getInfluencingOnCostCellLocator2(),
                d -> {
                    List<WebElement> shown = d.findElements(optionLocator).stream()
                            .filter(WebElement::isDisplayed)
                            .toList();
                    return shown.isEmpty() ? null : shown;
                });

        System.out.println(
                "Influencing on Cost dropdown opened."
        );

        System.out.println(
                "Number of visible Influencing on Cost options: " +
                        visibleOptions.size()
        );

        if (visibleOptions.isEmpty()) {
            throw new IllegalStateException(
                    "No selectable Influencing on Cost options were found."
            );
        }

        /**
         * Select a random option from the visible dropdown options.
         */
        Random random = new Random();

        WebElement randomOption =
                visibleOptions.get(
                        random.nextInt(visibleOptions.size())
                );

        String selectedValue =
                randomOption.getText().trim();

        System.out.println(
                "Random Influencing on Cost selected: [" +
                        selectedValue +
                        "]"
        );

        /**
         * Click the randomly selected option.
         */
        randomOption.click();

        /**
         * Wait until the dropdown closes, confirming that
         * the selected value has been committed.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        By.cssSelector("[role='option']")
                )
        );

        /**
         * Re-locate the cell and verify the committed value.
         */
        WebElement updatedCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getInfluencingOnCostCellLocator2()
                )
        );

        String actualValue =
                updatedCell.getText().trim();

        if (!actualValue.equals(selectedValue)) {

            throw new AssertionError(
                    "Influencing on Cost value was not committed correctly. " +
                            "Expected: [" + selectedValue + "] " +
                            "Actual: [" + actualValue + "]"
            );
        }

        System.out.println(
                "Influencing on Cost selected successfully: [" +
                        actualValue +
                        "]"
        );

        System.out.println("========================================");
    }


    /**
     * Enters the Discount Rate (نسبة الخصم) value into the
     * Discount Rate cell of the newly created row in the
     * Discounts & Additions AG Grid.
     *
     * The cell is activated using the existing AG Grid editor
     * activation mechanism. JavaScript assigns the requested value
     * directly to the native input, then TAB commits the value
     * without creating a new AG Grid row.
     */
    public void enterDiscountRateInDiscountsSection(String discountRate) {

        /**
         * Locator for the native AG Grid editor generated for
         * the Discount Rate cell in the Discounts & Additions grid.
         */
        By discountRateInputLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='discountRate'] " +
                        "input.ag-input-field-input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Rate in Discounts & Additions");
        System.out.println("Requested discount rate: " + discountRate);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Discount Rate cell in the newly created
         * Discounts & Additions row.
         */
        WebElement discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountRateCellLocator()
                )
        );

        System.out.println(
                "Discount Rate cell found. Current value: [" +
                        discountRateCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the Discount Rate cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                discountRateCell
        );

        /**
         * Re-locate the cell after scrolling because AG Grid may
         * re-render the cell during horizontal/vertical virtualization.
         */
        discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountRateCellLocator()
                )
        );

        /**
         * Activate the AG Grid inline editor.
         *
         * The existing framework helper is used so the same editor
         * activation logic remains centralized and consistent.
         */
        if (!tryActivateGridCellEditor(
                getDiscountAdditionDiscountRateCellLocator(),
                discountRateInputLocator
        )) {

            System.out.println(
                    "Discount Rate is disabled/non-editable. Skipping."
            );

            return;
        }

        System.out.println(
                "Discount Rate cell activated successfully."
        );

        /**
         * Locate the native input generated by AG Grid.
         */
        WebElement discountRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        System.out.println(
                "Discount Rate editor found."
        );

        /**
         * Set the requested Discount Rate directly through the
         * native HTML input value setter.
         *
         * This avoids keyboard-entry issues while still dispatching
         * the input/change events required by the application.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                discountRateInput,
                discountRate
        );

        /**
         * Verify the requested value inside the editor before
         * committing it.
         */
        String editorValue =
                discountRateInput.getAttribute("value");

        System.out.println(
                "Discount Rate editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!discountRate.equals(editorValue)) {

            throw new IllegalStateException(
                    "Discount Rate editor did not accept the requested value. " +
                            "Expected: [" + discountRate +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Commit the value using TAB.
         *
         * TAB is intentionally used instead of ENTER because
         * ENTER may trigger AG Grid row-related behavior.
         */
        discountRateInput.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit Discount Rate."
        );

        /**
         * Wait until the inline editor disappears after committing.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        /**
         * Re-locate the Discount Rate cell after AG Grid
         * re-renders the committed value.
         */
        discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountRateCellLocator()
                )
        );

        String committedValue =
                discountRateCell.getText().trim();

        System.out.println(
                "Discount Rate cell value after commit: [" +
                        committedValue +
                        "]"
        );

        /**
         * Normalize the expected and actual values because AG Grid
         * may display the value with decimal places, for example 20.00.
         */
        String normalizedExpected =
                new java.math.BigDecimal(discountRate)
                        .stripTrailingZeros()
                        .toPlainString();

        String normalizedActual =
                new java.math.BigDecimal(
                        committedValue.replace("%", "").trim()
                )
                        .stripTrailingZeros()
                        .toPlainString();

        /**
         * Verify that the committed value matches the requested value.
         */
        if (!normalizedExpected.equals(normalizedActual)) {

            throw new IllegalStateException(
                    "Discount Rate was not committed correctly. " +
                            "Expected: [" + normalizedExpected +
                            "], Actual: [" + normalizedActual + "]"
            );
        }

        System.out.println(
                "Discount Rate entered successfully: " +
                        committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Discount Value (قيمة الخصم) into the Discount Value
     * cell of the newly created row in the Discounts & Additions AG Grid.
     *
     * The method activates the AG Grid inline editor through keyboard
     * navigation, assigns the complete numeric value using JavaScript,
     * and commits the value using TAB without creating a new row.
     */
    public void enterDiscountValue(double discountValue) {

        System.out.println("========================================");
        System.out.println("Entering Discount Value in Discounts & Additions");
        System.out.println("Requested discount value: " + discountValue);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Discount Value cell in the newly created
         * Discounts & Additions row.
         */
        WebElement discountValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountValueCellLocator()
                )
        );

        System.out.println(
                "Discount Value cell found. Current value: [" +
                        discountValueCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the Discount Value cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                discountValueCell
        );

        /**
         * Re-locate the cell after scrolling because AG Grid may
         * re-render the cell during virtualization.
         */
        discountValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountValueCellLocator()
                )
        );

        /**
         * Give the Discount Value cell keyboard focus.
         */
        discountValueCell.click();

        System.out.println(
                "Discount Value cell is focused."
        );

        /**
         * Convert the requested numeric value to a clean string
         * without unnecessary trailing zeros.
         */
        String value =
                BigDecimal.valueOf(discountValue)
                        .stripTrailingZeros()
                        .toPlainString();

        /**
         * Send the first character through the AG Grid cell.
         *
         * This activates the inline editor using the same mechanism
         * that was successful with the existing Discount Value cell.
         */
        discountValueCell.sendKeys(value.substring(0, 1));

        System.out.println(
                "First discount value character sent: " +
                        value.charAt(0)
        );

        /**
         * Locator for the native input generated by AG Grid
         * while editing the Discount Value cell.
         */
        By discountValueEditorLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='discountValue'] " +
                        "input"
        );

        /**
         * Verify that the Discount Value editor can be activated.
         *
         * If the cell is disabled/non-editable, skip it without
         * failing the complete test.
         */
        if (!tryActivateGridCellEditor(
                getDiscountAdditionDiscountValueCellLocator(),
                discountValueEditorLocator
        )) {

            System.out.println(
                    "Discount Value is disabled/non-editable. Skipping."
            );

            return;
        }

        /**
         * Locate the active native input editor.
         */
        WebElement discountValueEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountValueEditorLocator
                )
        );

        System.out.println(
                "Discount Value editor found."
        );

        /**
         * Set the complete requested value through the native
         * HTML input value setter and dispatch the required events.
         */
        ((JavascriptExecutor) driver).executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                discountValueEditor,
                value
        );

        System.out.println(
                "Complete Discount Value set in editor: " +
                        value
        );

        /**
         * Verify the value inside the editor before committing.
         */
        String editorValue =
                discountValueEditor.getAttribute("value");

        System.out.println(
                "Discount Value editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!value.equals(editorValue)) {

            throw new IllegalStateException(
                    "Discount Value editor did not accept the requested value. " +
                            "Expected: [" + value +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Commit the Discount Value using TAB.
         *
         * TAB is preferred over ENTER because ENTER may trigger
         * AG Grid row-related behavior.
         */
        discountValueEditor.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit Discount Value."
        );

        /**
         * Wait until the inline editor disappears.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        discountValueEditorLocator
                )
        );

        /**
         * Re-locate the cell after AG Grid commits and re-renders it.
         */
        WebElement updatedDiscountValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionDiscountValueCellLocator()
                )
        );

        String actualValue =
                updatedDiscountValueCell.getText().trim();

        System.out.println(
                "Discount Value cell value after commit: [" +
                        actualValue +
                        "]"
        );

        /**
         * Normalize the displayed value because AG Grid may display
         * decimal places such as 20.00.
         */
        String normalizedExpected =
                new BigDecimal(value)
                        .stripTrailingZeros()
                        .toPlainString();

        String normalizedActual =
                new BigDecimal(
                        actualValue.replace(",", "").trim()
                )
                        .stripTrailingZeros()
                        .toPlainString();

        /**
         * Verify that the committed value matches the requested value.
         */
        if (!normalizedExpected.equals(normalizedActual)) {

            throw new IllegalStateException(
                    "Discount Value was not committed correctly. " +
                            "Expected: [" + normalizedExpected +
                            "], Actual: [" + normalizedActual + "]"
            );
        }

        System.out.println(
                "Discount Value entered successfully: " +
                        actualValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Addition Rate value (نسبة الإضافي) into the
     * Addition Rate cell of the newly created row in the
     * Discounts & Additions AG Grid.
     *
     * The method activates the AG Grid inline editor, assigns the
     * requested value directly to the native input using JavaScript,
     * and commits the value using TAB without creating a new row.
     */
    public void enterAdditionRateInDiscountsSection(String additionRate) {

        System.out.println("========================================");
        System.out.println("Entering Addition Rate in Discounts & Additions");
        System.out.println("Requested addition rate: " + additionRate);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Addition Rate cell in the newly created
         * Discounts & Additions row.
         */
        WebElement additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionRateCellLocator()
                )
        );

        System.out.println(
                "Addition Rate cell found. Current value: [" +
                        additionRateCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the Addition Rate cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                additionRateCell
        );

        /**
         * Re-locate the cell after scrolling because AG Grid may
         * re-render the cell during virtualization.
         */
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionRateCellLocator()
                )
        );

        /**
         * Locator for the native input generated by AG Grid
         * while editing the Addition Rate cell.
         */
        By additionRateInputLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='additionRate'] " +
                        "input.ag-input-field-input"
        );

        /**
         * Activate the Addition Rate editor using the existing
         * framework helper.
         *
         * If the cell is disabled/non-editable, skip it without
         * failing the complete test.
         */
        if (!tryActivateGridCellEditor(
                getDiscountAdditionAdditionRateCellLocator(),
                additionRateInputLocator
        )) {

            System.out.println(
                    "Addition Rate is disabled/non-editable. Skipping."
            );

            return;
        }

        System.out.println(
                "Addition Rate cell activated successfully."
        );

        /**
         * Locate the native AG Grid input editor.
         */
        WebElement additionRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        System.out.println(
                "Addition Rate editor found."
        );

        /**
         * Set the requested Addition Rate directly through the
         * native HTML input value setter.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                additionRateInput,
                additionRate
        );

        /**
         * Verify the requested value inside the editor
         * before committing it.
         */
        String editorValue =
                additionRateInput.getAttribute("value");

        System.out.println(
                "Addition Rate editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!additionRate.equals(editorValue)) {

            throw new IllegalStateException(
                    "Addition Rate editor did not accept the requested value. " +
                            "Expected: [" + additionRate +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Commit the Addition Rate using TAB.
         *
         * TAB is used instead of ENTER because ENTER may trigger
         * AG Grid row-related behavior.
         */
        additionRateInput.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit Addition Rate."
        );

        /**
         * Wait until the inline editor disappears after committing.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        /**
         * Re-locate the Addition Rate cell after AG Grid
         * re-renders the committed value.
         */
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionRateCellLocator()
                )
        );

        String committedValue =
                additionRateCell.getText().trim();

        System.out.println(
                "Addition Rate cell value after commit: [" +
                        committedValue +
                        "]"
        );

        /**
         * Normalize the expected and actual values because AG Grid
         * may display the Addition Rate using decimal places,
         * for example 20.00.
         */
        String normalizedExpected =
                new java.math.BigDecimal(additionRate)
                        .stripTrailingZeros()
                        .toPlainString();

        String normalizedActual =
                new java.math.BigDecimal(
                        committedValue.replace("%", "").trim()
                )
                        .stripTrailingZeros()
                        .toPlainString();

        /**
         * Verify that the committed value matches the requested value.
         */
        if (!normalizedExpected.equals(normalizedActual)) {

            throw new IllegalStateException(
                    "Addition Rate was not committed correctly. " +
                            "Expected: [" + normalizedExpected +
                            "], Actual: [" + normalizedActual + "]"
            );
        }

        System.out.println(
                "Addition Rate entered successfully: " +
                        committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Addition Value (قيمة الإضافي) into the
     * Addition Value cell of the newly created row in the
     * Discounts & Additions AG Grid.
     *
     * The method activates the AG Grid inline editor, assigns the
     * requested value directly to the native input using JavaScript,
     * and commits the value using TAB without creating a new row.
     */
    public void enterAdditionValueInDiscountsSection(String additionValue) {

        System.out.println("========================================");
        System.out.println("Entering Addition Value in Discounts & Additions");
        System.out.println("Requested addition value: " + additionValue);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Addition Value cell in the newly created
         * Discounts & Additions row.
         */
        WebElement additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionValueCellLocator()
                )
        );

        System.out.println(
                "Addition Value cell found. Current value: [" +
                        additionValueCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the Addition Value cell into the visible viewport.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                additionValueCell
        );

        /**
         * Re-locate the cell after scrolling because AG Grid may
         * re-render the cell during virtualization.
         */
        additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionValueCellLocator()
                )
        );

        /**
         * Locator for the native input generated by AG Grid
         * while editing the Addition Value cell.
         */
        By additionValueInputLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='additionValue'] " +
                        "input.ag-input-field-input"
        );

        /**
         * Activate the Addition Value editor using the existing
         * framework helper.
         *
         * If the cell is disabled/non-editable, skip it without
         * failing the complete test.
         */
        if (!tryActivateGridCellEditor(
                getDiscountAdditionAdditionValueCellLocator(),
                additionValueInputLocator
        )) {

            System.out.println(
                    "Addition Value is disabled/non-editable. Skipping."
            );

            return;
        }

        System.out.println(
                "Addition Value cell activated successfully."
        );

        /**
         * Locate the native AG Grid input editor.
         */
        WebElement additionValueInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueInputLocator
                )
        );

        System.out.println(
                "Addition Value editor found."
        );

        /**
         * Set the requested Addition Value directly through the
         * native HTML input value setter.
         */
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter =
                    Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype,
                        'value'
                    ).set;
    
                setter.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                additionValueInput,
                additionValue
        );

        /**
         * Verify the requested value inside the editor
         * before committing it.
         */
        String editorValue =
                additionValueInput.getAttribute("value");

        System.out.println(
                "Addition Value editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (!additionValue.equals(editorValue)) {

            throw new IllegalStateException(
                    "Addition Value editor did not accept the requested value. " +
                            "Expected: [" + additionValue +
                            "], Actual: [" + editorValue + "]"
            );
        }

        /**
         * Commit the Addition Value using TAB.
         *
         * TAB is used instead of ENTER because ENTER may trigger
         * AG Grid row-related behavior.
         */
        additionValueInput.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit Addition Value."
        );

        /**
         * Wait until the inline editor disappears after committing.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        additionValueInputLocator
                )
        );

        /**
         * Re-locate the Addition Value cell after AG Grid
         * re-renders the committed value.
         */
        additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionAdditionValueCellLocator()
                )
        );

        String committedValue =
                additionValueCell.getText().trim();

        System.out.println(
                "Addition Value cell value after commit: [" +
                        committedValue +
                        "]"
        );

        /**
         * Normalize the expected and actual values because AG Grid
         * may display the Addition Value using decimal places,
         * for example 20.00.
         */
        String normalizedExpected =
                new java.math.BigDecimal(additionValue)
                        .stripTrailingZeros()
                        .toPlainString();

        String normalizedActual =
                new java.math.BigDecimal(
                        committedValue.replace("%", "").trim()
                )
                        .stripTrailingZeros()
                        .toPlainString();

        /**
         * Verify that the committed value matches the requested value.
         */
        if (!normalizedExpected.equals(normalizedActual)) {

            throw new IllegalStateException(
                    "Addition Value was not committed correctly. " +
                            "Expected: [" + normalizedExpected +
                            "], Actual: [" + normalizedActual + "]"
            );
        }

        System.out.println(
                "Addition Value entered successfully: " +
                        committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Selects a random Currency (العملة) from the Currency cell
     * in the newly created row of the Discounts & Additions AG Grid.
     *
     * The Currency cell uses a React Select editor.
     * The method scrolls to the cell, activates the editor,
     * opens the dropdown, selects a random enabled Currency option,
     * and waits for the editor to close to confirm the selection.
     */
    public void selectRandomCurrencyInDiscountsSection() {

        System.out.println("========================================");
        System.out.println("Selecting random Currency");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Locate the Currency cell in the newly created
         * Discounts & Additions row.
         */
        WebElement currencyCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        getDiscountAdditionCurrencyCellLocator()
                )
        );

        System.out.println(
                "Currency cell found. Current value: [" +
                        currencyCell.getText().trim() +
                        "]"
        );

        /**
         * Scroll the Currency cell into the visible viewport.
         *
         * AG Grid may re-render virtualized cells during scrolling.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                currencyCell
        );

        System.out.println(
                "Currency cell scrolled into view."
        );

        /**
         * Re-locate the Currency cell after scrolling to protect
         * against AG Grid/React DOM re-rendering.
         */
        currencyCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        getDiscountAdditionCurrencyCellLocator()
                )
        );

        /**
         * Locate the React Select input generated inside
         * the Currency cell.
         */
        By currencyInputLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='currencyId'] input"
        );

        /*
         * Activate the React Select editor (double-click, then click + ENTER,
         * then click + F2 if it does not open).
         */
        WebElement currencyInput = openCellEditorWithRetry(
                "Currency",
                getDiscountAdditionCurrencyCellLocator(),
                ExpectedConditions.visibilityOfElementLocated(currencyInputLocator));

        System.out.println(
                "Currency React Select editor found."
        );

        /**
         * Re-locate the input immediately before opening the dropdown
         * because React Select may re-render during editor activation.
         */
        currencyInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        currencyInputLocator
                )
        );

        /**
         * Open the Currency React Select dropdown.
         */
        currencyInput.click();

        System.out.println(
                "Currency dropdown opening..."
        );

        /**
         * Locate the visible React Select listbox.
         */
        By currencyListboxLocator =
                By.cssSelector("div[role='listbox']");

        WebElement currencyListbox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyListboxLocator
                )
        );

        System.out.println(
                "Currency dropdown opened."
        );

        /**
         * Locate all options inside the currently opened Currency dropdown.
         */
        List<WebElement> currencyOptions =
                currencyListbox.findElements(
                        By.cssSelector("[role='option']")
                );

        /**
         * Keep only visible and enabled options.
         */
        List<WebElement> selectableOptions =
                currencyOptions.stream()
                        .filter(WebElement::isDisplayed)
                        .filter(option ->
                                !"true".equals(
                                        option.getAttribute("aria-disabled")
                                )
                        )
                        .toList();

        System.out.println(
                "Number of visible Currency options: " +
                        selectableOptions.size()
        );

        if (selectableOptions.isEmpty()) {

            throw new IllegalStateException(
                    "No selectable Currency options were found."
            );
        }

        /**
         * Select a random enabled Currency option.
         */
        Random random = new Random();

        WebElement randomCurrency =
                selectableOptions.get(
                        random.nextInt(
                                selectableOptions.size()
                        )
                );

        String selectedCurrency =
                randomCurrency.getText().trim();

        System.out.println(
                "Random Currency selected: [" +
                        selectedCurrency +
                        "]"
        );

        /**
         * Click the selected Currency option.
         */
        randomCurrency.click();

        System.out.println(
                "Currency option clicked."
        );

        /**
         * Wait until the React Select input disappears.
         *
         * Its disappearance confirms that the editor has closed
         * and the Currency selection has been committed.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        currencyInputLocator
                )
        );

        System.out.println(
                "Currency selected successfully: [" +
                        selectedCurrency +
                        "]"
        );

        System.out.println("========================================");
    }

    /**
     * Enters and commits the Note (البيان) value in the newly created
     * row of the Discounts & Additions AG Grid.
     *
     * The method re-locates the Note cell using the independent
     * Discounts & Additions row index because AG Grid may re-render
     * the row after Currency and Conversion Rate operations.
     *
     * It activates the editor using double-click first, then falls
     * back to F2 if the editor does not open.
     */
    public void enterDiscountsAdditionsNote(String note) {

        System.out.println("========================================");
        System.out.println("Entering Discounts & Additions Note");
        System.out.println("Requested note: [" + note + "]");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /**
         * Stable locator for the Note cell in the newly created
         * Discounts & Additions row.
         */
        By noteCellLocator =
                getDiscountAdditionNoteCellLocator();

        /**
         * Locator for the AG Grid text input used to edit the Note cell.
         *
         * The locator is scoped to the current Discounts & Additions row
         * so it cannot accidentally target a Note input from another grid.
         */
        By noteEditorLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "[role='gridcell'][col-id='note'] " +
                        "input.ag-text-field-input"
        );

        try {

            /**
             * Locate a fresh Note cell after the previous grid operations.
             */
            WebElement noteCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            noteCellLocator
                    )
            );

            System.out.println(
                    "Note cell found. Current value: [" +
                            noteCell.getText() +
                            "]"
            );

            /**
             * Scroll the Note cell into the visible viewport.
             */
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    noteCell
            );

            /**
             * Re-locate the cell after scrolling because AG Grid
             * may replace the DOM element.
             */
            noteCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            noteCellLocator
                    )
            );

            /**
             * First attempt: activate the AG Grid Note editor
             * using double-click.
             */
            new Actions(driver)
                    .doubleClick(noteCell)
                    .perform();

            System.out.println(
                    "Note cell double-clicked."
            );

            boolean editorOpened = false;

            try {

                /**
                 * Give the React/AG Grid editor a short opportunity
                 * to appear after the double-click.
                 */
                wait.withTimeout(Duration.ofSeconds(2))
                        .until(
                                ExpectedConditions.visibilityOfElementLocated(
                                        noteEditorLocator
                                )
                        );

                editorOpened = true;

                System.out.println(
                        "Note editor opened successfully after double-click."
                );

            } catch (TimeoutException ignored) {

                System.out.println(
                        "Note editor did not open after double-click."
                );

                /**
                 * Re-locate the Note cell before keyboard activation.
                 */
                noteCell = wait.withTimeout(Duration.ofSeconds(10))
                        .until(
                                ExpectedConditions.elementToBeClickable(
                                        noteCellLocator
                                )
                        );

                /**
                 * F2 is used as a fallback because AG Grid supports
                 * F2 for activating cell editing.
                 */
                noteCell.click();

                new Actions(driver)
                        .sendKeys(Keys.F2)
                        .perform();

                System.out.println(
                        "F2 sent to activate Note editor."
                );

                /**
                 * Wait for the editor after F2 activation.
                 */
                wait.withTimeout(Duration.ofSeconds(5))
                        .until(
                                ExpectedConditions.visibilityOfElementLocated(
                                        noteEditorLocator
                                )
                        );

                editorOpened = true;

                System.out.println(
                        "Note editor opened successfully using F2."
                );
            }

            if (!editorOpened) {

                throw new IllegalStateException(
                        "Unable to activate the Note editor."
                );
            }

            /**
             * Always obtain a fresh editor reference after activation
             * because React may replace the input during rendering.
             */
            WebElement noteEditor = wait.withTimeout(Duration.ofSeconds(5))
                    .until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    noteEditorLocator
                            )
                    );

            /**
             * Clear the current editor value using the native
             * HTMLInputElement value setter.
             */
            ((JavascriptExecutor) driver).executeScript(
                    """
                    const input = arguments[0];
    
                    const setter =
                        Object.getOwnPropertyDescriptor(
                            HTMLInputElement.prototype,
                            'value'
                        ).set;
    
                    setter.call(input, '');
    
                    input.dispatchEvent(
                        new Event('input', { bubbles: true })
                    );
    
                    input.dispatchEvent(
                        new Event('change', { bubbles: true })
                    );
                    """,
                    noteEditor
            );

            /**
             * Re-locate the editor after clearing it because React
             * may have re-rendered the input.
             */
            noteEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            noteEditorLocator
                    )
            );

            /**
             * Assign the requested Note using the native input setter.
             */
            ((JavascriptExecutor) driver).executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
    
                    const setter =
                        Object.getOwnPropertyDescriptor(
                            HTMLInputElement.prototype,
                            'value'
                        ).set;
    
                    setter.call(input, value);
    
                    input.dispatchEvent(
                        new Event('input', { bubbles: true })
                    );
    
                    input.dispatchEvent(
                        new Event('change', { bubbles: true })
                    );
                    """,
                    noteEditor,
                    note
            );

            /**
             * Re-locate the editor before reading its value.
             */
            noteEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            noteEditorLocator
                    )
            );

            String actualValue =
                    noteEditor.getDomProperty("value");

            System.out.println(
                    "Note editor value AFTER JavaScript assignment: [" +
                            actualValue +
                            "]"
            );

            /**
             * Verify that the requested value was actually inserted.
             */
            if (!note.equals(actualValue)) {

                throw new IllegalStateException(
                        "Note value was not entered correctly. Expected [" +
                                note +
                                "] but found [" +
                                actualValue +
                                "]"
                );
            }

            /**
             * Clicking the Discounts & Additions tab commits the
             * current Note value and exits AG Grid edit mode.
             */
            By discountsAdditionsTabLocator =
                    By.xpath(
                            "//button[normalize-space()='الخصومات والإضافات']"
                    );

            WebElement discountsAdditionsTab = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            discountsAdditionsTabLocator
                    )
            );

            discountsAdditionsTab.click();

            System.out.println(
                    "Clicked 'الخصومات والإضافات' to commit Note."
            );

            /**
             * Confirm that the Note editor has closed.
             */
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            noteEditorLocator
                    )
            );

            System.out.println(
                    "Note editor closed successfully."
            );

            /**
             * Re-locate the Note cell after committing the value because
             * AG Grid may rebuild the cell after leaving edit mode.
             */
            WebElement committedNoteCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            noteCellLocator
                    )
            );

            String committedValue =
                    committedNoteCell.getText();

            System.out.println(
                    "Note cell value after commit: [" +
                            committedValue +
                            "]"
            );

            /**
             * Final verification of the committed Note value.
             */
            if (!note.equals(committedValue.trim())) {

                throw new IllegalStateException(
                        "Note was not committed correctly. Expected [" +
                                note +
                                "] but found [" +
                                committedValue +
                                "]"
                );
            }

            System.out.println(
                    "Note committed successfully: [" +
                            note +
                            "]"
            );

            System.out.println("========================================");

        } catch (TimeoutException e) {

            System.out.println(
                    "FAILED: Note editor could not be activated."
            );

            /**
             * Capture the current Note cell HTML for diagnostics.
             */
            try {

                WebElement diagnosticCell =
                        driver.findElement(noteCellLocator);

                System.out.println(
                        "NOTE CELL HTML AT FAILURE:"
                );

                System.out.println(
                        diagnosticCell.getAttribute("outerHTML")
                );

            } catch (Exception diagnosticException) {

                System.out.println(
                        "Could not capture Note cell HTML: " +
                                diagnosticException.getMessage()
                );
            }

            throw e;
        }

        System.out.println("========================================");
    }

    /**
     * Clicks the main Save button of the Purchase Order.
     *
     * Uses the stable orders-save data-testid locator and waits until
     * the button becomes clickable before performing the click.
     */
    public void clickSave() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement saveButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        getSaveButtonLocator()
                )
        );

        saveButton.click();

        System.out.println(
                "Purchase Order Save button clicked successfully."
        );
    }

    /**
     * Confirms the Purchase Order action by clicking the "Yes" button
     * in the confirmation modal.
     *
     * Waits until the stable confirmation button is clickable before
     * performing the click.
     */
    public void clickConfirmYes() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement confirmYesButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        getConfirmYesButtonLocator()
                )
        );

        confirmYesButton.click();

        System.out.println(
                "Purchase Order confirmation 'Yes' button clicked successfully."
        );
    }

    /**
     * Selects a random Purchase Order row from the external AG Grid.
     *
     * Retrieves all available data rows using the stable AG Grid row
     * attributes, randomly selects one row, scrolls it into view, and
     * double-clicks it to open the selected Purchase Order.
     *
     * The method does not depend on a hard-coded Purchase Order number,
     * row index, row ID, or dynamically generated data-testid.
     */
    public void selectRandomPurchaseOrderFromExternalGrid() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        List<WebElement> purchaseOrderRows = wait.until(
                ExpectedConditions.visibilityOfAllElementsLocatedBy(
                        getPurchaseOrderRowsLocator()
                )
        );

        if (purchaseOrderRows.isEmpty()) {
            throw new NoSuchElementException(
                    "No Purchase Order rows were found in the external grid."
            );
        }

        Random random = new Random();

        WebElement randomPurchaseOrderRow =
                purchaseOrderRows.get(
                        random.nextInt(purchaseOrderRows.size())
                );

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                randomPurchaseOrderRow
        );

        System.out.println(
                "Random Purchase Order row selected. " +
                        "Row Index: " +
                        randomPurchaseOrderRow.getAttribute("row-index")
        );

        new Actions(driver)
                .doubleClick(randomPurchaseOrderRow)
                .perform();
    }




    /**
     * Waits briefly before locating the Purchase Order Copy button,
     * logs the details of all matching buttons, and performs exactly
     * one native Selenium click after the intended button becomes clickable.
     */
    public void clickCopy() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Wait until the opened order has finished loading (its number is shown)
        // instead of a fixed 3 s.
        wait.until(d -> !d.findElements(By.cssSelector("[data-testid='orders-code']")).isEmpty()
                && !d.findElement(By.cssSelector("[data-testid='orders-code']")).getAttribute("value").isBlank());

        // Wait until at least one Copy button is present in the DOM.
        wait.until(d -> !d.findElements(copyButtonLocator).isEmpty());

        // Retrieve all matching Copy buttons for diagnostic purposes.
        List<WebElement> buttons = driver.findElements(copyButtonLocator);

        System.out.println("Matching Copy buttons: " + buttons.size());

        for (int i = 0; i < buttons.size(); i++) {

            WebElement button = buttons.get(i);

            System.out.println(
                    "Button #" + i
                            + " | displayed=" + button.isDisplayed()
                            + " | enabled=" + button.isEnabled()
                            + " | tag=" + button.getTagName()
                            + " | text=" + button.getText()
                            + " | testId=" + button.getAttribute("data-testid")
            );
        }

        // Wait until the Copy button is visible and enabled for clicking.
        WebElement copyButton = wait.until(
                ExpectedConditions.elementToBeClickable(copyButtonLocator)
        );

        // Perform exactly one native Selenium click.
        copyButton.click();

        System.out.println("Purchase Order Copy button clicked successfully.");
    }





    /**
     * Clicks the Purchase Order Continue button only when the button
     * is currently present and available for interaction.
     *
     * The Continue button is optional and may appear only in some
     * Purchase Order flows. Therefore, this method must not fail the
     * test when the button is not present.
     */
    public void clickContinue() {

        List<WebElement> continueButtons =
                driver.findElements(continueButtonLocator);

        /**
         * The Continue button is not present in the current flow.
         * Skip the action without failing the test.
         */
        if (continueButtons.isEmpty()) {

            System.out.println(
                    "Purchase Order Continue button is not present. Skipping."
            );

            return;
        }

        /**
         * Get the first matching Continue button.
         */
        WebElement continueButton =
                continueButtons.get(0);

        /**
         * Verify that the button is actually displayed and enabled
         * before attempting to click it.
         */
        if (!continueButton.isDisplayed()) {

            System.out.println(
                    "Purchase Order Continue button exists but is not visible. Skipping."
            );

            return;
        }

        if (!continueButton.isEnabled()) {

            System.out.println(
                    "Purchase Order Continue button exists but is disabled. Skipping."
            );

            return;
        }

        /**
         * Click the Continue button when it is available.
         */
        continueButton.click();

        System.out.println(
                "Purchase Order Continue button clicked."
        );
    }


    /**
     * Clicks the "تحديث التكلفة" button only if it becomes visible and clickable.
     * If the button does not appear within 3 seconds, skips the action without
     * failing the test, allowing the Purchase Order workflow to continue.
     */
    public void clickUpdateCost() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        try {
            WebElement updateCostButton = wait.until(
                    ExpectedConditions.elementToBeClickable(updateCostButtonLocator)
            );

            updateCostButton.click();

            System.out.println(
                    "Clicked 'تحديث التكلفة' button successfully."
            );

        } catch (TimeoutException e) {

            System.out.println(
                    "'تحديث التكلفة' button did not appear within 3 seconds. " +
                            "Skipping this step."
            );
        }
    }


    /**
     * Clicks the Add button from the external Purchase Orders grid.
     *
     * This method is responsible only for triggering the creation of a new
     * Purchase Order from the external grid. The subsequent Purchase Order
     * creation steps remain handled by the existing Create PO flow.
     */
    public void clickAddPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        System.out.println(
                "========================================"
        );

        System.out.println(
                "Clicking Add Purchase Order button..."
        );

        WebElement addButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                addPurchaseOrderButton
                        )
                );

        System.out.println(
                "Add Purchase Order button found."
        );

        ((JavascriptExecutor) driver).executeScript(
                """
                arguments[0].scrollIntoView({
                    block: 'center',
                    inline: 'nearest'
                });
                """,
                addButton
        );

        addButton.click();

        System.out.println(
                "Add Purchase Order button clicked successfully."
        );

        System.out.println(
                "========================================"
        );
    }

    /**
     * Clicks the enabled "Download Item" button after the selected
     * Reference Number makes the button available for interaction.
     *
     * The button may remain disabled for a short period while the
     * Purchase Order page is re-rendering after Reference Number selection.
     * Therefore, the method waits until the enabled Download button
     * becomes clickable before interacting with it.
     */
    public void clickDownloadItemButton() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement downloadButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                downloadItemButtonLocator
                        )
                );

        downloadButton.click();

        System.out.println(
                "Download Item button clicked successfully."
        );
    }

    /**
     * Opens the Reference Document selection grid in the Purchase Order form.
     * Waits until the button associated with the referenceDocument field
     * becomes clickable, then clicks it to display the reference documents.
     */
    public void clickOpenReferenceDocumentGridButton() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement openButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        openReferenceDocumentGridButtonLocator
                )
        );

        openButton.click();
    }

    /**
     * Randomly selects either Purchases Store or Purchases Quotations Store.
     * Waits until the selected radio button is clickable, clicks it,
     * and logs the selected reference screen.
     */
    public void selectRandomReferenceScreen() {

        List<By> referenceScreenLocators = Arrays.asList(
                purchasesStoreRadioLocator,
                purchasesQuotationsStoreRadioLocator
        );

        List<String> referenceScreenNames = Arrays.asList(
                "Purchases Store",
                "Purchases Quotations Store"
        );

        int randomIndex = new Random().nextInt(referenceScreenLocators.size());

        By selectedLocator = referenceScreenLocators.get(randomIndex);
        String selectedName = referenceScreenNames.get(randomIndex);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement selectedRadio = wait.until(
                ExpectedConditions.elementToBeClickable(selectedLocator)
        );

        selectedRadio.click();

        System.out.println("Randomly selected Reference Screen: " + selectedName);
    }



    /**
     * Randomly selects a reference-document row, verifies its radio selection,
     * and double-clicks the corresponding row in the main AG Grid data area.
     *
     * The radio control is located in the pinned-right selection column, while
     * the double-click is performed on the matching row in the center container
     * to trigger the grid's row-level double-click handler.
     *
     * The method waits for the radio selection to be confirmed before performing
     * the double-click and does not return until the double-click action completes.
     */
    public void selectRandomRowFromGrid() {

        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(15));

        wait.pollingEvery(Duration.ofMillis(300));

        // Locates rows in the pinned-right column containing the selection radio.
        By rowsLocator = By.cssSelector(
                "div.ag-pinned-right-cols-container div[role='row']"
        );

        // Locates the radio control used to select a reference-document row.
        By radioLocator = By.cssSelector(
                "div[role='gridcell'][col-id='custom-checkbox'] [role='radio']"
        );

        // Waits until at least one visible, enabled radio control is available.
        wait.until(d -> {
            for (WebElement row : referenceGridScope(d).findElements(rowsLocator)) {
                try {
                    for (WebElement radio : row.findElements(radioLocator)) {
                        if (radio.isDisplayed() && radio.isEnabled()) {
                            return true;
                        }
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Retry if AG Grid re-renders while its rows are inspected.
                }
            }
            return false;
        });

        // Collects indexes of rows that contain selectable radio controls.
        List<Integer> availableRowIndexes = new ArrayList<>();

        for (WebElement row : referenceGridScope(driver).findElements(rowsLocator)) {
            try {
                String rowIndex = row.getAttribute("row-index");

                if (rowIndex == null) {
                    continue;
                }

                boolean selectable = false;

                for (WebElement radio : row.findElements(radioLocator)) {
                    try {
                        if (radio.isDisplayed() && radio.isEnabled()) {
                            selectable = true;
                            break;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Ignore an individual radio invalidated by re-rendering.
                    }
                }

                if (selectable) {
                    availableRowIndexes.add(Integer.parseInt(rowIndex));
                }

            } catch (StaleElementReferenceException ignored) {
                // Skip a row that was re-rendered during collection.
            }
        }

        // Stops with a clear diagnostic if no selectable rows were collected.
        if (availableRowIndexes.isEmpty()) {
            throw new NoSuchElementException(
                    "No selectable reference-document rows were found " +
                            "in the pinned-right AG Grid container."
            );
        }

        // Randomly chooses one of the available reference-document rows.
        int randomRowIndex = availableRowIndexes.get(
                new Random().nextInt(availableRowIndexes.size()));

        // Identifies the radio belonging to the randomly chosen row.
        By selectedRadioLocator = By.cssSelector(
                "div.ag-pinned-right-cols-container div[role='row']" +
                        "[row-index='" + randomRowIndex + "'] " +
                        "div[role='gridcell'][col-id='custom-checkbox'] [role='radio']"
        );

        // Clicks the radio only if it is not already selected.
        wait.until(d -> {
            try {
                WebElement radio = referenceGridScope(d).findElement(selectedRadioLocator);

                if (!radio.isDisplayed() || !radio.isEnabled()) {
                    return false;
                }

                boolean alreadySelected =
                        "true".equals(radio.getAttribute("aria-checked"))
                                || "checked".equals(radio.getAttribute("data-state"));

                if (alreadySelected) {
                    return true;
                }

                radio.click();
                return true;

            } catch (StaleElementReferenceException
                     | ElementClickInterceptedException
                     | NoSuchElementException ignored) {
                return false;
            }
        });

        // Verifies the radio's selected state before interacting with the row.
        wait.until(d -> {
            try {
                WebElement radio = referenceGridScope(d).findElement(selectedRadioLocator);

                return "true".equals(radio.getAttribute("aria-checked"))
                        || "checked".equals(radio.getAttribute("data-state"));

            } catch (StaleElementReferenceException
                     | NoSuchElementException ignored) {
                return false;
            }
        });

        System.out.println(
                "Reference-document radio selected. Row Index: "
                        + randomRowIndex);

        // Locates the matching row in the main data container, not the pinned
        // selection column, so the double-click targets the actual grid content.
        By mainGridRowLocator = By.cssSelector(
                "div.ag-center-cols-container div[role='row']" +
                        "[row-index='" + randomRowIndex + "']"
        );

        // Waits for the main grid row to become visible and scrolls it into view.
        // Searched inside the reference dialog: the order form behind it has its
        // own AG Grid with the same row-index values, and a global lookup can hit
        // that grid's (hidden) row instead.
        WebElement mainGridRow = wait.until(d -> {
            try {
                for (WebElement row : referenceGridScope(d).findElements(mainGridRowLocator)) {
                    if (row.isDisplayed()) {
                        return row;
                    }
                }
            } catch (StaleElementReferenceException ignored) {
                // Retry if AG Grid re-renders.
            }
            return null;
        });

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                mainGridRow);

        // Re-locates the row immediately before double-clicking to avoid stale DOM.
        wait.until(d -> {
            try {
                WebElement row = referenceGridScope(d).findElement(mainGridRowLocator);

                if (!row.isDisplayed()) {
                    return false;
                }

                new Actions(d)
                        .doubleClick(row)
                        .perform();

                return true;

            } catch (StaleElementReferenceException
                     | ElementClickInterceptedException
                     | NoSuchElementException ignored) {
                return false;
            }
        });

        System.out.println(
                "Double-click performed on main grid row. Row Index: "
                        + randomRowIndex);
    }





    /** First displayed + enabled match of {@code locator} inside {@code scope}, or null. */
    private static WebElement clickableIn(SearchContext scope, By locator) {
        try {
            for (WebElement e : scope.findElements(locator)) {
                if (e.isDisplayed() && e.isEnabled()) {
                    return e;
                }
            }
        } catch (StaleElementReferenceException ignored) {
            // re-rendered; caller's wait polls again
        }
        return null;
    }

    /**
     * The search root for the reference-document grid: the last open dialog
     * when one is shown, otherwise the whole page. Keeps row lookups away from
     * the order form's own grid, which uses the same AG Grid classes and
     * row-index values.
     */
    private SearchContext referenceGridScope(WebDriver d) {

        List<WebElement> dialogs = d.findElements(
                By.cssSelector("[role='dialog'][data-state='open']"));

        for (int i = dialogs.size() - 1; i >= 0; i--) {
            try {
                WebElement dialog = dialogs.get(i);
                if (!dialog.findElements(By.cssSelector(
                        "div.ag-center-cols-container")).isEmpty()) {
                    return dialog;
                }
            } catch (StaleElementReferenceException ignored) {
                // Dialog re-rendered; fall through to the next candidate.
            }
        }

        return d;
    }

    /**
     * Clicks the reference-document download button after the UI is ready.
     *
     * Waits for the button to become visible and enabled, re-locates it on each
     * retry, and handles transient DOM re-renders that may occur after selecting
     * a reference document.
     */
    public void downloadReferenceDocument() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(20)
        );

        // Allow retries when the button becomes stale or temporarily unclickable.
        wait.pollingEvery(Duration.ofMillis(300));

        // Re-find the download button on every poll instead of retaining an old element.
        wait.until(currentDriver -> {
            try {
                WebElement downloadButton =
                        currentDriver.findElement(downloadReferenceDocumentButton);

                // Ensure the button is visible and enabled before attempting to click.
                if (!downloadButton.isDisplayed() || !downloadButton.isEnabled()) {
                    return false;
                }

                // Click only when Selenium confirms that the button is ready.
                downloadButton.click();

                System.out.println(
                        "Reference-document download button clicked successfully."
                );

                return true;

            } catch (StaleElementReferenceException
                     | ElementClickInterceptedException
                     | NoSuchElementException e) {

                // Retry if the UI re-renders, the button is temporarily missing,
                // or another element temporarily blocks the click.
                return false;
            }
        });
    }


    // =====================================================================
    // Purchase Orders grid — pagination (verified in QC, 2026-10-27)
    // "first"/"last" are icon buttons with aria-label Previous/Next and a
    // hidden (sr-only) label; the summary reads "عرض 231 - 233 / 233".
    // =====================================================================

    private final By firstPageButton = By.xpath(
            "//button[@aria-label='Previous']" +
                    "[.//span[contains(@class,'sr-only') and normalize-space()='first']]");

    private final By lastPageButton = By.xpath(
            "//button[@aria-label='Next']" +
                    "[.//span[contains(@class,'sr-only') and normalize-space()='last']]");

    private final By gridNextPageButton = By.xpath(
            "//button[@aria-label='Next']" +
                    "[.//span[contains(@class,'sr-only') and normalize-space()='Next']]");

    private final By gridPreviousPageButton = By.xpath(
            "//button[@aria-label='Previous']" +
                    "[.//span[contains(@class,'sr-only') and normalize-space()='Previous']]");

    private final By purchaseOrderGridRows = By.cssSelector(
            ".ag-center-cols-container div[role='row'][row-id]");

    /** Jumps straight to the last page of the Purchase Orders grid ("last" button). */
    public void goToLastPurchaseOrdersPage() {
        clickPagerButtonUntilDisabled(lastPageButton, "last");
        waitForPaginationRange(r -> r[1] == r[2]);      // "... - total / total"
        waitForRowsToMatchSummary();
    }

    /** Jumps straight to the first page of the Purchase Orders grid ("first" button). */
    public void goToFirstPurchaseOrdersPage() {
        clickPagerButtonUntilDisabled(firstPageButton, "first");
        waitForPaginationRange(r -> r[0] == 1);         // "1 - ... / total"
        waitForRowsToMatchSummary();
    }

    /**
     * Waits until the grid has finished redrawing: the number of rendered rows
     * equals the summary's "to - from + 1". Right after navigation (or after
     * closing a form) AG Grid can briefly show rows of the old and new page
     * together (seen in QC, 2026-10-27: 12 rows on a 10-row page).
     */
    private void waitForRowsToMatchSummary() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(IllegalStateException.class)
                .ignoring(StaleElementReferenceException.class)
                .until(d -> {
                    int[] r = getPurchaseOrdersPaginationRange();
                    return getVisiblePurchaseOrderRowCount() == r[1] - r[0] + 1;
                });
    }

    /** Waits until the pagination summary satisfies {@code condition}. */
    private void waitForPaginationRange(java.util.function.Predicate<int[]> condition) {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(IllegalStateException.class)
                .until(d -> condition.test(getPurchaseOrdersPaginationRange()));
    }

    /**
     * Clicks a "first"/"last" button and waits until it becomes disabled,
     * which means the grid is now on that end page. Does nothing if it is
     * already disabled (already there, or only one page).
     */
    private void clickPagerButtonUntilDisabled(By button, String name) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));

        wait.until(ExpectedConditions.visibilityOfElementLocated(purchaseOrderGridRows));

        WebElement pagerButton =
                wait.until(ExpectedConditions.presenceOfElementLocated(button));

        if (!pagerButton.isEnabled()) {
            System.out.println("Pagination '" + name + "' button already disabled. No navigation needed.");
            return;
        }

        clickWhenReady(button);

        wait.ignoring(StaleElementReferenceException.class)
                .until(d -> !d.findElement(button).isEnabled());

        wait.until(ExpectedConditions.visibilityOfElementLocated(purchaseOrderGridRows));

        System.out.println("Navigated to the '" + name + "' page of Purchase Orders.");
    }

    public boolean isNextPurchaseOrdersPageEnabled() {
        return driver.findElement(gridNextPageButton).isEnabled();
    }

    public boolean isPreviousPurchaseOrdersPageEnabled() {
        return driver.findElement(gridPreviousPageButton).isEnabled();
    }

    public int getVisiblePurchaseOrderRowCount() {
        return driver.findElements(purchaseOrderGridRows).size();
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public int[] getPurchaseOrdersPaginationRange() {
        return list.getPaginationRange();
    }

    /**
     * Selects a random Purchase Order from the LAST page of the grid, reads its
     * status, opens it and returns the status.
     *
     * <p>Jumps directly to the last page with the "last" button instead of
     * scanning every page (the previous behaviour, kept as
     * {@link #selectRandomPurchaseOrderFromAllPagesAndGetStatus()}): much faster
     * and avoids the 429 rate limit. The last page holds the most recent orders.</p>
     *
     * @return the status of the selected Purchase Order
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String selectRandomPurchaseOrderAndGetStatus()
            throws InterruptedException {

        System.out.println("Purchase Orders grid is displayed.");

        goToLastPurchaseOrdersPage();

        List<String> rowIds = new ArrayList<>();
        for (WebElement row : driver.findElements(purchaseOrderGridRows)) {
            String rowId = row.getAttribute("row-id");
            if (rowId != null && !rowId.trim().isEmpty()) {
                rowIds.add(rowId);
            }
        }

        if (rowIds.isEmpty()) {
            throw new NoSuchElementException(
                    "No Purchase Orders were found on the last pagination page."
            );
        }

        String selectedRowId = rowIds.get(new Random().nextInt(rowIds.size()));

        System.out.println("Rows on last page: " + rowIds.size());
        System.out.println("Random Purchase Order selected from the last page. row-id: " + selectedRowId);

        return readStatusAndOpenPurchaseOrderRow(selectedRowId);
    }

    /**
     * Steps 5–6 of the original selector: reads the recordStatus of the row
     * (scrolling the RTL grid horizontally) and opens it with a double-click.
     * Shared by the last-page selector; the original keeps its own copy.
     */
    private String readStatusAndOpenPurchaseOrderRow(String selectedRowId)
            throws InterruptedException {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Locates the business-status cell in a Purchase Order grid row.
        By statusCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='recordStatus']"
        );

        // ---------------------------------------------------------
        // Step 5: Locate the selected row and read its status.
        // ---------------------------------------------------------

        // Locates the same Purchase Order by its row ID on the selected page.
        By selectedRowLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-id='" +
                        selectedRowId + "']"
        );

        // Re-locates the row to avoid using an outdated WebElement.
        WebElement selectedRow =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                selectedRowLocator
                        )
                );

        WebElement statusCell = null;

        /*
         * Attempts to locate the status cell while scrolling horizontally.
         * This preserves the incremental negative-scroll strategy used
         * in the existing RTL AG Grid implementation.
         */
        for (int attempt = 0; attempt < 12; attempt++) {

            // Re-locates the row after scrolling because AG Grid may re-render it.
            List<WebElement> selectedRows =
                    driver.findElements(selectedRowLocator);

            if (!selectedRows.isEmpty()) {

                // Searches for the status cell only within the selected row.
                List<WebElement> statusCells =
                        selectedRows.get(0).findElements(statusCellLocator);

                if (!statusCells.isEmpty()) {
                    statusCell = statusCells.get(0);
                    break;
                }
            }

            // Moves both AG Grid horizontal viewports incrementally.
            js.executeScript(
                    """
                    const viewport =
                        document.querySelector('.ag-center-cols-viewport');
    
                    const horizontal =
                        document.querySelector('.ag-body-horizontal-scroll-viewport');
    
                    const amount = arguments[0];
    
                    if (viewport) {
                        viewport.scrollLeft = amount;
                        viewport.dispatchEvent(
                            new Event('scroll', {bubbles: true})
                        );
                    }
    
                    if (horizontal) {
                        horizontal.scrollLeft = amount;
                        horizontal.dispatchEvent(
                            new Event('scroll', {bubbles: true})
                        );
                    }
                    """,
                    -(attempt + 1) * 300
            );

            // Waits (max 1 s) for AG Grid to render the newly visible columns.
            waitForGridColumnsToRender();
        }

        // Throws an informative exception if the status cell could not be located.
        if (statusCell == null) {
            throw new NoSuchElementException(
                    "Could not find recordStatus cell for Purchase Order row-id: " +
                            selectedRowId
            );
        }

        // Reads the displayed status text from the selected cell.
        String status = statusCell.getText().trim();

        // Falls back to textContent if Selenium's visible-text retrieval is empty.
        if (status.isEmpty()) {
            String textContent = statusCell.getAttribute("textContent");
            status = textContent == null ? "" : textContent.trim();
        }

        // Falls back to innerText if textContent did not provide the status.
        if (status.isEmpty()) {
            String innerText = statusCell.getAttribute("innerText");
            status = innerText == null ? "" : innerText.trim();
        }

        // Uses the accessibility label if the text properties are empty.
        if (status.isEmpty()) {
            String ariaLabel = statusCell.getAttribute("aria-label");
            status = ariaLabel == null ? "" : ariaLabel.trim();
        }

        // Uses the title attribute as the final text fallback.
        if (status.isEmpty()) {
            String title = statusCell.getAttribute("title");
            status = title == null ? "" : title.trim();
        }

        // Prints the cell HTML and fails clearly if no status was retrieved.
        if (status.isEmpty()) {

            System.out.println("recordStatus cell outerHTML:");
            System.out.println(statusCell.getAttribute("outerHTML"));

            throw new IllegalStateException(
                    "Purchase Order status is empty for row-id: " + selectedRowId
            );
        }

        System.out.println(
                "Selected Purchase Order status: [" + status + "]"
        );

        // ---------------------------------------------------------
        // Step 6: Open the same Purchase Order whose status was retrieved.
        // ---------------------------------------------------------

        // Re-locates the selected row immediately before opening it.
        WebElement rowToOpen =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                selectedRowLocator
                        )
                );

        // Opens the selected Purchase Order using a double-click.
        new Actions(driver)
                .doubleClick(rowToOpen)
                .perform();

        System.out.println(
                "Selected Purchase Order double-clicked. row-id: " + selectedRowId
        );

        // Returns the status retrieved from the selected Purchase Order.
        return status;
    }


    /**
     * Selects a random Purchase Order from a randomly selected pagination page,
     * scrolls horizontally through the RTL AG Grid until the selected row's
     * recordStatus cell is found, reads the status, and double-clicks the same row.
     *
     * Pagination and horizontal scrolling follow the same working approach used
     * by selectRandomPriceQuotationAndGetStatus().
     *
     * @return the business status of the selected Purchase Order.
     */
    /**
     * ORIGINAL implementation, kept unchanged under a new name (2026-10-27).
     * Scans EVERY page of the grid — slow (≈30 s for 22 pages) and can hit the
     * server's 429 rate limit, so the default selector now uses the last page only.
     * Used by the pagination test.
     *
     * Selects a random Purchase Order from all pagination pages,
     * reads its business status, opens the same Purchase Order,
     * and returns the retrieved status.
     *
     * The method scans each page only once, stores the selected row's
     * row-id and page number, and re-locates the row before opening it
     * to avoid using stale WebElements after pagination or scrolling.
     *
     * @return the status of the selected Purchase Order
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String selectRandomPurchaseOrderFromAllPagesAndGetStatus()
            throws InterruptedException {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        Random random = new Random();

        // Locates data rows inside the central Purchase Orders AG Grid container.
        By rowsLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-id]"
        );

        // Locates the Next pagination button without matching the Last button.
        By nextPageButton = By.xpath(
                "//button[@aria-label='Next']" +
                        "[.//span[contains(@class,'sr-only') " +
                        "and normalize-space()='Next']]"
        );

        // Locates the Previous pagination button without matching the First button.
        By previousPageButton = By.xpath(
                "//button[@aria-label='Previous']" +
                        "[.//span[contains(@class,'sr-only') " +
                        "and normalize-space()='Previous']]"
        );

        // Locates the business-status cell in a Purchase Order grid row.
        By statusCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='recordStatus']"
        );

        System.out.println("Purchase Orders grid is displayed.");

        // Waits until at least one Purchase Order row is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(rowsLocator)
        );

        // ---------------------------------------------------------
        // Step 1: Return to the first pagination page.
        // ---------------------------------------------------------

        while (true) {

            // Re-locates the Previous button to get its current enabled state.
            WebElement previousButton =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    previousPageButton
                            )
                    );

            // Stops when the Previous button is disabled on the first page.
            if (!previousButton.isEnabled()
                    || previousButton.getAttribute("disabled") != null) {
                break;
            }

            // Moves to the previous page and waits until the grid shows it.
            clickPagerAndWait(previousPageButton);

            // Waits for the rows to be rendered on the newly selected page.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(rowsLocator)
            );
        }

        System.out.println("Currently on the first page.");

        // ---------------------------------------------------------
        // Step 2: Scan each page once and collect row IDs and page numbers.
        // ---------------------------------------------------------

        List<String> purchaseOrderRowIds = new ArrayList<>();
        List<Integer> purchaseOrderPageNumbers = new ArrayList<>();

        int currentPage = 1;

        while (true) {

            // Waits for the current page's Purchase Order rows to be visible.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(rowsLocator)
            );

            // Captures the row IDs currently rendered on this page.
            List<WebElement> rows = driver.findElements(rowsLocator);

            System.out.println(
                    "Scanning Purchase Orders page: " + currentPage +
                            ", rows found: " + rows.size()
            );

            // Stores each valid row ID together with its pagination page number.
            for (WebElement row : rows) {

                String rowId = row.getAttribute("row-id");

                // Ignores rows that do not expose a usable row ID.
                if (rowId != null && !rowId.trim().isEmpty()) {

                    purchaseOrderRowIds.add(rowId);
                    purchaseOrderPageNumbers.add(currentPage);
                }
            }

            // Re-locates the Next button after processing the current page.
            WebElement nextButton =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    nextPageButton
                            )
                    );

            // Stops scanning when the Next button is disabled on the last page.
            if (!nextButton.isEnabled()
                    || nextButton.getAttribute("disabled") != null) {
                break;
            }

            // Navigates to the next page and waits until the grid shows it.
            clickPagerAndWait(nextPageButton);

            currentPage++;
        }

        int totalPages = currentPage;

        System.out.println("Total Purchase Orders pages: " + totalPages);
        System.out.println(
                "Total Purchase Orders collected: " + purchaseOrderRowIds.size()
        );

        // Throws a clear exception if no valid Purchase Order rows were collected.
        if (purchaseOrderRowIds.isEmpty()) {
            throw new NoSuchElementException(
                    "No Purchase Orders were found across the pagination pages."
            );
        }

        // ---------------------------------------------------------
        // Step 3: Select a random Purchase Order from all scanned pages.
        // ---------------------------------------------------------

        int randomIndex = random.nextInt(purchaseOrderRowIds.size());

        String selectedRowId = purchaseOrderRowIds.get(randomIndex);
        int selectedPage = purchaseOrderPageNumbers.get(randomIndex);

        System.out.println("Random Purchase Order selected.");
        System.out.println("Selected row-id: " + selectedRowId);
        System.out.println("Selected page: " + selectedPage);

        // ---------------------------------------------------------
        // Step 4: Return to the selected Purchase Order's page.
        // ---------------------------------------------------------

        // The grid is currently on its last page after the scan.
        while (currentPage > selectedPage) {

            // Waits for the Previous button to become clickable.
            WebElement previousButton =
                    wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    previousPageButton
                            )
                    );

            // Moves back one page and waits until the grid shows it.
            clickPagerAndWait(previousPageButton);

            currentPage--;
        }

        System.out.println("Returned to selected page: " + selectedPage);

        // Waits for the rows on the selected page to be rendered.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(rowsLocator)
        );

        // ---------------------------------------------------------
        // Step 5: Locate the selected row and read its status.
        // ---------------------------------------------------------

        // Locates the same Purchase Order by its row ID on the selected page.
        By selectedRowLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-id='" +
                        selectedRowId + "']"
        );

        // Re-locates the row to avoid using an outdated WebElement.
        WebElement selectedRow =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                selectedRowLocator
                        )
                );

        WebElement statusCell = null;

        /*
         * Attempts to locate the status cell while scrolling horizontally.
         * This preserves the incremental negative-scroll strategy used
         * in the existing RTL AG Grid implementation.
         */
        for (int attempt = 0; attempt < 12; attempt++) {

            // Re-locates the row after scrolling because AG Grid may re-render it.
            List<WebElement> selectedRows =
                    driver.findElements(selectedRowLocator);

            if (!selectedRows.isEmpty()) {

                // Searches for the status cell only within the selected row.
                List<WebElement> statusCells =
                        selectedRows.get(0).findElements(statusCellLocator);

                if (!statusCells.isEmpty()) {
                    statusCell = statusCells.get(0);
                    break;
                }
            }

            // Moves both AG Grid horizontal viewports incrementally.
            js.executeScript(
                    """
                    const viewport =
                        document.querySelector('.ag-center-cols-viewport');
    
                    const horizontal =
                        document.querySelector('.ag-body-horizontal-scroll-viewport');
    
                    const amount = arguments[0];
    
                    if (viewport) {
                        viewport.scrollLeft = amount;
                        viewport.dispatchEvent(
                            new Event('scroll', {bubbles: true})
                        );
                    }
    
                    if (horizontal) {
                        horizontal.scrollLeft = amount;
                        horizontal.dispatchEvent(
                            new Event('scroll', {bubbles: true})
                        );
                    }
                    """,
                    -(attempt + 1) * 300
            );

            // Waits (max 1 s) for AG Grid to render the newly visible columns.
            waitForGridColumnsToRender();
        }

        // Throws an informative exception if the status cell could not be located.
        if (statusCell == null) {
            throw new NoSuchElementException(
                    "Could not find recordStatus cell for Purchase Order row-id: " +
                            selectedRowId
            );
        }

        // Reads the displayed status text from the selected cell.
        String status = statusCell.getText().trim();

        // Falls back to textContent if Selenium's visible-text retrieval is empty.
        if (status.isEmpty()) {
            String textContent = statusCell.getAttribute("textContent");
            status = textContent == null ? "" : textContent.trim();
        }

        // Falls back to innerText if textContent did not provide the status.
        if (status.isEmpty()) {
            String innerText = statusCell.getAttribute("innerText");
            status = innerText == null ? "" : innerText.trim();
        }

        // Uses the accessibility label if the text properties are empty.
        if (status.isEmpty()) {
            String ariaLabel = statusCell.getAttribute("aria-label");
            status = ariaLabel == null ? "" : ariaLabel.trim();
        }

        // Uses the title attribute as the final text fallback.
        if (status.isEmpty()) {
            String title = statusCell.getAttribute("title");
            status = title == null ? "" : title.trim();
        }

        // Prints the cell HTML and fails clearly if no status was retrieved.
        if (status.isEmpty()) {

            System.out.println("recordStatus cell outerHTML:");
            System.out.println(statusCell.getAttribute("outerHTML"));

            throw new IllegalStateException(
                    "Purchase Order status is empty for row-id: " + selectedRowId
            );
        }

        System.out.println(
                "Selected Purchase Order status: [" + status + "]"
        );

        // ---------------------------------------------------------
        // Step 6: Open the same Purchase Order whose status was retrieved.
        // ---------------------------------------------------------

        // Re-locates the selected row immediately before opening it.
        WebElement rowToOpen =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                selectedRowLocator
                        )
                );

        // Opens the selected Purchase Order using a double-click.
        new Actions(driver)
                .doubleClick(rowToOpen)
                .perform();

        System.out.println(
                "Selected Purchase Order double-clicked. row-id: " + selectedRowId
        );

        // Returns the status retrieved from the selected Purchase Order.
        return status;
    }




    /**
     * Clicks the Edit/View button to open the selected Purchase Order.
     * Waits until the button is clickable before interacting with it.
     */
    public void clickEditPurchaseOrderButton() {

        // QC, 2026-10-31: right after an order opens, the first click on "تعديل"
        // can be lost (before item 2 a fixed 3 s pause hid this). The click is
        // checked by the URL: /edit/ (Draft, Completed) or /view-approved/
        // (Approved); one more click if it did not switch.
        for (int attempt = 1; attempt <= 2; attempt++) {
            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .ignoring(StaleElementReferenceException.class)
                    .until(ExpectedConditions.elementToBeClickable(editPurchaseOrderButton))
                    .click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(d -> d.getCurrentUrl().contains("/edit/")
                                || d.getCurrentUrl().contains("/view-approved/"));
                return;
            } catch (TimeoutException e) {
                System.out.println("Edit click did not switch to edit mode (attempt " + attempt
                        + "/2). URL: " + driver.getCurrentUrl());
            }
        }
        throw new TimeoutException("The order did not switch to edit mode after 2 clicks on \"تعديل\". URL: "
                + driver.getCurrentUrl());
    }


    /** Draft status text as shown in the "الحالة" column. */
    private static final String DRAFT_STATUS = "مسودة";

    /**
     * Opens a random Draft Purchase Order, navigating as little as possible.
     * See {@link #selectRandomPurchaseOrderWithStatus(String)}.
     *
     * @throws InterruptedException if a wait is interrupted.
     * @throws NoSuchElementException if no page has a Draft Purchase Order.
     */
    public void selectRandomDraftPurchaseOrder()
            throws InterruptedException {

        selectRandomPurchaseOrderWithStatus(DRAFT_STATUS);
    }

    /**
     * Opens a random Purchase Order whose "الحالة" is {@code status}, navigating
     * as little as possible.
     *
     * <ol>
     *     <li>Checks the page the grid is on now. If it has a matching order,
     *         one of them is opened and no navigation happens.</li>
     *     <li>Otherwise goes back page by page ("Previous") and checks each page
     *         on the way; stops at the first page that has a match.</li>
     *     <li>If none was found down to page 1, goes forward ("Next") past the
     *         pages already checked, then checks each new page; stops at the
     *         first page that has a match.</li>
     * </ol>
     *
     * <p>The random choice is made among the matches of the page where the search
     * stops. Fewer page changes also means fewer requests (the server answers
     * 429 when pages are changed quickly).</p>
     *
     * @param status the Arabic status text, e.g. "مسودة" or "معتمد".
     * @throws NoSuchElementException if no page has an order with that status.
     */
    public void selectRandomPurchaseOrderWithStatus(String status)
            throws InterruptedException {

        selectRandomPurchaseOrderWithStatus(status, Collections.emptySet());
    }

    /**
     * Same search, but never opens a row whose row-id is in {@code excludedRowIds}
     * (orders already tried and found unsuitable by the test). A page whose only
     * matches are excluded counts as "no match", so the search moves on.
     */
    public void selectRandomPurchaseOrderWithStatus(String status, Set<String> excludedRowIds)
            throws InterruptedException {

        this.excludedRowIds = excludedRowIds;
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(purchaseOrderGridRows));
        System.out.println("Purchase Orders grid is displayed. Looking for status [" + status + "].");

        // Step 1: the current page — before any navigation.
        if (openRandomRowWithStatusOnCurrentPage(status, "current page")) {
            return;
        }

        // Step 2: backwards, checking every page on the way.
        int pagesBack = 0;
        while (isPagerButtonEnabled(gridPreviousPageButton)) {
            clickPagerAndWait(gridPreviousPageButton);
            pagesBack++;
            if (openRandomRowWithStatusOnCurrentPage(status, "page -" + pagesBack + " from start")) {
                return;
            }
        }

        // Step 3: forwards. The first pagesBack "Next" clicks only return to the
        // starting page (already checked, no match), so nothing is read there.
        int pagesForward = 0;
        while (isPagerButtonEnabled(gridNextPageButton)) {
            clickPagerAndWait(gridNextPageButton);
            pagesForward++;
            if (pagesForward <= pagesBack) {
                continue;
            }
            if (openRandomRowWithStatusOnCurrentPage(status,
                    "page +" + (pagesForward - pagesBack) + " from start")) {
                return;
            }
        }

        throw new NoSuchElementException(
                "No Purchase Orders with status [" + status + "] were found on any page.");
    }

    /**
     * Reads the statuses of the current page; if it has orders with
     * {@code status}, opens one of them at random (double-click) and returns true.
     */
    private boolean openRandomRowWithStatusOnCurrentPage(String status, String where) {

        List<String> rowIds = readRowIdsWithStatusOnCurrentPage(status);
        rowIds.removeAll(excludedRowIds);
        int[] range = getPurchaseOrdersPaginationRange();
        System.out.println("Checked " + where + " (records " + range[0] + "-" + range[1]
                + " / " + range[2] + "): " + rowIds.size() + " [" + status + "] order(s).");

        if (rowIds.isEmpty()) {
            return false;
        }

        String rowId = rowIds.get(new Random().nextInt(rowIds.size()));
        By rowLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-id='" + rowId + "']");

        WebElement row = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(rowLocator));
        new Actions(driver).doubleClick(row).perform();

        lastOpenedRowId = rowId;
        System.out.println("Opened [" + status + "] Purchase Order. row-id: " + rowId);
        return true;
    }

    /** Row-ids the current status search must skip (set per search). */
    private Set<String> excludedRowIds = Collections.emptySet();

    /** Row-id of the order opened by the last status search. */
    private String lastOpenedRowId;

    public String getLastOpenedRowId() {
        return lastOpenedRowId;
    }

    /** Opens a random Draft order whose row-id is not in {@code excludedRowIds}. */
    public void selectRandomDraftPurchaseOrder(Set<String> excludedRowIds)
            throws InterruptedException {

        selectRandomPurchaseOrderWithStatus(DRAFT_STATUS, excludedRowIds);
    }

    /**
     * Returns the row-ids of the current page whose "الحالة" equals {@code status}.
     * Scrolls the RTL grid horizontally first so the status column is rendered.
     */
    private List<String> readRowIdsWithStatusOnCurrentPage(String status) {

        ((JavascriptExecutor) driver).executeScript(
                "for (const sel of ['.ag-center-cols-viewport', '.ag-body-horizontal-scroll-viewport']) {"
                        + "  const el = document.querySelector(sel);"
                        + "  if (el) { el.scrollLeft = -10000; el.dispatchEvent(new Event('scroll', {bubbles: true})); }"
                        + "}");

        List<String> rowIds = new ArrayList<>();
        for (WebElement row : driver.findElements(purchaseOrderGridRows)) {
            String rowId = row.getAttribute("row-id");
            if (rowId != null && !rowId.isBlank()) {
                rowIds.add(rowId);
            }
        }

        List<String> matching = new ArrayList<>();
        for (String rowId : rowIds) {
            if (status.equals(readRowStatus(rowId))) {
                matching.add(rowId);
            }
        }
        return matching;
    }

    /**
     * Status text of one row (waits up to 5 s for the cell after horizontal scrolling).
     * Uses textContent because getText() is empty for cells outside the viewport.
     */
    private String readRowStatus(String rowId) {

        WebElement cell;
        try {
            cell = new WebDriverWait(driver, Duration.ofSeconds(5))
                    .until(ExpectedConditions.presenceOfElementLocated(statusCellLocator(rowId)));
        } catch (TimeoutException e) {
            throw new NoSuchElementException(
                    "Could not find the recordStatus cell for Purchase Order row-id: " + rowId
                            + ". Check the horizontal scroll direction.");
        }

        String text = cell.getAttribute("textContent");
        if (text == null || text.isBlank()) {
            text = cell.getText();
        }
        if (text == null || text.isBlank()) {
            throw new IllegalStateException("Purchase Order status is empty for row-id: " + rowId);
        }
        return text.trim();
    }

    private boolean isPagerButtonEnabled(By button) {

        WebElement element = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(button));
        return element.isEnabled() && element.getAttribute("disabled") == null;
    }

    /**
     * Clicks "Previous"/"Next" and waits until the grid really shows the new page:
     * the summary's first record changed and the rendered rows match it.
     * Replaces the fixed 500 ms sleeps.
     */
    private void clickPagerAndWait(By button) {

        int fromBefore = getPurchaseOrdersPaginationRange()[0];

        clickWhenReady(button);

        waitForPaginationRange(r -> r[0] != fromBefore);
        try {
            waitForRowsToMatchSummary();
        } catch (TimeoutException e) {
            // QC, 2026-10-29: page 6 (records 51-60 / 301) rendered only 8 rows
            // (#51-#58) although the summary said 10, so this wait can never be
            // met there. The page has changed (checked above); go on with the
            // rows that are shown and report the mismatch.
            int[] r = getPurchaseOrdersPaginationRange();
            System.out.println("WARNING: page " + r[0] + "-" + r[1] + " shows "
                    + getVisiblePurchaseOrderRowCount() + " rows, summary says "
                    + (r[1] - r[0] + 1) + ". Continuing with the rows shown.");
        }
    }


    /**
     * Clicks the Delete button for the currently opened Purchase Order.
     *
     * Waits until the Delete button is clickable, then clicks it to initiate
     * the deletion process. If the application displays a confirmation dialog,
     * that dialog must be handled separately.
     */
    public void deletePurchaseOrder() {

        // Waits until the Delete button is clickable before interacting with it.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Clicks the Delete button of the currently opened Purchase Order.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        deletePurchaseOrderButton
                )
        ).click();

        System.out.println(
                "Delete button clicked for the currently opened Purchase Order."
        );
    }


    /**
     * Confirms deletion of the currently selected Purchase Order.
     *
     * Waits until the confirmation button in the deletion dialog is clickable,
     * then clicks it to submit the deletion request.
     */
    public void confirmDeletePurchaseOrder() {

        // Waits until the Delete confirmation button is clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Clicks the confirmation button to submit the deletion request.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmDeletePurchaseOrderButton
                )
        ).click();

        System.out.println(
                "Purchase Order deletion confirmation submitted."
        );
    }


    /**
     * Clicks the approval button for the currently opened Draft Purchase Order.
     *
     * Waits until the approval button is visible and clickable before clicking.
     * Uses the button ID and action type to locate the correct action.
     *
     * @throws org.openqa.selenium.TimeoutException if the approval button
     *         does not become visible and clickable within 15 seconds.
     */
    public void approveDraftPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        // Wait until the approval button is visible in the opened Purchase Order.
        WebElement approveButton = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        approvePurchaseOrderButton
                )
        );

        // Wait until the same approval button becomes clickable.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        approvePurchaseOrderButton
                )
        );

        // Click the approval button to open the confirmation dialog.
        approveButton.click();

        System.out.println(
                "Approval button clicked for the Draft Purchase Order."
        );
    }


    /**
     * Confirms approval of the currently opened Draft Purchase Order.
     *
     * Waits until the confirmation button is clickable, then clicks it
     * to submit the approval request.
     */
    public void confirmApproveDraftPurchaseOrder() {

        // Waits until the approval confirmation button is clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Clicks the confirmation button to submit the approval request.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmApprovePurchaseOrderButton
                )
        ).click();

        System.out.println(
                "Approval confirmation submitted for the Draft Purchase Order."
        );
    }


    /** Approved status text as shown in the "الحالة" column. */
    private static final String APPROVED_STATUS = "معتمد";

    /**
     * Opens a random Approved Purchase Order, navigating as little as possible.
     * Same search as {@link #selectRandomDraftPurchaseOrder()}: current page
     * first, then backwards, then forwards; stops at the first page that has
     * an Approved order.
     *
     * @throws InterruptedException if a wait is interrupted.
     * @throws NoSuchElementException if no page has an Approved Purchase Order.
     */
    public void selectRandomApprovedPurchaseOrder()
            throws InterruptedException {

        selectRandomPurchaseOrderWithStatus(APPROVED_STATUS);
    }

    /**
     * Clicks the Print button for the currently opened Purchase Order.
     *
     * Waits until the button is visible and clickable before clicking it.
     * The button may open a print-options menu rather than printing directly.
     */
    public void clickPrintPurchaseOrderButton() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Waits until the Purchase Order Print button becomes visible.
        WebElement printButton =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                printPurchaseOrderButton
                        )
                );

        // Waits until the Print button is enabled and clickable.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        printPurchaseOrderButton
                )
        );

        // Opens the Print menu for the current Purchase Order.
        printButton.click();

        System.out.println(
                "Purchase Order Print button clicked successfully."
        );
    }

    /**
     * Randomly selects one available Purchase Order print template.
     *
     * Collects all available template radio buttons, chooses one randomly,
     * clicks it if it is not already selected, and verifies that the selected
     * template has aria-checked="true".
     *
     * @return the data-testid of the randomly selected print template
     */
    public String selectRandomPurchaseOrderPrintTemplate() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        // Waits until the print-template radio group is displayed.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(printTemplatesGroup)
        );

        // Retrieves all available print-template radio buttons.
        List<WebElement> templates =
                wait.until(
                        ExpectedConditions.visibilityOfAllElementsLocatedBy(
                                printTemplateOptions
                        )
                );

        // Fails clearly if no print templates are available.
        if (templates.isEmpty()) {
            throw new NoSuchElementException(
                    "No Purchase Order print templates were found."
            );
        }

        // Selects a random template from the available options.
        int randomIndex = random.nextInt(templates.size());

        // Retrieves the selected template's stable test ID.
        String selectedTestId =
                templates.get(randomIndex).getAttribute("data-testid");

        if (selectedTestId == null || selectedTestId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "The selected print template has no data-testid."
            );
        }

        // Re-locates the selected template to avoid using a stale WebElement.
        By selectedTemplateLocator =
                By.cssSelector(
                        "button[role='radio'][data-testid='" +
                                selectedTestId + "']"
                );

        WebElement selectedTemplate =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedTemplateLocator
                        )
                );

        // Clicks the template only if it is not already selected.
        if (!"true".equals(
                selectedTemplate.getAttribute("aria-checked"))) {

            selectedTemplate.click();
        }

        // Waits until the selected template is confirmed as checked.
        wait.until(driver ->
                "true".equals(
                        driver.findElement(selectedTemplateLocator)
                                .getAttribute("aria-checked")
                )
        );

        System.out.println(
                "Random Purchase Order print template selected: " +
                        selectedTestId
        );

        // Returns the selected template ID for logging or further test steps.
        return selectedTestId;
    }

    /**
     * Confirms printing the currently opened Purchase Order.
     *
     * Waits until the confirmation button is visible and clickable,
     * clicks it, and logs the action.
     */
    public void confirmPrintPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Waits until the final Print confirmation button is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        confirmPrintPurchaseOrderButton
                )
        );

        // Waits until the confirmation button is clickable.
        WebElement confirmPrintButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                confirmPrintPurchaseOrderButton
                        )
                );

        // Confirms the print action for the Purchase Order.
        confirmPrintButton.click();

        System.out.println(
                "Purchase Order print confirmation button clicked."
        );
    }

    /**
     * Clicks the Save as PDF button in the Purchase Order print preview.
     *
     * Waits until the button is visible and clickable before clicking it.
     */
    public void savePurchaseOrderAsPdf() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        // Waits until the Save as PDF button is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        savePurchaseOrderAsPdfButton
                )
        );

        // Waits until the button is clickable.
        WebElement savePdfButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                savePurchaseOrderAsPdfButton
                        )
                );

        // Clicks Save as PDF in the print preview.
        savePdfButton.click();

        System.out.println(
                "Save as PDF button clicked for the Purchase Order."
        );
    }

    /**
     * Clicks the final Print button in the Purchase Order print preview.
     *
     * Waits until the button is visible and clickable before clicking it.
     */
    public void clickFinalPrintPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement printButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        finalPrintPurchaseOrderButton
                )
        );

        printButton.click();

        System.out.println(
                "Final Print button clicked for the Purchase Order."
        );
    }

    public void clickChromePrintButton() throws AWTException {

        Robot robot = new Robot();

        // Give Chrome Print Preview time to load
        robot.delay(3000);

        // Press Enter to activate the selected Print button
        robot.keyPress(KeyEvent.VK_ENTER);
        robot.keyRelease(KeyEvent.VK_ENTER);

        System.out.println(
                "Chrome Print button activated"
        );
    }

    /**
     * Clicks the Cancel Purchase Order button.
     *
     * Waits until the button is clickable before clicking it.
     * This method only initiates the cancellation action;
     * any confirmation dialog must be handled separately if displayed.
     */
    public void cancelPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement cancelButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        cancelPurchaseOrderButton
                )
        );

        cancelButton.click();

        System.out.println(
                "Cancel Purchase Order button clicked."
        );
    }

    /**
     * Confirms the cancellation of the currently opened Purchase Order.
     *
     * Waits until the confirmation button is clickable, then clicks it.
     */
    public void confirmCancelPurchaseOrder() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmCancelPurchaseOrderButton
                )
        );

        confirmButton.click();

        System.out.println(
                "Purchase Order cancellation confirmed."
        );
    }

    /**
     * Clicks the Attachments button to open the attachment interface
     * for the currently opened Purchase Order.
     *
     * Waits until the button is clickable before clicking it.
     */
    public void clickAttachments() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        attachmentsButton
                )
        );

        button.click();

        System.out.println(
                "Attachments button clicked successfully."
        );
    }

    /**
     * Clicks the Add Attachment button to submit the attachment form.
     * Waits until the button is clickable before clicking it.
     */
    public void clickAddAttachment() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        addAttachmentButton
                )
        );

        button.click();

        System.out.println(
                "Add Attachment button clicked successfully."
        );
    }

    public void uploadAllAttachments() {

        By fileInput = By.cssSelector(
                "input[type='file']"
        );

        Path attachmentsFolder = Paths.get(
                System.getProperty("user.dir"),
                "test-data",
                "attachments"
        );

        try (Stream<Path> files = Files.list(attachmentsFolder)) {

            List<String> filePaths = files
                    .filter(Files::isRegularFile)
                    .map(Path::toAbsolutePath)
                    .map(Path::toString)
                    .toList();

            if (filePaths.isEmpty()) {
                throw new RuntimeException(
                        "No attachment files found in: " +
                                attachmentsFolder.toAbsolutePath()
                );
            }

            String allFiles = String.join(
                    System.lineSeparator(),
                    filePaths
            );

            driver.findElement(fileInput).sendKeys(allFiles);

            System.out.println(
                    "All attachments uploaded successfully: " +
                            filePaths.size()
            );

        } catch (IOException e) {

            throw new RuntimeException(
                    "Failed to read attachments folder",
                    e
            );
        }
    }

    /**
     * Enters the specified title into the Attachment Title field.
     *
     * Waits until the field is visible, clears any existing value,
     * and enters the provided attachment title.
     *
     * @param title the title to assign to the attachment
     */
    public void enterAttachmentTitle(String title) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement titleInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        attachmentTitleInput
                )
        );

        titleInput.clear();
        titleInput.sendKeys(title);

        System.out.println(
                "Attachment title entered: " + title
        );
    }

    /**
     * Enters the specified description into the Attachment Description field.
     *
     * Waits until the field is visible, clears any existing value,
     * and enters the provided description.
     *
     * @param description the description to assign to the attachment
     */
    public void enterAttachmentDescription(String description) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement descriptionInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        attachmentDescriptionInput
                )
        );

        descriptionInput.clear();
        descriptionInput.sendKeys(description);

        System.out.println(
                "Attachment description entered: " + description
        );
    }

    /**
     * Confirms adding the attachment after entering its title,
     * description, and selecting the attachment file.
     *
     * Waits until the confirmation button is clickable before clicking it.
     */
    public void clickAddAttachmentConfirm() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        addAttachmentConfirmButton
                )
        );

        confirmButton.click();

        System.out.println(
                "Attachment addition confirmed successfully."
        );
    }

    /**
     * Clicks the Undo button to reverse the most recent supported action
     * on the currently opened Purchase Order page.
     *
     * Waits until the button is clickable before clicking it.
     */
    public void undo() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(undoButton)
        );

        button.click();

        System.out.println("Undo button clicked successfully.");
    }

    /**
     * Confirms the Undo action in the confirmation dialog.
     * Waits until the confirmation button is clickable before clicking it.
     */
    public void confirmUndo() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(confirmUndoButton)
        );

        confirmButton.click();

        System.out.println("Undo action confirmed successfully.");
    }

    /**
     * Clicks the clipboard actions menu button in the Purchase Order grid.
     *
     * Waits for the button to become clickable, clicks it, and checks whether
     * its data-state changes to "open". If the state does not change, prints
     * the current state and visibility details for troubleshooting.
     */
    public void clickClipboardBarButton() {

        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(10));

        // Waits until the clipboard actions button is clickable.
        WebElement button = wait.until(
                ExpectedConditions.elementToBeClickable(
                        clipboardBarButtonLocator));

        // Clicks the clipboard actions button.
        button.click();

        // Waits briefly for the button state to reflect the menu interaction.
        try {
            wait.until(d -> {
                try {
                    WebElement currentButton =
                            d.findElement(clipboardBarButtonLocator);

                    return "open".equals(
                            currentButton.getAttribute("data-state"));

                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });

            System.out.println(
                    "Clipboard actions button state changed to open.");

        } catch (TimeoutException e) {

            // Reads the current state to diagnose whether the menu interaction
            // changed the button state or left it unchanged.
            WebElement currentButton =
                    driver.findElement(clipboardBarButtonLocator);

            System.out.println(
                    "Clipboard button was clicked, but data-state did not " +
                            "become 'open'. Current data-state: " +
                            currentButton.getAttribute("data-state"));

            // Do not fail solely because the button's state attribute did not change.
        }
    }

    /**
     * Clicks the "طلب الشراء" option in the clipboard actions menu.
     *
     * Waits until the menu item is clickable, clicks the nested button,
     * and allows Selenium to wait for the click action to complete.
     */
    public void clickPurchaseRequestMenuItem() {

        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(10));

        // Waits until the "طلب الشراء" menu item is visible and clickable.
        WebElement menuItem = wait.until(
                ExpectedConditions.elementToBeClickable(
                        purchaseRequestMenuItemLocator));

        // Clicks the nested button to activate the Purchase Request option.
        menuItem.findElement(
                By.xpath(".//button[normalize-space()='طلب الشراء']")
        ).click();

        System.out.println(
                "The 'طلب الشراء' menu option was clicked successfully.");
    }

    /**
     * Clicks the modal confirmation button labeled "موافق".
     *
     * Waits until the button is clickable, clicks it, and waits until
     * the button is no longer visible or becomes stale after the modal closes.
     */
    public void clickConfirmModalButton() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(confirmModalButtonLocator)
        );

        confirmButton.click();

        wait.until(ExpectedConditions.or(
                ExpectedConditions.invisibilityOfElementLocated(
                        confirmModalButtonLocator
                ),
                ExpectedConditions.stalenessOf(confirmButton)
        ));

        System.out.println("The modal confirmation button 'موافق' was clicked successfully.");
    }

    /**
     * Clicks the "متابعة من عروض الاسعار" option in the menu.
     *
     * Waits until the nested button is clickable, then clicks it
     * to continue the current workflow from quotations.
     */
    public void clickContinueFromQuotationsMenuItem() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement menuItem = wait.until(
                ExpectedConditions.elementToBeClickable(
                        continueFromQuotationsMenuItemLocator
                )
        );

        WebElement continueButton = menuItem.findElement(
                By.xpath(".//button[normalize-space()='متابعة من عروض الاسعار']")
        );

        continueButton.click();

        System.out.println(
                "Clicked the 'متابعة من عروض الاسعار' menu option successfully."
        );
    }

    /**
     * Clicks the "الاستيراد من Excel" option in the menu.
     *
     * Waits until the menu option is clickable, then clicks its nested button
     * to open the Excel import workflow.
     */
    public void clickImportFromExcelMenuItem() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(2));

        WebElement menuItem = wait.until(
                ExpectedConditions.elementToBeClickable(
                        importFromExcelMenuItemLocator
                )
        );

        WebElement importButton = menuItem.findElement(
                By.xpath(".//button[normalize-space()='الاستيراد من Excel']")
        );

        importButton.click();

        System.out.println(
                "Clicked the 'الاستيراد من Excel' menu option successfully."
        );
    }

    /**
     * Uploads the PurchaseOrder.xlsx test file from the project's
     * test-data/import-excel directory.
     *
     * Resolves the Excel file path relative to the project root, waits
     * for the file input to be present in the DOM, and uploads the file
     * using Selenium's sendKeys() method.
     *
     * @throws IllegalArgumentException if the Excel file does not exist
     */
    public void uploadExcelFile() {

        Path excelFilePath = Paths.get(
                System.getProperty("user.dir"),
                "test-data",
                "import-excel",
                "PurchaseOrder.xlsx"
        ).toAbsolutePath().normalize();

        if (!Files.isRegularFile(excelFilePath)) {
            throw new IllegalArgumentException(
                    "Excel test file was not found: " + excelFilePath
            );
        }

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement fileInput = wait.until(
                ExpectedConditions.presenceOfElementLocated(excelFileInputLocator)
        );

        fileInput.sendKeys(excelFilePath.toString());

        System.out.println(
                "Excel file uploaded successfully: " + excelFilePath
        );
    }

    /**
     * Clicks the "الاستيراد من Excel" button to submit the selected Excel file.
     *
     * Waits until the button is clickable, then clicks it to start
     * the Excel import operation.
     */
    public void clickImportExcelConfirmButton() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement importButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        importExcelConfirmButtonLocator
                )
        );

        // Verify that the located confirmation button has the expected label.
        if (!importButton.getText().trim().equals("الاستيراد من Excel")) {
            throw new IllegalStateException(
                    "The located button is not the expected Excel import button."
            );
        }

        importButton.click();

        System.out.println(
                "Clicked the 'الاستيراد من Excel' confirmation button."
        );

        waitForExcelImportToFinish();
    }

    /** Upper bound for the Excel import (the button shows "جاري التحميل" meanwhile). */
    private static final Duration EXCEL_IMPORT_TIMEOUT = Duration.ofSeconds(120);

    /**
     * Waits until the "الاستيراد من Excel" dialog closes, i.e. the import
     * finished and the lines were added to the grid.
     *
     * <p>The import is not instant: the server looks up every item code of the
     * file. With 37 lines (QC, 2026-10-27) the dialog was still showing
     * "جاري التحميل..." when the test went on, and its overlay blocked the next
     * click. Fails with the dialog text if it never closes (e.g. the file was
     * rejected).</p>
     */
    public void waitForExcelImportToFinish() {

        By openImportDialog = By.xpath(
                "//*[@role='dialog' and @data-state='open']"
                        + "[.//*[normalize-space()='الاستيراد من Excel']]");

        try {
            new WebDriverWait(driver, EXCEL_IMPORT_TIMEOUT)
                    .pollingEvery(Duration.ofMillis(500))
                    .until(d -> d.findElements(openImportDialog).isEmpty());
        } catch (TimeoutException e) {
            List<WebElement> dialogs = driver.findElements(openImportDialog);
            String text = dialogs.isEmpty() ? "" : dialogs.get(0).getText().replace('\n', ' ');
            throw new TimeoutException(
                    "Excel import did not finish within " + EXCEL_IMPORT_TIMEOUT.toSeconds()
                            + " s. Dialog still open: [" + text + "]", e);
        }

        System.out.println("Excel import finished; dialog closed.");
    }

    /**
     * Clicks the "التصدير الى Excel" option in the actions menu.
     *
     * Waits until the menu item is clickable, locates its nested button,
     * and clicks it to initiate the Excel export workflow.
     */
    public void clickExportToExcelMenuItem() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement menuItem = wait.until(
                ExpectedConditions.elementToBeClickable(
                        exportToExcelMenuItemLocator
                )
        );

        WebElement exportButton = menuItem.findElement(
                By.xpath(".//button[normalize-space()='التصدير الى Excel']")
        );

        exportButton.click();

        System.out.println(
                "Clicked the 'التصدير الى Excel' menu option successfully."
        );
    }

    /**
     * Closes the current Purchase Order dialog or panel.
     *
     * Waits until the close button is clickable, then clicks it
     * to close the current interface.
     */
    public void clickClosePurchaseOrderDialog() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement closeButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        closePurchaseOrderDialogLocator
                )
        );

        closeButton.click();

        System.out.println("Clicked the Purchase Order close button.");
    }

    /**
     * Clicks the "نعم" button to confirm the current modal action.
     *
     * Waits until the confirmation button is clickable, then clicks it
     * to proceed with the requested operation.
     */
    public void clickConfirmYesButton() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(confirmYesButtonLocator)
        );

        confirmButton.click();

        System.out.println("Clicked the 'نعم' confirmation button successfully.");
    }


    // =====================================================================
    // Screen features added for the gap-analysis tests (QC, 2026-10-29).
    // All locators below were read from qc.studioerp.com on that date.
    // =====================================================================

    /** Free-text search box above the Purchase Orders list. */
    private final By listSearchInput =
            By.cssSelector("input[type='search'][placeholder^='بحث']");

    /** Reference-number cells of the rows currently shown in the list. */
    private final By listCodeCells = By.cssSelector(
            ".ag-center-cols-container div[role='row'][row-id] div[role='gridcell'][col-id='code']");

    /** Toolbar "record search" (magnifier) and the input of its drop-down. */
    private final By recordSearchButton = By.id("recordSearch");
    private final By recordSearchInput =
            By.cssSelector("input[placeholder^='ابحث بالاسم']");

    private final By transactionLogsButton = By.id("transactionLogs");
    private final By transactionLogDialog = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='سجل العمليات']]");
    /** Action badge of each log entry, e.g. "انشاء العملية", "اعتماد العملية". */
    private final By transactionLogActionBadges = By.cssSelector("span.rounded-full");

    private final By approvalDateInput = By.id("approvalDate");

    /**
     * Types {@code text} in the list search box and waits until every row shown
     * contains it (or the list is empty).
     */
    public void searchPurchaseOrdersList(String text) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement input = waitForVisibleOne(listSearchInput);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        input.sendKeys(text);

        wait.pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> getVisiblePurchaseOrderCodes().stream().allMatch(c -> c.contains(text)));
    }

    /**
     * Waits (max 15 s) until the list shows an order numbered {@code code}.
     * Returns false instead of throwing, so the test can report what it saw.
     */
    public boolean waitUntilListShows(String code) {

        try {
            new WebDriverWait(driver, Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> getVisiblePurchaseOrderCodes().contains(code));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * Returns the first DISPLAYED, enabled element matching {@code locator}.
     *
     * <p>The list renders the "بحث ..." box twice (one copy hidden, width 0,
     * depending on the layout — seen in QC, 2026-10-29). {@code findElement}
     * may return the hidden copy, so "clickable" never becomes true.</p>
     */
    private WebElement waitForVisibleOne(By locator) {

        return new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(300))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElements(locator).stream()
                        .filter(e -> e.isDisplayed() && e.isEnabled())
                        .findFirst()
                        .orElse(null));
    }

    /** Clears the list search box. */
    public void clearPurchaseOrdersListSearch() {

        WebElement input = waitForVisibleOne(listSearchInput);
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
    }

    /** Reference numbers of the rows currently rendered in the list. */
    public List<String> getVisiblePurchaseOrderCodes() {

        return driver.findElements(listCodeCells).stream()
                .map(cell -> cell.getText().trim())
                .filter(code -> !code.isEmpty())
                .collect(Collectors.toList());
    }

    /**
     * Opens an order from the toolbar record search: magnifier → type the code
     * → click the matching result. Waits until the URL changes.
     */
    public void openPurchaseOrderFromRecordSearch(String code) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        String urlBefore = driver.getCurrentUrl();

        wait.until(ExpectedConditions.elementToBeClickable(recordSearchButton)).click();
        WebElement input = waitForVisibleOne(recordSearchInput);
        input.sendKeys(code);

        By result = By.xpath("//button[span[1][normalize-space()='" + code + "']]");
        wait.ignoring(StaleElementReferenceException.class)
                .until(ExpectedConditions.elementToBeClickable(result)).click();

        if (!urlBefore.contains("/view/")) {
            wait.until(ExpectedConditions.urlContains("/view/"));
        } else {
            wait.until(d -> !d.getCurrentUrl().equals(urlBefore));
        }
    }

    /**
     * Record navigation inside the opened order.
     * {@code buttonId}: pageFirst, pagePrevious10, pageBack, pageNext, pageNext10, pageLast.
     */
    public void clickRecordNavigation(String buttonId) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        String urlBefore = driver.getCurrentUrl();
        wait.until(ExpectedConditions.elementToBeClickable(By.id(buttonId))).click();
        wait.until(d -> !d.getCurrentUrl().equals(urlBefore));
    }

    public boolean isRecordNavigationEnabled(String buttonId) {
        return driver.findElement(By.id(buttonId)).isEnabled();
    }

    /**
     * Opens "سجل العمليات". The button sits at the far end of the scrollable
     * toolbar and only reacts to a real click, so it is scrolled into view first.
     */
    public void openTransactionLog() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));
        WebElement button = wait.until(ExpectedConditions.presenceOfElementLocated(transactionLogsButton));
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({inline:'center', block:'nearest'});", button);
        wait.until(ExpectedConditions.elementToBeClickable(transactionLogsButton)).click();
        wait.until(ExpectedConditions.visibilityOfElementLocated(transactionLogDialog));
    }

    /** Action names of the log entries, oldest first (as displayed). */
    public List<String> getTransactionLogActions() {

        // The dialog opens first and its entries load afterwards (QC, 2026-10-29:
        // read right after opening → empty list). Wait until entries appear;
        // return an empty list only if none arrive within 10 s.
        try {
            return new WebDriverWait(driver, Duration.ofSeconds(10))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> {
                        List<String> actions = d.findElement(transactionLogDialog)
                                .findElements(transactionLogActionBadges).stream()
                                .map(e -> e.getText().trim())
                                .filter(t -> !t.isEmpty())
                                .collect(Collectors.toList());
                        return actions.isEmpty() ? null : actions;
                    });
        } catch (TimeoutException e) {
            return new ArrayList<>();
        }
    }

    /** Closes "سجل العمليات" with its "خروج" button. */
    public void closeTransactionLog() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(transactionLogDialog));
        dialog.findElement(By.xpath(".//button[normalize-space()='خروج']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(transactionLogDialog));
    }

    /**
     * Opens the "⋮" menu of the form and tells whether {@code label} is enabled.
     * The menu is closed again before returning.
     */
    public boolean isFormMenuItemEnabled(String label) {

        openFormMenu();
        By item = By.xpath("//*[@role='menu']//button[normalize-space()='" + label + "']");
        boolean enabled = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(item))
                .isEnabled();
        new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        return enabled;
    }

    private final By openFormMenu = By.cssSelector("[role='menu'][data-state='open']");

    /**
     * Opens the "⋮" menu of the form and waits until it is really open.
     *
     * <p>Right after an order opens, the first click on "⋮" can be lost while the
     * toolbar re-renders (QC, 2026-10-29: Draft row-id 16, no menu appeared for
     * 10 s, although a later manual click opened it). So the click is retried,
     * up to 3 times, until a {@code [role=menu][data-state=open]} appears.
     * Note: {@code data-state} on the button itself is the TOOLTIP state and
     * stays "closed", so it cannot tell whether the menu opened.</p>
     */
    private void openFormMenu() {

        for (int attempt = 1; attempt <= 3; attempt++) {

            new WebDriverWait(driver, Duration.ofSeconds(10))
                    .until(ExpectedConditions.elementToBeClickable(clipboardBarButtonLocator))
                    .click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.visibilityOfElementLocated(openFormMenu));
                return;
            } catch (TimeoutException e) {
                System.out.println("\"⋮\" menu did not open (attempt " + attempt + "/3) → retrying.");
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            }
        }
        throw new TimeoutException("The \"⋮\" menu of the Purchase Order did not open after 3 clicks.");
    }

    /** "⋮" → "انشاء امر استلام". Opens the Receipt Order screen. */
    public void clickCreateReceiptOrderMenuItem() {

        By item = By.xpath("//*[@role='menu']//button[normalize-space()='انشاء امر استلام']");

        openFormMenu();
        WebElement button = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(item));
        waitForAnimationsToFinish(By.cssSelector("[role='menu'][data-state='open']"));

        clickAndFollowToScreen(
                () -> new Actions(driver).moveToElement(button).pause(Duration.ofMillis(200)).click().perform(),
                "/inventory/recieve-orders/add/");
    }

    /**
     * Clicks something that opens another screen, then follows it.
     *
     * <p>In the test browser these follow-up screens open in a NEW tab, while
     * the order tab stays on /view/{id} (QC, 2026-10-29: "انشاء امر استلام"
     * added a window on every click, 1→2→3→4; "$" Items Pricing behaved the
     * same). In a normal browser they open in the same tab. This waits for
     * either case, switches to the new tab when there is one, and waits until
     * its URL contains {@code expectedPath}.</p>
     */
    private void clickAndFollowToScreen(Runnable click, String expectedPath) {

        Set<String> windowsBefore = driver.getWindowHandles();
        click.run();

        String target = new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> {
                    if (d.getCurrentUrl().contains(expectedPath)) {
                        return "same-tab";
                    }
                    for (String handle : d.getWindowHandles()) {
                        if (!windowsBefore.contains(handle)) {
                            return handle;
                        }
                    }
                    return null;
                });

        if (!"same-tab".equals(target)) {
            driver.switchTo().window(target);
            System.out.println("Screen opened in a new tab; switched to it.");
        }

        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains(expectedPath));
        System.out.println("Opened: " + driver.getCurrentUrl());
    }

    /** Toolbar "$" — opens Sales → Pricing pre-filled from this order. */
    public void clickItemsPricing() {

        WebElement button = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("itemsPricing")));
        clickAndFollowToScreen(button::click, "/sales/pricing/add");
    }

    /** Toolbar barcode button — opens Inventory → Item Barcodes for this order. */
    public void clickPrintBarcode() {

        WebElement button = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.id("printBarcode")));
        clickAndFollowToScreen(button::click, "/inventory/item-barcodes");
    }

    /** "تاريخ الاعتماد" of the opened order (yyyy-MM-dd, empty when not approved). */
    public String getApprovalDate() {

        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.presenceOfElementLocated(approvalDateInput))
                .getAttribute("value").trim();
    }


    // =====================================================================
    // Order lines — read / delete / tabs (level-1 tests, QC 2026-10-29)
    // =====================================================================

    /** Row index of the item line added by the last {@link #clickAddLine()}. */
    public int getNewLineRowIndex() {
        return newRowIndex;
    }

    /**
     * Shows a lines tab: "details" (items) or "discountsAndAdditions".
     * Only the active tab's grid is rendered, so grid reads apply to it.
     */
    public void openLinesTab(String tab) {

        By trigger = By.cssSelector("[role='tab'][id$='-trigger-" + tab + "']");
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        wait.until(ExpectedConditions.elementToBeClickable(trigger)).click();
        wait.ignoring(StaleElementReferenceException.class)
                .until(d -> "active".equals(d.findElement(trigger).getAttribute("data-state")));
    }

    /**
     * Text of a line cell, by column id and row index
     * ({@code data-testid="orders-<colId>-<row>"}). Scrolls the grid until
     * the row is rendered (AG Grid only renders rows near the viewport).
     */
    public String getLineText(String colId, int rowIndex) {

        By cell = By.cssSelector("[data-testid='orders-" + colId + "-" + rowIndex + "']");
        scrollGridToRow(rowIndex, cell);
        return new WebDriverWait(driver, Duration.ofSeconds(10))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElement(cell).getText().trim());
    }

    /**
     * Deletes a line with the trash button of its "إجراءات" cell
     * ({@code data-testid="orders-actions-<row>"}). Edit mode only.
     * QC, 2026-10-29: the line is removed at once (no confirmation) and the
     * serial numbers are renumbered; nothing is saved until "حفظ".
     */
    public void deleteLine(int rowIndex) {

        By trash = By.cssSelector("[data-testid='orders-actions-" + rowIndex + "'] button");
        scrollGridToRow(rowIndex, trash);
        WebElement button = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(trash));
        new Actions(driver).moveToElement(button).click().perform();
        System.out.println("Delete clicked on line row-index " + rowIndex + ".");
    }

    /** Reloads the opened order from the server (proves values were persisted). */
    public void reloadOrder() {

        driver.navigate().refresh();
        new WebDriverWait(driver, Duration.ofSeconds(30))
                .until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector(".ag-center-cols-container div[role='row']")));
    }

    /** Scrolls the active grid until {@code target} (inside row {@code rowIndex}) is in the DOM. */
    private void scrollGridToRow(int rowIndex, By target) {

        JavascriptExecutor js = (JavascriptExecutor) driver;
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    if (!d.findElements(target).isEmpty()) {
                        return true;
                    }
                    js.executeScript(
                            "const vp=document.querySelector('.ag-body-viewport');"
                                    + "const r=document.querySelector('.ag-center-cols-container [role=row]');"
                                    + "if(vp&&r){vp.scrollTop=Math.max(0,arguments[0]*r.offsetHeight-vp.clientHeight/2);}",
                            rowIndex);
                    return false;
                });
    }

    // =====================================================================
    // Attachments — count / add one file / delete (level-1 tests, QC 2026-10-31)
    //
    // Observed on QC: the toolbar "uploadAttachment" button opens
    // "تفاصيل المرفق". With no attachment it shows "لا يوجد ملف محدد"; otherwise
    // a preview and a grid (#, مسار الملف, الوصف, نوع الملف, تسلسل الملف,
    // إجراءات). "إضافة مرفق" is shown only in add/edit mode. "اضافة" uploads the
    // file at once (toast "تمت إضافة الملفات بنجاح"). The row's ⚙ menu has
    // عرض / حذف / فتح موقع الملف; "حذف" removes the row at once, no confirmation.
    // =====================================================================

    private final By attachmentDetailsDialog = By.xpath(
            "//*[@role='dialog' and @data-state='open'][.//h2[normalize-space()='تفاصيل المرفق']]");

    private static final String NO_ATTACHMENT_TEXT = "لا يوجد ملف محدد";

    /** Number of attachments listed in the open "تفاصيل المرفق" dialog. */
    public int getAttachmentCount() {

        WebElement dialog = new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.visibilityOfElementLocated(attachmentDetailsDialog));
        if (dialog.getText().contains(NO_ATTACHMENT_TEXT)) {
            return 0;
        }
        return dialog.findElements(By.cssSelector(".ag-center-cols-container div[role='row']")).size();
    }

    /** Waits (max 15 s) until the dialog lists {@code expected} attachments; returns the last count. */
    public int waitForAttachmentCount(int expected) {

        try {
            return new WebDriverWait(driver, Duration.ofSeconds(15))
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(StaleElementReferenceException.class)
                    .until(d -> getAttachmentCount() == expected ? expected : null);
        } catch (TimeoutException e) {
            return getAttachmentCount();
        }
    }

    /** Number of files in test-data/attachments (what {@link #uploadAllAttachments()} uploads). */
    public int countAttachmentFiles() {

        Path folder = Paths.get(System.getProperty("user.dir"), "test-data", "attachments");
        try (Stream<Path> files = Files.list(folder)) {
            return (int) files.filter(Files::isRegularFile).count();
        } catch (IOException e) {
            throw new RuntimeException("Failed to read attachments folder " + folder, e);
        }
    }

    /**
     * Deletes the last attachment of the open "تفاصيل المرفق" dialog:
     * its ⚙ actions menu → "حذف".
     */
    public void deleteLastAttachment() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(attachmentDetailsDialog));
        List<WebElement> menus = dialog.findElements(
                By.cssSelector(".ag-center-cols-container [col-id='actions'] button[aria-haspopup='menu']"));
        if (menus.isEmpty()) {
            throw new IllegalStateException("No attachment to delete in 'تفاصيل المرفق'.");
        }
        new Actions(driver).moveToElement(menus.get(menus.size() - 1)).click().perform();

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
                "//*[@role='menu' and @data-state='open']//*[@role='menuitem'][normalize-space()='حذف']")))
                .click();
        System.out.println("Attachment deleted from 'تفاصيل المرفق'.");
    }

    /** Closes "تفاصيل المرفق" with its "إلغاء" button. */
    public void closeAttachmentsDialog() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        WebElement dialog = wait.until(ExpectedConditions.visibilityOfElementLocated(attachmentDetailsDialog));
        dialog.findElement(By.xpath(".//button[normalize-space()='إلغاء']")).click();
        wait.until(ExpectedConditions.invisibilityOfElementLocated(attachmentDetailsDialog));
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForToastToAppearAndClear(String text) {
        ui.waitForToastToAppearAndClear(text);
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForToastsToClear() {
        ui.waitForToastsToClear();
    }

    /** Header fields read by {@link #readHeaderValues()} (data-testid without "orders-"). */
    public static final List<String> HEADER_FIELDS = List.of(
            "operationTypeCategoryId", "vendorId", "branchId", "warehouseId",
            "currencyId", "convertionRate", "costCenterId", "taxCalculationMethode",
            "orderDate", "quotationtDeadline", "purchaseRepresentativeId", "prices",
            "vendorInvoiceCode", "itemsCostsMethode", "bulkDiscountType",
            "bulkDiscountAmount", "tagId");

    /** Delegates to {@link com.erp.automation.ui.DocumentHeader} (shared by every screen). */
    public Map<String, String> readHeaderValues() {
        return header.readHeaderValues(HEADER_FIELDS);
    }

    /** Delegates to {@link com.erp.automation.ui.ApiRecorder} (shared by every screen). */
    public void startRecordingApiCalls() {
        apiRecorder.startRecordingApiCalls();
    }

    /** Delegates to {@link com.erp.automation.ui.ApiRecorder} (shared by every screen). */
    public List<String> getRecordedApiCalls() {
        return apiRecorder.getRecordedApiCalls();
    }

    // =====================================================================
    // Print preview and downloaded files (QC, 2026-10-31)
    // =====================================================================

    private final By printPreviewOverlay = By.cssSelector(".pd-overlay");

    /**
     * Waits for the print preview (".pd-overlay", opened after "طباعة" →
     * template → confirm) and returns its text: company header, order number,
     * date, supplier, branch, warehouse, note and the lines.
     */
    public String getPrintPreviewText() {

        WebElement overlay = new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.visibilityOfElementLocated(printPreviewOverlay));
        // The page content is rendered after the toolbar.
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .ignoring(StaleElementReferenceException.class)
                .until(d -> d.findElement(printPreviewOverlay).getText().contains("الرقم المرجعي"));
        return driver.findElement(printPreviewOverlay).getText();
    }

    /** Closes the print preview with its ✕ button. */
    public void closePrintPreview() {

        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.elementToBeClickable(By.cssSelector("button.pd-btn.pd-btn-close")))
                .click();
        new WebDriverWait(driver, Duration.ofSeconds(10))
                .until(ExpectedConditions.invisibilityOfElementLocated(printPreviewOverlay));
    }

    /** Delegates to {@link com.erp.automation.ui.Downloads} (shared by every screen). */
    public java.nio.file.Path prepareDownloadFolder(String name) {
        return downloads.prepareDownloadFolder(name);
    }

    /** Delegates to {@link com.erp.automation.ui.Downloads} (shared by every screen). */
    public java.nio.file.Path waitForDownloadedFile(java.nio.file.Path dir, String extension, Duration timeout) {
        return downloads.waitForDownloadedFile(dir, extension, timeout);
    }

    // =====================================================================
    // List views: Kanban, group by, summaries (QC, 2026-10-31)
    // =====================================================================
    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public void switchToKanbanView() {
        list.switchToKanbanView();
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public void switchToListView() {
        list.switchToListView();
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public List<String> getKanbanCardCodes() {
        return list.getKanbanCardCodes();
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public String getKanbanCardStatus(String code) {
        return list.getKanbanCardStatus(code);
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public void openKanbanCard(String code) {
        list.openKanbanCard(code);
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public void groupListBy(String field) {
        list.groupListBy(field);
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public Map<String, Integer> getGroupCounts() {
        return list.getGroupCounts();
    }

    /** Delegates to {@link com.erp.automation.ui.DocumentList} (shared by every screen). */
    public Map<String, Integer> getSummaryCards() {
        return list.getSummaryCards();
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public List<String> getGridColumnTexts(String colId) {
        return ui.getGridColumnTexts(colId);
    }

    // =====================================================================
    // Explicit waits that replace fixed sleeps (item 2, QC 2026-10-31)
    // =====================================================================

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForStableText(By locator, Duration stableFor, Duration timeout) {
        ui.waitForStableText(locator, stableFor, timeout);
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForAnimationsToFinish(By locator) {
        ui.waitForAnimationsToFinish(locator);
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForOpenDialogReady() {
        ui.waitForOpenDialogReady();
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public boolean waitForNoCellEditor(Duration timeout) {
        return ui.waitForNoCellEditor(timeout);
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void waitForGridToSettle() {
        ui.waitForGridToSettle();
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    private void waitForGridColumnsToRender() {
        ui.waitForGridColumnsToRender();
    }



    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    private static void pollPause() {
        com.erp.automation.ui.UiWaits.pollPause();
    }

    /**
     * "طباعة" → waits for the print-template choices (radio buttons
     * orders-template1..n). They are NOT inside a role=dialog (QC, 2026-10-31),
     * so a dialog wait cannot be used. One retry if the first click is lost.
     */
    public void openPrintTemplateChooser() {

        for (int attempt = 1; attempt <= 2; attempt++) {
            clickPrintPurchaseOrderButton();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(5))
                        .until(ExpectedConditions.visibilityOfElementLocated(printTemplateOptions));
                waitForAnimationsToFinish(By.cssSelector("[data-testid^='orders-template']"));
                return;
            } catch (TimeoutException e) {
                System.out.println("Print templates did not appear (attempt " + attempt + "/2).");
            }
        }
        throw new TimeoutException("The print templates did not appear after clicking \"طباعة\" twice.");
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public void clickWhenReady(By locator) {
        ui.clickWhenReady(locator);
    }

    /** Delegates to {@link com.erp.automation.ui.UiWaits} (shared by every screen). */
    public boolean dismissInstallPrompt() {
        return ui.dismissInstallPrompt();
    }
}
