package com.demo.smartnote.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.smartnote.data.Note
import com.demo.smartnote.data.NoteCategory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页：Material 3 Expressive 风格。
 * 大标题 + 会滑动的分类选择块 + 大圆角彩色卡片。
 */
@Composable
fun NoteListScreen(
    viewModel: NotesViewModel,
    onAddNote: () -> Unit,
    onOpenNote: (Long) -> Unit
) {
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val counts by viewModel.counts.collectAsStateWithLifecycle()
    val totalCount by viewModel.totalCount.collectAsStateWithLifecycle()

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
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
            contentPadding = PaddingValues(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Header(
                    totalCount = totalCount,
                    selectedCategory = selectedCategory
                )
            }
            item {
                CategorySwitcher(
                    selected = selectedCategory,
                    counts = counts,
                    totalCount = totalCount,
                    onSelect = viewModel::selectCategory
                )
            }
            if (notes.isEmpty()) {
                item { EmptyState(hasFilter = selectedCategory != null, onAddNote = onAddNote) }
            } else {
                items(notes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(totalCount: Int, selectedCategory: NoteCategory?) {
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 32.dp)) {
        Text(
            text = "一个笔记本",
            style = MaterialTheme.typography.displaySmall,
            fontSize = 34.sp,
            lineHeight = 40.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val label = selectedCategory?.label ?: "全部"
            Text(
                text = "$label · $totalCount 条",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(
                        selectedCategory?.let { categoryTone(it).first }
                            ?: MaterialTheme.colorScheme.primary
                    )
            )
        }
    }
}

/** 分类切换：一个整体凹槽，选中的块会滑过去（颜色 + 圆角动画） */
@Composable
private fun CategorySwitcher(
    selected: NoteCategory?,
    counts: Map<NoteCategory, Int>,
    totalCount: Int,
    onSelect: (NoteCategory?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SwitcherItem(
                label = "全部",
                count = totalCount,
                selected = selected == null,
                tone = categoryTone(null),
                onClick = { onSelect(null) }
            )
        }
        items(NoteCategory.entries) { category ->
            SwitcherItem(
                label = category.label,
                count = counts[category] ?: 0,
                selected = selected == category,
                tone = categoryTone(category),
                onClick = { onSelect(category) }
            )
        }
    }
}

@Composable
private fun SwitcherItem(
    label: String,
    count: Int,
    selected: Boolean,
    tone: Pair<Color, Color>,
    onClick: () -> Unit
) {
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
    val corner by animateDpAsState(
        targetValue = if (selected) 20.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = 700f),
        label = "switcher-corner"
    )

    Surface(
        onClick = onClick,
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(corner)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 11.dp),
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
private fun NoteCard(note: Note, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val tone = categoryTone(note.category)
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
                Surface(
                    color = tone.second.copy(alpha = 0.10f),
                    contentColor = tone.second,
                    shape = CircleShape
                ) {
                    Text(
                        text = note.category.label,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
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
 * 分类配色：每个分类一组「容器色 + 前景色」。
 * 这是 Expressive 风格的关键——大块浅色底 + 同色系深色字。
 */
@Composable
fun categoryTone(category: NoteCategory?): Pair<Color, Color> {
    val dark = androidx.compose.foundation.isSystemInDarkTheme()
    return when (category) {
        NoteCategory.ENGLISH ->
            if (dark) Color(0xFF28306B) to Color(0xFFD9DDFF) else Color(0xFFE3E7FF) to Color(0xFF1B2A6B)
        NoteCategory.QUOTE ->
            if (dark) Color(0xFF45255E) to Color(0xFFF0DDFF) else Color(0xFFF2E4FF) to Color(0xFF431C66)
        NoteCategory.IDIOM ->
            if (dark) Color(0xFF17453A) to Color(0xFFC9F3E2) else Color(0xFFDDF3E9) to Color(0xFF0C4232)
        NoteCategory.OTHER ->
            if (dark) Color(0xFF2E2D38) to Color(0xFFE4E2EC) else Color(0xFFEDECF3) to Color(0xFF3A3946)
        null ->
            if (dark) Color(0xFF2B2A3A) to Color(0xFFE4E2FF) else Color(0xFFE9E8FF) to Color(0xFF241F6B)
    }
}

@Composable
fun categoryColor(category: NoteCategory): Color = categoryTone(category).second

/** 详情页顶部的分类标签，沿用同一套配色 */
@Composable
fun CategoryBadge(category: NoteCategory) {
    val tone = categoryTone(category)
    Surface(color = tone.first, contentColor = tone.second, shape = CircleShape) {
        Text(
            text = category.label,
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
            text = if (hasFilter) "换个分类看看" else "想到什么就记下来",
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
