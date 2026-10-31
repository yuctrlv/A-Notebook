package com.demo.smartnote.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * 仓库层：把 DAO 的细节挡在 ViewModel 之外。
 * 演示工程里没有网络，所以这里只做本地读写 + 首次启动的示例数据。
 */
class NoteRepository(private val dao: NoteDao) {

    /** 防止 ViewModel 被创建两次时并发写入两份示例数据 */
    private val seedMutex = Mutex()

    fun observeAll(): Flow<List<Note>> = dao.observeAll()

    fun observeById(id: Long): Flow<Note?> = dao.observeById(id)

    fun observeByCategory(category: NoteCategory): Flow<List<Note>> =
        dao.observeByCategory(category)

    suspend fun save(note: Note) {
        val now = System.currentTimeMillis()
        if (note.id == 0L) {
            dao.insert(note.copy(createdAt = now, updatedAt = now))
        } else {
            dao.update(note.copy(updatedAt = now))
        }
    }

    suspend fun delete(id: Long) = dao.deleteById(id)

    /** 首次启动塞几条示例数据，方便直接看效果 */
    suspend fun seedIfEmpty() = seedMutex.withLock {
        if (dao.count() > 0) return@withLock
        val now = System.currentTimeMillis()
        val samples = listOf(
            Note(
                title = "serendipity",
                content = "serendipity /ˌserənˈdɪpəti/ n. 意外发现美好事物的运气。\n" +
                    "例句：Finding this café was pure serendipity.",
                category = NoteCategory.ENGLISH,
                createdAt = now - 6 * 60_000,
                updatedAt = now - 6 * 60_000
            ),
            Note(
                title = "Actions speak louder than words",
                content = "行动胜于言辞。\n" +
                    "与其反复表态，不如先做出一件小事来证明自己。",
                category = NoteCategory.QUOTE,
                createdAt = now - 5 * 60_000,
                updatedAt = now - 5 * 60_000
            ),
            Note(
                title = "守株待兔",
                content = "出自《韩非子·五蠹》。比喻死守狭隘经验，不知变通，妄想不劳而获。\n" +
                    "例句：机会要靠主动争取，守株待兔只会错过时机。",
                category = NoteCategory.IDIOM,
                createdAt = now - 4 * 60_000,
                updatedAt = now - 4 * 60_000
            ),
            Note(
                title = "本周待办",
                content = "1. 整理英语单词本第 3-5 页\n2. 每天读一条名言并复述\n3. 周末写一篇小结",
                category = NoteCategory.OTHER,
                createdAt = now - 3 * 60_000,
                updatedAt = now - 3 * 60_000
            )
        )
        samples.forEach { dao.insert(it) }
    }

    /** 分类计数，用于列表页顶部筛选条上的数字 */
    fun observeCounts(): Flow<Map<NoteCategory, Int>> = dao.observeAll().map { list ->
        list.groupingBy { it.category }.eachCount()
    }
}
