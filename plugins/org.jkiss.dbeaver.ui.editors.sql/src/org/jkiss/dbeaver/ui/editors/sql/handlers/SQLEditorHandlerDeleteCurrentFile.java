/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;

public class SQLEditorHandlerDeleteCurrentFile extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        IEditorPart editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActivePart(event), IEditorPart.class);
        if (editor == null) {
            log.error("No active SQL editor");
            return null;
        }

        IFile file = EditorUtils.getFileFromInput(editor.getEditorInput());
        if (file == null) {
            File localFile = EditorUtils.getLocalFileFromInput(editor.getEditorInput());
            if (localFile != null) {
                if (UIUtils.confirmAction(SQLEditorMessages.editor_file_delete_confirm_delete_title, NLS.bind(SQLEditorMessages.editor_file_delete_confirm_delete_text, localFile.getName()))) {
                    if (!localFile.delete()) {
                        DBWorkbench.getPlatformUI().showError(SQLEditorMessages.editor_file_delete_error_title, NLS.bind(SQLEditorMessages.editor_file_delete_error_text, localFile.getName()));
                    }
                }
            } else {
                DBWorkbench.getPlatformUI().showError("Rename", "Can't rename - no source file");
            }
        } else {
            if (UIUtils.confirmAction(SQLEditorMessages.editor_file_delete_confirm_delete_title, NLS.bind(SQLEditorMessages.editor_file_delete_confirm_delete_text, file.getName()))) {
                try {
                    file.delete(true, true, new NullProgressMonitor());
                } catch (CoreException e1) {
                    DBWorkbench.getPlatformUI().showError(SQLEditorMessages.editor_file_delete_error_title, NLS.bind(SQLEditorMessages.editor_file_delete_error_text, file.getName(), e1));
                }
            }
        }
        return null;
    }


}