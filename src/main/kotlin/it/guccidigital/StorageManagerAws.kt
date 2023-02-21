@file:Suppress("JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE")

import it.guccidigital.s3EndpointURI
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.*
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*


var s3Client: S3Client = S3Client.builder()
    .region(Region.of("elasticmq"))
    .credentialsProvider(ProfileCredentialsProvider.create())
    .endpointOverride(s3EndpointURI)
    .build()

fun createBucketAws(bucketName: String) {
    println("Create bucket $bucketName")
    val s3Waiter = s3Client.waiter();
    val bucketRequest = CreateBucketRequest.builder()
        .bucket(bucketName)
        .build();

    s3Client.createBucket(bucketRequest);
    val bucketRequestWait = HeadBucketRequest.builder()
        .bucket(bucketName)
        .build();

    // Wait until the bucket is created and print out the response.
    val waiterResponse = s3Waiter.waitUntilBucketExists(bucketRequestWait);
    waiterResponse.matched().response()
    println(bucketName +" is ready");
}

fun listBucketsAws() {
    // List buckets
    val listBucketsRequest = ListBucketsRequest.builder().build()
    val listBucketsResponse: ListBucketsResponse = s3Client.listBuckets(listBucketsRequest)
    listBucketsResponse.buckets().stream().forEach { x -> println(x.name()) }
}

fun getContentsAws(bucketName: String, keyName: String): String? {

    val getObjectRequest = GetObjectRequest.builder()
        .bucket(bucketName)
        .key(keyName)
        .build()
    val response = s3Client.getObject(getObjectRequest)
    return String(response.readAllBytes())
}

fun putObjectStreamAws(bucketName: String, objectKey: String, contents: String) {
    println(" sending data to the bucket " + contents)
    val objectRequest = PutObjectRequest.builder()
        .bucket(bucketName)
        .key(objectKey)
        .build()
    s3Client.putObject(objectRequest, RequestBody.fromString(contents))
    println(" object saved to bucket $bucketName" )
}

@Throws(IOException::class)
private fun getRandomByteBuffer(size: Int): ByteBuffer? {
    val b = ByteArray(size)
    Random().nextBytes(b)
    return ByteBuffer.wrap(b)
}

fun listObjectsAws(bucketName: String): ListObjectsResponse {
    // List objects
    val listObjects = ListObjectsRequest
        .builder()
        .bucket(bucketName)
        .build()

    val res: ListObjectsResponse = s3Client.listObjects(listObjects)
    return res
}

fun extractShippingDetailsAws(bucketName: String): ListObjectsResponse {
    val request = ListObjectsRequest.builder().bucket(bucketName).build()
    println("\n lookup objects inside " + bucketName)
    return s3Client.listObjects(request)
}


