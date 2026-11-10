package com.erp.tests;

import com.erp.automation.base.PurchasingBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.PurchaseOrderPage;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.Downloads;
import org.openqa.selenium.By;
import org.openqa.selenium.TimeoutException;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.awt.AWTException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.CANCELLED;
import static com.erp.automation.data.DocumentStatus.COMPLETED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Purchase Order (أوامر الشراء) end-to-end scenarios.
 *
 * <p><b>Reference layout for every Purchasing test class:</b></p>
 * <ol>
 *     <li>Extend {@link PurchasingBaseTest}; open the screen in {@link #openScreen}.
 *         Login and menu navigation are never repeated inside a test.</li>
 *     <li>Test data lives in {@link TestData} — no magic values inside tests.</li>
 *     <li>Each {@code @Test} reads like the business scenario: a short list of
 *         named steps. The steps are private methods in the "Reusable steps"
 *         section at the bottom.</li>
 *     <li>Scenarios that depend on a random record's status use
 *         {@link #requireStatus} → the test is reported as SKIPPED, not PASSED,
 *         when the record does not fit.</li>
 *     <li>Every action ends with a check of its RESULT, not just the clicks:
 *         success toast, persisted values, new status, button states
 *         (read through {@link DocumentForm}).</li>
 *     <li>Logging goes through {@code log}, never {@code System.out}.</li>
 * </ol>
 */
public class PurchaseOrderTest extends PurchasingBaseTest {

    private static final String PO = "purchase-order";

    private PurchaseOrderPage purchaseOrderPage;
    private DocumentForm form;

    @Override
    protected void openScreen(PurchasingPage purchasingPage) {
        purchasingPage.clickPurchaseOrders();
        purchaseOrderPage = new PurchaseOrderPage(driver);
        form = new DocumentForm(driver, "orders");
        // The "تثبيت Primex ERP" pop-up covers the bottom corner (pager buttons,
        // last grid rows). Close it if it is already shown; clickWhenReady()
        // closes it too when it appears later and blocks a click.
        purchaseOrderPage.dismissInstallPrompt();
    }

    // =====================================================================
    // Test data
    // =====================================================================

    private static final class TestData {


        /** API paths recorded to judge an action by the server's answer. */
        static final String API_CANCEL = "/cancel/";
        static final String API_APPROVE_OR_DISAPPROVE = "/approveOrDisApprove/";

        /** Status shown on the Kanban card of a disapproved order (QC, 2026-10-31). */
        static final String DISAPPROVED_CARD_LABEL = "غير معتمد";

        /** "تجميع حسب" option for the status column. */
        static final String GROUP_BY_STATUS = "الحالة";

        /** Title row of the PO Excel export (QC, 2026-10-31). */
        static final String EXCEL_EXPORT_TITLE = "تصدير الاصناف";

        /** Error toast when cancelling an approved PO that has a Receipt Order (QC, 2026-10-31). */
        static final String CANCEL_LINKED_TO_RECEIPT_ERROR = "لايمكن الغاء امر شراء مرتبط";
        static final String QUANTITY = "17";
        static final String PRICE = "100";
        static final String LINE_DISCOUNT_RATE = "20";
        static final String LINE_DISCOUNT_VALUE = "20";
        static final String LINE_EXTRA_RATE = "20";
        static final String LINE_EXTRA_VALUE = "20";
        static final int FREE_QUANTITY = 114;
        static final String LINE_NOTE = "بيان أمر شراء تجريبي";

        static final String SECTION_DISCOUNT_RATE = "20";
        static final double SECTION_DISCOUNT_VALUE = 20;
        static final String SECTION_ADDITION_RATE = "20";
        static final String SECTION_ADDITION_VALUE = "20";
        static final String SECTION_NOTE = "بيان أمر شراء تجريبي";

        static final String ATTACHMENT_TITLE = "مرفقات أمر شراء تجريبي";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات أمر الشراء";

        /**
         * A newly saved order is a Draft, or Approved when it is auto-approved on
         * save (seen in QC on 2026-10-26: PO2610T4-000102, created from a Price
         * Quotation, was "معتمد" right after saving). It must never be anything else.
         */
        static final DocumentStatus[] NEW_ORDER_STATUSES = {DRAFT, APPROVED};

        /** Price given to lines that are loaded from a reference document with price 0. */
        static final String FALLBACK_PRICE = "100";

        static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";
        static final String AT_LEAST_ONE_ITEM_MESSAGE = "يجب اضافة صنف واحد على الأقل";

        /** Mandatory header fields (data-testid without "orders-"). */
        static final List<String> REQUIRED_HEADER_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "branchId", "warehouseId",
                "purchaseRepresentativeId", "vendorInvoiceCode", "tagId");

        /** Every editable header field of the form. */
        static final List<String> HEADER_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "branchId", "warehouseId",
                "currencyId", "costCenterId", "taxCalculationMethode", "orderDate",
                "quotationtDeadline", "purchaseRepresentativeId", "vendorInvoiceCode",
                "itemsCostsMethode", "bulkDiscountType", "bulkDiscountAmount",
                "tagId", "note");

        /** Toolbar buttons (button ids) that must be DISABLED on a new, unsaved order (QC, 2026-10-29). */
        static final List<String> ACTIONS_DISABLED_ON_NEW_ORDER = List.of(
                "copy", "add", "undo", "delete", "print", "itemsPricing",
                "approveOrDisapprove", "cancel", "transactionLogs");

        /** Toolbar buttons that must be ENABLED on a new, unsaved order. */
        static final List<String> ACTIONS_ENABLED_ON_NEW_ORDER = List.of(
                "save", "uploadAttachment", "recordSearch", "clipboardBar", "xIcon");

        /** Toolbar buttons that must be ENABLED on a Draft order (view mode). */
        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "copy", "add", "edit", "delete", "print",
                "approveOrDisapprove", "cancel", "transactionLogs");

        /** "⋮" menu item that creates a Receipt Order from an Approved order. */
        static final String CREATE_RECEIPT_ORDER_MENU_ITEM = "انشاء امر استلام";

        /** Action names in "سجل العمليات". */
        static final String LOG_CREATED = "انشاء العملية";
        static final String LOG_APPROVED = "اعتماد العملية";

        /** data-testid prefix of the Receipt Order screen (spelling as in the app). */
        static final String RECEIPT_ORDER_PREFIX = "recieve-orders";

        /** Query parameters the order passes to the screens it opens. */
        static final String ITEMS_PRICING_TYPE_PARAM = "downOpType=4";

        /** Lines tabs of the order (ids end with "-trigger-<tab>"). */
        static final String ITEMS_TAB = "details";
        static final String DISCOUNTS_TAB = "discountsAndAdditions";

        private static final DateTimeFormatter STAMP =
                DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        /** Unique description, so a persisted value can be told apart from old data. */
        static String uniqueNote() {
            return "أمر شراء آلي " + LocalDateTime.now().format(STAMP);
        }

        private TestData() {
        }
    }

    // =====================================================================
    // Create — without reference document
    // =====================================================================

    @Test(priority = 1, groups = {PO, "create"},
            description = "New PO from the New button, without a reference document")
    public void userCanCreateNewPurchaseOrderWithoutReferenceDocument() {

        purchaseOrderPage.clickNew();

        fillManualOrder(PartySelectionFlow.NEW_DOCUMENT, HeaderOptions.NEW_ORDER);
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 2, groups = {PO, "create"},
            description = "Copy an existing PO, then re-fill it without a reference document")
    public void userCanDuplicatePurchaseOrderWithoutReferenceDocument()
            throws InterruptedException {

        purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
        purchaseOrderPage.clickCopy(); // no confirmation step for Copy

        fillManualOrder(PartySelectionFlow.COPIED_DOCUMENT, HeaderOptions.EXISTING_LINES);
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 3, groups = {PO, "create"},
            description = "New PO from the Add button of an opened PO, without a reference document")
    public void userCanAddPurchaseOrderWithoutReferenceDocument()
            throws InterruptedException {

        purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
        purchaseOrderPage.clickAddPurchaseOrder();

        fillManualOrder(PartySelectionFlow.NEW_DOCUMENT, HeaderOptions.NEW_ORDER);
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    // =====================================================================
    // Create — with reference document
    // =====================================================================

    @Test(priority = 4, groups = {PO, "create", "reference"},
            description = "New PO loaded from a reference document")
    public void userCanCreateNewPurchaseOrderWithReferenceDocument() {

        loadReferenceHavingWarehouse(() -> {
            purchaseOrderPage.clickNew();
            form.waitForMode(DocumentForm.Mode.ADD);
            loadFromReferenceDocumentGrid();
        });
        fillOrderLoadedFromReference();
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 5, groups = {PO, "create", "reference"},
            description = "Add PO (from an opened PO) loaded from a reference document")
    public void userCanAddPurchaseOrderWithReferenceDocument()
            throws InterruptedException {

        loadReferenceHavingWarehouse(() -> {
            purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
            clickAddUntilNewFormOpens();
            loadFromReferenceDocumentGrid();
        });
        fillOrderLoadedFromReference();
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 6, groups = {PO, "create", "reference"},
            description = "Copy an existing PO, then load it from a reference document")
    public void userCanDuplicatePurchaseOrderWithReferenceDocument()
            throws InterruptedException {

        loadReferenceHavingWarehouse(() -> {
            purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
            purchaseOrderPage.clickCopy();
            waitForCopiedOrder();
            loadFromReferenceDocumentGrid();
        });
        fillOrderLoadedFromReference();
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 16, groups = {PO, "create", "reference"},
            description = "New PO continued from a Purchase Requisition")
    public void userCanCreateNewPurchaseOrderFromPurchaseRequisition() {

        loadReferenceHavingWarehouse(() -> {
            purchaseOrderPage.clickNew();
            loadFromClipboardSource(purchaseOrderPage::clickPurchaseRequestMenuItem);
        });
        fillOrderLoadedFromReference();
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 17, groups = {PO, "create", "reference"},
            description = "New PO continued from a Price Quotation")
    public void userCanCreateNewPurchaseOrderFromPriceQuotation() {

        loadReferenceHavingWarehouse(() -> {
            purchaseOrderPage.clickNew();
            loadFromClipboardSource(purchaseOrderPage::clickContinueFromQuotationsMenuItem);
        });
        fillOrderLoadedFromReference();
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    @Test(priority = 18, groups = {PO, "create", "excel"},
            description = "New PO imported from Excel")
    public void userCanCreateNewPurchaseOrderFromExcel() {

        purchaseOrderPage.clickNew();
        purchaseOrderPage.clickClipboardBarButton();
        purchaseOrderPage.clickImportFromExcelMenuItem();
        purchaseOrderPage.uploadExcelFile();
        purchaseOrderPage.clickImportExcelConfirmButton();   // waits until the import finishes
        verifyExcelLinesImported();

        fillManualOrder(PartySelectionFlow.IMPORTED_FROM_EXCEL, HeaderOptions.IMPORTED_LINES);
        saveAndVerify(TestData.NEW_ORDER_STATUSES);
    }

    /** The import must have added lines; otherwise the rest of the test is meaningless. */
    private void verifyExcelLinesImported() {

        int lines = form.gridRowCount();
        log.info("Lines imported from Excel: {}", lines);
        assertTrue(lines > 0, "Excel import finished but no lines were added to the grid.");
    }

    // =====================================================================
    // Edit / Undo
    // =====================================================================

    @Test(priority = 7, groups = {PO, "edit"},
            description = "Edit a PO: Approved → description only; Draft/Completed → full edit")
    public void userCanEditPurchaseOrderBasedOnStatus()
            throws InterruptedException {

        DocumentStatus status = openRandomPurchaseOrder();
        requireStatus(status, APPROVED, DRAFT, COMPLETED);

        editOpenedOrderAccordingTo(status);
        saveAndVerify(status); // editing must not change the status
    }

    @Test(priority = 15, groups = {PO, "edit"},
            description = "Edit a PO then Undo the changes")
    public void userCanUndoPurchaseOrder()
            throws InterruptedException {

        // A Draft: full edit, then undo. QC, 2026-10-31 (mvn run): on an Approved
        // order (only the description is editable) "تراجع" stayed disabled after
        // the description was changed, so the test timed out there. Asked the
        // analysis team whether undo should work in that mode.
        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        form.waitForStatusLabel(DRAFT.label());
        DocumentStatus status = DRAFT;

        String originalCode = form.waitForCode();
        String originalNote = form.getNote();
        String originalVendor = form.getSelectedText("vendorId");

        editOpenedOrderAccordingTo(status);
        form.setNote(TestData.uniqueNote()); // guarantees at least one real change
        undoAndConfirm();

        // QC, 2026-10-31, reproduced by hand on PO2610T47-000019 (Draft): edit →
        // change "البيان" → تراجع → موافق leaves "البيان" EMPTY (the saved value is
        // still in the database). Reported; this test fails on it.
        // QC, 2026-10-31: after undo on PO2610T47-000019 the form showed
        // PO2610T47-000020 (record 305 = the last one) with an empty description.
        // Check first that undo stays on the same order, so a wrong record is
        // reported as such and not as "description not restored".
        String codeAfterUndo = waitUntilEquals(form::getCode, originalCode);
        assertEquals(codeAfterUndo, originalCode,
                "Undo on " + originalCode + " opened another order: " + codeAfterUndo + ".");

        // Undo reloads the saved order: right after the confirmation the
        // fields are briefly empty (QC, 2026-10-28: description read as ""),
        // so wait for the original values instead of reading once.
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(form.waitForNote(originalNote), originalNote,
                "Undo did not restore the description.");
        softly.assertEquals(waitUntilEquals(() -> form.getSelectedText("vendorId"), originalVendor),
                originalVendor, "Undo did not restore the supplier.");
        softly.assertAll();
    }

    // =====================================================================
    // Status actions
    // =====================================================================

    @Test(priority = 8, groups = {PO, "status"},
            description = "Delete a random Draft PO: success message, and the order is no longer in the list")
    public void userCanDeleteRandomDraftPurchaseOrder()
            throws InterruptedException {

        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        form.waitForStatusLabel(DRAFT.label());
        String code = form.waitForCode();
        waitUntilActionEnabled("delete");

        purchaseOrderPage.deletePurchaseOrder();
        purchaseOrderPage.confirmDeletePurchaseOrder();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);
        purchaseOrderPage.waitForToastsToClear();

        // The deleted order must not be found any more (was test 34).
        closeOpenForms();
        purchaseOrderPage.searchPurchaseOrdersList(code);
        assertFalse(purchaseOrderPage.waitUntilListShows(code),
                code + " is still listed after deletion: " + purchaseOrderPage.getVisiblePurchaseOrderCodes());
    }

    @Test(priority = 9, groups = {PO, "status"},
            description = "Approve a random Draft PO")
    public void userCanApproveRandomDraftPurchaseOrder()
            throws InterruptedException {

        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        toggleApprovalOfOpenedOrder();

        form.waitForStatusLabel(APPROVED.label());
        assertFalse(form.isActionEnabled("delete"),
                "An approved Purchase Order must not be deletable.");
    }

    @Test(priority = 10, groups = {PO, "status", "log"},
            description = "Disapprove an Approved PO (no receipt): back to Draft, draft actions enabled, new log entry")
    public void userCanDisapproveRandomApprovedPurchaseOrder()
            throws InterruptedException {

        // QC, 2026-10-31: like cancel, an approved PO linked to a Receipt Order
        // cannot be disapproved: PUT approveOrDisApprove/{id}/false gives 400
        // "لايمكن الغاء امر شراء مرتبط ب امر استلام" and it stays Approved
        // (PO2608-000001). Such an order is checked for that; another is tried.
        Set<String> tried = new HashSet<>();
        List<String> linked = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_DRAFT_ATTEMPTS; attempt++) {
            try {
                purchaseOrderPage.selectRandomPurchaseOrderWithStatus(APPROVED.label(), tried);
            } catch (org.openqa.selenium.NoSuchElementException noMore) {
                break;
            }
            tried.add(purchaseOrderPage.getLastOpenedRowId());
            form.waitForStatusLabel(APPROVED.label());
            String code = form.waitForCode();
            List<String> logBefore = readTransactionLog();

            ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE, this::toggleApprovalOfOpenedOrder);
            if (outcome.mentions(TestData.CANCEL_LINKED_TO_RECEIPT_ERROR)) {
                form.waitForStatusLabel(APPROVED.label()); // must stay Approved
                log.info("{} is linked to a Receipt Order and was not disapproved (expected); trying another ({}/{}).",
                        code, attempt, MAX_DRAFT_ATTEMPTS);
                linked.add(code);
                closeOpenForms();
                continue;
            }
            assertAccepted("Disapproving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
            verifyDisapproved(code, logBefore);
            return;
        }
        throw new SkipException("No approved order without a Receipt Order found. Linked: " + linked);
    }

    private void verifyDisapproved(String code, List<String> logBefore) {

        form.waitForStatusLabel(DRAFT.label());
        waitUntilActionEnabled("edit");
        List<String> logAfter = readTransactionLog();
        log.info("{} disapproved. Log before: {} | after: {} | approval date now: [{}]",
                code, logBefore, logAfter, purchaseOrderPage.getApprovalDate());

        SoftAssert softly = new SoftAssert();
        assertActions(softly, TestData.ACTIONS_ENABLED_ON_DRAFT, true, "disapproved order " + code);
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Disapproving " + code + " did not add exactly one log entry.");
        if (logAfter.size() > logBefore.size()) {
            softly.assertNotEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The new log entry of " + code + " is an approval, not a disapproval.");
        }
        softly.assertAll();
    }

    @Test(priority = 13, groups = {PO, "cancel"},
            description = "Cancel a Draft PO: status Cancelled, kept after reload, actions disabled")
    public void userCanCancelDraftPurchaseOrder()
            throws InterruptedException {

        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        form.waitForStatusLabel(DRAFT.label());
        cancelOpenedOrderAndVerify();
    }

    @Test(priority = 40, groups = {PO, "cancel"},
            description = "Cancel an Approved PO (no receipt): status Cancelled, kept after reload, actions disabled")
    public void userCanCancelApprovedPurchaseOrder()
            throws InterruptedException {

        // QC, 2026-10-31: an approved PO linked to a Receipt Order cannot be
        // cancelled. The cancel button is still enabled; after confirming, the
        // error toast "لايمكن الغاء امر شراء مرتبط ب امر استلام" is shown and the
        // status stays Approved (PO2608-000005). Such an order is checked for
        // that and another approved order is tried.
        Set<String> tried = new HashSet<>();
        List<String> linked = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_DRAFT_ATTEMPTS; attempt++) {
            try {
                purchaseOrderPage.selectRandomPurchaseOrderWithStatus(APPROVED.label(), tried);
            } catch (org.openqa.selenium.NoSuchElementException noMore) {
                break;
            }
            tried.add(purchaseOrderPage.getLastOpenedRowId());
            form.waitForStatusLabel(APPROVED.label());
            String code = form.waitForCode();

            ActionOutcome outcome = cancelOpenedOrder();
            if (outcome.mentions(TestData.CANCEL_LINKED_TO_RECEIPT_ERROR)) {
                form.waitForStatusLabel(APPROVED.label()); // must stay Approved
                log.info("{} is linked to a Receipt Order and was not cancelled (expected) → trying another ({}/{}).",
                        code, attempt, MAX_DRAFT_ATTEMPTS);
                linked.add(code);
                closeOpenForms();
                continue;
            }
            assertCancelAccepted(code, outcome);
            verifyCancelled(code);
            return;
        }
        throw new SkipException("No approved order without a Receipt Order found. Linked: " + linked);
    }

    /** Cancels the opened order and checks status, persistence and the read-only actions. */
    private void cancelOpenedOrderAndVerify() {

        String code = form.waitForCode();
        assertCancelAccepted(code, cancelOpenedOrder());
        verifyCancelled(code);
    }


    /** Cancel, then confirm; returns the toast and the recorded API calls. */
    private ActionOutcome cancelOpenedOrder() {

        log.info("Cancelling {}.", form.waitForCode());
        waitUntilActionEnabled("cancel");
        return performAndRecord(form, TestData.API_CANCEL, () -> {
            purchaseOrderPage.cancelPurchaseOrder();
            purchaseOrderPage.confirmCancelPurchaseOrder();
        });
    }



    /** The cancel API call must have been sent and answered 2xx, with no error toast. */
    private void assertCancelAccepted(String code, ActionOutcome outcome) {

        assertAccepted("Cancelling " + code, TestData.API_CANCEL, outcome);
    }


    private void verifyCancelled(String code) {

        form.waitForStatusLabel(CANCELLED.label());

        purchaseOrderPage.reloadOrder();
        form.waitForCode();
        form.waitForStatusLabel(CANCELLED.label()); // fails with a timeout if not kept

        // NOTE: "delete" stays ENABLED on cancelled orders in QC (2026-10-26).
        //       Not asserted until the business confirms whether that is intended.
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "approveOrDisapprove", "cancel", "uploadAttachment")) {
            softly.assertFalse(form.isActionEnabled(action),
                    "'" + action + "' must be disabled on cancelled order " + code + ".");
        }
        softly.assertAll();
    }

    @Test(priority = 41, groups = {PO, "edit"},
            description = "Draft PO: edited header fields are kept after save and reload")
    public void editedDraftHeaderIsKeptAfterSave()
            throws InterruptedException {

        onFirstSuitableDraft("editable", () -> Boolean.TRUE);
        String code = form.waitForCode();
        Map<String, String> before = purchaseOrderPage.readHeaderValues();

        fillHeader(HeaderOptions.EDITED_ORDER);
        fillZeroPricesOfLoadedLines();
        Map<String, String> expected = purchaseOrderPage.readHeaderValues();
        log.info("{} header before edit: {}", code, before);
        log.info("{} header after edit : {}", code, expected);
        List<String> changed = expected.keySet().stream()
                .filter(f -> !expected.get(f).equals(before.get(f))).toList();
        log.info("Changed fields: {}", changed);
        if (changed.isEmpty()) {
            log.warn("The random values matched the old ones; the test still checks they are kept.");
        }

        saveAndVerify(DRAFT);
        purchaseOrderPage.reloadOrder();
        form.waitForCode();
        // Dependent fields (e.g. warehouse after branch) are filled a moment after
        // the page loads: read again until all match or 10 s pass.
        Map<String, String> actual = purchaseOrderPage.readHeaderValues();
        long end = System.currentTimeMillis() + 10_000;
        while (!actual.equals(expected) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS); // polling interval
            actual = purchaseOrderPage.readHeaderValues();
        }
        if (expected.get("currencyId").isEmpty()) {
            log.warn("{} was saved WITHOUT a currency (random option had no text). "
                    + "Report: should the currency be required?", code);
        }
        SoftAssert softly = new SoftAssert();
        for (String field : expected.keySet()) {
            softly.assertEquals(actual.get(field), expected.get(field),
                    "Header field '" + field + "' of " + code + " was not kept after save.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // List views: Kanban, group by, summaries (level 1)
    // =====================================================================

    @Test(priority = 42, groups = {PO, "list"},
            description = "Kanban view shows the same orders as the list page, with a status; a card opens its order")
    public void userCanSwitchPurchaseOrdersListToKanbanAndBack() {

        waitForListLoaded();
        List<String> gridCodes = purchaseOrderPage.getVisiblePurchaseOrderCodes();
        try {
            purchaseOrderPage.switchToKanbanView();
            List<String> cardCodes = purchaseOrderPage.getKanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);

            SoftAssert softly = new SoftAssert();
            softly.assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes),
                    "Kanban cards do not show the same orders as the list page.");
            // QC, 2026-10-31: a disapproved order shows "غير معتمد" on its card
            // (PO2608-000010), a label the list status column does not use.
            List<String> labels = new ArrayList<>(
                    Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList());
            labels.add(TestData.DISAPPROVED_CARD_LABEL);
            for (String code : cardCodes) {
                String status = purchaseOrderPage.getKanbanCardStatus(code);
                softly.assertTrue(labels.contains(status), "Card " + code + " has an unknown status [" + status + "].");
            }
            softly.assertAll();

            String target = cardCodes.get(new java.util.Random().nextInt(cardCodes.size()));
            purchaseOrderPage.openKanbanCard(target);
            assertEquals(waitUntilEquals(form::getCode, target), target,
                    "Double-clicking the card of " + target + " opened another order.");
        } finally {
            closeOpenForms();
            if (driver.findElements(By.cssSelector(".ag-root")).isEmpty()) {
                purchaseOrderPage.switchToListView(); // leave the list as the other tests expect it
            }
        }
    }

    /**
     * Right after the screen opens the list is still loading (no rows, no
     * "عرض x - y / z"). Waits (max 20 s) for both; returns the list total.
     */
    private int waitForListLoaded() {

        long end = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < end) {
            try {
                if (!purchaseOrderPage.getVisiblePurchaseOrderCodes().isEmpty()) {
                    return purchaseOrderPage.getPurchaseOrdersPaginationRange()[2];
                }
            } catch (IllegalStateException | org.openqa.selenium.StaleElementReferenceException notYet) {
                // summary or rows not rendered yet
            }
            pause(POLL_MILLIS); // polling interval
        }
        fail("The Purchase Orders list did not load (no rows or no pagination summary) within 20 s.");
        return -1;
    }

    @Test(priority = 43, groups = {PO, "list"},
            description = "Group by status: one group per known status; the groups add up to the list total")
    public void userCanGroupPurchaseOrdersListByStatus() {

        int total = waitForListLoaded();
        purchaseOrderPage.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = purchaseOrderPage.getGroupCounts();
        int sum = groups.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Groups by status: {} (sum {}), list total {}", groups, sum, total);

        List<String> labels = Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList();
        SoftAssert softly = new SoftAssert();
        for (Map.Entry<String, Integer> g : groups.entrySet()) {
            softly.assertTrue(labels.contains(g.getKey()), "Unknown status group [" + g.getKey() + "].");
            softly.assertTrue(g.getValue() > 0, "Group [" + g.getKey() + "] is empty.");
        }
        // QC, 2026-10-31: groups 202+72+23+4 = 301, list total 308. The list API
        // (PurchasOrder/filter) returns 301 rows with totalCount 308 - the same
        // gap as page 6 showing 8 rows while the pager says 10. Reported.
        softly.assertEquals(sum, total,
                "Status groups add up to " + sum + " but the list total is " + total + ".");
        softly.assertAll();
    }

    @Test(priority = 44, groups = {PO, "list"},
            description = "Summaries (الملخصات): each status card equals the number of orders in that status")
    public void purchaseOrderSummariesMatchStatusGroups() {

        waitForListLoaded();
        Map<String, Integer> summaries = purchaseOrderPage.getSummaryCards();
        purchaseOrderPage.groupListBy(TestData.GROUP_BY_STATUS);
        Map<String, Integer> groups = purchaseOrderPage.getGroupCounts();
        log.info("Summaries: {} | groups by status: {}", summaries, groups);

        // QC, 2026-10-31: summaries مسودة 208 / معتمد 34 / ملغي 13 / منتهي 3
        // (API PurchasOrder/Summary: Draft/Posted/Canceld/Expired) while the
        // list has 202 / 72 / 23 / 4. Reported to the analysis team: what
        // should the cards count (the "+" suggests something else)?
        SoftAssert softly = new SoftAssert();
        for (DocumentStatus status : DocumentStatus.values()) {
            softly.assertEquals(summaries.getOrDefault(status.label(), 0),
                    groups.getOrDefault(status.label(), 0),
                    "Summary card [" + status.label() + "] does not match the orders in that status.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Print / Export / Attachments
    // =====================================================================

    @Test(priority = 11, groups = {PO, "print"},
            description = "Save a random PO as PDF: preview shows the order; a PDF file is downloaded")
    public void userCanSavePurchaseOrderAsPdf()
            throws InterruptedException {

        purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
        String code = form.waitForCode();
        java.nio.file.Path dir = purchaseOrderPage.prepareDownloadFolder("pdf");

        openPrintPreviewWithRandomTemplate();
        assertTrue(purchaseOrderPage.getPrintPreviewText().contains(code),
                "The print preview does not show order " + code + ".");

        purchaseOrderPage.savePurchaseOrderAsPdf();
        java.nio.file.Path pdf = purchaseOrderPage.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
        log.info("PDF of {} saved: {} ({} bytes)", code, pdf.getFileName(), Downloads.sizeOf(pdf));
    }

    @Test(priority = 12, groups = {PO, "print"},
            description = "Print a random PO: the preview shows its number, supplier, branch and warehouse")
    public void userCanPrintPurchaseOrder()
            throws InterruptedException, AWTException {

        purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
        String code = form.waitForCode();
        Map<String, String> header = purchaseOrderPage.readHeaderValues();

        openPrintPreviewWithRandomTemplate();
        String preview = purchaseOrderPage.getPrintPreviewText();

        // The preview shows the supplier NAME only ("PG005001 - تست" → "تست").
        String supplier = header.get("vendorId");
        String supplierName = supplier.contains(" - ") ? supplier.substring(supplier.indexOf(" - ") + 3) : supplier;
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(preview.contains(code), "Print preview does not show order " + code + ".");
        for (Map.Entry<String, String> e : Map.of(
                "supplier", supplierName,
                "branch", header.get("branchId"),
                "warehouse", header.get("warehouseId")).entrySet()) {
            if (!e.getValue().isBlank()) {
                softly.assertTrue(preview.contains(e.getValue()),
                        "Print preview of " + code + " does not show the " + e.getKey() + " [" + e.getValue() + "].");
            }
        }
        softly.assertAll();

        purchaseOrderPage.clickFinalPrintPurchaseOrder();
        // Chrome's own print dialog is outside the page, so Selenium cannot see it;
        // clickChromePrintButton() waits for it (Robot delay) and presses Enter.
        purchaseOrderPage.clickChromePrintButton();
    }

    @Test(priority = 19, groups = {PO, "excel"},
            description = "Export a random PO to Excel: an .xlsx with the order's item lines is downloaded")
    public void userCanExportPurchaseOrderToExcel() throws InterruptedException {

        // QC, 2026-10-31: "التصدير الى Excel" exports the ITEM LINES of the
        // opened order ("اوامر-الشراء.xlsx", title "تصدير الاصناف": row 1 title,
        // row 2 headers, then one row per line). The order number is not in it.
        purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus();
        String code = form.waitForCode();
        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
        int lines = form.gridRowCount();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < lines; row++) {
            items.add(purchaseOrderPage.getLineText("itemId", row));
        }
        java.nio.file.Path dir = purchaseOrderPage.prepareDownloadFolder("excel");

        purchaseOrderPage.clickClipboardBarButton();
        purchaseOrderPage.clickExportToExcelMenuItem();

        java.nio.file.Path xlsx = purchaseOrderPage.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        int dataRows = Downloads.xlsxRowCount(xlsx) - 2;
        log.info("Excel of {}: {} ({} bytes), {} data row(s); order has {} line(s): {}",
                code, xlsx.getFileName(), Downloads.sizeOf(xlsx), dataRows, lines, items);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(content.contains(TestData.EXCEL_EXPORT_TITLE),
                xlsx.getFileName() + " has no title '" + TestData.EXCEL_EXPORT_TITLE + "'.");
        softly.assertEquals(dataRows, lines, "Excel rows do not match the item lines of " + code + ".");
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }


    @Test(priority = 14, groups = {PO, "attachments"},
            description = "Attachments added to a PO (not cancelled) are listed and kept after save")
    public void userCanAddAttachmentsToPurchaseOrder()
            throws InterruptedException {

        DocumentStatus status = openRandomPurchaseOrder();
        requireStatus(status, APPROVED, DRAFT, COMPLETED);
        String code = form.waitForCode();

        purchaseOrderPage.clickEditPurchaseOrderButton();
        purchaseOrderPage.clickAttachments();
        int before = purchaseOrderPage.getAttachmentCount();
        int files = purchaseOrderPage.countAttachmentFiles();
        log.info("{} [{}] has {} attachment(s); adding {} file(s).", code, status.label(), before, files);

        addAttachmentFiles();
        assertEquals(purchaseOrderPage.waitForAttachmentCount(before + files), before + files,
                "The added files are not listed in 'تفاصيل المرفق'.");

        purchaseOrderPage.closeAttachmentsDialog();
        saveAndVerify(status);
        purchaseOrderPage.reloadOrder();
        purchaseOrderPage.clickAttachments();

        assertEquals(purchaseOrderPage.getAttachmentCount(), before + files,
                "Attachments of " + code + " were not kept after save and reload.");
    }

    @Test(priority = 39, groups = {PO, "attachments"},
            description = "Deleting an attachment of a PO (not cancelled) is kept after save")
    public void userCanDeleteAttachmentFromPurchaseOrder()
            throws InterruptedException {

        DocumentStatus status = openRandomPurchaseOrder();
        requireStatus(status, APPROVED, DRAFT, COMPLETED);
        String code = form.waitForCode();

        purchaseOrderPage.clickEditPurchaseOrderButton();
        purchaseOrderPage.clickAttachments();
        int before = purchaseOrderPage.getAttachmentCount();

        // Self-contained: an order without attachments gets one first (saved),
        // so the delete always has something to remove.
        if (before == 0) {
            int files = purchaseOrderPage.countAttachmentFiles();
            addAttachmentFiles();
            before = purchaseOrderPage.waitForAttachmentCount(files);
            purchaseOrderPage.closeAttachmentsDialog();
            saveAndVerify(status);
            purchaseOrderPage.clickEditPurchaseOrderButton();
            purchaseOrderPage.clickAttachments();
        }
        log.info("{} [{}] has {} attachment(s); deleting the last one.", code, status.label(), before);

        purchaseOrderPage.deleteLastAttachment();
        assertEquals(purchaseOrderPage.waitForAttachmentCount(before - 1), before - 1,
                "The deleted attachment is still listed in 'تفاصيل المرفق'.");

        purchaseOrderPage.closeAttachmentsDialog();
        purchaseOrderPage.waitForToastToAppearAndClear(""); // a delete toast must not be read as the save toast
        saveAndVerify(status);
        purchaseOrderPage.reloadOrder();
        purchaseOrderPage.clickAttachments();

        assertEquals(purchaseOrderPage.getAttachmentCount(), before - 1,
                "The deleted attachment of " + code + " came back after save and reload.");
    }

    // =====================================================================
    // Exit
    // =====================================================================

    @Test(priority = 20, groups = {PO, "exit"},
            description = "Close a new PO, a viewed PO, and an edited PO: each returns to the list")
    public void userCanExitPurchaseOrder()
            throws InterruptedException {

        // 1) Exit a new, unsaved PO → asks for confirmation.
        purchaseOrderPage.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        purchaseOrderPage.clickClosePurchaseOrderDialog();
        assertTrue(waitForConfirmDialog(form), "Closing a new order did not ask for confirmation.");
        purchaseOrderPage.clickConfirmYesButton();
        assertBackOnList("closing a new order");

        // 2) Exit a PO opened in view mode → closes directly, no confirmation.
        openRandomPurchaseOrder();
        String viewed = form.waitForCode();
        purchaseOrderPage.clickClosePurchaseOrderDialog();
        assertBackOnList("closing " + viewed + " in view mode");

        // 3) Exit a PO opened in edit mode → asks for confirmation.
        //    Cancelled orders cannot be edited, so they are only closed.
        DocumentStatus status = openRandomPurchaseOrder();
        String edited = form.waitForCode();
        if (status != CANCELLED) {
            purchaseOrderPage.clickEditPurchaseOrderButton();
            purchaseOrderPage.clickClosePurchaseOrderDialog();
            assertTrue(waitForConfirmDialog(form), "Closing " + edited + " in edit mode did not ask for confirmation.");
            purchaseOrderPage.clickConfirmYesButton();
        } else {
            purchaseOrderPage.clickClosePurchaseOrderDialog();
        }
        assertBackOnList("closing " + edited + " [" + status.label() + "]");
    }

    // =====================================================================
    // Validation & business rules
    // =====================================================================

    @Test(priority = 21, groups = {PO, "validation"},
            description = "Saving an empty PO is blocked with required-field and item errors")
    public void userCannotSavePurchaseOrderWithoutRequiredData() {

        purchaseOrderPage.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        purchaseOrderPage.clickSave();

        // Read the toast first: it disappears after ~2–3 seconds.
        DocumentForm.Toast toast = form.waitForToast();

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast.isError(), "Expected an error toast, got: " + toast);
        softly.assertTrue(toast.fullText().contains(TestData.AT_LEAST_ONE_ITEM_MESSAGE),
                "Unexpected toast message: " + toast);
        softly.assertEquals(
                form.fieldsWithRequiredError(),
                TestData.REQUIRED_HEADER_FIELDS,
                "Required-field errors do not match the mandatory fields.");
        softly.assertFalse(form.isDialogOpen(),
                "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD),
                "The form must stay in Add mode.");
        softly.assertAll();

        purchaseOrderPage.clickClosePurchaseOrderDialog();
        purchaseOrderPage.clickConfirmYesButton();
    }

    @Test(priority = 22, groups = {PO, "validation", "status"},
            description = "Approved PO: cannot be deleted; in edit mode only the description is editable")
    public void approvedPurchaseOrderAllowsDescriptionEditOnly()
            throws InterruptedException {

        purchaseOrderPage.selectRandomApprovedPurchaseOrder();
        form.waitForStatusLabel(APPROVED.label());

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(form.isActionEnabled("delete"), "Delete must be disabled.");
        softly.assertTrue(form.isActionEnabled("edit"), "Edit must be enabled.");
        softly.assertTrue(form.isActionEnabled("approveOrDisapprove"), "Disapprove must be enabled.");
        softly.assertTrue(form.isActionEnabled("cancel"), "Cancel must be enabled.");

        purchaseOrderPage.clickEditPurchaseOrderButton();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        form.waitUntilFieldEditable("note"); // form re-renders after the URL changes

        softly.assertEquals(
                form.editableFieldsAmong(TestData.HEADER_FIELDS),
                List.of("note"),
                "Only the description may be editable on an approved order.");
        softly.assertAll();

        // Leave without saving.
        purchaseOrderPage.clickClosePurchaseOrderDialog();
        purchaseOrderPage.clickConfirmYesButton();
    }

    // =====================================================================
    // Pagination
    // =====================================================================

    @Test(priority = 23, groups = {PO, "pagination"},
            description = "Purchase Orders grid: first/last page, summary and random selection across all pages")
    public void userCanNavigatePurchaseOrdersPagination()
            throws InterruptedException {

        SoftAssert softly = new SoftAssert();

        // First page
        purchaseOrderPage.goToFirstPurchaseOrdersPage();
        int[] first = purchaseOrderPage.getPurchaseOrdersPaginationRange(); // {from, to, total}
        int total = first[2];
        log.info("Pagination — first page: {} - {} / {}", first[0], first[1], total);

        softly.assertEquals(first[0], 1, "The first page must start at record 1.");
        softly.assertFalse(purchaseOrderPage.isPreviousPurchaseOrdersPageEnabled(),
                "'Previous' must be disabled on the first page.");
        softly.assertEquals(purchaseOrderPage.getVisiblePurchaseOrderRowCount(), first[1] - first[0] + 1,
                "Rows shown on the first page must match the summary.");

        // Last page (direct jump with the "last" button)
        purchaseOrderPage.goToLastPurchaseOrdersPage();
        int[] last = purchaseOrderPage.getPurchaseOrdersPaginationRange();
        log.info("Pagination — last page: {} - {} / {}", last[0], last[1], last[2]);

        softly.assertEquals(last[1], total, "The last page must end at the last record.");
        softly.assertEquals(last[2], total, "The total must not change between pages.");
        softly.assertFalse(purchaseOrderPage.isNextPurchaseOrdersPageEnabled(),
                "'Next' must be disabled on the last page.");
        softly.assertEquals(purchaseOrderPage.getVisiblePurchaseOrderRowCount(), last[1] - last[0] + 1,
                "Rows shown on the last page must match the summary.");

        // Back to the first page
        purchaseOrderPage.goToFirstPurchaseOrdersPage();
        softly.assertEquals(purchaseOrderPage.getPurchaseOrdersPaginationRange()[0], 1,
                "'first' must return to record 1.");
        softly.assertAll();

        // Random selection across ALL pages (original full-scan selector, Next/Previous):
        // the opened order must show the status read from the grid.
        String status = purchaseOrderPage.selectRandomPurchaseOrderFromAllPagesAndGetStatus();
        form.waitForStatusLabel(status);
        log.info("Opened Purchase Order {} with status {}", form.getCode(), status);
    }

    // =====================================================================
    // List search & record navigation
    // =====================================================================

    @Test(priority = 24, groups = {PO, "search"},
            description = "Purchase Orders list: searching by reference number shows only that order")
    public void userCanSearchPurchaseOrdersListByCode()
            throws InterruptedException {

        String code = codeOfRandomOrderThenBackToList();

        purchaseOrderPage.searchPurchaseOrdersList(code);
        boolean found = purchaseOrderPage.waitUntilListShows(code);

        List<String> shown = purchaseOrderPage.getVisiblePurchaseOrderCodes();
        log.info("Search [{}] → {}", code, shown);
        assertTrue(found,
                "Searching for " + code + " did not show it. Rows shown: " + shown);
    }

    @Test(priority = 25, groups = {PO, "search"},
            description = "Record search (magnifier) in the form opens the order typed")
    public void userCanOpenPurchaseOrderFromRecordSearch()
            throws InterruptedException {

        openRandomPurchaseOrder();
        String target = form.waitForCode();

        moveToAnotherRecord();
        assertFalse(target.equals(form.waitForCode()), "Could not move away from " + target + ".");

        purchaseOrderPage.openPurchaseOrderFromRecordSearch(target);

        assertEquals(waitUntilEquals(form::getCode, target), target,
                "Record search did not open " + target + ".");
    }

    @Test(priority = 26, groups = {PO, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenPurchaseOrderRecords()
            throws InterruptedException {

        openRandomPurchaseOrder();
        SoftAssert softly = new SoftAssert();

        // First record: nothing before it.
        if (purchaseOrderPage.isRecordNavigationEnabled("pageFirst")) {
            purchaseOrderPage.clickRecordNavigation("pageFirst");
        }
        String first = form.waitForCode();
        for (String button : List.of("pageFirst", "pagePrevious10", "pageBack")) {
            softly.assertFalse(purchaseOrderPage.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the first record (" + first + ").");
        }

        // Next → a different order; Back → the first one again.
        purchaseOrderPage.clickRecordNavigation("pageNext");
        String second = waitUntilDifferent(form::getCode, first);
        softly.assertNotEquals(second, first, "'Next' did not open another order.");

        purchaseOrderPage.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(form::getCode, first), first,
                "'Back' did not return to the first order.");

        // Last record: nothing after it.
        purchaseOrderPage.clickRecordNavigation("pageLast");
        String last = waitUntilDifferent(form::getCode, first);
        for (String button : List.of("pageLast", "pageNext10", "pageNext")) {
            softly.assertFalse(purchaseOrderPage.isRecordNavigationEnabled(button),
                    "'" + button + "' must be disabled on the last record (" + last + ").");
        }
        log.info("Record navigation: first={}, second={}, last={}", first, second, last);
        softly.assertAll();
    }

    // =====================================================================
    // Toolbar state per mode / status
    // =====================================================================

    @Test(priority = 27, groups = {PO, "validation"},
            description = "New PO: only save/attach/search/menu/close are enabled; Create Receipt is disabled")
    public void newPurchaseOrderEnablesOnlyAllowedActions() {

        purchaseOrderPage.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        // The toolbar settles after the URL changes: "save" stays disabled for
        // ~1.2 s after opening (measured in QC, 2026-10-29). Read the states
        // only once it is ready, otherwise "save" is reported as disabled.
        waitUntilActionEnabled("save");

        // QC, 2026-10-31: "تراجع" (undo) is now ENABLED on a new, empty order
        // (it turns on together with "حفظ" ~1.4 s after opening; it was disabled
        // on 2026-10-29). Kept as a failure until the analysis team confirms
        // which is intended.
        SoftAssert softly = new SoftAssert();
        assertActions(softly, TestData.ACTIONS_DISABLED_ON_NEW_ORDER, false, "a new order");
        assertActions(softly, TestData.ACTIONS_ENABLED_ON_NEW_ORDER, true, "a new order");
        softly.assertFalse(purchaseOrderPage.isFormMenuItemEnabled(TestData.CREATE_RECEIPT_ORDER_MENU_ITEM),
                "'Create Receipt Order' must be disabled on a new order.");
        softly.assertAll();

        purchaseOrderPage.clickClosePurchaseOrderDialog();
        purchaseOrderPage.clickConfirmYesButton();
    }

    @Test(priority = 28, groups = {PO, "validation", "status"},
            description = "Draft PO: edit/delete/approve/cancel enabled; Create Receipt disabled until approved")
    public void draftPurchaseOrderEnablesEditActionsButNotReceipt()
            throws InterruptedException {

        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        form.waitForStatusLabel(DRAFT.label());
        waitUntilActionEnabled("edit"); // same toolbar settling as on a new order

        SoftAssert softly = new SoftAssert();
        assertActions(softly, TestData.ACTIONS_ENABLED_ON_DRAFT, true, "a Draft order");
        softly.assertEquals(purchaseOrderPage.getApprovalDate(), "",
                "A Draft order must not have an approval date.");
        softly.assertFalse(purchaseOrderPage.isFormMenuItemEnabled(TestData.CREATE_RECEIPT_ORDER_MENU_ITEM),
                "'Create Receipt Order' must be disabled on a Draft order.");
        softly.assertAll();
    }

    // =====================================================================
    // Transaction log
    // =====================================================================

    @Test(priority = 29, groups = {PO, "log"},
            description = "Transaction log shows the creation entry (and approval for approved orders)")
    public void userCanViewPurchaseOrderTransactionLog()
            throws InterruptedException {

        DocumentStatus status = openRandomPurchaseOrder();

        purchaseOrderPage.openTransactionLog();
        List<String> actions = purchaseOrderPage.getTransactionLogActions();
        log.info("Transaction log of {} ({}): {}", form.getCode(), status, actions);

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), TestData.LOG_CREATED,
                "The first log entry must be the creation.");
        if (status == APPROVED) {
            softly.assertTrue(actions.contains(TestData.LOG_APPROVED),
                    "An Approved order must have an approval entry in its log.");
        }
        softly.assertAll();

        purchaseOrderPage.closeTransactionLog();
    }

    @Test(priority = 30, groups = {PO, "status", "log"},
            description = "Approving a Draft PO sets today's approval date and adds an approval log entry")
    public void approvingPurchaseOrderSetsApprovalDateAndLogsIt()
            throws InterruptedException {

        purchaseOrderPage.selectRandomDraftPurchaseOrder();
        form.waitForStatusLabel(DRAFT.label());
        String code = form.waitForCode();

        toggleApprovalOfOpenedOrder();
        form.waitForStatusLabel(APPROVED.label());

        // ASSUMPTION (QC data: approval date = day of approval): checked against today.
        String today = LocalDate.now().toString();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(purchaseOrderPage::getApprovalDate, today), today,
                "Approval date of " + code + " was not set to today.");

        purchaseOrderPage.openTransactionLog();
        List<String> actions = purchaseOrderPage.getTransactionLogActions();
        softly.assertTrue(actions.contains(TestData.LOG_APPROVED),
                "No approval entry in the log of " + code + ": " + actions);
        softly.assertAll();

        purchaseOrderPage.closeTransactionLog();
    }

    // =====================================================================
    // Follow-up documents opened from the order
    // =====================================================================

    @Test(priority = 31, groups = {PO, "follow-up"},
            description = "Approved PO → ⋮ → Create Receipt Order opens it pre-filled (not saved)")
    public void userCanCreateReceiptOrderFromApprovedPurchaseOrder()
            throws InterruptedException {

        purchaseOrderPage.selectRandomApprovedPurchaseOrder();
        form.waitForStatusLabel(APPROVED.label());
        String code = form.waitForCode();
        String supplier = form.getSelectedText("vendorId");

        purchaseOrderPage.clickCreateReceiptOrderMenuItem();

        DocumentForm receipt = new DocumentForm(driver, TestData.RECEIPT_ORDER_PREFIX);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains("typeName=PurchaseOrder"),
                "Receipt Order was not opened from a Purchase Order: " + driver.getCurrentUrl());
        softly.assertEquals(waitUntilEquals(receipt::getReferenceDocument, code), code,
                "Receipt Order does not reference " + code + ".");
        softly.assertEquals(waitUntilEquals(() -> receipt.getSelectedText("vendorId"), supplier), supplier,
                "Receipt Order supplier differs from the order's supplier.");
        softly.assertAll();
        // Left unsaved on purpose: the test must not create receipts in QC.
    }

    @Test(priority = 32, groups = {PO, "follow-up"},
            description = "Items Pricing ($) on a random PO (any status) opens Sales → Pricing linked to the order")
    public void userCanOpenItemsPricingFromPurchaseOrder()
            throws InterruptedException {

        // Any status: "$" is enabled on Approved and on Draft orders (QC, 2026-10-29).
        // Not checked yet for Cancelled / Completed → skipped, not failed, if disabled there.
        DocumentStatus status = openRandomPurchaseOrder();
        String code = form.waitForCode();
        if (!waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing ($) is disabled on " + code + " [" + status.label() + "].");
        }
        log.info("Opening Items Pricing from {} [{}]", code, status.label());

        purchaseOrderPage.clickItemsPricing();

        String url = decodedUrl();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("downOpCode=" + code),
                "Pricing screen is not linked to " + code + ": " + url);
        softly.assertTrue(url.contains(TestData.ITEMS_PRICING_TYPE_PARAM),
                "Pricing screen opened with an unexpected document type: " + url);
        softly.assertAll();
    }

    @Test(priority = 33, groups = {PO, "follow-up", "print"},
            description = "Print Barcode on a random PO (any status) opens Item Barcodes with the order's items")
    public void userCanOpenPrintBarcodeFromPurchaseOrder()
            throws InterruptedException {

        // Any status. If the button is disabled for some status → skipped, not failed.
        DocumentStatus status = openRandomPurchaseOrder();
        String code = form.waitForCode();
        if (!waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is disabled on " + code + " [" + status.label() + "].");
        }
        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
        List<String> orderItems = purchaseOrderPage.getGridColumnTexts("itemId");
        log.info("Opening Print Barcode from {} [{}], items {}", code, status.label(), orderItems);

        purchaseOrderPage.clickPrintBarcode();

        String url = decodedUrl();
        // QC, 2026-10-31: /inventory/item-barcodes?referenceCode=<code> lists the
        // order's lines (item, barcode, unit, cost, quantity).
        List<String> barcodeItems = purchaseOrderPage.getGridColumnTexts("itemId");
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("referenceCode=" + code),
                "Item Barcodes screen is not loaded from " + code + ": " + url);
        softly.assertEquals(barcodeItems, orderItems,
                "Item Barcodes does not list the items of " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Delete — result in the list
    // =====================================================================


    // =====================================================================
    // Order lines — saved values, edit, delete (level 1)
    // =====================================================================

    @Test(priority = 35, groups = {PO, "lines"},
            description = "A new PO keeps its item line (item, quantity, cost) and line counts after save and reload")
    public void savedPurchaseOrderKeepsItsLines() {

        purchaseOrderPage.clickNew();
        fillManualOrder(PartySelectionFlow.NEW_DOCUMENT, HeaderOptions.NEW_ORDER);

        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
        int itemLines = form.gridRowCount();
        Line added = readLine(purchaseOrderPage.getNewLineRowIndex());
        purchaseOrderPage.openLinesTab(TestData.DISCOUNTS_TAB);
        int discountLines = form.gridRowCount();
        log.info("Before save: {} item line(s), {} discount line(s); added line {}",
                itemLines, discountLines, added);

        saveAndVerify(TestData.NEW_ORDER_STATUSES);
        purchaseOrderPage.reloadOrder();

        SoftAssert softly = new SoftAssert();
        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
        softly.assertEquals(form.gridRowCount(), itemLines, "Item line count changed after save.");
        softly.assertTrue(containsLine(added),
                "The added line " + added + " was not found after save and reload.");
        purchaseOrderPage.openLinesTab(TestData.DISCOUNTS_TAB);
        softly.assertEquals(form.gridRowCount(), discountLines, "Discount line count changed after save.");
        softly.assertAll();
    }

    @Test(priority = 36, groups = {PO, "lines", "edit"},
            description = "Draft PO: changing the quantity of an existing line is saved")
    public void userCanEditQuantityOfExistingLine()
            throws InterruptedException {

        // QC, 2026-10-29: on some Drafts the existing lines are read-only in edit
        // mode (PO2608-000044: quantity and cost open no editor), on others they
        // are editable (PO2610T28-000094). Reported to the analysis team. Until
        // the rule is known, such a draft is skipped and another one is tried.
        BigDecimal newQuantity = onFirstSuitableDraft("with editable lines", () -> {
            purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
            BigDecimal candidate = form.getGridNumber("quantity", 0).add(BigDecimal.ONE);
            try {
                form.setGridNumber("quantity", 0, candidate.toPlainString());
                return candidate;
            } catch (IllegalStateException notEditable) {
                return null; // read-only lines
            }
        });
        String item = purchaseOrderPage.getLineText("itemId", 0);
        log.info("Line 1 ({}) quantity → {}", item, newQuantity);
        fillZeroPricesOfLoadedLines();

        saveAndVerify(DRAFT);
        purchaseOrderPage.reloadOrder();
        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(purchaseOrderPage.getLineText("itemId", 0), item, "Line 1 is a different item after save.");
        softly.assertEquals(form.getGridNumber("quantity", 0).compareTo(newQuantity), 0,
                "Quantity of line 1 was not saved as " + newQuantity
                        + " (found " + form.getGridNumber("quantity", 0) + ").");
        softly.assertAll();
    }

    @Test(priority = 37, groups = {PO, "lines"},
            description = "New PO: deleting an item line is saved")
    public void userCanDeleteItemLine() {

        // Built on a NEW order instead of an existing Draft: on QC (2026-10-29)
        // the first 5 Drafts found all had a single line (PO2608-000044,
        // PO2608T3-000049, PO2608T3-000050, PO2610T3-000058, PO2610T28-000009),
        // and deleting the only line leaves an order that cannot be saved.
        purchaseOrderPage.clickNew();
        fillManualOrder(PartySelectionFlow.NEW_DOCUMENT, HeaderOptions.NEW_ORDER);

        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);
        if (form.gridRowCount() < 2) {
            addItemLine(); // make sure one line remains after the delete
        }
        int before = form.gridRowCount();
        String deletedItem = purchaseOrderPage.getLineText("itemId", 0);
        log.info("New order has {} item lines; deleting line 1 ({}).", before, deletedItem);

        purchaseOrderPage.deleteLine(0);
        assertEquals(waitForLineCount(before - 1), before - 1, "Deleting a line did not remove it from the grid.");

        saveAndVerify(TestData.NEW_ORDER_STATUSES);
        purchaseOrderPage.reloadOrder();
        purchaseOrderPage.openLinesTab(TestData.ITEMS_TAB);

        assertEquals(form.gridRowCount(), before - 1, "The deleted line came back after save and reload.");
    }

    @Test(priority = 38, groups = {PO, "lines"},
            description = "New PO: deleting a discount/addition line is saved")
    public void userCanDeleteDiscountAdditionLine() {

        purchaseOrderPage.clickNew();
        fillManualOrder(PartySelectionFlow.NEW_DOCUMENT, HeaderOptions.NEW_ORDER);

        purchaseOrderPage.openLinesTab(TestData.DISCOUNTS_TAB);
        int before = form.gridRowCount();
        if (before < 1) {
            fail("The new order has no discount/addition line to delete.");
        }

        purchaseOrderPage.deleteLine(before - 1);
        assertEquals(waitForLineCount(before - 1), before - 1,
                "Deleting a discount/addition line did not remove it from the grid.");

        saveAndVerify(TestData.NEW_ORDER_STATUSES);
        purchaseOrderPage.reloadOrder();
        purchaseOrderPage.openLinesTab(TestData.DISCOUNTS_TAB);

        assertEquals(form.gridRowCount(), before - 1, "The deleted discount/addition line came back after save.");
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Which dialogs the app shows while choosing Supplier → Branch → Warehouse.
     * Each way of opening a PO behaves slightly differently, so the flow is
     * described once here instead of being copy-pasted per test.
     */
    private enum PartySelectionFlow {

        //                   continueAfterSupplier, selectBranch, continueAfterBranch, continueAfterWarehouse
        NEW_DOCUMENT(        false,                 true,         false,               false),
        COPIED_DOCUMENT(     true,                  true,         true,                false),
        EDITED_DOCUMENT(     true,                  false,        false,               true),
        IMPORTED_FROM_EXCEL( true,                  true,         true,                true);

        final boolean continueAfterSupplier;
        final boolean selectBranch;
        final boolean continueAfterBranch;
        final boolean continueAfterWarehouse;

        PartySelectionFlow(boolean continueAfterSupplier,
                           boolean selectBranch,
                           boolean continueAfterBranch,
                           boolean continueAfterWarehouse) {
            this.continueAfterSupplier = continueAfterSupplier;
            this.selectBranch = selectBranch;
            this.continueAfterBranch = continueAfterBranch;
            this.continueAfterWarehouse = continueAfterWarehouse;
        }
    }

    /**
     * Header differences between scenarios.
     *
     * @param clickUpdateCost          the "Update cost" button only exists when the
     *                                 order already has lines (copy / edit / import).
     * @param loadItemsFromAnotherOrder use "Download from" → reference number → download items.
     */
    private record HeaderOptions(boolean clickUpdateCost, boolean loadItemsFromAnotherOrder) {
        static final HeaderOptions NEW_ORDER = new HeaderOptions(false, true);
        static final HeaderOptions EXISTING_LINES = new HeaderOptions(true, true);
        static final HeaderOptions IMPORTED_LINES = new HeaderOptions(true, true);
        static final HeaderOptions EDITED_ORDER = new HeaderOptions(true, false);
    }

    /** Opens a random PO from the external grid and returns its status. */
    private DocumentStatus openRandomPurchaseOrder() throws InterruptedException {

        DocumentStatus status = DocumentStatus.fromLabel(
                purchaseOrderPage.selectRandomPurchaseOrderAndGetStatus()
        );
        // The order is open once its number and status are shown.
        form.waitForCode();
        form.waitForStatusLabel(status.label());
        return status;
    }

    /** Full manual entry: operation type, party, header, item line and discount line. */
    private void fillManualOrder(PartySelectionFlow flow, HeaderOptions header) {

        purchaseOrderPage.selectRandomOperationType();
        selectSupplierBranchAndWarehouse(flow);
        fillHeader(header);
        addItemLine();
        addDiscountAdditionLine();
    }

    /**
     * Supplier → Branch → Warehouse, repeated until the Warehouse is really filled
     * (some supplier/branch combinations have no warehouse), up to
     * {@link #DEFAULT_MAX_ATTEMPTS} times. Every retry chooses the supplier, then the
     * branch (also in flows that normally keep it, when it can be changed), then the
     * warehouse again. The warehouse is checked on the form, not only by the click.
     */
    private void selectSupplierBranchAndWarehouse(PartySelectionFlow flow) {

        for (int attempt = 1; attempt <= DEFAULT_MAX_ATTEMPTS; attempt++) {
            try {
                purchaseOrderPage.selectRandomSupplier();
                if (flow.continueAfterSupplier) {
                    purchaseOrderPage.clickContinue();
                }

                if (flow.selectBranch || (attempt > 1 && form.isFieldEditable("branchId"))) {
                    purchaseOrderPage.selectRandomBranch();
                    if (flow.continueAfterBranch || !flow.selectBranch) {
                        purchaseOrderPage.clickContinue(); // appears only in some flows
                    }
                }

                boolean clicked = purchaseOrderPage.selectRandomWarehouse();
                if (flow.continueAfterWarehouse) {
                    purchaseOrderPage.clickContinue();
                }
                String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(3));
                if (clicked && !warehouse.isEmpty() && !warehouse.equals("اختر")) {
                    log.info("Supplier → Branch → Warehouse: warehouse [{}] (attempt {}).", warehouse, attempt);
                    return;
                }
                log.warn("Warehouse is empty (attempt {}/{}) → supplier, branch and warehouse again.",
                        attempt, DEFAULT_MAX_ATTEMPTS);
            } catch (RuntimeException failed) {
                log.warn("Supplier → Branch → Warehouse failed (attempt {}/{}): {}",
                        attempt, DEFAULT_MAX_ATTEMPTS, failed.getMessage());
            }
        }
        fail("Supplier → Branch → Warehouse: the warehouse stayed empty after " + DEFAULT_MAX_ATTEMPTS + " attempts.");
    }

    private void fillHeader(HeaderOptions options) {

        purchaseOrderPage.selectRandomCurrency();
        purchaseOrderPage.selectRandomCostCenter();
        purchaseOrderPage.selectRandomTaxCalculationMethod();
        purchaseOrderPage.setPurchaseOrderDate();
        purchaseOrderPage.setPurchaseOrderDeadline();
        purchaseOrderPage.selectRandomPurchaseRepresentative();
        purchaseOrderPage.selectRandomPriceOption();
        purchaseOrderPage.enterRandomSupplierInvoiceNumber();
        purchaseOrderPage.selectRandomItemsCostsMethod();
        if (options.clickUpdateCost()) {
            purchaseOrderPage.clickUpdateCost();
        }
        purchaseOrderPage.selectRandomBulkDiscountType();
        purchaseOrderPage.enterBulkDiscountAmount();
        purchaseOrderPage.selectRandomTag();
        if (options.loadItemsFromAnotherOrder()) {
            purchaseOrderPage.selectRandomDownloadFrom();
            purchaseOrderPage.openReferenceNumberGrid();
            purchaseOrderPage.selectRandomReferenceNumber();
            purchaseOrderPage.clickDownloadItemButton();
        }
        purchaseOrderPage.enterDescription();
    }

    /** Reference-document button → random screen → random row → download. */
    private void loadFromReferenceDocumentGrid() {

        purchaseOrderPage.clickOpenReferenceDocumentGridButton();
        purchaseOrderPage.selectRandomReferenceScreen();
        purchaseOrderPage.selectRandomRowFromGrid();
        closeReferenceDialogIfStillOpen();
        downloadReferenceDocument();
    }

    /**
     * Double-clicking a row usually closes the reference dialog, but after
     * "Copy" it stays open (confirmed in QC, 2026-10-26) and covers the
     * download button. In that case confirm it with "موافق".
     */
    private void closeReferenceDialogIfStillOpen() {

        if (form.waitForDialogToClose(Duration.ofSeconds(3))) {
            return;
        }

        String title = form.openDialogTitle();
        log.info("Reference dialog [{}] still open after row selection → confirming.", title);

        try {
            purchaseOrderPage.clickConfirmModalButton();
        } catch (TimeoutException e) {
            fail("Reference dialog [" + title + "] stayed open after selecting a row"
                    + " and has no clickable 'موافق' button.", e);
        }
    }

    /** Clipboard menu → source (requisition / quotation) → random row → confirm → download. */
    private void loadFromClipboardSource(Runnable openSourceMenuItem) {

        purchaseOrderPage.clickClipboardBarButton();
        openSourceMenuItem.run();
        purchaseOrderPage.selectRandomRowFromGrid();
        purchaseOrderPage.clickConfirmModalButton();
        downloadReferenceDocument();
    }

    /**
     * Wraps the page-object download, whose 20 s wait hides WHY the button
     * was not clickable (missing / disabled / covered by a dialog).
     * On timeout, fails with a snapshot of the screen state instead.
     */
    private void downloadReferenceDocument() {

        try {
            purchaseOrderPage.downloadReferenceDocument();
        } catch (TimeoutException e) {
            fail("Reference-document download button never became clickable."
                    + " Open dialog: [" + form.openDialogTitle() + "]"
                    + " | download buttons: " + form.describeButtonsWithIcon("lucide-download"), e);
        }
    }

    /**
     * Completes an order whose items/currency/warehouse came from a reference
     * document: those fields are locked, so only the remaining header is filled.
     */
    private void fillOrderLoadedFromReference() {

        ReferenceSource source = detectReferenceSource();

        // Keep the supplier that came with the reference: changing it resets the
        // loaded warehouse (seen in QC, 2026-10-27, with PR2608-000020 → warehouse
        // became empty and locked). Pick one only when the reference has none.
        String supplier = form.getSelectedText("vendorId");
        if (supplier.isEmpty()) {
            purchaseOrderPage.selectRandomSupplier();
            purchaseOrderPage.clickContinue();
        } else {
            log.info("Keeping supplier from reference document: {}", supplier);
        }
        selectWarehouseIfMissing();
        purchaseOrderPage.selectRandomOperationType();
        purchaseOrderPage.selectRandomCostCenter();
        purchaseOrderPage.selectRandomTaxCalculationMethod();
        purchaseOrderPage.setPurchaseOrderDate();
        purchaseOrderPage.setPurchaseOrderDeadline();
        purchaseOrderPage.selectRandomPurchaseRepresentative();
        purchaseOrderPage.enterRandomSupplierInvoiceNumber();
        purchaseOrderPage.selectRandomItemsCostsMethod();
        purchaseOrderPage.clickUpdateCost();
        purchaseOrderPage.selectRandomBulkDiscountType();
        purchaseOrderPage.enterBulkDiscountAmount();
        purchaseOrderPage.selectRandomTag();
        purchaseOrderPage.enterDescription();

        fillZeroPricesOfLoadedLines();

        // Lines loaded from a Price Quotation are taken as-is:
        // no extra item line and no discount/addition line are added.
        if (source == ReferenceSource.PRICE_QUOTATION) {
            log.info("Reference is a Price Quotation → skipping extra item and discount lines.");
            return;
        }

        addItemLine();
        addDiscountAdditionLine();
    }

    /**
     * Lines loaded from another document ("انزال من", reference document, copy)
     * can arrive with price (التكلفة) 0, which blocks "Add Line" and saving.
     * Give each of them {@link TestData#FALLBACK_PRICE} so the order can be saved,
     * then check that the new price was actually committed.
     */
    private void fillZeroPricesOfLoadedLines() {

        int rows = form.gridRowCount();
        SoftAssert softly = new SoftAssert();

        for (int row = 0; row < rows; row++) {

            if (form.getGridNumber("cost", row).signum() != 0) {
                continue;
            }

            log.info("Line {} has price 0 → entering {}", row + 1, TestData.FALLBACK_PRICE);
            try {
                form.setGridNumber("cost", row, TestData.FALLBACK_PRICE);
            } catch (IllegalStateException e) {
                fail("Line " + (row + 1) + " was loaded with price 0 and its price cannot be edited.", e);
            }

            softly.assertEquals(
                    form.getGridNumber("cost", row).compareTo(new BigDecimal(TestData.FALLBACK_PRICE)), 0,
                    "Price of line " + (row + 1) + " was not updated to " + TestData.FALLBACK_PRICE + ".");
        }
        softly.assertAll();
    }

    /** Max clicks on "Add" before giving up. */
    private static final int MAX_ADD_CLICKS = 5;

    /**
     * Clicks "Add" until the new, empty Purchase Order form actually opens.
     *
     * <p>The button is always enabled, but a click can be swallowed while the
     * opened order is still settling (seen in QC, 2026-10-27: Add clicked on
     * PO2609T3-000053 and the screen did not change for 10 s).</p>
     */
    private void clickAddUntilNewFormOpens() {

        for (int attempt = 1; attempt <= MAX_ADD_CLICKS; attempt++) {

            purchaseOrderPage.clickAddPurchaseOrder();
            if (form.waitForNewDocument(Duration.ofSeconds(5))) {
                return;
            }
            log.warn("\"Add\" click did not open a new form (attempt {}/{}) → clicking again.",
                    attempt, MAX_ADD_CLICKS);
        }

        fail("\"Add\" did not open a new Purchase Order form after " + MAX_ADD_CLICKS + " clicks.");
    }

    /** A test step that may throw checked exceptions (page-object waits throw InterruptedException). */
    @FunctionalInterface
    private interface Step {
        void run() throws Exception;
    }

    /** Attempts to find a reference document that provides a warehouse. */
    private static final int MAX_REFERENCE_ATTEMPTS = 5;

    /**
     * Runs {@code openAndLoadReference} (open the order + load a random reference).
     *
     * <p>Price Quotations AND Purchase Requisitions lock the header; some of them
     * (e.g. PQ2609S26-000001, PR2608-000001) load with the mandatory warehouse
     * EMPTY and LOCKED, so the order can never be saved. Agreed with the team
     * (2026-10-27): discard such a form, start again and pick another reference.
     * A reference with a warehouse, or with an editable warehouse, is used as
     * loaded. The test is SKIPPED (not failed) if no usable one is found.</p>
     */
    private void loadReferenceHavingWarehouse(Step openAndLoadReference) {

        List<String> skipped = new ArrayList<>(); // references without a warehouse

        for (int attempt = 1; attempt <= MAX_REFERENCE_ATTEMPTS; attempt++) {

            try {
                openAndLoadReference.run();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("Opening the order / loading the reference failed.", e);
            }

            String reference = form.getReferenceDocument();

            // A reference without a supplier needs one first: choosing it can
            // lock/clear the warehouse (seen in QC, 2026-10-27, PR2608-000015),
            // so the warehouse is checked only AFTER the supplier is set.
            if (form.getSelectedText("vendorId").isEmpty()) {
                purchaseOrderPage.selectRandomSupplier();
                purchaseOrderPage.clickContinue();
            }

            String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(5));

            if (!warehouse.isEmpty() || form.isFieldEditable("warehouseId")) {
                if (!skipped.isEmpty()) {
                    log.info("Using reference {} after skipping {} (no warehouse).", reference, skipped);
                }
                return;
            }

            skipped.add(reference);
            log.info("Reference {} has no warehouse (empty and locked) → discarding, attempt {}/{}.",
                    reference, attempt, MAX_REFERENCE_ATTEMPTS);
            closeOpenForms();
        }

        throw new SkipException("No reference document with a warehouse found after "
                + MAX_REFERENCE_ATTEMPTS + " attempts. Skipped: " + skipped);
    }

    /**
     * Closes the order form(s) without saving, back to the Purchase Orders grid.
     * "Add" / "Copy" open the new form on top of an existing order, so up to
     * two forms may need closing. Confirms "نعم" when asked to discard changes.
     */
    private void closeOpenForms() {

        By closeButton = By.cssSelector("[data-testid='orders-xIcon']");

        for (int i = 0; i < 3 && !driver.findElements(closeButton).isEmpty(); i++) {
            purchaseOrderPage.clickClosePurchaseOrderDialog();
            // Either a confirmation dialog opens, or the form closes.
            waitUntil("the form to close or ask for confirmation", Duration.ofSeconds(5),
                    () -> form.isDialogOpen() || driver.findElements(closeButton).isEmpty());
            if (form.isDialogOpen()) {
                purchaseOrderPage.waitForOpenDialogReady();
                purchaseOrderPage.clickConfirmYesButton();
                waitUntil("the form to close", Duration.ofSeconds(10),
                        () -> !form.isDialogOpen());
            }
        }
    }

    /**
     * Some reference documents (seen with Price Quotations) do not carry a
     * warehouse, and the warehouse is mandatory. Select one when it is empty.
     */
    private void selectWarehouseIfMissing() {

        // The warehouse loaded from the reference arrives asynchronously:
        // give it time before concluding that it is missing.
        String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(5));
        if (!warehouse.isEmpty()) {
            log.info("Warehouse from reference document: {}", warehouse);
            return;
        }

        // Empty AND locked: the user could not fix it either → report it clearly.
        if (!form.isFieldEditable("warehouseId")) {
            fail("Warehouse is mandatory but empty and locked after loading reference "
                    + form.getReferenceDocument() + " (possible application bug).");
        }

        log.info("Reference document has no warehouse → selecting one.");
        retryUntilSuccessful("Warehouse selection", 3, () -> {
            boolean selected = purchaseOrderPage.selectRandomWarehouse();
            purchaseOrderPage.clickContinue(); // appears only in some flows
            return selected;
        });
    }

    /** Type of the loaded reference document, recognised from its number prefix. */
    private enum ReferenceSource {

        PURCHASE_REQUISITION("PR"),
        PRICE_QUOTATION("PQ");

        private final String prefix;

        ReferenceSource(String prefix) {
            this.prefix = prefix;
        }

        static ReferenceSource fromNumber(String number) {
            for (ReferenceSource source : values()) {
                if (number.startsWith(source.prefix)) {
                    return source;
                }
            }
            throw new IllegalStateException(
                    "Unknown reference document type: [" + number + "]");
        }
    }

    /**
     * Reads the loaded reference number (e.g. PQ2608-000001) and returns its type.
     * Needed because selectRandomReferenceScreen() picks the screen at random.
     */
    private ReferenceSource detectReferenceSource() {

        String number = form.getReferenceDocument();
        ReferenceSource source = ReferenceSource.fromNumber(number);
        log.info("Loaded reference document {} ({})", number, source);
        return source;
    }

    /**
     * Approved → only the description can change.
     * Draft / Completed → full edit (party, header, lines).
     */
    private void editOpenedOrderAccordingTo(DocumentStatus status) {

        purchaseOrderPage.clickEditPurchaseOrderButton();

        if (status == APPROVED) {
            purchaseOrderPage.enterDescription();
            return;
        }

        purchaseOrderPage.selectRandomOperationType();
        selectSupplierBranchAndWarehouse(PartySelectionFlow.EDITED_DOCUMENT);
        fillHeader(HeaderOptions.EDITED_ORDER);
        addItemLine();
        addDiscountAdditionLine();
    }

    /** Max items tried on a new line before giving up. */
    private static final int MAX_ITEM_ATTEMPTS = 5;

    /**
     * Picks a random item for the new line and enters its quantity.
     *
     * <p>On orders tied to a supplier/reference, the item list can offer items
     * the order does not accept: confirming one opens the "البحث عن صنف" dialog
     * with no results (seen in QC, 2026-10-27, "صنف مبارك" on an order from
     * PR2608-000014). In that case the dialog is closed and another item is
     * picked. Any other failure is re-thrown unchanged.</p>
     */
    private void selectAcceptedItemAndEnterQuantity() {

        for (int attempt = 1; attempt <= MAX_ITEM_ATTEMPTS; attempt++) {

            purchaseOrderPage.selectRandomItem();
            try {
                purchaseOrderPage.enterQuantity(TestData.QUANTITY);
                return;
            } catch (TimeoutException e) {
                if (!form.closeEmptyItemSearchDialog()) {
                    throw e; // not the "item not accepted" case
                }
                log.warn("Item not accepted by this order (empty \"البحث عن صنف\" dialog) "
                        + "→ choosing another item ({}/{}).", attempt, MAX_ITEM_ATTEMPTS);
            }
        }

        fail("No accepted item found for the new line after " + MAX_ITEM_ATTEMPTS + " attempts.");
    }

    private void addItemLine() {

        // Lines loaded from a reference / "انزال من" / copy arrive a moment
        // later: wait until the grid stops changing instead of a fixed 3 s.
        purchaseOrderPage.waitForGridToSettle();
        // "Add Line" does nothing while existing lines have price 0
        // (seen in QC: 3 lines from PR2608-000009 at 0.00 → row count stayed 3).
        // Covers every flow: "انزال من", reference documents, copy and edit.
        fillZeroPricesOfLoadedLines();
        purchaseOrderPage.clickAddLine();
        selectAcceptedItemAndEnterQuantity();
        purchaseOrderPage.enterPrice(TestData.PRICE);
        purchaseOrderPage.enterDiscountRate(TestData.LINE_DISCOUNT_RATE);
        purchaseOrderPage.enterDiscountValue(TestData.LINE_DISCOUNT_VALUE);
        purchaseOrderPage.enterExtraRate(TestData.LINE_EXTRA_RATE);
        purchaseOrderPage.enterExtraValue(TestData.LINE_EXTRA_VALUE);
        purchaseOrderPage.selectTodayAsProductionDate();
        purchaseOrderPage.selectExpireDate();
        purchaseOrderPage.enterFreeQuantity(TestData.FREE_QUANTITY);
        purchaseOrderPage.enterNote(TestData.LINE_NOTE);
    }

    private void addDiscountAdditionLine() {

        purchaseOrderPage.clickAddLine();
        purchaseOrderPage.captureNewDiscountAdditionRowIndex();
        purchaseOrderPage.selectRandomAccount();
        purchaseOrderPage.selectRandomInfluencingOnCost();
        purchaseOrderPage.enterDiscountRateInDiscountsSection(TestData.SECTION_DISCOUNT_RATE);
        // NOTE: the double overload targets the Discounts & Additions section;
        // the String overload (used in addItemLine) targets the item line.
        purchaseOrderPage.enterDiscountValue(TestData.SECTION_DISCOUNT_VALUE);
        purchaseOrderPage.enterAdditionRateInDiscountsSection(TestData.SECTION_ADDITION_RATE);
        purchaseOrderPage.enterAdditionValueInDiscountsSection(TestData.SECTION_ADDITION_VALUE);
        purchaseOrderPage.selectRandomCurrencyInDiscountsSection();
        // selectRandomCurrencyInDiscountsSection() already checks the committed
        // value. Its editor can stay open until the next cell is edited, so this
        // only gives it a moment to close; it does not fail.
        purchaseOrderPage.waitForNoCellEditor(Duration.ofSeconds(2));
        purchaseOrderPage.enterDiscountsAdditionsNote(TestData.SECTION_NOTE);
    }

    /**
     * Saves and verifies the RESULT:
     * success toast → form back in view mode → number generated →
     * description persisted (unique per run) → expected status.
     */
    private void saveAndVerify(DocumentStatus... allowedStatuses) {

        String note = TestData.uniqueNote();
        form.setNote(note);

        waitUntilActionEnabled("save");
        purchaseOrderPage.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        purchaseOrderPage.clickConfirmYes();

        // Read the toast first: it disappears after ~2–3 seconds.
        DocumentForm.Toast toast = form.waitForToast();

        // A warning ("تنبيه") does not block the save. QC, 2026-10-29: "إجمالي امر
        // الشراء (3400.00) تجاوز الحد المسموح به (1000.00)" is shown and the order
        // is still saved (confirmed manually). Only a real error fails the test.
        boolean warning = isWarning(toast);
        if (warning) {
            log.warn("Save shows a warning (not blocking): {}", toast);
        } else {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast);
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = form.waitForCode(); // data loads after the URL changes

        SoftAssert softly = new SoftAssert();
        if (!warning) {
            softly.assertTrue(toast.fullText().contains(TestData.SAVE_SUCCESS_MESSAGE),
                    "Unexpected success message: " + toast);
        }
        softly.assertEquals(form.waitForNote(note), note,
                "Description was not persisted in " + code + ".");
        String status = form.getStatusLabel();
        softly.assertTrue(DocumentStatus.fromLabel(status).isOneOf(allowedStatuses),
                "Unexpected status [" + status + "] after saving " + code
                        + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();

        log.info("Saved Purchase Order {} with status {}", code, status);
    }

    /** Title of a non-blocking warning toast. */
    private static final String WARNING_TITLE = "تنبيه";

    private static boolean isWarning(DocumentForm.Toast toast) {
        return toast.title() != null && toast.title().contains(WARNING_TITLE);
    }

    /**
     * Save opens a confirmation dialog only when the form is valid.
     * Otherwise fail with the actual reason (toast and/or required-field errors)
     * instead of a 10-second timeout on the "Yes" button.
     */
    private void failIfSaveBlockedBeforeConfirmation() {

        // QC, 2026-10-31 (mvn run, order from PQ2609S7-000009): the click on
        // "حفظ" was lost: no dialog, no toast, no field error (the button's
        // tooltip was showing). One more click in that case only.
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                form.waitForConfirmationDialogOrToast().ifPresent(toast -> fail(
                        "Save was rejected before confirmation: " + toast
                                + " | required-field errors: " + form.fieldsWithRequiredError()));
                return;
            } catch (TimeoutException e) {
                List<String> errors = form.fieldsWithRequiredError();
                if (attempt == 2 || !errors.isEmpty()) {
                    fail("Save did not open the confirmation dialog. Required-field errors: " + errors, e);
                }
                log.warn("Save click showed nothing (no dialog, toast or field error); clicking again.");
                purchaseOrderPage.clickSave();
            }
        }
    }


    private void undoAndConfirm() {

        purchaseOrderPage.undo();
        purchaseOrderPage.waitForOpenDialogReady();
        purchaseOrderPage.confirmUndo();
    }

    /**
     * The same button approves a Draft order and disapproves an Approved one
     * (button#approveOrDisapprove). The page-object method names say
     * "approveDraft…" for historical reasons.
     */
    private void toggleApprovalOfOpenedOrder() {

        waitUntilActionEnabled("approveOrDisapprove");
        purchaseOrderPage.approveDraftPurchaseOrder();
        purchaseOrderPage.waitForOpenDialogReady();
        purchaseOrderPage.confirmApproveDraftPurchaseOrder();
    }

    private void openPrintPreviewWithRandomTemplate() {

        waitUntilActionEnabled("print");
        purchaseOrderPage.openPrintTemplateChooser();
        String template = purchaseOrderPage.selectRandomPurchaseOrderPrintTemplate();
        log.info("Selected print template: {}", template);
        purchaseOrderPage.confirmPrintPurchaseOrder();
    }

    // ----- Steps used by the gap-analysis tests -----

    /** Opens a random order, reads its number, closes it (view mode closes directly). */
    private String codeOfRandomOrderThenBackToList() throws InterruptedException {

        openRandomPurchaseOrder();
        String code = form.waitForCode();
        closeOpenForms();
        return code;
    }

    /** Moves to the first record, or to the last one when already on the first. */
    private void moveToAnotherRecord() {

        purchaseOrderPage.clickRecordNavigation(
                purchaseOrderPage.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
    }

    /** Asserts every toolbar action in {@code ids} is enabled / disabled. */
    /** Opens the transaction log, returns its actions (oldest first) and closes it. */
    private List<String> readTransactionLog() {

        purchaseOrderPage.openTransactionLog();
        List<String> actions = purchaseOrderPage.getTransactionLogActions();
        purchaseOrderPage.closeTransactionLog();
        return actions;
    }


    /** After "نسخ": the new (unsaved) copy is open with the supplier of the source order. */
    private void waitForCopiedOrder() {

        form.waitForMode(DocumentForm.Mode.ADD);
        form.waitForSelectedText("vendorId", Duration.ofSeconds(15));
    }


    /** The order form is closed and the Purchase Orders list is shown again. */
    private void assertBackOnList(String afterWhat) {

        try {
            new org.openqa.selenium.support.ui.WebDriverWait(driver, Duration.ofSeconds(15))
                    .until(d -> d.findElements(By.cssSelector("[data-testid='orders-xIcon']")).isEmpty()
                            && !d.getCurrentUrl().matches(".*/purchase/orders/(view|add|edit)\\b.*"));
        } catch (TimeoutException e) {
            fail("Still on the order form after " + afterWhat + ". URL: " + driver.getCurrentUrl());
        }
        assertTrue(driver.getCurrentUrl().contains("/purchase/orders"),
                "Not back on the Purchase Orders list after " + afterWhat + ". URL: " + driver.getCurrentUrl());
    }

    private void assertActions(SoftAssert softly, List<String> ids, boolean enabled, String where) {

        for (String id : ids) {
            softly.assertEquals(form.isActionEnabled(id), enabled,
                    "'" + id + "' must be " + (enabled ? "enabled" : "disabled") + " on " + where + ".");
        }
    }

    /**
     * Like {@link #waitUntilActionEnabled(String)} but returns the result, for
     * callers that skip the test when the action stays disabled.
     */
    private boolean waitForActionEnabled(String id) {

        waitUntilActionEnabled(id);
        return form.isActionEnabled(id);
    }

    /** Waits (max 10 s) until toolbar action {@code id} is enabled; the caller asserts afterwards. */
    private void waitUntilActionEnabled(String id) {

        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            try {
                if (form.isActionEnabled(id)) {
                    return;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException ignored) {
                // toolbar re-rendering; read again
            }
            pause(POLL_MILLIS); // polling interval
        }
        log.warn("Toolbar action '{}' still disabled after 10 s.", id);
    }

    /** Re-reads {@code reader} until it differs from {@code old} (max 10 s); returns the last value. */
    private String waitUntilDifferent(java.util.function.Supplier<String> reader, String old) {

        long end = System.currentTimeMillis() + 10_000;
        String value = old;
        while (System.currentTimeMillis() < end) {
            try {
                value = reader.get();
                if (!value.isEmpty() && !value.equals(old)) {
                    return value;
                }
            } catch (org.openqa.selenium.StaleElementReferenceException
                     | org.openqa.selenium.NoSuchElementException ignored) {
                // form re-rendering; read again
            }
            pause(POLL_MILLIS); // polling interval
        }
        return value;
    }

    private String decodedUrl() {
        return URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
    }

    // ----- Steps used by the lines tests -----

    /** One item line as shown in the grid. */
    private record Line(String item, BigDecimal quantity, BigDecimal cost) {
        boolean sameAs(Line other) {
            return item.equals(other.item)
                    && quantity.compareTo(other.quantity) == 0
                    && cost.compareTo(other.cost) == 0;
        }
    }

    private Line readLine(int row) {
        return new Line(purchaseOrderPage.getLineText("itemId", row),
                form.getGridNumber("quantity", row),
                form.getGridNumber("cost", row));
    }

    /** True if some item line equals {@code expected} (lines may be re-ordered on save). */
    private boolean containsLine(Line expected) {

        int rows = form.gridRowCount();
        for (int row = rows - 1; row >= 0; row--) {
            Line actual = readLine(row);
            if (actual.sameAs(expected)) {
                log.info("Added line found after reload at row {}: {}", row + 1, actual);
                return true;
            }
        }
        return false;
    }

    /**
     * In the open "تفاصيل المرفق" dialog: "إضافة مرفق" → all files of
     * test-data/attachments → title + description → "اضافة".
     */
    private void addAttachmentFiles() {

        purchaseOrderPage.clickAddAttachment();
        // The upload form renders its file input after opening.
        waitUntil("the attachment file input", Duration.ofSeconds(10),
                () -> !driver.findElements(By.cssSelector("input[type='file']")).isEmpty());
        purchaseOrderPage.uploadAllAttachments();
        purchaseOrderPage.enterAttachmentTitle(TestData.ATTACHMENT_TITLE);
        purchaseOrderPage.enterAttachmentDescription(TestData.ATTACHMENT_DESCRIPTION);
        purchaseOrderPage.clickAddAttachmentConfirm();
        // The upload toast must be gone, otherwise the next save reads it.
        purchaseOrderPage.waitForToastToAppearAndClear("تمت إضافة الملفات");
    }

    /** Drafts tried before giving up when the chosen one does not fit the scenario. */
    private static final int MAX_DRAFT_ATTEMPTS = 5;

    /**
     * Opens Draft orders in edit mode, one at a time, until {@code check}
     * returns a non-null value for one of them, and returns that value.
     *
     * <p>An unsuitable draft is closed without saving and EXCLUDED from the next
     * search, so the same order is never tried twice (before this, a random pick
     * on a page with 3 drafts kept returning PO2608-000044). The test is skipped
     * when no suitable draft is found.</p>
     */
    private <T> T onFirstSuitableDraft(String need, java.util.function.Supplier<T> check)
            throws InterruptedException {

        Set<String> triedRowIds = new HashSet<>();
        List<String> triedCodes = new ArrayList<>();

        for (int attempt = 1; attempt <= MAX_DRAFT_ATTEMPTS; attempt++) {
            try {
                purchaseOrderPage.selectRandomDraftPurchaseOrder(triedRowIds);
            } catch (org.openqa.selenium.NoSuchElementException noMoreDrafts) {
                break;
            }
            triedRowIds.add(purchaseOrderPage.getLastOpenedRowId());

            form.waitForStatusLabel(DRAFT.label());
            waitUntilActionEnabled("edit");
            purchaseOrderPage.clickEditPurchaseOrderButton();
            form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
            String code = form.waitForCode();

            T result = check.get();
            if (result != null) {
                log.info("Using Draft {} ({}).", code, need);
                return result;
            }
            triedCodes.add(code);
            log.info("Draft {} is not {} → trying another one ({}/{}).", code, need, attempt, MAX_DRAFT_ATTEMPTS);
            closeOpenForms();
        }
        throw new SkipException("No Draft " + need + " found. Tried: " + triedCodes);
    }

    /** Waits (max 10 s) until the active grid has {@code expected} lines; returns the last count. */
    private int waitForLineCount(int expected) {

        long end = System.currentTimeMillis() + 10_000;
        int count = form.gridRowCount();
        while (count != expected && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS); // polling interval
            count = form.gridRowCount();
        }
        return count;
    }
}
