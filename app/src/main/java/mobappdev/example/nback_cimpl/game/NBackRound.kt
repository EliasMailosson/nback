package mobappdev.example.nback_cimpl.game

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.NBackHelper

enum class RoundPhase {
    Ready,
    Playing,
    Finished,
    Aborted,
}

enum class PressFeedback {
    None,
    Wrong,
}

/** Fixed basic-level round. The same numbers are shown on the start screen. */
object BasicRound {
    const val N_BACK = 2
    const val EVENT_COUNT = 20
    const val SECONDS_PER_EVENT = 2
    const val SHOW_MILLIS = 1_800L
    const val BLANK_MILLIS = 200L
    const val MATCH_COUNT = 6
    const val VISUAL_STIMULI = 9
    const val AUDIO_STIMULI = 6
}

/** Generator values 1..6 are these letters, in order. */
object AudioLetters {
    val letters = listOf("A", "F", "K", "M", "R", "U")

    fun letter(value: Int): String? = letters.getOrNull(value - 1)
}

/**
 * One round, without Android.
 * [events] uses the same values as the C generator: 1..9 for positions.
 * The cell is lit for 1 800 ms and blank for 200 ms, and both parts are the same event.
 */
data class NBackRound(
    val events: List<Int>,
    val nBack: Int = BasicRound.N_BACK,
    val index: Int = 0,
    val phase: RoundPhase = RoundPhase.Ready,
    val stimulusVisible: Boolean = false,
    val answered: Boolean = false,
    val correctPresses: Int = 0,
    val wrongPresses: Int = 0,
    val missedMatches: Int = 0,
    val feedback: PressFeedback = PressFeedback.None,
) {
    val score: Int
        get() = (correctPresses - wrongPresses).coerceAtLeast(0)
    val eventCount: Int get() = events.size

    /** 1-based number while a round has started. 0 before the first event. */
    val eventNumber: Int
        get() = if (phase == RoundPhase.Ready || events.isEmpty()) 0 else index + 1

    /** Position to draw, or -1 when the cell is blank or the round is not running. */
    val shownValue: Int
        get() = if (phase == RoundPhase.Playing && stimulusVisible && index in events.indices) {
            events[index]
        } else {
            -1
        }

    fun start(): NBackRound {
        return copy(
            index = 0,
            phase = RoundPhase.Playing,
            stimulusVisible = true,
            answered = false,
            correctPresses = 0,
            wrongPresses = 0,
            missedMatches = 0,
            feedback = PressFeedback.None,
        )
    }

    /** Same value as the event n steps back. Warmup events are never matches. */
    fun isMatch(): Boolean {
        if (index < nBack || index !in events.indices) return false
        return events[index] == events[index - nBack]
    }

    /**
     * One press for this event, including its blank part.
     * Presses outside a running event, during warmup, or after the first press do nothing.
     */
    fun press(): NBackRound {
        if (phase != RoundPhase.Playing || index < nBack || answered) return this
        return if (isMatch()) {
            copy(answered = true, correctPresses = correctPresses + 1, feedback = PressFeedback.None)
        } else {
            copy(answered = true, wrongPresses = wrongPresses + 1, feedback = PressFeedback.Wrong)
        }
    }

    fun blank(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        return copy(stimulusVisible = false)
    }

    /** Ends the response window. The next event starts, or the round is finished. */
    fun closeEvent(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        val missed = missedMatches + if (isMatch() && !answered) 1 else 0
        val next = index + 1
        return if (next >= events.size) {
            copy(
                phase = RoundPhase.Finished,
                stimulusVisible = false,
                missedMatches = missed,
                feedback = PressFeedback.None,
            )
        } else {
            copy(
                index = next,
                stimulusVisible = true,
                answered = false,
                missedMatches = missed,
                feedback = PressFeedback.None,
            )
        }
    }

    fun abort(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        return copy(phase = RoundPhase.Aborted, stimulusVisible = false, feedback = PressFeedback.None)
    }
}

fun interface Pause {
    suspend fun wait(millis: Long)
}

class RealPause : Pause {
    override suspend fun wait(millis: Long) {
        delay(millis)
    }
}

/**
 * Plays a round that is already [NBackRound.start]ed.
 * The caller publishes the first lit event before this runs.
 * [latest] is read after each wait so a press made during the wait is kept.
 */
suspend fun playFrom(
    started: NBackRound,
    pause: Pause,
    latest: () -> NBackRound,
    publish: (NBackRound) -> Unit,
    onEventStart: (NBackRound) -> Unit = {},
): NBackRound {
    onEventStart(started)
    var current = started
    while (current.phase == RoundPhase.Playing && currentCoroutineContext().isActive) {
        pause.wait(BasicRound.SHOW_MILLIS)
        if (!currentCoroutineContext().isActive) break
        current = latest().blank()
        publish(current)
        pause.wait(BasicRound.BLANK_MILLIS)
        if (!currentCoroutineContext().isActive) break
        current = latest().closeEvent()
        publish(current)
        if (current.phase == RoundPhase.Playing) {
            onEventStart(current)
        }
    }
    return current
}

fun interface SequenceGenerator {
    fun generate(size: Int, stimuli: Int, nBack: Int, matches: Int, seed: Int): NBackGeneration
}

class NativeSequenceGenerator : SequenceGenerator {
    private val helper = NBackHelper()

    override fun generate(
        size: Int,
        stimuli: Int,
        nBack: Int,
        matches: Int,
        seed: Int
    ): NBackGeneration {
        return helper.generateNBackString(size, stimuli, nBack, matches, seed)
    }
}
