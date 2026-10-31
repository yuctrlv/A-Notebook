package com.demo.smartnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.demo.smartnote.data.Category
import com.demo.smartnote.data.DailyTask
import com.demo.smartnote.data.Note
import com.demo.smartnote.data.NoteRepository
import com.demo.smartnote.data.TaskRepository
import com.demo.smartnote.reminder.ReminderSettings
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 整个 App 共用一个 ViewModel（Activity 作用域），
 * 列表、编辑、详情、任务都从它这里拿数据。
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NotesViewModel(
    private val repository: NoteRepository,
    private val taskRepository: TaskRepository
) : ViewModel() {

    /** null 表示「全部」 */
    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    /** 分类列表（用户可增删改） */
    val categories: StateFlow<List<Category>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val allNotes: StateFlow<List<Note>> = repository.observeNotes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 当前分类下的笔记；分类被删除或不存在时自动回落到全部 */
    val notes: StateFlow<List<Note>> =
        combine(allNotes, _selectedCategoryId, categories) { list, categoryId, cats ->
            val valid = categoryId?.takeIf { id -> cats.any { it.id == id } }
            if (valid == null) list else list.filter { it.categoryId == valid }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 每个分类的笔记数量 */
    val counts: StateFlow<Map<Long, Int>> = repository.observeCounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val totalCount: StateFlow<Int> = allNotes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** 一次性的提示消息（比如「已有同名分类」），界面消费后清空 */
    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    // ---------- 每日任务 ----------

    /** 当前日期（yyyy-MM-dd）。跨天后调用 refreshToday() 重新拉一次 */
    private val _today = MutableStateFlow(taskRepository.today())

    val tasks: StateFlow<List<DailyTask>> = taskRepository.tasks
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** 今天已完成的任务 id */
    val doneIds: StateFlow<Set<Long>> = _today
        .flatMapLatest { date -> taskRepository.doneIds(date) }
        .map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** 今日进度：已完成数 to 总数 */
    val progress: StateFlow<Pair<Int, Int>> =
        combine(tasks, doneIds) { list, done -> list.count { it.id in done } to list.size }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0 to 0)

    /** 提醒设置 */
    val reminder: StateFlow<ReminderSettings> = taskRepository.reminder
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReminderSettings())

    fun refreshToday() {
        _today.value = taskRepository.today()
    }

    fun addTask(title: String) {
        viewModelScope.launch { taskRepository.addTask(title) }
    }

    fun deleteTask(task: DailyTask) {
        viewModelScope.launch { taskRepository.deleteTask(task) }
    }

    fun toggleTask(taskId: Long) {
        viewModelScope.launch { taskRepository.toggleDone(taskId, _today.value) }
    }

    fun setReminderEnabled(enabled: Boolean) {
        viewModelScope.launch { taskRepository.setReminderEnabled(enabled) }
    }

    fun setReminderTime(hour: Int, minute: Int) {
        viewModelScope.launch { taskRepository.setReminderTime(hour, minute) }
    }

    init {
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun selectCategory(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun observeNote(id: Long): Flow<Note?> = repository.observeNoteById(id)

    fun save(note: Note) {
        viewModelScope.launch { repository.save(note) }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            taskRepository.cancelNoteReminder(id)
            repository.deleteNote(id)
        }
    }

    // ---------- 笔记的更多操作 ----------

    /** 只改标题（长按菜单里的「重命名」） */
    fun renameNote(note: Note, title: String) {
        viewModelScope.launch { repository.save(note.copy(title = title.trim())) }
    }

    /** 给笔记设一个定时提醒 */
    fun setNoteReminder(note: Note, atMillis: Long) {
        viewModelScope.launch {
            repository.setReminder(note.id, atMillis)
            taskRepository.scheduleNoteReminder(note.id, atMillis)
        }
    }

    fun clearNoteReminder(note: Note) {
        viewModelScope.launch {
            repository.setReminder(note.id, null)
            taskRepository.cancelNoteReminder(note.id)
        }
    }

    // ---------- 分类操作 ----------

    fun addCategory(name: String) {
        if (name.isBlank()) {
            _message.value = "分类名不能为空"
            return
        }
        viewModelScope.launch {
            val existing = categories.value.firstOrNull { it.name == name.trim() }
            if (existing != null) {
                _message.value = "已经有「${existing.name}」了"
                return@launch
            }
            repository.addCategory(name)
        }
    }

    fun renameCategory(id: Long, name: String) {
        if (name.isBlank()) {
            _message.value = "分类名不能为空"
            return
        }
        viewModelScope.launch { repository.renameCategory(id, name) }
    }

    fun deleteCategory(id: Long) {
        viewModelScope.launch {
            val ok = repository.deleteCategory(id)
            if (!ok) {
                _message.value = "「其他」是兜底分类，不能删除"
                return@launch
            }
            if (_selectedCategoryId.value == id) _selectedCategoryId.value = null
        }
    }

    /** 某个分类下有多少条笔记（删除前弹窗里用） */
    suspend fun countIn(categoryId: Long): Int = repository.noteCountIn(categoryId)

    fun consumeMessage() {
        _message.value = null
    }

    companion object {
        fun factory(repository: NoteRepository, taskRepository: TaskRepository) = viewModelFactory {
            initializer { NotesViewModel(repository, taskRepository) }
        }
    }
}
