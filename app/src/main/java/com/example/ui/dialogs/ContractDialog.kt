package com.example.ui.dialogs

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
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
import com.example.data.model.ContractStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.MintLight
import androidx.compose.ui.text.font.FontWeight
import com.example.util.Formatters
import com.example.util.PdfContractGenerator

import com.example.data.model.Tenant
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Person

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractDialog(
  initialUnitId: String?,
  existingContract: Contract?,
  units: List<UnitItem>,
  properties: List<Property>,
  settings: AppSettings,
  tenants: List<Tenant> = emptyList(),
  onSaveNewTenant: ((Tenant) -> Unit)? = null,
  onDismiss: () -> Unit,
  onSaveContract: (Contract, Boolean) -> Unit
) {
  val context = LocalContext.current
  val isAr = settings.language == "ar"
  val backIcon = if (isAr) Icons.Default.ArrowForward else Icons.Default.ArrowBack

  var step by remember { mutableStateOf(1) } // 1: Form, 2: Review & Print Step

  var selectedUnitId by remember {
    mutableStateOf(initialUnitId ?: existingContract?.unitId ?: units.firstOrNull()?.id ?: "")
  }

  val currentUnit = units.find { it.id == selectedUnitId }
  val currentProperty = currentUnit?.let { u -> properties.find { it.id == u.propertyId } }
  val isShop = currentProperty?.type == PropertyType.SHOPS

  // Tenant Linking State
  var selectedTenantId by remember {
    mutableStateOf(
      existingContract?.tenantId?.ifBlank {
        tenants.find { it.fullName.trim() == existingContract?.tenantName?.trim() }?.id
      } ?: ""
    )
  }
  var tenantPickerExpanded by remember { mutableStateOf(false) }
  var showInlineTenantForm by remember { mutableStateOf(false) }

  // Primary Fields
  var tenantName by remember { mutableStateOf(existingContract?.tenantName ?: "") }
  var nationalId by remember { mutableStateOf(existingContract?.nationalId ?: "") }
  var startDate by remember { mutableStateOf(existingContract?.startDate ?: Formatters.getTodayIso()) }
  var endDate by remember {
    mutableStateOf(
      existingContract?.endDate ?: run {
        val cal = java.util.Calendar.getInstance()
        cal.add(java.util.Calendar.YEAR, 1)
        cal.add(java.util.Calendar.DAY_OF_MONTH, -1)
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(cal.time)
      }
    )
  }
  var monthlyRent by remember { mutableStateOf(existingContract?.monthlyRent?.toInt()?.toString() ?: "3000") }
  var rentDueDay by remember { mutableStateOf(existingContract?.rentDueDay?.toString() ?: "1") }
  var notes by remember { mutableStateOf(existingContract?.notes ?: "") }

  // Extra contract fields
  var contractDate by remember { mutableStateOf(existingContract?.contractDate?.ifBlank { Formatters.getTodayIso() } ?: Formatters.getTodayIso()) }
  var tenantJob by remember { mutableStateOf(existingContract?.tenantJob ?: "موظف") }
  var tenantAddress by remember { mutableStateOf(existingContract?.tenantAddress ?: "الفيوم") }
  var tenantNationality by remember { mutableStateOf(existingContract?.tenantNationality ?: "مصري") }
  var idIssuedFrom by remember { mutableStateOf(existingContract?.idIssuedFrom ?: "الفيوم") }
  var usagePurpose by remember { mutableStateOf(existingContract?.usagePurpose ?: if (isShop) "محل تجاري" else "سكن عائلي") }
  var paymentTerms by remember { mutableStateOf(existingContract?.paymentTerms ?: "ويسدد الايجار في أول كل شهر بموجب ايصالات بتوقيع المالك او الوكيل") }
  var renewalTerms by remember { mutableStateOf(existingContract?.renewalTerms ?: "موافقه كتابية صريحة من المالك") }
  var water by remember { mutableStateOf(existingContract?.water ?: "على المستأجر") }
  var electricity by remember { mutableStateOf(existingContract?.electricity ?: "على المستأجر") }
  var stairsLight by remember { mutableStateOf(existingContract?.stairsLight ?: "على المستأجر") }
  var doormanFee by remember { mutableStateOf(existingContract?.doormanFee ?: "لا يوجد") }
  var depositAmount by remember { mutableStateOf(existingContract?.depositAmount?.toInt()?.toString() ?: "3000") }
  var extraClause by remember { mutableStateOf(existingContract?.extraClause ?: "") }

  var unitDropdownExpanded by remember { mutableStateOf(false) }

  fun buildContractObject(): Contract {
    return Contract(
      id = existingContract?.id ?: "",
      unitId = selectedUnitId,
      tenantId = selectedTenantId,
      tenantName = tenantName.trim(),
      nationalId = nationalId.trim(),
      startDate = startDate.trim(),
      endDate = endDate.trim(),
      monthlyRent = monthlyRent.toDoubleOrNull() ?: 3000.0,
      rentDueDay = rentDueDay.toIntOrNull() ?: 1,
      notes = notes.trim().ifBlank { null },
      status = existingContract?.status ?: ContractStatus.ACTIVE,
      isHistory = existingContract?.isHistory ?: false,
      contractDate = contractDate.trim(),
      tenantJob = tenantJob.trim(),
      tenantAddress = tenantAddress.trim(),
      tenantNationality = tenantNationality.trim(),
      idIssuedFrom = idIssuedFrom.trim(),
      usagePurpose = usagePurpose.trim(),
      paymentTerms = paymentTerms.trim(),
      renewalTerms = renewalTerms.trim(),
      water = water.trim(),
      electricity = electricity.trim(),
      stairsLight = stairsLight.trim(),
      doormanFee = doormanFee.trim(),
      depositAmount = depositAmount.toDoubleOrNull() ?: 0.0,
      extraClause = extraClause.trim()
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth()
        .fillMaxHeight(0.95f)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Modal Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            if (step == 2) {
              IconButton(onClick = { step = 1 }) {
                Icon(imageVector = backIcon, contentDescription = "Back", tint = ForestGreen)
              }
            }
            Column {
              Text(
                text = if (step == 1) {
                  if (existingContract != null) (if (isAr) "تعديل عقد الإيجار" else "Edit Lease Contract")
                  else (if (isAr) "تحرير عقد إيجار جديد" else "New Lease Contract")
                } else {
                  if (isAr) "مراجعة بنود العقد والطباعة" else "Review Lease & Print"
                },
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF0F172A)
              )
              Text(
                text = "${currentProperty?.name ?: ""} • ${if (isAr) "وحدة ${currentUnit?.unitNumber}" else "Unit ${currentUnit?.unitNumber}"}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
          }

          IconButton(onClick = onDismiss) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
          }
        }

        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFFE2E8F0)))

        if (step == 1) {
          // --- STEP 1: FORM ---
          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            // Unit selection if new
            if (existingContract == null) {
              item {
                ExposedDropdownMenuBox(
                  expanded = unitDropdownExpanded,
                  onExpandedChange = { unitDropdownExpanded = !unitDropdownExpanded },
                  modifier = Modifier.fillMaxWidth()
                ) {
                  OutlinedTextField(
                    value = "${currentProperty?.name ?: ""} - ${if (isAr) "وحدة ${currentUnit?.unitNumber}" else "Unit ${currentUnit?.unitNumber}"}",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isAr) "اختر الوحدة" else "Select Unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitDropdownExpanded) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                  )
                  ExposedDropdownMenu(
                    expanded = unitDropdownExpanded,
                    onDismissRequest = { unitDropdownExpanded = false }
                  ) {
                    for (u in units) {
                      val p = properties.find { it.id == u.propertyId }
                      DropdownMenuItem(
                        text = { Text("${p?.name ?: ""} - ${if (isAr) "وحدة ${u.unitNumber}" else "Unit ${u.unitNumber}"}") },
                        onClick = { selectedUnitId = u.id; unitDropdownExpanded = false }
                      )
                    }
                  }
                }
              }
            }

            // Tenant Details Section
            item {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      text = if (isAr) "بيانات المستأجر" else "Tenant Details",
                      style = MaterialTheme.typography.titleSmall,
                      color = ForestGreen
                    )

                    Surface(
                      onClick = { showInlineTenantForm = true },
                      shape = RoundedCornerShape(6.dp),
                      color = MintLight
                    ) {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                      ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = if (isAr) "+ مستأجر جديد" else "+ New Tenant", color = ForestGreen, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                      }
                    }
                  }

                  // Pick from existing registered tenants
                  if (tenants.isNotEmpty()) {
                    ExposedDropdownMenuBox(
                      expanded = tenantPickerExpanded,
                      onExpandedChange = { tenantPickerExpanded = it },
                      modifier = Modifier.fillMaxWidth()
                    ) {
                      val currentMatchedTenant = tenants.find { it.id == selectedTenantId || it.fullName.trim() == tenantName.trim() }
                      OutlinedTextField(
                        value = currentMatchedTenant?.let { "${it.fullName} (${it.phone})" } ?: if (tenantName.isNotBlank()) tenantName else (if (isAr) "اختر مستأجر مسجل (اختياري)..." else "Select registered tenant..."),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(if (isAr) "اختيار من المستأجرين المسجلين" else "Choose Existing Tenant") },
                        leadingIcon = {
                          Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ForestGreen)
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = tenantPickerExpanded) },
                        modifier = Modifier
                          .menuAnchor()
                          .fillMaxWidth()
                      )

                      ExposedDropdownMenu(
                        expanded = tenantPickerExpanded,
                        onDismissRequest = { tenantPickerExpanded = false }
                      ) {
                        for (t in tenants) {
                          DropdownMenuItem(
                            text = {
                              Column {
                                Text(text = t.fullName, fontWeight = FontWeight.SemiBold)
                                Text(
                                  text = "${t.job} • ${t.phone} • ${t.address}",
                                  fontSize = 11.sp,
                                  color = Color.Gray
                                )
                              }
                            },
                            onClick = {
                              selectedTenantId = t.id
                              tenantName = t.fullName
                              nationalId = t.nationalId
                              tenantJob = t.job
                              tenantAddress = t.address
                              tenantNationality = t.nationality
                              tenantPickerExpanded = false
                            }
                          )
                        }
                      }
                    }
                  }

                  OutlinedTextField(
                    value = tenantName,
                    onValueChange = { tenantName = it },
                    label = { Text(if (isAr) "اسم المستأجر بالكامل *" else "Tenant Full Name *") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  OutlinedTextField(
                    value = nationalId,
                    onValueChange = { if (it.length <= 14) nationalId = it.filter { ch -> ch.isDigit() } },
                    label = { Text(if (isAr) "الرقم القومي (١٤ رقم)" else "National ID (14 digits)") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = startDate,
                      onValueChange = { startDate = it },
                      label = { Text(if (isAr) "تاريخ البداية (YYYY-MM-DD)" else "Start Date") },
                      modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                      value = endDate,
                      onValueChange = { endDate = it },
                      label = { Text(if (isAr) "تاريخ النهاية (YYYY-MM-DD)" else "End Date") },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Text(
                    text = "${if (isAr) "مدة التعاقد المحسوبة:" else "Calculated duration:"} ${Formatters.calculateDurationText(startDate, endDate, settings.language)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF0284C7)
                  )

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = monthlyRent,
                      onValueChange = { monthlyRent = it },
                      label = { Text(if (isAr) "الإيجار الشهري (ج.م)" else "Monthly Rent") },
                      modifier = Modifier.weight(1.2f)
                    )
                    OutlinedTextField(
                      value = rentDueDay,
                      onValueChange = { rentDueDay = it },
                      label = { Text(if (isAr) "يوم الاستحقاق" else "Due Day") },
                      modifier = Modifier.weight(0.8f)
                    )
                  }

                  OutlinedTextField(
                    value = depositAmount,
                    onValueChange = { depositAmount = it },
                    label = { Text(if (isAr) "مبلغ التأمين (ج.م)" else "Deposit Amount") },
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            }

            // Extra Legal Fields Section
            item {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                  Text(
                    text = if (isAr) "بيانات وبنود العقد الإضافية (تظهر في PDF)" else "Extra Contract Terms (For PDF)",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color(0xFF334155)
                  )

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = contractDate,
                      onValueChange = { contractDate = it },
                      label = { Text(if (isAr) "تاريخ التحرير" else "Contract Date") },
                      modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                      value = tenantJob,
                      onValueChange = { tenantJob = it },
                      label = { Text(if (isAr) "مهنة المستأجر" else "Occupation") },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = tenantAddress,
                      onValueChange = { tenantAddress = it },
                      label = { Text(if (isAr) "عنوان المستأجر" else "Tenant Address") },
                      modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                      value = idIssuedFrom,
                      onValueChange = { idIssuedFrom = it },
                      label = { Text(if (isAr) "صادر من" else "ID Issued From") },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  OutlinedTextField(
                    value = usagePurpose,
                    onValueChange = { usagePurpose = it },
                    label = { Text(if (isAr) "الغرض المستعمل فيه المكان" else "Usage Purpose") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  OutlinedTextField(
                    value = paymentTerms,
                    onValueChange = { paymentTerms = it },
                    label = { Text(if (isAr) "شروط وطريقة السداد" else "Payment Terms") },
                    modifier = Modifier.fillMaxWidth()
                  )

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = water,
                      onValueChange = { water = it },
                      label = { Text(if (isAr) "المياه" else "Water") },
                      modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                      value = electricity,
                      onValueChange = { electricity = it },
                      label = { Text(if (isAr) "الكهرباء" else "Electricity") },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                      value = stairsLight,
                      onValueChange = { stairsLight = it },
                      label = { Text(if (isAr) "نور السلم" else "Stairs Light") },
                      modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                      value = doormanFee,
                      onValueChange = { doormanFee = it },
                      label = { Text(if (isAr) "أجر البواب" else "Doorman Fee") },
                      modifier = Modifier.weight(1f)
                    )
                  }

                  OutlinedTextField(
                    value = extraClause,
                    onValueChange = { extraClause = it },
                    label = { Text(if (isAr) "البند الإضافي (اختياري)" else "Extra Clause") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                  )
                }
              }
            }

            // Buttons
            item {
              Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                  Text(text = if (isAr) "إلغاء" else "Cancel")
                }
                Button(
                  onClick = {
                    if (tenantName.isNotBlank()) {
                      step = 2
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                  modifier = Modifier.weight(2f)
                ) {
                  Text(text = if (isAr) "مراجعة وتأكيد العقد" else "Review Contract Details")
                }
              }
            }
          }
        } else {
          // --- STEP 2: REVIEW & PRINT STEP ---
          val builtContract = remember(step) { buildContractObject() }

          LazyColumn(
            modifier = Modifier
              .weight(1f)
              .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
          ) {
            item {
              Surface(
                shape = RoundedCornerShape(12.dp),
                color = GreenLight,
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = GreenDark, modifier = Modifier.size(24.dp))
                  Spacer(modifier = Modifier.width(10.dp))
                  Text(
                    text = if (isAr) "راجع كافة حقول العقد أدناه للتأكد من صحتها قبل الحفظ والطباعة الرسمية." else "Review all contract fields below before saving and printing.",
                    style = MaterialTheme.typography.bodySmall,
                    color = GreenDark
                  )
                }
              }
            }

            // Review Details List
            item {
              Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
              ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                  ReviewRow(label = if (isAr) "اسم المستأجر" else "Tenant", value = builtContract.tenantName, isBold = true)
                  ReviewRow(label = if (isAr) "الرقم القومي" else "National ID", value = builtContract.nationalId.ifBlank { if (isAr) "غير محدد" else "Not set" })
                  ReviewRow(label = if (isAr) "الوحدة والعقار" else "Unit & Property", value = "${currentProperty?.name} (وحدة ${currentUnit?.unitNumber})")
                  ReviewRow(label = if (isAr) "مدة وسريان العقد" else "Duration", value = "${Formatters.calculateDurationText(builtContract.startDate, builtContract.endDate, settings.language)} (${Formatters.formatUIDate(builtContract.startDate)} إلى ${Formatters.formatUIDate(builtContract.endDate)})")
                  ReviewRow(label = if (isAr) "الإيجار الشهري" else "Monthly Rent", value = Formatters.formatCurrency(builtContract.monthlyRent, settings.language), isBold = true)
                  ReviewRow(label = if (isAr) "يوم الاستحقاق الشهري" else "Due Day", value = "يوم ${builtContract.rentDueDay} من كل شهر")
                  ReviewRow(label = if (isAr) "مبلغ التأمين" else "Deposit", value = Formatters.formatCurrency(builtContract.depositAmount, settings.language))
                  ReviewRow(label = if (isAr) "اسم المالك" else "Landlord", value = "${settings.landlord.name} (${settings.landlord.job})")
                  ReviewRow(label = if (isAr) "الغرض المستعمل" else "Usage", value = builtContract.usagePurpose)
                  ReviewRow(label = if (isAr) "البند الإضافي" else "Extra Clause", value = builtContract.extraClause.ifBlank { if (isAr) "لا يوجد" else "None" })
                }
              }
            }

            // Action Buttons in Review
            item {
              Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                  onClick = {
                    if (currentUnit != null && currentProperty != null) {
                      val template = if (currentProperty.type == PropertyType.SHOPS) settings.templates.shops else settings.templates.apartments
                      PdfContractGenerator.generateAndShareContractPdf(
                        context = context,
                        contract = builtContract,
                        unit = currentUnit,
                        property = currentProperty,
                        landlord = settings.landlord,
                        templateText = template
                      )
                    }
                  },
                  colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                  shape = RoundedCornerShape(12.dp),
                  modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                ) {
                  Icon(imageVector = Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(8.dp))
                  Text(text = if (isAr) "طباعة ومشاركة عقد الإيجار (PDF)" else "Generate & Share Contract PDF", style = MaterialTheme.typography.titleSmall)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                  OutlinedButton(
                    onClick = { step = 1 },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                  ) {
                    Text(text = if (isAr) "تعديل الحقول" else "Edit Fields")
                  }

                  Button(
                    onClick = {
                      onSaveContract(builtContract, existingContract == null)
                      onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GreenDark),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(2f)
                  ) {
                    Text(text = if (isAr) "حفظ العقد في النظام" else "Save Lease")
                  }
                }
              }
            }
          }
        }
      }
    }
  }

  // Inline Add New Tenant Dialog
  if (showInlineTenantForm) {
    TenantFormDialog(
      initialTenant = null,
      language = settings.language,
      onDismiss = { showInlineTenantForm = false },
      onSaveTenant = { newTenant ->
        onSaveNewTenant?.invoke(newTenant)
        selectedTenantId = newTenant.id
        tenantName = newTenant.fullName
        nationalId = newTenant.nationalId
        tenantJob = newTenant.job
        tenantAddress = newTenant.address
        tenantNationality = newTenant.nationality
        showInlineTenantForm = false
      }
    )
  }
}

@Composable
fun ReviewRow(label: String, value: String, isBold: Boolean = false) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 4.dp),
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(text = label, style = MaterialTheme.typography.labelMedium, color = Color.Gray, modifier = Modifier.weight(1f))
    Text(
      text = value,
      style = if (isBold) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
      color = if (isBold) ForestGreen else Color(0xFF0F172A),
      modifier = Modifier.weight(1.4f)
    )
  }
}
