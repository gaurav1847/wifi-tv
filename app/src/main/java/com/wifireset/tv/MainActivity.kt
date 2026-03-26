package com.wifireset.tv

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var tvRootStatus: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnForgetWifi: LinearLayout
    private lateinit var btnNetworkReset: LinearLayout
    private lateinit var btnFactoryWifi: LinearLayout

    private var isRooted = false
    private var isBusy = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupButtons()
        checkRoot()
    }

    private fun bindViews() {
        tvStatus = findViewById(R.id.tvStatus)
        tvRootStatus = findViewById(R.id.tvRootStatus)
        progressBar = findViewById(R.id.progressBar)
        btnForgetWifi = findViewById(R.id.btnForgetWifi)
        btnNetworkReset = findViewById(R.id.btnNetworkReset)
        btnFactoryWifi = findViewById(R.id.btnFactoryWifi)
    }

    private fun setupButtons() {
        btnForgetWifi.setOnClickListener {
            if (!canProceed()) return@setOnClickListener
            ConfirmDialog(
                context = this,
                title = "Forget All WiFi Networks",
                message = "This will delete ALL saved WiFi passwords and networks.\nYou will need to reconnect manually after this.",
                icon = "🗑️",
                onConfirm = { runForgetWifi() }
            ).show()
        }

        btnNetworkReset.setOnClickListener {
            if (!canProceed()) return@setOnClickListener
            ConfirmDialog(
                context = this,
                title = "Reset Network Settings",
                message = "This will reset WiFi, Bluetooth and network policies to factory defaults.\nContinue?",
                icon = "🔄",
                onConfirm = { runNetworkReset() }
            ).show()
        }

        btnFactoryWifi.setOnClickListener {
            if (!canProceed()) return@setOnClickListener
            ConfirmDialog(
                context = this,
                title = "Full WiFi Factory Reset",
                message = "⚠️ WARNING: This completely wipes the WiFi subsystem — all saved networks, DHCP leases, MAC settings and WiFi policies will be erased.\n\nThis is equivalent to a factory reset for WiFi only. Proceed?",
                icon = "⚠️",
                onConfirm = { runFullWifiFactoryReset() }
            ).show()
        }
    }

    // ─────────────────────────────────────────────
    // Root check
    // ─────────────────────────────────────────────

    private fun checkRoot() {
        setStatus("⏳ Checking root access…", Color.parseColor("#AABBCC"))
        lifecycleScope.launch {
            val rooted = withContext(Dispatchers.IO) { RootUtils.isRooted() }
            isRooted = rooted
            if (rooted) {
                tvRootStatus.text = "✅  Root access granted"
                tvRootStatus.setTextColor(Color.parseColor("#22AA66"))
                setStatus("Ready. Select an action using D-pad or remote control.", Color.parseColor("#AABBCC"))
            } else {
                tvRootStatus.text = "❌  Root access denied"
                tvRootStatus.setTextColor(Color.parseColor("#FF4455"))
                setStatus(
                    "Root not available. Grant Superuser permission and relaunch the app.",
                    Color.parseColor("#FF6677")
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // Action 1: Forget All WiFi Networks
    // ─────────────────────────────────────────────

    private fun runForgetWifi() {
        setBusy(true)
        setStatus("🗑️ Removing all saved WiFi networks…", Color.parseColor("#4488FF"))
        lifecycleScope.launch {
            val (success, log) = withContext(Dispatchers.IO) {
                RootUtils.forgetAllWifiNetworks()
            }
            setBusy(false)
            if (success) {
                setStatus(
                    "✅ All WiFi networks forgotten successfully.\nWiFi has been re-enabled with a clean slate.",
                    Color.parseColor("#22AA66")
                )
            } else {
                setStatus(
                    "❌ Failed to forget WiFi networks.\nLog: $log",
                    Color.parseColor("#FF4455")
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // Action 2: Reset Network Settings
    // ─────────────────────────────────────────────

    private fun runNetworkReset() {
        setBusy(true)
        setStatus("🔄 Resetting all network settings…", Color.parseColor("#4488FF"))
        lifecycleScope.launch {
            val (success, log) = withContext(Dispatchers.IO) {
                RootUtils.resetNetworkSettings()
            }
            setBusy(false)
            if (success) {
                setStatus(
                    "✅ Network settings reset successfully.\nWiFi and Bluetooth will re-initialize.",
                    Color.parseColor("#22AA66")
                )
            } else {
                setStatus(
                    "❌ Failed to reset network settings.\nLog: $log",
                    Color.parseColor("#FF4455")
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // Action 3: Full WiFi Factory Reset
    // ─────────────────────────────────────────────

    private fun runFullWifiFactoryReset() {
        setBusy(true)
        setStatus("⚠️ Performing full WiFi factory reset… please wait.", Color.parseColor("#FF9900"))
        lifecycleScope.launch {
            val (success, log) = withContext(Dispatchers.IO) {
                RootUtils.fullWifiFactoryReset()
            }
            setBusy(false)
            if (success) {
                setStatus(
                    "✅ Full WiFi factory reset completed.\nAll WiFi data wiped. WiFi stack restarted.",
                    Color.parseColor("#22AA66")
                )
            } else {
                setStatus(
                    "❌ Full WiFi factory reset failed.\nLog: $log",
                    Color.parseColor("#FF4455")
                )
            }
        }
    }

    // ─────────────────────────────────────────────
    // UI Helpers
    // ─────────────────────────────────────────────

    private fun canProceed(): Boolean {
        if (isBusy) {
            setStatus("⏳ Operation already in progress. Please wait…", Color.parseColor("#FF9900"))
            return false
        }
        if (!isRooted) {
            setStatus("❌ Root access is required. Grant Superuser and relaunch.", Color.parseColor("#FF4455"))
            return false
        }
        return true
    }

    private fun setBusy(busy: Boolean) {
        isBusy = busy
        progressBar.visibility = if (busy) View.VISIBLE else View.GONE
        btnForgetWifi.isEnabled = !busy
        btnNetworkReset.isEnabled = !busy
        btnFactoryWifi.isEnabled = !busy
        btnForgetWifi.alpha = if (busy) 0.5f else 1.0f
        btnNetworkReset.alpha = if (busy) 0.5f else 1.0f
        btnFactoryWifi.alpha = if (busy) 0.5f else 1.0f
    }

    private fun setStatus(message: String, color: Int = Color.parseColor("#AABBCC")) {
        tvStatus.text = message
        tvStatus.setTextColor(color)
    }
}
