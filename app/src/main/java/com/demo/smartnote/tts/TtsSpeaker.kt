package com.demo.smartnote.tts

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

/**
 * 对系统 TextToSpeech 的薄封装：
 * - 自动判断文本是中文还是英文，选择对应发音人
 * - 暴露 isReady / isSpeaking / errorMessage 给 Compose 直接消费
 * - 长文本按句切块后排队朗读，避免单次合成被截断
 */
class TtsSpeaker(private val context: Context) {

    private var engine: TextToSpeech? = null
    private var pendingChunks = 0
    private val mainHandler = Handler(Looper.getMainLooper())

    /** 引擎是否初始化完成 */
    var isReady by mutableStateOf(false)
        private set

    var isSpeaking by mutableStateOf(false)
        private set

    /** 初始化失败 / 语言包缺失等提示，UI 上直接展示 */
    var errorMessage by mutableStateOf<String?>(null)
        private set

    /** 当前使用的语言，展示用 */
    var languageLabel by mutableStateOf("—")
        private set

    fun init() {
        if (engine != null) return
        engine = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady = true
                errorMessage = null
                engine?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) = post { isSpeaking = true }

                    override fun onDone(utteranceId: String?) = finishOne()

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) = finishOne()

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        finishOne()
                        post { errorMessage = "朗读失败，错误码 $errorCode" }
                    }

                    override fun onStop(utteranceId: String?, interrupted: Boolean) = finishOne()
                })
            } else {
                isReady = false
                errorMessage = "TTS 初始化失败，请检查系统的「文字转语音」引擎"
            }
        }
    }

    /**
     * 开始朗读。
     * @param rate 语速，1.0 为正常
     * @param pitch 音调，1.0 为正常
     */
    fun speak(text: String, rate: Float, pitch: Float) {
        val tts = engine
        if (tts == null || !isReady) {
            errorMessage = "语音引擎尚未就绪，请稍后再试"
            return
        }
        val content = text.trim()
        if (content.isEmpty()) return

        val isChinese = CJK.containsMatchIn(content)
        val locale = if (isChinese) Locale.SIMPLIFIED_CHINESE else Locale.US
        when (tts.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_MISSING_DATA, TextToSpeech.LANG_NOT_SUPPORTED -> {
                errorMessage = if (isChinese) "系统缺少中文语音包，请在系统设置中安装" else "系统缺少英文语音包，请在系统设置中安装"
                return
            }
        }

        tts.language = locale
        tts.setSpeechRate(rate)
        tts.setPitch(pitch)
        languageLabel = if (isChinese) "中文" else "English"
        errorMessage = null
        isSpeaking = true

        val chunks = splitForTts(content)
        pendingChunks = chunks.size
        val sessionId = System.currentTimeMillis()
        chunks.forEachIndexed { index, chunk ->
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            tts.speak(chunk, queueMode, null, "note-$sessionId-$index")
        }
    }

    fun stop() {
        pendingChunks = 0
        engine?.stop()
        isSpeaking = false
    }

    /** 长文本会被切成多段排队朗读，最后一段结束才算读完整篇 */
    private fun finishOne() = post {
        pendingChunks = (pendingChunks - 1).coerceAtLeast(0)
        if (pendingChunks == 0) isSpeaking = false
    }

    /** Compose 页面销毁时调用，释放底层引擎 */
    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        isReady = false
        isSpeaking = false
        languageLabel = "—"
    }

    private fun postSpeaking(value: Boolean) = post { isSpeaking = value }

    private fun post(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else mainHandler.post(block)
    }

    private companion object {
        val CJK = Regex("[\\u4e00-\\u9fa5]")

        /** 单个 utterance 有长度上限，按句号/换行切开，每块不超过 800 字 */
        const val MAX_CHUNK = 800

        fun splitForTts(text: String): List<String> {
            if (text.length <= MAX_CHUNK) return listOf(text)
            val result = mutableListOf<String>()
            val buffer = StringBuilder()
            text.split(Regex("(?<=[。！？!?\\.\\n])")).forEach { sentence ->
                if (buffer.length + sentence.length > MAX_CHUNK && buffer.isNotEmpty()) {
                    result.add(buffer.toString())
                    buffer.clear()
                }
                if (sentence.length > MAX_CHUNK) {
                    sentence.chunked(MAX_CHUNK).forEach { result.add(it) }
                } else {
                    buffer.append(sentence)
                }
            }
            if (buffer.isNotEmpty()) result.add(buffer.toString())
            return result
        }
    }
}
