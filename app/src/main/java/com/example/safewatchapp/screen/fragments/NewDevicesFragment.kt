package com.example.safewatchapp.screen.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.databinding.FragmentNewDevicesBinding
import com.example.safewatchapp.databinding.ItemNewDeviceBinding
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.TimeFormatter
import kotlinx.coroutines.launch

class NewDevicesFragment : Fragment() {

    private var _binding: FragmentNewDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация RecyclerView с пустым адаптером
        adapter = DeviceAdapter(emptyList())
        binding.newDevicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@NewDevicesFragment.adapter
        }

        // Настройка SwipeRefreshLayout
        binding.swipeRefreshLayout.setOnRefreshListener {
            listChildDevices()
        }

        // Начальная загрузка данных
        listChildDevices()

        // Получение deviceId из аргументов
        val deviceId = arguments?.getString("deviceId")
        if (deviceId != null) {
            Log.d("NewDevicesFragment", "Received deviceId: $deviceId")
        }
    }

    private fun listChildDevices() {
        binding.swipeRefreshLayout.isRefreshing = true // Показываем индикатор

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val devices = ApiClient.childDeviceApiService.listChildDevice()
                val unconfirmedDevices = devices.filter { it.status == "unconfirmed" }
                updateRecyclerView(unconfirmedDevices)
            } catch (e: Exception) {
                Log.e("NewDevicesFragment", "Error loading devices: ${e.message}")
                updateRecyclerView(emptyList())
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateRecyclerView(devices: List<ChildDevice>) {
        adapter.updateData(devices)

        if (devices.isEmpty()) {
            binding.emptyStateCard.visibility = View.VISIBLE
            binding.newDevicesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateCard.visibility = View.GONE
            binding.newDevicesRecyclerView.visibility = View.VISIBLE

            arguments?.getString("deviceId")?.let { deviceId ->
                val position = devices.indexOfFirst { it.deviceId == deviceId } // Используем deviceId вместо id
                if (position >= 0) {
                    binding.newDevicesRecyclerView.scrollToPosition(position)
                    Log.d("NewDevicesFragment", "Scrolled to position: $position")
                }
            }
        }
    }

    private inner class DeviceAdapter(private var devices: List<ChildDevice>) :
        RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        inner class DeviceViewHolder(private val binding: ItemNewDeviceBinding) :
            RecyclerView.ViewHolder(binding.root) {
            fun bind(device: ChildDevice) {
                binding.deviceName.text = device.name
                binding.requestTime.text = TimeFormatter.formatDateTime(device.createdAt)

                binding.approveButton.setOnClickListener {
                    if (device.id == null) {
                        showToast("Device ID is missing")
                        return@setOnClickListener
                    }

                    val fragment = ChildAddProfileFragment().apply {
                        arguments = Bundle().apply { putString("deviceId", device.id) }
                    }
                    parentFragmentManager.beginTransaction()
                        .replace(android.R.id.content, fragment)
                        .addToBackStack(null)
                        .commit()
                }

                binding.declineButton.setOnClickListener {
                    handleDeviceAction(device, false)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val binding = ItemNewDeviceBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            return DeviceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position])
        }

        override fun getItemCount(): Int = devices.size

        fun updateData(newDevices: List<ChildDevice>) {
            devices = newDevices
            notifyDataSetChanged()
        }
    }

    private fun handleDeviceAction(device: ChildDevice, isConfirmed: Boolean) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                if (isConfirmed) {
                    val confirmResponse = ApiClient.childDeviceApiService.confirmChildDevice(device.id!!)
                    if (confirmResponse.isSuccessful) {
                        showToast("Device confirmed")
                    } else {
                        showToast("Failed to confirm device: ${confirmResponse.message()}")
                    }
                } else {
                    val cancelResponse = ApiClient.childDeviceApiService.cancelChildDevice(device.id!!)
                    if (cancelResponse.isSuccessful) {
                        showToast("Device canceled")
                    } else {
                        showToast("Failed to cancel device: ${cancelResponse.message()}")
                        Log.e("API_ERROR", "Cancel failed: ${cancelResponse.code()} - ${cancelResponse.message()}")
                    }
                }
                listChildDevices() // Обновляем список после действия
            } catch (e: Exception) {
                val action = if (isConfirmed) "confirm" else "cancel"
                showToast("Failed to $action device: ${e.message}")
                Log.e("API_ERROR", "Error: ${e.message}")
            }
        }
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}