import junit.framework.TestCase;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class BasicSentimentAnalysisTest extends TestCase {

    private List<Status> tweets = new ArrayList<>();;
    private String tweetsPath = "src/test/testTweets.txt";

    public BasicSentimentAnalysisTest() throws IOException {
        BufferedReader tweetReader = new BufferedReader(new FileReader(new File(tweetsPath)));

        String tweet; // tmp
        while ((tweet = tweetReader.readLine()) != null) {
            Status status = null;
            try {
                status = TwitterObjectFactory.createStatus(tweet);
                String text = status.getText().replace("\n"," ").trim();
                System.out.println(text);
            } catch (TwitterException e) {
                System.err.println("Invalid tweet format");
                e.printStackTrace();
            }
            tweets.add(status);
        }
        tweetReader.close();
    }

    public void testGetSentimentScore() throws Exception {
        BasicSentimentAnalysis model = new BasicSentimentAnalysis("negative-words.txt", "positive-words.txt");

        System.out.println("\n\ntestGetSentimentScore");
        for(Status tweet: tweets) {
            System.out.println(model.getSentimentScore(tweet.getText()));
        }
    }

}