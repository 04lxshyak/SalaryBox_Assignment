package com.salarybox.attendance.camera

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Renders a semi-transparent dark overlay with an oval cutout in the center
 * to guide user face positioning.
 */
@Composable
fun FaceOvalGuideOverlay(
    modifier: Modifier = Modifier,
    borderColor: Color = Color.White,
    strokeWidthDp: Float = 3f,
    maskColor: Color = Color(0x99000000)
) {
    Box(modifier = modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate oval dimensions centered in top 45% of screen
            val ovalWidth = width * 0.70f
            val ovalHeight = ovalWidth * 1.35f
            val ovalLeft = (width - ovalWidth) / 2f
            val ovalTop = (height - ovalHeight) * 0.35f

            val strokeWidthPx = strokeWidthDp.dp.toPx()

            // Draw full semi-transparent mask
            drawRect(color = maskColor)

            // Cut out the oval using BlendMode.Clear
            drawOval(
                color = Color.Transparent,
                topLeft = Offset(ovalLeft, ovalTop),
                size = Size(ovalWidth, ovalHeight),
                blendMode = BlendMode.Clear
            )

            // Draw oval outline border
            drawOval(
                color = borderColor,
                topLeft = Offset(ovalLeft, ovalTop),
                size = Size(ovalWidth, ovalHeight),
                style = Stroke(width = strokeWidthPx)
            )
        }
    }
}
