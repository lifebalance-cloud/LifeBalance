package com.example.mylife.lifebalance.ui.components

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Text

class TextResizeController(initial: TextUnit) {
    var currentSize by mutableStateOf(initial)
}
@Composable
fun GroupAutoResizedText(
    text: String,
    controller: TextResizeController,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    fontWeight: FontWeight? = null,
    minFont: TextUnit = 8.sp
) {
    Text(
        text = text,
        fontSize = controller.currentSize,
        color = color,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && controller.currentSize > minFont) {
                controller.currentSize = (controller.currentSize.value - 1).sp
            }
        }
    )
}


@Composable
fun AutoResizedText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    maxFontSize: TextUnit = 14.sp,
    minFontSize: TextUnit = 8.sp,
    fontWeight: FontWeight? = null
) {
    var textSize by remember { mutableStateOf(maxFontSize) }

    Text(
        text = text,
        color = color,
        fontWeight = fontWeight,
        fontSize = textSize,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = modifier,
        onTextLayout = { result ->
            if (result.didOverflowWidth && textSize > minFontSize) {
                textSize = (textSize.value - 1).sp  // ✔ правильное уменьшение
            }
        }
    )
}


