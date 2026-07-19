package com.example.data.database

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_messages")
data class MessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "user" or "professor"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    
    // Quiz related fields (optional)
    val isQuiz: Boolean = false,
    val quizQuestion: String? = null,
    val quizOptionsJson: String? = null, // JSON list of options e.g. ["Option A", "Option B"]
    val quizCorrectIndex: Int = -1,
    val quizExplanation: String? = null,
    val quizSelectedAnswer: Int = -1 // -1 means unanswered, otherwise 0..3
)

@Entity(tableName = "academic_resources")
data class ResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val url: String, // Can be URL or generic text/document info
    val category: String,
    val description: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessagesFlow(): Flow<List<MessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity): Long

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Query("DELETE FROM chat_messages")
    suspend fun clearHistory()
}

@Dao
interface ResourceDao {
    @Query("SELECT * FROM academic_resources ORDER BY timestamp DESC")
    fun getAllResourcesFlow(): Flow<List<ResourceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: ResourceEntity): Long

    @Query("DELETE FROM academic_resources WHERE id = :resourceId")
    suspend fun deleteResource(resourceId: Int)
}

@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val durationMinutes: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "quiz_performance")
data class QuizPerformanceEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String,
    val scorePercentage: Float,
    val timestamp: Long = System.currentTimeMillis()
)

@Dao
interface TrackingDao {
    @Query("SELECT * FROM study_sessions ORDER BY timestamp ASC")
    fun getAllStudySessionsFlow(): Flow<List<StudySessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStudySession(session: StudySessionEntity)

    @Query("SELECT * FROM quiz_performance ORDER BY timestamp ASC")
    fun getAllQuizPerformanceFlow(): Flow<List<QuizPerformanceEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuizPerformance(performance: QuizPerformanceEntity)
}

@Database(entities = [MessageEntity::class, ResourceEntity::class, StudySessionEntity::class, QuizPerformanceEntity::class], version = 3, exportSchema = false)
abstract class ChatDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun resourceDao(): ResourceDao
    abstract fun trackingDao(): TrackingDao

    companion object {
        @Volatile
        private var INSTANCE: ChatDatabase? = null

        fun getDatabase(context: Context): ChatDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ChatDatabase::class.java,
                    "nitika_chat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
