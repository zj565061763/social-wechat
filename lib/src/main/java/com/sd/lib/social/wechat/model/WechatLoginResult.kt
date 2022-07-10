package com.sd.lib.social.wechat.model

data class WechatLoginResult(
    val code: String,
    val openId: String,
    val accessToken: String,
)