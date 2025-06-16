package com.montilivi.esdeveniments.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayOutputStream

object ImageUtils {

    // comprimeix la imatge si passa de 500Kb
    fun compressImageFromUri(context: Context, uri: Uri, maxWidth: Int = 1024, quality: Int = 80, sizeThresholdKb: Int = 500): ByteArray {
        val fileSizeKb = context.contentResolver.openFileDescriptor(uri, "r")?.statSize?.div(1024)
        if (fileSizeKb != null && fileSizeKb <= sizeThresholdKb) {
            val inputStream = context.contentResolver.openInputStream(uri)
            val originalBytes = inputStream?.readBytes() ?: ByteArray(0)
            inputStream?.close()
            return originalBytes
        }

        val inputStream = context.contentResolver.openInputStream(uri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        val aspectRatio = originalBitmap.height.toDouble() / originalBitmap.width.toDouble()
        val targetHeight = (maxWidth * aspectRatio).toInt()

        val resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, maxWidth, targetHeight, true)

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        return outputStream.toByteArray()
    }
}
