package com.example.curio.data.fake

import com.example.curio.domain.ContentType
import com.example.curio.domain.Discovery
import com.example.curio.domain.DiscoveryRepository
import com.example.curio.domain.Quiz
import com.example.curio.domain.Source
import kotlinx.coroutines.delay
import java.time.LocalDate

class FakeDiscoveryRepository : DiscoveryRepository {

    private val smithsonian = Source("Smithsonian", "https://www.si.edu", "CC0")
    private val nasa = Source("NASA", "https://science.nasa.gov/mars/", "Public Domain")

    override suspend fun getDailyFeed(date: LocalDate): List<Discovery> {
        delay(600) // simulate loading so skeletons/indicators appear
        return listOf(
            Discovery(
                id = "fake.1",
                themeId = "animals",
                subcategoryId = "penguins",
                type = ContentType.FACT,
                title = "Penguins can drink seawater.",
                body = "A gland above their eyes filters out salt from their bloodstream.",
                source = smithsonian,
                accentColor = 0xFF38BDF8
            ),
            Discovery(
                id = "fake.2",
                themeId = "animals",
                subcategoryId = "penguins",
                type = ContentType.ARTICLE,
                title = "Why don't penguins' feet freeze?",
                body = "Warm blood flowing down into their feet meets cold blood flowing back up. Heat is exchanged before it reaches the ice.",
                source = smithsonian,
                accentColor = 0xFF0EA5E9
            ),
            Discovery(
                id = "fake.3",
                themeId = "animals",
                subcategoryId = "penguins",
                type = ContentType.QUIZ,
                title = "Test yourself",
                quiz = Quiz(
                    question = "How deep can an emperor penguin dive?",
                    options = listOf("50 m", "150 m", "500 m", "1000 m"),
                    correctIndex = 2,
                    explanation = "They can dive over 500 meters (1,600 feet) to hunt for fish!"
                ),
                source = smithsonian,
                accentColor = 0xFF6366F1
            ),
            Discovery(
                id = "fake.4",
                themeId = "space",
                subcategoryId = "mars",
                type = ContentType.FACT,
                title = "A day on Mars is 24 hours and 39 minutes.",
                body = "Just 37 minutes longer than an Earth day. A Martian year, however, is nearly twice as long at 687 days.",
                source = nasa,
                accentColor = 0xFFEA580C
            ),
            Discovery(
                id = "fake.5",
                themeId = "space",
                subcategoryId = "mars",
                type = ContentType.FACT,
                title = "Sunsets on Mars appear blue.",
                body = "Fine dust in the Martian atmosphere absorbs red wavelengths and scatters blue light toward the observer.",
                source = nasa,
                accentColor = 0xFF10B981
            )
        )
    }
}
