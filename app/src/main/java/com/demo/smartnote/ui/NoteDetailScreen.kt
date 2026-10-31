package com.demo.smartnote.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.demo.smartnote.tts.TtsSpeaker

/**
 * 详情页：看全文 + 朗读/停止 + 编辑 + 删除
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteDetailScreen(
    noteId: Long,
    viewModel: NotesViewModel,
    onBack: () -> Unit,
    onEdit: (Long) -> Unit
) {
    val note by viewModel.observeNote(noteId).collectAsStateWithLifecycle(initialValue = null)
    var showDeleteDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    // 每个详情页持有一个 TTS 实例，离开页面时释放
    val speaker = remember { TtsSpeaker(context) }
    DisposableEffect(Unit) {
        speaker.init()
        onDispose { speaker.shutdown() }
    }

    var rate by remember { mutableFloatStateOf(1.0f) }
    var pitch by remember { mutableFloatStateOf(1.0f) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("笔记详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onEdit(noteId) }) {
                        Icon(Icons.Default.Edit, contentDescription = "编辑")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "删除")
                    }
                }
            )
        }
    ) { innerPadding ->
        val current = note
        if (current == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("笔记不存在或已删除", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                CategoryBadge(current.category)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = current.displayTitle.ifBlank { "（无标题）" },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "更新于 ${formatTime(current.updatedAt)} · 创建于 ${formatTime(current.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(16.dp))
                SelectionContainer {
                    Text(
                        text = current.content.ifBlank { "（正文为空）" },
                        style = MaterialTheme.typography.bodyLarge,
                        lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.3f
                    )
                }
                Spacer(Modifier.height(24.dp))
                TtsPanel(
                    speaker = speaker,
                    text = current.content,
                    rate = rate,
                    pitch = pitch,
                    onRateChange = { rate = it },
                    onPitchChange = { pitch = it }
                )
                Spacer(Modifier.height(96.dp))
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("删除这条笔记？") },
            text = { Text("删除后无法恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        speaker.stop()
                        viewModel.delete(noteId)
                        onBack()
                    }
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("取消") }
            }
        )
    }
}

@Composable
private fun TtsPanel(
    speaker: TtsSpeaker,
    text: String,
    rate: Float,
    pitch: Float,
    onRateChange: (Float) -> Unit,
    onPitchChange: (Float) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("朗读", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { speaker.speak(text, rate, pitch) },
                    enabled = speaker.isReady && !speaker.isSpeaking && text.isNotBlank()
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("朗读")
                }
                OutlinedButton(
                    onClick = { speaker.stop() },
                    enabled = speaker.isSpeaking
                ) {
                    Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.size(6.dp))
                    Text("停止")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text("语速  ${"%.1f".format(rate)}x", style = MaterialTheme.typography.labelLarge)
            Slider(value = rate, onValueChange = onRateChange, valueRange = 0.5f..2.0f)

            Spacer(Modifier.height(4.dp))
            Text("音调  ${"%.1f".format(pitch)}x", style = MaterialTheme.typography.labelLarge)
            Slider(value = pitch, onValueChange = onPitchChange, valueRange = 0.5f..2.0f)

            Spacer(Modifier.height(8.dp))
            val status = when {
                speaker.errorMessage != null -> speaker.errorMessage!!
                !speaker.isReady -> "语音引擎初始化中…"
                speaker.isSpeaking -> "正在朗读（${speaker.languageLabel}）"
                else -> "引擎就绪 · 语言 ${speaker.languageLabel}"
            }
            Text(
                text = status,
                style = MaterialTheme.typography.bodySmall,
                color = if (speaker.errorMessage != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
