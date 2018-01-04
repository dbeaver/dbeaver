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

package org.jkiss.dbeaver.ui.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public abstract class AbstractDataSourceHandler extends AbstractHandler {

    static protected final Log log = Log.getLog(AbstractDataSourceHandler.class);

    protected DBCExecutionContext getExecutionContext(ExecutionEvent event, boolean useEditor)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor instanceof DBPContextProvider) {
                return ((DBPContextProvider) editor).getExecutionContext();
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);

        if (activePart instanceof DBPContextProvider) {
            return ((DBPContextProvider) activePart).getExecutionContext();
        }

        ISelection selection = HandlerUtil.getCurrentSelection(event);
        DBSObject selectedObject = NavigatorUtils.getSelectedObject(selection);
        if (selectedObject != null) {
            DBPDataSource dataSource = selectedObject.getDataSource();
            if (dataSource != null) {
                return dataSource.getDefaultContext(false);
            }
        }

        return null;
    }

    protected static DBPDataSourceContainer getDataSourceContainer(ExecutionEvent event, boolean useEditor)
    {
        if (useEditor) {
            IEditorPart editor = HandlerUtil.getActiveEditor(event);
            if (editor != null) {
                DBPDataSourceContainer container = getDataSourceContainer(editor);
                if (container != null) {
                    return container;
                }
            }
            return null;
        }
        IWorkbenchPart activePart = HandlerUtil.getActivePart(event);
        DBPDataSourceContainer container = getDataSourceContainer(activePart);
        if (container != null) {
            return container;
        }
        ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBSObject selectedObject = NavigatorUtils.getSelectedObject(selection);
        if (selectedObject instanceof DBPDataSourceContainer) {
            return (DBPDataSourceContainer)selectedObject;
        } else if (selectedObject != null) {
            DBPDataSource dataSource = selectedObject.getDataSource();
            return dataSource == null ? null : dataSource.getContainer();
        }

        return null;
    }

    public static DBCExecutionContext getExecutionContext(IWorkbenchPart activePart)
    {
        if (activePart instanceof DBPContextProvider) {
            return ((DBPContextProvider) activePart).getExecutionContext();
        }
        return null;
    }

    public static DBPDataSourceContainer getDataSourceContainer(IWorkbenchPart activePart)
    {
        if (activePart instanceof IDataSourceContainerProvider) {
            return ((IDataSourceContainerProvider) activePart).getDataSourceContainer();
        }
        if (activePart instanceof DBPContextProvider) {
            DBCExecutionContext context = ((DBPContextProvider) activePart).getExecutionContext();
            return context == null ? null : context.getDataSource().getContainer();
        }
        return null;
    }
}