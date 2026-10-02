package com.example.netmaster.domain

import com.example.netmaster.data.EngineeringScenario
import com.example.netmaster.data.EngineeringScenarioHit

class ScenarioEngine(private val scenarios:List<EngineeringScenario>) {
    fun retrieve(query:String,topK:Int=8):List<EngineeringScenarioHit>{
        val q=normalize(query).split(Regex("\\s+")).filter{it.length>1}.toSet()
        if(q.isEmpty()) return emptyList()
        return scenarios.map { s ->
            val hay=normalize(listOf(s.title,s.domain,s.symptom,s.scope,s.tags.joinToString(" ")).joinToString(" ")).split(Regex("\\s+")).toSet()
            val overlap=q.count{it in hay}.toFloat()/q.size
            val exact=when{
                normalize(s.domain).contains(normalize(query))->0.25f
                normalize(s.title).contains(normalize(query))->0.30f
                else->0f
            }
            EngineeringScenarioHit(s,(overlap*0.75f+exact).coerceAtMost(1f))
        }.filter{it.score>0f}.sortedByDescending{it.score}.take(topK)
    }
    private fun normalize(text:String)=text.lowercase().replace('ي','ی').replace('ك','ک').replace(Regex("[^\\p{L}\\p{N}._:/-]+")," ").trim()
}
