package com.example.ui.components

import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintLight
import com.example.ui.theme.TextMuted
import com.example.ui.viewmodel.ScreenTab

@Composable
fun AppBottomNav(
  currentTab: ScreenTab,
  onSelectTab: (ScreenTab) -> Unit,
  language: String,
  unreadCount: Int
) {
  val isAr = language == "ar"

  NavigationBar(
    containerColor = Color.White,
    tonalElevation = 8.dp,
    modifier = Modifier.navigationBarsPadding()
  ) {
    // 1. Dashboard
    NavigationBarItem(
      selected = currentTab == ScreenTab.DASHBOARD,
      onClick = { onSelectTab(ScreenTab.DASHBOARD) },
      icon = {
        Icon(imageVector = Icons.Default.Dashboard, contentDescription = "Dashboard")
      },
      label = {
        Text(text = if (isAr) "الرئيسية" else "Dashboard", fontSize = 11.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )

    // 2. Properties
    NavigationBarItem(
      selected = currentTab == ScreenTab.PROPERTIES,
      onClick = { onSelectTab(ScreenTab.PROPERTIES) },
      icon = {
        Icon(imageVector = Icons.Default.Apartment, contentDescription = "Properties")
      },
      label = {
        Text(text = if (isAr) "العقارات" else "Properties", fontSize = 10.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )

    // 3. Tenants (المستأجرون)
    NavigationBarItem(
      selected = currentTab == ScreenTab.TENANTS,
      onClick = { onSelectTab(ScreenTab.TENANTS) },
      icon = {
        Icon(imageVector = androidx.compose.material.icons.Icons.Default.People, contentDescription = "Tenants")
      },
      label = {
        Text(text = if (isAr) "المستأجرون" else "Tenants", fontSize = 10.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )

    // 4. Payments
    NavigationBarItem(
      selected = currentTab == ScreenTab.PAYMENTS,
      onClick = { onSelectTab(ScreenTab.PAYMENTS) },
      icon = {
        Icon(imageVector = Icons.Default.Receipt, contentDescription = "Payments")
      },
      label = {
        Text(text = if (isAr) "التحصيل" else "Payments", fontSize = 11.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )

    // 4. Notifications
    NavigationBarItem(
      selected = currentTab == ScreenTab.NOTIFICATIONS,
      onClick = { onSelectTab(ScreenTab.NOTIFICATIONS) },
      icon = {
        BadgedBox(
          badge = {
            if (unreadCount > 0) {
              Badge(containerColor = Color(0xFFEF4444)) {
                Text(text = "$unreadCount", fontSize = 9.sp)
              }
            }
          }
        ) {
          Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Alerts")
        }
      },
      label = {
        Text(text = if (isAr) "التنبيهات" else "Alerts", fontSize = 11.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )

    // 5. Settings
    NavigationBarItem(
      selected = currentTab == ScreenTab.SETTINGS,
      onClick = { onSelectTab(ScreenTab.SETTINGS) },
      icon = {
        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
      },
      label = {
        Text(text = if (isAr) "الإعدادات" else "Settings", fontSize = 11.sp)
      },
      colors = NavigationBarItemDefaults.colors(
        selectedIconColor = ForestGreen,
        selectedTextColor = ForestGreen,
        indicatorColor = MintLight,
        unselectedIconColor = TextMuted,
        unselectedTextColor = TextMuted
      )
    )
  }
}
