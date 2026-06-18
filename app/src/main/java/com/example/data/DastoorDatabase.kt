package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Category::class,
        Tag::class,
        Document::class,
        Bookmark::class,
        Note::class,
        Highlight::class,
        ReadingHistory::class,
        UserSettings::class
    ],
    version = 1,
    exportSchema = false
)
abstract class DastoorDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun noteDao(): NoteDao
    abstract fun highlightDao(): HighlightDao
    abstract fun readingHistoryDao(): ReadingHistoryDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: DastoorDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): DastoorDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DastoorDatabase::class.java,
                    "dastoor_database"
                )
                .addCallback(DastoorDatabaseCallback(scope))
                .build()
                INSTANCE = instance
                instance
            }
        }
    }

    private class DastoorDatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {
        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populateDatabase(database)
                }
            }
        }

        suspend fun populateDatabase(db: DastoorDatabase) {
            // Prepopulate Categories
            val categories = listOf(
                Category(name = "دستورالعمل‌ها", isSystem = true),
                Category(name = "استانداردها", isSystem = true),
                Category(name = "آیین‌نامه‌ها", isSystem = true),
                Category(name = "نقشه‌ها", isSystem = true),
                Category(name = "مشخصات فنی", isSystem = true),
                Category(name = "گزارش‌ها", isSystem = true),
                Category(name = "آموزشی", isSystem = true)
            )
            for (category in categories) {
                db.categoryDao().insertCategory(category)
            }

            // Prepopulate Tags
            val tags = listOf(
                Tag(name = "برق"),
                Tag(name = "عمران"),
                Tag(name = "مکانیک"),
                Tag(name = "استاندارد"),
                Tag(name = "توانیر"),
                Tag(name = "IEC"),
                Tag(name = "IEEE")
            )
            for (tag in tags) {
                db.tagDao().insertTag(tag)
            }

            // Populate some mock Technical Documents in Iranian Standards
            val docs = listOf(
                Document(
                    title = "نشریه ۵۵ - مشخصات فنی عمومی کارهای ساختمانی",
                    description = "مرجع اصلی کارهای ساختمانی و مهندسی عمران در پروژه‌های اجرایی عمرانی ایران.",
                    categoryId = 5, // مشخصات فنی
                    filePath = "system_preloaded_doc_55.pdf",
                    fileSize = "۱۲.۴ مگابایت",
                    totalPages = 480,
                    tags = "عمران,استاندارد",
                    contentText = """
                        فصل اول: مصالح ساختمانی
                        آجر، سیمان، گچ و ملات‌ها باید دارای استانداردهای ملی ایران به شماره‌های مصوب باشند.
                        برای ساخت بتن باربر باید حتماً سیمان پرتلند تیپ ۲ یا ۵ استفاده شود. 
                        کیفیت مصالح سنگی باید توسط آزمایشگاه مورد تایید کارفرما صحه‌گذاری و تایید گردد.
                        
                        فصل دوم: عملیات خاکی و پی‌سازی
                        عمق گودبرداری کارهای پی‌ریزی باید بر اساس نقشه‌های مصوب به تایید مهندس ناظر برسد.
                        تراکم خاک بستر شالوده‌ها نباید از ۹۵ درصد پروکتور استاندارد کمتر باشد.
                        در زمین‌های با مقاومت ضعیف استفاده از شمع یا میکروپایل الزامی است و محاسبات آن در پیوست آیین‌نامه ذکر شده است.
                    """.trimIndent()
                ),
                Document(
                    title = "مبحث ۹ مقررات ملی ساختمان - طرح و اجرای ساختمان‌های بتن آرمه",
                    description = "آیین‌نامه بتن ایران (آبا) و آخرین ویراست‌های الزامات اجرایی بتن آرمه.",
                    categoryId = 3, // آیین‌نامه‌ها
                    filePath = "system_preloaded_doc_9.pdf",
                    fileSize = "۸.۶ مگابایت",
                    totalPages = 320,
                    tags = "عمران,استاندارد",
                    contentText = """
                        بخش اول: کلیات و مبانی طراحی بتن‌آرمه
                        مقاومت مشخصه بتن بر اساس نمونه‌های استوانه‌ای ۲۸ روزه تعریف می‌شود.
                        حداقل پوشش بتن روی میلگردها (کاور بتن) برای شرایط محیطی شدید ۵۰ میلی‌متر است.
                        ضریب ایمنی جزئی برای بتن ۱.۵ و برای ساخت آرماتور میلگرد ۱.۱۵ در نظر گرفته می‌شود.
                        
                        بخش دوم: کنترل ترک‌خوردگی و خیز تیرها
                        عرض ترک‌های مجاز در محیط‌های تهاجمی طبق جداول پیوست حداکثر ۰.۲ میلی‌متر است.
                        تیرهای فرعی باید بر اساس الزامات شکل‌پذیری متوسط طراحی شوند. وصله میلگردهای کششی ترجیحاً باید مکانیکی (کوپلر) باشد.
                    """.trimIndent()
                ),
                Document(
                    title = "استاندارد توانیر - دستورالعمل طراحی شبکه‌های توزیع برق زمینی",
                    description = "الزامات فنی طراحی حریم کابل‌ها، جعبه‌های انشعاب و پست‌های برق توزیع همکف.",
                    categoryId = 1, // دستورالعمل‌ها
                    filePath = "system_preloaded_doc_tavanir.pdf",
                    fileSize = "۴.۲ مگابایت",
                    totalPages = 150,
                    tags = "برق,توانیر,IEC",
                    contentText = """
                        دستورالعمل اجرایی شبکه‌های توزیع نیروی برق زمینی:
                        عمق دفن کابل‌های ۲۰ کیلوولت در زیر پیاده‌رو باید حداقل ۸۰ سانتی‌متر و در زیر سواره‌رو ۱۲۰ سانتی‌متر باشد.
                        استفاده از نوار هشدار زرد رنگ با عنوان «توجه کابل برق» در عمق ۳۰ سانتی‌متری بالای کابل الزامی است.
                        جعبه‌های انشعاب (شالتر) باید دارای استاندارد حفاظتی IP54 بوده و بر پایه‌های بتنی خود استوار باشند.
                        مشخصات فنی خازن‌ها و رله‌های حفاظتی باید کاملاً طبق استانداردهای پیشرفته IEC 60871 طبقه‌بندی شود.
                    """.trimIndent()
                )
            )

            for (doc in docs) {
                db.documentDao().insertDocument(doc)
            }

            // Populate default setting
            db.userSettingsDao().saveUserSettings(UserSettings())
        }
    }
}
