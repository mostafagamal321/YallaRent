package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppSettings
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.InAppAlert
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UnitItem
import com.example.data.repository.RentRepository
import com.example.util.Formatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.example.data.model.Tenant

enum class ScreenTab {
  DASHBOARD,
  PROPERTIES,
  TENANTS,
  PAYMENTS,
  NOTIFICATIONS,
  SETTINGS,
  REPORTS
}

data class DashboardStats(
  val activeContractsCount: Int,
  val vacantUnitsCount: Int,
  val expectedMonthlyRent: Double,
  val rentCollectedThisMonth: Double,
  val totalOverdueAmount: Double,
  val expiringSoonCount: Int
)

class RentViewModel(application: Application) : AndroidViewModel(application) {

  private val repository = RentRepository(application)

  val properties: StateFlow<List<Property>> = repository.properties
  val units: StateFlow<List<UnitItem>> = repository.units
  val contracts: StateFlow<List<Contract>> = repository.contracts
  val payments: StateFlow<List<Payment>> = repository.payments
  val tenants: StateFlow<List<Tenant>> = repository.tenants
  val settings: StateFlow<AppSettings> = repository.settings

  private val _activeTab = MutableStateFlow(ScreenTab.DASHBOARD)
  val activeTab: StateFlow<ScreenTab> = _activeTab.asStateFlow()

  private val _searchQuery = MutableStateFlow("")
  val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

  private val _isPinLocked = MutableStateFlow(false)
  val isPinLocked: StateFlow<Boolean> = _isPinLocked.asStateFlow()

  init {
    val currentSettings = repository.settings.value
    if (currentSettings.pinEnabled && currentSettings.pinCode.isNotBlank()) {
      _isPinLocked.value = true
    }
  }

  fun setTab(tab: ScreenTab) {
    _activeTab.value = tab
  }

  fun setSearchQuery(query: String) {
    _searchQuery.value = query
  }

  fun toggleLanguage() {
    val current = settings.value
    val newLang = if (current.language == "ar") "en" else "ar"
    repository.updateSettings(current.copy(language = newLang))
  }

  fun unlockPin(enteredPin: String): Boolean {
    val target = settings.value.pinCode
    return if (!settings.value.pinEnabled || enteredPin == target) {
      _isPinLocked.value = false
      true
    } else {
      false
    }
  }

  // --- Computed Stats ---
  val dashboardStats: StateFlow<DashboardStats> = combine(
    contracts, units, payments
  ) { cntList, uList, payList ->
    val activeContracts = cntList.filter { !it.isHistory && it.status == ContractStatus.ACTIVE }
    val activeUnitIds = activeContracts.map { it.unitId }.toSet()
    val vacantCount = uList.count { it.id !in activeUnitIds }
    val expectedRent = activeContracts.sumOf { it.monthlyRent }

    val currentMonth = Formatters.getTodayIso().take(7) // YYYY-MM
    val collectedMonth = payList.filter {
      it.status == PaymentStatus.PAID && it.paidDate?.startsWith(currentMonth) == true
    }.sumOf { it.paidAmount ?: it.amount }

    val totalOverdue = payList.filter { it.status == PaymentStatus.OVERDUE }
      .sumOf { it.amount - (it.paidAmount ?: 0.0) }

    val expiringCount = cntList.count {
      !it.isHistory && it.status != ContractStatus.EXPIRED && Formatters.getDaysFromToday(it.endDate) in 0..60
    }

    DashboardStats(
      activeContractsCount = activeContracts.size,
      vacantUnitsCount = vacantCount,
      expectedMonthlyRent = expectedRent,
      rentCollectedThisMonth = collectedMonth,
      totalOverdueAmount = totalOverdue,
      expiringSoonCount = expiringCount
    )
  }.stateIn(
    viewModelScope,
    SharingStarted.WhileSubscribed(5000),
    DashboardStats(0, 0, 0.0, 0.0, 0.0, 0)
  )

  // --- Computed In-App Alerts ---
  val alerts: StateFlow<List<InAppAlert>> = combine(
    contracts, units, payments, properties, settings
  ) { cntList, uList, payList, propList, currentSettings ->
    val alertList = mutableListOf<InAppAlert>()
    val propMap = propList.associateBy { it.id }
    val unitMap = uList.associateBy { it.id }
    val isAr = currentSettings.language == "ar"

    // 1. Overdue payments
    val overdueList = payList.filter { it.status == PaymentStatus.OVERDUE }
    for (pay in overdueList) {
      val unit = unitMap[pay.unitId]
      val prop = unit?.let { propMap[it.propertyId] }
      val contract = cntList.find { it.id == pay.contractId }
      val tenant = contract?.tenantName ?: (if (isAr) "مستأجر" else "Tenant")

      alertList.add(
        InAppAlert(
          id = "overdue-${pay.id}",
          type = "overdue",
          title = if (isAr) "إيجار متأخر يستوجب التحصيل" else "Overdue Rent Payment",
          message = if (isAr)
            "المستأجر: $tenant - ${prop?.name ?: ""} (وحدة ${unit?.unitNumber ?: ""}) بمبلغ ${Formatters.formatCurrency(pay.amount, "ar")} مستحق منذ ${Formatters.formatUIDate(pay.dueDate)}"
          else
            "Tenant: $tenant - ${prop?.name ?: ""} (Unit ${unit?.unitNumber ?: ""}) amount ${Formatters.formatCurrency(pay.amount, "en")} due since ${Formatters.formatUIDate(pay.dueDate)}",
          date = pay.dueDate,
          relatedContractId = contract?.id,
          relatedUnitId = unit?.id
        )
      )
    }

    // 2. Rent due in upcoming days (e.g. 7 days)
    val pendingList = payList.filter { it.status == PaymentStatus.PENDING }
    for (pay in pendingList) {
      val days = Formatters.getDaysFromToday(pay.dueDate)
      if (days in 0..currentSettings.reminders.rentDueDaysBefore) {
        val unit = unitMap[pay.unitId]
        val prop = unit?.let { propMap[it.propertyId] }
        val contract = cntList.find { it.id == pay.contractId }
        val tenant = contract?.tenantName ?: (if (isAr) "مستأجر" else "Tenant")

        alertList.add(
          InAppAlert(
            id = "due-${pay.id}",
            type = "rent_due",
            title = if (days == 0) (if (isAr) "استحقاق إيجار اليوم" else "Rent Due Today") else (if (isAr) "استحقاق إيجار خلال $days أيام" else "Rent Due in $days days"),
            message = if (isAr)
              "المستأجر: $tenant - ${prop?.name ?: ""} (وحدة ${unit?.unitNumber ?: ""}) بمبلغ ${Formatters.formatCurrency(pay.amount, "ar")}"
            else
              "Tenant: $tenant - ${prop?.name ?: ""} (Unit ${unit?.unitNumber ?: ""}) amount ${Formatters.formatCurrency(pay.amount, "en")}",
            date = pay.dueDate,
            relatedContractId = contract?.id,
            relatedUnitId = unit?.id
          )
        )
      }
    }

    // 3. Contracts ending in next 60 days
    for (contract in cntList) {
      if (!contract.isHistory && contract.status != ContractStatus.EXPIRED) {
        val days = Formatters.getDaysFromToday(contract.endDate)
        if (days in 0..currentSettings.reminders.contractEndingDays1) {
          val unit = unitMap[contract.unitId]
          val prop = unit?.let { propMap[it.propertyId] }

          alertList.add(
            InAppAlert(
              id = "exp-${contract.id}",
              type = "contract_expiring",
              title = if (isAr) "عقد إيجار ينتهي قريباً ($days يوم)" else "Lease Ending Soon ($days days)",
              message = if (isAr)
                "المستأجر: ${contract.tenantName} - ${prop?.name ?: ""} (وحدة ${unit?.unitNumber ?: ""}) ينتهي في ${Formatters.formatUIDate(contract.endDate)}"
              else
                "Tenant: ${contract.tenantName} - ${prop?.name ?: ""} (Unit ${unit?.unitNumber ?: ""}) ends on ${Formatters.formatUIDate(contract.endDate)}",
              date = contract.endDate,
              relatedContractId = contract.id,
              relatedUnitId = unit?.id
            )
          )
        }
      }
    }

    // 4. Backup reminder
    val lastBackup = currentSettings.lastBackupDate
    if (lastBackup == null || Math.abs(Formatters.getDaysFromToday(lastBackup)) >= 30) {
      alertList.add(
        InAppAlert(
          id = "backup-alert",
          type = "backup",
          title = if (isAr) "تذكير بأخذ نسخة احتياطية" else "Monthly Backup Reminder",
          message = if (isAr)
            "يُنصح بحفظ نسخة احتياطية من بيانات العقود والإيجارات شهرياً من شاشة الإعدادات."
          else
            "It is recommended to take a monthly backup of your rental data from Settings.",
          date = Formatters.getTodayIso()
        )
      )
    }

    alertList
  }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

  // --- CRUD Operations ---
  fun addProperty(name: String, type: PropertyType, address: String) {
    val newProp = Property(
      id = "prop-${System.currentTimeMillis()}",
      name = name,
      type = type,
      address = address
    )
    repository.addProperty(newProp)
  }

  fun updateProperty(property: Property) {
    repository.updateProperty(property)
  }

  fun deleteProperty(propertyId: String) {
    repository.deleteProperty(propertyId)
  }

  fun addUnit(propertyId: String, unitNumber: String, description: String, notes: String?) {
    val newUnit = UnitItem(
      id = "unit-${System.currentTimeMillis()}",
      propertyId = propertyId,
      unitNumber = unitNumber,
      description = description,
      notes = notes?.ifBlank { null }
    )
    repository.addUnit(newUnit)
  }

  fun updateUnit(unit: UnitItem) {
    repository.updateUnit(unit)
  }

  fun deleteUnit(unitId: String) {
    repository.deleteUnit(unitId)
  }

  fun saveContract(contract: Contract, isNew: Boolean) {
    if (isNew) {
      val generated = contract.copy(id = "cnt-${System.currentTimeMillis()}")
      repository.createContract(generated)
    } else {
      repository.updateContract(contract)
    }
  }

  fun endContract(contractId: String) {
    repository.endContract(contractId)
  }

  fun deleteContract(contractId: String) {
    repository.deleteContract(contractId)
  }

  fun renewContract(oldContractId: String, newStartDate: String, newEndDate: String, newRent: Double?) {
    repository.renewContract(oldContractId, newStartDate, newEndDate, newRent)
  }

  fun markPaymentPaid(
    paymentId: String,
    paidAmount: Double,
    paidDate: String,
    note: String?,
    rentAmount: Double? = null,
    electricityAmount: Double? = null,
    waterAmount: Double? = null,
    stairsCleaningAmount: Double? = null,
    previousArrearsAmount: Double? = null
  ) {
    repository.markPaymentPaid(
      paymentId = paymentId,
      paidAmount = paidAmount,
      paidDate = paidDate,
      note = note,
      rentAmount = rentAmount,
      electricityAmount = electricityAmount,
      waterAmount = waterAmount,
      stairsCleaningAmount = stairsCleaningAmount,
      previousArrearsAmount = previousArrearsAmount
    )
  }

  fun updatePayment(payment: Payment) {
    repository.updatePayment(payment)
  }

  fun addTenant(tenant: Tenant) {
    repository.addTenant(tenant)
  }

  fun updateTenant(tenant: Tenant) {
    repository.updateTenant(tenant)
  }

  fun deleteTenant(tenantId: String) {
    repository.deleteTenant(tenantId)
  }

  fun updateSettings(newSettings: AppSettings) {
    repository.updateSettings(newSettings)
  }

  fun resetTemplate(type: PropertyType) {
    repository.resetTemplate(type)
  }

  fun exportBackupJson(): String {
    val json = repository.exportBackupJson()
    val current = settings.value
    repository.updateSettings(current.copy(lastBackupDate = Formatters.getTodayIso()))
    return json
  }

  fun importBackupJson(jsonString: String): Boolean {
    return repository.importBackupJson(jsonString)
  }
}
