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
import org.openqa.selenium.By;
import org.openqa.selenium.StaleElementReferenceException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebElement;
import java.util.List;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebDriver;


public class FreeQuantityProcessingPage extends BasePage {

    public FreeQuantityProcessingPage(WebDriver driver) {
        super(driver);
    }

    // Stores the stable test ID of the newly added Item cell.
    private String newItemTestId;

    // Stores the logical AG Grid row index; -1 means no row has been captured.
    private int newRowIndex = -1;
    /**
     * Clicks the New button on the Free Quantity Processing screen
     * to open the new free quantity processing form.
     *
     * Identifies the button using its visible Arabic label and the
     * stable prefix of its data-testid, avoiding the dynamically generated
     * portion of the test ID and unrelated buttons.
     */
    public void clickNew() {

        // Locate the New button using the stable data-testid prefix
        // and its exact visible Arabic label "جديد".
        By newButtonLocator = By.xpath(
                "//button[starts-with(@data-testid,'process-free-qty-')" +
                        " and .//span[normalize-space()='جديد']]"
        );

        // Wait until the matching New button becomes clickable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement newButton = wait.until(
                ExpectedConditions.elementToBeClickable(newButtonLocator)
        );

        // Click the New button to open the creation form.
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
     * Attempts to update the Free Quantity Processing operation date
     * to one month from today using the native input setter.
     *
     * Uses the stable data-testid container and dispatches bubbling
     * input and change events so the application can detect the update.
     * Verifies the final input value and reports a clear failure if the
     * application restores the original date.
     */
    public void enterOperationDateOneMonthFromNowDirectly() {

        // Locate the operation date input through its stable test identifier.
        By operationDateInput = By.cssSelector(
                "[data-testid='process-free-qty-operationDate'] input"
        );

        // Calculate the required date in the application's ISO format.
        String expectedDate = LocalDate.now()
                .plusMonths(1)
                .format(DateTimeFormatter.ISO_LOCAL_DATE);

        // Wait for the operation date field to become visible.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        WebElement dateField = wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        operationDateInput
                )
        );

        // Use the native HTML input value setter before dispatching events.
        JavascriptExecutor js = (JavascriptExecutor) driver;

        js.executeScript(
                "const input = arguments[0];" +
                        "const value = arguments[1];" +
                        "const setter = Object.getOwnPropertyDescriptor(" +
                        "window.HTMLInputElement.prototype, 'value').set;" +
                        "setter.call(input, value);" +
                        "input.dispatchEvent(new Event('input', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('change', { bubbles: true }));" +
                        "input.dispatchEvent(new Event('blur', { bubbles: true }));",
                dateField,
                expectedDate
        );

        // Read the field value after the application has had time to react.
        String actualDate = wait.until(currentDriver -> {
            WebElement currentField =
                    currentDriver.findElement(operationDateInput);

            String value = currentField.getAttribute("value");

            return expectedDate.equals(value) ? value : null;
        });

        // Report the successfully applied date.
        System.out.println(
                "Operation date successfully updated to: " + actualDate
        );
    }


    /**
     * Selects the Free Quantity Processing operation date six months
     * from today using the RMDP calendar.
     *
     * Uses stable data-testid and accessible aria-label attributes.
     * Navigates month by month without relying on a fixed navigation limit,
     * selects the exact target date, and verifies the final input value.
     */
    public void selectOperationDateSixMonthsFromNowFromCalendar() {

        // Locates the operation date input using its stable test identifier.
        By operationDateInput = By.cssSelector(
                "[data-testid='process-free-qty-operationDate'] input"
        );

        // Locates the calendar button using its accessible label.
        By operationDateCalendarButton = By.cssSelector(
                "[data-testid='process-free-qty-operationDate'] " +
                        "button[aria-label='Open date picker']"
        );

        // Locates the visible RMDP calendar dialog.
        By calendarWrapperLocator = By.cssSelector(
                "div.rmdp-wrapper[role='dialog']"
        );

        // Locates the calendar's Next Month button without generated IDs.
        By nextMonthButtonLocator = By.cssSelector(
                "div.rmdp-wrapper[role='dialog'] " +
                        "button.rmdp-arrow-container.rmdp-right"
        );

        // Locates the calendar header containing the displayed month and year.
        By calendarHeaderLocator = By.cssSelector(
                "div.rmdp-wrapper[role='dialog'] .rmdp-header-values"
        );

        // Calculates the target date six months from the current date.
        LocalDate currentDate = LocalDate.now();
        LocalDate targetOperationDate = currentDate.plusMonths(6);

        // Formats the target date to match the input's YYYY-MM-DD format.
        String expectedOperationDate = targetOperationDate.toString();

        // Calculates the exact number of month transitions required.
        int maxMonthNavigationAttempts =
                (targetOperationDate.getYear() - currentDate.getYear()) * 12
                        + targetOperationDate.getMonthValue()
                        - currentDate.getMonthValue();

        // Builds the accessible label used by RMDP for the exact target day.
        String targetDateAriaLabel = String.format(
                Locale.ENGLISH,
                "Choose %s %s %02d of %d",
                targetOperationDate.getDayOfWeek().getDisplayName(
                        TextStyle.FULL, Locale.ENGLISH
                ),
                targetOperationDate.getMonth().getDisplayName(
                        TextStyle.FULL, Locale.ENGLISH
                ),
                targetOperationDate.getDayOfMonth(),
                targetOperationDate.getYear()
        );

        // Locates the exact target date by its complete accessible label.
        By targetDateLocator = By.cssSelector(
                "div.rmdp-wrapper[role='dialog'] " +
                        ".rmdp-day[aria-label='" + targetDateAriaLabel + "']"
        );

        // Creates an explicit wait for calendar and date-field interactions.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Logs the date that this method is expected to select.
        System.out.println(
                "Selecting Free Quantity Processing operation date: "
                        + expectedOperationDate
        );

        // Waits until the operation date field is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        operationDateInput
                )
        );

        // Opens the operation date calendar.
        wait.until(
                ExpectedConditions.elementToBeClickable(
                        operationDateCalendarButton
                )
        ).click();

        // Waits until the RMDP calendar dialog is visible.
        wait.until(
                ExpectedConditions.visibilityOfElementLocated(
                        calendarWrapperLocator
                )
        );

        boolean targetDateVisible = false;

        // Checks the current month and navigates forward until the target date appears.
        for (int attempt = 0;
             attempt <= maxMonthNavigationAttempts;
             attempt++) {

            // Re-finds the target date on each iteration to avoid stale elements.
            targetDateVisible = driver.findElements(targetDateLocator)
                    .stream()
                    .anyMatch(element -> {
                        try {
                            return element.isDisplayed();
                        } catch (StaleElementReferenceException ignored) {
                            return false;
                        }
                    });

            // Stops navigating as soon as the exact target date is visible.
            if (targetDateVisible) {
                break;
            }

            // Stops if all expected month transitions have been attempted.
            if (attempt == maxMonthNavigationAttempts) {
                break;
            }

            // Reads the current calendar month and year before navigation.
            String previousHeader = wait.until(
                    ExpectedConditions.visibilityOfElementLocated(
                            calendarHeaderLocator
                    )
            ).getText().trim();

            // Clicks Next Month to move toward the target date.
            wait.until(
                    ExpectedConditions.elementToBeClickable(
                            nextMonthButtonLocator
                    )
            ).click();

            // Waits until the calendar header changes to the next month.
            wait.until(currentDriver -> {
                try {
                    WebElement currentHeader =
                            currentDriver.findElement(calendarHeaderLocator);

                    return currentHeader.isDisplayed()
                            && !currentHeader.getText().trim()
                            .equals(previousHeader);

                } catch (StaleElementReferenceException ignored) {
                    return false;
                }
            });
        }

        // Fails clearly if the exact target date cannot be found.
        if (!targetDateVisible) {
            throw new NoSuchElementException(
                    "Could not find operation date "
                            + expectedOperationDate
                            + " after navigating "
                            + maxMonthNavigationAttempts
                            + " calendar months."
            );
        }

        // Clicks the exact target date after waiting for it to become clickable.
        wait.until(
                ExpectedConditions.elementToBeClickable(targetDateLocator)
        ).click();

        // Verifies that the input contains the exact selected date.
        wait.until(
                ExpectedConditions.attributeToBe(
                        operationDateInput,
                        "value",
                        expectedOperationDate
                )
        );

        // Logs successful selection and verification.
        System.out.println(
                "Free Quantity Processing operation date selected and verified: "
                        + expectedOperationDate
        );
    }


    /**
     * Selects a random available branch from the Free Quantity Processing form.
     *
     * Uses the stable branch data-testid and React Select option structure.
     * Filters visible, non-empty options, re-locates the chosen option before
     * clicking, and retries temporary Selenium interaction failures.
     */
    public void selectRandomBranch() {

        // Stable locator for the Branch dropdown in Free Quantity Processing.
        By branchDropdown =
                By.cssSelector("[data-testid='process-free-qty-branchId']");

        // Locates React Select options without generated IDs or dynamic CSS classes.
        By branchOptions =
                By.cssSelector("div.select__option");

        // Waits for the dropdown and its options to become interactable.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Provides random selection among the currently available branch options.
        Random random = new Random();

        // Limits retries for temporary rendering or Selenium interaction failures.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Attempt " + attempt + " of " + maxAttempts
                                + ": Selecting a random branch..."
                );

                // Re-locates the branch container and waits until it is clickable.
                WebElement dropdown = wait.until(
                        ExpectedConditions.elementToBeClickable(branchDropdown)
                );

                // Opens the React Select branch dropdown.
                dropdown.click();

                // Collects visible, non-empty options and retries the wait if
                // React re-renders the options while Selenium is reading them.
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

                // Fails explicitly if the dropdown contains no selectable options.
                if (availableOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No visible branch options were found."
                    );
                }

                // Randomly chooses one of the currently visible branch options.
                int randomIndex = random.nextInt(availableOptions.size());

                String selectedBranch =
                        availableOptions.get(randomIndex).getText().trim();

                System.out.println(
                        "Available branches: " + availableOptions.size()
                );

                System.out.println(
                        "Randomly selected branch: " + selectedBranch
                );

                // Builds a text-based XPath for the selected branch without
                // relying on generated IDs or a stored WebElement reference.
                By selectedBranchOption = By.xpath(
                        "//div[contains(@class,'select__option')]" +
                                "[normalize-space(.)=" +
                                toXPathLiteral(selectedBranch) + "]"
                );

                // Re-finds the chosen option immediately before clicking it.
                WebElement optionToClick = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedBranchOption
                        )
                );

                // Selects the randomly chosen branch.
                optionToClick.click();

                // Waits until the dropdown reflects the selected branch text.
                wait.until(currentDriver -> {
                    try {
                        WebElement currentDropdown =
                                currentDriver.findElement(branchDropdown);

                        return currentDropdown.getText()
                                .contains(selectedBranch);

                    } catch (StaleElementReferenceException e) {
                        return false;
                    }
                });

                System.out.println(
                        "Branch selected successfully: " + selectedBranch
                );

                return;

            } catch (StaleElementReferenceException | TimeoutException e) {

                System.out.println(
                        "Attempt " + attempt + " of " + maxAttempts
                                + " failed while selecting a branch: "
                                + e.getMessage()
                );

                // Throws a clear exception when all retries have been exhausted.
                if (attempt == maxAttempts) {
                    throw new IllegalStateException(
                            "Failed to select a random branch after "
                                    + maxAttempts + " attempts.",
                            e
                    );
                }
            }
        }
    }


    /**
     * Randomly selects an available warehouse from the Free Quantity Processing form.
     *
     * Uses the stable warehouse data-testid and React Select option structure.
     * Returns false when no warehouse options are available or when a timeout
     * or stale-element error prevents selection.
     * Re-locates the selected option before clicking to reduce stale-element risks.
     */
    public boolean selectRandomWarehouse() {

        // Stable locator for the Warehouse dropdown in Free Quantity Processing.
        By warehouseDropdown =
                By.cssSelector("[data-testid='process-free-qty-warehouseId']");

        // Locates React Select options without generated IDs or dynamic CSS classes.
        By warehouseOptions =
                By.cssSelector("div.select__option");

        // Waits for the Warehouse dropdown to become clickable.
        WebDriverWait dropdownWait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Uses a short wait because the selected branch may have no available warehouses.
        WebDriverWait optionsWait =
                new WebDriverWait(driver, Duration.ofSeconds(3));

        // Randomly chooses an index from the available warehouse options.
        Random random = new Random();

        try {

            System.out.println("Opening the Warehouse dropdown...");

            // Re-locates the Warehouse dropdown and waits until it is clickable.
            WebElement dropdown = dropdownWait.until(
                    ExpectedConditions.elementToBeClickable(warehouseDropdown)
            );

            // Opens the Warehouse dropdown to load its available options.
            dropdown.click();

            // Collects visible, non-empty options while handling React re-rendering.
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

                    // Keeps waiting while no visible warehouse options are available.
                    return visibleOptions.isEmpty()
                            ? null
                            : visibleOptions;

                } catch (StaleElementReferenceException e) {
                    // Retries the condition if React replaces an option during inspection.
                    return null;
                }
            });

            // Returns false if the selected branch has no available warehouses.
            if (availableOptions == null || availableOptions.isEmpty()) {

                System.out.println(
                        "No warehouse options are available for the selected branch."
                );

                return false;
            }

            // Randomly selects one of the currently available warehouse options.
            int randomIndex = random.nextInt(availableOptions.size());

            String selectedWarehouse =
                    availableOptions.get(randomIndex).getText().trim();

            System.out.println(
                    "Available warehouses: " + availableOptions.size()
            );

            System.out.println(
                    "Randomly selected warehouse: " + selectedWarehouse
            );

            // Builds a text-based locator for the exact selected warehouse.
            By selectedWarehouseOption = By.xpath(
                    "//div[contains(@class,'select__option')]" +
                            "[normalize-space(.)=" +
                            toXPathLiteral(selectedWarehouse) + "]"
            );

            // Re-finds the selected option and waits until it is clickable.
            WebElement optionToClick = optionsWait.until(
                    ExpectedConditions.elementToBeClickable(
                            selectedWarehouseOption
                    )
            );

            // Clicks the freshly located warehouse option.
            optionToClick.click();

            // Verifies that the Warehouse dropdown reflects the selected value.
            dropdownWait.until(currentDriver -> {
                try {
                    WebElement currentDropdown =
                            currentDriver.findElement(warehouseDropdown);

                    return currentDropdown.getText()
                            .contains(selectedWarehouse);

                } catch (StaleElementReferenceException e) {
                    return false;
                }
            });

            System.out.println(
                    "Warehouse selected successfully: " + selectedWarehouse
            );

            return true;

        } catch (TimeoutException | StaleElementReferenceException e) {

            // Reports the selection failure without retaining stale WebElements.
            System.out.println(
                    "Could not select a warehouse: " + e.getMessage()
            );

            return false;
        }
    }


    /**
     * Enters the requested Note (البيان) into the Free Quantity Processing form.
     *
     * Uses the stable data-testid attribute to locate the Note input.
     * Updates the value through the native HTML input setter and dispatches
     * input/change events so the application can detect the new value.
     * Re-locates the input during verification to reduce stale-element risks.
     *
     * @param note the text to enter into the Note field
     */
    public void enterNote(String note) {

        // Log the beginning of the Note field interaction.
        System.out.println("========================================");
        System.out.println("Entering Note (البيان)");
        System.out.println("Requested note: [" + note + "]");

        // Reject null values before interacting with the application.
        if (note == null) {
            throw new IllegalArgumentException(
                    "Note cannot be null."
            );
        }

        // Stable locator for the Note input using its dedicated test identifier.
        By noteInputLocator = By.cssSelector(
                "input[data-testid='process-free-qty-note']"
        );

        // Wait for the Note input and verify its value after updating it.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Wait until the Note input is visible and enabled.
        WebElement noteInput = wait.until(currentDriver -> {
            try {
                WebElement input =
                        currentDriver.findElement(noteInputLocator);

                return input.isDisplayed() && input.isEnabled()
                        ? input
                        : null;

            } catch (StaleElementReferenceException
                     | NoSuchElementException ignored) {
                // Retry if the application replaces the input during rendering.
                return null;
            }
        });

        // Scroll the Note input into view before updating it.
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView({block:'center'});",
                noteInput
        );

        // Set the Note value and notify the application through input/change events.
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

        // Re-locate the input and verify that the requested value was applied.
        String actualNote = wait.until(currentDriver -> {
            try {
                WebElement currentInput =
                        currentDriver.findElement(noteInputLocator);

                String currentValue =
                        currentInput.getAttribute("value");

                return note.equals(currentValue)
                        ? currentValue
                        : null;

            } catch (StaleElementReferenceException
                     | NoSuchElementException ignored) {
                // Retry verification if the input is temporarily replaced.
                return null;
            }
        });

        // Log successful entry and verification.
        System.out.println(
                "Note entered and verified successfully: ["
                        + actualNote + "]"
        );
        System.out.println("========================================");
    }


    /**
     * Randomly selects an available Processing Type (نوع المعالجة)
     * from the Free Quantity Processing form.
     *
     * Supports the following options:
     * 1 - هدية مجانية للعميل
     * 2 - تخفيض التكلفة
     * 3 - إيراد للشركة
     *
     * Uses stable data-testid attributes, retries temporary Selenium failures,
     * and verifies that the selected radio button is checked.
     */
    public void selectRandomProcessingType() {

        // Stable locator for the Processing Type radio group.
        By processingTypeRadioButtons = By.cssSelector(
                "[data-testid='process-free-qty-processingTypeId'] " +
                        "button[role='radio']"
        );

        // Waits for the radio group options to become available.
        WebDriverWait wait =
                new WebDriverWait(driver, Duration.ofSeconds(10));

        // Randomly chooses one of the available Processing Type options.
        Random random = new Random();

        // Limits retries when React re-renders the radio group.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting random Processing Type - attempt "
                                + attempt + " of " + maxAttempts
                );

                // Collects stable test IDs and values rather than retaining
                // WebElement references that may become stale.
                List<String[]> processingOptions = wait.until(currentDriver -> {

                    try {

                        List<WebElement> elements =
                                currentDriver.findElements(
                                        processingTypeRadioButtons
                                );

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
                        // Retry if React replaces the radio buttons during inspection.
                        return null;
                    }
                });

                // Ensures that selectable Processing Type options were found.
                if (processingOptions.isEmpty()) {
                    throw new IllegalStateException(
                            "No available Processing Type radio buttons were found."
                    );
                }

                // Randomly selects one of the available Processing Type options.
                String[] selectedOption = processingOptions.get(
                        random.nextInt(processingOptions.size())
                );

                String selectedTestId = selectedOption[0];
                String selectedValue = selectedOption[1];

                // Converts the selected radio value into its visible Arabic label.
                String selectedProcessingType;

                switch (selectedValue) {
                    case "1":
                        selectedProcessingType = "هدية مجانية للعميل";
                        break;

                    case "2":
                        selectedProcessingType = "تخفيض التكلفة";
                        break;

                    case "3":
                        selectedProcessingType = "إيراد للشركة";
                        break;

                    default:
                        throw new IllegalStateException(
                                "Unexpected Processing Type value: "
                                        + selectedValue
                        );
                }

                System.out.println(
                        "Available Processing Type options: "
                                + processingOptions.size()
                );

                System.out.println(
                        "Randomly selected Processing Type: "
                                + selectedProcessingType
                                + " (value=" + selectedValue + ")"
                );

                // Re-locates the selected radio button using its stable test ID.
                By selectedRadioButton = By.cssSelector(
                        "[data-testid='" + selectedTestId + "']" +
                                "[role='radio']"
                );

                // Waits until the freshly located radio button is clickable.
                WebElement radioButton = wait.until(
                        ExpectedConditions.elementToBeClickable(
                                selectedRadioButton
                        )
                );

                // Clicks only when the randomly selected option is not checked.
                if (!"true".equals(
                        radioButton.getAttribute("aria-checked")
                )) {
                    radioButton.click();
                }

                // Verifies the selected radio button's state without retaining
                // a WebElement reference across polling iterations.
                wait.until(currentDriver -> {

                    try {

                        WebElement selectedRadio =
                                currentDriver.findElement(
                                        selectedRadioButton
                                );

                        return "true".equals(
                                selectedRadio.getAttribute("aria-checked")
                        );

                    } catch (StaleElementReferenceException
                             | NoSuchElementException e) {
                        return false;
                    }
                });

                System.out.println(
                        "Processing Type selected successfully: "
                                + selectedProcessingType
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "Processing Type radio buttons were re-rendered. "
                                + "Retrying attempt " + attempt
                                + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Processing Type selection timed out during attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        // Fails explicitly if all attempts to select and verify an option fail.
        throw new IllegalStateException(
                "Unable to select a Processing Type after "
                        + maxAttempts
                        + " attempts because of repeated re-rendering "
                        + "or timeouts."
        );
    }



    /**
     * Adds a new Free Quantity Processing row and captures its Item cell.
     *
     * Supports an initially empty grid and waits for the new row to appear.
     * Uses the Item test ID to distinguish the newly created cell from existing cells.
     */
    public void clickAddLine() {

        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(15)
        );

        By addLineButtonLocator = By.xpath(
                "//button[normalize-space(.)='اضافة سطر']"
        );

        By itemCellsLocator = By.cssSelector(
                "[data-testid^='process-free-qty-itemId-']"
        );

        // Capture existing Item cell IDs before adding a row.
        Set<String> previousIds = new HashSet<>();

        for (WebElement cell : driver.findElements(itemCellsLocator)) {
            String id = cell.getAttribute("data-testid");

            if (id != null && !id.isBlank()) {
                previousIds.add(id);
            }
        }

        // Reset the stored row information before starting a new operation.
        newItemTestId = null;
        newRowIndex = -1;

        // Click Add Line even when the grid contains no rows.
        wait.until(
                ExpectedConditions.elementToBeClickable(addLineButtonLocator)
        ).click();

        // Wait for a newly created Item cell and read its row index.
        String[] newRowDetails = wait.until(d -> {

            for (WebElement cell : d.findElements(itemCellsLocator)) {
                try {
                    String id = cell.getAttribute("data-testid");

                    if (id == null
                            || id.isBlank()
                            || previousIds.contains(id)
                            || !cell.isDisplayed()) {
                        continue;
                    }

                    WebElement row = cell.findElement(
                            By.xpath("./ancestor::*[@row-index][1]")
                    );

                    String index = row.getAttribute("row-index");

                    if (index != null && index.matches("\\d+")) {
                        return new String[]{id, index};
                    }

                } catch (StaleElementReferenceException
                         | NoSuchElementException ignored) {
                    // Retry while AG Grid is re-rendering.
                }
            }

            return null;
        });

        // Store the identity and index of the new row.
        newItemTestId = newRowDetails[0];
        newRowIndex = Integer.parseInt(newRowDetails[1]);

        System.out.println(
                "New row created: testId=" + newItemTestId
                        + ", rowIndex=" + newRowIndex
        );
    }



    /**
     * Randomly selects an Item Name from the React Select dropdown
     * in the newly added Free Quantity Processing row.
     *
     * Uses the stable AG Grid column identifier col-id='itemId'.
     * Identifies the target row by its previously captured row index
     * without constructing a dynamic CSS selector.
     *
     * Re-fetches cells and dropdown options after re-rendering to reduce
     * StaleElementReferenceException risks.
     *
     * @throws IllegalStateException if the new row has not been captured
     *                              or no selectable items are available
     */
    public void selectRandomItem() {

        // Waits for the target cell and the React Select editor.
        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Ensures that clickAddLine() has captured the newly added row.
        if (newRowIndex < 0) {
            throw new IllegalStateException(
                    "The new row index has not been captured. "
                            + "Call clickAddLine() first."
            );
        }

        // Stores the previously captured row index for this operation.
        final int targetRowIndex = newRowIndex;

        /*
         * Locates Item Name cells using the stable AG Grid column ID.
         * This selector contains no generated IDs or dynamic row indexes.
         */
        By itemNameCellsLocator = By.cssSelector(
                "div[role='gridcell'][col-id='itemId']"
        );

        /*
         * Locates the active React Select input using the editor structure
         * from the existing Item selection implementation.
         */
        By reactSelectInputLocator = By.cssSelector(
                ".ag-popup-editor input.select__input"
        );

        /*
         * Locates React Select options using the established option class.
         * No generated listbox ID or dynamic option selector is used.
         */
        By itemOptionsLocator = By.cssSelector(
                "div.select__option"
        );

        // Retries temporary grid re-rendering without clicking another row.
        int maxAttempts = 5;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {

                System.out.println(
                        "Selecting a random Item Name - attempt "
                                + attempt + " of " + maxAttempts
                );

                /*
                 * Re-fetches all Item cells and finds the cell whose parent
                 * AG Grid row matches the captured target row index.
                 */
                WebElement itemCell = wait.until(webDriver -> {

                    List<WebElement> cells =
                            webDriver.findElements(itemNameCellsLocator);

                    for (WebElement cell : cells) {

                        try {

                            // Finds the row containing the current Item cell.
                            WebElement row = cell.findElement(
                                    By.xpath(
                                            "./ancestor::div[@role='row'][1]"
                                    )
                            );

                            // Reads the row index without building a dynamic locator.
                            String rowIndex = row.getAttribute("row-index");

                            // Returns only the Item cell belonging to the target row.
                            if (String.valueOf(targetRowIndex).equals(rowIndex)
                                    && cell.isDisplayed()) {
                                return cell;
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Continues searching if AG Grid replaces this cell.
                        }
                    }

                    // Keeps waiting until the target cell is available.
                    return null;
                });

                // Scrolls the target Item cell into view before interaction.
                ((JavascriptExecutor) driver).executeScript(
                        "arguments[0].scrollIntoView({" +
                                "block:'center', inline:'nearest'" +
                                "});",
                        itemCell
                );

                /*
                 * Re-locates the target cell before double-clicking it,
                 * preventing interaction with the previously captured element.
                 */
                WebElement currentItemCell = wait.until(webDriver -> {

                    List<WebElement> cells =
                            webDriver.findElements(itemNameCellsLocator);

                    for (WebElement cell : cells) {

                        try {

                            WebElement row = cell.findElement(
                                    By.xpath(
                                            "./ancestor::div[@role='row'][1]"
                                    )
                            );

                            String rowIndex = row.getAttribute("row-index");

                            if (String.valueOf(targetRowIndex).equals(rowIndex)
                                    && cell.isDisplayed()
                                    && cell.isEnabled()) {
                                return cell;
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Ignores cells replaced during rendering.
                        }
                    }

                    return null;
                });

                // Activates the Item Name cell editor.
                new Actions(driver)
                        .moveToElement(currentItemCell)
                        .doubleClick()
                        .perform();

                System.out.println(
                        "Item Name cell activated for row index: "
                                + targetRowIndex
                );

                // Waits until the React Select editor becomes visible.
                wait.until(
                        ExpectedConditions.visibilityOfElementLocated(
                                reactSelectInputLocator
                        )
                );

                /*
                 * Re-fetches the editor before clicking to avoid using
                 * a WebElement captured before a possible re-render.
                 */
                wait.until(
                        ExpectedConditions.elementToBeClickable(
                                reactSelectInputLocator
                        )
                ).click();

                /*
                 * Waits for visible dropdown options.
                 * Only currently visible options are considered for selection.
                 */
                wait.until(webDriver -> {

                    List<WebElement> options =
                            webDriver.findElements(itemOptionsLocator);

                    for (WebElement option : options) {

                        try {
                            if (option.isDisplayed()
                                    && !option.getText().trim().isEmpty()) {
                                return true;
                            }
                        } catch (StaleElementReferenceException ignored) {
                            // Continues waiting for the refreshed options.
                        }
                    }

                    return false;
                });

                /*
                 * Collects option indexes rather than retaining WebElements
                 * while the React Select dropdown may re-render.
                 */
                List<Integer> visibleOptionIndexes = wait.until(webDriver -> {

                    List<WebElement> options =
                            webDriver.findElements(itemOptionsLocator);

                    List<Integer> indexes = new ArrayList<>();

                    for (int index = 0; index < options.size(); index++) {

                        try {

                            WebElement option = options.get(index);

                            if (option.isDisplayed()
                                    && !option.getText().trim().isEmpty()) {
                                indexes.add(index);
                            }

                        } catch (StaleElementReferenceException ignored) {

                            // Retries collection if an option becomes stale.
                            return null;
                        }
                    }

                    return indexes.isEmpty() ? null : indexes;
                });

                // Chooses one of the visible option indexes at random.
                int randomPosition = new Random().nextInt(
                        visibleOptionIndexes.size()
                );

                int selectedOptionIndex =
                        visibleOptionIndexes.get(randomPosition);

                /*
                 * Re-fetches the options immediately before clicking and
                 * verifies that the selected index is still valid and visible.
                 */
                WebElement selectedOption = wait.until(webDriver -> {

                    List<WebElement> options =
                            webDriver.findElements(itemOptionsLocator);

                    if (selectedOptionIndex >= options.size()) {
                        return null;
                    }

                    try {

                        WebElement option = options.get(selectedOptionIndex);

                        if (option.isDisplayed()
                                && !option.getText().trim().isEmpty()) {
                            return option;
                        }

                    } catch (StaleElementReferenceException ignored) {

                        // Retries until the option can be retrieved again.
                    }

                    return null;
                });

                // Saves the selected item's visible name for reporting.
                String selectedItemName = selectedOption.getText().trim();

                // Clicks the freshly retrieved option.
                selectedOption.click();

                /*
                 * Waits until the dropdown closes, indicating that the
                 * selection interaction has completed.
                 */
                wait.until(webDriver ->
                        webDriver.findElements(itemOptionsLocator)
                                .stream()
                                .noneMatch(option -> {
                                    try {
                                        return option.isDisplayed();
                                    } catch (StaleElementReferenceException ignored) {
                                        return false;
                                    }
                                })
                );

                System.out.println(
                        "Selected Item Name: " + selectedItemName
                );

                System.out.println(
                        "Item selection completed for row index: "
                                + targetRowIndex
                );

                return;

            } catch (StaleElementReferenceException e) {

                System.out.println(
                        "The grid or dropdown was re-rendered. Retrying attempt "
                                + attempt + " of " + maxAttempts
                );

            } catch (TimeoutException e) {

                System.out.println(
                        "Timed out while selecting an Item Name. Attempt "
                                + attempt + " of " + maxAttempts
                );
            }
        }

        throw new IllegalStateException(
                "Could not select a random Item Name in row index "
                        + targetRowIndex
                        + " after " + maxAttempts
                        + " attempts. Verify the Item editor and option locators."
        );
    }



    /**
     * Enters the requested quantity in the captured row and commits the edit
     * by clicking the Note cell in that same row.
     *
     * Verifies the quantity in the rendered grid before returning.
     */
    public void enterQuantityToBeProcessed(String quantity) {

        if (quantity == null || quantity.isBlank()) {
            throw new IllegalArgumentException(
                    "Quantity must not be null or blank."
            );
        }

        if (newRowIndex < 0) {
            throw new IllegalStateException(
                    "No new row is available. Call clickAddLine() first."
            );
        }

        WebDriverWait wait = new WebDriverWait(
                driver, Duration.ofSeconds(15)
        );

        // Locate the Quantity cell in the captured row.
        By quantityCellLocator = By.cssSelector(
                ".ag-center-cols-container .ag-row[row-index='"
                        + newRowIndex
                        + "'] .ag-cell[col-id='quantitiesToBeProcessed']"
        );

        // Locate the Note cell used to commit the quantity edit.
        By noteCellLocator = By.cssSelector(
                ".ag-center-cols-container .ag-row[row-index='"
                        + newRowIndex
                        + "'] .ag-cell[col-id='note']"
        );

        // Locate the active editor specifically inside the Quantity cell.
        By quantityEditorLocator = By.cssSelector(
                ".ag-center-cols-container .ag-row[row-index='"
                        + newRowIndex
                        + "'] .ag-cell[col-id='quantitiesToBeProcessed']"
                        + ".ag-cell-inline-editing input"
        );

        // Activate the Quantity cell editor.
        WebElement quantityCell = wait.until(
                ExpectedConditions.elementToBeClickable(quantityCellLocator)
        );

        new Actions(driver).doubleClick(quantityCell).perform();

        // Wait until the editor is actually available.
        WebElement editor = wait.until(d -> {
            for (WebElement element : d.findElements(quantityEditorLocator)) {
                if (element.isDisplayed() && element.isEnabled()) {
                    return element;
                }
            }
            return null;
        });

        // Set the value through the native input setter and notify React.
        ((JavascriptExecutor) driver).executeScript(
                """
                const input = arguments[0];
                const value = arguments[1];
                const setter = Object.getOwnPropertyDescriptor(
                    window.HTMLInputElement.prototype, 'value'
                ).set;
    
                setter.call(input, value);
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                """,
                editor,
                quantity
        );

        // Wait until the Note cell is available before committing the edit.
        wait.until(
                ExpectedConditions.elementToBeClickable(noteCellLocator)
        ).click();

        // Verify the quantity is displayed in the grid after leaving the editor.
        wait.until(d -> {
            try {
                WebElement cell = d.findElement(quantityCellLocator);
                String actual = cell.getText().trim();

                if (actual.equals(quantity.trim())) {
                    return true;
                }

                try {
                    return new java.math.BigDecimal(actual.replace(",", ""))
                            .compareTo(
                                    new java.math.BigDecimal(
                                            quantity.trim().replace(",", "")
                                    )
                            ) == 0;
                } catch (NumberFormatException ignored) {
                    return false;
                }

            } catch (StaleElementReferenceException
                     | NoSuchElementException ignored) {
                return false;
            }
        });

        System.out.println(
                "Quantity committed and verified: " + quantity
        );
    }




    /**
     * Enters and commits the Note (البيان) in the newly added
     * Free Quantity Processing AG Grid row.
     *
     * Uses the stable Note cell data-testid and the AG Grid column ID.
     * Double-clicks the Note cell to activate its editor, enters the
     * requested value, commits the edit, and verifies the rendered value.
     *
     * @param note the Note text to enter into the grid row
     * @throws IllegalArgumentException if the Note is null or blank
     * @throws IllegalStateException if the editor or committed value
     *                                cannot be verified
     */
    public void enterFreeQuantityProcessingNote(String note) {

        if (note == null || note.isBlank()) {
            throw new IllegalArgumentException(
                    "The Free Quantity Processing Note must not be null or blank."
            );
        }

        if (newRowIndex < 0) {
            throw new IllegalStateException(
                    "No new row is available. Call clickAddLine() first."
            );
        }

        WebDriverWait wait = new WebDriverWait(
                driver,
                Duration.ofSeconds(10)
        );

        // Locate the Note cell using its stable test ID for the captured row.
        By noteCellLocator = By.cssSelector(
                "[data-testid='process-free-qty-note-" + newRowIndex + "']"
        );

        // Locate the input editor only inside the target Note cell.
        By noteEditorLocator = By.cssSelector(
                "[data-testid='process-free-qty-note-" + newRowIndex
                        + "'].ag-cell-inline-editing input"
        );

        // Wait for the target Note cell to be present and visible.
        WebElement noteCell = wait.until(
                ExpectedConditions.visibilityOfElementLocated(noteCellLocator)
        );

        // Activate editing on the Note cell using a double-click.
        new Actions(driver)
                .doubleClick(noteCell)
                .perform();

        // Wait until AG Grid creates an input editor inside the Note cell.
        WebElement noteEditor = wait.until(
                ExpectedConditions.visibilityOfElementLocated(noteEditorLocator)
        );

        // Enter the Note using normal WebDriver keyboard interactions.
        noteEditor.click();
        noteEditor.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        noteEditor.sendKeys(note);

        // Commit the edit by moving focus away from the editor.
        noteEditor.sendKeys(Keys.TAB);

        // Verify that the committed Note is rendered in the target cell.
        wait.until(driver -> {
            WebElement cell = driver.findElement(noteCellLocator);
            return note.equals(cell.getText().trim());
        });

        // Report success only after the rendered cell value matches.
        System.out.println(
                "Free Quantity Processing Note entered and verified: " + note
        );
    }


}
