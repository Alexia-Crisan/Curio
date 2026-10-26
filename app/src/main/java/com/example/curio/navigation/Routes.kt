package com.example.curio.navigation

import kotlinx.serialization.Serializable

sealed interface Route {
    @Serializable
    data object Onboarding : Route

    @Serializable
    data object Feed : Route

    @Serializable
    data object Library : Route

    @Serializable
    data object Progress : Route

    @Serializable
    data object Settings : Route

    @Serializable
    data object Achievements : Route

    @Serializable
    data object Sources : Route

    @Serializable
    data class Tree(val themeId: String? = null) : Route

    @Serializable
    data class Detail(val discoveryId: String) : Route
}
