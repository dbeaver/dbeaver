/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.navigator.DBNDataSource;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

import java.util.Map;

public class DataSourceReadonlyHandler extends AbstractDataSourceHandler implements IElementUpdater {

    private DataSourceDescriptor currentDescriptor = null;

    @Override
    @Nullable
    public Object execute(@Nullable ExecutionEvent event) throws ExecutionException {
        final DataSourceDescriptor dataSourceContainer = currentDescriptor;
        if (dataSourceContainer != null) {
            dataSourceContainer.setConnectionReadOnly(!dataSourceContainer.isConnectionReadOnly());
            if (dataSourceContainer.getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
                dataSourceContainer.persistConfiguration();
            }
            if (dataSourceContainer.isConnected()) {
                DBPDataSource dataSource = dataSourceContainer.getDataSource();
                if (dataSource != null && !DataSourceInvalidateHandler.invalidateDataSource(dataSource)) {
                    dataSourceContainer.setConnectionReadOnly(!dataSourceContainer.isConnectionReadOnly());
                }
            }
            DataSourceToolbarUtils.triggerRefreshReadonlyElement();
        }
        return null;
    }

    @Override
    public void updateElement(@NotNull UIElement element, @Nullable Map parameters) {
        DBPDataSourceContainer container = getActiveDataSourceContainer(element.getServiceLocator().getService(IWorkbenchWindow.class));
        DataSourceDescriptor descriptor = container instanceof DataSourceDescriptor ? (DataSourceDescriptor) container : null;
        if (descriptor != currentDescriptor) {
            currentDescriptor = descriptor;
        }
        if (currentDescriptor != null) {
            boolean isReadonly = currentDescriptor.isConnectionReadOnly();
            if (isReadonly) {
                element.setTooltip(NLS.bind(CoreMessages.toolbar_checkbox_connection_not_readonly_tooltip, currentDescriptor.getName()));
                element.setIcon(DBeaverIcons.getImageDescriptor(UIIcon.SQL_READONLY));
            } else {
                element.setTooltip(NLS.bind(CoreMessages.toolbar_checkbox_connection_readonly_tooltip, currentDescriptor.getName()));
                element.setIcon(DBeaverIcons.getImageDescriptor(DBIcon.TREE_UNLOCKED));
            }
            element.setChecked(isReadonly);
        } else {
            element.setChecked(false);
            element.setTooltip(CoreMessages.dialog_connection_wizard_final_checkbox_connection_readonly);
            element.setIcon(DBeaverIcons.getImageDescriptor(DBIcon.TREE_UNLOCKED));
        }
    }


    @Nullable
    private static DBPDataSourceContainer getActiveDataSourceContainer(@NotNull IWorkbenchWindow window) {
        if (window != null) {
            IWorkbenchPage page = window.getActivePage();
            if (page != null) {
                IWorkbenchPart activePart = page.getActivePart();
                DBPDataSourceContainer container = getDataSourceContainerFromPart(activePart);
                if (container != null) {
                    return container;
                }
            }

            ISelection selection = window.getSelectionService().getSelection();
            if (selection != null) {
                DBSObject selectedObject = NavigatorUtils.getSelectedObject(selection);
                if (selectedObject instanceof DBPDataSourceContainer) {
                    return (DBPDataSourceContainer) selectedObject;
                } else if (selectedObject != null) {
                    DBPDataSource dataSource = selectedObject.getDataSource();
                    if (dataSource != null) {
                        return dataSource.getContainer();
                    }
                }
            }
        }
        return null;
    }

}
