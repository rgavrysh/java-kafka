FROM java:8

COPY target/stream-1.0-SNAPSHOT-jar-with-dependencies.jar stream.jar

ENV KAFKA_SERVER kafka-server:9092

RUN java -jar stream.jar 8081 ${KAFKA_SERVER}

EXPOSE 8081 
