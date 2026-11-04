package mobappdev.example.nback_cimpl

import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.ensureActive
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.audio.LetterSpeech
import mobappdev.example.nback_cimpl.audio.SpeechFactory
import mobappdev.example.nback_cimpl.audio.SpeechReady
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeechTest {

    @Test
    fun openingTheViewModelDoesNotSpeak() {
        val speech = FakeSpeech()
        val vm = viewModel(speech)

        assertTrue(speech.spoken.isEmpty())
        assertEquals(RoundPhase.Ready, vm.gameState.value.phase)
    }

    @Test
    fun aMissingVoiceDoesNotStartARoundAndVisualStillWorks() {
        val speech = FakeSpeech(ready = SpeechReady.Missing)
        val vm = viewModel(speech)

        vm.setGameType(GameType.Audio)
        vm.startGame()

        assertEquals(SpeechReady.Missing, speech.prepared)
        assertTrue(speech.spoken.isEmpty())
        assertEquals(1, speech.shutdowns)
        assertEquals("Ingen röst är tillgänglig. Välj visuellt läge.", vm.gameState.value.message)
        assertNotEquals(RoundPhase.Playing, vm.gameState.value.phase)

        vm.setGameType(GameType.Visual)
        vm.startGame()
        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertTrue(speech.spoken.isEmpty())
    }

    @Test
    fun aReadyVoiceDoesNotSpeakUntilARoundAsks() {
        val speech = FakeSpeech(ready = SpeechReady.Ready)
        val vm = viewModel(speech)

        vm.setGameType(GameType.Audio)
        assertTrue(speech.spoken.isEmpty())

        vm.startGame()
        assertEquals(20, speech.spoken.size)
        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
    }

    @Test
    fun abortAndClearStopTheVoice() {
        val speech = FakeSpeech(ready = SpeechReady.Ready)
        val vm = viewModel(speech, pause = FrozenPause())
        vm.setGameType(GameType.Audio)
        vm.startGame()
        vm.abortRound()
        assertEquals(RoundPhase.Aborted, vm.gameState.value.phase)
        assertEquals(1, speech.stops)

        ViewModelStore().apply { put("k", vm) }.clear()
        assertEquals(1, speech.shutdowns)
    }

    private fun viewModel(speech: FakeSpeech, pause: Pause = ImmediatePause()): GameVM {
        val generator = SequenceGenerator { _, _, _, _, _ ->
            NBackGeneration.Ok(IntArray(20) { 1 })
        }
        return GameVM(
            highScores = MemoryHighScores(),
            generator = generator,
            pause = pause,
            speechFactory = SpeechFactory { speech },
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
    }

}

private class FakeSpeech(var ready: SpeechReady = SpeechReady.Ready) : LetterSpeech {
    val spoken = mutableListOf<String>()
    var stops = 0
    var shutdowns = 0
    var prepared: SpeechReady? = null

    override suspend fun prepare(): SpeechReady {
        prepared = ready
        return ready
    }

    override fun speak(letter: String) {
        spoken += letter
    }

    override fun stop() {
        stops += 1
    }

    override fun shutdown() {
        shutdowns += 1
    }
}

private class ImmediatePause : Pause {
    override suspend fun wait(millis: Long) {
        kotlinx.coroutines.currentCoroutineContext().ensureActive()
    }
}

internal object SilentSpeech : LetterSpeech {
    override suspend fun prepare(): SpeechReady = SpeechReady.Missing
    override fun speak(letter: String) = Unit
    override fun stop() = Unit
    override fun shutdown() = Unit
}

private class FrozenPause : Pause {
    override suspend fun wait(millis: Long) {
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { }
    }
}
