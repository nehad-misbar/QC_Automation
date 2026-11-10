inajib
package com.erp.tests;

import com.erp.automation.base.PurchasingBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.ForeignInvoicePage;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.Downloads;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.function.Predicate;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Foreign Purchase Invoice (فواتير المشتريات الخارجية), level 1: 42 tests, written separately
 * from (and as a copy of) {@code LocalPurchaseInvoiceTest}, on {@link ForeignInvoicePage}
 * (2026-11-05, at the user's request: no shared base class). QC 2026-11-05 differences:
 * "الاعتماد المستندي" (required) and "التصنيف" in the header; items pricing by item ids;
 * no items-pricing button on a new invoice. Not applicable, reported as SKIPPED with the
 * reason: reference document (4–6, the field is disabled), purchase return and debit /
 * credit notes (29–31, 33, not in "⋮"), continue from a receipt order (41). The old test
 * is in {@code docs/legacy/ForeignPurchaseInvoiceTest.java.txt}.
 *
 * <p>The text below describes the local invoice this class was copied from; it applies here too.</p>
 *
 * <p>Local Purchase Invoice (فواتير المشتريات المحلية), level 1, modeled on
 * {@code PurchaseOrderTest} (asked by the user, 2026-11-05): create / copy / add
 * (with and without a reference receipt order), required fields, toolbar per
 * status, edit by status, undo, delete, approve, transaction log, list search,
 * record search and navigation, pagination, Kanban, print, Excel, follow-up
 * screens (items pricing, barcode, journal entry, purchase return, debit / credit
 * note, pay), attachments, lines, and exit; plus (2026-11-05, evening) the
 * "⋮" of a new invoice: Excel import (40) and continue from a receipt order
 * (41), and the payment schedule of a payment terms template (42).
 *
 * <p>Not applicable to this screen (QC 2026-11-05): save as PDF (the print
 * preview has no PDF button), group by status and summaries (not on the list),
 * disapprove and cancel (no such actions), create receipt order (the invoice is
 * created FROM a receipt order).</p>
 *
 * <p>Built on the Purchasing template ({@link PurchasingBaseTest},
 * {@code PurchasingDocumentPage}, {@link DocumentForm}), like
 * {@code QuotationTest}:</p>
 * <ul>
 *     <li>test data in {@link TestData};</li>
 *     <li>each test reads as the business scenario;</li>
 *     <li>steps are in "Reusable steps" at the bottom;</li>
 *     <li>every action ends with a check of its result.</li>
 * </ul>
 *
 * <p>Screen facts used here were checked in QC on 2026-11-05 (see
 * {@link ForeignInvoicePage}). The list has no status column, so documents are
 * chosen through the API ({@code PurchasInvoice/filter/0}). Tests that change
 * a document (edit, delete, approve) create their own invoice first, so they
 * do not depend on, or use up, existing QC data.</p>
 *
 * <p>The old test (5 tests, login in every test, fixed sleeps, no checks
 * after saving) was replaced on 2026-11-05. A copy is in
 * {@code docs/legacy/LocalPurchaseInvoiceTest.java.txt}. It is the MODEL of
 * the create / copy / add flows (tests 1–6): the same fields, in the same
 * order, with the same values (see {@link TestData}), now with explicit
 * waits and a check of every entered value. What changed from the old flow:</p>
 * <ul>
 *     <li>"متابعة" after changing the supplier / branch / warehouse of a copy
 *     is clicked BEFORE the new value is checked, with the old
 *     {@code clickContinue} (up to 5 s), when the form already has lines;
 *     see {@code asksToContinue} / {@code selectRandomOptionAndContinue};</li>
 *     <li>the branch → warehouse retry is bounded (5) and changes only the branch;</li>
 *     <li>dates are set once (the old test typed them and then picked them again
 *     in the calendar);</li>
 *     <li>"البيان" and "رقم فاتورة المورد" are unique per run, so the saved
 *     invoice can be found and checked;</li>
 *     <li>"مبلغ الفاتورة" is copied from the total right before saving, after
 *     every line, discount and addition is in;</li>
 *     <li>"إضافة" with a reference document is new (test 6).</li>
 * </ul>
 */
public class ForeignPurchaseInvoiceTest extends PurchasingBaseTest {

    private static final String LPI = "foreign-purchase-invoice";

    private ForeignInvoicePage page;
    private DocumentForm form;

    @Override
    protected void openScreen(PurchasingPage purchasingPage) {
        purchasingPage.clickForeignPurchaseInvoices();
        page = new ForeignInvoicePage(driver);
        form = new DocumentForm(driver, ForeignInvoicePage.PREFIX);
        page.dismissInstallPrompt();
        page.waitForListLoaded();
    }

    // =====================================================================
    // Test data
    // =====================================================================

    private static final class TestData {

        // ----- values of the old test (docs/legacy/LocalPurchaseInvoiceTest.java.txt) -----

        static final String QUANTITY = "17";
        static final String PRICE = "100";
        static final String LINE_DISCOUNT_RATE = "20";
        static final String LINE_DISCOUNT_VALUE = "20";
        static final String LINE_EXTRA_RATE = "20";
        static final String LINE_EXTRA_VALUE = "20";
        static final String FREE_QUANTITY = "114";
        static final String LINE_NOTE = "بيان فاتورة المشتريات الخارجية";
        /** "تاريخ الانتهاء" of the line: 30 days after today (production date = today). */
        static final int EXPIRY_IN_DAYS = 30;

        /** "تعميم خصم على الأصناف": a random type and this amount. */
        static final String BULK_DISCOUNT_AMOUNT = "10";

        /** "الخصومات والإضافات" line. */
        static final String DA_DISCOUNT_RATE = "20";
        static final String DA_DISCOUNT_VALUE = "20";
        static final String DA_ADDITION_RATE = "20";
        static final String DA_ADDITION_VALUE = "20";
        static final String DA_NOTE = "بيان فاتورة المشتريات الخارجية";

        static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";

        /** A newly saved invoice is a Draft, or Approved when the company approves on save. */
        static final DocumentStatus[] NEW_INVOICE_STATUSES = {DRAFT, APPROVED};

        /** Branches tried when the chosen one has no warehouse. */
        static final int MAX_BRANCH_ATTEMPTS = 5;
        /**
         * Supplier → branch → warehouse rounds while the warehouse stays empty; each round
         * tries {@link #BRANCH_ATTEMPTS_PER_SUPPLIER} branches of the supplier.
         */
        static final int MAX_SUPPLIER_ATTEMPTS = 5;
        static final int BRANCH_ATTEMPTS_PER_SUPPLIER = 2;

        /** Items tried on a new line before giving up (an item the invoice refuses never loads). */
        static final int MAX_ITEM_ATTEMPTS = 8;

        /** Receipt orders tried in "المستند المرجعي" when one brings no line. */
        static final int MAX_REFERENCE_ATTEMPTS = 3;

        /** Excel file with item lines (code + quantity; same layout as orders and quotations). */
        static final java.nio.file.Path EXCEL_LINES = java.nio.file.Paths.get(System.getProperty("user.dir"),
                "test-data", "import-excel", "PurchaseOrder.xlsx");

        /** Payment schedule: the portions add up to 100 % (rounded per line, e.g. 5 × 16.67 + 16.65). */
        static final BigDecimal FULL_PORTION = new BigDecimal("100");

        /** "تاريخ الاستحقاق": one year after today, as the old test. */
        static final int DUE_IN_DAYS = 365;

        /** Required on save (QC 2026-11-05, empty save of a new invoice). */
        static final List<String> REQUIRED_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "branchId", "warehouseId",
                "purchaseRepresentativeId", "tagId", "amountInvoice", "vendorInvoiceCode",
                "letterOfCredtId"); // "الاعتماد المستندي*" (foreign only)

        /** Toast of an empty save (QC 2026-11-05). */
        static final String NO_LINES_ERROR = "يجب اضافة صنف واحد على الأقل";

        /** Header values compared after save + reload ("notes" is checked by saveAndVerify). */
        static final List<String> PERSISTED_HEADER_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "currencyId", "branchId", "warehouseId",
                "costCenterId", "taxCalculationMethode", "purchaseRepresentativeId",
                "itemsCostsMethode", "tagId", "invoiceDate", "vendorInvoiceCode",
                "letterOfCredtId", "classification");

        /** Approved invoice in edit mode (/view-approved/): these must be editable (QC 2026-11-05, LPI2611T45-000007). */
        static final List<String> EDITABLE_ON_APPROVED = List.of("vendorId", "notes");

        /**
         * Also editable on an Approved invoice WHEN SHOWN: "تاريخ الاستحقاق" is on the
         * form only with payment method "آجل" (QC run 2026-11-05 16:36,
         * LPI2610T16-000202). Whether it should be editable after approval is a
         * question for the analysis team (see the guide).
         */
        static final List<String> EDITABLE_ON_APPROVED_WHEN_SHOWN = List.of("deservedDate");

        /** New invoice toolbar (QC 2026-11-05). edit / printBarcode are not shown on a new invoice. */
        static final List<String> ACTIONS_DISABLED_ON_NEW = List.of(
                "copy", "add", "undo", "delete", "print",
                "approveOrDisapprove", "journalEntry", "transactionLogs"); // no itemsPricing on a new foreign invoice
        static final List<String> ACTIONS_ENABLED_ON_NEW = List.of("save", "xIcon", "uploadAttachment");

        /** Draft in view mode (QC 2026-11-05, LPI2609T16-000036). */
        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "copy", "add", "edit", "delete", "print", "itemsPricing", "approveOrDisapprove", "transactionLogs");
        static final List<String> ACTIONS_DISABLED_ON_DRAFT = List.of("journalEntry", "save");

        /**
         * Approved in view mode (QC 2026-11-05, LPI2611T45-000007): no approve /
         * disapprove button, "قيد اليومية" enabled. "حذف" is ENABLED on an
         * approved invoice; that is a question to the analysis team, so it is
         * only logged, not asserted.
         */
        static final List<String> ACTIONS_ENABLED_ON_APPROVED = List.of("edit", "print", "journalEntry", "transactionLogs");

        /**
         * Part of the approve request's URL. The app's code calls
         * "/approveOrDisApprove/" (QC 2026-11-05, as on orders / quotations);
         * "pprove" also matches "Approve…" if this screen's request differs
         * in letter case. The recorded calls are printed if it is not found.
         */
        static final String API_APPROVE = "pprove";

        /** Assumed like orders/quotations; confirm on the first run. */
        static final String LOG_CREATED = "انشاء العملية";
        static final String LOG_APPROVED = "اعتماد العملية";

        private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        /** The old test's "البيان" plus date and time, so a persisted value can be told apart from old data. */
        static final String NOTE_PREFIX = "فاتورة مشتريات خارجية تجريبية ";

        static String uniqueNote() {
            return NOTE_PREFIX + LocalDateTime.now().format(STAMP);
        }

        /** "رقم فاتورة المورد": "VI-<date>-<time>"; a suffix is added if it is taken. */
        static String vendorInvoiceCode() {
            return "VI-" + LocalDateTime.now().format(STAMP);
        }

        // ----- level 1: list, print, follow-ups, attachments, lines (QC 2026-11-05) -----

        /** Header fields an Undo must restore (the supplier/branch/warehouse are not changed in edit mode). */
        static final List<String> UNDO_HEADER_FIELDS = List.of(
                "operationTypeCategoryId", "vendorId", "currencyId", "branchId", "warehouseId",
                "costCenterId", "taxCalculationMethode", "purchaseRepresentativeId", "tagId",
                "invoiceDate", "vendorInvoiceCode", "notes", "letterOfCredtId", "classification");

        /** Kanban card statuses an invoice can have (no cancel on invoices). */
        static final List<String> CARD_STATUSES = List.of(DRAFT.label(), APPROVED.label());

        static final String ATTACHMENT_TITLE = "مرفقات فاتورة مشتريات خارجية تجريبية";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات فاتورة المشتريات الخارجية";

        /** Title of the "دفع" dialog (QC 2026-11-05). */
        static final String PAY_DIALOG_TITLE = "جدولة الدفعات";
        /** "⋮" → "دفع" on an invoice without a payment schedule opens this form (QC 2026-11-05). */
        static final String DIRECT_PAY_DIALOG_TITLE = "دفع";
        static final List<String> DIRECT_PAY_FIELDS = List.of("paymentMethod", "accountId", "cashAmount", "descriptions");

        /** Title of the journal entry dialog. */
        static final String JOURNAL_ENTRY_TITLE = "قيد اليومية";

        /** Money compared to the cent. */
        static final BigDecimal CENT = new BigDecimal("0.01");

        private TestData() {
        }
    }

    // =====================================================================
    // Create: without a reference document
    // =====================================================================

    @Test(priority = 1, groups = {LPI, "create"},
            description = "New invoice from the New button: header, item line (discount, addition, dates, "
                    + "free quantity, note), discounts & additions line, payment terms; amount = total with tax")
    public void userCanCreateNewForeignPurchaseInvoiceWithoutReferenceDocument() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillInvoiceWithoutReference();
        saveAndVerify(TestData.NEW_INVOICE_STATUSES);
    }

    @Test(priority = 2, groups = {LPI, "create"},
            description = "Copy (نسخ) an existing invoice and save it as a new one; the source is unchanged")
    public void userCanDuplicateForeignPurchaseInvoiceWithoutReferenceDocument() {

        Map<String, Object> source = openRandomInvoice(r -> true);

        page.clickCopy();
        form.waitForMode(DocumentForm.Mode.ADD);

        // The copy keeps the supplier and the lines (QC 2026-11-05); they are completed first.
        fillInvoiceWithoutReference();
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);

        verifyNewInvoice(source, code, "\"نسخ\"");
    }

    @Test(priority = 3, groups = {LPI, "create"},
            description = "New invoice from the Add button of an opened invoice")
    public void userCanAddForeignPurchaseInvoiceWithoutReferenceDocument() {

        Map<String, Object> opened = openRandomInvoice(r -> true);

        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillInvoiceWithoutReference();
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);

        verifyNewInvoice(opened, code, "\"إضافة\"");
    }

    // =====================================================================
    // Create: with a reference document (a receipt order)
    // =====================================================================

    @Test(priority = 4, groups = {LPI, "create", "reference"},
            description = "New invoice from a receipt order (المستند المرجعي): its lines are downloaded, "
                    + "the rest of the header is completed; the reference is kept after save")
    public void userCanCreateNewForeignPurchaseInvoiceWithReferenceDocument() {
        throw new SkipException("Not applicable: \"المستند المرجعي\" is disabled on a foreign invoice (QC 2026-11-05).");
    }

    @Test(priority = 5, groups = {LPI, "create", "reference"},
            description = "Copy (نسخ) an existing invoice, then a receipt order as reference document")
    public void userCanDuplicateForeignPurchaseInvoiceWithReferenceDocument() {
        throw new SkipException("Not applicable: \"المستند المرجعي\" is disabled on a foreign invoice (QC 2026-11-05).");
    }

    @Test(priority = 6, groups = {LPI, "create", "reference"},
            description = "Add button of an opened invoice, then a receipt order as reference document")
    public void userCanAddForeignPurchaseInvoiceWithReferenceDocument() {
        throw new SkipException("Not applicable: \"المستند المرجعي\" is disabled on a foreign invoice (QC 2026-11-05).");
    }

    // =====================================================================
    // Validation and toolbar per status
    // =====================================================================

    @Test(priority = 7, groups = {LPI, "validation"},
            description = "Saving an empty invoice is blocked: no confirmation, required fields marked, "
                    + "\"add at least one item\" error, stays in Add")
    public void userCannotSaveForeignPurchaseInvoiceWithoutRequiredData() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" is disabled on a new invoice.");

        page.clickSave();
        DocumentForm.Toast toast;
        try {
            toast = form.waitForToast(); // read first: it disappears after 2–3 s
        } catch (org.openqa.selenium.TimeoutException none) {
            toast = null;
        }
        List<String> marked = page.requiredErrorFields();
        log.info("Empty save → toast [{}], required errors on {}", toast, marked);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast != null && toast.isError() && toast.fullText().contains(TestData.NO_LINES_ERROR),
                "Expected the error \"" + TestData.NO_LINES_ERROR + "\", got: " + toast);
        softly.assertTrue(marked.containsAll(TestData.REQUIRED_FIELDS),
                "Required fields not marked \"هذا الحقل مطلوب\": expected " + TestData.REQUIRED_FIELDS + ", marked " + marked);
        softly.assertFalse(form.isDialogOpen(), "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 8, groups = {LPI, "validation"},
            description = "New invoice toolbar: save / close / attachments enabled; copy, add, undo, delete, "
                    + "print, items pricing, approve, journal entry and log disabled")
    public void newForeignPurchaseInvoiceEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("save");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_ON_NEW) {
            log.info("New invoice: '{}' {}", action,
                    page.isActionEnabled(action) ? "ENABLED" : (page.hasAction(action) ? "disabled" : "not shown"));
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new invoice.");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_NEW) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new invoice.");
        }
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 9, groups = {LPI, "validation", "status"},
            description = "Draft invoice: edit / delete / approve enabled, journal entry disabled, no approval date")
    public void draftForeignPurchaseInvoiceEnablesEditActions() {

        openRandomInvoice(ForeignInvoicePage::isDraft);
        page.waitForStatusLabel(DRAFT.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        for (String action : TestData.ACTIONS_DISABLED_ON_DRAFT) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on Draft " + code + ".");
        }
        softly.assertEquals(page.inputValue("approvalDate"), "", "Draft " + code + " must not have an approval date.");
        softly.assertAll();
    }

    @Test(priority = 10, groups = {LPI, "validation", "status"},
            description = "Approved invoice: no approve/disapprove button, journal entry enabled, approval date set")
    public void approvedForeignPurchaseInvoiceShowsJournalEntryAndNoApproval() {

        openRandomInvoice(ForeignInvoicePage::isApproved);
        page.waitForStatusLabel(APPROVED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.hasAction("approveOrDisapprove") && page.isActionEnabled("approveOrDisapprove"),
                "Approved " + code + " must not offer approve / disapprove.");
        for (String action : TestData.ACTIONS_ENABLED_ON_APPROVED) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Approved " + code + ".");
        }
        softly.assertFalse(page.inputValue("approvalDate").isBlank(), "Approved " + code + " has no approval date.");
        softly.assertAll();
        if (page.isActionEnabled("delete")) {
            // QC 2026-11-05: delete is enabled on approved invoices. Question for the analysis team.
            log.warn("\"حذف\" is enabled on Approved {} (open question, not asserted).", code);
        }
    }

    // =====================================================================
    // Edit
    // =====================================================================

    @Test(priority = 11, groups = {LPI, "edit"},
            description = "Edit a Draft invoice (created by the test): header changed (supplier, branch and warehouse "
                    + "kept) and every tab "
                    + "filled as when creating (item line, discounts & additions line, payment terms); the header "
                    + "is kept after save and reload; it stays a Draft")
    public void userCanEditDraftForeignPurchaseInvoice() {

        createOwnInvoiceRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        // Same steps as create / copy / add (asked by the user, 2026-11-05), with
        // header values DIFFERENT from the saved ones.
        changeHeader();
        addItemLine();
        addDiscountAdditionLine();
        selectPaymentTermsTemplate();
        Map<String, String> entered = page.readHeaderValues(TestData.PERSISTED_HEADER_FIELDS);
        String code = saveAndVerify(DRAFT);

        page.reload();
        // After a reload some fields (the warehouse on quotations, 2026-11-02) fill in a
        // moment after the others: wait (max 10 s) until the header matches or stops changing.
        Map<String, String> saved = page.readHeaderValues(TestData.PERSISTED_HEADER_FIELDS);
        long end = System.currentTimeMillis() + 10_000;
        while (!saved.equals(entered) && System.currentTimeMillis() < end) {
            pause(500);
            saved = page.readHeaderValues(TestData.PERSISTED_HEADER_FIELDS);
        }
        assertEquals(saved, entered, "Edited header of " + code + " was not kept after reload.");
    }

    @Test(priority = 12, groups = {LPI, "edit", "lines"},
            description = "Edit a Draft (created by the test), change the supplier (\"استمرار\" clears the lines), "
                    + "add a line: its total must be calculated (quantity × cost); nothing is saved")
    public void lineTotalIsCalculatedAfterChangingSupplierOfDraft() {

        // QC 2026-11-05 (LPI2611T16-000215, item تست صنف جيمي, by hand): after the
        // supplier change the new line showed quantity 17, cost 100 and total 0.000
        // (also after entering them again); the discount % was then rejected. In the
        // same edit WITHOUT the supplier change the total was 1,545.455. Expected to
        // FAIL until the system is fixed (reported to the analysis team). The same happens
        // after changing the BRANCH in edit mode (QC run 2026-11-05 16:23, LPI2611T46-000002).
        String code = createOwnInvoiceRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        String supplier = page.selectRandomOptionAndContinue("vendorId", true);
        log.info("{}: supplier changed to [{}]; lines now {}.", code, supplier, page.lineCount());

        page.openTab(ForeignInvoicePage.TAB_LINES);
        page.deleteEmptyItemLines();
        int row = page.addLine();
        String item = null;
        for (int attempt = 1; attempt <= TestData.MAX_ITEM_ATTEMPTS && item == null; attempt++) {
            String chosen = page.selectRandomItem(row);
            if (page.readLineCell(row, "uomId").isEmpty()) {
                form.closeEmptyItemSearchDialog();
                continue;
            }
            item = chosen;
        }
        assertTrue(item != null, "No accepted item found for the new line.");
        assertTrue(page.fillLineCellIfEditable(row, "quantity", TestData.QUANTITY, true), "\"الكمية\" is disabled.");
        assertTrue(page.fillLineCellIfEditable(row, ForeignInvoicePage.PRICE_COLUMN, TestData.PRICE, true),
                "\"التكلفة\" is disabled.");

        java.math.BigDecimal total = page.readLineNumber(row, "total");
        log.info("{} after the supplier change: line {} ({}) {} × {} → total {}.",
                code, row + 1, item, TestData.QUANTITY, TestData.PRICE, total);
        assertTrue(total.signum() > 0, "SYSTEM ISSUE: after changing the supplier of Draft " + code
                + " in edit mode, the new line (" + item + ", " + TestData.QUANTITY + " × " + TestData.PRICE
                + ") has total " + total + ".");

        page.closeForm(); // nothing is saved
    }

    @Test(priority = 13, groups = {LPI, "edit", "status"},
            description = "Approved invoice in edit mode: only the supplier and the description can change, plus the "
                    + "due date when shown (credit invoice); nothing is saved")
    public void approvedForeignPurchaseInvoiceAllowsSupplierAndDescriptionEditOnly() {

        openRandomInvoice(ForeignInvoicePage::isApproved);
        String code = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        waitUntil("\"البيان\" editable on " + code, Duration.ofSeconds(10),
                () -> page.isInputEditable(ForeignInvoicePage.NOTES_FIELD));

        List<String> editable = page.editableHeaderInputs();
        log.info("Approved {} in edit mode: editable {}", code, editable);
        Set<String> allowed = new HashSet<>(TestData.EDITABLE_ON_APPROVED);
        allowed.addAll(TestData.EDITABLE_ON_APPROVED_WHEN_SHOWN);
        List<String> notAllowed = editable.stream().filter(f -> !allowed.contains(f)).toList();
        List<String> missing = TestData.EDITABLE_ON_APPROVED.stream().filter(f -> !editable.contains(f)).toList();

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(notAllowed.isEmpty(), "Approved " + code + ": these fields must not be editable: " + notAllowed);
        softly.assertTrue(missing.isEmpty(), "Approved " + code + ": these fields must be editable: " + missing);
        softly.assertAll();
        if (editable.contains("deservedDate")) {
            log.warn("Approved {}: \"تاريخ الاستحقاق\" is editable (credit invoice). Open question, not a failure.", code);
        }

        page.closeForm(); // confirms "leave without saving" if asked
    }

    // =====================================================================
    // Delete / approve / log
    // =====================================================================

    @Test(priority = 14, groups = {LPI, "status"},
            description = "Delete a Draft invoice (created by the test): success message, gone from list and API")
    public void userCanDeleteDraftForeignPurchaseInvoice() {

        String code = createOwnInvoiceRequiring(DRAFT);
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
        assertTrue(page.api().find(r -> code.equals(ForeignInvoicePage.codeOf(r))).isEmpty(),
                code + " is still returned by the API after deletion.");
    }

    @Test(priority = 15, groups = {LPI, "status", "log"},
            description = "Approve a Draft invoice (created by the test): Approved after reload, approval date today, "
                    + "approve button gone, journal entry enabled, one approval log entry")
    public void userCanApproveDraftForeignPurchaseInvoice() {

        String code = createOwnInvoiceRequiring(DRAFT);
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE, () -> {
            assertTrue(page.waitForActionEnabled("approveOrDisapprove"), "\"اعتماد\" is disabled on " + code + ".");
            page.clickApproveOrDisapprove();
            page.confirmDialog();
        });
        assertAccepted("Approving " + code, TestData.API_APPROVE, outcome);

        page.reload();
        page.waitForStatusLabel(APPROVED.label()); // kept on the server
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ الاعتماد\" of " + code + " is not today.");
        softly.assertFalse(page.isActionEnabled("approveOrDisapprove"),
                "Approved " + code + " still offers approve / disapprove.");
        softly.assertTrue(page.isActionEnabled("journalEntry"), "\"قيد اليومية\" is disabled on Approved " + code + ".");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Approving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (!logAfter.isEmpty()) {
            softly.assertEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The last log entry of " + code + " is not the approval.");
        }
        softly.assertAll();
    }

    @Test(priority = 16, groups = {LPI, "log"},
            description = "Transaction log: first entry is the creation; an Approved invoice has an approval entry")
    public void userCanViewForeignPurchaseInvoiceTransactionLog() {

        Map<String, Object> record = openRandomInvoice(r -> true);
        String code = page.waitForCode();
        List<String> actions = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log of " + code + " is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), TestData.LOG_CREATED,
                "The first log entry of " + code + " must be the creation.");
        if (ForeignInvoicePage.isApproved(record)) {
            softly.assertTrue(actions.contains(TestData.LOG_APPROVED),
                    "Approved " + code + " has no approval entry in its log: " + actions);
        }
        softly.assertAll();
    }

    // =====================================================================
    // List and exit
    // =====================================================================

    @Test(priority = 17, groups = {LPI, "search"},
            description = "List search by number shows that invoice")
    public void userCanSearchForeignPurchaseInvoicesListByCode() {

        openRandomInvoice(r -> true);
        String code = page.waitForCode();
        page.closeForm();
        page.waitForListLoaded();

        page.searchList(code);
        assertTrue(page.waitUntilListShows(code),
                "Searching for " + code + " did not show it. Rows shown: " + page.visibleListCodes());
    }

    @Test(priority = 18, groups = {LPI, "exit"},
            description = "Close a new and a viewed invoice: each returns to the list "
                    + "(the new one after confirming)")
    public void userCanExitForeignPurchaseInvoice() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.selectRandomOption("vendorId"); // something to lose
        page.confirmDialogIfShown(Duration.ofMillis(800));
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing a new invoice did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing a new invoice");

        openRandomInvoice(r -> true);
        String viewed = page.waitForCode();
        page.clickAction("xIcon");
        assertBackOnList("closing " + viewed + " in view mode");
    }

    // =====================================================================
    // Undo
    // =====================================================================

    @Test(priority = 19, groups = {LPI, "edit"},
            description = "Undo in edit mode (Draft created by the test): the saved header and lines come back")
    public void userCanUndoForeignPurchaseInvoiceChanges() {

        String code = createOwnInvoiceRequiring(DRAFT);
        Map<String, String> saved = stableHeader(TestData.UNDO_HEADER_FIELDS);
        int savedLines = page.lineCount();
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        changeHeader();
        addItemLine();
        log.info("Before undo: {} ({} line(s)); saved: {} ({} line(s)).",
                page.readHeaderValues(TestData.UNDO_HEADER_FIELDS), page.lineCount(), saved, savedLines);

        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        page.clickUndo();
        page.confirmDialog();

        assertTrue(driver.getCurrentUrl().matches(".*/" + id + "([/?#].*)?$"),
                "Undo left invoice " + code + " (id " + id + "): " + driver.getCurrentUrl());
        SoftAssert softly = new SoftAssert();
        // The number is checked with the header: changing the operation type in edit
        // shows the next number of its series (QC 2026-11-05: 000082 → 000083).
        Map<String, String> savedWithCode = new LinkedHashMap<>();
        savedWithCode.put(CODE_LABEL, code);
        savedWithCode.putAll(saved);
        Map<String, String> notRestored = new LinkedHashMap<>();
        savedWithCode.forEach((field, value) -> {
            String shown = waitUntilEquals(() -> undoFieldValue(field), value);
            if (!value.equals(shown)) {
                notRestored.put(field, shown);
            }
        });
        softly.assertEquals(waitUntilEquals(() -> String.valueOf(page.lineCount()), String.valueOf(savedLines)),
                String.valueOf(savedLines), "Undo did not remove the added line of " + code + ".");

        if (!notRestored.isEmpty()) {
            // QC 2026-11-05: after changing the operation type, undo kept either the
            // operation type or the number it gave. A reload tells a screen-only undo
            // bug from a change that was really saved.
            log.warn("Not restored by undo: {}", notRestored);
            page.reload();
            notRestored.forEach((field, shown) -> {
                String expected = savedWithCode.get(field);
                String afterReload = waitUntilEquals(() -> undoFieldValue(field), expected);
                softly.assertEquals(shown, expected, expected.equals(afterReload)
                        ? "SYSTEM ISSUE: \"تراجع\" did not restore \"" + field + "\" of " + code
                                + " on the screen (it kept the edited value; after a reload the saved value shows)."
                        : "\"" + field + "\" of " + code + " was changed although nothing was saved"
                                + " (after a reload it shows [" + afterReload + "]).");
            });
        }
        softly.assertAll();
    }

    private static final String CODE_LABEL = "الرقم المرجعي";

    /** A header value for the undo check; {@link #CODE_LABEL} reads the invoice number. */
    private String undoFieldValue(String field) {
        return CODE_LABEL.equals(field) ? page.code() : page.readHeaderValues(List.of(field)).get(field);
    }

    // =====================================================================
    // Record search / navigation / list pagination / Kanban
    // =====================================================================

    @Test(priority = 20, groups = {LPI, "search"},
            description = "Record search (magnifier) in the form opens the invoice typed")
    public void userCanOpenForeignPurchaseInvoiceFromRecordSearch() {

        openRandomInvoice(r -> true);
        String target = page.waitForCode();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
        assertNotEquals(waitUntilChanges(target), target, "Could not move away from " + target + ".");

        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 21, groups = {LPI, "navigation"},
            description = "Record navigation in the form: first / next / back / last, and the buttons disabled at each end")
    public void userCanNavigateBetweenForeignPurchaseInvoiceRecords() {

        openRandomInvoice(r -> true);
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
        softly.assertNotEquals(second, first, "'Next' did not open another invoice.");

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

    @Test(priority = 22, groups = {LPI, "pagination"},
            description = "List pagination: first / last / page by page; rows match the summary; "
                    + "a random row of a random page opens that invoice")
    public void userCanNavigateForeignPurchaseInvoicesPagination() {

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

        // Total of the list = invoices in the API (QC 2026-11-05: 400 / 400).
        softly.assertEquals(total, page.api().count(), "The list total does not match the invoices in the API.");

        // A few pages with "Next" (not all 40: the server answers 429 to fast paging, see the guide).
        page.goToFirstListPage();
        int pageSize = first[1] - first[0] + 1;
        int pages = Math.min(3, (total + pageSize - 1) / pageSize);
        for (int n = 2; n <= pages; n++) {
            int[] before = page.paginationRange();
            page.goToNextPage();
            int[] now = page.paginationRange();
            softly.assertEquals(now[0], before[1] + 1, "Page " + n + " does not follow the previous one: "
                    + Arrays.toString(before) + " → " + Arrays.toString(now));
            softly.assertEquals(page.visibleRowCount(), now[1] - now[0] + 1,
                    "Rows on page " + n + " do not match the summary " + Arrays.toString(now) + ".");
        }
        if (pages > 1) {
            int[] before = page.paginationRange();
            page.goToPreviousPage();
            softly.assertEquals(page.paginationRange()[1], before[0] - 1, "\"Previous\" did not go back one page.");
        }
        softly.assertAll();

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        log.info("Pagination: {} record(s), {} per page → opening {} from page {}.", total, pageSize, code,
                Arrays.toString(page.paginationRange()));
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another invoice than " + code + ".");
    }

    @Test(priority = 23, groups = {LPI, "list"},
            description = "Kanban view: one card per invoice of the list page (supplier, amount), each with a known "
                    + "status; double-clicking a card opens that invoice; back to the list view")
    public void userCanSwitchForeignPurchaseInvoicesListToKanbanAndBack() {

        page.waitForListLoaded();
        // QC 2026-11-05: invoice cards have no number, so they are matched by supplier and amount.
        // The card amount is the total BEFORE tax (API "total": 2000 for net 2300); the list only
        // shows "net" (with tax), so the amount of each listed invoice is read from the API.
        Map<String, Map<String, String>> rows = page.readListRows(List.of("invoiceCode", "parterName", "net"));
        Map<String, Object> totalByCode = new java.util.HashMap<>();
        page.api().findAll().forEach(r -> totalByCode.put(ForeignInvoicePage.codeOf(r), r.get("total")));
        List<String> listKeys = rows.values().stream()
                .map(r -> {
                    Object total = totalByCode.get(r.get("invoiceCode"));
                    assertNotNull(total, "Invoice " + r.get("invoiceCode") + " of the list is not in the API.");
                    return r.getOrDefault("parterName", "") + " | "
                            + new java.math.BigDecimal(String.valueOf(total)).setScale(2, java.math.RoundingMode.HALF_UP);
                })
                .sorted().toList();
        try {
            page.switchToKanbanView();
            List<Map<String, String>> cards = page.kanbanCards();
            List<String> cardKeys = cards.stream()
                    .map(c -> c.get("supplier") + " | " + ForeignInvoicePage.amountOf(c.get("amount")).setScale(2,
                            java.math.RoundingMode.HALF_UP))
                    .sorted().toList();
            log.info("List page: {} | Kanban: {}", listKeys, cardKeys);

            SoftAssert softly = new SoftAssert();
            softly.assertEquals(cardKeys, listKeys, "Kanban cards (supplier | amount) do not match the list page.");
            for (Map<String, String> card : cards) {
                softly.assertTrue(TestData.CARD_STATUSES.contains(card.get("status")),
                        "Card " + card + " has an unknown status.");
            }
            softly.assertAll();

            int index = new Random().nextInt(cards.size());
            Map<String, String> card = cards.get(index);
            page.openKanbanCardAt(index);
            String code = page.waitForCode();
            SoftAssert opened = new SoftAssert();
            opened.assertTrue(page.selectedText("vendorId").endsWith(card.get("supplier")),
                    "Card " + card + " opened " + code + " of supplier [" + page.selectedText("vendorId") + "].");
            opened.assertEquals(page.statusLabel(), card.get("status"), "Card " + card + " opened " + code
                    + " with another status.");
            opened.assertAll();
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView(); // leave the list as the other tests expect it
            }
        }
    }

    // =====================================================================
    // Print / Excel
    // =====================================================================

    @Test(priority = 24, groups = {LPI, "print"},
            description = "Print: the preview shows the number, supplier, branch and warehouse; then it is printed. "
                    + "(This screen's preview has no PDF button, so there is no \"save as PDF\" test.)")
    public void userCanPrintForeignPurchaseInvoice() throws java.awt.AWTException {

        openRandomInvoice(r -> true);
        String code = page.waitForCode();
        String supplier = page.selectedText("vendorId");
        String branch = page.selectedText("branchId");
        String warehouse = page.selectedText("warehouseId");

        String preview = page.openPrintPreviewWithRandomTemplate();

        // The preview shows the supplier NAME only ("PG005002 - اشرف الرشيدي" → "اشرف الرشيدي").
        String supplierName = supplier.contains(" - ") ? supplier.substring(supplier.indexOf(" - ") + 3) : supplier;
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(preview.contains(code), "Print preview does not show " + code + ".");
        for (String[] shown : new String[][]{{"supplier", supplierName}, {"branch", branch}, {"warehouse", warehouse}}) {
            if (!shown[1].isBlank() && !shown[1].equals("اختر")) {
                softly.assertTrue(preview.contains(shown[1]),
                        "Print preview of " + code + " does not show the " + shown[0] + " [" + shown[1] + "].");
            }
        }
        softly.assertAll();

        page.printFromPreview();
        page.confirmChromePrintDialog();
        page.closePrintPreview();
        assertEquals(page.waitForCode(), code, "After printing, the form does not show " + code + " any more.");
    }

    @Test(priority = 25, groups = {LPI, "excel"},
            description = "\"⋮\" → \"التصدير الى Excel\": an .xlsx with one row per item line")
    public void userCanExportForeignPurchaseInvoiceToExcel() {

        openRandomInvoice(r -> true);
        String code = page.waitForCode();
        page.waitForGridToSettle();
        int lines = page.lineCount();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < lines; row++) {
            items.add(page.readLineCell(row, "code"));
        }
        Path dir = page.prepareDownloadFolder("excel");

        page.clickMenuItem(ForeignInvoicePage.MENU_EXPORT_EXCEL);

        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        int rows = Downloads.xlsxRowCount(xlsx);
        log.info("Excel of {}: {} ({} bytes), {} row(s); {} line(s): {}",
                code, xlsx.getFileName(), Downloads.sizeOf(xlsx), rows, lines, items);

        // As on orders and quotations (assumed here, confirm on the first run): a title row
        // and a header row, then one row per line.
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(rows - 2, lines, "Excel rows do not match the item lines of " + code + ".");
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Follow-up screens
    // =====================================================================

    @Test(priority = 26, groups = {LPI, "follow-up"},
            description = "Items pricing ($): opens the pricing screen with the items of this invoice (by item ids)")
    public void userCanOpenItemsPricingFromForeignPurchaseInvoice() {

        openRandomInvoice(r -> true);
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
        // QC 2026-11-05: a foreign invoice opens the pricing screen by item ids (/sales/pricing/add?itemIds=…).
        softly.assertTrue(url.contains("/sales/pricing/add") && url.contains(ForeignInvoicePage.ITEMS_PRICING_BY_ITEMS),
                "Items pricing of " + code + " did not open the pricing screen by item ids: " + url);
        softly.assertEquals(new HashSet<>(priced), new HashSet<>(items), "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 27, groups = {LPI, "follow-up", "print"},
            description = "Print barcode: opens Item Barcodes loaded from this invoice, with its items")
    public void userCanOpenPrintBarcodeFromForeignPurchaseInvoice() {

        openRandomInvoice(r -> true);
        String code = page.waitForCode();
        if (!page.hasAction("printBarcode") || !page.waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is not available on " + code + ".");
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

    @Test(priority = 28, groups = {LPI, "follow-up", "accounting"},
            description = "Journal entry of an Approved invoice: lines shown, debit = credit = the invoice total")
    public void userCanViewJournalEntryOfApprovedForeignPurchaseInvoice() {

        Map<String, Object> record = openRandomInvoice(ForeignInvoicePage::isApproved);
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("journalEntry"), "\"قيد اليومية\" is disabled on Approved " + code + ".");

        ForeignInvoicePage.JournalEntry entry = page.readJournalEntry();
        BigDecimal net = new BigDecimal(String.valueOf(record.get("net")));

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(entry.title(), TestData.JOURNAL_ENTRY_TITLE, "Unexpected dialog for " + code + ".");
        softly.assertTrue(entry.lines() > 0, "The journal entry of " + code + " has no lines.");
        softly.assertTrue(entry.debit().subtract(entry.credit()).abs().compareTo(TestData.CENT) < 0,
                "Journal entry of " + code + " is not balanced: debit " + entry.debit() + ", credit " + entry.credit() + ".");
        softly.assertTrue(entry.credit().subtract(net).abs().compareTo(TestData.CENT) < 0,
                "Journal entry of " + code + " (" + entry.credit() + ") does not equal the invoice total " + net + ".");
        softly.assertAll();
    }

    @Test(priority = 29, groups = {LPI, "follow-up"},
            description = "\"⋮\" → \"انشاء مردود مشتريات\" on an Approved invoice opens a return with this invoice as "
                    + "reference, the same supplier and its lines (nothing is saved)")
    public void userCanCreatePurchaseReturnFromApprovedForeignPurchaseInvoice() {
        throw new SkipException("Not applicable: \"⋮\" of a foreign invoice has only \"التصدير الى Excel\" and \"دفع\" (QC 2026-11-05).");
    }

    @Test(priority = 30, groups = {LPI, "follow-up", "accounting"},
            description = "\"⋮\" → \"إنشاء إشعار مدين\" on an Approved invoice opens a debit note with this invoice "
                    + "as source document (nothing is saved)")
    public void userCanCreateDebitNoteFromApprovedForeignPurchaseInvoice() {
        throw new SkipException("Not applicable: \"⋮\" of a foreign invoice has only \"التصدير الى Excel\" and \"دفع\" (QC 2026-11-05).");
    }

    @Test(priority = 31, groups = {LPI, "follow-up", "accounting"},
            description = "\"⋮\" → \"إنشاء إشعار دائن\" on an Approved invoice opens a credit note with this invoice "
                    + "as source document (nothing is saved)")
    public void userCanCreateCreditNoteFromApprovedForeignPurchaseInvoice() {
        throw new SkipException("Not applicable: \"⋮\" of a foreign invoice has only \"التصدير الى Excel\" and \"دفع\" (QC 2026-11-05).");
    }

    @Test(priority = 32, groups = {LPI, "follow-up", "payment"},
            description = "\"⋮\" → \"دفع\" on an unpaid Approved invoice: with a payment schedule the installments dialog "
                    + "opens and its \"دفع\" is disabled until one is selected; without one the payment form opens "
                    + "(nothing is paid)")
    public void userCanOpenPaymentOfApprovedForeignPurchaseInvoice() {

        openRandomInvoice(r -> ForeignInvoicePage.isApproved(r)
                && r.get("remaining") instanceof Number left && left.doubleValue() > 0);
        String code = page.waitForCode();

        ForeignInvoicePage.PayDialog dialog = page.openPayDialog();
        log.info("Pay dialog of {}: {}", code, dialog);
        SoftAssert softly = new SoftAssert();
        if (TestData.PAY_DIALOG_TITLE.equals(dialog.title())) {
            // invoice with a payment schedule: installments, "دفع" only after ticking one
            softly.assertTrue(dialog.installments() > 0, "The pay dialog of " + code + " lists no installment.");
            softly.assertFalse(dialog.payEnabled(), "\"دفع\" must be disabled before an installment is selected.");
            if (dialog.installments() > 0) {
                page.selectFirstInstallment();
                softly.assertTrue(waitUntilEquals(() -> String.valueOf(page.readPayDialog().payEnabled()), "true")
                        .equals("true"), "\"دفع\" stayed disabled after selecting an installment of " + code + ".");
            }
        } else {
            // invoice without a payment schedule (QC 2026-11-05): the direct payment form
            softly.assertEquals(dialog.title(), TestData.DIRECT_PAY_DIALOG_TITLE,
                    "Unexpected dialog for \"دفع\" on " + code + ".");
            softly.assertTrue(dialog.inputs().containsAll(TestData.DIRECT_PAY_FIELDS),
                    "The payment form of " + code + " lacks fields " + TestData.DIRECT_PAY_FIELDS + ": " + dialog.inputs());
            softly.assertTrue(dialog.description().contains(code),
                    "The payment description does not name " + code + ": [" + dialog.description() + "].");
        }
        page.closeOpenDialogWith("Close"); // nothing is paid
        softly.assertAll();
    }

    @Test(priority = 33, groups = {LPI, "follow-up", "status"},
            description = "Draft invoice: \"انشاء مردود مشتريات\" must not open a return (only Approved invoices "
                    + "can be returned)")
    public void purchaseReturnCannotBeCreatedFromDraftForeignPurchaseInvoice() {
        throw new SkipException("Not applicable: \"⋮\" of a foreign invoice has only \"التصدير الى Excel\" and \"دفع\" (QC 2026-11-05).");
    }

    // =====================================================================
    // Attachments
    // =====================================================================

    @Test(priority = 34, groups = {LPI, "attachments"},
            description = "Add attachments to a Draft (created by the test): listed, and kept after save and reload")
    public void userCanAddAttachmentsToForeignPurchaseInvoice() {

        String code = createOwnInvoiceRequiring(DRAFT);
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

        saveAndVerify(DRAFT);
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before + files), before + files,
                "Attachments of " + code + " were not kept after save and reload.");
    }

    @Test(priority = 35, groups = {LPI, "attachments"},
            description = "Delete an attachment of a Draft (created by the test, with attachments): gone after save and reload")
    public void userCanDeleteAttachmentFromForeignPurchaseInvoice() {

        String code = createOwnInvoiceRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        int files = page.countAttachmentFiles();
        page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
        int before = page.waitForAttachmentCount(files);
        page.closeAttachments();
        saveAndVerify(DRAFT);

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        log.info("{} has {} attachment(s); deleting the last one.", code, before);
        page.deleteLastAttachment();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1,
                "The deleted attachment is still listed in 'تفاصيل المرفق'.");
        page.closeAttachments();
        page.waitForToastToAppearAndClear(); // the delete toast must not be read as the save toast

        saveAndVerify(DRAFT);
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1,
                "The deleted attachment of " + code + " came back after save and reload.");
    }

    // =====================================================================
    // Lines
    // =====================================================================

    @Test(priority = 36, groups = {LPI, "lines"},
            description = "A new invoice keeps its item lines (item × quantity @ cost) after save and reload")
    public void savedForeignPurchaseInvoiceKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        // Prices EXCLUDE tax here, as on quotations (2026-11-02): with "شاملة الضريبة" a
        // cost can come back changed by the tax; tax handling is level 3.
        if (!"priceNature-0".equals(page.priceNature())) {
            page.selectOtherPriceNature();
        }
        addItemLine();
        page.deleteEmptyItemLines();
        List<String> before = linesSnapshot();
        log.info("Lines before save: {}", before);

        saveAndVerify(TestData.NEW_INVOICE_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        List<String> after = linesSnapshot();

        assertEquals(after.stream().sorted().toList(), before.stream().sorted().toList(),
                "Lines changed after save and reload.");
    }

    @Test(priority = 37, groups = {LPI, "lines", "edit"},
            description = "Edit quantity and cost of an existing line of a Draft (created by the test): saved values kept")
    public void userCanEditQuantityAndCostOfExistingLine() {

        String code = createOwnInvoiceRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        String item = page.readLineCell(0, "code");
        BigDecimal quantity = page.readLineNumber(0, "quantity").add(BigDecimal.ONE);
        BigDecimal cost = page.readLineNumber(0, ForeignInvoicePage.PRICE_COLUMN).add(BigDecimal.TEN);
        assertTrue(page.fillLineCellIfEditable(0, "quantity", quantity.toPlainString(), true),
                "\"الكمية\" of line 1 of " + code + " is disabled.");
        assertTrue(page.fillLineCellIfEditable(0, ForeignInvoicePage.PRICE_COLUMN, cost.toPlainString(), true),
                "\"التكلفة\" of line 1 of " + code + " is disabled.");
        log.info("{} line 1 ({}): quantity → {}, cost → {}", code, item, quantity, cost);

        saveAndVerify(DRAFT);
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");

        // QC 2026-11-05 (LPI2611T40-000079, id 508): opened in edit mode (/edit/<id>) the line
        // showed cost 115.00 while the view showed the saved 110.00. When the form is still in
        // edit mode after the save, its value is compared too; the saved values are judged in the view.
        page.reload();
        page.waitForGridToSettle();
        String reloadedUrl = driver.getCurrentUrl();
        boolean editUrl = reloadedUrl.contains("/edit/");
        int editRow = rowOfItem(item);
        BigDecimal editCost = editRow < 0 ? null : page.readLineNumber(editRow, ForeignInvoicePage.PRICE_COLUMN);

        page.openDocument(id);
        page.waitForGridToSettle();
        int row = rowOfItem(item);
        assertTrue(row >= 0, "Item " + item + " is not in the lines of " + code + " after save.");
        BigDecimal savedQuantity = page.readLineNumber(row, "quantity");
        BigDecimal savedCost = page.readLineNumber(row, ForeignInvoicePage.PRICE_COLUMN);
        BigDecimal savedTotal = page.readLineNumber(row, "total");
        log.info("{} after save: view quantity {}, cost {}, total {}; reloaded form ({}) cost {}", code, savedQuantity,
                savedCost, savedTotal, reloadedUrl, editCost);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(savedQuantity.compareTo(quantity), 0,
                "Quantity of " + item + " was not saved as " + quantity + " (found " + savedQuantity + ").");
        softly.assertEquals(savedCost.compareTo(cost), 0,
                "Cost of " + item + " was not saved as " + cost + " (found " + savedCost + ").");
        // "الإجمالي" of a line is before tax: with prices INCLUDING tax it is quantity × cost
        // / (1 + tax rate). QC 2026-11-05: 18 × 110 / 1.10 = 1,800 and 18 × 110 / 1.20 = 1,650.
        String nature = page.priceNature();
        BigDecimal taxRate = page.readLineNumber(row, "taxRate");
        BigDecimal expectedTotal = quantity.multiply(cost);
        // priceNature() gives the radio id: priceNature-0 = "باستثناء الضريبة", priceNature-1 = "شاملة الضريبة".
        if (nature.contains("شامل") || nature.equals("priceNature-1")) {
            expectedTotal = expectedTotal.divide(BigDecimal.ONE.add(taxRate.movePointLeft(2)), 3, java.math.RoundingMode.HALF_UP);
        }
        softly.assertTrue(savedTotal.subtract(expectedTotal).abs().compareTo(new BigDecimal("0.01")) <= 0,
                "SYSTEM ISSUE: after editing line 1 of " + code + " the saved line total is " + savedTotal
                        + " instead of " + expectedTotal + " (" + quantity + " × " + cost + ", prices [" + nature
                        + "], tax " + taxRate + "%).");
        if (editCost != null) {
            // QC 2026-11-05: the form reloaded right after the save showed 115.00 (LPI2611T16-000217).
            softly.assertEquals(editCost.compareTo(savedCost), 0, "SYSTEM ISSUE: " + code + " shows cost " + editCost
                    + " in the form reloaded after the save (" + reloadedUrl + ") but " + savedCost + " in the view.");
        }
        softly.assertAll();
    }

    /** Index of the line of item {@code code}, or -1. */
    private int rowOfItem(String code) {
        for (int r = 0; r < page.lineCount(); r++) {
            if (code.equals(page.readLineCell(r, "code"))) {
                return r;
            }
        }
        return -1;
    }

    @Test(priority = 38, groups = {LPI, "lines"},
            description = "Delete an item line of a new invoice with two lines: the line is gone after save and reload")
    public void userCanDeleteItemLineOfForeignPurchaseInvoice() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        addItemLine(); // two lines, so one remains after the delete
        page.deleteEmptyItemLines();
        int before = page.lineCount();
        String deleted = page.readLineCell(0, "code");
        log.info("{} line(s); deleting line 1 ({}).", before, deleted);

        page.deleteLine(0);
        assertEquals(page.lineCount(), before - 1, "Deleting a line did not remove it from the grid.");

        saveAndVerify(TestData.NEW_INVOICE_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineCount(), before - 1, "The deleted line came back after save and reload.");
        for (int row = 0; row < page.lineCount(); row++) {
            softly.assertNotEquals(page.readLineCell(row, "code"), deleted, "Deleted item " + deleted + " is still a line.");
        }
        softly.assertAll();
    }

    @Test(priority = 39, groups = {LPI, "lines"},
            description = "Delete the discounts & additions line of a new invoice: gone after save and reload")
    public void userCanDeleteDiscountAdditionLine() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        addDiscountAdditionLine();

        // QC 2026-11-05 (user report): the grid adds an EMPTY line after the filled one, and the
        // test deleted that empty last line and passed while the real line stayed. The filled
        // line (with an account) is deleted, and only filled lines are counted.
        page.openTab(ForeignInvoicePage.TAB_DISCOUNTS_ADDITIONS);
        List<Integer> filled = filledDiscountAdditionRows();
        assertEquals(filled.size(), 1, "Expected one filled discounts & additions line (rows " + filled + ").");
        String account = page.readLineCell(filled.get(0), "accountId");
        page.deleteLine(filled.get(0));
        assertTrue(filledDiscountAdditionRows().isEmpty(),
                "Deleting the discounts & additions line [" + account + "] did not remove it.");
        page.openTab(ForeignInvoicePage.TAB_LINES);

        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");
        page.openDocument(id);
        page.openTab(ForeignInvoicePage.TAB_DISCOUNTS_ADDITIONS);
        page.waitForGridToSettle();
        List<Integer> after = filledDiscountAdditionRows();
        assertTrue(after.isEmpty(), "The deleted discounts & additions line [" + account + "] came back in " + code
                + " after save: " + after.stream().map(r -> page.readLineCell(r, "accountId")).toList());
    }

    /** Rows of the discounts & additions tab that have an account (the grid also adds an empty line). */
    private List<Integer> filledDiscountAdditionRows() {
        List<Integer> rows = new ArrayList<>();
        for (int r = 0; r < page.lineCount(); r++) {
            if (!page.readLineCell(r, "accountId").isBlank()) {
                rows.add(r);
            }
        }
        return rows;
    }

    // =====================================================================
    // Create: "⋮" of a new invoice (Excel import, continue from a receipt order)
    // =====================================================================

    @Test(priority = 40, groups = {LPI, "create", "excel"},
            description = "New invoice whose lines are imported from Excel (\"⋮\" → \"الاستيراد من Excel\"): "
                    + "lines added, costs completed, saved as a new invoice that keeps them")
    public void userCanCreateForeignPurchaseInvoiceFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        // QC 2026-11-05: "⋮" of a NEW invoice = "الاستيراد من Excel" and "متابعة من أمر استلام".
        List<String> menu = page.menuItems();
        if (menu.stream().noneMatch(i -> i.replaceAll("\\s+", "").equals(ForeignInvoicePage.MENU_IMPORT_EXCEL.replaceAll("\\s+", "")))) {
            throw new SkipException("\"" + ForeignInvoicePage.MENU_IMPORT_EXCEL + "\" is not in ⋮ of a new invoice: " + menu);
        }
        closeMenuIfOpen();

        fillHeader();
        page.importLinesFromExcel(TestData.EXCEL_LINES);
        page.openTab(ForeignInvoicePage.TAB_LINES);
        page.waitForGridToSettle();
        int imported = page.lineCount();
        log.info("Lines imported from Excel: {}", imported);
        assertTrue(imported > 0, "Excel import finished but no lines were added.");

        completeExistingLines(); // the file has code and quantity only; costs 0 are filled
        int lines = 0;
        for (int r = 0; r < page.lineCount(); r++) {
            if (!page.readLineCell(r, "code").isEmpty()) {
                lines++; // the empty line the grid adds is removed by the save
            }
        }
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);

        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");
        page.openDocument(id);
        page.waitForGridToSettle();
        assertEquals(page.lineCount(), lines, "Invoice " + code + " (from Excel) did not keep its " + lines + " line(s).");
    }

    @Test(priority = 41, groups = {LPI, "create", "reference"},
            description = "New invoice continued from a receipt order (\"⋮\" → \"متابعة من أمر استلام\"): the order "
                    + "is chosen in its dialog and downloaded (supplier, branch, warehouse, lines); saved, reference kept")
    public void userCanCreateForeignPurchaseInvoiceFromReceiptOrder() {
        throw new SkipException("Not applicable: \"⋮\" of a new foreign invoice has only \"الاستيراد من Excel\" (QC 2026-11-05).");
    }

    // =====================================================================
    // Payment schedule ("جدولة الدفعات")
    // =====================================================================

    @Test(priority = 42, groups = {LPI, "payment"},
            description = "Payment terms template (\"قالب شروط السداد\") on a new invoice: the schedule's portions add "
                    + "up to 100 %, each due amount = portion × invoice total, they add up to the total, "
                    + "and each due date = invoice date + credit days")
    public void paymentScheduleFollowsPaymentTermsTemplate() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillInvoiceWithoutReference(); // includes a random payment terms template
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");

        page.openDocument(id);
        List<Map<String, String>> schedule = page.readPaymentSchedule();
        if (schedule.isEmpty() && DocumentStatus.fromLabel(page.statusLabel()) == DRAFT) {
            // QC 2026-11-05: the schedules seen were on Approved invoices (LPI2611T45-000007).
            // Whether a Draft shows its schedule was not confirmed, so the test's own Draft is approved.
            log.warn("Draft {} shows no payment schedule → approving it (test-owned) and reading again.", code);
            ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE, () -> {
                assertTrue(page.waitForActionEnabled("approveOrDisapprove"), "\"اعتماد\" is disabled on " + code + ".");
                page.clickApproveOrDisapprove();
                page.confirmDialog();
            });
            assertAccepted("Approving " + code, TestData.API_APPROVE, outcome);
            page.openDocument(id);
            schedule = page.readPaymentSchedule();
        }
        log.info("Payment schedule of {}: {}", code, schedule);
        assertFalse(schedule.isEmpty(), "Invoice " + code + " with a payment terms template has no payment schedule.");

        BigDecimal total = ForeignInvoicePage.amountOf(page.totalWithTax());
        LocalDate invoiceDate = LocalDate.parse(page.inputValue("invoiceDate").trim());
        BigDecimal portions = BigDecimal.ZERO;
        BigDecimal dueSum = BigDecimal.ZERO;
        SoftAssert softly = new SoftAssert();
        for (Map<String, String> line : schedule) {
            BigDecimal portion = ForeignInvoicePage.amountOf(line.get("invoicePortion"));
            BigDecimal due = ForeignInvoicePage.amountOf(line.get("dueAmount"));
            portions = portions.add(portion);
            dueSum = dueSum.add(due);
            BigDecimal expectedDue = total.multiply(portion).movePointLeft(2);
            softly.assertTrue(due.subtract(expectedDue).abs().compareTo(new BigDecimal("0.05")) <= 0,
                    line.get("paymentTermName") + " of " + code + ": due " + due + " ≠ " + portion + " % of " + total + ".");
            String days = line.getOrDefault("creditDays", "").trim();
            String dueDate = line.getOrDefault("dueDate", "").trim();
            if (days.matches("\\d+") && !dueDate.isEmpty()) {
                softly.assertEquals(dueDate, invoiceDate.plusDays(Long.parseLong(days)).toString(),
                        line.get("paymentTermName") + " of " + code + ": due date ≠ invoice date " + invoiceDate
                                + " + " + days + " day(s).");
            }
        }
        softly.assertEquals(portions.compareTo(TestData.FULL_PORTION), 0,
                "The portions of " + code + " add up to " + portions + " %, not 100 %.");
        softly.assertTrue(dueSum.subtract(total).abs().compareTo(TestData.CENT) <= 0,
                "The due amounts of " + code + " add up to " + dueSum + ", not the invoice total " + total + ".");
        softly.assertAll();
    }

    /** Waits until the "⋮" menu is closed ({@code menuItems()} closes it with Escape). */
    private void closeMenuIfOpen() {
        waitUntil("the ⋮ menu to close", Duration.ofSeconds(5), () -> driver.findElements(
                org.openqa.selenium.By.cssSelector("[role='menu'][data-state='open']")).isEmpty());
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Opens a random invoice matching {@code filter}, chosen through the API.
     * The test is SKIPPED when QC has none.
     */
    private Map<String, Object> openRandomInvoice(Predicate<Map<String, Object>> filter) {

        Map<String, Object> record = page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no local purchase invoice for this scenario."));
        page.waitForCode();
        return record;
    }

    /**
     * Creates and saves a new invoice for a test that changes it, and keeps
     * it open. SKIPPED when the company approves invoices on save and the
     * scenario needs another status.
     *
     * @return the new invoice's number.
     */
    private String createOwnInvoiceRequiring(DocumentStatus needed) {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader();
        addItemLine();
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);
        requireStatus(DocumentStatus.fromLabel(page.statusLabel()), needed);
        return code;
    }

    /** True while changing a saved invoice: every field gets a value DIFFERENT from the saved one. */
    private boolean editing;

    private void fillHeader() {
        editing = false;
        fillHeaderInScreenOrder();
    }

    /**
     * The old test's flow without a reference document, in its order:
     * header → item line (quantity, cost, discount %, discount, addition %,
     * addition, production date, expiry date, free quantity, note) →
     * "الخصومات والإضافات" line → "جدولة الدفعات" template. "مبلغ الفاتورة",
     * "رقم فاتورة المورد" and "البيان" are filled by {@link #fillHeader} and
     * {@link #saveAndVerify}.
     */
    private void fillInvoiceWithoutReference() {
        fillHeader();
        addItemLine();
        addDiscountAdditionLine();
        selectPaymentTermsTemplate();
    }

    private void changeHeader() {
        editing = true;
        try {
            fillHeaderInScreenOrder();
        } finally {
            editing = false;
        }
    }

    /**
     * Header in the SAME ORDER as the screen (right to left, row by row,
     * QC 2026-11-05): النوع → المورد → العملة → الفرع → المخزن → مركز التكلفة →
     * طريقة احتساب الضريبة → الأسعار → التاريخ → تاريخ الاستحقاق →
     * مندوب المشتريات → تعميم خصم (type + amount) → تكاليف الأصناف → الوسم →
     * رقم فاتورة المورد → البيان. Same fields as the old test.
     * Left as they are: طريقة الدفع (default "آجل"; "نقدا" would need حساب الدفع)
     * and the read-only fields.
     */
    private void fillHeaderInScreenOrder() {

        selectIfEditable("operationTypeCategoryId");
        if (editing) {
            // QC 2026-11-05: in edit mode, changing the supplier (LPI2611T16-000215) or the
            // branch (LPI2611T46-000002) asks "استمرار" and clears the lines; every new line
            // then keeps total 0 and its discount is rejected. Agreed with the user: the
            // edit test keeps supplier, branch and warehouse;
            // lineTotalIsCalculatedAfterChangingSupplierOfDraft shows the bug.
            log.info("Edit: supplier [{}], branch [{}] and warehouse [{}] kept (see "
                            + "lineTotalIsCalculatedAfterChangingSupplierOfDraft).", page.selectedText("vendorId"),
                    page.selectedText("branchId"), page.selectedText("warehouseId"));
            selectIfEditable("currencyId");
        } else {
            selectVendorBranchAndWarehouseUntilWarehouseIsFilled();
        }
        selectIfEditable("costCenterId");
        selectIfEditable("taxCalculationMethode");
        if (editing) {
            page.selectOtherPriceNature();
        } else if (page.priceNature().isEmpty()) {
            page.selectRandomPriceNature();
        }
        LocalDate today = LocalDate.now();
        page.setInvoiceDate(editing ? today.minusDays(1) : today);
        page.setDueDate(today.plusDays(TestData.DUE_IN_DAYS));
        selectIfEditable("purchaseRepresentativeId");
        setBulkDiscount();
        selectIfEditable("itemsCostsMethode");
        selectIfEditable("tagId");
        selectIfEditable("letterOfCredtId"); // "الاعتماد المستندي" (foreign only, old test order)
        selectIfEditable("classification"); // "التصنيف"

        String vendorCode = unusedVendorInvoiceCode();
        if (page.setVendorInvoiceCode(vendorCode)) {
            assertEquals(page.inputValue("vendorInvoiceCode").trim(), vendorCode, "\"رقم فاتورة المورد\" was not filled.");
        }
        String note = TestData.uniqueNote();
        page.setNotes(note);
        assertEquals(page.waitForNotes(note), note, "\"البيان\" was not filled.");
    }

    /** "تعميم خصم على الأصناف": random type + {@link TestData#BULK_DISCOUNT_AMOUNT} (kept when locked). */
    private void setBulkDiscount() {
        if (!page.waitForSelectEnabled("bulkDiscountType", Duration.ofSeconds(5))) {
            log.info("bulkDiscountType is locked → kept as [{}].", page.selectedText("bulkDiscountType"));
            return;
        }
        String type = page.setBulkDiscount(TestData.BULK_DISCOUNT_AMOUNT);
        log.info("تعميم خصم = {} / {}", type, TestData.BULK_DISCOUNT_AMOUNT);
    }

    /** "جدولة الدفعات" → random "قالب شروط السداد"; must not stay empty. */
    private void selectPaymentTermsTemplate() {
        String template = page.selectRandomPaymentTermsTemplate();
        assertFalse(template.isEmpty(), "\"قالب شروط السداد\" has no option or is locked.");
    }

    /** Random option in {@code field} when it is editable (a different one when editing). */
    private void selectIfEditable(String field) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, page.selectedText(field));
            return;
        }
        if (asksToContinue(field)) {
            page.selectRandomOptionAndContinue(field, editing);
            return;
        }
        if (editing) {
            page.selectRandomOtherOption(field);
        } else {
            page.selectRandomOption(field);
        }
        page.confirmDialogIfShown(Duration.ofMillis(800));
    }

    /** Fields whose change on a form WITH lines asks "استمرار" (old test: clickContinue after them). */
    private static final List<String> FIELDS_ASKING_TO_CONTINUE = List.of("vendorId", "branchId", "warehouseId");

    /**
     * True when choosing {@code field} can ask "استمرار": the supplier /
     * branch / warehouse of a form that already has lines (copy, edit, after
     * a download), where the old test called {@code clickContinue}. Then the
     * choice is made with {@code selectRandomOptionAndContinue} (choose →
     * استمرار → check).
     */
    private boolean asksToContinue(String field) {
        return FIELDS_ASKING_TO_CONTINUE.contains(field) && page.lineCount() > 0;
    }

    /**
     * المورد → العملة → الفرع → المخزن (new document). Some branches have no warehouse:
     * up to {@link TestData#BRANCH_ATTEMPTS_PER_SUPPLIER} branches are tried; when the
     * warehouse is still empty, ANOTHER supplier is chosen and the branch / warehouse
     * again, up to {@link TestData#MAX_SUPPLIER_ATTEMPTS} rounds.
     */
    private void selectVendorBranchAndWarehouseUntilWarehouseIsFilled() {

        retryUntilSuccessful("Supplier → Branch → Warehouse (warehouse must not stay empty)",
                TestData.MAX_SUPPLIER_ATTEMPTS, () -> {
            try {
                if (isEmptySelect("vendorId")) {
                    selectIfEditable("vendorId");
                } else {
                    selectAnotherOption("vendorId"); // the warehouse stayed empty with the previous one
                }
                selectIfEditable("currencyId");
                if (tryBranchesUntilWarehouseIsFilled(TestData.BRANCH_ATTEMPTS_PER_SUPPLIER)) {
                    return true;
                }
                log.warn("Supplier [{}]: warehouse still empty → another supplier, then branch and warehouse.",
                        page.selectedText("vendorId"));
                return false;
            } catch (RuntimeException failed) {
                log.warn("Supplier → Branch → Warehouse failed ({}) → again.", failed.getMessage());
                return false;
            }
        });
    }

    /**
     * الفرع → المخزن only (the supplier is kept, e.g. it came from a reference document),
     * up to {@link TestData#MAX_BRANCH_ATTEMPTS} branches.
     */
    private void selectBranchUntilWarehouseIsFilled() {

        if (!tryBranchesUntilWarehouseIsFilled(TestData.MAX_BRANCH_ATTEMPTS)) {
            fail("Branch → Warehouse: the warehouse stayed empty after " + TestData.MAX_BRANCH_ATTEMPTS
                    + " branches of supplier [" + page.selectedText("vendorId") + "].");
        }
    }

    /** Up to {@code attempts} branch → warehouse tries; true once the warehouse is filled. */
    private boolean tryBranchesUntilWarehouseIsFilled(int attempts) {

        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                if (selectBranchThenWarehouse()) {
                    return true;
                }
            } catch (RuntimeException failed) {
                log.warn("Branch → Warehouse failed ({}), attempt {}/{}.", failed.getMessage(), attempt, attempts);
            }
        }
        return false;
    }

    /** One branch (another one when a branch is already chosen) → warehouse; true when the warehouse is filled. */
    private boolean selectBranchThenWarehouse() {

        if (page.waitForSelectEnabled("branchId", Duration.ofSeconds(5))) {
            boolean otherBranch = editing || !isEmptySelect("branchId");
            if (asksToContinue("branchId")) {
                page.selectRandomOptionAndContinue("branchId", otherBranch);
            } else {
                if (otherBranch) {
                    page.selectRandomOtherOption("branchId");
                } else {
                    page.selectRandomOption("branchId");
                }
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }
        }
        if (page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(5))) {
            if (asksToContinue("warehouseId")) {
                page.selectRandomOptionAndContinue("warehouseId", editing);
            } else {
                if (editing) {
                    page.selectRandomOtherOption("warehouseId");
                } else {
                    page.selectRandomOption("warehouseId");
                }
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }
        }
        String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(3));
        if (warehouse.isEmpty() || warehouse.equals("اختر")) {
            log.warn("Branch [{}] gave no warehouse → another branch.", page.selectedText("branchId"));
            return false;
        }
        log.info("Branch [{}], warehouse [{}]", page.selectedText("branchId"), warehouse);
        return true;
    }

    /** Another random option in {@code field} (kept when locked); "استمرار" confirmed when asked. */
    private void selectAnotherOption(String field) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, page.selectedText(field));
            return;
        }
        if (asksToContinue(field)) {
            page.selectRandomOptionAndContinue(field, true);
            return;
        }
        page.selectRandomOtherOption(field);
        page.confirmDialogIfShown(Duration.ofMillis(800));
    }

    /** Empty select; "اختر" is the placeholder shown when nothing is chosen. */
    private boolean isEmptySelect(String field) {
        String text = page.selectedText(field);
        return text.isEmpty() || text.equals("اختر");
    }

    /** Numbers given out in this run (several invoices are saved per run). */
    private static final Set<String> usedVendorInvoiceCodes = new HashSet<>();

    /** "رقم فاتورة المورد" no invoice in QC has yet (read through the API) and not given out in this run. */
    private String unusedVendorInvoiceCode() {
        Set<String> taken = new HashSet<>(usedVendorInvoiceCodes);
        page.api().findAll().forEach(r -> taken.add(String.valueOf(r.get("vendorInvoiceCode")).trim()));
        String base = TestData.vendorInvoiceCode();
        String code = base;
        for (int n = 2; taken.contains(code); n++) {
            code = base + "-" + n;
        }
        usedVendorInvoiceCodes.add(code);
        return code;
    }

    /**
     * Adds one item line: random item, quantity and cost. Lines already in
     * the grid (copied / downloaded) are completed first. An item the
     * invoice does not accept never loads its unit; another item is tried.
     */
    private void addItemLine() {

        page.openTab(ForeignInvoicePage.TAB_LINES);
        completeExistingLines();
        page.deleteEmptyItemLines();
        int row = page.addLine();

        for (int attempt = 1; attempt <= TestData.MAX_ITEM_ATTEMPTS; attempt++) {
            String item = page.selectRandomItem(row);
            if (page.readLineCell(row, "uomId").isEmpty()) {
                form.closeEmptyItemSearchDialog();
                log.warn("Item {} did not load into line {} → another item ({}/{}).",
                        item, row + 1, attempt, TestData.MAX_ITEM_ATTEMPTS);
                continue;
            }
            String code = page.readLineCell(row, "code");
            if (isItemOnAnotherLine(code, row)) {
                log.warn("Item {} ({}) is already on another line → another item ({}/{}).",
                        item, code, attempt, TestData.MAX_ITEM_ATTEMPTS);
                continue;
            }
            fillItemLine(row);
            log.info("Line {}: {} × {} @ {}", row + 1, item, TestData.QUANTITY, TestData.PRICE);
            return;
        }
        fail("No accepted item found for the new line after " + TestData.MAX_ITEM_ATTEMPTS + " attempts.");
    }

    /**
     * The old test's line values, each checked in the cell after it is
     * entered: quantity, cost, discount %, discount, addition %, addition,
     * production date (today), expiry date, free quantity, note.
     */
    private void fillItemLine(int row) {
        // Required: must be entered.
        assertTrue(page.fillLineCellIfEditable(row, "quantity", TestData.QUANTITY, true),
                "\"الكمية\" of line " + (row + 1) + " is disabled.");
        assertTrue(page.fillLineCellIfEditable(row, ForeignInvoicePage.PRICE_COLUMN, TestData.PRICE, true),
                "\"التكلفة\" of line " + (row + 1) + " is disabled.");
        // Optional: a DISABLED cell is skipped (logged), as on orders (asked by the user, 2026-11-05).
        page.fillLineCellIfEditable(row, "discountRate", TestData.LINE_DISCOUNT_RATE, true);
        page.fillLineCellIfEditable(row, "discountValue", TestData.LINE_DISCOUNT_VALUE, true);
        page.fillLineCellIfEditable(row, "extraRate", TestData.LINE_EXTRA_RATE, true);
        page.fillLineCellIfEditable(row, "extraValue", TestData.LINE_EXTRA_VALUE, true);
        LocalDate today = LocalDate.now();
        page.setLineDate(row, "productionDate", today);
        page.setLineDate(row, "expireDate", today.plusDays(TestData.EXPIRY_IN_DAYS));
        page.fillLineCellIfEditable(row, "freeQuantities", TestData.FREE_QUANTITY, true);
        page.fillLineCellIfEditable(row, "note", TestData.LINE_NOTE, false);
    }

    /**
     * "الخصومات والإضافات": one line with a random account, a random
     * "التأثير على التكلفة", discount % / value, addition % / value, a random
     * currency and a note (the old test's values). The cells are AG Grid rich
     * selects (QC 2026-11-05), filled with {@code selectLineOptionIfEditable}. The lines
     * tab is opened again afterwards.
     *
     * <p>The guide notes a possible server error with a FOREIGN currency on
     * this line together with "انزال من" (orders). The currency stays random
     * as in the old test; if the save fails, the log shows the currency.</p>
     */
    private void addDiscountAdditionLine() {

        page.openTab(ForeignInvoicePage.TAB_DISCOUNTS_ADDITIONS);
        int row = page.addLine();
        String account = page.selectLineOptionIfEditable(row, "accountId");
        String influence = page.selectLineOptionIfEditable(row, "influencingOnCost");
        // QC 2026-11-05: once a discount is entered, "نسبة الإضافي" is disabled
        // (a line is a discount OR an addition) → disabled cells are skipped.
        page.fillLineCellIfEditable(row, "discountRate", TestData.DA_DISCOUNT_RATE, true);
        page.fillLineCellIfEditable(row, "discountValue", TestData.DA_DISCOUNT_VALUE, true);
        page.fillLineCellIfEditable(row, "additionRate", TestData.DA_ADDITION_RATE, true);
        page.fillLineCellIfEditable(row, "additionValue", TestData.DA_ADDITION_VALUE, true);
        String currency = page.selectLineOptionIfEditable(row, "currencyId");
        page.fillLineCellIfEditable(row, "note", TestData.DA_NOTE, false);
        log.info("Discounts & additions line {}: account [{}], effect [{}], currency [{}]",
                row + 1, account, influence, currency);
        assertFalse(account.isEmpty(), "No account could be chosen on the discounts & additions line.");
        page.openTab(ForeignInvoicePage.TAB_LINES);
    }

    /**
     * Lines already in the grid (copied, downloaded from a receipt order, or
     * saved before an edit): a repeated item is removed; quantity / cost 0 get
     * the test values. Every value is read with {@code readLineCell}
     * (scrolled into view + textContent): with getText() a cell out of view
     * reads "", which would delete or overwrite a correct line. A locked
     * quantity / cost (e.g. from the receipt order) is skipped and logged.
     */
    private void completeExistingLines() {

        page.openTab(ForeignInvoicePage.TAB_LINES);
        page.waitForGridToSettle();
        int rows = page.lineCount();
        Map<String, Integer> firstRowOfItem = new java.util.HashMap<>();
        for (int row = 0; row < rows; row++) {
            firstRowOfItem.putIfAbsent(page.readLineCell(row, "code"), row);
        }
        for (int row = rows - 1; row >= 0; row--) {
            String code = page.readLineCell(row, "code");
            if (!code.isEmpty() && firstRowOfItem.get(code) != row) {
                log.info("Line {} repeats item {} → removing it.", row + 1, code);
                page.deleteLine(row);
                continue;
            }
            if (code.isEmpty()) {
                continue; // empty line added by the grid; removed before saving
            }
            if (page.readLineNumber(row, "quantity").signum() == 0) {
                log.info("Line {} ({}) has no quantity → {}.", row + 1, code, TestData.QUANTITY);
                page.fillLineCellIfEditable(row, "quantity", TestData.QUANTITY, true);
            }
            if (page.readLineNumber(row, ForeignInvoicePage.PRICE_COLUMN).signum() == 0) {
                log.info("Line {} ({}) has cost 0 → {}.", row + 1, code, TestData.PRICE);
                page.fillLineCellIfEditable(row, ForeignInvoicePage.PRICE_COLUMN, TestData.PRICE, true);
            }
        }
        page.stopCellEditing();
    }

    private boolean isItemOnAnotherLine(String code, int row) {
        int rows = page.lineCount();
        for (int other = 0; other < rows; other++) {
            if (other != row && code.equals(page.readLineCell(other, "code"))) {
                return true;
            }
        }
        return false;
    }

    /**
     * New invoice from a receipt order: {@code openForm} opens the empty form
     * (New / Copy / Add), then "المستند المرجعي" → random receipt order →
     * download, the rest of the header as in the old test (النوع، مركز التكلفة،
     * طريقة احتساب الضريبة، التواريخ، تعميم خصم، الوسم، and "آجل" when
     * {@code creditPayment}), "جدولة الدفعات" and save. As in the old test no
     * line is added: the receipt order's lines are used. A receipt order that
     * brings no line is replaced (max {@link TestData#MAX_REFERENCE_ATTEMPTS}).
     *
     * <p>QC 2026-11-05: the dialog offered ONE receipt order,
     * PURCHDR2608-000001, which is already the reference of Approved
     * LPI2608-000003. If the server refuses a second invoice for it, the save
     * fails with the server's message: that is the expected evidence.</p>
     *
     * @return the number shown after saving.
     */
    private String createFromReferenceDocumentAndVerify(boolean creditPayment, Runnable openForm) {
        return createFromReceiptOrderAndVerify(creditPayment, openForm, page::selectRandomReferenceDocument);
    }

    /**
     * Opens the form ({@code openForm}), picks a receipt order with {@code chooser}
     * (the "المستند المرجعي" dialog, or "⋮" → "متابعة من أمر استلام"), downloads it,
     * completes the invoice and saves it; the reference must be kept.
     */
    private String createFromReceiptOrderAndVerify(boolean creditPayment, Runnable openForm,
                                                   java.util.function.Supplier<String> chooser) {

        String reference = null;
        for (int attempt = 1; attempt <= TestData.MAX_REFERENCE_ATTEMPTS && reference == null; attempt++) {
            if (attempt > 1) {
                page.closeForm();
            }
            openForm.run();
            String chosen = chooser.get();
            page.downloadReferenceDocument();
            int lines = page.lineCount();
            log.info("المستند المرجعي {}: {} line(s) ({}/{}).", chosen, lines, attempt, TestData.MAX_REFERENCE_ATTEMPTS);
            if (lines > 0) {
                reference = chosen;
            }
        }
        if (reference == null) {
            throw new SkipException("No receipt order in \"المستند المرجعي\" brought any line after "
                    + TestData.MAX_REFERENCE_ATTEMPTS + " attempts.");
        }

        fillHeaderAfterReference(creditPayment);
        completeExistingLines();
        selectPaymentTermsTemplate();
        String code = saveAndVerify(TestData.NEW_INVOICE_STATUSES);

        assertEquals(page.inputValue("referenceDocument").trim(), reference,
                "\"المستند المرجعي\" of " + code + " was not kept.");
        return code;
    }

    /**
     * After a download, the old test's fields in screen order: النوع → مركز
     * التكلفة → طريقة احتساب الضريبة → (طريقة الدفع = آجل) → التاريخ → تاريخ
     * الاستحقاق → تعميم خصم → الوسم. Supplier, branch, warehouse and the other
     * fields the receipt order filled are KEPT; any that stayed empty is
     * filled so the invoice can be saved.
     */
    private void fillHeaderAfterReference(boolean creditPayment) {

        editing = false;
        selectIfEditable("operationTypeCategoryId");
        for (String field : List.of("vendorId", "currencyId")) {
            if (isEmptySelect(field)) {
                selectIfEditable(field);
            }
        }
        if (isEmptySelect("warehouseId")) {
            selectBranchUntilWarehouseIsFilled();
        }
        selectIfEditable("costCenterId");
        selectIfEditable("taxCalculationMethode");
        if (creditPayment && page.waitForSelectEnabled("paymentMethod", Duration.ofSeconds(5))) {
            page.selectPaymentMethod(ForeignInvoicePage.PAYMENT_METHOD_CREDIT);
        }
        if (page.priceNature().isEmpty()) {
            page.selectRandomPriceNature();
        }
        LocalDate today = LocalDate.now();
        page.setInvoiceDate(today);
        page.setDueDate(today.plusDays(TestData.DUE_IN_DAYS));
        for (String field : List.of("purchaseRepresentativeId", "itemsCostsMethode")) {
            if (isEmptySelect(field)) {
                selectIfEditable(field);
            }
        }
        setBulkDiscount();
        selectIfEditable("tagId");
        selectIfEditable("letterOfCredtId"); // "الاعتماد المستندي" (foreign only, old test order)
        selectIfEditable("classification"); // "التصنيف"
        page.setVendorInvoiceCode(unusedVendorInvoiceCode());
        String note = TestData.uniqueNote();
        page.setNotes(note);
        assertEquals(page.waitForNotes(note), note, "\"البيان\" was not filled.");
    }

    /**
     * Saves and verifies the RESULT:
     * success toast → view mode → number → description persisted → status.
     * "مبلغ الفاتورة" is set to the total with tax right before saving,
     * once the lines are final.
     *
     * @return the saved invoice's number.
     */
    private String saveAndVerify(DocumentStatus... allowedStatuses) {

        String note = page.notes();
        if (!note.startsWith(TestData.NOTE_PREFIX)) {
            note = TestData.uniqueNote();
            page.setNotes(note);
        }

        page.openTab(ForeignInvoicePage.TAB_LINES); // deleteEmptyLines works on the grid of the open tab
        page.stopCellEditing();
        page.deleteEmptyItemLines();
        String amount = page.copyTotalToInvoiceAmount();
        log.info("مبلغ الفاتورة = {}", amount);

        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.startRecordingSaveRequest();
        page.clickSave();
        if (!waitForConfirmDialog(form)) {
            fail("Save did not open the confirmation dialog. Required-field errors: "
                    + page.requiredErrorFields() + " | lines sent: " + page.lastSavedLines());
        }
        page.confirmDialog();

        DocumentForm.Toast toast = form.waitForToast(); // read first: it disappears after ~2–3 s
        // Attachments are uploaded WITH the save; their toast can come first (quotations,
        // 2026-11-02). Then the other toasts are read and none may be an error.
        boolean uploadToast = isUploadToast(toast);
        if (uploadToast) {
            List<String> others = page.otherToastTexts("تمت إضافة الملفات", Duration.ofSeconds(8));
            log.info("Upload toast [{}] read during the save; other toasts: {}", toast.fullText().replace('\n', ' '), others);
            assertTrue(others.stream().noneMatch(t -> t.contains("خطأ")),
                    "Save failed on the server: " + others + " | lines sent: " + page.lastSavedLines());
        } else {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast + " | lines sent: " + page.lastSavedLines());
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = page.waitForCode();

        SoftAssert softly = new SoftAssert();
        if (!uploadToast) {
            softly.assertTrue(toast.fullText().contains(TestData.SAVE_SUCCESS_MESSAGE), "Unexpected success message: " + toast);
        }
        String shownNote = page.waitForNotes(note);
        String status = page.statusLabel();
        if (!note.equals(shownNote)) {
            log.warn("After saving {} the form shows note [{}] → reloading to read what was saved.", code, shownNote);
            page.reload();
            shownNote = page.waitForNotes(note);
            status = page.statusLabel();
        }
        softly.assertEquals(shownNote, note, "Description was not persisted in " + code + ".");
        softly.assertTrue(isOneOf(status, allowedStatuses),
                "Unexpected status [" + status + "] after saving " + code
                        + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();

        log.info("Saved Foreign Purchase Invoice {} with status {}", code, status);
        return code;
    }

    /**
     * "إضافة" / "نسخ" must create a NEW invoice and leave the source as it
     * was. The new invoice is found in the API by its unique description.
     *
     * <p>QC run 2026-11-05 15:32: after saving a copy of LPI2610T40-000029 the
     * form still showed the SOURCE number, while the server had created
     * LPI2611T45-000009 (the same was seen on quotations, 2026-11-02). Rule
     * agreed with the user for quotations, applied here too: when the form
     * then shows the new number by itself (no reload) the test passes and it
     * is logged as a low-priority screen issue; it fails only when the form
     * never shows the new number.</p>
     */
    private void verifyNewInvoice(Map<String, Object> source, String shownCode, String how) {

        String sourceCode = ForeignInvoicePage.codeOf(source);
        String note = page.notes();
        List<Map<String, Object>> all = page.api().findAll();
        List<Map<String, Object>> saved = all.stream()
                .filter(r -> note.equals(String.valueOf(r.get("notes")))).toList();
        Map<String, Object> sourceNow = all.stream()
                .filter(r -> String.valueOf(source.get("id")).equals(String.valueOf(r.get("id"))))
                .findFirst().orElse(null);

        assertEquals(saved.size(), 1, how + ": expected one invoice with note [" + note + "], found " + saved.size());
        String newCode = ForeignInvoicePage.codeOf(saved.get(0));
        log.info("{}: source {} (id {}) → new {} (id {}); the form shows {}.", how, sourceCode, source.get("id"),
                newCode, saved.get(0).get("id"), shownCode);

        SoftAssert softly = new SoftAssert();
        softly.assertNotEquals(newCode, sourceCode, how + " saved over the source " + sourceCode + ".");
        softly.assertNotNull(sourceNow, how + ": source " + sourceCode + " is gone.");
        if (sourceNow != null) {
            softly.assertEquals(String.valueOf(sourceNow.get("notes")), String.valueOf(source.get("notes")),
                    how + ": the description of source " + sourceCode + " changed.");
            softly.assertEquals(sourceNow.get("isApproved"), source.get("isApproved"),
                    how + ": the status of source " + sourceCode + " changed.");
        }
        if (!shownCode.equals(newCode)) {
            String settled = waitUntilEquals(page::code, newCode);
            if (settled.equals(newCode)) {
                log.warn("Screen showed the source number {} for a moment after {}, then {} "
                        + "(low-priority screen issue).", shownCode, how, newCode);
            } else {
                page.reload();
                String afterReload = page.waitForCode();
                softly.fail("SCREEN ISSUE: after " + how + " the form kept showing " + settled
                        + " instead of the new invoice " + newCode + " (after a reload it shows " + afterReload
                        + ", url " + driver.getCurrentUrl() + ").");
            }
        }
        softly.assertAll();
    }

    /** The form is closed and the list is shown again. */
    private void assertBackOnList(String afterWhat) {
        waitUntil("the list after " + afterWhat, Duration.ofSeconds(15), () -> !page.isFormOpen());
        page.waitForListLoaded();
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

    /** Header read once it stops changing (some fields fill in a moment later, quotations 2026-11-02). */
    private Map<String, String> stableHeader(List<String> fields) {
        Map<String, String> previous = page.readHeaderValues(fields);
        long end = System.currentTimeMillis() + 10_000;
        while (System.currentTimeMillis() < end) {
            pause(500);
            Map<String, String> now = page.readHeaderValues(fields);
            if (now.equals(previous)) {
                return now;
            }
            previous = now;
        }
        return previous;
    }

    /** "code × quantity @ cost" of every line, as numbers (17 = 17.000), read safely (scrolled + textContent). */
    private List<String> linesSnapshot() {
        page.openTab(ForeignInvoicePage.TAB_LINES);
        List<String> lines = new ArrayList<>();
        int count = page.lineCount();
        for (int row = 0; row < count; row++) {
            lines.add(page.readLineCell(row, "code") + " × "
                    + page.readLineNumber(row, "quantity").stripTrailingZeros().toPlainString() + " @ "
                    + page.readLineNumber(row, ForeignInvoicePage.PRICE_COLUMN).stripTrailingZeros().toPlainString());
        }
        return lines;
    }

    /**
     * Debit / credit note from an Approved invoice: the note form opens for this
     * invoice ({@code <path><id>?typeName=PurchaseInvoice}) with it as "المستند
     * المصدر" (QC 2026-11-05, LPI2608-000002 → DN26119-000001 / CN26119-000008).
     * Nothing is saved.
     */
    private void verifyNoteOpensForApprovedInvoice(String menuItem, String path) {

        Map<String, Object> record = openRandomInvoice(ForeignInvoicePage::isApproved);
        String code = page.waitForCode();

        page.clickMenuItemAndFollow(menuItem, path);

        String url = driver.getCurrentUrl();
        waitUntil("the note form to load " + code, Duration.ofSeconds(20),
                () -> code.equals(page.inputValue("sourceDocument").trim()));
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains(path + record.get("id")) && url.contains("typeName=PurchaseInvoice"),
                "\"" + menuItem + "\" did not open the note for invoice id " + record.get("id") + ": " + url);
        softly.assertEquals(page.inputValue("sourceDocument").trim(), code, "The note is not linked to " + code + ".");
        softly.assertFalse(page.inputValue("noticeCode").isBlank(), "The note form of " + code + " has no number.");
        softly.assertAll();
    }

    /** The attachment upload toast ("تمت إضافة الملفات بنجاح" / "تم الرفع بنجاح"). */
    private static boolean isUploadToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.contains("تمت إضافة الملفات") || text.contains("تم الرفع");
    }

    private static boolean isOneOf(String statusLabel, DocumentStatus... allowed) {
        return Arrays.stream(DocumentStatus.values()).anyMatch(s -> s.label().equals(statusLabel))
                && DocumentStatus.fromLabel(statusLabel).isOneOf(allowed);
    }
}
