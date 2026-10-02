package com.example.netmaster.ui
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private fun masteryFa(v:String)=when(v){Mastery.UNKNOWN.name->"بلد نیستم";Mastery.REVIEW.name->"نیاز به مرور";Mastery.KNOWN.name->"بلدم";Mastery.MASTERED.name->"مسلط";else->v}
private fun modeFa(m:AiMode)=when(m){AiMode.TEACHER->"معلم";AiMode.SOCRATIC->"سقراطی";AiMode.TROUBLESHOOTER->"عیب‌یابی";AiMode.EXAMINER->"آزمون‌گر";AiMode.LAB_COACH->"مربی Lab";AiMode.CONFIG_REVIEWER->"بازبین Config";AiMode.AUTONOMOUS_COACH->"مربی خودکار"}

@Composable fun SectionCard(title:String,sub:String?=null,content:@Composable ColumnScope.()->Unit){
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=MaterialTheme.colorScheme.primary)
  if(!sub.isNullOrBlank()) Text(sub,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
  content()
 }}}
@Composable fun BodyText(t:String,mono:Boolean=false){Text(t,style=MaterialTheme.typography.bodyMedium.copy(textDirection=TextDirection.ContentOrRtl,fontFamily=if(mono)FontFamily.Monospace else FontFamily.Default,lineHeight=22.sp))}

@Composable fun TopologyGraph(nodes:List<TwinNode>,links:List<TwinLink>,mod:Modifier=Modifier){
 val primary=MaterialTheme.colorScheme.primary;val outline=MaterialTheme.colorScheme.outline
 Card(mod,shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp)){
  Text("توپولوژی شبکه",fontWeight=FontWeight.Bold,color=primary)
  Text("نودها و لینک‌های شبیه‌ساز",style=MaterialTheme.typography.bodySmall)
  Spacer(Modifier.height(8.dp))
  Canvas(Modifier.fillMaxWidth().height(220.dp).background(MaterialTheme.colorScheme.surfaceVariant,RoundedCornerShape(12.dp))){
   if(nodes.isEmpty())return@Canvas
   val cx=size.width/2f;val cy=size.height/2f;val r=min(size.width,size.height)*0.32f
   val pos=nodes.mapIndexed{idx,n->val a=idx*2.0*Math.PI/nodes.size;n.id to Offset(cx+r*cos(a).toFloat(),cy+r*sin(a).toFloat())}.toMap()
   links.forEach{e->val a=pos[e.from];val b=pos[e.to];if(a!=null&&b!=null)drawLine(outline,a,b,3f,pathEffect=if(e.up)null else PathEffect.dashPathEffect(floatArrayOf(10f,8f)))}
   nodes.forEach{n->val p=pos[n.id]?:return@forEach;drawCircle(primary,22f,p);drawCircle(androidx.compose.ui.graphics.Color.White,22f,p,style=Stroke(2f))
    drawContext.canvas.nativeCanvas.drawText(n.name.take(10),p.x-28f,p.y-28f,android.graphics.Paint().apply{color=android.graphics.Color.DKGRAY;textSize=28f;isAntiAlias=true})}
  }
 }}}

@Composable fun ProtocolFlowChart(title:String,steps:List<String>){
 val primary=MaterialTheme.colorScheme.primary
 Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(16.dp)){Column(Modifier.padding(12.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
  Text(title,fontWeight=FontWeight.Bold,color=primary)
  Text("مسیر پردازش پروتکل / لایه",style=MaterialTheme.typography.bodySmall)
  Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(4.dp)){
   steps.forEachIndexed{i,s->
    Surface(shape=RoundedCornerShape(10.dp),color=MaterialTheme.colorScheme.primaryContainer){Text(s,Modifier.padding(horizontal=10.dp,vertical=8.dp),fontSize=12.sp,fontWeight=FontWeight.Medium,textAlign=TextAlign.Center)}
    if(i<steps.lastIndex)Text("←",color=primary,fontWeight=FontWeight.Bold)
   }
  }
 }}}

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun NetMasterApp(vm:NetMasterViewModel){
 var tab by remember{mutableIntStateOf(0)};var selectedLevel by remember{mutableStateOf<Int?>(null)};var selectedLesson by remember{mutableStateOf<Lesson?>(null)};var searchOpen by remember{mutableStateOf(false)}
 val tabs=listOf("خانه" to Icons.Default.Home,"یادگیری" to Icons.Default.MenuBook,"آزمایشگاه" to Icons.Default.Build,"آزمون" to Icons.Default.Quiz,"یادداشت" to Icons.Default.EditNote,"هوش مصنوعی" to Icons.Default.AutoAwesome,"عملیات" to Icons.Default.Dns,"شبیه‌ساز" to Icons.Default.Terminal,"مهندس" to Icons.Default.Hub)
 Scaffold(
  topBar={if(selectedLesson==null&&selectedLevel==null&&!searchOpen) CenterAlignedTopAppBar(title={Column(horizontalAlignment=Alignment.CenterHorizontally){Text("نت‌مستر",fontWeight=FontWeight.Bold);Text("مرکز مهندسی شبکه",style=MaterialTheme.typography.labelSmall)}},actions={IconButton({searchOpen=true}){Icon(Icons.Default.Search,"جستجو")}})},
  bottomBar={NavigationBar{tabs.forEachIndexed{i,x->NavigationBarItem(selected=i==tab,onClick={tab=i;selectedLevel=null;selectedLesson=null;searchOpen=false},icon={Icon(x.second,x.first)},label={Text(x.first,fontSize=9.sp,maxLines=1)})}}}
 ){pad->Box(Modifier.padding(pad).fillMaxSize()){when{
  searchOpen->SearchScreen(vm){selectedLesson=it;searchOpen=false}
  selectedLesson!=null->LessonDetail(selectedLesson!!,vm){selectedLesson=null}
  selectedLevel!=null->LevelDetail(vm,selectedLevel!!){selectedLesson=it}
  tab==0->HomeScreen(vm){tab=1};tab==1->LearnScreen(vm){selectedLevel=it};tab==2->LabScreen(vm);tab==3->QuizScreen(vm){selectedLesson=it}
  tab==4->NotesScreen(vm);tab==5->AiScreen(vm);tab==6->OpsScreen(vm);tab==7->SimScreen(vm);tab==8->V11Screen(vm);else->HomeScreen(vm){tab=0}
 }}}
}

@Composable fun HomeScreen(vm:NetMasterViewModel,openLearn:()->Unit){
 val c=vm.curriculum.collectAsState().value;val p=vm.progress.collectAsState().value;val twin=vm.twin.collectAsState().value
 if(c==null){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()};return}
 val total=remember(c){c.levels.sumOf{it.lessons.size}};val done=remember(p){p.values.count{it.completed}};val mastered=remember(p){p.values.count{it.mastery==Mastery.MASTERED.name}}
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(12.dp),contentPadding=PaddingValues(vertical=12.dp)){
  item{SectionCard("پیشرفت شما","خلاصه وضعیت یادگیری آفلاین"){LinearProgressIndicator(progress={if(total==0)0f else done.toFloat()/total},modifier=Modifier.fillMaxWidth().height(8.dp));Text("تکمیل‌شده: $done از $total درس  |  مسلط: $mastered");Text("تعداد سطح‌ها: ${c.levels.size}")}}
  item{SectionCard("شروع سریع","از مسیر یادگیری حرفه‌ای وارد شوید"){Button(onClick=openLearn,modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.PlayArrow,null);Spacer(Modifier.width(8.dp));Text("شروع یادگیری")}}}
  item{TopologyGraph(twin.nodes,twin.links)}
  item{ProtocolFlowChart("فلوچارت عیب‌یابی استاندارد",listOf("لایه فیزیکی","L2/VLAN","IP/ARP","مسیریابی","DNS","فایروال","اپلیکیشن"))}
  item{SectionCard("درباره برنامه","نسخه یکپارچه مهندسی شبکه"){Text("سازنده: مهندس مسعود جوکار",fontWeight=FontWeight.Bold);Text("تماس: ۰۹۱۳۲۱۸۴۱۲۲");Text("نسخه ۱۱.۰ — آموزش + آزمایشگاه + هوش مصنوعی")}}
  item{Text("سطوح پیشنهادی",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)}
  items(c.levels.take(12),key={it.id}){l->Card(onClick=openLearn,shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(14.dp)){Text("${l.id}. ${l.title}",fontWeight=FontWeight.Bold);Text("${l.lessons.size} درس",style=MaterialTheme.typography.bodySmall)}}}
 }
}
@Composable fun LearnScreen(vm:NetMasterViewModel,onLevel:(Int)->Unit){
 val c=vm.curriculum.collectAsState().value?:return
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
  item{SectionCard("موتور یادگیری","Concept → Packet → Config → Lab → Mastery"){Text("هر سطح را باز کنید و درس‌ها را به ترتیب بخوانید.")}}
  items(c.levels,key={it.id}){l->Card(onClick={onLevel(l.id)},shape=RoundedCornerShape(14.dp)){Column(Modifier.padding(14.dp)){Text("${l.id}. ${l.title}",fontWeight=FontWeight.Bold);if(l.summary.isNotBlank())BodyText(l.summary);Text("${l.lessons.size} درس",color=MaterialTheme.colorScheme.primary)}}}
 }
}
@Composable fun LevelDetail(vm:NetMasterViewModel,id:Int,onLesson:(Lesson)->Unit){
 val l=vm.curriculum.collectAsState().value?.levels?.firstOrNull{it.id==id}?:return;val p=vm.progress.collectAsState().value
 LazyColumn(Modifier.fillMaxSize().padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
  item{Text("سطح ${l.id}: ${l.title}",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold);if(l.summary.isNotBlank())BodyText(l.summary)}
  items(l.lessons,key={it.id}){x->Card(onClick={onLesson(x)},shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){Text(x.title,fontWeight=FontWeight.Bold);if(x.goal.isNotBlank())Text(x.goal,style=MaterialTheme.typography.bodySmall,maxLines=2);Text(masteryFa(p[x.id]?.mastery?:Mastery.UNKNOWN.name),color=MaterialTheme.colorScheme.secondary)}}}
 }
}
@Composable fun LessonDetail(x:Lesson,vm:NetMasterViewModel,back:()->Unit){
 var note by remember{mutableStateOf("")};val pr=vm.progress.collectAsState().value[x.id];val b=vm.bookmarks.collectAsState().value
 LazyColumn(Modifier.fillMaxSize().padding(horizontal=16.dp),verticalArrangement=Arrangement.spacedBy(10.dp),contentPadding=PaddingValues(vertical=12.dp)){
  item{Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically){TextButton(onClick=back){Icon(Icons.AutoMirrored.Filled.ArrowBack,null);Text("بازگشت")};IconButton({vm.toggleBookmark(x.id)}){Icon(if(b.contains(x.id))Icons.Default.Bookmark else Icons.Default.BookmarkBorder,"نشانک")}};Text(x.title,style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)}
  if(x.diagram.isNotBlank()||x.packetWalkthrough.isNotBlank())item{ProtocolFlowChart("فلوچارت / مسیر پروتکل این درس",listOf("ورودی","پردازش","تصمیم","خروجی","تأیید"))}
  if(x.goal.isNotBlank())item{SectionCard("هدف درس"){BodyText(x.goal)}}
  if(x.simple.isNotBlank())item{SectionCard("مفهوم ساده","برای شروع"){BodyText(x.simple)}}
  if(x.technical.isNotBlank())item{SectionCard("مفهوم فنی"){BodyText(x.technical)}}
  if(x.deepTechnical.isNotBlank())item{SectionCard("تحلیل عمیق"){BodyText(x.deepTechnical)}}
  if(x.packetWalkthrough.isNotBlank())item{SectionCard("گام‌به‌گام Packet / State"){BodyText(x.packetWalkthrough)}}
  if(x.diagram.isNotBlank())item{SectionCard("دیاگرام / توپولوژی متنی"){BodyText(x.diagram,true)}}
  if(x.configurationPlaybook.isNotBlank())item{SectionCard("پلی‌بوک پیکربندی"){BodyText(x.configurationPlaybook,true)}}
  if(x.commands.isNotBlank()||x.platformCommands.isNotBlank())item{SectionCard("دستورات"){BodyText(x.platformCommands.ifBlank{x.commands},true)}}
  if(x.lab.isNotBlank())item{SectionCard("آزمایشگاه"){BodyText(x.lab)}}
  if(x.troubleshooting.isNotBlank())item{SectionCard("عیب‌یابی"){BodyText(x.troubleshooting)}}
  item{SectionCard("سطح تسلط"){Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){Mastery.entries.forEach{m->FilterChip(selected=pr?.mastery==m.name,onClick={vm.setMastery(x.id,m)},label={Text(masteryFa(m.name),fontSize=11.sp)})}}}}
  item{SectionCard("یادداشت شخصی"){OutlinedTextField(note,{note=it},Modifier.fillMaxWidth(),minLines=3,label={Text("یادداشت خود را بنویسید")});Button(onClick={if(note.isNotBlank()){vm.addNote(x.id,x.title,note);note=""}},modifier=Modifier.fillMaxWidth()){Text("ذخیره یادداشت")}}}
  if(x.quiz.isNotEmpty()){item{Text("سؤالات آزمون",fontWeight=FontWeight.Bold)};items(x.quiz){q->Card(shape=RoundedCornerShape(12.dp)){Column(Modifier.padding(12.dp)){BodyText(q.q);Text("پاسخ: ${q.a}",color=MaterialTheme.colorScheme.secondary)}}}}
 }
}
