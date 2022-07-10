package com.sd.lib.social.wechat.core

import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.model.WechatLoginResult
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

object FSocialWechatLoginApi {
    private val _isLogin = AtomicBoolean(false)
    private var _loginCallback: LoginCallback? = null

    private var _reqId: String = ""
    private var _getToken = false

    @JvmStatic
    @JvmOverloads
    fun login(
        /** 回调对象 */
        callback: LoginCallback,
        /** 授权成功后，是否用code换取token */
        getToken: Boolean = true,
    ) {
        if (_isLogin.compareAndSet(false, true)) {
            _loginCallback = callback
            _getToken = getToken
            with(FSocialWechat.wxapi) {
                val reqId = URLEncoder.encode(UUID.randomUUID().toString()).also {
                    _reqId = it
                }
                val req = SendAuth.Req().apply {
                    scope = "snsapi_userinfo"
                    state = reqId
                }
                sendReq(req);
            }
        }
    }

    internal fun handleResponse(resp: BaseResp) {
        if (resp.type == ConstantsAPI.COMMAND_SENDAUTH) {
            when (resp.errCode) {
                BaseResp.ErrCode.ERR_OK -> {
                    val authResp = resp as SendAuth.Resp
                    if (_reqId == authResp.state) {
                        if (_getToken) {
                            getToken(authResp.code)
                        } else {
                            _loginCallback?.onSuccess(WechatLoginResult(authResp.code, "", ""))
                            resetState()
                        }
                    }
                }
                BaseResp.ErrCode.ERR_USER_CANCEL,
                BaseResp.ErrCode.ERR_AUTH_DENIED,
                -> {
                    _loginCallback?.onCancel()
                    resetState()
                }
                else -> {
                    _loginCallback?.onError(resp.errCode, resp.errStr)
                    resetState()
                }
            }
        }
    }

    private fun getToken(code: String) {
        // TODO 获取token
    }

    private fun resetState() {
        _loginCallback = null
        _reqId = ""
        _isLogin.set(false)
    }

    interface LoginCallback {
        fun onSuccess(result: WechatLoginResult)

        fun onError(code: Int, message: String)

        fun onCancel()
    }
}