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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Property
import com.example.data.model.Tenant
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.GreenDark
import com.example.ui.theme.GreenLight
import com.example.ui.theme.MintLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.util.Formatters
import java.io.File

@Composable
fun TenantsScreen(
  tenants: List<Tenant>,
  contracts: List<Contract>,
  units: List<UnitItem>,
  properties: List<Property>,
  searchQuery: String,
  language: String,
  onOpenTenantProfile: (Tenant) -> Unit,
  onAddNewTenant: () -> Unit
) {
  val context = LocalContext.current
  val isAr = language == "ar"

  var localSearch by remember { mutableStateOf("") }
  var selectedBuildingId by remember { mutableStateOf<String?>("all") }
  var selectedStatusFilter by remember { mutableStateOf("all") } // "all", "active", "expiring_soon", "inactive"

  val effectiveSearch = if (searchQuery.isNotBlank()) searchQuery else localSearch

  // Pre-calculate contracts per tenant
  val contractsByTenantId = contracts.groupBy { it.tenantId.ifBlank { it.tenantName.trim() } }

  // Filter tenants
  val filteredTenants = tenants.filter { tenant ->
    val tenantContracts = contractsByTenantId[tenant.id] ?: contractsByTenantId[tenant.fullName.trim()] ?: emptyList()
    val activeContract = tenantContracts.firstOrNull { !it.isHistory && it.status == ContractStatus.ACTIVE }
      ?: tenantContracts.firstOrNull { !it.isHistory }

    val activeUnit = activeContract?.let { c -> units.find { it.id == c.unitId } }
    val activeProp = activeUnit?.let { u -> properties.find { it.id == u.propertyId } }

    // 1. Building filter
    val matchesBuilding = when (selectedBuildingId) {
      "all", null -> true
      else -> activeProp?.id == selectedBuildingId
    }

    // 2. Status filter
    val matchesStatus = when (selectedStatusFilter) {
      "all" -> true
      "active" -> {
        if (activeContract == null) false
        else {
          val daysLeft = Formatters.getDaysFromToday(activeContract.endDate)
          daysLeft > 60
        }
      }
      "expiring_soon" -> {
        if (activeContract == null) false
        else {
          val daysLeft = Formatters.getDaysFromToday(activeContract.endDate)
          daysLeft in 0..60
        }
      }
      "inactive" -> {
        activeContract == null || Formatters.getDaysFromToday(activeContract.endDate) < 0
      }
      else -> true
    }

    // 3. Search query filter (search by name, national ID, phone, building, unit)
    val matchesSearch = if (effectiveSearch.isBlank()) true else {
      val q = effectiveSearch.trim().lowercase()
      val nameMatch = tenant.fullName.lowercase().contains(q)
      val idMatch = tenant.nationalId.contains(q)
      val phoneMatch = tenant.phone.contains(q)
      val propMatch = activeProp?.name?.lowercase()?.contains(q) == true
      val unitMatch = activeUnit?.unitNumber?.lowercase()?.contains(q) == true
      nameMatch || idMatch || phoneMatch || propMatch || unitMatch
    }

    matchesBuilding && matchesStatus && matchesSearch
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // Header & Add Tenant Button
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.People, contentDescription = null, tint = ForestGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isAr) "دليل المستأجرين" else "Tenants Directory",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.Bold,
              color = Color(0xFF0F172A)
            )
          }
          Text(
            text = "${filteredTenants.size} ${if (isAr) "مستأجرين مسجلين" else "tenants registered"}",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF64748B)
          )
        }

        Button(
          onClick = onAddNewTenant,
          colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
          shape = RoundedCornerShape(8.dp),
          modifier = Modifier.testTag("add_tenant_header_button")
        ) {
          Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text(text = if (isAr) "إضافة مستأجر" else "Add Tenant", fontSize = 12.sp)
        }
      }
    }

    // In-Screen Search Bar (if top bar search is empty)
    if (searchQuery.isBlank()) {
      item {
        OutlinedTextField(
          value = localSearch,
          onValueChange = { localSearch = it },
          placeholder = { Text(if (isAr) "بحث بالاسم، الرقم القومي، الهاتف، العمارة، أو الشقة..." else "Search by name, ID, phone, building, unit...") },
          leadingIcon = {
            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = ForestGreen)
          },
          singleLine = true,
          shape = RoundedCornerShape(10.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedBorderColor = ForestGreen,
            unfocusedBorderColor = Color(0xFFCBD5E1)
          ),
          modifier = Modifier.fillMaxWidth().testTag("tenants_search_input")
        )
      }
    }

    // Filter Chips: Building & Status
    item {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Status filter chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          FilterChip(
            selected = selectedStatusFilter == "all",
            onClick = { selectedStatusFilter = "all" },
            label = { Text(if (isAr) "الكل" else "All") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ForestGreen,
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedStatusFilter == "active",
            onClick = { selectedStatusFilter = "active" },
            label = { Text(if (isAr) "عقود سارية" else "Active Contracts") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = ForestGreen,
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedStatusFilter == "expiring_soon",
            onClick = { selectedStatusFilter = "expiring_soon" },
            label = { Text(if (isAr) "تنتهي قريباً" else "Expiring Soon") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFFEA580C),
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = selectedStatusFilter == "inactive",
            onClick = { selectedStatusFilter = "inactive" },
            label = { Text(if (isAr) "بدون عقد ساري" else "No Active Contract") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = Color(0xFF64748B),
              selectedLabelColor = Color.White
            )
          )
        }

        // Building filter chips
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Icon(imageVector = Icons.Default.Apartment, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))

          FilterChip(
            selected = selectedBuildingId == "all",
            onClick = { selectedBuildingId = "all" },
            label = { Text(if (isAr) "جميع العقارات" else "All Buildings") },
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = MintLight,
              selectedLabelColor = ForestGreen
            )
          )

          for (prop in properties) {
            FilterChip(
              selected = selectedBuildingId == prop.id,
              onClick = { selectedBuildingId = prop.id },
              label = { Text(prop.name) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = MintLight,
                selectedLabelColor = ForestGreen
              )
            )
          }
        }
      }
    }

    // Empty State
    if (filteredTenants.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
        ) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Icon(
              imageVector = Icons.Default.People,
              contentDescription = null,
              tint = if (tenants.isEmpty()) ForestGreen else Color(0xFFCBD5E1),
              modifier = Modifier.size(54.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = if (tenants.isEmpty()) (if (isAr) "لا يوجد مستأجرون مسجلون بعد" else "No tenants registered yet")
              else (if (isAr) "لا يوجد مستأجرون يطابقون شروط البحث" else "No tenants match current criteria"),
              style = MaterialTheme.typography.titleMedium,
              color = Color(0xFF0F172A)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
              text = if (tenants.isEmpty()) (if (isAr) "سجّل بيانات المستأجرين وبطاقات الهوية وأرقام الهواتف لربطهم بالعقود وتتبع دفعاتهم." else "Add tenants with contact details and IDs to link to leases.")
              else (if (isAr) "جرب البحث بالاسم أو تغيير تصنيف المبنى أو الحالة." else "Try searching by name or altering filters."),
              style = MaterialTheme.typography.bodySmall,
              color = Color(0xFF64748B),
              textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (tenants.isEmpty()) {
              Spacer(modifier = Modifier.height(16.dp))
              Button(
                onClick = onAddNewTenant,
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(10.dp)
              ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = if (isAr) "إضافة مستأجر جديد" else "Add New Tenant")
              }
            }
          }
        }
      }
    } else {
      // List of Tenants
      items(filteredTenants, key = { it.id }) { tenant ->
        val tenantContracts = contractsByTenantId[tenant.id] ?: contractsByTenantId[tenant.fullName.trim()] ?: emptyList()
        val activeContract = tenantContracts.firstOrNull { !it.isHistory && it.status == ContractStatus.ACTIVE }
          ?: tenantContracts.firstOrNull { !it.isHistory }

        val activeUnit = activeContract?.let { c -> units.find { it.id == c.unitId } }
        val activeProp = activeUnit?.let { u -> properties.find { it.id == u.propertyId } }

        val daysLeft = activeContract?.let { Formatters.getDaysFromToday(it.endDate) } ?: -1
        val (badgeLabel, badgeBg, badgeColor) = when {
          activeContract == null -> Triple(if (isAr) "بدون عقد" else "No Contract", Color(0xFFF1F5F9), Color(0xFF64748B))
          daysLeft < 0 -> Triple(if (isAr) "منتهي" else "Expired", RedLight, RedAlert)
          daysLeft <= 60 -> Triple(if (isAr) "ينتهي قريباً" else "Expiring Soon", Color(0xFFFFEDD5), Color(0xFFC2410C))
          else -> Triple(if (isAr) "ساري" else "Active", GreenLight, GreenDark)
        }

        Card(
          shape = RoundedCornerShape(12.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          elevation = CardDefaults.cardElevation(1.dp),
          modifier = Modifier
            .fillMaxWidth()
            .clickable { onOpenTenantProfile(tenant) }
            .testTag("tenant_card_${tenant.id}")
        ) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Round Photo Avatar
            Box(
              modifier = Modifier
                .size(54.dp)
                .clip(CircleShape)
                .background(Color(0xFFE2E8F0))
                .border(1.5.dp, ForestGreen, CircleShape),
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
                  modifier = Modifier.size(32.dp)
                )
              }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details
            Column(modifier = Modifier.weight(1f)) {
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = tenant.fullName,
                  style = MaterialTheme.typography.titleSmall,
                  fontWeight = FontWeight.Bold,
                  color = Color(0xFF0F172A)
                )

                Surface(shape = RoundedCornerShape(4.dp), color = badgeBg) {
                  Text(
                    text = badgeLabel,
                    color = badgeColor,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                  )
                }
              }

              Spacer(modifier = Modifier.height(3.dp))

              // Building & Unit
              Text(
                text = "${activeProp?.name ?: (if (isAr) "بدون عقار" else "No property")} • ${activeUnit?.let { if (isAr) "وحدة ${it.unitNumber}" else "Unit ${it.unitNumber}" } ?: ""}",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF475569)
              )

              Spacer(modifier = Modifier.height(2.dp))

              // Phone & Rent
              Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                if (tenant.phone.isNotBlank()) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Phone, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(12.dp))
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(text = tenant.phone, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
                  }
                } else {
                  Spacer(modifier = Modifier.width(1.dp))
                }

                if (activeContract != null) {
                  Text(
                    text = "${Formatters.formatCurrency(activeContract.monthlyRent, language)} / ${if (isAr) "شهر" else "mo"}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = ForestGreen
                  )
                }
              }
            }

            Spacer(modifier = Modifier.width(6.dp))

            Icon(
              imageVector = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight,
              contentDescription = null,
              tint = Color(0xFFCBD5E1),
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }
    }
  }
}
