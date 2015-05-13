/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.ui.editors.sql.util;

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;

public class SQLWordFinder
{

    public static IRegion findWord(IDocument document, int offset)
    {

        int start;
        int end;

        start = getWordStartOffset(document.get(), offset);
        end = getWordEndOffset(document.get(), offset);

        if (start > -1 && end > -1)
        {
            if (start == offset && end == offset)
            {
                return new Region(offset, 0);
            }
            else if (start == offset)
            {
                return new Region(start, end - start);
            }
            else
            {
                return new Region(start + 1, end - start - 1);
            }
        }

        return null;
    }

    public static int getWordStartOffset(String text, int startIndex)
    {
        if (text == null || startIndex >= text.length())
        {
            return -1;
        }
        for (int offset = startIndex; offset >= 0; offset--)
        {
            char c = text.charAt(offset);
            if (isWhiteSpace(c))
            {
                return offset;
            }
        }
        return -1;
    }

    public static int getWordEndOffset(String text, int startIndex)
    {
        int pos = startIndex;
        char c;
        int length = text.length();

        while (pos < length)
        {
            c = text.charAt(pos);
            if (isWhiteSpace(c))
            {
                break;
            }
            ++pos;
        }

        return pos;
    }

    public static boolean isWhiteSpace(char c)
    {
        return Character.isWhitespace(c) || c == '(' || c == ')' || c == ',' || c == ';' || c == '\n' || c == '\r'
                || c == '=' || c == '>' || c == '<' || c == '+' || c == '-' || c == '*' || c == '/';
    }

}