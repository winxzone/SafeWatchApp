package com.example.safewatchapp

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.safewatchapp.databinding.ChooseUserActivityBinding


class ChooseUserActivity : AppCompatActivity() {
    lateinit var bindingClass: ChooseUserActivityBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        bindingClass = ChooseUserActivityBinding.inflate(layoutInflater)
        setContentView(bindingClass.root)

    }






}