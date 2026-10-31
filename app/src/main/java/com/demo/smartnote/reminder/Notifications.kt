package com.demo.smartnote.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.demo.smartnote.MainActivity
import com.demo.smartnote.R

/**
 * 通知的集中封装：建渠道 + 发「今日任务」提醒。
 */
object Notifications {

    private const val CHANNEL_ID = "daily_reminder"
    private const val CHANNEL_NOTE_ID = "note_reminder"
    private const val NOTIFICATION_ID = 1001

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "每日提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "每天固定时间提醒今日任务" }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_NOTE_ID,
                "笔记提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "单条笔记的定时提醒" }
        )
    }

    /** 是否有通知权限（Android 13 起需要用户手动授予） */
    fun hasPermission(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    fun showDailyReminder(context: Context, pending: Int, total: Int) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val text = when {
            total == 0 -> "今天还没有任务，先加几个吧"
            pending == 0 -> "今天的任务都完成啦，继续保持 🎉"
            else -> "今天还有 $pending 个任务没完成"
        }

        // 点通知直接进「今日任务」页
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(MainActivity.EXTRA_ROUTE, "tasks")
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_note)
            .setContentTitle("今日任务")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    /** 单条笔记的定时提醒 */
    fun showNoteReminder(context: Context, title: String, content: String) {
        if (!hasPermission(context)) return
        ensureChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_NOTE_ID)
            .setSmallIcon(R.drawable.ic_stat_note)
            .setContentTitle("笔记提醒：$title")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID + 1, notification)
    }
}
