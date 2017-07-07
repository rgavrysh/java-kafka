package producer;

import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import partitioner.CountryPartitioner;

import java.util.Properties;
import java.util.Scanner;

public class CustomProducer {
    private static Scanner in;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify topic as a parameter.");
            System.exit(-1);
        }

        String topicName = args[0];
        in = new Scanner(System.in);
        System.out.println("Enter message (type 'exit' to quit).");

        // CustomProducer parameters
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producerProperties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CountryPartitioner.class);
        producerProperties.put("partition.1", "USA");
        producerProperties.put("partition.2", "Ukraine");

        Producer producer = new KafkaProducer<String, String>(producerProperties);
        String line = in.nextLine();

        while (!line.equalsIgnoreCase("exit")) {
            ProducerRecord<String, String> producerRecord = new ProducerRecord<>(topicName, null, line);
            producer.send(producerRecord, new Callback() {
                @Override
                public void onCompletion(RecordMetadata recordMetadata, Exception e) {
                    System.out.println("Message sent to topic -> " + recordMetadata.topic() +
                    "\tpartition -> " + recordMetadata.partition() +
                    "\tstored at offset -> " + recordMetadata.offset());
                }
            });
            line = in.nextLine();
        }
        in.close();
        producer.close();
    }
}