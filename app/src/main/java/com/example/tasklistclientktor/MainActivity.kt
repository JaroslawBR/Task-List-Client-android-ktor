package com.example.tasklistclientktor

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.tasklistclientktor.ui.theme.TaskListClientKtorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        TaskStorage.start(this)
        UserRepository.start(this)
        setContent {
            TaskListClientKtorTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    MainScreen(modifier = Modifier.padding(innerPadding), viewModel = MainViewModel())
                }
            }
        }
    }
}

@Composable
fun MainScreen(modifier: Modifier, viewModel: MainViewModel) {
    val test by TaskStorage.responseState.collectAsState()
    val dialogEdit by viewModel.dialogEditState.collectAsState()
    val editId by viewModel.editIdState.collectAsState()
    val windowUser by viewModel.windowUserState.collectAsState()
    Column(modifier = modifier
        .fillMaxSize(),
        verticalArrangement = Arrangement.Center) {
        Button(
            onClick = {
                viewModel.openCloseWindowUser(true)
            }, colors = ButtonDefaults.buttonColors(
                containerColor = Color(
                    0xFF064F23
                ), disabledContainerColor = Color(0xFF363636)
            ), modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Ustawienia łączności", fontSize = 24.sp, color = Color(0xffffffff))
        }
        Button(onClick = {
            TaskStorage.checkFile(onClick = { viewModel.updateList() })
        }, colors = ButtonDefaults.buttonColors(containerColor = Color(
            0xFF064F23), disabledContainerColor = Color(0xFF363636)
        ), modifier = Modifier.fillMaxWidth()) {
            Text(text = "Synchronizuj: $test", fontSize = 24.sp, color = Color(0xffffffff))
        }
        CreateNewTask(viewModel)
        LazyColumnPart(viewModel = viewModel)
    }
    if (dialogEdit){
        DialogEdit(viewModel = viewModel, id = editId)
    }
    if (windowUser){
        DialogUser(onDismissRequest = {
            viewModel.openCloseWindowUser(false)
        })
    }
}

@Composable
fun DialogEdit(viewModel: MainViewModel, id: String){
    val data = viewModel.takeData(id)
    val text by viewModel.taskEditState.collectAsState()
    val localContext = LocalContext.current
    Dialog(onDismissRequest = {  viewModel.openCloseDialogEdit(false) }) {
        Column(modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF181818))
            .padding(10.dp)) {
            TextField(
                value = text.title,
                onValueChange = { viewModel.updateTaskTile(it) },
                label = { Text(text = "Tytuł") },
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
            )
            TextField(
                value = text.text,
                onValueChange = { viewModel.updateTaskText(it) },
                label = { Text(text = "Treść zadania") },
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
            )
            Button(
                onClick = {
                    viewModel.saveEditTask(id, localContext)
                    viewModel.openCloseDialogEdit(false)
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = Color(
                        0xFF064F23
                    ), disabledContainerColor = Color(0xFF363636)
                ), modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Zapisz zmiany", fontSize = 24.sp, color = Color(0xffffffff))
            }
        }
    }

}

@Composable
fun LazyColumnPart(viewModel: MainViewModel){
    val list by viewModel.keyListState.collectAsState()
    LazyColumn {
        items(list) { item ->
            Item(viewModel = viewModel, item = item)
        }
    }
}

@Composable
fun Item(viewModel: MainViewModel, item: String){
    Row(Modifier.fillMaxWidth()
        .padding(vertical = 5.dp)
        .background(Color(0xFF252525))) {
        val data = viewModel.takeData(item)
        if (data != null) {
            Row {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = data.title, fontSize = 24.sp, color = Color(0xffffffff))
                    Text(text = data.text, fontSize = 22.sp, color = Color(0xffffffff))
                }
                IconButton(onClick = {
                    viewModel.deleteTask(item)
                }) {
                    Icon(imageVector = Icons.Filled.Delete, contentDescription = "Delete")
                }
                IconButton(onClick = {
                    viewModel.updateTaskTile(data.title)
                    viewModel.updateTaskText(data.text)
                    viewModel.updateEditId(item)
                    viewModel.openCloseDialogEdit(true)
                }) {
                    Icon(imageVector = Icons.Filled.Edit, contentDescription = "Edit")
                }
            }
        }
    }
}


@Composable
fun CreateNewTask(viewModel: MainViewModel){
    var valueTitle by rememberSaveable { mutableStateOf("") }
    var valueText by rememberSaveable { mutableStateOf("") }
    val context = LocalContext.current
    Column(modifier = Modifier.fillMaxWidth()) {
        TextField(
            value = valueTitle,
            onValueChange = { valueTitle = it },
            label = { Text(text = "Tytuł") },
            modifier = Modifier.fillMaxWidth()
                .padding(5.dp)
        )
        TextField(
            value = valueText,
            onValueChange = { valueText = it },
            label = { Text(text = "Treść zadania") },
            modifier = Modifier.fillMaxWidth()
                .padding(5.dp)
        )
    }
    Button(onClick = {
        viewModel.addNewTask(valueTitle, valueText, context = context)
        valueText = ""
        valueTitle = ""
    }, enabled = valueTitle.isNotBlank(), colors = ButtonDefaults.buttonColors(containerColor = Color(
        0xFF064F23), disabledContainerColor = Color(0xFF363636)
    ), modifier = Modifier.fillMaxWidth()) {
       Text(text = "Dodaj nowe zdanienie", fontSize = 24.sp, color = Color(0xffffffff))
    }
}


@Preview(showBackground = true, device = "id:pixel_8_pro",
    uiMode = Configuration.UI_MODE_NIGHT_YES or Configuration.UI_MODE_TYPE_NORMAL
)

@Composable
fun GreetingPreview() {
    TaskListClientKtorTheme {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            MainScreen(modifier = Modifier.padding(innerPadding), viewModel = MainViewModel())
        }
    }
}