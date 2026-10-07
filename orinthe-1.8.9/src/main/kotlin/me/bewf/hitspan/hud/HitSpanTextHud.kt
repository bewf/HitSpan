package me.bewf.hitspan.hud

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import org.polyfrost.compose.composables.PolyBox
import org.polyfrost.compose.composables.PolyMcText
import org.polyfrost.compose.composables.PolyModifier
import org.polyfrost.compose.composables.PolyRow
import org.polyfrost.compose.composables.PolyText
import org.polyfrost.compose.composables.align
import org.polyfrost.compose.composables.padding
import org.polyfrost.compose.composables.size
import org.polyfrost.compose.layout.PolyInsets
import org.polyfrost.compose.render.PolyColor
import org.polyfrost.oneconfig.api.hud.v1.Font
import org.polyfrost.oneconfig.api.hud.v1.Hud
import org.polyfrost.oneconfig.api.hud.v1.HudManager
import org.polyfrost.oneconfig.api.hud.v1.TextHud

abstract class HitSpanTextHud(id: String, title: String, defaultLabel: String) :
    TextHud(id, title, Hud.Category.COMBAT, defaultLabel, "") {

    class Shown @JvmOverloads constructor(val value: String, val color: PolyColor? = null)

    private data class ColorSpec(val argb: Int, val chroma: Boolean, val chromaSpeed: Float)

    private data class Model(
        val pre: String,
        val value: String,
        val post: String,
        val valueColor: ColorSpec?,
    )

    private var modelState: MutableState<Model> = mutableStateOf(EMPTY)
    private var visibleNow: Boolean = false

    protected abstract fun compute(example: Boolean): Shown?

    override fun getText(): String? = null

    override fun update(): Boolean {
        val example = HudManager.isEditing || !isReal
        val shown = try {
            compute(example)
        } catch (t: Throwable) {
            null
        }

        if (shown == null) {
            visibleNow = false
            modelState.value = EMPTY
            return true
        }

        val c = shown.color
        modelState.value = Model(
            pre = (if (brackets) "[" else "") + prefix,
            value = shown.value,
            post = suffix + (if (brackets) "]" else ""),
            valueColor = c?.let { ColorSpec(it.rawArgb, it.chroma, it.chromaSpeed) },
        )
        visibleNow = true
        return true
    }

    override fun shouldShow(): Boolean = visibleNow

    override fun clone(): Hud = (super.clone() as HitSpanTextHud).also {
        it.modelState = mutableStateOf(EMPTY)
        it.visibleNow = false
    }

    @Composable
    override fun Content() {
        val model = modelState.value
        val scale = textScale
        val fg = PolyColor(textColor, textChroma, textChromaSpeed)

        val padInsets = PolyInsets(padLeft, padTop, padRight, padBottom)
        val isStaticValid = staticWidth && staticW > 0f && staticH > 0f

        val bg = hudBackground()
        val outer =
            if (isStaticValid) bg.size(staticW, staticH).padding(padInsets)
            else bg.padding(padInsets)

        PolyBox(modifier = outer) {
            val inner = if (isStaticValid) PolyModifier.align(alignment) else PolyModifier
            PolyRow(modifier = inner) {
                if (model.pre.isNotEmpty()) Segment(model.pre, fg, scale)
                if (model.value.isNotEmpty()) {
                    val vc = model.valueColor
                    val color = if (vc != null) PolyColor(vc.argb, vc.chroma, vc.chromaSpeed) else fg
                    Segment(model.value, color, scale)
                }
                if (model.post.isNotEmpty()) Segment(model.post, fg, scale)
            }
        }
    }

    @Composable
    private fun Segment(raw: String, color: PolyColor, scale: Float) {
        val text = when (caseType) {
            1 -> raw.uppercase()
            2 -> raw.lowercase()
            else -> raw
        }
        val mod = PolyModifier.align(org.polyfrost.compose.layout.PolyAlign.Left)

        if (font == Font.Poppins) {
            PolyText(
                text = text,
                color = color,
                fontSize = 8f * scale,
                shadow = showShadow,
                shadowColor = PolyColor(shadowColor, shadowChroma, shadowChromaSpeed),
                shadowOffset = shadowOffsetX,
                font = getPoppinsFontName(),
                modifier = mod,
            )
        } else {
            val formatted = buildString {
                if (textBold) append("§l")
                if (textItalic) append("§o")
                if (textUnderline) append("§n")
                append(text)
                if (textBold || textItalic || textUnderline) append("§r")
            }
            PolyMcText(
                text = formatted,
                color = color,
                shadow = showShadow,
                scale = scale,
                modifier = mod,
            )
        }
    }

    private companion object {
        val EMPTY = Model("", "", "", null)
    }
}
