package org.jkiss.dbeaver.ui.editors.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.parser.tokens.predicates.SQLTokenEntry;
import org.jkiss.dbeaver.ui.editors.sql.semantics.OffsetKeyedTreeMap.NodesIterator;

class SQLDocumentScriptItemSyntaxContext {
    public static class TokenEntryAtOffset {
        public final int offset;
        public final SQLQuerySymbolEntry entry;

        public TokenEntryAtOffset(int offset, SQLQuerySymbolEntry entry) {
            this.offset = offset;
            this.entry = entry;
        }
    }

    private final OffsetKeyedTreeMap<SQLQuerySymbolEntry> entries = new OffsetKeyedTreeMap<>();
    private int length;

    public SQLDocumentScriptItemSyntaxContext(int length) {
        this.length = length;
    }

    public int length() {
        return this.length;
    }

    @Nullable
    public TokenEntryAtOffset findToken(int offset) {
        NodesIterator<SQLQuerySymbolEntry> it = entries.nodesIteratorAt(offset);
        SQLQuerySymbolEntry entry = it.getCurrValue();
        int entryOffset = it.getCurrOffset();
        if (entry == null && it.prev()) {
            entry = it.getCurrValue();
            entryOffset = it.getCurrOffset();
        }
        if (entry != null && entryOffset <= offset && entryOffset + entry.getInterval().length() > offset) { 
            return new TokenEntryAtOffset(entryOffset, entry);
        } else {
            return null;
        }
    }

    public void registerToken(int offset, @NotNull SQLQuerySymbolEntry token) {
        entries.put(offset, token);
    }

    public void applyDelta(int offset, int oldLength, int newLength) {
        if (oldLength > 0) {
            // TODO remove the affected fragment and apply offset for the rest
            throw new UnsupportedOperationException(); 
        } else { // simple insertion
            this.entries.applyOffset(offset, newLength);
        }
        this.length += newLength - oldLength;
    }

    public void clear() {
        this.entries.clear();
    }
}