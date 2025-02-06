package com.example.safewatchapp.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.safewatchapp.models.Child
import com.example.safewatchapp.service.ApiClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.ResponseBody
import java.io.File

class ChildManager {

    private var childrenList: List<Child> = listOf()
    private var currentChildIndex = 0

    fun fetchChildren(token: String, onSuccess: (List<Child>) -> Unit, onError: (String) -> Unit) {
        ApiClient.apiService.getAllChildren("Bearer $token")
            .enqueue(object : Callback<List<Child>> {
                override fun onResponse(call: Call<List<Child>>, response: Response<List<Child>>) {
                    if (response.isSuccessful) {
                        childrenList = response.body() ?: listOf()
                        onSuccess(childrenList)
                    } else {
                        onError("Failed to fetch children")
                    }
                }

                override fun onFailure(call: Call<List<Child>>, t: Throwable) {
                    onError("Error: ${t.message}")
                }
            })
    }

    fun getChildProfilePhoto(childId: String, token: String, onSuccess: (Bitmap) -> Unit, onError: (String) -> Unit) {
        ApiClient.apiService.downloadChildPhoto(childId, "Bearer $token")
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        response.body()?.let { body ->
                            val inputStream = body.byteStream()
                            val bitmap = BitmapFactory.decodeStream(inputStream)
                            onSuccess(bitmap)
                        } ?: onError("No photo found")
                    } else {
                        onError("Failed to fetch photo")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onError("Error: ${t.message}")
                }
            })
    }


    fun getCurrentChild(): Child? {
        return if (childrenList.isNotEmpty()) childrenList[currentChildIndex] else null
    }

    fun switchToNextChild() {
        if (childrenList.isNotEmpty()) {
            currentChildIndex = (currentChildIndex + 1) % childrenList.size
        }
    }

    fun switchToPreviousChild() {
        if (childrenList.isNotEmpty()) {
            currentChildIndex = (currentChildIndex - 1 + childrenList.size) % childrenList.size
        }
    }

    fun updateChildName(token: String, childId: String, newName: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val body = mapOf("newName" to newName)
        ApiClient.apiService.updateChildName("Bearer $token", childId, body)
            .enqueue(object : Callback<Void> {
                override fun onResponse(call: Call<Void>, response: Response<Void>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        onError("Failed to update name")
                    }
                }

                override fun onFailure(call: Call<Void>, t: Throwable) {
                    onError("Error: ${t.message}")
                }
            })
    }

    fun updateChildPhoto(token: String, childId: String, photoFile: File, onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isImageValid(photoFile)) {
            onError("Invalid image")
            return
        }

        val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)

        ApiClient.apiService.updateChildPhoto(childId, "Bearer $token", filePart)
            .enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.isSuccessful) {
                        onSuccess()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        Log.e("ChildPhotoDialogFragment", "Ошибка при обновлении фото: ${response.code()} - $errorBody")
                        onError("Failed to update photo: ${response.code()} - $errorBody")
                    }
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    onError("Error: ${t.message}")
                }
            })
    }

    private fun isImageValid(file: File): Boolean {
        return isValidImage(file) && isFileSizeValid(file)
    }

    private fun isValidImage(file: File): Boolean {
        val validExtensions = listOf("jpg", "jpeg", "png", "webp")
        val extension = file.extension.lowercase()
        return validExtensions.contains(extension)
    }

    private fun isFileSizeValid(file: File): Boolean {
        val maxFileSize = 5 * 1024 * 1024 // 5 MB
        return file.length() <= maxFileSize
    }

    private fun isImageResolutionValid(file: File): Boolean {
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(file.absolutePath, options)
        val maxWidth = 2000
        val maxHeight = 2000
        return options.outWidth <= maxWidth && options.outHeight <= maxHeight
    }
}