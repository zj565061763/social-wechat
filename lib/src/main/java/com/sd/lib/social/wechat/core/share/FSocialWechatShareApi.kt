package com.sd.lib.social.wechat.core.share

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import coil.imageLoader
import coil.request.ImageRequest
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.core.FSocialWechatApi
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.UUID

/**
 * 分享
 */
object FSocialWechatShareApi : FSocialWechatApi() {
   private val _coroutineScope = MainScope()

   /** 是否正在分享中 */
   @Volatile
   private var _isShare = false

   private var _callback: ShareCallback? = null
   private var _shouldNotifyWhenResume = false

   /**
    * 分享url
    */
   @JvmStatic
   @JvmOverloads
   fun shareUrl(
      /** 跳转链接 */
      targetUrl: String?,
      /** 标题 */
      title: String?,
      /** 描述 */
      description: String?,
      /** 图片url，在线url或者本地路径 */
      imageUrl: String? = null,
      /** 场景 */
      scene: Int = SendMessageToWX.Req.WXSceneSession,
      /** 回调 */
      callback: ShareCallback,
   ) {
      _coroutineScope.launch {
         if (_isShare) {
            log { "shareUrl canceled in share" }
            callback.onError(FWechatShareExceptionInShare())
            return@launch
         }

         if (!startTrackActivity()) {
            log { "shareUrl canceled track activity failed" }
            callback.onError(FWechatShareException("track activity failed"))
            return@launch
         }

         log { "shareUrl" }
         _isShare = true
         _callback = callback

         val mediaObject = WXWebpageObject().apply {
            this.webpageUrl = targetUrl
         }
         val message = WXMediaMessage().apply {
            this.title = title
            this.description = description
            this.mediaObject = mediaObject
         }
         val req = SendMessageToWX.Req().apply {
            this.transaction = UUID.randomUUID().toString()
            this.message = message
            this.scene = scene
         }

         if (imageUrl.isNullOrEmpty()) {
            FSocialWechat.getWxapi().sendReq(req)
         } else {
            message.thumbData = try {
               downloadImage(imageUrl)
            } catch (e: Throwable) {
               e.printStackTrace()
               log { "downloadImage error $e" }
               null
            }
            FSocialWechat.getWxapi().sendReq(req)
         }
      }
   }

   internal fun handleResponse(resp: BaseResp) {
      if (!_isShare) return
      if (resp.type != ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX) return
      _coroutineScope.launch {
         log { "handleResponse code:${resp.errCode}" }
         when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> notifySuccess(WechatShareResult())
            BaseResp.ErrCode.ERR_USER_CANCEL -> notifyError(FWechatShareExceptionUserCancel())
            else -> notifyError(FWechatShareExceptionCode(resp.errCode, resp.errStr))
         }
      }
   }

   private fun notifySuccess(result: WechatShareResult) {
      log { "notifySuccess" }
      _callback?.onSuccess(result)
      resetState()
   }

   private fun notifyError(exception: FWechatShareException) {
      log { "notifyError:$exception" }
      _callback?.onError(exception)
      resetState()
   }

   private fun resetState() {
      if (!_isShare) return

      log { "resetState" }
      stopTrackActivity()

      _callback = null
      _shouldNotifyWhenResume = false

      _isShare = false
   }

   override fun onTrackActivityStopped() {
      super.onTrackActivityStopped()
      if (_isShare) {
         // 调用分享之后，Activity进入stop，标记为true
         _shouldNotifyWhenResume = true
         log { "mark should notify when resume" }
      }
   }

   override fun onTrackActivityResumed() {
      super.onTrackActivityResumed()
      if (_isShare && _shouldNotifyWhenResume) {
         // 调用分享之后，回到App，但是微信SDK没有回调，此时也通知成功
         _shouldNotifyWhenResume = false
         log { "notify when resume" }
         notifySuccess(WechatShareResult())
      }
   }

   override fun onTrackActivityDestroyed() {
      super.onTrackActivityDestroyed()
      resetState()
   }

   interface ShareCallback {
      fun onSuccess(result: WechatShareResult)
      fun onError(exception: FWechatShareException)
   }
}

private suspend fun downloadImage(url: String): ByteArray? {
   val request = ImageRequest.Builder(FSocialWechat.context)
      .data(url)
      .size(150)
      .build()

   val drawable = FSocialWechat.context
      .imageLoader.execute(request).drawable ?: return null

   return drawable.toBitmap()?.compressToLegalSize()
}

private fun Drawable.toBitmap(): Bitmap? {
   val drawable = this
   if (drawable is BitmapDrawable) return drawable.bitmap

   val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 1
   val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 1

   val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
   val canvas = Canvas(bitmap)

   drawable.setBounds(0, 0, canvas.width, canvas.height)
   drawable.draw(canvas)
   return bitmap
}

private suspend fun Bitmap.compressToLegalSize(): ByteArray {
   return withContext(Dispatchers.IO) {
      val stream = ByteArrayOutputStream()
      for (quality in 100 downTo 0 step 10) {
         stream.reset()
         compress(Bitmap.CompressFormat.WEBP, quality, stream)
         if (stream.size() <= WXMediaMessage.THUMB_LENGTH_LIMIT) {
            break
         }
      }
      stream.toByteArray()
   }
}