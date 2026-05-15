package com.signlink.data.repository

import com.signlink.data.local.dao.DocumentDao
import com.signlink.data.local.entity.DocumentEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentRepository @Inject constructor(
    private val documentDao: DocumentDao
) {
    fun getAllDocuments(): Flow<List<DocumentEntity>> = documentDao.getAllDocuments()

    fun getDocumentsByType(type: String): Flow<List<DocumentEntity>> = documentDao.getDocumentsByType(type)

    suspend fun getDocumentById(id: Int): DocumentEntity? = documentDao.getDocumentById(id)

    suspend fun insertDocument(document: DocumentEntity) = documentDao.insertDocument(document)

    suspend fun deleteDocument(document: DocumentEntity) = documentDao.deleteDocument(document)
}
