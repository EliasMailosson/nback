package mobappdev.example.nback_cimpl

import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.ui.viewmodels.GameState
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import org.junit.Assert.assertEquals
import org.junit.Test

class GameStateTest {

    @Test
    fun newStateIsReadyAndHasNoEvent() {
        val state = GameState()

        assertEquals(RoundPhase.Ready, state.phase)
        assertEquals(GameType.Visual, state.gameType)
        assertEquals(-1, state.eventValue)
    }
}
