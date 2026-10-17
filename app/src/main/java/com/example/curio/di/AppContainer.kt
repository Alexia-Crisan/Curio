package com.example.curio.di

import com.example.curio.data.RealClock
import com.example.curio.data.fake.FakeDiscoveryRepository
import com.example.curio.data.fake.FakeInteractionRepository
import com.example.curio.domain.Clock
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.InteractionRepository

class AppContainer {
    val clock: Clock = RealClock()
    val discoveryRepository: DiscoveryRepository = FakeDiscoveryRepository()
    val interactionRepository: InteractionRepository = FakeInteractionRepository()
}
