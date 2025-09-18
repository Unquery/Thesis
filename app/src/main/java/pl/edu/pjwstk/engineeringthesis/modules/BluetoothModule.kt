package pl.edu.pjwstk.engineeringthesis.modules

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object BluetoothModule {

    @Provides
    fun provideBluetoothManager(
        @ApplicationContext context: Context
    ): BluetoothManager = context.getSystemService(BluetoothManager::class.java)

    @Provides
    fun provideBluetoothAdapter(
        manager: BluetoothManager
    ): BluetoothAdapter = manager.adapter
}
