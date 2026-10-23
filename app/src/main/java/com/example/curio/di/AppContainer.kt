package com.example.curio.di

import android.content.Context
import androidx.room.Room
import com.example.curio.data.RealClock
import com.example.curio.data.fake.FakeDiscoveryRepository
import com.example.curio.data.fake.FakeInteractionRepository
import com.example.curio.data.settings.SettingsRepository
import com.example.curio.data.user.CurioDatabase
import com.example.curio.data.user.UserActivityRepository
import com.example.curio.domain.Clock
import com.example.curio.domain.ContentIndex
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.InteractionRepository
import com.example.curio.domain.TestContent
import com.example.curio.hardware.HapticsController
import com.example.curio.hardware.ShakeSensor
import com.example.curio.hardware.TiltSensor

class AppContainer(context: Context) {
    val clock: Clock = RealClock()
    val discoveryRepository: DiscoveryRepository = FakeDiscoveryRepository()
    val interactionRepository: InteractionRepository = FakeInteractionRepository()
    val settingsRepository: SettingsRepository = SettingsRepository(context)
    val shakeSensor: ShakeSensor = ShakeSensor(context)
    val tiltSensor: TiltSensor = TiltSensor(context)
    val hapticsController: HapticsController = HapticsController()

    val database: CurioDatabase = Room.databaseBuilder(
        context.applicationContext,
        CurioDatabase::class.java,
        "curio.db",
    ).fallbackToDestructiveMigration().build()

    val contentIndex: ContentIndex = TestContent.index

    val userActivityRepository: UserActivityRepository = UserActivityRepository(
        viewEventDao = database.viewEventDao(),
        savedIdsFlow = interactionRepository.observeSavedIds(),
        contentIndex = contentIndex,
    )
}
