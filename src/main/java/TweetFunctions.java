import twitter4j.Status;
import twitter4j.GeoLocation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TweetFunctions {
    public static String getTweetCountry(Status tweet) {
        String country = null;

        // TODO: use as favorite metric the GPS Coordinates optionnally provided in the tweet (tweet 'coordinates' field) and extract country
        // google maps API does it for example

        // fallback to place "associated" with tweet (not necessarily true place)
        if(tweet.getPlace() != null && tweet.getPlace().getCountry() != null) {
            country = tweet.getPlace().getCountry();
        }

        // TODO: fallback to user profile location, check that it is not stupid (e.g. 'Mars') and extract country
        return country;
    }

    public static String[] getHashtags(Status tweet) {
        return Arrays.stream(tweet.getHashtagEntities()).map(hashtagEntity -> hashtagEntity.getText()).toArray(String[]::new);
    }

    public static String[] getKeywords(Status tweet) {
        throw new UnsupportedOperationException();
    }

    public static double getSentiment(Status tweet) {
        throw new UnsupportedOperationException();
    }

    public static int getEngagement(Status tweet) {
        throw new UnsupportedOperationException();
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
            // TODO: use Java 8 Stream API
            List<Double> boundingBoxLatitudes = new ArrayList<>();
            List<Double> boundingBoxLongitudes = new ArrayList<>();
            for(GeoLocation geo : boundingBox) {
                boundingBoxLatitudes.add(geo.getLatitude());
                boundingBoxLongitudes.add(geo.getLongitude());
            }
            // compute center of bounding box
            double centerLatitude = boundingBoxLatitudes.stream().collect(Collectors.averagingDouble(d -> d));
            double centerLongitude = boundingBoxLongitudes.stream().collect(Collectors.averagingDouble(d -> d));
            coordinates = new GeoLocation(centerLatitude, centerLongitude);
        }
        return coordinates;
    }
}
