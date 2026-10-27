package com.example.util

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object Formatters {

  fun toArabicIndicDigits(input: String): String {
    val arabicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
    val sb = StringBuilder()
    for (ch in input) {
      if (ch in '0'..'9') {
        sb.append(arabicDigits[ch - '0'])
      } else {
        sb.append(ch)
      }
    }
    return sb.toString()
  }

  fun toArabicIndicDigits(number: Number): String {
    return toArabicIndicDigits(number.toString())
  }

  fun formatCurrency(amount: Double, language: String = "ar"): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("#,##0", symbols)
    val formatted = formatter.format(amount)
    return if (language == "ar") {
      "$formatted ج.م"
    } else {
      "$formatted EGP"
    }
  }

  fun formatNumber(number: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("#,##0", symbols)
    return formatter.format(number)
  }

  // Format ISO YYYY-MM-DD to UI format DD/MM/YYYY
  fun formatUIDate(isoDate: String): String {
    if (isoDate.isBlank()) return ""
    return try {
      val parts = isoDate.split("-")
      if (parts.size == 3) {
        "${parts[2]}/${parts[1]}/${parts[0]}"
      } else {
        isoDate
      }
    } catch (e: Exception) {
      isoDate
    }
  }

  // Format ISO YYYY-MM-DD to PDF format YYYY/MM/DD
  fun formatPDFDate(isoDate: String, useArabicIndic: Boolean = true): String {
    if (isoDate.isBlank()) return ""
    val res = try {
      val parts = isoDate.split("-")
      if (parts.size == 3) {
        "${parts[0]}/${parts[1]}/${parts[2]}"
      } else {
        isoDate.replace("-", "/")
      }
    } catch (e: Exception) {
      isoDate
    }
    return if (useArabicIndic) toArabicIndicDigits(res) else res
  }

  // Today in ISO format YYYY-MM-DD
  fun getTodayIso(): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return sdf.format(Date())
  }

  fun parseIsoDate(isoDate: String): Date {
    return try {
      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
      sdf.parse(isoDate) ?: Date()
    } catch (e: Exception) {
      Date()
    }
  }

  fun getDaysFromToday(isoDate: String): Int {
    if (isoDate.isBlank()) return 0
    return try {
      val target = parseIsoDate(isoDate)
      val today = Date()
      val diffMillis = target.time - today.time
      (diffMillis / (1000 * 60 * 60 * 24)).toInt()
    } catch (e: Exception) {
      0
    }
  }

  fun getRemainingDaysText(daysLeft: Int, language: String = "ar"): String {
    val isAr = language == "ar"
    return when {
      daysLeft < 0 -> if (isAr) "منتهي منذ ${Math.abs(daysLeft)} يوم" else "Expired ${Math.abs(daysLeft)}d ago"
      daysLeft == 0 -> if (isAr) "ينتهي اليوم" else "Expires today"
      daysLeft == 1 -> if (isAr) "يوم واحد متبقي" else "1 day left"
      daysLeft == 2 -> if (isAr) "يومان متبقيان" else "2 days left"
      daysLeft in 3..10 -> if (isAr) "$daysLeft أيام متبقية" else "$daysLeft days left"
      else -> if (isAr) "$daysLeft يوماً متبقياً" else "$daysLeft days left"
    }
  }

  fun calculateDurationText(startDateIso: String, endDateIso: String, language: String = "ar"): String {
    return try {
      val start = parseIsoDate(startDateIso)
      val end = parseIsoDate(endDateIso)

      val calStart = Calendar.getInstance().apply { time = start }
      val calEnd = Calendar.getInstance().apply {
        time = end
        add(Calendar.DAY_OF_MONTH, 1) // inclusive
      }

      val yearDiff = calEnd.get(Calendar.YEAR) - calStart.get(Calendar.YEAR)
      var monthDiff = yearDiff * 12 + (calEnd.get(Calendar.MONTH) - calStart.get(Calendar.MONTH))
      val dayDiff = calEnd.get(Calendar.DAY_OF_MONTH) - calStart.get(Calendar.DAY_OF_MONTH)
      if (dayDiff > 15) monthDiff++

      if (language == "ar") {
        when {
          monthDiff == 12 -> "سنة"
          monthDiff == 24 -> "سنتين"
          monthDiff == 36 -> "ثلاث سنوات"
          monthDiff == 48 -> "أربع سنوات"
          monthDiff == 60 -> "خمس سنوات"
          monthDiff % 12 == 0 -> "${monthDiff / 12} سنوات"
          monthDiff == 1 -> "شهر واحد"
          monthDiff == 2 -> "شهرين"
          monthDiff in 3..10 -> "$monthDiff أشهر"
          else -> "$monthDiff شهراً"
        }
      } else {
        when {
          monthDiff == 12 -> "1 Year"
          monthDiff == 24 -> "2 Years"
          monthDiff % 12 == 0 -> "${monthDiff / 12} Years"
          else -> "$monthDiff Months"
        }
      }
    } catch (e: Exception) {
      if (language == "ar") "سنة" else "1 Year"
    }
  }

  fun generateMonthlyDueDates(startDateIso: String, endDateIso: String, dueDay: Int): List<String> {
    val dates = mutableListOf<String>()
    try {
      val startCal = Calendar.getInstance().apply {
        time = parseIsoDate(startDateIso)
        set(Calendar.DAY_OF_MONTH, 1)
      }
      val endCal = Calendar.getInstance().apply {
        time = parseIsoDate(endDateIso)
        set(Calendar.DAY_OF_MONTH, 1)
      }

      val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

      while (!startCal.after(endCal)) {
        val maxDayInMonth = startCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val actualDay = dueDay.coerceAtMost(maxDayInMonth).coerceAtLeast(1)

        val currentDueDateCal = Calendar.getInstance().apply {
          time = startCal.time
          set(Calendar.DAY_OF_MONTH, actualDay)
        }

        dates.add(sdf.format(currentDueDateCal.time))
        startCal.add(Calendar.MONTH, 1)
      }
    } catch (e: Exception) {
      // Fallback
    }
    return dates
  }

  fun formatCurrencyInWords(amount: Double, language: String = "ar"): String {
    val integerPart = amount.toLong()
    if (language != "ar") {
      return "Only ${formatNumber(amount)} Egyptian Pounds"
    }
    if (integerPart <= 0) return "صفر جنيهاً مصرياً لا غير"
    val words = arabicNumberToWords(integerPart)
    return "فقط $words جنيهاً مصرياً لا غير"
  }

  private fun arabicNumberToWords(num: Long): String {
    if (num == 0L) return "صفر"
    if (num < 0L) return "سالب " + arabicNumberToWords(-num)

    val parts = mutableListOf<String>()

    val millions = num / 1_000_000
    val thousands = (num % 1_000_000) / 1000
    val remainder = num % 1000

    if (millions > 0) {
      when (millions) {
        1L -> parts.add("مليون")
        2L -> parts.add("مليونان")
        in 3L..10L -> parts.add("${convertThreeDigitsToArabic(millions.toInt())} ملايين")
        else -> parts.add("${convertThreeDigitsToArabic(millions.toInt())} مليوناً")
      }
    }

    if (thousands > 0) {
      when (thousands) {
        1L -> parts.add("ألف")
        2L -> parts.add("ألفان")
        in 3L..10L -> parts.add("${convertThreeDigitsToArabic(thousands.toInt())} آلاف")
        else -> parts.add("${convertThreeDigitsToArabic(thousands.toInt())} ألفاً")
      }
    }

    if (remainder > 0) {
      parts.add(convertThreeDigitsToArabic(remainder.toInt()))
    }

    return parts.joinToString(" و ")
  }

  private fun convertThreeDigitsToArabic(n: Int): String {
    val ones = arrayOf("", "واحد", "اثنان", "ثلاثة", "أربعة", "خمسة", "ستة", "سبعة", "ثمانية", "تسعة")
    val teens = arrayOf("عشرة", "أحد عشر", "اثنا عشر", "ثلاثة عشر", "أربعة عشر", "خمسة عشر", "ستة عشر", "سبعة عشر", "ثمانية عشر", "تسعة عشر")
    val tens = arrayOf("", "عشرة", "عشرون", "ثلاثون", "أربعون", "خمسون", "ستون", "سبعون", "ثمانون", "تسعون")
    val hundreds = arrayOf("", "مائة", "مائتان", "ثلاثمائة", "أربعمائة", "خمسمائة", "ستمائة", "سبعمائة", "ثمانمائة", "تسعمائة")

    val h = n / 100
    val r = n % 100
    val t = r / 10
    val o = r % 10

    val parts = mutableListOf<String>()

    if (h > 0) {
      parts.add(hundreds[h])
    }

    if (r in 10..19) {
      parts.add(teens[r - 10])
    } else {
      if (o > 0 && t > 0) {
        parts.add("${ones[o]} و ${tens[t]}")
      } else if (o > 0) {
        parts.add(ones[o])
      } else if (t > 0) {
        parts.add(tens[t])
      }
    }

    return parts.joinToString(" و ")
  }
}

