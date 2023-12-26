package ru.cbr.ht_ml;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bytedeco.opencv.presets.opencv_core;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DatasetSeparator {
    private final String datasetPath;
    private final String lineEnd;
    private static final int LINES_PER_FILE = 100;

    public DatasetSeparator(String path, String lineEnd) {
        datasetPath = path;
        this.lineEnd = lineEnd;
    }

    static void csvToFiles(String csvPath, String datasetRootPath) {
        String[] headers = {"id", "added_by", "context", "coordinates", "project_tc",
                "screenshot", "styles", "tab_id", "timestamp", "type", "rctikk_uid",
                "viewport_screenshot", "url_id", "gens", "listeners", "excluded_from_training"};
        String context;
        String styles;
        String type;
        String uid;
        String dirName = "DataSet_" + DateTime.now().toDate().toString().replace(":", "-").replace(" ", "_");
        FileWriter fw;
        BufferedWriter bw;
        String filePath;
        int lines = 0;
        Map<String, String> nameToDirPath = new HashMap<>();
        try {
            Reader in = new FileReader(csvPath);

            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                    .setHeader(headers)
                    .setSkipHeaderRecord(true)
                    .build();

            Iterable<CSVRecord> records = csvFormat.parse(in);
            Files.createDirectories(Paths.get(datasetRootPath + "\\" + dirName));
            for (CSVRecord record : records) {
                context = record.get("context");
                styles = record.get("styles");
                type = record.get("type");
                uid = record.get("rctikk_uid");
                if (!nameToDirPath.containsKey(type)) {
                    nameToDirPath.put(type, datasetRootPath + "\\" + dirName + "\\" + type);
                    Files.createDirectories(Paths.get(nameToDirPath.get(type)));
                }
                filePath = String.valueOf(Files.createFile(Paths.get(nameToDirPath.get(type) + "\\" + uid)));
                fw = new FileWriter(filePath, true);
                bw = new BufferedWriter(fw);
                bw.write(context);
                bw.newLine();
                bw.write(styles);
                bw.close();
                System.out.print("\rProcessed " + String.valueOf(++lines) + " lines\n");
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
    }

    static void subdivideDataSet(String dataSetPath, String outputPath, int maxBatch) {
        List<String> labels = new ArrayList<>();
        Map<String, List<String>> files = new HashMap<>();
        File baseDir = new File(dataSetPath);
        List<String> temp = new ArrayList<>();
        try {
            Stream.of(baseDir.listFiles())
                    .filter(File::isDirectory)
                    .forEach(
                            dir -> {
                                labels.add(dir.getName());
                                Stream.of(dir.listFiles())
                                        .forEach(
                                                file -> temp.add(file.getPath())
                                        );
                                files.put(dir.getName(), new ArrayList<>(temp));
                                temp.clear();
                            }
                    );
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        Set<String> finishedLabels = new HashSet<>();
        int divide = 0;
        int index = 0;
        String divDir, labelDir;
        FileWriter fw;
        BufferedWriter bw;
        BufferedReader br;
        String filePath, uid;
        String oldFile;
        String line;
        try {
            while (finishedLabels.size() < labels.size()) {
                divDir = outputPath + "\\" + baseDir.getName() + "_div_" + String.valueOf(divide);
                for (int i = 0; i < maxBatch; i++) {
                    finishedLabels.clear();
                    for (String label : labels) {
                        labelDir = divDir + "\\" + label;
                        if (i == 0) {
                            Files.createDirectories(Paths.get(labelDir));
                        }
                        if (divide * maxBatch + i < files.get(label).size()) {
                            oldFile = files.get(label).get(divide * maxBatch + i);
                            uid = oldFile.substring(oldFile.lastIndexOf("\\"));
                            filePath = String.valueOf(Files.createFile(Paths.get(labelDir + "\\" + uid)));
                            fw = new FileWriter(filePath, true);
                            bw = new BufferedWriter(fw);
                            br = new BufferedReader(new FileReader(oldFile));
                            bw.write(br.lines().collect(Collectors.joining()));
                            bw.close();
                        } else {
                            finishedLabels.add(label);
                        }
                    }
                }
                divide++;
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return;
    }

    static void subdivideDataSetCompress(String dataSetPath, String outputPath, int linesPerFile) {
        List<String> labels = new ArrayList<>();
        Map<String, List<String>> files = new HashMap<>();
        File baseDir = new File(dataSetPath);
        List<String> temp = new ArrayList<>();
        try {
            Stream.of(baseDir.listFiles())
                    .filter(File::isDirectory)
                    .forEach(
                            dir -> {
                                labels.add(dir.getName());
                                Stream.of(dir.listFiles())
                                        .forEach(
                                                file -> temp.add(file.getPath())
                                        );
                                files.put(dir.getName(), new ArrayList<>(temp));
                                temp.clear();
                            }
                    );
        } catch (Exception e) {
            System.out.println(e.toString());
        }

        boolean finishedLabel;
        int divide = 0;
        int index = 0;
        String divDir, labelDir;
        FileWriter fw;
        BufferedWriter bw = null;
        BufferedReader br = null;
        String filePath, uid;
        String oldFile;
        String line;
        try {
            divDir = outputPath + "\\" + baseDir.getName() + "_compressed";
            for (String label : labels) {
                divide = 0;
                finishedLabel = false;
                labelDir = divDir + "\\" + label;
                Files.createDirectories(Paths.get(labelDir));
                while (!finishedLabel) {
                    for (int i = 0; i < linesPerFile; i++) {
                        if (divide * linesPerFile + i < files.get(label).size()) {
                            oldFile = files.get(label).get(divide * linesPerFile + i);
                            uid = oldFile.substring(oldFile.lastIndexOf("\\"));
                            br = new BufferedReader(new FileReader(oldFile));
                            if (i == 0) {
                                filePath = String.valueOf(Files.createFile(Paths.get(labelDir + "\\" + uid)));
                                fw = new FileWriter(filePath, true);
                                bw = new BufferedWriter(fw);
                            }
                            bw.write(br.lines().collect(Collectors.joining()));
                            if (i < linesPerFile - 1) {
                                bw.newLine();
                            }
                            if (i == linesPerFile - 1) {
                                bw.close();
                            }
                        } else {
                            finishedLabel = true;
                            bw.close();
                            break;
                        }
                    }
                    divide++;
                }
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
        return;
    }

    void separateFiles() {
        separateFiles(false);
    }

    void separateFiles(boolean deleteBaseFile) {
        try {
            Stream.of(new File(datasetPath).listFiles())
                    .filter(File::isDirectory)
                    .forEach(
                            dir -> Stream.of(dir.listFiles()).forEach(
                                    file -> {
                                        try (Scanner scan = new Scanner(new FileReader(file))) {
                                            scan.useDelimiter(Pattern.compile(lineEnd));
                                            String line;
                                            int linesCount = 0;
                                            PrintWriter writer;
                                            while (scan.hasNext()) {
                                                writer = new PrintWriter(dir + "\\" + dir.getName() + DateTime.now().getMillis() + ".txt", "UTF-8");
                                                while (scan.hasNext() && linesCount < LINES_PER_FILE) {
                                                    line = scan.next();
                                                    if (!line.isEmpty()) {
                                                        linesCount++;
                                                        writer.write(line + "\n");
                                                    }
                                                }
                                                writer.close();
                                                linesCount = 0;
                                            }
                                            if (deleteBaseFile) {
                                                if (file.delete()) {
                                                    System.out.println("Deleted " + file.getName());
                                                }
                                            }
                                        } catch (IOException e) {
                                            throw new RuntimeException(e);
                                        }
                                    }
                            )
                    );
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}
