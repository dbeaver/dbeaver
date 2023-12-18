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

import org.antlr.v4.runtime.misc.Interval;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Region;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.editors.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.ui.editors.sql.semantics.SQLDocumentScriptItemSyntaxContext.TokenEntryAtOffset;
import org.jkiss.dbeaver.utils.ListNode;

public class SQLDocumentSyntaxContext {
    public static class ScriptItemAtOffset {
        public final int offset;
        public final SQLDocumentScriptItemSyntaxContext item;
        
        public ScriptItemAtOffset(int offset, SQLDocumentScriptItemSyntaxContext item) {
            this.offset = offset;
            this.item = item;
        }
    }
    
    private static final Log log = Log.getLog(SQLDocumentSyntaxContext.class);
    
    private final OffsetKeyedTreeMap<SQLDocumentScriptItemSyntaxContext> scriptItems = new OffsetKeyedTreeMap<>();
    
    private int lastTokenAccessOffset = Integer.MAX_VALUE;
    private int lastAccessedTokenOffset = Integer.MAX_VALUE;
    private SQLQuerySymbolEntry lastAccessedTokenEntry = null;
    
    private int lastItemAccessOffset = Integer.MAX_VALUE;
    private int lastAccessedItemOffset = Integer.MAX_VALUE;
    private SQLDocumentScriptItemSyntaxContext lastAccessedScriptItem = null;
    
    public SQLDocumentSyntaxContext() {
    }
    
    public ScriptItemAtOffset findScriptItem(int offset) {
        if (offset == this.lastItemAccessOffset) {
            // found, do nothing
        } else if (offset >= this.lastAccessedItemOffset && this.lastAccessedScriptItem != null &&
            offset < this.lastAccessedItemOffset + this.lastAccessedScriptItem.length()) {
            this.lastItemAccessOffset = offset;
        } else {
            NodesIterator<SQLDocumentScriptItemSyntaxContext> it = scriptItems.nodesIteratorAt(offset);
            SQLDocumentScriptItemSyntaxContext scriptItem = it.getCurrValue();
            int itemOffset = it.getCurrOffset();
            if (scriptItem == null && it.prev()) {
                scriptItem = it.getCurrValue();
                itemOffset = it.getCurrOffset();
            }
            
            this.lastItemAccessOffset = offset;
            if (scriptItem != null && itemOffset <= offset && itemOffset + scriptItem.length() > offset) {
                this.lastAccessedItemOffset = itemOffset;
                this.lastAccessedScriptItem = scriptItem;
            } else {
                this.lastAccessedItemOffset = Integer.MAX_VALUE;
                this.lastAccessedScriptItem = null;
            }
        }
        return this.lastAccessedScriptItem == null ? null : new ScriptItemAtOffset(this.lastAccessedItemOffset, this.lastAccessedScriptItem);
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
            ScriptItemAtOffset scriptItem = this.findScriptItem(offset);
            TokenEntryAtOffset token = scriptItem == null ? null : scriptItem.item.findToken(offset - scriptItem.offset);

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
    
    public void resetLastAccessCache() {
        this.lastTokenAccessOffset = Integer.MAX_VALUE;
        this.lastAccessedTokenOffset = Integer.MAX_VALUE;
        this.lastAccessedTokenEntry = null;
        
        this.lastItemAccessOffset = Integer.MAX_VALUE;
        this.lastAccessedItemOffset = Integer.MAX_VALUE;
        this.lastAccessedScriptItem = null;
    }
    
    public SQLDocumentScriptItemSyntaxContext registerScriptItemContext(int offset, int length) {
        SQLDocumentScriptItemSyntaxContext scriptItem = new SQLDocumentScriptItemSyntaxContext(length);
        this.scriptItems.put(offset, scriptItem);
        return scriptItem;
    }
    
    public IRegion applyDelta(int offset, int oldLength, int newLength) {
        IRegion affectedRegion;
        if (oldLength > 0) {
//            ScriptItemAtOffset scriptItem = this.findScriptItem(offset);
//            if (scriptItem != null && scriptItem.offset <= offset && scriptItem.offset + scriptItem.item.length() > offset + oldLength) {
//            }
//            
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
                    lastAffectedOffset = currOffset + currItem.length();
                } else if (it.prev()) {
                    currOffset = it.getCurrOffset();
                    currItem = it.getCurrValue();
                    if  (currOffset <= offset && currOffset + currItem.length() > offset) {
                        keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, currOffset);
                        lastAffectedOffset = currOffset + currItem.length();
                    }
                } else {
                    lastAffectedOffset = offset + oldLength;
                }
                while (it.next() && (delta < 0 || lastAffectedOffset <= (offset + oldLength))) {
                    currOffset = it.getCurrOffset();
                    currItem = it.getCurrValue();
                    keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, currOffset);
                    lastAffectedOffset = currOffset + currItem.length();
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
            ScriptItemAtOffset scriptItem = this.findScriptItem(offset);
            if (scriptItem != null) {
                scriptItem.item.applyDelta(offset, oldLength, newLength);
                affectedRegion = new Region(scriptItem.offset, scriptItem.item.length());
            } else {
                NodesIterator<SQLDocumentScriptItemSyntaxContext> it = this.scriptItems.nodesIteratorAt(offset);
                int start = it.prev() ? it.getCurrOffset() + it.getCurrValue().length() : 0;
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

    public void clear() {
        this.scriptItems.clear();
        this.resetLastAccessCache();
    }

    public Interval dropInvisibleScriptItems(Interval actualFragment) {
        int rangeStart = actualFragment.a; 
        int rangeEnd = actualFragment.b;

        // TODO use split operation here
        ListNode<Integer> keyOffsetsToRemove = null;

        NodesIterator<SQLDocumentScriptItemSyntaxContext> it1 = this.scriptItems.nodesIteratorAt(0);
        int off1 = it1.getCurrOffset();
        if (it1.getCurrValue() != null && off1 < rangeStart) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off1);
        }
        while (it1.next() && (off1 = it1.getCurrOffset()) < rangeStart) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off1);
        }
        int actualStart = it1.next() ? it1.getCurrOffset() : Integer.MAX_VALUE; 

        NodesIterator<SQLDocumentScriptItemSyntaxContext> it2 = this.scriptItems.nodesIteratorAt(Integer.MAX_VALUE);
        int off2 = it2.getCurrOffset();
        if (it2.getCurrValue() != null && off2 > rangeEnd) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off2);
        }
        while (it2.prev() && (off2 = it2.getCurrOffset()) > rangeEnd) {
            keyOffsetsToRemove = ListNode.push(keyOffsetsToRemove, off2);
        }
        int actualEnd = it1.prev() ? it2.getCurrOffset() : 0; 
        
        int droppedCount = 0;
        for (ListNode<Integer> kn = keyOffsetsToRemove; kn != null; kn = kn.next) {
            this.scriptItems.removeAt(kn.data);
            droppedCount++;
        }
        // System.out.println("dropped " + droppedCount + ", kept " + this.scriptItems.size());
        
        return new Interval(actualStart, actualEnd); 
    }
}
