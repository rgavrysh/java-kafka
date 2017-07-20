package stream;

import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.KStream;
import org.apache.kafka.streams.kstream.KStreamBuilder;
import org.apache.kafka.streams.kstream.KTable;

import java.util.Arrays;
import java.util.Properties;

public class WordCount {
    public static void main(String[] args) throws InterruptedException {
        Properties config = new Properties();
        config.put(StreamsConfig.APPLICATION_ID_CONFIG, "wordcount-application");
        config.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        config.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        config.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        KStreamBuilder builder = new KStreamBuilder();
        KStream<String, String> textLines = builder.stream(Serdes.String(), Serdes.String(),
                "stream-input-file");
        textLines.foreach((key, value) -> System.out.println(key + "\t=>\t" + value));
        System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> stream.print() <<<<<<<<<<<<<<<<<<<<<<<<<");
//        textLines.print();
        KTable<String, Long> wordCounts = textLines
//                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .flatMapValues(value -> Arrays.asList(value.toLowerCase().split("\\W+")))
                .groupBy((key, value) -> value)
                .count("counts");
        wordCounts.foreach((key, value) -> System.out.println("KTable: " + key + "\t=>\t" + value));
        wordCounts.to(Serdes.String(), Serdes.Long(), "stream-output");

        KafkaStreams streams = new KafkaStreams(builder, config);
        streams.setUncaughtExceptionHandler((Thread t,  Throwable e) -> {
            System.out.println("Exception handler ...");
            System.out.println("Exception with message: >>> \n" + e.getLocalizedMessage() + "\n" +
                    ">>> happened while executing thread: " + t.getName());
        });
        streams.start();

        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}
