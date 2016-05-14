/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPContextProvider;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.IDataSourceContainerProvider;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.registry.DataSourceRegistry;
import org.jkiss.dbeaver.ui.editors.ProjectFileEditorInput;

/**
 * SQLEditorInput
 */
public class SQLEditorInput extends ProjectFileEditorInput implements IPersistableElement, DBPContextProvider, IDataSourceContainerProvider
{
    public static final QualifiedName PROP_DATA_SOURCE_ID = new QualifiedName("org.jkiss.dbeaver", "sql-editor-data-source-id");

    private static final Log log = Log.getLog(SQLEditorInput.class);

    public SQLEditorInput(IFile file)
    {
        super(file);
    }

    @Override
    public DBPDataSourceContainer getDataSourceContainer()
    {
        return getScriptDataSource(getFile());
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
        DBPDataSourceContainer container = getDataSourceContainer();
        if (container != null) {
            DBPDataSource dataSource = container.getDataSource();
            if (dataSource != null) {
                return dataSource.getDefaultContext(false);
            }
        }
        return null;
    }

    @Nullable
    public static DBPDataSourceContainer getScriptDataSource(IFile file)
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

    public static void setScriptDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer)
    {
        setScriptDataSource(file, dataSourceContainer, false);
    }

    public static void setScriptDataSource(@NotNull IFile file, @Nullable DBPDataSourceContainer dataSourceContainer, boolean notify)
    {
        try {
            file.setPersistentProperty(PROP_DATA_SOURCE_ID, dataSourceContainer == null ? null : dataSourceContainer.getId());
            if (notify) {
                final DBNProject projectNode = DBeaverCore.getInstance().getNavigatorModel().getRoot().getProject(file.getProject());
                if (projectNode != null) {
                    final DBNResource fileNode = projectNode.findResource(file);
                    if (fileNode != null) {
                        fileNode.refreshResourceState(dataSourceContainer);
                    }
                }
            }
        } catch (CoreException e) {
            log.error("Internal error while writing file property", e);
        }
    }

}
