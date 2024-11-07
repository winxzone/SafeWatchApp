package com.example.safewatchapp.models

data class UserRegistration(
    val name: String,
    val email: String,
    val password: String,
    val confirmPassword: String
)
