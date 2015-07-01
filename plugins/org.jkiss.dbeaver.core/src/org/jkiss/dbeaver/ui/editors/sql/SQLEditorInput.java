/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.editors.sql;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * SQLEditorInput
 */
public class SQLEditorInput extends ProjectFileEditorInput implements IPersistableElement, DBPContextProvider, IDataSourceContainerProvider
{
    public static final QualifiedName PROP_DATA_SOURCE_ID = new QualifiedName("org.jkiss.dbeaver", "sql-editor-data-source-id");

    static final Log log = Log.getLog(SQLEditorInput.class);

    public static final String VAR_CONNECTION_NAME = "connectionName";
    public static final String VAR_FILE_NAME = "fileName";
    public static final String VAR_FILE_EXT = "fileExt";
    public static final String VAR_DRIVER_NAME = "driverName";

    public static final String DEFAULT_PATTERN = "<${" + VAR_CONNECTION_NAME + "}> ${" + VAR_FILE_NAME + "}";

    private String scriptName;

    public SQLEditorInput(IFile file)
    {
        super(file);
        this.scriptName = file.getFullPath().removeFileExtension().lastSegment();
    }

    @Override
    public DBSDataSourceContainer getDataSourceContainer()
    {
        return getScriptDataSource(getFile());
    }

    @Override
    public String getName()
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        DBPPreferenceStore preferenceStore;
        if (dataSourceContainer != null) {
            preferenceStore = dataSourceContainer.getPreferenceStore();
        } else {
            preferenceStore = DBeaverCore.getGlobalPreferenceStore();
        }
        String pattern = preferenceStore.getString(DBeaverPreferences.SCRIPT_TITLE_PATTERN);
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put(VAR_CONNECTION_NAME, dataSourceContainer == null ? "?" : dataSourceContainer.getName());
        vars.put(VAR_FILE_NAME, scriptName);
        vars.put(VAR_FILE_EXT, getFile().getFullPath().getFileExtension());
        vars.put(VAR_DRIVER_NAME, dataSourceContainer == null ? "?" : dataSourceContainer.getDriver().getFullName());
        return GeneralUtils.replaceVariables(pattern, vars);
    }

    @Override
    public String getToolTipText()
    {
        DBSDataSourceContainer dataSourceContainer = getDataSourceContainer();
        if (dataSourceContainer == null) {
            return super.getName();
        }
        return
            "Script: " + getFile().getName() + " \n" +
            "Connection: " + dataSourceContainer.getName() + " \n" +
            "Type: " + (dataSourceContainer.getDriver() == null ? "Unknown" : dataSourceContainer.getDriver().getFullName()) + " \n" +
            "URL: " + dataSourceContainer.getConnectionConfiguration().getUrl();
    }

    @Override
    public IPersistableElement getPersistable()
    {
//        if (!restoreEditorState()) {
//            return null;
//        }
        return this;
    }

    @Override
    public String getFactoryId()
    {
//        if (!restoreEditorState()) {
//            return null;
//        }
        return SQLEditorInputFactory.getFactoryId();
    }

    @Override
    public void saveState(IMemento memento)
    {
        SQLEditorInputFactory.saveState(memento, this);
    }

    @Nullable
    @Override
    public DBCExecutionContext getExecutionContext()
    {
        DBSDataSourceContainer container = getDataSourceContainer();
        if (container != null) {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource != null) {
                return dataSource.getDefaultContext(false);
            }
        }
        return null;
    }

    @Nullable
    public static DBSDataSourceContainer getScriptDataSource(IFile file)
    {
        try {
            if (!file.exists()) {
                return null;
            }
            String dataSourceId = file.getPersistentProperty(PROP_DATA_SOURCE_ID);
            if (dataSourceId != null) {
                DataSourceRegistry dataSourceRegistry = DBeaverCore.getInstance().getProjectRegistry().getDataSourceRegistry(file.getProject());
                return dataSourceRegistry == null ? null : dataSourceRegistry.getDataSource(dataSourceId);
            } else {
                return null;
            }
        } catch (CoreException e) {
            log.error("Internal error while reading file property", e);
            return null;
        }
    }

    public static void setScriptDataSource(@NotNull IFile file, @Nullable DBSDataSourceContainer dataSourceContainer)
    {
        setScriptDataSource(file, dataSourceContainer, false);
    }

    public static void setScriptDataSource(@NotNull IFile file, @Nullable DBSDataSourceContainer dataSourceContainer, boolean notify)
    {
        try {
            file.setPersistentProperty(PROP_DATA_SOURCE_ID, dataSourceContainer == null ? null : dataSourceContainer.getId());
            if (notify) {
                file.appendContents(new ByteArrayInputStream(new byte[0]), true, false, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            log.error("Internal error while writing file property", e);
        }
    }

}
