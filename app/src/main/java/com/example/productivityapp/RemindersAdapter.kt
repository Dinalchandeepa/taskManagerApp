package com.example.productivityapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class RemindersAdapter(
    private val reminders: List<Reminder>,
    private val onEdit: (Reminder) -> Unit,
    private val onDelete: (Reminder) -> Unit) : RecyclerView.Adapter<RemindersAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleTextView: TextView = view.findViewById(R.id.reminderTitleTextView)
        val descriptionTextView: TextView = view.findViewById(R.id.reminderDescriptionTextView)
        val dateTimeTextView: TextView = view.findViewById(R.id.reminderDateTimeTextView)
        val editButton: View = view.findViewById(R.id.editReminderButton)
        val deleteButton: View = view.findViewById(R.id.deleteReminderButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reminder, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val reminder = reminders[position]
        holder.titleTextView.text = reminder.title
        holder.descriptionTextView.text = reminder.description
        holder.dateTimeTextView.text = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            .format(Date(reminder.dateTime))

        holder.editButton.setOnClickListener { onEdit(reminder) }
        holder.deleteButton.setOnClickListener { onDelete(reminder) }
    }

    override fun getItemCount() = reminders.size
}