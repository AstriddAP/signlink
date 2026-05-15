package com.signlink.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.signlink.data.local.dao.DocumentDao
import com.signlink.data.local.dao.NoteDao
import com.signlink.data.local.entity.DocumentEntity
import com.signlink.data.local.entity.NoteEntity

@Database(entities = [DocumentEntity::class, NoteEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun noteDao(): NoteDao
}
