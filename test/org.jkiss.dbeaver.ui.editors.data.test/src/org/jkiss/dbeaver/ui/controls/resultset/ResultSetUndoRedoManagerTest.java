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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDComposite;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.DBDValueCloneable;
import org.jkiss.dbeaver.model.data.storage.StringContentStorage;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.data.JDBCContentChars;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.rdb.DBSManipulationType;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.data.managers.ContentValueManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

public class ResultSetUndoRedoManagerTest extends DBeaverUnitTest {
    private ResultSetViewer viewer;
    private ResultSetModel model;
    private ResultSetUndoRedoManager history;
    private DBPPreferenceStore preferences;
    private DBDAttributeBinding root;
    private ResultSetRow row;

    @BeforeEach
    public void setUp() {
        // Exercise real model/history state without creating spreadsheet controls.
        model = new ResultSetModel();
        viewer = mock(ResultSetViewer.class);
        preferences = mock(DBPPreferenceStore.class);
        when(preferences.getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL)).thenReturn(200);
        when(viewer.getModel()).thenReturn(model);
        when(viewer.getPreferenceStore()).thenReturn(preferences);
        when(viewer.getActivePresentation()).thenReturn(mock(IResultSetPresentation.class));
        history = new ResultSetUndoRedoManager(viewer);
        root = mock(DBDAttributeBinding.class);
        when(root.getTopParent()).thenReturn(root);
        when(root.getDataKind()).thenReturn(DBPDataKind.STRING);
        row = new ResultSetRow(0, new Object[]{"original"});
        model.getAllRows().add(row);
    }

    @AfterEach
    public void tearDown() {
        if (history != null) {
            history.clear();
        }
        if (model != null) {
            model.releaseAllData();
        }
    }

    @Test
    public void undoRedoRestoresValuesAndDirtyState() throws DBException {
        edit(root, "first");
        edit(root, "second");
        history.undo();
        assertEquals("first", row.values[0]);
        assertTrue(model.isDirty());
        history.undo();
        assertEquals("original", row.values[0]);
        assertFalse(model.isDirty());
        history.redo();
        assertEquals("first", row.values[0]);
        assertEquals("original", row.getChange(root));
        history.redo();
        assertEquals("second", row.values[0]);
    }

    @Test
    public void redoRejectsDetachedRowEvenIfItsNumberWasReused() throws DBException {
        edit(root, "edited");
        history.undo();
        model.clearData();
        ResultSetRow replacement = new ResultSetRow(0, new Object[]{"reloaded"});
        model.getAllRows().add(replacement);
        assertEquals(row, replacement);
        history.redo();
        assertEquals("reloaded", replacement.values[0]);
        assertFalse(model.isDirty());
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void replayIsBlockedDuringSaveAndRefresh() throws DBException {
        edit(root, "edited");
        model.setUpdateInProgress(mock(DataSourceJob.class));
        assertFalse(history.canUndo());
        history.undo();
        assertEquals("edited", row.values[0]);
        model.setUpdateInProgress(null);
        history.undo();
        when(viewer.isRefreshInProgress()).thenReturn(true);
        assertFalse(history.canRedo());
        history.redo();
        assertEquals("original", row.values[0]);
        when(viewer.isRefreshInProgress()).thenReturn(false);
        history.redo();
        assertEquals("edited", row.values[0]);
    }

    @Test
    public void undoNestedResetRestoresChildMarkersForEarlierUndo() throws DBException {
        DBDAttributeBinding child = child();
        row.values[0] = composite(Map.of(child, "original"));
        edit(child, "edited");
        assertTrue(history.resetCellValue(child, row, null));
        history.undo();
        assertEquals("edited", nestedValue(child));
        assertSame(root, row.getChange(child));
        history.undo();
        assertEquals("original", nestedValue(child));
        assertFalse(model.isDirty());
        history.redo();
        history.redo();
        assertEquals("original", nestedValue(child));
        assertFalse(model.isDirty());
    }

    @Test
    public void undoSiblingEditPreservesOtherChildAndOriginalRoot() throws DBException {
        DBDAttributeBinding first = child();
        DBDAttributeBinding second = child();
        row.values[0] = composite(Map.of(first, "a", second, "b"));
        edit(first, "changed a");
        edit(second, "changed b");
        assertEquals("a", ((DBDComposite) row.getChange(root)).getAttributeValue(first));
        history.undo();
        assertEquals("changed a", nestedValue(first));
        assertEquals("b", nestedValue(second));
        assertTrue(row.isChanged(first));
        assertFalse(row.isChanged(second));
        history.undo();
        assertEquals("a", nestedValue(first));
        assertFalse(model.isDirty());
    }

    @Test
    public void successiveContentEditsHaveIndependentSnapshots() throws DBException {
        row.values[0] = new JDBCContentChars(mock(DBCExecutionContext.class), "original");
        editContent("first");
        editContent("second");
        history.undo();
        assertEquals("first", ((DBDContent) row.values[0]).getRawValue());
        history.undo();
        assertEquals("original", ((DBDContent) row.values[0]).getRawValue());
        assertFalse(model.isDirty());
        history.redo();
        history.redo();
        assertEquals("second", ((DBDContent) row.values[0]).getRawValue());
    }

    @Test
    public void trimmingHistoryDoesNotChangeThePersistedBaseline() throws DBException {
        when(preferences.getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL)).thenReturn(1);
        edit(root, "first");
        edit(root, "second");
        history.undo();
        assertEquals("first", row.values[0]);
        assertEquals("original", row.getChange(root));
        assertFalse(history.canUndo());
        assertTrue(model.isDirty());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void partialSaveInvalidatesHistoryForThePreviousBaseline() throws Exception {
        edit(root, "saved");
        ResultSetRow failedRow = new ResultSetRow(1, new Object[]{"original"});
        model.getAllRows().add(failedRow);
        history.updateCellValue(root, failedRow, null, "failed");
        var historyField = ResultSetViewer.class.getDeclaredField("undoRedoManager");
        historyField.setAccessible(true);
        historyField.set(viewer, history);
        ResultSetPersister persister = new ResultSetPersister(viewer);
        ResultSetPersister.DataStatementInfo succeeded = new ResultSetPersister.DataStatementInfo(
            DBSManipulationType.UPDATE, row, mock(DBSEntity.class));
        succeeded.executed = true;
        ResultSetPersister.DataStatementInfo failed = new ResultSetPersister.DataStatementInfo(
            DBSManipulationType.UPDATE, failedRow, mock(DBSEntity.class));
        // Inject execution results at the database/UI boundary, without requiring a live database.
        var statementsField = ResultSetPersister.class.getDeclaredField("updateStatements");
        statementsField.setAccessible(true);
        var statements = (List<ResultSetPersister.DataStatementInfo>) statementsField.get(persister);
        statements.add(succeeded);
        statements.add(failed);
        var reflectChanges = ResultSetPersister.class.getDeclaredMethod("reflectChanges");
        reflectChanges.setAccessible(true);
        reflectChanges.invoke(persister);
        assertFalse(row.isChanged());
        assertTrue(failedRow.isChanged());
        assertFalse(history.canUndo());
        assertFalse(history.canRedo());
    }

    @Test
    public void disabledHistoryDoesNotCloneContent() throws DBException {
        when(preferences.getInt(ResultSetPreferences.RS_EDIT_UNDO_LEVEL)).thenReturn(0);
        DBDComposite value = composite(Map.of());
        row.setState(ResultSetRow.STATE_ADDED);
        row.values[0] = value;
        edit(root, "edited");
        verify((DBDValueCloneable) value, never()).cloneValue(any());
        assertFalse(history.canUndo());
    }

    private void edit(@NotNull DBDAttributeBinding attribute, @NotNull Object value) throws DBException {
        assertTrue(history.updateCellValue(attribute, row, null, value));
    }

    private void editContent(@NotNull String text) throws DBException {
        DBDContent original = (DBDContent) row.values[0];
        Object oldText = original.getRawValue();
        DBDContent edited = ContentValueManager.copyContentForEdit(new VoidProgressMonitor(), original);
        edited.updateContents(new VoidProgressMonitor(), new StringContentStorage(text));
        assertEquals(oldText, original.getRawValue());
        edit(root, edited);
    }

    @NotNull
    private DBDAttributeBinding child() throws DBException {
        when(root.getDataKind()).thenReturn(DBPDataKind.STRUCT);
        DBDAttributeBinding child = mock(DBDAttributeBinding.class);
        when(child.getLevel()).thenReturn(1);
        when(child.getTopParent()).thenReturn(root);
        when(child.getParent(0)).thenReturn(child);
        when(child.getDataKind()).thenReturn(DBPDataKind.STRING);
        when(child.extractNestedValue(any(), anyInt())).thenAnswer(
            invocation -> ((DBDComposite) invocation.getArgument(0)).getAttributeValue(child));
        return child;
    }

    private Object nestedValue(@NotNull DBDAttributeBinding attribute) throws DBException {
        return ((DBDComposite) row.values[0]).getAttributeValue(attribute);
    }

    @NotNull
    private static DBDComposite composite(@NotNull Map<DBSAttributeBase, Object> initialValues) throws DBException {
        Map<DBSAttributeBase, Object> values = new HashMap<>(initialValues);
        DBDComposite composite = mock(DBDComposite.class, withSettings().extraInterfaces(DBDValueCloneable.class));
        when(((DBDValueCloneable) composite).cloneValue(any())).thenAnswer(invocation -> composite(values));
        when(composite.getAttributeValue(any())).thenAnswer(invocation -> values.get(invocation.getArgument(0)));
        doAnswer(invocation -> {
            values.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(composite).setAttributeValue(any(), any());
        return composite;
    }
}
