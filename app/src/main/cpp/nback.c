#include <stdlib.h>
#include "nback.h"

#define CONTENT_MAX_SIZE 100

struct nback_type {
    int content[CONTENT_MAX_SIZE];
    int size;
};

/* Next value from the seed. The low bits of this step alternate, so the
   returned value keeps the higher bits. The same seed still gives the same series. */
static unsigned int next_from_seed(unsigned int *seed) {
    *seed = (*seed * 1664525u) + 1013904223u;
    return (*seed >> 16) & 0x7FFFu;
}

static const char *check_input(int size, int stimuli, int n, int matches) {
    if (size < 1 || size > CONTENT_MAX_SIZE) {
        return NBACK_ERROR_LENGTH;
    }
    if (stimuli < 1) {
        return NBACK_ERROR_STIMULI;
    }
    if (n < 1 || n >= size) {
        return NBACK_ERROR_N;
    }

    int possibleMatches = size - n;
    if (matches < 0 || matches > possibleMatches) {
        return NBACK_ERROR_MATCHES;
    }

    /* A non-match must be able to differ from the event n steps back. */
    int nonMatches = possibleMatches - matches;
    if (nonMatches > 0 && stimuli < 2) {
        return NBACK_ERROR_STIMULI;
    }
    return NULL;
}

/* Pick `matches` different indices from n .. size-1. For 2-back and 20 events that is events 3..20. */
static void choose_match_positions(int isMatch[], int size, int n, int matches,
                                   unsigned int *seed) {
    int candidateCount = size - n;
    int candidates[CONTENT_MAX_SIZE];
    for (int i = 0; i < candidateCount; i++) {
        candidates[i] = n + i;
    }

    for (int i = 0; i < matches; i++) {
        int remaining = candidateCount - i;
        int pick = next_from_seed(seed) % remaining;
        int from = i + pick;
        int chosen = candidates[from];
        candidates[from] = candidates[i];
        candidates[i] = chosen;
        isMatch[chosen] = 1;
    }
}

/* A value in 1..stimuli that is not `forbidden`. Requires stimuli >= 2. */
static int stimulus_except(unsigned int *seed, int stimuli, int forbidden) {
    int choice = next_from_seed(seed) % (stimuli - 1);
    int value = choice + 1;
    if (value >= forbidden) {
        value += 1;
    }
    return value;
}

static void write_sequence(int content[], int size, int stimuli, int n,
                           int matches, unsigned int seed) {
    int isMatch[CONTENT_MAX_SIZE] = {0};
    choose_match_positions(isMatch, size, n, matches, &seed);

    for (int i = 0; i < size; i++) {
        if (i >= n && isMatch[i]) {
            content[i] = content[i - n];
        } else if (i >= n) {
            content[i] = stimulus_except(&seed, stimuli, content[i - n]);
        } else {
            content[i] = 1 + (next_from_seed(&seed) % stimuli);
        }
    }
}

const char *nback_create(int size, int stimuli, int n, int matches,
                         unsigned int seed, Nback *out) {
    *out = NULL;

    const char *error = check_input(size, stimuli, n, matches);
    if (error != NULL) {
        return error;
    }

    Nback sequence = malloc(sizeof(*sequence));
    if (sequence == NULL) {
        return NBACK_ERROR_ALLOCATION;
    }

    sequence->size = size;
    write_sequence(sequence->content, size, stimuli, n, matches, seed);
    *out = sequence;
    return NULL;
}

void nback_free(Nback sequence) {
    free(sequence);
}

int nback_length(Nback sequence) {
    return sequence->size;
}

const int *nback_values(Nback sequence) {
    return sequence->content;
}
