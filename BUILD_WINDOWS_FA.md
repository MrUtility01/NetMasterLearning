# NetMaster V11 — Build Notes

## Windows
Use `gradlew.bat test` and `gradlew.bat assembleDebug` from the project root.

The project targets Android API 35, minSdk 26 and Kotlin JVM 17. A local Gradle distribution and Android SDK are required for a build.

## Web/PWA
Open `web/index.html` through a static web server. The PWA uses offline-cacheable JSON assets and requires no mandatory external LLM.

## V11 validation
- Web JavaScript syntax: `node --check web/app.js`
- JSON assets: parse with any standard JSON validator
- ZIP integrity should be checked with `unzip -t` after release packaging.
