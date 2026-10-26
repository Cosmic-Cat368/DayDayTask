package com.example.taskrecall

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.taskrecall.ui.TaskRecallApp
import com.example.taskrecall.ui.theme.TaskRecallTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * 唯一的 Activity。
 *
 * 规格：单 Activity + Compose。
 * 同时处理文件管理器/微信通过 ACTION_VIEW 发来的 zip 备份；
 * 先交给 TaskRecallApp 弹覆盖确认，确认后才导入。
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var pendingImportUri by mutableStateOf<Uri?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingImportUri = extractImportUri(intent)

        setContent {
            TaskRecallTheme {
                TaskRecallApp(
                    initialImportUri = pendingImportUri,
                    onImportUriConsumed = {
                        pendingImportUri = null
                        // 清掉启动 Intent，避免旋转屏幕后再次弹出导入确认。
                        setIntent(Intent(this, MainActivity::class.java))
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingImportUri = extractImportUri(intent)
    }

    private fun extractImportUri(intent: Intent?): Uri? {
        if (intent?.action != Intent.ACTION_VIEW) return null
        return intent.data
    }
}
