# How to Get and Install MACRIC.apk

Follow this beginner-friendly step-by-step guide to get your real, installable `MACRIC.apk` and run it on your PC Android emulator (**BlueStacks 5**, **LDPlayer 9**, **MSI App Player**) without installing Android Studio or building anything by hand.

---

## Step-by-Step Instructions

1. **Download the ZIP from AI Studio**
   - In Google AI Studio, click the project menu / settings icon in the top corner and select **Export as ZIP** (or Download Project).
   - Save the ZIP file to your computer (e.g., your `Downloads` or `Desktop` folder).

2. **Extract it**
   - Right-click the downloaded ZIP file and choose **Extract All...** (Windows) or double-click to uncompress it.
   - Make sure you open the extracted folder where you see files like `build.gradle.kts`, `app`, and `.github`.

3. **Create a free GitHub account and a new repository**
   - Go to [github.com](https://github.com) and sign up for a free account if you don't already have one.
   - Click the **+** (plus icon) at the top right and select **New repository**.
   - Enter a name (for example, `MACRIC`) and keep it **Public** (recommended so GitHub Actions runs with free unlimited minutes).
   - Click **Create repository**.

4. **Upload ALL files including the `.github` folder**
   - On your new empty repository page, click **uploading an existing file**.
   - Drag and drop all extracted project files and folders into GitHub's file uploader.
   - Click **Commit changes**.
   - *Note on `.github`*: Some web browsers hide files or folders starting with a dot (`.`). If the `.github` folder does not upload, do this:
     1. On GitHub, click **Add file > Create new file**.
     2. Type `.github/workflows/build-apk.yml` in the filename box.
     3. Copy and paste the entire contents of `.github/workflows/build-apk.yml` into the editor.
     4. Click **Commit changes**.

5. **Open the Actions tab**
   - In your GitHub repository, click the **Actions** tab at the top.
   - If prompted with *"Workflows aren't being run on this repository"*, click **I understand my workflows, go ahead and enable them**.

6. **Run "Build Android APK"**
   - In the left sidebar under *Workflows*, click **Build Android APK** (or `Build Android APK`).
   - Click the **Run workflow** dropdown button on the right side.
   - Leave the branch as `main` and click the green **Run workflow** button.

7. **Wait**
   - The workflow will show a yellow spinning icon indicating that it is compiling your APK in the cloud.
   - Wait approximately 2 to 3 minutes until a green checkmark appears.

8. **Download the `MACRIC-APK` artifact**
   - Click on the completed workflow run (e.g., *"Build Android APK"*).
   - Scroll down to the **Artifacts** section at the bottom of the summary page.
   - Click on **MACRIC-APK** to download the zip file (`MACRIC-APK.zip`).

9. **Extract it**
   - Right-click `MACRIC-APK.zip` on your PC and click **Extract All...**.
   - Inside you will find the real installer file: `MACRIC.apk` and its verification checksum `MACRIC.apk.sha256`.

10. **Drag `MACRIC.apk` into the emulator**
    - Launch your PC Android emulator (**BlueStacks 5**, **LDPlayer 9**, or **MSI App Player**).
    - Drag and drop `MACRIC.apk` directly into the emulator window.
    - Alternatively, click the emulator's sidebar button **Install APK** (or press `Ctrl+Shift+B` in BlueStacks) and select `MACRIC.apk`.
    - Wait a few seconds for the MACRIC icon to appear on your emulator home screen.

11. **Open MACRIC, tap HUD ON and allow "Display over other apps"**
    - Tap the **MACRIC** app icon to open it.
    - Tap **HUD ON** in the header or home dashboard.
    - When the Android permission screen opens, toggle **Allow display over other apps** (or "Draw over other apps") to **ON**.
    - Switch back to MACRIC. Your floating gaming reticle will now appear on screen, ready to overlay your game!

---

## Troubleshooting

### 1. Build Errors (Where to find the log)
- Open your GitHub repository and click the **Actions** tab.
- Click on the failed workflow run (marked with a red **X**).
- Click on the **build-apk** job link on the left.
- Expand any failed step (such as *Build Debug APK*) to read the exact error message and Gradle stacktrace.
- If you see a missing keystore error, verify that `debug.keystore.base64` was uploaded to the root of your GitHub repository.

### 2. "App not installed"
- **Do not drag the zip file:** Ensure you extracted `MACRIC-APK.zip` and dragged the `.apk` file (`MACRIC.apk`), not the `.zip`.
- **Conflicting signature or old install:** If an earlier version of the app was previously installed, Android will reject the update if the debug signature changed. Go to your emulator's home screen, long-press the old MACRIC icon, select **Uninstall**, and then drag `MACRIC.apk` again.
- **Android version:** MACRIC requires Android 7.0 (Nougat, API 24) or newer. All standard 64-bit and 32-bit emulator instances (Pie 64-bit, Android 11, Android 12/13) are fully supported.

### 3. Overlay Permission Not Appearing in Emulator
- Some emulator versions (especially custom Android 7/9 ROMs) do not support opening the app-specific overlay settings page directly from an intent.
- **Manual enable in BlueStacks / LDPlayer:**
  1. Open emulator **System Settings** (or the gear icon).
  2. Go to **Apps** (or **Apps & Notifications**) > **Special app access** > **Display over other apps**.
  3. Find **MACRIC** in the list and switch the toggle to **Allowed**.
- **ADB command (alternative for power users):**
  If your emulator has ADB debugging enabled:
  ```bash
  adb shell appops set com.macric.app SYSTEM_ALERT_WINDOW allow
  ```

---

## Fair Play Guarantee
MACRIC strictly adheres to fair-play rules:
- Custom floating visual crosshair
- Custom on-screen fire button HUD positioning
- Sensitivity configuration and profile management
- **Zero aimbots, zero memory reading, zero macros, and zero game file modification.**
