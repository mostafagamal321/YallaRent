package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.ForestGreen

@Composable
fun PinLockDialog(
  language: String,
  onUnlock: (String) -> Boolean
) {
  var pin by remember { mutableStateOf("") }
  var isError by remember { mutableStateOf(false) }
  val isAr = language == "ar"

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(ForestGreen)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Box(
        modifier = Modifier
          .size(72.dp)
          .clip(CircleShape)
          .background(Color.White.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = "Lock",
          tint = Color(0xFFFCD34D),
          modifier = Modifier.size(36.dp)
        )
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = if (isAr) "أهلاً بك في إدارة الإيجارات" else "Welcome to Rent Manager",
        style = MaterialTheme.typography.titleLarge,
        color = Color.White
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = if (isAr) "الرجاء إدخال رمز المرور (PIN) للمتابعة" else "Enter your 4-digit PIN to continue",
        style = MaterialTheme.typography.bodyMedium,
        color = Color(0xFFA7F3D0)
      )

      Spacer(modifier = Modifier.height(28.dp))

      // 4 PIN Dots
      Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        for (i in 0 until 4) {
          val isFilled = pin.length > i
          val dotColor by animateColorAsState(
            targetValue = when {
              isFilled && isError -> Color(0xFFEF4444)
              isFilled -> Color(0xFFFCD34D)
              else -> Color.White.copy(alpha = 0.25f)
            },
            animationSpec = tween(150),
            label = "dotColor"
          )

          Box(
            modifier = Modifier
              .size(18.dp)
              .clip(CircleShape)
              .background(dotColor)
              .border(2.dp, Color.White.copy(alpha = 0.4f), CircleShape)
          )
        }
      }

      if (isError) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = if (isAr) "رمز المرور غير صحيح، حاول مرة أخرى" else "Incorrect PIN, please try again",
          color = Color(0xFFFCA5A5),
          style = MaterialTheme.typography.labelMedium
        )
      }

      Spacer(modifier = Modifier.height(36.dp))

      // Numpad 1-9, 0, Backspace
      val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "del")
      )

      Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        for (row in digits) {
          Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            for (item in row) {
              if (item.isEmpty()) {
                Spacer(modifier = Modifier.size(72.dp))
              } else if (item == "del") {
                Box(
                  modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.12f))
                    .clickable {
                      if (pin.isNotEmpty()) {
                        pin = pin.dropLast(1)
                        isError = false
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Icon(
                    imageVector = Icons.Default.Backspace,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                  )
                }
              } else {
                Box(
                  modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.16f))
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .clickable {
                      if (pin.length < 4) {
                        val newPin = pin + item
                        pin = newPin
                        isError = false
                        if (newPin.length == 4) {
                          val ok = onUnlock(newPin)
                          if (!ok) {
                            isError = true
                            pin = ""
                          }
                        }
                      }
                    },
                  contentAlignment = Alignment.Center
                ) {
                  Text(
                    text = item,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
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
