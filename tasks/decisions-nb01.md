# NB-01 — tekniska beslut

Studenten valde följande 2026-11-03. Tidigare val, bland annat C-generator, 2-back, 20 händelser, exakt sex matcher, poäng och avbrott, står i `SPEC.md` avsnitt 4.1.

### D1. Spellogik

**Val:** En liten Android-fri Kotlin-typ för en runda, till exempel `NBackRound`. `GameVM` anropar den. C genererar bara sekvensen.

**Motivering:** Match, tryck och poäng kan testas utan Android. ViewModel visar det gränssnittet läser.

**Bortvalt:**

- Regler i `GameVM` blandas med livscykel och lagring.
- Match och poäng i C gör anropet större utan att poängregeln blir tydligare.

### D2. Observerbart tillstånd

**Val:** `GameState` utökas med faserna `ready`, `playing`, `finished` och `aborted`. High score är ett eget flöde.

**Motivering:** Fas, händelsenummer och stimulus följs åt i ett värde. High score kommer från DataStore och överlever rundan.

**Bortvalt:**

- Ett flöde per värde kan uppdatera nummer och stimulus i otakt.
- Om skärmen äger räknarna delas avbrott och rotation mellan skärm och ViewModel.

### D3. Navigation

**Val:** `NavHost` i `MainActivity` med Navigation Compose, som redan finns i projektet. En `GameVM` skapas ovanför `NavHost`.

**Motivering:** Navigation Compose är ett tydligt, idiomatiskt sätt att byta skärm och finns redan i projektet.

**Bortvalt:**

- En egen skärmvariabel lämnar det tillagda biblioteket oanvänt.
- ViewModel ska inte äga vilken skärm som visas.

### D4. Timer

**Val:** Coroutine-loop med 1 800 ms och 200 ms, och en pausfunktion som tester kan byta ut. Jobbet stoppas uttryckligen vid avbrott.

**Motivering:** Loopen liknar skelettets väntan, men testet behöver inte vänta på riktig tid. Stoppet måste vara uttryckligt eftersom ViewModel överlever rotation.

**Bortvalt:**

- Riktig väntan gör att testet tar två sekunder per händelse.
- `Handler` eller `CountDownTimer` ligger vid sidan av coroutinerna och är svårare att testa.

### D5. Uppläsning

**Val:** En liten `TextToSpeech`-omslutning som `GameVM` äger. Den skapas när en ljudrunda startar, och stoppas vid avbrott och i `onCleared`. Tester använder en fejk.

**Motivering:** Bokstaven sägs i samma steg som händelsen byts. Uppläsningen startar inte bara för att skärmen ritas om.

**Bortvalt:**

- Om skärmen äger rösten måste den hålla samma händelse som klockan i ViewModel.
- Färdiga ljudfiler svarar inte på frågorna om ljudmotorn och saknad röst.

### D6. C/JNI-kontrakt

**Val:** Indata är längd, antal stimuli, N, exakt antal matcher och frö. Anropet returnerar sekvensen eller ett namngivet fel. Allokeringen frigörs alltid.

**Motivering:** Parametrarna säger att sex matcher ska väljas. Testet kan kräva ett namngivet fel, och minnet lämnas inte kvar.

**Bortvalt:**

- Ett procenttal garanterar inte exakt sex matcher, och undantag är svårare att testa.
- Tom array eller `null` är ett tyst fel, och klockans frö går inte att upprepa.

### D7. Tester

**Val:** Minst tre fasta frön i `NBackHelperTest`, med exakt sex matcher, plus vanliga tester med fejk för paus och uppläsning.

**Motivering:** Generatorn testas där JNI körs. Tryck, poäng och faser testas på datorn utan riktig väntan.

**Bortvalt:**

- Ett eget C-program på datorn är mer byggstruktur än grundnivån behöver nu.
- Klockans frö och toleransen ±1 testar inte kravet på exakt sex matcher.

### D8. Visade inställningar och high score

**Val:** Visa 2-back, 20 händelser och 2 sekunder som fasta värden. DataStore används bara till high score, och bara när en avslutad runda slår rekordet.

**Motivering:** Startskärmen visar rundans fasta värden. Rekordet ligger kvar i den lagring som redan finns.

**Bortvalt:**

- Att lagra N, längd och intervall är mer än baskravet.
- Ett värde bara i minnet överlever inte omstart. Room är större än det gemensamma rekordet.

### D9. Saknad röst

**Val:** Ljudrundan startar inte. En kort text visas på samma språk som övrig skärmtext. Visuellt läge fungerar som vanligt.

**Motivering:** Spelaren ser att uppläsningen saknas innan någon händelse går. Den visuella rundan behöver ingen röst.

**Bortvalt:**

- En kontroll som låser ljudknappen gör gränssnittet mer invecklat.
- En tyst runda, eller ett annat språk, ser spelbar ut utan att bokstäverna hörs.

### D10. Kombinerat läge

**Val:** Bygg visuellt läge och ljudläge var för sig. `AudioVisual` lämnas oanvänd. Läraren ska stämmas av innan någon påstår att baskravet är klart.

**Motivering:** Grundnivån kan byggas med två separata lägen. Påståendet att hela grundnivån är uppfylld väntar på läraren.

**Bortvalt:**

- Ett kombinerat läge nu är extra omfattning innan baskravet är klart.
- Att vänta med allt övrigt arbete tills läraren svarat stoppar arbetet i onödan.

**Avstämning med läraren:**
