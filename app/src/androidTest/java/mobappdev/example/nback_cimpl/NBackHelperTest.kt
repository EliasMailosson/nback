package mobappdev.example.nback_cimpl

import org.junit.Assert
import org.junit.Test

class NBackHelperTest {

    private val helper = NBackHelper()

    @Test
    fun threeSeedsGiveExactlySixMatches() {
        val seeds = intArrayOf(1, 8, 42)
        val stimulusCounts = intArrayOf(9, 6)

        for (stimuli in stimulusCounts) {
            for (seed in seeds) {
                val result = helper.generateNBackString(
                    size = 20,
                    stimuli = stimuli,
                    nBack = 2,
                    matches = 6,
                    seed = seed
                )
                if (result !is NBackGeneration.Ok) {
                    Assert.fail("Seed $seed and $stimuli stimuli returned an error")
                    return
                }

                val sequence = result.sequence
                Assert.assertEquals(20, sequence.size)
                for (value in sequence) {
                    Assert.assertTrue(
                        "Value $value is outside 1..$stimuli",
                        value in 1..stimuli
                    )
                }
                Assert.assertEquals(6, countMatches(sequence, nBack = 2))
            }
        }
    }

    @Test
    fun sixStimuliCanStartWithTwoEqualEvents() {
        var foundEqualStart = false
        for (seed in 1..50) {
            val result = helper.generateNBackString(
                size = 20,
                stimuli = 6,
                nBack = 2,
                matches = 0,
                seed = seed
            )
            if (result !is NBackGeneration.Ok) {
                Assert.fail("Seed $seed returned an error")
                return
            }
            if (result.sequence[0] == result.sequence[1]) {
                foundEqualStart = true
            }
        }
        Assert.assertTrue("Events 1 and 2 were never equal for 6 stimuli", foundEqualStart)
    }

    @Test
    fun sameSeedGivesTheSameSequence() {
        val first = helper.generateNBackString(20, 9, 2, 6, 42)
        val second = helper.generateNBackString(20, 9, 2, 6, 42)
        if (first !is NBackGeneration.Ok || second !is NBackGeneration.Ok) {
            Assert.fail("Expected two sequences")
            return
        }
        Assert.assertTrue(first.sequence.contentEquals(second.sequence))
    }

    @Test
    fun impossibleRequestReturnsNamedError() {
        assertError(NBackGeneration.INVALID_LENGTH, size = 0, stimuli = 9, nBack = 2, matches = 6)
        assertError(NBackGeneration.INVALID_STIMULI, size = 20, stimuli = 0, nBack = 2, matches = 6)
        assertError(NBackGeneration.INVALID_N, size = 20, stimuli = 9, nBack = 0, matches = 6)
        assertError(NBackGeneration.INVALID_MATCH_COUNT, size = 20, stimuli = 9, nBack = 2, matches = 19)
    }

    private fun assertError(expectedName: String, size: Int, stimuli: Int, nBack: Int, matches: Int) {
        val result = helper.generateNBackString(size, stimuli, nBack, matches, seed = 1)
        if (result !is NBackGeneration.Error) {
            Assert.fail("Expected $expectedName")
            return
        }
        Assert.assertEquals(expectedName, result.name)
    }

    private fun countMatches(sequence: IntArray, nBack: Int): Int {
        var matches = 0
        for (i in nBack until sequence.size) {
            if (sequence[i] == sequence[i - nBack]) {
                matches++
            }
        }
        return matches
    }
}
