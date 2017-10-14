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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.dialogs.connection.SelectDataSourceDialog;
import org.jkiss.dbeaver.ui.editors.EditorUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NavigatorHandlerAssociateScript extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final Shell activeShell = HandlerUtil.getActiveShell(event);
        List<IFile> scripts = new ArrayList<>();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
            for (Iterator iter = ((IStructuredSelection)selection).iterator(); iter.hasNext(); ) {
                final DBNNode node = RuntimeUtils.getObjectAdapter(iter.next(), DBNNode.class);
                if (node instanceof DBNResource) {
                    IResource resource = ((DBNResource) node).getResource();
                    if (resource instanceof IFile) {
                        scripts.add((IFile) resource);
                    }
                }
            }
        }
        if (!scripts.isEmpty()) {
            SelectDataSourceDialog dialog = new SelectDataSourceDialog(activeShell, scripts.get(0).getProject(), null);
            if (dialog.open() == IDialogConstants.CANCEL_ID) {
                return null;
            }
            DBPDataSourceContainer dataSource = dialog.getDataSource();
            for (IFile script : scripts) {
                EditorUtils.setFileDataSource(script, dataSource, true);
            }
        }
        return null;
    }

}