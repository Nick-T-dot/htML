package ru.cbr.ht_ml;

import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.text.documentiterator.FileLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.nd4j.linalg.dataset.DataSet;

import java.io.File;
import java.util.ArrayList;

public class TfidfTokenizer {
    private TfidfVectorizer tfidf;

    public TfidfTokenizer() {

    }

    public void train(String path) {
        //SentenceIterator iter = new LineSentenceIterator(new File(path)); // todo try other iterators
        LabelAwareIterator iter = new FileLabelAwareIterator.Builder()
                .addSourceFolder(new File(path))
                .build();
        tfidf = new TfidfVectorizer.Builder()
                .setMinWordFrequency(1)
                .allowParallelTokenization(true)
                .setIterator(iter)
                .build();
    }

    public void evaluate() {
        // todo
    }

    public String outputTokenizedFile(String pathToFile) {
        // todo
        return "";
    }

    //public ArrayList<double[]> tokenize(String data);

    public double[] tokenizeWord(String word) {
        return new double[0]; //tfidf.vectorize();
    }

    public DataSet tokenizeDataset(String path) {
        DataSet dataSet = new DataSet();
        return dataSet;
    }

    public int getFeatureCount() {
        return tfidf.vectorize().numInputs();
    }
}
