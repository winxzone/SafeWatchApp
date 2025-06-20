package com.example.safewatchapp.screen.fragments

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import com.example.safewatchapp.R
import androidx.core.graphics.drawable.toDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.safewatchapp.databinding.FragmentAppFilterDialogBinding
import com.example.safewatchapp.manager.AppFilterManager
import com.example.safewatchapp.models.AppInfo
import com.example.safewatchapp.screen.adapter.AppFilterAdapter

// todo: Фильтр нужно делать не под каждое устройство, а под профили детей.
//  (В данный момент родитель не может настроить фильтр для устройства ребенка,
//  так как видит на своем устройстве свои приложения)
class AppFilterDialogFragment : DialogFragment() {

    private var _binding: FragmentAppFilterDialogBinding? = null
    private val binding get() = _binding!!

    private lateinit var appFilterAdapter: AppFilterAdapter
    private var allApps: List<AppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.TransparentDialogTheme)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAppFilterDialogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            setGravity(Gravity.CENTER)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        allApps = getInstalledUserApps(requireContext())
        setupRecyclerView()
        setupSearch()
        setupButtons()

        Log.d("AppFilter", "Apps: ${allApps.joinToString { it.appName }}")
    }

    private fun setupRecyclerView() {
        appFilterAdapter = AppFilterAdapter(requireContext(), allApps)
        binding.appListRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = appFilterAdapter
        }
    }

    private fun setupSearch() {
        binding.searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                filterApps(s?.toString() ?: "")
            }
        })
    }

    private fun setupButtons() {
        binding.cancelButton.setOnClickListener {
            dismiss()
        }

        binding.okButton.setOnClickListener {
            // Сохраняем изменения и закрываем диалог
            saveChanges()
            dismiss()
        }
    }

    private fun saveChanges() {
        // Получаем текущее состояние всех приложений и сохраняем
        val enabledApps = allApps.filter { it.isEnabled }.map { it.packageName }.toSet()
        AppFilterManager.setAllowedApps(requireContext(), enabledApps)

        // Можно добавить callback для уведомления родительской активности об изменениях
        Log.d("AppFilter", "Saved allowed apps: ${enabledApps.joinToString()}")
    }

    private fun filterApps(searchQuery: String) {
        val filteredApps = if (searchQuery.isNotBlank()) {
            allApps.filter { app ->
                app.appName.contains(searchQuery, ignoreCase = true) ||
                        app.packageName.contains(searchQuery, ignoreCase = true)
            }
        } else {
            allApps
        }

        appFilterAdapter.updateApps(filteredApps)
    }

    private fun getInstalledUserApps(context: Context): List<AppInfo> {
        val pm = context.packageManager
        val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        val allowedPackages = AppFilterManager.getAllowedApps(context)

        return installedPackages
            .filter {
                pm.getLaunchIntentForPackage(it.packageName) != null &&
                        (it.flags and ApplicationInfo.FLAG_SYSTEM == 0)
            }
            .map {
                AppInfo(
                    packageName = it.packageName,
                    appName = pm.getApplicationLabel(it).toString(),
                    icon = pm.getApplicationIcon(it),
                    isEnabled = allowedPackages.contains(it.packageName)
                )
            }
            .sortedBy { it.appName.lowercase() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}