import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;
import java.util.Properties;

public class KafkaConsumer {
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String KAFKA_TOPIC = "kafka.topic";

    public static void run(Context context) throws Exception {
        // set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", context.getString(BOOTSTRAP_SERVERS));
        properties.setProperty("group.id", "test"); // consumer group id
        // get input data
        DataStream<String> stream = env.addSource(new FlinkKafkaConsumer010<>(context.getString(KAFKA_TOPIC), new SimpleStringSchema(), properties));

        // Stream transformations
        DataStream<Status> tweets = stream.map(new JSONParser());

        DataStream<Tuple2<String, String>> geoInfo =
                tweets.filter(tweet -> (TweetFunctions.getTweetCountry(tweet) != null))
                        .map(tweet -> new Tuple2<>(tweet.getUser().getName(), TweetFunctions.getTweetCountry(tweet)))
                        .returns(new TypeHint<Tuple2<String,String>>(){});
        geoInfo.print();

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
            /* TODO: use ParameterTool class of Flink instead of custom Context class ?
            see: https://ci.apache.org/projects/flink/flink-docs-release-1.3/dev/best_practices.html
             */
            Context context = new Context(configFileLocation);
            KafkaConsumer.run(context);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
    /**
     * Implements the JSON parser provided by twitter4J into Flink MapFunction
     */
    public static final class JSONParser implements MapFunction<String, Status> {
        @Override
        public Status map(String value) {
            Status status = null;
            try {
                status = TwitterObjectFactory.createStatus(value);
            } catch(TwitterException e) {
                // TODO: use LOGGER class and add a small explaining message
                e.printStackTrace();
            } finally {
                return status; // return the parsed tweet, or null if exception occured
            }
        }
    }
}
