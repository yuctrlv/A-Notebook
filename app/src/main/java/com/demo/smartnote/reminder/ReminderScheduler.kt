package com.demo.smartnote.reminder

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

/**
 * 排「下一次」提醒。
 *
 * 为什么不用 PeriodicWorkRequest：它的最小周期是 15 分钟、而且允许系统推迟执行，
 * 想要「每天 20:00 整」这种固定时刻，它给不准。
 * 所以这里用 OneTimeWorkRequest + setInitialDelay：先算出距离今天（或明天）目标时刻还有多久，
 * Worker 执行完自己再排下一天，滚雪球式维持每天一次。
 */
object ReminderScheduler {

    private const val WORK_NAME = "daily_reminder"

    fun scheduleNext(context: Context, hour: Int, minute: Int) {
        val now = LocalDateTime.now()
        var target = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0)
        // 今天这个点已经过了（或正好是现在），就排到明天
        if (!target.isAfter(now)) target = target.plusDays(1)
        val delayMillis = Duration.between(now, target).toMillis()

        val request = OneTimeWorkRequestBuilder<ReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                Data.Builder()
                    .putInt(ReminderWorker.KEY_HOUR, hour)
                    .putInt(ReminderWorker.KEY_MINUTE, minute)
                    .build()
            )
            .build()

        // REPLACE：改了时间就顶掉旧的那一条，不会堆出多个提醒
        WorkManager.getInstance(context)
            .enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
