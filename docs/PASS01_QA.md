# Pass 01 QA evidence

Commands and final outcomes are recorded in the requested external report. Ignored build
evidence lives in `app/build/`; screenshots captured by instrumentation are copied there
from the emulator's application-specific external files directory.

The product layout test covers 320×568, 360×640, 360×800, 384×854, 412×915, 600×960 and
800×1280 dp, their landscape inverses, English/French and 100%, 130%, 200% fonts.
It measures actual glyph lines, word breaks, ellipsis/height overflow, row reachability,
and pinned Next actions. The inherited About test additionally exercises 115% and 150%.
Simulated-density matrix tests supplement real portrait/landscape emulator captures;
they are not physical-device tests.

Real app tests exercise first-run selections, disabled Next, language selection, the
default setup path, activity home destinations, final Repeat, recreation and rotation.
Domain tests cover all type combinations, hidden-category retention, ordering, uniqueness,
random alternatives, empty choices, and session serialization. DataStore tests reopen the
file to check saved empty sets and hidden choices. Session ViewModel tests restore the
SavedStateHandle snapshot and prove the final item cannot be advanced past the end.

No physical Samsung device, human TalkBack walkthrough, or signed release was tested.
Final launcher approval, guide/privacy pages and release QA are deferred.

## Executed outcome

28 JVM tests passed. Debug APK/test APK builds and lint passed (0 errors, 30 warnings).
The corrected complete 10-test instrumentation suite passed. After visual fixes, final
connected flow/restoration tests (2) and layout tests (2, including the full 1,008-case
matrix and unclipped panel-frame checks) passed. The inherited About matrix covered 140 cases.
Three-button navigation plus a corner cutout passed the bilingual safe-area/glyph test;
original emulator settings were restored. Actual system-dark Learn was inspected.
Final English/French portrait/landscape and forced-dark screenshots: `app/build/pass01/review`.
Actual system-dark screenshot: `app/build/pass01/actual-system-dark-learn.png`.
Original bootstrap tests: 16 passed, 1 Windows real-symlink privilege skip (17 total).
