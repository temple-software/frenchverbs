# Learn random sentences — Pass 18 (historical)

**Current behaviour: [Pass 20](LEARN_SENTENCES_PASS20.md), building on [Pass 19](LEARN_SENTENCES_PASS19.md).** The old context cross-product, normal French-only blocks and selection policy below describe the prior implementation.

The verified OfflineDictionary and the two approved LearnExamples comparison sentences remain unchanged. SentenceBank is a separate offline bank for the optional six-sentence view. No network API or permission was added.

## Content and grammar

Each verb has an explicit list of compatible complements, covering transitives, destinations/sources, state/location constructions, modal + infinitive, and prepositional complements. These original phrases were authored for this feature rather than copied from reference examples. The bank never mixes arbitrary objects across unrelated verbs. Short alternatives are curated too: e.g. appuyer ici is a valid instruction-like use, whereas modal pouvoir uses an infinitive.

Forms and highlight offsets come directly from each verified Form. Third-person subjects are selected from explicit il/elle and ils/elles alternatives. Elision for je/j’ is retained from the verified form. Avoir participles are invariant in these constructions because every direct object follows the verb; the bank has no preceding direct object, passive or pronominal construction.

The five être-auxiliary verbs (aller, venir, partir, sortir and apparaître) have explicit four-form participle paradigms. Gender and referential number determine the form; the conjugated auxiliary still uses the requested grammatical person. Vous can refer to one person politely or multiple people. Nous remains plural. Paraître uses explicit plural forms for its three adjective complements. Other objects may be singular or plural without changing the subject's person.

Tense contexts are separate structured prefix/suffix slots. Present includes ordinary statements and habitual contexts; imperfect uses earlier habitual frames; future uses tomorrow/soon; passé composé uses yesterday/this morning; passé simple uses narrative sequencing (Un jour, Puis, Alors). Plus-que-parfait places an already completed action before a meal or at a past return/waking/dinner reference point. These short nominal event references intentionally avoid lengthy subordinate clauses. Grandir has separately curated annual/age/time-passing contexts instead of mechanically claiming growth in a single morning.

References consulted for grammar and existing-form spot checks:
- https://www.academie-francaise.fr/questions-de-langue — participle agreement and polite singular vous.
- https://www.larousse.fr/conjugaison/francais/venir/9299 — venir uses être; person and compound-form agreement.
- https://www.larousse.fr/dictionnaires/francais/plus-que-parfait_plus-que-parfaits/61817 — completed aspect relative to an imperfect past reference.

This is structured validation plus author review, not certification of every sentence by a French linguist. Exhaustive tests compare all combinations and candidate forms to the verified dictionary, validate complete Unicode strings and exact highlight spans, and check explicit agreement metadata. Representative output is written to app/build/reports/pass18-bank.txt for naturalness review.

## Selection and persistence

LearnExampleState is saved through rememberSaveable and a JSON Saver: current item key, mode, six candidate IDs and recent displayed sentences. Entry prepares choices and resets Comparison. Activating Examples selects six fresh choices; leaving Examples does not change the saved set. Entry preparation does not overwrite displayed-history entries. Recomposition, measurement and restoration do not call random selection. Rotation keeps the IDs and mode.

SentenceSelection compares real TextMeasurer widths, including bold spans and font scaling, against available padded width and a conservative rotated-panel width. It excludes the last displayed text for each verb/tense/person. If fewer than two alternatives fit at an extreme size, the shortest alternatives remain available with complete wrapping/scrolling rather than ellipses or tiny type. A finite bank can repeat later; there is no indefinite non-repetition guarantee.

## Presentation

A 32 dp circle within a 48 dp touch target sits inside the first panel at bottom right, in both modes. Right/left arrows are used in landscape, down/up in portrait, with English/French accessibility descriptions. A reserved right-hand lane prevents the control overlapping first-panel text. Header, bottom navigation, learning sequence and progress code are unchanged.

LearnLandscapePanels extends Pass 17's measured shared row geometry. Both normal and example row metrics are accounted for before placement, including baseline ascent/descent. The first panel stays fixed when toggling. At large fonts the tallest bank rows are reserved to avoid geometry changes on refresh. Examples mode places only six sentences in the second card, uses the same left inset as the conjugations being replaced, left-aligns sentences and aligns their baselines with the primary conjugations. Hidden normal-mode top/footer slots reserve geometry but are not placed or exposed as visible controls. Both cards retain their colour and equal height.

Following owner review, random sentences now match the conjugation typography: 17 sp on compact landscape, 18 sp on taller landscape, and 19/20 sp in compact/taller portrait. Landscape uses the exact same text style and left origin as the replaced conjugation rows. The toggle retains its arrow and 48 dp target, with the home information button's 32 dp pale-blue circle, fine border and dark-blue ink. The original Pass 18 report's centred-group and smaller-text descriptions are superseded by this correction.

Selection measures the updated text sizes. Long sentences at unusually narrow widths or large accessibility settings retain complete text through wrapping/scrolling. The original exhaustive 300 dp width result was measured at the former 16 sp style and is historical; the correction's focused rendering checks compare actual replacement-row origins and sizes in both languages and orientations.

Only feature-focused tests are run: SentenceBankTest and selected LearnPass18Test methods. Full Stage 3 regression remains deferred until owner approval.

