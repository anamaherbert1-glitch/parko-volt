package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.SessionStatus
import com.example.data.model.VehicleType
import com.example.ui.i18n.AppLanguage
import com.example.ui.i18n.LocalAppLanguage
import com.example.ui.i18n.localizedLabel
import com.example.ui.i18n.tr
import com.example.ui.theme.*
import com.example.util.QrCodeGenerator
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

fun formatFcfa(amount: Int): String {
    val nf = NumberFormat.getIntegerInstance(Locale.FRANCE)
    return "${nf.format(amount)} FCFA"
}

fun formatTimeShort(millis: Long): String {
    val sdf = SimpleDateFormat("HH:mm", Locale.FRANCE)
    return sdf.format(Date(millis))
}

fun formatDateTime(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy • HH:mm", Locale.FRANCE)
    return sdf.format(Date(millis))
}

fun formatDateOnly(millis: Long): String {
    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.FRANCE)
    return sdf.format(Date(millis))
}

/**
 * Rich architectural atmospheric backdrop:
 * layered ambient gradients, cobalt & emerald aurora blooms, sweeping mobility curves,
 * concentric orbital arcs, and subtle corner dot geometry for Light & Dark modes.
 */
@Composable
fun Modifier.parkoAtmosphericBackground(): Modifier {
    val bg = MaterialTheme.colorScheme.background
    val isDark = bg.luminance() < 0.3f

    val gradientStops = if (isDark) {
        listOf(Color(0xFF070D1C), Color(0xFF0C1832), Color(0xFF081324))
    } else {
        listOf(Color(0xFFF6F9FF), Color(0xFFECF3FF), Color(0xFFF2F8F9))
    }
    val topBlueAlpha = if (isDark) 0.34f else 0.16f
    val midSkyAlpha = if (isDark) 0.18f else 0.10f
    val bottomEmeraldAlpha = if (isDark) 0.22f else 0.11f
    val ribbonAlpha = if (isDark) 0.16f else 0.11f
    val arcAlpha = if (isDark) 0.14f else 0.09f
    val dotAlpha = if (isDark) 0.12f else 0.08f

    return this
        .background(Brush.verticalGradient(gradientStops))
        .drawBehind {
            val w = size.width
            val h = size.height

            // Top-right Cobalt Blue aurora bloom
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ParkoBlue.copy(alpha = topBlueAlpha),
                        ParkoBlueMid.copy(alpha = topBlueAlpha * 0.45f),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.92f, h * 0.07f),
                    radius = size.maxDimension * 0.68f
                )
            )

            // Mid-left Sky Azure shimmer
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF0EA5E9).copy(alpha = midSkyAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.08f, h * 0.44f),
                    radius = size.maxDimension * 0.48f
                )
            )

            // Bottom-left Emerald mobility bloom
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        ParkoEmerald.copy(alpha = bottomEmeraldAlpha),
                        Color.Transparent
                    ),
                    center = Offset(w * 0.12f, h * 0.90f),
                    radius = size.maxDimension * 0.56f
                )
            )

            // Sweeping architectural ribbon wave 1
            val wave1 = Path().apply {
                moveTo(-w * 0.1f, h * 0.22f)
                cubicTo(w * 0.28f, h * 0.12f, w * 0.65f, h * 0.30f, w * 1.1f, h * 0.18f)
            }
            drawPath(
                path = wave1,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        ParkoBlueMid.copy(alpha = ribbonAlpha),
                        ParkoEmerald.copy(alpha = ribbonAlpha * 0.8f),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
            )

            // Sweeping architectural ribbon wave 2
            val wave2 = Path().apply {
                moveTo(-w * 0.08f, h * 0.74f)
                cubicTo(w * 0.32f, h * 0.64f, w * 0.72f, h * 0.84f, w * 1.08f, h * 0.70f)
            }
            drawPath(
                path = wave2,
                brush = Brush.horizontalGradient(
                    listOf(
                        Color.Transparent,
                        ParkoEmerald.copy(alpha = ribbonAlpha * 0.85f),
                        ParkoBlue.copy(alpha = ribbonAlpha),
                        Color.Transparent
                    )
                ),
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
            )

            // Concentric orbital arcs (top-right & bottom-left)
            drawCircle(
                color = ParkoBlueMid.copy(alpha = arcAlpha),
                radius = size.minDimension * 0.42f,
                center = Offset(w * 0.95f, h * 0.10f),
                style = Stroke(width = 2.2.dp.toPx())
            )
            drawCircle(
                color = ParkoBlue.copy(alpha = arcAlpha * 0.75f),
                radius = size.minDimension * 0.58f,
                center = Offset(w * 0.95f, h * 0.10f),
                style = Stroke(width = 1.5.dp.toPx())
            )
            drawCircle(
                color = ParkoEmerald.copy(alpha = arcAlpha * 0.85f),
                radius = size.minDimension * 0.34f,
                center = Offset(w * 0.04f, h * 0.88f),
                style = Stroke(width = 2.dp.toPx())
            )

            // Subtle architectural dot matrix in upper-left & lower-right corners
            val step = 18.dp.toPx()
            val dotRadius = 1.6.dp.toPx()
            for (row in 0..4) {
                for (col in 0..4) {
                    val alphaFactor = 1f - ((row + col) / 10f)
                    if (alphaFactor > 0f) {
                        drawCircle(
                            color = ParkoBlueMid.copy(alpha = dotAlpha * alphaFactor),
                            radius = dotRadius,
                            center = Offset(18.dp.toPx() + col * step, 24.dp.toPx() + row * step)
                        )
                    }
                }
            }
        }
}

@Composable
fun Modifier.parkoScannerAtmosphericBackground(): Modifier {
    return this
        .background(
            Brush.verticalGradient(
                listOf(Color(0xFF060C1B), Color(0xFF0B1733), Color(0xFF071122))
            )
        )
        .drawBehind {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ParkoBlue.copy(alpha = 0.28f), Color.Transparent),
                    center = Offset(size.width * 0.5f, size.height * 0.32f),
                    radius = size.maxDimension * 0.55f
                )
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(ParkoEmerald.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.maxDimension * 0.45f
                )
            )
            drawCircle(
                color = ParkoBlueMid.copy(alpha = 0.14f),
                radius = size.minDimension * 0.46f,
                center = Offset(size.width * 0.5f, size.height * 0.34f),
                style = Stroke(width = 1.5.dp.toPx())
            )
        }
}

@Composable
fun Modifier.parkoCardOutline(cornerRadius: Dp = 18.dp): Modifier {
    return this.border(
        width = 1.2.dp,
        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.58f),
        shape = RoundedCornerShape(cornerRadius)
    )
}

@Composable
fun ParkoLogoBadge(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    inverted: Boolean = false
) {
    val bg = if (inverted) Color.White else ParkoBlue
    val fg = if (inverted) ParkoBlue else Color.White
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(bg)
            .border(1.5.dp, fg.copy(alpha = 0.22f), RoundedCornerShape(size * 0.28f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.TwoWheeler,
            contentDescription = "Logo PARKO",
            tint = fg,
            modifier = Modifier.size(size * 0.62f)
        )
    }
}

@Composable
fun ParkoTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            Surface(
                onClick = onBack,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.94f),
                modifier = Modifier
                    .size(46.dp)
                    .minimumInteractiveComponentSize()
                    .parkoCardOutline(23.dp)
                    .testTag("back_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = tr("Retour", "Back"),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
        }
    }
}

@Composable
fun SessionStatusBadge(statusRaw: String) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val status = SessionStatus.entries.firstOrNull { it.name == statusRaw } ?: SessionStatus.ACTIVE
    when (status) {
        SessionStatus.ACTIVE -> StatusPill(
            label = status.localizedLabel(en),
            containerColor = ParkoEmeraldLight,
            contentColor = ParkoEmeraldDark,
            icon = Icons.Filled.CheckCircle
        )
        SessionStatus.COMPLETED -> StatusPill(
            label = status.localizedLabel(en),
            containerColor = ParkoBlueLight,
            contentColor = ParkoBlueDark,
            icon = Icons.Filled.DoneAll
        )
        SessionStatus.EXPIRED -> StatusPill(
            label = status.localizedLabel(en),
            containerColor = ParkoRedLight,
            contentColor = ParkoRedDark,
            icon = Icons.Filled.Error
        )
        SessionStatus.NEEDS_VERIFICATION -> StatusPill(
            label = status.localizedLabel(en),
            containerColor = ParkoAmberLight,
            contentColor = ParkoAmberDark,
            icon = Icons.Filled.Warning
        )
    }
}

@Composable
fun PaymentStatusBadge(statusRaw: String) {
    val en = LocalAppLanguage.current == AppLanguage.EN
    val status = PaymentStatus.entries.firstOrNull { it.name == statusRaw } ?: PaymentStatus.PENDING
    when (status) {
        PaymentStatus.CONFIRMED -> StatusPill(status.localizedLabel(en), ParkoEmeraldLight, ParkoEmeraldDark, Icons.Filled.Verified)
        PaymentStatus.PENDING -> StatusPill(status.localizedLabel(en), ParkoAmberLight, ParkoAmberDark, Icons.Filled.Schedule)
        PaymentStatus.FAILED -> StatusPill(status.localizedLabel(en), ParkoRedLight, ParkoRedDark, Icons.Filled.Cancel)
        PaymentStatus.CANCELLED -> StatusPill(status.localizedLabel(en), ParkoSurfaceVariant, ParkoSlate)
        PaymentStatus.REFUNDED -> StatusPill(status.localizedLabel(en), ParkoBlueLight, ParkoBlueDark)
    }
}

@Composable
fun VehicleThumbnail(
    photoUri: String?,
    drawableKey: String,
    vehicleType: String,
    modifier: Modifier = Modifier
) {
    val fallbackRes = if (drawableKey == "moto" || vehicleType == VehicleType.MOTO.name || vehicleType == VehicleType.TRICYCLE.name) {
        R.drawable.img_moto_sport
    } else {
        R.drawable.img_car_sedan
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(ParkoSurfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (!photoUri.isNullOrBlank()) {
            AsyncImage(
                model = photoUri,
                contentDescription = "Photo véhicule",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Image(
                painter = painterResource(id = fallbackRes),
                contentDescription = "Illustration véhicule",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun QrCodeCanvas(
    payload: String,
    modifier: Modifier = Modifier,
    moduleColor: Color = ParkoCharcoal
) {
    val matrix = remember(payload) { QrCodeGenerator.encode(payload) }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.5.dp, ParkoBlue.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val count = matrix.size
            val cellSize = size.minDimension / count.toFloat()
            val offsetX = (size.width - cellSize * count) / 2f
            val offsetY = (size.height - cellSize * count) / 2f
            for (y in 0 until count) {
                for (x in 0 until count) {
                    if (matrix[y][x]) {
                        drawRect(
                            color = moduleColor,
                            topLeft = Offset(offsetX + x * cellSize, offsetY + y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RevenueBarChart(
    values: List<Pair<String, Int>>,
    modifier: Modifier = Modifier
) {
    val maxVal = (values.maxOfOrNull { it.second } ?: 1).coerceAtLeast(1)
    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            if (values.isEmpty()) return@Canvas
            val barSlot = size.width / values.size
            val barWidth = (barSlot * 0.54f).coerceAtMost(42f)
            val usableHeight = size.height - 12f
            values.forEachIndexed { idx, (_, value) ->
                val ratio = (value.toFloat() / maxVal.toFloat()).coerceIn(0.08f, 1f)
                val barHeight = usableHeight * ratio
                val left = idx * barSlot + (barSlot - barWidth) / 2f
                val top = size.height - barHeight
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(ParkoBlue, ParkoBlueMid)
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(14f, 14f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            values.forEach { (label, _) ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    subtitle: String = "",
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .parkoCardOutline(20.dp),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(ParkoBlueLight)
                    .border(1.5.dp, ParkoBlue.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = ParkoBlue, modifier = Modifier.size(34.dp))
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onAction,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Text(actionLabel, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun PaymentMethodLabel(methodRaw: String): String {
    val en = LocalAppLanguage.current == AppLanguage.EN
    return PaymentMethod.entries.firstOrNull { it.name == methodRaw }?.localizedLabel(en) ?: methodRaw
}

@Composable
fun ParkoMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    iconTint: Color = ParkoBlue,
    trailingBadge: String? = null,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotEmpty()) Modifier.testTag(testTag) else Modifier)
            .clickable(onClick = onClick)
            .minimumInteractiveComponentSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(13.dp))
                .background(iconTint.copy(alpha = 0.18f))
                .border(1.dp, iconTint.copy(alpha = 0.35f), RoundedCornerShape(13.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (!trailingBadge.isNullOrBlank()) {
            StatusPill(
                label = trailingBadge,
                containerColor = ParkoBlueLight,
                contentColor = ParkoBlueDark
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(24.dp)
        )
    }
}
