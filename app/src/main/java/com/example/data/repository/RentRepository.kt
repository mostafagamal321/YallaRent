package com.example.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.AppSettings
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.ContractTemplates
import com.example.data.model.LandlordDetails
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.ReminderSettings
import com.example.data.model.UnitItem
import com.example.util.Formatters
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

import com.example.data.model.Tenant
import com.example.util.ImageUtils

class RentRepository(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("rent_manager_prefs", Context.MODE_PRIVATE)

  private val moshi: Moshi = Moshi.Builder()
    .addLast(KotlinJsonAdapterFactory())
    .build()

  private val propertiesType = Types.newParameterizedType(List::class.java, Property::class.java)
  private val unitsType = Types.newParameterizedType(List::class.java, UnitItem::class.java)
  private val contractsType = Types.newParameterizedType(List::class.java, Contract::class.java)
  private val paymentsType = Types.newParameterizedType(List::class.java, Payment::class.java)
  private val tenantsType = Types.newParameterizedType(List::class.java, Tenant::class.java)

  private val propertiesAdapter = moshi.adapter<List<Property>>(propertiesType)
  private val unitsAdapter = moshi.adapter<List<UnitItem>>(unitsType)
  private val contractsAdapter = moshi.adapter<List<Contract>>(contractsType)
  private val paymentsAdapter = moshi.adapter<List<Payment>>(paymentsType)
  private val tenantsAdapter = moshi.adapter<List<Tenant>>(tenantsType)
  private val settingsAdapter = moshi.adapter(AppSettings::class.java)

  private val _properties = MutableStateFlow<List<Property>>(emptyList())
  val properties: StateFlow<List<Property>> = _properties.asStateFlow()

  private val _units = MutableStateFlow<List<UnitItem>>(emptyList())
  val units: StateFlow<List<UnitItem>> = _units.asStateFlow()

  private val _contracts = MutableStateFlow<List<Contract>>(emptyList())
  val contracts: StateFlow<List<Contract>> = _contracts.asStateFlow()

  private val _payments = MutableStateFlow<List<Payment>>(emptyList())
  val payments: StateFlow<List<Payment>> = _payments.asStateFlow()

  private val _tenants = MutableStateFlow<List<Tenant>>(emptyList())
  val tenants: StateFlow<List<Tenant>> = _tenants.asStateFlow()

  private val _settings = MutableStateFlow<AppSettings>(
    AppSettings(
      templates = ContractTemplates(
        apartments = DefaultTemplates.APARTMENT_TEMPLATE,
        shops = DefaultTemplates.SHOPS_TEMPLATE
      )
    )
  )
  val settings: StateFlow<AppSettings> = _settings.asStateFlow()

  init {
    loadData()
  }

  private fun loadData() {
    val isInit = prefs.getBoolean("is_initialized", false)
    if (!isInit) {
      // Seed initial data
      val initialProperties = SeedData.INITIAL_PROPERTIES
      val initialUnits = SeedData.INITIAL_UNITS
      val initialTenants = SeedData.INITIAL_TENANTS
      val initialContracts = SeedData.INITIAL_CONTRACTS
      val initialPayments = SeedData.generateInitialPayments()
      val initialSettings = AppSettings(
        templates = ContractTemplates(
          apartments = DefaultTemplates.APARTMENT_TEMPLATE,
          shops = DefaultTemplates.SHOPS_TEMPLATE
        )
      )

      _properties.value = initialProperties
      _units.value = initialUnits
      _tenants.value = initialTenants
      _contracts.value = initialContracts
      _payments.value = initialPayments
      _settings.value = initialSettings

      saveAll(
        props = initialProperties,
        uList = initialUnits,
        cntList = initialContracts,
        payList = initialPayments,
        tenantList = initialTenants,
        appSettings = initialSettings
      )
      prefs.edit().putBoolean("is_initialized", true).apply()
    } else {
      // Load from prefs
      val propsJson = prefs.getString("properties", null)
      val unitsJson = prefs.getString("units", null)
      val contractsJson = prefs.getString("contracts", null)
      val paymentsJson = prefs.getString("payments", null)
      val tenantsJson = prefs.getString("tenants", null)
      val settingsJson = prefs.getString("settings", null)

      _properties.value = propsJson?.let { propertiesAdapter.fromJson(it) } ?: SeedData.INITIAL_PROPERTIES
      _units.value = unitsJson?.let { unitsAdapter.fromJson(it) } ?: SeedData.INITIAL_UNITS
      val loadedContracts = contractsJson?.let { contractsAdapter.fromJson(it) } ?: SeedData.INITIAL_CONTRACTS
      _payments.value = paymentsJson?.let { paymentsAdapter.fromJson(it) } ?: SeedData.generateInitialPayments()
      _settings.value = settingsJson?.let { settingsAdapter.fromJson(it) } ?: AppSettings(
        templates = ContractTemplates(
          apartments = DefaultTemplates.APARTMENT_TEMPLATE,
          shops = DefaultTemplates.SHOPS_TEMPLATE
        )
      )

      // Migration / Tenant linking
      var currentTenants = tenantsJson?.let { tenantsAdapter.fromJson(it) }
      if (currentTenants == null) {
        // Automatic migration: generate tenants for all existing occupied contracts
        val tenantMap = mutableMapOf<String, Tenant>()
        // First populate from seed tenants if available
        SeedData.INITIAL_TENANTS.forEach { tenantMap[it.fullName.trim()] = it }

        var idx = 0
        val migratedContracts = loadedContracts.map { cnt ->
          val trimmedName = cnt.tenantName.trim()
          if (trimmedName.isNotBlank()) {
            val existing = tenantMap[trimmedName]
            if (existing != null) {
              cnt.copy(tenantId = existing.id)
            } else {
              val newTenant = Tenant(
                id = "tnt-${System.currentTimeMillis()}-${idx++}",
                fullName = trimmedName,
                nationalId = cnt.nationalId,
                job = cnt.tenantJob,
                phone = "",
                address = cnt.tenantAddress,
                nationality = cnt.tenantNationality
              )
              tenantMap[trimmedName] = newTenant
              cnt.copy(tenantId = newTenant.id)
            }
          } else {
            cnt
          }
        }
        currentTenants = tenantMap.values.toList()
        _contracts.value = migratedContracts
        _tenants.value = currentTenants
        saveAll(cntList = migratedContracts, tenantList = currentTenants)
      } else {
        // Ensure any contract missing a tenantId is linked to a matching tenant
        val tenantByName = currentTenants.associateBy { it.fullName.trim() }
        var needsSave = false
        val fixedContracts = loadedContracts.map { cnt ->
          if (cnt.tenantId.isBlank() && cnt.tenantName.isNotBlank()) {
            val matched = tenantByName[cnt.tenantName.trim()]
            if (matched != null) {
              needsSave = true
              cnt.copy(tenantId = matched.id)
            } else {
              cnt
            }
          } else {
            cnt
          }
        }
        _contracts.value = fixedContracts
        _tenants.value = currentTenants
        if (needsSave) {
          saveAll(cntList = fixedContracts, tenantList = currentTenants)
        }
      }

      // Auto update overdue status
      val today = Formatters.getTodayIso()
      val updatedPayments = _payments.value.map { p ->
        if (p.status == PaymentStatus.PENDING && p.dueDate < today) {
          p.copy(status = PaymentStatus.OVERDUE)
        } else {
          p
        }
      }
      _payments.value = updatedPayments
    }
  }

  private fun saveAll(
    props: List<Property> = _properties.value,
    uList: List<UnitItem> = _units.value,
    cntList: List<Contract> = _contracts.value,
    payList: List<Payment> = _payments.value,
    tenantList: List<Tenant> = _tenants.value,
    appSettings: AppSettings = _settings.value
  ) {
    prefs.edit()
      .putString("properties", propertiesAdapter.toJson(props))
      .putString("units", unitsAdapter.toJson(uList))
      .putString("contracts", contractsAdapter.toJson(cntList))
      .putString("payments", paymentsAdapter.toJson(payList))
      .putString("tenants", tenantsAdapter.toJson(tenantList))
      .putString("settings", settingsAdapter.toJson(appSettings))
      .apply()
  }

  // --- Property CRUD ---
  fun addProperty(property: Property) {
    val updated = _properties.value + property
    _properties.value = updated
    saveAll(props = updated)
  }

  fun updateProperty(property: Property) {
    val updated = _properties.value.map { if (it.id == property.id) property else it }
    _properties.value = updated
    saveAll(props = updated)
  }

  fun deleteProperty(propertyId: String) {
    val updatedProps = _properties.value.filter { it.id != propertyId }
    val deletedUnitIds = _units.value.filter { it.propertyId == propertyId }.map { it.id }.toSet()
    val updatedUnits = _units.value.filter { it.propertyId != propertyId }
    val updatedContracts = _contracts.value.filter { it.unitId !in deletedUnitIds }
    val updatedPayments = _payments.value.filter { it.unitId !in deletedUnitIds }

    _properties.value = updatedProps
    _units.value = updatedUnits
    _contracts.value = updatedContracts
    _payments.value = updatedPayments
    saveAll(updatedProps, updatedUnits, updatedContracts, updatedPayments)
  }

  // --- UnitItem CRUD ---
  fun addUnit(unit: UnitItem) {
    val updated = _units.value + unit
    _units.value = updated
    saveAll(uList = updated)
  }

  fun updateUnit(unit: UnitItem) {
    val updated = _units.value.map { if (it.id == unit.id) unit else it }
    _units.value = updated
    saveAll(uList = updated)
  }

  fun deleteUnit(unitId: String) {
    val updatedUnits = _units.value.filter { it.id != unitId }
    val updatedContracts = _contracts.value.filter { it.unitId != unitId }
    val updatedPayments = _payments.value.filter { it.unitId != unitId }

    _units.value = updatedUnits
    _contracts.value = updatedContracts
    _payments.value = updatedPayments
    saveAll(uList = updatedUnits, cntList = updatedContracts, payList = updatedPayments)
  }

  // --- Contract CRUD ---
  fun createContract(contract: Contract) {
    // Generate payments
    val dueDates = Formatters.generateMonthlyDueDates(
      contract.startDate,
      contract.endDate,
      contract.rentDueDay
    )

    val newPayments = dueDates.mapIndexed { idx, dueDate ->
      Payment(
        id = "pay-${contract.id}-${idx + 1}",
        contractId = contract.id,
        unitId = contract.unitId,
        dueDate = dueDate,
        amount = contract.monthlyRent,
        status = PaymentStatus.PENDING
      )
    }

    val updatedContracts = _contracts.value + contract
    val updatedPayments = _payments.value + newPayments

    _contracts.value = updatedContracts
    _payments.value = updatedPayments
    saveAll(cntList = updatedContracts, payList = updatedPayments)
  }

  fun updateContract(contract: Contract) {
    val updated = _contracts.value.map { if (it.id == contract.id) contract else it }
    _contracts.value = updated
    saveAll(cntList = updated)
  }

  fun endContract(contractId: String) {
    val today = Formatters.getTodayIso()
    val updated = _contracts.value.map {
      if (it.id == contractId) {
        it.copy(
          status = ContractStatus.EXPIRED,
          isHistory = true,
          historyEndedDate = today
        )
      } else {
        it
      }
    }
    _contracts.value = updated
    saveAll(cntList = updated)
  }

  fun deleteContract(contractId: String) {
    val updatedContracts = _contracts.value.filter { it.id != contractId }
    val updatedPayments = _payments.value.filter { it.contractId != contractId }
    _contracts.value = updatedContracts
    _payments.value = updatedPayments
    saveAll(cntList = updatedContracts, payList = updatedPayments)
  }

  fun renewContract(
    oldContractId: String,
    newStartDate: String,
    newEndDate: String,
    newRent: Double?
  ) {
    val old = _contracts.value.find { it.id == oldContractId } ?: return
    val today = Formatters.getTodayIso()

    val updatedOld = old.copy(
      status = ContractStatus.EXPIRED,
      isHistory = true,
      historyEndedDate = newStartDate
    )

    val newContractId = "cnt-${System.currentTimeMillis()}"
    val rentAmount = if (newRent != null && newRent > 0) newRent else old.monthlyRent

    val newContract = old.copy(
      id = newContractId,
      startDate = newStartDate,
      endDate = newEndDate,
      monthlyRent = rentAmount,
      status = ContractStatus.ACTIVE,
      isHistory = false,
      historyEndedDate = null,
      contractDate = today
    )

    val dueDates = Formatters.generateMonthlyDueDates(
      newContract.startDate,
      newContract.endDate,
      newContract.rentDueDay
    )

    val newPayments = dueDates.mapIndexed { idx, dueDate ->
      Payment(
        id = "pay-$newContractId-${idx + 1}",
        contractId = newContractId,
        unitId = newContract.unitId,
        dueDate = dueDate,
        amount = newContract.monthlyRent,
        status = PaymentStatus.PENDING
      )
    }

    val updatedContracts = _contracts.value.map { if (it.id == oldContractId) updatedOld else it } + newContract
    val updatedPayments = _payments.value + newPayments

    _contracts.value = updatedContracts
    _payments.value = updatedPayments
    saveAll(cntList = updatedContracts, payList = updatedPayments)
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
    val updated = _payments.value.map { p ->
      if (p.id == paymentId) {
        val isFull = paidAmount >= p.amount
        p.copy(
          status = if (isFull) PaymentStatus.PAID else PaymentStatus.PENDING,
          paidAmount = paidAmount,
          paidDate = paidDate,
          note = note ?: if (isFull) "تم السداد بالكامل" else "سداد جزئي ($paidAmount من ${p.amount})",
          rentAmount = rentAmount,
          electricityAmount = electricityAmount,
          waterAmount = waterAmount,
          stairsCleaningAmount = stairsCleaningAmount,
          previousArrearsAmount = previousArrearsAmount
        )
      } else {
        p
      }
    }
    _payments.value = updated
    saveAll(payList = updated)
  }

  fun updatePayment(payment: Payment) {
    val updated = _payments.value.map { if (it.id == payment.id) payment else it }
    _payments.value = updated
    saveAll(payList = updated)
  }

  // --- Tenant CRUD ---
  fun addTenant(tenant: Tenant) {
    val updated = _tenants.value + tenant
    _tenants.value = updated
    saveAll(tenantList = updated)
  }

  fun updateTenant(tenant: Tenant) {
    val updatedTenants = _tenants.value.map { if (it.id == tenant.id) tenant else it }
    _tenants.value = updatedTenants
    // Propagate tenant fields to linked contracts
    val updatedContracts = _contracts.value.map { cnt ->
      if (cnt.tenantId == tenant.id) {
        cnt.copy(
          tenantName = tenant.fullName,
          nationalId = tenant.nationalId,
          tenantJob = tenant.job,
          tenantAddress = tenant.address,
          tenantNationality = tenant.nationality
        )
      } else {
        cnt
      }
    }
    _contracts.value = updatedContracts
    saveAll(cntList = updatedContracts, tenantList = updatedTenants)
  }

  fun deleteTenant(tenantId: String) {
    val updatedTenants = _tenants.value.filter { it.id != tenantId }
    val updatedContracts = _contracts.value.map { cnt ->
      if (cnt.tenantId == tenantId) cnt.copy(tenantId = "") else cnt
    }
    _tenants.value = updatedTenants
    _contracts.value = updatedContracts
    saveAll(cntList = updatedContracts, tenantList = updatedTenants)
  }

  // --- Settings ---
  fun updateSettings(newSettings: AppSettings) {
    _settings.value = newSettings
    saveAll(appSettings = newSettings)
  }

  fun resetTemplate(type: PropertyType) {
    val current = _settings.value
    val newTemplates = if (type == PropertyType.APARTMENTS) {
      current.templates.copy(apartments = DefaultTemplates.APARTMENT_TEMPLATE)
    } else {
      current.templates.copy(shops = DefaultTemplates.SHOPS_TEMPLATE)
    }
    updateSettings(current.copy(templates = newTemplates))
  }

  // --- Backup Export & Import with Photos ---
  fun exportBackupJson(): String {
    // Collect photos
    val photosMap = mutableMapOf<String, String>()
    for (t in _tenants.value) {
      listOfNotNull(t.photoPath, t.idCardPhotoFrontPath, t.idCardPhotoBackPath).forEach { path ->
        val file = File(path)
        if (file.exists()) {
          val b64 = ImageUtils.fileToBase64(path)
          if (b64 != null) {
            photosMap[file.name] = b64
          }
        }
      }
    }

    val bundle = mapOf(
      "version" to 2,
      "exportedAt" to Formatters.getTodayIso(),
      "properties" to _properties.value,
      "units" to _units.value,
      "tenants" to _tenants.value,
      "contracts" to _contracts.value,
      "payments" to _payments.value,
      "settings" to _settings.value,
      "photos" to photosMap
    )
    val mapAdapter = moshi.adapter<Map<String, Any>>(Map::class.java)
    return mapAdapter.toJson(bundle)
  }

  fun importBackupJson(jsonString: String): Boolean {
    return try {
      val mapAdapter = moshi.adapter<Map<String, Any>>(Map::class.java)
      val map = mapAdapter.fromJson(jsonString) ?: return false

      // Restore photos if present
      val photosRaw = map["photos"] as? Map<*, *>
      val restoredPathMap = mutableMapOf<String, String>() // filename -> new absolute path
      photosRaw?.forEach { (k, v) ->
        val fileName = k as? String
        val b64 = v as? String
        if (fileName != null && b64 != null) {
          val path = ImageUtils.base64ToFile(context, b64, fileName)
          if (path != null) {
            restoredPathMap[fileName] = path
          }
        }
      }

      val propsRaw = moshi.adapter(Any::class.java).toJson(map["properties"])
      val unitsRaw = moshi.adapter(Any::class.java).toJson(map["units"])
      val contractsRaw = moshi.adapter(Any::class.java).toJson(map["contracts"])
      val paymentsRaw = moshi.adapter(Any::class.java).toJson(map["payments"])
      val settingsRaw = moshi.adapter(Any::class.java).toJson(map["settings"])
      val tenantsRaw = map["tenants"]?.let { moshi.adapter(Any::class.java).toJson(it) }

      val newProps = propertiesAdapter.fromJson(propsRaw) ?: return false
      val newUnits = unitsAdapter.fromJson(unitsRaw) ?: return false
      val newContracts = contractsAdapter.fromJson(contractsRaw) ?: emptyList()
      val newPayments = paymentsAdapter.fromJson(paymentsRaw) ?: emptyList()
      val newSettings = settingsAdapter.fromJson(settingsRaw) ?: _settings.value
      val loadedTenants = tenantsRaw?.let { tenantsAdapter.fromJson(it) } ?: emptyList()

      // Fix tenant photo paths if restored
      val newTenants = loadedTenants.map { t ->
        t.copy(
          photoPath = t.photoPath?.let { p ->
            val name = File(p).name
            restoredPathMap[name] ?: p
          },
          idCardPhotoFrontPath = t.idCardPhotoFrontPath?.let { p ->
            val name = File(p).name
            restoredPathMap[name] ?: p
          },
          idCardPhotoBackPath = t.idCardPhotoBackPath?.let { p ->
            val name = File(p).name
            restoredPathMap[name] ?: p
          }
        )
      }

      _properties.value = newProps
      _units.value = newUnits
      _contracts.value = newContracts
      _payments.value = newPayments
      _tenants.value = newTenants
      _settings.value = newSettings

      saveAll(
        props = newProps,
        uList = newUnits,
        cntList = newContracts,
        payList = newPayments,
        tenantList = newTenants,
        appSettings = newSettings
      )
      true
    } catch (e: Exception) {
      e.printStackTrace()
      false
    }
  }
}
