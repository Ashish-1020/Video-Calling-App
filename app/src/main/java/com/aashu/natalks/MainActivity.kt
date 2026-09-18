package com.aashu.natalks

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.aashu.natalks.call.CallSignalingService
import com.aashu.natalks.navigation.NaTalksNavHost
import com.aashu.natalks.ui.theme.NaTalksTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestRuntimePermissionsIfNeeded()
        observeSessionForSignalingService()

        val container = (application as NaTalksApp).container
        setContent {
            NaTalksTheme {
                NaTalksNavHost(container = container)
            }
        }
    }

    private fun requestRuntimePermissionsIfNeeded() {
        val permissions = mutableListOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }

    private fun observeSessionForSignalingService() {
        val container = (application as NaTalksApp).container
        var serviceRunning = false
        lifecycleScope.launch {
            container.sessionHolder.session.collect { session ->
                if (session != null && !serviceRunning) {
                    serviceRunning = true
                    ContextCompat.startForegroundService(
                        this@MainActivity,
                        Intent(this@MainActivity, CallSignalingService::class.java)
                    )
                } else if (session == null && serviceRunning) {
                    serviceRunning = false
                    stopService(Intent(this@MainActivity, CallSignalingService::class.java))
                }
            }
        }
    }
}
