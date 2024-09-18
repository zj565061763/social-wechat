package com.sd.lib.social.wechat.core.share

open class FWechatShareException(
   message: String? = null,
   cause: Throwable? = null,
) : Exception(message, cause)

/**
 * 正在分享中
 */
class FWechatShareExceptionInShare : FWechatShareException()

/**
 * 取消分享
 */
class FWechatShareExceptionUserCancel : FWechatShareException()

/**
 * 错误码
 */
class FWechatShareExceptionCode(
   val code: Int,
   val msg: String?,
) : FWechatShareException()