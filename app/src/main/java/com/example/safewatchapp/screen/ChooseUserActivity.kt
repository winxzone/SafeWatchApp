package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.ChooseUserBinding
import com.example.safewatchapp.utils.RoleManager
import android.widget.Toast

class ChooseUserActivity : AppCompatActivity() {
    private lateinit var binding: ChooseUserBinding
    private var selectedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedRole = RoleManager.getRole(this)
        Log.d("ChooseUserActivity", "$savedRole")
        if (savedRole != null) {
            navigateToNextScreen(savedRole)
            finish()
            return
        }

        binding = ChooseUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    private fun setupUI() {
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundColor(getColor(R.color.gray))
    }

    private fun setupListeners() {
        binding.cardChild.setOnClickListener {
            selectRole(RoleManager.ROLE_CHILD)
        }

        binding.cardParent.setOnClickListener {
            selectRole(RoleManager.ROLE_PARENT)
        }

        binding.buttonNext.setOnClickListener {
            if (selectedRole != null) {
                saveRoleToPreferences()
                navigateToNextScreen(selectedRole!!)
                finish()
            } else {
                Toast.makeText(this, "Пожалуйста, выберите роль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun selectRole(role: String) {
        selectedRole = role
        when (role) {
            RoleManager.ROLE_CHILD -> {
                binding.cardChild.strokeColor = getColor(R.color.stroke_color)
                binding.cardParent.strokeColor = getColor(android.R.color.transparent)
            }
            RoleManager.ROLE_PARENT -> {
                binding.cardParent.strokeColor = getColor(R.color.stroke_color)
                binding.cardChild.strokeColor = getColor(android.R.color.transparent)
            }
        }
        binding.buttonNext.isEnabled = true
        binding.buttonNext.setBackgroundColor(getColor(R.color.primaryButtonBlue))
    }

    private fun saveRoleToPreferences() {
        selectedRole?.let {
            RoleManager.saveRole(this, it)
            Log.d("ChooseUserActivity", "Role saved = $it")
        }
    }

    private fun navigateToNextScreen(role: String) {
        val intent = Intent(this, LoginActivity::class.java)
        Log.d("ChooseUserActivity", "Navigating to LoginActivity with role = $role")
        startActivity(intent)
    }
}