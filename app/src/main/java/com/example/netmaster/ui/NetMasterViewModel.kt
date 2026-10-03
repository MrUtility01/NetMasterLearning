package com.example.netmaster.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*
import com.example.netmaster.export.ExportManager
import com.example.netmaster.search.GlobalSearchEngine
import com.example.netmaster.search.RagEngine
import com.example.netmaster.search.SearchEngine
import com.example.netmaster.search.UnifiedCatalogSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class NetMasterViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val repo = ContentRepository(app)
    private val offlineAi: AiProvider = OfflineAiProvider()
    private var localAi: AiProvider = LocalLlmProvider("http://10.0.2.2:11434", "llama3.2")
    private var activeAi: AiProvider = offlineAi
    val searchEngine = SearchEngine()
    val globalSearchEngine = GlobalSearchEngine(searchEngine)
    val unifiedCatalogSearch = UnifiedCatalogSearch()
    val ragEngine = RagEngine()
    val configDiffEngine = ConfigDiffEngine()
    val packetEngine = PacketAnalysisEngine()
    val pcapParser = PcapParser()
    val packetFilterEngine = PacketFilterEngine()
    val reviewEngine = ReviewEngine()
    val commandSimulator = CommandSimulator()
    val twinEngine = DigitalTwinEngine(commandSimulator)
    private var scenarioEngine = ScenarioEngine(emptyList())
    private var coachEngine = TroubleshootingCoach(commandSimulator, scenarioEngine)
    val v11Engine = V11EngineeringEngine()

    private val _curriculum = MutableStateFlow<Curriculum?>(null); val curriculum = _curriculum.asStateFlow()
    private val _progress = MutableStateFlow<Map<String, ProgressEntity>>(emptyMap()); val progress = _progress.asStateFlow()
    private val _notes = MutableStateFlow<List<NoteEntity>>(emptyList()); val notes = _notes.asStateFlow()
    private val _bookmarks = MutableStateFlow<Set<String>>(emptySet()); val bookmarks = _bookmarks.asStateFlow()
    private val _aiResponse = MutableStateFlow<AiResponse?>(null); val aiResponse = _aiResponse.asStateFlow()
    private val _twin = MutableStateFlow(twinEngine.defaultState()); val twin = _twin.asStateFlow()
    private val _exported = MutableStateFlow<File?>(null); val exported = _exported.asStateFlow()
    private val _incidents = MutableStateFlow<List<IncidentEntity>>(emptyList()); val incidents = _incidents.asStateFlow()
    private val _knowledge = MutableStateFlow(KnowledgeGraphView(emptyList(), emptyList())); val knowledge = _knowledge.asStateFlow()
    private val _captures = MutableStateFlow<List<PacketCaptureEntity>>(emptyList()); val captures = _captures.asStateFlow()
    private val _configs = MutableStateFlow<List<ConfigSnapshotEntity>>(emptyList()); val configs = _configs.asStateFlow()
    private val _dueReviews = MutableStateFlow<List<ReviewItemEntity>>(emptyList()); val dueReviews = _dueReviews.asStateFlow()
    private val _selectedIncident = MutableStateFlow<IncidentWorkspace?>(null); val selectedIncident = _selectedIncident.asStateFlow()
    private val _filteredPackets = MutableStateFlow<List<PacketRecord>>(emptyList()); val filteredPackets = _filteredPackets.asStateFlow()
    private val _basePackets = MutableStateFlow<List<PacketRecord>>(emptyList())
    private val _filterError = MutableStateFlow<String?>(null); val filterError = _filterError.asStateFlow()
    private val _simOutput = MutableStateFlow(""); val simOutput = _simOutput.asStateFlow()
    private val _coach = MutableStateFlow<CoachSession?>(null); val coach = _coach.asStateFlow()
    private val _vectorCount = MutableStateFlow(0); val vectorCount = _vectorCount.asStateFlow()
    private val _deepLessonCount = MutableStateFlow(0); val deepLessonCount = _deepLessonCount.asStateFlow()
    private val _scenarioCount = MutableStateFlow(0); val scenarioCount = _scenarioCount.asStateFlow()
    private val _aiBackend = MutableStateFlow("OFFLINE"); val aiBackend = _aiBackend.asStateFlow()
    private val _searchResults = MutableStateFlow<List<com.example.netmaster.search.GlobalSearchHit>>(emptyList()); val searchResults = _searchResults.asStateFlow()
    private val _selectedConfig = MutableStateFlow<ConfigSnapshotEntity?>(null); val selectedConfig = _selectedConfig.asStateFlow()
    private val _masterCurriculum = MutableStateFlow<MasterCurriculum?>(null); val masterCurriculum = _masterCurriculum.asStateFlow()
    private val _labCatalog = MutableStateFlow<LabCatalog?>(null); val labCatalog = _labCatalog.asStateFlow()
    private val _aiPlaybooks = MutableStateFlow<AiPlaybookCatalog?>(null); val aiPlaybooks = _aiPlaybooks.asStateFlow()
    private val _catalogCount = MutableStateFlow(0); val catalogCount = _catalogCount.asStateFlow()
    private val _v11Investigation = MutableStateFlow<V11EngineeringEngine.Investigation?>(null); val v11Investigation = _v11Investigation.asStateFlow()
    private val _v11Passport = MutableStateFlow<V11EngineeringEngine.Passport?>(null); val v11Passport = _v11Passport.asStateFlow()
    private val _v11Faults = MutableStateFlow(v11Engine.faultCatalog()); val v11Faults = _v11Faults.asStateFlow()
    private val _v11SelectedFault = MutableStateFlow<V11EngineeringEngine.FaultCase?>(null); val v11SelectedFault = _v11SelectedFault.asStateFlow()

    init { viewModelScope.launch { loadContent(); refresh() } }

    private suspend fun loadContent() {
        val base = repo.loadCurriculum()
        val deep = repo.loadDeepPack()
        val scenarios = repo.loadScenarios()
        val master = repo.loadMasterCurriculum()
        val labs = repo.loadLabs()
        val ai = repo.loadAiPlaybooks()
        val combined = DeepContentMapper.toCurriculum(base, deep)
        _curriculum.value = combined
        _deepLessonCount.value = deep.metadata.lessonCount
        _scenarioCount.value = scenarios.metadata.scenarioCount
        _masterCurriculum.value = master
        _labCatalog.value = labs
        _aiPlaybooks.value = ai
        unifiedCatalogSearch.rebuild(combined, deep, scenarios, master, labs, ai)
        _catalogCount.value = unifiedCatalogSearch.size()
        scenarioEngine = ScenarioEngine(scenarios.scenarios)
        coachEngine = TroubleshootingCoach(commandSimulator, scenarioEngine)
        val scenarioLessons = DeepContentMapper.scenariosAsLessons(scenarios)
        val all = combined.levels.flatMap { it.lessons } + scenarioLessons
        val persisted = dao.ragVectors()
        if (persisted.isNotEmpty() && persisted.count { it.source.startsWith("curriculum/") } >= combined.levels.sumOf { it.lessons.size }) {
            ragEngine.hydrate(persisted)
        } else {
            buildVectors(all, true)
        }
    }

    suspend fun refresh() {
        _progress.value = dao.progress().associateBy { it.lessonId }
        _notes.value = dao.notes()
        _bookmarks.value = dao.bookmarks().map { it.lessonId }.toSet()
        _incidents.value = dao.incidents()
        _knowledge.value = KnowledgeGraphView(dao.knowledgeNodes(), dao.knowledgeEdges())
        _captures.value = dao.captures()
        _configs.value = dao.allConfigs()
        _dueReviews.value = dao.dueReviews(System.currentTimeMillis(), 30)
        _vectorCount.value = dao.ragVectors().size
    }

    private suspend fun buildVectors(lessons: List<Lesson>, persist: Boolean) {
        val count = ragEngine.buildIndex(lessons)
        if (persist) {
            dao.clearRagVectors()
            dao.ragVectorsBatch(ragEngine.vectorEntities())
        }
        _vectorCount.value = count
    }

    fun rebuildVectorIndex() {
        viewModelScope.launch {
            val c = _curriculum.value ?: return@launch
            val scenarios = repo.loadScenarios()
            buildVectors(c.levels.flatMap { it.lessons } + DeepContentMapper.scenariosAsLessons(scenarios), true)
        }
    }

    fun setMastery(id: String, m: Mastery) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val next = when (m) {
                Mastery.UNKNOWN -> now
                Mastery.REVIEW -> now + ReviewDay * 1
                Mastery.KNOWN -> now + ReviewDay * 3
                Mastery.MASTERED -> now + ReviewDay * 14
            }
            dao.upsertProgress(ProgressEntity(id, m.name, m != Mastery.UNKNOWN, now, next))
            dao.review(if (m == Mastery.UNKNOWN) ReviewItemEntity(id, nextReviewAt = now, lastScore = 0) else reviewEngine.seed(id, m.name, now))
            refresh()
        }
    }

    fun gradeReview(lessonId: String, quality: Int) {
        viewModelScope.launch {
            dao.review(reviewEngine.update(dao.reviewByLesson(lessonId), lessonId, quality))
            val old = _progress.value[lessonId]
            if (old != null) {
                val mastery = when {
                    quality >= 5 -> Mastery.MASTERED
                    quality >= 4 -> Mastery.KNOWN
                    else -> Mastery.REVIEW
                }
                dao.upsertProgress(old.copy(mastery = mastery.name, completed = quality >= 4, nextReviewAt = dao.reviewByLesson(lessonId)?.nextReviewAt ?: System.currentTimeMillis(), updatedAt = System.currentTimeMillis()))
            }
            refresh()
        }
    }

    fun addNote(lessonId: String?, title: String, body: String, tags: String = "") {
        viewModelScope.launch {
            dao.insertNote(NoteEntity(lessonId = lessonId, title = title, body = body, tags = tags))
            refresh()
        }
    }

    fun toggleBookmark(id: String) {
        viewModelScope.launch {
            if (_bookmarks.value.contains(id)) dao.unbookmark(id) else dao.bookmark(BookmarkEntity(id))
            refresh()
        }
    }

    fun addSession(minutes: Int) {
        viewModelScope.launch {
            dao.session(StudySessionEntity(startedAt = System.currentTimeMillis(), minutes = minutes))
        }
    }

    fun configureLocalAi(endpoint: String, model: String, protocol: LocalLlmProtocol = LocalLlmProtocol.OLLAMA) {
        localAi = LocalLlmProvider(endpoint.trim(), model.trim(), protocol)
    }

    fun setAiBackend(local: Boolean) {
        activeAi = if (local) localAi else offlineAi
        _aiBackend.value = if (local) "LOCAL LLM" else "OFFLINE"
    }

    fun askAi(mode: AiMode, prompt: String) {
        viewModelScope.launch {
            val c = _curriculum.value ?: return@launch
            val retrieved = ragEngine.retrieve(prompt, 10)
            val ctx = retrieved.mapNotNull { h ->
                c.levels.asSequence().flatMap { it.lessons.asSequence() }.firstOrNull { h.id.startsWith(it.id + ":") }
            }.distinctBy { it.id }.take(10)
            val catalogHits = unifiedCatalogSearch.search(prompt, 8)
            val catalogContext = catalogHits.joinToString("\n---\n") { h -> "${h.kind} | ${h.title} | score=${h.score}\n${h.snippet}" }
            val grounded = prompt + "\n\n[Grounded RAG]\n" + retrieved.joinToString("\n---\n") { h ->
                "${h.title} | ${h.source} | score=${"%.3f".format(h.score)}\n${h.text.take(2200)}"
            } + "\n\n[Unified Catalog]\n" + catalogContext
            _aiResponse.value = activeAi.ask(mode, grounded, ctx)
        }
    }

    fun createIncident(title: String, symptom: String, severity: String = "MEDIUM") {
        viewModelScope.launch {
            val id = dao.incident(IncidentEntity(title = title, symptom = symptom, severity = severity))
            dao.incidentEvent(IncidentEventEntity(incidentId = id, eventType = "CREATED", message = "Incident created"))
            refresh()
            openIncident(id)
        }
    }

    fun openIncident(id: Long) {
        viewModelScope.launch {
            val i = dao.incidentById(id) ?: return@launch
            val ev = dao.evidence(id)
            val events = dao.incidentEvents(id)
            val capIds = ev.filter { it.type == "CAPTURE" }.mapNotNull { it.value.toLongOrNull() }
            val cfgIds = ev.filter { it.type == "CONFIG" }.mapNotNull { it.value.toLongOrNull() }
            val caps = capIds.mapNotNull { dao.captureById(it) }
            val cfgs = cfgIds.mapNotNull { dao.configById(it) }
            _selectedIncident.value = IncidentWorkspace(i, ev, events, caps, cfgs)
        }
    }

    fun closeIncident(i: IncidentEntity, root: String, resolution: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.updateIncident(i.copy(status = "RESOLVED", rootCause = root, resolution = resolution, updatedAt = now))
            dao.incidentEvent(IncidentEventEntity(incidentId = i.id, eventType = "RESOLVED", message = "Root cause: ${root.take(240)}"))
            openIncident(i.id)
            refresh()
        }
    }

    fun setIncidentStatus(i: IncidentEntity, status: String) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            dao.updateIncident(i.copy(status = status, updatedAt = now))
            dao.incidentEvent(IncidentEventEntity(incidentId = i.id, eventType = "STATUS", message = status))
            openIncident(i.id)
            refresh()
        }
    }

    fun addIncidentEvidence(id: Long, type: String, value: String) {
        viewModelScope.launch {
            dao.evidence(IncidentEvidenceEntity(incidentId = id, type = type, value = value))
            dao.incidentEvent(IncidentEventEntity(incidentId = id, eventType = "EVIDENCE", message = "$type: ${value.take(240)}"))
            openIncident(id)
            refresh()
        }
    }

    fun linkCaptureToIncident(id: Long, captureId: Long) = addIncidentEvidence(id, "CAPTURE", captureId.toString())
    fun linkConfigToIncident(id: Long, configId: Long) = addIncidentEvidence(id, "CONFIG", configId.toString())

    fun saveConfig(device: String, vendor: String, label: String, text: String) {
        viewModelScope.launch {
            val id = dao.config(ConfigSnapshotEntity(deviceId = device, vendor = vendor, label = label, content = text, fingerprint = configDiffEngine.fingerprint(text)))
            _selectedConfig.value = dao.configById(id)
            refresh()
        }
    }

    fun selectConfig(id: Long) { viewModelScope.launch { _selectedConfig.value = dao.configById(id) } }

    fun importCapture(name: String, raw: String) {
        viewModelScope.launch {
            val rows = packetEngine.parse(raw)
            dao.capture(PacketCaptureEntity(name = name, format = "TEXT", rawText = raw, packetCount = rows.size, protocolSummary = packetEngine.summary(rows)))
            _basePackets.value = rows
            _filteredPackets.value = rows
            refresh()
        }
    }

    fun importPcap(name: String, bytes: ByteArray) {
        viewModelScope.launch {
            val rows = pcapParser.parse(bytes)
            if (rows.isEmpty()) return@launch
            val raw = rows.joinToString("\n") { it.toText() }
            val format = if (bytes.size >= 4 && bytes[0].toInt() and 255 == 0x0A) "PCAPNG" else "PCAP"
            dao.capture(PacketCaptureEntity(name = name, format = format, rawText = raw, packetCount = rows.size, protocolSummary = packetEngine.summary(rows)))
            _basePackets.value = rows
            _filteredPackets.value = rows
            refresh()
        }
    }

    fun loadCapture(id: Long) {
        viewModelScope.launch {
            val c = dao.captureById(id) ?: return@launch
            val rows = packetEngine.parse(c.rawText)
            _basePackets.value = rows
            _filteredPackets.value = rows
            _filterError.value = null
        }
    }

    fun filterCapture(expression: String) {
        val r = packetFilterEngine.filter(_basePackets.value, expression)
        _filterError.value = r.error
        if (r.error == null) _filteredPackets.value = r.records
    }

    fun resetPacketFilter() {
        _filteredPackets.value = _basePackets.value
        _filterError.value = null
    }

    fun buildKnowledgeGraph() {
        viewModelScope.launch {
            val c = _curriculum.value ?: return@launch
            val nodes = mutableListOf<KnowledgeNodeEntity>()
            val edges = mutableListOf<KnowledgeEdgeEntity>()
            c.levels.forEach { level ->
                level.lessons.forEach { l ->
                    nodes += KnowledgeNodeEntity(l.id, l.title, "LESSON", l.technical, l.tags.joinToString(","))
                    l.tags.distinct().forEach { tag ->
                        val tagId = "tag:" + tag.lowercase()
                        nodes += KnowledgeNodeEntity(tagId, tag, "TAG", tag)
                        edges += KnowledgeEdgeEntity(l.id, tagId, "TAGGED_WITH")
                    }
                }
            }
            dao.knowledgeNodesBatch(nodes.distinctBy { it.id })
            dao.knowledgeEdgesBatch(edges.distinctBy { listOf(it.fromId, it.toId, it.relation) })
            refresh()
        }
    }

    fun injectFault(f: TwinFault) {
        _twin.value = twinEngine.inject(_twin.value, f)
        _coach.value = null
    }

    fun resetTwin() {
        _twin.value = twinEngine.defaultState()
        _coach.value = null
        _simOutput.value = ""
    }

    /** Switch live Digital Twin to a named multi-device topology preset. */
    fun loadTopologyPreset(presetId: String) {
        val preset = PacketPathEngine.presets().firstOrNull { it.id == presetId } ?: return
        val base = commandSimulator.defaultState().copy(nodes = preset.nodes, links = preset.links)
        _twin.value = TwinState(nodes = preset.nodes, links = preset.links, simulator = base)
        _coach.value = null
        _simOutput.value = "توپولوژی «${preset.title}» بارگذاری شد — ${preset.nodes.size} دیوایس"
    }

    fun recordEvidence(e: String) {
        _twin.value = twinEngine.recordEvidence(_twin.value, e)
    }

    fun runSimulator(command: String) {
        val state = _twin.value.simulator ?: commandSimulator.defaultState()
        val r = commandSimulator.execute(state, command)
        _simOutput.value = r.output
        if (r.changed) _twin.value = _twin.value.copy(simulator = r.state, nodes = r.state.nodes, links = r.state.links)
    }

    fun saveSimulatorSnapshot(label: String) {
        viewModelScope.launch {
            val state = _twin.value.simulator ?: commandSimulator.defaultState()
            dao.simulatorSnapshot(SimulatorSnapshotEntity(label = label, stateJson = SimulatorCodec.encode(state)))
            refresh()
        }
    }

    fun startCoach(symptom: String) {
        val state = _twin.value.simulator ?: commandSimulator.defaultState()
        val c = coachEngine.start(symptom, state, _filteredPackets.value)
        _coach.value = c
        viewModelScope.launch {
            dao.coachRun(CoachRunEntity(symptom = c.symptom, scenarioId = c.scenarioId, status = c.status, transcript = "START\n${c.hypotheses.joinToString(" | ") { it.title }}"))
        }
    }

    fun coachNext() {
        val c = _coach.value ?: return
        val state = _twin.value.simulator ?: commandSimulator.defaultState()
        val (next, r) = coachEngine.executeNext(c, state)
        _coach.value = next
        if (r != null) {
            _simOutput.value = r.output
            _twin.value = _twin.value.copy(simulator = r.state)
        }
        viewModelScope.launch {
            dao.coachRun(CoachRunEntity(symptom = next.symptom, scenarioId = next.scenarioId, status = next.status, transcript = next.evidence.takeLast(8).joinToString("\n")))
        }
    }

    fun searchAll(query: String) {
        viewModelScope.launch {
            val local = globalSearchEngine.search(
                _curriculum.value?.levels?.flatMap { it.lessons }.orEmpty(),
                _notes.value, _incidents.value, _configs.value, _captures.value, query
            )
            val catalog = unifiedCatalogSearch.search(query)
            _searchResults.value = (local + catalog).groupBy { it.kind + ":" + it.id }.values.map { it.maxBy { h -> h.score } }.sortedByDescending { it.score }.take(50)
        }
    }

    fun investigateV11(symptom: String, evidence: String) {
        viewModelScope.launch {
            val c = _curriculum.value ?: return@launch
            _v11Investigation.value = v11Engine.investigate(symptom, evidence, c.levels.flatMap { it.lessons }, _filteredPackets.value)
        }
    }

    fun computeV11Passport() {
        viewModelScope.launch {
            val c = _curriculum.value ?: return@launch
            val resolved = _incidents.value.count { it.status == "RESOLVED" }
            _v11Passport.value = v11Engine.passport(_progress.value, resolved, _captures.value.size, c.levels.flatMap { it.lessons })
        }
    }

    fun injectV11Fault(id: String) {
        val f = v11Engine.faultCatalog().firstOrNull { it.id == id } ?: return
        _v11SelectedFault.value = f
        val mapped = when (id) {
            "v11-dhcp-offer" -> "dhcp"
            "v11-dns-servfail" -> "dns"
            "v11-ospf-mtu" -> "ospf"
            "v11-vlan-allow" -> "vlan"
            "v11-tcp-mss" -> "mtu"
            "v11-fw-order" -> "firewall"
            "v11-nat-state" -> "nat"
            "v11-stp-root" -> "stp"
            else -> ""
        }
        vmFault(mapped)
    }

    private fun vmFault(id: String) {
        if (id.isBlank()) return
        val tf = twinEngine.faults().firstOrNull { it.id == id } ?: return
        _twin.value = twinEngine.inject(_twin.value, tf)
        _coach.value = null
    }

    fun clearV11() {
        _v11Investigation.value = null
        _v11SelectedFault.value = null
        computeV11Passport()
    }

    fun exportBackup() {
        viewModelScope.launch {
            _exported.value = ExportManager.createBackup(
                getApplication(), _curriculum.value, _progress.value.values.toList(), _notes.value,
                _bookmarks.value.map { BookmarkEntity(it) }, incidents = _incidents.value,
                evidence = dao.allIncidentEvidence(), configs = _configs.value, captures = _captures.value,
                reviews = dao.allReviews(), vectors = dao.ragVectors(), knowledgeNodes = dao.knowledgeNodes(),
                knowledgeEdges = dao.knowledgeEdges(), simulatorSnapshots = dao.simulatorSnapshots(),
                coachRuns = dao.coachRuns(), events = dao.allIncidentEvents()
            )
        }
    }

    private fun PacketRecord.toText() = "${number}\t${timestamp}\t${source}\t${destination}\t${protocol}\t${info}"

    companion object {
        const val ReviewDay = 86_400_000L
    }
}
