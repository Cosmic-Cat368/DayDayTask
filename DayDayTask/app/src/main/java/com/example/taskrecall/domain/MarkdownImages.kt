package com.example.taskrecall.domain

/**
 * Markdown 图片路径处理。
 *
 * 规格十六：数据库里只存 images/xxx 这样的相对路径。
 * 渲染 Markwon 时临时改成自定义 scheme，由 LocalImageSchemeHandler
 * 拼接到 App 私有目录；这个转换只发生在内存中。
 */
object MarkdownImages {

    private val imagePattern =
        Regex("""!\[[^\]]*]\(\s*(images/[^\s)]+)(?:\s+"[^"]*")?\s*\)""")

    const val LOCAL_SCHEME = "taskrecall"
    const val LOCAL_AUTHORITY = "local"

    /** 收集 Markdown 中被引用的图片相对路径，用于导出 zip。 */
    fun referencedPaths(markdown: String?): List<String> {
        if (markdown.isNullOrBlank()) return emptyList()
        return imagePattern.findAll(markdown)
            .map { match -> match.groupValues[1] }
            .distinct()
            .toList()
    }

    /**
     * 把 ![图](images/a.jpg) 临时改成 ![图](taskrecall://local/images/a.jpg)。
     * 自定义 scheme 不会离开 App，也不会触发网络请求。
     */
    fun forMarkwon(markdown: String): String = imagePattern.replace(markdown) { match ->
        val path = match.groupValues[1]
        match.value.replaceFirst(
            oldValue = path,
            newValue = "$LOCAL_SCHEME://$LOCAL_AUTHORITY/$path",
        )
    }
}
