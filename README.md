<h1 align="center">slawastr's launcher</h1>

<p align="center">
  <em>Sleek, glassmorphic and ultra-minimal home screen for Android TV and Google TV.</em>
</p>

<p align="center">
  <img alt="Android 8.0+" src="https://img.shields.io/badge/Android-8.0%2B-3ddc84">
  <img alt="APK size" src="https://img.shields.io/badge/APK-1.4%20MB-blue">
  <a href="LICENSE"><img alt="GPL-3.0 License" src="https://img.shields.io/badge/license-GPL--3.0-orange"></a>
</p>

<p align="center">
  <img src="docs/slawastr_launcher_home.png" alt="slawastr's launcher on TV" width="90%">
</p>

**slawastr's launcher** is a custom, performance-focused fork of [Plume Launcher](https://github.com/aradhyas8/Plume_Launcher) tailored for high-end TV setups (TCL, Google TV, Android TV). It pairs the lightning-fast responsiveness of Plume with a customized frosted glass UI, rounded floating settings card design, and out-of-the-box defaults.

---

## ✨ Features & Customizations

- **Floating Glass Settings Menu:** Completely reworked settings layout featuring a floating 400dp side-card with 28dp rounded corners, translucent frosted backdrop, and 24dp pill-shaped interactive rows.
- **Preconfigured Defaults on Install:**
  - **Theme:** Dark Glass style with 15% blur
  - **Typography:** Bold interface font with left-aligned header layout
  - **App Row:** Focused icon labels, high-contrast monochrome & branded icons
  - **Weather:** Enabled by default (Saint Petersburg, °C)
  - **Wallpaper:** Manual vertical positioning (-20% offset) & top gradient fade
- **Additional App Icons:** Native support for Kinopoisk, Rutube, V2Ray/v2rayNG, along with HDMI inputs (PlayStation, Xbox).
- **Home Button Control:** Works as exclusive Home launcher, seamlessly overriding bloated system launchers.
- **Zero Bloat & Ads:** No tracking, no background crawlers, minimal RAM usage (~50–70 MB).

---

## 📥 Installation

1. Grab the latest `slawastr_launcher.apk` from the [Releases](https://github.com/SLAWASTR/slawastr-launcher/releases) tab.
2. Install via ADB:
   ```bash
   adb install slawastr_launcher.apk
   ```
   *(Or transfer to a USB drive / use the Downloader app on your TV)*

3. *(Optional)* To permanently disable the default Google TV launcher and make **slawastr's launcher** the primary screen:
   ```bash
   adb shell cmd role add-role-holder --user 0 android.app.role.HOME dev.plume
   adb shell pm disable-user --user 0 com.google.android.apps.tv.launcherx
   adb shell pm disable-user --user 0 com.google.android.tungsten.setupwraith
   ```

---

## 🛠️ Building from Source

This project uses standard Gradle & Android SDK:

```bash
git clone https://github.com/SLAWASTR/slawastr-launcher.git
cd slawastr-launcher
./gradlew assembleDebug
```
The output APK will be placed under `app/build/outputs/apk/debug/app-debug.apk`.

---

## 📄 License

GPL-3.0 License. Based on [Plume Launcher](https://github.com/aradhyas8/Plume_Launcher) by aradhyas8.
