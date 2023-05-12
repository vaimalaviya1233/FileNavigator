package com.w2sv.filenavigator

enum class AppNotificationChannel(val title: String) {
    STARTED_FOREGROUND_SERVICE("File Navigator is running"),
    NEW_FILE_DETECTED("Detected a new file")
}