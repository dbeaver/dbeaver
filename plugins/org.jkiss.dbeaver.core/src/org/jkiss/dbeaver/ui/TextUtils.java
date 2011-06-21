package org.jkiss.dbeaver.ui;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.swt.graphics.GC;

/**
 * Text utils
 */
public class TextUtils {
    public static boolean isEmptyLine(IDocument document, int line)
        throws BadLocationException
    {
        IRegion region = document.getLineInformation(line);
        if (region == null || region.getLength() == 0) {
            return true;
        }
        String str = document.get(region.getOffset(), region.getLength());
        return str.trim().length() == 0;
    }

    /**
     * Shortens a supplied string so that it fits within the area specified by
     * the width argument. Strings that have been shorted have an "..." attached
     * to the end of the string. The width is computed using the
     * {@link org.eclipse.swt.graphics.GC#textExtent(String)}.
     *
     * @param gc GC used to perform calculation.
     * @param t text to modify.
     * @param width Pixels to display.
     * @return shortened string that fits in area specified.
     */
    public static String getShortText(GC gc, String t, int width)
    {
        if (t == null)
        {
            return null;
        }

        if (t.equals(""))
        {
            return "";
        }

        if (width >= gc.textExtent(t).x)
        {
            return t;
        }

        int w = gc.textExtent("...").x;
        String text = t;
        int l = text.length();
        int pivot = l / 2;
        int s = pivot;
        int e = pivot + 1;

        while (s >= 0 && e < l)
        {
            String s1 = text.substring(0, s);
            String s2 = text.substring(e, l);
            int l1 = gc.textExtent(s1).x;
            int l2 = gc.textExtent(s2).x;
            if (l1 + w + l2 < width)
            {
                text = s1 + "..." + s2;
                break;
            }
            s--;
            e++;
        }

        if (s == 0 || e == l)
        {
            text = text.substring(0, 1) + "..." + text.substring(l - 1, l);
        }

        return text;
    }

    /**
     * Shortens a supplied string so that it fits within the area specified by
     * the width argument. Strings that have been shorted have an "..." attached
     * to the end of the string. The width is computed using the
     * {@link org.eclipse.swt.graphics.GC#stringExtent(String)}.
     *
     * Text shorten removed due to awfull aglorythm (it works really slow on long strings).
     * TODO: make something better
     *
     * @param gc GC used to perform calculation.
     * @param t text to modify.
     * @param width Pixels to display.
     * @return shortened string that fits in area specified.
     */
    public static String getShortString(GC gc, String t, int width)
    {
        if (t == null)
        {
            return null;
        }

        if (t.equals("") || width <= 0) {
            return "";
        }
        int avgCharWidth = gc.getFontMetrics().getAverageCharWidth();
        float length = t.length();
        if (width < length * avgCharWidth) {
            length = (float)width / avgCharWidth;
            length *= 1.5;
/*
            for (;;) {
                String tmp = t.substring(0, length);
                int textLength = gc.textExtent(tmp).x;
                if (textLength >= width) {
                    break;
                }
                length += 100;
                if (length >= t.length()) {
                    return t;
                }
            }
*/
            if (length < t.length()) {
                t = t.substring(0, (int)length);
            }
        }
        return t;
/*
        if (width >= gc.stringExtent(t).x)
        {
            return t;
        }

        int w = gc.stringExtent("...").x;
        String text = t;
        int l = text.length();
        int pivot = l / 2;
        int s = pivot;
        int e = pivot + 1;
        while (s >= 0 && e < l)
        {
            String s1 = text.substring(0, s);
            String s2 = text.substring(e, l);
            int l1 = gc.stringExtent(s1).x;
            int l2 = gc.stringExtent(s2).x;
            if (l1 + w + l2 < width)
            {
                text = s1 + "..." + s2;
                break;
            }
            s--;
            e++;
        }

        if (s == 0 || e == l)
        {
            text = text.substring(0, 1) + "..." + text.substring(l - 1, l);
        }

        return text;
*/
    }
}
