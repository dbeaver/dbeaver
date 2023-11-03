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
package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.Log;

import java.util.TreeMap;
import java.util.Map.Entry;


class SQLDocumentLineSyntaxContext {
    private final TreeMap<Integer, SQLDocumentSyntaxTokenEntry> entries = new TreeMap<>();
    private int lineNumber, lineOffset;
    
    public SQLDocumentLineSyntaxContext(int lineNumber, int lineOffset) {
        this.lineNumber = lineNumber;
        this.lineOffset = lineOffset;
    }

    public int getLineNumber() {
        return lineNumber;
    }
    
    public int getLineOffset() {
        return lineNumber;
    }

    public SQLDocumentSyntaxTokenEntry findToken(int offset) {
        int column = offset - lineOffset;
        Entry<Integer, SQLDocumentSyntaxTokenEntry> kv = entries.floorEntry(column);
        return kv != null &&  kv.getValue().end > column ? kv.getValue() : null;
    }

    public void registerToken(SQLDocumentSyntaxTokenEntry token) {
        entries.put(token.position - lineOffset, token);
    }

    public void reset() {
        entries.clear();
    }

}

public class SQLDocumentSyntaxContext {
    private static final Log log = Log.getLog(SQLDocumentSyntaxContext.class);
    
    private final IDocument document;
    private final TreeMap<Integer, SQLDocumentLineSyntaxContext> lines = new TreeMap<>();
    private SQLDocumentLineSyntaxContext lastAccessedLine = null;
    
    public SQLDocumentSyntaxContext(IDocument document) {
        this.document = document;
    }

    public SQLDocumentSyntaxTokenEntry findToken(int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            SQLDocumentLineSyntaxContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == line ? lastAccessedLine : lines.get(line);
            if (lineContext != null) {
                return lineContext.findToken(offset);
            } else {
                return null;
            }
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    private SQLDocumentLineSyntaxContext getOrCreateLineContext(int line) {
        return lines.computeIfAbsent(line, n -> { 
            try {
                return new SQLDocumentLineSyntaxContext(n, document.getLineOffset(line));
            } catch (BadLocationException e) {
                log.debug(e);
                return null;
            }
        });
    }

    public void registerToken(SQLDocumentSyntaxTokenEntry token) {
        try {
            int fromLine = document.getLineOfOffset(token.position);
            int toLine = document.getLineOfOffset(token.end);
            
            SQLDocumentLineSyntaxContext lineContext = getOrCreateLineContext(fromLine);
            if (toLine == fromLine) {
                lineContext.registerToken(token);    
            } else {
                SQLDocumentLineSyntaxContext nextLineContext = getOrCreateLineContext(fromLine + 1);
                lineContext.registerToken(token.withInterval(token.position, nextLineContext.getLineOffset()));
                for (int line = fromLine + 1; line < toLine; line++) {
                    lineContext = nextLineContext;
                    nextLineContext = getOrCreateLineContext(line + 1);
                    lineContext.registerToken(token.withInterval(lineContext.getLineOffset(), nextLineContext.getLineOffset()));
                }
                nextLineContext.registerToken(token.withInterval(nextLineContext.getLineOffset(), token.end));
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }
    }

    public void dropLineOfOffset(int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            SQLDocumentLineSyntaxContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == line ? lastAccessedLine : lines.get(line);
            if (lineContext != null) {
                lineContext.reset();
            }
        } catch (BadLocationException e) {
            log.debug(e);
        }
    }

}
