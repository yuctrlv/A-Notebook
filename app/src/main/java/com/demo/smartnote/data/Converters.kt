package com.demo.smartnote.data

import androidx.room.TypeConverter

/**
 * 把枚举和字符串互转，Room 只认基本类型，所以需要转换器。
 */
class Converters {

    @TypeConverter
    fun fromCategory(category: NoteCategory): String = category.name

    @TypeConverter
    fun toCategory(value: String): NoteCategory = NoteCategory.fromName(value)
}
