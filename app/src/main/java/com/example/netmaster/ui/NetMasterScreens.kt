package com.example.netmaster.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*

@Composable
fun PacketPathPanel(path: PacketPathResult) {
    SectionCard(
        path.title,
        path.summary + if (path.brokenAt != null) " — قطع در: ${path.brokenAt}" else ""
    ) {
        Text(
            "پروتکل‌ها: " + path.protocols.joinToString(" · "),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary
        )
        path.hops.forEach { h ->
            Card(
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (h.ok)
                        MaterialTheme.colorScheme.surfaceVariant
                    else MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "${h.step}. ${h.deviceName} (${h.deviceType}) — ${h.action}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    Text("${h.layer} · ${h.detail}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun DeviceListCard(nodes: List<TwinNode>, links: List<TwinLink>) {
    SectionCard("دیوایس‌های توپولوژی", "${nodes.size} نود · ${links.size} لینک") {
        nodes.forEach { n ->
            Text(
                "• ${n.name}  [${n.type}]  ${n.ip ?: "—"}  ${n.vlan?.let { "VLAN $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall
            )
        }
        Spacer(Modifier.height(6.dp))
        Text("لینک‌ها:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
        links.forEach { l ->
            Text(
                "  ${l.from} → ${l.to}  (${l.protocol}) ${if (l.up) "UP" else "DOWN"}",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun LabScreen(vm: NetMasterViewModel) {
    val twin = vm.twin.collectAsState().value
    val faults = remember { vm.twinEngine.faults() }
    val presets = remember { PacketPathEngine.presets() }
    val scenarios = remember { PacketPathEngine.scenarios() }
    var presetId by remember { mutableStateOf("campus") }
    var scenarioId by remember { mutableStateOf("dns") }
    val path = remember(twin, scenarioId) { PacketPathEngine.analyze(scenarioId, twin) }

    LazyColumn(
        Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionCard("آزمایشگاه دوقلوی دیجیتال", "توپولوژی چنددیوایس + تحلیل بسته hop-by-hop") {
                Text("توپولوژی را انتخاب کنید، سناریوی بسته را ببینید، خطا تزریق کنید.")
            }
        }
        item {
            Text("نوع توپولوژی", fontWeight = FontWeight.Bold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                presets.forEach { p ->
                    FilterChip(
                        selected = presetId == p.id,
                        onClick = {
                            presetId = p.id
                            vm.loadTopologyPreset(p.id)
                        },
                        label = { Text(p.title, fontSize = 11.sp) }
                    )
                }
            }
        }
        item { TopologyGraph(twin.nodes, twin.links) }
        item { DeviceListCard(twin.nodes, twin.links) }
        item {
            Text("تحلیل مسیر بسته", fontWeight = FontWeight.Bold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEach { (id, title) ->
                    FilterChip(
                        selected = scenarioId == id,
                        onClick = { scenarioId = id },
                        label = { Text(title, fontSize = 11.sp) }
                    )
                }
            }
        }
        item { PacketPathPanel(path) }
        item {
            ProtocolFlowChart(
                title = "فلوچارت آزمایش",
                steps = listOf("توپولوژی", "سناریو بسته", "تزریق خطا", "مشاهده hop", "رفع")
            )
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.resetTwin(); presetId = "campus" }, Modifier.weight(1f)) {
                    Text("بازنشانی")
                }
                OutlinedButton({ vm.saveSimulatorSnapshot("دستی") }, Modifier.weight(1f)) {
                    Text("عکس‌فوری")
                }
            }
        }
        item {
            SectionCard("وضعیت سرویس‌ها") {
                twin.simulator?.let { s ->
                    Text("DNS: ${if (s.dnsHealthy) "سالم" else "خراب"}  |  DHCP: ${if (s.dhcpHealthy) "سالم" else "خراب"}")
                    Text("فایروال: ${if (s.firewallHealthy) "سالم" else "خراب"}  |  NAT: ${if (s.natHealthy) "سالم" else "خراب"}")
                    Text("OSPF: ${if (s.ospfHealthy) "بالا" else "ضعیف"}  |  BGP: ${if (s.bgpHealthy) "بالا" else "قطع"}")
                    Text("QoS: ${if (s.qosHealthy) "خوب" else "congested"}  |  STP: ${if (s.stpHealthy) "پایدار" else "ناپایدار"}")
                } ?: Text("شبیه‌ساز آماده نیست")
            }
        }
        item { Text("تزریق خطا (مسیر بسته عوض می‌شود)", fontWeight = FontWeight.Bold) }
        items(faults, key = { it.id }) { f ->
            Card(onClick = { vm.injectFault(f) }, shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(f.title, fontWeight = FontWeight.Bold)
                    BodyText(f.description)
                    Text("شدت: ${f.severity} · هدف: ${f.target}", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun QuizScreen(vm: NetMasterViewModel, open: (Lesson) -> Unit) {
    val c = vm.curriculum.collectAsState().value ?: return
    val ls = remember(c) { c.levels.flatMap { it.lessons }.filter { it.quiz.isNotEmpty() } }
    var i by remember { mutableIntStateOf(0) }
    if (ls.isEmpty()) {
        Text("سؤالی موجود نیست", Modifier.padding(16.dp))
        return
    }
    val l = ls[i % ls.size]
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("موتور آزمون", "تمرین با سؤالات درس‌ها") {
                Text(l.title, fontWeight = FontWeight.Bold)
            }
        }
        items(l.quiz) { q ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    BodyText(q.q)
                    Text("پاسخ کلیدی: ${q.a}", color = MaterialTheme.colorScheme.secondary)
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ i++ }, Modifier.weight(1f)) { Text("سؤال بعدی") }
                OutlinedButton({ open(l) }, Modifier.weight(1f)) { Text("رفتن به درس") }
            }
        }
    }
}

@Composable
fun NotesScreen(vm: NetMasterViewModel) {
    val n = vm.notes.collectAsState().value
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SectionCard("پایگاه دانش شخصی", "یادداشت‌ها و یافته‌های شما") {
                Text(if (n.isEmpty()) "هنوز یادداشتی ثبت نشده." else "${n.size} یادداشت")
            }
        }
        items(n, key = { it.id }) { x ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(x.title, fontWeight = FontWeight.Bold)
                    BodyText(x.body)
                }
            }
        }
    }
}

@Composable
fun SearchScreen(vm: NetMasterViewModel, onLesson: (Lesson) -> Unit) {
    var q by remember { mutableStateOf("") }
    val hits = vm.searchResults.collectAsState().value
    LaunchedEffect(q) { if (q.length >= 2) vm.searchAll(q) }
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("جستجوی سراسری", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        OutlinedTextField(q, { q = it }, Modifier.fillMaxWidth(), label = { Text("پروتکل، دستور، IP، رخداد…") }, singleLine = true)
        Spacer(Modifier.height(8.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(hits, key = { it.id + it.kind }) { h ->
                Card(shape = RoundedCornerShape(10.dp)) {
                    ListItem(
                        headlineContent = { Text(h.title) },
                        supportingContent = { Text("${h.kind} • ${h.snippet}", maxLines = 2) },
                        trailingContent = {
                            if (h.kind == "LESSON") {
                                TextButton({
                                    vm.curriculum.value?.levels?.flatMap { it.lessons }?.firstOrNull { it.id == h.id }?.let(onLesson)
                                }) { Text("باز کردن") }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun AiScreen(vm: NetMasterViewModel) {
    var prompt by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf(AiMode.TROUBLESHOOTER) }
    val r = vm.aiResponse.collectAsState().value
    val backend = vm.aiBackend.collectAsState().value
    fun modeFa(m: AiMode) = when (m) {
        AiMode.TEACHER -> "معلم"
        AiMode.SOCRATIC -> "سقراطی"
        AiMode.TROUBLESHOOTER -> "عیب‌یابی"
        AiMode.EXAMINER -> "آزمون‌گر"
        AiMode.LAB_COACH -> "مربی Lab"
        AiMode.CONFIG_REVIEWER -> "بازبین Config"
        AiMode.AUTONOMOUS_COACH -> "مربی خودکار"
    }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("مرکز هوش مصنوعی", "پشتیبانی: $backend — تحلیل با RAG") {
                Text("حالت مناسب را انتخاب کنید و سؤال یا علائم بنویسید.")
            }
        }
        item {
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                AiMode.entries.forEach {
                    FilterChip(selected = mode == it, onClick = { mode = it }, label = { Text(modeFa(it), fontSize = 11.sp) })
                }
            }
        }
        item {
            OutlinedTextField(prompt, { prompt = it }, Modifier.fillMaxWidth(), minLines = 3, label = { Text("سؤال یا علائم مشکل") })
            Button({ vm.askAi(mode, prompt) }, enabled = prompt.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                Text("تحلیل با هوش مصنوعی")
            }
        }
        r?.let {
            item {
                SectionCard(it.title) {
                    BodyText(it.answer)
                    it.hypotheses.forEach { h -> Text("فرضیه: ${h.title} (${h.confidence}٪)") }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton({ vm.rebuildVectorIndex() }, Modifier.weight(1f)) { Text("بازسازی RAG") }
                OutlinedButton({ vm.buildKnowledgeGraph() }, Modifier.weight(1f)) { Text("ساخت گراف") }
                OutlinedButton({ vm.exportBackup() }, Modifier.weight(1f)) { Text("پشتیبان") }
            }
        }
    }
}

@Composable
fun OpsScreen(vm: NetMasterViewModel) {
    val incidents by vm.incidents.collectAsState()
    val due by vm.dueReviews.collectAsState()
    var title by remember { mutableStateOf("") }
    var symptom by remember { mutableStateOf("") }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("مرکز عملیات", "رخداد، مرور هوشمند، شواهد") {
                Text("مرورهای سررسید: ${due.size}")
            }
        }
        item {
            SectionCard("ثبت رخداد جدید") {
                OutlinedTextField(title, { title = it }, Modifier.fillMaxWidth(), label = { Text("عنوان رخداد") })
                OutlinedTextField(symptom, { symptom = it }, Modifier.fillMaxWidth(), label = { Text("علائم") })
                Button({
                    if (title.isNotBlank()) {
                        vm.createIncident(title, symptom)
                        title = ""
                        symptom = ""
                    }
                }, Modifier.fillMaxWidth()) { Text("ثبت رخداد") }
            }
        }
        items(incidents.take(30), key = { it.id }) { i ->
            Card(shape = RoundedCornerShape(12.dp)) {
                Column(Modifier.padding(12.dp)) {
                    Text(i.title, fontWeight = FontWeight.Bold)
                    Text("وضعیت: ${i.status}  |  شدت: ${i.severity}")
                }
            }
        }
    }
}

@Composable
fun SimScreen(vm: NetMasterViewModel) {
    val twin = vm.twin.collectAsState().value
    val output = vm.simOutput.collectAsState().value
    var cmd by remember { mutableStateOf("") }
    var scenarioId by remember { mutableStateOf("http") }
    val path = remember(twin, scenarioId) { PacketPathEngine.analyze(scenarioId, twin) }
    val scenarios = remember { PacketPathEngine.scenarios() }

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            SectionCard("شبیه‌ساز شبکه", "دستور + توپولوژی + مسیر بسته روی دیوایس‌ها") {
                Text("دستور: show topology | ping | nslookup | tcpdump | disable dns")
            }
        }
        item { TopologyGraph(twin.nodes, twin.links) }
        item { DeviceListCard(twin.nodes, twin.links) }
        item {
            Text("سناریوی تحلیل بسته", fontWeight = FontWeight.Bold)
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                scenarios.forEach { (id, title) ->
                    FilterChip(
                        selected = scenarioId == id,
                        onClick = { scenarioId = id },
                        label = { Text(title, fontSize = 11.sp) }
                    )
                }
            }
        }
        item { PacketPathPanel(path) }
        item {
            OutlinedTextField(cmd, { cmd = it }, Modifier.fillMaxWidth(), label = { Text("دستور (مثلاً show topology)") })
            Button({ vm.runSimulator(cmd) }, Modifier.fillMaxWidth()) { Text("اجرا") }
        }
        item {
            SectionCard("خروجی") { BodyText(output.ifBlank { "—" }, mono = true) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button({ vm.resetTwin() }, Modifier.weight(1f)) { Text("بازنشانی") }
                OutlinedButton({ vm.saveSimulatorSnapshot("شبیه‌ساز") }, Modifier.weight(1f)) {
                    Text("عکس‌فوری")
                }
            }
        }
    }
}

@Composable
fun V11Screen(vm: NetMasterViewModel) {
    val inv = vm.v11Investigation.collectAsState().value
    val passport = vm.v11Passport.collectAsState().value
    val faults = vm.v11Faults.collectAsState().value
    var symptom by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { vm.computeV11Passport() }
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            SectionCard("سیستم مهندس شبکه V11", "زنجیره شواهد + پاسپورت مهندسی") {
                Text("علائم را وارد کنید تا فرضیه‌ها ساخته شوند.")
            }
        }
        item {
            ProtocolFlowChart(
                title = "زنجیره شواهد",
                steps = listOf("علائم", "فرضیه", "شواهد", "تفاوت اول", "رفع")
            )
        }
        item {
            OutlinedTextField(symptom, { symptom = it }, Modifier.fillMaxWidth(), label = { Text("علائم مشکل") })
            Button({ vm.investigateV11(symptom, "") }, Modifier.fillMaxWidth()) { Text("شروع بررسی") }
        }
        inv?.let {
            item {
                SectionCard("اولین نقطه انحراف") {
                    BodyText(it.firstDivergence)
                    it.hypotheses.take(5).forEach { h -> Text("${h.title} — اطمینان ${h.confidence}٪") }
                }
            }
        }
        item {
            SectionCard("پاسپورت مهندسی") {
                Text(
                    if (passport != null) "امتیاز کلی: ${passport.overall}٪" else "در حال محاسبه…",
                    fontWeight = FontWeight.Bold
                )
                passport?.competencies?.forEach { Text("${it.name}: ${it.score}٪") }
            }
        }
        item { Text("تزریق خطای کنترل‌شده", fontWeight = FontWeight.Bold) }
        items(faults, key = { it.id }) { f ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(f.title, modifier = Modifier.weight(1f))
                Button({ vm.injectV11Fault(f.id) }) { Text("تزریق") }
            }
        }
    }
}
