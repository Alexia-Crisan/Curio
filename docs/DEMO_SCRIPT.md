# Ten-Minute Demo Script

## 1. Before the Demo (5 min)
1. Install the debug build of Curio on the demo device.
2. Complete onboarding by selecting an available theme and Guided mode.
3. Run the following adb command to apply the "almost_there" preset:
   `adb shell am start -n com.example.curio/com.example.curio.debug.DebugDemoActivity --es preset almost_there`
4. Open Curio and verify that the Progress tab shows 3 complete days and 24 completed discoveries.
5. Turn on Airplane Mode. Ensure Battery Saver is off and system animations are enabled.
6. Note: Fresh-first ordering only shows on a revisit, so the `almost_there` preset (which never revisits today's topic) shows dataset order; this is expected behavior.

## 2. Scene 1: Daily Feed & Personal Notes (2 min)
1. Open the Feed tab and open any card detail view.
2. Tap "Adaugă o notiță" (Add a note), type a reflection in the dialog, and press "Salvează" (Save).
3. Observe the note appearing under the "Notița ta" (Your note) heading on the Detail screen and in the Library tab under the card title.
4. Tap "Editează notița" (Edit note), modify or clear the text (leaving empty to remove), and save.
5. Optional: open Android Studio App Inspection -> `curio.db` -> `saved_discovery` table to watch the `note` column update live.

## 3. Scene 2: Achievement Unlock (2 min)
1. Return to the Feed tab and view the remaining cards to complete today's topic (card 25 total).
2. Observe the "Curious Mind" unlock banner appearing at the top of the screen.
3. Tap the banner or navigate to the Badges screen.
4. Show the unlocked "Curious Mind" badge and the progress toward other badges like "Topic Explorer".

## 4. Scene 3: Shake Bonus Discovery (1 min)
1. Return to the Feed tab.
2. Shake the device (or tap the debug shake button).
3. Observe a 7th bonus card appearing at the end of today's feed with a "Revisit" badge if previously viewed.
4. Show that completing the bonus card does not inflate daily completed day counts.

## 5. Scene 4: Knowledge Tree & Art Theme (2 min)
1. Open the Progress tab and tap "Open tree" on the Knowledge Tree card.
2. Tap a branch node to inspect its explanation sheet (showing progress, state, and next steps).
3. Navigate to Settings, tap "Tema" (Theme), and switch to the Art theme to explore curated masterpieces.

## 6. Scene 5: Trust and Privacy (1 min)
1. Open the Settings tab.
2. Tap "Sources and licences" to view attributions for all content cards.
3. Tap "Reset progress" to show the confirmation dialog explaining that all progress will be reset.
4. Tap "Cancel" to dismiss the dialog without losing data.

## 7. After the Demo
1. Reset the demo state using:
   `adb shell am start -n com.example.curio/com.example.curio.debug.DebugDemoActivity --es preset fresh`
2. Available adb commands for demo presets:
   - Fresh state: `adb shell am start -n com.example.curio/com.example.curio.debug.DebugDemoActivity --es preset fresh`
   - Almost there state: `adb shell am start -n com.example.curio/com.example.curio.debug.DebugDemoActivity --es preset almost_there`
   - Two themes state: `adb shell am start -n com.example.curio/com.example.curio.debug.DebugDemoActivity --es preset two_themes`
3. If the physical shake sensor fails on an emulator, use the debug shake action in the UI.
