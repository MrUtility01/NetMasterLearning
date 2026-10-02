package com.example.netmaster.ui

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.netmaster.ai.*
import com.example.netmaster.data.*
import com.example.netmaster.domain.*
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun NetMasterApp(vm:NetMasterViewModel){
    var tab by remember{mutableIntStateOf(0)};var selectedLevel by remember{mutableStateOf<Int?>(null)};var selectedLesson by remember{mutableStateOf<Lesson?>(null)};var searchOpen by remember{mutableStateOf(false)}
    val tabs=listOf("خانه" to Icons.Default.Home,"یادگیری" to Icons.Default.MenuBook,"Lab" to Icons.Default.Build,"آزمون" to Icons.Default.Quiz,"یادداشت" to Icons.Default.EditNote,"AI" to Icons.Default.AutoAwesome,"Ops" to Icons.Default.Dns,"Simulator" to Icons.Default.Terminal,"Engineer OS" to Icons.Default.Hub)
    Scaffold(topBar={if(selectedLesson==null&&selectedLevel==null&&!searchOpen)TopAppBar(title={Text("NetMaster",fontWeight=FontWeight.Bold)},actions={IconButton({searchOpen=true}){Icon(Icons.Default.Search,"جستجو")}})},bottomBar={NavigationBar{tabs.forEachIndexed{i,x->NavigationBarItem(i==tab,{tab=i;selectedLevel=null;selectedLesson=null;searchOpen=false},{Icon(x.second,x.first)},label={Text(x.first,fontSize=10.sp)})}}}){pad->Box(Modifier.padding(pad).fillMaxSize()){when{searchOpen->Search(vm){selectedLesson=it;searchOpen=false};selectedLesson!=null->LessonScreen(selectedLesson!!,vm){selectedLesson=null};selectedLevel!=null->LevelScreen(vm,selectedLevel!!){selectedLesson=it};tab==0->Dashboard(vm){tab=1};tab==1->Learning(vm){selectedLevel=it};tab==2->Labs(vm);tab==3->Quiz(vm){selectedLesson=it};tab==4->Notes(vm);tab==5->AICenter(vm);tab==6->OperationsCenter(vm);tab==7->SimulatorCenter(vm);tab==8->V11EngineerOS(vm);else->Dashboard(vm){tab=0}}}}}
}
