package com.sd.lib.social.wechat.core

import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.imageLoader
import coil.request.ImageRequest
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.logMsg
import com.sd.lib.social.wechat.model.WechatShareResult
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
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 分享
 */
object FSocialWechatShareApi : FSocialWechatApi() {
    private val _isShare = AtomicBoolean(false)
    private var _shareCallback: ShareCallback? = null

    private val _coroutineScope = MainScope()
    private var _shouldCheckWhenResume = false

    /**
     * 分享url
     */
    fun shareUrl(
        /** 跳转链接 */
        targetUrl: String,
        /** 标题 */
        title: String,
        /** 描述 */
        description: String,
        /** 图片url，在线url或者本地路径 */
        imageUrl: String = "",
        /** 是否发送到朋友圈 */
        isTimeline: Boolean = false,
        /** 回调 */
        callback: ShareCallback,
    ) {
        shareUrlInternal(
            scene = if (isTimeline) SendMessageToWX.Req.WXSceneTimeline else SendMessageToWX.Req.WXSceneSession,
            targetUrl = targetUrl,
            title = title,
            description = description,
            imageUrl = imageUrl,
            callback = callback,
        )
    }

    /**
     * 分享url
     */
    private fun shareUrlInternal(
        scene: Int,
        /** 跳转链接 */
        targetUrl: String,
        /** 标题 */
        title: String,
        /** 描述 */
        description: String,
        /** 图片url，在线url或者本地路径 */
        imageUrl: String = "",
        /** 回调 */
        callback: ShareCallback,
    ) {
        if (_isShare.compareAndSet(false, true)) {
            logMsg { "FSocialWechatShareApi shareUrl scene:$scene hasImage:${imageUrl.isNotEmpty()}" }
            startTrackActivity()
            _shareCallback = callback
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

            if (imageUrl.isEmpty()) {
                FSocialWechat.wxapi.sendReq(req)
            } else {
                _coroutineScope.launch {
                    message.thumbData = try {
                        downloadImage(imageUrl).also {
                            logMsg { "FSocialWechatShareApi downloadImage success" }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        logMsg { "FSocialWechatShareApi downloadImage error $e" }
                        null
                    }
                    FSocialWechat.wxapi.sendReq(req)
                }
            }
        }
    }

    internal fun handleResponse(resp: BaseResp) {
        if (resp.type != ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX) {
            // 不是分享消息结果，不处理
            return
        }

        logMsg { "FSocialWechatShareApi handleResponse code:${resp.errCode}" }
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                notifySuccess(WechatShareResult())
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> notifyCancel()
            else -> notifyError(resp.errCode, resp.errStr)
        }
    }

    private fun notifySuccess(result: WechatShareResult) {
        logMsg { "FSocialWechatShareApi notifySuccess" }
        _shareCallback?.onSuccess(result)
        resetState()
    }

    private fun notifyError(code: Int, message: String) {
        logMsg { "FSocialWechatShareApi notifyError code:$code message:$message" }
        _shareCallback?.onError(code, message)
        resetState()
    }

    private fun notifyCancel() {
        logMsg { "FSocialWechatShareApi notifyCancel" }
        _shareCallback?.onCancel()
        resetState()
    }

    private fun resetState() {
        if (_isShare.get()) {
            logMsg { "FSocialWechatShareApi resetState" }
            stopTrackActivity()
            _shareCallback = null
            _shouldCheckWhenResume = false
            _isShare.set(false)
        }
    }

    override fun onTrackActivityStopped() {
        super.onTrackActivityStopped()
        if (_isShare.get()) {
            // 调用分享之后，Activity进入stop，标记为true
            _shouldCheckWhenResume = true
            logMsg { "FSocialWechatShareApi mark should check when resume" }
        }
    }

    override fun onTrackActivityResumed() {
        super.onTrackActivityResumed()
        if (_isShare.get() && _shouldCheckWhenResume) {
            // 调用分享之后，回到App，但是微信SDK没有回调，此时也通知成功
            _shouldCheckWhenResume = false
            logMsg { "FSocialWechatShareApi check resume" }
            notifySuccess(WechatShareResult())
        }
    }

    override fun onTrackActivityDestroyed() {
        super.onTrackActivityDestroyed()
        resetState()
    }

    interface ShareCallback {
        fun onSuccess(result: WechatShareResult)

        fun onError(code: Int, message: String)

        fun onCancel()
    }
}

private suspend fun downloadImage(url: String): ByteArray? {
    val request = ImageRequest.Builder(FSocialWechat.context)
        .data(url)
        .size(150, 150)
        .build()
    val imageLoader = FSocialWechat.context.imageLoader
    val drawable = imageLoader.execute(request).drawable ?: return null
    val bitmap = if (drawable is BitmapDrawable) {
        drawable.bitmap
    } else {
        null
    } ?: return null

    return bitmap.compressToLegalSize()
}

private suspend fun Bitmap.compressToLegalSize(): ByteArray {
    return withContext(Dispatchers.IO) {
        val baos = ByteArrayOutputStream()
        for (quality in 100 downTo 0 step 10) {
            baos.reset()
            compress(Bitmap.CompressFormat.WEBP, quality, baos)
            if (baos.size() <= WXMediaMessage.THUMB_LENGTH_LIMIT) {
                break
            }
        }
        baos.toByteArray()
    }
}