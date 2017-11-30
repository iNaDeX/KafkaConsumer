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
        free Public Domain project that does that: https://github.com/AReallyGoodName/OfflineReverseGeocode
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
      String fulltext = null;
      String[] words = null;
      // TODO: load a bigger stopword dictionnary from file
      String[] stopwords = {"", " ", "a", "about", "above", "above", "across", "after", "afterwards", "again", "against", "all", "almost", "alone", "along", "already", "also","although","always","am","among", "amongst", "amoungst", "amount",  "an", "and", "another", "any","anyhow","anyone","anything","anyway", "anywhere", "are", "around", "as",  "at", "back","be","became", "because","become","becomes", "becoming", "been", "before", "beforehand", "behind", "being", "below", "beside", "besides", "between", "beyond", "bill", "both", "bottom","but", "by", "call", "can", "cannot", "cant", "co", "con", "could", "couldnt", "cry", "d", "de", "describe", "detail", "do", "done", "down", "due", "during", "each", "eg", "eight", "either", "eleven","else", "elsewhere", "empty", "enough", "etc", "even", "ever", "every", "everyone", "everything", "everywhere", "except", "few", "fifteen", "fify", "fill", "find", "fire", "first", "five", "for", "former", "formerly", "forty", "found", "four", "from", "front", "full", "further", "get", "give", "go", "got", "gonna", "gotta", "had", "has", "hasnt", "have", "he", "hence", "her", "here", "hereafter", "hereby", "herein", "hereupon", "hers", "herself", "him", "himself", "his", "how", "however", "hundred", "i", "ie", "if", "in", "inc", "indeed", "interest", "into", "is", "it", "its", "itself", "keep", "last", "latter", "latterly", "least", "less", "ll", "ltd", "m", "made", "many", "may", "me", "meanwhile", "might", "mill", "mine", "more", "moreover", "most", "mostly", "move", "much", "must", "my", "myself", "name", "namely", "neither", "never", "nevertheless", "next", "nine", "no", "nobody", "none", "noone", "nor", "not", "nothing", "now", "nowhere", "of", "off", "often", "on", "once", "one", "only", "onto", "or", "other", "others", "otherwise", "our", "ours", "ourselves", "out", "over", "own","part", "per", "perhaps", "please", "pm", "put", "rather", "re", "rt", "s", "same", "see", "seem", "seemed", "seeming", "seems", "serious", "several", "she", "should", "show", "side", "since", "sincere", "six", "sixty", "so", "some", "somehow", "someone", "something", "sometime", "sometimes", "somewhere", "still", "such", "system", "take", "ten", "than", "that", "the", "their", "them", "themselves", "then", "thence", "there", "thereafter", "thereby", "therefore", "therein", "thereupon", "these", "they", "thickv", "thin", "third", "this", "those", "though", "three", "through", "throughout", "thru", "thus", "til", "to", "together", "too", "top", "toward", "towards", "twelve", "twenty", "two", "un", "under", "until", "up", "upon", "us", "ve", "very", "via", "was", "we", "well", "were", "what", "whatever", "when", "whence", "whenever", "where", "whereafter", "whereas", "whereby", "wherein", "whereupon", "wherever", "whether", "which", "while", "whither", "who", "whoever", "whole", "whom", "whose", "why", "will", "with", "within", "without", "would", "yet", "you", "your", "yours", "yourself", "yourselves", "the"};
      ArrayList<String> keywordslist = new ArrayList<String>();

      fulltext = tweet.getText();
      fulltext = fulltext.toLowerCase(); //transform to lowercase
      fulltext = fulltext.replaceAll("http\\S+", ""); //remove urls
      fulltext = fulltext.replaceAll("@\\S+", ""); //remove user mentions
      fulltext = fulltext.replaceAll("\\p{Punct}", ""); //remove punctuation
      fulltext = fulltext.replaceAll("\\W", " "); //remove non words
      fulltext = fulltext.replaceAll("[^a-z]", " "); //remove non letter words
      // split on spaces
      words = fulltext.split("\\s+");
      // remove stop words
      for(int i = 0; i < words.length; i++)
      {
        if(!Arrays.asList(stopwords).contains(words[i]))
          keywordslist.add(words[i]);
      }
      String[] keywords = new String[keywordslist.size()];
      keywords = keywordslist.toArray(keywords);
      return keywords;
    }

    // give a score to the tweet to measure whether the Author (User) is worth sponsoring
    // the higher the score, the better
    public static double getInfluencerScore(Status tweet) {
        int nbFollowers = tweet.getUser().getFollowersCount();
        int engagement = TweetFunctions.getEngagementScore(tweet);
        return Math.sqrt(nbFollowers * engagement); // geometric mean
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
