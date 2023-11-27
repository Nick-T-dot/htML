package ru.cbr.ht_ml;

import org.joda.time.DateTime;

import java.io.*;
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
