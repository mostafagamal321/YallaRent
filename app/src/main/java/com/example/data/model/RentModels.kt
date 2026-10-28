package com.example.data.model

import com.squareup.moshi.JsonClass

enum class PropertyType {
  APARTMENTS,
  SHOPS
}

enum class ContractStatus {
  ACTIVE,
  EXPIRING_SOON,
  EXPIRED,
  VACANT
}

enum class PaymentStatus {
  PAID,
  PENDING,
  OVERDUE
}

@JsonClass(generateAdapter = true)
data class Property(
  val id: String,
  val name: String,
  val type: PropertyType,
  val address: String
)

@JsonClass(generateAdapter = true)
data class UnitItem(
  val id: String,
  val propertyId: String,
  val unitNumber: String,
  val description: String,
  val notes: String? = null,
  val monthlyRent: Double? = null
)

@JsonClass(generateAdapter = true)
data class Tenant(
  val id: String,
  val fullName: String,
  val nationalId: String = "", // 14 digits
  val job: String = "موظف",
  val phone: String = "",
  val address: String = "الفيوم",
  val nationality: String = "مصري",
  val photoPath: String? = null,
  val idCardPhotoFrontPath: String? = null,
  val idCardPhotoBackPath: String? = null,
  val notes: String? = null
)

@JsonClass(generateAdapter = true)
data class Contract(
  val id: String,
  val unitId: String,
  val tenantId: String = "",
  val tenantName: String,
  val nationalId: String = "",
  val startDate: String, // YYYY-MM-DD
  val endDate: String,   // YYYY-MM-DD
  val monthlyRent: Double,
  val rentDueDay: Int = 1,
  val notes: String? = null,
  val status: ContractStatus = ContractStatus.ACTIVE,
  val isHistory: Boolean = false,
  val historyEndedDate: String? = null,
  
  // Extra contract fields with defaults
  val contractDate: String = "",
  val tenantJob: String = "موظف",
  val tenantAddress: String = "الفيوم",
  val tenantNationality: String = "مصري",
  val idIssuedFrom: String = "الفيوم",
  val usagePurpose: String = "سكن عائلي",
  val paymentTerms: String = "ويسدد الايجار في أول كل شهر بموجب ايصالات بتوقيع المالك او الوكيل",
  val renewalTerms: String = "موافقه كتابية صريحة من المالك",
  val water: String = "على المستأجر",
  val electricity: String = "على المستأجر",
  val stairsLight: String = "على المستأجر",
  val doormanFee: String = "لا يوجد",
  val depositAmount: Double = 0.0,
  val extraClause: String = ""
)

@JsonClass(generateAdapter = true)
data class Payment(
  val id: String,
  val contractId: String,
  val unitId: String,
  val dueDate: String, // YYYY-MM-DD
  val amount: Double,
  val paidAmount: Double? = null,
  val paidDate: String? = null, // YYYY-MM-DD
  val status: PaymentStatus = PaymentStatus.PENDING,
  val note: String? = null,
  val rentAmount: Double? = null,
  val electricityAmount: Double? = null,
  val waterAmount: Double? = null,
  val stairsCleaningAmount: Double? = null,
  val previousArrearsAmount: Double? = null
)

@JsonClass(generateAdapter = true)
data class LandlordDetails(
  val name: String = "جمال حامد عبد الوهاب",
  val job: String = "مهندس مدني حر",
  val address: String = "الفيوم – الجون – نهاية ش أبو بكر الصديق – مسجد الغفور",
  val nationality: String = "مصري"
)

@JsonClass(generateAdapter = true)
data class ReminderSettings(
  val rentDueDaysBefore: Int = 3,
  val rentDueOnDay: Boolean = true,
  val contractEndingDays1: Int = 60,
  val contractEndingDays2: Int = 30,
  val overdueDaily: Boolean = true,
  val reminderTime: String = "09:00"
)

@JsonClass(generateAdapter = true)
data class ContractTemplates(
  val apartments: String,
  val shops: String
)

@JsonClass(generateAdapter = true)
data class AppSettings(
  val language: String = "ar", // "ar" or "en"
  val pinEnabled: Boolean = false,
  val pinCode: String = "1234",
  val landlord: LandlordDetails = LandlordDetails(),
  val reminders: ReminderSettings = ReminderSettings(),
  val templates: ContractTemplates,
  val lastBackupDate: String? = null
)

data class InAppAlert(
  val id: String,
  val type: String, // "overdue", "rent_due", "contract_expiring", "backup"
  val title: String,
  val message: String,
  val date: String,
  val relatedContractId: String? = null,
  val relatedUnitId: String? = null
)
