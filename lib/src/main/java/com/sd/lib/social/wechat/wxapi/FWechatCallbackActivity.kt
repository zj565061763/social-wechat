package com.sd.lib.social.wechat.wxapi

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.core.login.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.share.FSocialWechatShareApi
import com.sd.lib.social.wechat.logMsg
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler

open class FWechatCallbackActivity : Activity(), IWXAPIEventHandler {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      logMsg { "callback activity onCreate $this" }
      runCatching {
         FSocialWechat.getWxapi().handleIntent(intent, this)
      }
   }

   override fun onNewIntent(intent: Intent?) {
      super.onNewIntent(intent)
      logMsg { "callback activity onNewIntent $this" }
      setIntent(intent)
      runCatching {
         FSocialWechat.getWxapi().handleIntent(intent, this)
      }
   }

   override fun onReq(req: BaseReq) {
      logMsg { "callback activity onReq $this" }
   }

   override fun onResp(resp: BaseResp) {
      logMsg { "callback activity onResp $this" }
      FSocialWechatLoginApi.handleResponse(resp)
      FSocialWechatShareApi.handleResponse(resp)
   }

   override fun onDestroy() {
      super.onDestroy()
      logMsg { "callback activity onDestroy $this" }
   }
}