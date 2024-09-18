package com.sd.lib.social.wechat

import android.app.Activity
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import com.sd.lib.social.wechat.core.login.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.share.FSocialWechatShareApi
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import java.lang.ref.WeakReference

object FSocialWechat {
   private var _context: Application? = null
   private var _register: WechatRegister? = null

   internal val context: Context
      get() = _context ?: synchronized(FSocialWechat) {
         checkNotNull(_context) { "You should init before this" }
      }

   internal val appId: String
      get() = _register?.appId ?: synchronized(FSocialWechat) {
         checkNotNull(_register?.appId) { "You should init before this" }
      }

   internal val appSecret: String
      get() = _register?.appSecret ?: synchronized(FSocialWechat) {
         checkNotNull(_register?.appSecret) { "You should init before this" }
      }

   internal val topActivity: Activity?
      get() = _activityCallback.topActivity

   /** 是否调试模式 */
   @JvmStatic
   var debug = false

   /**
    * 初始化
    */
   @JvmStatic
   fun init(
      context: Context,
      appId: String?,
      appSecret: String?,
   ) {
      require(!appId.isNullOrEmpty()) { "appId is null or empty" }
      require(!appSecret.isNullOrEmpty()) { "appSecret is null or empty" }
      synchronized(FSocialWechat) {
         logMsg { "init" }
         val application = context.applicationContext as Application
         _context = application

         _register?.let {
            if (it.appId == appId) {
               return
            } else {
               it.unregisterApp()
            }
         }

         WechatRegister(application, appId, appSecret).also {
            _register = it
            it.registerApp()
         }

         _broadcastReceiver.register()
         _activityCallback.register(application)
      }
   }

   @JvmStatic
   fun getWxapi(): IWXAPI {
      return _register?.wxapi ?: synchronized(FSocialWechat) {
         checkNotNull(_register?.wxapi)
      }
   }

   /**
    * 微信是否已安装
    */
   fun isWechatInstalled(): Boolean {
      return getWxapi().isWXAppInstalled
   }

   private val _broadcastReceiver = object : BroadcastReceiver() {
      private var _hasRegister = false

      override fun onReceive(context: Context?, intent: Intent?) {
         synchronized(FSocialWechat) {
            _register?.registerApp()
         }
      }

      fun register() {
         if (!_hasRegister) {
            _hasRegister = true
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

private class WechatRegister(
   context: Context,
   val appId: String,
   val appSecret: String,
) {
   init {
      require(appId.isNotEmpty())
      require(appSecret.isNotEmpty())
   }

   val wxapi: IWXAPI = WXAPIFactory.createWXAPI(context, appId, true)

   fun registerApp() {
      logMsg { "registerApp" }
      wxapi.registerApp(appId)
   }

   fun unregisterApp() {
      logMsg { "unregisterApp" }
      wxapi.unregisterApp()
   }
}

private open class ActivityCallback : Application.ActivityLifecycleCallbacks {
   private var _hasRegister = false
   private var _topActivityRef: WeakReference<Activity>? = null

   val topActivity: Activity?
      get() = _topActivityRef?.get()

   fun register(application: Application) {
      if (!_hasRegister) {
         _hasRegister = true
         application.registerActivityLifecycleCallbacks(this)
      }
   }

   override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
      _topActivityRef = WeakReference(activity)
   }

   override fun onActivityStarted(activity: Activity) {
   }

   override fun onActivityResumed(activity: Activity) {
      _topActivityRef = WeakReference(activity)
   }

   override fun onActivityPaused(activity: Activity) {
   }

   override fun onActivityStopped(activity: Activity) {
      if (_topActivityRef?.get() === activity) {
         _topActivityRef = null
      }
   }

   override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
   }

   override fun onActivityDestroyed(activity: Activity) {
   }
}

internal inline fun logMsg(block: () -> String) {
   if (FSocialWechat.debug) {
      Log.i("FSocialWechat", block())
   }
}