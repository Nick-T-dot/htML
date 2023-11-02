package ru.cbr.ht_ml;

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
}

