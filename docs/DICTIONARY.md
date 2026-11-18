# Offline dictionary construction and evidence

Pass 01 contains **95 distinct verbs, 570 tense tables and 3,420 person forms**.
Every entry includes six indicative tenses, each with six rows. The app never downloads
learning content. `OfflineDictionary.kt` is the compiled immutable catalogue;
`dictionary-data.json` is an inspectable build-time mirror, not a runtime dependency.

## Method and independent validation

`scripts/build_dictionary.py` contains an original curated word list, English meanings,
morphological rules, explicit irregular stems, educational senses and sentence complements.
The script constructs annotated forms first, then checks them against each verb's Larousse
conjugation page. It does **not** populate the dictionary by copying the reference tables.
Emission is refused if even one tense/person comparison fails. The exact resolved URLs and
per-entry results are recorded in `dictionary-verification.json`.

Verification selects the first (indicative) reference heading for each required tense and
compares the corresponding person. It accepts published alternatives, normalizes spacing and
apostrophes, and compares the masculine forms of the optional agreement notation. It checks
the intransitive entry for sortir, not Larousse's first, transitive entry. Local HTML cache
is ignored under `app/build/dictionary-reference`; it is not an app asset or source deliverable.

The independent comparison caught and corrected draft spelling/stem errors before emission.
Unit fixtures separately record important forms including `nous vînmes`, `nous mangeons`,
`nous commençons`, `j'appelle`, `j'enverrai`, `nous connaissions`, `nous avons relu`, and
être-auxiliary agreement. Structural tests cover every entry, Unicode normalization,
all six persons/tenses, examples, classification and highlight boundaries.

## Sources and rights

- [French Ministry of Education / Éduscol frequency list](https://eduscol.education.fr/document/15661/download),
  consulted 29 October 2026, especially the verb list on pages 10–14. It informed the emphasis
  on familiar verbs; the supplied category families and educational usefulness determine the
  final selection. No corpus or frequency table is incorporated. `usefulnessRank` is an
  editorial ordinal within the catalogue, **not** a claimed corpus frequency statistic.
- [Larousse conjugator](https://www.larousse.fr/conjugaison/francais/): independently published
  factual reference forms for every entry. Per-verb URLs are in the evidence and each entry.
  Copyright remains with Larousse. No publisher definitions, explanations, examples, page
  designs or downloadable lexical database are redistributed. The app's morphology, meanings,
  examples and annotations are original. Source attribution is not a claim of a Larousse licence
  or endorsement.
- [Académie française: prendre](https://www.dictionnaire-academie.fr/conjuguer/A9P4070),
  additional cross-check of the prendre paradigm.
- [Académie française: questions de langue, participe passé](https://www.academie-francaise.fr/questions-de-langue),
  authority for agreement with être and avoir. The app's non-pronominal être forms agree with
  the subject. Avoir examples have no preceding direct object.
- [Lexique](https://www.lexique.org/) was reviewed as a possible frequency source. Its current
  stated licence is CC BY-SA 4.0. **No Lexique data is incorporated or derived into this list**;
  consequently no Lexique dataset or share-alike material is distributed. If imported in a
  future pass, its attribution and share-alike obligations must be applied to that material.

Project code and original content inherit the repository licence. This documentation does
not license third-party publisher content. The local reference cache must stay excluded.

## Coverage

| Category | Verbs | Person forms |
|---|---:|---:|
| Important | 10 | 360 |
| Standard -er | 5 | 180 |
| -ger | 5 | 180 |
| -cer | 5 | 180 |
| -yer | 3 | 108 |
| e → è | 5 | 180 |
| é → è | 5 | 180 |
| Double consonant | 4 | 144 |
| Special future | 2 | 72 |
| Regular second-group -ir | 7 | 252 |
| Partir family | 6 | 216 |
| Mettre family | 5 | 180 |
| Connaître family | 4 | 144 |
| Écrire family | 5 | 180 |
| Lire family | 4 | 144 |
| Ouvrir family | 5 | 180 |
| Recevoir family | 5 | 180 |
| Conduire family | 5 | 180 |
| Peindre family | 5 | 180 |
| **Unique total** | **95** | **3,420** |

The -yer pool uses nettoyer/essuyer/appuyer, avoiding extra optional -ayer orthographies
in the first learning pass. Double consonant uses appeler/rappeler/jeter/rejeter rather
than obscure verbs or verbs whose modern spelling normally uses è. Special future uses
envoyer/renvoyer. The connaître and lire pools contain the four requested useful members.
Other pools meet or exceed five. Aller is Important, never regular -er. All seven regular
-ir verbs exhibit -issons; partir/dormir/sortir/venir are excluded from that group.
Protéger is classified with é→è and additionally implements its -ger orthography.

## Grammar policy and representation

- Traditional accented infinitives and future forms are used consistently (connaître,
  préférerai). Published reformed alternatives are accepted references but not mixed into
  a displayed paradigm.
- Sortir means to go out, with être. Partir/aller/venir also use être. Apparaître uses the
  accepted être construction. Paraître means to seem, with avoir. No pronominal/passive
  paradigms or preceding-direct-object agreement exercises are included in this pass.
- Rows are je/j', tu, il/elle, nous, vous, ils. For être compounds: `(e)` marks feminine;
  `nous ... (e)s` marks plural; `vous ... (e)(s)` permits polite singular or plural;
  `ils` takes masculine plural. The explanatory key is shown on highlighted compound panels.
- `Form(subject, before, marked, after)` provides bounded annotations. The renderer does
  no morphological slicing. Compound highlighting marks the participle, keeping auxiliary
  and agreement visible. Simple forms mark endings or a specified irregular portion.
- No dictionary entry is incomplete or marked as awaiting conjugation verification.
  Verification is reference-based automated comparison, **not** an independent human
  linguist certification. Editorial review of example quality and pedagogical choices
  remains a sensible pre-release review.

## Original examples

There are **two fixed French examples for every verb/tense: 1,140 sentences**. They are
compiled from manually curated sense-specific complements and tense contexts using the
verified nous form. They are original, with no copied publisher examples. Examples with
être use masculine/mixed plural consistently. Past historic examples use narrative contexts;
pluperfect examples name a prior reference point. Selection of the example index occurs
once when building the session, and survives rotation. The app never generates sentences.

These are intentionally simple repeated-context examples rather than 1,140 individually
commissioned narratives. Their forms are checked; no external language professional has
independently certified every sentence. Examples can be expanded below each distinct tense
panel so compact screens prioritize the six conjugation rows.

## Reproduce

Run `python -X utf8 scripts/build_dictionary.py --verify` with network access (or the local
reference cache). Only the Python standard library is needed. The Android build does not
run this script or require network access for dictionary content. Run JVM tests after any
data change. Do not bypass the reference gate or include reference HTML in source control.
