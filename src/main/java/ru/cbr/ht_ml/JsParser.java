package ru.cbr.ht_ml;

import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.stream.Collectors;

public class JsParser extends Parser {
    public String getFunctionFor(Element element, String js) {
        String funcName = element.attr("onClick");
        return getFunctionByName(js, funcName);
    }

    public String getFunctionFor(Element element, ArrayList<String> jsS) {
        return jsS.stream().map(js -> getFunctionFor(element, js)).collect(Collectors.joining(""));
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
