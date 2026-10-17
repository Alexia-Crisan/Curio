package com.example.curio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curio.feature.feed.FeedScreen
import com.example.curio.feature.feed.FeedViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as CurioApp).container
        setContent {
            MaterialTheme {
                val vm: FeedViewModel = viewModel(
                    factory = FeedViewModel.Factory(
                        container.discoveryRepository,
                        container.interactionRepository,
                        container.clock,
                    )
                )
                FeedScreen(vm)
            }
        }
    }
}
