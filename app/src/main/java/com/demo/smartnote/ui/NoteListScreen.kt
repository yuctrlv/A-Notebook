package com.demo.smartnote.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.smartnote.data.Note
import com.demo.smartnote.data.NoteCategory
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * 首页：一张摊开的纸。
 * 上面是日期和书名，中间是分类索引，下面是按时间排的条目。
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
            FloatingActionButton(
                onClick = onAddNote,
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "新建笔记")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            item { Masthead(totalCount = totalCount) }
            item {
                IndexTabs(
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
                    NoteEntry(note = note, onClick = { onOpenNote(note.id) })
                }
            }
        }
    }
}

/** 报头：日期 + 书名 + 双线 */
@Composable
private fun Masthead(totalCount: Int) {
    val today = LocalDate.now()
    val week = listOf("星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日")[today.dayOfWeek.value - 1]
    Column(modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 36.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "${today.monthValue} 月 ${today.dayOfMonth} 日 · $week",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = "$totalCount 则",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "一个笔记本",
            fontFamily = FontFamily.Serif,
            fontSize = 38.sp,
            lineHeight = 44.sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 2.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(14.dp))
        HorizontalDivider(thickness = 1.5.dp, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(3.dp))
        HorizontalDivider(thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
        Spacer(Modifier.height(6.dp))
    }
}

/** 分类索引：像书页边上的索引标签，纯文字 + 下划线 */
@Composable
private fun IndexTabs(
    selected: NoteCategory?,
    counts: Map<NoteCategory, Int>,
    totalCount: Int,
    onSelect: (NoteCategory?) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item {
            IndexTab(
                label = "全部",
                count = totalCount,
                selected = selected == null,
                accent = MaterialTheme.colorScheme.onBackground,
                onClick = { onSelect(null) }
            )
        }
        items(NoteCategory.entries) { category ->
            IndexTab(
                label = category.label,
                count = counts[category] ?: 0,
                selected = selected == category,
                accent = categoryColor(category),
                onClick = { onSelect(category) }
            )
        }
    }
    Spacer(Modifier.height(8.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
}

@Composable
private fun IndexTab(label: String, count: Int, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(top = 12.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = label,
                fontFamily = FontFamily.Serif,
                fontSize = 16.sp,
                color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (count > 0) {
                Spacer(Modifier.width(3.dp))
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .height(2.dp)
                .width(if (selected) 26.dp else 0.dp)
                .background(accent)
        )
    }
}

/** 单条笔记：像手账里贴上去的一张卡片 */
@Composable
private fun NoteEntry(note: Note, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 7.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(3.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = note.displayTitle.ifBlank { "无题" },
                    fontFamily = FontFamily.Serif,
                    fontSize = 19.sp,
                    lineHeight = 26.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(12.dp))
                CategoryBadge(note.category)
            }
            if (note.content.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = note.content.replace("\n", " ").trim(),
                    fontSize = 14.sp,
                    lineHeight = 23.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = formatTime(note.updatedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${note.content.length} 字",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}

/** 每个分类一个颜色，像不同颜色的索引标签 */
@Composable
fun categoryColor(category: NoteCategory): Color = when (category) {
    NoteCategory.ENGLISH -> Color(0xFF33506B)
    NoteCategory.QUOTE -> Color(0xFF6E4A6B)
    NoteCategory.IDIOM -> Color(0xFF3F5A44)
    NoteCategory.OTHER -> Color(0xFF6F6659)
}

/** 分类小标签：斜体衬线 + 颜色，不用色块 */
@Composable
fun CategoryBadge(category: NoteCategory) {
    Text(
        text = category.label,
        fontFamily = FontFamily.Serif,
        fontStyle = FontStyle.Italic,
        fontSize = 13.sp,
        color = categoryColor(category)
    )
}

@Composable
private fun EmptyState(hasFilter: Boolean, onAddNote: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp, vertical = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = if (hasFilter) "这页还空着" else "第一页还是白纸",
            fontFamily = FontFamily.Serif,
            fontSize = 20.sp,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "换个分类看看" else "写点什么吧，写下来就忘不掉了",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(20.dp))
        Surface(
            onClick = onAddNote,
            color = Color.Transparent,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            shape = CircleShape
        ) {
            Text(
                text = "写第一条",
                fontFamily = FontFamily.Serif,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
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
