package com.example.productivityapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import android.os.Handler
import android.os.Looper

class TimerActivity : AppCompatActivity() {

    // UI components
    private lateinit var timerTextView: TextView
    private lateinit var startPauseButton: Button
    private lateinit var resetButton: Button
    private lateinit var durationEditText: EditText
    private lateinit var savePresetButton: Button
    private lateinit var presetSpinner: Spinner

    // Timer variables
    private var timer: CountDownTimer? = null
    private var isTimerRunning = false
    private var timeLeftInMillis: Long = 0
    private var startTimeInMillis: Long = 0

    // Notification constants
    private val CHANNEL_ID = "TimerChannel"
    private val NOTIFICATION_ID = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timer)

        // Initialize UI elements
        timerTextView = findViewById(R.id.timerTextView)
        startPauseButton = findViewById(R.id.startPauseButton)
        resetButton = findViewById(R.id.resetButton)
        durationEditText = findViewById(R.id.durationEditText)
        savePresetButton = findViewById(R.id.savePresetButton)
        presetSpinner = findViewById(R.id.presetSpinner)

        // Set button click listeners
        startPauseButton.setOnClickListener { toggleTimer() }
        resetButton.setOnClickListener { resetTimer() }
        savePresetButton.setOnClickListener { savePreset() }

        // Load saved state and presets
        loadTimerState()
        loadPresets()
        updateCountDownText()
        createNotificationChannel()

        // Handle preset selection
        presetSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) {
                    // Retrieve selected preset and set duration
                    val selectedDuration = (parent?.getItemAtPosition(position) as? String)?.split(" ")?.get(0)?.toLongOrNull()
                    if (selectedDuration != null) {
                        durationEditText.setText(selectedDuration.toString())
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // Start or pause the timer
    private fun toggleTimer() {
        if (isTimerRunning) {
            pauseTimer()
        } else {
            startTimer()
        }
    }

    // Start the timer and update the text view
    private fun startTimer() {
        startTimeInMillis = durationEditText.text.toString().toLongOrNull()?.times(60000) ?: 0
        if (startTimeInMillis == 0L) return

        timeLeftInMillis = startTimeInMillis
        timer = object : CountDownTimer(timeLeftInMillis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateCountDownText()
            }

            override fun onFinish() {
                isTimerRunning = false
                updateButtons()
                showNotification()  // Show notification when timer finishes
                playAlarmSound()  // Play alarm sound when timer finishes
            }
        }.start()

        isTimerRunning = true
        updateButtons()
    }

    // Pause the timer
    private fun pauseTimer() {
        timer?.cancel()
        isTimerRunning = false
        updateButtons()
    }

    // Reset the timer to the original duration
    private fun resetTimer() {
        timer?.cancel()
        timeLeftInMillis = startTimeInMillis
        updateCountDownText()
        isTimerRunning = false
        updateButtons()
    }

    // Update the countdown text view with the remaining time
    private fun updateCountDownText() {
        val minutes = (timeLeftInMillis / 1000) / 60
        val seconds = (timeLeftInMillis / 1000) % 60
        val timeLeftFormatted = String.format("%02d:%02d", minutes, seconds)
        timerTextView.text = timeLeftFormatted
    }

    // Update the UI buttons depending on the timer state
    private fun updateButtons() {
        if (isTimerRunning) {
            startPauseButton.text = "Pause"
            resetButton.isEnabled = false
        } else {
            startPauseButton.text = "Start"
            resetButton.isEnabled = true
        }
    }

    // Save the current timer state when the activity stops
    private fun saveTimerState() {
        val prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putLong("startTimeInMillis", startTimeInMillis)
        editor.putLong("millisLeft", timeLeftInMillis)
        editor.putBoolean("timerRunning", isTimerRunning)
        editor.apply()
    }

    // Load the saved timer state when the activity starts
    private fun loadTimerState() {
        val prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        startTimeInMillis = prefs.getLong("startTimeInMillis", 0)
        timeLeftInMillis = prefs.getLong("millisLeft", startTimeInMillis)
        isTimerRunning = prefs.getBoolean("timerRunning", false)

        updateCountDownText()
        updateButtons()

        if (isTimerRunning) {
            startTimer()
        }
    }

    // Save a preset duration to shared preferences
    private fun savePreset() {
        val duration = durationEditText.text.toString().toLongOrNull() ?: return
        val prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        val presets = prefs.getStringSet("presets", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        presets.add("$duration minutes")
        prefs.edit().putStringSet("presets", presets).apply()
        loadPresets()  // Reload the spinner with the new preset
    }

    // Load saved presets into the spinner
    private fun loadPresets() {
        val prefs = getSharedPreferences("TimerPrefs", MODE_PRIVATE)
        val presets = prefs.getStringSet("presets", mutableSetOf())?.toMutableList() ?: mutableListOf()
        presets.add(0, "Select preset")  // Add default option
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, presets)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        presetSpinner.adapter = adapter
    }

    // Create a notification channel for timer notifications
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 500, 250, 500)
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
            Log.d("TimerActivity", "Notification channel created")
        }
    }

    // Show a notification when the timer finishes
    private fun showNotification() {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_timer)
            .setContentTitle("Timer Finished")
            .setContentText("Your timer has completed!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVibrate(longArrayOf(0, 500, 250, 500))  // Vibrate pattern
            .setAutoCancel(true)

        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, builder.build())
        Log.d("TimerActivity", "Notification shown")
    }

    // Play an alarm sound when the timer finishes
    private fun playAlarmSound() {
        var mediaPlayer: MediaPlayer? = null
        try {
            // Get the alarm sound URI
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            Log.d("TimerActivity", "Alarm URI: $notification")

            mediaPlayer = MediaPlayer()
            mediaPlayer.setDataSource(applicationContext, notification)

            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            mediaPlayer.setAudioAttributes(audioAttributes)

            mediaPlayer.prepare()
            mediaPlayer.setOnCompletionListener {
                it.release()
                Log.d("TimerActivity", "MediaPlayer released")
            }
            mediaPlayer.start()
            Log.d("TimerActivity", "Alarm sound started playing")

            // Stop the alarm after 5 seconds
            Handler(Looper.getMainLooper()).postDelayed({
                mediaPlayer?.stop()
                mediaPlayer?.release()
                Log.d("TimerActivity", "Alarm sound stopped after 5 seconds")
            }, 5000)

        } catch (e: Exception) {
            Log.e("TimerActivity", "Error playing alarm sound", e)
            mediaPlayer?.release()

            // Fallback to system notification sound
            try {
                val fallbackNotification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                val fallbackRingtone = RingtoneManager.getRingtone(applicationContext, fallbackNotification)
                fallbackRingtone.play()
                Log.d("TimerActivity", "Fallback notification sound played")
            } catch (e: Exception) {
                Log.e("TimerActivity", "Error playing fallback notification sound", e)
            }
        }
    }

    // Save the timer state when the activity is stopped
    override fun onStop() {
        super.onStop()
        saveTimerState()
    }
}
