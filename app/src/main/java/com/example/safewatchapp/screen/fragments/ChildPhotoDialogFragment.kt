package com.example.safewatchapp.screen.fragments

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DialogPhotoViewBinding
import com.example.safewatchapp.manager.ChildManager
import com.example.safewatchapp.utils.TokenManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.lang.ref.WeakReference

// todo: доступ к фото отклонено(по хорошему изменить функционал этого окна )
class ChildPhotoDialogFragment : DialogFragment() {

    private var _binding: DialogPhotoViewBinding? = null
    private val binding get() = _binding!!

    private var childId: String? = null

    private lateinit var childManager: ChildManager
    private var currentPhotoFile: File? = null
    
    // Флаг для отслеживания, была ли фотография изменена
    private var photoChanged = false

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Разрешение на доступ к фото отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                val file = uriToFile(requireContext(), uri)
                if (file != null && file.exists() && file.length() > 0) {
                    currentPhotoFile = file
                    photoChanged = true
                    Log.d("ChildPhotoDialogFragment", "Файл успешно создан: ${file.absolutePath}")
                    binding.childPhotoDialogImageView.setImageURI(Uri.fromFile(file))
                } else {
                    Log.e("ChildPhotoDialogFragment", "Ошибка: файл пустой или не создан")
                }
            }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.FullScreenDialogStyle)
        childId = arguments?.getString("childId")
        childManager = ChildManager()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogPhotoViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childId?.let { id ->
            // Загружаем текущее фото ребенка
            loadChildPhoto(id)
        }

        binding.changePhotoButton.setOnClickListener {
            checkPermissionAndOpenGallery()
        }

        binding.confirmChangePhotoButton.setOnClickListener {
            updateChildPhoto()
        }
    }
    
    private fun loadChildPhoto(childId: String) {
        val token = TokenManager.getToken(requireContext())
        if (token != null) {
            // Проверяем, есть ли фото в кэше
            val cachedPhoto = childManager.getCachedPhoto(childId)
            if (cachedPhoto != null) {
                binding.childPhotoDialogImageView.setImageBitmap(cachedPhoto)
            } else {
                // Если нет в кэше, загружаем с сервера
                viewLifecycleOwner.lifecycleScope.launch {
                    try {
                        childManager.getChildProfilePhoto(childId, onSuccess = { bitmap ->
                            binding.childPhotoDialogImageView.setImageBitmap(bitmap)
                        },
                            onError = { error ->
                                Toast.makeText(requireContext(), error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("ChildPhotoDialogFragment", "Error loading photo", e)
                        Toast.makeText(requireContext(), "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun updateChildPhoto() {
        if (!photoChanged || currentPhotoFile == null) {
            dismiss()
            return
        }
        
        val token = TokenManager.getToken(requireContext())
        val id = childId

        Log.d("ChildPhotoDialogFragment", "Нажата кнопка подтверждения фото. File: $currentPhotoFile")
        Log.d("ChildPhotoDialogFragment","$token, $id, $currentPhotoFile")
        
        if (token != null && id != null && currentPhotoFile != null && currentPhotoFile!!.exists()) {
            Log.d("ChildPhotoDialogFragment", "Файл найден, отправляем в updateChildPhoto")
            
            // Используем слабую ссылку на фрагмент
            val fragmentRef = WeakReference(this)
            
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    childManager.updateChildPhoto(
                        id,
                        currentPhotoFile!!,
                        onSuccess = {
                            fragmentRef.get()?.let { fragment ->
                                Log.d("ChildPhotoDialogFragment", "Фото успешно обновлено")
                                Toast.makeText(fragment.requireContext(), "Фото обновлено", Toast.LENGTH_SHORT).show()

                                // Передаем результат обратно в MainActivity
                                fragment.parentFragmentManager.setFragmentResult(
                                    "childPhotoUpdated",
                                    bundleOf("photoPath" to currentPhotoFile!!.absolutePath)
                                )

                                fragment.dismiss()
                            }
                        },
                        onError = { error ->
                            fragmentRef.get()?.let { fragment ->
                                Log.e("ChildPhotoDialogFragment", "Ошибка при обновлении: $error")
                                Toast.makeText(fragment.requireContext(), "Ошибка: $error", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                } catch (e: Exception) {
                    fragmentRef.get()?.let { fragment ->
                        Log.e("ChildPhotoDialogFragment", "Error updating photo", e)
                        Toast.makeText(fragment.requireContext(), "Error updating photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            Log.e("ChildPhotoDialogFragment", "Файл изображения отсутствует или недоступен!")
            Toast.makeText(requireContext(), "Файл изображения отсутствует или недоступен!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissionAndOpenGallery() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        if (ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED) {
            openGallery()
        } else {
            requestPermissionLauncher.launch(permission)
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun uriToFile(context: Context, uri: Uri): File? {
        Log.d("ChildPhotoDialogFragment", "Начало преобразования URI в файл: $uri")
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = getFileName(context, uri)
        val file = File(context.cacheDir, fileName)
        Log.d("ChildPhotoDialogFragment", "Файл будет сохранен как: ${file.absolutePath}")

        return try {
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 0) {
                Log.d("ChildPhotoDialogFragment", "Файл успешно сохранён: ${file.absolutePath}")
                file
            } else {
                Log.e("ChildPhotoDialogFragment", "Файл не был создан или пустой")
                null
            }
        } catch (e: Exception) {
            Log.e("ChildPhotoDialogFragment", "Ошибка копирования файла: ${e.message}")
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "selected_child_photo.jpg" // Дефолтное имя
        val cursor = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val nameIndex = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1) {
                    name = it.getString(nameIndex)
                }
            }
        }
        return name
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Очищаем временные файлы
        currentPhotoFile?.let {
            if (it.exists() && it.isFile) {
                it.delete()
            }
        }
    }
}