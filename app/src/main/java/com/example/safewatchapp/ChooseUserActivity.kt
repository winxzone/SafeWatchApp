package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.constance.Constance
import com.example.safewatchapp.databinding.ChooseUserBinding

class ChooseUserActivity : AppCompatActivity() {
    private lateinit var bindingClass: ChooseUserBinding
    private var selectedRole: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingClass = ChooseUserBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        setupUI()
        setupListeners()
    }

    // Настройка интерфейса
    private fun setupUI() {
        bindingClass.buttonNext.isEnabled = false
        bindingClass.buttonNext.setBackgroundColor(getColor(R.color.gray))
    }

    // Настройка всех слушателей
    private fun setupListeners() {

        bindingClass.cardChild.setOnClickListener {
            selectRole(Constance.CHILD)
        }

        bindingClass.cardParent.setOnClickListener {
            selectRole(Constance.PARENT)
        }

        bindingClass.buttonNext.setOnClickListener {
            navigateToNextScreen()
            finish()
        }
    }

    // Метод для обработки выбора роли
    private fun selectRole(role: String) {
        selectedRole = role

        // Обводка нужной карточки
        when (role) {
            Constance.CHILD -> {
                bindingClass.cardChild.strokeColor = getColor(R.color.stroke_color)
                bindingClass.cardParent.strokeColor = getColor(android.R.color.transparent)
            }
            Constance.PARENT -> {
                bindingClass.cardParent.strokeColor = getColor(R.color.stroke_color)
                bindingClass.cardChild.strokeColor = getColor(android.R.color.transparent)
            }
        }

        // Активируем кнопку "Next"
        bindingClass.buttonNext.isEnabled = true
        bindingClass.buttonNext.setBackgroundColor(getColor(R.color.blue_main))
    }

    // Переход на следующий экран
    private fun navigateToNextScreen() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.putExtra(Constance.ROLE, selectedRole)  // Передача роли в следующее Activity
        startActivity(intent)
    }
}
