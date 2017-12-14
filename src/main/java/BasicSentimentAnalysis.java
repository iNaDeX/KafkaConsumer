import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class BasicSentimentAnalysis {
    private List<String> positiveWords = new ArrayList<>();
    private List<String> negativeWords = new ArrayList<>();

    public BasicSentimentAnalysis(String negativeKeywordsPath, String positiveKeywordsPath) throws IOException {
        BufferedReader negativeWordsReader;
        BufferedReader positiveWordsReader;

        negativeWordsReader = new BufferedReader(new FileReader(new File(negativeKeywordsPath)));
        positiveWordsReader = new BufferedReader(new FileReader(new File(positiveKeywordsPath)));

        String word; // tmp
        // build lists
        while ((word = negativeWordsReader.readLine()) != null) {
            negativeWords.add(word);
        }
        while ((word = positiveWordsReader.readLine()) != null) {
            positiveWords.add(word);
        }

        negativeWordsReader.close();
        positiveWordsReader.close();
    }

    // get the sentiment score of the tweet (negative, neutral, positive) in a range [-1;1]
    // the higher, the more positive the sentiment
    public double getSentimentScore(String text) {
        int score = 0;
        int negCounter = 0;
        int posCounter = 0;
        text = text.toLowerCase().trim().replaceAll("[^a-zA-Z0-9\\s]", "");
        String[] words = text.split(" ");

        // check if the current word appears in our reference lists...
        for (int i = 0; i < words.length; i++) {
            if (positiveWords.contains(words[i])) {
                posCounter++;
            }
            if (negativeWords.contains(words[i])) {
                negCounter++;
            }
        }

        // compute total result
        score = posCounter - negCounter;

        return Math.tanh(score);
    }
}
