package com.demo.smartnote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Note::class, Category::class, DailyTask::class, TaskRecord::class],
    version = 4,
    exportSchema = false
)
abstract class NoteDatabase : RoomDatabase() {

    abstract fun noteDao(): NoteDao
    abstract fun categoryDao(): CategoryDao
    abstract fun taskDao(): TaskDao

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

        /**
         * 2 → 3：新增每日任务的两张表。
         * - daily_tasks：任务模板（每天重复出现的那几条）
         * - task_records：某天某任务是否完成（主键 taskId + date）
         * 建表语句必须和 Room 生成的 schema 完全一致，否则运行时会报 "Migration didn't properly handle"。
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `daily_tasks` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `title` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `task_records` (
                        `taskId` INTEGER NOT NULL,
                        `date` TEXT NOT NULL,
                        `done` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        PRIMARY KEY(`taskId`, `date`)
                    )
                    """.trimIndent()
                )
            }
        }

        /** 3 → 4：笔记新增可空的 reminderAt（定时提醒时间戳） */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE notes ADD COLUMN reminderAt INTEGER")
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
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
