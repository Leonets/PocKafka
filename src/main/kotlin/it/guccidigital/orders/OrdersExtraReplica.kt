package it.guccidigital.orders

import kotlinx.coroutines.*

fun main() {

    runBlocking {

        launch {
            //start a loop over the marketing queue
            managePriceAlerts()
        }
        launch {
            //start a loop to manage orders executions
            manageOrders()
        }
    }
}
