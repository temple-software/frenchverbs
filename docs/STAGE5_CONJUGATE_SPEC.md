# Stage 5 — Conjugate (Pass 31, initial implementation)

Status: implemented for owner visual inspection; NOT APPROVED. Stages 1–4 remain approved. Only focused Conjugate checks are authorized in this pass; full Stage 5 regression follows explicit owner approval.

## Entry, sequence and state

Activity Homepage → Conjugate and Full Repeat → Try yourself both start a Conjugate session using LearningSequence.create with the current selections and established previous-session random-choice rules. The sequence is built once per entry. Next question advances the retained list. The quiz has its own ConjugateViewModel/SavedStateHandle and does not mutate Learn or Repeat sessions, examples, progress or selection preferences.

Saved state contains the session, question index, six entries, locked rows, active row and remaining red-feedback time. Home/Android Back cancels quiz feedback and returns to Activity Homepage.

## Dictionary answer adapter

QuizQuestion reads OfflineDictionary; it does not generate French forms.

- Simple tenses in regular -er categories and regular -ir: fixed dictionary `before`, entered `marked`, fixed `after`.
- Simple irregular forms: enter the whole `before + marked + after` form, excluding the subject.
- Compound tenses: show the verified auxiliary as fixed text, enter the complete verified participle, and keep agreement notation fixed. This avoids punctuation keys and overly long auxiliary-plus-participle entry.
- Spelling-changing regular stems are taken from each dictionary row, never inferred by stripping suffixes.
- Subjects show je/j’ as recorded, il / elle and ils / elles. For the sixth être-compound row, the displayed feminine/plural notation is reused from that same conjugation's verified plural (nous) suffix; no new inflection is constructed. The canonical dictionary form remains available unchanged in QuizAnswer.text.

Every row gets the same number of boxes: the longest answer in that question. Trailing boxes remain empty and turn green with a completed row. The keypad uses required letters in first-occurrence order, then unique fillers from s, e, é, i, o, z, a, then alphabetic fallback if needed to fill nine positions. All required accented letters remain available. More than nine distinct letters produce extra three-column rows.

The adapter audit covers all 570 existing verb/tense combinations for reconstruction and answerability, not a fresh linguistic audit. The longest answers require 15 boxes; the largest keypad requires 13 letters.

## Interaction

Letters enter the current row without checking. Next (down-arrow row control), Enter, or selecting another unfinished row validates non-empty input. Empty rows skip without error and remain unfinished. Selecting the same or a completed row does nothing.

Correct entries lock green; unused trailing boxes turn green too. Next/Enter chooses the next unfinished row with wraparound, or a touched unfinished target is selected directly. Clear affects only the active unfinished entry.

Wrong non-empty input makes every box in the row red for 650 milliseconds of foreground feedback time. Letter keys, Clear, Enter, row Next and row switching are disabled, with model-level guards against repeated calls. At expiry, only that row resets to its locked hint prefix and the next box becomes active. The first mistake gives no hint; the second reveals the common prefix of the six entered answers plus one letter (or just the first letter if no common prefix). Every later mistake reveals one more letter, capped at the complete answer. Already displayed regular stems remain fixed. Revealed letters lock in dark orange and survive Clear, row changes and rotation; Clear removes only typed letters. Hint-only entries skip without a new error. A fully revealed answer completes the row automatically, retaining orange hint boxes. Mistake counts and pending reveals are saved with quiz state. Identical compound participles therefore complete on the second mistake. One lifecycle-bound frame clock drives the remaining duration; it pauses while stopped and is cancelled on exit. No blocking UI-thread delay or independent row timers.

After six completed answers, retain completed rows (green with dark orange hints), show a positive message and replace keypad/actions with Next question / Question suivante. The last question shows a simple finished message and Home / Accueil. No scoring, stars or Results screen.

## Presentation and responsive limits

Blue title bar with Home, bilingual title and completed-row progress percentage. Green infinitive (plus English meaning in English mode), blue French tense with English equivalent when applicable. White rounded answer panel, green keypad/actions, gold active-box border, green locked boxes and red incorrect boxes.

Portrait places the panel above the keypad; landscape places the panel left at full available height, with the verb/tense heading above the keypad in the right column. Compact landscape panel content is inset 16dp horizontally to clear the rounded corners. The active down-arrow sits directly after the final box (and any fixed agreement notation). System insets are respected. Fixed-text column widths are measured across all rows. Long forms can put fixed text above their boxes, and exceptionally long rows wrap their equal-count boxes to retain readable sizes. Very short screens, large accessibility text and expanded keypads use safe scrolling rather than losing controls.

Owner review is still needed for initial proportions, unusual long-form/compound questions and expanded keypads. This pass does not claim full Stage 5 regression or approval.


## Pass 33 — question selection before the approved quiz

The existing portrait/landscape quiz layout is visually accepted. These new selection screens are UNDER OWNER VISUAL REVIEW, not approved. No full Stage 5 regression is authorised yet.

- TOTALCONJ = 6.
- CONJSTARTLIST = ConjugateSelection.original, an immutable snapshot of ConjugateCandidate entries (LearningItem plus top-level selection-origin category). LearningSequence.create is called once by ConjugateViewModel.start on each explicit entry, using current saved preferences and the previous source sequence.
- CURRENTCONJ = ConjugateSelection.current. Before acceptance this derives from original plus independent tense/category filters; after acceptance it is the saved accepted list.
- CONJACCEPT = acceptable: a list of at most six; the zero-question case is separately routed to a safe empty state with Home/Back.
- CONJDONE = page DONE with a saved accepted list.
- CONJGO = the single transition into DONE in the model, which initialises QuizState with precisely that accepted list. No composable effect, secondary LearningSequence creation, duplicate launch or later resampling.

Initial routing: zero -> safe empty message; one to six -> quiz directly; more than six with multiple represented tenses -> Today; otherwise multiple represented categories -> Verb type; otherwise cap to six and enter quiz directly.

Today / Aujourd’hui: Concentrate on / Aujourd’hui, on travaille…; A tense / Un temps opens Tense; A verb type / Un type de verbe opens Verb type (or finishes immediately if only one category); Random / Au hasard accepts an ordered random subset of up to six.

Tense / Temps: heading lists distinct infinitives if CURRENTCONJ has fewer than five questions, otherwise spells the distinct verb count as a word with singular/plural grammar. Buttons use all original represented tenses and existing French/bilingual tense resources. Choosing a tense filters the immutable original: at most six starts quiz; one remaining category caps and starts; otherwise show Verb type.

Verb type / Type de verbe: heading describes currently represented tenses with existing bilingual rules. With multiple original tenses the heading returns to Tense, clearing abandoned filters and restoring every original tense. Display only represented top-level Important, Regular -er, Regular -ir and Irregular categories, never subfamilies. Category mapping uses selected Important membership first, then verified dictionary group metadata; never infinitive spelling. Choosing a category always finishes, capped at six if needed.

Sampling is without replacement by verb/tense identity; accepted entries retain their original relative order. No filler questions. Example IDs and generated verb choices remain unchanged. Back from a filtered Verb type returns to Tense; Back from directly chosen Verb type or Tense returns to Today when applicable; first-page Back/Home returns to Activity Homepage. Quiz Back/Home retains its established behaviour.

Original list, filters, page and accepted subset are stored in SavedStateHandle, alongside existing independent quiz state. Rotation/recomposition/background do not call start or resample. Home activity and Repeat Try yourself share this entry model. No persistent selection or language writes are added.

Layout: existing blue bar, Back/Home controls and green rounded buttons. Portrait uses a centred, compact column (maximum 420dp wide). Landscape uses three Today buttons, up to three tense columns (two on narrower screens), or two verb-type columns, maximum 1000dp. Safe insets and scrolling protect short screens and font scaling. All quiz layout source remains unchanged.

A debug-only ConjugateSelectionPreviewActivity provides an all-category/all-tense fixture for owner inspection without changing preferences. It is excluded from release and has no launcher icon. Explicit launch: adb shell am start -n com.templesoftware.frenchverbs/.ConjugateSelectionPreviewActivity (optional --es language en for English; default French). Home/Back closes the fixture. The normal app continues to use saved preferences. Remove this temporary review fixture when no longer needed.
