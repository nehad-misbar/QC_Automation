package com.erp.tests;

import com.erp.automation.base.PurchasingBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.PurchaseRequisitionPage;
import com.erp.automation.pages.PurchaseRequisitionPage.RecordStatus;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.DocumentApi;
import com.erp.automation.ui.Downloads;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.CANCELLED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Purchase Requisition (طلبات الشراء), level 1: create, edit, status
 * actions, list, print/export, attachments, lines and toolbar.
 *
 * <p>Built on the Purchasing template: {@link PurchasingBaseTest},
 * {@code PurchasingDocumentPage} and {@link DocumentForm}. See
 * {@code PurchaseOrderTest} for the reference layout:</p>
 * <ul>
 *     <li>test data in {@link TestData};</li>
 *     <li>each test reads as the business scenario;</li>
 *     <li>steps are in "Reusable steps" at the bottom;</li>
 *     <li>every action ends with a check of its result.</li>
 * </ul>
 *
 * <p>A Draft is chosen from the list like on orders. The test reads the
 * "الحالة" column page by page. Records that need flags the list does not
 * show (for example "approved and not linked to a quotation or invoice")
 * are chosen through the application's API ({@code DocumentApi}) and opened
 * by their URL.</p>
 */
public class PurchaseRequisitionTest extends PurchasingBaseTest {

    private static final String PR = "purchase-requisition";

    private PurchaseRequisitionPage page;
    private DocumentForm form;

    @Override
    protected void openScreen(PurchasingPage purchasingPage) {
        purchasingPage.clickPurchaseRequisitions();
        page = new PurchaseRequisitionPage(driver);
        form = new DocumentForm(driver, PurchaseRequisitionPage.PREFIX);
        page.dismissInstallPrompt();
        page.waitForListLoaded();
    }

    // =====================================================================
    // Test data
    // =====================================================================

    private static final class TestData {

        static final String QUANTITY = "17";
        static final String PRICE = "100";

        /** "البيان" of the new item line. */
        static final String LINE_NOTE = "بيان سطر طلب شراء تجريبي";

        static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";

        /** A newly saved requisition is a Draft, or Approved when auto-approved on save. */
        static final DocumentStatus[] NEW_REQUISITION_STATUSES = {DRAFT, APPROVED};

        /** Times another supplier is chosen when the warehouse stays empty. */
        static final int MAX_SUPPLIER_ATTEMPTS = 5;

        /** Header values compared after save + reload. */
        static final List<String> PERSISTED_HEADER_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "currencyId",
                "purchaseRepresentativeId", "branchId", "warehouseId", "costCenterId",
                "expectedReceiptDate", "requestEndDate");  // "note" is checked by saveAndVerify

        /** Excel file with item lines ("تصدير الاصناف" layout, shared with orders). */
        static final Path EXCEL_LINES = Paths.get(System.getProperty("user.dir"),
                "test-data", "import-excel", "PurchaseOrder.xlsx");

        /** Days from today for "اخر موعد للاستلام" and "تاريخ الانتهاء" (a future end date keeps it from expiring). */
        static final int EXPECTED_RECEIPT_IN_DAYS = 14;
        static final int REQUEST_END_IN_DAYS = 30;

        private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        /** Unique description, so a persisted value can be told apart from old data. */
        static final String NOTE_PREFIX = "طلب شراء آلي ";

        static String uniqueNote() {
            return NOTE_PREFIX + LocalDateTime.now().format(STAMP);
        }

        /** Parts of the API calls that prove an action reached the server. */
        static final String API_CANCEL = "/cancel/";
        static final String API_APPROVE_OR_DISAPPROVE = "/approveOrDisApprove/";

        /** Grouping field of the list filter. */
        static final String GROUP_BY_STATUS = "الحالة";

        /** Card label of a disapproved document on the Kanban board (seen on orders). */
        static final String DISAPPROVED_CARD_LABEL = "غير معتمد";

        static final String LOG_CREATED = "انشاء العملية";

        static final String ATTACHMENT_TITLE = "مرفقات طلب شراء تجريبي";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات طلب الشراء";

        /** Line added to "البيان" of the e-mail. */
        static final String EMAIL_NOTE_LINE = "سطر إضافي من الاختبار الآلي لطلب الشراء";

        /** Title of the send dialog ("⋮" → "ارسال للمورد عن طريق الايميل"). */
        static final String SEND_DIALOG_TITLE = "إرسال طلب الشراء";

        // The e-mail test REALLY sends the e-mail (asked by the user, 2026-11-01).

        /** Required header fields: an empty requisition shows an error on them. */
        static final List<String> REQUIRED_HEADER_FIELDS = List.of("branchId", "warehouseId");

        /** Toolbar of a NEW requisition. */
        static final List<String> ACTIONS_DISABLED_ON_NEW = List.of(
                "edit", "delete", "approveOrDisapprove", "cancel", "copy");
        static final List<String> ACTIONS_ENABLED_ON_NEW = List.of("save", "xIcon");

        /** "⋮" items of an APPROVED requisition and the screens they open (QC 2026-11-01). */
        static final String CREATE_ORDER_MENU_ITEM = "انشاء امر شراء";
        static final String CREATE_ORDER_PATH = "/purchase/orders/add/";
        static final String ORDER_PREFIX = "orders";
        static final String CREATE_QUOTATION_MENU_ITEM = "إنشاء عرض سعر";
        static final String CREATE_QUOTATION_PATH = "/purchase/price-offers/add/";
        static final String QUOTATION_PREFIX = "price-offers";
        static final String COMPARE_MENU_ITEM = "مقارنة عروض الاسعار";
        static final String MANUAL_COMPARE_ITEM = "مقارنة يدوية";
        static final String MANUAL_COMPARE_PATH = "/purchase-quotation/manual/";
        static final String AUTO_COMPARE_ITEM = "مقارنة آلية";
        static final String AUTO_COMPARE_PATH = "/purchase-quotation/auto/";

        /** Server message when an approved requisition is linked to other operations. */
        static final String LINKED_ERROR = "مرتبط";

        /** Approved requisitions tried before the test is skipped. */
        static final int MAX_LINKED_ATTEMPTS = 5;

        /** Action of a log entry written when the requisition is approved. */
        static final String LOG_APPROVED = "اعتماد العملية";

        /** Toolbar actions that must be disabled on a cancelled requisition. */
        static final List<String> ACTIONS_DISABLED_WHEN_CANCELLED = List.of(
                "edit", "approveOrDisapprove", "cancel");

        /** Toolbar actions of a Draft (after disapproving it). */
        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "edit", "delete", "approveOrDisapprove", "cancel");

        private TestData() {
        }
    }

    // =====================================================================
    // Create
    // =====================================================================

    @Test(priority = 1, groups = {PR, "create"},
            description = "New requisition from the New button: header in screen order, "
                    + "lines downloaded from a random document (انزال من / الرقم), note, then an item line")
    public void userCanCreateNewPurchaseRequisition() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        addItemLine();
        saveAndVerify(TestData.NEW_REQUISITION_STATUSES);
    }

    @Test(priority = 3, groups = {PR, "create"},
            description = "Copy an existing requisition and save the copy as a new one")
    public void userCanDuplicatePurchaseRequisition() {

        Map<String, Object> source = openRandomRequisition(r -> !RecordStatus.CANCELLED.of(r));
        String sourceCode = String.valueOf(source.get("code"));

        page.clickCopy(); // no confirmation step for Copy
        form.waitForMode(DocumentForm.Mode.ADD);

        // Same steps as a new requisition, as in the old test and in
        // PurchaseOrderTest (fillManualOrder for a copied order): header in
        // screen order → انزال من / الرقم → البيان → a new item line.
        fillHeader();
        addItemLine(); // completes the copied / downloaded lines first (quantity, then price)
        String newCode = saveAndVerify(TestData.NEW_REQUISITION_STATUSES);

        assertNotEquals(newCode, sourceCode, "The copy was saved over the source requisition.");
    }

    @Test(priority = 4, groups = {PR, "create"},
            description = "New requisition from the Add button of an opened requisition")
    public void userCanAddPurchaseRequisitionFromOpenedOne() {

        Map<String, Object> opened = openRandomRequisition(r -> true);

        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        addItemLine();
        String newCode = saveAndVerify(TestData.NEW_REQUISITION_STATUSES);

        assertNotEquals(newCode, opened.get("code"), "\"إضافة\" saved over the opened requisition.");
    }

    @Test(priority = 5, groups = {PR, "create", "excel"},
            description = "New requisition whose lines are imported from Excel")
    public void userCanCreatePurchaseRequisitionFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        page.importLinesFromExcel(TestData.EXCEL_LINES);
        int lines = form.gridRowCount();
        log.info("Lines imported from Excel: {}", lines);
        assertTrue(lines > 0, "Excel import finished but no lines were added.");

        // Same as orders (fillManualOrder for IMPORTED_FROM_EXCEL): complete the
        // imported lines, then add one more item line with "اضافة سطر".
        addItemLine();
        saveAndVerify(TestData.NEW_REQUISITION_STATUSES);
    }

    // =====================================================================
    // Edit / Undo
    // =====================================================================

    @Test(priority = 6, groups = {PR, "edit"},
            description = "Edit a Draft requisition: header and a new line; it stays a Draft")
    public void userCanEditDraftPurchaseRequisition() {

        openDraftRequisition();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        addItemLine();
        saveAndVerify(DRAFT);
    }

    @Test(priority = 7, groups = {PR, "edit", "status"},
            description = "Approved requisition: only the description can be edited; it stays Approved")
    public void approvedPurchaseRequisitionAllowsDescriptionEditOnly() {

        openRandomRequisition(r -> RecordStatus.APPROVED.of(r) && PurchaseRequisitionPage.isUnlinked(r));
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        form.waitUntilFieldEditable("note");

        List<String> editable = form.editableFieldsAmong(PurchaseRequisitionPage.SELECT_FIELDS);
        assertTrue(editable.isEmpty(),
                "Only the description may change on an Approved requisition, but these are editable: " + editable);

        saveAndVerify(APPROVED);
    }

    @Test(priority = 8, groups = {PR, "edit"},
            description = "Edit a Draft requisition, then Undo restores the saved values")
    public void userCanUndoPurchaseRequisitionChanges() {

        openDraftRequisition();
        String code = form.waitForCode();
        Map<String, String> saved = savedHeaderSnapshot();
        int savedLines = page.lineCount();
        log.info("Saved values of {}: {}, {} line(s).", code, saved, savedLines);

        // Same changes as the edit test: every header field + one new line.
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        changeHeader();
        addItemLine();
        log.info("Changed values before undo: {}, {} line(s).", headerSnapshot(), page.lineCount());

        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        page.clickUndo();
        page.confirmDialog();

        // Check first that undo stayed on the same requisition (on orders it
        // once opened another record, QC 2026-10-31).
        assertEquals(waitUntilEquals(form::getCode, code), code, "Undo opened another requisition.");

        SoftAssert softly = new SoftAssert();
        saved.forEach((field, value) -> softly.assertEquals(
                waitUntilEquals(() -> headerValue(field), value), value,
                "Undo did not restore \"" + field + "\" of " + code + "."));
        softly.assertEquals(waitUntilEquals(() -> String.valueOf(page.lineCount()), String.valueOf(savedLines)),
                String.valueOf(savedLines),
                "Undo did not remove the added line of " + code + ".");
        softly.assertAll();
    }

    /**
     * Header of the opened (saved) requisition, read once the form has
     * finished loading. The warehouse is filled a moment after the other
     * fields (QC 2026-11-01: read as "" right after opening, then
     * "رئيسي تكلفة الرياض"), so it waits until the warehouse, a required field,
     * has its value and two reads in a row are the same (max 10 s).
     */
    private Map<String, String> savedHeaderSnapshot() {
        Map<String, String> previous = headerSnapshot();
        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            pause(500);
            Map<String, String> now = headerSnapshot();
            if (now.equals(previous) && !now.get("warehouseId").isEmpty()) {
                return now;
            }
            previous = now;
        }
        log.warn("Header kept changing or the warehouse stayed empty for 10 s: {}", previous);
        return previous;
    }

    /** Every header field the test changes, with its value now on the screen. */
    private Map<String, String> headerSnapshot() {
        Map<String, String> values = new java.util.LinkedHashMap<>();
        for (String field : PurchaseRequisitionPage.SELECT_FIELDS) {
            values.put(field, headerValue(field));
        }
        for (String field : PurchaseRequisitionPage.DATE_FIELDS) {
            values.put(field, headerValue(field));
        }
        values.put("price", headerValue("price"));
        values.put("note", headerValue("note"));
        return values;
    }

    private String headerValue(String field) {
        if (field.equals("price")) {
            return page.priceOption();
        }
        if (field.equals("note")) {
            return form.getNote();
        }
        if (PurchaseRequisitionPage.DATE_FIELDS.contains(field)) {
            return page.inputValue(field);
        }
        return form.getSelectedText(field);
    }

    @Test(priority = 9, groups = {PR, "edit"},
            description = "Header values of an edited Draft are the same after save and reload")
    public void editedDraftHeaderIsKeptAfterSave() {

        openDraftRequisition();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        fillZeroPricesOfLines();
        Map<String, String> beforeSave = page.readHeaderValues(TestData.PERSISTED_HEADER_FIELDS);

        String code = saveAndVerify(DRAFT);
        page.reload();

        // After a reload some fields (the warehouse) are filled a moment
        // after the form appears, so each one is re-read until it matches
        // (max 10 s) before it is judged.
        SoftAssert softly = new SoftAssert();
        for (String field : TestData.PERSISTED_HEADER_FIELDS) {
            String expected = beforeSave.get(field);
            String actual = waitUntilEquals(
                    () -> page.readHeaderValues(List.of(field)).get(field), expected);
            softly.assertEquals(actual, expected, "Field " + field + " of " + code + " changed after save + reload.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Status actions: delete, approve, disapprove, cancel
    // =====================================================================

    @Test(priority = 10, groups = {PR, "status"},
            description = "Delete a Draft requisition: success message, and it is no longer in the list")
    public void userCanDeleteDraftPurchaseRequisition() {

        openDraftRequisition();
        String code = form.waitForCode();
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft " + code + ".");

        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);

        page.closeForm();
        page.waitForListLoaded();
        page.searchList(code);
        assertFalse(page.waitUntilListShows(code),
                code + " is still listed after deletion: " + page.visibleListCodes());
    }

    @Test(priority = 11, groups = {PR, "status", "log"},
            description = "Approve a Draft requisition: status Approved after reload, approval date today, "
                    + "delete disabled, one approval log entry")
    public void userCanApproveDraftPurchaseRequisition() {

        openDraftRequisition();
        String code = form.waitForCode();
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE,
                this::toggleApprovalOfOpenedRequisition);
        assertAccepted("Approving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
        form.waitForStatusLabel(APPROVED.label());

        page.reload();
        form.waitForStatusLabel(APPROVED.label()); // kept on the server
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ الاعتماد\" of " + code + " is not today.");
        softly.assertFalse(page.isActionEnabled("delete"), "An approved requisition (" + code + ") must not be deletable.");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Approving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (!logAfter.isEmpty()) {
            softly.assertEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The last log entry of " + code + " is not the approval.");
        }
        softly.assertAll();
    }

    @Test(priority = 12, groups = {PR, "status", "log"},
            description = "Disapprove an Approved requisition (not linked): back to Draft, draft actions enabled, "
                    + "one new log entry")
    public void userCanDisapproveApprovedPurchaseRequisition() {

        // QC 2026-11-01: the API flags isInvoice / totalQutotaionCount do not
        // cover every link. PR2610B45-000003 had neither, yet the server
        // refused: "لايمكن الغاء اعتماد طلب شراء مرتبط بعمليات اخرى". Such a
        // requisition must stay Approved; another one is tried.
        String code = null;
        List<String> logBefore = null;
        java.util.Set<String> tried = new java.util.HashSet<>();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS && code == null; attempt++) {
            Map<String, Object> record = openRandomRequisition(r -> RecordStatus.APPROVED.of(r)
                    && PurchaseRequisitionPage.isUnlinked(r) && !tried.contains(String.valueOf(r.get("code"))));
            form.waitForStatusLabel(APPROVED.label());
            String candidate = form.waitForCode();
            tried.add(candidate);
            List<String> logNow = page.readTransactionLog();

            ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE,
                    this::toggleApprovalOfOpenedRequisition);
            if (outcome.mentions(TestData.LINKED_ERROR)) {
                skipLinked(candidate, record, attempt);
                continue;
            }
            assertAccepted("Disapproving " + candidate, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
            code = candidate;
            logBefore = logNow;
        }
        if (code == null) {
            throw new SkipException("Every approved requisition tried is linked to other operations: " + tried);
        }
        form.waitForStatusLabel(DRAFT.label());

        page.reload();
        form.waitForStatusLabel(DRAFT.label()); // kept on the server
        page.waitForActionEnabled("edit");
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(form.isActionEnabled(action),
                    "'" + action + "' must be enabled on disapproved requisition " + code + ".");
        }
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Disapproving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (logAfter.size() > logBefore.size()) {
            softly.assertNotEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The new log entry of " + code + " is an approval, not a disapproval.");
        }
        softly.assertAll();
    }

    @Test(priority = 13, groups = {PR, "cancel"},
            description = "Cancel a Draft requisition: status Cancelled after reload, edit/approve/cancel disabled")
    public void userCanCancelDraftPurchaseRequisition() {

        openDraftRequisition();
        cancelOpenedRequisitionAndVerify();
    }

    @Test(priority = 14, groups = {PR, "cancel"},
            description = "Cancel an Approved requisition (not linked): status Cancelled after reload, "
                    + "edit/approve/cancel disabled")
    public void userCanCancelApprovedPurchaseRequisition() {

        java.util.Set<String> tried = new java.util.HashSet<>();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS; attempt++) {
            Map<String, Object> record = openRandomRequisition(r -> RecordStatus.APPROVED.of(r)
                    && PurchaseRequisitionPage.isUnlinked(r) && !tried.contains(String.valueOf(r.get("code"))));
            form.waitForStatusLabel(APPROVED.label());
            String code = form.waitForCode();
            tried.add(code);

            ActionOutcome outcome = cancelOpenedRequisition();
            if (outcome.mentions(TestData.LINKED_ERROR)) {
                skipLinked(code, record, attempt);
                continue;
            }
            assertAccepted("Cancelling " + code, TestData.API_CANCEL, outcome);
            verifyCancelled(code);
            return;
        }
        throw new SkipException("Every approved requisition tried is linked to other operations: " + tried);
    }

    @Test(priority = 15, groups = {PR, "cancel", "validation"},
            description = "Approved requisition linked to a quotation or an invoice: cancel and disapprove are disabled")
    public void linkedApprovedPurchaseRequisitionCannotBeCancelledOrDisapproved() {

        // Nothing is clicked here: if the system accepted it, a linked
        // requisition would really be cancelled. The buttons must be disabled
        // (QC 2026-11-01: "إلغاء" was enabled on PR2608-000018, linked to an invoice).
        Map<String, Object> record = openRandomRequisition(
                r -> RecordStatus.APPROVED.of(r) && !PurchaseRequisitionPage.isUnlinked(r));
        form.waitForStatusLabel(APPROVED.label());
        String code = form.waitForCode();
        page.waitForActionEnabled("print"); // toolbar has settled

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("cancel", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' is enabled on approved requisition "
                    + code + ", which is linked (invoice: " + record.get("isInvoice") + ", quotations: "
                    + record.get("totalQutotaionCount") + ").");
        }
        softly.assertAll();
    }

    // =====================================================================
    // List: search, pagination, Kanban, group by, summaries
    // =====================================================================

    @Test(priority = 16, groups = {PR, "search"},
            description = "List search by number shows that requisition")
    public void userCanSearchPurchaseRequisitionsListByCode() {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        page.closeForm();
        page.waitForListLoaded();

        page.searchList(code);
        assertTrue(page.waitUntilListShows(code),
                "Searching for " + code + " did not show it. Rows shown: " + page.visibleListCodes());
    }

    @Test(priority = 17, groups = {PR, "search"},
            description = "Record search (magnifier) in the form opens the requisition typed")
    public void userCanOpenPurchaseRequisitionFromRecordSearch() {

        openRandomRequisition(r -> true);
        String target = form.waitForCode();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
        assertNotEquals(form.waitForCode(), target, "Could not move away from " + target + ".");

        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(form::getCode, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 18, groups = {PR, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenPurchaseRequisitionRecords() {

        openRandomRequisition(r -> true);
        SoftAssert softly = new SoftAssert();

        if (page.isRecordNavigationEnabled("pageFirst")) {
            page.clickRecordNavigation("pageFirst");
        }
        String first = form.waitForCode();
        for (String button : List.of("pageFirst", "pagePrevious10", "pageBack")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the first record (" + first + ").");
        }

        page.clickRecordNavigation("pageNext");
        String second = form.waitForCode();
        softly.assertNotEquals(second, first, "'Next' did not open another requisition.");

        page.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(form::getCode, first), first, "'Back' did not return to the first one.");

        page.clickRecordNavigation("pageLast");
        String last = form.waitForCode();
        for (String button : List.of("pageLast", "pageNext10", "pageNext")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the last record (" + last + ").");
        }
        log.info("Record navigation: first={}, second={}, last={}", first, second, last);
        softly.assertAll();
    }

    @Test(priority = 19, groups = {PR, "pagination"},
            description = "List pager: first/last page, summary, rows per page, then a random Draft across pages")
    public void userCanNavigatePurchaseRequisitionsPagination() {

        SoftAssert softly = new SoftAssert();

        page.goToFirstListPage();
        int[] first = page.paginationRange(); // {from, to, total}
        int total = first[2];
        softly.assertEquals(first[0], 1, "The first page must start at record 1.");
        softly.assertFalse(page.isListPreviousPageEnabled(), "'Previous' must be disabled on the first page.");
        softly.assertEquals(page.visibleRowCount(), first[1] - first[0] + 1,
                "Rows on the first page do not match the summary " + Arrays.toString(first) + ".");

        page.goToLastListPage();
        int[] last = page.paginationRange();
        softly.assertEquals(last[1], total, "The last page must end at the last record.");
        softly.assertEquals(last[2], total, "The total must not change between pages.");
        softly.assertFalse(page.isListNextPageEnabled(), "'Next' must be disabled on the last page.");
        softly.assertEquals(page.visibleRowCount(), last[1] - last[0] + 1,
                "Rows on the last page do not match the summary " + Arrays.toString(last) + ".");

        page.goToFirstListPage();
        softly.assertEquals(page.paginationRange()[0], 1, "'first' must return to record 1.");
        log.info("Pager: first {} | last {}", Arrays.toString(first), Arrays.toString(last));

        // Page by page with "Next" up to the last page: every page follows the
        // previous one, has as many rows as its summary, and the total stays.
        int pageSize = first[1] - first[0] + 1;
        int pages = 1;
        while (page.isListNextPageEnabled()) {
            int[] before = page.paginationRange();
            page.goToNextPage();
            int[] now = page.paginationRange();
            pages++;
            softly.assertEquals(now[0], before[1] + 1, "Page " + pages + " does not follow the previous one: "
                    + Arrays.toString(before) + " → " + Arrays.toString(now));
            softly.assertEquals(now[2], total, "The total changed on page " + pages + ".");
            softly.assertEquals(page.visibleRowCount(), now[1] - now[0] + 1,
                    "Rows on page " + pages + " do not match the summary " + Arrays.toString(now) + ".");
        }
        softly.assertEquals(page.paginationRange()[1], total, "\"Next\" did not reach the last record.");
        softly.assertEquals(pages, (total + pageSize - 1) / pageSize, "Unexpected number of pages.");

        // Back with "Previous" to a random page, then a random row of it opens
        // with the status shown in the list.
        int target = 1 + new java.util.Random().nextInt(pages);
        for (int current = pages; current > target; current--) {
            int[] before = page.paginationRange();
            page.goToPreviousPage();
            int[] now = page.paginationRange();
            softly.assertEquals(now[1], before[0] - 1, "\"Previous\" did not go back exactly one page: "
                    + Arrays.toString(before) + " → " + Arrays.toString(now));
        }
        int[] chosen = page.paginationRange();
        softly.assertEquals(chosen[0], (target - 1) * pageSize + 1, "Not on page " + target + " after going back.");
        softly.assertAll();

        Map<String, String> rows = page.statusesOnCurrentPage();
        List<String> codes = new ArrayList<>(rows.keySet());
        String code = codes.get(new java.util.Random().nextInt(codes.size()));
        log.info("{} page(s); back on page {} {} → opening {} [{}].", pages, target, Arrays.toString(chosen),
                code, rows.get(code));
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(form::getCode, code), code, "The list opened another requisition than " + code + ".");
        form.waitForStatusLabel(rows.get(code));
    }

    @Test(priority = 20, groups = {PR, "list"},
            description = "Kanban shows the same requisitions as the list page, each with a status; a card opens it")
    public void userCanSwitchPurchaseRequisitionsListToKanbanAndBack() {

        page.waitForListLoaded();
        List<String> gridCodes = page.visibleListCodes();
        try {
            page.switchToKanbanView();
            List<String> cardCodes = page.kanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);

            SoftAssert softly = new SoftAssert();
            softly.assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes),
                    "Kanban cards do not show the same requisitions as the list page.");
            List<String> labels = new ArrayList<>(
                    Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList());
            labels.add(TestData.DISAPPROVED_CARD_LABEL);
            for (String code : cardCodes) {
                String status = page.kanbanCardStatus(code);
                softly.assertTrue(labels.contains(status), "Card " + code + " has an unknown status [" + status + "].");
            }
            softly.assertAll();

            String target = cardCodes.get(new java.util.Random().nextInt(cardCodes.size()));
            page.openKanbanCard(target);
            assertEquals(waitUntilEquals(form::getCode, target), target,
                    "Double-clicking the card of " + target + " opened another requisition.");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView(); // leave the list as the other tests expect it
            }
        }
    }

    @Test(priority = 21, groups = {PR, "list"},
            description = "Group by status: one group per known status; the groups add up to the list total")
    public void userCanGroupPurchaseRequisitionsListByStatus() {

        page.waitForListLoaded();
        int total = page.paginationRange()[2];
        page.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = page.groupCounts();
        int sum = groups.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Groups by status: {} (sum {}), list total {}", groups, sum, total);

        List<String> labels = Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList();
        SoftAssert softly = new SoftAssert();
        for (Map.Entry<String, Integer> group : groups.entrySet()) {
            softly.assertTrue(labels.contains(group.getKey()), "Unknown status group [" + group.getKey() + "].");
            softly.assertTrue(group.getValue() > 0, "Group [" + group.getKey() + "] is empty.");
        }
        softly.assertEquals(sum, total, "Status groups add up to " + sum + " but the list total is " + total + ".");
        softly.assertAll();
    }

    @Test(priority = 22, groups = {PR, "list"},
            description = "Summaries (الملخصات): each status card equals the number of requisitions in that status")
    public void purchaseRequisitionSummariesMatchStatusGroups() {

        page.waitForListLoaded();
        Map<String, Integer> summaries = page.summaryCards();
        page.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = page.groupCounts();
        log.info("Summaries: {} | groups by status: {}", summaries, groups);

        SoftAssert softly = new SoftAssert();
        for (DocumentStatus status : DocumentStatus.values()) {
            softly.assertEquals(summaries.getOrDefault(status.label(), 0), groups.getOrDefault(status.label(), 0),
                    "Summary card [" + status.label() + "] does not match the requisitions in that status.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Print, PDF, Excel, e-mail to the supplier
    // =====================================================================

    @Test(priority = 23, groups = {PR, "print"},
            description = "Print a random requisition: the preview shows its number, supplier and warehouse; "
                    + "then it is printed through Chrome's print dialog")
    public void userCanPrintPurchaseRequisition() throws java.awt.AWTException {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        String supplier = form.getSelectedText("vendorId");
        String warehouse = form.getSelectedText("warehouseId");

        String preview = page.openPrintPreviewWithRandomTemplate();

        // The preview shows the supplier NAME only ("PG0040236 - محمود محمد" → "محمود محمد").
        String supplierName = supplier.contains(" - ") ? supplier.substring(supplier.indexOf(" - ") + 3) : supplier;
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(preview.contains(code), "Print preview does not show " + code + ".");
        if (!supplierName.isBlank()) {
            softly.assertTrue(preview.contains(supplierName),
                    "Print preview of " + code + " does not show the supplier [" + supplierName + "].");
        }
        if (!warehouse.isBlank()) {
            softly.assertTrue(preview.contains(warehouse),
                    "Print preview of " + code + " does not show the warehouse [" + warehouse + "].");
        }
        softly.assertAll();

        // "طباعة" in the preview → Chrome's print dialog → Print (Enter), as on orders.
        page.printFromPreview();
        page.confirmChromePrintDialog();

        // The app must still respond after printing: the preview closes and
        // the same requisition is shown.
        page.closePrintPreview();
        assertEquals(form.waitForCode(), code, "After printing, the form does not show " + code + " any more.");
        log.info("{} printed.", code);
    }

    @Test(priority = 24, groups = {PR, "print"},
            description = "Save a random requisition as PDF: the preview shows it; a PDF file is downloaded")
    public void userCanSavePurchaseRequisitionAsPdf() {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        Path dir = page.prepareDownloadFolder("pdf");

        String preview = page.openPrintPreviewWithRandomTemplate();
        assertTrue(preview.contains(code), "The print preview does not show " + code + ".");

        page.savePreviewAsPdf();
        Path pdf = page.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
        log.info("PDF of {} saved: {} ({} bytes)", code, pdf.getFileName(), Downloads.sizeOf(pdf));
    }

    @Test(priority = 25, groups = {PR, "excel"},
            description = "⋮ → Export to Excel: an .xlsx with the requisition's item lines is downloaded")
    public void userCanExportPurchaseRequisitionToExcel() {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        page.waitForGridToSettle();
        int lines = page.lineCount();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < lines; row++) {
            items.add(page.lineText(row, "code"));
        }
        Path dir = page.prepareDownloadFolder("excel");

        page.clickMenuItem("التصدير الى Excel");

        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        int rows = Downloads.xlsxRowCount(xlsx);
        log.info("Excel of {}: {} ({} bytes), {} row(s); {} line(s): {}",
                code, xlsx.getFileName(), Downloads.sizeOf(xlsx), rows, lines, items);

        // On orders the file has a title row and a header row, then one row per line.
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(rows - 2, lines, "Excel rows do not match the item lines of " + code + ".");
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }

    @Test(priority = 26, groups = {PR, "email"},
            description = "Edit a Draft, choose a new supplier (branch and warehouse filled), save, then ⋮ → "
                    + "send it to the supplier by e-mail with an extra note line and PDF attachments")
    public void userCanSendPurchaseRequisitionToSupplierByEmail() {

        // 1) Edit a Draft and choose another supplier; branch and warehouse must be filled.
        openDraftRequisition();
        String code = form.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        String supplier = selectNewSupplierWithBranchAndWarehouse();
        fillZeroPricesOfLines();
        saveAndVerify(DRAFT); // the e-mail is sent for the saved requisition
        assertEquals(waitUntilEquals(() -> form.getSelectedText("vendorId"), supplier), supplier,
                "The new supplier of " + code + " was not saved.");

        // 2) ⋮ → "ارسال للمورد عن طريق الايميل": the dialog is pre-filled.
        PurchaseRequisitionPage.SendToSupplierDialog dialog = page.openSendToSupplierDialog();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(dialog.title(), TestData.SEND_DIALOG_TITLE, "Unexpected send dialog title.");
        softly.assertTrue(dialog.recipients().contains(supplier),
                "\"إلى\" does not have the new supplier of " + code + " [" + supplier + "]: " + dialog.recipients());
        softly.assertTrue(dialog.notes().contains(code), "\"البيان\" does not mention " + code + ": " + dialog.notes());
        softly.assertTrue(dialog.fileTypes().contains(".pdf"), "The attachment field does not take PDF files.");

        // 3) One more line in "البيان", PDF attachments, then "ارسال".
        String extraLine = TestData.EMAIL_NOTE_LINE + " " + code;
        String note = page.appendSendNote(extraLine);
        softly.assertTrue(note.contains(code) && note.endsWith(extraLine),
                "The extra line was not added at the end of \"البيان\": " + note);
        List<Path> pdfs = page.pdfAttachments();
        page.attachToSendDialog(pdfs);
        List<String> listed = page.sendDialogAttachments(pdfs);
        log.info("Attachments listed before sending: {}", listed);
        softly.assertEquals(listed.size(), pdfs.size(),
                "Not every PDF stays listed under \"المرفقات\" (a new file replaces the previous one?): " + listed);

        List<String> calls = page.sendToSupplier();
        softly.assertFalse(page.isSendDialogOpen(), "The send dialog stayed open after \"ارسال\". API: " + calls);
        DocumentForm.Toast toast;
        try {
            toast = form.waitForToast();
        } catch (org.openqa.selenium.TimeoutException none) {
            toast = null;
        }
        log.info("E-mail of {} to {} → toast {}", code, supplier, toast);
        // The toast may already be gone once the dialog has closed, so the API
        // answer is the evidence; a toast that is still there must not be an error.
        softly.assertTrue(toast == null || !toast.isError(),
                "Sending " + code + " to " + supplier + " showed an error: " + toast + " | API: " + calls);
        softly.assertTrue(calls.stream().anyMatch(c -> c.matches("(?s).* -> 2\\d\\d .*")),
                "No successful API call was made by \"ارسال\": " + calls);
        softly.assertAll();
    }

    // =====================================================================
    // Follow-up screens: Items Pricing ($), Print Barcode
    // =====================================================================

    @Test(priority = 27, groups = {PR, "follow-up"},
            description = "Items Pricing ($): Sales → Pricing opens with the requisition's items")
    public void userCanOpenItemsPricingFromPurchaseRequisition() {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        if (!page.waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing ($) is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("code");

        page.clickItemsPricing();

        // QC 2026-11-01: /sales/pricing/add?itemIds=440,442,443 lists the items (col-id itemCode).
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> priced = page.gridColumnTexts("itemCode");
        log.info("Items Pricing from {}: {} → {}", code, items, priced);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("itemIds="), "Pricing screen opened without the items: " + url);
        softly.assertEquals(new HashSet<>(priced), new HashSet<>(items),
                "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 28, groups = {PR, "follow-up", "print"},
            description = "Print Barcode: Item Barcodes opens with the requisition's items")
    public void userCanOpenPrintBarcodeFromPurchaseRequisition() {

        openRandomRequisition(r -> true);
        String code = form.waitForCode();
        if (!page.waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("itemId");

        page.clickPrintBarcode();

        // QC 2026-11-01: /inventory/item-barcodes?referenceCode=<code>.
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> barcodeItems = page.gridColumnTexts("itemId");
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("referenceCode=" + code), "Item Barcodes is not loaded from " + code + ": " + url);
        softly.assertEquals(barcodeItems, items, "Item Barcodes does not list the items of " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Attachments
    // =====================================================================

    @Test(priority = 29, groups = {PR, "attachments"},
            description = "Attachments added to a Draft are listed and kept after save and reload")
    public void userCanAddAttachmentsToPurchaseRequisition() {

        openDraftRequisition();
        String code = form.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        page.openAttachments();
        int before = page.attachmentCount();
        int files = page.countAttachmentFiles();
        log.info("{} has {} attachment(s); adding {} file(s).", code, before, files);

        page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
        assertEquals(page.waitForAttachmentCount(before + files), before + files,
                "The added files are not listed in 'تفاصيل المرفق'.");
        page.closeAttachments();

        fillZeroPricesOfLines();
        saveAndVerify(DRAFT);
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before + files), before + files,
                "Attachments of " + code + " were not kept after save and reload.");
    }

    @Test(priority = 30, groups = {PR, "attachments"},
            description = "Deleting an attachment of a Draft is kept after save and reload")
    public void userCanDeleteAttachmentFromPurchaseRequisition() {

        openDraftRequisition();
        String code = form.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        int before = page.attachmentCount();

        // A Draft without attachments gets some first (saved), so there is something to delete.
        if (before == 0) {
            int files = page.countAttachmentFiles();
            page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
            before = page.waitForAttachmentCount(files);
            page.closeAttachments();
            fillZeroPricesOfLines();
            saveAndVerify(DRAFT);
            page.clickEdit();
            form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
            page.openAttachments();
        }
        log.info("{} has {} attachment(s); deleting the last one.", code, before);

        page.deleteLastAttachment();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1,
                "The deleted attachment is still listed in 'تفاصيل المرفق'.");
        page.closeAttachments();
        page.waitForToastToAppearAndClear(); // the delete toast must not be read as the save toast

        fillZeroPricesOfLines();
        saveAndVerify(DRAFT);
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1,
                "The deleted attachment of " + code + " came back after save and reload.");
    }

    // =====================================================================
    // Exit, validation, toolbar per status, transaction log
    // =====================================================================

    @Test(priority = 31, groups = {PR, "exit"},
            description = "Close a new, a viewed and an edited requisition: each returns to the list")
    public void userCanExitPurchaseRequisition() {

        // New, unsaved → asks for confirmation.
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing a new requisition did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing a new requisition");

        // Viewed → closes directly.
        openRandomRequisition(r -> true);
        String viewed = form.waitForCode();
        page.clickAction("xIcon");
        assertBackOnList("closing " + viewed + " in view mode");

        // Edited Draft → asks for confirmation.
        openDraftRequisition();
        String edited = form.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing " + edited + " in edit mode did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing " + edited + " in edit mode");
    }

    @Test(priority = 32, groups = {PR, "validation"},
            description = "Saving an empty requisition is blocked: error toast, required fields marked, stays in Add")
    public void userCannotSavePurchaseRequisitionWithoutRequiredData() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("save");

        page.clickSave();
        DocumentForm.Toast toast = form.waitForToast(); // read first: it disappears after 2-3 s
        List<String> marked = form.fieldsWithRequiredError();
        log.info("Empty save → toast [{}], required errors on {}", toast, marked);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast.isError(), "Expected an error toast, got: " + toast);
        softly.assertTrue(marked.containsAll(TestData.REQUIRED_HEADER_FIELDS),
                "Required-field errors " + marked + " do not include " + TestData.REQUIRED_HEADER_FIELDS + ".");
        softly.assertFalse(form.isDialogOpen(), "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 33, groups = {PR, "validation"},
            description = "New requisition toolbar: save/close enabled; edit/delete/approve/cancel/copy disabled")
    public void newPurchaseRequisitionEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("save"); // the toolbar settles after the URL changes

        SoftAssert softly = new SoftAssert();
        // On a new requisition some of these buttons are not rendered at all
        // (QC 2026-11-01: no "requisitions-edit" in Add mode). A missing button
        // counts as not usable, so page.isActionEnabled (false when absent) is used.
        for (String action : TestData.ACTIONS_DISABLED_ON_NEW) {
            log.info("New requisition: '{}' {}", action,
                    page.isActionEnabled(action) ? "ENABLED" : (page.hasAction(action) ? "disabled" : "not shown"));
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new requisition.");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_NEW) {
            softly.assertTrue(form.isActionEnabled(action), "'" + action + "' must be enabled on a new requisition.");
        }
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 34, groups = {PR, "validation", "status"},
            description = "Draft requisition: edit/delete/approve/cancel enabled, no approval date")
    public void draftPurchaseRequisitionEnablesEditActions() {

        openDraftRequisition();
        String code = form.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(form.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        softly.assertEquals(page.inputValue("approvalDate"), "", "Draft " + code + " must not have an approval date.");
        softly.assertAll();
    }

    @Test(priority = 35, groups = {PR, "log"},
            description = "Transaction log: first entry is the creation; an Approved requisition has an approval entry")
    public void userCanViewPurchaseRequisitionTransactionLog() {

        Map<String, Object> record = openRandomRequisition(r -> true);
        String code = form.waitForCode();
        List<String> actions = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log of " + code + " is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), TestData.LOG_CREATED,
                "The first log entry of " + code + " must be the creation.");
        if (RecordStatus.APPROVED.of(record)) {
            softly.assertTrue(actions.contains(TestData.LOG_APPROVED),
                    "Approved " + code + " has no approval entry in its log: " + actions);
        }
        softly.assertAll();
    }

    // =====================================================================
    // Lines
    // =====================================================================

    @Test(priority = 36, groups = {PR, "lines"},
            description = "A new requisition keeps its lines (item, quantity, price) after save and reload")
    public void savedPurchaseRequisitionKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        page.deleteEmptyLines();
        List<String> before = linesSnapshot();
        log.info("Lines before save: {}", before);

        saveAndVerify(TestData.NEW_REQUISITION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        List<String> after = linesSnapshot();

        // Lines may come back in another order, so they are compared sorted.
        assertEquals(after.stream().sorted().toList(), before.stream().sorted().toList(),
                "Lines changed after save and reload.");
    }

    @Test(priority = 37, groups = {PR, "lines", "edit"},
            description = "Draft requisition: changing the quantity of an existing line is saved")
    public void userCanEditQuantityOfExistingLine() {

        openDraftRequisition();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.waitForGridToSettle();

        String item = page.lineText(0, "code");
        BigDecimal quantity = page.lineNumber(0, "quantity").add(BigDecimal.ONE);
        page.setLineNumber(0, "quantity", quantity.toPlainString());
        page.stopCellEditing();
        log.info("Line 1 ({}) quantity → {}", item, quantity);
        fillZeroPricesOfLines();

        saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();

        int row = page.lineTexts("code").indexOf(item);
        assertTrue(row >= 0, "Item " + item + " is not in the lines after save.");
        assertEquals(page.lineNumber(row, "quantity").compareTo(quantity), 0,
                "Quantity of " + item + " was not saved as " + quantity + " (found " + page.lineNumber(row, "quantity") + ").");
    }

    @Test(priority = 38, groups = {PR, "lines"},
            description = "New requisition: deleting an item line is saved")
    public void userCanDeleteItemLine() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();          // downloads lines from another document
        addItemLine();         // plus one, so a line remains after the delete
        page.deleteEmptyLines();
        int before = page.lineCount();
        String deleted = page.lineText(0, "code");
        log.info("{} line(s); deleting line 1 ({}).", before, deleted);

        page.deleteLine(0);
        assertEquals(page.lineCount(), before - 1, "Deleting a line did not remove it from the grid.");

        saveAndVerify(TestData.NEW_REQUISITION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        assertEquals(page.lineCount(), before - 1, "The deleted line came back after save and reload.");
    }

    // =====================================================================
    // Follow-up documents from an Approved requisition (⋮ menu)
    // Nothing is saved: the new documents are only opened and checked.
    // =====================================================================

    @Test(priority = 39, groups = {PR, "follow-up"},
            description = "Approved requisition → ⋮ → Create Purchase Order: a new order pre-filled from it (not saved)")
    public void userCanCreatePurchaseOrderFromApprovedRequisition() {

        Source source = openApprovedSource(r -> true);

        page.clickMenuItemAndFollow(TestData.CREATE_ORDER_MENU_ITEM, TestData.CREATE_ORDER_PATH);

        // QC 2026-11-01: /purchase/orders/add/<id>?origin=purchaseRequisition
        verifyFollowUpDocument(source, new DocumentForm(driver, TestData.ORDER_PREFIX), "purchase order");
    }

    @Test(priority = 40, groups = {PR, "follow-up"},
            description = "Approved requisition → ⋮ → Create Price Quotation: a new quotation pre-filled from it (not saved)")
    public void userCanCreatePriceQuotationFromApprovedRequisition() {

        Source source = openApprovedSource(r -> true);

        page.clickMenuItemAndFollow(TestData.CREATE_QUOTATION_MENU_ITEM, TestData.CREATE_QUOTATION_PATH);

        // QC 2026-11-01: /purchase/price-offers/add/<id>
        verifyFollowUpDocument(source, new DocumentForm(driver, TestData.QUOTATION_PREFIX), "price quotation");
    }

    @Test(priority = 41, groups = {PR, "follow-up"},
            description = "Approved requisition with quotations → Compare quotations → Manual: the quoted items are listed")
    public void userCanOpenManualQuotationComparison() {

        // QC 2026-11-01: the comparison can open EMPTY ("المحتوى فارغ الان") for
        // a requisition that has quotations by the API and offers the menu item
        // (PR2608-000011). Such a requisition is logged and another one is tried.
        java.util.Set<String> tried = new java.util.LinkedHashSet<>();
        Source source = null;
        List<String> quoted = List.of();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS && quoted.isEmpty(); attempt++) {
            source = openApprovedSourceWithMenuItem(
                    r -> DocumentApi.intOf(r, "totalQutotaionCount") > 0, TestData.COMPARE_MENU_ITEM, tried);
            page.clickSubMenuItemAndFollow(TestData.COMPARE_MENU_ITEM, TestData.MANUAL_COMPARE_ITEM, TestData.MANUAL_COMPARE_PATH);
            quoted = page.gridColumnTextsOrEmpty("itemCode", Duration.ofSeconds(15)).stream()
                    .filter(t -> !t.isBlank()).toList(); // blank = supplier group rows
            if (quoted.isEmpty()) {
                log.warn("Comparison of {} (API quotations: {}) is empty → another requisition ({}/{}).",
                        source.code(), source.record().get("totalQutotaionCount"), attempt, TestData.MAX_LINKED_ATTEMPTS);
            }
        }
        if (quoted.isEmpty()) {
            throw new SkipException("Every requisition tried opens an empty comparison: " + tried);
        }
        final Source chosen = source;
        log.info("Manual comparison of {}: quoted items {} | requisition items {}", chosen.code(), quoted, chosen.items());
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains(TestData.MANUAL_COMPARE_PATH + chosen.id()),
                "Manual comparison is not for " + chosen.code() + ": " + driver.getCurrentUrl());
        softly.assertFalse(quoted.isEmpty(), "Manual comparison of " + chosen.code() + " lists no quoted line.");
        // A quotation may hold items the requisition does not have (added on the
        // quotation; QC 2026-11-01: PR2609B18-000002 has 116, its quotations 116 and 140).
        // So the check is that the requisition's items are offered, not the reverse.
        softly.assertTrue(quoted.stream().anyMatch(chosen.items()::contains),
                "Manual comparison of " + chosen.code() + " offers none of its items " + chosen.items() + ": " + quoted);
        List<String> extra = quoted.stream().filter(i -> !chosen.items().contains(i)).distinct().toList();
        if (!extra.isEmpty()) {
            log.info("Quotations of {} also have items that are not in the requisition: {}", chosen.code(), extra);
        }
        softly.assertAll();
        // "اعتماد" is not clicked: it would approve quotations in QC.
    }

    @Test(priority = 42, groups = {PR, "follow-up"},
            description = "Approved requisition with quotations → Compare quotations → Automatic: its quotations are listed")
    public void userCanOpenAutomaticQuotationComparison() {

        // QC 2026-11-01: the comparison can open EMPTY ("المحتوى فارغ الان") for
        // a requisition that has quotations by the API and offers the menu item
        // (PR2608-000011). Such a requisition is logged and another one is tried.
        java.util.Set<String> tried = new java.util.LinkedHashSet<>();
        Source source = null;
        List<String> quotations = List.of();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS && quotations.isEmpty(); attempt++) {
            source = openApprovedSourceWithMenuItem(
                    r -> DocumentApi.intOf(r, "totalQutotaionCount") > 0, TestData.COMPARE_MENU_ITEM, tried);
            page.clickSubMenuItemAndFollow(TestData.COMPARE_MENU_ITEM, TestData.AUTO_COMPARE_ITEM, TestData.AUTO_COMPARE_PATH);
            quotations = page.gridColumnTextsOrEmpty("qutationCode", Duration.ofSeconds(15)).stream()
                    .filter(t -> !t.isBlank()).toList(); // blank = supplier group rows
            if (quotations.isEmpty()) {
                log.warn("Comparison of {} (API quotations: {}) is empty → another requisition ({}/{}).",
                        source.code(), source.record().get("totalQutotaionCount"), attempt, TestData.MAX_LINKED_ATTEMPTS);
            }
        }
        if (quotations.isEmpty()) {
            throw new SkipException("Every requisition tried opens an empty comparison: " + tried);
        }
        final Source chosen = source;
        List<String> references = page.gridColumnTexts("referenceDocument");
        log.info("Automatic comparison of {}: quotations {} (API count {})",
                chosen.code(), quotations, chosen.record().get("totalQutotaionCount"));
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains(TestData.AUTO_COMPARE_PATH + chosen.id()),
                "Automatic comparison is not for " + chosen.code() + ": " + driver.getCurrentUrl());
        softly.assertFalse(quotations.isEmpty(), "Automatic comparison of " + chosen.code() + " lists no quotation.");
        for (String reference : references) {
            softly.assertEquals(reference, chosen.code(), "A listed quotation refers to another document.");
        }
        // QC 2026-11-01: PR2608-000013 has 3 quotations by the API but the
        // automatic comparison lists 1 (PQ2608-000010). The screen probably
        // shows only the quotations that can still be compared (e.g. not
        // cancelled / not converted) - the rule is asked to the analysis team.
        // Until then it must not list MORE than the API, and the gap is logged.
        int apiCount = DocumentApi.intOf(chosen.record(), "totalQutotaionCount");
        softly.assertTrue(quotations.size() <= apiCount, "Automatic comparison of " + chosen.code()
                + " lists more quotations (" + quotations.size() + ") than the API counts (" + apiCount + ").");
        if (quotations.size() < apiCount) {
            log.warn("Automatic comparison of {} lists {} of the {} quotations counted by the API: {}",
                    chosen.code(), quotations.size(), apiCount, quotations);
        }
        softly.assertAll();
        // "اعتماد" is not clicked: it would approve a quotation in QC.
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Edit mode: chooses a supplier different from the current one until the
     * branch and the warehouse are both filled (max {@link TestData#MAX_SUPPLIER_ATTEMPTS}).
     * An empty branch or warehouse that can be selected is chosen at random.
     *
     * @return the new supplier.
     */
    private String selectNewSupplierWithBranchAndWarehouse() {
        String old = form.getSelectedText("vendorId");
        for (int attempt = 1; attempt <= TestData.MAX_SUPPLIER_ATTEMPTS; attempt++) {
            String supplier = page.selectRandomOtherOption("vendorId");
            page.confirmDialogIfShown(Duration.ofSeconds(2));
            for (String field : List.of("branchId", "warehouseId")) {
                if (waitForSelected(form, field).isEmpty() && page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
                    page.selectRandomOption(field);
                    page.confirmDialogIfShown(Duration.ofMillis(800));
                }
            }
            String branch = form.getSelectedText("branchId");
            String warehouse = form.getSelectedText("warehouseId");
            log.info("Supplier [{}] → [{}]: branch [{}], warehouse [{}] ({}/{})",
                    old, supplier, branch, warehouse, attempt, TestData.MAX_SUPPLIER_ATTEMPTS);
            if (!supplier.isEmpty() && !supplier.equals(old) && !branch.isEmpty() && !warehouse.isEmpty()) {
                return supplier;
            }
        }
        fail("No supplier left both the branch and the warehouse filled after "
                + TestData.MAX_SUPPLIER_ATTEMPTS + " attempts.");
        return "";
    }

    /**
     * Like {@link #openApprovedSource} but the requisition's "⋮" menu must
     * offer {@code menuItem}. QC 2026-11-01: PR2608-000025 has quotations by
     * the API (totalQutotaionCount > 0) but its menu had no "مقارنة عروض
     * الاسعار" (PR2608-000024 had it). Such a requisition is logged and
     * another one is tried; the test is skipped when none offers the item.
     */
    private Source openApprovedSourceWithMenuItem(Predicate<Map<String, Object>> filter, String menuItem) {
        return openApprovedSourceWithMenuItem(filter, menuItem, new java.util.LinkedHashSet<>());
    }

    /** Same, skipping the requisitions in {@code tried} (shared by callers that retry). */
    private Source openApprovedSourceWithMenuItem(Predicate<Map<String, Object>> filter, String menuItem,
                                                  java.util.Set<String> tried) {
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS; attempt++) {
            Source source = openApprovedSource(r -> filter.test(r) && !tried.contains(String.valueOf(r.get("code"))));
            tried.add(source.code());
            List<String> items = page.menuItems();
            if (items.stream().anyMatch(i -> i.replaceAll("\\s+", "").equals(menuItem.replaceAll("\\s+", "")))) {
                return source;
            }
            log.warn("{} (API quotations: {}) has no [{}] in ⋮ → another one ({}/{}). Menu: {}",
                    source.code(), source.record().get("totalQutotaionCount"), menuItem,
                    attempt, TestData.MAX_LINKED_ATTEMPTS, items);
            page.closeForm();
            page.waitForListLoaded();
        }
        throw new SkipException("No approved requisition with quotations offers [" + menuItem + "]. Tried: " + tried);
    }

    /** The approved requisition a follow-up document is made from. */
    private record Source(Map<String, Object> record, String id, String code,
                          String branch, String warehouse, java.util.Set<String> items) {
    }

    /** Opens a random APPROVED requisition matching {@code filter} and reads what the follow-up must carry. */
    private Source openApprovedSource(Predicate<Map<String, Object>> filter) {
        Map<String, Object> record = openRandomRequisition(r -> RecordStatus.APPROVED.of(r) && filter.test(r));
        form.waitForStatusLabel(APPROVED.label());
        page.waitForGridToSettle();
        Source source = new Source(record, String.valueOf(record.get("id")), form.waitForCode(),
                waitForSelected(form, "branchId"), waitForSelected(form, "warehouseId"),
                new java.util.LinkedHashSet<>(page.gridColumnTexts("code")));
        log.info("Source: {} branch [{}] warehouse [{}] items {}", source.code(), source.branch(),
                source.warehouse(), source.items());
        return source;
    }

    /** The selected text of {@code field}, once loaded (the warehouse is filled late, max 10 s). */
    private String waitForSelected(DocumentForm on, String field) {
        long end = System.currentTimeMillis() + 10_000;
        String value = on.getSelectedText(field);
        while (value.isEmpty() && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            value = on.getSelectedText(field);
        }
        return value;
    }

    /**
     * The new (unsaved) document refers to {@code source} ("المستند المرجعي"),
     * has its branch and warehouse, and lists its items.
     */
    private void verifyFollowUpDocument(Source source, DocumentForm target, String what) {
        assertEquals(waitUntilEquals(() -> page.inputValueById("referenceDocument"), source.code()), source.code(),
                "The new " + what + " does not refer to " + source.code() + ".");
        page.waitForGridToSettle();
        java.util.Set<String> items = new java.util.LinkedHashSet<>(page.gridColumnTexts("code"));
        log.info("New {} from {}: url {}, items {}", what, source.code(), driver.getCurrentUrl(), items);

        SoftAssert softly = new SoftAssert();
        if (!source.branch().isEmpty()) {
            softly.assertEquals(waitForSelected(target, "branchId"), source.branch(),
                    "Branch of the new " + what + " differs from " + source.code() + ".");
        }
        if (!source.warehouse().isEmpty()) {
            softly.assertEquals(waitForSelected(target, "warehouseId"), source.warehouse(),
                    "Warehouse of the new " + what + " differs from " + source.code() + ".");
        }
        softly.assertEquals(items, source.items(), "Items of the new " + what + " differ from " + source.code() + ".");
        softly.assertAll();
        // Left unsaved on purpose: the test must not create documents in QC.
    }


    /** "code × quantity @ price" of every line, as numbers (so 17 and 17.000 are equal). */
    private List<String> linesSnapshot() {
        List<String> lines = new ArrayList<>();
        int count = page.lineCount();
        for (int row = 0; row < count; row++) {
            lines.add(page.lineText(row, "code") + " × " + page.lineNumber(row, "quantity").stripTrailingZeros().toPlainString()
                    + " @ " + page.lineNumber(row, "price").stripTrailingZeros().toPlainString());
        }
        return lines;
    }

    /** The form is closed and the list is shown again. */
    private void assertBackOnList(String afterWhat) {
        waitUntil("the list after " + afterWhat, Duration.ofSeconds(15), () -> !page.isFormOpen());
        page.waitForListLoaded();
    }


    /** "اعتماد" / "عدم اعتماد" of the opened requisition, then "موافق". */
    private void toggleApprovalOfOpenedRequisition() {
        assertTrue(page.waitForActionEnabled("approveOrDisapprove"),
                "\"اعتماد\" is disabled on " + form.getCode() + ".");
        page.clickApproveOrDisapprove();
        page.confirmDialog();
    }

    /** Cancels the opened requisition, then checks status, persistence and the disabled actions. */
    private void cancelOpenedRequisitionAndVerify() {

        String code = form.waitForCode();
        assertAccepted("Cancelling " + code, TestData.API_CANCEL, cancelOpenedRequisition());
        verifyCancelled(code);
    }

    /** "إلغاء" then "موافق"; returns the toast and the API answer. */
    private ActionOutcome cancelOpenedRequisition() {

        String code = form.waitForCode();
        assertTrue(page.waitForActionEnabled("cancel"), "\"إلغاء\" is disabled on " + code + ".");
        return performAndRecord(form, TestData.API_CANCEL, () -> {
            page.clickCancelDocument();
            page.confirmDialog();
        });
    }

    private void verifyCancelled(String code) {

        form.waitForStatusLabel(CANCELLED.label());
        page.reload();
        form.waitForStatusLabel(CANCELLED.label()); // kept on the server

        // "delete" is not checked: on orders it stays enabled on cancelled
        // documents and the business has not decided yet (QC 2026-10-26).
        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action),
                    "'" + action + "' must be disabled on cancelled requisition " + code + ".");
        }
        softly.assertAll();
    }

    /**
     * The server refused because the requisition is linked to other
     * operations. It must still be Approved after a reload. Its API record is
     * logged so the flag that shows the link can be found and added to
     * {@link PurchaseRequisitionPage#isUnlinked}.
     */
    private void skipLinked(String code, Map<String, Object> record, int attempt) {

        page.reload();
        form.waitForStatusLabel(APPROVED.label()); // a refused action must change nothing
        Map<String, Object> flags = new java.util.TreeMap<>();
        record.forEach((key, value) -> {
            if (value instanceof Boolean || value instanceof Number) {
                flags.put(key, value);
            }
        });
        log.warn("{} is linked to other operations (server refused, expected) → another one ({}/{}). "
                + "API flags: {}", code, attempt, TestData.MAX_LINKED_ATTEMPTS, flags);
        page.closeForm();
        page.waitForListLoaded();
    }


    /**
     * Opens a random requisition matching {@code filter}, chosen through the
     * API. The test is SKIPPED when QC has none.
     */
    private Map<String, Object> openRandomRequisition(Predicate<Map<String, Object>> filter) {

        Map<String, Object> record = page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no requisition for this scenario."));
        form.waitForCode();
        return record;
    }

    /**
     * Opens a random Draft. QC had only 9 Drafts on 2026-11-01, so when there
     * is none, one is created first instead of skipping.
     */
    private void openDraftRequisition() {

        if (page.openRandomFromListWithStatus(DRAFT.label()).isPresent()) {
            form.waitForStatusLabel(DRAFT.label());
            return;
        }
        log.info("No Draft requisition on any list page → creating one.");
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        saveAndVerify(DRAFT);
    }

    /**
     * Fills a new requisition's header in the SAME ORDER as the screen
     * (right to left, row by row, QC 2026-11-01):
     * النوع → المورد → العملة → مندوب المشتريات →
     * الفرع → المخزن → مركز التكلفة → اخر موعد للاستلام → الاسعار →
     * التاريخ → تاريخ الانتهاء → انزال من / الرقم (new requisition only) →
     * البيان. The item line is added after this, by the caller.
     * Skipped on purpose: المستند المرجعي (left empty, as agreed with the
     * user on 2026-11-01), الرقم المرجعي, التعادل and تاريخ الاعتماد
     * (read-only).
     */
    private void fillHeader() {
        editing = false;
        fillHeaderInScreenOrder(true);
    }

    /**
     * True while changing a saved requisition: every field gets a value
     * DIFFERENT from the saved one, so the edit really changes all of them
     * (a random pick could pick the same supplier, the same price option or
     * the same date).
     */
    private boolean editing;

    /** Edit mode: same order; only editable fields; branch is kept. */
    private void changeHeader() {
        // All fields, same as a new requisition, branch and "انزال من"
        // included (agreed with the user, 2026-11-01), each one changed.
        editing = true;
        try {
            fillHeaderInScreenOrder(true);
        } finally {
            editing = false;
        }
    }

    private void fillHeaderInScreenOrder(boolean newDocument) {

        selectIfEditable("operationTypeCategoryId");
        selectSupplierUntilWarehouseIsFilled(newDocument);
        selectIfEditable("costCenterId");
        LocalDate today = LocalDate.now();
        page.setExpectedReceiptDate(changed("expectedReceiptDate", today.plusDays(TestData.EXPECTED_RECEIPT_IN_DAYS)));
        if (editing) {
            page.selectOtherPriceOption();
        } else {
            page.selectRandomPriceOption();
        }
        page.setRequestDate(editing && today.equals(page.dateValue("requestDate")) ? today.minusDays(1) : today);
        page.setRequestEndDate(changed("requestEndDate", today.plusDays(TestData.REQUEST_END_IN_DAYS)));
        // "انزال من / الرقم" exists only while adding. In edit mode the form
        // has no such field (QC, 2026-11-01, Draft PR2608-000019), so it is skipped.
        if (newDocument && page.hasField("downloadFrom", Duration.ofSeconds(2))) {
            downloadLinesFromRandomDocument();
        } else if (newDocument) {
            log.info("\"انزال من\" is not on the form in this mode → skipped.");
        }
        String note = TestData.uniqueNote();
        form.setNote(note);
        assertEquals(form.waitForNote(note), note, "\"البيان\" was not filled.");
        log.info("note = {}", note);
    }

    /** {@code wanted}, or one day later when editing and the field already holds it. */
    private LocalDate changed(String dateField, LocalDate wanted) {
        return editing && wanted.equals(page.dateValue(dateField)) ? wanted.plusDays(1) : wanted;
    }

    /**
     * انزال من (random screen) → open the grid of "الرقم" → random document →
     * download its lines into the grid (same flow as the old test and as
     * orders' HeaderOptions.loadItemsFromAnotherOrder).
     */
    private void downloadLinesFromRandomDocument() {

        int before = form.gridRowCount();
        String number = page.downloadLinesFrom(null);
        int after = form.gridRowCount();
        log.info("Downloaded {} line(s) from {}.", after - before, number);
        assertTrue(after > before, "No lines were downloaded from " + number + ".");
    }

    /** Random option in {@code field} when it is editable; confirms the optional dialog that may follow. */
    private void selectIfEditable(String field) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, form.getSelectedText(field));
            return;
        }
        if (editing) {
            page.selectRandomOtherOption(field);
        } else {
            page.selectRandomOption(field);
        }
        page.confirmDialogIfShown(Duration.ofMillis(800));
    }

    /**
     * المورد → العملة → مندوب المشتريات → (الفرع) → المخزن, in screen order.
     * The supplier comes first, as on the screen. When the warehouse is still
     * EMPTY at the end, ANOTHER supplier is chosen, then ANOTHER branch (when the
     * branch is chosen here), then the warehouse again, up to
     * {@link TestData#MAX_SUPPLIER_ATTEMPTS} times. Currency and representative
     * keep the value of the first selection (put back if the app resets them).
     *
     * @param chooseBranch true on a new requisition; in edit mode the branch is kept.
     */
    private void selectSupplierUntilWarehouseIsFilled(boolean chooseBranch) {

        List<String> between = chooseBranch
                ? List.of("currencyId", "purchaseRepresentativeId", "branchId")
                : List.of("currencyId", "purchaseRepresentativeId");
        Map<String, String> firstChoice = new java.util.LinkedHashMap<>();

        retryUntilSuccessful("Supplier → Branch → Warehouse (warehouse must not stay empty)",
                TestData.MAX_SUPPLIER_ATTEMPTS, () -> {
          try {

            if (firstChoice.isEmpty()) {
                selectIfEditable("vendorId");
                for (String field : between) {
                    selectIfEditable(field);
                    firstChoice.put(field, form.getSelectedText(field));
                }
            } else {
                // Retry (the warehouse stayed empty): ANOTHER supplier → the fields in between
                // as first chosen (put back if the app reset them) → ANOTHER branch → warehouse.
                selectAnotherOption("vendorId");
                Map<String, String> kept = new java.util.LinkedHashMap<>(firstChoice);
                kept.remove("branchId");
                restoreFirstChoice(kept);
                if (firstChoice.containsKey("branchId")) {
                    selectAnotherOption("branchId");
                }
            }

            if (waitForWarehouseSelectable()) { // always (re)chosen: every field is filled
                if (editing) {
                    page.selectRandomOtherOption("warehouseId");
                } else {
                    page.selectRandomOption("warehouseId");
                }
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }

            String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(3));
            if (warehouse.isEmpty()) {
                log.warn("Warehouse is empty with supplier [{}] → another supplier, then branch and warehouse.",
                        form.getSelectedText("vendorId"));
                return false;
            }
            log.info("Warehouse = {}", warehouse);
            return true;
          } catch (RuntimeException failed) {
            log.warn("Supplier → Branch → Warehouse failed ({}) → again.", failed.getMessage());
            return false;
          }
        });
    }

    /** Another random option in {@code field} (kept when locked). */
    private void selectAnotherOption(String field) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, form.getSelectedText(field));
            return;
        }
        page.selectRandomOtherOption(field);
        page.confirmDialogIfShown(Duration.ofMillis(800));
    }

    /** Re-selects every field whose value differs from {@code firstChoice} (kept when locked). */
    private void restoreFirstChoice(Map<String, String> firstChoice) {

        firstChoice.forEach((field, value) -> {
            String now = form.getSelectedText(field);
            if (value.isEmpty() || value.equals(now) || page.isSelectDisabled(field)) {
                return;
            }
            log.info("{} changed to [{}] after the supplier change → restoring [{}].", field, now, value);
            page.selectOption(field, value);
            page.confirmDialogIfShown(Duration.ofMillis(800));
        });
    }

    /** The warehouse becomes selectable after the branch (max 5 s); false if it stays locked. */
    private boolean waitForWarehouseSelectable() {
        long end = System.currentTimeMillis() + 5_000;
        while (System.currentTimeMillis() < end) {
            if (!page.isSelectDisabled("warehouseId")) {
                return true;
            }
            pause(POLL_MILLIS);
        }
        return false;
    }

    private boolean isItemOnAnotherLine(String code, int row) {
        int rows = form.gridRowCount();
        for (int other = 0; other < rows; other++) {
            if (other != row && code.equals(page.lineText(other, "code"))) {
                return true;
            }
        }
        return false;
    }

    /** Max items tried on a new line before giving up. */
    private static final int MAX_ITEM_ATTEMPTS = 8;

    /**
     * Adds one item line: random item, quantity and price.
     * Lines with price 0 (downloaded / copied) block "اضافة سطر", so they are
     * priced first. An item that the document does not accept opens an
     * empty "البحث عن صنف" dialog (seen on orders); that dialog is closed and
     * another item is tried.
     */
    private void addItemLine() {

        fillZeroPricesOfLines();
        // Enter on the last line makes the grid add an empty line by itself;
        // remove it so "اضافة سطر" adds the new line at a known place.
        page.deleteEmptyLines();
        int row = page.addLine();

        for (int attempt = 1; attempt <= MAX_ITEM_ATTEMPTS; attempt++) {
            String item = page.selectRandomItem(row); // waits until the item's data is in the line
            if (page.lineText(row, "uomId").isEmpty()) {
                // Not accepted: its data never loaded (on orders an empty
                // "البحث عن صنف" dialog opens in that case).
                form.closeEmptyItemSearchDialog();
                log.warn("Item {} did not load into line {} → another item ({}/{}).",
                        item, row + 1, attempt, MAX_ITEM_ATTEMPTS);
                continue;
            }
            // A 0 price is entered below, as on orders (agreed with the user,
            // 2026-11-01). If the item's cost is 0, the server refuses the save
            // ("هذا الصنف تكلفته صفر", an error), and the test fails on it. That
            // is reported as a system issue, not hidden by the test.
            // An item may appear only once ("الصنف مكرر").
            String code = page.lineText(row, "code");
            if (isItemOnAnotherLine(code, row)) {
                log.warn("Item {} ({}) is already on another line (\"الصنف مكرر\") → another item ({}/{}).",
                        item, code, attempt, MAX_ITEM_ATTEMPTS);
                continue;
            }
            // Only الصنف, الكمية, السعر and البيان are filled (agreed with the
            // user, 2026-11-01). Unit and warehouse keep what the item brings.
            page.setLineNumber(row, "quantity", TestData.QUANTITY);
            page.setLineNumber(row, "price", TestData.PRICE);
            page.setLineText(row, "note", TestData.LINE_NOTE);
            page.stopCellEditing();
            log.info("Line {}: {} × {} @ {}, note [{}]", row + 1, item, TestData.QUANTITY, TestData.PRICE,
                    TestData.LINE_NOTE);
            return;
        }
        fail("No accepted item found for the new line after " + MAX_ITEM_ATTEMPTS + " attempts.");
    }

    /**
     * Completes the lines already in the grid (downloaded, copied or imported),
     * the same way as orders (PurchaseOrderTest.fillZeroPricesOfLines) and the
     * old requisition code. Lines are completed from the last to the first:
     * <ul>
     *     <li>a repeated item is removed and the first line is kept. The
     *     server rejects it otherwise ("الصنف ... مكرر", QC 2026-11-01);</li>
     *     <li>QUANTITY first: an empty or 0 quantity gets {@link TestData#QUANTITY};</li>
     *     <li>then PRICE: a 0 price gets {@link TestData#PRICE}. A line with
     *     price 0 blocks "اضافة سطر". A 0 price means the item cost is 0,
     *     and the server then refuses the save ("هذا الصنف تكلفته صفر"). The
     *     line is kept and the test fails on it, as agreed with the user on
     *     2026-11-01: it is a system issue for the analysis team.</li>
     * </ul>
     * setLineNumber checks every committed value.
     */
    private void fillZeroPricesOfLines() {

        page.waitForGridToSettle();
        int rows = form.gridRowCount();
        java.util.Map<String, Integer> firstRowOfItem = new java.util.HashMap<>();
        for (int row = 0; row < rows; row++) {
            firstRowOfItem.putIfAbsent(page.lineText(row, "code"), row);
        }

        for (int row = rows - 1; row >= 0; row--) {
            String code = page.lineText(row, "code");
            if (!code.isEmpty() && firstRowOfItem.get(code) != row) {
                log.info("Line {} repeats item {} ({}) → removing it (\"الصنف مكرر\").",
                        row + 1, code, page.lineText(row, "itemId"));
                page.deleteLine(row);
                continue;
            }
            if (page.lineNumber(row, "quantity").signum() == 0) {
                log.info("Line {} has no quantity → entering {}.", row + 1, TestData.QUANTITY);
                page.setLineNumber(row, "quantity", TestData.QUANTITY);
            }
            if (page.lineNumber(row, "price").signum() == 0) {
                log.info("Line {} has price 0 → entering {}.", row + 1, TestData.PRICE);
                page.setLineNumber(row, "price", TestData.PRICE);
            }
        }
        page.stopCellEditing();
    }

    /**
     * Saves and verifies the RESULT:
     * success toast → view mode → number → description persisted → status.
     *
     * @return the saved requisition's number.
     */
    private String saveAndVerify(DocumentStatus... allowedStatuses) {

        // "البيان" was filled with a unique text in the header step; keep it.
        // Only when the scenario did not fill it (copy, approved edit) is one entered here.
        String note = form.getNote();
        if (!note.startsWith(TestData.NOTE_PREFIX)) {
            note = TestData.uniqueNote();
            form.setNote(note);
        }

        page.stopCellEditing();
        page.deleteEmptyLines();
        // Some saved requisitions have no branch / warehouse (QC 2026-11-01:
        // PR2610B44-000003). They are required, so they are filled before saving.
        fillRequiredFields(TestData.REQUIRED_HEADER_FIELDS.stream()
                .filter(f -> form.getSelectedText(f).isEmpty() && page.waitForSelectEnabled(f, Duration.ofSeconds(1)))
                .toList());
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.startRecordingSaveRequest();
        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog();

        DocumentForm.Toast toast = form.waitForToast(); // read first: it disappears after ~2–3 s
        // QC 2026-11-01: after adding attachments the upload toast "تمت إضافة
        // الملفات بنجاح / تم الرفع بنجاح" can still be shown (or come late) when
        // saving. It is not the save answer: the next toast is read instead.
        // Attachments are uploaded WITH the save: "تمت إضافة الملفات" shows
        // together with the save toast (stacked), and waitForToast() keeps
        // returning the first one. So the other toasts are read from the page:
        // an error among them fails the save; otherwise the save answer is
        // judged below by the view mode, the number and the note.
        boolean uploadOnly = false;
        if (isUploadToast(toast)) {
            List<String> others = page.otherToastTexts("تمت إضافة الملفات", Duration.ofSeconds(8));
            log.info("Upload toast [{}] read during the save; other toasts: {}", toast.fullText().replace('\n', ' '), others);
            assertTrue(others.stream().noneMatch(t -> t.contains("خطأ")),
                    "Save failed on the server: " + others + " | lines sent: " + page.lastSavedLines());
            uploadOnly = others.stream().noneMatch(t -> t.contains(TestData.SAVE_SUCCESS_MESSAGE));
            if (uploadOnly) {
                log.warn("Only the upload toast was seen; the save is checked by its result (view mode, number, note).");
            }
        }
        boolean warning = isWarning(toast) || uploadOnly || isUploadToast(toast);
        if (warning) {
            log.warn("Save shows a warning (not blocking): {}", toast);
        } else {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast
                    + " | lines sent: " + page.lastSavedLines());
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = form.waitForCode();

        SoftAssert softly = new SoftAssert();
        if (!warning) {
            softly.assertTrue(toast.fullText().contains(TestData.SAVE_SUCCESS_MESSAGE),
                    "Unexpected success message: " + toast);
        }
        softly.assertEquals(form.waitForNote(note), note, "Description was not persisted in " + code + ".");
        String status = form.getStatusLabel();
        softly.assertTrue(DocumentStatus.fromLabel(status).isOneOf(allowedStatuses),
                "Unexpected status [" + status + "] after saving " + code
                        + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();

        log.info("Saved Purchase Requisition {} with status {}", code, status);
        return code;
    }

    /**
     * A warning toast does not block the save. Titles seen in QC:
     * "تنبيه" (orders, total over the limit) and "تحذير" (requisitions,
     * 2026-11-01: "الصنف لديه طلب سابق لم ينفذ" – the item already has an
     * open requisition). Whether the save really happened is then checked by
     * the view mode, the number and the persisted description.
     */
    /** The attachment upload toast ("تمت إضافة الملفات بنجاح" / "تم الرفع بنجاح"). */
    private static boolean isUploadToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.contains("تمت إضافة الملفات") || text.contains("تم الرفع");
    }

    private static boolean isWarning(DocumentForm.Toast toast) {
        String title = toast.title() == null ? "" : toast.title();
        return title.contains("تنبيه") || title.contains("تحذير");
    }

    /**
     * "حفظ" opens a confirmation only when the form is valid. Otherwise fail
     * with the reason (toast / required fields) instead of a timeout. A lost
     * click (no dialog, no toast, no field error) is retried once.
     */
    private void failIfSaveBlockedBeforeConfirmation() {

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                java.util.Optional<DocumentForm.Toast> toast = form.waitForConfirmationDialogOrToast();
                if (toast.isPresent() && isWarning(toast.get())) {
                    // Warnings are not blocking: wait for the dialog that follows.
                    log.warn("Warning before saving (ignored): {}", toast.get());
                    waitUntil("the save confirmation after a warning", Duration.ofSeconds(10), form::isDialogOpen);
                    return;
                }
                toast.ifPresent(t -> fail("Save was rejected before confirmation: " + t
                        + " | required-field errors: " + form.fieldsWithRequiredError()));
                return;
            } catch (org.openqa.selenium.TimeoutException e) {
                List<String> errors = form.fieldsWithRequiredError();
                if (attempt == 2) {
                    fail("Save did not open the confirmation dialog. Required-field errors: " + errors, e);
                }
                if (!errors.isEmpty()) {
                    // Fix what the form asks for, then save again (asked by the
                    // user, 2026-11-01: PR2610B44-000003 had no warehouse).
                    log.warn("Save blocked by required fields {} → filling them and saving again.", errors);
                    fillRequiredFields(errors);
                } else {
                    log.warn("Save click showed nothing; clicking again.");
                }
                page.clickSave();
            }
        }
    }
    /**
     * Fixes empty required header fields before / after a blocked save, in
     * screen order. The warehouse list depends on the branch: when the
     * warehouse has no option for the chosen branch (QC 2026-11-01: branch
     * "فرع اختبار عميل" has no warehouse), ANOTHER branch is chosen and the
     * warehouse is tried again, up to {@link TestData#MAX_SUPPLIER_ATTEMPTS} times.
     */
    private void fillRequiredFields(List<String> fields) {
        boolean warehouseNeeded = fields.contains("warehouseId")
                || (fields.contains("branchId") && form.getSelectedText("warehouseId").isEmpty());
        if (fields.contains("branchId") || warehouseNeeded) {
            fillBranchAndWarehouse(fields.contains("branchId"));
        }
        for (String field : fields) {
            if (field.equals("branchId") || field.equals("warehouseId")) {
                continue;
            }
            if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
                log.warn("Required field {} is empty but locked → cannot fill it.", field);
                continue;
            }
            String chosen = page.selectRandomOption(field);
            page.confirmDialogIfShown(Duration.ofMillis(800));
            log.info("Required field {} was empty → {}", field, chosen);
        }
    }

    /** Branch (when empty or when its warehouses are missing) then a warehouse that exists for it. */
    private void fillBranchAndWarehouse(boolean branchEmpty) {
        boolean changeBranch = branchEmpty;
        for (int attempt = 1; attempt <= TestData.MAX_SUPPLIER_ATTEMPTS; attempt++) {
            if (changeBranch) {
                if (!page.waitForSelectEnabled("branchId", Duration.ofSeconds(5))) {
                    log.warn("Branch is locked → cannot change it to find a warehouse.");
                    return;
                }
                String branch = page.selectRandomOtherOption("branchId");
                page.confirmDialogIfShown(Duration.ofMillis(800));
                log.info("Branch → {} ({}/{})", branch, attempt, TestData.MAX_SUPPLIER_ATTEMPTS);
            }
            if (!form.getSelectedText("warehouseId").isEmpty()) {
                return; // the app filled it with the branch
            }
            if (!waitForWarehouseSelectable()) {
                log.warn("Warehouse stays locked after choosing the branch → another branch.");
                changeBranch = true;
                continue;
            }
            String warehouse = page.selectRandomOption("warehouseId");
            page.confirmDialogIfShown(Duration.ofMillis(800));
            if (!warehouse.isEmpty()) {
                log.info("Required field warehouseId was empty → {} (branch {})", warehouse,
                        form.getSelectedText("branchId"));
                return;
            }
            log.warn("Branch [{}] has no warehouse → choosing another branch.", form.getSelectedText("branchId"));
            changeBranch = true;
        }
        log.warn("No branch with a warehouse found after {} attempts.", TestData.MAX_SUPPLIER_ATTEMPTS);
    }
}
