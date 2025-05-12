package com.example.safewatchapp.retrofit

import com.example.safewatchapp.models.Notification
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface NotificationApiService {
    @GET("notification/list")
    suspend fun getNotifications(
    ): List<Notification>

    @POST("notification/create")
    suspend fun createNotification(
        @Body notification: Notification
    ): Notification

    @DELETE("notification/delete/{notificationId}")
    suspend fun deleteNotification(
        @Path("notificationId") notificationId: String
    ): Response<Unit>
}