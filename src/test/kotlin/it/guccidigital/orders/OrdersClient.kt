package it.guccidigital.orders

import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.CreateBucketRequest
import aws.smithy.kotlin.runtime.http.Url
import it.guccidigital.models.Order
import it.guccidigital.models.Orders
import org.http4k.client.JavaHttpClient
import org.http4k.core.*
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.filter.DebuggingFilters.PrintResponse

import org.http4k.format.Jackson.auto

data class Email(val value: String)
data class Message(val subject: String, val from: Email, val to: Email)

fun main()  {
    println(" starting test client for submitting orders to the main application " )
    val client: HttpHandler = JavaHttpClient()
    val printingClient: HttpHandler = PrintResponse().then(client)

    ////////////////////////////////////////////////////////////////////////////////////
    // We can use the auto method here from either Jackson, Gson or the Xml message format objects.
    // Note that the auto() method needs to be manually imported as IntelliJ won't pick it up automatically.
    val ordersLens = Body.auto<Orders>().toLens()

    val myMessage = Message("hello", Email("bob@git.com"), Email("sue@git.com"))
    val myOrder1 = Order("id1", "M", 400, "blue", "Via Roma, 2", "50144", "DE")
    val myOrder3 = Order("id3", "M", 1100, "blue", "Rue balzac, 2", "45445", "IT")
    val myOrder2 = Order("id2", "L", 2900, "blue", "Rue Habc 2", "45355", "SP")
    val ordersList: List<Order> = listOf(myOrder1, myOrder2, myOrder3)
    val myOrders: Orders = Orders(ordersList)

    // to inject the body into the message - this also works with Response
    val requestWithEmail = ordersLens(myOrders, Request(GET, "/"))

    //executes the request vs mulesoft
/*    val response: Response = printingClient(requestWithEmail.method(POST).uri(Uri.of( "http://localhost:9003/api/orders")))
    println(" output  " + response.bodyString() )*/

    //executes the request vs kotlin
    val response: Response = printingClient(requestWithEmail.method(POST).uri(Uri.of( "http://localhost:9002/orders")))
    println(" output  " + response.bodyString() )

}


