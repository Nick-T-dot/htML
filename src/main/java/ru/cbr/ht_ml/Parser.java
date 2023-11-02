package ru.cbr.ht_ml;

public class Parser {
    protected int getClosingBracketIndex(int start, String doc) {
        String findHere = doc.substring(start);
        int bracketRatio = 1;
        int curChar = findHere.indexOf("{") + 1;
        while (bracketRatio != 0) {
            if (findHere.charAt(curChar) == '{') {
                bracketRatio++;
            } else if (findHere.charAt(curChar) == '}') {
                bracketRatio--;
            }
            curChar++;
        }
        return curChar + start;
    }
}
