package com.example.curio.di

import android.content.Context
import com.example.curio.data.RealClock
import com.example.curio.data.fake.FakeDiscoveryRepository
import com.example.curio.data.fake.FakeInteractionRepository
import com.example.curio.domain.Clock
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.InteractionRepository
import com.example.curio.hardware.HapticsController
import com.example.curio.hardware.ShakeSensor
import com.example.curio.hardware.TiltSensor

class AppContainer(context: Context) {
    val clock: Clock = RealClock()
    val discoveryRepository: DiscoveryRepository = FakeDiscoveryRepository()
    val interactionRepository: InteractionRepository = FakeInteractionRepository()
    val shakeSensor: ShakeSensor = ShakeSensor(context)
    val tiltSensor: TiltSensor = TiltSensor(context)
    val hapticsController: HapticsController = HapticsController()
}
