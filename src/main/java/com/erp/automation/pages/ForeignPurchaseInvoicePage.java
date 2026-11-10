package com.erp.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.openqa.selenium.*;
import org.openqa.selenium.NoSuchElementException;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;


public class ForeignPurchaseInvoicePage extends BasePage {

    public ForeignPurchaseInvoicePage(WebDriver driver) {
        super(driver);
    }

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
     * Clicks the New button on the Foreign Purchase Invoice screen
     * to open the new foreign purchase invoice form.
     *
     * Identifies the button by its stable data-testid prefix
     * and its visible Arabic label to avoid clicking another button.
     */
    public void clickNew() {

        // Locate only the button whose test ID starts with the foreign invoice prefix
        // and whose visible text is "جديد".
        By newButtonLocator = By.xpath(
                "//button[starts-with(@data-testid,'invoice-foriegn-')" +
                        " and .//span[normalize-space()='جديد']]"
        );

        // Wait until the exact New button becomes clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement newButton = wait.until(
                ExpectedConditions.elementToBeClickable(newButtonLocator)
        );

        // Click the New button to open the invoice creation form.
        newButton.click();
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
     * Selects a random Operation Type from the Foreign Purchase Invoice form.
     *
     * Uses stable locators embedded directly inside the method.
     * Avoids React-generated IDs and retries if the dropdown re-renders
     * or the options cannot be located within the timeout.
     */
    public void selectRandomOperationType() {

        // Stable locator for the Foreign Purchase Invoice Operation Type dropdown.
        By operationTypeDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-operationTypeCategoryId']"
        );

        // Locator for the visible React Select options, without generated IDs.
        By operationTypeOptions = By.cssSelector(
                "div.select__option"
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Foreign Purchase Invoice Operation Type - attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

                // Re-locate and open the Operation Type dropdown.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                operationTypeDropdown
                        )
                );

                dropdown.click();

                System.out.println("Operation Type dropdown opened.");

                // Read the text of the currently visible options.
                // Retry the wait if React re-renders the options.
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

                // Choose one of the visible Operation Type options randomly.
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

                // Locate the selected option by its exact visible text.
                By selectedOperationTypeOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedOperationType)
                                + "]"
                );

                // Re-find and click the selected option.
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
                        "Operation Type dropdown was re-rendered. Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Operation Type dropdown/options timed out. Retrying attempt "
                                + attempt
                                + " of "
                                + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Operation Type after "
                        + maxAttempts
                        + " attempts because the dropdown kept re-rendering "
                        + "or the options could not be located."
        );
    }

    /**
     * Selects a random Vendor from the Foreign Purchase Invoice form.
     *
     * Uses a stable data-testid to locate the Vendor dropdown and avoids
     * dynamically generated React IDs.
     * Reads visible options, selects one randomly by its exact text,
     * and retries when React re-renders the dropdown or a timeout occurs.
     */
    public void selectRandomVendor() {

        // Stable locator for the Vendor dropdown on the Foreign Purchase Invoice form.
        By vendorDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-vendorId']"
        );

        // Locator for React Select options displayed after opening the Vendor dropdown.
        By vendorOptions = By.cssSelector(
                "div.select__option"
        );

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
                        ExpectedConditions.elementToBeClickable(
                                vendorDropdown
                        )
                );

                dropdown.click();

                System.out.println("Vendor dropdown opened.");

                // Collect the text of currently visible vendor options.
                // Retry the wait if React re-renders the options.
                List<String> availableVendors = wait.until(currentDriver -> {

                    try {

                        List<WebElement> visibleOptions =
                                currentDriver.findElements(vendorOptions);

                        List<String> vendorNames = new ArrayList<>();

                        for (WebElement option : visibleOptions) {

                            if (option.isDisplayed()) {

                                String optionText =
                                        option.getText().trim();

                                if (!optionText.isEmpty()) {
                                    vendorNames.add(optionText);
                                }
                            }
                        }

                        return vendorNames.isEmpty()
                                ? null
                                : vendorNames;

                    } catch (StaleElementReferenceException e) {

                        return null;
                    }
                });

                // Select one vendor randomly from the visible options.
                String selectedVendor = availableVendors.get(
                        random.nextInt(availableVendors.size())
                );

                System.out.println(
                        "Available vendors: " + availableVendors.size()
                );

                System.out.println(
                        "Randomly selected vendor: " + selectedVendor
                );

                // Locate the exact option text using a safe XPath literal.
                By selectedVendorOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedVendor)
                                + "]"
                );

                // Re-locate the selected option and wait until it is clickable.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedVendorOption
                        )
                );

                optionToClick.click();

                System.out.println(
                        "Vendor selected successfully: " + selectedVendor
                );

                return;

            } catch (StaleElementReferenceException | TimeoutException e) {

                System.out.println(
                        "Attempt " + attempt +
                                " failed while selecting a vendor: " +
                                e.getMessage()
                );

                if (attempt == maxAttempts) {

                    throw new IllegalStateException(
                            "Failed to select a random vendor after "
                                    + maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Selects a random Currency from the Foreign Purchase Invoice form.
     *
     * Uses stable locators embedded inside the method and avoids
     * dynamically generated React IDs.
     * Collects visible currency options, selects one randomly by its exact text,
     * and retries if React re-renders the dropdown or a timeout occurs.
     */
    public void selectRandomCurrency() {

        // Stable locator for the Currency dropdown on the Foreign Purchase Invoice form.
        By currencyDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-currencyId']"
        );

        // Locator for the React Select currency options.
        By currencyOptions = By.cssSelector(
                "div.select__option"
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt
                                + " of " + maxAttempts
                                + ": Selecting a random currency..."
                );

                // Re-locate and open the Currency dropdown on every attempt.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                currencyDropdown
                        )
                );

                dropdown.click();

                System.out.println("Currency dropdown opened.");

                // Collect the text of currently visible currency options.
                // Retry the wait if React re-renders the options.
                List<String> availableCurrencies = wait.until(currentDriver -> {

                    try {

                        List<WebElement> visibleOptions =
                                currentDriver.findElements(currencyOptions);

                        List<String> currencyNames = new ArrayList<>();

                        for (WebElement option : visibleOptions) {

                            if (option.isDisplayed()) {

                                String optionText = option.getText().trim();

                                if (!optionText.isEmpty()) {
                                    currencyNames.add(optionText);
                                }
                            }
                        }

                        return currencyNames.isEmpty()
                                ? null
                                : currencyNames;

                    } catch (StaleElementReferenceException e) {

                        return null;
                    }
                });

                // Select a random currency from the available options.
                String selectedCurrency = availableCurrencies.get(
                        random.nextInt(availableCurrencies.size())
                );

                System.out.println(
                        "Available currencies: " + availableCurrencies.size()
                );

                System.out.println(
                        "Randomly selected currency: " + selectedCurrency
                );

                // Locate the selected currency using its exact visible text.
                By selectedCurrencyOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedCurrency)
                                + "]"
                );

                // Re-locate the selected option and wait until it is clickable.
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
                        "Attempt " + attempt
                                + " failed while selecting a currency: "
                                + e.getMessage()
                );

                if (attempt == maxAttempts) {

                    throw new IllegalStateException(
                            "Failed to select a random currency after "
                                    + maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Selects a random available branch from the foreign purchase invoice.
     * Uses stable locators, filters visible non-empty options, and retries
     * up to five times if a temporary Selenium interaction failure occurs.
     */
    public void selectRandomBranch() {

        // Stable locator for the foreign purchase invoice Branch dropdown.
        By branchDropdown =
                By.cssSelector("[data-testid='invoice-foriegn-branchId']");

        // Locator for React Select options without generated IDs or CSS classes.
        By branchOptions =
                By.cssSelector("div.select__option");

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

                // Locate the Branch dropdown and wait until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(branchDropdown)
                );

                dropdown.click();

                // Collect visible, non-empty options while handling stale elements.
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

                if (availableOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No visible branch options were found."
                    );
                }

                // Randomly choose one of the available branches.
                int randomIndex = random.nextInt(availableOptions.size());

                String selectedBranch =
                        availableOptions.get(randomIndex).getText().trim();

                System.out.println(
                        "Available branches: " + availableOptions.size()
                );

                System.out.println(
                        "Randomly selected branch: " + selectedBranch
                );

                // Build a safe XPath using the selected branch's visible text.
                By selectedBranchOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedBranch) + "]"
                );

                // Re-locate the selected option before clicking to avoid stale elements.
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
     * Randomly selects an available warehouse from the foreign purchase invoice.
     * Returns false if no warehouse options are available for the selected branch
     * or if a Selenium timeout or stale-element error occurs.
     */
    public boolean selectRandomWarehouse() {

        // Stable locator for the foreign purchase invoice Warehouse dropdown.
        By warehouseDropdown =
                By.cssSelector("[data-testid='invoice-foriegn-warehouseId']");

        // Locates React Select options without using generated IDs or CSS classes.
        By warehouseOptions =
                By.cssSelector("div.select__option");

        WebDriverWait dropdownWait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Short wait because an empty warehouse list should return false promptly.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        Random random = new Random();

        try {

            System.out.println("Opening the Warehouse dropdown...");

            // Locate the Warehouse dropdown and wait until it is clickable.
            WebElement dropdown = dropdownWait.until(
                    ExpectedConditions.elementToBeClickable(warehouseDropdown)
            );

            dropdown.click();

            // Collect visible, non-empty warehouse options.
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

            // Return false when no warehouses are available for the selected branch.
            if (availableOptions == null || availableOptions.isEmpty()) {

                System.out.println(
                        "No warehouse options are available for the selected branch."
                );

                return false;
            }

            // Randomly select one of the available warehouses.
            int randomIndex = random.nextInt(availableOptions.size());

            String selectedWarehouse =
                    availableOptions.get(randomIndex).getText().trim();

            System.out.println(
                    "Available warehouses: " + availableOptions.size()
            );

            System.out.println(
                    "Randomly selected warehouse: " + selectedWarehouse
            );

            // Build an XPath matching the selected warehouse's exact visible text.
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
     * Randomly selects an available Cost Center from the foreign purchase invoice.
     * Skips selection if the field is disabled and returns false when no options
     * are available or a Selenium timeout/stale-element error occurs.
     */
    public boolean selectRandomCostCenter() {

        // Stable locator for the foreign purchase invoice Cost Center dropdown.
        By costCenterDropdown =
                By.cssSelector("[data-testid='invoice-foriegn-costCenterId']");

        // Locates the React Select options without generated IDs or CSS classes.
        By costCenterOptions =
                By.cssSelector("div.select__option");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Short wait because an empty options list should not delay execution.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        Random random = new Random();

        try {

            // Locate the Cost Center dropdown container.
            WebElement dropdownContainer = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            costCenterDropdown
                    )
            );

            // Locate the actual React Select input to check whether it is enabled.
            WebElement dropdownInput = dropdownContainer.findElement(
                    By.cssSelector("input[role='combobox']")
            );

            // Skip selection if the Cost Center field is disabled.
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

            // Collect visible, non-empty Cost Center options.
            List<WebElement> availableOptions = optionsWait.until(currentDriver -> {

                try {

                    List<WebElement> visibleOptions = new ArrayList<>();

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

            // Return false when no Cost Center options are available.
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

            // Build an XPath matching the exact visible text of the selected option.
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
     * Randomly selects a Tax Calculation Method from the foreign purchase invoice.
     * Uses stable locators and retries up to five times when React Select
     * re-renders or Selenium encounters a timeout or stale element.
     */
    public void selectRandomTaxCalculationMethod() {

        // Stable locator for the foreign purchase invoice Tax Calculation Method dropdown.
        By taxCalculationMethodDropdown =
                By.cssSelector("[data-testid='invoice-foriegn-taxCalculationMethode']");

        // Locates visible React Select options without using generated IDs or CSS classes.
        By taxCalculationMethodOptions =
                By.cssSelector("div.select__option");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Tax Calculation Method - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-locate the dropdown on every attempt and wait until clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                taxCalculationMethodDropdown
                        )
                );

                dropdown.click();

                System.out.println(
                        "Tax Calculation Method dropdown opened."
                );

                // Read visible, non-empty option texts and retry if the list is not ready.
                List<String> taxCalculationMethods = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(
                                        taxCalculationMethodOptions
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

                if (taxCalculationMethods == null
                        || taxCalculationMethods.isEmpty()) {

                    throw new IllegalStateException(
                            "No Tax Calculation Methods found in the dropdown."
                    );
                }

                // Randomly choose one of the available Tax Calculation Methods.
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

                // Build a safe XPath using the selected option's exact visible text.
                By selectedTaxCalculationMethodOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedTaxCalculationMethod)
                                + "]"
                );

                // Re-locate the selected option immediately before clicking.
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
                                + "Retrying attempt " + attempt
                                + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Tax Calculation Method dropdown/options timed out "
                                + "during attempt " + attempt
                                + " of " + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Tax Calculation Method after "
                        + maxAttempts
                        + " attempts because the dropdown kept re-rendering "
                        + "or timing out."
        );
    }

    /**
     * Randomly selects the Price Nature option:
     * "باستثناء الضريبة" or "شاملة الضريبة".
     *
     * Uses stable data-testid attributes, retries up to five times,
     * and verifies that the selected radio button is checked.
     */
    public void selectRandomPriceNature() {

        // Stable locator for the Price Nature radio group on the foreign purchase invoice.
        By priceNatureRadioButtons = By.cssSelector(
                "[data-testid='invoice-foriegn-priceNature'] button[role='radio']"
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Price Nature - attempt "
                                + attempt + " of " + maxAttempts
                );

                /*
                 * Read the available radio buttons and retain their stable
                 * data-testid and value attributes instead of WebElements.
                 */
                List<String[]> priceOptions = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(priceNatureRadioButtons);

                        List<String[]> availableOptions = new ArrayList<>();

                        for (WebElement element : elements) {

                            if (element.isDisplayed() && element.isEnabled()) {

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

                // Ensure that at least one selectable radio button was found.
                if (priceOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No available Price Nature radio buttons were found."
                    );
                }

                // Randomly choose one of the available Price Nature options.
                String[] selectedOption = priceOptions.get(
                        random.nextInt(priceOptions.size())
                );

                String selectedTestId = selectedOption[0];
                String selectedValue = selectedOption[1];

                String selectedPriceNature =
                        "0".equals(selectedValue)
                                ? "باستثناء الضريبة"
                                : "شاملة الضريبة";

                System.out.println(
                        "Available Price Nature options: " + priceOptions.size()
                );

                System.out.println(
                        "Selected Price Nature: " + selectedPriceNature
                                + " (value=" + selectedValue + ")"
                );

                // Re-locate the selected radio button using its stable data-testid.
                By selectedRadioButton = By.cssSelector(
                        "[data-testid='" + selectedTestId + "'][role='radio']"
                );

                WebElement radioButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedRadioButton
                        )
                );

                // Click only if the randomly selected option is not already checked.
                if (!"true".equals(radioButton.getAttribute("aria-checked"))) {
                    radioButton.click();
                }

                // Verify that the selected radio button is checked.
                wait.until(currentDriver -> {

                    try {

                        WebElement selectedRadio =
                                currentDriver.findElement(selectedRadioButton);

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
                                + "Retrying attempt " + attempt
                                + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Price Nature radio buttons timed out during attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Unable to select Price Nature after " + maxAttempts
                        + " attempts because the radio buttons kept "
                        + "re-rendering or timing out."
        );
    }


    /**
     * Sets today's Invoice Date directly without opening the calendar.
     *
     * Uses the stable data-testid attribute to locate the input field,
     * sets today's date in yyyy-MM-dd format, dispatches input and change
     * events, and verifies that the application reflects the new value.
     */
    public void enterTodayForeignInvoiceDateDirectly() {

        // Stable locator for the foreign purchase invoice date input.
        By invoiceDateInput = By.cssSelector(
                "[data-testid='invoice-foriegn-invoiceDate'] input"
        );

        // Format today's date using the format expected by the input field.
        String todayInvoiceDate = LocalDate.now().format(
                DateTimeFormatter.ofPattern("yyyy-MM-dd")
        );

        // Wait until the invoice date input becomes visible.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        invoiceDateInput
                )
        );

        // Update the input value and notify the application of the change.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "arguments[0].value = arguments[1];" +
                        "arguments[0].dispatchEvent(new Event('input', { bubbles: true }));" +
                        "arguments[0].dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                todayInvoiceDate
        );

        // Verify that the input contains today's date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        invoiceDateInput,
                        "value",
                        todayInvoiceDate
                )
        );

        System.out.println(
                "Foreign Invoice Date set and verified: "
                        + todayInvoiceDate
        );
    }



    /**
     * Selects today's Invoice Date from the calendar on the foreign
     * purchase invoice form.
     *
     * Uses stable data-testid and aria-label attributes instead of
     * generated IDs or dynamic CSS classes.
     * Opens the calendar, locates today's date, selects it, and verifies
     * that the invoice date input contains today's date.
     */
    public void selectTodayForeignInvoiceDateFromCalendar() {

        // Stable locator for the foreign purchase invoice date input.
        By invoiceDateInput = By.cssSelector(
                "[data-testid='invoice-foriegn-invoiceDate'] input"
        );

        // Stable locator for the calendar button inside the date field.
        By calendarButtonLocator = By.cssSelector(
                "[data-testid='invoice-foriegn-invoiceDate'] " +
                        "button[aria-label='Open date picker']"
        );

        // Wait for the date field and calendar controls to become available.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Get today's date from the machine's local date.
        LocalDate today = LocalDate.now();

        System.out.println(
                "Selecting today's Foreign Invoice Date: " + today
        );

        // Ensure that the invoice date field is visible before opening its calendar.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        invoiceDateInput
                )
        );

        // Open the calendar using the stable button locator.
        WebElement calendarButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        calendarButtonLocator
                )
        );

        calendarButton.click();

        // Build today's expected aria-label in English.
        String dayOfWeek = today.format(
                DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
        );

        String month = today.format(
                DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
        );

        int day = today.getDayOfMonth();
        int year = today.getYear();

        String dateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                dayOfWeek,
                month,
                day,
                year
        );

        // Stable date locator based on the calendar's accessible date label.
        // The selector avoids generated IDs and generated CSS class names.
        By todayLocator = By.cssSelector(
                ".rmdp-day[aria-label='" + dateAriaLabel + "']"
        );

        // Wait until today's date is available and clickable in the calendar.
        WebElement todayOption = wait.until(
                ExpectedConditions.elementToBeClickable(
                        todayLocator
                )
        );

        // Scroll today's date into view before clicking it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                todayOption
        );

        // Re-locate the date after scrolling to avoid using a stale element.
        todayOption = wait.until(
                ExpectedConditions.elementToBeClickable(
                        todayLocator
                )
        );

        // Select today's date using a normal Selenium click.
        todayOption.click();

        // Verify that the invoice date input contains today's date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        invoiceDateInput,
                        "value",
                        today.toString()
                )
        );

        System.out.println(
                "Today's Foreign Invoice Date selected and verified: "
                        + today
        );
    }


    /**
     * Sets the foreign purchase invoice Due Date to one year from today
     * without opening the calendar.
     *
     * Uses JavaScript to assign the date in yyyy-MM-dd format, dispatches
     * bubbling input and change events, and verifies the resulting value.
     */
    public void enterDueDateOneYearFromNowDirectly() {

        // Stable locator for the foreign invoice Due Date input.
        By dueDateInput = By.cssSelector(
                "[data-testid='invoice-foriegn-deservedDate'] input"
        );

        // Calculate the due date one year from today.
        String dueDate = LocalDate.now()
                .plusYears(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Wait until the Due Date input is visible.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        dueDateInput
                )
        );

        // Set the date and notify the application of the input change.
        JavascriptExecutor js = (JavascriptExecutor) driver;

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
                "Foreign Invoice Due Date set and verified: " + dueDate
        );
    }


    /**
     * Selects the foreign purchase invoice Due Date one year from today
     * using the RMDP calendar.
     *
     * Opens the calendar, navigates forward month by month until the target
     * date is visible, selects the exact date using its aria-label, and
     * verifies that the Due Date input contains the expected value.
     */
    public void selectDueDateOneYearFromNowFromCalendar() {

        // Stable locator for the foreign invoice Due Date input.
        By dueDateInput = By.cssSelector(
                "[data-testid='invoice-foriegn-deservedDate'] input"
        );

        // Stable locator for the calendar button within the Due Date field.
        By dueDateCalendarButton = By.cssSelector(
                "[data-testid='invoice-foriegn-deservedDate'] " +
                        "button[aria-label='Open date picker']"
        );

        // Locator for the visible RMDP calendar wrapper.
        By calendarWrapperLocator = By.cssSelector(
                ".rmdp-wrapper"
        );

        // Locator for the Next Month arrow in the RMDP calendar.
        By nextMonthButtonLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-arrow-container.rmdp-right"
        );

        // Calculate the target date one year from today.
        LocalDate targetDueDate = LocalDate.now().plusYears(1);

        String expectedDueDate = targetDueDate.toString();

        // Build the exact accessible label expected for the target date.
        String targetDateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                targetDueDate.getDayOfWeek().getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                ),
                targetDueDate.getMonth().getDisplayName(
                        TextStyle.FULL,
                        Locale.ENGLISH
                ),
                targetDueDate.getDayOfMonth(),
                targetDueDate.getYear()
        );

        // Stable locator for the exact target date in the calendar.
        By targetDateLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-day[aria-label='" +
                        targetDateAriaLabel + "']"
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        System.out.println(
                "Selecting foreign invoice Due Date: " + expectedDueDate
        );

        // Ensure the Due Date field is visible before opening its calendar.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        dueDateInput
                )
        );

        // Open the calendar using the button inside the Due Date field.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        dueDateCalendarButton
                )
        ).click();

        // Wait for the calendar to become visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        // Navigate forward month by month until the target date appears.
        int maxMonthNavigationAttempts = 12;
        boolean targetDateVisible = false;

        for (int attempt = 0;
             attempt <= maxMonthNavigationAttempts;
             attempt++) {

            // Check whether the exact target date is currently displayed.
            List<WebElement> targetDates =
                    driver.findElements(targetDateLocator);

            targetDateVisible = targetDates.stream().anyMatch(element -> {
                try {
                    return element.isDisplayed();
                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });

            // Stop navigating as soon as the target date is found.
            if (targetDateVisible) {
                break;
            }

            // Do not navigate beyond the configured month limit.
            if (attempt == maxMonthNavigationAttempts) {
                break;
            }

            System.out.println(
                    "Navigating to the next calendar month: "
                            + (attempt + 1)
                            + " of "
                            + maxMonthNavigationAttempts
            );

            // Re-locate the Next Month arrow before each click.
            WebElement nextMonthButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            nextMonthButtonLocator
                    )
            );

            nextMonthButton.click();

            // Wait for the calendar's month heading to change after navigation.
            // This avoids relying on a fixed Thread.sleep delay.
            wait.until(currentDriver -> {
                try {
                    List<WebElement> headings =
                            currentDriver.findElements(
                                    By.cssSelector(
                                            ".rmdp-wrapper .rmdp-header-values"
                                    )
                            );

                    return headings.stream().anyMatch(heading -> {
                        try {
                            return heading.isDisplayed()
                                    && !heading.getText().isBlank();
                        } catch (StaleElementReferenceException ignored) {
                            return false;
                        }
                    });

                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });
        }

        // Fail clearly if the target date could not be found.
        if (!targetDateVisible) {
            throw new NoSuchElementException(
                    "Could not find Due Date " + expectedDueDate
                            + " after navigating "
                            + maxMonthNavigationAttempts
                            + " calendar months."
            );
        }

        // Wait until the target date is clickable and select it.
        WebElement targetDate = wait.until(
                ExpectedConditions.elementToBeClickable(
                        targetDateLocator
                )
        );

        targetDate.click();

        // Verify that the Due Date input contains the expected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        dueDateInput,
                        "value",
                        expectedDueDate
                )
        );

        System.out.println(
                "Foreign Invoice Due Date selected and verified: "
                        + expectedDueDate
        );
    }


    /**
     * Randomly selects a Purchase Representative from the foreign purchase
     * invoice form.
     *
     * Uses stable data-testid and role attributes instead of generated
     * React Select IDs or dynamic CSS classes.
     * Waits for the field's enabled/disabled state to stabilize for
     * two seconds and skips selection if the field is disabled.
     * Selects a random visible and enabled option when available.
     */
    public void selectRandomPurchaseRepresentative() {

        // Stable locator for the Purchase Representative dropdown container.
        By purchaseRepresentativeDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-purchaseRepresentativeId']"
        );

        // Stable locator for the searchable input inside the dropdown.
        By purchaseRepresentativeInput = By.cssSelector(
                "[data-testid='invoice-foriegn-purchaseRepresentativeId'] " +
                        "input[role='combobox']"
        );

        // Locator for selectable React Select options in the opened menu.
        // Uses the semantic option role rather than generated class names.
        By purchaseRepresentativeOptions = By.cssSelector(
                "[role='option']"
        );

        // Wait up to ten seconds and poll frequently while React updates the field.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        wait.pollingEvery(Duration.ofMillis(200));
        wait.ignoring(StaleElementReferenceException.class);

        // Wait until the Purchase Representative dropdown is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        purchaseRepresentativeDropdown
                )
        );

        // Track the last observed state and the time when it first appeared.
        final String[] lastState = {null};
        final long[] stateStartTime = {0L};

        // Wait until the enabled/disabled state remains unchanged for two seconds.
        String stableState = wait.until(currentDriver -> {

            try {
                WebElement input = currentDriver.findElement(
                        purchaseRepresentativeInput
                );

                WebElement dropdown = currentDriver.findElement(
                        purchaseRepresentativeDropdown
                );

                String dropdownClass = dropdown.getAttribute("class");
                String ariaDisabled = input.getAttribute("aria-disabled");

                // Check the React Select disabled class, ARIA state, and input state.
                boolean disabled =
                        (dropdownClass != null
                                && dropdownClass.contains("select--is-disabled"))
                                || "true".equalsIgnoreCase(ariaDisabled)
                                || !input.isEnabled();

                String currentState = disabled ? "DISABLED" : "ENABLED";
                long now = System.nanoTime();

                // Restart the stability timer whenever the observed state changes.
                if (!currentState.equals(lastState[0])) {
                    lastState[0] = currentState;
                    stateStartTime[0] = now;
                    return null;
                }

                long stableDurationMillis =
                        java.util.concurrent.TimeUnit.NANOSECONDS.toMillis(
                                now - stateStartTime[0]
                        );

                // Accept the state only after two seconds without a change.
                return stableDurationMillis >= 2000
                        ? currentState
                        : null;

            } catch (StaleElementReferenceException e) {
                // Reset state tracking if React replaces the dropdown elements.
                lastState[0] = null;
                stateStartTime[0] = 0L;
                return null;
            }
        });

        // Skip selection when the dropdown is disabled.
        if ("DISABLED".equals(stableState)) {
            System.out.println(
                    "Purchase Representative field is disabled. " +
                            "Skipping representative selection."
            );
            return;
        }

        // Recheck the field immediately before opening the dropdown.
        try {
            WebElement input = driver.findElement(
                    purchaseRepresentativeInput
            );

            WebElement dropdown = driver.findElement(
                    purchaseRepresentativeDropdown
            );

            String dropdownClass = dropdown.getAttribute("class");
            String ariaDisabled = input.getAttribute("aria-disabled");

            boolean currentlyDisabled =
                    (dropdownClass != null
                            && dropdownClass.contains("select--is-disabled"))
                            || "true".equalsIgnoreCase(ariaDisabled)
                            || !input.isEnabled();

            // Prevent interaction if the field became disabled after the wait.
            if (currentlyDisabled) {
                System.out.println(
                        "Purchase Representative field became disabled. " +
                                "Skipping representative selection."
                );
                return;
            }

        } catch (StaleElementReferenceException e) {
            // Avoid interacting with a field that changed during verification.
            System.out.println(
                    "Purchase Representative field changed during verification. " +
                            "Skipping representative selection."
            );
            return;
        }

        // Open the dropdown using the stable container locator.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        purchaseRepresentativeInput
                )
        ).click();

        // Wait until at least one visible, enabled option appears.
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

        // Retrieve the currently visible and enabled options.
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

        // Fail clearly if the enabled dropdown has no selectable options.
        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No purchase representatives found."
            );
        }

        // Select a random index from the available representatives.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        // Read the selected representative's text before clicking.
        String selectedRepresentative =
                options.get(randomIndex).getText().trim();

        // Click the randomly selected representative.
        options.get(randomIndex).click();

        // Log the selected representative for test traceability.
        System.out.println(
                "Number of purchase representatives: " + options.size()
        );

        System.out.println(
                "Selected purchase representative: " + selectedRepresentative
        );
    }


    /**
     * Randomly selects a Bulk Discount Type from the foreign purchase invoice.
     *
     * Uses the stable data-testid attribute to locate the dropdown and avoids
     * generated React Select IDs and dynamic CSS classes.
     * Retries up to five times if the DOM changes or an operation times out.
     * Logs the available options and the selected discount type.
     */
    public void selectRandomBulkDiscountType() {

        // Stable locator for the Bulk Discount Type dropdown container.
        By bulkDiscountTypeDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-bulkDiscountType']"
        );

        // Stable locator for selectable options in the opened React Select menu.
        By bulkDiscountTypeOptions = By.cssSelector(
                "[role='option']"
        );

        // Wait up to ten seconds for each dropdown interaction.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Generate a random index when choosing an available discount type.
        Random random = new Random();

        // Limit retries if React re-renders the dropdown or an operation times out.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Track the current operation to make failure logs more informative.
            String currentStage =
                    "locating the Bulk Discount Type dropdown";

            try {

                // Log the current retry attempt.
                System.out.println(
                        "Selecting random Bulk Discount Type - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-locate the dropdown and wait until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                bulkDiscountTypeDropdown
                        )
                );

                // Update the diagnostic stage before opening the dropdown.
                currentStage = "opening the Bulk Discount Type dropdown";

                // Open the React Select menu.
                dropdown.click();

                // Update the diagnostic stage before retrieving the options.
                currentStage = "waiting for visible Bulk Discount Type options";

                // Collect visible, enabled, non-empty option texts.
                List<String> discountTypes = wait.until(currentDriver -> {

                    List<WebElement> options =
                            currentDriver.findElements(
                                    bulkDiscountTypeOptions
                            );

                    List<String> visibleTexts = new ArrayList<>();

                    for (WebElement option : options) {

                        try {

                            // Ignore options that are hidden or disabled.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Read the option's visible text.
                            String optionText = option.getText().trim();

                            // Exclude empty options from random selection.
                            if (!optionText.isEmpty()) {
                                visibleTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Retry the wait condition if React replaces an option.
                            return null;
                        }
                    }

                    // Continue waiting until at least one valid option is available.
                    return visibleTexts.isEmpty()
                            ? null
                            : visibleTexts;
                });

                // Fail clearly if no selectable discount type was found.
                if (discountTypes.isEmpty()) {
                    throw new IllegalStateException(
                            "The Bulk Discount Type dropdown contains no visible options."
                    );
                }

                // Choose a random discount type from the available option texts.
                String selectedDiscountType =
                        discountTypes.get(
                                random.nextInt(discountTypes.size())
                        );

                // Log the number of available discount types.
                System.out.println(
                        "Number of bulk discount types: "
                                + discountTypes.size()
                );

                // Log the selected discount type for test traceability.
                System.out.println(
                        "Selected bulk discount type: "
                                + selectedDiscountType
                );

                // Update the diagnostic stage before locating the selected option.
                currentStage =
                        "locating the selected Bulk Discount Type option";

                // Build an XPath matching the exact option text.
                // toXPathLiteral safely handles text containing quotes.
                By selectedOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedDiscountType)
                                + "]"
                );

                // Re-locate the selected option and wait until it is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                );

                // Update the diagnostic stage before selecting the option.
                currentStage =
                        "clicking the selected Bulk Discount Type option";

                // Select the randomly chosen discount type.
                optionToSelect.click();

                // Log successful selection.
                System.out.println(
                        "Bulk Discount Type selected successfully: "
                                + selectedDiscountType
                );

                // End the method after a successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Log DOM changes that require another selection attempt.
                System.out.println(
                        "Bulk Discount Type DOM changed during stage: "
                                + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Log which operation timed out before retrying.
                System.out.println(
                        "Timeout while "
                                + currentStage
                                + ". Attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Report a clear failure if all retry attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Bulk Discount Type after "
                        + maxAttempts
                        + " attempts. Check the dropdown and option locators "
                        + "and verify that the menu is available."
        );
    }

    /**
     * Enters the bulk discount amount into the foreign purchase invoice.
     *
     * Uses the stable data-testid attribute to locate the input field,
     * waits until the field is visible and enabled, and updates its value
     * through the native JavaScript setter so React detects the change.
     * Finally, verifies that the requested amount was entered successfully.
     */
    public void enterBulkDiscountAmount() {

        // Defines the bulk discount amount to enter into the invoice.
        String bulkDiscountAmount = "10";

        // Stable locator based on the input's data-testid attribute.
        // Avoids generated IDs and dynamic CSS classes.
        By bulkDiscountAmountField = By.cssSelector(
                "input[data-testid='invoice-foriegn-bulkDiscountAmount']"
        );

        // Allows up to ten seconds for the input to become available.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Waits until the discount amount input is visible and enabled.
        WebElement amountField = wait.until(currentDriver -> {

            WebElement field = currentDriver.findElement(
                    bulkDiscountAmountField
            );

            // Returns the field only when it is ready to receive a value.
            return field.isDisplayed() && field.isEnabled()
                    ? field
                    : null;
        });

        // Provides JavaScript execution for updating the React-controlled input.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Uses the native input value setter and dispatches events for React.
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

                // Keeps waiting if React re-renders the input element.
                return false;
            }
        });

        // Logs the entered amount for test execution traceability.
        System.out.println(
                "Bulk discount amount entered successfully: "
                        + bulkDiscountAmount
        );
    }

    /**
     * Randomly selects an available Item Costs Method from the foreign
     * purchase invoice dropdown.
     *
     * Uses the stable data-testid attribute to locate the dropdown and
     * the role="option" attribute to identify selectable options.
     * Re-finds the selected option before clicking it and retries if
     * React re-renders the DOM during selection.
     */
    public void selectRandomItemsCostsMethod() {

        // Stable locator for the Item Costs Method dropdown container.
        By itemsCostsMethodDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-itemsCostsMethode']"
        );

        // Stable locator for the dropdown's combobox input.
        By itemsCostsMethodInput = By.cssSelector(
                "[data-testid='invoice-foriegn-itemsCostsMethode'] "
                        + "input[role='combobox']"
        );

        // Stable semantic locator for selectable dropdown options.
        By itemsCostsMethodOptions = By.cssSelector(
                "[role='option']"
        );

        // Allows up to ten seconds for each dropdown interaction.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Provides random selection among the available options.
        Random random = new Random();

        // Limits retries if React re-renders the dropdown.
        int maxAttempts = 5;

        // Retries the selection process when the DOM becomes stale.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Tracks the current operation for clearer diagnostic messages.
            String currentStage = "locating the Item Costs Method dropdown";

            try {

                // Logs the current selection attempt.
                System.out.println(
                        "Selecting random Item Costs Method - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Waits until the dropdown container is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                itemsCostsMethodDropdown
                        )
                );

                // Updates the diagnostic stage before opening the dropdown.
                currentStage = "opening the Item Costs Method dropdown";

                // Opens the React Select dropdown using its stable container.
                dropdown.click();

                // Waits until the combobox is available after opening the menu.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                itemsCostsMethodInput
                        )
                );

                // Updates the diagnostic stage before collecting options.
                currentStage = "collecting visible Item Costs Method options";

                // Collects visible, enabled options and retries the wait
                // condition if React replaces an option during collection.
                List<String> itemsCostsMethods = wait.until(currentDriver -> {

                    List<WebElement> optionElements =
                            currentDriver.findElements(
                                    itemsCostsMethodOptions
                            );

                    List<String> optionTexts = new ArrayList<>();

                    for (WebElement option : optionElements) {

                        try {

                            // Ignores options that are hidden or disabled.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Reads the visible text of the current option.
                            String optionText = option.getText().trim();

                            // Excludes empty options from random selection.
                            if (!optionText.isEmpty()) {
                                optionTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Restarts option collection if React re-renders.
                            return null;
                        }
                    }

                    // Continues waiting until at least one option is available.
                    return optionTexts.isEmpty() ? null : optionTexts;
                });

                // Selects one available method randomly.
                String selectedItemsCostsMethod =
                        itemsCostsMethods.get(
                                random.nextInt(itemsCostsMethods.size())
                        );

                // Logs the number of available options.
                System.out.println(
                        "Number of Item Costs Methods: "
                                + itemsCostsMethods.size()
                );

                // Logs the randomly selected method for test traceability.
                System.out.println(
                        "Selected Item Costs Method: "
                                + selectedItemsCostsMethod
                );

                // Updates the diagnostic stage before locating the option.
                currentStage = "locating the randomly selected option";

                // Locates the exact option text without generated IDs
                // or dynamic CSS classes.
                By selectedOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedItemsCostsMethod)
                                + "]"
                );

                // Waits for the selected option to become clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                );

                // Updates the diagnostic stage before clicking the option.
                currentStage = "clicking the selected Item Costs Method";

                // Selects the randomly chosen method.
                optionToSelect.click();

                // Logs successful completion.
                System.out.println(
                        "Item Costs Method selected successfully: "
                                + selectedItemsCostsMethod
                );

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Reports a DOM re-render and proceeds to the next attempt.
                System.out.println(
                        "DOM changed while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Reports the operation that timed out before retrying.
                System.out.println(
                        "Timeout while " + currentStage
                                + ". Attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports a clear failure if all attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Item Costs Method after "
                        + maxAttempts
                        + " attempts. Verify the dropdown and option locators."
        );
    }

    /**
     * Randomly selects an available Tag from the foreign purchase invoice.
     *
     * Uses the stable data-testid attribute to locate the dropdown and
     * the role="option" attribute to identify available options.
     * Re-finds the selected option before clicking it and retries when
     * React re-renders the DOM or an operation times out.
     */
    public void selectRandomTag() {

        // Stable locator for the Tag dropdown container.
        By tagDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-tagId']"
        );

        // Stable locator for the dropdown's combobox input.
        By tagDropdownInput = By.cssSelector(
                "[data-testid='invoice-foriegn-tagId'] input[role='combobox']"
        );

        // Stable semantic locator for selectable dropdown options.
        By tagOptions = By.cssSelector(
                "[role='option']"
        );

        // Allows up to ten seconds for dropdown interactions.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Provides random selection among the available Tag options.
        Random random = new Random();

        // Limits retries when React changes the dropdown DOM.
        int maxAttempts = 5;

        // Retries the complete selection process if an interaction fails.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Identifies the current operation for diagnostic logging.
            String currentStage = "locating the Tag dropdown";

            try {

                // Logs the current attempt for troubleshooting.
                System.out.println(
                        "Selecting random Tag - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-locates the dropdown and waits until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(tagDropdown)
                );

                // Updates the diagnostic stage before opening the dropdown.
                currentStage = "opening the Tag dropdown";

                // Opens the dropdown to display available Tag options.
                dropdown.click();

                // Waits until the dropdown's combobox input is visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                tagDropdownInput
                        )
                );

                // Updates the diagnostic stage before collecting options.
                currentStage = "collecting visible Tag options";

                // Collects visible, enabled, non-empty option texts.
                // Retries collection if React replaces an option during reading.
                List<String> tags = wait.until(currentDriver -> {

                    List<WebElement> optionElements =
                            currentDriver.findElements(tagOptions);

                    List<String> optionTexts = new ArrayList<>();

                    for (WebElement option : optionElements) {

                        try {

                            // Ignores options that are hidden or disabled.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Reads the visible label of the current option.
                            String optionText = option.getText().trim();

                            // Excludes empty and duplicate option labels.
                            if (!optionText.isEmpty()
                                    && !optionTexts.contains(optionText)) {
                                optionTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException e) {

                            // Restarts option collection if React re-renders.
                            return null;
                        }
                    }

                    // Continues waiting until at least one valid option exists.
                    return optionTexts.isEmpty() ? null : optionTexts;
                });

                // Randomly selects one Tag from the available option labels.
                String selectedTag = tags.get(
                        random.nextInt(tags.size())
                );

                // Logs the number of available options.
                System.out.println(
                        "Number of available tags: " + tags.size()
                );

                // Logs the randomly selected Tag for test traceability.
                System.out.println(
                        "Selected tag: " + selectedTag
                );

                // Updates the diagnostic stage before locating the option.
                currentStage = "locating the selected Tag option";

                // Locates the exact option text without generated IDs
                // or dynamic CSS classes.
                By selectedTagOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedTag)
                                + "]"
                );

                // Waits until the selected option is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedTagOption
                        )
                );

                // Updates the diagnostic stage before selecting the option.
                currentStage = "clicking the selected Tag option";

                // Selects the randomly chosen Tag.
                optionToSelect.click();

                // Logs successful selection.
                System.out.println(
                        "Tag selected successfully: " + selectedTag
                );

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs a DOM refresh before retrying the selection process.
                System.out.println(
                        "DOM changed while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Logs the operation that timed out before retrying.
                System.out.println(
                        "Timeout while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports a clear failure if all attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Tag after "
                        + maxAttempts
                        + " attempts. Verify the dropdown and option locators."
        );
    }

    /**
     * Randomly selects an available Letter of Credit from the foreign
     * purchase invoice dropdown.
     *
     * Uses the stable data-testid attribute to locate the dropdown and
     * role="option" to identify selectable options.
     * Re-finds the selected option before clicking it and retries if
     * React re-renders the DOM or an operation times out.
     */
    public void selectRandomLetterOfCredit() {

        // Stable locator for the Letter of Credit dropdown container.
        By letterOfCreditDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-letterOfCredtId']"
        );

        // Stable locator for the dropdown's combobox input.
        By letterOfCreditInput = By.cssSelector(
                "[data-testid='invoice-foriegn-letterOfCredtId'] "
                        + "input[role='combobox']"
        );

        // Semantic locator for selectable dropdown options.
        By letterOfCreditOptions = By.cssSelector(
                "[role='option']"
        );

        // Allows up to ten seconds for dropdown interactions.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Provides random selection among available options.
        Random random = new Random();

        // Limits retries if React re-renders the dropdown.
        int maxAttempts = 5;

        // Retries the selection process if an interaction fails.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Tracks the current operation for diagnostic logging.
            String currentStage = "locating the Letter of Credit dropdown";

            try {

                // Logs the current selection attempt.
                System.out.println(
                        "Selecting random Letter of Credit - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-locates the dropdown and waits until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                letterOfCreditDropdown
                        )
                );

                // Updates the diagnostic stage before opening the dropdown.
                currentStage = "opening the Letter of Credit dropdown";

                // Opens the dropdown to display available letters of credit.
                dropdown.click();

                // Waits until the combobox input is visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                letterOfCreditInput
                        )
                );

                // Updates the diagnostic stage before collecting options.
                currentStage = "collecting visible Letter of Credit options";

                // Collects visible, enabled, non-empty option labels.
                // Retries collection if React replaces an option during reading.
                List<String> letterOfCredits = wait.until(currentDriver -> {

                    List<WebElement> optionElements =
                            currentDriver.findElements(
                                    letterOfCreditOptions
                            );

                    List<String> optionTexts = new ArrayList<>();

                    for (WebElement option : optionElements) {

                        try {

                            // Ignores options that are hidden or disabled.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Reads the visible text of the current option.
                            String optionText = option.getText().trim();

                            // Excludes empty and duplicate option labels.
                            if (!optionText.isEmpty()
                                    && !optionTexts.contains(optionText)) {
                                optionTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException e) {

                            // Restarts collection if React re-renders the DOM.
                            return null;
                        }
                    }

                    // Continues waiting until at least one valid option exists.
                    return optionTexts.isEmpty() ? null : optionTexts;
                });

                // Randomly chooses one available letter of credit.
                String selectedLetterOfCredit = letterOfCredits.get(
                        random.nextInt(letterOfCredits.size())
                );

                // Logs the number of available options.
                System.out.println(
                        "Number of available Letters of Credit: "
                                + letterOfCredits.size()
                );

                // Logs the selected letter of credit for test traceability.
                System.out.println(
                        "Selected Letter of Credit: "
                                + selectedLetterOfCredit
                );

                // Updates the diagnostic stage before locating the option.
                currentStage = "locating the selected Letter of Credit option";

                // Locates the exact option text without generated IDs
                // or dynamic CSS classes.
                By selectedLetterOfCreditOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedLetterOfCredit)
                                + "]"
                );

                // Waits until the selected option is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedLetterOfCreditOption
                        )
                );

                // Updates the diagnostic stage before clicking the option.
                currentStage = "clicking the selected Letter of Credit option";

                // Selects the randomly chosen letter of credit.
                optionToSelect.click();

                // Logs successful selection.
                System.out.println(
                        "Letter of Credit selected successfully: "
                                + selectedLetterOfCredit
                );

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs a DOM refresh before retrying the selection process.
                System.out.println(
                        "DOM changed while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Logs the operation that timed out before retrying.
                System.out.println(
                        "Timeout while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports a clear failure if all selection attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select a Letter of Credit after "
                        + maxAttempts
                        + " attempts. Verify the dropdown and option locators."
        );
    }

    /**
     * Randomly selects an available classification from the foreign
     * purchase invoice classification dropdown.
     *
     * Uses the stable data-testid attribute to locate the dropdown
     * and role="option" to identify selectable options.
     * Retries the selection process if React re-renders the DOM
     * or an operation times out.
     */
    public void selectRandomClassification() {

        // Stable locator for the classification dropdown container.
        By classificationDropdown = By.cssSelector(
                "[data-testid='invoice-foriegn-classification']"
        );

        // Stable locator for the classification combobox input.
        By classificationInput = By.cssSelector(
                "[data-testid='invoice-foriegn-classification'] "
                        + "input[role='combobox']"
        );

        // Semantic locator for the selectable dropdown options.
        By classificationOptions = By.cssSelector(
                "[role='option']"
        );

        // Waits up to ten seconds for dropdown interactions.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Randomly chooses an option from the available classifications.
        Random random = new Random();

        // Maximum number of attempts to handle React re-rendering.
        int maxAttempts = 5;

        // Retries the selection process when an interaction fails.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Identifies the current operation for diagnostic logging.
            String currentStage = "locating the classification dropdown";

            try {

                // Logs the current selection attempt.
                System.out.println(
                        "Selecting random classification - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Waits until the classification dropdown is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                classificationDropdown
                        )
                );

                // Updates the diagnostic stage before opening the dropdown.
                currentStage = "opening the classification dropdown";

                // Opens the dropdown to display available classifications.
                dropdown.click();

                // Waits until the classification input is visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                classificationInput
                        )
                );

                // Updates the diagnostic stage before collecting options.
                currentStage = "collecting available classification options";

                // Collects visible, enabled, non-empty option labels.
                // Repeats collection if React replaces an option during reading.
                List<String> classifications = wait.until(currentDriver -> {

                    List<WebElement> optionElements =
                            currentDriver.findElements(classificationOptions);

                    List<String> optionTexts = new ArrayList<>();

                    for (WebElement option : optionElements) {

                        try {

                            // Ignores options that are hidden or disabled.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Reads the visible classification label.
                            String optionText = option.getText().trim();

                            // Adds each non-empty classification only once.
                            if (!optionText.isEmpty()
                                    && !optionTexts.contains(optionText)) {
                                optionTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException e) {

                            // Restarts collection if React refreshes the DOM.
                            return null;
                        }
                    }

                    // Waits until at least one valid option is available.
                    return optionTexts.isEmpty() ? null : optionTexts;
                });

                // Randomly selects a classification label from the collected list.
                String selectedClassification = classifications.get(
                        random.nextInt(classifications.size())
                );

                // Logs the number of available classification options.
                System.out.println(
                        "Available classifications: "
                                + classifications.size()
                );

                // Logs the selected classification for test traceability.
                System.out.println(
                        "Selected classification: " + selectedClassification
                );

                // Updates the diagnostic stage before locating the selected option.
                currentStage = "locating the selected classification option";

                // Uses the exact visible option text without dynamic IDs or classes.
                By selectedClassificationOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedClassification)
                                + "]"
                );

                // Waits until the selected classification option is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedClassificationOption
                        )
                );

                // Updates the diagnostic stage before selecting the option.
                currentStage = "clicking the selected classification option";

                // Selects the randomly chosen classification.
                optionToSelect.click();

                // Logs successful classification selection.
                System.out.println(
                        "Classification selected successfully: "
                                + selectedClassification
                );

                // Ends the method after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs a DOM refresh before retrying.
                System.out.println(
                        "DOM changed while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Logs the operation that timed out before retrying.
                System.out.println(
                        "Timeout while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports a clear failure if all attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select a random classification after "
                        + maxAttempts
                        + " attempts. Verify the dropdown and option locators."
        );
    }

    /**
     * Clicks the Add Line button in the Foreign Purchase Invoice items grid,
     * then detects the newly created Item cell and stores its identifiers.
     *
     * Uses the stable Arabic button label and the invoice-specific Item
     * data-testid prefix instead of generated IDs or dynamic CSS classes.
     *
     * Captures existing Item test IDs and row indexes before clicking,
     * ensuring that the newly added row is distinguished from existing rows
     * and from the separate Discounts & Additions grid.
     */
    public void clickAddLine() {

        // Waits up to ten seconds for the button and newly rendered row.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        /*
         * Locates Item cells belonging specifically to the Foreign Purchase
         * Invoice items grid, excluding unrelated AG Grid cells.
         */
        By itemCellsLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId']" +
                        "[data-testid^='invoice-foriegn-itemId-']"
        );

        /*
         * Locates the Add Line button by its stable visible Arabic label.
         * Does not depend on the changing data-testid suffix.
         */
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space(.)='اضافة سطر']"
        );

        // Stores Item cell test IDs present before adding a new line.
        Set<String> existingItemTestIds = new HashSet<>();

        // Stores logical row indexes present before adding a new line.
        Set<String> existingRowIndexes = new HashSet<>();

        try {

            /*
             * Resets horizontal scrolling in AG Grid viewports so the Item
             * column can be accessed after editing a cell farther to the right.
             */
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll(" +
                            "'.ag-body-horizontal-scroll-viewport'" +
                            ").forEach(function(viewport) {" +
                            "viewport.scrollLeft = 0;" +
                            "});"
            );

            /*
             * Captures the currently rendered invoice Item cells before
             * clicking Add Line, to identify the newly created row afterward.
             */
            List<WebElement> existingItemCells =
                    driver.findElements(itemCellsLocator);

            for (WebElement cell : existingItemCells) {

                try {

                    // Captures the stable prefix-based Item cell test ID.
                    String testId = cell.getAttribute("data-testid");

                    if (testId != null && !testId.isBlank()) {
                        existingItemTestIds.add(testId);
                    }

                    // Finds the AG Grid row containing this Item cell.
                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::div[@role='row'][1]")
                    );

                    // Captures the row's logical index before adding a line.
                    String rowIndex = row.getAttribute("row-index");

                    if (rowIndex != null && rowIndex.matches("\\d+")) {
                        existingRowIndexes.add(rowIndex);
                    }

                } catch (StaleElementReferenceException ignored) {

                    // Ignores cells replaced during an AG Grid re-render.
                }
            }

            // Logs the state of the invoice grid before adding a line.
            System.out.println(
                    "FOREIGN PURCHASE INVOICE - ADD LINE DIAGNOSTIC"
            );

            System.out.println(
                    "Item cells BEFORE Add Line: " +
                            existingItemCells.size()
            );

            System.out.println(
                    "Existing Item test IDs: " +
                            existingItemTestIds
            );

            System.out.println(
                    "Existing row indexes: " +
                            existingRowIndexes
            );

            /*
             * Waits for the Add Line button to become clickable and clicks
             * it exactly once.
             */
            WebElement addLineButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            addLineButtonLocator
                    )
            );

            // Scrolls the button into view before interacting with it.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({" +
                            "block: 'center', inline: 'nearest'" +
                            "});",
                    addLineButton
            );

            // Waits again after scrolling in case the button was re-rendered.
            addLineButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            addLineButtonLocator
                    )
            );

            // Clicks Add Line once; no retry click is performed on timeout.
            addLineButton.click();

            System.out.println(
                    "Foreign Purchase Invoice Add Line button clicked."
            );

            /*
             * Waits for a new Item cell whose test ID did not exist before
             * the click and whose logical row index is also new.
             */
            WebElement detectedNewItemCell = wait.until(webDriver -> {

                List<WebElement> currentItemCells =
                        webDriver.findElements(itemCellsLocator);

                for (WebElement cell : currentItemCells) {

                    try {

                        // Reads the current Item cell test ID.
                        String currentTestId =
                                cell.getAttribute("data-testid");

                        // Skips cells with missing or previously captured IDs.
                        if (currentTestId == null
                                || currentTestId.isBlank()
                                || existingItemTestIds.contains(currentTestId)) {
                            continue;
                        }

                        // Finds the row containing the candidate Item cell.
                        WebElement row = cell.findElement(
                                By.xpath("./ancestor::div[@role='row'][1]")
                        );

                        // Reads the candidate row's logical index.
                        String rowIndex = row.getAttribute("row-index");

                        // Accepts only a valid row index not present before the click.
                        if (rowIndex != null
                                && rowIndex.matches("\\d+")
                                && !existingRowIndexes.contains(rowIndex)) {

                            return cell;
                        }

                    } catch (StaleElementReferenceException ignored) {

                        // AG Grid may replace the candidate while rendering.
                    }
                }

                // Continues waiting until the new Item cell can be identified.
                return null;
            });

            /*
             * Stores the new Item cell test ID for compatibility with
             * existing methods that use newItemTestId.
             */
            newItemTestId =
                    detectedNewItemCell.getAttribute("data-testid");

            if (newItemTestId == null || newItemTestId.isBlank()) {
                throw new IllegalStateException(
                        "A new Foreign Purchase Invoice Item cell was detected, " +
                                "but its data-testid is empty."
                );
            }

            // Stores the AG Grid row containing the newly added Item cell.
            newRow = detectedNewItemCell.findElement(
                    By.xpath("./ancestor::div[@role='row'][1]")
            );

            // Reads and validates the logical index of the newly created row.
            String rowIndex = newRow.getAttribute("row-index");

            if (rowIndex == null || !rowIndex.matches("\\d+")) {
                throw new IllegalStateException(
                        "Could not determine a valid row-index for the new " +
                                "Foreign Purchase Invoice line. Item test ID: " +
                                newItemTestId + ", row-index: " + rowIndex
                );
            }

            // Stores the new invoice row index for subsequent cell operations.
            newRowIndex = Integer.parseInt(rowIndex);

            // Logs the result of adding the invoice line.
            System.out.println(
                    "Item cells AFTER Add Line: " +
                            driver.findElements(itemCellsLocator).size()
            );

            System.out.println(
                    "New Item test ID: " + newItemTestId
            );

            System.out.println(
                    "New Foreign Purchase Invoice row index: " +
                            newRowIndex
            );

        } catch (TimeoutException e) {

            /*
             * Logs diagnostic information without clicking Add Line again.
             * This helps distinguish a failed click from a failure to detect
             * the new Item cell after the grid updates.
             */
            System.err.println(
                    "Timeout while waiting for the new Foreign Purchase " +
                            "Invoice line."
            );

            System.err.println(
                    "Existing Item test IDs: " + existingItemTestIds
            );

            System.err.println(
                    "Existing row indexes: " + existingRowIndexes
            );

            List<WebElement> currentItemCells =
                    driver.findElements(itemCellsLocator);

            System.err.println(
                    "Currently detected Foreign Purchase Invoice Item cells: " +
                            currentItemCells.size()
            );

            for (WebElement cell : currentItemCells) {

                try {

                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::div[@role='row'][1]")
                    );

                    System.err.println(
                            "Item cell test ID: " +
                                    cell.getAttribute("data-testid")
                    );

                    System.err.println(
                            "Item cell row index: " +
                                    row.getAttribute("row-index")
                    );

                } catch (StaleElementReferenceException ignored) {

                    // Records that the cell changed while diagnostics ran.
                    System.err.println(
                            "An Item cell was re-rendered during diagnostics."
                    );
                }
            }

            throw new RuntimeException(
                    "Failed to detect the newly added Item cell in the " +
                            "Foreign Purchase Invoice items grid. " +
                            "The Add Line button is located by its visible label.",
                    e
            );
        }
    }



    /**
     * Selects a random Item Name from the React Select dropdown
     * inside the newly added Foreign Purchase Invoice row.
     *
     * Uses the captured newRow and the stable AG Grid column ID.
     * Attempts to activate the cell editor using a double-click.
     * Locates the React Select input without generated IDs.
     * Selects a random visible item from the dropdown.
     */
    public void selectRandomItem() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Ensure the newly added foreign invoice row has already been captured.
        if (newRow == null) {
            throw new IllegalStateException(
                    "New Foreign Purchase Invoice row was not captured. "
                            + "Call clickAddLine() first."
            );
        }

        System.out.println(
                "Using captured Foreign Purchase Invoice row."
        );

        System.out.println(
                "New row index: " + newRow.getAttribute("row-index")
        );

        // Locate the Item Name cell using the stable AG Grid column ID.
        By itemNameCellLocator =
                By.cssSelector(
                        "div[role='gridcell'][col-id='itemId']"
                );

        // Find the Item Name cell only inside the captured new row.
        WebElement itemCell =
                newRow.findElement(itemNameCellLocator);

        System.out.println(
                "Item Name cell found."
        );

        // Scroll the target cell into view before interacting with it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
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

        // Attempt to activate the AG Grid Item Name editor using a double-click.
        new Actions(driver)
                .moveToElement(itemCell)
                .doubleClick()
                .perform();

        System.out.println(
                "Double-clicked the Item Name cell."
        );

        // Locate the active React Select input using stable class names.
        By reactSelectInputLocator =
                By.cssSelector(
                        ".ag-popup-editor input.select__input"
                );

        // Wait until the React Select editor becomes visible.
        WebElement editor =
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                reactSelectInputLocator
                        )
                );

        System.out.println(
                "React Select Item Name editor opened successfully."
        );

        // Focus the editor to display the available items.
        editor.click();

        System.out.println(
                "Item Name dropdown opened."
        );

        // Retrieve the listbox ID associated with the active React Select input.
        String listboxId =
                editor.getAttribute("aria-controls");

        if (listboxId == null || listboxId.isBlank()) {
            throw new IllegalStateException(
                    "React Select listbox ID was not found "
                            + "for the Foreign Purchase Invoice Item Name field."
            );
        }

        System.out.println(
                "React Select listbox ID: " + listboxId
        );

        // Locate dropdown options inside the listbox associated with the editor.
        // The listbox ID is read at runtime because React Select generates it dynamically.
        By optionLocator =
                By.cssSelector(
                        "#" + listboxId
                                + " div[class*='select__option']"
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
                    "No visible items were found in the "
                            + "Foreign Purchase Invoice Item Name dropdown."
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

        // Select the chosen option using JavaScript, matching the local invoice approach.
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
                "Item selected successfully in the new Foreign Purchase Invoice row."
        );
    }

    /**
     * Enters and verifies the requested quantity in the newly added
     * Foreign Purchase Invoice AG Grid row.
     *
     * Uses the stable Quantity column ID and the captured newItemTestId
     * to identify the target row without relying on generated element IDs
     * or hardcoded row indexes.
     *
     * Re-locates the cell and editor after interactions to handle
     * AG Grid re-rendering and stale element references.
     */
    public void enterQuantity(String quantity) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        System.out.println("========================================");
        System.out.println("Entering Foreign Purchase Invoice quantity");
        System.out.println("Requested quantity: [" + quantity + "]");
        System.out.println("New Item test id: " + newItemTestId);

        // Validate the requested quantity before interacting with the grid.
        if (quantity == null || quantity.isBlank()) {
            throw new IllegalArgumentException(
                    "Quantity must not be null or blank."
            );
        }

        try {
            double requestedQuantity = Double.parseDouble(quantity.trim());

            if (!Double.isFinite(requestedQuantity) || requestedQuantity < 0) {
                throw new IllegalArgumentException(
                        "Quantity must be a finite, non-negative number."
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Quantity must be a valid numeric value: " + quantity,
                    e
            );
        }

        // Ensure the new item's stable test ID was captured when adding the line.
        if (newItemTestId == null || newItemTestId.isBlank()) {
            throw new IllegalStateException(
                    "newItemTestId is empty. "
                            + "Call clickAddLine() before enterQuantity()."
            );
        }

        /*
         * Locate the Quantity cell using the stable test ID pattern shown
         * in the supplied HTML and the stable AG Grid column ID.
         *
         * The test ID is captured at runtime; no row index is hardcoded.
         */
        By quantityCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='quantity']"
                        + "[data-testid='" + newItemTestId.replace(
                        "itemId", "quantity"
                ) + "']"
        );

        /*
         * Locate the inline Quantity editor only when it belongs to
         * the target Quantity cell.
         */
        By quantityEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='quantity']"
                        + ".ag-cell-inline-editing input[data-ref='eInput']"
        );

        // Wait for the new row's Quantity cell and scroll it into view.
        WebElement quantityCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityCellLocator
                )
        );

        js.executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                quantityCell
        );

        // Re-locate the cell after scrolling to avoid using a stale reference.
        quantityCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        quantityCellLocator
                )
        );

        // Activate inline editing by clicking the Quantity cell and pressing ENTER.
        quantityCell.click();

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        quantityCellLocator
                )
        ).sendKeys(Keys.ENTER);

        System.out.println("ENTER sent to the new Quantity cell.");

        // Wait for the inline editor to appear inside the Quantity column.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityEditorLocator
                )
        );

        /*
         * Clear the current editor value through the native input setter
         * and dispatch input/change events so the frontend detects the update.
         */
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
                        const setter = Object.getOwnPropertyDescriptor(
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

        // Enter the requested quantity and notify the frontend of the change.
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);

                js.executeScript(
                        """
                        const input = arguments[0];
                        const value = arguments[1];
                        const setter = Object.getOwnPropertyDescriptor(
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
                        quantity.trim()
                );

                return true;

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        // Verify the requested value in the active editor before committing.
        wait.until(d -> {
            try {
                WebElement input = d.findElement(quantityEditorLocator);

                String currentValue = input.getDomProperty("value");

                System.out.println(
                        "Quantity editor current value: [" + currentValue + "]"
                );

                return quantity.trim().equals(currentValue);

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println("Requested quantity verified in the editor.");

        // Commit the quantity by pressing ENTER in the active editor.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        quantityEditorLocator
                )
        ).sendKeys(Keys.ENTER);

        /*
         * Verify the committed cell value.
         * Numeric comparison accepts display formats such as 17, 17.0 and 17.000.
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
                "SUCCESS: Foreign Purchase Invoice quantity entered: "
                        + quantity
        );
        System.out.println("========================================");
    }

    /**
     * Enters the requested cost into the Cost cell of the newly added
     * Foreign Purchase Invoice AG Grid row.
     *
     * Identifies the target row using newItemTestId and re-locates elements
     * whenever AG Grid may re-render the DOM.
     *
     * Uses stable column IDs and test IDs instead of generated element IDs.
     * Activates the Cost cell, enters the requested value, commits using TAB,
     * and verifies the final displayed value.
     *
     * @param price the cost value to enter into the new item's Cost cell
     */
    public void enterPrice(String price) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        JavascriptExecutor js =
                (JavascriptExecutor) driver;

        System.out.println("========================================");
        System.out.println("Entering Foreign Purchase Invoice cost");
        System.out.println("Requested cost: [" + price + "]");
        System.out.println("New Item test id: " + newItemTestId);

        // Validate the requested cost before interacting with the grid.
        if (price == null || price.isBlank()) {
            throw new IllegalArgumentException(
                    "Cost value must not be null or blank."
            );
        }

        try {
            java.math.BigDecimal requestedCost =
                    new java.math.BigDecimal(price.trim());

            if (requestedCost.signum() < 0) {
                throw new IllegalArgumentException(
                        "Cost value must not be negative."
                );
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Cost must be a valid numeric value: " + price,
                    e
            );
        }

        // Ensure the new item's test ID was captured after adding the row.
        if (newItemTestId == null || newItemTestId.isBlank()) {
            throw new IllegalStateException(
                    "Cannot enter cost because newItemTestId has not been captured."
            );
        }

        /*
         * Locate the new item's Item Name cell using its captured test ID.
         * This is the stable anchor used to identify the correct AG Grid row.
         */
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        /*
         * Locate the Cost cell within the same AG Grid row as the new item.
         * The stable col-id identifies the Cost column without relying on
         * the generated cell ID or the current row index.
         */
        By costCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost']"
        );

        /*
         * Locate the active Cost editor only inside the target row's Cost cell.
         * The editor locator uses stable AG Grid attributes rather than
         * generated input IDs.
         */
        By costInputLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost']"
                        + "//input[@data-ref='eInput']"
        );

        try {
            // Wait until the new item's stable row anchor is present.
            wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Wait until the new item's Cost cell is visible.
            WebElement costCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costCellLocator
                    )
            );

            // Scroll the Cost cell into view before activating its editor.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    costCell
            );

            // Re-locate the Cost cell after scrolling to avoid stale references.
            costCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            costCellLocator
                    )
            );

            // Activate the Cost cell using a regular click.
            new Actions(driver)
                    .moveToElement(costCell)
                    .click()
                    .perform();

            System.out.println("Clicked the new item's Cost cell.");

            // Wait until the inline Cost editor becomes visible.
            WebElement costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            /*
             * Clear the current input value using the native input setter
             * and dispatch events so the frontend detects the change.
             */
            js.executeScript(
                    """
                    const input = arguments[0];
                    const setter = Object.getOwnPropertyDescriptor(
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
                    costInput
            );

            // Re-locate the editor in case clearing triggered a grid re-render.
            costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            // Enter the requested cost and notify the application of the change.
            js.executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
                    const setter = Object.getOwnPropertyDescriptor(
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
                    costInput,
                    price.trim()
            );

            // Verify the complete requested value in the active editor.
            wait.until(currentDriver -> {
                try {
                    WebElement currentInput =
                            currentDriver.findElement(costInputLocator);

                    return price.trim().equals(
                            currentInput.getDomProperty("value")
                    );

                } catch (StaleElementReferenceException |
                         NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Cost value entered and verified in editor: " + price
            );

            // Re-locate the editor before committing the value with TAB.
            costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costInputLocator
                    )
            );

            // Commit the edited Cost cell without intentionally adding a new row.
            costInput.sendKeys(Keys.TAB);

            /*
             * Wait until the Cost cell exits inline-editing mode.
             * Re-locate the cell on each poll to handle grid re-rendering.
             */
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

                } catch (StaleElementReferenceException |
                         NoSuchElementException e) {
                    return false;
                }
            });

            // Read the committed Cost cell value after editing finishes.
            WebElement updatedCostCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            costCellLocator
                    )
            );

            String actualCost = updatedCostCell.getText().trim();

            System.out.println(
                    "Cost cell value after commit: [" + actualCost + "]"
            );

            /*
             * Normalize common display formatting such as commas and brackets.
             * This supports values displayed as [100.00] or 100.00.
             */
            String normalizedExpected =
                    price.replace(",", "").trim();

            String normalizedActual = actualCost
                    .replace(",", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            // Verify the committed value numerically.
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
                    "Cost committed and verified successfully: " + actualCost
            );
            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter cost value '" + price
                            + "' into the new Foreign Purchase Invoice item's Cost cell.",
                    e
            );
        }
    }

    /**
     * Enters the Discount Rate (%) into the newly added row
     * of the Foreign Purchase Invoice AG Grid.
     *
     * Uses newItemTestId to identify the new item's row and the stable
     * AG Grid column identifier (col-id='discountRate') to locate the
     * Discount Rate cell within that same row.
     *
     * F2 activates inline editing, JavaScript assigns the complete value,
     * and TAB commits the value.
     *
     * @param discountRate the discount percentage to enter
     */
    public void enterDiscountRate(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // Validate that the discount value is provided.
            if (discountRate == null || discountRate.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Discount Rate must not be null or blank."
                );
            }

            // Ensure the new item's test ID is available to identify its row.
            if (newItemTestId == null || newItemTestId.trim().isEmpty()) {
                throw new IllegalStateException(
                        "newItemTestId is not initialized. "
                                + "Add and identify the invoice item row first."
                );
            }

            // Stable locator for the new item's cell, used as the row anchor.
            By newItemCellLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='itemId'][data-testid='"
                            + newItemTestId + "']"
            );

            // Locate the Discount Rate cell within the same AG Grid row as the new item.
            By discountRateCellLocator = By.xpath(
                    "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                            + newItemTestId
                            + "']/ancestor::div[@role='row'][1]"
                            + "//div[@role='gridcell' and @col-id='discountRate']"
            );

            // Locate the inline editor specifically within the new item's Discount Rate cell.
            By discountRateEditorLocator = By.xpath(
                    "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                            + newItemTestId
                            + "']/ancestor::div[@role='row'][1]"
                            + "//div[@role='gridcell' and @col-id='discountRate']"
                            + "//input[contains(@class,'ag-input-field-input') "
                            + "and contains(@class,'ag-text-field-input')]"
            );

            // Wait until the new item's anchor cell is visible.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Scroll the new item into view before locating its Discount Rate cell.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    newItemCell
            );

            // Wait until the Discount Rate cell in the identified row is visible.
            WebElement discountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            System.out.println(
                    "New item's Discount Rate cell found. Test ID: "
                            + newItemTestId
            );

            System.out.println(
                    "Discount Rate before editing: ["
                            + discountRateCell.getText().trim() + "]"
            );

            // Click the target cell to establish AG Grid keyboard focus.
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            discountRateCellLocator
                    )
            ).click();

            // Re-locate the cell and activate inline editing with F2.
            discountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            discountRateCell.sendKeys(Keys.F2);

            // Wait until the editor inside the new item's Discount Rate cell appears.
            WebElement discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateEditorLocator
                    )
            );

            System.out.println("Discount Rate editor activated.");

            // Set the complete discount value using the native input value setter.
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
                    discountRate.trim()
            );

            // Verify that the editor contains the requested discount value.
            wait.until(currentDriver -> {
                try {
                    WebElement currentInput =
                            currentDriver.findElement(
                                    discountRateEditorLocator
                            );

                    return discountRate.trim().equals(
                            currentInput.getAttribute("value")
                    );

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Rate entered in editor: ["
                            + discountRate.trim() + "]"
            );

            // Re-locate the editor before committing the value.
            discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateEditorLocator
                    )
            );

            // Commit the entered value by pressing TAB.
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

            // Read the committed value from the Discount Rate cell in the new item's row.
            WebElement updatedDiscountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            String finalDiscountRate =
                    updatedDiscountRateCell.getText().trim();

            System.out.println(
                    "Discount Rate after commit: ["
                            + finalDiscountRate + "]"
            );

            System.out.println(
                    "Discount Rate entered successfully for the new item row."
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Discount Rate ["
                            + discountRate
                            + "] for new item row with test ID ["
                            + newItemTestId + "].",
                    e
            );
        }
    }

    /**
     * Enters the requested Discount Value into the Discount Value cell
     * of the newly added Foreign Purchase Invoice AG Grid row.
     *
     * Uses newItemTestId to identify the correct item row and the stable
     * col-id='discountValue' to locate the Discount Value cell.
     *
     * The cell is expected to receive keyboard focus after the Discount Rate
     * value is committed. The first character activates inline editing,
     * the complete value is assigned through the native input setter,
     * and TAB commits the value.
     *
     * @param discountValue the discount amount to enter
     */
    public void enterDiscountValue(String discountValue) {

        System.out.println("========================================");
        System.out.println("Entering Foreign Invoice Discount Value");
        System.out.println("Requested discount value: " + discountValue);

        // Validate the requested discount value before interacting with the grid.
        if (discountValue == null || discountValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Discount Value must not be null or empty."
            );
        }

        // Ensure the new item's test ID is available to identify its row.
        if (newItemTestId == null || newItemTestId.isBlank()) {
            throw new IllegalStateException(
                    "newItemTestId is not initialized. "
                            + "Add and identify the invoice item row first."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Stable locator for the new item's cell, used as the row anchor.
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        // Locate the Discount Value cell inside the same row as the new item.
        By discountValueCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='discountValue']"
        );

        // Locate the active editor inside the Discount Value cell of the identified row.
        // The XPath deliberately uses //input, without the invalid preceding dot.
        By discountValueEditorLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='discountValue']"
                        + "//input[contains(@class,'ag-input-field-input') "
                        + "and contains(@class,'ag-text-field-input')]"
        );

        try {
            // Wait until the new item's anchor cell is visible.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Scroll the new item row into view before locating its Discount Value cell.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    newItemCell
            );

            // Wait until the Discount Value cell in the identified row is visible.
            WebElement discountCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            );

            System.out.println(
                    "Discount Value cell found in new item row: "
                            + newItemTestId
            );

            System.out.println(
                    "Discount Value before editing: ["
                            + discountCell.getText().trim() + "]"
            );

            // Preserve the original method's logic: wait for focus from the preceding edit.
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
            String firstCharacter = discountValue.trim().substring(0, 1);

            new Actions(driver)
                    .sendKeys(firstCharacter)
                    .perform();

            System.out.println(
                    "First Discount Value character sent: " + firstCharacter
            );

            // Wait until the inline editor appears inside the identified row.
            WebElement discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            System.out.println("Discount Value editor activated.");

            // Assign the complete value using the native HTML input value setter.
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
                    discountEditor,
                    discountValue.trim()
            );

            // Verify the complete value, re-locating the editor on every polling attempt.
            wait.until(currentDriver -> {
                try {
                    WebElement currentEditor =
                            currentDriver.findElement(
                                    discountValueEditorLocator
                            );

                    return discountValue.trim().equals(
                            currentEditor.getAttribute("value")
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Value verified in editor: ["
                            + discountValue.trim() + "]"
            );

            // Re-find the editor immediately before committing the value.
            discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // TAB commits the value without intentionally adding another row.
            discountEditor.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Discount Value.");

            // Wait until the target cell exits inline-editing mode.
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

            // Re-find the cell after AG Grid commits and renders the value.
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
                    "Failed to enter Discount Value ["
                            + discountValue
                            + "] for new item row ["
                            + newItemTestId + "].",
                    e
            );
        }
    }

    /**
     * Enters the Addition Rate (%) value into the currently focused
     * Addition Rate cell in the Foreign Purchase Invoice AG Grid.
     *
     * Uses stable AG Grid attributes (role, col-id, and editing state)
     * without data-testid, generated IDs, or dynamic row indexes.
     *
     * The method expects the Addition Rate cell to receive keyboard focus
     * naturally after the preceding cell is committed.
     *
     * It activates inline editing with the first character, assigns the
     * complete value using the native HTML input setter, verifies the value,
     * and commits it using TAB instead of ENTER to avoid triggering Add Line.
     *
     * @param extraRate the addition percentage to enter
     */
    public void enterExtraRate(String extraRate) {

        System.out.println("========================================");
        System.out.println("Entering Foreign Invoice Addition Rate (%)");
        System.out.println("Requested addition rate: " + extraRate);

        // Validate the requested value before interacting with AG Grid.
        if (extraRate == null || extraRate.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Rate (%) cannot be null or blank."
            );
        }

        String expectedValue = extraRate.trim();

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        /*
         * Stable locator for the focused Addition Rate (%) cell.
         * Uses the semantic gridcell role, stable column identifier,
         * and AG Grid focus class without generated IDs or test IDs.
         */
        By extraRateCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraRate'].ag-cell-focus"
        );

        /*
         * Stable locator for the inline editor inside the Addition Rate cell.
         * The editor is located by its parent column and AG Grid editing state.
         */
        By extraRateEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraRate']"
                        + ".ag-cell-inline-editing input"
        );

        try {
            // Wait until the Addition Rate cell has keyboard focus.
            WebElement focusedExtraRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateCellLocator
                    )
            );

            System.out.println(
                    "Addition Rate (%) cell is focused."
            );

            // Send the first character to activate AG Grid inline editing.
            String firstCharacter = expectedValue.substring(0, 1);

            new Actions(driver)
                    .sendKeys(firstCharacter)
                    .perform();

            System.out.println(
                    "First character sent to activate editing: "
                            + firstCharacter
            );

            // Wait until the inline editor appears inside the target column.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateEditorLocator
                    )
            );

            System.out.println(
                    "Addition Rate (%) editor is visible."
            );

            // Re-locate the active editor before assigning the complete value.
            WebElement extraRateEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateEditorLocator
                    )
            );

            /*
             * Set the complete value using the native HTML input setter,
             * then dispatch input and change events so the application
             * can detect the updated value.
             */
            js.executeScript(
                    """
                    const input = arguments[0];
                    const value = arguments[1];
    
                    const descriptor = Object.getOwnPropertyDescriptor(
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
                    extraRateEditor,
                    expectedValue
            );

            // Verify the editor value while handling possible AG Grid re-renders.
            wait.until(currentDriver -> {
                try {
                    WebElement currentEditor =
                            currentDriver.findElement(
                                    extraRateEditorLocator
                            );

                    return expectedValue.equals(
                            currentEditor.getAttribute("value")
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            // Re-locate the editor and verify the exact value before committing.
            WebElement currentExtraRateEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateEditorLocator
                    )
            );

            String actualEditorValue =
                    currentExtraRateEditor.getAttribute("value");

            System.out.println(
                    "Addition Rate (%) value before commit: ["
                            + actualEditorValue + "]"
            );

            if (!expectedValue.equals(actualEditorValue)) {
                throw new IllegalStateException(
                        "Addition Rate (%) value mismatch. Requested=["
                                + expectedValue
                                + "], Actual=["
                                + actualEditorValue + "]"
                );
            }

            /*
             * Commit the value using TAB.
             * ENTER is intentionally avoided because it may trigger Add Line.
             */
            currentExtraRateEditor.sendKeys(Keys.TAB);

            System.out.println(
                    "TAB sent to commit Addition Rate (%)."
            );

            // Wait until inline editing ends for the Addition Rate cell.
            By extraRateCellAfterCommitLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='extraRate']"
                            + ":not(.ag-cell-inline-editing)"
            );

            WebElement committedExtraRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateCellAfterCommitLocator
                    )
            );

            String committedValue =
                    committedExtraRateCell.getText().trim();

            System.out.println(
                    "Addition Rate (%) after commit: ["
                            + committedValue + "]"
            );

            System.out.println(
                    "Addition Rate (%) entered and committed successfully."
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Addition Rate (%) ["
                            + expectedValue
                            + "] in the Foreign Purchase Invoice.",
                    e
            );
        }
    }

    /**
     * Enters the Addition Value into the currently focused AG Grid cell
     * in the Foreign Purchase Invoice.
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
     * @param extraValue the addition amount to enter
     */
    public void enterExtraValue(String extraValue) {

        System.out.println("========================================");
        System.out.println("Entering Foreign Invoice Addition Value");
        System.out.println("Requested addition value: " + extraValue);

        // Validate the requested value before interacting with AG Grid.
        if (extraValue == null || extraValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Value cannot be null or blank."
            );
        }

        String expectedValue = extraValue.trim();

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        /*
         * Stable locator for the focused Addition Value cell.
         * Uses the semantic gridcell role, stable col-id, and AG Grid
         * focus class without relying on test IDs or generated IDs.
         */
        By extraValueCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue'].ag-cell-focus"
        );

        /*
         * Stable locator for the active inline editor inside the
         * Addition Value column.
         */
        By extraValueEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue']"
                        + ".ag-cell-inline-editing input"
        );

        try {
            /*
             * Wait until the Addition Value cell is visible and has
             * keyboard focus. The cell is not clicked because the existing
             * keyboard navigation is expected to focus it naturally.
             */
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(extraValueCellLocator);

                    String classes = currentCell.getAttribute("class");

                    return currentCell.isDisplayed()
                            && classes != null
                            && classes.contains("ag-cell-focus");

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Addition Value cell received keyboard focus."
            );

            /*
             * Send the first character to activate inline editing.
             * The complete value will be assigned after the editor appears.
             */
            String firstCharacter = expectedValue.substring(0, 1);

            new Actions(driver)
                    .sendKeys(firstCharacter)
                    .perform();

            System.out.println(
                    "First character sent to activate editing: "
                            + firstCharacter
            );

            // Wait for the inline input editor to appear inside this column.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraValueEditorLocator
                    )
            );

            System.out.println(
                    "Addition Value inline editor is visible."
            );

            /*
             * Assign the complete value through the native HTML input setter.
             * Re-locate the editor on each attempt in case AG Grid re-renders it.
             */
            boolean valueAssigned = false;

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
                            expectedValue
                    );

                    valueAssigned = true;
                    break;

                } catch (StaleElementReferenceException e) {
                    System.out.println(
                            "Addition Value editor became stale during "
                                    + "assignment. Retry "
                                    + attempt + " of 3."
                    );
                }
            }

            if (!valueAssigned) {
                throw new IllegalStateException(
                        "Could not assign Addition Value because the editor "
                                + "kept changing in the DOM."
                );
            }

            /*
             * Verify that the editor contains the requested value.
             * The editor is freshly located during every polling attempt.
             */
            wait.until(currentDriver -> {
                try {
                    WebElement currentEditor =
                            currentDriver.findElement(
                                    extraValueEditorLocator
                            );

                    return currentEditor.isDisplayed()
                            && expectedValue.equals(
                            currentEditor.getAttribute("value")
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            WebElement verifiedEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraValueEditorLocator
                    )
            );

            String actualEditorValue =
                    verifiedEditor.getAttribute("value");

            System.out.println(
                    "Addition Value verified in editor: ["
                            + actualEditorValue + "]"
            );

            if (!expectedValue.equals(actualEditorValue)) {
                throw new IllegalStateException(
                        "Addition Value mismatch before commit. Expected=["
                                + expectedValue
                                + "], Actual=["
                                + actualEditorValue + "]"
                );
            }

            /*
             * Commit the value using TAB instead of ENTER to avoid
             * accidentally triggering the Add Line action.
             */
            verifiedEditor.sendKeys(Keys.TAB);

            System.out.println(
                    "TAB sent to commit Addition Value."
            );

            /*
             * Wait until the Addition Value cell is no longer in inline-edit
             * mode, then read the committed cell text.
             */
            By extraValueCellAfterCommitLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='extraValue']"
                            + ":not(.ag-cell-inline-editing)"
            );

            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(
                                    extraValueCellAfterCommitLocator
                            );

                    String classes = currentCell.getAttribute("class");

                    return currentCell.isDisplayed()
                            && classes != null
                            && !classes.contains("ag-cell-inline-editing");

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            WebElement committedCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraValueCellAfterCommitLocator
                    )
            );

            String committedValue = committedCell.getText().trim();

            System.out.println(
                    "Addition Value after commit: ["
                            + committedValue + "]"
            );

            System.out.println(
                    "Addition Value committed successfully."
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Addition Value ["
                            + expectedValue
                            + "] in the Foreign Purchase Invoice.",
                    e
            );
        }
    }

    /**
     * Selects today's date from the calendar for the Production Date
     * cell in the Foreign Purchase Invoice AG Grid.
     *
     * Uses the stable AG Grid col-id and a date-specific aria-label.
     * Does not use data-testid, row indexes, or generated element IDs.
     *
     * The Production Date cell is opened with a double-click.
     * Today's date is then located and selected in the calendar.
     *
     * Elements are re-located when necessary to reduce stale-element
     * failures caused by AG Grid and React re-rendering.
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
     * Selects the Expire Date for the targeted Foreign Purchase Invoice row.
     *
     * Uses the stable AG Grid column identifier without data-testid,
     * generated element IDs, or row-index-based locators.
     *
     * Opens the calendar using F2, navigates to the target date,
     * selects it using its accessible aria-label, and verifies the
     * committed value after the calendar closes.
     *
     * IMPORTANT:
     * This locator assumes the intended Expire Date cell is the only
     * matching cell in the current grid context.
     */
    public void selectExpireDate() {

        System.out.println("========================================");
        System.out.println("Selecting Expire Date");

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Calculate the target expiration date: 30 days from today.
        LocalDate expireDate = LocalDate.now().plusDays(30);

        System.out.println("Target Expire Date: " + expireDate);

        /*
         * Stable locator for the Expire Date AG Grid cell.
         * Uses the permanent column identifier from the supplied HTML.
         */
        By expireDateCellLocator = By.cssSelector(
                newRowCellSelector("expireDate")
        );

        /*
         * Locator for the visible React Modern DatePicker calendar.
         */
        By calendarWrapperLocator = By.cssSelector(
                ".rmdp-wrapper"
        );

        /*
         * Build the accessible label for the requested date.
         * The label is calculated at runtime rather than hard-coded.
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
         * Locator for the exact date inside the open calendar.
         * Assumes the calendar uses the supplied aria-label format.
         */
        By targetDateLocator = By.cssSelector(
                ".rmdp-wrapper [aria-label='"
                        + targetDateAriaLabel + "']"
        );

        /*
         * Locator for the Next Month control in the RMDP calendar.
         */
        By nextMonthLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-arrow-container.rmdp-right"
        );

        try {
            // Wait for the Expire Date cell to become visible.
            wait.until(driver -> {
                try {
                    return driver.findElements(expireDateCellLocator)
                            .stream()
                            .anyMatch(cell -> {
                                try {
                                    return cell.isDisplayed();
                                } catch (StaleElementReferenceException e) {
                                    return false;
                                }
                            });

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println("Expire Date cell is visible.");

            /*
             * Scroll the cell into view using the stable CSS selector.
             * The DOM is queried again instead of reusing an old element.
             */
            Boolean cellScrolledIntoView = (Boolean)
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

            if (!Boolean.TRUE.equals(cellScrolledIntoView)) {
                throw new IllegalStateException(
                        "Could not scroll the Expire Date cell into view."
                );
            }

            /*
             * Click the cell to focus it, then send F2 to open its editor
             * or associated calendar.
             */
            WebElement expireDateCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            expireDateCellLocator
                    )
            );

            expireDateCell.click();

            // Re-locate the cell after clicking in case AG Grid re-rendered.
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            expireDateCellLocator
                    )
            ).sendKeys(Keys.F2);

            System.out.println(
                    "F2 sent. Waiting for the Expire Date calendar..."
            );

            // Wait until the calendar becomes visible.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            calendarWrapperLocator
                    )
            );

            System.out.println("Expire Date calendar opened.");
            System.out.println(
                    "Target date aria-label: " + targetDateAriaLabel
            );

            /*
             * Search the currently displayed month for the target date.
             * Navigate forward one month at a time when the date is absent.
             */
            int maxMonthNavigationAttempts = 12;
            boolean targetDateVisible = false;

            for (int attempt = 0;
                 attempt <= maxMonthNavigationAttempts;
                 attempt++) {

                targetDateVisible = driver.findElements(targetDateLocator)
                        .stream()
                        .anyMatch(date -> {
                            try {
                                return date.isDisplayed();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        });

                if (targetDateVisible) {
                    break;
                }

                if (attempt == maxMonthNavigationAttempts) {
                    break;
                }

                System.out.println(
                        "Target date not visible. Moving to next month: "
                                + (attempt + 1)
                                + " of " + maxMonthNavigationAttempts
                );

                // Re-locate the Next Month control before clicking.
                WebElement nextMonthControl = wait.until(driver -> {
                    try {
                        for (WebElement control :
                                driver.findElements(nextMonthLocator)) {

                            try {
                                if (control.isDisplayed()
                                        && control.isEnabled()) {
                                    return control;
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // Retry with a fresh element.
                            }
                        }

                        return null;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                nextMonthControl.click();

                // Wait for the calendar to remain visible after navigation.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                calendarWrapperLocator
                        )
                );
            }

            if (!targetDateVisible) {
                throw new IllegalStateException(
                        "Could not locate Expire Date "
                                + expireDate
                                + " after "
                                + maxMonthNavigationAttempts
                                + " month navigation attempts."
                );
            }

            /*
             * Re-locate and click the exact target date.
             */
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            targetDateLocator
                    )
            ).click();

            System.out.println(
                    "Selected Expire Date: " + expireDate
            );

            // Wait for the calendar to close after selecting the date.
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            calendarWrapperLocator
                    )
            );

            /*
             * Read the committed value from the grid cell after the
             * calendar closes and the UI has updated.
             */
            String committedExpireDate = wait.until(driver -> {
                try {
                    for (WebElement cell :
                            driver.findElements(expireDateCellLocator)) {

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
                    "Expire Date cell value after selection: ["
                            + committedExpireDate + "]"
            );

            System.out.println(
                    "Expire Date selected successfully."
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to select Expire Date ["
                            + expireDate + "].",
                    e
            );
        }
    }

    /**
     * Enters the Note (البيان) value into the AG Grid Note cell
     * and commits the edit by clicking outside the grid editor.
     *
     * Uses the stable AG Grid column identifier (col-id='note')
     * without generated element IDs, data-testid attributes,
     * or row-index-based locators.
     *
     * JavaScript uses the native input value setter and dispatches
     * input/change events to notify React and AG Grid of the new value.
     *
     * TAB and ENTER are avoided because they may navigate to another
     * cell or create an additional row.
     *
     * @param note the text to enter into the Note cell
     */
    public void enterNote(String note) {

        System.out.println("========================================");
        System.out.println("Entering Note (البيان)");
        System.out.println("Requested note: [" + note + "]");

        // Validate the requested Note value before interacting with the UI.
        if (note == null) {
            throw new IllegalArgumentException(
                    "Note cannot be null."
            );
        }

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Note grid cell.
         * Uses the semantic gridcell role and fixed column identifier.
         */
        By noteCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note']"
        );

        /*
         * Locator for the inline text editor inside the Note cell.
         * This locator does not depend on generated React or AG Grid IDs.
         */
        By noteInputLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note']"
                        + ".ag-cell-inline-editing input.ag-input-field-input"
        );

        /*
         * Locator for the external Discounts and Additions section.
         * Clicking this section moves focus outside the active grid editor.
         */
        By discountsAndAdditionsLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' and not(*)]"
        );

        /*
         * Wait for a visible Note cell and re-find it during polling
         * to reduce stale-element failures caused by AG Grid rendering.
         */
        WebElement noteCell = wait.until(currentDriver -> {
            try {
                List<WebElement> cells =
                        currentDriver.findElements(noteCellLocator);

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

            } catch (StaleElementReferenceException ignored) {
                return null;
            }
        });

        // Scroll the Note cell into the viewport before editing.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                noteCell
        );

        System.out.println("Note cell found and scrolled into view.");

        /*
         * Double-click the Note cell to activate inline editing.
         */
        new Actions(driver)
                .doubleClick(noteCell)
                .perform();

        System.out.println("Note cell double-clicked.");

        /*
         * Wait until AG Grid creates and displays the Note input editor.
         */
        WebElement noteInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println("Note editor opened.");

        /*
         * Set the requested text through the native HTML input setter.
         * Dispatch input/change events so React and AG Grid can detect it.
         */
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
                noteInput,
                note
        );

        /*
         * Verify that the editor contains the exact requested text.
         * Re-locate the editor during polling in case of re-rendering.
         */
        String editorValue = wait.until(currentDriver -> {
            try {
                WebElement currentInput =
                        currentDriver.findElement(noteInputLocator);

                String currentValue =
                        currentInput.getAttribute("value");

                return note.equals(currentValue)
                        ? currentValue
                        : null;

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException ignored
            ) {
                return null;
            }
        });

        System.out.println(
                "Note editor value verified: [" + editorValue + "]"
        );

        /*
         * Wait for the external section to become clickable.
         * This avoids using TAB or ENTER to commit the cell.
         */
        WebElement discountsAndAdditions = wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAndAdditionsLocator
                )
        );

        // Click outside the editor to commit the Note value.
        discountsAndAdditions.click();

        System.out.println(
                "Clicked 'الخصومات والإضافات' to commit the Note."
        );

        /*
         * Wait until the inline editor disappears, indicating that
         * AG Grid has exited editing mode.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        noteInputLocator
                )
        );

        System.out.println("Note editor closed successfully.");

        System.out.println(
                "Note committed successfully: [" + note + "]"
        );

        System.out.println("========================================");
    }

    /**
     * Clicks the Add Line button in the Discounts & Additions tab
     * and waits until a new ordinary AG Grid row appears.
     *
     * Uses the stable Arabic button text and the tab's content
     * to avoid generated data-testid values and DOM element IDs.
     *
     * This method is separate from clickAddLine(), which adds rows
     * to the main invoice items grid.
     */
    public void clickAddDiscountAdditionLine() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Discounts & Additions tab heading.
         * Uses the visible Arabic text instead of a generated ID.
         */
        By discountsAndAdditionsTabLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' and not(*)]"
        );

        /*
         * Stable locator for the Add Line button.
         * Uses the button role and its visible Arabic label.
         */
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space(.)='اضافة سطر']"
        );

        /*
         * Locator for ordinary data rows in AG Grid.
         * Excludes pinned rows whose row-index starts with 'b-'.
         */
        By centerRowsLocator = By.cssSelector(
                "div.ag-root[role='grid'] "
                        + "div.ag-center-cols-container "
                        + "> div[role='row'][row-index]"
                        + ":not([row-index^='b-'])"
        );

        /*
         * Ensure the Discounts & Additions section is available
         * before attempting to click its Add Line button.
         */
        WebElement discountsTab = wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAndAdditionsTabLocator
                )
        );

        discountsTab.click();

        System.out.println(
                "Opened the Discounts & Additions tab."
        );

        /*
         * Wait for the Add Line button to become clickable.
         */
        WebElement addLineButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        addLineButtonLocator
                )
        );

        /*
         * Count ordinary AG Grid rows before clicking Add Line.
         */
        int rowsBefore = driver.findElements(
                centerRowsLocator
        ).size();

        System.out.println(
                "Discounts & Additions rows before Add Line: "
                        + rowsBefore
        );

        /*
         * Click Add Line to request a new row in the selected section.
         */
        addLineButton.click();

        System.out.println(
                "Clicked Add Line in Discounts & Additions."
        );

        /*
         * Wait until the number of ordinary rows increases.
         * This verifies that AG Grid rendered an additional row.
         */
        wait.until(currentDriver ->
                currentDriver.findElements(centerRowsLocator).size()
                        > rowsBefore
        );

        System.out.println(
                "A new Discounts & Additions row was detected successfully."
        );
    }

    /**
     * Captures the row-index of the newly added row in the Foreign
     * Purchase Invoice Discounts & Additions AG Grid.
     *
     * Uses the stable accountId column identifier to locate ordinary
     * data rows in the Foreign Purchase Invoice grid.
     *
     * Excludes pinned rows, including the totals row with row-index="b-0".
     * Does not use generated DOM IDs or dynamic data-testid values.
     *
     * Call this method after clicking Add Line and waiting for the
     * new row to be rendered.
     */
    public void captureNewDiscountAdditionRowIndex() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Locate the Foreign Purchase Invoice Discounts & Additions Grid
         * through its stable accountId column header.
         */
        By foreignInvoiceGridLocator = By.xpath(
                "//div[@role='grid']"
                        + "[.//div[@role='columnheader' and @col-id='accountId']]"
                        + "[.//div[@role='columnheader' and @col-id='additionRate']]"
                        + "[.//div[@role='columnheader' and @col-id='discountRate']]"
        );

        /*
         * Locate ordinary data rows within the identified grid.
         * Excludes pinned rows and requires a numeric row-index.
         */
        By ordinaryRowsLocator = By.cssSelector(
                "div.ag-center-cols-container"
                        + " > div[role='row'][row-index]"
                        + ":not([row-index^='b-'])"
        );

        /*
         * Locate the accountId cell within an ordinary data row.
         * This confirms that the row belongs to the expected grid structure.
         */
        By accountCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='accountId']"
        );

        /*
         * Wait for the intended grid to be present before reading its rows.
         */
        WebElement foreignInvoiceGrid = wait.until(
                ExpectedConditions.presenceOfElementLocated(
                        foreignInvoiceGridLocator
                )
        );

        /*
         * Wait until at least one ordinary data row is rendered.
         * The totals row is not included in this locator.
         */
        List<WebElement> rows = wait.until(currentDriver -> {

            try {
                WebElement currentGrid =
                        currentDriver.findElement(foreignInvoiceGridLocator);

                List<WebElement> currentRows =
                        currentGrid.findElements(ordinaryRowsLocator);

                return currentRows.isEmpty() ? null : currentRows;

            } catch (
                    StaleElementReferenceException |
                    NoSuchElementException ignored
            ) {
                return null;
            }
        });

        /*
         * Capture the last ordinary row, assuming Add Line appends
         * the newly created row at the end of the grid.
         */
        WebElement lastRow = rows.get(rows.size() - 1);

        /*
         * Verify that the selected row contains the expected accountId cell.
         */
        if (lastRow.findElements(accountCellLocator).isEmpty()) {
            throw new IllegalStateException(
                    "The selected row does not contain the expected "
                            + "accountId cell in the Foreign Purchase Invoice Grid."
            );
        }

        /*
         * Read and validate the row-index before storing it.
         */
        String rowIndex = lastRow.getAttribute("row-index");

        if (rowIndex == null
                || rowIndex.isBlank()
                || !rowIndex.matches("\\d+")) {

            throw new IllegalStateException(
                    "Could not determine a valid row-index for the new "
                            + "Foreign Purchase Invoice Discounts & Additions row."
            );
        }

        newDiscountAdditionRowIndex = Integer.parseInt(rowIndex);

        System.out.println(
                "Foreign Purchase Invoice Discounts & Additions row index: "
                        + newDiscountAdditionRowIndex
        );
    }


    /**
     * Selects a random Account (الحساب) from the Account cell
     * in the specified row of the Foreign Purchase Invoice
     * Discounts & Additions grid.
     *
     * Uses the stable AG Grid column identifier "accountId"
     * and the previously captured newDiscountAdditionRowIndex.
     *
     * Does not depend on generated React IDs or dynamic data-testid values.
     * ENTER activates the React Select editor, then a random visible,
     * enabled option is selected and committed.
     */
    public void selectRandomAccount() {

        System.out.println("========================================");
        System.out.println("Selecting random Account");
        System.out.println(
                "Discounts & Additions row index: "
                        + newDiscountAdditionRowIndex
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Account cell in the captured row.
         * Uses the fixed column identifier and the stored row index.
         */
        By accountCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='accountId']"
        );

        // Wait until the Account cell in the target row is visible.
        WebElement accountCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountCellLocator
                )
        );

        System.out.println(
                "Account cell found. Current value: ["
                        + accountCell.getText().trim()
                        + "]"
        );

        /*
         * Scroll the cell into view to handle grid positioning
         * and horizontally scrollable columns.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                accountCell
        );

        // Re-locate the cell after scrolling in case AG Grid re-rendered it.
        accountCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        accountCellLocator
                )
        );

        // Click the cell to give AG Grid keyboard focus.
        accountCell.click();

        // Press ENTER to activate the Account React Select editor.
        accountCell.sendKeys(Keys.ENTER);

        /*
         * Stable locator for the React Select input inside an Account cell.
         * Does not use generated input IDs.
         */
        By accountInputLocator = By.cssSelector(
                "div[role='gridcell'][col-id='accountId'] input"
        );

        // Wait until the Account editor input becomes visible.
        WebElement accountInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountInputLocator
                )
        );

        System.out.println("Account React Select editor opened.");

        // Click the input to display the available Account options.
        accountInput.click();

        /*
         * Locate the visible dropdown list without relying on
         * React Select's generated listbox ID.
         */
        By accountListboxLocator = By.cssSelector(
                "div[role='listbox']"
        );

        WebElement accountListbox = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        accountListboxLocator
                )
        );

        /*
         * Retrieve the options from the currently visible listbox.
         * Exclude options that are hidden or explicitly disabled.
         */
        List<WebElement> selectableOptions = accountListbox.findElements(
                        By.cssSelector("[role='option']")
                )
                .stream()
                .filter(WebElement::isDisplayed)
                .filter(option ->
                        !"true".equals(
                                option.getAttribute("aria-disabled")
                        )
                )
                .toList();

        System.out.println(
                "Selectable Account options: "
                        + selectableOptions.size()
        );

        // Fail clearly if the dropdown contains no selectable options.
        if (selectableOptions.isEmpty()) {
            throw new IllegalStateException(
                    "No visible, selectable Account options were found "
                            + "in the Discounts & Additions grid."
            );
        }

        // Select one option randomly from the available Accounts.
        Random random = new Random();

        WebElement randomAccount = selectableOptions.get(
                random.nextInt(selectableOptions.size())
        );

        String selectedAccount = randomAccount.getText().trim();

        System.out.println(
                "Random Account selected: [" + selectedAccount + "]"
        );

        // Click the selected option to commit the Account value.
        randomAccount.click();

        /*
         * Wait until the editor closes, indicating that the selection
         * has been committed to the grid cell.
         */
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        accountInputLocator
                )
        );

        System.out.println(
                "Account selected successfully: ["
                        + selectedAccount
                        + "]"
        );

        System.out.println("========================================");
    }


    /**
     * Selects a random available option from the Influencing on Cost
     * cell in the Foreign Purchase Invoice Discounts & Additions grid.
     *
     * Uses the captured newDiscountAdditionRowIndex and the stable
     * AG Grid column identifier "influencingOnCost".
     *
     * Double-clicks the cell to activate its editor, retries if the
     * options do not appear, and preserves an existing cell value.
     * Verifies that the selected value is displayed in the cell.
     *
     * Does not use generated IDs or press TAB.
     */
    public void selectRandomInfluencingOnCost() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(15));

        /*
         * Stable locator for the Influencing on Cost cell
         * in the captured Discounts & Additions row.
         */
        By influencingOnCostCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='influencingOnCost']"
        );

        /*
         * Stable locator for selectable dropdown options.
         * Excludes options explicitly marked as disabled.
         */
        By optionLocator = By.cssSelector(
                "[role='option']:not([aria-disabled='true'])"
        );

        final int maxAttempts = 2;

        System.out.println("========================================");
        System.out.println("Selecting random Influencing on Cost");
        System.out.println(
                "Discounts & Additions row index: "
                        + newDiscountAdditionRowIndex
        );

        try {

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                System.out.println(
                        "Activation attempt: " + attempt + "/" + maxAttempts
                );

                // Wait for the target cell in the captured row.
                WebElement cell = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                influencingOnCostCellLocator
                        )
                );

                // Preserve the current selection if the cell already has a value.
                String currentValue = cell.getText().trim();

                if (!currentValue.isEmpty()) {
                    System.out.println(
                            "Influencing on Cost already selected: "
                                    + currentValue
                    );
                    return;
                }

                // Scroll the cell into view before activating its editor.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                        cell
                );

                // Re-locate the cell after scrolling in case AG Grid re-rendered it.
                cell = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                influencingOnCostCellLocator
                        )
                );

                // Double-click to activate the cell editor.
                new Actions(driver)
                        .doubleClick(cell)
                        .perform();

                /*
                 * Wait for visible, enabled options.
                 * Re-query the DOM during polling to handle re-rendering.
                 */
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
                                if (option.isDisplayed()
                                        && option.isEnabled()) {
                                    visibleOptions.add(option);
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // Ignore options replaced during re-rendering.
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

                    // Log the current cell HTML to help diagnose activation failures.
                    try {
                        WebElement currentCell = driver.findElement(
                                influencingOnCostCellLocator
                        );

                        System.out.println(
                                "Cell HTML: "
                                        + currentCell.getAttribute("outerHTML")
                        );

                    } catch (NoSuchElementException
                             | StaleElementReferenceException ignored) {

                        System.out.println(
                                "Target cell was unavailable during diagnostics."
                        );
                    }

                    // Retry once before reporting a timeout.
                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "Influencing on Cost options did not appear after "
                                    + maxAttempts + " attempts.",
                            dropdownTimeout
                    );
                }

                /*
                 * Collect distinct, non-empty option labels.
                 * Duplicate labels are kept only once.
                 */
                List<String> optionTexts = new ArrayList<>();

                for (WebElement option : availableOptions) {
                    try {
                        String text = option.getText().trim();

                        if (!text.isEmpty() && !optionTexts.contains(text)) {
                            optionTexts.add(text);
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Ignore options replaced while reading their labels.
                    }
                }

                // Fail clearly if the dropdown contains no readable options.
                if (optionTexts.isEmpty()) {

                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "The Influencing on Cost dropdown opened, "
                                    + "but no readable option labels were found."
                    );
                }

                // Choose one available option randomly.
                String selectedText = optionTexts.get(
                        new Random().nextInt(optionTexts.size())
                );

                System.out.println(
                        "Random Influencing on Cost option: " + selectedText
                );

                /*
                 * Match the exact option text without using generated IDs.
                 * xpathLiteral() must already exist in the page object.
                 */
                By selectedOptionLocator = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedText)
                                + "]"
                );

                // Re-locate the chosen option before clicking it.
                WebElement selectedOption = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOptionLocator
                        )
                );

                selectedOption.click();

                /*
                 * Verify that the selected text is displayed in the target cell.
                 * Re-find the cell on each poll to handle AG Grid re-rendering.
                 */
                wait.until(currentDriver -> {
                    try {
                        WebElement updatedCell = currentDriver.findElement(
                                influencingOnCostCellLocator
                        );

                        return updatedCell.getText()
                                .trim()
                                .equals(selectedText);

                    } catch (StaleElementReferenceException
                             | NoSuchElementException ignored) {
                        return false;
                    }
                });

                System.out.println(
                        "Influencing on Cost selected successfully: "
                                + selectedText
                );

                System.out.println("========================================");
                return;
            }

        } catch (TimeoutException e) {

            // Print the target cell HTML when selection fails.
            System.out.println(
                    "Influencing on Cost selection failed."
            );

            try {
                List<WebElement> cells =
                        driver.findElements(influencingOnCostCellLocator);

                if (!cells.isEmpty()) {
                    System.out.println(
                            "Cell HTML: "
                                    + cells.get(0).getAttribute("outerHTML")
                    );
                }

            } catch (StaleElementReferenceException ignored) {
                System.out.println(
                        "Target cell was re-rendered during diagnostics."
                );
            }

            // Preserve the original exception for TestNG reporting.
            throw e;
        }
    }


    /**
     * Enters the requested Discount Rate into the Discount Rate cell
     * of the captured row in the Foreign Purchase Invoice
     * Discounts & Additions AG Grid.
     *
     * Uses stable AG Grid attributes: row-index and col-id.
     * Does not depend on data-testid or generated element IDs.
     *
     * Activates the inline editor, sets the requested value using
     * JavaScript, commits it with TAB, and verifies the saved value.
     */
    public void enterDiscountRateInDiscountsSection(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Discount Rate cell in the captured row.
         */
        By discountRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='discountRate']"
        );

        /*
         * Stable locator for the native input inside the Discount Rate editor.
         */
        By discountRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='discountRate'] "
                        + "input.ag-input-field-input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Rate in Discounts & Additions");
        System.out.println("Requested discount rate: " + discountRate);

        // Locate the Discount Rate cell in the captured row.
        WebElement discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        System.out.println(
                "Discount Rate cell current value: ["
                        + discountRateCell.getText().trim()
                        + "]"
        );

        // Scroll the target cell into view before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                discountRateCell
        );

        // Re-check the cell after scrolling in case AG Grid re-rendered it.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        /*
         * Activate the inline editor using the existing framework helper.
         * Skip the operation if the cell is disabled or non-editable.
         */
        if (!tryActivateGridCellEditor(
                discountRateCellLocator,
                discountRateInputLocator
        )) {
            System.out.println(
                    "Discount Rate is disabled or non-editable. Skipping."
            );
            return;
        }

        System.out.println("Discount Rate editor activated successfully.");

        // Wait for the native input belonging to the target cell editor.
        WebElement discountRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        /*
         * Set the requested value through the native input value setter
         * and dispatch input/change events for the UI.
         */
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

        // Verify the editor accepted the requested value before committing.
        String editorValue = discountRateInput.getAttribute("value");

        System.out.println(
                "Discount Rate editor value before commit: ["
                        + editorValue + "]"
        );

        if (!discountRate.equals(editorValue)) {
            throw new IllegalStateException(
                    "Discount Rate editor did not accept the requested value. "
                            + "Expected: [" + discountRate
                            + "], Actual: [" + editorValue + "]"
            );
        }

        // Commit the edit using TAB, consistent with the existing framework.
        discountRateInput.sendKeys(Keys.TAB);

        System.out.println("TAB sent to commit Discount Rate.");

        // Wait until the inline editor closes after committing the value.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        discountRateInputLocator
                )
        );

        // Re-locate the cell after AG Grid updates its displayed value.
        discountRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        discountRateCellLocator
                )
        );

        String committedValue = discountRateCell.getText().trim();

        System.out.println(
                "Discount Rate cell value after commit: ["
                        + committedValue + "]"
        );

        /*
         * Compare numeric values so inputs such as 20 and 20.00
         * are treated as equivalent.
         */
        java.math.BigDecimal expectedValue =
                new java.math.BigDecimal(discountRate)
                        .stripTrailingZeros();

        String actualText = committedValue
                .replace("%", "")
                .trim();

        java.math.BigDecimal actualValue =
                new java.math.BigDecimal(actualText)
                        .stripTrailingZeros();

        // Fail explicitly if the committed value differs from the requested one.
        if (expectedValue.compareTo(actualValue) != 0) {
            throw new IllegalStateException(
                    "Discount Rate was not committed correctly. "
                            + "Expected: [" + expectedValue.toPlainString()
                            + "], Actual: [" + actualValue.toPlainString() + "]"
            );
        }

        System.out.println(
                "Discount Rate entered successfully: " + committedValue
        );

        System.out.println("========================================");
    }


    /**
     * Enters the requested Discount Value into the Discount Value cell
     * of the captured row in the Foreign Purchase Invoice
     * Discounts & Additions AG Grid.
     *
     * Uses stable AG Grid attributes: row-index and col-id.
     * Does not depend on data-testid or generated element IDs.
     *
     * Activates the existing inline editor, enters the requested value,
     * verifies the editor value, commits with TAB, and verifies the result.
     *
     * If the cell is disabled or the editor cannot be activated,
     * the method skips the operation.
     *
     * @param discountValue the discount value to enter into the cell
     */
    public void enterDiscountValueInDiscountsSection(String discountValue) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Discount Value cell in the captured row.
         */
        By discountValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='discountValue']"
        );

        /*
         * Stable locator for the native input inside the Discount Value editor.
         */
        By discountValueEditorLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='discountValue'] "
                        + "input.ag-input-field-input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Value");
        System.out.println("Requested value: [" + discountValue + "]");

        try {
            // Wait until the target Discount Value cell is present.
            WebElement discountValueCell = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            discountValueCellLocator
                    )
            );

            // Log the current value before editing.
            String currentValue = discountValueCell.getText().trim();

            System.out.println(
                    "Discount Value before editing: [" + currentValue + "]"
            );

            // Scroll the target cell into view before activating its editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    discountValueCell
            );

            /*
             * Activate the inline editor using the existing framework helper.
             * Skip the operation if the cell is disabled or non-editable.
             */
            boolean editorActivated = tryActivateGridCellEditor(
                    discountValueCellLocator,
                    discountValueEditorLocator
            );

            if (!editorActivated) {
                System.out.println(
                        "Discount Value is disabled or non-editable. Skipping."
                );
                return;
            }

            // Wait until the Discount Value editor becomes visible.
            WebElement discountValueEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // Replace the existing editor value with the requested value.
            discountValueEditor.click();

            discountValueEditor.sendKeys(
                    Keys.chord(Keys.CONTROL, "a")
            );

            discountValueEditor.sendKeys(discountValue);

            // Verify that the editor accepted the requested value.
            String enteredValue =
                    discountValueEditor.getAttribute("value");

            System.out.println(
                    "Discount Value editor before commit: ["
                            + enteredValue + "]"
            );

            if (enteredValue == null
                    || !enteredValue.trim().equals(discountValue.trim())) {

                throw new IllegalStateException(
                        "Discount Value mismatch. Expected ["
                                + discountValue
                                + "] but found ["
                                + enteredValue + "]."
                );
            }

            // Commit the edit using TAB without intentionally adding another row.
            discountValueEditor.sendKeys(Keys.TAB);

            // Wait until the displayed cell value matches the requested number.
            wait.until(currentDriver -> {
                try {
                    WebElement updatedCell = currentDriver.findElement(
                            discountValueCellLocator
                    );

                    String displayedValue = updatedCell.getText().trim();

                    java.math.BigDecimal expected =
                            new java.math.BigDecimal(discountValue.trim());

                    String normalizedText = displayedValue
                            .replace(",", "")
                            .replace("%", "")
                            .trim();

                    java.math.BigDecimal actual =
                            new java.math.BigDecimal(normalizedText);

                    return expected.compareTo(actual) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException ignored) {
                    return false;

                } catch (NumberFormatException ignored) {
                    return false;
                }
            });

            // Read the final displayed value after the commit.
            String committedValue = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            ).getText().trim();

            System.out.println(
                    "Discount Value saved successfully: ["
                            + committedValue + "]"
            );

            System.out.println("========================================");

        } catch (TimeoutException e) {

            // Report a missing cell or editor with the captured row index.
            throw new IllegalStateException(
                    "Timed out while locating or updating Discount Value "
                            + "in row-index ["
                            + newDiscountAdditionRowIndex + "].",
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

        // Locate the Addition Rate cell using its stable column identifier
        // and the stored index of the new Discounts & Additions row.
        By additionRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='additionRate']"
        );

        // Locate the native input editor inside the Addition Rate cell.
        // The input is expected to appear when AG Grid activates editing.
        By additionRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='additionRate'] "
                        + "input.ag-input-field-input"
        );

        // Wait until the target Addition Rate cell is visible.
        WebElement additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        System.out.println(
                "Addition Rate cell found. Current value: ["
                        + additionRateCell.getText().trim()
                        + "]"
        );

        // Scroll the target cell into view before activating its editor.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                additionRateCell
        );

        // Re-locate the cell after scrolling in case AG Grid re-renders it.
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        // Activate the inline editor using the existing framework helper.
        // Skip the cell if editing is disabled or cannot be activated.
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

        // Wait for the native input editor to become visible.
        WebElement additionRateInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        System.out.println("Addition Rate editor found.");

        // Set the input value through the native HTML setter and dispatch
        // input/change events so the application detects the modification.
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

        // Verify the entered value before committing it.
        String editorValue = additionRateInput.getAttribute("value");

        System.out.println(
                "Addition Rate editor value BEFORE commit: ["
                        + editorValue
                        + "]"
        );

        if (editorValue == null
                || !additionRate.equals(editorValue.trim())) {

            throw new IllegalStateException(
                    "Addition Rate editor did not accept the requested value. "
                            + "Expected: [" + additionRate
                            + "], Actual: [" + editorValue + "]"
            );
        }

        // Commit the value with TAB to avoid unintended row-related behavior.
        additionRateInput.sendKeys(Keys.TAB);

        System.out.println("TAB sent to commit Addition Rate.");

        // Wait for the inline editor to disappear after committing.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        additionRateInputLocator
                )
        );

        // Re-locate the cell after AG Grid renders the committed value.
        additionRateCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionRateCellLocator
                )
        );

        String committedValue = additionRateCell.getText().trim();

        System.out.println(
                "Addition Rate cell value after commit: ["
                        + committedValue
                        + "]"
        );

        // Normalize decimal formatting so values such as 20 and 20.00
        // compare equally.
        java.math.BigDecimal expectedValue =
                new java.math.BigDecimal(additionRate)
                        .stripTrailingZeros();

        java.math.BigDecimal actualValue;

        try {
            actualValue = new java.math.BigDecimal(
                    committedValue.replace(",", "")
                            .replace("%", "")
                            .trim()
            ).stripTrailingZeros();

        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Addition Rate cell contains an invalid value: ["
                            + committedValue + "]",
                    e
            );
        }

        // Verify that the committed value matches the requested value.
        if (expectedValue.compareTo(actualValue) != 0) {
            throw new IllegalStateException(
                    "Addition Rate was not committed correctly. "
                            + "Expected: [" + expectedValue.toPlainString()
                            + "], Actual: [" + actualValue.toPlainString() + "]"
            );
        }

        System.out.println(
                "Addition Rate entered successfully: ["
                        + committedValue + "]"
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

        // Locate the Addition Value cell using its stable column identifier
        // and the stored index of the new Discounts & Additions row.
        By additionValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='additionValue']"
        );

        // Locate the native input editor inside the Addition Value cell.
        // This editor is expected to appear when AG Grid activates editing.
        By additionValueInputLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='additionValue'] "
                        + "input.ag-input-field-input"
        );

        // Wait until the target cell is visible before reading its current value.
        WebElement additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueCellLocator
                )
        );

        System.out.println(
                "Addition Value cell found. Current value: ["
                        + additionValueCell.getText().trim()
                        + "]"
        );

        // Scroll the target cell into view before activating its editor.
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
        // Skip the operation if the cell is disabled or cannot be edited.
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

        // Set the requested value through the native HTML input setter and
        // dispatch input/change events so the application detects the change.
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

        // Verify the editor accepted the requested value before committing it.
        String editorValue = additionValueInput.getAttribute("value");

        System.out.println(
                "Addition Value editor value BEFORE commit: ["
                        + editorValue + "]"
        );

        if (editorValue == null
                || !additionValue.equals(editorValue.trim())) {
            throw new IllegalStateException(
                    "Addition Value editor did not accept the requested value. "
                            + "Expected: [" + additionValue
                            + "], Actual: [" + editorValue + "]"
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

        // Re-locate the cell after AG Grid renders the committed value.
        additionValueCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        additionValueCellLocator
                )
        );

        String committedValue = additionValueCell.getText().trim();

        System.out.println(
                "Addition Value cell value after commit: ["
                        + committedValue + "]"
        );

        // Normalize decimal formatting so that 20 and 20.00 compare equally.
        java.math.BigDecimal normalizedExpected =
                new java.math.BigDecimal(additionValue)
                        .stripTrailingZeros();

        java.math.BigDecimal normalizedActual;

        try {
            normalizedActual = new java.math.BigDecimal(
                    committedValue.replace(",", "")
                            .replace("%", "")
                            .trim()
            ).stripTrailingZeros();

        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                    "Addition Value cell contains an invalid value: ["
                            + committedValue + "]",
                    e
            );
        }

        // Confirm that the displayed value matches the requested value.
        if (normalizedExpected.compareTo(normalizedActual) != 0) {
            throw new IllegalStateException(
                    "Addition Value was not committed correctly. "
                            + "Expected: [" + normalizedExpected.toPlainString()
                            + "], Actual: [" + normalizedActual.toPlainString() + "]"
            );
        }

        System.out.println(
                "Addition Value entered successfully: ["
                        + committedValue + "]"
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
                ".ag-center-cols-container "
                        + "div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='currencyId']"
        );

        // Locate the React Select input inside the target Currency cell.
        By currencyInputLocator = By.cssSelector(
                ".ag-center-cols-container "
                        + "div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='currencyId'] input"
        );

        // Wait until the target Currency cell is visible.
        WebElement currencyCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyCellLocator
                )
        );

        System.out.println(
                "Currency cell found. Current value: ["
                        + currencyCell.getText().trim() + "]"
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
        WebElement currencyInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyInputLocator
                )
        );

        System.out.println("Currency React Select editor found.");

        // Click the input to display the available Currency options.
        currencyInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        currencyInputLocator
                )
        );

        currencyInput.click();

        System.out.println("Currency dropdown opening...");

        // Locate the React Select listbox without using generated IDs.
        By currencyListboxLocator =
                By.cssSelector("div[role='listbox']");

        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        currencyListboxLocator
                )
        );

        System.out.println("Currency dropdown opened.");

        // Locate available options and exclude the placeholder "اختر".
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
                "Number of selectable actual Currency options: "
                        + selectableOptions.size()
        );

        // Select one valid Currency randomly.
        Random random = new Random();

        WebElement randomCurrency = selectableOptions.get(
                random.nextInt(selectableOptions.size())
        );

        String selectedCurrency = randomCurrency.getText().trim();

        System.out.println(
                "Random actual Currency selected: ["
                        + selectedCurrency + "]"
        );

        // Re-locate the selected option by its visible text to reduce
        // stale-element risk if React Select re-renders the dropdown.
        By selectedCurrencyOptionLocator = By.xpath(
                "//div[@role='listbox']"
                        + "//*[@role='option' and normalize-space(.)="
                        + toXPathLiteral(selectedCurrency) + "]"
        );

        // Wait until the selected Currency option is clickable.
        WebElement optionToClick = wait.until(
                ExpectedConditions.elementToBeClickable(
                        selectedCurrencyOptionLocator
                )
        );

        // Click the actual Currency option.
        optionToClick.click();

        System.out.println("Actual Currency option clicked.");

        // Wait for the dropdown to close after selecting the option.
        wait.until(
                ExpectedConditions.invisibilityOfElementLocated(
                        currencyListboxLocator
                )
        );

        // Wait until the selected Currency is committed to the target cell.
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
                "Currency cell value after selection: ["
                        + committedCurrency + "]"
        );

        // Fail clearly if the selected Currency was not committed correctly.
        if (!currencyCommitted
                || !selectedCurrency.equals(committedCurrency)) {

            throw new IllegalStateException(
                    "Currency selection was not committed correctly. "
                            + "Expected: [" + selectedCurrency
                            + "], Actual: [" + committedCurrency + "]"
            );
        }

        System.out.println(
                "Currency selected and verified successfully: ["
                        + committedCurrency + "]"
        );

        System.out.println("========================================");
    }

    /**
     * Enters and commits the Note in the Discounts & Additions AG Grid.
     *
     * Uses the stable AG Grid column identifier and the stored row index.
     * Attempts to activate the cell editor through double-click, then F2.
     * Prints the cell HTML if the editor cannot be activated.
     *
     * Commits the note by switching tabs and verifies the saved text.
     *
     * @param note the text to enter into the Note cell
     */
    public void enterDiscountsAdditionsNote(String note) {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        System.out.println("========================================");
        System.out.println("Entering Discounts & Additions Note.");
        System.out.println("Requested note: [" + note + "]");
        System.out.println("Target row index: " + newDiscountAdditionRowIndex);

        // Locate the Note cell using its stable column identifier
        // and the stored row index of the new Discounts & Additions row.
        By noteCellLocator = By.cssSelector(
                ".ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='note']"
        );

        // Locate the input or textarea editor inside the Note cell.
        // No generated input IDs or data-testid attributes are used.
        By noteEditorLocator = By.cssSelector(
                ".ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='note'] input, "
                        + ".ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='note'] textarea"
        );

        // Wait for the target Note cell to become visible.
        WebElement noteCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        noteCellLocator
                )
        );

        // Scroll the Note cell into view before activating editing.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                noteCell
        );

        // Re-locate the cell after scrolling in case AG Grid re-renders it.
        noteCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        noteCellLocator
                )
        );

        // First attempt: activate the editor using a double-click.
        new Actions(driver)
                .doubleClick(noteCell)
                .perform();

        System.out.println("Double-click performed on the Note cell.");

        WebElement noteEditor = null;

        // Wait briefly for an enabled editor to appear after double-clicking.
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
                    "Editor did not appear after double-click. Trying F2."
            );
        }

        // Second attempt: use F2 if double-click did not activate editing.
        if (noteEditor == null) {

            WebElement currentCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            noteCellLocator
                    )
            );

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

                // Print the actual cell HTML to diagnose the editor structure.
                try {
                    WebElement diagnosticCell =
                            driver.findElement(noteCellLocator);

                    System.out.println(
                            "NOTE CELL HTML DIAGNOSTICS:\n"
                                    + diagnosticCell.getAttribute("outerHTML")
                    );

                } catch (NoSuchElementException |
                         StaleElementReferenceException diagnosticException) {
                    System.out.println(
                            "The Note cell could not be found for HTML diagnostics."
                    );
                }

                throw new IllegalStateException(
                        "Could not open the Note editor. Neither double-click "
                                + "nor F2 activated editing. Check the printed "
                                + "cell HTML to confirm the editor structure.",
                        e
                );
            }
        }

        System.out.println("Note editor opened successfully.");

        // Re-locate the active editor before entering the requested note.
        WebElement currentEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        noteEditorLocator
                )
        );

        // Replace the existing text with the requested note.
        currentEditor.click();
        currentEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        currentEditor.sendKeys(note);

        // Verify the text entered into the active editor.
        wait.until(currentDriver -> {

            try {
                List<WebElement> editors =
                        currentDriver.findElements(noteEditorLocator);

                for (WebElement editor : editors) {
                    if (!editor.isDisplayed()) {
                        continue;
                    }

                    String editorValue = editor.getAttribute("value");

                    // Textareas expose their current content through value.
                    if (note.equals(editorValue)) {
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

        // Locate the Payment Scheduling tab to commit the edited cell
        // without pressing Enter, which may trigger unintended grid behavior.
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
                "//button[@role='tab' and "
                        + "contains(normalize-space(.), 'الخصومات والإضافات')]"
        );

        wait.until(
                ExpectedConditions.elementToBeClickable(
                        discountsAdditionsTabLocator
                )
        ).click();

        System.out.println("Returned to the Discounts & Additions tab.");

        // Verify the committed Note after returning to the grid.
        wait.until(currentDriver -> {

            try {
                WebElement committedCell =
                        currentDriver.findElement(noteCellLocator);

                return committedCell.isDisplayed()
                        && note.equals(committedCell.getText().trim());

            } catch (StaleElementReferenceException |
                     NoSuchElementException e) {
                return false;
            }
        });

        System.out.println(
                "Discounts & Additions Note committed successfully: ["
                        + note + "]"
        );

        System.out.println("========================================");
    }

    /**
     * Opens the Payments Scheduling tab on the Foreign Purchase Invoice page.
     *
     * Uses the stable Arabic tab label instead of dynamically generated Radix IDs.
     * Verifies that the tab is selected after clicking it.
     */
    public void openPaymentsSchedulingTab() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Payments Scheduling tab using its stable visible Arabic label.
        By paymentsSchedulingTabLocator = By.xpath(
                "//button[@role='tab' and .//div[normalize-space()='جدولة الدفعات']]"
        );

        // Wait until the tab is clickable before interacting with it.
        WebElement paymentsSchedulingTab = wait.until(
                ExpectedConditions.elementToBeClickable(
                        paymentsSchedulingTabLocator
                )
        );

        // Click the Payments Scheduling tab.
        paymentsSchedulingTab.click();

        // Verify that the selected state changes to true.
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
     * Randomly selects a Payment Terms Template and verifies that the selected
     * template is displayed correctly in the React Select control.
     *
     * Uses the stable paymentTermsTemplatesId input ID and avoids generated
     * React Select IDs and dynamically generated CSS classes.
     */
    public void selectRandomPaymentTermsTemplate() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Payment Terms Template input using its stable HTML ID.
        By paymentTermsTemplateInputLocator =
                By.id("paymentTermsTemplatesId");

        // Locate available React Select options using semantic attributes.
        By paymentTermsOptionsLocator = By.cssSelector(
                "[role='option'], div.select__option"
        );

        // Locate the selected value displayed inside the React Select control.
        By selectedTemplateValueLocator = By.xpath(
                "//*[@id='paymentTermsTemplatesId']"
                        + "/ancestor::div[contains(@class,'select__control')]"
                        + "//div[contains(@class,'select__single-value')]"
        );

        // Wait until the Payment Terms Template input is ready for interaction.
        WebElement paymentTermsTemplateInput = wait.until(
                ExpectedConditions.elementToBeClickable(
                        paymentTermsTemplateInputLocator
                )
        );

        // Open the Payment Terms Template dropdown.
        paymentTermsTemplateInput.click();

        // Wait until at least one visible and enabled option is available.
        wait.until(currentDriver ->
                currentDriver.findElements(paymentTermsOptionsLocator)
                        .stream()
                        .anyMatch(option ->
                                option.isDisplayed()
                                        && option.isEnabled()
                                        && !"true".equals(
                                        option.getAttribute("aria-disabled")
                                )
                        )
        );

        // Collect all visible and enabled options from the opened dropdown.
        List<WebElement> availableOptions = driver.findElements(
                        paymentTermsOptionsLocator
                ).stream()
                .filter(WebElement::isDisplayed)
                .filter(WebElement::isEnabled)
                .filter(option ->
                        !"true".equals(
                                option.getAttribute("aria-disabled")
                        )
                )
                .filter(option -> !option.getText().trim().isEmpty())
                .toList();

        // Ensure that the dropdown contains at least one selectable option.
        if (availableOptions.isEmpty()) {
            throw new NoSuchElementException(
                    "No selectable Payment Terms Templates were found."
            );
        }

        // Select a random option from the available templates.
        int randomIndex = ThreadLocalRandom.current().nextInt(
                availableOptions.size()
        );

        WebElement selectedOption = availableOptions.get(randomIndex);

        // Store the expected template text before clicking the option.
        String expectedTemplate = selectedOption.getText().trim();

        // Click the randomly selected Payment Terms Template.
        selectedOption.click();

        // Verify the displayed selected value instead of relying only on input.value.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        selectedTemplateValueLocator
                )
        );

        // Read the actual template text displayed by React Select.
        String displayedTemplate = driver.findElement(
                selectedTemplateValueLocator
        ).getText().trim();

        // Fail only if the displayed template differs from the selected option.
        if (!displayedTemplate.equals(expectedTemplate)) {
            throw new AssertionError(
                    "Payment Terms Template verification failed. Expected: ["
                            + expectedTemplate
                            + "], Actual: ["
                            + displayedTemplate
                            + "]"
            );
        }

        // Log successful selection and verification.
        System.out.println(
                "Payment Terms Template selected and verified successfully: "
                        + displayedTemplate
        );
    }

    /**
     * Copies the displayed total invoice amount, including tax, into the
     * Foreign Purchase Invoice Amount field and verifies it numerically.
     *
     * Uses stable locators and BigDecimal to avoid false failures caused
     * by thousands separators or different numeric display formats.
     */
    public void copyDisplayedAmountToInvoiceAmount() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the amount displayed beside the total invoice label.
        By sourceAmountLocator = By.xpath(
                "//div[contains(@class,'justify-between')]"
                        + "[.//span[normalize-space(.)="
                        + "'اجمالي قيمة الفاتورة شاملة الضريبة']]"
                        + "/span[normalize-space(.) != "
                        + "'اجمالي قيمة الفاتورة شاملة الضريبة']"
        );

        // Locate the Foreign Purchase Invoice Amount input using its stable test ID.
        By invoiceAmountLocator = By.cssSelector(
                "input[data-testid='invoice-foriegn-amountInvoice']"
        );

        // Wait for the displayed source amount and read its text.
        String amount = wait.until(currentDriver -> {
            List<WebElement> elements =
                    currentDriver.findElements(sourceAmountLocator);

            for (WebElement element : elements) {
                if (element.isDisplayed()) {
                    String value = element.getText().trim();

                    if (!value.isEmpty()) {
                        return value;
                    }
                }
            }

            return null;
        });

        System.out.println(
                "Total invoice amount including tax: [" + amount + "]"
        );

        // Convert the displayed amount into a numeric value.
        // Remove thousands separators before constructing BigDecimal.
        BigDecimal expectedAmount = new BigDecimal(
                amount.replace(",", "").trim()
        );

        // Wait until the destination input is visible and enabled.
        wait.until(currentDriver -> {
            List<WebElement> inputs =
                    currentDriver.findElements(invoiceAmountLocator);

            return !inputs.isEmpty()
                    && inputs.get(0).isDisplayed()
                    && inputs.get(0).isEnabled();
        });

        // Re-locate the input to avoid stale elements after React re-renders.
        WebElement invoiceAmountInput =
                driver.findElement(invoiceAmountLocator);

        // Replace the existing value using normal keyboard interactions.
        invoiceAmountInput.click();
        invoiceAmountInput.sendKeys(Keys.CONTROL, "a");
        invoiceAmountInput.sendKeys(amount);

        // Wait until the input contains a non-empty value.
        String actualValue = wait.until(currentDriver -> {
            List<WebElement> inputs =
                    currentDriver.findElements(invoiceAmountLocator);

            if (inputs.isEmpty()) {
                return null;
            }

            String value = inputs.get(0).getAttribute("value");

            return value != null && !value.trim().isEmpty()
                    ? value.trim()
                    : null;
        });

        System.out.println(
                "Invoice Amount field value after entry: ["
                        + actualValue + "]"
        );

        // Normalize the input value and compare the numeric amounts.
        BigDecimal actualAmount = new BigDecimal(
                actualValue.replace(",", "").trim()
        );

        // Fail only when the actual numeric amounts are different.
        if (expectedAmount.compareTo(actualAmount) != 0) {
            throw new AssertionError(
                    "Invoice Amount verification failed. Expected numeric value: ["
                            + expectedAmount.toPlainString()
                            + "], Actual numeric value: ["
                            + actualAmount.toPlainString()
                            + "]"
            );
        }

        System.out.println(
                "Invoice Amount copied and verified successfully. "
                        + "Source: [" + amount + "], Input: ["
                        + actualValue + "]"
        );
    }

    /**
     * Enters and verifies the Notes (البيان) field in the Foreign Purchase Invoice.
     * Uses the stable data-testid attribute to locate the field without dynamic IDs.
     *
     * @param notes the text to enter into the Notes field
     */
    public void enterInvoiceNotes(String notes) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Notes field using its stable data-testid attribute.
        By notesLocator = By.cssSelector(
                "input[data-testid='invoice-foriegn-notes']"
        );

        // Wait until the Notes field is visible and enabled.
        WebElement notesField = wait.until(d -> {
            WebElement element = d.findElement(notesLocator);
            return element.isDisplayed() && element.isEnabled()
                    ? element
                    : null;
        });

        // Clear any existing text and enter the requested notes.
        notesField.clear();
        notesField.sendKeys(notes);

        // Verify that the entered notes match the expected value.
        String actualNotes = notesField.getAttribute("value");

        if (!notes.equals(actualNotes)) {
            throw new AssertionError(
                    "Notes field verification failed. Expected: ["
                            + notes + "], Actual: [" + actualNotes + "]"
            );
        }

        System.out.println(
                "Invoice Notes entered and verified successfully: " + actualNotes
        );
    }

    /**
     * Enters a practically unique vendor invoice number into the
     * Foreign Purchase Invoice form.
     *
     * Uses a stable data-testid locator and scrolls the field into view
     * before interacting with it to reduce click interception.
     */
    public void enterUniqueRandomVendorInvoiceNumber() {

        // Locate the Vendor Invoice Number field using its stable test ID.
        By vendorInvoiceCodeLocator = By.cssSelector(
                "input[data-testid='invoice-foriegn-vendorInvoiceCode']"
        );

        // Generate a practically unique number for this test execution.
        String vendorInvoiceNumber =
                "VIN" + System.currentTimeMillis()
                        + String.format(
                        "%04d",
                        new java.security.SecureRandom().nextInt(10000)
                );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait until the field is visible and enabled.
        WebElement vendorInvoiceCodeInput = wait.until(d -> {
            WebElement element = d.findElement(vendorInvoiceCodeLocator);

            return element.isDisplayed() && element.isEnabled()
                    ? element
                    : null;
        });

        // Scroll the field to the center before attempting interaction.
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block: 'center', inline: 'nearest'});",
                vendorInvoiceCodeInput
        );

        // Re-locate the field after scrolling to avoid using a stale element.
        vendorInvoiceCodeInput = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        vendorInvoiceCodeLocator
                )
        );

        // Set focus directly to the input to avoid a potentially intercepted click.
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "arguments[0].focus();",
                vendorInvoiceCodeInput
        );

        // Replace the current value using keyboard input.
        vendorInvoiceCodeInput.sendKeys(Keys.CONTROL, "a");
        vendorInvoiceCodeInput.sendKeys(vendorInvoiceNumber);

        // Wait until the field contains the generated invoice number.
        String actualValue = wait.until(d -> {
            List<WebElement> inputs =
                    d.findElements(vendorInvoiceCodeLocator);

            if (inputs.isEmpty()) {
                return null;
            }

            WebElement input = inputs.get(0);
            String value = input.getAttribute("value");

            return vendorInvoiceNumber.equals(value)
                    ? value
                    : null;
        });

        System.out.println(
                "Vendor invoice number entered and verified successfully: ["
                        + actualValue + "]"
        );
    }

    /**
     * Clicks the Save button to save the Foreign Purchase Invoice.
     *
     * Uses the stable data-testid attribute instead of dynamic or generated selectors.
     * Scrolls the button into view, waits until it is clickable, and then clicks it.
     * Re-locates the button after scrolling to avoid using a stale WebElement.
     */
    public void clickSaveForeignPurchaseInvoice() {

        // Locate the Save button using its stable data-testid attribute.
        By saveButtonLocator = By.cssSelector(
                "button[data-testid='invoice-foriegn-save']"
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
                "Save button clicked successfully for the Foreign Purchase Invoice."
        );
    }

    /**
     * Confirms saving the Foreign Purchase Invoice by clicking the "Yes" button
     * in the confirmation dialog.
     *
     * Uses stable HTML attributes instead of the dynamic data-testid value.
     * Waits for the button to become clickable, scrolls it into view,
     * and re-locates it before clicking to reduce stale element issues.
     */
    public void confirmSaveForeignPurchaseInvoice() {

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

        // Re-locate the button after scrolling to avoid using a stale WebElement.
        confirmSaveButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        confirmSaveButtonLocator
                )
        );

        // Click "Yes" to confirm saving the Foreign Purchase Invoice.
        confirmSaveButton.click();

        System.out.println(
                "Clicked 'Yes' to confirm saving the Foreign Purchase Invoice."
        );
    }

}
