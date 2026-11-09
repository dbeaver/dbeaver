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
package org.jkiss.dbeaver.ui.controls.resultset.spreadsheet;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeConstraint;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetModel;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;

import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Reversible column visibility rules for one spreadsheet. Applies an overlay to visible attributes without changing
 * the data filter, which retains the user's manual visibility choices across reloads.
 */
final class ResultSetColumnFilter {
    enum Criterion {
        NULL_VALUES,
        EMPTY_STRINGS
    }

    private final Set<Criterion> criteria = EnumSet.noneOf(Criterion.class);
    private final Set<DBDAttributeBinding> hiddenColumns = new LinkedHashSet<>();

    boolean isEnabled(@NotNull Criterion criterion) {
        return criteria.contains(criterion);
    }

    void toggle(@NotNull Criterion criterion) {
        if (!criteria.remove(criterion)) {
            criteria.add(criterion);
        }
    }

    void disable() {
        criteria.clear();
    }

    boolean hasHiddenColumns() {
        return !hiddenColumns.isEmpty();
    }

    boolean apply(@NotNull ResultSetModel model) {
        List<ResultSetRow> rows = model.getAllRows();
        // No rows provide no evidence that a column is empty. Keep the headers accessible in an empty result.
        if (criteria.isEmpty() || rows.isEmpty()) {
            return restore(model);
        }

        Set<DBDAttributeBinding> candidates = new LinkedHashSet<>(model.getVisibleAttributes());
        candidates.addAll(hiddenColumns);
        List<DBDAttributeBinding> visibleColumns = model.getVisibleAttributes();
        boolean changed = false;
        for (DBDAttributeBinding attribute : candidates) {
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attribute);
            if (constraint == null || !constraint.isVisible()) {
                hiddenColumns.remove(attribute);
                continue;
            }
            boolean hide = true;
            for (ResultSetRow row : rows) {
                if (!matches(model.getCellValue(attribute, row))) {
                    hide = false;
                    break;
                }
            }
            // Keep the data filter's visibility as the user's choice. Copies of it are reapplied after a reload,
            // and must not turn our temporary hiding into a permanent, manually hidden column.
            if (hide) {
                hiddenColumns.add(attribute);
                changed |= visibleColumns.remove(attribute);
            } else if (hiddenColumns.remove(attribute) && !visibleColumns.contains(attribute)) {
                visibleColumns.add(attribute);
                changed = true;
            }
        }
        if (changed) {
            sortVisibleColumns(model);
        }
        return changed;
    }

    /**
     * Restore our visibility changes before the model replaces its bindings or the presentation is disposed.
     * The enabled criteria survive a reload, but old bindings must not.
     */
    boolean restore(@NotNull ResultSetModel model) {
        List<DBDAttributeBinding> visibleColumns = model.getVisibleAttributes();
        boolean changed = false;
        for (DBDAttributeBinding attribute : hiddenColumns) {
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attribute);
            if (constraint != null && constraint.isVisible() && !visibleColumns.contains(attribute)) {
                visibleColumns.add(attribute);
                changed = true;
            }
        }
        hiddenColumns.clear();
        if (changed) {
            sortVisibleColumns(model);
        }
        return changed;
    }

    private boolean matches(@Nullable Object value) {
        return criteria.contains(Criterion.NULL_VALUES) && DBUtils.isNullValue(value)
            || criteria.contains(Criterion.EMPTY_STRINGS) && value instanceof String text && text.isEmpty();
    }

    private static void sortVisibleColumns(@NotNull ResultSetModel model) {
        // Restore the user's visual order, including gaps left by manually hidden columns.
        model.getVisibleAttributes().sort(Comparator.comparingInt(attribute -> {
            DBDAttributeConstraint constraint = model.getDataFilter().getConstraint(attribute);
            return constraint == null ? attribute.getOrdinalPosition() : constraint.getVisualPosition();
        }));
    }
}
