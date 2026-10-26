package com.example.taskrecall.ui.detail

import android.graphics.drawable.Drawable
import android.net.Uri
import android.webkit.MimeTypeMap
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import coil.imageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.example.taskrecall.domain.MarkdownImages
import io.noties.markwon.Markwon
import io.noties.markwon.image.ImageItem
import io.noties.markwon.image.ImagesPlugin
import io.noties.markwon.image.MediaDecoder
import io.noties.markwon.image.SchemeHandler
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import kotlinx.coroutines.runBlocking

/**
 * Markwon + Coil 图片支持。
 *
 * Markwon 官方 image-coil 4.6.2 依赖 Coil 0.x，与项目的 Coil 2.7 不兼容。
 * 因此这里用 Markwon 的 image 模块开放接口自己实现：
 * - SchemeHandler 把 taskrecall://local/images/xxx 映射到 App 私有目录；
 * - MediaDecoder 用 Coil 2 解码图片字节。
 * 数据里始终只有相对路径，绝对路径只存在于内存中的临时 URI。
 */
@Composable
fun MarkdownText(
    markdown: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val textColor = MaterialTheme.colorScheme.onSurface
    val linkColor = MaterialTheme.colorScheme.primary

    val markwon = remember(context, textColor, linkColor) {
        Markwon.builder(context)
            .usePlugin(
                ImagesPlugin.create { plugin ->
                    plugin.addSchemeHandler(LocalImageSchemeHandler(context))
                    plugin.defaultMediaDecoder(CoilMediaDecoder(context))
                },
            )
            .build()
    }

    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { viewContext ->
            TextView(viewContext).apply {
                setTextColor(textColor.toArgb())
                setLinkTextColor(linkColor.toArgb())
                textSize = 16f
                setLineSpacing(0f, 1.18f)
                setPadding(0, 0, 0, 0)
                movementMethod = android.text.method.LinkMovementMethod.getInstance()
                setTextIsSelectable(true)
            }
        },
        update = { textView ->
            val rendered = MarkdownImages.forMarkwon(markdown)
            markwon.setMarkdown(textView, rendered)
        },
    )
}

private class LocalImageSchemeHandler(
    private val context: android.content.Context,
) : SchemeHandler() {

    override fun supportedSchemes(): Collection<String> = setOf(MarkdownImages.LOCAL_SCHEME)

    override fun handle(source: String, uri: Uri): ImageItem {
        val relativePath = uri.path?.trimStart('/')
            ?: throw IllegalStateException("Invalid local image uri: $source")
        if (!relativePath.startsWith("images/")) {
            throw IllegalStateException("Only images/ relative paths are allowed")
        }

        val root = File(context.filesDir, "images").canonicalFile
        val file = File(context.filesDir, relativePath).canonicalFile
        if (!file.path.startsWith(root.path + File.separator) || !file.isFile) {
            throw IllegalStateException("Local image not found: $relativePath")
        }

        val extension = file.extension.lowercase()
        val mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: "image/*"
        return ImageItem.withDecodingNeeded(mime, file.inputStream())
    }
}

private class CoilMediaDecoder(
    private val context: android.content.Context,
) : MediaDecoder() {

    override fun decode(contentType: String?, inputStream: InputStream): Drawable {
        val bytes = inputStream.use { it.readBytes() }
        val request = ImageRequest.Builder(context)
            .data(ByteBuffer.wrap(bytes))
            .allowHardware(false)
            .build()
        val result = runBlocking { context.imageLoader.execute(request) }
        return (result as? SuccessResult)?.drawable
            ?: throw IllegalStateException("Coil failed to decode local image")
    }

    /** 返回空集合表示作为默认解码器处理所有类型。 */
    override fun supportedTypes(): Collection<String> = emptyList()
}
