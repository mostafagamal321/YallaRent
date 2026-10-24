package com.example.ui.screens

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppSettings
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.ui.viewmodel.ScreenTab
import com.example.util.Formatters
import com.example.util.PdfContractGenerator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen(
  properties: List<Property>,
  units: List<UnitItem>,
  contracts: List<Contract>,
  payments: List<Payment>,
  settings: AppSettings,
  onNavigateTab: (ScreenTab) -> Unit
) {
  val context = LocalContext.current
  val isAr = settings.language == "ar"
  val backIcon = if (isAr) Icons.Default.ArrowForward else Icons.Default.ArrowBack

  var selectedMonth by remember { mutableStateOf(Formatters.getTodayIso().take(7)) }
  var monthDropdownExpanded by remember { mutableStateOf(false) }

  val monthsList = remember(payments) {
    (payments.map { it.dueDate.take(7) } + Formatters.getTodayIso().take(7)).distinct().sortedDescending()
  }

  val reportData = remember(properties, units, contracts, payments, selectedMonth) {
    properties.map { prop ->
      val propUnits = units.filter { it.propertyId == prop.id }
      val propUnitIds = propUnits.map { it.id }.toSet()
      val propPayments = payments.filter { propUnitIds.contains(it.unitId) && it.dueDate.startsWith(selectedMonth) }

      val expected = propPayments.sumOf { it.amount }
      val collected = propPayments.filter { it.status == PaymentStatus.PAID }.sumOf { it.paidAmount ?: it.amount }
      val overdue = propPayments.filter { it.status == PaymentStatus.OVERDUE }.sumOf { it.amount - (it.paidAmount ?: 0.0) }

      val activeLeases = contracts.count { !it.isHistory && it.status == ContractStatus.ACTIVE && propUnitIds.contains(it.unitId) }
      val rate = if (expected > 0) ((collected / expected) * 100).toInt() else 0

      PdfContractGenerator.ReportItem(
        propertyName = prop.name,
        totalUnits = propUnits.size,
        activeLeases = activeLeases,
        expected = expected,
        collected = collected,
        overdue = overdue,
        rate = rate
      )
    }
  }

  val grandExpected = reportData.sumOf { it.expected }
  val grandCollected = reportData.sumOf { it.collected }
  val grandOverdue = reportData.sumOf { it.overdue }
  val overallRate = if (grandExpected > 0) ((grandCollected / grandExpected) * 100).toInt() else 0

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          IconButton(onClick = { onNavigateTab(ScreenTab.DASHBOARD) }) {
            Icon(imageVector = backIcon, contentDescription = "Back", tint = ForestGreen)
          }
          Column {
            Text(
              text = if (isAr) "تقارير التحصيل الشهرية" else "Monthly Collection Reports",
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF1E293B)
            )
            Text(
              text = if (isAr) "ملخص الإيرادات والتحصيل لكل عقار" else "Monthly revenue breakdown per property",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B)
            )
          }
        }

        Button(
          onClick = {
            PdfContractGenerator.generateAndShareReportPdf(
              context = context,
              month = selectedMonth,
              landlordName = settings.landlord.name,
              reportData = reportData,
              grandExpected = grandExpected,
              grandCollected = grandCollected,
              grandOverdue = grandOverdue
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isAr) "مشاركة PDF" else "Share PDF", fontSize = 12.sp)
        }
      }
    }

    // Month Selector Dropdown
    item {
      ExposedDropdownMenuBox(
        expanded = monthDropdownExpanded,
        onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
        modifier = Modifier.fillMaxWidth()
      ) {
        OutlinedTextField(
          value = "${if (isAr) "الشهر المحدد:" else "Selected Month:"} $selectedMonth",
          onValueChange = {},
          readOnly = true,
          trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth().menuAnchor()
        )
        ExposedDropdownMenu(
          expanded = monthDropdownExpanded,
          onDismissRequest = { monthDropdownExpanded = false }
        ) {
          for (m in monthsList) {
            DropdownMenuItem(
              text = { Text(m) },
              onClick = { selectedMonth = m; monthDropdownExpanded = false }
            )
          }
        }
      }
    }

    // Overall Summary Card
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "${if (isAr) "إجمالي المحفظة العقارية لشهر" else "Overall Summary for"} $selectedMonth",
            style = MaterialTheme.typography.titleSmall,
            color = ForestGreen
          )
          Spacer(modifier = Modifier.height(12.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = if (isAr) "المطلوب" else "Expected", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              Text(text = Formatters.formatCurrency(grandExpected, settings.language), style = MaterialTheme.typography.titleSmall)
            }
            Column {
              Text(text = if (isAr) "المحصل" else "Collected", style = MaterialTheme.typography.labelSmall, color = GreenDark)
              Text(text = Formatters.formatCurrency(grandCollected, settings.language), style = MaterialTheme.typography.titleSmall, color = GreenDark)
            }
            Column {
              Text(text = if (isAr) "المتأخرات" else "Overdue", style = MaterialTheme.typography.labelSmall, color = RedAlert)
              Text(text = Formatters.formatCurrency(grandOverdue, settings.language), style = MaterialTheme.typography.titleSmall, color = RedAlert)
            }
            Column {
              Text(text = if (isAr) "النسبة" else "Rate", style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB))
              Text(text = "$overallRate%", style = MaterialTheme.typography.titleSmall, color = Color(0xFF2563EB))
            }
          }
        }
      }
    }

    // Properties Breakdown Cards
    items(reportData) { item ->
      Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(text = item.propertyName, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F172A))
              Text(
                text = "${item.activeLeases} ${if (isAr) "من" else "of"} ${item.totalUnits} ${if (isAr) "وحدات مؤجرة" else "units leased"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
              )
            }

            Surface(
              shape = RoundedCornerShape(6.dp),
              color = if (item.rate >= 90) GreenLight else if (item.rate >= 50) Color(0xFFFEF3C7) else RedLight
            ) {
              Text(
                text = "${item.rate}%",
                color = if (item.rate >= 90) GreenDark else if (item.rate >= 50) Color(0xFFB45309) else RedAlert,
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(8.dp))
              .background(Color(0xFFF8FAFC))
              .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = if (isAr) "المطلوب:" else "Due:", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
              Text(text = Formatters.formatCurrency(item.expected, settings.language), style = MaterialTheme.typography.labelSmall, color = Color(0xFF0F172A))
            }
            Column {
              Text(text = if (isAr) "المحصل:" else "Paid:", style = MaterialTheme.typography.labelSmall, color = GreenDark)
              Text(text = Formatters.formatCurrency(item.collected, settings.language), style = MaterialTheme.typography.labelSmall, color = GreenDark)
            }
            Column {
              Text(text = if (isAr) "المتبقي:" else "Left:", style = MaterialTheme.typography.labelSmall, color = RedAlert)
              Text(text = Formatters.formatCurrency(item.overdue, settings.language), style = MaterialTheme.typography.labelSmall, color = RedAlert)
            }
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
