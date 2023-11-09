package ru.cbr.ht_ml;

import org.datavec.api.records.reader.RecordReader;
import org.datavec.api.records.reader.impl.csv.CSVRecordReader;
import org.datavec.api.split.FileSplit;
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
import org.deeplearning4j.text.sentenceiterator.LineSentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentenceIterator;
import org.deeplearning4j.text.sentenceiterator.SentencePreProcessor;
import org.deeplearning4j.text.tokenization.tokenizer.preprocessor.CommonPreprocessor;
import org.deeplearning4j.text.tokenization.tokenizerfactory.BertWordPieceTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.DefaultTokenizerFactory;
import org.deeplearning4j.text.tokenization.tokenizerfactory.TokenizerFactory;
import org.deeplearning4j.ui.model.stats.StatsListener;
import org.deeplearning4j.ui.model.storage.InMemoryStatsStorage;
import org.deeplearning4j.util.ModelSerializer;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.AlexNet;
import org.deeplearning4j.zoo.model.ResNet50;
import org.deeplearning4j.zoo.model.TextGenerationLSTM;
import org.nd4j.common.io.Assert;
import org.nd4j.common.io.ClassPathResource;
import org.nd4j.evaluation.classification.Evaluation;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.api.ndarray.INDArray;
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
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public class Classifier {

    private static final int CLASSES_COUNT = 11;

    private static final int HEIGHT = 1;
    private static final int WIDTH = 1;
    private static  final int EPOCHS = 1000;
    private static final int BATCH_SIZE = 100;
    public static final String DEFAULT_MODEL_PATH = "\\models\\cnn.model";

    private MultiLayerNetwork model;

    Logger log = Logger.getLogger("Classifier");

    private final Tokenizer tokenizer;

    public Classifier(Tokenizer tokenizer) {
        this.tokenizer = tokenizer;
        model = null;
        File f = new File(DEFAULT_MODEL_PATH);
        if(f.exists() && !f.isDirectory()) {
            try {
                model = MultiLayerNetwork.load(new File("..."), true);
            } catch (IOException e) {
                System.out.println("Unable to load " + DEFAULT_MODEL_PATH);
            }
        }
    }

    public void train(String path) {
        try (RecordReader recordReader = new CSVRecordReader(0, ',')) {
            String dataPath = tokenizer.outputTokenizedFile(path);
            recordReader.initialize(new FileSplit(
                    new ClassPathResource(dataPath).getFile()));
            // todo Word2Vec + ConvNet
            long seed = '0';
            int featureCount = tokenizer.getFeatureCount();
            DataSetIterator iterator = new RecordReaderDataSetIterator(
                    recordReader, 150, featureCount, CLASSES_COUNT);
            DataSet allData = iterator.next();
            allData.shuffle(42);

            DataNormalization normalizer = new NormalizerStandardize();
            normalizer.fit(allData);
            normalizer.transform(allData);

            SplitTestAndTrain testAndTrain = allData.splitTestAndTrain(0.65);
            DataSet trainingData = testAndTrain.getTrain();
            DataSet testData = testAndTrain.getTest();
            MultiLayerConfiguration conf = new NeuralNetConfiguration.Builder()
                    .seed(seed)
                    //.iterations(iterations) todo why
                    .regularization(List.of()).l2(0.005) // tried 0.0001, 0.0005
                    .activation(Activation.RELU)
                    //.learningRate(0.05) // not here
                    .weightInit(WeightInit.XAVIER)
                    .optimizationAlgo(OptimizationAlgorithm.STOCHASTIC_GRADIENT_DESCENT)
                    .updater(new Adam.Builder().learningRate(2e-2).build())
                    .list()
                    .layer(0, convInit("cnn1", featureCount, 32 ,  new int[]{5, 5}, new int[]{1, 1}, new int[]{0, 0}, 0))
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

            model = new MultiLayerNetwork(conf);
            train(model, trainingData);
            model.evaluate(testData.iterator().next().iterateWithMiniBatches());
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public void train(MultiLayerNetwork model, DataSet dataSet) {
        model.init();
        IntStream.range(1, EPOCHS + 1).forEach(epoch -> {
            model.fit(dataSet.sample(BATCH_SIZE));
        });
    }
    private ConvolutionLayer convInit(String name, int in, int out, int[] kernel, int[] stride, int[] pad, double bias) {
        return new ConvolutionLayer.Builder(kernel, stride, pad).name(name).nIn(in).nOut(out).biasInit(bias).build();
    }

    private ConvolutionLayer conv3x3(String name, int out, double bias) {
        return new ConvolutionLayer.Builder(new int[]{3,3}, new int[] {1,1}, new int[] {1,1}).name(name).nOut(out).biasInit(bias).build();
    }

    private SubsamplingLayer maxPool(String name, int[] kernel) {
        return new SubsamplingLayer.Builder(kernel, new int[]{2,2}).name(name).build();
    }

}
