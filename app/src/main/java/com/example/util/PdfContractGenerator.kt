package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.example.R
import com.example.data.model.Contract
import com.example.data.model.LandlordDetails
import com.example.data.model.Payment
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UnitItem
import java.io.File
import java.io.FileOutputStream

object PdfContractGenerator {

  // Page dimensions for A4 (72 DPI): 595 x 842 points
  private const val PAGE_WIDTH = 595
  private const val PAGE_HEIGHT = 842
  private const val MARGIN = 36

  fun generateAndShareContractPdf(
    context: Context,
    contract: Contract,
    unit: UnitItem,
    property: Property,
    landlord: LandlordDetails,
    templateText: String,
    tenant: com.example.data.model.Tenant? = null
  ): Boolean {
    return try {
      val document = PdfDocument()

      val cairoTypeface = try {
        ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
      } catch (e: Exception) {
        Typeface.DEFAULT
      }

      val titlePaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 15f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val subtitlePaint = Paint().apply {
        color = Color.parseColor("#475569")
        textSize = 10f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val sectionTitlePaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 10.5f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val textPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 8.5f
        typeface = cairoTypeface
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val boldTextPaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 8.5f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val borderPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
      }

      val bgPaint = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        style = Paint.Style.FILL
      }

      val effectiveTenantName = tenant?.fullName?.ifBlank { null } ?: contract.tenantName
      val effectiveNationalId = tenant?.nationalId?.ifBlank { null } ?: contract.nationalId
      val effectiveJob = tenant?.job?.ifBlank { null } ?: contract.tenantJob
      val effectiveAddress = tenant?.address?.ifBlank { null } ?: contract.tenantAddress
      val effectiveNationality = tenant?.nationality?.ifBlank { null } ?: contract.tenantNationality

      val durationText = Formatters.calculateDurationText(contract.startDate, contract.endDate, "ar")
      val contractDateStr = Formatters.formatPDFDate(if (contract.contractDate.isNotBlank()) contract.contractDate else contract.startDate)
      val startDateStr = Formatters.formatPDFDate(contract.startDate)
      val endDateStr = Formatters.formatPDFDate(contract.endDate)
      val rentStr = Formatters.toArabicIndicDigits(contract.monthlyRent.toInt())
      val depositStr = Formatters.toArabicIndicDigits(if (contract.depositAmount > 0) contract.depositAmount.toInt() else contract.monthlyRent.toInt())
      val nationalIdStr = if (effectiveNationalId.isNotBlank()) Formatters.toArabicIndicDigits(effectiveNationalId) else "...................."

      // --- PAGE 1 ---
      val pageInfo1 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page1 = document.startPage(pageInfo1)
      val canvas1 = page1.canvas

      var y = MARGIN + 14f
      val centerX = PAGE_WIDTH / 2f
      val rightX = (PAGE_WIDTH - MARGIN).toFloat()
      val leftX = MARGIN.toFloat()
      val tableWidth = rightX - leftX

      // Header
      canvas1.drawText("عقد إيجار أملاك", centerX, y, titlePaint)
      y += 14f
      canvas1.drawText("طبقاً لأحكام القانون المدني علي الإيجارات الجديدة", centerX, y, subtitlePaint)
      y += 14f
      canvas1.drawText("تاريخ تحرير العقد: $contractDateStr", centerX, y, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
      boldTextPaint.textAlign = Paint.Align.RIGHT
      y += 16f

      // Paired Fields Table
      val rowHeight = 17f
      val colWidth = tableWidth / 2f

      val tableRows = listOf(
        Pair("اسم المالك: ${landlord.name}", "مهنة المالك: ${landlord.job}"),
        Pair("عنوان المالك: ${landlord.address}", "جنسية المالك: ${landlord.nationality}"),
        Pair("اسم المستأجر: $effectiveTenantName", "مهنة المستأجر: $effectiveJob"),
        Pair("عنوان المستأجر: $effectiveAddress", "جنسية المستأجر: $effectiveNationality"),
        Pair("الرقم القومي: $nationalIdStr", "صادر من: ${contract.idIssuedFrom}"),
        Pair("بيان العين: ${unit.description} (وحدة ${unit.unitNumber})", "الغرض: ${contract.usagePurpose}"),
        Pair("عنوان العقار: ${property.address}", ""),
        Pair("قيمة الإيجار: $rentStr جنيهاً شهرياً", "طريقة السداد: ${contract.paymentTerms}"),
        Pair("يسري التعاقد لمدة: $durationText تبدأ من: $startDateStr وتنتهي في: $endDateStr", ""),
        Pair("شروط التجديد: ${contract.renewalTerms}", ""),
        Pair("استهلاك المياه: ${contract.water} | كهرباء: ${contract.electricity}", "نور السلم: ${contract.stairsLight} | البواب: ${contract.doormanFee}"),
        Pair("تأمين مدفوع: $depositStr جنيهاً (لا يحسب عليه فوائد ويرد بنهاية التعاقد)", "")
      )

      val tableTop = y
      for ((idx, row) in tableRows.withIndex()) {
        val rowTop = y
        val rowBottom = y + rowHeight

        if (idx % 2 == 0) {
          canvas1.drawRect(leftX, rowTop, rightX, rowBottom, bgPaint)
        }
        canvas1.drawRect(leftX, rowTop, rightX, rowBottom, borderPaint)

        if (row.second.isBlank()) {
          // Span full row
          canvas1.drawText(row.first, rightX - 6f, rowTop + 12f, textPaint)
        } else {
          canvas1.drawLine(leftX + colWidth, rowTop, leftX + colWidth, rowBottom, borderPaint)
          canvas1.drawText(row.first, rightX - 6f, rowTop + 12f, textPaint)
          canvas1.drawText(row.second, leftX + colWidth - 6f, rowTop + 12f, textPaint)
        }
        y += rowHeight
      }

      y += 14f
      canvas1.drawText("(الشروط والأحكام)", rightX, y, sectionTitlePaint)
      y += 12f

      val isShop = property.type == PropertyType.SHOPS
      val clauses = if (isShop) {
        listOf(
          "١- موضوع العقد: بموجب هذا العقد وبعد قبول المستأجر قد أجر المالك للمستأجر العين الموضح وصفها بصدد هذا العقد.",
          "٢- صلاحية المكان: لا يجوز للمستأجر استعمال المكان موضوع هذا العقد بالصورة التي تنافي شروطه او الغرض منه.",
          "٣- صلاحية المكان المستأجر: يقر المستأجر بأن المكان موضوع هذا العقد يصلح للأغراض التي استؤجر من اجلها.",
          "٤- استلام المكان وأعادته: المستأجر يقر انه استلم المكان مستوفيا لكل لوازمه وهو يتعهد بالمحافظة عليه كملكه الخاص.",
          "٥- أحدث أي تغيرات: محظور على المستأجر ان يحدث أي تغيير في المكان المؤجر له الا بتصريح كتابي من المالك.",
          "٦- تأجير المكان من الباطن: محظور على المستأجر ان يتنازل عن ايجارته او يؤجر من الباطن كل او جزء من المكان المؤجر.",
          "٧- محتويات المكان: يقر المستأجر بأن كل الأشياء الموجودة بالمكان هي ملك له وضمان للمالك للوفاء بالحقوق (مادة ٥٨٩ مدني).",
          "٨- سلامة العقار: يحظر على المستأجر أن يضع في المكان المؤجر له اية مواد ملتهبة او مفرقعات او مضرة بالصحة العامة.",
          "٩- الإصلاحات والترميمات: كافة الإصلاحات الجوهرية والايجارية يلتزم بها الطرفان وفقاً لأحكام القانون المدني.",
          "١٠- توقيع الحجز التحفظي: يحق للمالك توقيع الحجز التحفظي على منقولات المستأجر في حالة تأخره في سداد الايجار دون انذار.",
          "١١- التأخير والفسخ: يفسخ هذا العقد فورا وتلقائيا في حالة تأخير المستأجر عن سداد قسط واحد في ميعاد استحقاقه.",
          "١٢- الوفاة والإعسار: لا يترتب على الوفاة فسخ العقد الا في حالات عدم وجود ورثة مقيمين بالعين أو في حالة الإفلاس والإعسار.",
          "١٣- معاينة التوصيلات: يقر المستأجر بمعاينة العدادات والتوصيلات والتأكد من مطابقتها للقوانين وعدم التلاعب بها.",
          "١٤- أحكام القوانين: ما لم يرد بشأنه نص في هذا العقد يرجع فيه لأحكام القانون المدني.",
          "١٥- الاختصاص القضائي: محكمة الأمور المستعجلة والمحكمة التابع لها المكان هي المختصة بنظر أي نزاع.",
          "١٦- الموطن المختار: يقر الطرفان باتخاذ العناوين المذكورة موطناً مختاراً للإعلانات الرسمية.",
          "١٧- نسخ العقد: تحرر هذا العقد من صورتين بيد كل طرف صورة للعمل بموجبها.",
          "١٨- بند إضافي: ${if (contract.extraClause.isNotBlank()) contract.extraClause else "لا يوجد"}"
        )
      } else {
        listOf(
          "١- موضوع العقد: بموجب هذا العقد وبعد قبول المستأجر قد أجر المالك للمستأجر العين الموضح وصفها بصدد هذا العقد.",
          "٢- صلاحية المكان: لا يجوز للمستأجر استعمال المكان موضوع هذا العقد بالصورة التي تنافي شروطه او الغرض منه.",
          "٣- صلاحية المكان المستأجر: يقر المستأجر بأن المكان موضوع هذا العقد يصلح للأغراض التي استؤجر من اجلها.",
          "٤- استلام المكان وأعادته: المستأجر يقر انه استلم المكان مستوفيا لكل لوازمه وهو يتعهد بالمحافظة عليه كملكه الخاص.",
          "٥- أحدث أي تغيرات: محظور على المستأجر ان يحدث أي تغيير في المكان المؤجر له الا بتصريح كتابي من المالك.",
          "٦- إقرار السكن العائلي: يقر المستأجر بأن الشقة ليست مسكن زوجية ويلتزم بعدم تمكين الغير من الإقامة بها كمسكن زوجية.",
          "٧- تأجير المكان من الباطن: محظور على المستأجر ان يتنازل عن ايجارته او يؤجر من الباطن كل او جزء من المكان المؤجر.",
          "٨- محتويات المكان: يقر المستأجر بأن كل الأشياء الموجودة بالمكان هي ملك له وضمان للمالك للوفاء بالحقوق (مادة ٥٨٩ مدني).",
          "٩- سلامة العقار: يحظر على المستأجر أن يضع في المكان المؤجر له اية مواد ملتهبة او مفرقعات او مضرة بالصحة العامة.",
          "١٠- الإصلاحات والترميمات: كافة الإصلاحات الجوهرية والايجارية يلتزم بها الطرفان وفقاً لأحكام القانون المدني.",
          "١١- توقيع الحجز التحفظي: يحق للمالك توقيع الحجز التحفظي على منقولات المستأجر في حالة تأخره في سداد الايجار دون انذار.",
          "١٢- التأخير والفسخ: يفسخ هذا العقد فورا وتلقائيا في حالة تأخير المستأجر عن سداد قسط واحد في ميعاد استحقاقه.",
          "١٣- الوفاة والإعسار: لا يترتب على الوفاة فسخ العقد الا في حالات عدم وجود ورثة مقيمين بالعين أو في حالة الإفلاس والإعسار.",
          "١٤- معاينة التوصيلات: يقر المستأجر بمعاينة العدادات والتوصيلات والتأكد من مطابقتها للقوانين وعدم التلاعب بها.",
          "١٥- أحكام القوانين: ما لم يرد بشأنه نص في هذا العقد يرجع فيه لأحكام القانون المدني.",
          "١٦- الاختصاص القضائي: محكمة الأمور المستعجلة والمحكمة التابع لها المكان هي المختصة بنظر أي نزاع.",
          "١٧- الموطن المختار: يقر الطرفان باتخاذ العناوين المذكورة موطناً مختاراً للإعلانات الرسمية.",
          "١٨- نسخ العقد: تحرر هذا العقد من صورتين بيد كل طرف صورة للعمل بموجبها.",
          "١٩- بند إضافي: ${if (contract.extraClause.isNotBlank()) contract.extraClause else "لا يوجد"}"
        )
      }

      val page1Clauses = clauses.take(9)
      val page2Clauses = clauses.drop(9)

      for (clause in page1Clauses) {
        canvas1.drawText(clause, rightX, y, textPaint)
        y += 15f
      }

      // Page 1 footer
      canvas1.drawText("صفحة ١ من ٢", centerX, (PAGE_HEIGHT - 20).toFloat(), subtitlePaint)
      document.finishPage(page1)

      // --- PAGE 2 ---
      val pageInfo2 = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 2).create()
      val page2 = document.startPage(pageInfo2)
      val canvas2 = page2.canvas

      var y2 = MARGIN + 20f
      canvas2.drawText("(تابع الشروط والأحكام)", rightX, y2, sectionTitlePaint)
      y2 += 16f

      for (clause in page2Clauses) {
        canvas2.drawText(clause, rightX, y2, textPaint)
        y2 += 16f
      }

      // Signature area kept strictly together at bottom of page 2
      val signBoxTop = (PAGE_HEIGHT - 170).toFloat()
      val signBoxBottom = (PAGE_HEIGHT - 45).toFloat()

      canvas2.drawRect(leftX, signBoxTop, rightX, signBoxBottom, bgPaint)
      canvas2.drawRect(leftX, signBoxTop, rightX, signBoxBottom, borderPaint)

      canvas2.drawText("إقرار وتوقيعات طرفي العقد والشهود", centerX, signBoxTop + 18f, boldTextPaint.apply { textAlign = Paint.Align.CENTER })
      boldTextPaint.textAlign = Paint.Align.RIGHT

      val signColW = tableWidth / 2f
      val line1Y = signBoxTop + 38f
      canvas2.drawText("الطرف الأول (المؤجر): ${landlord.name}", rightX - 10f, line1Y, textPaint)
      canvas2.drawText("الطرف الثاني (المستأجر): $effectiveTenantName", leftX + signColW - 10f, line1Y, textPaint)

      val line2Y = line1Y + 22f
      canvas2.drawText("التوقيع: ...........................................", rightX - 10f, line2Y, textPaint)
      canvas2.drawText("التوقيع: ...........................................", leftX + signColW - 10f, line2Y, textPaint)

      val line3Y = line2Y + 28f
      canvas2.drawText("شاهد أول: .........................................", rightX - 10f, line3Y, textPaint)
      canvas2.drawText("شاهد ثاني: .........................................", leftX + signColW - 10f, line3Y, textPaint)

      val line4Y = line3Y + 18f
      canvas2.drawText("الرقم القومي: .....................................", rightX - 10f, line4Y, textPaint)
      canvas2.drawText("الرقم القومي: .....................................", leftX + signColW - 10f, line4Y, textPaint)

      // Page 2 footer
      canvas2.drawText("صفحة ٢ من ٢ - عقد إيجار: $effectiveTenantName - ${property.name}", centerX, (PAGE_HEIGHT - 20).toFloat(), subtitlePaint)
      document.finishPage(page2)

      // Save file in cache dir
      val cacheDir = context.cacheDir
      val fileName = "عقد_${effectiveTenantName.replace(" ", "_")}_وحدة_${unit.unitNumber}.pdf"
      val file = File(cacheDir, fileName)
      val fos = FileOutputStream(file)
      document.writeTo(fos)
      fos.close()
      document.close()

      // Share intent
      val uri = FileProvider.getUriForFile(context, "com.family.rentmanager.fileprovider", file)
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "عقد إيجار - $effectiveTenantName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "مشاركة عقد الإيجار"))
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  // Monthly Report PDF Generator
  fun generateAndShareReportPdf(
    context: Context,
    month: String,
    landlordName: String,
    reportData: List<ReportItem>,
    grandExpected: Double,
    grandCollected: Double,
    grandOverdue: Double
  ): Boolean {
    return try {
      val document = PdfDocument()
      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      val cairoTypeface = try {
        ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
      } catch (e: Exception) {
        Typeface.DEFAULT
      }

      val titlePaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 16f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val subPaint = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 10f
        typeface = cairoTypeface
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val textPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 9f
        typeface = cairoTypeface
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val boldTextPaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 9.5f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val borderPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 0.8f
      }

      val bgPaint = Paint().apply {
        color = Color.parseColor("#F1F5F9")
        style = Paint.Style.FILL
      }

      val rightX = (PAGE_WIDTH - MARGIN).toFloat()
      val leftX = MARGIN.toFloat()
      val tableW = rightX - leftX

      var y = MARGIN + 20f
      canvas.drawText("تقرير تحصيل الإيجارات الشهري", PAGE_WIDTH / 2f, y, titlePaint)
      y += 16f
      canvas.drawText("عن شهر: $month • المالك: $landlordName", PAGE_WIDTH / 2f, y, subPaint)
      y += 24f

      // Summary Bar
      canvas.drawRect(leftX, y, rightX, y + 36f, bgPaint)
      canvas.drawRect(leftX, y, rightX, y + 36f, borderPaint)
      val expStr = "المطلوب: ${Formatters.formatCurrency(grandExpected)}"
      val colStr = "المحصل: ${Formatters.formatCurrency(grandCollected)}"
      val overStr = "المتأخرات: ${Formatters.formatCurrency(grandOverdue)}"
      canvas.drawText(expStr, rightX - 10f, y + 22f, boldTextPaint)
      canvas.drawText(colStr, leftX + tableW * 0.6f, y + 22f, boldTextPaint.apply { color = Color.parseColor("#15803D") })
      canvas.drawText(overStr, leftX + 10f + 100f, y + 22f, boldTextPaint.apply { color = Color.parseColor("#DC2626") })
      boldTextPaint.color = Color.parseColor("#1B4332")
      y += 50f

      // Table Header
      val rowH = 22f
      canvas.drawRect(leftX, y, rightX, y + rowH, bgPaint)
      canvas.drawRect(leftX, y, rightX, y + rowH, borderPaint)
      canvas.drawText("العقار", rightX - 6f, y + 15f, boldTextPaint)
      canvas.drawText("الوحدات", rightX - 140f, y + 15f, boldTextPaint)
      canvas.drawText("المطلوب", rightX - 220f, y + 15f, boldTextPaint)
      canvas.drawText("المحصل", rightX - 320f, y + 15f, boldTextPaint)
      canvas.drawText("المتأخرات", rightX - 420f, y + 15f, boldTextPaint)
      canvas.drawText("النسبة", leftX + 25f, y + 15f, boldTextPaint)
      y += rowH

      for ((idx, item) in reportData.withIndex()) {
        val rowTop = y
        val rowBottom = y + rowH
        if (idx % 2 == 1) {
          canvas.drawRect(leftX, rowTop, rightX, rowBottom, bgPaint)
        }
        canvas.drawRect(leftX, rowTop, rightX, rowBottom, borderPaint)

        canvas.drawText(item.propertyName, rightX - 6f, rowTop + 14f, textPaint)
        canvas.drawText("${item.activeLeases}/${item.totalUnits}", rightX - 140f, rowTop + 14f, textPaint)
        canvas.drawText(Formatters.formatCurrency(item.expected), rightX - 220f, rowTop + 14f, textPaint)
        canvas.drawText(Formatters.formatCurrency(item.collected), rightX - 320f, rowTop + 14f, textPaint)
        canvas.drawText(Formatters.formatCurrency(item.overdue), rightX - 420f, rowTop + 14f, textPaint)
        canvas.drawText("${item.rate}%", leftX + 25f, rowTop + 14f, boldTextPaint)

        y += rowH
      }

      document.finishPage(page)

      val cacheDir = context.cacheDir
      val fileName = "تقرير_تحصيل_${month}.pdf"
      val file = File(cacheDir, fileName)
      val fos = FileOutputStream(file)
      document.writeTo(fos)
      fos.close()
      document.close()

      val uri = FileProvider.getUriForFile(context, "com.family.rentmanager.fileprovider", file)
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "تقرير تحصيل الإيجارات - $month")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "مشاركة تقرير التحصيل"))
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  fun generateAndShareReceiptPdf(
    context: Context,
    payment: Payment,
    contract: Contract?,
    unit: UnitItem?,
    property: Property?,
    landlord: LandlordDetails,
    rentAmount: Double,
    electricityAmount: Double,
    waterAmount: Double,
    stairsCleaningAmount: Double,
    previousArrearsAmount: Double,
    totalAmount: Double,
    paidDate: String,
    notes: String?,
    language: String = "ar"
  ): Boolean {
    return try {
      val document = PdfDocument()

      val cairoTypeface = try {
        ResourcesCompat.getFont(context, R.font.cairo) ?: Typeface.DEFAULT
      } catch (e: Exception) {
        Typeface.DEFAULT
      }

      val titlePaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 15f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val subtitlePaint = Paint().apply {
        color = Color.parseColor("#475569")
        textSize = 10f
        typeface = Typeface.create(cairoTypeface, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }

      val headerPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 9.5f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val headerLeftPaint = Paint().apply {
        color = Color.parseColor("#475569")
        textSize = 9f
        typeface = Typeface.create(cairoTypeface, Typeface.NORMAL)
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
      }

      val textPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 9.5f
        typeface = cairoTypeface
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val textLeftPaint = Paint().apply {
        color = Color.parseColor("#1E293B")
        textSize = 9.5f
        typeface = cairoTypeface
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
      }

      val boldTextPaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        textSize = 10f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }

      val borderPaint = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.STROKE
        strokeWidth = 1f
      }

      val thickBorderPaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        style = Paint.Style.STROKE
        strokeWidth = 2f
      }

      val bannerPaint = Paint().apply {
        color = Color.parseColor("#1B4332")
        style = Paint.Style.FILL
      }

      val bgMintPaint = Paint().apply {
        color = Color.parseColor("#F0FDF4")
        style = Paint.Style.FILL
      }

      val bgCardPaint = Paint().apply {
        color = Color.parseColor("#F8FAFC")
        style = Paint.Style.FILL
      }

      val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
      val page = document.startPage(pageInfo)
      val canvas = page.canvas

      val leftX = MARGIN.toFloat()
      val rightX = (PAGE_WIDTH - MARGIN).toFloat()
      val centerX = (PAGE_WIDTH / 2).toFloat()
      var y = MARGIN.toFloat() + 5f

      // Outer decorative border
      canvas.drawRect(leftX - 10f, y - 5f, rightX + 10f, (PAGE_HEIGHT - MARGIN + 10).toFloat(), thickBorderPaint)
      canvas.drawRect(leftX - 7f, y - 2f, rightX + 7f, (PAGE_HEIGHT - MARGIN + 7).toFloat(), borderPaint)

      // Header Landlord Details (Right) & Date/Number (Left)
      canvas.drawText("المالك: ${landlord.name}", rightX, y + 12f, headerPaint)
      canvas.drawText("${landlord.job} • ${landlord.address}", rightX, y + 26f, subtitlePaint.apply { textAlign = Paint.Align.RIGHT; textSize = 8.5f })
      subtitlePaint.textAlign = Paint.Align.CENTER // Reset

      val receiptSerial = "#REC-${payment.id.takeLast(6).uppercase()}"
      canvas.drawText("رقم الإيصال: $receiptSerial", leftX, y + 12f, headerLeftPaint)
      canvas.drawText("تاريخ التحرير: ${Formatters.formatUIDate(paidDate)}", leftX, y + 26f, headerLeftPaint)
      y += 42f

      // Title Banner
      canvas.drawRect(leftX, y, rightX, y + 36f, bannerPaint)
      val whiteTitlePaint = Paint().apply {
        color = Color.WHITE
        textSize = 14f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }
      canvas.drawText("إيصال استلام نقدية (إيجار وخدمات)", centerX, y + 22f, whiteTitlePaint)
      y += 44f

      // Tenant & Property Box
      val boxTop = y
      val boxH = 75f
      canvas.drawRect(leftX, boxTop, rightX, boxTop + boxH, bgCardPaint)
      canvas.drawRect(leftX, boxTop, rightX, boxTop + boxH, borderPaint)

      val tenantName = contract?.tenantName ?: "المستأجر"
      val propName = property?.name ?: ""
      val unitNum = unit?.unitNumber ?: ""
      val unitDesc = unit?.description?.ifBlank { null } ?: ""

      canvas.drawText("استلمت أنا: ${landlord.name} (${landlord.job})", rightX - 10f, y + 18f, boldTextPaint)
      canvas.drawText("من السيد / المستأجر: $tenantName", rightX - 10f, y + 35f, boldTextPaint)
      canvas.drawText("عن الوحدة: رقم $unitNum - عقار: $propName ${if (unitDesc.isNotBlank()) "($unitDesc)" else ""}", rightX - 10f, y + 52f, textPaint)
      canvas.drawText("استحقاق شهر / تاريخ: ${Formatters.formatUIDate(payment.dueDate)}", rightX - 10f, y + 68f, textPaint)
      y += boxH + 16f

      // Breakdown Items Table Header
      val tableRowH = 25f
      canvas.drawRect(leftX, y, rightX, y + tableRowH, bannerPaint)
      val colItemX = rightX - 12f
      val colDescX = rightX - 160f
      val colAmtX = leftX + 130f
      val colNotesX = leftX + 15f

      val whiteTableHdrPaint = Paint().apply {
        color = Color.WHITE
        textSize = 10f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }
      canvas.drawText("البند والبيان", colItemX, y + 16f, whiteTableHdrPaint)
      canvas.drawText("البيان التفصيلي", colDescX, y + 16f, whiteTableHdrPaint)
      canvas.drawText("المبلغ (ج.م)", colAmtX, y + 16f, whiteTableHdrPaint)
      canvas.drawText("حالة السداد", leftX + 50f, y + 16f, whiteTableHdrPaint)
      y += tableRowH

      data class BreakdownRow(val title: String, val desc: String, val amount: Double)
      val rows = listOf(
        BreakdownRow("١. الإيجار الشهري", "القيمة الإيجارية الشهرية المحددة بالعقد", rentAmount),
        BreakdownRow("٢. استهلاك الكهرباء", "قيمة فاتورة الكهرباء المستحقة", electricityAmount),
        BreakdownRow("٣. استهلاك المياه", "قيمة استهلاك المياه المستحقة", waterAmount),
        BreakdownRow("٤. نظافة وصيانة السلم", "رسوم الصيانة ونظافة المدخل والخدمات", stairsCleaningAmount),
        BreakdownRow("٥. متأخرات سابقة مسددة", "مبالغ سابقة مستحقة على الوحدة", previousArrearsAmount)
      )

      for ((idx, r) in rows.withIndex()) {
        val rowTop = y
        val rowBottom = y + tableRowH
        if (idx % 2 == 1) {
          canvas.drawRect(leftX, rowTop, rightX, rowBottom, bgCardPaint)
        }
        canvas.drawRect(leftX, rowTop, rightX, rowBottom, borderPaint)

        canvas.drawText(r.title, colItemX, rowTop + 16f, boldTextPaint)
        canvas.drawText(r.desc, colDescX, rowTop + 16f, textPaint.apply { textSize = 8.5f })
        textPaint.textSize = 9.5f // Reset

        val amtStr = if (r.amount > 0) Formatters.formatCurrency(r.amount, "ar") else "٠ ج.م"
        canvas.drawText(amtStr, colAmtX, rowTop + 16f, if (r.amount > 0) boldTextPaint else textPaint)

        val statusLabel = if (r.amount > 0) "تم السداد ✓" else "—"
        canvas.drawText(statusLabel, leftX + 50f, rowTop + 16f, textPaint)

        y += tableRowH
      }

      // Total Row (Mint Green background)
      val totalRowH = 32f
      canvas.drawRect(leftX, y, rightX, y + totalRowH, bgMintPaint)
      canvas.drawRect(leftX, y, rightX, y + totalRowH, thickBorderPaint)

      val grandTotalPaint = Paint().apply {
        color = Color.parseColor("#14532D")
        textSize = 12f
        typeface = Typeface.create(cairoTypeface, Typeface.BOLD)
        textAlign = Paint.Align.RIGHT
        isAntiAlias = true
      }
      canvas.drawText("إجمالي المبلغ المستلم والمقيد:", colItemX, y + 20f, grandTotalPaint)
      canvas.drawText(Formatters.formatCurrency(totalAmount, "ar"), colAmtX, y + 20f, grandTotalPaint)
      y += totalRowH + 14f

      // Tafqeet Box (Written amount in Arabic words)
      val tafqeetH = 34f
      canvas.drawRect(leftX, y, rightX, y + tafqeetH, bgCardPaint)
      canvas.drawRect(leftX, y, rightX, y + tafqeetH, borderPaint)

      val writtenWords = Formatters.formatCurrencyInWords(totalAmount, "ar")
      canvas.drawText("المبلغ كتابةً:", rightX - 10f, y + 21f, boldTextPaint)
      canvas.drawText(writtenWords, rightX - 80f, y + 21f, textPaint.apply { color = Color.parseColor("#15803D") })
      textPaint.color = Color.parseColor("#1E293B") // Reset
      y += tafqeetH + 12f

      // Receipt Notes (if provided)
      val noteText = notes?.ifBlank { null } ?: "تم استلام المبلغ نقداً بموجب هذا الإيصال"
      val noteBoxH = 30f
      canvas.drawRect(leftX, y, rightX, y + noteBoxH, bgCardPaint)
      canvas.drawRect(leftX, y, rightX, y + noteBoxH, borderPaint)
      canvas.drawText("ملاحظات:", rightX - 10f, y + 19f, boldTextPaint)
      canvas.drawText(noteText, rightX - 60f, y + 19f, textPaint)
      y += noteBoxH + 30f

      // Signatures Block
      val sigBoxW = (rightX - leftX) / 2 - 20f

      // Landlord Signature (Right)
      canvas.drawText("المستلم (المالك / الوكيل)", rightX - 10f, y, boldTextPaint)
      canvas.drawText("الاسم: ${landlord.name}", rightX - 10f, y + 18f, textPaint)
      canvas.drawText("التوقيع: ........................................", rightX - 10f, y + 42f, textPaint)

      // Tenant Signature (Left)
      val leftSigX = leftX + sigBoxW
      canvas.drawText("المسدد (المستأجر)", leftSigX, y, boldTextPaint)
      canvas.drawText("الاسم: $tenantName", leftSigX, y + 18f, textPaint)
      canvas.drawText("التوقيع: ........................................", leftSigX, y + 42f, textPaint)

      // Bottom Notice
      y = (PAGE_HEIGHT - MARGIN - 25).toFloat()
      val footerPaint = Paint().apply {
        color = Color.parseColor("#64748B")
        textSize = 8.5f
        typeface = cairoTypeface
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
      }
      canvas.drawText("• صدر هذا الإيصال رسمياً عبر تطبيق إدارة الإيجارات — يُرجى الاحتفاظ به لإثبات السداد •", centerX, y, footerPaint)

      document.finishPage(page)

      val cacheDir = context.cacheDir
      val safeTenantName = tenantName.replace(" ", "_")
      val fileName = "إيصال_إيجار_${safeTenantName}_${paidDate}.pdf"
      val file = File(cacheDir, fileName)
      val fos = FileOutputStream(file)
      document.writeTo(fos)
      fos.close()
      document.close()

      val uri = FileProvider.getUriForFile(context, "com.family.rentmanager.fileprovider", file)
      val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "application/pdf"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(Intent.EXTRA_SUBJECT, "إيصال استلام إيجار - $tenantName")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      }
      context.startActivity(Intent.createChooser(shareIntent, "مشاركة وطباعة إيصال الاستلام"))
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }

  data class ReportItem(
    val propertyName: String,
    val totalUnits: Int,
    val activeLeases: Int,
    val expected: Double,
    val collected: Double,
    val overdue: Double,
    val rate: Int
  )
}
