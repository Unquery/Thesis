package pl.edu.pjwstk.engineeringthesis.modules

import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import pl.edu.pjwstk.engineeringthesis.data.db.DiaryDB
import pl.edu.pjwstk.engineeringthesis.data.db.dao.UserProfileDao
import pl.edu.pjwstk.engineeringthesis.data.db.dao.TempSampleDao

@Module
@InstallIn(SingletonComponent::class)
object DbModule {

    @Provides
    @Singleton
    fun provideDb(@ApplicationContext ctx: Context): DiaryDB =
        Room.databaseBuilder(ctx, DiaryDB::class.java, "diary.db")
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideUserProfileDao(db: DiaryDB): UserProfileDao = db.userProfileDao
    @Provides fun provideTempSampleDao(db: DiaryDB): TempSampleDao = db.tempSamples
}