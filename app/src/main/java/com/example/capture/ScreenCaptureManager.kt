package com.example.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.util.Log

class ScreenCaptureManager(
    private val mediaProjection: MediaProjection,
    private val width: Int,
    private val height: Int,
    private val densityDpi: Int
) {
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null

    init {
        setupVirtualDisplay()
    }

    private fun setupVirtualDisplay() {
        try {
            // Use clamped dimension to prevent excessive memory usage
            val captureWidth = width.coerceIn(360, 1080)
            val captureHeight = height.coerceIn(640, 2400)

            imageReader = ImageReader.newInstance(
                captureWidth,
                captureHeight,
                PixelFormat.RGBA_8888,
                2
            )

            virtualDisplay = mediaProjection.createVirtualDisplay(
                "TradingAnalyzerDisplay",
                captureWidth,
                captureHeight,
                densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader?.surface,
                null,
                null
            )
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Error setting up VirtualDisplay: ${e.message}", e)
        }
    }

    /**
     * Captures a single frame as a Bitmap. Safe in memory.
     */
    fun captureFrame(): Bitmap? {
        val reader = imageReader ?: return null
        var image: android.media.Image? = null
        return try {
            image = reader.acquireLatestImage()
            if (image == null) return null

            val planes = image.planes
            val buffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * image.width

            val bitmap = Bitmap.createBitmap(
                image.width + rowPadding / pixelStride,
                image.height,
                Bitmap.Config.ARGB_8888
            )
            bitmap.copyPixelsFromBuffer(buffer)

            // Crop out padding if needed
            if (rowPadding == 0) {
                bitmap
            } else {
                val cleanBitmap = Bitmap.createBitmap(bitmap, 0, 0, image.width, image.height)
                bitmap.recycle()
                cleanBitmap
            }
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Failed to capture frame: ${e.message}")
            null
        } finally {
            image?.close()
        }
    }

    fun release() {
        try {
            virtualDisplay?.release()
            virtualDisplay = null
            imageReader?.close()
            imageReader = null
            mediaProjection.stop()
        } catch (e: Exception) {
            Log.e("ScreenCaptureManager", "Error releasing resources: ${e.message}")
        }
    }
}
