package com.example.safewatchapp.screen

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DialogEditNameBinding
import com.example.safewatchapp.databinding.DrawerHeaderBinding
import com.example.safewatchapp.databinding.MainBinding
import com.example.safewatchapp.screen.fragments.ChildPhotoDialogFragment
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.manager.ChildManager
import com.example.safewatchapp.models.DeviceDailySummaryResponse
import com.example.safewatchapp.screen.fragments.AppFilterDialogFragment
import com.example.safewatchapp.service.SummaryAnalyzer
import com.example.safewatchapp.utils.RoleManager
import com.example.safewatchapp.utils.handlers.NavigationHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.navigation.NavigationView
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import android.app.DatePickerDialog
import android.graphics.BitmapFactory


//todo: добавить эффект переворачивания карточки, в котором будет отображаться данные по которым определяется эмоция

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainBinding
    private lateinit var navigationView: NavigationView
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var childManager: ChildManager
    private var userProfileFetched = false
    private var childrenFetched = false

    private var isShowingBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigationHandler = NavigationHandler(this, binding)
        navigationHandler.setupNavigationMenu()

        navigationView = binding.navigationView

        // Инициализируем ChildManager с контекстом для возможности использования SharedPreferences
        childManager = ChildManager(this)

        setupUI()
        setupListeners()
        setupFragmentResultListener()
        setupBackPressedHandler()
        loadData()
        setupFlipAnimation()
    }

    override fun onResume() {
        super.onResume()

        fetchChildren()
        if (!userProfileFetched) fetchUser()
    }


    private fun setupUI(){
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        ViewCompat.setOnApplyWindowInsetsListener(navigationView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadData(){
        fetchChildren()
        fetchUser()

    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupListeners() {
        binding.childNameTextView.setOnClickListener {
            showEditNameDialog()
        }

        binding.previousButton.setOnClickListener {
            childManager.switchToPreviousChild()
            displayChildProfile()
        }

        binding.nextButton.setOnClickListener {
            childManager.switchToNextChild()
            displayChildProfile()
        }

        binding.notificationButton.setOnClickListener {
            startActivity(Intent(this, NotificationActivity::class.java))
        }

        binding.menuButton.setOnClickListener {
            binding.drawerLayout.openDrawer(GravityCompat.START)
        }

        if (RoleManager.isChild(this)) {
            binding.appFilterButton.visibility = View.VISIBLE
            binding.appFilterButton.setOnClickListener {
                AppFilterDialogFragment().show(supportFragmentManager, "AppFilterDialog")
            }
        } else {
            binding.appFilterButton.visibility = View.GONE
        }

        binding.childPhotoImageView.setOnClickListener {
            val currentChildId = childManager.getCurrentChild()?.id
            if (currentChildId != null) {
                openChildPhotoDialog(currentChildId)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchChildren(true)
        }

        binding.calendarButton.setOnClickListener {
            val today = LocalDate.now()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    // Форматируем выбранную дату
                    val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                    val formattedDate = selectedDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    loadChildProfileForDate(formattedDate)
                },
                today.year,
                today.monthValue - 1, // month в DatePickerDialog начинается с 0
                today.dayOfMonth
            )

            // Ограничиваем выбор дат не позже сегодняшнего дня
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

            // Показываем календарь
            datePickerDialog.show()
        }
    }

    private fun setupFlipAnimation() {

        binding.flipButton.setOnClickListener {
            flipCard(true)
        }

        binding.flipBackButton.setOnClickListener {
            flipCard(false)
        }
    }

    private fun flipCard(showBack: Boolean) {
        if (isShowingBack == showBack) return

        val frontView = binding.summaryDataLayout
        val backView = binding.cardBack

        val visibleView = if (showBack) frontView else backView
        val hiddenView = if (showBack) backView else frontView

        visibleView.animate()
            .rotationY(90f)
            .setDuration(150)
            .withEndAction {
                visibleView.visibility = View.GONE
                hiddenView.visibility = View.VISIBLE
                hiddenView.rotationY = 90f
                hiddenView.animate()
                    .rotationY(0f)
                    .setDuration(150)
                    .start()
            }
            .start()

        isShowingBack = showBack
    }

    private fun updateBackCard(summary: DeviceDailySummaryResponse) {
        binding.backSummaryDateTextView.text = summary.date

        binding.backScreenTimeTextView.text = "Экранное время: ${summary.totalScreenTime / 60000} мин."
        binding.backTopAppTextView.text = "Топ-приложение: ${summary.topAppPackage ?: "--"}"
        binding.backNotificationsTextView.text = "Уведомлений: ${summary.notificationsCount}"
        binding.backUnlocksTextView.text = "Разблокировок: ${summary.screenUnlockCount}"
        binding.backUsedAtNightTextView.text = "Использовался ночью: ${if (summary.usedAtNight) "Да" else "Нет"}"
    }



    private fun setupFragmentResultListener() {
        supportFragmentManager.setFragmentResultListener("childPhotoUpdated", this) { _, bundle ->
            val photoPath = bundle.getString("photoPath")
            photoPath?.let {
                val file = File(it)
                binding.childPhotoImageView.setImageURI(Uri.fromFile(file))
                childManager.getCurrentChild()?.let { child ->
                    childManager.updatePhotoInCache(child.id, file)
                }
            }
        }
    }

    private fun setupBackPressedHandler() {
        val callback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        }
        onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun openChildPhotoDialog(childId: String) {
        val dialog = ChildPhotoDialogFragment().apply {
            arguments = Bundle().apply {
                putString("childId", childId)
            }
        }
        dialog.show(supportFragmentManager, "ChildPhotoDialogFragment")
    }

    private fun fetchChildren(forceRefresh: Boolean = false) {
        lifecycleScope.launch {
            try {
                childManager.fetchChildren(
                    forceRefresh = forceRefresh,
                    onSuccess = { children ->
                        childrenFetched = true
                        if (children.isNotEmpty()) {
                            binding.childDeviceLayout.visibility = View.VISIBLE
                            binding.noDeviceLayout.visibility = View.GONE
                            displayChildProfile()
                        } else {
                            binding.childDeviceLayout.visibility = View.GONE
                            binding.noDeviceLayout.visibility = View.VISIBLE
                        }
                        binding.swipeRefreshLayout.isRefreshing = false
                    },
                    onError = { error ->
                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                        binding.swipeRefreshLayout.isRefreshing = false
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Error fetching children", e)
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.swipeRefreshLayout.isRefreshing = false
            }
        }
    }

    private fun fetchUser() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.authApiService.getUserProfile()
                userProfileFetched = true

                if (response.isSuccessful) {
                    val user = response.body()
                    if (user != null) {
                        val headerBinding = DrawerHeaderBinding.bind(navigationView.getHeaderView(0))
                        headerBinding.userNameTextView.text = user.name
                        headerBinding.userEmailTextView.text = user.email
                    } else {
                        Toast.makeText(this@MainActivity, "User not found", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Failed to fetch profile: ${response.message()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Log.e("FetchUser", "Error fetching user profile: ${e.message}")
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadChildProfileForDate(date: String) {
        val child = childManager.getCurrentChild()
        if (child == null) {
            Toast.makeText(this@MainActivity, "Ребёнок не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            // Очищаем UI перед загрузкой новых данных
            binding.childNameTextView.text = ""
            binding.childPhotoImageView.setImageBitmap(null)
            showNoSummary() // Сбрасываем саммари

            childManager.loadExpandedChildProfile(
                childId = child.id,
                date = date,
                onSuccess = { profile ->
                    Log.d("MainActivity", "Profile loaded for $date: $profile")
                    // Обновляем UI
                    binding.childNameTextView.text = profile.name

                    // Загружаем фото асинхронно
                    lifecycleScope.launch {
                        val bitmap = childManager.getChildProfilePhoto(profile.id)
                        binding.childPhotoImageView.setImageBitmap(
                            bitmap ?: BitmapFactory.decodeResource(resources, R.drawable.ic_default_photo)
                        )
                        Log.d("MainActivity", "Photo loaded for ${profile.id}: ${bitmap != null}")
                    }

                    profile.summary?.let { summary ->
                        Log.d("MainActivity", "Summary for $date: $summary")
                        showSummary(summary)
                        updateBackCard(summary)
                    } ?: run {
                        Log.w("MainActivity", "No summary for $date")
                        Toast.makeText(
                            this@MainActivity,
                            "Нет данных за $date",
                            Toast.LENGTH_SHORT
                        ).show()
                        showNoSummary()
                    }

                },
                onError = { error ->
                    Log.e("MainActivity", "Failed to load profile for $date: $error")
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки профиля за $date: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNoSummary()
                }
            )
        }
    }

    private fun displayChildProfile() {
        val child = childManager.getCurrentChild()
        if (child == null) {
            Toast.makeText(this, "Ребёнок не выбран", Toast.LENGTH_SHORT).show()
            return
        }

        // Получаем текущую дату в формате YYYY-MM-DD
        val currentDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

        lifecycleScope.launch {
            childManager.loadExpandedChildProfile(
                childId = child.id,
                date = currentDate, // Передаем текущую дату
                onSuccess = { profile ->
                    binding.childNameTextView.text = profile.name

                    // Загружаем фото асинхронно
                    lifecycleScope.launch {
                        val bitmap = childManager.getChildProfilePhoto(profile.id)
                        if (bitmap != null) {
                            binding.childPhotoImageView.setImageBitmap(bitmap)
                        } else {
                            binding.childPhotoImageView.setImageResource(R.drawable.ic_default_photo)
                        }
                    }

                    profile.summary?.let { summary ->
                        showSummary(summary)
                        updateBackCard(summary) // Обновляем обратную сторону
                    } ?: showNoSummary()
                },
                onError = { error ->
                    Log.e("MainActivity", "Failed to load profile for $currentDate: $error")
                    Toast.makeText(
                        this@MainActivity,
                        "Ошибка загрузки профиля за $currentDate: $error",
                        Toast.LENGTH_SHORT
                    ).show()
                    showNoSummary()
                }
            )
        }
    }

    private fun showSummary(summary: DeviceDailySummaryResponse?) {
        if (summary == null) {
            showNoSummary()
            return
        }

        binding.summaryDataLayout.visibility = View.VISIBLE
        binding.noSummaryTextView.visibility = View.GONE

        // Отображение даты
        binding.summaryDateTextView.text = summary.date

        // Эмоция — делаем первую букву заглавной
        val emotionTranslated = SummaryAnalyzer.translateEmotion(summary.emotion)
        val emotionCapitalized = emotionTranslated.replaceFirstChar { it.uppercaseChar() }
        binding.emotionTextView.text = "Эмоция: $emotionCapitalized"


        // Уверенность
        val confidence = (summary.emotionConfidence * 100).toInt().coerceIn(0, 100)
        binding.emotionConfidenceProgress.progress = confidence
        binding.emotionConfidenceLabel.text = "Уверенность: $confidence%"

        // Причины
        val reasonsText = summary.reasons.takeIf { it.isNotEmpty() }
            ?.joinToString("\n") { "• $it" }
            ?: "Нет выявленных причин"
        binding.reasonTextView.text = reasonsText

        // Советы
        val adviceText = summary.advice.takeIf { it.isNotBlank() }
            ?: "Нет конкретных рекомендаций"
        binding.adviceTextView.text = adviceText
    }

    private fun showNoSummary() {
        val currentTime = LocalDateTime.now()
        val formattedTime = currentTime.format(DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm", Locale("ru")))

        binding.summaryDataLayout.visibility = View.GONE
        binding.noSummaryTextView.visibility = View.VISIBLE
        binding.noSummaryTextView.text = buildString {
            append("Нет доступных данных за сегодня.\n")
            append("Последнее обновление: $formattedTime")
        }
    }

    private fun showEditNameDialog() {
        val currentName = binding.childNameTextView.text.toString()

        // Создаем ViewGroup для диалога
        val dialogBinding = DialogEditNameBinding.inflate(layoutInflater)
        dialogBinding.editNameInput.setText(currentName)

        dialogBinding.errorTextView.visibility = View.GONE

        val dialog = MaterialAlertDialogBuilder(this, R.style.CustomAlertDialogTheme)
            .setTitle(R.string.edit_child_name)
            .setView(dialogBinding.root)
            .setPositiveButton(R.string.save, null)
            .setNegativeButton(R.string.cancel) { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        dialog.setOnShowListener {
            val saveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            saveButton.setOnClickListener {
                val newName = dialogBinding.editNameInput.text.toString().trim()

                // Валидация имени
                when {
                    newName.isEmpty() -> {
                        dialogBinding.errorTextView.visibility = View.VISIBLE
                        dialogBinding.errorTextView.text = getString(R.string.error_empty_name)
                    }
                    newName.length > 14 -> {
                        dialogBinding.errorTextView.visibility = View.VISIBLE
                        dialogBinding.errorTextView.text = getString(R.string.error_name_too_long)
                    }
                    newName.contains(Regex("\\d")) -> {
                        dialogBinding.errorTextView.visibility = View.VISIBLE
                        dialogBinding.errorTextView.text = getString(R.string.error_name_contains_digits)
                    }
                    newName.contains(Regex("\\s")) -> {
                        dialogBinding.errorTextView.visibility = View.VISIBLE
                        dialogBinding.errorTextView.text = getString(R.string.error_name_multiple_words)
                    }
                    !newName.matches(Regex("^[a-zA-Zа-яА-ЯёЁ]+$")) -> {
                        dialogBinding.errorTextView.visibility = View.VISIBLE
                        dialogBinding.errorTextView.text = getString(R.string.error_name_invalid_characters)
                    }
                    else -> {
                        updateChildName(newName)
                        dialog.dismiss()
                    }
                }
            }
        }
        dialog.show()
    }

    private fun updateChildName(newName: String) {
        val child = childManager.getCurrentChild() ?: return

        Log.d("MainActivity", "Начало обновления имени: childId=${child.id}, newName=$newName")

        lifecycleScope.launch {
            try {
                childManager.updateChildName(child.id, newName, onSuccess = {
                    // Обновляем имя в локальном списке детей (это уже делается внутри ChildManager)
                    binding.childNameTextView.text = newName
                    Toast.makeText(this@MainActivity, "Имя успешно обновлено", Toast.LENGTH_SHORT).show()
                    Log.d("MainActivity", "Имя успешно обновлено")
                },
                    onError = { error ->
                        Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                        Log.e("MainActivity", "Ошибка при обновлении имени: $error")
                    }
                )
            } catch (e: Exception) {
                Log.e("MainActivity", "Исключение при обновлении имени", e)
                Toast.makeText(this@MainActivity, "Ошибка: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
