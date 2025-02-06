package com.example.safewatchapp.screen

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safewatchapp.databinding.DeviceVerificationBinding
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.service.ApiClient
import com.example.safewatchapp.adapters.NewDevicesAdapter
import com.example.safewatchapp.adapters.AddedDevicesAdapter
import com.example.safewatchapp.utils.TokenManager
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class DeviceVerificationActivity : AppCompatActivity() {
    private lateinit var binding: DeviceVerificationBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = DeviceVerificationBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.newDevicesRecyclerView.layoutManager = LinearLayoutManager(this)
        binding.addedDevicesRecyclerView.layoutManager = LinearLayoutManager(this)

        listChildDevice()
        setupListeners()
    }


    private fun setupListeners() {
        binding.backButton.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

    }

    private fun listChildDevice() {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showToast("Authentication error: Token is missing")
            return
        }

        ApiClient.apiService.listChildDevice("Bearer $token").enqueue(object : Callback<List<ChildDevice>> {
            override fun onResponse(call: Call<List<ChildDevice>>, response: Response<List<ChildDevice>>) {
                if (response.isSuccessful) {
                    val devices = response.body() ?: emptyList()
                    updateRecyclerView(devices)
                } else {
                    showToast("Failed to load devices: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<List<ChildDevice>>, t: Throwable) {
                showToast("Network error: ${t.message}")
            }
        })
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    private fun updateRecyclerView(devices: List<ChildDevice>) {
        // Фильтр только "unconfirmed"
        val newDevices = devices.filter { it.status == "unconfirmed" }

        // Настройка адаптера для новых устройств
        val newDevicesAdapter = NewDevicesAdapter(newDevices) { device, isConfirmed ->
            handleDeviceAction(device, isConfirmed)
        }
        binding.newDevicesRecyclerView.adapter = newDevicesAdapter

        if (newDevices.isEmpty()) {
            binding.newDevicesRecyclerView.visibility = View.GONE
        } else {
            binding.newDevicesRecyclerView.visibility = View.VISIBLE
        }

        // Обновляем отображение для добавленных устройств
        val addedDevices = devices.filter { it.status == "confirmed" }
        val addedDevicesAdapter = AddedDevicesAdapter(addedDevices) { _ ->
        }
        binding.addedDevicesRecyclerView.adapter = addedDevicesAdapter
    }

    private fun handleDeviceAction(device: ChildDevice, isConfirmed: Boolean) {
        val token = TokenManager.getToken(this)
        if (token == null) {
            showToast("User not authenticated")
            return
        }

        val call = if (isConfirmed) {
            ApiClient.apiService.confirmChildDevice("Bearer $token", device.id!!)
        } else {
            ApiClient.apiService.cancelChildDevice("Bearer $token", device.id!!)
        }

        call.enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    showToast(if (isConfirmed) "Device confirmed" else "Device canceled")
                    listChildDevice() // обновление списка устройств
                } else {
                    showToast(if (isConfirmed) "Failed to confirm device" else "Failed to cancel device")
                    Log.e("API_ERROR", "Response code: ${response.code()}, message: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                showToast("Network error: ${t.message}")
                Log.e("API_FAILURE", "Error: ${t.message}")
            }
        })
    }
}