package com.umeng.soexample.wxapi

import com.sd.lib.social.wechat.wxapi.FWechatCallbackActivity
import com.tencent.mm.opensdk.modelbase.BaseResp

class WXEntryActivity : FWechatCallbackActivity() {
    override fun onResp(resp: BaseResp) {
        super.onResp(resp)
        finish()
    }
}