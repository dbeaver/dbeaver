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
package org.jkiss.dbeaver.ui.actions.datasource;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.impl.struct.ContextDefaultObjectsReader;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.DatabaseLazyEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDatabaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class SelectActiveSchemaHandler extends AbstractDataSourceHandler implements IElementUpdater {

    public static final int DEFAULT_SCHEMA_LIST_LIMIT = 100;

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        if (SelectActiveDataSourceHandler.getDataSourceContainerProvider(HandlerUtil.getActiveEditor(event)) == null) {
            return null;
        }

        DBPDataSourceContainer dataSourceContainer = DataSourceToolbarUtils.getCurrentDataSource(HandlerUtil.getActiveWorkbenchWindow(event));
        if (dataSourceContainer == null) {
            log.debug("No active connection. Action is in disabled state.");
            return null;
        }

        DBCExecutionContext executionContext = getExecutionContextFromPart(HandlerUtil.getActiveEditor(event));
        ContextDefaultObjectsReader contextDefaultObjectsReader = new ContextDefaultObjectsReader(dataSourceContainer.getDataSource(), executionContext);
        try {
            UIUtils.runInProgressService(contextDefaultObjectsReader);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Schema list", "Error reading schema list", e.getTargetException());
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        DBSObject selectedDB = null;
        DBSObject defaultObject = contextDefaultObjectsReader.getDefaultObject();
        if (defaultObject != null) {
            for (DBSObject object : contextDefaultObjectsReader.getObjectList()) {
                if (object == defaultObject || object == defaultObject.getParentObject()) {
                    selectedDB = object;
                }
            }
        }
        DBNDatabaseNode selectedNode = selectedDB == null ? null : DBWorkbench.getPlatform().getNavigatorModel().getNodeByObject(selectedDB);
        SelectDatabaseDialog dialog = new SelectDatabaseDialog(
            HandlerUtil.getActiveShell(event),
            dataSourceContainer,
            contextDefaultObjectsReader.getDefaultCatalogName(),
            contextDefaultObjectsReader.getNodeList(),
            selectedNode == null ? null : Collections.singletonList(selectedNode));
        dialog.setModeless(true);
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return null;
        }
        DBNDatabaseNode node = dialog.getSelectedObject();
        if (node != null && node.getObject() != defaultObject) {
            // Change current schema
            changeDataBaseSelection(dataSourceContainer,
                executionContext,
                contextDefaultObjectsReader.getDefaultCatalogName(),
                dialog.getCurrentInstanceName(),
                node.getNodeDisplayName());
        }

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        if ("true".equals(parameters.get("noCustomLabel"))) {
            return;
        }
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        DBPDataSourceContainer dataSource = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);

        String schemaName = "< N/A >";
        DBIcon schemaIcon = DBIcon.TREE_SCHEMA;
        String schemaTooltip = UIUtils.getCatalogSchemaTerms(dataSource, true);

        if (dataSource != null && dataSource.isConnected()) {
            //schemaName = "<no schema>";

            IEditorInput editorInput = activeEditor.getEditorInput();
            if (editorInput instanceof IDatabaseEditorInput) {
                if (editorInput instanceof DatabaseLazyEditorInput) {
                    activeEditor.addPropertyListener(new IPropertyListener() {
                        @Override
                        public void propertyChanged(Object source, int propId) {
                            if (EntityEditor.PROP_TITLE == propId) {
                                DataSourceToolbarUtils.updateCommandsUI();
                                activeEditor.removePropertyListener(this);
                            }
                        }
                    });
                }
                DBCExecutionContext executionContext = ((IDatabaseEditorInput) editorInput).getExecutionContext();
                if (executionContext != null) {
                    DBSObject schemaObject = DBUtils.getSelectedObject(executionContext);
                    if (schemaObject != null && DBUtils.getPublicObjectContainer(schemaObject) != dataSource) {
                        DBSObject schemaParent = schemaObject.getParentObject();
                        if (schemaParent instanceof DBSObjectContainer && !(schemaParent instanceof DBPDataSource)) {
                            schemaName = schemaObject.getName() + "@" + schemaParent.getName();
                        } else {
                            schemaName = schemaObject.getName();
                        }
                    }
                }
            } else {
                DBCExecutionContext executionContext = getExecutionContextFromPart(activeEditor);
                DBCExecutionContextDefaults contextDefaults = null;
                if (executionContext != null) {
                    contextDefaults = executionContext.getContextDefaults();
                }
                if (contextDefaults != null) {
                    DBSCatalog defaultCatalog = contextDefaults.getDefaultCatalog();
                    DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                    if (defaultCatalog != null && (defaultSchema != null || contextDefaults.supportsSchemaChange())) {
                        schemaName = defaultSchema == null ? "?": defaultSchema.getName() + "@" + defaultCatalog.getName();
                        schemaIcon = DBIcon.TREE_SCHEMA;
                    } else if (defaultCatalog != null) {
                        schemaName = defaultCatalog.getName();
                        schemaIcon = DBIcon.TREE_DATABASE;
                    } else if (defaultSchema != null) {
                        schemaName = defaultSchema.getName();
                        schemaIcon = DBIcon.TREE_SCHEMA;
                    }
                }
            }
        }
        element.setText(schemaName);
        element.setIcon(DBeaverIcons.getImageDescriptor(schemaIcon));
        element.setTooltip(schemaTooltip);
    }

    private static void changeDataBaseSelection(DBPDataSourceContainer dsContainer, DBCExecutionContext executionContext, @Nullable String curInstanceName, @Nullable String newInstanceName, @Nullable String newObjectName) {
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            new AbstractJob("Change active database") {
                {
                    setUser(true);
                }
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        DBExecUtils.setExecutionContextDefaults(monitor, dataSource, executionContext, newInstanceName, curInstanceName, newObjectName);
                        return Status.OK_STATUS;
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                }
            }.schedule();
        }
    }

    public static class MenuContributor extends DataSourceMenuContributor {

        static final int DB_LIST_READ_TIMEOUT = 3000;

        @Override
        protected void fillContributionItems(List<IContributionItem> menuItems) {
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            if (workbenchWindow.getActivePage() == null ||
                SelectActiveDataSourceHandler.getDataSourceContainerProvider(workbenchWindow.getActivePage().getActiveEditor()) == null)
            {
                return;
            }
            DBPDataSourceContainer dataSourceContainer = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);
            if (dataSourceContainer == null) {
                return;
            }

            DBCExecutionContext executionContext = getExecutionContextFromPart(workbenchWindow.getActivePage().getActiveEditor());
            ContextDefaultObjectsReader contextDefaultObjectsReader = new ContextDefaultObjectsReader(dataSourceContainer.getDataSource(), executionContext);
            contextDefaultObjectsReader.setReadNodes(true);
            RuntimeUtils.runTask(contextDefaultObjectsReader, "Read database list", DB_LIST_READ_TIMEOUT);

            DBSObject[] defObjects = null;
            if (executionContext != null) {
                DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    defObjects = new DBSObject[] { contextDefaults.getDefaultCatalog(), contextDefaults.getDefaultSchema() };
                }
            }
            if (defObjects == null) defObjects = new DBSObject[0];
            DBSObject[] finalDefObjects = defObjects;
            int nodesLimit = Math.min(
                DEFAULT_SCHEMA_LIST_LIMIT,
                DBWorkbench.getPlatform().getPreferenceStore().getInt(NavigatorPreferences.NAVIGATOR_LONG_LIST_FETCH_SIZE));

            for (DBNDatabaseNode node : contextDefaultObjectsReader.getNodeList()) {
                menuItems.add(
                    new ActionContributionItem(new Action(node.getName(), Action.AS_CHECK_BOX) {
                        private final DBSObject object = node.getObject();
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(node.getNodeIcon()));
                        }
                        @Override
                        public boolean isChecked() {
                            return ArrayUtils.contains(finalDefObjects, object);
                        }
                        @Override
                        public void run() {
                            changeDataBaseSelection(
                                dataSourceContainer,
                                executionContext,
                                contextDefaultObjectsReader.getDefaultCatalogName(),
                                (object instanceof DBSCatalog ? object.getName() : contextDefaultObjectsReader.getDefaultCatalogName()),
                                (object instanceof DBSSchema ? node.getNodeDisplayName() : null));
                        }
                    }
                ));
                if (menuItems.size() >= nodesLimit) {
                    break;
                }
            }
        }
    }
}