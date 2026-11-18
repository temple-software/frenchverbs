# French Verbs / Verbes français

Offline Nice Education Android application with English and French interfaces.
Package: `com.templesoftware.frenchverbs`; version 1.0 (code 1).

## Features

Select tenses and verb categories, then practise with Learn, timed Repeat,
Conjugate and Use sentence quizzes. Saved selections support Welcome back.
The quizzes provide answer feedback and results. Portrait and landscape layouts
are supported. The offline dictionary contains 95 verbs and six tense categories.

## Build

Use the included Gradle wrapper, Android SDK 37 and a compatible JDK (the verified
local build uses Android Studio's bundled JBR). Configure the SDK location locally
with Android Studio or an ignored `local.properties` file.

```sh
./gradlew testDebugUnitTest assembleDebug lintDebug
```

Instrumented tests require a connected Android test device:

```sh
./gradlew connectedDebugAndroidTest
```

These are available commands, not a claim that tests ran during this Git checkpoint.
No generation scripts are needed: accepted resources and dictionary data are checked in.

## Release signing

Copy `keystore.properties.example` to the ignored `keystore.properties` and configure
an authorized external upload keystore privately. Never commit passwords or keys.
The normal `bundleRelease` task requires signing credentials and fails if missing.
The optimized signed version 1.0 candidate was verified separately in Pass 52,
including focused runtime checks on a separate 16 KB Android emulator.
Signed bundles, local reports and test screenshots are deliberately not in Git.
No Google Play upload or publication has occurred as part of this checkpoint.

## Project layout

- `app/src/main`: Kotlin application, offline dictionary and bilingual resources.
- `app/src/test`, `app/src/androidTest`: existing unit and Android tests.
- `docs`: dictionary sources/data and historical feature/QA documentation.
- `design`: original artwork and brand assets.

Historical pass documents describe their original stage, not necessarily current
behaviour. The checked-in application source is authoritative. The inherited
PolyForm Noncommercial license is preserved in `LICENSE`.

Guide: https://www.nice-education.com/french-verbs/
Privacy: https://www.nice-education.com/french-verbs/privacy/
More apps: https://www.nice-education.com/
