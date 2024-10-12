package com.example.productivityapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.app.AlertDialog

class TaskListActivity : AppCompatActivity() {
    private lateinit var taskInput: EditText
    private lateinit var addButton: Button
    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private var tasks = mutableListOf<Task>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_list)

        taskInput = findViewById(R.id.taskInput)
        addButton = findViewById(R.id.addButton)
        taskRecyclerView = findViewById(R.id.taskRecyclerView)

        loadTasks()

        taskAdapter = TaskAdapter(
            tasks,
            onTaskChecked = { task -> updateTask(task) },
            onTaskEdit = { task -> showEditDialog(task) },
            onTaskDelete = { task -> deleteTask(task) }
        )

        taskRecyclerView.layoutManager = LinearLayoutManager(this)
        taskRecyclerView.adapter = taskAdapter

        addButton.setOnClickListener {
            val taskTitle = taskInput.text.toString()
            if (taskTitle.isNotEmpty()) {
                val newTask = Task(id = System.currentTimeMillis(), title = taskTitle)
                addTask(newTask)
                taskInput.text.clear()
            }
        }
    }

    private fun loadTasks() {
        tasks = TaskPreferences.getTasks(this).toMutableList()
        if (tasks.isEmpty()) {
            tasks = mutableListOf()
        }
    }

    private fun saveTasks() {
        TaskPreferences.saveTasks(this, tasks)
    }

    private fun addTask(task: Task) {
        tasks.add(task)
        taskAdapter.notifyItemInserted(tasks.size - 1)
        saveTasks()
    }

    private fun updateTask(task: Task) {
        val index = tasks.indexOfFirst { it.id == task.id }
        if (index != -1) {
            tasks[index] = task
            taskAdapter.notifyItemChanged(index)
            saveTasks()
        }
    }

    private fun showEditDialog(task: Task) {
        val editText = EditText(this).apply { setText(task.title) }
        AlertDialog.Builder(this)
            .setTitle("Edit Task")
            .setView(editText)
            .setPositiveButton("Save") { _, _ ->
                task.title = editText.text.toString()
                updateTask(task)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteTask(task: Task) {
        AlertDialog.Builder(this)
            .setTitle("Delete Task")
            .setMessage("Are you sure you want to delete this task?")
            .setPositiveButton("Yes") { _, _ ->
                val index = tasks.indexOfFirst { it.id == task.id }
                if (index != -1) {
                    tasks.removeAt(index)
                    taskAdapter.notifyItemRemoved(index)
                    saveTasks()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}