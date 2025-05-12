package com.example.safewatchapp.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import com.example.safewatchapp.utils.Result

class CreateChildProfileViewModel : ViewModel() {

    private val _childCreationStatus = MutableLiveData<Result<String>>() // Возвращаем только childId
    val childCreationStatus: LiveData<Result<String>> = _childCreationStatus

    fun createChildProfile(name: String, imageUri: Uri?, context: Context, deviceId: String) {
        TokenManager.getToken(context) ?: return

        viewModelScope.launch {
            try {
                val deviceIdBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val nameBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = imageUri?.let { createMultipartFromUri(it, context) }

                val response = ApiClient.deviceLinkApiService.linkDeviceToChild(
                    deviceIdBody, nameBody, photoPart
                )

                if (response.isSuccessful) {
                    val childId = response.body()?.get("childId")
                    if (!childId.isNullOrEmpty()) {
                        _childCreationStatus.postValue(Result.Success(childId))
                    } else {
                        _childCreationStatus.postValue(Result.Error("Сервер не вернул childId"))
                    }
                } else {
                    val error = response.errorBody()?.string() ?: "Неизвестная ошибка"
                    _childCreationStatus.postValue(Result.Error("Ошибка сервера: $error"))
                }

            } catch (e: Exception) {
                _childCreationStatus.postValue(Result.Error("Ошибка: ${e.localizedMessage}"))
            }
        }
    }

    private fun createMultipartFromUri(uri: Uri, context: Context): MultipartBody.Part? {
        return try {
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            context.contentResolver.openInputStream(uri)?.use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
            MultipartBody.Part.createFormData(
                "photo",
                file.name,
                file.asRequestBody("image/*".toMediaTypeOrNull())
            )
        } catch (e: Exception) {
            Log.e("CreateChildProfileVM", "Ошибка при обработке фото: ${e.message}")
            null
        }
    }
}
