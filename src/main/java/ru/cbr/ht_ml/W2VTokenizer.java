package ru.cbr.ht_ml;

import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.sentenceiterator.*;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.nd4j.common.io.Assert;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class W2VTokenizer extends Tokenizer {
    public static final String DEFAULT_MODEL_PATH = ".\\models\\w2v.model";
    int featureCount;
    Logger log = Logger.getLogger("Tokenizer");
    Word2Vec w2v;

    public W2VTokenizer() {
        featureCount = 100;
        w2v = null;
        File f = new File(DEFAULT_MODEL_PATH);
        if (f.exists() && !f.isDirectory()) {
            w2v = WordVectorSerializer.readWord2VecModel(DEFAULT_MODEL_PATH);
        }
    }

    public void train(String path) {
        try {
            log.info("Load data....");
            SentenceIterator iter = new FileSentenceIterator(new File(path));
            //iter.setPreProcessor(new SentencePreProcessor() {
            //    @Override
            //    public String preProcess(String sentence) {
            //        return sentence.toLowerCase();
            //    }
            //});

            /**
             * .minWordFrequency(1)
             * .layerSize(100)
             * .seed(42)
             * .windowSize(10)
             * .iterate(iter)
             * .tokenizerFactory(t)
             * .iterations(5)
             * .epochs(100)
             *  22:45 per epoch
             */

            TokenizerFactory t = new DefaultTokenizerFactory();
            t.setTokenPreProcessor(new CommonPreprocessor());
            w2v = new Word2Vec.Builder()
                    .minWordFrequency(1)
                    .layerSize(100)
                    .seed(42)
                    .windowSize(10)
                    .iterate(iter)
                    .tokenizerFactory(t)
                    .iterations(5)
                    .epochs(2)
                    .build();

            log.info("Fitting Word2Vec model....");
            w2v.fit();
            log.info("Save vectors....");
            File directory = new File("./");
            System.out.println(directory.getAbsolutePath());
            WordVectorSerializer.writeWord2VecModel(w2v, DEFAULT_MODEL_PATH);
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }

    public void evaluate() {
        List<String> words = List.of("man", "day", "deed", "land", "son");
        evaluate(words);
    }

    public void evaluate(String word) {
        System.out.println("Closest to " + word + ":");
        Collection<String> lst = w2v.wordsNearest(word, 10);
        System.out.println(lst);
    }

    public void evaluate(Collection<String> words) {
        words.forEach(this::evaluate);
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
        Assert.notNull(w2v, "No model found. Use fit() or put w2v.model in models folder.");
        ArrayList<double[]> dataVec = Arrays.stream(data.split(",")).sequential().map(this::tokenizeWord).collect(Collectors.toCollection(ArrayList::new));
        return dataVec;
    }

    public double[] tokenizeWord(String word) {
        Assert.notNull(w2v, "No model found. Use fit() or put w2v.model in models folder.");
        WeightLookupTable weightLookupTable = w2v.lookupTable();
        double[] wordVector = w2v.getWordVector(word);
        return wordVector;
    }

    @Override
    public INDArray tokenizeString(String s) {
        return null;
    }

    @Override
    public DataSet tokenizeDataset(String path) {
        return null;
    }

    public INDArray vectorToDiagonalMatrix(INDArray array) {
        return null;
    }

    public void setFeatureCount(int featureCount) {
        this.featureCount = featureCount;
    }

    public int getFeatureCount() {
        Assert.notNull(w2v, "No model found. Use fit() or put w2v.model in models folder.");
        return w2v.vectorSize();
    }
}
