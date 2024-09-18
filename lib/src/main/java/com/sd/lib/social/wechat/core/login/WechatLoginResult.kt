package com.sd.lib.social.wechat.core.login

data class WechatLoginResult(
   val code: String,
   val openId: String,
   val accessToken: String,
)