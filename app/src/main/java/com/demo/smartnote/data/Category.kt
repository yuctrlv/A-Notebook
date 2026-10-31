package com.demo.smartnote.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 分类实体。用户可以自己新建、重命名、删除。
 * 对应 Room 表 categories。
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    /** 分类名，用户可改 */
    val name: String = "",
    /** 排序，数字小的排前面 */
    val sortOrder: Int = 0,
    /**
     * 兜底分类标记。
     * 只有「其他」是兜底：它不能删除，其他分类被删掉时，里面的笔记会移动到这里。
     */
    val isDefault: Boolean = false
) {
    companion object {
        /** 首次启动创建的默认分类（顺序即展示顺序） */
        val DEFAULTS = listOf("英语", "名言名句", "成语", "其他")
        const val FALLBACK_NAME = "其他"
    }
}
