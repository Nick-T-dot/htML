package ru.cbr.ht_ml;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.sentenceiterator.labelaware.LabelAwareFileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.labelaware.LabelAwareSentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.common.io.Assert;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class D2VTokenizer extends Tokenizer {
    public static final String DEFAULT_MODEL_PATH = ".\\models\\d2v.model";
    Logger log = Logger.getLogger("Tokenizer");
    ParagraphVectors d2v;

    public D2VTokenizer() {
        d2v = null;
        File f = new File(DEFAULT_MODEL_PATH);
        try {
            d2v = WordVectorSerializer.readParagraphVectors(DEFAULT_MODEL_PATH);
        } catch (IOException e) {
            System.out.println("No D2V model found in path " + DEFAULT_MODEL_PATH);
        }
    }

    public void train(String path) {
        try {
            log.info("Load data....");
            LabelAwareSentenceIterator iter = new LabelAwareFileSentenceIterator(new File(path));
            iter.setPreProcessor(new SentencePreProcessor() {
                @Override
                public String preProcess(String sentence) {
                    return sentence.toLowerCase();
                }
            });

            TokenizerFactory t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CommonPreprocessor());
            d2v = new ParagraphVectors.Builder()
                    .minWordFrequency(1).labels(Arrays.asList("negative", "positive"))
                    .layerSize(100)
                    //.learningRate(0.025)
                    .epochs(5)
                    .iterations(5)
                    .useAdaGrad(true)
                    .stopWords(new ArrayList<String>())
                    .windowSize(50).iterate(iter).tokenizerFactory(t).build();

            log.info("Fitting Word2Vec model....");
            d2v.fit();
            log.info("Save vectors....");
            File directory = new File("./");
            System.out.println(directory.getAbsolutePath());

            WordVectorSerializer.writeWord2VecModel(d2v, DEFAULT_MODEL_PATH);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void evaluate() {
        // todo
    }

    public String outputTokenizedFile(String pathToFile) {
        String outFilePath = pathToFile + ".tokenized." + pathToFile.split("\\.")[1];
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
            log.info(e.toString());
            return null;
        }
    }

    public ArrayList<double[]> tokenize(String data) {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        ArrayList<double[]> dataVec = Arrays.stream(data.split(",")).sequential().map(this::tokenizeWord).collect(Collectors.toCollection(ArrayList::new));
        return dataVec;
    }

    public double[] tokenizeWord(String word) {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        WeightLookupTable weightLookupTable = d2v.lookupTable();
        double[] wordVector = d2v.getWordVector(word);
        return wordVector;
    }

    public int getFeatureCount() {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        return d2v.vectorSize();
    }
}
