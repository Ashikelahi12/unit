package com.example.data.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.room.Room
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.Customer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BillingReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            ReminderScheduler.scheduleDailyReminder(context)
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "utility_calc_db"
                ).build()

                val customers = db.customerDao().getAllCustomersDirect()
                val now = System.currentTimeMillis()

                customers.forEach { customer ->
                    val expectedDate = customer.nextExpectedDate
                    val daysDiff = getDaysDifference(now, expectedDate)

                    val title: String
                    val message: String
                    var showNotification = false

                    if (daysDiff == 0) {
                        // Due today
                        title = "Reading Due Today 📅"
                        message = "The meter reading for ${customer.name} is due today!"
                        showNotification = true
                    } else if (daysDiff == 3) {
                        // Due in 3 days
                        title = "Upcoming Billing Reminder 🔔"
                        message = "The meter reading for ${customer.name} is due in 3 days."
                        showNotification = true
                    } else if (daysDiff < 0) {
                        // Overdue
                        val daysLate = -daysDiff
                        title = "Billing Overdue! ⚠️"
                        message = "${customer.name}'s meter reading is overdue by $daysLate days!"
                        showNotification = true
                    } else {
                        // Future date not yet 3 days
                        title = ""
                        message = ""
                    }

                    if (showNotification) {
                        showNotification(context, customer.id, title, message)
                    }
                }

                db.close()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun getDaysDifference(currentMs: Long, targetMs: Long): Int {
        val calCurrent = Calendar.getInstance().apply {
            timeInMillis = currentMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calTarget = Calendar.getInstance().apply {
            timeInMillis = targetMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val diffMs = calTarget.timeInMillis - calCurrent.timeInMillis
        return (diffMs / (1000 * 60 * 60 * 24)).toInt()
    }

    private fun showNotification(context: Context, customerId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "billing_reminders"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Billing Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies when utility meter readings are due or overdue."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            customerId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Fallback standard icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        notificationManager.notify(customerId, builder.build())
    }
}
