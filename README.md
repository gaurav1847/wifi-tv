# 📶 WiFi Reset TV — Android 13 TV App

A root-powered Android TV app to forget all WiFi networks, reset network settings,
and perform a full WiFi factory reset — without wiping any other data on the device.

---

## ✅ Features

| Feature | What it does |
|---|---|
| **Forget All WiFi Networks** | Deletes all saved SSIDs and passwords (wpa_supplicant + WifiConfigStore) |
| **Reset Network Settings** | Resets WiFi, Bluetooth, DNS, network policies to factory state |
| **Full WiFi Factory Reset** | Deep wipe of all WiFi subsystem files + restarts WiFi stack |

---

## 📋 Requirements

- Android TV with **Android 13** (API 33)
- Device must be **rooted** (Magisk, KingRoot, SuperSU, etc.)
- A **Superuser manager** installed (e.g. Magisk Manager) to grant root permission
- The app must be granted root access when prompted

---

## 🛠️ How to Build

### Prerequisites
- Android Studio Hedgehog or newer
- JDK 17
- Android SDK 33

### Steps

1. **Open the project in Android Studio**
   ```
   File → Open → select the WifiResetTV folder
   ```

2. **Sync Gradle**
   Android Studio will automatically sync. Wait for it to complete.

3. **Build the APK**
   ```
   Build → Build Bundle(s) / APK(s) → Build APK(s)
   ```
   APK will be at:
   ```
   app/build/outputs/apk/debug/app-debug.apk
   ```

4. **Or build via command line**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 📲 How to Install on Android TV

### Method A — ADB (recommended)
Connect your TV and PC to the same WiFi network, enable ADB debugging on the TV:
```bash
adb connect <TV_IP_ADDRESS>:5555
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Method B — USB
```bash
adb install -t app-debug.apk
```

### Method C — File Manager
Copy the APK to a USB stick, plug into TV, open a file manager app and install.

---

## 🔑 Granting Root Access

On first launch:
1. The app checks for root access
2. Your Superuser manager (Magisk Manager, etc.) will show a **Grant / Deny** popup
3. Tap **Grant**
4. The status badge will turn **green ✅**

If you accidentally denied root, go to **Magisk → SuperUser** and grant access manually for `com.wifireset.tv`.

---

## 🗂️ Project Structure

```
WifiResetTV/
├── app/
│   └── src/main/
│       ├── AndroidManifest.xml          # Permissions + TV launcher config
│       ├── java/com/wifireset/tv/
│       │   ├── MainActivity.kt          # Main UI + button handlers
│       │   ├── RootUtils.kt             # All root shell commands
│       │   └── ConfirmDialog.kt         # TV-friendly confirmation dialog
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml    # Main screen layout (TV-optimised)
│           │   └── dialog_confirm.xml  # Confirmation dialog layout
│           ├── drawable/               # Button states, backgrounds
│           └── values/                 # Colors, strings, themes
└── build.gradle                        # Dependencies
```

---

## ⚙️ How Root Commands Work

The app uses `su` (Superuser) to run shell commands.

### Forget All WiFi Networks
Deletes:
- `/data/misc/wifi/wpa_supplicant.conf`
- `/data/misc/wifi/WifiConfigStore.xml`
- `/data/misc_ce/0/wifi/WifiConfigStore.xml` (Android 11–13)
- `/data/vendor/wifi/wpa/wpa_supplicant.conf` (Qualcomm / MediaTek SoCs)
- DHCP lease files

### Reset Network Settings
Runs the hidden `MASTER_CLEAR_NOTIFICATION` broadcast + deletes:
- WiFi config files
- Ethernet config
- Network policy database

### Full WiFi Factory Reset
- Kills `wpa_supplicant` daemon
- Wipes all WiFi data files (configs, leases, cache, entropy)
- Resets WiFi Settings DB keys via `settings delete`
- Restarts WiFi stack via `svc wifi`

---

## 🔒 Permissions Declared

```xml
android.permission.ACCESS_WIFI_STATE
android.permission.CHANGE_WIFI_STATE
android.permission.ACCESS_NETWORK_STATE
android.permission.CHANGE_NETWORK_STATE
android.permission.NETWORK_SETTINGS        ← system/root only
android.permission.MASTER_CLEAR           ← system/root only
```

The privileged permissions are only effective when:
- The APK is installed as a **system app** (`/system/priv-app/`), OR
- Root is granted via `su` at runtime (how this app works)

---

## ❓ Troubleshooting

| Problem | Fix |
|---|---|
| Root check fails | Open Magisk Manager → SuperUser → grant `com.wifireset.tv` |
| WiFi not re-enabling | Run `svc wifi enable` via ADB manually |
| App not in TV launcher | Reinstall with `adb install -r -t` |
| Operation fails partway | Some paths may differ on custom ROM — check logcat with `adb logcat -s RootUtils` |

---

## 📜 License
MIT — Free for personal and commercial use.
