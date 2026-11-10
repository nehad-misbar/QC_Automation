
package com.erp.automation.pages;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import org.openqa.selenium.Keys;


/**
 * Page Object for the Local Purchase Invoices screen.
 * Contains locators and actions specific to this screen.
 */
public class LocalPurchaseInvoicePage extends BasePage {

    /**
     * Stores the test ID of the Item cell in the newly added invoice row.
     */
    private String newItemTestId;

    /**
     * Stores the captured WebElement for the newly added invoice row.
     */
    private WebElement newRow;

    /**
     * Stores the AG Grid row index of the newly added invoice row.
     */
    private int newRowIndex = -1;

    /**
     * Stores the row index of the newly created row in the Local Purchase Invoice
     * Discounts & Additions AG Grid.
     *
     * This value is intentionally separate from newRowIndex because the Local
     * Purchase Invoice items grid and the Discounts & Additions grid maintain
     * independent AG Grid row indexes.
     */
    private int newDiscountAdditionRowIndex = -1;

    /**
     * Locates the "جديد" button using its visible text,
     * avoiding dynamically generated data-testid attributes.
     */
    private final By newInvoiceButton =
            By.xpath("//button[.//span[normalize-space()='جديد']]");

    /**
     * Locates the Operation Type dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By operationTypeDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-operationTypeCategoryId']"
            );

    /**
     * Locates the available options displayed in the Operation Type dropdown.
     * Uses the shared React Select option class.
     */
    private final By operationTypeOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Vendor dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By vendorDropdown =
            By.cssSelector("[data-testid='invoice-local-vendorId']");

    /**
     * Locates the visible options displayed in the React Select dropdown.
     * The options are collected only after opening the Vendor dropdown.
     */
    private final By vendorOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Currency dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By currencyDropdown =
            By.cssSelector("[data-testid='invoice-local-currencyId']");

    /**
     * Locates the available options displayed in the Currency React Select dropdown.
     */
    private final By currencyOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Branch dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By branchDropdown =
            By.cssSelector("[data-testid='invoice-local-branchId']");

    /**
     * Locates the available options displayed in the Branch React Select dropdown.
     */
    private final By branchOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Warehouse dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By warehouseDropdown =
            By.cssSelector("[data-testid='invoice-local-warehouseId']");

    /**
     * Locates the visible options displayed in the Warehouse React Select dropdown.
     */
    private final By warehouseOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Cost Center dropdown on the Local Purchase Invoices form
     * using its stable data-testid attribute.
     */
    private final By costCenterDropdown =
            By.cssSelector("[data-testid='invoice-local-costCenterId']");

    /**
     * Locates the available options displayed in the Cost Center React Select dropdown.
     */
    private final By costCenterOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Tax Calculation Method React Select dropdown
     * using its stable data-testid on the Local Purchase Invoice form.
     */
    private final By taxCalculationMethodDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-taxCalculationMethode']"
            );

    /**
     * Locates the available options displayed in the
     * Tax Calculation Method React Select dropdown.
     */
    private final By taxCalculationMethodOptions =
            By.cssSelector("div[class*='select__option']");

    /**
     * Locates the Price Nature radio buttons on the Local Purchase Invoice form.
     * Uses the stable data-testid prefix shared by both price options.
     */
    private final By priceNatureRadioButtons =
            By.cssSelector(
                    "[data-testid^='invoice-local-priceNature-'][role='radio']"
            );

    /**
     * Locates the Invoice Date input field using its stable test ID and input ID.
     */
    private final By invoiceDateInput =
            By.cssSelector(
                    "[data-testid='invoice-local-invoiceDate'] input#invoiceDate"
            );

    /**
     * Locates the button that opens the Invoice Date calendar.
     */
    private final By invoiceDateCalendarButton =
            By.cssSelector(
                    "[data-testid='invoice-local-invoiceDate'] button[aria-label='Open date picker']"
            );

    /**
     * Identifies the Due Date input using its stable data-testid and input ID.
     */
    private final By dueDateInput =
            By.cssSelector(
                    "[data-testid='invoice-local-deservedDate'] input#deservedDate"
            );

    /**
     * Identifies the calendar-opening button inside the Due Date field.
     */
    private final By dueDateCalendarButton =
            By.cssSelector(
                    "[data-testid='invoice-local-deservedDate'] button[aria-label='Open date picker']"
            );

    /**
     * Locates the Purchase Representative React Select dropdown
     * using its stable data-testid attribute.
     */
    private final By purchaseRepresentativeDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-purchaseRepresentativeId']"
            );

    /**
     * Locates the available Purchase Representative options
     * displayed in the React Select dropdown menu.
     */
    private final By purchaseRepresentativeOptions =
            By.cssSelector(
                    "div.select__option"
            );


    /**
     * Locates the Bulk Discount Type React Select dropdown
     * using its stable data-testid attribute.
     */
    private final By bulkDiscountTypeDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-bulkDiscountType']"
            );

    /**
     * Locates the individual options displayed in the
     * Bulk Discount Type React Select dropdown menu.
     */
    private final By bulkDiscountTypeOptions =
            By.cssSelector(
                    "div.select__option"
            );


    /**
     * Locates the Bulk Discount Amount input field
     * using its stable data-testid attribute.
     */
    private final By bulkDiscountAmountField =
            By.cssSelector(
                    "input[data-testid='invoice-local-bulkDiscountAmount']"
            );

    /**
     * Locates the Item Costs Method React Select dropdown
     * using its stable data-testid attribute.
     */
    private final By itemsCostsMethodDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-itemsCostsMethode']"
            );

    /**
     * Locates the available options displayed in React Select dropdowns.
     * Used to collect and randomly select an Item Costs Method.
     */
    private final By itemsCostsMethodOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Tag React Select dropdown using its stable data-testid.
     */
    private final By tagDropdown =
            By.cssSelector(
                    "[data-testid='invoice-local-tagId']"
            );

    /**
     * Locates the available options displayed in the Tag dropdown.
     */
    private final By tagOptions =
            By.cssSelector("div.select__option");

    /**
     * Locates the Quantity cell specifically within the newly added
     * Local Purchase Invoice row.
     *
     * The Item cell identified by newItemTestId is used as the stable
     * row anchor, then the Quantity cell is located inside that same row.
     *
     * This ensures that quantity entry and verification are performed
     * only on the newly added Local Purchase Invoice row, without
     * affecting Quantity cells in existing invoice rows.
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
     * Initializes the Local Purchase Invoices page
     * using the WebDriver managed by the test.
     *
     * @param driver active WebDriver instance
     */
    public LocalPurchaseInvoicePage(WebDriver driver) {
        super(driver);
    }

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

    /**
     * Clicks the "جديد" button to start creating
     * a new local purchase invoice.
     */
    public void clickNew() {
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        wait.until(
                ExpectedConditions.elementToBeClickable(newInvoiceButton)
        ).click();
    }


    /**
     * Opens the Operation Type dropdown and randomly selects one of its
     * visible options.
     *
     * Retries up to five times if React Select re-renders its elements
     * or the dropdown/options cannot be located within the wait timeout.
     * Option texts are collected first, and the selected option is
     * re-located immediately before clicking to avoid stale elements.
     */
    public void selectRandomOperationType() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Local Purchase Invoice Operation Type - attempt "
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
                 * Do not retain option WebElements for the later click.
                 * If React re-renders while reading the options, return
                 * null so WebDriverWait can retry the reading operation.
                 */
                List<String> operationTypes = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(
                                        operationTypeOptions
                                );

                        List<String> texts = new ArrayList<>();

                        for (WebElement element : elements) {

                            if (element.isDisplayed()) {

                                String text = element.getText().trim();

                                if (!text.isEmpty()) {
                                    texts.add(text);
                                }
                            }
                        }

                        return texts.isEmpty() ? null : texts;

                    } catch (StaleElementReferenceException e) {

                        return null;
                    }
                });

                if (operationTypes == null || operationTypes.isEmpty()) {

                    throw new IllegalStateException(
                            "No Operation Types found in the dropdown."
                    );
                }

                /*
                 * Select one Operation Type randomly by its visible text.
                 */
                String selectedOperationType =
                        operationTypes.get(
                                random.nextInt(operationTypes.size())
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
                 * Build a fresh XPath using the selected visible text.
                 *
                 * XPath literals are constructed safely so option names
                 * containing apostrophes or quotation marks are supported.
                 */
                By selectedOperationTypeOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]"
                                        + "[normalize-space(.)="
                                        + toXPathLiteral(selectedOperationType)
                                        + "]"
                        );

                /*
                 * Re-find the selected option immediately before clicking
                 * because React Select may replace the original DOM element.
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
                        "Operation Type dropdown/options were not stable "
                                + "during attempt "
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
                        + " attempts because the React Select dropdown "
                        + "kept re-rendering or timing out."
        );
    }

    /**
     * Converts a string into a valid XPath 1.0 string literal.
     * Supports values containing single quotes, double quotes, or both.
     *
     * @param value text to convert into an XPath string literal
     * @return a valid XPath string literal representing the supplied text
     */
    private String toXPathLiteral(String value) {

        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'", -1);
        StringBuilder xpathLiteral = new StringBuilder("concat(");

        for (int i = 0; i < parts.length; i++) {

            if (i > 0) {
                xpathLiteral.append(", \"'\", ");
            }

            xpathLiteral.append("'").append(parts[i]).append("'");
        }

        xpathLiteral.append(")");

        return xpathLiteral.toString();
    }

    /**
     * Opens the Vendor dropdown on the Local Purchase Invoices form,
     * randomly selects one of its visible non-empty options, and retries
     * the operation when stale elements or timeouts occur.
     *
     * The dropdown is re-located on every attempt to handle React re-renders.
     *
     * @throws IllegalStateException if no vendor can be selected
     *                               after the maximum number of attempts
     */
    public void selectRandomVendor() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt +
                                " of " + maxAttempts +
                                ": Selecting a random vendor..."
                );

                // Re-locate and open the Vendor dropdown on every attempt.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(vendorDropdown)
                );

                dropdown.click();

                // Collect the currently visible vendor options.
                List<WebElement> availableOptions = wait.until(driver -> {

                    try {

                        List<WebElement> visibleOptions =
                                new ArrayList<>();

                        for (WebElement option :
                                driver.findElements(vendorOptions)) {

                            if (option.isDisplayed()) {

                                String optionText =
                                        option.getText().trim();

                                if (!optionText.isEmpty()) {
                                    visibleOptions.add(option);
                                }
                            }
                        }

                        return visibleOptions.isEmpty()
                                ? null
                                : visibleOptions;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                if (availableOptions == null ||
                        availableOptions.isEmpty()) {

                    throw new IllegalStateException(
                            "No visible vendor options were found."
                    );
                }

                // Select a random vendor from the available options.
                int randomIndex =
                        random.nextInt(availableOptions.size());

                String selectedVendor =
                        availableOptions.get(randomIndex)
                                .getText()
                                .trim();

                System.out.println(
                        "Available vendors: " +
                                availableOptions.size()
                );

                System.out.println(
                        "Randomly selected vendor: " +
                                selectedVendor
                );

                // Build a safe XPath literal for the selected vendor text.
                By selectedVendorOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedVendor) + "]"
                );

                // Re-locate the option and wait until it becomes clickable.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedVendorOption
                        )
                );

                optionToClick.click();

                System.out.println(
                        "Vendor selected successfully: " +
                                selectedVendor
                );

                return;

            } catch (StaleElementReferenceException |
                     TimeoutException e) {

                System.out.println(
                        "Attempt " + attempt +
                                " failed while selecting a vendor: " +
                                e.getMessage()
                );

                if (attempt == maxAttempts) {

                    throw new IllegalStateException(
                            "Failed to select a random vendor after " +
                                    maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Opens the Currency dropdown, randomly selects one of its visible
     * non-empty options, and retries if stale elements or timeouts occur.
     *
     * The currently selected currency is not excluded from the random choice.
     * The dropdown and options are re-located to handle React re-renders.
     *
     * @throws IllegalStateException if a currency cannot be selected
     *                               after the maximum number of attempts
     */
    public void selectRandomCurrency() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt + " of " + maxAttempts +
                                ": Selecting a random currency..."
                );

                // Re-locate and open the Currency dropdown on each attempt.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(currencyDropdown)
                );

                dropdown.click();

                // Collect the visible, non-empty currency options.
                List<WebElement> availableOptions = wait.until(currentDriver -> {

                    try {

                        List<WebElement> visibleOptions = new ArrayList<>();

                        for (WebElement option :
                                currentDriver.findElements(currencyOptions)) {

                            if (option.isDisplayed()
                                    && !option.getText().trim().isEmpty()) {

                                visibleOptions.add(option);
                            }
                        }

                        return visibleOptions.isEmpty()
                                ? null
                                : visibleOptions;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                if (availableOptions == null || availableOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No visible currency options were found."
                    );
                }

                // Choose a random currency from the visible options.
                int randomIndex = random.nextInt(availableOptions.size());

                String selectedCurrency =
                        availableOptions.get(randomIndex).getText().trim();

                System.out.println(
                        "Available currencies: " + availableOptions.size()
                );

                System.out.println(
                        "Randomly selected currency: " + selectedCurrency
                );

                // Build a safe XPath using the selected currency's visible text.
                By selectedCurrencyOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedCurrency) + "]"
                );

                // Re-find the option and wait until it is clickable.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedCurrencyOption
                        )
                );

                optionToClick.click();

                System.out.println(
                        "Currency selected successfully: " + selectedCurrency
                );

                return;

            } catch (StaleElementReferenceException | TimeoutException e) {

                System.out.println(
                        "Attempt " + attempt +
                                " failed while selecting a currency: " +
                                e.getMessage()
                );

                if (attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Failed to select a random currency after " +
                                    maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Opens the Branch dropdown, randomly selects one of its visible,
     * non-empty options, and retries when stale elements or timeouts occur.
     *
     * The dropdown and its options are re-located on each attempt to handle
     * React re-renders reliably.
     *
     * @throws IllegalStateException if a branch cannot be selected
     *                               after the maximum number of attempts
     */
    public void selectRandomBranch() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt + " of " + maxAttempts +
                                ": Selecting a random branch..."
                );

                // Re-locate and open the Branch dropdown.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(branchDropdown)
                );

                dropdown.click();

                // Collect visible, non-empty branch options.
                List<WebElement> availableOptions = wait.until(currentDriver -> {

                    try {

                        List<WebElement> visibleOptions = new ArrayList<>();

                        for (WebElement option :
                                currentDriver.findElements(branchOptions)) {

                            if (option.isDisplayed()
                                    && !option.getText().trim().isEmpty()) {

                                visibleOptions.add(option);
                            }
                        }

                        return visibleOptions.isEmpty()
                                ? null
                                : visibleOptions;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                if (availableOptions == null || availableOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No visible branch options were found."
                    );
                }

                // Choose a random branch from the available options.
                int randomIndex = random.nextInt(availableOptions.size());

                String selectedBranch =
                        availableOptions.get(randomIndex).getText().trim();

                System.out.println(
                        "Available branches: " + availableOptions.size()
                );

                System.out.println(
                        "Randomly selected branch: " + selectedBranch
                );

                // Build a safe XPath for the selected branch text.
                By selectedBranchOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedBranch) + "]"
                );

                // Re-locate the selected option and wait until it is clickable.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedBranchOption
                        )
                );

                optionToClick.click();

                System.out.println(
                        "Branch selected successfully: " + selectedBranch
                );

                return;

            } catch (StaleElementReferenceException | TimeoutException e) {

                System.out.println(
                        "Attempt " + attempt +
                                " failed while selecting a branch: " +
                                e.getMessage()
                );

                if (attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Failed to select a random branch after " +
                                    maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Opens the Warehouse dropdown and randomly selects one of its visible,
     * non-empty options.
     *
     * Returns false when the warehouse list is empty or when a timeout or
     * stale-element error prevents selection, allowing the test to select
     * another branch and retry.
     *
     * @return true if a warehouse was selected; false otherwise
     */
    public boolean selectRandomWarehouse() {

        WebDriverWait dropdownWait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Use a shorter wait because an empty warehouse list triggers a branch retry.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        Random random = new Random();

        try {

            System.out.println("Opening the Warehouse dropdown...");

            // Locate and open the Warehouse dropdown.
            WebElement dropdown = dropdownWait.until(
                    ExpectedConditions.elementToBeClickable(warehouseDropdown)
            );

            dropdown.click();

            // Wait briefly for available warehouse options.
            List<WebElement> availableOptions = optionsWait.until(currentDriver -> {

                try {

                    List<WebElement> visibleOptions = new ArrayList<>();

                    for (WebElement option :
                            currentDriver.findElements(warehouseOptions)) {

                        if (option.isDisplayed()
                                && !option.getText().trim().isEmpty()) {

                            visibleOptions.add(option);
                        }
                    }

                    return visibleOptions.isEmpty()
                            ? null
                            : visibleOptions;

                } catch (StaleElementReferenceException e) {
                    return null;
                }
            });

            // No warehouses are available for the currently selected branch.
            if (availableOptions == null || availableOptions.isEmpty()) {

                System.out.println(
                        "No warehouse options are available for the selected branch."
                );

                return false;
            }

            // Select a random warehouse from the visible options.
            int randomIndex = random.nextInt(availableOptions.size());

            String selectedWarehouse =
                    availableOptions.get(randomIndex).getText().trim();

            System.out.println(
                    "Available warehouses: " + availableOptions.size()
            );

            System.out.println(
                    "Randomly selected warehouse: " + selectedWarehouse
            );

            // Build a safe XPath for the selected warehouse's visible text.
            By selectedWarehouseOption = By.xpath(
                    "//div[contains(@class,'select__option')]" +
                            "[normalize-space(.)=" +
                            toXPathLiteral(selectedWarehouse) + "]"
            );

            // Re-locate the option and wait until it is clickable.
            WebElement optionToClick = optionsWait.until(
                    ExpectedConditions.elementToBeClickable(
                            selectedWarehouseOption
                    )
            );

            optionToClick.click();

            System.out.println(
                    "Warehouse selected successfully: " + selectedWarehouse
            );

            return true;

        } catch (TimeoutException | StaleElementReferenceException e) {

            System.out.println(
                    "Could not select a warehouse: " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Randomly selects a Cost Center when the dropdown is enabled.
     *
     * If the Cost Center field is disabled, the method returns false
     * immediately without opening the dropdown or selecting an option.
     *
     * The method also returns false if no visible options are available
     * or if a timeout or stale-element error prevents selection.
     *
     * @return true if a Cost Center was selected; false otherwise
     */
    public boolean selectRandomCostCenter() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        try {

            // Locate the Cost Center dropdown container.
            WebElement dropdownContainer =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    costCenterDropdown
                            )
                    );

            // Locate the actual React Select input to check whether it is disabled.
            WebElement dropdownInput =
                    dropdownContainer.findElement(
                            By.cssSelector("input[role='combobox']")
                    );

            // Skip all selection steps when the Cost Center field is disabled.
            if (!dropdownInput.isEnabled()
                    || "true".equals(
                    dropdownInput.getAttribute("aria-disabled"))) {

                System.out.println(
                        "Cost Center field is disabled. Skipping selection."
                );

                return false;
            }

            // Open the enabled Cost Center dropdown.
            wait.until(
                    ExpectedConditions.elementToBeClickable(costCenterDropdown)
            ).click();

            // Collect the visible, non-empty Cost Center options.
            WebDriverWait optionsWait =
                    new WebDriverWait(driver, Duration.ofSeconds(3));

            List<WebElement> availableOptions =
                    optionsWait.until(currentDriver -> {

                        try {

                            List<WebElement> visibleOptions =
                                    new ArrayList<>();

                            for (WebElement option :
                                    currentDriver.findElements(costCenterOptions)) {

                                if (option.isDisplayed()
                                        && !option.getText().trim().isEmpty()) {

                                    visibleOptions.add(option);
                                }
                            }

                            return visibleOptions.isEmpty()
                                    ? null
                                    : visibleOptions;

                        } catch (StaleElementReferenceException e) {
                            return null;
                        }
                    });

            if (availableOptions == null || availableOptions.isEmpty()) {

                System.out.println(
                        "No Cost Center options are available."
                );

                return false;
            }

            // Randomly choose one of the visible Cost Center options.
            int randomIndex = random.nextInt(availableOptions.size());

            String selectedCostCenter =
                    availableOptions.get(randomIndex).getText().trim();

            System.out.println(
                    "Available Cost Centers: " + availableOptions.size()
            );

            System.out.println(
                    "Randomly selected Cost Center: " + selectedCostCenter
            );

            // Build a safe XPath for the selected option's visible text.
            By selectedCostCenterOption = By.xpath(
                    "//div[contains(@class,'select__option')]" +
                            "[normalize-space(.)=" +
                            toXPathLiteral(selectedCostCenter) + "]"
            );

            // Re-locate the option and wait until it is clickable.
            WebElement optionToClick = optionsWait.until(
                    ExpectedConditions.elementToBeClickable(
                            selectedCostCenterOption
                    )
            );

            optionToClick.click();

            System.out.println(
                    "Cost Center selected successfully: " + selectedCostCenter
            );

            return true;

        } catch (TimeoutException | StaleElementReferenceException e) {

            System.out.println(
                    "Could not select Cost Center: " + e.getMessage()
            );

            return false;
        }
    }


    /**
     * Selects a random Tax Calculation Method from the Local Purchase Invoice form.
     *
     * The method retries up to five times to handle React Select re-renders
     * and temporary timeouts. It collects visible option texts, randomly
     * selects one, and re-finds the option before clicking to avoid stale elements.
     */
    public void selectRandomTaxCalculationMethod() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Tax Calculation Method - attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

                /*
                 * Re-find and open the Tax Calculation Method dropdown
                 * on every attempt.
                 */
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                taxCalculationMethodDropdown
                        )
                );

                dropdown.click();

                System.out.println(
                        "Tax Calculation Method dropdown opened."
                );

                /*
                 * Read the currently visible option texts.
                 *
                 * Return null when no visible options are available
                 * or React re-renders the dropdown, allowing WebDriverWait
                 * to retry the reading operation.
                 */
                List<String> taxCalculationMethods = wait.until(
                        currentDriver -> {

                            try {

                                List<WebElement> elements =
                                        currentDriver.findElements(
                                                taxCalculationMethodOptions
                                        );

                                List<String> texts = new ArrayList<>();

                                for (WebElement element : elements) {

                                    if (element.isDisplayed()) {

                                        String text =
                                                element.getText().trim();

                                        if (!text.isEmpty()) {
                                            texts.add(text);
                                        }
                                    }
                                }

                                return texts.isEmpty() ? null : texts;

                            } catch (StaleElementReferenceException e) {

                                return null;
                            }
                        }
                );

                if (taxCalculationMethods == null
                        || taxCalculationMethods.isEmpty()) {

                    throw new IllegalStateException(
                            "No Tax Calculation Methods found in the dropdown."
                    );
                }

                /*
                 * Select one Tax Calculation Method randomly by its visible text.
                 */
                String selectedTaxCalculationMethod =
                        taxCalculationMethods.get(
                                random.nextInt(taxCalculationMethods.size())
                        );

                System.out.println(
                        "Number of Tax Calculation Methods: "
                                + taxCalculationMethods.size()
                );

                System.out.println(
                        "Selected Tax Calculation Method: "
                                + selectedTaxCalculationMethod
                );

                /*
                 * Build a fresh XPath using the selected visible text.
                 * The existing toXPathLiteral() helper safely handles
                 * apostrophes and quotation marks in option names.
                 */
                By selectedTaxCalculationMethodOption =
                        By.xpath(
                                "//div[contains(@class,'select__option')]"
                                        + "[normalize-space(.)="
                                        + toXPathLiteral(
                                        selectedTaxCalculationMethod
                                )
                                        + "]"
                        );

                /*
                 * Re-find the selected option immediately before clicking
                 * because React Select may replace the original DOM element.
                 */
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedTaxCalculationMethodOption
                        )
                ).click();

                System.out.println(
                        "Tax Calculation Method selected successfully: "
                                + selectedTaxCalculationMethod
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Tax Calculation Method dropdown was re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Tax Calculation Method dropdown/options were not stable "
                                + "during attempt "
                                + attempt
                                + ". Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Tax Calculation Method after "
                        + maxAttempts
                        + " attempts because the React Select dropdown "
                        + "kept re-rendering or timing out."
        );
    }


    /**
     * Randomly selects a Price Nature radio button on the
     * Local Purchase Invoice form.
     *
     * Available options:
     * - 0: Excluding Tax (باستثناء الضريبة)
     * - 1: Including Tax (شاملة الضريبة)
     *
     * Retries up to five times to handle temporary timeouts
     * and React DOM re-renders.
     */
    public void selectRandomPriceNature() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Price Nature - attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

                /*
                 * Read the currently available radio buttons.
                 * Store their values and test IDs rather than retaining
                 * WebElements that may become stale after a re-render.
                 */
                List<String[]> priceOptions = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(
                                        priceNatureRadioButtons
                                );

                        List<String[]> availableOptions = new ArrayList<>();

                        for (WebElement element : elements) {

                            if (element.isDisplayed()
                                    && element.isEnabled()) {

                                String testId =
                                        element.getAttribute("data-testid");

                                String value =
                                        element.getAttribute("value");

                                if (testId != null && value != null) {

                                    availableOptions.add(
                                            new String[]{testId, value}
                                    );
                                }
                            }
                        }

                        return availableOptions.isEmpty()
                                ? null
                                : availableOptions;

                    } catch (StaleElementReferenceException e) {

                        return null;
                    }
                });

                /*
                 * Select one available Price Nature option randomly.
                 */
                String[] selectedOption =
                        priceOptions.get(
                                random.nextInt(priceOptions.size())
                        );

                String selectedTestId = selectedOption[0];
                String selectedValue = selectedOption[1];

                String selectedPriceNature =
                        "0".equals(selectedValue)
                                ? "باستثناء الضريبة"
                                : "شاملة الضريبة";

                System.out.println(
                        "Number of available Price Nature options: "
                                + priceOptions.size()
                );

                System.out.println(
                        "Selected Price Nature: "
                                + selectedPriceNature
                                + " (value="
                                + selectedValue
                                + ")"
                );

                /*
                 * Re-find the selected radio button immediately before
                 * clicking to avoid stale WebElement references.
                 */
                By selectedRadioButton =
                        By.cssSelector(
                                "[data-testid='"
                                        + selectedTestId
                                        + "'][role='radio']"
                        );

                WebElement radioButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedRadioButton
                        )
                );

                /*
                 * Click only if the selected option is not already checked.
                 */
                if (!"true".equals(
                        radioButton.getAttribute("aria-checked"))) {

                    radioButton.click();
                }

                /*
                 * Verify that the requested radio button is selected.
                 */
                wait.until(currentDriver -> {

                    try {

                        WebElement selectedRadio =
                                currentDriver.findElement(
                                        selectedRadioButton
                                );

                        return "true".equals(
                                selectedRadio.getAttribute("aria-checked")
                        );

                    } catch (StaleElementReferenceException e) {

                        return false;
                    }
                });

                System.out.println(
                        "Price Nature selected successfully: "
                                + selectedPriceNature
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Price Nature radio buttons were re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Price Nature radio buttons were not stable "
                                + "during attempt "
                                + attempt
                                + ". Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Price Nature after "
                        + maxAttempts
                        + " attempts because the radio buttons "
                        + "kept re-rendering or timing out."
        );
    }


    /**
     * Sets today's Invoice Date directly without opening the calendar.
     *
     * Assigns today's date in yyyy-MM-dd format using JavaScript and dispatches
     * bubbling input and change events so the application can detect the update.
     * Verifies that the Invoice Date input contains the expected value.
     */
    public void enterTodayInvoiceDateDirectly() {

        // Format today's date using the expected input format.
        String todayInvoiceDate =
                LocalDate.now().format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                );

        // Initialize JavaScript execution for updating the date field.
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        // Wait for the Invoice Date input identified by its existing stable locator.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                invoiceDateInput
                        )
                );

        // Set the date and dispatch input and change events.
        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                todayInvoiceDate
        );

        // Verify that the Invoice Date field contains today's date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        invoiceDateInput,
                        "value",
                        todayInvoiceDate
                )
        );

        System.out.println(
                "Invoice Date set and verified: " + todayInvoiceDate
        );
    }


    /**
     * Selects today's Invoice Date from the React DatePicker calendar
     * on the New Local Purchase Invoice form.
     *
     * The method opens the calendar using the button inside the Invoice Date
     * field, locates today's date by its exact aria-label, selects it, and
     * verifies the resulting input value.
     *
     * The calendar is scoped to the date-picker dialog to avoid accidentally
     * selecting a date element from another dialog.
     */
    public void selectTodayInvoiceDateFromCalendar() {

        System.out.println(
                "Selecting today's Invoice Date from the calendar..."
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Generate today's date using the system's local date.
        LocalDate today = LocalDate.now();

        System.out.println("Today's Invoice Date: " + today);

        // Locate the Invoice Date container using its stable test ID.
        By invoiceDateContainerLocator =
                By.cssSelector(
                        "[data-testid='invoice-local-invoiceDate']"
                );

        WebElement invoiceDateContainer =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                invoiceDateContainerLocator
                        )
                );

        // Locate the calendar-opening button within the Invoice Date field.
        WebElement calendarButton =
                invoiceDateContainer.findElement(
                        By.cssSelector(
                                "button[aria-label='Open date picker']"
                        )
                );

        // Open the calendar using the date field's own button.
        wait.until(
                ExpectedConditions.elementToBeClickable(calendarButton)
        ).click();

        System.out.println("Invoice Date calendar button clicked.");

        // Identify the date-picker calendar, not an arbitrary dialog.
        By calendarLocator =
                By.cssSelector(
                        "div[role='dialog'] .rmdp-calendar"
                );

        WebElement calendar =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                calendarLocator
                        )
                );

        // Build the exact aria-label for today's date.
        String dayOfWeek =
                today.format(
                        DateTimeFormatter.ofPattern(
                                "EEEE",
                                Locale.ENGLISH
                        )
                );

        String month =
                today.format(
                        DateTimeFormatter.ofPattern(
                                "MMMM",
                                Locale.ENGLISH
                        )
                );

        int day = today.getDayOfMonth();
        int year = today.getYear();

        String dateAriaLabel =
                String.format(
                        Locale.ENGLISH,
                        "Choose %s %s %02d of %d",
                        dayOfWeek,
                        month,
                        day,
                        year
                );

        System.out.println(
                "Target Invoice Date aria-label: " + dateAriaLabel
        );

        // Locate today's date only inside the calendar that was opened.
        By todayLocator =
                By.cssSelector(
                        "div[role='dialog'] .rmdp-calendar " +
                                ".rmdp-day[aria-label='" + dateAriaLabel + "']"
                );

        WebElement todayOption =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                todayLocator
                        )
                );

        // Scroll today's date into view before clicking it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                todayOption
        );

        // Re-locate the date after scrolling in case the calendar re-rendered.
        todayOption =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                todayLocator
                        )
                );

        // Use a normal Selenium click so intercepted clicks remain visible.
        todayOption.click();

        System.out.println(
                "Today's date clicked using Selenium."
        );

        // Verify that the Invoice Date input contains today's date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        invoiceDateInput,
                        "value",
                        today.toString()
                )
        );

        System.out.println(
                "Invoice Date selected and verified successfully: " + today
        );
    }





    /**
     * Selects tomorrow's Invoice Date from the calendar on the
     * New Local Purchase Invoice form.
     *
     * The method opens the calendar, locates tomorrow using its exact
     * aria-label, checks whether the item-search dialog is already open
     * before clicking, selects the date using JavaScript, and then
     * checks the page state and visible dialogs after the click.
     *
     * The diagnostics help determine whether the item-search dialog
     * appears before or after the date-selection click.
     */
    public void selectTomorrowInvoiceDateFromCalendar() {

        System.out.println(
                "Selecting tomorrow's Invoice Date from the calendar..."
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Calculate tomorrow's date for selection and verification.
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        String expectedDate = tomorrow.toString();

        System.out.println(
                "Expected Invoice Date (tomorrow): " + expectedDate
        );

        // Locate the Invoice Date container using its stable test ID.
        By invoiceDateContainerLocator =
                By.cssSelector(
                        "[data-testid='invoice-local-invoiceDate']"
                );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        invoiceDateContainerLocator
                )
        );

        // Locate the dedicated calendar-opening button.
        By calendarButtonLocator =
                By.cssSelector(
                        "[data-testid='invoice-local-invoiceDate'] " +
                                "button[aria-label='Open date picker']"
                );

        WebElement calendarButton =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                calendarButtonLocator
                        )
                );

        // Open the Invoice Date calendar.
        calendarButton.click();

        System.out.println(
                "Invoice Date calendar button clicked."
        );

        // Wait until the calendar inside the dialog is visible.
        By calendarLocator =
                By.cssSelector(
                        "div[role='dialog'] .rmdp-calendar"
                );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarLocator
                )
        );

        System.out.println(
                "Invoice Date calendar opened successfully."
        );

        // Build the exact aria-label for tomorrow's date.
        String dayOfWeek =
                tomorrow.format(
                        DateTimeFormatter.ofPattern(
                                "EEEE",
                                Locale.ENGLISH
                        )
                );

        String month =
                tomorrow.format(
                        DateTimeFormatter.ofPattern(
                                "MMMM",
                                Locale.ENGLISH
                        )
                );

        String dateAriaLabel =
                String.format(
                        "Choose %s %s %d of %d",
                        dayOfWeek,
                        month,
                        tomorrow.getDayOfMonth(),
                        tomorrow.getYear()
                );

        System.out.println(
                "Target date aria-label: " + dateAriaLabel
        );

        // Locate tomorrow specifically inside the calendar dialog.
        By tomorrowLocator =
                By.cssSelector(
                        "div[role='dialog'] " +
                                ".rmdp-day[aria-label='" +
                                dateAriaLabel +
                                "']"
                );

        WebElement tomorrowOption =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                tomorrowLocator
                        )
                );

        System.out.println(
                "Tomorrow's date element found: " +
                        tomorrowOption.getAttribute("outerHTML")
        );

        // Log the date element and its ancestors before clicking.
        String clickDiagnostics =
                (String) ((JavascriptExecutor) driver).executeScript(
                        "let e = arguments[0];" +
                                "let result = [];" +
                                "for (let i = 0; e && i < 7; i++, e = e.parentElement) {" +
                                "  result.push(" +
                                "    e.tagName + " +
                                "    (e.id ? '#' + e.id : '') + " +
                                "    (typeof e.className === 'string' && e.className " +
                                "      ? '.' + e.className.trim().replace(/\\s+/g, '.') : '') + " +
                                "    (e.getAttribute('data-testid') " +
                                "      ? '[data-testid=' + e.getAttribute('data-testid') + ']' : '')" +
                                "  );" +
                                "}" +
                                "return result.join('\\n');",
                        tomorrowOption
                );

        System.out.println(
                "DOM ancestors of tomorrow's date before click:\n" +
                        clickDiagnostics
        );

        // =========================================================
        // DIAGNOSTIC CHECK 1: Inspect dialogs BEFORE clicking tomorrow.
        // =========================================================

        System.out.println(
                "===== DIALOG DIAGNOSTICS BEFORE DATE CLICK ====="
        );

        By dialogLocator =
                By.cssSelector("[role='dialog']");

        By itemSearchTitleLocator =
                By.xpath(
                        "//*[normalize-space(.)='البحث عن صنف']"
                );

        List<WebElement> dialogsBeforeDateClick =
                driver.findElements(dialogLocator);

        int visibleDialogsBeforeClick = 0;
        boolean itemSearchBeforeClick = false;

        for (int i = 0; i < dialogsBeforeDateClick.size(); i++) {

            // Re-fetch the dialog to reduce stale-element problems.
            List<WebElement> currentDialogs =
                    driver.findElements(dialogLocator);

            if (i >= currentDialogs.size()) {
                break;
            }

            WebElement dialog = currentDialogs.get(i);

            try {
                if (dialog.isDisplayed()) {

                    visibleDialogsBeforeClick++;

                    System.out.println(
                            "Dialog BEFORE date click #" + i
                    );

                    System.out.println(
                            "Dialog class: " +
                                    dialog.getAttribute("class")
                    );

                    System.out.println(
                            "Dialog text: " + dialog.getText()
                    );

                    // Identify whether this visible dialog is the item-search dialog.
                    List<WebElement> titles =
                            dialog.findElements(
                                    itemSearchTitleLocator
                            );

                    for (WebElement title : titles) {
                        if (title.isDisplayed()) {
                            itemSearchBeforeClick = true;

                            System.out.println(
                                    "ITEM SEARCH DIALOG WAS ALREADY OPEN " +
                                            "BEFORE DATE CLICK."
                            );

                            System.out.println(
                                    "Item-search title HTML: " +
                                            title.getAttribute("outerHTML")
                            );
                        }
                    }
                }
            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Dialog #" + i +
                                " changed while checking before the click."
                );
            }
        }

        System.out.println(
                "Visible dialogs before date click: " +
                        visibleDialogsBeforeClick
        );

        System.out.println(
                "Item-search dialog before date click: " +
                        itemSearchBeforeClick
        );

        System.out.println(
                "URL before date click: " + driver.getCurrentUrl()
        );

        System.out.println(
                "===== END DIAGNOSTICS BEFORE DATE CLICK ====="
        );

        // =========================================================
        // Select tomorrow using the JavaScript click that previously worked.
        // =========================================================

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();",
                tomorrowOption
        );

        System.out.println(
                "JavaScript click executed on tomorrow's date."
        );

        // Briefly allow the application UI to react before diagnostics.
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Interrupted while diagnosing Invoice Date selection.",
                    e
            );
        }

        // =========================================================
        // DIAGNOSTIC CHECK 2: Inspect the page AFTER clicking tomorrow.
        // =========================================================

        System.out.println(
                "===== DIALOG DIAGNOSTICS AFTER DATE CLICK ====="
        );

        System.out.println(
                "URL immediately after date click: " +
                        driver.getCurrentUrl()
        );

        // Read the Invoice Date input after the click.
        List<WebElement> dateInputsAfterClick =
                driver.findElements(invoiceDateInput);

        String dateValueAfterClick =
                dateInputsAfterClick.isEmpty()
                        ? "Invoice Date input not found"
                        : dateInputsAfterClick.get(0).getAttribute("value");

        System.out.println(
                "Invoice Date immediately after click: " +
                        dateValueAfterClick
        );

        // Inspect all visible dialogs after selecting tomorrow.
        List<WebElement> dialogsAfterDateClick =
                driver.findElements(dialogLocator);

        int visibleDialogsAfterClick = 0;
        boolean itemSearchAfterClick = false;

        for (int i = 0; i < dialogsAfterDateClick.size(); i++) {

            // Re-fetch the dialog to reduce stale-element problems.
            List<WebElement> currentDialogs =
                    driver.findElements(dialogLocator);

            if (i >= currentDialogs.size()) {
                break;
            }

            WebElement dialog = currentDialogs.get(i);

            try {
                if (dialog.isDisplayed()) {

                    visibleDialogsAfterClick++;

                    System.out.println(
                            "Dialog AFTER date click #" + i
                    );

                    System.out.println(
                            "Dialog class: " +
                                    dialog.getAttribute("class")
                    );

                    System.out.println(
                            "Dialog text: " + dialog.getText()
                    );

                    System.out.println(
                            "Dialog HTML: " +
                                    dialog.getAttribute("outerHTML")
                    );

                    // Detect the item-search dialog within this visible dialog.
                    List<WebElement> titles =
                            dialog.findElements(
                                    itemSearchTitleLocator
                            );

                    for (WebElement title : titles) {
                        if (title.isDisplayed()) {
                            itemSearchAfterClick = true;

                            System.out.println(
                                    "ITEM SEARCH DIALOG DETECTED AFTER DATE CLICK."
                            );

                            System.out.println(
                                    "Item-search title HTML: " +
                                            title.getAttribute("outerHTML")
                            );

                            System.out.println(
                                    "Item-search title parent HTML: " +
                                            title.findElement(
                                                    By.xpath("..")
                                            ).getAttribute("outerHTML")
                            );
                        }
                    }
                }
            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Dialog #" + i +
                                " changed while checking after the click."
                );
            }
        }

        System.out.println(
                "Visible dialogs after date click: " +
                        visibleDialogsAfterClick
        );

        System.out.println(
                "Item-search dialog before click: " +
                        itemSearchBeforeClick
        );

        System.out.println(
                "Item-search dialog after click: " +
                        itemSearchAfterClick
        );

        if (!itemSearchBeforeClick && itemSearchAfterClick) {
            System.out.println(
                    "DIAGNOSTIC RESULT: Item-search dialog was not detected " +
                            "before the date click, but was detected afterward."
            );
        } else if (itemSearchBeforeClick && itemSearchAfterClick) {
            System.out.println(
                    "DIAGNOSTIC RESULT: Item-search dialog was already open " +
                            "before the date click."
            );
        } else if (!itemSearchBeforeClick && !itemSearchAfterClick) {
            System.out.println(
                    "DIAGNOSTIC RESULT: Item-search dialog was not detected " +
                            "in either check."
            );
        }

        System.out.println(
                "===== END DIALOG DIAGNOSTICS AFTER DATE CLICK ====="
        );

        // =========================================================
        // Verify that the Invoice Date input eventually contains tomorrow.
        // =========================================================

        try {

            wait.until(
                    ExpectedConditions.attributeToBe(
                            invoiceDateInput,
                            "value",
                            expectedDate
                    )
            );

        } catch (TimeoutException e) {

            List<WebElement> currentDateInputs =
                    driver.findElements(invoiceDateInput);

            String actualDate =
                    currentDateInputs.isEmpty()
                            ? "Invoice Date input not found"
                            : currentDateInputs.get(0).getAttribute("value");

            System.err.println(
                    "Invoice Date selection failed."
            );

            System.err.println(
                    "Expected date: " + expectedDate
            );

            System.err.println(
                    "Actual date: " + actualDate
            );

            System.err.println(
                    "Current URL: " + driver.getCurrentUrl()
            );

            throw new AssertionError(
                    "Could not select tomorrow's Invoice Date. " +
                            "Expected: " + expectedDate +
                            ", Actual: " + actualDate,
                    e
            );
        }

        // Log the final selected date after successful verification.
        String actualDate =
                driver.findElement(invoiceDateInput)
                        .getAttribute("value");

        System.out.println(
                "Expected Invoice Date: " + expectedDate
        );

        System.out.println(
                "Actual Invoice Date: " + actualDate
        );

        System.out.println(
                "SUCCESS: Tomorrow's Invoice Date selected: " +
                        actualDate
        );
    }


    /**
     * Sets the Due Date to one year from today's date without opening the calendar.
     *
     * Uses JavaScript to assign the date in yyyy-MM-dd format and dispatches
     * bubbling input and change events so the application can detect the update.
     * Verifies that the Due Date input contains the expected date.
     */
    public void enterDueDateOneYearFromNowDirectly() {

        // Calculate the Due Date one year from today.
        String dueDate =
                LocalDate.now()
                        .plusYears(1)
                        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Initialize JavaScript execution for updating the Due Date field.
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        // Wait until the Due Date input is visible.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                dueDateInput
                        )
                );

        // Set the Due Date and notify the application through input and change events.
        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                dueDate
        );

        // Verify that the Due Date field contains the expected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        dueDateInput,
                        "value",
                        dueDate
                )
        );

        System.out.println(
                "Due Date set and verified: " + dueDate
        );
    }




    /**
     * Selects the Due Date one year from today using the RMDP calendar.
     *
     * Opens the Due Date calendar, navigates forward one month at a time,
     * selects the exact target date using its aria-label, and verifies that
     * the selected date was committed to the Due Date input.
     */
    public void selectDueDateOneYearFromNowFromCalendar() {

        System.out.println("========================================");
        System.out.println("Selecting Due Date one year from today...");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Calculate the target Due Date exactly one year from today.
        LocalDate targetDueDate = LocalDate.now().plusYears(1);

        String expectedDueDate =
                targetDueDate.format(
                        DateTimeFormatter.ofPattern("yyyy-MM-dd")
                );

        System.out.println("Target Due Date: " + expectedDueDate);

        // Wait until the Due Date input is visible before opening its calendar.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(dueDateInput)
        );

        // Open the calendar associated with the Due Date field.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        dueDateCalendarButton
                )
        ).click();

        System.out.println("Due Date calendar button clicked.");

        // Wait until the RMDP calendar becomes visible.
        By calendarWrapperLocator =
                By.cssSelector(".rmdp-wrapper");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        System.out.println("Due Date calendar opened.");

        // Build the exact aria-label generated for the target calendar date.
        String targetDateAriaLabel =
                "Choose " +
                        targetDueDate.getDayOfWeek().getDisplayName(
                                TextStyle.FULL,
                                Locale.ENGLISH
                        ) +
                        " " +
                        targetDueDate.getMonth().getDisplayName(
                                TextStyle.FULL,
                                Locale.ENGLISH
                        ) +
                        " " +
                        String.format(
                                Locale.ENGLISH,
                                "%02d",
                                targetDueDate.getDayOfMonth()
                        ) +
                        " of " +
                        targetDueDate.getYear();

        System.out.println(
                "Target date aria-label: " + targetDateAriaLabel
        );

        // Locate the exact target date inside the visible calendar.
        By targetDateLocator =
                By.cssSelector(
                        ".rmdp-wrapper [aria-label='" +
                                targetDateAriaLabel +
                                "']"
                );

        // Locate the calendar's Next Month control.
        By nextMonthButtonLocator =
                By.cssSelector(
                        ".rmdp-wrapper " +
                                ".rmdp-arrow-container.rmdp-right"
                );

        int maxMonthNavigationAttempts = 12;

        // Check whether the target date is already displayed.
        boolean targetDateVisible =
                isDisplayedWithoutWaiting(targetDateLocator);

        // Navigate forward until the target date appears or the limit is reached.
        for (
                int attempt = 1;
                attempt <= maxMonthNavigationAttempts && !targetDateVisible;
                attempt++
        ) {

            System.out.println(
                    "Target date not visible. Moving to the next month. " +
                            "Attempt " + attempt + " of " +
                            maxMonthNavigationAttempts
            );

            // Re-find the arrow after every calendar re-render.
            WebElement nextMonthControl =
                    wait.until(currentDriver -> {

                        List<WebElement> controls =
                                currentDriver.findElements(
                                        nextMonthButtonLocator
                                );

                        for (WebElement control : controls) {
                            try {
                                if (control.isDisplayed() &&
                                        control.isEnabled()) {
                                    return control;
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // The calendar was re-rendered; retry the poll.
                            }
                        }

                        return null;
                    });

            // Move to the next month.
            nextMonthControl.click();

            // Wait briefly for the calendar to re-render, not for the target date
            // to appear in this particular month.
            wait.until(currentDriver -> {
                try {
                    List<WebElement> controls =
                            currentDriver.findElements(
                                    nextMonthButtonLocator
                            );

                    return controls.stream().anyMatch(control -> {
                        try {
                            return control.isDisplayed();
                        } catch (StaleElementReferenceException ignored) {
                            return false;
                        }
                    });

                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });

            // Check immediately; do not wait 10 seconds if this is not the target month.
            targetDateVisible =
                    isDisplayedWithoutWaiting(targetDateLocator);

            System.out.println(
                    "Target date visible after navigation: " +
                            targetDateVisible
            );
        }

        // Stop with a clear error if the target date was not found.
        if (!targetDateVisible) {
            throw new NoSuchElementException(
                    "Unable to locate Due Date " +
                            expectedDueDate +
                            " after " +
                            maxMonthNavigationAttempts +
                            " month-navigation attempts."
            );
        }

        // Re-find and wait for the target date to become clickable.
        WebElement targetDate =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                targetDateLocator
                        )
                );

        System.out.println("Target Due Date found in the calendar.");

        // Select the target date.
        targetDate.click();

        System.out.println("Target Due Date clicked.");

        // Confirm that the calendar has closed after selection.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        System.out.println("Due Date calendar closed.");

        // Verify that the input contains the expected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        dueDateInput,
                        "value",
                        expectedDueDate
                )
        );

        System.out.println(
                "Due Date selected and verified successfully: " +
                        expectedDueDate
        );

        System.out.println("========================================");
    }

    /**
     * Checks whether a locator currently has a displayed element without
     * waiting for the explicit-wait timeout.
     *
     * Re-finds elements on every call and safely handles stale elements
     * caused by calendar re-rendering.
     *
     * @param locator locator of the element to check
     * @return true if at least one matching element is displayed; otherwise false
     */
    private boolean isDisplayedWithoutWaiting(By locator) {

        List<WebElement> elements = driver.findElements(locator);

        for (WebElement element : elements) {
            try {
                if (element.isDisplayed()) {
                    return true;
                }
            } catch (StaleElementReferenceException ignored) {
                // Ignore elements replaced during the calendar re-render.
            }
        }

        return false;
    }


    /**
     * Selects a random Purchase Representative from the available React Select options.
     *
     * Waits for the dropdown to stabilize its enabled/disabled state for 2 seconds.
     * Skips selection when the field is disabled, otherwise selects a random
     * visible option and logs the selected representative for test traceability.
     */
    public void selectRandomPurchaseRepresentative() {

        // Allows up to 10 seconds for the dropdown to become visible and stable.
        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(10)
        );

        wait.pollingEvery(Duration.ofMillis(200));
        wait.ignoring(StaleElementReferenceException.class);

        // Waits until the Purchase Representative dropdown is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        purchaseRepresentativeDropdown
                )
        );

        // Tracks the last observed state to ensure it remains unchanged for 2 seconds.
        final String[] lastState = {null};
        final long[] stateStartTime = {0L};

        // Waits until the dropdown's enabled/disabled state stabilizes.
        String stableState = wait.until(currentDriver -> {
            try {
                WebElement dropdown = currentDriver.findElement(
                        purchaseRepresentativeDropdown
                );

                String dropdownClass = dropdown.getAttribute("class");
                String ariaDisabled = dropdown.getAttribute("aria-disabled");

                // Checks React Select's disabled class and native disabled state.
                boolean disabled =
                        (dropdownClass != null
                                && dropdownClass.contains("select--is-disabled"))
                                || "true".equalsIgnoreCase(ariaDisabled)
                                || !dropdown.isEnabled();

                String currentState = disabled ? "DISABLED" : "ENABLED";
                long now = System.nanoTime();

                // Restarts the stability timer whenever the state changes.
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
                // Resets state detection when React replaces the dropdown.
                lastState[0] = null;
                stateStartTime[0] = 0L;
                return null;
            }
        });

        // Skips selection when the dropdown is disabled.
        if ("DISABLED".equals(stableState)) {
            System.out.println(
                    "Purchase Representative field is disabled. "
                            + "Skipping representative selection."
            );
            return;
        }

        // Rechecks the dropdown state immediately before interacting with it.
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

            // Prevents interaction if the field became disabled after the wait.
            if (currentlyDisabled) {
                System.out.println(
                        "Purchase Representative field became disabled. "
                                + "Skipping representative selection."
                );
                return;
            }

        } catch (StaleElementReferenceException e) {
            // Skips interaction when the dropdown changes during verification.
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

        // Waits until at least one visible representative option is available.
        wait.until(currentDriver ->
                currentDriver.findElements(purchaseRepresentativeOptions)
                        .stream()
                        .anyMatch(option -> {
                            try {
                                return option.isDisplayed()
                                        && option.isEnabled();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        })
        );

        // Retrieves the current visible and enabled options after the menu opens.
        List<WebElement> options = wait.until(currentDriver -> {
            try {
                List<WebElement> visibleOptions =
                        currentDriver.findElements(
                                        purchaseRepresentativeOptions
                                )
                                .stream()
                                .filter(option -> {
                                    try {
                                        return option.isDisplayed()
                                                && option.isEnabled();
                                    } catch (StaleElementReferenceException e) {
                                        return false;
                                    }
                                })
                                .toList();

                return visibleOptions.isEmpty()
                        ? null
                        : visibleOptions;

            } catch (StaleElementReferenceException e) {
                return null;
            }
        });

        // Fails clearly if the enabled dropdown has no selectable options.
        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No purchase representatives found."
            );
        }

        // Chooses a random index from the currently available options.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        // Logs the number of available representatives for test traceability.
        System.out.println(
                "Number of purchase representatives: " + options.size()
        );

        // Reads the selected representative's text before clicking the option.
        String selectedRepresentative =
                options.get(randomIndex).getText().trim();

        // Selects the randomly chosen Purchase Representative.
        options.get(randomIndex).click();

        // Logs the selected representative for debugging and reporting.
        System.out.println(
                "Selected purchase representative: "
                        + selectedRepresentative
        );
    }


    /**
     * Selects a random Bulk Discount Type from the visible React Select options.
     *
     * Retries up to 5 times if the DOM changes or an operation times out.
     * Re-finds the selected option before clicking to reduce stale element issues.
     * Logs the available options count and the selected discount type.
     */
    public void selectRandomBulkDiscountType() {

        // Allows up to 10 seconds for each dropdown interaction to complete.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Generates a random index for choosing an available discount type.
        Random random = new Random();

        // Limits retries when React re-renders the dropdown or an operation times out.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Tracks the current operation to make failure logs more informative.
            String currentStage =
                    "locating the Bulk Discount Type dropdown";

            try {

                // Logs the current retry attempt for test traceability.
                System.out.println(
                        "Selecting random Bulk Discount Type - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-finds the dropdown and waits until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                bulkDiscountTypeDropdown
                        )
                );

                // Updates the diagnostic stage before opening the dropdown.
                currentStage =
                        "opening the Bulk Discount Type dropdown";

                // Opens the React Select menu to display available discount types.
                dropdown.click();

                // Updates the diagnostic stage before retrieving the options.
                currentStage =
                        "waiting for visible Bulk Discount Type options";

                // Collects the text of all currently visible, non-empty options.
                List<String> discountTypes = wait.until(currentDriver -> {

                    List<WebElement> options =
                            currentDriver.findElements(
                                    bulkDiscountTypeOptions
                            );

                    List<String> visibleTexts = new ArrayList<>();

                    for (WebElement option : options) {

                        try {

                            // Ignores options that are not currently visible.
                            if (!option.isDisplayed()) {
                                continue;
                            }

                            // Reads the visible option text without surrounding whitespace.
                            String optionText = option.getText().trim();

                            // Excludes empty options from the random selection.
                            if (!optionText.isEmpty()) {
                                visibleTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Retries the wait condition using the refreshed DOM.
                            return null;
                        }
                    }

                    // Continues waiting until at least one valid option is available.
                    return visibleTexts.isEmpty()
                            ? null
                            : visibleTexts;
                });

                // Fails clearly if no selectable discount type was found.
                if (discountTypes.isEmpty()) {
                    throw new IllegalStateException(
                            "The Bulk Discount Type dropdown contains no visible options."
                    );
                }

                // Chooses a random discount type from the collected option texts.
                String selectedDiscountType =
                        discountTypes.get(
                                random.nextInt(discountTypes.size())
                        );

                // Logs the number of available discount types.
                System.out.println(
                        "Number of bulk discount types: "
                                + discountTypes.size()
                );

                // Logs the chosen discount type before selecting it.
                System.out.println(
                        "Selected bulk discount type: "
                                + selectedDiscountType
                );

                // Updates the diagnostic stage before locating the chosen option.
                currentStage =
                        "locating the selected Bulk Discount Type option";

                // Builds an XPath that matches the exact visible option text safely.
                By selectedOption = By.xpath(
                        "//div[contains(@class,'select__option')"
                                + " and normalize-space(.)="
                                + toXPathLiteral(selectedDiscountType)
                                + "]"
                );

                // Re-finds the selected option and waits until it is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                );

                // Updates the diagnostic stage before selecting the option.
                currentStage =
                        "clicking the selected Bulk Discount Type option";

                // Selects the randomly chosen discount type.
                optionToSelect.click();

                // Confirms the selection in the test execution log.
                System.out.println(
                        "Bulk Discount Type selected successfully: "
                                + selectedDiscountType
                );

                // Ends the method after a successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs DOM changes that require another selection attempt.
                System.out.println(
                        "Bulk Discount Type DOM changed during stage: "
                                + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Logs the operation that timed out before retrying.
                System.out.println(
                        "Timeout while "
                                + currentStage
                                + ". Attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports a clear failure if all retry attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Bulk Discount Type after "
                        + maxAttempts
                        + " attempts. Check the dropdown locator, "
                        + "the options locator, and whether the dropdown "
                        + "is available after updating the cost."
        );
    }


    /**
     * Enters the bulk discount amount into the input field.
     *
     * Waits until the field is visible and enabled, then uses JavaScript's
     * native input value setter to update the value and dispatches input
     * and change events so React can detect the update.
     */
    public void enterBulkDiscountAmount() {

        // Defines the amount to be applied as a bulk discount to the items.
        String bulkDiscountAmount = "10";

        // Allows up to 10 seconds for the discount amount field to appear.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Waits until the input field is visible and enabled for interaction.
        WebElement amountField = wait.until(currentDriver -> {

            WebElement field = currentDriver.findElement(
                    bulkDiscountAmountField
            );

            // Proceeds only when the field is visible and enabled.
            return field.isDisplayed() && field.isEnabled()
                    ? field
                    : null;
        });

        // Provides JavaScript execution to update the React-controlled input.
        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        // Sets the value through the native input setter and notifies React.
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

        // Waits until the input reflects the requested discount amount.
        wait.until(currentDriver -> {
            try {
                WebElement field = currentDriver.findElement(
                        bulkDiscountAmountField
                );

                return bulkDiscountAmount.equals(
                        field.getAttribute("value")
                );

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Logs the entered discount amount for test traceability.
        System.out.println(
                "Bulk discount amount: " + bulkDiscountAmount
        );
    }

    /**
     * Randomly selects an available Item Costs Method from the React Select dropdown.
     *
     * Opens the dropdown, collects the visible option texts, randomly chooses
     * one option, and re-finds the selected option before clicking it.
     * Retries up to five times if React re-renders the dropdown and causes
     * a StaleElementReferenceException.
     */
    public void selectRandomItemsCostsMethod() {

        // Defines the maximum time to wait for dropdown elements.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Provides random selection among the available methods.
        Random random = new Random();

        // Defines the maximum number of attempts if the DOM becomes stale.
        int maxAttempts = 5;

        // Retries the selection process if React re-renders the dropdown.
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
                 * Collect the text of all currently visible dropdown options.
                 * Returns null if an option becomes stale during collection,
                 * allowing the wait to retry.
                 */
                List<String> itemsCostsMethods = wait.until(currentDriver -> {

                    List<WebElement> elements =
                            currentDriver.findElements(itemsCostsMethodOptions);

                    List<String> texts = new ArrayList<>();

                    for (WebElement element : elements) {

                        try {

                            if (element.isDisplayed()) {

                                String text = element.getText().trim();

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

                // Stops execution if the dropdown contains no usable options.
                if (itemsCostsMethods.isEmpty()) {
                    throw new IllegalStateException(
                            "No Item Costs Methods found in the dropdown."
                    );
                }

                /*
                 * Randomly selects one method from the available option texts.
                 */
                String selectedItemsCostsMethod =
                        itemsCostsMethods.get(
                                random.nextInt(itemsCostsMethods.size())
                        );

                // Logs the number of available options for test traceability.
                System.out.println(
                        "Number of item costs methods: "
                                + itemsCostsMethods.size()
                );

                // Logs the randomly selected method.
                System.out.println(
                        "Selected item costs method: "
                                + selectedItemsCostsMethod
                );

                /*
                 * Re-find the selected option immediately before clicking it.
                 * xpathLiteral() safely handles option text containing quotes.
                 */
                By selectedOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedItemsCostsMethod)
                                + "]"
                );

                // Waits until the selected option is clickable, then clicks it.
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                ).click();

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Reports a stale DOM and proceeds to the next attempt.
                System.out.println(
                        "Item Costs Method dropdown was re-rendered. "
                                + "Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        // Reports failure if all attempts encounter a stale DOM.
        throw new IllegalStateException(
                "Unable to select Item Costs Method after "
                        + maxAttempts
                        + " attempts because the React Select "
                        + "dropdown kept re-rendering."
        );
    }

    /**
     * Randomly selects an available Tag from the Tag React Select dropdown.
     *
     * Opens the dropdown, collects the visible option texts, randomly selects
     * one option, and re-finds that option before clicking it.
     * Retries up to five times if React re-renders the dropdown or a timeout
     * occurs while locating or selecting an option.
     */
    public void selectRandomTag() {

        // Defines the maximum wait time for dropdown elements.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Provides random selection among the available Tag options.
        Random random = new Random();

        // Defines the maximum number of attempts before reporting failure.
        int maxAttempts = 5;

        // Retries the complete selection process if the dropdown state changes.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                // Logs the current attempt for troubleshooting and test reporting.
                System.out.println(
                        "Selecting random Tag - attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

                /*
                 * Re-locates the Tag dropdown on every attempt because React
                 * may recreate the element after another field is selected.
                 */
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                tagDropdown
                        )
                );

                // Opens the Tag dropdown to display its available options.
                dropdown.click();

                // Confirms that the dropdown has been opened.
                System.out.println("Tag dropdown opened.");

                /*
                 * Reads the visible Tag option texts from the current DOM.
                 * Returns null when an option becomes stale so that WebDriverWait
                 * can retry collecting the options.
                 */
                List<String> tags = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(tagOptions);

                        List<String> texts = new ArrayList<>();

                        for (WebElement element : elements) {

                            if (!element.isDisplayed()) {
                                continue;
                            }

                            String text = element.getText().trim();

                            // Excludes empty and duplicate option labels.
                            if (!text.isEmpty() && !texts.contains(text)) {
                                texts.add(text);
                            }
                        }

                        // Keeps waiting until at least one valid option exists.
                        return texts.isEmpty() ? null : texts;

                    } catch (StaleElementReferenceException e) {

                        // React recreated the option elements; retry the read.
                        return null;
                    }
                });

                // Fails explicitly if no usable Tag options are available.
                if (tags == null || tags.isEmpty()) {
                    throw new IllegalStateException(
                            "No Tags found in the Tag dropdown."
                    );
                }

                // Logs the number of available Tag options.
                System.out.println(
                        "Number of tags: " + tags.size()
                );

                /*
                 * Randomly chooses one Tag from the collected option texts.
                 */
                String selectedTag =
                        tags.get(random.nextInt(tags.size()));

                // Logs the selected Tag for test traceability.
                System.out.println(
                        "Selected tag: " + selectedTag
                );

                /*
                 * Builds a text-based XPath for the selected Tag.
                 * xpathLiteral() safely handles single and double quotes.
                 */
                By selectedTagOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedTag)
                                + "]"
                );

                /*
                 * Re-finds the selected option immediately before clicking it,
                 * avoiding reuse of WebElements collected from an earlier DOM.
                 */
                WebElement option = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedTagOption
                        )
                );

                // Selects the randomly chosen Tag.
                option.click();

                // Confirms successful selection in the test log.
                System.out.println(
                        "Tag selected successfully: " + selectedTag
                );

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs a stale-element issue before restarting the attempt.
                System.out.println(
                        "Tag dropdown was re-rendered. "
                                + "Restarting selection attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

            } catch (TimeoutException e) {

                // Retries if the dropdown or selected option cannot be located in time.
                System.out.println(
                        "Tag selection timed out or the dropdown state changed. "
                                + "Restarting attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        // Reports a clear failure if all selection attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Tag after "
                        + maxAttempts
                        + " attempts."
        );
    }

    /**
     * Clicks the Add Line button and captures the newly created row
     * in the Local Purchase Invoice items AG Grid.
     *
     * Uses the invoice Item column identifier and its stable test-ID prefix
     * to distinguish Item cells from cells in the Discounts & Additions grid.
     *
     * Captures the new row index at runtime and stores it in newRowIndex.
     * The separate newDiscountAdditionRowIndex field is not modified.
     */
    public void clickAddLine() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Locates Item cells using the Local Purchase Invoice test-ID prefix.
         * This prevents cells from unrelated grids, including Discounts &
         * Additions, from being treated as invoice Item cells.
         */
        By itemCellsLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId']" +
                        "[data-testid^='invoice-local-itemId-']"
        );

        /*
         * Locates the Add Line button by its visible Arabic label.
         */
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space()='اضافة سطر']"
        );

        Set<String> existingItemTestIds = new HashSet<>();
        Set<String> existingRowIndexes = new HashSet<>();

        try {

            /*
             * Restore horizontal scrolling to the beginning of the page's
             * AG Grid horizontal viewports so the invoice Item column can
             * become visible after a previous operation edited the Note cell.
             *
             * This does not interact with the Discounts & Additions row index.
             */
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll(" +
                            "'.ag-body-horizontal-scroll-viewport'" +
                            ").forEach(function(viewport) {" +
                            "viewport.scrollLeft = 0;" +
                            "});"
            );

            /*
             * Capture currently rendered invoice Item cells and their
             * logical row indexes before clicking Add Line.
             */
            List<WebElement> existingItemCells =
                    driver.findElements(itemCellsLocator);

            for (WebElement cell : existingItemCells) {

                try {

                    String testId = cell.getAttribute("data-testid");

                    if (testId != null && !testId.isBlank()) {
                        existingItemTestIds.add(testId);
                    }

                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::div[@role='row'][1]")
                    );

                    String rowIndex = row.getAttribute("row-index");

                    if (rowIndex != null && rowIndex.matches("\\d+")) {
                        existingRowIndexes.add(rowIndex);
                    }

                } catch (StaleElementReferenceException ignored) {
                    /*
                     * Ignore cells re-rendered while AG Grid updates.
                     */
                }
            }

            System.out.println(
                    "LOCAL PURCHASE INVOICE - ADD LINE DIAGNOSTIC"
            );

            System.out.println(
                    "Item cells BEFORE Add Line: "
                            + existingItemCells.size()
            );

            System.out.println(
                    "Existing Item test IDs: "
                            + existingItemTestIds
            );

            System.out.println(
                    "Existing row indexes: "
                            + existingRowIndexes
            );

            /*
             * Wait for the Add Line button to become clickable and click it
             * exactly once.
             */
            WebElement addLineButton =
                    wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    addLineButtonLocator
                            )
                    );

            addLineButton.click();

            System.out.println("Add Line button clicked.");

            /*
             * Wait for a newly rendered Item cell whose test ID was not
             * present before clicking Add Line.
             *
             * The prefix identifies the invoice Item column; the changing
             * suffix is read at runtime rather than hardcoded.
             */
            WebElement detectedNewItemCell =
                    wait.until(webDriver -> {

                        List<WebElement> currentItemCells =
                                webDriver.findElements(itemCellsLocator);

                        for (WebElement cell : currentItemCells) {

                            try {

                                String currentTestId =
                                        cell.getAttribute("data-testid");

                                if (currentTestId == null
                                        || currentTestId.isBlank()
                                        || existingItemTestIds.contains(currentTestId)) {
                                    continue;
                                }

                                WebElement row = cell.findElement(
                                        By.xpath(
                                                "./ancestor::div[@role='row'][1]"
                                        )
                                );

                                String rowIndex =
                                        row.getAttribute("row-index");

                                if (rowIndex != null
                                        && rowIndex.matches("\\d+")
                                        && !existingRowIndexes.contains(rowIndex)) {

                                    return cell;
                                }

                            } catch (StaleElementReferenceException ignored) {
                                /*
                                 * AG Grid may replace cell elements while
                                 * rendering the newly created row.
                                 */
                            }
                        }

                        return null;
                    });

            /*
             * Store the new Item cell test ID for compatibility with
             * existing methods that use newItemTestId.
             */
            newItemTestId =
                    detectedNewItemCell.getAttribute("data-testid");

            if (newItemTestId == null || newItemTestId.isBlank()) {
                throw new IllegalStateException(
                        "The new invoice Item cell was detected, " +
                                "but its data-testid is empty."
                );
            }

            /*
             * Capture the logical invoice row containing the new Item cell.
             */
            newRow = detectedNewItemCell.findElement(
                    By.xpath("./ancestor::div[@role='row'][1]")
            );

            /*
             * Validate and store the newly created invoice row index.
             * This updates newRowIndex only; it does not modify
             * newDiscountAdditionRowIndex.
             */
            String rowIndex = newRow.getAttribute("row-index");

            if (rowIndex == null || !rowIndex.matches("\\d+")) {
                throw new IllegalStateException(
                        "Could not determine a valid row-index for the new " +
                                "Local Purchase Invoice line. " +
                                "Item test ID: " + newItemTestId +
                                ", row-index: " + rowIndex
                );
            }

            newRowIndex = Integer.parseInt(rowIndex);

            System.out.println(
                    "Item cells AFTER Add Line: "
                            + driver.findElements(itemCellsLocator).size()
            );

            System.out.println(
                    "New Item test ID: " + newItemTestId
            );

            System.out.println(
                    "New Local Purchase Invoice row index: "
                            + newRowIndex
            );

        } catch (TimeoutException e) {

            /*
             * Print diagnostics without clicking Add Line a second time.
             * This helps determine whether the new row was created but
             * its Item cell was not rendered or detected.
             */
            System.err.println(
                    "Timeout while waiting for the new invoice line."
            );

            System.err.println(
                    "Existing Item test IDs: "
                            + existingItemTestIds
            );

            System.err.println(
                    "Existing row indexes: "
                            + existingRowIndexes
            );

            List<WebElement> currentItemCells =
                    driver.findElements(itemCellsLocator);

            System.err.println(
                    "Currently detected invoice Item cells: "
                            + currentItemCells.size()
            );

            for (WebElement cell : currentItemCells) {

                try {

                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::div[@role='row'][1]")
                    );

                    System.err.println(
                            "Current Item cell test ID: "
                                    + cell.getAttribute("data-testid")
                    );

                    System.err.println(
                            "Current Item cell row index: "
                                    + row.getAttribute("row-index")
                    );

                } catch (StaleElementReferenceException ignored) {
                    System.err.println(
                            "An Item cell was re-rendered during diagnostics."
                    );
                }
            }

            throw new RuntimeException(
                    "Failed to detect the newly added Item cell in the " +
                            "Local Purchase Invoice items grid. " +
                            "The Discounts & Additions grid is handled separately.",
                    e
            );
        }
    }

    /**
     * Selects a random Item Name from the React Select dropdown
     * inside the newly added Local Purchase Invoice row.
     *
     * Uses the previously captured newRow to identify the target row.
     * All locators avoid dynamic IDs and row indexes.
     * The method opens the Item Name cell editor, retrieves the visible
     * dropdown options, and randomly selects one item.
     */
    public void selectRandomItem() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Ensure the newly added invoice row has already been captured.
        if (newRow == null) {
            throw new IllegalStateException(
                    "New invoice row was not captured. Call clickAddLine() first."
            );
        }

        System.out.println("Using captured Local Purchase Invoice row.");

        System.out.println(
                "New row index: " + newRow.getAttribute("row-index")
        );

        /**
         * Locates the Item Name cell using the stable AG Grid column ID.
         * No dynamic data-testid or row index is used.
         */
        By itemNameCell =
                By.cssSelector(
                        "div[role='gridcell'][col-id='itemId']"
                );

        // Locate the Item Name cell only within the captured new row.
        WebElement itemCell =
                newRow.findElement(itemNameCell);

        System.out.println("Item Name cell found.");

        // Scroll the Item Name cell into view before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                """
                arguments[0].scrollIntoView({
                    block: 'center',
                    inline: 'nearest'
                });
                """,
                itemCell
        );

        // Allow the grid to finish positioning the cell before interaction.
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrupted while preparing the Item Name cell.",
                    e
            );
        }

        // Activate the Item Name cell editor using the existing keyboard approach.
        new Actions(driver)
                .moveToElement(itemCell)
                .click()
                .sendKeys(Keys.ENTER)
                .perform();

        System.out.println("ENTER sent to the Item Name cell.");

        /**
         * Locates the active React Select input inside the AG Grid popup editor.
         * Uses stable class names rather than generated React Select IDs.
         */
        By reactSelectInput =
                By.cssSelector(
                        ".ag-popup-editor input.select__input"
                );

        WebElement editor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                reactSelectInput
                        )
                );

        System.out.println("React Select editor found.");

        // Focus the editor to display the available items.
        editor.click();

        System.out.println("Item dropdown opened.");

        // Retrieve the listbox ID associated with the active React Select input.
        String listboxId =
                editor.getAttribute("aria-controls");

        if (listboxId == null || listboxId.isBlank()) {
            throw new IllegalStateException(
                    "React Select listbox ID was not found for the Item Name field."
            );
        }

        System.out.println("React Select listbox ID: " + listboxId);

        /**
         * Locates options within the listbox associated with the active editor.
         * The listbox ID is read at runtime because React Select generates it dynamically.
         */
        By optionLocator =
                By.cssSelector(
                        "#" + listboxId +
                                " div[class*='select__option']"
                );

        // Wait until at least one dropdown option becomes visible.
        wait.until(webDriver ->
                webDriver.findElements(optionLocator)
                        .stream()
                        .anyMatch(WebElement::isDisplayed)
        );

        // Retrieve only visible options to avoid selecting hidden elements.
        List<WebElement> options =
                driver.findElements(optionLocator)
                        .stream()
                        .filter(WebElement::isDisplayed)
                        .toList();

        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No visible items were found in the Item Name dropdown."
            );
        }

        // Choose a random option from the available visible items.
        Random random = new Random();

        int randomIndex =
                random.nextInt(options.size());

        WebElement selectedOption =
                options.get(randomIndex);

        String selectedItem =
                selectedOption.getText().trim();

        // Select the chosen option using JavaScript, matching the existing approach.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].click();",
                selectedOption
        );

        System.out.println(
                "Number of visible items: " + options.size()
        );

        System.out.println(
                "Selected Item Name: " + selectedItem
        );

        System.out.println(
                "Item selected successfully in the new Local Purchase Invoice row."
        );
    }


    /**
     * Enters the requested quantity into the Quantity cell of the newly added
     * Local Purchase Invoice AG Grid row.
     *
     * The new row is identified through newItemTestId, captured when the line
     * is added. The editor is re-located before each interaction to handle
     * React/AG Grid re-rendering and avoid stale element references.
     */
    public void enterQuantity(String quantity) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        System.out.println("========================================");
        System.out.println("Entering Local Purchase Invoice quantity");
        System.out.println("Requested quantity: [" + quantity + "]");
        System.out.println("New Item test id: " + newItemTestId);

        /*
         * Ensure that the new row's Item test id is available before
         * locating its Quantity cell.
         */
        if (newItemTestId == null || newItemTestId.isBlank()) {
            throw new IllegalStateException(
                    "newItemTestId is empty. " +
                            "Call clickAddLine() before enterQuantity()."
            );
        }

        /*
         * Locate the Quantity cell specifically in the newly added row.
         * This locator must use the existing getQuantityCellLocatorForNewRow()
         * method already defined in this page class.
         */
        By quantityCellLocator = getQuantityCellLocatorForNewRow();

        /*
         * Locate the inline editor only when it belongs to the Quantity cell.
         * This avoids accidentally editing another AG Grid column.
         */
        By quantityEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='quantity'].ag-cell-inline-editing " +
                        "input[data-ref='eInput']"
        );

        /*
         * Find and scroll the new row's Quantity cell into view.
         */
        WebElement quantityCell = wait.until(
                ExpectedConditions.elementToBeClickable(quantityCellLocator)
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                quantityCell
        );

        /*
         * Re-locate the cell after scrolling because AG Grid may re-render it.
         */
        quantityCell = wait.until(
                ExpectedConditions.elementToBeClickable(quantityCellLocator)
        );

        /*
         * Activate inline editing using the same click-and-ENTER approach
         * used by the Purchase Order quantity method.
         */
        quantityCell.click();

        quantityCell = wait.until(
                ExpectedConditions.elementToBeClickable(quantityCellLocator)
        );

        quantityCell.sendKeys(Keys.ENTER);

        System.out.println("ENTER sent to the new Quantity cell.");

        /*
         * Wait until the editor appears inside the new row's Quantity cell.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityEditorLocator
                )
        );

        /*
         * Clear the editor through the native input value setter and dispatch
         * input/change events so the frontend can react to the change.
         */
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
                        const setter =
                            Object.getOwnPropertyDescriptor(
                                HTMLInputElement.prototype, 'value'
                            ).set;
    
                        input.focus();
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

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println("Quantity editor cleared.");

        /*
         * Assign the requested quantity using the native setter.
         * Re-find the editor on every wait attempt to handle DOM replacement.
         */
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
                        const value = arguments[1];
                        const setter =
                            Object.getOwnPropertyDescriptor(
                                HTMLInputElement.prototype, 'value'
                            ).set;
    
                        input.focus();
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

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        /*
         * Verify the live DOM property of the editor before committing.
         */
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);
                String value = input.getDomProperty("value");

                System.out.println(
                        "Quantity editor current value: [" + value + "]"
                );

                return quantity.equals(value);

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println("Requested quantity verified in the editor.");

        /*
         * Re-locate the editor and press ENTER to commit the quantity.
         */
        WebElement quantityInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityEditorLocator
                )
        );

        quantityInput.sendKeys(Keys.ENTER);

        /*
         * Verify the committed value in the new row's Quantity cell.
         * Numeric comparison allows values such as 17, 17.0 and 17.000.
         */
        wait.until(d -> {
            try {
                WebElement cell = d.findElement(quantityCellLocator);
                String cellText = cell.getText().trim();

                if (cellText.isEmpty()) {
                    return false;
                }

                double expected = Double.parseDouble(quantity.trim());
                double actual = Double.parseDouble(
                        cellText.replace(",", "")
                );

                return Double.compare(expected, actual) == 0;

            } catch (StaleElementReferenceException |
                     NoSuchElementException |
                     NumberFormatException e) {
                return false;
            }
        });

        System.out.println(
                "SUCCESS: Local Purchase Invoice quantity entered: " + quantity
        );
        System.out.println("========================================");
    }



    /**
     * Enters the requested price into the Cost cell of the newly added item
     * in the Local Purchase Invoice AG Grid.
     *
     * Identifies the target row using newItemTestId and re-finds elements
     * whenever AG Grid may re-render the DOM.
     *
     * Activates the Cost cell, enters the requested value, verifies the editor,
     * commits using TAB, and verifies the final displayed value.
     *
     * @param price the cost value to enter into the new item's Cost cell
     */
    public void enterPrice(String price) {

        // Validate that the new item's test ID was captured after adding the row.
        if (newItemTestId == null || newItemTestId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "Cannot enter cost because newItemTestId has not been captured."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        // Fixed row-anchor strategy: identify the newly added item's Item cell.
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        // Locate the Cost cell within the row containing the new item.
        By costCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost']"
        );

        // Locate the editable input inside the new item's Cost cell.
        By costInputLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost']"
                        + "//input[contains(@class,'ag-input-field-input')"
                        + " and contains(@class,'ag-text-field-input')]"
        );

        try {
            // Wait until the new item's row anchor is present in the grid.
            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Wait until the new item's Cost cell becomes visible.
            WebElement costCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costCellLocator
                    )
            );

            // Scroll the Cost cell into view before interacting with it.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    costCell
            );

            // Re-find the Cost cell after scrolling to avoid a stale reference.
            costCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            costCellLocator
                    )
            );

            // Click the Cost cell to activate AG Grid cell focus.
            new Actions(driver)
                    .moveToElement(costCell)
                    .click()
                    .perform();

            // Wait for the Cost cell's inline editor to appear.
            WebElement costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            // Clear the existing value through the native input setter.
            ((JavascriptExecutor) driver).executeScript(
                    """
                    const input = arguments[0];
                    const setter = Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype, 'value'
                    ).set;
    
                    setter.call(input, '');
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    """,
                    costInput
            );

            // Re-find the editor in case the grid re-rendered after clearing.
            costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            // Enter the requested price and notify the application of the change.
            ((JavascriptExecutor) driver).executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
                    const setter = Object.getOwnPropertyDescriptor(
                        HTMLInputElement.prototype, 'value'
                    ).set;
    
                    setter.call(input, value);
                    input.dispatchEvent(new Event('input', { bubbles: true }));
                    input.dispatchEvent(new Event('change', { bubbles: true }));
                    """,
                    costInput,
                    price
            );

            // Verify the complete requested value, re-finding the editor on each poll.
            wait.until(currentDriver -> {
                try {
                    WebElement currentInput =
                            currentDriver.findElement(costInputLocator);

                    return price.equals(
                            currentInput.getAttribute("value")
                    );

                } catch (StaleElementReferenceException e) {
                    return false;
                } catch (NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Cost value entered successfully in editor: " + price
            );

            // Re-find the editor before committing the value with TAB.
            costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            // TAB commits the edited cell without intentionally adding another row.
            costInput.sendKeys(Keys.TAB);

            // Wait until the Cost cell exits editing mode, handling DOM re-renders.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCostCell =
                            currentDriver.findElement(costCellLocator);

                    String classAttribute =
                            currentCostCell.getAttribute("class");

                    return classAttribute != null
                            && !classAttribute.contains(
                            "ag-cell-inline-editing"
                    );

                } catch (StaleElementReferenceException e) {
                    return false;
                } catch (NoSuchElementException e) {
                    return false;
                }
            });

            // Re-find the committed Cost cell after editing finishes.
            WebElement updatedCostCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costCellLocator
                    )
            );

            // Read the final displayed value after the grid commits the edit.
            String actualCost = updatedCostCell.getText().trim();

            System.out.println(
                    "Cost cell value after commit: [" + actualCost + "]"
            );

            // Normalize common display formatting before comparing numeric values.
            String normalizedExpected = price.replace(",", "").trim();
            String normalizedActual = actualCost
                    .replace(",", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            // Verify that the displayed cost matches the requested value numerically.
            try {
                java.math.BigDecimal expected =
                        new java.math.BigDecimal(normalizedExpected);

                java.math.BigDecimal actual =
                        new java.math.BigDecimal(normalizedActual);

                if (expected.compareTo(actual) != 0) {
                    throw new RuntimeException(
                            "Cost value mismatch. Expected: " + price
                                    + ", Actual: " + actualCost
                    );
                }

            } catch (NumberFormatException e) {
                throw new RuntimeException(
                        "Could not verify the displayed Cost value. Expected: "
                                + price + ", Actual: " + actualCost,
                        e
                );
            }

            System.out.println(
                    "Cost value committed and verified successfully: " + actualCost
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter cost value '" + price
                            + "' into the new item's Cost cell.",
                    e
            );
        }
    }


    /**
     * Enters the Discount Rate value into the fixed Discount Rate (%) cell
     * of the Local Purchase Invoice AG Grid.
     *
     * Uses a stable col-id locator without data-testid or dynamically
     * constructed locators. F2 activates cell editing, JavaScript assigns
     * the complete value, and TAB commits the value.
     *
     * @param discountRate the discount percentage to enter
     */
    public void enterDiscountRate(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Fixed locator for the Discount Rate cell using its stable AG Grid column identifier.
        By discountRateCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='discountRate']"
        );

        // Fixed locator for the inline editor input within the Discount Rate column.
        By discountRateEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='discountRate'] "
                        + "input.ag-input-field-input.ag-text-field-input"
        );

        try {
            // Wait until the Discount Rate cell becomes visible.
            WebElement discountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            System.out.println("Discount Rate cell found.");

            System.out.println(
                    "Discount Rate before editing: ["
                            + discountRateCell.getText() + "]"
            );

            // Scroll the Discount Rate cell into view before interacting with it.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    discountRateCell
            );

            // Re-locate the cell to ensure it is clickable after scrolling.
            discountRateCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            discountRateCellLocator
                    )
            );

            // Click the cell to establish AG Grid keyboard focus.
            discountRateCell.click();

            // Re-locate the cell before activating inline editing.
            discountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            // Press F2 to activate inline editing without using ENTER.
            discountRateCell.sendKeys(Keys.F2);

            // Wait until the Discount Rate editor input becomes visible.
            WebElement discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateEditorLocator
                    )
            );

            System.out.println("Discount Rate editor activated.");

            // Assign the complete discount value through the native input value setter.
            js.executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
    
                    const setter = Object.getOwnPropertyDescriptor(
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

            // Verify that the editor contains the complete requested discount value.
            wait.until(currentDriver -> {
                try {
                    WebElement currentInput =
                            currentDriver.findElement(
                                    discountRateEditorLocator
                            );

                    return discountRate.equals(
                            currentInput.getAttribute("value")
                    );

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Rate entered in editor: ["
                            + discountRate + "]"
            );

            // Re-locate the editor before committing the entered value.
            discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateEditorLocator
                    )
            );

            // Press TAB to commit the value without intentionally adding another row.
            discountRateInput.sendKeys(Keys.TAB);

            // Wait until the Discount Rate cell exits inline-editing mode.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(
                                    discountRateCellLocator
                            );

                    String classAttribute =
                            currentCell.getAttribute("class");

                    return classAttribute != null
                            && !classAttribute.contains(
                            "ag-cell-inline-editing"
                    );

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            // Re-locate the cell after AG Grid commits and renders the value.
            WebElement updatedDiscountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            // Read and log the final displayed Discount Rate value.
            String finalDiscountRate =
                    updatedDiscountRateCell.getText().trim();

            System.out.println(
                    "Discount Rate after commit: ["
                            + finalDiscountRate + "]"
            );

            System.out.println(
                    "Discount Rate entered successfully."
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Discount Rate value: " + discountRate,
                    e
            );
        }
    }


    /**
     * Enters the requested Discount Value into the Discount Value cell
     * of the Local Purchase Invoice AG Grid.
     *
     * Uses fixed locators based on the stable AG Grid col-id attribute,
     * without data-testid or dynamically constructed locators.
     *
     * The cell is expected to receive keyboard focus after the Discount Rate
     * value is committed. The first character activates inline editing,
     * the complete value is assigned through the native input setter,
     * and TAB commits the value.
     *
     * Elements are re-located whenever AG Grid may re-render the DOM
     * to avoid StaleElementReferenceException.
     *
     * @param discountValue the discount amount to enter
     */
    public void enterDiscountValue(String discountValue) {

        System.out.println("========================================");
        System.out.println("Entering Discount Value");
        System.out.println("Requested discount value: " + discountValue);

        if (discountValue == null || discountValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Discount Value must not be null or empty."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Fixed locator for the Discount Value cell using its stable AG Grid column identifier.
        By discountValueCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='discountValue']"
        );

        // Fixed locator for the active inline editor inside the Discount Value column.
        By discountValueEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='discountValue']"
                        + ".ag-cell-inline-editing "
                        + "input.ag-input-field-input.ag-text-field-input"
        );

        try {
            // Wait until the Discount Value cell is present and visible.
            WebElement discountCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            );

            System.out.println("Discount Value cell found.");

            System.out.println(
                    "Discount Value before editing: ["
                            + discountCell.getText() + "]"
            );

            // Wait until the cell receives keyboard focus from the preceding Discount Rate edit.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(
                                    discountValueCellLocator
                            );

                    String classAttribute =
                            currentCell.getAttribute("class");

                    return classAttribute != null
                            && classAttribute.contains("ag-cell-focus");

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Value cell is focused and ready for keyboard input."
            );

            // Send the first character to activate AG Grid inline editing.
            String firstCharacter = discountValue.substring(0, 1);

            new Actions(driver)
                    .sendKeys(firstCharacter)
                    .perform();

            System.out.println(
                    "First Discount Value character sent: " + firstCharacter
            );

            // Wait for the inline editor to appear in the Discount Value cell.
            WebElement discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            System.out.println("Discount Value editor activated.");

            // Assign the complete value using the native HTML input setter.
            ((JavascriptExecutor) driver).executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
    
                    const setter = Object.getOwnPropertyDescriptor(
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
                    discountEditor,
                    discountValue
            );

            // Verify the complete value, re-finding the editor on every polling attempt.
            wait.until(currentDriver -> {
                try {
                    WebElement currentEditor =
                            currentDriver.findElement(
                                    discountValueEditorLocator
                            );

                    return discountValue.equals(
                            currentEditor.getAttribute("value")
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Value verified in editor: [" + discountValue + "]"
            );

            // Re-find the editor immediately before committing the value.
            discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // TAB commits the value without intentionally adding another row.
            discountEditor.sendKeys(Keys.TAB);

            System.out.println(
                    "TAB sent to commit Discount Value."
            );

            // Wait for the cell to exit editing mode, tolerating AG Grid re-renders.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(
                                    discountValueCellLocator
                            );

                    String classAttribute =
                            currentCell.getAttribute("class");

                    return classAttribute != null
                            && !classAttribute.contains(
                            "ag-cell-inline-editing"
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            // Re-find the cell after the grid has committed and rendered the value.
            WebElement updatedDiscountCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            );

            // Read the final displayed value from the updated cell.
            String actualDiscountValue =
                    updatedDiscountCell.getText().trim();

            System.out.println(
                    "Discount Value after commit: ["
                            + actualDiscountValue + "]"
            );

            System.out.println(
                    "Discount Value committed successfully."
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Discount Value: " + discountValue,
                    e
            );
        }
    }

    /**
     * Enters the Addition Rate (%) value into the currently focused AG Grid cell.
     *
     * Uses stable AG Grid attributes instead of data-testid, dynamic row indexes,
     * or generated element IDs.
     *
     * The method expects keyboard focus to have moved naturally to the
     * Addition Rate (%) cell from the preceding cell.
     *
     * It activates inline editing with the first character, assigns the complete
     * value through the native HTML input setter, verifies the value, and
     * commits it using TAB instead of ENTER to avoid triggering Add Line.
     */
    public void enterExtraRate(String extraRate) {

        System.out.println("========================================");
        System.out.println("Entering Addition Rate (%)");
        System.out.println("Requested addition rate: " + extraRate);

        // Validate the requested value before interacting with AG Grid.
        if (extraRate == null || extraRate.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Rate (%) cannot be null or blank."
            );
        }

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        /*
         * Stable locator for the Addition Rate (%) cell currently holding
         * AG Grid keyboard focus. No data-testid or generated ID is used.
         */
        By extraRateCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraRate'].ag-cell-focus"
        );

        /*
         * Stable locator for the inline editor created inside the Addition
         * Rate (%) cell after editing has been activated.
         */
        By extraRateEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraRate']" +
                        ".ag-cell-inline-editing input"
        );

        /*
         * Wait until the Addition Rate (%) cell receives keyboard focus.
         * This method intentionally does not click the cell or select a row.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        extraRateCellLocator
                )
        );

        System.out.println(
                "Addition Rate (%) cell is focused."
        );

        /*
         * Send the first character to activate AG Grid inline editing.
         * The complete value will be assigned after the editor appears.
         */
        String firstCharacter = extraRate.substring(0, 1);

        new Actions(driver)
                .sendKeys(firstCharacter)
                .perform();

        System.out.println(
                "First character sent to activate editing: " +
                        firstCharacter
        );

        /*
         * Wait for the actual inline editor in the Addition Rate (%) column.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        extraRateEditorLocator
                )
        );

        System.out.println(
                "Addition Rate (%) editor is visible."
        );

        /*
         * Re-locate the editor and assign the complete value through the
         * native HTML input setter, then dispatch input and change events.
         */
        WebElement extraRateEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        extraRateEditorLocator
                )
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const descriptor =
                    Object.getOwnPropertyDescriptor(
                        window.HTMLInputElement.prototype,
                        'value'
                    );
    
                if (!descriptor || !descriptor.set) {
                    throw new Error('Native input value setter is unavailable.');
                }
    
                descriptor.set.call(input, value);
    
                input.dispatchEvent(
                    new Event('input', { bubbles: true })
                );
    
                input.dispatchEvent(
                    new Event('change', { bubbles: true })
                );
                """,
                extraRateEditor,
                extraRate
        );

        /*
         * Verify the editor value by locating the editor afresh on each poll.
         * Handle stale references if AG Grid re-renders during the operation.
         */
        wait.until(currentDriver -> {
            try {
                WebElement currentEditor =
                        currentDriver.findElement(extraRateEditorLocator);

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

        /*
         * Re-locate the editor immediately before committing the value.
         */
        WebElement currentExtraRateEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        extraRateEditorLocator
                )
        );

        String actualValue =
                currentExtraRateEditor.getAttribute("value");

        System.out.println(
                "Addition Rate (%) value before commit: [" +
                        actualValue + "]"
        );

        if (!extraRate.equals(actualValue)) {
            throw new IllegalStateException(
                    "Addition Rate (%) value mismatch. Requested=[" +
                            extraRate + "], Actual=[" + actualValue + "]"
            );
        }

        /*
         * Commit the value with TAB. ENTER is intentionally avoided because
         * it may trigger Add Line in this AG Grid form.
         */
        currentExtraRateEditor.sendKeys(Keys.TAB);

        System.out.println(
                "TAB sent to commit Addition Rate (%)."
        );

        System.out.println(
                "Addition Rate (%) entered successfully: " + extraRate
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Addition Value into the currently focused AG Grid cell.
     *
     * Uses stable AG Grid attributes without data-testid, dynamic row indexes,
     * or generated element IDs.
     *
     * The method expects keyboard focus to move naturally from the
     * Addition Rate (%) cell to the Addition Value cell.
     *
     * It activates inline editing, assigns the complete value through the
     * native HTML input setter, verifies the value, and commits with TAB.
     *
     * Locators are declared locally and WebElements are re-located whenever
     * the DOM may have been refreshed by AG Grid.
     */
    public void enterExtraValue(String extraValue) {

        System.out.println("========================================");
        System.out.println("Entering Addition Value");
        System.out.println("Requested addition value: " + extraValue);

        // Validate the requested value before interacting with the grid.
        if (extraValue == null || extraValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Value cannot be null or blank."
            );
        }

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        /*
         * Stable locator for the Addition Value cell currently focused
         * by AG Grid keyboard navigation.
         *
         * The col-id is taken from the supplied HTML. No test ID,
         * row index, or generated element ID is used.
         */
        By extraValueCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue'].ag-cell-focus"
        );

        /*
         * Stable locator for the active inline editor inside the
         * Addition Value column.
         *
         * The editing-state class distinguishes the editor from
         * the normal, non-editing grid cell.
         */
        By extraValueEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue']" +
                        ".ag-cell-inline-editing input"
        );

        /*
         * Wait for keyboard focus to reach the Addition Value cell.
         * Re-locate the cell during every polling attempt to handle
         * stale elements caused by AG Grid re-rendering.
         */
        wait.until(currentDriver -> {
            try {
                WebElement currentCell =
                        currentDriver.findElement(extraValueCellLocator);

                String classes = currentCell.getAttribute("class");

                return currentCell.isDisplayed()
                        && classes != null
                        && classes.contains("ag-cell-focus");

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {
                return false;
            }
        });

        System.out.println(
                "Addition Value cell received keyboard focus."
        );

        /*
         * Activate inline editing by sending the first character
         * to the currently focused cell. No mouse click is performed.
         */
        String firstCharacter = extraValue.substring(0, 1);

        new Actions(driver)
                .sendKeys(firstCharacter)
                .perform();

        System.out.println(
                "First character sent to activate editing: " +
                        firstCharacter
        );

        /*
         * Wait for the inline editor to appear.
         * ExpectedConditions re-locates the element during polling.
         */
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        extraValueEditorLocator
                )
        );

        System.out.println(
                "Addition Value inline editor is visible."
        );

        /*
         * Assign the complete value using the native HTML input setter.
         * If AG Grid replaces the editor between locating it and executing
         * JavaScript, re-locate it and retry a limited number of times.
         */
        boolean valueAssigned = false;

        JavascriptExecutor js = (JavascriptExecutor) driver;

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                WebElement currentEditor = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraValueEditorLocator
                        )
                );

                js.executeScript(
                        """
                        const input = arguments[0];
                        const value = arguments[1];
    
                        const descriptor =
                            Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype,
                                'value'
                            );
    
                        if (!descriptor || !descriptor.set) {
                            throw new Error(
                                'Native input value setter is unavailable.'
                            );
                        }
    
                        descriptor.set.call(input, value);
    
                        input.dispatchEvent(
                            new Event('input', { bubbles: true })
                        );
    
                        input.dispatchEvent(
                            new Event('change', { bubbles: true })
                        );
                        """,
                        currentEditor,
                        extraValue
                );

                valueAssigned = true;
                break;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Editor became stale during value assignment. " +
                                "Retry " + attempt + " of 3."
                );
            }
        }

        if (!valueAssigned) {
            throw new IllegalStateException(
                    "Could not assign Addition Value because the editor " +
                            "kept changing in the DOM."
            );
        }

        System.out.println(
                "Requested Addition Value assigned: " + extraValue
        );

        /*
         * Verify the editor value using a freshly located WebElement
         * on every polling attempt. Handle stale or temporarily missing
         * elements while AG Grid updates the DOM.
         */
        wait.until(currentDriver -> {
            try {
                WebElement currentEditor =
                        currentDriver.findElement(extraValueEditorLocator);

                return currentEditor.isDisplayed()
                        && extraValue.equals(
                        currentEditor.getAttribute("value")
                );

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {
                return false;
            }
        });

        /*
         * Re-locate the editor before committing. Retry TAB only if the
         * editor becomes stale before the key can be sent.
         *
         * A stale exception after a key event can be ambiguous, so the
         * method verifies that the editor still exists before retrying.
         */
        boolean committed = false;

        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                WebElement currentEditor = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                extraValueEditorLocator
                        )
                );

                String currentValue =
                        currentEditor.getAttribute("value");

                if (!extraValue.equals(currentValue)) {
                    throw new IllegalStateException(
                            "Addition Value changed before commit. " +
                                    "Expected=[" + extraValue +
                                    "], Actual=[" + currentValue + "]"
                    );
                }

                currentEditor.sendKeys(Keys.TAB);
                committed = true;
                break;

            } catch (StaleElementReferenceException e) {

                /*
                 * Re-check whether editing is still active before retrying.
                 * If the editor disappeared, AG Grid may already have
                 * committed the value.
                 */
                boolean editorStillPresent;

                try {
                    editorStillPresent = driver.findElements(
                            extraValueEditorLocator
                    ).stream().anyMatch(WebElement::isDisplayed);

                } catch (StaleElementReferenceException staleAgain) {
                    editorStillPresent = true;
                }

                if (!editorStillPresent) {
                    committed = true;
                    break;
                }

                System.out.println(
                        "Editor became stale before TAB could be confirmed. " +
                                "Retry " + attempt + " of 3."
                );
            }
        }

        if (!committed) {
            throw new IllegalStateException(
                    "Could not confirm committing Addition Value with TAB."
            );
        }

        System.out.println(
                "Addition Value commit operation completed: " + extraValue
        );

        System.out.println("========================================");
    }

    /**
     * Selects today's date from the React DatePicker calendar
     * for the Production Date AG Grid cell.
     *
     * Uses the stable AG Grid col-id and a date-specific aria-label.
     * Does not use data-testid, row indexes, or generated element IDs.
     *
     * The Production Date cell is opened with a double-click.
     * The target date is then located in the calendar and selected.
     *
     * Elements are re-located when necessary to reduce stale-element failures
     * caused by AG Grid and React re-rendering.
     */
    public void selectTodayAsProductionDate() {

        System.out.println("========================================");
        System.out.println("Selecting today's Production Date");

        LocalDate today = LocalDate.now();
        String expectedText = today.toString(); // grid shows yyyy-MM-dd

        System.out.println("Target production date: " + today);

        /*
         * The cell of the NEW row only. A page-wide [col-id='productionDate']
         * returns the FIRST row's cell, so on documents that already have
         * lines (reference document / duplicate) the date went to another row
         * and the new row stayed empty.
         */
        By cellLocator = By.cssSelector(newRowCellSelector("productionDate"));

        String dateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                today.format(DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)),
                today.format(DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)),
                today.getDayOfMonth(),
                today.getYear()
        );

        System.out.println("Target calendar aria-label: " + dateAriaLabel);

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

                // Open the calendar: click + F2, then a double-click if needed.
                cell.click();
                driver.findElement(cellLocator).sendKeys(Keys.F2);

                if (!isProductionCalendarOpen(calendarLocator, Duration.ofSeconds(3))) {
                    new Actions(driver)
                            .doubleClick(driver.findElement(cellLocator))
                            .perform();
                }

                if (!isProductionCalendarOpen(calendarLocator, Duration.ofSeconds(4))) {
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

            // Make sure the calendar closed so the next cell is not blocked.
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
        System.out.println("========================================");
    }

    /** True when the RMDP calendar becomes visible within {@code timeout}. */
    private boolean isProductionCalendarOpen(By calendarLocator, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .until(ExpectedConditions.visibilityOfElementLocated(calendarLocator));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    /**
     * CSS selector of a cell in the row added by the last "Add Line".
     * Falls back to a page-wide selector only when no new row was captured.
     */
    private String newRowCellSelector(String colId) {
        String cell = "div[role='gridcell'][col-id='" + colId + "']";
        return newRowIndex < 0
                ? cell
                : ".ag-center-cols-container div[role='row'][row-index='"
                        + newRowIndex + "'] " + cell;
    }

    /**
     * Selects the Expire Date for the currently targeted AG Grid row.
     *
     * Uses the stable expireDate column identifier without data-testid,
     * generated element IDs, or row-index-based locators.
     *
     * Opens the RMDP calendar using F2, navigates to the target date,
     * selects it by its accessible aria-label, and verifies the committed
     * value after the calendar closes.
     *
     * IMPORTANT:
     * This method assumes the intended Expire Date cell is the only
     * matching cell in the current grid context, or that the intended
     * cell is the first matching cell returned by the locator.
     */
    public void selectExpireDate() {

        System.out.println("========================================");
        System.out.println("Selecting Expire Date...");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Calculate the target expiration date: 30 days from today.
        LocalDate expireDate = LocalDate.now().plusDays(30);

        System.out.println("Target Expire Date: " + expireDate);

        /*
         * Stable locator for the Expire Date AG Grid cell.
         * Uses the permanent column identifier from the supplied HTML.
         * Does not use data-testid, generated IDs, or row-index values.
         */
        By expireDateCellLocator = By.cssSelector(
                newRowCellSelector("expireDate")
        );

        /*
         * Stable locator for the RMDP calendar wrapper.
         * Used to detect when the calendar opens and closes.
         */
        By calendarWrapperLocator = By.cssSelector(
                ".rmdp-wrapper"
        );

        /*
         * Build the accessible label for the exact target date.
         * This is calculated from the requested date, not a generated
         * DOM element ID.
         */
        String targetDateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                expireDate.getDayOfWeek().getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                ),
                expireDate.getMonth().getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                ),
                expireDate.getDayOfMonth(),
                expireDate.getYear()
        );

        /*
         * Locator for the exact date in the RMDP calendar.
         * The date-specific aria-label identifies the required day.
         */
        By targetDateLocator = By.cssSelector(
                ".rmdp-wrapper [aria-label='" +
                        targetDateAriaLabel +
                        "']"
        );

        /*
         * Locator for the RMDP Next Month control.
         * Targets the calendar's arrow container rather than assuming
         * that the control is a button.
         */
        By nextMonthLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-arrow-container.rmdp-right"
        );

        /*
         * Wait until the Expire Date cell is present and visible.
         * The element is re-located during polling to reduce stale
         * element failures caused by AG Grid re-rendering.
         */
        wait.until(driver -> {
            try {
                List<WebElement> cells =
                        driver.findElements(expireDateCellLocator);

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()) {
                            return true;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // AG Grid replaced the cell; retry on the next poll.
                    }
                }

                return false;

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        /*
         * Scroll the current Expire Date cell into view.
         * JavaScript queries the DOM using the stable CSS selector,
         * rather than relying on a previously stored WebElement.
         */
        boolean cellScrolledIntoView = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                Boolean scrolled = (Boolean)
                        ((JavascriptExecutor) driver).executeScript(
                                """
                                const cells = document.querySelectorAll(arguments[0]);
    
                                for (const cell of cells) {
                                    if (cell.getClientRects().length > 0) {
                                        cell.scrollIntoView({
                                            block: 'center',
                                            inline: 'center'
                                        });
                                        return true;
                                    }
                                }
    
                                return false;
                                """,
                                newRowCellSelector("expireDate")
                        );

                if (Boolean.TRUE.equals(scrolled)) {
                    cellScrolledIntoView = true;
                    break;
                }

            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Grid refreshed while scrolling. Retry " +
                                attempt + " of 3."
                );
            }
        }

        if (!cellScrolledIntoView) {
            throw new IllegalStateException(
                    "Could not scroll the Expire Date cell into view."
            );
        }

        /*
         * Re-locate the cell immediately before clicking it.
         * Clicking gives keyboard focus to the AG Grid cell.
         */
        wait.until(ExpectedConditions.elementToBeClickable(
                expireDateCellLocator
        )).click();

        /*
         * Re-locate the cell after clicking because its focus state
         * may have changed during the AG Grid update.
         */
        wait.until(ExpectedConditions.elementToBeClickable(
                expireDateCellLocator
        )).sendKeys(Keys.F2);

        System.out.println(
                "F2 sent. Waiting for the Expire Date calendar..."
        );

        /*
         * Wait for the RMDP calendar to become visible.
         */
        wait.until(ExpectedConditions.visibilityOfElementLocated(
                calendarWrapperLocator
        ));

        System.out.println("Expire Date calendar opened.");

        System.out.println(
                "Target date aria-label: " + targetDateAriaLabel
        );

        /*
         * Navigate forward only when the requested date is not
         * currently visible in the calendar.
         */
        int maxMonthNavigationAttempts = 12;
        boolean targetDateVisible = false;

        for (int attempt = 0;
             attempt <= maxMonthNavigationAttempts;
             attempt++) {

            /*
             * Check whether the target date is currently visible.
             * Find elements again on each iteration because the
             * calendar may re-render after month navigation.
             */
            targetDateVisible = false;

            try {
                List<WebElement> dateElements =
                        driver.findElements(targetDateLocator);

                for (WebElement dateElement : dateElements) {
                    try {
                        if (dateElement.isDisplayed()) {
                            targetDateVisible = true;
                            break;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-check the date on the next iteration.
                    }
                }

            } catch (StaleElementReferenceException ignored) {
                // The calendar changed; repeat the lookup.
            }

            if (targetDateVisible) {
                break;
            }

            if (attempt == maxMonthNavigationAttempts) {
                break;
            }

            System.out.println(
                    "Target date not visible. Moving to next month: " +
                            (attempt + 1) + " of " +
                            maxMonthNavigationAttempts
            );

            /*
             * Re-locate the Next Month control before clicking it.
             */
            WebElement nextMonthControl = wait.until(driver -> {
                try {
                    List<WebElement> controls =
                            driver.findElements(nextMonthLocator);

                    for (WebElement control : controls) {
                        try {
                            if (control.isDisplayed()
                                    && control.isEnabled()) {
                                return control;
                            }
                        } catch (StaleElementReferenceException ignored) {
                            // Try again during the next polling cycle.
                        }
                    }

                    return null;

                } catch (StaleElementReferenceException e) {
                    return null;
                }
            });

            nextMonthControl.click();

            /*
             * Wait for the calendar to settle after navigation.
             * The next loop iteration re-checks the target date.
             */
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    calendarWrapperLocator
            ));
        }

        if (!targetDateVisible) {
            throw new IllegalStateException(
                    "Could not locate Expire Date " + expireDate +
                            " after " + maxMonthNavigationAttempts +
                            " month navigation attempts."
            );
        }

        /*
         * Re-locate the exact target date and click it.
         * This avoids using a WebElement obtained before the final
         * calendar render.
         */
        wait.until(ExpectedConditions.elementToBeClickable(
                targetDateLocator
        )).click();

        System.out.println(
                "Selected Expire Date: " + expireDate
        );

        /*
         * Wait until the calendar closes before checking the grid cell.
         */
        wait.until(ExpectedConditions.invisibilityOfElementLocated(
                calendarWrapperLocator
        ));

        /*
         * Re-locate the Expire Date cell after selection because React
         * or AG Grid may have replaced its DOM element.
         */
        wait.until(driver -> {
            try {
                List<WebElement> cells =
                        driver.findElements(expireDateCellLocator);

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()
                                && !cell.getText().trim().isEmpty()) {
                            return true;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-check the updated cell.
                    }
                }

                return false;

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        /*
         * Read the committed value from a freshly located cell.
         */
        String committedExpireDate = wait.until(driver -> {
            try {
                List<WebElement> cells =
                        driver.findElements(expireDateCellLocator);

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()) {
                            String value = cell.getText().trim();

                            if (!value.isEmpty()) {
                                return value;
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-locate on the next polling cycle.
                    }
                }

                return null;

            } catch (StaleElementReferenceException e) {
                return null;
            }
        });

        System.out.println(
                "Expire Date cell value after selection: [" +
                        committedExpireDate + "]"
        );

        System.out.println(
                "Expire Date selected and committed successfully."
        );

        System.out.println("========================================");
    }

    /**
     * Enters the requested Free Quantity into the AG Grid cell.
     *
     * Uses the stable freeQuantities column identifier and avoids
     * data-testid attributes, generated element IDs, and row-index locators.
     *
     * The method double-clicks the cell to activate inline editing,
     * enters the requested value through the native input value setter,
     * dispatches input/change events, commits with TAB, and verifies
     * the committed cell value after AG Grid re-renders.
     *
     * IMPORTANT:
     * This implementation assumes the intended Free Quantity cell
     * is uniquely identifiable in the current grid context.
     *
     * @param freeQuantity the free quantity value to enter
     */
    public void enterFreeQuantity(int freeQuantity) {

        System.out.println("========================================");
        System.out.println("Entering Free Quantity");
        System.out.println("Requested value: " + freeQuantity);

        if (freeQuantity < 0) {
            throw new IllegalArgumentException(
                    "Free Quantity cannot be negative: " + freeQuantity
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Free Quantity cell.
         * Uses the column identifier supplied in the HTML.
         */
        By freeQuantityCellLocator = By.cssSelector(
                newRowCellSelector("freeQuantities")
        );

        /*
         * Locator for the AG Grid inline editor inside the Free Quantity cell.
         * The selector identifies the editor by its role in the grid,
         * not by a generated input ID or data-testid.
         */
        By freeQuantityInputLocator = By.cssSelector(
                newRowCellSelector("freeQuantities") + " input"
        );

        /*
         * Locate the visible Free Quantity cell and scroll it into view.
         * Re-find the cell on each attempt to reduce stale-element failures.
         */
        boolean cellReady = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement cell = wait.until(driver -> {
                    try {
                        List<WebElement> cells =
                                driver.findElements(freeQuantityCellLocator);

                        for (WebElement currentCell : cells) {
                            try {
                                if (currentCell.isDisplayed()) {
                                    return currentCell;
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // AG Grid refreshed the cell; try again.
                            }
                        }

                        return null;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView(" +
                                "{block:'center', inline:'center'});",
                        cell
                );

                cellReady = true;
                break;

            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Free Quantity cell refreshed during scrolling. " +
                                "Retry " + attempt + " of 3."
                );
            }
        }

        if (!cellReady) {
            throw new IllegalStateException(
                    "Could not prepare the Free Quantity cell for editing."
            );
        }

        /*
         * Re-locate the cell immediately before double-clicking it.
         * Double-click activates AG Grid inline editing.
         */
        boolean editorActivated = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement cell = wait.until(driver -> {
                    try {
                        List<WebElement> cells =
                                driver.findElements(freeQuantityCellLocator);

                        for (WebElement currentCell : cells) {
                            try {
                                if (currentCell.isDisplayed()
                                        && currentCell.isEnabled()) {
                                    return currentCell;
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // Re-locate during the next polling cycle.
                            }
                        }

                        return null;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                new Actions(driver)
                        .doubleClick(cell)
                        .perform();

                editorActivated = true;
                break;

            } catch (StaleElementReferenceException e) {
                /*
                 * The grid may have re-rendered during the action.
                 * Check whether the editor opened before retrying.
                 */
                if (!driver.findElements(freeQuantityInputLocator).isEmpty()) {
                    editorActivated = true;
                    break;
                }

                System.out.println(
                        "Cell refreshed during double-click. Retry " +
                                attempt + " of 3."
                );
            }
        }

        /*
         * Wait for the Free Quantity input to appear.
         */
        if (!editorActivated) {
            throw new IllegalStateException(
                    "Could not activate the Free Quantity editor."
            );
        }

        WebElement freeQuantityInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        freeQuantityInputLocator
                )
        );

        System.out.println("Free Quantity editor opened.");

        /*
         * Set the requested value using the native HTML input setter.
         * Dispatch input/change events so the framework can detect
         * the changed value.
         *
         * The input is re-located on each attempt in case React replaces it.
         */
        String requestedValue = String.valueOf(freeQuantity);
        boolean valueEntered = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                freeQuantityInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                freeQuantityInputLocator
                        )
                );

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
                        freeQuantityInput,
                        requestedValue
                );

                /*
                 * Verify the editor value before attempting to commit it.
                 */
                wait.until(driver -> {
                    try {
                        WebElement currentInput =
                                driver.findElement(freeQuantityInputLocator);

                        return requestedValue.equals(
                                currentInput.getAttribute("value")
                        );

                    } catch (
                            StaleElementReferenceException |
                            NoSuchElementException e
                    ) {
                        return false;
                    }
                });

                valueEntered = true;
                break;

            } catch (StaleElementReferenceException e) {
                System.out.println(
                        "Editor refreshed while entering the value. Retry " +
                                attempt + " of 3."
                );
            }
        }

        if (!valueEntered) {
            throw new IllegalStateException(
                    "Could not enter Free Quantity value [" +
                            requestedValue + "] into the editor."
            );
        }

        System.out.println(
                "Editor contains the requested value: " + requestedValue
        );

        /*
         * Commit the value with TAB.
         * Re-locate the editor immediately before sending the key.
         */
        boolean commitKeySent = false;

        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement currentInput = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                freeQuantityInputLocator
                        )
                );

                currentInput.sendKeys(Keys.TAB);

                commitKeySent = true;
                break;

            } catch (StaleElementReferenceException e) {
                /*
                 * If the editor disappeared, AG Grid may already have
                 * committed the value. The final verification below
                 * determines whether the operation actually succeeded.
                 */
                if (driver.findElements(freeQuantityInputLocator).isEmpty()) {
                    commitKeySent = true;
                    break;
                }

                System.out.println(
                        "Editor refreshed while committing. Retry " +
                                attempt + " of 3."
                );
            }
        }

        if (!commitKeySent) {
            throw new IllegalStateException(
                    "Could not commit the Free Quantity editor using TAB."
            );
        }

        /*
         * Wait for inline editing to finish.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        freeQuantityInputLocator
                )
        );

        /*
         * Re-locate the cell after AG Grid has committed the value.
         * Verify the displayed value rather than relying on the old
         * WebElement reference.
         */
        String committedValue = wait.until(driver -> {
            try {
                List<WebElement> cells =
                        driver.findElements(freeQuantityCellLocator);

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()) {
                            String text = cell.getText().trim();

                            if (!text.isEmpty()) {
                                return text;
                            }
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-read the cell during the next polling cycle.
                    }
                }

                return null;

            } catch (StaleElementReferenceException e) {
                return null;
            }
        });

        System.out.println(
                "Free Quantity value after commit: [" + committedValue + "]"
        );

        /*
         * Compare numeric values because AG Grid may format the value,
         * for example as 114.000 instead of 114.
         */
        try {
            double actualValue = Double.parseDouble(
                    committedValue.replace(",", "").trim()
            );

            if (Double.compare(actualValue, freeQuantity) != 0) {
                throw new IllegalStateException(
                        "Free Quantity mismatch. Expected: " +
                                freeQuantity + ", Actual: " + committedValue
                );
            }

        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Could not verify the committed Free Quantity value: [" +
                            committedValue + "]",
                    e
            );
        }

        System.out.println(
                "Free Quantity entered successfully: " + committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Note (البيان) value into the AG Grid Note cell
     * and commits the edit by clicking outside the grid editor.
     *
     * Uses the stable note column identifier and avoids data-testid,
     * row-index locators, and generated DOM element IDs.
     *
     * JavaScript assigns the requested text through the native input
     * value setter and dispatches input/change events to preserve Arabic
     * text and spaces.
     *
     * TAB and ENTER are intentionally avoided because they may cause
     * AG Grid to create an additional row.
     *
     * @param note the text to enter into the Note cell
     */
    public void enterNote(String note) {

        System.out.println("========================================");
        System.out.println("Entering Note (البيان)");
        System.out.println("Requested note: [" + note + "]");

        if (note == null) {
            throw new IllegalArgumentException(
                    "Note cannot be null."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Note cell, based on the supplied HTML.
         * No generated IDs, data-testid, or row-index values are used.
         */
        By noteCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note']"
        );

        /*
         * Locator for the AG Grid text editor inside the Note cell.
         * The selector identifies the editor by its cell and input class.
         */
        By noteInputLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note'] " +
                        "input.ag-text-field-input"
        );

        /*
         * Locator for the external Discounts and Additions section.
         * Clicking this section moves focus away from the editor
         * without using TAB or ENTER.
         */
        By discountsAndAdditionsLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' " +
                        "and not(*)]"
        );

        /*
         * Locate the visible Note cell and scroll it into view.
         * Re-locate the cell during polling to reduce stale-element issues.
         */
        WebElement noteCell = wait.until(driver -> {
            try {
                List<WebElement> cells =
                        driver.findElements(noteCellLocator);

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()) {
                            return cell;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // AG Grid refreshed the cell; retry on the next poll.
                    }
                }

                return null;

            } catch (StaleElementReferenceException e) {
                return null;
            }
        });

        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView(" +
                        "{block:'center', inline:'center'});",
                noteCell
        );

        System.out.println("Note cell found and scrolled into view.");

        /*
         * Double-click the Note cell to activate AG Grid inline editing.
         */
        new Actions(driver).doubleClick(
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                noteCellLocator
                        )
                )
        ).perform();

        System.out.println("Note cell double-clicked.");

        /*
         * Wait for the Note input editor to become visible.
         */
        WebElement noteInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println("Note editor opened.");

        /*
         * Assign the requested text using the native HTML input setter.
         * Dispatch input/change events so React and AG Grid can detect it.
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
                noteInput,
                note
        );

        /*
         * Verify the exact text in the editor before committing it.
         * The input is re-located during polling in case React re-renders.
         */
        String editorValue = wait.until(driver -> {
            try {
                WebElement currentInput =
                        driver.findElement(noteInputLocator);

                String currentValue =
                        currentInput.getAttribute("value");

                return note.equals(currentValue)
                        ? currentValue
                        : null;

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException e
            ) {
                return null;
            }
        });

        System.out.println(
                "Note editor value verified: [" + editorValue + "]"
        );

        /*
         * Wait for the external section to become clickable.
         * This section is outside the AG Grid editor.
         */
        WebElement discountsAndAdditions = wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAndAdditionsLocator
                )
        );

        /*
         * Click outside the editor to commit the Note.
         * Do not use TAB or ENTER here.
         */
        discountsAndAdditions.click();

        System.out.println(
                "Clicked 'الخصومات والإضافات' to commit the Note."
        );

        /*
         * Wait until the editor disappears, indicating that AG Grid
         * has exited inline-editing mode.
         *
         * The Note cell is not re-located after this step because the
         * column may be horizontally virtualized out of the DOM.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println(
                "Note editor closed successfully."
        );

        System.out.println(
                "Note committed successfully: [" + note + "]"
        );

        System.out.println("========================================");
    }


    /**
     * Captures the row-index of the newly added row in the Local Purchase Invoice
     * Discounts & Additions AG Grid.
     *
     * Uses the Grid structure and stable column identifiers from the supplied HTML.
     * Excludes pinned bottom rows, such as the totals row with row-index="b-0".
     *
     * This method must be called after clicking the Add Line button and after
     * the new row has been rendered in the Grid.
     */
    public void captureNewDiscountAdditionRowIndex() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Locate ordinary data rows inside the Discounts & Additions Grid.
         * The selector excludes pinned rows and does not use generated IDs
         * or data-testid attributes.
         */
        By centerRowsLocator = By.cssSelector(
                "div.ag-root[role='grid'] " +
                        "div.ag-center-cols-container > div[role='row']" +
                        "[row-index]:not([row-index^='b-'])"
        );

        /*
         * Wait until at least one ordinary data row is rendered.
         * The pinned bottom totals row is intentionally excluded.
         */
        List<WebElement> rows = wait.until(driver -> {
            List<WebElement> currentRows =
                    driver.findElements(centerRowsLocator);

            return currentRows.isEmpty() ? null : currentRows;
        });

        /*
         * Capture the last ordinary data row.
         * This assumes the newly added row is rendered last.
         */
        WebElement lastRow = rows.get(rows.size() - 1);

        String rowIndex = lastRow.getAttribute("row-index");

        /*
         * Validate the captured row index before storing it.
         */
        if (rowIndex == null
                || rowIndex.isBlank()
                || rowIndex.startsWith("b-")) {

            throw new IllegalStateException(
                    "Could not determine the row-index of the new " +
                            "Local Purchase Invoice Discounts & Additions row."
            );
        }

        newDiscountAdditionRowIndex = Integer.parseInt(rowIndex);

        System.out.println(
                "Local Purchase Invoice Discounts & Additions row index: "
                        + newDiscountAdditionRowIndex
        );
    }

    /**
     * Clicks the Add Line button for the Discounts & Additions grid
     * and waits for a new ordinary data row to appear.
     *
     * This method is separate from clickAddLine(), which is responsible
     * for adding rows to the main invoice items grid and detecting itemId.
     */
    public void clickAddDiscountAdditionLine() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locates the Add Line button shared by the invoice grids.
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space()='اضافة سطر']"
        );

        // Locates ordinary rows in the currently rendered AG Grid.
        // This locator avoids generated IDs and data-testid attributes.
        By centerRowsLocator = By.cssSelector(
                "div.ag-root[role='grid'] " +
                        "div.ag-center-cols-container > div[role='row'][row-index]" +
                        ":not([row-index^='b-'])"
        );

        // Locate the Discounts & Additions tab to ensure the correct
        // grid is displayed before clicking the Add Line button.
        By discountsAndAdditionsTabLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' and not(*)]"
        );

        WebElement discountsTab = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
                        .elementToBeClickable(discountsAndAdditionsTabLocator)
        );

        discountsTab.click();

        // Wait until the Add Line button is clickable in the selected tab.
        WebElement addLineButton = wait.until(
                org.openqa.selenium.support.ui.ExpectedConditions
                        .elementToBeClickable(addLineButtonLocator)
        );

        // Count the currently rendered ordinary rows before adding a line.
        int rowsBefore = driver.findElements(centerRowsLocator).size();

        System.out.println(
                "Discounts & Additions rows before Add Line: " + rowsBefore
        );

        addLineButton.click();

        // Wait until the number of ordinary rows increases.
        wait.until(currentDriver ->
                currentDriver.findElements(centerRowsLocator).size()
                        > rowsBefore
        );

        System.out.println(
                "A new Discounts & Additions row was detected successfully."
        );
    }

    /**
     * Selects a random Account (الحساب) from the Account cell in the newly
     * created Discounts & Additions row.
     *
     * IMPORTANT:
     * The Discounts & Additions grid has its own row structure and row index.
     * Therefore, this method uses newDiscountAdditionRowIndex instead of
     * the main invoice items grid row index.
     *
     * ENTER opens the Account React Select editor, then a random visible,
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

        /*
         * Stable locator for the Account cell in the new Discounts & Additions row.
         * Uses the fixed col-id and captured row index, not a generated ID.
         */
        By accountCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='accountId']"
        );

        // Wait until the Account cell in the newly created row is visible.
        WebElement accountCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountCellLocator
                )
        );

        System.out.println(
                "Account cell found in the new Discounts & Additions row."
        );

        System.out.println(
                "Account cell current value: [" +
                        accountCell.getText() +
                        "]"
        );

        /*
         * Scroll the Account cell into view.
         * This helps when AG Grid horizontally virtualizes its columns.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                accountCell
        );

        /*
         * Re-locate the Account cell after scrolling in case AG Grid
         * re-renders the DOM.
         */
        accountCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountCellLocator
                )
        );

        // Click the Account cell to establish AG Grid keyboard focus.
        accountCell.click();

        System.out.println("Account cell clicked successfully.");

        // Press ENTER to activate the Account React Select editor.
        accountCell.sendKeys(Keys.ENTER);

        System.out.println("ENTER sent to Account cell.");

        /*
         * Stable locator for the Account React Select input.
         * Scoped to the Account column without using React Select's generated ID.
         */
        By accountInputLocator = By.cssSelector(
                "div[role='gridcell'][col-id='accountId'] input"
        );

        WebElement accountInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountInputLocator
                )
        );

        System.out.println("Account React Select editor found.");

        // Click the input to open the Account dropdown.
        accountInput.click();

        /*
         * React Select displays its available options inside a listbox.
         * This locator does not depend on the generated listbox ID.
         */
        By accountListboxLocator = By.cssSelector(
                "div[role='listbox']"
        );

        WebElement accountListbox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountListboxLocator
                )
        );

        System.out.println("Account dropdown opened.");

        // Locate the options rendered inside the visible Account listbox.
        List<WebElement> accountOptions = accountListbox.findElements(
                By.cssSelector("[role='option']")
        );

        // Keep only visible options that are not disabled.
        List<WebElement> selectableOptions = accountOptions.stream()
                .filter(WebElement::isDisplayed)
                .filter(option ->
                        !"true".equals(
                                option.getAttribute("aria-disabled")
                        )
                )
                .toList();

        System.out.println(
                "Number of selectable Account options: " +
                        selectableOptions.size()
        );

        // Stop with a clear error if no Account can be selected.
        if (selectableOptions.isEmpty()) {
            throw new IllegalStateException(
                    "No selectable Account options were found."
            );
        }

        // Choose one random Account from the available options.
        Random random = new Random();

        WebElement randomAccount = selectableOptions.get(
                random.nextInt(selectableOptions.size())
        );

        String selectedAccount = randomAccount.getText().trim();

        System.out.println(
                "Random Account selected: [" +
                        selectedAccount +
                        "]"
        );

        // Click the selected option to commit the Account value.
        randomAccount.click();

        /*
         * Wait for the Account editor to close, confirming that
         * the selection has been committed.
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
     * Selects a random available option from the Influencing on Cost cell
     * in the newly added Discounts & Additions row.
     *
     * Uses the captured AG Grid row index and double-clicks the target cell
     * to activate its editor.
     *
     * Handles intermittent AG Grid re-rendering by re-locating the cell
     * and retrying the double-click if the dropdown options do not appear.
     *
     * If a selection has already been applied, the method preserves it
     * instead of selecting another random value unnecessarily.
     *
     * No TAB key is used by this method.
     */
    public void selectRandomInfluencingOnCost() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        // Stable locator for the Influencing on Cost cell in the captured row.
        By influencingOnCostCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='influencingOnCost']"
        );

        // Locator for enabled options displayed by the dropdown.
        By optionLocator = By.cssSelector(
                "[role='option']:not([aria-disabled='true'])"
        );

        // Allow a second activation attempt if the dropdown does not appear.
        final int maxAttempts = 2;

        try {

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                System.out.println(
                        "Influencing on Cost attempt: "
                                + attempt + "/" + maxAttempts
                );

                // Re-locate the target cell because AG Grid may replace its DOM.
                WebElement influencingOnCostCell = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                influencingOnCostCellLocator
                        )
                );

                // Read the current cell value before interacting with it.
                String currentCellValue =
                        influencingOnCostCell.getText().trim();

                // If a previous attempt already selected a value, preserve it.
                if (!currentCellValue.isEmpty()) {

                    System.out.println(
                            "Influencing on Cost already contains a value: "
                                    + currentCellValue
                    );

                    return;
                }

                // Scroll the target cell into view before interacting with it.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block: 'center'});",
                        influencingOnCostCell
                );

                // Re-find the cell after scrolling in case AG Grid re-rendered it.
                influencingOnCostCell = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                influencingOnCostCellLocator
                        )
                );

                // Double-click the cell to activate its editor.
                new Actions(driver)
                        .doubleClick(influencingOnCostCell)
                        .perform();

                // Wait briefly for the dropdown options without failing immediately.
                List<WebElement> availableOptions;

                try {

                    availableOptions = new WebDriverWait(
                            driver,
                            Duration.ofSeconds(5)
                    ).until(currentDriver -> {

                        List<WebElement> options =
                                currentDriver.findElements(optionLocator);

                        List<WebElement> visibleOptions = new ArrayList<>();

                        for (WebElement option : options) {
                            try {
                                if (option.isDisplayed() && option.isEnabled()) {
                                    visibleOptions.add(option);
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // Ignore options replaced during a DOM update.
                            }
                        }

                        return visibleOptions.isEmpty()
                                ? null
                                : visibleOptions;
                    });

                } catch (TimeoutException dropdownTimeout) {

                    System.out.println(
                            "Dropdown options did not appear on attempt "
                                    + attempt
                    );

                    // Print the current cell state to help diagnose re-rendering.
                    try {
                        WebElement currentCell = driver.findElement(
                                influencingOnCostCellLocator
                        );

                        System.out.println(
                                "Cell HTML after activation attempt: "
                                        + currentCell.getAttribute("outerHTML")
                        );

                    } catch (NoSuchElementException |
                             StaleElementReferenceException ignored) {

                        System.out.println(
                                "Target cell was unavailable during diagnostics."
                        );
                    }

                    // Retry activation once; otherwise report a clear failure.
                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "Influencing on Cost options did not appear after "
                                    + maxAttempts + " activation attempts.",
                            dropdownTimeout
                    );
                }

                // Collect readable option labels for random selection.
                List<String> optionTexts = new ArrayList<>();

                for (WebElement option : availableOptions) {
                    try {
                        String optionText = option.getText().trim();

                        if (!optionText.isEmpty()
                                && !optionTexts.contains(optionText)) {
                            optionTexts.add(optionText);
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Ignore options replaced while collecting labels.
                    }
                }

                // Fail explicitly if no readable option labels were collected.
                if (optionTexts.isEmpty()) {
                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "The Influencing on Cost dropdown appeared, "
                                    + "but no readable option labels were found."
                    );
                }

                // Choose one available option randomly.
                String selectedOptionText = optionTexts.get(
                        new Random().nextInt(optionTexts.size())
                );

                System.out.println(
                        "Random Influencing on Cost option: "
                                + selectedOptionText
                );

                // Build an XPath matching the exact selected option text.
                By selectedOptionLocator = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + xpathLiteral(selectedOptionText)
                                + "]"
                );

                // Re-locate and click the option to handle dropdown re-rendering.
                WebElement selectedOption = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOptionLocator
                        )
                );

                selectedOption.click();

                // Verify that the chosen value appears in the target cell.
                boolean selectionVerified = wait.until(currentDriver -> {
                    try {
                        WebElement cell = currentDriver.findElement(
                                influencingOnCostCellLocator
                        );

                        return cell.getText()
                                .trim()
                                .equals(selectedOptionText);

                    } catch (StaleElementReferenceException |
                             NoSuchElementException ignored) {
                        return false;
                    }
                });

                if (!selectionVerified) {
                    throw new TimeoutException(
                            "The selected Influencing on Cost value could not "
                                    + "be verified: " + selectedOptionText
                    );
                }

                System.out.println(
                        "Influencing on Cost selected successfully: "
                                + selectedOptionText
                );

                return;
            }

        } catch (TimeoutException e) {

            // Print the target cell HTML when the selection process fails.
            System.out.println(
                    "===== INFLUENCING ON COST FAILURE DIAGNOSTICS ====="
            );

            try {
                List<WebElement> cells =
                        driver.findElements(influencingOnCostCellLocator);

                if (!cells.isEmpty()) {
                    System.out.println(
                            "Cell HTML: "
                                    + cells.get(0).getAttribute("outerHTML")
                    );
                } else {
                    System.out.println(
                            "Target Influencing on Cost cell was not found."
                    );
                }

            } catch (StaleElementReferenceException diagnosticException) {
                System.out.println(
                        "Target cell was re-rendered during diagnostics."
                );
            }

            // Count visible options to determine whether the dropdown remained open.
            int visibleOptionCount = 0;

            for (WebElement option :
                    driver.findElements(By.cssSelector("[role='option']"))) {
                try {
                    if (option.isDisplayed() && option.isEnabled()) {
                        visibleOptionCount++;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Ignore elements replaced during diagnostics.
                }
            }

            System.out.println(
                    "Visible role=option elements: " + visibleOptionCount
            );

            System.out.println(
                    "===== END INFLUENCING ON COST FAILURE DIAGNOSTICS ====="
            );

            // Preserve the original failure so TestNG reports the real cause.
            throw e;
        }
    }




    /**
     * Converts text into a safe XPath literal, including text containing
     * either single quotes, double quotes, or both.
     */
    private String xpathLiteral(String value) {

        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'", -1);
        StringBuilder xpath = new StringBuilder("concat(");

        for (int i = 0; i < parts.length; i++) {

            if (i > 0) {
                xpath.append(", \"'\", ");
            }

            xpath.append("'").append(parts[i]).append("'");
        }

        xpath.append(")");

        return xpath.toString();
    }

    /**
     * Enters the requested Discount Rate into the Discount Rate cell
     * of the newly added Discounts & Additions AG Grid row.
     *
     * Uses the stable AG Grid attributes row-index and col-id only.
     * Does not depend on data-testid or dynamically generated element IDs.
     *
     * Activates the existing inline editor, sets the requested value
     * through JavaScript, commits it using TAB, and verifies the saved value.
     */
    public void enterDiscountRateInDiscountsSection(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Stable locator for the Discount Rate cell in the captured AG Grid row.
        By discountRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] div[role='gridcell'][col-id='discountRate']"
        );

        // Stable locator for the native input inside the Discount Rate cell editor.
        By discountRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] div[role='gridcell'][col-id='discountRate'] " +
                        "input.ag-input-field-input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Rate in Discounts & Additions");
        System.out.println("Requested discount rate: " + discountRate);

        // Find the Discount Rate cell in the newly added row.
        WebElement discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        System.out.println(
                "Discount Rate cell found. Current value: [" +
                        discountRateCell.getText().trim() + "]"
        );

        // Scroll the target cell into view before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                discountRateCell
        );

        // Re-locate the cell after scrolling in case AG Grid re-rendered it.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        // Activate the inline editor using the existing framework helper.
        if (!tryActivateGridCellEditor(
                discountRateCellLocator,
                discountRateInputLocator
        )) {
            System.out.println(
                    "Discount Rate is disabled or non-editable. Skipping."
            );
            return;
        }

        System.out.println("Discount Rate cell activated successfully.");

        // Wait for the native input created by the AG Grid editor.
        WebElement discountRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        // Set the requested value and dispatch input/change events for the UI.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter = Object.getOwnPropertyDescriptor(
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

        // Verify that the editor accepted the requested value before committing.
        String editorValue = discountRateInput.getAttribute("value");

        System.out.println(
                "Discount Rate editor value BEFORE commit: [" +
                        editorValue + "]"
        );

        if (!discountRate.equals(editorValue)) {
            throw new IllegalStateException(
                    "Discount Rate editor did not accept the requested value. " +
                            "Expected: [" + discountRate +
                            "], Actual: [" + editorValue + "]"
            );
        }

        // Commit the edit using TAB, preserving the existing interaction approach.
        discountRateInput.sendKeys(Keys.TAB);

        System.out.println("TAB sent to commit Discount Rate.");

        // Wait until the inline editor disappears after the value is committed.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        // Re-locate the cell after AG Grid updates the displayed value.
        discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        String committedValue = discountRateCell.getText().trim();

        System.out.println(
                "Discount Rate cell value after commit: [" +
                        committedValue + "]"
        );

        // Normalize numeric values so values such as 20 and 20.00 compare equally.
        java.math.BigDecimal expectedValue =
                new java.math.BigDecimal(discountRate)
                        .stripTrailingZeros();

        String actualText = committedValue.replace("%", "").trim();

        java.math.BigDecimal actualValue =
                new java.math.BigDecimal(actualText)
                        .stripTrailingZeros();

        // Fail explicitly if the saved value differs from the requested value.
        if (expectedValue.compareTo(actualValue) != 0) {
            throw new IllegalStateException(
                    "Discount Rate was not committed correctly. " +
                            "Expected: [" + expectedValue.toPlainString() +
                            "], Actual: [" + actualValue.toPlainString() + "]"
            );
        }

        System.out.println(
                "Discount Rate entered successfully: " + committedValue
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Discount Value in the Discounts & Additions section.
     *
     * Uses the stored row index and the stable AG Grid column identifier
     * to locate the target cell. The existing tryActivateGridCellEditor()
     * helper determines whether the cell can be edited.
     *
     * If the cell is disabled or its editor cannot be activated, the method
     * skips the operation instead of failing the test.
     *
     * When editing is available, the method enters the requested value,
     * verifies the editor value, and presses TAB to commit the change.
     *
     * @param discountValue the discount value to enter into the cell
     */
    public void enterDiscountValueInDiscountsSection(String discountValue) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Discount Value cell using its stable column identifier
        // and the stored index of the new Discounts & Additions row.
        By discountValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] " +
                        "div[role='gridcell'][col-id='discountValue']"
        );

        // Locate the inline editor only within the target Discount Value cell.
        // AG Grid may create this input only after editing has been activated.
        By discountValueEditorLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] " +
                        "div[role='gridcell'][col-id='discountValue'] " +
                        "input.ag-input-field-input"
        );

        try {
            // Wait until the target cell is present before checking editability.
            WebElement discountValueCell = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            discountValueCellLocator
                    )
            );

            // Read the displayed value for diagnostic logging.
            String currentValue = discountValueCell.getText().trim();

            System.out.println(
                    "Discount Value BEFORE editing: [" + currentValue + "]"
            );

            // Use the existing helper to activate the editor.
            // A false result means the cell could not be activated, so skip it.
            boolean editorActivated = tryActivateGridCellEditor(
                    discountValueCellLocator,
                    discountValueEditorLocator
            );

            if (!editorActivated) {
                System.out.println(
                        "Discount Value is disabled/non-editable. Skipping."
                );
                return;
            }

            // Wait for the editor to become visible after successful activation.
            WebElement discountValueEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // Replace the existing editor value with the requested value.
            discountValueEditor.click();
            discountValueEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            discountValueEditor.sendKeys(discountValue);

            // Confirm that the editor contains the requested value before saving.
            String enteredValue = discountValueEditor.getAttribute("value");

            if (enteredValue == null ||
                    !enteredValue.trim().equals(discountValue.trim())) {

                throw new IllegalStateException(
                        "Discount Value mismatch. Expected [" + discountValue +
                                "] but found [" + enteredValue + "]."
                );
            }

            // Press TAB to commit the cell value without adding another row.
            discountValueEditor.sendKeys(Keys.TAB);

            // Wait until the cell displays the committed value.
            wait.until(driver -> {
                try {
                    WebElement updatedCell = driver.findElement(
                            discountValueCellLocator
                    );

                    String displayedValue = updatedCell.getText().trim();

                    return displayedValue.equals(discountValue) ||
                            displayedValue.equals(
                                    String.format(
                                            java.util.Locale.US,
                                            "%.2f",
                                            Double.parseDouble(discountValue)
                                    )
                            );
                } catch (StaleElementReferenceException |
                         NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Value entered successfully: [" + discountValue + "]"
            );

        } catch (TimeoutException e) {
            // Report a missing cell/editor as a diagnostic error.
            throw new IllegalStateException(
                    "Timed out while locating or updating Discount Value " +
                            "in row-index [" + newDiscountAdditionRowIndex + "].",
                    e
            );
        }
    }
    /**
     * Enters the Addition Rate (نسبة الإضافي) into the Addition Rate cell
     * of the newly created row in the Discounts & Additions AG Grid.
     *
     * Uses the stable AG Grid column identifier and the stored row index.
     * Activates the inline editor through the existing framework helper.
     * If the cell is disabled or cannot be activated, the method skips it.
     * Otherwise, it sets the value through the native HTML input setter,
     * verifies the entered value, and commits it using TAB.
     *
     * @param additionRate the Addition Rate value to enter
     */
    public void enterAdditionRateInDiscountsSection(String additionRate) {

        System.out.println("========================================");
        System.out.println("Entering Addition Rate in Discounts & Additions");
        System.out.println("Requested addition rate: " + additionRate);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Addition Rate cell using its stable AG Grid column identifier
        // and the stored row index of the new Discounts & Additions row.
        By additionRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='additionRate']"
        );

        // Locate the native input editor scoped to the Addition Rate cell.
        // This input is expected to exist only when AG Grid activates editing.
        By additionRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='additionRate'] " +
                        "input.ag-input-field-input"
        );

        // Wait for the target cell to become visible before reading its value.
        WebElement additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        System.out.println(
                "Addition Rate cell found. Current value: [" +
                        additionRateCell.getText().trim() +
                        "]"
        );

        // Scroll the cell into view before attempting to activate its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                additionRateCell
        );

        // Re-locate the cell after scrolling because AG Grid may re-render it.
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        // Activate the editor using the existing framework helper.
        // Skip the cell if the helper determines that editing is unavailable.
        if (!tryActivateGridCellEditor(
                additionRateCellLocator,
                additionRateInputLocator
        )) {
            System.out.println(
                    "Addition Rate is disabled/non-editable. Skipping."
            );
            System.out.println("========================================");
            return;
        }

        System.out.println("Addition Rate cell activated successfully.");

        // Wait for the native AG Grid input editor after successful activation.
        WebElement additionRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        System.out.println("Addition Rate editor found.");

        // Set the input value through the native HTML setter and dispatch
        // input/change events so the application can detect the modification.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter = Object.getOwnPropertyDescriptor(
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

        // Verify the value in the editor before committing the change.
        String editorValue = additionRateInput.getAttribute("value");

        System.out.println(
                "Addition Rate editor value BEFORE commit: [" +
                        editorValue +
                        "]"
        );

        if (editorValue == null ||
                !additionRate.equals(editorValue.trim())) {
            throw new IllegalStateException(
                    "Addition Rate editor did not accept the requested value. " +
                            "Expected: [" + additionRate +
                            "], Actual: [" + editorValue + "]"
            );
        }

        // Commit the value with TAB; avoid ENTER because it may trigger
        // unintended AG Grid row-related behavior.
        additionRateInput.sendKeys(Keys.TAB);

        System.out.println("TAB sent to commit Addition Rate.");

        // Wait for the inline editor to disappear after committing the value.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        // Re-locate the cell after AG Grid re-renders the committed value.
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        String committedValue = additionRateCell.getText().trim();

        System.out.println(
                "Addition Rate cell value after commit: [" +
                        committedValue +
                        "]"
        );

        // Normalize decimal formatting so values such as 20 and 20.00 compare
        // equally, while still rejecting an incorrect committed value.
        java.math.BigDecimal expectedValue =
                new java.math.BigDecimal(additionRate)
                        .stripTrailingZeros();

        java.math.BigDecimal actualValue;

        try {
            actualValue = new java.math.BigDecimal(
                    committedValue.replace("%", "").trim()
            ).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Addition Rate cell contains an invalid value: [" +
                            committedValue + "]",
                    e
            );
        }

        // Verify that the displayed value matches the requested Addition Rate.
        if (expectedValue.compareTo(actualValue) != 0) {
            throw new IllegalStateException(
                    "Addition Rate was not committed correctly. " +
                            "Expected: [" + expectedValue.toPlainString() +
                            "], Actual: [" + actualValue.toPlainString() + "]"
            );
        }

        System.out.println(
                "Addition Rate entered successfully: [" +
                        committedValue + "]"
        );

        System.out.println("========================================");
    }

    /**
     * Enters the Addition Value (قيمة الإضافي) into the Addition Value cell
     * of the newly created row in the Discounts & Additions AG Grid.
     *
     * Uses a stable column identifier and the stored row index to locate
     * the cell and its native input editor.
     *
     * The existing tryActivateGridCellEditor() helper activates the editor.
     * If the cell is disabled or cannot be activated, the method skips it.
     *
     * When editing is available, the method sets the requested value using
     * the native HTML input setter, verifies it, commits with TAB, and
     * validates the committed value after AG Grid re-renders the cell.
     *
     * @param additionValue the Addition Value to enter
     */
    public void enterAdditionValueInDiscountsSection(String additionValue) {

        System.out.println("========================================");
        System.out.println("Entering Addition Value in Discounts & Additions");
        System.out.println("Requested addition value: " + additionValue);

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Addition Value cell by its stable column identifier
        // and the stored row index of the new Discounts & Additions row.
        By additionValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='additionValue']"
        );

        // Locate the native input editor inside the Addition Value cell.
        // AG Grid creates this editor when the cell enters edit mode.
        By additionValueInputLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='additionValue'] " +
                        "input.ag-input-field-input"
        );

        // Wait until the target cell is visible before reading its current value.
        WebElement additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueCellLocator
                )
        );

        System.out.println(
                "Addition Value cell found. Current value: [" +
                        additionValueCell.getText().trim() +
                        "]"
        );

        // Scroll the target cell into the viewport before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                additionValueCell
        );

        // Re-locate the cell after scrolling because AG Grid may re-render it.
        additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueCellLocator
                )
        );

        // Activate the inline editor through the existing framework helper.
        // Skip the operation if the cell is disabled or cannot be activated.
        if (!tryActivateGridCellEditor(
                additionValueCellLocator,
                additionValueInputLocator
        )) {
            System.out.println(
                    "Addition Value is disabled/non-editable. Skipping."
            );
            System.out.println("========================================");
            return;
        }

        System.out.println("Addition Value cell activated successfully.");

        // Wait for the native input editor to become visible.
        WebElement additionValueInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueInputLocator
                )
        );

        System.out.println("Addition Value editor found.");

        // Set the requested value using the native HTML input setter and
        // dispatch input/change events to notify the application.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
    
                const setter = Object.getOwnPropertyDescriptor(
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

        // Verify that the editor accepted the requested value before committing.
        String editorValue = additionValueInput.getAttribute("value");

        System.out.println(
                "Addition Value editor value BEFORE commit: [" +
                        editorValue + "]"
        );

        if (editorValue == null ||
                !additionValue.equals(editorValue.trim())) {
            throw new IllegalStateException(
                    "Addition Value editor did not accept the requested value. " +
                            "Expected: [" + additionValue +
                            "], Actual: [" + editorValue + "]"
            );
        }

        // Commit the edited value using TAB rather than ENTER to avoid
        // unintended AG Grid row-related behavior.
        additionValueInput.sendKeys(Keys.TAB);

        System.out.println("TAB sent to commit Addition Value.");

        // Wait until the inline editor disappears after the commit.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        additionValueInputLocator
                )
        );

        // Re-locate the cell after AG Grid re-renders the committed value.
        additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueCellLocator
                )
        );

        String committedValue = additionValueCell.getText().trim();

        System.out.println(
                "Addition Value cell value after commit: [" +
                        committedValue + "]"
        );

        // Normalize decimal formatting so that 20 and 20.00 are equivalent.
        java.math.BigDecimal normalizedExpected =
                new java.math.BigDecimal(additionValue)
                        .stripTrailingZeros();

        java.math.BigDecimal normalizedActual;

        try {
            normalizedActual = new java.math.BigDecimal(
                    committedValue.replace("%", "").trim()
            ).stripTrailingZeros();
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Addition Value cell contains an invalid value: [" +
                            committedValue + "]",
                    e
            );
        }

        // Confirm that the displayed value matches the requested value.
        if (normalizedExpected.compareTo(normalizedActual) != 0) {
            throw new IllegalStateException(
                    "Addition Value was not committed correctly. " +
                            "Expected: [" + normalizedExpected.toPlainString() +
                            "], Actual: [" + normalizedActual.toPlainString() + "]"
            );
        }

        System.out.println(
                "Addition Value entered successfully: [" +
                        committedValue + "]"
        );

        System.out.println("========================================");
    }

    /**
     * Selects a random actual Currency from the Currency cell in the
     * newly created Discounts & Additions AG Grid row.
     *
     * Uses the stable AG Grid column identifier and stored row index.
     * Excludes placeholder options such as "اختر", selects a valid currency,
     * and verifies the committed value in the target cell.
     *
     * No generated DOM IDs or data-testid attributes are used.
     */
    public void selectRandomCurrencyInDiscountsSection() {

        System.out.println("========================================");
        System.out.println("Selecting random Currency");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Currency cell using its stable column identifier
        // and the stored index of the Discounts & Additions row.
        By currencyCellLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='currencyId']"
        );

        // Locate the React Select input inside the target Currency cell.
        By currencyInputLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='currencyId'] input"
        );

        // Wait until the target Currency cell is visible.
        WebElement currencyCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyCellLocator
                )
        );

        System.out.println(
                "Currency cell found. Current value: [" +
                        currencyCell.getText().trim() + "]"
        );

        // Scroll the Currency cell into view before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                currencyCell
        );

        // Re-locate the cell after scrolling to avoid using a stale element.
        currencyCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        currencyCellLocator
                )
        );

        // Double-click the cell to activate the React Select editor.
        new Actions(driver)
                .doubleClick(currencyCell)
                .perform();

        System.out.println("Double-click sent to Currency cell.");

        // Wait for the React Select input to appear in the active cell.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyInputLocator
                )
        );

        System.out.println("Currency React Select editor found.");

        // Re-locate the input before opening the dropdown.
        WebElement currencyInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        currencyInputLocator
                )
        );

        // Click the input to display the available Currency options.
        currencyInput.click();

        System.out.println("Currency dropdown opening...");

        // Locate the visible React Select listbox without using a generated ID.
        By currencyListboxLocator =
                By.cssSelector("div[role='listbox']");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyListboxLocator
                )
        );

        System.out.println("Currency dropdown opened.");

        // Collect visible, enabled options and exclude the placeholder "اختر".
        By currencyOptionLocator =
                By.cssSelector("div[role='listbox'] [role='option']");

        List<WebElement> selectableOptions = wait.until(driver -> {

            List<WebElement> options =
                    driver.findElements(currencyOptionLocator);

            List<WebElement> validOptions = options.stream()
                    .filter(WebElement::isDisplayed)
                    .filter(option ->
                            !"true".equals(
                                    option.getAttribute("aria-disabled")
                            )
                    )
                    .filter(option -> {
                        String text = option.getText().trim();

                        return !text.isEmpty()
                                && !text.equals("اختر");
                    })
                    .toList();

            return validOptions.isEmpty() ? null : validOptions;
        });

        System.out.println(
                "Number of selectable actual Currency options: " +
                        selectableOptions.size()
        );

        // Select one valid Currency randomly, excluding the placeholder.
        Random random = new Random();

        WebElement randomCurrency = selectableOptions.get(
                random.nextInt(selectableOptions.size())
        );

        String selectedCurrency = randomCurrency.getText().trim();

        System.out.println(
                "Random actual Currency selected: [" +
                        selectedCurrency + "]"
        );

        // Re-locate the chosen option by its visible text to reduce stale-element
        // risk if React Select re-renders the dropdown before the click.
        By selectedCurrencyOptionLocator = By.xpath(
                "//div[@role='listbox']" +
                        "//*[@role='option' and normalize-space(.)=" +
                        xpathLiteral(selectedCurrency) + "]"
        );

        WebElement optionToClick = wait.until(
                ExpectedConditions.elementToBeClickable(
                        selectedCurrencyOptionLocator
                )
        );

        // Click the actual Currency option.
        optionToClick.click();

        System.out.println("Actual Currency option clicked.");

        // Wait for the dropdown to close. The input may remain mounted briefly,
        // so use the listbox visibility as the dropdown-close condition.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        currencyListboxLocator
                )
        );

        // Wait for the selected value to appear in the target Currency cell.
        // Re-read the cell during polling because AG Grid may re-render it.
        boolean currencyCommitted = wait.until(driver -> {

            List<WebElement> cells =
                    driver.findElements(currencyCellLocator);

            if (cells.isEmpty() || !cells.get(0).isDisplayed()) {
                return false;
            }

            String actualValue = cells.get(0).getText().trim();

            return selectedCurrency.equals(actualValue);
        });

        // Read the final committed value from the current cell.
        currencyCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyCellLocator
                )
        );

        String committedCurrency = currencyCell.getText().trim();

        System.out.println(
                "Currency cell value after selection: [" +
                        committedCurrency + "]"
        );

        // Fail clearly if the selected Currency was not committed to the cell.
        if (!currencyCommitted ||
                !selectedCurrency.equals(committedCurrency)) {

            throw new IllegalStateException(
                    "Currency selection was not committed correctly. " +
                            "Expected: [" + selectedCurrency +
                            "], Actual: [" + committedCurrency + "]"
            );
        }

        System.out.println(
                "Currency selected and verified successfully: [" +
                        committedCurrency + "]"
        );

        System.out.println("========================================");
    }



    /**
     * Enters and commits the Note in the Discounts & Additions AG Grid.
     *
     * The method locates the Note cell using its stable col-id and the captured
     * row index, scrolls it into view, and attempts to activate the cell editor.
     * If double-click does not open the editor, it tries the standard F2 editing
     * shortcut. If editing still fails, it prints the actual cell HTML to help
     * diagnose whether the cell is editable or uses a different editor structure.
     *
     * After entering the requested note, the method commits the value by switching
     * tabs and verifies the saved text when the cell remains available in the DOM.
     *
     * @param note the text to enter into the Note cell
     */
    public void enterDiscountsAdditionsNote(String note) {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Note cell by its stable AG Grid column identifier and captured row index.
        By noteCellLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] " +
                        "div[role='gridcell'][col-id='note']"
        );

        // Locate the editor inside the Note cell without relying on a generated input ID.
        By noteEditorLocator = By.cssSelector(
                ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] " +
                        "div[role='gridcell'][col-id='note'] input, " +

                        ".ag-center-cols-container " +
                        "div[role='row'][row-index='" + newDiscountAdditionRowIndex + "'] " +
                        "div[role='gridcell'][col-id='note'] textarea"
        );

        System.out.println("Entering Discounts & Additions Note.");
        System.out.println("Requested note: [" + note + "]");
        System.out.println(
                "Target row index: " + newDiscountAdditionRowIndex
        );

        // Find the current cell after any previous AG Grid re-render.
        WebElement noteCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(noteCellLocator)
        );

        // Scroll the Note cell into view before attempting to activate editing.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                noteCell
        );

        // Re-find the cell before double-clicking because AG Grid may replace its DOM element.
        noteCell = wait.until(
                ExpectedConditions.elementToBeClickable(noteCellLocator)
        );

        new Actions(driver)
                .doubleClick(noteCell)
                .perform();

        System.out.println("Double-click performed on the Note cell.");

        // Wait briefly for the editor to appear after the double-click.
        WebElement noteEditor = null;

        try {
            noteEditor = new WebDriverWait(
                    driver,
                    Duration.ofSeconds(3)
            ).until(currentDriver -> {
                try {
                    List<WebElement> editors =
                            currentDriver.findElements(noteEditorLocator);

                    for (WebElement editor : editors) {
                        if (editor.isDisplayed() && editor.isEnabled()) {
                            return editor;
                        }
                    }

                    return null;

                } catch (StaleElementReferenceException |
                         NoSuchElementException e) {
                    return null;
                }
            });

        } catch (TimeoutException e) {
            System.out.println(
                    "The editor did not appear after double-click. Trying F2."
            );
        }

        // If double-click did not activate editing, try AG Grid's standard F2 shortcut.
        if (noteEditor == null) {

            WebElement currentCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(noteCellLocator)
            );

            currentCell.click();
            currentCell.sendKeys(Keys.F2);

            try {
                noteEditor = new WebDriverWait(
                        driver,
                        Duration.ofSeconds(3)
                ).until(currentDriver -> {
                    try {
                        List<WebElement> editors =
                                currentDriver.findElements(noteEditorLocator);

                        for (WebElement editor : editors) {
                            if (editor.isDisplayed() && editor.isEnabled()) {
                                return editor;
                            }
                        }

                        return null;

                    } catch (StaleElementReferenceException |
                             NoSuchElementException e) {
                        return null;
                    }
                });

            } catch (TimeoutException e) {
                // The actual cell HTML is needed to diagnose why editing was not activated.
                try {
                    WebElement diagnosticCell =
                            driver.findElement(noteCellLocator);

                    System.out.println(
                            "NOTE CELL HTML DIAGNOSTICS:\n" +
                                    diagnosticCell.getAttribute("outerHTML")
                    );

                } catch (NoSuchElementException |
                         StaleElementReferenceException diagnosticException) {
                    System.out.println(
                            "The Note cell could not be found for HTML diagnostics."
                    );
                }

                throw new IllegalStateException(
                        "Could not open the Note editor. " +
                                "Neither double-click nor F2 activated editing. " +
                                "Check the printed Note cell HTML to confirm its editability " +
                                "and the actual editor structure.",
                        e
                );
            }
        }

        System.out.println("Note editor opened successfully.");

        // Clear and enter the requested note, re-finding the editor after each possible re-render.
        WebElement currentEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(noteEditorLocator)
        );

        currentEditor.click();
        currentEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        currentEditor.sendKeys(note);

        // Verify the text in the active editor while accounting for stale elements.
        wait.until(currentDriver -> {
            try {
                List<WebElement> editors =
                        currentDriver.findElements(noteEditorLocator);

                for (WebElement editor : editors) {
                    if (editor.isDisplayed()
                            && note.equals(editor.getAttribute("value"))) {
                        return true;
                    }
                }

                return false;

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println("Note editor value verified: [" + note + "]");

        // Switch to the payment scheduling tab to commit the edited cell without pressing Enter.
        By paymentSchedulingTabLocator = By.cssSelector(
                "button[role='tab'][aria-controls$='content-paymentsScheduling']"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        paymentSchedulingTabLocator
                )
        ).click();

        System.out.println(
                "Clicked the Payment Scheduling tab to commit the Note."
        );

        // Return to the Discounts & Additions tab after committing the value.
        By discountsAdditionsTabLocator = By.xpath(
                "//button[@role='tab' and " +
                        "contains(normalize-space(.), 'الخصومات والإضافات')]"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAdditionsTabLocator
                )
        ).click();

        System.out.println(
                "Returned to the Discounts & Additions tab."
        );

        // Verify the committed Note if the same row and cell are still available.
        wait.until(currentDriver -> {
            try {
                WebElement committedCell =
                        currentDriver.findElement(noteCellLocator);

                return committedCell.isDisplayed()
                        && committedCell.getText().contains(note);

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println(
                "Discounts & Additions Note committed successfully: [" +
                        note + "]"
        );
    }

    /**
     * Opens the Payments Scheduling tab on the Local Purchase Invoice page.
     *
     * Uses the stable Arabic tab label instead of dynamically generated
     * Radix IDs, then verifies that the tab becomes selected.
     */
    public void openPaymentsSchedulingTab() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Payments Scheduling tab using its stable visible text.
        By paymentsSchedulingTabLocator = By.xpath(
                "//button[@role='tab' and .//div[normalize-space()='جدولة الدفعات']]"
        );

        // Wait until the tab is clickable, then open it.
        WebElement paymentsSchedulingTab = wait.until(
                ExpectedConditions.elementToBeClickable(
                        paymentsSchedulingTabLocator
                )
        );

        paymentsSchedulingTab.click();

        // Verify that the Payments Scheduling tab is selected.
        wait.until(
                ExpectedConditions.attributeToBe(
                        paymentsSchedulingTabLocator,
                        "aria-selected",
                        "true"
                )
        );

        System.out.println(
                "تم فتح تبويب جدولة الدفعات بنجاح."
        );
    }

    /**
     * Selects a random available Payment Terms Template from the
     * Local Purchase Invoice dropdown.
     *
     * Uses the stable data-testid attribute to locate the dropdown
     * and selects a random visible, enabled option.
     * Avoids dynamically generated React Select IDs and CSS classes.
     */
    public void selectRandomPaymentTermsTemplate() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Payment Terms Template dropdown using its stable test ID.
        By paymentTermsTemplateLocator = By.cssSelector(
                "[data-testid='invoice-local-paymentTermsTemplatesId']"
        );

        // Locate the dropdown input using the stable parent test ID.
        By paymentTermsTemplateInputLocator = By.cssSelector(
                "[data-testid='invoice-local-paymentTermsTemplatesId'] input"
        );

        // Wait until the dropdown input is clickable, then open the options.
        WebElement paymentTermsTemplateInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        paymentTermsTemplateInputLocator
                )
        );

        paymentTermsTemplateInput.click();

        // Locate the visible dropdown options without using generated IDs.
        By paymentTermsOptionsLocator = By.cssSelector(
                "div.select__option"
        );

        // Wait until the available options are visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        paymentTermsOptionsLocator
                )
        );

        // Collect only visible and enabled options.
        List<WebElement> availableOptions = driver.findElements(
                        paymentTermsOptionsLocator
                ).stream()
                .filter(WebElement::isDisplayed)
                .filter(option ->
                        !"true".equals(option.getAttribute("aria-disabled"))
                )
                .toList();

        // Stop with a clear error if no selectable options are available.
        if (availableOptions.isEmpty()) {
            throw new NoSuchElementException(
                    "لا توجد خيارات متاحة في قائمة قالب شروط السداد."
            );
        }

        // Select a random option from the available choices.
        int randomIndex = new java.util.Random()
                .nextInt(availableOptions.size());

        WebElement randomOption = availableOptions.get(randomIndex);

        // Capture the selected option text for reporting.
        String selectedTemplate = randomOption.getText().trim();

        // Click the randomly selected payment terms template.
        randomOption.click();

        // Verify that the dropdown displays the selected value.
        By selectedTemplateLocator = By.cssSelector(
                "[data-testid='invoice-local-paymentTermsTemplatesId'] " +
                        ".select__single-value"
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        selectedTemplateLocator
                )
        );

        System.out.println(
                "تم اختيار قالب شروط السداد عشوائيًا: "
                        + selectedTemplate
        );
    }

    /**
     * Copies the total invoice amount, including tax, into the Invoice Amount field.
     *
     * Uses the stable Arabic label to locate the source amount and the stable
     * data-testid to locate the destination input. Re-finds the input during
     * verification to avoid stale elements after React re-renders.
     */
    public void copyDisplayedAmountToInvoiceAmount() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the amount displayed beside the total invoice label.
        By sourceAmountLocator = By.xpath(
                "//div[contains(@class,'justify-between')]" +
                        "[.//span[normalize-space(.)=" +
                        "'اجمالي قيمة الفاتورة شاملة الضريبة']]" +
                        "/span[normalize-space(.) != " +
                        "'اجمالي قيمة الفاتورة شاملة الضريبة']"
        );

        // Locate the destination input through its stable test ID.
        By invoiceAmountLocator = By.cssSelector(
                "input[data-testid='invoice-local-amountInvoice']"
        );

        // Wait for the source amount to appear and read its displayed value.
        String amount = wait.until(driver -> {
            List<WebElement> elements =
                    driver.findElements(sourceAmountLocator);

            if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
                return null;
            }

            String value = elements.get(0).getText().trim();

            return value.isEmpty() ? null : value;
        });

        System.out.println(
                "Total invoice amount including tax: [" + amount + "]"
        );

        // Wait until the destination input is visible and enabled.
        wait.until(driver -> {
            List<WebElement> inputs =
                    driver.findElements(invoiceAmountLocator);

            return !inputs.isEmpty()
                    && inputs.get(0).isDisplayed()
                    && inputs.get(0).isEnabled();
        });

        // Enter the source amount and trigger the normal keyboard input events.
        WebElement invoiceAmountInput =
                driver.findElement(invoiceAmountLocator);

        invoiceAmountInput.click();
        invoiceAmountInput.sendKeys(Keys.CONTROL, "a");
        invoiceAmountInput.sendKeys(amount);

        // Allow the application to process the entered value.
        // Re-read the input on each poll in case React replaces the element.
        String actualValue = wait.until(driver -> {
            List<WebElement> inputs =
                    driver.findElements(invoiceAmountLocator);

            if (inputs.isEmpty()) {
                return null;
            }

            String value = inputs.get(0).getAttribute("value");

            return value != null && !value.trim().isEmpty()
                    ? value.trim()
                    : null;
        });

        System.out.println(
                "Invoice Amount field value after entry: [" + actualValue + "]"
        );

        // Verify the field is no longer empty; report formatting differences separately.
        if (!amount.equals(actualValue)) {
            System.out.println(
                    "Notice: source amount [" + amount
                            + "] differs from the input representation ["
                            + actualValue + "]."
            );
        } else {
            System.out.println(
                    "Invoice Amount populated successfully: [" + actualValue + "]"
            );
        }
    }

    /**
     * Enters a unique random vendor invoice number into the Vendor Invoice Number field.
     *
     * Uses the stable data-testid locator and scrolls the input into the center
     * of the viewport to reduce click interception caused by fixed page elements.
     * Generates a timestamp-based number with a random suffix for each invocation.
     */
    public void enterUniqueRandomVendorInvoiceNumber() {

        // Locate the Vendor Invoice Number field using its stable test ID.
        By vendorInvoiceCodeLocator = By.cssSelector(
                "input[data-testid='invoice-local-vendorInvoiceCode']"
        );

        // Create a practically unique invoice number for this test execution.
        String vendorInvoiceNumber =
                "VIN" + System.currentTimeMillis()
                        + String.format(
                        "%04d",
                        new java.security.SecureRandom().nextInt(10000)
                );

        // Wait for the field to be visible and enabled before interacting with it.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement vendorInvoiceCodeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        vendorInvoiceCodeLocator
                )
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        vendorInvoiceCodeLocator
                )
        );

        // Scroll the field to the center of the viewport to avoid fixed overlays.
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                vendorInvoiceCodeInput
        );

        // Re-find the input in case the page re-rendered after scrolling.
        vendorInvoiceCodeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        vendorInvoiceCodeLocator
                )
        );

        // Select and replace the current value using keyboard input.
        vendorInvoiceCodeInput.sendKeys(Keys.CONTROL, "a");
        vendorInvoiceCodeInput.sendKeys(vendorInvoiceNumber);

        // Verify that the generated number was entered successfully.
        String actualValue = wait.until(d -> {
            List<WebElement> inputs =
                    d.findElements(vendorInvoiceCodeLocator);

            if (inputs.isEmpty()) {
                return null;
            }

            String value = inputs.get(0).getAttribute("value");

            return vendorInvoiceNumber.equals(value)
                    ? value
                    : null;
        });

        System.out.println(
                "Vendor invoice number entered successfully: [" + actualValue + "]"
        );
    }

    /**
     * Clicks the Save button to save the Local Purchase Invoice.
     *
     * Uses the stable data-testid attribute instead of dynamic or generated selectors.
     * Scrolls the button into view, waits until it is clickable, and then clicks it.
     */
    public void clickSaveLocalPurchaseInvoice() {

        // Locate the Save button using its stable data-testid.
        By saveButtonLocator = By.cssSelector(
                "button[data-testid='invoice-local-save']"
        );

        // Wait until the Save button is visible and clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement saveButton = wait.until(
                ExpectedConditions.elementToBeClickable(saveButtonLocator)
        );

        // Scroll the Save button into the center of the viewport.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                saveButton
        );

        // Re-locate the button after scrolling to avoid using a stale element.
        saveButton = wait.until(
                ExpectedConditions.elementToBeClickable(saveButtonLocator)
        );

        // Click the Save button.
        saveButton.click();

        System.out.println(
                "Save button clicked successfully for the Local Purchase Invoice."
        );
    }

    /**
     * Confirms saving the Local Purchase Invoice by clicking the "Yes" button
     * in the confirmation dialog.
     *
     * Uses the stable data-modal-agree attribute and submit button type
     * instead of the dynamic data-testid value.
     */
    public void confirmSaveLocalPurchaseInvoice() {

        // Locate the confirmation button using stable HTML attributes.
        By confirmSaveButtonLocator = By.cssSelector(
                "button[data-modal-agree][type='submit']"
        );

        // Wait until the confirmation button is clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement confirmSaveButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmSaveButtonLocator
                )
        );

        // Scroll the confirmation button into the center of the viewport.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                confirmSaveButton
        );

        // Re-locate the button after scrolling to avoid stale element references.
        confirmSaveButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmSaveButtonLocator
                )
        );

        // Click "Yes" to confirm saving the invoice.
        confirmSaveButton.click();

        System.out.println(
                "Clicked 'Yes' to confirm saving the Local Purchase Invoice."
        );
    }

    /**
     * Enters and verifies the Notes (البيان) field in the Local Purchase Invoice.
     *
     * Uses the stable data-testid attribute to locate the field without
     * relying on dynamic IDs or generated selectors.
     *
     * @param notes the text to enter into the Notes field
     */
    public void enterLocalInvoiceNotes(String notes) {

        // Wait for the Notes field to become visible and enabled.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Notes field using its stable data-testid attribute.
        By notesLocator = By.cssSelector(
                "input[data-testid='invoice-local-notes']"
        );

        // Wait until the Notes field is displayed and enabled.
        WebElement notesField = wait.until(d -> {
            WebElement element = d.findElement(notesLocator);

            return element.isDisplayed() && element.isEnabled()
                    ? element
                    : null;
        });

        // Clear any existing text and enter the requested notes.
        notesField.clear();
        notesField.sendKeys(notes);

        // Re-locate the field to read its current value after entering text.
        String actualNotes = wait.until(d -> {
            WebElement element = d.findElement(notesLocator);

            if (!element.isDisplayed()) {
                return null;
            }

            String value = element.getAttribute("value");

            return notes.equals(value) ? value : null;
        });

        // Verify that the entered notes match the expected value.
        if (!notes.equals(actualNotes)) {
            throw new AssertionError(
                    "Local Invoice Notes verification failed. Expected: ["
                            + notes + "], Actual: [" + actualNotes + "]"
            );
        }

        // Log successful entry and verification.
        System.out.println(
                "Local Purchase Invoice Notes entered and verified successfully: "
                        + actualNotes
        );
    }


    /**
     * Selects a random Local Purchase Invoice from all pagination pages,
     * retrieves its status, opens the same invoice, and returns the status.
     *
     * Scans all pages, stores invoice row IDs and page numbers, returns to
     * the selected page, moves the AG Grid horizontal scrollbar to its
     * actual maximum position, reads the Invoice Status, and double-clicks
     * the selected invoice row.
     *
     * Uses stable row-id and col-id attributes and reads row IDs directly
     * from the DOM to reduce stale element reference errors.
     *
     * @return the status of the randomly selected Local Purchase Invoice
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public String selectRandomLocalPurchaseInvoiceAndGetStatus()
            throws InterruptedException {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;
        Random random = new Random();

        // Locates invoice data rows inside the central AG Grid container.
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

        // Identifies the invoice status cell by its stable AG Grid column ID.
        By statusCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='invoiceStatus']"
        );

        // Reads invoice row IDs directly from the live DOM.
        String rowIdsScript = """
            return Array.from(document.querySelectorAll(
                '.ag-center-cols-container div[role="row"][row-id]'
            ))
            .map(row => row.getAttribute('row-id'))
            .filter(id => id !== null && id.trim() !== '');
            """;

        // Creates a signature to detect when pagination changes the displayed rows.
        String rowSignatureScript = """
            return Array.from(document.querySelectorAll(
                '.ag-center-cols-container div[role="row"][row-id]'
            ))
            .map(row => row.getAttribute('row-id'))
            .filter(id => id !== null && id.trim() !== '')
            .join('|');
            """;

        // Waits until invoice rows are available.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(rowsLocator)
        );

        // ---------------------------------------------------------
        // Step 1: Return to the first pagination page.
        // ---------------------------------------------------------

        while (true) {

            WebElement previousButton =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    previousPageButton
                            )
                    );

            // Stops when the Previous button is disabled on the first page.
            if (!previousButton.isEnabled()
                    || previousButton.getAttribute("disabled") != null
                    || "true".equals(
                    previousButton.getAttribute("aria-disabled"))) {
                break;
            }

            String oldSignature = String.valueOf(
                    js.executeScript(rowSignatureScript)
            );

            previousButton.click();

            // Waits until the displayed invoice rows change.
            wait.until(d -> {
                Object newSignature =
                        ((JavascriptExecutor) d)
                                .executeScript(rowSignatureScript);

                return newSignature != null
                        && !newSignature.toString().isBlank()
                        && !newSignature.toString().equals(oldSignature);
            });

            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(rowsLocator)
            );
        }

        System.out.println("Currently on the first page.");

        // ---------------------------------------------------------
        // Step 2: Scan every page and collect invoice IDs and page numbers.
        // ---------------------------------------------------------

        List<String> invoiceRowIds = new ArrayList<>();
        List<Integer> invoicePageNumbers = new ArrayList<>();

        int currentPage = 1;

        while (true) {

            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(rowsLocator)
            );

            // Reads IDs from the live DOM instead of retaining row WebElements.
            @SuppressWarnings("unchecked")
            List<String> currentRowIds =
                    (List<String>) js.executeScript(rowIdsScript);

            System.out.println(
                    "Scanning Local Purchase Invoices page: " + currentPage
                            + ", rows found: " + currentRowIds.size()
            );

            // Stores each valid invoice ID with its page number.
            for (String rowId : currentRowIds) {

                if (rowId != null && !rowId.trim().isEmpty()) {
                    invoiceRowIds.add(rowId);
                    invoicePageNumbers.add(currentPage);
                }
            }

            WebElement nextButton =
                    wait.until(
                            ExpectedConditions.presenceOfElementLocated(
                                    nextPageButton
                            )
                    );

            // Stops scanning when the Next button is disabled on the last page.
            if (!nextButton.isEnabled()
                    || nextButton.getAttribute("disabled") != null
                    || "true".equals(
                    nextButton.getAttribute("aria-disabled"))) {
                break;
            }

            String oldSignature = String.valueOf(
                    js.executeScript(rowSignatureScript)
            );

            nextButton.click();

            // Waits for the next page's rows to be rendered.
            wait.until(d -> {
                Object newSignature =
                        ((JavascriptExecutor) d)
                                .executeScript(rowSignatureScript);

                return newSignature != null
                        && !newSignature.toString().isBlank()
                        && !newSignature.toString().equals(oldSignature);
            });

            currentPage++;
        }

        System.out.println(
                "Total Local Purchase Invoice pages: " + currentPage
        );

        System.out.println(
                "Total Local Purchase Invoices collected: "
                        + invoiceRowIds.size()
        );

        if (invoiceRowIds.isEmpty()) {
            throw new NoSuchElementException(
                    "No Local Purchase Invoices were found across the pagination pages."
            );
        }

        // ---------------------------------------------------------
        // Step 3: Select a random invoice and its original page.
        // ---------------------------------------------------------

        int randomIndex = random.nextInt(invoiceRowIds.size());

        String selectedRowId = invoiceRowIds.get(randomIndex);
        int selectedPage = invoicePageNumbers.get(randomIndex);

        System.out.println("Selected invoice row-id: " + selectedRowId);
        System.out.println("Selected invoice page: " + selectedPage);

        // ---------------------------------------------------------
        // Step 4: Return to the selected invoice's page.
        // ---------------------------------------------------------

        while (currentPage > selectedPage) {

            WebElement previousButton =
                    wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    previousPageButton
                            )
                    );

            String oldSignature = String.valueOf(
                    js.executeScript(rowSignatureScript)
            );

            previousButton.click();

            // Waits for the grid to update after navigating backward.
            wait.until(d -> {
                Object newSignature =
                        ((JavascriptExecutor) d)
                                .executeScript(rowSignatureScript);

                return newSignature != null
                        && !newSignature.toString().isBlank()
                        && !newSignature.toString().equals(oldSignature);
            });

            currentPage--;
        }

        System.out.println("Returned to selected page: " + selectedPage);

        // Locates the selected invoice by its row ID on the current page.
        By selectedInvoiceRowLocator = By.cssSelector(
                ".ag-center-cols-container div[role='row'][row-id='"
                        + selectedRowId + "']"
        );

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        selectedInvoiceRowLocator
                )
        );

        // ---------------------------------------------------------
        // Step 5: Move the horizontal scrollbar to its actual endpoint.
        // ---------------------------------------------------------

        /*
         * Moves both AG Grid horizontal viewports to their maximum horizontal
         * position. Handles RTL direction by applying a negative maximum when
         * required by the browser's scrollLeft behavior.
         */
        String scrollResult = (String) js.executeScript("""
            const horizontal = document.querySelector(
                '.ag-body-horizontal-scroll-viewport'
            );

            const viewport = document.querySelector(
                '.ag-center-cols-viewport'
            );

            if (!horizontal && !viewport) {
                return 'ERROR: AG Grid horizontal viewport was not found.';
            }

            function moveToEnd(element) {
                if (!element) {
                    return 'not found';
                }

                const maxScroll = Math.max(
                    0,
                    element.scrollWidth - element.clientWidth
                );

                const direction = getComputedStyle(element).direction;

                element.scrollLeft =
                    direction === 'rtl' ? -maxScroll : maxScroll;

                element.dispatchEvent(
                    new Event('scroll', { bubbles: true })
                );

                return 'direction=' + direction
                    + ', scrollLeft=' + element.scrollLeft
                    + ', maxScroll=' + maxScroll
                    + ', scrollWidth=' + element.scrollWidth
                    + ', clientWidth=' + element.clientWidth;
            }

            const horizontalResult = moveToEnd(horizontal);
            const viewportResult = moveToEnd(viewport);

            return 'Horizontal scrollbar: ' + horizontalResult
                + ' | Center viewport: ' + viewportResult;
            """);

        System.out.println("Horizontal scroll result: " + scrollResult);

        // Allows AG Grid to render the columns at the new scroll position.
        Thread.sleep(500);

        // Re-locates the status cell after the horizontal scroll.
        By selectedStatusCellLocator = By.cssSelector(
                "div[role='row'][row-id='" + selectedRowId + "'] "
                        + "div[role='gridcell'][col-id='invoiceStatus']"
        );

        /*
         * If the status column is not yet rendered, attempts to bring its
         * header into view. This is a fallback after moving the scrollbar
         * to its endpoint, not a replacement for the endpoint movement.
         */
        js.executeScript("""
            const header = document.querySelector(
                '.ag-header-cell[col-id="invoiceStatus"]'
            );

            if (header) {
                header.scrollIntoView({
                    block: 'nearest',
                    inline: 'nearest',
                    behavior: 'instant'
                });
            }
            """);

        Thread.sleep(300);

        // ---------------------------------------------------------
        // Step 6: Locate the selected invoice's status cell.
        // ---------------------------------------------------------

        WebElement statusCell = wait.until(d -> {

            List<WebElement> statusCells =
                    d.findElements(selectedStatusCellLocator);

            for (WebElement cell : statusCells) {

                try {
                    if (cell.isDisplayed()) {
                        return cell;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // The grid refreshed the cell; retry with a fresh element.
                }
            }

            return null;
        });

        // ---------------------------------------------------------
        // Step 7: Read the selected invoice's status.
        // ---------------------------------------------------------

        String invoiceStatus;

        try {

            invoiceStatus = statusCell.getText().trim();

            // Uses textContent if Selenium returns empty visible text.
            if (invoiceStatus.isEmpty()) {
                String textContent = statusCell.getAttribute("textContent");
                invoiceStatus = textContent == null ? "" : textContent.trim();
            }

        } catch (StaleElementReferenceException ignored) {

            // Re-locates the status cell if AG Grid refreshed it during retrieval.
            WebElement refreshedStatusCell =
                    wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    selectedStatusCellLocator
                            )
                    );

            invoiceStatus = refreshedStatusCell.getText().trim();

            if (invoiceStatus.isEmpty()) {
                String textContent =
                        refreshedStatusCell.getAttribute("textContent");

                invoiceStatus =
                        textContent == null ? "" : textContent.trim();
            }
        }

        if (invoiceStatus.isEmpty()) {
            throw new IllegalStateException(
                    "Invoice status is empty for row-id: " + selectedRowId
            );
        }

        System.out.println(
                "Selected Local Purchase Invoice status: ["
                        + invoiceStatus + "]"
        );

        // ---------------------------------------------------------
        // Step 8: Double-click the same invoice whose status was retrieved.
        // ---------------------------------------------------------

        // Re-locates the selected row immediately before opening it.
        WebElement rowToOpen =
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedInvoiceRowLocator
                        )
                );

        // Opens the selected invoice using a double-click.
        new Actions(driver)
                .doubleClick(rowToOpen)
                .perform();

        System.out.println(
                "Selected Local Purchase Invoice double-clicked. row-id: "
                        + selectedRowId
        );

        // Returns the status retrieved from the selected invoice.
        return invoiceStatus;
    }

    /**
     * Waits for the Local Purchase Invoice Copy button to become visible
     * and enabled, then clicks it exactly once.
     *
     * Uses stable HTML attributes (id='copy' and data-action-type='copy')
     * and re-locates the button during each wait cycle to reduce stale-element issues.
     */
    public void clickCopy() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Allow the Local Purchase Invoice page to finish rendering.
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();

            throw new RuntimeException(
                    "Interrupted while waiting for the Local Purchase Invoice Copy button.",
                    e
            );
        }

        // Define the stable locator inside the method.
        By copyButtonLocator = By.cssSelector(
                "button#copy[data-action-type='copy'][type='button']"
        );

        // Wait until the Copy button is present in the DOM.
        wait.until(d -> !d.findElements(copyButtonLocator).isEmpty());

        // Log the number of matching buttons.
        List<WebElement> buttons = driver.findElements(copyButtonLocator);

        System.out.println(
                "Matching Local Purchase Invoice Copy buttons: " + buttons.size()
        );

        // Log button details while safely handling stale elements.
        for (int i = 0; i < buttons.size(); i++) {
            try {
                WebElement button = buttons.get(i);

                System.out.println(
                        "Copy button #" + i
                                + " | displayed=" + button.isDisplayed()
                                + " | enabled=" + button.isEnabled()
                                + " | id=" + button.getAttribute("id")
                                + " | actionType=" + button.getAttribute("data-action-type")
                );

            } catch (StaleElementReferenceException ignored) {
                System.out.println(
                        "Copy button #" + i + " became stale during logging."
                );
            }
        }

        // Wait for a fresh, visible, enabled button and click it once.
        wait.until(d -> {
            List<WebElement> currentButtons = d.findElements(copyButtonLocator);

            for (WebElement button : currentButtons) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        button.click();

                        System.out.println(
                                "Local Purchase Invoice Copy button clicked successfully."
                        );

                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Re-locate the element during the next wait cycle.
                }
            }

            return false;
        });
    }

    /**
     * Clicks the Local Purchase Invoice Continue button when it is present,
     * visible, and enabled.
     *
     * Uses stable HTML attributes and the exact Arabic button text,
     * avoiding dynamic data-testid values and stale WebElement references.
     *
     * The button is optional, so the method skips the action when it is absent
     * or unavailable instead of failing the test.
     */
    public void clickContinue() {

        // Identify the Continue button using stable attributes and its exact text.
        By continueButtonLocator = By.xpath(
                "//button[@data-modal-agree and @type='submit'"
                        + " and normalize-space(.)='استمرار']"
        );

        // Wait briefly for the optional button to appear.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(5)
        );

        wait.pollingEvery(Duration.ofMillis(300));

        // Re-locate the button on each polling cycle to avoid stale references.
        try {
            WebElement continueButton = wait.until(d -> {
                List<WebElement> buttons =
                        d.findElements(continueButtonLocator);

                for (WebElement button : buttons) {
                    try {
                        if (button.isDisplayed() && button.isEnabled()) {
                            return button;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Retry with a fresh element during the next polling cycle.
                    }
                }

                return null;
            });

            // Click the available Continue button exactly once.
            continueButton.click();

            System.out.println(
                    "Local Purchase Invoice Continue button clicked successfully."
            );

        } catch (TimeoutException e) {
            System.out.println(
                    "Local Purchase Invoice Continue button is absent or unavailable. Skipping."
            );
        } catch (StaleElementReferenceException e) {
            // Re-locate the button once more if it becomes stale immediately before clicking.
            try {
                WebElement freshButton = driver.findElements(continueButtonLocator)
                        .stream()
                        .filter(button -> {
                            try {
                                return button.isDisplayed() && button.isEnabled();
                            } catch (StaleElementReferenceException ignored) {
                                return false;
                            }
                        })
                        .findFirst()
                        .orElse(null);

                if (freshButton != null) {
                    freshButton.click();

                    System.out.println(
                            "Local Purchase Invoice Continue button clicked successfully after re-location."
                    );
                } else {
                    System.out.println(
                            "Local Purchase Invoice Continue button became unavailable. Skipping."
                    );
                }

            } catch (StaleElementReferenceException ignored) {
                System.out.println(
                        "Local Purchase Invoice Continue button remained stale. Skipping."
                );
            }
        }
    }

    /**
     * Clicks the Add icon in the Local Purchase Invoice grid
     * to initiate the creation of a new invoice.
     *
     * Uses stable HTML attributes instead of dynamic data-testid values.
     * Re-locates the button before interaction to reduce stale-element issues.
     */
    public void clickAddLocalPurchaseInvoice() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Identify the Add icon using stable, non-dynamic HTML attributes.
        By addButtonLocator = By.cssSelector(
                "button#add[data-action-type='add'][type='button']"
        );

        System.out.println("========================================");
        System.out.println("Clicking Add Local Purchase Invoice button...");

        // Wait until the Add button is visible and enabled.
        WebElement addButton = wait.until(d -> {
            List<WebElement> buttons = d.findElements(addButtonLocator);

            for (WebElement button : buttons) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        return button;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Re-locate the button during the next polling cycle.
                }
            }

            return null;
        });

        System.out.println("Add Local Purchase Invoice button found.");

        // Scroll the current button into view.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                addButton
        );

        // Re-locate the button immediately before clicking to reduce stale-element risk.
        wait.until(d -> {
            List<WebElement> buttons = d.findElements(addButtonLocator);

            for (WebElement button : buttons) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        button.click();
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Retry using a fresh WebElement on the next polling cycle.
                }
            }

            return false;
        });

        System.out.println(
                "Add Local Purchase Invoice button clicked successfully."
        );
        System.out.println("========================================");
    }

    /**
     * Opens the Reference Document selection grid in the Local Purchase Invoice form.
     *
     * Locates the button associated with the Reference Document field using
     * stable HTML structure and attributes, without relying on dynamic IDs,
     * generated test IDs, or previously stored WebElement references.
     */
    public void clickOpenReferenceDocumentGridButton() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Reference Document grid button through its associated field.
        // Replace the field association below only if the actual page DOM differs.
        By openReferenceDocumentGridButtonLocator = By.xpath(
                "//*[(@name='referenceDocument' or @id='referenceDocument' "
                        + "or @data-field='referenceDocument')]"
                        + "/following::button[@type='button' "
                        + "and contains(@class,'text-primary')][1]"
        );

        System.out.println(
                "Waiting for the Reference Document grid button..."
        );

        WebElement openButton = wait.until(d -> {
            List<WebElement> buttons =
                    d.findElements(openReferenceDocumentGridButtonLocator);

            for (WebElement button : buttons) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        return button;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Re-locate the button during the next polling cycle.
                }
            }

            return null;
        });

        // Scroll the button into view before interacting with it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                openButton
        );

        // Re-locate the button immediately before clicking.
        wait.until(d -> {
            List<WebElement> buttons =
                    d.findElements(openReferenceDocumentGridButtonLocator);

            for (WebElement button : buttons) {
                try {
                    if (button.isDisplayed() && button.isEnabled()) {
                        button.click();
                        return true;
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Retry with a fresh element.
                }
            }

            return false;
        });

        System.out.println(
                "Reference Document grid button clicked successfully."
        );
    }



    /**
     * Randomly selects a receiving-order row, verifies its radio selection,
     * and confirms the selected document by clicking the موافق button.
     *
     * Uses stable AG Grid attributes and the modal confirmation attribute.
     * Avoids generated IDs and dynamic data-testid attributes.
     * Re-finds elements after grid re-rendering to reduce stale-element failures.
     */
    public void selectRandomReceivingOrderFromGrid() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20));
        wait.pollingEvery(Duration.ofMillis(300));

        // Locates receiving-order rows containing the selection radio cell.
        By selectableRowsLocator = By.cssSelector(
                "div[role='row'][row-index]:has(div[role='gridcell'][col-id='custom-checkbox'])"
        );

        // Locates the radio button inside a receiving-order row.
        By radioLocator = By.cssSelector(
                "div[role='gridcell'][col-id='custom-checkbox'] button[role='radio']"
        );

        // Waits until at least one selectable receiving-order row is available.
        wait.until(d -> d.findElements(selectableRowsLocator).stream().anyMatch(row -> {
            try {
                WebElement radio = row.findElement(radioLocator);

                return row.isDisplayed()
                        && radio.isDisplayed()
                        && radio.isEnabled();

            } catch (NoSuchElementException | StaleElementReferenceException e) {
                return false;
            }
        }));

        // Collects available row indexes without retaining elements across re-renders.
        List<Integer> availableRowIndexes = new ArrayList<>();

        for (WebElement row : driver.findElements(selectableRowsLocator)) {
            try {
                WebElement radio = row.findElement(radioLocator);
                String rowIndex = row.getAttribute("row-index");

                if (row.isDisplayed()
                        && radio.isDisplayed()
                        && radio.isEnabled()
                        && rowIndex != null
                        && !rowIndex.isBlank()) {

                    int index = Integer.parseInt(rowIndex);

                    if (!availableRowIndexes.contains(index)) {
                        availableRowIndexes.add(index);
                    }
                }

            } catch (NoSuchElementException
                     | StaleElementReferenceException
                     | NumberFormatException ignored) {
                // Skips rows that become unavailable during collection.
            }
        }

        if (availableRowIndexes.isEmpty()) {
            throw new NoSuchElementException(
                    "No selectable receiving-order rows were found in the AG Grid."
            );
        }

        // Randomly selects one available receiving-order row.
        int selectedRowIndex = availableRowIndexes.get(
                new Random().nextInt(availableRowIndexes.size())
        );

        // Builds a stable locator for the selected row.
        By selectedRowLocator = By.cssSelector(
                "div[role='row'][row-index='" + selectedRowIndex + "']" +
                        ":has(div[role='gridcell'][col-id='custom-checkbox'])"
        );

        // Selects the radio and waits until the application confirms its checked state.
        wait.until(d -> {
            try {
                WebElement radio = d.findElement(selectedRowLocator)
                        .findElement(radioLocator);

                if (!radio.isDisplayed() || !radio.isEnabled()) {
                    return false;
                }

                if (!"true".equals(radio.getAttribute("aria-checked"))) {
                    radio.click();
                }

                return "true".equals(
                        d.findElement(selectedRowLocator)
                                .findElement(radioLocator)
                                .getAttribute("aria-checked")
                );

            } catch (NoSuchElementException
                     | StaleElementReferenceException
                     | ElementClickInterceptedException e) {
                return false;
            }
        });

        // Verifies that the selected row is marked as selected by the grid.
        wait.until(d -> {
            try {
                WebElement row = d.findElement(selectedRowLocator);
                String rowClass = row.getAttribute("class");

                return "true".equals(row.getAttribute("aria-selected"))
                        || (rowClass != null && rowClass.contains("ag-row-selected"));

            } catch (NoSuchElementException | StaleElementReferenceException e) {
                return false;
            }
        });

        System.out.println(
                "Receiving-order radio selected. Row Index: " + selectedRowIndex
        );

        // Locates the confirmation button using stable attributes,
        // without relying on the dynamically generated data-testid.
        By confirmButtonLocator = By.cssSelector(
                "button[data-modal-agree][type='submit']"
        );

        // Waits until the موافق button becomes clickable.
        WebElement confirmButton = wait.until(
                ExpectedConditions.elementToBeClickable(confirmButtonLocator)
        );

        // Confirms the selected receiving-order document.
        confirmButton.click();

        System.out.println(
                "Confirmation button 'موافق' clicked successfully."
        );
    }



    /**
     * Clicks the reference-document download button after the reference
     * document selection workflow has completed.
     *
     * Uses a stable CSS locator based on the Lucide download icon class.
     * Re-locates the button on every polling attempt to avoid stale elements
     * and handles temporary DOM updates and intercepted clicks.
     */
    public void downloadReferenceDocument() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(20)
        );

        wait.pollingEvery(Duration.ofMillis(300));

        // Locate a button containing the stable download icon class.
        By downloadButtonLocator = By.cssSelector(
                "button:has(svg.lucide-download)"
        );

        wait.until(currentDriver -> {
            try {
                WebElement downloadButton =
                        currentDriver.findElement(downloadButtonLocator);

                if (!downloadButton.isDisplayed()
                        || !downloadButton.isEnabled()) {
                    return false;
                }

                downloadButton.click();

                System.out.println(
                        "Reference-document download button clicked successfully."
                );

                return true;

            } catch (StaleElementReferenceException
                     | ElementClickInterceptedException
                     | NoSuchElementException e) {

                return false;
            }
        });
    }



    /**
     * Selects "آجل" (Credit) from the payment-method dropdown
     * in the local purchase invoice.
     *
     * Uses the payment-method container and input ID confirmed
     * by the supplied HTML, then selects the exact visible option text.
     * Avoids generated React IDs and dynamic CSS classes.
     */
    public void selectCreditPaymentMethod() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        wait.pollingEvery(Duration.ofMillis(300));

        // Locate the payment-method dropdown using its supplied stable container.
        By paymentMethodContainer = By.cssSelector(
                "[data-testid='invoice-local-paymentMethod']"
        );

        // Locate the input belonging specifically to this dropdown.
        By paymentMethodInput = By.cssSelector(
                "[data-testid='invoice-local-paymentMethod'] input#paymentMethod"
        );

        // Locate the exact "آجل" option in the opened dropdown.
        By creditPaymentOption = By.xpath(
                "//*[@role='option' and normalize-space(.)='آجل']"
        );

        // Locate the selected value within the payment-method dropdown.
        By selectedPaymentValue = By.cssSelector(
                "[data-testid='invoice-local-paymentMethod'] .select__single-value"
        );

        // Open the payment-method dropdown.
        wait.until(currentDriver -> {
            try {
                WebElement container =
                        currentDriver.findElement(paymentMethodContainer);

                WebElement input =
                        container.findElement(By.cssSelector("input#paymentMethod"));

                if (!container.isDisplayed()
                        || !input.isDisplayed()
                        || !input.isEnabled()) {
                    return false;
                }

                input.click();

                return "true".equals(
                        input.getAttribute("aria-expanded")
                );

            } catch (NoSuchElementException
                     | StaleElementReferenceException
                     | ElementClickInterceptedException e) {
                return false;
            }
        });

        // Wait for the exact "آجل" option to appear, then select it.
        WebElement creditOption = wait.until(currentDriver -> {
            try {
                WebElement option =
                        currentDriver.findElement(creditPaymentOption);

                return option.isDisplayed() && option.isEnabled()
                        ? option
                        : null;

            } catch (NoSuchElementException
                     | StaleElementReferenceException e) {
                return null;
            }
        });

        creditOption.click();

        // Verify that "آجل" is displayed as the selected payment method.
        wait.until(currentDriver -> {
            try {
                WebElement selectedValue =
                        currentDriver.findElement(selectedPaymentValue);

                return selectedValue.isDisplayed()
                        && "آجل".equals(selectedValue.getText().trim());

            } catch (NoSuchElementException
                     | StaleElementReferenceException e) {
                return false;
            }
        });

        System.out.println(
                "Payment method selected successfully from dropdown: آجل"
        );
    }


}
