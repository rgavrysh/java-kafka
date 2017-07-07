package partitioner;

import org.apache.kafka.clients.producer.Partitioner;
import org.apache.kafka.common.Cluster;
import org.apache.kafka.common.PartitionInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CountryPartitioner implements Partitioner {
    private static Map<String, Integer> countryToPartition;

    @Override
    public int partition(String topic, Object o, byte[] bytes, Object o1, byte[] bytes1, Cluster cluster) {
        List<PartitionInfo> partitions = cluster.availablePartitionsForTopic(topic);
        String valueStr = (String) o1;
        String countryName = ((String) o1).split(":")[0];
        if (countryToPartition.containsKey(countryName)) {
            return countryToPartition.get(countryName);
        } else {
            int noOfPartitions = cluster.topics().size();
            return o1.hashCode() % noOfPartitions + countryToPartition.size();
        }
    }

    @Override
    public void close() {

    }

    @Override
    public void configure(Map<String, ?> map) {
        System.out.println("Inside Country Partitioner config " + map);
        countryToPartition = new HashMap<String, Integer>();
        for (Map.Entry<String, ?> entry : map.entrySet()) {
            if (entry.getKey().startsWith("partition.")) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                System.out.println("Key name: " + key.substring(10));
                int partitionId = Integer.parseInt(key.substring(10));
                countryToPartition.put(value, partitionId);
            }
        }
    }
}
