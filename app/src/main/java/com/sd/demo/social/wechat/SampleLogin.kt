package com.sd.demo.social.wechat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.sd.demo.social.wechat.theme.AppTheme
import com.sd.lib.social.wechat.core.login.FSocialWechatLoginApi
import com.sd.lib.social.wechat.core.login.FWechatLoginException
import com.sd.lib.social.wechat.core.login.WechatLoginResult

class SampleLogin : ComponentActivity() {
   override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContent {
         AppTheme {
            Content()
         }
      }
   }
}

@Composable
private fun Content(
   modifier: Modifier = Modifier,
) {
   Column(
      modifier = modifier.fillMaxSize(),
      horizontalAlignment = Alignment.CenterHorizontally,
   ) {
      Button(onClick = {
         FSocialWechatLoginApi.login(true, object : FSocialWechatLoginApi.LoginCallback {
            override fun onSuccess(result: WechatLoginResult) {
               logMsg { "onSuccess:$result" }
            }

            override fun onError(exception: FWechatLoginException) {
               logMsg { "onError:$exception" }
            }
         })
      }) {
         Text("login")
      }
   }
}