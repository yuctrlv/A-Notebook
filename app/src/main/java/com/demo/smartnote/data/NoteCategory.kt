package com.demo.smartnote.data

/**
 * 笔记分类。
 * 第一版写死为 4 个分类，后续要扩展成自定义分类时，只需要把这里换成数据表即可。
 */
enum class NoteCategory(val label: String) {
    ENGLISH("英语"),
    QUOTE("名言名句"),
    IDIOM("成语"),
    OTHER("其他");

    companion object {
        /** 从数据库里的字符串还原枚举，遇到脏数据时兜底为「其他」 */
        fun fromName(name: String): NoteCategory =
            entries.firstOrNull { it.name == name } ?: OTHER
    }
}
