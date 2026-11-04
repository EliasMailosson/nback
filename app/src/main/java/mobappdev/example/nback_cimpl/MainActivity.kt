package mobappdev.example.nback_cimpl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import mobappdev.example.nback_cimpl.ui.screens.GameScreen
import mobappdev.example.nback_cimpl.ui.screens.HomeScreen
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import mobappdev.example.nback_cimpl.ui.viewmodels.GameType
import mobappdev.example.nback_cimpl.ui.viewmodels.GameVM

/**
 * One [GameVM] is created above the NavHost so both screens share it.
 * This activity decides which screen is shown. The ViewModel does not.
 */

private const val HOME_ROUTE = "home"
private const val GAME_ROUTE = "game"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NBack_CImplTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val gameViewModel: GameVM = viewModel(factory = GameVM.Factory)
                    val navController = rememberNavController()

                    NavHost(
                        navController = navController,
                        startDestination = HOME_ROUTE
                    ) {
                        composable(HOME_ROUTE) {
                            HomeScreen(
                                vm = gameViewModel,
                                onStartVisual = {
                                    gameViewModel.setGameType(GameType.Visual)
                                    navController.navigate(GAME_ROUTE) {
                                        launchSingleTop = true
                                    }
                                }
                            )
                        }
                        composable(GAME_ROUTE) {
                            GameScreen(
                                vm = gameViewModel,
                                onLeave = { navController.popBackStack(HOME_ROUTE, inclusive = false) }
                            )
                        }
                    }
                }
            }
        }
    }
}
