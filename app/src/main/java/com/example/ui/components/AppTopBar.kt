package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen
import com.example.ui.theme.MintLight
import com.example.ui.viewmodel.ScreenTab

@Composable
fun AppTopBar(
  language: String,
  unreadCount: Int,
  searchQuery: String,
  onSearchChange: (String) -> Unit,
  onToggleLanguage: () -> Unit,
  onOpenNotifications: () -> Unit
) {
  var isSearchExpanded by remember { mutableStateOf(false) }
  val isAr = language == "ar"

  Surface(
    color = ForestGreen,
    shadowElevation = 4.dp
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .statusBarsPadding()
        .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // App Icon
        Box(
          modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.18f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.Shield,
            contentDescription = "Shield",
            tint = Color(0xFFFCD34D),
            modifier = Modifier.size(22.dp)
          )
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Title
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isAr) "إدارة الإيجارات" else "Rent Manager",
            style = MaterialTheme.typography.titleMedium,
            color = Color.White
          )
          Text(
            text = if (isAr) "إدارة العقود والأملاك الخاصة" else "Landlord Portfolio Management",
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFFA7F3D0)
          )
        }

        // Search button
        IconButton(
          onClick = { isSearchExpanded = !isSearchExpanded },
          modifier = Modifier.size(40.dp)
        ) {
          Icon(
            imageVector = if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search,
            contentDescription = "Search",
            tint = Color.White
          )
        }

        // Language toggle
        Surface(
          onClick = onToggleLanguage,
          shape = RoundedCornerShape(20.dp),
          color = Color.White.copy(alpha = 0.16f),
          border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
          modifier = Modifier.height(36.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Language,
              contentDescription = "Language",
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = if (isAr) "English" else "عربي",
              color = Color.White,
              style = MaterialTheme.typography.labelMedium
            )
          }
        }

        Spacer(modifier = Modifier.width(6.dp))

        // Notifications Bell with Badge
        IconButton(
          onClick = onOpenNotifications,
          modifier = Modifier.size(40.dp)
        ) {
          BadgedBox(
            badge = {
              if (unreadCount > 0) {
                Badge(
                  containerColor = Color(0xFFEF4444),
                  contentColor = Color.White
                ) {
                  Text(text = if (unreadCount > 9) "9+" else "$unreadCount", fontSize = 10.sp)
                }
              }
            }
          ) {
            Icon(
              imageVector = Icons.Default.Notifications,
              contentDescription = "Notifications",
              tint = Color.White
            )
          }
        }
      }

      // Expandable search field
      AnimatedVisibility(visible = isSearchExpanded) {
        Column(modifier = Modifier.padding(top = 10.dp)) {
          OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = {
              Text(
                text = if (isAr) "ابحث باسم المستأجر، رقم الوحدة، أو العقار..." else "Search tenant, unit, or property...",
                fontSize = 13.sp,
                color = Color.Gray
              )
            },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = Color.White,
              unfocusedContainerColor = Color.White,
              focusedBorderColor = MintLight,
              unfocusedBorderColor = Color.Transparent,
              focusedTextColor = Color(0xFF0F172A),
              unfocusedTextColor = Color(0xFF0F172A)
            ),
            trailingIcon = {
              if (searchQuery.isNotBlank()) {
                IconButton(onClick = { onSearchChange("") }) {
                  Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                }
              }
            },
            modifier = Modifier
              .fillMaxWidth()
              .height(50.dp)
          )
        }
      }
    }
  }
}
