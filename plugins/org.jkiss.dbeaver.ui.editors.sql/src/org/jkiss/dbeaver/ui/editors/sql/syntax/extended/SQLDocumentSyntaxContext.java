package org.jkiss.dbeaver.ui.editors.sql.syntax.extended;

import java.util.TreeMap;
import java.util.Map.Entry;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.jkiss.dbeaver.model.sql.parser.tokens.SQLTokenType;


class TokenEntry {
    public final int position, length;
    public final SQLTokenType tokenType;
    
    public TokenEntry(int position, int length, SQLTokenType tokenType) {
        this.position = position;
        this.length = length;
        this.tokenType = tokenType;
        // System.out.println(this);
    }
    
    public int end() {
        return this.position + this.length;
    }
    
    @Override
    public String toString() {
        return "TokenEntry[@" + position + "+" + length + ":" + tokenType + "]";
    }
}
    
class TokenLineContext {
    private final TreeMap<Integer, TokenEntry> entries = new TreeMap<>();
    private int lineNumber, lineOffset;
    
    public TokenLineContext(int lineNumber, int lineOffset) {
        this.lineNumber = lineNumber;
        this.lineOffset = lineOffset;
    }

    public int getLineNumber() {
        return lineNumber;
    }
    
    public int getLineOffset() {
        return lineNumber;
    }

    public TokenEntry findToken(int offset) {
        int column = offset - lineOffset;
        Entry<Integer, TokenEntry> kv = entries.floorEntry(column);
        return kv != null &&  kv.getValue().end() > column ? kv.getValue() : null;
    }

    public void registerToken(int offset, int end, SQLTokenType tokenType) {
        entries.put(offset - lineOffset, new TokenEntry(offset, end - offset, tokenType));
    }

    public void reset() {
        entries.clear();
    }

}

public class SQLDocumentSyntaxContext {
    private final IDocument document;
    private final TreeMap<Integer, TokenLineContext> lines = new TreeMap<>();
    private TokenLineContext lastAccessedLine = null;
    
    public SQLDocumentSyntaxContext(IDocument document) {
        this.document = document;
    }

    public TokenEntry findToken(int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            TokenLineContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == line ? lastAccessedLine : lines.get(line);
            if (lineContext != null) {
                return lineContext.findToken(offset);
            } else {
                return null;
            }
        } catch (BadLocationException e) {
            return null;
        }
    }
    
    private TokenLineContext getOrCreateLineContext(int line) {
        return lines.computeIfAbsent(line, n -> { 
            try { return new TokenLineContext(n, document.getLineOffset(line)); }
            catch (BadLocationException e) { return null; }
        });
    }

    public void registerToken(int start, int end, SQLTokenType tokenType) {
        try {
            int fromLine = document.getLineOfOffset(start);
            int toLine = document.getLineOfOffset(end);
            
            TokenLineContext lineContext = getOrCreateLineContext(fromLine);
            if (toLine == fromLine) {
                lineContext.registerToken(start, end, tokenType);    
            } else {
                TokenLineContext nextLineContext = getOrCreateLineContext(fromLine + 1);
                lineContext.registerToken(start, nextLineContext.getLineOffset(), tokenType);
                for (int line = fromLine + 1; line < toLine; line++) {
                    lineContext = nextLineContext;
                    nextLineContext = getOrCreateLineContext(line + 1);
                    lineContext.registerToken(lineContext.getLineOffset(), nextLineContext.getLineOffset(), tokenType);
                }
                nextLineContext.registerToken(nextLineContext.getLineOffset(), end, tokenType);
            }
        } catch (BadLocationException e) {
        }
    }

    public void dropLineOfOffset(int offset) {
        try {
            int line = document.getLineOfOffset(offset);
            TokenLineContext lineContext = lastAccessedLine != null && lastAccessedLine.getLineNumber() == line ? lastAccessedLine : lines.get(line);
            if (lineContext != null) {
                lineContext.reset();
            }
        } catch (BadLocationException e) {
        }
    }

}
