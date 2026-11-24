# أوامر البيع، فواتير المبيعات، مردود المبيعات – حالات الأتمتة

الكود:
- الصفحات: `src/main/java/com/erp/automation/pages/sales/` → `SalesOrderPage`, `SalesInvoiceDocumentPage` (المشترك بين الفاتورة والمردود), `SalesInvoicePage`, `SalesReturnPage`
- القاعدة المشتركة للحالات: `src/test/java/com/erp/automation/base/SalesDocumentBaseTest.java`. فيها الحالات المتكررة في كل شاشة (القائمة، البحث، التنقل، الصفحات، كانبان، التجميع، الطباعة، PDF، Excel، المرفقات، الخروج، سجل العمليات، الحفظ الفارغ)، وتعمل مرة لكل شاشة وتظهر في التقرير تحت اسمها.
- الحالات: `src/test/java/com/erp/tests/sales/` → `SalesOrderTest` (50 حالة), `SalesInvoiceTest` (41), `SalesReturnTest` (53)

التشغيل: `mvn test -Dtest=SalesOrderTest` (أو SalesInvoiceTest / SalesReturnTest)

## نتائج الاستكشاف (QC – 2026-11-12)

| البند | أمر البيع | فاتورة المبيعات | مردود المبيعات |
|---|---|---|---|
| المسار | `/sales/sales-order` | `/sales/invoices` | `/sales/sales-return` |
| بادئة data-testid | `sales-order` | `invoices` | `sales-return` |
| الرقم | `#code` – SO | `#invoiceCode` – SI | `#invoiceCode` – SRI |
| الـ API | `SalesOrder/filter` | `SaleInvoice/filter/0?includePOS=false` | `SaleInvoice/filter/1?includePOS=false` |
| العميل | `partnerId` | `vendorId` | `vendorId` |
| البيان | `note` | `notes` | `notes` |
| أعمدة الكمية/السعر | `qty` / `price` | `quantity` / `cost` | `quantity` / `cost` |
| الحالات | مسودة / معتمد / ملغي | حالة الفاتورة (معتمد فوراً عند الحفظ) + حالة التسليم + حالة الدفع | مثل الفاتورة |
| السجلات في QC | 35 (14 مسودة، 20 معتمد، 1 ملغي) | 215 كلها معتمدة | 60 كلها معتمدة |
| تبويبات الأصناف | لا يوجد | تفاصيل الفاتورة، الخصومات والإضافات، شروط السداد و جدولة الدفعات، الدفعات | تفاصيل الفاتورة، الخصومات والإضافات |
| المستند المرجعي | زران: اختيار عرض سعر معتمد / تحميله | اختيار أمر بيع / تحميله | اختيار فاتورة / تحميلها |
| انزال من | أوامر البيع | فواتير المبيعات، مردود المبيعات | فواتير المبيعات، مردود المبيعات |
| ⋮ (جديد) | الاستيراد من Excel، متابعة من عرض سعر | استيراد من Execl (معطل)، متابعة من أمر بيع | استيراد من Execl (معطل) |
| ⋮ (محفوظ) | التصدير الى Excel، انشاء فاتورة مبيعات (للمعتمد) | تصدير الى Execl، انشاء مردود مبيعات، إنشاء إشعار مدين، إنشاء إشعار دائن، دفع، إنشاء أمر تسليم | تصدير الى Execl، دفع |
| التعديل بعد الاعتماد | زر التعديل معطل | `/view-approved/` البيان فقط | `/view-approved/` البيان فقط |
| أزرار خاصة | – | قيد اليومية | قيد اليومية، لا يوجد "نسخ" |
| حقول إلزامية إضافية | – | حساب الدفع مع "نقدا" (رسالة الخادم: حساب الدفع مطلوب) | سعر المرتجع* |

مسار المستندات الذي تم التحقق منه يدوياً: أمر بيع **SO2611-000038** ← اعتماد ← فاتورة **SI2611-000217** (قيد: مدين 23 = دائن 20 + 3 ضريبة) ← مردود **SRI2611-000060**. هذه المستندات حُفظت في QC أثناء الاستكشاف.

## ملاحظات للمحللين (قد تسبب فشل حالات عن قصد)

1. أمر بيع معتمد صدرت منه فاتورة: زر "عدم اعتماد" معطل لكن زر "إلغاء" مفعل (SO2608-000001). الحالة `invoicedApprovedSalesOrderCannotBeCancelledOrDisapproved` متوقع أن تفشل حتى يتم تأكيد القاعدة.
2. لا تصدر فاتورة مبيعات إلا من أمر بيع "معتمد". وجود أمر "مسودة" صدرت منه فاتورة في QC (SO2609-000009) خلل، والحالة 22 تبلغ عنه.
3. الفاتورة والمردود يُعتمدان فور الحفظ، ولا يوجد إلغاء أو عدم اعتماد. حالة `savedSalesInvoiceToolbar` تتأكد من ذلك.
4. المردود بدون مستند مرجعي غير مسموح (قاعدة مؤكدة من QC – 2026-11-18). حالة `userCannotSaveSalesReturnWithoutReferenceDocument` تتأكد أنه لا يُحفظ.
5. ملف Excel المستخدم في الاستيراد هو ملف المشتريات، وقد يحتاج أمر البيع ملفاً خاصاً.
6. أسماء قائمة ⋮ في الفاتورة والمردود مكتوبة "Execl" بدل "Excel" في النظام.
7. مردود المبيعات (2026-11-18) – قواعد تحتاج تأكيد، والحالات تبلغ عنها إذا اختلف النظام:
   - الكمية المرتجعة ≤ (المفوتر − ما سبق إرجاعه) من نفس الفاتورة (الحالات 34، 35)، والكمية صفر أو سالبة مرفوضة (36).
   - "حساب الدفع" مطلوب مع "نقدا" في المردود كما في الفاتورة (38).
   - خيارات "سعر المرتجع" غير اليدوية تعطي سعراً أكبر من صفر، و"تحديد يدوي" يسمح بكتابة السعر (41، 42).
   - قيد المردود يجعل حساب العميل دائناً بإجمالي المردود شامل الضريبة (48).
   - الفاتورة المرجعية تُختار من الـ API (العميل والفرع والمخزن معبأة، الأكثر تعبئة أولاً، والتي لم يُرجع منها شيء أولاً دون استبعاد غيرها)، ويُحفظ المردود بالكميات والأسعار كما نزلت؛ لا يُعدَّل إلا الحقول الفارغة وحقول المردود.
   - المردود من "المستند المرجعي" لا يظهر فيه "سعر المرتجع" (QC 2026-11-18): هل هذا مقصود؟
   - مبلغ الدفع في المردود يتبع الكمية المرتجعة (قاعدة مؤكدة من QC – 2026-11-18). الحالات 5 و40 و43 و53 ترجع كمية أقل من النازلة؛ رفض الحفظ بـ "مبلغ الدفع اكبر من قيمة الفاتورة" خلل في النظام.
   - لا يمكن عمل مردود مبيعات بدون "المستند المرجعي" (قاعدة مؤكدة من QC – 2026-11-18): كل حالات الإنشاء والإضافة تختار فاتورة من الـ API كمستند مرجعي، وحالات التعديل تفتح مردوداً له مستند مرجعي فقط، والحالة 7 تتأكد أن المردود بدون مستند مرجعي لا يُحفظ. لا يوجد "نسخ" (تكرار) في شاشة المردود.
   - "سعر المرتجع" لا يظهر في المردود الذي له مستند مرجعي مُنزَّل؛ الحالات 37 و41 و42 تتخطى (Skip) إذا لم يظهر: هل يبقى الحقل مستخدماً؟
   - غير مغطى آلياً حتى الآن: أثر المردود على رصيد المخزون، ومدة السماح بالإرجاع من تاريخ الفاتورة (لا توجد قاعدة معروفة).

## حالات أمر البيع

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | listOpensFromSalesMenu | المبيعات → العمليات → the screen link opens its list with its columns and "جديد" |
| 2 | navigation | newButtonOpensEmptyDocument | "جديد" opens an empty document: a new number, today's date, no customer, no status |
| 3 | navigation, list | userCanOpenDocumentFromListRowActions | List row ⋯ → "عرض" opens that document |
| 4 | navigation, list | userCanOpenDocumentByDoubleClickOnListRow | Double-click on a list row opens that document with the status shown in the list |
| 5 | create | userCanCreateNewSalesOrder | New order from "جديد": every header field (فريق المبيعات before مندوب المبيعات, العملة + التعادل, مركز التكلفة, الأسعار, تعميم خصم على الأصناف + قيمته) and an item line with every editable cell; the header is kept after save + reload |
| 6 | create | userCanAddNewSalesOrderFromOpenedOne | "إضافة" on an opened order creates a NEW order (full header, full line); the opened one is unchanged; the header is kept after reload |
| 7 | create | userCanCopySalesOrder | "نسخ" (copy) of an order, every header field changed and a full line added, saved as a new order; the source is unchanged; the header is kept after reload |
| 8 | create, excel | userCanCreateSalesOrderFromExcel | New order whose lines are imported from Excel (⋮ → الاستيراد من Excel) |
| 9 | create, reference | userCanCreateSalesOrderFromQuotationThroughMenu | ⋮ → "متابعة من عرض سعر" → an approved quotation → download: its customer and lines are brought in; "المستند المرجعي" is kept after save |
| 10 | create, reference | userCanCreateSalesOrderFromQuotationThroughReferenceField | "المستند المرجعي" (first button) → an approved quotation → download (second button): customer and lines brought in; the rest of the header and a full line are filled; the reference and the header are kept after save |
| 11 | create, lines | userCanDownloadLinesFromAnotherSalesOrder | Full header; "انزال من" = أوامر البيع → "الرقم" = random order → download: its lines are added; a full line is added; the downloaded items and the header are kept after save |
| 12 | edit | userCanEditDraftSalesOrder | Edit a Draft: every editable header field (team before sales person, currency, prices, bulk discount; مركز التكلفة is locked) and a new line with every editable cell; it stays a Draft and the header is kept after reload |
| 13 | edit, validation | draftSalesOrderEditModeHasExpectedEditableFields | Draft in edit mode: header fields editable; number, rate, tax number, approval date and "المستند المرجعي" are not; "انزال من" is offered |
| 14 | edit | userCanUndoSalesOrderChanges | Edit a Draft (all fields + a line), then Undo restores the saved values |
| 15 | edit | editedSalesOrderHeaderIsKeptAfterSave | Header values of an edited Draft are the same after save and reload |
| 16 | status | userCanDeleteDraftSalesOrder | Delete a Draft: success message, not in the list nor in the API any more |
| 17 | status, log | userCanApproveDraftSalesOrder | Approve a Draft: Approved after reload, approval date today, edit and delete disabled, an approval log entry, ⋮ "انشاء فاتورة مبيعات" enabled |
| 18 | status | userCanDisapproveApprovedSalesOrder | Disapprove an Approved order without invoice: back to Draft, edit and delete enabled again |
| 19 | cancel | userCanCancelDraftSalesOrder | Cancel a Draft: Cancelled after reload; edit/delete/cancel/approve/attachments disabled |
| 20 | cancel | userCanCancelApprovedSalesOrder | Cancel an Approved order without invoice: Cancelled after reload |
| 21 | cancel, validation | invoicedApprovedSalesOrderCannotBeCancelledOrDisapproved | Approved order already invoiced: disapprove and cancel are disabled |
| 22 | validation, status | draftSalesOrderCannotBeInvoiced | A sales invoice is made from an APPROVED order only: a Draft offers no "انشاء فاتورة مبيعات", and no Draft order in QC is marked as invoiced (isInvoice) |
| 23 | validation | newSalesOrderEnablesOnlyAllowedActions | New order toolbar: save/close/attachments/record search enabled; the rest disabled; ⋮ has the Excel import and "متابعة من عرض سعر" |
| 24 | validation, status | draftSalesOrderEnablesEditActions | Draft without invoice: edit/add/copy/delete/approve/cancel/print enabled, no approval date, ⋮ has no "انشاء فاتورة مبيعات" |
| 25 | validation, status | approvedSalesOrderIsLocked | Approved without invoice: edit and delete disabled; disapprove/cancel/copy/print enabled; approval date set |
| 26 | validation, status | cancelledSalesOrderIsReadOnly | Cancelled order is read-only: edit/delete/cancel/approve/attachments disabled |
| 27 | validation | userCannotSaveEmptyDocument | Saving an empty document is blocked: error toast, required fields marked, stays in Add |
| 28 | validation | userCannotSaveSalesOrderWithoutLines | Header complete but no item line: saving is refused with "يجب اضافة صنف واحد على الأقل" |
| 29 | search | userCanSearchListByCode | List search by number shows that document |
| 30 | search | listSearchWithoutMatchShowsNoRows | List search for a text no document has shows no rows |
| 31 | search | userCanOpenDocumentFromRecordSearch | Record search (magnifier) in the form opens the document typed |
| 32 | navigation | userCanNavigateBetweenRecords | Record navigation in the form: first / next / back / last |
| 33 | pagination | userCanNavigateListPagination | List pager: first/last page, Next page by page to the end, back to a random page |
| 34 | list | userCanSwitchListToKanbanAndBack | Kanban shows the same documents as the list page; a card opens its document |
| 35 | list | userCanGroupListByStatus | Group by status: every group has documents and the groups add up to the list total |
| 36 | print | userCanPrintDocument | Print a random document: the preview shows its number; printed through Chrome's dialog |
| 37 | print | userCanSaveDocumentAsPdf | Save a random document as PDF: a PDF file is downloaded |
| 38 | excel | userCanExportDocumentToExcel | ⋮ → export to Excel: an .xlsx with the document's items is downloaded |
| 39 | follow-up | userCanOpenItemsPricingFromSalesOrder | Items Pricing ($): Sales → Pricing opens with the order's items |
| 40 | follow-up, print | userCanOpenPrintBarcodeFromSalesOrder | Print Barcode: Item Barcodes opens with the order's items |
| 41 | follow-up | userCanCreateSalesInvoiceFromApprovedSalesOrder | Approved order → ⋮ → انشاء فاتورة مبيعات: a new invoice that refers to it, with its customer, branch, warehouse and items (not saved) |
| 42 | attachments | userCanAddAttachments | Attachments added in edit mode are listed and kept after save and reload |
| 43 | attachments | userCanDeleteAttachment | Deleting an attachment in edit mode is kept after save and reload |
| 44 | exit | userCanExitDocument | Close a new, a viewed and an edited document: each returns to the list |
| 45 | log | userCanViewTransactionLog | Transaction log: the first entry is the creation |
| 46 | lines | savedSalesOrderKeepsItsLines | A new order keeps its lines (item, quantity, price) after save and reload |
| 47 | lines, edit | userCanEditExistingLineOfSalesOrder | Draft: changing the quantity, the price and the discount % of a line is saved |
| 48 | lines | userCanDeleteItemLineOfSalesOrder | New order: deleting an item line is saved |
| 49 | sales | userCanApplyBulkDiscountToSalesOrder | "تعميم خصم على الأصناف" (نسبة %) is saved with the order |
| 50 | sales | customerAndBranchFillDependentFieldsOfSalesOrder | العميل fills its tax number; الفرع empties the warehouse and limits the warehouses |

## حالات فاتورة المبيعات

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | listOpensFromSalesMenu | المبيعات → العمليات → the screen link opens its list with its columns and "جديد" |
| 2 | navigation | newButtonOpensEmptyDocument | "جديد" opens an empty document: a new number, today's date, no customer, no status |
| 3 | navigation, list | userCanOpenDocumentFromListRowActions | List row ⋯ → "عرض" opens that document |
| 4 | navigation, list | userCanOpenDocumentByDoubleClickOnListRow | Double-click on a list row opens that document with the status shown in the list |
| 5 | create | userCanCreateNewSalesInvoice | New invoice from "جديد": header, payment, an item line; saved as Approved with today's approval date, a payment status and a journal entry |
| 6 | create, reference | userCanCreateSalesInvoiceFromOrderThroughMenu | ⋮ → "متابعة من أمر بيع" → an order → download: customer, branch, warehouse and lines come from the order; "المستند المرجعي" is kept |
| 7 | create, reference | userCanCreateSalesInvoiceFromOrderThroughReferenceField | "المستند المرجعي" (first button) → a sales order → download (second button) → save |
| 8 | create, lines | userCanDownloadLinesFromAnotherSalesInvoice | "انزال من" = فواتير المبيعات → random invoice: its lines are added to the new invoice |
| 9 | create | userCanAddNewSalesInvoiceFromOpenedOne | "إضافة" on an opened invoice creates a NEW invoice; the opened one is unchanged |
| 10 | create | userCanCopySalesInvoice | "نسخ" (copy) of an invoice, completed and saved as a new invoice; the source is unchanged |
| 11 | edit | savedSalesInvoiceAllowsDescriptionEditOnly | Edit a saved invoice: only "البيان" can be changed; it stays Approved |
| 12 | status | savedSalesInvoiceShowsItsStatuses | A saved invoice shows its three statuses: invoice (معتمد), delivery and payment |
| 13 | journal | salesInvoiceJournalEntryIsBalanced | "قيد اليومية" of a saved invoice is balanced and its debit equals the invoice total with tax |
| 14 | validation | newSalesInvoiceEnablesOnlyAllowedActions | New invoice toolbar: "حفظ" waits for a change; copy/add/undo/delete/approve/journal/log disabled; ⋮ has "متابعة من أمر بيع" |
| 15 | validation, status | savedSalesInvoiceToolbar | Saved invoice toolbar: edit/add/copy/print/journal/attachments enabled; delete disabled; no approve or cancel |
| 16 | validation | savedSalesInvoiceMenuHasFollowUpActions | ⋮ of a saved invoice: export, create return, debit note, credit note, pay, delivery order |
| 17 | sales | userCanOpenEveryLineTabOfSalesInvoice | Every line tab of a saved invoice opens (تفاصيل الفاتورة، الخصومات والإضافات، شروط السداد، الدفعات) |
| 18 | validation | userCannotSaveEmptyDocument | Saving an empty document is blocked: error toast, required fields marked, stays in Add |
| 19 | validation | userCannotSaveSalesInvoiceWithoutLines | Header complete but no item line: saving is refused with "يجب اضافة صنف واحد على الأقل" |
| 20 | validation | cashInvoiceNeedsPaymentAccount | Payment method "نقدا" without "حساب الدفع" is refused by the server ("حساب الدفع مطلوب") |
| 21 | search | userCanSearchListByCode | List search by number shows that document |
| 22 | search | listSearchWithoutMatchShowsNoRows | List search for a text no document has shows no rows |
| 23 | search | userCanOpenDocumentFromRecordSearch | Record search (magnifier) in the form opens the document typed |
| 24 | navigation | userCanNavigateBetweenRecords | Record navigation in the form: first / next / back / last |
| 25 | pagination | userCanNavigateListPagination | List pager: first/last page, Next page by page to the end, back to a random page |
| 26 | list | userCanSwitchListToKanbanAndBack | Kanban shows the same documents as the list page; a card opens its document |
| 27 | list | userCanGroupListByStatus | Group by status: every group has documents and the groups add up to the list total |
| 28 | print | userCanPrintDocument | Print a random document: the preview shows its number; printed through Chrome's dialog |
| 29 | print | userCanSaveDocumentAsPdf | Save a random document as PDF: a PDF file is downloaded |
| 30 | excel | userCanExportDocumentToExcel | ⋮ → export to Excel: an .xlsx with the document's items is downloaded |
| 31 | follow-up | userCanOpenItemsPricingFromSalesInvoice | Items Pricing ($): Sales → Pricing opens with the invoice's items |
| 32 | follow-up, print | userCanOpenPrintBarcodeFromSalesInvoice | Print Barcode: Item Barcodes opens with the invoice's items |
| 33 | follow-up | userCanCreateSalesReturnFromSalesInvoice | ⋮ → انشاء مردود مبيعات: a new return that refers to the invoice, with its customer, branch, warehouse and items (not saved) |
| 34 | follow-up | followUpScreensOpenFromSalesInvoice | ⋮ → إنشاء إشعار مدين / إنشاء إشعار دائن / إنشاء أمر تسليم each open their screen (not saved) |
| 35 | attachments | userCanAddAttachments | Attachments added in edit mode are listed and kept after save and reload |
| 36 | attachments | userCanDeleteAttachment | Deleting an attachment in edit mode is kept after save and reload |
| 37 | exit | userCanExitDocument | Close a new, a viewed and an edited document: each returns to the list |
| 38 | log | userCanViewTransactionLog | Transaction log: the first entry is the creation |
| 39 | lines | savedSalesInvoiceKeepsItsLines | A new invoice keeps its lines (item, quantity, price) after save and reload |
| 40 | lines | userCanDeleteItemLineOfSalesInvoice | New invoice: deleting an item line before saving is kept |
| 41 | sales | customerAndBranchFillDependentFieldsOfSalesInvoice | العميل fills الرقم الضريبي and حساب العميل; الفرع fills مركز التكلفة |

## حالات مردود المبيعات

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | listOpensFromSalesMenu | المبيعات → العمليات → the screen link opens its list with its columns and "جديد" |
| 2 | navigation | newButtonOpensEmptyDocument | "جديد" opens an empty document: a new number, today's date, no customer, no status |
| 3 | navigation, list | userCanOpenDocumentFromListRowActions | List row ⋯ → "عرض" opens that document |
| 4 | navigation, list | userCanOpenDocumentByDoubleClickOnListRow | Double-click on a list row opens that document with the status shown in the list |
| 5 | create, reference | userCanCreateSalesReturnFromInvoiceThroughReferenceField | "المستند المرجعي" → a sales invoice → download: customer, branch, warehouse and lines come from the invoice; 1 of each item is returned; saved as Approved; the reference is kept |
| 6 | create, lines | userCanCreateSalesReturnByDownloadingInvoiceLines | "المستند المرجعي" = an API invoice (not downloaded), then "انزال من" = فواتير المبيعات → random invoice: its lines are added; 1 of each is returned |
| 7 | create, validation | userCannotSaveSalesReturnWithoutReferenceDocument | A return without "المستند المرجعي" (header, "سعر المرتجع", an item line) is not saved |
| 8 | create | userCanAddNewSalesReturnFromOpenedOne | "إضافة" on an opened return (that has a reference) → "المستند المرجعي" = an API invoice, downloaded → partial return saved as a NEW return; the opened one is unchanged |
| 9 | edit | savedSalesReturnAllowsDescriptionEditOnly | Edit a saved return: only "البيان" can be changed; it stays Approved |
| 10 | sales | returnPriceOffersTheAgreedOptions | "سعر المرتجع" offers the four agreed options |
| 11 | journal | salesReturnJournalEntryIsBalanced | "قيد اليومية" of a saved return is balanced |
| 12 | validation | newSalesReturnEnablesOnlyAllowedActions | New return toolbar: "حفظ" waits for a change; add/undo/delete/approve/journal/log disabled; no "نسخ" button |
| 13 | validation, status | savedSalesReturnToolbar | Saved return toolbar: edit/add/print/journal/attachments enabled; delete disabled; ⋮ has the export and "دفع" |
| 14 | sales | userCanOpenEveryLineTabOfSalesReturn | Both line tabs of a saved return open |
| 15 | validation | userCannotSaveEmptyDocument | Saving an empty document is blocked: error toast, required fields marked, stays in Add |
| 16 | validation | userCannotSaveSalesReturnWithoutLines | "المستند المرجعي" set (not downloaded), no item line: saving is refused with "يجب اضافة صنف واحد على الأقل" |
| 17 | search | userCanSearchListByCode | List search by number shows that document |
| 18 | search | listSearchWithoutMatchShowsNoRows | List search for a text no document has shows no rows |
| 19 | search | userCanOpenDocumentFromRecordSearch | Record search (magnifier) in the form opens the document typed |
| 20 | navigation | userCanNavigateBetweenRecords | Record navigation in the form: first / next / back / last |
| 21 | pagination | userCanNavigateListPagination | List pager: first/last page, Next page by page to the end, back to a random page |
| 22 | list | userCanSwitchListToKanbanAndBack | Kanban shows the same documents as the list page; a card opens its document |
| 23 | list | userCanGroupListByStatus | Group by status: every group has documents and the groups add up to the list total |
| 24 | print | userCanPrintDocument | Print a random document: the preview shows its number; printed through Chrome's dialog |
| 25 | print | userCanSaveDocumentAsPdf | Save a random document as PDF: a PDF file is downloaded |
| 26 | excel | userCanExportDocumentToExcel | ⋮ → export to Excel: an .xlsx with the document's items is downloaded |
| 27 | follow-up | userCanOpenItemsPricingFromSalesReturn | Items Pricing ($): Sales → Pricing opens with the return's items |
| 28 | follow-up, print | userCanOpenPrintBarcodeFromSalesReturn | Print Barcode: Item Barcodes opens with the return's items |
| 29 | attachments | userCanAddAttachments | Attachments added in edit mode are listed and kept after save and reload |
| 30 | attachments | userCanDeleteAttachment | Deleting an attachment in edit mode is kept after save and reload |
| 31 | exit | userCanExitDocument | Close a new, a viewed and an edited document: each returns to the list |
| 32 | log | userCanViewTransactionLog | Transaction log: the first entry is the creation |
| 33 | lines | savedSalesReturnKeepsItsLines | Return from an API reference invoice keeps its items and typed quantities after save and reload |
| 34 | validation, quantity | userCannotReturnMoreThanInvoicedQuantity | Invoice ⋮ → انشاء مردود مبيعات; quantity = invoiced + 1 → refused, not saved |
| 35 | validation, quantity | cumulativeReturnsCannotExceedInvoicedQuantity | After a return of 1, the next return of the same invoice offers at most (invoiced − 1); returning the full invoiced quantity again is refused |
| 36 | validation, quantity | nonPositiveReturnQuantityIsRefused | Quantity 0 is refused; -1 is not kept by the cell or is refused on save |
| 37 | validation | userCannotSaveSalesReturnWithoutReturnPrice | "سعر المرتجع" is required: a return without it is not saved |
| 38 | validation | cashSalesReturnNeedsPaymentAccount | "نقدا" without "حساب الدفع" is refused ("حساب الدفع مطلوب") |
| 39 | create, reference | referenceDownloadBringsInvoiceCustomerBranchWarehouseAndItems | "المستند المرجعي" → download: customer, branch, warehouse = the invoice's (API); every line is an item of the invoice |
| 40 | create, follow-up | userCanSaveSalesReturnCreatedFromInvoiceMenu | Invoice ⋮ → انشاء مردود مبيعات → 1 of each item → saved as Approved; "المستند المرجعي" is the invoice |
| 41 | sales, calculation | returnPriceOptionsFillTheLinePrice | Each "سعر المرتجع" option gives the line a price > 0; with "تحديد يدوي" the price cell is editable |
| 42 | lines, edit | userCanChangeQuantityAndPriceOfSalesReturnLine | "تحديد يدوي": quantity and typed price of a line are saved as typed |
| 43 | lines | userCanDeleteItemLineOfSalesReturn | Deleting a downloaded line before saving is kept after save and reload |
| 44 | edit, lines | savedSalesReturnLinesAreLocked | Edit a saved return: quantity, price and item cells are locked; no "اضافة سطر" |
| 45 | edit | editedDescriptionOfSalesReturnIsKept | Edit a saved return: the new "البيان" is saved and kept after reload; it stays Approved |
| 46 | edit | userCanUndoDescriptionChangeOfSalesReturn | Change "البيان" in edit mode, then "تراجع" restores the saved one |
| 47 | status | savedSalesReturnShowsItsStatuses | A saved return shows its statuses (معتمد, التسليم, الدفع) and an approval date |
| 48 | journal, calculation | salesReturnJournalMatchesReturnTotal | "قيد اليومية" is balanced and one line credits the return total with tax × معامل التحويل |
| 49 | create, lines | userCanDownloadLinesFromAnotherSalesReturn | "انزال من" = مردود المبيعات → random return: its lines are added (not saved) |
| 50 | validation, excel | newSalesReturnMenuHasDisabledExcelImport | ⋮ of a new return: "استيراد من Execl" listed and disabled while the header is empty |
| 51 | follow-up | userCanOpenPaymentFromSalesReturnMenu | ⋮ → "دفع" on an unpaid return opens the payment dialog (not paid) |
| 52 | sales | customerAndBranchFillDependentFieldsOfSalesReturn | العميل fills الرقم الضريبي and حساب العميل (read-only); الفرع limits المخزن |
| 53 | create, reference, quantity | userCanReturnWhatIsLeftOfPartlyReturnedInvoice | Invoice that already has a return (API) → "المستند المرجعي" → download brings what is left → saved as Approved; a part of it is returned; "مبلغ الدفع اكبر من قيمة الفاتورة" is a defect (the payment amount follows the returned quantity) |
