# java-kafka

# how to use kafka-clients (custom producer/consumer)

1) Download and untar kafka
2) Start zookeeper ```./bin/zookeeper-server-start.sh ./config/zookeeper.properties```
3) Start kafka server ```./bin/kafka-server-start.sh ./config/server.properties```
4) Clone and package this project
5) Start producer ```java -jar target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar twitt``` where ```twitt``` is a name of topic
6) Start few consumers ```java -jar target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar twitt kafka-group```. 3rd argument takes starting offset and can be: 0 (default) -> seekToBeginning(); -1 -> seekToEnd(); positiveNumber -> seek to particular starting offset.

# Note
Topic will be deleted if all consumers are closed before producer, otherwise you'd have to delete it manually ```./bin/kafka-topics.sh --delete --topic twitt --zookeeper localhost:2181```

# How to use kafka-stream (word-count example)
1) Create input topic 'stream-input-file' ```./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic stream-input-file```
2) Create ouput topic (kafka stream will use this topic for output data) ```./bin/kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic stream-ouput```
3) Start console producer for input topic ```./bin/kafka-console-producer.sh --broker-list localhost:9092 --topic stream-input-file```
4) Start word-count stream ```java -jar target/stream-1.0-SNAPSHOT-jar-with-dependencies.jar```
5) Start consumer of output stream ```./bin/kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic stream-output --from-beginning --property print.key=true --property print.value=true --property key.deserializer=org.apache.kafka.common.serialization.StringDeserializer --property value.deserializer=org.apache.kafka.common.serialization.LongDeserializer```
6) To make direct query to state store via REST request, open in your browser http://localhost:8081/word-count/kafka-table/{word}, where ```{word}``` is a path parameter. Example: http://localhost:8081/word-count/kafka-table/message -> as a result you'll get text/plain response with the count-number, like: 34.

Provide messages to 'stream-input-file' topic, you will see them in a word-count console output as a key => value pair (btw. key is null) as well as updates in a KTable, after data being calculated (number of words) then redirected to stream-output topic, check result in the stream-output consumer console.