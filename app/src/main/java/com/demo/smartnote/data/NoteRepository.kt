package com.demo.smartnote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 仓库层：把 DAO 的细节挡在 ViewModel 之外。
 * 笔记和分类两块数据都在这里统一入口。
 */
class NoteRepository(
    private val noteDao: NoteDao,
    private val categoryDao: CategoryDao
) {

    /** 防止 ViewModel 被创建两次时并发写入两份示例数据 */
    private val seedMutex = Mutex()

    // ---------- 笔记 ----------

    fun observeNotes(categoryId: Long? = null): Flow<List<Note>> =
        if (categoryId == null) noteDao.observeAll() else noteDao.observeByCategory(categoryId)

    fun observeNoteById(id: Long): Flow<Note?> = noteDao.observeById(id)

    suspend fun save(note: Note) {
        val now = System.currentTimeMillis()
        if (note.id == 0L) {
            noteDao.insert(note.copy(createdAt = now, updatedAt = now))
        } else {
            noteDao.update(note.copy(updatedAt = now))
        }
    }

    suspend fun deleteNote(id: Long) = noteDao.deleteById(id)

    // ---------- 分类 ----------

    fun observeCategories(): Flow<List<Category>> = categoryDao.observeAll()

    /** 每个分类下的笔记数 */
    fun observeCounts(): Flow<Map<Long, Int>> = noteDao.observeAll().map { list ->
        list.groupingBy { it.categoryId }.eachCount()
    }

    suspend fun defaultCategoryId(): Long =
        categoryDao.getFallback()?.id ?: categoryDao.getAll().firstOrNull()?.id ?: 0L

    /** 新建分类：同名则直接返回已有 id，避免出现两个「英语」 */
    suspend fun addCategory(name: String): Long {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return 0L
        val all = categoryDao.getAll()
        all.firstOrNull { it.name == trimmed }?.let { return it.id }
        return categoryDao.insert(
            Category(name = trimmed, sortOrder = (all.maxOfOrNull { it.sortOrder } ?: -1) + 1)
        )
    }

    suspend fun renameCategory(id: Long, name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        val all = categoryDao.getAll()
        if (all.any { it.name == trimmed && it.id != id }) return
        val target = categoryDao.getById(id) ?: return
        categoryDao.update(target.copy(name = trimmed))
    }

    /**
     * 删除分类：先把里面的笔记搬到兜底分类，再删分类本身。
     * 兜底分类（「其他」）不允许删除，否则笔记会没有归属。
     */
    suspend fun deleteCategory(id: Long): Boolean {
        val target = categoryDao.getById(id) ?: return false
        if (target.isDefault) return false
        val fallback = categoryDao.getFallback() ?: return false
        noteDao.moveAllTo(fromCategoryId = id, toCategoryId = fallback.id, now = System.currentTimeMillis())
        categoryDao.deleteById(id)
        return true
    }

    // ---------- 首次启动的示例数据 ----------

    suspend fun seedIfEmpty() = seedMutex.withLock {
        // 1. 分类不能空，否则新建笔记没有归属
        if (categoryDao.count() == 0) {
            Category.DEFAULTS.forEachIndexed { index, name ->
                categoryDao.insert(
                    Category(
                        name = name,
                        sortOrder = index,
                        isDefault = name == Category.FALLBACK_NAME
                    )
                )
            }
        }
        // 2. 笔记只在完全空的时候塞一次
        if (noteDao.count() > 0) return@withLock

        val idByName = categoryDao.getAll().associate { it.name to it.id }
        val fallback = (categoryDao.getFallback() ?: categoryDao.getAll().first()).id
        fun idOf(name: String) = idByName[name] ?: fallback

        val now = System.currentTimeMillis()
        val samples = listOf(
            Note(
                title = "serendipity",
                content = "serendipity /ˌserənˈdɪpəti/ n. 意外发现美好事物的运气。\n" +
                    "例句：Finding this café was pure serendipity.",
                categoryId = idOf("英语"),
                createdAt = now - 6 * 60_000,
                updatedAt = now - 6 * 60_000
            ),
            Note(
                title = "Actions speak louder than words",
                content = "行动胜于言辞。\n" +
                    "与其反复表态，不如先做出一件小事来证明自己。",
                categoryId = idOf("名言名句"),
                createdAt = now - 5 * 60_000,
                updatedAt = now - 5 * 60_000
            ),
            Note(
                title = "守株待兔",
                content = "出自《韩非子·五蠹》。比喻死守狭隘经验，不知变通，妄想不劳而获。\n" +
                    "例句：机会要靠主动争取，守株待兔只会错过时机。",
                categoryId = idOf("成语"),
                createdAt = now - 4 * 60_000,
                updatedAt = now - 4 * 60_000
            ),
            Note(
                title = "本周待办",
                content = "1. 整理英语单词本第 3-5 页\n2. 每天读一条名言并复述\n3. 周末写一篇小结",
                categoryId = fallback,
                createdAt = now - 3 * 60_000,
                updatedAt = now - 3 * 60_000
            )
        )
        samples.forEach { noteDao.insert(it) }
    }

    /** 分类被删掉后，把它从选中状态清掉 */
    suspend fun categoryExists(id: Long): Boolean = categoryDao.getById(id) != null

    suspend fun noteCountIn(categoryId: Long): Int = noteDao.countIn(categoryId)
}
