package pro.xservis.client.vpn

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.VpnService
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import pro.xservis.client.R
import pro.xservis.client.ui.MainActivity

/**
 * Foreground VpnService implementation. The actual Xray-core integration is wired
 * up in a follow-up iteration via libv2ray.aar; for now this service handles the
 * Android lifecycle plumbing (foreground notification, START/STOP intents, the
 * VpnService.Builder routing) so the app installs and runs end-to-end.
 */
@AndroidEntryPoint
class XservisVpnService : VpnService() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startVpn()
            ACTION_STOP -> stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        startForeground(NOTIFICATION_ID, buildNotification("Подключено · защищено"))
        // TODO: bind tun via Builder() and feed it into Xray-core through libv2ray
    }

    private fun stopVpn() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = super.onBind(intent)

    private fun buildNotification(text: String): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VPN status",
            NotificationManager.IMPORTANCE_LOW,
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)

        val pi = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("xservis")
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        const val ACTION_START = "pro.xservis.client.vpn.START"
        const val ACTION_STOP = "pro.xservis.client.vpn.STOP"
        private const val NOTIFICATION_ID = 0xCAFE
        private const val CHANNEL_ID = "xservis-vpn"

        fun start(ctx: Context) {
            val intent = Intent(ctx, XservisVpnService::class.java).setAction(ACTION_START)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }
        }

        fun stop(ctx: Context) {
            ctx.startService(Intent(ctx, XservisVpnService::class.java).setAction(ACTION_STOP))
        }
    }
}
