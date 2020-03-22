package edu.uci.ics.charm.tabellion

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection

/*
Created Date: 05/31/2019
Created By: Myles Liu
Last Modified: 05/31/2019
Last Modified By: Myles Liu
Notes:

 */

class ScreenCapture(private val context: Context) : ImageReader.OnImageAvailableListener {

    // This class's credit is given to http://3jigen.net/2018/05/post-720/

    private var display: VirtualDisplay? = null
    private var onCaptureListener: ((Bitmap) -> Unit)? = null

    fun run(mediaProjection: MediaProjection, onCaptureListener: (Bitmap) -> Unit) {
        this.onCaptureListener = onCaptureListener
        if (display == null) {
            display = createDisplay(mediaProjection)
        }
    }

    private fun createDisplay(mediaProjection: MediaProjection): VirtualDisplay {
        context.resources.displayMetrics.run {
            val maxImages = 2
            val reader = ImageReader.newInstance(
                    widthPixels, heightPixels, PixelFormat.RGBA_8888, maxImages)
            reader.setOnImageAvailableListener(this@ScreenCapture, null)
            return mediaProjection.createVirtualDisplay(
                    "Capture Display", widthPixels, heightPixels, densityDpi,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    reader.surface, null, null)
        }
    }

    override fun onImageAvailable(reader: ImageReader) {
        if (display != null) {
            onCaptureListener?.invoke(captureImage(reader))
        }
    }

    private fun captureImage(reader: ImageReader): Bitmap {
        val image = reader.acquireLatestImage()
        context.resources.displayMetrics.run {
            image.planes[0].run {
                val bitmap = Bitmap.createBitmap(
                        rowStride / pixelStride, heightPixels, Bitmap.Config.ARGB_8888)
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()
                return bitmap
            }
        }
    }

    fun stop() {
        display?.release()
        display = null
        onCaptureListener = null
    }
}