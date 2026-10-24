package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.InAppAlert
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.ui.viewmodel.ScreenTab

@Composable
fun NotificationsScreen(
  alerts: List<InAppAlert>,
  language: String,
  onNavigateTab: (ScreenTab) -> Unit,
  onOpenUnitDetail: (unitId: String) -> Unit
) {
  val isAr = language == "ar"
  val chevronIcon = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight
  var filterType by remember { mutableStateOf("all") }

  val filteredAlerts = alerts.filter {
    if (filterType == "all") true else it.type == filterType
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      Column {
        Text(
          text = if (isAr) "مركز التنبيهات والإشعارات" else "Alerts & Notifications Center",
          style = MaterialTheme.typography.titleMedium,
          color = Color(0xFF1E293B)
        )
        Text(
          text = if (isAr) "تنبيهات فورية بمواعيد الإيجار وقرب انتهاء العقود" else "Real-time alerts for rent dues and expiring contracts",
          style = MaterialTheme.typography.bodySmall,
          color = Color(0xFF64748B)
        )
      }
    }

    // Filter Chips
    item {
      val chipOptions = listOf(
        Pair("all", if (isAr) "الكل" else "All"),
        Pair("overdue", if (isAr) "متأخرات" else "Overdue"),
        Pair("rent_due", if (isAr) "استحقاق إيجار" else "Rent Due"),
        Pair("contract_expiring", if (isAr) "انتهاء عقود" else "Expiring Leases"),
        Pair("backup", if (isAr) "نسخ احتياطي" else "Backup")
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for ((key, label) in chipOptions) {
          val isSelected = filterType == key
          Surface(
            onClick = { filterType = key },
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

    // Alerts List
    if (filteredAlerts.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(44.dp))
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = if (isAr) "لا توجد تنبيهات جديدة" else "No new notifications", style = MaterialTheme.typography.titleSmall)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = if (isAr) "جميع المستحقات والعقود تحت المتابعة المنتظمة." else "All records and leases are currently up to date.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
          }
        }
      }
    } else {
      items(filteredAlerts) { alert ->
        val (iconVector, iconTint, borderClr) = when (alert.type) {
          "overdue" -> Triple(Icons.Default.Warning, RedAlert, Color(0xFFFCA5A5))
          "rent_due" -> Triple(Icons.Default.HourglassTop, Color(0xFFD97706), Color(0xFFFDE68A))
          "contract_expiring" -> Triple(Icons.Default.CalendarToday, Color(0xFF0284C7), Color(0xFFBAE6FD))
          else -> Triple(Icons.Default.Save, Color(0xFF16A34A), Color(0xFFBBF7D0))
        }

        Card(
          onClick = {
            if (alert.type == "backup") {
              onNavigateTab(ScreenTab.SETTINGS)
            } else if (alert.relatedUnitId != null) {
              onOpenUnitDetail(alert.relatedUnitId)
            }
          },
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(1.dp, borderClr),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Box(
              modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFFF8FAFC)),
              contentAlignment = Alignment.Center
            ) {
              Icon(imageVector = iconVector, contentDescription = null, tint = iconTint, modifier = Modifier.size(20.dp))
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
              Text(text = alert.title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F172A))
              Spacer(modifier = Modifier.height(2.dp))
              Text(text = alert.message, style = MaterialTheme.typography.bodySmall, color = Color(0xFF475569))
            }

            Icon(imageVector = chevronIcon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(40.dp))
    }
  }
}
