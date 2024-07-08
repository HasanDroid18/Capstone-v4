package com.example.rssitest

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : AppCompatActivity() {

    // Request code for location permission
    private val PERMISSIONS_REQUEST_CODE = 123
    // Interval for refreshing RSSI value (10 seconds)
    private val REFRESH_INTERVAL_MS = 10000L

    // Lateinit properties for WiFi manager, TextView, BroadcastReceiver, Handler, and Runnable
    private lateinit var wifiManager: WifiManager
    private lateinit var rssiTextView: TextView
    private lateinit var wifiReceiver: BroadcastReceiver
    private lateinit var handler: Handler
    private lateinit var refreshRunnable: Runnable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge layout
        enableEdgeToEdge()
        // Set the content view to activity_main layout
        setContentView(R.layout.activity_main)

        // Initialize WiFi manager
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        // Get reference to the TextView for displaying RSSI
        rssiTextView = findViewById(R.id.rssiTextView)

        // Initialize the BroadcastReceiver for WiFi scan results
        initializeWifiReceiver()

        // Create a handler for scheduling tasks on the main thread
        handler = Handler(Looper.getMainLooper())

        // Define a runnable task for auto-refreshing WiFi scan
        refreshRunnable = Runnable {
            autoRefresh()
            handler.postDelayed(refreshRunnable, REFRESH_INTERVAL_MS)
        }

        // Check for necessary permissions and request if not granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), PERMISSIONS_REQUEST_CODE)
        } else {
            // Permissions already granted, start auto-refresh
            startAutoRefresh()
        }
    }

    // Start the auto-refresh process
    private fun startAutoRefresh() {
        handler.post(refreshRunnable)
    }

    // Trigger a WiFi scan
    private fun autoRefresh() {
        wifiManager.startScan()
    }

    override fun onResume() {
        super.onResume()
        // Register the BroadcastReceiver for WiFi scan results and RSSI changes
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
        registerReceiver(wifiReceiver, IntentFilter(WifiManager.RSSI_CHANGED_ACTION))
        // Start auto-refresh and update RSSI
        startAutoRefresh()
        updateRSSI()
    }

    override fun onPause() {
        super.onPause()
        // Unregister the BroadcastReceiver and stop auto-refresh
        unregisterReceiver(wifiReceiver)
        handler.removeCallbacks(refreshRunnable)
    }

    // Initialize the BroadcastReceiver for WiFi scan results
    private fun initializeWifiReceiver() {
        wifiReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION == intent.action) {
                    updateRSSI()
                }
            }
        }
    }

    // Update the RSSI value in the TextView
    private fun updateRSSI() {
        val wifiInfo: WifiInfo? = wifiManager.connectionInfo
        if (wifiInfo != null) {
            val rssi = wifiInfo.rssi
            rssiTextView.text = "RSSI: $rssi dBm"
            println("RSSI: $rssi dBm")
        } else {
            rssiTextView.text = "RSSI: N/A"
            println("RSSI: N/A")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted, start auto-refresh
                startAutoRefresh()
            } else {
                // Permission denied, show a message to the user
                Toast.makeText(this, "Permission denied to access location. Cannot scan for Wi-Fi networks.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
