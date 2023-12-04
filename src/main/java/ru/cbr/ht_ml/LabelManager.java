package ru.cbr.ht_ml;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class LabelManager {
    Map<String, Integer> core;
    ArrayList<String> labels;

    public LabelManager() {
        core  = new HashMap<>();
        labels = new ArrayList<>();
    }

    public void addLabel(String label) {
        labels.add(label);
    }

    public void tryAddLabels(String labelsCommaDelimited) {
        ArrayList<String> separateLabels = Arrays.stream(labelsCommaDelimited.replace(" ", "").split(",")).collect(Collectors.toCollection(ArrayList::new));
        labels.addAll(separateLabels);
        labels = new ArrayList<>(new HashSet<>(labels).stream().toList());
    }

    public double[][] getLabelIndexes(List<String> stringLabels) {
        IntStream.range(0, labels.size()).forEach(i -> core.put(labels.get(i), i));
        double[][] labelIndexes = new double[1][labels.size()];
        stringLabels.forEach(l -> labelIndexes[0][core.get(l)] = 1.);
        return labelIndexes;
    }
}
