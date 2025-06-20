package com.example.safewatchapp.screen.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.safewatchapp.databinding.ItemAppFilterBinding
import com.example.safewatchapp.manager.AppFilterManager
import com.example.safewatchapp.models.AppInfo

class AppFilterAdapter(
    private val context: Context,
    private var apps: List<AppInfo>
) : RecyclerView.Adapter<AppFilterAdapter.AppViewHolder>() {

    inner class AppViewHolder(val binding: ItemAppFilterBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val binding = ItemAppFilterBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AppViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]

        holder.binding.apply {
            appName.text = app.appName
            appIcon.setImageDrawable(app.icon)

            // Очищаем предыдущий listener чтобы избежать ложных срабатываний
            appToggle.setOnCheckedChangeListener(null)
            appToggle.isChecked = app.isEnabled

            appToggle.setOnCheckedChangeListener { _, isChecked ->
                app.isEnabled = isChecked
                val currentAllowed = AppFilterManager.getAllowedApps(context).toMutableSet()
                if (isChecked) {
                    currentAllowed.add(app.packageName)
                } else {
                    currentAllowed.remove(app.packageName)
                }
                AppFilterManager.setAllowedApps(context, currentAllowed)
            }
        }
    }

    override fun getItemCount(): Int = apps.size

    // Метод для обновления списка приложений
    fun updateApps(newApps: List<AppInfo>) {
        apps = newApps
        notifyDataSetChanged()
    }
}