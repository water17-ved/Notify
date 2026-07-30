package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CustomNotification
import com.example.notification.NotificationHelper
import com.example.ui.components.AddEditNotificationDialog
import com.example.ui.components.FullScreenMotivationalCover
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.CustomNotifyTheme
import com.example.ui.viewmodel.NotificationViewModel

enum class MainTab(val title: String) {
    SCHEDULED("Scheduled Alerts"),
    HISTORY("Log History"),
    SETTINGS("Settings")
}

class MainActivity : ComponentActivity() {

    private val viewModel: NotificationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Create Notification Channels
        NotificationHelper.createNotificationChannels(this)

        setContent {
            CustomNotifyTheme {
                MainAppScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(viewModel: NotificationViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }

    val isAddEditOpen by viewModel.isAddEditOpen.collectAsStateWithLifecycle()
    val notificationToEdit by viewModel.notificationToEdit.collectAsStateWithLifecycle()
    val activeCoverNotification by viewModel.activeCoverNotification.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.toastEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = when (selectedTab) {
                            0 -> "SHADOW NOTIFY ⚡"
                            1 -> "COMBAT LOGS 📜"
                            else -> "SYSTEM HUD ⚙️"
                        },
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.testTag("bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = {
                        Icon(imageVector = Icons.Default.Alarm, contentDescription = "Scheduled Alerts")
                    },
                    label = { Text("Alerts") },
                    modifier = Modifier.testTag("tab_alerts"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = {
                        Icon(imageVector = Icons.Default.History, contentDescription = "Log History")
                    },
                    label = { Text("History") },
                    modifier = Modifier.testTag("tab_history"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = {
                        Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings")
                    },
                    label = { Text("Settings") },
                    modifier = Modifier.testTag("tab_settings"),
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary
                    )
                )
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    viewModel = viewModel,
                    onOpenAddDialog = { viewModel.openAddDialog() }
                )
                1 -> HistoryScreen(viewModel = viewModel)
                2 -> SettingsScreen(viewModel = viewModel)
            }
        }
    }

    // Add / Edit Dialog Sheet
    if (isAddEditOpen) {
        AddEditNotificationDialog(
            notificationToEdit = notificationToEdit,
            onDismiss = { viewModel.closeAddEditDialog() },
            onSave = { id, title, message, triggerTimeMillis, repeatMode, category, priority, soundEnabled, vibrateEnabled ->
                viewModel.saveNotification(
                    id = id,
                    title = title,
                    message = message,
                    triggerTimeMillis = triggerTimeMillis,
                    repeatMode = repeatMode,
                    category = category,
                    priority = priority,
                    soundEnabled = soundEnabled,
                    vibrateEnabled = vibrateEnabled
                )
            }
        )
    }

    // Full Screen Motivational Cover Overlay
    activeCoverNotification?.let { coverNotif ->
        FullScreenMotivationalCover(
            notification = coverNotif,
            onDismiss = { viewModel.dismissCoverNotification() },
            onSnooze = { viewModel.snoozeCoverNotification(coverNotif) }
        )
    }
}
