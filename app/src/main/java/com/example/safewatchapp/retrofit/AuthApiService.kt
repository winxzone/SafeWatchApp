package com.example.safewatchapp.retrofit

import com.example.safewatchapp.models.User
import com.example.safewatchapp.models.TokenResponse
import com.example.safewatchapp.models.UserLogin
import com.example.safewatchapp.models.UserRegistration
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface AuthApiService {
    @POST("/user/register")
    suspend fun registerUser(@Body user: UserRegistration): Response<User>

    @POST("/user/login")
    suspend fun loginUser(@Body userLogin: UserLogin): Response<TokenResponse>

    @GET("/user/profile")
    suspend fun getUserProfile(
    ): Response<User>
}