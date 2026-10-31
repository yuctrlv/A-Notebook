package com.demo.smartnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.smartnote.data.Category
import com.demo.smartnote.data.Note

/**
 * 新建 / 编辑页。noteId 为 null 表示新建。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditScreen(
    noteId: Long?,
    viewModel: NotesViewModel,
    onDone: () -> Unit
) {
    val existing by viewModel
        .observeNote(noteId ?: -1L)
        .collectAsStateWithLifecycle(initialValue = null)
    val categories by viewModel.categories.collectAsStateWithLifecycle()

    var title by remember(noteId) { mutableStateOf("") }
    var content by remember(noteId) { mutableStateOf("") }
    var categoryId by remember(noteId) { mutableStateOf(0L) }
    var loaded by remember(noteId) { mutableStateOf(noteId == null) }

    // 新建笔记：默认落在兜底分类（「其他」），拿不到就取第一个
    LaunchedEffect(categories, noteId) {
        if (noteId == null && categoryId == 0L && categories.isNotEmpty()) {
            categoryId = (categories.firstOrNull { it.isDefault } ?: categories.first()).id
        }
    }

    // 编辑已有笔记：把数据库里的值灌进输入框（只灌一次）
    LaunchedEffect(existing) {
        val value = existing
        if (!loaded && value != null) {
            title = value.title
            content = value.content
            categoryId = value.categoryId
            loaded = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (noteId == null) "新建笔记" else "编辑笔记") },
                navigationIcon = {
                    IconButton(onClick = onDone) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            val base = existing
                            val note = if (base != null && noteId != null) {
                                base.copy(
                                    title = title.trim(),
                                    content = content.trim(),
                                    categoryId = categoryId
                                )
                            } else {
                                Note(
                                    title = title.trim(),
                                    content = content.trim(),
                                    categoryId = categoryId
                                )
                            }
                            viewModel.save(note)
                            onDone()
                        },
                        enabled = content.isNotBlank() && categoryId != 0L
                    ) { Text("保存") }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = "分类",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(categories, key = { it.id }) { category ->
                    FilterChip(
                        selected = categoryId == category.id,
                        onClick = { categoryId = category.id },
                        label = { Text(category.name) },
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = categoryTone(categories.indexOf(category)).first,
                            selectedLabelColor = categoryTone(categories.indexOf(category)).second
                        )
                    )
                }
            }
            if (categories.isEmpty()) {
                Text(
                    text = "还没有分类，请先到首页底部新建一个",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(Modifier.height(16.dp))
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                label = { Text("标题（可选）") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Next
                )
            )

            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = content,
                onValueChange = { content = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(horizontal = 16.dp),
                label = { Text("正文") },
                placeholder = { Text("写点什么，比如一个单词、一句名言、一条成语…") }
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = "${content.length} 字" +
                    if (content.isBlank()) "" else " · 保存后可在详情页朗读",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

/** 供其他页面复用：按 id 找分类名 */
internal fun List<Category>.nameOf(id: Long): String =
    firstOrNull { it.id == id }?.name ?: "未分类"
