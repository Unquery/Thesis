package pl.edu.pjwstk.engineeringthesis.modules

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepository
import pl.edu.pjwstk.engineeringthesis.data.repository.ProfileRepositoryImpl
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): ProfileRepository
}