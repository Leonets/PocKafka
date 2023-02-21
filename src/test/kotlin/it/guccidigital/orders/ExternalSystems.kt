package it.guccidigital.orders

import it.guccidigital.models.Orders
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.format.Jackson.auto
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val ordersLens = Body.auto<Orders>().toLens()

val delayOk = 5000L //it manages only 2 messages because the third goes out of visibility
val delayLessThanVisibilityTimeout = 19000L //it manages only 1 messages because the other goes out of visibility
val delayMoreThanVisibilityTimeout = 25000L //no message gets managed because everyone goes out of visibility

//COnfiguration of dev-mao-order-events {
//    defaultVisibilityTimeout = 10 seconds
//            delay = 5 seconds
//            fifo = false
//    receiveMessageWait = 0 seconds
//}

//definizione delle routes
val app: HttpHandler = routes(
    //register some routes
    "/mock/Accounting" bind GET to {
        Response(OK).body("Accounting operations started")
    },"/mock/Order" bind GET to {
        println("/Order called by someone ")
        Thread.sleep(delayLessThanVisibilityTimeout) //intentionally blocking IO
        Response(OK).body("Order operations started")
    },"/mock/Shipping" bind GET to {
        Response(OK).body("Shipping operations started")
    },
)

fun main() {
    val printingApp: HttpHandler = PrintRequest().then(app)

    val server = printingApp.asServer(SunHttp(mockHttpPort)).start()
    println("Server started on " + server.port())

}
