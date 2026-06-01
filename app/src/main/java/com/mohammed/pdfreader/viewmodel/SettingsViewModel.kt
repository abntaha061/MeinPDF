package com.mohammed.pdfreader.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohammed.pdfreader.utils.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val theme: String = "dark",          // dark | light | auto | sepia
    val fontSize: Float = 16f,
    val lineSpacing: String = "normal",  // tight | normal | wide
    val scrollDirection: String = "vertical",
    val pageMode: String = "continuous",
    val fitMode: String = "width",
    val autoScrollSpeed: Float = 1f,
    val defaultSourceLang: String = "auto",
    val defaultTargetLang: String = "ar",
    val ttsSpeed: Float = 1.0f,
    val ttsVoice: String = "female",
    val highlightColor: String = "#FFFF00",
    val penThickness: Float = 3f,
    val annotationOpacity: Float = 0.8f,
    val highContrast: Boolean = false,
    val reduceMotion: Boolean = false,
    val screenSecurity: Boolean = false,
    val appLockEnabled: Boolean = false,
    val appLockType: String = "biometric", // biometric | pin
    val brightness: Float = -1f,          // -1 = system
    val fontFamily: String = "Noto Sans Arabic",
    val isLoading: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            settingsDataStore.settingsFlow.collect { prefs ->
                _uiState.update {
                    it.copy(
                        theme = prefs.theme,
                        fontSize = prefs.fontSize,
                        lineSpacing = prefs.lineSpacing,
                        scrollDirection = prefs.scrollDirection,
                        pageMode = prefs.pageMode,
                        fitMode = prefs.fitMode,
                        defaultSourceLang = prefs.defaultSourceLang,
                        defaultTargetLang = prefs.defaultTargetLang,
                        ttsSpeed = prefs.ttsSpeed,
                        ttsVoice = prefs.ttsVoice,
                        highlightColor = prefs.highlightColor,
                        penThickness = prefs.penThickness,
                        highContrast = prefs.highContrast,
                        reduceMotion = prefs.reduceMotion,
                        screenSecurity = prefs.screenSecurity,
                        appLockEnabled = prefs.appLockEnabled,
                        appLockType = prefs.appLockType,
                        brightness = prefs.brightness,
                        fontFamily = prefs.fontFamily
                    )
                }
            }
        }
    }

    fun setTheme(theme: String) {
        viewModelScope.launch {
            settingsDataStore.setTheme(theme)
        }
    }

    fun setFontSize(size: Float) {
        viewModelScope.launch {
            settingsDataStore.setFontSize(size)
        }
    }

    fun setLineSpacing(spacing: String) {
        viewModelScope.launch {
            settingsDataStore.setLineSpacing(spacing)
        }
    }

    fun setScrollDirection(direction: String) {
        viewModelScope.launch {
            settingsDataStore.setScrollDirection(direction)
        }
    }

    fun setPageMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setPageMode(mode)
        }
    }

    fun setFitMode(mode: String) {
        viewModelScope.launch {
            settingsDataStore.setFitMode(mode)
        }
    }

    fun setDefaultSourceLang(lang: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultSourceLang(lang)
        }
    }

    fun setDefaultTargetLang(lang: String) {
        viewModelScope.launch {
            settingsDataStore.setDefaultTargetLang(lang)
        }
    }

    fun setTtsSpeed(speed: Float) {
        viewModelScope.launch {
            settingsDataStore.setTtsSpeed(speed)
        }
    }

    fun setTtsVoice(voice: String) {
        viewModelScope.launch {
            settingsDataStore.setTtsVoice(voice)
        }
    }

    fun setHighlightColor(color: String) {
        viewModelScope.launch {
            settingsDataStore.setHighlightColor(color)
        }
    }

    fun setPenThickness(thickness: Float) {
        viewModelScope.launch {
            settingsDataStore.setPenThickness(thickness)
        }
    }

    fun setHighContrast(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setHighContrast(enabled)
        }
    }

    fun setReduceMotion(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setReduceMotion(enabled)
        }
    }

    fun setScreenSecurity(enabled: Boolean) {
        viewModelScope.launch {
            settingsDataStore.setScreenSecurity(enabled)
        }
    }

    fun setAppLock(enabled: Boolean, type: String) {
        viewModelScope.launch {
            settingsDataStore.setAppLock(enabled, type)
        }
    }

    fun setBrightness(brightness: Float) {
        viewModelScope.launch {
            settingsDataStore.setBrightness(brightness)
        }
    }

    fun setFontFamily(font: String) {
        viewModelScope.launch {
            settingsDataStore.setFontFamily(font)
        }
    }

    fun resetAllSettings() {
        viewModelScope.launch {
            settingsDataStore.resetAll()
        }
    }
}
