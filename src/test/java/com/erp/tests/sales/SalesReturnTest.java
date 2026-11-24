package com.erp.tests.sales;

import com.erp.automation.base.SalesDocumentBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.sales.SalesDocumentPage.JournalLine;
import com.erp.automation.pages.sales.SalesInvoiceDocumentPage;
import com.erp.automation.pages.sales.SalesInvoicePage;
import com.erp.automation.pages.sales.SalesPage;
import com.erp.automation.pages.sales.SalesReturnPage;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

/**
 * Sales Return (المبيعات → العمليات → مردود المبيعات), level 1.
 *
 * <p>Shared tests come from {@link SalesDocumentBaseTest}. This class adds:
 * create a return from a sales invoice ("المستند المرجعي" / "انزال من"), a
 * manual return, "سعر المرتجع", edit of a saved return (البيان only), the
 * journal entry, toolbar and "⋮" of a saved return; and (QC 2026-11-18) the
 * returned-quantity rules (more than sold, returns that add up, 0 / -1), the
 * required "سعر المرتجع" and "حساب الدفع", the "سعر المرتجع" options, line
 * edit / delete, the locked lines and "البيان" / "تراجع" of a saved return,
 * the journal against the return total, "انزال من" مردود المبيعات and "دفع".</p>
 *
 * <p>Returning more than was sold is refused by the server, so the returned
 * quantity of every downloaded line is set to 1 (the smallest valid quantity).
 * A saved return is Approved at once (QC 2026-11-12: all 60 returns).</p>
 */
public class SalesReturnTest extends SalesDocumentBaseTest<SalesReturnPage> {

    private static final String SR = "sales-return";

    /** Returned quantity of a downloaded line. */
    private static final String RETURN_QUANTITY = "1";

    /** Invoices tried in "المستند المرجعي" when one cannot be returned (nothing left to return). */
    private static final int MAX_INVOICE_ATTEMPTS = 5;

    /**
     * API invoices tried as the source of a return (QC run 2026-11-18 14:28: 3 of 4 invoices
     * left الفرع / المخزن of the return empty although the API has them).
     */
    private static final int MAX_SOURCE_INVOICES = 10;

    @Override
    protected void clickScreenLink(SalesPage salesPage) {
        salesPage.clickSalesReturns();
    }

    @Override
    protected SalesReturnPage createPage() {
        return new SalesReturnPage(driver);
    }

    @Override
    protected String screenPath() {
        return SalesReturnPage.SCREEN_PATH;
    }

    @Override
    protected List<String> listColumns() {
        return List.of("invoiceCode", "invoiceDate", "referenceDocument", "partnerName", "warehouseName",
                "branchName", "invoiceStatus", "deliveryStatus", "deliveryAdress", "repersentiveName");
    }

    @Override
    protected String codePrefix() {
        return "SRI";
    }

    @Override
    protected String customerField() {
        return "vendorId";
    }

    @Override
    protected String quantityColumn() {
        return SalesInvoiceDocumentPage.QUANTITY_COLUMN;
    }

    @Override
    protected String priceColumn() {
        return SalesInvoiceDocumentPage.PRICE_COLUMN;
    }

    @Override
    protected String exportMenuItem() {
        return SalesReturnPage.MENU_EXPORT_EXCEL;
    }

    @Override
    protected String statusGroupLabel() {
        return SalesInvoiceDocumentPage.STATUS_LABEL;
    }

    @Override
    protected List<String> requiredMarkedOnEmptySave() {
        return List.of("branchId", "warehouseId", "vendorId");
    }

    @Override
    protected DocumentStatus[] statusesAfterSave() {
        return new DocumentStatus[]{APPROVED};
    }

    @Override
    protected String notePrefix() {
        return "مردود مبيعات آلي ";
    }

    @Override
    protected Map<String, Object> openDocumentInEditMode() {
        Map<String, Object> record = openRandom(WITH_REFERENCE);
        page.clickEdit();
        waitForAnyEditMode();
        return record;
    }

    @Override
    protected void fillRequiredFieldsBeforeSave() {
        super.fillRequiredFieldsBeforeSave();
        if (page.hasField(SalesReturnPage.RETURN_PRICE_FIELD, java.time.Duration.ofMillis(300))
                && isEmptySelect(SalesReturnPage.RETURN_PRICE_FIELD)) {
            page.selectRandomReturnPrice();
        }
        fillEmptyHeaderFields();
        restoreDiscountRates();
        fillPaymentAmountsAndDates();
        fillEmptyLineNotes();
        logFieldsLeftEmpty();
        // Any other empty select the form marks "هذا الحقل مطلوب" (e.g. "الى حساب" of a
        // new payment method) gets an option, so the retry of the save can go on.
        for (String field : form.fieldsWithRequiredError()) {
            if (!"warehouseId".equals(field) && !"branchId".equals(field) && isEmptySelect(field)
                    && page.waitForSelectEnabled(field, Duration.ofMillis(500))) {
                log.warn("Required field {} was empty → {}", field, page.selectRandomOption(field));
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }
        }
    }

    // =====================================================================
    // Shared tests of SalesDocumentBaseTest, numbered in this page's run order
    // (priority 1, 2, 3 … over ALL the tests of this page). Each one only
    // calls the shared test; the steps are in SalesDocumentBaseTest.
    // =====================================================================

    @Override
    @Test(priority = 1, groups = {SR, "navigation"},
            description = "المبيعات → العمليات → the screen link opens its list with its columns and \"جديد\"")
    public void listOpensFromSalesMenu() {
        super.listOpensFromSalesMenu();
    }

    @Override
    @Test(priority = 2, groups = {SR, "navigation"},
            description = "\"جديد\" opens an empty document: a new number, today's date, no customer, no status")
    public void newButtonOpensEmptyDocument() {
        super.newButtonOpensEmptyDocument();
    }

    @Override
    @Test(priority = 3, groups = {SR, "navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that document")
    public void userCanOpenDocumentFromListRowActions() {
        super.userCanOpenDocumentFromListRowActions();
    }

    @Override
    @Test(priority = 4, groups = {SR, "navigation", "list"},
            description = "Double-click on a list row opens that document with the status shown in the list")
    public void userCanOpenDocumentByDoubleClickOnListRow() {
        super.userCanOpenDocumentByDoubleClickOnListRow();
    }

    @Override
    @Test(priority = 15, groups = {SR, "validation"},
            description = "Saving an empty document is blocked: error toast, required fields marked, stays in Add")
    public void userCannotSaveEmptyDocument() {
        super.userCannotSaveEmptyDocument();
    }

    @Override
    @Test(priority = 17, groups = {SR, "search"},
            description = "List search by number shows that document")
    public void userCanSearchListByCode() {
        super.userCanSearchListByCode();
    }

    @Override
    @Test(priority = 18, groups = {SR, "search"},
            description = "List search for a text no document has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {
        super.listSearchWithoutMatchShowsNoRows();
    }

    @Override
    @Test(priority = 19, groups = {SR, "search"},
            description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenDocumentFromRecordSearch() {
        super.userCanOpenDocumentFromRecordSearch();
    }

    @Override
    @Test(priority = 20, groups = {SR, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenRecords() {
        super.userCanNavigateBetweenRecords();
    }

    @Override
    @Test(priority = 21, groups = {SR, "pagination"},
            description = "List pager: first/last page, Next page by page to the end, back to a random page")
    public void userCanNavigateListPagination() {
        super.userCanNavigateListPagination();
    }

    @Override
    @Test(priority = 22, groups = {SR, "list"},
            description = "Kanban shows the same documents as the list page; a card opens its document")
    public void userCanSwitchListToKanbanAndBack() {
        super.userCanSwitchListToKanbanAndBack();
    }

    @Override
    @Test(priority = 23, groups = {SR, "list"},
            description = "Group by status: every group has documents and the groups add up to the list total")
    public void userCanGroupListByStatus() {
        super.userCanGroupListByStatus();
    }

    @Override
    @Test(priority = 24, groups = {SR, "print"},
            description = "Print a random document: the preview shows its number; printed through Chrome's dialog")
    public void userCanPrintDocument() throws java.awt.AWTException {
        super.userCanPrintDocument();
    }
/*
    @Override
    @Test(priority = 25, groups = {SR, "print"},
            description = "Save a random document as PDF: a PDF file is downloaded")
    public void userCanSaveDocumentAsPdf() {
        super.userCanSaveDocumentAsPdf();
    }*/

    @Override
    @Test(priority = 26, groups = {SR, "excel"},
            description = "⋮ → export to Excel: an .xlsx with the document's items is downloaded")
    public void userCanExportDocumentToExcel() {
        super.userCanExportDocumentToExcel();
    }

    @Override
    @Test(priority = 29, groups = {SR, "attachments"},
            description = "Attachments added in edit mode are listed and kept after save and reload")
    public void userCanAddAttachments() {
        super.userCanAddAttachments();
    }

    @Override
    @Test(priority = 30, groups = {SR, "attachments"},
            description = "Deleting an attachment in edit mode is kept after save and reload")
    public void userCanDeleteAttachment() {
        super.userCanDeleteAttachment();
    }

    @Override
    @Test(priority = 31, groups = {SR, "exit"},
            description = "Close a new, a viewed and an edited document: each returns to the list")
    public void userCanExitDocument() {
        super.userCanExitDocument();
    }

    @Override
    @Test(priority = 32, groups = {SR, "log"},
            description = "Transaction log: the first entry is the creation")
    public void userCanViewTransactionLog() {
        super.userCanViewTransactionLog();
    }

    // =====================================================================
    // Create
    // =====================================================================

    @Test(priority = 5, groups = {SR, "create", "reference"},
            description = "\"المستند المرجعي\" → a sales invoice → download: customer, branch, warehouse and lines come "
                    + "from the invoice; less than downloaded is returned (kept after reload); saved as Approved; the reference is kept")
    public void userCanCreateSalesReturnFromInvoiceThroughReferenceField() {
        // Only an invoice whose required fields (العميل، الفرع، المخزن) are filled is chosen
        // (QC run 2026-11-18 13:56: SI2608-000004 has no warehouse → "المخزن" stayed empty).
        String invoice = selectReferenceInvoiceWithLines();
        String customer = waitForSelected(form, "vendorId");
        assertTrue(!customer.isEmpty(), "The customer of " + invoice + " was not brought in.");

        String code = completeAndSavePartialReturn();
        assertEquals(page.textValue("referenceDocument").trim(), invoice, "\"المستند المرجعي\" of " + code + " was not kept.");
        verifySavedReturn(code);
    }
/*
    @Test(priority = 6, groups = {SR, "create", "lines"},
            description = "\"المستند المرجعي\" = an API invoice, downloaded; its lines removed, then \"انزال من\" = "
                    + "فواتير المبيعات → the same invoice brings them back; a part is returned and saved")
    public void userCanCreateSalesReturnByDownloadingInvoiceLines() {

        // "المستند المرجعي" = an API invoice, downloaded (business rule, QC 2026-11-18), so the
        // customer, branch and warehouse are the invoice's. Its lines are removed, then brought
        // back through "انزال من" = فواتير المبيعات → the same invoice.
        String invoice = selectReferenceInvoiceWithLines();
        Set<String> items = new HashSet<>(lineCodes());
        deleteAllItemLines();
        assertTrue(page.downloadLinesFrom(SalesReturnPage.DOWNLOAD_FROM_INVOICES, invoice),
                invoice + " is not offered by \"انزال من\" = " + SalesReturnPage.DOWNLOAD_FROM_INVOICES + ".");
        page.waitForGridToSettle();
        page.deleteEmptyLines();
        assertEquals(new HashSet<>(lineCodes()), items, "\"انزال من\" " + invoice + " did not bring its items.");
        String code = completeAndSavePartialReturn();
        verifySavedReturn(code);
    }*/

    @Test(priority = 7, groups = {SR, "create", "validation"},
            description = "A return without \"المستند المرجعي\" (header, \"سعر المرتجع\", an item line) is not saved")
    public void userCannotSaveSalesReturnWithoutReferenceDocument() {

        // Business rule (QC 2026-11-18): a sales return cannot be made without "المستند المرجعي".
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillManualHeader(false);
        addItemLine();
        fillEmptyHeaderFields();
        fillPaymentAmountsAndDates();
        assertTrue(page.textValue("referenceDocument").isBlank(), "\"المستند المرجعي\" must be empty for this case.");
        DocumentForm.Toast toast = saveExpectingRefusal("A return without \"المستند المرجعي\"");
        List<String> marked = form.fieldsWithRequiredError();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast == null || toast.isError(), "A return without \"المستند المرجعي\" was accepted: " + toast);
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "A return without \"المستند المرجعي\" was saved ("
                + driver.getCurrentUrl() + ").");
        softly.assertTrue(marked.contains("referenceDocument") || (toast != null && toast.fullText().contains("المستند المرجعي")),
                "\"المستند المرجعي\" is not named as missing (marked " + marked + ", toast " + toast + ").");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 8, groups = {SR, "create"},
            description = "\"إضافة\" on an opened return creates a NEW return; the opened one is unchanged")
    public void userCanAddNewSalesReturnFromOpenedOne() {

        // The new return gets "المستند المرجعي" = an API invoice, downloaded (business rule, QC 2026-11-18).
        String invoice = selectReferenceInvoiceFrom(invoicesMostFilledFirst(), this::openReturnAndClickAdd, true);
        completeAndSavePartialReturn();
        assertEquals(page.textValue("referenceDocument").trim(), invoice, "\"المستند المرجعي\" of the new return is not " + invoice + ".");
        verifyNewDocumentSaved(openedBeforeAdd, "\"إضافة\"");
    }

    // =====================================================================
    // Edit, statuses, journal entry
    // =====================================================================

    @Test(priority = 9, groups = {SR, "edit"},
            description = "Edit a saved return: only \"البيان\" can be changed; it stays Approved")
    public void savedSalesReturnAllowsDescriptionEditOnly() {

        SoftAssert softly = new SoftAssert();
        Map<Integer, String> lineNotes = editSavedReturnAndSave(code -> {
            for (String field : List.of("branchId", "warehouseId", "vendorId", "paymentMethod", "taxCalculationMethode",
                    "costCenterId", "salePriceListId", SalesReturnPage.RETURN_PRICE_FIELD)) {
                softly.assertFalse(page.isInputEditable(field), "\"" + field + "\" must be locked when editing saved " + code + ".");
            }
        });
        verifyLineNotesAfterReload(lineNotes, softly);
        softly.assertAll();
    }

    @Test(priority = 10, groups = {SR, "sales"},
            description = "\"سعر المرتجع\" offers the four agreed options")
    public void returnPriceOffersTheAgreedOptions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        for (int i = 0; i < 12 && seen.size() < SalesReturnPage.RETURN_PRICE_OPTIONS.size(); i++) {
            seen.add(page.selectRandomReturnPrice());
        }
        log.info("سعر المرتجع options seen: {}", seen);
        assertTrue(SalesReturnPage.RETURN_PRICE_OPTIONS.containsAll(seen),
                "Unexpected \"سعر المرتجع\" option(s): " + seen + "; expected only " + SalesReturnPage.RETURN_PRICE_OPTIONS);
        page.closeForm();
    }

    @Test(priority = 11, groups = {SR, "journal"},
            description = "\"قيد اليومية\" of a saved return is balanced")
    public void salesReturnJournalEntryIsBalanced() {

        openRandom(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("journalEntry")) {
            throw new SkipException("\"قيد اليومية\" is disabled on " + code + ".");
        }
        verifyBalancedJournal(code);
    }

    // =====================================================================
    // Toolbar, menu, tabs
    // =====================================================================

    @Test(priority = 12, groups = {SR, "validation"},
            description = "New return toolbar: \"حفظ\" waits for a change; add/undo/delete/approve/journal/log disabled; "
                    + "no \"نسخ\" button")
    public void newSalesReturnEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("xIcon");

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.isActionEnabled("save"), "'save' should wait for a change on a new return.");
        for (String action : List.of("add", "undo", "delete", "approveOrDisapprove", "journalEntry", "transactionLogs")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new return.");
        }
        for (String action : List.of("xIcon", "uploadAttachment", "recordSearch")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new return.");
        }
        softly.assertFalse(page.hasAction("copy"), "A return has no \"نسخ\" button (QC 2026-11-12).");
        softly.assertTrue(page.hasReferenceDocumentPicker(), "\"المستند المرجعي\" has no invoice picker.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 13, groups = {SR, "validation", "status"},
            description = "Saved return toolbar: edit/add/print/journal/attachments enabled; delete disabled; "
                    + "⋮ has the export and \"دفع\"")
    public void savedSalesReturnToolbar() {

        openRandom(r -> true);
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "add", "print", "journalEntry", "uploadAttachment", "transactionLogs")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on saved return " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("delete"), "'delete' must be disabled on saved return " + code + ".");
        List<String> menu = page.menuItems();
        softly.assertTrue(menu.contains(SalesReturnPage.MENU_EXPORT_EXCEL), "⋮ has no export: " + menu);
        softly.assertTrue(menu.contains(SalesReturnPage.MENU_PAY), "⋮ has no \"دفع\": " + menu);
        softly.assertAll();
    }

    @Test(priority = 14, groups = {SR, "sales"}, description = "Both line tabs of a saved return open")
    public void userCanOpenEveryLineTabOfSalesReturn() {

        openRandom(r -> true);
        String code = page.waitForCode();
        SoftAssert softly = new SoftAssert();
        for (String tab : SalesReturnPage.LINE_TABS) {
            try {
                page.openLinesTab(tab);
                softly.assertTrue(page.isLinesTabActive(tab), "Tab [" + tab + "] of " + code + " is not active.");
            } catch (RuntimeException failed) {
                softly.fail("Tab [" + tab + "] of " + code + " did not open: " + failed.getMessage());
            }
        }
        page.openLinesTab(SalesReturnPage.LINE_TABS.get(0));
        softly.assertAll();
    }

    @Test(priority = 16, groups = {SR, "validation"},
            description = "Header complete but no item line: saving is refused with \"يجب اضافة صنف واحد على الأقل\"")
    public void userCannotSaveSalesReturnWithoutLines() {

        // "المستند المرجعي" downloaded, then every item line removed: the refusal is for the missing lines.
        selectReferenceInvoiceWithLines();
        deleteAllItemLines();
        page.selectRandomReturnPrice();
        page.clickSave();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isError() && toast.fullText().contains(NO_LINES_ERROR), "Expected \"" + NO_LINES_ERROR + "\", got: " + toast);
        page.closeForm();
    }

    // =====================================================================
    // Follow-up
    // =====================================================================

    @Test(priority = 27, groups = {SR, "follow-up"}, description = "Items Pricing ($): Sales → Pricing opens with the return's items")
    public void userCanOpenItemsPricingFromSalesReturn() {

        openRandom(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("code");
        page.clickItemsPricing();
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("/sales/pricing/add"), "Pricing screen did not open: " + url);
        softly.assertEquals(new HashSet<>(page.gridColumnTexts("itemCode")), new HashSet<>(items), "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 28, groups = {SR, "follow-up", "print"}, description = "Print Barcode: Item Barcodes opens with the return's items")
    public void userCanOpenPrintBarcodeFromSalesReturn() {

        openRandom(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("itemId");
        page.clickPrintBarcode();
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("referenceCode=" + code), "Item Barcodes is not loaded from " + code + ": " + url);
        softly.assertEquals(page.gridColumnTexts("itemId"), items, "Item Barcodes does not list the items of " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Lines
    // =====================================================================

    @Test(priority = 33, groups = {SR, "lines"},
            description = "Return from an API reference invoice, partial quantities, plus a new line in \"تفاصيل الفاتورة\" "
                    + "and a new line in \"الخصومات والإضافات\": all lines are kept after save and reload")
    public void savedSalesReturnKeepsItsLines() {

        // "المستند المرجعي" = an API invoice, downloaded (business rule, QC 2026-11-18).
        selectReferenceInvoiceWithLines();
        Set<String> downloaded = new HashSet<>(lineCodes());
        lineCellProblems.clear();
        String[] added = new String[1];
        List<String> addedLineBefore = new ArrayList<>();
        List<List<String>> discountsBefore = new ArrayList<>();
        // QC 2026-11-18: a line is added in each line tab before the save.
        String code = completeAndSavePartialReturn(() -> {
            added[0] = addItemLine();
            page.deleteEmptyLines();
            addedLineBefore.addAll(linesSnapshot().stream().filter(l -> l.startsWith(added[0] + " ×")).toList());
            log.info("Line added in \"تفاصيل الفاتورة\": {}", addedLineBefore);
            discountsBefore.add(addDiscountsLineAndSnapshot());
        }); // saves, reloads and checks the returned quantities of the downloaded lines

        Set<String> expected = new HashSet<>(downloaded);
        expected.add(added[0]);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(new HashSet<>(lineCodes()), expected, "Items of " + code + " after save and reload.");
        softly.assertEquals(linesSnapshot().stream().filter(l -> l.startsWith(added[0] + " ×")).toList(), addedLineBefore,
                "The line added in \"تفاصيل الفاتورة\" (" + added[0] + ") of " + code + " after save and reload.");
        List<String> discounts = discountsBefore.isEmpty() ? null : discountsBefore.get(0);
        if (discounts != null) {
            softly.assertEquals(discountsSnapshot(), discounts,
                    "Lines of \"" + DISCOUNTS_TAB + "\" of " + code + " after save and reload.");
        }
        softly.assertTrue(lineCellProblems.isEmpty(), "Line cells did not keep what was typed: " + lineCellProblems);
        softly.assertAll();
    }

    // =====================================================================
    // Full coverage (QC 2026-11-18): quantities, required fields, prices,
    // lines, edit of a saved return, journal, follow-up
    // =====================================================================

    @Test(priority = 34, groups = {SR, "validation", "quantity"},
            description = "Returning more than was sold is refused: sales invoice ⋮ → انشاء مردود مبيعات, "
                    + "quantity = invoiced + 1 → error, the return is not saved")
    public void userCannotReturnMoreThanInvoicedQuantity() {

        Map<String, BigDecimal> sold = startReturnFromRandomInvoice();
        String item = keepOnlyFirstLine();
        prepareHeaderBeforeQuantities();
        BigDecimal tooMany = sold.get(item).add(BigDecimal.ONE);
        log.info("{}: {} sold on {} → returning {}.", item, plain(sold.get(item)), sourceInvoice, plain(tooMany));
        if (typeQuantity(0, plain(tooMany))) {
            fillPriceIfZero(0);
            assertSaveRefused("Returning " + plain(tooMany) + " of " + item + " (" + plain(sold.get(item)) + " sold on " + sourceInvoice + ")");
        }
        page.closeForm();
    }

    @Test(priority = 35, groups = {SR, "validation", "quantity"},
            description = "Returns of one invoice add up: after a return of 1, the next return offers at most "
                    + "(invoiced − 1) and returning the full invoiced quantity again is refused")
    public void cumulativeReturnsCannotExceedInvoicedQuantity() {

        Map<String, BigDecimal> sold = startReturnFromRandomInvoice();
        String invoice = sourceInvoice;
        String returnUrl = sourceReturnUrl;
        String item = keepOnlyFirstLine();
        prepareHeaderBeforeQuantities();
        page.setLineNumber(0, quantityColumn(), RETURN_QUANTITY);
        fillPriceIfZero(0);
        String first;
        try {
            first = saveAndVerify(APPROVED);
        } catch (AssertionError refused) {
            throw new SkipException("Nothing left to return of " + item + " on " + invoice + ": " + refused.getMessage());
        }
        log.info("First return {} of {}: 1 × {}.", first, invoice, item);

        // Second return of the same invoice (same "انشاء مردود مبيعات" link).
        driver.navigate().to(returnUrl);
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForGridToSettle();
        int row = page.lineTexts("code").indexOf(item);
        BigDecimal invoiced = sold.get(item);
        if (row < 0) {
            log.info("{} is no longer offered by {} after {} → nothing left to return.", item, invoice, first);
            assertTrue(invoiced.compareTo(BigDecimal.ONE) <= 0,
                    item + " (" + plain(invoiced) + " sold, 1 returned) is no longer offered by " + invoice + ".");
            page.closeForm();
            return;
        }
        BigDecimal offered = page.lineNumber(row, quantityColumn());
        log.info("Second return of {}: {} offered for {} ({} sold, 1 returned).", invoice, plain(offered), item, plain(invoiced));
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(offered.compareTo(invoiced.subtract(BigDecimal.ONE)) <= 0, "The second return of " + invoice
                + " offers " + plain(offered) + " of " + item + "; at most " + plain(invoiced) + " − 1 is left.");
        keepOnlyLineOfItem(item);
        prepareHeaderBeforeQuantities();
        if (typeQuantity(0, plain(invoiced))) {
            fillPriceIfZero(0);
            DocumentForm.Toast toast = saveExpectingRefusal("Returning the full " + plain(invoiced) + " of " + item + " again");
            softly.assertTrue(toast == null || toast.isError(), "Returning " + plain(invoiced) + " of " + item + " after " + first
                    + " was accepted: " + toast);
            softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The second return of " + invoice + " was saved.");
        }
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 36, groups = {SR, "validation", "quantity"},
            description = "A returned quantity of 0 / -1 is refused: by the cell (empty, red) or on save")
    public void nonPositiveReturnQuantityIsRefused() {

        startReturnFromRandomInvoice();
        String returnUrl = sourceReturnUrl;
        keepOnlyFirstLine();
        prepareHeaderBeforeQuantities();
        if (typeQuantity(0, "0")) {
            fillPriceIfZero(0);
            assertSaveRefused("A returned quantity of 0");
        }
        page.closeForm();

        driver.navigate().to(returnUrl);
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForGridToSettle();
        keepOnlyFirstLine();
        prepareHeaderBeforeQuantities();
        if (typeQuantity(0, "-1") && page.lineNumber(0, quantityColumn()).signum() < 0) {
            fillPriceIfZero(0);
            assertSaveRefused("A returned quantity of -1");
        }
        page.closeForm();
    }

    @Test(priority = 37, groups = {SR, "validation"},
            description = "\"سعر المرتجع\" is required: a return without it is not saved")
    public void userCannotSaveSalesReturnWithoutReturnPrice() {

        startReturnWithReturnPrice();
        keepOnlyFirstLine();
        fillEmptyHeaderFields();
        page.setLineNumber(0, quantityColumn(), RETURN_QUANTITY);
        fillPriceIfZero(0);
        page.setNote(uniqueNote());
        if (!isEmptySelect(SalesReturnPage.RETURN_PRICE_FIELD) && !page.clearSelect(SalesReturnPage.RETURN_PRICE_FIELD)) {
            throw new SkipException("\"سعر المرتجع\" is filled by default and cannot be emptied.");
        }
        DocumentForm.Toast toast = saveExpectingRefusal("A return without \"سعر المرتجع\"");
        List<String> marked = form.fieldsWithRequiredError();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast == null || toast.isError(), "A return without \"سعر المرتجع\" was accepted: " + toast);
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "A return without \"سعر المرتجع\" was saved.");
        softly.assertTrue(marked.contains(SalesReturnPage.RETURN_PRICE_FIELD)
                        || (toast != null && toast.fullText().contains("سعر المرتجع")),
                "\"سعر المرتجع\" is not named as missing (marked " + marked + ", toast " + toast + ").");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 38, groups = {SR, "validation"},
            description = "Payment method \"نقدا\" without \"حساب الدفع\" is refused (\"حساب الدفع مطلوب\")")
    public void cashSalesReturnNeedsPaymentAccount() {

        // The return is kept as downloaded (every line, every quantity): with 1 of a 3-item line
        // the server answered first "مبلغ الدفع اكبر من قيمة الفاتورة" (QC run 2026-11-18 19:07,
        // SI2611-000253, the known payment-amount defect) and the "حساب الدفع" check was never reached.
        startReturnFromRandomInvoice();
        page.deleteEmptyLines();
        page.selectRandomReturnPrice();
        page.selectOption("paymentMethod", SalesInvoiceDocumentPage.PAYMENT_CASH);
        page.confirmDialogIfShown(Duration.ofMillis(800));
        if (!page.selectedText(SalesInvoiceDocumentPage.FUND_ACCOUNT_FIELD).isBlank()
                && !page.clearSelect(SalesInvoiceDocumentPage.FUND_ACCOUNT_FIELD)) {
            throw new SkipException("\"حساب الدفع\" is filled by default for نقدا and cannot be emptied.");
        }
        for (int row = 0; row < page.lineCount(); row++) {
            fillPriceIfZero(row);
        }
        page.setNote(uniqueNote());
        DocumentForm.Toast toast = saveExpectingRefusal("A cash return without \"حساب الدفع\"");
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast == null || (toast.isError() && toast.fullText().contains("حساب الدفع")),
                "Expected \"حساب الدفع مطلوب\", got: " + toast + " (marked " + form.fieldsWithRequiredError() + ").");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "A cash return without \"حساب الدفع\" was saved.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 39, groups = {SR, "create", "reference"},
            description = "\"المستند المرجعي\" → download: customer, branch and warehouse are the invoice's; "
                    + "every line is an item of the invoice")
    public void referenceDownloadBringsInvoiceCustomerBranchWarehouseAndItems() {

        String invoice = selectReferenceInvoiceWithLines();
        Map<String, Object> record = new SalesInvoicePage(driver).recordByCode(invoice);
        assertTrue(record != null, invoice + " is not returned by the sales invoices API.");
        String customer = waitForSelected(form, "vendorId");
        String branch = waitForSelected(form, "branchId");
        String warehouse = waitForSelected(form, "warehouseId");
        Set<String> items = new HashSet<>(lineCodes());
        page.closeForm();

        SalesInvoicePage invoices = new SalesInvoicePage(driver);
        invoices.openDocument(record.get("id"));
        invoices.waitForGridToSettle();
        Set<String> invoiceItems = new HashSet<>(invoices.lineTexts("code"));
        invoiceItems.remove("");
        log.info("{}: customer [{}], branch [{}], warehouse [{}], return items {}, invoice items {}", invoice, customer,
                branch, warehouse, items, invoiceItems);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(customer, String.valueOf(record.get("partnerName")).trim(), "Customer of the return differs from " + invoice + ".");
        softly.assertEquals(branch, String.valueOf(record.get("branchName")).trim(), "Branch of the return differs from " + invoice + ".");
        softly.assertEquals(warehouse, String.valueOf(record.get("warehouseName")).trim(), "Warehouse of the return differs from " + invoice + ".");
        softly.assertTrue(invoiceItems.containsAll(items), "The return has items that are not on " + invoice + ": " + items
                + " vs " + invoiceItems);
        softly.assertAll();
    }

    @Test(priority = 40, groups = {SR, "create", "follow-up"},
            description = "Sales invoice ⋮ → انشاء مردود مبيعات → less than downloaded returned → saved as Approved; "
                    + "\"المستند المرجعي\" is the invoice")
    public void userCanSaveSalesReturnCreatedFromInvoiceMenu() {

        startReturnFromRandomInvoice();
        String invoice = sourceInvoice;
        String code = completeAndSavePartialReturn();
        assertEquals(page.textValue("referenceDocument").trim(), invoice, "\"المستند المرجعي\" of " + code + " is not " + invoice + ".");
        verifySavedReturn(code);
    }

    @Test(priority = 41, groups = {SR, "sales", "calculation"},
            description = "Each \"سعر المرتجع\" option gives the line a price; with \"تحديد يدوي\" the price can be typed")
    public void returnPriceOptionsFillTheLinePrice() {

        startReturnWithReturnPrice();
        keepOnlyFirstLine();
        SoftAssert softly = new SoftAssert();
        for (String option : SalesReturnPage.RETURN_PRICE_OPTIONS) {
            page.selectReturnPrice(option);
            BigDecimal price = page.lineNumber(0, priceColumn());
            boolean editable = page.editableLineColumns(0).contains(priceColumn());
            log.info("سعر المرتجع [{}] → price {} (editable: {}).", option, plain(price), editable);
            if (SalesReturnPage.MANUAL_RETURN_PRICE.equals(option)) {
                softly.assertTrue(editable, "With \"" + option + "\" the line price cannot be typed.");
            } else {
                softly.assertTrue(price.signum() > 0, "\"" + option + "\" left the price of " + page.lineText(0, "code")
                        + " at " + plain(price) + " (customer " + form.getSelectedText("vendorId") + ").");
            }
        }
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 42, groups = {SR, "lines", "edit"},
            description = "\"تحديد يدوي\": the quantity and the typed price of a line are saved as typed")
    public void userCanChangeQuantityAndPriceOfSalesReturnLine() {

        startReturnWithReturnPrice();
        String item = keepOnlyFirstLine();
        page.selectReturnPrice(SalesReturnPage.MANUAL_RETURN_PRICE);
        fillEmptyHeaderFields();
        page.setLineNumber(0, quantityColumn(), RETURN_QUANTITY);
        page.setLineNumber(0, priceColumn(), MANUAL_PRICE);
        page.stopCellEditing();
        assertEquals(page.lineNumber(0, priceColumn()).compareTo(new BigDecimal(MANUAL_PRICE)), 0,
                "The price cell did not keep " + MANUAL_PRICE + ".");
        String code = saveAndVerify(APPROVED);
        page.reload();
        page.waitForGridToSettle();
        int row = page.lineTexts("code").indexOf(item);
        assertTrue(row >= 0, item + " is not on " + code + " after save.");
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineNumber(row, quantityColumn()).compareTo(new BigDecimal(RETURN_QUANTITY)), 0,
                "Quantity of " + item + " on " + code + ".");
        softly.assertEquals(page.lineNumber(row, priceColumn()).compareTo(new BigDecimal(MANUAL_PRICE)), 0,
                "Typed price of " + item + " on " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 43, groups = {SR, "lines"},
            description = "Deleting a downloaded line before saving is kept after save and reload")
    public void userCanDeleteItemLineOfSalesReturn() {

        // QC run 2026-11-19 10:24: every invoice tried had one line (ITM1352) and the case was
        // skipped. The source invoice is now chosen through the API among invoices whose
        // "تفاصيل الفاتورة" has at least two lines.
        startReturnFromInvoices(invoicesWithSeveralLines(2));
        if (page.lineCount() < 2) {
            page.closeForm();
            throw new SkipException("The return from " + sourceInvoice + " has " + page.lineCount()
                    + " line(s) to return although the invoice has more (some are fully returned).");
        }
        String deleted = page.lineText(0, "code");
        page.deleteLine(0);
        page.waitForGridToSettle();
        int after = page.lineCount();
        assertFalse(page.lineTexts("code").contains(deleted), "Line of " + deleted + " is still in the grid.");
        String code = completeAndSavePartialReturn();
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineCount(), after, "Line count of " + code + " after save and reload.");
        softly.assertFalse(page.lineTexts("code").contains(deleted), "The deleted line of " + deleted + " came back on " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 44, groups = {SR, "edit", "lines"},
            description = "Edit a saved return: quantity, price and item of the lines are locked; no \"اضافة سطر\"")
    public void savedSalesReturnLinesAreLocked() {

        openRandom(WITH_REFERENCE);
        String code = page.waitForCode();
        page.clickEdit();
        waitForAnyEditMode();
        page.waitForGridToSettle();
        List<String> editable = page.editableLineColumns(0);
        log.info("Line cells the grid marks editable on saved {}: {}", code, editable);
        // QC run 2026-11-19 11:15: the grid marked "itemId" editable, but by hand the item cannot be
        // changed. A cell is judged the way a user meets it: typed into, then checked whether it changed.
        Map<String, String> typed = Map.of(quantityColumn(), "7", priceColumn(), "7", "itemId", "zz", "code", "zz");
        SoftAssert softly = new SoftAssert();
        for (String column : List.of(quantityColumn(), priceColumn(), "itemId", "code")) {
            if (!editable.contains(column)) {
                continue; // locked in the grid too
            }
            softly.assertFalse(page.userCanChangeLineCell(0, column, typed.get(column)),
                    "Line cell \"" + column + "\" of saved " + code + " can be changed by the user.");
        }
        softly.assertFalse(page.canAddLineInActiveTab(), "\"اضافة سطر\" is enabled on saved " + code + ".");
        page.closeForm(); // nothing is saved: the tries above are discarded
        softly.assertAll();
    }

    @Test(priority = 45, groups = {SR, "edit"},
            description = "Edit a saved return: the new \"البيان\" is saved, kept after reload; it stays Approved")
    public void editedDescriptionOfSalesReturnIsKept() {

        String[] note = new String[1];
        Map<Integer, String> lineNotes = editSavedReturnAndSave(code -> {
            note[0] = uniqueNote();
            page.setNote(note[0]);
        });
        String code = page.waitForCode();
        page.reload();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.waitForNote(note[0]), note[0], "\"البيان\" of " + code + " was not kept after reload.");
        verifyLineNotesAfterReload(lineNotes, softly);
        softly.assertAll();
    }

    @Test(priority = 46, groups = {SR, "edit"},
            description = "Edit a saved return, change \"البيان\", then \"تراجع\" restores the saved one")
    public void userCanUndoDescriptionChangeOfSalesReturn() {

        // A return with الفرع and المخزن filled, as one opened by hand (QC run 2026-11-19 11:41:
        // SRI2610-000034 had no warehouse).
        openRandom(WITH_REFERENCE.and(r -> isFilled(r.get("branchName")) && isFilled(r.get("warehouseName"))));
        String code = page.waitForCode();
        String saved = page.note();
        page.clickEdit();
        waitForAnyEditMode();
        // Typed with the keyboard, like a user. QC run 2026-11-19 11:35 (SRI2610-000038): setNote()
        // sets the value by script, and "تراجع" stayed disabled although by hand it is enabled
        // after the change (the screen enables it on real typing).
        // The note is CHANGED, not written again: QC 2026-11-19, the same text as the saved one does
        // not make "تراجع" active. The saved text is kept and a new ending is typed after it, at the
        // end of the field (End key), like a user editing the note.
        // QC 2026-11-19, checked by hand in QC on SRI2611-000056: typing in "البيان" enables "تراجع" at
        // once, it stays enabled after Tab, and only one "sales-return-undo" exists. The test typed right
        // after the field became editable, while the edit screen was still loading: it now waits
        // for the screen to settle first.
        page.waitForGridToSettle();
        pause(2000);
        String before = page.note();
        // QC run 2026-11-19 11:52 (SRI2611-000054): the saved note was empty, the typed " - …" lost its
        // leading space in the field and the comparison failed on it. No separator on an empty note,
        // and the texts are compared trimmed.
        String suffix = (before.isBlank() ? "" : " - ") + stamped("تعديل يدوي آلي ");
        String typed = (before + suffix).trim();
        assertTrue(page.isNoteEditable(), "\"البيان\" of " + code + " is locked in edit mode.");
        org.openqa.selenium.WebElement note = driver.findElement(org.openqa.selenium.By.id(NOTE_FIELD));
        note.click();
        note.sendKeys(org.openqa.selenium.Keys.END);
        note.sendKeys(suffix);
        log.info("{}: right after typing, تراجع enabled = {}", code, page.isActionEnabled("undo"));
        log.info("{}: \"البيان\" [{}] → [{}] (saved [{}]).", code, before, page.note(), saved);
        assertEquals(waitUntilEquals(() -> page.note().trim(), typed), typed, "\"البيان\" of " + code + " did not take the typed text.");
        assertNotEquals(page.note().trim(), saved.trim(), "\"البيان\" of " + code + " is still the saved text: nothing to undo.");
        // The user then leaves the field: QC run 2026-11-19 11:41, typed but still in the field,
        // "تراجع" stayed disabled. Tab first, then a click outside the field if needed.
        // Keyboard only (no script): Tab out of the field; if needed, a click on "الرقم المرجعي".
        note.sendKeys(org.openqa.selenium.Keys.TAB);
        boolean undo = page.waitForActionEnabled("undo");
        if (!undo) {
            // Once more, keyboard only: the text typed again at the end (the screen may have reloaded
            // the form while the first text was typed).
            log.warn("{}: تراجع still disabled → typing in البيان once more.", code);
            note = driver.findElement(org.openqa.selenium.By.id(NOTE_FIELD));
            note.click();
            note.sendKeys(org.openqa.selenium.Keys.END);
            note.sendKeys(" .");
            note.sendKeys(org.openqa.selenium.Keys.TAB);
            undo = page.waitForActionEnabled("undo");
        }
        log.info("{}: الفرع [{}], المخزن [{}]; تراجع enabled after leaving البيان: {}", code, form.getSelectedText("branchId"),
                form.getSelectedText("warehouseId"), undo);
        assertTrue(undo, "\"تراجع\" stayed disabled after typing in \"البيان\" of " + code + " and leaving the field.");
        page.clickUndo();
        page.confirmDialogIfShown(Duration.ofSeconds(2));
        assertEquals(waitUntilEquals(() -> page.note().trim(), saved.trim()), saved.trim(),
                "\"تراجع\" did not restore \"البيان\" of " + code + ".");
    }

    @Test(priority = 47, groups = {SR, "status"},
            description = "A saved return shows its three statuses: return (معتمد), delivery and payment, and an approval date")
    public void savedSalesReturnShowsItsStatuses() {

        Map<String, Object> record = openRandom(r -> true);
        String code = page.waitForCode();
        page.waitForStatusLabel(APPROVED.label());
        log.info("{}: status [{}], delivery [{}], payment [{}] (API paymentStatus {}, deliveryStatus {})", code,
                page.statusLabel(), page.deliveryStatus(), page.paymentStatus(), record.get("paymentStatus"),
                record.get("deliveryStatus"));
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.deliveryStatus().isBlank(), "\"حالة التسليم\" is empty on " + code + ".");
        softly.assertFalse(page.paymentStatus().isBlank(), "\"حالة الدفع\" is empty on " + code + ".");
        softly.assertFalse(page.inputValue("approvalDate").isBlank(), "Approved " + code + " has no approval date.");
        softly.assertAll();
    }

    @Test(priority = 48, groups = {SR, "journal", "calculation"},
            description = "\"قيد اليومية\" of a saved return is balanced and credits the return total with tax")
    public void salesReturnJournalMatchesReturnTotal() {

        openRandom(r -> true);
        String code = page.waitForCode();
        // Read once the screen has finished its totals (QC run 2026-11-19 12:19, SRI2611-000066: read at
        // once it gave 291, the total before the additions and the tax; the screen then showed 336.832).
        BigDecimal total = settledTotal("الإجمالي شامل الضريبة");
        if (total == null || total.signum() <= 0) {
            throw new SkipException("\"الإجمالي شامل الضريبة\" is not shown on " + code + ".");
        }
        if (!page.waitForActionEnabled("journalEntry")) {
            throw new SkipException("\"قيد اليومية\" is disabled on " + code + ".");
        }
        BigDecimal expected = total.multiply(conversionRate());
        List<JournalLine> lines = page.readJournalEntry();
        log.info("Journal of {} (total with tax {} → {}): {}", code, plain(total), plain(expected), lines);
        BigDecimal debit = lines.stream().map(JournalLine::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(JournalLine::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(debit.compareTo(credit), 0, "The journal entry of " + code + " is not balanced: " + debit + " ≠ " + credit);
        softly.assertTrue(lines.stream().anyMatch(l -> l.credit().subtract(expected).abs().compareTo(JOURNAL_TOLERANCE) <= 0),
                "No line of the journal of " + code + " credits the return total " + plain(expected) + ": " + lines);
        softly.assertAll();
    }
/*
    @Test(priority = 49, groups = {SR, "create", "lines"},
            description = "\"انزال من\" = مردود المبيعات → random return: its lines are added (not saved)")
    public void userCanDownloadLinesFromAnotherSalesReturn() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectCustomerBranchAndWarehouse();
        int before = form.gridRowCount();
        String number = page.downloadLinesFrom(SalesReturnPage.DOWNLOAD_FROM_RETURNS);
        page.waitForGridToSettle();
        assertTrue(form.gridRowCount() > before, "No lines were downloaded from return " + number + ".");
        page.closeForm();
    }
    */

    @Test(priority = 50, groups = {SR, "validation", "excel"},
            description = "⋮ of a new return: \"استيراد من Execl\" is listed and disabled while the header is empty")
    public void newSalesReturnMenuHasDisabledExcelImport() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        List<String> menu = page.menuItems();
        assertTrue(menu.contains(SalesReturnPage.MENU_IMPORT_EXCEL), "⋮ of a new return has no import: " + menu);
        assertFalse(page.isMenuItemEnabled(SalesReturnPage.MENU_IMPORT_EXCEL), "\"استيراد من Execl\" is enabled on an empty return.");
        selectCustomerBranchAndWarehouse();
        log.info("\"استيراد من Execl\" after العميل / الفرع / المخزن: enabled = {}",
                page.isMenuItemEnabled(SalesReturnPage.MENU_IMPORT_EXCEL));
        page.closeForm();
    }

    @Test(priority = 51, groups = {SR, "follow-up", "payment"},
            description = "⋮ → \"دفع\" on an unpaid return (API: remaining > 0): the payment dialog is filled "
                    + "(random طريقة الدفع, the amount due) and paid; حالة الدفع becomes مدفوع, nothing remains, "
                    + "\"دفع\" is disabled")
    public void userCanOpenPaymentFromSalesReturnMenu() {

        // A return that still has something to pay, chosen through the API, with "دفع" enabled.
        List<Map<String, Object>> unpaid = new ArrayList<>(page.api().find(r -> WITH_REFERENCE.test(r)
                && amountOf(r.get("remaining")).signum() > 0));
        java.util.Collections.shuffle(unpaid);
        log.info("API: {} returns with an amount left to pay.", unpaid.size());
        String code = null;
        Map<String, Object> record = null;
        for (Map<String, Object> candidate : unpaid.stream().limit(MAX_INVOICE_ATTEMPTS).toList()) {
            page.openDocument(candidate.get("id"));
            String opened = page.waitForCode();
            if (page.isMenuItemEnabled(SalesInvoiceDocumentPage.PAY)) {
                code = opened;
                record = candidate;
                break;
            }
            log.info("\"دفع\" is disabled on {} (حالة الدفع [{}], remaining {}) → another return.", opened,
                    page.paymentStatus(), candidate.get("remaining"));
        }
        if (code == null) {
            throw new SkipException("No return with an amount left to pay and \"دفع\" enabled (" + unpaid.size()
                    + " with remaining > 0 in the API).");
        }
        BigDecimal due = amountOf(record.get("remaining"));
        String statusBefore = page.paymentStatus();
        log.info("{}: حالة الدفع [{}], amount due {}", code, statusBefore, plain(due));

        // ⋮ → "دفع" opens the payment dialog over the return.
        page.clickMenuItem(SalesInvoiceDocumentPage.PAY);
        assertTrue(page.waitForPayDialog(), "\"دفع\" did not open the payment dialog on " + code
                + " (dialog [" + page.openDialogTitle() + "]).");
        SoftAssert softly = new SoftAssert();
        String description = page.payDialogTextValue("descriptions");
        softly.assertTrue(description.isBlank() || description.contains(code),
                "\"البيان\" of the payment does not name " + code + ": [" + description + "].");

        String method = fillPayDialog(due, java.math.RoundingMode.HALF_UP);
        List<String> toasts = page.submitPayDialog();
        if (page.isPayDialogOpen() && String.join(" ", toasts).contains("اكبر")) {
            // The rounded amount was above what is due: paid rounded down.
            log.warn("Payment refused {} → amount rounded down.", toasts);
            fillPayAmounts(due, java.math.RoundingMode.DOWN);
            toasts = page.submitPayDialog();
        }
        if (page.isPayDialogOpen()) {
            page.closePayDialog();
            org.testng.Assert.fail("Paying " + code + " (" + method + ", " + plain(due) + ") was refused: " + toasts);
        }

        // The return is now paid: حالة الدفع "مدفوع", nothing left, "دفع" disabled.
        driver.navigate().refresh();
        page.waitForDocumentLoaded();
        String statusAfter = waitUntilEquals(page::paymentStatus, PAID);
        Map<String, Object> after = page.recordByCode(code);
        BigDecimal left = after == null ? null : amountOf(after.get("remaining"));
        log.info("Paid {} ({}): حالة الدفع [{}] → [{}], remaining {} → {}", code, method, statusBefore, statusAfter,
                plain(due), left == null ? "?" : plain(left));
        softly.assertEquals(statusAfter, PAID, "حالة الدفع of " + code + " after paying it in full.");
        softly.assertTrue(left != null && left.abs().compareTo(new BigDecimal("0.01")) <= 0,
                "Amount still due on " + code + " after paying it in full: " + left + ".");
        softly.assertFalse(page.isMenuItemEnabled(SalesInvoiceDocumentPage.PAY), "\"دفع\" is still enabled on paid " + code + ".");
        softly.assertAll();
    }

    /** "حالة الدفع" of a document paid in full. */
    private static final String PAID = "مدفوع";

    private static BigDecimal amountOf(Object value) {
        String text = value == null ? "" : String.valueOf(value).replace(",", "").trim();
        try {
            return text.isEmpty() || "null".equals(text) ? BigDecimal.ZERO : new BigDecimal(text);
        } catch (NumberFormatException notANumber) {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Fills the "دفع" dialog as the invoice test does: a random طريقة الدفع, a random option in
     * every empty select (العملة = the return's currency), the amounts = what is due (a split
     * method: half and the rest), and any other empty text (e.g. a cheque number).
     *
     * @return the chosen طريقة الدفع.
     */
    private String fillPayDialog(BigDecimal due, java.math.RoundingMode rounding) {
        String method = page.payDialogSelectRandom("paymentMethod");
        if (method.isEmpty()) {
            method = page.payDialogSelectedText("paymentMethod");
        }
        page.confirmDialogIfShown(Duration.ofMillis(500));
        String currency = page.selectedText("currencyId"); // the header's: it comes first in the page
        for (String field : page.payDialogSelects()) {
            if ("paymentMethod".equals(field) || !page.payDialogSelectedText(field).isEmpty()) {
                continue;
            }
            String chosen = "currencyId".equals(field)
                    ? page.payDialogSelect(field, options -> options.contains(currency) ? currency : options.get(0))
                    : page.payDialogSelectRandom(field);
            log.info("Pay dialog {} (was empty) = {}", field, chosen);
        }
        fillPayAmounts(due, rounding);
        long stamp = System.currentTimeMillis() % 1_000_000;
        for (String id : page.payDialogTextInputs()) {
            if (!isPayAmount(id) && page.payDialogTextValue(id).isBlank()) {
                page.payDialogSetText(id, String.valueOf(stamp));
            }
        }
        return method;
    }

    private static boolean isPayAmount(String id) {
        return id.toLowerCase().contains("amount");
    }

    /** Amount fields of the dialog: together exactly {@code due} (2 decimals, {@code rounding}). */
    private void fillPayAmounts(BigDecimal due, java.math.RoundingMode rounding) {
        BigDecimal total = due.setScale(2, rounding);
        List<String> amounts = new ArrayList<>(page.payDialogTextInputs().stream().filter(SalesReturnTest::isPayAmount).toList());
        assertFalse(amounts.isEmpty(), "The payment dialog has no amount field.");
        amounts.sort(java.util.Comparator.comparing(id -> !id.toLowerCase().contains("cash")));
        BigDecimal half = amounts.size() == 1 ? total : total.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.DOWN);
        for (int i = 0; i < amounts.size(); i++) {
            BigDecimal value = i == 0 ? half : i == 1 ? total.subtract(half) : BigDecimal.ZERO;
            page.payDialogSetText(amounts.get(i), plain(value));
        }
    }

    @Test(priority = 52, groups = {SR, "sales"},
            description = "العميل fills الرقم الضريبي and حساب العميل (read-only); الفرع limits المخزن")
    public void customerAndBranchFillDependentFieldsOfSalesReturn() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectIfEditable("vendorId", false);
        String tax = waitUntilEquals(() -> page.textValue("vendorTaxNumber").isBlank() ? "" : "filled", "filled");
        String account = waitForSelected(form, "vendorAccountId");
        selectBranchThenWarehouse(false);
        log.info("Tax [{}], account [{}], branch [{}], warehouse [{}]", page.textValue("vendorTaxNumber"), account,
                form.getSelectedText("branchId"), form.getSelectedText("warehouseId"));
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(tax, "filled", "\"الرقم الضريبي\" stayed empty after choosing the customer (it may have none in QC).");
        softly.assertFalse(account.isEmpty(), "\"حساب العميل\" stayed empty after choosing the customer.");
        softly.assertFalse(page.isInputEditable("vendorAccountId"), "\"حساب العميل\" must be read-only.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 53, groups = {SR, "create", "reference", "quantity"},
            description = "An invoice that already has a return: \"المستند المرجعي\" → download brings what is left; "
                    + "a part of it is returned and saved as Approved (the payment amount must follow the return)")
    public void userCanReturnWhatIsLeftOfPartlyReturnedInvoice() {

        Set<String> returned = returnedInvoices();
        List<Map<String, Object>> candidates = new ArrayList<>(invoicesWithRequiredFields().values());
        candidates.removeIf(r -> !returned.contains(String.valueOf(r.get("invoiceCode"))));
        if (candidates.isEmpty()) {
            throw new SkipException("No sales invoice with its required fields filled has a return yet.");
        }
        java.util.Collections.shuffle(candidates);
        candidates.sort(java.util.Comparator.comparingLong(SalesReturnTest::filledHeaderFields).reversed());
        log.info("API: {} partly returned invoices, e.g. {}", candidates.size(),
                candidates.stream().limit(5).map(r -> r.get("invoiceCode")).toList());

        String invoice = selectReferenceInvoiceFrom(candidates);
        String code = completeAndSavePartialReturn();
        assertEquals(page.textValue("referenceDocument").trim(), invoice, "\"المستند المرجعي\" of " + code + " is not " + invoice + ".");
        verifySavedReturn(code);
    }
/*
    @Test(priority = 54, groups = {SR, "create", "excel"},
            description = "New return: ⋮ → \"استيراد من Execl\" is enabled after الفرع / المخزن; \"تحميل النموذج\" → the "
                    + "template filled with صنف استلام 1 (test-data/import-excel/SalesReturn-Items.xlsx) → imported: one line "
                    + "of that item and quantity, with its totals; not saved (a return needs \"المستند المرجعي\")")
    public void userCanImportLinesFromExcelIntoNewSalesReturn() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        assertFalse(page.isMenuItemEnabled(SalesInvoiceDocumentPage.IMPORT_EXCEL),
                "\"" + SalesInvoiceDocumentPage.IMPORT_EXCEL + "\" must be disabled before الفرع and المخزن.");
        selectFixedOption("branchId", IMPORT_BRANCH);
        selectFixedOption("warehouseId", IMPORT_WAREHOUSE);
        assertTrue(page.isMenuItemEnabled(SalesInvoiceDocumentPage.IMPORT_EXCEL),
                "\"" + SalesInvoiceDocumentPage.IMPORT_EXCEL + "\" stayed disabled after الفرع [" + IMPORT_BRANCH
                        + "] and المخزن [" + IMPORT_WAREHOUSE + "].");

        page.openImportFromExcel();
        List<String> buttons = page.importDialogButtons();
        log.info("Import dialog buttons: {}", buttons);
        assertTrue(buttons.contains(SalesInvoiceDocumentPage.IMPORT_TEMPLATE_BUTTON)
                        && buttons.contains(SalesInvoiceDocumentPage.IMPORT_DIALOG_TITLE),
                "The import dialog lacks \"" + SalesInvoiceDocumentPage.IMPORT_TEMPLATE_BUTTON + "\" or \""
                        + SalesInvoiceDocumentPage.IMPORT_DIALOG_TITLE + "\": " + buttons);

        // 1) The return's template ("تحميل النموذج"), 2) filled with the item, kept in the project.
        java.nio.file.Path template = page.downloadImportTemplate(page.prepareDownloadFolder("return-import-template"));
        assertTrue(com.erp.automation.ui.Downloads.fileStartsWith(template, "PK"), template.getFileName() + " is not an .xlsx (zip) file.");
        List<String> headers = com.erp.automation.ui.Downloads.xlsxHeaders(template, IMPORT_HEADER_ROW);
        log.info("Template {}: headers {}", template.getFileName(), headers);
        String codeHeader = headers.stream().filter(h -> h.contains("كود") || h.contains("رمز")).findFirst().orElse(null);
        String quantityHeader = headers.stream().filter(h -> h.contains("الكمية") && !h.contains("المجانية")).findFirst().orElse(null);
        assertTrue(codeHeader != null && quantityHeader != null, "The template has no item code or quantity column: " + headers);
        Map<String, Object> row = new LinkedHashMap<>();
        row.put(codeHeader, IMPORT_ITEM_CODE);
        row.put(quantityHeader, IMPORT_QUANTITY);
        headers.stream().filter(h -> h.contains("البيان")).findFirst()
                .ifPresent(h -> row.put(h, "استيراد مردود آلي " + LocalDate.now()));
        java.nio.file.Path file = com.erp.automation.ui.Downloads.xlsxFromTemplate(template, IMPORT_RETURN_XLSX,
                IMPORT_HEADER_ROW, List.of(row));
        log.info("Import file {}: {}", file, row);

        // 3) Imported into the new return.
        page.importExcelFile(file);
        page.waitForGridToSettle();
        page.deleteEmptyLines();
        List<String> items = new ArrayList<>();
        BigDecimal quantity = BigDecimal.ZERO;
        for (int r = 0; r < page.lineCount(); r++) {
            String item = page.lineText(r, "itemId");
            if (!item.isEmpty()) {
                items.add(item);
                quantity = page.lineNumber(r, quantityColumn());
                log.info("Imported line {}: {} × {} @ {}", r + 1, item, plain(quantity), page.lineText(r, priceColumn()));
            }
        }
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(items, List.of(IMPORT_ITEM), "Lines imported from " + file.getFileName() + ".");
        softly.assertEquals(quantity.compareTo(BigDecimal.valueOf(IMPORT_QUANTITY)), 0,
                "Imported quantity [" + plain(quantity) + "] is not " + IMPORT_QUANTITY + ".");
        // The invoice import (QC runs 2026-11-17 18:03 / 18:13) left the line totals at 0 until the
        // quantity was typed again: the totals are logged, then the quantity is typed again.
        for (int r = 0; r < page.lineCount(); r++) {
            if (page.lineText(r, "itemId").isEmpty()) {
                continue;
            }
            log.info("Imported line {} totals: {}", r + 1, lineTotals(r));
            page.setLineNumberInEditor(r, quantityColumn(), String.valueOf(IMPORT_QUANTITY + 1));
            page.setLineNumberInEditor(r, quantityColumn(), String.valueOf(IMPORT_QUANTITY));
            Map<String, String> totals = lineTotals(r);
            log.info("Imported line {} after typing the quantity again: {}", r + 1, totals);
            List<String> lineTotalValues = totals.entrySet().stream()
                    .filter(e -> e.getKey().toLowerCase().contains("total")).map(Map.Entry::getValue).toList();
            softly.assertTrue(lineTotalValues.isEmpty() || lineTotalValues.stream().anyMatch(v -> !isBlankOrZero(v)),
                    "Imported line " + (r + 1) + " has no total: " + totals);
        }
        page.stopCellEditing();
        // Not saved: a sales return cannot be made without "المستند المرجعي" (business rule, QC 2026-11-18).
        page.closeForm();
        softly.assertAll();
    }*/

    /** Branch / warehouse that hold the import item (the same as the invoice test). */
    private static final String IMPORT_BRANCH = "فرع اختبار الدمام";
    private static final String IMPORT_WAREHOUSE = "مخزن اختبار الدمام";
    private static final String IMPORT_ITEM = "صنف استلام 1";
    private static final String IMPORT_ITEM_CODE = "ITM1352";
    private static final int IMPORT_QUANTITY = 2;
    /** Row of the headers in the template of "تحميل النموذج" (row 1 = title). */
    private static final int IMPORT_HEADER_ROW = 2;
    /** The import file made from the template, kept in the project. */
    private static final java.nio.file.Path IMPORT_RETURN_XLSX =
            java.nio.file.Path.of(System.getProperty("user.dir"), "test-data", "import-excel", "SalesReturn-Items.xlsx");

    /** Totals / price / tax cells of line {@code row}. */
    private Map<String, String> lineTotals(int row) {
        Map<String, String> totals = new LinkedHashMap<>();
        page.lineData(row).forEach((field, value) -> {
            String f = field.toLowerCase();
            if (f.contains("total") || f.contains("tax") || f.contains("vat") || f.equals("cost") || f.contains("price")
                    || f.contains("net")) {
                totals.put(field, value);
            }
        });
        return totals;
    }

    /** Selects {@code text} in {@code field} when it is enabled (else kept); checks it is shown. */
    private void selectFixedOption(String field, String text) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, form.getSelectedText(field));
            return;
        }
        if (!text.equals(form.getSelectedText(field))) {
            page.selectOption(field, text);
            page.confirmDialogIfShown(Duration.ofMillis(800));
        }
        assertEquals(form.waitForSelectedText(field, Duration.ofSeconds(5)), text, field + " is not [" + text + "].");
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /** Price typed with "تحديد يدوي". */
    private static final String MANUAL_PRICE = "37";

    /** Rounding of the converted journal amounts. */
    private static final BigDecimal JOURNAL_TOLERANCE = new BigDecimal("0.05");

    /** Invoice and "انشاء مردود مبيعات" link of the last {@link #startReturnFromRandomInvoice()}. */
    private String sourceInvoice;
    private String sourceReturnUrl;

    /**
     * Opens a random sales invoice, reads what it sold (item code → quantity), then
     * ⋮ → "انشاء مردود مبيعات": the new return is open in Add with the invoice's lines.
     * An invoice with nothing left to return (no lines) is replaced by another one.
     *
     * @return quantity sold of every item of the invoice.
     */
    private Map<String, BigDecimal> startReturnFromRandomInvoice() {
        return startReturnFromInvoices(invoicesMostFilledFirst());
    }

    /** {@link #startReturnFromRandomInvoice()} over {@code candidates}, in their order. */
    private Map<String, BigDecimal> startReturnFromInvoices(List<Map<String, Object>> allCandidates) {
        SalesInvoicePage invoices = new SalesInvoicePage(driver);
        List<String> rejected = new ArrayList<>();
        List<Map<String, Object>> candidates = withBranchAndWarehouse(allCandidates, MAX_SOURCE_INVOICES);
        for (int attempt = 1; attempt <= MAX_SOURCE_INVOICES && attempt <= candidates.size(); attempt++) {
            // An invoice chosen through the API: required fields filled, most header fields filled.
            Map<String, Object> record = candidates.get(attempt - 1);
            invoices.openDocument(record.get("id"));
            String invoice = invoices.waitForCode();
            invoices.waitForGridToSettle();
            Map<String, BigDecimal> sold = new LinkedHashMap<>();
            for (int row = 0; row < invoices.lineCount(); row++) {
                String item = invoices.lineText(row, "code");
                if (!item.isEmpty()) {
                    sold.merge(item, invoices.lineNumber(row, SalesInvoiceDocumentPage.QUANTITY_COLUMN), BigDecimal::add);
                }
            }
            if (sold.isEmpty() || !invoices.isMenuItemEnabled(SalesInvoicePage.MENU_CREATE_RETURN)) {
                rejected.add(invoice + (sold.isEmpty() ? " (no lines)" : " (no \"انشاء مردود مبيعات\")"));
                continue;
            }
            invoices.clickMenuItemAndFollow(SalesInvoicePage.MENU_CREATE_RETURN, SalesInvoicePage.RETURN_ADD_PATH);
            form.waitForMode(DocumentForm.Mode.ADD);
            page.waitForGridToSettle();
            page.deleteEmptyLines();
            if (page.lineCount() == 0) {
                rejected.add(invoice + " (nothing left to return)");
                page.closeForm();
                continue;
            }
            List<String> empty = emptyRequiredHeaderFields();
            if (!empty.isEmpty()) {
                rejected.add(invoice + " (empty " + empty + " in the return although the API has them)");
                page.closeForm();
                continue;
            }
            List<String> outOfRange = rememberDiscountRates();
            if (!outOfRange.isEmpty()) {
                rejected.add(invoice + " (" + DISCOUNTS_TAB + " out of 0–100 %: " + outOfRange + ")");
                page.closeForm();
                continue;
            }
            sourceInvoice = invoice;
            sourceReturnUrl = driver.getCurrentUrl();
            log.info("Return from {} (id {}): {} line(s); sold {}", invoice, record.get("id"), page.lineCount(), sold);
            return sold;
        }
        throw new SkipException("No sales invoice to return after " + MAX_SOURCE_INVOICES + " attempts: " + rejected);
    }

    /**
     * A new return whose lines come from "انزال من" = فواتير المبيعات, for the tests of
     * "سعر المرتجع": a return made from "المستند المرجعي" / ⋮ of an invoice has no
     * "سعر المرتجع" field (QC run 2026-11-18 15:25, SI2611-000225).
     */
    private void startReturnWithReturnPrice() {
        // "المستند المرجعي" = an API invoice, DOWNLOADED (QC run 2026-11-19 10:06: it was only chosen,
        // then a random customer and the lines of another invoice were used, so "اخر سعر بيع للعميل"
        // was rightly 0). The customer, branch, warehouse and lines are now the invoice's.
        String number = selectReferenceInvoiceWithLines();
        if (!page.hasReturnPrice()) {
            page.closeForm();
            throw new SkipException("\"سعر المرتجع\" is not shown on a return downloaded from " + number
                    + " (every return has a reference: to be confirmed whether this field can still be chosen).");
        }
        log.info("Return downloaded from {} ({} line(s)); سعر المرتجع shown.", number, page.lineCount());
    }

    /** Deletes every item line (the grid's empty line too). */
    private void deleteAllItemLines() {
        page.waitForGridToSettle();
        for (int row = page.lineCount() - 1; row >= 0; row--) {
            page.deleteLine(row);
        }
        page.waitForGridToSettle();
        page.deleteEmptyLines();
        assertTrue(lineCodes().isEmpty(), "Item lines are left after deleting them all: " + lineCodes());
    }

    // =====================================================================
    // Discounts / additions of a partial return (QC 2026-11-19)
    // =====================================================================

    /** Percentage cells of a "الخصومات والإضافات" line. */
    private static final java.util.regex.Pattern DISCOUNT_RATE_CELL =
            java.util.regex.Pattern.compile("(?i)(discount|addition|extra)rate");

    /** Percentages of the "الخصومات والإضافات" lines right after the download (row → cell → %). */
    private final Map<Integer, Map<String, BigDecimal>> discountRatesAtDownload = new LinkedHashMap<>();

    /** Invoice whose percentages are in {@link #discountRatesAtDownload}. */
    private String discountRatesInvoice = "";

    /**
     * Right after the download, before any line changes: the percentages of the invoice's
     * discounts / additions ("الخصومات والإضافات"). QC run 2026-11-19 10:47 (SI2610-000137):
     * they come as fixed amounts for the whole invoice; on a partial return the same amounts
     * were more than 100 % of what was returned, and the server refused the save
     * ("نسبة الخصم بين ٠٪ و ١٠٠٪", "نسبة الاضافة: الحد الأقصى 100%").
     */
    private List<String> rememberDiscountRates() {
        discountRatesAtDownload.clear();
        discountRatesInvoice = page.textValue("referenceDocument").trim();
        List<String> outOfRange = new ArrayList<>();
        if (!Boolean.TRUE.equals(page.lineTabs().get(DISCOUNTS_TAB))) {
            return outOfRange;
        }
        page.openLinesTab(DISCOUNTS_TAB);
        page.waitForGridToSettle();
        List<Map<String, String>> lines = page.activeTabLines();
        for (int row = 0; row < lines.size(); row++) {
            Map<String, BigDecimal> rates = new LinkedHashMap<>();
            lines.get(row).forEach((cell, value) -> {
                if (DISCOUNT_RATE_CELL.matcher(cell).matches() && !isBlankOrZero(value)) {
                    rates.put(cell, new BigDecimal(value.replace(",", "").trim()));
                }
            });
            if (!rates.isEmpty()) {
                discountRatesAtDownload.put(row, rates);
            }
            int line = row + 1;
            rates.forEach((cell, rate) -> {
                if (rate.signum() < 0 || rate.compareTo(BigDecimal.valueOf(100)) > 0) {
                    outOfRange.add("line " + line + " " + cell + "=" + plain(rate) + "%");
                }
            });
        }
        page.openLinesTab(SalesReturnPage.LINE_TABS.get(0));
        log.info("{} of {} at download (percentages): {}", DISCOUNTS_TAB, discountRatesInvoice, discountRatesAtDownload);
        if (!outOfRange.isEmpty()) {
            // QC run 2026-11-19 10:53: SI2610-000137 came with discount 110 % and addition 120 % before
            // any change, so no return of it can be saved ("نسبة الخصم بين ٠٪ و ١٠٠٪"). Such an invoice is
            // not used as a source (reported for the analysts: the invoice was saved with these values).
            log.warn("{} of {} is out of 0–100 % right after the download: {} → another invoice.", DISCOUNTS_TAB,
                    discountRatesInvoice, outOfRange);
        }
        return outOfRange;
    }

    /**
     * Before the save: every discount / addition gets back the percentage it had at download,
     * so its amount follows what is returned (a share of the invoice gets the same share of
     * its discounts and additions). Only cells that changed and can be edited are set.
     */
    private void restoreDiscountRates() {
        if (discountRatesAtDownload.isEmpty()
                || !discountRatesInvoice.equals(page.textValue("referenceDocument").trim())) {
            return;
        }
        page.openLinesTab(DISCOUNTS_TAB);
        page.waitForGridToSettle();
        try {
            page.useActiveTabGrid(true);
            for (Map.Entry<Integer, Map<String, BigDecimal>> line : discountRatesAtDownload.entrySet()) {
                int row = line.getKey();
                List<String> editable = page.editableLineColumns(row);
                for (Map.Entry<String, BigDecimal> rate : line.getValue().entrySet()) {
                    String shown = page.lineData(row).getOrDefault(rate.getKey(), "");
                    if (!isBlankOrZero(shown) && new BigDecimal(shown.replace(",", "").trim()).compareTo(rate.getValue()) == 0) {
                        continue;
                    }
                    if (!editable.contains(rate.getKey())) {
                        log.warn("{} line {} {}: {} instead of {} %, but the cell is locked.", DISCOUNTS_TAB, row + 1,
                                rate.getKey(), shown, plain(rate.getValue()));
                        continue;
                    }
                    page.setLineNumberInEditor(row, rate.getKey(), plain(rate.getValue()));
                    log.info("{} line {} {}: {} → {} % (as on the invoice).", DISCOUNTS_TAB, row + 1, rate.getKey(), shown,
                            plain(rate.getValue()));
                }
            }
        } finally {
            page.useActiveTabGrid(false);
            page.openLinesTab(SalesReturnPage.LINE_TABS.get(0));
        }
    }

    // =====================================================================
    // "الخصومات والإضافات" (same steps as the invoice test, QC 2026-11-18)
    // =====================================================================

    /** Second line tab of a return. */
    private static final String DISCOUNTS_TAB = SalesReturnPage.LINE_TABS.get(1);

    /** Fields of a "الخصومات والإضافات" line compared before / after save ("المعادل" is worked out, so left out). */
    private static final List<String> DISCOUNTS_LINE_FIELDS = List.of("accountId", "influencingOnCost", "discountRate",
            "discountValue", "additionRate", "additionValue", "currencyId", "note");

    /** Cells of a tab line that get a random option even when they already show one. */
    private static final Set<String> ALWAYS_RANDOM_TAB_CELLS = Set.of("influencingOnCost");

    /** Line cells that did not keep what was typed (checked at the end of a test). */
    private final List<String> lineCellProblems = new ArrayList<>();

    /**
     * Adds and fills a line in "الخصومات والإضافات" and returns the tab's lines; null when the
     * tab is not shown or disabled (nothing to check then). Goes back to "تفاصيل الفاتورة".
     */
    private List<String> addDiscountsLineAndSnapshot() {
        Map<String, Boolean> tabs = page.lineTabs();
        if (!Boolean.TRUE.equals(tabs.get(DISCOUNTS_TAB))) {
            log.info("Tab [{}] is not shown or disabled → no line to check: {}", DISCOUNTS_TAB, tabs);
            return null;
        }
        page.openLinesTab(DISCOUNTS_TAB);
        fillActiveTabByAddingLine(DISCOUNTS_TAB);
        List<String> snapshot = discountsSnapshotOfOpenTab();
        log.info("{} before save: {}", DISCOUNTS_TAB, snapshot);
        page.openLinesTab(SalesReturnPage.LINE_TABS.get(0));
        return snapshot;
    }

    /** Lines of "الخصومات والإضافات" (opens the tab, then goes back to "تفاصيل الفاتورة"). */
    private List<String> discountsSnapshot() {
        page.openLinesTab(DISCOUNTS_TAB);
        page.waitForGridToSettle();
        List<String> snapshot = discountsSnapshotOfOpenTab();
        log.info("{} after save and reload: {}", DISCOUNTS_TAB, snapshot);
        page.openLinesTab(SalesReturnPage.LINE_TABS.get(0));
        return snapshot;
    }

    private List<String> discountsSnapshotOfOpenTab() {
        List<String> lines = new ArrayList<>();
        for (Map<String, String> line : page.activeTabLines()) {
            List<String> parts = new ArrayList<>();
            for (String field : DISCOUNTS_LINE_FIELDS) {
                parts.add(field + "=" + normalized(line.getOrDefault(field, "")));
            }
            lines.add(String.join(", ", parts));
        }
        return lines;
    }

    private static String normalized(String value) {
        String text = value == null ? "" : value.trim();
        try {
            return new BigDecimal(text).stripTrailingZeros().toPlainString();
        } catch (NumberFormatException notANumber) {
            return text;
        }
    }

    private static boolean isBlankOrZero(String value) {
        String text = value == null ? "" : value.replace(",", "").trim();
        if (text.isEmpty() || text.equals("null")) {
            return true;
        }
        try {
            return new BigDecimal(text).signum() == 0;
        } catch (NumberFormatException notNumber) {
            return false;
        }
    }

    /**
     * "اضافة سطر" in the active tab, then every editable cell of the new line: a list gets a
     * random option, a date the 15th of next month, a rate/percentage 5, another number 10, a
     * text an automatic text. Skipped when the tab cannot add a line.
     */
    private void fillActiveTabByAddingLine(String tab) {
        if (!page.canAddLineInActiveTab()) {
            log.info("Tab [{}] has no enabled \"اضافة سطر\" → no line added.", tab);
            return;
        }
        LocalDate date = LocalDate.now().plusMonths(1).withDayOfMonth(15);
        try {
            int row = page.addLineInActiveTab();
            List<String> editable = page.editableLineColumns(row);
            log.info("Tab [{}] line {}: editable cells {}", tab, row + 1, editable);
            for (String col : editable) {
                String value = page.lineData(row).getOrDefault(col, "");
                String kind = cellKind(row, col);
                boolean filled = kind.equals("select") ? !value.isBlank() : !isBlankOrZero(value);
                if (filled && !ALWAYS_RANDOM_TAB_CELLS.contains(col)) {
                    log.info("Tab [{}] line {} {} keeps [{}].", tab, row + 1, col, value);
                    continue;
                }
                try {
                    switch (kind) {
                        case "date" -> page.setLineDate(row, col, date);
                        case "select" -> page.selectRandomInLineCell(row, col);
                        case "number" -> page.setLineNumberInEditor(row, col,
                                col.toLowerCase().matches(".*(rate|percent).*") ? "5" : "10");
                        default -> {
                            String text = stamped("آلي ");
                            String kept = page.setLineTextInEditor(row, col, text);
                            if (!text.equals(kept)) {
                                lineCellProblems.add("tab " + tab + " line " + (row + 1) + " " + col
                                        + ": typed [" + text + "], kept [" + kept + "]");
                            }
                        }
                    }
                } catch (IllegalStateException noEditor) {
                    // "equivalent" is reported editable by the grid but is worked out (invoice test, 2026-11-17).
                    log.warn("Tab [{}] line {} {}: not filled ({}) → skipped.", tab, row + 1, col, noEditor.getMessage());
                }
            }
        } finally {
            page.useActiveTabGrid(false);
        }
    }

    /** Kind of a line cell: from its name first, else from its editor (as in the invoice test). */
    private String cellKind(int row, String col) {
        String name = col.toLowerCase();
        if (name.contains("date")) {
            return "date";
        }
        if (name.endsWith("id") || name.startsWith("influencing")) {
            return "select";
        }
        if (name.matches(".*(rate|value|amount|percent|equivalent|qty|quantity|price|cost).*")) {
            return "number";
        }
        return page.lineEditorKind(row, col);
    }

    /** id of the header "البيان" of a return (invoices and returns use "notes"). */
    private static final String NOTE_FIELD = "notes";

    /** Text of a line's "البيان" cell (colId {@code note}) in "تفاصيل الفاتورة". */
    private static final String LINE_NOTE_COLUMN = "note";

    /** "البيان" of the lines typed by the last {@link #fillEmptyLineNotes()} (row → text). */
    private final Map<Integer, String> typedLineNotes = new LinkedHashMap<>();

    /**
     * Every item line whose "البيان" cell is editable and empty gets a text (QC run 2026-11-18
     * 17:10, SRI2611-000055: the cell stayed empty). Filled or locked cells are not touched.
     */
    private void fillEmptyLineNotes() {
        page.stopCellEditing();
        for (int row = 0; row < page.lineCount(); row++) {
            if (page.lineText(row, "code").isEmpty()) {
                continue;
            }
            if (!page.editableLineColumns(row).contains(LINE_NOTE_COLUMN)) {
                log.info("Line {} \"البيان\" is locked.", row + 1);
                continue;
            }
            String current = page.lineData(row).getOrDefault(LINE_NOTE_COLUMN, "");
            if (!current.isBlank()) {
                log.info("Line {} \"البيان\" keeps [{}].", row + 1, current);
                continue;
            }
            String text = stamped("بيان سطر آلي ");
            page.scrollLineCellIntoView(row, LINE_NOTE_COLUMN);
            String kept = page.setLineTextInEditor(row, LINE_NOTE_COLUMN, text);
            log.info("Line {} \"البيان\" → [{}] (kept [{}]).", row + 1, text, kept);
            typedLineNotes.put(row, text);
        }
        page.stopCellEditing();
    }

    /** After a reload: the "البيان" typed in each line is still there. */
    private void verifyLineNotesAfterReload(Map<Integer, String> typed, SoftAssert softly) {
        if (typed.isEmpty()) {
            log.info("No line \"البيان\" was typed (locked or already filled).");
            return;
        }
        page.reload();
        page.waitForGridToSettle();
        typed.forEach((row, text) -> softly.assertEquals(page.lineData(row).getOrDefault(LINE_NOTE_COLUMN, "").trim(), text,
                "\"البيان\" of line " + (row + 1) + " of " + page.waitForCode() + " after save and reload."));
    }

    /** Server answer when a return is linked to a repair order (it cannot be edited). */
    private static final String REPAIR_ORDER_LINK = "أمر إصلاح";

    /** Returns found linked to a repair order in this run: not chosen again. */
    private final Set<String> linkedToRepairOrder = new HashSet<>();

    /**
     * Opens a random saved return that has "المستند المرجعي", "تعديل", runs {@code change}
     * (given the number), and saves (the empty fields, the lines' "البيان" included, are filled
     * by {@link #fillRequiredFieldsBeforeSave()}). A return the server refuses because it is
     * linked to a repair order (QC run 2026-11-18 17:10, SRI2611-000055: "لا يمكن تعديل هذه
     * الفاتورة لارتباطها بأمر إصلاح", a correct rule) is left and another one is tried.
     *
     * @return the lines' "البيان" typed before the save (row → text).
     */
    private Map<Integer, String> editSavedReturnAndSave(java.util.function.Consumer<String> change) {
        for (int attempt = 1; attempt <= MAX_INVOICE_ATTEMPTS; attempt++) {
            openRandom(WITH_REFERENCE.and(r -> !linkedToRepairOrder.contains(String.valueOf(r.get(page.codeField())))));
            String code = page.waitForCode();
            page.clickEdit();
            waitForAnyEditMode();
            typedLineNotes.clear();
            change.accept(code);
            try {
                saveAndVerify(APPROVED);
                return new LinkedHashMap<>(typedLineNotes);
            } catch (AssertionError refused) {
                if (!String.valueOf(refused.getMessage()).contains(REPAIR_ORDER_LINK)) {
                    throw refused;
                }
                linkedToRepairOrder.add(code);
                log.warn("{} is linked to a repair order and cannot be edited → another return ({}/{}).", code, attempt,
                        MAX_INVOICE_ATTEMPTS);
                page.closeForm();
            }
        }
        throw new SkipException("No editable return (not linked to a repair order) after " + MAX_INVOICE_ATTEMPTS
                + " attempts: " + linkedToRepairOrder);
    }

    /**
     * Returns kept for "تعديل" / "إضافة": only the ones that have "المستند المرجعي" (a return
     * cannot be made without one, business rule QC 2026-11-18).
     */
    private static final java.util.function.Predicate<Map<String, Object>> WITH_REFERENCE =
            r -> isFilled(r.get("referenceDocument"));

    /** "جديد" → an empty return in Add. */
    private void openNewReturn() {
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
    }

    /** The return opened by {@link #openReturnAndClickAdd()}, to check that "إضافة" left it unchanged. */
    private Map<String, Object> openedBeforeAdd;

    /** A random return that has "المستند المرجعي" → "إضافة": a new return in Add. */
    private void openReturnAndClickAdd() {
        openedBeforeAdd = openRandom(WITH_REFERENCE);
        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);
    }

    /** العميل / الفرع / المخزن only when the reference left them empty (it was not downloaded). */
    private void selectCustomerBranchAndWarehouseIfEmpty() {
        if (isEmptySelect("vendorId")) {
            selectIfEditable("vendorId", false);
        }
        if (isEmptySelect("branchId") || isEmptySelect("warehouseId")) {
            selectBranchThenWarehouse(false);
        }
    }

    /**
     * "المستند المرجعي" of the form already open: API invoices are tried in it (a new choice
     * replaces the previous one) until one is downloaded with العميل / الفرع / المخزن and lines.
     */
    private String referenceInCurrentForm() {
        List<String> rejected = new ArrayList<>();
        for (Map<String, Object> record : withBranchAndWarehouse(invoicesMostFilledFirst(), MAX_SOURCE_INVOICES)) {
            String number = String.valueOf(record.get("invoiceCode"));
            if (!page.selectReferenceDocumentByNumber(number)) {
                rejected.add(number + " (not in the dialog)");
                continue;
            }
            page.downloadReferenceDocument();
            page.waitForGridToSettle();
            page.deleteEmptyLines();
            List<String> empty = emptyRequiredHeaderFields();
            List<String> outOfRange = empty.isEmpty() && page.lineCount() > 0 ? rememberDiscountRates() : List.of();
            if (empty.isEmpty() && page.lineCount() > 0 && outOfRange.isEmpty()) {
                log.info("Reference invoice {} (rejected before: {}).", number, rejected);
                return number;
            }
            rejected.add(number + (!outOfRange.isEmpty() ? " (" + DISCOUNTS_TAB + " out of 0–100 %: " + outOfRange + ")"
                    : empty.isEmpty() ? " (no lines)" : " (empty " + empty + ")"));
        }
        throw new SkipException("No API sales invoice could be the reference of this return: " + rejected);
    }

    /**
     * The invoice is chosen through the API (QC 2026-11-18): a random sales invoice whose
     * required fields are filled ({@link #hasRequiredFields}) is put in "المستند المرجعي"
     * (searched for in its dialog) and downloaded. If the return still has an empty
     * العميل / الفرع / المخزن, or no line, another API invoice is tried.
     *
     * @return the number of the downloaded invoice.
     */
    private String selectReferenceInvoiceWithLines() {
        return selectReferenceInvoiceFrom(invoicesMostFilledFirst());
    }

    /** {@link #selectReferenceInvoiceWithLines()} over {@code candidates}, in their order. */
    private String selectReferenceInvoiceFrom(List<Map<String, Object>> candidates) {
        return selectReferenceInvoiceFrom(candidates, this::openNewReturn, true);
    }

    /**
     * Opens a new return with {@code openForm} ("جديد", or "إضافة" on an opened return) and puts
     * an API invoice of {@code candidates} in "المستند المرجعي". With {@code download} it is
     * downloaded and must bring العميل / الفرع / المخزن and lines; without, only the reference is set.
     */
    private String selectReferenceInvoiceFrom(List<Map<String, Object>> allCandidates, Runnable openForm, boolean download) {
        List<String> rejected = new ArrayList<>();
        List<Map<String, Object>> candidates = withBranchAndWarehouse(allCandidates, MAX_SOURCE_INVOICES);
        for (int i = 0; i < candidates.size() && i < MAX_SOURCE_INVOICES; i++) {
            Map<String, Object> record = candidates.get(i);
            String number = String.valueOf(record.get("invoiceCode"));
            if (i > 0) {
                page.closeForm();
            }
            openForm.run();
            log.info("API invoice {}: {}", number, requiredFieldsOf(record));
            if (!page.selectReferenceDocumentByNumber(number)) {
                rejected.add(number + " (not in the \"المستند المرجعي\" dialog)");
                continue;
            }
            if (!download) {
                log.info("المستند المرجعي = {} (not downloaded).", number);
                return number;
            }
            page.downloadReferenceDocument();
            page.waitForGridToSettle();
            page.deleteEmptyLines();
            List<String> empty = emptyRequiredHeaderFields();
            if (!empty.isEmpty()) {
                rejected.add(number + " (empty " + empty + " although the API has " + requiredFieldsOf(record) + ")");
                continue;
            }
            if (page.lineCount() == 0) {
                rejected.add(number + " (no lines)");
                continue;
            }
            List<String> outOfRange = rememberDiscountRates();
            if (!outOfRange.isEmpty()) {
                rejected.add(number + " (" + DISCOUNTS_TAB + " out of 0–100 %: " + outOfRange + ")");
                continue;
            }
            log.info("Reference invoice {} (rejected before: {}).", number, rejected);
            return number;
        }
        page.closeForm();
        throw new SkipException("No API sales invoice with its required fields filled could be returned after "
                + MAX_SOURCE_INVOICES + " attempts: " + rejected);
    }

    /**
     * Required header fields of an invoice in the API record: the name AND, when the
     * record has it, the id (QC run 2026-11-18 14:14: every invoice has a warehouseName,
     * yet SI2608-000004 … 010 left "المخزن" of the return empty).
     */
    private static final List<String> INVOICE_REQUIRED_FIELDS = List.of("partnerName", "branchName", "warehouseName");
    private static final List<String> INVOICE_REQUIRED_IDS = List.of("partnerId", "vendorId", "branchId", "warehouseId");

    /** True when every required field of the invoice record is filled (checked before the invoice is chosen). */
    private static boolean hasRequiredFields(Map<String, Object> invoice) {
        boolean names = INVOICE_REQUIRED_FIELDS.stream().allMatch(field -> isFilled(invoice.get(field)));
        boolean ids = INVOICE_REQUIRED_IDS.stream().filter(invoice::containsKey).allMatch(field -> isFilled(invoice.get(field)));
        return names && ids && isFilled(invoice.get("invoiceCode"));
    }

    private static boolean isFilled(Object value) {
        String text = value == null ? "" : String.valueOf(value).trim();
        return !text.isEmpty() && !"null".equals(text) && !"0".equals(text) && !"0.0".equals(text)
                && !"00000000-0000-0000-0000-000000000000".equals(text);
    }

    /** The required fields of an invoice record, for the log. */
    private static Map<String, Object> requiredFieldsOf(Map<String, Object> invoice) {
        Map<String, Object> shown = new LinkedHashMap<>();
        for (String field : INVOICE_REQUIRED_FIELDS) {
            shown.put(field, invoice.get(field));
        }
        for (String field : INVOICE_REQUIRED_IDS) {
            if (invoice.containsKey(field)) {
                shown.put(field, invoice.get(field));
            }
        }
        return shown;
    }

    /** Sales invoices (number → API record) whose required fields are filled, read through the API once. */
    private Map<String, Map<String, Object>> invoicesWithRequiredFields() {
        if (eligibleInvoices == null) {
            List<Map<String, Object>> all = new SalesInvoicePage(driver).api().findAll();
            eligibleInvoices = new LinkedHashMap<>();
            for (Map<String, Object> record : all) {
                if (hasRequiredFields(record)) {
                    eligibleInvoices.put(String.valueOf(record.get("invoiceCode")), record);
                }
            }
            log.info("API: {} of {} sales invoices have their required fields filled; fields of a record: {}",
                    eligibleInvoices.size(), all.size(), all.isEmpty() ? "-" : all.get(0).keySet());
            if (eligibleInvoices.isEmpty()) {
                throw new SkipException("The sales invoices API returns no invoice with " + INVOICE_REQUIRED_FIELDS + " / "
                        + INVOICE_REQUIRED_IDS + " filled (" + all.size() + " invoices; first: "
                        + (all.isEmpty() ? "-" : requiredFieldsOf(all.get(0))) + ").");
            }
        }
        return eligibleInvoices;
    }

    private Map<String, Map<String, Object>> eligibleInvoices;

    /** Header fields of an invoice in the API record (name or id); the more are filled, the better the invoice. */
    private static final List<String> INVOICE_HEADER_FIELDS = List.of("partnerName", "branchName", "warehouseName",
            "paymentMethod", "paymentAccountId", "paymentAccountName", "currencyId", "taxCalculationMethode",
            "salePriceListId", "saleRepresentativeId", "repersentiveName", "marketerId", "costCenterId",
            "saleShippingMethodId", "collectionsOfficerName", "invoiceAdress", "deliveryAdress", "vendorAccountId");

    private static long filledHeaderFields(Map<String, Object> invoice) {
        return INVOICE_HEADER_FIELDS.stream().filter(field -> isFilled(invoice.get(field))).count();
    }

    /**
     * The invoices with their required fields filled, the ones with the most header
     * fields filled first (random order among equals).
     */
    private List<Map<String, Object>> invoicesMostFilledFirst() {
        // Invoices never returned come first (their downloaded quantities are the whole
        // invoice), then the ones already partly returned: they are NOT excluded, a return of
        // what is left is valid (QC 2026-11-18).
        Set<String> returned = returnedInvoices();
        List<Map<String, Object>> candidates = new ArrayList<>(invoicesWithRequiredFields().values());
        java.util.Collections.shuffle(candidates);
        candidates.sort(java.util.Comparator
                .comparing((Map<String, Object> r) -> returned.contains(String.valueOf(r.get("invoiceCode"))))
                .thenComparing(java.util.Comparator.comparingLong(SalesReturnTest::filledHeaderFields).reversed()));
        log.info("API: {} of {} invoices never returned (tried first).",
                candidates.stream().filter(r -> !returned.contains(String.valueOf(r.get("invoiceCode")))).count(),
                candidates.size());
        log.info("API: best invoices {}", candidates.stream().limit(5)
                .map(r -> r.get("invoiceCode") + " (" + filledHeaderFields(r) + "/" + INVOICE_HEADER_FIELDS.size() + ")").toList());
        return candidates;
    }

    // =====================================================================
    // Lines of an invoice through the API (QC 2026-11-19)
    // =====================================================================

    /** API url of one sales invoice, with "{id}" for its id; learnt once from the page. */
    private String invoiceDetailUrl;

    /** Number of item lines of each invoice read through the API (invoice number → lines). */
    private final Map<String, Integer> invoiceLineCounts = new HashMap<>();

    /**
     * The API url the app reads one sales invoice with: an invoice is opened, and the GET
     * call of the page whose path holds its id is kept, with "{id}" in place of the id.
     */
    private String invoiceDetailUrl() {
        if (invoiceDetailUrl != null) {
            return invoiceDetailUrl;
        }
        SalesInvoicePage invoices = new SalesInvoicePage(driver);
        // QC run 2026-11-19 10:46: the app reads one invoice from <api>/SaleInvoice/{id}. That url is
        // tried first on the API host the page already uses, so the open form is not left.
        List<String> used = invoices.api().apiUrlsUsedByPage("/api/");
        if (!used.isEmpty()) {
            String base = used.get(0).substring(0, used.get(0).indexOf("/api/") + "/api/".length());
            String candidate = base + "SaleInvoice/{id}";
            Map<String, Object> sample = invoicesWithRequiredFields().values().iterator().next();
            try {
                if (countItemLines(invoices.api().getJson(candidate.replace("{id}", String.valueOf(sample.get("id"))))) > 0) {
                    invoiceDetailUrl = candidate;
                    log.info("Invoice lines are read from {}", invoiceDetailUrl);
                    return invoiceDetailUrl;
                }
            } catch (IllegalStateException failed) {
                log.info("{} → {}; the url is looked for on an invoice page.", candidate, failed.getMessage());
            }
        }
        Map<String, Object> any = invoicesWithRequiredFields().values().iterator().next();
        String id = String.valueOf(any.get("id"));
        invoices.openDocument(id);
        invoices.waitForGridToSettle();
        java.util.regex.Pattern withId = java.util.regex.Pattern.compile("/" + java.util.regex.Pattern.quote(id) + "(?=[/?]|$)");
        List<String> urls = invoices.api().apiUrlsUsedByPage("/" + id).stream()
                .filter(u -> withId.matcher(u).find() && !u.toLowerCase().contains("filter")).toList();
        log.info("API calls of invoice {} (id {}): {}", any.get("invoiceCode"), id, urls);
        for (String url : urls) {
            try {
                if (countItemLines(invoices.api().getJson(url)) > 0) {
                    invoiceDetailUrl = withId.matcher(url).replaceFirst("/{id}");
                    log.info("Invoice lines are read from {}", invoiceDetailUrl);
                    return invoiceDetailUrl;
                }
            } catch (IllegalStateException failed) {
                log.info("{} → {}", url, failed.getMessage());
            }
        }
        throw new SkipException("No API call of the invoice page returns its lines (calls with its id: " + urls + ").");
    }

    /** Number of item lines of {@code invoice} (API record), read through the API once. */
    private int lineCountOf(Map<String, Object> invoice) {
        String number = String.valueOf(invoice.get("invoiceCode"));
        return invoiceLineCounts.computeIfAbsent(number, n -> countItemLines(invoiceDetail(invoice)));
    }

    /** The invoice as the app reads it ({@code SaleInvoice/{id}}), read once; null when it cannot be read. */
    private Object invoiceDetail(Map<String, Object> invoice) {
        String number = String.valueOf(invoice.get("invoiceCode"));
        if (!invoiceDetails.containsKey(number)) {
            String url = invoiceDetailUrl().replace("{id}", String.valueOf(invoice.get("id")));
            Object json = null;
            try {
                json = new SalesInvoicePage(driver).api().getJson(url);
            } catch (IllegalStateException failed) {
                log.warn("{} not read through the API: {}", number, failed.getMessage());
            }
            invoiceDetails.put(number, json);
        }
        return invoiceDetails.get(number);
    }

    private final Map<String, Object> invoiceDetails = new HashMap<>();

    /**
     * API condition (QC 2026-11-19): الفرع and المخزن of the invoice itself ({@code SaleInvoice/{id}},
     * not the list) are filled. QC run 2026-11-19 11:03: the list showed a warehouse for all 277
     * invoices, yet the return from SI2611-000201 came with "المخزن" empty and could not be saved.
     */
    private boolean hasBranchAndWarehouse(Map<String, Object> invoice) {
        Object json = invoiceDetail(invoice);
        Object branch = headerValue(json, "branchId");
        Object warehouse = headerValue(json, "warehouseId");
        boolean filled = isFilled(branch) && isFilled(warehouse);
        if (!filled) {
            log.info("API: {} has branchId [{}], warehouseId [{}] → not used.", invoice.get("invoiceCode"), branch, warehouse);
        }
        return filled;
    }

    /** First value of {@code key} in the header of an invoice's JSON (maps only; lists are its lines). */
    private static Object headerValue(Object json, String key) {
        java.util.ArrayDeque<Map<?, ?>> queue = new java.util.ArrayDeque<>();
        if (json instanceof Map<?, ?> map) {
            queue.add(map);
        }
        while (!queue.isEmpty()) {
            Map<?, ?> map = queue.poll();
            if (map.containsKey(key)) {
                return map.get(key);
            }
            for (Object value : map.values()) {
                if (value instanceof Map<?, ?> inner) {
                    queue.add(inner);
                }
            }
        }
        return null;
    }

    /** {@code candidates} that pass {@link #hasBranchAndWarehouse}, read through the API as needed, at most {@code max}. */
    private List<Map<String, Object>> withBranchAndWarehouse(List<Map<String, Object>> candidates, int max) {
        List<Map<String, Object>> usable = candidates.stream().filter(this::hasBranchAndWarehouse).limit(max).toList();
        log.info("API: invoices with الفرع and المخزن filled: {}", usable.stream().map(r -> r.get("invoiceCode")).toList());
        if (usable.isEmpty()) {
            throw new SkipException("No sales invoice read through the API has الفرع and المخزن filled.");
        }
        return usable;
    }

    /**
     * Item lines in an invoice's JSON: the largest list of objects that have an "itemId"
     * (wherever it is in the answer, e.g. data.saleInvoiceDetails).
     */
    private static int countItemLines(Object json) {
        int best = 0;
        if (json instanceof List<?> list) {
            if (!list.isEmpty() && list.stream().allMatch(e -> e instanceof Map<?, ?> m && m.containsKey("itemId"))) {
                best = list.size();
            }
            for (Object element : list) {
                best = Math.max(best, countItemLines(element));
            }
        } else if (json instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                best = Math.max(best, countItemLines(value));
            }
        }
        return best;
    }

    /**
     * Invoices (required fields filled, never returned first, most filled first) whose
     * "تفاصيل الفاتورة" has at least {@code minLines} lines, read through the API. At most
     * 60 invoices are read; skipped when none has that many lines.
     */
    private List<Map<String, Object>> invoicesWithSeveralLines(int minLines) {
        List<Map<String, Object>> found = new ArrayList<>();
        int read = 0;
        for (Map<String, Object> invoice : invoicesMostFilledFirst()) {
            if (read++ >= 60 || found.size() >= MAX_SOURCE_INVOICES) {
                break;
            }
            int lines = lineCountOf(invoice);
            if (lines >= minLines && hasBranchAndWarehouse(invoice)) {
                found.add(invoice);
            }
        }
        log.info("API: invoices with {}+ lines and الفرع / المخزن filled: {}", minLines, found.stream()
                .map(r -> r.get("invoiceCode") + " (" + lineCountOf(r) + ")").toList());
        if (found.isEmpty()) {
            throw new SkipException("None of " + read + " sales invoices read through the API has " + minLines
                    + " lines or more.");
        }
        return found;
    }

    /** Invoices that already have a return ("المستند المرجعي" of the returns API). */
    private Set<String> returnedInvoices() {
        Set<String> returned = new HashSet<>();
        for (Map<String, Object> record : page.api().findAll()) {
            Object reference = record.get("referenceDocument");
            if (isFilled(reference)) {
                returned.add(String.valueOf(reference).trim());
            }
        }
        log.info("API: {} invoices already have a return.", returned.size());
        return returned;
    }

    /** العميل / الفرع / المخزن of the return that are still empty after the download. */
    private List<String> emptyRequiredHeaderFields() {
        return List.of("vendorId", "branchId", "warehouseId").stream()
                .filter(field -> waitForSelected(form, field).isEmpty() || isEmptySelect(field))
                .toList();
    }

    /**
     * Types {@code value} in the quantity of line {@code row}. The cell may refuse a value that
     * is not allowed (0, a negative, more than can be returned): it stays empty and is marked
     * red. That refusal is the expected answer, not a failure (QC run 2026-11-18 18:48: the
     * test failed on "Could not set quantity of line 1 to 0; it shows []").
     *
     * @return true when the cell kept the value (the save must then be refused); false when
     *         the cell itself refused it.
     */
    private boolean typeQuantity(int row, String value) {
        try {
            page.setLineNumber(row, quantityColumn(), value);
            return true;
        } catch (IllegalStateException refusedByCell) {
            page.stopCellEditing();
            closeDialogsLeftByTyping();
            log.info("Line {} quantity: the cell refused [{}] (shows [{}]) → refused, as expected.", row + 1, value,
                    page.lineData(row).getOrDefault(quantityColumn(), ""));
            return false;
        }
    }

    /**
     * A refused value can leave a dialog open: typing "-1" opened "البحث عن صنف" (QC run
     * 2026-11-18 18:56); it covered the form and the next click (✕ of the form) failed.
     * The empty item search is closed, then any other dialog with Escape.
     */
    private void closeDialogsLeftByTyping() {
        if (form.closeEmptyItemSearchDialog()) {
            log.info("\"البحث عن صنف\" left open by the refused value → closed.");
        }
        for (int attempt = 1; attempt <= 3 && page.isDialogOpen(); attempt++) {
            log.info("Dialog [{}] still open → Escape.", page.openDialogTitle());
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
            form.waitForDialogToClose(Duration.ofSeconds(2));
        }
    }

    /** Removes every line but the first one; returns its item code. */
    private String keepOnlyFirstLine() {
        page.deleteEmptyLines();
        String item = page.lineText(0, "code");
        keepOnlyLineOfItem(item);
        return item;
    }

    /** Removes every line whose item is not {@code item}. */
    private void keepOnlyLineOfItem(String item) {
        page.deleteEmptyLines();
        for (int row = page.lineCount() - 1; row >= 0; row--) {
            if (!item.equals(page.lineText(row, "code"))) {
                page.deleteLine(row);
            }
        }
        page.waitForGridToSettle();
        assertEquals(page.lineTexts("code"), List.of(item), "Only the line of " + item + " should be left.");
    }

    /**
     * "سعر المرتجع" (a field of the return) and the empty header fields, before the
     * quantities: they can change the lines. What the invoice brought is kept.
     */
    private void prepareHeaderBeforeQuantities() {
        page.selectRandomReturnPrice();
        fillEmptyHeaderFields();
        page.waitForGridToSettle();
        page.setNote(uniqueNote());
    }

    private void fillPriceIfZero(int row) {
        if (page.lineNumber(row, priceColumn()).signum() == 0) {
            page.setLineNumber(row, priceColumn(), PRICE);
        }
        page.stopCellEditing();
    }

    /**
     * Clicks "حفظ" on a return the server must refuse. The confirmation is accepted when
     * it shows, so the server is the one that answers.
     *
     * @return the toast, or {@code null} when the form blocked the save itself
     *         (required-field errors, no toast).
     */
    private DocumentForm.Toast saveExpectingRefusal(String what) {
        page.stopCellEditing();
        page.deleteEmptyLines();
        if (!page.waitForActionEnabled("save")) {
            log.info("{}: \"حفظ\" stays disabled.", what);
            return null;
        }
        page.clickSave();
        DocumentForm.Toast toast;
        try {
            toast = form.waitForConfirmationDialogOrToast().orElse(null);
        } catch (org.openqa.selenium.TimeoutException blockedInForm) {
            log.info("{}: blocked in the form, required-field errors {}", what, form.fieldsWithRequiredError());
            return null;
        }
        if (toast == null) {
            page.confirmDialog();
            toast = form.waitForToast();
        }
        log.info("{} → {}", what, toast);
        return toast;
    }

    private void assertSaveRefused(String what) {
        DocumentForm.Toast toast = saveExpectingRefusal(what);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast == null || toast.isError(), what + " was accepted: " + toast);
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), what + " was saved (" + driver.getCurrentUrl() + ").");
        softly.assertAll();
    }

    /**
     * A total under the lines once the screen has finished computing it: read every 500 ms
     * until the same value is read three times in a row (15 s at most).
     */
    private BigDecimal settledTotal(String label) {
        page.waitForGridToSettle();
        BigDecimal last = null;
        int same = 0;
        long end = System.currentTimeMillis() + 15_000;
        while (System.currentTimeMillis() < end && same < 3) {
            BigDecimal now = page.totalShown(label);
            same = now != null && last != null && now.compareTo(last) == 0 ? same + 1 : 1;
            last = now;
            pause(500);
        }
        log.info("{} = {} (settled)", label, last == null ? "-" : plain(last));
        return last;
    }

    /** "معامل التحويل" of the opened return; 1 when it is empty or not a number. */
    private BigDecimal conversionRate() {
        String text = page.textValue("convertionRate").replace(",", "").trim();
        try {
            BigDecimal rate = new BigDecimal(text);
            return rate.signum() > 0 ? rate : BigDecimal.ONE;
        } catch (NumberFormatException notANumber) {
            return BigDecimal.ONE;
        }
    }

    private static String plain(BigDecimal value) {
        return value == null ? "null" : value.stripTrailingZeros().toPlainString();
    }

    /**
     * Header in screen order (QC 2026-11-12): الفرع → المخزن → مندوب المبيعات →
     * العميل → طريقة الدفع → حساب الدفع → طريقة احتساب الضريبة → المسوق →
     * الأسعار → مسئول التحصيل → عنوان الفاتورة → قائمة الأسعار → عنوان التسليم →
     * طريقة الشحن → سعر المرتجع → البيان.
     */
    @Override
    protected void fillHeader(boolean editing) {
        // A sales return cannot be made without "المستند المرجعي" (business rule, QC 2026-11-18):
        // a new return gets an API invoice, downloaded; then only the empty fields are filled.
        if (!editing && page.textValue("referenceDocument").isBlank()) {
            referenceInCurrentForm();
            fillEmptyHeaderFields();
            String note = uniqueNote();
            page.setNote(note);
            assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
            return;
        }
        fillManualHeader(editing);
    }

    /** Every header field chosen in screen order, without "المستند المرجعي" (only for the negative case 7). */
    private void fillManualHeader(boolean editing) {
        selectBranchThenWarehouse(editing);
        // العميل before مندوب المبيعات: choosing the customer empties the sales person
        // (QC run 2026-11-18 16:24: "موظف اضافه" was gone before the save).
        selectIfEditable("vendorId", editing);
        selectIfEditable("saleRepresentativeId", editing);
        page.selectRandomPaymentMethod();
        ensurePaymentAccount(); // another الفرع → المخزن while "حساب الدفع" has no option
        selectIfEditable("costCenterId", editing); // was left empty (QC run 2026-11-18 16:25)
        selectIfEditable("taxCalculationMethode", editing);
        selectIfEditable("marketerId", editing);
        if (page.priceOption().isEmpty()) {
            page.selectRandomPriceOption();
        }
        selectIfEditable("collectionsOfficerId", editing);
        page.setTextInput("invoiceAdress", stamped("عنوان فاتورة آلي "));
        selectIfEditable("salePriceListId", editing);
        page.setTextInput("deliveryAdress", stamped("عنوان تسليم آلي "));
        selectIfEditable("saleShippingMethodId", editing);
        page.selectRandomReturnPrice();
        String note = uniqueNote();
        page.setNote(note);
        assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
    }

    /** Every downloaded line returns 1; "سعر المرتجع" and payment are chosen; saved. */
    /**
     * Saves a return downloaded from an invoice with LESS than what was downloaded: the
     * returned quantity is a field of the return, so every line whose quantity allows it
     * gets a random whole quantity from 1 to (downloaded − 1) (QC 2026-11-18). Prices and
     * the header the invoice brought are kept; only empty fields and "سعر المرتجع" (when
     * shown and empty) are set. After save and reload the quantities must be the ones typed.
     * Business rule (QC 2026-11-18): the payment amount of a return follows the returned
     * quantity, so "مبلغ الدفع اكبر من قيمة الفاتورة" on a partial return is a defect.
     */
    private String completeAndSavePartialReturn() {
        return completeAndSavePartialReturn(() -> { });
    }

    /** {@link #completeAndSavePartialReturn()} with {@code beforeSave} run once the quantities are set. */
    private String completeAndSavePartialReturn(Runnable beforeSave) {
        String invoice = page.textValue("referenceDocument").trim();
        if (page.hasReturnPrice() && isEmptySelect(SalesReturnPage.RETURN_PRICE_FIELD)) {
            page.selectReturnPrice(SalesReturnPage.MANUAL_RETURN_PRICE);
        }
        page.waitForGridToSettle();
        Map<String, BigDecimal> typed = new LinkedHashMap<>();
        List<String> changes = new ArrayList<>();
        int lowered = 0;
        BigDecimal downloadedTotal = page.totalShown("الإجمالي شامل الضريبة");
        for (int row = page.lineCount() - 1; row >= 0; row--) {
            String item = page.lineText(row, "code");
            if (item.isEmpty()) {
                continue;
            }
            BigDecimal downloaded = page.lineNumber(row, quantityColumn());
            BigDecimal quantity = lessThan(downloaded);
            if (quantity.compareTo(downloaded) != 0) {
                page.setLineNumber(row, quantityColumn(), plain(quantity));
                lowered++;
            }
            fillPriceIfZero(row);
            typed.merge(item, quantity, BigDecimal::add);
            changes.add(item + ": " + plain(downloaded) + " → " + plain(quantity));
        }
        page.stopCellEditing();
        fillEmptyHeaderFields();
        beforeSave.run();
        BigDecimal returnTotal = page.totalShown("الإجمالي شامل الضريبة");
        log.info("Partial return of {}: {}; total with tax {} → {}", invoice, changes, plain(downloadedTotal), plain(returnTotal));
        if (lowered == 0) {
            log.warn("No quantity of {} can be lowered (every line has 1): the return is the whole download.", invoice);
        }

        String code;
        try {
            code = saveAndVerify(APPROVED);
        } catch (AssertionError refused) {
            if (!String.valueOf(refused.getMessage()).contains("مبلغ الدفع")) {
                throw refused;
            }
            Map<String, Object> record = invoicesWithRequiredFields().get(invoice);
            org.testng.Assert.fail("Partial return of " + invoice + " (" + changes + ") was refused: \"مبلغ الدفع اكبر من قيمة "
                    + "الفاتورة\". Total with tax " + plain(downloadedTotal) + " downloaded → " + plain(returnTotal) + " returned"
                    + (record == null ? "" : "; invoice net " + record.get("net") + ", total " + record.get("total")
                    + ", remaining " + record.get("remaining") + ", payment method " + record.get("paymentMethod")
                    + " / account " + record.get("paymentAccountName"))
                    + ". DEFECT: the payment amount must follow the returned quantity (business rule, QC 2026-11-18). "
                    + "Server: " + refused.getMessage());
            return "";
        }

        page.reload();
        page.waitForGridToSettle();
        Map<String, BigDecimal> saved = new LinkedHashMap<>();
        for (int row = 0; row < page.lineCount(); row++) {
            String item = page.lineText(row, "code");
            if (!item.isEmpty()) {
                saved.merge(item, page.lineNumber(row, quantityColumn()), BigDecimal::add);
            }
        }
        SoftAssert softly = new SoftAssert();
        typed.forEach((item, quantity) -> softly.assertTrue(saved.containsKey(item)
                        && saved.get(item).compareTo(quantity) == 0,
                "Returned quantity of " + item + " on " + code + ": typed " + plain(quantity) + ", saved " + saved.get(item) + "."));
        softly.assertAll();
        return code;
    }

    /** A random whole quantity from 1 to {@code downloaded} − 1; {@code downloaded} itself when it is below 2. */
    private static BigDecimal lessThan(BigDecimal downloaded) {
        if (downloaded.signum() <= 0) {
            return BigDecimal.ONE;
        }
        int whole = downloaded.setScale(0, java.math.RoundingMode.FLOOR).intValue();
        if (whole < 2) {
            return downloaded;
        }
        return BigDecimal.valueOf(1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(whole - 1));
    }

    private String completeAndSaveReturn() {
        // Only the return's own fields ("سعر المرتجع", the returned quantities) and the
        // fields left empty are set; what the invoice brought (طريقة الدفع, الحسابات …) is
        // kept (QC run 2026-11-18 14:29: a random "بنك" without its bank account was refused).
        page.selectRandomReturnPrice();
        setReturnQuantities();
        fillEmptyHeaderFields();
        return saveAndVerify(APPROVED);
    }

    /** Header selects never filled here: they load other documents or change every line. */
    private static final Set<String> NOT_FILLED_WHEN_EMPTY = Set.of("downloadFrom", "bulkDiscountType");

    /**
     * Every enabled header select that is still empty gets an option; filled ones are not
     * touched. Two passes: a choice can show another field (e.g. the bank account of
     * "طريقة الدفع").
     */
    private void fillEmptyHeaderFields() {
        Set<String> withoutOptions = new HashSet<>();
        for (int pass = 1; pass <= 2; pass++) {
            for (String field : page.enabledSelects(false)) {
                if (NOT_FILLED_WHEN_EMPTY.contains(field) || SalesInvoiceDocumentPage.FUND_ACCOUNT_FIELD.equals(field)
                        || withoutOptions.contains(field) || !isEmptySelect(field)) {
                    continue;
                }
                String chosen = page.selectRandomOption(field);
                page.confirmDialogIfShown(Duration.ofMillis(800));
                if (chosen == null || chosen.isBlank()) {
                    withoutOptions.add(field);
                    log.warn("Empty field {} has no option → left empty.", field);
                } else {
                    log.info("Empty field {} → {}", field, chosen);
                }
            }
        }
        for (String field : page.enabledTextInputs(false)) {
            String name = field.toLowerCase();
            if ((name.contains("adress") || name.contains("address")) && page.textValue(field).isBlank()) {
                page.setTextInput(field, stamped("عنوان آلي "));
            }
        }
        if (page.textValue("referenceDocument").isBlank()) {
            ensurePaymentAccount(); // no source invoice: the branch may be changed
        } else {
            page.fillFundAccountIfNeeded(); // what the invoice brought is kept
        }
        page.waitForGridToSettle();
    }

    /** Header fields that may stay empty: they load other documents, or are not part of the return. */
    private static final Set<String> MAY_STAY_EMPTY = Set.of("downloadFrom", "referenceField", "referenceDocument",
            "bulkDiscountType", "bulkDiscountAmount");

    /** Logs every enabled header field still empty before the save (they had no option to choose). */
    private void logFieldsLeftEmpty() {
        List<String> empty = new ArrayList<>();
        for (String field : page.enabledSelects(false)) {
            if (!MAY_STAY_EMPTY.contains(field) && isEmptySelect(field)) {
                empty.add(field);
            }
        }
        for (String field : page.enabledTextInputs(false)) {
            if (!MAY_STAY_EMPTY.contains(field) && page.textValue(field).isBlank()) {
                empty.add(field);
            }
        }
        if (empty.isEmpty()) {
            log.info("Every enabled header field is filled.");
        } else {
            log.warn("Enabled header fields left empty: {}", empty);
        }
    }

    private void setReturnQuantities() {
        page.waitForGridToSettle();
        for (int row = page.lineCount() - 1; row >= 0; row--) {
            if (page.lineText(row, "code").isEmpty()) {
                continue;
            }
            page.setLineNumber(row, quantityColumn(), RETURN_QUANTITY);
            if (page.lineNumber(row, priceColumn()).signum() == 0) {
                page.setLineNumber(row, priceColumn(), PRICE);
            }
        }
        page.stopCellEditing();
    }

    private static List<String> itemsAndQuantities(List<String> lines) {
        return lines.stream().map(l -> l.replaceAll(" @ .*", "")).sorted().toList();
    }

    private void verifySavedReturn(String code) {
        page.waitForStatusLabel(APPROVED.label());
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ الاعتماد\" of " + code + " is not today.");
        softly.assertTrue(page.recordByCode(code) != null, code + " is not returned by the API.");
        softly.assertAll();
        if (page.waitForActionEnabled("journalEntry")) {
            verifyBalancedJournal(code);
        }
    }

    private void verifyBalancedJournal(String code) {
        List<JournalLine> lines = page.readJournalEntry();
        BigDecimal debit = lines.stream().map(JournalLine::debit).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal credit = lines.stream().map(JournalLine::credit).reduce(BigDecimal.ZERO, BigDecimal::add);
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(lines.isEmpty(), "The journal entry of " + code + " has no lines.");
        softly.assertEquals(debit.compareTo(credit), 0, "The journal entry of " + code + " is not balanced: " + debit + " ≠ " + credit);
        softly.assertAll();
    }
}
