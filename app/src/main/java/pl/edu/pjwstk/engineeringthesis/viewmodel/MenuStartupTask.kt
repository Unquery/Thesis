package pl.edu.pjwstk.engineeringthesis.viewmodel

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.multibindings.Multibinds

fun interface MenuStartupTask {
    suspend fun run()
}

@Module
@InstallIn(ViewModelComponent::class)
interface MenuStartupTaskModule {
    @Multibinds
    fun bindMenuStartupTasks(): Set<MenuStartupTask>
}
