# Verifiering av grundversionen

## Kommando

JVM och bygge, på varje gren i stacken:

```
./gradlew --offline :app:testDebugUnitTest :app:assembleDebug
```

Instrumentation, på den här grenen, mot en headless-emulator (API 30, x86_64, KVM):

```
./gradlew :app:connectedDebugAndroidTest
```

## Resultat

JVM-testerna på den här grenen är 43, alla gröna, och debug-apk byggs.

| Test | Antal |
| --- | --- |
| GameStateTest | 1 |
| PositionGridTest | 2 |
| RoundTimingTest | 6 |
| MatchResponseTest | 8 |
| RoundResultTest | 7 |
| HighScoreTest | 5 |
| SpeechTest | 4 |
| AudioRoundTest | 8 |
| ReadabilityTest | 1 |
| ExampleUnitTest | 1 |

Instrumentation: 14 tester, alla gröna. Där ingår `ExampleInstrumentedTest` (1), `NBackHelperTest` (8), `NavigationTest` (2) och `PositionGridUiTest` (3).

## Buggar som granskningen hittade

Ett rätt tryck visades som Rätt: 1 och föll sedan tillbaka till 0. Timern publicerade nästa tomma steg från en äldre kopia av rundan. Från NB-07 läser loopen samma runda efter varje väntan.

Rotation körde om återställningen till Redo, eftersom den satt i skärmens `LaunchedEffect`. Då försvann både en avbruten runda och ett visat resultat. Återställningen anropas nu bara när spelaren öppnar visuellt läge eller ljudläge.

Ljudstarten väntade på rösten i en coroutine som avbrott inte kunde stoppa. Tryckte man Tillbaka innan rösten var klar kunde rundan ändå starta. Den väntan ligger nu i samma jobb som avbrott avbryter.

## Kvar att göra för hand

- Provkör visuellt läge och ljudläge på en telefon: start, 20 händelser, sista svarstiden, resultat, ny runda.
- Lyssna på A, F, K, M, R, U. Uttalet är inte bedömt. En upprepad bokstav ska höras som två händelser.
- Feltryck: texten "Inte en match" ska synas under det släckta steget, och rundan ska fortsätta.
- Rekord efter att appen stängts och öppnats. Ett sämre resultat ska inte sänka det.
- Avbrott med tillbaka, bakgrund och rotation. Efter rotation ska en avbruten runda fortfarande visas som avbruten, och ett färdigt resultat ska ligga kvar.
- Saknad röst, om röstdata kan stängas av. Visuellt läge ska ändå gå.
- Läraren ska bekräfta att visuellt läge och ljudläge var för sig räcker, enligt beslut D10. Kombinerat läge är inte byggt.

Surfplatta och liggande läge ingår inte i grundnivån och är inte provade.
