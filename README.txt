# PDF Reader — ملفات مكملة

## الملفات في هذا الـ ZIP

هذه الملفات **تكملة** للمشروع الموجود في مجلدي:
- `pdfreader_txt`  
- `pdfreader_txt2`

### الملفات الجديدة المضافة:

| الملف | الوصف |
|-------|-------|
| `FlashcardsScreen.txt` | شاشة بطاقات التعلم مع تأثير الـ Flip |
| `FlashcardsViewModel.txt` | ViewModel لبطاقات التعلم + Spaced Repetition |
| `BookmarksViewModel.txt` | ViewModel للإشارات المرجعية مع فلترة وتصدير |
| `LibraryViewModel.txt` | ViewModel للمكتبة مع تصنيف وفرز متقدم |
| `SettingsViewModel.txt` | ViewModel الإعدادات مع DataStore |
| `VocabularyViewModel.txt` | ViewModel المفردات مع وضع اختبار Quiz |
| `SearchViewModel.txt` | ViewModel للبحث مع Debounce وتاريخ البحث |
| `QAViewModel.txt` | ViewModel الأسئلة والأجوبة على محتوى PDF |
| `StatsScreen.txt` | شاشة الإحصائيات مع رسم بياني |
| `StatsViewModel.txt` | ViewModel الإحصائيات مع Streak |
| `AIManager.txt` | مدير الذكاء الاصطناعي (تلخيص، Q&A، استخراج مفردات) |
| `PdfOperationsViewModel.txt` | ViewModel عمليات PDF (ضغط، تشفير، دمج...) |
| `ExtraModels.txt` | نماذج البيانات الإضافية (SearchResult, VocabularyWord...) |
| `WorkerModule.txt` | Hilt Module للـ Workers والـ Utils |

## كيفية الاستخدام

1. في Android Studio: اذهب لـ File > New > Import Project
2. كل ملف `.txt` يمثل ملف Kotlin — احذف `.txt` وضعه في المسار الصحيح
   - المسار يتبين من اسم الملف: `app_src_main_java_com_mohammed_pdfreader_...`
   - مثال: `app_src_main_java_com_mohammed_pdfreader_ui_flashcards_FlashcardsScreen.txt`
   - = `app/src/main/java/com/mohammed/pdfreader/ui/flashcards/FlashcardsScreen.kt`

3. **تأكد من إضافة الـ dependencies في build.gradle:**
```kotlin
// Room
implementation "androidx.room:room-runtime:2.6.1"
implementation "androidx.room:room-ktx:2.6.1"
kapt "androidx.room:room-compiler:2.6.1"

// Hilt
implementation "com.google.dagger:hilt-android:2.50"
kapt "com.google.dagger:hilt-android-compiler:2.50"

// ML Kit
implementation "com.google.mlkit:translate:17.0.2"
implementation "com.google.mlkit:text-recognition-arabic:16.0.0"

// Compose
implementation "androidx.compose.material3:material3:1.2.0"
implementation "androidx.hilt:hilt-navigation-compose:1.1.0"
