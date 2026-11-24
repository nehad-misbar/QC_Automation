# عروض أسعار المبيعات – حالات الأتمتة

الشاشة: المبيعات ← العمليات ← عرض السعر (`/sales/price-quotations`)
الكود:
- `src/main/java/com/erp/automation/pages/sales/` → `SalesPage`, `SalesDocumentPage`, `SalesQuotationPage`
- `src/test/java/com/erp/automation/base/SalesBaseTest.java`
- `src/test/java/com/erp/tests/sales/SalesQuotationTest.java` (58 حالة)

التشغيل:
- كل الحالات: `mvn test -Dtest=SalesQuotationTest`
- مجموعة واحدة: `mvn test -Dtest=SalesQuotationTest -Dgroups=navigation`

## نتائج استكشاف الشاشة (QC – 2026-11-12)

| البند | ما تم رصده |
|---|---|
| المسار | أيقونة "المبيعات" ← `sales-operations` (العمليات) ← `sales-price-quotations` (عرض السعر) |
| بادئة data-testid | `price-quotations` – رقم العرض `#code` بصيغة `SQyyMM-nnnnnn` |
| الـ API | `POST /api/SalesQuotation/filter` – `recordStatus` 0 = مسودة، 1 = معتمد؛ `isCanceled`، `isInvoice` |
| أعمدة القائمة | الرقم المرجعي، مندوب المبيعات، العميل، التاريخ، تاريخ الانتهاء، الاجمالي، الإجمالي الاجنبي، العملة، التعادل، قائمة الأسعار، الحالة |
| أدوات القائمة | جديد، بحث، عرض جدول/كانبان، التصفيات (تجميع حسب)، المفضلة، ترقيم الصفحات (10 صفوف). لا يوجد "الملخصات" |
| قائمة إجراءات الصف (⋯) | "عرض" فقط |
| حقول الرأس | طريقة الشحن، الرقم المرجعي، العملة*، التعادل، شروط السداد، التاريخ، عنوان التوصيل، مندوب المبيعات، تاريخ اعتماد العرض، العميل*، الأسعار*، تعميم خصم على الأصناف (نسبة % / قيمة + مبلغ)، البيان، فريق المبيعات، الفرع*، الرقم الضريبي، طريقة احتساب الضريبة*، تاريخ الانتهاء، قالب عرض السعر، مركز التكلفة، قائمة الاسعار، المخزن*، الحالة، المستند المرجعي (نص حر)، انزال من، الرقم |
| الاعتماديات | العميل يملأ الرقم الضريبي؛ الفرع يحدد قائمة المخازن ويملأ مركز التكلفة ويقفله؛ "اضافة سطر" يظهر بعد العميل والفرع والمخزن |
| تبويبات الأصناف | "قائمة الأصناف" و"مميز" (أصناف اختيارية + مربع اختيار + زر "إضافة الأصناف المحددة إلى قائمة الأصناف") |
| أعمدة الأصناف | رمز الصنف، اسم الصنف، الوحدة، العبوة، الكمية، السعر، الإجمالي، الخصم%، الخصم، الاضافة%، الاضافة، معدل الضريبة، ضريبة القيمة المضافة، الإجمالي بعد الخصم والاضافة والضريبة |
| أسفل الشاشة | الإجماليات + "الشروط والاحكام" بزر "عرض" |
| ⋮ | الاستيراد من Excel (جديد) / التصدير الى Excel (محفوظ)، ارسال للعميل لغرض التوقيع، انشاء امر بيع |
| الحفظ | "هل انت متاكد من عملية الحفظ؟" ← نعم. إذا تجاوز تاريخ الانتهاء مهلة البيع (اليوم + 20 يوم) تظهر رسالة "هل تريد المتابعة؟" ← متابعة |
| الاعتماد | "هل تريد الاعتماد ؟" ← موافق؛ تاريخ الاعتماد = اليوم |
| انشاء امر بيع | يفتح `/sales/sales-order/add/<id>` والمستند المرجعي = رقم العرض |

## ملاحظات للمحللين (قد تسبب فشل حالات عن قصد)

1. **الحالة "منتهي"** تظهر في الفورم عندما يكون تاريخ الانتهاء أقدم من اليوم، بينما القائمة والـ API ما زالت تعرض مسودة/معتمد (مثال: SQ2608-000016). حالة #27 تسجل الفرق.
2. **زر الاعتماد مفعل على عرض منتهي** (SQ2608-000016، SQ2610-000033)، بخلاف عروض أسعار المشتريات → حالة #26 متوقع أن تفشل حتى يتم تأكيد القاعدة.
3. **زر الإلغاء مفعل على عرض معتمد تم تحويله لأمر بيع** (SQ2611-000039, isInvoice) → حالة #21 متوقع أن تفشل حتى يتم تأكيد القاعدة.
4. "ارسال للعميل لغرض التوقيع" كان معطلاً على كل العروض التي تم فحصها → حالة #44 تُتخطى (Skipped) حتى معرفة شرط تفعيله.
5. في وضع تعديل المسودة: تاريخ الانتهاء وطريقة الشحن وقالب عرض السعر والمستند المرجعي مقفلة (تُسجل في اللوج فقط).
6. ملف Excel المستخدم في الاستيراد: `test-data/import-excel/PriceQuotation.xlsx` (ملف خاص بعروض أسعار المبيعات من فريق QC).
7. **مندوب المبيعات**: مع أغلب المندوبين (مثل "vbfgg" و"jgh") تبقى قائمة "فريق المبيعات" فارغة، ولا تفتح "حفظ" نافذة التأكيد ولا تظهر أي رسالة (حالات #5 و#6 و#7 في تشغيل 12:24). الحالات تستخدم الآن **محمود السيد** لتجاوز المشكلة (يملأ "فريق مبيعات الرياض" تلقائياً). تم التحقق يدوياً في 2026-11-12.
8. **التراجع يمسح "البيان"**: بعد تعديل البيان ثم "تراجع" → "موافق" يصبح الحقل فارغاً بدلاً من القيمة المحفوظة (SQ2611-000041). القيمة المخزنة لا تتغير. حالة #14 متوقع أن تفشل حتى الإصلاح.
9. **نافذة معاينة الطباعة (v5.8.9)** لم تعد تحتوي زر "حفظ كـ PDF" (فقط الطباعة والتكبير والصفحات والإغلاق) → حالة #39 تُتخطى (Skipped) حتى تأكيد إن كان الحذف مقصوداً.
10. **قالب عرض السعر** يفرّغ "المخزن" ويجعل تاريخ الانتهاء اليوم؛ بدون مخزن يختفي زر "اضافة سطر". الحالة #56 تعيد اختيار الفرع والمخزن.
11. **حذف مرفق** يطلب الآن "تأكيد الحذف — هل انت متأكد من حذف المرفق؟" (نعم / إلغاء)، والأتمتة تضغط "نعم".
12. **تغيير العميل** في عرض فيه أصناف يسأل "سيتم حذف جميع الأصناف المضافة في حالة تغيير العميل، هل تريد الاستمرار؟"؛ الأتمتة تضغط "استمرار" ثم تضيف صنفاً من جديد.
13. **الأسطر**: الخلايا القابلة للتعديل في "قائمة الأصناف" و"مميز": الوحدة، الكمية، السعر، الخصم%، الخصم، الاضافة%، الاضافة، الكمية المجانية، البيان (و"مميز" فيها خانة التحديد). غير قابلة للتعديل: العبوة، الإجمالي، معدل الضريبة، ضريبة القيمة المضافة، الإجمالي بعد الخصم والاضافة والضريبة. حالات الإنشاء والتعديل تملأ كل هذه الخلايا في التابين.

> أثناء الاستكشاف تم حفظ واعتماد عرض واحد في QC: **SQ2611-000040** (البيان: "عرض سعر آلي - استكشاف الشاشة 20261112")، ولم يُحفظ أمر البيع الذي فُتح منه.

## الحالات

| # | المجموعة | الحالة | الهدف |
|---|---|---|---|
| 1 | navigation | userCanOpenSalesQuotationsFromSalesMenu | المسار المبيعات ← العمليات ← عرض السعر يفتح القائمة بأعمدتها وزر "جديد" |
| 2 | navigation | newButtonOpensEmptyQuotationWithDefaults | "جديد" يفتح عرضاً فارغاً برقم جديد وتاريخ اليوم والقيم الافتراضية |
| 3 | navigation, list | userCanOpenQuotationFromListRowActions | إجراءات الصف ⋯ ← عرض يفتح العرض |
| 4 | navigation, list | userCanOpenQuotationByDoubleClickOnListRow | النقر المزدوج على صف يفتح العرض بحالته |
| 5 | create | userCanCreateNewSalesQuotation | إنشاء عرض جديد: الرأس بترتيب الشاشة + انزال أصناف من عرض آخر + سطر صنف |
| 6 | create | userCanAddNewSalesQuotationFromOpenedOne | "إضافة" من عرض مفتوح تنشئ عرضاً جديداً ولا تغير المفتوح |
| 7 | create | userCanCopySalesQuotation | "تكرار" عرض وحفظه كعرض جديد |
| 8 | create, excel | userCanCreateSalesQuotationFromExcel | استيراد الأصناف من Excel |
| 9 | create, lines | userCanDownloadLinesFromAnotherSalesQuotation | انزال من "عروض أسعار المبيعات" ← الرقم ← تحميل الأصناف |
| 10 | edit | userCanEditDraftSalesQuotation | تعديل مسودة (كل الحقول القابلة + سطر) |
| 11 | edit, validation | draftEditModeHasExpectedEditableFields | الحقول القابلة وغير القابلة للتعديل في المسودة |
| 12 | edit, status | approvedSalesQuotationAllowsDescriptionEditOnly | العرض المعتمد: البيان فقط قابل للتعديل |
| 13 | edit, status | userCanEditExpiredDraftSalesQuotation | تعديل عرض منتهي |
| 14 | edit | userCanUndoSalesQuotationChanges | التراجع يعيد القيم المحفوظة |
| 15 | edit | editedDraftSalesQuotationHeaderIsKeptAfterSave | قيم الرأس تبقى بعد الحفظ وإعادة التحميل |
| 16 | status | userCanDeleteDraftSalesQuotation | حذف مسودة |
| 17 | status, log | userCanApproveDraftSalesQuotation | اعتماد مسودة + تاريخ الاعتماد + سجل العمليات + "انشاء امر بيع" مفعل |
| 18 | status, log | userCanDisapproveApprovedSalesQuotation | عدم اعتماد عرض معتمد غير مرتبط |
| 19 | cancel | userCanCancelDraftSalesQuotation | إلغاء مسودة |
| 20 | cancel | userCanCancelApprovedSalesQuotation | إلغاء معتمد غير مرتبط |
| 21 | cancel, validation | linkedApprovedSalesQuotationCannotBeCancelledOrDisapproved | المعتمد المحول لأمر بيع لا يُلغى ولا يُلغى اعتماده |
| 22 | validation | newSalesQuotationEnablesOnlyAllowedActions | أزرار شريط الأدوات في العرض الجديد |
| 23 | validation, status | draftSalesQuotationEnablesEditActions | أزرار المسودة |
| 24 | validation, status | approvedSalesQuotationCannotBeDeleted | أزرار المعتمد |
| 25 | validation, status | cancelledSalesQuotationIsReadOnly | الملغي للقراءة فقط |
| 26 | validation, status | expiredSalesQuotationCannotBeApproved | المنتهي لا يُعتمد ولا يُنشأ منه أمر بيع |
| 27 | validation, status | expiredQuotationShowsExpiredOnFormOnly | "منتهي" في الفورم مقابل الحالة في القائمة |
| 28 | validation | userCannotSaveSalesQuotationWithoutRequiredData | حفظ عرض فارغ: رسالة خطأ + تعليم العميل/الفرع/المخزن |
| 29 | validation | userCannotSaveSalesQuotationWithoutLines | حفظ بدون أصناف مرفوض |
| 30 | validation | expiryBeyondSalesPeriodAsksForConfirmation | تاريخ انتهاء بعد مهلة البيع يطلب تأكيداً، والإلغاء لا يحفظ |
| 31 | search | userCanSearchSalesQuotationsListByCode | البحث في القائمة بالرقم |
| 32 | search | listSearchWithoutMatchShowsNoRows | بحث بنص غير موجود لا يعرض صفوفاً |
| 33 | search | userCanOpenSalesQuotationFromRecordSearch | البحث عن سجل (العدسة) داخل الفورم |
| 34 | navigation | userCanNavigateBetweenSalesQuotationRecords | التنقل بين السجلات: الأول/التالي/السابق/الأخير |
| 35 | pagination | userCanNavigateSalesQuotationsPagination | التنقل بين صفحات القائمة |
| 36 | list | userCanSwitchSalesQuotationsListToKanbanAndBack | عرض كانبان والعودة للجدول |
| 37 | list | userCanGroupSalesQuotationsListByStatus | التجميع حسب الحالة |
| 38 | print | userCanPrintSalesQuotation | الطباعة |
| 39 | print | userCanSaveSalesQuotationAsPdf | الحفظ PDF |
| 40 | excel | userCanExportSalesQuotationToExcel | التصدير إلى Excel |
| 41 | follow-up | userCanOpenItemsPricingFromSalesQuotation | تسعير الأصناف ($) |
| 42 | follow-up, print | userCanOpenPrintBarcodeFromSalesQuotation | طباعة الباركود |
| 43 | follow-up | userCanCreateSalesOrderFromApprovedSalesQuotation | انشاء امر بيع من عرض معتمد (بدون حفظ) |
| 44 | follow-up | userCanOpenSendForSignatureFromApprovedSalesQuotation | ارسال للعميل لغرض التوقيع (بدون إرسال) |
| 45 | attachments | userCanAddAttachmentsToSalesQuotation | إضافة مرفقات |
| 46 | attachments | userCanDeleteAttachmentFromSalesQuotation | حذف مرفق |
| 47 | exit | userCanExitSalesQuotation | الخروج من عرض جديد/معروض/قيد التعديل |
| 48 | log | userCanViewSalesQuotationTransactionLog | سجل العمليات |
| 49 | lines | savedSalesQuotationKeepsItsLines | الأصناف تبقى بعد الحفظ |
| 50 | lines, edit | userCanEditExistingLineOfSalesQuotation | تعديل الكمية والسعر والخصم% لسطر |
| 51 | lines | userCanDeleteItemLineOfSalesQuotation | حذف سطر صنف |
| 52 | sales, lines | userCanMoveOptionalItemsToItemsList | نقل صنف من "مميز" إلى "قائمة الأصناف" |
| 53 | sales, lines | optionalItemIsSavedInFeaturesTab | صنف "مميز" غير منقول يُحفظ في "مميز" فقط |
| 54 | sales | userCanShowTermsAndConditions | الشروط والاحكام ← عرض |
| 55 | sales | userCanApplyBulkDiscountPercentage | تعميم خصم على الأصناف (نسبة %) |
| 56 | sales | userCanChooseQuotationTemplate | قالب عرض السعر |
| 57 | sales | referenceTextAndDeliveryAddressAreKept | المستند المرجعي (نص حر) وعنوان التوصيل يُحفظان |
| 58 | sales | customerAndBranchFillDependentFields | العميل يملأ الرقم الضريبي والفرع يحدد المخازن |
