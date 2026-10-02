package com.example.netmaster.domain

import java.util.Locale

/** Pure deterministic network CLI sandbox. It never opens a socket or touches a real device. */
data class SimulatedCommandResult(
    val command:String,
    val output:String,
    val state:SimNetworkState,
    val changed:Boolean=false,
    val success:Boolean=true,
    val category:String="show"
)

data class SimInterface(val name:String,val up:Boolean=true,val vlan:Int?=null,val mtu:Int=1500,val addresses:List<String> = emptyList())
data class SimRoute(val prefix:String,val nextHop:String?,val protocol:String="static",val distance:Int=1,val metric:Int=0)

data class SimNetworkState(
    val nodes:List<TwinNode>,
    val links:List<TwinLink>,
    val vlans:Set<Int> = setOf(10,20,30,40,50,60),
    val routes:List<SimRoute> = listOf(
        SimRoute("0.0.0.0/0","10.0.0.254","static"),
        SimRoute("192.168.10.0/24",null,"connected"),
        SimRoute("192.168.20.0/24",null,"connected"),
        SimRoute("192.168.30.0/24",null,"connected")
    ),
    val arp:Map<String,String> = mapOf("192.168.10.1" to "02:10:00:00:00:01","192.168.20.10" to "02:20:00:00:00:10"),
    val macTable:Map<String,String> = mapOf("02:10:00:00:00:01" to "Gi1/0/10","02:20:00:00:00:10" to "Gi1/0/20"),
    val interfaces:List<SimInterface> = listOf(
        SimInterface("Gi1/0/1",true,null,1500,listOf("10.0.0.2/30")),
        SimInterface("Gi1/0/10",true,10,1500,listOf("192.168.10.1/24")),
        SimInterface("Gi1/0/20",true,20,1500,listOf("192.168.20.1/24")),
        SimInterface("Gi1/0/30",true,30,1500,listOf("192.168.30.1/24"))
    ),
    val dnsHealthy:Boolean=true,
    val dhcpHealthy:Boolean=true,
    val natHealthy:Boolean=true,
    val firewallHealthy:Boolean=true,
    val ospfHealthy:Boolean=true,
    val bgpHealthy:Boolean=true,
    val stpHealthy:Boolean=true,
    val qosHealthy:Boolean=true
)

class CommandSimulator {
    fun defaultState():SimNetworkState {
        val nodes=listOf(
            TwinNode("edge","MikroTik Edge","Router","10.0.0.1"),TwinNode("core","Core Switch","Switch"),
            TwinNode("users","Users VLAN","VLAN","192.168.10.1",10),TwinNode("servers","Server VLAN","VLAN","192.168.20.1",20),
            TwinNode("dns","AD/DNS Server","Server","192.168.20.10",20),TwinNode("pbx","PBX","VoIP","192.168.30.10",30)
        )
        val links=listOf(TwinLink("edge","core","802.1Q"),TwinLink("core","users","VLAN 10"),TwinLink("core","servers","VLAN 20"),TwinLink("servers","dns","TCP/UDP 53"),TwinLink("core","pbx","VLAN 30"))
        return SimNetworkState(nodes,links)
    }

    fun execute(state:SimNetworkState,raw:String):SimulatedCommandResult {
        val cmd=raw.trim(); if(cmd.isBlank()) return result(cmd,"No command",state,false,false,"error")
        val l=cmd.lowercase(Locale.ROOT)
        return when {
            l=="show version" || l=="/system resource print" -> result(cmd,"NetMaster Network Simulator v3.1\nCPU: 2 virtual cores\nRAM: 2048 MB\nRouterOS/IOS semantic sandbox",state)
            l=="show vlan" || l=="show vlan brief" -> result(cmd,state.vlans.sorted().joinToString("\n"){"$it\tACTIVE"},state)
            l=="show interfaces trunk" -> result(cmd,state.links.filter{it.protocol=="802.1Q"}.joinToString("\n"){"${it.from}<->${it.to}\tallowed=${state.vlans.sorted().joinToString(",")}"},state)
            l=="show interfaces status" || l=="/interface print" -> result(cmd,state.interfaces.joinToString("\n"){"${it.name}\t${if(it.up)"up" else "down"}\tVLAN=${it.vlan?:"-"}\tMTU=${it.mtu}\t${it.addresses.joinToString(",")}"},state)
            l=="show mac address-table" -> result(cmd,state.macTable.entries.joinToString("\n"){"${it.key}\t${it.value}"},state)
            l=="show spanning-tree" -> result(cmd,if(state.stpHealthy)"Root bridge: CORE\nRSTP: forwarding\nNo topology change" else "Topology change detected\nRoot path inconsistency",state)
            l=="show etherchannel summary" || l=="show lacp neighbor" -> result(cmd,if(state.qosHealthy)"Port-channel1\tProtocol LACP\tMembers active" else "Port-channel1\tLACP member inconsistency",state)
            l=="show ip route" || l=="ip route" -> result(cmd,state.routes.joinToString("\n"){routeLine(it)},state)
            l=="/ip route print" -> result(cmd,state.routes.mapIndexed{i,r->"$i  ${routeLine(r)}"}.joinToString("\n"),state)
            l=="show arp" || l=="/ip arp print" || l=="arp -a" -> result(cmd,state.arp.entries.joinToString("\n"){"${it.key}\t${it.value}"},state)
            l=="show ip ospf neighbor" || l=="/routing ospf neighbor print" -> result(cmd,if(state.ospfHealthy)"Neighbor 10.0.0.2\tFULL\tarea 0.0.0.0" else "Neighbor 10.0.0.2\tEXSTART\tarea 0.0.0.0",state)
            l=="show ip bgp summary" || l=="/routing bgp session print" -> result(cmd,if(state.bgpHealthy)"10.0.0.9\tEstablished\tPrefixes=42" else "10.0.0.9\tActive\tPrefixes=0",state)
            l=="show firewall" || l=="/ip firewall filter print" -> result(cmd,if(state.firewallHealthy)"rules: input=accept-established,mgmt; forward=stateful" else "rule 0: DROP before established/related",state)
            l=="show nat" || l=="/ip firewall nat print" -> result(cmd,if(state.natHealthy)"srcnat masquerade: active\nhairpin: configured" else "srcnat rule inactive / conntrack mismatch",state)
            l=="show dhcp" || l=="/ip dhcp-server print" -> result(cmd,if(state.dhcpHealthy)"DHCP server: running\nScope: 192.168.10.100-192.168.10.200" else "DHCP server: stopped\nNo lease offers",state)
            l=="show dns" || l=="/ip dns print" -> result(cmd,if(state.dnsHealthy)"DNS: reachable\nServer: 192.168.20.10\nCache: healthy" else "DNS: unreachable\nServer: 192.168.20.10",state)
            l=="show qos" || l=="/queue simple print" -> result(cmd,if(state.qosHealthy)"VoIP class: priority 1\nVoice queue: low loss / low jitter" else "Voice queue: congestion\nDrops: elevated",state)
            l=="show logging" -> result(cmd,"2026-09-29 08:00:01 INFO link/core up\n2026-09-29 08:00:03 INFO route convergence stable\n2026-09-29 08:00:05 INFO DNS probe success=${state.dnsHealthy}",state)
            l=="show resource" || l=="/system resource print" -> result(cmd,"CPU 18%\nMemory 42%\nSessions 1842",state)
            l.startsWith("ping ") -> ping(state,cmd.substringAfter(' ').trim())
            l.startsWith("traceroute ") || l.startsWith("tracert ") -> trace(state,cmd.substringAfter(' ').trim())
            l.startsWith("nslookup ") || l.startsWith("dig ") -> dns(state,cmd.substringAfter(' ').trim())
            l.startsWith("tcpdump ") || l.startsWith("torch ") -> result(cmd,"SIM-CAPTURE\n1 TCP 192.168.10.50:51522 → 93.184.216.34:443 SYN\n2 TCP 93.184.216.34:443 → 192.168.10.50:51522 SYN,ACK",state)
            l.startsWith("ip addr") || l=="ifconfig" || l=="ipconfig" -> result(cmd,state.interfaces.joinToString("\n"){"${it.name}: ${if(it.up)"UP" else "DOWN"} ${it.addresses.joinToString(",")}"},state)
            l=="route print" || l=="get-netroute" -> result(cmd,state.routes.joinToString("\n"){routeLine(it)},state)
            l=="ip neigh" -> result(cmd,state.arp.entries.joinToString("\n"){"${it.key} dev sim lladdr ${it.value} REACHABLE"},state)
            l=="ss" || l=="get-nettcpconnection" -> result(cmd,"ESTABLISHED 192.168.10.50:51522 → 192.168.20.10:443\nESTABLISHED 192.168.10.50:51523 → 192.168.20.10:53",state)
            l=="dcdiag" -> result(cmd,if(state.dnsHealthy)"Directory Server tests: PASS\nDNS test: PASS" else "Directory Server tests: WARNING\nDNS test: FAIL",state)
            l.matches(Regex("vlan\\s+\\d+")) -> addVlan(state,cmd.substringAfter(' ').trim().toInt())
            l.matches(Regex("no vlan\\s+\\d+")) -> removeVlan(state,cmd.substringAfter("no vlan ").trim().toInt())
            l.startsWith("/ip route add ") -> addRoute(state,cmd.substringAfter("/ip route add ").trim())
            l.startsWith("/ip route remove ") || l.startsWith("no ip route ") -> removeRoute(state,cmd.substringAfterLast(' ').trim())
            l=="disable dns" -> mutate(cmd,state.copy(dnsHealthy=false),"DNS simulator flag disabled")
            l=="enable dns" -> mutate(cmd,state.copy(dnsHealthy=true),"DNS simulator flag enabled")
            l=="disable firewall" -> mutate(cmd,state.copy(firewallHealthy=false),"Firewall simulator flag disabled")
            l=="enable firewall" -> mutate(cmd,state.copy(firewallHealthy=true),"Firewall simulator flag enabled")
            l=="disable nat" -> mutate(cmd,state.copy(natHealthy=false),"NAT simulator flag disabled")
            l=="enable nat" -> mutate(cmd,state.copy(natHealthy=true),"NAT simulator flag enabled")
            l=="disable ospf" -> mutate(cmd,state.copy(ospfHealthy=false),"OSPF adjacency degraded")
            l=="enable ospf" -> mutate(cmd,state.copy(ospfHealthy=true),"OSPF adjacency restored")
            l=="disable bgp" -> mutate(cmd,state.copy(bgpHealthy=false),"BGP session moved to Active")
            l=="enable bgp" -> mutate(cmd,state.copy(bgpHealthy=true),"BGP session Established")
            l=="disable stp" -> mutate(cmd,state.copy(stpHealthy=false),"STP protection fault injected")
            l=="enable stp" -> mutate(cmd,state.copy(stpHealthy=true),"STP healthy")
            l=="disable qos" -> mutate(cmd,state.copy(qosHealthy=false),"QoS congestion injected")
            l=="enable qos" -> mutate(cmd,state.copy(qosHealthy=true),"QoS restored")
            else -> result(cmd,"Simulator: command not implemented. Try show vlan, show ip route, show arp, show interfaces trunk, show ip ospf neighbor, show ip bgp summary, ping, traceroute, nslookup, or disable/enable <feature>.",state,false,false,"error")
        }
    }

    private fun ping(state:SimNetworkState,target:String):SimulatedCommandResult {
        val normalized=target.trim()
        if(!state.firewallHealthy && normalized!="10.0.0.1") return result("ping $normalized","Request timed out.\nSIM cause: firewall policy",state)
        if(normalized=="192.168.20.10" || normalized=="192.168.20.1") return result("ping $normalized",if(state.vlans.contains(20))"Reply from $normalized: bytes=32 time<1ms TTL=64\n4 packets sent, 4 received, 0% loss" else "Destination host unreachable",state)
        if(normalized=="192.168.10.1") return result("ping $normalized",if(state.vlans.contains(10))"Reply from 192.168.10.1: bytes=32 time<1ms TTL=64" else "Destination host unreachable",state)
        if(normalized=="10.0.0.1") return result("ping $normalized","Reply from 10.0.0.1: bytes=32 time<1ms TTL=64",state)
        return if(state.routes.any{it.prefix=="0.0.0.0/0"}) result("ping $normalized","Reply from $normalized: bytes=32 time=8ms TTL=52\n4 packets sent, 4 received, 0% loss",state) else result("ping $normalized","Network is unreachable",state)
    }
    private fun trace(state:SimNetworkState,target:String):SimulatedCommandResult {
        val hops=if(state.routes.any{it.prefix=="0.0.0.0/0"}) listOf("1  10.0.0.1","2  10.0.0.254","3  $target") else listOf("1  10.0.0.1","2  * * *")
        return result("traceroute $target",hops.joinToString("\n"),state)
    }
    private fun dns(state:SimNetworkState,target:String)=result("nslookup $target",if(state.dnsHealthy)"Server: 192.168.20.10\nName: $target\nAddress: 93.184.216.34" else "DNS request timed out.\nNo response from 192.168.20.10",state)
    private fun addVlan(state:SimNetworkState,id:Int):SimulatedCommandResult = if(state.vlans.contains(id)) result("vlan $id","VLAN $id already exists",state,false,true,"config") else mutate("vlan $id",state.copy(vlans=state.vlans+id),"VLAN $id created","config")
    private fun removeVlan(state:SimNetworkState,id:Int):SimulatedCommandResult = if(!state.vlans.contains(id)) result("no vlan $id","VLAN $id does not exist",state,false,false,"config") else mutate("no vlan $id",state.copy(vlans=state.vlans-id),"VLAN $id removed from simulator","config")
    private fun addRoute(state:SimNetworkState,raw:String):SimulatedCommandResult { val p=raw.substringBefore(' '); val nh=raw.substringAfter("via ","").ifBlank{null}; return if(state.routes.any{it.prefix==p}) result("/ip route add $raw","Route already exists",state,false,true,"config") else mutate("/ip route add $raw",state.copy(routes=state.routes+SimRoute(p,nh,"static")),"Route $p added","config") }
    private fun removeRoute(state:SimNetworkState,key:String)=mutate("remove route",state.copy(routes=state.routes.filterNot{it.prefix==key}),"Removed route $key","config")
    private fun routeLine(r:SimRoute)="${when(r.protocol){"connected"->"C";"ospf"->"O";"bgp"->"B";else->"S"}} ${r.prefix}${r.nextHop?.let{" via $it"}.orEmpty()} [${r.distance}/${r.metric}]"
    private fun result(cmd:String,out:String,state:SimNetworkState,changed:Boolean=false,success:Boolean=true,category:String="show")=SimulatedCommandResult(cmd,out,state,changed,success,category)
    private fun mutate(cmd:String,state:SimNetworkState,out:String,category:String="config")=result(cmd,out,state,true,true,category)
}
