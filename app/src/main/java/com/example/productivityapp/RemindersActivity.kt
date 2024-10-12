package com.example.productivityapp

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.text.SimpleDateFormat
import java.util.*

class RemindersActivity : AppCompatActivity() {

    private lateinit var remindersList: RecyclerView
    private lateinit var addReminderButton: Button
    private var reminders = mutableListOf<Reminder>()
    private lateinit var adapter: RemindersAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reminders)

        remindersList = findViewById(R.id.remindersList)
        addReminderButton = findViewById(R.id.addReminderButton)

        loadReminders()

        adapter = RemindersAdapter(reminders,
            onEdit = { reminder -> showEditDialog(reminder) },
            onDelete = { reminder -> deleteReminder(reminder) }
        )
        remindersList.layoutManager = LinearLayoutManager(this)
        remindersList.adapter = adapter

        addReminderButton.setOnClickListener {
            showAddDialog()
        }
    }

    private fun showAddDialog() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        val dateTimeButton = dialogView.findViewById<Button>(R.id.dateTimeButton)

        var selectedDateTime: Calendar? = null

        dateTimeButton.setOnClickListener {
            showDateTimePicker { dateTime ->
                selectedDateTime = dateTime
                dateTimeButton.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(dateTime.time)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Add Reminder")
            .setView(dialogView)
            .setPositiveButton("Add") { _, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                if (title.isNotEmpty() && selectedDateTime != null) {
                    val reminder = Reminder(title = title, description = description, dateTime = selectedDateTime!!.timeInMillis)
                    addReminder(reminder)
                } else {
                    if (title.isEmpty()) {
                        Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                    } else if (selectedDateTime == null) {
                        Toast.makeText(this, "Please select a date and time", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showEditDialog(reminder: Reminder) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_add_reminder, null)
        val titleEditText = dialogView.findViewById<EditText>(R.id.titleEditText)
        val descriptionEditText = dialogView.findViewById<EditText>(R.id.descriptionEditText)
        val dateTimeButton = dialogView.findViewById<Button>(R.id.dateTimeButton)

        titleEditText.setText(reminder.title)
        descriptionEditText.setText(reminder.description)
        dateTimeButton.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(reminder.dateTime))

        var selectedDateTime: Calendar = Calendar.getInstance().apply { timeInMillis = reminder.dateTime }

        dateTimeButton.setOnClickListener {
            showDateTimePicker { dateTime ->
                selectedDateTime = dateTime
                dateTimeButton.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(dateTime.time)
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Edit Reminder")
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = titleEditText.text.toString()
                val description = descriptionEditText.text.toString()
                if (title.isNotEmpty()) {
                    reminder.title = title
                    reminder.description = description
                    reminder.dateTime = selectedDateTime.timeInMillis
                    updateReminder(reminder)
                } else {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDateTimePicker(callback: (Calendar) -> Unit) {
        val currentDateTime = Calendar.getInstance()
        val startYear = currentDateTime.get(Calendar.YEAR)
        val startMonth = currentDateTime.get(Calendar.MONTH)
        val startDay = currentDateTime.get(Calendar.DAY_OF_MONTH)
        val startHour = currentDateTime.get(Calendar.HOUR_OF_DAY)
        val startMinute = currentDateTime.get(Calendar.MINUTE)

        DatePickerDialog(this, { _, year, month, day ->
            TimePickerDialog(this, { _, hour, minute ->
                val pickedDateTime = Calendar.getInstance()
                pickedDateTime.set(year, month, day, hour, minute)
                callback(pickedDateTime)
            }, startHour, startMinute, false).show()
        }, startYear, startMonth, startDay).show()
    }

    private fun addReminder(reminder: Reminder) {
        if (reminder.dateTime > System.currentTimeMillis()) {
            reminders.add(reminder)
            adapter.notifyItemInserted(reminders.size - 1)
            saveReminders()
            scheduleNotification(reminder)
        } else {
            Toast.makeText(this, "Cannot add reminders for past dates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateReminder(reminder: Reminder) {
        val index = reminders.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            if (reminder.dateTime > System.currentTimeMillis()) {
                reminders[index] = reminder
                adapter.notifyItemChanged(index)
                saveReminders()
                scheduleNotification(reminder)
            } else {
                deleteReminder(reminder)
                Toast.makeText(this, "Reminder updated to a past date and removed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun deleteReminder(reminder: Reminder) {
        val index = reminders.indexOfFirst { it.id == reminder.id }
        if (index != -1) {
            reminders.removeAt(index)
            adapter.notifyItemRemoved(index)
            saveReminders()
            cancelNotification(reminder)
        }
    }

    private fun removeExpiredReminders() {
        val currentTime = System.currentTimeMillis()
        val expiredReminders = reminders.filter { it.dateTime <= currentTime }

        if (expiredReminders.isNotEmpty()) {
            reminders.removeAll(expiredReminders)
            adapter.notifyDataSetChanged()
            saveReminders()

            expiredReminders.forEach { cancelNotification(it) }

            val expiredCount = expiredReminders.size
            Toast.makeText(this, "$expiredCount expired reminder(s) removed", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        removeExpiredReminders()
    }

    private fun saveReminders() {
        val sharedPreferences = getSharedPreferences("Reminders", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = Gson().toJson(reminders)
        editor.putString("reminders_list", json)
        editor.apply()
    }

    private fun loadReminders() {
        val sharedPreferences = getSharedPreferences("Reminders", MODE_PRIVATE)
        val json = sharedPreferences.getString("reminders_list", null)
        if (json != null) {
            val type = object : TypeToken<List<Reminder>>() {}.type
            reminders = Gson().fromJson(json, type)
        }
    }

    private fun scheduleNotification(reminder: Reminder) {
        if (reminder.dateTime > System.currentTimeMillis()) {
            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // Schedule the main notification
            val mainIntent = Intent(this, ReminderNotificationReceiver::class.java).apply {
                putExtra("REMINDER_ID", reminder.id)
                putExtra("REMINDER_TITLE", reminder.title)
                putExtra("REMINDER_DESCRIPTION", reminder.description)
                putExtra("IS_PERSISTENT", false)
            }
            val mainPendingIntent = PendingIntent.getBroadcast(
                this,
                reminder.id.toInt(),
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.dateTime,
                mainPendingIntent
            )

            // Schedule the persistent notification 10 minutes before
            val persistentIntent = Intent(this, ReminderNotificationReceiver::class.java).apply {
                putExtra("REMINDER_ID", reminder.id + 1) // Use a different ID
                putExtra("REMINDER_TITLE", "Upcoming: ${reminder.title}")
                putExtra("REMINDER_DESCRIPTION", "This reminder is due in less than 10 minutes")
                putExtra("IS_PERSISTENT", true)
            }
            val persistentPendingIntent = PendingIntent.getBroadcast(
                this,
                (reminder.id + 1).toInt(),
                persistentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                reminder.dateTime - (10 * 60 * 1000), // 10 minutes before
                persistentPendingIntent
            )
        } else {
            Toast.makeText(this, "Cannot schedule notifications for past dates", Toast.LENGTH_SHORT).show()
        }
    }

    private fun cancelNotification(reminder: Reminder) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // Cancel main notification
        val mainIntent = Intent(this, ReminderNotificationReceiver::class.java)
        val mainPendingIntent = PendingIntent.getBroadcast(
            this,
            reminder.id.toInt(),
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(mainPendingIntent)

        // Cancel persistent notification
        val persistentIntent = Intent(this, ReminderNotificationReceiver::class.java)
        val persistentPendingIntent = PendingIntent.getBroadcast(
            this,
            (reminder.id + 1).toInt(),
            persistentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(persistentPendingIntent)

        // Remove any existing notifications
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(reminder.id.toInt())
        notificationManager.cancel((reminder.id + 1).toInt())
    }
}