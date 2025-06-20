package com.example.safewatchapp.screen.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.databinding.DialogDeleteObjectBinding
import com.example.safewatchapp.databinding.FragmentAddedDevicesBinding
import com.example.safewatchapp.databinding.ItemAddedDeviceBinding
import com.example.safewatchapp.manager.ChildManager
import com.example.safewatchapp.models.ChildDevice
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.utils.TimeFormatter
import com.google.android.material.bottomsheet.BottomSheetDialog
import kotlinx.coroutines.launch

class AddedDevicesFragment : Fragment() {

    private var _binding: FragmentAddedDevicesBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: DeviceAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddedDevicesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = DeviceAdapter(emptyList())
        binding.addedDevicesRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = this@AddedDevicesFragment.adapter
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            listChildDevices()
        }

        listChildDevices()
    }

    private fun listChildDevices() {
        binding.swipeRefreshLayout.isRefreshing = true

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val devices = ApiClient.childDeviceApiService.listChildDevice()
                val confirmedDevices = devices.filter { it.status == "confirmed" }
                adapter.updateData(confirmedDevices)

                updateEmptyState(confirmedDevices)
            } catch (e: Exception) {
                Log.e("AddedDevicesFragment", "Error loading devices: ${e.message}")
                adapter.updateData(emptyList())
                updateEmptyState(emptyList())
            } finally {
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun updateEmptyState(devices: List<ChildDevice>) {
        if (devices.isEmpty()) {
            binding.emptyStateCard.visibility = View.VISIBLE
            binding.addedDevicesRecyclerView.visibility = View.GONE
        } else {
            binding.emptyStateCard.visibility = View.GONE
            binding.addedDevicesRecyclerView.visibility = View.VISIBLE
        }
    }

    private inner class DeviceAdapter(private var devices: List<ChildDevice>) :
        RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

        private var selectedPosition: Int = RecyclerView.NO_POSITION

        inner class DeviceViewHolder(private val binding: ItemAddedDeviceBinding) :
            RecyclerView.ViewHolder(binding.root) {

            fun bind(device: ChildDevice, position: Int) {
                binding.deviceName.text = device.name
                binding.confirmedTime.text = TimeFormatter.formatDateTime(device.createdAt)

                // Показываем или скрываем кнопку удаления
                binding.btnDeleteDevice.visibility = if (position == selectedPosition) View.VISIBLE else View.GONE

                binding.root.setOnClickListener {
                    if (selectedPosition == position) {
                        selectedPosition = RecyclerView.NO_POSITION
                    } else {
                        selectedPosition = position
                    }
                    notifyDataSetChanged()
                }

                binding.btnDeleteDevice.setOnClickListener {
                    showDeleteConfirmationDialog(device)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
            val inflater = LayoutInflater.from(parent.context)
            val binding = ItemAddedDeviceBinding.inflate(inflater, parent, false)
            return DeviceViewHolder(binding)
        }

        override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
            holder.bind(devices[position], position)
        }

        override fun getItemCount(): Int = devices.size

        fun updateData(newDevices: List<ChildDevice>) {
            devices = newDevices
            selectedPosition = RecyclerView.NO_POSITION
            notifyDataSetChanged()
        }

        private fun showDeleteConfirmationDialog(device: ChildDevice) {
            val bottomSheetDialog = BottomSheetDialog(requireContext())

            val binding = DialogDeleteObjectBinding.inflate(layoutInflater)

            binding.deviceNameTextView.text = device.name

            binding.btnConfirmDelete.setOnClickListener {
                bottomSheetDialog.dismiss()
                deleteDevice(device)
            }

            binding.btnCancelDelete.setOnClickListener {
                bottomSheetDialog.dismiss()
            }

            bottomSheetDialog.setContentView(binding.root)
            bottomSheetDialog.show()
        }

        // todo: ошибка удаления
        private fun deleteDevice(device: ChildDevice) {
            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val response = ApiClient.deviceLinkApiService.deleteDeviceAndChild(device.id!!)
                    if (response.isSuccessful) {
                        showToast("Устройство удалено")

                        device.childId?.let { childId ->
                            ChildManager(requireContext()).removeChildFromCache(childId)
                        }

                        listChildDevices()
                    } else {
                        showToast("Ошибка удаления: ${response.message()}")
                    }
                } catch (e: Exception) {
                    Log.e("AddedDevicesFragment", "Ошибка удаления устройства: ${e.message}")
                    showToast("Ошибка удаления: ${e.message}")
                }
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
