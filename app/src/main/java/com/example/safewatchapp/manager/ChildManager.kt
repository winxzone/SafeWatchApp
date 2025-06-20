package com.example.safewatchapp.manager

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import com.example.safewatchapp.models.Child
import com.example.safewatchapp.retrofit.ApiClient
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import androidx.core.content.edit
import com.example.safewatchapp.models.ExpandedChildProfile
import retrofit2.HttpException

class ChildManager(private val context: Context? = null) {

    companion object {
        private const val PREFS_NAME = "SafeWatchCache"
        private const val CHILDREN_KEY = "cached_children"
        private const val CURRENT_CHILD_ID_KEY = "current_child_id"
        private const val TAG = "ChildManager"
        private const val PHOTO_CACHE_DIR = "child_photos"
    }

    private val photosDir: File by lazy {
        File(context?.filesDir, "child_photos").also { dir ->
            if (!dir.exists()) {
                val created = dir.mkdirs()
                Log.d(TAG, "Создание директории: ${dir.absolutePath}, успех: $created")
            }
            Log.d(TAG, "Photos directory инициализирована: ${dir.absolutePath}")
        }
    }

    private var childrenList: MutableList<Child> = mutableListOf()
    var currentChildId: String? = null
        set(value) {
            field = value
            saveCurrentChildId()
        }
    private val photoCache = ConcurrentHashMap<String, Bitmap>()
    private var dataChanged = false

    init {
        loadCachedData()
    }

    private fun loadCachedData() {
        context?.let {
            try {
                val sharedPrefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val childrenJson = sharedPrefs.getString(CHILDREN_KEY, null)

                if (!childrenJson.isNullOrEmpty()) {
                    val type = object : TypeToken<List<Child>>() {}.type
                    val cachedChildren = Gson().fromJson<List<Child>>(childrenJson, type)

                    if (cachedChildren.isNotEmpty()) {
                        childrenList.clear()
                        childrenList.addAll(cachedChildren)
                        Log.d(TAG, "Loaded ${childrenList.size} children from cache")
                    } else {
                        Log.d(TAG, "Cached children data is empty")
                    }
                } else {
                    Log.d(TAG, "No cached children data found")
                }

                currentChildId = sharedPrefs.getString(CURRENT_CHILD_ID_KEY, null)
                if (currentChildId == null || !childrenList.any { it.id == currentChildId }) {
                    currentChildId = childrenList.firstOrNull()?.id
                } else {
                    Log.d(TAG, "Error loading ChildId: $currentChildId")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading cached data: ${e.message}", e)
            }
        }
    }

    suspend fun loadExpandedChildProfile(
        childId: String,
        date: String,
        onSuccess: (ExpandedChildProfile) -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val response = ApiClient.childProfileApiService.getExpandedChildProfile(childId, date)
            Log.w("MainActivity", "!!!! $response")
            if (response.isSuccessful && response.body() != null) {
                onSuccess(response.body()!!)
            } else {
                onError("Ошибка загрузки профиля: ${response.message()}")
            }
        } catch (e: Exception) {
            onError("Ошибка запроса: ${e.message}")
        }
    }

    private fun saveChildrenToCache() {
        context?.let {
            try {
                val childrenJson = Gson().toJson(childrenList)
                val sharedPrefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                sharedPrefs.edit { putString(CHILDREN_KEY, childrenJson) }
                Log.d(TAG, "Saved ${childrenList.size} children to cache")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving children to cache: ${e.message}", e)
            }
        }
    }

    private fun saveCurrentChildId() {
        context?.let {
            val sharedPrefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit { putString(CURRENT_CHILD_ID_KEY, currentChildId) }
        }
    }

    private fun getPhotoDirectory(): File? {
        return context?.let {
            val photoDir = File(it.filesDir, PHOTO_CACHE_DIR)
            if (!photoDir.exists()) {
                photoDir.mkdirs()
            }
            photoDir
        }
    }

    private fun savePhotoToDisk(childId: String, bitmap: Bitmap) {
        try {
            val photoDir = photosDir
            // Директория уже должна существовать, но проверим на всякий случай
            if (!photoDir.exists()) {
                val created = photoDir.mkdirs()
                Log.d(TAG, "Создание директории при сохранении: ${photoDir.absolutePath}, успех: $created")
            }

            val photoFile = File(photoDir, "$childId.jpg")
            Log.d(TAG, "Сохранение фото в: ${photoFile.absolutePath}")

            FileOutputStream(photoFile).use { out ->
                val success = bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
                if (!success) {
                    Log.e(TAG, "Не удалось сохранить bitmap: compress вернул false")
                } else {
                    Log.d(TAG, "Bitmap успешно сжат и записан")
                }
            }

            // Проверка размера файла
            if (photoFile.length() == 0L) {
                Log.e(TAG, "Файл изображения сохранён, но он пустой!")
            } else {
                Log.d(TAG, "Photo saved to disk: ${photoFile.absolutePath}, size: ${photoFile.length()} bytes")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error saving photo to disk: ${e.message}", e)
        }
    }


    fun removeChildFromCache(childId: String) {
        val wasRemoved = childrenList.removeAll { it.id == childId }
        if (wasRemoved) {
            saveChildrenToCache()
            Log.d(TAG, "Removed child $childId from cache")
        } else {
            Log.w(TAG, "Attempted to remove child $childId, but not found in cache")
        }
    }

    fun clearAllCache() {
        childrenList.clear()
        photoCache.clear()
        currentChildId = null
        dataChanged = false

        context?.let {
            val sharedPrefs = it.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            sharedPrefs.edit { clear() }
            Log.d(TAG, "Cleared SharedPreferences cache")
        }
        clearPhotoFiles()
        Log.d(TAG, "All caches cleared successfully")
    }

    private fun clearPhotoFiles() {
        try {
            val photoDir = getPhotoDirectory()
            photoDir?.listFiles()?.forEach { file ->
                if (file.isFile) {
                    val deleted = file.delete()
                    Log.d(TAG, "Deleted photo file ${file.name}: $deleted")
                }
            }
            Log.d(TAG, "Cleared photo files from disk")
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing photo files: ${e.message}", e)
        }
    }

    suspend fun fetchChildren(
        forceRefresh: Boolean = false,
        onSuccess: (List<Child>) -> Unit,
        onError: (String) -> Unit
    ) {
        if (forceRefresh || childrenList.isEmpty() || dataChanged) {
            try {
                Log.d(TAG, "Fetching children from server")
                val newList = ApiClient.childProfileApiService.getAllChildren()
                childrenList.clear()
                childrenList.addAll(newList)
                dataChanged = false
                saveChildrenToCache()

                // Check if currentChildId is still valid
                if (currentChildId == null || !childrenList.any { it.id == currentChildId }) {
                    currentChildId = childrenList.firstOrNull()?.id
                }

                onSuccess(childrenList)
            } catch (e: Exception) {
                if (childrenList.isNotEmpty()) {
                    Log.d(TAG, "Network error, using cached data: ${e.message}")
                    onSuccess(childrenList)
                } else {
                    Log.e(TAG, "Error fetching children with no cache available: ${e.message}", e)
                    onError("Error: ${e.message}")
                }
            }
        } else {
            Log.d(TAG, "Using cached children data")
            onSuccess(childrenList)
        }
    }

    suspend fun getChildProfilePhoto(childId: String): Bitmap? {
        Log.d(TAG, "=== Загрузка фото для childId: $childId ===")

        // Проверка кэша
        photoCache[childId]?.let {
            Log.d(TAG, "Фото найдено в памяти (кэше)")
            return it
        }

        // Используем кэшированную директорию
        val photoDir = photosDir
        Log.d(TAG, "photoDir: ${photoDir.absolutePath}")

        val photoFile = File(photoDir, "$childId.jpg")
        Log.d(TAG, "Ожидаемый путь к фото: ${photoFile.absolutePath}")

        if (photoFile.exists()) {
            Log.d(TAG, "Файл существует. Размер: ${photoFile.length()} байт")
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            if (bitmap != null) {
                Log.d(TAG, "Bitmap успешно декодирован с диска")
                photoCache[childId] = bitmap
                return bitmap
            } else {
                Log.e(TAG, "Bitmap = null после декодирования файла")
            }
        } else {
            Log.e(TAG, "Файл НЕ существует по пути: ${photoFile.absolutePath}")
        }

        // Попытка загрузить с сервера
        return try {
            Log.d(TAG, "Фото не найдено локально, пробуем загрузить с API")
            val responseBody = ApiClient.childProfileApiService.downloadChildPhoto(childId)
            val inputStream = responseBody.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            if (bitmap != null) {
                Log.d(TAG, "Bitmap успешно загружен из сети")
                photoCache[childId] = bitmap
                savePhotoToDisk(childId, bitmap)
            } else {
                Log.e(TAG, "Bitmap = null после загрузки из сети")
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при загрузке фото с API: ${e.message}", e)
            null
        }
    }

    fun updatePhotoInCache(childId: String, photoFile: File) {
        try {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            photoCache[childId] = bitmap
            savePhotoToDisk(childId, bitmap)
            Log.d(TAG, "Updated photo in cache for child: $childId")
        } catch (e: Exception) {
            Log.e(TAG, "Error updating photo in cache", e)
        }
    }

    private fun updateChildNameInCache(childId: String, newName: String) {
        val childIndex = childrenList.indexOfFirst { it.id == childId }
        if (childIndex != -1) {
            val updatedChild = childrenList[childIndex].copy(name = newName)
            childrenList[childIndex] = updatedChild
            dataChanged = true
            saveChildrenToCache()
            Log.d(TAG, "Updated name in cache for child: $childId")
        }
    }

    fun getCurrentChild(): Child? {
        return currentChildId?.let { id -> childrenList.find { it.id == id } } ?: childrenList.firstOrNull()
    }

    fun switchToNextChild() {
        if (childrenList.isNotEmpty()) {
            val currentIndex = childrenList.indexOfFirst { it.id == currentChildId }
            val nextIndex = if (currentIndex == -1) 0 else (currentIndex + 1) % childrenList.size
            currentChildId = childrenList[nextIndex].id
        }
    }

    fun switchToPreviousChild() {
        if (childrenList.isNotEmpty()) {
            val currentIndex = childrenList.indexOfFirst { it.id == currentChildId }
            val prevIndex = if (currentIndex == -1) childrenList.size - 1 else (currentIndex - 1 + childrenList.size) % childrenList.size
            currentChildId = childrenList[prevIndex].id
        }
    }

    suspend fun updateChildName(
        childId: String,
        newName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val body = mapOf("newName" to newName)
        try {
            Log.d(TAG, "Sending request to update name: childId=$childId, newName=$newName")
            val response = ApiClient.childProfileApiService.updateChildName(childId, body)
            Log.d(TAG, "Server response: $response")
            updateChildNameInCache(childId, newName)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating name: ${e.message}", e)
            onError("Error: ${e.message}")
        }
    }

    suspend fun updateChildPhoto(
        childId: String,
        photoFile: File,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isImageValid(photoFile)) {
            onError("Invalid image")
            return
        }

        // Определяем правильный MIME-тип на основе расширения файла
        val mimeType = when (photoFile.extension.lowercase()) {
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "webp" -> "image/webp"
            else -> "image/jpeg"
        }

        val requestFile = photoFile.asRequestBody(mimeType.toMediaTypeOrNull())

        // ВАЖНО: сервер ожидает поле с именем "photo", а не "file"
        val filePart = MultipartBody.Part.createFormData("photo", photoFile.name, requestFile)

        try {
            Log.d(TAG, "Sending request to update photo:")
            Log.d(TAG, "- childId: $childId")
            Log.d(TAG, "- fileName: ${photoFile.name}")
            Log.d(TAG, "- fileSize: ${photoFile.length()} bytes")
            Log.d(TAG, "- mimeType: $mimeType")

            val response = ApiClient.childProfileApiService.updateChildPhoto(childId, filePart)
            Log.d(TAG, "Photo upload successful")

            updatePhotoInCache(childId, photoFile)
            onSuccess()
        } catch (e: HttpException) {
            Log.e(TAG, "HTTP Error ${e.code()}: ${e.message()}")
            try {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e(TAG, "Error response body: $errorBody")
                onError("Upload failed: HTTP ${e.code()} - $errorBody")
            } catch (ex: Exception) {
                onError("Upload failed: HTTP ${e.code()}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating photo: ${e.message}", e)
            onError("Error: ${e.message}")
        }
    }

    // Улучшенная валидация изображения
    private fun isImageValid(photoFile: File): Boolean {
        if (!photoFile.exists()) {
            Log.e(TAG, "File does not exist: ${photoFile.absolutePath}")
            return false
        }

        if (photoFile.length() == 0L) {
            Log.e(TAG, "File is empty: ${photoFile.name}")
            return false
        }

        if (photoFile.length() > 10 * 1024 * 1024) { // 10MB лимит
            Log.e(TAG, "File too large: ${photoFile.length()} bytes")
            return false
        }

        val allowedExtensions = listOf("jpg", "jpeg", "png", "webp")
        val extension = photoFile.extension.lowercase()

        if (extension !in allowedExtensions) {
            Log.e(TAG, "Unsupported file type: $extension")
            return false
        }

        Log.d(TAG, "File validation passed: ${photoFile.name}")
        return true
    }
}