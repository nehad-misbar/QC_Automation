package com.erp.automation.pages;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.Actions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.erp.automation.ui.DocumentApi.isTrue;
import static com.erp.automation.ui.DocumentApi.textOf;

/**
 * Purchase Return Invoice screen (فواتير مردود المشتريات): {@code /purchase/invoice-return},
 * data-testid prefix {@code invoice-return}, list API {@code PurchasInvoice/filter/1}, numbers PRI….
 * QC 2026-11-05: the same form as the local invoice (this class started as a copy of
 * {@code LocalInvoicePage}), with these differences: "المستند المرجعي" = a purchase invoice
 * (LPI…; every return on QC has one), no "مبلغ الفاتورة", no production date / free quantity
 * columns, "النوع" not required; "⋮" of a new return: only "الاستيراد من Excel"; of a saved one:
 * "التصدير الى Excel" and "دفع"; items pricing by item ids. The local notes below were kept
 * where they apply to both screens.
 *
 * <p>Everything common to Purchasing documents is in
 * {@link PurchasingDocumentPage}. This class adds only what is specific to
 * local invoices.</p>
 *
 * <p>Checked in QC on 2026-11-05 (nothing saved):</p>
 * <ul>
 *     <li>data-testid prefix {@code invoice-return}; the number is
 *     {@code #invoiceCode} on the form and col-id {@code invoiceCode} in the list;</li>
 *     <li>API {@code POST PurchasInvoice/filter/0} (387 invoices: 63 Draft,
 *     324 Approved, none cancelled). There is no {@code recordStatus}: the
 *     status is {@code isApproved} / {@code invoiceStatus} (0 Draft, 1 Approved);</li>
 *     <li>the list has NO status column, so documents are chosen through the API;</li>
 *     <li>required (empty save): النوع، المورد، الفرع، المخزن، مندوب المشتريات،
 *     الوسم، مبلغ الفاتورة، رقم فاتورة المورد, and the toast
 *     "تفاصيل الفاتورة: يجب اضافة صنف واحد على الأقل";</li>
 *     <li>the header field ids differ from the other screens:
 *     {@code taxCalculationMethode}, {@code itemsCostsMethode},
 *     {@code deservedDate} (تاريخ الاستحقاق), {@code notes} (not "note");</li>
 *     <li>the price column of the lines is {@code cost} (not "price");</li>
 *     <li>there is no "إلغاء" (cancel) action; an Approved invoice has no
 *     "اعتماد/عدم اعتماد" button but has "قيد اليومية" (journalEntry);</li>
 *     <li>Draft in edit mode ({@code /edit/}): every header field except
 *     الرقم المرجعي، المستند المرجعي، التعادل، الرقم الضريبي، تاريخ الاعتماد;</li>
 *     <li>Approved in edit mode ({@code /view-approved/}): المورد and البيان only;</li>
 *     <li>"المستند المرجعي" opens a dialog of receipt orders (أوامر الاستلام);</li>
 *     <li>"⋮" on a new invoice: الاستيراد من Excel، متابعة من أمر استلام.</li>
 * </ul>
 *
 * <p>The old page object {@code LocalPurchaseInvoicePage} (346 KB, one method
 * per field, fixed sleeps) is no longer used by the tests. It is left in the
 * project until this class has run green on QC, then it can be deleted.</p>
 */
public class ReturnInvoicePage extends PurchasingDocumentPage {

    public static final String PREFIX = "invoice-return";

    /** Form input id and list col-id of the invoice number. */
    public static final String CODE_FIELD = "invoiceCode";

    /** The filter endpoint of local invoices has an extra "/0" (invoice type). */
    private static final String API_FILTER_PATH = "PurchasInvoice/filter/1";

    /** Items pricing ($) of a return opens {@code /sales/pricing/add?itemIds=…} (QC 2026-11-05). */
    public static final String ITEMS_PRICING_BY_ITEMS = "itemIds=";

    /** Id of the "البيان" input ("notes" on invoices). */
    public static final String NOTES_FIELD = "notes";

    /** Line column of the unit price ("التكلفة"). */
    public static final String PRICE_COLUMN = "cost";

    /** Header fields that are React Select dropdowns, in screen order (right to left, row by row). */
    public static final List<String> SELECT_FIELDS = List.of(
            "operationTypeCategoryId", "vendorId", "currencyId", "branchId", "warehouseId",
            "costCenterId", "taxCalculationMethode", "paymentMethod", "purchaseRepresentativeId",
            "itemsCostsMethode", "tagId");

    /** "الأسعار" radios: باستثناء الضريبة (priceNature-0) / شاملة الضريبة (priceNature-1). */
    private static final String[] PRICE_NATURE_RADIOS = {"priceNature-0", "priceNature-1"};

    /** Label of the total that "مبلغ الفاتورة" must match. */
    private static final String TOTAL_WITH_TAX_LABEL = "اجمالي قيمة الفاتورة شاملة الضريبة";

    public ReturnInvoicePage(WebDriver driver) {
        super(driver, PREFIX, "/purchase/invoice-return", API_FILTER_PATH, CODE_FIELD);
    }

    // ----- tabs under the header -----

    /** Tabs under the header (QC 2026-11-05). Only the open tab's grid is on the page. */
    public static final String TAB_LINES = "تفاصيل الفاتورة";
    public static final String TAB_DISCOUNTS_ADDITIONS = "الخصومات والإضافات";
    public static final String TAB_PAYMENT_SCHEDULING = "جدولة الدفعات";

    /**
     * Opens tab {@code label} and waits until it is selected. A script click
     * does not switch these tabs; a real click does (QC 2026-11-05).
     */
    public void openTab(String label) {
        By tab = By.xpath("//*[@role='tab'][normalize-space(.)=" + xpathLiteral(label) + "]");
        for (int attempt = 1; attempt <= 3; attempt++) {
            WebElement element = new WebDriverWait(driver, WAIT)
                    .until(ExpectedConditions.presenceOfElementLocated(tab));
            if ("true".equals(element.getAttribute("aria-selected"))) {
                ui.waitForGridToSettle();
                return;
            }
            stopCellEditing();
            ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
            driver.findElement(tab).click();
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(200))
                        .until(d -> "true".equals(d.findElement(tab).getAttribute("aria-selected")));
                ui.waitForGridToSettle();
                log.info("Tab [{}] opened.", label);
                return;
            } catch (TimeoutException e) {
                log.warn("Tab [{}] did not open (attempt {}/3).", label, attempt);
            }
        }
        throw new IllegalStateException("Tab [" + label + "] could not be opened.");
    }

    // ----- header -----

    /** "طريقة الدفع" = "آجل" (credit), as the old test did for a copy with a reference document. */
    public static final String PAYMENT_METHOD_CREDIT = "آجل";

    public String selectPaymentMethod(String text) {
        if (text.equals(selectedText("paymentMethod"))) {
            log.info("paymentMethod is already {}", text);
            return text;
        }
        return selectOption("paymentMethod", text);
    }

    /**
     * "تعميم خصم على الأصناف": a random type, then the amount (the amount
     * field is locked until a type is chosen, QC 2026-11-05).
     *
     * @return the chosen type, "" when the field is locked.
     */
    public String setBulkDiscount(String amount) {
        String type = selectRandomOption("bulkDiscountType");
        if (type.isEmpty()) {
            return "";
        }
        By input = By.cssSelector("input[data-testid='" + PREFIX + "-bulkDiscountAmount']");
        new WebDriverWait(driver, WAIT).until(d -> d.findElement(input).isEnabled());
        setTextOn(input, "bulkDiscountAmount", amount);
        return type;
    }

    /** "استمرار" in the dialog shown after changing the supplier / branch / warehouse of a form with lines. */
    private static final By CONTINUE_BUTTON = By.xpath(
            "//button[@data-modal-agree and @type='submit' and normalize-space(.)='استمرار']");

    /**
     * Clicks "استمرار" when it appears within {@code timeout}; does nothing
     * otherwise. Taken from the old page object's {@code clickContinue}
     * (5 s wait, the button is optional), which the old test called after
     * choosing the supplier, branch and warehouse of a COPIED invoice
     * (asked by the user, 2026-11-05).
     *
     * @return true when the button was clicked.
     */
    public boolean clickContinueIfShown(Duration timeout) {
        WebElement button;
        try {
            button = new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(300))
                    .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                    .until(d -> d.findElements(CONTINUE_BUTTON).stream()
                            .filter(b -> b.isDisplayed() && b.isEnabled())
                            .findFirst().orElse(null));
        } catch (TimeoutException absent) {
            log.info("\"استمرار\" not shown within {} s → nothing to confirm.", timeout.toSeconds());
            return false;
        }
        try {
            button.click();
        } catch (org.openqa.selenium.StaleElementReferenceException rerendered) {
            driver.findElements(CONTINUE_BUTTON).stream()
                    .filter(WebElement::isDisplayed).findFirst().ifPresent(WebElement::click);
        }
        new WebDriverWait(driver, WAIT).pollingEvery(Duration.ofMillis(200))
                .until(d -> d.findElements(CONTINUE_BUTTON).stream().noneMatch(WebElement::isDisplayed));
        log.info("\"استمرار\" clicked.");
        return true;
    }

    /**
     * Random option of header field {@code field} on a form that already has
     * lines (copy, edit, after a download), in the old test's order: choose →
     * "استمرار" → check.
     *
     * <p>Why not {@code selectRandomOption}: it checks the field right after
     * the click. QC run 2026-11-05 15:28 (copy of LPI2610T16-000150): the
     * "استمرار" dialog opens BEFORE the supplier changes, so the field still
     * showed the old supplier, the selection was retried behind the dialog,
     * and the test failed. Here the dialog is confirmed first (up to 5 s, as
     * the old {@code clickContinue}), then the field must show the choice.</p>
     *
     * @param other choose an option different from the current one (edit mode).
     * @return the chosen option, or "" when the field is disabled.
     */
    public String selectRandomOptionAndContinue(String field, boolean other) {

        By control = By.cssSelector("[data-testid='" + PREFIX + "-" + field + "'] .select__control");
        By options = By.cssSelector("div[class*='select__option']");
        for (int attempt = 1; attempt <= 3; attempt++) {
            if (isSelectDisabled(field)) {
                log.info("Field {} is disabled → not changed.", field);
                return "";
            }
            String current = selectedText(field).replaceAll("\\s+", " ").trim();
            try {
                WebElement controlElement = new WebDriverWait(driver, WAIT)
                        .until(ExpectedConditions.elementToBeClickable(control));
                ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", controlElement);
                controlElement.click();
                List<String> texts = new WebDriverWait(driver, Duration.ofSeconds(4))
                        .pollingEvery(Duration.ofMillis(150))
                        .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                        .until(d -> {
                            List<String> shown = d.findElements(options).stream().filter(WebElement::isDisplayed)
                                    .map(o -> o.getText().trim()).filter(t -> !t.isEmpty()).toList();
                            return shown.isEmpty() ? null : shown;
                        });
                List<String> candidates = other
                        ? texts.stream().filter(t -> !t.replaceAll("\\s+", " ").equals(current)).toList()
                        : texts;
                if (candidates.isEmpty()) {
                    candidates = texts;
                }
                String chosen = candidates.get(random.nextInt(candidates.size()));
                By option = By.xpath("//div[contains(@class,'select__option')][normalize-space(.)="
                        + xpathLiteral(chosen) + "]");
                new WebDriverWait(driver, WAIT).ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                        .until(ExpectedConditions.elementToBeClickable(option)).click();

                boolean confirmed = clickContinueIfShown(Duration.ofSeconds(5));
                if (!confirmed) {
                    confirmDialogIfShown(Duration.ofMillis(800)); // any other optional confirmation
                }
                String expected = chosen.replaceAll("\\s+", " ");
                try {
                    new WebDriverWait(driver, Duration.ofSeconds(5)).pollingEvery(Duration.ofMillis(200))
                            .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                            .until(d -> selectedText(field).replaceAll("\\s+", " ").equals(expected));
                    log.info("{} = {}{}", field, chosen, confirmed ? " (after استمرار)" : "");
                    return chosen;
                } catch (TimeoutException notShown) {
                    log.warn("Field {}: chose [{}] but it shows [{}] (attempt {}/3).",
                            field, chosen, selectedText(field), attempt);
                }
            } catch (TimeoutException | org.openqa.selenium.StaleElementReferenceException
                     | org.openqa.selenium.ElementClickInterceptedException e) {
                log.warn("Field {}: selection interrupted ({}) (attempt {}/3).", field, e.getClass().getSimpleName(), attempt);
                if (isDialogOpen()) {
                    clickContinueIfShown(Duration.ofSeconds(2)); // a dialog left open blocks the field
                }
                new Actions(driver).sendKeys(Keys.ESCAPE).perform();
            }
        }
        throw new IllegalStateException("Could not select an option in " + field + " after 3 attempts (shows ["
                + selectedText(field) + "]).");
    }

    /** "جدولة الدفعات" → "قالب شروط السداد" at random; the lines tab is opened again afterwards. */
    public String selectRandomPaymentTermsTemplate() {
        openTab(TAB_PAYMENT_SCHEDULING);
        String template = selectRandomOption("paymentTermsTemplatesId");
        confirmDialogIfShown(Duration.ofMillis(800));
        openTab(TAB_LINES);
        return template;
    }

    /** "التاريخ" (invoice date). */
    public void setInvoiceDate(LocalDate date) {
        setDate("invoiceDate", date);
    }

    /** "تاريخ الاستحقاق" (shown with payment method "آجل"). */
    public void setDueDate(LocalDate date) {
        if (driver.findElements(By.id("deservedDate")).isEmpty()) {
            log.info("\"تاريخ الاستحقاق\" is not on the form (payment method is not آجل) → not set.");
            return;
        }
        setDate("deservedDate", date);
    }

    /** "الأسعار" at random; returns the radio id ("" when locked). */
    public String selectRandomPriceNature() {
        return clickRandomRadio(PRICE_NATURE_RADIOS);
    }

    /** "الأسعار": the option that is not selected now (edit mode). */
    public String selectOtherPriceNature() {
        return clickOtherRadio(PRICE_NATURE_RADIOS);
    }

    public String priceNature() {
        return checkedRadio(PRICE_NATURE_RADIOS);
    }

    /** "البيان" of the invoice. */
    public boolean setNotes(String text) {
        return setText(NOTES_FIELD, text);
    }

    public String notes() {
        return inputValue(NOTES_FIELD).trim();
    }

    /** Waits (max 10 s) until "البيان" shows {@code expected}; returns the last value read. */
    public String waitForNotes(String expected) {
        try {
            return new WebDriverWait(driver, WAIT)
                    .pollingEvery(Duration.ofMillis(250))
                    .until(d -> expected.equals(notes()) ? expected : null);
        } catch (TimeoutException e) {
            return notes();
        }
    }

    /** "رقم فاتورة المورد" (required). */
    public boolean setVendorInvoiceCode(String code) {
        return setText("vendorInvoiceCode", code);
    }

    /**
     * "مبلغ الفاتورة" (required) = "اجمالي قيمة الفاتورة شاملة الضريبة"
     * shown under the lines (QC 2026-11-05, LPI2609T16-000036: 31.623 in both).
     *
     * @return the amount entered.
     */
    public String copyTotalToInvoiceAmount() {
        // QC 2026-11-05: a purchase return has no "مبلغ الفاتورة" (amountInvoice) field.
        if (driver.findElements(By.cssSelector("[data-testid='" + PREFIX + "-amountInvoice'], input[name='amountInvoice']")).isEmpty()) {
            return "(no \"مبلغ الفاتورة\" on a return; total " + totalWithTax() + ")";
        }
        String total = totalWithTax();
        if (total.isEmpty()) {
            throw new IllegalStateException("\"" + TOTAL_WITH_TAX_LABEL + "\" is not shown.");
        }
        setText("amountInvoice", total);
        return total;
    }

    /** "اجمالي قيمة الفاتورة شاملة الضريبة" as shown (thousand separators removed), "" when not found. */
    public String totalWithTax() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const s=[...document.querySelectorAll('span')]"
                        + "  .find(x=>x.children.length===0 && x.textContent.trim()===arguments[0]);"
                        + "if(!s || !s.parentElement) return '';"
                        + "return s.parentElement.innerText.replace(arguments[0],'').trim();",
                TOTAL_WITH_TAX_LABEL);
        return text == null ? "" : text.toString().replace(",", "").trim();
    }

    /**
     * Sets text input {@code #<field>} the way React expects (native setter +
     * input event) and checks it keeps {@code text}; if not, it is typed key
     * by key. Same approach as {@code QuotationPage.setTextInput} (typing the
     * whole text at once lost characters while the form re-rendered).
     *
     * @return false when the field is locked.
     */
    public boolean setText(String field, String text) {
        return setTextOn(By.id(field), field, text);
    }

    /** {@link #setText} for an input found by {@code locator} ({@code name} is for the log). */
    private boolean setTextOn(By locator, String name, String text) {
        WebElement input = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.presenceOfElementLocated(locator));
        if (!input.isEnabled() || input.getAttribute("readonly") != null) {
            log.info("Field {} is not editable → not changed.", name);
            return false;
        }
        ((JavascriptExecutor) driver).executeScript(
                "const i=arguments[0];"
                        + "Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(i, arguments[1]);"
                        + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('change',{bubbles:true}));"
                        + "i.dispatchEvent(new Event('blur',{bubbles:true}));",
                input, text);
        if (keeps(locator, text)) {
            log.info("{} = {}", name, text);
            return true;
        }
        log.warn("{} shows [{}] after setting [{}] → typing it key by key.", name, valueAt(locator), text);
        WebElement retry = driver.findElement(locator);
        retry.click();
        retry.sendKeys(Keys.chord(Keys.CONTROL, "a"), Keys.DELETE);
        for (char c : text.toCharArray()) {
            driver.findElement(locator).sendKeys(String.valueOf(c));
        }
        if (!keeps(locator, text)) {
            throw new IllegalStateException(name + " shows [" + valueAt(locator) + "] instead of [" + text + "].");
        }
        log.info("{} = {} (typed)", name, text);
        return true;
    }

    private String valueAt(By locator) {
        List<WebElement> inputs = driver.findElements(locator);
        return inputs.isEmpty() ? "" : inputs.get(0).getAttribute("value");
    }

    /** True when the input shows {@code text} and still shows it one render later. */
    private boolean keeps(By locator, String text) {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(2)).pollingEvery(Duration.ofMillis(200))
                    .until(d -> text.equals(valueAt(locator)));
            new WebDriverWait(driver, Duration.ofMillis(600)).pollingEvery(Duration.ofMillis(200))
                    .until(d -> !text.equals(valueAt(locator)));
            return false; // changed back after a render
        } catch (TimeoutException stillThereOrNeverShown) {
            return text.equals(valueAt(locator));
        }
    }

    /**
     * Fields marked "هذا الحقل مطلوب": the field name ({@code data-testid}
     * without the prefix, or the input id), read from the message up to its
     * field (QC 2026-11-05, empty save of a new invoice).
     */
    @SuppressWarnings("unchecked")
    public List<String> requiredErrorFields() {
        Object names = ((JavascriptExecutor) driver).executeScript(
                "const p=arguments[0]+'-';"
                        + "return [...document.querySelectorAll('*')]"
                        + " .filter(e=>e.children.length===0 && e.textContent.trim()==='هذا الحقل مطلوب')"
                        + " .map(e=>{let x=e; for(let i=0;i<5 && x;i++){x=x.parentElement;"
                        + "   const f=x && x.querySelector('[data-testid^=\"'+p+'\"],input[id]');"
                        + "   if(f) return (f.dataset.testid||'').startsWith(p) ? f.dataset.testid.substring(p.length) : f.id;}"
                        + "   return '?';});",
                PREFIX);
        return names == null ? List.of() : (List<String>) names;
    }

    /**
     * Ids of the header inputs that can be changed now (React Select inputs
     * carry the field id too). Grid inputs ({@code ag-…}) are left out.
     * QC 2026-11-05: Approved in edit mode → [vendorId, notes].
     */
    @SuppressWarnings("unchecked")
    public List<String> editableHeaderInputs() {
        Object ids = ((JavascriptExecutor) driver).executeScript(
                "return [...document.querySelectorAll('input[id]')]"
                        + " .filter(i=>!i.id.startsWith('ag-') && !i.disabled && !i.readOnly).map(i=>i.id);");
        return ids == null ? List.of() : (List<String>) ids;
    }

    /** True when text input {@code #<field>} can be typed into. */
    public boolean isInputEditable(String field) {
        List<WebElement> inputs = driver.findElements(By.id(field));
        return !inputs.isEmpty() && inputs.get(0).isEnabled() && inputs.get(0).getAttribute("readonly") == null;
    }

    // ----- lines grid: date cells -----

    /** Calendar of a date cell (react-multi-date-picker, QC 2026-11-05). */
    private static final By CALENDAR = By.cssSelector(".rmdp-wrapper");
    private static final By CALENDAR_HEADER = By.cssSelector(".rmdp-header-values");
    private static final By CALENDAR_NEXT = By.cssSelector(".rmdp-arrow-container.rmdp-right");
    private static final By CALENDAR_PREVIOUS = By.cssSelector(".rmdp-arrow-container.rmdp-left");

    /**
     * The calendar that is SHOWN, or null. QC run 2026-11-05 14:48: after the
     * production date, the expiry calendar "did not open" 3 times; a closed
     * calendar can stay in the page hidden, and a plain {@code By} wait
     * looks only at the FIRST {@code .rmdp-wrapper}. Everything about the
     * calendar (header, arrows, day) is therefore read inside this one.
     */
    private WebElement visibleCalendar() {
        for (WebElement wrapper : driver.findElements(CALENDAR)) {
            try {
                if (wrapper.isDisplayed()) {
                    return wrapper;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException gone) {
                // re-rendered; the next one or the next poll
            }
        }
        return null;
    }

    private boolean isCalendarShown() {
        return visibleCalendar() != null;
    }

    private By lineCellOf(int row, String colId) {
        return By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] div[role='gridcell'][col-id='" + colId + "']");
    }

    /**
     * Date cell {@code colId} ("productionDate", "expireDate") of line
     * {@code row}: opens the calendar, moves to the month, clicks the day
     * (aria-label "Choose Thursday November 05 of 2026"), makes sure the
     * calendar is closed (an open one swallows the next click), and checks
     * the cell shows the date.
     */
    public boolean setLineDate(int row, String colId, LocalDate date) {

        String expected = date.toString(); // the grid shows yyyy-MM-dd
        String dayLabel = String.format(Locale.ENGLISH, "Choose %s %s %02d of %d",
                date.getDayOfWeek().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                date.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH),
                date.getDayOfMonth(), date.getYear());
        By day = By.cssSelector("[aria-label='" + dayLabel + "']");

        for (int attempt = 1; attempt <= 3; attempt++) {
            // QC run 2026-11-05 14:51: the expiry cell was not RENDERED (AG Grid renders
            // only the columns in view; the grid had scrolled after the production date),
            // so every attempt waited 10 s for a cell that did not exist. Scroll to it first.
            closeEditorsOtherThan(row, colId);
            revealLineCell(row, colId);
            if (!openCalendarIfEditable(row, colId)) {
                if (attempt == 1) {
                    // Disabled cell → skipped, as on orders (asked by the user, 2026-11-05).
                    log.info("Line {} {} is disabled (no calendar) → skipped ({} not entered).", row + 1, colId, expected);
                    return false;
                }
                log.warn("Calendar of {} on line {} did not open (attempt {}/3).", colId, row + 1, attempt);
                continue;
            }
            moveCalendarTo(YearMonth.from(date));
            new WebDriverWait(driver, WAIT)
                    .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                    .until(d -> {
                        WebElement calendar = visibleCalendar();
                        List<WebElement> days = calendar == null ? List.of() : calendar.findElements(day);
                        return days.isEmpty() ? null : days.get(0);
                    }).click();
            // Commit, never cancel: Escape (stopCellEditing) discards the date while the
            // popup editor is still open (QC run 2026-11-05 14:26: the cell stayed empty
            // three times). TAB commits it and moves on, as the old page object did.
            try {
                new WebDriverWait(driver, Duration.ofSeconds(3)).pollingEvery(Duration.ofMillis(150))
                        .until(d -> !isCalendarShown());
            } catch (TimeoutException stillOpen) {
                log.info("Calendar of {} still open after choosing the day → TAB.", colId);
                new Actions(driver).sendKeys(Keys.TAB).perform();
            }
            if (waitForCellToShow(row, colId, expected, Duration.ofSeconds(2))) {
                closeEditorByCommitting(row, colId);
                log.info("Line {} {} = {}", row + 1, colId, expected);
                return true;
            }
            if (isAnyCellEditorOpen()) {
                log.info("Line {} {}: editor still open and the cell is empty → TAB to commit.", row + 1, colId);
                new Actions(driver).sendKeys(Keys.TAB).perform();
                if (waitForCellToShow(row, colId, expected, Duration.ofSeconds(3))) {
                    closeEditorByCommitting(row, colId);
                    log.info("Line {} {} = {} (after TAB)", row + 1, colId, expected);
                    return true;
                }
            }
            log.warn("Line {} {} shows [{}] instead of {} (attempt {}/3).",
                    row + 1, colId, cellTextContent(row, colId), expected, attempt);
        }
        throw new IllegalStateException(colId + " of line " + (row + 1) + " was not set to " + expected
                + ". Cell shows [" + cellTextContent(row, colId) + "]; calendars in the page: "
                + driver.findElements(CALENDAR).size() + ", shown: " + isCalendarShown() + ".");
    }

    // ----- lines grid: cells at the end of the row ("الكمية المجانية", "البيان") -----

    /**
     * Sets a cell at the END of the line ({@code freeQuantities}, {@code note}).
     *
     * <p>Why not {@code setLineNumber} / {@code setLineText}: they commit with
     * Enter. QC 2026-11-05 (by hand, new invoice, item صنف5): after Enter in
     * "الكمية المجانية" the value IS kept (114), but the grid moves the editor
     * back to "الكمية" (or adds a new line) and scrolls sideways, so the
     * column is no longer rendered and reading it gives "". The test took
     * that as "not saved" and its retries then met the moved editor
     * ("Editor ... did not open"). Here the value is committed with Enter,
     * the editor the grid then opens elsewhere is closed, the column is
     * scrolled back into view, and the cell is read with {@code textContent}.
     * (TAB was tried on 14:54 and lost the value: see the comment below.)</p>
     *
     * @param numeric compare as numbers (114 = 114.000) instead of as text.
     */
    public void setEndOfLineCell(int row, String colId, String value, boolean numeric) {
        if (!fillLineCellIfEditable(row, colId, value, numeric)) {
            throw new IllegalStateException(colId + " of line " + (row + 1) + " is disabled; it cannot be set to " + value + ".");
        }
    }

    /**
     * Fills cell {@code colId} of line {@code row} when it is EDITABLE and
     * skips it when it is disabled, like
     * {@code PurchaseOrderPage.tryActivateGridCellEditor} (asked by the user,
     * 2026-11-05: "إذا كانت الخلية disabled نتخطى الكود الخاص بها").
     *
     * <p>Steps: scroll the cell into view; open its editor (F2, then a
     * double-click), waiting {@link #CELL_EDITABLE_WAIT} each; no editor →
     * disabled → logged and skipped. Otherwise the value is set by script
     * (native setter + input event) and committed with Enter, as
     * {@code setLineValue} does; the editor the grid then opens elsewhere is
     * closed (untouched); the cell is scrolled back into view and read with
     * {@code textContent}. Up to 3 attempts when the value is not kept.</p>
     *
     * <p>QC run 2026-11-05 15:05: in "الخصومات والإضافات", after the discount
     * was entered, "نسبة الإضافي" did not open (the line holds a discount OR an
     * addition); the old way waited 3 × 3 × 4 s and failed the test.</p>
     *
     * @return true when the value was entered, false when the cell is disabled.
     */
    public boolean fillLineCellIfEditable(int row, String colId, String value, boolean numeric) {

        By editorInput = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "'].ag-cell-inline-editing :is(input, textarea)");
        for (int attempt = 1; attempt <= 3; attempt++) {
            closeEditorsOtherThan(row, colId);
            revealLineCell(row, colId);
            WebElement input = openEditorIfEditable(row, colId, editorInput);
            if (input == null) {
                log.info("Line {} {} is disabled → skipped (value {} not entered).", row + 1, colId, value);
                return false;
            }
            try {
                ((JavascriptExecutor) driver).executeScript(
                        "const i=arguments[0];"
                                + "const set=Object.getOwnPropertyDescriptor(Object.getPrototypeOf(i),'value').set;"
                                + "i.focus(); set.call(i, arguments[1]);"
                                + "i.dispatchEvent(new Event('input',{bubbles:true}));"
                                + "i.dispatchEvent(new Event('change',{bubbles:true}));",
                        input, value);
                input.sendKeys(Keys.ENTER);
            } catch (org.openqa.selenium.StaleElementReferenceException rerendered) {
                log.warn("Editor of {} re-rendered while typing (attempt {}/3).", colId, attempt);
                continue;
            }
            closeEditorsOtherThan(row, colId);
            revealLineCell(row, colId);
            String shown = cellTextContent(row, colId);
            if (sameValue(shown, value, numeric)) {
                log.info("Line {} {} = {}", row + 1, colId, shown);
                return true;
            }
            log.warn("Line {} {} shows [{}] instead of [{}] (attempt {}/3).", row + 1, colId, shown, value, attempt);
        }
        throw new IllegalStateException("Could not set " + colId + " of line " + (row + 1) + " to " + value
                + "; it shows [" + cellTextContent(row, colId) + "].");
    }

    /** Opens the calendar of a date cell (F2, then a double-click); false when none opens → disabled. */
    private boolean openCalendarIfEditable(int row, String colId) {
        By cell = lineCellOf(row, colId);
        for (int gesture = 1; gesture <= 2; gesture++) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.presenceOfElementLocated(cell));
                if (gesture == 1) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
                    new Actions(driver).sendKeys(Keys.F2).perform();
                } else {
                    new Actions(driver).doubleClick(driver.findElement(cell)).perform();
                }
                new WebDriverWait(driver, CELL_EDITABLE_WAIT).pollingEvery(Duration.ofMillis(150))
                        .until(d -> isCalendarShown());
                return true;
            } catch (TimeoutException | org.openqa.selenium.StaleElementReferenceException
                     | org.openqa.selenium.ElementClickInterceptedException
                     | org.openqa.selenium.interactions.MoveTargetOutOfBoundsException notThisWay) {
                // try the other gesture
            }
        }
        return false;
    }

    /**
     * Rich-select cell (AG Grid list, e.g. "الحساب", "التأثير على التكلفة",
     * "العملة" in "الخصومات والإضافات"): a random option, or skipped when the
     * cell is disabled.
     *
     * <p>Why not {@code setLineOption}: it reads the cell with
     * {@code getText()}, which is "" for a cell outside the visible part of
     * the grid. QC run 2026-11-05 15:15: "الحساب" read as empty three times in
     * 0.25 s (the run before chose «الخصم المكتسب» with the same code). The
     * choice is made the same way (keyboard: ↓ a random number of times, then
     * Enter; a mouse click on an option left the cell empty, 2026-11-01), and
     * the cell is read with {@code textContent} after scrolling it into view.</p>
     *
     * @return the option shown in the cell, or "" when the cell is disabled.
     */
    public String selectLineOptionIfEditable(int row, String colId) {

        By editor = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "'].ag-cell-inline-editing [aria-controls^='ag-rich-select-list']");
        for (int attempt = 1; attempt <= 3; attempt++) {
            closeEditorsOtherThan(row, colId);
            revealLineCell(row, colId);
            WebElement wrapper = openEditorIfEditable(row, colId, editor);
            if (wrapper == null) {
                if (attempt == 1) {
                    log.info("Line {} {} is disabled → skipped.", row + 1, colId);
                    return "";
                }
                log.warn("List of {} on line {} did not open (attempt {}/3).", colId, row + 1, attempt);
                continue;
            }
            Number options = (Number) ((JavascriptExecutor) driver).executeScript(
                    "const l=document.getElementById(arguments[0].getAttribute('aria-controls'));"
                            + "return l ? l.querySelectorAll('.ag-rich-select-row').length : 0;", wrapper);
            int count = options == null ? 0 : options.intValue();
            if (count == 0) {
                // The list renders its rows a moment after it opens.
                try {
                    count = new WebDriverWait(driver, Duration.ofSeconds(2)).pollingEvery(Duration.ofMillis(150))
                            .until(d -> {
                                Number n = (Number) ((JavascriptExecutor) d).executeScript(
                                        "const l=document.getElementById(arguments[0].getAttribute('aria-controls'));"
                                                + "return l ? l.querySelectorAll('.ag-rich-select-row').length : 0;", wrapper);
                                return n != null && n.intValue() > 0 ? n.intValue() : null;
                            });
                } catch (TimeoutException | org.openqa.selenium.StaleElementReferenceException none) {
                    log.info("Line {} {} has no options → skipped.", row + 1, colId);
                    stopCellEditing();
                    return "";
                }
            }
            int steps = 1 + random.nextInt(count);
            Actions keys = new Actions(driver);
            for (int i = 0; i < steps; i++) {
                keys.sendKeys(Keys.ARROW_DOWN);
            }
            keys.sendKeys(Keys.ENTER).perform();
            closeEditorsOtherThan(row, colId); // Enter moves the editor to the next cell
            revealLineCell(row, colId);
            String shown = cellTextContent(row, colId);
            if (!shown.isEmpty()) {
                log.info("Line {} {} = {}", row + 1, colId, shown);
                return shown;
            }
            log.warn("Line {} {} is empty after choosing option {} of {} (attempt {}/3).",
                    row + 1, colId, steps, count, attempt);
        }
        throw new IllegalStateException("Could not choose an option in " + colId + " of line " + (row + 1)
                + "; it shows [" + cellTextContent(row, colId) + "].");
    }

    /**
     * Text of cell {@code colId} in line {@code row}, read SAFELY: the cell is
     * scrolled into view first and read with {@code textContent}. Use it
     * instead of {@code lineText} (getText) everywhere a decision depends on
     * the value: getText() gives "" for a cell outside the visible part of
     * the grid (QC runs 2026-11-05 14:45 and 15:15).
     */
    public String readLineCell(int row, String colId) {
        revealLineCell(row, colId);
        return cellTextContent(row, colId);
    }

    /** {@link #readLineCell} as a number (0 when empty or not a number; "1,478.261" → 1478.261). */
    public java.math.BigDecimal readLineNumber(int row, String colId) {
        String text = readLineCell(row, colId).replace(",", "").trim();
        return text.matches("-?\\d+(\\.\\d+)?") ? new java.math.BigDecimal(text) : java.math.BigDecimal.ZERO;
    }

    /**
     * Removes the lines with no item (the grid adds one after Enter on a
     * complete line). Same as {@code deleteEmptyLines}, but item and code are
     * read with {@link #readLineCell}: with getText() a real line whose
     * columns are out of view reads as empty and would be DELETED.
     *
     * @return number of lines removed.
     */
    public int deleteEmptyItemLines() {
        int removed = 0;
        for (int row = lineCount() - 1; row >= 0; row--) {
            if (readLineCell(row, "itemId").isEmpty() && readLineCell(row, "code").isEmpty()) {
                log.info("Line {} has no item (added by the grid) → removing it.", row + 1);
                deleteLine(row);
                removed++;
            }
        }
        return removed;
    }

    /** How long an editor is given to open before the cell counts as disabled (as on orders: 2 s). */
    private static final Duration CELL_EDITABLE_WAIT = Duration.ofSeconds(2);

    /** Opens the editor of the cell (F2, then a double-click); null when none opens → disabled. */
    private WebElement openEditorIfEditable(int row, String colId, By editorInput) {
        By cell = lineCellOf(row, colId);
        for (int gesture = 1; gesture <= 2; gesture++) {
            try {
                WebElement element = new WebDriverWait(driver, Duration.ofSeconds(3))
                        .until(ExpectedConditions.presenceOfElementLocated(cell));
                if (gesture == 1) {
                    ((JavascriptExecutor) driver).executeScript("arguments[0].focus();", element);
                    new Actions(driver).sendKeys(Keys.F2).perform();
                } else {
                    new Actions(driver).doubleClick(driver.findElement(cell)).perform();
                }
                return new WebDriverWait(driver, CELL_EDITABLE_WAIT)
                        .pollingEvery(Duration.ofMillis(150))
                        .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                        .until(d -> d.findElements(editorInput).stream()
                                .filter(WebElement::isDisplayed).findFirst().orElse(null));
            } catch (TimeoutException | org.openqa.selenium.StaleElementReferenceException
                     | org.openqa.selenium.ElementClickInterceptedException
                     | org.openqa.selenium.interactions.MoveTargetOutOfBoundsException notThisWay) {
                // try the other gesture
            }
        }
        return null;
    }

    /**
     * Text of a line cell read with {@code textContent}. Selenium's
     * {@code getText()} returns "" for a cell outside the visible part of the
     * grid (QC run 2026-11-05 14:45: free quantity 114 was saved but read as
     * "" three times; the same was seen on the grouped list, see the guide).
     * "" when the cell is not rendered.
     */
    public String cellTextContent(int row, String colId) {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const c=document.querySelector('.ag-center-cols-container [role=row][row-index=\"'+arguments[0]"
                        + "+'\"] [role=gridcell][col-id=\"'+arguments[1]+'\"]');"
                        + "return c ? c.textContent.trim() : '';",
                row, colId);
        return text == null ? "" : text.toString();
    }

    private static boolean sameValue(String shown, String expected, boolean numeric) {
        if (!numeric) {
            return shown.trim().equals(expected.trim());
        }
        try {
            return new java.math.BigDecimal(shown.replace(",", "").trim())
                    .compareTo(new java.math.BigDecimal(expected)) == 0;
        } catch (NumberFormatException notANumber) {
            return false;
        }
    }

    /**
     * Scrolls the lines grid sideways (both directions: the grid is right to
     * left) until cell {@code colId} of line {@code row} is rendered and fully
     * in view.
     */
    private void revealLineCell(int row, String colId) {
        ensureLineRendered(row);
        Object found = ((JavascriptExecutor) driver).executeAsyncScript(
                "const [row, col, done] = arguments;"
                        + "const sel='.ag-center-cols-container [role=row][row-index=\"'+row+'\"] [col-id=\"'+col+'\"]';"
                        + "const vps=[...document.querySelectorAll('.ag-center-cols-viewport,.ag-body-horizontal-scroll-viewport')]"
                        + "  .filter(v=>v.offsetParent!==null);"
                        + "const steps=[null,0,-400,-800,-1200,-1600,-2000,-2400,-2800,-3200,400,800,1200,1600,2000,2400,2800,3200];"
                        + "let i=0; const next=()=>{"
                        + "  const c=document.querySelector(sel);"
                        + "  if(c){ c.scrollIntoView({block:'nearest', inline:'center'}); setTimeout(()=>done(true),200); return; }"
                        + "  if(i>=steps.length){ done(false); return; }"
                        + "  const x=steps[i++]; if(x!==null) vps.forEach(v=>{v.scrollLeft=x; v.dispatchEvent(new Event('scroll',{bubbles:true}));});"
                        + "  setTimeout(next,200); };"
                        + "next();",
                row, colId);
        if (!Boolean.TRUE.equals(found)) {
            log.warn("Column {} of line {} could not be scrolled into view.", colId, row + 1);
        }
    }

    /**
     * Closes an editor the grid opened by itself on ANOTHER cell (untouched,
     * so Escape cancels nothing). An editor on the target cell is left open.
     */
    private void closeEditorsOtherThan(int row, String colId) {
        By target = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] [col-id='" + colId + "'].ag-cell-inline-editing");
        if (isAnyCellEditorOpen() && driver.findElements(target).isEmpty()) {
            stopCellEditing();
        }
    }

    /** True when cell {@code colId} of line {@code row} shows {@code expected} within {@code timeout}. */
    private boolean waitForCellToShow(int row, String colId, String expected, Duration timeout) {
        try {
            new WebDriverWait(driver, timeout)
                    .pollingEvery(Duration.ofMillis(200))
                    .ignoring(org.openqa.selenium.StaleElementReferenceException.class)
                    .until(d -> cellTextContent(row, colId).contains(expected));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }

    private static final By CELL_EDITOR = By.cssSelector(".ag-cell-inline-editing, .ag-popup-editor");

    private boolean isAnyCellEditorOpen() {
        return driver.findElements(CELL_EDITOR).stream().anyMatch(e -> {
            try {
                return e.isDisplayed();
            } catch (org.openqa.selenium.StaleElementReferenceException gone) {
                return false;
            }
        });
    }

    /**
     * Leaves no editor open after a date was set: if THIS cell is still
     * editing (its popup is open), TAB commits it; an editor the grid then
     * opened on the NEXT cell is untouched, so {@code stopCellEditing}
     * (Escape) only cancels that empty edit.
     */
    private void closeEditorByCommitting(int row, String colId) {
        // Clicking a day commits the date and closes the calendar (checked by hand on
        // QC 2026-11-05). Give it a moment before deciding anything is still open:
        // a TAB sent while it closes moved the grid and hid the next date column.
        try {
            new WebDriverWait(driver, Duration.ofSeconds(1)).pollingEvery(Duration.ofMillis(100))
                    .until(d -> !isCalendarShown() && !isAnyCellEditorOpen());
            return;
        } catch (TimeoutException stillOpen) {
            // handled below
        }
        By thisCellEditing = By.cssSelector(".ag-center-cols-container div[role='row'][row-index='" + row
                + "'] div[role='gridcell'][col-id='" + colId + "'].ag-cell-inline-editing");
        if (!driver.findElements(thisCellEditing).isEmpty() || isCalendarShown()) {
            new Actions(driver).sendKeys(Keys.TAB).perform();
        }
        if (isAnyCellEditorOpen()) {
            stopCellEditing(); // an untouched editor on another cell
        }
    }

    /**
     * Clicks the calendar arrows until its header shows {@code target} (max
     * 36 clicks). The arrow is chosen from the shown month; if a click moves
     * AWAY from the target (arrows mirrored in the right-to-left page), the
     * other arrow is used from then on.
     */
    private void moveCalendarTo(YearMonth target) {
        boolean swapped = false;
        for (int i = 0; i < 36; i++) {
            YearMonth shown = calendarMonth();
            if (shown.equals(target)) {
                return;
            }
            boolean forward = shown.isBefore(target) != swapped;
            visibleCalendar().findElement(forward ? CALENDAR_NEXT : CALENDAR_PREVIOUS).click();
            new WebDriverWait(driver, Duration.ofSeconds(3)).until(d -> !calendarMonth().equals(shown));
            YearMonth now = calendarMonth();
            if (Math.abs(now.until(target, java.time.temporal.ChronoUnit.MONTHS))
                    > Math.abs(shown.until(target, java.time.temporal.ChronoUnit.MONTHS))) {
                swapped = !swapped;
                log.info("Calendar arrows are mirrored → using the other arrow.");
            }
        }
        throw new IllegalStateException("The calendar did not reach " + target + " (shows " + calendarMonth() + ").");
    }

    /** Month shown in the open calendar's header ("November,2026"). */
    private YearMonth calendarMonth() {
        WebElement calendar = visibleCalendar();
        if (calendar == null) {
            throw new IllegalStateException("No calendar is shown.");
        }
        String header = calendar.findElement(CALENDAR_HEADER).getAttribute("textContent").replaceAll("\\s+", "");
        return YearMonth.parse("1," + header, DateTimeFormatter.ofPattern("d,MMMM,yyyy", Locale.ENGLISH));
    }

    // ----- "المستند المرجعي" (a receipt order) -----

    private By referenceDocumentButton(int index) {
        return By.xpath("//input[@data-testid='" + PREFIX + "-referenceDocument']"
                + "/following-sibling::div//button[" + index + "]");
    }

    /**
     * Opens "المستند المرجعي" (receipt orders), selects a random row and confirms.
     *
     * @return the chosen receipt-order number, as shown in the field.
     */
    public String selectRandomReferenceDocument() {
        ui.clickWhenReady(referenceDocumentButton(1));
        String picked = selectRandomRowInOpenDialog();
        confirmDialog();
        String number = new WebDriverWait(driver, WAIT)
                .until(d -> {
                    String value = inputValue("referenceDocument").trim();
                    return value.isBlank() ? null : value;
                });
        log.info("المستند المرجعي = {} (row {})", number, picked);
        return number;
    }

    /** Download button next to "المستند المرجعي": loads the receipt order's data and lines. */
    public void downloadReferenceDocument() {
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(referenceDocumentButton(2)));
        button.click();
        confirmDialogIfShown(Duration.ofSeconds(2));
        ui.waitForGridToSettle();
    }

    // ----- "⋮" of a NEW invoice (clipboardBar, QC 2026-11-05) -----

    /** Items of "⋮" on a new invoice: the Excel import and the continuation from a receipt order. */
    public static final String MENU_IMPORT_EXCEL = "الاستيراد من Excel";
    public static final String MENU_CONTINUE_FROM_RECEIPT = "متابعة من أمر استلام";

    /**
     * "⋮" → "متابعة من أمر استلام": the dialog lists receipt orders (code, branch,
     * warehouse, cost center, currency, total, date) with a radio each; a random
     * one is chosen and confirmed with "موافق". QC 2026-11-05: the order is then
     * shown in "المستند المرجعي" and nothing else is filled until the download
     * button is clicked ({@link #downloadReferenceDocument}); the URL then
     * becomes {@code /add/<receipt id>?typeName=PurchaseReciept}.
     *
     * @return the receipt order number shown in "المستند المرجعي".
     */
    public String continueFromReceiptOrder() {
        clickMenuItem(MENU_CONTINUE_FROM_RECEIPT);
        String picked = selectRandomRowInOpenDialog();
        confirmDialog();
        String number = new WebDriverWait(driver, WAIT)
                .until(d -> {
                    String value = inputValue("referenceDocument").trim();
                    return value.isBlank() ? null : value;
                });
        log.info("متابعة من أمر استلام → {} (dialog row {})", number, picked);
        return number;
    }

    // ----- "جدولة الدفعات" grid (QC 2026-11-05, LPI2611T45-000007: template "سداد 6 اقساط") -----

    /**
     * Rows of the payment schedule, each as col-id → text: paymentTermName,
     * invoicePortion (%), dueAmount, creditDays, dueDate, discountAmount,
     * dueAmountAfterDiscount, discountDueDate, status. Opens the tab. Off-view
     * columns are read by scrolling the grid sideways (AG Grid renders only
     * the columns in view).
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> readPaymentSchedule() {
        openTab(TAB_PAYMENT_SCHEDULING);
        ui.waitForGridToSettle();
        Object rows = ((JavascriptExecutor) driver).executeAsyncScript(
                "const done=arguments[arguments.length-1];"
                        + "const grid=[...document.querySelectorAll('.ag-root')].find(g=>g.offsetParent!==null);"
                        + "if(!grid){done([]);return;}"
                        + "const h=grid.querySelector('.ag-body-horizontal-scroll-viewport')||grid.querySelector('.ag-center-cols-viewport');"
                        + "const out={};"
                        + "const read=()=>grid.querySelectorAll('.ag-center-cols-container [role=row][row-index]').forEach(r=>{"
                        + "  const i=r.getAttribute('row-index'); out[i]=out[i]||{};"
                        + "  r.querySelectorAll('[role=gridcell][col-id]').forEach(c=>out[i][c.getAttribute('col-id')]=c.textContent.trim());});"
                        + "const steps=[0,0.25,0.5,0.75,1];let k=0;"
                        + "const next=()=>{ if(k>=steps.length){ if(h) h.scrollLeft=0;"
                        + "  done(Object.keys(out).sort((a,b)=>a-b).map(i=>out[i])); return; }"
                        + "  if(h){ const max=h.scrollWidth-h.clientWidth; const rtl=getComputedStyle(grid).direction==='rtl';"
                        + "    h.scrollLeft=(rtl?-1:1)*max*steps[k]; h.dispatchEvent(new Event('scroll')); }"
                        + "  k++; setTimeout(()=>{read(); next();},300); };"
                        + "read(); next();");
        return (List<Map<String, String>>) rows;
    }

    /** True when "المستند المرجعي" can be changed (new invoice only, QC 2026-11-05). */
    public boolean isReferenceDocumentEditable() {
        return isInputEditable("referenceDocument");
    }

    // ----- "⋮" follow-up screens (QC 2026-11-05) -----

    /** "⋮" items of a saved invoice (Draft and Approved show the same five). */
    public static final String MENU_EXPORT_EXCEL = "التصدير الى Excel";
    public static final String MENU_PURCHASE_RETURN = "انشاء مردود مشتريات";
    public static final String MENU_DEBIT_NOTE = "إنشاء إشعار مدين";
    public static final String MENU_CREDIT_NOTE = "إنشاء إشعار دائن";
    public static final String MENU_PAY = "دفع";

    /** Screens they open from an Approved invoice (id = the invoice's id). */
    public static final String PURCHASE_RETURN_PATH = "/purchase/invoice-return/add/";
    public static final String DEBIT_NOTE_PATH = "/accounting/notifications-debit/add/";
    public static final String CREDIT_NOTE_PATH = "/accounting/notifications-credit/add/";

    /** "قيمة" of the items-pricing screen for a local invoice: {@code downOpType=5}. */
    public static final String ITEMS_PRICING_TYPE = "downOpType=5";

    // ----- Kanban (cards show supplier, reference, status, amount; NO number) -----

    /**
     * Cards of the Kanban view in screen order, each as supplier / ref /
     * status / amount. QC 2026-11-05: an invoice card has no invoice number
     * (unlike orders), so {@code DocumentList.getKanbanCardCodes} does not
     * apply; a card is a button holding span.text-lg (supplier),
     * span.text-xs (reference document), span.rounded-full (status) and
     * span.text-xl (amount, e.g. "$2000.00").
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, String>> kanbanCards() {
        Object cards = new WebDriverWait(driver, WAIT).pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    Object found = ((JavascriptExecutor) d).executeScript(
                            "return [...document.querySelectorAll('button')]"
                                    + " .filter(b=>b.querySelector(':scope > div span.text-lg') && b.querySelector('span.rounded-full'))"
                                    + " .map(b=>({supplier:b.querySelector('span.text-lg').textContent.trim(),"
                                    + "   ref:(b.querySelector('span.text-xs')||{textContent:''}).textContent.trim(),"
                                    + "   status:b.querySelector('span.rounded-full').textContent.trim(),"
                                    + "   amount:(b.querySelector('span.text-xl')||{textContent:''}).textContent.trim()}));");
                    return found instanceof List<?> list && !list.isEmpty() ? found : null;
                });
        return (List<Map<String, String>>) cards;
    }

    private static final By KANBAN_VIEW_BUTTON = By.xpath(
            "//button[.//*[local-name()='rect' and @width='3' and @height='9']]");

    /**
     * Kanban icon → waits until the grid is gone and invoice cards show.
     * The shared {@code DocumentList.switchToKanbanView} waits for a card
     * number (PO/PR/PQ…), which invoice cards do not have (QC 2026-11-05).
     */
    @Override
    public void switchToKanbanView() {
        new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(KANBAN_VIEW_BUTTON)).click();
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(d -> d.findElements(By.cssSelector(".ag-root")).isEmpty());
        kanbanCards(); // waits for the cards
    }

    /** Double-clicks Kanban card number {@code index} (0-based, screen order); waits for the form. */
    public void openKanbanCardAt(int index) {
        By card = By.xpath("(//button[./div//span[contains(@class,'text-lg')]][.//span[contains(@class,'rounded-full')]])["
                + (index + 1) + "]");
        WebElement element = new WebDriverWait(driver, WAIT).until(ExpectedConditions.elementToBeClickable(card));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({block:'center'});", element);
        new Actions(driver).doubleClick(driver.findElement(card)).perform();
        new WebDriverWait(driver, WAIT).until(ExpectedConditions.urlContains("/view/"));
        waitForDocumentLoaded();
    }

    /** "$2000.00" / "2,090.40" → 2000.00 / 2090.40. */
    public static java.math.BigDecimal amountOf(String text) {
        String digits = text == null ? "" : text.replaceAll("[^0-9.\\-]", "");
        return digits.isEmpty() ? java.math.BigDecimal.ZERO : new java.math.BigDecimal(digits);
    }

    // ----- "قيد اليومية" (Approved invoice) -----

    private static final By OPEN_DIALOG = By.cssSelector("[role='dialog'][data-state='open']");

    /** The journal entry of the opened invoice: number of lines and the debit / credit totals (base currency). */
    public record JournalEntry(int lines, java.math.BigDecimal debit, java.math.BigDecimal credit, String title) {
    }

    /**
     * "قيد اليومية" → reads the dialog (QC 2026-11-05, LPI2611T45-000007: 3 lines,
     * debit 2090.4 = credit 2090.4 = invoice total) and closes it ("إغلاق").
     * The button needs a real click (like "سجل العمليات").
     */
    @SuppressWarnings("unchecked")
    public JournalEntry readJournalEntry() {
        WebElement button = new WebDriverWait(driver, WAIT)
                .until(ExpectedConditions.elementToBeClickable(By.id("journalEntry")));
        ((JavascriptExecutor) driver).executeScript("arguments[0].scrollIntoView({inline:'center', block:'nearest'});", button);
        driver.findElement(By.id("journalEntry")).click();
        Map<String, Object> read = (Map<String, Object>) new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    Object r = ((JavascriptExecutor) d).executeScript(
                            "const dl=[...document.querySelectorAll(\"[role='dialog'][data-state='open']\")]"
                                    + " .find(x=>(x.querySelector('h2')||{}).textContent==='قيد اليومية');"
                                    + "if(!dl) return null;"
                                    + "const rows=[...dl.querySelectorAll('.ag-center-cols-container [role=row]')];"
                                    + "if(!rows.length) return null;"
                                    + "const sum=c=>rows.reduce((s,r)=>s+Number(((r.querySelector('[col-id=\"'+c+'\"]')||{}).textContent||'0').replace(/,/g,'')),0);"
                                    + "return {lines: rows.length, debit: String(sum('baseDebitAmount')), credit: String(sum('baseCreditAmount')),"
                                    + " title: dl.querySelector('h2').textContent};");
                    return r instanceof Map<?, ?> ? r : null;
                });
        JournalEntry entry = new JournalEntry(((Number) read.get("lines")).intValue(),
                new java.math.BigDecimal(String.valueOf(read.get("debit"))),
                new java.math.BigDecimal(String.valueOf(read.get("credit"))), String.valueOf(read.get("title")));
        closeOpenDialogWith("إغلاق");
        log.info("Journal entry: {}", entry);
        return entry;
    }

    // ----- "⋮" → "دفع" (installments dialog) -----

    /** What the "دفع" dialog shows: title, installments, and whether its "دفع" button is enabled. */
    /**
     * What "⋮" → "دفع" opened. QC 2026-11-05: an invoice WITH a payment schedule
     * opens "جدولة الدفعات" (installments; "دفع" disabled until one is ticked); an
     * invoice WITHOUT one (LPI2609T16-000085) opens the direct "دفع" form with the
     * inputs paymentMethod, accountId, cashAmount, currencyId, currencyRatio and
     * descriptions ("سداد فاتورة مشتريات رقم <code> نقدا.").
     */
    public record PayDialog(String title, int installments, boolean payEnabled,
                            List<String> inputs, String description) {
    }

    /** "⋮" → "دفع": reads the dialog without paying (QC 2026-11-05: "جدولة الدفعات", 6 installments). */
    public PayDialog openPayDialog() {
        clickMenuItem(MENU_PAY);
        return readPayDialog();
    }

    /**
     * Reads the open pay dialog once its content is there: installments (or the
     * schedule title) or the inputs of the payment form. QC 2026-11-05: read 10 ms
     * after the click, the form showed its title but no inputs yet. After 8 s the
     * last reading is returned, so the caller's assertion shows what was there.
     */
    @SuppressWarnings("unchecked")
    public PayDialog readPayDialog() {
        Map<String, Object> read = readPayDialogOnce();
        long end = System.currentTimeMillis() + 8_000;
        while (!payDialogLoaded(read) && System.currentTimeMillis() < end) {
            try {
                Thread.sleep(300);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
            read = readPayDialogOnce();
        }
        if (!payDialogLoaded(read)) {
            log.warn("Pay dialog content after 8 s ({} open dialog(s)): {}", read.get("dialogs"), read.get("text"));
        }
        return new PayDialog(String.valueOf(read.get("title")).trim(), ((Number) read.get("rows")).intValue(),
                Boolean.TRUE.equals(read.get("pay")),
                ((List<Object>) read.get("inputs")).stream().map(String::valueOf).toList(),
                String.valueOf(read.get("desc")));
    }

    private static boolean payDialogLoaded(Map<String, Object> read) {
        return ((Number) read.get("rows")).intValue() > 0
                || (!((List<?>) read.get("inputs")).isEmpty() && !String.valueOf(read.get("desc")).isBlank());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readPayDialogOnce() {
        return (Map<String, Object>) new WebDriverWait(driver, Duration.ofSeconds(15))
                .pollingEvery(Duration.ofMillis(300))
                .until(d -> {
                    Object r = ((JavascriptExecutor) d).executeScript(
                            // QC 2026-11-05: the reading found the title but no inputs, so every open
                            // dialog is read (the last one is on top) and its text is kept for the log.
                            "const all=[...document.querySelectorAll(\"[role='dialog'][data-state='open'], [role='alertdialog'][data-state='open']\")];"
                                    + "if(!all.length) return null;"
                                    + "const dl=all[all.length-1];"
                                    + "const q=s=>all.flatMap(d=>[...d.querySelectorAll(s)]);"
                                    + "const rows=q('.ag-center-cols-container [role=row]').length;"
                                    + "const pay=q('button').find(b=>b.textContent.trim()==='دفع');"
                                    + "const inputs=[...new Set(q('input,textarea').map(i=>i.name||i.id).filter(n=>n))];"
                                    + "const desc=q('input,textarea').find(i=>(i.name||i.id)==='descriptions');"
                                    + "return {title:(dl.querySelector('h2')||{}).textContent||'', rows: rows, pay: !!pay && !pay.disabled,"
                                    + " inputs: inputs, desc: desc ? desc.value : '', dialogs: all.length,"
                                    + " text: all.map(d=>d.innerText.replace(/\\s+/g,' ').slice(0,300)).join(' || ')};");
                    return r instanceof Map<?, ?> ? r : null;
                });
    }

    /** Ticks the first installment of the "دفع" dialog (its check box in the pinned column). */
    public void selectFirstInstallment() {
        By box = By.cssSelector("[role='dialog'][data-state='open'] .ag-row[row-index='0'] input[type='checkbox'], "
                + "[role='dialog'][data-state='open'] .ag-row[row-index='0'] [role='checkbox']");
        WebElement element = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(box));
        try {
            element.click();
        } catch (RuntimeException covered) {
            ((JavascriptExecutor) driver).executeScript("arguments[0].click();", element);
        }
    }

    /** Closes the open dialog with its button {@code label} ("إغلاق" / "Close"), and waits until it is gone. */
    public void closeOpenDialogWith(String label) {
        WebElement dialog = new WebDriverWait(driver, WAIT).until(ExpectedConditions.presenceOfElementLocated(OPEN_DIALOG));
        List<WebElement> buttons = dialog.findElements(By.xpath(".//button[normalize-space()=" + xpathLiteral(label)
                + " or normalize-space()='Close']"));
        if (buttons.isEmpty()) {
            new Actions(driver).sendKeys(Keys.ESCAPE).perform();
        } else {
            buttons.get(0).click();
        }
        new WebDriverWait(driver, WAIT).until(d -> d.findElements(OPEN_DIALOG).stream().noneMatch(WebElement::isDisplayed));
    }

    // ----- record filters for DocumentApi -----

    /** Number of the invoice in an API record. */
    public static String codeOf(Map<String, Object> record) {
        return textOf(record, CODE_FIELD);
    }

    public static boolean isDraft(Map<String, Object> record) {
        return !isTrue(record, "isApproved") && !isTrue(record, "isCanceled");
    }

    public static boolean isApproved(Map<String, Object> record) {
        return isTrue(record, "isApproved") && !isTrue(record, "isCanceled");
    }

    public static boolean hasReferenceDocument(Map<String, Object> record) {
        return !textOf(record, "referenceDocument").isBlank();
    }
}
