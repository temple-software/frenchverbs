# Stage 4 — Repeat: approved implementation

The owner explicitly approved Stage 4 after Pass 29. Pass 30 full Stage 4 regression PASSED on 6 November 2026 (21 distinct functional tests). Stage 4 is complete and approved; the report at E:/CodexResults/FrenchVerbs/codex_frenchverbs_30.txt records its actual outcome. Stages 1–3 remain approved. Stage 5 is not implemented; Conjugate and Use remain placeholders.

## Entry and sequence

Full Repeat opens from the Activity Homepage. RepeatViewModel calls the approved LearningSequence.create once per entry, preserving selected important verbs, regular categories, irregular families, tenses, ordering, exclusions and fresh random category choices. Each chosen verb is reused across its selected tenses. Manual Next retains the same sequence and advances one item.

Single Repeat opens from Learn with exactly the currently displayed LearningItem: one verb and one tense. It uses an independent RepeatViewModel and returns automatically to the same Learn navigation entry after completion. Learn's session, index, progress, comparison panels and saveable example selection are preserved.

## Approved layout and language

The menu title is Repeat / Répéter. A prominent green French verb appears beneath it, with the English meaning in English mode. The blue French tense follows, including the English equivalent where appropriate. The countdown is horizontally centred at the measured centre of the third (il/elle) row; it does not move the conjugation rows.

The complete six-row block and bottom action space are reserved from the first frame. Rows share the left edge of a horizontally centred block measured from the exact styled dictionary strings. Portrait uses the approved spacing and 25sp phone / 30sp tablet forms. Landscape uses the approved larger typography, with compact measurement choosing 21/20/19sp where necessary. Safe Android insets keep navigation clear. Standard supported sizes fit without unnecessary scrolling; very short landscapes and increased accessibility scaling retain a safe scroll fallback.

No scaling, bouncing, movement, or other added animation is used.

## Central timing and introduction

RepeatTiming is the sole configuration source. Default milliseconds:

- Initial verb/tense hold: 1000.
- Instruction fade-in and each dim/brighten leg: 500.
- Fully bright holds for 3, 2 and 1: 750 each.
- Subject fade-in: 250.
- Fully visible subject before verb: 1000.
- Verb fade-in: 250.
- Pause before next subject: 1000.
- Single Repeat completed-form hold: 2000.
- Final item fade-out: 350.

The instruction says “Repeat out loud ... 3/2/1” or “Répète à voix haute ... 3/2/1”. Introduction opacity follows raised-cosine easing: (1 - cos(pi * progress)) / 2, clamped to the phase. Intermediate pulses range from configurable pulseMinimumAlpha=0.25 to 1.0. Initial entry starts at zero; final exit ends at zero. Number changes occur at the dimmest point, with continuous opacity and zero endpoint velocity.

The green verb dims and brightens with the 3-to-2 transition. The blue tense dims and brightens with 2-to-1. The final instruction exit lasts 500ms: it starts 250ms before the first subject, then overlaps that subject's existing 250ms fade. This avoids a blank interval without changing subsequent reveal timing.

Default first subject begins at 6000ms of active animation time. Sixth form finishes at 20000ms. Single Repeat finishes at 22350ms. These totals are derived from RepeatTiming rather than separate UI constants.

## Conjugation reveal and completion

For each of the six persons, reveal the subject, pause, reveal the complete conjugated form, pause, then begin the next subject. Completed lines remain visible. Use the verified dictionary's exact subject/before/marked/after fields; preserve je/j’ elision, auxiliaries, participles, agreement and masculine/feminine and singular/plural forms. Do not generate conjugations independently.

Full Repeat enters genuine untimed WAITING immediately when the sixth form finishes fading in. There is no extra completion hold before Next appears. The entire conjugation remains visible indefinitely. Next identifies the upcoming verb and tense in natural English/French with a right arrow, adapting long labels to width. It accepts one press, disables during the 350ms transition, and advances exactly once without rebuilding the session.

The final Full Repeat item stays visible with “Try yourself” / “À ton tour !”. The action opens the existing Conjugate placeholder. There is no automatic loop.

Single Repeat retains its two-second completed-conjugation hold, then the 350ms fade and automatic return to its originating Learn item. It has no completion action.

## Lifecycle and cancellation

RepeatViewModel saves session, index, elapsed animation time, mode, completion and playback state. RepeatClock is one frame-driven effect, active only while the destination is RESUMED. It resets its frame origin after lifecycle gaps, so stopped time does not accumulate. A stalled frame is capped at 100ms of progress to avoid skipping teaching phases.

Rotation must preserve the current model/item/phase, not restart the introduction or duplicate clocks. WAITING consumes no time. Next is accepted only once from WAITING when an upcoming item exists.

Home cancels and returns to Activity Homepage. Android Back from Single Repeat cancels and returns to Learn; Back from Full Repeat cancels and returns to Activity Homepage. Cancellation is distinct from successful Single completion, preventing a delayed automatic return. Cancelled playback does not continue in the background.

## Accepted limitations and scope

The Stage 3 example-display latency remains owner-accepted and unchanged. Very short screens and increased accessibility font scaling can require scrolling. Visual smoothness depends on device rendering; regression tests are not a frame-rate benchmark. Pass 30 covers Stage 4 only, with adjacent Learn navigation checks solely to verify Single Repeat return preservation. No full Stage 1/2/3 or application-wide suites are authorized or claimed. No Stage 5 implementation, release, commit, push or publication is part of this pass.
