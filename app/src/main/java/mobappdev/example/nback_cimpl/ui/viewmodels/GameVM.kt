package mobappdev.example.nback_cimpl.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.NativeSequenceGenerator
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.RealPause
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.game.SequenceGenerator
import mobappdev.example.nback_cimpl.game.playFrom

/**
 * This is the GameViewModel.
 *
 * It is good practice to first make an interface, which acts as the blueprint
 * for your implementation. With this interface we can create fake versions
 * of the viewmodel, which we can use to test other parts of our app that depend on the VM.
 *
 * Our viewmodel itself has functions to start a game, to specify a gametype,
 * and to check if we are having a match
 *
 * Date: 25-08-2023
 * Version: Version 1.0
 * Author: Yeetivity
 *
 */


interface GameViewModel {
    val gameState: StateFlow<GameState>
    val highscore: StateFlow<Int>
    val nBack: Int

    fun setGameType(gameType: GameType)
    fun startGame()
    fun abortRound()
    fun enterGameScreen()

    fun checkMatch()
}

class GameVM(
    highscore: Flow<Int>,
    private val generator: SequenceGenerator = NativeSequenceGenerator(),
    private val pause: Pause = RealPause(),
    private val gameScope: CoroutineScope? = null,
) : GameViewModel, ViewModel() {
    private val scope: CoroutineScope = gameScope ?: viewModelScope

    private val _gameState = MutableStateFlow(GameState())
    override val gameState: StateFlow<GameState>
        get() = _gameState.asStateFlow()

    private val _highscore = MutableStateFlow(0)
    override val highscore: StateFlow<Int>
        get() = _highscore

    override val nBack: Int = BasicRound.N_BACK

    private var round = NBackRound(emptyList())
    private var job: Job? = null
    private var roundToken = 0
    private var note: String? = null

    override fun setGameType(gameType: GameType) {
        if (_gameState.value.phase == RoundPhase.Playing) return
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        if (_gameState.value.gameType != GameType.Visual) {
            note = "Ljudläget är inte tillgängligt ännu"
            _gameState.value = _gameState.value.copy(message = note)
            return
        }
        val generated = generator.generate(
            size = BasicRound.EVENT_COUNT,
            stimuli = BasicRound.VISUAL_STIMULI,
            nBack = BasicRound.N_BACK,
            matches = BasicRound.MATCH_COUNT,
            seed = System.currentTimeMillis().toInt()
        )
        val events = when (generated) {
            is NBackGeneration.Ok -> generated.sequence.toList()
            is NBackGeneration.Error -> {
                // A failed call must not run an empty round.
                failToStart()
                return
            }
        }
        begin(events)
    }

    override fun abortRound() {
        if (round.phase != RoundPhase.Playing) {
            job?.cancel()
            job = null
            note = null
            publish()
            return
        }
        // Bump the token first so the cancelled job cannot publish over the abort.
        roundToken += 1
        job?.cancel()
        job = null
        round = round.abort()
        note = null
        publish()
    }

    override fun enterGameScreen() {
        if (round.phase == RoundPhase.Playing) return
        job?.cancel()
        job = null
        round = NBackRound(emptyList())
        note = null
        publish()
    }

    override fun checkMatch() {
        round = round.press()
        publish()
    }

    private fun begin(events: List<Int>) {
        roundToken += 1
        val token = roundToken
        job?.cancel()
        round = NBackRound(events).start()
        note = null
        publish()
        job = scope.launch {
            try {
                val played = playFrom(round, pause, latest = { round }) { updated ->
                    if (token == roundToken) {
                        round = updated
                        publish()
                    }
                }
                if (token == roundToken) {
                    round = played
                    publish()
                }
            } finally {
                if (token == roundToken && round.phase == RoundPhase.Playing) {
                    round = round.abort()
                    publish()
                }
            }
        }
    }

    private fun failToStart() {
        roundToken += 1
        job?.cancel()
        job = null
        round = NBackRound(emptyList())
        note = "Kunde inte starta rundan"
        _gameState.value = _gameState.value.copy(
            phase = RoundPhase.Ready,
            eventValue = -1,
            eventNumber = 0,
            stimulusVisible = false,
            message = note,
        )
    }

    private fun publish() {
        _gameState.value = GameState(
            gameType = _gameState.value.gameType,
            phase = round.phase,
            eventValue = round.shownValue,
            eventNumber = round.eventNumber,
            eventCount = if (round.events.isEmpty()) BasicRound.EVENT_COUNT else round.eventCount,
            stimulusVisible = round.stimulusVisible,
            answered = round.answered,
            correctPresses = round.correctPresses,
            wrongPresses = round.wrongPresses,
            missedMatches = round.missedMatches,
            score = round.score,
            message = note,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(application.userPreferencesRespository.highscore)
            }
        }
    }

    init {
        scope.launch {
            highscore.collect { _highscore.value = it }
        }
    }
}

// Class with the different game types
enum class GameType{
    Audio,
    Visual,
    AudioVisual
}

data class GameState(
    val gameType: GameType = GameType.Visual,
    val phase: RoundPhase = RoundPhase.Ready,
    val eventValue: Int = -1,
    val eventNumber: Int = 0,
    val eventCount: Int = BasicRound.EVENT_COUNT,
    val stimulusVisible: Boolean = false,
    val answered: Boolean = false,
    val correctPresses: Int = 0,
    val wrongPresses: Int = 0,
    val missedMatches: Int = 0,
    val score: Int = 0,
    val message: String? = null,
)

class FakeVM: GameViewModel{
    override val gameState: StateFlow<GameState>
        get() = MutableStateFlow(GameState()).asStateFlow()
    override val highscore: StateFlow<Int>
        get() = MutableStateFlow(42).asStateFlow()
    override val nBack: Int
        get() = 2

    override fun setGameType(gameType: GameType) {
    }

    override fun startGame() {
    }

    override fun abortRound() {
    }

    override fun enterGameScreen() {
    }

    override fun checkMatch() {
    }
}