package com.example.curio.debug

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.curio.CurioApp

class DebugDemoActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val seeder = DemoDataSeeder((application as CurioApp).container)
        val automatic = DemoPreset.fromKey(intent.getStringExtra(EXTRA_PRESET))
        if (automatic != null) {
            lifecycleScope.launch {
                seeder.apply(automatic)
                finish()
            }
            return
        }
        setContent {
            MaterialTheme {
                val scope = rememberCoroutineScope()
                var status by remember { mutableStateOf("") }
                Column(
                    modifier = Modifier.fillMaxSize().systemBarsPadding().padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("Curio demo tools", style = MaterialTheme.typography.headlineSmall)
                    DemoPreset.values().forEach { preset ->
                        Button(
                            onClick = {
                                scope.launch {
                                    status = when (val r = seeder.apply(preset)) {
                                        is DemoDataSeeder.Result.Applied -> "Applied ${r.preset.key} (${r.touches} events)"
                                        is DemoDataSeeder.Result.Failed -> "Failed: ${r.reason}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        ) {
                            Text(preset.key)
                        }
                    }
                    Text(status)
                }
            }
        }
    }

    companion object {
        const val EXTRA_PRESET = "preset"
    }
}
