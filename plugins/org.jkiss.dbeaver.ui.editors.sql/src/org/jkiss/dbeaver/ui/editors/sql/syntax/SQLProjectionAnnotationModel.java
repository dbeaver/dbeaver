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
package org.jkiss.dbeaver.ui.editors.sql.syntax;

import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.jkiss.code.NotNull;

import java.util.*;

/**
 * Expands enclosed collapsed folds before collapsing a parent region.
 * Remembers enclosed collapse state and restores it when the parent is expanded.
 * <p>
 * Nested collapse/expand behavior is covered by manual UI tests only (requires SWT projection viewer).
 * Manual test plan:
 * <ul>
 *   <li>Collapse OUTER with expanded INNER → expand OUTER → INNER collapse state restored</li>
 *   <li>Toggle expansion is symmetric for parent and enclosed regions</li>
 *   <li>Manually expand INNER then expand OUTER → INNER stays expanded ({@code forgetDeferredCollapse})</li>
 * </ul>
 */
public class SQLProjectionAnnotationModel extends ProjectionAnnotationModel {

    private final Map<Integer, Set<Integer>> deferredCollapsedEnclosedOffsets = new HashMap<>();

    @Override
    public void collapse(Annotation annotation) {
        saveAndExpandEnclosedCollapsed(annotation);
        super.collapse(annotation);
    }

    @Override
    public void expand(Annotation annotation) {
        super.expand(annotation);
        forgetDeferredCollapse(annotation);
        restoreDeferredCollapsedEnclosed(annotation);
    }

    @Override
    public void toggleExpansionState(Annotation annotation) {
        boolean wasCollapsed = annotation instanceof ProjectionAnnotation projectionAnnotation && projectionAnnotation.isCollapsed();
        if (!wasCollapsed) {
            saveAndExpandEnclosedCollapsed(annotation);
        }
        super.toggleExpansionState(annotation);
        if (wasCollapsed) {
            forgetDeferredCollapse(annotation);
            restoreDeferredCollapsedEnclosed(annotation);
        }
    }

    private void forgetDeferredCollapse(@NotNull Annotation annotation) {
        Position position = getPosition(annotation);
        if (position == null) {
            return;
        }
        int offset = position.getOffset();
        for (Set<Integer> offsets : deferredCollapsedEnclosedOffsets.values()) {
            offsets.remove(offset);
        }
        deferredCollapsedEnclosedOffsets.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void saveAndExpandEnclosedCollapsed(@NotNull Annotation outerAnnotation) {
        Position outerPosition = getPosition(outerAnnotation);
        if (outerPosition == null) {
            return;
        }
        Set<Integer> enclosedCollapsedOffsets = collectEnclosedCollapsedOffsets(outerPosition, outerAnnotation);
        if (enclosedCollapsedOffsets.isEmpty()) {
            return;
        }
        deferredCollapsedEnclosedOffsets.put(outerPosition.getOffset(), enclosedCollapsedOffsets);
        expandEnclosedCollapsed(outerPosition, outerAnnotation);
    }

    @NotNull
    private Set<Integer> collectEnclosedCollapsedOffsets(
        @NotNull Position outerPosition,
        @NotNull Annotation outerAnnotation
    ) {
        int outerStart = outerPosition.getOffset();
        int outerEnd = outerStart + outerPosition.getLength();
        Set<Integer> enclosedCollapsedOffsets = new HashSet<>();
        Iterator<Annotation> it = getAnnotationIterator();
        while (it.hasNext()) {
            Annotation enclosedAnnotation = it.next();
            if (enclosedAnnotation == outerAnnotation
                || !(enclosedAnnotation instanceof ProjectionAnnotation inner)
                || !inner.isCollapsed()
            ) {
                continue;
            }
            Position innerPosition = getPosition(enclosedAnnotation);
            if (innerPosition == null) {
                continue;
            }
            int innerStart = innerPosition.getOffset();
            int innerEnd = innerStart + innerPosition.getLength();
            if (isStrictlyEnclosed(innerStart, innerEnd, outerStart, outerEnd)) {
                enclosedCollapsedOffsets.add(innerStart);
            }
        }
        return enclosedCollapsedOffsets;
    }

    private void expandEnclosedCollapsed(
        @NotNull Position outerPosition,
        @NotNull Annotation outerAnnotation
    ) {
        int outerStart = outerPosition.getOffset();
        int outerEnd = outerStart + outerPosition.getLength();
        Iterator<Annotation> it = getAnnotationIterator();
        while (it.hasNext()) {
            Annotation enclosedAnnotation = it.next();
            if (enclosedAnnotation == outerAnnotation
                || !(enclosedAnnotation instanceof ProjectionAnnotation inner)
                || !inner.isCollapsed()
            ) {
                continue;
            }
            Position innerPosition = getPosition(enclosedAnnotation);
            if (innerPosition == null) {
                continue;
            }
            int innerStart = innerPosition.getOffset();
            int innerEnd = innerStart + innerPosition.getLength();
            if (isStrictlyEnclosed(innerStart, innerEnd, outerStart, outerEnd)) {
                super.expand(enclosedAnnotation);
            }
        }
    }

    private void restoreDeferredCollapsedEnclosed(@NotNull Annotation outerAnnotation) {
        Position outerPosition = getPosition(outerAnnotation);
        if (outerPosition == null) {
            return;
        }
        Set<Integer> deferredOffsets = deferredCollapsedEnclosedOffsets.remove(outerPosition.getOffset());
        if (deferredOffsets == null || deferredOffsets.isEmpty()) {
            return;
        }
        List<ProjectionAnnotation> toCollapse = new ArrayList<>();
        Iterator<Annotation> it = getAnnotationIterator();
        while (it.hasNext()) {
            Annotation annotation = it.next();
            if (!(annotation instanceof ProjectionAnnotation projectionAnnotation)) {
                continue;
            }
            Position position = getPosition(annotation);
            if (position != null && deferredOffsets.contains(position.getOffset())) {
                toCollapse.add(projectionAnnotation);
            }
        }
        toCollapse.sort(Comparator.comparingInt(annotation -> {
            Position position = getPosition(annotation);
            return position != null ? position.getLength() : Integer.MAX_VALUE;
        }));
        for (ProjectionAnnotation annotation : toCollapse) {
            super.collapse(annotation);
        }
    }

    private static boolean isStrictlyEnclosed(int innerStart, int innerEnd, int outerStart, int outerEnd) {
        return innerStart >= outerStart && innerEnd <= outerEnd
            && (innerStart > outerStart || innerEnd < outerEnd);
    }
}
