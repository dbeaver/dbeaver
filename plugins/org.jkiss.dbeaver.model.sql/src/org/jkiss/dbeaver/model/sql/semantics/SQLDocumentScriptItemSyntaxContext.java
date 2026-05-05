/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;

import java.util.List;

public class SQLDocumentScriptItemSyntaxContext {
    private static final Log log = Log.getLog(SQLDocumentScriptItemSyntaxContext.class);
    @NotNull
    private final Object lock = new Object();
    @NotNull
    private final OffsetKeyedTreeMap<SQLQuerySymbolEntry> entries = new OffsetKeyedTreeMap<>();
    @NotNull
    private final String originalText;
    @NotNull
    private final SQLQueryModel queryModel;
    private final int initialPosition;
    private int length;
    private boolean hasContextBoundaryAtLength = true;
    private boolean isDirty = false;
    @Nullable
    private List<SQLQueryRecognitionProblemInfo> problems = null;

    public SQLDocumentScriptItemSyntaxContext(
        int initialPosition,
        @NotNull String originalText,
        @NotNull SQLQueryModel queryModel,
        int length
    ) {
        this.initialPosition = initialPosition;
        this.originalText = originalText;
        this.queryModel = queryModel;
        this.length = length;
    }

    public int getInitialPosition() {
        return this.initialPosition;
    }

    @NotNull
    public String getOriginalText() {
        return this.originalText;
    }

    @NotNull
    public SQLQueryModel getQueryModel() {
        return this.queryModel;
    }

    public int length() {
        return this.length;
    }

    public boolean hasContextBoundaryAtLength() {
        return this.hasContextBoundaryAtLength;
    }

    public void setHasContextBoundaryAtLength(boolean hasContextBoundaryAtLength) {
        this.hasContextBoundaryAtLength = hasContextBoundaryAtLength;
    }

    public boolean isDirty() {
        synchronized (this.lock) {
            return this.isDirty;
        }
    }
    
    public void setProblems(@Nullable List<SQLQueryRecognitionProblemInfo> problems) {
        this.problems = problems;
    }

    @Nullable
    public List<SQLQueryRecognitionProblemInfo> getProblems() {
        return problems;
    }

    @Nullable
    public SQLTokenEntryAtOffset findToken(int offset) {
        synchronized (this.lock) {
            NodesIterator<SQLQuerySymbolEntry> it = entries.nodesIteratorAt(offset);
            SQLQuerySymbolEntry entry = it.getCurrValue();
            int entryOffset = it.getCurrOffset();
            if (entry == null && it.prev()) {
                entry = it.getCurrValue();
                entryOffset = it.getCurrOffset();
            }
            if (entry != null && entryOffset <= offset && entryOffset + entry.getInterval().length() > offset) {
                return new SQLTokenEntryAtOffset(entryOffset, entry);
            } else {
                return null;
            }
        }
    }

    public void registerToken(int offset, @NotNull SQLQuerySymbolEntry token) {
        synchronized (this.lock) {
            entries.put(offset, token);
            this.isDirty = true;
        }
    }

    public void applyDelta(int offset, int oldLength, int newLength) {
        synchronized (this.lock) {
            if (oldLength > 0) {
                // TODO remove the affected fragment and apply offset for the rest
                throw new UnsupportedOperationException();
            } else { // simple insertion
                this.entries.applyOffset(offset, newLength);
            }
            this.length += newLength - oldLength;
            this.isDirty = true;
        }
    }

    public void clear() {
        synchronized (this.lock) {
            this.entries.clear();
        }
    }

    public void refreshCompleted() {
        synchronized (this.lock) {
            this.isDirty = false;
        }
    }
}