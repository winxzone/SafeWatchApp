package com.example.safewatchapp.retrofit

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.DELETE
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface DeviceLinkApiService {
    @Multipart
    @POST("child-device/link")
    suspend fun linkDeviceToChild(
        @Part("childDeviceId") childDeviceId: RequestBody,
        @Part("name") name: RequestBody,
        @Part photo: MultipartBody.Part?
    ): Response<Map<String, String>>

    @DELETE("child-device/{deviceId}/with-child")
    suspend fun deleteDeviceAndChild(
        @Path("childDeviceId") childDeviceId: String
    ): Response<Unit>
}