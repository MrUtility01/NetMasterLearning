package com.example.netmaster.search

import com.example.netmaster.data.Lesson
import com.example.netmaster.data.RagChunk
import com.example.netmaster.data.RagVectorEntity
import kotlin.math.max

/** Hybrid local RAG: vector similarity + lexical coverage + exact title/domain boosts + MMR diversity. */
class RagEngine(private val embedding:EmbeddingEngine=HashedEmbeddingEngine()) {
    data class Hit(val id:String,val title:String,val text:String,val score:Float,val source:String,val matchedTags:List<String> = emptyList())
    private data class VC(val chunk:RagChunk,val vector:FloatArray)
    private var index:List<VC> = emptyList()
    private var signature:Int=0

    fun buildIndex(lessons:List<Lesson>):Int {
        val sig=lessons.fold(1){a,l->31*a+l.hashCode()}
        if(sig==signature&&index.isNotEmpty())return index.size
        index=lessons.flatMap{lesson->
            val blocks=linkedMapOf(
                "goal" to lesson.goal,
                "concept" to listOf(lesson.simple,lesson.technical,lesson.keyPoints.joinToString("\n")).joinToString("\n"),
                "deep" to listOf(lesson.deepTechnical,lesson.topicSpecific.joinToString("\n"),lesson.commonMistakes.joinToString("\n")).joinToString("\n"),
                "packet" to listOf(lesson.packetWalkthrough,lesson.traffic,lesson.diagram).joinToString("\n"),
                "config" to listOf(lesson.configurationPlaybook,lesson.commands,lesson.platformCommands).joinToString("\n"),
                "lab" to listOf(lesson.lab,lesson.labSteps.joinToString("\n"),lesson.realScenario).joinToString("\n"),
                "troubleshooting" to listOf(lesson.troubleshooting,lesson.failureMatrix.joinToString("\n"){c->"${c.symptom} | ${c.hypothesis} | ${c.evidence} | ${c.next}"},lesson.evidenceChecklist.joinToString("\n")).joinToString("\n")
            )
            blocks.mapNotNull{(kind,text)->if(text.isBlank())null else{val c=RagChunk("${lesson.id}:$kind",lesson.title,text.take(7000),lesson.tags,"curriculum/${lesson.id}/$kind");VC(c,embedding.embed(c.title+"\n"+c.text+"\n"+c.tags.joinToString(" ")))} }
        }
        signature=sig
        return index.size
    }

    fun retrieve(query:String,topK:Int=8):List<Hit>{
        if(query.isBlank()||index.isEmpty())return emptyList()
        val qv=embedding.embed(query)
        val initial=index.map{vc->
            val lexical=lexicalScore(query,vc.chunk);val vector=embedding.cosine(qv,vc.vector)
            val titleBoost=if(vc.chunk.title.contains(query,true))0.20f else 0f
            Hit(vc.chunk.id,vc.chunk.title,vc.chunk.text,(vector*0.65f+lexical*0.20f+titleBoost).coerceIn(-1f,1f),vc.chunk.source,matchedTags=queryTags(query,vc.chunk.tags))
        }.sortedByDescending{it.score}.take(max(topK*4,16))
        return mmr(initial,qv,topK)
    }

    fun indexedChunks()=index.size
    fun vectorEntities()=index.map{v->RagVectorEntity(v.chunk.id,v.chunk.title,v.chunk.text,v.chunk.source,v.chunk.tags.joinToString(","),v.vector.joinToString(","))}
    fun hydrate(vectors:List<RagVectorEntity>){index=vectors.mapNotNull{v->val vec=v.vector.split(",").mapNotNull{it.toFloatOrNull()}.toFloatArray();if(vec.isEmpty())null else VC(RagChunk(v.id,v.title,v.text,v.tags.split(",").filter(String::isNotBlank),v.source),vec)};signature=0}

    private fun mmr(candidates:List<Hit>,qv:FloatArray,k:Int):List<Hit>{
        if(candidates.isEmpty())return emptyList();val remaining=candidates.toMutableList();val selected=mutableListOf<Hit>()
        while(remaining.isNotEmpty()&&selected.size<k){
            val next=remaining.maxByOrNull{h->val vc=index.first{it.chunk.id==h.id};val relevance=embedding.cosine(qv,vc.vector);val diversity=selected.maxOfOrNull{s->embedding.cosine(vc.vector,index.first{it.chunk.id==s.id}.vector)}?:0f;0.78f*relevance+0.22f*(1f-diversity)} ?: break
            selected+=next;remaining.remove(next)
        }
        return selected.sortedByDescending{it.score}
    }
    private fun lexicalScore(query:String,c:RagChunk):Float{val q=tokenize(query).toSet();if(q.isEmpty())return 0f;val hay=tokenize(c.title+" "+c.text+" "+c.tags.joinToString(" ")).toSet();return q.count{it in hay}.toFloat()/q.size}
    private fun queryTags(query:String,tags:List<String>)=tags.filter{query.contains(it,true)}
    private fun tokenize(t:String)=t.lowercase().replace('ي','ی').replace('ك','ک').split(Regex("[^\\p{L}\\p{N}._:/-]+" )).filter{it.length>1}
}
