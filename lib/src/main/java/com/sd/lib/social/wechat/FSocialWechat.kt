package com.sd.lib.social.wechat

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
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

    val wxapi: IWXAPI
        get() {
            synchronized(this@FSocialWechat) {
                val api = _wxapi
                if (api != null) return api

                val id = appId
                return WXAPIFactory.createWXAPI(context, id, true).also {
                    _wxapi = it
                    it.registerApp(id)
                    _broadcastReceiver.register()
                }
            }
        }

    @JvmStatic
    fun init(context: Context, appId: String, appSecret: String) {
        require(appId.isNotEmpty()) { "appId is empty" }
        require(appSecret.isNotEmpty()) { "appSecret is empty" }
        synchronized(this@FSocialWechat) {
            _context = context.applicationContext as Application
            _appSecret = appSecret
            if (_appId != appId) {
                _appId = appId
                _wxapi?.unregisterApp()
                _wxapi = null
            }
        }
    }

    private val _broadcastReceiver = object : BroadcastReceiver() {
        private val _hasRegister = AtomicBoolean(false)

        override fun onReceive(context: Context?, intent: Intent?) {
            val id = _appId
            if (id.isNotEmpty()) {
                _wxapi?.registerApp(id)
            }
        }

        fun register() {
            if (_hasRegister.compareAndSet(false, true)) {
                context.registerReceiver(this, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
            }
        }
    }
}