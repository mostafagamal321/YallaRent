package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

  private const val TARGET_MAX_WIDTH = 800
  private const val JPEG_QUALITY = 82

  /**
   * Scales a Bitmap so its width does not exceed TARGET_MAX_WIDTH (~800px),
   * preserving the aspect ratio, and saves it as a JPEG in internal app storage.
   */
  fun saveCompressedBitmap(
    context: Context,
    originalBitmap: Bitmap,
    prefix: String = "tenant"
  ): String {
    val dir = File(context.filesDir, "tenant_photos")
    if (!dir.exists()) {
      dir.mkdirs()
    }

    val width = originalBitmap.width
    val height = originalBitmap.height

    val scaledBitmap = if (width > TARGET_MAX_WIDTH) {
      val targetHeight = (height.toFloat() * (TARGET_MAX_WIDTH.toFloat() / width.toFloat())).toInt().coerceAtLeast(1)
      Bitmap.createScaledBitmap(originalBitmap, TARGET_MAX_WIDTH, targetHeight, true)
    } else {
      originalBitmap
    }

    val fileName = "${prefix}_${System.currentTimeMillis()}_${(1000..9999).random()}.jpg"
    val file = File(dir, fileName)

    FileOutputStream(file).use { out ->
      scaledBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
      out.flush()
    }

    return file.absolutePath
  }

  /**
   * Loads a Bitmap from a Content Uri, compresses it to ~800px max width,
   * and saves it to internal app storage.
   */
  fun saveCompressedImageFromUri(
    context: Context,
    uri: Uri,
    prefix: String = "tenant"
  ): String? {
    return try {
      val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
          decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
          decoder.isMutableRequired = true
          val originalWidth = info.size.width
          if (originalWidth > TARGET_MAX_WIDTH) {
            val targetHeight = (info.size.height.toFloat() * (TARGET_MAX_WIDTH.toFloat() / originalWidth.toFloat())).toInt()
            decoder.setTargetSize(TARGET_MAX_WIDTH, targetHeight.coerceAtLeast(1))
          }
        }
      } else {
        context.contentResolver.openInputStream(uri)?.use { stream ->
          BitmapFactory.decodeStream(stream)
        }
      }

      if (bitmap != null) {
        saveCompressedBitmap(context, bitmap, prefix)
      } else {
        null
      }
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Encodes a file to Base64 string for backup.
   */
  fun fileToBase64(filePath: String?): String? {
    if (filePath.isNullOrBlank()) return null
    val file = File(filePath)
    if (!file.exists()) return null
    return try {
      val bytes = file.readBytes()
      Base64.encodeToString(bytes, Base64.NO_WRAP)
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Decodes a Base64 string and writes it to internal app storage, returning the absolute path.
   */
  fun base64ToFile(context: Context, base64Str: String, fileName: String): String? {
    if (base64Str.isBlank()) return null
    return try {
      val dir = File(context.filesDir, "tenant_photos")
      if (!dir.exists()) {
        dir.mkdirs()
      }
      val file = File(dir, fileName)
      val bytes = Base64.decode(base64Str, Base64.NO_WRAP)
      FileOutputStream(file).use { out ->
        out.write(bytes)
        out.flush()
      }
      file.absolutePath
    } catch (e: Exception) {
      e.printStackTrace()
      null
    }
  }

  /**
   * Formats a phone number for direct WhatsApp intent or dialer.
   * Handles Egyptian numbers (010, 011, 012, 015) converting to international +201...
   */
  fun formatWhatsAppNumber(phone: String): String {
    val digits = phone.filter { it.isDigit() }
    return when {
      digits.startsWith("0020") -> digits.removePrefix("00")
      digits.startsWith("20") -> digits
      digits.startsWith("01") -> "20" + digits.removePrefix("0")
      digits.length == 10 && digits.startsWith("1") -> "20$digits"
      else -> digits
    }
  }

  /**
   * Partially masks national ID number, showing e.g. ••••••••••1234
   */
  fun maskNationalId(nationalId: String, isArabic: Boolean = true): String {
    if (nationalId.length < 4) return nationalId
    val lastFour = nationalId.takeLast(4)
    val maskedPrefix = "••••••••••"
    val formattedLastFour = if (isArabic) Formatters.toArabicIndicDigits(lastFour) else lastFour
    return "$maskedPrefix$formattedLastFour"
  }
}
