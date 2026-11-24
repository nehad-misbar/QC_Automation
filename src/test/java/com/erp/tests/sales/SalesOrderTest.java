package com.erp.tests.sales;

import com.erp.automation.base.SalesDocumentBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.sales.SalesOrderPage;
import com.erp.automation.pages.sales.SalesOrderPage.RecordStatus;
import com.erp.automation.pages.sales.SalesPage;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.CANCELLED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Sales Order (المبيعات → العمليات → أمر البيع), level 1.
 *
 * <p>The tests every Sales document screen shares (list, search, record
 * navigation, pager, Kanban, group by, print / PDF / Excel, attachments, exit,
 * log, empty save) come from {@link SalesDocumentBaseTest}. This class adds
 * the order's own scenarios: create (New / Add / Copy / Excel / from a
 * quotation / "انزال من"), edit and undo, delete, approve, disapprove, cancel,
 * toolbar per status, create a sales invoice, lines.</p>
 *
 * <p>Screen facts: see {@link SalesOrderPage} (QC 2026-11-12).</p>
 */
public class SalesOrderTest extends SalesDocumentBaseTest<SalesOrderPage> {

    private static final String SO = "sales-order";

    @Override
    protected void clickScreenLink(SalesPage salesPage) {
        salesPage.clickSalesOrders();
    }

    @Override
    protected SalesOrderPage createPage() {
        return new SalesOrderPage(driver);
    }

    @Override
    protected String screenPath() {
        return SalesOrderPage.SCREEN_PATH;
    }

    @Override
    protected List<String> listColumns() {
        return List.of("code", "saleOrderDate", "partnerName", "salesPersonName", "approvalDate", "total",
                "forignTotal", "referenceDocument", "recordStatus", "note");
    }

    @Override
    protected String codePrefix() {
        return "SO";
    }

    @Override
    protected String customerField() {
        return "partnerId";
    }

    @Override
    protected String quantityColumn() {
        return SalesOrderPage.QUANTITY_COLUMN;
    }

    @Override
    protected String priceColumn() {
        return SalesOrderPage.PRICE_COLUMN;
    }

    @Override
    protected String exportMenuItem() {
        return SalesOrderPage.MENU_EXPORT_EXCEL;
    }

    @Override
    protected String statusGroupLabel() {
        return "الحالة";
    }

    @Override
    protected List<String> requiredMarkedOnEmptySave() {
        return List.of("partnerId", "branchId", "warehouseId");
    }

    @Override
    protected DocumentStatus[] statusesAfterSave() {
        return new DocumentStatus[]{DRAFT, APPROVED};
    }

    @Override
    protected String notePrefix() {
        return "أمر بيع آلي ";
    }

    @Override
    protected Map<String, Object> openDocumentInEditMode() {
        Map<String, Object> record = openFreeDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        form.waitUntilFieldEditable("note");
        return record;
    }

    private static final Path EXCEL_LINES = Paths.get(System.getProperty("user.dir"),
            "test-data", "import-excel", "PurchaseOrder.xlsx");

    private static final int MAX_TEAM_ATTEMPTS = 5;
    private static final int MAX_QUOTATION_ATTEMPTS = 8;
    private static final org.openqa.selenium.By ADD_LINE_BUTTON =
            org.openqa.selenium.By.xpath("//button[normalize-space()='اضافة سطر']");
    private static final String BULK_DISCOUNT_PERCENT = "10";
    private static final String BULK_DISCOUNT_VALUE = "5";
    private static final String LINE_DISCOUNT_VALUE = "10";
    private static final String LINE_EXTRA_RATE = "2";
    private static final String LINE_EXTRA_VALUE = "5";
    private static final String LINE_FREE_QUANTITY = "1";
    private static final java.util.Random RANDOM = new java.util.Random();

    /** Header fields filled by {@link #fillHeader(boolean)}, checked after save + reload. */
    private static final List<String> FILLED_HEADER_FIELDS = List.of(
            "partnerId", "salesTeamId", "salesPersonId", "currencyId", "currencyRate", "branchId", "warehouseId",
            "costCenterId", "taxCalculationMethode", "prices", "bulkDiscountType", "bulkDiscountAmount",
            "salePriceListId", "paymentTerms", "saleShippingMethodId");
    private static final Set<String> NUMERIC_HEADER_FIELDS = Set.of("currencyRate", "bulkDiscountAmount");
    /** The fields asked for on a new order (QC 2026-11-15), which must not stay empty. */
    private static final List<String> REQUIRED_HEADER_FIELDS = List.of(
            "partnerId", "salesTeamId", "salesPersonId", "currencyId", "currencyRate", "branchId", "warehouseId",
            "costCenterId", "prices", "bulkDiscountType", "bulkDiscountAmount");

    /** Lines filled so far by {@link #fillNewLine(int)}: even ones get the rates, odd ones the values. */
    private int filledLines;

    // =====================================================================
    // Shared tests of SalesDocumentBaseTest, numbered in this page's run order
    // (priority 1, 2, 3 … over ALL the tests of this page). Each one only
    // calls the shared test; the steps are in SalesDocumentBaseTest.
    // =====================================================================

    @Override
    @Test(priority = 1, groups = {SO, "navigation"},
            description = "المبيعات → العمليات → the screen link opens its list with its columns and \"جديد\"")
    public void listOpensFromSalesMenu() {
        super.listOpensFromSalesMenu();
    }

    @Override
    @Test(priority = 2, groups = {SO, "navigation"},
            description = "\"جديد\" opens an empty document: a new number, today's date, no customer, no status")
    public void newButtonOpensEmptyDocument() {
        super.newButtonOpensEmptyDocument();
    }

    @Override
    @Test(priority = 3, groups = {SO, "navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that document")
    public void userCanOpenDocumentFromListRowActions() {
        super.userCanOpenDocumentFromListRowActions();
    }

    @Override
    @Test(priority = 4, groups = {SO, "navigation", "list"},
            description = "Double-click on a list row opens that document with the status shown in the list")
    public void userCanOpenDocumentByDoubleClickOnListRow() {
        super.userCanOpenDocumentByDoubleClickOnListRow();
    }

    @Override
    @Test(priority = 27, groups = {SO, "validation"},
            description = "Saving an empty document is blocked: error toast, required fields marked, stays in Add")
    public void userCannotSaveEmptyDocument() {
        super.userCannotSaveEmptyDocument();
    }

    @Override
    @Test(priority = 29, groups = {SO, "search"},
            description = "List search by number shows that document")
    public void userCanSearchListByCode() {
        super.userCanSearchListByCode();
    }

    @Override
    @Test(priority = 30, groups = {SO, "search"},
            description = "List search for a text no document has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {
        super.listSearchWithoutMatchShowsNoRows();
    }

    @Override
    @Test(priority = 31, groups = {SO, "search"},
            description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenDocumentFromRecordSearch() {
        super.userCanOpenDocumentFromRecordSearch();
    }

    @Override
    @Test(priority = 32, groups = {SO, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenRecords() {
        super.userCanNavigateBetweenRecords();
    }

    @Override
    @Test(priority = 33, groups = {SO, "pagination"},
            description = "List pager: first/last page, Next page by page to the end, back to a random page")
    public void userCanNavigateListPagination() {
        super.userCanNavigateListPagination();
    }

    @Override
    @Test(priority = 34, groups = {SO, "list"},
            description = "Kanban shows the same documents as the list page; a card opens its document")
    public void userCanSwitchListToKanbanAndBack() {
        super.userCanSwitchListToKanbanAndBack();
    }

    @Override
    @Test(priority = 35, groups = {SO, "list"},
            description = "Group by status: every group has documents and the groups add up to the list total")
    public void userCanGroupListByStatus() {
        super.userCanGroupListByStatus();
    }

    @Override
    @Test(priority = 36, groups = {SO, "print"},
            description = "Print a random document: the preview shows its number; printed through Chrome's dialog")
    public void userCanPrintDocument() throws java.awt.AWTException {
        super.userCanPrintDocument();
    }

    @Override
    @Test(priority = 37, groups = {SO, "print"},
            description = "Save a random document as PDF: a PDF file is downloaded")
    public void userCanSaveDocumentAsPdf() {
        super.userCanSaveDocumentAsPdf();
    }

    @Override
    @Test(priority = 38, groups = {SO, "excel"},
            description = "⋮ → export to Excel: an .xlsx with the document's items is downloaded")
    public void userCanExportDocumentToExcel() {
        super.userCanExportDocumentToExcel();
    }

    @Override
    @Test(priority = 42, groups = {SO, "attachments"},
            description = "Attachments added in edit mode are listed and kept after save and reload")
    public void userCanAddAttachments() {
        super.userCanAddAttachments();
    }

    @Override
    @Test(priority = 43, groups = {SO, "attachments"},
            description = "Deleting an attachment in edit mode is kept after save and reload")
    public void userCanDeleteAttachment() {
        super.userCanDeleteAttachment();
    }

    @Override
    @Test(priority = 44, groups = {SO, "exit"},
            description = "Close a new, a viewed and an edited document: each returns to the list")
    public void userCanExitDocument() {
        super.userCanExitDocument();
    }

    @Override
    @Test(priority = 45, groups = {SO, "log"},
            description = "Transaction log: the first entry is the creation")
    public void userCanViewTransactionLog() {
        super.userCanViewTransactionLog();
    }

    // =====================================================================
    // Create
    // =====================================================================

    @Test(priority = 5, groups = {SO, "create"},
            description = "New order from \"جديد\": every header field (فريق المبيعات before مندوب المبيعات, "
                    + "العملة, مركز التكلفة, الأسعار, تعميم خصم على الأصناف + its amount) and an item line with "
                    + "every editable cell; saved, and the header is the same after reload")
    public void userCanCreateNewSalesOrder() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        Map<String, String> header = headerBeforeSave(true);
        String code = saveAndVerify(DRAFT, APPROVED);
        assertTrue(page.recordByCode(code) != null, code + " is not returned by the API after saving.");
        verifyHeaderKeptAfterReload(code, header);
    }

    @Test(priority = 6, groups = {SO, "create"},
            description = "\"إضافة\" on an opened order creates a NEW order (every header field and line cell "
                    + "filled); the opened one is unchanged and the new header is kept after reload")
    public void userCanAddNewSalesOrderFromOpenedOne() throws InterruptedException {

        Map<String, Object> opened = openRandom(r -> true);
        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        Map<String, String> header = headerBeforeSave(true);
        String code = saveAndVerify(DRAFT, APPROVED);
        verifyNewDocumentSaved(opened, "\"إضافة\"");
        verifyHeaderKeptAfterReload(code, header);
    }

    @Test(priority = 7, groups = {SO, "create"},
            description = "\"نسخ\" (copy) of an order, every header field changed and a full line added, saved as "
                    + "a new order; the source is unchanged and the new header is kept after reload")
    public void userCanCopySalesOrder() {

        Map<String, Object> source = openRandom(r -> !SalesOrderPage.isCancelled(r));
        page.clickCopy();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        Map<String, String> header = headerBeforeSave(true);
        String code = saveAndVerify(DRAFT, APPROVED);
        verifyNewDocumentSaved(source, "\"نسخ\"");
        verifyHeaderKeptAfterReload(code, header);
    }

    @Test(priority = 8, groups = {SO, "create", "excel"},
            description = "New order whose lines are imported from Excel (⋮ → الاستيراد من Excel)")
    public void userCanCreateSalesOrderFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        page.importLinesFromExcel(EXCEL_LINES);
        int lines = form.gridRowCount();
        assertTrue(lines > 0, "Excel import finished but no lines were added (file " + EXCEL_LINES + ").");
        addItemLine();
        saveAndVerify(DRAFT, APPROVED);
    }

    @Test(priority = 9, groups = {SO, "create", "reference"},
            description = "⋮ → \"متابعة من عرض سعر\" → an approved quotation → download: its customer and lines are "
                    + "brought in; \"المستند المرجعي\" is kept after save")
    public void userCanCreateSalesOrderFromQuotationThroughMenu() {

        String quotation = downloadQuotationWithBranchAndWarehouse(
                () -> page.followFromMenu(SalesOrderPage.MENU_FOLLOW_QUOTATION));
        verifyReferenceDownloadedAndSave(quotation);
    }

    @Test(priority = 10, groups = {SO, "create", "reference"},
            description = "\"المستند المرجعي\" (first button) → an approved quotation → download (second button): "
                    + "its customer and lines are brought in; the rest of the header and a full line are filled; "
                    + "the reference and the header are kept after save")
    public void userCanCreateSalesOrderFromQuotationThroughReferenceField() {

        String quotation = downloadQuotationWithBranchAndWarehouse(page::selectRandomReferenceDocument);
        verifyReferenceDownloadedAndSave(quotation);
    }

    @Test(priority = 11, groups = {SO, "create", "lines"},
            description = "New order, every header field filled; \"انزال من\" = أوامر البيع → \"الرقم\" = a random "
                    + "order → download: its lines are added; a full line is added; the downloaded items and the "
                    + "header are kept after save")
    public void userCanDownloadLinesFromAnotherSalesOrder() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        String number = downloadLinesFromAnotherOrder();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(form.getSelectedText("downloadFrom"), SalesOrderPage.DOWNLOAD_SOURCE,
                "\"انزال من\" does not show " + SalesOrderPage.DOWNLOAD_SOURCE + ".");
        softly.assertEquals(page.textValue("referenceField").trim(), number, "\"الرقم\" does not show " + number + ".");
        softly.assertAll();
        fillZeroQuantitiesAndPrices();
        Set<String> downloaded = new HashSet<>(lineCodes());
        downloaded.remove("");
        log.info("Items downloaded from {}: {}", number, downloaded);
        addItemLine();
        Map<String, String> header = headerBeforeSave(true);
        String code = saveAndVerify(DRAFT, APPROVED);

        verifyHeaderKeptAfterReload(code, header);
        page.waitForGridToSettle();
        Set<String> saved = lineCodes();
        assertTrue(saved.containsAll(downloaded),
                "Items downloaded from " + number + " are missing in " + code + " after save: " + downloaded + " vs " + saved);
    }

    // =====================================================================
    // Edit / Undo
    // =====================================================================

    @Test(priority = 12, groups = {SO, "edit"},
            description = "Edit a Draft: every editable header field (team before sales person, currency, prices, "
                    + "bulk discount) and a new line with every editable cell; it stays a Draft and the header "
                    + "is kept after reload")
    public void userCanEditDraftSalesOrder() {

        openFreeDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillHeader(true);
        addItemLine();
        // Edit mode locks "مركز التكلفة" (and the rate of the base currency): kept, not required.
        Map<String, String> header = headerBeforeSave(false);
        String code = saveAndVerify(DRAFT);
        verifyHeaderKeptAfterReload(code, header);
    }

    @Test(priority = 13, groups = {SO, "edit", "validation"},
            description = "Draft in edit mode: header fields editable; number, rate, tax number, approval date and "
                    + "\"المستند المرجعي\" are not; \"انزال من\" is offered")
    public void draftSalesOrderEditModeHasExpectedEditableFields() {

        openFreeDraft();
        String code = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        form.waitUntilFieldEditable("note");

        SoftAssert softly = new SoftAssert();
        for (String field : List.of("partnerId", "salesPersonId", "currencyId", "salesTeamId", "saleOrderDate",
                "warehouseId", "orderDeadLine", "branchId", "taxCalculationMethode", "salePriceListId",
                "paymentTerms", "saleShippingMethodId", "deliveryAddress", "note")) {
            softly.assertTrue(page.isInputEditable(field), "\"" + field + "\" must be editable on Draft " + code + ".");
        }
        for (String field : SalesOrderPage.READ_ONLY_FIELDS) {
            softly.assertFalse(page.isInputEditable(field), "\"" + field + "\" must be read-only on Draft " + code + ".");
        }
        softly.assertFalse(page.isInputEditable("referenceDocument"), "\"المستند المرجعي\" must be locked when editing " + code + ".");
        softly.assertTrue(page.hasField("downloadFrom", Duration.ofSeconds(2)), "\"انزال من\" is not offered when editing " + code + ".");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 14, groups = {SO, "edit"},
            description = "Edit a Draft (all fields + a line), then Undo restores the saved values")
    public void userCanUndoSalesOrderChanges() {

        openFreeDraft();
        String code = page.waitForCode();
        List<String> fields = List.of("partnerId", "salesPersonId", "salesTeamId", "branchId", "warehouseId",
                "taxCalculationMethode", "salePriceListId", "paymentTerms", "note");
        Map<String, String> saved = page.readHeaderValues(fields);
        int savedLines = page.lineCount();

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillHeader(true);
        addItemLine();
        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        page.clickUndo();
        page.confirmDialog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(page::code, code), code, "Undo did not restore the number of " + code + ".");
        saved.forEach((field, value) -> softly.assertEquals(
                waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), value), value,
                "Undo did not restore \"" + field + "\" of " + code + "."));
        softly.assertEquals(waitUntilEquals(() -> String.valueOf(page.lineCount()), String.valueOf(savedLines)),
                String.valueOf(savedLines), "Undo did not remove the added line of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 15, groups = {SO, "edit"},
            description = "Header values of an edited Draft are the same after save and reload")
    public void editedSalesOrderHeaderIsKeptAfterSave() {

        openFreeDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillHeader(true);
        fillZeroQuantitiesAndPrices();
        List<String> fields = List.of("partnerId", "salesPersonId", "currencyId", "salesTeamId", "saleOrderDate",
                "warehouseId", "orderDeadLine", "branchId", "taxCalculationMethode", "salePriceListId",
                "paymentTerms", "saleShippingMethodId", "prices");
        Map<String, String> beforeSave = page.readHeaderValues(fields);
        String address = page.textValue("deliveryAddress");

        String code = saveAndVerify(DRAFT);
        page.reload();

        SoftAssert softly = new SoftAssert();
        for (String field : fields) {
            String expected = beforeSave.get(field);
            softly.assertEquals(waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), expected), expected,
                    "Field " + field + " of " + code + " changed after save + reload.");
        }
        softly.assertEquals(page.textValue("deliveryAddress"), address, "\"عنوان التوصيل\" of " + code + " changed after save.");
        softly.assertAll();
    }

    // =====================================================================
    // Status actions
    // =====================================================================

    @Test(priority = 16, groups = {SO, "status"},
            description = "Delete a Draft: success message, not in the list nor in the API any more")
    public void userCanDeleteDraftSalesOrder() {

        openFreeDraft();
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft " + code + ".");
        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);

        page.closeForm();
        page.waitForListLoaded();
        page.searchList(code);
        assertFalse(page.waitUntilListShows(code), code + " is still listed after deletion.");
        assertTrue(page.recordByCode(code) == null, code + " is still returned by the API after deletion.");
    }

    @Test(priority = 17, groups = {SO, "status", "log"},
            description = "Approve a Draft: Approved after reload, approval date today, edit and delete disabled, "
                    + "an approval log entry, ⋮ \"انشاء فاتورة مبيعات\" enabled")
    public void userCanApproveDraftSalesOrder() {

        openFreeDraft();
        String code = page.waitForCode();
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, "approve", this::toggleApproval);
        assertAccepted("Approving " + code, "approve", outcome);
        page.waitForStatusLabel(APPROVED.label());
        page.reload();
        page.waitForStatusLabel(APPROVED.label());
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ الاعتماد\" of " + code + " is not today.");
        softly.assertFalse(page.isActionEnabled("delete"), "Approved " + code + " must not be deletable.");
        softly.assertFalse(page.isActionEnabled("edit"), "Approved " + code + " must not be editable (QC 2026-11-12).");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1, "Approving " + code + " did not add exactly one log entry.");
        if (!logAfter.isEmpty()) {
            softly.assertEquals(logAfter.get(logAfter.size() - 1), LOG_APPROVED, "The last log entry of " + code + " is not the approval.");
        }
        softly.assertTrue(page.isMenuItemEnabled(SalesOrderPage.MENU_CREATE_INVOICE),
                "\"" + SalesOrderPage.MENU_CREATE_INVOICE + "\" must be enabled on Approved " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 18, groups = {SO, "status"},
            description = "Disapprove an Approved order without invoice: back to Draft, edit and delete enabled again")
    public void userCanDisapproveApprovedSalesOrder() {

        openFreeApproved();
        String code = page.waitForCode();
        ActionOutcome outcome = performAndRecord(form, "approve", this::toggleApproval);
        if (outcome.mentions(LINKED_ERROR)) {
            throw new SkipException(code + " is linked to other operations: " + outcome);
        }
        assertAccepted("Disapproving " + code, "approve", outcome);
        page.waitForStatusLabel(DRAFT.label());
        page.reload();
        page.waitForStatusLabel(DRAFT.label());

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete", "approveOrDisapprove", "cancel")) {
            softly.assertTrue(page.waitForActionEnabled(action), "'" + action + "' must be enabled on disapproved " + code + ".");
        }
        softly.assertEquals(page.inputValue("approvalDate"), "", "Disapproved " + code + " still has an approval date.");
        softly.assertAll();
    }

    @Test(priority = 19, groups = {SO, "cancel"},
            description = "Cancel a Draft: Cancelled after reload; edit/delete/cancel/approve/attachments disabled")
    public void userCanCancelDraftSalesOrder() {

        openFreeDraft();
        String code = page.waitForCode();
        assertAccepted("Cancelling " + code, "cancel", cancelOpenedDocument());
        verifyCancelled(code);
    }

    @Test(priority = 20, groups = {SO, "cancel"},
            description = "Cancel an Approved order without invoice: Cancelled after reload")
    public void userCanCancelApprovedSalesOrder() {

        openFreeApproved();
        String code = page.waitForCode();
        ActionOutcome outcome = cancelOpenedDocument();
        if (outcome.mentions(LINKED_ERROR)) {
            throw new SkipException(code + " is linked to other operations: " + outcome);
        }
        assertAccepted("Cancelling " + code, "cancel", outcome);
        verifyCancelled(code);
    }

    @Test(priority = 21, groups = {SO, "cancel", "validation"},
            description = "Approved order already invoiced: disapprove and cancel are disabled")
    public void invoicedApprovedSalesOrderCannotBeCancelledOrDisapproved() {

        // QC 2026-11-12 (SO2608-000001, isInvoice): "عدم اعتماد" is disabled but "إلغاء" is
        // ENABLED → expected to fail on "cancel" until the analysis team confirms the rule.
        Map<String, Object> record = openRandom(r -> RecordStatus.APPROVED.of(r) && SalesOrderPage.isInvoiced(r));
        page.waitForStatusLabel(APPROVED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("copy");

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("cancel", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' is enabled on " + code
                    + ", which already has a sales invoice (isInvoice: " + record.get("isInvoice") + ").");
        }
        softly.assertAll();
    }

    @Test(priority = 22, groups = {SO, "validation", "status"},
            description = "A sales invoice is made from an APPROVED order only: a Draft offers no "
                    + "\"انشاء فاتورة مبيعات\", and no Draft order in QC is marked as invoiced (isInvoice)")
    public void draftSalesOrderCannotBeInvoiced() {

        // Business rule (QC team, 2026-11-15): an invoice must not be made from a Draft order.
        // This test used to open a Draft WITH an invoice (SO2609-000009) and check its toolbar,
        // i.e. it took that state as valid. Such a Draft is now reported as a defect instead.
        openFreeDraft();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.menuItems().contains(SalesOrderPage.MENU_CREATE_INVOICE),
                "\"" + SalesOrderPage.MENU_CREATE_INVOICE + "\" must not be offered on Draft " + code + ".");

        List<String> invoicedDrafts = page.api()
                .find(r -> RecordStatus.DRAFT.of(r) && !SalesOrderPage.isCancelled(r) && SalesOrderPage.isInvoiced(r))
                .stream().map(SalesOrderPage::codeOf).toList();
        log.info("Draft orders marked as invoiced in QC: {}", invoicedDrafts);
        softly.assertTrue(invoicedDrafts.isEmpty(),
                "Draft sales orders must not have a sales invoice, but QC has: " + invoicedDrafts + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Toolbar per status
    // =====================================================================

    @Test(priority = 23, groups = {SO, "validation"},
            description = "New order toolbar: save/close/attachments/record search enabled; the rest disabled; "
                    + "⋮ has the Excel import and \"متابعة من عرض سعر\"")
    public void newSalesOrderEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        // The toolbar is enabled in steps: ExtentReport 2026-11-15 15:51 read "حفظ" as disabled
        // right after "✕" was enabled; timed on QC, ✕ is enabled ~0.6 s after "جديد" and
        // "حفظ" ~1.1 s. Each enabled action is waited for (up to 10 s), and the disabled
        // ones are read only after that, once the toolbar has settled.
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("save", "xIcon", "uploadAttachment", "recordSearch")) {
            softly.assertTrue(page.waitForActionEnabled(action), "'" + action + "' must be enabled on a new order.");
        }
        for (String action : List.of("copy", "add", "undo", "delete", "print", "approveOrDisapprove", "cancel", "transactionLogs")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new order.");
        }
        List<String> menu = page.menuItems();
        softly.assertTrue(menu.contains(SalesOrderPage.MENU_IMPORT_EXCEL), "⋮ has no Excel import: " + menu);
        softly.assertTrue(menu.contains(SalesOrderPage.MENU_FOLLOW_QUOTATION), "⋮ has no \"متابعة من عرض سعر\": " + menu);
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 24, groups = {SO, "validation", "status"},
            description = "Draft without invoice: edit/add/copy/delete/approve/cancel/print enabled, no approval date, "
                    + "⋮ has no \"انشاء فاتورة مبيعات\"")
    public void draftSalesOrderEnablesEditActions() {

        openFreeDraft();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "add", "copy", "delete", "approveOrDisapprove", "cancel", "print",
                "uploadAttachment", "transactionLogs", "itemsPricing", "printBarcode")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("save"), "'save' must be disabled in view mode.");
        softly.assertEquals(page.inputValue("approvalDate"), "", "Draft " + code + " must not have an approval date.");
        softly.assertFalse(page.menuItems().contains(SalesOrderPage.MENU_CREATE_INVOICE),
                "\"" + SalesOrderPage.MENU_CREATE_INVOICE + "\" must not be offered on Draft " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 25, groups = {SO, "validation", "status"},
            description = "Approved without invoice: edit and delete disabled; disapprove/cancel/copy/print enabled; "
                    + "approval date set")
    public void approvedSalesOrderIsLocked() {

        openFreeApproved();
        String code = page.waitForCode();
        page.waitForActionEnabled("copy");

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on Approved " + code + ".");
        }
        for (String action : List.of("approveOrDisapprove", "cancel", "copy", "add", "print")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Approved " + code + ".");
        }
        softly.assertFalse(page.inputValue("approvalDate").isBlank(), "Approved " + code + " has no approval date.");
        softly.assertAll();
    }

    @Test(priority = 26, groups = {SO, "validation", "status"},
            description = "Cancelled order is read-only: edit/delete/cancel/approve/attachments disabled")
    public void cancelledSalesOrderIsReadOnly() {

        if (tryOpenRandom(SalesOrderPage::isCancelled).isEmpty()) {
            openFreeDraft();
            assertAccepted("Cancelling " + page.code(), "cancel", cancelOpenedDocument());
            page.reload();
        }
        String code = page.waitForCode();
        page.waitForStatusLabel(CANCELLED.label());
        page.waitForActionEnabled("copy");

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete", "cancel", "approveOrDisapprove", "uploadAttachment")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        softly.assertAll();
    }

    @Test(priority = 28, groups = {SO, "validation"},
            description = "Header complete but no item line: saving is refused with \"يجب اضافة صنف واحد على الأقل\"")
    public void userCannotSaveSalesOrderWithoutLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectCustomerBranchAndWarehouse();
        page.clickSave();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isError() && toast.fullText().contains(NO_LINES_ERROR), "Expected \"" + NO_LINES_ERROR + "\", got: " + toast);
        assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        page.closeForm();
    }

    // =====================================================================
    // Follow-up
    // =====================================================================

    @Test(priority = 39, groups = {SO, "follow-up"}, description = "Items Pricing ($): Sales → Pricing opens with the order's items")
    public void userCanOpenItemsPricingFromSalesOrder() {

        openRandom(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("code");
        page.clickItemsPricing();
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> priced = page.gridColumnTexts("itemCode");
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("/sales/pricing/add"), "Pricing screen did not open: " + url);
        softly.assertEquals(new HashSet<>(priced), new HashSet<>(items), "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 40, groups = {SO, "follow-up", "print"}, description = "Print Barcode: Item Barcodes opens with the order's items")
    public void userCanOpenPrintBarcodeFromSalesOrder() {

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

    @Test(priority = 41, groups = {SO, "follow-up"},
            description = "Approved order → ⋮ → انشاء فاتورة مبيعات: a new invoice that refers to it, with its "
                    + "customer, branch, warehouse and items (not saved)")
    public void userCanCreateSalesInvoiceFromApprovedSalesOrder() {

        Map<String, Object> record = openFreeApproved();
        page.waitForGridToSettle();
        String code = page.waitForCode();
        String customer = waitForSelected(form, "partnerId");
        String branch = waitForSelected(form, "branchId");
        String warehouse = waitForSelected(form, "warehouseId");
        Set<String> items = lineCodes();

        page.clickMenuItemAndFollow(SalesOrderPage.MENU_CREATE_INVOICE, SalesOrderPage.INVOICE_ADD_PATH);

        // QC 2026-11-12: /sales/invoices/add/41?origin=SalesOrderStore, prefix invoices.
        DocumentForm invoice = new DocumentForm(driver, "invoices");
        assertEquals(waitUntilEquals(() -> page.inputValueById("referenceDocument"), code), code,
                "The new invoice does not refer to " + code + ".");
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains(SalesOrderPage.INVOICE_ADD_PATH + record.get("id")),
                "The invoice is not made from " + code + ": " + driver.getCurrentUrl());
        softly.assertEquals(waitForSelected(invoice, "vendorId"), customer, "Customer of the invoice differs from " + code + ".");
        softly.assertEquals(waitForSelected(invoice, "branchId"), branch, "Branch of the invoice differs from " + code + ".");
        softly.assertEquals(waitForSelected(invoice, "warehouseId"), warehouse, "Warehouse of the invoice differs from " + code + ".");
        softly.assertEquals(lineCodes(), items, "Items of the invoice differ from " + code + ".");
        softly.assertAll();
        // Left unsaved on purpose.
    }

    // =====================================================================
    // Lines and header extras
    // =====================================================================

    @Test(priority = 46, groups = {SO, "lines"}, description = "A new order keeps its lines (item, quantity, price) after save and reload")
    public void savedSalesOrderKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        List<String> before = linesSnapshot();
        saveAndVerify(DRAFT, APPROVED);
        page.reload();
        page.waitForGridToSettle();
        assertEquals(linesSnapshot().stream().sorted().toList(), before.stream().sorted().toList(), "Lines changed after save and reload.");
    }

    @Test(priority = 47, groups = {SO, "lines", "edit"},
            description = "Draft: changing the quantity, the price and the discount % of a line is saved")
    public void userCanEditExistingLineOfSalesOrder() {

        openFreeDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillZeroQuantitiesAndPrices();
        if (page.lineCount() == 0) {
            addItemLine();
        }
        String item = page.lineText(0, "code");
        BigDecimal quantity = page.lineNumber(0, quantityColumn()).add(BigDecimal.ONE);
        BigDecimal price = page.lineNumber(0, priceColumn()).add(BigDecimal.TEN);
        page.setLineNumber(0, quantityColumn(), quantity.toPlainString());
        page.setLineNumber(0, priceColumn(), price.toPlainString());
        page.setLineNumber(0, "discountRate", LINE_DISCOUNT_RATE);
        page.stopCellEditing();

        saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        int row = page.lineTexts("code").indexOf(item);
        assertTrue(row >= 0, "Item " + item + " is not in the lines after save.");
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineNumber(row, quantityColumn()).compareTo(quantity), 0, "Quantity of " + item + " was not saved.");
        softly.assertEquals(page.lineNumber(row, priceColumn()).compareTo(price), 0, "Price of " + item + " was not saved.");
        softly.assertEquals(page.lineNumber(row, "discountRate").compareTo(new BigDecimal(LINE_DISCOUNT_RATE)), 0,
                "Discount % of " + item + " was not saved.");
        softly.assertAll();
    }

    @Test(priority = 48, groups = {SO, "lines"}, description = "New order: deleting an item line is saved")
    public void userCanDeleteItemLineOfSalesOrder() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        int before = page.lineCount();
        page.deleteLine(0);
        assertEquals(page.lineCount(), before - 1, "Deleting a line did not remove it from the grid.");
        saveAndVerify(DRAFT, APPROVED);
        page.reload();
        page.waitForGridToSettle();
        assertEquals(page.lineCount(), before - 1, "The deleted line came back after save and reload.");
    }

    @Test(priority = 49, groups = {SO, "sales"}, description = "\"تعميم خصم على الأصناف\" (نسبة %) is saved with the order")
    public void userCanApplyBulkDiscountToSalesOrder() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        assertTrue(page.setBulkDiscount("نسبة %", "10"), "\"تعميم خصم على الأصناف\" could not be set.");
        String code = saveAndVerify(DRAFT, APPROVED);
        page.reload();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitForSelected(form, "bulkDiscountType"), "نسبة %", "The discount type of " + code + " was not kept.");
        softly.assertEquals(number(page.textValue("bulkDiscountAmount")).compareTo(BigDecimal.TEN), 0,
                "The discount amount of " + code + " was not kept.");
        softly.assertAll();
    }

    @Test(priority = 50, groups = {SO, "sales"},
            description = "العميل fills its tax number; الفرع empties the warehouse and limits the warehouses")
    public void customerAndBranchFillDependentFieldsOfSalesOrder() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectIfEditable("partnerId", false);
        String tax = waitUntilEquals(() -> page.textValue("taxNumber").isBlank() ? "" : "filled", "filled");
        selectBranchThenWarehouse(false);
        String warehouse = form.getSelectedText("warehouseId");
        page.selectRandomOtherOption("branchId");
        page.confirmDialogIfShown(Duration.ofMillis(800));
        String afterBranchChange = form.getSelectedText("warehouseId");
        log.info("Tax number [{}], warehouse [{}] → after another branch [{}]", page.textValue("taxNumber"), warehouse, afterBranchChange);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(tax, "filled", "\"الرقم الضريبي\" stayed empty after choosing the customer (it may have none in QC).");
        softly.assertTrue(afterBranchChange.isEmpty() || afterBranchChange.equals("اختر") || !afterBranchChange.equals(warehouse),
                "The warehouse [" + warehouse + "] was kept after choosing another branch.");
        softly.assertAll();
        page.closeForm();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Every header field, row by row (right to left), with "فريق المبيعات" BEFORE
     * "مندوب المبيعات" because the team limits the sales persons (QC 2026-11-15):
     * العميل → فريق المبيعات → مندوب المبيعات → العملة (+ التعادل) → التاريخ →
     * الفرع → المخزن (the branch empties the warehouse, so it comes first) →
     * مركز التكلفة → تاريخ الانتهاء → طريقة احتساب الضريبة → الأسعار →
     * تعميم خصم على الأصناف + قيمته → قائمة الأسعار → شروط السداد → طريقة الشحن →
     * عنوان التوصيل → البيان. A locked field (edit mode) is kept as it is.
     */
    @Override
    protected void fillHeader(boolean editing) {
        fillHeader(editing, false);
    }

    /**
     * @param fromSource true after a reference document was downloaded: its customer is
     *                   kept, and so are its branch and warehouse when it brought them.
     */
    private void fillHeader(boolean editing, boolean fromSource) {
        if (!fromSource) {
            selectIfEditable("partnerId", editing);
        }
        if (fromSource) {
            // After a quotation is downloaded, most header fields are locked (QC 2026-11-15:
            // only the dates, "عنوان التوصيل" and "البيان" stay editable). A locked field is
            // skipped at once instead of waiting 5 s for it to become editable.
            fillUnlockedHeaderFromSource();
            return;
        }
        selectTeamThenSalesPerson(editing);
        selectCurrency(editing);
        if (page.isInputEditable("saleOrderDate")) {
            page.setOrderDate(LocalDate.now());
        }
        if (!fromSource || isEmptySelect("warehouseId")) {
            selectBranchThenWarehouse(editing);
        }
        selectIfEditable("costCenterId", editing);
        if (page.isInputEditable("orderDeadLine")) {
            page.setDeadline(LocalDate.now().plusDays(editing ? 15 : 10));
        }
        selectIfEditable("taxCalculationMethode", editing);
        String prices = editing ? page.selectOtherPriceOption() : page.selectRandomPriceOption();
        log.info("الأسعار = {}", prices);
        setBulkDiscount(editing);
        selectIfEditable("salePriceListId", editing);
        selectIfEditable("paymentTerms", editing);
        selectIfEditable("saleShippingMethodId", editing);
        page.setDeliveryAddress(stamped("عنوان توصيل آلي "));
        String note = uniqueNote();
        page.setNote(note);
        assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
    }

    /** Header of an order made from a quotation: only the fields the quotation left unlocked. */
    private void fillUnlockedHeaderFromSource() {
        for (String field : List.of("salesTeamId", "salesPersonId", "currencyId", "costCenterId",
                "taxCalculationMethode", "salePriceListId", "paymentTerms", "saleShippingMethodId")) {
            if (page.isSelectDisabled(field)) {
                log.info("{} is locked by the quotation → kept as [{}].", field, form.getSelectedText(field));
            } else {
                selectIfEditable(field, false);
            }
        }
        if (page.isInputEditable("saleOrderDate")) {
            page.setOrderDate(LocalDate.now());
        }
        if (page.isInputEditable("orderDeadLine")) {
            page.setDeadline(LocalDate.now().plusDays(10));
        }
        if (page.isSelectDisabled("bulkDiscountType")) {
            log.info("\"تعميم خصم على الأصناف\" is locked by the quotation → kept as [{}].", form.getSelectedText("bulkDiscountType"));
        } else {
            setBulkDiscount(false);
        }
        log.info("الأسعار = [{}] (changed only when unlocked: {})", page.priceOption(), page.selectRandomPriceOption());
        page.setDeliveryAddress(stamped("عنوان توصيل آلي "));
        String note = uniqueNote();
        page.setNote(note);
        assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
    }

    /**
     * فريق المبيعات → مندوب المبيعات. The team limits the sales persons (QC 2026-11-15:
     * فريق مبيعات الرياض offers 4 of 32), so it is chosen first; a team without
     * sales persons is replaced by another one.
     */
    private void selectTeamThenSalesPerson(boolean editing) {
        if (!page.waitForSelectEnabled("salesTeamId", Duration.ofSeconds(5))) {
            log.info("salesTeamId is locked → kept as [{}].", form.getSelectedText("salesTeamId"));
            selectIfEditable("salesPersonId", editing);
            return;
        }
        for (int attempt = 1; attempt <= MAX_TEAM_ATTEMPTS; attempt++) {
            String team = (editing || attempt > 1) && !isEmptySelect("salesTeamId")
                    ? page.selectRandomOtherOption("salesTeamId")
                    : page.selectRandomOption("salesTeamId");
            page.confirmDialogIfShown(Duration.ofMillis(800));
            if (!page.waitForSelectEnabled("salesPersonId", Duration.ofSeconds(5))) {
                log.info("salesPersonId is locked after team [{}] → kept as [{}].", team, form.getSelectedText("salesPersonId"));
                return;
            }
            String person = editing && !isEmptySelect("salesPersonId")
                    ? page.selectRandomOtherOption("salesPersonId")
                    : page.selectRandomOption("salesPersonId");
            page.confirmDialogIfShown(Duration.ofMillis(800));
            if (!person.isEmpty()) {
                log.info("Sales team [{}] → sales person [{}]", team, person);
                return;
            }
            log.warn("Sales team [{}] offers no sales person → another team ({}/{}).", team, attempt, MAX_TEAM_ATTEMPTS);
        }
        fail("No sales team with a sales person after " + MAX_TEAM_ATTEMPTS + " attempts.");
    }

    /**
     * "العملة": a random currency (another one when editing), never the empty option QC
     * offers; "التعادل" must then show a rate (1 for the base currency, e.g. 3.75 for
     * الدولار الأمريكي, QC 2026-11-15).
     */
    private void selectCurrency(boolean editing) {
        if (!page.waitForSelectEnabled("currencyId", Duration.ofSeconds(5))) {
            log.info("currencyId is locked → kept as [{}].", form.getSelectedText("currencyId"));
            return;
        }
        String current = form.getSelectedText("currencyId");
        String currency = page.selectRandomOption("currencyId",
                option -> !option.isBlank() && !(editing && option.equals(current)));
        page.confirmDialogIfShown(Duration.ofMillis(800));
        String rate = waitUntilEquals(() -> number(page.textValue("currencyRate")).signum() > 0 ? "rate" : "", "rate");
        log.info("Currency [{}] → rate [{}]", currency, page.textValue("currencyRate"));
        assertEquals(rate, "rate", "\"التعادل\" shows no rate for currency [" + currency + "].");
    }

    /**
     * "تعميم خصم على الأصناف" + its amount: a random type for a new order, the other type
     * when editing; نسبة % → {@value #BULK_DISCOUNT_PERCENT}, قيمة → {@value #BULK_DISCOUNT_VALUE}.
     */
    private void setBulkDiscount(boolean editing) {
        String current = form.getSelectedText("bulkDiscountType");
        String type;
        if (editing && SalesOrderPage.DISCOUNT_PERCENT.equals(current)) {
            type = SalesOrderPage.DISCOUNT_VALUE;
        } else if (editing) {
            type = SalesOrderPage.DISCOUNT_PERCENT;
        } else {
            type = RANDOM.nextBoolean() ? SalesOrderPage.DISCOUNT_PERCENT : SalesOrderPage.DISCOUNT_VALUE;
        }
        String amount = SalesOrderPage.DISCOUNT_PERCENT.equals(type) ? BULK_DISCOUNT_PERCENT : BULK_DISCOUNT_VALUE;
        if (!page.setBulkDiscount(type, amount)) {
            log.info("\"تعميم خصم على الأصناف\" is locked → kept as [{}] [{}].", current, page.textValue("bulkDiscountAmount"));
            return;
        }
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitForSelected(form, "bulkDiscountType"), type, "\"تعميم خصم على الأصناف\" was not set to " + type + ".");
        softly.assertEquals(number(page.textValue("bulkDiscountAmount")).compareTo(new BigDecimal(amount)), 0,
                "The bulk discount amount was not set to " + amount + ".");
        softly.assertAll();
        log.info("Bulk discount [{}] = {}", type, amount);
    }

    /**
     * Every editable cell of a new line, in screen order (right to left): الوحدة → الكمية →
     * السعر → الخصم% or الخصم → الاضافة% or الاضافة → الكمية المجانية → البيان. The unit
     * comes before the price, because a new unit brings its own price. Every other line
     * gets the rates, the next one the values; the grid calculates the other cell of each pair.
     */
    @Override
    protected void fillNewLine(int row) {
        String unit = page.setLineOption(row, SalesOrderPage.COL_UNIT);
        page.setLineNumber(row, SalesOrderPage.QUANTITY_COLUMN, QUANTITY);
        page.setLineNumber(row, SalesOrderPage.PRICE_COLUMN, PRICE);
        boolean rates = filledLines++ % 2 == 0;
        if (rates) {
            page.setLineNumber(row, SalesOrderPage.COL_DISCOUNT_RATE, LINE_DISCOUNT_RATE);
            page.setLineNumber(row, SalesOrderPage.COL_EXTRA_RATE, LINE_EXTRA_RATE);
        } else {
            page.setLineNumber(row, SalesOrderPage.COL_DISCOUNT_VALUE, LINE_DISCOUNT_VALUE);
            page.setLineNumber(row, SalesOrderPage.COL_EXTRA_VALUE, LINE_EXTRA_VALUE);
        }
        page.setLineNumber(row, SalesOrderPage.COL_FREE_QUANTITY, LINE_FREE_QUANTITY);
        page.setLineText(row, SalesOrderPage.COL_NOTE, stamped("بيان سطر آلي "));
        page.stopCellEditing();

        String pairDiscount = rates ? SalesOrderPage.COL_DISCOUNT_VALUE : SalesOrderPage.COL_DISCOUNT_RATE;
        String pairExtra = rates ? SalesOrderPage.COL_EXTRA_VALUE : SalesOrderPage.COL_EXTRA_RATE;
        log.info("Line {}: unit [{}], qty {}, price {}, discount {} / {}, addition {} / {}, free {}, note [{}]", row + 1, unit,
                page.lineText(row, SalesOrderPage.QUANTITY_COLUMN), page.lineText(row, SalesOrderPage.PRICE_COLUMN),
                page.lineText(row, SalesOrderPage.COL_DISCOUNT_RATE), page.lineText(row, SalesOrderPage.COL_DISCOUNT_VALUE),
                page.lineText(row, SalesOrderPage.COL_EXTRA_RATE), page.lineText(row, SalesOrderPage.COL_EXTRA_VALUE),
                page.lineText(row, SalesOrderPage.COL_FREE_QUANTITY), page.lineText(row, SalesOrderPage.COL_NOTE));
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(page.lineNumber(row, pairDiscount).signum() > 0,
                "Line " + (row + 1) + ": \"" + pairDiscount + "\" was not calculated from the discount.");
        softly.assertTrue(page.lineNumber(row, pairExtra).signum() > 0,
                "Line " + (row + 1) + ": \"" + pairExtra + "\" was not calculated from the addition.");
        softly.assertAll();
    }

    /**
     * The header fields this class fills, read before saving. {@code allRequired}: a new
     * order, where {@link #REQUIRED_HEADER_FIELDS} can all be chosen, so none may be empty.
     */
    private Map<String, String> headerBeforeSave(boolean allRequired) {
        Map<String, String> header = page.readHeaderValues(FILLED_HEADER_FIELDS);
        log.info("Header before save: {}", header);
        if (allRequired) {
            SoftAssert softly = new SoftAssert();
            for (String field : REQUIRED_HEADER_FIELDS) {
                String value = header.get(field);
                softly.assertFalse(value == null || value.isBlank() || value.equals("اختر"),
                        "\"" + field + "\" is still empty before saving.");
            }
            softly.assertAll();
        }
        return header;
    }

    /** After save: reload, then every field of {@code before} shows the same value (numbers compared as numbers). */
    private void verifyHeaderKeptAfterReload(String code, Map<String, String> before) {
        page.reload();
        page.waitForCode();
        SoftAssert softly = new SoftAssert();
        before.forEach((field, expected) -> {
            if (expected == null || expected.isBlank() || expected.equals("اختر")) {
                return;
            }
            if (NUMERIC_HEADER_FIELDS.contains(field)) {
                String shown = waitUntilEquals(() -> number(page.readHeaderValues(List.of(field)).get(field))
                        .compareTo(number(expected)) == 0 ? expected : page.readHeaderValues(List.of(field)).get(field), expected);
                softly.assertEquals(shown, expected, "\"" + field + "\" of " + code + " changed after save + reload.");
            } else {
                String shown = waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), expected);
                softly.assertEquals(shown, expected, "\"" + field + "\" of " + code + " changed after save + reload.");
            }
        });
        softly.assertAll();
    }

    /** A Draft without invoice (form shows مسودة); created when QC has none. */
    private Map<String, Object> openFreeDraft() {
        java.util.Optional<Map<String, Object>> opened = tryOpenRandom(SalesOrderPage::isFreeDraft);
        if (opened.isPresent()) {
            page.waitForStatusLabel(DRAFT.label());
            return opened.get();
        }
        log.info("No free Draft sales order in QC → creating one.");
        return createDocument();
    }

    /** An Approved order without invoice; a Draft is created and approved when QC has none. */
    private Map<String, Object> openFreeApproved() {
        java.util.Optional<Map<String, Object>> opened = tryOpenRandom(SalesOrderPage::isFreeApproved);
        if (opened.isPresent()) {
            page.waitForStatusLabel(APPROVED.label());
            return opened.get();
        }
        log.info("No free Approved sales order in QC → creating and approving one.");
        createDocument();
        String code = page.code();
        if (!APPROVED.label().equals(page.statusLabel())) {
            assertAccepted("Approving " + code, "approve", performAndRecord(form, "approve", this::toggleApproval));
        }
        page.reload();
        page.waitForStatusLabel(APPROVED.label());
        return page.recordByCode(code);
    }

    /**
     * "جديد" → {@code chooseQuotation} (⋮ "متابعة من عرض سعر" or the first button of
     * "المستند المرجعي") → download. The order then LOCKS the header fields brought from the
     * quotation, the required الفرع and المخزن included. ExtentReport 2026-11-15 13:54
     * (userCanCreateSalesOrderFromQuotationThroughMenu): SQ2610-000034 has no branch and no
     * warehouse, both stayed empty and locked, and 5 branch attempts failed. Such a quotation
     * cannot become an order, so the form is closed and another quotation is chosen.
     *
     * @return the downloaded quotation's number.
     */
    private String downloadQuotationWithBranchAndWarehouse(java.util.function.Supplier<String> chooseQuotation) {
        Set<String> rejected = new java.util.LinkedHashSet<>();
        for (int attempt = 1; attempt <= MAX_QUOTATION_ATTEMPTS; attempt++) {
            page.clickNew();
            form.waitForMode(DocumentForm.Mode.ADD);
            String quotation = chooseQuotation.get();
            page.downloadReferenceDocument();
            page.waitForGridToSettle();
            boolean missing = isEmptySelect("branchId") || isEmptySelect("warehouseId");
            if (!missing || page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(2))) {
                log.info("Quotation {}: branch [{}], warehouse [{}].", quotation,
                        form.getSelectedText("branchId"), form.getSelectedText("warehouseId"));
                return quotation;
            }
            rejected.add(quotation);
            log.warn("Quotation {} has no branch / warehouse and the order locks them → another quotation ({}/{}).",
                    quotation, attempt, MAX_QUOTATION_ATTEMPTS);
            page.clickAction("xIcon");
            if (waitForConfirmDialog(form)) {
                page.confirmDialog();
            }
            assertBackOnList("closing the order of quotation " + quotation);
        }
        throw new SkipException("No approved quotation with a branch and a warehouse found in QC after "
                + MAX_QUOTATION_ATTEMPTS + " attempts (rejected: " + rejected + ").");
    }

    /** "انزال من" = أوامر البيع → "الرقم" = a random order → download; returns that order's number. */
    private String downloadLinesFromAnotherOrder() {
        int before = form.gridRowCount();
        String number = page.downloadLinesFromAnotherOrder();
        log.info("Downloaded {} line(s) from {}.", form.gridRowCount() - before, number);
        assertTrue(form.gridRowCount() > before, "No lines were downloaded from " + number + ".");
        return number;
    }

    /** After a quotation was chosen: download it, check customer + lines, complete, save, check the reference. */
    private void verifyReferenceDownloadedAndSave(String quotation) {
        int lines = page.lineCount();
        String customer = waitForSelected(form, "partnerId");
        log.info("Quotation {} → customer [{}], {} line(s), url {}", quotation, customer, lines, driver.getCurrentUrl());

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(customer.isEmpty(), "The customer of " + quotation + " was not brought in.");
        softly.assertTrue(lines > 0, "No lines were brought from " + quotation + ".");
        softly.assertAll();

        // The rest of the header (the quotation's customer, branch and warehouse are kept).
        fillHeader(false, true);

        // ExtentReport 2026-11-15 14:08 (SQ2608-000022): an order made from a quotation has NO
        // "اضافة سطر" and every cell of its lines is read-only (checked on QC: all column
        // definitions not editable). Its lines are the quotation's and are only checked to be
        // kept; a full line is added only when the screen offers "اضافة سطر".
        boolean canAddLines = !driver.findElements(ADD_LINE_BUTTON).isEmpty();
        List<String> broughtLines = linesSnapshot();
        if (canAddLines) {
            fillZeroQuantitiesAndPrices();
            addItemLine();
        } else {
            log.info("Order from {} has no \"اضافة سطر\": its line(s) come from the quotation and are read-only: {}",
                    quotation, broughtLines);
        }
        // The header fields brought from the quotation are locked, so they may stay empty
        // when the quotation has none (e.g. no sales team): not required here.
        Map<String, String> header = headerBeforeSave(false);
        String code = saveAndVerify(DRAFT, APPROVED);
        assertEquals(page.textValue("referenceDocument").trim(), quotation, "\"المستند المرجعي\" of " + code + " was not kept.");
        Map<String, Object> record = page.recordByCode(code);
        if (record != null) {
            assertEquals(String.valueOf(record.get("referenceDocument")), quotation, "The API does not show " + quotation + " as the reference of " + code + ".");
        }
        verifyHeaderKeptAfterReload(code, header);
        assertEquals(page.textValue("referenceDocument").trim(), quotation, "\"المستند المرجعي\" of " + code + " was not kept after reload.");
        if (!canAddLines) {
            page.waitForGridToSettle();
            assertEquals(linesSnapshot().stream().sorted().toList(), broughtLines.stream().sorted().toList(),
                    "The lines of " + code + " are not the lines of " + quotation + " after save and reload.");
        }
    }

    private void verifyCancelled(String code) {
        page.waitForStatusLabel(CANCELLED.label());
        page.reload();
        page.waitForStatusLabel(CANCELLED.label());
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete", "cancel", "approveOrDisapprove", "uploadAttachment")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        Map<String, Object> record = page.recordByCode(code);
        softly.assertTrue(record != null && SalesOrderPage.isCancelled(record), "The API does not mark " + code + " as cancelled.");
        softly.assertAll();
    }
}
