package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Evaluator;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class HtmlParser extends Parser {
    public Document parseHtml(String path) throws IOException {
        String content = Files.readString(Paths.get(path), Charset.defaultCharset());
        return Jsoup.parse(content);
    }

    public Document downloadHtml(String url) throws IOException {
        String html = Jsoup.connect(url).get().html();
        return Jsoup.parse(html);
    }

    public Map<String, String> downloadCss(String url) throws IOException {
        String[] urlPieces = url.split("/");
        String cssUrl = urlPieces[0] + "//" + urlPieces[2];
        Element head = Jsoup.connect(url).get().head();
        ArrayList<String> cssLinks = head.children().select("[rel=stylesheet]").stream().map(el -> el.attr("href")).collect(Collectors.toCollection(ArrayList::new));
        ArrayList<String> cssParts = cssLinks.stream().map(css -> {
            try {
                return new String(new URL(cssUrl + css).openStream().readAllBytes());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).collect(Collectors.toCollection(ArrayList::new));
        int cssCount = cssLinks.size();
        ArrayList<String> cssNames = cssLinks.stream().map(link -> link.substring(link.lastIndexOf("/") + 1).split("\\?")[0]).collect(Collectors.toCollection(ArrayList::new));
        Map<String, String> css = new HashMap<>();
        IntStream.range(0, cssParts.size()).forEach(i -> css.put(cssNames.get(i), cssParts.get(i)));
        return css;
    }

    public Map<String, String> downloadJavaScript(String url) throws IOException {
        String[] urlPieces = url.split("/");
        String jsUrl = urlPieces[0] + "//" + urlPieces[2];
        Element head = Jsoup.connect(url).get().head();
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


    public ArrayList<Element> getByTag(Document doc, Tag tag) {
        ArrayList<Element> found = new ArrayList<>();
        Stack<Element> toVisit = new Stack<>();
        Element cur;
        toVisit.push(doc.child(0));
        while (!toVisit.isEmpty()) {
            cur = toVisit.pop();
            cur.children().forEach(toVisit::push);
            if (cur.tag() == tag) {
                found.add(cur);
            }
        }
        return found;
    }

    public ArrayList<Element> getByAttribute(Document doc, Attribute attribute) {
        ArrayList<Element> found = new ArrayList<>();
        Stack<Element> toVisit = new Stack<>();
        Element cur;
        toVisit.push(doc.child(0));
        while (!toVisit.isEmpty()) {
            cur = toVisit.pop();
            cur.children().forEach(toVisit::push);
            if (Objects.equals(cur.attributes().get(attribute.getKey()), attribute.getValue())) {
                found.add(cur);
            }
        }
        return found;
    }

    public ArrayList<Element> getWithAttribute(Document doc, Attribute attribute) {
        ArrayList<Element> found = new ArrayList<>();
        Stack<Element> toVisit = new Stack<>();
        Element cur;
        toVisit.push(doc.child(0));
        while (!toVisit.isEmpty()) {
            cur = toVisit.pop();
            cur.children().forEach(toVisit::push);
            if (cur.hasAttr(attribute.getKey())) {
                found.add(cur);
            }
        }
        return found;
    }

    public ArrayList<Element> getById(Document doc, Evaluator.Id id) {
        ArrayList<Element> found = new ArrayList<>();
        Stack<Element> toVisit = new Stack<>();
        Element cur;
        toVisit.push(doc.child(0));
        while (!toVisit.isEmpty()) {
            cur = toVisit.pop();
            cur.children().forEach(toVisit::push);
            if (cur.is(id)) {
                found.add(cur);
            }
        }
        return found;
    }

    public String getFunctionByName(String js, String name) {
        int startIndex = js.indexOf(name);
        String croppedJs = js.substring(startIndex);
        int curChar = getClosingBracketIndex(0, croppedJs);
        return croppedJs.substring(0, curChar);
    }

    public String getFunctionByName(ArrayList<String> jss, String name) {
        return jss.stream().map(js -> getFunctionByName(js, name)).filter(str -> !str.isEmpty()).collect(Collectors.joining("\n"));
    }
}

