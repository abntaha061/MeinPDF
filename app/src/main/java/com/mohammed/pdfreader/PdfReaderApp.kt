package com.mohammed.pdfreader

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class PdfReaderApp : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
