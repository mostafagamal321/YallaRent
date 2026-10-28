package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Assessment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Contract
import com.example.data.model.ContractStatus
import com.example.data.model.Payment
import com.example.data.model.PaymentStatus
import com.example.data.model.Property
import com.example.data.model.Tenant
import com.example.data.model.UnitItem
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintDark
import com.example.ui.theme.MintLight
import com.example.ui.theme.RedAlert
import com.example.ui.theme.RedLight
import com.example.ui.viewmodel.DashboardStats
import com.example.ui.viewmodel.ScreenTab
import com.example.util.Formatters

@Composable
fun DashboardScreen(
  stats: DashboardStats,
  properties: List<Property>,
  units: List<UnitItem>,
  contracts: List<Contract>,
  payments: List<Payment>,
  language: String,
  tenants: List<Tenant> = emptyList(),
  onNavigateTab: (ScreenTab) -> Unit,
  onOpenContractDialog: (unitId: String?, contract: Contract?) -> Unit,
  onOpenPaymentDialog: (paymentId: String) -> Unit,
  onOpenUnitDetail: (unitId: String) -> Unit,
  onOpenTenantProfile: ((Tenant) -> Unit)? = null
) {
  val isAr = language == "ar"
  val chevronIcon = if (isAr) Icons.Default.ChevronLeft else Icons.Default.ChevronRight

  val propMap = properties.associateBy { it.id }
  val unitMap = units.associateBy { it.id }
  val contractMap = contracts.associateBy { it.id }

  // 1. Rent due next 7 days
  val upcomingPayments = payments.filter {
    it.status == PaymentStatus.PENDING && Formatters.getDaysFromToday(it.dueDate) in 0..7
  }

  // 2. Overdue payments
  val overduePayments = payments.filter { it.status == PaymentStatus.OVERDUE }

  // 3. Contracts ending in next 60 days
  val expiringContracts = contracts.filter {
    !it.isHistory && it.status != ContractStatus.EXPIRED && Formatters.getDaysFromToday(it.endDate) in 0..60
  }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp, vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    if (properties.isEmpty()) {
      item {
        Card(
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = Color.White),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFBBF7D0)),
          elevation = CardDefaults.cardElevation(2.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(42.dp)
                  .clip(RoundedCornerShape(10.dp))
                  .background(MintLight),
                contentAlignment = Alignment.Center
              ) {
                Icon(
                  imageVector = Icons.Default.Apartment,
                  contentDescription = null,
                  tint = ForestGreen,
                  modifier = Modifier.size(24.dp)
                )
              }
              Spacer(modifier = Modifier.width(10.dp))
              Column {
                Text(
                  text = if (isAr) "مرحباً بك في إدارة الأملاك والإيجارات" else "Welcome to Rent Manager",
                  style = MaterialTheme.typography.titleMedium,
                  color = ForestGreen
                )
                Text(
                  text = if (isAr) "قاعدة بياناتك فارغة تماماً وجاهزة لإدخال بياناتك الحقيقية." else "Your database is clean and ready for your data.",
                  style = MaterialTheme.typography.bodySmall,
                  color = Color(0xFF64748B)
                )
              }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              Button(
                onClick = { onNavigateTab(ScreenTab.PROPERTIES) },
                colors = ButtonDefaults.buttonColors(containerColor = ForestGreen),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(text = if (isAr) "+ أضف أول عقار" else "+ Add Property", fontSize = 12.sp)
              }
              Button(
                onClick = { onNavigateTab(ScreenTab.TENANTS) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Text(text = if (isAr) "+ أضف مستأجراً" else "+ Add Tenant", fontSize = 12.sp)
              }
            }
          }
        }
      }
    }

    // Top Overview Card
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
            Column {
              Text(
                text = if (isAr) "ملخص المحفظة العقارية" else "Portfolio Overview",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF1E293B)
              )
              Text(
                text = if (isAr) "متابعة سريعة للإيجارات والوحدات والعقود" else "Real-time rent tracking & active leases",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF64748B)
              )
            }

            Surface(
              onClick = { onNavigateTab(ScreenTab.REPORTS) },
              shape = RoundedCornerShape(10.dp),
              color = Color(0xFFECFDF5),
              border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFA7F3D0)),
              modifier = Modifier.height(36.dp)
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 10.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Assessment,
                  contentDescription = "Reports",
                  tint = Color(0xFF065F46),
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = if (isAr) "التقارير" else "Reports",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color(0xFF065F46)
                )
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // 2x2 Grid of primary metrics + Banner
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Active Contracts
            MetricBox(
              modifier = Modifier
                .weight(1f)
                .clickable { onNavigateTab(ScreenTab.PROPERTIES) },
              title = if (isAr) "العقود السارية" else "Active Leases",
              value = "${stats.activeContractsCount}",
              icon = Icons.Default.Description,
              iconColor = Color(0xFF16A34A),
              bgColor = Color(0xFFF0FDF4),
              borderColor = Color(0xFFBBF7D0)
            )

            // Vacant Units
            MetricBox(
              modifier = Modifier
                .weight(1f)
                .clickable { onNavigateTab(ScreenTab.PROPERTIES) },
              title = if (isAr) "الوحدات الشاغرة" else "Vacant Units",
              value = "${stats.vacantUnitsCount}",
              icon = Icons.Default.Apartment,
              iconColor = Color(0xFF64748B),
              bgColor = Color(0xFFF8FAFC),
              borderColor = Color(0xFFE2E8F0)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Expected monthly rent full card
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(Color(0xFFEFF6FF))
              .border(1.dp, Color(0xFFBFDBFE), RoundedCornerShape(12.dp))
              .padding(14.dp)
          ) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Icon(
                    imageVector = Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color(0xFF2563EB),
                    modifier = Modifier.size(18.dp)
                  )
                  Spacer(modifier = Modifier.width(6.dp))
                  Text(
                    text = if (isAr) "إجمالي الإيجار الشهري المتوقع" else "Expected Monthly Rent",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF1E40AF)
                  )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                  text = Formatters.formatCurrency(stats.expectedMonthlyRent, language),
                  style = MaterialTheme.typography.titleLarge,
                  color = Color(0xFF1D4ED8)
                )
              }

              Surface(
                onClick = { onOpenContractDialog(null, null) },
                shape = RoundedCornerShape(10.dp),
                color = ForestGreen
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                ) {
                  Icon(imageVector = Icons.Default.AddCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text(text = if (isAr) "عقد جديد" else "New Lease", color = Color.White, style = MaterialTheme.typography.labelSmall)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Collected this month
            MetricBox(
              modifier = Modifier
                .weight(1f)
                .clickable { onNavigateTab(ScreenTab.PAYMENTS) },
              title = if (isAr) "المحصل هذا الشهر" else "Collected (Month)",
              value = Formatters.formatCurrency(stats.rentCollectedThisMonth, language),
              icon = Icons.Default.CheckCircle,
              iconColor = Color(0xFF059669),
              bgColor = Color(0xFFECFDF5),
              borderColor = Color(0xFFA7F3D0)
            )

            // Overdue Total
            MetricBox(
              modifier = Modifier
                .weight(1f)
                .clickable { onNavigateTab(ScreenTab.PAYMENTS) },
              title = if (isAr) "المتأخرات" else "Total Overdue",
              value = Formatters.formatCurrency(stats.totalOverdueAmount, language),
              icon = Icons.Default.Warning,
              iconColor = RedAlert,
              bgColor = RedLight,
              borderColor = Color(0xFFFECACA)
            )
          }
        }
      }
    }

    // LIST 1: Rent Due in Next 7 Days
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isAr) "إيجارات مستحقة خلال ٧ أيام (${upcomingPayments.size})" else "Rent Due in Next 7 Days (${upcomingPayments.size})",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF1E293B)
          )
        }
        Text(
          text = if (isAr) "عرض الكل" else "View All",
          color = ForestGreen,
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.clickable { onNavigateTab(ScreenTab.PAYMENTS) }
        )
      }
    }

    if (upcomingPayments.isEmpty()) {
      item {
        EmptyListNotice(message = if (isAr) "لا توجد دفعات مستحقة خلال السبعة أيام القادمة." else "No payments due in the next 7 days.")
      }
    } else {
      items(upcomingPayments) { p ->
        val contract = contractMap[p.contractId]
        val unit = unitMap[p.unitId]
        val prop = unit?.let { propMap[it.propertyId] }
        val days = Formatters.getDaysFromToday(p.dueDate)

        DashboardActionItem(
          title = contract?.tenantName ?: (if (isAr) "مستأجر" else "Tenant"),
          subtitle = "${prop?.name ?: ""} • ${if (isAr) "وحدة ${unit?.unitNumber}" else "Unit ${unit?.unitNumber}"} • ${Formatters.formatUIDate(p.dueDate)}",
          badgeText = if (days == 0) (if (isAr) "اليوم" else "Today") else (if (isAr) "خلال $days أيام" else "in ${days}d"),
          badgeBg = Color(0xFFFFEDD5),
          badgeColor = Color(0xFFC2410C),
          amountText = Formatters.formatCurrency(p.amount, language),
          actionText = if (isAr) "سداد الآن" else "Pay Now",
          onClick = { onOpenPaymentDialog(p.id) },
          chevronIcon = chevronIcon
        )
      }
    }

    // LIST 2: Overdue Rent
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Warning, contentDescription = null, tint = RedAlert, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isAr) "إيجارات متأخرة (${overduePayments.size})" else "Overdue Rent (${overduePayments.size})",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF1E293B)
          )
        }
        Text(
          text = if (isAr) "عرض الكل" else "View All",
          color = RedAlert,
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.clickable { onNavigateTab(ScreenTab.PAYMENTS) }
        )
      }
    }

    if (overduePayments.isEmpty()) {
      item {
        EmptyListNotice(
          message = if (isAr) "ممتاز! لا توجد إيجارات متأخرة حالياً." else "Great! No overdue rent payments.",
          bgColor = Color(0xFFF0FDF4),
          textColor = Color(0xFF15803D)
        )
      }
    } else {
      items(overduePayments) { p ->
        val contract = contractMap[p.contractId]
        val unit = unitMap[p.unitId]
        val prop = unit?.let { propMap[it.propertyId] }
        val daysLate = Math.abs(Formatters.getDaysFromToday(p.dueDate))

        DashboardActionItem(
          title = contract?.tenantName ?: (if (isAr) "مستأجر" else "Tenant"),
          subtitle = "${prop?.name ?: ""} • ${if (isAr) "وحدة ${unit?.unitNumber}" else "Unit ${unit?.unitNumber}"} • ${Formatters.formatUIDate(p.dueDate)}",
          badgeText = if (isAr) "متأخر $daysLate يوم" else "${daysLate}d late",
          badgeBg = RedLight,
          badgeColor = RedAlert,
          amountText = Formatters.formatCurrency(p.amount, language),
          actionText = if (isAr) "تسجيل السداد" else "Mark Paid",
          onClick = { onOpenPaymentDialog(p.id) },
          chevronIcon = chevronIcon
        )
      }
    }

    // LIST 3: Contracts ending in next 60 days
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(imageVector = Icons.Default.Description, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = if (isAr) "عقود تنتهي خلال ٦٠ يوماً (${expiringContracts.size})" else "Contracts Ending in 60 Days (${expiringContracts.size})",
            style = MaterialTheme.typography.titleSmall,
            color = Color(0xFF1E293B)
          )
        }
        Text(
          text = if (isAr) "عرض الكل" else "View All",
          color = Color(0xFF0284C7),
          style = MaterialTheme.typography.labelMedium,
          modifier = Modifier.clickable { onNavigateTab(ScreenTab.PROPERTIES) }
        )
      }
    }

    if (expiringContracts.isEmpty()) {
      item {
        EmptyListNotice(message = if (isAr) "لا توجد عقود تنتهي خلال الستين يوماً القادمة." else "No contracts ending in the next 60 days.")
      }
    } else {
      items(expiringContracts) { c ->
        val unit = unitMap[c.unitId]
        val prop = unit?.let { propMap[it.propertyId] }
        val days = Formatters.getDaysFromToday(c.endDate)

        DashboardActionItem(
          title = c.tenantName,
          subtitle = "${prop?.name ?: ""} • ${if (isAr) "وحدة ${unit?.unitNumber}" else "Unit ${unit?.unitNumber}"} • ${if (isAr) "ينتهي:" else "Ends:"} ${Formatters.formatUIDate(c.endDate)}",
          badgeText = if (isAr) "متبقي $days يوم" else "${days}d left",
          badgeBg = Color(0xFFE0F2FE),
          badgeColor = Color(0xFF0284C7),
          amountText = Formatters.formatCurrency(c.monthlyRent, language),
          actionText = if (isAr) "تجديد العقد" else "Renew",
          onClick = { unit?.let { onOpenUnitDetail(it.id) } },
          chevronIcon = chevronIcon
        )
      }
    }

    item {
      Spacer(modifier = Modifier.height(30.dp))
    }
  }
}

@Composable
fun MetricBox(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  icon: androidx.compose.ui.graphics.vector.ImageVector,
  iconColor: Color,
  bgColor: Color,
  borderColor: Color
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(12.dp))
      .background(bgColor)
      .border(1.dp, borderColor, RoundedCornerShape(12.dp))
      .padding(12.dp)
  ) {
    Column {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = title, style = MaterialTheme.typography.labelSmall, color = Color(0xFF334155))
      }
      Spacer(modifier = Modifier.height(6.dp))
      Text(text = value, style = MaterialTheme.typography.titleMedium, color = Color(0xFF0F172A))
    }
  }
}

@Composable
fun DashboardActionItem(
  title: String,
  subtitle: String,
  badgeText: String,
  badgeBg: Color,
  badgeColor: Color,
  amountText: String,
  actionText: String,
  onClick: () -> Unit,
  chevronIcon: androidx.compose.ui.graphics.vector.ImageVector
) {
  Card(
    onClick = onClick,
    shape = RoundedCornerShape(12.dp),
    colors = CardDefaults.cardColors(containerColor = Color.White),
    elevation = CardDefaults.cardElevation(1.dp),
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(text = title, style = MaterialTheme.typography.titleSmall, color = Color(0xFF0F172A))
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = RoundedCornerShape(6.dp),
            color = badgeBg
          ) {
            Text(
              text = badgeText,
              style = MaterialTheme.typography.labelSmall,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = Color(0xFF64748B))
      }

      Column(horizontalAlignment = Alignment.End) {
        Text(text = amountText, style = MaterialTheme.typography.titleSmall, color = ForestGreen)
        Text(text = actionText, style = MaterialTheme.typography.labelSmall, color = Color(0xFF2563EB))
      }

      Spacer(modifier = Modifier.width(8.dp))
      Icon(imageVector = chevronIcon, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
    }
  }
}

@Composable
fun EmptyListNotice(
  message: String,
  bgColor: Color = Color.White,
  textColor: Color = Color(0xFF64748B)
) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(bgColor)
      .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(10.dp))
      .padding(14.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(text = message, style = MaterialTheme.typography.bodySmall, color = textColor)
  }
}
