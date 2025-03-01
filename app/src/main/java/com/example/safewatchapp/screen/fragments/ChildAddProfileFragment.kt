package com.example.safewatchapp.screen.fragments

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.example.safewatchapp.databinding.FragmentAddChildProfileBinding
import com.example.safewatchapp.screen.MainActivity
import com.example.safewatchapp.viewmodels.ChildProfileViewModel
import java.io.File
import com.example.safewatchapp.utils.Result

class ChildAddProfileFragment : Fragment() {

    private lateinit var binding: FragmentAddChildProfileBinding
    private var selectedImageUri: Uri? = null
    private val viewModel: ChildProfileViewModel by viewModels()

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentAddChildProfileBinding.inflate(inflater, container, false)
        savedInstanceState?.let {
            selectedImageUri = it.getParcelable("selectedImageUri")
            selectedImageUri?.let { uri ->
                binding.avatarImageView.setImageURI(uri)
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.cameraIcon.setOnClickListener { showImagePickerDialog() }
        binding.confirmButton.setOnClickListener { saveChildProfile() }

        observeViewModel()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable("selectedImageUri", selectedImageUri)
    }

    private fun observeViewModel() {
        viewModel.childCreationStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    Toast.makeText(requireContext(), "Профиль создан и устройство привязано", Toast.LENGTH_SHORT).show()
                    val intent = Intent(requireContext(), MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    requireActivity().finish()
                }
                is Result.Error -> {
                    Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }
    // todo:
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

        if (deviceId == null) {
            Toast.makeText(requireContext(), "Ошибка: ID устройства отсутствует", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.createChildProfile(name, selectedImageUri, requireContext(), deviceId)
    }
}