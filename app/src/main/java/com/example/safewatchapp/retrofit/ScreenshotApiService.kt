package com.example.safewatchapp.retrofit

import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface ScreenshotApiService {
    @Multipart
    @POST("screenshot/upload/{childDeviceId}")
    suspend fun uploadScreenshot(
        @Path("childDeviceId") childDeviceId: String,
        @Part file: MultipartBody.Part
    ): Response<Void>

    // todo: разработать функционал отправки скриншотов на сервер и ScreenMonitoringService
    @Multipart
    @POST("screenshot/batch/{childDeviceId}")
    suspend fun uploadScreenshotsBatch(
        @Path("childDeviceId") childDeviceId: String,
        @Part files: List<MultipartBody.Part>
    ): Response<Map<String, List<String>>>
}