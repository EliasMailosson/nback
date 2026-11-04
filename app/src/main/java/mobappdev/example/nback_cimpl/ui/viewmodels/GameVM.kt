package mobappdev.example.nback_cimpl.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import mobappdev.example.nback_cimpl.GameApplication
import mobappdev.example.nback_cimpl.audio.AndroidLetterSpeech
import mobappdev.example.nback_cimpl.audio.LetterSpeech
import mobappdev.example.nback_cimpl.audio.SpeechFactory
import mobappdev.example.nback_cimpl.audio.SpeechReady
import mobappdev.example.nback_cimpl.NBackGeneration
import mobappdev.example.nback_cimpl.data.HighScoreStore
import mobappdev.example.nback_cimpl.data.SaveResult
import mobappdev.example.nback_cimpl.game.AudioLetters
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.game.NBackRound
import mobappdev.example.nback_cimpl.game.NativeSequenceGenerator
import mobappdev.example.nback_cimpl.game.Pause
import mobappdev.example.nback_cimpl.game.PressFeedback
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
    private val highScores: HighScoreStore,
    private val generator: SequenceGenerator = NativeSequenceGenerator(),
    private val pause: Pause = RealPause(),
    private val speechFactory: SpeechFactory,
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
    private var speech: LetterSpeech? = null
    private var job: Job? = null
    private var roundToken = 0
    private var note: String? = null

    override fun setGameType(gameType: GameType) {
        if (_gameState.value.phase == RoundPhase.Playing) return
        _gameState.value = _gameState.value.copy(gameType = gameType)
    }

    override fun startGame() {
        when (_gameState.value.gameType) {
            GameType.Visual -> {
                job?.cancel()
                job = scope.launch {
                    runRound(BasicRound.VISUAL_STIMULI, speakLetters = false)
                }
            }
            GameType.Audio -> {
                job?.cancel()
                job = scope.launch {
                    if (ensureSpeech() != SpeechReady.Ready) {
                        if (isActive) showMissingVoice()
                        return@launch
                    }
                    if (!isActive) return@launch
                    runRound(BasicRound.AUDIO_STIMULI, speakLetters = true)
                }
            }
            GameType.AudioVisual -> Unit
        }
    }

    private suspend fun runRound(stimuli: Int, speakLetters: Boolean) {
        val generated = generator.generate(
            size = BasicRound.EVENT_COUNT,
            stimuli = stimuli,
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
        playRound(events, speakLetters)
    }

    override fun abortRound() {
        speech?.stop()
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

    private suspend fun ensureSpeech(): SpeechReady {
        val speaker = speech ?: speechFactory.create().also { speech = it }
        val result = speaker.prepare()
        if (result == SpeechReady.Missing) {
            speaker.shutdown()
            if (speech === speaker) speech = null
        }
        return result
    }

    private fun showMissingVoice() {
        note = "Ingen röst är tillgänglig. Välj visuellt läge."
        _gameState.value = _gameState.value.copy(message = note)
    }

    private fun speakLetter(current: NBackRound) {
        val letter = AudioLetters.letter(current.shownValue) ?: return
        speech?.speak(letter)
    }

    override fun checkMatch() {
        round = round.press()
        publish()
    }

    private suspend fun playRound(events: List<Int>, speakLetters: Boolean) {
        roundToken += 1
        val token = roundToken
        round = NBackRound(events).start()
        note = null
        publish()
        try {
            val played = playFrom(
                    round,
                    pause,
                    latest = { round },
                    publish = { updated ->
                        if (token == roundToken) {
                            round = updated
                            publish()
                        }
                    },
                    onEventStart = { current ->
                        if (speakLetters) speakLetter(current)
                    }
                )
                if (token == roundToken) {
                    round = played
                    if (played.phase == RoundPhase.Finished) {
                        if (speakLetters) speech?.stop()
                        val result = withContext(NonCancellable) {
                            highScores.saveIfHigher(played.score)
                        }
                        if (token == roundToken) {
                            note = when (result) {
                                SaveResult.Saved -> "Nytt rekord"
                                SaveResult.Unchanged -> null
                                SaveResult.Failed -> "Kunde inte spara rekordet"
                            }
                        }
                    }
                    if (token == roundToken) publish()
                }
            } finally {
                if (token == roundToken && round.phase == RoundPhase.Playing) {
                    speech?.stop()
                    round = round.abort()
                    publish()
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
            feedback = round.feedback,
            message = note,
        )
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = (this[APPLICATION_KEY] as GameApplication)
                GameVM(
                    highScores = application.userPreferencesRespository,
                    speechFactory = SpeechFactory { AndroidLetterSpeech(application) }
                )
            }
        }
    }

    init {
        scope.launch {
            highScores.highscore.collect { _highscore.value = it }
        }
    }

    override fun onCleared() {
        job?.cancel()
        speech?.shutdown()
        speech = null
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
    val feedback: PressFeedback = PressFeedback.None,
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