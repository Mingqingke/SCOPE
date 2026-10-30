package com.scope.app.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.scope.app.core.AppRepo
import com.scope.app.core.Prefs
import com.scope.app.ui.theme.LocalScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.CardDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.extended.ChevronBackward
import top.yukonga.miuix.kmp.icon.extended.ChevronForward
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.PressFeedbackType

/** 卡片按压反馈统一走 Miuix 官方实现：0 Sink / 1 Tilt / 2 关闭。 */
@Composable
fun pressFeedback(): PressFeedbackType {
    val mode by Prefs.cardFeedback.collectAsState()
    val reduceMotion by Prefs.reduceMotion.collectAsState()
    return when {
        mode == 2 -> PressFeedbackType.None
        mode == 1 && !reduceMotion -> PressFeedbackType.Tilt
        else -> PressFeedbackType.Sink
    }
}

@Composable
fun ScreenTitle(
    title: String,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val scope = LocalScope.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (onBack != null) 8.dp else 20.dp, end = 12.dp, top = 6.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(MiuixIcons.ChevronBackward, "返回", Modifier.size(22.dp), tint = MiuixTheme.colorScheme.onBackground)
            }
            Spacer(Modifier.width(4.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = if (onBack != null) 20.sp else 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = scope.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        actions()
    }
}

@Composable
fun SectionLabel(text: String) {
    Text(
        text = text,
        modifier = Modifier.padding(start = 22.dp, top = 18.dp, bottom = 8.dp),
        fontSize = 13.sp,
        fontWeight = FontWeight.Medium,
        color = LocalScope.current.textSecondary,
    )
}

@Composable
fun EntryCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    trailing: String? = null,
    interactive: Boolean = true,
) {
    val scope = LocalScope.current
    val feedback = if (interactive) pressFeedback() else PressFeedbackType.None
    Card(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
        colors = CardDefaults.defaultColors(color = scope.card),
        pressFeedbackType = feedback,
        showIndication = interactive,
        onClick = if (enabled) onClick else null,
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(text = title, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(3.dp))
                Text(text = subtitle, fontSize = 13.sp, color = scope.textSecondary)
            }
            if (trailing != null) {
                Text(text = trailing, fontSize = 13.sp, color = scope.textSecondary)
                Spacer(Modifier.width(6.dp))
            }
            Icon(
                imageVector = MiuixIcons.ChevronForward,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = scope.textSecondary.copy(alpha = 0.55f),
            )
        }
    }
}

/** 通用信息卡片外壳。 */
@Composable
fun InfoCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    tint: Color? = null,
    padding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 16.dp),
    content: @Composable ColumnScope.() -> Unit,
) {
    val scope = LocalScope.current
    val fb = pressFeedback()
    if (onClick != null) {
        Card(
            modifier = modifier,
            cornerRadius = 22.dp,
            colors = CardDefaults.defaultColors(color = tint ?: scope.card),
            pressFeedbackType = fb,
            showIndication = true,
            onClick = onClick,
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    } else {
        Card(
            modifier = modifier,
            cornerRadius = 22.dp,
            colors = CardDefaults.defaultColors(color = tint ?: scope.card),
        ) {
            Column(Modifier.padding(padding), content = content)
        }
    }
}

@Composable
fun KeyValueRow(label: String, value: String, mono: Boolean = false) {
    val scope = LocalScope.current
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(text = label, fontSize = 14.sp, color = scope.textSecondary, modifier = Modifier.width(96.dp))
        Text(
            text = value.ifBlank { "—" },
            fontSize = 14.sp,
            fontWeight = if (mono) FontWeight.Normal else FontWeight.Medium,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
fun StatusDot(color: Color, size: Dp = 8.dp) {
    Box(
        Modifier
            .size(size)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
fun AppIcon(pkg: String, size: Dp = 42.dp) {
    val context = LocalContext.current
    val bitmap by produceState<ImageBitmap?>(initialValue = null, pkg) {
        value = withContext(Dispatchers.IO) {
            val drawable: Drawable? = AppRepo.icon(context, pkg)
            runCatching { drawable?.toBitmap(120, 120)?.asImageBitmap() }.getOrNull()
        }
    }
    val shape = RoundedCornerShape(size * 0.28f)
    Box(
        Modifier
            .size(size)
            .clip(shape)
            .background(LocalScope.current.hairline),
        contentAlignment = Alignment.Center,
    ) {
        val image = bitmap
        if (image != null) {
            Image(bitmap = image, contentDescription = null, modifier = Modifier.size(size).clip(shape))
        }
    }
}

enum class AuthVisual { OK, WARN, OFF }

/**
 * 首页授权卡片右下角的圆环对勾：Canvas 矢量绘制，
 * 粗圆环 + 粗勾号，允许被卡片右下边缘自然裁切。
 */
@Composable
fun CheckBadge(kind: AuthVisual, modifier: Modifier = Modifier) {
    val scope = LocalScope.current
    val color = when (kind) {
        AuthVisual.OK -> scope.green
        AuthVisual.WARN -> scope.amber
        AuthVisual.OFF -> scope.textSecondary.copy(alpha = 0.40f)
    }
    Canvas(modifier) {
        val d = size.minDimension
        val stroke = d * 0.075f
        val radius = d / 2f - stroke / 2f
        drawCircle(
            color = color,
            radius = radius,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke, cap = StrokeCap.Round),
        )
        when (kind) {
            AuthVisual.OK -> {
                val path = Path().apply {
                    moveTo(d * 0.30f, d * 0.52f)
                    lineTo(d * 0.44f, d * 0.66f)
                    lineTo(d * 0.72f, d * 0.36f)
                }
                drawPath(
                    path = path,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke * 1.1f,
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
            AuthVisual.WARN -> {
                drawLine(
                    color = color,
                    start = Offset(d * 0.5f, d * 0.31f),
                    end = Offset(d * 0.5f, d * 0.56f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawCircle(color = color, radius = stroke * 0.72f, center = Offset(d * 0.5f, d * 0.71f))
            }
            AuthVisual.OFF -> Unit
        }
    }
}

@Composable
fun InlineIcon(icon: ImageVector, tint: Color? = null, size: Dp = 18.dp) {
    Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(size),
        tint = tint ?: MiuixTheme.colorScheme.onBackgroundVariant,
    )
}

@Composable
fun EmptyHint(text: String) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(text = text, fontSize = 14.sp, color = LocalScope.current.textSecondary, textAlign = TextAlign.Center)
    }
}
