package mobappdev.example.nback_cimpl

/**
 * This class provides the connection to the C-model
 *
 * The class loads the JniBridge when it is initialized.
 * Secondly, it provides the funtion generateNBackString to the rest of the code.
 *
 * Date: 25-08-2023
 * Version: Version 1.1
 * Author: Yeetivity
 *
 */

/**
 * A generated sequence, or one named error from the C generator.
 * The error names match the NBACK_ERROR_* texts in nback.h.
 */
sealed class NBackGeneration {
    class Ok(val sequence: IntArray) : NBackGeneration()
    class Error(val name: String) : NBackGeneration()

    companion object {
        const val INVALID_LENGTH = "INVALID_LENGTH"
        const val INVALID_STIMULI = "INVALID_STIMULI"
        const val INVALID_N = "INVALID_N"
        const val INVALID_MATCH_COUNT = "INVALID_MATCH_COUNT"
        const val ALLOCATION = "ALLOCATION"

        @JvmStatic
        fun ok(sequence: IntArray): NBackGeneration = Ok(sequence)

        @JvmStatic
        fun error(name: String): NBackGeneration = Error(name)
    }
}

class NBackHelper {

    private external fun createNBackString(
        size: Int,
        stimuli: Int,
        nBack: Int,
        matches: Int,
        seed: Int
    ): NBackGeneration

    fun generateNBackString(
        size: Int,
        stimuli: Int,
        nBack: Int,
        matches: Int,
        seed: Int
    ): NBackGeneration {
        return createNBackString(size, stimuli, nBack, matches, seed)
    }

    companion object {
        init {
            System.loadLibrary("JniBridge") // Load the native C library
        }
    }
}
