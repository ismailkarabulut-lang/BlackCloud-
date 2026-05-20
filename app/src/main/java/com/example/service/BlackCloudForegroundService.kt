package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.repository.TheiaRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * PROJECT BLACKCLOUD için Kalıcı Arka Plan Servisi (Foreground Service).
 * Yerel FastAPI sunucusunu (localhost:8765) periyodik pingleyerek liveness takibi yapar,
 * durum değişikliklerini sisteme bildirir ve işletim sistemi tarafından arka planda öldürülmesini engeller.
 */
class BlackCloudForegroundService : Service() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)
    private val repository = TheiaRepository()

    companion object {
        private const val TAG = "BlackCloudService"
        private const val CHANNEL_ID = "blackcloud_channel"
        private const val NOTIFICATION_ID = 8878

        // Bağlantı durumunu uygulama içinde gözlemlemek için Flow yapısı
        private val _isBackendAlive = MutableStateFlow<Boolean>(false)
        val isBackendAlive: StateFlow<Boolean> = _isBackendAlive

        // Servisi başlatmak/durdurmak için kolaylaştırıcı fonksiyonlar
        fun startService(context: Context) {
            val intent = Intent(context, BlackCloudForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, BlackCloudForegroundService::class.java)
            context.stopService(intent)
        }
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Foreground Servisi oluşturuldu.")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Foreground Servisi başlatılıyor...")
        
        // İlk bildirimi göster ve kendisini foreground servis yap
        val initialNotification = buildNotification("Bağlantı kuruluyor...", false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID, 
                initialNotification, 
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NOTIFICATION_ID, initialNotification)
        }

        // Çekirdeği pingleyecek rutin işi başlat
        startBackendPinger()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null // Binder kullanımayacak, Flow üzerinden iletişim sağlanacak
    }

    /**
     * Her 10 saniyede bir IP / localhost pingleyerek liveness durumunu günceller.
     */
    private fun startBackendPinger() {
        serviceScope.launch {
            while (isActive) {
                val alive = repository.ping()
                if (_isBackendAlive.value != alive) {
                    _isBackendAlive.value = alive
                    // Bildirimi anlık duruma göre güncelle
                    updateNotification(alive)
                }
                Log.d(TAG, "BLACKCLOUD ping durumu: ${if (alive) "BAĞLI" else "BAĞLANTI YOK"}")
                delay(10000) // 10 saniyede bir ping
            }
        }
    }

    private fun updateNotification(alive: Boolean) {
        val message = if (alive) {
            "BLACKCLOUD Çekirdeği Aktif ⬤ (Bağlandı)"
        } else {
            "Çekirdek Bağlantısı Kesildi ◯ (Yeniden bağlanıyor...)"
        }
        val notification = buildNotification(message, alive)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildNotification(message: String, alive: Boolean): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 
            0, 
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val title = "PROJECT BLACKCLOUD"
        val iconRes = android.R.drawable.stat_sys_data_bluetooth // Varsayılan sistem ikon yedek

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setColor(if (alive) 0xFF00E676.toInt() else 0xFFFF1744.toInt()) // Yeşil veya Kırmızı accent renk
            .setPriority(NotificationCompat.PRIORITY_LOW) // Çıkmayan ve sessiz bildirim
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "BLACKCLOUD Arka Plan Bildirimi"
            val descriptionText = "Bilişsel ikiz bağlantısını ve servis durumunu gösterir"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Foreground Servisi yok ediliyor.")
        serviceJob.cancel() // CoroutineScope'u iptal et
    }
}
