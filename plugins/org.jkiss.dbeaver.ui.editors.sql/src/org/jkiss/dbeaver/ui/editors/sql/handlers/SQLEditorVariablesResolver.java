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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.resources.IFile;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DataSourceVariableResolver;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionContextDefaults;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;

import java.util.Objects;

public class SQLEditorVariablesResolver extends DataSourceVariableResolver {
    private final DBPDataSourceContainer dataSourceContainer;
    private final DBCExecutionContext context;
    private final String scriptName;
    private final IFile file;
    private final DBPProject project;

    public SQLEditorVariablesResolver(@Nullable DBPDataSourceContainer dataSourceContainer,
                                      @Nullable DBPConnectionConfiguration configuration,
                                      @Nullable DBCExecutionContext context,
                                      @Nullable String scriptName,
                                      @Nullable IFile file, DBPProject project) {
        super(dataSourceContainer, configuration);
        this.dataSourceContainer = dataSourceContainer;
        this.context = context;
        this.scriptName = scriptName;
        this.file = file;
        this.project = project;
    }

    @Nullable
    public String get(String name) {
        switch (name) {
            case SQLPreferenceConstants.VAR_ACTIVE_PROJECT:
                if (project != null){
                    return project.getName();
                } else {
                    return dataSourceContainer == null ? null : dataSourceContainer.getProject().getName();
                }
            case SQLPreferenceConstants.VAR_CONNECTION_NAME:
                return dataSourceContainer == null ? "none" : dataSourceContainer.getName();
            case SQLPreferenceConstants.VAR_DRIVER_NAME:
                return dataSourceContainer == null ? "?" : dataSourceContainer.getDriver().getFullName();
            case SQLPreferenceConstants.VAR_ACTIVE_DATABASE:
                return getContextInfo(SQLPreferenceConstants.VAR_ACTIVE_DATABASE);
            case SQLPreferenceConstants.VAR_ACTIVE_SCHEMA:
                return getContextInfo(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA);
            case SQLPreferenceConstants.VAR_FILE_NAME:
                return Objects.requireNonNullElse(this.scriptName, "");
            case SQLPreferenceConstants.VAR_FILE_EXT:
                if (file != null) {
                    return file.getFullPath().getFileExtension();
                } else {
                    return "";
                }
            default:
                return super.get(name);
        }
    }

    private String getContextInfo(String type) {
        if (context != null) {
            DBCExecutionContextDefaults<?, ?> contextDefaults = context.getContextDefaults();
            if (contextDefaults != null) {
                if (type.equals(SQLPreferenceConstants.VAR_ACTIVE_DATABASE))
                    return contextDefaults.getDefaultCatalog() == null ? null : contextDefaults.getDefaultCatalog().getName();
                if (type.equals(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA))
                    return contextDefaults.getDefaultSchema() == null ? null : contextDefaults.getDefaultSchema().getName();
            }
        }
        return null;
    }
}
