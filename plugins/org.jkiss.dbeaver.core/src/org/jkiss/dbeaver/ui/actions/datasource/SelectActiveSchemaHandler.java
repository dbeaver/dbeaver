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
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.model.struct.DBSObjectSelector;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;

import java.util.Map;

public class SelectActiveSchemaHandler extends AbstractDataSourceHandler implements IElementUpdater
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        IWorkbenchWindow workbenchWindow = element.getServiceLocator().getService(IWorkbenchWindow.class);
        if (workbenchWindow == null || workbenchWindow.getActivePage() == null) {
            return;
        }
        IEditorPart activeEditor = workbenchWindow.getActivePage().getActiveEditor();
        if (activeEditor == null) {
            return;
        }

        String schemaName = "<No active connection>";
        DBIcon schemaIcon = DBIcon.TREE_SCHEMA;
        String schemaTooltip = CoreMessages.toolbar_datasource_selector_combo_database_tooltip;

        if (activeEditor instanceof DBPContextProvider) {
            DBCExecutionContext executionContext = ((DBPContextProvider) activeEditor).getExecutionContext();
            if (executionContext != null) {
                schemaName = "<no schema>";
                //DBSObjectContainer objectContainer = DBUtils.getAdapter(DBSObjectContainer.class, executionContext.getDataSource());
                DBSObjectSelector objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, executionContext.getDataSource());
                DBSObject defObject = objectSelector.getDefaultObject();

                if (defObject instanceof DBSObjectContainer) {
                    // Default object can be object container + object selector (e.g. in PG)
                    objectSelector = DBUtils.getAdapter(DBSObjectSelector.class, defObject);
                    if (objectSelector != null && objectSelector.supportsDefaultChange()) {
                        //objectContainer = (DBSObjectContainer) defObject;
                        defObject = objectSelector.getDefaultObject();
                    }
                }

                if (defObject != null) {
                    schemaName = defObject.getName();
                    schemaIcon = DBIcon.TREE_SCHEMA;
                }
            }
        }
        element.setText(schemaName);
        element.setIcon(DBeaverIcons.getImageDescriptor(schemaIcon));
        element.setTooltip(schemaTooltip);
    }
}