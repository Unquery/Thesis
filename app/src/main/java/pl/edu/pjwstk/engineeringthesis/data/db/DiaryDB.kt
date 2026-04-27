package pl.edu.pjwstk.engineeringthesis.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import pl.edu.pjwstk.engineeringthesis.data.db.dao.GsrSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.HearthRateSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.MeasurementPacketDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.SpO2SampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.TempSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.UserProfileDao
import pl.edu.pjwstk.engineeringthesis.data.db.entity.GsrSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.HearthRateSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.MeasurementPacketEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.SpO2SampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.TempSampleEntity
import pl.edu.pjwstk.engineeringthesis.data.db.entity.UserProfileEntity

@Database(
    entities = [
        TempSampleEntity::class,
        GsrSampleEntity::class,
        UserProfileEntity::class,
        HearthRateSampleEntity::class,
        SpO2SampleEntity::class,
        MeasurementPacketEntity::class
    ],
    version = 22
)
abstract class DiaryDB : RoomDatabase() {

    abstract val tempSamples : TempSampleDao

    abstract val gsrSamples : GsrSampleDao

    abstract val hearthRateSamples : HearthRateSampleDao

    abstract val spO2Samples : SpO2SampleDao

    abstract val measurementPackets: MeasurementPacketDao

    abstract val userProfileDao: UserProfileDao

}
