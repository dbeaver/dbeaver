/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.model.struct.rdb.DBSCatalog;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.DatabaseLazyEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.dialogs.SelectDatabaseDialog;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.*;

public class SelectActiveSchemaHandler extends AbstractDataSourceHandler implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        DBPDataSourceContainer dataSourceContainer = DataSourceToolbarUtils.getCurrentDataSource(HandlerUtil.getActiveWorkbenchWindow(event));
        if (dataSourceContainer == null) {
            log.debug("No active connection. Action is in disabled state.");
            return null;
        }

        DBCExecutionContext executionContext = getExecutionContext(HandlerUtil.getActiveEditor(event));
        DatabaseListReader databaseListReader = new DatabaseListReader(dataSourceContainer.getDataSource(), executionContext);
        try {
            UIUtils.runInProgressService(databaseListReader);
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError("Schema list", "Error reading schema list", e.getTargetException());
            return null;
        } catch (InterruptedException e) {
            return null;
        }

        DBNDatabaseNode selectedDB = null;
        for (DBNDatabaseNode node : databaseListReader.nodeList) {
            if (node.getObject() == databaseListReader.active) {
                selectedDB = node;
            }
        }
        SelectDatabaseDialog dialog = new SelectDatabaseDialog(
            HandlerUtil.getActiveShell(event),
            dataSourceContainer,
            databaseListReader.currentDatabaseInstanceName,
            databaseListReader.nodeList,
            selectedDB == null ? null : Collections.singletonList(selectedDB));
        dialog.setModeless(true);
        if (dialog.open() == IDialogConstants.CANCEL_ID) {
            return null;
        }
        DBNDatabaseNode node = dialog.getSelectedObject();
        if (node != null && node != databaseListReader.active) {
            // Change current schema
            changeDataBaseSelection(dataSourceContainer, executionContext, databaseListReader.currentDatabaseInstanceName, dialog.getCurrentInstanceName(), node.getNodeName());
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

        String schemaName = "< N/A >";
        DBIcon schemaIcon = DBIcon.TREE_SCHEMA;
        String schemaTooltip = UINavigatorMessages.toolbar_datasource_selector_combo_database_tooltip;

        DBPDataSourceContainer dataSource = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);
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
                DBSObjectContainer schemaObject = null;
                DBSObject curObject = ((IDatabaseEditorInput) editorInput).getDatabaseObject();
                for (DBSObject parent = curObject; parent != null; parent = parent.getParentObject()) {
                    if (parent instanceof DBSObjectContainer) {
                        schemaObject = (DBSObjectContainer) parent;
                        if (parent.getParentObject() instanceof DBSObjectSelector) {
                            break;
                        }
                    }
                }
                if (schemaObject != null && DBUtils.getPublicObjectContainer(schemaObject) != dataSource) {
                    DBSObject schemaParent = schemaObject.getParentObject();
                    if (schemaParent instanceof DBSObjectContainer && !(schemaParent instanceof DBPDataSource)) {
                        schemaName = schemaObject.getName() + "@" + schemaParent.getName();
                    } else {
                        schemaName = schemaObject.getName();
                    }
                }
            } else {
                DBCExecutionContext executionContext = getExecutionContext(activeEditor);
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
                } else {
                    DBSObject[] defObjects = getSelectedSchema(dataSource);
                    if (defObjects.length > 0) {
                        schemaIcon = DBIcon.TREE_SCHEMA;
                        if (defObjects.length == 1) {
                            schemaName = defObjects[0].getName();
                        } else {
                            schemaName = defObjects[1].getName() + "@" + defObjects[0].getName();
                        }
                    }
                }
            }
        }
        element.setText(schemaName);
        element.setIcon(DBeaverIcons.getImageDescriptor(schemaIcon));
        element.setTooltip(schemaTooltip);
    }

    @Deprecated
    private static DBSObject[] getSelectedSchema(DBPDataSourceContainer dataSource) {

        DBSObject firstContainer = null, secondContainer = null;
        DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
        if (objectSelector != null) {
            firstContainer = objectSelector.getDefaultObject();

            if (firstContainer instanceof DBSObjectContainer) {
                // Default object can be object container + object selector (e.g. in PG)
                DBSObjectSelector objectSelector2 = DBUtils.getAdapter(DBSObjectSelector.class, firstContainer);
                if (objectSelector2 != null && objectSelector2.supportsDefaultChange()) {
                    //objectContainer = (DBSObjectContainer) defObject;
                    secondContainer = objectSelector2.getDefaultObject();
                }
            }
        }
        if (firstContainer == null && secondContainer == null) {
            return new DBSObject[0];
        } else if (secondContainer == null) {
            return new DBSObject[] { firstContainer };
        } else {
            return new DBSObject[]{firstContainer, secondContainer};
        }
    }

    private static void changeDataBaseSelection(DBPDataSourceContainer dsContainer, DBCExecutionContext executionContext, @Nullable String curInstanceName, @Nullable String newInstanceName, @Nullable String newSchemaName) {
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            new AbstractJob("Change active database") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        DBSObjectContainer rootContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
                        if (rootContainer == null) {
                            return Status.OK_STATUS;
                        }

                        DBCExecutionContextDefaults contextDefaults = null;
                        if (executionContext != null) {
                            contextDefaults = executionContext.getContextDefaults();
                        }
                        if (contextDefaults != null && (contextDefaults.supportsSchemaChange() || contextDefaults.supportsCatalogChange())) {
                            changeDefaultObject(monitor, rootContainer, contextDefaults, newInstanceName, curInstanceName, newSchemaName);
                        } else {
                            changeDefaultObjectLegacy(monitor, dataSource, rootContainer, curInstanceName, newInstanceName, newSchemaName);
                        }
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    @SuppressWarnings("unchecked")
    private static void changeDefaultObject(DBRProgressMonitor monitor, DBSObjectContainer rootContainer, DBCExecutionContextDefaults contextDefaults, @Nullable String newInstanceName, @Nullable String curInstanceName, @Nullable String newSchemaName) throws DBException {
        DBSCatalog newCatalog = null;
        DBSSchema newSchema = null;

        if (newInstanceName != null) {
            DBSObject newInstance = rootContainer.getChild(monitor, newInstanceName);
            if (newInstance instanceof DBSCatalog) {
                newCatalog = (DBSCatalog) newInstance;
            }
        }
        DBSObject schemaObject = null;
        if (newSchemaName != null) {
            if (newCatalog == null) {
                schemaObject = rootContainer.getChild(monitor, newSchemaName);
            } else {
                schemaObject = newCatalog.getChild(monitor, newSchemaName);
            }
            if (schemaObject instanceof DBSSchema) {
                newSchema = (DBSSchema) schemaObject;
            }
        }

        boolean changeCatalog = !CommonUtils.equalObjects(curInstanceName, newInstanceName);

        if (newCatalog != null && newSchema != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, newSchema);
        } else if (newSchema != null) {
            contextDefaults.setDefaultSchema(monitor, newSchema);
        } else if (newCatalog != null && changeCatalog) {
            contextDefaults.setDefaultCatalog(monitor, newCatalog, null);
        }
    }

    @SuppressWarnings("deprecated")
    private static void changeDefaultObjectLegacy(DBRProgressMonitor monitor, DBPDataSource dataSource, DBSObjectContainer rootContainer, @Nullable String curInstanceName, @Nullable String newInstanceName, @Nullable String newSchemaName) throws DBException {
        DBSObjectSelector os = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
        if (os != null) {
            if (newInstanceName != null && !CommonUtils.equalObjects(curInstanceName, newInstanceName)) {
                // Change current instance
                DBSObject newInstance = rootContainer.getChild(monitor, newInstanceName);
                if (newInstance != null) {
                    os.setDefaultObject(monitor, newInstance);
                }
            }
            final DBSObject defObject = os.getDefaultObject();
            if (defObject instanceof DBSObjectContainer) {
                // USe seconds level of active object
                DBSObjectSelector os2 = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                if (os2 != null && os2.supportsDefaultChange()) {
                    rootContainer = (DBSObjectContainer) defObject;
                    os = os2;
                }
            }
        }

        if (newSchemaName != null) {
            if (rootContainer != null && os != null && os.supportsDefaultChange()) {
                DBSObject newChild = rootContainer.getChild(monitor, newSchemaName);
                if (newChild != null) {
                    os.setDefaultObject(monitor, newChild);
                } else {
                    throw new DBException(MessageFormat.format(UINavigatorMessages.toolbar_datasource_selector_error_database_not_found, newSchemaName));
                }
            } else {
                throw new DBException(UINavigatorMessages.toolbar_datasource_selector_error_database_change_not_supported);
            }
        }
    }

    private static class DatabaseListReader implements DBRRunnableWithProgress {
        private final DBPDataSource dataSource;
        private final DBCExecutionContext executionContext;
        private final List<DBNDatabaseNode> nodeList = new ArrayList<>();
        // Remote instance node
        private DBSObject active;
        private boolean enabled;
        private String currentDatabaseInstanceName;

        DatabaseListReader(DBPDataSource dataSource, DBCExecutionContext executionContext) {
            this.dataSource = dataSource;
            this.executionContext = executionContext;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();

            DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            if (objectContainer == null) {
                return;
            }

            DBCExecutionContextDefaults contextDefaults = null;
            if (executionContext != null) {
                contextDefaults = executionContext.getContextDefaults();
            }

            DBSObjectSelector objectSelector = null;
            if (contextDefaults == null) {
                objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
            }
            if (contextDefaults == null && objectSelector == null) {
                return;
            }
            try {
                monitor.beginTask(UINavigatorMessages.toolbar_datasource_selector_action_read_databases, 1);
                currentDatabaseInstanceName = null;

                Class<? extends DBSObject> childType = objectContainer.getChildType(monitor);
                if (childType == null || !DBSObjectContainer.class.isAssignableFrom(childType)) {
                    enabled = false;
                } else {
                    enabled = true;

                    DBSObject defObject = null;
                    if (contextDefaults != null) {
                        if (DBSCatalog.class.isAssignableFrom(childType)) {
                            defObject = contextDefaults.getDefaultCatalog();
                        }
                    } else {
                        defObject = objectSelector.getDefaultObject();
                    }
                    if (defObject instanceof DBSObjectContainer) {
                        if (contextDefaults != null) {
                            currentDatabaseInstanceName = defObject.getName();
                            if (contextDefaults.supportsSchemaChange()) {
                                objectContainer = (DBSObjectContainer) defObject;
                            } else if (!contextDefaults.supportsCatalogChange()) {
                                // Nothing can be changed
                                objectContainer = null;
                            }
                            DBSSchema defaultSchema = contextDefaults.getDefaultSchema();
                            if (defaultSchema != null) {
                                defObject = defaultSchema;
                            }
                        } else {
                            // Default object can be object container + object selector (e.g. in PG)
                            objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                            if (objectSelector != null && objectSelector.supportsDefaultChange()) {
                                currentDatabaseInstanceName = defObject.getName();
                                objectContainer = (DBSObjectContainer) defObject;
                                defObject = objectSelector.getDefaultObject();
                            }
                        }
                    }
                    Collection<? extends DBSObject> children = objectContainer == null ?
                        (defObject == null ? Collections.emptyList() : Collections.singletonList(defObject)) :
                        objectContainer.getChildren(monitor);
                    active = defObject;
                    // Cache navigator nodes
                    if (children != null) {
                        for (DBSObject child : children) {
                            if (DBUtils.getAdapter(DBSObjectContainer.class, child) != null) {
                                DBNDatabaseNode node = navigatorModel.getNodeByObject(monitor, child, false);
                                if (node != null) {
                                    nodeList.add(node);
                                }
                            }
                        }
                    }
                }
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            } finally {
                monitor.done();
            }
        }
    }

    public static class MenuContributor extends DataSourceMenuContributor {

        static final int DB_LIST_READ_TIMEOUT = 3000;

        @Override
        protected void fillContributionItems(List<IContributionItem> menuItems) {
            IWorkbenchWindow workbenchWindow = UIUtils.getActiveWorkbenchWindow();
            DBPDataSourceContainer dataSourceContainer = DataSourceToolbarUtils.getCurrentDataSource(workbenchWindow);
            if (dataSourceContainer == null) {
                return;
            }

            DBCExecutionContext executionContext = getExecutionContext(workbenchWindow.getActivePage().getActiveEditor());
            DatabaseListReader databaseListReader = new DatabaseListReader(dataSourceContainer.getDataSource(), executionContext);
            RuntimeUtils.runTask(databaseListReader, "Read database list", DB_LIST_READ_TIMEOUT);

            DBSObject[] defObjects = null;
            if (executionContext != null) {
                DBCExecutionContextDefaults contextDefaults = executionContext.getContextDefaults();
                if (contextDefaults != null) {
                    defObjects = new DBSObject[] { contextDefaults.getDefaultCatalog(), contextDefaults.getDefaultSchema() };
                }
            }
            if (defObjects == null) {
                defObjects = getSelectedSchema(dataSourceContainer);
            }
            DBSObject[] finalDefObjects = defObjects;
            for (DBNDatabaseNode node : databaseListReader.nodeList) {
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
                                databaseListReader.currentDatabaseInstanceName,
                                (object instanceof DBSCatalog ? object.getName() : databaseListReader.currentDatabaseInstanceName),
                                (object instanceof DBSSchema ? node.getNodeName() : null));
                        }
                    }
                ));
            }
        }
    }
}