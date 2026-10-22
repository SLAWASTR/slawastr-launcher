package dev.plume

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.ViewOutlineProvider
import android.widget.HorizontalScrollView

/**
 * A row that scrolls by whole items: when focus moves past an edge, the focused item lands flush with
 * that edge of the padded area, so the row always shows full tiles and never half-cut ones. The items
 * sit on a fixed grid that exactly fills the area, so aligning one edge aligns them all.
 */
class SnapScrollView(context: Context, attrs: AttributeSet?) : HorizontalScrollView(context, attrs) {
    init {
        // Clip to our own bounds. The parent can't do it: it leaves clipping off so other views can lift.
        outlineProvider = ViewOutlineProvider.BOUNDS
        clipToOutline = true
        clipChildren = false // or the row inside clips the first tile's focus lift to its own edge

    }

    override fun computeScrollDeltaToGetChildRectOnScreen(rect: Rect): Int {
        val left = scrollX + paddingLeft
        val right = scrollX + width - paddingRight
        return when {
            rect.left < left -> rect.left - left
            rect.right > right -> rect.right - right
            else -> 0
        }
    }
}
