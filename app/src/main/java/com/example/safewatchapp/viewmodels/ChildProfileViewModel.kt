package com.example.safewatchapp.viewmodels

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.utils.TokenManager
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import com.example.safewatchapp.utils.Result

class ChildProfileViewModel : ViewModel() {

    private val _childCreationStatus = MutableLiveData<Result<String>>()
    val childCreationStatus: LiveData<Result<String>> = _childCreationStatus

    fun createChildProfile(name: String, imageUri: Uri?, context: Context, deviceId: String) {
        val token = TokenManager.getToken(context) ?: return

        viewModelScope.launch {
            try {
                val deviceIdRequestBody = deviceId.toRequestBody("text/plain".toMediaTypeOrNull())
                val nameRequestBody = name.toRequestBody("text/plain".toMediaTypeOrNull())
                val photoPart = imageUri?.let {
                    val file = getFileFromUri(it, context) ?: throw Exception("Не удалось получить файл фото")
                    Log.d("ChildProfileViewModel", "Photo file: ${file.absolutePath}, Size: ${file.length()} bytes")
                    MultipartBody.Part.createFormData("photo", file.name, file.asRequestBody("image/*".toMediaTypeOrNull()))
                }

                Log.d("ChildProfileViewModel", "Sending request: deviceId=$deviceId, name=$name, photo=${photoPart != null}")
                val response = ApiClient.deviceLinkApiService.linkDeviceToChild("Bearer $token", deviceIdRequestBody, nameRequestBody, photoPart)
                if (response.isSuccessful) {
                    val childId = response.body()?.get("childId") ?: throw Exception("Нет ID ребёнка")
                    Log.d("ChildProfileViewModel", "Success: childId=$childId")
                    _childCreationStatus.postValue(Result.Success("Профиль создан и устройство привязано"))
                } else {
                    Log.e("ChildProfileViewModel", "Request failed: ${response.code()} - ${response.errorBody()?.string()}")
                    _childCreationStatus.postValue(Result.Error("Ошибка сервера: ${response.errorBody()?.string()}"))
                }
            } catch (e: Exception) {
                Log.e("ChildProfileViewModel", "Exception: ${e.localizedMessage}")
                _childCreationStatus.postValue(Result.Error("Ошибка: ${e.localizedMessage}"))
            }
        }
    }

    private fun getFileFromUri(uri: Uri, context: Context): File? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            if (inputStream == null) {
                Log.e("ChildProfileViewModel", "InputStream is null for URI: $uri")
                return null
            }
            val file = File(context.cacheDir, "upload_${System.currentTimeMillis()}.jpg")
            file.outputStream().use { outputStream ->
                inputStream.use { it.copyTo(outputStream) }
            }
            file
        } catch (e: Exception) {
            Log.e("ChildProfileViewModel", "Error converting URI to file: ${e.message}")
            null
        }
    }
}