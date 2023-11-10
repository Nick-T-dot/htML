package ru.cbr.ht_ml;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

public class Main {
    public static void main(String[] args){
        Tokenizer tokenizer = new Tokenizer();
        String path = "C:\\Users\\Tsvetkov_NK\\Documents\\BillionairesStatisticsDataset.csv";
        //tokenizer.train(path);
        tokenizer.evaluate();
        Classifier classifier = new Classifier(tokenizer);
        classifier.train(path);
        HtmlParser htmlParser = new HtmlParser();
        CssParser cssParser = new CssParser();
        JsParser jsParser = new JsParser();
        BrowserAgent browserAgent = new BrowserAgent("https://www.w3schools.com/");
        String curUrl;
        while (true) {
            try {
                assert(false);
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