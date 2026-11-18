# Pass 20 — context-sensitive English examples

French mode retains the exact Pass 19 two-example output. The 753-situation random bank, selection history, six French-only rows, refresh events and UI geometry are unchanged. Only normal English-mode example selection uses the new `EnglishLearnContexts` metadata. Changing the interface language recomputes the appropriate normal pair without mutating either dictionary or the saved random choices.

Each of the 95 verb/sense entries has reviewed context profiles. They select a French prefix, adverb position or suffix and its corresponding English wording. These choices are bound to the existing complement and are not random combinations or general tense-conversion rules. Two English-mode families select a more suitable underlying context throughout all six tenses: finding an answer and knowing Léa. Paired panels always refer to that same situation.

## Readings

- Present/future retain their ordinary readings.
- Imperfect distinguishes continuing actions, background states, perceptions and habits. Explicit souvent/encore/autrefois is used where helpful; English can use a continuous form, simple past, could, or used to.
- Perfect examples include dated events with simple past, experiential déjà with present perfect and before, and achieved enfin results with present perfect and finally. Hier is never paired with English present perfect. Promettre places Hier/Yesterday first so it dates the promise rather than ambiguously dating the promised visit.
- Past historic uses literary narrative frames (Ce jour-là/That day; Un jour/One day; Alors/Then). Connaître can mark becoming acquainted, while its imperfect remains a state.
- Pluperfect uses an earlier completion/experience/state, often déjà/already/before, or À midi/By noon for an action completed by that past reference. Some short sentences still rely on an implied past reference supplied by discourse or the paired comparison.
- Grandir describes a life stage, not an event compressed into yesterday or a single day. Its perfect and past historic both remain “I grew up in town.” Accuracy takes precedence over visibly different English.

The French sentence itself carries the added context; English never invents yesterday, before or finally independently. Normal English examples remain first-person singular. French mode includes first/second persons and nous; the six-row bank continues to provide all persons and explicit third-person gender/number agreement.

## Highlighting and preservation

Verified form segments and agreement remain untouched. A compound adverb is inserted after the auxiliary and before the highlighted participle; simple-tense adverbs follow the finite verb. Prefix lengths naturally shift the stored highlight offsets. English has no artificial conjugation span. No composable, layout size, typography, navigation, progress, button or rotation code is changed.

## Focused validation

`Pass19ExampleBaseline` is test-only frozen data used to compare all 1,140 French blocks, including text, subjects, complements and spans. `LearnPass20Test` audits all 570 English verb/tense pairs, same-situation comparisons, important contrast examples, and representative first/second/third-person agreement. The directly affected Pass 19 bilingual expectation test is updated for contextual English.

`LearnPass20UiTest` measures all 9,120 normal lines across language, pairing and orientation conditions, and checks representative portrait/landscape language switching, conjugations, spans, progress, navigation access, stable six-row text and refresh. Only the existing focused Learn navigation test is reused. Full stage/application regression is deferred.

Structural tests cannot certify naturalness. Context profiles and generated output receive AI-assisted editorial review; this is not independent human linguistic certification. Complete audit evidence is written to `app/build/reports/pass20/linguistic-audit.txt`.

References consulted for grammar, without copying their example sentences:

- [OQLF: imperfect readings](https://vitrinelinguistique.oqlf.gouv.qc.ca/24197/la-grammaire/le-verbe/temps-grammaticaux/passe/valeur-temporelle-de-limparfait)
- [OQLF: past historic](https://vitrinelinguistique.oqlf.gouv.qc.ca/24210/la-grammaire/le-verbe/temps-grammaticaux/passe/generalites-sur-le-passe-simple)
- [OQLF: perfect](https://vitrinelinguistique.oqlf.gouv.qc.ca/24218/la-grammaire/le-verbe/temps-grammaticaux/passe/generalites-sur-le-passe-compose)
- [British Council: present perfect versus finished past contexts](https://learnenglish.britishcouncil.org/free-resources/grammar/b1-b2/present-perfect)
