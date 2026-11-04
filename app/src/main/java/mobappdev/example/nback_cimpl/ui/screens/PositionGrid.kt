package mobappdev.example.nback_cimpl.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Visual positions from the C generator are 1..9, row by row from the top left:
 * 1 2 3
 * 4 5 6
 * 7 8 9
 * A missing or out-of-range value leaves every cell off.
 */
fun activeCell(position: Int?): Int? = position?.takeIf { it in 1..9 }

fun gridRow(position: Int): Int = (position - 1) / 3

fun gridColumn(position: Int): Int = (position - 1) % 3

@Composable
fun PositionGrid(
    position: Int?,
    modifier: Modifier = Modifier
) {
    val marked = activeCell(position)
    val activeRow = marked?.let { gridRow(it) }
    val activeColumn = marked?.let { gridColumn(it) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        for (row in 0..2) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (column in 0..2) {
                    val cell = row * 3 + column + 1
                    val active = row == activeRow && column == activeColumn
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                            .semantics {
                                contentDescription = if (active) {
                                    "Markerad ruta $cell"
                                } else {
                                    "Ruta $cell"
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (active) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize(0.4f)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onPrimary)
                            )
                        }
                    }
                }
            }
        }
    }
}
