package com.sd.lib.social.wechat.core

import android.app.Activity
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.logMsg
import java.lang.ref.WeakReference

abstract class FSocialWechatApi {
    private var _trackActivityRef: WeakReference<Activity>? = null

    /**
     * 开始追踪Activity
     */
    protected fun startTrackActivity(): Boolean {
        val activity = FSocialWechat.topActivity ?: return false
        if (activity.isFinishing) return false
        _trackActivityRef = WeakReference(activity)
        logMsg { "${javaClass.simpleName} startTrackActivity $activity" }
        return true
    }

    /**
     * 停止追踪Activity
     */
    protected fun stopTrackActivity() {
        if (_trackActivityRef != null) {
            _trackActivityRef = null
            logMsg { "${javaClass.simpleName} stopTrackActivity" }
        }
    }

    internal fun onActivityStarted(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            logMsg { "${javaClass.simpleName} onTrackActivityStarted" }
            onTrackActivityStarted()
        }
    }

    internal fun onActivityResumed(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            logMsg { "${javaClass.simpleName} onTrackActivityResumed" }
            onTrackActivityResumed()
        }
    }

    internal fun onActivityStopped(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            logMsg { "${javaClass.simpleName} onTrackActivityStopped" }
            onTrackActivityStopped()
        }
    }

    internal fun onActivityDestroyed(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            logMsg { "${javaClass.simpleName} onTrackActivityDestroyed" }
            stopTrackActivity()
            onTrackActivityDestroyed()
        }
    }

    protected open fun onTrackActivityStarted() {
    }

    protected open fun onTrackActivityResumed() {
    }

    protected open fun onTrackActivityStopped() {
    }

    protected open fun onTrackActivityDestroyed() {
    }
}