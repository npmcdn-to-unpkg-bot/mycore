package org.mycore.iiif.parser;


import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import org.mycore.iiif.model.MCRIIIFImageSourceRegion;


public class MCRIIIFRegionParser {

    public static final String PERCENT_PREFIX = "pct:";
    private final int w;
    private final int h;
    private String sourceRegion;
    private boolean completeValid = true;

    public MCRIIIFRegionParser(String sourceRegion, int w, int h) {
        this.sourceRegion = sourceRegion.toLowerCase(Locale.ENGLISH);

        this.w = w;
        this.h = h;

        if (w < 0 || h < 0) {
            throw new IllegalArgumentException("w or h are zero!");
        }

        if (isFull()) {
            return;
        }

        if (parseNumbers().size() != 4) {
            completeValid = false;
            throw new IllegalArgumentException("sourceRegion must have 4 numbers!");
        }
    }

    public MCRIIIFImageSourceRegion parseImageRegion() throws NumberFormatException {
        return isPercent() ?
                parsePercentImageRegion() : isFull() ?
                    new MCRIIIFImageSourceRegion(0, 0, w - 1, h - 1) : parseAbsoluteImageRegion();
    }

    private MCRIIIFImageSourceRegion parsePercentImageRegion() {
        List<Double> doubles = parseNumbers();

        double x = Math.floor(doubles.get(0) * (w / 100));
        double y = Math.floor(doubles.get(1) * (h / 100));
        double x2 = Math.ceil(x + (doubles.get(2) * (w / 100)));
        double y2 = Math.ceil(y + (doubles.get(3) * (w / 100)));

        return parseImageRegion((int) x, (int) y, (int) x2, (int) y2);
    }

    private MCRIIIFImageSourceRegion parseAbsoluteImageRegion() {
        List<Double> doubles = parseNumbers();
        Double x1 = doubles.get(0), y1 = doubles.get(1), x2 = doubles.get(2) + x1, y2 = doubles.get(3) + y1;
        return parseImageRegion((int) Math.round(x1), (int) Math.round(y1), (int) Math.round(x2), (int) Math.round(y2));
    }

    private MCRIIIFImageSourceRegion parseImageRegion(int x1, int y1, int x2, int y2) {
        if (x1 >= w || y1 >= h) {
            completeValid = false;
            throw new IllegalArgumentException("x[" + x1 + "] or y[" + y1 + "] cant be bigger then or equal image size[" + w + "x" + h + "]!");
        }

        if (!(y1 < y2 && x1 < x2 && x1 >= 0 && y1 >= 0 && x2 < w && y2 < h)) {
            completeValid = false;
        }

        // negative values become 0
        x1 = Math.max(0, x1);
        y1 = Math.max(0, y1);

        // end values bigger then image become as big as image
        x2 = Math.min(w - 1, x2);
        y2 = Math.min(h - 1, y2);

        return new MCRIIIFImageSourceRegion(x1, y1, x2, y2);
    }

    private boolean isPercent() {
        return this.sourceRegion.startsWith(PERCENT_PREFIX);
    }


    private boolean isFull() {
        return sourceRegion.equals("full");
    }

    private List<Double> parseNumbers() {
        return Arrays.stream((isPercent() ? this.sourceRegion.substring(PERCENT_PREFIX.length()) : this.sourceRegion).split(","))
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }


    public boolean isCompleteValid() {
        return completeValid;
    }
}
