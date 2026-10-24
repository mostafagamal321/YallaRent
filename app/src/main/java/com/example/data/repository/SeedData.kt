package com.example.data.repository

import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UnitItem
import com.example.util.Formatters

object SeedData {

  val INITIAL_PROPERTIES = listOf(
    Property(
      id = "prop-1",
      name = "ع مسجد الغفور",
      type = PropertyType.APARTMENTS,
      address = "الفيوم – الجون – نهاية ش أبو بكر الصديق – مسجد الغفور"
    ),
    Property(
      id = "prop-2",
      name = "محلات ع مسجد الغفور",
      type = PropertyType.SHOPS,
      address = "الفيوم – الجون – ش أبو بكر الصديق – محلات مسجد الغفور"
    ),
    Property(
      id = "prop-3",
      name = "ع مسجد التوحيد",
      type = PropertyType.APARTMENTS,
      address = "الفيوم – الجون – عمارة مسجد التوحيد"
    ),
    Property(
      id = "prop-4",
      name = "ع الفيوم الجديدة",
      type = PropertyType.APARTMENTS,
      address = "الفيوم الجديدة – عمارة الفيوم الجديدة"
    ),
    Property(
      id = "prop-5",
      name = "شقة برج الانشاء والتعمير",
      type = PropertyType.APARTMENTS,
      address = "الفيوم – برج الانشاء والتعمير"
    ),
    Property(
      id = "prop-6",
      name = "شقة المهندسين",
      type = PropertyType.APARTMENTS,
      address = "الفيوم – حي المهندسين"
    ),
    Property(
      id = "prop-7",
      name = "شقة المهندسين (2)",
      type = PropertyType.APARTMENTS,
      address = "الفيوم – حي المهندسين (٢)"
    )
  )

  val INITIAL_UNITS = listOf(
    // Property 1: ع مسجد الغفور
    UnitItem("unit-1-1", "prop-1", "1", "شقة سكنية", "المستأجر السابق أو المتوقع: احمد حامد"),
    UnitItem("unit-1-4", "prop-1", "4", "شقة سكنية", "المستأجر السابق أو المتوقع: كمال"),
    UnitItem("unit-1-5", "prop-1", "5", "شقة سكنية", "المستأجر السابق أو المتوقع: احمد عرفه"),
    UnitItem("unit-1-6", "prop-1", "6", "شقة سكنية"),
    UnitItem("unit-1-8", "prop-1", "8", "شقة سكنية"),
    UnitItem("unit-1-9", "prop-1", "9", "شقة سكنية", "المستأجر السابق أو المتوقع: محمد حامد"),
    UnitItem("unit-1-11", "prop-1", "11", "شقة سكنية", "المستأجر السابق أو المتوقع: محمد اشرف"),
    UnitItem("unit-1-12", "prop-1", "12", "شقة سكنية", "المستأجر السابق أو المتوقع: احمد رجب"),
    UnitItem("unit-1-13", "prop-1", "13", "شقة سكنية"),
    UnitItem("unit-1-14", "prop-1", "14", "شقة سكنية", "المستأجر السابق أو المتوقع: محمد عزت"),
    UnitItem("unit-1-15", "prop-1", "15", "شقه ٣ حجرات وصالة"),
    UnitItem("unit-1-16", "prop-1", "16", "شقة سكنية"),
    UnitItem("unit-1-17", "prop-1", "17", "شقة سكنية"),

    // Property 2: محلات ع مسجد الغفور (shops)
    UnitItem("unit-2-1", "prop-2", "1", "محل تجاري"),
    UnitItem("unit-2-2", "prop-2", "2", "محل تجاري"),
    UnitItem("unit-2-3", "prop-2", "3", "محل تجاري", "المستأجر السابق أو المتوقع: سيد"),
    UnitItem("unit-2-4", "prop-2", "4", "محل تجاري", "شاغرة"),
    UnitItem("unit-2-5", "prop-2", "5", "محل تجاري", "المستأجر السابق أو المتوقع: عماد وهيب"),
    UnitItem("unit-2-6", "prop-2", "6", "محل تجاري", "المستأجر السابق أو المتوقع: حسام"),

    // Property 3: ع مسجد التوحيد
    UnitItem("unit-3-1", "prop-3", "1", "شقة سكنية", "المستأجر السابق أو المتوقع: شعبان"),
    UnitItem("unit-3-2", "prop-3", "2", "شقة سكنية", "المستأجر السابق أو المتوقع: محمد الشكلي"),
    UnitItem("unit-3-3", "prop-3", "3", "شقة سكنية", "المستأجر السابق أو المتوقع: محمد عزت"),
    UnitItem("unit-3-4", "prop-3", "4", "شقة سكنية"),
    UnitItem("unit-3-5", "prop-3", "5", "شقة سكنية"),
    UnitItem("unit-3-6", "prop-3", "6", "شقة سكنية"),

    // Property 4: ع الفيوم الجديدة
    UnitItem("unit-4-1", "prop-4", "1", "شقة سكنية", "المستأجر السابق أو المتوقع: احمد سيد"),
    UnitItem("unit-4-2", "prop-4", "2", "شقة سكنية"),
    UnitItem("unit-4-3", "prop-4", "3", "شقة سكنية"),
    UnitItem("unit-4-4", "prop-4", "4", "شقة سكنية", "المستأجر السابق أو المتوقع: احمد جمال"),

    // Property 5: شقة برج الانشاء والتعمير
    UnitItem("unit-5-1", "prop-5", "1", "شقة سكنية", "المالك / منال حسن"),

    // Property 6: شقة المهندسين
    UnitItem("unit-6-1", "prop-6", "1", "شقة سكنية", "المستأجر السابق أو المتوقع: عمرو"),

    // Property 7: شقة المهندسين (2)
    UnitItem("unit-7-1", "prop-7", "1", "شقة سكنية", "المالك / منال حسن")
  )

  val INITIAL_TENANTS = listOf(
    com.example.data.model.Tenant(
      id = "tnt-1",
      fullName = "محي الدين مصطفي عثمان",
      nationalId = "29001010101234",
      job = "موظف حكومي",
      phone = "01001234567",
      address = "الفيوم – حي الجون",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-2",
      fullName = "عمرو إسماعيل ربيع مراد",
      nationalId = "28802020202345",
      job = "أعمال حرة",
      phone = "01112345678",
      address = "الفيوم – الحواتم",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-3",
      fullName = "ماهر سمير جرجس بسطوروس",
      nationalId = "28503030303456",
      job = "محاسب قانوني",
      phone = "01223456789",
      address = "الفيوم – شارع البحر",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-4",
      fullName = "محمد سيد قرني عباس حسن",
      nationalId = "29204040404567",
      job = "مهندس مدني",
      phone = "01098765432",
      address = "الفيوم – المسلة",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-5",
      fullName = "حسام الدين نجم الدين ربيع حسن الريفي",
      nationalId = "28905050505678",
      job = "طبيب بشري",
      phone = "01187654321",
      address = "الفيوم – دله",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-6",
      fullName = "احمد طلعت عبد الفتاح عبد الحليم",
      nationalId = "29406060606789",
      job = "تاجر مستلزمات",
      phone = "01276543210",
      address = "الفيوم – كيمان فارس",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-7",
      fullName = "احمد محمود صوفي محمود",
      nationalId = "28707070707890",
      job = "صاحب نشاط تجاري",
      phone = "01511223344",
      address = "الفيوم – شارع أبو بكر الصديق",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-8",
      fullName = "محمود محمد محمود علي",
      nationalId = "29108080808901",
      job = "صاحب محل بقالة",
      phone = "01033445566",
      address = "الفيوم – الجون",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-9",
      fullName = "حماد محمد إبراهيم جداوي",
      nationalId = "28609090909012",
      job = "أستاذ جامعي",
      phone = "01155667788",
      address = "الفيوم – باغوص",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-10",
      fullName = "اسلام حسام عطا محمود",
      nationalId = "29510101010123",
      job = "مهندس برمجيات",
      phone = "01288990011",
      address = "الفيوم – حي الجامعة",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-11",
      fullName = "حسن عبد الجواد وزير عبد الجواد",
      nationalId = "28311111111234",
      job = "معلم لغة عربية",
      phone = "01066778899",
      address = "الفيوم – حي التفتيش",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-12",
      fullName = "يسري محمد محمد علي",
      nationalId = "29012121212345",
      job = "أخصائي شؤون إدارية",
      phone = "01144556677",
      address = "الفيوم الجديدة",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-13",
      fullName = "عبد الرحمن حامد جبالي زايد",
      nationalId = "29301010101567",
      job = "فني أجهزة طبية",
      phone = "01222334455",
      address = "الفيوم الجديدة",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-14",
      fullName = "مريم رفعت عطية بطرس",
      nationalId = "29602020202678",
      job = "صيدلانية",
      phone = "01055443322",
      address = "الفيوم – برج الإنشاء والتعمير",
      nationality = "مصري"
    ),
    com.example.data.model.Tenant(
      id = "tnt-15",
      fullName = "محمد احمد محمد احمد الدش",
      nationalId = "28803030303789",
      job = "مدير مالي وإداري",
      phone = "01199887766",
      address = "الفيوم – حي المهندسين",
      nationality = "مصري"
    )
  )

  val INITIAL_CONTRACTS = listOf(
    // Property 1
    Contract(
      id = "cnt-1-6",
      unitId = "unit-1-6",
      tenantId = "tnt-1",
      tenantName = "محي الدين مصطفي عثمان",
      nationalId = "29001010101234",
      startDate = "2026-09-01",
      endDate = "2028-08-30",
      monthlyRent = 4000.0,
      rentDueDay = 1,
      contractDate = "2026-09-01",
      depositAmount = 4000.0,
      tenantJob = "موظف حكومي",
      tenantAddress = "الفيوم – حي الجون"
    ),
    Contract(
      id = "cnt-1-8",
      unitId = "unit-1-8",
      tenantId = "tnt-2",
      tenantName = "عمرو إسماعيل ربيع مراد",
      nationalId = "28802020202345",
      startDate = "2026-09-01",
      endDate = "2027-08-30",
      monthlyRent = 2850.0,
      rentDueDay = 1,
      contractDate = "2026-09-01",
      depositAmount = 2850.0,
      tenantJob = "أعمال حرة",
      tenantAddress = "الفيوم – الحواتم"
    ),
    Contract(
      id = "cnt-1-13",
      unitId = "unit-1-13",
      tenantId = "tnt-3",
      tenantName = "ماهر سمير جرجس بسطوروس",
      nationalId = "28503030303456",
      startDate = "2026-06-01",
      endDate = "2027-05-30",
      monthlyRent = 4000.0,
      rentDueDay = 1,
      contractDate = "2026-06-01",
      depositAmount = 4000.0,
      tenantJob = "محاسب قانوني",
      tenantAddress = "الفيوم – شارع البحر"
    ),
    Contract(
      id = "cnt-1-15",
      unitId = "unit-1-15",
      tenantId = "tnt-4",
      tenantName = "محمد سيد قرني عباس حسن",
      nationalId = "29204040404567",
      startDate = "2026-07-01",
      endDate = "2027-06-30",
      monthlyRent = 2600.0,
      rentDueDay = 1,
      contractDate = "2026-05-30",
      depositAmount = 2700.0,
      tenantJob = "مهندس مدني",
      tenantAddress = "الفيوم – المسلة"
    ),
    Contract(
      id = "cnt-1-16",
      unitId = "unit-1-16",
      tenantId = "tnt-5",
      tenantName = "حسام الدين نجم الدين ربيع حسن الريفي",
      nationalId = "28905050505678",
      startDate = "2026-10-15",
      endDate = "2028-10-14",
      monthlyRent = 4500.0,
      rentDueDay = 15,
      contractDate = "2026-10-15",
      depositAmount = 4500.0,
      tenantJob = "طبيب بشري",
      tenantAddress = "الفيوم – دله"
    ),
    Contract(
      id = "cnt-1-17",
      unitId = "unit-1-17",
      tenantId = "tnt-6",
      tenantName = "احمد طلعت عبد الفتاح عبد الحليم",
      nationalId = "29406060606789",
      startDate = "2026-07-01",
      endDate = "2028-06-30",
      monthlyRent = 6000.0,
      rentDueDay = 1,
      contractDate = "2026-07-01",
      depositAmount = 6000.0,
      tenantJob = "تاجر مستلزمات",
      tenantAddress = "الفيوم – كيمان فارس"
    ),

    // Property 2 (Shops)
    Contract(
      id = "cnt-2-1",
      unitId = "unit-2-1",
      tenantId = "tnt-7",
      tenantName = "احمد محمود صوفي محمود",
      nationalId = "28707070707890",
      startDate = "2026-05-01",
      endDate = "2027-04-30",
      monthlyRent = 1200.0,
      rentDueDay = 1,
      usagePurpose = "محل تجاري",
      contractDate = "2026-05-01",
      depositAmount = 1200.0,
      tenantJob = "صاحب نشاط تجاري",
      tenantAddress = "الفيوم – شارع أبو بكر الصديق"
    ),
    Contract(
      id = "cnt-2-2",
      unitId = "unit-2-2",
      tenantId = "tnt-8",
      tenantName = "محمود محمد محمود علي",
      nationalId = "29108080808901",
      startDate = "2026-09-01",
      endDate = "2028-08-30",
      monthlyRent = 1100.0,
      rentDueDay = 1,
      usagePurpose = "محل تجاري",
      contractDate = "2026-09-01",
      depositAmount = 1100.0,
      tenantJob = "صاحب محل بقالة",
      tenantAddress = "الفيوم – الجون"
    ),

    // Property 3
    Contract(
      id = "cnt-3-4",
      unitId = "unit-3-4",
      tenantId = "tnt-9",
      tenantName = "حماد محمد إبراهيم جداوي",
      nationalId = "28609090909012",
      startDate = "2026-07-01",
      endDate = "2027-06-30",
      monthlyRent = 5000.0,
      rentDueDay = 1,
      contractDate = "2026-07-01",
      depositAmount = 5000.0,
      tenantJob = "أستاذ جامعي",
      tenantAddress = "الفيوم – باغوص"
    ),
    Contract(
      id = "cnt-3-5",
      unitId = "unit-3-5",
      tenantId = "tnt-10",
      tenantName = "اسلام حسام عطا محمود",
      nationalId = "29510101010123",
      startDate = "2026-07-01",
      endDate = "2027-06-30",
      monthlyRent = 5000.0,
      rentDueDay = 1,
      contractDate = "2026-07-01",
      depositAmount = 5000.0,
      tenantJob = "مهندس برمجيات",
      tenantAddress = "الفيوم – حي الجامعة"
    ),
    Contract(
      id = "cnt-3-6",
      unitId = "unit-3-6",
      tenantId = "tnt-11",
      tenantName = "حسن عبد الجواد وزير عبد الجواد",
      nationalId = "28311111111234",
      startDate = "2026-06-01",
      endDate = "2027-05-30",
      monthlyRent = 2300.0,
      rentDueDay = 1,
      contractDate = "2026-06-01",
      depositAmount = 2300.0,
      tenantJob = "معلم لغة عربية",
      tenantAddress = "الفيوم – حي التفتيش"
    ),

    // Property 4
    Contract(
      id = "cnt-4-2",
      unitId = "unit-4-2",
      tenantId = "tnt-12",
      tenantName = "يسري محمد محمد علي",
      nationalId = "29012121212345",
      startDate = "2026-08-01",
      endDate = "2027-07-30",
      monthlyRent = 3000.0,
      rentDueDay = 1,
      contractDate = "2026-08-01",
      depositAmount = 3000.0,
      tenantJob = "أخصائي شؤون إدارية",
      tenantAddress = "الفيوم الجديدة"
    ),
    Contract(
      id = "cnt-4-3",
      unitId = "unit-4-3",
      tenantId = "tnt-13",
      tenantName = "عبد الرحمن حامد جبالي زايد",
      nationalId = "29301010101567",
      startDate = "2026-08-01",
      endDate = "2027-07-30",
      monthlyRent = 3000.0,
      rentDueDay = 1,
      contractDate = "2026-08-01",
      depositAmount = 3000.0,
      tenantJob = "فني أجهزة طبية",
      tenantAddress = "الفيوم الجديدة"
    ),

    // Property 5
    Contract(
      id = "cnt-5-1",
      unitId = "unit-5-1",
      tenantId = "tnt-14",
      tenantName = "مريم رفعت عطية بطرس",
      nationalId = "29602020202678",
      startDate = "2026-09-01",
      endDate = "2027-08-30",
      monthlyRent = 5500.0,
      rentDueDay = 1,
      notes = "المالك / منال حسن",
      contractDate = "2026-09-01",
      depositAmount = 5500.0,
      tenantJob = "صيدلانية",
      tenantAddress = "الفيوم – برج الإنشاء والتعمير"
    ),

    // Property 7
    Contract(
      id = "cnt-7-1",
      unitId = "unit-7-1",
      tenantId = "tnt-15",
      tenantName = "محمد احمد محمد احمد الدش",
      nationalId = "28803030303789",
      startDate = "2026-08-01",
      endDate = "2027-07-30",
      monthlyRent = 7000.0,
      rentDueDay = 1,
      notes = "المالك / منال حسن",
      contractDate = "2026-08-01",
      depositAmount = 7000.0,
      tenantJob = "مدير مالي وإداري",
      tenantAddress = "الفيوم – حي المهندسين"
    )
  )

  fun generateInitialPayments(): List<Payment> {
    val payments = mutableListOf<Payment>()
    val today = "2026-10-24"

    for (contract in INITIAL_CONTRACTS) {
      val dueDates = Formatters.generateMonthlyDueDates(
        contract.startDate,
        contract.endDate,
        contract.rentDueDay
      )

      dueDates.forEachIndexed { idx, dueDate ->
        val isPast = dueDate < today
        val isOctober = dueDate.startsWith("2026-10")

        val status: PaymentStatus
        val paidDate: String?
        val paidAmount: Double?

        if (isPast) {
          if (contract.id == "cnt-1-15" && isOctober) {
            status = PaymentStatus.OVERDUE
            paidDate = null
            paidAmount = null
          } else if (contract.id == "cnt-3-6" && isOctober) {
            status = PaymentStatus.OVERDUE
            paidDate = null
            paidAmount = null
          } else {
            status = PaymentStatus.PAID
            paidDate = dueDate
            paidAmount = contract.monthlyRent
          }
        } else {
          status = PaymentStatus.PENDING
          paidDate = null
          paidAmount = null
        }

        payments.add(
          Payment(
            id = "pay-${contract.id}-${idx + 1}",
            contractId = contract.id,
            unitId = contract.unitId,
            dueDate = dueDate,
            amount = contract.monthlyRent,
            paidAmount = paidAmount,
            paidDate = paidDate,
            status = status,
            note = if (status == PaymentStatus.PAID) "تم السداد نقداً بإيصال" else null
          )
        )
      }
    }
    return payments
  }
}
