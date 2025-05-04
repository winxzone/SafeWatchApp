package com.example.safewatchapp.service

import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.safewatchapp.R
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import androidx.core.graphics.createBitmap

class ScreenCaptureService : Service() {

    private val CHANNEL_ID = "screen_capture_channel"
    private val NOTIFICATION_ID = 1

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.d("ScreenCaptureService", "Сервис создан")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ScreenCaptureService", "onStartCommand вызван")

        val resultCode = intent?.getIntExtra("code", Activity.RESULT_CANCELED) ?: return START_NOT_STICKY
        val resultData = intent.getParcelableExtra<Intent>("data") ?: return START_NOT_STICKY

        startScreenCapture(resultCode, resultData)

        return START_STICKY
    }

    private fun saveBitmapToCache(bitmap: Bitmap): File? {
        return try {
            val fileName = "screenshot_${System.currentTimeMillis()}.png"
            val file = File(cacheDir, fileName)
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            Log.d("ScreenCaptureService", "Скриншот сохранён во временный файл: ${file.absolutePath}")
            file
        } catch (e: IOException) {
            Log.e("ScreenCaptureService", "Ошибка при сохранении скриншота", e)
            null
        }
    }


    private fun startScreenCapture(resultCode: Int, data: Intent) {
        Log.d("ScreenCaptureService", "Старт захвата экрана")

        val metrics = resources.displayMetrics

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val dpi = metrics.densityDpi

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)

        val projectionManager =
            getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                super.onStop()
                Log.d("ScreenCaptureService", "MediaProjection остановлен системой")
                stopSelf()
            }
        }, null)

        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            width,
            height,
            dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface,
            null,
            null
        )

        Log.d("ScreenCaptureService", "VirtualDisplay создан")

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener

            val planes = image.planes
            val buffer: ByteBuffer = planes[0].buffer
            val pixelStride = planes[0].pixelStride
            val rowStride = planes[0].rowStride
            val rowPadding = rowStride - pixelStride * width
            val bitmapWidth = width + rowPadding / pixelStride

            Log.d("ScreenCaptureService", "Снимок экрана получен: ширина=$bitmapWidth, высота=$height")

            // Создаем Bitmap из Image
            val bitmap = createBitmap(bitmapWidth, height)
            bitmap.copyPixelsFromBuffer(buffer)

            // Обрезаем лишние паддинги
            val croppedBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height)

            // Сохраняем
            val savedFile = saveBitmapToCache(croppedBitmap)
            if (savedFile != null) {
                Log.d("ScreenCaptureService", "Файл успешно сохранён: ${savedFile.absolutePath}")
            }

            image.close()
            stopSelf() // Пока делаем 1 скриншот

        }, Handler(Looper.getMainLooper()))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("ScreenCaptureService", "Сервис уничтожен")
        virtualDisplay?.release()
        imageReader?.close()
        mediaProjection?.stop()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Анализ эмоций активен")
            .setContentText("Приложение анализирует экран")
            .setSmallIcon(R.drawable.ic_notification)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Захват экрана"
            val descriptionText = "Фоновая работа анализа экрана"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}