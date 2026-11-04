package mobappdev.example.nback_cimpl

import androidx.compose.foundation.layout.width
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import mobappdev.example.nback_cimpl.ui.screens.PositionGrid
import mobappdev.example.nback_cimpl.ui.theme.NBack_CImplTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PositionGridUiTest {

    @get:Rule
    val rule = createComposeRule()

    @Test
    fun eachPositionMarksExactlyOneCell() {
        val position = mutableStateOf(1)
        rule.setContent {
            NBack_CImplTheme {
                PositionGrid(position.value)
            }
        }
        for (value in 1..9) {
            position.value = value
            rule.waitForIdle()
            rule.onNodeWithContentDescription("Markerad ruta $value").assertIsDisplayed()
            rule.onAllNodesWithContentDescription("Markerad", substring = true)
                .assertCountEquals(1)
        }
    }

    @Test
    fun blankAndOutOfRangeMarkNothing() {
        val position = mutableStateOf<Int?>(null)
        rule.setContent {
            NBack_CImplTheme {
                PositionGrid(position.value)
            }
        }
        for (value in listOf(null, -1, 0, 10)) {
            position.value = value
            rule.waitForIdle()
            rule.onAllNodesWithContentDescription("Markerad", substring = true)
                .assertCountEquals(0)
        }
    }

    @Test
    fun gridIsSquare() {
        rule.setContent {
            NBack_CImplTheme {
                PositionGrid(
                    position = 1,
                    modifier = Modifier
                        .width(180.dp)
                        .testTag("position-grid")
                )
            }
        }
        val bounds = rule.onNodeWithTag("position-grid").getUnclippedBoundsInRoot()
        assertEquals(bounds.right - bounds.left, bounds.bottom - bounds.top)
    }
}
