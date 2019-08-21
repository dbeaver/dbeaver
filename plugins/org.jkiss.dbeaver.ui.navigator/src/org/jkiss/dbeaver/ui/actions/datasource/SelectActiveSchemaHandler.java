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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
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

        DatabaseListReader databaseListReader = new DatabaseListReader(dataSourceContainer.getDataSource());
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
            changeDataBaseSelection(dataSourceContainer, databaseListReader.currentDatabaseInstanceName, dialog.getCurrentInstanceName(), node.getNodeName());
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
        element.setText(schemaName);
        element.setIcon(DBeaverIcons.getImageDescriptor(schemaIcon));
        element.setTooltip(schemaTooltip);
    }

    public static DBSObject[] getSelectedSchema(DBPDataSourceContainer dataSource) {

        DBSObject firstContainer = null, secondContainer = null;
        DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
        if (objectSelector != null && objectSelector.supportsDefaultChange()) {
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

    private static void changeDataBaseSelection(DBPDataSourceContainer dsContainer, @Nullable String curInstanceName, @Nullable String newInstanceName, @NotNull String newSchemaName) {
        if (dsContainer != null && dsContainer.isConnected()) {
            final DBPDataSource dataSource = dsContainer.getDataSource();
            new AbstractJob("Change active database") {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        DBSObjectContainer oc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
                        DBSObjectSelector os = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
                        if (os != null) {
                            if (newInstanceName != null && !CommonUtils.equalObjects(curInstanceName, newInstanceName)) {
                                // Change current instance
                                DBSObject newInstance = oc.getChild(monitor, newInstanceName);
                                if (newInstance != null) {
                                    os.setDefaultObject(monitor, newInstance);
                                }
                            }
                            final DBSObject defObject = os.getDefaultObject();
                            if (defObject instanceof DBSObjectContainer) {
                                // USe seconds level of active object
                                DBSObjectSelector os2 = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                                if (os2 != null && os2.supportsDefaultChange()) {
                                    oc = (DBSObjectContainer) defObject;
                                    os = os2;
                                }
                            }
                        }

                        if (oc != null && os != null && os.supportsDefaultChange()) {
                            DBSObject newChild = oc.getChild(monitor, newSchemaName);
                            if (newChild != null) {
                                os.setDefaultObject(monitor, newChild);
                            } else {
                                throw new DBException(MessageFormat.format(UINavigatorMessages.toolbar_datasource_selector_error_database_not_found, newSchemaName));
                            }
                        } else {
                            throw new DBException(UINavigatorMessages.toolbar_datasource_selector_error_database_change_not_supported);
                        }
                    } catch (DBException e) {
                        return GeneralUtils.makeExceptionStatus(e);
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

    private static class DatabaseListReader implements DBRRunnableWithProgress {
        private final DBPDataSource dataSource;
        private final List<DBNDatabaseNode> nodeList = new ArrayList<>();
        // Remote instance node
        private DBSObject active;
        private boolean enabled;
        private String currentDatabaseInstanceName;

        DatabaseListReader(DBPDataSource dataSource) {
            this.dataSource = dataSource;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
            DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, dataSource);
            if (objectContainer == null || objectSelector == null) {
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

                    DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();

                    DBSObject defObject = objectSelector.getDefaultObject();
                    if (defObject instanceof DBSObjectContainer) {
                        // Default object can be object container + object selector (e.g. in PG)
                        objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                        if (objectSelector != null && objectSelector.supportsDefaultChange()) {
                            currentDatabaseInstanceName = defObject.getName();
                            objectContainer = (DBSObjectContainer) defObject;
                            defObject = objectSelector.getDefaultObject();
                        }
                    }
                    Collection<? extends DBSObject> children = objectContainer.getChildren(monitor);
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
        @Override
        protected void fillContributionItems(List<IContributionItem> menuItems) {
            DBPDataSourceContainer dataSourceContainer = DataSourceToolbarUtils.getCurrentDataSource(UIUtils.getActiveWorkbenchWindow());
            if (dataSourceContainer == null) {
                return;
            }

            DatabaseListReader databaseListReader = new DatabaseListReader(dataSourceContainer.getDataSource());
            try {
                UIUtils.runInProgressService(databaseListReader);
            } catch (InvocationTargetException e) {
                DBWorkbench.getPlatformUI().showError("Schema list", "Error reading schema list", e.getTargetException());
                return;
            } catch (InterruptedException e) {
                return;
            }

            DBSObject[] defObjects = getSelectedSchema(dataSourceContainer);
            for (DBNDatabaseNode node : databaseListReader.nodeList) {
                menuItems.add(
                    new ActionContributionItem(new Action(node.getName(), Action.AS_CHECK_BOX) {
                        {
                            setImageDescriptor(DBeaverIcons.getImageDescriptor(node.getNodeIcon()));
                        }
                        @Override
                        public boolean isChecked() {
                            return ArrayUtils.contains(defObjects, node.getObject());
                        }
                        @Override
                        public void run() {
                            changeDataBaseSelection(dataSourceContainer, databaseListReader.currentDatabaseInstanceName, databaseListReader.currentDatabaseInstanceName, node.getNodeName());
                        }
                    }
                ));
            }
        }
    }
}