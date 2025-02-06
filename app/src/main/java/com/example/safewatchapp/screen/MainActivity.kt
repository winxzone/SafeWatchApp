package com.example.safewatchapp.screen

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DialogEditNameBinding
import com.example.safewatchapp.databinding.DrawerHeaderBinding
import com.example.safewatchapp.databinding.MainBinding
import com.example.safewatchapp.utils.TokenManager
import com.example.safewatchapp.models.User
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.utils.ChildManager
import com.example.safewatchapp.utils.handlers.NavigationHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainBinding
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var childManager: ChildManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigationHandler = NavigationHandler(this, binding)
        navigationHandler.setupNavigationMenu()

        childManager = ChildManager()


        setupListeners()
        setupFragmentResultListener()

        val token = TokenManager.getToken(this)
        if (token != null) {
            fetchChildren(token)
            fetchUser(token)
        } else {
            Toast.makeText(this, "Token not found", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupListeners() {
        binding.childNameTextView.setOnClickListener {
            showEditNameDialog()
        }

        binding.previousButton.setOnClickListener {
            childManager.switchToPreviousChild()
            displayChildProfile()
        }

        binding.nextButton.setOnClickListener {
            childManager.switchToNextChild()
            displayChildProfile()
        }

        binding.notificationButton.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.menuButton.setOnClickListener{
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        binding.childPhotoImageView.setOnClickListener {
            val currentChildId = childManager.getCurrentChild()?.id
            if (currentChildId != null) {
                openChildPhotoDialog(currentChildId)
            } else {
                Toast.makeText(this, "Child ID not found", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener("childPhotoUpdated", this) { _, bundle ->
            val photoPath = bundle.getString("photoPath")
            photoPath?.let {
                val file = File(it)
                binding.childPhotoImageView.setImageURI(Uri.fromFile(file))
            }
        }
    }

    private fun openChildPhotoDialog(childId: String) {
        val dialog = ChildPhotoDialogFragment().apply {
            arguments = Bundle().apply {
                putString("childId", childId)
            }
        }
        dialog.show(supportFragmentManager, "ChildPhotoDialogFragment")
    }

    private fun fetchChildren(token: String) {
        childManager.fetchChildren(token,
            onSuccess = { children ->
                if (children.isNotEmpty()) {
                    binding.childDeviceLayout.visibility = View.VISIBLE
                    binding.noDeviceLayout.visibility = View.GONE
                    displayChildProfile()
                } else {
                    binding.childDeviceLayout.visibility = View.GONE
                    binding.noDeviceLayout.visibility = View.VISIBLE
                    Toast.makeText(this, "No children found", Toast.LENGTH_SHORT).show()
                }
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

    private fun fetchUser(token: String) {
        ApiClient.apiService.getUserProfile("Bearer $token")
            .enqueue(object : Callback<User> {
                override fun onResponse(call: Call<User>, response: Response<User>) {

                    if (response.isSuccessful) {
                        val user = response.body()
                        if (user != null) {
                            val headerBinding = DrawerHeaderBinding.bind(binding.navigationView.getHeaderView(0))
                            headerBinding.userNameTextView.text = user.name
                            headerBinding.userEmailTextView.text = user.email
                        } else {
                            Toast.makeText(this@MainActivity, "User not found", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(this@MainActivity, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onFailure(call: Call<User>, t: Throwable) {
                    Log.d("FetchUser", "onFailure called: ${t.message}")
                    Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun displayChildProfile() {
        val child = childManager.getCurrentChild()
        if (child != null) {
            binding.childNameTextView.text = child.name

            val token = TokenManager.getToken(this) ?: return
            childManager.getChildProfilePhoto(child.id, token,
                onSuccess = { bitmap ->
                    binding.childPhotoImageView.setImageBitmap(bitmap)
                },
                onError = { error ->
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
                }
            )
        }
    }




    private fun showEditNameDialog() {
        val currentName = binding.childNameTextView.text.toString()

        // Создаем ViewGroup для диалога
        val dialogBinding = DialogEditNameBinding.inflate(layoutInflater)
        dialogBinding.editNameInput.setText(currentName)

        dialogBinding.errorTextView.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.edit_child_name)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null) // Устанавливаем слушатель позже
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = dialogBinding.editNameInput.text.toString().trim()
                if (newName.isNotEmpty()) {
                    updateChildName(newName)
                    dialog.dismiss()
                } else {
                    // Отображаем сообщение об ошибке
                    dialogBinding.errorTextView.visibility = View.VISIBLE
                    dialogBinding.errorTextView.text = getString(R.string.error_empty_name)
                }
            }
        }
        dialog.show()
    }
    private fun updateChildName(newName: String) {
        val token = TokenManager.getToken(this) ?: return
        val child = childManager.getCurrentChild() ?: return

        childManager.updateChildName(token, child.id, newName,
            onSuccess = {
                binding.childNameTextView.text = newName
                Toast.makeText(this, "Name updated successfully", Toast.LENGTH_SHORT).show()
            },
            onError = { error ->
                Toast.makeText(this, error, Toast.LENGTH_SHORT).show()
            }
        )
    }

}

