package consumer;

import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.*;

public class CustomConsumer {

    private static Scanner in;
    private static boolean stop = false;
    private static long offset = 0;

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 2) {
            System.err.printf("Usage: %s <topicName> <groupId> <startingOffset>\n",
                    CustomConsumer.class.getSimpleName());
            System.exit(-1);
        } else if (args.length == 3){
            offset = Long.valueOf(args[2]);
        }

        in = new Scanner(System.in);
        String topicName = args[0];
        String groupId = args[1];

        ConsumerThread consumerRunnable = new ConsumerThread(topicName, groupId, offset);
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
        private long startingOffset;
        private KafkaConsumer<String, String> kafkaConsumer;

        ConsumerThread(String topic, String group, long offset) {
            this.topicName = topic;
            this.groupId = group;
            this.startingOffset = offset;
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
            consumerProperties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);


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
                    Iterator<TopicPartition> iterator = collection.iterator();
                    while (iterator.hasNext()) {
                        TopicPartition topicPartition = iterator.next();
                        System.out.println("Current offset is -> " + kafkaConsumer.position(topicPartition) +
                                "\tcommitted offset is -> " + kafkaConsumer.committed(topicPartition));
                        if (startingOffset == 0) {
                            System.out.println("Setting offset to beginning.");
                            kafkaConsumer.seekToBeginning(collection);
                        } else if (startingOffset == -1) {
                            System.out.println("Setting offset to the end.");
                            kafkaConsumer.seekToEnd(collection);
                    } else {
                            System.out.printf("Resetting partition to " + startingOffset);
                            kafkaConsumer.seek(topicPartition, startingOffset);
                        }
                }
            }
        });

        // Processing messages
            try

        {
            while (true) {
                ConsumerRecords<String, String> records = kafkaConsumer.poll(1000);
                for (ConsumerRecord<String, String> record : records) {
                    System.out.println(record.toString());
                }
            }
        } catch(
        WakeupException ex)

        {
            System.out.println("Exception caught. " + ex.getMessage());
        } finally

        {
            kafkaConsumer.close();
            System.out.println("KafkaConsumer closed.");
        }
    }

    public KafkaConsumer<String, String> getKafkaConsumer() {
        return kafkaConsumer;
    }
}
}