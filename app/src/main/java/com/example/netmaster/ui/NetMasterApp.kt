package com.example.netmaster.ui
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetMasterApp(vm:NetMasterViewModel){
 var tab by remember{mutableIntStateOf(0)}
 var selectedLevel by remember{mutableStateOf<Int?>(null)}
 var selectedLesson by remember{mutableStateOf<Lesson?>(null)}
 var searchOpen by remember{mutableStateOf(false)}
 val tabs=listOf("خانه" to Icons.Default.Home,"یادگیری" to Icons.Default.MenuBook,"Lab" to Icons.Default.Build,"آزمون" to Icons.Default.Quiz,"یادداشت" to Icons.Default.EditNote,"AI" to Icons.Default.AutoAwesome,"Ops" to Icons.Default.Dns,"Simulator" to Icons.Default.Terminal,"Engineer OS" to Icons.Default.Hub)
 Scaffold(topBar={if(selectedLesson==null&&selectedLevel==null&&!searchOpen) TopAppBar(title={Text("NetMaster",fontWeight=FontWeight.Bold)},actions={IconButton({searchOpen=true}){Icon(Icons.Default.Search,"search")}})},
 bottomBar={NavigationBar{tabs.forEachIndexed{i,x->NavigationBarItem(selected=i==tab,onClick={tab=i;selectedLevel=null;selectedLesson=null;searchOpen=false},icon={Icon(x.second,x.first)},label={Text(x.first,fontSize=10.sp)})}}})
 {pad->Box(Modifier.padding(pad).fillMaxSize()){when{
  searchOpen->SearchScreen(vm){selectedLesson=it;searchOpen=false}
  selectedLesson!=null->LessonDetail(selectedLesson!!,vm){selectedLesson=null}
  selectedLevel!=null->LevelDetail(vm,selectedLevel!!){selectedLesson=it}
  tab==0->HomeScreen(vm){tab=1}
  tab==1->LearnScreen(vm){selectedLevel=it}
  tab==2->LabScreen(vm)
  tab==3->QuizScreen(vm){selectedLesson=it}
  tab==4->NotesScreen(vm)
  tab==5->AiScreen(vm)
  tab==6->OpsScreen(vm)
  tab==7->SimScreen(vm)
  tab==8->V11Screen(vm)
  else->HomeScreen(vm){tab=0}
 }}}
}

@Composable fun HomeScreen(vm:NetMasterViewModel,openLearn:()->Unit){
 val c=vm.curriculum.collectAsState().value?:return
 val p=vm.progress.collectAsState().value
 val total=c.levels.sumOf{it.lessons.size};val done=p.values.count{it.completed}
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{Text("NetMaster — مرکز مهندسی شبکه",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Text("درس‌ها: $done / $total • سطح: ${c.levels.size}")}
  item{Button(openLearn,Modifier.fillMaxWidth()){Text("شروع یادگیری")}}
  item{Card{Column(Modifier.padding(14.dp)){Text("سازنده: مهندس مسعود جوکار",fontWeight=FontWeight.Bold);Text("09132184122");Text("نسخه 11.0.0")}}}
  items(c.levels.take(15)){l->Card{Column(Modifier.padding(12.dp)){Text("${l.id}. ${l.title}",fontWeight=FontWeight.Bold);Text("${l.lessons.size} درس")}}}
 }
}
@Composable fun LearnScreen(vm:NetMasterViewModel,onLevel:(Int)->Unit){
 val c=vm.curriculum.collectAsState().value?:return
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Learning Engine",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  items(c.levels){l->Card(onClick={onLevel(l.id)}){Column(Modifier.padding(14.dp)){Text("${l.id}. ${l.title}",fontWeight=FontWeight.Bold);Text(l.summary)}}}
 }
}
@Composable fun LevelDetail(vm:NetMasterViewModel,id:Int,onLesson:(Lesson)->Unit){
 val l=vm.curriculum.collectAsState().value?.levels?.firstOrNull{it.id==id}?:return
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("سطح ${l.id}: ${l.title}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  items(l.lessons){x->Card(onClick={onLesson(x)}){Column(Modifier.padding(12.dp)){Text(x.title,fontWeight=FontWeight.Bold);Text(x.goal)}}}
 }
}
@Composable fun LessonDetail(x:Lesson,vm:NetMasterViewModel,back:()->Unit){
 var note by remember{mutableStateOf("")}
 val pr=vm.progress.collectAsState().value[x.id]
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{TextButton(back){Text("بازگشت")};Text(x.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  if(x.goal.isNotBlank()) item{Text("هدف",fontWeight=FontWeight.Bold);Card{Text(x.goal,Modifier.padding(12.dp))}}
  if(x.simple.isNotBlank()) item{Text("مفهوم ساده",fontWeight=FontWeight.Bold);Card{Text(x.simple,Modifier.padding(12.dp))}}
  if(x.technical.isNotBlank()) item{Text("مفهوم فنی",fontWeight=FontWeight.Bold);Card{Text(x.technical,Modifier.padding(12.dp))}}
  if(x.deepTechnical.isNotBlank()) item{Text("تحلیل عمیق",fontWeight=FontWeight.Bold);Card{Text(x.deepTechnical,Modifier.padding(12.dp))}}
  if(x.commands.isNotBlank()||x.platformCommands.isNotBlank()) item{Text("Commands",fontWeight=FontWeight.Bold);Card{Text(x.platformCommands.ifBlank{x.commands},Modifier.padding(12.dp))}}
  if(x.lab.isNotBlank()) item{Text("Lab",fontWeight=FontWeight.Bold);Card{Text(x.lab,Modifier.padding(12.dp))}}
  if(x.troubleshooting.isNotBlank()) item{Text("Troubleshooting",fontWeight=FontWeight.Bold);Card{Text(x.troubleshooting,Modifier.padding(12.dp))}}
  item{Text("Mastery",fontWeight=FontWeight.Bold);Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){Mastery.entries.forEach{m->FilterChip(selected=pr?.mastery==m.name,onClick={vm.setMastery(x.id,m)},label={Text(m.name)})}}}
  item{OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),label={Text("یادداشت")});Button({if(note.isNotBlank()){vm.addNote(x.id,x.title,note);note=""}},Modifier.fillMaxWidth()){Text("ذخیره")}}
  items(x.quiz){q->Card{Column(Modifier.padding(12.dp)){Text(q.q,fontWeight=FontWeight.Bold);Text(q.a)}}}
 }
}
@Composable fun LabScreen(vm:NetMasterViewModel){
 val twin=vm.twin.collectAsState().value
 val faults=vm.twinEngine.faults()
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Digital Twin Lab",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({vm.resetTwin()}){Text("Reset")};Button({vm.saveSimulatorSnapshot("manual")}){Text("Snapshot")}}}
  item{Card{Column(Modifier.padding(12.dp)){Text("Nodes: ${twin.nodes.size} Links: ${twin.links.size}");twin.simulator?.let{Text("DNS ${it.dnsHealthy} FW ${it.firewallHealthy}")}}}}
  items(faults){f->Card(onClick={vm.injectFault(f)}){Column(Modifier.padding(12.dp)){Text(f.title,fontWeight=FontWeight.Bold);Text(f.description)}}}
 }
}
@Composable fun QuizScreen(vm:NetMasterViewModel,open:(Lesson)->Unit){
 val c=vm.curriculum.collectAsState().value?:return
 val ls=remember(c){c.levels.flatMap{it.lessons}.filter{it.quiz.isNotEmpty()}}
 var i by remember{mutableIntStateOf(0)}
 if(ls.isEmpty()){Text("Quiz موجود نیست",Modifier.padding(16.dp));return}
 val l=ls[i%ls.size]
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Quiz: ${l.title}",fontWeight=FontWeight.Bold)}
  items(l.quiz){q->Card{Column(Modifier.padding(12.dp)){Text(q.q);Text(q.a)}}}
  item{Row{Button({i++},Modifier.weight(1f)){Text("بعدی")};OutlinedButton({open(l)},Modifier.weight(1f)){Text("درس")}}}
 }
}
@Composable fun NotesScreen(vm:NetMasterViewModel){
 val n=vm.notes.collectAsState().value
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("یادداشت‌ها",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  if(n.isEmpty()) item{Text("خالی")} else items(n){x->Card{Column(Modifier.padding(12.dp)){Text(x.title,fontWeight=FontWeight.Bold);Text(x.body)}}}
 }
}
@Composable fun SearchScreen(vm:NetMasterViewModel,onLesson:(Lesson)->Unit){
 var q by remember{mutableStateOf("")}
 val hits=vm.searchResults.collectAsState().value
 LaunchedEffect(q){if(q.length>=2)vm.searchAll(q)}
 Column(Modifier.padding(16.dp)){
  Text("جستجو",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
  OutlinedTextField(q,{q=it},Modifier.fillMaxWidth())
  LazyColumn{items(hits){h->ListItem(headlineContent={Text(h.title)},supportingContent={Text(h.snippet)},trailingContent={if(h.kind=="LESSON") TextButton({vm.curriculum.value?.levels?.flatMap{it.lessons}?.firstOrNull{it.id==h.id}?.let(onLesson)}){Text("باز")}})}}
 }
}
@Composable fun AiScreen(vm:NetMasterViewModel){
 var prompt by remember{mutableStateOf("")}
 var mode by remember{mutableStateOf(AiMode.TROUBLESHOOTER)}
 val r=vm.aiResponse.collectAsState().value
 val backend=vm.aiBackend.collectAsState().value
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("AI Center ($backend)",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){AiMode.entries.take(4).forEach{FilterChip(selected=mode==it,onClick={mode=it},label={Text(it.name,fontSize=9.sp)})}}}
  item{OutlinedTextField(prompt,{prompt=it},Modifier.fillMaxWidth(),minLines=3);Button({vm.askAi(mode,prompt)},enabled=prompt.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("تحلیل")}}
  r?.let{item{Card{Column(Modifier.padding(12.dp)){Text(it.title,fontWeight=FontWeight.Bold);Text(it.answer)}}}}
  item{Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){Button({vm.rebuildVectorIndex()}){Text("RAG")};Button({vm.buildKnowledgeGraph()}){Text("Graph")};Button({vm.exportBackup()}){Text("Backup")}}}
 }
}
@Composable fun OpsScreen(vm:NetMasterViewModel){
 val incidents by vm.incidents.collectAsState()
 val due by vm.dueReviews.collectAsState()
 var title by remember{mutableStateOf("")};var symptom by remember{mutableStateOf("")}
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Operations",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Text("Reviews due: ${due.size}")}
  item{OutlinedTextField(title,{title=it},Modifier.fillMaxWidth(),label={Text("Incident title")});OutlinedTextField(symptom,{symptom=it},Modifier.fillMaxWidth(),label={Text("Symptom")});Button({if(title.isNotBlank()){vm.createIncident(title,symptom);title="";symptom=""}},Modifier.fillMaxWidth()){Text("ثبت Incident")}}
  items(incidents.take(20)){i->Card{Text("${i.title} [${i.status}]",Modifier.padding(12.dp))}}
 }
}
@Composable fun SimScreen(vm:NetMasterViewModel){
 val twin=vm.twin.collectAsState().value
 val output=vm.simOutput.collectAsState().value
 var cmd by remember{mutableStateOf("")}
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Simulator",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{Text("Nodes ${twin.nodes.size} / Links ${twin.links.size}")}
  item{OutlinedTextField(cmd,{cmd=it},Modifier.fillMaxWidth(),label={Text("Command")});Button({vm.runSimulator(cmd)},Modifier.fillMaxWidth()){Text("Run")}}
  item{Card{Text(output.ifBlank{"—"},Modifier.padding(12.dp))}}
 }
}
@Composable fun V11Screen(vm:NetMasterViewModel){
 val inv=vm.v11Investigation.collectAsState().value
 val passport=vm.v11Passport.collectAsState().value
 val faults=vm.v11Faults.collectAsState().value
 var symptom by remember{mutableStateOf("")}
 LaunchedEffect(Unit){vm.computeV11Passport()}
 LazyColumn(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("Engineer OS V11",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  item{OutlinedTextField(symptom,{symptom=it},Modifier.fillMaxWidth(),label={Text("Symptom")});Button({vm.investigateV11(symptom,"")},Modifier.fillMaxWidth()){Text("Investigate")}}
  inv?.let{item{Card{Column(Modifier.padding(12.dp)){Text("Divergence: ${it.firstDivergence}");it.hypotheses.take(5).forEach{h->Text("${h.title} ${h.confidence}%")}}}}}
  item{Text("Passport: ${passport?.overall ?: "-"}%")}
  items(faults){f->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(f.title);Button({vm.injectV11Fault(f.id)}){Text("Inject")}}}
 }
}
