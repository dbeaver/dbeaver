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

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;


public class SQLDocumentSyntaxContext {
    private static final Log log = Log.getLog(SQLDocumentSyntaxContext.class);
    @NotNull
    private final Set<SQLDocumentSyntaxContextListener> listeners = new HashSet<>();
    @NotNull
    private final OffsetKeyedTreeMap<SQLDocumentScriptItemSyntaxContext> scriptItems = new OffsetKeyedTreeMap<>();

    @Nullable
    private SQLQuerySymbolEntry lastAccessedTokenEntry = null;
    @Nullable
    private SQLDocumentScriptItemSyntaxContext lastAccessedScriptItem = null;
    private int lastTokenAccessOffset = Integer.MAX_VALUE;
    private int lastAccessedTokenOffset = Integer.MAX_VALUE;
    private int lastItemAccessOffset = Integer.MAX_VALUE;
    private int lastAccessedItemOffset = Integer.MAX_VALUE;

    public SQLDocumentSyntaxContext() {
    }

    /**
     * Add syntax context event listener
     */
    public void addListener(@NotNull SQLDocumentSyntaxContextListener listener) {
        this.listeners.add(listener);
    }

    /**
     * Remove syntax context event listener
     */
    public void removeListener(@NotNull SQLDocumentSyntaxContextListener listener) {
        this.listeners.remove(listener);
    }
    
    private void forEachListener(@NotNull Consumer<SQLDocumentSyntaxContextListener> action) {
        for (SQLDocumentSyntaxContextListener listener : this.listeners) {
            action.accept(listener);
        }
    }

    /**
     * Get all script items from the document syntax context
     */
    @NotNull
    public List<SQLScriptItemAtOffset> getScriptItems() {
        List<SQLScriptItemAtOffset> result = new ArrayList<>();
        NodesIterator<SQLDocumentScriptItemSyntaxContext> it = this.scriptItems.nodesIteratorAt(Integer.MIN_VALUE);
        while (it.next()) {
            result.add(new SQLScriptItemAtOffset(it.getCurrOffset(), it.getCurrValue()));
        }
        return result;
    }

    /**
     * Find script item from the document syntax context at the provided offset
     */
    @Nullable
    public SQLScriptItemAtOffset findScriptItem(int offset) {
        if (offset == this.lastItemAccessOffset) {
            // found, do nothing
        } else if (offset >= this.lastAccessedItemOffset && this.lastAccessedScriptItem != null &&
            offset < this.lastAccessedItemOffset + this.lastAccessedScriptItem.length()) {
            this.lastItemAccessOffset = offset;
        } else {
            NodesIterator<SQLDocumentScriptItemSyntaxContext> it = this.scriptItems.nodesIteratorAt(offset);
            SQLDocumentScriptItemSyntaxContext scriptItem = it.getCurrValue();
            int itemOffset = it.getCurrOffset();
            if (scriptItem == null && it.prev()) {
                scriptItem = it.getCurrValue();
                itemOffset = it.getCurrOffset();
            }

            this.lastItemAccessOffset = offset;
                                                              // area behind the query belongs to prepending query
            if (scriptItem != null && itemOffset <= offset) { // && itemOffset + scriptItem.length() > offset) {
                this.lastAccessedItemOffset = itemOffset;
                this.lastAccessedScriptItem = scriptItem;
            } else {
                this.lastAccessedItemOffset = Integer.MAX_VALUE;
                this.lastAccessedScriptItem = null;
            }
        }
        return this.lastAccessedScriptItem == null ?
            null :
            new SQLScriptItemAtOffset(this.lastAccessedItemOffset, this.lastAccessedScriptItem);
    }

    /**
     * Find token by offset
     */
    @Nullable
    public SQLQuerySymbolEntry findToken(int offset) {
        if (offset == this.lastTokenAccessOffset) {
            // found, do nothing
        } else if (offset >= this.lastAccessedTokenOffset && this.lastAccessedTokenEntry != null &&
            offset < this.lastAccessedTokenOffset + this.lastAccessedTokenEntry.getInterval().length()) {
            this.lastTokenAccessOffset = offset;
        } else {
            SQLScriptItemAtOffset scriptItem = this.findScriptItem(offset);
            SQLTokenEntryAtOffset token = scriptItem == null ? null : scriptItem.item.findToken(offset - scriptItem.offset);

            this.lastTokenAccessOffset = offset;
            if (token != null) {
                this.lastAccessedTokenOffset = token.offset + scriptItem.offset;
                this.lastAccessedTokenEntry = token.entry;
            } else {
                this.lastAccessedTokenOffset = Integer.MAX_VALUE;
                this.lastAccessedTokenEntry = null;
            }
        }

        return this.lastAccessedTokenEntry;
    }

    public int getLastAccessedTokenOffset() {
        return this.lastAccessedTokenOffset;
    }

    public int getLastAccessedScriptElementOffset() {
        return this.lastAccessedItemOffset;
    }

    /**
     * Reset the cached information about document context items
     */
    public void resetLastAccessCache() {
        this.lastTokenAccessOffset = Integer.MAX_VALUE;
        this.lastAccessedTokenOffset = Integer.MAX_VALUE;
        this.lastAccessedTokenEntry = null;

        this.lastItemAccessOffset = Integer.MAX_VALUE;
        this.lastAccessedItemOffset = Integer.MAX_VALUE;
        this.lastAccessedScriptItem = null;
    }

    /**
     * Introduce new script item to the document syntax context
     */
    public SQLDocumentScriptItemSyntaxContext registerScriptItemContext(
        @NotNull String elementOriginalText,
        @NotNull SQLQueryModel queryModel,
        int offset,
        int length,
        boolean hasContextBoundaryAtLength
    ) {
        SQLDocumentScriptItemSyntaxContext scriptItem = new SQLDocumentScriptItemSyntaxContext(
            offset,
            elementOriginalText,
            queryModel,
            length
        );
        scriptItem.setHasContextBoundaryAtLength(hasContextBoundaryAtLength);
        SQLDocumentScriptItemSyntaxContext oldScriptItem = this.scriptItems.put(offset, scriptItem);
        if (oldScriptItem != scriptItem && oldScriptItem != null) {
            this.forEachListener(l -> l.onScriptItemInvalidated(oldScriptItem));
        }
        this.forEachListener(l -> l.onScriptItemIntroduced(scriptItem));
        return scriptItem;
    }

    /**
     * Update script items according to the document text changes
     */
    @NotNull
    public IRegion applyDelta(int offset, int oldLength, int newLength) {
        IRegion affectedRegion;
        if (oldLength > 0) {
            // TODO:
            //   if oldLength fits in one scriptItem, them remove some part of it using split-join operation
            //   otherwise, drop all the scriptItems in oldLength range and apply newLength-oldLength as offset for all the tailing

            int delta = newLength - oldLength;

            // temporary solution - just explicitly drop what was invalidated starting at the current position
            {
                // otherwise just drop the whole tail being affected
                ListNode<Integer> keyOffsetsToRemove = null;
                NodesIterator<SQLDocumentScriptItemSyntaxContext> it = this.scriptItems.nodesIteratorAt(offset);
                SQLDocumentScriptItemSyntaxContext currItem = it.getCurrValue();
                int currOffset = it.getCurrOffset();
                int lastAffectedOffset = currOffset;
                if (currItem != null) {
                    keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, currOffset);
                    this.forEachListener(l -> l.onScriptItemInvalidated(currItem));
                    lastAffectedOffset = currOffset + currItem.length();
                } else if (it.prev() && it.getCurrValue() != null) {
                    currOffset = it.getCurrOffset();
                    SQLDocumentScriptItemSyntaxContext currItem2 = it.getCurrValue();
                    if (currOffset <= offset && currOffset + currItem2.length() > offset) {
                        keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, currOffset);
                        this.forEachListener(l -> l.onScriptItemInvalidated(currItem2));
                        lastAffectedOffset = currOffset + currItem2.length();
                    }
                } else {
                    lastAffectedOffset = offset + oldLength;
                }
                while (it.next() && it.getCurrValue() != null && (delta < 0 || lastAffectedOffset <= (offset + oldLength))) {
                    currOffset = it.getCurrOffset();
                    SQLDocumentScriptItemSyntaxContext currItem3 = it.getCurrValue();
                    keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, currOffset);
                    this.forEachListener(l -> l.onScriptItemInvalidated(currItem3));
                    lastAffectedOffset = currOffset + currItem3.length();
                }
                int firstAffectedOffset = 0;
                for (ListNode<Integer> kn = keyOffsetsToRemove; kn != null; kn = kn.next) {
                    firstAffectedOffset = kn.data;
                    this.scriptItems.removeAt(kn.data);
                }
                if (delta > 0) { // when delta is non-negative, we are dropping only the affected region and applying offset for the rest
                    this.scriptItems.applyOffset(offset, delta);
                }

                if (keyOffsetsToRemove != null) {
                    affectedRegion = new Region(firstAffectedOffset, lastAffectedOffset - firstAffectedOffset - oldLength + newLength);
                } else {
                    affectedRegion = null;
                }
            }
            // TODO removeAtRange(..) is needed either way

        } else { // simple insertion
            SQLScriptItemAtOffset scriptItem = this.findScriptItem(offset);
            if (scriptItem != null) {
                scriptItem.item.applyDelta(offset, oldLength, newLength);
                int affectedStart = Math.min(scriptItem.offset, offset);
                int affectedEnd = Math.max(scriptItem.offset + scriptItem.item.length(), offset + newLength);
                affectedRegion = new Region(affectedStart, affectedEnd - affectedStart);
            } else {
                NodesIterator<SQLDocumentScriptItemSyntaxContext> it = this.scriptItems.nodesIteratorAt(offset);
                int start = it.prev() && it.getCurrValue() != null ? it.getCurrOffset() + it.getCurrValue().length() : 0;
                int length = it.next() ? (it.getCurrOffset() - start) : Integer.MAX_VALUE;
                affectedRegion = new Region(start, length);
            }
            this.scriptItems.applyOffset(offset, newLength);
        }
        this.resetLastAccessCache();

        if (affectedRegion == null) {
            affectedRegion = new Region(offset, newLength);
        }
        return affectedRegion;
    }

    /**
     * Reset document syntax context state
     */
    public void clear() {
        this.forEachListener(SQLDocumentSyntaxContextListener::onAllScriptItemsInvalidated);
        this.scriptItems.clear();
        this.resetLastAccessCache();
    }

    /**
     * Drop script items outside the given text fragment range
     */
    @NotNull
    public Interval dropInvisibleScriptItems(@NotNull Interval actualFragment) {
        int rangeStart = actualFragment.a;
        int rangeEnd = actualFragment.b;

        // TODO use split operation here
        ListNode<Integer> keyOffsetsToRemove = null;

        NodesIterator<SQLDocumentScriptItemSyntaxContext> it1 = this.scriptItems.nodesIteratorAt(0);
        int off1 = it1.getCurrOffset();
        if (it1.getCurrValue() != null && off1 + it1.getCurrValue().length() < rangeStart) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off1);
            this.forEachListener(l -> l.onScriptItemInvalidated(it1.getCurrValue()));
        }
        while (it1.next() && it1.getCurrValue() != null && (off1 = it1.getCurrOffset()) + it1.getCurrValue().length() < rangeStart) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off1);
            this.forEachListener(l -> l.onScriptItemInvalidated(it1.getCurrValue()));
        }
        int actualStart = it1.next() ? it1.getCurrOffset() : Integer.MAX_VALUE;

        NodesIterator<SQLDocumentScriptItemSyntaxContext> it2 = this.scriptItems.nodesIteratorAt(Integer.MAX_VALUE);
        int off2 = it2.getCurrOffset();
        if (it2.getCurrValue() != null && off2 > rangeEnd) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off2);
            this.forEachListener(l -> l.onScriptItemInvalidated(it2.getCurrValue()));
        }
        while (it2.prev() && it2.getCurrValue() != null && (off2 = it2.getCurrOffset()) > rangeEnd) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off2);
            this.forEachListener(l -> l.onScriptItemInvalidated(it2.getCurrValue()));
        }
        int actualEnd = it1.prev() ? it2.getCurrOffset() : 0;

        int droppedCount = 0;
        for (ListNode<Integer> kn = keyOffsetsToRemove; kn != null; kn = kn.next) {
            this.scriptItems.removeAt(kn.data);
            droppedCount++;
        }

        return actualEnd >= actualStart ? new Interval(actualStart, actualEnd) : new Interval(actualStart, 0);
    }
}
