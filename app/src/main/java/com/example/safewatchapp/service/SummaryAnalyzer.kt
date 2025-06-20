package com.example.safewatchapp.service

import com.example.safewatchapp.models.DeviceDailySummaryResponse
import android.util.Log

object SummaryAnalyzer {

    private val EMOTION_TRANSLATIONS = mapOf(
        "anxiety" to "тревога",
        "stress" to "стресс",
        "anger" to "раздражение",
        "disgust" to "отвращение",
        "sadness" to "грусть",
        "guilt" to "вина",
        "neutral" to "нейтрально",
        "joy" to "радость"
    )

    private data class Thresholds(
        val highScreenTime: Long = 4 * 60 * 60 * 1000, // 4 часа
        val criticalScreenTime: Long = 6 * 60 * 60 * 1000, // 6 часов
        val lowConfidence: Double = 0.5,
        val mediumConfidence: Double = 0.7
    )

    private val thresholds = Thresholds()

    fun translateEmotion(englishEmotion: String?): String {
        if (englishEmotion.isNullOrBlank()) return "неизвестно"
        val key = englishEmotion.lowercase().trim()
        Log.d("EmotionTranslation", "Input: $englishEmotion, Output: ${EMOTION_TRANSLATIONS[englishEmotion?.lowercase()]}")
        return EMOTION_TRANSLATIONS[key] ?: "неизвестно"
    }

    fun generateReasons(summary: DeviceDailySummaryResponse): List<String> {
        val reasons = mutableListOf<String>()
        val screenTimeHours = summary.totalScreenTime.toHours()

        if (summary.usedAtNight) {
            reasons.add("📱 Использование устройства в ночное время")
        }

        when {
            summary.totalScreenTime > thresholds.criticalScreenTime -> {
                reasons.add("⏰ Критически высокое время использования экрана (${screenTimeHours} ч)")
            }
            summary.totalScreenTime > thresholds.highScreenTime -> {
                reasons.add("📊 Повышенное время использования экрана (${screenTimeHours} ч)")
            }
        }

        if (summary.screenUnlockCount > 80) {
            reasons.add("🔓 Частые разблокировки устройства (${summary.screenUnlockCount} раз)")
        }

        if (summary.notificationsCount > 50) {
            reasons.add("🔔 Высокая активность уведомлений (${summary.notificationsCount})")
        }

        when {
            summary.emotionConfidence < thresholds.lowConfidence -> {
                reasons.add("⚠️ Очень низкая точность анализа эмоционального состояния")
            }
            summary.emotionConfidence < thresholds.mediumConfidence -> {
                reasons.add("📉 Средняя точность анализа эмоционального состояния")
            }
        }

        if (summary.topAppPackage.containsSocialMedia()) {
            reasons.add("📱 Активное использование социальных сетей")
        }

        return reasons
    }

    fun generateAdvice(summary: DeviceDailySummaryResponse): String {
        val emotion = translateEmotion(summary.emotion)
        val screenTimeHours = summary.totalScreenTime.toHours()

        return when {
            emotion in listOf("тревога", "стресс") && summary.usedAtNight -> {
                """
                🌙 Помогите ребенку наладить сон:
                • Установите "комендантский час" для гаджетов за 1-2 часа до сна
                • Создайте зону без телефонов в спальне ребенка
                • Введите вечерний ритуал: чтение, спокойная музыка
                """.trimIndent()
            }

            screenTimeHours > 6 -> {
                """
                ⏰ Стратегии снижения экранного времени:
                • Договоритесь о "цифровых перерывах" каждый час
                • Предложите интересные офлайн-активности (спорт, творчество)
                • Установите семейное время без гаджетов (обеды, прогулки)
                """.trimIndent()
            }

            emotion == "раздражение" && summary.notificationsCount > 50 -> {
                """
                🔕 Научите ребенка управлять уведомлениями:
                • Вместе просмотрите настройки уведомлений и отключите лишние
                • Объясните разницу между важными и развлекательными уведомлениями
                • Установите "тихие часы" во время учебы и сна
                """.trimIndent()
            }

            summary.screenUnlockCount > 80 -> {
                """
                🎯 Помогите снизить зависимость от телефона:
                • Вместе уберите соцсети с главного экрана
                • Предложите оставлять телефон в другой комнате во время учебы
                • Научите техникам осознанности: "зачем я взял телефон?"
                """.trimIndent()
            }

            emotion in listOf("радость", "нейтрально") && screenTimeHours < 3 -> {
                """
                ✅ Отличные цифровые привычки у ребенка!
                • Похвалите за сбалансированное использование технологий
                • Поддержите интерес к офлайн-хобби
                • Обсудите, как ребенок достигает такого баланса
                """.trimIndent()
            }

            emotion in listOf("грусть", "вина") -> {
                """
                💙 Поддержите ребенка эмоционально:
                • Проведите открытый разговор о чувствах без осуждения
                • Ограничьте время в соцсетях, где много сравнений
                • Больше живого общения: семейные игры, прогулки
                """.trimIndent()
            }

            summary.topAppPackage.containsSocialMedia() && emotion in listOf("тревога", "стресс", "грусть") -> {
                """
                📱 Здоровое отношение к социальным сетям:
                • Обсудите с ребенком, что он видит в соцсетях
                • Научите критически оценивать контент
                • Установите временные рамки для соцсетей
                """.trimIndent()
            }

            else -> {
                """
                🌟 Общие советы для родителей:
                • Станьте примером здорового использования технологий
                • Регулярно обсуждайте цифровые привычки ребенка
                • Планируйте совместные активности без экранов
                """.trimIndent()
            }
        }
    }

    private fun Long.toHours(): Long = this / (60 * 60 * 1000)

    private fun String.containsSocialMedia(): Boolean {
        val socialMediaKeywords = listOf(
            "tiktok", "instagram", "facebook", "youtube",
            "twitter", "snapchat", "telegram", "whatsapp",
            "vk", "ok.ru", "pinterest"
        )
        return socialMediaKeywords.any { this.contains(it, ignoreCase = true) }
    }
}
