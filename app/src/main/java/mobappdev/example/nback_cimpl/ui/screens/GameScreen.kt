package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import mobappdev.example.nback_cimpl.game.RoundPhase
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Game screen. It shows the shared round state and can be left.
 * Opening this screen does not start a timer or a round.
 */
@Composable
fun GameScreen(
    vm: GameViewModel,
    onLeave: () -> Unit
) {
    val state by vm.gameState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    // The ViewModel survives rotation, so this screen stops the round itself.
    // ON_STOP is the app going to the background. onDispose is leaving or recreation.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                vm.abortRound()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.abortRound()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Button(onClick = onLeave) {
                Text("Tillbaka")
            }
            Text(
                text = modeLabel(state.gameType),
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = phaseLabel(state.phase),
                style = MaterialTheme.typography.headlineLarge
            )
            if (state.eventNumber > 0) {
                Text(
                    text = "Händelse ${state.eventNumber} av ${state.eventCount}",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "Rätt: ${state.correctPresses}",
                style = MaterialTheme.typography.titleLarge
            )
            if (state.phase == RoundPhase.Finished) {
                Text(text = "Fel: ${state.wrongPresses}", style = MaterialTheme.typography.titleLarge)
                Text(text = "Missade: ${state.missedMatches}", style = MaterialTheme.typography.titleLarge)
                Text(text = "Poäng: ${state.score}", style = MaterialTheme.typography.headlineMedium)
            }
            if (state.phase == RoundPhase.Aborted) {
                Text(text = "Inget resultat", style = MaterialTheme.typography.titleLarge)
            }
            val canPress = state.phase == RoundPhase.Playing &&
                !state.answered &&
                state.eventNumber > vm.nBack
            Button(onClick = vm::checkMatch, enabled = canPress) {
                Text("Match")
            }
            if (state.message != null) {
                Text(text = state.message ?: "", style = MaterialTheme.typography.bodyLarge)
            }
            val shown = if (state.stimulusVisible) state.eventValue else null
            PositionGrid(position = activeCell(shown))
            if (state.phase != RoundPhase.Playing) {
                Button(onClick = vm::startGame) {
                    Text(if (state.phase == RoundPhase.Finished) "Ny runda" else "Starta runda")
                }
            }
        }
    }
}

private fun modeLabel(gameType: GameType): String {
    return when (gameType) {
        GameType.Visual -> "Visuellt läge"
        GameType.Audio -> "Ljudläge"
        GameType.AudioVisual -> "Kombinerat läge"
    }
}

private fun phaseLabel(phase: RoundPhase): String {
    return when (phase) {
        RoundPhase.Ready -> "Redo"
        RoundPhase.Playing -> "Pågår"
        RoundPhase.Finished -> "Avslutad"
        RoundPhase.Aborted -> "Avbruten"
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    Surface {
        GameScreen(vm = FakeVM(), onLeave = {})
    }
}
