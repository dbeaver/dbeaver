/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.sql.semantics.tracking;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.sql.SQLBatchDelimiterElement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap;
import org.jkiss.dbeaver.model.sql.semantics.OffsetKeyedTreeMap.NodesIterator;
import org.jkiss.dbeaver.model.sql.semantics.SQLQueryVariableInfo;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryModel;
import org.jkiss.dbeaver.model.sql.semantics.model.SQLQueryVariableStatementModel;
import org.jkiss.dbeaver.utils.ListNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.Function;

/**
 * Maintains the position-indexed script elements used by document-ordered trackers.
 * It owns queuing, reconciliation, edit-delta handling, and synchronization; subclasses interpret
 * the resulting elements into visible variable state and invalidate that derived state as needed.
 */
abstract class AbstractSQLScriptVariablesTracker implements SQLScriptVariablesTracker {
    private record InvalidationRange(int start, int end, boolean clearAll) {
        @NotNull
        private static InvalidationRange forClearAll() {
            return new InvalidationRange(0, 0, true);
        }

        @NotNull
        private InvalidationRange expand(int start, int end) {
            return this.clearAll
                ? this
                : new InvalidationRange(Math.min(this.start, start), Math.max(this.end, end), false);
        }

        @NotNull
        private InvalidationRange applyDelta(int offset, int oldLength, int newLength) {
            if (this.clearAll) {
                return this;
            }
            int editEnd = offset + oldLength;
            int delta = newLength - oldLength;
            if (editEnd <= this.start) {
                return new InvalidationRange(this.start + delta, this.end + delta, false);
            }
            if (offset >= this.end) {
                return this;
            }
            return new InvalidationRange(
                Math.min(this.start, offset),
                Math.max(offset + newLength, this.end + delta),
                false
            );
        }
    }

    protected record TrackedElement(
        int length,
        @Nullable String variableStatementText,
        boolean batchDelimiter
    ) {
        @NotNull
        protected static TrackedElement variableStatement(@NotNull SQLScriptElement element) {
            return new TrackedElement(
                element.getLength(),
                element instanceof SQLQuery query && query.hasVariableKeyword() ? element.getOriginalText() : null,
                false
            );
        }

        @NotNull
        protected static TrackedElement analyzedVariableStatement(@NotNull SQLScriptElement element) {
            return new TrackedElement(element.getLength(), element.getOriginalText(), false);
        }

        @NotNull
        protected static TrackedElement batchDelimiter(@NotNull SQLBatchDelimiterElement element) {
            return new TrackedElement(element.getLength(), null, true);
        }

        int coverageStart(int elementOffset) {
            return elementOffset;
        }

        private int coverageEnd(int elementOffset) {
            return elementOffset + this.length;
        }
    }

    @NotNull
    private final Object queueLock = new Object();
    @NotNull
    private final Object stateLock = new Object();
    @NotNull
    private final OffsetKeyedTreeMap<TrackedElement> queuedElements = new OffsetKeyedTreeMap<>();
    @Nullable
    private InvalidationRange queuedInvalidationRange;
    @NotNull
    private final OffsetKeyedTreeMap<TrackedElement> trackedElements = new OffsetKeyedTreeMap<>();

    @Override
    public final void trackElements(@NotNull Collection<? extends SQLScriptElement> elements) {
        synchronized (this.queueLock) {
            for (SQLScriptElement element : elements) {
                TrackedElement trackedElement = this.createTrackedElement(element);
                if (trackedElement != null) {
                    this.queuedElements.put(element.getOffset(), trackedElement);
                }
            }
        }
    }

    @Override
    public final void reconcileElements(
        int offset,
        int length,
        @NotNull Collection<? extends SQLScriptElement> elements
    ) {
        int end = offset + length;
        synchronized (this.queueLock) {
            if (offset == 0 && length == 0) {
                this.queuedElements.clear();
                this.queuedInvalidationRange = InvalidationRange.forClearAll();
            } else {
                removeElementsInRange(this.queuedElements, offset, end);
                this.queuedInvalidationRange = this.queuedInvalidationRange == null
                    ? new InvalidationRange(offset, end, false)
                    : this.queuedInvalidationRange.expand(offset, end);
            }
            for (SQLScriptElement element : elements) {
                TrackedElement trackedElement = this.createTrackedElement(element);
                if (trackedElement != null) {
                    this.queuedElements.put(element.getOffset(), trackedElement);
                }
            }
        }
    }

    @Override
    public final void applyDelta(int offset, int oldLength, int newLength) {
        if (oldLength == 0 && newLength == 0) {
            return;
        }
        synchronized (this.stateLock) {
            synchronized (this.queueLock) {
                applyDeltaToElements(this.queuedElements, offset, oldLength, newLength, Function.identity());
                if (this.queuedInvalidationRange != null) {
                    this.queuedInvalidationRange = this.queuedInvalidationRange.applyDelta(offset, oldLength, newLength);
                }
            }
            int affectedElementStart = applyDeltaToElements(
                this.trackedElements,
                offset,
                oldLength,
                newLength,
                Function.identity()
            );
            this.applyDeltaToDerivedState(offset, oldLength, newLength);
            this.invalidateDerivedStateFrom(Math.min(offset, affectedElementStart));
        }
    }

    @Override
    public final void clear() {
        synchronized (this.stateLock) {
            synchronized (this.queueLock) {
                this.queuedElements.clear();
                this.queuedInvalidationRange = null;
            }
            this.trackedElements.clear();
            this.clearDerivedState();
        }
    }

    @Nullable
    protected abstract TrackedElement createTrackedElement(@NotNull SQLScriptElement element);

    protected void applyDeltaToDerivedState(int offset, int oldLength, int newLength) {
    }

    protected abstract void invalidateDerivedStateFrom(int offset);

    protected abstract void clearDerivedState();

    protected void trackedElementChanged(int offset) {
    }

    @NotNull
    protected final Object getStateLock() {
        return this.stateLock;
    }

    @NotNull
    protected final OffsetKeyedTreeMap<TrackedElement> getTrackedElements() {
        return this.trackedElements;
    }

    protected final void consumeQueuedElements() {
        List<OffsetKeyedTreeMap.ValueAndOffset<TrackedElement>> pending = new ArrayList<>();
        InvalidationRange invalidationRange;
        synchronized (this.queueLock) {
            this.queuedElements.forEach((offset, element) ->
                pending.add(new OffsetKeyedTreeMap.ValueAndOffset<>(element, offset)));
            this.queuedElements.clear();
            invalidationRange = this.queuedInvalidationRange;
            this.queuedInvalidationRange = null;
        }
        boolean changed = false;
        int invalidatedFrom;
        if (invalidationRange == null) {
            invalidatedFrom = Integer.MAX_VALUE;
        } else if (invalidationRange.clearAll) {
            this.trackedElements.clear();
            invalidatedFrom = 0;
            changed = true;
        } else {
            removeElementsInRange(this.trackedElements, invalidationRange.start, invalidationRange.end);
            invalidatedFrom = invalidationRange.start;
            changed = true;
        }
        for (OffsetKeyedTreeMap.ValueAndOffset<TrackedElement> item : pending) {
            TrackedElement previous = this.trackedElements.put(item.offset, item.value);
            if (!item.value.equals(previous)) {
                invalidatedFrom = Math.min(invalidatedFrom, item.value.coverageStart(item.offset));
                this.trackedElementChanged(item.offset);
                changed = true;
            }
        }
        if (changed) {
            this.invalidateDerivedStateFrom(invalidatedFrom);
        }
    }

    protected static boolean isVariableStatement(@NotNull SQLScriptElement element) {
        return element instanceof SQLQuery query && query.hasVariableKeyword();
    }

    @NotNull
    protected static List<SQLQueryVariableInfo> getIntroducedVariables(@Nullable SQLQueryModel model) {
        if (model == null || !(model.getQueryModel() instanceof SQLQueryVariableStatementModel statement)) {
            return Collections.emptyList();
        }
        List<SQLQueryVariableInfo> result = new ArrayList<>();
        for (
            ListNode<SQLQueryVariableInfo> node = statement.getResultingVariables().getIntroducedVariables();
            node != null;
            node = node.next
        ) {
            result.add(node.data);
        }
        result.sort(Comparator.comparingInt(SQLQueryVariableInfo::relativeOffset));
        return result;
    }

    protected static <T> int applyDeltaToElements(
        @NotNull OffsetKeyedTreeMap<T> elements,
        int offset,
        int oldLength,
        int newLength,
        @NotNull Function<T, TrackedElement> trackedElementAccessor
    ) {
        int editEnd = offset + oldLength;
        int delta = newLength - oldLength;
        List<Integer> affectedOffsets = new ArrayList<>();
        int affectedElementStart = Integer.MAX_VALUE;
        NodesIterator<T> iterator = elements.nodesIteratorAt(offset);
        T value = iterator.getCurrValue();
        if (value == null && iterator.prev()) {
            value = iterator.getCurrValue();
        } else if (value == null && iterator.next()) {
            value = iterator.getCurrValue();
        }

        if (delta < 0) {
            if (value != null && trackedElementAccessor.apply(value).coverageEnd(iterator.getCurrOffset()) > offset) {
                affectedOffsets.add(iterator.getCurrOffset());
                affectedElementStart = trackedElementAccessor.apply(value).coverageStart(iterator.getCurrOffset());
            }
            while (iterator.next()) {
                affectedOffsets.add(iterator.getCurrOffset());
                affectedElementStart = Math.min(
                    affectedElementStart,
                    trackedElementAccessor.apply(iterator.getCurrValue()).coverageStart(iterator.getCurrOffset())
                );
            }
        } else {
            while (value != null) {
                int elementOffset = iterator.getCurrOffset();
                TrackedElement element = trackedElementAccessor.apply(value);
                int coverageStart = element.coverageStart(elementOffset);
                boolean affected = oldLength == 0
                    ? coverageStart <= offset && element.coverageEnd(elementOffset) >= offset
                    : coverageStart < editEnd && element.coverageEnd(elementOffset) > offset;
                if (affected) {
                    affectedOffsets.add(elementOffset);
                    affectedElementStart = Math.min(affectedElementStart, element.coverageStart(elementOffset));
                }
                if (coverageStart > (oldLength == 0 ? offset : editEnd) || !iterator.next()) {
                    break;
                }
                value = iterator.getCurrValue();
            }
        }
        for (int affectedOffset : affectedOffsets) {
            elements.removeAt(affectedOffset);
        }
        if (delta > 0) {
            elements.applyOffset(editEnd, delta);
        }
        return affectedElementStart;
    }

    private static void removeElementsInRange(
        @NotNull OffsetKeyedTreeMap<TrackedElement> elements,
        int start,
        int end
    ) {
        if (end <= start) {
            return;
        }
        List<Integer> offsets = new ArrayList<>();
        NodesIterator<TrackedElement> iterator = elements.nodesIteratorAt(start);
        TrackedElement element = iterator.getCurrValue();
        if (element == null && !iterator.prev() && !iterator.next()) {
            return;
        }
        do {
            int elementOffset = iterator.getCurrOffset();
            element = iterator.getCurrValue();
            if (element.coverageStart(elementOffset) >= end) {
                break;
            }
            if (element.coverageEnd(elementOffset) > start) {
                offsets.add(elementOffset);
            }
        } while (iterator.next());
        for (int elementOffset : offsets) {
            elements.removeAt(elementOffset);
        }
    }
}
