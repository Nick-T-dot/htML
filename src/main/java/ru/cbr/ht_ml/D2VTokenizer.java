package ru.cbr.ht_ml;

import org.bytedeco.opencv.presets.opencv_core;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.paragraphvectors.ParagraphVectors;
import org.deeplearning4j.models.sequencevectors.interfaces.VectorsListener;
import org.deeplearning4j.models.sequencevectors.listeners.SerializingListener;
import org.deeplearning4j.models.word2vec.VocabWord;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.text.documentiterator.FileDocumentIterator;
import org.deeplearning4j.text.documentiterator.FileLabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelAwareIterator;
import org.deeplearning4j.text.documentiterator.LabelledDocument;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.sentenceiterator.labelaware.LabelAwareFileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.labelaware.LabelAwareSentenceIterator;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.ui.api.UIServer;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.nd4j.autodiff.listeners.impl.ScoreListener;
import org.nd4j.common.io.Assert;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class D2VTokenizer extends Tokenizer {
    public static final String DEFAULT_MODEL_PATH = ".\\models\\d2v.model";
    public static final int FEATURE_COUNT = 1000;
    Logger log = Logger.getLogger("Tokenizer");
    ParagraphVectors d2v;
    TokenizerFactory t;
    LabelManager labelManager;

    public D2VTokenizer() {
        this.labelManager = new LabelManager();
        d2v = null;
        t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
        try {
            d2v = WordVectorSerializer.readParagraphVectors(DEFAULT_MODEL_PATH);
            d2v.setTokenizerFactory(t);
        } catch (IOException e) {
            System.out.println("No D2V model found in path " + DEFAULT_MODEL_PATH);
        }
    }

    public void train(String path) {
        try {
            log.info("Load data....");
            LabelAwareIterator iter = new FileLabelAwareIterator.Builder()
                    .addSourceFolder(new File(path))
                    .build();
            //iter.setPreProcessor(new SentencePreProcessor() {
            //    @Override
            //    public String preProcess(String sentence) {
            //        return sentence.toLowerCase();
           //     }
            //});

            d2v = new ParagraphVectors.Builder()
                    .minWordFrequency(1)
                    .layerSize(FEATURE_COUNT)
                    //.learningRate(0.025)
                    .epochs(1)
                    .iterations(1)
                    //.batchSize(100)
                    .minLearningRate(0.001)
                    .useAdaGrad(true)
                    .trainWordVectors(true)
                    .stopWords(new ArrayList<String>())
                    .windowSize(10)
                    .iterate(iter)
                    .tokenizerFactory(t)
                    .build();

            log.info("Fitting Word2Vec model....");
            d2v.fit();
            log.info("Save vectors....");
            WordVectorSerializer.writeWord2VecModel(d2v, DEFAULT_MODEL_PATH);
            log.info("Model saved to " + DEFAULT_MODEL_PATH);
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

    public INDArray tokenizeString(String s) {
        return d2v.inferVector(s);
    }

    public double[] tokenizeWord(String word) {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        double[] wordVector = d2v.getWordVector(word);
        return wordVector;
    }

    @Override
    public DataSet tokenizeDataset(String path) {
        File datasetDir = new File(path);
        LabelAwareIterator iter = new FileLabelAwareIterator.Builder()
                .addSourceFolder(datasetDir)
                .build();
        ArrayList<String> nonUniqueLabels = Arrays.stream(datasetDir.listFiles()).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
        nonUniqueLabels.forEach(s -> labelManager.tryAddLabels(s));
        LabelledDocument doc;
        INDArray data;
        INDArray labels;
        List<DataSet> dataSets = new ArrayList<>();
        while (iter.hasNext()) {
            doc = iter.nextDocument();
            //data = data.addRowVector(tokenizeString(doc.getContent()));
            dataSets.add(new DataSet(tokenizeString(doc.getContent()), Nd4j.create(labelManager.getLabelIndexes(doc.getLabels()))));
            //data = tokenizeString(doc.getContent());
            //labels = Nd4j.create(labelManager.getLabelIndexes(doc.getLabels()));
        }
        DataSet dataSet = DataSet.merge(dataSets);
        return dataSet;
    }

    public int getFeatureCount() {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        return d2v.vectorSize();
    }
}
