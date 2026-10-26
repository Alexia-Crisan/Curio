package com.example.curio.data

import org.junit.Assume.assumeTrue
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class RoomSchemaExportTest {
    private fun schemaFile(): File? =
        File("schemas").walkTopDown().firstOrNull { it.isFile && it.name == "1.json" }
            ?: File("app/schemas").walkTopDown().firstOrNull { it.isFile && it.name == "1.json" }
            ?: File("../schemas").walkTopDown().firstOrNull { it.isFile && it.name == "1.json" }

    @Test fun theExportedSchemaExistsAndIsVersionOne() {
        val file = schemaFile()
        assumeTrue("Schema export not enabled / file not present", file != null && file.exists())
        val text = file!!.readText()
        assertTrue(text.contains("\"version\": 1"))
        assertTrue(text.contains("\"identityHash\""))
    }

    @Test fun theExportedSchemaContainsEveryTable() {
        val file = schemaFile()
        assumeTrue("Schema export not enabled / file not present", file != null && file.exists())
        val text = file!!.readText()
        listOf("view_event", "day_choice", "saved_discovery", "achievement_unlock").forEach {
            assertTrue("missing table $it", text.contains("\"tableName\": \"$it\""))
        }
    }

    @Test fun viewEventHasTheSourceAndDetailDwellColumns() {
        val file = schemaFile()
        assumeTrue("Schema export not enabled / file not present", file != null && file.exists())
        val text = file!!.readText()
        listOf("source", "detailDwellMs").forEach { assertTrue(text.contains("\"columnName\": \"$it\"")) }
    }
}
