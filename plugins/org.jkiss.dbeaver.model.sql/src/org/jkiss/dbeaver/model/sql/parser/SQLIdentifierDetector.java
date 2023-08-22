/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.jface.text.Region;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.sql.completion.SQLCompletionAnalyzer;
import org.jkiss.dbeaver.model.text.parser.TPRuleBasedScanner;
import org.jkiss.dbeaver.model.text.parser.TPToken;
import org.jkiss.dbeaver.model.text.parser.TPTokenAbstract;
import org.jkiss.dbeaver.model.text.parser.TPWordDetector;
import org.jkiss.utils.Pair;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Determines whether a given character is valid as part of an SQL identifier.
 */
public class SQLIdentifierDetector extends TPWordDetector {
    protected SQLDialect dialect;
    private final char structSeparator;
    @NotNull
    private final String[][] quoteStrings;
    private final String[][] stringQuoteStrings;

    public SQLIdentifierDetector(SQLDialect dialect) {
        this(dialect, dialect.getStructSeparator(), dialect.getIdentifierQuoteStrings());
    }

    public SQLIdentifierDetector(SQLDialect dialect, char structSeparator, @Nullable String[][] quoteStrings) {
        this.dialect = dialect;
        this.structSeparator = structSeparator;
        this.quoteStrings = quoteStrings != null ? quoteStrings : new String[0][];
        this.stringQuoteStrings = dialect == null ? new String[][] {{ "'", "'"}} : dialect.getStringQuoteStrings();
    }

    protected boolean isQuote(char c) {
        for (String[] quoteString : quoteStrings) {
            if (quoteString[0].indexOf(c) != -1 || quoteString[1].indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    protected boolean isStringQuote(char c) {
        for (String[] stringQuoteString : stringQuoteStrings) {
            if (stringQuoteString[0].indexOf(c) != -1 || stringQuoteString[1].indexOf(c) != -1) {
                return true;
            }
        }
        return false;
    }

    public char getStructSeparator() {
        return structSeparator;
    }

    public boolean containsSeparator(String identifier) {
        return identifier.indexOf(structSeparator) != -1;
    }

    public String[] splitIdentifier(String identifier) {
        return SQLUtils.splitFullIdentifier(identifier, String.valueOf(structSeparator), quoteStrings, true);
    }

    @Override
    public boolean isWordStart(char c) {
        return super.isWordStart(c) || dialect.validIdentifierStart(c);
    }

    @Override
    public boolean isWordPart(char c) {
        return super.isWordPart(c) || isQuote(c) || structSeparator == c || dialect.validIdentifierPart(c, true);
    }

    public boolean isPlainWordPart(char c) {
        return super.isWordPart(c) || dialect.validIdentifierPart(c, false);
    }

    public boolean isQuoted(String token) {
        for (String[] quoteString : quoteStrings) {
            if (token.startsWith(quoteString[0])) {
                return true;
            }
        }
        return false;
    }

    public String removeQuotes(String name) {
        // Remove leading (and trailing) quotes if any
        for (String[] quoteString : quoteStrings) {
            if (name.startsWith(quoteString[0])) {
                name = name.substring(quoteString[0].length());
            }
            if (name.endsWith(quoteString[1])) {
                name = name.substring(0, name.length() - quoteString[0].length());
            }
        }

        return name;
    }


    @NotNull
    private WordRegion detectIdentifier(@NotNull IDocument document, @NotNull IRegion region) {
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

        WordRegion(int offset) {
            identStart = offset;
            identEnd = offset;
        }

        void extract(IDocument document) throws BadLocationException {
            if (wordStart < 0) wordStart = identStart;
            if (wordEnd < 0) wordEnd = identEnd;
            identifier = document.get(identStart, identEnd - identStart);
            word = document.get(wordStart, wordEnd - wordStart);
        }

        public boolean isEmpty() {
            return word.isEmpty();
        }
    }

    /**
     * Returns identifier under cursor position or empty word region
     */
    @NotNull
    public WordRegion extractIdentifier(@NotNull IDocument document, @NotNull IRegion region, @Nullable SQLRuleManager ruleManager) {
        if (ruleManager == null) {
            return detectIdentifier(document, region);
        }
        
        final WordRegion id = new WordRegion(region.getOffset());
        try {
            IRegion line = document.getLineInformationOfOffset(region.getOffset());
        
            final TPRuleBasedScanner scanner = new TPRuleBasedScanner();
            scanner.setRules(ruleManager.getAllRules());
            scanner.setRange(document, line.getOffset(), line.getLength());
            
            List<Pair<TPToken, Region>> tokens = new ArrayList<>();
            int tokenIndex = -1;
    
            TPToken token = scanner.nextToken();
            while (!token.isEOF()) {
                if (token instanceof TPTokenAbstract && !token.isWhitespace()) {
                    if (scanner.getTokenOffset() <= region.getOffset() && scanner.getTokenEndOffset() > region.getOffset()) {
                        if (!SQLCompletionAnalyzer.isNamePartToken(token)) {
                            return id; // there is no identifier at the given position
                        }
                        tokenIndex = tokens.size();
                    }
                    tokens.add(new Pair<>(token, new Region(scanner.getTokenOffset(), scanner.getTokenLength())));
                }
                token = scanner.nextToken();
            }
            if (tokenIndex == -1) {
                return id;
            }
            
            LinkedList<String> parts = new LinkedList<>();
            {
                Region r = tokens.get(tokenIndex).getSecond();
                String word = document.get(r.getOffset(), r.getLength());
                parts.add(word);
                id.word = word;
                id.wordStart = r.getOffset();
                id.wordEnd = r.getOffset() + r.getLength();
                id.identStart = id.wordStart;
                id.identEnd = id.wordEnd;
            }
            // Explore identifier to the left
            for (int i = tokenIndex; i - 2 >= 0;) {
                i--;
                Region maybeSepRegion = tokens.get(i).getSecond();
                String maybeSepText = document.get(maybeSepRegion.getOffset(), maybeSepRegion.getLength());
                if (maybeSepText.indexOf(structSeparator) < 0) {
                    break;
                }
                i--;
                if (!SQLCompletionAnalyzer.isNamePartToken(tokens.get(i).getFirst())) {
                    break;
                }
                Region prefixWordRegion = tokens.get(i).getSecond();
                String prefixWord = document.get(prefixWordRegion.getOffset(), prefixWordRegion.getLength());
                parts.addFirst(maybeSepText);
                parts.addFirst(prefixWord);
                id.identStart = prefixWordRegion.getOffset();
            }
            // Explore identifier to the right
            for (int i = tokenIndex; i + 2 < tokens.size();) {
                i++;
                Region maybeSepRegion = tokens.get(i).getSecond();
                String maybeSepText = document.get(maybeSepRegion.getOffset(), maybeSepRegion.getLength());
                if (maybeSepText.indexOf(structSeparator) < 0) {
                    break;
                }
                i++;
                if (!SQLCompletionAnalyzer.isNamePartToken(tokens.get(i).getFirst())) {
                    break;
                }
                Region suffixWordRegion = tokens.get(i).getSecond();
                String suffixWord = document.get(suffixWordRegion.getOffset(), suffixWordRegion.getLength());
                parts.addLast(maybeSepText);
                parts.addLast(suffixWord);
                id.identEnd = suffixWordRegion.getOffset() + suffixWordRegion.getLength();
            }

            id.identifier = String.join("", parts);
            return id;
        } catch (BadLocationException e) {
            return id;
        }
    }
}