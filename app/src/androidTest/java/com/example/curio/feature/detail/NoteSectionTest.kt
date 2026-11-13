package com.example.curio.feature.detail

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import com.example.curio.R
import com.example.curio.domain.NoteRules
import com.example.curio.ui.theme.CurioTheme

class NoteSectionTest {
    @get:Rule
    val rule = createComposeRule()

    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun addingANoteShowsItAndOffersEditing() {
        var note by mutableStateOf<String?>(null)
        rule.setContent {
            CurioTheme { NoteSection(note = note, onSave = { note = NoteRules.normalize(it) }) }
        }
        rule.onNodeWithText(ctx.getString(R.string.note_add)).performClick()
        rule.onNodeWithTag("note_field").performTextInput("Ask the teacher")
        rule.onNodeWithText(ctx.getString(R.string.note_save)).performClick()
        rule.onNodeWithTag("saved_note").assertTextEquals("Ask the teacher")
        rule.onNodeWithText(ctx.getString(R.string.note_edit)).assertExists()
    }

    @Test
    fun editingStartsFromTheCurrentText() {
        var note by mutableStateOf<String?>("old text")
        rule.setContent {
            CurioTheme { NoteSection(note = note, onSave = { note = NoteRules.normalize(it) }) }
        }
        rule.onNodeWithText(ctx.getString(R.string.note_edit)).performClick()
        rule.onNodeWithTag("note_field").assertTextContains("old text")
    }

    @Test
    fun cancellingKeepsTheNoteUnchanged() {
        var note by mutableStateOf<String?>("keep")
        rule.setContent {
            CurioTheme { NoteSection(note = note, onSave = { note = NoteRules.normalize(it) }) }
        }
        rule.onNodeWithText(ctx.getString(R.string.note_edit)).performClick()
        rule.onNodeWithTag("note_field").performTextInput(" and more")
        rule.onNodeWithText(ctx.getString(R.string.note_cancel)).performClick()
        rule.onNodeWithTag("saved_note").assertTextEquals("keep")
    }

    @Test
    fun theCounterShowsHowManyCharactersAreUsed() {
        var note by mutableStateOf<String?>(null)
        rule.setContent {
            CurioTheme { NoteSection(note = note, onSave = { note = NoteRules.normalize(it) }) }
        }
        rule.onNodeWithText(ctx.getString(R.string.note_add)).performClick()
        rule.onNodeWithTag("note_field").performTextInput("hello")
        rule.onNodeWithText("5/280").assertExists()
    }
}
