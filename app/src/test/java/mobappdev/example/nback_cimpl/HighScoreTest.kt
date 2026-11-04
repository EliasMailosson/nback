package mobappdev.example.nback_cimpl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.audio.SpeechFactory
import mobappdev.example.nback_cimpl.data.HighScoreStore
import mobappdev.example.nback_cimpl.data.SaveResult
import mobappdev.example.nback_cimpl.data.UserPreferencesRepository
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class HighScoreTest {

    @Test
    fun aBetterScoreReplacesTheRecordAndAWorseOneDoesNot() = runBlocking<Unit> {
        val file = File.createTempFile("highscore", ".preferences_pb")
        file.delete()
        try {
            val dataStore = PreferenceDataStoreFactory.create(scope = this) { file }
            val repository = UserPreferencesRepository(dataStore)

            assertEquals(SaveResult.Saved, repository.saveIfHigher(4))
            assertEquals(SaveResult.Unchanged, repository.saveIfHigher(4))
            assertEquals(SaveResult.Unchanged, repository.saveIfHigher(1))
            assertEquals(SaveResult.Saved, repository.saveIfHigher(6))
            assertEquals(6, repository.highscore.first())
        } finally {
            file.delete()
        }
    }

    @Test
    fun aFinishedRoundRaisesTheRecordAndAWeakerRoundDoesNot() {
        val store = MemoryHighScores()
        var press = true
        lateinit var vm: GameVM
        val pause = Pause {
            if (press) vm.checkMatch()
            currentCoroutineContext().ensureActive()
        }
        vm = viewModel(store, pause)

        vm.startGame()
        assertEquals(18, vm.gameState.value.score)
        assertEquals(SaveResult.Saved, store.results.last())
        assertEquals(18, vm.highscore.value)
        assertEquals("Nytt rekord", vm.gameState.value.message)

        press = false
        vm.startGame()
        assertEquals(0, vm.gameState.value.score)
        assertEquals(SaveResult.Unchanged, store.results.last())
        assertEquals(18, vm.highscore.value)
        assertEquals(null, vm.gameState.value.message)
    }

    @Test
    fun anAbortedRoundIsNotStored() {
        val store = MemoryHighScores(start = 4)
        val vm = viewModel(store, BlockedPause())

        vm.startGame()
        vm.abortRound()

        assertEquals(RoundPhase.Aborted, vm.gameState.value.phase)
        assertNotEquals(RoundPhase.Finished, vm.gameState.value.phase)
        assertTrue(store.results.isEmpty())
        assertEquals(4, vm.highscore.value)
    }

    @Test
    fun aFailedSaveIsNotShownAsSuccess() {
        val store = MemoryHighScores()
        store.fail = true
        lateinit var vm: GameVM
        val pause = Pause {
            vm.checkMatch()
            currentCoroutineContext().ensureActive()
        }
        vm = viewModel(store, pause)

        vm.startGame()

        assertEquals(18, vm.gameState.value.score)
        assertEquals("Kunde inte spara rekordet", vm.gameState.value.message)
        assertEquals(0, vm.highscore.value)
    }

    @Test
    fun aSaveFinishesAfterThePlayerLeaves() = runBlocking {
        val store = GatedHighScores()
        lateinit var vm: GameVM
        val pause = Pause {
            vm.checkMatch()
            currentCoroutineContext().ensureActive()
        }
        vm = viewModel(store, pause)

        vm.startGame()
        vm.abortRound()
        store.release.complete(Unit)

        assertEquals(18, vm.highscore.value)
        assertEquals("Nytt rekord", vm.gameState.value.message)
    }

    private fun viewModel(store: HighScoreStore, pause: Pause): GameVM {
        val generator = SequenceGenerator { _, _, _, _, _ ->
            NBackGeneration.Ok(IntArray(20) { 1 })
        }
        return GameVM(
            highScores = store,
            generator = generator,
            pause = pause,
            speechFactory = SpeechFactory { SilentSpeech },
            gameScope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        )
    }
}

internal class MemoryHighScores(start: Int = 0) : HighScoreStore {
    private val values = MutableStateFlow(start)
    override val highscore: Flow<Int> = values
    val results = mutableListOf<SaveResult>()
    var fail = false

    override suspend fun saveIfHigher(score: Int): SaveResult {
        val result = when {
            fail -> SaveResult.Failed
            score <= values.value -> SaveResult.Unchanged
            else -> {
                values.value = score
                SaveResult.Saved
            }
        }
        results += result
        return result
    }
}

private class GatedHighScores : HighScoreStore {
    val release = CompletableDeferred<Unit>()
    private val values = MutableStateFlow(0)
    override val highscore: Flow<Int> = values

    override suspend fun saveIfHigher(score: Int): SaveResult {
        release.await()
        if (score <= values.value) return SaveResult.Unchanged
        values.value = score
        return SaveResult.Saved
    }
}

private class BlockedPause : Pause {
    override suspend fun wait(millis: Long) {
        kotlinx.coroutines.suspendCancellableCoroutine<Unit> { }
    }
}
