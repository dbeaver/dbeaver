/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

public class NavigatorHandlerCreateFolder extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNResource)) {
                return null;
            }
            DBNResource parentResource = (DBNResource) element;
            Shell activeShell = HandlerUtil.getActiveShell(event);
            EnterNameDialog dialog = new EnterNameDialog(activeShell, UINavigatorMessages.actions_navigator_create_folder_folder_name, null) {
                @Override
                protected Composite createDialogArea(Composite parent) {
                    final Composite area = super.createDialogArea(parent);
                    UIUtils.createLabelText(area, "Container", parentResource.getResourcePath(), SWT.BORDER | SWT.READ_ONLY);
                    return area;
                }
            };
            if (dialog.open() == IDialogConstants.OK_ID) {
                String folderName = dialog.getResult();
                if (!CommonUtils.isEmpty(folderName)) {
                    try {
                        parentResource.createNewFolder(folderName);
                    } catch (DBException e) {
                        DBWorkbench.getPlatformUI().showError(
                            UINavigatorMessages.actions_navigator_create_folder_error_title,
                            NLS.bind(UINavigatorMessages.actions_navigator_create_folder_error_message, folderName),
                            e);
                    }
                }
            }
        }
        return null;
    }

}