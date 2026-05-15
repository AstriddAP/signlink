package com.signlink.data.repository

import com.signlink.data.local.dao.NoteDao
import com.signlink.data.local.entity.NoteEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao
) {
    fun getAllNotes(): Flow<List<NoteEntity>> = noteDao.getAllNotes()

    fun getNotesByType(type: String): Flow<List<NoteEntity>> = noteDao.getNotesByType(type)

    suspend fun insertNote(note: NoteEntity) = noteDao.insertNote(note)

    suspend fun deleteNote(note: NoteEntity) = noteDao.deleteNote(note)

    suspend fun getNoteById(id: Int): NoteEntity? = noteDao.getNoteById(id)
}
