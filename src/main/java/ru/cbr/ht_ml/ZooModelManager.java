package ru.cbr.ht_ml;

import org.deeplearning4j.nn.api.Model;
import org.deeplearning4j.nn.api.OptimizationAlgorithm;
import org.deeplearning4j.nn.conf.*;
import org.deeplearning4j.nn.updater.UpdaterBlock;
import org.deeplearning4j.nn.weights.IWeightInit;
import org.deeplearning4j.nn.weights.WeightInit;
import org.deeplearning4j.zoo.InstantiableModel;
import org.deeplearning4j.zoo.PretrainedType;
import org.deeplearning4j.zoo.ZooModel;
import org.deeplearning4j.zoo.model.ResNet50;
import org.deeplearning4j.zoo.model.UNet;
import org.deeplearning4j.zoo.model.VGG16;
import org.nd4j.linalg.activations.Activation;
import org.nd4j.linalg.learning.config.Adam;
import org.nd4j.linalg.learning.config.IUpdater;
import org.nd4j.weightinit.impl.ZeroInitScheme;

import java.io.IOException;

public class ZooModelManager {
    private final int inputSize;
    private final int outputSize;
    public ZooModelManager(int inputSize, int outputSize) {
        this.inputSize = inputSize;
        this.outputSize = outputSize;
    }

    public Model getResNet50() {
        return ResNet50.builder()
                .inputShape(new int[]{inputSize})
                .cacheMode(CacheMode.HOST)
                .numClasses(outputSize)
                .updater(new Adam(0.01))
                //.weightInit(???)
                .build().init();
    }

    public Model getUNet() {
        return UNet.builder()
                .inputShape(new int[]{inputSize})
                .cacheMode(CacheMode.HOST)
                .numClasses(outputSize)
                .updater(new Adam(0.01))
                //.weightInit(???)
                .build().init();
    }

    public Model getVGG16() throws IOException {
        return VGG16.builder()
                //.inputShape(new int[]{1, inputSize})
                .cacheMode(CacheMode.HOST)
                .numClasses(outputSize)
                .updater(new Adam(0.01))
                //.weightInit(???)
                .build().initPretrained(PretrainedType.IMAGENET);
    }
}
