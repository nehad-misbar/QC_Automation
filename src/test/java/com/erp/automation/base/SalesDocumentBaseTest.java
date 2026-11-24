package com.erp.automation.base;

import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.purchasing.PurchasingDocumentPage;
import com.erp.automation.pages.sales.SalesDocumentPage;
import com.erp.automation.pages.sales.SalesInvoiceDocumentPage;
import com.erp.automation.pages.sales.SalesPage;
import com.erp.automation.ui.DocumentList;
import com.erp.automation.ui.Downloads;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.function.Predicate;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * Template for the Sales document screens that follow the quotation
 * (أمر البيع، فواتير المبيعات، مردود المبيعات).
 *
 * <p>It holds:</p>
 * <ul>
 *     <li>the tests every one of these screens needs in the same way (list,
 *     search, record navigation, pager, Kanban, group by, print / PDF / Excel,
 *     attachments, exit, log, empty save). They run once per screen class and
 *     appear in the report under that screen;</li>
 *     <li>the reusable steps (fill a line, save and check the result, …),
 *     driven by what the subclass says about its screen (customer field,
 *     quantity / price columns, statuses after save, …).</li>
 * </ul>
 * <p>A subclass adds the scenarios only its screen has.</p>
 *
 * @param <P> the screen's page object
 */
public abstract class SalesDocumentBaseTest<P extends SalesDocumentPage> extends SalesBaseTest {

    protected P page;
    protected DocumentForm form;

    // =====================================================================
    // What the subclass says about its screen
    // =====================================================================

    /** Clicks the screen's link under المبيعات → العمليات. */
    protected abstract void clickScreenLink(SalesPage salesPage);

    protected abstract P createPage();

    /** List path, e.g. /sales/sales-order. */
    protected abstract String screenPath();

    /** col-ids the list must show. */
    protected abstract List<String> listColumns();

    /** Start of a new number, e.g. "SO". */
    protected abstract String codePrefix();

    /** Field of "العميل": partnerId (orders) or vendorId (invoices, returns); null when the screen has none (pricing). */
    protected abstract String customerField();

    /** Quantity col-id of the lines; null when the lines have no quantity (pricing). */
    protected abstract String quantityColumn();

    protected abstract String priceColumn();

    /** "⋮" item that exports the opened document to Excel. */
    protected abstract String exportMenuItem();

    /** Group-by option of the status column ("الحالة" / "حالة الفاتورة"). */
    protected abstract String statusGroupLabel();

    /** Fields marked "هذا الحقل مطلوب" by an empty save. */
    protected abstract List<String> requiredMarkedOnEmptySave();

    /** Status (form) of a newly saved document. */
    protected abstract DocumentStatus[] statusesAfterSave();

    /** Prefix of "البيان" written by the tests, so a saved value is told apart from old data. */
    protected abstract String notePrefix();

    /** Fills the header of a new document ({@code editing} = change every editable value of a saved one). */
    protected abstract void fillHeader(boolean editing);

    /**
     * Opens a document and switches it to edit mode (Draft order: {@code /edit/};
     * invoice / return: {@code /view-approved/}). Used by the attachment and exit tests.
     *
     * @return the opened record.
     */
    protected abstract Map<String, Object> openDocumentInEditMode();

    @Override
    protected void openScreen(SalesPage salesPage) {
        clickScreenLink(salesPage);
        page = createPage();
        form = new DocumentForm(driver, page.prefix());
        page.dismissInstallPrompt();
        page.waitForListLoaded();
    }

    // =====================================================================
    // Shared test data
    // =====================================================================

    protected static final String QUANTITY = "3";
    protected static final String PRICE = "100";
    protected static final String LINE_DISCOUNT_RATE = "5";
    protected static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";
    protected static final String NO_LINES_ERROR = "يجب اضافة صنف واحد على الأقل";
    protected static final String LOG_CREATED = "انشاء العملية";
    protected static final String LOG_APPROVED = "اعتماد العملية";
    protected static final String ATTACHMENT_TITLE = "مرفقات مستند مبيعات تجريبي";
    protected static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات مستند المبيعات";
    protected static final String LINKED_ERROR = "مرتبط";
    protected static final int MAX_BRANCH_ATTEMPTS = 5;
    protected static final int MAX_ITEM_ATTEMPTS = 8;
    protected static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    protected String uniqueNote() {
        return notePrefix() + LocalDateTime.now().format(STAMP);
    }

    protected static String stamped(String prefix) {
        return prefix + LocalDateTime.now().format(STAMP);
    }

    // =====================================================================
    // Shared tests: list and the path to the screen
    // =====================================================================

    @Test(priority = 1, groups = {"navigation"},
            description = "المبيعات → العمليات → the screen link opens its list with its columns and \"جديد\"")
    public void listOpensFromSalesMenu() {

        Map<String, String> columns = page.listColumns();
        log.info("List URL {} | columns {}", driver.getCurrentUrl(), columns);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().endsWith(screenPath()), "The menu did not open " + screenPath()
                + ": " + driver.getCurrentUrl());
        softly.assertTrue(page.isNewButtonShown(), "\"جديد\" is not shown on the list.");
        softly.assertTrue(page.visibleRowCount() > 0, "The list shows no rows.");
        for (String column : listColumns()) {
            softly.assertTrue(columns.containsKey(column), "List column [" + column + "] is missing: " + columns);
        }
        softly.assertAll();
    }

    @Test(priority = 2, groups = {"navigation"},
            description = "\"جديد\" opens an empty document: a new number, today's date, no customer, no status")
    public void newButtonOpensEmptyDocument() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        String code = waitUntilEquals(() -> page.code().isEmpty() ? "" : "loaded", "loaded");

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(code, "loaded", "A new document shows no number.");
        softly.assertTrue(page.code().startsWith(codePrefix()), "Unexpected new number: " + page.code());
        if (customerField() != null) {
            softly.assertTrue(isEmptySelect(customerField()), "\"العميل\" must be empty on a new document.");
        }
        softly.assertEquals(page.statusLabel(), "", "A new document must not show a status.");
        softly.assertTrue(page.isActionEnabled("xIcon"), "\"خروج\" must be enabled on a new document.");
        softly.assertAll();
        log.info("New document {}: date fields {}", page.code(), dateFieldsOfNewDocument());
        page.closeForm();
    }

    @Test(priority = 3, groups = {"navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that document")
    public void userCanOpenDocumentFromListRowActions() {

        List<String> items = page.rowActionItems();
        assertTrue(items.contains("عرض"), "The row menu has no \"عرض\": " + items);
        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        page.openFromRowActions(code);
        assertEquals(waitUntilEquals(page::code, code), code, "Row menu → عرض did not open " + code + ".");
    }

    @Test(priority = 4, groups = {"navigation", "list"},
            description = "Double-click on a list row opens that document with the status shown in the list")
    public void userCanOpenDocumentByDoubleClickOnListRow() {

        Map<String, String> rows = page.statusesOnCurrentPage();
        List<String> codes = new ArrayList<>(rows.keySet());
        String code = codes.get(new Random().nextInt(codes.size()));
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another document than " + code + ".");
        page.waitForStatusLabel(rows.get(code));
    }

    // =====================================================================
    // Shared tests: validation
    // =====================================================================

    @Test(priority = 50, groups = {"validation"},
            description = "Saving an empty document is blocked: error toast, required fields marked, stays in Add")
    public void userCannotSaveEmptyDocument() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        if (!page.waitForActionEnabled("save")) {
            // QC 2026-11-12 (invoices, returns): "حفظ" waits for a first change.
            page.setNote(uniqueNote());
            assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled after typing the description.");
        }

        page.clickSave();
        DocumentForm.Toast toast;
        try {
            toast = form.waitForToast();
        } catch (org.openqa.selenium.TimeoutException none) {
            toast = null;
        }
        List<String> marked = form.fieldsWithRequiredError();
        log.info("Empty save → toast [{}], required errors on {}", toast, marked);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast != null && toast.isError(), "Expected an error message, got: " + toast);
        softly.assertTrue(toast != null && toast.fullText().contains(NO_LINES_ERROR),
                "The error does not say that an item is needed: " + toast);
        softly.assertTrue(marked.containsAll(requiredMarkedOnEmptySave()),
                "Required fields not marked: expected " + requiredMarkedOnEmptySave() + ", marked " + marked);
        softly.assertFalse(form.isDialogOpen(), "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();
        page.closeForm();
    }

    // =====================================================================
    // Shared tests: search, record navigation, pager, Kanban, group by
    // =====================================================================

    @Test(priority = 60, groups = {"search"}, description = "List search by number shows that document")
    public void userCanSearchListByCode() {

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        page.searchList(code);
        assertTrue(page.waitUntilListShows(code), "Searching for " + code + " did not show it: " + page.visibleListCodes());
    }

    @Test(priority = 61, groups = {"search"}, description = "List search for a text no document has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {

        String text = "لا-يوجد-" + System.currentTimeMillis();
        page.searchList(text);
        waitUntil("an empty list for [" + text + "]", Duration.ofSeconds(10), () -> page.visibleListCodes().isEmpty());
    }

    @Test(priority = 62, groups = {"search"}, description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenDocumentFromRecordSearch() {

        openRandom(r -> true);
        String target = page.waitForCode();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
        assertNotEquals(waitUntilChanges(target), target, "Could not move away from " + target + ".");
        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 63, groups = {"navigation"}, description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenRecords() {

        openRandom(r -> true);
        SoftAssert softly = new SoftAssert();
        if (page.isRecordNavigationEnabled("pageFirst")) {
            String before = page.waitForCode();
            page.clickRecordNavigation("pageFirst");
            waitUntilChanges(before);
        }
        String first = page.waitForCode();
        for (String button : List.of("pageFirst", "pageBack")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the first record.");
        }
        page.clickRecordNavigation("pageNext");
        String second = waitUntilChanges(first);
        softly.assertNotEquals(second, first, "'Next' did not open another document.");
        page.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(page::code, first), first, "'Back' did not return to the first one.");
        page.clickRecordNavigation("pageLast");
        String last = waitUntilChanges(first);
        for (String button : List.of("pageLast", "pageNext")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the last record (" + last + ").");
        }
        softly.assertAll();
    }

    @Test(priority = 64, groups = {"pagination"},
            description = "List pager: first/last page, Next page by page to the end, back to a random page")
    public void userCanNavigateListPagination() {

        SoftAssert softly = new SoftAssert();
        page.goToFirstListPage();
        int[] first = page.paginationRange();
        int total = first[2];
        softly.assertEquals(first[0], 1, "The first page must start at record 1.");
        softly.assertFalse(page.isListPreviousPageEnabled(), "'Previous' must be disabled on the first page.");
        softly.assertEquals(total, page.api().count(), "The list total differs from the API.");

        page.goToLastListPage();
        int[] last = page.paginationRange();
        softly.assertEquals(last[1], total, "The last page must end at the last record.");
        softly.assertFalse(page.isListNextPageEnabled(), "'Next' must be disabled on the last page.");

        page.goToFirstListPage();
        int pageSize = first[1] - first[0] + 1;
        int pages = 1;
        // Long lists (215 invoices) are walked up to 30 pages.
        while (page.isListNextPageEnabled() && pages < 30) {
            int[] before = page.paginationRange();
            page.goToNextPage();
            int[] now = page.paginationRange();
            pages++;
            softly.assertEquals(now[0], before[1] + 1, "Page " + pages + " does not follow the previous one: "
                    + Arrays.toString(before) + " → " + Arrays.toString(now));
        }
        int target = 1 + new Random().nextInt(pages);
        for (int current = pages; current > target; current--) {
            int[] before = page.paginationRange();
            page.goToPreviousPage();
            softly.assertEquals(page.paginationRange()[1], before[0] - 1, "\"Previous\" did not go back exactly one page.");
        }
        softly.assertEquals(page.paginationRange()[0], (target - 1) * pageSize + 1, "Not on page " + target + ".");
        softly.assertAll();

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another document than " + code + ".");
    }

    @Test(priority = 65, groups = {"list"},
            description = "Kanban shows the same documents as the list page; a card opens its document")
    public void userCanSwitchListToKanbanAndBack() {

        page.waitForListLoaded();
        List<String> gridCodes = page.visibleListCodes();
        try {
            page.switchToKanbanView();
            List<String> cardCodes = page.kanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);
            // QC 2026-11-16: the invoice / return cards show "المستند المرجعي" where the
            // number belongs (empty on an invoice, the source SI… on a return).
            assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes),
                    "Kanban cards do not show the documents' own numbers. List page: " + gridCodes
                            + " | numbers on the cards: " + cardCodes
                            + (cardCodes.isEmpty() ? " (no number on any card)" : "") + ".");
            for (String code : cardCodes) {
                log.info("Card {} status [{}]", code, page.kanbanCardStatus(code));
            }
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

    @Test(priority = 66, groups = {"list"},
            description = "Group by status: every group has documents and the groups add up to the list total")
    public void userCanGroupListByStatus() {

        page.waitForListLoaded();
        int total = page.paginationRange()[2];
        page.groupListBy(statusGroupLabel());
        Map<String, Integer> groups = page.groupCounts();
        int sum = groups.values().stream().mapToInt(Integer::intValue).sum();
        log.info("Groups by status: {} (sum {}), list total {}", groups, sum, total);

        SoftAssert softly = new SoftAssert();
        for (Map.Entry<String, Integer> group : groups.entrySet()) {
            softly.assertTrue(group.getValue() > 0, "Group [" + group.getKey() + "] is empty.");
        }
        softly.assertEquals(sum, total, "Status groups add up to " + sum + " but the list total is " + total + ".");

        // QC 2026-11-16 (فواتير المبيعات): group "معتمد (222)" showed "ملغي" on its own
        // row while every invoice in it is معتمد. Each group row and each document in
        // it must show the group's status.
        for (DocumentList.GroupStatus group : page.groupStatuses()) {
            log.info("Group [{}]: its row shows [{}], documents on screen {}",
                    group.label(), group.rowStatus(), group.childStatuses());
            softly.assertTrue(group.rowStatus().isEmpty() || group.rowStatus().equals(group.label()),
                    "Group [" + group.label() + "] shows status [" + group.rowStatus() + "] on its own row.");
            softly.assertFalse(group.childStatuses().isEmpty(),
                    "Group [" + group.label() + "] shows no documents when expanded.");
            List<String> others = group.childStatuses().stream()
                    .filter(status -> !status.equals(group.label())).distinct().toList();
            softly.assertTrue(others.isEmpty(),
                    "Group [" + group.label() + "] holds documents with status " + others + ".");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Shared tests: print, PDF, Excel
    // =====================================================================

    @Test(priority = 70, groups = {"print"},
            description = "Print a random document: the preview shows its number; printed through Chrome's dialog")
    public void userCanPrintDocument() throws java.awt.AWTException {

        openRandom(r -> true);
        String code = page.waitForCode();
        String preview = page.openPrintPreviewWithRandomTemplate();
        assertTrue(preview.contains(code), "Print preview does not show " + code + ".");
        page.printFromPreview();
        page.confirmChromePrintDialog();
        page.closePrintPreview();
        assertEquals(page.waitForCode(), code, "After printing, the form does not show " + code + " any more.");
    }

    @Test(priority = 71, groups = {"print"}, description = "Save a random document as PDF: a PDF file is downloaded")
    public void userCanSaveDocumentAsPdf() {

        openRandom(r -> true);
        String code = page.waitForCode();
        Path dir = page.prepareDownloadFolder("pdf");
        String preview = page.openPrintPreviewWithRandomTemplate();
        assertTrue(preview.contains(code), "The print preview does not show " + code + ".");
        page.savePreviewAsPdf();
        Path pdf = page.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
    }

    @Test(priority = 72, groups = {"excel"}, description = "⋮ → export to Excel: an .xlsx with the document's items is downloaded")
    public void userCanExportDocumentToExcel() {

        openRandom(r -> true);
        String code = page.waitForCode();
        page.waitForGridToSettle();
        List<String> items = page.lineTexts(codeColumn());
        Path dir = page.prepareDownloadFolder("excel");

        page.clickMenuItem(exportMenuItem());

        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        SoftAssert softly = new SoftAssert();
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Shared tests: attachments, exit, transaction log
    // =====================================================================

    @Test(priority = 90, groups = {"attachments"},
            description = "Attachments added in edit mode are listed and kept after save and reload")
    public void userCanAddAttachments() {

        openDocumentInEditMode();
        String code = page.waitForCode();
        page.openAttachments();
        int before = page.attachmentCount();
        int files = page.countAttachmentFiles();
        page.addAttachmentFiles(ATTACHMENT_TITLE, ATTACHMENT_DESCRIPTION);
        assertEquals(page.waitForAttachmentCount(before + files), before + files, "The added files are not listed.");
        page.closeAttachments();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(currentStatuses());
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before + files), before + files,
                "Attachments of " + code + " were not kept after save and reload.");
    }

    @Test(priority = 91, groups = {"attachments"},
            description = "Deleting an attachment in edit mode is kept after save and reload")
    public void userCanDeleteAttachment() {

        openDocumentInEditMode();
        String code = page.waitForCode();
        page.openAttachments();
        int before = page.attachmentCount();
        if (before == 0) {
            int files = page.countAttachmentFiles();
            page.addAttachmentFiles(ATTACHMENT_TITLE, ATTACHMENT_DESCRIPTION);
            before = page.waitForAttachmentCount(files);
            page.closeAttachments();
            fillZeroQuantitiesAndPrices();
            DocumentStatus[] statuses = currentStatuses();
            saveAndVerify(statuses);
            page.clickEdit();
            waitForAnyEditMode();
            page.openAttachments();
        }
        String deleted = page.deleteLastAttachment();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1, "The deleted attachment is still listed.");
        page.closeAttachments();
        page.waitForToastToAppearAndClear();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(currentStatuses());
        page.reload();
        page.openAttachments();
        // Read when the list has stopped changing, and look for the deleted file itself:
        // a count read while the rows were still loading let a deleted attachment that had
        // come back pass (QC 2026-11-12 SQ2611-000050, 2026-11-15 SO2609-000025).
        List<String> after = page.attachmentRowsWhenLoaded();
        log.info("Attachments of {} after save + reload: {} (deleted: [{}])", code, after, deleted);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(after.size(), before - 1,
                "The deleted attachment of " + code + " came back after save and reload: " + after);
        softly.assertFalse(PurchasingDocumentPage.isAttachmentListed(after, deleted),
                "Attachment [" + PurchasingDocumentPage.attachmentKey(deleted) + "] of " + code
                        + " is still listed after save and reload.");
        softly.assertAll();
    }

    @Test(priority = 95, groups = {"exit"},
            description = "Close a new, a viewed and an edited document: each returns to the list")
    public void userCanExitDocument() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.clickAction("xIcon");
        if (waitForConfirmDialog(form)) { // a new, untouched invoice may close without asking
            page.confirmDialog();
        }
        assertBackOnList("closing a new document");

        openRandom(r -> true);
        String viewed = page.waitForCode();
        page.clickAction("xIcon");
        assertBackOnList("closing " + viewed + " in view mode");

        openDocumentInEditMode();
        String edited = page.waitForCode();
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing " + edited + " in edit mode did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing " + edited + " in edit mode");
    }

    @Test(priority = 96, groups = {"log"}, description = "Transaction log: the first entry is the creation")
    public void userCanViewTransactionLog() {

        openRandom(r -> true);
        String code = page.waitForCode();
        List<String> actions = page.readTransactionLog();
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log of " + code + " is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), LOG_CREATED, "The first log entry of " + code + " must be the creation.");
        softly.assertAll();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /** Opens a random document matching {@code filter} (chosen through the API); SKIPPED when QC has none. */
    protected Map<String, Object> openRandom(Predicate<Map<String, Object>> filter) {
        Map<String, Object> record = page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no document for this scenario on " + screenPath() + "."));
        page.waitForCode();
        return record;
    }

    /** Like {@link #openRandom} but empty instead of skipping. */
    protected Optional<Map<String, Object>> tryOpenRandom(Predicate<Map<String, Object>> filter) {
        Optional<Map<String, Object>> record = page.openRandomDocument(filter);
        record.ifPresent(r -> page.waitForCode());
        return record;
    }

    /** "جديد" → header → one item line → save; returns the API record of the new document. */
    protected Map<String, Object> createDocument() {
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        String code = saveAndVerify(statusesAfterSave());
        return page.recordByCode(code);
    }

    /** Edit mode of a Draft ({@code /edit/}) or of a saved invoice ({@code /view-approved/}). */
    protected void waitForAnyEditMode() {
        waitUntil("edit mode", Duration.ofSeconds(15),
                () -> driver.getCurrentUrl().contains("/edit/") || driver.getCurrentUrl().contains("/view-approved/"));
        waitUntil("an editable description", Duration.ofSeconds(10), page::isNoteEditable);
    }

    /** The statuses the opened document may have after saving it again. */
    protected DocumentStatus[] currentStatuses() {
        String label = page.statusLabel();
        return Arrays.stream(DocumentStatus.values()).filter(s -> s.label().equals(label)).findFirst()
                .map(s -> new DocumentStatus[]{s}).orElse(statusesAfterSave());
    }

    protected String waitUntilChanges(String previous) {
        long end = System.currentTimeMillis() + 10_000;
        String now = page.code();
        while ((now.isEmpty() || now.equals(previous)) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            now = page.code();
        }
        return now;
    }

    protected void assertBackOnList(String afterWhat) {
        waitUntil("the list after " + afterWhat, Duration.ofSeconds(15), () -> !page.isFormOpen());
        page.waitForListLoaded();
    }

    protected String waitForSelected(DocumentForm on, String field) {
        long end = System.currentTimeMillis() + 10_000;
        String value = on.getSelectedText(field);
        while (value.isEmpty() && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            value = on.getSelectedText(field);
        }
        return value;
    }

    /** Empty select; "اختر" is the placeholder shown when nothing is chosen. */
    protected boolean isEmptySelect(String field) {
        String text = form.getSelectedText(field);
        return text.isEmpty() || text.equals("اختر");
    }

    /** Random option when editable ({@code editing}: another one); confirms the optional dialog after it. */
    protected void selectIfEditable(String field, boolean editing) {
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, form.getSelectedText(field));
            return;
        }
        if (editing && !isEmptySelect(field)) {
            page.selectRandomOtherOption(field);
        } else {
            page.selectRandomOption(field);
        }
        page.confirmDialogIfShown(Duration.ofMillis(800));
    }

    /**
     * الفرع → المخزن. The warehouses depend on the branch (QC 2026-11-12) and
     * choosing the branch empties the warehouse, so the branch comes first;
     * when a branch offers no warehouse, another branch is chosen.
     */
    protected void selectBranchThenWarehouse(boolean editing) {
        retryUntilSuccessful("Branch → Warehouse (warehouse must not stay empty)", MAX_BRANCH_ATTEMPTS, () -> {
            try {
                if (page.waitForSelectEnabled("branchId", Duration.ofSeconds(5))) {
                    if (editing || !isEmptySelect("branchId")) {
                        page.selectRandomOtherOption("branchId");
                    } else {
                        page.selectRandomOption("branchId");
                    }
                    page.confirmDialogIfShown(Duration.ofMillis(800));
                }
                if (page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(5))) {
                    page.selectRandomOption("warehouseId");
                    page.confirmDialogIfShown(Duration.ofMillis(800));
                }
                String warehouse = form.waitForSelectedText("warehouseId", Duration.ofSeconds(3));
                if (warehouse.isEmpty() || warehouse.equals("اختر") || warehouse.equals("المخزن")) {
                    log.warn("Branch [{}] gave no warehouse → another branch.", form.getSelectedText("branchId"));
                    return false;
                }
                log.info("Branch [{}] → warehouse [{}]", form.getSelectedText("branchId"), warehouse);
                return true;
            } catch (RuntimeException failed) {
                log.warn("Branch → Warehouse failed ({}) → again.", failed.getMessage());
                return false;
            }
        });
    }

    // =====================================================================
    // "حساب الدفع" / payment fields (invoices and returns), QC 2026-11-18
    // =====================================================================

    /** Branches tried until "حساب الدفع" can be filled. */
    protected static final int MAX_BRANCHES_FOR_PAYMENT_ACCOUNT = 5;

    /**
     * "حساب الدفع" must not stay empty: its accounts depend on الفرع (QC run 2026-11-18 16:24,
     * SRI2611-000071: "فرع1-مأ" offered none for "نقدا / آجل", and the server refused the save
     * with "يجب أن يتم دفع جزء من المبلغ نقدا"). While it has no option, another الفرع → المخزن
     * is chosen (the same "طريقة الدفع" is kept), {@link #MAX_BRANCHES_FOR_PAYMENT_ACCOUNT}
     * times at most. A locked الفرع (e.g. brought by a source document) is not changed.
     * Screens without "حساب الدفع" (orders) are not concerned.
     */
    protected void ensurePaymentAccount() {
        if (!(page instanceof SalesInvoiceDocumentPage payable)) {
            return;
        }
        String method = payable.selectedText("paymentMethod");
        for (int attempt = 1; attempt <= MAX_BRANCHES_FOR_PAYMENT_ACCOUNT; attempt++) {
            if (!isPaymentAccountShownAndEmpty()) {
                return;
            }
            payable.fillFundAccountIfNeeded();
            if (!isPaymentAccountShownAndEmpty()) {
                log.info("حساب الدفع = {} (branch [{}], warehouse [{}], payment [{}]).",
                        payable.selectedText(SalesInvoiceDocumentPage.FUND_ACCOUNT_FIELD), form.getSelectedText("branchId"),
                        form.getSelectedText("warehouseId"), method);
                return;
            }
            if (!payable.waitForSelectEnabled("branchId", Duration.ofSeconds(1))) {
                break; // الفرع is locked: nothing else can be tried
            }
            log.warn("حساب الدفع has no option for branch [{}] / warehouse [{}] (payment [{}]) → another branch ({}/{}).",
                    form.getSelectedText("branchId"), form.getSelectedText("warehouseId"), method, attempt,
                    MAX_BRANCHES_FOR_PAYMENT_ACCOUNT);
            selectBranchThenWarehouse(true);
            if (!method.isBlank() && !method.equals(payable.selectedText("paymentMethod"))) {
                payable.selectPaymentMethod(method); // fills "حساب الدفع" too
            } else {
                payable.fillFundAccountIfNeeded();
            }
        }
        if (isPaymentAccountShownAndEmpty()) {
            fail("\"حساب الدفع\" stayed empty for \"" + method + "\" (last branch [" + form.getSelectedText("branchId")
                    + "], warehouse [" + form.getSelectedText("warehouseId") + "], " + MAX_BRANCHES_FOR_PAYMENT_ACCOUNT
                    + " branches at most; a locked branch is not changed).");
        }
    }

    /** True when "حساب الدفع" is shown, can be chosen and is empty. */
    protected boolean isPaymentAccountShownAndEmpty() {
        String field = SalesInvoiceDocumentPage.FUND_ACCOUNT_FIELD;
        return page.hasField(field, Duration.ofMillis(500)) && page.waitForSelectEnabled(field, Duration.ofSeconds(1))
                && isEmptySelect(field);
    }

    /**
     * What the payment method shows besides its selects, once the lines give the total:
     * every empty date (e.g. "تاريخ الاستحقاق": the 15th, one year ahead) and the amounts
     * left empty or 0. One amount gets half the total with tax ("نقدا / آجل" needs a part paid
     * in cash, QC run 2026-11-18 16:25); two amounts (a split method) get half and the rest,
     * so that together they are exactly the total ("مبلغ الدفع اكبر من قيمة الفاتورة" otherwise).
     */
    protected void fillPaymentAmountsAndDates() {
        if (!(page instanceof SalesInvoiceDocumentPage payable)) {
            return;
        }
        LocalDate due = LocalDate.now().plusYears(1).withDayOfMonth(15);
        for (String field : payable.emptyDateFields(false)) {
            payable.setDateField(field, false, due);
            log.info("Empty date {} → {}", field, due);
        }
        List<String> amounts = payable.enabledTextInputs(false).stream()
                .filter(field -> {
                    String name = field.toLowerCase();
                    return (name.contains("amount") || name.contains("paid")) && !name.contains("bulkdiscount");
                }).toList();
        if (amounts.isEmpty() || amounts.stream().noneMatch(field -> number(page.textValue(field)).signum() == 0)) {
            return;
        }
        BigDecimal total = payable.totalShown("الإجمالي شامل الضريبة");
        if (total == null || total.signum() <= 0) {
            log.warn("Payment amounts {} are empty but no total with tax is shown.", amounts);
            return;
        }
        BigDecimal half = total.divide(BigDecimal.valueOf(2), 2, java.math.RoundingMode.DOWN);
        List<String> ordered = new ArrayList<>(amounts);
        ordered.sort(java.util.Comparator.comparing(field -> !field.toLowerCase().contains("cash")));
        for (int i = 0; i < ordered.size(); i++) {
            BigDecimal value = ordered.size() == 1 || i == 0 ? half : i == 1 ? total.subtract(half) : BigDecimal.ZERO;
            page.setTextInput(ordered.get(i), value.stripTrailingZeros().toPlainString());
            log.info("Payment amount {} → {} (total with tax {}).", ordered.get(i), value.stripTrailingZeros().toPlainString(),
                    total.stripTrailingZeros().toPlainString());
        }
    }

    /** العميل → الفرع → المخزن: the minimum for "اضافة سطر". */
    protected void selectCustomerBranchAndWarehouse() {
        selectIfEditable(customerField(), false);
        selectBranchThenWarehouse(false);
    }

    private boolean isItemOnAnotherLine(String code, int row) {
        int rows = form.gridRowCount();
        for (int other = 0; other < rows; other++) {
            if (other != row && code.equals(page.lineText(other, codeColumn()))) {
                return true;
            }
        }
        return false;
    }

    /** Adds one item line (lines already there are completed first); returns its item code. */
    protected String addItemLine() {
        fillZeroQuantitiesAndPrices();
        page.deleteEmptyLines();
        int row = page.addLine();
        for (int attempt = 1; attempt <= MAX_ITEM_ATTEMPTS; attempt++) {
            String item = page.selectRandomItem(row);
            if (page.lineText(row, "uomId").isEmpty()) {
                form.closeEmptyItemSearchDialog();
                log.warn("Item {} did not load into line {} → another item ({}/{}).", item, row + 1, attempt, MAX_ITEM_ATTEMPTS);
                continue;
            }
            String code = page.lineText(row, codeColumn());
            if (isItemOnAnotherLine(code, row)) {
                log.warn("Item {} ({}) is already on another line → another item.", item, code);
                continue;
            }
            fillNewLine(row);
            page.stopCellEditing();
            log.info("Line {}: {} ({}) × {} @ {}", row + 1, item, code, QUANTITY, PRICE);
            return code;
        }
        fail("No accepted item found for the new line after " + MAX_ITEM_ATTEMPTS + " attempts.");
        return "";
    }

    /**
     * Cells of a new line once its item is chosen ({@link #addItemLine()}): the quantity
     * and the price. A screen overrides it to fill every editable cell of its lines.
     */
    protected void fillNewLine(int row) {
        if (quantityColumn() != null) {
            page.setLineNumber(row, quantityColumn(), QUANTITY);
        }
        page.setLineNumber(row, priceColumn(), PRICE);
    }

    /** Lines already in the grid: a repeated item is removed; empty / 0 quantity or price gets the test value. */
    protected void fillZeroQuantitiesAndPrices() {
        page.waitForGridToSettle();
        int rows = form.gridRowCount();
        Map<String, Integer> firstRowOfItem = new HashMap<>();
        for (int row = 0; row < rows; row++) {
            firstRowOfItem.putIfAbsent(page.lineText(row, codeColumn()), row);
        }
        for (int row = rows - 1; row >= 0; row--) {
            String code = page.lineText(row, codeColumn());
            if (!code.isEmpty() && firstRowOfItem.get(code) != row) {
                page.deleteLine(row);
                continue;
            }
            if (code.isEmpty()) {
                continue;
            }
            if (quantityColumn() != null && page.lineNumber(row, quantityColumn()).signum() == 0) {
                page.setLineNumber(row, quantityColumn(), QUANTITY);
            }
            if (page.lineNumber(row, priceColumn()).signum() == 0) {
                page.setLineNumber(row, priceColumn(), PRICE);
            }
        }
        page.stopCellEditing();
    }

    /** "code × quantity @ price" of every line. */
    protected List<String> linesSnapshot() {
        List<String> lines = new ArrayList<>();
        int count = page.lineCount();
        for (int row = 0; row < count; row++) {
            lines.add(page.lineText(row, codeColumn()) + " × "
                    + (quantityColumn() == null ? "-" : page.lineNumber(row, quantityColumn()).stripTrailingZeros().toPlainString())
                    + " @ " + page.lineNumber(row, priceColumn()).stripTrailingZeros().toPlainString());
        }
        return lines;
    }

    /** Item codes of the lines, as a set. */
    protected java.util.Set<String> lineCodes() {
        return new java.util.LinkedHashSet<>(page.lineTexts(codeColumn()));
    }

    /** col-id of the item code in the lines: "code", or "itemCode" on pricing. */
    protected String codeColumn() {
        return "code";
    }

    /** Text of the top-most open dialog. */
    protected String dialogText() {
        Object text = ((JavascriptExecutor) driver).executeScript(
                "const d=[...document.querySelectorAll(\"[role=dialog][data-state=open],[role=alertdialog][data-state=open]\")];"
                        + "return d.length ? d[d.length-1].innerText : '';");
        return text == null ? "" : text.toString();
    }

    /** "اعتماد" / "عدم اعتماد", then "موافق". */
    protected void toggleApproval() {
        assertTrue(page.waitForActionEnabled("approveOrDisapprove"), "\"اعتماد\" is disabled on " + page.code() + ".");
        page.clickApproveOrDisapprove();
        page.confirmDialog();
    }

    /** "إلغاء" then "موافق"; returns the toast and the API answer. */
    protected ActionOutcome cancelOpenedDocument() {
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("cancel"), "\"إلغاء\" is disabled on " + code + ".");
        return performAndRecord(form, "cancel", () -> {
            page.clickCancelDocument();
            page.confirmDialog();
        });
    }

    /**
     * Hook: false when a screen checks "البيان" in a test of its own, so that
     * {@link #saveAndVerify} checks only the save itself (message, view mode,
     * status) and every test that saves is not failed by the description.
     */
    protected boolean verifiesNoteOnSave() {
        return true;
    }

    /** Questions a save may ask after its confirmation before the toast. */
    private static final int MAX_SAVE_QUESTIONS = 3;

    /**
     * Saves and verifies the RESULT: "هل انت متاكد من عملية الحفظ؟" → نعم (and a
     * second confirmation when one is asked) → success toast → view mode →
     * number → "البيان" persisted → status.
     *
     * @return the saved document's number.
     */
    protected String saveAndVerify(DocumentStatus... allowedStatuses) {

        String note = page.note();
        if (!note.startsWith(notePrefix())) {
            note = uniqueNote();
            page.setNote(note);
        }
        page.stopCellEditing();
        page.deleteEmptyLines();
        fillRequiredFieldsBeforeSave();
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.startRecordingSaveRequest();
        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForConfirmationDialogOrToast().orElse(null);
        // A save may ask several questions in a row (QC 2026-11-19, pricing:
        // "أصناف بدون تسعير" then "أصناف الفارق فيها بالسالب"): confirm each one.
        for (int question = 1; toast == null && question <= MAX_SAVE_QUESTIONS; question++) {
            if (!page.confirmDialogIfShown(Duration.ofSeconds(2))) {
                break;
            }
            log.warn("Save asked to continue ({}) → confirmed.", question);
            toast = form.waitForConfirmationDialogOrToast().orElse(null);
        }
        if (toast == null) {
            toast = form.waitForToast();
        }
        boolean uploadOnly = false;
        if (isUploadToast(toast)) {
            List<String> others = page.otherToastTexts("تمت إضافة الملفات", Duration.ofSeconds(8));
            assertTrue(others.stream().noneMatch(t -> t.contains("خطأ")), "Save failed on the server: " + others
                    + " | lines sent: " + page.lastSavedLines());
            uploadOnly = others.stream().noneMatch(t -> t.contains(SAVE_SUCCESS_MESSAGE));
        }
        boolean warning = isWarning(toast) || uploadOnly || isUploadToast(toast);
        if (!warning) {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast + " | lines sent: " + page.lastSavedLines());
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = page.waitForCode();
        boolean checkNote = verifiesNoteOnSave();
        String shownNote = checkNote ? page.waitForNote(note) : note;
        String status = page.statusLabel();
        if (!note.equals(shownNote) || !isOneOf(status, allowedStatuses)) {
            log.warn("After saving {}: note [{}] status [{}] → reloading.", code, shownNote, status);
            page.reload();
            shownNote = checkNote ? page.waitForNote(note) : note;
            status = page.statusLabel();
        }
        SoftAssert softly = new SoftAssert();
        if (!warning) {
            softly.assertTrue(toast.fullText().contains(SAVE_SUCCESS_MESSAGE), "Unexpected success message: " + toast);
        }
        softly.assertEquals(shownNote, note, "\"البيان\" was not persisted in " + code + ".");
        softly.assertTrue(isOneOf(status, allowedStatuses), "Unexpected status [" + status + "] after saving " + code
                + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();
        log.info("Saved {} with status {}", code, status);
        return code;
    }

    /** Hook: required fields the screen fills right before saving (e.g. "حساب الدفع"). */
    protected void fillRequiredFieldsBeforeSave() {
        List<String> fields = new ArrayList<>(List.of("currencyId", "taxCalculationMethode"));
        if (customerField() != null) {
            fields.add(0, customerField());
        }
        for (String field : fields) {
            if (page.hasField(field, Duration.ofMillis(300)) && isEmptySelect(field)
                    && page.waitForSelectEnabled(field, Duration.ofSeconds(1))) {
                page.selectRandomOption(field);
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }
        }
        if (!page.hasField("branchId", Duration.ofMillis(300)) || !page.hasField("warehouseId", Duration.ofMillis(300))) {
            return; // pricing has neither
        }
        if ((isEmptySelect("branchId") && page.waitForSelectEnabled("branchId", Duration.ofSeconds(1)))
                || (isEmptySelect("warehouseId") && page.waitForSelectEnabled("warehouseId", Duration.ofSeconds(1)))) {
            selectBranchThenWarehouse(false);
        }
    }

    /**
     * "إضافة" / "تكرار" must create a NEW document and leave the source as it
     * was; the new one is found in the API by its unique description.
     */
    protected String verifyNewDocumentSaved(Map<String, Object> source, String how) {
        String note = page.note();
        String sourceCode = String.valueOf(source.get(page.codeField()));
        Map<String, Object> saved = page.recordByNote(note);
        assertTrue(saved != null, "No document with the description [" + note + "] in the API after " + how + ".");
        String newCode = String.valueOf(saved.get(page.codeField()));
        Map<String, Object> sourceNow = page.recordByCode(sourceCode);
        SoftAssert softly = new SoftAssert();
        softly.assertNotEquals(newCode, sourceCode, how + " saved over the source " + sourceCode + ".");
        if (sourceNow != null) {
            for (String field : List.of("recordStatus", "invoiceStatus", "notes", "note")) {
                if (source.containsKey(field)) {
                    softly.assertEquals(String.valueOf(sourceNow.get(field)), String.valueOf(source.get(field)),
                            "\"" + field + "\" of the source " + sourceCode + " changed after " + how + ".");
                }
            }
        }
        softly.assertEquals(waitUntilEquals(page::code, newCode), newCode,
                "After " + how + " the form shows " + page.code() + " instead of the new document " + newCode + ".");
        softly.assertAll();
        return newCode;
    }

    /** "حفظ" opens a confirmation only when the form is valid; otherwise the reason is reported. */
    protected void failIfSaveBlockedBeforeConfirmation() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                Optional<DocumentForm.Toast> toast = form.waitForConfirmationDialogOrToast();
                if (toast.isPresent() && isWarning(toast.get())) {
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
                fillRequiredFieldsBeforeSave();
                page.clickSave();
            }
        }
    }

    protected static boolean isOneOf(String statusLabel, DocumentStatus... allowed) {
        return Arrays.stream(DocumentStatus.values()).anyMatch(s -> s.label().equals(statusLabel))
                && DocumentStatus.fromLabel(statusLabel).isOneOf(allowed);
    }

    protected static boolean isUploadToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.contains("تمت إضافة الملفات") || text.contains("تم الرفع");
    }

    protected static boolean isWarning(DocumentForm.Toast toast) {
        String title = toast.title() == null ? "" : toast.title();
        return title.contains("تنبيه") || title.contains("تحذير");
    }

    /** Values of the date inputs of a new document, for the log. */
    private Map<String, String> dateFieldsOfNewDocument() {
        Map<String, String> dates = new HashMap<>();
        for (String field : List.of("saleOrderDate", "orderDeadLine", "invoiceDate", "quotationDate")) {
            String value = page.textValue(field);
            if (!value.isEmpty()) {
                dates.put(field, value);
            }
        }
        if (dates.values().stream().noneMatch(v -> v.equals(LocalDate.now().toString()))) {
            log.warn("No date field of the new document shows today: {}", dates);
        }
        return dates;
    }

    /** {@code value} as a number ("" → 0). */
    protected static BigDecimal number(String value) {
        String digits = value == null ? "" : value.replaceAll("[^0-9.\\-]", "");
        return digits.isEmpty() ? BigDecimal.ZERO : new BigDecimal(digits);
    }
}
