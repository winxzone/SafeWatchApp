package com.example.safewatchapp.screen.fragments

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
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
import androidx.core.content.FileProvider
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DialogPhotoViewBinding
import com.example.safewatchapp.manager.ChildManager
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

// todo: доступ к фото отклонено(по хорошему изменить функционал этого окна )
// исправить
class ChildPhotoDialogFragment : DialogFragment() {

    private var _binding: DialogPhotoViewBinding? = null
    private val binding get() = _binding!!

    private var childId: String? = null
    private lateinit var childManager: ChildManager

    private var currentPhotoFile: File? = null
    private var photoChanged = false

    private var selectedImageUri: Uri? = null

    // 📷 Камера: разрешение
    private val requestCameraPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            takePhoto()
        } else {
            Toast.makeText(requireContext(), "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    // 📷 Камера: запуск
    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success && selectedImageUri != null) {
            val file = File(requireContext().cacheDir, "child_photo_${System.currentTimeMillis()}.jpg")
            requireContext().contentResolver.openInputStream(selectedImageUri!!)?.use { input ->
                FileOutputStream(file).use { output -> input.copyTo(output) }
            }
            currentPhotoFile = file
            photoChanged = true
            binding.childPhotoDialogImageView.setImageURI(selectedImageUri)
            Log.d("ChildPhotoDialogFragment", "Фото сделано: $file")
        } else {
            Log.w("ChildPhotoDialogFragment", "Съёмка не удалась")
        }
    }

    // 🖼 Галерея: разрешение
    private val requestGalleryPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            openGallery()
        } else {
            Toast.makeText(requireContext(), "Разрешение на доступ к галерее отклонено", Toast.LENGTH_SHORT).show()
        }
    }

    // 🖼 Галерея: выбор изображения
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            data?.data?.let { uri ->
                val file = uriToFile(requireContext(), uri)
                if (file != null && file.exists() && file.length() > 0) {
                    currentPhotoFile = file
                    photoChanged = true
                    binding.childPhotoDialogImageView.setImageURI(Uri.fromFile(file))
                    Log.d("ChildPhotoDialogFragment", "Выбрано фото: ${file.absolutePath}")
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DialogPhotoViewBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        childId?.let { loadChildPhoto(it) }

        binding.changePhotoButton.setOnClickListener {
            showImageChoiceDialog()
        }

        binding.confirmChangePhotoButton.setOnClickListener {
            updateChildPhoto()
        }
    }

    private fun showImageChoiceDialog() {
        val options = arrayOf("Сделать фото", "Выбрать из галереи")
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите источник")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> checkGalleryPermission()
                }
            }
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED
        ) {
            takePhoto()
        } else {
            requestCameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun takePhoto() {
        val file = File.createTempFile("child_photo_", ".jpg", requireContext().cacheDir).apply {
            createNewFile()
            deleteOnExit()
        }
        selectedImageUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
        takePictureLauncher.launch(selectedImageUri)
    }

    private fun checkGalleryPermission() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_MEDIA_IMAGES
        else
            Manifest.permission.READ_EXTERNAL_STORAGE

        Log.d("ChildPhotoDialog", "Checking gallery permission: $permission")
        Log.d("ChildPhotoDialog", "Android version: ${Build.VERSION.SDK_INT}")

        val currentPermissionStatus = ContextCompat.checkSelfPermission(requireContext(), permission)
        Log.d("ChildPhotoDialog", "Current permission status: $currentPermissionStatus")

        if (currentPermissionStatus == PackageManager.PERMISSION_GRANTED) {
            Log.d("ChildPhotoDialog", "Permission already granted, opening gallery")
            openGallery()
        } else {
            Log.d("ChildPhotoDialog", "Permission not granted, requesting...")
            Log.d("ChildPhotoDialog", "Should show rationale: ${shouldShowRequestPermissionRationale(permission)}")

            // Проверяем, можем ли мы запросить разрешение
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    requestGalleryPermissionLauncher.launch(permission)
                    Log.d("ChildPhotoDialog", "Permission request launched successfully")
                } catch (e: Exception) {
                    Log.e("ChildPhotoDialog", "Error launching permission request", e)
                    Toast.makeText(requireContext(), "Ошибка запроса разрешения", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Для старых версий Android
                openGallery()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageLauncher.launch(intent)
    }

    private fun loadChildPhoto(childId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val bitmap = childManager.getChildProfilePhoto(childId)
                if (bitmap != null) {
                    binding.childPhotoDialogImageView.setImageBitmap(bitmap)
                } else {
                    Toast.makeText(requireContext(), "Фото не найдено", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("ChildPhotoDialogFragment", "Ошибка загрузки фото", e)
            }
        }
    }

    private fun updateChildPhoto() {
        if (!photoChanged || currentPhotoFile == null) {
            dismiss()
            return
        }

        val id = childId ?: return
        val file = currentPhotoFile ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                childManager.updateChildPhoto(
                    childId = id,
                    photoFile = file,
                    onSuccess = {
                        parentFragmentManager.setFragmentResult(
                            "childPhotoUpdated",
                            bundleOf("photoPath" to file.absolutePath)
                        )
                        Toast.makeText(requireContext(), "Фото обновлено", Toast.LENGTH_SHORT).show()
                        dismiss()
                    },
                    onError = { error ->
                        Toast.makeText(requireContext(), "Ошибка: $error", Toast.LENGTH_SHORT).show()
                    }
                )
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun uriToFile(context: Context, uri: Uri): File? {
        val inputStream = context.contentResolver.openInputStream(uri)
        val fileName = getFileName(context, uri)
        val photoDir = File(context.filesDir, "photo_cache").apply { mkdirs() }
        val file = File(photoDir, fileName)

        return try {
            inputStream?.use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            if (file.exists() && file.length() > 0) file else null
        } catch (e: Exception) {
            Log.e("ChildPhotoDialogFragment", "Ошибка сохранения файла: ${e.message}")
            null
        }
    }

    private fun getFileName(context: Context, uri: Uri): String {
        var name = "selected_photo.jpg"
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
        currentPhotoFile?.takeIf { it.exists() }?.delete()
    }
}
