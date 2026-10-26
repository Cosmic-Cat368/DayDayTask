package com.example.taskrecall.data.repository

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import coil.annotation.ExperimentalCoilApi
import coil.imageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 规格十六：图片只存相对路径。
 *
 * 用户从系统相册选的图片会被复制到 App 私有目录 files/images/，
 * Markdown 中只写入 images/xxx.jpg。绝对路径永远不会进入数据库或导出文件。
 */
@Singleton
class ImageStorage @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    val imagesDir: File
        get() = File(context.filesDir, IMAGES_DIR_NAME)

    /**
     * 复制一张图片，返回相对路径 images/<uuid>.<ext>。
     * 如果复制失败返回 null，由 UI 提示。
     */
    suspend fun importImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val mime = resolver.getType(uri) ?: "image/jpeg"
        if (!mime.startsWith("image/")) return@withContext null

        val extension = MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime)
            ?.lowercase()
            ?.let { if (it == "jpeg") "jpg" else it }
            ?: "jpg"

        imagesDir.mkdirs()
        val fileName = "img_${UUID.randomUUID()}.$extension"
        val target = File(imagesDir, fileName)
        val temp = File(imagesDir, "$fileName.tmp")

        try {
            resolver.openInputStream(uri)?.use { input ->
                temp.outputStream().use { output -> input.copyTo(output) }
            } ?: return@withContext null
            if (!temp.renameTo(target)) {
                temp.copyTo(target, overwrite = true)
                temp.delete()
            }
            "$IMAGES_DIR_NAME/$fileName"
        } catch (_: Exception) {
            temp.delete()
            target.delete()
            null
        }
    }

    /**
     * 将相对路径安全地解析到私有目录。
     * 只接受 images/ 前缀，并阻止 ../ 越权访问。
     */
    fun resolveRelative(relativePath: String): File? {
        if (!relativePath.startsWith("$IMAGES_DIR_NAME/")) return null
        val root = context.filesDir.canonicalFile
        val candidate = File(root, relativePath).canonicalFile
        val allowedRoot = File(root, IMAGES_DIR_NAME).canonicalFile
        if (!candidate.path.startsWith(allowedRoot.path + File.separator)) return null
        return candidate.takeIf { it.isFile }
    }

    /**
     * 导入时整体替换图片目录。
     * 先复制到临时目录，再替换，降低中途失败导致旧图片全部丢失的概率。
     */
    @OptIn(ExperimentalCoilApi::class)
    suspend fun replaceImages(stagingDir: File?) = withContext(Dispatchers.IO) {
        val target = imagesDir
        val old = File(context.filesDir, "$IMAGES_DIR_NAME.old")
        val fresh = File(context.filesDir, "$IMAGES_DIR_NAME.new")

        old.deleteRecursively()
        fresh.deleteRecursively()
        fresh.mkdirs()

        if (stagingDir != null && stagingDir.isDirectory) {
            stagingDir.copyRecursively(fresh, overwrite = true)
        }

        if (target.exists()) {
            target.renameTo(old)
        }
        if (!fresh.renameTo(target)) {
            fresh.copyRecursively(target, overwrite = true)
            fresh.deleteRecursively()
        }
        old.deleteRecursively()

        // 导入后同路径可能对应新图片，清掉 Coil 缓存避免显示旧图。
        context.imageLoader.memoryCache?.clear()
        context.imageLoader.diskCache?.clear()
    }

    private companion object {
        const val IMAGES_DIR_NAME = "images"
    }
}
