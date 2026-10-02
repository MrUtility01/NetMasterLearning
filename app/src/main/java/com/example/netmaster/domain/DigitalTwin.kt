package com.example.netmaster.domain

data class TwinNode(val id:String,val name:String,val type:String,val ip:String?=null,val vlan:Int?=null)
data class TwinLink(val from:String,val to:String,val protocol:String="Ethernet",val up:Boolean=true)
data class TwinFault(val id:String,val title:String,val description:String,val target:String,val hint:String,val severity:String="MEDIUM")
data class TwinState(val nodes:List<TwinNode>,val links:List<TwinLink>,val activeFault:TwinFault?=null,val evidence:List<String> = emptyList(),val score:Int=0,val simulator:SimNetworkState?=null)

class DigitalTwinEngine(private val simulator:CommandSimulator=CommandSimulator()) {
    fun defaultState():TwinState {
        val s=simulator.defaultState()
        return TwinState(s.nodes,s.links,simulator=s)
    }
    fun faults(): List<TwinFault> =listOf(
        TwinFault("dhcp","DHCP Offer Failure","کلاینت Discover می‌فرستد اما Offer دریافت نمی‌کند.","edge","DHCP server/relay، VLAN و UDP 67/68 را بررسی کن.","HIGH"),
        TwinFault("dns","DNS Failure","کاربران IP دارند اما نام دامنه resolve نمی‌شود.","dns","ping DNS server و nslookup را بررسی کن.","HIGH"),
        TwinFault("vlan","VLAN/Trunk Mismatch","کاربر به Gateway دسترسی ندارد.","core","Access VLAN و allowed VLAN را بررسی کن.","HIGH"),
        TwinFault("route","Missing Route","شبکه مقصد از Edge قابل دسترسی نیست.","edge","Route/FIB و next-hop را بررسی کن.","HIGH"),
        TwinFault("firewall","Firewall Rule Order","ترافیک مجاز توسط Drop بالاتر قطع می‌شود.","edge","Rule order و counters را بررسی کن.","HIGH"),
        TwinFault("nat","NAT/PAT Failure","LAN به Internet route دارد ولی translation انجام نمی‌شود.","edge","srcnat/conntrack را بررسی کن.","MEDIUM"),
        TwinFault("ospf","OSPF Adjacency","Routeهای داخلی ناپدید شده‌اند.","edge","Neighbor state و area/hello را بررسی کن.","HIGH"),
        TwinFault("bgp","BGP Session","Prefixهای upstream دریافت نمی‌شوند.","edge","Session state و policy را بررسی کن.","HIGH"),
        TwinFault("stp","STP Topology Fault","Topology change و path instability دیده می‌شود.","core","Root/role/state و BPDU را بررسی کن.","HIGH"),
        TwinFault("mtu","TCP MSS / MTU Black Hole","Handshake works but larger transfers stall.","edge","Check interface MTU, PMTUD, MSS and blocked ICMP too-big messages.","HIGH"),
        TwinFault("qos","QoS Congestion","VoIP jitter/loss در زمان congestion بالا می‌رود.","core","Queue/marking و interface drops را بررسی کن.","MEDIUM")
    )
    fun inject(state:TwinState,fault:TwinFault):TwinState {
        val sim=state.simulator?:simulator.defaultState()
        val modified=when(fault.id){
            "dhcp"->sim.copy(dhcpHealthy=false)
            "dns"->sim.copy(dnsHealthy=false)
            "vlan"->sim.copy(vlans=sim.vlans-10)
            "route"->sim.copy(routes=sim.routes.filterNot{it.prefix=="192.168.20.0/24"})
            "firewall"->sim.copy(firewallHealthy=false)
            "nat"->sim.copy(natHealthy=false)
            "ospf"->sim.copy(ospfHealthy=false,routes=sim.routes.filterNot{it.protocol=="ospf"})
            "bgp"->sim.copy(bgpHealthy=false,routes=sim.routes.filterNot{it.protocol=="bgp"})
            "stp"->sim.copy(stpHealthy=false)
            "mtu"->sim.copy(interfaces=sim.interfaces.map{it.copy(mtu=1400)})
            "qos"->sim.copy(qosHealthy=false)
            else->sim
        }
        return state.copy(activeFault=fault,evidence=emptyList(),score=100,simulator=modified)
    }
    fun recordEvidence(state:TwinState,evidence:String):TwinState=state.copy(evidence=(state.evidence+evidence).distinct(),score=(state.score-5).coerceAtLeast(0))
    fun reset(state:TwinState)=defaultState().copy(nodes=state.nodes,links=state.links)
}
