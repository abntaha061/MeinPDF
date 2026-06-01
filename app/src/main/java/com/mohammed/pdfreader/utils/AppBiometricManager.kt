package com.mohammed.pdfreader.utils

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppBiometricManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    // ===== Check availability =====
    fun isBiometricAvailable(): Boolean {
        val manager = BiometricManager.from(context)
        return manager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }

    fun getBiometricStatus(): String {
        val manager = BiometricManager.from(context)
        return when (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> "متاح"
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "لا يوجد جهاز بصمة"
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "الجهاز غير متاح"
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "لم تُسجَّل بصمة"
            else -> "غير متاح"
        }
    }

    // ===== Show biometric prompt =====
    fun showBiometricPrompt(
        activity: FragmentActivity,
        title: String = "قفل التطبيق",
        subtitle: String = "أثبت هويتك للمتابعة",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(context)

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString.toString())
            }
            override fun onAuthenticationFailed() {
                onFailure("فشل التحقق من الهوية")
            }
        }

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
            .build()

        BiometricPrompt(activity, executor, callback).authenticate(promptInfo)
    }
}
