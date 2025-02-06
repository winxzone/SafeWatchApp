package com.example.safewatchapp.service

import com.example.safewatchapp.models.Child
import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.User
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.models.UserRegistration
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.models.Notification
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path

interface ApiService {

    // Регистрация пользователя
    @POST("/user/register")
    fun registerUser(@Body user: UserRegistration): Call<User>

    // Авторизация пользователя
    @POST("/user/login")
    fun loginUser(@Body userLogin: UserLogin): Call<TokenResponse>

    // Получение профиля пользователя
    @GET("/user/profile")
    fun getUserProfile(
        @Header("Authorization") authHeader: String
    ): Call<User>

    @GET("/child-device/list")
    fun listChildDevice(
        @Header("Authorization") authHeader: String
    ): Call<List<ChildDevice>>

    // Регистрация устройства ребенка
    @POST("/child-device/register")
    fun registerChildDevice(
        @Body childDevice: ChildDevice,
        @Header("Authorization") authHeader: String
    ): Call<ChildDevice>

    // Подтверждение устройства ребенка
    @POST("child-device/confirm/{deviceId}")
    fun confirmChildDevice(
        @Header("Authorization") authHeader: String,
        @Path("deviceId") deviceId: String
    ): Call<Void>

    @POST("child-device/cancel/{deviceId}")
    fun cancelChildDevice(
        @Header("Authorization") authHeader: String,
        @Path("deviceId") deviceId: String
    ): Call<Void>

    @PUT("child/{childId}/profile/change-name")
    fun updateChildName(
        @Header("Authorization") authHeader: String,
        @Path("childId") childId: String,
        @Body newName: Map<String, String>
    ): Call<Void>

    @PUT("child/{childId}/profile/photo/upload")
    @Multipart
    fun updateChildPhoto(
        @Path("childId") childId: String,
        @Header("Authorization") authHeader: String,
        @Part file: MultipartBody.Part
    ): Call<ResponseBody>

    @GET("child/{childId}/profile/photo")
    fun downloadChildPhoto(
        @Path("childId") childId: String,
        @Header("Authorization") authHeader: String
    ): Call<ResponseBody>

    @GET("child/all")
    fun getAllChildren(
        @Header("Authorization") authHeader: String
    ): Call<List<Child>>

    @GET("notification/list")
    fun getNotifications(
        @Header("Authorization") authHeader: String
    ): Call<List<Notification>>

    @POST("notification/create")
    fun createNotification(
        @Header("Authorization") authHeader: String,
        @Body notification: Notification
    ): Call<Notification>

    @DELETE("notification/delete/{notificationId}")
    fun deleteNotification(
        @Header("Authorization") authHeader: String,
        @Path("notificationId") notificationId: String
    ): Call<Void>
}