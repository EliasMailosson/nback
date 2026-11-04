package mobappdev.example.nback_cimpl

import androidx.annotation.Keep

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
 * JNI calls [ok] and [error] by name, and builds [Ok] and [Error].
 * [Keep] leaves those names in place if shrinking is turned on.
 */
@Keep
sealed class NBackGeneration {
    @Keep
    class Ok(val sequence: IntArray) : NBackGeneration()

    @Keep
    class Error(val name: String) : NBackGeneration()

    companion object {
        const val INVALID_LENGTH = "INVALID_LENGTH"
        const val INVALID_STIMULI = "INVALID_STIMULI"
        const val INVALID_N = "INVALID_N"
        const val INVALID_MATCH_COUNT = "INVALID_MATCH_COUNT"
        const val ALLOCATION = "ALLOCATION"

        /** Same limit as NBACK_MAX_SIZE in nback.h. */
        const val MAX_LENGTH = 100

        /** Same limit as NBACK_MAX_STIMULI in nback.h. The C step has 15 bits. */
        const val MAX_STIMULI = 32768

        @Keep
        @JvmStatic
        fun ok(sequence: IntArray): NBackGeneration = Ok(sequence)

        @Keep
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
    ): NBackGeneration?

    fun generateNBackString(
        size: Int,
        stimuli: Int,
        nBack: Int,
        matches: Int,
        seed: Int
    ): NBackGeneration {
        // Null here means the native error name could not be allocated.
        // A pending exception leaves this function before the Elvis operator.
        return createNBackString(size, stimuli, nBack, matches, seed)
            ?: NBackGeneration.Error(NBackGeneration.ALLOCATION)
    }

    companion object {
        init {
            System.loadLibrary("JniBridge") // Load the native C library
        }
    }
}
