package com.sd.lib.social.wechat.core

import android.app.Activity
import com.sd.lib.social.wechat.FSocialWechat
import java.lang.ref.WeakReference

abstract class FSocialWechatApi {
    private var _trackActivityRef: WeakReference<Activity>? = null

    protected fun startTrackActivity() {
        val activity = FSocialWechat.topActivity ?: return
        if (activity.isFinishing) return
        _trackActivityRef = WeakReference(activity)
    }

    protected fun stopTrackActivity() {
        _trackActivityRef = null
    }

    internal fun onActivityDestroyed(activity: Activity) {
        if (_trackActivityRef?.get() === activity && activity.isFinishing) {
            stopTrackActivity()
            onTrackActivityDestroyed()
        }
    }

    protected open fun onTrackActivityDestroyed() {

    }
}