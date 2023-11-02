package ru.cbr.ht_ml;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class BrowserAgent {
    public WebDriver driver = new ChromeDriver();
    Document lastDownloadedDoc;
    String lastDownloadedLink;
    String curBaseUrl;

    public BrowserAgent() {
        driver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
        lastDownloadedDoc = null;
        lastDownloadedLink = null;
        curBaseUrl = null;
    }

    public BrowserAgent(String defaultUrl) {
        this();
        driver.get(defaultUrl);
    }

    private String getBaseUrl;

    public void switchToActive(){
        driver.switchTo().window(driver.getWindowHandle());
    }
    public String getCurPageUrl() {
        return driver.getCurrentUrl();
    }

    public String getHandle() {
        return driver.getWindowHandle();
    }

    public Document downloadHtml(String url) throws IOException {
        String html = Jsoup.connect(url).get().html();
        lastDownloadedLink = url;
        lastDownloadedDoc = Jsoup.parse(html);
        String[] urlPieces = url.split("/");
        curBaseUrl = urlPieces[0] + "//" + urlPieces[2];
        return lastDownloadedDoc;
    }

    public Map<String, String> downloadCss(String url) throws IOException {
        if (!Objects.equals(url, lastDownloadedLink)) {
            downloadHtml(url);
        }
        String cssUrl = curBaseUrl;
        Element head = lastDownloadedDoc.head();
        ArrayList<String> cssLinks = head.children().select("[rel=stylesheet]").stream().map(el -> el.attr("href")).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> cssParts = cssLinks.stream().map(css -> {
            try {
                return new String(new URL(cssUrl + css).openStream().readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> cssNames = cssLinks.stream().map(link -> link.substring(link.lastIndexOf("/") + 1).split("\\?")[0]).collect(Collectors.toCollection(ArrayList::new));
        Map<String, String> css = new HashMap<>();
        IntStream.range(0, cssParts.size()).forEach(i -> css.put(cssNames.get(i), cssParts.get(i)));
        return css;
    }

    public Map<String, String> downloadJavaScript(String url) throws IOException {
        if (!Objects.equals(url, lastDownloadedLink)) {
            downloadHtml(url);
        }
        String jsUrl = curBaseUrl;
        Element head = lastDownloadedDoc.head();
        ArrayList<Element> jsNodes = head.children().select("script");//.stream().map(el -> el.attr("href")).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> jsParts = new ArrayList<>();
        ArrayList<String> jsLinks = jsNodes.stream().map(el -> el.attr("src")).filter(str -> str.startsWith("/")).collect(Collectors.toCollection(ArrayList::new));
        jsParts.addAll(jsLinks.stream().map(js -> {
            try {
                return new String(new URL(jsUrl + js).openStream().readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toCollection(ArrayList::new)));
        int cssCount = jsParts.size();
        ArrayList<String> jsNames = jsLinks.stream().map(link -> link.substring(link.lastIndexOf("/") + 1).split("\\?")[0]).collect(Collectors.toCollection(ArrayList::new));
        Map<String, String> js = new HashMap<>();
        IntStream.range(0, cssCount).forEach(i -> js.put(jsNames.get(i), jsParts.get(i)));
        jsParts.clear();
        jsNodes.forEach(p -> {
            if (!p.childNodes().isEmpty())
                if(!p.childNode(0).outerHtml().isEmpty())
                    jsParts.add(p.childNode(0).outerHtml());
        });
        cssCount = jsParts.size();
        IntStream.range(0, cssCount).forEach(i -> js.put("unnamed_script_" + i + ".js", jsParts.get(i)));
        return js;
    }
}
