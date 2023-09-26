package co.sidhant.terminal

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import co.sidhant.terminal.ui.theme.TerminalIntegrationTheme
import android.content.Intent
import android.os.Build
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import co.sidhant.terminal.stripe.ReaderActivity
import co.sidhant.terminal.stripe.TerminalManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TerminalIntegrationTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text("Android")
                }
            }
        }
        requestPermissions()
    }

    private val requestPermissionLauncher =
        registerForActivityResult(RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                if (it.value) {
                    allPermissionsGranted()
                } else {
                    requestPermissions()
                }
            }
        }

    private fun requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestPermissionLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.BLUETOOTH_SCAN,
                    android.Manifest.permission.BLUETOOTH_CONNECT,
                )
            )
        } else {
            requestLocationPermissionOnly()
        }
    }

    private fun requestLocationPermissionOnly() {
        requestPermissionLauncher.launch(
            arrayOf(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            )
        )
    }

    private fun allPermissionsGranted() {
        TerminalManager.initTerminal(applicationContext)
        val intent = Intent(this, ReaderActivity::class.java)
        startActivity(intent)
    }
}