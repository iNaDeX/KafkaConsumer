import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.util.Collector;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;

import java.util.Properties;

public class KafkaConsumer {
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String KAFKA_TOPIC = "kafka.topic";

    public static void run(Context context) throws Exception {
        // set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment
                .getExecutionEnvironment();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", context.getString(BOOTSTRAP_SERVERS));
        properties.setProperty("group.id", "test");
        // get input data
        DataStream<String> text = env.addSource(new FlinkKafkaConsumer010<>(context.getString(KAFKA_TOPIC), new SimpleStringSchema(), properties));

        // Stream transformations
        DataStream<Tuple2<String, Integer>> counts =
            // Foreach
            text.flatMap(new LineSplitter())
                    // group by the tuple field "0" and sum up tuple field "1"
                    .keyBy(0)
                    .sum(1);

        counts.print();

        // execute program
        env.execute("Java Flink KafkaConsumer");
    }

    public static void main(String[] args) {
        if (args.length != 1){
            System.err.println("USAGE:\nKafkaConsumer <configFilePath>");
            return;
        }
        try {
            String configFileLocation = args[0];
            Context context = new Context(configFileLocation);
            KafkaConsumer.run(context);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    /**
     * Implements the string tokenizer that splits sentences into words as a user-defined
     * FlatMapFunction. The function takes a line (String) and splits it into
     * multiple pairs in the form of "(word,1)" (Tuple2<String, Integer>).
     */
    public static final class LineSplitter implements FlatMapFunction<String, Tuple2<String, Integer>> {
        @Override
        public void flatMap(String value, Collector<Tuple2<String, Integer>> out) {
            // normalize and split the line
            String[] tokens = value.toLowerCase().split("\\W+");
            // emit the pairs
            for (String token : tokens) {
                if (token.length() > 0) out.collect(new Tuple2<>(token, 1));
            }
        }
    }
}
