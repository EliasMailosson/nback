package mobappdev.example.nback_cimpl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Test

class RoundResultTest {

    @Test
    fun wrongPressIsKeptWhenTheShownScoreIsZero() {
        var round = NBackRound(listOf(1, 2, 9, 2)).start()
        round = round.closeEvent().closeEvent().press()
        assertEquals(1, round.wrongPresses)
        assertEquals(0, round.score)

        round = round.closeEvent().press().closeEvent()
        assertEquals(RoundPhase.Finished, round.phase)
        assertEquals(1, round.correctPresses)
        assertEquals(1, round.wrongPresses)
        assertEquals(0, round.score)
    }

    @Test
    fun silenceOnANonMatchScoresNothingAndIsNotAMiss() {
        val round = NBackRound(listOf(1, 2, 3))
            .start()
            .closeEvent()
            .closeEvent()
            .closeEvent()

        assertEquals(0, round.missedMatches)
        assertEquals(0, round.score)
        assertEquals(0, round.correctPresses)
    }

    @Test
    fun anUnansweredLastMatchIsMissedOnce() {
        val round = NBackRound(listOf(4, 8, 4))
            .start()
            .closeEvent()
            .closeEvent()
            .closeEvent()

        assertEquals(RoundPhase.Finished, round.phase)
        assertEquals(1, round.missedMatches)
        assertEquals(0, round.score)
    }

    @Test
    fun anAnsweredLastMatchIsNotMissed() {
        var round = NBackRound(listOf(4, 8, 4)).start().closeEvent().closeEvent()
        round = round.press().closeEvent()

        assertEquals(RoundPhase.Finished, round.phase)
        assertEquals(0, round.missedMatches)
        assertEquals(1, round.correctPresses)
        assertEquals(1, round.score)
    }

    @Test
    fun aNewRoundStartsAtZero() {
        val played = NBackRound(listOf(1, 2, 1))
            .start()
            .closeEvent()
            .closeEvent()
            .press()
            .closeEvent()
        val again = played.start()

        assertEquals(RoundPhase.Playing, again.phase)
        assertEquals(0, again.correctPresses)
        assertEquals(0, again.wrongPresses)
        assertEquals(0, again.missedMatches)
        assertEquals(0, again.score)
        assertEquals(0, again.index)
    }

    @Test
    fun abortDoesNotCloseTheRoundAsAResult() {
        val round = NBackRound(listOf(1, 2, 1)).start().closeEvent().closeEvent().abort()

        assertEquals(RoundPhase.Aborted, round.phase)
        assertEquals(0, round.missedMatches)
    }

    @Test
    fun answeringAllSixMatchesScoresSix() = runBlocking {
        val matchIndexes = setOf(2, 5, 8, 11, 14, 19)
        val events = MutableList(20) { 1 }
        for (index in 2 until 20) {
            val earlier = events[index - 2]
            events[index] = if (index in matchIndexes) earlier else if (earlier == 1) 2 else 1
        }
        lateinit var vm: GameVM
        val pause = Pause {
            val state = vm.gameState.value
            if (state.stimulusVisible && state.eventNumber > 2) {
                val index = state.eventNumber - 1
                if (events[index] == events[index - 2]) vm.checkMatch()
            }
            currentCoroutineContext().ensureActive()
        }
        vm = GameVM(
            highscore = MutableStateFlow(0),
            generator = SequenceGenerator { _, _, _, _, _ -> NBackGeneration.Ok(events.toIntArray()) },
            pause = pause,
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )

        vm.startGame()

        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertEquals(6, vm.gameState.value.correctPresses)
        assertEquals(0, vm.gameState.value.wrongPresses)
        assertEquals(0, vm.gameState.value.missedMatches)
        assertEquals(6, vm.gameState.value.score)
    }
}
