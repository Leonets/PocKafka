package it.guccidigital.orders

import it.guccidigital.*
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import java.io.File

const val iods_topic = "Referential_Stock_Gucci_QA"

val iodsProducerProps = mapOf<String, String>(
    "bootstrap.servers" to "localhost:9092",
    "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
    "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
    "group.id" to "DEV-OMS-STOCK-GUCCI-GROUP-FOR-MAO",
    "security.protocol" to "PLAINTEXT"
)
val kafkaIodsProducer = KafkaProducer<String, String>(iodsProducerProps)

fun main() {
    send()
}


fun send() {
    val messageFromIods = readFileContents(File("src/test/resources/kafkaRecords.json"))
    sendMessage(iods_topic, null, messageFromIods)
}

fun sendMessage(topicUrlVal: String, partition: String?, message: String) {
    println("\n Sending this $message ")
    kafkaIodsProducer.send(ProducerRecord(topicUrlVal, partition, message))
    println("\n A single message was successfully sent to " + topicUrlVal)
    //println("Message sent = " + message)
}

fun readFileContents(file: File): String
        = file.readText(Charsets.UTF_8)
