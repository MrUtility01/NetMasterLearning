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
                        .height(180.dp)
                        .background(surfaceVar, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "توپولوژی خالی است — از آزمایشگاه یا شبیه‌ساز بارگذاری کنید",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else {
                val positions = remember(nodes) {
                    val n = nodes.size.coerceAtLeast(1)
                    nodes.mapIndexed { idx, node ->
                        val angle = idx * 2.0 * Math.PI / n - Math.PI / 2
                        node.id to angle
                    }.toMap()
                }

                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .background(surfaceVar, RoundedCornerShape(14.dp))
                ) {
                    Canvas(Modifier.fillMaxSize()) {
                        val cx = size.width / 2f
                        val cy = size.height / 2f
                        val r = min(size.width, size.height) * 0.34f

                        val posMap = positions.mapValues { (_, angle) ->
                            Offset(
                                cx + r * cos(angle).toFloat(),
                                cy + r * sin(angle).toFloat()
                            )
                        }

                        links.forEach { link ->
                            val a = posMap[link.from]
                            val b = posMap[link.to]
                            if (a != null && b != null) {
                                drawLine(
                                    color = if (link.up) outline else outline.copy(alpha = 0.45f),
                                    start = a,
                                    end = b,
                                    strokeWidth = 3.5f,
                                    pathEffect = if (link.up) null
                                    else PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
                                )
                            }
                        }

                        nodes.forEach { node ->
                            val p = posMap[node.id] ?: return@forEach
                            val fill = primary
                            drawCircle(fill, 26f, p)
                            drawCircle(Color.White, 26f, p, style = Stroke(width = 2.5f))
                        }
                    }

                    nodes.forEach { node ->
                        val angle = positions[node.id] ?: return@forEach
                        val labelOffset = 0.48f
                        BoxWithConstraints(Modifier.fillMaxSize()) {
                            val cx = maxWidth / 2
                            val cy = maxHeight / 2
                            val r = minOf(maxWidth, maxHeight) * labelOffset
                            val x = cx + r * cos(angle).toFloat()
                            val y = cy + r * sin(angle).toFloat()
                            Text(
                                text = node.name.take(12),
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    textDirection = TextDirection.ContentOrRtl
                                ),
                                color = onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .offset(
                                        x = (x.value - 36).dp.coerceIn(0.dp, maxWidth - 72.dp),
                                        y = (y.value - 14).dp.coerceIn(0.dp, maxHeight - 28.dp)
                                    )
                                    .widthIn(max = 72.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(10.dp)
                                .background(primary, CircleShape)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("نود فعال", style = MaterialTheme.typography.labelSmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("────", color = outline.copy(alpha = 0.5f), fontSize = 11.sp)
                        Spacer(Modifier.width(4.dp))
                        Text("لینک قطع (خط‌چین)", style = MaterialTheme.typography.labelSmall)
                    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetMasterApp(vm: NetMasterViewModel) {
    var tab by remember { mutableIntStateOf(0) }
    var selectedLevel by remember { mutableStateOf<Int?>(null) }
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    var searchOpen by remember { mutableStateOf(false) }

    val tabs = listOf(
        "خانه" to Icons.Default.Home,
        "یادگیری" to Icons.Default.MenuBook,
        "آزمایشگاه" to Icons.Default.Build,
        "آزمون" to Icons.Default.Quiz,
        "یادداشت" to Icons.Default.EditNote,
        "هوش مصنوعی" to Icons.Default.AutoAwesome,
        "عملیات" to Icons.Default.Dns,
        "شبیه‌ساز" to Icons.Default.Terminal,
        "مهندس" to Icons.Default.Hub
    )

    Scaffold(
        topBar = {
            if (selectedLesson == null && selectedLevel == null && !searchOpen) {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("نت‌مستر", fontWeight = FontWeight.Bold)
                            Text(
                                "مرکز مهندسی شبکه",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { searchOpen = true }) {
                            Icon(Icons.Default.Search, contentDescription = "جستجو")
                        }
                    }
                )
            }
        },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { i, pair ->
                    NavigationBarItem(
                        selected = i == tab,
                        onClick = {
                            tab = i
                            selectedLevel = null
                            selectedLesson = null
                            searchOpen = false
                        },
                        icon = { Icon(pair.second, contentDescription = pair.first) },
                        label = {
                            Text(
                                pair.first,
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                searchOpen -> SearchScreen(vm) {
                    selectedLesson = it
                    searchOpen = false
                }
                selectedLesson != null -> LessonDetail(selectedLesson!!, vm) {
                    selectedLesson = null
                }
                selectedLevel != null -> LevelDetail(vm, selectedLevel!!) {
                    selectedLesson = it
                }
                tab == 0 -> HomeScreen(vm) { tab = 1 }
                tab == 1 -> LearnScreen(vm) { selectedLevel = it }
                tab == 2 -> LabScreen(vm)
                tab == 3 -> QuizScreen(vm) { selectedLesson = it }
                tab == 4 -> NotesScreen(vm)
                tab == 5 -> AiScreen(vm)
                tab == 6 -> OpsScreen(vm)
                tab == 7 -> SimScreen(vm)
                tab == 8 -> V11Screen(vm)
                else -> HomeScreen(vm) { tab = 0 }
            }
        }
    }
}

@Composable
fun HomeScreen(vm: NetMasterViewModel, openLearn: () -> Unit) {
    val curriculum = vm.curriculum.collectAsState().value
    val progress = vm.progress.collectAsState().value
    val twin = vm.twin.collectAsState().value

    if (curriculum == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("در حال بارگذاری محتوا…")
            }
        }
        return
    }

    val total = remember(curriculum) { curriculum.levels.sumOf { it.lessons.size } }
    val done = remember(progress) { progress.values.count { it.completed } }
    val mastered = remember(progress) {
        progress.values.count { it.mastery == Mastery.MASTERED.name }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(vertical = 14.dp)
    ) {
        item {
            SectionCard(
                title = "پیشرفت شما",
                subtitle = "خلاصه وضعیت یادگیری آفلاین",
                icon = Icons.Default.Assessment
            ) {
                LinearProgressIndicator(
                    progress = { if (total == 0) 0f else done.toFloat() / total },
                    modifier = Modifier.fillMaxWidth().height(10.dp),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
                Text("تکمیل‌شده: $done از $total درس   •   مسلط: $mastered")
                Text("تعداد سطح‌ها: ${curriculum.levels.size}")
            }
        }

        item {
            SectionCard(
                title = "شروع سریع",
                subtitle = "از مسیر یادگیری حرفه‌ای وارد شوید",
                icon = Icons.Default.PlayArrow
            ) {
                Button(
                    onClick = openLearn,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("شروع یادگیری", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            TopologyGraph(twin.nodes, twin.links)
        }

        item {
            ProtocolFlowChart(
                title = "فلوچارت عیب‌یابی استاندارد",
                steps = listOf(
                    "لایه فیزیکی",
                    "لایه ۲ / VLAN",
                    "IP و ARP",
                    "مسیریابی",
                    "DNS",
                    "فایروال",
                    "اپلیکیشن"
                )
            )
        }

        item {
            SectionCard(
                title = "درباره برنامه",
                subtitle = "نسخه یکپارچه مهندسی شبکه",
                icon = Icons.Default.Info
            ) {
                Text("سازنده: مهندس مسعود جوکار", fontWeight = FontWeight.Bold)
                Text("تماس: ۰۹۱۳۲۱۸۴۱۲۲")
                Text("نسخه ۱۱.۰ — آموزش + آزمایشگاه + هوش مصنوعی")
            }
        }

        item {
            Text(
                "سطوح پیشنهادی",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        items(curriculum.levels.take(12), key = { it.id }) { level ->
            Card(
                onClick = openLearn,
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "${level.id}. ${level.title}",
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${level.lessons.size} درس",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LearnScreen(vm: NetMasterViewModel, onLevel: (Int) -> Unit) {
    val curriculum = vm.curriculum.collectAsState().value ?: return

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard(
                title = "موتور یادگیری",
                subtitle = "مفهوم ← بسته ← پیکربندی ← آزمایشگاه ← تسلط",
                icon = Icons.Default.MenuBook
            ) {
                Text("هر سطح را باز کنید و درس‌ها را به ترتیب بخوانید.")
            }
        }
        items(curriculum.levels, key = { it.id }) { level ->
            Card(
                onClick = { onLevel(level.id) },
                shape = RoundedCornerShape(14.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text(
                        "${level.id}. ${level.title}",
                        fontWeight = FontWeight.Bold
                    )
                    if (level.summary.isNotBlank()) {
                        BodyText(level.summary)
                    }
                    Text(
                        "${level.lessons.size} درس",
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LevelDetail(vm: NetMasterViewModel, id: Int, onLesson: (Lesson) -> Unit) {
    val level = vm.curriculum.collectAsState().value?.levels?.firstOrNull { it.id == id }
        ?: return
    val progress = vm.progress.collectAsState().value

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "سطح ${level.id}: ${level.title}",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            if (level.summary.isNotBlank()) {
                BodyText(level.summary)
            }
        }
        items(level.lessons, key = { it.id }) { lesson ->
            Card(
                onClick = { onLesson(lesson) },
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text(lesson.title, fontWeight = FontWeight.Bold)
                    if (lesson.goal.isNotBlank()) {
                        Text(
                            lesson.goal,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        masteryFa(progress[lesson.id]?.mastery ?: Mastery.UNKNOWN.name),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}

@Composable
fun LessonDetail(lesson: Lesson, vm: NetMasterViewModel, back: () -> Unit) {
    var note by remember { mutableStateOf("") }
    val progress = vm.progress.collectAsState().value[lesson.id]
    val bookmarks = vm.bookmarks.collectAsState().value

    LazyColumn(
        Modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = back) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("بازگشت")
                }
                IconButton(onClick = { vm.toggleBookmark(lesson.id) }) {
                    Icon(
                        if (bookmarks.contains(lesson.id)) Icons.Default.Bookmark
                        else Icons.Default.BookmarkBorder,
                        contentDescription = "نشانک"
                    )
                }
            }
            Text(
                lesson.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        if (lesson.diagram.isNotBlank() || lesson.packetWalkthrough.isNotBlank()) {
            item {
                ProtocolFlowChart(
                    title = "فلوچارت مسیر پروتکل این درس",
                    steps = listOf("ورودی", "پردازش", "تصمیم", "خروجی", "تأیید")
                )
            }
        }

        if (lesson.goal.isNotBlank()) {
            item {
                SectionCard("هدف درس", icon = Icons.Default.Flag) {
                    BodyText(lesson.goal)
                }
            }
        }
        if (lesson.simple.isNotBlank()) {
            item {
                SectionCard("مفهوم ساده", "برای شروع یادگیری", Icons.Default.LightMode) {
                    BodyText(lesson.simple)
                }
            }
        }
        if (lesson.technical.isNotBlank()) {
            item {
                SectionCard("مفهوم فنی", icon = Icons.Default.Memory) {
                    BodyText(lesson.technical)
                }
            }
        }
        if (lesson.deepTechnical.isNotBlank()) {
            item {
                SectionCard("تحلیل عمیق", icon = Icons.Default.Assessment) {
                    BodyText(lesson.deepTechnical)
                }
            }
        }
        if (lesson.packetWalkthrough.isNotBlank()) {
            item {
                SectionCard("گام‌به‌گام بسته و وضعیت", icon = Icons.Default.MoreVert) {
                    BodyText(lesson.packetWalkthrough)
                }
            }
        }
        if (lesson.diagram.isNotBlank()) {
            item {
                SectionCard("دیاگرام و توپولوژی متنی", icon = Icons.Default.Hub) {
                    BodyText(lesson.diagram, mono = true)
                }
            }
        }
        if (lesson.configurationPlaybook.isNotBlank()) {
            item {
                SectionCard("پلی‌بوک پیکربندی", icon = Icons.Default.Settings) {
                    BodyText(lesson.configurationPlaybook, mono = true)
                }
            }
        }
        if (lesson.commands.isNotBlank() || lesson.platformCommands.isNotBlank()) {
            item {
                SectionCard("دستورات", icon = Icons.Default.Terminal) {
                    BodyText(
                        lesson.platformCommands.ifBlank { lesson.commands },
                        mono = true
                    )
                }
            }
        }
        if (lesson.lab.isNotBlank()) {
            item {
                SectionCard("آزمایشگاه", icon = Icons.Default.Build) {
                    BodyText(lesson.lab)
                }
            }
        }
        if (lesson.troubleshooting.isNotBlank()) {
            item {
                SectionCard("عیب‌یابی", icon = Icons.Default.Warning) {
                    BodyText(lesson.troubleshooting)
                }
            }
        }

        item {
            SectionCard("سطح تسلط شما", icon = Icons.Default.Star) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Mastery.entries.forEach { m ->
                        FilterChip(
                            selected = progress?.mastery == m.name,
                            onClick = { vm.setMastery(lesson.id, m) },
                            label = {
                                Text(masteryFa(m.name), fontSize = 12.sp)
                            }
                        )
                    }
                }
            }
        }

        item {
            SectionCard("یادداشت شخصی", icon = Icons.Default.EditNote) {
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    label = { Text("یادداشت خود را بنویسید") },
                    shape = RoundedCornerShape(12.dp)
                )
                Button(
                    onClick = {
                        if (note.isNotBlank()) {
                            vm.addNote(lesson.id, lesson.title, note)
                            note = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(46.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("ذخیره یادداشت")
                }
            }
        }

        if (lesson.quiz.isNotEmpty()) {
            item {
                Text(
                    "سؤالات آزمون",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            items(lesson.quiz) { q ->
                Card(shape = RoundedCornerShape(12.dp)) {
                    Column(Modifier.padding(12.dp)) {
                        BodyText(q.q)
                        Text(
                            "پاسخ: ${q.a}",
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }
    }
}
