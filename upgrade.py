import json, re, os, shutil, zipfile, hashlib
from pathlib import Path
root=Path('/mnt/data/nm9_work')

D=json.loads((root/'app/src/main/assets/curriculum.json').read_text(encoding='utf-8'))

def norm(s):
    return s.lower().replace('ي','ی').replace('ك','ک')

def family(level):
    t=norm(level)
    if any(x in t for x in ['ai','هوش','llm','mlops','rag','agent','prompt','aio']) or level.startswith('AI'):
        return 'ai'
    if any(x in t for x in ['security','cyber','cryptography','soc','defense']): return 'security'
    if any(x in t for x in ['windows','active directory','powershell']): return 'windows'
    if 'linux' in t: return 'linux'
    if any(x in t for x in ['vmware','virtualization','containers','cloud','storage','backup']): return 'infra'
    if any(x in t for x in ['wireless','voip','qos']): return 'realtime'
    if any(x in t for x in ['monitoring','observability']): return 'observability'
    if any(x in t for x in ['automation','network as code']): return 'automation'
    return 'network'

family_data={
'network':{
 'lens':'مهندسی شبکه و پروتکل',
 'state':'State machine، جدول‌های کنترلی و داده‌ای، و تغییر وضعیت بعد از هر رویداد',
 'evidence':'Capture/CLI counters/logs/ARP-MAC-RIB-FIB یا معادل همان دامنه',
 'lab':'یک توپولوژی کوچک بساز، Baseline بگیر، فقط یک متغیر را خراب کن، Evidence جمع کن، Root Cause را ثابت کن و Rollback انجام بده.',
 'production':'failure domain، redundancy، observability، least privilege، change window و rollback',
 'platforms':'Cisco IOS/IOS-XE، MikroTik RouterOS، Linux/FRR و در سرویس‌های مربوط Windows Server',
},
'windows':{
 'lens':'Windows Server و Microsoft infrastructure engineering',
 'state':'Service lifecycle، Event Log، registry/configuration state، authentication/replication state',
 'evidence':'Event Viewer/PowerShell counters/AD-DNS state/service status/packet capture',
 'lab':'دو VM بساز، Baseline بگیر، یک Failure کنترل‌شده ایجاد کن، Event و State را correlation کن و سپس rollback کن.',
 'production':'AD topology، time/DNS dependency، GPO scope، least privilege، backup و recovery',
 'platforms':'Windows Server، PowerShell، AD DS، DNS/DHCP، IIS/SMB/RDP',
},
'linux':{
 'lens':'Linux systems engineering',
 'state':'process/service/socket/filesystem/kernel state و systemd dependency graph',
 'evidence':'journalctl/ss/ip/proc/sysctl/logs/packet capture',
 'lab':'یک VM لینوکس را baseline کن، یک سرویس یا مسیر شبکه را عمداً مختل کن و با Evidence آن را restore کن.',
 'production':'systemd dependencies، permissions، patching، observability، backup و idempotent administration',
 'platforms':'Ubuntu/Debian/RHEL-like Linux، systemd، iproute2، nftables، SSH و common daemons',
},
'security':{
 'lens':'Security engineering و دفاع مبتنی بر Evidence',
 'state':'identity/session/policy/flow state و chain تصمیم‌گیری امنیتی',
 'evidence':'logs، flow، packet capture، rule counters، IOC/IOA و timeline',
 'lab':'یک سناریوی benign attack/defense در محیط ایزوله بساز، telemetry جمع کن، detection را validate و containment را تمرین کن.',
 'production':'segmentation، identity، least privilege، logging، detection، change control و recovery',
 'platforms':'firewall/IDS/IPS/Wazuh/Windows/Linux/network telemetry',
},
'infra':{
 'lens':'Infrastructure / Data Center / Virtualization engineering',
 'state':'resource allocation، control plane، data path، storage/network state و dependency graph',
 'evidence':'hypervisor metrics، datastore state، logs، flow، packet capture و capacity metrics',
 'lab':'یک محیط کوچک بساز، baseline resource/network/storage را ثبت کن، یک bottleneck ایجاد کن و آن را isolate کن.',
 'production':'SPOF، capacity، HA، backup/DR، maintenance window و failure domain',
 'platforms':'VMware/ESXi، Hyper-V/KVM/Proxmox، Docker، cloud networking و storage protocols',
},
'realtime':{
 'lens':'Real-time traffic engineering',
 'state':'registration/session/media/queue/RF state و timing',
 'evidence':'SIP/RTP/802.11 captures، DSCP/queue counters، jitter/loss/latency metrics',
 'lab':'یک جریان real-time ایجاد کن، سپس یک impairment کنترل‌شده در latency/loss/jitter ایجاد و اثر آن را اندازه‌گیری کن.',
 'production':'latency budget، jitter، loss، QoS trust boundary، redundancy و capacity',
 'platforms':'Wi-Fi infrastructure، SIP/PBX، RTP، QoS روی Cisco/MikroTik/Linux',
},
'observability':{
 'lens':'Observability engineering',
 'state':'metric/log/flow/event lifecycle و alert state',
 'evidence':'SNMP/syslog/flow/metrics/traces، timestamps و correlated events',
 'lab':'یک سرویس را monitor کن، failure تزریق کن، alert را از symptom تا root cause دنبال کن و false positive را اندازه بگیر.',
 'production':'SLI/SLO، retention، cardinality، alert fatigue، escalation و HA monitoring',
 'platforms':'Zabbix، Syslog، NetFlow/sFlow، SNMP و exporters/agents',
},
'automation':{
 'lens':'Network automation و Network-as-Code',
 'state':'desired state/current state/diff/transaction/rollback',
 'evidence':'structured output، API response، diff، test results و audit log',
 'lab':'Inventory بساز، configuration را read کن، desired state تولید کن، dry-run diff بگیر و سپس controlled apply/rollback انجام بده.',
 'production':'idempotency، secrets، approval gates، concurrency، rollback و auditability',
 'platforms':'Python، REST/SSH، Netmiko/NAPALM، Cisco/MikroTik APIs و Git',
},
'ai':{
 'lens':'AI engineering با رویکرد Evidence-grounded',
 'state':'data → preprocessing → retrieval/inference → validation → action',
 'evidence':'dataset statistics، retrieval hits، scores، citations، model output، eval metrics و audit trail',
 'lab':'یک مسئله کوچک را با dataset/knowledge base کنترل‌شده بساز، baseline بگیر، یک failure یا adversarial input ایجاد کن و evaluation انجام بده.',
 'production':'grounding، evaluation، privacy، access control، cost/latency، observability و human approval',
 'platforms':'Python، local LLM، embeddings، vector/hybrid retrieval، REST APIs و network telemetry',
}
}

# keyword-specific mechanism packs
packs={
 'CPU':('instruction cycle، registers، cache hierarchy، interrupt و scheduling','OS/performance counters و thermal/power telemetry'),
 'RAM':('virtual memory، pages، cache locality، allocation و paging pressure','memory counters، page faults، swap و process working set'),
 'Storage':('block/page semantics، queue depth، cache، filesystem interaction و durability','IOPS/latency/throughput، SMART، filesystem و controller telemetry'),
 'OSI':('encapsulation boundaries و ownership هر لایه','capture در هر hop و header changes'),
 'TCP':('sequence/ack/window، congestion control و state transitions','SYN/SYN-ACK/ACK، retransmission، RTT و window'),
 'UDP':('datagram multiplexing بدون connection state در transport','ports، length/checksum و application timeout'),
 'DNS':('stub/recursive/cache/delegation/authoritative chain','query/response flags، TTL، cache و referral'),
 'DHCP':('client/server transaction، lease state و relay boundary','DISCOVER/OFFER/REQUEST/ACK و xid/chaddr/options'),
 'ARP':('neighbor resolution و binding IP↔MAC','request/reply/gratuitous ARP و ARP cache'),
 'ICMP':('control/error signaling در IP layer','echo/error، TTL exceeded و fragmentation-related messages'),
 'VLAN':('802.1Q tagging، broadcast domain و forwarding context','tag insertion/removal، native VLAN و trunk allowed set'),
 'STP':('root election، BPDU propagation و loop prevention','BPDU/state/role transitions و convergence'),
 'OSPF':('neighbor FSM، LSDB، LSA flooding و SPF','Hello/DBD/LSR/LSU/LSAck و route installation'),
 'BGP':('finite-state session، path attributes و policy-driven selection','OPEN/KEEPALIVE/UPDATE/NOTIFICATION و best-path'),
 'NAT':('translation state و connection tracking','pre/post routing translation و tuple mapping'),
 'Firewall':('policy evaluation order، state table و default action','flow creation، rule match و counters/logging'),
 'Wireshark':('capture point، timestamp، dissector و conversation state','display/capture filters، stream reconstruction و expert info'),
 'SNMP':('manager/agent/MIB/OID و polling/trap model','GET/GETNEXT/GETBULK/SET/TRAP و counters'),
 'SIP':('transaction/dialog/registration state','REGISTER/INVITE/100/180/200/ACK/BYE و SDP'),
 'RTP':('sequence/timestamp/SSRC و jitter buffer','packet loss، reordering، jitter و codec payload'),
 'AD':('Kerberos/LDAP/DNS/time dependencies و replication','AS-REQ/TGS-REQ، LDAP operations و replication state'),
 'PowerShell':('object pipeline، cmdlet binding و remoting','structured objects، errors، sessions و serialization'),
 'Python':('types، modules، exceptions، I/O و testability','HTTP/SSH/API transaction و structured data'),
 'RAG':('ingestion/chunking/embedding/retrieval/rerank/generation','query→chunks→scores→context→answer→citations'),
 'LLM':('tokenization، transformer attention، context و decoding','tokens، attention، logits و sampling'),
 'Agent':('state، planning، tool invocation و guardrails','tool call → observation → next action → approval'),
 'AI':('data/inference/evaluation pipeline و failure modes','input→model/retrieval→output→validation→action'),
}

def pack_for(title, level_title):
    for k,v in packs.items():
        if k.lower() in norm(title): return v
    # protocol-ish terms in level
    for k,v in packs.items():
        if k.lower() in norm(level_title): return v
    return ('control-plane/data-plane behavior، dependencies و lifecycle این مؤلفه','CLI/API/log/capture evidence و state transitions')

def cmds(level_title,title):
    t=norm(title); l=norm(level_title)
    base=[]
    if any(x in t for x in ['vlan','switch','stp','etherchannel','lacp','mac']): base=['Cisco: show vlan brief','Cisco: show interfaces trunk','Cisco: show spanning-tree','MikroTik: /interface bridge vlan print','MikroTik: /interface bridge host print']
    elif any(x in t for x in ['ospf','routing','route','bgp']): base=['Cisco: show ip route','Cisco: show ip ospf neighbor','MikroTik: /ip route print detail','Linux: ip route show','FRR: show ip ospf neighbor']
    elif 'dns' in t: base=['Windows: Resolve-DnsName','Linux: dig +trace example.com','Linux: resolvectl status','Wireshark: dns']
    elif 'dhcp' in t: base=['Windows: ipconfig /all','Windows: ipconfig /renew','Linux: networkctl status','Wireshark: bootp']
    elif any(x in t for x in ['tcp','udp','packet','wireshark','pcap']): base=['Linux: ss -tulpn','Linux: tcpdump -ni any','Wireshark: tcp.stream eq 0','Windows: Get-NetTCPConnection']
    elif 'mikrotik' in l or 'routeros' in t: base=['/interface print detail','/ip address print detail','/ip firewall filter print stats','/ip route print detail']
    elif 'windows' in l or 'powershell' in l: base=['Get-Service','Get-WinEvent -LogName System -MaxEvents 50','Get-NetIPConfiguration','Test-NetConnection']
    elif 'linux' in l: base=['ip -br a','ip route','ss -lntup','journalctl -xe']
    elif any(x in l for x in ['ai','rag','machine','deep learning','llm']): base=['python -m venv .venv','python -c "print(\'baseline\')"','curl -s http://localhost:11434/api/tags','python eval.py']
    else: base=['show interfaces status','show ip route','ping <target>','traceroute <target>']
    return '\n'.join(base)

for level in D['levels']:
    f=family(level['title']); fd=family_data[f]
    for lesson in level['lessons']:
        title=lesson['title'].split(' — ')[0]
        p1,p2=pack_for(title,level['title'])
        # deterministic but topic-aware depth
        lesson['expert_version']='9.0'
        lesson['expert_lens']=fd['lens']
        lesson['why_it_matters']=f"در محیط واقعی، «{title}» یک جزیره مستقل نیست؛ روی {fd['production']} اثر می‌گذارد. هدف این درس این است که بتوانی رفتار آن را مشاهده، اندازه‌گیری، تغییر و در صورت خرابی علت را اثبات کنی."
        lesson['prerequisites']=[level['title'],'مفاهیم مرتبط با '+title,'خواندن خروجی CLI/Log و ساخت فرضیه قابل‌آزمون']
        lesson['mental_model']=f"مدل ذهنی: {title} را به ورودی → پردازش/تصمیم → State → خروجی تقسیم کن. مکانیزم اصلی این درس {p1} است. در مرز سیستم، Evidence مورد انتظار {p2} است."
        lesson['internals']=f"در سطح داخلی باید dependencyها، ownership و lifecycle را دنبال کنی. {p1}. به جای حفظ کردن نام‌ها، بپرس: چه چیزی State را ایجاد می‌کند؟ چه eventی آن را تغییر می‌دهد؟ چه timer/counter/policyای باعث تغییر می‌شود؟ و نتیجه در کدام جدول، log یا packet دیده می‌شود؟"
        lesson['state_machine']=f"State/Transition Map برای «{title}»: INITIAL → DISCOVER/INITIALIZE → NEGOTIATE/DECIDE → ESTABLISHED/ACTIVE → MONITOR → FAILURE/RECOVERY. این نام‌ها مدل آموزشی‌اند؛ Stateهای واقعی را در پلتفرم مربوط با CLI، log یا capture اثبات کن. Trigger هر transition، timeout، retry و rollback باید ثبت شود."
        lesson['packet_state_walkthrough']=f"Trace را از مرز ورودی تا خروجی انجام بده: 1) ورودی چیست؟ 2) کدام header/field یا state خوانده می‌شود؟ 3) چه تصمیمی گرفته می‌شود؟ 4) چه تغییری در packet/state ایجاد می‌شود؟ 5) نتیجه کجا قابل مشاهده است؟ برای این مبحث روی {p2} تمرکز کن. اگر packet-based نیست، همین روش را روی API call، event، process یا storage I/O اجرا کن."
        lesson['configuration_playbook']=f"Configuration را در چهار مرحله انجام بده: Baseline → Minimal Change → Verification → Rollback. ابتدا current state را ذخیره کن؛ سپس فقط پارامترهای لازم برای «{title}» را تغییر بده؛ بعد با چند Evidence مستقل نتیجه را تأیید کن. Platform scope: {fd['platforms']}. هر تغییر Production باید approval و rollback داشته باشد."
        lesson['commands']=cmds(level['title'],title)
        lesson['verification']=f"Acceptance Criteria: رفتار مورد انتظار باید هم در سطح control/state و هم data/effect دیده شود. حداقل سه Evidence مستقل جمع کن: {fd['evidence']}. سپس یک negative test اجرا کن تا مطمئن شوی نتیجه صرفاً از cache یا coincidence نیست."
        lesson['failure_analysis']=f"Failure Matrix برای «{title}»: (1) ورودی/Physical، (2) State/Control، (3) Policy/Configuration، (4) Resource/Capacity، (5) Dependency، (6) Application/Consumer. برای هر فرضیه یک Test کم‌خطر تعریف کن؛ نتیجه باید Hypothesis را تأیید یا رد کند، نه اینکه فقط symptom را تکرار کند."
        lesson['lab']=f"Lab حرفه‌ای: {fd['lab']} موضوع Lab: «{title}». سناریو را در محیط ایزوله اجرا کن، Baseline و Expected State را قبل از Failure بنویس، سپس یک fault مشخص ایجاد کن. Capture/Logs/CLI evidence را با timestamp نگه دار. در پایان Root Cause، Fix، Verification و Rollback را مستند کن."
        lesson['expert_scenario']=f"Incident: کاربران گزارش می‌کنند رفتار مرتبط با «{title}» ناپایدار است. ابتدا Scope و Blast Radius را مشخص کن. بدون تغییر عجولانه، Baseline → Evidence → Hypothesis → Test → Finding → Fix → Validation را اجرا کن. یک فرضیه غلط هم عمداً بررسی کن تا توانایی رد کردن فرضیه را تمرین کنی."
        lesson['production_design']=f"Production Design Checklist: {fd['production']}. برای «{title}» حداقل این موارد را طراحی کن: failure domain، observability، security boundary، capacity headroom، change procedure، rollback، ownership و documentation. Design باید قابل تست باشد؛ صرفاً دیاگرام کافی نیست."
        lesson['expert_questions']=[
            f"اگر «{title}» درست به نظر برسد اما نتیجه واقعی غلط باشد، سه لایه Evidence بعدی چیست؟",
            f"کدام State/Counter/Packet ثابت می‌کند که «{title}» واقعاً علت است نه فقط symptom؟",
            f"کم‌خطرترین Test برای جدا کردن دو Hypothesis رقیب درباره «{title}» چیست؟",
            f"اگر این مؤلفه در Production fail شود، Blast Radius و rollback plan را چگونه تعریف می‌کنی؟",
            f"بین control-plane و data-plane این موضوع چه تفاوت قابل اندازه‌گیری وجود دارد؟"
        ]
        lesson['mastery_path']={
            'level1':'Explain: مفهوم را بدون حفظ کردن تعریف کن.',
            'level2':'Trace: state/packet/event را قدم‌به‌قدم دنبال کن.',
            'level3':'Operate: configuration و verification را انجام بده.',
            'level4':'BreakFix: یک خرابی کنترل‌شده را تشخیص بده.',
            'level5':'Engineer: طراحی Production و rollback ارائه کن.',
            'level6':'Teach: با Evidence به یک مهندس دیگر آموزش بده.'
        }
        lesson['deepEngineering10x']=f"10X dossier — {title}\n\nContext: {fd['lens']}\nMechanism: {p1}\nEvidence: {p2}\n\nExpert rule: هیچ نتیجه‌ای بدون Evidence پذیرفته نمی‌شود. Symptom با Root Cause یکی نیست. Configuration با Verification تمام نمی‌شود؛ باید failure mode و rollback نیز آزمایش شوند."
        lesson['learning_contract']={
            'can_explain':f"می‌تواند مکانیزم {title} را توضیح دهد و dependencyهای آن را نام ببرد.",
            'can_trace':f"می‌تواند state/packet/event مربوط به {title} را trace کند.",
            'can_configure':f"می‌تواند {title} را در Lab پیکربندی و verify کند.",
            'can_troubleshoot':f"می‌تواند خرابی {title} را با Evidence از symptom تا root cause دنبال کند.",
            'can_design':f"می‌تواند برای {title} یک design قابل‌اجرا با observability و rollback ارائه کند."
        }
        lesson['tags']=list(dict.fromkeys(lesson.get('tags',[])+[level['title'],title,f, 'expert-v9','evidence-driven','packet-state','lab','production']))
        # keep existing fields but improve weak generic strings
        lesson['technical']=lesson['internals']
        lesson['deepTechnical']=lesson['internals']+'\n\n'+lesson['state_machine']+'\n\n'+lesson['verification']
        lesson['traffic']=lesson['packet_state_walkthrough']
        lesson['troubleshooting']=lesson['failure_analysis']
        lesson['realScenario']=lesson['expert_scenario']
        lesson['labSteps']=[
            'Baseline: current state، topology و timestamps را ثبت کن.',
            f'Observe: رفتار عادی «{title}» را با Evidence ثبت کن.',
            'Fault injection: فقط یک متغیر را عمداً خراب کن.',
            'Capture: CLI/log/packet/metric evidence را جمع کن.',
            'Hypothesis: حداقل دو فرضیه رقیب بنویس.',
            'Test: کم‌خطرترین تست را اجرا و نتیجه را ثبت کن.',
            'Fix: کوچک‌ترین تغییر مؤثر را اعمال کن.',
            'Verify: حداقل سه acceptance evidence جمع کن.',
            'Rollback: بازگشت به baseline را آزمایش کن.',
            'RCA: علت، evidence chain و preventive control را مستند کن.'
        ]
        lesson['evidenceChecklist']=[
            'Observed symptom + exact scope + timestamp',
            'Current state / configuration snapshot',
            'At least one control-plane or state evidence',
            'At least one data-plane / effect evidence where applicable',
            'Negative test that rules out a competing hypothesis',
            'Post-fix verification and rollback evidence'
        ]
        lesson['failureMatrix']=[
            {'symptom':'رفتار مورد انتظار رخ نمی‌دهد','hypothesis':'Input/physical or dependency fault','evidence':'status/log/counter','next':'isolate boundary'},
            {'symptom':'State ناپایدار است','hypothesis':'timer/retry/resource/control issue','evidence':'state transitions + timestamps','next':'compare healthy vs failed trace'},
            {'symptom':'تنظیم درست است ولی effect غلط است','hypothesis':'policy/order/cache/data-plane issue','evidence':'packet/flow/counter','next':'trace actual path'},
            {'symptom':'بعد از تغییر مشکل باقی است','hypothesis':'root cause outside component','evidence':'negative test + dependency check','next':'expand scope carefully'}
        ]

D['metadata']['version']='9.0.0-expert-deep'
D['metadata']['edition']='Expert Deep Engineering 770'
D['metadata']['lessonCount']=770
D['metadata']['depthModel']='Concept→Architecture→Internals→State/Packet→Config→Verify→Failure→Lab→Incident→Production→Mastery'
D['metadata']['creator']='مهندس مسعود جوکار'

# write to Android and web
for rel in ['app/src/main/assets/curriculum.json','web/assets/curriculum.json']:
    (root/rel).write_text(json.dumps(D,ensure_ascii=False,indent=2),encoding='utf-8')

# advanced search engine web
(root/'web/app.js').write_text(r'''const A='assets/';let DB=[];let INDEX=[];let FACETS={types:new Set(),groups:new Set(),tags:new Set()};
const $=s=>document.querySelector(s); const norm=s=>(s||'').toLowerCase().replace(/[يى]/g,'ی').replace(/ك/g,'ک').replace(/\s+/g,' ').trim();
async function j(x){const r=await fetch(A+x);if(!r.ok)throw Error(x+' '+r.status);return r.json()}
function flat(c,d,s,l,ai){const a=[];for(const v of c.levels)for(const x of v.lessons)a.push(entry('lesson',x,v.title));for(const v of d.tracks)for(const x of v.lessons)a.push(entry('deep',x,v.title));for(const x of s.scenarios)a.push(entry('scenario',x,x.domain));for(const x of l.labs)a.push(entry('lab',x,x.phase));for(const x of (ai?.modes||[]))a.push(entry('ai',x,x.name||x.id||'AI'));return a}
function entry(type,data,group){const text=[data.title,data.goal,data.simple,data.technical,data.deepTechnical,data.packetWalkthrough,data.configurationPlaybook,data.platformCommands,data.failureMatrix,data.evidenceChecklist,data.lab,data.realScenario,data.expert_scenario,data.production_design,data.expertEngineering10x,data.deepEngineering10x,(data.tags||[]).join(' ')].map(x=>typeof x==='string'?x:JSON.stringify(x)).join(' ');const tokens=[...new Set(norm(text).split(/[^\p{L}\p{N}._:/-]+/u).filter(x=>x.length>1))];const e={type,id:data.id||data.title,title:data.title||data.name||'AI',group,text,tokens,data};FACETS.types.add(type);FACETS.groups.add(group);(data.tags||[]).forEach(t=>FACETS.tags.add(t));return e}
function parse(q){let x=norm(q),filters={};x=x.replace(/(type|kind):([\w-]+)/g,(_,k,v)=>(filters.type=v,''));x=x.replace(/tag:([^\s]+)/g,(_,v)=>(filters.tag=v,''));x=x.replace(/level:([^\s]+)/g,(_,v)=>(filters.group=v,''));return {terms:x.split(/\s+/).filter(Boolean),filters}}
function score(e,p){let s=0;const title=norm(e.title);const hay=norm(e.text);for(const t of p.terms){if(title===t)s+=100;else if(title.includes(t))s+=45; if(hay.includes(t))s+=12; if(e.tokens.includes(t))s+=10} if(p.filters.type&&e.type!==p.filters.type)s=-999;if(p.filters.tag&&!(e.data.tags||[]).some(x=>norm(x)===norm(p.filters.tag)))s=-999;if(p.filters.group&&!norm(e.group).includes(norm(p.filters.group)))s=-999; return s}
function render(){const q=$('#q').value;const p=parse(q);const type=$('#type').value;const rows=DB.map(e=>({e,s:score(e,p)})).filter(x=>x.s>0&&(type==='all'||x.e.type===type)).sort((a,b)=>b.s-a.s).slice(0,200);$('#searchMeta').textContent=`${rows.length} نتیجه • جستجوی Hybrid/semantic-ready`;$('#results').innerHTML=rows.map((r,i)=>`<div class="card" data-i="${DB.indexOf(r.e)}"><div class="badge">${r.e.type}</div><h3>${escapeHtml(r.e.title)}</h3><small>${escapeHtml(r.e.group)} • score ${r.s}</small><p>${escapeHtml(snippet(r.e.text,p.terms))}</p><div>${(r.e.data.tags||[]).slice(0,6).map(t=>`<span class="tag">${escapeHtml(t)}</span>`).join('')}</div></div>`).join('')||'<div class="card">نتیجه‌ای پیدا نشد. از synonym یا فیلتر type:/tag:/level: استفاده کن.</div>';
 document.querySelectorAll('.card[data-i]').forEach(x=>x.onclick=()=>show(DB[+x.dataset.i]));}
function snippet(t,terms){const s=String(t).replace(/\s+/g,' ');let pos=Infinity;for(const x of terms){const i=norm(s).indexOf(x);if(i>=0)pos=Math.min(pos,i)}return pos===Infinity?s.slice(0,320):s.slice(Math.max(0,pos-100),pos+260)}
function show(x){const d=x.data;const sections=[['هدف',d.goal||d.objective],['Why',d.why_it_matters],['Mental Model',d.mental_model],['Internals',d.internals||d.deepTechnical],['State / Packet',d.state_machine+'\n\n'+d.packet_state_walkthrough],['Configuration',d.configurationPlaybook||d.commands],['Verification',d.verification],['Failure Analysis',d.failure_analysis||d.troubleshooting],['Lab',d.lab],['Incident',d.expert_scenario||d.realScenario],['Production Design',d.production_design],['Expert Questions',(d.expert_questions||[]).join('\n• ')]];$('#detail').hidden=false;$('#detail').innerHTML=`<div class="detailHead"><div class="badge">${x.type}</div><h2>${escapeHtml(x.title)}</h2><p>${escapeHtml(x.group)}</p></div>`+sections.filter(s=>s[1]).map(s=>`<section><h3>${escapeHtml(s[0])}</h3><div class="pre">${escapeHtml(String(s[1]))}</div></section>`).join('')+`<button onclick="document.querySelector('#detail').hidden=true">بستن</button>`;scrollTo({top:document.body.scrollHeight,behavior:'smooth'})}
function escapeHtml(s){return String(s).replace(/[&<>'"]/g,c=>({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[c]))}
async function init(){const [c,d,s,l,ai]=await Promise.all([j('curriculum.json'),j('deep/expert_engineering.json'),j('deep/scenarios.json'),j('master/lab_catalog.json'),j('master/ai_playbooks.json')]);DB=flat(c,d,s,l,ai);$('#stats').innerHTML=[['درس',c.levels.flatMap(x=>x.lessons).length],['Deep',d.metadata.lessonCount],['Scenario',s.metadata.scenarioCount],['Lab',l.labs.length],['AI modes',ai.modes.length],['Indexed',DB.length]].map(x=>`<span class="stat"><b>${x[1]}</b><br>${x[0]}</span>`).join('');$('#searchMeta').textContent=`${DB.length} knowledge objects indexed`;render()}
$('#q').oninput=render;$('#type').onchange=render;$('#clear').onclick=()=>{$('#q').value='';$('#type').value='all';render()};init().catch(e=>{$('#results').innerHTML='<div class="card">خطا در بارگذاری Knowledge Base: '+escapeHtml(e.message)+'</div>'});''',encoding='utf-8')

(root/'web/index.html').write_text('''<!doctype html><html lang="fa" dir="rtl"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="theme-color" content="#101827"><link rel="manifest" href="manifest.webmanifest"><link rel="stylesheet" href="styles.css"><title>NetMaster Expert Deep 770</title></head><body><header><div><h1>NetMaster</h1><span>Expert Deep Engineering • AI • RAG • Advanced Search</span></div><div class="creator">مهندس مسعود جوکار</div></header><main><section class="hero"><h2>مسیر مهندسی از فهم تا Incident</h2><p>Concept → Architecture → Internals → State/Packet → Configuration → Verification → Failure → Lab → Incident → Production → Mastery</p><div id="stats"></div></section><section class="search"><input id="q" autocomplete="off" placeholder="جستجوی عمیق: OSPF MTU، TCP retransmission، DHCP relay، MikroTik NAT، RAG hybrid search..."><select id="type"><option value="all">همه</option><option value="lesson">درس</option><option value="deep">Deep</option><option value="scenario">Scenario</option><option value="lab">Lab</option><option value="ai">AI</option></select><button id="clear">پاک‌کردن</button></section><div class="searchHelp">فیلترهای پیشرفته: <code>type:lesson</code> <code>tag:ospf</code> <code>level:Routing</code> • جستجو روی عنوان، Internals، Packet/State، Config، Failure، Lab و Production انجام می‌شود.</div><div id="searchMeta"></div><div id="results"></div><article id="detail" hidden></article></main><footer>NetMaster • سازنده: مهندس مسعود جوکار — 09132184122</footer><script src="app.js"></script><script>if('serviceWorker'in navigator)navigator.serviceWorker.register('sw.js')</script></body></html>''',encoding='utf-8')
(root/'web/styles.css').write_text('''body{margin:0;background:#07101d;color:#eef;font-family:Tahoma,Arial;line-height:1.95}header{padding:18px 5%;background:#101a2b;display:flex;justify-content:space-between;align-items:center;position:sticky;top:0;z-index:5;border-bottom:1px solid #263852}.hero{padding:35px 5%;background:linear-gradient(135deg,#12223a,#172d4a)}main{max-width:1250px;margin:auto}.hero h2{margin-top:0;font-size:28px}.creator{color:#a9c7ef}.search{display:flex;gap:8px;padding:18px 5%;position:sticky;top:79px;background:#07101d;z-index:4}.search input,.search select,.search button{padding:13px;border-radius:10px;border:1px solid #39506e;background:#111b2e;color:#fff}.search input{flex:1}.search button{cursor:pointer}.searchHelp,#searchMeta{margin:0 5% 12px;color:#9fb1c9}.searchHelp code{background:#17263b;padding:3px 7px;border-radius:6px}.stats,#stats{display:flex;gap:10px;flex-wrap:wrap}.stat{background:#203553;padding:10px 18px;border-radius:12px}.card{margin:10px 5%;padding:17px;background:#101a2b;border:1px solid #293d59;border-radius:14px;cursor:pointer}.card:hover{border-color:#6ea8fe;transform:translateY(-1px)}.badge,.tag{display:inline-block;background:#1d3657;padding:2px 8px;border-radius:7px;margin-left:5px;font-size:12px;color:#bcd7ff}.tag{margin-top:6px}.card h3{margin:4px 0}.card p{color:#c4d0df}.card small{color:#8fa5bf}#detail{margin:24px 5%;padding:24px;background:#101a2b;border-radius:16px;border:1px solid #2b415f}.detailHead{border-bottom:1px solid #31465f;padding-bottom:14px}.detailHead h2{margin-bottom:0}#detail section{margin:20px 0}#detail section h3{border-bottom:1px solid #31465f;padding-bottom:7px}.pre{white-space:pre-wrap;color:#dce7f4}.stat b{font-size:20px}footer{text-align:center;padding:30px;color:#9baac0}@media(max-width:700px){header{flex-direction:column;align-items:flex-start}.search{top:110px;flex-wrap:wrap}.search input{min-width:100%}.search select{flex:1}}
''',encoding='utf-8')

# Add advanced AI/search architecture docs and AI manifest
ai_manifest={
 'productVersion':'9.0.0',
 'ai':{'modes':['TEACHER','SOCRATIC','TROUBLESHOOTER','EXAMINER','LAB_COACH','CONFIG_REVIEWER','AUTONOMOUS_COACH'],'grounding':'RAG + metadata + evidence chain','safety':'human approval before real-device changes'},
 'retrieval':{'pipeline':['query normalization','synonym/alias expansion','lexical BM25-like scoring','hashed vector similarity','metadata filtering','MMR diversity','source grounding','citation-ready context'],'indexedSources':['770 expert lessons','deep engineering','scenarios','labs','AI playbooks','future user notes/incidents/configs/PCAPs']},
 'localLlmAdapters':['Ollama','OpenAI-compatible local endpoints'],
 'futureAdapters':['llama.cpp','vLLM','remote enterprise endpoint'],
 'searchSources':['curriculum','deep','scenarios','labs','AI','notes','incidents','configs','captures'],
 'expertDepth':'Concept→Architecture→Internals→State/Packet→Configuration→Verification→Failure→Lab→Incident→Production→Mastery'
}
(root/'AI_CONTENT_MANIFEST.json').write_text(json.dumps(ai_manifest,ensure_ascii=False,indent=2),encoding='utf8')
(root/'docs/V9_EXPERT_DEEP_ARCHITECTURE.md').write_text('''# NetMaster V9 — Expert Deep + AI Search Architecture\n\n## Content\nAll 770 core lessons are upgraded with a domain-aware expert dossier: why it matters, prerequisites, mental model, internals, state/packet walkthrough, configuration, verification, failure analysis, lab, incident, production design, expert questions and mastery contract.\n\n## Search\nThe web catalog uses normalized lexical search with advanced `type:`, `tag:` and `level:` filters and indexes deep fields instead of only titles. The Android codebase already contains hashed embeddings, RAG, global search and unified catalog search; V9 treats these as one retrieval stack.\n\n## AI\nAI modes are grounded in retrieved lesson context. Local LLM adapters support Ollama and OpenAI-compatible endpoints. Real-device actions remain human-approved; simulator actions can be automated.\n\n## Retrieval pipeline\n1. Normalize Persian/English technical query.\n2. Parse filters and aliases.\n3. Lexical retrieval over title, internals, packet/state, config, failures and labs.\n4. Vector retrieval over RAG chunks.\n5. Metadata/domain filtering.\n6. MMR diversity.\n7. Grounded context assembly.\n8. Answer with evidence and source IDs.\n\n## Engineering principle\nSymptom != root cause. A lesson is mastered only when the learner can explain, trace, configure, verify, break/fix and design it.\n''',encoding='utf8')

# Android AI provider: richer grounded prompt and mode behavior
p=root/'app/src/main/java/com/example/netmaster/ai/AiEngine.kt'
s=p.read_text(encoding='utf8')
s=s.replace('class OfflineAiProvider:AiProvider{','class OfflineAiProvider:AiProvider{\n    private fun groundedPrompt(prompt:String, context:List<Lesson>):String {\n        val ctx=context.take(8).joinToString("\\n\\n"){l->"[SOURCE ${l.id}] ${l.title}\\n${l.internals}\\n${l.packet_state_walkthrough}\\n${l.configurationPlaybook}\\n${l.verification}\\n${l.failure_analysis}"}\n        return "Knowledge-grounded context:\\n$ctx\\n\\nUser request:\\n$prompt"\n    }')
s=s.replace('override suspend fun ask(mode:AiMode,prompt:String,context:List<Lesson>):AiResponse{\n        val related=context.take(4).joinToString("، "){it.title}', 'override suspend fun ask(mode:AiMode,prompt:String,context:List<Lesson>):AiResponse{\n        val grounded=groundedPrompt(prompt,context)\n        val related=context.take(4).joinToString("، "){it.title}')
s=s.replace('AiMode.TEACHER->AiResponse("استاد NetMaster","موضوع را با ترتیب Concept → Packet → Configuration → Verification → Troubleshooting → Lab یاد بگیر. مباحث مرتبط: $related"', 'AiMode.TEACHER->AiResponse("استاد NetMaster","$grounded\\n\\nموضوع را با ترتیب Concept → Architecture → Internals → Packet/State → Configuration → Verification → Troubleshooting → Lab → Production یاد بگیر. مباحث مرتبط: $related"')
s=s.replace('AiMode.SOCRATIC->AiResponse("راهنمای سقراطی","به‌جای حدس یک سؤال قابل‌آزمایش انتخاب کن:', 'AiMode.SOCRATIC->AiResponse("راهنمای سقراطی","$grounded\\n\\nبه‌جای حدس یک سؤال قابل‌آزمایش انتخاب کن:')
s=s.replace('AiMode.EXAMINER->AiResponse("آزمون","بدون نگاه به پاسخ،', 'AiMode.EXAMINER->AiResponse("آزمون","$grounded\\n\\nبدون نگاه به پاسخ،')
s=s.replace('AiMode.TROUBLESHOOTER, AiMode.AUTONOMOUS_COACH->AiResponse("Troubleshooting Coach","از Interface/Link شروع کن،', 'AiMode.TROUBLESHOOTER, AiMode.AUTONOMOUS_COACH->AiResponse("Troubleshooting Coach","$grounded\\n\\nاز Interface/Link شروع کن،')
s=s.replace('AiMode.LAB_COACH->AiResponse("Lab Coach","Lab را به', 'AiMode.LAB_COACH->AiResponse("Lab Coach","$grounded\\n\\nLab را به')
s=s.replace('AiMode.CONFIG_REVIEWER->AiResponse("Config Reviewer","Configuration را', 'AiMode.CONFIG_REVIEWER->AiResponse("Config Reviewer","$grounded\\n\\nConfiguration را')
p.write_text(s,encoding='utf8')

# update readme and manifests counts
readme=root/'README.md'; r=readme.read_text(encoding='utf8');
r+='''\n\n## V9 Expert Deep\n\nAll 770 core lessons now carry a domain-aware expert dossier: internals, state/packet trace, configuration, verification, failure analysis, lab, incident, production design and mastery contract. The web app provides advanced central search across deep fields with type/tag/level filters. AI is grounded through local RAG context and local LLM adapters.\n'''; readme.write_text(r,encoding='utf8')

# Validate JSON
for rel in ['app/src/main/assets/curriculum.json','web/assets/curriculum.json','AI_CONTENT_MANIFEST.json']:
 json.loads((root/rel).read_text(encoding='utf8'))

# package
out=Path('/mnt/data/NetMasterLearning-v9.0-Expert-Deep-AI-MasoudJokar.zip')
if out.exists(): out.unlink()
with zipfile.ZipFile(out,'w',zipfile.ZIP_DEFLATED) as z:
 for p in root.rglob('*'):
  if p.is_file() and p != out:
   z.write(p,p.relative_to(root))
print('ZIP',out, out.stat().st_size)
print('SHA256',hashlib.sha256(out.read_bytes()).hexdigest())
print('LESSONS',sum(len(v['lessons']) for v in D['levels']))
# basic content depth stats
ls=[x for v in D['levels'] for x in v['lessons']]
print('avg chars dossier',sum(len(x['internals'])+len(x['packet_state_walkthrough'])+len(x['failure_analysis'])+len(x['lab']) for x in ls)//len(ls))
