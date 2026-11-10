package com.erp.tests;

import com.erp.automation.base.PurchasingBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.pages.QuotationPage;
import com.erp.automation.pages.QuotationPage.RecordStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.Downloads;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.CANCELLED;
import static com.erp.automation.data.DocumentStatus.COMPLETED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Price Quotation (عروض الاسعار), level 1: create (with and without a
 * reference requisition), edit, undo, status actions, list, print/export,
 * follow-up screens, attachments, toolbar per status and lines.
 *
 * <p>Built on the Purchasing template ({@link PurchasingBaseTest},
 * {@code PurchasingDocumentPage}, {@link DocumentForm}), the same way as
 * {@code PurchaseOrderTest} and {@code PurchaseRequisitionTest}:</p>
 * <ul>
 *     <li>test data in {@link TestData};</li>
 *     <li>each test reads as the business scenario;</li>
 *     <li>steps are in "Reusable steps" at the bottom;</li>
 *     <li>every action ends with a check of its result.</li>
 * </ul>
 *
 * <p>The quotation number is {@code #qCode} on the form and col-id
 * {@code qCode} in the list (not "code" as on orders and requisitions), so
 * the number and the status are read through {@link QuotationPage}.</p>
 *
 * <p>The old test (10 tests, login in every test, fixed sleeps, no checks
 * after saving) was replaced on 2026-11-02. It is kept in git history. Its
 * scenarios are all here: create / add / repeat (copy), each with and
 * without a reference document, edit by status, delete, approve and
 * disapprove.</p>
 */
public class QuotationTest extends PurchasingBaseTest {

    private static final String PQ = "price-quotation";

    private QuotationPage page;
    private DocumentForm form;

    @Override
    protected void openScreen(PurchasingPage purchasingPage) {
        purchasingPage.clickPriceOffers();
        page = new QuotationPage(driver);
        form = new DocumentForm(driver, QuotationPage.PREFIX);
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
        static final String LINE_NOTE = "بيان سطر عرض سعر تجريبي";

        static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";

        /** A newly saved quotation is a Draft, or Approved when auto-approved on save. */
        static final DocumentStatus[] NEW_QUOTATION_STATUSES = {DRAFT, APPROVED};

        /** Times another supplier (or branch) is chosen when the warehouse stays empty. */
        static final int MAX_SUPPLIER_ATTEMPTS = 5;

        /** "طريقة احتساب الضريبة", "الأسعار" and the other required fields (labels with *, QC 2026-11-02). */
        static final List<String> REQUIRED_HEADER_FIELDS = List.of(
                "vendorId", "currencyId", "branchId", "warehouseId", "taxCalculationMethod");

        /** Header values compared after save + reload ("note" is checked by saveAndVerify). */
        static final List<String> PERSISTED_HEADER_FIELDS = List.of(
                "vendorId", "currencyId", "branchId", "warehouseId", "costCenterId",
                "taxCalculationMethod", "prices", "quotationDeadLine", "receivedDate",
                "purchaseRepresentativeId", "vendorQuotationNumber");

        /** Header values an Undo must restore. */
        static final List<String> UNDO_HEADER_FIELDS = List.of(
                "vendorId", "currencyId", "branchId", "warehouseId", "costCenterId",
                "taxCalculationMethod", "prices", "quotationDeadLine", "receivedDate",
                "purchaseRepresentativeId", "vendorQuotationNumber", "note");

        /** Excel file with item lines ("تصدير الاصناف" layout, shared with orders). */
        static final Path EXCEL_LINES = Paths.get(System.getProperty("user.dir"),
                "test-data", "import-excel", "PurchaseOrder.xlsx");
        static final String IMPORT_EXCEL_MENU_ITEM = "الاستيراد من Excel";
        static final String EXPORT_EXCEL_MENU_ITEM = "التصدير الى Excel";

        /** Days from today: "تاريخ الانتهاء" in the future keeps the quotation from expiring. */
        static final int DEADLINE_IN_DAYS = 30;
        static final int RECEIVED_IN_DAYS = 14;
        /** "التاريخ" of a new quotation: in the future, like the other dates. */
        static final int QUOTATION_DATE_IN_DAYS = 1;

        private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        /** Unique description, so a persisted value can be told apart from old data. */
        static final String NOTE_PREFIX = "عرض سعر آلي ";

        static String uniqueNote() {
            return NOTE_PREFIX + LocalDateTime.now().format(STAMP);
        }

        /** "رقم عرض المورد": "VQ-<date>-<time>", as before; a suffix is added if it is taken. */
        static String vendorQuotationNumber() {
            return "VQ-" + LocalDateTime.now().format(STAMP);
        }

        /** Parts of the API calls that prove an action reached the server (same as orders / requisitions). */
        static final String API_CANCEL = "/cancel/";
        static final String API_APPROVE_OR_DISAPPROVE = "/approveOrDisApprove/";

        static final String GROUP_BY_STATUS = "الحالة";

        /** Card label of a disapproved document on the Kanban board (seen on orders). */
        static final String DISAPPROVED_CARD_LABEL = "غير معتمد";

        static final String LOG_CREATED = "انشاء العملية";
        static final String LOG_APPROVED = "اعتماد العملية";

        static final String ATTACHMENT_TITLE = "مرفقات عرض سعر تجريبي";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات عرض السعر";

        /** Toolbar of a NEW quotation (QC 2026-11-02: undo, delete, approve, cancel, copy, print disabled). */
        static final List<String> ACTIONS_DISABLED_ON_NEW = List.of(
                "edit", "delete", "approveOrDisapprove", "cancel", "copy", "undo", "print", "transactionLogs");
        static final List<String> ACTIONS_ENABLED_ON_NEW = List.of("xIcon");

        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "edit", "delete", "approveOrDisapprove", "cancel", "copy", "print");

        /** Approved: delete disabled; edit (description), disapprove and cancel enabled. */
        static final List<String> ACTIONS_ENABLED_ON_APPROVED = List.of("edit", "approveOrDisapprove", "cancel");
        static final List<String> ACTIONS_DISABLED_ON_APPROVED = List.of("delete");

        /** Cancelled (QC 2026-11-02, PQ2608S7-000003): nothing that changes it; approve is not shown. */
        static final List<String> ACTIONS_DISABLED_WHEN_CANCELLED = List.of(
                "edit", "delete", "cancel", "approveOrDisapprove", "uploadAttachment");

        /** Expired (QC 2026-11-02, PQ2608-000011): approve is disabled. */
        static final List<String> ACTIONS_DISABLED_WHEN_EXPIRED = List.of("approveOrDisapprove");

        /** "⋮" → "انشاء امر شراء": enabled on Approved only; opens a new order (QC 2026-11-02). */
        static final String CREATE_ORDER_MENU_ITEM = "انشاء امر شراء";
        static final String CREATE_ORDER_PATH = "/purchase/orders/add/";
        static final String ORDER_PREFIX = "orders";

        /** Server message when an approved document is linked to other operations. */
        static final String LINKED_ERROR = "مرتبط";

        /** Approved quotations tried before the test is skipped. */
        static final int MAX_LINKED_ATTEMPTS = 5;

        /** Requisitions tried in "المستند المرجعي" when one brings no line or an empty, locked warehouse. */
        static final int MAX_REFERENCE_ATTEMPTS = 5;

        private TestData() {
        }
    }

    // =====================================================================
    // Create: without a reference document
    // =====================================================================

    @Test(priority = 1, groups = {PQ, "create"},
            description = "New quotation from the New button: header in screen order, lines downloaded "
                    + "from a random document (انزال من / الرقم), then an item line")
    public void userCanCreateNewPriceQuotationWithoutReferenceDocument() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        addItemLine();
        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
    }

    @Test(priority = 2, groups = {PQ, "create"},
            description = "New quotation from the Add button of an opened quotation")
    public void userCanAddNewPriceQuotationWithoutReferenceDocument() {

        Map<String, Object> opened = openRandomQuotation(r -> true);

        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        addItemLine();
        String shown = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        verifyNewQuotationSaved(opened, shown, "\"إضافة\"");
    }

    @Test(priority = 3, groups = {PQ, "create"},
            description = "Copy (تكرار) an existing quotation, change it and save it as a new one")
    public void userCanRepeatAndEditPriceQuotationWithoutReferenceDocument() {

        Map<String, Object> source = openRandomQuotation(r -> !RecordStatus.CANCELLED.of(r));
        String sourceCode = QuotationPage.codeOf(source);

        page.clickCopy(); // no confirmation step for Copy
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader();
        addItemLine(); // completes the copied / downloaded lines first (quantity, then price)
        String shown = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        verifyNewQuotationSaved(source, shown, "\"تكرار\"");
    }

    @Test(priority = 4, groups = {PQ, "create", "excel"},
            description = "New quotation whose lines are imported from Excel")
    public void userCanCreatePriceQuotationFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        // QC 2026-11-02: on a saved quotation "⋮" has only the export. Whether a
        // NEW one offers the import is checked here; without it the test is skipped.
        if (page.menuItems().stream().noneMatch(i -> i.replaceAll("\\s+", "")
                .equals(TestData.IMPORT_EXCEL_MENU_ITEM.replaceAll("\\s+", "")))) {
            throw new SkipException("\"" + TestData.IMPORT_EXCEL_MENU_ITEM + "\" is not in ⋮ of a new quotation.");
        }

        fillHeader();
        page.importLinesFromExcel(TestData.EXCEL_LINES);
        int lines = form.gridRowCount();
        log.info("Lines imported from Excel: {}", lines);
        assertTrue(lines > 0, "Excel import finished but no lines were added.");

        addItemLine();
        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
    }

    // =====================================================================
    // Create: with a reference document (a requisition)
    // =====================================================================

    @Test(priority = 5, groups = {PQ, "create", "reference"},
            description = "New quotation from a requisition (المستند المرجعي): its lines are downloaded, "
                    + "the rest of the header is completed; the reference is kept after save")
    public void userCanCreateNewPriceQuotationWithReferenceDocument() {

        createFromReferenceDocumentAndVerify(() -> {
            page.clickNew();
            form.waitForMode(DocumentForm.Mode.ADD);
        });
    }

    @Test(priority = 6, groups = {PQ, "create", "reference"},
            description = "Add button of an opened quotation, then a requisition as reference document")
    public void userCanAddNewPriceQuotationWithReferenceDocument() {

        Map<String, Object> opened = openRandomQuotation(r -> true);

        String shown = createFromReferenceDocumentAndVerify(() -> {
            if (!page.isFormOpen()) {
                page.openDocument(opened.get("id")); // a retry starts again from the same quotation
            }
            page.clickAdd();
            form.waitForMode(DocumentForm.Mode.ADD);
        });
        verifyNewQuotationSaved(opened, shown, "\"إضافة\" with a reference document");
    }

    @Test(priority = 7, groups = {PQ, "create", "reference"},
            description = "Copy (تكرار) an existing quotation, then a requisition as reference document")
    public void userCanRepeatAndEditPriceQuotationWithReferenceDocument() {

        Map<String, Object> source = openRandomQuotation(r -> !RecordStatus.CANCELLED.of(r));
        String sourceCode = QuotationPage.codeOf(source);

        String shown = createFromReferenceDocumentAndVerify(() -> {
            if (!page.isFormOpen()) {
                page.openDocument(source.get("id")); // a retry starts again from the same quotation
            }
            page.clickCopy();
            form.waitForMode(DocumentForm.Mode.ADD);
            if (!page.isReferenceDocumentEditable()) {
                throw new SkipException("\"المستند المرجعي\" is locked on the copy of " + sourceCode + ".");
            }
        });
        verifyNewQuotationSaved(source, shown, "\"تكرار\" with a reference document");
    }

    // =====================================================================
    // Edit / Undo
    // =====================================================================

    @Test(priority = 8, groups = {PQ, "edit"},
            description = "Edit a Draft quotation: every editable header field and a new line; it stays a Draft")
    public void userCanEditDraftPriceQuotation() {

        openDraftQuotation();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        addItemLine();
        saveAndVerify(DRAFT);
    }

    @Test(priority = 9, groups = {PQ, "edit", "status"},
            description = "Approved quotation: only the description can be edited; it stays Approved")
    public void approvedPriceQuotationAllowsDescriptionEditOnly() {

        openRandomQuotation(RecordStatus.APPROVED::of);
        page.waitForStatusLabel(APPROVED.label());
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        form.waitUntilFieldEditable("note");

        List<String> editable = form.editableFieldsAmong(QuotationPage.SELECT_FIELDS);
        assertTrue(editable.isEmpty(),
                "Only the description may change on an Approved quotation, but these are editable: " + editable);

        saveAndVerify(APPROVED);
    }

    @Test(priority = 10, groups = {PQ, "edit", "status"},
            description = "Expired quotation (منتهي): edit the header and the description; it stays Expired")
    public void userCanEditExpiredPriceQuotation() {

        // The old test edited "منتهي" like a Draft. QC 2026-11-02: edit is
        // enabled on expired quotations (PQ2608-000011). Whether that is
        // intended is asked to the analysis team; here the result of the
        // edit is checked: saved, and still "منتهي".
        openRandomQuotation(RecordStatus.EXPIRED::of);
        String code = page.waitForCode();
        page.waitForStatusLabel(COMPLETED.label());
        if (!page.waitForActionEnabled("edit")) {
            throw new SkipException("\"تعديل\" is disabled on expired quotation " + code + ".");
        }
        page.clickEdit();
        waitForEditMode();

        changeHeader();
        fillZeroPricesOfLines();
        saveAndVerify(COMPLETED, DRAFT);
    }

    @Test(priority = 11, groups = {PQ, "edit"},
            description = "Edit a Draft quotation (all fields + a line), then Undo restores the saved values")
    public void userCanUndoPriceQuotationChanges() {

        openDraftQuotation();
        String code = page.waitForCode();
        Map<String, String> saved = savedHeaderSnapshot();
        int savedLines = page.lineCount();
        log.info("Saved values of {}: {}, {} line(s).", code, saved, savedLines);

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        changeHeader();
        addItemLine();
        log.info("Changed values before undo: {}, {} line(s).",
                page.readHeaderValues(TestData.UNDO_HEADER_FIELDS), page.lineCount());

        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");
        page.clickUndo();
        page.confirmDialog();

        // Same quotation = same id in the URL. The number field itself is
        // checked with the other fields: QC 2026-11-02 (PQ2609S26-000001) Undo
        // left "الرقم المرجعي" and "البيان" EMPTY while the rest came back.
        assertTrue(driver.getCurrentUrl().matches(".*/" + id + "([/?#].*)?$"),
                "Undo left quotation " + code + " (id " + id + "): " + driver.getCurrentUrl());

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(page::code, code), code,
                "Undo did not restore \"الرقم المرجعي\" of " + code + ".");
        saved.forEach((field, value) -> softly.assertEquals(
                waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), value), value,
                "Undo did not restore \"" + field + "\" of " + code + "."));
        softly.assertEquals(waitUntilEquals(() -> String.valueOf(page.lineCount()), String.valueOf(savedLines)),
                String.valueOf(savedLines), "Undo did not remove the added line of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 12, groups = {PQ, "edit"},
            description = "Header values of an edited Draft are the same after save and reload")
    public void editedDraftPriceQuotationHeaderIsKeptAfterSave() {

        openDraftQuotation();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        fillZeroPricesOfLines();
        Map<String, String> beforeSave = page.readHeaderValues(TestData.PERSISTED_HEADER_FIELDS);

        String code = saveAndVerify(DRAFT);
        page.reload();

        SoftAssert softly = new SoftAssert();
        for (String field : TestData.PERSISTED_HEADER_FIELDS) {
            String expected = beforeSave.get(field);
            String actual = waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), expected);
            softly.assertEquals(actual, expected, "Field " + field + " of " + code + " changed after save + reload.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Status actions: delete, approve, disapprove, cancel
    // =====================================================================

    @Test(priority = 13, groups = {PQ, "status"},
            description = "Delete a Draft quotation: success message, and it is no longer in the list")
    public void userCanDeleteDraftPriceQuotation() {

        openDraftQuotation();
        String code = page.waitForCode();
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
        assertTrue(page.api().find(r -> code.equals(QuotationPage.codeOf(r))).isEmpty(),
                code + " is still returned by the API after deletion.");
    }

    @Test(priority = 14, groups = {PQ, "status", "log"},
            description = "Approve a Draft quotation: Approved after reload, approval date today, "
                    + "delete disabled, one approval log entry")
    public void userCanApproveDraftPriceQuotation() {

        openDraftQuotation();
        String code = page.waitForCode();
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE,
                this::toggleApprovalOfOpenedQuotation);
        assertAccepted("Approving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
        page.waitForStatusLabel(APPROVED.label());

        page.reload();
        page.waitForStatusLabel(APPROVED.label()); // kept on the server
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ الاعتماد\" of " + code + " is not today.");
        softly.assertFalse(page.isActionEnabled("delete"), "An approved quotation (" + code + ") must not be deletable.");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Approving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (!logAfter.isEmpty()) {
            softly.assertEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The last log entry of " + code + " is not the approval.");
        }
        softly.assertAll();
    }

    @Test(priority = 15, groups = {PQ, "status", "log"},
            description = "Disapprove an Approved quotation not converted to an order: back to Draft, "
                    + "draft actions enabled, one new log entry")
    public void userCanDisapproveApprovedPriceQuotation() {

        String code = null;
        List<String> logBefore = null;
        Set<String> tried = new HashSet<>();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS && code == null; attempt++) {
            Map<String, Object> record = openRandomQuotation(r -> RecordStatus.APPROVED.of(r)
                    && QuotationPage.isUnlinked(r) && !tried.contains(QuotationPage.codeOf(r)));
            page.waitForStatusLabel(APPROVED.label());
            String candidate = page.waitForCode();
            tried.add(candidate);
            List<String> logNow = page.readTransactionLog();

            ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE,
                    this::toggleApprovalOfOpenedQuotation);
            if (outcome.mentions(TestData.LINKED_ERROR)) {
                skipLinked(candidate, record, attempt);
                continue;
            }
            assertAccepted("Disapproving " + candidate, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
            code = candidate;
            logBefore = logNow;
        }
        if (code == null) {
            throw new SkipException("Every approved quotation tried is linked to other operations: " + tried);
        }
        page.waitForStatusLabel(DRAFT.label());

        page.reload();
        page.waitForStatusLabel(DRAFT.label()); // kept on the server
        page.waitForActionEnabled("edit");
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action),
                    "'" + action + "' must be enabled on disapproved quotation " + code + ".");
        }
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Disapproving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (logAfter.size() > logBefore.size()) {
            softly.assertNotEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The new log entry of " + code + " is an approval, not a disapproval.");
        }
        softly.assertAll();
    }

    @Test(priority = 16, groups = {PQ, "cancel"},
            description = "Cancel a Draft quotation: Cancelled after reload; edit/delete/cancel/attachments disabled")
    public void userCanCancelDraftPriceQuotation() {

        openDraftQuotation();
        String code = page.waitForCode();
        assertAccepted("Cancelling " + code, TestData.API_CANCEL, cancelOpenedQuotation());
        verifyCancelled(code);
    }

    @Test(priority = 17, groups = {PQ, "cancel"},
            description = "Cancel an Approved quotation not converted to an order: Cancelled after reload")
    public void userCanCancelApprovedPriceQuotation() {

        Set<String> tried = new HashSet<>();
        for (int attempt = 1; attempt <= TestData.MAX_LINKED_ATTEMPTS; attempt++) {
            Map<String, Object> record = openRandomQuotation(r -> RecordStatus.APPROVED.of(r)
                    && QuotationPage.isUnlinked(r) && !tried.contains(QuotationPage.codeOf(r)));
            page.waitForStatusLabel(APPROVED.label());
            String code = page.waitForCode();
            tried.add(code);

            ActionOutcome outcome = cancelOpenedQuotation();
            if (outcome.mentions(TestData.LINKED_ERROR)) {
                skipLinked(code, record, attempt);
                continue;
            }
            assertAccepted("Cancelling " + code, TestData.API_CANCEL, outcome);
            verifyCancelled(code);
            return;
        }
        throw new SkipException("Every approved quotation tried is linked to other operations: " + tried);
    }

    @Test(priority = 18, groups = {PQ, "cancel", "validation"},
            description = "Approved quotation converted to a purchase order: cancel and disapprove are disabled")
    public void linkedApprovedPriceQuotationCannotBeCancelledOrDisapproved() {

        // Nothing is clicked: if the system accepted it, a linked quotation
        // would really be cancelled. QC 2026-11-02: both buttons are ENABLED on
        // PQ2608-000005 and PQ2609S7-000009 (isInvoice = true), the same as
        // on orders and requisitions, so this test is expected to fail.
        Map<String, Object> record = openRandomQuotation(
                r -> RecordStatus.APPROVED.of(r) && !QuotationPage.isUnlinked(r));
        page.waitForStatusLabel(APPROVED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("print"); // toolbar has settled

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("cancel", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' is enabled on approved quotation "
                    + code + ", which is converted to a purchase order (isInvoice: " + record.get("isInvoice") + ").");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Toolbar per status
    // =====================================================================

    @Test(priority = 19, groups = {PQ, "validation"},
            description = "New quotation toolbar: save/close enabled; edit/delete/approve/cancel/copy/undo/print disabled")
    public void newPriceQuotationEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("xIcon");

        SoftAssert softly = new SoftAssert();
        // QC 2026-11-02: on an untouched new quotation "حفظ" is DISABLED too; it
        // becomes enabled once something is entered (unlike orders, where it is
        // enabled ~1 s after opening). Checked below after choosing a supplier.
        softly.assertFalse(page.isActionEnabled("save"), "'save' should wait for a change on a new quotation.");
        // A button that is not rendered counts as not usable (page.isActionEnabled is false when absent).
        for (String action : TestData.ACTIONS_DISABLED_ON_NEW) {
            log.info("New quotation: '{}' {}", action,
                    page.isActionEnabled(action) ? "ENABLED" : (page.hasAction(action) ? "disabled" : "not shown"));
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new quotation.");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_NEW) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new quotation.");
        }
        page.selectRandomOption("vendorId");
        page.confirmDialogIfShown(Duration.ofMillis(800));
        softly.assertTrue(page.waitForActionEnabled("save"), "'save' stayed disabled after choosing a supplier.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 20, groups = {PQ, "validation", "status"},
            description = "Draft quotation: edit/delete/approve/cancel enabled, no approval date, "
                    + "⋮ \"Create purchase order\" disabled")
    public void draftPriceQuotationEnablesEditActions() {

        openDraftQuotation();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        softly.assertEquals(page.inputValue("approvalDate"), "", "Draft " + code + " must not have an approval date.");
        softly.assertFalse(page.isMenuItemEnabled(TestData.CREATE_ORDER_MENU_ITEM),
                "\"" + TestData.CREATE_ORDER_MENU_ITEM + "\" must be disabled on Draft " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 21, groups = {PQ, "validation", "status"},
            description = "Approved quotation: delete disabled; edit/disapprove/cancel enabled; approval date set; "
                    + "⋮ \"Create purchase order\" enabled")
    public void approvedPriceQuotationCannotBeDeleted() {

        openRandomQuotation(RecordStatus.APPROVED::of);
        page.waitForStatusLabel(APPROVED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_ON_APPROVED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on Approved " + code + ".");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_APPROVED) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Approved " + code + ".");
        }
        softly.assertFalse(page.inputValue("approvalDate").isBlank(), "Approved " + code + " has no approval date.");
        softly.assertTrue(page.isMenuItemEnabled(TestData.CREATE_ORDER_MENU_ITEM),
                "\"" + TestData.CREATE_ORDER_MENU_ITEM + "\" must be enabled on Approved " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 22, groups = {PQ, "validation", "status"},
            description = "Cancelled quotation is read-only: edit/delete/cancel/approve/attachments disabled, "
                    + "⋮ \"Create purchase order\" disabled")
    public void cancelledPriceQuotationIsReadOnly() {

        openRandomQuotation(RecordStatus.CANCELLED::of);
        page.waitForStatusLabel(CANCELLED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("print");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        softly.assertFalse(page.isMenuItemEnabled(TestData.CREATE_ORDER_MENU_ITEM),
                "\"" + TestData.CREATE_ORDER_MENU_ITEM + "\" must be disabled on cancelled " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 23, groups = {PQ, "validation", "status"},
            description = "Expired quotation: approve disabled, ⋮ \"Create purchase order\" disabled")
    public void expiredPriceQuotationCannotBeApproved() {

        openRandomQuotation(RecordStatus.EXPIRED::of);
        page.waitForStatusLabel(COMPLETED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("print");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_EXPIRED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on expired " + code + ".");
        }
        softly.assertFalse(page.isMenuItemEnabled(TestData.CREATE_ORDER_MENU_ITEM),
                "\"" + TestData.CREATE_ORDER_MENU_ITEM + "\" must be disabled on expired " + code + ".");
        // QC 2026-11-02: edit, delete and cancel are ENABLED on expired
        // quotations (as on requisitions). The rule is asked to the analysis
        // team, so they are only logged here.
        for (String action : List.of("edit", "delete", "cancel")) {
            log.info("Expired {}: '{}' is {}", code, action, page.isActionEnabled(action) ? "enabled" : "disabled");
        }
        softly.assertAll();
    }

    @Test(priority = 24, groups = {PQ, "validation"},
            description = "Saving an empty quotation is blocked: error, required fields marked, stays in Add")
    public void userCannotSavePriceQuotationWithoutRequiredData() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        // "حفظ" is enabled only after a change (QC 2026-11-02), so only "البيان"
        // is filled: every required field stays empty.
        form.setNote(TestData.uniqueNote());
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled after typing the description.");

        page.clickSave();
        DocumentForm.Toast toast;
        try {
            toast = form.waitForToast(); // read first: it disappears after 2-3 s
        } catch (org.openqa.selenium.TimeoutException none) {
            toast = null; // the field errors alone may block it
        }
        List<String> marked = form.fieldsWithRequiredError();
        log.info("Empty save → toast [{}], required errors on {}", toast, marked);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast != null && toast.isError(), "Expected an error message, got: " + toast);
        // Agreed with the user (2026-11-02): the error message and the blocked
        // save are enough. QC 2026-11-02: the screen shows only "يجب إضافة
        // تفاصيل صنف واحد على الأقل" and marks NO empty required field (orders
        // mark them); that is a note in the analysis report, only logged here.
        List<String> empty = TestData.REQUIRED_HEADER_FIELDS.stream().filter(this::isEmptySelect).toList();
        if (!marked.containsAll(empty)) {
            log.warn("Empty required fields {} are not marked \"هذا الحقل مطلوب\" (marked: {}).", empty, marked);
        }
        softly.assertFalse(form.isDialogOpen(), "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();

        page.closeForm();
    }

    // =====================================================================
    // List: search, navigation, pagination, Kanban, group by, summaries
    // =====================================================================

    @Test(priority = 25, groups = {PQ, "search"},
            description = "List search by number shows that quotation")
    public void userCanSearchPriceQuotationsListByCode() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        page.closeForm();
        page.waitForListLoaded();

        page.searchList(code);
        assertTrue(page.waitUntilListShows(code),
                "Searching for " + code + " did not show it. Rows shown: " + page.visibleListCodes());
    }

    @Test(priority = 26, groups = {PQ, "search"},
            description = "Record search (magnifier) in the form opens the quotation typed")
    public void userCanOpenPriceQuotationFromRecordSearch() {

        openRandomQuotation(r -> true);
        String target = page.waitForCode();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
        assertNotEquals(waitUntilChanges(target), target, "Could not move away from " + target + ".");

        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 27, groups = {PQ, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenPriceQuotationRecords() {

        openRandomQuotation(r -> true);
        SoftAssert softly = new SoftAssert();

        if (page.isRecordNavigationEnabled("pageFirst")) {
            String before = page.waitForCode();
            page.clickRecordNavigation("pageFirst");
            waitUntilChanges(before);
        }
        String first = page.waitForCode();
        for (String button : List.of("pageFirst", "pagePrevious10", "pageBack")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the first record (" + first + ").");
        }

        page.clickRecordNavigation("pageNext");
        String second = waitUntilChanges(first);
        softly.assertNotEquals(second, first, "'Next' did not open another quotation.");

        page.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(page::code, first), first, "'Back' did not return to the first one.");

        page.clickRecordNavigation("pageLast");
        String last = waitUntilChanges(first);
        for (String button : List.of("pageLast", "pageNext10", "pageNext")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the last record (" + last + ").");
        }
        log.info("Record navigation: first={}, second={}, last={}", first, second, last);
        softly.assertAll();
    }

    @Test(priority = 28, groups = {PQ, "pagination"},
            description = "List pager: first/last page, Next to the last page, back to a random page, open a random row")
    public void userCanNavigatePriceQuotationsPagination() {

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

        // Page by page with "Next" up to the last page.
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

        // Back with "Previous" to a random page, then a random row of it opens with its status.
        int target = 1 + new Random().nextInt(pages);
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
        String code = codes.get(new Random().nextInt(codes.size()));
        log.info("{} page(s); back on page {} {} → opening {} [{}].", pages, target, Arrays.toString(chosen),
                code, rows.get(code));
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another quotation than " + code + ".");
        page.waitForStatusLabel(rows.get(code));
    }

    @Test(priority = 29, groups = {PQ, "list"},
            description = "Kanban shows the same quotations as the list page, each with a status; a card opens it")
    public void userCanSwitchPriceQuotationsListToKanbanAndBack() {

        page.waitForListLoaded();
        List<String> gridCodes = page.visibleListCodes();
        try {
            page.switchToKanbanView();
            List<String> cardCodes = page.kanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);

            SoftAssert softly = new SoftAssert();
            softly.assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes),
                    "Kanban cards do not show the same quotations as the list page.");
            List<String> labels = new ArrayList<>(
                    Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList());
            labels.add(TestData.DISAPPROVED_CARD_LABEL);
            for (String code : cardCodes) {
                String status = page.kanbanCardStatus(code);
                softly.assertTrue(labels.contains(status), "Card " + code + " has an unknown status [" + status + "].");
            }
            softly.assertAll();

            String target = cardCodes.get(new Random().nextInt(cardCodes.size()));
            page.openKanbanCard(target);
            assertEquals(waitUntilEquals(page::code, target), target,
                    "Double-clicking the card of " + target + " opened another quotation.");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView(); // leave the list as the other tests expect it
            }
        }
    }

    @Test(priority = 30, groups = {PQ, "list"},
            description = "Group by status: one group per known status; the groups add up to the list total")
    public void userCanGroupPriceQuotationsListByStatus() {

        page.waitForListLoaded();
        int total = page.paginationRange()[2];
        page.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = page.groupCounts();
        int sum = groups.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Groups by status: {} (sum {}), list total {}, API {}", groups, sum, total, page.api().count());

        List<String> labels = Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList();
        SoftAssert softly = new SoftAssert();
        for (Map.Entry<String, Integer> group : groups.entrySet()) {
            softly.assertTrue(labels.contains(group.getKey()), "Unknown status group [" + group.getKey() + "].");
            softly.assertTrue(group.getValue() > 0, "Group [" + group.getKey() + "] is empty.");
        }
        softly.assertEquals(sum, total, "Status groups add up to " + sum + " but the list total is " + total + ".");
        softly.assertAll();
    }

    @Test(priority = 31, groups = {PQ, "list"},
            description = "Summaries (الملخصات): each status card equals the number of quotations in that status")
    public void priceQuotationSummariesMatchStatusGroups() {

        page.waitForListLoaded();
        Map<String, Integer> summaries = page.summaryCards();
        page.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = page.groupCounts();
        log.info("Summaries: {} | groups by status: {}", summaries, groups);

        SoftAssert softly = new SoftAssert();
        for (DocumentStatus status : DocumentStatus.values()) {
            softly.assertEquals(summaries.getOrDefault(status.label(), 0), groups.getOrDefault(status.label(), 0),
                    "Summary card [" + status.label() + "] does not match the quotations in that status.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Print, PDF, Excel
    // =====================================================================

    @Test(priority = 32, groups = {PQ, "print"},
            description = "Print a random quotation: the preview shows its number and supplier; "
                    + "then it is printed through Chrome's print dialog")
    public void userCanPrintPriceQuotation() throws java.awt.AWTException {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
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
        if (!warehouse.isBlank() && !warehouse.equals("اختر")) {
            softly.assertTrue(preview.contains(warehouse),
                    "Print preview of " + code + " does not show the warehouse [" + warehouse + "].");
        }
        softly.assertAll();

        page.printFromPreview();
        page.confirmChromePrintDialog();

        page.closePrintPreview();
        assertEquals(page.waitForCode(), code, "After printing, the form does not show " + code + " any more.");
        log.info("{} printed.", code);
    }

    @Test(priority = 33, groups = {PQ, "print"},
            description = "Save a random quotation as PDF: the preview shows it; a PDF file is downloaded")
    public void userCanSavePriceQuotationAsPdf() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        Path dir = page.prepareDownloadFolder("pdf");

        String preview = page.openPrintPreviewWithRandomTemplate();
        assertTrue(preview.contains(code), "The print preview does not show " + code + ".");

        page.savePreviewAsPdf();
        Path pdf = page.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
        log.info("PDF of {} saved: {} ({} bytes)", code, pdf.getFileName(), Downloads.sizeOf(pdf));
    }

    @Test(priority = 34, groups = {PQ, "excel"},
            description = "⋮ → Export to Excel: an .xlsx with the quotation's item lines is downloaded")
    public void userCanExportPriceQuotationToExcel() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        page.waitForGridToSettle();
        int lines = page.lineCount();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < lines; row++) {
            items.add(page.lineText(row, "code"));
        }
        Path dir = page.prepareDownloadFolder("excel");

        page.clickMenuItem(TestData.EXPORT_EXCEL_MENU_ITEM);

        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        int rows = Downloads.xlsxRowCount(xlsx);
        log.info("Excel of {}: {} ({} bytes), {} row(s); {} line(s): {}",
                code, xlsx.getFileName(), Downloads.sizeOf(xlsx), rows, lines, items);

        // As on orders: a title row and a header row, then one row per line.
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(rows - 2, lines, "Excel rows do not match the item lines of " + code + ".");
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Follow-up screens: Items Pricing ($), Print Barcode, Create Purchase Order
    // =====================================================================

    @Test(priority = 35, groups = {PQ, "follow-up"},
            description = "Items Pricing ($): Sales → Pricing opens with the quotation's items")
    public void userCanOpenItemsPricingFromPriceQuotation() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing ($) is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("code");

        page.clickItemsPricing();

        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> priced = page.gridColumnTexts("itemCode");
        log.info("Items Pricing from {}: {} → {} ({})", code, items, priced, url);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("/sales/pricing/add"), "Pricing screen did not open: " + url);
        softly.assertEquals(new HashSet<>(priced), new HashSet<>(items), "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 36, groups = {PQ, "follow-up", "print"},
            description = "Print Barcode: Item Barcodes opens with the quotation's items")
    public void userCanOpenPrintBarcodeFromPriceQuotation() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("itemId");

        page.clickPrintBarcode();

        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> barcodeItems = page.gridColumnTexts("itemId");
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("referenceCode=" + code), "Item Barcodes is not loaded from " + code + ": " + url);
        softly.assertEquals(barcodeItems, items, "Item Barcodes does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 37, groups = {PQ, "follow-up"},
            description = "Approved quotation → ⋮ → Create Purchase Order: a new order that refers to it, "
                    + "with its supplier, branch and items (not saved)")
    public void userCanCreatePurchaseOrderFromApprovedPriceQuotation() {

        Map<String, Object> record = openRandomQuotation(RecordStatus.APPROVED::of);
        page.waitForStatusLabel(APPROVED.label());
        page.waitForGridToSettle();
        String code = page.waitForCode();
        String supplier = waitForSelected(form, "vendorId");
        String branch = waitForSelected(form, "branchId");
        String warehouse = waitForSelected(form, "warehouseId");
        Set<String> items = new LinkedHashSet<>(page.gridColumnTexts("code"));
        log.info("Source {} (isInvoice {}): supplier [{}] branch [{}] warehouse [{}] items {}",
                code, record.get("isInvoice"), supplier, branch, warehouse, items);

        page.clickMenuItemAndFollow(TestData.CREATE_ORDER_MENU_ITEM, TestData.CREATE_ORDER_PATH);

        // QC 2026-11-02: /purchase/orders/add/<id>?typeName=PurchaseQutation
        DocumentForm order = new DocumentForm(driver, TestData.ORDER_PREFIX);
        assertEquals(waitUntilEquals(() -> page.inputValueById("referenceDocument"), code), code,
                "The new purchase order does not refer to " + code + ".");
        page.waitForGridToSettle();
        Set<String> orderItems = new LinkedHashSet<>(page.gridColumnTexts("code"));
        log.info("New order from {}: url {}, items {}", code, driver.getCurrentUrl(), orderItems);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains(TestData.CREATE_ORDER_PATH + record.get("id")),
                "The new order is not made from " + code + ": " + driver.getCurrentUrl());
        if (!supplier.isEmpty()) {
            softly.assertEquals(waitForSelected(order, "vendorId"), supplier,
                    "Supplier of the new order differs from " + code + ".");
        }
        if (!branch.isEmpty()) {
            softly.assertEquals(waitForSelected(order, "branchId"), branch,
                    "Branch of the new order differs from " + code + ".");
        }
        // QC 2026-11-02: the order made from PQ2609S26-000001 had an empty warehouse
        // while the quotation has one (also seen on orders, 2026-10-29).
        if (!warehouse.isEmpty() && !warehouse.equals("اختر")) {
            softly.assertEquals(waitForSelected(order, "warehouseId"), warehouse,
                    "Warehouse of the new order differs from " + code + ".");
        }
        softly.assertEquals(orderItems, items, "Items of the new order differ from " + code + ".");
        softly.assertAll();
        // Left unsaved on purpose: the test must not create orders in QC.
    }

    // =====================================================================
    // Attachments
    // =====================================================================

    @Test(priority = 38, groups = {PQ, "attachments"},
            description = "Attachments added to a Draft are listed and kept after save and reload")
    public void userCanAddAttachmentsToPriceQuotation() {

        openDraftQuotation();
        String code = page.waitForCode();
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

    @Test(priority = 39, groups = {PQ, "attachments"},
            description = "Deleting an attachment of a Draft is kept after save and reload")
    public void userCanDeleteAttachmentFromPriceQuotation() {

        openDraftQuotation();
        String code = page.waitForCode();
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
    // Exit, transaction log
    // =====================================================================

    @Test(priority = 40, groups = {PQ, "exit"},
            description = "Close a new, a viewed and an edited quotation: each returns to the list")
    public void userCanExitPriceQuotation() {

        // New, unsaved → asks for confirmation.
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing a new quotation did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing a new quotation");

        // Viewed → closes directly.
        openRandomQuotation(r -> true);
        String viewed = page.waitForCode();
        page.clickAction("xIcon");
        assertBackOnList("closing " + viewed + " in view mode");

        // Edited Draft → asks for confirmation.
        openDraftQuotation();
        String edited = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing " + edited + " in edit mode did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing " + edited + " in edit mode");
    }

    @Test(priority = 41, groups = {PQ, "log"},
            description = "Transaction log: first entry is the creation; an Approved quotation has an approval entry")
    public void userCanViewPriceQuotationTransactionLog() {

        Map<String, Object> record = openRandomQuotation(r -> true);
        String code = page.waitForCode();
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

    @Test(priority = 42, groups = {PQ, "lines"},
            description = "A new quotation keeps its lines (item, quantity, price) after save and reload")
    public void savedPriceQuotationKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        // This test checks that the lines are KEPT, so prices exclude tax
        // (agreed with the user, 2026-11-02). QC 2026-11-02: with "شاملة الضريبة"
        // and "ضريبة 25" a price of 100 came back as 125 after saving; tax
        // handling belongs to the calculation tests (level 3).
        assertEquals(page.selectPricesExcludingTax(), "prices-0", "\"باستثناء الضريبة\" could not be chosen.");
        addItemLine();
        page.deleteEmptyLines();
        List<String> before = linesSnapshot();
        log.info("Lines before save: {}", before);

        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        List<String> after = linesSnapshot();

        assertEquals(after.stream().sorted().toList(), before.stream().sorted().toList(),
                "Lines changed after save and reload.");
    }

    @Test(priority = 43, groups = {PQ, "lines", "edit"},
            description = "Draft quotation: changing the quantity and the price of an existing line is saved")
    public void userCanEditQuantityAndPriceOfExistingLine() {

        openDraftQuotation();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillZeroPricesOfLines();
        if (page.lineCount() == 0) {
            addItemLine();
        }

        String item = page.lineText(0, "code");
        BigDecimal quantity = page.lineNumber(0, "quantity").add(BigDecimal.ONE);
        BigDecimal price = page.lineNumber(0, "price").add(BigDecimal.TEN);
        page.setLineNumber(0, "quantity", quantity.toPlainString());
        page.setLineNumber(0, "price", price.toPlainString());
        page.stopCellEditing();
        log.info("Line 1 ({}) quantity → {}, price → {}", item, quantity, price);

        saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();

        int row = page.lineTexts("code").indexOf(item);
        assertTrue(row >= 0, "Item " + item + " is not in the lines after save.");
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineNumber(row, "quantity").compareTo(quantity), 0,
                "Quantity of " + item + " was not saved as " + quantity + " (found " + page.lineNumber(row, "quantity") + ").");
        softly.assertEquals(page.lineNumber(row, "price").compareTo(price), 0,
                "Price of " + item + " was not saved as " + price + " (found " + page.lineNumber(row, "price") + ").");
        softly.assertAll();
    }

    @Test(priority = 44, groups = {PQ, "lines"},
            description = "New quotation: deleting an item line is saved")
    public void userCanDeleteItemLineOfPriceQuotation() {

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

        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        assertEquals(page.lineCount(), before - 1, "The deleted line came back after save and reload.");
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Opens a random quotation matching {@code filter}, chosen through the
     * API ({@code PurchasQutations/filter}). The test is SKIPPED when QC has none.
     */
    private Map<String, Object> openRandomQuotation(Predicate<Map<String, Object>> filter) {

        Map<String, Object> record = page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no price quotation for this scenario."));
        page.waitForCode();
        return record;
    }

    /** Opens a random Draft from the list (page by page); when there is none, one is created first. */
    private void openDraftQuotation() {

        if (page.openRandomFromListWithStatus(DRAFT.label()).isPresent()) {
            page.waitForStatusLabel(DRAFT.label());
            return;
        }
        // The list said "no Draft": check with the API before creating one, so
        // a list-reading problem does not turn an edit test into create + edit
        // (QC 2026-11-02: the list had Drafts on page 1 but none was found).
        if (page.openRandomDocument(RecordStatus.DRAFT::of).isPresent()) {
            log.warn("The list showed no Draft, but the API has some → opened {} by its URL.", page.waitForCode());
            page.waitForStatusLabel(DRAFT.label());
            return;
        }
        log.info("No Draft quotation in QC → creating one.");
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        saveAndVerify(DRAFT);
    }

    /** Edit mode of a Draft or an Expired quotation (/edit/). */
    private void waitForEditMode() {
        waitUntil("edit mode", Duration.ofSeconds(15), () -> driver.getCurrentUrl().contains("/edit/"));
        form.waitUntilFieldEditable("note");
    }

    /** Waits (max 10 s) until the form shows a number other than {@code previous}; returns it. */
    private String waitUntilChanges(String previous) {
        long end = System.currentTimeMillis() + 10_000;
        String now = page.code();
        while ((now.isEmpty() || now.equals(previous)) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            now = page.code();
        }
        return now;
    }

    /**
     * New quotation from a requisition: {@code openForm} opens the empty
     * form (New / Add / Copy), then "المستند المرجعي" → random requisition →
     * download, the rest of the header (with "انزال من / الرقم"), an item
     * line and save.
     *
     * <p>A requisition that brings no line, or leaves the warehouse EMPTY and
     * LOCKED, cannot be used: the form is closed and another one is tried
     * (max {@link TestData#MAX_REFERENCE_ATTEMPTS}). QC 2026-11-02:
     * PR2608-000001 left branch and warehouse locked and empty, and then
     * "اضافة سطر" was not on the page (also seen on orders with
     * PR2608-000001 / PR2608-000015).</p>
     *
     * @return the number shown after saving.
     */
    private String createFromReferenceDocumentAndVerify(Runnable openForm) {

        String reference = null;
        List<String> rejected = new ArrayList<>();
        for (int attempt = 1; attempt <= TestData.MAX_REFERENCE_ATTEMPTS && reference == null; attempt++) {
            if (attempt > 1) {
                page.closeForm(); // discards the unusable form (confirms "leave without saving")
            }
            openForm.run();
            int before = form.gridRowCount();
            String chosen = page.selectRandomReferenceDocument();
            page.downloadReferenceDocument();
            int after = form.gridRowCount();
            // The download REPLACES the lines already in the grid (QC 2026-11-02: a copy
            // of PQ2609S7-000009 went from its own lines to the requisition's, fewer).
            log.info("المستند المرجعي {}: grid {} → {} line(s) ({}/{}).", chosen, before, after,
                    attempt, TestData.MAX_REFERENCE_ATTEMPTS);
            if (after == 0) {
                rejected.add(chosen + " (no lines)");
                continue;
            }
            if (!completeHeaderAfterReference()) {
                log.warn("{} leaves the warehouse empty and locked (branch [{}]) → another requisition.",
                        chosen, form.getSelectedText("branchId"));
                rejected.add(chosen + " (warehouse empty and locked)");
                continue;
            }
            reference = chosen;
        }
        if (reference == null) {
            fail("No usable requisition in \"المستند المرجعي\" after " + TestData.MAX_REFERENCE_ATTEMPTS
                    + " attempts: " + rejected);
        }
        if (!rejected.isEmpty()) {
            log.warn("Requisitions that could not be used as reference: {}", rejected);
        }

        fillDatesNumbersAndNote(true); // "انزال من / الرقم" too (asked by the user, 2026-11-02)
        addItemLine();
        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        String saved = page.inputValue("referenceDocument").trim();
        assertEquals(saved, reference, "\"المستند المرجعي\" of " + code + " was not kept.");
        String note = form.getNote();
        Map<String, Object> record = page.api().find(r -> note.equals(String.valueOf(r.get("note")))).stream()
                .findFirst().orElse(null);
        if (record != null) {
            assertEquals(String.valueOf(record.get("referenceDocument")), reference,
                    "The API does not show " + reference + " as the reference of " + QuotationPage.codeOf(record) + ".");
        }
        return code;
    }

    /**
     * After downloading a requisition: fields it filled are KEPT; empty ones
     * are filled in screen order (supplier, currency, branch, warehouse,
     * cost centre, tax method, prices).
     *
     * @return false when the warehouse is still empty (locked by the requisition).
     */
    private boolean completeHeaderAfterReference() {

        editing = false;
        if (form.getSelectedText("vendorId").isEmpty()) {
            selectIfEditable("vendorId");
        }
        for (String field : List.of("currencyId", "branchId")) {
            if (form.getSelectedText(field).isEmpty()) {
                selectIfEditable(field);
            }
        }
        if (isEmptySelect("warehouseId")) {
            fillBranchAndWarehouse(form.getSelectedText("branchId").isEmpty());
            if (isEmptySelect("warehouseId")) {
                return false;
            }
        }
        for (String field : List.of("costCenterId", "taxCalculationMethod")) {
            if (form.getSelectedText(field).isEmpty()) {
                selectIfEditable(field);
            }
        }
        if (page.priceOption().isEmpty()) {
            page.selectRandomPriceOption();
        }
        return true;
    }

    /**
     * Fills a new quotation's header in the SAME ORDER as the screen (right
     * to left, row by row, QC 2026-11-02):
     * المورد → العملة → الفرع → المخزن → مركز التكلفة → طريقة احتساب الضريبة →
     * الأسعار → التاريخ → تاريخ الانتهاء → اخر موعد للاستلام → مندوب المشتريات →
     * رقم عرض المورد → انزال من / الرقم (new quotation only) → البيان.
     * "مده العرض" is not changed (asked by the user, 2026-11-02).
     * Left as they are: المستند المرجعي (its own scenarios), الرقم الضريبي and
     * التعادل (read-only, filled from the supplier / currency), تاريخ الاعتماد.
     */
    private void fillHeader() {
        editing = false;
        fillHeaderInScreenOrder();
    }

    /**
     * True while changing a saved quotation: every field gets a value
     * DIFFERENT from the saved one, so the edit really changes all of them.
     */
    private boolean editing;

    /** Edit mode: same order; only editable fields (QC 2026-11-02: التاريخ and انزال من are not). */
    private void changeHeader() {
        editing = true;
        try {
            fillHeaderInScreenOrder();
        } finally {
            editing = false;
        }
    }

    private void fillHeaderInScreenOrder() {

        selectSupplierUntilWarehouseIsFilled();
        selectIfEditable("costCenterId");
        selectIfEditable("taxCalculationMethod");
        if (editing) {
            page.selectOtherPriceOption();
        } else {
            page.selectRandomPriceOption();
        }
        fillDatesNumbersAndNote(true);
    }

    /**
     * التاريخ → تاريخ الانتهاء → اخر موعد للاستلام → مندوب المشتريات →
     * رقم عرض المورد → مده العرض → (انزال من) → البيان.
     */
    private void fillDatesNumbersAndNote(boolean downloadLines) {

        LocalDate today = LocalDate.now();
        // Every date the test sets is in the FUTURE (asked by the user, 2026-11-02).
        // "التاريخ" is locked in edit mode → kept.
        page.setQuotationDate(changed("quotationDate", today.plusDays(TestData.QUOTATION_DATE_IN_DAYS)));
        page.setQuotationDeadline(changed("quotationDeadLine", today.plusDays(TestData.DEADLINE_IN_DAYS)));
        page.setReceivedDate(changed("receivedDate", today.plusDays(TestData.RECEIVED_IN_DAYS)));
        selectIfEditable("purchaseRepresentativeId");
        String vendorNumber = randomUnusedVendorQuotationNumber();
        if (page.setVendorQuotationNumber(vendorNumber)) { // false when the field is locked
            assertEquals(page.inputValue("vendorQuotationNumber").trim(), vendorNumber,
                    "\"رقم عرض المورد\" was not filled.");
        }
        // "مده العرض" is left as it is (asked by the user, 2026-11-02).
        // "انزال من / الرقم" exists only while adding (QC 2026-11-02: not on /edit/).
        if (downloadLines && !editing && page.hasField("downloadFrom", Duration.ofSeconds(2))) {
            downloadLinesFromRandomDocument();
        }
        String note = TestData.uniqueNote();
        form.setNote(note);
        assertEquals(form.waitForNote(note), note, "\"البيان\" was not filled.");
        log.info("note = {}", note);
    }

    /** Numbers given out in this run (several quotations are saved per run). */
    private static final Set<String> usedVendorQuotationNumbers = new HashSet<>();

    /**
     * "رقم عرض المورد": "VQ-yyyyMMdd-HHmmss" (the earlier format, asked by the
     * user, 2026-11-02) that no quotation in QC has yet (read through the API)
     * and that was not given out in this run; "-2", "-3"… is added if taken.
     */
    private String randomUnusedVendorQuotationNumber() {
        Set<String> taken = new HashSet<>(usedVendorQuotationNumbers);
        page.api().findAll().forEach(r -> taken.add(String.valueOf(r.get("vendorQuotationNumber")).trim()));
        String base = TestData.vendorQuotationNumber();
        String number = base;
        for (int n = 2; taken.contains(number); n++) {
            number = base + "-" + n;
        }
        usedVendorQuotationNumbers.add(number);
        return number;
    }

    /** {@code wanted}, or one day later when editing and the field already holds it. */
    private LocalDate changed(String dateField, LocalDate wanted) {
        return editing && wanted.equals(page.dateValue(dateField)) ? wanted.plusDays(1) : wanted;
    }

    /** انزال من (random screen) → "الرقم" → random document → download its lines. */
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
     * المورد → العملة → الفرع → المخزن, in screen order. When the warehouse is
     * still EMPTY, ANOTHER supplier is chosen, then ANOTHER branch, then the
     * warehouse again, up to {@link TestData#MAX_SUPPLIER_ATTEMPTS} times. The
     * currency keeps the value of the first selection (put back if the app resets it).
     */
    private void selectSupplierUntilWarehouseIsFilled() {

        List<String> between = List.of("currencyId", "branchId");
        Map<String, String> firstChoice = new LinkedHashMap<>();

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

            if (waitForWarehouseSelectable()) {
                if (editing) {
                    page.selectRandomOtherOption("warehouseId");
                } else {
                    page.selectRandomOption("warehouseId");
                }
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }

            String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(3));
            if (warehouse.isEmpty() || warehouse.equals("اختر")) {
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
     * Adds one item line: random item, quantity and price. Lines already in
     * the grid (downloaded / copied) are completed first. An item the
     * quotation does not accept never loads its unit; another item is tried.
     * Filled: الصنف, الكمية, السعر and البيان. The "البيان" column is not in
     * view when the line is added (the grid renders only the columns in
     * view), so it is found by its header and scrolled to (asked by the user,
     * 2026-11-02: the line note was left empty).
     */
    private void addItemLine() {

        fillZeroPricesOfLines();
        page.deleteEmptyLines();
        int row = page.addLine();

        for (int attempt = 1; attempt <= MAX_ITEM_ATTEMPTS; attempt++) {
            String item = page.selectRandomItem(row);
            if (page.lineText(row, "uomId").isEmpty()) {
                form.closeEmptyItemSearchDialog();
                log.warn("Item {} did not load into line {} → another item ({}/{}).",
                        item, row + 1, attempt, MAX_ITEM_ATTEMPTS);
                continue;
            }
            String code = page.lineText(row, "code");
            if (isItemOnAnotherLine(code, row)) {
                log.warn("Item {} ({}) is already on another line → another item ({}/{}).",
                        item, code, attempt, MAX_ITEM_ATTEMPTS);
                continue;
            }
            page.setLineNumber(row, "quantity", TestData.QUANTITY);
            page.setLineNumber(row, "price", TestData.PRICE);
            String noteColumn = page.setLineNote(row, TestData.LINE_NOTE);
            page.stopCellEditing();
            assertFalse(noteColumn.isEmpty(), "The lines grid has no \"البيان\" column to fill.");
            assertEquals(page.lineText(row, noteColumn), TestData.LINE_NOTE,
                    "\"البيان\" of line " + (row + 1) + " was not filled.");
            log.info("Line {}: {} × {} @ {}, note [{}] (col-id {})", row + 1, item, TestData.QUANTITY,
                    TestData.PRICE, TestData.LINE_NOTE, noteColumn);
            return;
        }
        fail("No accepted item found for the new line after " + MAX_ITEM_ATTEMPTS + " attempts.");
    }

    /**
     * Completes the lines already in the grid (downloaded, copied, imported),
     * from the last to the first: a repeated item is removed; an empty / 0
     * quantity gets {@link TestData#QUANTITY}; a 0 price gets {@link TestData#PRICE}.
     */
    private void fillZeroPricesOfLines() {

        page.waitForGridToSettle();
        int rows = form.gridRowCount();
        Map<String, Integer> firstRowOfItem = new java.util.HashMap<>();
        for (int row = 0; row < rows; row++) {
            firstRowOfItem.putIfAbsent(page.lineText(row, "code"), row);
        }

        for (int row = rows - 1; row >= 0; row--) {
            String code = page.lineText(row, "code");
            if (!code.isEmpty() && firstRowOfItem.get(code) != row) {
                log.info("Line {} repeats item {} → removing it.", row + 1, code);
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

    /**
     * Header of the opened (saved) quotation, read once the form has finished
     * loading: the warehouse can be filled a moment after the other fields,
     * so it waits until two reads in a row are the same (max 10 s).
     */
    private Map<String, String> savedHeaderSnapshot() {
        Map<String, String> previous = page.readHeaderValues(TestData.UNDO_HEADER_FIELDS);
        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            pause(500);
            Map<String, String> now = page.readHeaderValues(TestData.UNDO_HEADER_FIELDS);
            if (now.equals(previous)) {
                return now;
            }
            previous = now;
        }
        log.warn("Header kept changing for 10 s: {}", previous);
        return previous;
    }

    /** The selected text of {@code field}, once loaded (max 10 s). */
    private String waitForSelected(DocumentForm on, String field) {
        long end = System.currentTimeMillis() + 10_000;
        String value = on.getSelectedText(field);
        while (value.isEmpty() && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            value = on.getSelectedText(field);
        }
        return value;
    }

    /** The form is closed and the list is shown again. */
    private void assertBackOnList(String afterWhat) {
        waitUntil("the list after " + afterWhat, Duration.ofSeconds(15), () -> !page.isFormOpen());
        page.waitForListLoaded();
    }

    /** "اعتماد" / "عدم اعتماد" of the opened quotation, then "موافق". */
    private void toggleApprovalOfOpenedQuotation() {
        assertTrue(page.waitForActionEnabled("approveOrDisapprove"),
                "\"اعتماد\" is disabled on " + page.code() + ".");
        page.clickApproveOrDisapprove();
        page.confirmDialog();
    }

    /** "إلغاء" then "موافق"; returns the toast and the API answer. */
    private ActionOutcome cancelOpenedQuotation() {

        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("cancel"), "\"إلغاء\" is disabled on " + code + ".");
        return performAndRecord(form, TestData.API_CANCEL, () -> {
            page.clickCancelDocument();
            page.confirmDialog();
        });
    }

    private void verifyCancelled(String code) {

        page.waitForStatusLabel(CANCELLED.label());
        page.reload();
        page.waitForStatusLabel(CANCELLED.label()); // kept on the server

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action),
                    "'" + action + "' must be disabled on cancelled quotation " + code + ".");
        }
        softly.assertAll();
    }

    /**
     * The server refused because the quotation is linked to other
     * operations. It must still be Approved after a reload. Its API flags are
     * logged so the flag that shows the link can be added to
     * {@link QuotationPage#isUnlinked}.
     */
    private void skipLinked(String code, Map<String, Object> record, int attempt) {

        page.reload();
        page.waitForStatusLabel(APPROVED.label()); // a refused action must change nothing
        Map<String, Object> flags = new java.util.TreeMap<>();
        record.forEach((key, value) -> {
            if (value instanceof Boolean || value instanceof Number) {
                flags.put(key, value);
            }
        });
        log.warn("{} is linked to other operations (server refused) → another one ({}/{}). API flags: {}",
                code, attempt, TestData.MAX_LINKED_ATTEMPTS, flags);
        page.closeForm();
        page.waitForListLoaded();
    }

    /**
     * Saves and verifies the RESULT:
     * success toast → view mode → number → description persisted → status.
     *
     * @return the saved quotation's number.
     */
    private String saveAndVerify(DocumentStatus... allowedStatuses) {

        String note = form.getNote();
        if (!note.startsWith(TestData.NOTE_PREFIX)) {
            note = TestData.uniqueNote();
            form.setNote(note);
        }

        page.stopCellEditing();
        page.deleteEmptyLines();
        // Some saved quotations have no warehouse (QC 2026-11-02: PQ2608-000008
        // shows "اختر"). Required fields are filled before saving.
        fillRequiredFields(TestData.REQUIRED_HEADER_FIELDS.stream()
                .filter(f -> isEmptySelect(f) && page.waitForSelectEnabled(f, Duration.ofSeconds(1)))
                .toList());
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.startRecordingSaveRequest();
        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog();

        DocumentForm.Toast toast = form.waitForToast(); // read first: it disappears after ~2–3 s
        // Attachments are uploaded WITH the save; their toast can be stacked with the save toast.
        boolean uploadOnly = false;
        if (isUploadToast(toast)) {
            List<String> others = page.otherToastTexts("تمت إضافة الملفات", Duration.ofSeconds(8));
            log.info("Upload toast [{}] read during the save; other toasts: {}", toast.fullText().replace('\n', ' '), others);
            assertTrue(others.stream().noneMatch(t -> t.contains("خطأ")),
                    "Save failed on the server: " + others + " | lines sent: " + page.lastSavedLines());
            uploadOnly = others.stream().noneMatch(t -> t.contains(TestData.SAVE_SUCCESS_MESSAGE));
        }
        boolean warning = isWarning(toast) || uploadOnly || isUploadToast(toast);
        if (warning) {
            log.warn("Save shows a warning (not blocking): {}", toast);
        } else {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast
                    + " | lines sent: " + page.lastSavedLines());
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = page.waitForCode();

        SoftAssert softly = new SoftAssert();
        if (!warning) {
            softly.assertTrue(toast.fullText().contains(TestData.SAVE_SUCCESS_MESSAGE),
                    "Unexpected success message: " + toast);
        }
        String shownNote = form.waitForNote(note);
        String status = page.statusLabel();
        if (!note.equals(shownNote) || !isOneOf(status, allowedStatuses)) {
            // QC 2026-11-02 (PQ2609S6-000007): after a success toast the form
            // showed the OLD description and "منتهي". The saved record is read
            // from the API, and the form is reloaded, so the report tells a
            // screen that was not refreshed from a save that did not happen.
            String id = driver.getCurrentUrl().replaceAll(".*/view/(\\d+).*", "$1");
            Map<String, Object> record = page.api().find(r -> id.equals(String.valueOf(r.get("id")))).stream()
                    .findFirst().orElse(null);
            log.warn("After saving {}: form note [{}] status [{}]; API record: note [{}] recordStatus [{}] "
                            + "vendorQuotationNumber [{}] duration [{}] quotationDate [{}]", code, shownNote, status,
                    record == null ? "?" : record.get("note"), record == null ? "?" : record.get("recordStatus"),
                    record == null ? "?" : record.get("vendorQuotationNumber"), record == null ? "?" : record.get("duration"),
                    record == null ? "?" : record.get("quotationDate"));
            page.reload();
            shownNote = form.waitForNote(note);
            status = page.statusLabel();
            log.warn("After a reload: note [{}], status [{}].", shownNote, status);
        }
        softly.assertEquals(shownNote, note, "Description was not persisted in " + code + ".");
        softly.assertTrue(isOneOf(status, allowedStatuses),
                "Unexpected status [" + status + "] after saving " + code
                        + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();

        log.info("Saved Price Quotation {} with status {}", code, status);
        return code;
    }

    /**
     * "إضافة" / "تكرار" must create a NEW quotation and leave the source as
     * it was. The saved quotation is found in the API by its unique
     * description (not by the number on the screen).
     *
     * <p>QC 2026-11-02: after saving a copy of PQ2608-000005 and of
     * PQ2609S6-000003 the form still showed the SOURCE number, while the
     * server had created a new quotation (PQ2611S43-000001) and the source
     * was unchanged. The correct number shows a moment later, so that is
     * logged and reported as a low-priority screen issue; the test fails only
     * when the form never shows the new number.</p>
     *
     * @return the number of the new quotation (from the API).
     */
    private String verifyNewQuotationSaved(Map<String, Object> source, String shownCode, String how) {

        String note = form.getNote();
        String sourceCode = QuotationPage.codeOf(source);
        List<Map<String, Object>> all = page.api().findAll();
        Map<String, Object> saved = all.stream()
                .filter(r -> note.equals(String.valueOf(r.get("note")))).findFirst().orElse(null);
        Map<String, Object> sourceNow = all.stream()
                .filter(r -> String.valueOf(source.get("id")).equals(String.valueOf(r.get("id")))).findFirst().orElse(null);
        assertTrue(saved != null, "No quotation with the description [" + note + "] in the API after " + how + ".");
        String newCode = QuotationPage.codeOf(saved);
        log.info("{}: source {} (id {}) → new {} (id {}); the form shows {}.", how, sourceCode, source.get("id"),
                newCode, saved.get("id"), shownCode);

        SoftAssert softly = new SoftAssert();
        softly.assertNotEquals(newCode, sourceCode, how + " saved over the source quotation " + sourceCode + ".");
        if (sourceNow != null) {
            softly.assertEquals(sourceNow.get("recordStatus"), source.get("recordStatus"),
                    "The status of the source " + sourceCode + " changed after " + how + ".");
            softly.assertEquals(String.valueOf(sourceNow.get("note")), String.valueOf(source.get("note")),
                    "The description of the source " + sourceCode + " changed after " + how + ".");
        }
        if (!shownCode.equals(newCode)) {
            // Agreed with the user (2026-11-02): the source number may show for a
            // moment after saving; the test passes when the form then shows the
            // new number by itself (no reload). It fails only if it never does.
            String settled = waitUntilEquals(page::code, newCode);
            if (settled.equals(newCode)) {
                log.warn("Screen showed the source number {} for a moment after {}, then {} (reported as a "
                        + "low-priority screen issue).", shownCode, how, newCode);
            } else {
                page.reload();
                String afterReload = page.waitForCode();
                softly.fail("SCREEN ISSUE: after " + how + " the form kept showing " + settled
                        + " instead of the new quotation " + newCode + " (after a reload it shows " + afterReload
                        + ", url " + driver.getCurrentUrl() + ").");
            }
        }
        softly.assertAll();
        return newCode;
    }

    private static boolean isOneOf(String statusLabel, DocumentStatus... allowed) {
        return Arrays.stream(DocumentStatus.values()).anyMatch(s -> s.label().equals(statusLabel))
                && DocumentStatus.fromLabel(statusLabel).isOneOf(allowed);
    }

    /** Empty select; "اختر" is the placeholder shown when nothing is chosen. */
    private boolean isEmptySelect(String field) {
        String text = form.getSelectedText(field);
        return text.isEmpty() || text.equals("اختر");
    }

    /** The attachment upload toast ("تمت إضافة الملفات بنجاح" / "تم الرفع بنجاح"). */
    private static boolean isUploadToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.contains("تمت إضافة الملفات") || text.contains("تم الرفع");
    }

    /** A warning toast ("تنبيه" / "تحذير") does not block the save. */
    private static boolean isWarning(DocumentForm.Toast toast) {
        String title = toast.title() == null ? "" : toast.title();
        return title.contains("تنبيه") || title.contains("تحذير");
    }

    /**
     * "حفظ" opens a confirmation only when the form is valid. Otherwise the
     * reason is given (toast / required fields) instead of a timeout. Missing
     * required fields are filled and the save is clicked again.
     */
    private void failIfSaveBlockedBeforeConfirmation() {

        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                java.util.Optional<DocumentForm.Toast> toast = form.waitForConfirmationDialogOrToast();
                if (toast.isPresent() && isWarning(toast.get())) {
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
                    log.warn("Save blocked by required fields {} → filling them and saving again.", errors);
                    fillRequiredFields(errors);
                } else {
                    log.warn("Save click showed nothing; clicking again.");
                }
                page.clickSave();
            }
        }
    }

    /** Fills empty required header fields in screen order (branch / warehouse together). */
    private void fillRequiredFields(List<String> fields) {
        if (fields.contains("prices") && page.priceOption().isEmpty()) {
            page.selectRandomPriceOption();
        }
        boolean warehouseNeeded = fields.contains("warehouseId")
                || (fields.contains("branchId") && isEmptySelect("warehouseId"));
        for (String field : fields) {
            if (List.of("branchId", "warehouseId", "prices").contains(field)) {
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
        if (fields.contains("branchId") || warehouseNeeded) {
            fillBranchAndWarehouse(fields.contains("branchId"));
        }
    }

    /** Branch (when empty or without warehouses) then a warehouse that exists for it. */
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
            if (!isEmptySelect("warehouseId")) {
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
