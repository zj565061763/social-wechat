package com.sd.lib.social.wechat.core

import com.github.kevinsawicki.http.HttpRequest
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.model.WechatLoginResult
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 登录
 */
object FSocialWechatLoginApi : FSocialWechatApi() {
    private val _isLogin = AtomicBoolean(false)
    private var _loginCallback: LoginCallback? = null
    private var _reqId: String = ""
    private var _getToken = false

    private val _coroutineScope = MainScope()

    /**
     * 登录
     */
    @JvmStatic
    @JvmOverloads
    fun login(
        /** 回调对象 */
        callback: LoginCallback,
        /** 授权成功后，是否用code换取token */
        getToken: Boolean = true,
    ) {
        if (_isLogin.compareAndSet(false, true)) {
            trackActivity()
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
                sendReq(req)
            }
        }
    }

    internal fun handleResponse(resp: BaseResp) {
        if (resp.type != ConstantsAPI.COMMAND_SENDAUTH) {
            // 不是登录授权结果，不处理
            return
        }
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val authResp = resp as SendAuth.Resp
                if (_reqId == authResp.state) {
                    if (_getToken) {
                        getToken(authResp.code)
                    } else {
                        notifySuccess(WechatLoginResult(authResp.code, "", ""))
                    }
                } else {
                    notifyError(-1, "unknown state ${authResp.state}")
                }
            }
            BaseResp.ErrCode.ERR_USER_CANCEL,
            BaseResp.ErrCode.ERR_AUTH_DENIED,
            -> {
                notifyCancel()
            }
            else -> notifyError(resp.errCode, resp.errStr)
        }
    }

    private fun getToken(code: String) {
        val url = with(FSocialWechat) {
            "https://api.weixin.qq.com/sns/oauth2/access_token?appid=${appId}&secret=${appSecret}&code=${code}&grant_type=authorization_code"
        }
        _coroutineScope.launch {
            val request = HttpRequest.get(url).apply {
                trustAllCerts()
                trustAllHosts()
            }
            withContext(Dispatchers.IO) {
                try {
                    val body = request.body()
                    JSONObject(body)
                } catch (e: HttpRequest.HttpRequestException) {
                    e.printStackTrace()
                    null
                }
            }.let { response ->
                if (response == null) {
                    notifyError(-1, "error get token")
                } else {
                    with(response) {
                        WechatLoginResult(
                            code = code,
                            openId = optString("openid"),
                            accessToken = optString("access_token")
                        )
                    }.let {
                        notifySuccess(it)
                        val refreshToken = response.optString("refresh_token")
                        refreshToken(refreshToken)
                    }
                }
            }
        }
    }

    private fun refreshToken(refreshToken: String) {
        if (refreshToken.isEmpty()) return
        val url = with(FSocialWechat) {
            "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=${appId}&grant_type=refresh_token&refresh_token=${refreshToken}"
        }
        _coroutineScope.launch {
            val request = HttpRequest.get(url).apply {
                trustAllCerts()
                trustAllHosts()
            }
            withContext(Dispatchers.IO) {
                try {
                    request.body().also {
                        it.length
                    }
                } catch (e: HttpRequest.HttpRequestException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun notifySuccess(result: WechatLoginResult) {
        _loginCallback?.onSuccess(result)
        resetState()
    }

    private fun notifyError(code: Int, message: String) {
        _loginCallback?.onError(code, message)
        resetState()
    }

    private fun notifyCancel() {
        _loginCallback?.onCancel()
        resetState()
    }

    private fun resetState() {
        _loginCallback = null
        _reqId = ""
        _getToken = false
        _isLogin.set(false)
    }

    override fun onTrackActivityDestroyed() {
        super.onTrackActivityDestroyed()
        resetState()
    }

    interface LoginCallback {
        fun onSuccess(result: WechatLoginResult)

        fun onError(code: Int, message: String)

        fun onCancel()
    }
}