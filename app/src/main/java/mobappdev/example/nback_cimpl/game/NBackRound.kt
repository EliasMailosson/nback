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

/** Fixed basic-level round. The same numbers are shown on the start screen. */
object BasicRound {
    const val N_BACK = 2
    const val EVENT_COUNT = 20
    const val SECONDS_PER_EVENT = 2
    const val SHOW_MILLIS = 1_800L
    const val BLANK_MILLIS = 200L
    const val MATCH_COUNT = 6
    const val VISUAL_STIMULI = 9
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
) {
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
        )
    }

    fun blank(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        return copy(stimulusVisible = false)
    }

    /** Ends the response window. The next event starts, or the round is finished. */
    fun closeEvent(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        val next = index + 1
        return if (next >= events.size) {
            copy(phase = RoundPhase.Finished, stimulusVisible = false)
        } else {
            copy(index = next, stimulusVisible = true)
        }
    }

    fun abort(): NBackRound {
        if (phase != RoundPhase.Playing) return this
        return copy(phase = RoundPhase.Aborted, stimulusVisible = false)
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
 */
suspend fun playFrom(
    started: NBackRound,
    pause: Pause,
    publish: (NBackRound) -> Unit,
): NBackRound {
    var current = started
    while (current.phase == RoundPhase.Playing && currentCoroutineContext().isActive) {
        pause.wait(BasicRound.SHOW_MILLIS)
        if (!currentCoroutineContext().isActive) break
        current = current.blank()
        publish(current)
        pause.wait(BasicRound.BLANK_MILLIS)
        if (!currentCoroutineContext().isActive) break
        current = current.closeEvent()
        publish(current)
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
