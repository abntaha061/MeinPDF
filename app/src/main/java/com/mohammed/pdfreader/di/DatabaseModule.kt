package com.mohammed.pdfreader.di

import android.content.Context
import androidx.room.Room
import com.mohammed.pdfreader.data.db.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pdf_reader_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    fun providePdfFileDao(db: AppDatabase): PdfFileDao = db.pdfFileDao()

    @Provides
    fun provideBookmarkDao(db: AppDatabase): BookmarkDao = db.bookmarkDao()

    @Provides
    fun provideAnnotationDao(db: AppDatabase): AnnotationDao = db.annotationDao()

    @Provides
    fun provideVocabularyDao(db: AppDatabase): VocabularyDao = db.vocabularyDao()

    @Provides
    fun provideReadHistoryDao(db: AppDatabase): ReadHistoryDao = db.readHistoryDao()
}
