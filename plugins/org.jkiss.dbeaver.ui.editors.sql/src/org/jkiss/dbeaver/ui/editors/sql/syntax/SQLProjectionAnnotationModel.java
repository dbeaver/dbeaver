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

    private final Map<Annotation, Set<Annotation>> deferredCollapsedEnclosedAnnotations = new IdentityHashMap<>();

    @Override
    public void collapse(Annotation annotation) {
        pruneDeferredCollapseState();
        saveAndExpandEnclosedCollapsed(annotation);
        super.collapse(annotation);
    }

    @Override
    public void expand(Annotation annotation) {
        pruneDeferredCollapseState();
        super.expand(annotation);
        forgetDeferredCollapse(annotation);
        restoreDeferredCollapsedEnclosed(annotation);
    }

    @Override
    public void toggleExpansionState(Annotation annotation) {
        pruneDeferredCollapseState();
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

    private void pruneDeferredCollapseState() {
        deferredCollapsedEnclosedAnnotations.entrySet().removeIf(entry -> {
            if (getPosition(entry.getKey()) == null) {
                return true;
            }
            entry.getValue().removeIf(annotation -> getPosition(annotation) == null);
            return entry.getValue().isEmpty();
        });
    }

    private void forgetDeferredCollapse(@NotNull Annotation annotation) {
        for (Set<Annotation> annotations : deferredCollapsedEnclosedAnnotations.values()) {
            annotations.remove(annotation);
        }
        deferredCollapsedEnclosedAnnotations.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }

    private void saveAndExpandEnclosedCollapsed(@NotNull Annotation outerAnnotation) {
        Position outerPosition = getPosition(outerAnnotation);
        if (outerPosition == null) {
            return;
        }
        Set<Annotation> enclosedCollapsedAnnotations = collectEnclosedCollapsedAnnotations(outerPosition, outerAnnotation);
        if (enclosedCollapsedAnnotations.isEmpty()) {
            return;
        }
        deferredCollapsedEnclosedAnnotations.put(outerAnnotation, enclosedCollapsedAnnotations);
        expandEnclosedCollapsed(enclosedCollapsedAnnotations);
    }

    @NotNull
    private Set<Annotation> collectEnclosedCollapsedAnnotations(
        @NotNull Position outerPosition,
        @NotNull Annotation outerAnnotation
    ) {
        int outerStart = outerPosition.getOffset();
        int outerEnd = outerStart + outerPosition.getLength();
        Set<Annotation> enclosedCollapsedAnnotations = Collections.newSetFromMap(new IdentityHashMap<>());
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
                enclosedCollapsedAnnotations.add(enclosedAnnotation);
            }
        }
        return enclosedCollapsedAnnotations;
    }

    private void expandEnclosedCollapsed(
        @NotNull Set<Annotation> enclosedCollapsedAnnotations
    ) {
        for (Annotation enclosedAnnotation : enclosedCollapsedAnnotations) {
            if (enclosedAnnotation instanceof ProjectionAnnotation inner && inner.isCollapsed()
                && getPosition(enclosedAnnotation) != null
            ) {
                super.expand(enclosedAnnotation);
            }
        }
    }

    private void restoreDeferredCollapsedEnclosed(@NotNull Annotation outerAnnotation) {
        Set<Annotation> deferredAnnotations = deferredCollapsedEnclosedAnnotations.remove(outerAnnotation);
        if (deferredAnnotations == null || deferredAnnotations.isEmpty()) {
            return;
        }
        List<ProjectionAnnotation> toCollapse = new ArrayList<>();
        for (Annotation annotation : deferredAnnotations) {
            if (!(annotation instanceof ProjectionAnnotation projectionAnnotation)) {
                continue;
            }
            Position position = getPosition(annotation);
            if (position != null) {
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
