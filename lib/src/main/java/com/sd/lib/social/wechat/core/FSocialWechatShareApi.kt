package com.sd.lib.social.wechat.core

import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.model.WechatShareResult
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage
import com.tencent.mm.opensdk.modelmsg.WXWebpageObject
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 分享
 */
object FSocialWechatShareApi {
    private val _isShare = AtomicBoolean(false)
    private var _shareCallback: ShareCallback? = null

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
        /** 回调 */
        callback: ShareCallback,
    ) {
        if (_isShare.compareAndSet(false, true)) {
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
            FSocialWechat.wxapi.sendReq(req)
        }
    }

    interface ShareCallback {
        fun onSuccess(result: WechatShareResult)

        fun onError(code: Int, message: String)

        fun onCancel()
    }
}