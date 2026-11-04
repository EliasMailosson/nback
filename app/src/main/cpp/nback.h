#ifndef nback_h
#define nback_h

/* Names are part of the JNI contract and must match NBackGeneration in Kotlin. */
#define NBACK_ERROR_LENGTH "INVALID_LENGTH"
#define NBACK_ERROR_STIMULI "INVALID_STIMULI"
#define NBACK_ERROR_N "INVALID_N"
#define NBACK_ERROR_MATCHES "INVALID_MATCH_COUNT"
#define NBACK_ERROR_ALLOCATION "ALLOCATION"

typedef struct nback_type *Nback;

/*
 * Builds one sequence.
 *
 * size     number of events
 * stimuli  number of different values; each value is in 1..stimuli
 * n        look-back distance
 * matches  exact number of events that equal the event n steps earlier
 * seed     same seed gives the same sequence
 *
 * The first n events are warmup and are never matches.
 * Match positions are chosen among the later events, then the sequence is
 * filled from the start. A non-match is never equal to the event n steps back.
 *
 * On success returns NULL and stores the sequence in *out.
 * On failure returns one of the NBACK_ERROR_* names and stores NULL in *out.
 * Call nback_free on *out in both cases. Freeing NULL is allowed.
 */
const char *nback_create(int size, int stimuli, int n, int matches,
                         unsigned int seed, Nback *out);

void nback_free(Nback sequence);
int nback_length(Nback sequence);
const int *nback_values(Nback sequence);

#endif
