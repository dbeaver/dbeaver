/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import net.sf.jkiss.utils.CommonUtils;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;

public class NavigatorHandlerCreateFolder extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (!(element instanceof DBNResource)) {
                return null;
            }
            Shell activeShell = HandlerUtil.getActiveShell(event);
            String folderName = EnterNameDialog.chooseName(activeShell, "Folder name");
            if (!CommonUtils.isEmpty(folderName)) {
                try {
                    ((DBNResource)element).createNewFolder(folderName);
                } catch (DBException e) {
                    UIUtils.showErrorDialog(activeShell, "Create folder", "Can't create new folder '" + folderName + "'", e);
                }
            }
        }
        return null;
    }

}