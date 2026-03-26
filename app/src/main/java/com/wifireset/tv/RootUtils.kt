package com.wifireset.tv

import android.util.Log
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

/**
 * RootUtils — executes shell commands with root (su) privileges.
 *
 * All methods are blocking; call them from a background coroutine.
 */
object RootUtils {

    private const val TAG = "RootUtils"

    /** Returns true if the device is rooted and grants su access. */
    fun isRooted(): Boolean {
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "echo rooted"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = reader.readLine()
            process.waitFor()
            output?.trim() == "rooted"
        } catch (e: Exception) {
            Log.e(TAG, "Root check failed: ${e.message}")
            false
        }
    }

    /**
     * Executes one or more shell commands as root.
     * @return Pair(success, outputLog)
     */
    fun execAsRoot(vararg commands: String): Pair<Boolean, String> {
        val output = StringBuilder()
        return try {
            val process = Runtime.getRuntime().exec("su")
            val stdin = DataOutputStream(process.outputStream)
            val stdout = BufferedReader(InputStreamReader(process.inputStream))
            val stderr = BufferedReader(InputStreamReader(process.errorStream))

            for (cmd in commands) {
                Log.d(TAG, "Executing: $cmd")
                stdin.writeBytes("$cmd\n")
                stdin.flush()
            }

            stdin.writeBytes("exit\n")
            stdin.flush()

            val exitCode = process.waitFor()

            // Collect stdout
            var line: String?
            while (stdout.readLine().also { line = it } != null) {
                output.appendLine(line)
                Log.d(TAG, "stdout: $line")
            }
            // Collect stderr
            while (stderr.readLine().also { line = it } != null) {
                output.appendLine("[ERR] $line")
                Log.w(TAG, "stderr: $line")
            }

            stdin.close()
            process.destroy()

            Pair(exitCode == 0, output.toString())
        } catch (e: Exception) {
            Log.e(TAG, "execAsRoot failed: ${e.message}")
            Pair(false, e.message ?: "Unknown error")
        }
    }

    // ─────────────────────────────────────────────
    // ACTION 1 — Forget All Saved WiFi Networks
    // ─────────────────────────────────────────────

    /**
     * Deletes all saved WiFi configurations by:
     * 1. Disabling WiFi
     * 2. Deleting the wpa_supplicant.conf file (stores passwords/SSIDs)
     * 3. Deleting the wifi config XML files used by Android's WifiConfigStore
     * 4. Re-enabling WiFi so the OS re-initialises with a clean state
     */
    fun forgetAllWifiNetworks(): Pair<Boolean, String> {
        val commands = arrayOf(
            // Step 1: Turn WiFi off cleanly
            "svc wifi disable",
            "sleep 1",

            // Step 2: Delete wpa_supplicant config (legacy path, still used on many ROM/TV builds)
            "rm -f /data/misc/wifi/wpa_supplicant.conf",
            "rm -f /data/misc/wifi/WifiConfigStore.xml",
            "rm -f /data/misc/wifi/softap.conf",

            // Step 3: Android 10+ stores WiFi configs here
            "rm -f /data/vendor/wifi/wpa/wpa_supplicant.conf",
            "rm -f /data/vendor/wifi/wpa_supplicant.conf",

            // Step 4: Android 11-13 WifiConfigStore paths
            "rm -f /data/misc_ce/0/wifi/WifiConfigStore.xml",
            "rm -f /data/misc_de/0/wifi/WifiConfigStore.xml",

            // Step 5: Remove any IP/DHCP leases
            "rm -f /data/misc/dhcp/*.lease",
            "rm -f /data/misc/dhcp/dnsmasq.leases",

            // Step 6: Re-enable WiFi (OS will create fresh config files)
            "sleep 1",
            "svc wifi enable"
        )
        return execAsRoot(*commands)
    }

    // ─────────────────────────────────────────────
    // ACTION 2 — Reset Network Settings
    // ─────────────────────────────────────────────

    /**
     * Resets network settings (WiFi + Ethernet + DNS + network policies).
     * Equivalent to Settings → System → Reset → Reset network settings.
     * Uses the hidden NetworkManagementService reset via am broadcast
     * and also clears the database entries for configured networks.
     */
    fun resetNetworkSettings(): Pair<Boolean, String> {
        val commands = arrayOf(
            // Disable WiFi and Bluetooth first
            "svc wifi disable",
            "svc bluetooth disable",
            "sleep 1",

            // Use Android's built-in network reset broadcast (works on AOSP/TV builds)
            "am broadcast -a android.intent.action.MASTER_CLEAR_NOTIFICATION --ez keepAccounts true",

            // Reset network policies database
            "rm -f /data/system/netpolicy.xml",
            "rm -f /data/system/netstats/",

            // Clear WifiConfigStore (all saved networks)
            "rm -f /data/misc/wifi/WifiConfigStore.xml",
            "rm -f /data/misc_ce/0/wifi/WifiConfigStore.xml",
            "rm -f /data/misc_de/0/wifi/WifiConfigStore.xml",
            "rm -f /data/misc/wifi/wpa_supplicant.conf",
            "rm -f /data/vendor/wifi/wpa/wpa_supplicant.conf",

            // Reset Ethernet settings
            "rm -f /data/misc/ethernet/ipconfig.txt",

            // Clear DNS/hosts overrides
            "rm -f /data/misc/net/rt_tables",

            // Restart networking
            "sleep 1",
            "svc wifi enable"
        )
        return execAsRoot(*commands)
    }

    // ─────────────────────────────────────────────
    // ACTION 3 — Full WiFi Factory Reset
    // ─────────────────────────────────────────────

    /**
     * Performs a deep WiFi factory reset:
     * — Wipes ALL WiFi-related data files
     * — Resets WiFi MAC randomisation settings
     * — Clears network permission grants
     * — Restarts the WiFi stack (wpa_supplicant daemon + wifi service)
     *
     * This mimics exactly what a full factory reset does to the WiFi subsystem
     * WITHOUT wiping the rest of the device data.
     */
    fun fullWifiFactoryReset(): Pair<Boolean, String> {
        val commands = arrayOf(
            // ── Step 1: Turn off WiFi ──
            "svc wifi disable",
            "sleep 2",

            // ── Step 2: Kill wpa_supplicant daemon ──
            "killall wpa_supplicant || true",
            "sleep 1",

            // ── Step 3: Wipe all WiFi data directories ──
            // Main config files
            "rm -f /data/misc/wifi/wpa_supplicant.conf",
            "rm -f /data/misc/wifi/WifiConfigStore.xml",
            "rm -f /data/misc/wifi/WifiConfigStoreEncrypted.xml",
            "rm -f /data/misc/wifi/softap.conf",
            "rm -f /data/misc/wifi/p2p_supplicant.conf",
            "rm -f /data/misc/wifi/entropy.bin",

            // Vendor WiFi path (Qualcomm, MediaTek, Amlogic TV SoCs)
            "rm -f /data/vendor/wifi/wpa/wpa_supplicant.conf",
            "rm -f /data/vendor/wifi/wpa_supplicant.conf",
            "rm -f /data/vendor/wifi/*.conf",

            // Android 11-13 multi-user aware paths
            "rm -f /data/misc_ce/0/wifi/WifiConfigStore.xml",
            "rm -f /data/misc_de/0/wifi/WifiConfigStore.xml",

            // ── Step 4: Clear WiFi scan results cache ──
            "rm -f /data/misc/wifi/*.bin",

            // ── Step 5: Remove DHCP leases ──
            "rm -f /data/misc/dhcp/*.lease",
            "rm -f /data/misc/dhcp/dnsmasq.leases",

            // ── Step 6: Reset network policies ──
            "rm -f /data/system/netpolicy.xml",

            // ── Step 7: Clear WiFi-related settings from SettingsProvider ──
            // Reset the stored SSID / password / security type in Settings DB
            "settings delete global wifi_networks_available_notification_on",
            "settings delete global wifi_on",
            "settings delete global wifi_saved_state",
            "settings delete global wifi_country_code",
            "settings delete global wifi_p2p_device_name",
            "settings delete global wifi_scan_always_enabled",
            "settings delete secure wifi_networks_available_notification_on",

            // ── Step 8: Reset MAC address randomisation store ──
            "rm -f /data/misc/wifi/PersistentMacRandomization.xml",

            // ── Step 9: Restart WiFi stack ──
            "sleep 1",
            "svc wifi enable",
            "sleep 2",

            // Force wifi service restart via am (triggers WifiService re-init)
            "am force-stop com.android.wifi || true",
            "sleep 1",
            "svc wifi enable"
        )
        return execAsRoot(*commands)
    }
}
