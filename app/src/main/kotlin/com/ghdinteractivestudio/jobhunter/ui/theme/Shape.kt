package com.ghdinteractivestudio.jobhunter.ui.theme

import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Capsule(): Shape = RoundedCornerShape(percent = 50)

fun RoundedRectangle(cornerRadius: Dp = 16.dp): Shape = RoundedCornerShape(cornerRadius)

fun RoundedRectangle(
    topStart: Dp = 16.dp,
    topEnd: Dp = 16.dp,
    bottomEnd: Dp = 16.dp,
    bottomStart: Dp = 16.dp
): Shape = RoundedCornerShape(
    topStart = CornerSize(topStart),
    topEnd = CornerSize(topEnd),
    bottomEnd = CornerSize(bottomEnd),
    bottomStart = CornerSize(bottomStart)
)
