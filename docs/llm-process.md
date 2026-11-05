# How I used an LLM for the basic-level N-back app

## Tools

I used Grok Bot, an assistant app, to coordinate Cursor cloud agents. Those agents wrote the code on branches and opened a pull request for each step. A second Grok Bot reviewed every pull request. I made the main decisions and did the final review before merge.

The decisions are D1–D10 in `tasks/decisions-nb01.md`. One later choice was that the play button already asks for 20 events, N = 2 and exactly 6 matches.

## Context I gave the assistant

For each step I pointed it at the repository, the GitHub issue, the code on the default branch, and these files:

- `SPEC.md` — the basic-level requirements and the choices already approved
- `AGENTS.md` — the working rules for the project
- `tasks/plan.md` — the order of steps NB-01 to NB-15
- `tasks/decisions-nb01.md` — after I had chosen D1–D10

This repository has no `STYLE.md`. The notes on Kotlin and Compose style are in `SPEC.md` and `AGENTS.md`.

## Three prompts that shaped the code

**1. The C generator (NB-02).** I asked for that issue only, following the decisions already made:

> Work on issue NB-02. Read the issue, AGENTS.md, SPEC.md and tasks/decisions-nb01.md first. D6: the input is length, number of stimuli, N, an exact match count and a seed; the call returns the sequence or a named error; the allocation is always freed. D7: at least three fixed seeds in NBackHelperTest, with exactly six matches. Do only what NB-02 requires. Run the tests this environment can run, and report exactly what ran and what could not.

Three questions were still open. I answered:

> (1) Change the button in GameVM so it already asks for 20 events, N = 2 and exactly 6 matches. (2) Keep the stimulus values 1..9 and 1..6. (3) Leave the length limit of 100 to NB-03.

That produced `app/src/main/cpp/nback.c`, the JNI call in `JniBridge.c`, and `NBackHelper.generateNBackString`. A round asks for 20 events, N = 2 and 6 matches (`BasicRound`), with 9 positions or 6 letters. `NBackHelperTest.threeSeedsGiveExactlySixMatches` uses seeds 1, 8 and 42 and counts matches by comparing each value with the one two steps earlier.

**2. Safe memory and errors (NB-03).**

> Make the Kotlin-to-C path robust. An unreasonable length, N, stimulus count or match request must be rejected with the named errors INVALID_LENGTH, INVALID_STIMULI, INVALID_N, INVALID_MATCH_COUNT or ALLOCATION, before it can overrun a buffer, divide by zero or search forever. Free C memory on the normal path and on the error path. Valid visual and audio calls still return exactly six matches. Do not weaken the NB-02 tests.

That produced the checks in `check_input`, freeing the C sequence after the values are copied, and the same error names on `NBackGeneration`. `NBackHelperTest` covers impossible values and the length limit of 100.

**3. The rest of the basic level (NB-04 to NB-14).**

> Implement issues NB-04 through NB-14, in order, one issue per branch and per pull request. Only the basic level. Keep the code as small and readable as possible. Add JVM unit tests where the issue asks for them, with fakes for the pause and for speech. A known leftover: if generation returns an error, GameVM currently starts an empty round. If a later issue touches that code, handle it simply there.

That produced the home screen and the game screen, the 3×3 grid, the visual round (1 800 ms lit, 200 ms blank), one match press per event, the score max(0, correct − wrong), the text "Inte en match", one shared high score in DataStore, and the spoken letters A, F, K, M, R, U. The tests are listed in `docs/test-report.md`.

## What the assistant got wrong

The reviewer found these before merge, and they were corrected.

The random step used the low bits of the generator with `%`. Those bits alternate, so the sequences were patterned. With 9 stimuli, 1 was followed by 9 in 99% of cases. With 0 matches the sequence stuck in the cycle 1, 9, 4, 2, 6, 5, 8, 7. The call `nback_create(20, 9, 2, 0, 1, ...)` produced `1 7 8 6 1 9 4 2 6 5 8 7 1 9 4 2 6 5 8 7`. With 6 stimuli, the first two events were never equal. The step now keeps the high 15 bits, `(*seed >> 16) & 0x7FFF`, before the modulo. `sixStimuliCanStartWithTwoEqualEvents` checks that events 1 and 2 are equal for at least one seed from 1 to 50.

The timer kept its own copy of the round. A correct press showed one hit, then the old copy was written back and the count fell to 0. Answering all six matches ended as 0 correct, 6 missed and score 0. The loop now keeps one round and reads it again after each wait. `aCorrectPressSurvivesTheBlankStep` and `answeringAllSixMatchesScoresSix` cover that.

The game screen reset the round from a `LaunchedEffect`, which runs again when the phone rotates. An aborted round then showed "Redo", and a finished result disappeared. The reset now runs only when the player chooses visual or audio mode, in `MainActivity`, before navigation.

Starting audio waited for the voice in a job that Back could not cancel, so a 20-event round could still start. That wait is now the same job `abortRound` cancels. `abortBeforeTheVoiceIsReadyDoesNotStartTheRound` covers it. A save of the high score could also be cancelled if the player left during the write; the save now finishes (`aSaveFinishesAfterThePlayerLeaves`).

Two smaller review fixes: a failed JNI class or method lookup was reported as `ALLOCATION`, which hid the real error, and those exceptions are now left for Kotlin; a grid UI test called `setContent` inside a loop, which the Compose test rule allows only once. A generation error leaves the screen ready and shows that the round could not start (`aFailedGeneratorDoesNotStartAnEmptyRound`).

I have not listened to the letters on a phone, and I have not checked rotation, the background, or the saved record after closing and reopening the app. Those checks are listed in the test report.
