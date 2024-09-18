package com.sd.lib.social.wechat.core.login

import com.github.kevinsawicki.http.HttpRequest
import com.sd.lib.social.wechat.FSocialWechat
import com.sd.lib.social.wechat.core.FSocialWechatApi
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

/**
 * 登录
 */
object FSocialWechatLoginApi : FSocialWechatApi() {
   private val _coroutineScope = MainScope()

   /** 是否正在登录中 */
   @Volatile
   private var _isLogin = false

   private var _reqId = ""
   private var _getToken = false
   private var _callback: LoginCallback? = null

   private var _appId = ""
   private var _appSecret = ""

   /**
    * 登录
    */
   @JvmStatic
   fun login(
      /** 授权成功后，是否用code换取token */
      getToken: Boolean,
      /** 回调对象 */
      callback: LoginCallback,
   ) {
      _coroutineScope.launch {
         if (_isLogin) {
            log { "login canceled in login" }
            callback.onError(FWechatLoginExceptionInLogin())
            return@launch
         }

         if (!startTrackActivity()) {
            log { "login canceled track activity failed" }
            callback.onError(FWechatLoginException("track activity failed"))
            return@launch
         }

         val reqId = System.currentTimeMillis().toString()
         log { "login getToken:$getToken reqId:$reqId" }

         _isLogin = true
         _reqId = reqId
         _getToken = getToken
         _callback = callback

         with(FSocialWechat) {
            _appId = appId
            _appSecret = appSecret
            getWxapi().sendReq(
               SendAuth.Req().apply {
                  this.scope = "snsapi_userinfo"
                  this.state = reqId
               }
            )
         }
      }
   }

   internal fun handleResponse(resp: BaseResp) {
      if (!_isLogin) return
      if (resp.type != ConstantsAPI.COMMAND_SENDAUTH) return
      _coroutineScope.launch {
         log { "handleResponse code:${resp.errCode}" }
         when (resp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
               val authResp = resp as SendAuth.Resp
               log { "handleResponse OK reqId:${resp.state}" }
               if (authResp.state == _reqId) {
                  val code = authResp.code
                  if (code.isNullOrEmpty()) {
                     notifyError(FWechatLoginException("code is null or empty"))
                  } else {
                     if (_getToken) {
                        getToken(appId = _appId, appSecret = _appSecret, code = code)
                     } else {
                        notifySuccess(WechatLoginResult(code = code, "", ""))
                     }
                  }
               }
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> notifyError(FWechatLoginExceptionUserCancel())
            BaseResp.ErrCode.ERR_AUTH_DENIED -> notifyError(FWechatLoginExceptionAuthDenied())
            else -> notifyError(FWechatLoginExceptionCode(resp.errCode, resp.errStr))
         }
      }
   }

   private suspend fun getToken(
      appId: String,
      appSecret: String,
      code: String,
   ) {
      if (appId.isEmpty()) {
         log { "getToken canceled appId is empty" }
         notifyError(FWechatLoginException("appId is empty"))
         return
      }
      if (appSecret.isEmpty()) {
         log { "getToken canceled appSecret is empty" }
         notifyError(FWechatLoginException("appSecret is empty"))
         return
      }

      log { "getToken" }
      val url = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=${appId}&secret=${appSecret}&code=${code}&grant_type=authorization_code"

      // 创建请求对象
      val request = HttpRequest.get(url).apply {
         trustAllCerts()
         trustAllHosts()
      }

      // 发起请求
      val bodyResult = runCatching {
         withContext(Dispatchers.IO) {
            request.body()
         }
      }

      // 处理请求失败
      bodyResult.onFailure { e ->
         log { "getToken request error:$e" }
         val cause = if (e is HttpRequest.HttpRequestException) {
            e.cause
         } else {
            e
         }
         notifyError(FWechatLoginException(cause = cause))
         return
      }

      // 获取请求的数据
      val body = bodyResult.getOrThrow()
      if (body.isNullOrEmpty()) {
         log { "getToken body is null or empty" }
         notifyError(FWechatLoginException("body is null or empty"))
         return
      }

      // json解析
      val jsonResult = runCatching {
         withContext(Dispatchers.IO) {
            JSONObject(body)
         }
      }

      // 处理解析失败
      jsonResult.onFailure { e ->
         log { "getToken json error:$e" }
         notifyError(FWechatLoginException(cause = e))
         return
      }

      val json = jsonResult.getOrThrow()
      log { "getToken success" }

      notifySuccess(
         WechatLoginResult(
            code = "",
            openId = json.optString("openid"),
            accessToken = json.optString("access_token")
         )
      )

      refreshToken(
         appId = appId,
         refreshToken = json.optString("refresh_token"),
      )
   }

   private suspend fun refreshToken(appId: String, refreshToken: String) {
      if (refreshToken.isEmpty()) {
         log { "refreshToken canceled refreshToken is empty" }
         return
      }

      log { "refreshToken" }
      val url = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=${appId}&grant_type=refresh_token&refresh_token=${refreshToken}"

      val request = HttpRequest.get(url).apply {
         trustAllCerts()
         trustAllHosts()
      }

      try {
         withContext(Dispatchers.IO) {
            request.body().also { log { "refreshToken success" } }
         }
      } catch (e: Throwable) {
         e.printStackTrace()
         log { "refreshToken error $e" }
      }
   }

   private fun notifySuccess(result: WechatLoginResult) {
      log { "notifySuccess" }
      _callback?.onSuccess(result)
      resetState()
   }

   private fun notifyError(exception: FWechatLoginException) {
      log { "notifyError:$exception" }
      _callback?.onError(exception)
      resetState()
   }

   private fun resetState() {
      if (!_isLogin) return

      log { "resetState" }
      stopTrackActivity()

      _reqId = ""
      _getToken = false
      _callback = null

      _appId = ""
      _appSecret = ""

      _isLogin = false
   }

   override fun onTrackActivityDestroyed() {
      super.onTrackActivityDestroyed()
      resetState()
   }

   interface LoginCallback {
      fun onSuccess(result: WechatLoginResult)
      fun onError(exception: FWechatLoginException)
   }
}