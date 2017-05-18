/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class RenameHandler extends AbstractDataSourceHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActivePart(event), SQLEditor.class);
        if (editor == null) {
            log.error("No active SQL editor");
            return null;
        }
        Shell shell = HandlerUtil.getActiveShell(event);

        IFile file = EditorUtils.getFileFromInput(editor.getEditorInput());
        if (file == null) {
            UIUtils.showErrorDialog(shell, "Rename", "Can't rename - no source file");
            return null;
        }
        String newName = EnterNameDialog.chooseName(shell, "Rename SQL script [" + file.getName() + "]", file.getName());
        if (newName == null) {
            return null;
        }
        if (newName.indexOf('.') == -1) {
            int divPos = file.getName().lastIndexOf('.');
            if (divPos != -1) {
                newName += file.getName().substring(divPos);
            }
        }
        if (!newName.equals(file.getName())) {

            NullProgressMonitor monitor = new NullProgressMonitor();
            editor.doSave(monitor);
            try {
                file.move(file.getParent().getFullPath().append(newName), true, monitor);
            } catch (CoreException e) {
                UIUtils.showErrorDialog(shell, "Rename", "Error renaming file '" + file.getName() + "'", e);
            }
        }
        //file.set
        return null;
    }


}