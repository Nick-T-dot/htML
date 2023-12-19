package ru.cbr.ht_ml;
import org.deeplearning4j.bagofwords.vectorizer.TfidfVectorizer;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.nd4j.linalg.api.ndarray.INDArray;
import org.nd4j.linalg.cpu.nativecpu.NDArray;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Main {
    public static void main(String[] args) {
        //DatasetSeparator.csvToFiles("C:\\Users\\Tsvetkov_NK\\Documents\\elements.csv", "C:\\Users\\Tsvetkov_NK\\Documents\\datasets");
        //DatasetSeparator.subdivideDataSet("C:\\Users\\Tsvetkov_NK\\Documents\\datasets\\DataSet_Mon_Dec_18_08-51-04_MSK_2023",
        //        "C:\\Users\\Tsvetkov_NK\\Documents\\datasets", 20);
        int side = 70;
        new ZooModelManager(new int[]{3, side, side}, 8).getResNet50();
        Tokenizer tokenizer = new D2VTokenizer(side * side * 3);
        String path = "C:\\Users\\Tsvetkov_NK\\Documents\\datasets\\DataSet_Mon_Dec_18_08-51-04_MSK_2023_parts";
        DatasetSeparator separator = new DatasetSeparator(path, "\\.");
        //separator.separateFiles(true);
        tokenizer.trainParts(path);
        tokenizer.evaluate();
        path = "C:\\Users\\Tsvetkov_NK\\Documents\\labeledt";
        Classifier classifier = Classifier.builder()
                .dataSet(null)
                        .coreModel(Classifier.CoreModel.RESNET50)
                                .featureCount(side * side * 3)
                .classesCount(8)
                .normalize(true)
                .tokenizer(tokenizer)
                                        .build();
        //path = "C:\\Users\\Tsvetkov_NK\\Documents\\IMDB Dataset.csv";
        //path = "C:\\Users\\Tsvetkov_NK\\Documents\\IdeaProjects\\MLTest\\datasets";
        //classifier.setDataSet(path);
        //classifier.loadDataSet();
        //classifier.setSelectedCoreModel(Classifier.CoreModel.RESNET50);
        //classifier.loadModel("C:\\Users\\Tsvetkov_NK\\Documents\\IdeaProjects\\MLTest\\models\\checkpoint_7_ComputationGraph.zip");
        //classifier.train();
        //classifier.test();
        String tokens = Arrays.toString(tokenizer.tokenizeWord("improve the quality"));
        HtmlParser htmlParser = new HtmlParser();
        CssParser cssParser = new CssParser();
        JsParser jsParser = new JsParser();
        BrowserAgent browserAgent;
        String curUrl;
        while (true) {
            try {
                new File("0");
                browserAgent = new BrowserAgent("https://www.w3schools.com/");
                TimeUnit.SECONDS.sleep(5);
                browserAgent.switchToActive();
                curUrl = browserAgent.getCurPageUrl();
                System.out.println("You are browsing " + curUrl);
                Document doc = browserAgent.downloadHtml(curUrl);
                Map<String, String> css = browserAgent.downloadCss(curUrl);
                Map<String, String> js = browserAgent.downloadJavaScript(curUrl);
                System.out.println(jsParser.getFunctionByName(" hjahs window { ad{ ddd}d}vqvqv", "window"));
                Map<String, String> styles = cssParser.getStylesFor(Tag.valueOf("user-profile-btn"), " acadfa .user-profile-btn { we:hate-you; lol:als_o; } qfqvqvqv .user-profile-btn {hecc:21;}gfg");
                ArrayList<Element> buttons = htmlParser.getWithAttribute(doc, Attribute.createFromEncoded("onClick", ""));
                buttons.forEach(System.out::println);
                return;
            } catch (Exception e) {
                System.out.print(e.toString());
            }
        }
    }
}