package it.guccidigital

import aws.smithy.kotlin.runtime.http.Url
import com.google.gson.Gson
import it.guccidigital.models.Orders
import org.http4k.core.Body
import org.http4k.format.Jackson.auto
import java.net.URI
import java.util.concurrent.ThreadLocalRandom
import kotlin.streams.asSequence

val gson = Gson()

val bucketName by lazy { "guccibucket" }

const val orders_topic = "local-orders_topic"

//constant for queues and topic
const val queueMarketingUrl: String = "mao-marketing-events"
const val pricingPolicyUrl: String = "mao-pricing-policy-events"
const val queueAccountingUrl: String = "dev-mao-accounting-events"
const val queueOrderUrl: String = "dev-mao-order-events"
const val queueShippingUrl: String = "dev-mao-shipping-events"

val s3EndpointURI: URI = URI.create("http://localhost:9090")

val ordersLens = Body.auto<Orders>().toLens()
val loopMarketingTiming = 60000L
val loopOrdersTiming = 20000L
val loopExternalTiming = 40000L
val httpPort = 9002

val charPool : List<Char> = ('a'..'z') + ('A'..'Z') + ('0'..'9')

fun randomStringByJavaRandom() = ThreadLocalRandom.current()
    .ints(10L, 0, charPool.size)
    .asSequence()
    .map(charPool::get)
    .joinToString("")
