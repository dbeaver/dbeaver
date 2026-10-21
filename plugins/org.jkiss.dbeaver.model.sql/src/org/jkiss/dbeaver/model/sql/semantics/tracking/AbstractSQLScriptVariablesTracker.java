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
    /**
     * Parser coverage awaiting commitment to the contiguous observation frontier.
     */
    private record ObservedRange(int start, int end) {
        @NotNull
        private ObservedRange merge(int start, int end) {
            if (end <= start) {
                return this;
            }
            if (start <= this.end && end >= this.start) {
                return new ObservedRange(Math.min(start, this.start), Math.max(end, this.end));
            }
            return start < this.start ? new ObservedRange(start, end) : this;
        }

        @Nullable
        private ObservedRange truncateAt(int offset) {
            if (offset >= this.end) {
                return this;
            }
            return offset <= this.start ? null : new ObservedRange(this.start, offset);
        }
    }

    /**
     * Structural removal queued by reconciliation. The clear-all variant represents the empty-document reset signal.
     */
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

    /**
     * Minimal immutable representation retained from parser output. Variable text supports lazy recognition, while
     * batch delimiters define scope boundaries without requiring a semantic model.
     */
    protected record TrackedElement(
        int length,
        @Nullable String variableStatementText,
        boolean batchDelimiter
    ) {
        /** Creates parser-derived structural knowledge, retaining text only for parser-identified variable statements. */
        @NotNull
        protected static TrackedElement variableStatement(@NotNull SQLScriptElement element) {
            return new TrackedElement(
                element.getLength(),
                element instanceof SQLQuery query && query.hasVariableKeyword() ? element.getOriginalText() : null,
                false
            );
        }

        /** Creates structural knowledge backed by a successfully accepted variable-statement model. */
        @NotNull
        protected static TrackedElement analyzedVariableStatement(@NotNull SQLScriptElement element) {
            return new TrackedElement(element.getLength(), element.getOriginalText(), false);
        }

        /** Creates a structural batch boundary that carries no variable-statement text. */
        @NotNull
        protected static TrackedElement batchDelimiter(@NotNull SQLBatchDelimiterElement element) {
            return new TrackedElement(element.getLength(), null, true);
        }

        /** Returns the first document position affected by this element. */
        int coverageStart(int elementOffset) {
            return elementOffset;
        }

        /** Returns the exclusive end of document coverage affected by this element. */
        private int coverageEnd(int elementOffset) {
            return elementOffset + this.length;
        }
    }

    /**
     * Protects cheap producer-side updates submitted by reconciliation and background parsing.
     */
    @NotNull
    private final Object queueLock = new Object();
    /**
     * Protects committed elements and all derived semantic state maintained by subclasses.
     */
    @NotNull
    private final Object stateLock = new Object();
    /**
     * Presence updates waiting to be applied to {@link #trackedElements}.
     */
    @NotNull
    private final OffsetKeyedTreeMap<TrackedElement> queuedElements = new OffsetKeyedTreeMap<>();
    /**
     * Parser-observed range waiting to advance {@link #lastObservedPosition}. It represents evidence about the current
     * document, so an edit truncates it at the edit offset rather than translating it to new document coordinates.
     */
    @Nullable
    private ObservedRange queuedObservedRange;
    /**
     * Pending removal range applied to {@link #trackedElements} before queued replacements are inserted. Unlike
     * {@link #queuedObservedRange}, it follows document deltas so that the previously queued removal still reaches
     * stale elements after their offsets change.
     */
    @Nullable
    private InvalidationRange queuedInvalidationRange;
    /**
     * Committed relevant elements indexed by their current document offsets.
     */
    @NotNull
    private final OffsetKeyedTreeMap<TrackedElement> trackedElements = new OffsetKeyedTreeMap<>();
    /**
     * Inclusive end of the contiguous document prefix whose relevant elements and absences have been observed.
     * Pending observation advances this frontier only after its elements and invalidation are consumed.
     */
    private int lastObservedPosition = -1;

    @Override
    public final int getLastCoveredPosition() {
        synchronized (this.stateLock) {
            this.consumeQueuedElements();
            return this.lastObservedPosition;
        }
    }

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
                this.queuedObservedRange = null;
                this.queuedInvalidationRange = InvalidationRange.forClearAll();
            } else {
                removeElementsInRange(this.queuedElements, offset, end);
                this.queuedInvalidationRange = this.queuedInvalidationRange == null
                    ? new InvalidationRange(offset, end, false)
                    : this.queuedInvalidationRange.expand(offset, end);
                this.queuedObservedRange = this.queuedObservedRange == null
                    ? new ObservedRange(offset, end)
                    : this.queuedObservedRange.merge(offset, end);
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
                if (this.queuedObservedRange != null) {
                    this.queuedObservedRange = this.queuedObservedRange.truncateAt(offset);
                }
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
            if (offset <= this.lastObservedPosition) {
                this.lastObservedPosition = offset - 1;
            }
            this.applyDeltaToDerivedState(offset, oldLength, newLength);
            this.invalidateDerivedStateFrom(Math.min(offset, affectedElementStart));
        }
    }

    @Override
    public final void clear() {
        synchronized (this.stateLock) {
            synchronized (this.queueLock) {
                this.queuedElements.clear();
                this.queuedObservedRange = null;
                this.queuedInvalidationRange = null;
            }
            this.trackedElements.clear();
            this.lastObservedPosition = -1;
            this.clearDerivedState();
        }
    }

    /**
     * Converts parser output into the minimal element representation required by the concrete scope strategy.
     *
     * @param element parser-produced script element
     * @return retained representation, or {@code null} when the element is irrelevant
     */
    @Nullable
    protected abstract TrackedElement createTrackedElement(@NotNull SQLScriptElement element);

    /**
     * Lets subclasses transform derived positional caches after a document replacement.
     */
    protected void applyDeltaToDerivedState(int offset, int oldLength, int newLength) {
    }

    /**
     * Invalidates derived semantic state beginning at the earliest changed structural position.
     */
    protected abstract void invalidateDerivedStateFrom(int offset);

    /**
     * Clears all subclass-owned semantic state.
     */
    protected abstract void clearDerivedState();

    /**
     * Notifies subclasses that the retained representation at an offset changed.
     */
    protected void trackedElementChanged(int offset) {
    }

    /**
     * Returns the lock guarding committed and derived tracker state.
     */
    @NotNull
    protected final Object getStateLock() {
        return this.stateLock;
    }

    /**
     * Returns committed structural elements. Callers must hold {@link #getStateLock()}.
     */
    @NotNull
    protected final OffsetKeyedTreeMap<TrackedElement> getTrackedElements() {
        return this.trackedElements;
    }

    /**
     * Atomically drains producer-side updates, applies invalidation before replacement elements, advances observation
     * coverage, and invalidates subclass-derived state. Callers must hold {@link #getStateLock()}.
     */
    protected final void consumeQueuedElements() {
        List<OffsetKeyedTreeMap.ValueAndOffset<TrackedElement>> pending = new ArrayList<>();
        ObservedRange observedRange;
        InvalidationRange invalidationRange;
        synchronized (this.queueLock) {
            this.queuedElements.forEach((offset, element) ->
                pending.add(new OffsetKeyedTreeMap.ValueAndOffset<>(element, offset)));
            this.queuedElements.clear();
            observedRange = this.queuedObservedRange;
            this.queuedObservedRange = null;
            invalidationRange = this.queuedInvalidationRange;
            this.queuedInvalidationRange = null;
        }
        boolean changed = false;
        int invalidatedFrom;
        if (invalidationRange == null) {
            invalidatedFrom = Integer.MAX_VALUE;
        } else if (invalidationRange.clearAll) {
            this.trackedElements.clear();
            this.lastObservedPosition = -1;
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
        if (observedRange != null && observedRange.start <= this.lastObservedPosition + 1) {
            this.lastObservedPosition = Math.max(this.lastObservedPosition, observedRange.end - 1);
        }
        if (changed) {
            this.invalidateDerivedStateFrom(invalidatedFrom);
        }
    }

    /**
     * Returns whether an element is a parser-identified variable statement.
     */
    protected static boolean isVariableStatement(@NotNull SQLScriptElement element) {
        return element instanceof SQLQuery query && query.hasVariableKeyword();
    }

    /**
     * Extracts variable operations from an accepted variable-statement model in source order.
     */
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

    /**
     * Applies a document replacement to an offset-indexed element map and removes representations intersecting the
     * changed text. Deletions conservatively discard the tail because its offsets and parser interpretation may change.
     *
     * @return earliest removed element coverage, or {@link Integer#MAX_VALUE} when no element was removed
     */
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

    /** Removes retained elements whose coverage overlaps the half-open range. */
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
