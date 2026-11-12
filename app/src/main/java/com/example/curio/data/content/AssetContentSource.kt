package com.example.curio.data.content

import android.content.Context
import com.example.curio.domain.ContentFile
import com.example.curio.domain.ContentLoadResult
import com.example.curio.domain.ContentLoader

class AssetContentSource(private val context: Context) {
    fun load(): ContentLoadResult {
        val assets = context.assets
        val names = assets.list(DIR).orEmpty().filter { it.endsWith(".json") }.sorted()
        val files = names.map { name ->
            ContentFile(name, assets.open("$DIR/$name").bufferedReader(Charsets.UTF_8).use { it.readText() })
        }
        return ContentLoader.load(files)
    }

    private companion object {
        const val DIR = "content"
    }
}
