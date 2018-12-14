/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.sql.parser;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.sql.SQLUtils;

/**
 * Determines whether a given character is valid as part of an SQL identifier.
 */
public class SQLIdentifierDetector extends SQLWordDetector
{
    private char structSeparator;
    @NotNull
    private final String[][] quoteStrings;

    public SQLIdentifierDetector(char structSeparator, @Nullable String[][] quoteStrings)
    {
        this.structSeparator = structSeparator;
        this.quoteStrings = quoteStrings != null ? quoteStrings : new String[0][];
    }

    protected boolean isQuote(char c) {
        for (int i = 0; i < quoteStrings.length; i++) {
            if (quoteStrings[i][0].indexOf(c) != -1 || quoteStrings[i][1].indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    public boolean containsSeparator(String identifier)
    {
        return identifier.indexOf(structSeparator) != -1;
    }

    public String[] splitIdentifier(String identifier)
    {
        return SQLUtils.splitFullIdentifier(identifier, structSeparator, quoteStrings);
    }

    @Override
    public boolean isWordPart(char c) {
        return super.isWordPart(c) || isQuote(c) || structSeparator == c;
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c);
    }

    public boolean isQuoted(String token)
    {
        for (int i = 0; i < quoteStrings.length; i++) {
            if (token.startsWith(quoteStrings[i][0])) {
                return true;
            }
        }
        return false;
    }

    public String removeQuotes(String name)
    {
        for (int i = 0; i < quoteStrings.length; i++) {
            name = DBUtils.getUnQuotedIdentifier(name, quoteStrings[i][0], quoteStrings[i][1]);
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