package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.R
import mobappdev.example.nback_cimpl.game.BasicRound
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Start screen. Shows the fixed round and the saved record.
 * Visual mode and audio mode both open the game. A missing voice is reported when the audio round starts.
 */

@Composable
fun HomeScreen(
    vm: GameViewModel,
    onStartVisual: () -> Unit,
    onStartAudio: () -> Unit
) {
    val highscore by vm.highscore.collectAsState()
    val state by vm.gameState.collectAsState()

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Rekord: $highscore",
                style = MaterialTheme.typography.headlineLarge
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${vm.nBack}-back",
                    style = MaterialTheme.typography.headlineMedium
                )
                Text(
                    text = "${BasicRound.EVENT_COUNT} händelser",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "${BasicRound.SECONDS_PER_EVENT} sekunder per händelse",
                    style = MaterialTheme.typography.headlineSmall
                )
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    modifier = Modifier.padding(bottom = 16.dp),
                    text = "Välj läge",
                    style = MaterialTheme.typography.titleLarge
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = onStartVisual) {
                            Icon(
                                painter = painterResource(id = R.drawable.visual),
                                contentDescription = null,
                                modifier = Modifier.height(48.dp)
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Visuellt"
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(onClick = onStartAudio) {
                            Icon(
                                painter = painterResource(id = R.drawable.sound_on),
                                contentDescription = null,
                                modifier = Modifier.height(48.dp)
                            )
                            Text(
                                modifier = Modifier.padding(start = 8.dp),
                                text = "Ljud"
                            )
                        }
                    }
                }
            }
            if (state.message != null) {
                Text(
                    text = state.message ?: "",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    Surface {
        HomeScreen(vm = FakeVM(), onStartVisual = {}, onStartAudio = {})
    }
}
