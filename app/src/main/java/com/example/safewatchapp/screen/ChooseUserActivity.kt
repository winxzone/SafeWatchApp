package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R
import com.example.safewatchapp.utils.Constants
import com.example.safewatchapp.databinding.ChooseUserBinding
import com.example.safewatchapp.utils.RoleManager

class ChooseUserActivity : AppCompatActivity() {
    private lateinit var binding: ChooseUserBinding
    private var selectedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val savedRole = RoleManager.getRole(this)
        if (savedRole != null) {
            navigateToNextScreen()
            finish()
            return
        }

        binding = ChooseUserBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupListeners()
    }

    // Настройка интерфейса
    private fun setupUI() {
        binding.buttonNext.isEnabled = false
        binding.buttonNext.setBackgroundColor(getColor(R.color.gray))
    }

    // Настройка всех слушателей
    private fun setupListeners() {

        binding.cardChild.setOnClickListener {
            selectRole(Constants.CHILD)
        }

        binding.cardParent.setOnClickListener {
            selectRole(Constants.PARENT)
        }

        binding.buttonNext.setOnClickListener {
            saveRoleToPreferences()
            navigateToNextScreen()
            finish()
        }
    }

    // Метод для обработки выбора роли
    private fun selectRole(role: String) {
        selectedRole = role

        // Обводка нужной карточки
        when (role) {
            Constants.CHILD -> {
                binding.cardChild.strokeColor = getColor(R.color.stroke_color)
                binding.cardParent.strokeColor = getColor(android.R.color.transparent)
            }
            Constants.PARENT -> {
                binding.cardParent.strokeColor = getColor(R.color.stroke_color)
                binding.cardChild.strokeColor = getColor(android.R.color.transparent)
            }
        }

        // Активируем кнопку "Next"
        binding.buttonNext.isEnabled = true
        binding.buttonNext.setBackgroundColor(getColor(R.color.blue_main))
    }

    // Сохранение роли в SharedPreferences через RoleManager
    private fun saveRoleToPreferences() {
        selectedRole?.let {
            RoleManager.saveRole(this, it)  // Сохраняем выбранную роль
        }
    }

    // Переход на следующий экран
    private fun navigateToNextScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }
}
