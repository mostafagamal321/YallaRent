package com.example.ui.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.House
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.Contract
import com.example.data.model.LandlordDetails
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.MintLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.ui.theme.SoftBackground
import com.example.util.Formatters
import com.example.util.PdfContractGenerator

@Composable
fun PaymentDialog(
  payment: Payment,
  contract: Contract?,
  unit: UnitItem?,
  property: Property?,
  landlord: LandlordDetails,
  language: String,
  onDismiss: () -> Unit,
  onConfirmPaid: (
    paymentId: String,
    paidAmount: Double,
    paidDate: String,
    note: String?,
    rentAmount: Double,
    electricityAmount: Double,
    waterAmount: Double,
    stairsCleaningAmount: Double,
    previousArrearsAmount: Double
  ) -> Unit,
  onRevertToPending: (Payment) -> Unit
) {
  val context = LocalContext.current
  val isAr = language == "ar"

  // Base rent default
  val defaultRent = payment.rentAmount ?: contract?.monthlyRent ?: payment.amount
  var rentInput by remember { mutableStateOf(if (defaultRent > 0) defaultRent.toInt().toString() else "") }
  var electricityInput by remember { mutableStateOf(payment.electricityAmount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
  var waterInput by remember { mutableStateOf(payment.waterAmount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
  var stairsCleaningInput by remember { mutableStateOf(payment.stairsCleaningAmount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }
  var arrearsInput by remember { mutableStateOf(payment.previousArrearsAmount?.takeIf { it > 0 }?.toInt()?.toString() ?: "") }

  var paidDateInput by remember { mutableStateOf(payment.paidDate ?: Formatters.getTodayIso()) }
  var noteInput by remember { mutableStateOf(payment.note ?: (if (isAr) "تم استلام المبلغ نقداً بموجب إيصال" else "Paid in cash with receipt")) }

  // Computed total
  val rentVal by remember { derivedStateOf { rentInput.toDoubleOrNull() ?: 0.0 } }
  val elecVal by remember { derivedStateOf { electricityInput.toDoubleOrNull() ?: 0.0 } }
  val waterVal by remember { derivedStateOf { waterInput.toDoubleOrNull() ?: 0.0 } }
  val stairsVal by remember { derivedStateOf { stairsCleaningInput.toDoubleOrNull() ?: 0.0 } }
  val arrearsVal by remember { derivedStateOf { arrearsInput.toDoubleOrNull() ?: 0.0 } }

  val totalPaid by remember {
    derivedStateOf { rentVal + elecVal + waterVal + stairsVal + arrearsVal }
  }

  val wordsTotal = remember(totalPaid) {
    Formatters.formatCurrencyInWords(totalPaid, language)
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
    modifier = Modifier
      .fillMaxWidth(0.95f)
      .padding(vertical = 16.dp),
    title = {
      Column {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Column {
            Text(
              text = if (isAr) "تحصيل إيجار وتفصيل بنود السداد" else "Rent Collection & Breakdown",
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF0F172A),
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "${property?.name ?: ""} • ${if (isAr) "وحدة ${unit?.unitNumber}" else "Unit ${unit?.unitNumber}"}",
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B)
            )
          }

          Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (payment.status == PaymentStatus.PAID) GreenLight else RedLight,
            modifier = Modifier.padding(start = 4.dp)
          ) {
            Text(
              text = if (payment.status == PaymentStatus.PAID) (if (isAr) "مسدد ✓" else "PAID") else (if (isAr) "غير مسدد" else "PENDING"),
              color = if (payment.status == PaymentStatus.PAID) GreenDark else RedAlert,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Tenant Header Info
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = SoftBackground,
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = "${if (isAr) "المستأجر:" else "Tenant:"} ${contract?.tenantName ?: (if (isAr) "شاغرة" else "Vacant")}",
                style = MaterialTheme.typography.titleSmall,
                color = ForestGreen
              )
              Text(
                text = "${if (isAr) "استحقاق شهر:" else "Due:"} ${Formatters.formatUIDate(payment.dueDate)}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
              )
            }
          }
        }

        // Section Title
        Text(
          text = if (isAr) "تفصيل وتوزيع المبالغ المحصلة (ج.م):" else "Itemized Payment Breakdown (EGP):",
          style = MaterialTheme.typography.labelMedium,
          color = ForestGreen,
          fontWeight = FontWeight.Bold
        )

        // 1. Rent Amount
        OutlinedTextField(
          value = rentInput,
          onValueChange = { rentInput = it },
          label = { Text(if (isAr) "١. قيمة الإيجار الشهري" else "1. Monthly Rent") },
          leadingIcon = {
            Icon(Icons.Default.House, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
          },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // 2. Electricity Amount
        OutlinedTextField(
          value = electricityInput,
          onValueChange = { electricityInput = it },
          label = { Text(if (isAr) "٢. استهلاك الكهرباء" else "2. Electricity") },
          leadingIcon = {
            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
          },
          placeholder = { Text("0") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // 3. Water Amount
        OutlinedTextField(
          value = waterInput,
          onValueChange = { waterInput = it },
          label = { Text(if (isAr) "٣. استهلاك المياه" else "3. Water") },
          leadingIcon = {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
          },
          placeholder = { Text("0") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // 4. Stairs Cleaning Amount
        OutlinedTextField(
          value = stairsCleaningInput,
          onValueChange = { stairsCleaningInput = it },
          label = { Text(if (isAr) "٤. نظافة وصيانة السلم" else "4. Stairs Cleaning & Services") },
          leadingIcon = {
            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Color(0xFF7C3AED), modifier = Modifier.size(20.dp))
          },
          placeholder = { Text("0") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // 5. Previously Unpaid Amount
        OutlinedTextField(
          value = arrearsInput,
          onValueChange = { arrearsInput = it },
          label = { Text(if (isAr) "٥. متأخرات سابقة مسددة" else "5. Previously Unpaid / Arrears") },
          leadingIcon = {
            Icon(Icons.Default.History, contentDescription = null, tint = RedAlert, modifier = Modifier.size(20.dp))
          },
          placeholder = { Text("0") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Real-Time Total Card
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MintLight,
          border = androidx.compose.foundation.BorderStroke(1.5.dp, ForestGreen),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = if (isAr) "إجمالي المبلغ المحصل:" else "Total Paid:",
                style = MaterialTheme.typography.titleMedium,
                color = ForestGreen,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = Formatters.formatCurrency(totalPaid, language),
                style = MaterialTheme.typography.titleLarge,
                color = ForestGreen,
                fontWeight = FontWeight.Bold
              )
            }
            Text(
              text = wordsTotal,
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF14532D)
            )
          }
        }

        // Paid Date
        OutlinedTextField(
          value = paidDateInput,
          onValueChange = { paidDateInput = it },
          label = { Text(if (isAr) "تاريخ التحصيل الفعلي (YYYY-MM-DD)" else "Payment Date (YYYY-MM-DD)") },
          leadingIcon = {
            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )

        // Receipt Notes
        OutlinedTextField(
          value = noteInput,
          onValueChange = { noteInput = it },
          label = { Text(if (isAr) "ملاحظات الإيصال" else "Receipt Notes") },
          leadingIcon = {
            Icon(Icons.Default.Notes, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(20.dp))
          },
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(4.dp))

        // PDF Receipt Generation Button (Primary highlighted button)
        Button(
          onClick = {
            PdfContractGenerator.generateAndShareReceiptPdf(
              context = context,
              payment = payment,
              contract = contract,
              unit = unit,
              property = property,
              landlord = landlord,
              rentAmount = rentVal,
              electricityAmount = elecVal,
              waterAmount = waterVal,
              stairsCleaningAmount = stairsVal,
              previousArrearsAmount = arrearsVal,
              totalAmount = totalPaid,
              paidDate = paidDateInput,
              notes = noteInput.ifBlank { null },
              language = language
            )
          },
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
        ) {
          Icon(Icons.Default.Print, contentDescription = null, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (isAr) "إصدار وطباعة إيصال استلام (PDF)" else "Print & Share Receipt (PDF)",
            fontWeight = FontWeight.Bold
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onConfirmPaid(
            payment.id,
            totalPaid,
            paidDateInput,
            noteInput.ifBlank { null },
            rentVal,
            elecVal,
            waterVal,
            stairsVal,
            arrearsVal
          )
          onDismiss()
        },
        colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
        modifier = Modifier.height(42.dp)
      ) {
        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = if (isAr) "حفظ التحصيل" else "Save Payment")
      }
    },
    dismissButton = {
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        if (payment.status == PaymentStatus.PAID) {
          Button(
            onClick = {
              onRevertToPending(
                payment.copy(
                  status = PaymentStatus.PENDING,
                  paidDate = null,
                  paidAmount = null,
                  note = null,
                  rentAmount = null,
                  electricityAmount = null,
                  waterAmount = null,
                  stairsCleaningAmount = null,
                  previousArrearsAmount = null
                )
              )
              onDismiss()
            },
            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
            modifier = Modifier.height(42.dp)
          ) {
            Text(text = if (isAr) "إلغاء السداد" else "Revert")
          }
        }
        OutlinedButton(
          onClick = onDismiss,
          modifier = Modifier.height(42.dp)
        ) {
          Text(text = if (isAr) "إغلاق" else "Close")
        }
      }
    }
  )
}
