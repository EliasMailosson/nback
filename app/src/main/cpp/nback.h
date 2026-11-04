#ifndef nback_h
#define nback_h

/* Names are part of the JNI contract and must match NBackGeneration in Kotlin. */
#define NBACK_ERROR_LENGTH "INVALID_LENGTH"
#define NBACK_ERROR_STIMULI "INVALID_STIMULI"
#define NBACK_ERROR_N "INVALID_N"
#define NBACK_ERROR_MATCHES "INVALID_MATCH_COUNT"
#define NBACK_ERROR_ALLOCATION "ALLOCATION"

/* The sequence lives in a fixed array. A longer request is INVALID_LENGTH. */
#define NBACK_MAX_SIZE 100

/* The random step keeps 15 bits, so a larger count cannot form every value. */
#define NBACK_MAX_STIMULI 32768

typedef struct nback_type *Nback;

/*
 * Builds one sequence.
 *
 * size     usable length is 2..NBACK_MAX_SIZE. Length 1 always
 *          returns INVALID_N, because n is at least 1 and must be below size.
 * stimuli  number of different values, 1..NBACK_MAX_STIMULI
 * n        look-back distance, 1..size-1
 * matches  exact number of events that equal the event n steps earlier
 * seed     same seed gives the same sequence
 *
 * The first n events are warmup and are never matches.
 * Match positions are chosen among the later events, then the sequence is
 * filled from the start. A non-match is never equal to the event n steps back.
 * The non-match comes from one modulo.
 *
 * On success returns NULL and stores the sequence in *out.
 * On failure returns one of the NBACK_ERROR_* names and stores NULL in *out.
 * A rejected request does not write into the fixed buffer.
 * Call nback_free on *out in both cases. Freeing NULL is allowed.
 */
const char *nback_create(int size, int stimuli, int n, int matches,
                         unsigned int seed, Nback *out);

void nback_free(Nback sequence);

/* sequence must not be NULL. nback_free is the call that accepts NULL. */
int nback_length(Nback sequence);
const int *nback_values(Nback sequence);

#endif
