package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.ui.text.input.KeyboardType
import com.example.data.model.Contract
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.util.Formatters

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentsScreen(
  payments: List<Payment>,
  contracts: List<Contract>,
  properties: List<Property>,
  units: List<UnitItem>,
  language: String,
  onOpenPaymentDialog: (paymentId: String) -> Unit,
  onQuickPay: (paymentId: String, amount: Double) -> Unit,
  onAddPayment: ((Payment) -> Unit)? = null,
  onDeletePayment: ((String) -> Unit)? = null
) {
  val isAr = language == "ar"
  val chevronIcon = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight

  var showAddPaymentDialog by remember { mutableStateOf(false) }

  var statusFilter by remember { mutableStateOf("all") }
  var propertyFilterId by remember { mutableStateOf("all") }
  var monthFilter by remember { mutableStateOf("all") }

  var propExpanded by remember { mutableStateOf(false) }
  var monthExpanded by remember { mutableStateOf(false) }

  val propMap = properties.associateBy { it.id }
  val unitMap = units.associateBy { it.id }
  val contractMap = contracts.associateBy { it.id }

  // Available months
  val monthsList = remember(payments) {
    payments.map { it.dueDate.take(7) }.distinct().sortedDescending()
  }

  // Filter payments
  val filteredPayments = payments.filter { p ->
    val matchesStatus = when (statusFilter) {
      "overdue" -> p.status == PaymentStatus.OVERDUE
      "pending" -> p.status == PaymentStatus.PENDING
      "paid" -> p.status == PaymentStatus.PAID
      else -> true
    }
    val matchesMonth = monthFilter == "all" || p.dueDate.startsWith(monthFilter)
    val matchesProp = if (propertyFilterId == "all") true else {
      val u = unitMap[p.unitId]
      u?.propertyId == propertyFilterId
    }
    matchesStatus && matchesMonth && matchesProp
  }.sortedWith(
    compareBy<Payment> { if (it.status == PaymentStatus.OVERDUE) 0 else 1 }
      .thenByDescending { it.dueDate }
  )

  val totalDue = filteredPayments.sumOf { it.amount }
  val totalCollected = filteredPayments.filter { it.status == PaymentStatus.PAID }.sumOf { it.paidAmount ?: it.amount }
  val totalOverdue = filteredPayments.filter { it.status == PaymentStatus.OVERDUE }.sumOf { it.amount - (it.paidAmount ?: 0.0) }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isAr) "سجل تحصيل الإيجارات" else "Rent Collection Ledger",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E293B)
          )
          Text(
            text = if (isAr) "إدارة ومتابعة تحصيل الأقساط الشهرية والمتأخرات" else "Track and collect monthly rent installments",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )
        }

        Button(
          onClick = { showAddPaymentDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isAr) "إضافة دفعة" else "Add Payment", fontSize = 12.sp)
        }
      }
    }

    // Summary Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = ForestGreen),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.SpaceAround,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (isAr) "إجمالي المطلوب" else "Total Due", style = MaterialTheme.typography.labelSmall, color = Color(0xFFA7F3D0))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = Formatters.formatCurrency(totalDue, language), style = MaterialTheme.typography.titleSmall, color = Color.White)
          }

          Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.2f)))

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (isAr) "المحصل" else "Collected", style = MaterialTheme.typography.labelSmall, color = Color(0xFF86EFAC))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = Formatters.formatCurrency(totalCollected, language), style = MaterialTheme.typography.titleSmall, color = Color(0xFF4ADE80))
          }

          Box(modifier = Modifier.width(1.dp).height(30.dp).background(Color.White.copy(alpha = 0.2f)))

          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = if (isAr) "المتأخرات" else "Overdue", style = MaterialTheme.typography.labelSmall, color = Color(0xFFFCA5A5))
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = Formatters.formatCurrency(totalOverdue, language), style = MaterialTheme.typography.titleSmall, color = Color(0xFFF87171))
          }
        }
      }
    }

    // Status Filter Chips
    item {
      val statusOptions = listOf(
        Pair("all", if (isAr) "الكل" else "All"),
        Pair("overdue", if (isAr) "متأخرات" else "Overdue"),
        Pair("pending", if (isAr) "مستحق" else "Pending"),
        Pair("paid", if (isAr) "تم سداده" else "Paid")
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for ((key, label) in statusOptions) {
          val isSelected = statusFilter == key
          Surface(
            onClick = { statusFilter = key },
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) ForestGreen else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ForestGreen else Color(0xFFCBD5E1)),
            modifier = Modifier.height(34.dp)
          ) {
            Box(modifier = Modifier.padding(horizontal = 14.dp), contentAlignment = Alignment.Center) {
              Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF475569),
                style = MaterialTheme.typography.labelSmall
              )
            }
          }
        }
      }
    }

    // Dropdown Filters (Property and Month)
    item {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        // Property Dropdown
        ExposedDropdownMenuBox(
          expanded = propExpanded,
          onExpandedChange = { propExpanded = !propExpanded },
          modifier = Modifier.weight(1f)
        ) {
          OutlinedTextField(
            value = if (propertyFilterId == "all") (if (isAr) "جميع العقارات" else "All Properties") else propMap[propertyFilterId]?.name ?: "",
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = propExpanded) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = propExpanded,
            onDismissRequest = { propExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text(if (isAr) "جميع العقارات" else "All Properties") },
              onClick = { propertyFilterId = "all"; propExpanded = false }
            )
            for (p in properties) {
              DropdownMenuItem(
                text = { Text(p.name) },
                onClick = { propertyFilterId = p.id; propExpanded = false }
              )
            }
          }
        }

        // Month Dropdown
        ExposedDropdownMenuBox(
          expanded = monthExpanded,
          onExpandedChange = { monthExpanded = !monthExpanded },
          modifier = Modifier.weight(1f)
        ) {
          OutlinedTextField(
            value = if (monthFilter == "all") (if (isAr) "جميع الشهور" else "All Months") else monthFilter,
            onValueChange = {},
            readOnly = true,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthExpanded) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.menuAnchor()
          )
          ExposedDropdownMenu(
            expanded = monthExpanded,
            onDismissRequest = { monthExpanded = false }
          ) {
            DropdownMenuItem(
              text = { Text(if (isAr) "جميع الشهور" else "All Months") },
              onClick = { monthFilter = "all"; monthExpanded = false }
            )
            for (m in monthsList) {
              DropdownMenuItem(
                text = { Text(m) },
                onClick = { monthFilter = m; monthExpanded = false }
              )
            }
          }
        }
      }
    }

    // Payments List
    if (filteredPayments.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
        ) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = ForestGreen,
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (payments.isEmpty()) (if (isAr) "لا توجد دفعات أو فواتير مسجلة بعد" else "No payments recorded yet")
              else (if (isAr) "لا توجد دفعات تطابق هذه التصفية" else "No payments match this filter"),
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (payments.isEmpty()) (if (isAr) "تتولد الأقساط تلقائياً عند إنشاء العقود، كما يمكنك إنشاء دفعة أو فاتورة مخصصة لأي مستأجر يدوياً الآن." else "Installments generate with leases, or you can create a custom payment bill now.")
              else (if (isAr) "جرب تغيير حالة التصفية أو اختيار عقار أو شهر آخر." else "Try clearing or selecting other filters."),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
              onClick = { showAddPaymentDialog = true },
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(10.dp)
            ) {
              Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(text = if (isAr) "إضافة دفعة / فاتورة جديدة" else "Add New Payment / Bill")
            }
          }
        }
      }
    } else {
      items(filteredPayments) { payment ->
        val contract = contractMap[payment.contractId]
        val unit = unitMap[payment.unitId]
        val prop = unit?.let { propMap[it.propertyId] }

        val isPaid = payment.status == PaymentStatus.PAID
        val isOverdue = payment.status == PaymentStatus.OVERDUE

        val (statusLabel, statusBg, statusColor) = when {
          isPaid -> Triple(if (isAr) "مسدد" else "Paid", GreenLight, GreenDark)
          isOverdue -> Triple(if (isAr) "متأخر" else "Overdue", RedLight, RedAlert)
          else -> Triple(if (isAr) "مستحق" else "Pending", Color(0xFFFEF3C7), Color(0xFFB45309))
        }

        Card(
          onClick = { onOpenPaymentDialog(payment.id) },
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(if (isOverdue) 1.5.dp else 1.dp, if (isOverdue) Color(0xFFFCA5A5) else Color(0xFFE2E8F0)),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = RoundedCornerShape(6.dp), color = statusBg) {
                  Text(
                    text = statusLabel,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = contract?.tenantName ?: (if (isAr) "مستأجر" else "Tenant"),
                  style = MaterialTheme.typography.titleSmall,
                  color = Color(0xFF0F172A)
                )
              }

              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "${prop?.name ?: ""} • ${if (isAr) "وحدة ${unit?.unitNumber}" else "Unit ${unit?.unitNumber}"} • ${if (isAr) "الاستحقاق:" else "Due:"} ${Formatters.formatUIDate(payment.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
              )

              val hasBreakdown = (payment.electricityAmount ?: 0.0) > 0 ||
                (payment.waterAmount ?: 0.0) > 0 ||
                (payment.stairsCleaningAmount ?: 0.0) > 0 ||
                (payment.previousArrearsAmount ?: 0.0) > 0

              if (hasBreakdown) {
                Spacer(modifier = Modifier.height(3.dp))
                val breakdownParts = mutableListOf<String>()
                payment.rentAmount?.let { if (it > 0) breakdownParts.add(if (isAr) "إيجار: ${it.toInt()}" else "Rent: ${it.toInt()}") }
                payment.electricityAmount?.let { if (it > 0) breakdownParts.add(if (isAr) "كهرباء: ${it.toInt()}" else "Elec: ${it.toInt()}") }
                payment.waterAmount?.let { if (it > 0) breakdownParts.add(if (isAr) "مياه: ${it.toInt()}" else "Water: ${it.toInt()}") }
                payment.stairsCleaningAmount?.let { if (it > 0) breakdownParts.add(if (isAr) "نظافة: ${it.toInt()}" else "Stairs: ${it.toInt()}") }
                payment.previousArrearsAmount?.let { if (it > 0) breakdownParts.add(if (isAr) "متأخرات: ${it.toInt()}" else "Arrears: ${it.toInt()}") }

                Text(
                  text = breakdownParts.joinToString(" • "),
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF0284C7),
                  fontSize = 11.sp
                )
              }

              if (isPaid && payment.paidDate != null) {
                Spacer(modifier = Modifier.height(3.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "${if (isAr) "سُدد في:" else "Paid on:"} ${Formatters.formatUIDate(payment.paidDate)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = GreenDark
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFFDCFCE7),
                    modifier = Modifier.padding(1.dp)
                  ) {
                    Text(
                      text = if (isAr) "إيصال PDF ✓" else "Receipt PDF ✓",
                      color = GreenDark,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                    )
                  }
                }
              }
            }

            Column(horizontalAlignment = Alignment.End) {
              Text(
                text = Formatters.formatCurrency(payment.amount, language),
                style = MaterialTheme.typography.titleSmall,
                color = if (isOverdue) RedAlert else ForestGreen
              )

              if (!isPaid) {
                Surface(
                  onClick = { onQuickPay(payment.id, payment.amount) },
                  shape = RoundedCornerShape(8.dp),
                  color = ForestGreen,
                  modifier = Modifier.padding(top = 4.dp)
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = if (isAr) "سداد سريع" else "Quick Pay", color = Color.White, style = MaterialTheme.typography.labelSmall)
                  }
                }
              } else {
                Text(
                  text = "✓ ${if (isAr) "مكتمل" else "Paid"}",
                  color = GreenDark,
                  style = MaterialTheme.typography.labelSmall,
                  modifier = Modifier.padding(top = 4.dp)
                )
              }
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(40.dp))
    }
  }

  // Create New Payment Bill Dialog
  if (showAddPaymentDialog) {
    var selectedContractId by remember { mutableStateOf(contracts.firstOrNull { !it.isHistory }?.id ?: "") }
    var contractDropdownExpanded by remember { mutableStateOf(false) }

    val initialContract = contracts.find { it.id == selectedContractId }
    val initialUnit = initialContract?.let { c -> units.find { it.id == c.unitId } }

    var dueDateInput by remember { mutableStateOf(Formatters.getTodayIso()) }
    var rentInput by remember { mutableStateOf(initialContract?.monthlyRent?.toInt()?.toString() ?: initialUnit?.monthlyRent?.toInt()?.toString() ?: "") }
    var electricityInput by remember { mutableStateOf("") }
    var waterInput by remember { mutableStateOf("") }
    var stairsInput by remember { mutableStateOf("") }
    var arrearsInput by remember { mutableStateOf("") }
    var noteInput by remember { mutableStateOf("") }
    var isPaidNow by remember { mutableStateOf(false) }

    val totalCalculated = (rentInput.toDoubleOrNull() ?: 0.0) +
      (electricityInput.toDoubleOrNull() ?: 0.0) +
      (waterInput.toDoubleOrNull() ?: 0.0) +
      (stairsInput.toDoubleOrNull() ?: 0.0) +
      (arrearsInput.toDoubleOrNull() ?: 0.0)

    AlertDialog(
      onDismissRequest = { showAddPaymentDialog = false },
      title = {
        Text(text = if (isAr) "إصدار دفعة أو فاتورة جديدة" else "Create New Payment / Bill")
      },
      text = {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          // Lease / Unit Selector
          if (contracts.isNotEmpty()) {
            ExposedDropdownMenuBox(
              expanded = contractDropdownExpanded,
              onExpandedChange = { contractDropdownExpanded = !contractDropdownExpanded },
              modifier = Modifier.fillMaxWidth()
            ) {
              val currentContract = contracts.find { it.id == selectedContractId }
              val currentUnit = currentContract?.let { c -> units.find { it.id == c.unitId } }
              val currentProp = currentUnit?.let { u -> properties.find { it.id == u.propertyId } }
              val labelText = if (currentContract != null) {
                "${currentContract.tenantName} - ${if (isAr) "وحدة" else "Unit"} ${currentUnit?.unitNumber ?: ""} (${currentProp?.name ?: ""})"
              } else {
                if (isAr) "اختر المستأجر أو العقد" else "Select Tenant / Lease"
              }

              OutlinedTextField(
                value = labelText,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (isAr) "المستأجر / الوحدة" else "Tenant / Unit") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = contractDropdownExpanded) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().menuAnchor()
              )
              ExposedDropdownMenu(
                expanded = contractDropdownExpanded,
                onDismissRequest = { contractDropdownExpanded = false }
              ) {
                for (c in contracts.filter { !it.isHistory }) {
                  val u = units.find { it.id == c.unitId }
                  val p = u?.let { propMap[it.propertyId] }
                  DropdownMenuItem(
                    text = { Text("${c.tenantName} - ${if (isAr) "وحدة" else "Unit"} ${u?.unitNumber ?: ""} (${p?.name ?: ""})") },
                    onClick = {
                      selectedContractId = c.id
                      if (rentInput.isBlank() || rentInput == "0") {
                        rentInput = c.monthlyRent.toInt().toString()
                      }
                      contractDropdownExpanded = false
                    }
                  )
                }
              }
            }
          }

          // Due Date
          OutlinedTextField(
            value = dueDateInput,
            onValueChange = { dueDateInput = it },
            label = { Text(if (isAr) "تاريخ الاستحقاق (YYYY-MM-DD)" else "Due Date (YYYY-MM-DD)") },
            placeholder = { Text("2026-10-01") },
            modifier = Modifier.fillMaxWidth()
          )

          // Breakdown fields
          OutlinedTextField(
            value = rentInput,
            onValueChange = { rentInput = it },
            label = { Text(if (isAr) "قيمة الإيجار الشهري (ج.م)" else "Rent Amount (EGP)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = electricityInput,
              onValueChange = { electricityInput = it },
              label = { Text(if (isAr) "كهرباء (ج.م)" else "Electric") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = waterInput,
              onValueChange = { waterInput = it },
              label = { Text(if (isAr) "مياه (ج.م)" else "Water") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
              value = stairsInput,
              onValueChange = { stairsInput = it },
              label = { Text(if (isAr) "نظافة وصيانة (ج.م)" else "Cleaning") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = arrearsInput,
              onValueChange = { arrearsInput = it },
              label = { Text(if (isAr) "متأخرات (ج.م)" else "Arrears") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              modifier = Modifier.weight(1f)
            )
          }

          // Total calculated banner
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF0FDF4),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(12.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isAr) "إجمالي المبلغ المطلوب:" else "Total Amount Due:",
                style = MaterialTheme.typography.titleSmall,
                color = ForestGreen
              )
              Text(
                text = Formatters.formatCurrency(totalCalculated, language),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = ForestGreen
              )
            }
          }

          OutlinedTextField(
            value = noteInput,
            onValueChange = { noteInput = it },
            label = { Text(if (isAr) "بيان الفاتورة / ملاحظات" else "Bill Description / Note") },
            placeholder = { Text(if (isAr) "مثال: إيجار شهر أكتوبر مع قراءة العداد" else "e.g. October rent") },
            modifier = Modifier.fillMaxWidth()
          )

          // Paid Immediately Switch
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFFF8FAFC),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isAr) "تم تحصيل وسداد المبلغ الآن" else "Mark as paid immediately",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF1E293B)
              )
              Switch(
                checked = isPaidNow,
                onCheckedChange = { isPaidNow = it },
                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = ForestGreen)
              )
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val selectedContract = contracts.find { it.id == selectedContractId }
            val unitId = selectedContract?.unitId ?: units.firstOrNull()?.id ?: ""
            val finalRent = rentInput.toDoubleOrNull()
            val finalElec = electricityInput.toDoubleOrNull()
            val finalWater = waterInput.toDoubleOrNull()
            val finalStairs = stairsInput.toDoubleOrNull()
            val finalArrears = arrearsInput.toDoubleOrNull()

            val newPayment = Payment(
              id = "pay-${System.currentTimeMillis()}",
              contractId = selectedContract?.id ?: "",
              unitId = unitId,
              dueDate = dueDateInput.trim().ifBlank { Formatters.getTodayIso() },
              amount = if (totalCalculated > 0) totalCalculated else (finalRent ?: 0.0),
              status = if (isPaidNow) PaymentStatus.PAID else PaymentStatus.PENDING,
              paidAmount = if (isPaidNow) (if (totalCalculated > 0) totalCalculated else (finalRent ?: 0.0)) else null,
              paidDate = if (isPaidNow) Formatters.getTodayIso() else null,
              note = noteInput.trim().ifBlank { if (isPaidNow) "تم السداد بالكامل" else null },
              rentAmount = finalRent,
              electricityAmount = finalElec,
              waterAmount = finalWater,
              stairsCleaningAmount = finalStairs,
              previousArrearsAmount = finalArrears
            )
            onAddPayment?.invoke(newPayment)
            showAddPaymentDialog = false
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
          Text(text = if (isAr) "إصدار وحفظ" else "Save & Issue")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showAddPaymentDialog = false }) {
          Text(text = if (isAr) "إلغاء" else "Cancel")
        }
      }
    )
  }
}
