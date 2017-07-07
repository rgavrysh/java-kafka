package consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Properties;
import java.util.Scanner;

public class CustomConsumer {

    private static Scanner in;
    private static boolean stop = false;

    public static void main(String[] args) throws InterruptedException {
        if (args.length != 2) {
            System.err.printf("Usage: %s <topicName> <groupId>\n",
                    CustomConsumer.class.getSimpleName());
            System.exit(-1);
        }

        in = new Scanner(System.in);
        String topicName = args[0];
        String groupId = args[1];

        ConsumerThread consumerRunnable = new ConsumerThread(topicName, groupId);
        consumerRunnable.start();
        System.out.println("Consumer thread started ...");

        String line = "";
        while (!line.equalsIgnoreCase("exit")) {
            line = in.nextLine();
        }

        consumerRunnable.getKafkaConsumer().wakeup();
        System.out.println("Stopping consumer ...");
        consumerRunnable.join();
    }

    private static class ConsumerThread extends Thread {
        private String topicName;
        private String groupId;
        private KafkaConsumer<String, String> kafkaConsumer;

        ConsumerThread(String topic, String group) {
            this.topicName = topic;
            this.groupId = group;
        }

        @Override
        public void run() {
            // Custom Consumer parameters
            Properties consumerProperties = new Properties();
            consumerProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
            consumerProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            consumerProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
            consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
            consumerProperties.put(ConsumerConfig.CLIENT_ID_CONFIG, "simple");

            kafkaConsumer = new KafkaConsumer<String, String>(consumerProperties);
            kafkaConsumer.subscribe(Arrays.asList(topicName), new ConsumerRebalanceListener() {
                @Override
                public void onPartitionsRevoked(Collection<TopicPartition> collection) {
                    System.out.printf("%s topic-partitions are revoked from this consumer.\n",
                            Arrays.toString(collection.toArray()));
                }

                @Override
                public void onPartitionsAssigned(Collection<TopicPartition> collection) {
                    System.out.printf("%s topic-partitions are assigned to this consumer.\n",
                            Arrays.toString(collection.toArray()));
                }
            });

            // Processing messages
            try {
                while (true) {
                    ConsumerRecords<String, String> records = kafkaConsumer.poll(1000);
                    for (ConsumerRecord<String, String> record : records) {
                        System.out.println(record.toString());
                    }
                }
            } catch (WakeupException ex) {
                System.out.println("Exception caught. " + ex.getMessage());
            } finally {
                kafkaConsumer.close();
                System.out.println("KafkaConsumer closed.");
            }
        }

        public KafkaConsumer<String, String> getKafkaConsumer() {
            return kafkaConsumer;
        }
    }
}