package ru.cbr.ht_ml;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer;
import org.deeplearning4j.core.storage.StatsStorage;
import org.deeplearning4j.datasets.datavec.RecordReaderDataSetIterator;
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
import org.deeplearning4j.optimize.listeners.ScoreIterationListener;
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
import org.nd4j.linalg.dataset.SplitTestAndTrain;
import org.nd4j.linalg.dataset.api.iterator.DataSetIterator;
import org.nd4j.linalg.dataset.api.preprocessor.DataNormalization;
import org.nd4j.linalg.dataset.api.preprocessor.NormalizerStandardize;
import org.nd4j.linalg.learning.config.Adam;
import org.deeplearning4j.ui.api.UIServer;
import org.nd4j.linalg.learning.config.Nesterovs;
import org.nd4j.linalg.lossfunctions.LossFunctions;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Classifier {

    private static  final int EPOCHS = 1000;
    private static final int BATCH_SIZE = 100;
    public static final String DEFAULT_MODEL_PATH = ".\\models\\cnn.model";
    public static final String DEFAULT_DATASET_PATH = ".\\datasets\\latest.ds";

    private ComputationGraph model;
    private DataSet trainSet;
    private DataSet testSet;
    private int classesCount;

    Logger log = Logger.getLogger("Classifier");

    private final Tokenizer tokenizer;

    public Classifier(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        model = null;
        File f = new File(DEFAULT_MODEL_PATH);
        if(f.exists() && !f.isDirectory()) {
            try {
                model = ComputationGraph.load(new File("..."), true);
            } catch (IOException e) {
                System.out.println("Unable to load " + DEFAULT_MODEL_PATH);
            }
        }
    }

    public void setDataSet(String path) {
        setDataSet(path, DEFAULT_DATASET_PATH);
    }

    public void setDataSet(String path, String outputPath) {
        DataSet allData = tokenizer.tokenizeDataset(path);
        //allData.save(new File(outputPath));
        classesCount = tokenizer.getLabelCount();
        trainTestSplit(allData);
    }

    public void loadDataSet() {
        loadDataSet(DEFAULT_DATASET_PATH);
    }

    public void loadDataSet(String path) {
        DataSet allData = new DataSet();
        allData.load(new File(path));
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
    }
    public void train() {
        try {
            Assert.notNull(trainSet, "No dataset specified");
            int featureCount = (int) Math.sqrt(tokenizer.getFeatureCount() / 3.);
            /**
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(0)
                    //.iterations(iterations) todo why
                    .l2(0.005) // tried 0.0001, 0.0005
                    .activation(Activation.RELU)
                    //.learningRate(0.05) // not here
                    .weightInit(WeightInit.XAVIER)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .updater(new Adam.Builder().learningRate(2e-2).build())
                    .list()
                    .layer(0, convInit("cnn1", featureCount, 64 ,  new int[]{5, 5}, new int[]{1, 1}, new int[]{0, 0}, 0))
                    .layer(1, maxPool("maxpool1", new int[]{2,2}))
                    .layer(2, conv3x3("cnn2", 64, 0))
                    .layer(3, conv3x3("cnn3", 64,1))
                    .layer(4, maxPool("maxpool2", new int[]{2,2}))
                    .layer(5, new DenseLayer.Builder().activation(Activation.RELU)
                            .nOut(512).dropOut(0.5).build())
                    .layer(6, new OutputLayer.Builder(LossFunctions.LossFunction.RECONSTRUCTION_CROSSENTROPY)
                            .nOut(CLASSES_COUNT)
                            .activation(Activation.SOFTMAX)
                            .build())
                    .setInputType(InputType.convolutional(HEIGHT, WIDTH, featureCount))
                    .build();
            **/
            model = (ComputationGraph) new ZooModelManager(new int[]{
                    3,
                    featureCount,
                    featureCount
            }, classesCount).getResNet50();
            UIServer uiServer = UIServer.getInstance();

            //Configure where the network information (gradients, activations, score vs. time etc) is to be stored
            //Then add the StatsListener to collect this information from the network, as it trains
            StatsStorage statsStorage = new FileStatsStorage(new File(System.getProperty("java.io.tmpdir"), "ui-stats.dl4j"));
            int listenerFrequency = 1;
            model.setListeners(new StatsListener(statsStorage, listenerFrequency));

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
        for (int i = 1; i < EPOCHS + 1; i++) {
            batches = dataSet.dataSetBatches(10);
            batches.forEach(net::fit);
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
}
