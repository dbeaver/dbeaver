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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseItem;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.TasksJob;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class NavigatorHandlerSetDefaultObject extends NavigatorHandlerObjectBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        final IEditorPart activeEditor = HandlerUtil.getActiveEditor(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection) selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNDatabaseNode) {
                markObjectAsActive((DBNDatabaseNode) element, activeEditor);
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void markObjectAsActive(final DBNDatabaseNode databaseNode, IEditorPart activeEditor) {
        DBNNode parentNode = databaseNode.getParentNode();

        if (parentNode instanceof DBNDatabaseItem) {
            markObjectAsActive((DBNDatabaseItem) parentNode, activeEditor);
            return;
        }

        DBSObject object = databaseNode.getObject();
        DBPDataSource dataSource = object.getDataSource();
        final DBCExecutionContext editorContext;
        if (activeEditor instanceof DBPContextProvider) {
            editorContext = ((DBPContextProvider) activeEditor).getExecutionContext();
        } else {
            editorContext = null;
        }

        TasksJob.runTask("Change default object", monitor -> {
            try {
                DBExecUtils.tryExecuteRecover(monitor, dataSource, param -> {
                    try {
                        DBCExecutionContext defaultContext = dataSource.getDefaultInstance().getDefaultContext(monitor, false);

                        DBCExecutionContext[] contextsToChange;
                        if (editorContext != null && editorContext != defaultContext && editorContext.getDataSource() == defaultContext.getDataSource()) {
                            contextsToChange = new DBCExecutionContext[] { defaultContext, editorContext };
                        } else {
                            contextsToChange = new DBCExecutionContext[] { defaultContext };
                        }

                        for (DBCExecutionContext executionContext : contextsToChange) {
                            DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                            if (contextDefaults != null) {
                                if (object instanceof DBSCatalog && contextDefaults.supportsCatalogChange()) {
                                    contextDefaults.setDefaultCatalog(monitor, (DBSCatalog) object, null);
                                } else if (object instanceof DBSSchema && contextDefaults.supportsSchemaChange()) {
                                    contextDefaults.setDefaultSchema(monitor, (DBSSchema) object);
                                } else {
                                    throw new DBCException("Internal error: active object change not supported");
                                }
                            }
                        }
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
            } catch (Exception e) {
                throw new InvocationTargetException(e);
            }
        });

    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        //element.setText("Set as default" + NavigatorHandlerObjectCreateNew.getObjectTypeName(element));
    }

}
