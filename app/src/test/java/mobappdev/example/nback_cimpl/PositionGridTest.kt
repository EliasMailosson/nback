package mobappdev.example.nback_cimpl

import mobappdev.example.nback_cimpl.ui.screens.activeCell
import mobappdev.example.nback_cimpl.ui.screens.gridColumn
import mobappdev.example.nback_cimpl.ui.screens.gridRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PositionGridTest {

    @Test
    fun eachValueMarksADifferentCell() {
        val cells = (1..9).map { position ->
            position to (gridRow(position) to gridColumn(position))
        }

        assertEquals(
            listOf(
                1 to (0 to 0),
                2 to (0 to 1),
                3 to (0 to 2),
                4 to (1 to 0),
                5 to (1 to 1),
                6 to (1 to 2),
                7 to (2 to 0),
                8 to (2 to 1),
                9 to (2 to 2),
            ),
            cells
        )
        assertEquals(9, cells.map { it.second }.toSet().size)
    }

    @Test
    fun blankAndOutOfRangeMarkNoCell() {
        assertNull(activeCell(null))
        assertNull(activeCell(-1))
        assertNull(activeCell(0))
        assertNull(activeCell(10))
        assertEquals(5, activeCell(5))
    }
}
