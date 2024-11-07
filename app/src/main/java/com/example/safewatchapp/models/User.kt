package com.example.safewatchapp.models

data class User(
    val id: String?,
    val name: String?,
    val email: String?,
    val children: List<Child>?
)