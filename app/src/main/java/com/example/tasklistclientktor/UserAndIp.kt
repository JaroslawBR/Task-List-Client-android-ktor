package com.example.tasklistclientktor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog


@Composable
fun DialogUser(onDismissRequest: () -> Unit){
    Dialog(
        onDismissRequest = { onDismissRequest() },
    ) {
        Column(modifier = Modifier.fillMaxWidth()
            .background(Color(0xFF2A2A2A))
            .padding(8.dp)) {
            val value by UserRepository.userState.collectAsState()
            var user by rememberSaveable { mutableStateOf(value.name) }
            var password by rememberSaveable { mutableStateOf(value.password) }
            var showPassword by rememberSaveable { mutableStateOf(false) }
            val value1 by TaskStorage.connectionParametersState.collectAsState()
            var valueTitle by rememberSaveable { mutableStateOf(value1.ipAddress) }
            var valueText by rememberSaveable { mutableStateOf(value1.portAddress) }
            val context = LocalContext.current

            TextField(
                value = user,
                onValueChange = { user = it },
                label = { Text(text = "Nazwa użytkownika") },
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
            )
            Row {
                TextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(text = "Hasło") },
                    modifier = Modifier.fillMaxWidth()
                        .padding(5.dp)
                        .weight(1f),
                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                )
                Checkbox(checked = showPassword, onCheckedChange = { showPassword = it })
            }
            TextField(
                value = valueTitle,
                onValueChange = { valueTitle = it },
                label = { Text(text = "Adres Ip") },
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
            )
            TextField(
                value = valueText,
                onValueChange = { valueText = it },
                label = { Text(text = "Adres Portu") },
                modifier = Modifier.fillMaxWidth()
                    .padding(5.dp)
            )
            Button(
                onClick = {
                        TaskStorage.updateConnectionParameters(
                            valueTitle,
                            valueText
                        )
                        UserRepository.updateUser(user, password)
                        onDismissRequest()
                }, colors = ButtonDefaults.buttonColors(
                    containerColor = Color(
                        0xFF064F23
                    ), disabledContainerColor = Color(0xFF363636)
                ), modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Zapisz", fontSize = 24.sp, color = Color(0xffffffff))
            }



        }
    }
}


