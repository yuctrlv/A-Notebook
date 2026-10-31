package com.demo.smartnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.demo.smartnote.data.Category
import com.demo.smartnote.data.Note
import com.demo.smartnote.data.NoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 整个 App 共用一个 ViewModel（Activity 作用域），
 * 列表、编辑、详情都从它这里拿数据。
 */
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

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
        viewModelScope.launch { repository.deleteNote(id) }
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
        fun factory(repository: NoteRepository) = viewModelFactory {
            initializer { NotesViewModel(repository) }
        }
    }
}
