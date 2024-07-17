package com.example.tasklistclientktor

import android.content.Context
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.time.Instant
import java.util.UUID

class MainViewModel: ViewModel() {

    private val taskList = MutableStateFlow(TaskStorage.taskListState.value) // id - string

    private var hashTask = MutableStateFlow<String?>(null)

    private val oldHashList = MutableStateFlow(mutableSetOf<String>())
    val oldHashListState: StateFlow<MutableSet<String>> = oldHashList

    private val idListToIgnore = MutableStateFlow(mutableSetOf<String>())
    val idListToIgnoreState: StateFlow<MutableSet<String>> = idListToIgnore

    private val keyList = MutableStateFlow(taskList.value.keys.toMutableList())
    val keyListState: StateFlow<MutableList<String>> = keyList

    private val dialogEdit = MutableStateFlow(false)
    val dialogEditState: StateFlow<Boolean> = dialogEdit

    private val taskEdit = MutableStateFlow(Task())
    val taskEditState: StateFlow<Task> = taskEdit

    private val editId = MutableStateFlow("")
    val editIdState: StateFlow<String> = editId

    private val windowUser = MutableStateFlow(false)
    val windowUserState: StateFlow<Boolean> = windowUser

    fun openCloseWindowUser(boolean: Boolean){
        windowUser.value = boolean
    }

    fun deleteTask(id: String){
        val taskListHolder = taskList.value
        taskListHolder.remove(id)
        TaskStorage.updateTaskList(taskListHolder)
        TaskStorage.addNewIgnoredId(id)
        taskList.value = taskListHolder
        keyList.value = taskListHolder.keys.toMutableList()
    }




    fun updateTaskTile(string: String){
        taskEdit.update {
            it.copy(title = string)
        }
    }

    fun updateTaskText(string: String){
        taskEdit.update {
            it.copy(text = string)
        }

    }

    fun updateEditId(string: String){
        editId.value = string
    }

    fun saveEditTask(id: String, context: Context){
        val taskListHolder = taskList.value
        val instant = Instant.now().toEpochMilli()
        val userId = getStoredDeviceUUID(context)
        val newTask = Task(taskEdit.value.title, taskEdit.value.text,
            EditInfo(
                userId = userId,
                timeStamp = instant
            ))
        taskListHolder[id] = newTask
        TaskStorage.updateTaskList(taskListHolder)
        taskList.value = taskListHolder
        keyList.value = taskListHolder.keys.toMutableList()
    }
    private fun getStoredDeviceUUID(context: Context): String {
        val  id = getUUID(context)
        return if (id != null) {
            id
        } else {
            saveUUID(context)
            getUUID(context)!!
        }

    }

    private fun saveUUID(context: Context){
        val newId = UUID.randomUUID().toString()
        val prefs = context.getSharedPreferences("userID", Context.MODE_PRIVATE)
        with(prefs.edit()) {
            putString("ID", newId)
            apply()
        }

    }


    private fun getUUID(context: Context): String?{
        val prefs = context.getSharedPreferences("userID", Context.MODE_PRIVATE)
        return prefs.getString("ID", null)
    }


    fun openCloseDialogEdit(boolean: Boolean){
        dialogEdit.value = boolean
    }

    fun takeData(string: String): Task? {
        return taskList.value[string]
    }

    fun addNewTask(title: String, text: String, context: Context){
        val taskListHolder = taskList.value
        val id = IdGenerator().idGenerator(context)
        val task = Task(title, text)
        taskListHolder[id] = task
        TaskStorage.updateTaskList(taskListHolder)
        taskList.value = taskListHolder
        keyList.value = taskListHolder.keys.toMutableList()


    }

    fun updateList(){
        taskList.value = TaskStorage.taskListState.value
        keyList.value = TaskStorage.taskListState.value.keys.toMutableList()
    }


}