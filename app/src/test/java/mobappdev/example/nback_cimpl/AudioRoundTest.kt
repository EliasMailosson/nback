package mobappdev.example.nback_cimpl

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.audio.LetterSpeech
import mobappdev.example.nback_cimpl.audio.SpeechFactory
import mobappdev.example.nback_cimpl.audio.SpeechReady
import mobappdev.example.nback_cimpl.game.AudioLetters
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudioRoundTest {

    @Test
    fun lettersFollowTheGeneratorValues() {
        assertEquals(listOf("A", "F", "K", "M", "R", "U"), (1..6).map { AudioLetters.letter(it) })
        assertNull(AudioLetters.letter(0))
        assertNull(AudioLetters.letter(7))
    }

    @Test
    fun theSameValueIsTheSameLetterForAMatch() {
        val round = NBackRound(listOf(1, 2, 1)).start().closeEvent().closeEvent().press()
        assertEquals(1, round.correctPresses)
        assertEquals("A", AudioLetters.letter(round.events[0]))
        assertEquals(AudioLetters.letter(round.events[0]), AudioLetters.letter(round.events[2]))
    }

    @Test
    fun eachEventIsSpokenOnceIncludingRepeats() {
        val events = List(BasicRound.EVENT_COUNT) { index -> if (index % 2 == 0) 1 else 2 }
        val speech = CountingSpeech()
        val vm = viewModel(speech, events)

        vm.setGameType(GameType.Audio)
        vm.startGame()

        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertEquals(events.map { AudioLetters.letter(it) }, speech.spoken)
        assertEquals(10, speech.spoken.count { it == "A" })
        assertEquals(1, speech.stops)
    }

    @Test
    fun aMissingVoiceDoesNotStartTheAudioRound() {
        val speech = CountingSpeech(ready = SpeechReady.Missing)
        val vm = viewModel(speech, List(BasicRound.EVENT_COUNT) { 1 })

        vm.setGameType(GameType.Audio)
        vm.startGame()

        assertEquals(RoundPhase.Ready, vm.gameState.value.phase)
        assertTrue(speech.spoken.isEmpty())
        assertEquals("Ingen röst är tillgänglig. Välj visuellt läge.", vm.gameState.value.message)
    }

    @Test
    fun aFinishedAudioRoundUsesTheSameRecord() {
        val store = MemoryHighScores()
        val speech = CountingSpeech()
        lateinit var vm: GameVM
        val pause = Pause {
            vm.checkMatch()
            currentCoroutineContext().ensureActive()
        }
        vm = viewModel(speech, List(BasicRound.EVENT_COUNT) { 1 }, pause, store)

        vm.setGameType(GameType.Audio)
        vm.startGame()

        assertEquals(18, vm.gameState.value.score)
        assertEquals(18, vm.highscore.value)
        assertEquals("Nytt rekord", vm.gameState.value.message)
        assertEquals(listOf("A"), speech.spoken.toSet().toList())
    }

    @Test
    fun abortBeforeTheVoiceIsReadyDoesNotStartTheRound() = runBlocking {
        val release = CompletableDeferred<Unit>()
        val speech = object : LetterSpeech {
            val spoken = mutableListOf<String>()
            override suspend fun prepare(): SpeechReady {
                release.await()
                return SpeechReady.Ready
            }

            override fun speak(letter: String) {
                spoken += letter
            }

            override fun stop() = Unit
            override fun shutdown() = Unit
        }
        val vm = GameVM(
            highScores = MemoryHighScores(),
            generator = SequenceGenerator { _, _, _, _, _ ->
                NBackGeneration.Ok(IntArray(BasicRound.EVENT_COUNT) { 1 })
            },
            pause = QuietPause(),
            speechFactory = SpeechFactory { speech },
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )

        vm.setGameType(GameType.Audio)
        vm.startGame()
        vm.abortRound()
        release.complete(Unit)

        assertNotEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertTrue(speech.spoken.isEmpty())
    }

    @Test
    fun abortingAudioStopsSpeechAndDoesNotSave() {
        val store = MemoryHighScores(start = 3)
        val speech = CountingSpeech()
        val vm = viewModel(speech, List(BasicRound.EVENT_COUNT) { 1 }, StuckPause(), store)

        vm.setGameType(GameType.Audio)
        vm.startGame()
        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertEquals(listOf("A"), speech.spoken)

        vm.abortRound()

        assertEquals(RoundPhase.Aborted, vm.gameState.value.phase)
        assertTrue(store.results.isEmpty())
        assertEquals(3, vm.highscore.value)
        assertEquals(1, speech.stops)
    }

    @Test
    fun visualModeDoesNotSpeak() {
        val speech = CountingSpeech()
        val vm = viewModel(speech, List(BasicRound.EVENT_COUNT) { 3 })

        vm.setGameType(GameType.Visual)
        vm.startGame()

        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertTrue(speech.spoken.isEmpty())
    }

    private fun viewModel(
        speech: CountingSpeech,
        events: List<Int>,
        pause: Pause = QuietPause(),
        store: MemoryHighScores = MemoryHighScores(),
    ): GameVM {
        val generator = SequenceGenerator { _, _, _, _, _ ->
            NBackGeneration.Ok(events.toIntArray())
        }
        return GameVM(
            highScores = store,
            generator = generator,
            pause = pause,
            speechFactory = SpeechFactory { speech },
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
    }
}

private class CountingSpeech(var ready: SpeechReady = SpeechReady.Ready) : LetterSpeech {
    val spoken = mutableListOf<String>()
    var stops = 0

    override suspend fun prepare(): SpeechReady = ready

    override fun speak(letter: String) {
        spoken += letter
    }

    override fun stop() {
        stops += 1
    }

    override fun shutdown() = Unit
}

private class StuckPause : Pause {
    override suspend fun wait(millis: Long) {
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { }
    }
}

private class QuietPause : Pause {
    override suspend fun wait(millis: Long) {
        currentCoroutineContext().ensureActive()
    }
}
