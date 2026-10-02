package com.example.netmaster.ai

import com.example.netmaster.data.Lesson

data class AiEvidence(val type:String,val value:String)
data class AiHypothesis(val title:String,val confidence:Int,val evidenceToCheck:List<String>)
data class AiResponse(val title:String,val answer:String,val hypotheses:List<AiHypothesis> = emptyList(),val nextTests:List<String> = emptyList(),val safetyNotes:List<String> = emptyList())

enum class AiMode{TEACHER,SOCRATIC,TROUBLESHOOTER,EXAMINER,LAB_COACH,CONFIG_REVIEWER,AUTONOMOUS_COACH}
interface AiProvider{suspend fun ask(mode:AiMode,prompt:String,context:List<Lesson> = emptyList()):AiResponse}

class OfflineAiProvider:AiProvider{
    private fun groundedPrompt(prompt:String, context:List<Lesson>):String {
        val ctx=context.take(8).joinToString("\n\n"){l->"[SOURCE ${l.id}] ${l.title}\n${l.internals}\n${l.packet_state_walkthrough}\n${l.configurationPlaybook}\n${l.verification}\n${l.failure_analysis}"}
        return "Knowledge-grounded context:\n$ctx\n\nUser request:\n$prompt"
    }
    override suspend fun ask(mode:AiMode,prompt:String,context:List<Lesson>):AiResponse{
        val grounded=groundedPrompt(prompt,context)
        val related=context.take(4).joinToString("، "){it.title}
        return when(mode){
            AiMode.TEACHER->AiResponse("استاد NetMaster","$grounded\n\nموضوع را با ترتیب Concept → Architecture → Internals → Packet/State → Configuration → Verification → Troubleshooting → Lab → Production یاد بگیر. مباحث مرتبط: $related",safetyNotes=listOf("این پاسخ Offline و deterministic است."))
            AiMode.SOCRATIC->AiResponse("راهنمای سقراطی","$grounded\n\nبه‌جای حدس یک سؤال قابل‌آزمایش انتخاب کن: Scope چیست؟ کدام Layer محتمل است؟ چه Evidenceای آن را تأیید یا رد می‌کند؟",safetyNotes=listOf("هیچ Testای روی تجهیز واقعی اجرا نشده است."))
            AiMode.EXAMINER->AiResponse("آزمون","$grounded\n\nبدون نگاه به پاسخ، Symptom، Hypothesis، Evidence و اولین Test کم‌خطر را بنویس؛ سپس نتیجه را با معیار acceptance مقایسه کن.",safetyNotes=listOf("از تغییر Configuration برای پاسخ‌دادن به سؤال تا مشخص‌شدن علت خودداری کن."))
            AiMode.TROUBLESHOOTER, AiMode.AUTONOMOUS_COACH->AiResponse("Troubleshooting Coach","$grounded\n\nاز Interface/Link شروع کن، سپس ARP/ND، Route/FIB، Service/DNS و در پایان Policy/Firewall را بررسی کن.",listOf(AiHypothesis("Layered path fault",60,listOf("interface status","arp/nd","route","service probe"))),listOf("show interfaces status","show arp","show ip route","nslookup example.com"),listOf("فقط Simulator داخلی قابل اجرای خودکار است."))
            AiMode.LAB_COACH->AiResponse("Lab Coach","$grounded\n\nLab را به Baseline → Fault Injection → Capture/Logs → Root Cause → Fix → Rollback تقسیم کن.",safetyNotes=listOf("Change واقعی بدون تأیید انسانی انجام نمی‌شود."))
            AiMode.CONFIG_REVIEWER->AiResponse("Config Reviewer","$grounded\n\nConfiguration را از نظر ordering، scope، least privilege، logging و rollback بررسی کن.",safetyNotes=listOf("این بررسی جایگزین Review انسانی نیست."))
        }
    }
}
