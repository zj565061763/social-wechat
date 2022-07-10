package com.sd.lib.social.wechat

import android.content.Context
import androidx.core.content.FileProvider

class SocialWechatFileProvider : FileProvider() {
    companion object {
        @JvmStatic
        fun getAuthority(context: Context): String {
            return "${context.packageName}.f-fp-lib-social-wechat"
        }
    }
}