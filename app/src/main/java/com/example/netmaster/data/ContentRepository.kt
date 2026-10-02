package com.example.netmaster.data

import android.content.Context
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class ContentRepository(private val context:Context) {
    private val json=Json{ignoreUnknownKeys=true}

    fun loadCurriculum():Curriculum=load("curriculum.json")
    fun loadDeepPack():DeepContentPack=load("deep/expert_engineering.json")
    fun loadScenarios():ScenarioPack=load("deep/scenarios.json")
    fun loadMasterCurriculum():MasterCurriculum=load("master/netmaster_master_curriculum.json")
    fun loadLabs():LabCatalog=load("master/lab_catalog.json")
    fun loadAiPlaybooks():AiPlaybookCatalog=load("master/ai_playbooks.json")

    private inline fun <reified T> load(path:String):T {
        return context.assets.open(path).bufferedReader(Charsets.UTF_8).use { json.decodeFromString(it.readText()) }
    }
}
