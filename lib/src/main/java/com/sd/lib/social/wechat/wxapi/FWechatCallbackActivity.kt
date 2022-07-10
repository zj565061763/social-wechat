package com.sd.lib.social.wechat.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.core.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.FSocialWechatShareApi
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

open class FWechatCallbackActivity : Activity(), IWXAPIEventHandler {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        runCatching {
            FSocialWechat.wxapi.handleIntent(intent, this)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        runCatching {
            FSocialWechat.wxapi.handleIntent(intent, this)
        }
    }

    override fun onReq(req: BaseReq) {
    }

    override fun onResp(resp: BaseResp) {
        FSocialWechatLoginApi.handleResponse(resp)
        FSocialWechatShareApi.handleResponse(resp)
    }
}