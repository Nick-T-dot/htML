package ru.cbr.ht_ml;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
import org.deeplearning4j.datasets.iterator.BaseDatasetIterator;
import org.deeplearning4j.datasets.iterator.ExistingDataSetIterator;
import org.deeplearning4j.datasets.iterator.INDArrayDataSetIterator;
import org.deeplearning4j.datasets.iterator.loader.DataSetLoaderIterator;
import org.deeplearning4j.models.embeddings.WeightLookupTable;
import org.deeplearning4j.models.embeddings.loader.WordVectorSerializer;
import org.deeplearning4j.models.word2vec.Word2Vec;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.MultiLayerConfiguration;
import org.deeplearning4j.nn.conf.NeuralNetConfiguration;
import org.deeplearning4j.nn.conf.Updater;
import org.deeplearning4j.nn.conf.inputs.InputType;
import org.deeplearning4j.nn.conf.layers.ConvolutionLayer;
import org.deeplearning4j.nn.conf.layers.DenseLayer;
import org.deeplearning4j.nn.conf.layers.OutputLayer;
import org.deeplearning4j.nn.conf.layers.SubsamplingLayer;
import org.deeplearning4j.nn.graph.ComputationGraph;
import org.deeplearning4j.nn.multilayer.MultiLayerNetwork;
import org.deeplearning4j.nn.transferlearning.FineTuneConfiguration;
import org.deeplearning4j.nn.transferlearning.TransferLearning;
import org.deeplearning4j.nn.transferlearning.TransferLearningHelper;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.optimize.api.InvocationType;
import org.deeplearning4j.optimize.listeners.CheckpointListener;
import org.deeplearning4j.optimize.listeners.EvaluativeListener;
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
import org.deeplearning4j.text.documentiterator.SimpleLabelAwareIterator;
import org.deeplearning4j.text.sentenceiterator.FileSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.BertWordPieceTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.FileStatsStorage;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.AlexNet;
import org.deeplearning4j.zoo.model.ResNet50;
import org.deeplearning4j.zoo.model.TextGenerationLSTM;
import org.nd4j.common.io.Assert;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.nd4j.linalg.dataset.DataSet;
import org.nd4j.linalg.dataset.MultiDataSet;
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.iterator.SamplingDataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Adam;
import org.deeplearning4j.ui.api.UIServer;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;
import org.opencv.core.Core;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Classifier {

    private static  final int EPOCHS = 1000;
    private static final int BATCH_SIZE = 250;
    public static final String DEFAULT_MODEL_PATH = ".\\models\\cnn.model";
    public static final String DEFAULT_DATASET_PATH = ".\\datasets\\latest.ds";

    private ComputationGraph model;
    private DataSet trainSet;
    private DataSet testSet;
    private int classesCount;

    private ZooModelManager modelManager;

    public enum CoreModel {RESNET50, UNET}

    private CoreModel selectedCoreModel;

    Logger log = Logger.getLogger("Classifier");

    private Tokenizer tokenizer;

    public Classifier(Tokenizer tokenizer) {
        selectedCoreModel = CoreModel.RESNET50;
        this.tokenizer = tokenizer;
        int featureCount = (int) Math.sqrt(tokenizer.getFeatureCount() / 3.);
        modelManager = new ZooModelManager(new int[]{
                3,
                featureCount,
                featureCount
        }, classesCount);
        model = null;
        File f = new File(DEFAULT_MODEL_PATH);
        if(f.exists() && !f.isDirectory()) {
            try {
                model = ComputationGraph.load(f, true);
            } catch (IOException e) {
                System.out.println("Unable to load " + DEFAULT_MODEL_PATH);
            }
        }
    }

    private Classifier(int featureCount, int classesCount) {
        int side = (int) Math.sqrt(featureCount / 3.);
        modelManager = new ZooModelManager(new int[]{
                3,
                side,
                side
        }, classesCount);
    }

    public void loadModel(String path) {
        try {
            model = ComputationGraph.load(new File(path), true);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void setDataSet(String path) {
        setDataSet(path, DEFAULT_DATASET_PATH);
    }

    public void setDataSet(String path, String outputPath) {
        log.info("Tokenizing dataset...");
        DataSet allData = tokenizer.tokenizeDataset(path);
        allData.save(new File(outputPath));
        classesCount = tokenizer.getLabelCount();
        trainTestSplit(allData);
        log.info("Dataset done.");
    }

    public void loadDataSet() {
        loadDataSet(DEFAULT_DATASET_PATH);
    }

    public void loadDataSet(boolean normalize) {
        loadDataSet(DEFAULT_DATASET_PATH, normalize);
    }

    public void loadDataSet(String path) {
        loadDataSet(path, false);
    }

    public void loadDataSet(String path, boolean normalize) {
        DataSet allData = new DataSet();
        allData.load(new File(path));
        if (normalize) {
            DataNormalization normalizer = new NormalizerStandardize();
            normalizer.fit(allData);
            normalizer.transform(allData);
        }
        classesCount = tokenizer.getLabelCount(allData.getLabels());
        trainTestSplit(allData);
    }

    public void loadDataSets(String folderPath) {
        List<DataSet> dataSets = new ArrayList<>();
        File dir = new File(folderPath);
        Assert.isTrue(dir.isDirectory(), folderPath + " is not a directory!");
        Arrays.stream(dir.listFiles()).sequential().forEach(file -> {
            dataSets.add(new DataSet());
            log.info("Loading " + file.getName());
            dataSets.get(dataSets.size() - 1).load(file);
        });
        DataSet allData = DataSet.merge(dataSets);
        classesCount = tokenizer.getLabelCount(allData.getLabels());
        trainTestSplit(allData);
    }

    private void trainTestSplit(DataSet data) {
        DataSet allData = data.copy();
        allData.shuffle(42);
        SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.65);
        trainSet = testAndTrain.getTrain();
        testSet = testAndTrain.getTest();
        allData.shuffle(42);
    }
    public void train() {
        try {
            Assert.notNull(trainSet, "No dataset specified");
            switch (selectedCoreModel) {
                case RESNET50 -> model = (ComputationGraph) modelManager.getResNet50();
                case UNET -> model = (ComputationGraph) modelManager.getUNet();
            }

            UIServer uiServer = UIServer.getInstance();
            //Configure where the network information (gradients, activations, score vs. time etc) is to be stored
            //Then add the StatsListener to collect this information from the network, as it trains
            log.info("Writing training stats to " + System.getProperty("java.io.tmpdir"));
            StatsStorage statsStorage = new FileStatsStorage(new File(System.getProperty("java.io.tmpdir"), "ui-stats.dl4j"));
            CheckpointListener checkpointListener = new CheckpointListener.Builder("C:\\Users\\Tsvetkov_NK\\Documents\\IdeaProjects\\MLTest\\models")
                    .keepLast(2)
                    .saveEvery(1, TimeUnit.HOURS)
                    .logSaving(true)
                    .deleteExisting(false)
                    .build();
            StatsListener statsListener = new StatsListener(statsStorage, 1);
            EvaluativeListener evaluativeListener = new EvaluativeListener(testSet, 10, InvocationType.ITERATION_END);
            ScoreIterationListener scoreIterationListener = new ScoreIterationListener();
            model.setListeners(checkpointListener, statsListener, evaluativeListener, scoreIterationListener);

            //Attach the StatsStorage instance to the UI: this allows the contents of the StatsStorage to be visualized
            uiServer.attach(statsStorage);
            train(model, trainSet);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void train(ComputationGraph net, DataSet dataSet) throws IOException {
        net.init();
        log.info("Training...");
        List<DataSet> batches;
        Instant start ,end;
        for (int i = 1; i < EPOCHS + 1; i++) {
            log.info("EPOCH " + i + " / " + (EPOCHS));
            net.incrementEpochCount();
            //model.evaluate(new SamplingDataSetIterator(testSet,32 ,testSet.numExamples()));
            start = Instant.now();
            batches = dataSet.dataSetBatches(BATCH_SIZE);
            batches.forEach(net::fit);
            //net.fit(trainSet);
            end = Instant.now();

            log.info("Epoch " + i + " finished in " + Duration.between(start, end).getSeconds() + " s");
        }
        log.info("Saving model...");
        model.save(new File(DEFAULT_MODEL_PATH));
        log.info("Model saved to " + DEFAULT_MODEL_PATH);
    }

    public void test() {
        Assert.notNull(testSet, "No dataset specified");
        Assert.notNull(model, "Model not initialized");
        test(model, testSet);
    }

    private void test(ComputationGraph model, DataSet dataSet) {
        model.evaluate(dataSet.iterateWithMiniBatches());
    }

    public void setSelectedCoreModel(CoreModel coreModel) {
        selectedCoreModel = coreModel;
    }

    public static class ClassifierBuilder {
        private CoreModel model = null;
        private Tokenizer tokenizer = null;
        private int featureCount = 0;
        private int classesCount = 0;
        private boolean mustNormalize = false;
        String dataSetPath = null;
        public ClassifierBuilder coreModel(CoreModel model) {
            this.model = model;
            return this;
        }

        public ClassifierBuilder tokenizer(Tokenizer tokenizer) {
            this.tokenizer = tokenizer;
            return this;
        }

        public ClassifierBuilder featureCount(int featureCount) {
            this.featureCount = featureCount;
            return this;
        }

        public ClassifierBuilder classesCount(int classesCount) {
            this.classesCount = classesCount;
            return this;
        }

        public ClassifierBuilder dataSet (String dataSetPath) {
            this.dataSetPath = dataSetPath;
            return this;
        }

        public ClassifierBuilder normalize (boolean mustNormalize) {
            this.mustNormalize = mustNormalize;
            return this;
        }

        public Classifier build() {
            Classifier classifier;
            classifier = new Classifier(featureCount, classesCount);
            classifier.setTokenizer(tokenizer);
            classifier.setSelectedCoreModel(model);
            if (dataSetPath != null) {
                classifier.setDataSet(dataSetPath);
            } else {
                classifier.loadDataSet(mustNormalize);
            }
            return classifier;
        }
    }

    private void setTokenizer(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
    }

    public static ClassifierBuilder builder() {
        return new ClassifierBuilder();
    }


}
