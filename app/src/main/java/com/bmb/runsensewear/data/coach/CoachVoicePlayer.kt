package com.bmb.runsensewear.data.coach

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale

private const val TAG = "CoachVoice"

/**
 * AI 코치 음성 안내.
 *
 * 1) AI 서버가 만들어 준 mp3(voice_file)를 스트리밍 재생
 * 2) 파일이 없거나 재생 실패하면 워치 TTS(한국어)로 읽어 줍니다
 *
 * 갤럭시 워치 5/6는 스피커가 있어 그대로 들리고, 블루투스 이어폰이 워치에 연결돼 있으면 그쪽으로 나갑니다.
 */
class CoachVoicePlayer(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val _speaking = MutableStateFlow(false)
    val speaking: StateFlow<Boolean> = _speaking

    private var player: MediaPlayer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false

    init {
        tts = TextToSpeech(context) { status ->
            ttsReady = status == TextToSpeech.SUCCESS
            if (ttsReady) {
                tts?.language = Locale.KOREAN
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) { _speaking.value = true }
                    override fun onDone(utteranceId: String?) { _speaking.value = false }
                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) { _speaking.value = false }
                })
            }
        }
    }

    fun speak(message: CoachingMessage) {
        stop()
        val url = message.voiceUrl
        if (url != null) playUrl(url, fallbackText = message.text) else speakTts(message.text)
    }

    fun stop() {
        player?.runCatching { if (isPlaying) stop(); release() }
        player = null
        tts?.stop()
        _speaking.value = false
    }

    private fun playUrl(url: String, fallbackText: String) {
        scope.launch {
            runCatching {
                val mp = MediaPlayer().apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_NAVIGATION_GUIDANCE)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    setDataSource(url)
                    setOnPreparedListener {
                        _speaking.value = true
                        it.start()
                    }
                    setOnCompletionListener {
                        _speaking.value = false
                        it.release()
                        if (player === it) player = null
                    }
                    setOnErrorListener { mp, what, extra ->
                        Log.w(TAG, "음성 파일 재생 실패 what=$what extra=$extra, TTS로 대체")
                        _speaking.value = false
                        mp.release()
                        if (player === mp) player = null
                        speakTts(fallbackText)
                        true
                    }
                }
                player = mp
                mp.prepareAsync()
            }.onFailure {
                Log.w(TAG, "MediaPlayer 준비 실패, TTS로 대체", it)
                speakTts(fallbackText)
            }
        }
    }

    private fun speakTts(text: String) {
        val engine = tts ?: return
        if (!ttsReady) return
        engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "coach-${System.currentTimeMillis()}")
    }
}
