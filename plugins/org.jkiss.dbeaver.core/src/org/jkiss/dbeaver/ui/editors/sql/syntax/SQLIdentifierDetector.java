/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Determines whether a given character is valid as part of an SQL identifier.
 */
public class SQLIdentifierDetector extends SQLWordDetector
{
    private char structSeparator;
    private String quoteSymbol;

    public SQLIdentifierDetector(char structSeparator, String quoteSymbol)
    {
        this.structSeparator = structSeparator;
        this.quoteSymbol = quoteSymbol;
    }

    public boolean containsSeparator(String identifier)
    {
        return identifier.indexOf(structSeparator) != -1;
    }

    public List<String> splitIdentifier(String identifier)
    {
        if (!containsSeparator(identifier)) {
            return Collections.singletonList(identifier);
        }
        List<String> tokens = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(identifier, String.valueOf(structSeparator));
        while (st.hasMoreTokens()) {
            tokens.add(st.nextToken());
        }
        return tokens;
    }

    @Override
    public boolean isWordPart(char c) {
        return super.isWordPart(c) ||
            (quoteSymbol != null && quoteSymbol.indexOf(c) != -1) ||
            structSeparator == c;
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c);
    }

    public boolean isQuoted(String token)
    {
        return quoteSymbol != null && !quoteSymbol.isEmpty() && token.startsWith(quoteSymbol);
    }

    public String removeQuotes(String name)
    {
        if (quoteSymbol == null || quoteSymbol.isEmpty()) {
            return name;
        }
        if (name.startsWith(quoteSymbol)) {
            name = name.substring(quoteSymbol.length());
        }
        if (name.endsWith(quoteSymbol)) {
            name = name.substring(name.length() - quoteSymbol.length());
        }
        return name;
    }


    public WordRegion detectIdentifier(IDocument document, IRegion region)
    {
        final WordRegion id = new WordRegion(region.getOffset());
        int docLength = document.getLength();

        try {
            if (!isPlainWordPart(document.getChar(region.getOffset()))) {
                return id;
            }
            while (id.identStart >= 0) {
                char ch = document.getChar(id.identStart);
                if (!isWordPart(ch)) {
                    break;
                }
                if (id.wordStart < 0 && !isPlainWordPart(ch)) {
                    id.wordStart = id.identStart + 1;
                }
                id.identStart--;
            }
            id.identStart++;
            while (id.identEnd < docLength) {
                char ch = document.getChar(id.identEnd);
                if (!isWordPart(ch)) {
                    break;
                }
                if (!isPlainWordPart(ch)) {
                    id.wordEnd = id.identEnd;
                }
                id.identEnd++;
            }
            id.extract(document);
        } catch (BadLocationException e) {
            // ignore
        }

        return id;
    }

    public static class WordRegion {
        public int identStart;
        public int identEnd;
        public int wordStart = -1, wordEnd = -1;
        public String identifier = "";
        public String word = "";

        WordRegion(int offset)
        {
            identStart = offset;
            identEnd = offset;
        }

        void extract(IDocument document) throws BadLocationException
        {
            if (wordStart < 0) wordStart = identStart;
            if (wordEnd < 0) wordEnd = identEnd;
            identifier = document.get(identStart, identEnd - identStart);
            word = document.get(wordStart, wordEnd - wordStart);
        }

        public boolean isEmpty()
        {
            return word.isEmpty();
        }
    }
}