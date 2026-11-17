package com.ivanarroyo.pesito.presentation.notifications

import android.Manifest
import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.ivanarroyo.pesito.MainActivity
import com.ivanarroyo.pesito.R
import com.ivanarroyo.pesito.domain.model.AlertDirection
import com.ivanarroyo.pesito.domain.model.TriggeredAlert
import com.ivanarroyo.pesito.domain.repository.AlertNotifier
import com.ivanarroyo.pesito.presentation.util.MoneyFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class AndroidAlertNotifier @Inject constructor(
    @ApplicationContext private val context: Context,
) : AlertNotifier {

    private val manager = NotificationManagerCompat.from(context)

    // Ya comprobamos el permiso en canNotify(); el lint no lo ve porque está en otra función.
    @SuppressLint("MissingPermission")
    override fun notify(triggered: List<TriggeredAlert>) {
        if (!canNotify()) return
        createChannel()
        triggered.forEach { manager.notify(it.alert.id.toInt(), build(it)) }
    }

    /** Android 13+ exige el permiso POST_NOTIFICATIONS; además el usuario puede apagarlas en Ajustes. */
    private fun canNotify(): Boolean {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        return hasPermission && manager.areNotificationsEnabled()
    }

    /** Desde Android 8 toda notificación va en un "canal" que el usuario puede configurar. */
    private fun createChannel() {
        val channel = NotificationChannelCompat.Builder(CHANNEL_ID, NotificationManagerCompat.IMPORTANCE_DEFAULT)
            .setName(context.getString(R.string.alert_channel_name))
            .setDescription(context.getString(R.string.alert_channel_description))
            .build()
        manager.createNotificationChannel(channel) // Si ya existe, no pasa nada.
    }

    private fun build(triggered: TriggeredAlert) = with(triggered.alert) {
        val text = context.getString(
            if (direction == AlertDirection.BELOW) R.string.alert_notification_below else R.string.alert_notification_above,
            from,
            MoneyFormat.format(triggered.currentRate),
            to,
            MoneyFormat.format(target),
        )
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notifications)
            .setContentTitle(context.getString(R.string.alert_notification_title, from, to))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(openAppIntent())
            .setAutoCancel(true)
            .build()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private companion object {
        const val CHANNEL_ID = "rate_alerts"
    }
}
