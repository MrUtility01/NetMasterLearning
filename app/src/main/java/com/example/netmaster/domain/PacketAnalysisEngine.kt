package com.example.netmaster.domain

import com.example.netmaster.data.PacketRecord

class PacketAnalysisEngine {
    private val protocols=listOf("ARP","ICMP","TCP","UDP","DNS","DHCP","HTTP","HTTPS","TLS","SIP","RTP","IPv4","IPv6")
    fun parse(text:String): List<PacketRecord> =text.lines().filter{it.isNotBlank()}.take(10000).mapIndexed{idx,line->
        val p=protocols.firstOrNull{Regex("\\b${Regex.escape(it)}\\b",RegexOption.IGNORE_CASE).containsMatchIn(line)}?:"UNKNOWN"
        val ips=Regex("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b").findAll(line).map{it.value}.toList()
        val ports=Regex("(?<!\\d)(\\d{1,5})\\s*(?:→|->|:)").findAll(line).mapNotNull{it.groupValues[1].toIntOrNull()}.toList()
        val ports2=Regex("(?:→|->|:|\\s)(\\d{1,5})(?!\\d)").findAll(line).mapNotNull{it.groupValues[1].toIntOrNull()}.toList()
        val ps=(ports+ports2).distinct().filter{it in 0..65535}.take(2)
        val flags=listOf("SYN","ACK","FIN","RST","PSH","URG").filter{line.contains(it,true)}.joinToString(",")
        PacketRecord(idx+1,idx.toString(),ips.getOrNull(0).orEmpty(),ips.getOrNull(1).orEmpty(),p,line.trim(),line.toByteArray().size,ps.getOrNull(0),ps.getOrNull(1),flags,vlanId=Regex("vlan[ =:]+(\\d+)",RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)?.toIntOrNull(),ipVersion=when{line.contains("IPv6",true)->6;line.contains("IPv4",true)||ips.isNotEmpty()->4;else->null})
    }
    fun summary(records:List<PacketRecord>)=records.groupingBy{it.protocol}.eachCount().entries.sortedByDescending{it.value}.joinToString(", "){"${it.key}: ${it.value}"}
    fun findings(records:List<PacketRecord>): List<String>{
        val out=mutableListOf<String>();if(records.count{it.protocol=="TCP"}>=5&&records.any{it.info.contains("Retransmission",true)})out+="TCP retransmission detected";if(records.any{it.protocol=="ARP"&&it.info.contains("is-at",true)})out+="ARP resolution observed";if(records.any{it.protocol=="DNS"})out+="DNS traffic observed";if(records.any{it.protocol=="DHCP"})out+="DHCP exchange observed";if(records.any{it.protocol=="TCP"&&"RST" in it.tcpFlags})out+="TCP reset observed";return out.distinct()
    }
}
