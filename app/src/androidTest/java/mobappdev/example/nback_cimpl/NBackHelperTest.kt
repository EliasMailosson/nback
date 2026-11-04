package mobappdev.example.nback_cimpl

import org.junit.Assert
import org.junit.Test

class NBackHelperTest {

    private val helper = NBackHelper()

    /** Positions on the 3×3 grid. */
    private val visualPositions = 9

    /** Letters A, F, K, M, R and U. */
    private val audioLetters = 6

    @Test
    fun threeSeedsGiveExactlySixMatches() {
        val seeds = intArrayOf(1, 8, 42)
        val stimulusCounts = intArrayOf(visualPositions, audioLetters)

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
                stimuli = audioLetters,
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
        val first = helper.generateNBackString(20, visualPositions, 2, 6, 42)
        val second = helper.generateNBackString(20, visualPositions, 2, 6, 42)
        if (first !is NBackGeneration.Ok || second !is NBackGeneration.Ok) {
            Assert.fail("Expected two sequences")
            return
        }
        Assert.assertTrue(first.sequence.contentEquals(second.sequence))
    }

    @Test
    fun impossibleRequestReturnsNamedError() {
        assertError(NBackGeneration.INVALID_LENGTH, size = 0, stimuli = visualPositions, nBack = 2, matches = 6)
        assertError(NBackGeneration.INVALID_STIMULI, size = 20, stimuli = 0, nBack = 2, matches = 6)
        assertError(NBackGeneration.INVALID_N, size = 20, stimuli = visualPositions, nBack = 0, matches = 6)
        assertError(NBackGeneration.INVALID_MATCH_COUNT, size = 20, stimuli = visualPositions, nBack = 2, matches = 19)
    }

    private fun assertError(expectedName: String, size: Int, stimuli: Int, nBack: Int, matches: Int) {
        val result = helper.generateNBackString(size, stimuli, nBack, matches, seed = 1)
        if (result !is NBackGeneration.Error) {
            Assert.fail("Expected $expectedName")
            return
        }
        Assert.assertEquals(expectedName, result.name)
    }

    @Test
    fun unreasonableInputReturnsNamedError() {
        assertError(
            NBackGeneration.INVALID_LENGTH,
            size = NBackGeneration.MAX_LENGTH + 1,
            stimuli = visualPositions,
            nBack = 2,
            matches = 6
        )
        assertError(
            NBackGeneration.INVALID_LENGTH,
            size = -1,
            stimuli = visualPositions,
            nBack = 2,
            matches = 6
        )
        assertError(
            NBackGeneration.INVALID_LENGTH,
            size = Int.MIN_VALUE,
            stimuli = visualPositions,
            nBack = 2,
            matches = 6
        )
        assertError(
            NBackGeneration.INVALID_STIMULI,
            size = 20,
            stimuli = 1,
            nBack = 2,
            matches = 6
        )
        assertError(
            NBackGeneration.INVALID_STIMULI,
            size = 20,
            stimuli = NBackGeneration.MAX_STIMULI + 1,
            nBack = 2,
            matches = 6
        )
        assertError(
            NBackGeneration.INVALID_STIMULI,
            size = 20,
            stimuli = -1,
            nBack = 2,
            matches = 0
        )
        assertError(NBackGeneration.INVALID_N, size = 20, stimuli = visualPositions, nBack = -1, matches = 6)
        assertError(NBackGeneration.INVALID_N, size = 20, stimuli = visualPositions, nBack = 20, matches = 6)
        assertError(
            NBackGeneration.INVALID_N,
            size = 20,
            stimuli = visualPositions,
            nBack = Int.MIN_VALUE,
            matches = 0
        )
        assertError(NBackGeneration.INVALID_N, size = 1, stimuli = visualPositions, nBack = 1, matches = 0)
        assertError(
            NBackGeneration.INVALID_MATCH_COUNT,
            size = 20,
            stimuli = visualPositions,
            nBack = 2,
            matches = -1
        )
    }

    @Test
    fun acceptedBoundariesKeepExactMatches() {
        val atLengthLimit = requireSequence(
            size = NBackGeneration.MAX_LENGTH,
            stimuli = visualPositions,
            nBack = 2,
            matches = 6,
            seed = 1
        )
        Assert.assertEquals(NBackGeneration.MAX_LENGTH, atLengthLimit.size)
        assertValuesInRange(atLengthLimit, visualPositions)
        Assert.assertEquals(6, countMatches(atLengthLimit, nBack = 2))

        val oneBack = requireSequence(20, visualPositions, nBack = 1, matches = 6, seed = 3)
        Assert.assertEquals(6, countMatches(oneBack, nBack = 1))

        val longestN = requireSequence(20, audioLetters, nBack = 19, matches = 1, seed = 3)
        Assert.assertEquals(1, countMatches(longestN, nBack = 19))
        Assert.assertEquals(longestN[0], longestN[19])

        val onlyOneStimulus = requireSequence(20, stimuli = 1, nBack = 2, matches = 18, seed = 4)
        Assert.assertEquals(18, countMatches(onlyOneStimulus, nBack = 2))
        for (value in onlyOneStimulus) {
            Assert.assertEquals(1, value)
        }

        val widestStimuli = requireSequence(
            size = 5,
            stimuli = NBackGeneration.MAX_STIMULI,
            nBack = 1,
            matches = 0,
            seed = 1
        )
        assertValuesInRange(widestStimuli, NBackGeneration.MAX_STIMULI)
        Assert.assertEquals(0, countMatches(widestStimuli, nBack = 1))

        val everyComparison = requireSequence(20, visualPositions, nBack = 2, matches = 18, seed = 7)
        Assert.assertEquals(18, countMatches(everyComparison, nBack = 2))
        assertValuesInRange(everyComparison, visualPositions)
    }

    @Test
    fun differentSeedsChangeTheSequence() {
        val first = requireSequence(20, visualPositions, nBack = 2, matches = 6, seed = 1)
        val second = requireSequence(20, visualPositions, nBack = 2, matches = 6, seed = 2)
        Assert.assertFalse(first.contentEquals(second))
    }

    @Test
    fun repeatedCallsKeepTheSameResult() {
        assertError(
            NBackGeneration.INVALID_LENGTH,
            size = NBackGeneration.MAX_LENGTH + 1,
            stimuli = visualPositions,
            nBack = 2,
            matches = 6
        )
        val first = requireSequence(20, audioLetters, nBack = 2, matches = 6, seed = 42)
        assertError(NBackGeneration.INVALID_STIMULI, size = 20, stimuli = 1, nBack = 2, matches = 6)
        val second = requireSequence(20, audioLetters, nBack = 2, matches = 6, seed = 42)
        Assert.assertTrue(first.contentEquals(second))
        Assert.assertEquals(6, countMatches(second, nBack = 2))

        val visual = requireSequence(20, visualPositions, nBack = 2, matches = 6, seed = 8)
        Assert.assertEquals(6, countMatches(visual, nBack = 2))
        assertValuesInRange(visual, visualPositions)
    }

    private fun requireSequence(
        size: Int,
        stimuli: Int,
        nBack: Int,
        matches: Int,
        seed: Int
    ): IntArray {
        val result = helper.generateNBackString(size, stimuli, nBack, matches, seed)
        if (result !is NBackGeneration.Ok) {
            val name = (result as NBackGeneration.Error).name
            Assert.fail("Expected a sequence, got $name")
            return intArrayOf()
        }
        return result.sequence
    }

    private fun assertValuesInRange(sequence: IntArray, stimuli: Int) {
        for (value in sequence) {
            Assert.assertTrue(
                "Value $value is outside 1..$stimuli",
                value in 1..stimuli
            )
        }
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
