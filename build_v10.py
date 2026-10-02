import json, os, re, hashlib, zipfile, shutil
from pathlib import Path
ROOT=Path('/mnt/data/nm10_work')
web=ROOT/'web'; assets=web/'assets'; assets.mkdir(parents=True,exist_ok=True)
# load curriculum and supporting catalogs
cur=json.loads((assets/'curriculum.json').read_text(encoding='utf-8'))
lessons=[l for lv in cur['levels'] for l in lv['lessons']]
# Knowledge graph: lesson <-> tags <-> levels, plus title-token links for high-value overlaps
nodes=[]; edges=[]; seen=set()
def node(i,t,k,label,meta=None):
    if i in seen:return
    seen.add(i);nodes.append({'id':i,'type':k,'label':label,'meta':meta or {}})
for lv in cur['levels']:
    lid=f'level:{lv["id"]}'; node(lid,'LEVEL','level',lv['title'],{'summary':lv.get('summary','')})
    for l in lv['lessons']:
        node(l['id'],'LESSON','lesson',l['title'],{'level':lv['id'],'tags':l.get('tags',[])})
        edges.append({'from':l['id'],'to':lid,'relation':'IN_LEVEL'})
        for t in l.get('tags',[]):
            tid='tag:'+re.sub(r'\s+','-',t.lower())
            node(tid,'TAG','tag',t); edges.append({'from':l['id'],'to':tid,'relation':'TAGGED_WITH'})
# protocol/entity alias relationships from tags/title overlap
for l in lessons:
    words=set(re.findall(r'[\w.-]{3,}', (l.get('title','')+' '+' '.join(l.get('tags',[]))).lower()))
    for other in lessons:
        if l['id']>=other['id']: continue
        ow=set(re.findall(r'[\w.-]{3,}', (other.get('title','')+' '+' '.join(other.get('tags',[]))).lower()))
        overlap=words & ow
        if len(overlap)>=2:
            edges.append({'from':l['id'],'to':other['id'],'relation':'RELATED','evidence':sorted(overlap)[:6]})
            if len([e for e in edges if e['from']==l['id'] and e['relation']=='RELATED'])>=4: break
kg={'version':'10.0','nodeCount':len(nodes),'edgeCount':len(edges),'nodes':nodes,'edges':edges}
(assets/'knowledge_graph_v10.json').write_text(json.dumps(kg,ensure_ascii=False),encoding='utf-8')
# adaptive competency map: level/tag coverage; deterministic initial state
competencies=[]
for lv in cur['levels']:
    competencies.append({'id':f'level:{lv["id"]}','name':lv['title'],'lessons':len(lv['lessons']),'mastery':0,'evidence':0,'next':'Learn → Lab → Fault Injection → Explain'})
profile={'version':'10.0','algorithm':'evidence-weighted mastery','masteryLevels':['UNKNOWN','LEARNING','PRACTICING','COMPETENT','MASTERED','FORENSICS'],'competencies':competencies,'rules':[
 {'signal':'lesson_completed','weight':0.15},{'signal':'quiz_correct','weight':0.20},{'signal':'lab_verified','weight':0.25},{'signal':'incident_root_cause','weight':0.25},{'signal':'forensics_first_divergence','weight':0.15}]}
(assets/'adaptive_profile_v10.json').write_text(json.dumps(profile,ensure_ascii=False),encoding='utf-8')
# incident playbooks
playbooks=[
 {'id':'pb-tcp-retransmission','title':'TCP Retransmission / Loss','symptoms':['slow application','duplicate ACK','retransmission','timeout'],'layers':['Ethernet','IP','TCP','Application'],'tests':['capture both directions','check RTT/jitter/loss','inspect ACK progression','check MTU/PMTUD','check congestion/window'],'rootCauses':['packet loss','asymmetric path','MTU black-hole','receiver window','congestion']},
 {'id':'pb-dhcp-failure','title':'DHCP Lease Failure','symptoms':['no IP','169.254','DORA incomplete','DHCP timeout'],'layers':['Ethernet','VLAN','DHCP'],'tests':['check link/VLAN','capture DHCP Discover','verify relay giaddr','inspect server scope','check UDP 67/68 policy'],'rootCauses':['wrong VLAN','relay failure','scope exhaustion','firewall policy','server unreachable']},
 {'id':'pb-dns-failure','title':'DNS Resolution Failure','symptoms':['name resolution','SERVFAIL','NXDOMAIN','timeout'],'layers':['IP','UDP/TCP','DNS'],'tests':['query authoritative','check recursion','inspect cache/TTL','test TCP fallback','verify reachability'],'rootCauses':['delegation','recursive resolver','policy','transport','stale/negative cache']},
 {'id':'pb-ospf-neighbor','title':'OSPF Neighbor Down','symptoms':['FULL missing','neighbor down','LSDB mismatch','route missing'],'layers':['Ethernet','IP','OSPF'],'tests':['interface/MTU','hello/dead timers','area/auth','network type','LSDB/SPF/RIB'],'rootCauses':['MTU mismatch','timer mismatch','area/auth mismatch','link issue','route policy']},
 {'id':'pb-vlan-isolation','title':'VLAN / Trunk Isolation','symptoms':['same VLAN unreachable','native VLAN','tagged traffic missing'],'layers':['Ethernet','802.1Q','Switching'],'tests':['MAC table','allowed VLANs','tag/native VLAN','STP state','access VLAN'],'rootCauses':['wrong access VLAN','trunk allow-list','native mismatch','STP blocking','CAM learning']}
]
(assets/'incident_playbooks_v10.json').write_text(json.dumps({'version':'10.0','playbooks':playbooks},ensure_ascii=False),encoding='utf-8')
# packet journey templates
journeys={'version':'10.0','journeys':[
 {'id':'tcp-internet','title':'TCP Application Flow to External Destination','steps':['Application data','DNS resolution','ARP/ND for gateway','Ethernet frame','IPv4/IPv6 routing','NAT/conntrack if applicable','Firewall state/policy','WAN encapsulation','Remote routing','TCP ACK/data progression','Application response'],'evidence':['DNS query/response','ARP/ND','MAC rewrite per hop','TTL/Hop Limit decrement','NAT tuple','TCP sequence/ACK','retransmission/window']},
 {'id':'dhcp-client','title':'DHCP DORA Across a Relay','steps':['DHCPDISCOVER broadcast','Access VLAN / broadcast domain','DHCP relay / giaddr','DHCP server scope selection','DHCPOFFER','DHCPREQUEST','DHCPACK','ARP/duplicate-address checks','Lease timers / renewal'],'evidence':['xid','giaddr','options 53/54/50','UDP 67/68','relay hop count','lease T1/T2']},
 {'id':'ospf-convergence','title':'OSPF Convergence','steps':['Interface up','Hello exchange','2-Way','ExStart/Exchange','Loading','Full','LSDB install','SPF calculation','RIB/FIB update','Forwarding convergence'],'evidence':['Hello timers','MTU','RID','LSA sequence','LSDB','SPF reason','RIB/FIB']}
]}
(assets/'packet_journeys_v10.json').write_text(json.dumps(journeys,ensure_ascii=False),encoding='utf-8')
# release docs
(ROOT/'docs/V10_INTELLIGENT_ENGINEERING.md').write_text('''# NetMaster V10 — Intelligent Engineering Layer\n\nV10 adds an intelligence/correlation layer above the V9.1 engines.\n\n## Core pipeline\nIncident → Evidence → Retrieval → Hypotheses → Discriminating Tests → First Divergence → Root Cause → Remediation → Verification → RCA\n\n## New offline assets\n- knowledge_graph_v10.json\n- adaptive_profile_v10.json\n- incident_playbooks_v10.json\n- packet_journeys_v10.json\n\n## Safety\nNo real-device mutation is performed by the offline engine. Configuration changes require explicit human approval.\n\n## AI\nLocal/offline responses are grounded in retrieved lessons and evidence. External LLMs remain adapters, not mandatory dependencies.\n''',encoding='utf-8')
(ROOT/'RELEASE_NOTES_V10.md').write_text('''# NetMaster V10.0 — Intelligent Engineering\n\n- Added Evidence Correlation workspace to the web PWA.\n- Added First-Divergence forensic reasoning flow.\n- Added adaptive mastery profile and evidence-weighted progression model.\n- Added knowledge graph asset connecting lessons, levels and tags.\n- Added packet journey visualizer data for TCP, DHCP and OSPF.\n- Added incident playbooks for TCP, DHCP, DNS, OSPF and VLAN failures.\n- Added deterministic offline hypothesis engine and test selection.\n- Added Config Diff + risk hints to the web workspace.\n- Added PCAP/text packet ingestion workflow in the web layer.\n- Preserved V9.1 content and Android engines.\n''',encoding='utf-8')
# versioned manifests
for fn in ['CONTENT_MANIFEST.json','AI_CONTENT_MANIFEST.json']:
    p=ROOT/fn
    if p.exists():
        try:
            x=json.loads(p.read_text(encoding='utf-8')); x['productVersion']='10.0.0'; x['intelligenceLayer']='Evidence Correlation + Adaptive Mastery + Knowledge Graph'; p.write_text(json.dumps(x,ensure_ascii=False,indent=2),encoding='utf-8')
        except Exception: pass
print('assets generated',len(lessons),len(nodes),len(edges))
