package mobappdev.example.nback_cimpl

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.game.playFrom
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoundTimingTest {

    @Test
    fun eachEventIsLitThenBlankAndTheLastWindowCloses() = runBlocking {
        val events = (1..BasicRound.EVENT_COUNT).toList()
        val pause = RecordingPause()
        val seen = mutableListOf<NBackRound>()
        val started = NBackRound(events).start()
        seen += started

        var latestRound = started
        val finished = playFrom(started, pause, latest = { latestRound }) {
            latestRound = it
            seen += it
        }
        seen += finished

        assertEquals(RoundPhase.Finished, seen.last().phase)
        assertEquals(40, pause.waits.size)
        for (index in pause.waits.indices) {
            val expected = if (index % 2 == 0) BasicRound.SHOW_MILLIS else BasicRound.BLANK_MILLIS
            assertEquals(expected, pause.waits[index])
        }
        assertEquals(BasicRound.SHOW_MILLIS, pause.waits[pause.waits.lastIndex - 1])
        assertEquals(BasicRound.BLANK_MILLIS, pause.waits.last())

        val visible = seen.filter { it.stimulusVisible }
        assertEquals(BasicRound.EVENT_COUNT, visible.size)
        visible.forEachIndexed { index, round ->
            assertEquals(index + 1, round.eventNumber)
            assertEquals(events[index], round.shownValue)
        }
        val blanks = seen.filter { it.phase == RoundPhase.Playing && !it.stimulusVisible }
        assertEquals(BasicRound.EVENT_COUNT, blanks.size)
        blanks.forEachIndexed { index, round ->
            assertEquals(index + 1, round.eventNumber)
            assertEquals(-1, round.shownValue)
        }
    }

    @Test
    fun aFailedGeneratorDoesNotStartAnEmptyRound() {
        val pause = RecordingPause()
        val vm = gameVm(
            pause = pause,
            sequences = listOf(NBackGeneration.Error(NBackGeneration.ALLOCATION))
        )

        vm.startGame()

        assertEquals(RoundPhase.Ready, vm.gameState.value.phase)
        assertEquals(-1, vm.gameState.value.eventValue)
        assertEquals("Kunde inte starta rundan", vm.gameState.value.message)
        assertTrue(pause.waits.isEmpty())
    }

    @Test
    fun abortStopsTheRoundAndANewStartReplacesIt() {
        val first = List(BasicRound.EVENT_COUNT) { 1 }
        val second = List(BasicRound.EVENT_COUNT) { 9 }
        val pause = HoldingPause()
        val vm = gameVm(
            pause = pause,
            sequences = listOf(
                NBackGeneration.Ok(first.toIntArray()),
                NBackGeneration.Ok(second.toIntArray())
            )
        )

        vm.startGame()
        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertEquals(1, vm.gameState.value.eventValue)
        assertEquals(1, pause.calls)

        vm.abortRound()
        assertEquals(RoundPhase.Aborted, vm.gameState.value.phase)
        assertEquals(-1, vm.gameState.value.eventValue)
        assertEquals(1, pause.calls)

        vm.startGame()
        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertEquals(9, vm.gameState.value.eventValue)
        assertEquals(1, vm.gameState.value.eventNumber)
        assertEquals(2, pause.calls)
    }

    @Test
    fun startingAgainCancelsThePreviousRound() {
        val first = List(BasicRound.EVENT_COUNT) { 3 }
        val second = List(BasicRound.EVENT_COUNT) { 4 }
        val pause = HoldingPause()
        val vm = gameVm(
            pause = pause,
            sequences = listOf(
                NBackGeneration.Ok(first.toIntArray()),
                NBackGeneration.Ok(second.toIntArray())
            )
        )

        vm.startGame()
        vm.startGame()

        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertEquals(4, vm.gameState.value.eventValue)
        assertEquals(2, pause.calls)
    }

    @Test
    fun fullRoundFinishesWithoutWaitingForRealTime() {
        val pause = RecordingPause()
        val vm = gameVm(
            pause = pause,
            sequences = listOf(NBackGeneration.Ok(IntArray(BasicRound.EVENT_COUNT) { it + 1 }))
        )
        val started = System.nanoTime()

        vm.startGame()

        assertEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertEquals(BasicRound.EVENT_COUNT, vm.gameState.value.eventNumber)
        assertTrue(System.nanoTime() - started < 1_000_000_000L)

        vm.enterGameScreen()
        assertEquals(RoundPhase.Ready, vm.gameState.value.phase)
        assertEquals(0, vm.gameState.value.eventNumber)
    }

    @Test
    fun enterGameScreenDoesNotResetARoundThatIsStillPlaying() {
        val pause = HoldingPause()
        val vm = gameVm(
            pause = pause,
            sequences = listOf(NBackGeneration.Ok(IntArray(BasicRound.EVENT_COUNT) { 1 }))
        )

        vm.startGame()
        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        vm.enterGameScreen()
        assertEquals(RoundPhase.Playing, vm.gameState.value.phase)
        assertEquals(1, vm.gameState.value.eventNumber)
        vm.abortRound()
    }

    private fun gameVm(pause: Pause, sequences: List<NBackGeneration>): GameVM {
        var index = 0
        val generator = SequenceGenerator { _, _, _, _, _ ->
            sequences[index++]
        }
        return GameVM(
            highScores = MemoryHighScores(),
            generator = generator,
            pause = pause,
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
    }
}

private class RecordingPause : Pause {
    val waits = mutableListOf<Long>()

    override suspend fun wait(millis: Long) {
        waits += millis
        currentCoroutineContext().ensureActive()
    }
}

private class HoldingPause : Pause {
    var calls = 0

    override suspend fun wait(millis: Long) {
        calls += 1
        suspendCancellableCoroutine<Unit> { }
    }
}
