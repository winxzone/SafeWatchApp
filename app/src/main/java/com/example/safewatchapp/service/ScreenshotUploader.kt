package com.example.safewatchapp.service

import android.content.Context
import android.util.Log
import com.example.safewatchapp.retrofit.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody

class ScreenshotUploader(
    private val context: Context
) {
    private val TAG = "ScreenshotUploader"

    suspend fun uploadAllScreenshots(childDeviceId: String) = withContext(Dispatchers.IO) {
        val cacheDir = context.cacheDir
        val files = cacheDir.listFiles()?.filter { it.name.endsWith(".png") } ?: emptyList()

        if (files.isEmpty()) {
            Log.d(TAG, "Нет скриншотов для отправки.")
            return@withContext
        }

        val parts = files.mapIndexed { index, file ->
            val requestFile = file.asRequestBody("image/png".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("files", file.name, requestFile)
        }

        try {
            val response = ApiClient.screenshotApiService.uploadScreenshotsBatch(childDeviceId, parts)
            if (response.isSuccessful) {
                Log.d(TAG, "Скриншоты успешно отправлены. Удаляем локальные копии...")
                files.forEach { it.delete() }
            } else {
                Log.e(TAG, "Ошибка при отправке: ${response.code()} - ${response.message()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке скриншотов", e)
        }
    }
}