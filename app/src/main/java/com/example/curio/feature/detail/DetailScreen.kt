package com.example.curio.feature.detail

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curio.R
import com.example.curio.di.AppContainer
import com.example.curio.ui.theme.Spacing

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    container: AppContainer,
    id: String,
    onBack: () -> Unit,
) {
    val vm: DetailViewModel = viewModel(factory = DetailViewModel.factory(container, id))
    val discovery by vm.discovery.collectAsStateWithLifecycle()
    val isSaved by vm.isSaved.collectAsStateWithLifecycle()
    val note by vm.note.collectAsStateWithLifecycle()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> vm.onVisible()
                Lifecycle.Event.ON_PAUSE -> vm.onHidden()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            vm.onHidden()
        }
    }

    val d = discovery ?: return

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(d.title, style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
                actions = {
                    IconButton(onClick = vm::toggleSave) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = if (isSaved) "Unsave" else "Save",
                            tint = if (isSaved) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                        )
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.xl),
        ) {
            Text(d.title, style = MaterialTheme.typography.headlineMedium)
            d.body?.let { body ->
                Spacer(Modifier.height(Spacing.md))
                Text(body, style = MaterialTheme.typography.bodyLarge)
            }
            if (isSaved) {
                Spacer(Modifier.height(Spacing.lg))
                NoteSection(note = note, onSave = vm::saveNote)
            }
            Spacer(Modifier.height(Spacing.lg))
            Text(
                text = "${d.source.name} (${d.source.license})",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
