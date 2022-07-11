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
            onTrackActivityStarted()
        }
    }

    internal fun onActivityResumed(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            onTrackActivityResumed()
        }
    }

    internal fun onActivityStopped(activity: Activity) {
        if (_trackActivityRef?.get() === activity) {
            onTrackActivityStopped()
        }
    }

    internal fun onActivityDestroyed(activity: Activity) {
        if (_trackActivityRef?.get() === activity && activity.isFinishing) {
            stopTrackActivity()
            onTrackActivityDestroyed()
        }
    }

    protected open fun onTrackActivityStarted() {
        logMsg { "${javaClass.simpleName} onTrackActivityStarted" }
    }

    protected open fun onTrackActivityResumed() {
        logMsg { "${javaClass.simpleName} onTrackActivityResumed" }
    }

    protected open fun onTrackActivityStopped() {
        logMsg { "${javaClass.simpleName} onTrackActivityStopped" }
    }

    protected open fun onTrackActivityDestroyed() {
        logMsg { "${javaClass.simpleName} onTrackActivityDestroyed" }
    }
}