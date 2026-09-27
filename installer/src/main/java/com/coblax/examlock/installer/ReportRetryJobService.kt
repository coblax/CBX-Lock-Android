package com.coblax.examlock.installer

import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context

/**
 * Sends kept reports when the phone has a connection. Survives a reboot, so a report from a
 * student who installed offline and never reopened the installer still reaches the admins.
 */
class ReportRetryJobService : JobService() {
    override fun onStartJob(params: JobParameters): Boolean {
        Thread {
            val remaining = runCatching { TelegramReporter.flushPending(applicationContext) }.getOrDefault(1)
            jobFinished(params, remaining > 0)
        }.start()
        return true
    }

    override fun onStopJob(params: JobParameters): Boolean = true

    companion object {
        private const val JobId = 4201

        fun schedule(context: Context) {
            runCatching {
                val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
                val job = JobInfo.Builder(JobId, ComponentName(context, ReportRetryJobService::class.java))
                    .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                    .setBackoffCriteria(30_000L, JobInfo.BACKOFF_POLICY_EXPONENTIAL)
                    .setPersisted(true)
                    .build()
                scheduler.schedule(job)
            }
        }
    }
}
