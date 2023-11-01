package org.example;

import org.jsoup.nodes.Attribute;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Tag;

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
    public Map<String, String> getStylesFor(Tag tag, String css) {
        Map<String, String> styles = new HashMap<>();
        String tagStr = tag.getName();
        Pattern pattern = Pattern.compile(tagStr);
        Matcher matcher = pattern.matcher(css);
        int closingBracket;
        String foundStyle;
        while (matcher.find()) {
            closingBracket = getClosingBracketIndex(matcher.start(), css);
            foundStyle = css.substring(matcher.start(), closingBracket);
            //System.out.println();
            styles.putAll(parseCssStyle(foundStyle));
        }
        return styles;
    }

    public Map<String, String> getStylesFor(Attribute attribute, String css) {
        Map<String, String> styles = new HashMap<>();
        return styles;
    }

    public Map<String, String> getStylesFor(String htmlClass, String css) {
        Map<String, String> styles = new HashMap<>();
        String tagStr = "\\." + htmlClass;
        Pattern pattern = Pattern.compile(tagStr);
        Matcher matcher = pattern.matcher(css);
        int closingBracket;
        while (matcher.find()) {
            closingBracket = getClosingBracketIndex(matcher.start(), css);
            System.out.println(css.substring(matcher.start(), closingBracket));
        }
        return styles;
    }

    public Map<String, String> getStylesFor(Element element, String css) {
        Map<String, String> styles = new HashMap<>();
        return styles;
    }
}
