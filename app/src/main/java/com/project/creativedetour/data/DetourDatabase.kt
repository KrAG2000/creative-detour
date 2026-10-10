package com.project.creativedetour.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Feedback::class], version = 3, exportSchema = false)
abstract class DetourDatabase : RoomDatabase() {
    abstract fun feedbackDao(): FeedbackDao

    companion object {
        fun create(context: Context): DetourDatabase =
            Room.databaseBuilder(context, DetourDatabase::class.java, "detour.db")
                // Pre-release: schema changes just reset the (test) history. Add real migrations before v1.0.
                .fallbackToDestructiveMigration(dropAllTables = true)
                .build()
    }
}
