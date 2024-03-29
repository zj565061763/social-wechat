package com.sd.lib.social.wechat

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.sd.lib.social.wechat.core.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.FSocialWechatShareApi
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.lang.ref.WeakReference
import java.util.concurrent.atomic.AtomicBoolean

object FSocialWechat {
    private var _context: Application? = null
    private var _appId: String = ""
    private var _appSecret: String = ""
    private var _wxapi: IWXAPI? = null

    internal val context: Context
        get() = checkNotNull(_context) { "You should init before this" }

    internal val appId: String
        get() = _appId.also { check(it.isNotEmpty()) { "You should init before this" } }

    internal val appSecret: String
        get() = _appSecret.also { check(it.isNotEmpty()) { "You should init before this" } }

    internal val topActivity: Activity?
        get() = _activityCallback.topActivity

    @JvmStatic
    var isDebug = false

    @JvmStatic
    val wxapi: IWXAPI
        get() {
            synchronized(this@FSocialWechat) {
                val api = _wxapi
                if (api != null) return api
                return WXAPIFactory.createWXAPI(context, appId, true).also {
                    logMsg { "createWXAPI" }
                    _wxapi = it
                    it.registerApp(appId)
                }
            }
        }

    @JvmStatic
    fun init(context: Context, appId: String, appSecret: String) {
        require(appId.isNotEmpty()) { "appId is empty" }
        require(appSecret.isNotEmpty()) { "appSecret is empty" }
        synchronized(this@FSocialWechat) {
            logMsg { "init" }
            val application = context.applicationContext as Application
            _context = application
            _appSecret = appSecret
            _broadcastReceiver.register()
            _activityCallback.register(application)
            if (_appId != appId) {
                _appId = appId
                _wxapi?.unregisterApp()
                _wxapi = null
            }
        }
    }

    /**
     * 微信是否已安装
     */
    fun isWechatInstalled(): Boolean {
        return wxapi.isWXAppInstalled
    }

    private val _broadcastReceiver = object : BroadcastReceiver() {
        private val _hasRegister = AtomicBoolean(false)

        override fun onReceive(context: Context?, intent: Intent?) {
            val id = _appId
            val api = _wxapi
            if (id.isNotEmpty() && api != null) {
                logMsg { "BroadcastReceiver onReceive registerApp" }
                api.registerApp(id)
            }
        }

        fun register() {
            if (_hasRegister.compareAndSet(false, true)) {
                logMsg { "register BroadcastReceiver" }
                context.registerReceiver(this, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
            }
        }
    }

    private val _activityCallback by lazy {
        object : ActivityCallback() {
            override fun onActivityResumed(activity: Activity) {
                super.onActivityResumed(activity)
                FSocialWechatLoginApi.onActivityResumed(activity)
                FSocialWechatShareApi.onActivityResumed(activity)
            }

            override fun onActivityStopped(activity: Activity) {
                super.onActivityStopped(activity)
                FSocialWechatLoginApi.onActivityStopped(activity)
                FSocialWechatShareApi.onActivityStopped(activity)
            }

            override fun onActivityDestroyed(activity: Activity) {
                super.onActivityDestroyed(activity)
                FSocialWechatLoginApi.onActivityDestroyed(activity)
                FSocialWechatShareApi.onActivityDestroyed(activity)
            }
        }
    }
}

private open class ActivityCallback : Application.ActivityLifecycleCallbacks {
    private val _hasRegister = AtomicBoolean(false)
    private var _topActivityRef: WeakReference<Activity>? = null

    val topActivity: Activity?
        get() = _topActivityRef?.get()

    fun register(application: Application) {
        if (_hasRegister.compareAndSet(false, true)) {
            logMsg { "register ActivityCallback" }
            application.registerActivityLifecycleCallbacks(this)
        }
    }

    private fun saveTopActivity(activity: Activity) {
        val old = _topActivityRef?.get()
        if (old != activity) {
            _topActivityRef = WeakReference(activity)
        }
    }

    override fun onActivityPreCreated(activity: Activity, savedInstanceState: Bundle?) {
        super.onActivityPreCreated(activity, savedInstanceState)
        saveTopActivity(activity)
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
    }

    override fun onActivityStarted(activity: Activity) {
    }

    override fun onActivityPreResumed(activity: Activity) {
        super.onActivityPreResumed(activity)
        saveTopActivity(activity)
    }

    override fun onActivityResumed(activity: Activity) {
    }

    override fun onActivityPaused(activity: Activity) {
    }

    override fun onActivityStopped(activity: Activity) {
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
    }

    override fun onActivityDestroyed(activity: Activity) {
    }
}

internal inline fun logMsg(block: () -> String) {
    if (FSocialWechat.isDebug) {
        Log.i("FSocialWechat", block())
    }
}