# Learn sentences — Pass 19

## Normal comparison

BilingualExamples contains 95 original sense-specific pairs: two different French complements and six curated British English tense readings for the first one. LearnExamples assembles verified French forms without modifying OfflineDictionary. English shows French plus its translation; French shows two French sentences in different persons. Paired panels preserve the same context while translating each panel's tense independently. Same-tense examples remain only in the second panel. Only French has green conjugation spans.

Imperfect readings are chosen per sense: ongoing (was watching), habitual (used to go), state (was hungry), perception (could see). Perfect and past historic have a completed past reading; pluperfect has past perfect. These are translations of these situations, not universal equivalences between tenses. Everything works offline.

## Situations and grammar

SentenceSituations binds each original situation to its verb, complete complement, theme, structure and optional circumstance. Prefixes cannot migrate to other complements. Pattern has explicit tense, person, gender and number masks. The current situations allow human speakers in all six indicative tenses and persons. Occupational examples describe plausible adult roles (prescribing, driving a bus, producing honey). There are no unrestricted noun/prefix/suffix cross-products.

Themes cover school, family, friends, animals, food, cooking, music, sports, travel, nature, weather, hobbies, art, reading, everyday life, home, holidays, shopping and helping. Meanings are bound: prendre food versus transport; mettre clothing versus setting a table; conduire vehicles versus leading a group. Short deictic alternatives support some long compound forms. Distinct situations increase substantially even though total candidates decrease.

Forms and offsets come from the verified dictionary. Explicit participle paradigms handle gender and number for aller, venir, partir, sortir and apparaître. Vous permits singular polite or plural reference; nous remains plural. Paraître has explicit adjective plurals and invariant prepositional predicates. No preceding direct objects, pronominal or passive constructions are generated, so avoir participles remain invariant. Subject elision is inherited from verified forms.

References consulted for grammar, not copied for sentence content:

- [Académie française](https://www.academie-francaise.fr/questions-de-langue): participle agreement, partitives and prepositions.
- [Larousse, venir](https://www.larousse.fr/conjugaison/francais/venir/9299): être auxiliary and compound agreement.

Structural tests inspect every emitted combination and normal bilingual block. Editorial review covers source situations and generated examples across all 95 verbs, six tenses, singular/plural and masculine/feminine subjects. This is AI-assisted review, not independent human linguistic certification. New situations need semantic review as well as structural checks.

## Selection, fitting and state

The alternate view remains six French sentences. Entry prepares choices; activation refreshes them. Recomposition, measurement, rotation and backgrounding do not randomise. Saved state holds mode, indices, exact-text history and bounded semantic history. Versioning discards incompatible old-bank indices safely. Entry preparation does not overwrite displayed history.

Real annotated text widths include bold spans, scaling, approved font sizes and the narrower rotated panel. Among fitting alternatives, selection excludes the last text and penalises recently used contexts, complements, shared vocabulary, themes, structures and introductions. Vocabulary token sets are cached. This is bounded repeat resistance, not an indefinite uniqueness guarantee. Below two fitting alternatives at extreme sizes, the shortest complete alternatives can wrap and scroll. No ellipses or tiny type are introduced.

The circular arrow, colours, typography, left origins, six landscape baselines, navigation and progress remain intact. Normal bilingual row heights measure the text actually rendered. At large accessibility sizes, complete text remains scrollable with navigation outside the scroll area.

## Focused evidence

LearnPass19Test validates structures, bilingual correspondence and a seeded 41,040-selection audit per version. BaselineSentenceBank is a test-only frozen Pass 18 fixture, excluded from the APK. SentenceBankTest retains its focused structural checks with updated context expectations. LearnPass19UiTest checks bilingual and French rendering, actual 20sp/300dp fitting across 3,420 combinations, 27,360 measured selections, toggle alignment, accessibility and restoration.

Reports: app/build/reports/pass19. Emulator evidence: app/build/pass19. Audits cover complements, noun-head heuristics, introductions, structures, themes, repeats and parc/billes/livre/vélo. Frequency evidence does not constitute linguistic certification. Full application and full Stage 3 regression remain deferred.
