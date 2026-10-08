package com.example.platform

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.example.domain.UpiData
import com.example.domain.UpiParser
import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.PlanarYUVLuminanceSource
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.nio.ByteBuffer

class QrScannerManager {

    private val reader = MultiFormatReader()

    fun createAnalyzer(onQrCodeDetected: (UpiData) -> Unit): ImageAnalysis.Analyzer {
        return QrImageAnalyzer(reader, onQrCodeDetected)
    }

    fun decodeFromUri(context: Context, uri: Uri): UpiData? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream) ?: return null
            decodeFromBitmap(bitmap)
        } catch (e: Exception) {
            null
        }
    }

    fun decodeFromBitmap(bitmap: Bitmap): UpiData? {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        val source = RGBLuminanceSource(width, height, pixels)
        val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

        return try {
            val result = reader.decodeWithState(binaryBitmap)
            UpiParser.parse(result.text)
        } catch (e: Exception) {
            null
        } finally {
            reader.reset()
        }
    }

    private class QrImageAnalyzer(
        private val reader: MultiFormatReader,
        private val onQrCodeDetected: (UpiData) -> Unit
    ) : ImageAnalysis.Analyzer {

        private var isProcessing = false

        override fun analyze(imageProxy: ImageProxy) {
            if (isProcessing) {
                imageProxy.close()
                return
            }

            val buffer = imageProxy.planes[0].buffer
            val data = toByteArray(buffer)
            val width = imageProxy.width
            val height = imageProxy.height

            val source = PlanarYUVLuminanceSource(
                data,
                width,
                height,
                0,
                0,
                width,
                height,
                false
            )
            val binaryBitmap = BinaryBitmap(HybridBinarizer(source))

            try {
                val result = reader.decodeWithState(binaryBitmap)
                val upiData = UpiParser.parse(result.text)
                if (upiData != null) {
                    isProcessing = true
                    onQrCodeDetected(upiData)
                }
            } catch (e: NotFoundException) {
                // No QR in current frame, normal
            } catch (e: Exception) {
                // Ignored
            } finally {
                reader.reset()
                imageProxy.close()
            }
        }

        private fun toByteArray(buffer: ByteBuffer): ByteArray {
            buffer.rewind()
            val data = ByteArray(buffer.remaining())
            buffer.get(data)
            return data
        }
    }
}
