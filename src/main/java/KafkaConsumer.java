import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.runtime.state.FunctionInitializationContext;
import org.apache.flink.runtime.state.FunctionSnapshotContext;
import org.apache.flink.streaming.api.checkpoint.CheckpointedFunction;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.elasticsearch.ElasticsearchSinkFunction;
import org.apache.flink.streaming.connectors.elasticsearch.RequestIndexer;
import org.apache.flink.streaming.connectors.elasticsearch5.ElasticsearchSink;
import org.apache.flink.streaming.util.serialization.SimpleStringSchema;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Requests;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.*;

public class KafkaConsumer {
    private static final String BOOTSTRAP_SERVERS = "bootstrap.servers";
    private static final String KAFKA_TOPIC = "kafka.topic";

    public static void run(Context context) throws Exception {
        // set up the execution environment
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        Properties properties = new Properties();
        properties.setProperty("bootstrap.servers", context.getString(BOOTSTRAP_SERVERS));
        properties.setProperty("group.id", "test"); // consumer group id

        // set up elasticsearch sink
        Map<String, String> config = new HashMap<>();
        config.put("cluster.name", "elasticsearch"); // the name of the actual elasticsearch cluster we connect to
        // This instructs the sink to emit after every element, otherwise they would be buffered
        config.put("bulk.flush.max.actions", "1");
        List<InetSocketAddress> transportAddresses = new ArrayList<>();
        transportAddresses.add(new InetSocketAddress(InetAddress.getByName("localhost"), 9300));

        // get input data
        DataStream<String> stream = env.addSource(new FlinkKafkaConsumer010<>(context.getString(KAFKA_TOPIC), new SimpleStringSchema(), properties));

        // Stream transformations
        DataStream<Status> tweets = stream.map(new JSONParser()).filter(tweet -> ((tweet.getCreatedAt() != null) && (tweet.getText() != null)));

        // create a stream of GPS information
        DataStream<Tuple4<String, Double, Double, Long>> geoInfo =
                tweets.filter(tweet -> (TweetFunctions.getTweetCountry(tweet) != null))
                        .filter(tweet -> TweetFunctions.getTweetGPSCoordinates(tweet) != null)
                        .map(new TweetToLocation());
        //geoInfo.print();
        geoInfo.addSink(new ElasticsearchSink<>(config, transportAddresses, new LocationInserter()));

        // create a stream of Sentiment Analysis
        DataStream<Tuple2<Long, String>> sentimentInfo = tweets.map(new TweetToSentiment());
        //sentimentInfo.print();
        sentimentInfo.addSink(new ElasticsearchSink<>(config, transportAddresses, new SentimentInserter()));

        // create a stream of hashtags
        DataStream<Tuple2<Long, String>> hashtagsInfo = tweets.filter(tweet -> TweetFunctions.getHashtags(tweet).length > 0)
                .map(tweet -> new Tuple2<>(tweet.getCreatedAt().getTime(), TweetFunctions.getHashtagsAsString(tweet)))
                .returns(new TypeHint<Tuple2<Long, String>>() {});
        hashtagsInfo.addSink(new ElasticsearchSink<>(config, transportAddresses, new HashtagsInserter()));

        // create a stream of keywords
        DataStream<Tuple2<Long, String>> keywordsInfo = tweets.filter(tweet -> TweetFunctions.getKeywords(tweet).length > 0)
                .map(tweet -> new Tuple2<>(tweet.getCreatedAt().getTime(), TweetFunctions.getKeywordsAsString(tweet)))
                .returns(new TypeHint<Tuple2<Long, String>>() {});
        keywordsInfo.addSink(new ElasticsearchSink<>(config, transportAddresses, new KeywordsInserter()));

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
     * Inserts tweet locations into elasticsearch.
     */
    public static class LocationInserter
            implements ElasticsearchSinkFunction<Tuple4<String, Double, Double, Long>> {

        // construct index request
        @Override
        public void process(
                Tuple4<String, Double, Double, Long> record,
                RuntimeContext ctx,
                RequestIndexer indexer) {

            // construct JSON document to index
            Map<String, String> json = new HashMap<>();
            json.put("time", record.f3.toString());         // timestamp
            json.put("geolocation", record.f1+","+record.f2);  // lat,lon pair
            json.put("country", record.f0.toString());      // country

            IndexRequest rqst = Requests.indexRequest()
                    .index("locations")        // index name
                    .type("locationsType")  // mapping name
                    .source(json);

            indexer.add(rqst);
        }
    }

    /**
     * Inserts tweet sentiments into elasticsearch.
     */
    public static class SentimentInserter implements ElasticsearchSinkFunction<Tuple2<Long, String>> {

        // construct index request
        @Override
        public void process(
                Tuple2<Long, String> record,
                RuntimeContext ctx,
                RequestIndexer indexer) {

            // construct JSON document to index
            Map<String, String> json = new HashMap<>();
            json.put("time", record.f0.toString());         // timestamp
            json.put("sentiment", record.f1);      // sentiment

            IndexRequest rqst = Requests.indexRequest()
                    .index("sentiments")        // index name
                    .type("sentimentsType")  // mapping name
                    .source(json);

            indexer.add(rqst);
        }
    }

    /**
     * Inserts tweet hashtags into the "twitter-analytics" index.
     */
    public static class HashtagsInserter implements ElasticsearchSinkFunction<Tuple2<Long, String>> {

        // construct index request
        @Override
        public void process(
                Tuple2<Long, String> record,
                RuntimeContext ctx,
                RequestIndexer indexer) {

            // construct JSON document to index
            Map<String, String> json = new HashMap<>();
            json.put("time", record.f0.toString());         // timestamp
            json.put("hashtags", record.f1);      // hashtags

            IndexRequest rqst = Requests.indexRequest()
                    .index("hashtags")        // index name
                    .type("hashtagsType")  // mapping name
                    .source(json);

            indexer.add(rqst);
        }
    }

    /**
     * Inserts tweet keywords into the "twitter-analytics" index.
     */
    public static class KeywordsInserter implements ElasticsearchSinkFunction<Tuple2<Long, String>> {

        // construct index request
        @Override
        public void process(
                Tuple2<Long, String> record,
                RuntimeContext ctx,
                RequestIndexer indexer) {

            // construct JSON document to index
            Map<String, String> json = new HashMap<>();
            json.put("time", record.f0.toString());         // timestamp
            json.put("keywords", record.f1);      // keywords

            IndexRequest rqst = Requests.indexRequest()
                    .index("keywords")        // index name
                    .type("keywordsType")  // mapping name
                    .source(json);

            indexer.add(rqst);
        }
    }


    /**
     * Maps a tweet to its country, latitude, longitude, and timestamp
     */
    public static class TweetToLocation implements MapFunction<Status, Tuple4<String, Double, Double, Long>> {
        @Override
        public Tuple4<String, Double, Double, Long> map(Status tweet) throws Exception {
            return new Tuple4<>(TweetFunctions.getTweetCountry(tweet),
                    TweetFunctions.getTweetGPSCoordinates(tweet).getLatitude(),
                    TweetFunctions.getTweetGPSCoordinates(tweet).getLongitude(),
                    tweet.getCreatedAt().getTime()
                    );
        }
    }

    /**
     * Maps a tweet to its country, latitude, longitude, and timestamp
     */
    public static class TweetToSentiment implements MapFunction<Status, Tuple2<Long, String>>, CheckpointedFunction {

        private transient ListState<BasicSentimentAnalysis> modelState;

        private transient BasicSentimentAnalysis model;

        @Override
        public Tuple2<Long,String> map(Status tweet) throws Exception {
            Long time = tweet.getCreatedAt().getTime();
            String text = tweet.getText();
            String score = this.model.getSentimentLabel(text);
            return new Tuple2<>(time, score);
        }

        @Override
        public void snapshotState(FunctionSnapshotContext functionSnapshotContext) throws Exception {
            // constant model, so nothing to do
        }

        @Override
        public void initializeState(FunctionInitializationContext context) throws Exception {
            ListStateDescriptor<BasicSentimentAnalysis> listStateDescriptor = new ListStateDescriptor<>("model", BasicSentimentAnalysis.class);

            modelState = context.getOperatorStateStore().getUnionListState(listStateDescriptor);

            if (context.isRestored()) {
                // restore the model from state
                model = modelState.get().iterator().next();
            } else {
                modelState.clear();

                // read the model from somewhere, e.g. read from a file
                model = new BasicSentimentAnalysis("negative-words.txt", "positive-words.txt");

                // update the modelState so that it is checkpointed from now
                modelState.add(model);
            }
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
