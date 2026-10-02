package com.example.netmaster.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun masteryFa(v: String) = when (v) {
    Mastery.UNKNOWN.name -> "بلد نیستم"
    Mastery.REVIEW.name -> "نیاز به مرور"
    Mastery.KNOWN.name -> "بلدم"
    Mastery.MASTERED.name -> "مسلط"
    else -> v
}

private fun modeFa(m: AiMode) = when (m) {
    AiMode.TEACHER -> "معلم"
    AiMode.SOCRATIC -> "سقراطی"
    AiMode.TROUBLESHOOTER -> "عیب‌یابی"
    AiMode.EXAMINER -> "آزمون‌گر"
    AiMode.LAB_COACH -> "مربی آزمایشگاه"
    AiMode.CONFIG_REVIEWER -> "بازبین پیکربندی"
    AiMode.AUTONOMOUS_COACH -> "مربی خودکار"
}

@Composable
fun SectionCard(
    title: String,
    subtitle: String? = null,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (icon != null) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                icon,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f)) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Start
                    )
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun BodyText(text: String, mono: Boolean = false) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium.copy(
            textDirection = if (mono) TextDirection.Ltr else TextDirection.ContentOrRtl,
            fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
            lineHeight = 24.sp
        )
    )
}

@Composable
fun TopologyGraph(
    nodes: List<TwinNode>,
    links: List<TwinLink>,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val surfaceVar = MaterialTheme.colorScheme.surfaceVariant
    val onSurface = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Hub, null, tint = primary, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        "توپولوژی شبکه",
                        fontWeight = FontWeight.Bold,
                        color = primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "نودها و لینک‌های دوقلوی دیجیتال",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            if (nodes.isEmpty()) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .background(surfaceVar, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "توپولوژی خالی است — از آزمایشگاه بارگذاری کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(surfaceVar, RoundedCornerShape(14.dp))
                ) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = min(size.width, size.height) * 0.32f
                    val n = nodes.size.coerceAtLeast(1)
                    val pos = nodes.mapIndexed { idx, node ->
                        val a = idx * 2.0 * Math.PI / n - Math.PI / 2
                        node.id to Offset(cx + r * cos(a).toFloat(), cy + r * sin(a).toFloat())
                    }.toMap()
                    links.forEach { link ->
                        val a = pos[link.from]
                        val b = pos[link.to]
                        if (a != null && b != null) {
                            drawLine(
                                color = if (link.up) outline else outline.copy(alpha = 0.4f),
                                start = a,
                                end = b,
                                strokeWidth = 3f,
                                pathEffect = if (link.up) null else PathEffect.dashPathEffect(floatArrayOf(10f, 8f))
                            )
                        }
                    }
                    nodes.forEach { node ->
                        val p = pos[node.id] ?: return@forEach
                        drawCircle(primary, 24f, p)
                        drawCircle(Color.White, 24f, p, style = Stroke(width = 2.5f))
                    }
                }
                Text(
                    nodes.joinToString("  •  ") { it.name.take(14) },
                    style = MaterialTheme.typography.labelMedium,
                    color = onSurface,
                    modifier = Modifier.padding(top = 8.dp),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    Modifier.padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("● نود فعال", color = primary, style = MaterialTheme.typography.labelSmall)
                    Text("┄ لینک قطع", color = outline, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ProtocolFlowChart(title: String, steps: List<String>) {
    val primary = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Hub, null, tint = primary, modifier = Modifier.size(22.dp))
                Column {
                    Text(
                        title,
                        fontWeight = FontWeight.Bold,
                        color = primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "مسیر پردازش پروتکل و لایه‌ها",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            steps.forEachIndexed { index, step ->
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = primary,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                "${index + 1}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = container,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            step,
                            Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            fontWeight = FontWeight.Medium,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                if (index < steps.lastIndex) {
                    Box(
                        Modifier
                            .padding(start = 10.dp)
                            .size(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "↓",
                            color = primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }
    }
}
