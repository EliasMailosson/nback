# Genomförandeplan: N-back grundnivå

Datum: 2026-11-02.
Status: issueplan skapad; ingen implementation har påbörjats i denna uppgift.
Uppgifter spåras i [GitHub Issues](https://github.com/EliasMailosson/nback_skeletoncode/issues).
15 issues är publicerade. NB-16 finns som lokalt utkast eftersom publiceringen nekades i verktygsdialogen.

## Underlag och basrevision

- [SPEC.md](../SPEC.md) beskriver krav och vilka beslut som är godkända respektive öppna.
- [AGENTS.md](../AGENTS.md) beskriver arbetsregler, arkitekturbeslut, granskning och behörigheter.
- Kodbasen kontrollerades på master, revision `5394e320ead98f5858ffc3b67f3a29161293009d`.
  GitHubs master hade samma revision vid förkontrollen.
- SPEC.md och AGENTS.md publicerades på uttryckligt uppdrag i commit `9628628`.
- Före varje implementation ska agenten jämföra issuen med senaste mergade kod och
  dokumentera eventuella justeringar inom den godkända omfattningen.

## Beslutad inriktning

- Behåll sekvensgenereringen i C och den befintliga kopplingen via JNI till Android.
- 2-back med 20 händelser, exakt 6 matcher och slumpade matchplatser bland händelse 3–20.
- Visuellt läge och ljudläge var för sig; 1 800 ms visning plus 200 ms släckt del per visuell händelse.
- Godkänd poängformel, gemensamt bestående high score och avbrott vid bakgrund/rotation.
- Lättförklarlig och idiomatisk kod, små avgränsade uppgifter och redovisningsövning längs vägen.
- Högre nivå, återföring av stashar och botintegration ingår inte i denna plan.

C-valet behåller språket, men den nuvarande generatorn behöver ändras för det exakta
matchkravet. Dess befintliga test tillåter en tolerans; NB-02 ska kontrollera exakt
antal verkliga matchningar. C-buffertens gränser och minnesägandet mellan C och JNI
behöver granskas i NB-03. Detta är observationer från kodläsning, inte nya testresultat.

## Ordning och beroenden

Detta är ett index till GitHub-uppgifterna, inte en separat konkurrerande att-göra-lista.
Varje issue innehåller mål, tre godkännandekriterier, verifiering, troliga filer och
1–3 relevanta redovisningsfrågor. Följ ordningen och de uttryckliga beroendena.

- NB-01: [#1: Fastställ återstående tekniska beslut före implementation](https://github.com/EliasMailosson/nback_skeletoncode/issues/1). Beroenden: inga.
- NB-02: [#2: Generera exakt sex 2-back-matcher i C](https://github.com/EliasMailosson/nback_skeletoncode/issues/2). Beroenden: #1.
- NB-03: [#3: Hantera minne och fel säkert i C/JNI-anropet](https://github.com/EliasMailosson/nback_skeletoncode/issues/3). Beroenden: #2.
- NB-04: [#4: Öppna spelskärmen från startskärmen](https://github.com/EliasMailosson/nback_skeletoncode/issues/4). Beroenden: #1.
- NB-05: [#5: Visa en angiven position i ett kvadratiskt 3×3-rutnät](https://github.com/EliasMailosson/nback_skeletoncode/issues/5). Beroenden: #4.
- NB-06: [#6: Kör en avbrytbar visuell runda med 20 händelser](https://github.com/EliasMailosson/nback_skeletoncode/issues/6). Beroenden: #3, #5.
- NB-07: [#7: Registrera högst ett matchtryck per giltig händelse](https://github.com/EliasMailosson/nback_skeletoncode/issues/7). Beroenden: #6.
- NB-08: [#8: Beräkna poäng och avsluta rundans resultat](https://github.com/EliasMailosson/nback_skeletoncode/issues/8). Beroenden: #7.
- NB-09: [#9: Ge mild animerad återkoppling vid feltryck](https://github.com/EliasMailosson/nback_skeletoncode/issues/9). Beroenden: #8.
- NB-10: [#10: Spara gemensamt high score efter avslutad runda](https://github.com/EliasMailosson/nback_skeletoncode/issues/10). Beroenden: #8.
- NB-11: [#11: Gör uppläsningen redo och hantera saknad röst](https://github.com/EliasMailosson/nback_skeletoncode/issues/11). Beroenden: #1, #6.
- NB-12: [#12: Spela en fullständig runda med upplästa bokstäver](https://github.com/EliasMailosson/nback_skeletoncode/issues/12). Beroenden: #3, #9, #10, #11.
- NB-13: [#13: Kontrollera grundskärmarnas läsbarhet och tryckytor](https://github.com/EliasMailosson/nback_skeletoncode/issues/13). Beroenden: #12.
- NB-14: [#14: Verifiera grundversionens kompletta spel- och livscykelflöden](https://github.com/EliasMailosson/nback_skeletoncode/issues/14). Beroenden: #13.
- NB-15: [#15: Sammanställ AI-process, testrapport och egen reflektion](https://github.com/EliasMailosson/nback_skeletoncode/issues/15). Beroenden: #14.
- NB-16: [NB-16: Öva redovisningen och stäm av grundnivåns krav](issue-NB-16.md) (lokalt, ej publicerat). Beroenden: #15.

## Kontrollpunkter

- Efter NB-01–03: krav/beslut är förankrade, C-generatorn uppfyller det exakta
  matchkravet och JNI-kontraktet är verifierat.
- Efter NB-04–06: startskärm, rutnät och en hel visuell runda fungerar, inklusive avbrott.
- Efter NB-07–09: svarsspärrar, poäng, sista svarsfönstret och animerad återkoppling är verifierade.
- Efter NB-10–12: high score och en hel ljudrunda fungerar med samma spelregler.
- Efter NB-13–14: läsbarhet, tryckytor och hela grundflödet har kontrollerats på aktuell kod.
- Efter NB-15–16: dokumentation och redovisningsövning är genomgångna; studenten tar nästa beslut.

Kontrollpunkterna är planerade och har inte utförts. Varje enskild issue avslutas
dessutom med förklaring och studentens återkoppling innan nästa påbörjas.

## Gemensam verifiering och klart-definition

- Kör relevanta tester för beteendet i issuen och bygg den berörda appversionen.
- Native/JNI-testning kan använda befintliga instrumentationstester på Android.
  Reproducerbarhet, felkontrakt och eventuell ny testinfrastruktur beslutas i NB-01.
- Offline-kommandona i issues förutsätter installerade beroenden. Hämta eller
  installera inget nytt utan godkännande. Rapportera om en kontroll inte kan köras.
- Redovisa testad revision, verkliga resultat, manuell provkörning och begränsningar.
- I ett uttryckligen tilldelat issue-till-PR-uppdrag granskar en annan agent aktuell
  commit innan studenten granskar slutligt. Ingen granskare har tilldelats här.
- Studenten beslutar om merge och nästa issue. En publicerad issue är inte ett
  uppdrag till en bot att automatiskt börja implementera.
- Alla större arkitekturval kräver uttryckligt beslut, även om behovet upptäcks mitt i arbetet.
- Följ integritetsreglerna i AGENTS.md och skicka inte privat kod eller material
  vidare bara för att issuebeskrivningen ligger offentligt.

## Öppna frågor och begränsningar

- SPEC är fortfarande märkt utkast. NB-01 ska förankra de kvarvarande tekniska valen
  och ansvarsfördelningen; redan godkända beslut ska inte tas om utan ändrade förutsättningar.
- Grundnivåns formulering om kombinerat läge är oklar. Avstämning med läraren behövs
  före ett påstående om full kravuppfyllelse; ingen extern kontakt görs automatiskt.
- Filnamn som inte finns ännu är förslag. Inför bara de hjälpfiler som faktiskt behövs.
- Ljud måste provlyssnas. Genomförd kompilering är inte bevis på korrekt uttal eller ljudlivscykel.
- Frågor om inställningslagring och poängdatabas hör till en eventuell högre nivå.
  Övriga relevanta frågor fördelas på issues, och fråga 26 återkommer i AI-processen.
- NB-16 är inte publicerad. Utkastet finns i [issue-NB-16.md](issue-NB-16.md).

## Vad som verifierades vid planeringen

- Repositoryt är offentligt och Issues är aktiverat.
- SPEC.md och AGENTS.md har committats och pushats enligt studentens uppdrag.
- Alla 15 publicerade issues lästes tillbaka från GitHub: titlar, hela texter och
  beroendereferenser stämde med de förberedda utkasten.
- Issue-ID:n och beroendeordningen kontrollerades; varje uppgift har kriterier,
  verifiering och redovisningsövning.
- Inga appbyggen, speltester, PR-granskningar eller implementationer utfördes för
  denna dokumentations- och planeringsuppgift.
