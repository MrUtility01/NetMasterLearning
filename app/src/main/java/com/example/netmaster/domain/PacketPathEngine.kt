package com.example.netmaster.domain

/** Hop-by-hop packet path analysis across topology devices (offline, deterministic). */
data class PacketHop(
    val step: Int,
    val deviceId: String,
    val deviceName: String,
    val deviceType: String,
    val action: String,
    val layer: String,
    val detail: String,
    val ok: Boolean = true
)

data class PacketPathResult(
    val scenarioId: String,
    val title: String,
    val summary: String,
    val hops: List<PacketHop>,
    val protocols: List<String>,
    val brokenAt: String? = null
)

data class TopologyPreset(
    val id: String,
    val title: String,
    val description: String,
    val nodes: List<TwinNode>,
    val links: List<TwinLink>
)

object PacketPathEngine {

    fun presets(): List<TopologyPreset> = listOf(
        TopologyPreset(
            id = "campus",
            title = "Campus Enterprise",
            description = "Edge Router + Core + Access + Server + DNS + PBX + Client + Firewall + Internet",
            nodes = listOf(
                TwinNode("inet", "Internet", "Cloud", "8.8.8.8"),
                TwinNode("fw", "Firewall", "Firewall", "10.0.0.254"),
                TwinNode("edge", "Edge Router", "Router", "10.0.0.1"),
                TwinNode("core", "Core Switch", "Switch"),
                TwinNode("acc", "Access Switch", "Switch"),
                TwinNode("user", "Client PC", "Host", "192.168.10.50", 10),
                TwinNode("srv", "App Server", "Server", "192.168.20.20", 20),
                TwinNode("dns", "DNS Server", "Server", "192.168.20.10", 20),
                TwinNode("pbx", "PBX", "VoIP", "192.168.30.10", 30),
                TwinNode("ap", "WiFi AP", "AP", vlan = 10)
            ),
            links = listOf(
                TwinLink("inet", "fw", "WAN"),
                TwinLink("fw", "edge", "Ethernet"),
                TwinLink("edge", "core", "802.1Q"),
                TwinLink("core", "acc", "Trunk"),
                TwinLink("acc", "user", "Access VLAN10"),
                TwinLink("acc", "ap", "Access VLAN10"),
                TwinLink("core", "srv", "VLAN20"),
                TwinLink("core", "dns", "VLAN20"),
                TwinLink("core", "pbx", "VLAN30")
            )
        ),
        TopologyPreset(
            id = "branch",
            title = "Branch Office",
            description = "Router + Switch + Clients + Local DNS",
            nodes = listOf(
                TwinNode("inet", "ISP", "Cloud"),
                TwinNode("r1", "Branch Router", "Router", "10.1.0.1"),
                TwinNode("sw1", "Branch Switch", "Switch"),
                TwinNode("pc1", "PC-1", "Host", "192.168.1.10", 1),
                TwinNode("pc2", "PC-2", "Host", "192.168.1.11", 1),
                TwinNode("dns", "Local DNS", "Server", "192.168.1.2", 1)
            ),
            links = listOf(
                TwinLink("inet", "r1", "PPPoE"),
                TwinLink("r1", "sw1", "Ethernet"),
                TwinLink("sw1", "pc1", "Access"),
                TwinLink("sw1", "pc2", "Access"),
                TwinLink("sw1", "dns", "Access")
            )
        ),
        TopologyPreset(
            id = "dc",
            title = "Data Center",
            description = "Spine-Leaf + LoadBalancer + App + DB",
            nodes = listOf(
                TwinNode("spine", "Spine", "Switch"),
                TwinNode("leaf1", "Leaf-1", "Switch"),
                TwinNode("leaf2", "Leaf-2", "Switch"),
                TwinNode("lb", "Load Balancer", "LB", "10.10.0.10"),
                TwinNode("app", "App Node", "Server", "10.10.1.10"),
                TwinNode("db", "Database", "Server", "10.10.2.10"),
                TwinNode("fw", "DC Firewall", "Firewall")
            ),
            links = listOf(
                TwinLink("fw", "spine", "L3"),
                TwinLink("spine", "leaf1", "Fabric"),
                TwinLink("spine", "leaf2", "Fabric"),
                TwinLink("leaf1", "lb", "VLAN100"),
                TwinLink("leaf1", "app", "VLAN100"),
                TwinLink("leaf2", "db", "VLAN200")
            )
        )
    )

    fun analyze(
        scenarioId: String,
        state: TwinState
    ): PacketPathResult {
        val sim = state.simulator
        val nodes = state.nodes.associateBy { it.id }
        fun hop(step: Int, id: String, action: String, layer: String, detail: String, ok: Boolean = true): PacketHop {
            val n = nodes[id]
            return PacketHop(step, id, n?.name ?: id, n?.type ?: "?", action, layer, detail, ok)
        }

        return when (scenarioId) {
            "dns" -> {
                val dnsOk = sim?.dnsHealthy != false
                val fwOk = sim?.firewallHealthy != false
                val hops = buildList {
                    add(hop(1, "user", "Query", "L7 DNS", "Client می‌فرستد: A? example.com → 192.168.20.10:53"))
                    add(hop(2, "acc", "Forward", "L2", "Switch MAC lookup → Core"))
                    add(hop(3, "core", "Switch", "L2/L3", "VLAN20 به DNS Server"))
                    if (!dnsOk) {
                        add(hop(4, "dns", "Timeout", "L7 DNS", "DNS پاسخ نمی‌دهد — سرویس down", false))
                    } else if (!fwOk) {
                        add(hop(4, "fw", "Drop", "L4", "فایروال UDP/53 را drop می‌کند", false))
                        add(hop(5, "dns", "No reply", "L7", "بسته به سرور نمی‌رسد", false))
                    } else {
                        add(hop(4, "dns", "Answer", "L7 DNS", "Response: example.com → 93.184.216.34"))
                        add(hop(5, "core", "Return", "L2", "برگشت به Client"))
                        add(hop(6, "user", "Resolve OK", "L7", "Client IP مقصد را دارد"))
                    }
                }
                PacketPathResult(
                    "dns", "تحلیل بسته DNS",
                    if (dnsOk && fwOk) "مسیر DNS سالم است" else "مسیر DNS در یکی از دیوایس‌ها قطع شده",
                    hops, listOf("UDP", "DNS"), if (!dnsOk) "dns" else if (!fwOk) "fw" else null
                )
            }
            "http" -> {
                val natOk = sim?.natHealthy != false
                val fwOk = sim?.firewallHealthy != false
                val hops = buildList {
                    add(hop(1, "user", "SYN", "L4 TCP", "192.168.10.50:51522 → 93.184.216.34:443"))
                    add(hop(2, "acc", "Forward", "L2", "به Edge از طریق Core"))
                    add(hop(3, "edge", "Route", "L3", "Lookup default route 0.0.0.0/0"))
                    if (!natOk) {
                        add(hop(4, "edge", "NAT fail", "L3/L4", "srcnat/masquerade غیرفعال — ترجمه انجام نشد", false))
                    } else {
                        add(hop(4, "edge", "NAT", "L3/L4", "srcnat: 192.168.10.50 → Public IP"))
                        if (!fwOk) {
                            add(hop(5, "fw", "Drop", "L4", "سیاست فایروال TCP/443 را drop کرد", false))
                        } else {
                            add(hop(5, "fw", "Allow", "L4", "Established/related یا allow HTTPS"))
                            add(hop(6, "inet", "SYN-ACK", "L4", "سرور اینترنت پاسخ می‌دهد"))
                            add(hop(7, "user", "Established", "L4", "Handshake کامل — TLS شروع می‌شود"))
                        }
                    }
                }
                PacketPathResult(
                    "http", "تحلیل بسته HTTPS به اینترنت",
                    if (natOk && fwOk) "مسیر HTTPS از Client تا Internet سالم" else "قطع در NAT یا Firewall",
                    hops, listOf("TCP", "TLS", "HTTPS"), if (!natOk) "edge" else if (!fwOk) "fw" else null
                )
            }
            "dhcp" -> {
                val dhcpOk = sim?.dhcpHealthy != false
                val hops = buildList {
                    add(hop(1, "user", "Discover", "L7 DHCP", "Broadcast DHCP Discover (0.0.0.0 → 255.255.255.255)"))
                    add(hop(2, "acc", "Broadcast", "L2", "Flood در VLAN 10"))
                    if (!dhcpOk) {
                        add(hop(3, "edge", "No Offer", "L7 DHCP", "DHCP server/relay پاسخ نمی‌دهد", false))
                    } else {
                        add(hop(3, "edge", "Offer", "L7 DHCP", "DHCP Offer: 192.168.10.50/24 gw 192.168.10.1"))
                        add(hop(4, "user", "Request", "L7 DHCP", "Client Request همان IP"))
                        add(hop(5, "edge", "ACK", "L7 DHCP", "Lease تأیید شد"))
                    }
                }
                PacketPathResult(
                    "dhcp", "تحلیل بسته DHCP",
                    if (dhcpOk) "چهار مرحله DORA کامل شد" else "Discover بدون Offer — DHCP خراب",
                    hops, listOf("UDP", "DHCP"), if (!dhcpOk) "edge" else null
                )
            }
            "voip" -> {
                val qosOk = sim?.qosHealthy != false
                val hops = buildList {
                    add(hop(1, "user", "SIP INVITE", "L7 SIP", "Client → PBX 192.168.30.10:5060"))
                    add(hop(2, "core", "VLAN30", "L2", "سوییچ به VLAN Voice"))
                    add(hop(3, "pbx", "200 OK", "L7 SIP", "تماس برقرار — RTP شروع می‌شود"))
                    if (!qosOk) {
                        add(hop(4, "core", "Congestion", "L2/QoS", "صف Voice drop دارد — jitter/loss", false))
                    } else {
                        add(hop(4, "core", "Priority", "QoS", "Voice queue priority — loss پایین"))
                        add(hop(5, "user", "RTP OK", "L4 UDP", "رسانه صوتی پایدار"))
                    }
                }
                PacketPathResult(
                    "voip", "تحلیل بسته VoIP/SIP",
                    if (qosOk) "مسیر Voice سالم با QoS" else "QoS congestion روی مسیر Voice",
                    hops, listOf("SIP", "RTP", "UDP"), if (!qosOk) "core" else null
                )
            }
            else -> PacketPathResult(
                scenarioId, "سناریو نامشخص", "سناریوی پشتیبانی‌نشده",
                emptyList(), emptyList()
            )
        }
    }

    fun scenarios(): List<Pair<String, String>> = listOf(
        "dns" to "DNS Resolve",
        "http" to "HTTPS اینترنت",
        "dhcp" to "DHCP Lease",
        "voip" to "VoIP / SIP"
    )
}
