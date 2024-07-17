package com.example.tasklistclientktor

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

import java.io.File

data class User(
    val name: String,
    val password: String,)

object UserRepository {
    private val user = MutableStateFlow(User("abcd", "abcd"))
    val userState: StateFlow<User> = user

    private lateinit var userFile: File
    val gson = Gson()

    fun start(context: Context) {
        val folder = context.filesDir
        userFile = File(folder, "user.json")

        loadFromFile()
    }
    private fun loadFromFile() {
        if (userFile.exists()) {
            val taskType = object : TypeToken<User>() {}.type
            val userFile: User = gson.fromJson(userFile.readText(), taskType)
            user.value = userFile
        }
    }

    private fun saveToFile() {
        userFile.writeText(gson.toJson(user.value))
    }

    fun updateUser(name: String, password: String) {
        user.update {
            it.copy(name = name.trim(), password = password.trim())
        }
        saveToFile()
    }

    fun getUser():User{
        return user.value

    }

}