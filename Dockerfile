FROM java:8

RUN wget http://www-eu.apache.org/dist/kafka/0.11.0.0/kafka_2.11-0.11.0.0.tgz && tar -xzf kafka_2.11-0.11.0.0.tgz && cd kafka_2.11-0.11.0.0/

WORKDIR kafka_2.11-0.11.0.0

ENV ZK_SERVER zookeeper
