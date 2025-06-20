package com.example.safewatchapp.screen.fragments

import android.Manifest
import android.content.ContentResolver
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.FragmentAddChildProfileBinding
import com.example.safewatchapp.screen.MainActivity
import com.example.safewatchapp.viewmodels.CreateChildProfileViewModel
import java.io.File
import com.example.safewatchapp.utils.Result
import androidx.core.net.toUri

class ChildAddProfileFragment : Fragment() {

    private var _binding: FragmentAddChildProfileBinding? = null
    private val binding get() = _binding ?: throw IllegalStateException("Binding is null")
    private var selectedImageUri: Uri? = null
    private val viewModel: CreateChildProfileViewModel by viewModels()

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            binding.avatarImageView.setImageURI(it)
            Log.d("ChildAddProfile", "Image selected from gallery: $selectedImageUri")
        } ?: Log.w("ChildAddProfile", "No image selected from gallery")
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success: Boolean ->
        if (success && selectedImageUri != null) {
            binding.avatarImageView.setImageURI(selectedImageUri)
            Log.d("ChildAddProfile", "Photo taken successfully: $selectedImageUri")
        } else {
            Log.w("ChildAddProfile", "Photo capture failed or URI is null: $selectedImageUri")
        }
    }

    // Permission launcher for CAMERA
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                launchCamera()
            } else {
                Toast.makeText(requireContext(), "Разрешение на камеру отклонено", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddChildProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraIcon.setOnClickListener { showImagePickerDialog() }
        binding.confirmButton.setOnClickListener { saveChildProfile() }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.childCreationStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val childId = result.data

                    parentFragmentManager.setFragmentResult(
                        "childProfileAdded",
                        Bundle().apply { putString("childId", childId) }
                    )

                    val intent = Intent(requireContext(), MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    requireActivity().finish()
                }

                is Result.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }

                else -> {}
            }
        }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Сделать фото", "Выбрать из галереи")
        AlertDialog.Builder(requireContext())
            .setTitle("Выберите действие")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> capturePhoto()
                    1 -> pickImageFromGallery()
                }
            }
            .show()
    }

    private fun pickImageFromGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun capturePhoto() {
        // Проверка разрешения на камеру
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        } else {
            launchCamera()
        }
    }

    private fun launchCamera() {
        selectedImageUri = createImageUri()
        takePictureLauncher.launch(selectedImageUri)
    }

    private fun createImageUri(): Uri {
        val file = File(requireContext().cacheDir, "child_avatar_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
    }

    private fun saveChildProfile() {
        val name = binding.childNameEditText.text.toString().trim()
        val deviceId = arguments?.getString("deviceId")

        if (name.isEmpty()) {
            Toast.makeText(requireContext(), "Введите имя", Toast.LENGTH_SHORT).show()
            return
        }

        if (deviceId == null) return

        val imageUriToUse = selectedImageUri ?: getDefaultAvatarUri()
        viewModel.createChildProfile(name, imageUriToUse, requireContext(), deviceId)
    }

    private fun getDefaultAvatarUri(): Uri {
        val resources = requireContext().resources
        return (ContentResolver.SCHEME_ANDROID_RESOURCE +
                "://" + resources.getResourcePackageName(R.drawable.ic_default_photo)
                + "/" + resources.getResourceTypeName(R.drawable.ic_default_photo)
                + "/" + resources.getResourceEntryName(R.drawable.ic_default_photo)).toUri()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
