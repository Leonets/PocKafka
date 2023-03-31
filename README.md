# Gucci Playground (experiment with Kafka, Kotlin & Local Development)

## Coding core

Design and implement a system that receives orders and then submits to the appropriate external system.

- When an order is detected, store the information regarding the order, the total amount and the destination, and then:
    - let a subsystem decide if the price needs to be changed
    - distribute the order to the external system (order, shipping and accounting API) 
- [OPT] Given a list of countries, just filter orders involving those countries.
- [OPT] Create a dashboard showing grouped orders by country and total amount
- Data can be stored in memory or in a cloud storage solution 
- Orders need to be tracked only since the application is started

Please detail a solution using Kotlin, Kafka and AWS S3

Please provide useful examples for evaluating the following:

- fault tolerance to kafka topic crash
- coroutines simple usage

# Proposed solution

The solution developed makes use of the following

## Messaging Layer 

The messaging layer has been deployed over kafka, a distributed event streaming platform you can read about it at https://kafka.apache.org/

## Data Layer

An S3 service offers the storage service

## API

The following API have been built with following endpoints:

| Endpoint          | Medhod | Description                                                                         | Availability |
|-------------------|--------|-------------------------------------------------------------------------------------|--------------|
| /orders           | POST   | Receives orders in json format and delivers in topic 'Local-Order'                  | Done         |
|                   |        | (it also save the payload in the S3 GucciBucket)                                    |              |
| /orders/dashboard | GET    | Returns orders summary by showing country, total orders, total amount (data from S3) | Done         |
| /orders/shipping  | GET    | Returns order shipping details (data from S3)                                       | Done         |

## Integration layer
This layer manages the following operations:

- listens over the main topic (Local-Order) and forwards to the four topics (Marketing, Accounting, Shipping, Orders) 

- listens on a specific topic (Marketing) and then decides if the item price need to be changed and if then sends a message to a specific topic (Pricing_Policy)

- listens over the three topic (Accounting, Shipping, Orders) for then forwarding the request to an external system (HTTP)

## Libraries used and functions used

- publish/consume over topic (with kafka standard lib)
- create bucket/insert key/read key and its content
- routes get and post
- http client over get/post api
- data type conversion to/from json/httpmessage/custom 

## Diagrams

[Main Application Diagram](support/guccidemokafka2.png)

[Custom Serializer](support/kafkaCustomSerializer.png)

## Stack

Execute the container with compose

support> docker compose -f compose.yml up
support> docker compose -f compose.yml down

docker --version
Docker version 20.10.22, build 3a2c30b

docker-compose --version
docker-compose version 1.25.1, build a82fef07

docker compose version
Docker Compose version v2.14.1

Execute the kotlin application:

- Orders.kt (main application)
- OrdersClient.kt (test client for submitting orders to the main application)
- ExternalSystems.kt (external system mock application)

Queue can be monitored from here: http://localhost:9000/

Application API are available here:

    http://localhost:9003/api/orders
    http://localhost:9003/api/shipping
    http://localhost:9003/api/dashboard

## Payload that represents Orders
```
{
“Orders”: 
    "Order":{
        "item": "id1",
        "size": "M"
        "price": 400,
        "color": "blue",
        "address": "via roma 1",
        "cap": "50144",
        "destinationCountry": "IT"
    },
    "Order":{
        "item": "id2",
        "size": "L"
        "price": 800,
        "color": "red",
        "address": "via milano 1",
        "cap": "50100",
        "destinationCountry": "IT"
    },
    "Order":{
        "item": "id3",
        "size": "L"
        "price": 3000,
        "color": "black",
        "address": "unknown 898",
        "cap": "19090",
        "destinationCountry": "JP"
    }
}
```



## TODO

when kafka container is stopped the rest application stops to answer to http requests

manage orders function should not ack the message when the external system is unavailable