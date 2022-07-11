package com.sd.lib.social.wechat.core

import android.app.Activity
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.logMsg
import java.lang.ref.WeakReference

abstract class FSocialWechatApi {
    private var _trackActivityRef: WeakReference<Activity>? = null

    protected fun startTrackActivity() {
        val activity = FSocialWechat.topActivity ?: return
        if (activity.isFinishing) return
        _trackActivityRef = WeakReference(activity)
        logMsg { "${javaClass.simpleName} startTrackActivity $activity" }
    }

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
        if (_trackActivityRef?.get() === activity && activity.isFinishing) {
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