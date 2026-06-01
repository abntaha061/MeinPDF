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
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): SettingsDataStore = SettingsDataStore(context)

    @Provides
    @Singleton
    fun providePdfUtils(
        @ApplicationContext context: Context
    ): PdfUtils = PdfUtils(context)

    @Provides
    @Singleton
    fun provideOcrManager(
        @ApplicationContext context: Context
    ): OcrManager = OcrManager(context)

    @Provides
    @Singleton
    fun provideTranslationManager(
        @ApplicationContext context: Context
    ): TranslationManager = TranslationManager(context)

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
}
