package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.data.repository.PdfRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

data class StatsUiState(
    val totalPagesRead: Int = 0,
    val totalMinutesRead: Int = 0,
    val totalFilesRead: Int = 0,
    val todayPages: Int = 0,
    val todayMinutes: Int = 0,
    val weekPages: Int = 0,
    val weeklyActivity: List<Int> = emptyList(),  // pages per day, last 7 days
    val weeklyLabels: List<String> = emptyList(),
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val totalVocabWords: Int = 0,
    val masteredWords: Int = 0,
    val pendingWords: Int = 0
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val repository: PdfRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        loadStats()
    }

    private fun loadStats() {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val today = sdf.format(Date())

            // Reading sessions
            val allSessions = repository.getAllReadingSessions()
            val totalPages = allSessions.sumOf { it.pagesRead }
            val totalMinutes = allSessions.sumOf { ((it.endTime - it.startTime) / 60000).toInt() }
            val totalFiles = allSessions.map { it.pdfFileId }.distinct().size

            val todaySessions = allSessions.filter { it.date == today }
            val todayPages = todaySessions.sumOf { it.pagesRead }
            val todayMinutes = todaySessions.sumOf { ((it.endTime - it.startTime) / 60000).toInt() }

            // Last 7 days
            val cal = Calendar.getInstance()
            val labels = mutableListOf<String>()
            val activity = mutableListOf<Int>()
            val dayNames = listOf("أح", "إث", "ثل", "أر", "خم", "جم", "سب")
            for (i in 6 downTo 0) {
                cal.time = Date()
                cal.add(Calendar.DAY_OF_YEAR, -i)
                val dateStr = sdf.format(cal.time)
                val dayIdx = cal.get(Calendar.DAY_OF_WEEK) - 1
                labels.add(dayNames.getOrElse(dayIdx) { "" })
                val pages = allSessions.filter { it.date == dateStr }.sumOf { it.pagesRead }
                activity.add(pages)
            }
            val weekPages = activity.sum()

            // Streak calculation
            val uniqueDates = allSessions.map { it.date }.distinct().sorted()
            var currentStreak = 0
            var longestStreak = 0
            var tempStreak = 0
            var prevDate: Calendar? = null
            for (dateStr in uniqueDates) {
                val cur = Calendar.getInstance().apply {
                    time = sdf.parse(dateStr) ?: Date()
                }
                if (prevDate == null) {
                    tempStreak = 1
                } else {
                    val diff = ((cur.timeInMillis - prevDate.timeInMillis) / 86400000).toInt()
                    if (diff == 1) tempStreak++ else tempStreak = 1
                }
                if (tempStreak > longestStreak) longestStreak = tempStreak
                prevDate = cur
            }
            // Check if streak is current
            if (uniqueDates.isNotEmpty()) {
                val lastDate = sdf.parse(uniqueDates.last())
                val diff = ((Date().time - (lastDate?.time ?: 0)) / 86400000).toInt()
                currentStreak = if (diff <= 1) tempStreak else 0
            }

            // Vocabulary
            val vocabWords = repository.getAllVocabularyWordsOnce()
            val mastered = vocabWords.count { it.correctCount >= 3 }
            val pending = vocabWords.count { it.correctCount < 3 }

            _uiState.update {
                it.copy(
                    totalPagesRead = totalPages,
                    totalMinutesRead = totalMinutes,
                    totalFilesRead = totalFiles,
                    todayPages = todayPages,
                    todayMinutes = todayMinutes,
                    weekPages = weekPages,
                    weeklyActivity = activity,
                    weeklyLabels = labels,
                    currentStreak = currentStreak,
                    longestStreak = longestStreak,
                    totalVocabWords = vocabWords.size,
                    masteredWords = mastered,
                    pendingWords = pending
                )
            }
        }
    }
}
