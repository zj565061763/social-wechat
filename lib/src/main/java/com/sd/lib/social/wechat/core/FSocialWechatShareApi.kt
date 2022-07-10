package com.sd.lib.social.wechat.core

import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.model.WechatShareResult
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
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
            // TODO 压缩图片
            FSocialWechat.wxapi.sendReq(req)
        }
    }

    internal fun handleResponse(resp: BaseResp) {
        if (resp.type != ConstantsAPI.COMMAND_SENDMESSAGE_TO_WX) {
            // 不是登录授权的结果，不处理
            return
        }
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                notifySuccess(WechatShareResult())
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> notifyCancel()
            else -> notifyError(resp.errCode, resp.errStr)
        }
    }

    private fun notifySuccess(result: WechatShareResult) {
        _shareCallback?.onSuccess(result)
        resetState()
    }

    private fun notifyError(code: Int, message: String) {
        _shareCallback?.onError(code, message)
        resetState()
    }

    private fun notifyCancel() {
        _shareCallback?.onCancel()
        resetState()
    }

    private fun resetState() {
        _shareCallback = null
        _isShare.set(false)
    }

    interface ShareCallback {
        fun onSuccess(result: WechatShareResult)

        fun onError(code: Int, message: String)

        fun onCancel()
    }
}