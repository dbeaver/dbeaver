/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.jkiss.utils.CommonUtils;

import java.util.Locale;
import java.util.StringTokenizer;

/**
 * Text utils
 */
public class TextUtils {

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
        double avgCharWidth = fontMetrics.getAverageCharacterWidth();
        double length = t.length();
        if (width < length * avgCharWidth) {
            length = (double) width / avgCharWidth;
            length *= 2; // In case of big number of narrow characters
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

    /**
     * Copied from Apache FuzzyScore  implementation.
     * One point is given for every matched character. Subsequent matches yield two bonus points. A higher score
     * indicates a higher similarity.
     */
    public static int fuzzyScore(CharSequence term, CharSequence query) {
        return fuzzyScore(term, query, Locale.getDefault());
    }

    public static int fuzzyScore(CharSequence term, CharSequence query, Locale locale) {
        if (term == null || query == null) {
            throw new IllegalArgumentException("Strings must not be null");
        }

        // fuzzy logic is case insensitive. We normalize the Strings to lower
        // case right from the start. Turning characters to lower case
        // via Character.toLowerCase(char) is unfortunately insufficient
        // as it does not accept a locale.
        final String termLowerCase = term.toString().toLowerCase(locale);
        final String queryLowerCase = query.toString().toLowerCase(locale);

        // the resulting score
        int score = 0;

        // the position in the term which will be scanned next for potential
        // query character matches
        int termIndex = 0;

        // index of the previously matched character in the term
        int previousMatchingCharacterIndex = Integer.MIN_VALUE;
        int sequenceScore = 0;

        for (int queryIndex = 0; queryIndex < queryLowerCase.length(); queryIndex++) {
            final char queryChar = queryLowerCase.charAt(queryIndex);

            boolean termCharacterMatchFound = false;
            for (; termIndex < termLowerCase.length(); termIndex++) {
                final char termChar = termLowerCase.charAt(termIndex);

                if (queryChar == termChar) {
                    // simple character matches result in one point
                    score++;
                    if (termIndex == 0) {
                        // First character
                        score += 4;
                    } else if (!Character.isLetter(termLowerCase.charAt(termIndex - 1))) {
                        // Previous character was a divider
                        score += 2;
                    }

                    // subsequent character matches further improve
                    // the score.
                    if (previousMatchingCharacterIndex + 1 == termIndex) {
                        if (sequenceScore == 0) {
                            sequenceScore = 4;
                        } else {
                            sequenceScore *= 2;
                        }
                        score += sequenceScore;
                    } else {
                        sequenceScore = 0;
                    }

                    previousMatchingCharacterIndex = termIndex;
                    termIndex++;

                    // we can leave the nested loop. Every character in the
                    // query can match at most one character in the term.
                    termCharacterMatchFound = true;
                    break;
                }
            }
            if (!termCharacterMatchFound) {
                return 0;
            }
        }

        return score;
    }

    public static String cutExtraLines(String message, int maxLines) {
        if (message == null || message.indexOf('\n') == -1) {
            return message;
        }
        int lfCount = 0;
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < message.length(); i++) {
            char c = message.charAt(i);
            if (c == '\n') {
                lfCount++;
            }
            buf.append(c);
            if (lfCount == maxLines) {
                buf.append("...");
                break;
            }
        }
        return buf.toString();
    }
}
