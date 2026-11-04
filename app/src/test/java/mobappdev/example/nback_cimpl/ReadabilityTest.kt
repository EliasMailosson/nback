package mobappdev.example.nback_cimpl

import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.ui.screens.matchHint
import org.junit.Assert.assertEquals
import org.junit.Test

class ReadabilityTest {

    @Test
    fun theMatchHintExplainsTheButtonWithoutColourAlone() {
        assertEquals("", matchHint(RoundPhase.Ready, 1, 2, answered = false))
        assertEquals("Uppvärmning", matchHint(RoundPhase.Playing, 1, 2, answered = false))
        assertEquals("Uppvärmning", matchHint(RoundPhase.Playing, 2, 2, answered = false))
        assertEquals("Tryck om det är en match", matchHint(RoundPhase.Playing, 3, 2, answered = false))
        assertEquals("Svar registrerat", matchHint(RoundPhase.Playing, 3, 2, answered = true))
    }
}
