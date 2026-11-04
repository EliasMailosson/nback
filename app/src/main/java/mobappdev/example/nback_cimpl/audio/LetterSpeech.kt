package mobappdev.example.nback_cimpl.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import kotlinx.coroutines.CompletableDeferred
import java.util.Locale

enum class SpeechReady {
    Ready,
    Missing,
}

/**
 * Speaks one letter at a time.
 * The init listener only reports whether a voice exists. It does not start a round.
 */
interface LetterSpeech {
    suspend fun prepare(): SpeechReady
    fun speak(letter: String)
    fun stop()
    fun shutdown()
}

fun interface SpeechFactory {
    fun create(): LetterSpeech
}

class AndroidLetterSpeech(context: Context) : LetterSpeech {
    private val initStatus = CompletableDeferred<Int>()
    private var engine: TextToSpeech? = null
    private var prepared: SpeechReady? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            if (!initStatus.isCompleted) {
                initStatus.complete(status)
            }
        }
    }

    override suspend fun prepare(): SpeechReady {
        prepared?.let { return it }
        val status = initStatus.await()
        val tts = engine
        val result = if (status != TextToSpeech.SUCCESS || tts == null) {
            SpeechReady.Missing
        } else {
            val language = tts.setLanguage(Locale.US)
            if (
                language == TextToSpeech.LANG_MISSING_DATA ||
                language == TextToSpeech.LANG_NOT_SUPPORTED
            ) {
                SpeechReady.Missing
            } else {
                SpeechReady.Ready
            }
        }
        prepared = result
        return result
    }

    override fun speak(letter: String) {
        val tts = engine ?: return
        tts.stop()
        tts.speak(letter, TextToSpeech.QUEUE_FLUSH, null as Bundle?, "nback-letter")
    }

    override fun stop() {
        engine?.stop()
    }

    override fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        prepared = null
    }
}
