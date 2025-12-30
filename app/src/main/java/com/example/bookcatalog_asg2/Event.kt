package com.example.bookcatalog_asg2

data class Event(
    val id: String = "",
    val title: String = "",
    val dateTime: String = "",
    val venue: String = "",
    val overview: String = "",
    val highlights: String = "",
    val organizerName: String = "",
    val category: String = "",
    val imageRef: String = "",
    val createdBy: String = "",

    val featured: Boolean = false,
    val viewCount: Long = 0
)
