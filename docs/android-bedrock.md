# Xelo Client on Android Bedrock

This is the original `:app` Android application and UI. It is not the separate Companion. The application IDs remain `com.origin.launcher` (release) and `com.origin.launcher.beta` (debug).

**Device test target:** Minecraft Bedrock **26.62**, Android package version **`1.26.62.1`** ([official hotfix notes](https://feedback.minecraft.net/hc/en-us/articles/50159598275725-Minecraft-Bedrock-Edition-26-62-Hotfix-Changelog)). The app reads the installed version, not a list of downloads. Opening Minecraft or importing a pack does not prove in-game compatibility. Java Edition and previews are not test targets.

| Original feature | Status of this build |
| --- | --- |
| Home → Launch Minecraft | Opens the installed `com.mojang.minecraftpe` package through Android. No game binary is embedded or modified. |
| Dashboard → Modules → **Xelo Title**, **No pumpkin overlay**, **No spyglass overlay** | Generates a separate resource pack for each. The pack contents can be tested on the JVM. The effects still need to be verified in Minecraft on the target device. The title texture applies only to screens that use the classic title. |
| Dashboard → Modules → Touch Modules | Original entries and local layout preview remain. Quick Drop, AutoSprint, hotbar buttons, Zoom, FPS/CPS and the other touch modules **do not run** in the installed game in this build. |
| Other legacy modules: No Fog, Better Brightness, No Shadows, Custom CrossHair, etc. | Listed as unavailable, not shown as enabled. No legacy module configs are overwritten by the resource packs. |
| Dashboard → Resource installer | Can send a manually selected `.mcpack`, `.mcaddon`, `.mcworld` or `.mctemplate` to Minecraft with a temporary read grant. Minecraft reports import results. |
| Dashboard → Legacy Xelo files | Backup and options tools work on the client's own old data, not the game app's sandbox. Version isolation for the embedded runtime is disabled. |

The module texture paths and manifest format follow Mojang's [vanilla 26.60 resource pack sample](https://github.com/Mojang/bedrock-samples/tree/v1.26.60.5/resource_pack). The replacement textures are Xelo assets, not copies of the game's textures. The legacy runtime sources remain in `app/src/legacy`, `app/src/main/cpp`, `minecraft` and `minecraft-msftauth` and are not in the active APK. The build does not depend on the old preloader, the native submodules, the extracted game libraries or a store/integrity shim.

## Build and verify

Use JDK 21, the Android SDK command-line tools, Android Platform 36 and Build-Tools 36.0.0. `ANDROID_HOME` can point to the SDK, or set `sdk.dir` in your untracked `local.properties`. Gradle 8.13 is pinned in the wrapper. The active build does not require CMake or an NDK.

```sh
sdkmanager --install 'platforms;android-36' 'build-tools;36.0.0'
./gradlew :app:testDebugUnitTest :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`. CI workflow: **Build Xelo Client APK**, artifact **xelo-client-debug**. These build and package checks are not tests of Minecraft. Install only a signed APK from a trusted build. Debug APKs with a different signing key cannot replace an existing beta APK; keep a backup of your existing client data and do not uninstall Minecraft to install Xelo.

## Test the Hotbar shortcuts in Minecraft 26.62

1. On Xelo Home, check that the installed game version is `1.26.62.1`. Go to **Dashboard → Modules → Touch Modules**. Select **Hotbar 1**, **Hotbar 5** and **Hotbar 9**. A prior selection of a Hotbar slot on the same Xelo install carries over; legacy preview selections do not create controls in the pack.
2. Tap **Import Xelo Hotbar → Import**. Wait for Minecraft to confirm import. If it only opens the game or rejects the handoff, return to Xelo and choose **Save .mcpack** from the import dialog. Open that file in Minecraft from the Android Files app.
3. In Minecraft, go to **Settings → Global Resources → My Packs**. Activate **Xelo - Hotbar (1, 5, 9)**, with no other Xelo Hotbar pack active. If Minecraft reports a duplicate import, look for the already-imported pack. Disable competing HUD packs for this test and re-enter the world after changing packs.
4. In a local world with touch controls and the HUD visible, put different items into slots 1, 5 and 9. Check that only buttons **1**, **5** and **9** appear in the shortcut row above the normal hotbar. Tap them in a different order. A pass means the normal hotbar selection and the item in hand change to the matching slot on every tap, without placing/using an item or breaking a block. An import message or visible buttons without this effect are **not** a pass.
5. Disable the pack in Global Resources and re-enter the world. The shortcut buttons should be gone and vanilla controls should still work. To change slots, export the new selection and switch packs in Minecraft. Deselecting in Xelo does not change an active pack. The legacy preview editor does not set the positions or opacity of the in-game row.

The pack adds only a small `ui/hud_screen.json` modification, using `hud.root_panel`, `#hotbar_visible` and the `button.slot_1` … `button.slot_9` action names in Mojang's [26.60 HUD sample](https://github.com/Mojang/bedrock-samples/blob/v1.26.60.5/resource_pack/ui/hud_screen.json). It uses no behavior scripts, overlay permissions or game binary changes. JSON UI, touch modes, screen layout and interactions with other HUD packs can differ in the game; the unit tests cover pack contents and mapping, not input delivery in Minecraft. Report the game version, touch mode, and whether import, visibility and each tap worked. Start in a local world; server compatibility has not been verified.

## Test the visual packs in Minecraft

1. On a phone with the official game installed, open Xelo Home. Record the version it shows. Launch the game and return to Xelo.
2. Go to **Dashboard → Modules → No pumpkin overlay → Import**. In Minecraft, wait for its *import succeeded* message; opening the app is not enough.
3. In **Settings → Global Resources → My Packs**, activate **Xelo - No Pumpkin Overlay** above competing packs. In a local test world, wear a carved pumpkin in first person. Check that the dark mask disappears. Disable the pack and check that the mask returns.
4. Repeat for **No Spyglass Overlay** by looking through a spyglass in the same world. The zoom should still work; only the frame should disappear. For **Xelo Title**, check the classic start screen after enabling the pack (restart the game if the old screen is cached).
5. Record the game version, device/Android version, import result, whether each visible effect occurs, and whether disabling the pack reverses it. If the game rejects an import or a pack imports without changing the game, record that as a failure, not a successful port.
