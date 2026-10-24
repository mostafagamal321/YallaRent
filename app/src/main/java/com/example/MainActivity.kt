package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.Contract
import com.example.data.model.PaymentStatus
import com.example.ui.components.AppBottomNav
import com.example.ui.components.AppTopBar
import com.example.ui.components.PinLockDialog
import com.example.ui.dialogs.ContractDialog
import com.example.ui.dialogs.PaymentDialog
import com.example.ui.dialogs.UnitDetailDialog
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.PaymentsScreen
import com.example.ui.screens.PropertiesScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.RentManagerTheme
import com.example.ui.theme.SoftBackground
import com.example.ui.viewmodel.RentViewModel
import com.example.ui.viewmodel.ScreenTab
import com.example.util.Formatters

class MainActivity : ComponentActivity() {

  private val viewModel: RentViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      val settings by viewModel.settings.collectAsStateWithLifecycle()
      val layoutDirection = if (settings.language == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr

      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        RentManagerTheme {
          RentManagerApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun RentManagerApp(viewModel: RentViewModel) {
  val settings by viewModel.settings.collectAsStateWithLifecycle()
  val properties by viewModel.properties.collectAsStateWithLifecycle()
  val units by viewModel.units.collectAsStateWithLifecycle()
  val contracts by viewModel.contracts.collectAsStateWithLifecycle()
  val payments by viewModel.payments.collectAsStateWithLifecycle()
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
  val isPinLocked by viewModel.isPinLocked.collectAsStateWithLifecycle()
  val stats by viewModel.dashboardStats.collectAsStateWithLifecycle()
  val alerts by viewModel.alerts.collectAsStateWithLifecycle()

  // Dialog & Navigation states
  var showContractDialog by remember { mutableStateOf(false) }
  var contractDialogUnitId by remember { mutableStateOf<String?>(null) }
  var contractDialogExistingContract by remember { mutableStateOf<Contract?>(null) }

  var activeUnitDetailId by remember { mutableStateOf<String?>(null) }
  var activePaymentDialogId by remember { mutableStateOf<String?>(null) }

  if (isPinLocked) {
    PinLockDialog(
      language = settings.language,
      onUnlock = { enteredPin ->
        viewModel.unlockPin(enteredPin)
      }
    )
    return
  }

  // Handle Android back button
  val isAnyDialogOpen = showContractDialog || activeUnitDetailId != null || activePaymentDialogId != null
  BackHandler(enabled = isAnyDialogOpen) {
    if (showContractDialog) {
      showContractDialog = false
      contractDialogExistingContract = null
      contractDialogUnitId = null
    } else if (activePaymentDialogId != null) {
      activePaymentDialogId = null
    } else if (activeUnitDetailId != null) {
      activeUnitDetailId = null
    }
  }

  BackHandler(enabled = !isAnyDialogOpen && activeTab != ScreenTab.DASHBOARD) {
    viewModel.setTab(ScreenTab.DASHBOARD)
  }

  Scaffold(
    topBar = {
      AppTopBar(
        language = settings.language,
        unreadCount = alerts.size,
        searchQuery = searchQuery,
        onSearchChange = { viewModel.setSearchQuery(it) },
        onToggleLanguage = { viewModel.toggleLanguage() },
        onOpenNotifications = { viewModel.setTab(ScreenTab.NOTIFICATIONS) }
      )
    },
    bottomBar = {
      AppBottomNav(
        currentTab = activeTab,
        onSelectTab = { viewModel.setTab(it) },
        language = settings.language,
        unreadCount = alerts.size
      )
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .imePadding()
        .background(SoftBackground)
    ) {
      when (activeTab) {
        ScreenTab.DASHBOARD -> {
          DashboardScreen(
            stats = stats,
            properties = properties,
            units = units,
            contracts = contracts,
            payments = payments,
            language = settings.language,
            onNavigateTab = { viewModel.setTab(it) },
            onOpenContractDialog = { unitId, contract ->
              contractDialogUnitId = unitId
              contractDialogExistingContract = contract
              showContractDialog = true
            },
            onOpenPaymentDialog = { paymentId ->
              activePaymentDialogId = paymentId
            },
            onOpenUnitDetail = { unitId ->
              activeUnitDetailId = unitId
            }
          )
        }
        ScreenTab.PROPERTIES -> {
          PropertiesScreen(
            properties = properties,
            units = units,
            contracts = contracts,
            searchQuery = searchQuery,
            language = settings.language,
            onAddProperty = { name, type, address ->
              viewModel.addProperty(name, type, address)
            },
            onUpdateProperty = { prop ->
              viewModel.updateProperty(prop)
            },
            onAddUnit = { propId, unitNum, desc, notes ->
              viewModel.addUnit(propId, unitNum, desc, notes)
            },
            onUpdateUnit = { unit ->
              viewModel.updateUnit(unit)
            },
            onOpenContractDialog = { unitId, contract ->
              contractDialogUnitId = unitId
              contractDialogExistingContract = contract
              showContractDialog = true
            },
            onOpenUnitDetail = { unitId ->
              activeUnitDetailId = unitId
            }
          )
        }
        ScreenTab.PAYMENTS -> {
          PaymentsScreen(
            payments = payments,
            contracts = contracts,
            properties = properties,
            units = units,
            language = settings.language,
            onOpenPaymentDialog = { paymentId ->
              activePaymentDialogId = paymentId
            },
            onQuickPay = { paymentId, amount ->
              viewModel.markPaymentPaid(
                paymentId = paymentId,
                paidAmount = amount,
                paidDate = Formatters.getTodayIso(),
                note = null
              )
            }
          )
        }
        ScreenTab.NOTIFICATIONS -> {
          NotificationsScreen(
            alerts = alerts,
            language = settings.language,
            onNavigateTab = { viewModel.setTab(it) },
            onOpenUnitDetail = { unitId ->
              activeUnitDetailId = unitId
            }
          )
        }
        ScreenTab.SETTINGS -> {
          SettingsScreen(
            settings = settings,
            onUpdateSettings = { newSettings ->
              viewModel.updateSettings(newSettings)
            },
            onResetTemplate = { type ->
              viewModel.resetTemplate(type)
            },
            onExportBackup = {
              viewModel.exportBackupJson()
            },
            onImportBackup = { json ->
              viewModel.importBackupJson(json)
            }
          )
        }
        ScreenTab.REPORTS -> {
          ReportsScreen(
            properties = properties,
            units = units,
            contracts = contracts,
            payments = payments,
            settings = settings,
            onNavigateTab = { viewModel.setTab(it) }
          )
        }
      }
    }
  }

  // Contract Add / Edit Dialog
  if (showContractDialog) {
    ContractDialog(
      initialUnitId = contractDialogUnitId,
      existingContract = contractDialogExistingContract,
      units = units,
      properties = properties,
      settings = settings,
      onDismiss = {
        showContractDialog = false
        contractDialogExistingContract = null
        contractDialogUnitId = null
      },
      onSaveContract = { contract, isNew ->
        viewModel.saveContract(contract, isNew)
        showContractDialog = false
        contractDialogExistingContract = null
        contractDialogUnitId = null
      }
    )
  }

  // Unit Detail Modal (Contract, payments, renewals, history)
  activeUnitDetailId?.let { unitId ->
    UnitDetailDialog(
      unitId = unitId,
      units = units,
      properties = properties,
      contracts = contracts,
      payments = payments,
      settings = settings,
      onDismiss = {
        activeUnitDetailId = null
      },
      onEditContract = { contractToEdit ->
        activeUnitDetailId = null
        contractDialogExistingContract = contractToEdit
        contractDialogUnitId = contractToEdit.unitId
        showContractDialog = true
      },
      onRenewContract = { oldContractId, newStartDate, newEndDate, newRent ->
        viewModel.renewContract(oldContractId, newStartDate, newEndDate, newRent)
        activeUnitDetailId = null
      },
      onEndContract = { contractIdToEnd ->
        viewModel.endContract(contractIdToEnd)
        activeUnitDetailId = null
      },
      onOpenPaymentDialog = { paymentId ->
        activePaymentDialogId = paymentId
      }
    )
  }

  // Payment Recording Modal
  activePaymentDialogId?.let { paymentId ->
    val payment = payments.find { it.id == paymentId }
    if (payment != null) {
      val contract = contracts.find { it.id == payment.contractId }
      val unit = units.find { it.id == payment.unitId }
      val property = properties.find { it.id == unit?.propertyId }

      PaymentDialog(
        payment = payment,
        contract = contract,
        unit = unit,
        property = property,
        landlord = settings.landlord,
        language = settings.language,
        onDismiss = {
          activePaymentDialogId = null
        },
        onConfirmPaid = { pId, amount, date, note, rentAmt, elecAmt, waterAmt, stairsAmt, arrearsAmt ->
          viewModel.markPaymentPaid(
            paymentId = pId,
            paidAmount = amount,
            paidDate = date,
            note = note,
            rentAmount = rentAmt,
            electricityAmount = elecAmt,
            waterAmount = waterAmt,
            stairsCleaningAmount = stairsAmt,
            previousArrearsAmount = arrearsAmt
          )
          activePaymentDialogId = null
        },
        onRevertToPending = { p ->
          viewModel.updatePayment(
            p.copy(
              status = PaymentStatus.PENDING,
              paidDate = null,
              paidAmount = null
            )
          )
          activePaymentDialogId = null
        }
      )
    }
  }
}
