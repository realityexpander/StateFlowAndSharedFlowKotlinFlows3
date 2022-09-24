package com.realityexpander.kotlinflows

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(
    private val dispatchers: DispatcherProvider = DefaultDispatchers()
) : ViewModel() {

    val countDownFlow = flow<Int> {
        val startingValue = 5
        var currentValue = startingValue

        emit(startingValue)

        while (currentValue > 0) {
            delay(1000L)
            currentValue--
            emit(currentValue)
        }
    }.flowOn(dispatchers.main)

    // Keep the latest value as state.
    private val _stateFlow = MutableStateFlow(0)
    val stateFlow = _stateFlow.asStateFlow()

    // Used for one-time events. (can have multiple observers/collectors/subscribers unlike channels which can only have one)
    // Hot flow, so without a collector, the emission is lost.
    private val _sharedFlow =
        MutableSharedFlow<Int>(
            replay = 0 // Use NO PARAMETERS to send one-time messages that will not be replayed to new subscribers (but throttled by consumer)

            //replay = 1,  // use "replay=1, DROP_OLDEST" to send messages that will be replayed for each new subscriber.

            //replay = 5, // Collects 5 items until buffering (throttle)
            //extraBufferCapacity = 5,  // will not start buffering (throttle) until 5 more items come in (after replay buffer is full)
            //onBufferOverflow = BufferOverflow.DROP_LATEST   // when buffer is full, drop the latest item.
            //onBufferOverflow = BufferOverflow.DROP_OLDEST // when buffer is full, drop the oldest item.
        )
    val sharedFlow = _sharedFlow.asSharedFlow()

    var countForSharedFlow = 0

    init {
        incrementCounterSharedFlow() // no collectors set up yet, so this will be emission will lost. (if replay is 0)

        // This will throttle the flow to 1 item per 500ms (unless onBufferOverflow is set to DROP_OLDEST)
        viewModelScope.launch(dispatchers.main) {
            sharedFlow.collect {
                println("FIRST FLOW sharedFlow: replay cache:" + sharedFlow.replayCache.joinToString { it.toString() })

                delay(500L)
                println("FIRST FLOW sharedFlow: The received number is $it")
            }
        }

        // This will throttle the flow to 1 item per 1500ms (unless onBufferOverflow is set to DROP_OLDEST)
        viewModelScope.launch(dispatchers.main) {
            delay(500) // hold off on setting up this collector for 500ms

            sharedFlow.collect {
                delay(1500L)
                println("SECOND FLOW sharedFlow: The received number is $it")
            }
        }

//        viewModelScope.launch {
//            println("STARTING COUNTDOWN, THREAD: ${Thread.currentThread().name}")
//            countDownFlow.collect {
//                println("CountDownFlow received number is $it")
//            }
//        }

        incrementCounterSharedFlow() // since collectors are setup now, this emission will be collected.
    }

    fun incrementCounterSharedFlow() {
        viewModelScope.launch(dispatchers.main) {
            println("incrementCounterSharedFlow, THREAD: ${Thread.currentThread().name}, emitting sharedFlow: ${++countForSharedFlow}")
            _sharedFlow.emit(countForSharedFlow)
        }
    }

    fun incrementCounterStateFlow() {
        _stateFlow.value += 1
    }

    private fun collectFlow() {
        val flow = flow {
            delay(250L)
            emit("Appetizer")
            delay(1000L)
            emit("Main dish")
            delay(100L)
            emit("Dessert")
        }

        viewModelScope.launch {
            flow.onEach {
                println("FLOW: $it is delivered")
            }
                .collectLatest {
                    println("FLOW: Now eating $it")
                    delay(1500L)
                    println("FLOW: Finished eating $it")
                }
        }
    }
}