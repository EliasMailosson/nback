package mobappdev.example.nback_cimpl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.audio.SpeechFactory
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.PressFeedback
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchResponseTest {

    @Test
    fun pressesOutsideARunningEventDoNothing() {
        val ready = NBackRound(listOf(1, 2, 1))
        assertEquals(ready, ready.press())

        val finished = ready.start().closeEvent().closeEvent().closeEvent()
        assertEquals(RoundPhase.Finished, finished.phase)
        assertEquals(finished, finished.press())

        val aborted = ready.start().abort()
        assertEquals(RoundPhase.Aborted, aborted.phase)
        assertEquals(aborted, aborted.press())
    }

    @Test
    fun warmupPressesAreIgnored() {
        var round = NBackRound(listOf(1, 1, 2)).start()
        round = round.press()
        assertEquals(0, round.correctPresses)
        assertEquals(0, round.wrongPresses)
        assertFalse(round.answered)

        round = round.closeEvent().press()
        assertEquals(0, round.correctPresses)
        assertEquals(0, round.wrongPresses)
        assertFalse(round.answered)
    }

    @Test
    fun aCorrectPressAndAWrongPressAreCountedOnce() {
        var round = NBackRound(listOf(4, 7, 4, 1)).start()
        round = round.closeEvent().closeEvent()
        assertTrue(round.isMatch())
        round = round.press()
        assertEquals(1, round.correctPresses)
        assertEquals(0, round.wrongPresses)

        round = round.closeEvent()
        assertFalse(round.isMatch())
        round = round.press()
        assertEquals(1, round.correctPresses)
        assertEquals(1, round.wrongPresses)
    }

    @Test
    fun aSecondPressOnTheSameEventIsIgnoredEvenWhenTheFirstWasWrong() {
        var round = NBackRound(listOf(1, 2, 3)).start()
        round = round.closeEvent().closeEvent().press()
        val afterFirst = round.press().press()

        assertEquals(1, afterFirst.wrongPresses)
        assertEquals(0, afterFirst.correctPresses)
        assertTrue(afterFirst.answered)
        assertEquals(afterFirst, round.press())
    }

    @Test
    fun theBlankPartStillBelongsToTheSameEvent() {
        var round = NBackRound(listOf(5, 6, 5, 8)).start().closeEvent().closeEvent()
        round = round.blank()
        assertFalse(round.stimulusVisible)
        round = round.press()
        assertEquals(1, round.correctPresses)

        val next = round.closeEvent()
        assertFalse(next.answered)
        val pressedAgain = next.press()
        assertEquals(1, pressedAgain.wrongPresses)
        assertEquals(1, pressedAgain.correctPresses)
    }

    @Test
    fun aCorrectPressSurvivesTheBlankStep() = runBlocking {
        val events = listOf(4, 7, 4) + List(BasicRound.EVENT_COUNT - 3) { 8 }
        val pause = SteppedPause()
        val vm = gameVm(events, pause)

        vm.startGame()
        repeat(4) { pause.step() }
        assertEquals(3, vm.gameState.value.eventNumber)
        assertTrue(vm.gameState.value.stimulusVisible)

        vm.checkMatch()
        assertEquals(1, vm.gameState.value.correctPresses)
        assertTrue(vm.gameState.value.answered)

        pause.step()
        assertFalse(vm.gameState.value.stimulusVisible)
        assertEquals(3, vm.gameState.value.eventNumber)
        assertEquals(1, vm.gameState.value.correctPresses)
        assertTrue(vm.gameState.value.answered)
        vm.abortRound()
    }

    @Test
    fun onlyAWrongFirstPressAsksForFeedback() {
        val warmup = NBackRound(listOf(1, 2, 3, 2)).start().press()
        assertEquals(PressFeedback.None, warmup.feedback)

        val wrong = warmup.closeEvent().closeEvent().press()
        assertEquals(PressFeedback.Wrong, wrong.feedback)
        assertEquals(wrong, wrong.press())

        val next = wrong.closeEvent()
        assertEquals(PressFeedback.None, next.feedback)

        val correct = next.press()
        assertEquals(PressFeedback.None, correct.feedback)
        assertEquals(1, correct.correctPresses)
        assertEquals(1, correct.wrongPresses)
    }

    @Test
    fun aWrongPressStaysThroughTheBlankStep() = runBlocking {
        val events = listOf(1, 2, 3) + List(BasicRound.EVENT_COUNT - 3) { 8 }
        val pause = SteppedPause()
        val vm = gameVm(events, pause)

        vm.startGame()
        repeat(4) { pause.step() }
        assertEquals(3, vm.gameState.value.eventNumber)
        vm.checkMatch()
        assertEquals(PressFeedback.Wrong, vm.gameState.value.feedback)
        assertTrue(vm.gameState.value.answered)

        pause.step()
        assertFalse(vm.gameState.value.stimulusVisible)
        assertEquals(PressFeedback.Wrong, vm.gameState.value.feedback)
        assertTrue(vm.gameState.value.answered)
        assertEquals(1, vm.gameState.value.wrongPresses)
        vm.abortRound()
    }
}

private fun gameVm(events: List<Int>, pause: Pause): GameVM {
    return GameVM(
        highScores = MemoryHighScores(),
        generator = SequenceGenerator { _, _, _, _, _ -> NBackGeneration.Ok(events.toIntArray()) },
        pause = pause,
        speechFactory = SpeechFactory { SilentSpeech },
        gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    )
}

private class SteppedPause : Pause {
    private val entered = Channel<Unit>(Channel.UNLIMITED)
    private val advance = Channel<Unit>(Channel.UNLIMITED)

    override suspend fun wait(millis: Long) {
        entered.send(Unit)
        advance.receive()
        currentCoroutineContext().ensureActive()
    }

    suspend fun step() {
        entered.receive()
        advance.send(Unit)
    }
}
