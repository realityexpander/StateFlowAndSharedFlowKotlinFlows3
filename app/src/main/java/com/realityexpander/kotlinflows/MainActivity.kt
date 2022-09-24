package com.realityexpander.kotlinflows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.realityexpander.kotlinflows.ui.theme.KotlinFlowsTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectLatestLifecycleFlow(viewModel.stateFlow) { number ->
            delay(200)
            println("collectLatestLifecycleFlow: $number")
        }

        collectLifecycleFlow(viewModel.stateFlow) { number ->
            delay(200)
            println("collectLifecycleFlow: $number")
        }

        setContent {
            KotlinFlowsTheme {
                val viewModel = viewModel<MainViewModel>()
                val count = viewModel.stateFlow.collectAsState(initial = 0)

                val sharedCount = viewModel.sharedFlow.collectAsState(initial = 0)

                LaunchedEffect(key1 = true) {
                    viewModel.sharedFlow.collect { number ->
                        println("sharedFlow: $number")
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Button(onClick = { viewModel.incrementCounterStateFlow() }) {
                        Text(text = "stateFlow Counter: ${count.value}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { viewModel.setCounterSharedFlow(sharedCount.value + 1) }) {
                        Text(text = "sharedFlow value: ${sharedCount.value}")
                    }
                }
            }
        }
    }
}

// Collects only the latest emission of the flow, cancelling the previous collection.
fun <T> ComponentActivity.collectLatestLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collectLatest(collect)
        }
    }
}

// Collects each emission of the flow, but only while the lifecycle is in the STARTED state.
fun <T> ComponentActivity.collectLifecycleFlow(flow: Flow<T>, collect: suspend (T) -> Unit) {
    lifecycleScope.launch {
        repeatOnLifecycle(Lifecycle.State.STARTED) {
            flow.collect(collect)
        }
    }
}