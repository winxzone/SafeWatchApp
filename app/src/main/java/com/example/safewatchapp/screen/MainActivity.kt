package com.example.safewatchapp.screen

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.safewatchapp.R

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupUI()
    }

    // Метод для настройки интерфейса
    private fun setupUI() {
        enableEdgeToEdge()
        setContentView(R.layout.main)
    }
}
