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
package org.jkiss.dbeaver.ext.tibero.ui.editors;

import org.eclipse.ui.part.MultiPageEditorSite;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.tibero.TiberoConstants;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceObject;
import org.jkiss.dbeaver.ext.tibero.model.source.TiberoSourceType;
import org.jkiss.dbeaver.ext.tibero.ui.actions.CompileHandler;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileError;
import org.jkiss.dbeaver.model.exec.compile.DBCCompileLog;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorDescriptor;
import org.jkiss.dbeaver.ui.editors.sql.SQLSourceViewer;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

final class TiberoSourceEditorUtils {

    private static final String CONTEXT_KEY_POST_SAVE = "org.jkiss.dbeaver.ext.tibero.postSave";

    private TiberoSourceEditorUtils() {
    }

    static void runPostSave(
        SQLSourceViewer<TiberoSourceObject> editor,
        Map<String, Object> context,
        TiberoSourceType... objectTypes
    ) {
        if (context.get(CONTEXT_KEY_POST_SAVE) != null) {
            return;
        }
        context.put(CONTEXT_KEY_POST_SAVE, true);

        TiberoSourceObject unit = editor.getSourceObject();
        if (unit == null || !unit.supportsCompile()) {
            return;
        }
        if (!isCompileAfterSaveEnabled(unit)) {
            refreshSavedState(editor, unit);
            return;
        }

        DBCCompileLog compileLog = editor.getCompileLog();
        compileLog.clearLog();
        Throwable error = null;
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask("Compile " + unit.getName(), 1);
                try {
                    CompileHandler.compileUnit(monitor, compileLog, unit, objectTypes);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
            if (compileLog.getError() != null) {
                error = compileLog.getError();
            }
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            return;
        }

        String typeLabel = unit.getSourceType().name().toLowerCase();
        if (error != null) {
            DBWorkbench.getPlatformUI().showError("Unexpected compilation error", null, error);
        } else if (!CommonUtils.isEmpty(compileLog.getErrorStack())) {
            positionOnFirstError(editor, compileLog);
            editor.setCompileInfo(unit.getName() + " " + typeLabel + " compilation failed", true);
            editor.showCompileLog();
            refreshPropertiesPart(editor, unit);
        } else {
            editor.setCompileInfo(unit.getName() + " " + typeLabel + " compiled successfully", false);
            refreshPropertiesPart(editor, unit);
        }
    }

    private static void refreshSavedState(SQLSourceViewer<TiberoSourceObject> editor, TiberoSourceObject unit) {
        Throwable error = null;
        try {
            UIUtils.runInProgressService(monitor -> {
                monitor.beginTask("Refresh " + unit.getName() + " state", 1);
                try {
                    unit.refreshObjectState(monitor);
                    DBUtils.fireObjectUpdate(unit);
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                } finally {
                    monitor.done();
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            return;
        }

        if (error != null) {
            DBWorkbench.getPlatformUI().showError("Unexpected state refresh error", null, error);
        } else {
            refreshPropertiesPart(editor, unit);
        }
    }

    private static void refreshPropertiesPart(SQLSourceViewer<TiberoSourceObject> editor, TiberoSourceObject unit) {
        if (editor.getSite() instanceof MultiPageEditorSite site
            && site.getMultiPageEditor() instanceof EntityEditor entityEditor) {
            var propertiesEditor = entityEditor.getPageEditor(EntityEditorDescriptor.DEFAULT_OBJECT_EDITOR_ID);
            if (propertiesEditor instanceof IRefreshablePart refreshablePart) {
                UIUtils.syncExec(() -> refreshablePart.refreshPart(unit, true));
            }
        }
    }

    private static boolean isCompileAfterSaveEnabled(TiberoSourceObject unit) {
        String property = unit.getDataSource().getContainer()
            .getConnectionConfiguration()
            .getProviderProperty(TiberoConstants.PROP_COMPILE_AFTER_SAVE);
        return property == null || CommonUtils.toBoolean(property);
    }

    private static void positionOnFirstError(SQLSourceViewer<TiberoSourceObject> editor, DBCCompileLog compileLog) {
        for (DBCCompileError ce : compileLog.getErrorStack()) {
            int line = ce.getLine();
            int position = ce.getPosition();
            if (line > 0 && position >= 0) {
                editor.positionSource(line, position);
                return;
            }
        }
    }
}
