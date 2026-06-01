package com.mohammed.pdfreader.utils

import android.content.Context
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TranslationPair(
    val originalText: String,
    val translatedText: String,
    val sourceLang: String,
    val targetLang: String,
    val partOfSpeech: String = "",
    val phonetics: String = "",
    val example: String = ""
)

@Singleton
class TranslationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // German → Arabic
    private val deToAr by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.GERMAN)
                .setTargetLanguage(TranslateLanguage.ARABIC)
                .build()
        )
    }

    // Arabic → German
    private val arToDe by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ARABIC)
                .setTargetLanguage(TranslateLanguage.GERMAN)
                .build()
        )
    }

    // German → English (fallback)
    private val deToEn by lazy {
        Translation.getClient(
            TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.GERMAN)
                .setTargetLanguage(TranslateLanguage.ENGLISH)
                .build()
        )
    }

    // ===== Download models =====
    fun downloadModels() {
        deToAr.downloadModelIfNeeded()
        arToDe.downloadModelIfNeeded()
    }

    // ===== Translate German → Arabic =====
    suspend fun translateDeToAr(text: String): TranslationPair = withContext(Dispatchers.IO) {
        val translated = translateWith(deToAr, text)
        TranslationPair(
            originalText = text,
            translatedText = translated,
            sourceLang = "de",
            targetLang = "ar",
            partOfSpeech = guessPartOfSpeech(text),
            phonetics = getGermanPhonetics(text)
        )
    }

    // ===== Translate Arabic → German =====
    suspend fun translateArToDe(text: String): TranslationPair = withContext(Dispatchers.IO) {
        val translated = translateWith(arToDe, text)
        TranslationPair(
            originalText = text,
            translatedText = translated,
            sourceLang = "ar",
            targetLang = "de"
        )
    }

    // ===== Auto detect and translate =====
    suspend fun translateAuto(text: String, targetLang: String = "ar"): TranslationPair =
        withContext(Dispatchers.IO) {
            val isArabic = text.any { it in '\u0600'..'\u06FF' }
            if (isArabic) {
                translateArToDe(text)
            } else {
                translateDeToAr(text)
            }
        }

    // ===== Translate full page text =====
    suspend fun translatePage(pageText: String, targetLang: String = "ar"): String =
        withContext(Dispatchers.IO) {
            // Split into chunks to avoid ML Kit limits
            val chunks = pageText.chunked(500)
            val translated = chunks.map { chunk ->
                runCatching { translateWith(deToAr, chunk) }.getOrDefault(chunk)
            }
            translated.joinToString(" ")
        }

    private suspend fun translateWith(
        translator: com.google.mlkit.nl.translate.Translator,
        text: String
    ): String = suspendCancellableCoroutine { cont ->
        translator.translate(text)
            .addOnSuccessListener { cont.resume(it) }
            .addOnFailureListener { cont.resumeWithException(it) }
    }

    // Basic part-of-speech heuristics for German
    private fun guessPartOfSpeech(word: String): String {
        val lower = word.lowercase()
        return when {
            word.first().isUpperCase() && word.length > 1 -> "اسم"
            lower.endsWith("en") || lower.endsWith("ern") || lower.endsWith("eln") -> "فعل"
            lower.endsWith("lich") || lower.endsWith("ig") || lower.endsWith("isch") -> "صفة"
            lower.endsWith("ung") || lower.endsWith("heit") || lower.endsWith("keit") -> "اسم"
            lower.endsWith("er") || lower.endsWith("ste") -> "صفة تفضيلية"
            else -> ""
        }
    }

    // Basic IPA hints for common German patterns
    private fun getGermanPhonetics(word: String): String {
        // Very simplified — in production use a proper TTS phoneme API
        return word
            .replace("sch", "ʃ")
            .replace("ch", "x")
            .replace("ei", "aɪ")
            .replace("ie", "iː")
            .replace("ö", "øː")
            .replace("ü", "yː")
            .replace("ä", "ɛː")
            .replace("ss", "s")
            .replace("ß", "s")
            .let { if (it != word) "[$it]" else "" }
    }

    fun close() {
        deToAr.close()
        arToDe.close()
        deToEn.close()
    }
}
