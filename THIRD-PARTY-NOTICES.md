# Third-Party Notices

DevInfo is distributed under the MIT License (see [LICENSE](LICENSE)). It builds on the
following third-party components. This file records their licenses so that the attribution
requirements of those licenses are met when an APK or a source archive is redistributed.

## Runtime dependencies (bundled into the APK)

| Component | License | Project |
| --- | --- | --- |
| AndroidX Core, Activity, Lifecycle, DataStore, Core Splashscreen | Apache-2.0 | https://developer.android.com/jetpack/androidx |
| Jetpack Compose (UI, Material 3, Material Icons) | Apache-2.0 | https://developer.android.com/jetpack/compose |
| Kotlin standard library, kotlinx-coroutines | Apache-2.0 | https://github.com/JetBrains/kotlin |
| Backdrop (`io.github.kyant0:backdrop`) | Apache-2.0 | https://github.com/Kyant0/AndroidLiquidGlass |

No runtime dependency is licensed under GPL, LGPL, or AGPL.

## Build and test only (not bundled into the APK)

| Component | License | Scope |
| --- | --- | --- |
| JUnit 4 | EPL-1.0 | `testImplementation` |
| AndroidX Test (ext-junit, espresso-core) | Apache-2.0 | `androidTestImplementation` |
| Android Gradle Plugin, Kotlin Gradle Plugin | Apache-2.0 | Build time |
| ktlint Gradle plugin | Apache-2.0 | Build time |

## Apache-2.0 compliance

The Apache License 2.0 requires that the copyright notice and, where present, the `NOTICE`
file of a redistributed work be preserved. The components above are consumed as unmodified
binary artifacts from Google Maven and Maven Central; no `NOTICE` file from those artifacts
is modified by this project. Their license text is available at
https://www.apache.org/licenses/LICENSE-2.0.

## Acknowledgements

The Material 3 component styling in this project was designed after the visual language of
the KernelSU manager interface. Earlier releases (v1.3.4 through v1.4.3) contained code
adapted from [KernelSU-Style-UI-Kit](https://github.com/chenaizhang/KernelSU-Style-UI-Kit)
under GPL-3.0; those portions were replaced with independent implementations before the
project returned to the MIT License. No GPL-licensed code remains in the current tree.
