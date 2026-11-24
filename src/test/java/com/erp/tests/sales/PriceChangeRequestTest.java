package com.erp.tests.sales;

import com.erp.automation.base.SalesDocumentBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.sales.PriceChangeRequestPage;
import com.erp.automation.pages.sales.PricingOperationPage;
import com.erp.automation.pages.sales.SalesPage;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static com.erp.automation.pages.sales.PricingOperationPage.ITEM_CODE_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.PRICE_COLUMN;
import static com.erp.automation.ui.DocumentApi.intOf;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

/**
 * "طلبات تعديل الأسعار" (المبيعات → العمليات → طلبات تعديل الأسعار), level 1.
 *
 * <p>Shared tests come from {@link SalesDocumentBaseTest}; the ones that look
 * documents up by their number in the list are replaced, because this list has
 * no number column. This class adds what only this screen has: every listed
 * request opens with its number and status, create (manual / Excel import),
 * "البيان" kept, edit, delete, the line decisions (موافقة / رفض / سبب الرفض)
 * and the approval, the locked approved request and the toolbars.</p>
 *
 * <p>The form itself (automatic entry, increase / decrease / formula, line
 * calculations, below-cost question, field rules) is the same component as
 * "التسعير" and is tested once, in {@link PricingTest}; so is creating a
 * request from "التسعير" (approval → "إنشاء طلب تعديل الأسعار").</p>
 *
 * <p>Reviewed against QC on 2026-11-23 (see docs/sales/pricing-tests.md).</p>
 */
public class PriceChangeRequestTest extends SalesDocumentBaseTest<PriceChangeRequestPage> {

    private static final String PCR = "price-change-request";

    private static final String DEFAULT_PRICE_LIST = "القائمة الافتراضية";

    @Override
    protected void clickScreenLink(SalesPage salesPage) {
        salesPage.clickPriceChangeRequests();
    }

    @Override
    protected PriceChangeRequestPage createPage() {
        return new PriceChangeRequestPage(driver);
    }

    @Override
    protected String screenPath() {
        return PriceChangeRequestPage.SCREEN_PATH;
    }

    @Override
    protected List<String> listColumns() {
        return List.of("priceOperationDate", "branchName", "changePolicy", "applyOn", "itemGroupsCount", "itemsCount",
                "baseOfComparison", "recordStatus", "typeOfProfitMargin", "profitMarginValue", "profitMarginRate");
    }

    @Override
    protected String codePrefix() {
        return "";
    }

    @Override
    protected String customerField() {
        return null;
    }

    @Override
    protected String quantityColumn() {
        return null;
    }

    @Override
    protected String priceColumn() {
        return PRICE_COLUMN;
    }

    @Override
    protected String codeColumn() {
        return ITEM_CODE_COLUMN;
    }

    @Override
    protected String exportMenuItem() {
        return PriceChangeRequestPage.MENU_EXPORT_EXCEL;
    }

    @Override
    protected String statusGroupLabel() {
        return "الحالة";
    }

    @Override
    protected List<String> requiredMarkedOnEmptySave() {
        return List.of();
    }

    @Override
    protected DocumentStatus[] statusesAfterSave() {
        return new DocumentStatus[]{DRAFT};
    }

    @Override
    protected String notePrefix() {
        return "طلب تعديل أسعار آلي ";
    }

    /**
     * QC 2026-11-23: "البيان" is not kept (request 17). It is checked by
     * {@link #descriptionIsKeptAfterSave} only, so that one bug does not fail
     * every test that saves.
     */
    @Override
    protected boolean verifiesNoteOnSave() {
        return false;
    }

    @Override
    protected Map<String, Object> openDocumentInEditMode() {
        Map<String, Object> record = openDraft();
        page.clickEdit();
        waitForAnyEditMode();
        return record;
    }

    /**
     * The record with API id 0 (code 1) opens as an empty form with status
     * "ملغي", so the API-based choice leaves it out; it is checked on its own by
     * {@link #everyListedRequestOpensWithItsNumberAndStatus()}.
     */
    @Override
    protected Map<String, Object> openRandom(Predicate<Map<String, Object>> filter) {
        return super.openRandom(filter.and(r -> intOf(r, "id") != 0));
    }

    @Override
    protected java.util.Optional<Map<String, Object>> tryOpenRandom(Predicate<Map<String, Object>> filter) {
        return super.tryOpenRandom(filter.and(r -> intOf(r, "id") != 0));
    }

    // =====================================================================
    // Shared tests of SalesDocumentBaseTest, numbered in this page's run order
    // (priority 1, 2, 3 … over ALL the tests of this page). Each one only
    // calls the shared test; the steps are in SalesDocumentBaseTest.
    // =====================================================================

    @Override
    @Test(priority = 1, groups = {PCR, "navigation"},
            description = "المبيعات → العمليات → the screen link opens its list with its columns and \"جديد\"")
    public void listOpensFromSalesMenu() {
        super.listOpensFromSalesMenu();
    }

    @Override
    @Test(priority = 2, groups = {PCR, "navigation"},
            description = "\"جديد\" opens an empty document: a new number, today's date, no status")
    public void newButtonOpensEmptyDocument() {
        super.newButtonOpensEmptyDocument();
    }

    @Override
    @Test(priority = 16, groups = {PCR, "validation"},
            description = "Saving without lines is blocked: \"يجب اضافة صنف واحد على الأقل\", no confirmation, stays in Add")
    public void userCannotSaveEmptyDocument() {
        super.userCannotSaveEmptyDocument();
    }

    @Override
    @Test(priority = 17, groups = {PCR, "search"},
            description = "List search for a text no document has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {
        super.listSearchWithoutMatchShowsNoRows();
    }

    /**
     * Not the shared steps, for two reasons found on QC 2026-11-23:
     * <ul>
     *     <li>the shared steps open the request by its URL (/view/&lt;id&gt;). Opened that way the
     *     form has no list behind it: the record counter shows 1 and the record buttons are
     *     disabled after a move. A user opens a request from the list, where the buttons work
     *     (checked by hand: list row 10 → الأخير 20 → السابق 19, all buttons usable);</li>
     *     <li>they leave the request with "الأول", which opens /view/0 (the bug of list row 1).</li>
     * </ul>
     * So the request is opened from the list and left with "الأخير" / "السابق".
     */
    @Override
    @Test(priority = 18, groups = {PCR, "search"},
            description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenDocumentFromRecordSearch() {

        String target = openListRowOtherThanTheFirst();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageLast") ? "pageLast" : "pageBack");
        String other = waitUntilChanges(target);
        assertTrue(!other.equals(target), "Could not move away from request " + target + ".");
        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open request " + target + ".");
    }

    /**
     * Opened from the list, as a user does (see {@link #userCanOpenDocumentFromRecordSearch} for
     * why not by URL). "الأخير" / "السابق" / "التالي" work (checked by hand on 2026-11-23);
     * "الأول" opens /view/0 with an empty form and status "ملغي" — the bug of list row 1 — so the
     * last step is expected to fail, with a clear message, until that is fixed.
     */
    @Override
    @Test(priority = 19, groups = {PCR, "navigation"},
            description = "Record navigation in the form (opened from the list): last / back / next / first")
    public void userCanNavigateBetweenRecords() {

        List<String> codes = page.api().findAll().stream()
                .filter(r -> !Boolean.TRUE.equals(r.get("isDeleted")))
                .map(r -> String.valueOf(new BigDecimal(String.valueOf(r.get("priceOperationCode"))).intValue()))
                .sorted(java.util.Comparator.comparingInt(Integer::parseInt)).toList();
        String firstCode = codes.get(0);
        String lastCode = codes.get(codes.size() - 1);
        String beforeLastCode = codes.get(codes.size() - 2);

        openListRowOtherThanTheFirst();
        SoftAssert softly = new SoftAssert();

        page.clickRecordNavigation("pageLast");
        softly.assertEquals(waitUntilEquals(page::code, lastCode), lastCode, "'الأخير' did not open the last request.");
        for (String button : List.of("pageLast", "pageNext")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the last request.");
        }
        page.clickRecordNavigation("pageBack");
        softly.assertEquals(waitUntilEquals(page::code, beforeLastCode), beforeLastCode,
                "'السابق' from request " + lastCode + " did not open request " + beforeLastCode + ".");
        page.clickRecordNavigation("pageNext");
        softly.assertEquals(waitUntilEquals(page::code, lastCode), lastCode,
                "'التالي' from request " + beforeLastCode + " did not open request " + lastCode + ".");

        // "الأول": clickAction, not clickRecordNavigation, which waits 30 s for a number and times out.
        String urlBefore = driver.getCurrentUrl();
        page.clickAction("pageFirst");
        waitUntil("another record after 'الأول'", Duration.ofSeconds(15), () -> !driver.getCurrentUrl().equals(urlBefore));
        String first = waitUntilEquals(page::code, firstCode);
        log.info("'الأول' → {} code [{}] status [{}]", driver.getCurrentUrl(), first, page.statusLabel());
        softly.assertEquals(first, firstCode, "'الأول' opened " + driver.getCurrentUrl() + " with number [" + first
                + "] and status [" + page.statusLabel() + "] instead of request " + firstCode + ".");
        for (String button : List.of("pageFirst", "pageBack")) {
            softly.assertFalse(page.isRecordNavigationEnabled(button), "'" + button + "' must be disabled on the first request.");
        }
        softly.assertAll();
    }

    /** Double-clicks a random row of the first list page except row 1 (it opens /view/0); returns the number shown. */
    private String openListRowOtherThanTheFirst() {
        page.goToFirstListPage();
        int rows = page.listRowCount();
        assertTrue(rows >= 2, "The list needs at least 2 requests; it shows " + rows + ".");
        int row = 1 + new java.util.Random().nextInt(rows - 1);
        page.openListRow(row);
        return page.waitForCode();
    }

    @Override
    @Test(priority = 22, groups = {PCR, "list"},
            description = "Group by status: every group has documents and the groups add up to the list total")
    public void userCanGroupListByStatus() {
        super.userCanGroupListByStatus();
    }

    @Override
    @Test(priority = 23, groups = {PCR, "print"},
            description = "Print a random document: the preview shows its number; printed through Chrome's dialog")
    public void userCanPrintDocument() throws java.awt.AWTException {
        super.userCanPrintDocument();
    }

    // Switched off on request (QC 2026-11-23). An override with enabled = false is needed: without
    // it TestNG runs the inherited test of SalesDocumentBaseTest.
    @Override
    @Test(enabled = false, description = "Switched off: save a random document as PDF")
    public void userCanSaveDocumentAsPdf() {
        super.userCanSaveDocumentAsPdf();
    }

    @Override
    @Test(priority = 25, groups = {PCR, "excel"},
            description = "⋮ → export to Excel: an .xlsx with the document's items is downloaded")
    public void userCanExportDocumentToExcel() {
        super.userCanExportDocumentToExcel();
    }

    @Override
    @Test(priority = 26, groups = {PCR, "attachments"},
            description = "Attachments added in edit mode are listed and kept after save and reload")
    public void userCanAddAttachments() {
        super.userCanAddAttachments();
    }

    @Override
    @Test(priority = 27, groups = {PCR, "attachments"},
            description = "Deleting an attachment in edit mode is kept after save and reload")
    public void userCanDeleteAttachment() {
        super.userCanDeleteAttachment();
    }

    @Override
    @Test(priority = 28, groups = {PCR, "exit"},
            description = "Close a new, a viewed and an edited document: each returns to the list")
    public void userCanExitDocument() {
        super.userCanExitDocument();
    }

    @Override
    @Test(priority = 29, groups = {PCR, "log"},
            description = "Transaction log: the first entry is the creation")
    public void userCanViewTransactionLog() {
        super.userCanViewTransactionLog();
    }

    // =====================================================================
    // Shared tests that need a number column in the list.
    //
    // They are @Test methods of SalesDocumentBaseTest and find documents by
    // their number; this list has no number column ("#" is only the row
    // index). The overrides below either test the same thing by row position,
    // or switch the test off (enabled = false) when another test covers it or
    // it cannot be done. Do not delete the switched-off ones: TestNG would run
    // the inherited test and it would fail.
    // =====================================================================

    @Override
    @Test(priority = 20, groups = {PCR, "navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that request with its number and the status of its row")
    public void userCanOpenDocumentFromListRowActions() {

        List<String> items = page.rowActionItems();
        assertTrue(items.contains("عرض"), "The row menu has no \"عرض\": " + items);
        page.goToFirstListPage();
        int rows = page.listRowCount();
        int row = 1 + new java.util.Random().nextInt(rows - 1); // not row 1: it opens /view/0 (see test 3)
        String listStatus = page.listCell(row, "recordStatus");
        page.openListRowFromActions(row);
        String code = waitUntilEquals(() -> page.code().isEmpty() ? "" : "shown", "shown");
        log.info("Row {} ⋯ → عرض → {} code [{}] status [{}]", row + 1, driver.getCurrentUrl(), page.code(), page.statusLabel());

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(code, "shown", "Row " + (row + 1) + " ⋯ → عرض opened " + driver.getCurrentUrl() + " without a number.");
        softly.assertEquals(waitUntilEquals(page::statusLabel, listStatus), listStatus,
                "Row " + (row + 1) + " ⋯ → عرض opened a request with another status than its row.");
        softly.assertAll();
    }

    /**
     * QC 2026-11-23, by hand: one card per request of the list page, in the list order (card 2
     * opened request 2). Every card shows only "الريال السعودي" and "غير معتمد", while the list
     * page shows 9 معتمد and 1 مسودة → the status check is expected to fail until fixed (the same
     * as on "التسعير"). The cards carry no number, so a card is matched to its list row by position.
     */
    @Override
    @Test(priority = 21, groups = {PCR, "list"},
            description = "Kanban: one card per request of the list page with the statuses of the list; a card opens "
                    + "the request of its list row; back to the list")
    public void userCanSwitchListToKanbanAndBack() {

        page.goToFirstListPage();
        int rows = page.listRowCount();
        List<String> listStatuses = new java.util.ArrayList<>();
        for (int row = 0; row < rows; row++) {
            listStatuses.add(page.listCell(row, "recordStatus"));
        }
        SoftAssert softly = new SoftAssert();
        try {
            page.switchToKanbanView();
            List<String> cards = page.kanbanCardTexts();
            List<String> statuses = page.kanbanCardStatuses();
            log.info("List page statuses {} | Kanban cards {}", listStatuses, cards);

            softly.assertEquals(cards.size(), rows, "Kanban shows " + cards.size() + " cards for " + rows + " requests on the list page.");
            softly.assertEquals(statuses, listStatuses, "Statuses on the Kanban cards differ from \"الحالة\" in the list (same order).");

            int index = 1 + new java.util.Random().nextInt(Math.min(cards.size(), rows) - 1); // not card 1: /view/0
            page.openKanbanCardAt(index);
            String code = waitUntilEquals(() -> page.code().isEmpty() ? "" : "shown", "shown");
            log.info("Kanban card {} → {} code [{}] status [{}]", index + 1, driver.getCurrentUrl(), page.code(), page.statusLabel());
            softly.assertEquals(code, "shown", "Kanban card " + (index + 1) + " opened " + driver.getCurrentUrl() + " without a number.");
            softly.assertEquals(waitUntilEquals(page::statusLabel, listStatuses.get(index)), listStatuses.get(index),
                    "Kanban card " + (index + 1) + " opened a request with another status than list row " + (index + 1) + ".");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView();
            }
        }
        softly.assertTrue(page.isListViewShown(), "The list view did not come back after Kanban.");
        softly.assertAll();
    }

    @Override
    @Test(enabled = false, description = "Replaced by everyListedRequestOpensWithItsNumberAndStatus")
    public void userCanOpenDocumentByDoubleClickOnListRow() {
        super.userCanOpenDocumentByDoubleClickOnListRow();
    }

    @Override
    @Test(enabled = false, description = "Not possible: the list has no number column to search by")
    public void userCanSearchListByCode() {
        super.userCanSearchListByCode();
    }

    @Override
    @Test(enabled = false, description = "Replaced by requestListPagerMatchesTheApi")
    public void userCanNavigateListPagination() {
        super.userCanNavigateListPagination();
    }

    // =====================================================================
    // List
    // =====================================================================

    @Test(priority = 3, groups = {PCR, "list"},
            description = "Every request in the list opens (double-click) with a number and the status shown in the list")
    public void everyListedRequestOpensWithItsNumberAndStatus() {

        // QC 2026-11-23: the first row (code 1, معتمد) still opens /view/0 with an EMPTY form
        // and status "ملغي" → expected to fail until it is fixed.
        int rows = page.listRowCount();
        SoftAssert softly = new SoftAssert();
        for (int row = 0; row < rows; row++) {
            String listStatus = page.listCell(row, "recordStatus");
            String date = page.listCell(row, "priceOperationDate");
            page.openListRow(row);
            String code = waitUntilEquals(() -> page.code().isEmpty() ? "" : "shown", "shown");
            String status = waitUntilEquals(page::statusLabel, listStatus);
            log.info("Row {} ({} / {}) → {} code [{}] status [{}]", row + 1, date, listStatus,
                    driver.getCurrentUrl(), page.code(), status);
            softly.assertEquals(code, "shown", "Row " + (row + 1) + " (" + date + ") opened without a number: " + driver.getCurrentUrl());
            softly.assertEquals(status, listStatus, "Row " + (row + 1) + " (" + date + ") shows [" + status
                    + "] but the list says [" + listStatus + "].");
            page.closeForm();
            page.waitForListLoaded();
        }
        softly.assertAll();
    }

    @Test(priority = 4, groups = {PCR, "pagination"},
            description = "The pager total equals the number of requests in the API; the rows match the page range")
    public void requestListPagerMatchesTheApi() {

        page.goToFirstListPage();
        int[] range = page.paginationRange();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(range[2], page.api().count(), "The list total differs from the API.");
        softly.assertEquals(page.visibleRowCount(), range[1] - range[0] + 1, "Rows on the page do not match the summary.");
        softly.assertAll();
    }

    // =====================================================================
    // Create / Edit / Delete
    // =====================================================================

    @Test(priority = 5, groups = {PCR, "create"},
            description = "Manual request (يدوي): header with هامش الربح = قيمة 10 and two item lines with a price; "
                    + "saved as Draft; price list, هامش الربح and lines kept")
    public void userCanCreateManualPriceChangeRequest() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        String priceList = form.getSelectedText("salePriceListId");
        // "هامش الربح" = قيمة 10 (not used before 2026-11-23).
        page.selectOption(PricingOperationPage.PROFIT_MARGIN_TYPE, PricingOperationPage.MARGIN_BY_VALUE);
        assertTrue(page.hasField(PricingOperationPage.MARGIN_VALUE_FIELD, Duration.ofSeconds(3)),
                "\"القيمة\" is not shown for هامش الربح = قيمة.");
        page.setNumberField(PricingOperationPage.MARGIN_VALUE_FIELD, "10");
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        List<String> before = linesSnapshot();
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(form.getSelectedText("salePriceListId"), priceList,
                "\"قوائم الأسعار\" of request " + code + " changed after save and reload.");
        softly.assertEquals(waitForSelected(form, PricingOperationPage.PROFIT_MARGIN_TYPE), PricingOperationPage.MARGIN_BY_VALUE,
                "\"هامش الربح\" of request " + code + " was not kept after reload.");
        softly.assertEquals(number(page.textValue(PricingOperationPage.MARGIN_VALUE_FIELD)).compareTo(BigDecimal.TEN), 0,
                "\"القيمة\" of request " + code + " was not kept as 10 after reload.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertEquals(record == null ? -1 : number(String.valueOf(record.get(PricingOperationPage.MARGIN_VALUE_FIELD)))
                .compareTo(BigDecimal.TEN), 0, "profitMarginValue of request " + code + " is not 10 in the API: " + record);
        softly.assertEquals(linesSnapshot().stream().sorted().toList(), before.stream().sorted().toList(),
                "Lines of request " + code + " changed after save and reload.");
        softly.assertAll();
    }

    @Test(priority = 6, groups = {PCR, "create"},
            description = "Automatic request (تلقائي): header with هامش الربح = قيمة 10, two items chosen in \"الأصناف\" "
                    + "and loaded with \"انزال\"; saved as Draft; price list, هامش الربح and lines kept")
    public void userCanCreateAutomaticPriceChangeRequest() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        String priceList = form.getSelectedText("salePriceListId");
        page.selectOption(PricingOperationPage.PROFIT_MARGIN_TYPE, PricingOperationPage.MARGIN_BY_VALUE);
        assertTrue(page.hasField(PricingOperationPage.MARGIN_VALUE_FIELD, Duration.ofSeconds(3)),
                "\"القيمة\" is not shown for هامش الربح = قيمة.");
        page.setNumberField(PricingOperationPage.MARGIN_VALUE_FIELD, "10");

        // Two items the lines accept (priced in the list, with a value for "أساس المقارنة"), chosen in
        // "الأصناف" by their label "code - name" — the same choice as addItemLine().
        page.setItemEntry(PricingOperationPage.ENTRY_AUTOMATIC);
        List<Map<String, Object>> items = acceptedItems(List.of());
        assertTrue(items.size() >= 2, "Fewer than 2 items are accepted for price list [" + priceList + "]: " + items);
        List<String> codes = items.subList(0, 2).stream().map(i -> String.valueOf(i.get("code"))).toList();
        List<String> labels = items.subList(0, 2).stream().map(i -> i.get("code") + " - " + i.get("name")).toList();
        List<String> chosen = page.chooseItems(labels);
        assertTrue(chosen.containsAll(labels), "\"الأصناف\" shows " + chosen + " instead of " + labels + ".");
        page.loadItems();
        page.deleteEmptyLines();
        assertEquals(page.lineTexts(ITEM_CODE_COLUMN).stream().sorted().toList(), codes.stream().sorted().toList(),
                "\"انزال\" did not load the chosen items " + codes + ".");
        List<String> before = linesSnapshot();
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(form.getSelectedText("salePriceListId"), priceList,
                "\"قوائم الأسعار\" of request " + code + " changed after save and reload.");
        softly.assertEquals(waitForSelected(form, "downTypeId"), PricingOperationPage.ENTRY_AUTOMATIC,
                "\"آلية إدخال الأصناف\" of request " + code + " was not kept as تلقائي after reload.");
        softly.assertEquals(waitForSelected(form, PricingOperationPage.PROFIT_MARGIN_TYPE), PricingOperationPage.MARGIN_BY_VALUE,
                "\"هامش الربح\" of request " + code + " was not kept after reload.");
        softly.assertEquals(number(page.textValue(PricingOperationPage.MARGIN_VALUE_FIELD)).compareTo(BigDecimal.TEN), 0,
                "\"القيمة\" of request " + code + " was not kept as 10 after reload.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertEquals(record == null ? -1 : number(String.valueOf(record.get(PricingOperationPage.MARGIN_VALUE_FIELD)))
                .compareTo(BigDecimal.TEN), 0, "profitMarginValue of request " + code + " is not 10 in the API: " + record);
        softly.assertEquals(linesSnapshot().stream().sorted().toList(), before.stream().sorted().toList(),
                "Lines of request " + code + " changed after save and reload.");
        softly.assertAll();
    }

    @Test(priority = 7, groups = {PCR, "create"},
            description = "\"البيان\" of a new request is kept: shown after save and after reload, and in the API")
    public void descriptionIsKeptAfterSave() {

        // QC 2026-11-23: request 17 was saved with "QC-Claude coverage check" and showed an
        // empty "البيان" after save; the API returned note "" → expected to fail until fixed.
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false); // types a unique "البيان"
        addItemLine();
        String note = page.note();
        String code = saveAndVerify(DRAFT);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.waitForNote(note), note, "\"البيان\" of request " + code + " is not shown after save.");
        page.reload();
        softly.assertEquals(page.waitForNote(note), note, "\"البيان\" of request " + code + " is not shown after reload.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertEquals(record == null ? null : String.valueOf(record.get("note")), note,
                "\"البيان\" of request " + code + " is not saved in the API (note).");
        softly.assertAll();
    }

    @Test(priority = 8, groups = {PCR, "create", "excel"},
            description = "⋮ → \"الاستيراد من Excel\" (Pricing.xlsx: 2 @ 50.5, 15 @ 60.25) loads one line per item "
                    + "with its price; saved as Draft and the prices are kept after reload")
    public void userCanImportRequestLinesFromExcel() {

        // Same file and expectations as PricingTest.userCanImportPricingLinesFromExcel; on this
        // screen the menu shows "الاستيراد من Excel" on a new request (QC 2026-11-23).
        Map<String, BigDecimal> expected = Map.of("2", new BigDecimal("50.5"), "15", new BigDecimal("60.25"));
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false); // طريقة التطبيق = يدوي: the file's prices are kept
        page.importLinesFromExcel(importFile("Pricing.xlsx"));
        DocumentForm.Toast imported = form.waitForToast();
        page.waitForToastToAppearAndClear();
        page.deleteEmptyLines();

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(imported.isSuccess() && imported.fullText().contains("2"),
                "The import did not report 2 imported rows: " + imported);
        softly.assertEquals(pricesByItemCode(), expected, "Lines imported from Pricing.xlsx (item code → price).");
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        softly.assertEquals(pricesByItemCode(), expected, "Imported lines of request " + code + " after save and reload.");
        softly.assertAll();
    }

    @Test(priority = 9, groups = {PCR, "edit"},
            description = "Edit a Draft request: a line price changes and is kept; it stays a Draft")
    public void userCanEditDraftPriceChangeRequest() {

        openDraft();
        page.clickEdit();
        waitForAnyEditMode();
        if (page.lineCount() == 0) {
            addItemLine();
        }
        String item = page.lineText(0, ITEM_CODE_COLUMN);
        String base = form.getSelectedText("baseOfComparison");
        BigDecimal price = page.lineValue(0, PRICE_COLUMN).add(BigDecimal.ONE);
        // One try and a clear message: QC 2026-11-23, by hand on request 12 (أساس المقارنة = التكلفة,
        // item 6, cost 86.313): 90 → 91 + Enter showed 86.31 — the price was replaced by the cost.
        String shown = page.typeLineNumberOnce(0, PRICE_COLUMN, price.toPlainString(), org.openqa.selenium.Keys.ENTER);
        page.stopCellEditing();
        assertEquals(number(shown).compareTo(price), 0, "Edit mode: \"السعر بعد التعديل\" of item " + item + " typed as "
                + price.toPlainString() + " shows [" + shown + "] (أساس المقارنة = " + base + ").");
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        int row = page.lineTexts(ITEM_CODE_COLUMN).indexOf(item);
        assertTrue(row >= 0, "Item " + item + " is not in request " + code + " after save.");
        assertEquals(page.lineValue(row, PRICE_COLUMN).compareTo(price), 0, "The price of " + item + " was not saved.");
    }

    @Test(priority = 10, groups = {PCR, "status"},
            description = "Delete a Draft created by the test: success message; gone from the API")
    public void userCanDeleteDraftPriceChangeRequest() {

        createDocument(); // the test's own Draft: QC data is not deleted
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft request " + code + ".");
        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting request " + code + " failed: " + toast);
        Map<String, Object> record = page.recordByCode(code);
        assertTrue(record == null || Boolean.TRUE.equals(record.get("isDeleted")), "Request " + code + " is still in the API.");
    }

    // =====================================================================
    // Approval
    // =====================================================================

    @Test(priority = 11, groups = {PCR, "status", "validation"},
            description = "Approving a request whose lines have no موافقة / رفض decision is refused with a message; "
                    + "it stays a Draft")
    public void approvingWithoutLineDecisionsIsRefused() {

        // QC 2026-11-23, request 17: "لا يمكن الاعتماد، توجد أصناف لم يتم تحديد موافقة أو رفض لها".
        createDocument(); // a new request: no line has a decision yet
        String code = page.waitForCode();
        page.clickApproveOrDisapprove();
        page.confirmDialog(); // "هل تريد الاعتماد ؟" → موافق
        DocumentForm.Toast toast = form.waitForToast();
        log.info("Approving request {} without line decisions → [{}]", code, toast);
        page.reload();
        page.waitForStatusLabel(DRAFT.label());

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast.isError() && toast.fullText().contains(PriceChangeRequestPage.LINE_DECISION_REQUIRED),
                "Expected the error about undecided lines, got: " + toast);
        Map<String, Object> record = page.recordByCode(code);
        softly.assertTrue(record != null && PricingOperationPage.isDraft(record),
                "Request " + code + " must stay a Draft in the API: " + record);
        softly.assertAll();
    }

    @Test(priority = 12, groups = {PCR, "status"},
            description = "In edit mode each line gets موافقة or رفض (with سبب الرفض); one box "
                    + "disables the other; the decisions are saved; a request with an undecided line is refused; "
                    + "once every line is decided it is approved and locked")
    public void userCanApproveRequestAfterDecidingEveryLine() {

        // QC 2026-11-23, by hand: request 19 (تلقائي: رفض 6 with a reason, موافقة 2) and request 17
        // (يدوي: رفض 2) were decided in edit mode, saved and approved → معتمد. The columns are
        // the last ones of the grid and appear only when the grid is scrolled to them.
        // A manual request (the usual case) with two lines; its prices are in a test price list.
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        assertTrue(page.lineCount() >= 2, "Two item lines were not added.");
        String code = saveAndVerify(DRAFT);
        page.clickEdit();
        waitForAnyEditMode();
        page.waitForGridToSettle();
        String rejectedItem = page.lineText(0, ITEM_CODE_COLUMN);
        String approvedItem = page.lineText(1, ITEM_CODE_COLUMN);
        String reason = "سبب رفض آلي " + System.currentTimeMillis();

        SoftAssert softly = new SoftAssert();
        for (String column : List.of(PriceChangeRequestPage.LINE_APPROVAL_COLUMN, PriceChangeRequestPage.LINE_REJECT_COLUMN,
                PriceChangeRequestPage.LINE_REJECT_REASON_COLUMN)) {
            softly.assertTrue(page.revealLineColumn(0, column), "Edit mode of request " + code + " has no line column [" + column + "].");
        }
        softly.assertAll();

        // 1) Decide line 1 only: رفض with a reason; "موافقة" of that line is then disabled.
        page.rejectLine(0, reason);
        softly.assertFalse(page.isLineCheckEnabled(0, PriceChangeRequestPage.LINE_APPROVAL_COLUMN),
                "\"موافقة\" must be disabled on a line marked \"رفض\" (item " + rejectedItem + ").");
        saveAndVerify(DRAFT);
        page.clickApproveOrDisapprove();
        page.confirmDialog();
        DocumentForm.Toast refused = form.waitForToast();
        softly.assertTrue(refused.isError() && refused.fullText().contains(PriceChangeRequestPage.LINE_DECISION_REQUIRED),
                "Approving with item " + approvedItem + " undecided must be refused, got: " + refused);
        page.reload();
        page.waitForStatusLabel(DRAFT.label());

        // 2) Decide line 2: موافقة; "رفض" of that line is then disabled.
        page.clickEdit();
        waitForAnyEditMode();
        page.waitForGridToSettle();
        page.approveLine(1);
        softly.assertFalse(page.isLineCheckEnabled(1, PriceChangeRequestPage.LINE_REJECT_COLUMN),
                "\"رفض\" must be disabled on a line marked \"موافقة\" (item " + approvedItem + ").");
        saveAndVerify(DRAFT);

        Map<String, Map<String, Object>> saved = apiLinesByItem();
        softly.assertEquals(saved.getOrDefault(rejectedItem, Map.of()).get("isRejected"), Boolean.TRUE, "Item " + rejectedItem + " is not saved as رفض.");
        softly.assertEquals(String.valueOf(saved.getOrDefault(rejectedItem, Map.of()).get("rejectReason")), reason,
                "\"سبب الرفض\" of item " + rejectedItem + " is not saved.");
        softly.assertEquals(saved.getOrDefault(approvedItem, Map.of()).get("isApproved"), Boolean.TRUE, "Item " + approvedItem + " is not saved as موافقة.");

        // 3) Every line decided → the approval goes through.
        page.approve(PricingOperationPage.APPROVE_CONTINUE);
        page.waitForStatusLabel(APPROVED.label());
        page.reload();
        page.waitForStatusLabel(APPROVED.label());
        Map<String, Object> record = page.recordByCode(code);
        softly.assertTrue(record != null && PricingOperationPage.isApproved(record),
                "Request " + code + " is not Approved (recordStatus 1) in the API: " + record);
        for (String action : List.of("edit", "delete", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on approved request " + code + ".");
        }
        softly.assertAll();
    }

    @Test(priority = 13, groups = {PCR, "status", "validation"},
            description = "An approved request is locked (edit/delete/approve disabled) and every line has a decision: "
                    + "موافقة or رفض")
    public void approvedRequestIsLockedAndEveryLineIsDecided() {

        // Read-only: an existing approved request, so older data is checked too
        // (approving one is covered by userCanApproveRequestAfterDecidingEveryLine).
        openRandom(PricingOperationPage::isApproved);
        String code = page.waitForCode();
        page.waitForStatusLabel(APPROVED.label());
        page.waitForActionEnabled("add");
        List<Map<String, Object>> lines = page.apiLinesOfOpenRequest();
        log.info("Approved request {}: lines {}", code, lines.stream()
                .map(l -> l.get("itemCode") + " approved=" + l.get("isApproved") + " rejected=" + l.get("isRejected")).toList());

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on approved request " + code + ".");
        }
        softly.assertFalse(lines.isEmpty(), "Approved request " + code + " has no lines.");
        for (Map<String, Object> line : lines) {
            boolean approved = Boolean.TRUE.equals(line.get("isApproved"));
            boolean rejected = Boolean.TRUE.equals(line.get("isRejected"));
            softly.assertTrue(approved ^ rejected, "Line " + line.get("itemCode") + " of approved request " + code
                    + " must be either موافقة or رفض (isApproved=" + approved + ", isRejected=" + rejected + ").");
        }
        softly.assertAll();
    }

    // =====================================================================
    // Toolbar
    // =====================================================================

    @Test(priority = 14, groups = {PCR, "validation"},
            description = "New request toolbar: save waits for a change; add/undo/delete/print/approve/log disabled")
    public void newPriceChangeRequestEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("xIcon");
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.isActionEnabled("save"), "'save' should wait for a change.");
        for (String action : List.of("add", "undo", "delete", "print", "approveOrDisapprove", "transactionLogs")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new request.");
        }
        softly.assertTrue(page.isInputEditable("branches"), "\"الفرع\" must be editable on a new request.");
        softly.assertTrue(page.isAddLineShown(), "\"اضافة سطر\" is not shown on a new request.");
        softly.assertTrue(page.menuItems().contains(PriceChangeRequestPage.MENU_IMPORT_EXCEL),
                "⋮ has no \"" + PriceChangeRequestPage.MENU_IMPORT_EXCEL + "\" on a new request.");
        softly.assertAll();
        //page.closeForm();
    }

    @Test(priority = 15, groups = {PCR, "validation", "status"},
            description = "Draft request toolbar: add/edit/delete/approve/print/attachments/log enabled; save disabled")
    public void draftPriceChangeRequestEnablesEditActions() {

        openDraft();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("add", "edit", "delete", "approveOrDisapprove", "print", "uploadAttachment",
                "transactionLogs")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft request " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("save"), "'save' must be disabled in view mode.");
        softly.assertAll();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /** Header: الفرع → قوائم الأسعار (not the default one) → البيان → يدوي → أساس المقارنة → سياسة التعديل → يدوي. */
    @Override
    protected void fillHeader(boolean editing) {
        selectIfEditable("branches", editing);
        if (!page.isSelectDisabled("salePriceListId")) {
            // Only a list that prices items (read through the API; on 2026-11-23 only "مجموعه مصر"
            // besides the default one): the lines refuse items not priced in the chosen list.
            List<String> usable = page.priceListNames().stream()
                    .filter(l -> !l.equals(DEFAULT_PRICE_LIST) && page.itemsPricedIn(l).size() >= PRICED_ITEMS_NEEDED).toList();
            if (usable.isEmpty()) {
                throw new org.testng.SkipException("No price list other than [" + DEFAULT_PRICE_LIST + "] prices "
                        + PRICED_ITEMS_NEEDED + " items.");
            }
            page.selectOption("salePriceListId", usable.get(new java.util.Random().nextInt(usable.size())));
            page.confirmDialogIfShown(Duration.ofMillis(800));
        }
        String note = uniqueNote();
        page.setNote(note);
        assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
        if (!page.isSelectDisabled("downTypeId")) {
            page.setItemEntry(PricingOperationPage.ENTRY_MANUAL);
        }
        selectIfEditable("baseOfComparison", editing);
        useBaseWithEnoughItems();
        selectIfEditable("changePolicy", editing);
        if (!page.isSelectDisabled("applicationMethod")) {
            page.setApplicationMethod(PricingOperationPage.METHOD_MANUAL);
        }
    }

    /** Items a test puts on the lines of one request (2 lines + 1 spare). */
    private static final int PRICED_ITEMS_NEEDED = 3;

    /**
     * The lines need items priced in the chosen list that also have a value for "أساس المقارنة"
     * (e.g. a purchase price for "آخر سعر شراء"). When fewer than 2 such items exist, "أساس
     * المقارنة" becomes "قائمة الأسعار المحددة", which every item priced in the list satisfies.
     */
    private void useBaseWithEnoughItems() {
        if (page.isSelectDisabled("baseOfComparison")) {
            return;
        }
        String base = form.getSelectedText("baseOfComparison");
        String priceList = form.getSelectedText("salePriceListId");
        List<String> pricedInList = page.itemsPricedIn(priceList).stream().map(i -> String.valueOf(i.get("code"))).toList();
        long usable = page.itemsAcceptedFor(base, priceList).stream()
                .filter(i -> pricedInList.contains(String.valueOf(i.get("code")))).count();
        if (usable < 2) {
            log.info("\"{}\" has {} item(s) priced in [{}] → أساس المقارنة = {}.", base, usable, priceList,
                    PriceChangeRequestPage.BASE_SELECTED_PRICE_LIST);
            page.selectOption("baseOfComparison", PriceChangeRequestPage.BASE_SELECTED_PRICE_LIST);
        }
    }

    /**
     * Items the lines accept, in random order, without {@code exclude} (codes): priced in the chosen
     * list (the lines refuse other items: QC runs 2026-11-23) and with a value for "أساس المقارنة".
     */
    private List<Map<String, Object>> acceptedItems(List<String> exclude) {
        String base = form.getSelectedText("baseOfComparison");
        String priceList = form.getSelectedText("salePriceListId");
        List<String> pricedInList = page.itemsPricedIn(priceList).stream().map(i -> String.valueOf(i.get("code"))).toList();
        List<Map<String, Object>> items = new java.util.ArrayList<>(page.itemsAcceptedFor(base, priceList).stream()
                .filter(i -> pricedInList.contains(String.valueOf(i.get("code"))))
                .filter(i -> !exclude.contains(String.valueOf(i.get("code")))).toList());
        java.util.Collections.shuffle(items);
        return items;
    }

    /**
     * One item line, with an item the chosen "أساس المقارنة" accepts (read through the API, see
     * PriceChangeRequestPage#itemsAcceptedFor) and not already on a line. A line that refuses its item
     * ("هذا الصنف غير مسعر") is removed by the app; another item is then tried on a new line.
     */
    @Override
    protected String addItemLine() {
        String base = form.getSelectedText("baseOfComparison");
        String priceList = form.getSelectedText("salePriceListId");
        // lineTexts waits for a line to exist: on the first line of a new request the grid is empty.
        List<String> onLines = page.lineCount() == 0 ? List.of() : page.lineTexts(ITEM_CODE_COLUMN);
        List<Map<String, Object>> free = acceptedItems(onLines);
        assertTrue(!free.isEmpty(), "No item left that \"" + base + "\" accepts (price list [" + priceList + "]).");
        for (Map<String, Object> item : free.subList(0, Math.min(8, free.size()))) {
            page.deleteEmptyLines();
            int rowsBefore = page.lineCount();
            int row = page.addLine();
            log.info("Line {}: trying item {} ({}) for \"{}\" / [{}]", row + 1, item.get("name"), item.get("code"), base, priceList);
            try {
                // One word of the name: the item search drops the spaces of what is typed.
                String name = String.valueOf(item.get("name"));
                String word = java.util.Arrays.stream(name.split("\\s+")).max(java.util.Comparator.comparingInt(String::length)).orElse(name);
                page.selectItem(row, word, name);
            } catch (org.openqa.selenium.WebDriverException notOffered) {
                // QC run 2026-11-23 15:32: the typed name offered no option within 10 s.
                log.warn("Item {} ({}) was not offered in the item cell of line {} → another item.",
                        item.get("name"), item.get("code"), row + 1);
                new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
                page.stopCellEditing();
                continue;
            }
            page.waitForGridToSettle();
            if (page.lineCount() <= rowsBefore || page.lineText(row, "uomId").isEmpty()) {
                log.warn("Line {} refused item {} ({}) with \"{}\" / [{}] → another item.",
                        row + 1, item.get("name"), item.get("code"), base, priceList);
                continue;
            }
            fillNewLine(row);
            page.stopCellEditing();
            log.info("Line {}: {} ({}) — \"{}\" / [{}]", row + 1, item.get("name"), item.get("code"), base, priceList);
            return String.valueOf(item.get("code"));
        }
        fail("Lines refused 8 items that \"" + base + "\" should accept (price list [" + priceList + "]).");
        return "";
    }

    /** A Draft request (not id 0); created when QC has none. */
    private Map<String, Object> openDraft() {
        java.util.Optional<Map<String, Object>> opened = tryOpenRandom(PricingOperationPage::isDraft);
        if (opened.isPresent()) {
            page.waitForStatusLabel(DRAFT.label());
            return opened.get();
        }
        log.info("No Draft price change request in QC → creating one.");
        return createDocument();
    }

    /** API lines of the open request by item code. */
    private Map<String, Map<String, Object>> apiLinesByItem() {
        Map<String, Map<String, Object>> byItem = new java.util.HashMap<>();
        for (Map<String, Object> line : page.apiLinesOfOpenRequest()) {
            byItem.put(String.valueOf(line.get("itemCode")), line);
        }
        return byItem;
    }

    /** test-data/import-excel: row 1 instructions, row 2 headers (رمز الصنف, السعر بعد التعديل), then lines. */
    private static Path importFile(String name) {
        return Path.of(System.getProperty("user.dir"), "test-data", "import-excel", name);
    }

    /** "رمز الصنف" → "السعر بعد التعديل" of every line with an item. */
    private Map<String, BigDecimal> pricesByItemCode() {
        Map<String, BigDecimal> prices = new java.util.TreeMap<>();
        for (int row = 0; row < page.lineCount(); row++) {
            String item = page.lineText(row, ITEM_CODE_COLUMN);
            if (!item.isEmpty()) {
                prices.put(item, page.lineValue(row, PRICE_COLUMN).stripTrailingZeros());
            }
        }
        return prices;
    }
}
