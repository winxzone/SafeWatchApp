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
import com.example.safewatchapp.viewmodels.CreateChildProfileViewModel
import java.io.File
import com.example.safewatchapp.utils.Result

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddChildProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // todo почему cameraIcon? изменить название
        binding.cameraIcon.setOnClickListener { showImagePickerDialog() }
        binding.confirmButton.setOnClickListener { saveChildProfile() }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.childCreationStatus.observe(viewLifecycleOwner) { result ->
            when (result) {
                is Result.Success -> {
                    val childId = result.data

                    // Можно также передать результат другим фрагментам
                    parentFragmentManager.setFragmentResult(
                        "childProfileAdded",
                        Bundle().apply { putString("childId", childId) }
                    )

                    // Навигация на главный экран
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

        viewModel.createChildProfile(name, selectedImageUri, requireContext(), deviceId)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}