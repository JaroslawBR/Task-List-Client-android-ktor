package com.example.tasklistclientktor

import android.content.Context
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.accept
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.client.utils.EmptyContent.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.gson.gson
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.security.MessageDigest

data class Task(
    var title: String = "",
    var text: String = "",
    var lastEdit: EditInfo? = null
)

data class EditInfo(
    var userId: String = "",
    var timeStamp: Long = 0
)

data class ConnectionParameters(
    var ipAddress: String = "192.168.68.61",
    var portAddress: String = "8080"
)

object TaskStorage {
    private val taskList = MutableStateFlow(mutableMapOf<String, Task>()) // id - string
    val taskListState: StateFlow<MutableMap<String, Task>> = taskList

    private var hashTask: String? =  null
    private val response = MutableStateFlow("")
    val responseState: StateFlow<String> = response

    private var oldHashList = mutableSetOf<String>()
    private var idListToIgnore = mutableSetOf<String>()
    private val lock = Any()
    val gsonTask = Gson()

    private lateinit var taskListFile: File
    private lateinit var hashTaskFile: File
    private lateinit var oldHashListFile: File
    private lateinit var connectionParametersFile: File

    private var connectionParameters = MutableStateFlow(ConnectionParameters())
    val connectionParametersState: StateFlow<ConnectionParameters> = connectionParameters

    /**
     * Initializes the TaskStorage with the given context.
     * Loads task list, hash, and old hash list from files.
     */
    fun start(context: Context) {
        val folder = context.filesDir
        taskListFile = File(folder, "taskList.json")
        hashTaskFile = File(folder, "hashTask.json")
        oldHashListFile = File(folder, "oldHashList.json")
        connectionParametersFile = File(folder, "connectionParameters.json")
        loadFromFile()
    }

    private fun updateIgnoreIdList(list: MutableSet<String>){
        idListToIgnore = list
    }

        private fun saveToFile() {
            synchronized(lock) {
                taskListFile.writeText(gsonTask.toJson(taskList.value))
                hashTaskFile.writeText(gsonTask.toJson(hashTask))
                oldHashListFile.writeText(gsonTask.toJson(oldHashList))
                connectionParametersFile.writeText(gsonTask.toJson(connectionParameters.value))
            }
        }

    private fun loadFromFile() {
        synchronized(lock) {
            if (taskListFile.exists()) {
                val taskType = object : TypeToken<MutableMap<String, Task>>() {}.type
                val loadedTaskList: MutableMap<String, Task> = gsonTask.fromJson(taskListFile.readText(), taskType)
                taskList.value = loadedTaskList
            }

            if (hashTaskFile.exists()) {
                hashTask = gsonTask.fromJson(hashTaskFile.readText(), String::class.java)
            }

            if (oldHashListFile.exists()) {
                val oldHashType = object : TypeToken<MutableSet<String>>() {}.type
                val loadedOldHashList: MutableSet<String> = gsonTask.fromJson(oldHashListFile.readText(), oldHashType)
                oldHashList = loadedOldHashList
            }

            if (connectionParametersFile.exists()) {
                val connectionParametersType = object : TypeToken<ConnectionParameters>() {}.type
                val loadedConnectionParameters: ConnectionParameters = gsonTask.fromJson(connectionParametersFile.readText(), connectionParametersType)
                connectionParameters.value = loadedConnectionParameters
            }
        }
    }

    fun updateConnectionParameters(ip: String, port: String){
        connectionParameters.update {
            it.copy(ipAddress = ip, portAddress = port)
        }
        saveToFile()
    }

    private fun createAddress(): String {
        return "https://${connectionParameters.value.ipAddress}"

    }


    fun addNewIgnoredId(id: String){
        idListToIgnore.add(id)
        saveToFile()
    }

    fun updateTaskList(taskList: MutableMap<String, Task>){
        this.taskList.value = taskList
        saveToFile()
    }

    private fun hashTaskList(taskList: Map<String, Task>) {
        val digest = MessageDigest.getInstance("SHA-256")
        taskList.toSortedMap().forEach { (key, task) ->
            digest.update(key.toByteArray())
            digest.update(task.title.toByteArray())
            digest.update(task.text.toByteArray())
            task.lastEdit?.let {
                digest.update(it.userId.toByteArray())
                digest.update(it.timeStamp.toString().toByteArray())
            }
        }
        updateHash(digest.digest().joinToString("") { "%02x".format(it) })
    }

    private fun updateHash(string: String) {
        hashTask?.let { oldHashList.add(it) }
        hashTask = string
    }

    fun checkFile(onClick: () -> Unit) {
        CoroutineScope(Dispatchers.IO).launch {
            hashTaskList(taskList.value ?: mutableMapOf())
            val hash = hashTask

            val response1 = makeHttpClient().use { client ->
                try {
                    client.get("${createAddress()}/checkTaskList") {
                        headers {
                            append(HttpHeaders.Authorization, "Basic ${encodeCredentials()}")
                        }
                        contentType(ContentType.Application.Json)
                        parameter("hash", hash)
                    }.bodyAsText()
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }

            checkReceived(response1)
            onClick()
        }
    }

    fun encodeCredentials(): String {
        val user = UserRepository.getUser()
        val credentials = "${user.name}:${user.password}"
        return Base64.encodeToString(credentials.toByteArray(), Base64.NO_WRAP)
    }

    private suspend fun checkReceived(responseHash: String?) {
        var serverInput = responseHash ?: return
        var counter = 0
        while (true) {
            counter++
            if (counter == 10) {
                return
            }
            when (serverInput) {
                "downlandFile" -> {
                    serverInput = downlandIgnoreList()
                    serverInput = downlandFile()
                    response.value = serverInput
                }
                "sendFile" -> {
                    serverInput = sendIdListIgnore()
                    serverInput = sendFile()
                    response.value = serverInput
                }
                "filesSynchronized" -> {
                    response.value = "Files synchronized"
                    return
                }
                else -> {
                    response.value = serverInput
                    return
                }
            }
        }
    }

    private suspend fun sendFile(): String {
        return withContext(Dispatchers.IO) {
            val hash = hashTask ?: return@withContext "Hash is null"
            val taskListJson = gsonTask.toJson(taskList.value)

            makeHttpClient().use { client ->
                try {
                    client.post("${createAddress()}/updateTaskList") {
                        headers {
                            append(HttpHeaders.Authorization, "Basic ${encodeCredentials()}")
                        }
                        contentType(ContentType.Application.Json)
                        header("Hash", hash)
                        setBody(taskListJson)
                    }.bodyAsText()
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
        }
    }

    private suspend fun sendIdListIgnore(): String {
        return withContext(Dispatchers.IO) {
            val hash = hashTask ?: return@withContext "Hash is null"
            val ignoreList = gsonTask.toJson(idListToIgnore.toList())

            makeHttpClient().use { client ->
                try {
                    client.post("${createAddress()}/updateIgnoreID") {
                        headers {
                            append(HttpHeaders.Authorization, "Basic ${encodeCredentials()}")
                        }
                        contentType(ContentType.Application.Json)
                        header("Hash", hash)
                        setBody(ignoreList)
                    }.bodyAsText()
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
        }
    }

    private suspend fun downlandFile(): String {
        return withContext(Dispatchers.IO) {
            makeHttpClient().use { client ->
                try {
                    val response: String = client.get("${createAddress()}/downlandTaskList") {
                        headers {
                            append(HttpHeaders.Authorization, "Basic ${encodeCredentials()}")
                        }
                        accept(ContentType.Application.Json)
                    }.bodyAsText()

                    val taskType = object : TypeToken<MutableMap<String, Task>>() {}.type
                    val downloadedTaskList: MutableMap<String, Task> = gsonTask.fromJson(response, taskType)
                    synchronized(lock) {
                        updateTaskList(downloadedTaskList)
                        saveToFile()
                    }
                    "File downloaded"
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
        }
    }

    private suspend fun downlandIgnoreList(): String {
        return withContext(Dispatchers.IO) {
            makeHttpClient().use { client ->
                try {
                    val response: String = client.get("${createAddress()}/downlandIgnoreTask") {
                        headers {
                            append(HttpHeaders.Authorization, "Basic ${encodeCredentials()}")
                        }
                        accept(ContentType.Application.Json)
                    }.bodyAsText()

                    val taskType = object : TypeToken< MutableSet<String>>() {}.type
                    val downloadedTaskList:  MutableSet<String> = gsonTask.fromJson(response, taskType)
                    synchronized(lock) {
                        updateIgnoreIdList(downloadedTaskList)
                        saveToFile()
                    }
                    "File downloaded"
                } catch (e: Exception) {
                    "Error: ${e.message}"
                }
            }
        }
    }

    private fun makeHttpClient(): HttpClient {
        return HttpClient {
            install(ContentNegotiation) {
                gson {
                    setPrettyPrinting()
                }
            }
        }
    }
}
