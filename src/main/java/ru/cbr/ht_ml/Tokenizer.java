package ru.cbr.ht_ml;

import org.bytedeco.opencv.presets.opencv_core;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.common.io.Assert;
import org.nd4j.linalg.api.ndarray.INDArray;

import java.io.*;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Tokenizer {
    public static final String DEFAULT_MODEL_PATH = "\\models\\w2v.model";
    Logger log = Logger.getLogger("Tokenizer");
    Word2Vec w2v;

    public Tokenizer() {
        w2v = null;
        File f = new File(DEFAULT_MODEL_PATH);
        if (f.exists() && !f.isDirectory()) {
            w2v = WordVectorSerializer.readWord2VecModel(DEFAULT_MODEL_PATH);
        }
    }

    public void train(String path) {
        try {
            log.info("Load data....");
            SentenceIterator iter = new LineSentenceIterator(new File(path));
            iter.setPreProcessor(new SentencePreProcessor() {
                @Override
                public String preProcess(String sentence) {
                    return sentence.toLowerCase();
                }
            });

            TokenizerFactory t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CommonPreprocessor());
            w2v = new Word2Vec.Builder()
                    .minWordFrequency(1)
                    .layerSize(100)
                    .seed(42)
                    .windowSize(5)
                    .iterate(iter)
                    .tokenizerFactory(t)
                    .build();

            log.info("Fitting Word2Vec model....");
            w2v.fit();
            log.info("Save vectors....");
            WordVectorSerializer.writeWord2VecModel(w2v, "\\models\\w2v.model");
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public String outputTokenizedFile(String pathToFile) {
        String outFilePath = pathToFile + ".tokenized.csv";
        try (BufferedReader reader = new BufferedReader(new FileReader(pathToFile));
             FileWriter fw = new FileWriter(outFilePath)) {
            String line = reader.readLine();
            while (line != null) {
                System.out.println(line);
                line = reader.readLine();
                fw.write(Stream.of(tokenize(line)).map(String::valueOf).collect(Collectors.joining()));
            }
            return outFilePath;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public double[] tokenize(String data) {
        Assert.notNull(w2v, "No model found. Use fit() or put w2v.model in models folder.");
        WeightLookupTable weightLookupTable = w2v.lookupTable();
        Iterator vectors = weightLookupTable.vectors();
        INDArray wordVectorMatrix = w2v.getWordVectorMatrix(data);
        double[] wordVector = w2v.getWordVector(data);
        return wordVector;
    }

    public int getFeatureCount() {
        Assert.notNull(w2v, "No model found. Use fit() or put w2v.model in models folder.");
        return w2v.vectorSize();
    }
}
