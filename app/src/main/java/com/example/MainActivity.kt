package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.service.BlackCloudForegroundService
import com.example.ui.screens.BlackCloudMainScreen
import com.example.ui.theme.BlackCloudTheme
import com.example.ui.viewmodel.BlackCloudViewModel

/**
 * PROJECT BLACKCLOUD Kabuk İstemcisinin Ana Giriş Aktivitesi.
 * Edge-to-Edge çizim, bildirim izni talebi ve Arka Plan Servisi kontrolünü kapsar.
 */
class MainActivity : ComponentActivity() {

    private val viewModel: BlackCloudViewModel by viewModels()

    // Android 13+ (API 33) için Bildirim İzni İsteyicisi
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            // İzin verildi, arka plan durum servisini başlat
            BlackCloudForegroundService.startService(this)
        } else {
            Toast.makeText(
                this, 
                "Bildirim izni reddedildi. Bağlantı durumlarını arka planda takip edemeyebilirsiniz.", 
                Toast.LENGTH_LONG
            ).show()
            // İzin verilmese dahi servisi başlatmayı deneyebiliriz (Android 13+ bildirim göstermez ama çalışır)
            BlackCloudForegroundService.startService(this)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Bildirim İzin Kontrolleri
        checkAndRequestNotificationPermission()

        setContent {
            BlackCloudTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    BlackCloudMainScreen(
                        viewModel = viewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) {
                BlackCloudForegroundService.startService(this)
            } else {
                requestNotificationPermissionLauncher.launch(permission)
            }
        } else {
            // Android 13 öncesi izin istemeye gerek yok
            BlackCloudForegroundService.startService(this)
        }
    }
}

/**
 * Robolectric veya görsel testlerle tam uyumluluğu korumak için 
 * Greeting metodu aynen muhafaza edilmiştir.
 */
@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(text = "Hello $name!", modifier = modifier)
}
