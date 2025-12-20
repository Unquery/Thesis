package pl.edu.pjwstk.engineeringthesis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.edu.pjwstk.engineeringthesis.data.db.dao.GsrSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.TempSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.UserProfileDao
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

@Database(
    entities = [TempSampleEntity::class, GsrSampleEntity::class, UserProfileEntity::class],
    version = 3
)
abstract class DiaryDB : RoomDatabase() {

    abstract val tempSamples : TempSampleDao

    abstract val gsrSamples : GsrSampleDao

    abstract val userProfileDao: UserProfileDao

}