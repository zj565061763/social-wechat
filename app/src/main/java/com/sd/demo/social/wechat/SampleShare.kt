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
import com.sd.lib.social.wechat.core.share.FSocialWechatShareApi
import com.sd.lib.social.wechat.core.share.FWechatShareException
import com.sd.lib.social.wechat.core.share.WechatShareResult

class SampleShare : ComponentActivity() {
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
         FSocialWechatShareApi.shareUrl(
            targetUrl = "https://www.baidu.com",
            title = "title",
            description = "description",
            imageUrl = "https://www.baidu.com/img/PCtm_d9c8750bed0b3c7d089fa7d55720d6cf.png",
            callback = object : FSocialWechatShareApi.ShareCallback {
               override fun onSuccess(result: WechatShareResult) {
                  logMsg { "onSuccess:$result" }
               }

               override fun onError(exception: FWechatShareException) {
                  logMsg { "onError:$exception" }
               }
            })
      }) {
         Text("shareUrl")
      }
   }
}