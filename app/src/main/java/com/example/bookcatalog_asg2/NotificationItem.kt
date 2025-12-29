package com.example.bookcatalog_asg2

sealed class NotificationItem {

    // Section header (Upcoming / Recent)
    data class Header(
        val title: String
    ) : NotificationItem()

    // Event notification item
    data class EventItem(
        val event: Event
    ) : NotificationItem()
}
