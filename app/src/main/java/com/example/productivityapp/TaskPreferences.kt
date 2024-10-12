package com.example.productivityapp

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object TaskPreferences {
    private const val PREF_NAME = "TaskPreferences"
    private const val KEY_TASKS = "tasks"

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveTasks(context: Context, tasks: List<Task>) {
        val editor = getPreferences(context).edit()
        val json = Gson().toJson(tasks)
        editor.putString(KEY_TASKS, json)
        editor.apply()
    }

    fun getTasks(context: Context): List<Task> {
        val json = getPreferences(context).getString(KEY_TASKS, null)
        return if (json != null) {
            val type = object : TypeToken<List<Task>>() {}.type
            Gson().fromJson(json, type)
        } else {
            emptyList()
        }
    }
}