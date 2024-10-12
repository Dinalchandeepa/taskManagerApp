package com.example.productivityapp


// Implemanting reminder class
data class Reminder(
    val id: Long = System.currentTimeMillis(),
    var title: String,
    var description: String = "",
    var dateTime: Long
)