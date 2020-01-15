package org.esa.snap.graphbuilder.ui.components.utils;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.Point;

public class GridUtils {
    public static final int gridSize = 15;
    private static final int gridMajor = 5;
    private static final Color gridMajorColor = new Color(255, 255, 255, 30);
    private static final Color gridMinorColor = new Color(255, 255, 255, 15);

    static public BufferedImage gridPattern(int width, int height) {
        // initalize gridPattern image buffer
        BufferedImage gridPattern = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D gbuff = (Graphics2D) gridPattern.getGraphics();

        int nCol = (int) Math.ceil(width / (float) gridSize);
        int nRow = (int) Math.ceil(height / (float) gridSize);
        int nMax = Math.max(nCol, nRow);

        BasicStroke majorStroke = new BasicStroke(2);
        BasicStroke minorStroke = new BasicStroke(1);

        for (int i = 0; i <= nMax; i++) {
            int pos = i * gridSize;

            if (i % gridMajor == 0) {
                // set style for major lines
                gbuff.setStroke(majorStroke);
                gbuff.setColor(gridMajorColor);
            } else {
                // set style for minor lines
                gbuff.setStroke(minorStroke);
                gbuff.setColor(gridMinorColor);
            }

            if (i <= nRow) {
                // draw row
                gbuff.drawLine(0, pos, width, pos);
            }
            if (i <= nCol) {
                // draw col
                gbuff.drawLine(pos, 0, pos, height);
            }
        }
        return gridPattern;
    }

    static public Point normalize(Point p) {
        int x = Math.round(p.x / gridSize) * gridSize;
        int y = Math.round(p.y / gridSize) * gridSize;
        return new Point(x, y);
    }

    static public int round(int dim) {
        return Math.round(dim / gridSize) * gridSize;
    }

    static public int ceil(int dim) {
        return (int)Math.ceil(dim / gridSize) * gridSize;
    }

    static public int floor(int dim) {
        return (int)Math.floor(dim / gridSize) * gridSize;
    }
}