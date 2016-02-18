/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.jkiss.dbeaver.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.jkiss.utils.CommonUtils;

import java.util.StringTokenizer;

/**
 * Text utils
 */
public class TextUtils {
    public static final char PARAGRAPH_CHAR = (char) 182;

    public static boolean isEmptyLine(IDocument document, int line)
            throws BadLocationException {
        IRegion region = document.getLineInformation(line);
        if (region == null || region.getLength() == 0) {
            return true;
        }
        String str = document.get(region.getOffset(), region.getLength());
        return str.trim().length() == 0;
    }

    public static int getOffsetOf(IDocument document, int line, String pattern)
        throws BadLocationException {
        IRegion region = document.getLineInformation(line);
        if (region == null || region.getLength() == 0) {
            return -1;
        }
        String str = document.get(region.getOffset(), region.getLength());
        return str.indexOf(pattern);
    }

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

        if (width <= 0) {
            return ""; //$NON-NLS-1$
        }
        int avgCharWidth = fontMetrics.getAverageCharWidth();
        float length = t.length();
        if (width < length * avgCharWidth) {
            length = (float) width / avgCharWidth;
            length *= 1.5;
            if (length < t.length()) {
                t = t.substring(0, (int) length);
                //return getShortText(gc, t, width);
            }
        }
        return t;
    }

    public static String formatSentence(String sent)
	{
		if (sent == null) {
			return "";
		}
		StringBuilder result = new StringBuilder();
		StringTokenizer st = new StringTokenizer(sent, " \t\n\r-,.\\/", true);
		while (st.hasMoreTokens()) {
			String word = st.nextToken();
			if (word.length() > 0) {
				result.append(formatWord(word));
			}
		}

		return result.toString();
	}

    public static String formatWord(String word)
	{
		if (word == null) {
			return "";
		}
		StringBuilder sb = new StringBuilder(word.length());
		sb.append(Character.toUpperCase(word.charAt(0)));
		for (int i = 1; i < word.length(); i++) {
			char c = word.charAt(i);
			if ((c == 'i' || c == 'I') && sb.charAt(i - 1) == 'I') {
				sb.append('I');
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
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

    public static boolean isPointInRectangle(int x, int y, int rectX, int rectY, int rectWidth, int rectHeight)
    {
        return (x >= rectX) && (y >= rectY) && x < (rectX + rectWidth) && y < (rectY + rectHeight);
    }

    public static String getSingleLineString(String displayString) {
        return displayString.replace('\n', PARAGRAPH_CHAR).replace("\r", "").replace((char)0, ' ');
    }
}
