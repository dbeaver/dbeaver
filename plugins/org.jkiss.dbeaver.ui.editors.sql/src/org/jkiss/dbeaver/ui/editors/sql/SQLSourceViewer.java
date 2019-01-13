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

package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.jface.action.*;
import org.eclipse.ui.IEditorInput;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.sql.handlers.OpenHandler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Display Source text (Read Only)
 */
public class SQLSourceViewer<T extends DBPScriptObject & DBSObject> extends SQLEditorNested<T> {

    private IAction OPEN_CONSOLE_ACTION = new Action("Open in SQL console", DBeaverIcons.getImageDescriptor(UIIcon.SQL_CONSOLE)) {
        @Override
        public void run()
        {
            final DBPDataSource dataSource = getDataSource();
            OpenHandler.openSQLConsole(
                UIUtils.getActiveWorkbenchWindow(),
                dataSource == null ? null : dataSource.getContainer(),
                "Source",
                getDocument().get()
            );
        }
    };

    @Override
    protected String getSourceText(DBRProgressMonitor monitor) throws DBException
    {
        return getSourceObject().getObjectDefinitionText(monitor, getSourceOptions());
    }

    protected Map<String, Object> getSourceOptions() {
        IEditorInput editorInput = getEditorInput();
        Map<String, Object> options = new HashMap<>();
        if (editorInput instanceof IDatabaseEditorInput) {
            Collection<String> attributeNames = ((IDatabaseEditorInput)editorInput).getAttributeNames();
            options.put(DBPScriptObject.OPTION_DDL_SOURCE, true);
            if (!attributeNames.isEmpty()) {
                for (String name : attributeNames) {
                    Object attribute = ((IDatabaseEditorInput)editorInput).getAttribute(name);
                    options.put(name, attribute);
                }
            }
        }
        return options;
    }

    @Override
    protected boolean isReadOnly()
    {
        return true;
    }

    @Override
    protected void setSourceText(DBRProgressMonitor monitor, String sourceText)
    {
    }

    @Override
    protected void contributeEditorCommands(IContributionManager toolBarManager) {
        super.contributeEditorCommands(toolBarManager);
        toolBarManager.add(new Separator());
        toolBarManager.add(OPEN_CONSOLE_ACTION);
    }

}
