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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDValue;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.ResultSetValuePath;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.handler.ResultSetPropertyTester;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

final class ResultSetUndoRedoManager {
    private final ResultSetViewer viewer;
    private final List<CellEditHistoryItem> history = new ArrayList<>();
    private int historyPosition;

    ResultSetUndoRedoManager(@NotNull ResultSetViewer viewer) {
        this.viewer = viewer;
    }

    boolean updateCellValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull ResultSetRow row,
        @Nullable int[] rowIndexes,
        @Nullable Object value
    ) throws DBException {
        int historyLimit = Math.clamp(
            viewer.getPreferenceStore().getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL),
            0,
            ResultSetPreferences.MAX_EDIT_UNDO_LEVEL
        );
        if (historyLimit == 0) {
            clear();
            return viewer.getModel().updateCellValue(attribute, row, rowIndexes, value, true);
        }
        CellEditHistoryItem historyItem = makeHistoryItem(attribute, row, rowIndexes);
        boolean updated;
        try {
            updated = viewer.getModel().updateCellValue(attribute, row, rowIndexes, value, true);
        } catch (DBException e) {
            if (historyItem != null) {
                historyItem.release();
            }
            throw e;
        }
        if (updated && historyItem != null) {
            CellSnapshot newValue = snapshotCell(historyItem.valueLocation);
            if (newValue != null) {
                historyItem.setNewValue(newValue);
                addHistoryItem(historyItem);
            } else {
                historyItem.release();
                clear();
            }
        } else if (updated) {
            clear();
        } else if (historyItem != null) {
            historyItem.release();
        }
        return updated;
    }

    boolean resetCellValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull ResultSetRow row,
        @Nullable int[] rowIndexes
    ) {
        if (row.getState() == ResultSetRow.STATE_REMOVED) {
            viewer.getModel().resetCellValue(attribute, row, rowIndexes);
            return true;
        }
        if (!row.isChanged(attribute)) {
            return false;
        }
        int historyLimit = Math.clamp(
            viewer.getPreferenceStore().getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL),
            0,
            ResultSetPreferences.MAX_EDIT_UNDO_LEVEL
        );
        if (historyLimit == 0) {
            clear();
            viewer.getModel().resetCellValue(attribute, row, rowIndexes);
            return true;
        }
        CellEditHistoryItem historyItem = makeHistoryItem(attribute, row, rowIndexes);
        viewer.getModel().resetCellValue(attribute, row, rowIndexes);
        if (historyItem != null) {
            CellSnapshot newValue = snapshotCell(historyItem.valueLocation);
            if (newValue != null) {
                historyItem.setNewValue(newValue);
                addHistoryItem(historyItem);
            } else {
                historyItem.release();
                clear();
            }
        } else {
            clear();
        }
        return true;
    }

    boolean canUndo() {
        return canReplay() && historyPosition > 0;
    }

    boolean canRedo() {
        return canReplay() && historyPosition < history.size();
    }

    private boolean canReplay() {
        return !viewer.isActionsDisabled() && !viewer.isRefreshInProgress() && !viewer.getModel().isUpdateInProgress();
    }

    void undo() {
        if (!canUndo()) {
            return;
        }
        CellEditHistoryItem item = history.get(historyPosition - 1);
        if (applyHistoryItem(item, false)) {
            historyPosition--;
            updateActions();
        }
    }

    void redo() {
        if (!canRedo()) {
            return;
        }
        CellEditHistoryItem item = history.get(historyPosition);
        if (applyHistoryItem(item, true)) {
            historyPosition++;
            updateActions();
        }
    }

    void clear() {
        if (history.isEmpty()) {
            return;
        }
        for (CellEditHistoryItem item : history) {
            item.release();
        }
        history.clear();
        historyPosition = 0;
        updateActions();
    }

    void updateLimit() {
        if (trimToLimit()) {
            updateActions();
        }
    }

    private boolean applyHistoryItem(@NotNull CellEditHistoryItem item, boolean redo) {
        CellSnapshot snapshot = redo ? item.newValue : item.oldValue;
        ResultSetCellLocation valueCell = item.valueLocation;
        if (!viewer.getModel().containsRow(valueCell.getRow())) {
            clear();
            return false;
        }
        try {
            snapshot.restore(valueCell);
            viewer.getModel().refreshChangeCount();
        } catch (DBException e) {
            DBWorkbench.getPlatformUI().showError("Cell edit", "Error restoring cell value", e);
            return false;
        }

        ResultSetCellLocation cell = item.cellLocation;
        viewer.getActivePresentation().setCurrentCellLocation(cell);
        viewer.refreshHintCache(
            Collections.singletonList(cell.getAttribute()),
            Collections.singletonList(cell.getRow()),
            cell.getRowIndexes());
        viewer.redrawData(false, false);
        viewer.updatePanelsContent(false);
        viewer.updateEditControls();
        return true;
    }

    @Nullable
    private CellEditHistoryItem makeHistoryItem(
        @NotNull DBDAttributeBinding attribute,
        @NotNull ResultSetRow row,
        @Nullable int[] rowIndexes
    ) {
        ResultSetCellLocation currentCell = viewer.getActivePresentation().getCurrentCellLocation();
        ResultSetValuePath valuePath = currentCell != null
            && currentCell.getRow() == row
            && currentCell.getAttribute() == attribute
            && Arrays.equals(currentCell.getRowIndexes(), rowIndexes)
            ? currentCell.getValuePath()
            : null;
        ResultSetCellLocation cell = new ResultSetCellLocation(
            attribute,
            row,
            rowIndexes == null ? null : rowIndexes.clone(),
            valuePath);
        // Nested edits and resets affect the entire root, including its original value and child change markers.
        ResultSetCellLocation valueLocation = new ResultSetCellLocation(
            attribute.getLevel() == 0 ? attribute : attribute.getTopParent(), row);
        CellSnapshot oldValue = snapshotCell(valueLocation);
        if (oldValue == null) {
            return null;
        }
        return new CellEditHistoryItem(
            cell,
            valueLocation,
            oldValue);
    }

    @Nullable
    private static CellSnapshot snapshotCell(@NotNull ResultSetCellLocation location) {
        ResultSetRow row = location.getRow();
        DBDAttributeBinding root = location.getAttribute();
        ValueSnapshot value = snapshotValue(row.values[root.getOrdinalPosition()]);
        if (value == null) {
            return null;
        }
        ValueSnapshot originalValue = null;
        if (row.isChanged(root)) {
            originalValue = snapshotValue(row.getChange(root));
            if (originalValue == null) {
                value.release();
                return null;
            }
        }
        List<DBDAttributeBinding> changedChildren = new ArrayList<>();
        for (var change : row.getChanges()) {
            if (change.getValue() == root) {
                changedChildren.add(change.getKey());
            }
        }
        return new CellSnapshot(value, originalValue, changedChildren);
    }

    private void addHistoryItem(@NotNull CellEditHistoryItem item) {
        truncateRedo();
        history.add(item);
        historyPosition++;
        trimToLimit();
        updateActions();
    }

    private boolean trimToLimit() {
        int historyLimit = Math.clamp(
            viewer.getPreferenceStore().getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL),
            0,
            ResultSetPreferences.MAX_EDIT_UNDO_LEVEL
        );
        int undoToKeep = Math.min(historyPosition, (historyLimit + 1) / 2);
        int redoToKeep = Math.min(history.size() - historyPosition, historyLimit - undoToKeep);
        undoToKeep = Math.min(historyPosition, historyLimit - redoToKeep);
        boolean changed = false;
        while (historyPosition > undoToKeep) {
            history.removeFirst().release();
            historyPosition--;
            changed = true;
        }
        while (history.size() - historyPosition > redoToKeep) {
            history.removeLast().release();
            changed = true;
        }
        return changed;
    }

    private boolean truncateRedo() {
        boolean changed = false;
        while (historyPosition < history.size()) {
            history.removeLast().release();
            changed = true;
        }
        return changed;
    }

    void updateActions() {
        if (!PlatformUI.isWorkbenchRunning()) {
            return;
        }
        UIUtils.asyncExec(() -> {
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_UNDO);
            ResultSetPropertyTester.firePropertyChange(ResultSetPropertyTester.PROP_CAN_REDO);
        });
    }

    @Nullable
    private static ValueSnapshot snapshotValue(@Nullable Object value) {
        if (value instanceof DBDValueCloneable cloneable) {
            try {
                return new ValueSnapshot(cloneable.cloneValue(new VoidProgressMonitor()), true);
            } catch (DBCException e) {
                return null;
            }
        }
        if (value instanceof DBDValue) {
            return null;
        }
        return new ValueSnapshot(value, false);
    }

    @Nullable
    private static Object copyHistoryValue(@Nullable Object value) throws DBException {
        if (value instanceof DBDValueCloneable cloneable) {
            try {
                return cloneable.cloneValue(new VoidProgressMonitor());
            } catch (DBCException e) {
                throw new DBException("Error copying cell value from edit history", e);
            }
        }
        return value;
    }

    private static class CellEditHistoryItem {
        private final ResultSetCellLocation cellLocation;
        private final ResultSetCellLocation valueLocation;
        private final CellSnapshot oldValue;
        private CellSnapshot newValue;

        private CellEditHistoryItem(
            @NotNull ResultSetCellLocation cellLocation,
            @NotNull ResultSetCellLocation valueLocation,
            @NotNull CellSnapshot oldValue
        ) {
            this.cellLocation = cellLocation;
            this.valueLocation = valueLocation;
            this.oldValue = oldValue;
        }

        private void setNewValue(@NotNull CellSnapshot newValue) {
            this.newValue = newValue;
        }

        private void release() {
            oldValue.release();
            if (newValue != null) {
                newValue.release();
            }
        }
    }

    private record CellSnapshot(
        @NotNull ValueSnapshot value,
        @Nullable ValueSnapshot originalValue,
        @NotNull List<DBDAttributeBinding> changedChildren
    ) {
        private void restore(@NotNull ResultSetCellLocation location) throws DBException {
            Object restoredValue = copyHistoryValue(value.value);
            Object restoredOriginal;
            try {
                restoredOriginal = originalValue == null ? null : copyHistoryValue(originalValue.value);
            } catch (DBException e) {
                if (value.owned) {
                    DBUtils.releaseValue(restoredValue);
                }
                throw e;
            }
            ResultSetRow row = location.getRow();
            DBDAttributeBinding root = location.getAttribute();
            int index = root.getOrdinalPosition();
            Object previousValue = row.values[index];
            Object previousOriginal = row.getChange(root);
            row.values[index] = restoredValue;
            row.clearChange(root);
            if (originalValue != null) {
                row.addChange(root, restoredOriginal);
                for (DBDAttributeBinding child : changedChildren) {
                    row.addChange(child, root);
                }
            }
            DBUtils.releaseValue(previousValue);
            if (previousOriginal != previousValue) {
                DBUtils.releaseValue(previousOriginal);
            }
        }

        private void release() {
            value.release();
            if (originalValue != null) {
                originalValue.release();
            }
        }
    }

    private record ValueSnapshot(@Nullable Object value, boolean owned) {
        private void release() {
            if (owned) {
                DBUtils.releaseValue(value);
            }
        }
    }
}
