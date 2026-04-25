package pl.edu.pjwstk.engineeringthesis.modules

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.dao.GsrSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.HearthRateSampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.SpO2SampleDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.UserProfileDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.TempSampleDao

private const val DEFAULT_TEMPERATURE_LOW_SQL = 36.5
private const val DEFAULT_TEMPERATURE_HIGH_SQL = 37.3
private const val DEFAULT_TEMPERATURE_OFFSET_C_SQL = 0.0
private const val DEFAULT_HEART_RATE_LOW_SQL = 60.0
private const val DEFAULT_HEART_RATE_HIGH_SQL = 100.0
private const val DEFAULT_SPO2_LOW_SQL = 97.0
private const val DEFAULT_SPO2_HIGH_SQL = 100.0
private const val DEFAULT_SKIN_CONDUCTANCE_LOW_SQL = 5.0
private const val DEFAULT_SKIN_CONDUCTANCE_HIGH_SQL = 15.0

private val MIGRATION_11_12 = object : Migration(11, 12) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN weightKg INTEGER NOT NULL DEFAULT 0"
        )
    }
}

private val MIGRATION_12_13 = object : Migration(12, 13) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS user_profile_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                gender TEXT NOT NULL,
                birthDateEpochDays INTEGER NOT NULL,
                heightCm INTEGER NOT NULL,
                weightKg REAL NOT NULL DEFAULT 0,
                isActive INTEGER NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO user_profile_new (id, name, gender, birthDateEpochDays, heightCm, weightKg, isActive)
            SELECT id, name, gender, birthDateEpochDays, heightCm, CAST(weightKg AS REAL), isActive
            FROM user_profile
            """.trimIndent()
        )
        database.execSQL("DROP TABLE user_profile")
        database.execSQL("ALTER TABLE user_profile_new RENAME TO user_profile")
    }
}

private val MIGRATION_13_14 = object : Migration(13, 14) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS gsr_sample_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                userId INTEGER NOT NULL,
                epoch INTEGER NOT NULL,
                gsr REAL NOT NULL
            )
            """.trimIndent()
        )
        database.execSQL(
            """
            INSERT INTO gsr_sample_new (id, userId, epoch, gsr)
            SELECT id, userId, epoch, CAST(gsr AS REAL)
            FROM gsr_sample
            """.trimIndent()
        )
        database.execSQL("DROP TABLE gsr_sample")
        database.execSQL("ALTER TABLE gsr_sample_new RENAME TO gsr_sample")
    }
}

private val MIGRATION_15_16 = object : Migration(15, 16) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN temperatureNormalLow REAL NOT NULL DEFAULT $DEFAULT_TEMPERATURE_LOW_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN temperatureNormalHigh REAL NOT NULL DEFAULT $DEFAULT_TEMPERATURE_HIGH_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN heartRateNormalLow REAL NOT NULL DEFAULT $DEFAULT_HEART_RATE_LOW_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN heartRateNormalHigh REAL NOT NULL DEFAULT $DEFAULT_HEART_RATE_HIGH_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN skinConductanceNormalLow REAL NOT NULL DEFAULT $DEFAULT_SKIN_CONDUCTANCE_LOW_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN skinConductanceNormalHigh REAL NOT NULL DEFAULT $DEFAULT_SKIN_CONDUCTANCE_HIGH_SQL"
        )
    }
}

private val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN spO2NormalLow REAL NOT NULL DEFAULT $DEFAULT_SPO2_LOW_SQL"
        )
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN spO2NormalHigh REAL NOT NULL DEFAULT $DEFAULT_SPO2_HIGH_SQL"
        )
    }
}

private val MIGRATION_19_20 = object : Migration(19, 20) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE user_profile ADD COLUMN temperatureOffsetC REAL NOT NULL DEFAULT $DEFAULT_TEMPERATURE_OFFSET_C_SQL"
        )
    }
}

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): DiaryDB =
        Room.databaseBuilder(ctx, DiaryDB::class.java, "diary.db")
            .addMigrations(
                MIGRATION_11_12,
                MIGRATION_12_13,
                MIGRATION_13_14,
                MIGRATION_15_16,
                MIGRATION_18_19,
                MIGRATION_19_20
            )
            .fallbackToDestructiveMigration(false)
            .build()

    @Provides fun provideUserProfileDao(db: DiaryDB): UserProfileDao = db.userProfileDao
    @Provides fun provideTempSampleDao(db: DiaryDB): TempSampleDao = db.tempSamples
    @Provides fun provideGsrSampleDao(diaryDB: DiaryDB): GsrSampleDao = diaryDB.gsrSamples
    @Provides fun provideHearthRateSampleDao(db: DiaryDB): HearthRateSampleDao = db.hearthRateSamples
    @Provides fun provideSpO2SampleDao(db: DiaryDB): SpO2SampleDao = db.spO2Samples


}
