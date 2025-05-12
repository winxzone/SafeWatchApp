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
import androidx.core.view.GravityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.safewatchapp.R
import com.example.safewatchapp.databinding.DialogEditNameBinding
import com.example.safewatchapp.databinding.DrawerHeaderBinding
import com.example.safewatchapp.databinding.MainBinding
import com.example.safewatchapp.utils.TokenManager
import com.example.safewatchapp.screen.fragments.ChildPhotoDialogFragment
import com.example.safewatchapp.retrofit.ApiClient
import com.example.safewatchapp.manager.ChildManager
import com.example.safewatchapp.utils.handlers.NavigationHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.io.File

class MainActivity : AppCompatActivity() {
    private lateinit var binding: MainBinding
    private lateinit var navigationHandler: NavigationHandler
    private lateinit var childManager: ChildManager
    private var userProfileFetched = false
    private var childrenFetched = false

    // TODO: 04.05 Сделать валидацию на имя ребенка

    // todo: после добавления ребенка нужно автоматически обновлять список в главном меню
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = MainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        navigationHandler = NavigationHandler(this, binding)
        navigationHandler.setupNavigationMenu()

        // Инициализируем ChildManager с контекстом для возможности использования SharedPreferences
        childManager = ChildManager(this)

//        if(RoleManager.isChild(this)){
//            checkPermissionsAndRedirect()
//        } else{
//            Log.d("MainActivity", "ROle Parent, skip checkPermissions")
//        }

        setupUI()
        setupListeners()
        setupFragmentResultListener()
        setupBackPressedHandler()
        loadData()
    }

    override fun onResume() {
        super.onResume()
        // Даже если данные уже загружены из кэша, мы все равно пытаемся обновить их с сервера
        val token = TokenManager.getToken(this)
        if (token != null) {
            fetchChildren()  // Метод fetchChildren теперь сам решает, нужно ли обращаться к серверу
            if (!userProfileFetched) fetchUser()
        }
    }

    private fun setupUI(){
        enableEdgeToEdge()
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.navigationView) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun loadData(){
        val token = TokenManager.getToken(this)
        if (token != null) {
            fetchChildren()
            fetchUser()
        } else {
            Toast.makeText(this, "Token not found", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    // todo: проверка разрешений при входе
//    private fun checkPermissionsAndRedirect() {
//        val childDeviceId = DeviceManager.getChildDeviceId(this)
//        val prefs = getSharedPreferences("ScreenCapturePrefs", MODE_PRIVATE)
//        val hasMediaProjection = prefs.getBoolean("hasMediaProjection", false)
//
////        if (childDeviceId?.isNotEmpty() != true || !hasMediaProjection) {
////            Log.d("MainActivity", "Отсутствуют разрешения или childDeviceId, открываем PermissionsActivity")
////            startActivity(Intent(this, PermissionsActivity::class.java))
////            finish()
////        } else {
////            Log.d("MainActivity", "Все разрешения на месте для Child")
////        }
//    }

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

        binding.childPhotoImageView.setOnClickListener {
            val currentChildId = childManager.getCurrentChild()?.id
            if (currentChildId != null) {
                openChildPhotoDialog(currentChildId)
            }
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            fetchChildren(true)
        }

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
                        val headerBinding = DrawerHeaderBinding.bind(binding.navigationView.getHeaderView(0))
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

    private fun displayChildProfile() {
        val child = childManager.getCurrentChild()
        if (child != null) {
            binding.childNameTextView.text = child.name

            // Проверяем, есть ли фото в кэше
            val cachedPhoto = childManager.getCachedPhoto(child.id)
            if (cachedPhoto != null) {
                binding.childPhotoImageView.setImageBitmap(cachedPhoto)
            } else {
                TokenManager.getToken(this) ?: return
                lifecycleScope.launch {
                    try {
                        childManager.getChildProfilePhoto(child.id, onSuccess = { bitmap ->
                            binding.childPhotoImageView.setImageBitmap(bitmap)
                        },
                            onError = { error ->
                                Toast.makeText(this@MainActivity, error, Toast.LENGTH_SHORT).show()
                            }
                        )
                    } catch (e: Exception) {
                        Log.e("MainActivity", "Error loading photo", e)
                        Toast.makeText(this@MainActivity, "Error loading photo: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
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
                if (newName.isNotEmpty()) {
                    updateChildName(newName)
                    dialog.dismiss()
                } else {
                    // Отображаем сообщение об ошибке
                    dialogBinding.errorTextView.visibility = View.VISIBLE
                    dialogBinding.errorTextView.text = getString(R.string.error_empty_name)
                }
            }
        }
        dialog.show()
    }

    private fun updateChildName(newName: String) {
        TokenManager.getToken(this) ?: return
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
