# Test report

Run on 5 November 2026, from the current basic-level code.

## What I ran

JVM unit tests:

```
./gradlew :app:testDebugUnitTest
```

**43 tests, 0 failures, 0 errors, 0 skipped.** The build succeeded.

| Test class | Tests | File |
| --- | ---: | --- |
| GameStateTest | 1 | `app/src/test/java/mobappdev/example/nback_cimpl/GameStateTest.kt` |
| PositionGridTest | 2 | `app/src/test/java/mobappdev/example/nback_cimpl/PositionGridTest.kt` |
| RoundTimingTest | 6 | `app/src/test/java/mobappdev/example/nback_cimpl/RoundTimingTest.kt` |
| MatchResponseTest | 8 | `app/src/test/java/mobappdev/example/nback_cimpl/MatchResponseTest.kt` |
| RoundResultTest | 7 | `app/src/test/java/mobappdev/example/nback_cimpl/RoundResultTest.kt` |
| HighScoreTest | 5 | `app/src/test/java/mobappdev/example/nback_cimpl/HighScoreTest.kt` |
| SpeechTest | 4 | `app/src/test/java/mobappdev/example/nback_cimpl/SpeechTest.kt` |
| AudioRoundTest | 8 | `app/src/test/java/mobappdev/example/nback_cimpl/AudioRoundTest.kt` |
| ReadabilityTest | 1 | `app/src/test/java/mobappdev/example/nback_cimpl/ReadabilityTest.kt` |
| ExampleUnitTest | 1 | `app/src/test/java/mobappdev/example/nback_cimpl/ExampleUnitTest.kt` |

Instrumented tests, on a headless emulator (API 30, Android 11, x86_64, KVM):

```
./gradlew :app:connectedDebugAndroidTest
```

**14 tests, 0 failures, 0 errors, 0 skipped.** The device was `nback_api30` (AVD), Android 11.

| Test class | Tests | File |
| --- | ---: | --- |
| NBackHelperTest | 8 | `app/src/androidTest/java/mobappdev/example/nback_cimpl/NBackHelperTest.kt` |
| NavigationTest | 2 | `app/src/androidTest/java/mobappdev/example/nback_cimpl/NavigationTest.kt` |
| PositionGridUiTest | 3 | `app/src/androidTest/java/mobappdev/example/nback_cimpl/PositionGridUiTest.kt` |
| ExampleInstrumentedTest | 1 | `app/src/androidTest/java/mobappdev/example/nback_cimpl/ExampleInstrumentedTest.kt` |

`ExampleUnitTest` and `ExampleInstrumentedTest` are the template tests from the skeleton (2 + 2, and the app package name). They do not check the game.

The pause and the voice in the JVM tests are fakes, so those tests do not wait two seconds per event and do not speak.

## Logic that was tested

**Sequence and match count** (`NBackHelperTest`, on the emulator, because it calls C through JNI). Seeds 1, 8 and 42 each give length 20, values in range, and exactly 6 real matches, for both 9 positions and 6 letters. The test counts matches itself. The same seed repeats the same sequence; seeds 1 and 2 differ. Bad length, stimulus count, N or match count returns `INVALID_LENGTH`, `INVALID_STIMULI`, `INVALID_N` or `INVALID_MATCH_COUNT`. Length 101 is rejected. Length 100 with 6 matches is accepted. `sixStimuliCanStartWithTwoEqualEvents` checks that events 1 and 2 can be equal for 6 stimuli, which the old low-bit generator never did.

**Grid.** `PositionGridTest` maps values 1–9 to one cell each, row by row from the top left, and leaves 0 and out-of-range values blank. `PositionGridUiTest` checks the same on screen, and that the grid is square.

**Round timing and a failed start** (`RoundTimingTest`). Each event is lit, then blank, and the last window closes. A failed generator leaves the screen ready. Abort stops the round, and a new start replaces it. `enterGameScreen` does not reset a round that is still playing. The full round finishes without waiting in real time.

**Presses** (`MatchResponseTest`). Presses before start, after the end, after abort, and during the first two events do nothing. The first correct press and the first wrong press are counted once. A second press on the same event is ignored even when the first was wrong (`aSecondPressOnTheSameEventIsIgnoredEvenWhenTheFirstWasWrong`). The blank 200 ms still belongs to the same event. A correct press and a wrong-press flag both survive that blank step. Only a first wrong press asks for the "Inte en match" feedback.

**Score** (`RoundResultTest`). The score is correct presses minus wrong presses, and never below 0. A wrong press is still stored when the shown score is 0. Silence on a non-match scores nothing and is not a miss. An unanswered match on the last event is one miss; an answered one is not. A new round starts at zero. Abort is not stored as a finished result. Answering all six matches scores 6.

**High score** (`HighScoreTest`). A higher score replaces the record in a real DataStore file; an equal or lower score does not. A finished round can raise the record; a weaker finished round does not. An aborted round is not stored. A failed save is not shown as a new record. A save that is still running when the player leaves still completes.

**Speech and the audio round** (`SpeechTest`, `AudioRoundTest`). Creating the view model does not speak. A missing voice does not start a round, and visual mode still works. A ready voice stays quiet until a round asks for a letter. Abort and clearing the view model stop the voice. Values 1–6 are A, F, K, M, R, U, and the same value is the same letter, so a match is the same letter. Each event is spoken once, including a repeat. A finished audio round uses the same record. Abort before the voice is ready does not start the round. Aborting audio stops speech and does not save. Visual mode does not speak.

**Screens.** `GameStateTest` checks that a new state is Ready and has no event. `ReadabilityTest` checks the match-button hint: empty when ready, "Uppvärmning" for the first two events, "Tryck om det är en match" after that, and "Svar registrerat" after a press. `NavigationTest` opens visual mode from the home screen (2-back, 20 events, 2 seconds, the record) and returns with Tillbaka and with the system back button.
