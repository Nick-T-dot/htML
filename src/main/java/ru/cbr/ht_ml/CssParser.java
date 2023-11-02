package ru.cbr.ht_ml;

import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;
import org.jsoup.select.Evaluator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CssParser extends Parser {
    private Map<String, String> parseCssStyle(String style) {
        Pattern pattern = Pattern.compile("[\\w-]+:[\\w-]+;");
        Matcher matcher = pattern.matcher(style);
        Map<String, String> styles = new HashMap<>();
        int colonIndex;
        String fused;
        while (matcher.find()) {
            fused = style.substring(matcher.start(), matcher.end());
            colonIndex = fused.indexOf(":");
            styles.put(fused.substring(0, colonIndex), fused.substring(colonIndex + 1, fused.length() - 1));
        }
        return styles;
    }

    private Map<String, String> getStylesForSelector(String selector, String css) {
        Map<String, String> styles = new HashMap<>();
        Pattern pattern = Pattern.compile(selector);
        Matcher matcher = pattern.matcher(css);
        int closingBracket;
        String foundStyle;
        while (matcher.find()) {
            closingBracket = getClosingBracketIndex(matcher.start(), css);
            foundStyle = css.substring(matcher.start(), closingBracket);
            styles.putAll(parseCssStyle(foundStyle));
        }
        return styles;
    }
    public Map<String, String> getStylesFor(Tag tag, String css) {
        String tagStr = tag.getName();
        return getStylesForSelector(tagStr, css);
    }

    public Map<String, String> getStylesFor(Evaluator.Id id, String css) {
        String idStr =  "#" + id.toString();
        return getStylesForSelector(idStr, css);
    }

    public Map<String, String> getStylesFor(String htmlClass, String css) {
        String tagStr = "\\." + htmlClass;
        return getStylesForSelector(tagStr, css);
    }

    public Map<String, String> getStylesFor(Element element, String css) {
        Map<String, String> styles = new HashMap<>();
        styles.putAll(getStylesFor(element.tagName(), css));
        element.classNames().forEach(className -> styles.putAll(getStylesFor(className, css)));
        styles.putAll(getStylesFor(element.id(), css));
        return styles;
    }

    public Map<String, String> getStylesFor(Element element, ArrayList<String> cssS) {
        Map<String, String> styles = new HashMap<>();
        cssS.forEach(css -> styles.putAll(getStylesFor(element, css)));
        return styles;
    }
}
