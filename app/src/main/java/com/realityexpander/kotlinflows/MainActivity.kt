package com.realityexpander.kotlinflows

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
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

// Source material
//https://www.youtube.com/watch?v=za-EEkqJLCQ

// Testing flows
// https://www.youtube.com/watch?v=rk6aKkWqqcI

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        collectLatestLifecycleFlow(viewModel.stateFlow) { number ->
            delay(200)
            println("collectLatestLifecycleFlow stateFlow: $number")
        }

        collectLifecycleFlow(viewModel.stateFlow) { number ->
            delay(200)
            println("collectLifecycleFlow stateFlow: $number")
        }

        setContent {
            KotlinFlowsTheme {
                val viewModel = viewModel<MainViewModel>()

                val countFromStateFlow = viewModel.stateFlow.collectAsState(initial = 0)
                val countFromSharedFlow = viewModel.sharedFlow.collectAsState(initial = 0)
                var sharedFlowReplayCacheSize by remember { mutableStateOf(0) }
                var sharedFlowReplayCacheValues by remember { mutableStateOf("") }

                // This will collect one-time events from sharedFlow
                LaunchedEffect(key1 = true) {
                    //delay(100)  // if the delay is here, the sharedFlow collect will not be called (sharedFlow is a hot flow)
                    println("LaunchedEffect setup sharedFlow collector")

                    viewModel.sharedFlow.collect { number ->
                        println("sharedFlow collect in LaunchedEffect: $number")
                        sharedFlowReplayCacheSize = viewModel.sharedFlow.replayCache.size
                        sharedFlowReplayCacheValues = viewModel.sharedFlow.replayCache.joinToString { it.toString() }
                    }
                }

                Column(modifier = Modifier.fillMaxSize()) {
                    Button(onClick = { viewModel.incrementCounterStateFlow() }) {
                        Text(text = "stateFlow Counter: ${countFromStateFlow.value}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Button(onClick = { viewModel.incrementCounterSharedFlow() }) {
                        Text(text = "sharedFlow last value: ${countFromSharedFlow.value}")
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("sharedFlow Cache\n" +
                            " Size: $sharedFlowReplayCacheSize\n" +
                            " Values: $sharedFlowReplayCacheValues",
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colors.onBackground
                    )
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