package com.mohammed.pdfreader.utils

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import com.mohammed.pdfreader.data.model.VocabularyWord
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@Singleton
class AIManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // =========================================================
    // SUMMARIZATION
    // =========================================================

    suspend fun summarizePage(text: String, lang: String = "ar", style: SummaryStyle = SummaryStyle.BULLETS): String {
        return withContext(Dispatchers.Default) {
            if (text.isBlank()) return@withContext "لا يوجد نص للتلخيص"

            val sentences = text.split(Regex("[.!?؟،]"))
                .map { it.trim() }
                .filter { it.length > 20 }

            if (sentences.isEmpty()) return@withContext "النص قصير جداً للتلخيص"

            // Simple extractive summarization — pick key sentences
            val scored = sentences.mapIndexed { idx, s ->
                val posScore = when {
                    idx == 0 -> 3.0
                    idx == sentences.size - 1 -> 2.0
                    else -> 1.0
                }
                val lengthScore = minOf(s.length / 50.0, 2.0)
                s to (posScore + lengthScore)
            }.sortedByDescending { it.second }

            val topCount = when (style) {
                SummaryStyle.SHORT -> 3
                SummaryStyle.MEDIUM -> 5
                SummaryStyle.DETAILED -> 8
                SummaryStyle.BULLETS -> 5
            }

            val summary = scored.take(topCount).map { it.first }

            return@withContext when (style) {
                SummaryStyle.BULLETS -> summary.joinToString("\n") { "• $it" }
                else -> summary.joinToString(". ") + "."
            }
        }
    }

    suspend fun summarizeDocument(pages: List<String>, lang: String = "ar"): String {
        return withContext(Dispatchers.Default) {
            val combined = pages.joinToString("\n\n")
            summarizePage(combined, lang, SummaryStyle.DETAILED)
        }
    }

    // =========================================================
    // Q&A on document
    // =========================================================

    suspend fun answerQuestion(question: String, documentText: String, lang: String = "ar"): QAResult {
        return withContext(Dispatchers.Default) {
            if (documentText.isBlank()) {
                return@withContext QAResult("لا يوجد محتوى في المستند", -1)
            }

            // Simple keyword matching + sentence extraction
            val keywords = extractKeywords(question)
            val sentences = documentText.split(Regex("[.!?؟]"))
                .mapIndexed { idx, s -> idx to s.trim() }
                .filter { it.second.length > 15 }

            // Score sentences by keyword overlap
            val scored = sentences.map { (idx, sentence) ->
                val score = keywords.count { kw ->
                    sentence.contains(kw, ignoreCase = true)
                }
                Triple(idx, sentence, score)
            }.filter { it.third > 0 }.sortedByDescending { it.third }

            if (scored.isEmpty()) {
                return@withContext QAResult(
                    "لم أجد إجابة مباشرة لهذا السؤال في المستند.",
                    -1
                )
            }

            val best = scored.first()
            val context = scored.take(3).joinToString(" ... ") { it.second }

            // Estimate page number (roughly 3000 chars per page)
            val estimatedPage = best.first / 15 + 1

            QAResult(
                answer = context,
                pageNumber = estimatedPage,
                confidence = minOf(best.third.toFloat() / keywords.size, 1f)
            )
        }
    }

    private fun extractKeywords(text: String): List<String> {
        val stopWords = setOf("ما", "هو", "هي", "في", "من", "على", "أن", "إن", "the", "is", "what", "how", "der", "die", "das", "ist", "wie")
        return text.split(Regex("[\\s،,;?؟!]"))
            .map { it.trim() }
            .filter { it.length > 2 && !stopWords.contains(it.lowercase()) }
    }

    // =========================================================
    // VOCABULARY EXTRACTION
    // =========================================================

    suspend fun extractGermanVocabulary(text: String): List<VocabularyWord> {
        return withContext(Dispatchers.Default) {
            val words = text.split(Regex("[\\s،,;:.!?؟()\\[\\]\"']"))
                .map { it.trim() }
                .filter { it.length > 2 }
                .distinct()

            // Filter German-looking words (contain umlauts or common patterns)
            val germanWords = words.filter { word ->
                word.any { it in "äöüÄÖÜß" } ||
                (word.first().isUpperCase() && word.length > 3) || // German nouns are capitalized
                word.endsWith("ung") ||
                word.endsWith("keit") ||
                word.endsWith("heit") || word.endsWith("lich") ||
                word.endsWith("isch") || word.endsWith("en") ||
                word.endsWith("er") ||
                word.endsWith("ieren")
            }.take(50) // limit

            germanWords.mapNotNull { word ->
                try {
                    val type = guessWordType(word)
                    val difficulty = guessDifficulty(word)
                    VocabularyWord(
                        german = word,
                        arabic = "", // will be filled by TranslationManager
                        wordType = type,
                        difficulty = difficulty,
                        example = "",
                        addedAt = System.currentTimeMillis(),
                        reviewCount = 0
                    )
                } catch (e: Exception) { null }
            }
        }
    }

    private fun guessWordType(word: String): String {
        return when {
            word.first().isUpperCase() -> "Nomen"
            word.endsWith("ieren") ||
            word.endsWith("en") -> "Verb"
            word.endsWith("lich") || word.endsWith("isch") ||
            word.endsWith("ig") -> "Adjektiv"
            word.endsWith("lich") -> "Adverb"
            else -> "Andere"
        }
    }

    private fun guessDifficulty(word: String): String {
        val commonA1 = setOf("Hallo", "Danke", "Bitte", "Ja", "Nein", "Haus", "Mann", "Frau", "Kind", "Tag")
        return when {
            commonA1.contains(word) -> "A1"
            word.length <= 5 -> "A2"
            word.length <= 8 -> "B1"
            word.contains("schaft") ||
            word.contains("keit") || word.contains("heit") -> "B2"
            word.length > 12 -> "C1"
            else -> "B1"
        }
    }

    // =========================================================
    // SMART TEXT DETECTION
    // =========================================================

    fun detectLanguage(text: String): String {
        val arabicChars = text.count { it in '\u0600'..'\u06FF' }
        val latinChars = text.count { it.isLetter() && it !in '\u0600'..'\u06FF' }
        val hasUmlauts = text.any { it in "äöüÄÖÜß" }

        return when {
            arabicChars > latinChars * 2 -> "ar"
            hasUmlauts ||
            latinChars > arabicChars * 2 -> "de"
            latinChars > arabicChars -> "en"
            else -> "auto"
        }
    }

    fun splitIntoSentences(text: String): List<String> {
        return text.split(Regex("[.!?؟]\n?"))
            .map { it.trim() }
            .filter { it.length > 5 }
    }
}

data class QAResult(
    val answer: String,
    val pageNumber: Int,
    val confidence: Float = 0f
)

enum class SummaryStyle { SHORT, MEDIUM, DETAILED, BULLETS }
