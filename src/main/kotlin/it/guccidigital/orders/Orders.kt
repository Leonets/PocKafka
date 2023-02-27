package it.guccidigital.orders

import createBucketAws
import extractShippingDetailsAws
import getContentsAws
import it.guccidigital.*
import it.guccidigital.models.Order
import it.guccidigital.models.Orders
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.filter.DebuggingFilters
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import putObjectStreamAws
import java.io.File
import java.lang.Thread.currentThread
import java.lang.Thread.sleep

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)
    val server = printingApp.asServer(SunHttp(httpPort)).start()
    println("Server started on " + server.port())

    runBlocking {
        //create gucci bucket
        createBucketAws(bucketName)

        launch {
            //start a loop over the main input topic to forward to the subsequent queues
            manageOrdersTopic()
        }

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

//definizione delle routes
val app: HttpHandler = routes(
    //register some routes
    "/ping" bind GET to {
        Response(OK).body("pong")
    },

    //register some routes
    "/orders" bind Method.POST to {
        println(message = " payload  " + it.toMessage())
        //save payload in the bucket and then process it
        runBlocking {
            savePayload(it = it)
            //process means the single orders are sent to a topic for later processing
            process(it = it)
        }
        Response(OK).body("Orders received")
    },

    "/orders/pricing" bind Method.GET to {
        println(message = " extract and show current pricing policy " )
        Response(OK).body("TO BE CONSTRUCTED")
    },
    "/orders/shipping" bind Method.GET to {
        println(message = " extract and show shipping countries " )

        //operation:
        //1. extract object from S3 store (list<S3Object>)
        //2. create an object Orders by using the gson library
        //3. to create the Orders objects uses the key (it.key) to get the contents of the object inside the S3 Vault
        //4. by using the ordersList property it gets back the property (List<Order>)
        //5. by using the flatmap function it aggregates all the elements from single element transformation
        val result1 = extractShippingDetailsAws(bucketName).contents()
            ?.flatMap  {gson.fromJson(getContentsAws(bucketName,it.key()), Orders::class.java).ordersList }

        result1?.forEach { println("Order $it") }

        //operation: render country and price of the orders received
        Response(status = OK).body(result1?.joinToString { "Country = ${it.destinationCountry}  Price = ${it.price}  \n" } ?: "no data")
    },
    "/orders/dashboard" bind Method.GET to {
        println(message = " extract and show dashboard " )

        //operation, the same as shipping
        val result1 = extractShippingDetailsAws(bucketName).contents()
            ?.flatMap  {gson.fromJson(getContentsAws(bucketName,it.key()), Orders::class.java).ordersList }

        val output = result1?.groupingBy { it.destinationCountry }?.aggregate { key, accumulator: Int?, element, first
                -> if (first)
                        element.price
                    else
                        accumulator!!.plus(element.price)
        }
        //operation: render total amount of orders per country
        Response(status = OK).body(output?.entries?.joinToString { it.key + " = " + it.value }?: "no data")
    }
)


suspend fun savePayload(it: Request) {
    // extract the body from the message
    val extractedMessage: Orders = ordersLens(it)
    //sending data to the bucket
    val dataToSent: String = gson.toJson(extractedMessage)
    //put the payload in the bucket with a random key (json sintax)
    putObjectStreamAws(bucketName, randomStringByJavaRandom(), contents = dataToSent)
}

suspend fun process(it: Request) {
    // extract the body from the message
    val extractedMessage: Orders = ordersLens(it)
    //loop around and execute
    extractedMessage.ordersList.forEach { it ->
        //send to an internal topic (orders_topic)
        sendMessage(orders_topic, null, gson.toJson(it))
    }
}

suspend fun manageOrdersTopic() {
    kafkaConsumer.subscribe(listOf(orders_topic.toString()))
    while (true){
        try {
            delay(loopOrdersTiming) //non blocking
            //consume messages over the queue
            val messages = receiveMessage(kafkaConsumer)
            println(" \n Orders are arriving and need to be managed " )
            messages?.map {
                singleMessage ->
                    sendMessage(queueMarketingUrl,null,singleMessage.value())
                    sendMessage(queueShippingUrl,null,singleMessage.value())
                    sendMessage(queueOrderUrl,null,singleMessage.value())
                    sendMessage(queueAccountingUrl,null,singleMessage.value())
            }
        } catch (ex: Exception) {
            println("${currentThread().name} failed with {$ex}. Retrying...")
        }
        println(" Order topic has been managed \n")
    }
}


fun manageOrders() {
    internalKafkaAccountingConsumer.subscribe(listOf(queueAccountingUrl, queueErrorUrl))
    internalKafkaOrderConsumer.subscribe(listOf(queueOrderUrl))
    internalKafkaShippingConsumer.subscribe(listOf(queueShippingUrl))
        while (true) {
            try {
                //consume messages over the queues
                manageQueue(internalKafkaAccountingConsumer, "Accounting")
                manageQueue(internalKafkaOrderConsumer, "Order")
                manageQueue(internalKafkaShippingConsumer, "Shipping")

                sleep(loopExternalTiming) //non blocking
            } catch (ex: Exception) {
                println("${currentThread().name} failed with {$ex}. Retrying...")
            }
            println(" >>> Some orders have been managed \n")
        }
}

private fun manageQueue(topicConsumer: KafkaConsumer<String, String>, subsystem: String) {
    println(" \n Start polling over ${topicConsumer.subscription()} ")
    val messages = pollMessage(topicConsumer)
    println("  message from ${topicConsumer.subscription()} to be managed " )
    //ask for execution to a subsytem
    messages?.map { singleMessage
        -> when(execute(singleMessage.value(), subsystem) == Status.OK) {
            true -> {
                //ack the message !
                ackMessage(topicConsumer)
            }
            false -> {
                println(" subsystem unavailable $subsystem  -- $singleMessage.value" )
                sendMessage(queueErrorUrl,null,singleMessage.value())
                println(" send to error topic $subsystem" )
            }
        }
    }
}

suspend fun managePriceAlerts() {
    kafkaMarketingConsumer.subscribe(listOf(queueMarketingUrl))
    while (true){
        try {
            delay(loopMarketingTiming) //non blocking
            //consume messages over the queue
            println(" \n Marketing/Price alerts to be managed " )
            val messages = receiveMessage(kafkaMarketingConsumer)
            //based on some business logic, send to a subsequent queue
            messages?.map {
                    singleMessage ->
                        when(decidePriceChanges(getJsonOrder(singleMessage.value()))) {
                            true -> {
                                runCatching {
                                    //logic has decided to
                                    sendMessage(pricingPolicyUrl,null, "Raise price over item " + getJsonOrder(singleMessage.value())?.item) }
                                    .onSuccess {
                                        //if the message has been sent over, then delete for avoid any subsequent read
                                        //deleteSQSMessage(queueMarketingUrl,singleMessage)
                                    }
                            }
                            false ->
                            {
                                println("no price movement")
                                //when the price does not increase then delete the message
                                //deleteSQSMessage(queueMarketingUrl,singleMessage)
                            }
                }
            }
        } catch (ex: Exception) {
            println("${currentThread().name} failed with {$ex}. Retrying...")
        }
        println(" Marketing/Price alerts has been managed \n")
    }
}


fun execute(it: String, subsystem: String): Status {
    var response: Response = Response(SERVICE_UNAVAILABLE)
    val client: HttpHandler = JavaHttpClient()
    val printingClient: HttpHandler = DebuggingFilters.PrintResponse().then(client)
    val request = Request(GET, Uri.of("http://localhost:8081/mock/${subsystem}"), it)
    //executes the request
    response = printingClient(request)
    println(" output from ${subsystem} " + response.bodyString())
    return response.status
}

private fun getJsonOrder(singleMessage: String): Order? {
    return gson.fromJson(singleMessage, Order::class.java)
}

fun decidePriceChanges(extractedMessage: Order?): Boolean {
//    TODO("Price should change it item has already been sell a lot")
    val currentMoment = Clock.System.now()
    return when {
        //bizzarre logic: price over 800eur will raise during hours between 15 and 21
        (extractedMessage?.price!!.compareTo(800) > 0)
                && 13 < currentMoment.toLocalDateTime(TimeZone.UTC).time.hour
                && currentMoment.toLocalDateTime(TimeZone.UTC).time.hour < 21
        -> {
            println(" Raise price");
            true
        }
        else -> {
            println(" No price movement");
            false
        }
    }
}

fun readFileContents(file: File): String
        = file.readText(Charsets.UTF_8)
