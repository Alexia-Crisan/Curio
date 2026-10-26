package com.example.curio.domain

object TreeSelection {
    fun resolve(trees: List<ThemeTree>, manual: String?, current: String?): String? =
        listOf(manual, current).firstOrNull { id -> id != null && trees.any { it.theme.id == id } }
            ?: trees.firstOrNull()?.theme?.id
}
