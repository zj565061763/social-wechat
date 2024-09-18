package com.sd.lib.social.wechat.core.login

open class FWechatLoginException(
   message: String? = null,
   cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 正在登录中
 */
class FWechatLoginExceptionInLogin : FWechatLoginException()

/**
 * 取消登录
 */
class FWechatLoginExceptionUserCancel : FWechatLoginException()

/**
 * 拒绝授权
 */
class FWechatLoginExceptionAuthDenied : FWechatLoginException()

/**
 * 错误码
 */
class FWechatLoginExceptionCode(
   val code: Int,
   val msg: String?,
) : FWechatLoginException()