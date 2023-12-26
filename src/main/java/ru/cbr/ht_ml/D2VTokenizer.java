package ru.cbr.ht_ml;

import org.bytedeco.opencv.presets.opencv_core;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.iterator.impl.EmnistDataSetIterator;
import org.deeplearning4j.datasets.iterator.impl.MnistDataSetIterator;
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
import org.eclipse.deeplearning4j.resources.utils.EMnistSet;
import org.nd4j.autodiff.listeners.impl.ScoreListener;
import org.nd4j.common.io.Assert;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.factory.Nd4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class D2VTokenizer extends Tokenizer {
    public static final String DEFAULT_MODEL_PATH = ".\\models\\d2v.model";
    public final int featureCount;
    Logger log = Logger.getLogger("Tokenizer");
    ParagraphVectors d2v;
    TokenizerFactory t;
    LabelManager labelManager;

    public D2VTokenizer(int featureCount) {
        this.featureCount = featureCount;
        this.labelManager = new LabelManager();
        d2v = null;
        t = new DefaultTokenizerFactory();
        t.setTokenPreProcessor(new CommonPreprocessor());
    }

    public void tryLoadModel() {
        try {
            log.info("Tokenizer: Loading model...");
            Instant start = Instant.now();
            d2v = WordVectorSerializer.readParagraphVectors(DEFAULT_MODEL_PATH);
            Instant end = Instant.now();
            d2v.setTokenizerFactory(t);
            log.info("Tokenizer: Loaded model " + DEFAULT_MODEL_PATH);
            log.info("Tokenizer: Loaded in " + Duration.between(start, end).getSeconds() + " s");
        } catch (IOException e) {
            log.info("Tokenizer: No D2V model found in path " + DEFAULT_MODEL_PATH);
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
                    .layerSize(featureCount)
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

    @Override
    public void trainParts(String path) {
        log.info("Load data....");
        File baseDir = new File(path);
        List<File> parts = List.of(baseDir.listFiles());
        LabelAwareIterator iter;
        log.info("Fitting Word2Vec model....");
        int epoch = 0;
        String savePath;
        for (File part : parts) {
            log.info("Batch " + part.getName());
            iter = new FileLabelAwareIterator.Builder()
                    .addSourceFolder(part)
                    .build();
            if (d2v == null) {
                d2v = new ParagraphVectors.Builder()
                        .minWordFrequency(1)
                        .layerSize(featureCount)
                        .epochs(1)
                        //.iterations(10)
                        .batchSize(1)
                        .minLearningRate(0.001)
                        //.useAdaGrad(true)
                        .trainWordVectors(true)
                        .stopWords(new ArrayList<String>())
                        .iterate(iter)
                        .windowSize(10)
                        .tokenizerFactory(t)
                        .build();
            } else {
                d2v.setLabelAwareIterator(iter);
            }
            d2v.fit();
            log.info("Save epoch " + String.valueOf(++epoch) + "....");
            savePath = ".\\models\\d2v_epoch_" + epoch + ".model";
            WordVectorSerializer.writeWord2VecModel(d2v, savePath);
            log.info("Epoch " + epoch + " saved to " + savePath);
        }
        log.info("Save vectors....");
        WordVectorSerializer.writeWord2VecModel(d2v, DEFAULT_MODEL_PATH);
        log.info("Model saved to " + DEFAULT_MODEL_PATH);
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
        System.out.println(datasetDir.listFiles());
        int fileCount = Arrays.stream(datasetDir.listFiles()).sequential().map(dir ->
                dir.listFiles().length).reduce(0, Integer::sum);
        LabelAwareIterator iter = new FileLabelAwareIterator.Builder()
                .addSourceFolder(datasetDir)
                .build();
        ArrayList<String> nonUniqueLabels = Arrays.stream(datasetDir.listFiles()).map(File::getName).collect(Collectors.toCollection(ArrayList::new));
        nonUniqueLabels.forEach(s -> labelManager.tryAddLabels(s));
        LabelledDocument doc;
        double[][] labels;
        List<DataSet> dataSets = new ArrayList<>();
        DataSet dataSet;
        int rowNum = 0;
        int side = (int) Math.sqrt(featureCount / 3.);
        while (iter.hasNext()) {
            doc = iter.nextDocument();
            labels = labelManager.getLabelIndexes(doc.getLabels());
            dataSets.add(new DataSet(
                    vectorTo3dMatrix(tokenizeString(doc.getContent())).reshape(1, 3, side, side),
                    new NDArray(labels)
            ));
            log.info(++rowNum + "/" + fileCount);
        }
        if (dataSets.size() > 1) {
            dataSet = DataSet.merge(dataSets);
        } else {
            dataSet = dataSets.get(0);
        }
        DataNormalization normalizer = new NormalizerStandardize();
        normalizer.fit(dataSet);
        normalizer.transform(dataSet);
        return dataSet;
    }

    public INDArray vectorToDiagonalMatrix(INDArray array) {
        INDArray matrix = new NDArray(new int[] { (int) array.length(), (int) array.length()});
        IntStream.range(0, (int) array.length()).forEach(
                i -> matrix.putScalar(new int[]{i,i},
                        array.getDouble(i))
        );
        INDArray fatMatrix = new NDArray(new int[] { 3, (int) array.length(), (int) array.length()});
        IntStream.range(0, 3).forEach(
                i -> fatMatrix.putSlice(i, matrix)
        );
        return fatMatrix;
    }

    public INDArray vectorToMatrix(INDArray array) {
        double dside = Math.sqrt(array.length());
        Assert.isTrue((dside - Math.round(dside)) == 0, "Array length is not a square.");
        int side = (int) dside;
        INDArray matrix = new NDArray(new int[] { side, side });
        for (int i = 0; i < side; i++) {
            for (int j = 0; j < side; j++) {
                matrix.putScalar(new int[]{i, i%2==0 ? i : side - i - 1}, array.getDouble(i * side + j));
            }
        }
        INDArray fatMatrix = new NDArray(new int[] { 3, side, side });
        IntStream.range(0, 3).forEach(
                i -> fatMatrix.putSlice(i, matrix)
        );
        return fatMatrix;
    }

    public INDArray vectorTo3dMatrix(INDArray array) {
        Assert.isTrue(array.length() % 3 == 0, "Array length is not a multiple of 3!");
        double dside = Math.sqrt(array.length() / 3.);
        Assert.isTrue((dside - Math.round(dside)) == 0, "Array length is not a square!");
        int side = (int) dside;
        INDArray matrix;
        INDArray fatMatrix = new NDArray(new int[] { 3, side, side });
        for (int d = 0; d < 3; d++) {
            matrix = new NDArray(new int[] { side, side });
            for (int h = 0; h < side; h++) {
                for (int w = 0; w < side; w++) {
                    matrix.putScalar(new int[]{d % 2 == 0 ? h : side - h - 1, h % 2 == 0 ? w : side - w - 1}, array.getDouble(d * side * side + h * side + w));
                }
            }
            fatMatrix.putSlice(d, matrix);
        }
        return fatMatrix;
    }

    public int getFeatureCount() {
        Assert.notNull(d2v, "No model found. Use fit() or put w2v.model in models folder.");
        return featureCount;
    }

    public int getLabelCount() {
        return labelManager.getLabelCount();
    }
    public int getLabelCount(INDArray array) {
        return (int) array.getRow(0).length();
    }

    public static void modelToJson(String path, String outPath) {
        try {
            String modelName = path.substring(path.lastIndexOf("\\") + 1, path.lastIndexOf('.'));
            ParagraphVectors temp = WordVectorSerializer.readParagraphVectors(path);
            String filePath = String.valueOf(Files.createFile(Paths.get(outPath + "\\" + modelName + ".json")));
            FileWriter fw = new FileWriter(filePath, true);
            BufferedWriter bw = new BufferedWriter(fw);
            bw.write(temp.toJson());
            bw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
