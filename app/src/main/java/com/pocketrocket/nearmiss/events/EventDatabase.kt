package com.pocketrocket.nearmiss.events

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "near_miss_events")
data class NearMissEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val timestampMs: Long,
    val lat: Double?,
    val lon: Double?,
    val minTtc: Float,
    val objectClass: String,
    val brakeG: Float?,
    val yawRate: Float?,
    val confirmed: Boolean
)

@Dao
interface EventDao {
    @Insert
    suspend fun insertEvent(event: NearMissEvent)

    @Query("SELECT * FROM near_miss_events ORDER BY timestampMs DESC")
    fun getAllEvents(): Flow<List<NearMissEvent>>
    
    @Query("SELECT * FROM near_miss_events ORDER BY timestampMs DESC")
    suspend fun getAllEventsSync(): List<NearMissEvent>
}

@Database(entities = [NearMissEvent::class], version = 1, exportSchema = false)
abstract class EventDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: EventDatabase? = null

        fun getDatabase(context: Context): EventDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EventDatabase::class.java,
                    "near_miss_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
