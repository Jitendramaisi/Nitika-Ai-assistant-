package com.example.data.repository

import com.example.data.database.QuizPerformanceEntity
import com.example.data.database.StudySessionEntity
import com.example.data.database.TrackingDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class TrackingRepository(private val trackingDao: TrackingDao) {
    val allStudySessions: Flow<List<StudySessionEntity>> = trackingDao.getAllStudySessionsFlow()
    val allQuizPerformance: Flow<List<QuizPerformanceEntity>> = trackingDao.getAllQuizPerformanceFlow()

    suspend fun addStudySession(subject: String, durationMinutes: Int) = withContext(Dispatchers.IO) {
        trackingDao.insertStudySession(StudySessionEntity(subject = subject, durationMinutes = durationMinutes))
    }

    suspend fun addQuizPerformance(subject: String, scorePercentage: Float) = withContext(Dispatchers.IO) {
        trackingDao.insertQuizPerformance(QuizPerformanceEntity(subject = subject, scorePercentage = scorePercentage))
    }
}
