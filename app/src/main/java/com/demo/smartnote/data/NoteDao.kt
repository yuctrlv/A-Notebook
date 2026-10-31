package com.demo.smartnote.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** 分类计数（分组查询的返回类型） */
data class CategoryCount(
    val categoryId: Long,
    val count: Int
)

@Dao
interface NoteDao {

    /** 全部笔记，按最近修改时间倒序 */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    /** 按分类 id 查询 */
    @Query("SELECT * FROM notes WHERE categoryId = :categoryId ORDER BY updatedAt DESC")
    fun observeByCategory(categoryId: Long): Flow<List<Note>>

    /** 详情页用：观察单条笔记，被删除时自动发出 null */
    @Query("SELECT * FROM notes WHERE id = :id")
    fun observeById(id: Long): Flow<Note?>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)

    /** 删除分类时：把该分类下的笔记整体搬到兜底分类 */
    @Query("UPDATE notes SET categoryId = :toCategoryId, updatedAt = :now WHERE categoryId = :fromCategoryId")
    suspend fun moveAllTo(fromCategoryId: Long, toCategoryId: Long, now: Long)

    @Query("SELECT COUNT(*) FROM notes")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM notes WHERE categoryId = :categoryId")
    suspend fun countIn(categoryId: Long): Int

    /** 设置 / 清除某条笔记的定时提醒 */
    @Query("UPDATE notes SET reminderAt = :at, updatedAt = :now WHERE id = :id")
    suspend fun setReminder(id: Long, at: Long?, now: Long = System.currentTimeMillis())

    /** 每个分类有多少条笔记 */
    @Query("SELECT categoryId AS categoryId, COUNT(*) AS count FROM notes GROUP BY categoryId")
    suspend fun countsByCategory(): List<CategoryCount>
}
