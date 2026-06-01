package com.mohammed.pdfreader.data.model

import androidx.room.*

// ===================================================
// SearchResult — result of full-text search in a PDF
// ===================================================
data class SearchResult(
    val fileId: Long,
    val fileName: String,
    val pageNumber: Int,
    val snippet: String,       // text snippet with the match highlighted
    val matchStart: Int,       // char index of match start in snippet
    val matchEnd: Int,         // char index of match end in snippet
    val totalMatches: Int = 1
)

// ===================================================
// VocabularyWord — German word with Arabic translation
// ===================================================
@Entity(tableName = "vocabulary_words")
data class VocabularyWord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val german: String,
    val arabic: String,
    val wordType: String = "",     // Nomen, Verb, Adjektiv, Adverb, Andere
    val difficulty: String = "B1", // A1, A2, B1, B2, C1, C2
    val example: String = "",
    val exampleTranslation: String = "",
    val plural: String = "",
    val conjugation: String = "",  // JSON string for verb forms
    val ipaPhonetics: String = "",
    val addedAt: Long = System.currentTimeMillis(),
    val reviewCount: Int = 0,
    val correctCount: Int = 0,
    val nextReviewAt: Long = 0,    // Spaced repetition timestamp
    val sourceFileId: Long? = null // which PDF this word came from
)

// ===================================================
// Bookmark
// ===================================================
@Entity(tableName = "bookmarks")
data class Bookmark(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfFileId: Long,
    val pageNumber: Int,
    val title: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val color: String = "#3B82F6"
)

// ===================================================
// ReadingSession — for stats/history
// ===================================================
@Entity(tableName = "reading_sessions")
data class ReadingSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pdfFileId: Long,
    val startTime: Long,
    val endTime: Long,
    val pagesRead: Int,
    val date: String // yyyy-MM-dd
)
