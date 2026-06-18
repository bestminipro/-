package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isSystem: Boolean = false
)

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String
)

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val categoryId: Int,
    val filePath: String,
    val fileSize: String,
    val totalPages: Int,
    val dateAdded: String = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date()),
    val contentText: String, // Stored content textual representations
    val isFavorite: Boolean = false,
    val tags: String = "" // comma separated tags (e.g. "برق,استاندارد")
)

@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val pageNumber: Int,
    val title: String,
    val note: String,
    val dateAdded: String = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
)

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val pageNumber: Int,
    val text: String,
    val dateAdded: String = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
)

@Entity(tableName = "highlights")
data class Highlight(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val pageNumber: Int,
    val highlightedText: String,
    val colorHex: String, // #FFFF00 (Yellow), #00FF00 (Green), #0000FF (Blue), #FF0000 (Red)
    val dateAdded: String = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
)

@Entity(tableName = "reading_history")
data class ReadingHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val documentId: Int,
    val lastPage: Int,
    val progress: Float, // 0f to 1f
    val lastReadTime: Long = System.currentTimeMillis(),
    val durationSeconds: Long = 0
)

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val id: Int = 1, // Single row configuration
    val themeMode: String = "SYSTEM", // SYSTEM, LIGHT, DARK
    val fontSize: String = "MEDIUM", // SMALL, MEDIUM, LARGE
    val appLanguage: String = "FA", // FA, EN
    val securePin: String = "", // empty if disabled
    val isBiometricEnabled: Boolean = false
)
