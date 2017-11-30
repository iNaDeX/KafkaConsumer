import twitter4j.Status;
import twitter4j.GeoLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import java.lang.Math;

public class TweetFunctions {
    // Provide (the most likely) Country of Origin of the Tweet
    public static String getTweetCountry(Status tweet) {
        String country = null;

        /*
        TODO: use as favorite metric the GPS Coordinates optionnally provided in the tweet (tweet 'coordinates' field) and extract country
        free Public Domain project that does that: https://github.com/che0/countries
        or google maps API (rate limited & on the internet = slow)
         */

        // fallback to place "associated" with tweet (not necessarily true place)
        if(tweet.getPlace() != null && tweet.getPlace().getCountry() != null) {
            country = tweet.getPlace().getCountry();
        }

        // TODO: fallback to user profile location, check that it is not stupid (e.g. 'Mars') and extract country
        return country;
    }

    // get the hashtags present in the tweet
    public static String[] getHashtags(Status tweet) {
        return Arrays.stream(tweet.getHashtagEntities()).map(hashtagEntity -> hashtagEntity.getText()).toArray(String[]::new);
    }

    // get the keywords / topics mentionned in the tweet
    public static String[] getKeywords(Status tweet) {
        throw new UnsupportedOperationException();
    }

    // give a score to the tweet to measure whether the Author (User) is worth sponsoring
    // the higher the score, the better
    public static double getInfluencerScore(Status tweet) {
        int nbFollowers = tweet.getUser().getFollowersCount();
        int engagement = TweetFunctions.getEngagementScore(tweet);
        return Math.sqrt(nbFollowers * engagement); // geometric mean
    }

    // get the sentiment score of the tweet (negative, neutral, positive) in a range [-1;1]
    // the higher, the more positive the sentiment
    public static double getSentimentScore(Status tweet) {
        throw new UnsupportedOperationException();
    }

    // give a score to how well a tweet is engaging with users of Twitter (popularity of the tweet)
    // the higher, the better
    public static int getEngagementScore(Status tweet) {
        int retweets = tweet.getRetweetCount();
        int favorites = tweet.getFavoriteCount();
        /* TODO: Twitter4J doesn't allow to retrieve reply_count or quote_count although I have these fields in my JSONs..
        -> it seems these fields are not even supposed to be accessible on free Twitter Streaming API (information dated of 18 October 2017):
        --> weird
        https://twittercommunity.com/t/reply-count-quote-count-not-available-in-statuses-lookup-answer/95241
        https://github.com/yusuke/twitter4j/blob/master/twitter4j-core/src/main/java/twitter4j/Status.java
        https://github.com/yusuke/twitter4j/blob/master/twitter4j-core/src/internal-json/java/twitter4j/StatusJSONImpl.java
        */
        return retweets+favorites;
    }

    public static GeoLocation getTweetGPSCoordinates(Status tweet) {
        GeoLocation coordinates = null;

        // favorite metric
        if(tweet.getGeoLocation() != null) {
            coordinates = tweet.getGeoLocation();
        }

        // fallback to place "associated" with tweet and extract center GPS coordinates from bounding box
        else if(tweet.getPlace() != null && tweet.getPlace().getBoundingBoxCoordinates() != null) {
            GeoLocation[] boundingBox =  tweet.getPlace().getBoundingBoxCoordinates()[0]; // get bounding box
            // compute center of bounding box
            double centerLatitude = Arrays.stream(boundingBox).map(location -> location.getLatitude())
                                        .collect(Collectors.averagingDouble(d -> d));
            double centerLongitude = Arrays.stream(boundingBox).map(location -> location.getLongitude())
                                        .collect(Collectors.averagingDouble(d -> d));

            coordinates = new GeoLocation(centerLatitude, centerLongitude);
        }
        return coordinates;
    }
}
