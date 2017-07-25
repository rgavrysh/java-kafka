package producer;

import kafka.admin.AdminUtils;
import kafka.admin.RackAwareMode;
import kafka.utils.ZKStringSerializer$;
import kafka.utils.ZkUtils;
import org.I0Itec.zkclient.ZkClient;
import org.I0Itec.zkclient.ZkConnection;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.zookeeper.server.ServerConfig;
import org.apache.zookeeper.server.ZooKeeperServerMain;
import org.apache.zookeeper.server.quorum.QuorumPeerConfig;
import partitioner.CountryPartitioner;

import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

public class CustomProducer {
    private static Scanner in;
    private static String zkServer = "localhost:2181";
    private static String brokerServer = "localhost:9092";
        private static ZkClient zkClient = new ZkClient(zkServer, 10000, 10000,
            ZKStringSerializer$.MODULE$);
    private static ZkUtils zkUtils = new ZkUtils(zkClient, new ZkConnection(zkServer), false);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Please specify topic as a parameter.");
            System.exit(-1);
        }

        String topicName = args[0];
        in = new Scanner(System.in);
        System.out.println("Enter message (type 'exit' to quit).");

        // Create topic
        createTopic(topicName);

        // CustomProducer parameters
        Properties producerProperties = getProducerProperties();

        Producer producer = new KafkaProducer<String, String>(producerProperties);
        String line = in.nextLine();

        produceMessages(topicName, producer, line);
        in.close();
        deleteTopic(topicName);
        zkClient.close();
        producer.close();
    }

    private static Properties getProducerProperties() {
        Properties producerProperties = new Properties();
        producerProperties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerServer);
        producerProperties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, BytesSerializer.class);
        producerProperties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);

        producerProperties.put(ProducerConfig.PARTITIONER_CLASS_CONFIG, CountryPartitioner.class);
        producerProperties.put("partition.1", "USA");
        producerProperties.put("partition.2", "Ukraine");
        return producerProperties;
    }

    private static void deleteTopic(String topicName) {
        if (AdminUtils.topicExists(zkUtils, topicName)) {
            System.out.println("Deleting topic.");
            AdminUtils.deleteTopic(zkUtils, topicName);
        }
    }

    private static void createTopic(String topicName) {
        if (!AdminUtils.topicExists(zkUtils, topicName)) {
            System.out.println("Creating topic.");
            AdminUtils.createTopic(zkUtils, topicName, 3, 1, new Properties(), RackAwareMode.Enforced$.MODULE$);
        }
    }

    private static void produceMessages(String topicName, Producer producer, String line) {
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
    }
}