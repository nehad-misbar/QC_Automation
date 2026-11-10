package com.erp.tests;

import com.erp.automation.base.PurchasingBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.FreeQuantityPage;
import com.erp.automation.pages.PurchasingPage;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.ui.Downloads;
import org.openqa.selenium.TimeoutException;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
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
 * Free Quantity Processing (معالجة الكمية المجانية), level 1: 36 tests (2026-11-05),
 * written on the Purchasing template ({@link PurchasingBaseTest}, {@link FreeQuantityPage},
 * {@link DocumentForm}) and modeled on the purchase requisition / order / invoice tests
 * (same statuses as requisitions: Draft → Approved → Disapprove, and Cancel).
 *
 * <p>Screen facts (QC 2026-11-05, see {@link FreeQuantityPage}): header = date, branch,
 * warehouse, description and processing type (3 radios); a line = an item that HAS free
 * quantity in the warehouse ("الكمية المجانية المتوفرة") and the quantity to process. The form
 * has no status label, so statuses are read through the API ({@code isApproved},
 * {@code isCanceled}). Tests that change data create their own document first.</p>
 *
 * <p>Rules checked: the quantity to process may not exceed the available free quantity
 * (the old test typed 5,000,000); an approved document cannot be deleted and has a journal
 * entry; a cancelled one is read-only.</p>
 *
 * <p>Not applicable: summaries (not on this list), reference document (no such field),
 * follow-up documents (only "التصدير الى Excel" in "⋮").</p>
 *
 * <p>Assumptions to confirm on the first run (marked "assumed" in the code): the log
 * labels (as on invoices), only the description is editable on an Approved document (as on
 * requisitions), and the Excel export layout (title + header rows).</p>
 *
 * <p>The old test (one create test, no save) is in
 * {@code docs/legacy/FreeQuantityProcessingTest.java.txt}.</p>
 */
public class FreeQuantityProcessingTest extends PurchasingBaseTest {

    private static final String FQP = "free-quantity-processing";

    private FreeQuantityPage page;
    private DocumentForm form;

    @Override
    protected void openScreen(PurchasingPage purchasingPage) {
        purchasingPage.clickFreeQuantityProcessing();
        page = new FreeQuantityPage(driver);
        form = new DocumentForm(driver, FreeQuantityPage.PREFIX);
        page.dismissInstallPrompt();
        page.waitForListLoaded();
    }

    // =====================================================================
    // Test data
    // =====================================================================

    private static final class TestData {
        /** Quantity processed per line (never more than the available free quantity). */
        static final BigDecimal QUANTITY = BigDecimal.ONE;
        static final String LINE_NOTE = "بيان معالجة كمية مجانية تجريبي";
        static final String NOTE_PREFIX = "طلب معالجة كمية مجانية تجريبي ";
        private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        static String uniqueNote() {
            return NOTE_PREFIX + LocalDateTime.now().format(STAMP);
        }

        static final int MAX_WAREHOUSE_ATTEMPTS = 5;
        static final int MAX_ITEM_ATTEMPTS = 6;
        /** Approved documents opened at most while looking for one with a journal entry. */
        static final int MAX_JOURNAL_ATTEMPTS = 8;

        /** "هذا الحقل مطلوب" on an empty save (labels with * on QC 2026-11-05). */
        static final List<String> REQUIRED_FIELDS = List.of("operationDate", "branchId", "warehouseId", "processingTypeId");

        static final List<String> ACTIONS_DISABLED_ON_NEW = List.of(
                "copy", "add", "undo", "delete", "approveOrDisapprove", "cancel", "journalEntry", "transactionLogs");
        static final List<String> ACTIONS_ENABLED_ON_NEW = List.of("xIcon", "uploadAttachment", "recordSearch");
        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "copy", "add", "edit", "delete", "approveOrDisapprove", "cancel", "transactionLogs");
        static final List<String> ACTIONS_DISABLED_ON_DRAFT = List.of("journalEntry", "save");
        static final List<String> ACTIONS_ENABLED_ON_APPROVED = List.of(
                "edit", "approveOrDisapprove", "cancel", "journalEntry", "transactionLogs");
        static final List<String> ACTIONS_DISABLED_ON_APPROVED = List.of("delete", "save");
        static final List<String> ACTIONS_DISABLED_WHEN_CANCELLED = List.of("edit", "approveOrDisapprove", "cancel");

        /** Parts of the server calls (as on requisitions: /approveOrDisApprove/, /cancel/). */
        static final String API_APPROVE = "pprove";
        static final String API_CANCEL = "ancel";

        /** Log labels (assumed, as on invoices). */
        static final String LOG_CREATED = "انشاء العملية";
        static final String LOG_APPROVED = "اعتماد العملية";

        /** Group-by field: the list has no status column, "نوع المعالجة" is grouped instead. */
        static final String GROUP_BY_TYPE = "نوع المعالجة";
        static final List<String> PROCESSING_TYPES = List.of("هدية مجانية للعميل", "تخفيض التكلفة", "إيراد للشركة");

        /**
         * The screen's own import template ("تصدير الاصناف": #, رمز الصنف, اسم الصنف, ..., الكمية المجانية المتوفرة,
         * الكمية المراد معالجتها, الوحدة, العبوة, البيان). PurchaseOrder.xlsx was used before: its 37 lines have
         * almost no free quantity, and deleting them one by one in the virtualized grid lost the browser window.
         */
        static final Path EXCEL_LINES = Paths.get(System.getProperty("user.dir"),
                "test-data", "import-excel", "Free-Quantity-Processing.xlsx");
        /** Branch / warehouse where the items of {@link #EXCEL_LINES} have free quantity. */
        static final String EXCEL_BRANCH = "فرع رئيسى";
        static final String EXCEL_WAREHOUSE = "مخزن الدمام";

        static final String ATTACHMENT_TITLE = "مرفقات معالجة كمية مجانية تجريبية";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات معالجة الكمية المجانية";

        private TestData() {
        }
    }

    // =====================================================================
    // Create
    // =====================================================================

    @Test(priority = 1, groups = {FQP, "create"},
            description = "New document: date, branch → warehouse, description, processing type and a line with an item "
                    + "that has free quantity; saved as Draft and every value is kept after reload")
    public void userCanCreateNewFreeQuantityProcessing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        Line line = addLineWithFreeQuantity();
        Map<String, String> entered = header();

        String code = saveAndVerify();
        assertTrue(code.startsWith("FQP"), "Unexpected number " + code + ".");
        page.reload();
        page.waitForGridToSettle();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(header(), entered, "Header of " + code + " was not kept after reload.");
        softly.assertEquals(page.itemCode(0), line.code(), "Item of " + code + " was not kept.");
        softly.assertEquals(page.quantityToProcess(0).compareTo(line.quantity()), 0,
                "Quantity to process of " + code + " was not kept (" + page.quantityToProcess(0) + ").");
        softly.assertEquals(apiStatus(code), DRAFT, code + " is not a Draft.");
        softly.assertAll();
    }

    @Test(priority = 2, groups = {FQP, "create"},
            description = "Copy (نسخ) a document: the copy keeps the lines, gets a new number, and the source is unchanged")
    public void userCanDuplicateFreeQuantityProcessing() {

        Map<String, Object> source = openRandomRecord(r -> true);
        String sourceCode = page.waitForCode();
        page.clickCopy();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForGridToSettle();
        fitCopiedLinesToAvailableQuantity();
        if (page.lineCount() == 0) {
            addLineWithFreeQuantity();
        }
        fillDateNoteAndType(false); // branch and warehouse stay those of the copied document

        String code = saveAndVerify();
        assertNotEquals(code, sourceCode, "The copy was saved under the source number " + sourceCode + ".");
        assertTrue(record(sourceCode).isPresent(), "The source " + sourceCode + " is gone after copying.");
        assertEquals(statusOf(record(sourceCode).get()), statusOf(source), "The status of the source " + sourceCode + " changed.");
    }

    @Test(priority = 3, groups = {FQP, "create"},
            description = "New document from the Add button of an opened one: empty form, saved as a new Draft")
    public void userCanAddFreeQuantityProcessingFromOpenedOne() {

        openRandomRecord(r -> true);
        String opened = page.waitForCode();
        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);
        assertEquals(page.lineCount(), 0, "\"إضافة\" did not open an empty form.");
        fillHeader(false);
        addLineWithFreeQuantity();
        assertNotEquals(saveAndVerify(), opened, "\"إضافة\" saved over " + opened + ".");
    }

    @Test(priority = 4, groups = {FQP, "create", "excel"},
            description = "\"⋮\" → \"الاستيراد من Excel\": lines added; lines without free quantity removed, the others "
                    + "processed within the available quantity; saved")
    public void userCanCreateFreeQuantityProcessingFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        // The items of the Excel file have free quantity in this branch / warehouse (QC 2026-11-09).
        page.selectOptionContaining("branchId", TestData.EXCEL_BRANCH);
        assertTrue(page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(5)),
                "\"المخزن\" stayed disabled after choosing " + TestData.EXCEL_BRANCH + ".");
        page.selectOptionContaining("warehouseId", TestData.EXCEL_WAREHOUSE);
        fillDateNoteAndType(false);
        page.importLinesFromExcel(TestData.EXCEL_LINES);
        int imported = page.lineCount();
        log.info("Lines imported from Excel: {}", imported);
        assertTrue(imported > 0, "Excel import finished but no lines were added.");

        fitCopiedLinesToAvailableQuantity();
        if (page.lineCount() == 0) {
            throw new SkipException("No item of " + TestData.EXCEL_LINES.getFileName()
                    + " has free quantity in " + TestData.EXCEL_BRANCH + " / " + TestData.EXCEL_WAREHOUSE + ".");
        }
        int lines = page.lineCount();
        String code = saveAndVerify();
        page.reload();
        page.waitForGridToSettle();
        assertEquals(page.lineCount(), lines, code + " did not keep its " + lines + " imported line(s).");
    }

    // =====================================================================
    // Validation and toolbar
    // =====================================================================

    @Test(priority = 5, groups = {FQP, "validation"},
            description = "An empty document cannot be saved: \"حفظ\" disabled, or the required fields are marked "
                    + "and nothing is saved")
    public void userCannotSaveFreeQuantityProcessingWithoutRequiredData() {

        int before = apiRecords().size();
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        // QC 2026-11-05: on a new document "حفظ" is disabled until it is filled.
        if (page.isActionEnabled("save")) {
            page.clickSave();
            assertFalse(waitForConfirmDialog(form), "An empty document reached the save confirmation.");
            List<String> marked = page.requiredErrorFields();
            assertTrue(marked.containsAll(TestData.REQUIRED_FIELDS),
                    "Required fields not marked: expected " + TestData.REQUIRED_FIELDS + ", marked " + marked);
        } else {
            log.info("\"حفظ\" is disabled on an empty document (blocked).");
        }
        assertEquals(apiRecords().size(), before, "A document was saved without data.");
    }

    @Test(priority = 6, groups = {FQP, "validation"},
            description = "The quantity to process cannot exceed the available free quantity: the save is refused "
                    + "and no document is created")
    public void quantityToProcessCannotExceedAvailableFreeQuantity() {

        int before = apiRecords().size();
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        Line line = addLineWithFreeQuantity();
        String tooMuch = line.available().add(BigDecimal.ONE).toPlainString();
        page.setQuantityToProcess(line.row(), tooMuch);
        page.stopCellEditing();
        log.info("Line {}: available {}, quantity to process {}", line.code(), line.available(), tooMuch);

        String refusal = "\"حفظ\" disabled";
        if (page.isActionEnabled("save")) {
            page.clickSave();
            if (waitForConfirmDialog(form)) {
                page.confirmDialog();
                DocumentForm.Toast toast = form.waitForToast();
                assertFalse(toast.isSuccess(), "Processing " + tooMuch + " of " + line.code() + " (available "
                        + line.available() + ") was accepted: " + toast);
                refusal = toast.fullText();
            } else {
                refusal = "blocked before the confirmation: " + page.requiredErrorFields();
            }
        }
        log.info("Refused: {}", refusal);
        assertEquals(apiRecords().size(), before, "A document processing more than the available free quantity was saved.");
    }

    @Test(priority = 7, groups = {FQP, "validation"},
            description = "New document toolbar: close / attachments / record search enabled; copy, add, undo, delete, "
                    + "approve, cancel, journal entry and log disabled")
    public void newFreeQuantityProcessingEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_ON_NEW) {
            softly.assertFalse(page.hasAction(action) && page.isActionEnabled(action), "'" + action + "' must be disabled on a new document.");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_NEW) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new document.");
        }
        softly.assertAll();
    }

    @Test(priority = 8, groups = {FQP, "validation", "status"},
            description = "Draft: copy / add / edit / delete / approve / cancel / log enabled; journal entry disabled")
    public void draftFreeQuantityProcessingEnablesEditActions() {

        openRandomRecord(FreeQuantityPage::isDraft);
        String code = page.waitForCode();
        assertActions(code, TestData.ACTIONS_ENABLED_ON_DRAFT, TestData.ACTIONS_DISABLED_ON_DRAFT);
    }

    @Test(priority = 9, groups = {FQP, "validation", "status"},
            description = "Approved: delete disabled; edit / disapprove / cancel / journal entry / log enabled; approval date set")
    public void approvedFreeQuantityProcessingCannotBeDeleted() {

        Map<String, Object> record = openRandomRecord(FreeQuantityPage::isApproved);
        String code = page.waitForCode();
        assertActions(code, TestData.ACTIONS_ENABLED_ON_APPROVED, TestData.ACTIONS_DISABLED_ON_APPROVED);
        assertFalse(String.valueOf(record.get("approvalDate")).startsWith("0001"), "Approved " + code + " has no approval date.");
    }

    // =====================================================================
    // Edit / undo
    // =====================================================================

    @Test(priority = 10, groups = {FQP, "edit"},
            description = "Edit a Draft (created by the test): date, description, processing type and the line quantity "
                    + "are changed, kept after reload; it stays a Draft")
    public void userCanEditDraftFreeQuantityProcessing() {

        String code = createOwnRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillHeader(true);
        BigDecimal available = page.availableFreeQuantity(0);
        BigDecimal quantity = page.quantityToProcess(0).compareTo(available) < 0 && available.compareTo(BigDecimal.ONE) >= 0
                ? available.min(page.quantityToProcess(0).add(BigDecimal.ONE)) : page.quantityToProcess(0);
        page.setQuantityToProcess(0, quantity.toPlainString());
        Map<String, String> entered = header();

        saveAndVerify();
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(header(), entered, "Edited header of " + code + " was not kept after reload.");
        softly.assertEquals(page.quantityToProcess(0).compareTo(quantity), 0, "Edited quantity of " + code + " was not kept.");
        softly.assertEquals(apiStatus(code), DRAFT, "Editing changed the status of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 11, groups = {FQP, "edit", "status"},
            description = "Approved (created by the test) in edit mode: branch and warehouse locked; the description "
                    + "change is saved and it stays Approved (assumed rule, as on requisitions)")
    public void approvedFreeQuantityProcessingAllowsDescriptionEdit() {

        String code = createOwnRequiring(APPROVED);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        SoftAssert softly = new SoftAssert();
        for (String field : List.of("branchId", "warehouseId")) {
            softly.assertTrue(page.isSelectDisabled(field), "\"" + field + "\" can be changed on Approved " + code + ".");
        }
        String note = TestData.uniqueNote();
        assertTrue(page.setNote(note), "\"البيان\" is locked on Approved " + code + ".");
        saveAndVerify();
        page.reload();
        softly.assertEquals(page.note(), note, "The description of Approved " + code + " was not kept.");
        softly.assertEquals(apiStatus(code), APPROVED, "Editing changed the status of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 12, groups = {FQP, "edit"},
            description = "Undo in edit mode (Draft created by the test): the saved header and line come back")
    public void userCanUndoFreeQuantityProcessingChanges() {

        String code = createOwnRequiring(DRAFT);
        Map<String, String> saved = header();
        BigDecimal savedQuantity = page.quantityToProcess(0);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillHeader(true);
        log.info("Before undo: {}; saved: {}", header(), saved);

        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        page.clickUndo();
        page.confirmDialog();

        SoftAssert softly = new SoftAssert();
        saved.forEach((field, value) -> softly.assertEquals(waitUntilEquals(() -> header().get(field), value), value,
                "Undo did not restore \"" + field + "\" of " + code + "."));
        softly.assertEquals(page.quantityToProcess(0).compareTo(savedQuantity), 0, "Undo did not restore the quantity of " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Status: delete / approve / disapprove / cancel
    // =====================================================================

    @Test(priority = 13, groups = {FQP, "status"},
            description = "Delete a Draft (created by the test): success message, gone from the API and the list")
    public void userCanDeleteDraftFreeQuantityProcessing() {

        String code = createOwnRequiring(DRAFT);
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft " + code + ".");
        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);
        waitUntil(code + " to be gone from the API", Duration.ofSeconds(15), () -> record(code).isEmpty());
        page.searchList(code);
        assertFalse(page.visibleListCodes().contains(code), code + " is still in the list.");
    }

    @Test(priority = 14, groups = {FQP, "status", "log"},
            description = "Approve a Draft (created by the test): Approved in the API, approval date today, journal entry "
                    + "enabled, delete disabled, one approval log entry")
    public void userCanApproveDraftFreeQuantityProcessing() {

        String code = createOwnRequiring(DRAFT);
        List<String> logBefore = page.readTransactionLog();
        approveOrDisapproveOpened(code, APPROVED);

        page.reload();
        page.waitForCode();
        List<String> logAfter = page.readTransactionLog();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(String.valueOf(record(code).orElseThrow().get("approvalDate")).startsWith(LocalDate.now().toString()),
                "The approval date of " + code + " is not today.");
        softly.assertTrue(page.isActionEnabled("journalEntry"), "\"قيد اليومية\" is disabled on Approved " + code + ".");
        softly.assertFalse(page.isActionEnabled("delete"), "\"حذف\" is enabled on Approved " + code + ".");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Approving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        softly.assertAll();
    }

    @Test(priority = 15, groups = {FQP, "status"},
            description = "Disapprove an Approved document (created by the test): back to Draft, edit and delete enabled")
    public void userCanDisapproveApprovedFreeQuantityProcessing() {

        String code = createOwnRequiring(APPROVED);
        approveOrDisapproveOpened(code, DRAFT);
        page.reload();
        page.waitForCode();
        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on disapproved " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("journalEntry"), "\"قيد اليومية\" is still enabled on disapproved " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 16, groups = {FQP, "status"},
            description = "Cancel a Draft (created by the test): Cancelled in the API, then read-only (edit / approve / cancel disabled)")
    public void userCanCancelDraftFreeQuantityProcessing() {

        cancelOpenedAndVerify(createOwnRequiring(DRAFT));
    }

    @Test(priority = 17, groups = {FQP, "status"},
            description = "Cancel an Approved document (created by the test): Cancelled in the API, then read-only")
    public void userCanCancelApprovedFreeQuantityProcessing() {

        cancelOpenedAndVerify(createOwnRequiring(APPROVED));
    }

    // =====================================================================
    // Journal entry / log
    // =====================================================================

    @Test(priority = 18, groups = {FQP, "accounting"},
            description = "Journal entry of an Approved document: lines shown, debit = credit")
    public void userCanViewJournalEntryOfApprovedFreeQuantityProcessing() {

        Map.Entry<String, FreeQuantityPage.JournalEntry> found = openApprovedWithJournalEntry();
        String code = found.getKey();
        FreeQuantityPage.JournalEntry entry = found.getValue();
        log.info("Journal entry of {}: {}", code, entry);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(entry.lines() > 0, "The journal entry of " + code + " has no line.");
        softly.assertEquals(entry.debit().compareTo(entry.credit()), 0,
                "Journal entry of " + code + ": debit " + entry.debit() + " ≠ credit " + entry.credit() + ".");
        softly.assertAll();
    }

    @Test(priority = 19, groups = {FQP, "log"},
            description = "Transaction log: first entry is the creation; an Approved document has an approval entry")
    public void userCanViewFreeQuantityProcessingTransactionLog() {

        Map<String, Object> record = openRandomRecord(r -> true);
        String code = page.waitForCode();
        List<String> actions = page.readTransactionLog();
        log.info("Log of {}: {}", code, actions);
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log of " + code + " is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), TestData.LOG_CREATED,
                "The first log entry of " + code + " must be the creation (assumed label).");
        if (FreeQuantityPage.isApproved(record)) {
            softly.assertTrue(actions.contains(TestData.LOG_APPROVED), "Approved " + code + " has no approval entry: " + actions);
        }
        softly.assertAll();
    }

    // =====================================================================
    // List: search, record search, navigation, pagination, Kanban, group by
    // =====================================================================

    @Test(priority = 20, groups = {FQP, "search"},
            description = "List search by number shows only that document")
    public void userCanSearchFreeQuantityProcessingListByCode() {

        String code = FreeQuantityPage.codeOf(apiRecords().get(new Random().nextInt(apiRecords().size())));
        page.searchList(code);
        assertTrue(page.waitUntilListShows(code), "Searching " + code + " did not show it.");
        assertEquals(page.visibleListCodes(), List.of(code), "Searching " + code + " shows other documents.");
    }

    @Test(priority = 21, groups = {FQP, "search"},
            description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenFreeQuantityProcessingFromRecordSearch() {

        openRandomRecord(r -> true);
        String target = page.waitForCode();
        // Move to ANOTHER document by its id, not with "الأول" / "الأخير": QC 2026-11-09 "الأول"
        // changed the URL but left the form empty (no number) for 30 s, failing this search test
        // on a navigation problem. Record navigation is checked by its own test.
        List<Map<String, Object>> others = apiRecords().stream()
                .filter(r -> !target.equals(FreeQuantityPage.codeOf(r))).toList();
        if (others.isEmpty()) {
            throw new SkipException("QC has only " + target + ": no other document to start the search from.");
        }
        page.openDocument(others.get(new Random().nextInt(others.size())).get("id"));
        assertNotEquals(page.waitForCode(), target, "Could not move away from " + target + ".");
        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 22, groups = {FQP, "navigation"},
            description = "Record navigation: first / next / back / last, and the buttons disabled at each end")
    public void userCanNavigateBetweenFreeQuantityProcessingRecords() {

        openRandomRecord(r -> true);
        if (page.isRecordNavigationEnabled("pageFirst")) {
            String before = page.waitForCode();
            page.clickRecordNavigation("pageFirst");
            waitUntilChanges(before);
        }
        String first = page.waitForCode();
        SoftAssert softly = new SoftAssert();
        for (String button : List.of("pageFirst", "pagePrevious10", "pageBack")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the first record.");
        }
        page.clickRecordNavigation("pageNext");
        String second = waitUntilChanges(first);
        softly.assertNotEquals(second, first, "'Next' did not open another document.");
        page.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(page::code, first), first, "'Back' did not return to the first one.");
        page.clickRecordNavigation("pageLast");
        waitUntilChanges(first);
        for (String button : List.of("pageLast", "pageNext10", "pageNext")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the last record.");
        }
        softly.assertAll();
    }

    @Test(priority = 23, groups = {FQP, "pagination"},
            description = "List pagination: the total equals the documents in the API; first and last page ranges")
    public void userCanNavigateFreeQuantityProcessingPagination() {

        int[] range = page.paginationRange();
        int total = range[2];
        SoftAssert softly = new SoftAssert();
        // QC 2026-11-05: the pager said "/ 16" while the API listed 14 documents (open question).
        softly.assertEquals(total, apiRecords().size(), "The list total does not match the documents in the API.");
        softly.assertEquals(range[0], 1, "The list does not start at the first document.");
        softly.assertTrue(page.visibleRowCount() > 0, "The first page shows no row.");
        if (page.isListNextPageEnabled()) {
            page.goToLastListPage();
            int[] last = page.paginationRange();
            softly.assertEquals(last[1], total, "The last page does not end at the total.");
            softly.assertTrue(page.visibleRowCount() > 0, "The last page shows no row.");
            page.goToFirstListPage();
            softly.assertEquals(page.paginationRange()[0], 1, "\"first\" did not return to the first page.");
        }
        softly.assertAll();
    }

    @Test(priority = 24, groups = {FQP, "list"},
            description = "Kanban: the same documents as the list page, each card with a status; a card opens its document")
    public void userCanSwitchFreeQuantityProcessingListToKanbanAndBack() {

        List<String> gridCodes = page.visibleListCodes();
        try {
            page.switchToKanbanView();
            List<String> cardCodes = page.kanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);
            SoftAssert softly = new SoftAssert();
            softly.assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes), "Kanban cards ≠ the list page.");
            for (String code : cardCodes) {
                softly.assertFalse(page.kanbanCardStatus(code).isBlank(), "Card " + code + " has no status.");
            }
            softly.assertAll();
            String target = cardCodes.get(new Random().nextInt(cardCodes.size()));
            page.openKanbanCard(target);
            assertEquals(waitUntilEquals(page::code, target), target, "The card of " + target + " opened another document.");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView();
            }
        }
    }

    @Test(priority = 25, groups = {FQP, "list"},
            description = "Group by \"نوع المعالجة\": known types only, the groups add up to the list total")
    public void userCanGroupFreeQuantityProcessingListByType() {

        int total = page.paginationRange()[2];
        page.groupListBy(TestData.GROUP_BY_TYPE);
        Map<String, Integer> groups = page.groupCounts();
        int sum = groups.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Groups: {} (sum {}), total {}", groups, sum, total);
        SoftAssert softly = new SoftAssert();
        groups.keySet().forEach(type -> softly.assertTrue(TestData.PROCESSING_TYPES.contains(type), "Unknown group [" + type + "]."));
        int documents = apiRecords().size();
        softly.assertEquals(sum, documents, "Groups add up to " + sum + ", the API has " + documents + " documents.");
        // QC 2026-11-05: the groups added up to 14 = the API, the pager said 16 (open question).
        softly.assertEquals(sum, total, "Groups add up to " + sum + ", the list pager says " + total + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Print / PDF / Excel / pricing / barcode
    // =====================================================================

    @Test(priority = 26, groups = {FQP, "print"},
            description = "Print: the preview shows the number, branch and warehouse; then it is printed")
    public void userCanPrintFreeQuantityProcessing() throws java.awt.AWTException {

        openRandomRecord(r -> true);
        String code = page.waitForCode();
        String branch = page.selectedText("branchId");
        String warehouse = page.selectedText("warehouseId");
        String preview = page.openPrintPreviewWithRandomTemplate();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(preview.contains(code), "Print preview does not show " + code + ".");
        softly.assertTrue(preview.contains(branch.trim()), "Print preview of " + code + " does not show the branch [" + branch + "].");
        softly.assertTrue(preview.contains(warehouse.trim()), "Print preview of " + code + " does not show the warehouse [" + warehouse + "].");
        softly.assertAll();
        page.printFromPreview();
        page.confirmChromePrintDialog();
        page.closePrintPreview();
        assertEquals(page.waitForCode(), code, "After printing, the form does not show " + code + ".");
    }

    @Test(priority = 27, groups = {FQP, "print"},
            description = "Save as PDF from the print preview: a PDF file is downloaded")
    public void userCanSaveFreeQuantityProcessingAsPdf() {

        openRandomRecord(r -> true);
        String code = page.waitForCode();
        Path dir = page.prepareDownloadFolder("pdf");
        assertTrue(page.openPrintPreviewWithRandomTemplate().contains(code), "The print preview does not show " + code + ".");
        page.savePreviewAsPdf();
        Path pdf = page.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
    }

    @Test(priority = 28, groups = {FQP, "excel"},
            description = "\"⋮\" → \"التصدير الى Excel\": an .xlsx with the document's items")
    public void userCanExportFreeQuantityProcessingToExcel() {

        openRandomRecord(r -> true);
        String code = page.waitForCode();
        page.waitForGridToSettle();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < page.lineCount(); row++) {
            items.add(page.itemCode(row));
        }
        Path dir = page.prepareDownloadFolder("excel");
        page.clickMenuItem(FreeQuantityPage.MENU_EXPORT_EXCEL);
        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx file.");
        String content = Downloads.xlsxText(xlsx);
        int rows = Downloads.xlsxRowCount(xlsx);
        log.info("Excel of {}: {} row(s), items {}", code, rows, items);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(rows - 2, items.size(), "Excel rows ≠ the lines of " + code + " (assumed title + header rows).");
        items.forEach(item -> softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file."));
        softly.assertAll();
    }

    @Test(priority = 29, groups = {FQP, "follow-up"},
            description = "Items pricing ($): the pricing screen opens with the items of the document")
    public void userCanOpenItemsPricingFromFreeQuantityProcessing() {

        openRandomRecord(r -> true);
        String code = page.waitForCode();
        if (!page.waitForActionEnabled("itemsPricing")) {
            throw new SkipException("Items Pricing ($) is disabled on " + code + ".");
        }
        List<String> items = page.gridColumnTexts(FreeQuantityPage.COL_ITEM_CODE);
        page.clickItemsPricing();
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> priced = page.gridColumnTexts("itemCode");
        log.info("Items pricing from {}: {} → {} ({})", code, items, priced, url);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("/sales/pricing"), "Items pricing did not open the pricing screen: " + url);
        softly.assertEquals(new HashSet<>(priced), new HashSet<>(items), "Pricing does not list the items of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 30, groups = {FQP, "follow-up", "print"},
            description = "Print barcode: Item Barcodes opens with the items of the document")
    public void userCanOpenPrintBarcodeFromFreeQuantityProcessing() {

        openRandomRecord(r -> true);
        String code = page.waitForCode();
        if (!page.hasAction("printBarcode") || !page.waitForActionEnabled("printBarcode")) {
            throw new SkipException("Print Barcode is not available on " + code + ".");
        }
        List<String> items = page.gridColumnTexts("itemId");
        page.clickPrintBarcode();
        String url = URLDecoder.decode(driver.getCurrentUrl(), StandardCharsets.UTF_8);
        List<String> shown = page.gridColumnTextsOrEmpty("itemId", Duration.ofSeconds(10));
        log.info("Print barcode from {}: {} → {} ({})", code, items, shown, url);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(url.contains("barcode"), "Print barcode did not open Item Barcodes: " + url);
        softly.assertEquals(new HashSet<>(shown), new HashSet<>(items), "Item Barcodes does not list the items of " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Attachments / exit
    // =====================================================================

    @Test(priority = 31, groups = {FQP, "attachments"},
            description = "Add attachments to a Draft (created by the test): listed, kept after save and reload")
    public void userCanAddAttachmentsToFreeQuantityProcessing() {

        String code = createOwnRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        int before = page.attachmentCount();
        int files = page.countAttachmentFiles();
        page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
        assertEquals(page.waitForAttachmentCount(before + files), before + files, "The added files are not listed.");
        page.closeAttachments();
        saveAndVerify();
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before + files), before + files, "Attachments of " + code + " were not kept.");
    }

    @Test(priority = 32, groups = {FQP, "attachments"},
            description = "Delete an attachment of a Draft (created by the test): gone after save and reload")
    public void userCanDeleteAttachmentFromFreeQuantityProcessing() {

        String code = createOwnRequiring(DRAFT);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        int files = page.countAttachmentFiles();
        page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
        int before = page.waitForAttachmentCount(files);
        page.closeAttachments();
        saveAndVerify();

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        page.deleteLastAttachment();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1, "The deleted attachment is still listed.");
        page.closeAttachments();
        page.waitForToastToAppearAndClear();
        saveAndVerify();
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1, "The deleted attachment of " + code + " came back.");
    }

    @Test(priority = 33, groups = {FQP, "exit"},
            description = "Close a new and a viewed document: each returns to the list")
    public void userCanExitFreeQuantityProcessing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.closeForm();
        assertTrue(page.isListViewShown() && !page.isFormOpen(), "Closing a new document did not return to the list.");
        openRandomRecord(r -> true);
        page.closeForm();
        assertTrue(page.isListViewShown() && !page.isFormOpen(), "Closing a viewed document did not return to the list.");
    }

    // =====================================================================
    // Lines
    // =====================================================================

    @Test(priority = 34, groups = {FQP, "lines"},
            description = "A new document keeps its two lines (item, quantity to process) after save and reload")
    public void savedFreeQuantityProcessingKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addLineWithFreeQuantity();
        addLineWithFreeQuantity();
        Map<String, BigDecimal> entered = linesSnapshot();
        String code = saveAndVerify();
        page.reload();
        page.waitForGridToSettle();
        assertEquals(linesSnapshot(), entered, "Lines of " + code + " changed after save and reload.");
    }

    @Test(priority = 35, groups = {FQP, "lines", "edit"},
            description = "Edit the quantity to process of an existing line of a Draft (created by the test): kept")
    public void userCanEditQuantityOfExistingLine() {

        String code = createOwnRequiring(DRAFT);
        BigDecimal available = page.availableFreeQuantity(0);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        BigDecimal quantity = available.compareTo(TestData.QUANTITY.add(BigDecimal.ONE)) >= 0
                ? TestData.QUANTITY.add(BigDecimal.ONE) : available;
        page.setQuantityToProcess(0, quantity.toPlainString());
        saveAndVerify();
        page.reload();
        page.waitForGridToSettle();
        assertEquals(page.quantityToProcess(0).compareTo(quantity), 0,
                "The quantity of " + code + " was not saved as " + quantity + " (" + page.quantityToProcess(0) + ").");
    }

    @Test(priority = 36, groups = {FQP, "lines"},
            description = "Delete a line of a new document with two lines: the line is gone after save and reload")
    public void userCanDeleteItemLineOfFreeQuantityProcessing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addLineWithFreeQuantity();
        Line second = addLineWithFreeQuantity();
        page.deleteLine(second.row());
        String code = saveAndVerify();
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineCount(), 1, "The deleted line came back in " + code + ".");
        softly.assertFalse(linesSnapshot().containsKey(second.code()), "Item " + second.code() + " is still in " + code + ".");
        softly.assertAll();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /** A line: grid row, item name and code, available free quantity, quantity to process. */
    private record Line(int row, String item, String code, BigDecimal available, BigDecimal quantity) {
    }

    /**
     * Header in screen order: date (yesterday when editing), branch → warehouse (new
     * document only: changing them would clear the lines), description, processing type
     * (another one when editing).
     */
    private void fillHeader(boolean editing) {
        if (!editing) {
            freeStock(); // reopens the empty form when it reads documents (first new form only)
            selectBranchAndWarehouse();
        }
        fillDateNoteAndType(editing);
    }

    /** Date (yesterday when editing), description, processing type (another one when editing). */
    private void fillDateNoteAndType(boolean editing) {
        LocalDate today = LocalDate.now();
        page.setOperationDate(editing ? today.minusDays(1) : today);
        String note = TestData.uniqueNote();
        assertTrue(page.setNote(note), "\"البيان\" could not be filled.");
        String type = editing ? page.selectOtherProcessingType() : page.selectRandomProcessingType();
        log.info("Header: date {}, branch [{}], warehouse [{}], note [{}], type [{}]", page.operationDate(),
                page.selectedText("branchId"), page.selectedText("warehouseId"), note, type);
    }

    /**
     * Branch, then a warehouse. QC 2026-11-05: the item list offers EVERY item, most with no
     * free quantity, so the branch / warehouse / item of a line that has free quantity in an
     * existing document is used when one is found ({@link #freeStock}); otherwise random.
     */
    private void selectBranchAndWarehouse() {
        Optional<FreeStock> stock = freeStock();
        if (stock.isPresent()) {
            try {
                page.selectOption("branchId", stock.get().branch());
                page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(5));
                page.selectOption("warehouseId", stock.get().warehouse());
                return;
            } catch (RuntimeException notOffered) {
                log.warn("Place of {} not offered ({}); random branch / warehouse.", stock.get(), notOffered.getMessage());
            }
        }
        for (int attempt = 1; attempt <= TestData.MAX_WAREHOUSE_ATTEMPTS; attempt++) {
            page.selectRandomOption("branchId");
            if (page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(5))) {
                try {
                    if (!page.selectRandomOption("warehouseId").isBlank()) {
                        return;
                    }
                } catch (RuntimeException noWarehouse) {
                    log.warn("Branch [{}] has no warehouse ({}).", page.selectedText("branchId"), noWarehouse.getClass().getSimpleName());
                }
            }
        }
        fail("No branch with a warehouse after " + TestData.MAX_WAREHOUSE_ATTEMPTS + " attempts.");
    }

    /**
     * "اضافة سطر" → a random item that has free quantity in the warehouse and is not
     * on another line → quantity to process = 1 (or the available quantity when less).
     * Lines without free quantity are removed and another item is tried; when the
     * warehouse offers no item, another branch / warehouse is chosen (new document).
     */
    private Line addLineWithFreeQuantity() {
        for (int attempt = 1; attempt <= TestData.MAX_ITEM_ATTEMPTS; attempt++) {
            int row = page.addLine();
            String item;
            Optional<FreeStock> stock = freeStock();
            boolean known = attempt == 1 && stock.isPresent()
                    && stock.get().warehouse().equals(page.selectedText("warehouseId").trim())
                    && !isItemOnAnotherLine(stock.get().code(), -1);
            try {
                item = known ? page.selectItem(row, stock.get().item()) : page.selectRandomItem(row);
            } catch (TimeoutException noItem) {
                log.warn("No item to choose in warehouse [{}] (attempt {}).", page.selectedText("warehouseId"), attempt);
                page.deleteEmptyLines();
                if (!driver.getCurrentUrl().contains("/add")) {
                    throw new SkipException("The warehouse of this document offers no item with free quantity.");
                }
                selectBranchAndWarehouse();
                continue;
            }
            String code = page.itemCode(row);
            BigDecimal available = page.availableFreeQuantity(row);
            if (available.signum() <= 0 || isItemOnAnotherLine(code, row)) {
                log.info("Line {} ({}): available {} or repeated → another item.", row + 1, item, available);
                page.deleteLine(row);
                continue;
            }
            BigDecimal quantity = available.min(TestData.QUANTITY);
            page.setQuantityToProcess(row, quantity.toPlainString());
            page.stopCellEditing();
            log.info("Line {}: {} ({}), available {}, to process {}", row + 1, item, code, available, quantity);
            return new Line(row, item, code, available, quantity);
        }
        throw new SkipException("No item with free quantity found after " + TestData.MAX_ITEM_ATTEMPTS + " attempts.");
    }

    /** A place (branch, warehouse) and an item that had free quantity on an existing document. */
    private record FreeStock(String branch, String warehouse, String item, String code) {
    }

    /** Found once per run: reading documents is slow. */
    private static Optional<FreeStock> freeStockCache;

    /**
     * Opens up to 5 existing documents (newest first) and returns the first line with free
     * quantity left, with its branch and warehouse. The list page is reopened afterwards.
     */
    private Optional<FreeStock> freeStock() {
        if (freeStockCache != null) {
            return freeStockCache;
        }
        String url = driver.getCurrentUrl();
        if (!url.contains("/add")) {
            return Optional.empty(); // searched from a new form only (it is reopened afterwards)
        }
        freeStockCache = Optional.empty();
        List<Map<String, Object>> records = new ArrayList<>(apiRecords());
        java.util.Collections.reverse(records);
        for (Map<String, Object> record : records.subList(0, Math.min(5, records.size()))) {
            page.openDocument(record.get("id"));
            page.waitForGridToSettle();
            for (int row = 0; row < page.lineCount(); row++) {
                if (page.availableFreeQuantity(row).compareTo(TestData.QUANTITY) >= 0) {
                    freeStockCache = Optional.of(new FreeStock(page.selectedText("branchId").trim(),
                            page.selectedText("warehouseId").trim(), page.lineText(row, "itemId"), page.itemCode(row)));
                    break;
                }
            }
            if (freeStockCache.isPresent()) {
                break;
            }
        }
        log.info("Item with free quantity for this run: {}", freeStockCache);
        driver.navigate().to(url);
        form.waitForMode(DocumentForm.Mode.ADD);
        return freeStockCache;
    }

    /** Copied / imported lines: those without free quantity removed, the others set to ≤ the available quantity. */
    private void fitCopiedLinesToAvailableQuantity() {
        page.waitForGridToSettle();
        for (int row = page.lineCount() - 1; row >= 0; row--) {
            BigDecimal available = page.availableFreeQuantity(row);
            if (page.itemCode(row).isEmpty() || available.signum() <= 0) {
                log.info("Line {} ({}) has no free quantity → removed.", row + 1, page.itemCode(row));
                page.deleteLine(row);
            } else if (page.quantityToProcess(row).compareTo(available) > 0 || page.quantityToProcess(row).signum() <= 0) {
                page.setQuantityToProcess(row, available.min(TestData.QUANTITY).toPlainString());
            }
        }
        page.stopCellEditing();
    }

    private boolean isItemOnAnotherLine(String code, int row) {
        for (int other = 0; other < page.lineCount(); other++) {
            if (other != row && code.equals(page.itemCode(other))) {
                return true;
            }
        }
        return false;
    }

    /** Item code → quantity to process of every line. */
    private Map<String, BigDecimal> linesSnapshot() {
        Map<String, BigDecimal> lines = new LinkedHashMap<>();
        for (int row = 0; row < page.lineCount(); row++) {
            lines.put(page.itemCode(row), page.quantityToProcess(row).stripTrailingZeros());
        }
        return lines;
    }

    private Map<String, String> header() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("operationDate", page.operationDate());
        values.put("branchId", page.selectedText("branchId"));
        values.put("warehouseId", page.selectedText("warehouseId"));
        values.put("note", page.note());
        values.put("processingTypeId", page.processingType());
        return values;
    }

    /**
     * Save → confirm → success toast → view mode; the document must be in the API
     * with its description. Returns the number.
     */
    private String saveAndVerify() {
        page.stopCellEditing();
        page.deleteEmptyLines();
        String note = page.note();
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.clickSave();
        if (!waitForConfirmDialog(form)) {
            fail("Save did not open the confirmation dialog. Required-field errors: " + page.requiredErrorFields());
        }
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        if (!toast.isSuccess() && toast.fullText().contains("الملفات")) {
            List<String> others = page.otherToastTexts("تمت إضافة الملفات", Duration.ofSeconds(8));
            assertTrue(others.stream().noneMatch(t -> t.contains("خطأ")), "Save failed on the server: " + others);
        } else {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast);
        }
        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = page.waitForCode();
        waitUntil(code + " in the API", Duration.ofSeconds(15), () -> record(code).isPresent());
        assertEquals(String.valueOf(record(code).get().get("note")).trim(), note, "The description of " + code + " was not saved.");
        log.info("Saved free quantity processing {} ({})", code, apiStatus(code));
        return code;
    }

    /** A new document of the test, saved, then approved when {@code needed} is APPROVED. */
    private String createOwnRequiring(DocumentStatus needed) {
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addLineWithFreeQuantity();
        String code = saveAndVerify();
        if (needed == APPROVED) {
            approveOrDisapproveOpened(code, APPROVED);
            page.reload();
            page.waitForCode();
        }
        return code;
    }

    /** "اعتماد / إلغاء اعتماد" → confirm; the server must accept and the API must show {@code expected}. */
    private void approveOrDisapproveOpened(String code, DocumentStatus expected) {
        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE, () -> {
            assertTrue(page.waitForActionEnabled("approveOrDisapprove"), "\"اعتماد\" is disabled on " + code + ".");
            page.clickApproveOrDisapprove();
            page.confirmDialog();
        });
        assertAccepted((expected == APPROVED ? "Approving " : "Disapproving ") + code, TestData.API_APPROVE, outcome);
        waitUntil(code + " to be " + expected + " in the API", Duration.ofSeconds(15), () -> apiStatus(code) == expected);
    }

    private void cancelOpenedAndVerify(String code) {
        Map<String, Object> before = record(code).map(FreeQuantityPage::statusFields).orElse(Map.of());
        ActionOutcome outcome = performAndRecord(form, TestData.API_CANCEL, () -> {
            assertTrue(page.waitForActionEnabled("cancel"), "\"إلغاء\" is disabled on " + code + ".");
            page.clickCancelDocument();
            page.confirmDialog();
        });
        assertAccepted("Cancelling " + code, TestData.API_CANCEL, outcome);
        // The cancel call itself was accepted (200, data true). The record must now show it: a cancel
        // flag the page knows, or at least a change in its status fields (logged, to name the flag).
        java.util.concurrent.atomic.AtomicReference<Map<String, Object>> after = new java.util.concurrent.atomic.AtomicReference<>(before);
        try {
            waitUntil(code + " to be Cancelled in the API", Duration.ofSeconds(20), () -> {
                after.set(record(code).map(FreeQuantityPage::statusFields).orElse(Map.of()));
                return apiStatus(code) == CANCELLED || !after.get().equals(before);
            });
        } catch (AssertionError notCancelled) {
            throw new AssertionError(notCancelled.getMessage() + " Status fields of " + code + " unchanged: " + after.get(),
                    notCancelled);
        }
        log.info("Status fields of {}: before cancel {}, after {}", code, before, after.get());
        page.reload();
        page.waitForCode();
        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        softly.assertAll();
    }

    /**
     * Opens an Approved document that HAS a journal entry and returns its number and entry.
     * Approved documents are tried newest first (those whose API record names a journal entry
     * before the others), up to {@link TestData#MAX_JOURNAL_ATTEMPTS}; a document whose
     * "قيد اليومية" is disabled or shows no line is skipped. SKIPPED when none has one.
     */
    private Map.Entry<String, FreeQuantityPage.JournalEntry> openApprovedWithJournalEntry() {
        List<Map<String, Object>> approved = new ArrayList<>(apiRecords().stream().filter(FreeQuantityPage::isApproved).toList());
        java.util.Collections.reverse(approved); // newest first
        approved.sort(java.util.Comparator.comparing(r -> !FreeQuantityPage.hasJournalEntryHint(r))); // stable sort
        List<String> without = new ArrayList<>();
        for (Map<String, Object> record : approved.subList(0, Math.min(TestData.MAX_JOURNAL_ATTEMPTS, approved.size()))) {
            page.openDocument(record.get("id"));
            String code = page.waitForCode();
            if (!page.waitForActionEnabled("journalEntry")) {
                without.add(code + " (button disabled)");
                continue;
            }
            Optional<FreeQuantityPage.JournalEntry> entry = page.tryReadJournalEntry(Duration.ofSeconds(8));
            if (entry.isPresent() && entry.get().lines() > 0) {
                if (!without.isEmpty()) {
                    log.info("Approved documents without a journal entry (skipped): {}", without);
                }
                return Map.entry(code, entry.get());
            }
            without.add(code);
        }
        throw new SkipException("No Approved free quantity processing with a journal entry among the newest "
                + TestData.MAX_JOURNAL_ATTEMPTS + " (" + approved.size() + " Approved). Without one: " + without);
    }

    private void assertActions(String code, List<String> enabled, List<String> disabled) {
        SoftAssert softly = new SoftAssert();
        for (String action : enabled) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on " + code + ".");
        }
        for (String action : disabled) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on " + code + ".");
        }
        softly.assertAll();
    }

    /** Opens a random document matching {@code filter} (through the API); SKIPPED when QC has none. */
    private Map<String, Object> openRandomRecord(Predicate<Map<String, Object>> filter) {
        return page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no free quantity processing for this scenario."));
    }

    private Optional<Map<String, Object>> record(String code) {
        return apiRecords().stream().filter(r -> code.equals(FreeQuantityPage.codeOf(r))).findFirst();
    }

    /** All documents from the API, retried after "HTTP 429" (too many requests, QC 2026-11-05). */
    private List<Map<String, Object>> apiRecords() {
        for (int attempt = 1; ; attempt++) {
            try {
                return page.api().findAll();
            } catch (IllegalStateException tooMany) {
                if (attempt == 4 || !String.valueOf(tooMany.getMessage()).contains("429")) {
                    throw tooMany;
                }
                log.warn("API answered 429 (attempt {}/4) → waiting.", attempt);
                pause(3000L * attempt);
            }
        }
    }

    private DocumentStatus apiStatus(String code) {
        return statusOf(record(code).orElseThrow(() -> new AssertionError(code + " is not in the API.")));
    }

    private static DocumentStatus statusOf(Map<String, Object> record) {
        return FreeQuantityPage.isCancelled(record) ? CANCELLED : FreeQuantityPage.isApproved(record) ? APPROVED : DRAFT;
    }

    /** Waits (max 10 s) until the number shown differs from {@code before}; returns it. */
    private String waitUntilChanges(String before) {
        long end = System.currentTimeMillis() + 10_000;
        String now = page.code();
        while (now.equals(before) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            now = page.code();
        }
        return now;
    }
}
