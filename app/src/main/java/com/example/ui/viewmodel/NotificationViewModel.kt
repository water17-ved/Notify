package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.model.CustomNotification
import com.example.data.model.NotificationCategory
import com.example.data.model.NotificationLog
import com.example.data.model.NotificationPriority
import com.example.data.model.RepeatMode
import com.example.data.repository.NotificationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

enum class QuickPreset(val label: String, val subtitle: String) {
    IN_15_MINS("In 15 Minutes", "Quick reminder"),
    IN_1_HOUR("In 1 Hour", "Focus check-in"),
    TONIGHT_8PM("Tonight 8 PM", "Evening summary"),
    TOMORROW_9AM("Tomorrow 9 AM", "Morning start")
}

class NotificationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: NotificationRepository

    init {
        val db = AppDatabase.getDatabase(application)
        repository = NotificationRepository(application, db.notificationDao(), db.logDao())
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<NotificationCategory?>(null)
    val selectedCategory: StateFlow<NotificationCategory?> = _selectedCategory.asStateFlow()

    private val _isAddEditOpen = MutableStateFlow(false)
    val isAddEditOpen: StateFlow<Boolean> = _isAddEditOpen.asStateFlow()

    private val _notificationToEdit = MutableStateFlow<CustomNotification?>(null)
    val notificationToEdit: StateFlow<CustomNotification?> = _notificationToEdit.asStateFlow()

    private val _toastEvent = MutableSharedFlow<String>()
    val toastEvent: SharedFlow<String> = _toastEvent.asSharedFlow()

    val notifications: StateFlow<List<CustomNotification>> = combine(
        repository.allNotifications,
        _searchQuery,
        _selectedCategory
    ) { items, query, category ->
        items.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.title.contains(query, ignoreCase = true) ||
                item.message.contains(query, ignoreCase = true)
            val matchesCategory = category == null || item.category == category
            matchesQuery && matchesCategory
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val logs: StateFlow<List<NotificationLog>> = repository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _isAiGenerating = MutableStateFlow(false)
    val isAiGenerating: StateFlow<Boolean> = _isAiGenerating.asStateFlow()

    private val _aiGeneratedResult = MutableStateFlow<com.example.data.remote.AiNotificationResult?>(null)
    val aiGeneratedResult: StateFlow<com.example.data.remote.AiNotificationResult?> = _aiGeneratedResult.asStateFlow()

    private val _fullScreenCoverEnabled = MutableStateFlow(true)
    val fullScreenCoverEnabled: StateFlow<Boolean> = _fullScreenCoverEnabled.asStateFlow()

    private val _activeCoverNotification = MutableStateFlow<CustomNotification?>(null)
    val activeCoverNotification: StateFlow<CustomNotification?> = _activeCoverNotification.asStateFlow()

    fun toggleFullScreenCover(enabled: Boolean) {
        _fullScreenCoverEnabled.value = enabled
        viewModelScope.launch {
            val msg = if (enabled) "Full-screen motivational cover ENABLED ⚡" else "Full-screen cover DISABLED"
            _toastEvent.emit(msg)
        }
    }

    fun showCoverNotification(notification: CustomNotification) {
        _activeCoverNotification.value = notification
    }

    fun dismissCoverNotification() {
        _activeCoverNotification.value = null
    }

    fun snoozeCoverNotification(notification: CustomNotification) {
        dismissCoverNotification()
        viewModelScope.launch {
            val snoozedItem = notification.copy(
                id = 0,
                triggerTimeMillis = System.currentTimeMillis() + (10 * 60 * 1000L)
            )
            repository.saveNotification(snoozedItem)
            _toastEvent.emit("⏰ Mission snoozed for 10 minutes!")
        }
    }

    fun generateAiPersonalizedNotification(userContext: String, onSuccess: ((com.example.data.remote.AiNotificationResult) -> Unit)? = null) {
        if (userContext.isBlank()) return
        viewModelScope.launch {
            _isAiGenerating.value = true
            val result = com.example.data.remote.GeminiNotificationService.generatePersonalizedNotification(userContext)
            _isAiGenerating.value = false

            result.onSuccess { aiRes ->
                _aiGeneratedResult.value = aiRes
                onSuccess?.invoke(aiRes)
                _toastEvent.emit("✨ AI generated personalized notification!")
            }.onFailure { err ->
                _toastEvent.emit("AI Error: ${err.localizedMessage ?: "Failed to generate AI notification"}")
            }
        }
    }

    fun sendInstantAiNotification(userContext: String) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            val result = com.example.data.remote.GeminiNotificationService.generatePersonalizedNotification(userContext)
            _isAiGenerating.value = false

            result.onSuccess { aiRes ->
                val tempItem = CustomNotification(
                    title = aiRes.title,
                    message = aiRes.message,
                    triggerTimeMillis = System.currentTimeMillis() + 1000,
                    category = NotificationCategory.GENERAL,
                    priority = NotificationPriority.HIGH
                )
                repository.triggerTestNotification(tempItem)
                if (_fullScreenCoverEnabled.value) {
                    _activeCoverNotification.value = tempItem
                }
                _toastEvent.emit("⚡ AI Personalized Notification sent!")
            }.onFailure { err ->
                _toastEvent.emit("AI Error: ${err.localizedMessage ?: "Failed to generate AI notification"}")
            }
        }
    }

    fun scheduleAiNotification(userContext: String, delayMinutes: Int = 5) {
        viewModelScope.launch {
            _isAiGenerating.value = true
            val result = com.example.data.remote.GeminiNotificationService.generatePersonalizedNotification(userContext)
            _isAiGenerating.value = false

            result.onSuccess { aiRes ->
                val targetMillis = System.currentTimeMillis() + (delayMinutes * 60 * 1000L)
                val item = CustomNotification(
                    title = aiRes.title,
                    message = aiRes.message,
                    triggerTimeMillis = targetMillis,
                    category = NotificationCategory.GENERAL,
                    priority = NotificationPriority.HIGH
                )
                repository.saveNotification(item)
                _toastEvent.emit("⚡ AI Notification scheduled in $delayMinutes min!")
            }.onFailure { err ->
                _toastEvent.emit("AI Error: ${err.localizedMessage ?: "Failed to generate AI notification"}")
            }
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategory(category: NotificationCategory?) {
        _selectedCategory.value = category
    }

    fun openAddDialog() {
        _notificationToEdit.value = null
        _isAddEditOpen.value = true
    }

    fun openEditDialog(notification: CustomNotification) {
        _notificationToEdit.value = notification
        _isAddEditOpen.value = true
    }

    fun closeAddEditDialog() {
        _isAddEditOpen.value = false
        _notificationToEdit.value = null
    }

    fun saveNotification(
        id: Long = 0,
        title: String,
        message: String,
        triggerTimeMillis: Long,
        repeatMode: RepeatMode,
        category: NotificationCategory,
        priority: NotificationPriority,
        soundEnabled: Boolean,
        vibrateEnabled: Boolean
    ) {
        viewModelScope.launch {
            val item = CustomNotification(
                id = id,
                title = title.ifBlank { "Scheduled Reminder" },
                message = message.ifBlank { "Time for your scheduled custom alert." },
                triggerTimeMillis = triggerTimeMillis,
                repeatMode = repeatMode,
                category = category,
                priority = priority,
                isEnabled = true,
                soundEnabled = soundEnabled,
                vibrateEnabled = vibrateEnabled
            )
            repository.saveNotification(item)
            _toastEvent.emit("Notification scheduled successfully!")
            closeAddEditDialog()
        }
    }

    fun toggleNotification(notification: CustomNotification) {
        viewModelScope.launch {
            val nextState = !notification.isEnabled
            repository.toggleNotification(notification.id, nextState)
            val msg = if (nextState) "Notification enabled" else "Notification disabled"
            _toastEvent.emit(msg)
        }
    }

    fun deleteNotification(notification: CustomNotification) {
        viewModelScope.launch {
            repository.deleteNotification(notification)
            _toastEvent.emit("Notification deleted")
        }
    }

    fun triggerTestNotification(notification: CustomNotification) {
        viewModelScope.launch {
            repository.triggerTestNotification(notification)
            if (_fullScreenCoverEnabled.value) {
                _activeCoverNotification.value = notification
            }
            _toastEvent.emit("Test notification sent!")
        }
    }

    fun applyQuickPreset(preset: QuickPreset) {
        viewModelScope.launch {
            val now = Calendar.getInstance()
            val (title, msg, targetMillis) = when (preset) {
                QuickPreset.IN_15_MINS -> Triple(
                    "15-Minute Reminder",
                    "Take a quick drink of water or stretch break!",
                    now.timeInMillis + (15 * 60 * 1000)
                )
                QuickPreset.IN_1_HOUR -> Triple(
                    "1-Hour Follow Up",
                    "Check back on your ongoing work or pending task.",
                    now.timeInMillis + (60 * 60 * 1000)
                )
                QuickPreset.TONIGHT_8PM -> {
                    val cal = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, 20)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
                    }
                    Triple(
                        "Evening Review",
                        "Review today's achievements and prepare for tomorrow.",
                        cal.timeInMillis
                    )
                }
                QuickPreset.TOMORROW_9AM -> {
                    val cal = Calendar.getInstance().apply {
                        add(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 9)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                    }
                    Triple(
                        "Morning Goal",
                        "Start your primary goal for the day with full focus!",
                        cal.timeInMillis
                    )
                }
            }

            val item = CustomNotification(
                title = title,
                message = msg,
                triggerTimeMillis = targetMillis,
                repeatMode = RepeatMode.ONCE,
                category = NotificationCategory.GENERAL,
                priority = NotificationPriority.HIGH
            )
            repository.saveNotification(item)
            _toastEvent.emit("Preset '${preset.label}' scheduled!")
        }
    }

    fun clearLogs() {
        viewModelScope.launch {
            repository.clearLogs()
            _toastEvent.emit("History logs cleared")
        }
    }
}
