package com.erp.tests.sales;

import com.erp.automation.base.SalesBaseTest;
import com.erp.automation.data.DocumentStatus;
import com.erp.automation.pages.components.DocumentForm;
import com.erp.automation.pages.purchasing.PurchasingDocumentPage;
import com.erp.automation.pages.sales.SalesPage;
import com.erp.automation.pages.sales.SalesQuotationPage;
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
import java.util.HashMap;
import java.util.HashSet;
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
 * Sales Price Quotation (المبيعات → العمليات → عرض السعر), level 1.
 *
 * <p>Built on the same template as the Purchasing suite ({@code QuotationTest}):
 * test data in {@link TestData}, each test reads as the business scenario,
 * steps in "Reusable steps" at the bottom, and every action ends with a check
 * of its result.</p>
 *
 * <p>Groups (run one group with {@code -Dgroups=...}):</p>
 * <ul>
 *     <li>{@code navigation} – the path to the screen and the list before a quotation is opened;</li>
 *     <li>{@code create} – New / Add / Copy, with lines downloaded ("انزال من"), from Excel;</li>
 *     <li>{@code edit} – edit by status, Undo, values kept after save;</li>
 *     <li>{@code status} / {@code cancel} – delete, approve, disapprove, cancel;</li>
 *     <li>{@code validation} – toolbar per status, required fields, sales-period warning;</li>
 *     <li>{@code search} / {@code pagination} / {@code list} – list search, record search and
 *     navigation, pager, Kanban, group by, row actions;</li>
 *     <li>{@code print} / {@code excel} – print preview, PDF, export;</li>
 *     <li>{@code follow-up} – Items Pricing, Print Barcode, Create Sales Order, send for signature;</li>
 *     <li>{@code attachments}, {@code exit}, {@code log}, {@code lines};</li>
 *     <li>{@code sales} – what only Sales has: "مميز" tab, "الشروط والاحكام", "تعميم خصم على
 *     الأصناف", "قالب عرض السعر", "المستند المرجعي" as free text, delivery address.</li>
 * </ul>
 *
 * <p>Screen facts used here were checked in QC on 2026-11-12 (see
 * {@link SalesQuotationPage}). The most important one: the FORM shows
 * "منتهي" as soon as "تاريخ الانتهاء" is before today, while the list and the
 * API still say مسودة / معتمد. On 2026-11-12 every Draft in QC was expired,
 * so the tests choose documents through the API by status AND expiry date,
 * and create a fresh quotation when none fits.</p>
 */
public class SalesQuotationTest extends SalesBaseTest {

    private static final String SQ = "sales-quotation";

    private SalesQuotationPage page;
    private DocumentForm form;

    @Override
    protected void openScreen(SalesPage salesPage) {
        salesPage.clickPriceQuotations();
        page = new SalesQuotationPage(driver);
        form = new DocumentForm(driver, SalesQuotationPage.PREFIX);
        page.dismissInstallPrompt();
        page.waitForListLoaded();
    }

    // =====================================================================
    // Test data
    // =====================================================================

    private static final class TestData {

        static final String QUANTITY = "3";
        static final String PRICE = "100";
        static final String LINE_DISCOUNT_RATE = "5";

        /**
         * The other editable cells of a new line (both tabs). Discount and addition are
         * pairs: one line gets the rates (the grid fills the values), the next one the
         * values (the grid fills the rates), alternately. 3 × 100 = 300, so 10 and 5 fit.
         */
        static final String LINE_DISCOUNT_VALUE = "10";
        static final String LINE_EXTRA_RATE = "2";
        static final String LINE_EXTRA_VALUE = "5";
        static final String LINE_FREE_QUANTITY = "1";

        static String lineNote() {
            return "بيان سطر آلي " + LocalDateTime.now().format(STAMP);
        }

        static final String SAVE_SUCCESS_MESSAGE = "تم الحفظ بنجاح";

        /** Title of the system's live notifications (e.g. an item reached its reorder level). */
        static final String NOTIFICATION_TITLE = "إشعار جديد";

        /** A new quotation is a Draft, or Approved when the settings approve on save. */
        static final DocumentStatus[] NEW_QUOTATION_STATUSES = {DRAFT, APPROVED};

        /**
         * "تاريخ الانتهاء" of a new quotation. QC 2026-11-12: a date later than
         * today + 20 days ("مهلة البيع" in the settings) asks for confirmation,
         * so the normal tests stay well inside it.
         */
        static final int EXPIRY_IN_DAYS = 10;

        /** Beyond the sales period of the settings (today + 20 on 2026-11-12). */
        static final int EXPIRY_BEYOND_SALES_PERIOD_IN_DAYS = 45;
        static final String SALES_PERIOD_WARNING = "يتجاوز الموعد المحدد لمهلة البيع";

        static final List<String> REQUIRED_HEADER_FIELDS = List.of(
                "partnerId", "currencyId", "branchId", "warehouseId", "taxCalculationMethode");

        /** Marked "هذا الحقل مطلوب" on an empty save (QC 2026-11-12). */
        static final List<String> MARKED_REQUIRED_ON_EMPTY_SAVE = List.of("partnerId", "branchId", "warehouseId");
        static final String NO_LINES_ERROR = "يجب اضافة صنف واحد على الأقل";

        /** Header values compared after save + reload ("note" is checked by saveAndVerify). */
        static final List<String> PERSISTED_HEADER_FIELDS = List.of(
                "partnerId", "currencyId", "paymentTerms", "salesRepresentativeId", "saleTeamId",
                "branchId", "warehouseId", "costCenterId", "taxCalculationMethode", "priceListId",
                "prices", "quotationDate", "quotationExpiryDate", "deliveryAddress");

        /** Header values an Undo must restore. */
        static final List<String> UNDO_HEADER_FIELDS = List.of(
                "partnerId", "currencyId", "paymentTerms", "salesRepresentativeId", "saleTeamId",
                "branchId", "warehouseId", "costCenterId", "taxCalculationMethode", "priceListId",
                "prices", "quotationDate", "deliveryAddress", "note");

        /** List columns (col-id) seen in QC on 2026-11-12. */
        static final List<String> LIST_COLUMNS = List.of(
                "code", "salesRepresentativeName", "partnerName", "quotationDate", "quotationExpiryDate",
                "total", "forignTotal", "currencyName", "conversionRate", "priceListName", "recordStatus");

        /** Excel file with item lines for sales quotations (provided by QC, 2026-11-12). */
        static final Path EXCEL_LINES = Paths.get(System.getProperty("user.dir"),
                "test-data", "import-excel", "PriceQuotation.xlsx");

        private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

        /** Unique description, so a persisted value can be told apart from old data. */
        static final String NOTE_PREFIX = "عرض سعر مبيعات آلي ";

        static String uniqueNote() {
            return NOTE_PREFIX + LocalDateTime.now().format(STAMP);
        }

        static String deliveryAddress() {
            return "عنوان توصيل آلي " + LocalDateTime.now().format(STAMP);
        }

        static String referenceText() {
            return "REF-SQ-" + LocalDateTime.now().format(STAMP);
        }

        /** Parts of the API calls that prove an action reached the server (matched case-insensitively). */
        static final String API_CANCEL = "cancel";
        static final String API_APPROVE_OR_DISAPPROVE = "approve";

        static final String GROUP_BY_STATUS = "الحالة";

        /** Kanban label of a Draft (QC 2026-11-12: "غير معتمد"). */
        static final String DISAPPROVED_CARD_LABEL = "غير معتمد";

        static final String LOG_CREATED = "انشاء العملية";
        static final String LOG_APPROVED = "اعتماد العملية";

        static final String ATTACHMENT_TITLE = "مرفقات عرض سعر مبيعات تجريبي";
        static final String ATTACHMENT_DESCRIPTION = "وصف مرفقات عرض سعر المبيعات";

        /**
         * New quotation (QC 2026-11-12): attachments, record search and close are usable.
         * "حفظ" is disabled on the empty form and becomes enabled with the first entry.
         */
        static final List<String> ACTIONS_DISABLED_ON_NEW = List.of(
                "copy", "add", "undo", "delete", "print", "approveOrDisapprove", "cancel", "transactionLogs");
        static final List<String> ACTIONS_ENABLED_ON_NEW = List.of("xIcon", "uploadAttachment", "recordSearch");

        static final List<String> ACTIONS_ENABLED_ON_DRAFT = List.of(
                "edit", "add", "copy", "delete", "print", "approveOrDisapprove", "cancel",
                "uploadAttachment", "transactionLogs");

        static final List<String> ACTIONS_ENABLED_ON_APPROVED = List.of(
                "edit", "add", "copy", "print", "approveOrDisapprove", "cancel");
        static final List<String> ACTIONS_DISABLED_ON_APPROVED = List.of("delete");

        static final List<String> ACTIONS_DISABLED_WHEN_CANCELLED = List.of(
                "edit", "delete", "cancel", "approveOrDisapprove", "uploadAttachment");

        /** Toolbar of a Draft in edit mode (QC 2026-11-12, SQ2610-000033). */
        static final List<String> EDITABLE_ON_DRAFT_EDIT = List.of(
                "partnerId", "currencyId", "paymentTerms", "salesRepresentativeId", "saleTeamId",
                "branchId", "warehouseId", "taxCalculationMethode", "priceListId", "note");

        /** Server message when an approved document is linked to other operations. */
        static final String LINKED_ERROR = "مرتبط";

        static final int MAX_LINKED_ATTEMPTS = 5;
        static final int MAX_BRANCH_ATTEMPTS = 5;
        static final int MAX_ITEM_ATTEMPTS = 8;

        private TestData() {
        }
    }

    // =====================================================================
    // A. Path to the screen and the list (before a quotation is opened)
    // =====================================================================

    @Test(priority = 1, groups = {SQ, "navigation"},
            description = "Sales → Operations → عرض السعر opens the quotations list with its columns and \"جديد\"")
    public void userCanOpenSalesQuotationsFromSalesMenu() {

        // loginAndOpenScreen() has already clicked المبيعات → العمليات → عرض السعر.
        Map<String, String> columns = page.listColumns();
        log.info("List URL {} | columns {}", driver.getCurrentUrl(), columns);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().endsWith(SalesQuotationPage.SCREEN_PATH),
                "The menu did not open the quotations list: " + driver.getCurrentUrl());
        softly.assertTrue(page.isNewButtonShown(), "\"جديد\" is not shown on the quotations list.");
        softly.assertTrue(page.visibleRowCount() > 0, "The quotations list shows no rows.");
        for (String column : TestData.LIST_COLUMNS) {
            softly.assertTrue(columns.containsKey(column), "List column [" + column + "] is missing: " + columns);
        }
        softly.assertAll();
    }

    @Test(priority = 2, groups = {SQ, "navigation"},
            description = "\"جديد\" opens an empty quotation: new number, today's date and the default "
                    + "currency, price list and tax method; no status yet")
    public void newButtonOpensEmptyQuotationWithDefaults() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        String code = waitUntilEquals(() -> page.code().isEmpty() ? "" : "loaded", "loaded");

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(code, "loaded", "A new quotation shows no number.");
        softly.assertTrue(page.code().startsWith("SQ"), "Unexpected number of a new quotation: " + page.code());
        softly.assertEquals(page.inputValue("quotationDate"), LocalDate.now().toString(),
                "\"التاريخ\" of a new quotation is not today.");
        softly.assertFalse(form.getSelectedText("currencyId").isEmpty(), "\"العملة\" has no default.");
        softly.assertFalse(form.getSelectedText("priceListId").isEmpty(), "\"قائمة الاسعار\" has no default.");
        softly.assertFalse(form.getSelectedText("taxCalculationMethode").isEmpty(),
                "\"طريقة احتساب الضريبة\" has no default.");
        softly.assertTrue(isEmptySelect("partnerId"), "\"العميل\" must be empty on a new quotation.");
        softly.assertEquals(page.inputValue("approvalDate"), "", "A new quotation must not have an approval date.");
        softly.assertEquals(page.statusLabel(), "", "A new quotation must not show a status.");
        softly.assertTrue(page.isLinesTabActive(SalesQuotationPage.ITEMS_TAB), "\"قائمة الأصناف\" is not the open tab.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 3, groups = {SQ, "navigation", "list"},
            description = "List row ⋯ → \"عرض\" opens that quotation")
    public void userCanOpenQuotationFromListRowActions() {

        List<String> items = page.rowActionItems();
        assertTrue(items.contains("عرض"), "The row menu has no \"عرض\": " + items);

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        page.openFromRowActions(code);
        assertEquals(waitUntilEquals(page::code, code), code, "Row menu → عرض did not open " + code + ".");
    }

    @Test(priority = 4, groups = {SQ, "navigation", "list"},
            description = "Double-click on a list row opens that quotation with the status of the list")
    public void userCanOpenQuotationByDoubleClickOnListRow() {

        Map<String, String> rows = page.statusesOnCurrentPage();
        List<String> codes = new ArrayList<>(rows.keySet());
        String code = codes.get(new Random().nextInt(codes.size()));
        page.openFromListByCode(code);

        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another quotation than " + code + ".");
        // The form shows "منتهي" for an expired quotation while the list shows its stored status.
        Map<String, Object> record = page.api().byCode(code);
        String expected = record != null && SalesQuotationPage.isExpired(record) ? COMPLETED.label() : rows.get(code);
        page.waitForStatusLabel(expected);
    }

    // =====================================================================
    // B. Create
    // =====================================================================

    @Test(priority = 5, groups = {SQ, "create"},
            description = "New quotation from \"جديد\": header in screen order, lines downloaded from "
                    + "another quotation (انزال من / الرقم), then an item line; saved as Draft")
    public void userCanCreateNewSalesQuotation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);

        fillHeader(true);
        addLinesInBothTabs();
        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        Map<String, Object> record = page.api().byCode(code);
        assertTrue(record != null, code + " is not returned by the API after saving.");
    }

    @Test(priority = 6, groups = {SQ, "create"},
            description = "\"إضافة\" on an opened quotation creates a NEW quotation; the opened one is unchanged")
    public void userCanAddNewSalesQuotationFromOpenedOne() {

        Map<String, Object> opened = openRandomQuotation(r -> true);

        page.clickAdd();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(true);
        addLinesInBothTabs();
        String shown = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        verifyNewQuotationSaved(opened, shown, "\"إضافة\"");
    }

    @Test(priority = 7, groups = {SQ, "create"},
            description = "\"تكرار\" (copy) of a quotation, changed and saved as a new one; the source is unchanged")
    public void userCanCopySalesQuotation() {

        Map<String, Object> source = openRandomQuotation(r -> !SalesQuotationPage.isCancelled(r));

        page.clickCopy();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addLinesInBothTabs();
        String shown = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);

        verifyNewQuotationSaved(source, shown, "\"تكرار\"");
    }

    @Test(priority = 8, groups = {SQ, "create", "excel"},
            description = "New quotation whose lines are imported from Excel (⋮ → الاستيراد من Excel)")
    public void userCanCreateSalesQuotationFromExcel() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        if (page.menuItems().stream().noneMatch(i -> i.replaceAll("\\s+", "")
                .equals(SalesQuotationPage.MENU_IMPORT_EXCEL.replaceAll("\\s+", "")))) {
            throw new SkipException("\"" + SalesQuotationPage.MENU_IMPORT_EXCEL + "\" is not in ⋮ of a new quotation.");
        }

        fillHeader(false);
        page.importLinesFromExcel(TestData.EXCEL_LINES);
        int lines = form.gridRowCount();
        log.info("Lines imported from Excel: {}", lines);
        assertTrue(lines > 0, "Excel import finished but no lines were added (file " + TestData.EXCEL_LINES + ").");

        addLinesInBothTabs();
        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
    }

    @Test(priority = 9, groups = {SQ, "create", "lines"},
            description = "\"انزال من\" = عروض أسعار المبيعات → random quotation: its lines are added to the new one")
    public void userCanDownloadLinesFromAnotherSalesQuotation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectCustomerBranchAndWarehouse();

        int before = form.gridRowCount();
        String number = page.downloadLinesFromAnotherQuotation();
        int after = form.gridRowCount();
        log.info("انزال من {}: {} → {} line(s).", number, before, after);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(form.getSelectedText("downloadFrom"), SalesQuotationPage.DOWNLOAD_SOURCE,
                "\"انزال من\" does not show " + SalesQuotationPage.DOWNLOAD_SOURCE + ".");
        softly.assertEquals(page.inputValue("referenceField").trim(), number, "\"الرقم\" does not show " + number + ".");
        softly.assertTrue(after > before, "No lines were downloaded from " + number + ".");
        softly.assertAll();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
    }

    // =====================================================================
    // C. Edit / Undo
    // =====================================================================

    @Test(priority = 10, groups = {SQ, "edit"},
            description = "Edit a Draft: every editable header field and a new line; it stays a Draft")
    public void userCanEditDraftSalesQuotation() {

        openActiveDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        addLinesInBothTabs();
        saveAndVerify(DRAFT);
    }

    @Test(priority = 11, groups = {SQ, "edit", "validation"},
            description = "Draft in edit mode: the agreed fields are editable; number, rate, approval date, "
                    + "tax number are not; \"انزال من\" is not offered")
    public void draftEditModeHasExpectedEditableFields() {

        openActiveDraft();
        String code = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        form.waitUntilFieldEditable("note");

        SoftAssert softly = new SoftAssert();
        for (String field : TestData.EDITABLE_ON_DRAFT_EDIT) {
            softly.assertTrue(page.isInputEditable(field), "\"" + field + "\" must be editable on Draft " + code + ".");
        }
        for (String field : SalesQuotationPage.READ_ONLY_FIELDS) {
            softly.assertFalse(page.isInputEditable(field), "\"" + field + "\" must be read-only on Draft " + code + ".");
        }
        softly.assertFalse(page.hasField("downloadFrom", Duration.ofSeconds(1)),
                "\"انزال من\" must not be offered when editing " + code + ".");
        // QC 2026-11-12 (SQ2610-000033): "تاريخ الانتهاء", "طريقة الشحن", "قالب عرض السعر" and
        // "المستند المرجعي" are locked in edit mode. Whether that is intended is asked to the
        // analysis team; logged only.
        for (String field : List.of("quotationExpiryDate", "shippingMethodId", "salesQuotationTemplateId", "referenceDocument")) {
            log.info("Edit {}: '{}' is {}", code, field, page.isInputEditable(field) ? "editable" : "locked");
        }
        softly.assertAll();
        page.closeForm();
    }

    @Test(priority = 12, groups = {SQ, "edit", "status"},
            description = "Approved quotation: only the description can be edited; it stays Approved")
    public void approvedSalesQuotationAllowsDescriptionEditOnly() {

        openActiveApproved(false);
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_APPROVED);
        form.waitUntilFieldEditable("note");

        List<String> editable = form.editableFieldsAmong(SalesQuotationPage.SELECT_FIELDS);
        assertTrue(editable.isEmpty(),
                "Only the description may change on an Approved quotation, but these are editable: " + editable);
        assertFalse(page.isInputEditable("quotationDate"), "\"التاريخ\" must be locked on an Approved quotation.");

        saveAndVerify(APPROVED);
    }

    @Test(priority = 13, groups = {SQ, "edit", "status"},
            description = "Expired Draft (منتهي): edit the header and save; it stays منتهي")
    public void userCanEditExpiredDraftSalesQuotation() {

        // QC 2026-11-12: edit is enabled on expired Drafts (SQ2608-000016). Whether
        // that is intended is asked to the analysis team; the result is checked.
        openRandomQuotation(r -> SalesQuotationPage.RecordStatus.DRAFT.of(r) && SalesQuotationPage.isExpired(r));
        String code = page.waitForCode();
        page.waitForStatusLabel(COMPLETED.label());
        if (!page.waitForActionEnabled("edit")) {
            throw new SkipException("\"تعديل\" is disabled on expired quotation " + code + ".");
        }
        page.clickEdit();
        waitForEditMode();

        changeHeader();
        fillZeroQuantitiesAndPrices();
        saveAndVerify(COMPLETED, DRAFT);
    }

    @Test(priority = 14, groups = {SQ, "edit"},
            description = "Edit a Draft (all fields + a line), then Undo restores the saved values")
    public void userCanUndoSalesQuotationChanges() {

        openActiveDraft();
        String code = page.waitForCode();
        Map<String, String> saved = savedHeaderSnapshot();
        int savedLines = page.lineCount();
        log.info("Saved values of {}: {}, {} line(s).", code, saved, savedLines);

        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        changeHeader();
        addItemLine();

        assertTrue(page.waitForActionEnabled("undo"), "\"تراجع\" stayed disabled after editing " + code + ".");
        String id = driver.getCurrentUrl().replaceAll(".*/(\\d+).*", "$1");
        page.clickUndo();
        page.confirmDialog();

        assertTrue(driver.getCurrentUrl().matches(".*/" + id + "([/?#].*)?$"),
                "Undo left quotation " + code + " (id " + id + "): " + driver.getCurrentUrl());
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(page::code, code), code, "Undo did not restore the number of " + code + ".");
        saved.forEach((field, value) -> softly.assertEquals(
                waitUntilEquals(() -> page.readHeaderValues(List.of(field)).get(field), value), value,
                "Undo did not restore \"" + field + "\" of " + code + "."));
        softly.assertEquals(waitUntilEquals(() -> String.valueOf(page.lineCount()), String.valueOf(savedLines)),
                String.valueOf(savedLines), "Undo did not remove the added line of " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 15, groups = {SQ, "edit"},
            description = "Header values of an edited Draft are the same after save and reload")
    public void editedDraftSalesQuotationHeaderIsKeptAfterSave() {

        openActiveDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        changeHeader();
        fillZeroQuantitiesAndPrices();
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
    // D. Status actions: delete, approve, disapprove, cancel
    // =====================================================================

    @Test(priority = 16, groups = {SQ, "status"},
            description = "Delete a Draft: success message, and it is no longer in the list nor in the API")
    public void userCanDeleteDraftSalesQuotation() {

        openActiveDraft();
        String code = page.waitForCode();
        assertTrue(page.waitForActionEnabled("delete"), "\"حذف\" is disabled on Draft " + code + ".");

        page.clickDelete();
        page.confirmDialog();
        DocumentForm.Toast toast = form.waitForToast();
        assertTrue(toast.isSuccess(), "Deleting " + code + " failed: " + toast);

        page.closeForm();
        page.waitForListLoaded();
        page.searchList(code);
        assertFalse(page.waitUntilListShows(code), code + " is still listed after deletion: " + page.visibleListCodes());
        assertTrue(page.api().byCode(code) == null, code + " is still returned by the API after deletion.");
    }

    @Test(priority = 17, groups = {SQ, "status", "log"},
            description = "Approve a Draft: Approved after reload, approval date today, delete disabled, "
                    + "one approval log entry, ⋮ \"انشاء امر بيع\" enabled")
    public void userCanApproveDraftSalesQuotation() {

        openActiveDraft();
        String code = page.waitForCode();
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE, this::toggleApproval);
        assertAccepted("Approving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
        page.waitForStatusLabel(APPROVED.label());

        page.reload();
        page.waitForStatusLabel(APPROVED.label());
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitUntilEquals(() -> page.inputValue("approvalDate"), LocalDate.now().toString()),
                LocalDate.now().toString(), "\"تاريخ اعتماد العرض\" of " + code + " is not today.");
        softly.assertFalse(page.isActionEnabled("delete"), "Approved " + code + " must not be deletable.");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Approving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        if (!logAfter.isEmpty()) {
            softly.assertEquals(logAfter.get(logAfter.size() - 1), TestData.LOG_APPROVED,
                    "The last log entry of " + code + " is not the approval.");
        }
        softly.assertTrue(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be enabled on Approved " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 18, groups = {SQ, "status", "log"},
            description = "Disapprove an Approved quotation not turned into an order: back to Draft, "
                    + "draft actions enabled, one new log entry")
    public void userCanDisapproveApprovedSalesQuotation() {

        openActiveApproved(true);
        String code = page.waitForCode();
        List<String> logBefore = page.readTransactionLog();

        ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE, this::toggleApproval);
        if (outcome.mentions(TestData.LINKED_ERROR)) {
            throw new SkipException(code + " is linked to other operations: " + outcome);
        }
        assertAccepted("Disapproving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
        page.waitForStatusLabel(DRAFT.label());

        page.reload();
        page.waitForStatusLabel(DRAFT.label());
        page.waitForActionEnabled("edit");
        List<String> logAfter = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on disapproved " + code + ".");
        }
        softly.assertEquals(page.inputValue("approvalDate"), "", "Disapproved " + code + " still has an approval date.");
        softly.assertEquals(logAfter.size(), logBefore.size() + 1,
                "Disapproving " + code + " did not add exactly one log entry. Before " + logBefore + ", after " + logAfter);
        softly.assertAll();
    }

    @Test(priority = 19, groups = {SQ, "cancel"},
            description = "Cancel a Draft: Cancelled after reload; edit/delete/cancel/approve/attachments disabled")
    public void userCanCancelDraftSalesQuotation() {

        openActiveDraft();
        String code = page.waitForCode();
        assertAccepted("Cancelling " + code, TestData.API_CANCEL, cancelOpenedQuotation());
        verifyCancelled(code);
    }

    @Test(priority = 20, groups = {SQ, "cancel"},
            description = "Cancel an Approved quotation not turned into an order: Cancelled after reload")
    public void userCanCancelApprovedSalesQuotation() {

        openActiveApproved(true);
        String code = page.waitForCode();
        ActionOutcome outcome = cancelOpenedQuotation();
        if (outcome.mentions(TestData.LINKED_ERROR)) {
            throw new SkipException(code + " is linked to other operations: " + outcome);
        }
        assertAccepted("Cancelling " + code, TestData.API_CANCEL, outcome);
        verifyCancelled(code);
    }

    @Test(priority = 21, groups = {SQ, "cancel", "validation"},
            description = "Approved quotation already turned into a sales order (isInvoice): "
                    + "disapprove and cancel are disabled, ⋮ \"انشاء امر بيع\" disabled")
    public void linkedApprovedSalesQuotationCannotBeCancelledOrDisapproved() {

        // Nothing is clicked. QC 2026-11-12 (SQ2611-000039, isInvoice): "اعتماد/عدم اعتماد" is
        // disabled but "إلغاء" is ENABLED → this test is expected to fail on "cancel" until
        // the analysis team confirms the rule.
        //
        // The quotation is chosen through the API: a sales order (not cancelled) whose
        // "referenceDocument" is the quotation's number (QC 2026-11-12: SO2608-000001 →
        // SQ2608-000002). Only approved quotations whose form shows "معتمد" are used.
        Map<String, String> orderOfQuotation = new HashMap<>();
        new com.erp.automation.ui.DocumentApi(driver, com.erp.automation.pages.sales.SalesOrderPage.API_RESOURCE)
                .find(o -> !com.erp.automation.ui.DocumentApi.isTrue(o, "isCanceled"))
                .forEach(o -> {
                    String quotation = com.erp.automation.ui.DocumentApi.textOf(o, "referenceDocument").trim();
                    if (quotation.startsWith("SQ")) {
                        orderOfQuotation.putIfAbsent(quotation, com.erp.automation.ui.DocumentApi.textOf(o, "code"));
                    }
                });
        log.info("Quotations with a sales order (API): {}", orderOfQuotation);
        if (orderOfQuotation.isEmpty()) {
            throw new SkipException("No sales order in QC refers to a sales quotation (\"referenceDocument\" = SQ…).");
        }
        Map<String, Object> record = page.openRandomDocument(r -> SalesQuotationPage.isActiveApproved(r)
                        && orderOfQuotation.containsKey(SalesQuotationPage.codeOf(r)))
                .orElseThrow(() -> new SkipException("No approved (not expired) sales quotation has a sales order: "
                        + orderOfQuotation.keySet()));
        page.waitForStatusLabel(APPROVED.label());
        String code = page.waitForCode();
        String order = orderOfQuotation.get(code);
        log.info("Linked quotation {} ← sales order {}", code, order);
        page.waitForActionEnabled("print");

        SoftAssert softly = new SoftAssert();
        for (String action : List.of("cancel", "approveOrDisapprove")) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' is enabled on approved " + code
                    + ", which already has sales order " + order + " (isInvoice: " + record.get("isInvoice") + ").");
        }
        softly.assertFalse(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be disabled on " + code
                        + ", which already has a sales order.");
        softly.assertAll();
    }

    // =====================================================================
    // E. Toolbar per status
    // =====================================================================

    @Test(priority = 22, groups = {SQ, "validation"},
            description = "New quotation toolbar: save/close/attachments/record search enabled; "
                    + "copy/add/undo/delete/print/approve/cancel/log disabled; ⋮ has the Excel import")
    public void newSalesQuotationEnablesOnlyAllowedActions() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.waitForActionEnabled("xIcon");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_ON_NEW) {
            log.info("New quotation: '{}' {}", action,
                    page.isActionEnabled(action) ? "ENABLED" : (page.hasAction(action) ? "disabled" : "not shown"));
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must not be usable on a new quotation.");
        }
        for (String action : TestData.ACTIONS_ENABLED_ON_NEW) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on a new quotation.");
        }
        List<String> menu = page.menuItems();
        softly.assertTrue(menu.stream().anyMatch(i -> i.contains("الاستيراد")), "⋮ of a new quotation has no Excel import: " + menu);
        softly.assertFalse(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be disabled on a new quotation.");

        // "حفظ": disabled while the form is empty, enabled after the first entry.
        log.info("New quotation: 'save' {} while empty", page.isActionEnabled("save") ? "ENABLED" : "disabled");
        selectIfEditable("partnerId");
        softly.assertTrue(page.waitForActionEnabled("save"),
                "'save' stayed disabled after choosing a customer on a new quotation.");
        softly.assertAll();

        page.closeForm(); // confirms "leave without saving?" by itself
    }

    @Test(priority = 23, groups = {SQ, "validation", "status"},
            description = "Draft: edit/add/copy/delete/print/approve/cancel enabled, no approval date, "
                    + "⋮ \"انشاء امر بيع\" disabled")
    public void draftSalesQuotationEnablesEditActions() {

        openActiveDraft();
        String code = page.waitForCode();
        page.waitForActionEnabled("edit");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_ENABLED_ON_DRAFT) {
            softly.assertTrue(page.isActionEnabled(action), "'" + action + "' must be enabled on Draft " + code + ".");
        }
        softly.assertFalse(page.isActionEnabled("save"), "'save' must be disabled in view mode (" + code + ").");
        softly.assertEquals(page.inputValue("approvalDate"), "", "Draft " + code + " must not have an approval date.");
        softly.assertFalse(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be disabled on Draft " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 24, groups = {SQ, "validation", "status"},
            description = "Approved: delete disabled; edit/disapprove/cancel enabled; approval date set; "
                    + "⋮ \"انشاء امر بيع\" enabled")
    public void approvedSalesQuotationCannotBeDeleted() {

        openActiveApproved(true);
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
        softly.assertTrue(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be enabled on Approved " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 25, groups = {SQ, "validation", "status"},
            description = "Cancelled quotation is read-only: edit/delete/cancel/approve/attachments disabled")
    public void cancelledSalesQuotationIsReadOnly() {

        // QC 2026-11-12 had no cancelled quotation: one is cancelled first when needed.
        Map<String, Object> record = page.api().randomRecord(SalesQuotationPage::isCancelled);
        String code;
        if (record == null) {
            log.info("No cancelled sales quotation in QC → cancelling a Draft first.");
            openActiveDraft();
            code = page.waitForCode();
            assertAccepted("Cancelling " + code, TestData.API_CANCEL, cancelOpenedQuotation());
            page.reload();
        } else {
            page.openDocument(record.get("id"));
            code = page.waitForCode();
        }
        page.waitForStatusLabel(CANCELLED.label());
        page.waitForActionEnabled("print");

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        softly.assertFalse(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be disabled on cancelled " + code + ".");
        softly.assertAll();
    }

    @Test(priority = 26, groups = {SQ, "validation", "status"},
            description = "Expired quotation (منتهي): approve disabled, ⋮ \"انشاء امر بيع\" disabled")
    public void expiredSalesQuotationCannotBeApproved() {

        // QC 2026-11-12: "اعتماد" is ENABLED on expired Drafts (SQ2608-000016, SQ2610-000033),
        // unlike purchase quotations. This test is expected to fail until the analysis team
        // confirms the rule; "انشاء امر بيع" is disabled as expected.
        openRandomQuotation(r -> SalesQuotationPage.RecordStatus.DRAFT.of(r) && SalesQuotationPage.isExpired(r));
        page.waitForStatusLabel(COMPLETED.label());
        String code = page.waitForCode();
        page.waitForActionEnabled("print");

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(page.isActionEnabled("approveOrDisapprove"), "'approveOrDisapprove' must be disabled on expired " + code + ".");
        softly.assertFalse(page.isMenuItemEnabled(SalesQuotationPage.MENU_CREATE_SALES_ORDER),
                "\"" + SalesQuotationPage.MENU_CREATE_SALES_ORDER + "\" must be disabled on expired " + code + ".");
        for (String action : List.of("edit", "delete", "cancel")) {
            log.info("Expired {}: '{}' is {}", code, action, page.isActionEnabled(action) ? "enabled" : "disabled");
        }
        softly.assertAll();
    }

    @Test(priority = 27, groups = {SQ, "validation", "status"},
            description = "The form shows \"منتهي\" for a quotation whose expiry date has passed, "
                    + "while the list shows its stored status")
    public void expiredQuotationShowsExpiredOnFormOnly() {

        Map<String, Object> record = page.api().randomRecord(SalesQuotationPage::isExpired);
        if (record == null) {
            throw new SkipException("QC has no expired sales quotation.");
        }
        String code = SalesQuotationPage.codeOf(record);
        page.searchList(code);
        assertTrue(page.waitUntilListShows(code), code + " is not found in the list.");
        String listStatus = page.statusesOnCurrentPage().get(code);
        page.openFromListByCode(code);
        String formStatus = waitUntilEquals(page::statusLabel, COMPLETED.label());
        log.info("{} (expiry {}): list [{}], form [{}]", code, SalesQuotationPage.expiryOf(record), listStatus, formStatus);

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(formStatus, COMPLETED.label(), "The form of expired " + code + " does not show منتهي.");
        // Logged as an analysis note: the list keeps مسودة / معتمد for an expired quotation.
        if (!COMPLETED.label().equals(listStatus)) {
            log.warn("ANALYSIS NOTE: the list shows [{}] for expired {} while the form shows منتهي.", listStatus, code);
        }
        softly.assertAll();
    }

    // =====================================================================
    // F. Validation of the form
    // =====================================================================

    @Test(priority = 28, groups = {SQ, "validation"},
            description = "Saving an empty quotation is blocked: error toast, العميل/الفرع/المخزن marked "
                    + "\"هذا الحقل مطلوب\", stays in Add")
    public void userCannotSaveSalesQuotationWithoutRequiredData() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" is disabled on a new quotation.");

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
        softly.assertTrue(toast != null && toast.fullText().contains(TestData.NO_LINES_ERROR),
                "The error does not say that an item is needed: " + toast);
        softly.assertTrue(marked.containsAll(TestData.MARKED_REQUIRED_ON_EMPTY_SAVE),
                "Required fields not marked: expected " + TestData.MARKED_REQUIRED_ON_EMPTY_SAVE + ", marked " + marked);
        softly.assertFalse(form.isDialogOpen(), "The save confirmation must not open while the form is invalid.");
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 29, groups = {SQ, "validation"},
            description = "Header complete but no item line: saving is refused with \"يجب اضافة صنف واحد على الأقل\"")
    public void userCannotSaveSalesQuotationWithoutLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectCustomerBranchAndWarehouse();
        page.setExpiryDate(LocalDate.now().plusDays(TestData.EXPIRY_IN_DAYS));

        page.clickSave();
        DocumentForm.Toast toast = form.waitForToast();
        log.info("Save without lines → {}", toast);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast.isError() && toast.fullText().contains(TestData.NO_LINES_ERROR),
                "Expected \"" + TestData.NO_LINES_ERROR + "\", got: " + toast);
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "The form must stay in Add mode.");
        softly.assertAll();

        page.closeForm();
    }

    @Test(priority = 30, groups = {SQ, "validation"},
            description = "\"تاريخ الانتهاء\" beyond the sales period of the settings asks for confirmation; "
                    + "\"إلغاء\" leaves the quotation unsaved")
    public void expiryBeyondSalesPeriodAsksForConfirmation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        selectCustomerBranchAndWarehouse();
        page.setExpiryDate(LocalDate.now().plusDays(TestData.EXPIRY_BEYOND_SALES_PERIOD_IN_DAYS));
        addItemLine();
        String note = TestData.uniqueNote();
        form.setNote(note);

        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog(); // "هل انت متاكد من عملية الحفظ؟"

        waitUntil("the sales-period warning", Duration.ofSeconds(10), form::isDialogOpen);
        String warning = dialogText();
        log.info("Warning after save: {}", warning);
        assertTrue(warning.contains(TestData.SALES_PERIOD_WARNING),
                "Expected the sales-period warning, got: " + warning);

        cancelOpenDialog();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(form.isInMode(DocumentForm.Mode.ADD), "\"إلغاء\" must keep the quotation unsaved (Add mode).");
        softly.assertTrue(page.api().find(r -> note.equals(String.valueOf(r.get("note")))).isEmpty(),
                "The quotation was saved although the warning was cancelled.");
        softly.assertAll();

        page.closeForm();
    }

    // =====================================================================
    // G. List: search, record search, navigation, pagination, Kanban, group by
    // =====================================================================

    @Test(priority = 31, groups = {SQ, "search"},
            description = "List search by number shows that quotation")
    public void userCanSearchSalesQuotationsListByCode() {

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        page.searchList(code);
        assertTrue(page.waitUntilListShows(code), "Searching for " + code + " did not show it: " + page.visibleListCodes());
    }

    @Test(priority = 32, groups = {SQ, "search"},
            description = "List search for a text no quotation has shows no rows")
    public void listSearchWithoutMatchShowsNoRows() {

        String text = "لا-يوجد-" + System.currentTimeMillis();
        page.searchList(text);
        waitUntil("an empty list for [" + text + "]", Duration.ofSeconds(10), () -> page.visibleListCodes().isEmpty());
        assertTrue(page.visibleListCodes().isEmpty(), "Rows are shown for a text no quotation has: " + page.visibleListCodes());
    }

    @Test(priority = 33, groups = {SQ, "search"},
            description = "Record search (magnifier) in the form opens the quotation typed")
    public void userCanOpenSalesQuotationFromRecordSearch() {

        openRandomQuotation(r -> true);
        String target = page.waitForCode();
        page.clickRecordNavigation(page.isRecordNavigationEnabled("pageFirst") ? "pageFirst" : "pageLast");
        assertNotEquals(waitUntilChanges(target), target, "Could not move away from " + target + ".");

        page.openFromRecordSearch(target);
        assertEquals(waitUntilEquals(page::code, target), target, "Record search did not open " + target + ".");
    }

    @Test(priority = 34, groups = {SQ, "navigation"},
            description = "Record navigation in the form: first / next / back / last")
    public void userCanNavigateBetweenSalesQuotationRecords() {

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

    @Test(priority = 35, groups = {SQ, "pagination"},
            description = "List pager: first/last page, Next to the last page, back to a random page, open a random row")
    public void userCanNavigateSalesQuotationsPagination() {

        SoftAssert softly = new SoftAssert();

        page.goToFirstListPage();
        int[] first = page.paginationRange(); // {from, to, total}
        int total = first[2];
        softly.assertEquals(first[0], 1, "The first page must start at record 1.");
        softly.assertFalse(page.isListPreviousPageEnabled(), "'Previous' must be disabled on the first page.");
        softly.assertEquals(page.visibleRowCount(), first[1] - first[0] + 1,
                "Rows on the first page do not match the summary " + Arrays.toString(first) + ".");
        softly.assertEquals(total, page.api().count(), "The list total differs from the API.");

        page.goToLastListPage();
        int[] last = page.paginationRange();
        softly.assertEquals(last[1], total, "The last page must end at the last record.");
        softly.assertFalse(page.isListNextPageEnabled(), "'Next' must be disabled on the last page.");
        softly.assertEquals(page.visibleRowCount(), last[1] - last[0] + 1,
                "Rows on the last page do not match the summary " + Arrays.toString(last) + ".");

        page.goToFirstListPage();
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
        }
        softly.assertEquals(page.paginationRange()[1], total, "\"Next\" did not reach the last record.");
        softly.assertEquals(pages, (total + pageSize - 1) / pageSize, "Unexpected number of pages.");

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

        List<String> codes = page.visibleListCodes();
        String code = codes.get(new Random().nextInt(codes.size()));
        log.info("{} page(s); back on page {} {} → opening {}.", pages, target, Arrays.toString(chosen), code);
        page.openFromListByCode(code);
        assertEquals(waitUntilEquals(page::code, code), code, "The list opened another quotation than " + code + ".");
    }

    @Test(priority = 36, groups = {SQ, "list"},
            description = "Kanban shows the same quotations as the list page, each with a status; a card opens it")
    public void userCanSwitchSalesQuotationsListToKanbanAndBack() {

        page.waitForListLoaded();
        List<String> gridCodes = page.visibleListCodes();
        try {
            page.switchToKanbanView();
            List<String> cardCodes = page.kanbanCardCodes();
            log.info("List page: {} | Kanban: {}", gridCodes, cardCodes);

            SoftAssert softly = new SoftAssert();
            softly.assertEquals(new HashSet<>(cardCodes), new HashSet<>(gridCodes),
                    "Kanban cards do not show the same quotations as the list page.");
            List<String> labels = new ArrayList<>(Arrays.stream(DocumentStatus.values()).map(DocumentStatus::label).toList());
            labels.add(TestData.DISAPPROVED_CARD_LABEL);
            for (String code : cardCodes) {
                String status = page.kanbanCardStatus(code);
                softly.assertTrue(labels.contains(status), "Card " + code + " has an unknown status [" + status + "].");
            }
            softly.assertAll();

            String target = cardCodes.get(new Random().nextInt(cardCodes.size()));
            page.openKanbanCard(target);
            assertEquals(waitUntilEquals(page::code, target), target, "The card of " + target + " opened another quotation.");
        } finally {
            page.closeForm();
            if (!page.isListViewShown()) {
                page.switchToListView();
            }
        }
    }

    @Test(priority = 37, groups = {SQ, "list"},
            description = "Group by status: one group per known status; the groups add up to the list total")
    public void userCanGroupSalesQuotationsListByStatus() {

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

    // =====================================================================
    // H. Print, PDF, Excel
    // =====================================================================

    @Test(priority = 38, groups = {SQ, "print"},
            description = "Print a random quotation: the preview shows its number and customer; "
                    + "then it is printed through Chrome's print dialog")
    public void userCanPrintSalesQuotation() throws java.awt.AWTException {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        String customer = waitForSelected(form, "partnerId");

        String preview = page.openPrintPreviewWithRandomTemplate();
        SoftAssert softly = new SoftAssert();
        softly.assertTrue(preview.contains(code), "Print preview does not show " + code + ".");
        if (!customer.isBlank()) {
            softly.assertTrue(preview.contains(customer), "Print preview of " + code + " does not show the customer [" + customer + "].");
        }
        softly.assertAll();

        page.printFromPreview();
        page.confirmChromePrintDialog();
        page.closePrintPreview();
        assertEquals(page.waitForCode(), code, "After printing, the form does not show " + code + " any more.");
    }

    @Test(priority = 39, groups = {SQ, "print"},
            description = "Save a random quotation as PDF: the preview shows it; a PDF file is downloaded")
    public void userCanSaveSalesQuotationAsPdf() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        Path dir = page.prepareDownloadFolder("pdf");

        String preview = page.openPrintPreviewWithRandomTemplate();
        assertTrue(preview.contains(code), "The print preview does not show " + code + ".");

        if (!page.hasPreviewPdfButton()) {
            // QC 2026-11-12 (v5.8.9): the preview offers only "طباعة" (and zoom / pages / ✕).
            // Saving as PDF is then done through Chrome's print dialog, which is covered by
            // userCanPrintSalesQuotation. Reported to the analysts as a removed feature.
            page.closePrintPreview();
            throw new SkipException("The print preview of " + code + " has no \"حفظ كـ PDF\" button any more "
                    + "(only طباعة / zoom / pages / ✕).");
        }
        page.savePreviewAsPdf();
        Path pdf = page.waitForDownloadedFile(dir, ".pdf", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(pdf, "%PDF"), pdf.getFileName() + " is not a PDF file.");
    }

    @Test(priority = 40, groups = {SQ, "excel"},
            description = "⋮ → التصدير الى Excel: an .xlsx with the quotation's item lines is downloaded")
    public void userCanExportSalesQuotationToExcel() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        page.waitForGridToSettle();
        int lines = page.lineCount();
        List<String> items = new ArrayList<>();
        for (int row = 0; row < lines; row++) {
            items.add(page.lineText(row, "code"));
        }
        Path dir = page.prepareDownloadFolder("excel");

        page.clickMenuItem(SalesQuotationPage.MENU_EXPORT_EXCEL);

        Path xlsx = page.waitForDownloadedFile(dir, ".xlsx", Duration.ofSeconds(60));
        assertTrue(Downloads.fileStartsWith(xlsx, "PK"), xlsx.getFileName() + " is not an .xlsx (zip) file.");
        String content = Downloads.xlsxText(xlsx);
        log.info("Excel of {}: {} row(s); {} line(s): {}", code, Downloads.xlsxRowCount(xlsx), lines, items);

        SoftAssert softly = new SoftAssert();
        for (String item : items) {
            softly.assertTrue(content.contains(item), "Item [" + item + "] of " + code + " is missing in the Excel file.");
        }
        softly.assertAll();
    }

    // =====================================================================
    // I. Follow-up: Items Pricing, Print Barcode, Create Sales Order, send for signature
    // =====================================================================

    @Test(priority = 41, groups = {SQ, "follow-up"},
            description = "Items Pricing ($): Sales → Pricing opens with the quotation's items")
    public void userCanOpenItemsPricingFromSalesQuotation() {

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

    @Test(priority = 42, groups = {SQ, "follow-up", "print"},
            description = "Print Barcode: Item Barcodes opens with the quotation's items")
    public void userCanOpenPrintBarcodeFromSalesQuotation() {

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

    @Test(priority = 43, groups = {SQ, "follow-up"},
            description = "Approved quotation → ⋮ → انشاء امر بيع: a new sales order that refers to it, "
                    + "with its customer, branch and items (not saved)")
    public void userCanCreateSalesOrderFromApprovedSalesQuotation() {

        Map<String, Object> record = openActiveApproved(true);
        page.waitForGridToSettle();
        String code = page.waitForCode();
        String customer = waitForSelected(form, "partnerId");
        String branch = waitForSelected(form, "branchId");
        Set<String> items = new LinkedHashSet<>(page.gridColumnTexts("code"));

        page.clickMenuItemAndFollow(SalesQuotationPage.MENU_CREATE_SALES_ORDER, SalesQuotationPage.SALES_ORDER_ADD_PATH);

        // QC 2026-11-12: /sales/sales-order/add/41, prefix sales-order, المستند المرجعي = SQ2611-000040.
        DocumentForm order = new DocumentForm(driver, SalesQuotationPage.SALES_ORDER_PREFIX);
        assertEquals(waitUntilEquals(() -> page.inputValueById("referenceDocument"), code), code,
                "The new sales order does not refer to " + code + ".");
        page.waitForGridToSettle();
        Set<String> orderItems = new LinkedHashSet<>(page.gridColumnTexts("code"));

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(driver.getCurrentUrl().contains(SalesQuotationPage.SALES_ORDER_ADD_PATH + record.get("id")),
                "The new order is not made from " + code + ": " + driver.getCurrentUrl());
        if (!customer.isEmpty()) {
            softly.assertEquals(waitForSelected(order, "partnerId"), customer, "Customer of the new order differs from " + code + ".");
        }
        if (!branch.isEmpty()) {
            softly.assertEquals(waitForSelected(order, "branchId"), branch, "Branch of the new order differs from " + code + ".");
        }
        softly.assertEquals(orderItems, items, "Items of the new order differ from " + code + ".");
        softly.assertAll();
        // Left unsaved on purpose: the test must not create sales orders in QC.
    }

    @Test(priority = 44, groups = {SQ, "follow-up"},
            description = "⋮ → ارسال للعميل لغرض التوقيع on an Approved quotation opens its dialog (nothing is sent)")
    public void userCanOpenSendForSignatureFromApprovedSalesQuotation() {

        openActiveApproved(true);
        String code = page.waitForCode();
        // QC 2026-11-12: the item was DISABLED on every quotation seen (Draft, Approved,
        // expired). The condition that enables it is asked to the analysis team.
        if (!page.isMenuItemEnabled(SalesQuotationPage.MENU_SEND_FOR_SIGNATURE)) {
            throw new SkipException("\"" + SalesQuotationPage.MENU_SEND_FOR_SIGNATURE + "\" is disabled on Approved " + code + ".");
        }
        page.clickMenuItem(SalesQuotationPage.MENU_SEND_FOR_SIGNATURE);
        waitUntil("the send-for-signature dialog", Duration.ofSeconds(10), form::isDialogOpen);
        log.info("Send for signature on {}: [{}]", code, dialogText());
        cancelOpenDialog();
    }

    // =====================================================================
    // J. Attachments
    // =====================================================================

    @Test(priority = 45, groups = {SQ, "attachments"},
            description = "Attachments added to a Draft are listed and kept after save and reload")
    public void userCanAddAttachmentsToSalesQuotation() {

        openActiveDraft();
        String code = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);

        page.openAttachments();
        int before = page.attachmentCount();
        int files = page.countAttachmentFiles();
        page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
        assertEquals(page.waitForAttachmentCount(before + files), before + files, "The added files are not listed.");
        page.closeAttachments();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(DRAFT);
        page.reload();
        page.openAttachments();
        assertEquals(page.waitForAttachmentCount(before + files), before + files,
                "Attachments of " + code + " were not kept after save and reload.");
    }

    @Test(priority = 46, groups = {SQ, "attachments"},
            description = "Deleting an attachment of a Draft is kept after save and reload")
    public void userCanDeleteAttachmentFromSalesQuotation() {

        openActiveDraft();
        String code = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.openAttachments();
        int before = page.attachmentCount();

        if (before == 0) {
            int files = page.countAttachmentFiles();
            page.addAttachmentFiles(TestData.ATTACHMENT_TITLE, TestData.ATTACHMENT_DESCRIPTION);
            before = page.waitForAttachmentCount(files);
            page.closeAttachments();
            fillZeroQuantitiesAndPrices();
            saveAndVerify(DRAFT);
            page.clickEdit();
            form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
            page.openAttachments();
        }

        String deleted = page.deleteLastAttachment();
        assertEquals(page.waitForAttachmentCount(before - 1), before - 1, "The deleted attachment is still listed.");
        page.closeAttachments();
        page.waitForToastToAppearAndClear();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(DRAFT);
        page.reload();
        page.waitForCode();
        page.openAttachments();
        // Read when the list has stopped changing, and look for the deleted row itself.
        // QC 2026-11-12 (SQ2611-000050): a count read while the rows were still loading
        // (2 of 3) let this test pass, but all 3 attachments were there after reopening.
        List<String> after = page.attachmentRowsWhenLoaded();
        log.info("Attachments of {} after save + reload: {} (deleted: [{}])", code, after, deleted);
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(after.size(), before - 1,
                "The deleted attachment of " + code + " came back after save and reload: " + after);
        // By file (URL), not by the whole row text, which may differ between edit and view mode.
        softly.assertFalse(PurchasingDocumentPage.isAttachmentListed(after, deleted),
                "Attachment [" + PurchasingDocumentPage.attachmentKey(deleted) + "] of " + code
                        + " is still listed after save and reload.");
        softly.assertAll();
    }

    // =====================================================================
    // K. Exit, transaction log
    // =====================================================================

    @Test(priority = 47, groups = {SQ, "exit"},
            description = "Close a new, a viewed and an edited quotation: each returns to the list")
    public void userCanExitSalesQuotation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        page.clickAction("xIcon");
        assertTrue(waitForConfirmDialog(form), "Closing a new quotation did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing a new quotation");

        openRandomQuotation(r -> true);
        String viewed = page.waitForCode();
        page.clickAction("xIcon");
        assertBackOnList("closing " + viewed + " in view mode");

        openActiveDraft();
        String edited = page.waitForCode();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        page.clickAction("xIcon");
        // QC 2026-11-12: "خروج / هل انت متأكد من تجاهل التغييرات التي اجريتها؟" → نعم.
        assertTrue(waitForConfirmDialog(form), "Closing " + edited + " in edit mode did not ask for confirmation.");
        page.confirmDialog();
        assertBackOnList("closing " + edited + " in edit mode");
    }

    @Test(priority = 48, groups = {SQ, "log"},
            description = "Transaction log: first entry is the creation; an Approved quotation has an approval entry")
    public void userCanViewSalesQuotationTransactionLog() {

        Map<String, Object> record = openRandomQuotation(r -> true);
        String code = page.waitForCode();
        List<String> actions = page.readTransactionLog();

        SoftAssert softly = new SoftAssert();
        softly.assertFalse(actions.isEmpty(), "Transaction log of " + code + " is empty.");
        softly.assertEquals(actions.isEmpty() ? "" : actions.get(0), TestData.LOG_CREATED,
                "The first log entry of " + code + " must be the creation.");
        if (SalesQuotationPage.RecordStatus.APPROVED.of(record)) {
            softly.assertTrue(actions.contains(TestData.LOG_APPROVED), "Approved " + code + " has no approval entry: " + actions);
        }
        softly.assertAll();
    }

    // =====================================================================
    // L. Lines
    // =====================================================================

    @Test(priority = 49, groups = {SQ, "lines"},
            description = "A new quotation keeps its lines (item, quantity, price) after save and reload")
    public void savedSalesQuotationKeepsItsLines() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        assertEquals(page.selectPricesExcludingTax(), "prices-0", "\"باستثناء الضريبة\" could not be chosen.");
        addItemLine();
        addItemLine();
        page.deleteEmptyLines();
        List<String> before = linesSnapshot();

        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        List<String> after = linesSnapshot();

        assertEquals(after.stream().sorted().toList(), before.stream().sorted().toList(), "Lines changed after save and reload.");
    }

    @Test(priority = 50, groups = {SQ, "lines", "edit"},
            description = "Draft: changing the quantity, the price and the discount % of an existing line is saved")
    public void userCanEditExistingLineOfSalesQuotation() {

        openActiveDraft();
        page.clickEdit();
        form.waitForMode(DocumentForm.Mode.EDIT_DRAFT);
        fillZeroQuantitiesAndPrices();
        if (page.lineCount() == 0) {
            addItemLine();
        }

        String item = page.lineText(0, "code");
        BigDecimal quantity = page.lineNumber(0, "quantity").add(BigDecimal.ONE);
        BigDecimal price = page.lineNumber(0, "price").add(BigDecimal.TEN);
        page.setLineNumber(0, "quantity", quantity.toPlainString());
        page.setLineNumber(0, "price", price.toPlainString());
        page.setLineNumber(0, "discountRate", TestData.LINE_DISCOUNT_RATE);
        page.stopCellEditing();
        log.info("Line 1 ({}) quantity → {}, price → {}, discount % → {}", item, quantity, price, TestData.LINE_DISCOUNT_RATE);

        saveAndVerify(DRAFT);
        page.reload();
        page.waitForGridToSettle();

        int row = page.lineTexts("code").indexOf(item);
        assertTrue(row >= 0, "Item " + item + " is not in the lines after save.");
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.lineNumber(row, "quantity").compareTo(quantity), 0, "Quantity of " + item + " was not saved.");
        softly.assertEquals(page.lineNumber(row, "price").compareTo(price), 0, "Price of " + item + " was not saved.");
        softly.assertEquals(page.lineNumber(row, "discountRate").compareTo(new BigDecimal(TestData.LINE_DISCOUNT_RATE)), 0,
                "Discount % of " + item + " was not saved.");
        softly.assertAll();
    }

    @Test(priority = 51, groups = {SQ, "lines"},
            description = "New quotation: deleting an item line is saved")
    public void userCanDeleteItemLineOfSalesQuotation() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        addItemLine();
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
    // M. What only Sales has
    // =====================================================================

    @Test(priority = 52, groups = {SQ, "sales", "lines"},
            description = "\"مميز\" tab: an optional item, ticked and moved with \"إضافة الأصناف المحددة إلى قائمة "
                    + "الأصناف\", leaves \"مميز\" and appears in \"قائمة الأصناف\"; it is kept after save")
    public void userCanMoveOptionalItemsToItemsList() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        int itemsBefore = itemLineCount();

        page.openFeaturesTab();
        assertTrue(driver.getCurrentUrl().contains("tab=features"), "\"مميز\" did not open: " + driver.getCurrentUrl());
        assertFalse(page.isMoveFeaturesEnabled(), "The move button must be disabled while nothing is ticked.");
        String item = addItemLineInOpenTab();
        page.tickLine(0);
        assertTrue(page.isMoveFeaturesEnabled(), "The move button stayed disabled after ticking a line.");

        page.moveTickedFeaturesToItems();
        DocumentForm.Toast toast = form.waitForToast();
        // Lines WITH an item: the empty line the grid added after Enter stays in "مميز"
        // (QC 2026-11-12: it was counted as "the moved line is still there").
        int featuresAfter = itemLineCount();
        page.openItemsTab();
        List<String> items = page.lineTexts("code");
        log.info("Moved {} → toast [{}]; مميز {} line(s); قائمة الأصناف {}", item, toast, featuresAfter, items);

        SoftAssert softly = new SoftAssert();
        softly.assertTrue(toast.isSuccess(), "Moving the optional item failed: " + toast);
        softly.assertEquals(featuresAfter, 0, "The moved line is still in \"مميز\".");
        softly.assertEquals(itemLineCount(), itemsBefore + 1, "\"قائمة الأصناف\" did not get the moved line.");
        softly.assertTrue(items.contains(item), "Item " + item + " is not in \"قائمة الأصناف\": " + items);
        softly.assertAll();

        fillZeroQuantitiesAndPrices();
        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        page.waitForGridToSettle();
        assertTrue(page.lineTexts("code").contains(item), "The moved item " + item + " was not kept after save.");
    }

    @Test(priority = 53, groups = {SQ, "sales", "lines"},
            description = "\"مميز\" tab: an optional item that is NOT moved is saved in \"مميز\" only")
    public void optionalItemIsSavedInFeaturesTab() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        List<String> items = linesSnapshot();

        page.openFeaturesTab();
        String optional = addItemLineInOpenTab();
        fillZeroQuantitiesAndPrices();
        page.openItemsTab();

        saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        page.waitForGridToSettle();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(linesSnapshot().stream().sorted().toList(), items.stream().sorted().toList(),
                "\"قائمة الأصناف\" changed after saving an optional item.");
        page.openFeaturesTab();
        softly.assertTrue(page.lineTexts("code").contains(optional),
                "Optional item " + optional + " was not kept in \"مميز\": " + page.lineTexts("code"));
        softly.assertAll();
    }

    @Test(priority = 54, groups = {SQ, "sales"},
            description = "\"الشروط والاحكام\" → \"عرض\" shows the terms text")
    public void userCanShowTermsAndConditions() {

        openRandomQuotation(r -> true);
        String code = page.waitForCode();
        String terms = page.showTermsAndConditions();
        log.info("Terms of {}: [{}]", code, terms);
        assertFalse(terms.isBlank(), "\"الشروط والاحكام\" → \"عرض\" showed no text on " + code + ".");
    }

    @Test(priority = 55, groups = {SQ, "sales"},
            description = "\"تعميم خصم على الأصناف\" (نسبة %) is saved with the quotation")
    public void userCanApplyBulkDiscountPercentage() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        assertTrue(page.setBulkDiscount(SalesQuotationPage.DISCOUNT_PERCENT, "10"),
                "\"تعميم خصم على الأصناف\" could not be set.");
        page.waitForGridToSettle();
        log.info("Line discount after the bulk discount: {}", page.lineTexts("discountRate"));

        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(waitForSelected(form, "bulkDiscountType"), SalesQuotationPage.DISCOUNT_PERCENT,
                "The discount type of " + code + " was not kept.");
        softly.assertEquals(new BigDecimal(page.bulkDiscountAmount().isBlank() ? "0" : page.bulkDiscountAmount())
                .compareTo(BigDecimal.TEN), 0, "The discount amount of " + code + " was not kept.");
        softly.assertAll();
    }

    @Test(priority = 56, groups = {SQ, "sales"},
            description = "\"قالب عرض السعر\" can be chosen on a new quotation and is kept after save")
    public void userCanChooseQuotationTemplate() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        int linesBefore = page.lineCount();
        String terms = page.termsText();
        String template = page.selectRandomOption("salesQuotationTemplateId");
        page.confirmDialogIfShown(Duration.ofSeconds(1));
        page.waitForGridToSettle();
        log.info("Template [{}]: lines {} → {}, terms [{}] → [{}]", template, linesBefore, page.lineCount(), terms, page.termsText());

        // QC 2026-11-12: choosing a template empties "المخزن" (and sets "تاريخ الانتهاء"
        // to today); without a warehouse "اضافة سطر" is not shown at all.
        String warehouse = form.getSelectedText("warehouseId");
        if (warehouse.isEmpty() || warehouse.equals("اختر") || warehouse.equals("المخزن")) {
            log.info("Template [{}] emptied \"المخزن\" → choosing the branch and warehouse again.", template);
            selectBranchUntilWarehouseIsFilled();
        }
        addItemLine();
        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();
        assertEquals(waitForSelected(form, "salesQuotationTemplateId"), template, "The template of " + code + " was not kept.");
    }

    @Test(priority = 57, groups = {SQ, "sales"},
            description = "\"المستند المرجعي\" (free text) and \"عنوان التوصيل\" are kept after save")
    public void referenceTextAndDeliveryAddressAreKept() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        String reference = TestData.referenceText();
        assertTrue(page.setReferenceDocument(reference), "\"المستند المرجعي\" is locked on a new quotation.");
        addItemLine();
        String address = page.inputValue("deliveryAddress");

        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        page.reload();

        SoftAssert softly = new SoftAssert();
        softly.assertEquals(page.inputValue("referenceDocument").trim(), reference, "\"المستند المرجعي\" of " + code + " was not kept.");
        softly.assertEquals(page.inputValue("deliveryAddress").trim(), address.trim(), "\"عنوان التوصيل\" of " + code + " was not kept.");
        softly.assertAll();
    }

    @Test(priority = 58, groups = {SQ, "sales"},
            description = "Choosing the customer fills its tax number; choosing the branch limits the warehouses")
    public void customerAndBranchFillDependentFields() {

        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        String customer = page.selectRandomOption("partnerId");
        page.confirmDialogIfShown(Duration.ofMillis(800));
        String taxNumber = waitUntilEquals(() -> page.inputValue("taxNumber").isBlank() ? "" : "filled", "filled");
        log.info("Customer [{}] → tax number [{}]", customer, page.inputValue("taxNumber"));

        selectBranchUntilWarehouseIsFilled();
        SoftAssert softly = new SoftAssert();
        softly.assertEquals(taxNumber, "filled", "\"الرقم الضريبي\" stayed empty after choosing " + customer
                + " (it may have no tax number in QC).");
        softly.assertFalse(isEmptySelect("warehouseId"), "No warehouse could be chosen for branch "
                + form.getSelectedText("branchId") + ".");
        softly.assertFalse(page.isInputEditable("taxNumber"), "\"الرقم الضريبي\" must be read-only.");
        softly.assertAll();

        page.closeForm();
    }

    // =====================================================================
    // Reusable steps
    // =====================================================================

    /** Opens a random quotation matching {@code filter} (chosen through the API). SKIPPED when QC has none. */
    private Map<String, Object> openRandomQuotation(Predicate<Map<String, Object>> filter) {
        Map<String, Object> record = page.openRandomDocument(filter)
                .orElseThrow(() -> new SkipException("QC has no sales quotation for this scenario."));
        page.waitForCode();
        return record;
    }

    /** Opens a Draft whose form shows "مسودة" (not expired); when there is none, one is created. */
    private Map<String, Object> openActiveDraft() {
        java.util.Optional<Map<String, Object>> opened = page.openRandomDocument(SalesQuotationPage::isActiveDraft);
        if (opened.isPresent()) {
            page.waitForStatusLabel(DRAFT.label());
            return opened.get();
        }
        log.info("No active Draft sales quotation in QC → creating one.");
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        String code = saveAndVerify(DRAFT);
        return page.api().byCode(code);
    }

    /**
     * Opens an Approved quotation whose form shows "معتمد" (not expired);
     * {@code unlinked}: also not turned into a sales order. When there is
     * none, a Draft is created and approved.
     */
    private Map<String, Object> openActiveApproved(boolean unlinked) {
        Predicate<Map<String, Object>> filter = r -> SalesQuotationPage.isActiveApproved(r)
                && (!unlinked || SalesQuotationPage.isUnlinked(r));
        java.util.Optional<Map<String, Object>> opened = page.openRandomDocument(filter);
        if (opened.isPresent()) {
            page.waitForStatusLabel(APPROVED.label());
            return opened.get();
        }
        log.info("No suitable Approved sales quotation in QC → creating and approving one.");
        page.clickNew();
        form.waitForMode(DocumentForm.Mode.ADD);
        fillHeader(false);
        addItemLine();
        String code = saveAndVerify(TestData.NEW_QUOTATION_STATUSES);
        if (!APPROVED.label().equals(page.statusLabel())) {
            ActionOutcome outcome = performAndRecord(form, TestData.API_APPROVE_OR_DISAPPROVE, this::toggleApproval);
            assertAccepted("Approving " + code, TestData.API_APPROVE_OR_DISAPPROVE, outcome);
        }
        page.reload();
        page.waitForStatusLabel(APPROVED.label());
        return page.api().byCode(code);
    }

    private void waitForEditMode() {
        waitUntil("edit mode", Duration.ofSeconds(15), () -> driver.getCurrentUrl().contains("/edit/"));
        form.waitUntilFieldEditable("note");
    }

    private String waitUntilChanges(String previous) {
        long end = System.currentTimeMillis() + 10_000;
        String now = page.code();
        while ((now.isEmpty() || now.equals(previous)) && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            now = page.code();
        }
        return now;
    }

    /** True while changing a saved quotation: every field gets a value DIFFERENT from the saved one. */
    private boolean editing;

    /**
     * Fills a new quotation's header. العميل first (it reloads the price lists and
     * the tax number), then the screen order (right to left, row by row, QC 2026-11-12):
     * طريقة الشحن → (العملة kept) → شروط السداد → التاريخ → عنوان التوصيل →
     * مندوب المبيعات → الأسعار → فريق المبيعات → الفرع → طريقة احتساب الضريبة →
     * تاريخ الانتهاء → قائمة الاسعار → المخزن → (انزال من / الرقم) → البيان.
     * "العملة" keeps its default (a foreign currency needs an exchange rate);
     * "قالب عرض السعر", "تعميم خصم" and "المستند المرجعي" have their own tests;
     * "مركز التكلفة" is filled (and locked) by the branch.
     */
    private void fillHeader(boolean downloadLines) {
        editing = false;
        fillHeaderInScreenOrder(downloadLines);
    }

    private void changeHeader() {
        editing = true;
        try {
            fillHeaderInScreenOrder(false);
        } finally {
            editing = false;
        }
        // Another customer removes every line: "سيتم حذف جميع الأصناف المضافة في حالة تغيير
        // العميل، هل تريد الاستمرار؟" → استمرار (QC 2026-11-12). The save then failed with
        // "يجب اضافة صنف واحد على الأقل", so a line is added again.
        if (!page.isLinesTabActive(SalesQuotationPage.ITEMS_TAB)) {
            page.openItemsTab();
        }
        if (itemLineCount() == 0) {
            log.info("Changing the customer removed the lines → adding an item line again.");
            addItemLine();
        }
    }

    /**
     * "مندوب المبيعات" = {@link SalesQuotationPage#SALES_REPRESENTATIVE}. Other reps leave
     * "فريق المبيعات" without options and "حفظ" then opens no confirmation (QC bug, 2026-11-12).
     * The rep list follows the chosen team, so a team without this rep is cleared first.
     */
    private void selectSalesRepresentative() {
        String field = "salesRepresentativeId";
        String rep = SalesQuotationPage.SALES_REPRESENTATIVE;
        if (!page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
            log.info("{} is locked → kept as [{}].", field, form.getSelectedText(field));
            return;
        }
        if (rep.equals(form.getSelectedText(field))) {
            log.info("{} is already {}.", field, rep);
            return;
        }
        try {
            page.selectOption(field, rep);
        } catch (org.openqa.selenium.NoSuchElementException notInTeam) {
            log.info("{} is not offered with team [{}] → clearing the team.", rep, form.getSelectedText("saleTeamId"));
            new org.openqa.selenium.interactions.Actions(driver).sendKeys(org.openqa.selenium.Keys.ESCAPE).perform();
            page.clearSelect("saleTeamId");
            page.selectOption(field, rep);
        }
        page.confirmDialogIfShown(Duration.ofMillis(800));
        assertEquals(form.getSelectedText(field), rep, "\"مندوب المبيعات\" is not " + rep + ".");
    }

    /** Lines of the open tab that have an item (an empty line added by the grid is not counted). */
    private int itemLineCount() {
        page.waitForGridToSettle();
        int count = 0;
        int rows = page.lineCount();
        for (int row = 0; row < rows; row++) {
            if (!page.isEmptyLine(row)) {
                count++;
            }
        }
        return count;
    }

    private void fillHeaderInScreenOrder(boolean downloadLines) {

        selectIfEditable("partnerId");
        selectIfEditable("shippingMethodId");
        selectIfEditable("paymentTerms");
        if (page.isInputEditable("quotationDate")) {
            page.setQuotationDate(LocalDate.now()); // a future date is not needed here; today is valid
        }
        String address = TestData.deliveryAddress();
        if (page.setDeliveryAddress(address)) {
            assertEquals(page.inputValue("deliveryAddress"), address, "\"عنوان التوصيل\" was not filled.");
        }
        selectSalesRepresentative();
        if (editing) {
            page.selectOtherPriceOption();
        } else if (page.priceOption().isEmpty()) {
            page.selectRandomPriceOption();
        }
        // "فريق المبيعات" is filled from the rep; another team would clear the rep.
        if (isEmptySelect("saleTeamId")) {
            selectIfEditable("saleTeamId");
        }
        selectBranchUntilWarehouseIsFilled();
        selectIfEditable("taxCalculationMethode");
        if (page.isInputEditable("quotationExpiryDate")) {
            page.setExpiryDate(LocalDate.now().plusDays(TestData.EXPIRY_IN_DAYS));
        }
        selectIfEditable("priceListId");
        if (downloadLines && !editing && page.hasField("downloadFrom", Duration.ofSeconds(2))) {
            int before = form.gridRowCount();
            String number = page.downloadLinesFromAnotherQuotation();
            log.info("Downloaded {} line(s) from {}.", form.gridRowCount() - before, number);
            assertTrue(form.gridRowCount() > before, "No lines were downloaded from " + number + ".");
        }
        String note = TestData.uniqueNote();
        form.setNote(note);
        assertEquals(form.waitForNote(note), note, "\"البيان\" was not filled.");
    }

    /** العميل → الفرع → المخزن only (the minimum for "اضافة سطر"), plus a valid expiry date. */
    private void selectCustomerBranchAndWarehouse() {
        editing = false;
        selectIfEditable("partnerId");
        selectBranchUntilWarehouseIsFilled();
    }

    /** Random option when editable (another one while editing); confirms the optional dialog after it. */
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
     * الفرع → المخزن. The warehouses depend on the branch (QC 2026-11-12: فرع رئيسى
     * offers 6 of 27). When a branch offers none, another branch is chosen.
     */
    private void selectBranchUntilWarehouseIsFilled() {
        retryUntilSuccessful("Branch → Warehouse (warehouse must not stay empty)", TestData.MAX_BRANCH_ATTEMPTS, () -> {
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
                log.info("Branch [{}] → warehouse [{}], cost centre [{}]", form.getSelectedText("branchId"),
                        warehouse, form.getSelectedText("costCenterId"));
                return true;
            } catch (RuntimeException failed) {
                log.warn("Branch → Warehouse failed ({}) → again.", failed.getMessage());
                return false;
            }
        });
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

    /** Adds one item line in "قائمة الأصناف" (lines already there are completed first). */
    private void addItemLine() {
        if (!page.isLinesTabActive(SalesQuotationPage.ITEMS_TAB)) {
            page.openItemsTab();
        }
        fillZeroQuantitiesAndPrices();
        page.deleteEmptyLines();
        addItemLineInOpenTab();
    }

    /**
     * "اضافة سطر" in the open tab → random item → quantity and price.
     * An item the quotation does not accept never loads its unit; another is tried.
     *
     * @return the item code of the new line.
     */
    private String addItemLineInOpenTab() {
        int row = page.addLine();
        boolean priceListChanged = false;
        for (int attempt = 1; attempt <= TestData.MAX_ITEM_ATTEMPTS; attempt++) {
            String item;
            try {
                item = page.selectRandomItem(row);
            } catch (org.openqa.selenium.TimeoutException noItems) {
                // QC 2026-11-12: with "قائمة د" + "مخزن 100" the item list opened empty.
                page.stopCellEditing();
                if (priceListChanged || !page.waitForSelectEnabled("priceListId", Duration.ofSeconds(2))) {
                    throw noItems;
                }
                log.warn("\"اسم الصنف\" offers no item with price list [{}] → {}.",
                        form.getSelectedText("priceListId"), SalesQuotationPage.DEFAULT_PRICE_LIST);
                page.selectOption("priceListId", SalesQuotationPage.DEFAULT_PRICE_LIST);
                page.confirmDialogIfShown(Duration.ofMillis(800));
                priceListChanged = true;
                row = page.addLine();
                continue;
            }
            if (page.lineText(row, "uomId").isEmpty()) {
                form.closeEmptyItemSearchDialog();
                log.warn("Item {} did not load into line {} → another item ({}/{}).", item, row + 1, attempt, TestData.MAX_ITEM_ATTEMPTS);
                continue;
            }
            String code = page.lineText(row, "code");
            if (isItemOnAnotherLine(code, row)) {
                // A new item chosen on the SAME line kept showing the old code for a moment
                // (QC 2026-11-12, 37 imported lines: "صنف 35 (28)" right after "صنف للرمز (28)"),
                // and the re-render made the next read stale. The line is removed and a fresh one used.
                log.warn("Item {} ({}) is already on another line → new line, another item.", item, code);
                page.deleteLine(row);
                row = page.addLine();
                continue;
            }
            fillEditableCells(row);
            log.info("Line {}: {} ({}) × {} @ {}", row + 1, item, code, TestData.QUANTITY, TestData.PRICE);
            return code;
        }
        fail("No accepted item found for the new line after " + TestData.MAX_ITEM_ATTEMPTS + " attempts.");
        return "";
    }

    /** Lines filled so far; even lines get the discount / addition rates, odd lines the values. */
    private int filledLines;

    /**
     * Every editable cell of line {@code row}, in screen order (right to left):
     * الوحدة → الكمية → السعر → الخصم% or الخصم → الاضافة% or الاضافة →
     * الكمية المجانية → البيان. The unit comes before the price, because a new
     * unit brings its own price. Not editable (QC 2026-11-12): العبوة, الإجمالي,
     * معدل الضريبة, ضريبة القيمة المضافة, الإجمالي بعد الخصم والاضافة والضريبة.
     */
    private void fillEditableCells(int row) {
        String unit = page.setLineOption(row, SalesQuotationPage.COL_UNIT);
        page.setLineNumber(row, "quantity", TestData.QUANTITY);
        page.setLineNumber(row, "price", TestData.PRICE);
        boolean rates = filledLines++ % 2 == 0;
        if (rates) {
            page.setLineNumber(row, SalesQuotationPage.COL_DISCOUNT_RATE, TestData.LINE_DISCOUNT_RATE);
            page.setLineNumber(row, SalesQuotationPage.COL_EXTRA_RATE, TestData.LINE_EXTRA_RATE);
        } else {
            page.setLineNumber(row, SalesQuotationPage.COL_DISCOUNT_VALUE, TestData.LINE_DISCOUNT_VALUE);
            page.setLineNumber(row, SalesQuotationPage.COL_EXTRA_VALUE, TestData.LINE_EXTRA_VALUE);
        }
        page.setLineNumber(row, SalesQuotationPage.COL_FREE_QUANTITY, TestData.LINE_FREE_QUANTITY);
        page.setLineText(row, SalesQuotationPage.COL_NOTE, TestData.lineNote());
        page.stopCellEditing();

        // The other cell of each pair is calculated by the grid.
        SoftAssert softly = new SoftAssert();
        String pairDiscount = rates ? SalesQuotationPage.COL_DISCOUNT_VALUE : SalesQuotationPage.COL_DISCOUNT_RATE;
        String pairExtra = rates ? SalesQuotationPage.COL_EXTRA_VALUE : SalesQuotationPage.COL_EXTRA_RATE;
        softly.assertTrue(page.lineNumber(row, pairDiscount).signum() > 0,
                "Line " + (row + 1) + ": \"" + pairDiscount + "\" was not calculated from the discount.");
        softly.assertTrue(page.lineNumber(row, pairExtra).signum() > 0,
                "Line " + (row + 1) + ": \"" + pairExtra + "\" was not calculated from the addition.");
        softly.assertAll();
        log.info("Line {}: unit [{}], discount {} / {}, addition {} / {}, free {}, note [{}]", row + 1, unit,
                page.lineText(row, SalesQuotationPage.COL_DISCOUNT_RATE), page.lineText(row, SalesQuotationPage.COL_DISCOUNT_VALUE),
                page.lineText(row, SalesQuotationPage.COL_EXTRA_RATE), page.lineText(row, SalesQuotationPage.COL_EXTRA_VALUE),
                page.lineText(row, SalesQuotationPage.COL_FREE_QUANTITY), page.lineText(row, SalesQuotationPage.COL_NOTE));
    }

    /**
     * One item line in "قائمة الأصناف" and one optional line in "مميز" (every
     * editable cell filled in both), then back to "قائمة الأصناف" for the save.
     */
    private void addLinesInBothTabs() {
        addItemLine();
        page.openFeaturesTab();
        fillZeroQuantitiesAndPrices();
        page.deleteEmptyLines();
        String optional = addItemLineInOpenTab();
        page.stopCellEditing();
        page.deleteEmptyLines();
        log.info("Optional line in \"{}\": {}", SalesQuotationPage.FEATURES_TAB, optional);
        page.openItemsTab();
    }

    /**
     * Completes the lines in the open grid (downloaded, copied, imported), last
     * to first: a repeated item is removed; an empty / 0 quantity or price gets
     * the test value (QC 2026-11-12: the price list gave price 0 for some items).
     */
    private void fillZeroQuantitiesAndPrices() {
        page.waitForGridToSettle();
        int rows = form.gridRowCount();
        Map<String, Integer> firstRowOfItem = new HashMap<>();
        for (int row = 0; row < rows; row++) {
            firstRowOfItem.putIfAbsent(page.lineText(row, "code"), row);
        }
        for (int row = rows - 1; row >= 0; row--) {
            String code = page.lineText(row, "code");
            if (!code.isEmpty() && firstRowOfItem.get(code) != row) {
                page.deleteLine(row);
                continue;
            }
            if (code.isEmpty()) {
                continue; // an empty line is removed before saving
            }
            if (page.lineNumber(row, "quantity").signum() == 0) {
                page.setLineNumber(row, "quantity", TestData.QUANTITY);
            }
            if (page.lineNumber(row, "price").signum() == 0) {
                page.setLineNumber(row, "price", TestData.PRICE);
            }
        }
        page.stopCellEditing();
    }

    /** "code × quantity @ price" of every line of the open grid. */
    private List<String> linesSnapshot() {
        List<String> lines = new ArrayList<>();
        int count = page.lineCount();
        for (int row = 0; row < count; row++) {
            // QC 2026-11-12: the empty line the grid adds after Enter was counted here
            // ("قائمة الأصناف" 2 lines before save, 1 after), but it is never saved.
            if (page.isEmptyLine(row)) {
                continue;
            }
            lines.add(page.lineText(row, "code") + " × " + page.lineNumber(row, "quantity").stripTrailingZeros().toPlainString()
                    + " @ " + page.lineNumber(row, "price").stripTrailingZeros().toPlainString());
        }
        return lines;
    }

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
        return previous;
    }

    private String waitForSelected(DocumentForm on, String field) {
        long end = System.currentTimeMillis() + 10_000;
        String value = on.getSelectedText(field);
        while (value.isEmpty() && System.currentTimeMillis() < end) {
            pause(POLL_MILLIS);
            value = on.getSelectedText(field);
        }
        return value;
    }

    private void assertBackOnList(String afterWhat) {
        waitUntil("the list after " + afterWhat, Duration.ofSeconds(15), () -> !page.isFormOpen());
        page.waitForListLoaded();
    }

    /** "اعتماد" / "عدم اعتماد" of the opened quotation, then "موافق". */
    private void toggleApproval() {
        assertTrue(page.waitForActionEnabled("approveOrDisapprove"), "\"اعتماد\" is disabled on " + page.code() + ".");
        page.clickApproveOrDisapprove();
        page.confirmDialog();
    }

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
        page.waitForStatusLabel(CANCELLED.label());

        SoftAssert softly = new SoftAssert();
        for (String action : TestData.ACTIONS_DISABLED_WHEN_CANCELLED) {
            softly.assertFalse(page.isActionEnabled(action), "'" + action + "' must be disabled on cancelled " + code + ".");
        }
        Map<String, Object> record = page.api().byCode(code);
        softly.assertTrue(record != null && SalesQuotationPage.isCancelled(record),
                "The API does not mark " + code + " as cancelled: " + record);
        softly.assertAll();
    }

    /** Text of the top-most open dialog. */
    private String dialogText() {
        Object text = ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const d=[...document.querySelectorAll(\"[role=dialog][data-state=open],[role=alertdialog][data-state=open]\")];"
                        + "return d.length ? d[d.length-1].innerText : '';");
        return text == null ? "" : text.toString();
    }

    /** "إلغاء" of the top-most open dialog; waits until it is closed. */
    private void cancelOpenDialog() {
        ((org.openqa.selenium.JavascriptExecutor) driver).executeScript(
                "const d=[...document.querySelectorAll(\"[role=dialog][data-state=open],[role=alertdialog][data-state=open]\")].pop();"
                        + "const b=d && [...d.querySelectorAll('button')].find(x=>x.innerText.trim()==='إلغاء');"
                        + "if(b) b.click();");
        waitUntil("the dialog to close", Duration.ofSeconds(10), () -> !form.isDialogOpen());
    }

    /**
     * Saves and verifies the RESULT: "هل انت متاكد من عملية الحفظ؟" → نعم; the
     * sales-period warning (when shown) → متابعة; success toast → view mode →
     * number → description persisted → status.
     *
     * @return the saved quotation's number.
     */
    private String saveAndVerify(DocumentStatus... allowedStatuses) {

        String note = form.getNote();
        if (!note.startsWith(TestData.NOTE_PREFIX)) {
            note = TestData.uniqueNote();
            form.setNote(note);
        }
        if (!page.isLinesTabActive(SalesQuotationPage.ITEMS_TAB)) {
            page.openItemsTab();
        }
        page.stopCellEditing();
        page.deleteEmptyLines();
        fillRequiredFields(TestData.REQUIRED_HEADER_FIELDS.stream()
                .filter(f -> isEmptySelect(f) && page.waitForSelectEnabled(f, Duration.ofSeconds(1)))
                .toList());
        assertTrue(page.waitForActionEnabled("save"), "\"حفظ\" stayed disabled.");
        page.startRecordingSaveRequest();
        page.clickSave();
        failIfSaveBlockedBeforeConfirmation();
        page.confirmDialog();
        // A second dialog ("هل تريد المتابعة؟", sales period exceeded) or the toast comes next.
        // The toast lasts only 2–3 s, so it is taken as soon as it shows.
        DocumentForm.Toast toast = form.waitForConfirmationDialogOrToast().orElse(null);
        if (toast == null) {
            if (page.confirmDialogIfShown(Duration.ofSeconds(2))) {
                log.warn("Save asked to continue (sales-period warning) → متابعة.");
            }
            toast = form.waitForToast();
        }
        boolean uploadOnly = false;
        if (isUploadToast(toast) || isNotificationToast(toast)) {
            // Not the save's answer: the attachments upload, or a live notification
            // ("إشعار جديد — تنبيه: حد الطلب …", QC 2026-11-12) that came at the same time.
            String ignored = isUploadToast(toast) ? "تمت إضافة الملفات" : TestData.NOTIFICATION_TITLE;
            List<String> others = page.otherToastTexts(ignored, Duration.ofSeconds(8));
            log.info("Toast [{}] is not the save's answer; other toasts: {}", toast.title(), others);
            assertTrue(others.stream().noneMatch(t -> t.startsWith("خطأ") || t.contains("[خطأ]")),
                    "Save failed on the server: " + others + " | lines sent: " + page.lastSavedLines());
            uploadOnly = others.stream().noneMatch(t -> t.contains(TestData.SAVE_SUCCESS_MESSAGE));
        }
        boolean warning = isWarning(toast) || uploadOnly || isUploadToast(toast) || isNotificationToast(toast);
        if (!warning) {
            assertTrue(toast.isSuccess(), "Save failed on the server: " + toast + " | lines sent: " + page.lastSavedLines());
        }

        form.waitForMode(DocumentForm.Mode.VIEW);
        String code = page.waitForCode();

        SoftAssert softly = new SoftAssert();
        if (!warning) {
            softly.assertTrue(toast.fullText().contains(TestData.SAVE_SUCCESS_MESSAGE), "Unexpected success message: " + toast);
        }
        String shownNote = form.waitForNote(note);
        String status = page.statusLabel();
        if (!note.equals(shownNote) || !isOneOf(status, allowedStatuses)) {
            log.warn("After saving {}: note [{}] status [{}] → reloading.", code, shownNote, status);
            page.reload();
            shownNote = form.waitForNote(note);
            status = page.statusLabel();
        }
        softly.assertEquals(shownNote, note, "Description was not persisted in " + code + ".");
        softly.assertTrue(isOneOf(status, allowedStatuses), "Unexpected status [" + status + "] after saving " + code
                + "; expected one of " + Arrays.toString(allowedStatuses) + ".");
        softly.assertAll();

        log.info("Saved Sales Quotation {} with status {}", code, status);
        return code;
    }

    /**
     * "إضافة" / "تكرار" must create a NEW quotation and leave the source as it
     * was. The saved one is found in the API by its unique description.
     */
    private String verifyNewQuotationSaved(Map<String, Object> source, String shownCode, String how) {

        String note = form.getNote();
        String sourceCode = SalesQuotationPage.codeOf(source);
        List<Map<String, Object>> all = page.api().findAll();
        Map<String, Object> saved = all.stream().filter(r -> note.equals(String.valueOf(r.get("note")))).findFirst().orElse(null);
        Map<String, Object> sourceNow = all.stream()
                .filter(r -> String.valueOf(source.get("id")).equals(String.valueOf(r.get("id")))).findFirst().orElse(null);
        assertTrue(saved != null, "No quotation with the description [" + note + "] in the API after " + how + ".");
        String newCode = SalesQuotationPage.codeOf(saved);
        log.info("{}: source {} → new {}; the form shows {}.", how, sourceCode, newCode, shownCode);

        SoftAssert softly = new SoftAssert();
        softly.assertNotEquals(newCode, sourceCode, how + " saved over the source quotation " + sourceCode + ".");
        if (sourceNow != null) {
            softly.assertEquals(sourceNow.get("recordStatus"), source.get("recordStatus"),
                    "The status of the source " + sourceCode + " changed after " + how + ".");
            softly.assertEquals(String.valueOf(sourceNow.get("note")), String.valueOf(source.get("note")),
                    "The description of the source " + sourceCode + " changed after " + how + ".");
        }
        softly.assertEquals(waitUntilEquals(page::code, newCode), newCode,
                "After " + how + " the form shows " + page.code() + " instead of the new quotation " + newCode + ".");
        softly.assertAll();
        return newCode;
    }

    private static boolean isOneOf(String statusLabel, DocumentStatus... allowed) {
        return Arrays.stream(DocumentStatus.values()).anyMatch(s -> s.label().equals(statusLabel))
                && DocumentStatus.fromLabel(statusLabel).isOneOf(allowed);
    }

    /** Empty select; "اختر" (or the field's own name) is the placeholder shown when nothing is chosen. */
    private boolean isEmptySelect(String field) {
        String text = form.getSelectedText(field);
        return text.isEmpty() || text.equals("اختر");
    }

    private static boolean isUploadToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.contains("تمت إضافة الملفات") || text.contains("تم الرفع");
    }

    /** A live notification of the system ("إشعار جديد"), not an answer to the user's action. */
    private static boolean isNotificationToast(DocumentForm.Toast toast) {
        String text = toast.fullText() == null ? "" : toast.fullText();
        return text.startsWith(TestData.NOTIFICATION_TITLE) || text.contains("[" + TestData.NOTIFICATION_TITLE + "]");
    }

    private static boolean isWarning(DocumentForm.Toast toast) {
        String title = toast.title() == null ? "" : toast.title();
        return title.contains("تنبيه") || title.contains("تحذير");
    }

    /** "حفظ" opens a confirmation only when the form is valid; otherwise the reason is reported. */
    private void failIfSaveBlockedBeforeConfirmation() {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                java.util.Optional<DocumentForm.Toast> toast = form.waitForConfirmationDialogOrToast();
                if (toast.isPresent() && isNotificationToast(toast.get())) {
                    log.info("Notification [{}] while saving → waiting for the save confirmation.", toast.get().fullText());
                    waitUntil("the save confirmation after a notification", Duration.ofSeconds(15), form::isDialogOpen);
                    return;
                }
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
                if (!errors.isEmpty()) {
                    fillRequiredFields(errors);
                }
                page.clickSave();
            }
        }
    }

    /** Fills empty required header fields (branch / warehouse together). */
    private void fillRequiredFields(List<String> fields) {
        if (fields.contains("prices") && page.priceOption().isEmpty()) {
            page.selectRandomPriceOption();
        }
        for (String field : fields) {
            if (List.of("branchId", "warehouseId", "prices").contains(field)) {
                continue;
            }
            if (page.waitForSelectEnabled(field, Duration.ofSeconds(5))) {
                page.selectRandomOption(field);
                page.confirmDialogIfShown(Duration.ofMillis(800));
            }
        }
        if (fields.contains("branchId") || fields.contains("warehouseId")) {
            boolean wasEditing = editing;
            editing = false;
            selectBranchUntilWarehouseIsFilled();
            editing = wasEditing;
        }
    }
}
