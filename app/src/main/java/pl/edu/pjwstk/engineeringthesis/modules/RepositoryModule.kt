package pl.edu.pjwstk.engineeringthesis.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.GsrSampleRepositoryImpl
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.HearthRateSampleRepositoryImpl
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepositoryImpl
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.SpO2SampleRepositoryImpl
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.TempSampleRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository

    @Binds
    @Singleton
    abstract fun bindSpO2SampleRepository(
        impl: SpO2SampleRepositoryImpl
    ): SpO2SampleRepository

    @Binds
    @Singleton
    abstract fun bindHearthRateSampleRepository(
        impl: HearthRateSampleRepositoryImpl
    ): HearthRateSampleRepository

    @Binds
    @Singleton
    abstract fun bindTempSampleRepository(
        impl: TempSampleRepositoryImpl
    ): TempSampleRepository

    @Binds
    @Singleton
    abstract fun bindGsrSampleRepository(
        impl: GsrSampleRepositoryImpl
    ): GsrSampleRepository
}