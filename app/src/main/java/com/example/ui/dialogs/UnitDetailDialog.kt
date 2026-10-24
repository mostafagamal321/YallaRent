package com.example.ui.dialogs

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppSettings
import com.example.data.model.Contract
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
import com.example.util.Formatters
import com.example.util.PdfContractGenerator

@Composable
fun UnitDetailDialog(
  unitId: String,
  units: List<UnitItem>,
  properties: List<Property>,
  contracts: List<Contract>,
  payments: List<Payment>,
  settings: AppSettings,
  onDismiss: () -> Unit,
  onEditContract: (Contract) -> Unit,
  onRenewContract: (oldContractId: String, newStartDate: String, newEndDate: String, newRent: Double?) -> Unit,
  onEndContract: (String) -> Unit,
  onOpenPaymentDialog: (String) -> Unit
) {
  val context = LocalContext.current
  val isAr = settings.language == "ar"

  val unit = units.find { it.id == unitId } ?: return
  val property = properties.find { it.id == unit.propertyId } ?: return
  val activeContract = contracts.find { !it.isHistory && it.unitId == unitId }
  val historyContracts = contracts.filter { it.isHistory && it.unitId == unitId }
  val unitPayments = payments.filter { it.unitId == unitId }.sortedByDescending { it.dueDate }

  // Renewal Dialog state
  var showRenewModal by remember { mutableStateOf(false) }
  var renewStartDate by remember { mutableStateOf("") }
  var renewEndDate by remember { mutableStateOf("") }
  var renewRent by remember { mutableStateOf(activeContract?.monthlyRent?.toInt()?.toString() ?: "") }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Bar
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = "${if (isAr) "وحدة رقم" else "Unit #"} ${unit.unitNumber}",
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF0F172A)
            )
            Text(
              text = "${property.name} • ${unit.description}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B)
            )
          }

          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
          }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(horizontal = 16.dp, vertical = 14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // Active Contract Card
          item {
            if (activeContract != null) {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Surface(shape = RoundedCornerShape(8.dp), color = GreenLight) {
                      Text(
                        text = if (isAr) "العقد الساري حالياً" else "Active Lease",
                        color = GreenDark,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                      )
                    }
                    Text(
                      text = "${if (isAr) "تحرير:" else "Date:"} ${Formatters.formatUIDate(activeContract.contractDate.ifBlank { activeContract.startDate })}",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color.Gray
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))
                  Text(text = activeContract.tenantName, style = MaterialTheme.typography.titleMedium, color = ForestGreen)
                  Spacer(modifier = Modifier.height(6.dp))

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(
                      text = "${if (isAr) "الإيجار الشهري:" else "Monthly Rent:"} ${Formatters.formatCurrency(activeContract.monthlyRent, settings.language)}",
                      style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                      text = "${if (isAr) "الاستحقاق: يوم" else "Due Day:"} ${activeContract.rentDueDay}",
                      style = MaterialTheme.typography.bodyMedium
                    )
                  }

                  Spacer(modifier = Modifier.height(4.dp))
                  Text(
                    text = "${if (isAr) "مدة العقد:" else "Duration:"} ${Formatters.calculateDurationText(activeContract.startDate, activeContract.endDate, settings.language)} (${Formatters.formatUIDate(activeContract.startDate)} - ${Formatters.formatUIDate(activeContract.endDate)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0284C7)
                  )

                  if (!activeContract.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                      text = "${if (isAr) "ملاحظة:" else "Note:"} ${activeContract.notes}",
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFFD97706)
                    )
                  }

                  Spacer(modifier = Modifier.height(14.dp))

                  // Actions Grid
                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                      onClick = {
                        val template = if (property.type == PropertyType.SHOPS) settings.templates.shops else settings.templates.apartments
                        PdfContractGenerator.generateAndShareContractPdf(
                          context = context,
                          contract = activeContract,
                          unit = unit,
                          property = property,
                          landlord = settings.landlord,
                          templateText = template
                        )
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(text = if (isAr) "مشاركة PDF" else "Share PDF", fontSize = 11.sp)
                    }

                    Button(
                      onClick = {
                        val oldEnd = Formatters.parseIsoDate(activeContract.endDate)
                        val nextStartCal = java.util.Calendar.getInstance().apply {
                          time = oldEnd
                          add(java.util.Calendar.DAY_OF_MONTH, 1)
                        }
                        val nextEndCal = java.util.Calendar.getInstance().apply {
                          time = nextStartCal.time
                          add(java.util.Calendar.YEAR, 1)
                          add(java.util.Calendar.DAY_OF_MONTH, -1)
                        }
                        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US)
                        renewStartDate = sdf.format(nextStartCal.time)
                        renewEndDate = sdf.format(nextEndCal.time)
                        renewRent = activeContract.monthlyRent.toInt().toString()
                        showRenewModal = true
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(text = if (isAr) "تجديد العقد" else "Renew", fontSize = 11.sp)
                    }
                  }

                  Spacer(modifier = Modifier.height(6.dp))

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                      onClick = {
                        onDismiss()
                        onEditContract(activeContract)
                      },
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(text = if (isAr) "تعديل البيانات" else "Edit", fontSize = 11.sp)
                    }

                    Button(
                      onClick = {
                        onEndContract(activeContract.id)
                        onDismiss()
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f)
                    ) {
                      Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                      Spacer(modifier = Modifier.width(4.dp))
                      Text(text = if (isAr) "إنهاء العقد" else "End Lease", fontSize = 11.sp)
                    }
                  }
                }
              }
            } else {
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(Color(0xFFF8FAFC))
                  .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                  .padding(20.dp),
                contentAlignment = Alignment.Center
              ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                  Text(text = if (isAr) "الوحدة شاغرة حالياً" else "Unit is currently vacant", style = MaterialTheme.typography.titleSmall, color = Color.Gray)
                  if (!unit.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = unit.notes, style = MaterialTheme.typography.bodySmall, color = Color(0xFF0284C7))
                  }
                }
              }
            }
          }

          // Payment History for this Unit
          item {
            Text(
              text = if (isAr) "سجل دفعات الوحدة (${unitPayments.size})" else "Payment History (${unitPayments.size})",
              style = MaterialTheme.typography.titleSmall,
              color = Color(0xFF1E293B)
            )
          }

          if (unitPayments.isEmpty()) {
            item {
              Text(
                text = if (isAr) "لا توجد دفعات مسجلة بعد." else "No payments recorded yet.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
          } else {
            items(unitPayments) { p ->
              val isPaid = p.status == PaymentStatus.PAID
              val isOverdue = p.status == PaymentStatus.OVERDUE

              Card(
                onClick = { onOpenPaymentDialog(p.id) },
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                      Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isPaid) GreenLight else if (isOverdue) RedLight else Color(0xFFFEF3C7)
                      ) {
                        Text(
                          text = if (isPaid) (if (isAr) "مسدد" else "Paid") else if (isOverdue) (if (isAr) "متأخر" else "Overdue") else (if (isAr) "مستحق" else "Pending"),
                          color = if (isPaid) GreenDark else if (isOverdue) RedAlert else Color(0xFFB45309),
                          style = MaterialTheme.typography.labelSmall,
                          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                      }
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(text = Formatters.formatUIDate(p.dueDate), style = MaterialTheme.typography.bodyMedium, color = Color(0xFF0F172A))
                    }
                    if (isPaid && p.paidDate != null) {
                      Text(
                        text = "${if (isAr) "سدد في:" else "Paid on:"} ${Formatters.formatUIDate(p.paidDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = GreenDark
                      )
                    }
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(text = Formatters.formatCurrency(p.amount, settings.language), style = MaterialTheme.typography.titleSmall, color = ForestGreen)
                    if (!isPaid) {
                      Text(text = if (isAr) "سداد" else "Pay", color = Color(0xFF2563EB), style = MaterialTheme.typography.labelSmall)
                    }
                  }
                }
              }
            }
          }

          // Historical Contracts
          if (historyContracts.isNotEmpty()) {
            item {
              Text(
                text = if (isAr) "أرشيف العقود السابقة (${historyContracts.size})" else "Previous Contracts (${historyContracts.size})",
                style = MaterialTheme.typography.titleSmall,
                color = Color.Gray
              )
            }

            items(historyContracts) { hc ->
              Box(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(Color(0xFFF8FAFC))
                  .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                  .padding(10.dp)
              ) {
                Column {
                  Text(text = hc.tenantName, style = MaterialTheme.typography.labelMedium)
                  Text(
                    text = "${Formatters.formatUIDate(hc.startDate)} - ${Formatters.formatUIDate(hc.endDate)} • ${Formatters.formatCurrency(hc.monthlyRent, settings.language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Renewal Dialog
  if (showRenewModal && activeContract != null) {
    AlertDialog(
      onDismissRequest = { showRenewModal = false },
      title = { Text(if (isAr) "تجديد عقد الإيجار" else "Renew Lease Contract") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = if (isAr) "سيتم أرشفة العقد الحالي وإنشاء فترة تعاقد جديدة للمستأجر: ${activeContract.tenantName}" else "Previous contract will be archived and renewed for: ${activeContract.tenantName}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
          )
          OutlinedTextField(
            value = renewStartDate,
            onValueChange = { renewStartDate = it },
            label = { Text(if (isAr) "تاريخ البداية (YYYY-MM-DD)" else "Start Date") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = renewEndDate,
            onValueChange = { renewEndDate = it },
            label = { Text(if (isAr) "تاريخ النهاية (YYYY-MM-DD)" else "End Date") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = renewRent,
            onValueChange = { renewRent = it },
            label = { Text(if (isAr) "الإيجار الشهري الجديد (ج.م)" else "New Rent") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (renewStartDate.isNotBlank() && renewEndDate.isNotBlank()) {
              val rAmt = renewRent.toDoubleOrNull() ?: activeContract.monthlyRent
              onRenewContract(activeContract.id, renewStartDate, renewEndDate, rAmt)
              showRenewModal = false
              onDismiss()
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
          Text(text = if (isAr) "تأكيد التجديد" else "Confirm Renewal")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showRenewModal = false }) {
          Text(text = if (isAr) "إلغاء" else "Cancel")
        }
      }
    )
  }
}
