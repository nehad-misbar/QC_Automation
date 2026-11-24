# التسعير وطلبات تعديل الأسعار – حالات الأتمتة

الكود:
- الصفحات: `pages/sales/PricingOperationPage` (النموذج المشترك)، `PricingPage`، `PriceChangeRequestPage`
- الحالات: `tests/sales/PricingTest` (35 حالة) و`tests/sales/PriceChangeRequestTest` (28 حالة). الحالات العامة تأتي من `SalesDocumentBaseTest`، وما لا يناسب الشاشتين منها معطّل ومستبدل بحالات خاصة.

التشغيل: `mvn test -Dtest=PricingTest` / `mvn test -Dtest=PriceChangeRequestTest`

**الاختبارات لا تعيد تسعير "القائمة الافتراضية" أبداً**، لأن الشاشات الأخرى تعتمد على أسعارها. كل حالة تختار قائمة أسعار أخرى.

## نتائج الاستكشاف (QC – 2026-11-12)

| البند | التسعير | طلبات تعديل الأسعار |
|---|---|---|
| المسار | `/sales/pricing` | `/sales/pricing-request` |
| بادئة data-testid | `pricing` | `pricing-request` |
| الـ API | `SalePriceOperation/filter` (62: 10 مسودة، 52 معتمد) | `SalePriceOperationRequestV/filter` (7: 5 معتمد، 2 مسودة) |
| الرقم | `#priceOperationCode` (رقم بسيط) | نفسه |
| البيان | data-testid `statement` (بدون id) | نفسه |
| أعمدة القائمة | رمز، التاريخ، نوع الصلاحية، سياسة التعديل، التطبيق على، أساس المقارنة، هامش الربح، هامش الربح قيمة، نسبة هامش الربح، طريقة التسعير. **لا يوجد عمود حالة** | التاريخ، الفرع، سياسة التعديل، التطبيق على، فئات الأصناف، الأصناف، أساس المقارنة، الحالة، هامش الربح، هامش الربح قيمة. **لا يوجد عمود رقم** |
| ⋮ | الاستيراد من Excel (جديد) / التصدير الى Excel | التصدير الى Excel |
| عمود إضافي في الأصناف | – | "موافقة" (مربع اختيار) |

**النموذج (مشترك بين الشاشتين):**
- الحقول: قوائم الأسعار*، آلية إدخال الأصناف (يدوي / تلقائي)، أساس المقارنة* (4 خيارات)، التطبيق على، الأصناف (اختيار متعدد)، سياسة التعديل (2)، هامش الربح (نسبة / قيمة)، تسعير الأصناف (الكل / المسعرة / الغير مسعرة)، التعامل بكافة الوحدات، طريقة التطبيق* (يدوي / تخفيض / زيادة / معادلة)، انزال من.
- "يدوي" يُظهر زر "اضافة سطر". "تلقائي" يفتح حقول التطبيق على / الأصناف / تسعير الأصناف / انزال من، وزر "انزال" يحمّل الأصناف المختارة.
- زيادة 10% غيّرت السعر بعد التعديل من 20 إلى 22 ومن 100 إلى 110.
- أعمدة الأسطر: الفارق = السعر − التكلفة، والسعر شامل الضريبة = السعر × 1.15، ونسبة هامش الربح = الفارق ÷ التكلفة (−70 ÷ 90 = −77.78%).
- الحفظ: "هل انت متاكد؟". إذا كان سعر أقل من التكلفة تظهر "أصناف الفارق فيها بالسالب" مع قائمة الأصناف، وخياران: "نعم، متأكد من الحفظ" / "لا، مراجعة الأصناف". بعد الحفظ تصبح العملية مسودة.
- الاعتماد: "هل تريد الاعتماد ؟". في أول اعتماد ظهر سؤال "توجد أصناف مسعرة مسبقا"، وخياراته: المتابعة في الإعتماد / تجاهل الأصناف المسعرة / إنشاء طلب تعديل الأسعار. في الاعتماد الثاني لم يظهر السؤال.
- بعد الاعتماد تتعطل أزرار التعديل والحذف والاعتماد، ولا يوجد إلغاء ولا عدم اعتماد.

مستندات حُفظت في QC أثناء الاستكشاف (قائمة "قائمه د" وليس القائمة الافتراضية):
- **تسعير 64**: زيادة 10% للصنفين 6 و2، ثم اعتُمد.
- **تسعير 65**: زيادة 5% لنفس الصنفين، ثم اعتُمد.

## ملاحظات للمحللين (قد تسبب فشل حالات عن قصد)

1. **طلب التعديل الأول** (الرمز 1، معتمد) يفتح من القائمة على `/view/0` بنموذج فارغ وحالة "ملغي". السبب أن الـ API يعيد id يساوي 0. الحالة `everyListedRequestOpensWithItsNumberAndStatus` ستفشل حتى يُصلح.
2. ~~حفظ طلب تعديل بدون أصناف لا يُظهر أي رسالة~~ — أُصلح (QC 2026-11-23): تظهر "يجب اضافة صنف واحد على الأقل".
3. **سؤال "أصناف مسعرة مسبقا" لا يظهر بشكل ثابت.** الحالة `approvingAlreadyPricedItemsCanCreateAPriceChangeRequest` تُتخطى إذا لم يظهر، ونحتاج من المحللين شرط ظهوره.
4. **معادلة "معادلة" غير موثقة.** الحالة تتحقق فقط أن الأسعار تغيرت وحُفظت، وتسجّل في اللوج الفرق عن السعر × (1 − الخصم) + المبلغ.
5. **قائمة التسعير بلا عمود حالة وقائمة الطلبات بلا عمود رقم.** لذلك عطّلتُ حالات البحث بالرقم والتجميع وكانبان التي لا تنطبق، واستبدلتها بحالات مناسبة.

## حالات التسعير

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | listOpensFromSalesMenu | المبيعات → العمليات → the screen link opens its list with its columns and "جديد" |
| 2 | navigation | newButtonOpensEmptyDocument | "جديد" opens an empty document: a new number, today's date, no customer, no status |
| 3 | navigation, list | userCanOpenDocumentFromListRowActions | List row ⋯ → "عرض" opens that document |
| 4 | navigation, list | userCanOpenPricingByDoubleClickOnListRow | Double-click on a list row opens that pricing operation |
| 5 | create | userCanCreateManualPricing | Manual entry (يدوي): header, two item lines with a price; saved as Draft and the lines are kept |
| 6 | create | userCanCreateAutomaticPricing | Automatic entry (تلقائي): items chosen in "الأصناف" are loaded with "انزال"; saved as Draft |
| 7 | calculation | increaseRaisesEveryPriceByTheRate | طريقة التطبيق = زيادة 10%: every "السعر بعد التعديل" becomes base × 1.10 and is kept after save |
| 8 | calculation | decreaseLowersEveryPriceByTheRate | طريقة التطبيق = تخفيض 10%: every "السعر بعد التعديل" becomes base × 0.90 and is kept after save |
| 9 | calculation | formulaChangesThePrices | طريقة التطبيق = معادلة (خصم 10%، مبلغ إضافي 5): the prices change and are kept after save |
| 10 | calculation | lineCalculationsAreConsistent | Line calculations: الفارق = السعر − التكلفة; السعر شامل الضريبة = السعر × (1 + الضريبة); هامش الربح قيمة = الفارق; نسبة هامش الربح = الفارق ÷ التكلفة × 100 |
| 11 | validation | priceBelowCostAsksForConfirmation | A price below the cost asks "أصناف الفارق فيها بالسالب" listing the item; "لا، مراجعة الأصناف" leaves the operation unsaved |
| 12 | validation | itemEntryModeSwitchesTheFields | يدوي shows "اضافة سطر" and locks the automatic fields; تلقائي unlocks التطبيق على / الأصناف / تسعير الأصناف / انزال من and hides "اضافة سطر" |
| 13 | validation | applicationMethodShowsItsFields | Each "طريقة التطبيق" shows its own fields: زيادة → نوع الزيادة + النسبة; تخفيض → نوع التخفيض + النسبة; معادلة → خصم + مبلغ إضافي |
| 14 | validation | pricingOptionListsAreComplete | Option lists: أساس المقارنة (4), طريقة التطبيق (4), سياسة التعديل (2) |
| 15 | edit | userCanEditDraftPricing | Edit a Draft: description and a line price change and are kept; it stays a Draft |
| 16 | edit | userCanUndoPricingChanges | Edit a Draft, then Undo restores the saved description and lines |
| 17 | status | userCanDeleteDraftPricing | Delete a Draft: success message; not returned by the API any more |
| 18 | status | userCanApproveDraftPricing | Approve a Draft: Approved after reload; edit, delete and approve disabled |
| 19 | status, request | approvingAlreadyPricedItemsCanCreateAPriceChangeRequest | Approving items that are already priced and choosing "إنشاء طلب تعديل الأسعار" creates a new price change request |
| 20 | validation | newPricingEnablesOnlyAllowedActions | New operation toolbar: save waits for a change; add/undo/delete/print/approve/log disabled |
| 21 | validation, status | draftPricingEnablesEditActions | Draft toolbar: add/edit/delete/approve/attachments/log enabled; save disabled |
| 22 | validation, status | approvedPricingIsLocked | Approved toolbar: edit/delete/approve disabled (no disapprove, no cancel) |
| 23 | validation | userCannotSavePricingWithoutLines | Saving an operation without lines is not possible and says why |
| 24 | search | userCanSearchListByCode | List search by number shows that document |
| 25 | search | listSearchWithoutMatchShowsNoRows | List search for a text no document has shows no rows |
| 26 | search | userCanOpenDocumentFromRecordSearch | Record search (magnifier) in the form opens the document typed |
| 27 | navigation | userCanNavigateBetweenRecords | Record navigation in the form: first / next / back / last |
| 28 | pagination | userCanNavigateListPagination | List pager: first/last page, Next page by page to the end, back to a random page |
| 29 | print | userCanPrintDocument | Print a random document: the preview shows its number; printed through Chrome's dialog |
| 30 | print | userCanSaveDocumentAsPdf | Save a random document as PDF: a PDF file is downloaded |
| 31 | excel | userCanExportDocumentToExcel | ⋮ → export to Excel: an .xlsx with the document's items is downloaded |
| 32 | attachments | userCanAddAttachments | Attachments added in edit mode are listed and kept after save and reload |
| 33 | attachments | userCanDeleteAttachment | Deleting an attachment in edit mode is kept after save and reload |
| 34 | exit | userCanExitDocument | Close a new, a viewed and an edited document: each returns to the list |
| 35 | log | userCanViewTransactionLog | Transaction log: the first entry is the creation |

## مراجعة طلبات تعديل الأسعار على QC (2026-11-23)

ما تم التأكد منه يدوياً:
- الطلب رقم 1 ما زال يفتح على `/view/0` بنموذج فارغ وحالة "ملغي". زر "الأول" في النموذج يفتح نفس الطلب، لذلك فشل `userCanOpenDocumentFromRecordSearch` في تشغيل 2026-11-23 (انتظر رقم الطلب 30 ثانية). الحالة الآن تترك الطلب بـ"الأخير" أو "السابق"، و`userCanNavigateBetweenRecords` تفشل برسالة واضحة عند "الأول" حتى يُصلح.
- **أزرار التنقل بين السجلات** تعمل بشكل طبيعي عند فتح الطلب من القائمة. تم التحقق يدوياً: الصف 10، ثم "الأخير" (20)، ثم "السابق" (19)، والأزرار كلها متاحة. الخطأ الوحيد هو "الأول"، فهو يفتح `/view/0`. سبب فشل تشغيل 12:51 أن الحالات المشتركة تفتح الطلب برابطه مباشرة (`/view/<id>`)، وعندها لا يكون للنموذج قائمة: عداد السجلات يظهر 1 والأزرار تتعطل بعد أول انتقال. لذلك أصبحت `userCanOpenDocumentFromRecordSearch` و`userCanNavigateBetweenRecords` تفتحان الطلب من القائمة (أي صف غير الأول).
- **الحفظ بدون أصناف أصبح يُمنع** برسالة "يجب اضافة صنف واحد على الأقل"، ويبقى النموذج على `/add`. لذلك حُذفت الحالة الخاصة `userCannotSaveRequestWithoutLines`، وأُعيد تفعيل الحالة المشتركة `userCannotSaveEmptyDocument`. كانت الحالة المحذوفة تتحقق من عدم الحفظ عن طريق "البيان"، والبيان لا يُحفظ، فكانت تنجح دائماً. الطلبات 14 و15 و16 محفوظة بدون أصناف، وهي غالباً من قبل الإصلاح.
- **"البيان" لا يُحفظ**: الطلب 17 حُفظ بـ "QC-Claude coverage check" وظهر فارغاً بعد الحفظ، وفي الـ API أيضاً (`note = ""`). قائمة الأسعار تُحفظ. لذلك أصبح `verifiesNoteOnSave()` يعيد false، وأُضيفت الحالة `descriptionIsKeptAfterSave` لتفشل وحدها حتى يُصلح الخطأ.
- **الاعتماد يحتاج قراراً لكل صنف**: الاعتماد يُرفض برسالة "لا يمكن الاعتماد، توجد أصناف لم يتم تحديد موافقة أو رفض لها" إذا وُجد صنف بلا قرار. في وضع التعديل يظهر ثلاثة أعمدة للأصناف: "موافقة" و"رفض" و"سبب الرفض"، سواء كانت آلية الإدخال يدوي أو تلقائي. هذه الأعمدة هي آخر أعمدة الجدول، ولا تظهر إلا بالتمرير إليها. تحديد أحد المربعين يعطّل الآخر في نفس السطر. تم التحقق من ذلك على الطلب 19 (تلقائي: رفض الصنف 6 مع سبب، وموافقة الصنف 2) وعلى الطلب 17 (يدوي: رفض الصنف 2 بدون سبب)، ثم الحفظ والاعتماد، فأصبح كلاهما "معتمد". الحالة `userCanApproveRequestAfterDecidingEveryLine` تغطي هذا المسار على طلب يدوي. "رفض" بدون سبب مقبول (يحتاج تأكيد)، و`rejectedAt` يبقى null بعد الرفض.
- القائمة فيها عمود "نسبة هامش الربح" (`profitMarginRate`)، وأُضيف لقائمة الأعمدة. عمودا "الفرع" و"الأصناف" فارغان في كل الصفوف رغم أن الطلبات فيها أصناف (يحتاج تأكيد).
- ⋮ في الطلب الجديد: الاستيراد من Excel / التصدير الى Excel، فأُضيفت `userCanImportRequestLinesFromExcel`.
- "انزال من" (أمر توريد، أمر استلام، أمر تسليم، أمر شراء، فاتورة شراء محلية، تحويل مخزني) غير مغطى في الشاشتين.

- **حالتان مشتركتان أُعيدت كتابتهما بموقع الصف بدل الرقم:** `userCanOpenDocumentFromListRowActions` (⋯ ← عرض لصف غير الأول: يفتح الطلب برقم وبحالة صفه)، و`userCanSwitchListToKanbanAndBack` (بطاقة لكل طلب في صفحة القائمة وبنفس الترتيب، والبطاقة 2 فتحت الطلب 2). كل البطاقات تعرض "غير معتمد" بينما القائمة فيها 9 معتمد و1 مسودة، فحالة كانبان ستفشل في فحص الحالات حتى يُصلح ذلك (نفس خطأ التسعير). بقيت ثلاث حالات مشتركة معطّلة: النقر المزدوج والترقيم (لهما بديل) والبحث بالرقم (لا يمكن).
حالات حُذفت: `userCanCreateAutomaticPriceChangeRequest` (الإدخال التلقائي والزيادة نفس مكون التسعير ومغطاة في `PricingTest`)، و`userCannotSaveRequestWithoutLines`، و`userCanApproveDraftPriceChangeRequest`.
حالة الحذف أصبحت تحذف مسودة ينشئها الاختبار نفسه، وليس مسودة من بيانات QC.

## حالات طلبات تعديل الأسعار

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | listOpensFromSalesMenu | المبيعات → العمليات → the screen link opens its list with its columns and "جديد" |
| 2 | navigation | newButtonOpensEmptyDocument | "جديد" opens an empty document: a new number, today's date, no status |
| 3 | list | everyListedRequestOpensWithItsNumberAndStatus | Every request in the list opens (double-click) with a number and the status shown in the list |
| 4 | pagination | requestListPagerMatchesTheApi | The pager total equals the number of requests in the API; the rows match the page range |
| 5 | create | userCanCreateManualPriceChangeRequest | Manual request: header and two item lines; saved as Draft; price list and lines kept |
| 6 | create | descriptionIsKeptAfterSave | "البيان" is shown after save and reload, and saved in the API |
| 7 | create, excel | userCanImportRequestLinesFromExcel | ⋮ → الاستيراد من Excel (Pricing.xlsx) loads the items with their prices; kept after save and reload |
| 8 | edit | userCanEditDraftPriceChangeRequest | Edit a Draft request: a line price changes and is kept |
| 9 | status | userCanDeleteDraftPriceChangeRequest | Delete a Draft created by the test: success message; gone from the API |
| 10 | status, validation | approvingWithoutLineDecisionsIsRefused | Approving with undecided lines is refused with a message; stays a Draft |
| 11 | status | userCanApproveRequestAfterDecidingEveryLine | Manual request: موافقة / رفض + سبب الرفض per line in edit mode (one box disables the other); refused while a line is undecided; approved once every line is decided |
| 12 | status, validation | approvedRequestIsLockedAndEveryLineIsDecided | Approved request: edit/delete/approve disabled; every line is موافقة or رفض |
| 13 | validation | newPriceChangeRequestEnablesOnlyAllowedActions | New request toolbar: save waits for a change; add/undo/delete/print/approve/log disabled; ⋮ has import |
| 14 | validation, status | draftPriceChangeRequestEnablesEditActions | Draft toolbar: add/edit/delete/approve/print/attachments/log enabled; save disabled |
| 15 | validation | userCannotSaveEmptyDocument | Saving without lines: "يجب اضافة صنف واحد على الأقل", no confirmation, stays in Add |
| 16 | search | listSearchWithoutMatchShowsNoRows | List search for a text no document has shows no rows |
| 17 | search | userCanOpenDocumentFromRecordSearch | Opened from the list; record search (magnifier) opens the request typed (leaves it with "الأخير"/"السابق") |
| 18 | navigation | userCanNavigateBetweenRecords | Opened from the list: الأخير / السابق / التالي / الأول open the right request ("الأول" fails until the /view/0 bug is fixed) |
| 19 | list | userCanGroupListByStatus | Group by status: every group has documents and the groups add up to the list total |
| 20 | print | userCanPrintDocument | Print a random document: the preview shows its number |
| 21 | print | userCanSaveDocumentAsPdf | Save a random document as PDF: a PDF file is downloaded |
| 22 | excel | userCanExportDocumentToExcel | ⋮ → export to Excel: an .xlsx with the document's items is downloaded |
| 23 | attachments | userCanAddAttachments | Attachments added in edit mode are kept after save and reload |
| 24 | attachments | userCanDeleteAttachment | Deleting an attachment in edit mode is kept after save and reload |
| 25 | exit | userCanExitDocument | Close a new, a viewed and an edited document: each returns to the list |
| 26 | log | userCanViewTransactionLog | Transaction log: the first entry is the creation |
