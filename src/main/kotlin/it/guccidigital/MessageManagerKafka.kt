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

//consumer for the internal topics
val internalConsumerProps =
    mapOf(
        "bootstrap.servers" to "localhost:9092",
        "auto.offset.reset" to "earliest",//the consumer start to read from an offset
        "key.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "value.deserializer" to "org.apache.kafka.common.serialization.StringDeserializer",
        "group.id" to "gucciInternalGroup",
        "security.protocol" to "PLAINTEXT"
    )
val internalKafkaConsumer = KafkaConsumer<String, String>(internalConsumerProps)

/*
consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
consumerProps.put(JsonDeSerializer.TYPE_MAPPINGS, "cat:com.yourcat.Cat, hat:com.yourhat.hat");
*/


suspend fun <K, V> Producer<K, V>.asyncSend(record: ProducerRecord<K, V>) =
    suspendCoroutine<RecordMetadata> {
            continuation ->
                send(record) {
                    metadata,
                    exception ->
                        exception?.let(continuation::resumeWithException) ?: continuation.resume(metadata)
                }
    }


suspend fun sendMessage(topicUrlVal: String, partition: String?, message: String) {
    kafkaProducer.asyncSend(ProducerRecord(topicUrlVal, partition, message))
    println("A single message was successfully sent to " + topicUrlVal)
    println("Message sent = " + message)
}

tailrec fun <T> repeatUntilSome(block: () -> T?): T = block() ?: repeatUntilSome(block)

fun receiveMessage(topicUrlVal: String): ConsumerRecords<String, String>? {
    kafkaConsumer.subscribe(listOf(topicUrlVal))

    println(" \n Start polling over $topicUrlVal ")
    val records = kafkaConsumer.poll(Duration.ofSeconds(1))
    println(" End polling over $topicUrlVal ")

    records.iterator().forEach {
        val personJson = it.value()
        println("receiving message $personJson " )
        /* + gson.toJson(it.value().toString())) */
    }
    //commit the offset to kafka to avoid reprocessing the same message (a kind of)
    //when/why do I need this ?
    kafkaConsumer.commitAsync()
    return records
}

fun pollMessage(topicConsumer: KafkaConsumer<String, String>): ConsumerRecords<String, String>? {
    return topicConsumer.poll(Duration.ofSeconds(1))
}

fun ackMessage(topicConsumer: KafkaConsumer<String, String>) {
    return topicConsumer.commitAsync()
}