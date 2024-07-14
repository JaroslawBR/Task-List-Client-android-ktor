package com.example.tasklistclientktor

import android.content.Context
import java.time.Instant
import java.util.UUID

class IdGenerator {

        fun idGenerator(context: Context): String {
            val timeStamp = Instant.now().toEpochMilli()
            val uniqueID = getStoredDeviceUUID(context)
            return "$uniqueID-$timeStamp"
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
}