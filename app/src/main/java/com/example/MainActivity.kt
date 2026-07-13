package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.room.Room
import com.example.data.db.AppDatabase
import com.example.data.receiver.ReminderScheduler
import com.example.data.repository.UtilityRepository
import com.example.ui.screens.UtilityAppUI
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.UtilityViewModel
import com.example.ui.viewmodel.UtilityViewModelFactory

class MainActivity : ComponentActivity() {

    private lateinit var database: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Initialize Database on App Launch
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "utility_calc_db"
        )
        .fallbackToDestructiveMigration(dropAllTables = true) // Prevent crashes if models change during rapid development
        .build()

        // 2. Initialize Repository and ViewModel
        val repository = UtilityRepository(
            customerDao = database.customerDao(),
            readingDao = database.readingDao(),
            settingsDao = database.settingsDao()
        )
        val factory = UtilityViewModelFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[UtilityViewModel::class.java]

        // 3. Schedule Inexact Repeating Daily Reminders
        ReminderScheduler.scheduleDailyReminder(this)

        // 4. Request Post Notification Permissions on Android 13+ (API 33+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
        }

        setContent {
            val settings by viewModel.settings.collectAsState()

            // 5. Dynamic Theme Selection (Database Override vs System Default)
            val useDarkTheme = when (settings.isDarkMode) {
                true -> true
                false -> false
                null -> isSystemInDarkTheme()
            }

            MyApplicationTheme(darkTheme = useDarkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    UtilityAppUI(viewModel = viewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::database.isInitialized) {
            database.close()
        }
    }
}
