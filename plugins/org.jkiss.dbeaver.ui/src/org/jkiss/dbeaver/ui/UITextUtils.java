/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.jkiss.utils.CommonUtils;

/**
 * Text utils
 */
public class UITextUtils {

    /**
     * Shortens a supplied string so that it fits within the area specified by
     * the width argument. Strings that have been shorted have an "..." attached
     * to the end of the string. The width is computed using the
     * {@link org.eclipse.swt.graphics.GC#textExtent(String)}.
     *
     * @param gc    GC used to perform calculation.
     * @param t     text to modify.
     * @param width Pixels to display.
     * @return shortened string that fits in area specified.
     */
    public static String getShortText(GC gc, String t, int width) {
        if (CommonUtils.isEmpty(t)) {
            return t;
        }

        if (width >= gc.textExtent(t).x) {
            return t;
        }

        int w = gc.textExtent("...").x;
        String text = t;
        int l = text.length();
        if (l > 500) l = 500; // Performance issue fix
        int pivot = l / 2;
        int s = pivot;
        int e = pivot + 1;

        while (s >= 0 && e < l) {
            String s1 = text.substring(0, s);
            String s2 = text.substring(e, l);
            int l1 = gc.textExtent(s1).x;
            int l2 = gc.textExtent(s2).x;
            if (l1 + w + l2 < width) {
                text = s1 + " ... " + s2;
                break;
            }
            s--;
            e++;
        }

        if (s == 0 || e == l) {
            text = text.substring(0, 1) + "..." + text.substring(l - 1, l);
        }

        return text;
    }

    /**
     * Shortens a supplied string so that it fits within the area specified by
     * the width argument. Strings that have been shorted have an "..." attached
     * to the end of the string. The width is computed using the
     * {@link org.eclipse.swt.graphics.GC#stringExtent(String)}.
     * <p/>
     * Text shorten removed due to awful algorithm (it works really slow on long strings).
     * TODO: make something better
     *
     * @param fontMetrics    fontMetrics used to perform calculation.
     * @param t     text to modify.
     * @param width Pixels to display.
     * @return shortened string that fits in area specified.
     */
    public static String getShortString(FontMetrics fontMetrics, String t, int width) {

//        return t;
        if (CommonUtils.isEmpty(t)) {
            return t;
        }

        if (width <= 1) {
            return ""; //$NON-NLS-1$
        }
        double avgCharWidth = fontMetrics.getAverageCharacterWidth();
        double length = t.length();
        if (width < length * avgCharWidth) {
            length = (float) width / avgCharWidth;
            length *= 2; // In case of big number of narrow characters
            if (length < t.length()) {
                t = t.substring(0, (int) length);
                //return getShortText(gc, t, width);
            }
        }
        return t;
    }

    public static boolean isPointInRectangle(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        return (x >= rectX) && (y >= rectY) && x < (rectX + rectWidth) && y < (rectY + rectHeight);
    }

    /**
     * Gets text size.
     * x: maximum line length
     * y: number of lines
     * @param text    source text
     * @return size
     */
    public static Point getTextSize(String text) {
        int length = text.length();
        int maxLength = 0;
        int lineCount = 1;
        int lineLength = 0;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (c) {
                case '\n':
                    maxLength = Math.max(maxLength, lineLength);
                    lineCount++;
                    lineLength = 0;
                    break;
                case '\r':
                    break;
                case '\t':
                    lineLength += 4;
                    break;
                default:
                    lineLength++;
                    break;
            }
        }
        maxLength = Math.max(maxLength, lineLength);
        return new Point(maxLength, lineCount);
    }

}
