# java-kafka

# how to

1) Download and untar kafka
2) Start zookeeper ```./bin/zookeeper-server-start.sh ./config/zookeeper.properties```
3) Start kafka server ```./bin/kafka-server-start.sh ./config/server.properties```
4) Clone and package this project
5) Start producer ```java -jar target/producer-1.0-SNAPSHOT-jar-with-dependencies.jar twitt``` where ```twitt``` is a name of topic
6) Start few consumers ```java -jar target/consumer-1.0-SNAPSHOT-jar-with-dependencies.jar twitt kafka-group```. 3rd argument takes starting offset and can be: 0 (default) -> seekToBeginning(); -1 -> seekToEnd(); positiveNumber -> seek to particular starting offset.
# Note
Topic will be deleted if all consumers are closed before producer, otherwise you'd have to delete it manually ```./bin/kafka-topics.sh --delete --topic twitt --zookeeper localhost:2181```