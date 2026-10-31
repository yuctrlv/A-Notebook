package com.demo.smartnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Category::class],
    version = 2,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        @Volatile
        private var INSTANCE: NoteDatabase? = null

        /**
         * 1 → 2：分类从写死的枚举改成可编辑的表。
         * - 新建 categories 表并插入 4 个默认分类
         * - notes 表重建：category(TEXT) 换成 categoryId(INTEGER)
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `categories` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `isDefault` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO categories (id, name, sortOrder, isDefault) VALUES
                    (1, '英语', 0, 0),
                    (2, '名言名句', 1, 0),
                    (3, '成语', 2, 0),
                    (4, '其他', 3, 1)
                    """.trimIndent()
                )
                // 重建 notes 表，把旧的枚举名映射到新的分类 id
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `notes_new` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `categoryId` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO notes_new (id, title, content, categoryId, createdAt, updatedAt)
                    SELECT id, title, content,
                        CASE category
                            WHEN 'ENGLISH' THEN 1
                            WHEN 'QUOTE' THEN 2
                            WHEN 'IDIOM' THEN 3
                            ELSE 4
                        END,
                        createdAt, updatedAt
                    FROM notes
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE notes")
                db.execSQL("ALTER TABLE notes_new RENAME TO notes")
            }
        }

        /** 单例，整个 App 共用一个数据库连接 */
        fun get(context: Context): NoteDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NoteDatabase::class.java,
                    "smartnote.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
