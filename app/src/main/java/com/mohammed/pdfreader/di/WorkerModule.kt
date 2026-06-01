package com.mohammed.pdfreader.di

import android.content.Context
import com.mohammed.pdfreader.utils.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    @Singleton
    fun provideAIManager(
        @ApplicationContext context: Context
    ): AIManager = AIManager(context)

    @Provides
    @Singleton
    fun providePdfUtils(
        @ApplicationContext context: Context
    ): PdfUtils = PdfUtils(context)

    @Provides
    @Singleton
    fun provideTranslationManager(
        @ApplicationContext context: Context
    ): TranslationManager = TranslationManager(context)

    @Provides
    @Singleton
    fun provideOcrManager(
        @ApplicationContext context: Context
    ): OcrManager = OcrManager(context)

    @Provides
    @Singleton
    fun provideFileManager(
        @ApplicationContext context: Context
    ): FileManager = FileManager(context)

    @Provides
    @Singleton
    fun providePermissionManager(
        @ApplicationContext context: Context
    ): PermissionManager = PermissionManager(context)

    @Provides
    @Singleton
    fun provideBiometricManager(
        @ApplicationContext context: Context
    ): BiometricManager = BiometricManager(context)

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStore(context)
}
