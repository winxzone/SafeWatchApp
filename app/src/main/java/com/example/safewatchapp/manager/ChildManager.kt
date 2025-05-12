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

class ChildManager(private val context: Context? = null) {

    companion object {
        private const val PREFS_NAME = "SafeWatchCache"
        private const val CHILDREN_KEY = "cached_children"
        private const val CURRENT_CHILD_ID_KEY = "current_child_id"
        private const val TAG = "ChildManager"
        private const val PHOTO_CACHE_DIR = "child_photos"
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

                // Load currentChildId
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
            val photoDir = getPhotoDirectory() ?: return
            val photoFile = File(photoDir, "$childId.jpg")

            FileOutputStream(photoFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
            Log.d(TAG, "Photo saved to disk for child: $childId")
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

    suspend fun getChildProfilePhoto(
        childId: String,
        onSuccess: (Bitmap) -> Unit,
        onError: (String) -> Unit
    ) {
        photoCache[childId]?.let {
            onSuccess(it)
            return
        }

        val photoDir = getPhotoDirectory() ?: run {
            onError("Photo directory not available")
            return
        }
        val photoFile = File(photoDir, "$childId.jpg")
        if (photoFile.exists()) {
            val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
            if (bitmap != null) {
                photoCache[childId] = bitmap
                onSuccess(bitmap)
                return
            }
        }

        try {
            Log.d(TAG, "Downloading photo for child: $childId")
            val responseBody = ApiClient.childProfileApiService.downloadChildPhoto(childId)
            val inputStream = responseBody.byteStream()
            val bitmap = BitmapFactory.decodeStream(inputStream)
            photoCache[childId] = bitmap
            savePhotoToDisk(childId, bitmap)
            onSuccess(bitmap)
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading photo: ${e.message}", e)
            onError("Error: ${e.message}")
        }
    }

    fun getCachedPhoto(childId: String): Bitmap? {
        return photoCache[childId]
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

        val requestFile = photoFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
        val filePart = MultipartBody.Part.createFormData("file", photoFile.name, requestFile)

        try {
            Log.d(TAG, "Sending request to update photo: childId=$childId")
            ApiClient.childProfileApiService.updateChildPhoto(childId, filePart)
            updatePhotoInCache(childId, photoFile)
            onSuccess()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating photo: ${e.message}", e)
            onError("Error: ${e.message}")
        }
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
}