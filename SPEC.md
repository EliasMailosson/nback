# N-back för Android - specifikation för grundnivån

Status: utkast för granskning. Ingen implementation startas enbart för att denna fil finns.
Datum: 2026-10-30.
Senast uppdaterad: 2026-11-03.

## 1. Syfte och källa

Bygg en fungerande N-back-app i Kotlin och Jetpack Compose, utifrån det befintliga
Android-skelettet. Målet är grundnivån i labben och att studenten kan hitta,
förklara och testa sin kod på redovisningen.

Källa: *Lab B:1 - Building a Native N-Back Mobile App*, daterad 25 oktober 2026.
- Uppgift: https://canvas.kth.se/courses/65590/assignments/398378
- PDF: https://canvas.kth.se/courses/65590/files/10917586

Labbkrav nedan är en sammanfattning av den lokala PDF-kopian, inte nya lärarkrav.
Föreslagna projektbeslut är uttryckligen märkta. Ändrade kurskrav kontrolleras i
Canvas inför inlämning.

## 2. Beslutad omfattning och arbetssätt

- Android, Kotlin och Jetpack Compose. Ingen webbversion.
- Grundnivån färdigställs först. Högre nivå kan göras därefter om tiden räcker
  och studenten uttryckligen vill gå vidare.
- Arbeta i små, atomiska steg: en avgränsad förändring med tillhörande verifiering.
- Utgå från skelettet. Den tidigare spelversionen i stashen återförs inte automatiskt.
- Studenten ska förstå förändringen innan arbetet går vidare till nästa del.
- Koppla arbetet till så många relevanta redovisningsfrågor som möjligt.

### Tekniskt upplägg - beslutad C-generator

Skelettet har redan Compose för gränssnittet, en ViewModel som håller spelstatus
och ett repository för high score. ViewModel anropar i dag NBackHelper, som i sin
tur anropar C-kod för att skapa sekvensen.

Beslutat: sekvensgenereringen behålls i C och anropas från Kotlin via JNI.
C-generatorn behöver anpassas till den godkända metoden med exakt sex matcher;
språkvalet innebär inte att dess nuvarande algoritm redan uppfyller kraven.
Förslaget att ersätta generatorn med Kotlin har därmed valts bort.

Kotlin och Compose används fortsatt för Android-delen. Ansvarsfördelning,
timer, ljud, navigation, tillstånd, C/JNI-kontrakt och tester är beslutade,
se avsnitt 4.2. Behåll skelettets användbara struktur, håll reglerna
testbara och inför bara klasser eller gränssnitt med konkret nytta. Befintliga
C-filer och bygginställningar tas inte bort som orelaterad städning.

Alternativet att återföra hela den tidigare prototypen är snabbare som start,
men väljs inte som arbetssätt: studenten vill gå igenom små förändringar.
En nystart i ett annat projekt skulle ge onödigt arbete och lämna skelettet.

### Kodkvalitet och officiell dokumentation

- Koden ska vara idiomatisk Kotlin och Compose: använd språkets och ramverkets
  etablerade mönster där de passar, inte direktöversatt Java/C eller onödigt smarta konstruktioner.
- Följ Kotlins officiella namn-, formaterings- och kodkonventioner. Prioritera tydliga
  namn, läsbara funktioner och lättförklarliga lösningar framför kortast möjliga kod.
- Följ relevant officiell Android-/Compose-dokumentation för tillstånd, livscykel,
  resurshantering och arkitektur, anpassad till appens storlek.
- Kontrollera att använda API:er och kodexempel passar projektets installerade
  versioner. Uppgradera inte beroenden eller kopiera nyare API:er slentrianmässigt.
- Motivera relevanta avvikelser från rekommendationerna. Dokumentera ansvar och
  icke-självklara beslut; kommentarer ska förklara varför, inte bara återberätta syntax.
- Kodgranskning mot dessa krav ingår i varje berört atomiskt steg. En godkänd
  testkörning är inte i sig bevis för att koden följer alla rekommendationer.

Primära dokumentationskällor:
- Kotlin Coding conventions: https://kotlinlang.org/docs/coding-conventions.html
- Kotlin Idioms: https://kotlinlang.org/docs/idioms.html
- Android Architecture recommendations: https://developer.android.com/topic/architecture/recommendations

### Enkel och lättförklarlig kod

Kravet bygger på principerna i skillen `agent-skills:code-simplification`:
koden ska vara lätt att läsa, förstå, ändra och felsöka. Färre kodrader är inte
ett mål i sig. Studenten ska kunna följa flödet och förklara lösningen på redovisningen.

- Ge funktioner ett tydligt ansvar och använd beskrivande namn. Dela upp långa
  funktioner när de blandar ansvar, inte enbart för att nå en viss radgräns.
- Undvik djup nästling, svårtolkade uttryck och täta kedjor av operationer när
  tydliga villkor, namngivna mellanresultat eller hjälpfunktioner gör koden mer begriplig.
- Inför bara klasser, gränssnitt och abstraktioner som har ett konkret syfte i
  appen nu. Bygg inte generell infrastruktur för tänkbara framtida funktioner.
- Behåll ansvarsfördelningen i MVVM och hjälpfunktioner som förbättrar förståelse
  eller testbarhet. Enkelhet innebär inte att samla hela appen i en stor klass.
- Förstå befintlig kod och dess gränsfall innan den förenklas. Följ projektets
  konventioner och avgränsa förenklingen till den aktuella uppgiften.
- Gör rena förenklingar som separata atomiska steg från nya funktioner och
  buggfixar. Bevara resultat, sidoeffekter, ordningsföljd, felhantering och gränsfall.
  Kör relevanta befintliga tester efteråt; ta inte bort skydd eller försvaga tester
  för att få en förenkling att passera.

Vid granskning av varje ändrad del ska vi kunna svara på: Vad ansvarar den för,
vilka indata och resultat har den, vilka gränsfall hanteras och varför behövs
dess hjälpfunktioner eller abstraktioner? Om koden är svårare att förklara än
nödvändigt ska vi förenkla den innan delen betraktas som klar.

## 3. Funktionella krav

### Startskärm

- Visa sparat high score.
- Låt spelaren välja visuellt läge eller ljudläge.
- Visa valt läge, N, tid per händelse och antal händelser per runda.
- Ge ett tydligt sätt att öppna spelet och starta en ny runda.

### Spelskärm

- I visuellt läge: visa ett 3×3-rutnät med en markerad position per händelse.
- I ljudläge: läs upp en bokstav per händelse från en fast, tydligt urskiljbar uppsättning.
- Visa aktuellt händelsenummer och antal korrekta matchtryck.
- Ha en matchknapp och ett sätt att lämna spelet.
- Ge mild animerad återkoppling vid feltryck, utan kränkande text eller ljud.
- Visa när rundan är klar och låt spelaren starta en ny runda.

### Regler för en runda

- En match betyder samma stimulus som exakt N händelser tidigare.
- Sekvensen ska ha ungefär 20-40 procent matchningar.
- De första N händelserna är uppvärmning och kan aldrig registreras som matcher.
- Ignorera matchtryck före start, efter slut och under uppvärmningen.
- Godkänn högst ett tryck per händelse, även om det första trycket var fel.
- Återställ tryckskyddet först när nästa händelse börjar.
- Räkna rätta tryck, feltryck och obesvarade matcher. Avsluta även den sista
  händelsens svarsfönster innan slutresultatet bestäms.
- Timer, stimulus och händelsenummer måste avse samma aktuella händelse.
- Stoppa timern när rundan slutar eller spelaren lämnar spelskärmen.
- Ny runda nollställer räknare och gamla timerjobb innan den börjar.

## 4. Projektbeslut och kvarvarande förslag

Här skiljs studentens godkännanden från assistentens återstående förslag.
Godkända projektval är inte nya lärarkrav och innebär inte att beteendet är implementerat eller testat.

### 4.1 Godkänt av studenten

- 2-back med 20 händelser totalt per runda, inklusive de två uppvärmningshändelserna.
- Sekvensgeneratorn ska vara skriven i C och fortsatt användas från Android via JNI.
- Exakt 6 matcher per runda: 30 procent av rundans samtliga 20 händelser.
  De två uppvärmningshändelserna kan inte vara matcher. Bland de 18 möjliga
  jämförelserna motsvarar detta cirka 33 procent.
- Generering enligt alternativ 1: välj slumpmässigt 6 olika händelser bland
  nummer 3-20 som matchplatser. Bygg sedan sekvensen i ordning. Vid en matchplats
  kopieras stimuluset två steg tillbaka; vid övriga jämförbara händelser väljs
  ett annat stimulus. Därmed ska inga oavsiktliga extra matcher uppstå.
- Testet ska självständigt jämföra varje stimulus med det två steg tidigare
  och kontrollera att exakt 6 verkliga matcher finns. Generatorns egna markeringar
  får inte användas som bevis. Längd, giltiga värden och uppvärmningsregler testas också.
  Den godkända metoden implementeras i den beslutade C-generatorn.
- Visuellt stimulus syns i 1 800 ms och följs av 200 ms utan markering, så att
  två likadana positioner i följd fortfarande går att uppfatta som två händelser.
  Matchknappen avser samma händelse under hela dess 2 000 ms.
- Bokstavsuppsättning att börja med: A, F, K, M, R, U. Uttalet måste provlyssnas
  med den röst som används; uppsättningen ändras om bokstäverna är svåra att skilja åt.
- Poäng = max(0, antal rätta tryck - antal feltryck). En obesvarad match ger
  ingen poäng och redovisas separat som missad. Tystnad vid en icke-match ger ingen poäng.
- En färdig runda kan höja high score. Ett sämre resultat får inte sänka rekordet,
  och rekordet ska finnas kvar efter att appen stängts. Historik med namn och datum skjuts upp.
- Visuellt läge och ljudläge har ett gemensamt high score.
- Avbryt rundan när appen går till bakgrunden eller spelskärmen återskapas,
  exempelvis vid rotation. Stoppa rundans timer och pågående uppläsning.
  Rundan återupptas inte automatiskt och det avbrutna resultatet sparas inte.

### 4.2 Beslut i NB-01

Valen finns i `tasks/decisions-nb01.md`.

Fortfarande förslag:

- Ingen server, inloggning, analysinsamling eller nätverksfunktion behövs.

### 4.3 Varifrån valen kommer

- N = 2 och 2 000 ms finns redan i skelettet. Även värdet 30 procent används i dess
  generatoranrop. Studenten har valt alternativ 1 i 4.1: exakt 6 matcher av totalt
  20 händelser med slumpade matchplatser. Det tidigare förslaget om 5 matcher
  bland 18 jämförelser har ersatts av detta beslut. Studenten har valt att behålla C.
- Förslaget om 20 händelser kom från assistentens tidigare prototyp och är inte
  ett labbkrav eller ett belagt optimalt värde. Studenten har nu valt 2-back och 20 händelser.
- Fördelningen 1 800/200 ms, bokstäverna och poängformeln började som assistentens
  designförslag och har nu godkänts. Speltest och provlyssning återstår.
- Avbrott vid bakgrundsläge och återskapad spelskärm började som assistentens
  förslag och har nu godkänts. Det preciserar beteenden utöver labbens uttryckliga
  krav på att stoppa timern när rundan slutar eller spelaren lämnar spelskärmen.

## 5. Avgränsning och oklarhet i labbtexten

Dual N-back, redigerbara och sparade inställningar, poänghistorik i databas och
en särskild satsning på surfplattor/liggande läge skjuts upp till eventuell högre nivå.
Grundversionen ska ändå ha läsbara etiketter, användbara knappar och ett kvadratiskt rutnät.

PDF:ens grunddel nämner valet visuellt, ljud eller båda, medan avsnittet för högre
nivå uttryckligen kräver dual N-back. Utkastet tolkar grundnivån som visuellt och
ljud var för sig. Den tolkningen behöver bekräftas med läraren innan vi kan hävda
att hela grundnivån är uppfylld. Om båda krävs även där måste omfattningen justeras.

## 6. Tester och kriterier för klart

Labbkravet är tester inom minst tre av de fem angivna områdena. Projektets förslag
är att täcka alla fem, i små steg tillsammans med respektive funktion:

1. Sekvensgenerering: rätt längd och giltiga rutpositioner/bokstäver.
2. Matchandel: rätt antal avsedda matcher och inga oavsiktliga extra matcher.
3. Första-N-garantin: inga matchtryck eller poäng under uppvärmningen.
4. Dubbeltryck: andra trycket ändrar inte resultatet; nästa händelse kan besvaras igen.
5. Poäng: rätt och fel räknas korrekt och totalen blir aldrig negativ.

Testa också gränserna före start och efter slut samt missad match på sista händelsen.
Automatiska tester ska vara reproducerbara, inte bero på slumpmässig tur eller
verklig väntetid. Ljud provlyssnas på enheten; skärmnavigering, timeravslut och
sparat high score kontrolleras manuellt och/eller med lämpliga Android-tester.

Varje steg är klart först när relevant bygg-/testkontroll har utförts, studenten
har fått en förklaring och kan provköra eller gå igenom testet. Otestade beteenden
och saknad emulatorverifiering redovisas uttryckligen, inte som godkända.

## 7. Arbetsgång och redovisningsövning

För varje atomiskt steg:

1. Beskriv ett tydligt mål, vad som inte ingår och vilka filer som berörs.
2. Skriv eller uppdatera ett relevant test där det är möjligt.
3. Gör minsta sammanhängande kodändring och verifiera den.
4. Förklara dataflödet och låt studenten hitta den relevanta koden.
5. Ta upp 1-3 relevanta frågor från labbens lista och låt studenten försöka svara.
6. Redovisa AI-bidrag, faktisk verifiering och kvarvarande brister.
7. Stanna för studentens återkoppling innan nästa steg.

Atomiskt betyder här avgränsat och verifierbart, inte en automatisk Git-commit.
Exempel: implementera dubbeltrycksskyddet och dess test, inte hela spelmotorn på en gång.

### Frågor att koppla till respektive område

Numren kommer från labb-PDF:en. Beskrivningarna nedan är sammanfattningar,
inte ordagranna frågor eller färdiga svar.

- Sekvens och matchning: 1-3. Följ även ett visuellt respektive auditivt stimulus
  genom programmet, den för grundnivån relevanta delen av fråga 4.
- Spelläge, status och timer: 5-8, 12 och 14. Händelseflödet i fråga 6 följs
  för det aktuella läget, inte som om dual redan fanns.
- Dubbeltryck, poäng och återkoppling: 9-11, 13 och 28.
- MVVM, observerbart tillstånd, navigation och testbarhet: 15-17.
- Ljudmotor, bokstavsuppsättning och saknad röst: 18-20.
- Bakgrundsarbete, kvadratiskt rutnät, knappar och systemytor: 23-25.
  Telefon/surfplatta och båda orienteringarna är ännu inte verifierade högre-nivåkrav.
- AI-process och testdemonstration: 26-30. Fråga 26 är enligt PDF:en alltid öppningsfrågan:
  vilka delar studenten skrev själv respektive fick AI-hjälp med.
- Fråga 21 om sparade inställningar och fråga 22 om poängdatabas sparas till eventuell
  högre nivå. High score-lagring är inte samma sak som att dessa funktioner är färdiga.

## 8. Inlämningsunderlag och säker arbetsgräns

- Förbered full projektkod med tydliga namn, lämpliga klasser och begripliga kommentarer.
- Samla under arbetet underlag till ett kort AI-processdokument, testrapport med
  testfiler och en egen reflektion, enligt labbens krav.
- Studenten ska kunna förklara varje inlämnad kodrad, även AI-genererad kod.
- Ändra inte originalkursmaterial, byggkonfiguration eller befintligt användararbete
  som en orelaterad del av en uppgift.
- Inga commits, branches, worktrees, subagenter, installationer, uppladdningar eller
  publiceringar utan studentens uttryckliga uppdrag eller godkännande.
- Denna specifikation godkänns innan en separat genomförandeplan tas fram.
- Grundnivån färdigställs och verifieras innan högre nivå övervägs.
