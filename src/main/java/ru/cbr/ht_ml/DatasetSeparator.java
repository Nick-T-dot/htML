package ru.cbr.ht_ml;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.bytedeco.opencv.presets.opencv_core;
import org.joda.time.DateTime;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Pattern;
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
        String[] headers = { "id", "added_by", "context", "coordinates", "project_tc",
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
                System.out.print("\r Processed " + String.valueOf(++lines) + " lines\n");
            }
        } catch (IOException e) {
            System.out.println(e.toString());
        }
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
                                                if(file.delete()) {
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
