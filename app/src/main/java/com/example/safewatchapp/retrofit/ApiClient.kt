package com.example.safewatchapp.retrofit
import android.os.Build
import com.example.safewatchapp.screen.LoginActivity
import com.example.safewatchapp.utils.TokenManager
import com.jakewharton.retrofit2.adapter.kotlin.coroutines.CoroutineCallAdapterFactory
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okhttp3.Interceptor
import okhttp3.Response
import android.content.Context
import android.content.Intent
import android.util.Log

object ApiClient {

    private const val EMULATOR_BASE_URL = "http://10.0.2.2:8080"
    private const val DEVICE_BASE_URL = "http://192.168.0.57:8080"

    private lateinit var applicationContext: Context

    fun initialize(context: Context) {
        applicationContext = context.applicationContext
    }

    private val okHttpClient: OkHttpClient by lazy {
        if (!::applicationContext.isInitialized) {
            throw IllegalStateException("ApiClient must be initialized with a context")
        }
        OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(TokenManager, applicationContext))
            .build()
    }

    private val retrofit: Retrofit by lazy {
        val baseUrl = if (isEmulator()) EMULATOR_BASE_URL else DEVICE_BASE_URL
        Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(CoroutineCallAdapterFactory())
            .build()
    }

    private fun isEmulator(): Boolean {
        return (Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"))
    }

    val authApiService: AuthApiService by lazy {
        retrofit.create(AuthApiService::class.java)
    }

    val childDeviceApiService: ChildDeviceApiService by lazy {
        retrofit.create(ChildDeviceApiService::class.java)
    }

    val childProfileApiService: ChildProfileApiService by lazy {
        retrofit.create(ChildProfileApiService::class.java)
    }

    val notificationApiService: NotificationApiService by lazy {
        retrofit.create(NotificationApiService::class.java)
    }

    val deviceLinkApiService: DeviceLinkApiService by lazy {
        retrofit.create(DeviceLinkApiService::class.java)
    }

    val deviceDataApiService: DeviceDataApiService by lazy {
        retrofit.create(DeviceDataApiService::class.java)
    }

    class AuthInterceptor(
        private val tokenManager: TokenManager,
        private val context: Context
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val originalRequest = chain.request()
            val token = tokenManager.getToken(context)

            // Создаем билдер запроса
            val requestBuilder = originalRequest.newBuilder()

            // Добавляем токен в заголовок ТОЛЬКО если он не пустой
            if (!token.isNullOrEmpty()) {
                Log.d("AuthInterceptor", "Adding token to request: ${originalRequest.url}")
                requestBuilder.header("Authorization", "Bearer $token")
            } else {
                Log.d("AuthInterceptor", "No token available for request: ${originalRequest.url}")
            }

            val request = requestBuilder.build()
            val response = chain.proceed(request)

            // Если токен истек (401), очищаем токен и логируем
            if (response.code == 401) {

                val url = originalRequest.url.toString()

                if(!url.contains("/user/login")){
                    Log.e("AuthInterceptor", "Got 401 response from $url — redirecting to login")
                    tokenManager.clearToken(context)
                    redirectToLogin()}
                else{
                    Log.d("AuthIntercepter", "401 на login — не вызываем redirect")

                }
            }

            return response
        }

        private fun redirectToLogin() {
            val intent = Intent(context, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            context.startActivity(intent)
            Log.e("SOS", "Перекидывает на LoginActivity, ВОТ В ЧЕМ ПРОБЛЕМА")
        }
    }
}