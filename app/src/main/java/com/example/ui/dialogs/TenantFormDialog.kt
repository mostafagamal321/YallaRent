package com.example.ui.dialogs

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Tenant
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenLight
import com.example.util.ImageUtils
import java.io.File

@Composable
fun TenantFormDialog(
  initialTenant: Tenant? = null,
  language: String,
  onDismiss: () -> Unit,
  onSaveTenant: (Tenant) -> Unit
) {
  val context = LocalContext.current
  val isAr = language == "ar"

  var fullName by remember { mutableStateOf(initialTenant?.fullName ?: "") }
  var nationalId by remember { mutableStateOf(initialTenant?.nationalId ?: "") }
  var job by remember { mutableStateOf(initialTenant?.job ?: if (isAr) "موظف" else "Employee") }
  var phone by remember { mutableStateOf(initialTenant?.phone ?: "") }
  var address by remember { mutableStateOf(initialTenant?.address ?: if (isAr) "الفيوم" else "Fayoum") }
  var nationality by remember { mutableStateOf(initialTenant?.nationality ?: if (isAr) "مصري" else "Egyptian") }
  var notes by remember { mutableStateOf(initialTenant?.notes ?: "") }

  var photoPath by remember { mutableStateOf(initialTenant?.photoPath) }
  var idFrontPath by remember { mutableStateOf(initialTenant?.idCardPhotoFrontPath) }
  var idBackPath by remember { mutableStateOf(initialTenant?.idCardPhotoBackPath) }

  var activePhotoTarget by remember { mutableStateOf<String?>(null) } // "avatar", "front", "back"

  // Activity Result Launchers
  val pickMediaLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.PickVisualMedia()
  ) { uri: Uri? ->
    if (uri != null) {
      val savedPath = ImageUtils.saveCompressedImageFromUri(
        context = context,
        uri = uri,
        prefix = activePhotoTarget ?: "tenant"
      )
      when (activePhotoTarget) {
        "avatar" -> photoPath = savedPath
        "front" -> idFrontPath = savedPath
        "back" -> idBackPath = savedPath
      }
    }
  }

  val takePictureLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.TakePicturePreview()
  ) { bitmap: Bitmap? ->
    if (bitmap != null) {
      val savedPath = ImageUtils.saveCompressedBitmap(
        context = context,
        originalBitmap = bitmap,
        prefix = activePhotoTarget ?: "tenant"
      )
      when (activePhotoTarget) {
        "avatar" -> photoPath = savedPath
        "front" -> idFrontPath = savedPath
        "back" -> idBackPath = savedPath
      }
    }
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = Color.White,
      modifier = Modifier
        .fillMaxWidth(0.95f)
        .fillMaxHeight(0.92f)
    ) {
      Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .background(ForestGreen)
            .padding(horizontal = 16.dp, vertical = 14.dp),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = if (initialTenant == null) {
              if (isAr) "إضافة مستأجر جديد" else "Add New Tenant"
            } else {
              if (isAr) "تعديل بيانات المستأجر" else "Edit Tenant Details"
            },
            color = Color.White,
            style = MaterialTheme.typography.titleMedium
          )

          IconButton(
            onClick = onDismiss,
            modifier = Modifier.size(32.dp).testTag("close_tenant_form_button")
          ) {
            Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = Color.White)
          }
        }

        LazyColumn(
          modifier = Modifier
            .weight(1f)
            .padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
          // --- 1. Photo Avatar Section ---
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
              ) {
                Text(
                  text = if (isAr) "صورة المستأجر" else "Tenant Photo",
                  style = MaterialTheme.typography.labelLarge,
                  color = Color(0xFF1E293B)
                )

                Spacer(modifier = Modifier.height(10.dp))

                Box(
                  modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE2E8F0))
                    .border(2.dp, ForestGreen, CircleShape),
                  contentAlignment = Alignment.Center
                ) {
                  if (photoPath != null && File(photoPath!!).exists()) {
                    AsyncImage(
                      model = ImageRequest.Builder(context)
                        .data(File(photoPath!!))
                        .crossfade(true)
                        .build(),
                      contentDescription = "Tenant Photo",
                      contentScale = ContentScale.Crop,
                      modifier = Modifier.fillMaxSize()
                    )
                  } else {
                    Icon(
                      imageVector = Icons.Default.Person,
                      contentDescription = null,
                      tint = Color(0xFF94A3B8),
                      modifier = Modifier.size(54.dp)
                    )
                  }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                  Button(
                    onClick = {
                      activePhotoTarget = "avatar"
                      takePictureLauncher.launch(null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("tenant_camera_button")
                  ) {
                    Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isAr) "كاميرا" else "Camera", fontSize = 12.sp)
                  }

                  OutlinedButton(
                    onClick = {
                      activePhotoTarget = "avatar"
                      pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("tenant_gallery_button")
                  ) {
                    Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = if (isAr) "المعرض" else "Gallery", fontSize = 12.sp)
                  }

                  if (photoPath != null) {
                    IconButton(
                      onClick = { photoPath = null },
                      modifier = Modifier.size(38.dp)
                    ) {
                      Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete photo", tint = Color(0xFFEF4444))
                    }
                  }
                }
              }
            }
          }

          // --- 2. Personal Info Fields ---
          item {
            OutlinedTextField(
              value = fullName,
              onValueChange = { fullName = it },
              label = { Text(if (isAr) "الاسم بالكامل *" else "Full Name *") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("tenant_name_input")
            )
          }

          item {
            OutlinedTextField(
              value = nationalId,
              onValueChange = { if (it.length <= 14 && it.all { c -> c.isDigit() }) nationalId = it },
              label = { Text(if (isAr) "الرقم القومي (١٤ رقم)" else "National ID (14 digits)") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
              singleLine = true,
              supportingText = {
                Text(
                  text = "${nationalId.length}/14 ${if (isAr) "رقم" else "digits"}",
                  fontSize = 11.sp,
                  color = if (nationalId.length == 14) ForestGreen else Color.Gray
                )
              },
              modifier = Modifier.fillMaxWidth().testTag("tenant_national_id_input")
            )
          }

          item {
            OutlinedTextField(
              value = job,
              onValueChange = { job = it },
              label = { Text(if (isAr) "مهنة المستأجر" else "Tenant Job") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("tenant_job_input")
            )
          }

          item {
            OutlinedTextField(
              value = phone,
              onValueChange = { phone = it },
              label = { Text(if (isAr) "رقم الهاتف" else "Phone Number") },
              placeholder = { Text("010XXXXXXXX") },
              keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
              singleLine = true,
              modifier = Modifier.fillMaxWidth().testTag("tenant_phone_input")
            )
          }

          item {
            OutlinedTextField(
              value = address,
              onValueChange = { address = it },
              label = { Text(if (isAr) "العنوان / محل الإقامة" else "Address") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          item {
            OutlinedTextField(
              value = nationality,
              onValueChange = { nationality = it },
              label = { Text(if (isAr) "الجنسية" else "Nationality") },
              singleLine = true,
              modifier = Modifier.fillMaxWidth()
            )
          }

          // --- 3. ID Card Photos Section (Optional Front & Back) ---
          item {
            Card(
              shape = RoundedCornerShape(12.dp),
              colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(18.dp))
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isAr) "صور بطاقة الرقم القومي (اختياري)" else "National ID Card Photos (Optional)",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color(0xFF1E293B)
                  )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  // Front card slot
                  IdCardPhotoSlot(
                    title = if (isAr) "وجه البطاقة" else "ID Front",
                    photoPath = idFrontPath,
                    isArabic = isAr,
                    onCameraClick = {
                      activePhotoTarget = "front"
                      takePictureLauncher.launch(null)
                    },
                    onGalleryClick = {
                      activePhotoTarget = "front"
                      pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = { idFrontPath = null },
                    modifier = Modifier.weight(1f)
                  )

                  // Back card slot
                  IdCardPhotoSlot(
                    title = if (isAr) "ظهر البطاقة" else "ID Back",
                    photoPath = idBackPath,
                    isArabic = isAr,
                    onCameraClick = {
                      activePhotoTarget = "back"
                      takePictureLauncher.launch(null)
                    },
                    onGalleryClick = {
                      activePhotoTarget = "back"
                      pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemove = { idBackPath = null },
                    modifier = Modifier.weight(1f)
                  )
                }
              }
            }
          }

          // --- 4. Notes ---
          item {
            OutlinedTextField(
              value = notes,
              onValueChange = { notes = it },
              label = { Text(if (isAr) "ملاحظات إضافية" else "Additional Notes") },
              minLines = 2,
              modifier = Modifier.fillMaxWidth()
            )
          }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        // Bottom Action Buttons
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
          OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier.weight(1f)
          ) {
            Text(if (isAr) "إلغاء" else "Cancel")
          }

          Button(
            onClick = {
              if (fullName.isNotBlank()) {
                val tenantToSave = Tenant(
                  id = initialTenant?.id ?: "tnt-${System.currentTimeMillis()}",
                  fullName = fullName.trim(),
                  nationalId = nationalId.trim(),
                  job = job.trim().ifBlank { if (isAr) "موظف" else "Employee" },
                  phone = phone.trim(),
                  address = address.trim().ifBlank { if (isAr) "الفيوم" else "Fayoum" },
                  nationality = nationality.trim().ifBlank { if (isAr) "مصري" else "Egyptian" },
                  photoPath = photoPath,
                  idCardPhotoFrontPath = idFrontPath,
                  idCardPhotoBackPath = idBackPath,
                  notes = notes.trim().ifBlank { null }
                )
                onSaveTenant(tenantToSave)
              }
            },
            enabled = fullName.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
            modifier = Modifier.weight(1f).testTag("save_tenant_button")
          ) {
            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (isAr) "حفظ المستأجر" else "Save Tenant")
          }
        }
      }
    }
  }
}

@Composable
private fun IdCardPhotoSlot(
  title: String,
  photoPath: String?,
  isArabic: Boolean,
  onCameraClick: () -> Unit,
  onGalleryClick: () -> Unit,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  Column(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(Color.White)
      .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(8.dp))
      .padding(8.dp),
    horizontalAlignment = Alignment.CenterHorizontally
  ) {
    Text(text = title, fontSize = 11.sp, style = MaterialTheme.typography.labelMedium, color = Color(0xFF334155))
    Spacer(modifier = Modifier.height(6.dp))

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(80.dp)
        .clip(RoundedCornerShape(6.dp))
        .background(Color(0xFFF1F5F9)),
      contentAlignment = Alignment.Center
    ) {
      if (photoPath != null && File(photoPath).exists()) {
        AsyncImage(
          model = ImageRequest.Builder(context)
            .data(File(photoPath))
            .crossfade(true)
            .build(),
          contentDescription = title,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize()
        )
      } else {
        Icon(imageVector = Icons.Default.Badge, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(32.dp))
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
      IconButton(onClick = onCameraClick, modifier = Modifier.size(30.dp)) {
        Icon(imageVector = Icons.Default.CameraAlt, contentDescription = "Camera", tint = ForestGreen, modifier = Modifier.size(16.dp))
      }
      IconButton(onClick = onGalleryClick, modifier = Modifier.size(30.dp)) {
        Icon(imageVector = Icons.Default.PhotoLibrary, contentDescription = "Gallery", tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
      }
      if (photoPath != null) {
        IconButton(onClick = onRemove, modifier = Modifier.size(30.dp)) {
          Icon(imageVector = Icons.Default.Delete, contentDescription = "Remove", tint = Color(0xFFEF4444), modifier = Modifier.size(16.dp))
        }
      }
    }
  }
}
