import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import { Filesystem, Directory } from '@capacitor/filesystem';
import { Share } from '@capacitor/share';
import { Contract, LandlordDetails, Property, Unit, Tenant, Payment } from '../types';
import { calculateDurationText, formatPDFDate } from '../utils/date';
import { toArabicIndicDigits } from '../utils/number';

export interface ContractPDFData {
  contract: Contract;
  unit: Unit;
  property: Property;
  landlord: LandlordDetails;
  templateText: string;
  tenant?: Tenant;
}

// Replace placeholders in contract template
export function fillContractTemplate(data: ContractPDFData): string {
  const { contract, unit, property, landlord, templateText, tenant } = data;
  const durationText = calculateDurationText(contract.startDate, contract.endDate, 'ar');
  const contractDate = formatPDFDate(contract.contractDate || contract.startDate, true);
  const startDate = formatPDFDate(contract.startDate, true);
  const endDate = formatPDFDate(contract.endDate, true);
  const rent = toArabicIndicDigits(contract.monthlyRent);
  const deposit = toArabicIndicDigits(contract.depositAmount || contract.monthlyRent);

  const effTenantName = tenant?.fullName || contract.tenantName || '';
  const effNationalId = tenant?.nationalId || contract.nationalId || '';
  const effJob = tenant?.job || contract.tenantJob || 'موظف';
  const effAddress = tenant?.address || contract.tenantAddress || 'الفيوم';
  const effNationality = tenant?.nationality || contract.tenantNationality || 'مصري';
  const nationalIdStr = effNationalId ? toArabicIndicDigits(effNationalId) : '....................';

  let filled = templateText
    .replace(/{contractDate}/g, contractDate)
    .replace(/{landlordName}/g, landlord.name || 'جمال حامد عبد الوهاب')
    .replace(/{landlordJob}/g, landlord.job || 'مهندس مدني حر')
    .replace(/{landlordAddress}/g, landlord.address || '')
    .replace(/{landlordNationality}/g, landlord.nationality || 'مصري')
    .replace(/{tenantName}/g, effTenantName)
    .replace(/{tenantJob}/g, effJob)
    .replace(/{tenantAddress}/g, effAddress)
    .replace(/{tenantNationality}/g, effNationality)
    .replace(/{nationalId}/g, nationalIdStr)
    .replace(/{idIssuedFrom}/g, contract.idIssuedFrom || 'الفيوم')
    .replace(/{unitDescription}/g, unit.description || 'شقة سكنية')
    .replace(/{usagePurpose}/g, contract.usagePurpose || (property.type === 'shops' ? 'محل تجاري' : 'سكن عائلي'))
    .replace(/{propertyAddress}/g, property.address || '')
    .replace(/{rent}/g, rent)
    .replace(/{paymentTerms}/g, contract.paymentTerms || 'ويسدد الايجار في أول كل شهر بموجب ايصالات بتوقيع المالك او الوكيل')
    .replace(/{durationText}/g, durationText)
    .replace(/{startDate}/g, startDate)
    .replace(/{endDate}/g, endDate)
    .replace(/{renewalTerms}/g, contract.renewalTerms || 'موافقه كتابية صريحة من المالك')
    .replace(/{water}/g, contract.water || 'على المستأجر')
    .replace(/{electricity}/g, contract.electricity || 'على المستأجر')
    .replace(/{stairsLight}/g, contract.stairsLight || 'على المستأجر')
    .replace(/{doormanFee}/g, contract.doormanFee || 'لا يوجد')
    .replace(/{depositAmount}/g, deposit)
    .replace(/{extraClause}/g, contract.extraClause || 'لا يوجد');

  return filled;
}

// Generate A4 Arabic 2-page PDF
export async function generateAndShareContractPDF(data: ContractPDFData): Promise<boolean> {
  const { contract, property, unit, landlord, tenant } = data;
  const durationText = calculateDurationText(contract.startDate, contract.endDate, 'ar');
  const contractDate = formatPDFDate(contract.contractDate || contract.startDate, true);
  const startDate = formatPDFDate(contract.startDate, true);
  const endDate = formatPDFDate(contract.endDate, true);
  const rent = toArabicIndicDigits(contract.monthlyRent);
  const deposit = toArabicIndicDigits(contract.depositAmount || contract.monthlyRent);
  
  const effTenantName = tenant?.fullName || contract.tenantName || '';
  const effNationalId = tenant?.nationalId || contract.nationalId || '';
  const effJob = tenant?.job || contract.tenantJob || 'موظف';
  const effAddress = tenant?.address || contract.tenantAddress || 'الفيوم';
  const effNationality = tenant?.nationality || contract.tenantNationality || 'مصري';
  const nationalId = effNationalId ? toArabicIndicDigits(effNationalId) : '....................';

  // Create an offscreen DOM element formatted as exactly 2 A4 pages
  const container = document.createElement('div');
  container.style.position = 'fixed';
  container.style.top = '-9999px';
  container.style.left = '-9999px';
  container.style.width = '794px'; // standard A4 width at 96 DPI
  container.style.backgroundColor = '#ffffff';
  container.style.color = '#111827';
  container.style.fontFamily = "'Cairo', sans-serif";
  container.style.direction = 'rtl';
  container.style.textAlign = 'right';
  container.style.zIndex = '-1000';

  // Parse clauses from template
  const isShop = property.type === 'shops';
  const clauses = isShop
    ? [
        { num: '١', title: 'موضوع العقد', text: 'بموجب هذا العقد وبعد قبول المستأجر قد أجر المالك للمستأجر العين الموضح وصفها بصدد هذا العقد وكذا بالشروط الموضحة بصدد هذا العقد.' },
        { num: '٢', title: 'صلاحية المكان', text: 'لا يجوز للمستأجر استعمال المكان موضوع هذا العقد بالصورة التي تنافي شروطه او الغرض منه.' },
        { num: '٣', title: 'صلاحية المكان المستأجر', text: 'يقر المستأجر بأن المكان موضوع هذا العقد يصلح للأغراض التي استؤجر من اجلها وانه يلائم صحته وصحة افراد عائلته.' },
        { num: '٤', title: 'استلام المكان وأعادته', text: 'المستأجر يقر انه استلم المكان مستوفيا لكل لوازمه وهو يتعهد بالمحافظة عليه كما يحافظ على ملكه الخاص.' },
        { num: '٥', title: 'أحدث أي تغيرات بالمكان', text: 'محظور على المستأجر ان يحدث أي تغيير في المكان المؤجر له الا بتصريح كتابي من المالك وإذا خالف ذلك يفسخ العقد تلقائيا.' },
        { num: '٦', title: 'تأجير المكان من الباطن او التنازل عنه للغير', text: 'محظور على المستأجر ان يتنازل عن ايجارته او يؤجر من الباطن كل او جزء من المكان المؤجر مهما كانت الأسباب بمقابل او بغير مقابل، ولا أي شخص حتى لأقرب اقربائه وإذا خالف المستأجر ذلك يكون هذا العقد مفسوخا من تلقاء نفسه.' },
        { num: '٧', title: 'محتويات المكان', text: 'يقر المستأجر بأن كل الأشياء الموجودة بالمكان المؤجر هي ملكا خاصا له وهي ضمان للمالك للوفاء بكافة الحقوق التي تثبت له بموجب هذا العقد وطبقا لنص المادة رقم ٥٨٩ من القانون المدني يحق له حبسها وان يمنع نقلها حتى ولو لم يكن قد وقع حجزا عليها وحقه استردادها من الحائز عليها إذا نقلت من المكان ولو كان حسن النية.' },
        { num: '٨', title: 'تهديد سلامة العقار', text: 'يحظر على المستأجر أن يضع في المكان المؤجر له اية مواد ملتهبة او مضرة بالصحة.' },
        { num: '٩', title: 'الإصلاحات والترميمات', text: 'كافة الإصلاحات والترميمات الجوهرية وكذا الترميمات الايجارية يلتزم بها كل من المالك والمستأجر بالشروط المنصوص عليها بالقانون.' },
        { num: '١٠', title: 'توقيع الحجز التحفظي', text: 'يحق للمالك توقيع الحجز التحفظي على منقولات المستأجر في حالة تأخره في سداد الايجار وذلك دون انذار سابق.' },
        { num: '١١', title: 'التأخير في سداد الايجار وفسخ العقد', text: 'يفسخ هذا العقد فورا وتلقائيا وبدون تنبيه او انذار في حالة تأخير المستأجر في سداد قسط واحد من قيمة الايجار في ميعاد استحقاقه وللمالك الحق في رفع دعوي امام قاضي الأمور المستعجلة لاستصدار الحكم لأخلاء المستأجر بمجرد التثبت من التأخير في السداد اذ ان المستأجر في هذه الحالة يعتبر غاصبا للمكان وشاغلا له دون سند ولا يجوز للمستأجر ان يؤخر سداد أقساط الايجار لأي سبب او يدعي مقاصته مع أي مصاريف كتصليحات او خلافه او ان يطلب تخفيضه أو يقوم بإيداعه خزينة المحكمة الا في حالة توقيع حجزا نهائيا ضد المالك بموجب حكم تحت يد المستأجر.' },
        { num: '١٢', title: 'في حالة الوفاة والافلاس والإعسار', text: 'لا يترتب على وفاة المالك او المستأجر فسخ هذا العقد لكن في حالات وفاة المستأجر وعدم ترك أقرباء له كانوا يقيمون معه في نفس المكان المؤجر له، وممن لهم الحق في ذلك او ان التعاقد قد تم معه بسبب حرفته او مهنته او لاعتبارات خاصة تتعلق بشخصه او إذا أفلس او اعسر فللمالك الحق في فسخ هذا العقد واسترداد المكان.' },
        { num: '١٣', title: 'معاينة التوصيلات', text: 'يقر المستأجر بأنه قد عاين المعاينة النافية للجهالة توصيلات الكهرباء والمياه والغاز وكذلك العدادات الخاصة بها وتأكد من عدم مخالفتها للقوانين، وأي عبث او تلاعب بهذه التوصيلات والعدادات يعتبر هذا العقد مفسوخا تلقائيا ويتحمل المستأجر وحده ما يترتب عن هذا العبث من مخالفات قانونية.' },
        { num: '١٤', title: 'أحكام القوانين', text: 'ما لم يذكر في هذا العقد يخضع لأحكام القانون المدني.' },
        { num: '١٥', title: 'الاختصاص', text: 'محكمة الأمور المستعجلة هي التي تخص المستأجر في حالة مخالفته لشروط هذا العقد او بسبب انتهاء مدته كما يكون الاختصاص المحلي للمحكمة التابع لها المكان المؤجر.' },
        { num: '١٦', title: 'قانونية التخاطب الرسمي على الموطن المختار', text: 'يقر كل من المالك والمستأجر صراحة انه اتخذ موطنا مختارا له في العنوان المذكور بصدد هذا العقد وكل تخاطب رسمي عليه يعتبر قانونيا.' },
        { num: '١٧', title: 'صور العقد', text: 'تحرر هذا العقد من صورتين بيد كل من المتعاقدين صورة للعمل بها عند اللزوم.' },
        { num: '١٨', title: 'بند إضافي', text: contract.extraClause || 'لا يوجد' },
      ]
    : [
        { num: '١', title: 'موضوع العقد', text: 'بموجب هذا العقد وبعد قبول المستأجر قد أجر المالك للمستأجر العين الموضح وصفها بصدد هذا العقد وكذا بالشروط الموضحة بصدد هذا العقد.' },
        { num: '٢', title: 'صلاحية المكان', text: 'لا يجوز للمستأجر استعمال المكان موضوع هذا العقد بالصورة التي تنافي شروطه او الغرض منه.' },
        { num: '٣', title: 'صلاحية المكان المستأجر', text: 'يقر المستأجر بأن المكان موضوع هذا العقد يصلح للأغراض التي استؤجر من اجلها وانه يلائم صحته وصحة افراد عائلته.' },
        { num: '٤', title: 'استلام المكان وأعادته', text: 'المستأجر يقر انه استلم المكان مستوفيا لكل لوازمه وهو يتعهد بالمحافظة عليه كما يحافظ على ملكه الخاص.' },
        { num: '٥', title: 'أحدث أي تغيرات بالمكان', text: 'محظور على المستأجر ان يحدث أي تغيير في المكان المؤجر له الا بتصريح كتابي من المالك وإذا خالف ذلك يفسخ العقد تلقائيا.' },
        { num: '٦', title: 'إقرار السكن العائلي', text: 'يقر المستأجر بأن الشقة محل العقد ليست مسكن زوجية كما يلتزم بعدم تمكين أي شخص سواء من اقاربه او اصهاره او الغير من الإقامة بالعين المؤجرة او استخدامها كمسكن زوجية الا بعد موافقة كتابية مسبقة من المالك ويعد مخالفة هذا البند سببا كافيا لفسخ العقد فورا دون تنبه او انذار مع احتفاظ المالك بحقه في أقامه دعوي طرد واسترداد العين المؤجرة.' },
        { num: '٧', title: 'تأجير المكان من الباطن او التنازل عنه للغير', text: 'محظور على المستأجر ان يتنازل عن ايجارته او يؤجر من الباطن كل او جزء من المكان المؤجر مهما كانت الأسباب بمقابل او بغير مقابل، ولا أي شخص حتى لأقرب اقربائه وإذا خالف المستأجر ذلك يكون هذا العقد مفسوخا من تلقاء نفسه.' },
        { num: '٨', title: 'محتويات المكان', text: 'يقر المستأجر بأن كل الأشياء الموجودة بالمكان المؤجر هي ملكا خاصا له وهي ضمان للمالك للوفاء بكافة الحقوق التي تثبت له بموجب هذا العقد وطبقا لنص المادة رقم ٥٨٩ من القانون المدني يحق له حبسها وان يمنع نقلها حتى ولو لم يكن قد وقع حجزا عليها وحقه استردادها من الحائز عليها إذا نقلت من المكان ولو كان حسن النية.' },
        { num: '٩', title: 'تهديد سلامة العقار', text: 'يحظر على المستأجر أن يضع في المكان المؤجر له اية مواد ملتهبة او مضرة بالصحة.' },
        { num: '١٠', title: 'الإصلاحات والترميمات', text: 'كافة الإصلاحات والترميمات الجوهرية وكذا الترميمات الايجارية يلتزم بها كل من المالك والمستأجر بالشروط المنصوص عليها بالقانون.' },
        { num: '١١', title: 'توقيع الحجز التحفظي', text: 'يحق للمالك توقيع الحجز التحفظي على منقولات المستأجر في حالة تأخره في سداد الايجار وذلك دون انذار سابق.' },
        { num: '١٢', title: 'التأخير في سداد الايجار وفسخ العقد', text: 'يفسخ هذا العقد فورا وتلقائيا وبدون تنبيه او انذار في حالة تأخير المستأجر في سداد قسط واحد من قيمة الايجار في ميعاد استحقاقه وللمالك الحق في رفع دعوي امام قاضي الأمور المستعجلة لاستصدار الحكم لأخلاء المستأجر بمجرد التثبت من التأخير في السداد اذ ان المستأجر في هذه الحالة يعتبر غاصبا للمكان وشاغلا له دون سند ولا يجوز للمستأجر ان يؤخر سداد أقساط الايجار لأي سبب او يدعي مقاصته مع أي مصاريف كتصليحات او خلافه او ان يطلب تخفيضه أو يقوم بإيداعه خزينة المحكمة الا في حالة توقيع حجزا نهائيا ضد المالك بموجب حكم تحت يد المستأجر.' },
        { num: '١٣', title: 'في حالة الوفاة والافلاس والإعسار', text: 'لا يترتب على وفاة المالك او المستأجر فسخ هذا العقد لكن في حالات وفاة المستأجر وعدم ترك أقرباء له كانوا يقيمون معه في نفس المكان المؤجر له، وممن لهم الحق في ذلك او ان التعاقد قد تم معه بسبب حرفته او مهنته او لاعتبارات خاصة تتعلق بشخصه او إذا أفلس او اعسر فللمالك الحق في فسخ هذا العقد واسترداد المكان.' },
        { num: '١٤', title: 'معاينة التوصيلات', text: 'يقر المستأجر بأنه قد عاين المعاينة النافية للجهالة توصيلات الكهرباء والمياه والغاز وكذلك العدادات الخاصة بها وتأكد من عدم مخالفتها للقوانين، وأي عبث او تلاعب بهذه التوصيلات والعدادات يعتبر هذا العقد مفسوخا تلقائيا ويتحمل المستأجر وحده ما يترتب عن هذا العبث من مخالفات قانونية.' },
        { num: '١٥', title: 'أحكام القوانين', text: 'ما لم يذكر في هذا العقد يخضع لأحكام القانون المدني.' },
        { num: '١٦', title: 'الاختصاص', text: 'محكمة الأمور المستعجلة هي التي تخص المستأجر في حالة مخالفته لشروط هذا العقد او بسبب انتهاء مدته كما يكون الاختصاص المحلي للمحكمة التابع لها المكان المؤجر.' },
        { num: '١٧', title: 'قانونية التخاطب الرسمي على الموطن المختار', text: 'يقر كل من المالك والمستأجر صراحة انه اتخذ موطنا مختارا له في العنوان المذكور بصدد هذا العقد وكل تخاطب رسمي عليه يعتبر قانونيا.' },
        { num: '١٨', title: 'صور العقد', text: 'تحرر هذا العقد من صورتين بيد كل من المتعاقدين صورة للعمل بها عند اللزوم.' },
        { num: '١٩', title: 'بند إضافي', text: contract.extraClause || 'لا يوجد' },
      ];

  // Split clauses across Page 1 and Page 2 nicely
  const page1Clauses = clauses.slice(0, 9);
  const page2Clauses = clauses.slice(9);

  container.innerHTML = `
    <!-- PAGE 1 -->
    <div id="pdf-page-1" style="width: 794px; min-height: 1120px; padding: 36px 42px; box-sizing: border-box; background: #fff; page-break-after: always; display: flex; flex-direction: column;">
      <div style="text-align: center; border-bottom: 2px solid #1b4332; padding-bottom: 10px; margin-bottom: 14px;">
        <h1 style="font-size: 21px; font-weight: 800; color: #1b4332; margin: 0 0 4px 0;">عقد إيجار أملاك</h1>
        <p style="font-size: 13px; font-weight: 600; color: #475569; margin: 0 0 6px 0;">طبقاً لأحكام القانون المدني علي الإيجارات الجديدة</p>
        <div style="display: inline-block; background: #f1f5f9; padding: 4px 14px; border-radius: 4px; font-size: 12px; font-weight: 700;">
          تاريخ تحرير العقد: <span style="color: #1b4332;">${contractDate}</span>
        </div>
      </div>

      <!-- Two-column paired form fields -->
      <table style="width: 100%; border-collapse: collapse; margin-bottom: 12px; font-size: 11.5px; border: 1px solid #cbd5e1;">
        <tbody>
          <tr style="background: #f8fafc;">
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1; width: 50%;"><strong>اسم المالك:</strong> ${landlord.name}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1; width: 50%;"><strong>مهنة المالك:</strong> ${landlord.job}</td>
          </tr>
          <tr>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>عنوان المالك:</strong> ${landlord.address}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>جنسية المالك:</strong> ${landlord.nationality}</td>
          </tr>
          <tr style="background: #f8fafc;">
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>اسم المستأجر:</strong> ${effTenantName}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>مهنة المستأجر:</strong> ${effJob}</td>
          </tr>
          <tr>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>عنوان المستأجر:</strong> ${effAddress}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>جنسية المستأجر:</strong> ${effNationality}</td>
          </tr>
          <tr style="background: #f8fafc;">
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>الرقم القومي:</strong> ${nationalId}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>صادر من:</strong> ${contract.idIssuedFrom || 'الفيوم'}</td>
          </tr>
          <tr>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>بيان ووصف العين:</strong> ${unit.description || 'شقة سكنية'} (وحدة ${unit.unitNumber})</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>الغرض المستعمل فيه:</strong> ${contract.usagePurpose || (property.type === 'shops' ? 'محل تجاري' : 'سكن عائلي')}</td>
          </tr>
          <tr style="background: #f8fafc;">
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>عنوان العقار المؤجر:</strong> ${property.address}</td>
          </tr>
          <tr>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>قيمة الإيجار المتفق عليه:</strong> <span style="font-weight: 700; color: #1b4332;">${rent}</span> جنيهاً مصرياً شهرياً</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>طريقة وميعاد السداد:</strong> ${contract.paymentTerms}</td>
          </tr>
          <tr style="background: #f8fafc;">
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1;">
              <strong>يسري التعاقد لمدة:</strong> <span style="font-weight: 700; color: #1b4332;">${durationText}</span> تبدأ من: <strong>${startDate}</strong> وتنتهي في: <strong>${endDate}</strong>
            </td>
          </tr>
          <tr>
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>شروط تجديد العقد:</strong> ${contract.renewalTerms}</td>
          </tr>
          <tr style="background: #f8fafc;">
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1; font-size: 10.5px;">
              <strong>التنبيه بالإخلاء:</strong> إذا أخطر المستأجر المالك برغبته في إخلاء المكان المؤجر فيتعهد المستأجر بتمكين المالك من دخول كل من يرغب في معاينة المكان بالإضافة إلي السماح للمالك بوضع اللافتات المنوهة بخلو المكان.
            </td>
          </tr>
          <tr>
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1; font-size: 10.5px;">
              <strong>تسليم المكان عند انتهاء العقد:</strong> يقوم المستأجر بتسليم المكان المؤجر إلي المالك حال انتهاء التعاقد علي نفس هيئته وقت الإيجار.
            </td>
          </tr>
          <tr style="background: #f8fafc;">
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>استهلاك المياه:</strong> ${contract.water} | <strong>الكهرباء:</strong> ${contract.electricity}</td>
            <td style="padding: 5px 8px; border: 1px solid #cbd5e1;"><strong>نور السلم:</strong> ${contract.stairsLight} | <strong>أجر البواب:</strong> ${contract.doormanFee}</td>
          </tr>
          <tr>
            <td colspan="2" style="padding: 5px 8px; border: 1px solid #cbd5e1; font-size: 10.5px;">
              <strong>تأمين دفع من المستأجر للمالك وقدره:</strong> <span style="font-weight: 700; color: #1b4332;">${deposit}</span> جنيهاً ولا يجوز للمستأجر استرداده إلا بعد انتهاء التعاقد وتسليم المكان علي أحسن حال ولا تحسب فوائد لهذا التأمين.
            </td>
          </tr>
        </tbody>
      </table>

      <!-- First half of clauses -->
      <div style="font-size: 12px; font-weight: 800; color: #1b4332; margin-bottom: 6px; border-bottom: 1px solid #cbd5e1; padding-bottom: 3px;">
        (الشروط والأحكام)
      </div>
      <div style="display: flex; flex-direction: column; gap: 5px; font-size: 10.5px; line-height: 1.4;">
        ${page1Clauses
          .map(
            (c) => `
          <div>
            <span style="font-weight: 700; color: #1b4332;">${c.num}- ${c.title}:</span> ${c.text}
          </div>`
          )
          .join('')}
      </div>
      <div style="margin-top: auto; text-align: center; font-size: 10px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 6px;">
        صفحة ١ من ٢
      </div>
    </div>

    <!-- PAGE 2 -->
    <div id="pdf-page-2" style="width: 794px; min-height: 1120px; padding: 36px 42px; box-sizing: border-box; background: #fff; display: flex; flex-direction: column;">
      <div style="font-size: 12px; font-weight: 800; color: #1b4332; margin-bottom: 8px; border-bottom: 1px solid #cbd5e1; padding-bottom: 4px;">
        (تابع الشروط والأحكام)
      </div>
      <div style="display: flex; flex-direction: column; gap: 7px; font-size: 10.5px; line-height: 1.45; flex: 1;">
        ${page2Clauses
          .map(
            (c) => `
          <div>
            <span style="font-weight: 700; color: #1b4332;">${c.num}- ${c.title}:</span> ${c.text}
          </div>`
          )
          .join('')}
      </div>

      <!-- Signature section strictly kept together at bottom of page 2 -->
      <div style="margin-top: 24px; border: 1.5px solid #1b4332; border-radius: 6px; padding: 14px 16px; background: #fbfcf8;">
        <div style="text-align: center; font-size: 12px; font-weight: 800; color: #1b4332; margin-bottom: 12px;">
          إقرار وتوقيعات طرفي العقد
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 11px;">
          <tbody>
            <tr>
              <td style="width: 50%; padding: 8px 12px; vertical-align: top;">
                <strong>الطرف الأول (المؤجر):</strong><br/>
                الاسم: ${landlord.name}<br/>
                التوقيع: .......................................<br/>
                بصمة الإبهام:
              </td>
              <td style="width: 50%; padding: 8px 12px; vertical-align: top;">
                <strong>الطرف الثاني (المستأجر):</strong><br/>
                الاسم: ${effTenantName}<br/>
                التوقيع: .......................................<br/>
                بصمة الإبهام:
              </td>
            </tr>
            <tr>
              <td style="padding: 10px 12px 0 12px;">
                <strong>شاهد أول:</strong><br/>
                الاسم: .......................................<br/>
                الرقم القومي: ............................<br/>
                التوقيع: .....................................
              </td>
              <td style="padding: 10px 12px 0 12px;">
                <strong>شاهد ثاني:</strong><br/>
                الاسم: .......................................<br/>
                الرقم القومي: ............................<br/>
                التوقيع: .....................................
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div style="margin-top: 14px; text-align: center; font-size: 10px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 6px;">
        صفحة ٢ من ٢ - عقد إيجار: ${effTenantName} - ${property.name}
      </div>
    </div>
  `;

  document.body.appendChild(container);

  try {
    const page1El = container.querySelector('#pdf-page-1') as HTMLElement;
    const page2El = container.querySelector('#pdf-page-2') as HTMLElement;

    const canvas1 = await html2canvas(page1El, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    });

    const canvas2 = await html2canvas(page2El, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    });

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'pt',
      format: 'a4',
    });

    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();

    const imgData1 = canvas1.toDataURL('image/jpeg', 0.95);
    pdf.addImage(imgData1, 'JPEG', 0, 0, pdfWidth, pdfHeight);

    pdf.addPage('a4', 'portrait');
    const imgData2 = canvas2.toDataURL('image/jpeg', 0.95);
    pdf.addImage(imgData2, 'JPEG', 0, 0, pdfWidth, pdfHeight);

    const fileName = `عقد_${effTenantName.replace(/\s+/g, '_')}_وحدة_${unit.unitNumber}.pdf`;

    // Try saving via Capacitor Filesystem & Share
    try {
      const pdfBase64 = pdf.output('datauristring').split(',')[1];
      const writeResult = await Filesystem.writeFile({
        path: fileName,
        data: pdfBase64,
        directory: Directory.Cache,
      });

      await Share.share({
        title: `عقد إيجار - ${effTenantName}`,
        text: `عقد إيجار وحدة ${unit.unitNumber} - ${property.name}`,
        url: writeResult.uri,
        dialogTitle: 'مشاركة عقد الإيجار',
      });
      return true;
    } catch (capError) {
      console.log('Capacitor share/save fallback:', capError);
    }

    // Web Fallback: direct download
    pdf.save(fileName);
    return true;
  } catch (error) {
    console.error('PDF generation failed:', error);
    return false;
  } finally {
    document.body.removeChild(container);
  }
}

// Receipt PDF Data Interface
export interface ReceiptPDFData {
  payment: Payment;
  tenantName: string;
  tenantPhone?: string;
  propertyName: string;
  unitNumber: string;
  landlord: LandlordDetails;
  breakdown: {
    rentAmount: number;
    electricityAmount: number;
    waterAmount: number;
    stairsCleaningAmount: number;
    previousArrearsAmount: number;
    totalAmount: number;
  };
  currentDate?: string;
  notes?: string;
}

// Generate professional Itemized Payment Receipt PDF (إيصال استلام نقدية وتحصيل إيجار)
export async function generateAndShareReceiptPDF(data: ReceiptPDFData): Promise<boolean> {
  const { payment, tenantName, tenantPhone, propertyName, unitNumber, landlord, breakdown, currentDate } = data;
  const dateStr = currentDate ? formatPDFDate(currentDate, true) : formatPDFDate(new Date().toISOString(), true);
  const paymentMonth = payment.monthYear || 'الشهر الحالي';

  const rentStr = toArabicIndicDigits(breakdown.rentAmount);
  const elecStr = toArabicIndicDigits(breakdown.electricityAmount);
  const waterStr = toArabicIndicDigits(breakdown.waterAmount);
  const stairsStr = toArabicIndicDigits(breakdown.stairsCleaningAmount);
  const arrearsStr = toArabicIndicDigits(breakdown.previousArrearsAmount);
  const totalStr = toArabicIndicDigits(breakdown.totalAmount);

  // Offscreen container for A4 / Half-A4 Receipt
  const container = document.createElement('div');
  container.style.position = 'fixed';
  container.style.top = '-9999px';
  container.style.left = '-9999px';
  container.style.width = '794px'; // standard A4 width
  container.style.backgroundColor = '#ffffff';
  container.style.color = '#111827';
  container.style.fontFamily = "'Cairo', sans-serif";
  container.style.direction = 'rtl';
  container.style.textAlign = 'right';
  container.style.zIndex = '-1000';

  container.innerHTML = `
    <div id="receipt-page" style="width: 794px; min-height: 1120px; padding: 45px 50px; box-sizing: border-box; background: #fff; display: flex; flex-direction: column;">
      
      <!-- Header Banner -->
      <div style="border: 2px solid #1b4332; border-radius: 8px; padding: 18px 24px; margin-bottom: 24px; background: #fbfcf8;">
        <div style="display: flex; justify-content: space-between; align-items: center; border-bottom: 2px solid #1b4332; padding-bottom: 12px; margin-bottom: 14px;">
          <div>
            <h1 style="font-size: 24px; font-weight: 800; color: #1b4332; margin: 0 0 4px 0;">إيصال استلام نقدية وتحصيل</h1>
            <p style="font-size: 13px; color: #475569; margin: 0;">إيجار شهري وخدمات ومرافق</p>
          </div>
          <div style="text-align: left; direction: ltr;">
            <div style="font-size: 13px; font-weight: 700; color: #1b4332;">No: REC-${payment.id.slice(-6).toUpperCase()}</div>
            <div style="font-size: 12px; color: #64748b; direction: rtl;">التاريخ: <strong>${dateStr}</strong></div>
          </div>
        </div>

        <!-- Landlord & Property info bar -->
        <div style="display: flex; justify-content: space-between; font-size: 13px;">
          <div><strong>المؤجر:</strong> ${landlord.name}</div>
          <div><strong>العقار:</strong> ${propertyName}</div>
          <div><strong>الوحدة:</strong> رقم (${unitNumber})</div>
        </div>
      </div>

      <!-- Tenant Info Card -->
      <div style="background: #f8fafc; border: 1px solid #e2e8f0; border-radius: 6px; padding: 14px 18px; margin-bottom: 24px; font-size: 14px; line-height: 1.8;">
        <div>وصلنا من السيد / المستأجر: <strong style="font-size: 16px; color: #1b4332;">${tenantName}</strong> ${tenantPhone ? `(هاتف: ${tenantPhone})` : ''}</div>
        <div>وذلك عن شهر / استحقاق: <strong style="color: #1b4332;">${paymentMonth}</strong></div>
      </div>

      <!-- Itemized Breakdown Table -->
      <div style="margin-bottom: 24px;">
        <div style="font-size: 15px; font-weight: 800; color: #1b4332; margin-bottom: 8px;">
          تفاصيل المبالغ المسددة (التحصيل التفصيلي):
        </div>
        <table style="width: 100%; border-collapse: collapse; font-size: 13.5px; border: 1.5px solid #1b4332;">
          <thead>
            <tr style="background: #1b4332; color: #ffffff;">
              <th style="padding: 10px 14px; text-align: center; width: 60px; border: 1px solid #1b4332;">م</th>
              <th style="padding: 10px 14px; text-align: right; border: 1px solid #1b4332;">البند والبيان</th>
              <th style="padding: 10px 14px; text-align: left; width: 180px; border: 1px solid #1b4332;">المبلغ المسدد (جنيه مصري)</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td style="padding: 9px 12px; text-align: center; border: 1px solid #cbd5e1;">١</td>
              <td style="padding: 9px 12px; border: 1px solid #cbd5e1;"><strong>قيمة الإيجار الشهري</strong></td>
              <td style="padding: 9px 12px; text-align: left; font-weight: 700; border: 1px solid #cbd5e1;">${rentStr} ج.م</td>
            </tr>
            <tr style="background: #f8fafc;">
              <td style="padding: 9px 12px; text-align: center; border: 1px solid #cbd5e1;">٢</td>
              <td style="padding: 9px 12px; border: 1px solid #cbd5e1;"><strong>استهلاك الكهرباء</strong></td>
              <td style="padding: 9px 12px; text-align: left; font-weight: 700; border: 1px solid #cbd5e1;">${elecStr} ج.م</td>
            </tr>
            <tr>
              <td style="padding: 9px 12px; text-align: center; border: 1px solid #cbd5e1;">٣</td>
              <td style="padding: 9px 12px; border: 1px solid #cbd5e1;"><strong>استهلاك المياه</strong></td>
              <td style="padding: 9px 12px; text-align: left; font-weight: 700; border: 1px solid #cbd5e1;">${waterStr} ج.م</td>
            </tr>
            <tr style="background: #f8fafc;">
              <td style="padding: 9px 12px; text-align: center; border: 1px solid #cbd5e1;">٤</td>
              <td style="padding: 9px 12px; border: 1px solid #cbd5e1;"><strong>نظافة وصيانة السلم والخدمات</strong></td>
              <td style="padding: 9px 12px; text-align: left; font-weight: 700; border: 1px solid #cbd5e1;">${stairsStr} ج.م</td>
            </tr>
            <tr>
              <td style="padding: 9px 12px; text-align: center; border: 1px solid #cbd5e1;">٥</td>
              <td style="padding: 9px 12px; border: 1px solid #cbd5e1;"><strong>مبالغ متأخرة سابقة (سداد المتأخرات)</strong></td>
              <td style="padding: 9px 12px; text-align: left; font-weight: 700; border: 1px solid #cbd5e1;">${arrearsStr} ج.م</td>
            </tr>
            <tr style="background: #eaf5ee; border-top: 2px solid #1b4332;">
              <td colspan="2" style="padding: 12px 14px; font-weight: 800; font-size: 15px; color: #1b4332; border: 1px solid #1b4332;">
                الإجمالي الكلي المستلم والمقبوض:
              </td>
              <td style="padding: 12px 14px; text-align: left; font-weight: 800; font-size: 16px; color: #1b4332; border: 1px solid #1b4332;">
                ${totalStr} جنيهاً مصرياً
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      ${data.notes ? `
      <div style="margin-bottom: 20px; font-size: 12.5px; color: #475569; background: #fffbeb; border: 1px solid #fef3c7; border-radius: 6px; padding: 10px 14px;">
        <strong>ملاحظات:</strong> ${data.notes}
      </div>` : ''}

      <!-- Signatures Box -->
      <div style="margin-top: auto; border: 1.5px solid #1b4332; border-radius: 6px; padding: 18px 24px; background: #fbfcf8;">
        <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
          <tbody>
            <tr>
              <td style="width: 50%; vertical-align: top;">
                <strong>المستلم (المؤجر / الوكيل):</strong><br/><br/>
                الاسم: ${landlord.name}<br/>
                التوقيع: ...........................................
              </td>
              <td style="width: 50%; vertical-align: top;">
                <strong>المستأجر (الدافع):</strong><br/><br/>
                الاسم: ${tenantName}<br/>
                التوقيع: ...........................................
              </td>
            </tr>
          </tbody>
        </table>
      </div>

      <div style="margin-top: 20px; text-align: center; font-size: 11px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 8px;">
        تم إصدار هذا الإيصال إلكترونياً من تطبيق إدارة العقارات والأملاك
      </div>
    </div>
  `;

  document.body.appendChild(container);

  try {
    const pageEl = container.querySelector('#receipt-page') as HTMLElement;
    const canvas = await html2canvas(pageEl, {
      scale: 2,
      useCORS: true,
      backgroundColor: '#ffffff',
    });

    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'pt',
      format: 'a4',
    });

    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();
    const imgData = canvas.toDataURL('image/jpeg', 0.95);
    pdf.addImage(imgData, 'JPEG', 0, 0, pdfWidth, pdfHeight);

    const fileName = `ايصال_سداد_${tenantName.replace(/\s+/g, '_')}_وحدة_${unitNumber}.pdf`;

    try {
      const pdfBase64 = pdf.output('datauristring').split(',')[1];
      const writeResult = await Filesystem.writeFile({
        path: fileName,
        data: pdfBase64,
        directory: Directory.Cache,
      });

      await Share.share({
        title: `إيصال سداد - ${tenantName}`,
        text: `إيصال سداد إيجار وحدة ${unitNumber} - ${propertyName}`,
        url: writeResult.uri,
        dialogTitle: 'مشاركة إيصال السداد',
      });
      return true;
    } catch (capError) {
      console.log('Capacitor share/save fallback:', capError);
    }

    // Web download fallback
    pdf.save(fileName);
    return true;
  } catch (error) {
    console.error('Receipt PDF generation failed:', error);
    return false;
  } finally {
    document.body.removeChild(container);
  }
}
