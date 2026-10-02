import json, re, zipfile, hashlib, os
from pathlib import Path
root=Path('/mnt/data/nm9_work')
p=root/'app/src/main/assets/curriculum.json'
D=json.loads(p.read_text(encoding='utf8'))

def norm(s): return str(s or '').lower().replace('ي','ی').replace('ك','ک')
def first(s,n=1800): return str(s or '')[:n]
for level in D['levels']:
  for x in level['lessons']:
    title=x['title'].split(' — ')[0]
    original=[x.get('simple'),x.get('example'),x.get('diagram'),x.get('deepDive'),x.get('commonMistakes'),x.get('questions'),x.get('keyPoints')]
    ref='\n'.join(first(v,1200) for v in original if v)
    # Topic-specific expert anchors derived from exact lesson title.
    kw=norm(title)
    anchors=[]
    rules=[
      (['cpu'], 'برای CPU روی instruction cycle، privilege boundary، cache locality، interrupts، scheduler interaction و performance counters تمرکز کن.'),
      (['motherboard'], 'برای Motherboard روی chipset/bus topology، firmware handoff، PCIe، memory channels، power sequencing و device enumeration تمرکز کن.'),
      (['ram','memory'], 'برای Memory روی virtual/physical address، page tables، working set، page fault، cache/TLB و NUMA تمرکز کن.'),
      (['filesystem'], 'برای Filesystem روی inode/metadata، allocation، journaling، permissions، cache و crash consistency تمرکز کن.'),
      (['process','service'], 'برای Process/Service روی lifecycle، PID/PPID، scheduling، IPC، socket ownership، dependency و restart semantics تمرکز کن.'),
      (['osi','tcp/ip'], 'برای مدل‌ها روی mapping واقعی protocol-to-layer، encapsulation boundary و تفاوت conceptual model با implementation تمرکز کن.'),
      (['mtu','mss'], 'برای MTU/MSS روی fragmentation، PMTUD، DF، TCP segmentation و black-hole MTU تمرکز کن.'),
      (['mac','cam','ethernet'], 'برای Ethernet روی source/destination MAC، EtherType/length، FCS، CAM learning، aging و flooding تمرکز کن.'),
      (['subnet','cidr','ipv4','ipv6'], 'برای Addressing روی prefix math، longest-prefix logic، route summarization، address scopes و planning trade-offs تمرکز کن.'),
      (['arp'], 'برای ARP روی broadcast request، unicast reply، cache aging، gratuitous ARP، proxy behavior و security implications تمرکز کن.'),
      (['icmp','ping','traceroute'], 'برای ICMP روی Echo، TTL exceeded، unreachable codes، probe construction و interpretation of intermediate hops تمرکز کن.'),
      (['vlan','trunk','802.1q','voice vlan'], 'برای VLAN روی tag insertion/removal، ingress/egress rules، native VLAN، allowed list و broadcast-domain boundary تمرکز کن.'),
      (['stp','rstp','bpdu','root'], 'برای STP روی BPDU comparison، root election، port role/state، topology change و convergence timing تمرکز کن.'),
      (['route','routing','ospf','bgp','fhrp','hsrp','vrrp','glbp'], 'برای Routing روی control-plane learning، RIB selection، FIB programming، next-hop recursion، convergence و policy تمرکز کن.'),
      (['tcp'], 'برای TCP روی 4-tuple، sequence space، cumulative ACK، window، retransmission timer، congestion control و FIN/RST semantics تمرکز کن.'),
      (['udp'], 'برای UDP روی datagram boundaries، 4-tuple، checksum، application-level reliability و timeout behavior تمرکز کن.'),
      (['dhcp'], 'برای DHCP روی transaction ID، DORA state، relay giaddr، option handling، lease timers و renewal/rebinding تمرکز کن.'),
      (['dns'], 'برای DNS روی recursion, cache, delegation, authoritative answer, TTL, negative caching و DNSSEC-aware boundaries تمرکز کن.'),
      (['nat','pat','masquerade'], 'برای NAT روی original/reply tuple، conntrack state، pre/post routing hooks، port allocation و asymmetric-flow failure تمرکز کن.'),
      (['firewall','acl','ids','ips','nac','802.1x'], 'برای Security Policy روی rule order، state table، identity/context، logging، default-deny و false positive/negative trade-offs تمرکز کن.'),
      (['wireless','wifi','802.11','wpa','roaming'], 'برای Wireless روی association/authentication، RF channel plan، RSSI/SNR، roaming، airtime و interference تمرکز کن.'),
      (['sip','voip'], 'برای VoIP روی registration/dialog/transaction، SDP offer-answer، RTP media path، NAT traversal و QoS budget تمرکز کن.'),
      (['rtp','qos','dscp'], 'برای Real-time Traffic روی sequence/timestamp، jitter buffer، loss concealment، DSCP trust boundary و queue behavior تمرکز کن.'),
      (['windows','active directory','ad ','gpo','fsmо'], 'برای Windows/AD روی DNS/time/Kerberos dependencies، LDAP objects، replication، GPO scope و Event correlation تمرکز کن.'),
      (['linux','systemd','ssh','cron'], 'برای Linux روی process/file/socket state، systemd dependency graph، permissions، journald و iproute2 evidence تمرکز کن.'),
      (['python','powershell','automation','api','netmiko','napalm'], 'برای Automation روی idempotency، structured output، retries، secrets، transaction boundaries، diff و rollback تمرکز کن.'),
      (['snmp','syslog','netflow','monitoring','zabbix','observability'], 'برای Observability روی metric semantics، counter rollover، polling interval، event correlation، cardinality و alert state تمرکز کن.'),
      (['docker','container','kubernetes'], 'برای Containers روی namespace/cgroup، image layer، network namespace، bridge/NAT، volume semantics و orchestration state تمرکز کن.'),
      (['vmware','hyper-v','kvm','proxmox','virtual'], 'برای Virtualization روی hypervisor boundary، vCPU scheduling، memory overcommit، vNIC/vSwitch، datastore و control-plane dependencies تمرکز کن.'),
      (['storage','raid','iscsi','nfs','san','nas'], 'برای Storage روی block vs file semantics، queue depth، latency, IOPS، RAID failure/rebuild و durability semantics تمرکز کن.'),
      (['backup','dr','rpo','rto'], 'برای Backup/DR روی consistency، recovery point/time، restore validation، immutability، dependency order و recovery testing تمرکز کن.'),
      (['cryptography','aes','rsa','ecc','hash','tls','https','pki'], 'برای Cryptography روی primitive، key lifecycle، certificate chain، nonce/IV، authentication، forward secrecy و handshake state تمرکز کن.'),
      (['soc','siem','mitre','forensics','incident'], 'برای Security Operations روی evidence preservation، timeline، detection logic، triage، containment و chain of custody تمرکز کن.'),
      (['llm','transformer','token','attention','generative'], 'برای LLM روی tokenization، embeddings، attention، positional information، logits، decoding و context limitations تمرکز کن.'),
      (['rag','retrieval','embedding','vector'], 'برای RAG روی ingestion، chunk boundary، metadata، embedding space، hybrid retrieval، reranking، context budget و citation grounding تمرکز کن.'),
      (['agent','tool calling','autonomous'], 'برای Agents روی state machine، tool schema، planning/execution loop، idempotency، approval gate، retries و audit trail تمرکز کن.'),
      (['machine learning','regression','classification','clustering','random forest','svm'], 'برای ML روی feature representation، objective function، train/validation split، leakage، calibration و error analysis تمرکز کن.'),
      (['deep learning','cnn','lstm','transformer','backprop'], 'برای Deep Learning روی tensor shapes، forward/backward pass، gradient flow، optimizer dynamics، regularization و evaluation تمرکز کن.'),
      (['ai','aio','mlops'], 'برای AI Engineering روی data lineage، evaluation set، reproducibility، deployment boundary، monitoring، drift و rollback تمرکز کن.'),
    ]
    for keys,text in rules:
      if any(k in kw for k in keys): anchors.append(text); break
    if not anchors: anchors.append(f'برای «{title}» روی مکانیزم واقعی، dependencyها، state transitions، observable evidence و failure modes همان مؤلفه تمرکز کن.')
    anchor=anchors[0]
    x['expert_reference']=ref
    x['internals']=x['internals']+'\n\nموضوع‌محوری: '+anchor+'\n\nمنبع اولیه درس: '+first(ref,2200)
    x['packet_state_walkthrough']=x['packet_state_walkthrough']+'\n\nTrace اختصاصی: '+anchor+' برای هر مرحله یک timestamp، ورودی، تصمیم، خروجی و Evidence ثبت کن.'
    x['failure_analysis']=x['failure_analysis']+'\n\nFailure-specific drill: یک healthy trace و یک failed trace از همین موضوع کنار هم بگذار و اولین divergence را پیدا کن. سپس با یک negative test ثابت کن که divergence علت بوده است.'
    x['lab']=x['lab']+'\n\nDeliverable: topology/inputs + baseline + fault injection + raw evidence + hypothesis table + root-cause proof + fix + verification + rollback report.'
    x['expert_questions']=x['expert_questions']+[f'در «{title}» اولین divergence بین healthy و failed trace معمولاً در کدام boundary باید جستجو شود و چرا؟',f'کدام counter/log/field در «{title}» بیشترین قدرت تفکیک بین دو علت رقیب را دارد؟']
    x['mastery_path']['level7']='Forensics: یک trace سالم و خراب را مقایسه و اولین divergence را با Evidence اثبات کن.'
D['metadata']['version']='9.1.0-expert-forensics'
for rel in ['app/src/main/assets/curriculum.json','web/assets/curriculum.json']:
 (root/rel).write_text(json.dumps(D,ensure_ascii=False,indent=2),encoding='utf8')
# Add alias/search data for web
alias={
 'ospf':['open shortest path first','link state','lsa','spf'], 'bgp':['border gateway protocol','as path','best path'],
 'dhcp':['dora','lease','relay','bootp'], 'dns':['domain name system','resolver','authoritative','recursive'],
 'mikrotik':['routeros','ros'], 'cisco':['ios','ios-xe'], 'tcp':['transmission control protocol','syn','ack'],
 'udp':['user datagram protocol'], 'vlan':['802.1q','tagging'], 'stp':['spanning tree','rstp','bpdu'],
 'rag':['retrieval augmented generation','vector search','embedding'], 'llm':['large language model','transformer'],
 'ai':['artificial intelligence','machine learning','ml'], 'pcap':['packet capture','wireshark'],
 'troubleshooting':['break fix','incident','root cause','rca']
}
(root/'web/assets/search_aliases.json').write_text(json.dumps(alias,ensure_ascii=False,indent=2),encoding='utf8')
# patch web app with aliases + hashed semantic score
app=root/'web/app.js'; s=app.read_text(encoding='utf8')
s=s.replace("async function init(){const [c,d,s,l,ai]=await Promise.all([j('curriculum.json'),j('deep/expert_engineering.json'),j('deep/scenarios.json'),j('master/lab_catalog.json'),j('master/ai_playbooks.json')]);", "let ALIAS={};\nfunction hv(t){let v=new Map();for(const w of norm(t).split(/[^\\p{L}\\p{N}._:/-]+/u).filter(x=>x.length>1)){let h=0;for(let i=0;i<w.length;i++)h=((h<<5)-h+w.charCodeAt(i))|0;const k=Math.abs(h)%128;v.set(k,(v.get(k)||0)+1)}return v}\nfunction cos(a,b){let dot=0,aa=0,bb=0;for(const v of a.values())aa+=v*v;for(const v of b.values())bb+=v*v;for(const [k,v] of a)dot+=v*(b.get(k)||0);return aa&&bb?dot/Math.sqrt(aa*bb):0}\nfunction expand(terms){const out=[...terms];for(const t of terms)for(const [k,vs] of Object.entries(ALIAS))if(k===t||vs.includes(t))out.push(k,...vs);return [...new Set(out)]}\nasync function init(){const [c,d,s,l,ai,aliases]=await Promise.all([j('curriculum.json'),j('deep/expert_engineering.json'),j('deep/scenarios.json'),j('master/lab_catalog.json'),j('master/ai_playbooks.json'),j('search_aliases.json')]);ALIAS=aliases;")
s=s.replace("function score(e,p){let s=0;const title=norm(e.title);const hay=norm(e.text);for(const t of p.terms){", "function score(e,p){let s=0;const title=norm(e.title);const hay=norm(e.text);const terms=expand(p.terms);const qv=hv(terms.join(' '));const ev=hv(e.text);s+=cos(qv,ev)*24;for(const t of terms){")
app.write_text(s,encoding='utf8')
# Repackage v9.1
out=Path('/mnt/data/NetMasterLearning-v9.1-Expert-Forensics-AI-MasoudJokar.zip')
if out.exists(): out.unlink()
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for q in root.rglob('*'):
  if q.is_file(): z.write(q,q.relative_to(root))
print('lessons',sum(len(v['lessons']) for v in D['levels']))
ls=[x for v in D['levels'] for x in v['lessons']]
print('unique internals',len({x['internals'] for x in ls}),'unique refs',len({x['expert_reference'] for x in ls}))
print('size',out.stat().st_size,'sha',hashlib.sha256(out.read_bytes()).hexdigest())
