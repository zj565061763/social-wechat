package com.sd.lib.social.wechat.core

import com.github.kevinsawicki.http.HttpRequest
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.logMsg
import com.sd.lib.social.wechat.model.WechatLoginResult
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.util.concurrent.atomic.AtomicBoolean

/**
 * 登录
 */
object FSocialWechatLoginApi : FSocialWechatApi() {
    private val _isLogin = AtomicBoolean(false)

    private var _loginCallback: LoginCallback? = null
    private var _getToken = false
    private var _reqId = ""

    private var _appId = ""
    private var _appSecret = ""

    private val _coroutineScope = MainScope()

    /** 是否正在登录 */
    val isLogin: Boolean
        get() = _isLogin.get()

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
            logMsg { "FSocialWechatLoginApi login getToken:$getToken" }
            _loginCallback = callback
            _getToken = getToken
            _reqId = System.currentTimeMillis().toString()

            if (!startTrackActivity()) {
                // 如果追踪Activity失败，则通知取消并返回
                logMsg { "FSocialWechatLoginApi login track activity failed" }
                notifyCancel()
                return
            }

            with(FSocialWechat) {
                _appId = appId
                _appSecret = appSecret

                val req = SendAuth.Req().apply {
                    this.scope = "snsapi_userinfo"
                    this.state = _reqId
                }
                wxapi.sendReq(req)
            }
        } else {
            callback.onCancel()
        }
    }

    internal fun handleResponse(resp: BaseResp) {
        if (!isLogin) return
        if (resp.type != ConstantsAPI.COMMAND_SENDAUTH) return

        logMsg { "FSocialWechatLoginApi handleResponse code:${resp.errCode} isLogin:$isLogin" }
        when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val authResp = resp as SendAuth.Resp
                if (authResp.state == _reqId) {
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
        logMsg { "FSocialWechatLoginApi getToken" }
        val url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=${_appId}&secret=${_appSecret}&code=${code}&grant_type=authorization_code"

        _coroutineScope.launch {
            val request = HttpRequest.get(url).apply {
                trustAllCerts()
                trustAllHosts()
            }

            val response = withContext(Dispatchers.IO) {
                val body = try {
                    request.body().also { logMsg { "FSocialWechatLoginApi getToken success" } }
                } catch (e: HttpRequest.HttpRequestException) {
                    e.printStackTrace()
                    logMsg { "FSocialWechatLoginApi getToken error $e" }
                    null
                }

                if (body == null) {
                    null
                } else {
                    try {
                        JSONObject(body).also { logMsg { "FSocialWechatLoginApi getToken parse json success" } }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                        logMsg { "FSocialWechatLoginApi getToken parse json error $e" }
                        null
                    }
                }
            }

            ensureActive()
            if (!isLogin) return@launch

            if (response == null) {
                notifyError(-1, "error get token")
            } else {
                val refreshToken = response.optString("refresh_token")
                refreshToken(_appId, refreshToken)

                val loginResult = WechatLoginResult(
                    code = "",
                    openId = response.optString("openid"),
                    accessToken = response.optString("access_token")
                )
                notifySuccess(loginResult)
            }
        }
    }

    private fun refreshToken(appId: String, refreshToken: String) {
        if (appId.isEmpty()) return
        if (refreshToken.isEmpty()) return

        logMsg { "FSocialWechatLoginApi refreshToken" }
        val url = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=${appId}&grant_type=refresh_token&refresh_token=${refreshToken}"

        _coroutineScope.launch {
            val request = HttpRequest.get(url).apply {
                trustAllCerts()
                trustAllHosts()
            }
            withContext(Dispatchers.IO) {
                try {
                    request.body().also { logMsg { "FSocialWechatLoginApi refreshToken success" } }
                } catch (e: HttpRequest.HttpRequestException) {
                    e.printStackTrace()
                    logMsg { "FSocialWechatLoginApi refreshToken error $e" }
                }
            }
        }
    }

    private fun notifySuccess(result: WechatLoginResult) {
        logMsg { "FSocialWechatLoginApi notifySuccess" }
        _loginCallback?.onSuccess(result)
        resetState()
    }

    private fun notifyError(code: Int, message: String) {
        logMsg { "FSocialWechatLoginApi notifyError code:$code message:$message" }
        _loginCallback?.onError(code, message)
        resetState()
    }

    private fun notifyCancel() {
        logMsg { "FSocialWechatLoginApi notifyCancel" }
        _loginCallback?.onCancel()
        resetState()
    }

    private fun resetState() {
        if (_isLogin.get()) {
            logMsg { "FSocialWechatLoginApi resetState" }
            stopTrackActivity()

            _loginCallback = null
            _getToken = false
            _reqId = ""

            _appId = ""
            _appSecret = ""

            _isLogin.set(false)
        }
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