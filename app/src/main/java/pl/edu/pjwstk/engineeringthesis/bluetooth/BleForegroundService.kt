package pl.edu.pjwstk.engineeringthesis.bluetooth

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import pl.edu.pjwstk.engineeringthesis.MainActivity
import pl.edu.pjwstk.engineeringthesis.R
import pl.edu.pjwstk.engineeringthesis.viewmodel.ConnectedDevice
import pl.edu.pjwstk.engineeringthesis.viewmodel.ScanUiState
import javax.inject.Inject

@AndroidEntryPoint
class BleForegroundService : Service() {

    @Inject
    lateinit var bleConnectionManager: BleConnectionManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var notificationJob: Job? = null
    private var isForegroundStarted = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        observeNotificationState()
        bleConnectionManager.resumeManagedSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }

            else -> {
                bleConnectionManager.resumeManagedSession()
                startOrUpdateForeground(
                    title = getString(R.string.ble_service_title),
                    text = getString(R.string.ble_service_text_idle)
                )
                return START_STICKY
            }
        }
    }

    override fun onDestroy() {
        notificationJob?.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun observeNotificationState() {
        notificationJob?.cancel()
        notificationJob = serviceScope.launch {
            combine(
                bleConnectionManager.connectedDevice,
                bleConnectionManager.scanState
            ) { connectedDevice, scanState ->
                buildNotificationText(connectedDevice, scanState)
            }.collect { (title, text) ->
                startOrUpdateForeground(title, text)
            }
        }
    }

    private fun buildNotificationText(
        connectedDevice: ConnectedDevice?,
        scanState: ScanUiState
    ): Pair<String, String> {
        val deviceName = connectedDevice?.name ?: getString(R.string.band_unknown)
        return when {
            connectedDevice?.isConnected == true ->
                getString(R.string.ble_service_title) to
                    getString(R.string.ble_service_text_connected, deviceName)

            connectedDevice?.disconnectPending == true ->
                getString(R.string.ble_service_title) to
                    getString(R.string.ble_service_text_disconnect_pending, deviceName)

            connectedDevice != null ->
                getString(R.string.ble_service_title) to
                    getString(R.string.ble_service_text_reconnecting, deviceName)

            scanState is ScanUiState.Scanning ->
                getString(R.string.ble_service_title) to
                    getString(R.string.ble_service_text_scanning)

            else ->
                getString(R.string.ble_service_title) to
                    getString(R.string.ble_service_text_idle)
        }
    }

    private fun startOrUpdateForeground(title: String, text: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()

        if (!isForegroundStarted) {
            startForeground(NOTIFICATION_ID, notification)
            isForegroundStarted = true
        } else {
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.notify(NOTIFICATION_ID, notification)
        }
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return PendingIntent.getActivity(this, 0, intent, flags)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.ble_service_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = getString(R.string.ble_service_channel_description)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_START = "pl.edu.pjwstk.engineeringthesis.bluetooth.action.START"
        private const val ACTION_STOP = "pl.edu.pjwstk.engineeringthesis.bluetooth.action.STOP"
        private const val CHANNEL_ID = "ble_connection"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_START
            }
            ContextCompat.startForegroundService(context, intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, BleForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }
}
