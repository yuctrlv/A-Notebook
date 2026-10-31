package com.demo.smartnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体。
 * 对应 Room 表 notes。
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 标题可选，空字符串表示没有标题 */
    val title: String = "",
    /** 正文，不能为空 */
    val content: String = "",
    /** 所属分类 id，指向 categories 表 */
    val categoryId: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    /** 定时提醒的时间戳；null 表示没设提醒 */
    val reminderAt: Long? = null
) {
    /** 列表页展示用的标题：没有标题就取正文前 30 个字 */
    val displayTitle: String
        get() = title.ifBlank { content.take(30).replace("\n", " ") }
}
