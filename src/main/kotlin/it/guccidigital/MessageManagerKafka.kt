package it.guccidigital

import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import java.time.Duration
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


val producerProps = mapOf<String, String>(
    "bootstrap.servers" to "localhost:9092",
    "key.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
    "value.serializer" to "org.apache.kafka.common.serialization.StringSerializer",
    "security.protocol" to "PLAINTEXT"
)
val kafkaProducer = KafkaProducer<String, String>(producerProps)

//data is pushed to the broker from the producer and pulled from the broker by the consumer.
val consumerProps =
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "auto.offset.reset" to "earliest",//the consumer start to read from an offset
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "gucciGroup",
        "security.protocol" to "PLAINTEXT"
    )
val kafkaConsumer = KafkaConsumer<String, String>(consumerProps)
val kafkaMarketingConsumer = KafkaConsumer<String, String>(consumerProps)

//consumer for the internal topics
val internalConsumerProps =
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "auto.offset.reset" to "earliest",//the consumer start to read from an offset
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "gucciInternalGroup",
        "security.protocol" to "PLAINTEXT",
        "enable.auto.commit" to "false"
    )
val internalKafkaOrderConsumer = KafkaConsumer<String, String>(internalConsumerProps)
val internalKafkaShippingConsumer = KafkaConsumer<String, String>(internalConsumerProps)
val internalKafkaAccountingConsumer = KafkaConsumer<String, String>(internalConsumerProps)


//consumer for the marketing topics
val marketingConsumerProps =
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "auto.offset.reset" to "earliest",//the consumer start to read from an offset
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "gucciMarketingGroup",
        "security.protocol" to "PLAINTEXT",
        "enable.auto.commit" to "true"
    )
val marketingKafkaConsumer = KafkaConsumer<String, String>(marketingConsumerProps)


suspend fun <K, V> Producer<K, V>.asyncSend(record: ProducerRecord<K, V>) =
    suspendCoroutine<RecordMetadata> {
            continuation ->
                send(record) {
                    metadata,
                    exception ->
                        exception?.let(continuation::resumeWithException) ?: continuation.resume(metadata)
                }
    }


fun sendMessage(topicUrlVal: String, partition: String?, message: String) {
    println("\n Sending this $message ")
    kafkaProducer.send(ProducerRecord(topicUrlVal, partition, message))
    println("\n A single message was successfully sent to " + topicUrlVal)
    //println("Message sent = " + message)
}

tailrec fun <T> repeatUntilSome(block: () -> T?): T = block() ?: repeatUntilSome(block)

fun receiveMessage(kafkaConsumer: KafkaConsumer<String, String>): ConsumerRecords<String, String>? {
    println(" \n Start polling over ${kafkaConsumer.subscription()} ")
    val records = kafkaConsumer.poll(Duration.ofSeconds(1))
    println(" End polling over ${kafkaConsumer.subscription()} ")

    records.iterator().forEach {
        val personJson = it.value()
        println("receiving message $personJson " )
    }
    //I do not need to commit because autocommit is true
    return records
}

fun pollMessage(topicConsumer: KafkaConsumer<String, String>): ConsumerRecords<String, String>? {
    return topicConsumer.poll(Duration.ofSeconds(60))
}

fun ackMessage(topicConsumer: KafkaConsumer<String, String>) {
    return topicConsumer.commitSync()
}