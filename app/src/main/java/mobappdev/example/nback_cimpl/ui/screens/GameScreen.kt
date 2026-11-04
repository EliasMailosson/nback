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
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel
import mobappdev.example.nback_cimpl.ui.viewmodels.RoundPhase

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
            PositionGrid(position = activeCell(state.eventValue))
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
    }
}

@Preview
@Composable
fun GameScreenPreview() {
    Surface {
        GameScreen(vm = FakeVM(), onLeave = {})
    }
}
