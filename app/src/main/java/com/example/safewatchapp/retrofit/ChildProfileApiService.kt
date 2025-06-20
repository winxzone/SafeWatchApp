package com.example.safewatchapp.retrofit

import com.example.safewatchapp.models.Child
import com.example.safewatchapp.models.ExpandedChildProfile
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface ChildProfileApiService {

    @GET("child/all")
    suspend fun getAllChildren(
    ): List<Child>

    @PUT("child/{childId}/profile/change-name")
    suspend fun updateChildName(
        @Path("childId") childId: String,
        @Body newName: Map<String, String>
    ): Map<String, String>

    @PUT("child/{childId}/profile/photo/upload")
    @Multipart
    suspend fun updateChildPhoto(
        @Path("childId") childId: String,
        @Part photo: MultipartBody.Part
    ): ResponseBody

    @GET("child/{childId}/profile/photo")
    suspend fun downloadChildPhoto(
        @Path("childId") childId: String,
    ): ResponseBody

    @GET("/child/{childId}/expanded-profile")
    suspend fun getExpandedChildProfile(@Path("childId") childId: String, @Query("date") date: String): Response<ExpandedChildProfile>

}