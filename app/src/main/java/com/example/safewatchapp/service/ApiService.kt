package com.example.safewatchapp.service

import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.User
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.models.UserRegistration
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    // Регистрация пользователя
    @POST("/users/register")
    fun registerUser(@Body user: UserRegistration): Call<User>

    // Авторизация пользователя
    @POST("/users/login")
    fun loginUser(@Body userLogin: UserLogin): Call<TokenResponse>

    // Получение профиля пользователя
    @GET("/users/{id}")
    fun getUser(
        @Path("id") userId: String,
        @Header("Authorization") token: String
    ): Call<User>
}