package it.guccidigital.orders

import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Status.Companion.OK
import org.http4k.filter.DebuggingFilters.PrintRequest
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

val delay = 1000L //it delays the response of the http server

//definizione delle routes
val app: HttpHandler = routes(
    //register some routes
    "/mock/Accounting" bind GET to {
        Response(OK).body("Accounting operations started")
    },
    "/mock/Order" bind GET to {
        println("/Order called by someone ")
        Thread.sleep(delay) //intentionally blocking IO
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
