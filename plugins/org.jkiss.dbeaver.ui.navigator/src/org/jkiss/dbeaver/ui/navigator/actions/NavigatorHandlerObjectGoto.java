/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.struct.DBSFolder;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSWrapper;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.navigator.dialogs.GotoObjectDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.Arrays;
import java.util.Collection;

public class NavigatorHandlerObjectGoto extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBCExecutionContext context = null;
        DBSObject container = null;
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        if (activePart instanceof DBPContextProvider) {
            context = ((DBPContextProvider) activePart).getExecutionContext();
        } else if (GeneralUtils.adapt(activePart, INavigatorModelView.class) != null) {
            final ISelection selection = HandlerUtil.getCurrentSelection(event);
            if (selection instanceof IStructuredSelection) {
                Object element = ((IStructuredSelection) selection).getFirstElement();
                if (element instanceof DBSWrapper) {
                    DBSObject object = ((DBSWrapper) element).getObject();
                    if (object != null) {
                        container = object;
                        while (container instanceof DBSFolder) {
                            container = container.getParentObject();
                        }
                        
                        context = DBUtils.getDefaultContext(object, true);
                    }
                }
            }
        }
        if (context == null) {
            DBWorkbench.getPlatformUI().showError(
                "Go to object",
                "You must select a connected datasource");
            return null;
        }
        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        GotoObjectDialog dialog = new GotoObjectDialog(HandlerUtil.getActiveShell(event), context, container);
        dialog.open();
        Object[] objectsToOpen = dialog.getResult();
        if (!ArrayUtils.isEmpty(objectsToOpen)) {
            Collection<DBNDatabaseNode> nodes = NavigatorHandlerObjectBase.getNodesByObjects(Arrays.asList(objectsToOpen));
            for (DBNDatabaseNode node : nodes) {
                NavigatorUtils.openNavigatorNode(node, workbenchWindow);
            }
        }

        return null;
    }


}
