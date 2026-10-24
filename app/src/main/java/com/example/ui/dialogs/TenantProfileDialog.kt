package com.example.ui.dialogs

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.AppSettings
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.Tenant
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.util.Formatters
import com.example.util.ImageUtils
import com.example.util.PdfContractGenerator
import java.io.File

@Composable
fun TenantProfileDialog(
  tenant: Tenant,
  contracts: List<Contract>,
  units: List<UnitItem>,
  properties: List<Property>,
  payments: List<Payment>,
  settings: AppSettings,
  onDismiss: () -> Unit,
  onEditTenant: (Tenant) -> Unit,
  onDeleteTenant: (String) -> Unit,
  onOpenContract: (Contract) -> Unit
) {
  val context = LocalContext.current
  val isAr = settings.language == "ar"

  // Tenant's contracts (active + history)
  val tenantContracts = contracts.filter { it.tenantId == tenant.id || it.tenantName.trim() == tenant.fullName.trim() }
  val activeContract = tenantContracts.firstOrNull { !it.isHistory && it.status == ContractStatus.ACTIVE }
    ?: tenantContracts.firstOrNull { !it.isHistory }
    ?: tenantContracts.firstOrNull()

  val activeUnit = activeContract?.let { c -> units.find { it.id == c.unitId } }
  val activeProperty = activeUnit?.let { u -> properties.find { it.id == u.propertyId } }

  // Tenant's payments
  val contractIds = tenantContracts.map { it.id }.toSet()
  val tenantPayments = payments.filter { it.contractId in contractIds }.sortedByDescending { it.dueDate }

  val lastPaidPayment = tenantPayments.filter { it.status == PaymentStatus.PAID }.maxByOrNull { it.paidDate ?: "" }
  val nextPendingPayment = tenantPayments.filter { it.status != PaymentStatus.PAID }.minByOrNull { it.dueDate }
  val totalOverdue = tenantPayments.filter { it.status == PaymentStatus.OVERDUE }.sumOf { it.amount }

  // Privacy toggles & PIN challenge
  var isNationalIdRevealed by remember { mutableStateOf(false) }
  var isIdDocsExpanded by remember { mutableStateOf(false) }
  var isIdDocsUnlocked by remember { mutableStateOf(!settings.pinEnabled) }

  var showPinDialogFor by remember { mutableStateOf<String?>(null) } // "nationalId" or "idDocs"
  var enteredPin by remember { mutableStateOf("") }
  var pinError by remember { mutableStateOf(false) }

  var showDeleteConfirmation by remember { mutableStateOf(false) }

  // PIN challenge Dialog
  if (showPinDialogFor != null) {
    AlertDialog(
      onDismissRequest = {
        showPinDialogFor = null
        enteredPin = ""
        pinError = false
      },
      title = {
        Text(if (isAr) "أدخل رمز الأمان (PIN)" else "Security PIN Required")
      },
      text = {
        Column {
          Text(
            text = if (isAr) "هذا المحتوى محمي برمز القفل لحماية خصوصية بيانات المستأجر."
            else "This content is protected by PIN lock to protect tenant privacy.",
            fontSize = 13.sp,
            color = Color(0xFF64748B)
          )
          Spacer(modifier = Modifier.height(10.dp))
          OutlinedTextField(
            value = enteredPin,
            onValueChange = {
              enteredPin = it
              pinError = false
            },
            label = { Text(if (isAr) "رمز PIN" else "PIN Code") },
            isError = pinError,
            singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("tenant_privacy_pin_input")
          )
          if (pinError) {
            Text(
              text = if (isAr) "رمز PIN غير صحيح" else "Incorrect PIN code",
              color = Color.Red,
              fontSize = 12.sp,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (enteredPin == settings.pinCode || !settings.pinEnabled) {
              if (showPinDialogFor == "nationalId") {
                isNationalIdRevealed = true
              } else if (showPinDialogFor == "idDocs") {
                isIdDocsUnlocked = true
                isIdDocsExpanded = true
              }
              showPinDialogFor = null
              enteredPin = ""
              pinError = false
            } else {
              pinError = true
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          modifier = Modifier.testTag("confirm_privacy_pin_button")
        ) {
          Text(if (isAr) "تأكيد" else "Confirm")
        }
      },
      dismissButton = {
        TextButton(onClick = {
          showPinDialogFor = null
          enteredPin = ""
          pinError = false
        }) {
          Text(if (isAr) "إلغاء" else "Cancel")
        }
      }
    )
  }

  // Delete Confirmation Dialog
  if (showDeleteConfirmation) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirmation = false },
      title = { Text(if (isAr) "حذف المستأجر" else "Delete Tenant") },
      text = {
        Text(
          if (isAr) "هل أنت متأكد من حذف المستأجر \"${tenant.fullName}\"؟ سيتم فك ارتباط العقود به ولن يتم حذف سجل العقود التاريخي."
          else "Are you sure you want to delete tenant \"${tenant.fullName}\"? Active and historical contracts will remain intact."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirmation = false
            onDeleteTenant(tenant.id)
            onDismiss()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
          modifier = Modifier.testTag("confirm_delete_tenant_button")
        ) {
          Text(if (isAr) "نعم، حذف" else "Yes, Delete")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirmation = false }) {
          Text(if (isAr) "إلغاء" else "Cancel")
        }
      }
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color(0xFFF8FAFC),
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Top Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(ForestGreen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isAr) "ملف المستأجر" else "Tenant Profile",
              color = Color.White,
              style = MaterialTheme.typography.titleMedium
            )
          }

          IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp).testTag("close_tenant_profile_button")) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // --- 1. Top Card: Main Profile Details ---
          item {
            Card(
              shape = RoundedCornerShape(14.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(2.dp),
              modifier = Modifier.fillMaxWidth().testTag("tenant_profile_card")
            ) {
              Column(modifier = Modifier.padding(16.dp)) {
                // Photo + Name + Job
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Box(
                    modifier = Modifier
                      .size(86.dp)
                      .clip(CircleShape)
                      .background(Color(0xFFE2E8F0))
                      .border(2.5.dp, ForestGreen, CircleShape),
                    contentAlignment = Alignment.Center
                  ) {
                    if (tenant.photoPath != null && File(tenant.photoPath).exists()) {
                      AsyncImage(
                        model = ImageRequest.Builder(context)
                          .data(File(tenant.photoPath))
                          .crossfade(true)
                          .build(),
                        contentDescription = tenant.fullName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                      )
                    } else {
                      Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(48.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.width(14.dp))

                  Column(modifier = Modifier.weight(1f)) {
                    Text(
                      text = tenant.fullName,
                      style = MaterialTheme.typography.titleMedium,
                      fontWeight = FontWeight.Bold,
                      color = Color(0xFF0F172A)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                      text = "${tenant.job} • ${tenant.nationality}",
                      style = MaterialTheme.typography.bodyMedium,
                      color = Color(0xFF475569)
                    )
                    if (tenant.address.isNotBlank()) {
                      Text(
                        text = tenant.address,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF64748B)
                      )
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // National ID with Privacy Eye Button
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column {
                    Text(
                      text = if (isAr) "الرقم القومي" else "National ID",
                      fontSize = 11.sp,
                      color = Color(0xFF64748B)
                    )
                    val displayId = if (isNationalIdRevealed) {
                      if (tenant.nationalId.isNotBlank()) {
                        if (isAr) Formatters.toArabicIndicDigits(tenant.nationalId) else tenant.nationalId
                      } else {
                        if (isAr) "غير مسجل" else "Not provided"
                      }
                    } else {
                      ImageUtils.maskNationalId(tenant.nationalId, isAr)
                    }
                    Text(
                      text = displayId,
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.SemiBold,
                      color = Color(0xFF1E293B)
                    )
                  }

                  IconButton(
                    onClick = {
                      if (isNationalIdRevealed) {
                        isNationalIdRevealed = false
                      } else {
                        if (settings.pinEnabled) {
                          showPinDialogFor = "nationalId"
                        } else {
                          isNationalIdRevealed = true
                        }
                      }
                    },
                    modifier = Modifier.size(34.dp).testTag("reveal_national_id_button")
                  ) {
                    Icon(
                      imageVector = if (isNationalIdRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                      contentDescription = "Toggle National ID",
                      tint = ForestGreen,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Phone Actions (Direct Call & WhatsApp)
                if (tenant.phone.isNotBlank()) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    // Call Button
                    Button(
                      onClick = {
                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${tenant.phone}"))
                        context.startActivity(intent)
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f).testTag("call_tenant_button")
                    ) {
                      Icon(imageVector = Icons.Default.Call, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(text = if (isAr) "اتصال" else "Call", fontSize = 12.sp)
                    }

                    // WhatsApp Button
                    Button(
                      onClick = {
                        val formatted = ImageUtils.formatWhatsAppNumber(tenant.phone)
                        val url = "https://api.whatsapp.com/send?phone=$formatted"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        context.startActivity(intent)
                      },
                      colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                      shape = RoundedCornerShape(8.dp),
                      modifier = Modifier.weight(1f).testTag("whatsapp_tenant_button")
                    ) {
                      Icon(imageVector = Icons.Default.Message, contentDescription = null, modifier = Modifier.size(16.dp))
                      Spacer(modifier = Modifier.width(6.dp))
                      Text(text = if (isAr) "واتساب" else "WhatsApp", fontSize = 12.sp)
                    }
                  }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(12.dp))

                // Building & Unit
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(text = if (isAr) "العمارة / المبنى" else "Building / Property", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                      text = activeProperty?.name ?: if (isAr) "بدون عقار محدد" else "None",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      color = Color(0xFF0F172A)
                    )
                  }
                  Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (isAr) "رقم الوحدة" else "Unit Number", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                      text = activeUnit?.let { if (isAr) "وحدة ${it.unitNumber}" else "Unit ${it.unitNumber}" } ?: if (isAr) "غير محدد" else "None",
                      style = MaterialTheme.typography.bodyMedium,
                      fontWeight = FontWeight.Medium,
                      color = Color(0xFF0F172A)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Current Contract Summary
                if (activeContract != null) {
                  val daysLeft = Formatters.getDaysFromToday(activeContract.endDate)
                  val remainingText = Formatters.getRemainingDaysText(daysLeft, language)

                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Column {
                      Text(text = if (isAr) "العقد الحالي" else "Current Contract", fontSize = 11.sp, color = Color(0xFF64748B))
                      Text(
                        text = "${Formatters.formatUIDate(activeContract.startDate)} ← ${Formatters.formatUIDate(activeContract.endDate)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                      )
                    }
                    Surface(
                      shape = RoundedCornerShape(4.dp),
                      color = if (daysLeft < 0) RedLight else if (daysLeft <= 60) Color(0xFFFFEDD5) else GreenLight
                    ) {
                      Text(
                        text = remainingText,
                        color = if (daysLeft < 0) RedAlert else if (daysLeft <= 60) Color(0xFFC2410C) else GreenDark,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                      )
                    }
                  }

                  Spacer(modifier = Modifier.height(10.dp))

                  // Monthly Rent & Due Day
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                  ) {
                    Column {
                      Text(text = if (isAr) "قيمة الإيجار الشهري" else "Monthly Rent", fontSize = 11.sp, color = Color(0xFF64748B))
                      Text(
                        text = Formatters.formatCurrency(activeContract.monthlyRent, language),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = ForestGreen
                      )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                      Text(text = if (isAr) "يوم الاستحقاق" else "Due Day", fontSize = 11.sp, color = Color(0xFF64748B))
                      Text(
                        text = if (isAr) "يوم ${Formatters.toArabicIndicDigits(activeContract.rentDueDay)} من كل شهر"
                        else "Day ${activeContract.rentDueDay} of month",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF334155)
                      )
                    }
                  }
                } else {
                  Text(
                    text = if (isAr) "لا يوجد عقد نشط حالياً لهذا المستأجر" else "No active contract for this tenant",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFFF1F5F9))
                Spacer(modifier = Modifier.height(10.dp))

                // Payment Status (Last Paid, Next Due, Total Overdue)
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Column {
                    Text(text = if (isAr) "آخر سداد" else "Last Payment", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                      text = lastPaidPayment?.paidDate?.let { Formatters.formatUIDate(it) } ?: (if (isAr) "لا يوجد" else "None"),
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF334155)
                    )
                  }

                  Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = if (isAr) "القسط القادم" else "Next Due", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                      text = nextPendingPayment?.dueDate?.let { Formatters.formatUIDate(it) } ?: (if (isAr) "مسدد بالكامل" else "All Paid"),
                      style = MaterialTheme.typography.bodySmall,
                      color = Color(0xFF334155)
                    )
                  }

                  Column(horizontalAlignment = Alignment.End) {
                    Text(text = if (isAr) "إجمالي المتأخرات" else "Overdue Total", fontSize = 11.sp, color = Color(0xFF64748B))
                    Text(
                      text = Formatters.formatCurrency(totalOverdue, language),
                      style = MaterialTheme.typography.titleSmall,
                      fontWeight = FontWeight.Bold,
                      color = if (totalOverdue > 0) RedAlert else GreenDark
                    )
                  }
                }
              }
            }
          }

          // --- 2. ID Documents Section (Hidden until tapped & PIN checked) ---
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(1.dp),
              modifier = Modifier.fillMaxWidth().testTag("id_documents_card")
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                      if (!isIdDocsExpanded) {
                        if (settings.pinEnabled && !isIdDocsUnlocked) {
                          showPinDialogFor = "idDocs"
                        } else {
                          isIdDocsExpanded = true
                        }
                      } else {
                        isIdDocsExpanded = false
                      }
                    },
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                      text = if (isAr) "صور البطاقة الشخصية" else "National ID Documents",
                      style = MaterialTheme.typography.titleSmall,
                      color = Color(0xFF0F172A)
                    )
                    if (settings.pinEnabled && !isIdDocsUnlocked) {
                      Spacer(modifier = Modifier.width(6.dp))
                      Icon(imageVector = Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFF59E0B), modifier = Modifier.size(16.dp))
                    }
                  }

                  Icon(
                    imageVector = if (isIdDocsExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                  )
                }

                AnimatedVisibility(
                  visible = isIdDocsExpanded,
                  enter = expandVertically(),
                  exit = shrinkVertically()
                ) {
                  Column(modifier = Modifier.padding(top = 12.dp)) {
                    val hasFront = tenant.idCardPhotoFrontPath != null && File(tenant.idCardPhotoFrontPath).exists()
                    val hasBack = tenant.idCardPhotoBackPath != null && File(tenant.idCardPhotoBackPath).exists()

                    if (!hasFront && !hasBack) {
                      Text(
                        text = if (isAr) "لم يتم إرفاق صور لبطاقة هذا المستأجر بعد."
                        else "No ID card photos have been attached yet.",
                        fontSize = 12.sp,
                        color = Color.Gray
                      )
                    } else {
                      Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                      ) {
                        if (hasFront) {
                          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isAr) "الوجه" else "Front", fontSize = 11.sp, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(
                              model = ImageRequest.Builder(context)
                                .data(File(tenant.idCardPhotoFrontPath!!))
                                .crossfade(true)
                                .build(),
                              contentDescription = "ID Front",
                              contentScale = ContentScale.Crop,
                              modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            )
                          }
                        }
                        if (hasBack) {
                          Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = if (isAr) "الظهر" else "Back", fontSize = 11.sp, color = Color(0xFF475569))
                            Spacer(modifier = Modifier.height(4.dp))
                            AsyncImage(
                              model = ImageRequest.Builder(context)
                                .data(File(tenant.idCardPhotoBackPath!!))
                                .crossfade(true)
                                .build(),
                              contentDescription = "ID Back",
                              contentScale = ContentScale.Crop,
                              modifier = Modifier
                                .fillMaxWidth()
                                .height(100.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // --- 3. Contract History Section ---
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(1.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isAr) "سجل العقود (${tenantContracts.size})" else "Contract History (${tenantContracts.size})",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF0F172A)
                  )
                }

                Spacer(modifier = Modifier.height(10.dp))

                if (tenantContracts.isEmpty()) {
                  Text(
                    text = if (isAr) "لا توجد عقود مسجلة لهذا المستأجر" else "No contracts found",
                    fontSize = 12.sp,
                    color = Color.Gray
                  )
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (cnt in tenantContracts) {
                      val unit = units.find { it.id == cnt.unitId }
                      val prop = unit?.let { u -> properties.find { it.id == u.propertyId } }

                      Surface(
                        onClick = { onOpenContract(cnt) },
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFFF8FAFC),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                        modifier = Modifier.fillMaxWidth().testTag("contract_history_item_${cnt.id}")
                      ) {
                        Row(
                          modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text(
                              text = "${prop?.name ?: ""} • ${unit?.let { if (isAr) "وحدة ${it.unitNumber}" else "Unit ${it.unitNumber}" } ?: ""}",
                              style = MaterialTheme.typography.labelMedium,
                              fontWeight = FontWeight.SemiBold,
                              color = Color(0xFF0F172A)
                            )
                            Text(
                              text = "${Formatters.formatUIDate(cnt.startDate)} ← ${Formatters.formatUIDate(cnt.endDate)}",
                              style = MaterialTheme.typography.bodySmall,
                              color = Color(0xFF64748B)
                            )
                          }

                          Column(horizontalAlignment = Alignment.End) {
                            Text(
                              text = Formatters.formatCurrency(cnt.monthlyRent, language),
                              style = MaterialTheme.typography.labelSmall,
                              fontWeight = FontWeight.Bold,
                              color = ForestGreen
                            )
                            Text(
                              text = if (cnt.isHistory) (if (isAr) "منتهي" else "Expired") else (if (isAr) "ساري" else "Active"),
                              fontSize = 10.sp,
                              color = if (cnt.isHistory) Color.Gray else GreenDark
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }

          // --- 4. Payment History Section ---
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color.White),
              elevation = CardDefaults.cardElevation(1.dp),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Text(
                  text = if (isAr) "سجل الدفع (${tenantPayments.size})" else "Payment History (${tenantPayments.size})",
                  style = MaterialTheme.typography.titleSmall,
                  color = Color(0xFF0F172A)
                )

                Spacer(modifier = Modifier.height(10.dp))

                if (tenantPayments.isEmpty()) {
                  Text(
                    text = if (isAr) "لا توجد دفعات مسجلة" else "No payment records",
                    fontSize = 12.sp,
                    color = Color.Gray
                  )
                } else {
                  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (pay in tenantPayments.take(8)) {
                      val isPaid = pay.status == PaymentStatus.PAID
                      val isOverdue = pay.status == PaymentStatus.OVERDUE

                      Row(
                        modifier = Modifier
                          .fillMaxWidth()
                          .clip(RoundedCornerShape(6.dp))
                          .background(Color(0xFFF8FAFC))
                          .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                      ) {
                        Column {
                          Text(
                            text = "${if (isAr) "استحقاق:" else "Due:"} ${Formatters.formatUIDate(pay.dueDate)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF1E293B)
                          )
                          if (isPaid && pay.paidDate != null) {
                            Text(
                              text = "${if (isAr) "سُدد بتاريخ:" else "Paid:"} ${Formatters.formatUIDate(pay.paidDate)}",
                              fontSize = 10.sp,
                              color = GreenDark
                            )
                          }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = Formatters.formatCurrency(pay.amount, language),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (isPaid) GreenDark else if (isOverdue) RedAlert else Color(0xFF0F172A)
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = if (isPaid) GreenLight else if (isOverdue) RedLight else Color(0xFFFEF3C7)
                          ) {
                            Text(
                              text = if (isPaid) (if (isAr) "مسدد" else "Paid") else if (isOverdue) (if (isAr) "متأخر" else "Overdue") else (if (isAr) "معلق" else "Pending"),
                              color = if (isPaid) GreenDark else if (isOverdue) RedAlert else Color(0xFFB45309),
                              fontSize = 10.sp,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
          }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        // --- 5. Bottom Action Buttons ---
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            // Edit Profile Button
            Button(
              onClick = { onEditTenant(tenant) },
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f).testTag("edit_tenant_profile_button")
            ) {
              Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(if (isAr) "تعديل البيانات" else "Edit Profile", fontSize = 12.sp)
            }

            // Open Contract Button
            if (activeContract != null) {
              OutlinedButton(
                onClick = { onOpenContract(activeContract) },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("open_contract_button")
              ) {
                Icon(imageVector = Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAr) "فتح العقد" else "Open Contract", fontSize = 12.sp)
              }
            }

            // Print Contract PDF Button
            if (activeContract != null && activeUnit != null && activeProperty != null) {
              OutlinedButton(
                onClick = {
                  val template = if (activeProperty.type == com.example.data.model.PropertyType.SHOPS) {
                    settings.templates.shops
                  } else {
                    settings.templates.apartments
                  }
                  PdfContractGenerator.generateAndShareContractPdf(
                    context = context,
                    contract = activeContract,
                    unit = activeUnit,
                    property = activeProperty,
                    landlord = settings.landlord,
                    templateText = template,
                    tenant = tenant
                  )
                },
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f).testTag("print_contract_pdf_button")
              ) {
                Icon(imageVector = Icons.Default.Print, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isAr) "طباعة العقد" else "Print PDF", fontSize = 12.sp)
              }
            }
          }

          // Delete Tenant Button
          OutlinedButton(
            onClick = { showDeleteConfirmation = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = RedAlert),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth().testTag("delete_tenant_button")
          ) {
            Icon(imageVector = Icons.Default.Delete, contentDescription = null, tint = RedAlert, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isAr) "حذف المستأجر" else "Delete Tenant", fontSize = 12.sp, color = RedAlert)
          }
        }
      }
    }
  }
}
