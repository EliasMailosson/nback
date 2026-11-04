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
import mobappdev.example.nback_cimpl.ui.viewmodels.FakeVM
import mobappdev.example.nback_cimpl.ui.viewmodels.GameViewModel

/**
 * Start screen. Shows the fixed round and the saved record.
 * Visual mode opens the game. Audio stays closed until the letter round exists.
 */

private const val EVENTS_PER_ROUND = 20
private const val SECONDS_PER_EVENT = 2

@Composable
fun HomeScreen(
    vm: GameViewModel,
    onStartVisual: () -> Unit
) {
    val highscore by vm.highscore.collectAsState()

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
                    text = "$EVENTS_PER_ROUND händelser",
                    style = MaterialTheme.typography.headlineSmall
                )
                Text(
                    text = "$SECONDS_PER_EVENT sekunder per händelse",
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
                        Button(onClick = {}, enabled = false) {
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
                        Text(
                            modifier = Modifier.padding(top = 8.dp),
                            text = "Inte tillgängligt ännu",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun HomeScreenPreview() {
    Surface {
        HomeScreen(vm = FakeVM(), onStartVisual = {})
    }
}
