package mobappdev.example.nback_cimpl

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun visualModeOpensTheGameAndTheButtonReturnsHome() {
        rule.onNodeWithText("2-back").assertIsDisplayed()
        rule.onNodeWithText("20 händelser").assertIsDisplayed()
        rule.onNodeWithText("2 sekunder per händelse").assertIsDisplayed()
        rule.onNodeWithText("Rekord:", substring = true).assertIsDisplayed()
        rule.onNodeWithText("Ljud").assertIsNotEnabled()
        rule.onNodeWithText("Inte tillgängligt ännu").assertIsDisplayed()

        rule.onNodeWithText("Visuellt").performClick()
        rule.onNodeWithText("Redo").assertIsDisplayed()
        rule.onNodeWithText("Visuellt läge").assertIsDisplayed()

        rule.onNodeWithText("Tillbaka").performClick()
        rule.onNodeWithText("2-back").assertIsDisplayed()
    }

    @Test
    fun systemBackLeavesTheGameScreen() {
        rule.onNodeWithText("Visuellt").performClick()
        rule.onNodeWithText("Redo").assertIsDisplayed()

        Espresso.pressBack()

        rule.onNodeWithText("2-back").assertIsDisplayed()
    }
}
