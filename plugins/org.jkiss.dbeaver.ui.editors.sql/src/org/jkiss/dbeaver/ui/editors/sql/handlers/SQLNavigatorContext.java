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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.sql.SQLScriptContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.actions.AbstractDataSourceHandler;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorContext;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;

public class SQLNavigatorContext implements DatabaseEditorContext {

    private DBPDataSourceContainer dataSourceContainer;
    private DBSObject selectedObject;
    private DBCExecutionContext executionContext;
    private SQLScriptContext scriptContext;

    SQLNavigatorContext(ExecutionEvent event) {
        this.selectedObject = AbstractDataSourceHandler.getActiveObject(event);
        if (selectedObject != null) {
            if (selectedObject instanceof DBPDataSourceContainer) {
                this.dataSourceContainer = (DBPDataSourceContainer) selectedObject;
            } else {
                this.dataSourceContainer = selectedObject.getDataSource().getContainer();
            }
            this.executionContext = null;
        } else {
            this.executionContext = AbstractDataSourceHandler.getActiveExecutionContext(event, false);
            if (this.executionContext != null) {
                this.dataSourceContainer = executionContext.getDataSource().getContainer();
            } else {
                this.dataSourceContainer = AbstractDataSourceHandler.getActiveDataSourceContainer(event, false);
            }
        }
    }

    public SQLNavigatorContext() {
        this.dataSourceContainer = null;
        this.selectedObject = null;
        this.executionContext = null;
    }

    public SQLNavigatorContext(DBSObject selectedObject) {
        this.selectedObject = selectedObject;
        this.dataSourceContainer = selectedObject == null ? null :
            (selectedObject instanceof DBPDataSourceContainer ?
                (DBPDataSourceContainer) selectedObject :
                selectedObject.getDataSource().getContainer());
        this.executionContext = null;
    }

    public SQLNavigatorContext(DBPDataSourceContainer dataSourceContainer, DBCExecutionContext executionContext) {
        this.dataSourceContainer = dataSourceContainer;
        this.executionContext = executionContext;
        this.selectedObject = null;
    }

    public SQLNavigatorContext(DBCExecutionContext executionContext) {
        this.selectedObject = null;
        this.dataSourceContainer = executionContext == null ? null : executionContext.getDataSource().getContainer();
        this.executionContext = executionContext;
    }

    public SQLNavigatorContext(SQLScriptContext scriptContext) {
        this.selectedObject = null;
        this.scriptContext = scriptContext;
    }

    @Nullable
    public SQLScriptContext getScriptContext() {
        return scriptContext;
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer() {
        if (dataSourceContainer != null) {
            return dataSourceContainer;
        } else if (scriptContext != null) {
            return scriptContext.getExecutionContext().getDataSource().getContainer();
        }
        return null;
    }

    public void setDataSourceContainer(DBPDataSourceContainer dataSourceContainer) {
        this.dataSourceContainer = dataSourceContainer;
    }

    @Override
    public DBSObject getSelectedObject() {
        return selectedObject;
    }

    public void setSelectedObject(DBSObject selectedObject) {
        this.selectedObject = selectedObject;
    }

    @Override
    public DBCExecutionContext getExecutionContext() {
        if (executionContext != null) {
            return executionContext;
        } else if (scriptContext != null) {
            return scriptContext.getExecutionContext();
        }
        return null;
    }

    public RCPProject getProject() {
        DBPDataSourceContainer ds = getDataSourceContainer();
        return (RCPProject) (ds != null ? ds.getProject() : NavigatorUtils.getSelectedProject());
    }

}
