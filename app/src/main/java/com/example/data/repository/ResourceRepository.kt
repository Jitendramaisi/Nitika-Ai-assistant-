package com.example.data.repository

import com.example.data.database.ResourceDao
import com.example.data.database.ResourceEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ResourceRepository(private val resourceDao: ResourceDao) {
    val allResources: Flow<List<ResourceEntity>> = resourceDao.getAllResourcesFlow()

    suspend fun addResource(title: String, url: String, category: String, description: String) = withContext(Dispatchers.IO) {
        val resource = ResourceEntity(
            title = title,
            url = url,
            category = category,
            description = description
        )
        resourceDao.insertResource(resource)
    }

    suspend fun deleteResource(id: Int) = withContext(Dispatchers.IO) {
        resourceDao.deleteResource(id)
    }
}
