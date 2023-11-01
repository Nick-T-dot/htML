package org.example;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;


class Control {

}

class BrowserAgent {
    public WebDriver driver = new ChromeDriver();

    public BrowserAgent() {
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
    }

    public BrowserAgent(String defaultUrl) {
        this();
        driver.get(defaultUrl);
    }

    public void switchToActive(){
        driver.switchTo().window(driver.getWindowHandle());
    }
    public String getCurPageUrl() {
        return driver.getCurrentUrl();
    }

    public String getHandle() {
        return driver.getWindowHandle();
    }
}

public class Main {
    public static void main(String[] args){
        HtmlParser htmlParser = new HtmlParser();
        CssParser cssParser = new CssParser();
        BrowserAgent browserAgent = new BrowserAgent("https://www.w3schools.com/");
        String curUrl;
        while (true) {
            try {
                TimeUnit.SECONDS.sleep(5);
                browserAgent.switchToActive();
                curUrl = browserAgent.getCurPageUrl();
                System.out.println("You are browsing " + curUrl);
                Document doc = htmlParser.downloadHtml(curUrl);
                Map<String, String> css = htmlParser.downloadCss(curUrl);
                Map<String, String> js = htmlParser.downloadJavaScript(curUrl);
                System.out.println(htmlParser.getFunctionByName(" hjahs window { ad{ ddd}d}vqvqv", "window"));
                cssParser.getStylesFor(Tag.valueOf("user-profile-btn"), " acadfa .user-profile-btn { we:hate_you; lol:also; } qfqvqvqv .user-profile-btn {hecc:21;}gfg");
                ArrayList<Element> buttons = htmlParser.getWithAttribute(doc, Attribute.createFromEncoded("onClick", ""));
                buttons.forEach(System.out::println);
                return;
            } catch (Exception e) {
                System.out.print(e.toString());
            }
        }
    }
}