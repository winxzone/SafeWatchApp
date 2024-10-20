package com.example.safewatchapp

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.databinding.ChooseUserBinding


class ChooseUserActivity : AppCompatActivity() {
    private lateinit var bindingClass: ChooseUserBinding
    private var selectedRole: String? = null  // Переменная для сохранения значения RadioButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bindingClass = ChooseUserBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

        // Начально делаем кнопку неактивной
        bindingClass.buttonNext.isEnabled = false
        bindingClass.buttonNext.setBackgroundColor(getColor(R.color.gray))

        bindingClass.radioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioButtonChild -> selectedRole = "Child"
                R.id.radioButtonParent -> selectedRole = "Parent"
            }

            bindingClass.buttonNext.isEnabled = true
            bindingClass.buttonNext.setBackgroundColor(getColor(R.color.blue_main))
        }

        bindingClass.buttonNext.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            // Можно передать значение selectedRole в следующее Activity через Intent
            startActivity(intent)
        }
    }
}
