package com.sd.demo.social.wechat.activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.sd.demo.social.wechat.databinding.ActivityMainBinding
import com.sd.lib.social.wechat.core.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.FSocialWechatShareApi
import com.sd.lib.social.wechat.model.WechatLoginResult
import com.sd.lib.social.wechat.model.WechatShareResult

class MainActivity : AppCompatActivity() {
    private val _binding by lazy { ActivityMainBinding.inflate(LayoutInflater.from(this)) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(_binding.root)

        // 登录
        _binding.btnLogin.setOnClickListener {
            Log.i(TAG, "click login")
            FSocialWechatLoginApi.login(object : FSocialWechatLoginApi.LoginCallback {
                override fun onSuccess(result: WechatLoginResult) {
                    Log.i(TAG, "login onSuccess $result")
                }

                override fun onError(code: Int, message: String) {
                    Log.i(TAG, "login onError $code $message")
                }

                override fun onCancel() {
                    Log.i(TAG, "login onCancel")
                }
            })
        }

        // 分享
        _binding.btnShare.setOnClickListener {
            Log.i(TAG, "click share")
            FSocialWechatShareApi.shareUrl(
                targetUrl = "http://www.baidu.com",
                title = "我是标题",
                description = "我是描述",
                callback = object : FSocialWechatShareApi.ShareCallback {
                    override fun onSuccess(result: WechatShareResult) {
                        Log.i(TAG, "share onSuccess $result")
                    }

                    override fun onError(code: Int, message: String) {
                        Log.i(TAG, "share onError $code $message")
                    }

                    override fun onCancel() {
                        Log.i(TAG, "share onCancel")
                    }
                }
            )
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}