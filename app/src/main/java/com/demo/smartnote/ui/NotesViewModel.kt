package com.demo.smartnote.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.demo.smartnote.data.Note
import com.demo.smartnote.data.NoteCategory
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
 * 列表、编辑、详情都从它这里拿数据，避免页面之间各自建连接。
 */
class NotesViewModel(private val repository: NoteRepository) : ViewModel() {

    /** null 表示「全部」 */
    private val _selectedCategory = MutableStateFlow<NoteCategory?>(null)
    val selectedCategory: StateFlow<NoteCategory?> = _selectedCategory.asStateFlow()

    private val allNotes: StateFlow<List<Note>> = repository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val notes: StateFlow<List<Note>> =
        combine(allNotes, _selectedCategory) { list, category ->
            if (category == null) list else list.filter { it.category == category }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val counts: StateFlow<Map<NoteCategory, Int>> = repository.observeCounts()
        .map { it.toMap() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val totalCount: StateFlow<Int> = allNotes
        .map { it.size }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        // 首次启动写示例数据
        viewModelScope.launch { repository.seedIfEmpty() }
    }

    fun selectCategory(category: NoteCategory?) {
        _selectedCategory.value = category
    }

    fun observeNote(id: Long): Flow<Note?> = repository.observeById(id)

    fun save(note: Note) {
        viewModelScope.launch { repository.save(note) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    companion object {
        fun factory(repository: NoteRepository) = viewModelFactory {
            initializer { NotesViewModel(repository) }
        }
    }
}
