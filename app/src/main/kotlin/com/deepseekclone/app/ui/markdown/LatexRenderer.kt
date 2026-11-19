package com.deepseekclone.app.ui.markdown

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import ru.noties.jlatexmath.JLatexMathAndroid
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * LaTeX 公式渲染：jlatexmath（Android 移植）→ Bitmap，带 LRU 缓存。
 * 首次调用时初始化字体资源。
 */
object LatexRenderer {

    private var initialized = false
    private val cache = LruCache<String, Bitmap>(48)

    fun render(context: Context, latex: String, textSizePx: Float, color: Int, display: Boolean): Bitmap? {
        if (latex.isBlank()) return null
        ensureInit(context)
        val key = "$latex|${textSizePx.toInt()}|$color|$display"
        cache.get(key)?.let { return it }
        return runCatching {
            val builder = JLatexMathDrawable.builder(latex)
                .textSize(textSizePx)
                .color(color)
                .padding(if (display) 8 else 2)
            val drawable = builder.build()
            val width = drawable.intrinsicWidth.coerceAtLeast(1)
            val height = drawable.intrinsicHeight.coerceAtLeast(1)
            // 过大的公式做上限保护，避免 OOM
            if (width > 4000 || height > 2000) return null
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(Canvas(bitmap))
            bitmap
        }.getOrNull()?.also { cache.put(key, it) }
    }

    @Synchronized
    private fun ensureInit(context: Context) {
        if (initialized) return
        runCatching { JLatexMathAndroid.init(context.applicationContext) }
        initialized = true
    }
}
