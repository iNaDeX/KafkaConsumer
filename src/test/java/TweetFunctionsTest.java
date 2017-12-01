import junit.framework.TestCase;
import twitter4j.GeoLocation;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.TwitterObjectFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class TweetFunctionsTest extends TestCase {

    private List<Status> tweets = new ArrayList<>();
    private String tweetsPath = "src/test/testTweets.txt";

    public TweetFunctionsTest() throws IOException {
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
        System.out.println();
        tweetReader.close();
    }

    public void testGetTweetCountry() throws Exception {
        System.out.println("\n\ntestGetTweetCountry");
        for(Status tweet: this.tweets) {
            System.out.println(TweetFunctions.getTweetCountry(tweet));
        }
    }

    public void testGetHashtags() throws Exception {
        System.out.println("\n\ntestGetHashtags");
        for(Status tweet: this.tweets) {
            String[] hashtags = TweetFunctions.getHashtags(tweet);
            for(String hashtag: hashtags) {
                System.out.print(hashtag + " ");
            }
            System.out.println();
        }
    }

    public void testGetKeywords() throws Exception {
        System.out.println("\n\ntestGetKeywords");
        for(Status tweet: this.tweets) {
            String[] keywords = TweetFunctions.getKeywords(tweet);
            for(String keyword: keywords) {
                System.out.print(keyword + " ");
            }
            System.out.println();
        }
    }

    public void testGetInfluencerScore() throws Exception {
        System.out.println("\n\ntestGetInfluencerScore");
        for(Status tweet: this.tweets) {
            System.out.println(TweetFunctions.getInfluencerScore(tweet));
        }
    }

    public void testGetEngagementScore() throws Exception {
        System.out.println("\n\ntestGetEngagementScore");
        for(Status tweet: this.tweets) {
            System.out.println(TweetFunctions.getEngagementScore(tweet));
        }
    }

    public void testGetTweetGPSCoordinates() throws Exception {
        System.out.println("\n\ntestGetTweetGPSCoordinates");
        for(Status tweet: this.tweets) {
            GeoLocation gps = TweetFunctions.getTweetGPSCoordinates(tweet);
            if(gps != null) {
                System.out.println("lat: " + gps.getLatitude() + " , long: " + gps.getLongitude());
            }
        }
    }

}