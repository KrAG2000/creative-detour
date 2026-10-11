# Contributing to Creative Detour

Thanks for helping people spend a little less time on their screens! Bug reports, ideas, docs and code are all welcome.

## Before you start

- **Small fixes** (typos, obvious bugs): open a pull request directly.
- **New features or bigger changes:** open an issue first, so we can agree on the idea before you spend time on it.
- **Security problems:** don't open a public issue. See [Reporting a vulnerability](#reporting-a-vulnerability).

## Set up

You need:

- **Android Studio** (recent version; the project uses AGP 9 and Kotlin 2.4)
- **JDK 21.** The JetBrains Runtime bundled with Android Studio works. If Gradle complains about the Java version, set *Settings → Build Tools → Gradle → Gradle JDK* to it.
- **Android SDK 36**
- **A real Android phone** is strongly recommended. Emulators have no step counter, and usage stats are hard to test on them.

```bash
git clone https://github.com/KrAG2000/creative-detour.git
cd creative-detour
```

Open the folder in Android Studio, wait for the Gradle sync, and press **Run ▶**.

### Getting the AI model while developing

The model is **never committed** (`*.litertlm` is in `.gitignore`). Either:

- download it in the app (*Settings → AI model*), or
- push a copy you already have (debug builds only):

```bash
adb push gemma-4-E2B-it.litertlm /data/local/tmp/
adb shell run-as com.project.creativedetour mkdir -p files/models
adb shell run-as com.project.creativedetour cp /data/local/tmp/gemma-4-E2B-it.litertlm files/models/
adb shell rm /data/local/tmp/gemma-4-E2B-it.litertlm
```

### Useful commands

```bash
./gradlew assembleDebug          # build the APK
./gradlew testDebugUnitTest      # unit tests
./gradlew lintDebug -Dorg.gradle.jvmargs="-Xmx4g"   # lint needs extra memory
```

## Project layout

All code lives in `app/src/main/java/com/project/creativedetour/`:

| Package | What's inside |
|---|---|
| `sensing/` | Reads steps, screen time and calendar; permission checks |
| `engine/` | Decides *when* to nudge (`NudgeEngine`) and *what* to suggest (`ActivityPicker`). Pure Kotlin, no Android |
| `ai/` | Writes the nudge: Gemma via LiteRT-LM, template fallback, model download, device capability test |
| `notify/` | Notifications and their buttons |
| `data/` | Room database for nudges and answers |
| `service/` | The background service and boot receiver |
| `ui/` | Compose screens: Today, Activity, Settings, onboarding |

### Common changes

- **New activity type:** add an entry to `engine/Activity.kt`, and an emoji in `ui/Format.kt`.
- **New "not now" reason:** add it to `engine/RejectReason.kt`.
- **New nudge rule:** change `engine/NudgeEngine.kt` and add a test in `NudgeEngineTest`.
- **New model:** add a `ModelSpec` to `ai/ModelCatalog.kt` with its URL, size and SHA-256. On Hugging Face, the `x-linked-etag` response header of the file URL is its SHA-256.

> ⚠️ `Activity` and `RejectReason` are stored in the database **by name**. Never rename an entry; change its label instead.

> ⚠️ **Database changes** (new column or table) need a version bump in `DetourDatabase` *and* a proper Room `Migration`, so users keep their history. Don't rely on destructive migration.

## Code style

- Follow the [Kotlin coding conventions](https://kotlinlang.org/docs/coding-conventions.html) and match the code around you.
- Keep classes small and focused. Comments explain **why**, not what.
- Logic that decides things belongs in `engine/` with unit tests, not in UI or service code.
- Heavy work (model, database, file I/O) never runs on the main thread.

## Privacy rules (non-negotiable)

Detour's promise is that **nothing leaves the phone**. Every change must keep it:

- No analytics, telemetry, crash reporting services or ads.
- No new network calls. The only one allowed is downloading a model the user asked for.
- Read the minimum. For example, screen time uses screen on/off events only, never which apps were used.
- Every new permission needs a disclosure card in `ui/settings/PermissionCards.kt` explaining what and why, plus a justification in your pull request.

## Security best practices

- **Never commit secrets:** keystores (`*.jks`, `*.keystore`), `keystore.properties`, `local.properties`, API tokens. Keep signing keys outside the repository.
- **Dependencies** come only from Google Maven or Maven Central, are declared in `gradle/libs.versions.toml`, and stay reasonably up to date. Explain any new dependency in your PR.
- **Downloads are verified.** Every model has a SHA-256 in `ModelCatalog` and is checked before use. Keep it that way.
- **Components stay private.** Use `android:exported="false"` unless the system must reach the component (like `BootReceiver`), and say why in a comment.
- **Use explicit intents** and `PendingIntent.FLAG_IMMUTABLE`.
- **Don't log personal data:** no calendar titles, nudge history or reasons in `Log` calls.
- **Validate everything from outside:** intent extras, files and model output.

## Reporting a vulnerability

Please **don't open a public issue**. Report it privately through the repository's **Security** tab → **Report a vulnerability**, and include steps to reproduce. You'll get a reply as soon as possible.

## Pull requests

1. Create a branch from `main`, e.g. `fix/reason-dialog` or `feature/water-reminders`.
2. Keep the PR focused on one thing.
3. Before you open it:
   - [ ] `./gradlew testDebugUnitTest` passes
   - [ ] `./gradlew lintDebug` shows no new errors
   - [ ] You tested on a real phone. Mention the model and Android version.
   - [ ] UI changes include a screenshot
   - [ ] README is updated if behaviour changed
4. In the description, explain **what** changed, **why**, and **how you tested it**.

If your change touches notifications or the background service, also check: a nudge arrives, all buttons work (*Let's go*, *Not now* → reason, *More…*, swipe away), and the service comes back after a reboot.

**AI-assisted contributions are welcome.** You're responsible for understanding and testing every line you submit.

## License

By contributing, you agree that your contributions are licensed under the project's [MIT License](LICENSE.md).
