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
import org.eclipse.jface.text.IRegion;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;

import java.util.Map.Entry;
import java.util.TreeMap;

public class SQLDocumentSyntaxContext {
    
    class SQLDocumentLineSyntaxContext {
        private final TreeMap<Integer, SQLDocumentSyntaxTokenEntry> entries = new TreeMap<>();
        private final int lineNumber;
        private final int lineOffset;
        private final int lineLength;
        
        public SQLDocumentLineSyntaxContext(int lineNumber, int lineOffset, int lineLength) {
            this.lineNumber = lineNumber;
            this.lineOffset = lineOffset;
            this.lineLength = lineLength;
        }
    
        public int getLineNumber() {
            return lineNumber;
        }
        
        public int getLineOffset() {
            return lineNumber;
        }
        
        public int getLineLength() {
            return lineLength;
        }

        @Nullable
        public SQLDocumentSyntaxTokenEntry findToken(int offset) {
            int column = offset - lineOffset;
            Entry<Integer, SQLDocumentSyntaxTokenEntry> kv = entries.floorEntry(column);
            return kv != null &&  kv.getValue().end > column ? kv.getValue() : null;
        }
    
        public void registerToken(@NotNull SQLDocumentSyntaxTokenEntry token) {
            entries.put(token.position - lineOffset, token);
        }
    
        public void reset() {
            entries.clear();
        }

        public void replace(int offset, int length, int newLength) {
            int column = offset - lineOffset;
            Entry<Integer, SQLDocumentSyntaxTokenEntry> firstEntry = entries.floorEntry(column);
            if (firstEntry.getValue().end > column) {
                if (column + length < firstEntry.getValue().end) { // only this entry affected
                    entries.remove(firstEntry.getKey());
                } else if (offset + length >= this.lineLength) { // the rest of line affected
                    entries.tailMap(firstEntry.getKey()).clear();
                } else { // subset of entries affected
                    entries.subMap(firstEntry.getKey(), column + length).clear();
                }
            }
            int delta = newLength - length;
            for (SQLDocumentSyntaxTokenEntry token : entries.tailMap(column + length).values()) {
                token.end += delta;
                token.position += delta;
            }
        }
    }

    private static final Log log = Log.getLog(SQLDocumentSyntaxContext.class);
    
    private final IDocument document;
    private final TreeMap<Integer, SQLDocumentLineSyntaxContext> lines = new TreeMap<>();
    
    private int lastAccessedOffset = -1;
    private SQLDocumentSyntaxTokenEntry lastAccessedTokenEntry = null;
    private SQLDocumentLineSyntaxContext lastAccessedLine = null;
    
    public SQLDocumentSyntaxContext(IDocument document) {
        this.document = document;
    }

    /**
     * Find token by offset
     */
    @Nullable
    public SQLDocumentSyntaxTokenEntry findToken(int offset) {
        try {
            if (offset == this.lastAccessedOffset) {
                return this.lastAccessedTokenEntry; 
            } else {
                int line = document.getLineOfOffset(offset);
                SQLDocumentLineSyntaxContext lineContext = this.lastAccessedLine != null && this.lastAccessedLine.getLineNumber() == line 
                    ? this.lastAccessedLine : this.lines.get(line);
                this.lastAccessedOffset = offset;
                if (lineContext != null) {
                    return this.lastAccessedTokenEntry = lineContext.findToken(offset);
                } else {
                    return this.lastAccessedTokenEntry = null;
                }
            }
        } catch (BadLocationException e) {
            return null;
        }
    }

    @Nullable
    private SQLDocumentLineSyntaxContext getOrCreateLineContext(int line) {
        return lines.computeIfAbsent(line, n -> { 
            try {
                IRegion region = document.getLineInformation(line);
                return new SQLDocumentLineSyntaxContext(n, region.getOffset(), region.getLength());
            } catch (BadLocationException e) {
                log.debug(e);
                return null;
            }
        });
    }
    
    private void resetLastAccessCache() {
        this.lastAccessedOffset = -1;
        this.lastAccessedTokenEntry = null;
        this.lastAccessedLine = null;
    }

    public void registerToken(@NotNull SQLDocumentSyntaxTokenEntry token) {
        try {
            int fromLine = document.getLineOfOffset(token.position);
            int toLine = document.getLineOfOffset(token.end);
            
            SQLDocumentLineSyntaxContext lineContext = getOrCreateLineContext(fromLine);
            if (lineContext == null) {
                return;
            }
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
            this.dropLine(line);
        } catch (BadLocationException e) {
            log.debug(e);
        }
    }
    
    private void dropLine(int line) {
        SQLDocumentLineSyntaxContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == line
            ? lastAccessedLine : lines.get(line);
        if (lineContext != null) {
            lineContext.reset();
        }
        this.lastAccessedOffset = -1;
        this.lastAccessedTokenEntry = null;
    }

    public void dropLinesOfRange(int offset, int length) {
        if (length == 0) {
            return;
        }
        try {
            int firstLine = document.getLineOfOffset(offset);
            int end = offset + length;
            if (document.getLength() <= end) {
                lines.subMap(firstLine, Integer.MAX_VALUE).clear();
                this.resetLastAccessCache();
            } else {
                int lastLine = document.getLineOfOffset(offset + length);
                if (firstLine == lastLine) {
                    this.dropLine(firstLine);
                } else {
                    for (SQLDocumentLineSyntaxContext ctx : lines.subMap(firstLine, lastLine).values()) {
                        ctx.reset();
                    }
                    this.resetLastAccessCache();
                }
            }
        } catch (BadLocationException e) {
             log.debug(e);
        }
    }
//    
//    public void replace(int offset, int length, int newLength) {
//        try {
//            int firstLine = document.getLineOfOffset(offset);
//            int lastLine = document.getLineOfOffset(offset + length);
//            if (firstLine == lastLine) {
//                SQLDocumentLineSyntaxContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == firstLine ? lastAccessedLine : lines.get(firstLine);
//                lineContext.replace(offset, length, newLength);
//                int delta = newLength - length;
//                for (SQLDocumentLineSyntaxContext ctx : lines.tailMap(firstLine, false).values()) {
//                    ctx.lineOffset += delta;
//                }
//            } else {
//                
//            }
//        } catch (BadLocationException e) {
//            log.debug(e);
//        }
//    }

}
