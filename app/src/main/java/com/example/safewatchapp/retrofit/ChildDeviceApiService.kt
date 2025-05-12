package com.example.safewatchapp.retrofit

import com.example.safewatchapp.models.ChildDevice
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ChildDeviceApiService {
    @GET("/child-device/list")
    suspend fun listChildDevice(
    ): List<ChildDevice>

    @POST("/child-device/register")
    suspend fun registerChildDevice(
        @Body childDevice: ChildDevice,
    ): Response<ChildDevice>

    @POST("child-device/confirm/{deviceId}")
    suspend fun confirmChildDevice(
        @Path("childDeviceId") childDeviceId: String
    ): Response<Unit>

    @DELETE("child-device/cancel/{deviceId}")
    suspend fun cancelChildDevice(
        @Path("childDeviceId") childDeviceId: String
    ): Response<Unit>
}