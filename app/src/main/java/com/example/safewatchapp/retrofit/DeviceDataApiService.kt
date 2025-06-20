package com.example.safewatchapp.retrofit

import com.example.safewatchapp.models.DeviceDataPayload
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface DeviceDataApiService {

    @POST("device-data")
    suspend fun uploadDeviceData(
        @Body data: DeviceDataPayload
    ): Response<Unit>
}
