package com.example.safewatchapp.service

import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.User
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.models.UserRegistration
import com.example.safewatchapp.models.ChildDevice
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Регистрация пользователя
    @POST("/user/register")
    fun registerUser(@Body user: UserRegistration): Call<User>

    // Авторизация пользователя
    @POST("/user/login")
    fun loginUser(@Body userLogin: UserLogin): Call<TokenResponse>

    // Получение профиля пользователя
    // TODO: Добавить функцию получения профиля пользователя
    // TODO: Перепроверить работу JWT токена на клиентской части
//    @GET("/user/{id}")
//    fun getUser(
//        @Path("id") userId: String,
//        @Header("Authorization") token: String
//    ): Call<User>

    @GET("/child-device/list")
    fun listChildDevice(
        @Header("Authorization") token: String
    ): Call<List<ChildDevice>>

    // Регистрация устройства ребенка
    @POST("/child-device/register")
    fun registerChildDevice(
        @Body childDevice: ChildDevice,
        @Header("Authorization") token: String
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
}