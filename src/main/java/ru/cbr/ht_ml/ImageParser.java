package ru.cbr.ht_ml;

import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ImageParser {

    ArrayList<String> imageExtensions;

    public ImageParser() {
        imageExtensions = new ArrayList<>(List.of(".apng", ".gif", ".ico", ".cur", ".jpg", ".jpeg", ".jfif", ".pjpeg", ".pjp", ".png", ".svg"));
    }
    public Map<String, String> getImagesFrom(Elements element) {
        Map<String, String> imgs = new HashMap<>();
        return imgs;
    }
    public Map<String, String> getImageFrom(Element element) {
        Map<String, String> imgs = new HashMap<>();

        return imgs;
    }
}
