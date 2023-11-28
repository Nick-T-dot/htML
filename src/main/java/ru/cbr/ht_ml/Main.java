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
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Main {
    public static void main(String[] args) {
        Tokenizer tokenizer = new D2VTokenizer();
        String path = "C:\\Users\\Tsvetkov_NK\\Documents\\labeledt";
        DatasetSeparator separator = new DatasetSeparator(path, "\\.");
        //separator.separateFiles(true);
        //tokenizer.train(path);
        tokenizer.evaluate();
        path = "C:\\Users\\Tsvetkov_NK\\Documents\\labeledc";
        tokenizer.tokenizeDataset(path);
        String tokens = Arrays.toString(tokenizer.tokenizeWord("improve the quality"));
        System.out.println(tokens);
        INDArray indArray = tokenizer.tokenizeString("improve the quality");
        if (tokens.equals("null")) {
            return;
        }
        //double[] word = tokenizer.tokenizeWord("day");
        //Classifier classifier = new Classifier(tokenizer);
        //classifier.train(path);
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