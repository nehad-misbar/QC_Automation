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
import org.openqa.selenium.JavascriptExecutor;


import java.time.Duration;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class PurchaseInvoiceReturnPage extends BasePage {

    public PurchaseInvoiceReturnPage(WebDriver driver) {
        super(driver);
    }

    /**
     * Stores the data-testid of the Item cell in the newly added
     * Purchase Return Invoice details row.
     */
    private String newItemTestId;

    /**
     * Stores the WebElement representing the newly added row
     * in the Purchase Return Invoice details AG Grid.
     */
    private WebElement newRow;

    /**
     * Stores the AG Grid row index of the newly added
     * Purchase Return Invoice details row.
     */
    private int newRowIndex = -1;

    /**
     * Stores the row index of the newly created row in the
     * Purchase Return Invoice Discounts & Additions AG Grid.
     *
     * This value is intentionally separate from newRowIndex because
     * the Purchase Return Invoice details grid and the Discounts
     * & Additions grid maintain independent AG Grid row indexes.
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
     * Clicks the New button on the Purchase Return Invoices screen
     * to open the new purchase return invoice form.
     *
     * Identifies the button using the stable data-testid prefix
     * and its visible Arabic label to avoid clicking another button.
     */
    public void clickNew() {

        // Locate the New button using its stable test ID prefix
        // and its exact visible Arabic label.
        By newButtonLocator = By.xpath(
                "//button[starts-with(@data-testid,'invoice-return-')" +
                        " and .//span[normalize-space()='جديد']]"
        );

        // Wait until the New button becomes clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement newButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        newButtonLocator
                )
        );

        // Click the New button to open the purchase return invoice form.
        newButton.click();

        // Log the successful click action.
        System.out.println(
                "Clicked the New button on the Purchase Return Invoices screen."
        );
    }

    /**
     * Converts a string into a valid XPath 1.0 string literal
     * for locating elements on the Purchase Return Invoice page.
     *
     * Supports values containing single quotes, double quotes, or both,
     * ensuring that dynamically selected option text can be safely
     * used in XPath expressions without causing syntax errors.
     *
     * @param value the text to represent as an XPath string literal
     *              when locating elements on the Purchase Return Invoice page
     * @return a valid XPath 1.0 string literal representing the supplied text
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
     * Selects a random Operation Type from the Purchase Return Invoice form.
     *
     * Uses stable locators and avoids React-generated IDs.
     * Retries if React Select re-renders the dropdown or its options.
     */
    public void selectRandomOperationType() {

        // Stable locator for the Purchase Return Invoice Operation Type dropdown.
        By operationTypeDropdown = By.cssSelector(
                "[data-testid='invoice-return-operationTypeCategoryId']"
        );

        // Locator for React Select options without generated IDs.
        By operationTypeOptions = By.cssSelector(
                "div.select__option"
        );

        // Initialize an explicit wait for dropdown interactions.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Random generator used to choose an option.
        Random random = new Random();

        // Maximum number of attempts if the dropdown re-renders.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Purchase Return Invoice Operation Type - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Re-locate and open the Operation Type dropdown.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                operationTypeDropdown
                        )
                );

                dropdown.click();

                System.out.println("Operation Type dropdown opened.");

                // Collect the text of all currently visible, non-empty options.
                List<String> operationTypes = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(operationTypeOptions);

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
                        // Retry the wait if React re-renders the options.
                        return null;
                    }
                });

                // Choose a random option from the available operation types.
                String selectedOperationType =
                        operationTypes.get(
                                random.nextInt(operationTypes.size())
                        );

                System.out.println(
                        "Number of available operation types: "
                                + operationTypes.size()
                );

                System.out.println(
                        "Randomly selected operation type: "
                                + selectedOperationType
                );

                // Locate the selected option by its exact visible text.
                By selectedOperationTypeOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedOperationType) +
                                "]"
                );

                // Wait for the selected option to become clickable, then click it.
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
                        "Operation Type dropdown re-rendered. Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Operation Type dropdown/options timed out. Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Fail explicitly if all attempts were unsuccessful.
        throw new IllegalStateException(
                "Unable to select an Operation Type after "
                        + maxAttempts
                        + " attempts because the dropdown kept re-rendering "
                        + "or its options could not be located."
        );
    }

    /**
     * Selects a random Vendor from the Purchase Return Invoice form.
     *
     * Uses the stable data-testid attribute to locate the Vendor dropdown
     * and avoids dynamically generated React IDs.
     * Collects visible options, selects one randomly by its exact text,
     * and retries if React re-renders the dropdown or a timeout occurs.
     */
    public void selectRandomVendor() {

        // Stable locator for the Vendor dropdown on the Purchase Return Invoice form.
        By vendorDropdown = By.cssSelector(
                "[data-testid='invoice-return-vendorId']"
        );

        // Locate visible React Select options without using generated IDs.
        By vendorOptions = By.cssSelector(
                "div.select__option"
        );

        // Initialize an explicit wait for dropdown interactions.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Random generator used to select a vendor.
        Random random = new Random();

        // Maximum number of attempts when the dropdown re-renders or times out.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt + " of " + maxAttempts
                                + ": Selecting a random vendor for the Purchase Return Invoice."
                );

                // Re-locate and open the Vendor dropdown.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                vendorDropdown
                        )
                );

                dropdown.click();

                System.out.println("Vendor dropdown opened.");

                // Collect the text of all currently visible, non-empty vendor options.
                // Retry the wait if React re-renders the options.
                List<String> availableVendors = wait.until(currentDriver -> {

                    try {

                        List<WebElement> visibleOptions =
                                currentDriver.findElements(vendorOptions);

                        List<String> vendorNames = new ArrayList<>();

                        for (WebElement option : visibleOptions) {

                            if (option.isDisplayed()) {

                                String optionText = option.getText().trim();

                                if (!optionText.isEmpty()) {
                                    vendorNames.add(optionText);
                                }
                            }
                        }

                        return vendorNames.isEmpty() ? null : vendorNames;

                    } catch (StaleElementReferenceException e) {

                        // Retry if React replaces an option during collection.
                        return null;
                    }
                });

                // Select one vendor randomly from the available options.
                String selectedVendor = availableVendors.get(
                        random.nextInt(availableVendors.size())
                );

                System.out.println(
                        "Number of available vendors: " + availableVendors.size()
                );

                System.out.println(
                        "Randomly selected vendor: " + selectedVendor
                );

                // Locate the selected vendor by its exact text using a safe XPath literal.
                By selectedVendorOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedVendor)
                                + "]"
                );

                // Wait until the selected vendor option is clickable, then click it.
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
                        "Attempt " + attempt
                                + " failed while selecting a vendor: "
                                + e.getMessage()
                );

                // Stop and report the failure if all attempts have been exhausted.
                if (attempt == maxAttempts) {

                    throw new IllegalStateException(
                            "Failed to select a random vendor on the "
                                    + "Purchase Return Invoice form after "
                                    + maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }


    /**
     * Selects a random currency from the Local Purchase Return form.
     *
     * Uses stable locators based on data-testid and React Select option classes,
     * avoiding dynamically generated React IDs and unstable CSS class suffixes.
     *
     * Collects visible currency options, randomly selects one by its exact text,
     * and retries if React re-renders the dropdown or a timeout occurs.
     */
    public void selectRandomCurrency() {

        // Stable locator for the Currency dropdown on the Local Purchase Return form.
        By currencyDropdown = By.cssSelector(
                "[data-testid='invoice-return-currencyId']"
        );

        // Stable locator for the visible React Select currency options.
        By currencyOptions = By.cssSelector(
                "div.select__option"
        );

        // Waits for the dropdown and its options to become available.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Generates a random index for selecting one of the available currencies.
        Random random = new Random();

        // Maximum number of attempts to handle React re-rendering and timeouts.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt
                                + " of " + maxAttempts
                                + ": Selecting a random currency..."
                );

                // Re-locates the stable Currency dropdown before each attempt.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                currencyDropdown
                        )
                );

                // Opens the currency dropdown to display the available options.
                dropdown.click();

                System.out.println("Currency dropdown opened.");

                // Collects the text of visible currency options.
                // Returns null while waiting if options are unavailable or stale.
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

                // Selects a random currency from the currently visible options.
                String selectedCurrency = availableCurrencies.get(
                        random.nextInt(availableCurrencies.size())
                );

                System.out.println(
                        "Available currencies: " + availableCurrencies.size()
                );

                System.out.println(
                        "Randomly selected currency: " + selectedCurrency
                );

                // Builds an exact-text XPath without relying on generated React IDs.
                By selectedCurrencyOption = By.xpath(
                        "//div[contains(@class,'select__option')]"
                                + "[normalize-space(.)="
                                + toXPathLiteral(selectedCurrency)
                                + "]"
                );

                // Re-locates the selected option and waits until it is clickable.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedCurrencyOption
                        )
                );

                // Selects the chosen currency from the dropdown.
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

                // Fails explicitly if all attempts have been exhausted.
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
     * Selects a random available branch from the Branch React Select dropdown.
     *
     * Uses stable data-testid and select__control locators instead of
     * generated React IDs or an input that may be hidden after vendor changes.
     * Logs diagnostic information when the Branch field cannot be interacted with.
     *
     * @throws IllegalStateException if branch selection fails after all attempts.
     */
    public void selectRandomBranch() {

        // Stable locator for the Branch React Select container.
        By branchDropdown =
                By.cssSelector("[data-testid='invoice-return-branchId']");

        // Locates the visible React Select control used to open the dropdown.
        By branchControl =
                By.cssSelector(
                        "[data-testid='invoice-return-branchId'] .select__control"
                );

        // Locates the Branch input for diagnostic purposes only.
        By branchInput =
                By.cssSelector(
                        "[data-testid='invoice-return-branchId'] input[role='combobox']"
                );

        // Locates the currently selected Branch value.
        By selectedBranchValue =
                By.cssSelector(
                        "[data-testid='invoice-return-branchId'] div.select__single-value"
                );

        // Locates React Select options displayed in the open dropdown.
        By branchOptions =
                By.cssSelector("div.select__option");

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        Random random = new Random();

        String currentStage = "initializing branch selection";

        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {
                System.out.println(
                        "Branch selection attempt "
                                + attempt + " of " + maxAttempts
                );

                // Diagnose the Branch container, control, and input.
                currentStage = "running the Branch field diagnostic";

                List<WebElement> containers =
                        driver.findElements(branchDropdown);

                List<WebElement> controls =
                        driver.findElements(branchControl);

                List<WebElement> inputs =
                        driver.findElements(branchInput);

                System.out.println(
                        "Branch diagnostic: containers found = "
                                + containers.size()
                );

                System.out.println(
                        "Branch diagnostic: controls found = "
                                + controls.size()
                );

                System.out.println(
                        "Branch diagnostic: inputs found = "
                                + inputs.size()
                );

                if (!containers.isEmpty()) {
                    System.out.println(
                            "Branch container displayed = "
                                    + containers.get(0).isDisplayed()
                    );
                }

                if (!controls.isEmpty()) {
                    System.out.println(
                            "Branch control displayed = "
                                    + controls.get(0).isDisplayed()
                    );

                    System.out.println(
                            "Branch control enabled = "
                                    + controls.get(0).isEnabled()
                    );
                }

                if (!inputs.isEmpty()) {
                    System.out.println(
                            "Branch input displayed = "
                                    + inputs.get(0).isDisplayed()
                    );

                    System.out.println(
                            "Branch input enabled = "
                                    + inputs.get(0).isEnabled()
                    );
                }

                // Retry if the Branch container or visible control is absent.
                if (containers.isEmpty() || controls.isEmpty()) {
                    System.out.println(
                            "Branch container or control is absent from the DOM."
                    );
                    continue;
                }

                // Wait for the visible React Select control to become clickable.
                currentStage = "waiting for the Branch control to become clickable";

                WebElement control = wait.until(
                        ExpectedConditions.elementToBeClickable(branchControl)
                );

                // Scroll the Branch control into view before clicking it.
                currentStage = "scrolling the Branch control into view";

                ((org.openqa.selenium.JavascriptExecutor) driver)
                        .executeScript(
                                "arguments[0].scrollIntoView({block: 'center'});",
                                control
                        );

                // Re-locate and click the visible control to open the dropdown.
                currentStage = "opening the Branch dropdown";

                wait.until(
                        ExpectedConditions.elementToBeClickable(branchControl)
                ).click();

                // Wait until at least one Branch option is visible.
                currentStage = "waiting for visible Branch options";

                wait.until(driver -> {
                    List<WebElement> options =
                            driver.findElements(branchOptions);

                    for (WebElement option : options) {
                        if (option.isDisplayed()) {
                            return true;
                        }
                    }

                    return false;
                });

                // Collect only visible Branch options.
                List<WebElement> visibleOptions = new ArrayList<>();

                for (WebElement option : driver.findElements(branchOptions)) {
                    if (option.isDisplayed()) {
                        visibleOptions.add(option);
                    }
                }

                if (visibleOptions.isEmpty()) {
                    System.out.println(
                            "Branch selection: no visible options were found."
                    );
                    continue;
                }

                // Choose one available Branch option at random.
                currentStage = "selecting a random Branch option";

                WebElement chosenBranch =
                        visibleOptions.get(random.nextInt(visibleOptions.size()));

                String chosenBranchName = chosenBranch.getText().trim();

                System.out.println(
                        "Randomly selected Branch: " + chosenBranchName
                );

                chosenBranch.click();

                // Verify that the selected Branch matches the chosen option.
                currentStage = "verifying the selected Branch";

                wait.until(driver -> {
                    List<WebElement> selectedValues =
                            driver.findElements(selectedBranchValue);

                    return !selectedValues.isEmpty()
                            && selectedValues.get(0).isDisplayed()
                            && selectedValues.get(0).getText()
                            .trim()
                            .equals(chosenBranchName);
                });

                System.out.println(
                        "Branch selected successfully: " + chosenBranchName
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Branch selection encountered a stale element at stage: "
                                + currentStage
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Branch selection timed out at stage: "
                                + currentStage
                );

                // Inspect the Branch field after a timeout.
                List<WebElement> containersAfterFailure =
                        driver.findElements(branchDropdown);

                List<WebElement> controlsAfterFailure =
                        driver.findElements(branchControl);

                List<WebElement> inputsAfterFailure =
                        driver.findElements(branchInput);

                System.out.println(
                        "Post-failure diagnostic: containers found = "
                                + containersAfterFailure.size()
                );

                System.out.println(
                        "Post-failure diagnostic: controls found = "
                                + controlsAfterFailure.size()
                );

                System.out.println(
                        "Post-failure diagnostic: inputs found = "
                                + inputsAfterFailure.size()
                );

                if (!containersAfterFailure.isEmpty()) {
                    System.out.println(
                            "Post-failure container displayed = "
                                    + containersAfterFailure.get(0).isDisplayed()
                    );
                }

                if (!controlsAfterFailure.isEmpty()) {
                    System.out.println(
                            "Post-failure control displayed = "
                                    + controlsAfterFailure.get(0).isDisplayed()
                    );

                    System.out.println(
                            "Post-failure control enabled = "
                                    + controlsAfterFailure.get(0).isEnabled()
                    );
                }

                if (!inputsAfterFailure.isEmpty()) {
                    System.out.println(
                            "Post-failure input displayed = "
                                    + inputsAfterFailure.get(0).isDisplayed()
                    );

                    System.out.println(
                            "Post-failure input enabled = "
                                    + inputsAfterFailure.get(0).isEnabled()
                    );
                }

            } catch (Exception e) {

                System.out.println(
                        "Branch selection failed at stage: " + currentStage
                );

                System.out.println(
                        "Failure details: "
                                + e.getClass().getSimpleName()
                                + " - " + e.getMessage()
                );
            }

            // Pause briefly before retrying the Branch selection.
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();

                throw new IllegalStateException(
                        "Branch selection was interrupted.", e
                );
            }
        }

        // Report failure explicitly when all attempts have been exhausted.
        throw new IllegalStateException(
                "Failed to select a random branch after "
                        + maxAttempts + " attempts."
        );
    }



    /**
     * Randomly selects an available warehouse from the Local Purchase Return form.
     *
     * Uses stable data-testid and React Select option locators,
     * avoiding dynamically generated React IDs and unstable CSS class suffixes.
     *
     * Returns false if no warehouse options are available for the selected branch,
     * or if a Selenium timeout or stale-element error occurs.
     */
    public boolean selectRandomWarehouse() {

        // Stable locator for the Warehouse dropdown on the Local Purchase Return form.
        By warehouseDropdown = By.cssSelector(
                "[data-testid='invoice-return-warehouseId']"
        );

        // Stable locator for React Select warehouse options.
        By warehouseOptions = By.cssSelector(
                "div.select__option"
        );

        // Waits for the Warehouse dropdown to become clickable.
        WebDriverWait dropdownWait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Uses a short wait because the selected branch may have no available warehouses.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        // Generates a random index for selecting an available warehouse.
        Random random = new Random();

        try {

            System.out.println("Opening the Warehouse dropdown...");

            // Re-locates the stable Warehouse dropdown and waits until it is clickable.
            WebElement dropdown = dropdownWait.until(
                    ExpectedConditions.elementToBeClickable(warehouseDropdown)
            );

            // Opens the warehouse list for the currently selected branch.
            dropdown.click();

            // Collects visible, non-empty warehouse options while handling React re-rendering.
            List<String> availableWarehouses = optionsWait.until(currentDriver -> {

                try {

                    List<String> warehouseNames = new ArrayList<>();

                    for (WebElement option :
                            currentDriver.findElements(warehouseOptions)) {

                        if (option.isDisplayed()) {

                            String warehouseName = option.getText().trim();

                            if (!warehouseName.isEmpty()) {
                                warehouseNames.add(warehouseName);
                            }
                        }
                    }

                    return warehouseNames.isEmpty()
                            ? null
                            : warehouseNames;

                } catch (StaleElementReferenceException e) {

                    return null;
                }
            });

            // Returns false when no warehouses are available for the selected branch.
            if (availableWarehouses == null || availableWarehouses.isEmpty()) {

                System.out.println(
                        "No warehouse options are available for the selected branch."
                );

                return false;
            }

            // Randomly selects one warehouse from the available options.
            String selectedWarehouse = availableWarehouses.get(
                    random.nextInt(availableWarehouses.size())
            );

            System.out.println(
                    "Available warehouses: " + availableWarehouses.size()
            );

            System.out.println(
                    "Randomly selected warehouse: " + selectedWarehouse
            );

            // Builds an exact-text XPath without relying on generated React IDs.
            By selectedWarehouseOption = By.xpath(
                    "//div[contains(@class,'select__option')]"
                            + "[normalize-space(.)="
                            + toXPathLiteral(selectedWarehouse)
                            + "]"
            );

            // Re-locates the selected warehouse and waits until it is clickable.
            WebElement optionToClick = optionsWait.until(
                    ExpectedConditions.elementToBeClickable(
                            selectedWarehouseOption
                    )
            );

            // Selects the chosen warehouse from the dropdown.
            optionToClick.click();

            System.out.println(
                    "Warehouse selected successfully: " + selectedWarehouse
            );

            return true;

        } catch (TimeoutException | StaleElementReferenceException e) {

            // Reports the failure and returns false if the list cannot be selected.
            System.out.println(
                    "Could not select a warehouse: " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Randomly selects an available Cost Center from the Purchase Return Invoice.
     *
     * Uses stable data-testid attributes and skips selection when the field
     * is disabled. Returns true when a Cost Center is selected successfully.
     *
     * @return true if selection succeeds; otherwise false.
     */
    public boolean selectRandomCostCenter() {

        // Stable locator for the Cost Center dropdown container.
        By costCenterDropdown =
                By.cssSelector("[data-testid='invoice-return-costCenterId']");

        // Stable locator for the visible React Select control.
        By costCenterControl =
                By.cssSelector(
                        "[data-testid='invoice-return-costCenterId'] .select__control"
                );

        // Stable locator for the Cost Center input used to check disabled state.
        By costCenterInput =
                By.cssSelector(
                        "[data-testid='invoice-return-costCenterId'] input[role='combobox']"
                );

        // Locates React Select options without using generated IDs.
        By costCenterOptions =
                By.cssSelector("div.select__option");

        // Stable locator for verifying the selected Cost Center.
        By selectedCostCenterValue =
                By.cssSelector(
                        "[data-testid='invoice-return-costCenterId'] div.select__single-value"
                );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        try {

            // Wait until the Cost Center field exists in the page.
            WebElement dropdownContainer = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            costCenterDropdown
                    )
            );

            // Locate the input within the Cost Center field.
            WebElement dropdownInput =
                    dropdownContainer.findElement(
                            By.cssSelector("input[role='combobox']")
                    );

            // Skip selection if the Cost Center field is disabled.
            if (!dropdownInput.isEnabled()
                    || "true".equals(
                    dropdownInput.getAttribute("aria-disabled"))
                    || "true".equals(
                    dropdownContainer.getAttribute("aria-disabled"))) {

                System.out.println(
                        "Cost Center is disabled. Skipping random selection."
                );

                return false;
            }

            // Click the visible control because the input itself may be hidden.
            WebElement control = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            costCenterControl
                    )
            );

            control.click();

            // Collect only visible, non-empty options from the opened dropdown.
            List<WebElement> availableOptions = optionsWait.until(
                    currentDriver -> {

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
                    }
            );

            // Skip selection if no available options are displayed.
            if (availableOptions.isEmpty()) {

                System.out.println(
                        "No Cost Center options are available."
                );

                return false;
            }

            // Randomly choose one of the available Cost Center options.
            int randomIndex =
                    new Random().nextInt(availableOptions.size());

            String selectedCostCenter =
                    availableOptions.get(randomIndex).getText().trim();

            System.out.println(
                    "Available Cost Centers: " + availableOptions.size()
            );

            System.out.println(
                    "Randomly selected Cost Center: " + selectedCostCenter
            );

            // Re-locate the selected option by its visible text, not a generated ID.
            By selectedOption = By.xpath(
                    "//div[contains(@class,'select__option')]" +
                            "[normalize-space(.)=" +
                            toXPathLiteral(selectedCostCenter) + "]"
            );

            // Wait until the chosen option is clickable, then select it.
            optionsWait.until(
                    ExpectedConditions.elementToBeClickable(selectedOption)
            ).click();

            // Verify that the selected Cost Center appears in the field.
            wait.until(currentDriver -> {

                List<WebElement> selectedValues =
                        currentDriver.findElements(selectedCostCenterValue);

                return !selectedValues.isEmpty()
                        && selectedValues.get(0).isDisplayed()
                        && selectedValues.get(0).getText()
                        .trim()
                        .equals(selectedCostCenter);
            });

            System.out.println(
                    "Cost Center selected successfully: " + selectedCostCenter
            );

            return true;

        } catch (TimeoutException | StaleElementReferenceException e) {

            System.out.println(
                    "Could not select Cost Center: "
                            + e.getClass().getSimpleName()
                            + " - " + e.getMessage()
            );

            return false;
        }
    }

    /**
     * Randomly selects a Tax Calculation Method from the Purchase Return Invoice.
     *
     * Uses stable data-testid locators and retries up to five times when
     * React Select re-renders or Selenium encounters a timeout or stale element.
     */
    public void selectRandomTaxCalculationMethod() {

        // Stable locator for the Purchase Return Invoice Tax Calculation Method dropdown.
        By taxCalculationMethodDropdown =
                By.cssSelector(
                        "[data-testid='invoice-return-taxCalculationMethode']"
                );

        // Locates React Select options without relying on generated React IDs.
        By taxCalculationMethodOptions =
                By.cssSelector("div.select__option");

        // Stable locator for verifying the selected Tax Calculation Method.
        By selectedTaxCalculationMethodValue =
                By.cssSelector(
                        "[data-testid='invoice-return-taxCalculationMethode'] " +
                                "div.select__single-value"
                );

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

                // Re-locate the dropdown on each attempt to handle React re-rendering.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                taxCalculationMethodDropdown
                        )
                );

                // Open the Tax Calculation Method dropdown.
                dropdown.click();

                System.out.println(
                        "Tax Calculation Method dropdown opened."
                );

                // Collect visible, non-empty option texts from the opened dropdown.
                List<String> taxCalculationMethods = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(
                                        taxCalculationMethodOptions
                                );

                        List<String> texts = new ArrayList<>();

                        for (WebElement element : elements) {

                            if (element.isDisplayed()) {

                                String optionText = element.getText().trim();

                                if (!optionText.isEmpty()
                                        && !texts.contains(optionText)) {

                                    texts.add(optionText);
                                }
                            }
                        }

                        return texts.isEmpty() ? null : texts;

                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                // Retry if the dropdown contains no readable options.
                if (taxCalculationMethods == null
                        || taxCalculationMethods.isEmpty()) {

                    throw new IllegalStateException(
                            "No Tax Calculation Methods found in the dropdown."
                    );
                }

                // Randomly select one of the available Tax Calculation Methods.
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

                // Build a text-based XPath without using dynamic React IDs.
                By selectedTaxCalculationMethodOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedTaxCalculationMethod) +
                                "]"
                );

                // Re-locate and click the randomly selected option.
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedTaxCalculationMethodOption
                        )
                ).click();

                // Verify that the selected value is displayed in the dropdown.
                wait.until(currentDriver -> {

                    List<WebElement> selectedValues =
                            currentDriver.findElements(
                                    selectedTaxCalculationMethodValue
                            );

                    return !selectedValues.isEmpty()
                            && selectedValues.get(0).isDisplayed()
                            && selectedValues.get(0).getText()
                            .trim()
                            .equals(selectedTaxCalculationMethod);
                });

                System.out.println(
                        "Tax Calculation Method selected successfully: "
                                + selectedTaxCalculationMethod
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Tax Calculation Method dropdown was re-rendered. " +
                                "Retrying attempt " + attempt +
                                " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Tax Calculation Method dropdown/options timed out " +
                                "during attempt " + attempt +
                                " of " + maxAttempts
                );
            }
        }

        // Report a clear failure if all selection attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Tax Calculation Method after "
                        + maxAttempts
                        + " attempts because the dropdown kept re-rendering "
                        + "or timing out."
        );
    }

    /**
     * Randomly selects the Price Nature option in the Purchase Return Invoice:
     * "باستثناء الضريبة" or "شاملة الضريبة".
     *
     * Uses stable data-testid attributes, retries up to five times,
     * and verifies that the selected radio button is checked.
     */
    public void selectRandomPriceNature() {

        // Stable locator for the Price Nature radio buttons in the Purchase Return Invoice.
        By priceNatureRadioButtons = By.cssSelector(
                "[data-testid='invoice-return-priceNature'] button[role='radio']"
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

                // Collect stable test IDs and values instead of retaining WebElements.
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

                                // Keep only the two expected, valid Price Nature options.
                                if (testId != null
                                        && value != null
                                        && ("0".equals(value) || "1".equals(value))) {

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

                // Ensure at least one valid radio option is available.
                if (priceOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No available Price Nature radio buttons were found."
                    );
                }

                // Randomly choose between excluding tax and including tax.
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

                // Re-locate the selected radio using its stable data-testid.
                By selectedRadioButton = By.cssSelector(
                        "[data-testid='" + selectedTestId + "'][role='radio']"
                );

                WebElement radioButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedRadioButton
                        )
                );

                // Click the option only when it is not already selected.
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

        // Fail clearly if all selection attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Price Nature after " + maxAttempts
                        + " attempts because the radio buttons kept "
                        + "re-rendering or timing out."
        );
    }
    /**
     * Sets today's Invoice Date directly without opening the calendar.
     *
     * Uses a stable data-testid locator, updates the date in yyyy-MM-dd format,
     * dispatches input and change events, and verifies the resulting value.
     */
    public void enterTodayPurchaseReturnInvoiceDateDirectly() {

        // Stable locator for the Purchase Return Invoice date input.
        By invoiceDateInput = By.cssSelector(
                "[data-testid='invoice-return-invoiceDate'] input"
        );

        // Format today's date in the format expected by the input field.
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

        // Set the date and dispatch events so the application can detect the change.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const valueSetter = Object.getOwnPropertyDescriptor(" +
                        "window.HTMLInputElement.prototype, 'value').set;" +
                        "valueSetter.call(input, arguments[1]);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                todayInvoiceDate
        );

        // Verify that the input reflects today's date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        invoiceDateInput,
                        "value",
                        todayInvoiceDate
                )
        );

        System.out.println(
                "Purchase Return Invoice Date set and verified: "
                        + todayInvoiceDate
        );
    }
    /**
     * Selects today's Invoice Date from the calendar on the Purchase Return Invoice.
     *
     * Uses stable data-testid and aria-label attributes rather than generated IDs.
     * Opens the calendar, selects today's date, and verifies the resulting value.
     */
    public void selectTodayPurchaseReturnInvoiceDateFromCalendar() {

        // Stable locator for the Purchase Return Invoice date input.
        By invoiceDateInput = By.cssSelector(
                "[data-testid='invoice-return-invoiceDate'] input"
        );

        // Stable locator for the calendar button inside the date field.
        By calendarButtonLocator = By.cssSelector(
                "[data-testid='invoice-return-invoiceDate'] " +
                        "button[aria-label='Open date picker']"
        );

        // Wait for the date field and calendar controls.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Determine today's date using the machine's local date.
        LocalDate today = LocalDate.now();

        System.out.println(
                "Selecting today's Purchase Return Invoice Date: " + today
        );

        // Ensure the invoice date input is visible before opening the calendar.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        invoiceDateInput
                )
        );

        // Open the calendar using its stable accessible label.
        WebElement calendarButton = wait.until(
                ExpectedConditions.elementToBeClickable(
                        calendarButtonLocator
                )
        );

        calendarButton.click();

        // Build the expected accessible date label in English.
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

        // Locate today's calendar date by its accessible label.
        // This avoids generated IDs and generated CSS class names.
        By todayLocator = By.cssSelector(
                ".rmdp-day[aria-label='" + dateAriaLabel + "']"
        );

        // Wait until today's date is available and clickable.
        WebElement todayOption = wait.until(
                ExpectedConditions.elementToBeClickable(
                        todayLocator
                )
        );

        // Scroll today's date into view before selecting it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                todayOption
        );

        // Re-locate today's date after scrolling to avoid stale references.
        todayOption = wait.until(
                ExpectedConditions.elementToBeClickable(
                        todayLocator
                )
        );

        // Select today's date using Selenium.
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
                "Today's Purchase Return Invoice Date selected and verified: "
                        + today
        );
    }

    /**
     * Sets the Purchase Return Invoice Due Date to one year from today
     * without opening the calendar.
     *
     * Uses a stable data-testid locator, sets the date in yyyy-MM-dd format,
     * dispatches input and change events, and verifies the resulting value.
     */
    public void enterDueDateOneYearFromNowDirectly() {

        // Stable locator for the Purchase Return Invoice Due Date input.
        By dueDateInput = By.cssSelector(
                "[data-testid='invoice-return-deservedDate'] input"
        );

        // Calculate the due date one year from today.
        String dueDate = LocalDate.now()
                .plusYears(1)
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        // Wait until the Due Date input becomes visible.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        dueDateInput
                )
        );

        // Set the date using the native input value setter and notify the application.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const setter = Object.getOwnPropertyDescriptor(" +
                        "window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, arguments[1]);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                dateField,
                dueDate
        );

        // Verify that the Due Date input contains the expected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        dueDateInput,
                        "value",
                        dueDate
                )
        );

        System.out.println(
                "Purchase Return Invoice Due Date set and verified: "
                        + dueDate
        );
    }

    /**
     * Selects the Purchase Return Invoice Due Date one year from today
     * using the RMDP calendar.
     *
     * Opens the calendar, navigates forward month by month, selects the
     * exact target date using its accessible label, and verifies the value.
     */
    public void selectDueDateOneYearFromNowFromCalendar() {

        // Stable locator for the Purchase Return Invoice Due Date input.
        By dueDateInput = By.cssSelector(
                "[data-testid='invoice-return-deservedDate'] input"
        );

        // Stable locator for the calendar button inside the Due Date field.
        By dueDateCalendarButton = By.cssSelector(
                "[data-testid='invoice-return-deservedDate'] " +
                        "button[aria-label='Open date picker']"
        );

        // Locates the visible calendar wrapper without generated IDs.
        By calendarWrapperLocator = By.cssSelector(
                ".rmdp-wrapper"
        );

        // Locates the Next Month arrow using the calendar's semantic CSS class.
        By nextMonthButtonLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-arrow-container.rmdp-right"
        );

        // Locates the calendar's month/year heading for navigation verification.
        By calendarHeaderLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-header-values"
        );

        // Calculate the target due date one year from today.
        LocalDate targetDueDate = LocalDate.now().plusYears(1);
        String expectedDueDate = targetDueDate.toString();

        // Build the expected accessible label for the target date.
        String dayOfWeek = targetDueDate.getDayOfWeek()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        String month = targetDueDate.getMonth()
                .getDisplayName(TextStyle.FULL, Locale.ENGLISH);

        String targetDateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                dayOfWeek,
                month,
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
                "Selecting Purchase Return Invoice Due Date: "
                        + expectedDueDate
        );

        // Ensure the Due Date input is visible before opening the calendar.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        dueDateInput
                )
        );

        // Open the calendar using its stable accessible button label.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        dueDateCalendarButton
                )
        ).click();

        // Wait until the calendar is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        // Navigate forward until the target date appears, with a 12-month limit.
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
                    return element.isDisplayed() && element.isEnabled();
                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });

            // Stop navigating when the target date is found.
            if (targetDateVisible) {
                break;
            }

            // Do not exceed the configured month-navigation limit.
            if (attempt == maxMonthNavigationAttempts) {
                break;
            }

            // Capture the current header text to verify month navigation.
            String previousHeader = wait.until(currentDriver -> {
                List<WebElement> headers =
                        currentDriver.findElements(calendarHeaderLocator);

                for (WebElement header : headers) {
                    if (header.isDisplayed()) {
                        return header.getText().trim();
                    }
                }

                return null;
            });

            System.out.println(
                    "Navigating to the next calendar month: "
                            + (attempt + 1) + " of "
                            + maxMonthNavigationAttempts
            );

            // Re-locate and click the Next Month arrow.
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            nextMonthButtonLocator
                    )
            ).click();

            // Wait for the calendar header to change after navigation.
            wait.until(currentDriver -> {
                try {
                    List<WebElement> headers =
                            currentDriver.findElements(calendarHeaderLocator);

                    for (WebElement header : headers) {
                        if (header.isDisplayed()
                                && !header.getText().trim()
                                .equals(previousHeader)) {
                            return true;
                        }
                    }

                    return false;

                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });
        }

        // Fail clearly if the target date was not found within the month limit.
        if (!targetDateVisible) {
            throw new NoSuchElementException(
                    "Could not find Due Date " + expectedDueDate
                            + " after navigating "
                            + maxMonthNavigationAttempts
                            + " calendar months."
            );
        }

        // Wait for the target date to be clickable and select it.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        targetDateLocator
                )
        ).click();

        // Verify that the Due Date input contains the expected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        dueDateInput,
                        "value",
                        expectedDueDate
                )
        );

        System.out.println(
                "Purchase Return Invoice Due Date selected and verified: "
                        + expectedDueDate
        );
    }

    /**
     * Randomly selects a Purchase Representative from the Purchase Return Invoice form.
     *
     * Uses stable data-testid and semantic role attributes instead of generated
     * React Select IDs or dynamic CSS classes.
     * Waits for the field's enabled/disabled state to remain stable for two seconds.
     * Skips selection if the field is disabled and selects a random visible,
     * enabled option when available.
     */
    public void selectRandomPurchaseRepresentative() {

        // Stable locator for the Purchase Representative dropdown container.
        By purchaseRepresentativeDropdown = By.cssSelector(
                "[data-testid='invoice-return-purchaseRepresentativeId']"
        );

        // Stable locator for the searchable input inside the dropdown.
        By purchaseRepresentativeInput = By.cssSelector(
                "[data-testid='invoice-return-purchaseRepresentativeId'] " +
                        "input[role='combobox']"
        );

        // Semantic locator for selectable options in the opened React Select menu.
        By purchaseRepresentativeOptions = By.cssSelector(
                "[role='option']"
        );

        // Wait up to ten seconds and poll frequently while the form updates.
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

        // Track the observed enabled/disabled state and when it first appeared.
        final String[] lastState = {null};
        final long[] stateStartTime = {0L};

        // Wait until the field's enabled/disabled state remains stable for two seconds.
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

                // Determine whether the dropdown is disabled using its state and input.
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

                // Reset tracking if React replaces the field during the wait.
                lastState[0] = null;
                stateStartTime[0] = 0L;
                return null;
            }
        });

        // Skip selection if the field remained disabled.
        if ("DISABLED".equals(stableState)) {
            System.out.println(
                    "Purchase Representative field is disabled. " +
                            "Skipping representative selection."
            );
            return;
        }

        // Recheck the field's state immediately before interacting with it.
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

            // Avoid interacting with an element replaced during verification.
            System.out.println(
                    "Purchase Representative field changed during verification. " +
                            "Skipping representative selection."
            );
            return;
        }

        // Open the dropdown using the stable searchable-input locator.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        purchaseRepresentativeInput
                )
        ).click();

        // Wait until at least one visible and enabled option is available.
        wait.until(currentDriver ->
                currentDriver.findElements(purchaseRepresentativeOptions)
                        .stream()
                        .anyMatch(option -> {
                            try {
                                return option.isDisplayed() && option.isEnabled();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        })
        );

        // Collect the currently visible and enabled representative options.
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

        // Fail clearly if the enabled dropdown contains no selectable options.
        if (options.isEmpty()) {
            throw new IllegalStateException(
                    "No purchase representatives found in the dropdown."
            );
        }

        // Choose a random index from the available representatives.
        Random random = new Random();
        int randomIndex = random.nextInt(options.size());

        // Capture the selected representative's name for logging.
        String selectedRepresentative =
                options.get(randomIndex).getText().trim();

        // Click the randomly selected representative.
        options.get(randomIndex).click();

        // Log the available option count and selected representative.
        System.out.println(
                "Number of purchase representatives: " + options.size()
        );

        System.out.println(
                "Selected purchase representative: " + selectedRepresentative
        );
    }

    /**
     * Randomly selects a Bulk Discount Type from the Purchase Return Invoice form.
     *
     * Uses stable data-testid and semantic role attributes instead of generated
     * React Select IDs or dynamic CSS classes.
     * Retries up to five times if the DOM changes or an operation times out.
     * Logs the available discount types and the selected value.
     */
    public void selectRandomBulkDiscountType() {

        // Stable locator for the Bulk Discount Type dropdown container.
        By bulkDiscountTypeDropdown = By.cssSelector(
                "[data-testid='invoice-return-bulkDiscountType']"
        );

        // Stable locator for the searchable input inside the dropdown.
        By bulkDiscountTypeInput = By.cssSelector(
                "[data-testid='invoice-return-bulkDiscountType'] input[role='combobox']"
        );

        // Semantic locator for selectable options in the opened dropdown menu.
        By bulkDiscountTypeOptions = By.cssSelector(
                "[role='option']"
        );

        // Wait up to ten seconds for each dropdown interaction.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Generate random indexes when choosing an available discount type.
        Random random = new Random();

        // Limit retries if React re-renders the dropdown or an operation times out.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Track the current operation to make failure logs more informative.
            String currentStage = "locating the Bulk Discount Type dropdown";

            try {

                // Log the current retry attempt.
                System.out.println(
                        "Selecting random Bulk Discount Type - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Wait until the dropdown container is visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                bulkDiscountTypeDropdown
                        )
                );

                // Update the diagnostic stage before opening the dropdown.
                currentStage = "opening the Bulk Discount Type dropdown";

                // Locate the stable input and click it to open the options menu.
                WebElement input = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                bulkDiscountTypeInput
                        )
                );

                input.click();

                // Update the diagnostic stage before retrieving available options.
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

                // Fail clearly if the dropdown contains no selectable options.
                if (discountTypes.isEmpty()) {
                    throw new IllegalStateException(
                            "The Bulk Discount Type dropdown contains no visible options."
                    );
                }

                // Choose a random discount type from the available option texts.
                String selectedDiscountType = discountTypes.get(
                        random.nextInt(discountTypes.size())
                );

                // Log the number of available discount types.
                System.out.println(
                        "Number of bulk discount types: " + discountTypes.size()
                );

                // Log the selected discount type for test traceability.
                System.out.println(
                        "Selected bulk discount type: " + selectedDiscountType
                );

                // Update the diagnostic stage before locating the selected option.
                currentStage = "locating the selected Bulk Discount Type option";

                // Build an XPath that matches the exact visible option text.
                // toXPathLiteral safely handles text containing quotation marks.
                By selectedOption = By.xpath(
                        "//*[@role='option' and normalize-space(.)="
                                + toXPathLiteral(selectedDiscountType)
                                + "]"
                );

                // Wait until the selected option is clickable.
                WebElement optionToSelect = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOption
                        )
                );

                // Update the diagnostic stage before selecting the option.
                currentStage = "clicking the selected Bulk Discount Type option";

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
                        + " attempts. Verify that the dropdown and option locators "
                        + "are correct and that the menu is available."
        );
    }

    /**
     * Enters the bulk discount amount into the Purchase Return Invoice form.
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
                "input[data-testid='invoice-return-bulkDiscountAmount']"
        );

        // Allows up to ten seconds for the input to become available.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Wait until the discount amount input is visible and enabled.
        WebElement amountField = wait.until(currentDriver -> {

            WebElement field = currentDriver.findElement(
                    bulkDiscountAmountField
            );

            // Return the field only when it is ready to receive a value.
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
                        "window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, value);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));",
                amountField,
                bulkDiscountAmount
        );

        // Wait until the input reflects the requested discount amount.
        wait.until(currentDriver -> {
            try {
                WebElement field = currentDriver.findElement(
                        bulkDiscountAmountField
                );

                return bulkDiscountAmount.equals(
                        field.getAttribute("value")
                );

            } catch (StaleElementReferenceException e) {

                // Continue waiting if React re-renders the input element.
                return false;
            }
        });

        // Log the entered amount for test execution traceability.
        System.out.println(
                "Bulk discount amount entered successfully: "
                        + bulkDiscountAmount
        );
    }


    /**
     * Randomly selects an Item Costs Method from the Purchase Return Invoice.
     *
     * Uses stable data-testid and role attributes, avoiding generated React IDs
     * and dynamic CSS classes. Verifies the selected value from the dropdown
     * container rather than relying on the combobox input value.
     */
    public void selectRandomItemsCostsMethod() {

        // Stable locator for the Item Costs Method dropdown container.
        By itemsCostsMethodDropdown = By.cssSelector(
                "[data-testid='invoice-return-itemsCostsMethode']"
        );

        // Stable locator for the dropdown's combobox input.
        By itemsCostsMethodInput = By.cssSelector(
                "[data-testid='invoice-return-itemsCostsMethode'] input[role='combobox']"
        );

        // Semantic locator for visible options in the open React Select menu.
        By itemsCostsMethodOptions = By.cssSelector(
                "[role='option']"
        );

        // Waits up to ten seconds for each required UI condition.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Randomly chooses one option from the available list.
        Random random = new Random();

        // Waits for the dropdown container to be visible and enabled.
        WebElement dropdown = wait.until(currentDriver -> {
            WebElement element = currentDriver.findElement(
                    itemsCostsMethodDropdown
            );

            return element.isDisplayed() && element.isEnabled()
                    ? element
                    : null;
        });

        // Reads the currently displayed value to avoid reopening an already selected value.
        String currentValue = dropdown.getText().trim();

        // Opens the dropdown only once.
        dropdown.click();

        // Waits for the combobox input to become visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        itemsCostsMethodInput
                )
        );

        // Collects the visible, enabled option texts.
        List<String> availableOptions = wait.until(currentDriver -> {
            List<String> optionTexts = new ArrayList<>();

            for (WebElement option :
                    currentDriver.findElements(itemsCostsMethodOptions)) {
                try {
                    if (option.isDisplayed()
                            && option.isEnabled()
                            && !option.getText().trim().isEmpty()) {

                        optionTexts.add(option.getText().trim());
                    }
                } catch (StaleElementReferenceException ignored) {
                    // Retry collection if React updates the options.
                    return null;
                }
            }

            return optionTexts.isEmpty() ? null : optionTexts;
        });

        // Randomly chooses one of the available methods.
        String selectedMethod = availableOptions.get(
                random.nextInt(availableOptions.size())
        );

        System.out.println(
                "Available Item Costs Methods: " + availableOptions.size()
        );
        System.out.println(
                "Randomly selected Item Costs Method: " + selectedMethod
        );

        // Finds the exact visible option using its text and semantic role.
        // The locator is based on option text, not generated React IDs.
        By selectedOption = By.xpath(
                "//*[@role='option' and normalize-space(.)="
                        + toXPathLiteral(selectedMethod)
                        + "]"
        );

        // Waits for the matching option to be visible.
        WebElement optionToSelect = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        selectedOption
                )
        );

        // Clicks the selected option only once.
        optionToSelect.click();

        // Verifies the selected text is displayed by the dropdown container.
        // Does not reopen the dropdown or repeat the selection after success.
        wait.until(currentDriver -> {
            try {
                WebElement updatedDropdown = currentDriver.findElement(
                        itemsCostsMethodDropdown
                );

                String displayedValue = updatedDropdown.getText().trim();

                return displayedValue.contains(selectedMethod);

            } catch (StaleElementReferenceException ignored) {
                // Wait for the dropdown to finish re-rendering.
                return false;
            }
        });

        // Logs successful selection after the displayed value is verified.
        System.out.println(
                "Item Costs Method selected and verified successfully: "
                        + selectedMethod
        );
    }

    /**
     * Randomly selects an available Tag from the Purchase Return Invoice.
     *
     * Uses stable data-testid and role attributes to locate the dropdown
     * and its options. Retries if React re-renders the DOM, and verifies
     * the selected value before returning to avoid unnecessary reselection.
     */
    public void selectRandomTag() {

        // Stable locator for the Tag dropdown container.
        By tagDropdown = By.cssSelector(
                "[data-testid='invoice-return-tagId']"
        );

        // Stable locator for the dropdown's combobox input.
        By tagDropdownInput = By.cssSelector(
                "[data-testid='invoice-return-tagId'] input[role='combobox']"
        );

        // Semantic locator for selectable dropdown options.
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

        // Limits retries if React changes the dropdown DOM.
        int maxAttempts = 5;

        // Retries only when selection has not been confirmed.
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            // Identifies the current operation for diagnostic logging.
            String currentStage = "locating the Tag dropdown";

            try {

                // Logs the current selection attempt.
                System.out.println(
                        "Selecting random Tag - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Waits until the dropdown container is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(tagDropdown)
                );

                // Opens the dropdown to display available Tag options.
                currentStage = "opening the Tag dropdown";
                dropdown.click();

                // Waits until the combobox input is visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                tagDropdownInput
                        )
                );

                // Collects visible, enabled, non-empty option labels.
                currentStage = "collecting visible Tag options";

                List<String> tags = wait.until(currentDriver -> {

                    List<String> optionTexts = new ArrayList<>();

                    for (WebElement option :
                            currentDriver.findElements(tagOptions)) {

                        try {
                            // Ignores hidden and disabled options.
                            if (!option.isDisplayed() || !option.isEnabled()) {
                                continue;
                            }

                            // Reads the visible label of the option.
                            String optionText = option.getText().trim();

                            // Excludes empty and duplicate labels.
                            if (!optionText.isEmpty()
                                    && !optionTexts.contains(optionText)) {
                                optionTexts.add(optionText);
                            }

                        } catch (StaleElementReferenceException ignored) {
                            // Retries collection if React replaces an option.
                            return null;
                        }
                    }

                    // Continues waiting until at least one option is available.
                    return optionTexts.isEmpty() ? null : optionTexts;
                });

                // Randomly chooses one available Tag.
                String selectedTag = tags.get(
                        random.nextInt(tags.size())
                );

                // Logs the available option count.
                System.out.println(
                        "Number of available tags: " + tags.size()
                );

                // Logs the randomly selected Tag.
                System.out.println(
                        "Selected tag: " + selectedTag
                );

                // Locates the exact option text without generated IDs.
                currentStage = "locating the selected Tag option";

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

                // Clicks the randomly selected option.
                currentStage = "clicking the selected Tag option";
                optionToSelect.click();

                // Verifies that the selected label is displayed in the dropdown.
                currentStage = "verifying the selected Tag";

                wait.until(currentDriver -> {
                    try {
                        WebElement updatedDropdown =
                                currentDriver.findElement(tagDropdown);

                        String displayedText =
                                updatedDropdown.getText().trim();

                        return displayedText.contains(selectedTag);

                    } catch (StaleElementReferenceException ignored) {
                        // Waits for React to finish rendering the selected value.
                        return false;
                    }
                });

                // Logs success only after the selection is verified.
                System.out.println(
                        "Tag selected and verified successfully: "
                                + selectedTag
                );

                // Stops after successful selection.
                return;

            } catch (StaleElementReferenceException e) {

                // Logs a DOM refresh and retries the operation.
                System.out.println(
                        "DOM changed while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                // Logs the stage that timed out before retrying.
                System.out.println(
                        "Timeout while " + currentStage
                                + ". Retrying attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Reports failure if all attempts are exhausted.
        throw new IllegalStateException(
                "Unable to select Tag after " + maxAttempts
                        + " attempts. Last operation: verify the dropdown "
                        + "and option locators."
        );
    }

    /**
     * Clicks the Add Line button in the Purchase Return Invoice items grid
     * and identifies the newly created Item cell.
     *
     * Uses the stable Arabic button label and the Purchase Return Invoice
     * Item test-id prefix instead of generated React IDs or CSS classes.
     *
     * Captures existing Item test IDs and row indexes before clicking,
     * so the newly added row can be distinguished from existing rows.
     *
     * Stores the new Item test ID, row WebElement, and logical row index
     * for subsequent grid-cell operations.
     */
    public void clickAddLine() {

        // Waits for the Add Line button and the newly rendered Item cell.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        /*
         * Locates Item cells belonging to the Purchase Return Invoice.
         * Verify the prefix against the actual Item cell HTML if necessary.
         */
        By itemCellsLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId']" +
                        "[data-testid^='invoice-return-itemId-']"
        );

        /*
         * Locates the Add Line button using its stable visible Arabic label,
         * avoiding the generated data-testid suffix and dynamic CSS classes.
         */
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space(.)='اضافة سطر']"
        );

        // Stores Item cell test IDs that exist before adding a new line.
        Set<String> existingItemTestIds = new HashSet<>();

        // Stores logical AG Grid row indexes that exist before adding a line.
        Set<String> existingRowIndexes = new HashSet<>();

        try {

            /*
             * Resets horizontal scrolling in AG Grid viewports so the Item
             * column can be accessed consistently before capturing existing rows.
             */
            ((JavascriptExecutor) driver).executeScript(
                    "document.querySelectorAll(" +
                            "'.ag-body-horizontal-scroll-viewport'" +
                            ").forEach(function(viewport) {" +
                            "viewport.scrollLeft = 0;" +
                            "});"
            );

            /*
             * Captures the currently rendered Item cells before clicking Add Line,
             * providing a baseline for detecting the newly created row.
             */
            List<WebElement> existingItemCells =
                    driver.findElements(itemCellsLocator);

            for (WebElement cell : existingItemCells) {

                try {

                    // Reads the stable test ID of the existing Item cell.
                    String testId = cell.getAttribute("data-testid");

                    if (testId != null && !testId.isBlank()) {
                        existingItemTestIds.add(testId);
                    }

                    // Locates the AG Grid row containing the current Item cell.
                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::div[@role='row'][1]")
                    );

                    // Captures the row's logical index before adding a line.
                    String rowIndex = row.getAttribute("row-index");

                    if (rowIndex != null && rowIndex.matches("\\d+")) {
                        existingRowIndexes.add(rowIndex);
                    }

                } catch (StaleElementReferenceException ignored) {
                    // Ignores cells replaced while AG Grid is rendering.
                }
            }

            // Logs the state of the Purchase Return Invoice grid before clicking.
            System.out.println(
                    "PURCHASE RETURN INVOICE - ADD LINE DIAGNOSTIC"
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

            // Waits until the Add Line button is clickable.
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

            // Re-locates the button after scrolling to avoid using a stale element.
            addLineButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            addLineButtonLocator
                    )
            );

            // Clicks Add Line exactly once.
            addLineButton.click();

            System.out.println(
                    "Purchase Return Invoice Add Line button clicked."
            );

            /*
             * Waits for an Item cell with a previously unseen test ID and
             * a row index that did not exist before the click.
             */
            WebElement detectedNewItemCell = wait.until(webDriver -> {

                List<WebElement> currentItemCells =
                        webDriver.findElements(itemCellsLocator);

                for (WebElement cell : currentItemCells) {

                    try {

                        // Reads the current candidate Item cell test ID.
                        String currentTestId =
                                cell.getAttribute("data-testid");

                        // Skips missing IDs and cells that existed before the click.
                        if (currentTestId == null
                                || currentTestId.isBlank()
                                || existingItemTestIds.contains(currentTestId)) {
                            continue;
                        }

                        // Locates the row containing the candidate Item cell.
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
                        // Continues polling if AG Grid replaces the candidate cell.
                    }
                }

                // Keeps waiting until the newly added Item cell is detected.
                return null;
            });

            /*
             * Stores the newly created Item cell test ID for reuse by
             * methods that interact with the new invoice line.
             */
            newItemTestId =
                    detectedNewItemCell.getAttribute("data-testid");

            if (newItemTestId == null || newItemTestId.isBlank()) {
                throw new IllegalStateException(
                        "A new Purchase Return Invoice Item cell was detected, " +
                                "but its data-testid is empty."
                );
            }

            // Stores the AG Grid row containing the newly created Item cell.
            newRow = detectedNewItemCell.findElement(
                    By.xpath("./ancestor::div[@role='row'][1]")
            );

            // Reads and validates the logical index of the new row.
            String rowIndex = newRow.getAttribute("row-index");

            if (rowIndex == null || !rowIndex.matches("\\d+")) {
                throw new IllegalStateException(
                        "Could not determine a valid row-index for the new " +
                                "Purchase Return Invoice line. Item test ID: " +
                                newItemTestId + ", row-index: " + rowIndex
                );
            }

            // Stores the row index for subsequent AG Grid cell operations.
            newRowIndex = Integer.parseInt(rowIndex);

            // Logs the resulting grid state and the identifiers of the new row.
            System.out.println(
                    "Item cells AFTER Add Line: " +
                            driver.findElements(itemCellsLocator).size()
            );

            System.out.println(
                    "New Item test ID: " + newItemTestId
            );

            System.out.println(
                    "New Purchase Return Invoice row index: " +
                            newRowIndex
            );

        } catch (TimeoutException e) {

            /*
             * Logs diagnostic information without clicking Add Line again,
             * helping distinguish a failed click from a failed row detection.
             */
            System.err.println(
                    "Timeout while waiting for the new Purchase Return Invoice line."
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
                    "Currently detected Purchase Return Invoice Item cells: " +
                            currentItemCells.size()
            );

            for (WebElement cell : currentItemCells) {

                try {

                    // Locates the row associated with the Item cell for diagnostics.
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
                    // Records that a cell changed while diagnostic details were read.
                    System.err.println(
                            "An Item cell was re-rendered during diagnostics."
                    );
                }
            }

            throw new RuntimeException(
                    "Failed to detect the newly added Item cell in the " +
                            "Purchase Return Invoice items grid. " +
                            "Check the Item cell data-testid prefix and grid structure.",
                    e
            );
        }
    }


    /**
     * Selects a random Item Name from the React Select dropdown
     * inside the newly added Purchase Return Invoice row.
     *
     * Uses the captured newRow and the stable AG Grid column ID.
     * Locates the Item Name cell using the stable column ID and
     * the Purchase Return Invoice data-testid prefix.
     * Attempts to activate the cell editor using a double-click.
     * Locates the React Select input using the original locator.
     * Selects a random visible item from the dropdown.
     */
    public void selectRandomItem() {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Ensure the newly added Purchase Return Invoice row has already been captured.
        if (newRow == null) {
            throw new IllegalStateException(
                    "New Purchase Return Invoice row was not captured. "
                            + "Call clickAddLine() first."
            );
        }

        System.out.println(
                "Using captured Purchase Return Invoice row."
        );

        System.out.println(
                "New row index: " + newRow.getAttribute("row-index")
        );

        // Locate the Item Name cell using the stable AG Grid column ID
        // and the Purchase Return Invoice data-testid prefix.
        By itemNameCellLocator =
                By.cssSelector(
                        "div[role='gridcell'][col-id='itemId']"
                                + "[data-testid^='invoice-return-itemId-']"
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

        // Locate the active React Select input using the original working locator.
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
                            + "for the Purchase Return Invoice Item Name field."
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
                            + "Purchase Return Invoice Item Name dropdown."
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

        // Select the chosen option using JavaScript, preserving the original approach.
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
                "Item selected successfully in the new Purchase Return Invoice row."
        );
    }


    /**
     * Enters the requested quantity into the newly added Purchase Return Invoice row.
     * Uses the captured item test ID to locate the correct quantity cell, commits
     * the value, and verifies the saved quantity numerically.
     *
     * @param quantity the quantity to enter into the new invoice row
     */
    public void enterQuantity(String quantity) {

        // Validate the requested quantity before interacting with the grid.
        if (quantity == null || quantity.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Quantity must not be null or empty."
            );
        }

        // Ensure that the new item row has already been captured.
        if (newItemTestId == null || newItemTestId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "New item test ID is missing. Select the item before entering quantity."
            );
        }

        // Parse the expected quantity to reject invalid numeric input early.
        final BigDecimal expectedValue;

        try {
            expectedValue = new BigDecimal(quantity.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Quantity must be a valid number: " + quantity,
                    e
            );
        }

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        String expectedQuantity = quantity.trim();

        // Derive the quantity test ID from the captured item test ID.
        String quantityTestId = newItemTestId.replace(
                "itemId",
                "quantity"
        );

        // Locate the quantity cell belonging to the newly added invoice row.
        By quantityCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='quantity']"
                        + "[data-testid='" + quantityTestId + "']"
        );

        // Locate the inline input editor while the quantity cell is in edit mode.
        By quantityEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='quantity']"
                        + "[data-testid='" + quantityTestId + "']"
                        + ".ag-cell-inline-editing input[data-ref='eInput']"
        );

        try {
            // Wait until the target quantity cell is ready for interaction.
            WebElement quantityCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            quantityCellLocator
                    )
            );

            // Activate editing on the quantity cell.
            quantityCell.click();
            quantityCell.sendKeys(Keys.ENTER);

            System.out.println(
                    "ENTER sent to the new Quantity cell."
            );

            // Wait until AG Grid displays the inline quantity editor.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            quantityEditorLocator
                    )
            );

            // Locate the current editor; do not reuse it after a possible re-render.
            WebElement quantityEditor = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            quantityEditorLocator
                    )
            );

            // Clear the input using the native setter and notify React of the change.
            js.executeScript(
                    "const input = arguments[0];" +
                            "const setter = Object.getOwnPropertyDescriptor(" +
                            "window.HTMLInputElement.prototype, 'value').set;" +
                            "setter.call(input, '');" +
                            "input.dispatchEvent(new Event('input', {bubbles: true}));" +
                            "input.dispatchEvent(new Event('change', {bubbles: true}));",
                    quantityEditor
            );

            System.out.println("Quantity editor cleared.");

            // Re-locate the editor because the previous update may have re-rendered it.
            quantityEditor = wait.until(
                    ExpectedConditions.presenceOfElementLocated(
                            quantityEditorLocator
                    )
            );

            // Enter the requested quantity and notify React of the updated value.
            js.executeScript(
                    "const input = arguments[0];" +
                            "const value = arguments[1];" +
                            "const setter = Object.getOwnPropertyDescriptor(" +
                            "window.HTMLInputElement.prototype, 'value').set;" +
                            "setter.call(input, value);" +
                            "input.dispatchEvent(new Event('input', {bubbles: true}));" +
                            "input.dispatchEvent(new Event('change', {bubbles: true}));",
                    quantityEditor,
                    expectedQuantity
            );

            // Verify the value in the editor, retrying if AG Grid replaces the element.
            boolean editorValueVerified = wait.until(d -> {
                try {
                    WebElement currentEditor =
                            d.findElement(quantityEditorLocator);

                    String actualValue = currentEditor.getAttribute("value");

                    System.out.println(
                            "Quantity editor current value: [" + actualValue + "]"
                    );

                    if (actualValue == null || actualValue.trim().isEmpty()) {
                        return false;
                    }

                    return new BigDecimal(actualValue.trim())
                            .compareTo(expectedValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;

                } catch (NumberFormatException e) {
                    return false;
                }
            });

            if (!editorValueVerified) {
                throw new TimeoutException(
                        "The requested quantity was not verified in the editor: "
                                + expectedQuantity
                );
            }

            System.out.println(
                    "Requested quantity verified in the editor."
            );

            // Commit the value through the keyboard without reusing a stale WebElement.
            new Actions(driver)
                    .sendKeys(Keys.ENTER)
                    .perform();

            System.out.println(
                    "ENTER sent to commit the quantity."
            );

            // Wait until AG Grid closes the inline editor after committing the value.
            wait.until(d -> {
                try {
                    return d.findElements(quantityEditorLocator).isEmpty();
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println(
                    "Quantity editor closed successfully."
            );

            /*
             * Verify the committed quantity numerically.
             * For example, 17, 17.0, and 17.000 are considered equivalent.
             */
            boolean quantitySaved = wait.until(d -> {
                try {
                    WebElement cell = d.findElement(quantityCellLocator);

                    String cellText = cell.getText().trim();

                    if (cellText.isEmpty()) {
                        return false;
                    }

                    // Remove the common thousands separator before parsing the number.
                    String normalizedCellText = cellText.replace(",", "");

                    BigDecimal actualValue =
                            new BigDecimal(normalizedCellText);

                    System.out.println(
                            "Saved quantity cell value: [" + cellText + "]"
                    );

                    return actualValue.compareTo(expectedValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;

                } catch (NumberFormatException e) {
                    return false;
                }
            });

            if (!quantitySaved) {
                throw new TimeoutException(
                        "Quantity was not saved correctly in the grid. Expected: "
                                + expectedQuantity
                );
            }

            System.out.println(
                    "Quantity saved and verified successfully: "
                            + expectedQuantity
            );

        } catch (TimeoutException e) {
            throw new RuntimeException(
                    "Failed to enter or verify quantity ["
                            + expectedQuantity
                            + "] for row ["
                            + newItemTestId
                            + "].",
                    e
            );
        }
    }


    /**
     * Escapes an item name for safe use as a literal in an XPath expression.
     * Supports item names containing single quotes, double quotes, or both.
     */
    private String xpathLiteral(String value) {

        if (!value.contains("'")) {
            return "'" + value + "'";
        }

        if (!value.contains("\"")) {
            return "\"" + value + "\"";
        }

        String[] parts = value.split("'", -1);

        return "concat('"
                + String.join("', \"'\", '", parts)
                + "')";
    }


    /**
     * Enters the requested Cost value into the newly added Purchase Return Invoice row.
     *
     * Uses the stable item data-testid to identify the target row and the AG Grid
     * column identifier col-id='cost' to locate the Cost cell.
     * Avoids generated element IDs and dynamic row indexes.
     *
     * Enters the value using the native input value setter, commits the edit with TAB,
     * waits safely for the editor to close despite AG Grid DOM re-rendering,
     * and verifies the committed cell value.
     *
     * @param price the Cost value to enter into the new invoice item row
     */
    public void enterPrice(String price) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Validates the requested Cost value before interacting with the grid.
        if (price == null || price.trim().isEmpty()) {
            throw new IllegalArgumentException("Cost value must not be null or empty.");
        }

        BigDecimal expectedPrice;

        try {
            expectedPrice = new BigDecimal(price.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid Cost value: " + price, e
            );
        }

        // Ensures the stable test ID of the newly added item row is available.
        if (newItemTestId == null || newItemTestId.trim().isEmpty()) {
            throw new IllegalStateException(
                    "The new item's data-testid has not been captured."
            );
        }

        // Locates the item cell using its stable data-testid.
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        // Locates the Cost cell within the same AG Grid row as the new item.
        By costCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost'"
                        + " and starts-with(@data-testid,'invoice-return-cost-')]"
        );

        // Locates the active inline editor inside the target Cost cell.
        By costInputLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='cost'"
                        + " and starts-with(@data-testid,'invoice-return-cost-')]"
                        + "//input[@data-ref='eInput']"
        );

        try {
            // Waits until the newly added item's cell is visible in the grid.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(newItemCellLocator)
            );

            // Scrolls the target item row into view before editing the Cost cell.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    newItemCell
            );

            // Waits for the Cost cell to be visible and ready for editing.
            WebElement costCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(costCellLocator)
            );

            // Opens the AG Grid inline editor for the Cost cell.
            new Actions(driver)
                    .doubleClick(costCell)
                    .perform();

            // Waits until the Cost input editor becomes visible.
            WebElement costInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(costInputLocator)
            );

            // Uses the native input value setter so React can detect the new value.
            JavascriptExecutor js = (JavascriptExecutor) driver;

            js.executeScript(
                    "const input = arguments[0];" +
                            "const value = arguments[1];" +
                            "const setter = Object.getOwnPropertyDescriptor(" +
                            "window.HTMLInputElement.prototype, 'value').set;" +
                            "setter.call(input, value);" +
                            "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                            "input.dispatchEvent(new Event('change', { bubbles: true }));",
                    costInput,
                    price.trim()
            );

            // Verifies the requested value is present in the active editor before committing.
            String editorValue = costInput.getDomProperty("value");

            if (editorValue == null
                    || new BigDecimal(editorValue.trim()).compareTo(expectedPrice) != 0) {
                throw new IllegalStateException(
                        "Cost editor value mismatch. Expected: "
                                + expectedPrice + ", Actual: " + editorValue
                );
            }

            System.out.println(
                    "Cost value entered and verified in editor: " + editorValue
            );

            // Commits the Cost cell edit using TAB.
            costInput.sendKeys(Keys.TAB);

            /*
             * Waits until the inline Cost editor is no longer present or visible.
             * Re-finds the editor on every poll to handle AG Grid re-rendering.
             * A stale element causes another poll rather than failing the test.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> editors =
                            currentDriver.findElements(costInputLocator);

                    for (WebElement editor : editors) {
                        try {
                            if (editor.isDisplayed()) {
                                return false;
                            }
                        } catch (StaleElementReferenceException e) {
                            // AG Grid replaced the editor; retry using the refreshed DOM.
                            return false;
                        }
                    }

                    // No matching editor remains visible.
                    return true;

                } catch (StaleElementReferenceException e) {
                    // Retries if the grid is refreshing while the editor is being checked.
                    return false;
                }
            });

            // Re-locates the Cost cell after the grid finishes re-rendering.
            WebElement updatedCostCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(costCellLocator)
            );

            // Reads the committed value from the Cost cell, not from the old editor.
            String actualPriceText = updatedCostCell.getText().trim();

            // Removes display formatting before converting the cell text to a number.
            String normalizedPrice = actualPriceText
                    .replace(",", "")
                    .replace("[", "")
                    .replace("]", "")
                    .trim();

            if (normalizedPrice.isEmpty()) {
                throw new IllegalStateException(
                        "Cost cell is empty after committing the value."
                );
            }

            BigDecimal actualPrice;

            try {
                actualPrice = new BigDecimal(normalizedPrice);
            } catch (NumberFormatException e) {
                throw new IllegalStateException(
                        "Unable to parse the committed Cost value. Actual text: "
                                + actualPriceText,
                        e
                );
            }

            // Confirms the committed cell value matches the requested Cost value.
            if (actualPrice.compareTo(expectedPrice) != 0) {
                throw new IllegalStateException(
                        "Cost value mismatch after commit. Expected: "
                                + expectedPrice
                                + ", Actual: "
                                + actualPriceText
                );
            }

            System.out.println(
                    "Cost value successfully committed and verified: "
                            + actualPriceText
            );

        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Timed out while entering or verifying the Cost value: " + price,
                    e
            );
        }
    }


    /**
     * Enters and verifies the Discount Rate (%) in the newly added row
     * of the Purchase Invoice Return AG Grid.
     *
     * Uses the new item's data-testid to identify the correct row and
     * the stable AG Grid column identifier to locate the Discount Rate cell.
     * Re-finds elements after potential grid re-rendering and retries stale
     * cell clicks. F2 activates inline editing, JavaScript sets the complete
     * value, and TAB commits the entered value.
     *
     * @param discountRate the non-negative discount percentage to enter
     */
    public void enterDiscountRate(String discountRate) {

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        JavascriptExecutor js = (JavascriptExecutor) driver;

        try {
            // Validate that the discount value was provided.
            if (discountRate == null || discountRate.trim().isEmpty()) {
                throw new IllegalArgumentException(
                        "Discount Rate must not be null or blank."
                );
            }

            // Validate that the discount value is numeric and non-negative.
            BigDecimal discountValue;

            try {
                discountValue = new BigDecimal(discountRate.trim());
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "Discount Rate must be a valid numeric value: "
                                + discountRate,
                        e
                );
            }

            if (discountValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException(
                        "Discount Rate must not be negative."
                );
            }

            // Ensure the new item's stable test ID is available.
            if (newItemTestId == null || newItemTestId.trim().isEmpty()) {
                throw new IllegalStateException(
                        "newItemTestId is not initialized. "
                                + "Add and identify the invoice return item row first."
                );
            }

            // Derive the Discount Rate test ID for the same item row.
            String discountRateTestId =
                    newItemTestId.replace("itemId", "discountRate");

            // Locate the new item's cell by its data-testid.
            By newItemCellLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='itemId'][data-testid='"
                            + newItemTestId + "']"
            );

            // Locate the Discount Rate cell in the identified row.
            By discountRateCellLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='discountRate'][data-testid='"
                            + discountRateTestId + "']"
            );

            // Locate the inline input belonging to the Discount Rate cell.
            By discountRateEditorLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='discountRate'][data-testid='"
                            + discountRateTestId
                            + "'].ag-cell-inline-editing input"
            );

            // Wait for the new item's cell and scroll its row into view.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            newItemCellLocator
                    )
            );

            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    newItemCell
            );

            // Wait for the target Discount Rate cell to become visible.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            System.out.println(
                    "Discount Rate cell found: " + discountRateTestId
            );

            // Read the current cell text using a fresh element reference.
            System.out.println(
                    "Discount Rate before editing: ["
                            + wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    discountRateCellLocator
                            )
                    ).getText().trim() + "]"
            );

            // Click the target cell, retrying if AG Grid replaces its DOM element.
            boolean cellClicked = false;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement currentCell = wait.until(
                            ExpectedConditions.elementToBeClickable(
                                    discountRateCellLocator
                            )
                    );

                    currentCell.click();
                    cellClicked = true;
                    break;

                } catch (StaleElementReferenceException e) {
                    System.out.println(
                            "Discount Rate cell became stale during click. "
                                    + "Retry " + attempt + "/3."
                    );
                }
            }

            if (!cellClicked) {
                throw new RuntimeException(
                        "Could not click the Discount Rate cell after 3 attempts."
                );
            }

            // Re-find the cell before activating inline editing with F2.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            // Retry F2 if the cell is re-rendered before the key can be sent.
            boolean editingActivated = false;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement currentCell = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    discountRateCellLocator
                            )
                    );

                    currentCell.sendKeys(Keys.F2);
                    editingActivated = true;
                    break;

                } catch (StaleElementReferenceException e) {
                    System.out.println(
                            "Discount Rate cell became stale while activating edit. "
                                    + "Retry " + attempt + "/3."
                    );
                }
            }

            if (!editingActivated) {
                throw new RuntimeException(
                        "Could not activate Discount Rate editing after 3 attempts."
                );
            }

            // Wait for the inline editor to appear in the target cell.
            WebElement discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateEditorLocator
                    )
            );

            // Set the complete value through the native input value setter.
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

            // Verify the editor value while safely handling DOM re-rendering.
            wait.until(currentDriver -> {
                try {
                    WebElement currentInput =
                            currentDriver.findElement(discountRateEditorLocator);

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

            // Re-find the editor before committing the value with TAB.
            boolean valueCommitted = false;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement currentInput = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    discountRateEditorLocator
                            )
                    );

                    currentInput.sendKeys(Keys.TAB);
                    valueCommitted = true;
                    break;

                } catch (StaleElementReferenceException e) {
                    System.out.println(
                            "Discount Rate editor became stale during commit. "
                                    + "Retry " + attempt + "/3."
                    );
                }
            }

            if (!valueCommitted) {
                throw new RuntimeException(
                        "Could not commit the Discount Rate value after 3 attempts."
                );
            }

            // Wait until the target cell exits inline-editing mode.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(discountRateCellLocator);

                    String classAttribute = currentCell.getAttribute("class");

                    return classAttribute != null
                            && !classAttribute.contains("ag-cell-inline-editing");

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            // Re-find the cell and read the committed value.
            String finalDiscountRate = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            ).getText().trim();

            System.out.println(
                    "Discount Rate after commit: [" + finalDiscountRate + "]"
            );

            // Normalize the displayed value before numeric comparison.
            String normalizedDiscountRate =
                    finalDiscountRate.replace(",", "").trim();

            if (normalizedDiscountRate.isEmpty()) {
                throw new AssertionError(
                        "Discount Rate cell is empty after committing the value."
                );
            }

            // Parse the committed value and compare it with the requested value.
            BigDecimal actualDiscountRate =
                    new BigDecimal(normalizedDiscountRate);

            if (actualDiscountRate.compareTo(discountValue) != 0) {
                throw new AssertionError(
                        "Discount Rate mismatch. Expected: "
                                + discountValue.toPlainString()
                                + ", Actual: " + finalDiscountRate
                );
            }

            System.out.println(
                    "Discount Rate entered and verified successfully."
            );

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Discount Rate ["
                            + discountRate
                            + "] for the purchase return invoice item with test ID ["
                            + newItemTestId + "].",
                    e
            );
        }
    }



    /**
     * Enters the requested Discount Value into the Discount Value cell
     * of the newly added Purchase Return Invoice AG Grid row.
     *
     * Uses the stable item data-testid to identify the correct row,
     * and col-id='discountValue' to locate the Discount Value cell.
     *
     * Activates inline editing, enters the complete requested value
     * through the native HTML input setter, verifies the editor value,
     * and commits the change using TAB.
     *
     * @param discountValue the discount amount to enter
     */
    public void enterDiscountValue(String discountValue) {

        System.out.println("========================================");
        System.out.println("Entering Purchase Return Invoice Discount Value");
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

        // Stable locator for the newly added item's cell, used as the row anchor.
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        // Locate the Discount Value cell inside the same row as the identified item.
        By discountValueCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='discountValue']"
        );

        // Locate the active text editor inside the Discount Value cell of that row.
        By discountValueEditorLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' and @data-testid='"
                        + newItemTestId
                        + "']/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='discountValue']"
                        + "//input[contains(@class,'ag-input-field-input') "
                        + "and contains(@class,'ag-text-field-input')]"
        );

        try {

            // Wait until the identified item cell is visible.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Scroll the identified item row into view before editing its discount.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});",
                    newItemCell
            );

            // Wait until the Discount Value cell in the identified row is visible.
            WebElement discountCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            );

            System.out.println(
                    "Discount Value cell found in row: " + newItemTestId
            );

            System.out.println(
                    "Discount Value before editing: ["
                            + discountCell.getText().trim() + "]"
            );

            // Wait until the Discount Value cell receives focus from the preceding edit.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(discountValueCellLocator);

                    String classAttribute = currentCell.getAttribute("class");

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
            String trimmedValue = discountValue.trim();

            new Actions(driver)
                    .sendKeys(trimmedValue.substring(0, 1))
                    .perform();

            System.out.println("Inline editing activation requested.");

            // Wait until the Discount Value input editor appears in the identified row.
            WebElement discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            System.out.println("Discount Value editor activated.");

            // Set the complete value through the native HTML input setter.
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
                    trimmedValue
            );

            // Verify the entire requested value in the active editor.
            wait.until(currentDriver -> {
                try {
                    WebElement currentEditor =
                            currentDriver.findElement(discountValueEditorLocator);

                    return trimmedValue.equals(
                            currentEditor.getAttribute("value")
                    );

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discount Value verified in editor: [" + trimmedValue + "]"
            );

            // Re-locate the editor before committing to avoid using a stale element.
            discountEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // Commit the Discount Value without intentionally adding another row.
            discountEditor.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Discount Value.");

            // Wait until the Discount Value cell exits inline-editing mode.
            wait.until(currentDriver -> {
                try {
                    WebElement currentCell =
                            currentDriver.findElement(discountValueCellLocator);

                    String classAttribute = currentCell.getAttribute("class");

                    return classAttribute != null
                            && !classAttribute.contains("ag-cell-inline-editing");

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            // Re-locate the cell after AG Grid commits and renders the value.
            WebElement updatedDiscountCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueCellLocator
                    )
            );

            // Read the final displayed value after the commit.
            String actualDiscountValue =
                    updatedDiscountCell.getText().trim();

            System.out.println(
                    "Discount Value after commit: [" + actualDiscountValue + "]"
            );

            // Fail explicitly if the committed cell does not display the requested value.
            if (actualDiscountValue.isBlank()) {
                throw new IllegalStateException(
                        "Discount Value cell is empty after committing the value ["
                                + trimmedValue + "]."
                );
            }

            System.out.println("Discount Value committed successfully.");
            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Discount Value ["
                            + discountValue
                            + "] for Purchase Return Invoice item row ["
                            + newItemTestId + "].",
                    e
            );
        }
    }




    /**
     * Enters the requested Addition Rate (%) into the Addition Rate cell
     * of the newly added Purchase Return Invoice AG Grid row.
     *
     * Uses the stable item data-testid to identify the correct row and
     * col-id='extraRate' to locate the Addition Rate cell.
     *
     * Re-locates the inline editor immediately before JavaScript interaction
     * to avoid stale WebElement references caused by AG Grid re-rendering.
     * Retries the editing operation when the editor becomes stale, verifies
     * the entered value, commits it with TAB, and checks the rendered value.
     *
     * @param extraRate the addition percentage to enter
     */
    public void enterExtraRate(String extraRate) {

        System.out.println("========================================");
        System.out.println("Entering Purchase Return Invoice Addition Rate (%)");
        System.out.println("Requested addition rate: " + extraRate);

        // Validate the requested value before interacting with the grid.
        if (extraRate == null || extraRate.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Rate (%) cannot be null or blank."
            );
        }

        // Ensure the new item row can be identified reliably.
        if (newItemTestId == null || newItemTestId.isBlank()) {
            throw new IllegalStateException(
                    "newItemTestId is not initialized. "
                            + "Add and identify the invoice item row first."
            );
        }

        String expectedValue = extraRate.trim();

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Stable identifier for the newly added item cell.
        By newItemCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId'][data-testid='"
                        + newItemTestId + "']"
        );

        // Locate the Addition Rate cell inside the identified item row.
        By extraRateCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' "
                        + "and @data-testid='" + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='extraRate']"
        );

        // Locate the focused Addition Rate cell in the identified row.
        By focusedExtraRateCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' "
                        + "and @data-testid='" + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='extraRate' "
                        + "and contains(@class,'ag-cell-focus')]"
        );

        // Locate the inline editor belonging to the identified Addition Rate cell.
        By extraRateEditorLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' "
                        + "and @data-testid='" + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='extraRate' "
                        + "and contains(@class,'ag-cell-inline-editing')]"
                        + "//input"
        );

        // Locate the Addition Rate cell after inline editing has ended.
        By committedExtraRateCellLocator = By.xpath(
                "//div[@role='gridcell' and @col-id='itemId' "
                        + "and @data-testid='" + newItemTestId + "']"
                        + "/ancestor::div[@role='row'][1]"
                        + "//div[@role='gridcell' and @col-id='extraRate' "
                        + "and not(contains(@class,'ag-cell-inline-editing'))]"
        );

        try {

            // Wait until the newly added item's identifying cell is visible.
            WebElement newItemCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            newItemCellLocator
                    )
            );

            // Scroll the correct item row into view before editing its Addition Rate.
            js.executeScript(
                    "arguments[0].scrollIntoView({block:'center',inline:'nearest'});",
                    newItemCell
            );

            // Wait until the target Addition Rate cell is visible.
            WebElement extraRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateCellLocator
                    )
            );

            System.out.println(
                    "Addition Rate cell found in item row: " + newItemTestId
            );

            System.out.println(
                    "Addition Rate before editing: ["
                            + extraRateCell.getText().trim() + "]"
            );

            // Focus the target cell without relying on its previously stored reference.
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            extraRateCellLocator
                    )
            );

            new Actions(driver)
                    .moveToElement(
                            driver.findElement(extraRateCellLocator)
                    )
                    .click()
                    .perform();

            // Wait until keyboard focus reaches the correct cell.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            focusedExtraRateCellLocator
                    )
            );

            System.out.println("Addition Rate cell is focused.");

            /*
             * Activate inline editing with the first character.
             * The editor is deliberately located again after this action,
             * because AG Grid may replace its DOM element during editing.
             */
            new Actions(driver)
                    .sendKeys(expectedValue.substring(0, 1))
                    .perform();

            boolean valueEntered = false;
            RuntimeException lastEditingException = null;

            // Retry if AG Grid re-renders the editor during value assignment.
            for (int attempt = 1; attempt <= 3 && !valueEntered; attempt++) {
                try {

                    System.out.println(
                            "Entering Addition Rate, attempt " + attempt
                    );

                    // Find the current editor rather than reusing an old WebElement.
                    wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    extraRateEditorLocator
                            )
                    );

                    WebElement currentEditor = driver.findElement(
                            extraRateEditorLocator
                    );

                    // Confirm that the editor is attached before JavaScript interaction.
                    wait.until(
                            ExpectedConditions.refreshed(
                                    ExpectedConditions.visibilityOfElementLocated(
                                            extraRateEditorLocator
                                    )
                            )
                    );

                    // Re-find the editor immediately before executing JavaScript.
                    currentEditor = driver.findElement(
                            extraRateEditorLocator
                    );

                    /*
                     * Set the complete value using the native HTML input setter.
                     * Dispatch input/change events so the application can process it.
                     */
                    js.executeScript(
                            """
                            const input = arguments[0];
                            const value = arguments[1];
    
                            if (!input || !input.isConnected) {
                                throw new Error(
                                    'Addition Rate editor is no longer attached to the DOM.'
                                );
                            }
    
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
                            currentEditor,
                            expectedValue
                    );

                    // Verify the requested value in the current inline editor.
                    wait.until(currentDriver -> {
                        try {
                            WebElement refreshedEditor =
                                    currentDriver.findElement(
                                            extraRateEditorLocator
                                    );

                            return expectedValue.equals(
                                    refreshedEditor.getAttribute("value")
                            );

                        } catch (StaleElementReferenceException
                                 | NoSuchElementException e) {
                            return false;
                        }
                    });

                    valueEntered = true;

                    System.out.println(
                            "Addition Rate verified in editor: ["
                                    + expectedValue + "]"
                    );

                } catch (StaleElementReferenceException e) {

                    lastEditingException = new RuntimeException(
                            "Addition Rate editor became stale during attempt "
                                    + attempt,
                            e
                    );

                    System.out.println(
                            "Addition Rate editor was re-rendered. "
                                    + "Retrying with a fresh element."
                    );

                }
            }

            // Stop rather than commit an unverified value.
            if (!valueEntered) {
                throw new RuntimeException(
                        "Could not enter Addition Rate after 3 attempts.",
                        lastEditingException
                );
            }

            /*
             * Re-locate the current editor immediately before TAB.
             * Do not reuse the editor reference from the JavaScript operation.
             */
            WebElement editorBeforeCommit = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            extraRateEditorLocator
                    )
            );

            // Commit the value with TAB to avoid accidentally triggering Add Line.
            editorBeforeCommit.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Addition Rate.");

            // Wait until editing ends in the same identified item row.
            WebElement committedCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            committedExtraRateCellLocator
                    )
            );

            // Verify that the committed cell contains the requested value.
            wait.until(currentDriver -> {
                try {
                    WebElement refreshedCommittedCell =
                            currentDriver.findElement(
                                    committedExtraRateCellLocator
                            );

                    String displayedValue =
                            refreshedCommittedCell.getText().trim();

                    return !displayedValue.isBlank()
                            && displayedValue.equals(expectedValue);

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            // Read the final rendered value from the target row.
            String committedValue = driver.findElement(
                    committedExtraRateCellLocator
            ).getText().trim();

            System.out.println(
                    "Addition Rate after commit: [" + committedValue + "]"
            );

            System.out.println(
                    "Addition Rate committed successfully in row: "
                            + newItemTestId
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Addition Rate (%) ["
                            + expectedValue
                            + "] in Purchase Return Invoice item row ["
                            + newItemTestId + "].",
                    e
            );
        }
    }


    /**
     * Enters and verifies the Addition Value in the currently focused AG Grid cell.
     *
     * Uses stable CSS locators based on the gridcell role and extraValue column ID.
     * All locators are defined locally inside this method.
     *
     * The intended Addition Value cell must already have keyboard focus.
     *
     * @param extraValue the addition amount to enter
     */
    public void enterExtraValue(String extraValue) {

        System.out.println("========================================");
        System.out.println("Entering Purchase Return Invoice Addition Value");

        // Validate the input before interacting with the grid.
        if (extraValue == null || extraValue.isBlank()) {
            throw new IllegalArgumentException(
                    "Addition Value cannot be null or blank."
            );
        }

        final BigDecimal expectedValue;

        try {
            expectedValue = new BigDecimal(extraValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Addition Value must be numeric: " + extraValue,
                    e
            );
        }

        String expectedText = extraValue.trim();

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        JavascriptExecutor js = (JavascriptExecutor) driver;

        // Stable locator for the Addition Value cell currently receiving focus.
        By focusedCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue'].ag-cell-focus"
        );

        // Stable locator for the inline editor in the Addition Value column.
        By editorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='extraValue']"
                        + ".ag-cell-inline-editing input[data-ref='eInput']"
        );

        try {
            // Wait until an Addition Value cell receives keyboard focus.
            wait.until(d -> {
                try {
                    WebElement focusedCell = d.findElement(focusedCellLocator);
                    return focusedCell.isDisplayed();
                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                }
            });

            System.out.println("Addition Value cell is focused.");

            // Activate inline editing by sending the first character.
            new Actions(driver)
                    .sendKeys(expectedText.substring(0, 1))
                    .perform();

            // Wait for the inline editor to become visible.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(editorLocator)
            );

            // Set the complete value using the native HTML input setter.
            boolean valueAssigned = false;

            for (int attempt = 1; attempt <= 3; attempt++) {
                try {
                    WebElement editor = wait.until(
                            ExpectedConditions.visibilityOfElementLocated(
                                    editorLocator
                            )
                    );

                    js.executeScript(
                            """
                            const input = arguments[0];
                            const value = arguments[1];
    
                            const setter = Object.getOwnPropertyDescriptor(
                                window.HTMLInputElement.prototype,
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
                            editor,
                            expectedText
                    );

                    valueAssigned = true;
                    break;

                } catch (StaleElementReferenceException e) {
                    System.out.println(
                            "Editor refreshed; retrying assignment, attempt "
                                    + attempt + " of 3."
                    );
                }
            }

            if (!valueAssigned) {
                throw new IllegalStateException(
                        "Could not assign the Addition Value after 3 attempts."
                );
            }

            // Verify the entered value numerically inside the editor.
            boolean editorValueVerified = wait.until(d -> {
                try {
                    WebElement editor = d.findElement(editorLocator);
                    String actualValue = editor.getAttribute("value");

                    return editor.isDisplayed()
                            && actualValue != null
                            && new BigDecimal(actualValue.trim())
                            .compareTo(expectedValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException e) {
                    return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            });

            if (!editorValueVerified) {
                throw new TimeoutException(
                        "Addition Value was not verified in the editor: "
                                + expectedText
                );
            }

            System.out.println(
                    "Addition Value entered successfully: " + expectedText
            );

            // Commit the value with TAB to avoid triggering Add Line with ENTER.
            WebElement currentEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(editorLocator)
            );

            currentEditor.sendKeys(Keys.TAB);

            // Wait until inline editing ends.
            wait.until(d -> d.findElements(editorLocator).isEmpty());

            // Stable locator for committed Addition Value cells.
            By committedCellsLocator = By.cssSelector(
                    "div[role='gridcell'][col-id='extraValue']"
                            + ":not(.ag-cell-inline-editing)"
            );

            /*
             * Verify that a committed Addition Value cell displays the expected
             * numeric value. This locator does not identify a specific row.
             */
            boolean savedValueVerified = wait.until(d -> {
                try {
                    List<WebElement> cells = d.findElements(
                            committedCellsLocator
                    );

                    for (WebElement cell : cells) {
                        if (!cell.isDisplayed()) {
                            continue;
                        }

                        String displayedValue = cell.getText().trim();

                        if (displayedValue.isEmpty()) {
                            continue;
                        }

                        String normalizedValue = displayedValue.replace(",", "");

                        if (new BigDecimal(normalizedValue)
                                .compareTo(expectedValue) == 0) {
                            return true;
                        }
                    }

                    return false;

                } catch (StaleElementReferenceException e) {
                    return false;
                } catch (NumberFormatException e) {
                    return false;
                }
            });

            if (!savedValueVerified) {
                throw new TimeoutException(
                        "Addition Value was not verified after commit: "
                                + expectedText
                );
            }

            System.out.println(
                    "Addition Value committed successfully: " + expectedText
            );
            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to enter Addition Value [" + expectedText + "].",
                    e
            );
        }
    }

    /**
     * Selects the Expire Date for the Purchase Return Invoice.
     *
     * Uses stable AG Grid column identifiers and accessible calendar labels.
     * All locators are declared locally inside this method.
     * Avoids data-testid attributes, generated element IDs, and fixed row indexes.
     *
     * The intended Expire Date cell must be the active cell in the current grid.
     * Selects a date 30 days from today and verifies the committed cell value.
     */
    public void selectExpireDate() {

        System.out.println("========================================");
        System.out.println("Selecting Purchase Return Invoice Expire Date");

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Calculate the target expiration date: 30 days from today.
        LocalDate expireDate = LocalDate.now().plusDays(30);

        System.out.println("Target Expire Date: " + expireDate);

        // Stable locator for the Expire Date cell, based on its AG Grid column ID.
        By expireDateCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='expireDate']"
        );

        // Identify the active Expire Date cell without using generated identifiers.
        By focusedExpireDateCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='expireDate'].ag-cell-focus"
        );

        // Locate the visible React Modern DatePicker calendar.
        By calendarWrapperLocator = By.cssSelector(
                ".rmdp-wrapper"
        );

        // Locate the Next Month control using the calendar's stable class names.
        By nextMonthLocator = By.cssSelector(
                ".rmdp-wrapper .rmdp-arrow-container.rmdp-right"
        );

        // Build the accessible label for the requested date.
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

        // Locate the exact date using its accessible label.
        By targetDateLocator = By.cssSelector(
                ".rmdp-wrapper [aria-label='"
                        + targetDateAriaLabel + "']"
        );

        try {
            /*
             * Wait until the intended Expire Date cell is focused.
             * This avoids clicking an arbitrary row in a multi-row grid.
             */
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    focusedExpireDateCellLocator
            ));

            System.out.println("Expire Date cell is focused.");

            /*
             * Scroll the focused cell into view using its stable column locator.
             * Re-query the DOM instead of relying on a previously stored element.
             */
            Boolean cellScrolledIntoView = (Boolean)
                    ((JavascriptExecutor) driver).executeScript(
                            """
                            const cell = document.querySelector(
                                "div[role='gridcell'][col-id='expireDate'].ag-cell-focus"
                            );
    
                            if (!cell || cell.getClientRects().length === 0) {
                                return false;
                            }
    
                            cell.scrollIntoView({
                                block: 'center',
                                inline: 'center'
                            });
    
                            return true;
                            """
                    );

            if (!Boolean.TRUE.equals(cellScrolledIntoView)) {
                throw new IllegalStateException(
                        "Could not scroll the focused Expire Date cell into view."
                );
            }

            /*
             * Re-locate the focused cell and send F2 to activate editing
             * or open the associated date picker.
             */
            WebElement expireDateCell = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            focusedExpireDateCellLocator
                    )
            );

            expireDateCell.sendKeys(Keys.F2);

            System.out.println(
                    "F2 sent. Waiting for the Expire Date calendar..."
            );

            // Wait until the date picker becomes visible.
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    calendarWrapperLocator
            ));

            System.out.println("Expire Date calendar opened.");
            System.out.println(
                    "Target date aria-label: " + targetDateAriaLabel
            );

            /*
             * Search for the target date in the currently displayed month.
             * Navigate forward one month at a time when the date is not present.
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
                                return date.isDisplayed()
                                        && date.isEnabled();
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

                // Find an enabled Next Month control without using generated IDs.
                WebElement nextMonthControl = wait.until(d -> {
                    for (WebElement control :
                            d.findElements(nextMonthLocator)) {
                        try {
                            if (control.isDisplayed() && control.isEnabled()) {
                                return control;
                            }
                        } catch (StaleElementReferenceException ignored) {
                            // Retry with a fresh element on the next polling cycle.
                        }
                    }

                    return null;
                });

                nextMonthControl.click();

                // Wait for the calendar to remain visible after month navigation.
                wait.until(ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                ));
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

            // Click the exact target date after confirming it is available.
            wait.until(ExpectedConditions.elementToBeClickable(
                    targetDateLocator
            )).click();

            System.out.println("Selected Expire Date: " + expireDate);

            // Wait until the date picker closes after selection.
            wait.until(ExpectedConditions.invisibilityOfElementLocated(
                    calendarWrapperLocator
            ));

            /*
             * Verify that the active Expire Date cell displays a value.
             * This uses the stable column locator, not a dynamic test ID.
             */
            String committedExpireDate = wait.until(d -> {
                try {
                    WebElement focusedCell = d.findElement(
                            focusedExpireDateCellLocator
                    );

                    String value = focusedCell.getText().trim();

                    return focusedCell.isDisplayed() && !value.isEmpty()
                            ? value
                            : null;

                } catch (NoSuchElementException
                         | StaleElementReferenceException e) {
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
                    "Failed to select Expire Date [" + expireDate + "].",
                    e
            );
        }
    }




    /**
     * Enters the Note (البيان) into the Purchase Return Invoice AG Grid cell.
     *
     * Uses col-id="note" as the stable column locator.
     * Preserves spaces by setting the editor value and dispatching input events.
     * Commits the edit by clicking the Discounts and Additions section.
     * Does not use data-testid, generated IDs, or dynamic row indexes.
     *
     * @param note the note text to enter, including spaces
     */
    public void enterNote(String note) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locates the Note cell through its stable AG Grid column identifier.
        By noteCellLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note']"
        );

        WebElement noteCell = wait.until(
                ExpectedConditions.elementToBeClickable(noteCellLocator)
        );

        // Scrolls the Note cell into view before activating edit mode.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                noteCell
        );

        // Activates editing on the Note cell using the keyboard.
        noteCell.click();
        noteCell.sendKeys(Keys.F2);

        // Locates the active text editor inside the Note column without generated IDs.
        By noteEditorLocator = By.cssSelector(
                "div[role='gridcell'][col-id='note'] input, " +
                        "div[role='gridcell'][col-id='note'] textarea"
        );

        WebElement noteEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(noteEditorLocator)
        );

        // Sets the exact text, preserving spaces, and notifies the application of the change.
        ((JavascriptExecutor) driver).executeScript("""
        const editor = arguments[0];
        const text = arguments[1];

        const prototype = editor.tagName === 'TEXTAREA'
            ? window.HTMLTextAreaElement.prototype
            : window.HTMLInputElement.prototype;

        const setter = Object.getOwnPropertyDescriptor(
            prototype, 'value'
        ).set;

        setter.call(editor, text);

        editor.dispatchEvent(new Event('input', { bubbles: true }));
        editor.dispatchEvent(new Event('change', { bubbles: true }));
        """, noteEditor, note);

        // Confirms the editor contains the exact requested value before committing.
        String enteredValue = noteEditor.getAttribute("value");

        if (enteredValue == null || !enteredValue.equals(note)) {
            throw new IllegalStateException(
                    "Note editor value does not match the requested text."
            );
        }

        // Locates the Discounts and Additions section to commit the cell edit.
        By discountsAndAdditionsLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' and not(*)]"
        );

        // Clicking the section moves focus outside the grid editor and commits the value.
        wait.until(
                ExpectedConditions.elementToBeClickable(discountsAndAdditionsLocator)
        ).click();

        // Waits for the Note editor to close instead of comparing rendered cell text,
        // which may normalize whitespace and incorrectly fail the test.
        wait.until(d ->
                d.findElements(noteEditorLocator).stream()
                        .noneMatch(WebElement::isDisplayed)
        );

        System.out.println(
                "Note entered with spaces and committed successfully: [" + note + "]"
        );
    }


    /**
     * Clicks the Add Line button in the Discounts & Additions section
     * and waits until AG Grid renders an additional ordinary data row.
     *
     * Uses the stable Arabic button text and AG Grid row structure.
     * All locators are declared locally inside this method.
     *
     * This method is dedicated to the Discounts & Additions grid
     * and is separate from the main invoice items Add Line method.
     */
    public void clickAddDiscountAdditionLine() {

        System.out.println("========================================");
        System.out.println("Adding a Discounts & Additions row");

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Stable locator for the Discounts & Additions section heading.
        By discountsAndAdditionsHeadingLocator = By.xpath(
                "//*[normalize-space(.)='الخصومات والإضافات' and not(*)]"
        );

        // Stable locator for the Add Line button using its visible Arabic label.
        By addLineButtonLocator = By.xpath(
                "//button[normalize-space(.)='اضافة سطر']"
        );

        /*
         * Locator for ordinary AG Grid data rows.
         * Excludes pinned rows whose row-index begins with 'b-'.
         */
        By ordinaryGridRowsLocator = By.cssSelector(
                "div.ag-root[role='grid'] "
                        + "div.ag-center-cols-container "
                        + "> div[role='row'][row-index]"
                        + ":not([row-index^='b-'])"
        );

        try {
            // Ensure the Discounts & Additions section is visible and accessible.
            WebElement sectionHeading = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountsAndAdditionsHeadingLocator
                    )
            );

            // Bring the section heading into view before interacting with the grid.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center'});",
                    sectionHeading
            );

            System.out.println(
                    "Discounts & Additions section is visible."
            );

            // Wait for the Add Line button to become clickable.
            WebElement addLineButton = wait.until(
                    ExpectedConditions.elementToBeClickable(
                            addLineButtonLocator
                    )
            );

            /*
             * Count ordinary grid rows before clicking.
             * Re-querying the DOM avoids relying on previously stored row elements.
             */
            int rowsBefore = driver.findElements(
                    ordinaryGridRowsLocator
            ).size();

            System.out.println(
                    "Rows before adding a line: " + rowsBefore
            );

            // Click the Add Line button in the Discounts & Additions section.
            addLineButton.click();

            System.out.println(
                    "Clicked the Discounts & Additions Add Line button."
            );

            /*
             * Wait until the ordinary row count increases.
             * This confirms that AG Grid rendered an additional data row.
             */
            wait.until(currentDriver ->
                    currentDriver.findElements(ordinaryGridRowsLocator).size()
                            > rowsBefore
            );

            int rowsAfter = driver.findElements(
                    ordinaryGridRowsLocator
            ).size();

            System.out.println(
                    "Rows after adding a line: " + rowsAfter
            );

            System.out.println(
                    "New Discounts & Additions row detected successfully."
            );

            System.out.println("========================================");

        } catch (Exception e) {
            throw new RuntimeException(
                    "Failed to add a row in the Discounts & Additions section.",
                    e
            );
        }
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
     * in the captured row of the Discounts & Additions grid.
     *
     * Uses the stable AG Grid column identifier "accountId"
     * and the previously captured newDiscountAdditionRowIndex.
     *
     * Does not depend on generated React IDs or data-testid attributes.
     * Activates the cell editor using ENTER, then selects a random
     * visible and enabled React Select option.
     */
    public void selectRandomAccount() {

        System.out.println("========================================");
        System.out.println("Selecting random Account (الحساب)");
        System.out.println(
                "Discounts & Additions row index: "
                        + newDiscountAdditionRowIndex
        );

        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        /*
         * Stable locator for the Account cell in the captured AG Grid row.
         * Uses the fixed column identifier and the stored row index.
         */
        By accountCellLocator = By.cssSelector(
                "div.ag-center-cols-container "
                        + "> div[role='row'][row-index='"
                        + newDiscountAdditionRowIndex
                        + "'] "
                        + "div[role='gridcell'][col-id='accountId']"
        );

        // Wait until the Account cell in the target row becomes visible.
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
         * Scroll the Account cell into view, including horizontal
         * positioning, so the cell can receive keyboard focus.
         */
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                accountCell
        );

        // Re-locate the cell after scrolling to avoid using a stale element.
        accountCell = wait.until(
                ExpectedConditions.elementToBeClickable(
                        accountCellLocator
                )
        );

        // Give AG Grid keyboard focus before activating its editor.
        accountCell.click();

        // Activate the Account cell editor using the Enter key.
        accountCell.sendKeys(Keys.ENTER);

        /*
         * Stable locator for the React Select input inside an Account cell.
         * Does not use generated React input IDs.
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

        // Click the editor input to display the available Account options.
        accountInput.click();

        /*
         * Locate the visible React Select listbox without relying
         * on its generated ID.
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
         * Collect visible options and exclude those explicitly marked
         * as disabled.
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

        // Stop with a clear error if no selectable Accounts are available.
        if (selectableOptions.isEmpty()) {
            throw new IllegalStateException(
                    "No visible, selectable Account options were found "
                            + "in the Discounts & Additions grid."
            );
        }

        // Select one Account randomly from the available options.
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
     * (التأثير على التكلفة) cell in the Discounts & Additions grid.
     *
     * Uses the captured row index and the stable AG Grid column identifier.
     * Preserves an existing value and retries if the dropdown options
     * do not appear on the first attempt.
     * Verifies the selected value in the target cell.
     * Does not use generated element IDs or press TAB.
     */
    public void selectRandomInfluencingOnCost() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        // Locates the target cell by its row and stable column identifier.
        By influencingOnCostCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='influencingOnCost']"
        );

        // Locates enabled dropdown options without relying on generated IDs.
        By optionLocator = By.cssSelector(
                "[role='option']:not([aria-disabled='true'])"
        );

        final int maxAttempts = 2;

        System.out.println("========================================");
        System.out.println("Selecting random Influencing on Cost");
        System.out.println(
                "Target row index: " + newDiscountAdditionRowIndex
        );

        try {

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {

                System.out.println(
                        "Activation attempt: " + attempt + "/" + maxAttempts
                );

                // Waits for the target cell to become visible.
                WebElement cell = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                influencingOnCostCellLocator
                        )
                );

                // Preserves the current selection if the cell already has a value.
                String currentValue = cell.getText().trim();

                if (!currentValue.isEmpty()) {
                    System.out.println(
                            "Influencing on Cost already selected: " + currentValue
                    );
                    return;
                }

                // Scrolls the target cell into view before activating its editor.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView(" +
                                "{block:'center', inline:'center'});",
                        cell
                );

                // Re-finds the cell after scrolling to avoid using a stale reference.
                cell = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                influencingOnCostCellLocator
                        )
                );

                // Double-clicks the cell to activate its editor.
                new Actions(driver)
                        .doubleClick(cell)
                        .perform();

                // Waits for visible, enabled options and re-queries during polling.
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
                                // Ignores options replaced during grid re-rendering.
                            }
                        }

                        return visibleOptions.isEmpty()
                                ? null
                                : visibleOptions;
                    });

                } catch (TimeoutException dropdownTimeout) {

                    System.out.println(
                            "Dropdown options did not appear on attempt " + attempt
                    );

                    // Logs the current cell HTML to help diagnose activation failures.
                    try {
                        WebElement currentCell = driver.findElement(
                                influencingOnCostCellLocator
                        );

                        System.out.println(
                                "Cell HTML: " +
                                        currentCell.getAttribute("outerHTML")
                        );

                    } catch (NoSuchElementException |
                             StaleElementReferenceException ignored) {
                        System.out.println(
                                "Target cell was unavailable during diagnostics."
                        );
                    }

                    // Retries once before reporting a failure.
                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "Influencing on Cost options did not appear after " +
                                    maxAttempts + " attempts.",
                            dropdownTimeout
                    );
                }

                // Collects distinct, non-empty option labels.
                List<String> optionTexts = new ArrayList<>();

                for (WebElement option : availableOptions) {
                    try {
                        String text = option.getText().trim();

                        if (!text.isEmpty() && !optionTexts.contains(text)) {
                            optionTexts.add(text);
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Ignores options replaced while reading their labels.
                    }
                }

                // Fails clearly if no readable options are available.
                if (optionTexts.isEmpty()) {
                    if (attempt < maxAttempts) {
                        continue;
                    }

                    throw new TimeoutException(
                            "The Influencing on Cost dropdown opened, " +
                                    "but no readable option labels were found."
                    );
                }

                // Selects one available option randomly.
                String selectedText = optionTexts.get(
                        new Random().nextInt(optionTexts.size())
                );

                System.out.println(
                        "Random Influencing on Cost option: " + selectedText
                );

                // Locates the exact option by its visible text, not a generated ID.
                By selectedOptionLocator = By.xpath(
                        "//*[@role='option' and normalize-space(.)=" +
                                toXPathLiteral(selectedText) +
                                "]"
                );

                // Waits for the selected option to be clickable.
                WebElement selectedOption = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOptionLocator
                        )
                );

                // Clicks the randomly selected option.
                selectedOption.click();

                // Verifies the selected value while re-finding the cell on each poll.
                wait.until(currentDriver -> {
                    try {
                        WebElement updatedCell = currentDriver.findElement(
                                influencingOnCostCellLocator
                        );

                        return updatedCell.getText()
                                .trim()
                                .equals(selectedText);

                    } catch (StaleElementReferenceException |
                             NoSuchElementException ignored) {
                        return false;
                    }
                });

                System.out.println(
                        "Influencing on Cost selected successfully: " +
                                selectedText
                );
                System.out.println("========================================");
                return;
            }

        } catch (TimeoutException e) {

            // Logs the target cell HTML when selection fails.
            System.out.println("Influencing on Cost selection failed.");

            try {
                List<WebElement> cells =
                        driver.findElements(influencingOnCostCellLocator);

                if (!cells.isEmpty()) {
                    System.out.println(
                            "Cell HTML: " +
                                    cells.get(0).getAttribute("outerHTML")
                    );
                }

            } catch (StaleElementReferenceException ignored) {
                System.out.println(
                        "Target cell was re-rendered during diagnostics."
                );
            }

            // Preserves the original exception for TestNG reporting.
            throw e;
        }
    }

    /**
     * Enters the requested Discount Rate (نسبة الخصم) into the
     * captured row of the Purchase Return Invoice Discounts & Additions grid.
     *
     * Uses stable AG Grid attributes: row-index and col-id.
     * Does not depend on data-testid or generated element IDs.
     * Activates the inline editor, enters the requested value,
     * commits it with TAB, and verifies the saved value.
     */
    public void enterDiscountRateInDiscountsSection(String discountRate) {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locates the Discount Rate cell using the captured row and stable column identifier.
        By discountRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='discountRate']"
        );

        // Locates the native input editor associated with the Discount Rate cell.
        By discountRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container " +
                        "div[role='row'][row-index='" +
                        newDiscountAdditionRowIndex +
                        "'] " +
                        "div[role='gridcell'][col-id='discountRate'] " +
                        "input.ag-input-field-input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Rate in Purchase Return Invoice");
        System.out.println("Requested discount rate: " + discountRate);
        System.out.println(
                "Target row index: " + newDiscountAdditionRowIndex
        );

        try {
            // Waits for the Discount Rate cell to become visible.
            WebElement discountRateCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            System.out.println(
                    "Discount Rate before editing: [" +
                            discountRateCell.getText().trim() + "]"
            );

            // Scrolls the target cell into view before activating its editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    discountRateCell
            );

            // Re-locates the cell after scrolling to avoid using a stale reference.
            wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            // Uses the existing framework helper to activate the inline editor.
            if (!tryActivateGridCellEditor(
                    discountRateCellLocator,
                    discountRateInputLocator
            )) {
                System.out.println(
                        "Discount Rate is disabled or non-editable. Skipping."
                );
                return;
            }

            System.out.println("Discount Rate editor activated.");

            // Waits for the input editor to become visible.
            WebElement discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateInputLocator
                    )
            );

            // Sets the input value and dispatches events for the UI framework.
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
                    discountRateInput,
                    discountRate
            );

            // Re-locates the editor before committing to avoid a stale reference.
            discountRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateInputLocator
                    )
            );

            // Checks that the editor accepted the requested value.
            String editorValue = discountRateInput.getAttribute("value");

            System.out.println(
                    "Discount Rate editor value: [" + editorValue + "]"
            );

            if (!discountRate.equals(editorValue)) {
                throw new IllegalStateException(
                        "Discount Rate editor did not accept the requested value. " +
                                "Expected: [" + discountRate +
                                "], Actual: [" + editorValue + "]"
                );
            }

            // Commits the edited value using TAB.
            discountRateInput.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Discount Rate.");

            // Waits for the inline editor to close after committing.
            wait.until(
                    ExpectedConditions.invisibilityOfElementLocated(
                            discountRateInputLocator
                    )
            );

            // Re-locates the cell after AG Grid updates its displayed value.
            WebElement updatedCell = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountRateCellLocator
                    )
            );

            String committedValue = updatedCell.getText().trim();

            System.out.println(
                    "Discount Rate after commit: [" + committedValue + "]"
            );

            // Compares numeric values so 20 and 20.00 are treated as equivalent.
            java.math.BigDecimal expectedValue =
                    new java.math.BigDecimal(discountRate)
                            .stripTrailingZeros();

            String actualText = committedValue
                    .replace("%", "")
                    .trim();

            java.math.BigDecimal actualValue =
                    new java.math.BigDecimal(actualText)
                            .stripTrailingZeros();

            // Fails explicitly if the committed value differs from the requested value.
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

        } catch (TimeoutException e) {
            throw new RuntimeException(
                    "Timed out while editing or committing the Discount Rate cell.",
                    e
            );
        }
    }

    /**
     * Enters and verifies the requested Discount Value in the Purchase Return Invoice grid.
     *
     * Uses stable AG Grid attributes only:
     * - The fixed ag-center-cols-container container.
     * - The fixed role='row' and role='gridcell' attributes.
     * - The fixed col-id='discountValue' column identifier.
     *
     * Does not use data-testid, row-index, row-id, generated IDs,
     * or dynamically constructed locators.
     *
     * Re-finds elements after AG Grid re-renders to handle stale references.
     * Commits the edit with TAB and verifies the final displayed value.
     *
     * @param discountValue the discount value to enter
     */
    public void enterDiscountValueInDiscountsSection(String discountValue) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Validate the requested discount value.
        if (discountValue == null || discountValue.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Discount Value must not be null or empty."
            );
        }

        BigDecimal expectedValue;

        try {
            expectedValue = new BigDecimal(discountValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid Discount Value: " + discountValue, e
            );
        }

        /*
         * Stable locator for the Discount Value cell.
         * Uses only fixed container, row, cell, and column attributes.
         */
        By discountValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='discountValue']"
        );

        /*
         * Stable locator for the native input inside the Discount Value cell.
         * Does not use data-testid or generated IDs.
         */
        By discountValueEditorLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='discountValue'] "
                        + "input"
        );

        System.out.println("========================================");
        System.out.println("Entering Discount Value");
        System.out.println("Requested value: [" + discountValue + "]");

        try {
            // Wait until exactly one target Discount Value cell is present.
            wait.until(currentDriver ->
                    currentDriver.findElements(discountValueCellLocator).size() == 1
            );

            // Locate the current cell only after the grid has stabilized.
            WebElement discountValueCell =
                    driver.findElement(discountValueCellLocator);

            // Log the existing cell value before editing.
            System.out.println(
                    "Discount Value before editing: ["
                            + discountValueCell.getText().trim() + "]"
            );

            // Scroll the target cell into view before opening the editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    discountValueCell
            );

            /*
             * Activate the inline editor using the existing framework helper.
             * If the cell cannot be edited, skip the operation.
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

            // Wait for the inline editor to become visible.
            WebElement discountValueEditor = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            discountValueEditorLocator
                    )
            );

            // Replace the current editor value with the requested value.
            discountValueEditor.click();
            discountValueEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            discountValueEditor.sendKeys(discountValue.trim());

            // Verify the value before committing the edit.
            String enteredValue = discountValueEditor.getDomProperty("value");

            System.out.println(
                    "Discount Value editor before commit: ["
                            + enteredValue + "]"
            );

            if (enteredValue == null
                    || new BigDecimal(enteredValue.trim())
                    .compareTo(expectedValue) != 0) {

                throw new IllegalStateException(
                        "Discount Value mismatch in editor. Expected ["
                                + discountValue
                                + "] but found ["
                                + enteredValue + "]."
                );
            }

            // Commit the edited value with TAB.
            discountValueEditor.sendKeys(Keys.TAB);

            /*
             * Wait until the inline editor is no longer visible.
             * Re-query the DOM on every poll because AG Grid can replace elements.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> editors =
                            currentDriver.findElements(discountValueEditorLocator);

                    for (WebElement editor : editors) {
                        try {
                            if (editor.isDisplayed()) {
                                return false;
                            }
                        } catch (StaleElementReferenceException e) {
                            // AG Grid replaced the editor; retry on the next poll.
                            return false;
                        }
                    }

                    return true;

                } catch (StaleElementReferenceException e) {
                    // Retry while the grid is refreshing.
                    return false;
                }
            });

            /*
             * Verify the committed value using a fresh cell reference.
             * Stale elements and temporarily missing cells are retried.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> cells =
                            currentDriver.findElements(discountValueCellLocator);

                    // Do not read an ambiguous cell if more than one row is present.
                    if (cells.size() != 1) {
                        return false;
                    }

                    String displayedValue = cells.get(0).getText().trim();

                    String normalizedValue = displayedValue
                            .replace(",", "")
                            .replace("%", "")
                            .replace("[", "")
                            .replace("]", "")
                            .trim();

                    if (normalizedValue.isEmpty()) {
                        return false;
                    }

                    BigDecimal actualValue = new BigDecimal(normalizedValue);

                    return expectedValue.compareTo(actualValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException
                         | NumberFormatException e) {
                    // Retry until the grid displays the committed value.
                    return false;
                }
            });

            // Read the saved value after successful verification.
            String committedValue =
                    driver.findElement(discountValueCellLocator).getText().trim();

            System.out.println(
                    "Discount Value saved successfully: ["
                            + committedValue + "]"
            );
            System.out.println("========================================");

        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Timed out while entering or verifying Discount Value ["
                            + discountValue
                            + "]. Check that exactly one target row exists "
                            + "and that the cell is editable.",
                    e
            );
        }
    }

    /**
     * Enters the requested Addition Rate (نسبة الإضافي) into the Addition Rate cell.
     *
     * Uses stable AG Grid attributes and a fixed container structure.
     * Does not use data-testid, row-index, row-id, generated IDs,
     * or dynamically constructed locators.
     *
     * Re-finds elements after AG Grid re-renders to reduce stale element failures.
     * Activates the inline editor, sets the requested value, commits with TAB,
     * and verifies the final displayed value.
     *
     * @param additionRate the Addition Rate value to enter
     */
    public void enterAdditionRateInDiscountsSection(String additionRate) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Validate the requested Addition Rate before interacting with the grid.
        if (additionRate == null || additionRate.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Addition Rate must not be null or empty."
            );
        }

        BigDecimal expectedValue;

        try {
            expectedValue = new BigDecimal(additionRate.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid Addition Rate: " + additionRate, e
            );
        }

        /*
         * Stable locator for the Addition Rate cell.
         * Uses only the fixed AG Grid container, row structure, and col-id.
         */
        By additionRateCellLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='additionRate']"
        );

        /*
         * Stable locator for the input editor inside the Addition Rate cell.
         * Does not depend on data-testid or generated IDs.
         */
        By additionRateInputLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='additionRate'] "
                        + "input"
        );

        System.out.println("========================================");
        System.out.println("Entering Addition Rate in Discounts & Additions");
        System.out.println("Requested addition rate: [" + additionRate + "]");

        try {
            /*
             * Wait until exactly one Addition Rate cell exists.
             * If multiple rows match, do not edit an ambiguous cell.
             */
            wait.until(currentDriver ->
                    currentDriver.findElements(additionRateCellLocator).size() == 1
            );

            // Locate the current cell after the grid has stabilized.
            WebElement additionRateCell =
                    driver.findElement(additionRateCellLocator);

            System.out.println(
                    "Addition Rate before editing: ["
                            + additionRateCell.getText().trim() + "]"
            );

            // Scroll the target cell into view before opening the editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    additionRateCell
            );

            /*
             * Activate the inline editor using the existing framework helper.
             * Skip the operation if the cell is disabled or cannot be activated.
             */
            boolean editorActivated = tryActivateGridCellEditor(
                    additionRateCellLocator,
                    additionRateInputLocator
            );

            if (!editorActivated) {
                System.out.println(
                        "Addition Rate is disabled or non-editable. Skipping."
                );
                System.out.println("========================================");
                return;
            }

            System.out.println("Addition Rate cell activated successfully.");

            // Re-locate the input after activation to avoid using a stale reference.
            WebElement additionRateInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            additionRateInputLocator
                    )
            );

            /*
             * Set the value through the native HTML input setter.
             * Dispatch input and change events so the application can detect the edit.
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
                    additionRateInput,
                    additionRate.trim()
            );

            // Verify the requested value in the active editor before committing.
            String editorValue = additionRateInput.getDomProperty("value");

            System.out.println(
                    "Addition Rate editor value before commit: ["
                            + editorValue + "]"
            );

            if (editorValue == null
                    || new BigDecimal(editorValue.trim())
                    .compareTo(expectedValue) != 0) {

                throw new IllegalStateException(
                        "Addition Rate editor mismatch. Expected: ["
                                + additionRate
                                + "], Actual: ["
                                + editorValue + "]"
                );
            }

            // Commit the Addition Rate value using TAB.
            additionRateInput.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Addition Rate.");

            /*
             * Wait until the editor is no longer visible.
             * Re-query the DOM on each poll because AG Grid may replace the editor.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> editors =
                            currentDriver.findElements(additionRateInputLocator);

                    for (WebElement editor : editors) {
                        try {
                            if (editor.isDisplayed()) {
                                return false;
                            }
                        } catch (StaleElementReferenceException e) {
                            // AG Grid replaced the editor; retry on the next poll.
                            return false;
                        }
                    }

                    return true;

                } catch (StaleElementReferenceException e) {
                    // Retry while the grid is refreshing.
                    return false;
                }
            });

            /*
             * Wait for the committed cell value to match the requested value.
             * Locate the cell afresh during each poll to handle stale references.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> cells =
                            currentDriver.findElements(additionRateCellLocator);

                    // Avoid editing or validating an ambiguous cell when multiple rows exist.
                    if (cells.size() != 1) {
                        return false;
                    }

                    String displayedValue = cells.get(0).getText().trim();

                    String normalizedValue = displayedValue
                            .replace(",", "")
                            .replace("%", "")
                            .replace("[", "")
                            .replace("]", "")
                            .trim();

                    if (normalizedValue.isEmpty()) {
                        return false;
                    }

                    BigDecimal actualValue = new BigDecimal(normalizedValue);

                    return expectedValue.compareTo(actualValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException
                         | NumberFormatException e) {
                    // Retry until the grid displays the committed value.
                    return false;
                }
            });

            // Read the final displayed value after successful verification.
            String committedValue =
                    driver.findElement(additionRateCellLocator).getText().trim();

            System.out.println(
                    "Addition Rate entered successfully: ["
                            + committedValue + "]"
            );
            System.out.println("========================================");

        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Timed out while entering or verifying Addition Rate ["
                            + additionRate
                            + "]. Check that exactly one matching row exists "
                            + "and that the cell is editable.",
                    e
            );
        }
    }

    /**
     * Enters the requested Addition Value (قيمة الإضافي) into the Addition Value cell.
     *
     * Uses stable AG Grid attributes and a fixed container structure.
     * Does not use data-testid, row-index, row-id, generated IDs,
     * or dynamically constructed locators.
     *
     * Re-finds elements after AG Grid re-renders to reduce stale element failures.
     * Activates the inline editor, enters the requested value, commits with TAB,
     * and verifies the final displayed value.
     *
     * @param additionValue the Addition Value to enter
     */
    public void enterAdditionValueInDiscountsSection(String additionValue) {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(15));

        // Validate the requested Addition Value before interacting with the grid.
        if (additionValue == null || additionValue.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Addition Value must not be null or empty."
            );
        }

        BigDecimal expectedValue;

        try {
            expectedValue = new BigDecimal(additionValue.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(
                    "Invalid Addition Value: " + additionValue, e
            );
        }

        /*
         * Stable locator for the Addition Value cell.
         * Uses only the fixed AG Grid container, row structure, and col-id.
         */
        By additionValueCellLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='additionValue']"
        );

        /*
         * Stable locator for the native input editor inside the Addition Value cell.
         * Does not depend on data-testid or generated IDs.
         */
        By additionValueInputLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] "
                        + "> div[role='row'] "
                        + "> div[role='gridcell'][col-id='additionValue'] "
                        + "input"
        );

        System.out.println("========================================");
        System.out.println("Entering Addition Value in Discounts & Additions");
        System.out.println("Requested addition value: [" + additionValue + "]");

        try {
            /*
             * Wait until exactly one Addition Value cell exists.
             * Avoid modifying an ambiguous cell if multiple rows are present.
             */
            wait.until(currentDriver ->
                    currentDriver.findElements(additionValueCellLocator).size() == 1
            );

            // Locate the current cell after the grid has stabilized.
            WebElement additionValueCell =
                    driver.findElement(additionValueCellLocator);

            System.out.println(
                    "Addition Value before editing: ["
                            + additionValueCell.getText().trim() + "]"
            );

            // Scroll the target cell into view before activating its editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    additionValueCell
            );

            /*
             * Activate the inline editor using the existing framework helper.
             * Skip the operation if the cell is disabled or cannot be activated.
             */
            boolean editorActivated = tryActivateGridCellEditor(
                    additionValueCellLocator,
                    additionValueInputLocator
            );

            if (!editorActivated) {
                System.out.println(
                        "Addition Value is disabled or non-editable. Skipping."
                );
                System.out.println("========================================");
                return;
            }

            System.out.println("Addition Value cell activated successfully.");

            // Re-locate the input after activation to avoid using a stale reference.
            WebElement additionValueInput = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            additionValueInputLocator
                    )
            );

            /*
             * Set the value using the native HTML input setter.
             * Dispatch input/change events so the application can detect the edit.
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
                    additionValueInput,
                    additionValue.trim()
            );

            // Verify the value entered in the editor before committing.
            String editorValue = additionValueInput.getDomProperty("value");

            System.out.println(
                    "Addition Value editor before commit: ["
                            + editorValue + "]"
            );

            if (editorValue == null
                    || new BigDecimal(editorValue.trim())
                    .compareTo(expectedValue) != 0) {

                throw new IllegalStateException(
                        "Addition Value editor mismatch. Expected: ["
                                + additionValue
                                + "], Actual: ["
                                + editorValue + "]"
                );
            }

            // Commit the edited value with TAB.
            additionValueInput.sendKeys(Keys.TAB);

            System.out.println("TAB sent to commit Addition Value.");

            /*
             * Wait until the editor is no longer visible.
             * Re-query the DOM on each poll because AG Grid may replace the editor.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> editors =
                            currentDriver.findElements(additionValueInputLocator);

                    for (WebElement editor : editors) {
                        try {
                            if (editor.isDisplayed()) {
                                return false;
                            }
                        } catch (StaleElementReferenceException e) {
                            // AG Grid replaced the editor; retry on the next poll.
                            return false;
                        }
                    }

                    return true;

                } catch (StaleElementReferenceException e) {
                    // Retry while the grid is refreshing.
                    return false;
                }
            });

            /*
             * Wait for the committed cell value to match the requested value.
             * Locate the cell afresh during every poll to handle stale references.
             */
            wait.until(currentDriver -> {
                try {
                    List<WebElement> cells =
                            currentDriver.findElements(additionValueCellLocator);

                    // Do not validate an ambiguous cell when multiple rows exist.
                    if (cells.size() != 1) {
                        return false;
                    }

                    String displayedValue = cells.get(0).getText().trim();

                    String normalizedValue = displayedValue
                            .replace(",", "")
                            .replace("%", "")
                            .replace("[", "")
                            .replace("]", "")
                            .trim();

                    if (normalizedValue.isEmpty()) {
                        return false;
                    }

                    BigDecimal actualValue = new BigDecimal(normalizedValue);

                    return expectedValue.compareTo(actualValue) == 0;

                } catch (StaleElementReferenceException
                         | NoSuchElementException
                         | NumberFormatException e) {
                    // Retry until the grid displays the committed value.
                    return false;
                }
            });

            // Read the final displayed value using a fresh element reference.
            String committedValue =
                    driver.findElement(additionValueCellLocator).getText().trim();

            System.out.println(
                    "Addition Value entered successfully: ["
                            + committedValue + "]"
            );
            System.out.println("========================================");

        } catch (TimeoutException e) {
            throw new IllegalStateException(
                    "Timed out while entering or verifying Addition Value ["
                            + additionValue
                            + "]. Check that exactly one matching cell exists "
                            + "and that it is editable.",
                    e
            );
        }
    }

    /**
     * Selects a random actual Currency in the Discounts & Additions grid.
     *
     * Uses the stable AG Grid column identifier col-id='currencyId'.
     * Does not use data-testid, row-index, row-id, generated IDs,
     * or partially dynamic locator values.
     *
     * Handles stale elements by re-locating cells and options during waits.
     * Excludes empty options and the placeholder "اختر".
     * Verifies that the selected Currency is committed to the grid cell.
     *
     * Important: The locator must match exactly one Currency cell.
     * If multiple rows contain this column, the method fails safely
     * rather than selecting a Currency in an ambiguous row.
     */
    public void selectRandomCurrencyInDiscountsSection() {

        System.out.println("========================================");
        System.out.println("Selecting random Currency in Discounts & Additions");

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the Currency cell using the stable AG Grid column identifier only.
        By currencyCellLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] " +
                        "> div[role='row'] " +
                        "> div[role='gridcell'][col-id='currencyId']"
        );

        // Locate the React Select input inside the Currency cell editor.
        By currencyInputLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] " +
                        "> div[role='row'] " +
                        "> div[role='gridcell'][col-id='currencyId'] input"
        );

        // Locate the currently displayed React Select listbox without generated IDs.
        By currencyListboxLocator = By.cssSelector(
                "div[role='listbox']"
        );

        // Locate selectable Currency options inside the visible listbox.
        By currencyOptionLocator = By.cssSelector(
                "div[role='listbox'] [role='option']"
        );

        try {

            // Wait until exactly one Currency cell is visible to avoid editing an ambiguous row.
            WebElement currencyCell = wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(currencyCellLocator);

                List<WebElement> visibleCells = cells.stream()
                        .filter(cell -> {
                            try {
                                return cell.isDisplayed();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        })
                        .toList();

                if (visibleCells.size() > 1) {
                    throw new IllegalStateException(
                            "Cannot select Currency safely: found " +
                                    visibleCells.size() +
                                    " Currency cells. A stable row identifier is required."
                    );
                }

                return visibleCells.size() == 1 ? visibleCells.get(0) : null;
            });

            System.out.println(
                    "Currency cell found. Current value: [" +
                            currencyCell.getText().trim() + "]"
            );

            // Scroll the located Currency cell into view before activating its editor.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'center'});",
                    currencyCell
            );

            // Re-find the Currency cell after scrolling to avoid using a stale reference.
            currencyCell = wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(currencyCellLocator);

                if (cells.size() != 1) {
                    return null;
                }

                try {
                    WebElement cell = cells.get(0);
                    return cell.isDisplayed() && cell.isEnabled() ? cell : null;
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            });

            // Double-click the Currency cell to activate its inline editor.
            new Actions(driver)
                    .doubleClick(currencyCell)
                    .perform();

            System.out.println("Double-click sent to Currency cell.");

            // Wait until the React Select input becomes visible inside the Currency cell.
            wait.until(currentDriver -> {

                List<WebElement> inputs =
                        currentDriver.findElements(currencyInputLocator);

                if (inputs.size() != 1) {
                    return false;
                }

                try {
                    return inputs.get(0).isDisplayed();
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            // Re-find and click the input to open the available Currency options.
            wait.until(currentDriver -> {

                List<WebElement> inputs =
                        currentDriver.findElements(currencyInputLocator);

                if (inputs.size() != 1) {
                    return false;
                }

                try {
                    WebElement input = inputs.get(0);

                    if (input.isDisplayed() && input.isEnabled()) {
                        input.click();
                        return true;
                    }

                    return false;

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println("Currency dropdown opening...");

            // Wait until the Currency listbox is visible.
            wait.until(currentDriver -> {

                List<WebElement> listboxes =
                        currentDriver.findElements(currencyListboxLocator);

                for (WebElement listbox : listboxes) {
                    try {
                        if (listbox.isDisplayed()) {
                            return true;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-locate the listbox during the next polling attempt.
                    }
                }

                return false;
            });

            System.out.println("Currency dropdown opened.");

            // Wait for actual Currency options and exclude placeholders and disabled options.
            List<WebElement> validOptions = wait.until(currentDriver -> {

                List<WebElement> options =
                        currentDriver.findElements(currencyOptionLocator);

                List<WebElement> valid = new ArrayList<>();

                for (WebElement option : options) {
                    try {
                        String text = option.getText().trim();
                        String disabled = option.getAttribute("aria-disabled");

                        if (option.isDisplayed()
                                && option.isEnabled()
                                && !"true".equalsIgnoreCase(disabled)
                                && !text.isEmpty()
                                && !"اختر".equals(text)) {

                            valid.add(option);
                        }

                    } catch (StaleElementReferenceException ignored) {
                        // Skip stale option references and re-query on the next wait cycle.
                    }
                }

                return valid.isEmpty() ? null : valid;
            });

            System.out.println(
                    "Number of selectable Currency options: " + validOptions.size()
            );

            // Choose one valid Currency randomly and retain its visible text.
            Random random = new Random();

            WebElement randomCurrency = validOptions.get(
                    random.nextInt(validOptions.size())
            );

            String selectedCurrency = randomCurrency.getText().trim();

            System.out.println(
                    "Random Currency selected: [" + selectedCurrency + "]"
            );

            // Re-locate the option by its exact visible text to avoid using a stale option reference.
            By selectedCurrencyOptionLocator = By.xpath(
                    "//div[@role='listbox']" +
                            "//*[@role='option' and normalize-space(.)=" +
                            toXPathLiteral(selectedCurrency) + "]"
            );

            // Click the freshly located matching option, retrying if React re-renders the list.
            wait.until(currentDriver -> {

                List<WebElement> matchingOptions =
                        currentDriver.findElements(selectedCurrencyOptionLocator);

                for (WebElement option : matchingOptions) {
                    try {
                        if (option.isDisplayed() && option.isEnabled()) {
                            option.click();
                            return true;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-locate the option during the next polling attempt.
                    }
                }

                return false;
            });

            System.out.println("Currency option clicked.");

            // Wait until the Currency dropdown is closed.
            wait.until(currentDriver -> {

                List<WebElement> listboxes =
                        currentDriver.findElements(currencyListboxLocator);

                for (WebElement listbox : listboxes) {
                    try {
                        if (listbox.isDisplayed()) {
                            return false;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // A stale listbox may have been replaced during closing.
                    }
                }

                return true;
            });

            // Wait until the selected Currency text is committed to the grid cell.
            wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(currencyCellLocator);

                if (cells.size() != 1) {
                    return false;
                }

                try {
                    return cells.get(0).isDisplayed()
                            && selectedCurrency.equals(
                            cells.get(0).getText().trim()
                    );
                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            // Re-read the committed value from a freshly located Currency cell.
            WebElement committedCell = wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(currencyCellLocator);

                if (cells.size() != 1) {
                    return null;
                }

                try {
                    WebElement cell = cells.get(0);

                    if (cell.isDisplayed()
                            && selectedCurrency.equals(cell.getText().trim())) {
                        return cell;
                    }

                    return null;

                } catch (StaleElementReferenceException e) {
                    return null;
                }
            });

            String committedCurrency = committedCell.getText().trim();

            // Fail clearly if the selected Currency was not committed correctly.
            if (!selectedCurrency.equals(committedCurrency)) {
                throw new IllegalStateException(
                        "Currency selection was not committed correctly. " +
                                "Expected: [" + selectedCurrency + "], " +
                                "Actual: [" + committedCurrency + "]"
                );
            }

            System.out.println(
                    "Currency selected and verified successfully: [" +
                            committedCurrency + "]"
            );

        } catch (TimeoutException e) {

            throw new RuntimeException(
                    "Failed to select and verify a random Currency in Discounts & Additions. " +
                            "Check whether the Currency cell editor and options are displayed, " +
                            "and whether the cell locator matches exactly one row.",
                    e
            );

        } finally {
            System.out.println("========================================");
        }
    }

    /**
     * Enters and commits the requested Note in the Discounts & Additions AG Grid.
     *
     * Uses the stable AG Grid column identifier col-id='note'.
     * Does not use data-testid, row-index, row-id, generated IDs,
     * or partially dynamic locator values.
     *
     * Attempts to activate the editor using double-click, then F2.
     * Re-locates elements during waits to reduce stale-element failures.
     * Commits the note by switching tabs and verifies the saved text.
     *
     * Important: The Note locator must match exactly one cell.
     *
     * @param note the text to enter into the Note cell
     */
    public void enterDiscountsAdditionsNote(String note) {

        if (note == null) {
            throw new IllegalArgumentException("Note must not be null.");
        }

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        System.out.println("========================================");
        System.out.println("Entering Discounts & Additions Note.");
        System.out.println("Requested note: [" + note + "]");

        // Locate the Note cell through the stable AG Grid column identifier only.
        By noteCellLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] " +
                        "> div[role='row'] " +
                        "> div[role='gridcell'][col-id='note']"
        );

        // Locate the inline editor inside the Note cell without generated IDs.
        By noteEditorLocator = By.cssSelector(
                "div.ag-center-cols-container[role='rowgroup'] " +
                        "> div[role='row'] " +
                        "> div[role='gridcell'][col-id='note'] input, " +
                        "div.ag-center-cols-container[role='rowgroup'] " +
                        "> div[role='row'] " +
                        "> div[role='gridcell'][col-id='note'] textarea"
        );

        try {

            // Wait for exactly one visible Note cell to avoid editing an ambiguous row.
            WebElement noteCell = wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(noteCellLocator);

                List<WebElement> visibleCells = new ArrayList<>();

                for (WebElement cell : cells) {
                    try {
                        if (cell.isDisplayed()) {
                            visibleCells.add(cell);
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-query the cell on the next polling attempt.
                    }
                }

                if (visibleCells.size() > 1) {
                    throw new IllegalStateException(
                            "Cannot enter Note safely: found " +
                                    visibleCells.size() +
                                    " Note cells. A stable row identifier is required."
                    );
                }

                return visibleCells.size() == 1
                        ? visibleCells.get(0)
                        : null;
            });

            System.out.println(
                    "Note cell found. Current value: [" +
                            noteCell.getText().trim() + "]"
            );

            // Scroll the Note cell into view before activating inline editing.
            ((JavascriptExecutor) driver).executeScript(
                    "arguments[0].scrollIntoView({block:'center', inline:'nearest'});",
                    noteCell
            );

            // Re-find the cell after scrolling to avoid using a stale reference.
            noteCell = wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(noteCellLocator);

                if (cells.size() != 1) {
                    return null;
                }

                try {
                    WebElement cell = cells.get(0);
                    return cell.isDisplayed() && cell.isEnabled()
                            ? cell
                            : null;
                } catch (StaleElementReferenceException e) {
                    return null;
                }
            });

            // First attempt: activate the Note editor with a double-click.
            new Actions(driver)
                    .doubleClick(noteCell)
                    .perform();

            System.out.println("Double-click performed on the Note cell.");

            // Wait briefly for an enabled input or textarea editor to appear.
            WebElement noteEditor = null;

            try {
                noteEditor = new WebDriverWait(
                        driver,
                        Duration.ofSeconds(3)
                ).until(currentDriver -> {

                    List<WebElement> editors =
                            currentDriver.findElements(noteEditorLocator);

                    for (WebElement editor : editors) {
                        try {
                            if (editor.isDisplayed() && editor.isEnabled()) {
                                return editor;
                            }
                        } catch (StaleElementReferenceException ignored) {
                            // Re-query the editor during the next polling attempt.
                        }
                    }

                    return null;
                });

            } catch (TimeoutException e) {
                System.out.println(
                        "Editor did not appear after double-click. Trying F2."
                );
            }

            // Second attempt: press F2 if double-click did not activate editing.
            if (noteEditor == null) {

                WebElement currentCell = wait.until(currentDriver -> {

                    List<WebElement> cells =
                            currentDriver.findElements(noteCellLocator);

                    if (cells.size() != 1) {
                        return null;
                    }

                    try {
                        WebElement cell = cells.get(0);
                        return cell.isDisplayed() && cell.isEnabled()
                                ? cell
                                : null;
                    } catch (StaleElementReferenceException e) {
                        return null;
                    }
                });

                currentCell.sendKeys(Keys.F2);

                try {
                    noteEditor = new WebDriverWait(
                            driver,
                            Duration.ofSeconds(3)
                    ).until(currentDriver -> {

                        List<WebElement> editors =
                                currentDriver.findElements(noteEditorLocator);

                        for (WebElement editor : editors) {
                            try {
                                if (editor.isDisplayed() && editor.isEnabled()) {
                                    return editor;
                                }
                            } catch (StaleElementReferenceException ignored) {
                                // Re-query the editor on the next polling attempt.
                            }
                        }

                        return null;
                    });

                } catch (TimeoutException e) {

                    // Print the current Note cell HTML to diagnose the editor structure.
                    try {
                        List<WebElement> cells =
                                driver.findElements(noteCellLocator);

                        if (cells.size() == 1) {
                            System.out.println(
                                    "NOTE CELL HTML DIAGNOSTICS:\n" +
                                            cells.get(0).getAttribute("outerHTML")
                            );
                        } else {
                            System.out.println(
                                    "Cannot print Note cell HTML: expected one cell, found " +
                                            cells.size()
                            );
                        }

                    } catch (StaleElementReferenceException diagnosticException) {
                        System.out.println(
                                "The Note cell changed during HTML diagnostics."
                        );
                    }

                    throw new IllegalStateException(
                            "Could not open the Note editor. " +
                                    "Neither double-click nor F2 activated editing.",
                            e
                    );
                }
            }

            System.out.println("Note editor opened successfully.");

            // Re-find the visible editor before replacing its content.
            WebElement currentEditor = wait.until(currentDriver -> {

                List<WebElement> editors =
                        currentDriver.findElements(noteEditorLocator);

                for (WebElement editor : editors) {
                    try {
                        if (editor.isDisplayed() && editor.isEnabled()) {
                            return editor;
                        }
                    } catch (StaleElementReferenceException ignored) {
                        // Re-query the editor during the next polling attempt.
                    }
                }

                return null;
            });

            // Replace the existing editor value with the requested Note.
            currentEditor.click();
            currentEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
            currentEditor.sendKeys(note);

            // Verify the requested text is present in the active editor.
            wait.until(currentDriver -> {

                List<WebElement> editors =
                        currentDriver.findElements(noteEditorLocator);

                for (WebElement editor : editors) {
                    try {
                        if (!editor.isDisplayed()) {
                            continue;
                        }

                        String editorValue = editor.getAttribute("value");

                        if (note.equals(editorValue)) {
                            return true;
                        }

                    } catch (StaleElementReferenceException ignored) {
                        // Re-query the editor during the next polling attempt.
                    }
                }

                return false;
            });

            System.out.println("Note editor value verified: [" + note + "]");

            // Locate the Payment Scheduling tab using its semantic tab relationship.
            By paymentSchedulingTabLocator = By.cssSelector(
                    "button[role='tab'][aria-controls$='content-paymentsScheduling']"
            );

            // Click the Payment Scheduling tab to commit the Note edit.
            wait.until(ExpectedConditions.elementToBeClickable(
                    paymentSchedulingTabLocator
            )).click();

            System.out.println(
                    "Clicked the Payment Scheduling tab to commit the Note."
            );

            // Locate the Discounts & Additions tab by its visible Arabic label.
            By discountsAdditionsTabLocator = By.xpath(
                    "//button[@role='tab' and " +
                            "contains(normalize-space(.), 'الخصومات والإضافات')]"
            );

            // Return to the Discounts & Additions tab after committing the edit.
            wait.until(ExpectedConditions.elementToBeClickable(
                    discountsAdditionsTabLocator
            )).click();

            System.out.println("Returned to the Discounts & Additions tab.");

            // Verify the committed Note using a freshly located cell.
            wait.until(currentDriver -> {

                List<WebElement> cells =
                        currentDriver.findElements(noteCellLocator);

                if (cells.size() != 1) {
                    return false;
                }

                try {
                    WebElement cell = cells.get(0);

                    return cell.isDisplayed()
                            && note.equals(cell.getText().trim());

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println(
                    "Discounts & Additions Note committed successfully: [" +
                            note + "]"
            );

        } catch (TimeoutException e) {

            throw new RuntimeException(
                    "Failed to enter and verify the Discounts & Additions Note: [" +
                            note + "]. Check the cell locator, editor activation, and tab locators.",
                    e
            );

        } finally {
            System.out.println("========================================");
        }
    }


    /**
     * Opens the Payments Scheduling (جدولة الدفعات) tab.
     *
     * Uses the stable visible Arabic tab label instead of generated Radix IDs
     * or dynamically generated attributes.
     * Re-locates the tab during waits to reduce stale element issues.
     * Verifies that the tab is selected after clicking.
     */
    public void openPaymentsSchedulingTab() {

        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(10));

        // Locate the tab by its stable, visible Arabic label.
        By paymentsSchedulingTabLocator = By.xpath(
                "//button[@role='tab' and .//div[normalize-space(.)='جدولة الدفعات']]"
        );

        // Wait until the tab is visible and enabled, re-finding it on each poll.
        wait.until(webDriver -> {
            try {
                List<WebElement> tabs =
                        webDriver.findElements(paymentsSchedulingTabLocator);

                return tabs.size() == 1
                        && tabs.get(0).isDisplayed()
                        && tabs.get(0).isEnabled();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Click the freshly located tab, retrying if the DOM refreshes.
        boolean clicked = false;

        for (int attempt = 1; attempt <= 3 && !clicked; attempt++) {
            try {
                WebElement tab = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                paymentsSchedulingTabLocator
                        )
                );

                tab.click();
                clicked = true;

            } catch (StaleElementReferenceException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }

        // Verify selection by re-locating the tab using the same stable locator.
        wait.until(webDriver -> {
            try {
                List<WebElement> tabs =
                        webDriver.findElements(paymentsSchedulingTabLocator);

                return tabs.size() == 1
                        && "true".equals(
                        tabs.get(0).getAttribute("aria-selected")
                );
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        System.out.println("تم فتح تبويب جدولة الدفعات بنجاح.");
    }


    /**
     * Randomly selects a Payment Terms Template and verifies the selected value.
     *
     * Uses the stable input ID and semantic role attributes.
     * Avoids generated React IDs, dynamic row indexes, and generated CSS classes.
     * Re-finds elements during waits to reduce stale element issues.
     */
    public void selectRandomPaymentTermsTemplate() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Payment Terms Template input using its stable HTML ID.
        By paymentTermsTemplateInputLocator =
                By.id("paymentTermsTemplatesId");

        // Locate dropdown options through their semantic role.
        By paymentTermsOptionsLocator =
                By.cssSelector("[role='option']");

        // Locate the selected value relative to the stable input ID.
        By selectedTemplateValueLocator = By.xpath(
                "//input[@id='paymentTermsTemplatesId']" +
                        "/ancestor::div[contains(@class,'select__control')]" +
                        "//*[@class='select__single-value' " +
                        "or contains(concat(' ', normalize-space(@class), ' '), ' select__single-value ')]"
        );

        // Wait until the input is visible and enabled, re-finding it on each poll.
        wait.until(webDriver -> {
            try {
                List<WebElement> inputs =
                        webDriver.findElements(paymentTermsTemplateInputLocator);

                return inputs.size() == 1
                        && inputs.get(0).isDisplayed()
                        && inputs.get(0).isEnabled();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Open the dropdown using a freshly located input.
        clickPaymentTermsTemplateInput(
                wait,
                paymentTermsTemplateInputLocator
        );

        // Wait until at least one selectable option is visible.
        wait.until(webDriver -> {
            try {
                return webDriver.findElements(paymentTermsOptionsLocator)
                        .stream()
                        .anyMatch(option -> {
                            try {
                                return option.isDisplayed()
                                        && option.isEnabled()
                                        && !"true".equals(
                                        option.getAttribute("aria-disabled")
                                )
                                        && !option.getText().trim().isEmpty();
                            } catch (StaleElementReferenceException e) {
                                return false;
                            }
                        });
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Collect the visible, enabled options and store their text rather than
        // retaining WebElement references that may become stale.
        List<String> availableTemplateNames = driver
                .findElements(paymentTermsOptionsLocator)
                .stream()
                .filter(option -> {
                    try {
                        return option.isDisplayed()
                                && option.isEnabled()
                                && !"true".equals(
                                option.getAttribute("aria-disabled")
                        )
                                && !option.getText().trim().isEmpty();
                    } catch (StaleElementReferenceException e) {
                        return false;
                    }
                })
                .map(option -> option.getText().trim())
                .distinct()
                .toList();

        // Ensure that the dropdown contains at least one selectable template.
        if (availableTemplateNames.isEmpty()) {
            throw new NoSuchElementException(
                    "No selectable Payment Terms Templates were found."
            );
        }

        // Select a random template name.
        String expectedTemplate = availableTemplateNames.get(
                ThreadLocalRandom.current().nextInt(
                        availableTemplateNames.size()
                )
        );

        // Build an exact-text XPath for the chosen option.
        By selectedOptionLocator = By.xpath(
                "//*[@role='option' and normalize-space(.)=" +
                        toXPathLiteral(expectedTemplate) + "]"
        );

        // Click the freshly located option, retrying if React refreshes the DOM.
        boolean optionClicked = false;

        for (int attempt = 1; attempt <= 3 && !optionClicked; attempt++) {
            try {
                WebElement selectedOption = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedOptionLocator
                        )
                );

                selectedOption.click();
                optionClicked = true;

            } catch (StaleElementReferenceException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }

        // Wait for the dropdown selection to appear in the control.
        wait.until(webDriver -> {
            try {
                List<WebElement> selectedValues =
                        webDriver.findElements(selectedTemplateValueLocator);

                return selectedValues.size() == 1
                        && selectedValues.get(0).isDisplayed()
                        && expectedTemplate.equals(
                        selectedValues.get(0).getText().trim()
                );
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Read the final displayed value from a freshly located element.
        String displayedTemplate = driver
                .findElement(selectedTemplateValueLocator)
                .getText()
                .trim();

        // Verify the selected template.
        if (!expectedTemplate.equals(displayedTemplate)) {
            throw new AssertionError(
                    "Payment Terms Template verification failed. Expected: [" +
                            expectedTemplate +
                            "], Actual: [" +
                            displayedTemplate +
                            "]"
            );
        }

        System.out.println(
                "Payment Terms Template selected and verified successfully: " +
                        displayedTemplate
        );
    }

    /**
     * Clicks the Payment Terms Template input after re-locating it.
     * Retries when React replaces the input during interaction.
     */
    private void clickPaymentTermsTemplateInput(
            WebDriverWait wait,
            By inputLocator
    ) {
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                WebElement input = wait.until(
                        ExpectedConditions.elementToBeClickable(inputLocator)
                );

                input.click();
                return;

            } catch (StaleElementReferenceException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }
    }


    /**
     * Enters and verifies the Notes (البيان) field in the Purchase Return Invoice.
     *
     * Uses the stable HTML ID "notes" without relying on generated IDs,
     * dynamic attributes, or CSS class combinations.
     * Re-locates the input during waits and retries stale-element interactions.
     *
     * @param notes the text to enter into the Notes field
     */
    public void enterInvoiceNotes(String notes) {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Notes field using its stable HTML ID.
        By notesLocator = By.id("notes");

        // Reject null input to avoid silently entering an unintended value.
        if (notes == null) {
            throw new IllegalArgumentException(
                    "Notes value must not be null."
            );
        }

        // Wait until exactly one matching field is visible and enabled.
        wait.until(webDriver -> {
            try {
                List<WebElement> fields =
                        webDriver.findElements(notesLocator);

                return fields.size() == 1
                        && fields.get(0).isDisplayed()
                        && fields.get(0).isEnabled();
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Clear the field, enter the requested text, and retry if React
        // replaces the input during the interaction.
        boolean valueEntered = false;

        for (int attempt = 1; attempt <= 3 && !valueEntered; attempt++) {
            try {
                WebElement notesField = wait.until(
                        ExpectedConditions.elementToBeClickable(notesLocator)
                );

                notesField.clear();
                notesField.sendKeys(notes);

                valueEntered = true;

            } catch (StaleElementReferenceException e) {
                if (attempt == 3) {
                    throw e;
                }
            }
        }

        // Verify the final input value by re-locating the element.
        wait.until(webDriver -> {
            try {
                List<WebElement> fields =
                        webDriver.findElements(notesLocator);

                return fields.size() == 1
                        && notes.equals(fields.get(0).getAttribute("value"));
            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Read the verified value from the current DOM element.
        String actualNotes = driver.findElement(notesLocator)
                .getAttribute("value");

        // Fail explicitly if the entered text does not match the expected value.
        if (!notes.equals(actualNotes)) {
            throw new AssertionError(
                    "Notes field verification failed. Expected: [" +
                            notes +
                            "], Actual: [" +
                            actualNotes +
                            "]"
            );
        }

        System.out.println(
                "تم تعبئة حقل البيان والتحقق منه بنجاح: " + actualNotes
        );
    }

    /**
     * Enters and verifies a unique random Vendor Invoice Number.
     *
     * Uses the stable HTML ID "vendorInvoiceCode".
     * Scrolls the field into the visible viewport before interaction.
     * Re-locates the field during waits to reduce stale-element issues.
     * Retries intercepted clicks without relying on dynamic attributes.
     *
     * @return the generated vendor invoice number
     */
    public String enterUniqueRandomVendorInvoiceNumber() {

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        // Stable locator for the Vendor Invoice Number input.
        By vendorInvoiceCodeLocator = By.id("vendorInvoiceCode");

        // Generate a practically unique invoice number.
        String vendorInvoiceNumber = "VIN-" +
                UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 16)
                        .toUpperCase(Locale.ROOT);

        // Wait until exactly one visible and enabled input is available.
        wait.until(webDriver -> {
            try {
                List<WebElement> fields =
                        webDriver.findElements(vendorInvoiceCodeLocator);

                return fields.size() == 1
                        && fields.get(0).isDisplayed()
                        && fields.get(0).isEnabled();

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Scroll the freshly located input into the center of the viewport.
        wait.until(webDriver -> {
            try {
                List<WebElement> fields =
                        webDriver.findElements(vendorInvoiceCodeLocator);

                if (fields.size() != 1) {
                    return false;
                }

                WebElement field = fields.get(0);

                if (!field.isDisplayed() || !field.isEnabled()) {
                    return false;
                }

                ((JavascriptExecutor) webDriver).executeScript(
                        "arguments[0].scrollIntoView({" +
                                "behavior:'instant'," +
                                "block:'center'," +
                                "inline:'nearest'" +
                                "});",
                        field
                );

                return true;

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Allow the page layout to settle after scrolling, then interact
        // with the field using Selenium's native click and keyboard actions.
        boolean valueEntered = false;

        for (int attempt = 1; attempt <= 3 && !valueEntered; attempt++) {

            try {
                WebElement input = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                vendorInvoiceCodeLocator
                        )
                );

                // Re-center the current element before each click attempt.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({" +
                                "behavior:'instant'," +
                                "block:'center'," +
                                "inline:'nearest'" +
                                "});",
                        input
                );

                // Use a real WebDriver click rather than JavaScript click.
                input = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                vendorInvoiceCodeLocator
                        )
                );

                input.click();

                // Replace any existing content with the generated number.
                input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
                input.sendKeys(vendorInvoiceNumber);

                valueEntered = true;

            } catch (StaleElementReferenceException |
                     ElementClickInterceptedException e) {

                if (attempt == 3) {
                    throw new RuntimeException(
                            "Could not interact with the Vendor Invoice Number " +
                                    "field after 3 attempts. Check whether another " +
                                    "element or a fixed footer is covering the input.",
                            e
                    );
                }

                // Re-locate and scroll again on the next attempt.
                wait.until(webDriver -> {
                    try {
                        List<WebElement> fields =
                                webDriver.findElements(vendorInvoiceCodeLocator);

                        if (fields.size() != 1
                                || !fields.get(0).isDisplayed()
                                || !fields.get(0).isEnabled()) {
                            return false;
                        }

                        ((JavascriptExecutor) webDriver).executeScript(
                                "arguments[0].scrollIntoView({" +
                                        "behavior:'instant'," +
                                        "block:'center'," +
                                        "inline:'nearest'" +
                                        "});",
                                fields.get(0)
                        );

                        return true;

                    } catch (StaleElementReferenceException ex) {
                        return false;
                    }
                });
            }
        }

        // Verify the value by querying the current DOM element on each poll.
        wait.until(webDriver -> {
            try {
                List<WebElement> fields =
                        webDriver.findElements(vendorInvoiceCodeLocator);

                return fields.size() == 1
                        && vendorInvoiceNumber.equals(
                        fields.get(0).getAttribute("value")
                );

            } catch (StaleElementReferenceException e) {
                return false;
            }
        });

        // Read the final value from the current input element.
        String actualValue = driver.findElement(
                vendorInvoiceCodeLocator
        ).getAttribute("value");

        // Fail explicitly if the entered value does not match the expected value.
        if (!vendorInvoiceNumber.equals(actualValue)) {
            throw new AssertionError(
                    "Vendor Invoice Number verification failed. Expected: [" +
                            vendorInvoiceNumber +
                            "], Actual: [" +
                            actualValue +
                            "]"
            );
        }

        System.out.println(
                "Vendor Invoice Number entered and verified successfully: [" +
                        actualValue + "]"
        );

        return actualValue;
    }

    /**
     * Clicks the Save button to save the Purchase Return Invoice.
     *
     * Uses the stable HTML ID "save" instead of dynamic attributes,
     * generated IDs, or CSS class combinations.
     * Scrolls the button into view and re-locates it before clicking
     * to reduce the risk of using a stale WebElement.
     * Retries when the click is intercepted or the element becomes stale.
     */
    public void clickSavePurchaseReturnInvoice() {

        // Locate the Save button using its stable HTML ID.
        By saveButtonLocator = By.id("save");

        // Wait for the Save button to become visible and enabled.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        // Retry the interaction if the DOM changes or another element
        // temporarily intercepts the click.
        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                // Re-locate the button and wait until it is visible.
                WebElement saveButton = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                saveButtonLocator
                        )
                );

                // Ensure the located element is enabled before proceeding.
                wait.until(webDriver -> {
                    try {
                        List<WebElement> buttons =
                                webDriver.findElements(saveButtonLocator);

                        return buttons.size() == 1
                                && buttons.get(0).isDisplayed()
                                && buttons.get(0).isEnabled();

                    } catch (StaleElementReferenceException e) {
                        return false;
                    }
                });

                // Scroll the current Save button into the center of the viewport.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({" +
                                "behavior:'instant'," +
                                "block:'center'," +
                                "inline:'nearest'" +
                                "});",
                        saveButton
                );

                // Re-locate the button after scrolling to avoid stale references.
                saveButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                saveButtonLocator
                        )
                );

                // Click the Save button using Selenium's native click.
                saveButton.click();

                System.out.println(
                        "Save button clicked successfully for the Purchase Return Invoice."
                );

                return;

            } catch (StaleElementReferenceException |
                     ElementClickInterceptedException e) {

                // Retry with a freshly located element; fail after the last attempt.
                if (attempt == 3) {
                    throw new RuntimeException(
                            "Failed to click the Purchase Return Invoice Save button " +
                                    "after 3 attempts. Check whether another element " +
                                    "is covering the button.",
                            e
                    );
                }
            }
        }
    }

    /**
     * Confirms saving the Purchase Return Invoice by clicking the "نعم" button
     * in the save confirmation dialog.
     *
     * Uses stable HTML attributes (data-modal-agree and type='submit')
     * instead of the dynamic data-testid or generated CSS classes.
     * Waits for the button to become clickable, scrolls it into view,
     * and re-locates it before clicking to reduce stale element issues.
     * Retries the click if the element becomes stale or the click is intercepted.
     */
    public void confirmSavePurchaseReturnInvoice() {

        // Locate the confirmation button using stable HTML attributes.
        By confirmSaveButtonLocator = By.cssSelector(
                "button[data-modal-agree][type='submit']"
        );

        // Wait up to 15 seconds for the confirmation button.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(15)
        );

        // Retry the interaction if the DOM changes or another element intercepts the click.
        for (int attempt = 1; attempt <= 3; attempt++) {

            try {
                // Wait until the confirmation button is visible.
                WebElement confirmSaveButton = wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                confirmSaveButtonLocator
                        )
                );

                // Scroll the confirmation button into the center of the viewport.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({" +
                                "behavior:'instant'," +
                                "block:'center'," +
                                "inline:'nearest'" +
                                "});",
                        confirmSaveButton
                );

                // Re-locate the button after scrolling to avoid reusing a stale WebElement.
                confirmSaveButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                confirmSaveButtonLocator
                        )
                );

                // Confirm that exactly one matching button is present before clicking.
                List<WebElement> matchingButtons =
                        driver.findElements(confirmSaveButtonLocator);

                if (matchingButtons.size() != 1) {
                    throw new IllegalStateException(
                            "Expected exactly one Purchase Return Invoice confirmation button, " +
                                    "but found " + matchingButtons.size() + "."
                    );
                }

                // Re-locate once more immediately before the native Selenium click.
                confirmSaveButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                confirmSaveButtonLocator
                        )
                );

                // Click "نعم" to confirm saving the Purchase Return Invoice.
                confirmSaveButton.click();

                System.out.println(
                        "Clicked 'نعم' to confirm saving the Purchase Return Invoice."
                );

                return;

            } catch (StaleElementReferenceException |
                     ElementClickInterceptedException e) {

                // Retry with a freshly located element; propagate the error after the final attempt.
                if (attempt == 3) {
                    throw new RuntimeException(
                            "Failed to confirm saving the Purchase Return Invoice " +
                                    "after 3 attempts. Check whether the confirmation dialog " +
                                    "is visible and whether another element is intercepting the click.",
                            e
                    );
                }
            }
        }
    }




}
