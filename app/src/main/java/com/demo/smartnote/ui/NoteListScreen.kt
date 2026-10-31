package com.demo.smartnote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.smartnote.data.Category
import com.demo.smartnote.data.Note
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页：大标题 + 笔记卡片列表。
 * 分类切换条放在屏幕底部（FAB 上方），支持长按重命名 / 删除，末尾「＋」新建分类。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NoteListScreen(
    viewModel: NotesViewModel,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val selectedId by viewModel.selectedCategoryId.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // 分类操作弹窗状态
    var actionTarget by remember { mutableStateOf<Category?>(null) }
    var renameTarget by remember { mutableStateOf<Category?>(null) }
    var deletingTarget by remember { mutableStateOf<Category?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(message) {
        val text = message
        if (text != null) {
            snackbarHostState.showSnackbar(text)
            viewModel.consumeMessage()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            CategoryBar(
                categories = categories,
                counts = counts,
                totalCount = totalCount,
                selectedId = selectedId,
                onSelect = viewModel::selectCategory,
                onLongPress = { actionTarget = it },
                onAddCategory = { showAddDialog = true }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddNote,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = RoundedCornerShape(22.dp),
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("记一笔", fontWeight = FontWeight.Bold) }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(top = 8.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header(
                    totalCount = totalCount,
                    selectedName = categories.firstOrNull { it.id == selectedId }?.name,
                    selectedIndex = categories.indexOfFirst { it.id == selectedId }
                )
            }
            if (notes.isEmpty()) {
                item { EmptyState(hasFilter = selectedId != null, onAddNote = onAddNote) }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        category = categories.firstOrNull { it.id == note.categoryId },
                        toneIndex = categories.indexOfFirst { it.id == note.categoryId },
                        onClick = { onOpenNote(note.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }

    // ---------- 长按分类弹出的操作表 ----------
    val action = actionTarget
    if (action != null) {
        ModalBottomSheet(onDismissRequest = { actionTarget = null }) {
            Column(modifier = Modifier.navigationBarsPadding()) {
                Text(
                    text = "「${action.name}」",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                )
                ListItem(
                    headlineContent = { Text("重命名") },
                    leadingContent = { Icon(Icons.Default.Edit, contentDescription = null) },
                    modifier = Modifier.combinedClickable(onClick = {
                        actionTarget = null
                        renameTarget = action
                    })
                )
                ListItem(
                    headlineContent = {
                        Text(if (action.isDefault) "删除（兜底分类不可删）" else "删除分类")
                    },
                    supportingContent = {
                        if (!action.isDefault) Text("里面的笔记会移动到「其他」")
                    },
                    leadingContent = {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = null,
                            tint = if (action.isDefault) MaterialTheme.colorScheme.outline
                            else MaterialTheme.colorScheme.error
                        )
                    },
                    modifier = Modifier.combinedClickable(
                        onClick = {
                            if (!action.isDefault) {
                                actionTarget = null
                                deletingTarget = action
                            }
                        }
                    )
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }

    // ---------- 新建分类 ----------
    if (showAddDialog) {
        CategoryNameDialog(
            title = "新建分类",
            initial = "",
            confirmText = "创建",
            onDismiss = { showAddDialog = false },
            onConfirm = { name ->
                showAddDialog = false
                viewModel.addCategory(name)
            }
        )
    }

    // ---------- 重命名 ----------
    val renaming = renameTarget
    if (renaming != null) {
        CategoryNameDialog(
            title = "重命名分类",
            initial = renaming.name,
            confirmText = "保存",
            onDismiss = { renameTarget = null },
            onConfirm = { name ->
                renameTarget = null
                viewModel.renameCategory(renaming.id, name)
            }
        )
    }

    // ---------- 删除确认 ----------
    val deleting = deletingTarget
    if (deleting != null) {
        var noteCount by remember(deleting.id) { mutableStateOf<Int?>(null) }
        LaunchedEffect(deleting.id) { noteCount = viewModel.countIn(deleting.id) }
        AlertDialog(
            onDismissRequest = { deletingTarget = null },
            title = { Text("删除「${deleting.name}」？") },
            text = {
                Text(
                    if ((noteCount ?: 0) > 0) {
                        "这个分类下的 ${noteCount} 条笔记会移动到「其他」，不会丢。"
                    } else {
                        "分类里没有笔记，可以放心删除。"
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    deletingTarget = null
                    viewModel.deleteCategory(deleting.id)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deletingTarget = null }) { Text("取消") }
            }
        )
    }
}

/** 底部悬浮的分类切换条 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CategoryBar(
    categories: List<Category>,
    counts: Map<Long, Int>,
    totalCount: Int,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onLongPress: (Category) -> Unit,
    onAddCategory: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        tonalElevation = 2.dp,
        shadowElevation = 8.dp
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            item {
                SwitcherItem(
                    label = "全部",
                    count = totalCount,
                    selected = selectedId == null,
                    toneIndex = -1,
                    onClick = { onSelect(null) }
                )
            }
            items(categories, key = { it.id }) { category ->
                val index = categories.indexOf(category)
                SwitcherItem(
                    label = category.name,
                    count = counts[category.id] ?: 0,
                    selected = selectedId == category.id,
                    toneIndex = index,
                    onClick = { onSelect(category.id) },
                    onLongClick = { onLongPress(category) }
                )
            }
            item {
                Surface(
                    onClick = onAddCategory,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "新建分类", modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("分类", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SwitcherItem(
    label: String,
    count: Int,
    selected: Boolean,
    toneIndex: Int,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null
) {
    val tone = categoryTone(toneIndex)
    val container by animateColorAsState(
        targetValue = if (selected) tone.first else MaterialTheme.colorScheme.surfaceContainerHigh,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "switcher-container"
    )
    val content by animateColorAsState(
        targetValue = if (selected) tone.second else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "switcher-content"
    )

    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(horizontal = 18.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = label, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
            if (count > 0) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text = count.toString(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = content.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
private fun Header(totalCount: Int, selectedName: String?, selectedIndex: Int) {
    val tone = categoryTone(selectedIndex)
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp)) {
        Text(
            text = "一个笔记本",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${selectedName ?: "全部"} · $totalCount 条",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(tone.second)
            )
        }
    }
}

@Composable
private fun NoteCard(
    note: Note,
    category: Category?,
    toneIndex: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tone = categoryTone(toneIndex)
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = tone.first,
        contentColor = tone.second,
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = note.displayTitle.ifBlank { "无题" },
                    style = MaterialTheme.typography.titleLarge,
                    fontSize = 20.sp,
                    lineHeight = 27.sp,
                    color = tone.second,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                if (category != null) {
                    Surface(
                        color = tone.second.copy(alpha = 0.10f),
                        contentColor = tone.second,
                        shape = CircleShape
                    ) {
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note.content.replace("\n", " ").trim(),
                    style = MaterialTheme.typography.bodyMedium,
                    lineHeight = 22.sp,
                    color = tone.second.copy(alpha = 0.78f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = tone.second.copy(alpha = 0.6f)
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${note.content.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = tone.second.copy(alpha = 0.6f)
                )
            }
        }
    }
}

/**
 * 分类配色：一个分类对应一组「容器色 + 前景色」。
 * index < 0 表示「全部」。
 * 色板循环使用，所以新增分类也能自动拿到颜色。
 */
@Composable
fun categoryTone(index: Int): Pair<Color, Color> {
    val dark = isSystemInDarkTheme()
    val light = listOf(
        Color(0xFFE3E7FF) to Color(0xFF1B2A6B), // 靛蓝
        Color(0xFFF2E4FF) to Color(0xFF431C66), // 紫
        Color(0xFFDDF3E9) to Color(0xFF0C4232), // 薄荷绿
        Color(0xFFEDECF3) to Color(0xFF3A3946), // 灰
        Color(0xFFFFE6D5) to Color(0xFF5C2B0F), // 橙
        Color(0xFFDFF1F8) to Color(0xFF0D3B4C)  // 蓝
    )
    val darkColors = listOf(
        Color(0xFF28306B) to Color(0xFFD9DDFF),
        Color(0xFF45255E) to Color(0xFFF0DDFF),
        Color(0xFF17453A) to Color(0xFFC9F3E2),
        Color(0xFF2E2D38) to Color(0xFFE4E2EC),
        Color(0xFF4A2A12) to Color(0xFFFFD8BC),
        Color(0xFF10384A) to Color(0xFFCBE9F5)
    )
    val palette = if (dark) darkColors else light
    if (index < 0) return palette.first()
    return palette[index % palette.size]
}

@Composable
fun CategoryBadge(name: String, toneIndex: Int) {
    val tone = categoryTone(toneIndex)
    Surface(color = tone.first, contentColor = tone.second, shape = CircleShape) {
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun EmptyState(hasFilter: Boolean, onAddNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = if (hasFilter) "🗂" else "✍️", fontSize = 44.sp)
        Spacer(Modifier.height(14.dp))
        Text(
            text = if (hasFilter) "这个分类还是空的" else "还没有笔记",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "换个分类看看，或者直接记一条" else "想到什么就记下来",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = onAddNote,
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape
        ) {
            Text(
                text = "新建笔记",
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        }
    }
}

/** 新建 / 重命名分类共用的输入弹窗 */
@Composable
private fun CategoryNameDialog(
    title: String,
    initial: String,
    confirmText: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var name by remember { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                label = { Text("分类名") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(name) }, enabled = name.isNotBlank()) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}

/** 今天显示时间，昨天显示「昨天」，更早显示日期 */
internal fun formatTime(millis: Long): String {
    val now = Calendar.getInstance()
    val then = Calendar.getInstance().apply { timeInMillis = millis }
    val sameYear = now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
    val dayDiff = now.get(Calendar.DAY_OF_YEAR) - then.get(Calendar.DAY_OF_YEAR)
    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
    return when {
        sameYear && dayDiff == 0 -> "今天 $time"
        sameYear && dayDiff == 1 -> "昨天 $time"
        sameYear -> SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
        else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(millis))
    }
}
