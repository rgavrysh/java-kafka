package stream;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;
import org.apache.kafka.streams.state.HostInfo;

import java.util.Arrays;
import java.util.Properties;

public class WordCount {
    private static final String DEFAULT_REST_ENDPOINT_HOSTNAME = "localhost";
    static String stateStoreName = "Counts";

    public static void main(String[] args) throws Exception {
        final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
        final String DEFAULT_SCHEMA_REGISTRY_URL = "http://localhost:8081";
        if (args.length == 0 || args.length > 4) {
            throw new IllegalArgumentException("\nusage ... <portForRestEndpoint>" +
                    "\n[<bootsrtapServers> (optional, default: " + DEFAULT_BOOTSTRAP_SERVERS + ")]" +
                    "\n[<schemaRegistryUrl> (optional, default: " + DEFAULT_SCHEMA_REGISTRY_URL + ")]" +
                    "\n[<hostnameForRestEndpoint> (optional, default: " + DEFAULT_REST_ENDPOINT_HOSTNAME + ")]");
        }

        final Integer restEndpointPort = Integer.valueOf(args[0]);
        final String bootstrapServers = args.length > 1 ? args[1] : DEFAULT_BOOTSTRAP_SERVERS;
        final String schemaRegistryUrl = args.length > 2 ? args[2] : DEFAULT_REST_ENDPOINT_HOSTNAME;
        final String hostnameForRestEndpoint = args.length > 3 ? args[3] : DEFAULT_REST_ENDPOINT_HOSTNAME;
        final HostInfo hostInfo = new HostInfo(hostnameForRestEndpoint, restEndpointPort);
        final String stateDir = "/tmp/kafka-streams";

        System.out.println("Connecting to Kafka cluster via bootstrap server: " + bootstrapServers);
        System.out.println("Starting schema registry server on: " + schemaRegistryUrl);

        KafkaStreams streams = createKafkaCharts(bootstrapServers,
                schemaRegistryUrl,
                restEndpointPort,
                stateDir);

        streams.setUncaughtExceptionHandler((Thread t, Throwable e) -> {
            System.out.println("Exception handler ...");
            System.out.println("Exception with message: >>> \n" + e.getLocalizedMessage() + "\n" +
                    ">>> happened while executing thread: " + t.getName());
        });

        streams.cleanUp();
        streams.start();

        final WordCountRestService wordCountRestService = startRestService(streams, hostInfo);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            wordCountRestService.stop();
            streams.close();
        }));
    }

    private static KafkaStreams createKafkaCharts(String bootstrapServers,
                                                  String schemaRegistryUrl,
                                                  Integer restEndpointPort,
                                                  String stateDir) {
        final String streamInputTopic = "stream-input-file";
        final String streamOutputTopic = "stream-output";

        Properties properties = new Properties();
        properties.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
        properties.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        properties.put(StreamsConfig.APPLICATION_SERVER_CONFIG, schemaRegistryUrl + ":" + restEndpointPort);
        properties.put(StreamsConfig.STATE_DIR_CONFIG, stateDir);
        properties.put(StreamsConfig.COMMIT_INTERVAL_MS_CONFIG, 500);
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, String> textLines = builder.stream(Serdes.String(), Serdes.String(),
                streamInputTopic);
        textLines.foreach((key, value) -> System.out.println(key + "\t=>\t" + value));
        KTable<String, Long> wordCounts = textLines
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, value) -> value)
                .count(stateStoreName);
        wordCounts.foreach((key, value) -> System.out.println("KTable: " + key + "\t=>\t" + value));
        wordCounts.to(Serdes.String(), Serdes.Long(), streamOutputTopic);
        return new KafkaStreams(builder, properties);
    }

    private static WordCountRestService startRestService(KafkaStreams streams, HostInfo hostInfo) throws Exception {
        final WordCountRestService wordCountRestService = new WordCountRestService(streams, hostInfo);
        wordCountRestService.start();
        return wordCountRestService;
    }
}
