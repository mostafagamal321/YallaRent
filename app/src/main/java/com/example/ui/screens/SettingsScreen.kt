package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.core.content.FileProvider
import com.example.data.model.AppSettings
import com.example.data.model.LandlordDetails
import com.example.data.model.PropertyType
import com.example.data.model.ReminderSettings
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.util.Formatters
import java.io.File
import java.io.FileOutputStream

@Composable
fun SettingsScreen(
  settings: AppSettings,
  onUpdateSettings: (AppSettings) -> Unit,
  onResetTemplate: (PropertyType) -> Unit,
  onExportBackup: () -> String,
  onImportBackup: (String) -> Boolean,
  onClearAllData: (() -> Unit)? = null
) {
  val context = LocalContext.current
  val isAr = settings.language == "ar"

  var showResetConfirmDialog by remember { mutableStateOf(false) }

  // Landlord Form
  var landlordName by remember { mutableStateOf(settings.landlord.name) }
  var landlordJob by remember { mutableStateOf(settings.landlord.job) }
  var landlordAddress by remember { mutableStateOf(settings.landlord.address) }
  var landlordNationality by remember { mutableStateOf(settings.landlord.nationality) }

  // PIN
  var pinEnabled by remember { mutableStateOf(settings.pinEnabled) }
  var pinCode by remember { mutableStateOf(settings.pinCode) }

  // Reminders
  var rentDueDays by remember { mutableStateOf(settings.reminders.rentDueDaysBefore.toString()) }
  var contractEndingDays1 by remember { mutableStateOf(settings.reminders.contractEndingDays1.toString()) }
  var contractEndingDays2 by remember { mutableStateOf(settings.reminders.contractEndingDays2.toString()) }
  var reminderTime by remember { mutableStateOf(settings.reminders.reminderTime) }

  // Templates
  var activeTemplateType by remember { mutableStateOf(PropertyType.APARTMENTS) }
  var aptTemplateText by remember { mutableStateOf(settings.templates.apartments) }
  var shopTemplateText by remember { mutableStateOf(settings.templates.shops) }

  var showSuccessSave by remember { mutableStateOf(false) }

  // Import JSON file picker launcher
  val importLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri: Uri? ->
    if (uri != null) {
      try {
        val inputStream = context.contentResolver.openInputStream(uri)
        val json = inputStream?.bufferedReader()?.use { it.readText() }
        if (json != null) {
          val success = onImportBackup(json)
          if (success) {
            Toast.makeText(context, if (isAr) "تم استعادة البيانات بنجاح!" else "Backup restored successfully!", Toast.LENGTH_SHORT).show()
          } else {
            Toast.makeText(context, if (isAr) "فشل استيراد الملف." else "Failed to parse backup.", Toast.LENGTH_SHORT).show()
          }
        }
      } catch (e: Exception) {
        Toast.makeText(context, if (isAr) "خطأ في قراءة الملف." else "Error reading file.", Toast.LENGTH_SHORT).show()
      }
    }
  }

  fun saveAll() {
    val updated = settings.copy(
      pinEnabled = pinEnabled,
      pinCode = pinCode,
      landlord = LandlordDetails(
        name = landlordName.trim(),
        job = landlordJob.trim(),
        address = landlordAddress.trim(),
        nationality = landlordNationality.trim()
      ),
      reminders = ReminderSettings(
        rentDueDaysBefore = rentDueDays.toIntOrNull() ?: 3,
        contractEndingDays1 = contractEndingDays1.toIntOrNull() ?: 60,
        contractEndingDays2 = contractEndingDays2.toIntOrNull() ?: 30,
        reminderTime = reminderTime
      ),
      templates = settings.templates.copy(
        apartments = aptTemplateText,
        shops = shopTemplateText
      )
    )
    onUpdateSettings(updated)
    showSuccessSave = true
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isAr) "الإعدادات والبيانات" else "Settings & Configurations",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E293B)
          )
          Text(
            text = if (isAr) "بيانات المالك، النماذج، الأمان والنسخ الاحتياطي" else "Landlord info, templates, security & backup",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )
        }

        Button(
          onClick = { saveAll() },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isAr) "حفظ التعديلات" else "Save", fontSize = 12.sp)
        }
      }
    }

    if (showSuccessSave) {
      item {
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFDCFCE7))
            .border(1.dp, Color(0xFFBBF7D0), RoundedCornerShape(10.dp))
            .padding(12.dp)
        ) {
          Text(
            text = if (isAr) "تم حفظ كافة الإعدادات بنجاح!" else "All settings saved successfully!",
            color = Color(0xFF15803D),
            style = MaterialTheme.typography.labelMedium
          )
        }
      }
    }

    // 1. Landlord Details
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isAr) "بيانات المالك (تُستخدم في كافة العقود)" else "Landlord Details", style = MaterialTheme.typography.titleSmall)
          }

          OutlinedTextField(
            value = landlordName,
            onValueChange = { landlordName = it },
            label = { Text(if (isAr) "اسم المالك (المؤجر)" else "Landlord Name") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = landlordJob,
              onValueChange = { landlordJob = it },
              label = { Text(if (isAr) "مهنة المالك" else "Occupation") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = landlordNationality,
              onValueChange = { landlordNationality = it },
              label = { Text(if (isAr) "الجنسية" else "Nationality") },
              modifier = Modifier.weight(1f)
            )
          }

          OutlinedTextField(
            value = landlordAddress,
            onValueChange = { landlordAddress = it },
            label = { Text(if (isAr) "عنوان وموطن المالك" else "Official Address") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // 2. PIN Lock & Security
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Column {
                Text(text = if (isAr) "قفل التطبيق برمز مرور (PIN)" else "PIN Lock Protection", style = MaterialTheme.typography.titleSmall)
                Text(text = if (isAr) "طلب رمز المرور عند فتح التطبيق" else "Require PIN code on app open", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
              }
            }

            Switch(
              checked = pinEnabled,
              onCheckedChange = { pinEnabled = it },
              colors = SwitchDefaults.colors(checkedThumbColor = ForestGreen, checkedTrackColor = Color(0xFFD8F3DC))
            )
          }

          AnimatedVisibility(visible = pinEnabled) {
            Column(modifier = Modifier.padding(top = 10.dp)) {
              OutlinedTextField(
                value = pinCode,
                onValueChange = { if (it.length <= 4) pinCode = it.filter { ch -> ch.isDigit() } },
                label = { Text(if (isAr) "رمز المرور (٤ أرقام)" else "4-digit PIN") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(0.5f)
              )
            }
          }
        }
      }
    }

    // 3. Reminders Settings
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Notifications, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isAr) "إعدادات مواعيد التنبيهات" else "Reminder Timings", style = MaterialTheme.typography.titleSmall)
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = rentDueDays,
              onValueChange = { rentDueDays = it },
              label = { Text(if (isAr) "استحقاق الإيجار (قبل كم يوم)" else "Rent Due (days before)") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = reminderTime,
              onValueChange = { reminderTime = it },
              label = { Text(if (isAr) "وقت التنبيه اليومي" else "Reminder Time") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
              value = contractEndingDays1,
              onValueChange = { contractEndingDays1 = it },
              label = { Text(if (isAr) "انتهاء العقد (تنبيه ١ يوم)" else "Contract End Alert 1") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = contractEndingDays2,
              onValueChange = { contractEndingDays2 = it },
              label = { Text(if (isAr) "انتهاء العقد (تنبيه ٢ يوم)" else "Contract End Alert 2") },
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 4. Contract Templates
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(text = if (isAr) "تعديل نماذج وبنود العقود" else "Contract Templates", style = MaterialTheme.typography.titleSmall)
            }

            Surface(
              onClick = {
                onResetTemplate(activeTemplateType)
                if (activeTemplateType == PropertyType.APARTMENTS) {
                  aptTemplateText = com.example.data.repository.DefaultTemplates.APARTMENT_TEMPLATE
                } else {
                  shopTemplateText = com.example.data.repository.DefaultTemplates.SHOPS_TEMPLATE
                }
              },
              shape = RoundedCornerShape(6.dp),
              color = Color(0xFFFFEDD5)
            ) {
              Text(
                text = if (isAr) "استعادة الافتراضي" else "Reset Default",
                color = Color(0xFFC2410C),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
              onClick = { activeTemplateType = PropertyType.APARTMENTS },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (activeTemplateType == PropertyType.APARTMENTS) ForestGreen else Color(0xFFF1F5F9),
                contentColor = if (activeTemplateType == PropertyType.APARTMENTS) Color.White else Color(0xFF475569)
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = if (isAr) "عقود الشقق" else "Apartments", fontSize = 12.sp)
            }
            Button(
              onClick = { activeTemplateType = PropertyType.SHOPS },
              colors = ButtonDefaults.buttonColors(
                containerColor = if (activeTemplateType == PropertyType.SHOPS) ForestGreen else Color(0xFFF1F5F9),
                contentColor = if (activeTemplateType == PropertyType.SHOPS) Color.White else Color(0xFF475569)
              ),
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = if (isAr) "عقود المحلات" else "Shops", fontSize = 12.sp)
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          OutlinedTextField(
            value = if (activeTemplateType == PropertyType.APARTMENTS) aptTemplateText else shopTemplateText,
            onValueChange = {
              if (activeTemplateType == PropertyType.APARTMENTS) aptTemplateText = it else shopTemplateText = it
            },
            minLines = 8,
            maxLines = 12,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // 5. Backup & Restore
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.Backup, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = if (isAr) "النسخ الاحتياطي واستعادة البيانات" else "Backup & Restore", style = MaterialTheme.typography.titleSmall)
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (isAr) "احفظ ملف نسخة احتياطية على هاتفك أو شاركه عبر واتساب/الإيميل لضمان عدم فقدان بياناتك." else "Export a JSON backup to your device or share it to prevent data loss.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )

          if (settings.lastBackupDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = "${if (isAr) "تاريخ آخر نسخة احتياطية:" else "Last backup:"} ${Formatters.formatUIDate(settings.lastBackupDate)}",
              style = MaterialTheme.typography.labelSmall,
              color = ForestGreen
            )
          }

          Spacer(modifier = Modifier.height(14.dp))

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
              onClick = {
                val json = onExportBackup()
                val cacheDir = context.cacheDir
                val file = File(cacheDir, "RentManager-Backup-${Formatters.getTodayIso()}.json")
                val fos = FileOutputStream(file)
                fos.write(json.toByteArray())
                fos.close()

                val uri = FileProvider.getUriForFile(context, "com.family.rentmanager.fileprovider", file)
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                  type = "application/json"
                  putExtra(Intent.EXTRA_STREAM, uri)
                  putExtra(Intent.EXTRA_SUBJECT, "Rent Manager Backup")
                  addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(shareIntent, if (isAr) "مشاركة النسخة الاحتياطية" else "Share Backup"))
              },
              colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = if (isAr) "تصدير نسخة" else "Export", fontSize = 12.sp)
            }

            OutlinedButton(
              onClick = {
                importLauncher.launch(arrayOf("application/json", "*/*"))
              },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.weight(1f)
            ) {
              Icon(imageVector = Icons.Default.FileUpload, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = if (isAr) "استعادة نسخة" else "Import", fontSize = 12.sp)
            }
          }
        }
      }
    }

    // Reset Data / Start from Empty
    item {
      Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.RestartAlt,
              contentDescription = null,
              tint = RedAlert,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isAr) "البدء من الصفر وتفريغ قاعدة البيانات" else "Start from Empty / Reset Data",
              style = MaterialTheme.typography.titleSmall,
              color = RedAlert
            )
          }

          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = if (isAr) "تفريغ كافة العقارات والوحدات والعقود والمستأجرين والدفعات والبدء بقاعدة بيانات جديدة ونظيفة لإدخال بياناتك الحقيقية من الصفر." else "Clear all properties, units, leases, tenants, and payments to start completely fresh.",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )

          Spacer(modifier = Modifier.height(14.dp))

          Button(
            onClick = { showResetConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = RedAlert),
            shape = RoundedCornerShape(10.dp)
          ) {
            Icon(imageVector = Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (isAr) "تفريغ كافة البيانات والبدء من الصفر" else "Clear All Data & Start Fresh",
              fontSize = 12.sp
            )
          }
        }
      }
    }

    item {
      Spacer(modifier = Modifier.height(40.dp))
    }
  }

  // Reset Confirmation Dialog
  if (showResetConfirmDialog) {
    AlertDialog(
      onDismissRequest = { showResetConfirmDialog = false },
      title = {
        Text(text = if (isAr) "تأكيد تفريغ كافة البيانات" else "Confirm Reset All Data")
      },
      text = {
        Text(
          text = if (isAr) "هل أنت متأكد من رغبتك في حذف جميع العقارات والوحدات والعقود والمستأجرين والدفعات والبدء بقاعدة بيانات فارغة تماماً؟ يمكنك إدخال كافة بياناتك الخاصة بحرية بعد ذلك."
          else "Are you sure you want to clear all data and start completely fresh? You will be able to add your real properties, contracts, and tenants."
        )
      },
      confirmButton = {
        Button(
          onClick = {
            onClearAllData?.invoke()
            showResetConfirmDialog = false
            Toast.makeText(context, if (isAr) "تم تفريغ كافة البيانات والبدء من الصفر بنجاح" else "All data cleared successfully", Toast.LENGTH_SHORT).show()
          },
          colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
        ) {
          Text(text = if (isAr) "تأكيد والبدء من الصفر" else "Confirm & Start Fresh")
        }
      },
      dismissButton = {
        OutlinedButton(onClick = { showResetConfirmDialog = false }) {
          Text(text = if (isAr) "إلغاء" else "Cancel")
        }
      }
    )
  }
}
