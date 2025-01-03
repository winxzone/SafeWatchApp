package com.example.safewatchapp.adapters

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.models.ChildDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.safewatchapp.R
import com.example.safewatchapp.utils.TimeFormatter



class AddedDevicesAdapter(private val devices: List<ChildDevice>, private val onDeviceAction: (ChildDevice) -> Unit) :
    RecyclerView.Adapter<AddedDevicesAdapter.DeviceViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_added_device, parent, false)
        return DeviceViewHolder(view)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        val device = devices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int = devices.size

    inner class DeviceViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val deviceName: TextView = itemView.findViewById(R.id.deviceName)
        private val confirmedTime: TextView = itemView.findViewById(R.id.confirmedTime)

        fun bind(device: ChildDevice) {
            deviceName.text = device.name
            confirmedTime.text = TimeFormatter.formatDateTime(device.createdAt)

            itemView.setOnClickListener {
                onDeviceAction(device)
            }
        }
    }
}
