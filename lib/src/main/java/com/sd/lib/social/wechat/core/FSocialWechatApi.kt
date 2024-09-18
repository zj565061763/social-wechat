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
      log { "startTrackActivity $activity" }
      return true
   }

   /**
    * 停止追踪Activity
    */
   protected fun stopTrackActivity() {
      if (_trackActivityRef != null) {
         _trackActivityRef = null
         log { "stopTrackActivity" }
      }
   }

   internal fun onActivityResumed(activity: Activity) {
      if (_trackActivityRef?.get() === activity) {
         log { "onTrackActivityResumed" }
         onTrackActivityResumed()
      }
   }

   internal fun onActivityStopped(activity: Activity) {
      if (_trackActivityRef?.get() === activity) {
         log { "onTrackActivityStopped" }
         onTrackActivityStopped()
      }
   }

   internal fun onActivityDestroyed(activity: Activity) {
      if (_trackActivityRef?.get() === activity) {
         log { "onTrackActivityDestroyed" }
         stopTrackActivity()
         onTrackActivityDestroyed()
      }
   }

   protected open fun onTrackActivityResumed() {
   }

   protected open fun onTrackActivityStopped() {
   }

   protected open fun onTrackActivityDestroyed() {
   }

   internal inline fun log(block: () -> String) {
      logMsg {
         "${javaClass.simpleName} ${block()}"
      }
   }
}