package ru.byteforge.xrayvpnclient.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle

@Composable
fun AnimatedTimeDisplay(
    hours: String,
    minutes: String,
    seconds: String,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle(
        fontSize = 48.sp,
        fontWeight = FontWeight.Bold,
        color = Color.White
    )
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        AnimatedDigits(
            value = hours,
            textStyle = textStyle
        )

        Text(
            text = ":",
            style = textStyle
        )

        AnimatedDigits(
            value = minutes,
            textStyle = textStyle
        )

        Text(
            text = ":",
            style = textStyle
        )

        AnimatedDigits(
            value = seconds,
            textStyle = textStyle
        )
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AnimatedDigits(
    value: String,
    textStyle: TextStyle
) {
    Row {
        value.forEachIndexed { index, digit ->
            AnimatedContent(
                targetState = digit,
                transitionSpec = {
                    slideInVertically(initialOffsetY = { it }) + fadeIn() with
                            slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                }
            ) { char ->
                Text(
                    text = char.toString(),
                    style = textStyle
                )
            }
        }
    }
}