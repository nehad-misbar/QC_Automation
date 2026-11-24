package com.erp.tests.sales;

import com.erp.automation.base.SalesDocumentBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.sales.PriceChangeRequestPage;
import com.erp.automation.pages.sales.PricingOperationPage;
import com.erp.automation.pages.sales.PricingPage;
import com.erp.automation.pages.sales.SalesPage;
import com.erp.automation.ui.DocumentApi;
import org.testng.SkipException;
import org.testng.annotations.Test;
import org.testng.asserts.SoftAssert;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.List;
import java.util.Map;

import static com.erp.automation.data.DocumentStatus.APPROVED;
import static com.erp.automation.data.DocumentStatus.DRAFT;
import static com.erp.automation.pages.sales.PricingOperationPage.COST_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.DIFFERENCE_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.ITEM_CODE_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.MARGIN_RATIO_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.MARGIN_VALUE_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.PRICE_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.PRICE_WITH_TAX_COLUMN;
import static com.erp.automation.pages.sales.PricingOperationPage.TAX_COLUMN;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * "التسعير" (المبيعات → العمليات → التسعير), level 1.
 *
 * <p>Shared tests come from {@link SalesDocumentBaseTest}; only the Kanban test
 * is replaced here (the cards show no operation number). This class adds:
 * manual and automatic item
 * entry, the four application methods and their calculations, the line
 * calculations (difference, price with tax, profit margin), the "price below
 * cost" warning, edit / delete a line / undo / delete / approve (with the "already priced"
 * choice that creates a price change request), toolbars and option lists.</p>
 *
 * <p><b>The default price list is never changed</b>: the tests choose
 * another price list, so the prices other tests rely on stay as they are.
 * Edit, delete and approve work on a Draft the test creates itself, so the
 * QC drafts are neither changed nor deleted.</p>
 */
public class PricingTest extends SalesDocumentBaseTest<PricingPage> {

    private static final String PR = "pricing";

    /** Never repriced by the tests (other screens read their prices from it). */
    private static final String DEFAULT_PRICE_LIST = "القائمة الافتراضية";

    /** Rate used by the increase / decrease tests. */
    private static final BigDecimal RATE = BigDecimal.TEN;

    /** Items loaded by the automatic-entry tests. */
    private static final int ITEMS_TO_LOAD = 2;

    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    @Override
    protected void clickScreenLink(SalesPage salesPage) {
        salesPage.clickPricing();
    }

    @Override
    protected PricingPage createPage() {
        return new PricingPage(driver);
    }

    @Override
    protected String screenPath() {
        return PricingPage.SCREEN_PATH;
    }

    @Override
    protected List<String> listColumns() {
        return List.of("priceOperationCode", "priceOperationDate", "typeOfValidity", "changePolicy", "applyOn",
                "baseOfComparison", "typeOfProfitMargin", "profitMarginValue", "profitMarginRate", "applicationMethod");
    }

    @Override
    protected String codePrefix() {
        return ""; // plain numbers (64, 65 …)
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
        return PricingPage.MENU_EXPORT_EXCEL;
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
        return "تسعير آلي ";
    }

    /** "البيان" is checked by {@link #descriptionIsKeptAfterSave} only. */
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

    // =====================================================================
    // Shared tests of SalesDocumentBaseTest, numbered in this page's run order
    // (priority 1, 2, 3 … over ALL the tests of this page). Each one only
    // calls the shared test; the steps are in SalesDocumentBaseTest.
    // =====================================================================

    @Override
    @Test(priority = 1, groups = {PR, "navigation"},
            description = "المبيعات → العمليات → the screen link opens its list with its columns and \"جديد\"")
    public void listOpensFromSalesMenu() {
        super.listOpensFromSalesMenu();
    }

    @Override
    @Test(priority = 2, groups = {PR, "navigation"},
            description = "\"جديد\" opens an empty document: a new number, today's date, no customer, no status")
    public void newButtonOpensEmptyDocument() {
        super.newButtonOpensEmptyDocument();
    }

    @Override
    @Test(priority = 3, groups = {PR, "navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that document")
    public void userCanOpenDocumentFromListRowActions() {
        super.userCanOpenDocumentFromListRowActions();
    }

    @Override
    @Test(priority = 36, groups = {PR, "search"},
            description = "List search by number shows that document")
    public void userCanSearchListByCode() {
        super.userCanSearchListByCode();
    }

    @Override
    @Test(priority = 37, groups = {PR, "search"},
            description = "List search for a text no document has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {
        super.listSearchWithoutMatchShowsNoRows();
    }

    @Override
    @Test(priority = 38, groups = {PR, "search"},
            description = "Record search (magnifier) in the form opens the document typed")
    public void userCanOpenDocumentFromRecordSearch() {
        super.userCanOpenDocumentFromRecordSearch();
    }

    @Override
    @Test(priority = 39, groups = {PR, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenRecords() {
        super.userCanNavigateBetweenRecords();
    }

    @Override
    @Test(priority = 40, groups = {PR, "pagination"},
            description = "List pager: first/last page, Next page by page to the end, back to a random page")
    public void userCanNavigateListPagination() {
        super.userCanNavigateListPagination();
    }

    @Override
    @Test(priority = 43, groups = {PR, "print"},
            description = "Print a random document: the preview shows its number; printed through Chrome's dialog")
    public void userCanPrintDocument() throws java.awt.AWTException {
        super.userCanPrintDocument();
    }

    /*
    @Override
    @Test(priority = 44, groups = {PR, "print"},
            description = "Save a random document as PDF: a PDF file is downloaded")
    public void userCanSaveDocumentAsPdf() {
        super.userCanSaveDocumentAsPdf();
    }*/

    @Override
    @Test(priority = 45, groups = {PR, "excel"},
            description = "⋮ → export to Excel: an .xlsx with the document's items is downloaded")
    public void userCanExportDocumentToExcel() {
        super.userCanExportDocumentToExcel();
    }

    @Override
    @Test(priority = 46, groups = {PR, "attachments"},
            description = "Attachments added in edit mode are listed and kept after save and reload")
    public void userCanAddAttachments() {
        super.userCanAddAttachments();
    }

    @Override
    @Test(priority = 47, groups = {PR, "attachments"},
            description = "Deleting an attachment in edit mode is kept after save and reload")
    public void userCanDeleteAttachment() {
        super.userCanDeleteAttachment();
    }

    @Override
    @Test(priority = 48, groups = {PR, "exit"},
            description = "Close a new, a viewed and an edited document: each returns to the list")
    public void userCanExitDocument() {
        super.userCanExitDocument();
    }

    @Override
    @Test(priority = 49, groups = {PR, "log"},
            description = "Transaction log: the first entry is the creation")
    public void userCanViewTransactionLog() {
        super.userCanViewTransactionLog();
    }

    // =====================================================================
    // Shared tests on the list. "الحالة" (recordStatus) is a list column, far
    // to the side: the shared steps scroll to it. The Kanban cards show no
    // number (QC 2026-11-19), so the Kanban test is this screen's own.
    // =====================================================================

    @Override
    @Test(priority = 4, groups = {PR, "navigation", "list"},
            description = "Double-click on a list row opens that operation with the status shown in the list")
    public void userCanOpenDocumentByDoubleClickOnListRow() {
        super.userCanOpenDocumentByDoubleClickOnListRow();
    }

    @Override
    @Test(priority = 41, groups = {PR, "list"},
            description = "Group by \"الحالة\": every group has operations of that status only and the groups add up "
                    + "to the list total")
    public void userCanGroupListByStatus() {
        super.userCanGroupListByStatus();
    }

    @Override
    @Test(priority = 42, groups = {PR, "list"},
            description = "Kanban: one card per operation of the list page, with the statuses of the list and the "
                    + "operations' numbers; a card opens an operation of that page with the card's status")
    public void userCanSwitchListToKanbanAndBack() {

        page.waitForListLoaded();
        Map<String, String> listStatuses = page.statusesOnCurrentPage(); // code → "الحالة" in the list
        List<String> codes = new java.util.ArrayList<>(listStatuses.keySet());
        List<String> expectedStatuses = listStatuses.values().stream().sorted().toList();
        SoftAssert softly = new SoftAssert();
        try {
            page.switchToKanbanView();
            List<String> cards = page.kanbanCardTexts();
            List<String> statuses = page.kanbanCardStatuses();
            log.info("List page {} | Kanban cards {}", listStatuses, cards);

            softly.assertEquals(cards.size(), codes.size(), "Kanban shows " + cards.size() + " cards for "
                    + codes.size() + " operations on the list page.");
            // QC 2026-11-19: every card showed "غير معتمد" while the list showed معتمد.
            softly.assertEquals(statuses.stream().sorted().toList(), expectedStatuses,
                    "Statuses on the Kanban cards differ from \"الحالة\" in the list.");
            // QC 2026-11-19: the cards show only the currency: an operation cannot be told from another.
            for (String card : cards) {
                softly.assertTrue(codes.stream().anyMatch(code -> card.matches("(?s).*\\b" + code + "\\b.*")),
                        "Kanban card [" + card.replace('\n', ' ') + "] shows no operation number.");
            }
            int index = new java.util.Random().nextInt(cards.size());
            page.openKanbanCardAt(index);
            String opened = page.waitForCode();
            softly.assertTrue(codes.contains(opened), "Card " + (index + 1) + " opened " + opened
                    + ", not an operation of the list page " + codes + ".");
            softly.assertEquals(page.statusLabel(), statuses.get(index),
                    "Operation " + opened + " shows another status than its card.");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView();
            }
        }
        softly.assertAll();
    }

    // =====================================================================
    // Create
    // =====================================================================

    @Test(priority = 5, groups = {PR, "create"},
            description = "Manual entry (يدوي): random header, two item lines with a random price; saved as Draft; "
                    + "every header field and line cell is kept after reload")
    public void userCanCreateManualPricing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        Map<String, String> before = formSnapshot();
        Map<String, String> codesBefore = itemCodesByName();
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        assertKeptAfterReload(softly, before, code);
        // QC 2026-11-19 (confirmed by hand): before save "رمز الصنف" of a manual line
        // shows the BARCODE (3558898130004); after save it shows the item code (16).
        softly.assertEquals(codesBefore, itemCodesByName(),
                "\"رمز الصنف\" of the manual lines before save (left) differs from the saved item codes (right).");
        softly.assertAll();
    }

    @Test(priority = 6, groups = {PR, "create"},
            description = "Automatic entry (تلقائي): items chosen in \"الأصناف\" are loaded with \"انزال\"; saved as Draft; "
                    + "every header field and line cell is kept after reload")
    public void userCanCreateAutomaticPricing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        List<String> items = loadRandomItems();
        if (page.isAllUnits()) { // every unit of an item may get its own line
            assertTrue(page.lineCount() >= items.size(), "\"انزال\" loaded fewer lines than items: " + items);
        } else {
            assertEquals(page.lineCount(), items.size(), "\"انزال\" did not load one line per chosen item: " + items);
        }
        for (String item : items) {
            String code = item.split(" - ")[0].trim();
            assertTrue(lineCodes().contains(code), "Item " + item + " was not loaded: " + lineCodes());
        }
        Map<String, String> before = formSnapshot();
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        SoftAssert softly = new SoftAssert();
        assertKeptAfterReload(softly, before, code);
        softly.assertAll();
    }

    @Test(priority = 7, groups = {PR, "create"},
            description = "\"البيان\" of a new operation is kept: shown after save and after reload, and in the API")
    public void descriptionIsKeptAfterSave() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false); // types a unique "البيان"
        addItemLine();
        String note = page.note();
        String code = saveAndVerify(DRAFT);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.waitForNote(note), note, "\"البيان\" of " + code + " is not shown after save.");
        page.reload();
        softly.assertEquals(page.waitForNote(note), note, "\"البيان\" of " + code + " is not shown after reload.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertEquals(record == null ? null : String.valueOf(record.get("note")), note,
                "\"البيان\" of " + code + " is not saved in the API (note).");
        softly.assertAll();
    }

    /** test-data/import-excel: row 1 instructions, row 2 headers (رمز الصنف, السعر بعد التعديل), then lines. */
    private static java.nio.file.Path importFile(String name) {
        return java.nio.file.Path.of(System.getProperty("user.dir"), "test-data", "import-excel", name);
    }

    @Test(priority = 8, groups = {PR, "create", "excel"},
            description = "⋮ → \"الاستيراد من Excel\" (Pricing.xlsx: 2 @ 50.5, 15 @ 60.25) loads one line per item with "
                    + "its price; saved as Draft and the prices are kept after reload")
    public void userCanImportPricingLinesFromExcel() {
        // QC 2026-11-22, by hand: the file (row 2 = "رمز الصنف" / "السعر بعد التعديل") loaded
        // صنف ساره2 @ 50.5 and ميني جاتوه لوتس @ 60.25 with their default unit.
        Map<String, BigDecimal> expected = Map.of("2", new BigDecimal("50.5"), "15", new BigDecimal("60.25"));
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        useManualMethod(); // the file's prices, not a method's
        page.importLinesFromExcel(importFile("Pricing.xlsx"));
        // The import toast ("تم استيراد 2 صف بنجاح") is read, then waited out: left on
        // screen it was taken for the save toast (QC 2026-11-22).
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
        softly.assertEquals(pricesByItemCode(), expected, "Imported lines of " + code + " after save and reload.");
        softly.assertAll();
    }

    @Test(priority = 9, groups = {PR, "validation", "excel"},
            description = "Excel import with an unknown item code (NOPE-999) loads the known item only (5 @ 12.5) and "
                    + "warns with the unknown code")
    public void excelImportReportsUnknownItemCodes() {
        // QC 2026-11-22, by hand: Pricing-UnknownCode.xlsx loaded only صنف ساره 5 @ 12.5; the app
        // then shows a warning toast listing the codes it did not find (ImportedWithUnmatched).
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        useManualMethod();
        page.importLinesFromExcel(importFile("Pricing-UnknownCode.xlsx"));
        DocumentForm.Toast toast;
        try {
            toast = form.waitForToast();
        } catch (org.openqa.selenium.TimeoutException none) {
            toast = null;
        }
        page.deleteEmptyLines();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(pricesByItemCode(), Map.of("5", new BigDecimal("12.5")),
                "Only the known item must be imported (item code → price).");
        softly.assertTrue(toast != null && toast.fullText().contains("NOPE-999"),
                "No warning names the unknown item code NOPE-999: " + toast);
        softly.assertAll();
        page.closeForm();
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

    @Test(priority = 10, groups = {PR, "calculation"},
            description = "طريقة التطبيق = زيادة 10%: every \"السعر بعد التعديل\" becomes base × 1.10 and is kept after save")
    public void increaseRaisesEveryPriceByTheRate() {
        verifyRateMethod(PricingOperationPage.METHOD_INCREASE, "extraRate", BigDecimal.ONE.add(RATE.movePointLeft(2)));
    }

    @Test(priority = 11, groups = {PR, "calculation"},
            description = "طريقة التطبيق = تخفيض 10%: every \"السعر بعد التعديل\" becomes base × 0.90 and is kept after save")
    public void decreaseLowersEveryPriceByTheRate() {
        verifyRateMethod(PricingOperationPage.METHOD_DISCOUNT, "discountRate", BigDecimal.ONE.subtract(RATE.movePointLeft(2)));
    }

    @Test(priority = 12, groups = {PR, "calculation"},
            description = "طريقة التطبيق = معادلة (خصم 10%، مبلغ إضافي 5): السعر بعد التعديل = قبل × 0.90 + 5, التعديل = بعد − قبل; "
                    + "kept after save")
    public void formulaChangesThePrices() {
        verifyMethod(PricingOperationPage.METHOD_FORMULA, () -> {
            page.setNumberField("formulaDiscountRate", "10");
            page.setNumberField("formulaExtraValue", "5");
        }, before -> before.multiply(new BigDecimal("0.90")).add(new BigDecimal("5")), "قبل × 0.90 + 5");
    }

    @Test(priority = 13, groups = {PR, "calculation"},
            description = "Line calculations: الفارق = السعر − التكلفة; السعر شامل الضريبة = السعر × (1 + الضريبة); "
                    + "هامش الربح قيمة = الفارق; نسبة هامش الربح = الفارق ÷ التكلفة × 100; "
                    + "السعر قبل التعديل + التعديل = السعر بعد التعديل")
    public void lineCalculationsAreConsistent() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        SoftAssert softly = new SoftAssert();
        for (int row = 0; row < page.lineCount(); row++) {
            String item = page.lineText(row, ITEM_CODE_COLUMN);
            BigDecimal price = page.lineValue(row, PRICE_COLUMN);
            BigDecimal cost = page.lineValue(row, COST_COLUMN);
            BigDecimal difference = page.lineValue(row, DIFFERENCE_COLUMN);
            BigDecimal tax = page.lineValue(row, TAX_COLUMN);
            BigDecimal withTax = page.lineValue(row, PRICE_WITH_TAX_COLUMN);
            BigDecimal marginValue = page.lineValue(row, MARGIN_VALUE_COLUMN);
            BigDecimal marginRatio = page.lineValue(row, MARGIN_RATIO_COLUMN);
            log.info("Item {}: price {}, cost {}, difference {}, tax {}%, with tax {}, margin {} / {}%",
                    item, price, cost, difference, tax, withTax, marginValue, marginRatio);

            softly.assertTrue(close(difference, price.subtract(cost)), "الفارق of " + item + " is not price − cost.");
            softly.assertTrue(close(withTax, price.multiply(BigDecimal.ONE.add(tax.movePointLeft(2)))),
                    "السعر شامل الضريبة of " + item + " is not price × (1 + " + tax + "%).");
            softly.assertTrue(close(marginValue, difference), "هامش الربح قيمة of " + item + " is not the difference.");
            if (cost.signum() > 0) {
                BigDecimal ratio = difference.multiply(BigDecimal.valueOf(100)).divide(cost, 6, RoundingMode.HALF_UP);
                softly.assertTrue(close(marginRatio, ratio), "نسبة هامش الربح of " + item + " is " + marginRatio
                        + ", expected difference ÷ cost × 100 = " + ratio + ".");
            }
        }
        // السعر قبل التعديل (priceBefore2) + التعديل (change) = السعر بعد التعديل, when a
        // method other than يدوي changed the prices and both columns are shown.
        // QC 2026-11-22: "السعر قبل التعديل" showed 0 while التعديل and بعد were filled.
        String method = form.getSelectedText("applicationMethod").trim();
        if (!PricingOperationPage.METHOD_MANUAL.equals(method)) {
            for (Map<String, String> cells : page.readListRows(List.of("itemId", "priceBefore2", "change", PRICE_COLUMN)).values()) {
                if (!cells.containsKey("priceBefore2") || !cells.containsKey("change")) {
                    log.warn("\"السعر قبل التعديل\" / \"التعديل\" are hidden in the grid → not compared.");
                    break;
                }
                BigDecimal before = number(cells.get("priceBefore2"));
                BigDecimal change = number(cells.get("change"));
                BigDecimal after = number(cells.get(PRICE_COLUMN));
                softly.assertTrue(close(before.add(change), after), "\"" + cells.get("itemId") + "\" with " + method
                        + ": السعر قبل التعديل " + before + " + التعديل " + change + " ≠ السعر بعد التعديل " + after + ".");
            }
        }
        softly.assertAll();
       // page.closeForm();
    }

    @Test(priority = 14, groups = {PR, "validation"},
            description = "A price below the cost asks \"أصناف الفارق فيها بالسالب\" listing the item; "
                    + "\"لا، مراجعة الأصناف\" leaves the operation unsaved")
    public void priceBelowCostAsksForConfirmation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        int row = -1;
        for (int r = 0; r < page.lineCount() && row < 0; r++) {
            if (page.lineValue(r, COST_COLUMN).signum() > 0) {
                row = r;
            }
        }
        if (row < 0) {
            throw new SkipException("None of the loaded items has a cost; cannot price below cost.");
        }
        String item = page.lineText(row, ITEM_CODE_COLUMN);
        page.setLineNumber(row, PRICE_COLUMN, "0.01");
        page.stopCellEditing();
        String note = uniqueNote();
        int before = pricingCount();
        page.setNote(note);

        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog();
        // QC 2026-11-22: when another line kept its price, "أصناف بدون تسعير" comes first
        // (the lemon line stayed 130.43): "نعم، حفظ" leads to the negative-difference one.
        waitUntil("a save question after the confirmation", Duration.ofSeconds(10),
                () -> page.topDialogText().contains(PricingOperationPage.NEGATIVE_DIFFERENCE_TITLE)
                        || page.topDialogText().contains(PricingOperationPage.UNPRICED_ITEMS_TITLE));
        if (page.topDialogText().contains(PricingOperationPage.UNPRICED_ITEMS_TITLE)) {
            log.info("\"{}\" asked first → نعم، حفظ.", PricingOperationPage.UNPRICED_ITEMS_TITLE);
            page.confirmDialog();
        }
        waitUntil("the price-below-cost warning", Duration.ofSeconds(10),
                () -> page.topDialogText().contains(PricingOperationPage.NEGATIVE_DIFFERENCE_TITLE));
        String warning = page.topDialogText();
        log.info("Warning: {}", warning);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(warning.contains(item), "The warning does not list item " + item + ": " + warning);
        page.clickDialogButton(PricingOperationPage.REVIEW_ITEMS_BUTTON);
        waitUntil("the warning to close", Duration.ofSeconds(10), () -> !page.isDialogOpen());
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "\"مراجعة الأصناف\" must keep the operation unsaved.");
        softly.assertTrue(pricingCount() == before, "The operation was saved although the warning was declined.");
        softly.assertAll();
        //page.closeForm();
    }

    // =====================================================================
    // Header behaviour
    // =====================================================================

    @Test(priority = 15, groups = {PR, "validation"},
            description = "يدوي shows \"اضافة سطر\" and locks the automatic fields; تلقائي unlocks التطبيق على / الأصناف / "
                    + "تسعير الأصناف / انزال من and hides \"اضافة سطر\"")
    public void itemEntryModeSwitchesTheFields() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        SoftAssert softly = new SoftAssert();
        page.setItemEntry(PricingOperationPage.ENTRY_MANUAL);
        softly.assertTrue(page.isAddLineShown(), "\"اضافة سطر\" is not shown with manual entry.");
        for (String field : PricingOperationPage.AUTOMATIC_ENTRY_FIELDS) {
            softly.assertTrue(page.isSelectDisabled(field), "\"" + field + "\" must be locked with manual entry.");
        }
        page.setItemEntry(PricingOperationPage.ENTRY_AUTOMATIC);
        for (String field : PricingOperationPage.AUTOMATIC_ENTRY_FIELDS) {
            softly.assertTrue(page.waitForSelectEnabled(field, Duration.ofSeconds(3)), "\"" + field + "\" must be editable with automatic entry.");
        }
        softly.assertFalse(page.isAddLineShown(), "\"اضافة سطر\" must be hidden with automatic entry.");
        softly.assertFalse(page.isLoadItemsEnabled(), "\"انزال\" must be disabled until an item is chosen.");
        page.selectRandomItems(1);
        softly.assertTrue(page.isLoadItemsEnabled(), "\"انزال\" stayed disabled after choosing an item.");
        softly.assertAll();
        //page.closeForm();
    }

    @Test(priority = 16, groups = {PR, "validation"},
            description = "Each \"طريقة التطبيق\" shows its own fields: زيادة → نوع الزيادة + النسبة; تخفيض → نوع التخفيض + "
                    + "النسبة; معادلة → خصم + مبلغ إضافي")
    public void applicationMethodShowsItsFields() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        SoftAssert softly = new SoftAssert();
        Map<String, List<String>> fields = Map.of(
                PricingOperationPage.METHOD_INCREASE, List.of("typeOfExtra", "extraRate"),
                PricingOperationPage.METHOD_DISCOUNT, List.of("typeOfDiscount", "discountRate"),
                PricingOperationPage.METHOD_FORMULA, List.of("formulaDiscountRate", "formulaExtraValue"));
        for (Map.Entry<String, List<String>> method : fields.entrySet()) {
            page.setApplicationMethod(method.getKey());
            for (String field : method.getValue()) {
                softly.assertTrue(page.hasField(field, Duration.ofSeconds(2)), "\"" + field + "\" is not shown for " + method.getKey() + ".");
            }
        }
        softly.assertAll();
        //page.closeForm();
    }

    @Test(priority = 17, groups = {PR, "validation"},
            description = "Option lists: أساس المقارنة (4), طريقة التطبيق (4), سياسة التعديل (2)")
    public void pricingOptionListsAreComplete() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        SoftAssert softly = new SoftAssert();
        for (Map.Entry<String, List<String>> field : Map.of(
                "baseOfComparison", PricingOperationPage.BASE_OF_COMPARISON_OPTIONS,
                "applicationMethod", PricingOperationPage.APPLICATION_METHOD_OPTIONS,
                "changePolicy", PricingOperationPage.CHANGE_POLICY_OPTIONS).entrySet()) {
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            for (int i = 0; i < 15 && seen.size() < field.getValue().size(); i++) {
                seen.add(page.selectRandomOption(field.getKey()));
                page.confirmDialogIfShown(Duration.ofMillis(500));
            }
            softly.assertEquals(new java.util.HashSet<>(seen), new java.util.HashSet<>(field.getValue()),
                    "Options of \"" + field.getKey() + "\" differ.");
        }
        softly.assertAll();
       // page.closeForm();
    }

    // =====================================================================
    // Edit / Undo / Delete / Approve
    // =====================================================================

    @Test(priority = 18, groups = {PR, "edit"},
            description = "Edit a Draft: the FIRST line's price change is kept, the other line keeps its price; "
                    + "it stays a Draft")
    public void userCanEditDraftPricing() {
        editDraftPrice(false, org.openqa.selenium.Keys.ENTER);
    }

    @Test(priority = 19, groups = {PR, "edit"},
            description = "Edit a Draft: a price typed on the LAST line and confirmed with Enter is kept, and the other "
                    + "lines keep their prices")
    public void lastLinePriceIsKeptInEditMode() {
        // QC 2026-11-22 (checked by hand on التسعير 72): in edit mode, Enter on the last
        // line adds an empty line and EVERY line goes back to its "السعر قبل التعديل"
        // (100 / 100 → 490.12 / 115.07; with زيادة 39%: 171.9 → 4 × 1.39 = 5.56).
        // Enter on another line, Tab (confirmed by hand, 2026-11-22), or the same keys while
        // creating, keep the prices.
        editDraftPrice(true, org.openqa.selenium.Keys.ENTER);
    }

    @Test(priority = 20, groups = {PR, "edit"},
            description = "Edit a Draft: a price typed on the LAST line and confirmed with Tab is kept, and the other "
                    + "lines keep their prices")
    public void lastLinePriceConfirmedWithTabIsKeptInEditMode() {
        // Same as lastLinePriceIsKeptInEditMode but with Tab, which keeps the prices
        // (QC 2026-11-22, by hand): tells the Enter bug apart from editing itself.
        editDraftPrice(true, org.openqa.selenium.Keys.TAB);
    }

    /**
     * The test's own Draft (QC data is not changed) with طريقة التطبيق = يدوي and
     * two lines; in edit mode the price of the first or the last line + 1 is
     * typed (confirmed with {@code confirmKey}), the other line must keep its
     * price, then it is saved and checked after reload.
     */
    private void editDraftPrice(boolean lastLine, org.openqa.selenium.Keys confirmKey) {
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        useManualMethod();
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        saveAndVerify(DRAFT);
        page.clickEdit();
        waitForAnyEditMode();
        page.deleteEmptyLines();
        int row = lastLine ? page.lineCount() - 1 : 0;
        int other = lastLine ? 0 : page.lineCount() - 1;
        String item = page.lineText(row, "itemId");
        String otherItem = page.lineText(other, "itemId");
        BigDecimal otherPrice = page.lineValue(other, PRICE_COLUMN);
        BigDecimal price = page.lineValue(row, PRICE_COLUMN).add(BigDecimal.ONE);
        // One try only (no retry into the empty line the grid may add → no "البحث عن صنف").
        BigDecimal shown = number(page.typeLineNumberOnce(row, PRICE_COLUMN, price.toPlainString(), confirmKey));
        page.stopCellEditing();
        BigDecimal otherNow = page.lineValue(other, PRICE_COLUMN);
        SoftAssert typed = new SoftAssert();
        typed.assertTrue(close(shown, price), "In edit mode the price " + price + " typed on line " + (row + 1) + " ("
                + item + ") and confirmed with " + confirmKey.name() + " turned into " + shown + ".");
        typed.assertTrue(close(otherNow, otherPrice), "Editing line " + (row + 1) + " changed the price of " + otherItem
                + " from " + otherPrice + " to " + otherNow + ".");
        typed.assertAll();
        page.deleteEmptyLines();
        int lines = page.lineCount();
        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineCount(), lines, "The number of lines of " + code + " changed after the edit.");
        List<String> names = page.lineTexts("itemId");
        softly.assertTrue(names.contains(item) && close(page.lineValue(names.indexOf(item), PRICE_COLUMN), price),
                "The price of " + item + " was not saved as " + price + ".");
        softly.assertTrue(names.contains(otherItem) && close(page.lineValue(names.indexOf(otherItem), PRICE_COLUMN), otherPrice),
                "The price of " + otherItem + " changed from " + otherPrice + " after the edit.");
        softly.assertAll();
    }

    @Test(priority = 21, groups = {PR, "edit"},
            description = "Edit a Draft and delete one of its lines: after save the line is gone and the other lines are kept")
    public void userCanDeleteLineFromDraftPricing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        String code = saveAndVerify(DRAFT);

        page.clickEdit();
        waitForAnyEditMode();
        page.deleteEmptyLines();
        List<String> lines = linesSnapshot();
        assertTrue(lines.size() >= 2, code + " needs two lines to delete one: " + lines);
        String removed = page.lineText(0, ITEM_CODE_COLUMN);
        page.deleteLine(0);
        page.stopCellEditing();
        assertEquals(page.lineCount(), lines.size() - 1, "\"حذف السطر\" did not remove the line of " + removed + ".");
        saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();

        List<String> expected = lines.subList(1, lines.size()).stream().sorted().toList();
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(lineCodes().contains(removed), "Deleted item " + removed + " is still in " + code + " after save.");
        softly.assertEquals(linesSnapshot().stream().sorted().toList(), expected,
                "The other lines of " + code + " changed after deleting " + removed + ".");
        softly.assertAll();
    }

    @Test(priority = 22, groups = {PR, "edit"},
            description = "Edit a Draft: after a line price is changed, \"البيان\" clicked and typed with the keyboard "
                    + "gets the whole text and no \"البحث عن صنف\" opens")
    public void descriptionCanBeTypedAfterEditingALine() {
        // QC 2026-11-22, by hand on التسعير 123 (real clicks and keys): price 170 + Tab,
        // click in "البيان", keys "a b ␣ 1 2 ␣ c" → "البيان" stayed EMPTY, "البحث عن صنف"
        // opened and the price became 84.7. In the automation only "تسعيرآلي" arrived
        // of "تسعير آلي 20261122-…".
        createDocument();
        page.clickEdit();
        waitForAnyEditMode();
        BigDecimal changed = page.lineValue(0, PRICE_COLUMN).add(BigDecimal.ONE);
        page.typeLineNumberOnce(0, PRICE_COLUMN, changed.toPlainString(), org.openqa.selenium.Keys.TAB);
        page.stopCellEditing();
        String note = uniqueNote();
        try {
            page.typeNote(note);
        } catch (IllegalStateException lost) {
            org.testng.Assert.fail("After a line edit, typing \"البيان\" did not work: " + lost.getMessage()
                    + (page.topDialogText().contains("البحث عن صنف") ? " \"البحث عن صنف\" opened." : ""));
        }
        assertFalse(page.topDialogText().contains("البحث عن صنف"), "\"البحث عن صنف\" opened while typing \"البيان\".");
        page.closeForm();
    }

    @Test(priority = 23, groups = {PR, "status"},
            description = "Delete a Draft created by the test: success message; not returned by the API any more")
    public void userCanDeleteDraftPricing() {

        createDocument(); // the test's own Draft: QC data is not deleted
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft " + code + ".");
        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);
        assertTrue(page.recordByCode(code) == null || Boolean.TRUE.equals(page.recordByCode(code).get("isDeleted")),
                code + " is still returned by the API after deletion.");
    }

    @Test(priority = 24, groups = {PR, "status"},
            description = "Approve a Draft created by the test: Approved after reload and in the API "
                    + "(the locked toolbar is checked by approvedPricingIsLocked)")
    public void userCanApproveDraftPricing() {

        createDocument(); // the test's own Draft: QC drafts are not approved
        String code = page.waitForCode();
        boolean asked = page.approve(PricingOperationPage.APPROVE_CONTINUE);
        log.info("Approving {}: already-priced question {}", code, asked ? "asked" : "not asked");
        page.waitForStatusLabel(APPROVED.label());
        page.reload();
        page.waitForStatusLabel(APPROVED.label());

        Map<String, Object> record = page.recordByCode(code);
        assertTrue(record != null && PricingOperationPage.isApproved(record),
                code + " is not Approved (recordStatus 1) in the API: " + record);
    }

    @Test(priority = 25, groups = {PR, "status", "request"},
            description = "Approving items that are already priced and choosing \"إنشاء طلب تعديل الأسعار\" creates a "
                    + "new price change request")
    public void approvingAlreadyPricedItemsCanCreateAPriceChangeRequest() {

        DocumentApi requests = new DocumentApi(driver, PriceChangeRequestPage.API_RESOURCE);
        int before = requests.count();

        // Price the same items twice in the same list: the second approval may ask the question.
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        String priceList = form.getSelectedText("salePriceListId");
        List<String> items = loadRandomItems();
        saveAndVerify(DRAFT);
        page.approve(PricingOperationPage.APPROVE_CONTINUE);
        page.waitForStatusLabel(APPROVED.label());

        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        page.selectOption("salePriceListId", priceList);
        page.setItemEntry(PricingOperationPage.ENTRY_AUTOMATIC);
        for (String item : items) {
            page.selectOption("items", item);
        }
        page.loadItems();
        page.setApplicationMethod(PricingOperationPage.METHOD_INCREASE);
        page.setNumberField("extraRate", "5");
        saveAndVerify(DRAFT);
        boolean asked = page.approve(PricingOperationPage.APPROVE_CREATE_REQUEST);
        if (!asked) {
            throw new SkipException("The approval did not ask about already-priced items (list " + priceList + ", items " + items + ").");
        }
        int after = requests.count();
        log.info("Price change requests: {} → {}", before, after);
        assertEquals(after, before + 1, "Choosing \"" + PricingOperationPage.APPROVE_CREATE_REQUEST + "\" did not create a request.");
    }

    // =====================================================================
    // Toolbar per status
    // =====================================================================

    @Test(priority = 26, groups = {PR, "validation"},
            description = "New operation toolbar: save waits for a change; add/undo/delete/print/approve/log disabled")
    public void newPricingEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("xIcon");
        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.isActionEnabled("save"), "'save' should wait for a change (QC 2026-11-12).");
        for (String action : List.of("add", "undo", "delete", "print", "approveOrDisapprove", "transactionLogs")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new operation.");
        }
        for (String action : List.of("xIcon", "uploadAttachment", "recordSearch")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new operation.");
        }
        page.setNote(uniqueNote());
        softly.assertTrue(page.waitForActionEnabled("save"), "'save' stayed disabled after typing the description.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 27, groups = {PR, "validation", "status"},
            description = "Draft toolbar: add/edit/delete/approve/attachments/log enabled; save disabled")
    public void draftPricingEnablesEditActions() {

        openDraft();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("add", "edit", "delete", "approveOrDisapprove", "uploadAttachment", "transactionLogs")) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("save"), "'save' must be disabled in view mode.");
        softly.assertAll();
    }

    @Test(priority = 28, groups = {PR, "validation", "status"},
            description = "Approved toolbar: edit/delete/approve disabled (no disapprove, no cancel)")
    public void approvedPricingIsLocked() {

        openRandom(PricingOperationPage::isApproved);
        String code = page.waitForCode();
        page.waitForStatusLabel(APPROVED.label());
        page.waitForActionEnabled("add");
        SoftAssert softly = new SoftAssert();
        for (String action : List.of("edit", "delete", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on Approved " + code + ".");
        }
        softly.assertFalse(page.hasAction("cancel"), "Pricing has no \"إلغاء\" (QC 2026-11-12).");
        softly.assertAll();
    }

    @Override
    @Test(priority = 29, groups = {PR, "validation"},
            description = "Saving without lines is blocked: \"يجب اضافة صنف واحد على الأقل\", no confirmation, stays in Add")
    public void userCannotSaveEmptyDocument() {
        super.userCannotSaveEmptyDocument();
    }

    @Test(priority = 30, groups = {PR, "validation"},
            description = "Clearing the required \"طريقة التطبيق\" blocks save with \"هذا الحقل مطلوب\"; nothing is saved")
    public void userCannotSavePricingWithoutApplicationMethod() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        int before = pricingCount();
        page.clearApplicationMethod();
        page.clickSave();
        waitUntil("the required-field error", Duration.ofSeconds(5),
                () -> page.isTextShown(PricingOperationPage.REQUIRED_MESSAGE));

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.isDialogOpen(), "Save asked for confirmation although \"طريقة التطبيق\" is empty.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The operation left add mode without \"طريقة التطبيق\".");
        softly.assertTrue(pricingCount() == before, "An operation without \"طريقة التطبيق\" was saved.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 31, groups = {PR, "validation", "calculation"},
            description = "تخفيض النسبة boundaries: 100 → every price 0 with no error; 101 → \"يجب أن يكون أقل من أو يساوي 100\" "
                    + "and save is blocked; a negative rate is not accepted")
    public void discountRateMustBeBetweenZeroAndHundred() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        int before = pricingCount();
        page.setApplicationMethod(PricingOperationPage.METHOD_DISCOUNT);
        SoftAssert softly = new SoftAssert();

        page.setNumberField("discountRate", "100");
        page.waitForGridToSettle();
        softly.assertTrue(page.pricesAfter().stream().allMatch(p -> p.signum() == 0),
                "تخفيض 100% must make every price 0: " + page.pricesAfter());
        softly.assertFalse(page.isTextShown(PricingOperationPage.RATE_MAX_MESSAGE), "100% must be accepted.");

        page.setNumberField("discountRate", "101");
        softly.assertTrue(page.isTextShown(PricingOperationPage.RATE_MAX_MESSAGE),
                "101% did not show \"" + PricingOperationPage.RATE_MAX_MESSAGE + "\".");
        page.clickSave();
        pause(1500);
        softly.assertFalse(page.isDialogOpen(), "Save asked for confirmation with a 101% discount.");
        softly.assertTrue(pricingCount() == before, "An operation with a 101% discount was saved.");

        boolean negativeKept;
        try {
            negativeKept = page.setNumberField("discountRate", "-5");
        } catch (IllegalStateException refused) {
            negativeKept = false; // the input did not keep "-5"
        }
        softly.assertFalse(negativeKept, "A negative discount rate was accepted.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 32, groups = {PR, "validation", "create"},
            description = "هامش الربح = قيمة: \"القيمة\" 0 blocks save with \"هذا الحقل مطلوب\"; 10 saves a Draft "
                    + "with قيمة / 10 kept after reload and in the API")
    public void profitMarginValueIsRequiredAndSaved() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        page.selectOption(PricingOperationPage.PROFIT_MARGIN_TYPE, PricingOperationPage.MARGIN_BY_VALUE);
        assertTrue(page.hasField(PricingOperationPage.MARGIN_VALUE_FIELD, Duration.ofSeconds(3)),
                "\"القيمة\" is not shown for هامش الربح = قيمة.");
        page.setNumberField(PricingOperationPage.MARGIN_VALUE_FIELD, "0"); // the header may have filled it
        int before = pricingCount();

        page.clickSave();
        waitUntil("the required-field error", Duration.ofSeconds(5),
                () -> page.isTextShown(PricingOperationPage.REQUIRED_MESSAGE));
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(form.fieldsWithRequiredError().contains(PricingOperationPage.MARGIN_VALUE_FIELD),
                "\"القيمة\" 0 is not marked required: " + form.fieldsWithRequiredError());
        softly.assertFalse(page.isDialogOpen(), "Save asked for confirmation with \"القيمة\" 0.");
        softly.assertEquals(pricingCount(), before, "An operation with هامش الربح قيمة 0 was saved.");

        page.setNumberField(PricingOperationPage.MARGIN_VALUE_FIELD, "10");
        String code = saveAndVerify(DRAFT);
        page.reload();
        softly.assertEquals(waitForSelected(form, PricingOperationPage.PROFIT_MARGIN_TYPE), PricingOperationPage.MARGIN_BY_VALUE,
                "\"هامش الربح\" of " + code + " was not kept after reload.");
        softly.assertEquals(number(page.textValue(PricingOperationPage.MARGIN_VALUE_FIELD)).compareTo(BigDecimal.TEN), 0,
                "\"القيمة\" of " + code + " was not kept as 10 after reload.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertEquals(DocumentApi.intOf(record, PricingOperationPage.PROFIT_MARGIN_TYPE), 1,
                "typeOfProfitMargin of " + code + " is not 1 (قيمة) in the API.");
        softly.assertEquals(number(String.valueOf(record.get(PricingOperationPage.MARGIN_VALUE_FIELD))).compareTo(BigDecimal.TEN), 0,
                "profitMarginValue of " + code + " is not 10 in the API.");
        softly.assertAll();
    }

    @Test(priority = 33, groups = {PR, "validation"},
            description = "هامش الربح = نسبة shows \"النسبة\" instead of \"القيمة\"; 101 shows \"يجب أن يكون أقل من أو يساوي 100\" "
                    + "and save is blocked")
    public void profitMarginRateMustNotExceedHundred() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        page.selectOption(PricingOperationPage.PROFIT_MARGIN_TYPE, PricingOperationPage.MARGIN_BY_RATE);
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(page.hasField(PricingOperationPage.MARGIN_RATE_FIELD, Duration.ofSeconds(3)),
                "\"النسبة\" is not shown for هامش الربح = نسبة.");
        softly.assertFalse(page.hasField(PricingOperationPage.MARGIN_VALUE_FIELD, Duration.ofMillis(500)),
                "\"القيمة\" is shown for هامش الربح = نسبة.");
        int before = pricingCount();

        page.setNumberField(PricingOperationPage.MARGIN_RATE_FIELD, "101");
        softly.assertTrue(page.isTextShown(PricingOperationPage.RATE_MAX_MESSAGE),
                "A 101% profit margin did not show \"" + PricingOperationPage.RATE_MAX_MESSAGE + "\".");
        page.clickSave();
        pause(1500);
        softly.assertFalse(page.isDialogOpen(), "Save asked for confirmation with a 101% profit margin.");
        softly.assertEquals(pricingCount(), before, "An operation with a 101% profit margin was saved.");
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 34, groups = {PR, "create", "exit"},
            description = "Closing a new operation with changes asks to discard them: \"إلغاء\" keeps the form and its data; "
                    + "\"نعم\" returns to the list and nothing is saved")
    public void userCanDiscardUnsavedPricing() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        loadRandomItems();
        String note = page.note();
        int lines = page.lineCount();
        int before = pricingCount();

        page.clickAction("xIcon");
        waitUntil("the discard question", Duration.ofSeconds(5),
                () -> page.topDialogText().contains(PricingOperationPage.DISCARD_QUESTION));
        page.clickDialogButton(PricingOperationPage.DIALOG_CANCEL);
        waitUntil("the question to close", Duration.ofSeconds(5), () -> !page.isDialogOpen());
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "\"إلغاء\" closed the new operation.");
        softly.assertEquals(page.note(), note, "\"إلغاء\" lost \"البيان\".");
        softly.assertEquals(page.lineCount(), lines, "\"إلغاء\" lost the lines.");

        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing the changed operation did not ask again.");
        page.confirmDialog();
        assertBackOnList("discarding a new operation");
        softly.assertTrue(pricingCount() == before, "The discarded operation was saved.");
        softly.assertAll();
    }

    @Test(priority = 35, groups = {PR, "status"},
            description = "Delete → \"هل انت متاكد من الحذف؟\" → \"إلغاء\": the Draft stays, unchanged")
    public void cancellingDeleteKeepsPricing() {

        openDraft(); // nothing is changed: the deletion is cancelled
        String code = page.waitForCode();
        page.clickDelete();
        waitUntil("the delete question", Duration.ofSeconds(5),
                () -> page.topDialogText().contains(PricingOperationPage.DELETE_QUESTION));
        page.clickDialogButton(PricingOperationPage.DIALOG_CANCEL);
        waitUntil("the question to close", Duration.ofSeconds(5), () -> !page.isDialogOpen());

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.waitForCode(), code, "Cancelling the deletion left " + code + ".");
        softly.assertEquals(page.statusLabel(), DRAFT.label(), code + " is no longer a Draft after cancelling.");
        Map<String, Object> record = page.recordByCode(code);
        softly.assertTrue(record != null && PricingOperationPage.isDraft(record),
                code + " was deleted although the deletion was cancelled: " + record);
        softly.assertAll();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /**
     * Header (QC 2026-11-12 order): قوائم الأسعار (any list but the default one) →
     * البيان → آلية إدخال الأصناف (يدوي: the lines are added one by one) → أساس
     * المقارنة → سياسة التعديل → هامش الربح + its value → طريقة التطبيق + its
     * fields → التعامل بكافة الوحدات, all at random on a new operation. Kept fixed:
     * "الفرع" (locked), التطبيق على = الأصناف and تسعير الأصناف = الكل (the
     * automatic tests choose items, which another value would hide), انزال من
     * (needs a source document).
     */
    @Override
    protected void fillHeader(boolean editing) {
        selectPriceListOtherThanDefault(editing);
        String note = uniqueNote();
        page.setNote(note);
        assertEquals(page.waitForNote(note), note, "\"البيان\" was not filled.");
        if (!page.isSelectDisabled("downTypeId")) {
            page.setItemEntry(PricingOperationPage.ENTRY_MANUAL);
        }
        selectIfEditable("baseOfComparison", editing);
        selectIfEditable("changePolicy", editing);
        if (editing) {
            return; // an edited operation keeps its margin, method and units
        }
        randomProfitMargin();
        randomApplicationMethod();
        page.setAllUnits(RANDOM.nextBoolean());
    }

    private static final java.util.Random RANDOM = new java.util.Random();

    /** A random whole number in [min, max] as text. */
    private static String randomNumber(int min, int max) {
        return String.valueOf(min + RANDOM.nextInt(max - min + 1));
    }

    /**
     * "هامش الربح" = نسبة or قيمة with a value 1–50 (0 counts as empty and blocks
     * save). The margin does not change the line prices (QC 2026-11-19).
     */
    private void randomProfitMargin() {
        String type = page.selectRandomOption(PricingOperationPage.PROFIT_MARGIN_TYPE);
        String field = PricingOperationPage.MARGIN_BY_VALUE.equals(type)
                ? PricingOperationPage.MARGIN_VALUE_FIELD : PricingOperationPage.MARGIN_RATE_FIELD;
        page.setNumberField(field, randomNumber(1, 50));
    }

    /**
     * "طريقة التطبيق" at random with its own fields: زيادة / تخفيض 1–50 %
     * (type left at نسبة), معادلة خصم 1–50 % + مبلغ إضافي 1–50. Tests that compare
     * prices with a base call {@link #useManualMethod()} before loading items.
     */
    private void randomApplicationMethod() {
        if (page.isSelectDisabled("applicationMethod")) {
            return;
        }
        String method = PricingOperationPage.APPLICATION_METHOD_OPTIONS.get(
                RANDOM.nextInt(PricingOperationPage.APPLICATION_METHOD_OPTIONS.size()));
        page.setApplicationMethod(method);
        switch (method) {
            case PricingOperationPage.METHOD_INCREASE -> page.setNumberField("extraRate", randomNumber(1, 50));
            case PricingOperationPage.METHOD_DISCOUNT -> page.setNumberField("discountRate", randomNumber(1, 50));
            case PricingOperationPage.METHOD_FORMULA -> {
                page.setNumberField("formulaDiscountRate", randomNumber(1, 50));
                page.setNumberField("formulaExtraValue", randomNumber(1, 50));
            }
            default -> { } // يدوي: no field
        }
        log.info("طريقة التطبيق = {}", method);
    }

    /** "طريقة التطبيق" = يدوي, so the loaded prices are the base the calculation tests compare. */
    private void useManualMethod() {
        page.setApplicationMethod(PricingOperationPage.METHOD_MANUAL);
    }

    /** A new line gets a random price 10.00–500.99 (the only editable price cell needed). */
    @Override
    protected void fillNewLine(int row) {
        String price = randomNumber(10, 500) + "." + String.format("%02d", RANDOM.nextInt(100));
        page.setLineNumber(row, PRICE_COLUMN, price);
    }

    /**
     * QC 2026-11-19: right after "جديد" the field was not rendered yet, so
     * isSelectDisabled() said "locked" and the list was never chosen: every new
     * operation stayed on the default list. A new operation now waits for the
     * field and fails when it stays locked; an edited one may keep its list.
     */
    private void selectPriceListOtherThanDefault(boolean editing) {
        boolean editable = editing
                ? !page.isSelectDisabled("salePriceListId")
                : page.waitForSelectEnabled("salePriceListId", Duration.ofSeconds(10));
        if (!editable) {
            if (!editing) {
                org.testng.Assert.fail("\"قوائم الأسعار\" stayed locked on a new operation; "
                        + "the tests must not use the default list.");
            }
            log.info("\"قوائم الأسعار\" is locked while editing → kept as {}.", form.getSelectedText("salePriceListId"));
            return;
        }
        for (int attempt = 0; attempt < 6; attempt++) {
            String chosen = page.selectRandomOption("salePriceListId");
            page.confirmDialogIfShown(Duration.ofMillis(800));
            if (!chosen.equals(DEFAULT_PRICE_LIST)) {
                log.info("قوائم الأسعار = {}", chosen);
                return;
            }
        }
        org.testng.Assert.fail("Only the default price list could be chosen; the tests must not reprice it.");
    }

    /** تلقائي → {@link #ITEMS_TO_LOAD} random items → "انزال"; returns the chosen item labels. */
    private List<String> loadRandomItems() {
        page.setItemEntry(PricingOperationPage.ENTRY_AUTOMATIC);
        List<String> items = page.selectRandomItems(ITEMS_TO_LOAD);
        page.loadItems();
        page.waitForGridToSettle();
        assertTrue(page.lineCount() > 0, "\"انزال\" loaded no line for " + items + ".");
        return items;
    }

    /**
     * Loads items, applies {@code method} with rate 10 % and checks the SAVED lines:
     * السعر بعد التعديل = السعر قبل التعديل × {@code factor} and التعديل = بعد − قبل.
     *
     * <p>"السعر قبل التعديل" is the value of "أساس المقارنة", not the price the
     * grid shows under يدوي (QC 2026-11-22, آخر سعر شراء: ميني جاتوه كراميل showed
     * 0, تخفيض 10% gave 108 and the API saved قبل 120, التعديل −12, بعد 108). It is
     * not a grid column, so it is read from the API after save.</p>
     */
    private void verifyRateMethod(String method, String rateField, BigDecimal factor) {
        verifyMethod(method, () -> {
            page.selectOption(rateField.equals("extraRate") ? "typeOfExtra" : "typeOfDiscount",
                    PricingOperationPage.RATE_TYPE_PERCENT);
            page.setNumberField(rateField, RATE.toPlainString());
        }, before -> before.multiply(factor), "قبل × " + factor);
    }

    /**
     * Loads items under يدوي, applies {@code method} with {@code fillFields}, saves,
     * and checks every SAVED line: السعر بعد التعديل = {@code expected}(السعر قبل
     * التعديل), التعديل = بعد − قبل, and the grid shows the saved قبل / التعديل.
     * QC 2026-11-22: معادلة خصم 10% + 5 gave 15.12 → 18.61 and 490.12 → 446.11,
     * i.e. قبل × 0.90 + 5.
     */
    private void verifyMethod(String method, Runnable fillFields,
                              java.util.function.UnaryOperator<BigDecimal> expected, String formula) {
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        useManualMethod();
        loadRandomItems();
        List<BigDecimal> shown = page.pricesAfter();
        page.setApplicationMethod(method);
        fillFields.run();
        page.waitForGridToSettle();
        List<BigDecimal> after = waitForPricesToChange(shown);
        log.info("{}: prices shown {} → {}", method, shown, after);

        String code = saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();
        assertTrue(sameValues(page.pricesAfter(), after), "Prices of " + code + " changed after save: " + page.pricesAfter());

        List<Map<String, Object>> lines = savedLines(code);
        // An item with no value for "أساس المقارنة" has قبل 0: 0 × factor = 0 checks nothing.
        if (lines.stream().noneMatch(l -> number(String.valueOf(l.get("priceBefore"))).signum() > 0)) {
            throw new SkipException("No saved line of " + code + " has a \"السعر قبل التعديل\" (all 0): " + lines);
        }
        SoftAssert softly = new SoftAssert();
        // The grid's "السعر قبل التعديل" / "التعديل" must show the saved values. QC
        // 2026-11-22: "السعر قبل التعديل" is the column priceBefore2 (saved 0) and showed 0
        // for both items while the API had priceBefore 120 / 20; "التعديل" (change) was right.
        // Both columns can be hidden in the grid's column settings: then nothing is compared.
        Map<String, Map<String, String>> grid = new java.util.HashMap<>();
        page.readListRows(List.of("itemId", "priceBefore2", "change"))
                .values().forEach(cells -> grid.put(cells.getOrDefault("itemId", ""), cells));
        for (Map<String, Object> line : lines) {
            BigDecimal before = number(String.valueOf(line.get("priceBefore")));
            BigDecimal change = number(String.valueOf(line.get("change")));
            BigDecimal priceAfter = number(String.valueOf(line.get(PRICE_COLUMN)));
            String item = String.valueOf(line.get("itemName"));
            log.info("Saved {}: قبل {} + التعديل {} = بعد {}", item, before, change, priceAfter);
            softly.assertTrue(close(priceAfter, expected.apply(before)), "Saved \"السعر بعد التعديل\" of " + item + " is "
                    + priceAfter + ", expected " + formula + " = " + expected.apply(before) + " (قبل = " + before + ").");
            softly.assertTrue(close(change, priceAfter.subtract(before)), "Saved \"التعديل\" of " + item + " is " + change
                    + ", expected " + priceAfter + " − " + before + ".");
            Map<String, String> cells = grid.getOrDefault(item, Map.of());
            if (cells.containsKey("priceBefore2")) {
                softly.assertTrue(close(number(cells.get("priceBefore2")), before), "Grid \"السعر قبل التعديل\" of " + item
                        + " shows " + cells.get("priceBefore2") + "; saved السعر قبل التعديل (priceBefore) is " + before + ".");
            } else {
                log.warn("\"السعر قبل التعديل\" is hidden in the grid → not compared.");
            }
            if (cells.containsKey("change")) {
                softly.assertTrue(close(number(cells.get("change")), change), "Grid \"التعديل\" of " + item
                        + " shows " + cells.get("change") + "; saved " + change + ".");
            }
        }
        softly.assertAll();
    }

    /** Lines of operation {@code code} as the API saved them (GET SalePriceOperation/{id}). */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> savedLines(String code) {
        Object id = page.recordByCode(code).get("id");
        DocumentApi api = new DocumentApi(driver, PricingPage.API_RESOURCE);
        String url = api.apiUrlsUsedByPage("SalePriceOperation/" + id).stream().reduce((a, b) -> b)
                .orElseThrow(() -> new IllegalStateException("The page did not load SalePriceOperation/" + id + "."));
        Object json = api.getJson(url);
        while (json instanceof Map<?, ?> map && map.containsKey("data") && !map.containsKey("saleItemPriceOperations")) {
            json = map.get("data"); // the API wraps the record: {data: {data: {...}}}
        }
        return (List<Map<String, Object>>) ((Map<String, Object>) json).get("saleItemPriceOperations");
    }

    /**
     * Pricing operations in the API. "البيان" is not saved on this screen (QC
     * 2026-11-19: no record has a note), so "was it saved?" is checked by the
     * count, not by {@code recordByNote}.
     */
    private int pricingCount() {
        return new DocumentApi(driver, PricingPage.API_RESOURCE).count();
    }

    /** Header selects compared before save and after reload (those shown on the form). */
    private static final List<String> HEADER_SELECTS = List.of("salePriceListId", "downTypeId", "baseOfComparison",
            "changePolicy", PricingOperationPage.PROFIT_MARGIN_TYPE, "applicationMethod", "applyOn", "downPricingType",
            "typeOfExtra", "typeOfDiscount");
    /** Header numbers compared before save and after reload (those shown on the form). */
    private static final List<String> HEADER_NUMBERS = List.of(PricingOperationPage.MARGIN_VALUE_FIELD,
            PricingOperationPage.MARGIN_RATE_FIELD, "extraRate", "discountRate", "formulaDiscountRate", "formulaExtraValue");
    /**
     * Line cells compared before save and after reload. "رمز الصنف" is left out:
     * before save it shows the barcode (QC 2026-11-19, checked by
     * {@link #itemCodesByName()}).
     */
    private static final List<String> LINE_CELLS = List.of("itemId", "uomId", "barcode", "displayPackingUnit", "avgCost",
            PRICE_COLUMN, DIFFERENCE_COLUMN, TAX_COLUMN, PRICE_WITH_TAX_COLUMN, MARGIN_RATIO_COLUMN, MARGIN_VALUE_COLUMN);

    /**
     * Every header field and line cell of the form as "name → value": header
     * selects and numbers that are shown, "التعامل بكافة الوحدات", the chosen
     * items, and each line (keyed by item name / unit) cell by cell. "البيان" is
     * checked by {@link #descriptionIsKeptAfterSave()} only.
     */
    private Map<String, String> formSnapshot() {
        Map<String, String> snapshot = new java.util.TreeMap<>();
        for (String field : HEADER_SELECTS) {
            if (page.hasField(field, Duration.ofMillis(100))) {
                snapshot.put(field, form.getSelectedText(field).trim());
            }
        }
        for (String field : HEADER_NUMBERS) {
            if (page.hasField(field, Duration.ofMillis(100))) {
                snapshot.put(field, normalized(page.textValue(field)));
            }
        }
        snapshot.put("downAllUOM", String.valueOf(page.isAllUnits()));
        // Manual entry: "الأصناف" is locked and empty while lines are added, and
        // shows the lines' items once saved (QC 2026-11-22) → compared only for تلقائي.
        if (PricingOperationPage.ENTRY_AUTOMATIC.equals(form.getSelectedText("downTypeId").trim())) {
            snapshot.put("items", String.join(", ", page.chosenItems().stream().sorted().toList()));
        }
        // Scrolls the grid sideways, so cells out of view are read too.
        page.readListRows(LINE_CELLS).values().forEach(cells -> {
            String line = "line[" + cells.getOrDefault("itemId", "") + " / " + cells.getOrDefault("uomId", "") + "].";
            for (String cell : LINE_CELLS.subList(2, LINE_CELLS.size())) {
                snapshot.put(line + cell, normalized(cells.getOrDefault(cell, "")));
            }
        });
        return snapshot;
    }

    /**
     * A number rounded to 2 decimals, without "%" ("38.020000" → "38.02",
     * "-89.36 %" → "-89.36": QC 2026-11-22, نسبة هامش الربح shows "%" only after
     * reload); other text trimmed.
     */
    private static String normalized(String value) {
        String text = value == null ? "" : value.trim();
        String digits = text.replaceAll("[\\s%]", "");
        if (!digits.matches("-?\\d+(\\.\\d+)?")) {
            return text;
        }
        String number = new BigDecimal(digits).setScale(2, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
        return number;
    }

    /** Compares {@link #formSnapshot()} now with {@code before}; lists every field that differs. */
    private void assertKeptAfterReload(SoftAssert softly, Map<String, String> before, String code) {
        Map<String, String> after = formSnapshot();
        java.util.Set<String> fields = new java.util.TreeSet<>(before.keySet());
        fields.addAll(after.keySet());
        List<String> changed = fields.stream()
                .filter(f -> !java.util.Objects.equals(before.get(f), after.get(f)))
                .map(f -> f + ": [" + before.get(f) + "] → [" + after.get(f) + "]")
                .toList();
        log.info("Form of {} before save: {}", code, before);
        softly.assertTrue(changed.isEmpty(), "Fields of " + code + " changed after save and reload:\n  "
                + String.join("\n  ", changed));
    }

    /** item name → "رمز الصنف" of every line. */
    private Map<String, String> itemCodesByName() {
        Map<String, String> codes = new java.util.TreeMap<>();
        for (int row = 0; row < page.lineCount(); row++) {
            codes.put(page.lineText(row, "itemId"), page.lineText(row, ITEM_CODE_COLUMN));
        }
        return codes;
    }

    private List<BigDecimal> waitForPricesToChange(List<BigDecimal> base) {
        long end = System.currentTimeMillis() + 10_000;
        List<BigDecimal> now = page.pricesAfter();
        while (sameValues(now, base) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            now = page.pricesAfter();
        }
        return now;
    }

    private static boolean close(BigDecimal a, BigDecimal b) {
        return a.subtract(b).abs().compareTo(TOLERANCE) <= 0;
    }

    private static boolean sameValues(List<BigDecimal> a, List<BigDecimal> b) {
        if (a.size() != b.size()) {
            return false;
        }
        for (int i = 0; i < a.size(); i++) {
            if (!close(a.get(i), b.get(i))) {
                return false;
            }
        }
        return true;
    }

    /** A Draft (recordStatus 0); created when QC has none. */
    private Map<String, Object> openDraft() {
        java.util.Optional<Map<String, Object>> opened = tryOpenRandom(PricingOperationPage::isDraft);
        if (opened.isPresent()) {
            page.waitForStatusLabel(DRAFT.label());
            return opened.get();
        }
        log.info("No Draft pricing operation in QC → creating one.");
        return createDocument();
    }
}
