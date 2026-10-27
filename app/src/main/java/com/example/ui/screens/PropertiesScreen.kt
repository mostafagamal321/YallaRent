package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Property
import com.example.data.model.PropertyType
import com.example.data.model.Tenant
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.util.Formatters
import java.io.File

@Composable
fun PropertiesScreen(
  properties: List<Property>,
  units: List<UnitItem>,
  contracts: List<Contract>,
  searchQuery: String,
  language: String,
  tenants: List<Tenant> = emptyList(),
  onAddProperty: (String, PropertyType, String) -> Unit,
  onUpdateProperty: (Property) -> Unit,
  onDeleteProperty: ((String) -> Unit)? = null,
  onAddUnit: (String, String, String, String?) -> Unit,
  onUpdateUnit: (UnitItem) -> Unit,
  onDeleteUnit: ((String) -> Unit)? = null,
  onOpenContractDialog: (unitId: String?, contract: Contract?) -> Unit,
  onOpenUnitDetail: (unitId: String) -> Unit,
  onOpenTenantProfile: ((Tenant) -> Unit)? = null
) {
  val context = LocalContext.current
  val isAr = language == "ar"
  var filterStatus by remember { mutableStateOf("all") }
  var expandedPropertyId by remember { mutableStateOf(properties.firstOrNull()?.id) }

  // Property Dialog State
  var showPropertyDialog by remember { mutableStateOf(false) }
  var editingProperty by remember { mutableStateOf<Property?>(null) }
  var propNameInput by remember { mutableStateOf("") }
  var propTypeInput by remember { mutableStateOf(PropertyType.APARTMENTS) }
  var propAddressInput by remember { mutableStateOf("") }

  // Unit Dialog State
  var showUnitDialog by remember { mutableStateOf(false) }
  var targetPropertyId by remember { mutableStateOf("") }
  var editingUnit by remember { mutableStateOf<UnitItem?>(null) }
  var unitNumInput by remember { mutableStateOf("") }
  var unitDescInput by remember { mutableStateOf("") }
  var unitNotesInput by remember { mutableStateOf("") }

  val activeContractsByUnitId = contracts.filter { !it.isHistory }.associateBy { it.unitId }

  fun getUnitStatus(unit: UnitItem): String {
    val contract = activeContractsByUnitId[unit.id]
    if (contract == null) return "vacant"
    val daysLeft = Formatters.getDaysFromToday(contract.endDate)
    return when {
      daysLeft < 0 -> "expired"
      daysLeft <= 60 -> "expiring_soon"
      else -> "active"
    }
  }

  // Filter properties & units
  val filteredProperties = properties.map { prop ->
    var propUnits = units.filter { it.propertyId == prop.id }
    if (filterStatus != "all") {
      propUnits = propUnits.filter { getUnitStatus(it) == filterStatus }
    }
    if (searchQuery.isNotBlank()) {
      val q = searchQuery.trim().lowercase()
      val propMatches = prop.name.lowercase().contains(q) || prop.address.lowercase().contains(q)
      propUnits = propUnits.filter { u ->
        val cnt = activeContractsByUnitId[u.id]
        val tenantMatches = cnt?.tenantName?.lowercase()?.contains(q) == true
        val unitMatches = u.unitNumber.lowercase().contains(q)
        val noteMatches = u.notes?.lowercase()?.contains(q) == true
        propMatches || tenantMatches || unitMatches || noteMatches
      }
    }
    prop to propUnits
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Top Bar
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = if (isAr) "العقارات والوحدات" else "Properties & Units",
            style = MaterialTheme.typography.titleMedium,
            color = Color(0xFF1E293B)
          )
          Text(
            text = "${properties.size} ${if (isAr) "عقارات" else "Properties"} • ${units.size} ${if (isAr) "وحدات" else "Units"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )
        }

        Button(
          onClick = {
            editingProperty = null
            propNameInput = ""
            propTypeInput = PropertyType.APARTMENTS
            propAddressInput = ""
            showPropertyDialog = true
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(10.dp)
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isAr) "إضافة عقار" else "Add Property", fontSize = 12.sp)
        }
      }
    }

    // Filter Chips
    item {
      val filterOptions = listOf(
        Pair("all", if (isAr) "الكل" else "All"),
        Pair("active", if (isAr) "ساري (مؤجر)" else "Active"),
        Pair("vacant", if (isAr) "شاغرة" else "Vacant"),
        Pair("expiring_soon", if (isAr) "ينتهي قريباً" else "Expiring Soon"),
        Pair("expired", if (isAr) "منتهي" else "Expired")
      )

      Row(
        modifier = Modifier
          .fillMaxWidth()
          .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        for ((key, label) in filterOptions) {
          val isSelected = filterStatus == key
          Surface(
            onClick = { filterStatus = key },
            shape = RoundedCornerShape(20.dp),
            color = if (isSelected) ForestGreen else Color.White,
            border = androidx.compose.foundation.BorderStroke(1.dp, if (isSelected) ForestGreen else Color(0xFFCBD5E1)),
            modifier = Modifier.height(34.dp)
          ) {
            Box(
              modifier = Modifier.padding(horizontal = 14.dp),
              contentAlignment = Alignment.Center
            ) {
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

    // Properties List
    items(filteredProperties) { (property, propUnits) ->
      val isExpanded = expandedPropertyId == property.id
      val isShop = property.type == PropertyType.SHOPS
      val allUnitsInProp = units.count { it.propertyId == property.id }

      Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column {
          // Property Card Header
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { expandedPropertyId = if (isExpanded) null else property.id }
              .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(if (isShop) Color(0xFFFEF3C7) else GreenLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = if (isShop) Icons.Default.Store else Icons.Default.Home,
                  contentDescription = null,
                  tint = if (isShop) Color(0xFFB45309) else GreenDark,
                  modifier = Modifier.size(22.dp)
                )
              }

              Spacer(modifier = Modifier.width(10.dp))

              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(text = property.name, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F172A))
                  Spacer(modifier = Modifier.width(6.dp))
                  Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = if (isShop) Color(0xFFFEF3C7) else Color(0xFFE0E7FF)
                  ) {
                    Text(
                      text = if (isShop) (if (isAr) "محلات" else "Shops") else (if (isAr) "عمارة سكنية" else "Apartments"),
                      color = if (isShop) Color(0xFF92400E) else Color(0xFF3730A3),
                      style = MaterialTheme.typography.labelSmall,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                  text = "${property.address} • $allUnitsInProp ${if (isAr) "وحدات" else "units"}",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF64748B)
                )
              }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  editingProperty = property
                  propNameInput = property.name
                  propTypeInput = property.type
                  propAddressInput = property.address
                  showPropertyDialog = true
                },
                modifier = Modifier.size(34.dp)
              ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
              }

              Icon(
                imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null,
                tint = Color(0xFF64748B)
              )
            }
          }

          // Units List when expanded
          AnimatedVisibility(visible = isExpanded) {
            Column(
              modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF8FAFC))
                .padding(14.dp),
              verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = if (isAr) "الوحدات والمستأجرين (${propUnits.size})" else "Units & Tenants (${propUnits.size})",
                  style = MaterialTheme.typography.labelMedium,
                  color = Color(0xFF334155)
                )

                Surface(
                  onClick = {
                    targetPropertyId = property.id
                    editingUnit = null
                    unitNumInput = ""
                    unitDescInput = if (isShop) (if (isAr) "محل تجاري" else "Commercial Shop") else (if (isAr) "شقة سكنية" else "Apartment")
                    unitNotesInput = ""
                    showUnitDialog = true
                  },
                  shape = RoundedCornerShape(8.dp),
                  color = Color(0xFFF0FDF4),
                  border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0))
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                  ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = GreenDark, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = if (isAr) "إضافة وحدة" else "Add Unit", color = GreenDark, style = MaterialTheme.typography.labelSmall)
                  }
                }
              }

              if (propUnits.isEmpty()) {
                Text(
                  text = if (isAr) "لا توجد وحدات تطابق التصفية الحالية" else "No units match current filter",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color.Gray,
                  modifier = Modifier.padding(vertical = 10.dp)
                )
              } else {
                for (unit in propUnits) {
                  val contract = activeContractsByUnitId[unit.id]
                  val status = getUnitStatus(unit)

                  val (statusLabel, statusBg, statusColor) = when (status) {
                    "vacant" -> Triple(if (isAr) "شاغرة" else "Vacant", Color(0xFFF1F5F9), Color(0xFF475569))
                    "expired" -> Triple(if (isAr) "منتهي" else "Expired", RedLight, RedAlert)
                    "expiring_soon" -> Triple(if (isAr) "ينتهي قريباً" else "Expiring Soon", Color(0xFFFFEDD5), Color(0xFFC2410C))
                    else -> Triple(if (isAr) "ساري" else "Active", GreenLight, GreenDark)
                  }

                  Card(
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                  ) {
                    Row(
                      modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                      horizontalArrangement = Arrangement.SpaceBetween,
                      verticalAlignment = Alignment.CenterVertically
                    ) {
                      Column(
                        modifier = Modifier
                          .weight(1f)
                          .clickable { onOpenUnitDetail(unit.id) }
                      ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                          Text(
                            text = if (isAr) "وحدة ${unit.unitNumber}" else "Unit ${unit.unitNumber}",
                            style = MaterialTheme.typography.titleSmall,
                            color = Color(0xFF0F172A)
                          )
                          Spacer(modifier = Modifier.width(6.dp))
                          Surface(shape = RoundedCornerShape(4.dp), color = statusBg) {
                            Text(
                              text = statusLabel,
                              color = statusColor,
                              style = MaterialTheme.typography.labelSmall,
                              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                          }
                        }

                        Spacer(modifier = Modifier.height(2.dp))

                        if (contract != null) {
                          val matchedTenant = tenants.find { it.id == contract.tenantId || it.fullName.trim() == contract.tenantName.trim() }
                          Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                              .clip(RoundedCornerShape(6.dp))
                              .clickable(enabled = onOpenTenantProfile != null) {
                                val tenantToOpen = matchedTenant ?: Tenant(
                                  id = contract.tenantId.ifBlank { "tenant-${contract.id}" },
                                  fullName = contract.tenantName,
                                  nationalId = contract.nationalId,
                                  job = contract.tenantJob,
                                  phone = "",
                                  address = contract.tenantAddress,
                                  nationality = contract.tenantNationality
                                )
                                onOpenTenantProfile?.invoke(tenantToOpen)
                              }
                              .padding(vertical = 2.dp)
                          ) {
                            Box(
                              modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFE2E8F0)),
                              contentAlignment = Alignment.Center
                            ) {
                              val photoFile = matchedTenant?.photoPath?.let { File(it) }
                              if (photoFile?.exists() == true) {
                                AsyncImage(
                                  model = ImageRequest.Builder(context)
                                    .data(photoFile)
                                    .crossfade(true)
                                    .build(),
                                  contentDescription = contract.tenantName,
                                  contentScale = ContentScale.Crop,
                                  modifier = Modifier.fillMaxSize()
                                )
                              } else {
                                Icon(
                                  imageVector = Icons.Default.Person,
                                  contentDescription = null,
                                  tint = Color.Gray,
                                  modifier = Modifier.size(14.dp)
                                )
                              }
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                              text = contract.tenantName,
                              style = MaterialTheme.typography.labelMedium,
                              color = ForestGreen
                            )
                          }
                          Text(
                            text = "${Formatters.formatCurrency(contract.monthlyRent, language)} • ${if (isAr) "ينتهي:" else "Ends:"} ${Formatters.formatUIDate(contract.endDate)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF64748B)
                          )
                        } else {
                          Text(
                            text = unit.notes ?: (if (isAr) "لا يوجد عقد نشط حالياً" else "No active lease"),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (unit.notes != null) Color(0xFF0284C7) else Color(0xFF64748B)
                          )
                        }
                      }

                      Row(verticalAlignment = Alignment.CenterVertically) {
                        if (contract != null) {
                          Surface(
                            onClick = { onOpenUnitDetail(unit.id) },
                            shape = RoundedCornerShape(8.dp),
                            color = ForestGreen
                          ) {
                            Text(
                              text = if (isAr) "العقد" else "Lease",
                              color = Color.White,
                              style = MaterialTheme.typography.labelSmall,
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                          }
                        } else {
                          Surface(
                            onClick = { onOpenContractDialog(unit.id, null) },
                            shape = RoundedCornerShape(8.dp),
                            color = Color(0xFF2563EB)
                          ) {
                            Text(
                              text = if (isAr) "إنشاء عقد" else "Add Lease",
                              color = Color.White,
                              style = MaterialTheme.typography.labelSmall,
                              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                          }
                        }

                        IconButton(
                          onClick = {
                            targetPropertyId = property.id
                            editingUnit = unit
                            unitNumInput = unit.unitNumber
                            unitDescInput = unit.description
                            unitNotesInput = unit.notes ?: ""
                            showUnitDialog = true
                          },
                          modifier = Modifier.size(32.dp)
                        ) {
                          Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit Unit", tint = Color(0xFF94A3B8), modifier = Modifier.size(14.dp))
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
    }

    item {
      Spacer(modifier = Modifier.height(40.dp))
    }
  }

  // Property Add/Edit Dialog
  if (showPropertyDialog) {
    AlertDialog(
      onDismissRequest = { showPropertyDialog = false },
      title = {
        Text(text = if (editingProperty != null) (if (isAr) "تعديل العقار" else "Edit Property") else (if (isAr) "إضافة عقار جديد" else "Add New Property"))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = propNameInput,
            onValueChange = { propNameInput = it },
            label = { Text(if (isAr) "اسم العقار" else "Property Name") },
            modifier = Modifier.fillMaxWidth()
          )

          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
              onClick = { propTypeInput = PropertyType.APARTMENTS },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (propTypeInput == PropertyType.APARTMENTS) GreenLight else Color.Transparent
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = if (isAr) "شقق سكنية" else "Apartments", color = ForestGreen)
            }
            OutlinedButton(
              onClick = { propTypeInput = PropertyType.SHOPS },
              colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (propTypeInput == PropertyType.SHOPS) Color(0xFFFEF3C7) else Color.Transparent
              ),
              modifier = Modifier.weight(1f)
            ) {
              Text(text = if (isAr) "محلات تجارية" else "Shops", color = Color(0xFF92400E))
            }
          }

          OutlinedTextField(
            value = propAddressInput,
            onValueChange = { propAddressInput = it },
            label = { Text(if (isAr) "عنوان العقار (المذكور بالعقد)" else "Property Address") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (propNameInput.isNotBlank()) {
              if (editingProperty != null) {
                onUpdateProperty(editingProperty!!.copy(name = propNameInput.trim(), type = propTypeInput, address = propAddressInput.trim()))
              } else {
                onAddProperty(propNameInput.trim(), propTypeInput, propAddressInput.trim())
              }
              showPropertyDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
          Text(text = if (isAr) "حفظ" else "Save")
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (editingProperty != null && onDeleteProperty != null) {
            Button(
              onClick = {
                onDeleteProperty(editingProperty!!.id)
                showPropertyDialog = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
            ) {
              Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = if (isAr) "حذف" else "Delete")
            }
          }
          OutlinedButton(onClick = { showPropertyDialog = false }) {
            Text(text = if (isAr) "إلغاء" else "Cancel")
          }
        }
      }
    )
  }

  // Unit Add/Edit Dialog
  if (showUnitDialog) {
    AlertDialog(
      onDismissRequest = { showUnitDialog = false },
      title = {
        Text(text = if (editingUnit != null) (if (isAr) "تعديل بيانات الوحدة" else "Edit Unit") else (if (isAr) "إضافة وحدة جديدة" else "Add New Unit"))
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = unitNumInput,
            onValueChange = { unitNumInput = it },
            label = { Text(if (isAr) "رقم أو معرف الوحدة" else "Unit Number") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = unitDescInput,
            onValueChange = { unitDescInput = it },
            label = { Text(if (isAr) "بيان ووصف العين" else "Unit Description") },
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = unitNotesInput,
            onValueChange = { unitNotesInput = it },
            label = { Text(if (isAr) "ملاحظات (المستأجر السابق أو المتوقع)" else "Notes") },
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (unitNumInput.isNotBlank()) {
              if (editingUnit != null) {
                onUpdateUnit(editingUnit!!.copy(unitNumber = unitNumInput.trim(), description = unitDescInput.trim(), notes = unitNotesInput.trim().ifBlank { null }))
              } else {
                onAddUnit(targetPropertyId, unitNumInput.trim(), unitDescInput.trim(), unitNotesInput.trim())
              }
              showUnitDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen)
        ) {
          Text(text = if (isAr) "حفظ" else "Save")
        }
      },
      dismissButton = {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          if (editingUnit != null && onDeleteUnit != null) {
            Button(
              onClick = {
                onDeleteUnit(editingUnit!!.id)
                showUnitDialog = false
              },
              colors = ButtonDefaults.buttonColors(containerColor = RedAlert)
            ) {
              Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text(text = if (isAr) "حذف" else "Delete")
            }
          }
          OutlinedButton(onClick = { showUnitDialog = false }) {
            Text(text = if (isAr) "إلغاء" else "Cancel")
          }
        }
      }
    )
  }
}
